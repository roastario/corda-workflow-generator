package com.stefano.corda.workflow

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeSpec
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType

interface Stage<T : LinearState, out O : Any> {

    fun property(): KProperty1<T, O?>

    fun toFlow(workflowName: String, transition: ContinuingTransition<out LinearState, T>): FileSpec


    fun getContractNameForWorkflow(name: String): String {
        return getWorkflowName(name) + "Contract"
    }

    private fun getWorkflowName(name: String) = name + "Workflow"


    fun buildInitiatorType(classOfInput: KClass<*>): TypeSpec.Builder {
        val initiatorBuilder = TypeSpec.classBuilder("Inititator")
                .addAnnotation(InitiatingFlow::class)
                .addAnnotation(StartableByRPC::class)
                .primaryConstructor(FunSpec.constructorBuilder().addParameter("val input", classOfInput).build())
                .superclass(ParameterizedTypeName.get(FlowLogic::class, UniqueIdentifier::class))
        return initiatorBuilder
    }

    fun buildInitiatorForUpdateType(classOfInput: KClass<*>): TypeSpec.Builder {
        val initiatorBuilder = TypeSpec.classBuilder("Inititator")
                .addAnnotation(InitiatingFlow::class)
                .addAnnotation(StartableByRPC::class)
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("val input", classOfInput)
                        .addParameter("val stateId", UniqueIdentifier::class)
                        .build())
                .superclass(ParameterizedTypeName.get(FlowLogic::class, UniqueIdentifier::class))
        return initiatorBuilder
    }

    fun buildInitiatorForUpdateType(typeOfInput: KType): TypeSpec.Builder {

        if (typeOfInput.classifier is KClass<*>){
            return buildInitiatorForUpdateType(typeOfInput.classifier as KClass<*>)
        }else{
            throw IllegalArgumentException(typeOfInput.toString() + " cannot be processed")
        }
    }

}