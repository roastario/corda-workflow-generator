package com.r3.workflows.generated

import com.stefano.corda.deposits.DemoState
import com.stefano.corda.deposits.OtherState
import java.lang.IllegalStateException
import kotlin.Any
import kotlin.Boolean
import net.corda.core.contracts.LinearState

class OtherStateWorkflowChecks {
    enum class OtherStateWorkflowStages {
        WAITING_FOR_INSTRUMENT_INFO,

        CLOSED,

        UNKNOWN
    }
    companion object {
        fun isInStageWaitingForInstrumentInfo(toCheck: OtherState): Boolean {
            if (OtherState::name.get(toCheck) == null) {
                return false
            }
            return !isInStageClosed(toCheck)
        }

        fun isInStageClosed(toCheck: OtherState): Boolean {
            if (OtherState::age.get(toCheck) == null) {
                return false
            }
            return true
        }

        fun getStage(toCheck: OtherState): OtherStateWorkflowStages {
            if (isInStageClosed(toCheck)) {
                return OtherStateWorkflowStages.CLOSED
            }
            if (isInStageWaitingForInstrumentInfo(toCheck)) {
                return OtherStateWorkflowStages.WAITING_FOR_INSTRUMENT_INFO
            }
            return OtherStateWorkflowStages.UNKNOWN
        }
    }
}

class DemoStateWorkflowChecks {
    enum class DemoStateWorkflowStages {
        START,

        OPEN,

        WAITING_FOR_FUNDING,

        WAITING_FOR_SIGN_OF,

        UNKNOWN
    }
    companion object {
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
            return DemoStateWorkflowStages.UNKNOWN
        }
    }
}

fun getStage(toCheck: LinearState): Any = when (toCheck)  {
    is OtherState -> OtherStateWorkflowChecks.getStage(toCheck)
    is DemoState -> DemoStateWorkflowChecks.getStage(toCheck)
    else -> throw IllegalStateException()
}

fun canTransition(
        input: LinearState,
        output: LinearState,
        transitionToken: Any?
): Boolean {
    val inputStage = getStage(input)
    val outputStage = getStage(output)
    if (inputStage === OtherStateWorkflowChecks.OtherStateWorkflowStages.CLOSED) {
    }
    if (inputStage === DemoStateWorkflowChecks.DemoStateWorkflowStages.WAITING_FOR_SIGN_OF) {
        return outputStage === OtherStateWorkflowChecks.OtherStateWorkflowStages.CLOSED && OtherState::market.get(output as OtherState) == transitionToken
    }
    if (inputStage === OtherStateWorkflowChecks.OtherStateWorkflowStages.WAITING_FOR_INSTRUMENT_INFO) {
        return outputStage === DemoStateWorkflowChecks.DemoStateWorkflowStages.WAITING_FOR_SIGN_OF && DemoState::issuer.get(output as DemoState) == transitionToken
    }
    if (inputStage === DemoStateWorkflowChecks.DemoStateWorkflowStages.WAITING_FOR_FUNDING) {
        return outputStage === OtherStateWorkflowChecks.OtherStateWorkflowStages.WAITING_FOR_INSTRUMENT_INFO && OtherState::market.get(output as OtherState) == transitionToken
    }
    if (inputStage === DemoStateWorkflowChecks.DemoStateWorkflowStages.OPEN) {
        return outputStage === DemoStateWorkflowChecks.DemoStateWorkflowStages.WAITING_FOR_FUNDING && DemoState::owner.get(output as DemoState) == transitionToken
    }
    if (inputStage === DemoStateWorkflowChecks.DemoStateWorkflowStages.START) {
        return outputStage === DemoStateWorkflowChecks.DemoStateWorkflowStages.OPEN && DemoState::issuer.get(output as DemoState) == transitionToken
    }
    return false
}