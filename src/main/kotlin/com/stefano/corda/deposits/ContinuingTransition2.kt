package com.stefano.corda.deposits

import net.corda.core.contracts.LinearState
import java.util.HashSet
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

data class ContinuingTransition2<PREVIOUS : LinearState, CURRENT : LinearState>(
        val thisClass: KClass<CURRENT>,
        val previousState: ContinuingTransition2<*, PREVIOUS>?,
        val stageName: String,
        val mustNotBeNullInStage: Set<KProperty1<CURRENT, Any?>> = setOf(),
        var nextStage: ContinuingTransition2<CURRENT, *>? = null,
        val partyAllowedToTransition: KProperty1<CURRENT, Any?>?) {

    object Companion {
        fun <T : LinearState> wrap(stateClass: KClass<T>): ContinuingTransition2<LinearState, T> {
            return ContinuingTransition2(stateClass, null, "start", setOf(), null, null)
        }
    }

    fun transitionUsingSameState(newStageName: String,
                                 noLongerNull: Set<KProperty1<CURRENT, Any?>>,
                                 allowedTransitioner: KProperty1<CURRENT, Any?>): ContinuingTransition2<CURRENT, CURRENT> {
        return transitionUsingNewState(this.thisClass, newStageName, noLongerNull, allowedTransitioner);
    }

    fun transitionUsingSameState(newStageName: String,
                                 noLongerNull: KProperty1<CURRENT, Any?>,
                                 allowedTransitioner: KProperty1<CURRENT, Any?>): ContinuingTransition2<CURRENT, CURRENT> {


        return transitionUsingSameState(newStageName, setOf(noLongerNull), allowedTransitioner);
    }

    fun <NEWSTATE : LinearState> transitionUsingNewState(newStateClass: KClass<NEWSTATE>,
                                                         newStageName: String,
                                                         noLongerNull: Set<KProperty1<NEWSTATE, Any?>>,
                                                         allowedTransitioner: KProperty1<NEWSTATE, Any?>): ContinuingTransition2<CURRENT, NEWSTATE> {

        val newTransition = ContinuingTransition2(newStateClass, this, newStageName, noLongerNull, null, allowedTransitioner)
        this.nextStage = newTransition
        return newTransition;
    }

    fun <NEWSTATE : LinearState> transitionUsingNewState(newStateClass: KClass<NEWSTATE>,
                                                         newStageName: String,
                                                         noLongerNull: KProperty1<NEWSTATE, Any?>,
                                                         allowedTransitioner: KProperty1<NEWSTATE, Any?>): ContinuingTransition2<CURRENT, NEWSTATE> {

        return transitionUsingNewState(newStateClass, newStageName, setOf(noLongerNull), allowedTransitioner);
    }


}







