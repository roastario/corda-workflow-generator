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
import net.corda.core.contracts.Command
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

class OpenStage<T : LinearState>(private val stateClass: KClass<T>) : Stage<T> {

    override fun toDot(workflowName: String, transition: ContinuingTransition<*, *>): Node {
        return Node(transition.stageName.capitalize()).setShape(Shape.rect)
                .setComment("Commits a new: " + transition.thisClass.simpleName)
    }


    override fun toFlow(workflowName: String, transition: ContinuingTransition<*, *>): TypeSpec {

        val flowObject = TypeSpec.objectBuilder(transition.stageName.capitalize() + "Flow");
        val initiatorBuilder = buildInitiatorType(stateClass)

        val callBuilder = FunSpec.builder("call").returns(UniqueIdentifier::class).addModifiers(KModifier.OVERRIDE).addAnnotation(Suspendable::class)
        callBuilder.addStatement("val notary : %T = serviceHub.networkMapCache.notaryIdentities[0]", Party::class)
        callBuilder.addStatement("val ourKey = serviceHub.myInfo.legalIdentities.map { it.owningKey }.first()")
        callBuilder.addStatement("val command = %T(%T.Commands.%L(), ourKey)",
                Command::class,
                ClassName.bestGuess(getContractNameForWorkflow(workflowName)),
                transition.stageName.capitalize()
        )
        callBuilder.addStatement("val proposedTransaction: %T = %T(notary).withItems( %T(input, %L.CONTRACT_ID), command )",
                TransactionBuilder::class,
                TransactionBuilder::class,
                StateAndContract::class,
                getContractNameForWorkflow(workflowName)
        )
        callBuilder.addStatement("proposedTransaction.verify(serviceHub)")
        callBuilder.addStatement("val signedTransaction = serviceHub.signInitialTransaction(proposedTransaction)")
        callBuilder.addStatement("subFlow(%T(signedTransaction))", FinalityFlow::class)
        callBuilder.addStatement("return input.linearId")

        initiatorBuilder.addFunction(callBuilder.build())
        flowObject.addType(initiatorBuilder.build())
        buildDumbResponderFlow(flowObject)
        return flowObject.build();
    }


    override fun property(): KProperty1<T, UniqueIdentifier?> {
        return stateClass.declaredMemberProperties.find { property -> property.name == LinearState::linearId.name } as KProperty1<T, UniqueIdentifier?>;
    }


}
