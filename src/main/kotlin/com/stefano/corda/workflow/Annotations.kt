package com.stefano.corda.workflow

import com.stefano.corda.workflow.stages.AttachFileStage

enum class DemoWorkFlow(){
    OPEN,
    START,
    DOCUMENT_ADDED,
    DOCUMENT_REVIEWED,
    CLOSED
}

fun main(args: Array<String>) {


    val thing = AttachFileStage(OtherState::documentHash)


}