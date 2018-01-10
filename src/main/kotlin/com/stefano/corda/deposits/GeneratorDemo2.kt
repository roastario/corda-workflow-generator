package com.stefano.corda.deposits

import com.stefano.corda.deposits.ContinuingTransition2.Companion.wrap
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty

data class OtherState(val name: String? = null,
                      val age: Int? = null,
                      val market: String,
                      override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
    override val participants: List<AbstractParty> get() = listOf()
}


fun main(args: Array<String>) {
    val start: ContinuingTransition2<LinearState, DemoState> = wrap(DemoState::class)
    val stage1 = start.transitionUsingSameState("open", DemoState::A, DemoState::issuer)
    val stage2 = stage1.transitionUsingSameState("waitingForFunding", DemoState::B, DemoState::owner)
    val stage3 = stage2.transitionUsingSameState("waitingForSignOf", DemoState::C, DemoState::issuer)
    val stage4 = stage3.transitionUsingNewState(OtherState::class, "closed", OtherState::name, OtherState::market)
    GeneratedWorkFlowMultiState(start).printOutFlow().generate();
}