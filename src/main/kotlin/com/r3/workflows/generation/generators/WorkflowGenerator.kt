package com.r3.workflows.generation.generators

import com.r3.workflows.generation.ContinuingTransition
import net.corda.core.contracts.LinearState

abstract class WorkflowGenerator( workflowName: String,
                                  start: ContinuingTransition<out LinearState, out LinearState>) : Generator(workflowName, start){





}