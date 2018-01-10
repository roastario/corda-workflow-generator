package com.stefano.corda.deposits

import com.google.common.base.CaseFormat
import com.squareup.kotlinpoet.*
import com.stefano.corda.deposits.GeneratedWorkFlowMultiState.Companion.reversedOrder
import net.corda.core.contracts.LinearState
import kotlin.reflect.KClass

class GeneratedWorkFlowMultiState(val start: ContinuingTransition<out LinearState, out LinearState>,
                                  val end: ContinuingTransition<out LinearState, out LinearState> = Companion.getEnd(start)
) {
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

    fun printOutFlow(): GeneratedWorkFlowMultiState {
        var next: ContinuingTransition<out LinearState, out LinearState>? = start;
        var previous: ContinuingTransition<out LinearState, out LinearState>? = null
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
        val laneHolder = HashMap<KClass<out LinearState>, MutableList<ContinuingTransition<*, *>>>()
        Companion.inOrder(start).forEach { stage ->
            laneHolder.computeIfAbsent(stage.thisClass, { _ -> ArrayList() }).add(stage);
        }
        laneHolder.forEach({ (kclass, stages) ->
            generateWhatStageChecks(file, kclass, stages)
        })
        generateGlobalWhatStage(file, laneHolder.keys)
        generateGlobalCanTransition(file);
        file.build().writeTo(System.out)
    }

    private fun generateCheckClassName(kclass: KClass<*>): String {
        return kclass.simpleName + "WorkflowChecks";
    }

    private fun generateEnumCollectiveName(kclass: KClass<*>): String {
        return kclass.simpleName + "WorkflowStages";
    }

    private fun generateEnumConstantName(stage: ContinuingTransition<out LinearState, out LinearState>): String {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, stage.stageName)
    }

    private fun generateWhatStageChecks(file: FileSpec.Builder, kclass: KClass<*>, stages: MutableList<ContinuingTransition<*, *>>) {
        val className = generateCheckClassName(kclass);
        val classBuilder = TypeSpec.classBuilder(className)
        val companionObject = TypeSpec.companionObjectBuilder();

        generateStageEnums(kclass, stages, classBuilder)
        val endOfClassStageList = stages.last()
        for (stage in stages) {
            generateIsStageCheck(stage, stages, endOfClassStageList, companionObject)
        }
        generateGetStage(kclass, stages, companionObject)
        classBuilder.companionObject(companionObject.build())
        file.addType(classBuilder.build());
    }

    private fun generateGlobalWhatStage(file: FileSpec.Builder, classes: Set<KClass<out LinearState>>) {

        val functionBuilder = FunSpec.builder("getStage")
                .addParameter("toCheck", LinearState::class)
                .returns(Any::class)


        functionBuilder.beginControlFlow("return when (toCheck) ")

        for (kclass in classes) {
            functionBuilder.addStatement("is %T -> %L", kclass, generateCheckClassName(kclass) + "." + "getStage(toCheck)")
        }

        functionBuilder.addStatement("else -> throw %T()", IllegalStateException::class)
        functionBuilder.endControlFlow()
        file.addFunction(functionBuilder.build())
    }

    private fun generateGlobalCanTransition(file: FileSpec.Builder) {

        val functionBuilder = FunSpec.builder("canTransition")
                .addParameter("input", LinearState::class)
                .addParameter("output", LinearState::class)
                .addParameter("transitionToken", ClassName.bestGuess("Any?"))
                .returns(Boolean::class)

        functionBuilder.addStatement("val inputStage = getStage(input)")
        functionBuilder.addStatement("val outputStage = getStage(output)")

        reversedOrder(end, null).forEach {
            functionBuilder.beginControlFlow("if (inputStage === %T.%L)",
                    ClassName.bestGuess(generateCheckClassName(it.thisClass) + '.'
                            + generateEnumCollectiveName(it.thisClass)),
                    generateEnumConstantName(it))

            it.nextStage?.let { nextStage ->
                functionBuilder.addStatement("return outputStage === %T.%L && %T::%L.get(output as %T) == %L",
                        ClassName.bestGuess(generateCheckClassName(nextStage.thisClass) + '.'
                                + generateEnumCollectiveName(nextStage.thisClass)),
                        generateEnumConstantName(nextStage),
                        nextStage.thisClass,
                        nextStage.partyAllowedToTransition!!.name,
                        nextStage.thisClass,
                        "transitionToken"
                )
            }

            functionBuilder.endControlFlow()
        }

        functionBuilder.addStatement("return false")
        file.addFunction(functionBuilder.build())
    }

    private fun generateGetStage(kclass: KClass<*>, stages: MutableList<ContinuingTransition<*, *>>, classBuilder: TypeSpec.Builder) {
        val functionBuilder = FunSpec.builder("getStage").addParameter("toCheck", kclass)

        stages.constrainedReverse(stages.last(), null).forEach { stage ->
            functionBuilder.beginControlFlow("if (" + buildStageCheckName(stage) + "(toCheck))")
            functionBuilder.addStatement("return " + buildFullEnumName(stage))
            functionBuilder.endControlFlow()
        }

        functionBuilder.returns(ClassName.bestGuess(generateEnumCollectiveName(kclass)))
        functionBuilder.addStatement("return " + generateEnumCollectiveName(kclass) + '.' + "UNKNOWN")
        classBuilder.addFunction(functionBuilder.build())

    }

    private fun buildFullEnumName(stage: ContinuingTransition<*, *>): Any? {
        return generateEnumCollectiveName(stage.thisClass) + "." + generateEnumConstantName(stage)
    }

    private fun generateIsStageCheck(stage: ContinuingTransition<*, *>, stages: MutableList<ContinuingTransition<*, *>>, endOfClassStageList: ContinuingTransition<*, *>, classBuilder: TypeSpec.Builder) {
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

    private fun generateStageEnums(kclass: KClass<*>, stages: MutableList<ContinuingTransition<*, *>>, classBuilder: TypeSpec.Builder) {
        val enums = TypeSpec.enumBuilder(generateEnumCollectiveName(kclass));
        for (stage in stages) {
            enums.addEnumConstant(generateEnumConstantName(stage))
        }
        enums.addEnumConstant("UNKNOWN")
        classBuilder.addType(enums.build());
    }
}


private fun buildStageCheckName(it: ContinuingTransition<*, *>) =
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
