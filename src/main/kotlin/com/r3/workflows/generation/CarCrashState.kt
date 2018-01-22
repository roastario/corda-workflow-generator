package com.r3.workflows.generation

import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.Instant
import java.util.*

data class CarCrashState(val insuredBy: Party,
                         val amountClaimed: Amount<Currency>,
                         val claimant: Party,
                         val timeProcessed: Instant?,
                         val evidence: SecureHash?,
                         val amountAccepted: Amount<Currency>?,
                         val timeSettled: Instant?,
                         override val linearId: UniqueIdentifier = UniqueIdentifier(),
                         override val participants: List<AbstractParty> = listOf(insuredBy)) : LinearState {
}

