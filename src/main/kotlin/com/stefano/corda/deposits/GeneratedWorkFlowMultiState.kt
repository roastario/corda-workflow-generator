package com.stefano.corda.deposits

import com.google.common.base.CaseFormat
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import net.corda.core.contracts.LinearState
import kotlin.reflect.KClass

class GeneratedWorkFlowMultiState(val start: ContinuingTransition2<out LinearState, out LinearState>,
                                  val end: ContinuingTransition2<out LinearState, out LinearState> = Companion.getEnd(start)
) {
    object Companion {
        fun getEnd(start: ContinuingTransition2<out LinearState, out LinearState>): ContinuingTransition2<out LinearState, out LinearState> {
            return inOrder(start).last()
        }


        internal fun inOrder(toStartFrom: ContinuingTransition2<out LinearState, out LinearState>): Iterable<ContinuingTransition2<out LinearState, out LinearState>> {
            return Iterable({
                object : Iterator<ContinuingTransition2<out LinearState, out LinearState>> {
                    var next: ContinuingTransition2<out LinearState, out LinearState>? = toStartFrom
                    override fun hasNext(): Boolean {
                        return next != null
                    }

                    override fun next(): ContinuingTransition2<out LinearState, out LinearState> {
                        val current = next;
                        next = next!!.nextStage;
                        return current!!
                    }
                }
            });
        }


        internal fun <T : LinearState> reversedOrder(toStartFrom: ContinuingTransition2<in LinearState, in LinearState>,
                                                     toEndAt: ContinuingTransition2<in LinearState, in LinearState>?): Iterable<ContinuingTransition2<out LinearState, out LinearState>> {
            return Iterable({
                object : Iterator<ContinuingTransition2<out LinearState, out LinearState>> {
                    var previous: ContinuingTransition2<out LinearState, out LinearState>? = toStartFrom;
                    override fun hasNext(): Boolean {
                        return previous != null && previous != toEndAt
                    }

                    override fun next(): ContinuingTransition2<out LinearState, out LinearState> {
                        val current = previous;
                        previous = previous!!.previousState;
                        return current!!;
                    }
                }
            });
        }
    }

    fun printOutFlow(): GeneratedWorkFlowMultiState {
        var next: ContinuingTransition2<out LinearState, out LinearState>? = start;
        var previous: ContinuingTransition2<out LinearState, out LinearState>? = null
        while (next != null) {
            println(previous?.stageName + "[" + previous?.thisClass?.simpleName + "] -> "
                    + next.stageName + "[" + next.thisClass.simpleName + "] must not be null: "
                    + next.mustNotBeNullInStage.map { it ->
                next?.thisClass?.simpleName + "::" + it.name
            }
                    + " authorised by " + next.thisClass.simpleName + "::" + next.partyAllowedToTransition?.name)
            previous = next
            next = next.nextStage
        }
        return this;
    }

    fun generate() {

        val file = FileSpec.builder("com.r3.workflows.generated", "Output")


        //step 1 - divide all the various stages and states into class specific lanes
        val laneHolder = HashMap<KClass<*>, MutableList<ContinuingTransition2<*, *>>>()
        Companion.inOrder(start).forEach { stage ->
            laneHolder.computeIfAbsent(stage.thisClass, { _ -> ArrayList() }).add(stage);
        }

        laneHolder.forEach({ (kclass, stages) ->
            generateWhatStageChecks(file, kclass, stages)
        })

        file.build().writeTo(System.out)

    }

    private fun generateCheckClassName(kclass: KClass<*>): String {
        return kclass.simpleName + "WorkflowChecks";
    }

    private fun generateEnumCollectiveName(kclass: KClass<*>): String {
        return kclass.simpleName + "WorkflowStages";
    }

    private fun generateEnumConstantName(stage: ContinuingTransition2<out LinearState, out LinearState>): String {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, stage.stageName)
    }

    private fun generateWhatStageChecks(file: FileSpec.Builder, kclass: KClass<*>, stages: MutableList<ContinuingTransition2<*, *>>) {
        val className = generateCheckClassName(kclass);
        val classBuilder = TypeSpec.classBuilder(className)
        generateStageEnums(kclass, stages, classBuilder)
        val endOfClassStageList = stages.last()


        for (stage in stages) {
            generateIsStageCheck(stage, stages, endOfClassStageList, classBuilder)
        }

        generateGetStage(kclass, stages, classBuilder)

        file.addType(classBuilder.build());
    }

    private fun generateGetStage(kclass: KClass<*>, stages: MutableList<ContinuingTransition2<*, *>>, classBuilder: TypeSpec.Builder) {
        val functionBuilder = FunSpec.builder("getStage").addParameter("toCheck", kclass)

        stages.constrainedReverse(stages.last(), null).forEach { stage ->
            functionBuilder.beginControlFlow("if (" + buildStageCheckName(stage) + "(toCheck))")
            functionBuilder.addStatement("return " + buildFullEnumName(stage))
            functionBuilder.endControlFlow()
        }

        functionBuilder.returns(ClassName.bestGuess(generateEnumCollectiveName(kclass)))
        functionBuilder.addStatement("throw %T()", IllegalStateException::class)
        classBuilder.addFunction(functionBuilder.build())

    }

    private fun buildFullEnumName(stage: ContinuingTransition2<*, *>): Any? {
        return generateEnumCollectiveName(stage.thisClass) + "." + generateEnumConstantName(stage)
    }

    private fun generateIsStageCheck(stage: ContinuingTransition2<*, *>, stages: MutableList<ContinuingTransition2<*, *>>, endOfClassStageList: ContinuingTransition2<*, *>, classBuilder: TypeSpec.Builder) {
        val checkBuilder = FunSpec.builder(buildStageCheckName(stage))
        checkBuilder.addParameter("toCheck", stage.thisClass)
        checkBuilder.returns(Boolean::class)

        stage.mustNotBeNullInStage.forEach { nonNullProperty ->
            checkBuilder.beginControlFlow("if (%T::%L.get(toCheck) == null)", stage.thisClass, nonNullProperty.name)
                    .addStatement("return false")
                    .endControlFlow()
        }

        val otherStatesCheck = stages.constrainedReverse(endOfClassStageList, stage).joinToString(separator = "\n&& ", transform = { otherStage ->
            "!" + buildStageCheckName(otherStage) + "(toCheck)"
        })

        checkBuilder.addStatement("return " + if (otherStatesCheck.isEmpty()) "true" else otherStatesCheck);
        classBuilder.addFunction(checkBuilder.build())
    }

    private fun generateStageEnums(kclass: KClass<*>, stages: MutableList<ContinuingTransition2<*, *>>, classBuilder: TypeSpec.Builder) {
        val enums = TypeSpec.enumBuilder(generateEnumCollectiveName(kclass));
        for (stage in stages) {
            enums.addEnumConstant(generateEnumConstantName(stage))
        }
        classBuilder.addType(enums.build());
    }
}


private fun buildStageCheckName(it: ContinuingTransition2<*, *>) =
        "isInStage" + it.stageName.capitalize()

private fun <E> Iterable<E>.constrainedReverse(toStartFrom: E, toStopAt: E?): Iterable<E> {
    val reversedIterator = this.reversed().iterator()

    while (reversedIterator.hasNext() && reversedIterator.next() != toStartFrom) {
    }
    return Iterable({
        object : Iterator<E> {
            var toReturn: E? = toStartFrom;
            override fun hasNext(): Boolean {

                if (toStartFrom === null) {
                    return reversedIterator.hasNext();
                }

                return toReturn != null && toReturn != toStopAt
            }

            override fun next(): E {
                val copied = toReturn;
                toReturn = if (reversedIterator.hasNext()) reversedIterator.next() else null;
                return copied!!;
            }
        }
    })

}
