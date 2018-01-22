package com.r3.workflows.generation.stages

import co.paralleluniverse.fibers.Suspendable
import com.r3.workflows.generation.ContinuingTransition
import com.r3.workflows.generation.stages.Stage.Companion.getContractNameForWorkflow
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import info.leadinglight.jdot.Node
import info.leadinglight.jdot.enums.Shape
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.finance.contracts.asset.Cash
import java.util.*
import kotlin.reflect.KProperty1

class UpdateSingleFieldAndGenerateCashMovementStage<IN : LinearState, OUT : Any>(
        private val underlying: UpdateSingleFieldStage<IN, OUT>,
        private val propertyForPayment: KProperty1<IN, Party?>,
        private val propertyForPaymentAmount: KProperty1<IN, Amount<Currency>?>) : Stage<IN> {

    override fun toDot(workflowName: String, transition: ContinuingTransition<*, *>): Node {
        return Node(transition.stageName.capitalize()).setShape(Shape.rect)
                .setLabel("Updates: " + transition.thisClass.simpleName + "::" + property().name + "\n"
                        + "and generates a cash movement to " + transition.thisClass.simpleName + "::" + propertyForPayment.name + "\n"
                        + "for amount " + transition.thisClass.simpleName + "::" + propertyForPaymentAmount.name)
    }


    override fun toFlow(workflowName: String, transition: ContinuingTransition<*, *>): TypeSpec {
        val flowObject = TypeSpec.objectBuilder(transition.stageName.capitalize() + "Flow");

        val initiatorBuilder = buildInitiatorForUpdateType(
                "update" to transition.stageDescription.property().returnType,
                "identifier" to UniqueIdentifier::class,
                "toSendTo" to Party::class)

        initiatorBuilder.addFunction(buildLookup(transition.thisClass).build())

        val callBuilder = FunSpec.builder("call").returns(UniqueIdentifier::class).addModifiers(KModifier.OVERRIDE).addAnnotation(Suspendable::class)
        callBuilder.addStatement("val notary : %T = serviceHub.networkMapCache.notaryIdentities[0]", Party::class)
        callBuilder.addStatement("val ourKey = serviceHub.myInfo.legalIdentities.map { it.owningKey }.first()")
        callBuilder.addStatement("val command = %T(%T.Commands.%L(), listOf(ourKey, toSendTo.owningKey))",
                Command::class,
                ClassName.bestGuess(getContractNameForWorkflow(workflowName)),
                transition.stageName.capitalize())

        callBuilder.addStatement("val inputRefAndState = loadInput(identifier)")
        callBuilder.addStatement("val input = inputRefAndState.state.data")
        callBuilder.addStatement("val output = input.copy(%L=update)", property().name)

        callBuilder.addStatement("val proposedTransaction: %T = %T(notary)\n" +
                ".addInputState(inputRefAndState)\n" +
                ".addOutputState(output, %L.CONTRACT_ID)\n" +
                ".addCommand(command)",
                TransactionBuilder::class,
                TransactionBuilder::class,
                getContractNameForWorkflow(workflowName)
        )


        callBuilder.addStatement("val partyToPay = %T::%L.get(input)", transition.thisClass, propertyForPayment.name)
        callBuilder.addStatement("val amountToPay = %T::%L.get(input)!!", transition.thisClass, propertyForPaymentAmount.name)
        callBuilder.addStatement("%T.generateSpend(serviceHub, proposedTransaction, amountToPay, partyToPay)", Cash::class)
        callBuilder.addStatement("proposedTransaction.verify(serviceHub)")
        callBuilder.addStatement("val ourSignedTransaction = serviceHub.signInitialTransaction(proposedTransaction)")
        callBuilder.addStatement("val counterPartySession = initiateFlow(toSendTo)")
        callBuilder.addStatement("val fullySignedTx = subFlow( %T(ourSignedTransaction, setOf(counterPartySession)) )", CollectSignaturesFlow::class)
        callBuilder.addStatement("val committedTransaction = subFlow(%T(fullySignedTx))", FinalityFlow::class)
        callBuilder.addStatement("return committedTransaction.tx.outputsOfType(%T::class.java).first().linearId", transition.thisClass)

        initiatorBuilder.addFunction(callBuilder.build())
        flowObject.addType(initiatorBuilder.build())
        buildDumbResponderFlow(flowObject)

        return flowObject.build()

    }

    override fun property(): KProperty1<IN, OUT?> {
        return underlying.property();
    }


}