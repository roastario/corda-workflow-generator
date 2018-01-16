package com.stefano.corda.workflow

import com.squareup.kotlinpoet.FileSpec
import com.stefano.corda.workflow.ContinuingTransition.Companion.wrap
import com.stefano.corda.workflow.stages.AttachFileStage
import com.stefano.corda.workflow.stages.UpdateSingleFieldStage
import net.corda.core.contracts.LinearState
import kotlin.reflect.jvm.internal.impl.util.Checks


fun main(args: Array<String>) {
    val start: ContinuingTransition<LinearState, DemoState> = wrap(DemoState::class)
    val stage1 = start.transition("open", UpdateSingleFieldStage(DemoState::received), DemoState::issuer)
    val stage2 = stage1.transition("fileAttached", AttachFileStage(DemoState::document), DemoState::issuer)
    val stage3 = stage2.transition("documentReviewed", UpdateSingleFieldStage(DemoState::documentReviewed), DemoState::owner)
    val stage4 = stage3.transition("documentPublished", UpdateSingleFieldStage(DemoState::documentPublished), DemoState::owner)

//    val checks = ChecksGenerator("FileUploadAndCheck", start).printOutFlow().generate()

    val generatedChecks = ChecksGenerator("FileUploadAndCheck", start).generate()

    FileSpec.builder("com.r3.workflows.generated", "Checks").addType(generatedChecks.build()).build().writeTo(System.out)
    UpdateSingleFieldStage(DemoState::documentReviewed).toFlow("FileUploadAndCheck", stage3).writeTo(System.out)
}
