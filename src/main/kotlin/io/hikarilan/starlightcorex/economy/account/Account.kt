package io.hikarilan.starlightcorex.economy.account

import io.hikarilan.starlightcorex.economy.currency.Currency
import io.hikarilan.starlightcorex.economy.result.HoldingsActionResult
import io.hikarilan.starlightcorex.economy.result.HoldingsMoveActionResult
import io.hikarilan.starlightcorex.generic.Unique
import io.hikarilan.starlightcorex.generic.source.ActionSource
import org.apache.commons.lang.math.DoubleRange
import java.math.BigDecimal

/**
 * 账户对象
 *
 * **这些操作应当平台无关，且允许异步执行**
 */
interface Account : Unique {

    val name: String

    val holder: AccountHolder

    val holdingRange: ClosedFloatingPointRange<Double>
        get() = 0.0..Double.MAX_VALUE

    fun holding(currency: Currency): BigDecimal

    fun has(currency: Currency, amount: BigDecimal): Boolean

    fun holdings(): Map<Currency, BigDecimal>

    // 储蓄
    fun deposit(currency: Currency, amount: BigDecimal, source: AccountActionSource): HoldingsActionResult

    // 取款
    fun withdraw(currency: Currency, amount: BigDecimal, source: AccountActionSource): HoldingsActionResult

    // 转账
    fun transfer(
        receiving: Account,
        currency: Currency,
        amount: BigDecimal,
        source: AccountActionSource
    ): HoldingsMoveActionResult
}

interface AccountActionSource : ActionSource {

    val operator: AccountOperator

}