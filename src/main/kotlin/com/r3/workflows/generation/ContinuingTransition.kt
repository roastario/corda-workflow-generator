package com.r3.workflows.generation

import com.r3.workflows.generation.stages.OpenStage
import com.r3.workflows.generation.stages.Stage
import net.corda.core.contracts.LinearState
import net.corda.core.identity.AbstractParty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

data class ContinuingTransition<PREVIOUS : LinearState, CURRENT : LinearState>(
        val thisClass: KClass<CURRENT>,
        val previousState: ContinuingTransition<*, PREVIOUS>?,
        val stageName: String,
        val stageDescription: Stage<CURRENT>,
        val partyAllowedToTransition: KProperty1<CURRENT, AbstractParty?>?,
        var nextStage: ContinuingTransition<CURRENT, *>? = null) {

    object Companion {
        fun <T : LinearState> wrap(stateClass: KClass<T>): ContinuingTransition<LinearState, T> {
            return ContinuingTransition(stateClass,
                    null, "start",
                    OpenStage(stateClass),
                    null,
                    null)
        }
    }

    fun transition(stageName: String,
                   stageDescription: Stage<CURRENT>,
                   transitioner: KProperty1<CURRENT, AbstractParty?>): ContinuingTransition<CURRENT, CURRENT> {
        val nextStage = ContinuingTransition(thisClass, this, stageName, stageDescription, transitioner)
        this.nextStage = nextStage
        return nextStage;
    }
}









