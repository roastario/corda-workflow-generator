package com.r3.workflows.generated

import com.stefano.corda.deposits.DemoState
import com.stefano.corda.deposits.OtherState
import java.lang.IllegalStateException
import kotlin.Boolean

class OtherStateWorkflowChecks {
    fun isInStageClosed(toCheck: OtherState): Boolean {
        if (OtherState::name.get(toCheck) == null) {
            return false
        }
        return true
    }

    fun getStage(toCheck: OtherState): OtherStateWorkflowStages {
        if (isInStageClosed(toCheck)) {
            return OtherStateWorkflowStages.CLOSED
        }
        throw IllegalStateException()
    }

    enum class OtherStateWorkflowStages {
        CLOSED
    }
}

class DemoStateWorkflowChecks {
    fun isInStageStart(toCheck: DemoState): Boolean = !isInStageWaitingForSignOf(toCheck)
            && !isInStageWaitingForFunding(toCheck)
            && !isInStageOpen(toCheck)

    fun isInStageOpen(toCheck: DemoState): Boolean {
        if (DemoState::A.get(toCheck) == null) {
            return false
        }
        return !isInStageWaitingForSignOf(toCheck)
                && !isInStageWaitingForFunding(toCheck)
    }

    fun isInStageWaitingForFunding(toCheck: DemoState): Boolean {
        if (DemoState::B.get(toCheck) == null) {
            return false
        }
        return !isInStageWaitingForSignOf(toCheck)
    }

    fun isInStageWaitingForSignOf(toCheck: DemoState): Boolean {
        if (DemoState::C.get(toCheck) == null) {
            return false
        }
        return true
    }

    fun getStage(toCheck: DemoState): DemoStateWorkflowStages {
        if (isInStageWaitingForSignOf(toCheck)) {
            return DemoStateWorkflowStages.WAITING_FOR_SIGN_OF
        }
        if (isInStageWaitingForFunding(toCheck)) {
            return DemoStateWorkflowStages.WAITING_FOR_FUNDING
        }
        if (isInStageOpen(toCheck)) {
            return DemoStateWorkflowStages.OPEN
        }
        if (isInStageStart(toCheck)) {
            return DemoStateWorkflowStages.START
        }
        throw IllegalStateException()
    }

    enum class DemoStateWorkflowStages {
        START,

        OPEN,

        WAITING_FOR_FUNDING,

        WAITING_FOR_SIGN_OF
    }
}