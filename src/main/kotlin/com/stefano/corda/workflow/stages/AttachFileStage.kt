package com.stefano.corda.workflow.stages

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.stefano.corda.workflow.ContinuingTransition
import com.stefano.corda.workflow.Stage
import net.corda.core.contracts.LinearState
import net.corda.core.crypto.SecureHash
import kotlin.reflect.KProperty1

class AttachFileStage<T : LinearState>(private val property: KProperty1<T, SecureHash?>) : (T) -> SecureHash, Stage<T, SecureHash> {


    override fun toFlow(workflowName: String, transition: ContinuingTransition<out LinearState, T>): FileSpec {
        val builder = FileSpec.builder("com.r3.workflows.generated", "OpenFlow");
        val flowObject = TypeSpec.objectBuilder("OpenFlow");

        val initiatorBuilder = buildInitiatorForUpdateType(property.returnType)

        return builder.build();
    }

    override fun property(): KProperty1<T, SecureHash?> {
        return property;
    }


    override fun invoke(p1: T): SecureHash {
        TODO("not implemented")
    }


}