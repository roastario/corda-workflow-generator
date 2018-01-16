package com.r3.workflows.generated

import kotlin.String
import kotlin.jvm.JvmStatic
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.LinearState
import net.corda.core.transactions.LedgerTransaction

class FileUploadAndCheckWorkflowContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val input = tx.inputsOfType<LinearState>().first()
        val output = tx.outputsOfType<LinearState>().first()
        val validTransition = FileUploadAndCheckWorkFlowChecks.canTransition(input, output, null)
    }

    companion object {
        @JvmStatic
        val CONTRACT_ID: String = FileUploadAndCheckWorkflowContract::class.qualifiedName!!
    }

    interface Commands : CommandData {
        class Start : Commands

        class Open : Commands

        class FileAttached : Commands

        class DocumentReviewed : Commands

        class DocumentPublished : Commands
    }
}
