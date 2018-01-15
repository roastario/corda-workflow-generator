package com.stefano.corda.workflow

import com.stefano.corda.workflow.ContinuingTransition.Companion.wrap
import com.stefano.corda.workflow.stages.AttachFileStage
import com.stefano.corda.workflow.stages.OpenStage
import com.stefano.corda.workflow.stages.UpdateSingleFieldStage
import com.sun.xml.internal.bind.v2.schemagen.episode.Klass
import net.corda.core.contracts.LinearState
import kotlin.reflect.KClass


fun main(args: Array<String>) {
    val start: ContinuingTransition<LinearState, DemoState> = wrap(DemoState::class)
    val stage1 = start.transition("open", UpdateSingleFieldStage(DemoState::received), DemoState::issuer)
    val stage2 = stage1.transition("fileAttached", AttachFileStage(DemoState::document), DemoState::issuer)
    val stage3 = stage2.transition("documentReviewed", UpdateSingleFieldStage(DemoState::documentReviewed), DemoState::owner)
    val stage4 = stage3.transition("documentPublished", UpdateSingleFieldStage(DemoState::documentPublished), DemoState::owner)

//    val checks = ChecksGenerator("FileUploadAndCheck", start).printOutFlow().generate()

    println(OpenStage<DemoState>(DemoState::class).toFlow("FileUploadAndCheck", start))
    println(Class.forName(DemoState::document::returnType.toString()).kotlin)
}
