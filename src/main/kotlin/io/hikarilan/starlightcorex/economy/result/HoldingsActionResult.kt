package io.hikarilan.starlightcorex.economy.result

import io.hikarilan.starlightcorex.economy.account.Account
import io.hikarilan.starlightcorex.economy.currency.Currency
import io.hikarilan.starlightcorex.generic.result.Response
import java.math.BigDecimal

interface HoldingsActionResult : EconomyResponse {

    val actionType: String

    val currency: Currency

    val holding: BigDecimal

    val result: Response

}

data class HoldingsActionResultImpl(
    override val actionType: String,
    override val currency: Currency,
    override val account: Account,
    override val holding: BigDecimal = account.holding(currency),
    override val result: Response,
    override val isSuccess: Boolean = result.isSuccess,
    override val reason: String = result.reason
) : HoldingsActionResult