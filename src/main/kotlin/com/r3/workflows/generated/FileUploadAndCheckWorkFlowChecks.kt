package com.r3.workflows.generated

import com.stefano.corda.workflow.DemoState
import java.lang.IllegalStateException
import kotlin.Any
import kotlin.Boolean
import net.corda.core.contracts.LinearState

class FileUploadAndCheckWorkFlowChecks {
    class DemoStateWorkflowChecks {
        enum class DemoStateWorkflowStages {
            START,

            OPEN,

            FILE_ATTACHED,

            DOCUMENT_REVIEWED,

            DOCUMENT_PUBLISHED,

            UNKNOWN
        }
        companion object {
            fun isInStageStart(toCheck: DemoState): Boolean {
                if (DemoState::linearId.get(toCheck) == null) {
                    return false
                }
                return !isInStageDocumentPublished(toCheck)
                        && !isInStageDocumentReviewed(toCheck)
                        && !isInStageFileAttached(toCheck)
                        && !isInStageOpen(toCheck)
            }

            fun isInStageOpen(toCheck: DemoState): Boolean {
                if (DemoState::received.get(toCheck) == null) {
                    return false
                }
                return !isInStageDocumentPublished(toCheck)
                        && !isInStageDocumentReviewed(toCheck)
                        && !isInStageFileAttached(toCheck)
            }

            fun isInStageFileAttached(toCheck: DemoState): Boolean {
                if (DemoState::document.get(toCheck) == null) {
                    return false
                }
                return !isInStageDocumentPublished(toCheck)
                        && !isInStageDocumentReviewed(toCheck)
            }

            fun isInStageDocumentReviewed(toCheck: DemoState): Boolean {
                if (DemoState::documentReviewed.get(toCheck) == null) {
                    return false
                }
                return !isInStageDocumentPublished(toCheck)
            }

            fun isInStageDocumentPublished(toCheck: DemoState): Boolean {
                if (DemoState::documentPublished.get(toCheck) == null) {
                    return false
                }
                return true
            }

            fun getStage(toCheck: DemoState): DemoStateWorkflowStages {
                if (isInStageDocumentPublished(toCheck)) {
                    return DemoStateWorkflowStages.DOCUMENT_PUBLISHED
                }
                if (isInStageDocumentReviewed(toCheck)) {
                    return DemoStateWorkflowStages.DOCUMENT_REVIEWED
                }
                if (isInStageFileAttached(toCheck)) {
                    return DemoStateWorkflowStages.FILE_ATTACHED
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

    companion object {
        fun getStage(toCheck: LinearState): Any = when (toCheck)  {
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
            if (inputStage === DemoStateWorkflowChecks.DemoStateWorkflowStages.DOCUMENT_PUBLISHED) {
            }
            if (inputStage === DemoStateWorkflowChecks.DemoStateWorkflowStages.DOCUMENT_REVIEWED) {
                return outputStage === DemoStateWorkflowChecks.DemoStateWorkflowStages.DOCUMENT_PUBLISHED && DemoState::owner.get(output as DemoState) == transitionToken
            }
            if (inputStage === DemoStateWorkflowChecks.DemoStateWorkflowStages.FILE_ATTACHED) {
                return outputStage === DemoStateWorkflowChecks.DemoStateWorkflowStages.DOCUMENT_REVIEWED && DemoState::owner.get(output as DemoState) == transitionToken
            }
            if (inputStage === DemoStateWorkflowChecks.DemoStateWorkflowStages.OPEN) {
                return outputStage === DemoStateWorkflowChecks.DemoStateWorkflowStages.FILE_ATTACHED && DemoState::issuer.get(output as DemoState) == transitionToken
            }
            if (inputStage === DemoStateWorkflowChecks.DemoStateWorkflowStages.START) {
                return outputStage === DemoStateWorkflowChecks.DemoStateWorkflowStages.OPEN && DemoState::issuer.get(output as DemoState) == transitionToken
            }
            return false
        }
    }
}