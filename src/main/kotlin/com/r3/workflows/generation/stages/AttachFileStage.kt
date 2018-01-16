package com.r3.workflows.generation.stages

import co.paralleluniverse.fibers.Suspendable
import com.r3.workflows.generation.ContinuingTransition
import com.r3.workflows.generation.stages.Stage.Companion.getContractNameForWorkflow
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import info.leadinglight.jdot.Node
import info.leadinglight.jdot.enums.Shape
import net.corda.core.contracts.Command
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.reflect.KProperty1

class AttachFileStage<T : LinearState>(private val property: KProperty1<T, SecureHash?>) : Stage<T> {


    override fun toDot(workflowName: String, transition: ContinuingTransition<*, *>): Node {
        return Node(transition.stageName.capitalize()).setShape(Shape.rect)
                .setLabel("Attaches file to: " + transition.thisClass.simpleName + "::" + property.name)
    }


    override fun toFlow(workflowName: String, transition: ContinuingTransition<*, *>): TypeSpec {
        val flowObject = TypeSpec.objectBuilder(transition.stageName.capitalize() + "Flow");

        val initiatorBuilder = buildInitiatorForUpdateType(
                "attachment" to InputStream::class,
                "identifier" to UniqueIdentifier::class,
                "toSendTo" to Party::class)

        initiatorBuilder.addFunction(buildLookup(transition.thisClass).build())
        initiatorBuilder.addFunction(buildAttachmentUploadFunction().build())


        val callBuilder = FunSpec.builder("call").returns(UniqueIdentifier::class).addModifiers(KModifier.OVERRIDE).addAnnotation(Suspendable::class)
        callBuilder.addStatement("val notary : %T = serviceHub.networkMapCache.notaryIdentities[0]", Party::class)
        callBuilder.addStatement("val ourKey = serviceHub.myInfo.legalIdentities.map { it.owningKey }.first()")
        callBuilder.addStatement("val command = %T(%T.Commands.%L(), listOf(ourKey, toSendTo.owningKey))",
                Command::class,
                ClassName.bestGuess(getContractNameForWorkflow(workflowName)),
                transition.stageName.capitalize())

        callBuilder.addStatement("val inputRefAndState = loadInput(identifier)")
        callBuilder.addStatement("val input = inputRefAndState.state.data")
        callBuilder.addStatement("val attachmentHash = uploadAttachment(attachment)")
        callBuilder.addStatement("val output = input.copy(%L=attachmentHash)", property.name)

        callBuilder.addStatement("val proposedTransaction: %T = %T(notary)\n" +
                ".addInputState(inputRefAndState)\n" +
                ".addOutputState(output, %L.CONTRACT_ID)\n" +
                ".addCommand(command)",
                TransactionBuilder::class,
                TransactionBuilder::class,
                getContractNameForWorkflow(workflowName)
        )


        callBuilder.addStatement("proposedTransaction.verify(serviceHub)")
        callBuilder.addStatement("val ourSignedTransaction = serviceHub.signInitialTransaction(proposedTransaction)")
        callBuilder.addStatement("val counterPartySession = initiateFlow(toSendTo)")
        callBuilder.addStatement("val fullySignedTx = subFlow( %T(ourSignedTransaction, setOf(counterPartySession)) )", CollectSignaturesFlow::class)
        callBuilder.addStatement("val committedTransaction = subFlow(%T(fullySignedTx))", FinalityFlow::class)
        callBuilder.addStatement("return committedTransaction.tx.outputsOfType(%T::class.java).first().linearId", transition.thisClass)



        initiatorBuilder.addFunction(callBuilder.build())
        flowObject.addType(initiatorBuilder.build())
        buildDumbResponderFlow(flowObject)

        return flowObject.build();
    }

    override fun property(): KProperty1<T, SecureHash?> {
        return property;
    }


    private fun buildAttachmentUploadFunction(): FunSpec.Builder {


        return FunSpec.builder("uploadAttachment")
                .addParameter("attachment", InputStream::class)
                .addModifiers(KModifier.PRIVATE, KModifier.INLINE)
                .returns(SecureHash::class)
                .addStatement("val outputStream = %T()", ByteArrayOutputStream::class)
                .beginControlFlow("try ")
                .addStatement("val zippedOutputStream = %T(outputStream)", ZipOutputStream::class)
                .addStatement("zippedOutputStream.putNextEntry(%T(\"%L\"))", ZipEntry::class, "file")
                .addStatement("attachment.copyTo(zippedOutputStream)")
                .addStatement("zippedOutputStream.closeEntry()")
                .addStatement("zippedOutputStream.flush()")
                .addStatement("zippedOutputStream.close()")
                .addStatement("return serviceHub.attachments.importAttachment(%T(outputStream.toByteArray()))", ByteArrayInputStream::class)
                .nextControlFlow("finally")
                .addStatement("outputStream.close()")
                .endControlFlow()
    }


}