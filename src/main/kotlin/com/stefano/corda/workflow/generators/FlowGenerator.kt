package com.stefano.corda.workflow

import com.squareup.kotlinpoet.TypeSpec
import com.stefano.corda.workflow.WorkflowGenerator.Companion.inOrder
import net.corda.core.contracts.LinearState

class FlowGenerator(workflowName: String,
                    start: ContinuingTransition<out LinearState, out LinearState>) : WorkflowGenerator(workflowName, start){


    override fun generate(): TypeSpec.Builder{
        val flowsHolder = TypeSpec.classBuilder(workflowName.capitalize() + "Flows")
        inOrder(start).forEach{transition ->
            val generatedFlow = transition.stageDescription.toFlow(workflowName, transition)
            flowsHolder.addType(generatedFlow)
        }
        return flowsHolder;


    }


}