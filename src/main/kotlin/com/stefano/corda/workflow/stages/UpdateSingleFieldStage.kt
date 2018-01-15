package com.stefano.corda.workflow.stages

import com.squareup.kotlinpoet.FileSpec
import com.stefano.corda.workflow.ContinuingTransition
import com.stefano.corda.workflow.Stage
import net.corda.core.contracts.LinearState
import kotlin.reflect.KProperty1

class UpdateSingleFieldStage<IN : LinearState, OUT : Any>(private val property: KProperty1<IN, OUT?>) : Stage<IN, OUT> {
    override fun toFlow(workflowName: String, transition: ContinuingTransition<out LinearState, IN>): FileSpec {
        TODO("not implemented")
    }

    override fun property(): KProperty1<IN, OUT?> {
        return property;
    }


}