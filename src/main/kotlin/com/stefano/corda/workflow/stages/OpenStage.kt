package com.stefano.corda.workflow.stages

import co.paralleluniverse.fibers.Suspendable
import com.squareup.kotlinpoet.*
import com.stefano.corda.workflow.ContinuingTransition
import com.stefano.corda.workflow.Stage
import net.corda.core.contracts.Command
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

class OpenStage<T : LinearState>(private val stateClass: KClass<T>) : Stage<T, UniqueIdentifier> {


    object Companion {




    }


    override fun toFlow(workflowName: String, transition: ContinuingTransition<out LinearState, T>): FileSpec {

        val builder = FileSpec.builder("com.r3.workflows.generated", "OpenFlow");
        val flowObject = TypeSpec.objectBuilder("OpenFlow");
        val initiatorBuilder = buildInitiatorType(stateClass)

        val callBuilder = FunSpec.builder("call").returns(UniqueIdentifier::class).addModifiers(KModifier.OVERRIDE).addAnnotation(Suspendable::class)
        callBuilder.addStatement("val notary : %T = serviceHub.networkMapCache.notaryIdentities[0]", Party::class)
        callBuilder.addStatement("val ourKey = serviceHub.myInfo.legalIdentities.map { it.owningKey }.first()")
        callBuilder.addStatement("val command = %T(%T.Commands.Open(), ourKey)", Command::class, ClassName.bestGuess(getContractNameForWorkflow(workflowName)))
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
        builder.addType(flowObject.build())




        return builder.build();


    }



    override fun property(): KProperty1<T, UniqueIdentifier?> {
        return stateClass.declaredMemberProperties.find { property -> property.name == LinearState::linearId.name } as KProperty1<T, UniqueIdentifier?>;
    }


}
