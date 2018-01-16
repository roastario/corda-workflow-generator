package com.r3.workflows.generated

import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData

class FileUploadAndCheckWorkflowContract {
    companion object {
        @JvmStatic
        val CONTRACT_ID = "com.stefano.corda.deposits.DepositContract"

    }


    interface Commands : CommandData {
        class Open : Commands
        class FileAttached : Commands
        class DocumentReviewed : Commands
        class LandlordDeduct : Commands
        class TenantDeduct : Commands
        class RequestRefund : Commands
        class SendBackToTenant : Commands
        class SendBackToLandlord : Commands
        class Refund : Commands
        class SendToArbitrator : Commands {
        }

        class Arbitrate : Commands {
        }
    }
}