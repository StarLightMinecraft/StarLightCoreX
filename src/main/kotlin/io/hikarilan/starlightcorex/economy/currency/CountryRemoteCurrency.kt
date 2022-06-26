package io.hikarilan.starlightcorex.economy.currency

import io.hikarilan.starlightcorex.storage.getStorageFor
import java.math.BigDecimal
import java.util.*

data class CountryRemoteCurrency(
    override val uniqueId: UUID
) : RemoteCurrency {

    override val linkedCurrency: LocalCurrency
        get() = getStorageFor<LocalCurrency>(uniqueId) ?: throw IllegalStateException("Currency not found")

    override var name: String
        get() = linkedCurrency.name
        set(_) {}

    override var symbol: String
        get() = linkedCurrency.symbol
        set(_) {}

    override fun format(amount: BigDecimal): String = linkedCurrency.format(amount)


}