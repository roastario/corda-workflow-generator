package com.stefano.corda.deposits

import com.stefano.corda.deposits.ContinuingTransition.Companion.wrap
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty

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
    val start = wrap(DemoState::class)
    val stage1 = start.transition("open", DemoState::A, DemoState::issuer)
    val stage2 = stage1.transition("waitingForFunding", DemoState::B, DemoState::owner)
    val stage3 = stage2.transition("waitingForSignOf", DemoState::C, DemoState::issuer)
    val stage4 = stage3.transition("closed", DemoState::D, DemoState::issuer)
    val demoOpen = DemoState(A = true, issuer = "issuer", owner = "owner")
    val demoWaitingForFunding = DemoState(A = true, B = true, issuer = "issuer", owner = "owner")
    val demoWaitingForSignOf = DemoState(A = true, B = true, C = true, issuer = "issuer", owner = "owner")
    val demoClosed = DemoState(A = true, B = true, C = true, D = true, issuer = "issuer", owner = "owner")
    println(isInStageOpen(demoOpen)) //true
    println(isInStageOpen(demoWaitingForFunding)) // false
    println(isInStageWaitingForFunding(demoWaitingForSignOf)) // false
    println(isInStageWaitingForFunding(demoWaitingForFunding)) // true
    println(canTransition(demoOpen, demoWaitingForFunding, "issuer")) // false (wrong transition token)
    println(canTransition(demoOpen, demoWaitingForFunding, "owner")) // true
    println(canTransition(demoOpen, demoWaitingForSignOf, "issuer")) // false (wrong output stage)
    println(canTransition(demoWaitingForSignOf, demoClosed, "issuer")) // true
    GeneratedWorkFlow(start).printOutFlow().generate()
}