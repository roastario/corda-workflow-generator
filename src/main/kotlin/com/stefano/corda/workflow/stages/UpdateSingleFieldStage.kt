package com.stefano.corda.workflow.stages

import co.paralleluniverse.fibers.Suspendable
import com.squareup.kotlinpoet.*
import com.stefano.corda.workflow.ContinuingTransition
import com.stefano.corda.workflow.Stage
import net.corda.core.contracts.Command
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import java.io.InputStream
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class UpdateSingleFieldStage<IN : LinearState, OUT : Any>(private val property: KProperty1<IN, OUT?>) : Stage<IN, OUT> {


    override fun toFlow(workflowName: String, transition: ContinuingTransition<out LinearState, IN>): FileSpec {
        val builder = FileSpec.builder("com.r3.workflows.generated", transition.stageName.capitalize() + "Flow");
        val flowObject = TypeSpec.objectBuilder(transition.stageName.capitalize() + "Flow");

        val initiatorBuilder = buildInitiatorForUpdateType(
                "update" to transition.stageDescription.property().returnType.classifier as KClass<*>,
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
        callBuilder.addStatement("val output = input.copy(%L=update)", property.name)

        callBuilder.addStatement("val proposedTransaction: %T = %T(notary)\n" +
                ".addInputState(inputRefAndState)\n" +
                ".addOutputState(output, %L.CONTRACT_ID)\n" +
                ".addCommand(command)",
                TransactionBuilder::class,
                TransactionBuilder::class,
                getContractNameForWorkflow(workflowName)
        )


        callBuilder.addStatement("proposedTransaction.verify(serviceHub)")
        callBuilder.addStatement("val ourSignedTransaction = serviceHub.signInitialTransaction(proposedTransaction)")
        callBuilder.addStatement("val counterPartySession = initiateFlow(toSendTo)" )
        callBuilder.addStatement("val fullySignedTx = subFlow( %T(ourSignedTransaction, setOf(counterPartySession)) )", CollectSignaturesFlow::class)
        callBuilder.addStatement("val committedTransaction = subFlow(%T(fullySignedTx))", FinalityFlow::class)
        callBuilder.addStatement("return committedTransaction.tx.outputsOfType(%T::class.java).first().linearId", transition.thisClass)

        initiatorBuilder.addFunction(callBuilder.build())
        flowObject.addType(initiatorBuilder.build())
        buildDumbResponderFlow(flowObject)
        builder.addType(flowObject.build())

        return builder.build();

    }

    override fun property(): KProperty1<IN, OUT?> {
        return property;
    }


}