package com.stefano.corda.deposits

import com.r3.workflows.generated.canTransition
import com.stefano.corda.deposits.ContinuingTransition.Companion.wrap
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

data class DemoState(val A: Boolean? = null,
                     val B: Boolean? = null,
                     val C: Boolean? = null,
                     val D: Boolean? = null,
                     val issuer: String,
                     val owner: String,
                     override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
    override val participants: List<AbstractParty> get() = listOf()
}



fun main(args: Array<String>) {
    val start: ContinuingTransition<LinearState, DemoState> = wrap(DemoState::class)
    val stage1 = start.transitionUsingSameState("open", DemoState::A, DemoState::issuer)
    val stage2 = stage1.transitionUsingSameState("waitingForFunding", DemoState::B, DemoState::owner)
    val stage3 = stage2.transitionUsingNewState(OtherState::class, "waitingForInstrumentInfo", OtherState::name, OtherState::market)
    val stage4 = stage3.transitionUsingNewState(DemoState::class, "waitingForSignOf", DemoState::C, DemoState::issuer)
    val stage5 = stage4.transitionUsingNewState(OtherState::class, "closed", OtherState::age, OtherState::market)
    GeneratedWorkFlowMultiState(start).printOutFlow().generate();


    val demoOpen = DemoState(A = true, issuer = "HSBC", owner = "RBS")
    val demoWaitingForFunding = DemoState(A = true, B = true, issuer = "HSBC", owner = "RBS")
    val demoWaitingForSignOf = DemoState(A = true, B = true, C = true, issuer = "HSBC", owner = "RBS")

    val demoWaitingForInstrumentInfo = OtherState(name = "ObscureOption1", market = "LSE")
    val demoClosed = OtherState(name = "ObscureOption1", age = 10, market = "LSE")



    println(canTransition(demoOpen, demoWaitingForFunding, "NYSE")) //NO
    println(canTransition(demoOpen, demoWaitingForFunding, "RBS")) //YES
    println(canTransition(demoWaitingForFunding, demoWaitingForFunding, "NYSE")) //NO
    println(canTransition(demoWaitingForFunding, demoWaitingForInstrumentInfo, "LSE")) //YES

    println(canTransition(demoWaitingForSignOf, demoClosed, "NYSE")) //NO
    println(canTransition(demoWaitingForSignOf, demoClosed, "LSE")) //YES
    println(canTransition(demoClosed, demoOpen, "LSE")) //NO
}