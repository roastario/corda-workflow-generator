package com.r3.workflows.generation.generators

import com.r3.workflows.generation.ContinuingTransition
import com.r3.workflows.generation.stages.Stage
import com.squareup.kotlinpoet.*
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.LinearState
import net.corda.core.transactions.LedgerTransaction

class ContractGenerator(workflowName: String, start: ContinuingTransition<out LinearState, out LinearState>)
    : WorkflowGenerator(workflowName, start) {


    override fun <T: Any> generate() : T {
        val contractName = Stage.getContractNameForWorkflow(workflowName)
        val contractBuilder = TypeSpec.classBuilder(contractName).addSuperinterface(Contract::class)
        val commandsBuilder = TypeSpec.interfaceBuilder("Commands").addSuperinterface(CommandData::class)

        val contractIdField = PropertySpec.builder("CONTRACT_ID", String::class)
                .initializer("%L::class.qualifiedName!!", contractName)
                .addAnnotation(JvmStatic::class)

        contractBuilder.addType(TypeSpec.companionObjectBuilder().addProperty(contractIdField.build()).build())

        inOrder(start).forEach { transition ->
            val commandName = transition.stageName.capitalize()
            commandsBuilder.addType(TypeSpec.classBuilder(commandName).addSuperinterface(ClassName.bestGuess("Commands")).build())

        }

        val verifyFunction = FunSpec.builder("verify")
                .addParameter("tx", LedgerTransaction::class)
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("val input = tx.inputsOfType<%T>().first()", LinearState::class)
                .addStatement("val output = tx.outputsOfType<%T>().first()", LinearState::class)
                .addStatement("val validTransition = %T.canTransition(input, output)", ClassName.bestGuess(buildGlobalChecksClassName(workflowName)))
                .beginControlFlow("if (!validTransition)")
                .addStatement("val inputStage = %T.getStage(input)", ClassName.bestGuess(buildGlobalChecksClassName(workflowName)))
                .addStatement("val outputStage = %T.getStage(output)",  ClassName.bestGuess(buildGlobalChecksClassName(workflowName)))
                .addStatement("throw %T(\"cannot transition from: \$inputStage to: \$outputStage\")", IllegalStateException::class)
                .endControlFlow()


        contractBuilder.addFunction(verifyFunction.build())
        contractBuilder.addType(commandsBuilder.build())
        return contractBuilder as T;
    }
}