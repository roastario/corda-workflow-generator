package com.r3.workflows.generated

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.LinearState
import net.corda.core.transactions.LedgerTransaction
import java.lang.IllegalStateException

class FileUploadAndCheckWorkflowContract : Contract {
  override fun verify(tx: LedgerTransaction) {
    val input = tx.inputsOfType<LinearState>().first()
    val output = tx.outputsOfType<LinearState>().first()
    val validTransition = FileUploadAndCheckWorkFlowChecks.canTransition(input, output)
    if (!validTransition) {
      val inputStage = FileUploadAndCheckWorkFlowChecks.getStage(input)
      val outputStage = FileUploadAndCheckWorkFlowChecks.getStage(output)
      throw IllegalStateException("cannot transition from: $inputStage to: $outputStage")
    }
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
