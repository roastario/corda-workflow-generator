package com.stefano.corda.workflow

import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty

data class OtherState(val name: String? = null,
                      val age: Int? = null,
                      val documentHash: SecureHash?,
                      val market: String,
                      override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
    override val participants: List<AbstractParty> get() = listOf()
}