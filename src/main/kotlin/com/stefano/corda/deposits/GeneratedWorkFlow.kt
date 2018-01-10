package com.stefano.corda.deposits

import com.google.common.base.CaseFormat
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.stefano.corda.deposits.GeneratedWorkFlow.Companion.getEnd
import com.stefano.corda.deposits.GeneratedWorkFlow.Companion.inOrder
import com.stefano.corda.deposits.GeneratedWorkFlow.Companion.reversedOrder
import net.corda.core.contracts.LinearState

class GeneratedWorkFlow<T : LinearState>(val start: ContinuingTransition<T>,
                                         val end: ContinuingTransition<T> = getEnd(start)
) {
    object Companion {
        fun <T : LinearState> getEnd(start: ContinuingTransition<T>): ContinuingTransition<T> {
            return inOrder(start).last()
        }


        internal fun <T : LinearState> inOrder(toStartFrom: ContinuingTransition<T>): Iterable<ContinuingTransition<T>> {
            return Iterable({
                object : Iterator<ContinuingTransition<T>> {
                    var next: ContinuingTransition<T>? = toStartFrom
                    override fun hasNext(): Boolean {
                        return next != null
                    }

                    override fun next(): ContinuingTransition<T> {
                        val current = next;
                        next = next!!.nextStage;
                        return current!!
                    }
                }
            });
        }


        internal fun <T : LinearState> reversedOrder(toStartFrom: ContinuingTransition<T>,
                                                     toEndAt: ContinuingTransition<T>?): Iterable<ContinuingTransition<T>> {
            return Iterable({
                object : Iterator<ContinuingTransition<T>> {
                    var previous: ContinuingTransition<T>? = toStartFrom;
                    override fun hasNext(): Boolean {
                        return previous != null && previous != toEndAt
                    }

                    override fun next(): ContinuingTransition<T> {
                        val current = previous;
                        previous = previous!!.previousState;
                        return current!!;
                    }
                }
            });
        }
    }

    fun printOutFlow(): GeneratedWorkFlow<T> {
        var next: ContinuingTransition<T>? = start;
        var previous: ContinuingTransition<T>? = null
        while (next != null) {
            println(previous?.stageName + " -> " + next.stageName + " must not be null: " + next.noLongerNull.map { it.name })
            previous = next
            next = next.nextStage
        }
        return this;
    }


    fun generate() {
        val file = FileSpec.builder(start.clazz.java.`package`.name, start.clazz.simpleName + "Checks")
        buildStageEnumConstants(file)
        buildIsStageChecks(file)
        buildWhatStageCheck(file)
        buildTransitionChecks(file)
        file.build().writeTo(System.out)
    }

    private fun buildStageEnumConstants(fileSpec: FileSpec.Builder) {
        val typeSpec = TypeSpec.enumBuilder(buildEnumTypeNameForState())
        inOrder(start).forEach { stage ->
            typeSpec.addEnumConstant(buildEnumConstantForStageName(stage))
        }
        fileSpec.addType(typeSpec.build())
    }

    private fun buildEnumTypeNameForState() = start.clazz.simpleName + "Stages"

    private fun buildEnumConstantForStageName(stage: ContinuingTransition<T>) =
            CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, stage.stageName)

    private fun buildIsStageChecks(fileSpec: FileSpec.Builder) {
        inOrder(start).forEach { it ->
            if (it.noLongerNull.isNotEmpty()) {
                val functionPointer = FunSpec.builder(buildStageCheckName(it))
                        .addParameter("toCheck", start.clazz)
                        .returns(Boolean::class)

                //build the check for each non null property
                it.noLongerNull.forEach { nonNullProperty ->
                    functionPointer.beginControlFlow("if (%T::%L.get(toCheck) == null)", start.clazz, nonNullProperty.name)
                            .addStatement("return false")
                            .endControlFlow()
                }

                //now enforce the check that it is not in any other further along states
                val otherStatesCheck = reversedOrder(end, it).joinToString(separator = "\n&& ", transform = { otherStage ->
                    "!" + buildStageCheckName(otherStage) + "(toCheck)"
                })

                functionPointer.addStatement("return " + if (otherStatesCheck.isEmpty()) "true" else otherStatesCheck);
                fileSpec.addFunction(functionPointer.build())
            }
        }
    }

    private fun buildStageCheckName(it: ContinuingTransition<T>) =
            "isInStage" + it.stageName.capitalize()


    private fun buildWhatStageCheck(fileSpec: FileSpec.Builder) {
        val functionBuilder = FunSpec.builder("getStage").addParameter("toCheck", start.clazz)
        reversedOrder(end, start).forEach { stage ->
            functionBuilder.beginControlFlow("if (" + buildStageCheckName(stage) + "(toCheck))")
            functionBuilder.addStatement("return " + buildQualifiedEnum(stage))
            functionBuilder.endControlFlow()
        }

        functionBuilder.returns(ClassName.bestGuess(buildEnumTypeNameForState()))
        functionBuilder.addStatement("throw %T()", IllegalStateException::class)
        fileSpec.addFunction(functionBuilder.build())
    }

    private fun buildTransitionChecks(fileSpec: FileSpec.Builder) {
        val functionBuilder = FunSpec.builder("canTransition")
                .addParameter("input", start.clazz)
                .addParameter("output", start.clazz)
                .addParameter("transitionToken", ClassName.bestGuess("Any?"))
                .returns(Boolean::class)


        reversedOrder(end, null).forEach { stage ->
            functionBuilder.beginControlFlow("if (getStage(input) === %L)", buildQualifiedEnum(stage))
            var returnStatement = "return getStage(output) === %L"
            stage.nextStage?.let { nextStage ->
                nextStage.partyAllowedToTransition?.let { transitionTokenGetter ->
                    returnStatement = returnStatement +
                            "\n&& " + stage.clazz.simpleName + "::" + transitionTokenGetter.name +
                            ".get(input).equals(transitionToken);"
                }
                functionBuilder.addStatement(returnStatement, buildQualifiedEnum(nextStage))
            }
            functionBuilder.endControlFlow()
        }
        functionBuilder.addStatement("throw %T()", IllegalStateException::class)
        fileSpec.addFunction(functionBuilder.build())
    }

    private fun buildQualifiedEnum(nextStage: ContinuingTransition<T>) =
            buildEnumTypeNameForState() + "." + buildEnumConstantForStageName(nextStage)
}
