package com.r3.workflows.generation

import com.r3.workflows.generation.ContinuingTransition.Companion.wrap
import com.r3.workflows.generation.GeneratorDemo2.Companion.generateAll
import com.r3.workflows.generation.generators.ChecksGenerator
import com.r3.workflows.generation.generators.ContractGenerator
import com.r3.workflows.generation.generators.FlowGenerator
import com.r3.workflows.generation.generators.Generator
import com.r3.workflows.generation.stages.AttachFileStage
import com.r3.workflows.generation.stages.UpdateSingleFieldStage
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import info.leadinglight.jdot.Edge
import info.leadinglight.jdot.Graph
import info.leadinglight.jdot.enums.GraphType
import net.corda.core.contracts.LinearState
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.util.stream.IntStream
import kotlin.streams.asSequence


class GeneratorDemo2 {
    companion object {
        fun generateAll(workflowName: String, packageOfOutput: String, start: ContinuingTransition<out LinearState, out LinearState>) {
            val generatedChecks = ChecksGenerator(workflowName, start).generate<TypeSpec.Builder>().build()
            val generatedFlows = FlowGenerator(workflowName, start).generate<TypeSpec.Builder>().build()
            val generatedContract = ContractGenerator(workflowName, start).generate<TypeSpec.Builder>().build()
            val generatedDot = DotGenerator(workflowName, start).generate<Graph>();

            val targetDirectory = System.getProperty("user.dir") + "/src/main/kotlin"
            val targetFile = File(targetDirectory)
            targetFile.mkdirs()
            listOf(generatedChecks, generatedContract, generatedFlows).forEach { generated ->

                FileSpec.builder(packageOfOutput, generated.name!!)
                        .addType(generated).build().writeTo(targetFile)
            }
            writeDotToSvg(generatedDot, workflowName)
        }


        private fun writeDotToSvg(generatedDot: Graph, workflowName: String) {
            val svg = generatedDot.dot2svg("/usr/bin/dot")

            val dotDirectory = System.getProperty("user.dir") + "/src/main/dot"
            File(dotDirectory).mkdir()

            val svgOutputStream = FileOutputStream("$dotDirectory/$workflowName.svg")
            svgOutputStream.write(svg.toByteArray(Charset.forName("UTF-8")))
            svgOutputStream.close()
        }

        class DotGenerator(s: String, start: ContinuingTransition<out LinearState, out LinearState>) : Generator(s, start) {
            override fun <T : Any> generate(): T {
                val nodeList = inOrder(start).map { it.stageDescription.toDot(workflowName, it) }.toList().toTypedArray()

                val edges = IntStream.range(1, nodeList.size).asSequence().map { idx ->
                    val previous = nodeList.get(idx - 1);
                    val current = nodeList.get(idx)
                    Edge(previous.name, current.name).setLabel("   " + current.name)
                }.toList().toTypedArray()


                val g = Graph(workflowName.capitalize() + "Graph").setType(GraphType.digraph)
                g.addNodes(*nodeList)
                g.addEdges(*edges)
                return g as T;
            }

        }

    }
}


fun main(args: Array<String>) {

    val start: ContinuingTransition<LinearState, DemoState> = wrap(DemoState::class)
    val stage1 = start.transition("open", UpdateSingleFieldStage(DemoState::received), DemoState::issuer)
    val stage2 = stage1.transition("fileAttached", AttachFileStage(DemoState::document), DemoState::issuer)
    val stage3 = stage2.transition("documentReviewed", UpdateSingleFieldStage(DemoState::documentReviewed), DemoState::owner)
    val stage4 = stage3.transition("documentPublished", UpdateSingleFieldStage(DemoState::documentPublished), DemoState::owner)


    generateAll("FileUploadAndCheck", "com.r3.workflows.generated", start)

}

