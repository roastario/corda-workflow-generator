package com.stefano.corda.workflow

import com.google.common.base.CaseFormat
import com.squareup.kotlinpoet.TypeSpec
import net.corda.core.contracts.LinearState
import kotlin.reflect.KClass

abstract class WorkflowGenerator(val workflowName: String, val start: ContinuingTransition<out LinearState, out LinearState>, val end: ContinuingTransition<out LinearState, out LinearState> = Companion.getEnd(start)) {
    object Companion {
        fun getEnd(start: ContinuingTransition<out LinearState, out LinearState>): ContinuingTransition<out LinearState, out LinearState> {
            return inOrder(start).last()
        }


        internal fun inOrder(toStartFrom: ContinuingTransition<out LinearState, out LinearState>): Iterable<ContinuingTransition<out LinearState, out LinearState>> {
            return Iterable({
                object : Iterator<ContinuingTransition<out LinearState, out LinearState>> {
                    var next: ContinuingTransition<out LinearState, out LinearState>? = toStartFrom
                    override fun hasNext(): Boolean {
                        return next != null
                    }

                    override fun next(): ContinuingTransition<out LinearState, out LinearState> {
                        val current = next;
                        next = next!!.nextStage;
                        return current!!
                    }
                }
            });
        }


        internal fun reversedOrder(toStartFrom: ContinuingTransition<out LinearState, out LinearState>,
                                   toEndAt: ContinuingTransition<out LinearState, out LinearState>?): Iterable<ContinuingTransition<out LinearState, out LinearState>> {
            return Iterable({
                object : Iterator<ContinuingTransition<out LinearState, out LinearState>> {
                    var previous: ContinuingTransition<out LinearState, out LinearState>? = toStartFrom;
                    override fun hasNext(): Boolean {
                        return previous != null && previous != toEndAt
                    }

                    override fun next(): ContinuingTransition<out LinearState, out LinearState> {
                        val current = previous;
                        previous = previous!!.previousState;
                        return current!!;
                    }
                }
            });
        }
    }

    protected fun generateCheckClassName(kclass: KClass<*>): String {
        return kclass.simpleName + "WorkflowChecks";
    }

    protected fun generateEnumCollectiveName(kclass: KClass<*>): String {
        return kclass.simpleName + "WorkflowStages";
    }

    protected fun generateEnumConstantName(stage: ContinuingTransition<out LinearState, out LinearState>): String {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, stage.stageName)
    }

    protected fun buildFullEnumName(stage: ContinuingTransition<*, *>): Any? {
        return generateEnumCollectiveName(stage.thisClass) + "." + generateEnumConstantName(stage)
    }

    abstract fun generate() : TypeSpec.Builder
}