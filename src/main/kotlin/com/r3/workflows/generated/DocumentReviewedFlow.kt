package com.r3.workflows.generated

import co.paralleluniverse.fibers.Suspendable
import com.stefano.corda.workflow.DemoState
import java.time.Instant
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

object DocumentReviewedFlow {
    @InitiatingFlow
    @StartableByRPC
    class Inititator(
            val update: Instant,
            val identifier: UniqueIdentifier,
            val toSendTo: Party
    ) : FlowLogic<UniqueIdentifier>() {
        private inline fun loadInput(identifier: UniqueIdentifier): StateAndRef<DemoState> {
            val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(identifier), status=Vault.StateStatus.UNCONSUMED)
            return serviceHub.vaultService.queryBy<DemoState>(DemoState::class.java, criteria).states.single()
        }

        @Suspendable
        override fun call(): UniqueIdentifier {
            val notary : Party = serviceHub.networkMapCache.notaryIdentities[0]
            val ourKey = serviceHub.myInfo.legalIdentities.map { it.owningKey }.first()
            val command = Command(FileUploadAndCheckWorkflowContract.Commands.DocumentReviewed(), listOf(ourKey, toSendTo.owningKey))
            val inputRefAndState = loadInput(identifier)
            val input = inputRefAndState.state.data
            val output = input.copy(documentReviewed=update)
            val proposedTransaction: TransactionBuilder = TransactionBuilder(notary)
                    .addInputState(inputRefAndState)
                    .addOutputState(output, FileUploadAndCheckWorkflowContract.CONTRACT_ID)
                    .addCommand(command)
            proposedTransaction.verify(serviceHub)
            val ourSignedTransaction = serviceHub.signInitialTransaction(proposedTransaction)
            val counterPartySession = initiateFlow(toSendTo)
            val fullySignedTx = subFlow( CollectSignaturesFlow(ourSignedTransaction, setOf(counterPartySession)) )
            val committedTransaction = subFlow(FinalityFlow(fullySignedTx))
            return committedTransaction.tx.outputsOfType(DemoState::class.java).first().linearId
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