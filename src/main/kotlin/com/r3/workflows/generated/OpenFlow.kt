package com.r3.workflows.generated

import co.paralleluniverse.fibers.Suspendable
import com.stefano.corda.workflow.DemoState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

object OpenFlow {
    @InitiatingFlow
    @StartableByRPC
    class Inititator(val input: DemoState) : FlowLogic<UniqueIdentifier>() {
        @Suspendable
        override fun call(): UniqueIdentifier {
            val notary : Party = serviceHub.networkMapCache.notaryIdentities[0]
            val ourKey = serviceHub.myInfo.legalIdentities.map { it.owningKey }.first()
            val command = Command(FileUploadAndCheckWorkflowContract.Commands.Open(), ourKey)
            val proposedTransaction: TransactionBuilder = TransactionBuilder(notary).withItems( StateAndContract(input, FileUploadAndCheckWorkflowContract.CONTRACT_ID), command )
            proposedTransaction.verify(serviceHub)
            val signedTransaction = serviceHub.signInitialTransaction(proposedTransaction)
            subFlow(FinalityFlow(signedTransaction))
            return input.linearId
        }
    }

    @InitiatingFlow
    @InitiatedBy(Inititator::class)
    class Responder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {

            val flow = object : SignTransactionFlow(counterpartySession) {
                @Suspendable
                override fun checkTransaction(stx: SignedTransaction) {
                    //all checks delegated to contract
                }
            }

            val stx = subFlow(flow)
            return waitForLedgerCommit(stx.id)

        }
    }
}