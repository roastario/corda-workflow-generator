package com.r3.workflows.generation

import com.r3.workflows.generation.stages.AttachFileStage
import com.r3.workflows.generation.stages.UpdateSingleFieldAndGenerateCashMovementStage
import com.r3.workflows.generation.stages.UpdateSingleFieldStage

fun main(args: Array<String>) {

    val start = ContinuingTransition.Companion.wrap(CarCrashState::class)
    val opened = start.transition("submitted", UpdateSingleFieldStage(CarCrashState::timeProcessed), CarCrashState::insuredBy)
    val investigated = opened.transition("investigated", AttachFileStage(CarCrashState::evidence), CarCrashState::insuredBy)
    val adjudicated = investigated.transition("adjudicated", UpdateSingleFieldStage(CarCrashState::amountAccepted), CarCrashState::insuredBy)
    val completed = adjudicated.transition("payment", UpdateSingleFieldAndGenerateCashMovementStage(
            UpdateSingleFieldStage(CarCrashState::timeSettled),
            CarCrashState::claimant,
            CarCrashState::amountAccepted
    ), CarCrashState::insuredBy)


    GeneratorDemo2.generateAll("CarCrashWorkflow", "com.b3i.insurance.carcrash", start)
}