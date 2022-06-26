package io.hikarilan.starlightcorex.economy.currency

import io.hikarilan.starlightcorex.utils.currencyFormatter
import java.math.BigDecimal
import java.util.*

data class CountryLocalCurrency(
    override var name: String,
    override var symbol: String,
    override val uniqueId: UUID = UUID.randomUUID()
) : LocalCurrency {

    override fun format(amount: BigDecimal): String {
        return currencyFormatter.format(amount)
    }
}