package com.r3.workflows.generation

import com.r3.workflows.generation.demo.DemoExampleState
import com.r3.workflows.generation.stages.AttachFileStage
import com.r3.workflows.generation.stages.UpdateSingleFieldAndGenerateCashMovementStage
import com.r3.workflows.generation.stages.UpdateSingleFieldStage

fun main(args: Array<String>) {
    val start = ContinuingTransition.wrap(DemoExampleState::class)

    start.transition("offered",
            UpdateSingleFieldStage(DemoExampleState::buyer),
            DemoExampleState::seller
    ).transition("termsAttached",
            AttachFileStage(DemoExampleState::terms),
            DemoExampleState::buyer
    ).transition("priced",
            UpdateSingleFieldStage(DemoExampleState::price),
            DemoExampleState::seller
    ).transition("complete",
            UpdateSingleFieldAndGenerateCashMovementStage(
                    UpdateSingleFieldStage(DemoExampleState::paid),
                    DemoExampleState::seller,
                    DemoExampleState::paymentAmount),
            DemoExampleState::buyer)

    Generator.generateAll("DemoExample", "com.r3.states.demo", start)
}