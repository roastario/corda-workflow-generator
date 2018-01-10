package com.stefano.corda.deposits

import net.corda.core.contracts.LinearState
import java.util.HashSet
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

data class ContinuingTransition<T : LinearState>(
        val clazz: KClass<T>,
        val previousState: ContinuingTransition<T>?,
        val stageName: String,
        val noLongerNull: Set<KProperty<Any?>> = setOf(),
        var nextStage: ContinuingTransition<T>? = null,
        val partyAllowedToTransition: KProperty<Any>?) {

    object Companion {
        fun <T : LinearState> wrap(stateClass: KClass<T>): ContinuingTransition<T> {
            return ContinuingTransition(stateClass, null, "start", setOf(), null, null)
        }
    }


    fun transition(newStageName: String, noLongerNull: Set<KProperty<Any?>>, allowedTransitioner: KProperty<Any>): ContinuingTransition<T> {
        val setOfNonNulls = HashSet(this.noLongerNull)
        setOfNonNulls.addAll(noLongerNull)
        val continuingTransition = ContinuingTransition(this.clazz, this, newStageName, setOfNonNulls, null, allowedTransitioner)
        this.nextStage = continuingTransition
        return continuingTransition
    }

    fun transition(newStageName: String, noLongerNull: KProperty<Any?>, allowedTransitioner: KProperty<Any>): ContinuingTransition<T> {
        return transition(newStageName, setOf(noLongerNull), allowedTransitioner)
    }
}







