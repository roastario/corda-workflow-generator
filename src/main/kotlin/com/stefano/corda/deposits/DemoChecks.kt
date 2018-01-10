package com.stefano.corda.deposits

import java.lang.IllegalStateException
import kotlin.Boolean

enum class DemoStateStages {
    START,

    OPEN,

    WAITING_FOR_FUNDING,

    WAITING_FOR_SIGN_OF,

    CLOSED
}

fun isInStageOpen(toCheck: DemoState): Boolean {
    if (DemoState::A.get(toCheck) == null) {
        return false
    }
    return !isInStageClosed(toCheck)
            && !isInStageWaitingForSignOf(toCheck)
            && !isInStageWaitingForFunding(toCheck)
}

fun isInStageWaitingForFunding(toCheck: DemoState): Boolean {
    if (DemoState::B.get(toCheck) == null) {
        return false
    }
    if (DemoState::A.get(toCheck) == null) {
        return false
    }
    return !isInStageClosed(toCheck)
            && !isInStageWaitingForSignOf(toCheck)
}

fun isInStageWaitingForSignOf(toCheck: DemoState): Boolean {
    if (DemoState::B.get(toCheck) == null) {
        return false
    }
    if (DemoState::A.get(toCheck) == null) {
        return false
    }
    if (DemoState::C.get(toCheck) == null) {
        return false
    }
    return !isInStageClosed(toCheck)
}

fun isInStageClosed(toCheck: DemoState): Boolean {
    if (DemoState::B.get(toCheck) == null) {
        return false
    }
    if (DemoState::D.get(toCheck) == null) {
        return false
    }
    if (DemoState::A.get(toCheck) == null) {
        return false
    }
    if (DemoState::C.get(toCheck) == null) {
        return false
    }
    return true
}

fun getStage(toCheck: DemoState): DemoStateStages {
    if (isInStageClosed(toCheck)) {
        return DemoStateStages.CLOSED
    }
    if (isInStageWaitingForSignOf(toCheck)) {
        return DemoStateStages.WAITING_FOR_SIGN_OF
    }
    if (isInStageWaitingForFunding(toCheck)) {
        return DemoStateStages.WAITING_FOR_FUNDING
    }
    if (isInStageOpen(toCheck)) {
        return DemoStateStages.OPEN
    }
    throw IllegalStateException()
}

fun canTransition(
        input: DemoState,
        output: DemoState,
        transitionToken: Any?
): Boolean {
    if (getStage(input) === DemoStateStages.CLOSED) {
    }
    if (getStage(input) === DemoStateStages.WAITING_FOR_SIGN_OF) {
        return getStage(output) === DemoStateStages.CLOSED
                && DemoState::issuer.get(input).equals(transitionToken);
    }
    if (getStage(input) === DemoStateStages.WAITING_FOR_FUNDING) {
        return getStage(output) === DemoStateStages.WAITING_FOR_SIGN_OF
                && DemoState::issuer.get(input).equals(transitionToken);
    }
    if (getStage(input) === DemoStateStages.OPEN) {
        return getStage(output) === DemoStateStages.WAITING_FOR_FUNDING
                && DemoState::owner.get(input).equals(transitionToken);
    }
    if (getStage(input) === DemoStateStages.START) {
        return getStage(output) === DemoStateStages.OPEN
                && DemoState::issuer.get(input).equals(transitionToken);
    }
    throw IllegalStateException()
}