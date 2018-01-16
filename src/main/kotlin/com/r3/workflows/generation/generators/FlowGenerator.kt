package com.r3.workflows.generation.generators

import com.r3.workflows.generation.ContinuingTransition
import com.squareup.kotlinpoet.TypeSpec
import net.corda.core.contracts.LinearState

class FlowGenerator(workflowName: String,
                    start: ContinuingTransition<out LinearState, out LinearState>) : WorkflowGenerator(workflowName, start){


    override fun <T: Any> generate() : T{
        val flowsHolder = TypeSpec.classBuilder(workflowName.capitalize() + "Flows")
        inOrder(start).forEach{ transition ->
            val generatedFlow = transition.stageDescription.toFlow(workflowName, transition)
            flowsHolder.addType(generatedFlow)
        }
        return flowsHolder as T;


    }


}