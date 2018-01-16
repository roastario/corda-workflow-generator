package com.r3.workflows.generation

import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.Instant

data class DemoState(val received: Instant? = null,
                     val document: SecureHash? = null,
                     val documentReviewed: Instant? = null,
                     val documentPublished : Instant? = null,
                     val issuer: Party,
                     val owner: Party,
                     override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
    override val participants: List<AbstractParty> get() = listOf()
}