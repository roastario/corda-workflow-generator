package com.r3.workflows.generation.demo

import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.finance.AMOUNT
import java.util.*


data class DemoExampleState(val instrumentId: String,
                            val quantity: Double,
                            val seller: Party,
                            val buyer: Party?,
                            val terms: SecureHash?,
                            val price: Double?,
                            val paid: Boolean?): LinearState {

    override val linearId: UniqueIdentifier = UniqueIdentifier(instrumentId+quantity)
    override val participants: List<AbstractParty>
        get() = listOfNotNull(seller, buyer)

    val paymentAmount: Amount<Currency>? = price?.let{ AMOUNT(it, Currency.getInstance("GBP")) }



}