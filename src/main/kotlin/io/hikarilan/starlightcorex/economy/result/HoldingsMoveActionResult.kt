package io.hikarilan.starlightcorex.economy.result

import io.hikarilan.starlightcorex.economy.account.Account
import io.hikarilan.starlightcorex.economy.currency.Currency
import io.hikarilan.starlightcorex.generic.result.Response
import java.math.BigDecimal

interface HoldingsMoveActionResult : HoldingsActionResult {

    val receivingAccount: Account

    val moving: BigDecimal

}

data class HoldingsMoveActionResultImpl(
    override val actionType: String,
    override val currency: Currency,
    override val account: Account,
    override val holding: BigDecimal = account.holding(currency),
    override val receivingAccount: Account,
    override val moving: BigDecimal,
    override val result: Response,
    override val isSuccess: Boolean = result.isSuccess,
    override val reason: String = result.reason,
) : HoldingsMoveActionResult