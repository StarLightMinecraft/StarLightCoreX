package io.hikarilan.starlightcorex.economy.account

import io.hikarilan.starlightcorex.economy.currency.Currency
import io.hikarilan.starlightcorex.economy.result.*
import io.hikarilan.starlightcorex.generic.result.Success
import io.hikarilan.starlightcorex.generic.result.Unsupported
import io.hikarilan.starlightcorex.storage.getStorageFor
import io.hikarilan.starlightcorex.utils.ComponentUtils
import io.hikarilan.starlightcorex.utils.Mail
import io.hikarilan.starlightcorex.utils.log
import java.math.BigDecimal
import java.util.*
import kotlin.math.absoluteValue

abstract class CommonAccount(
    override val uniqueId: UUID = UUID.randomUUID(),
    private val holdings: MutableMap<UUID, BigDecimal> = mutableMapOf()
) : GenericAccount {

    abstract override val name: String

    abstract override val holder: AccountHolder

    override fun holding(currency: Currency): BigDecimal = holdings[currency.uniqueId] ?: BigDecimal.ZERO

    override fun has(currency: Currency, amount: BigDecimal): Boolean =
        holdings[currency.uniqueId]?.let {
            it + if (holdingRange.start < 0) {
                holdingRange.start.absoluteValue.toBigDecimal()
            } else {
                BigDecimal.ZERO
            } >= amount
        } ?: false

    override fun holdings(): Map<Currency, BigDecimal> =
        holdings.mapKeys {
            getStorageFor<Currency>(it.key) ?: throw IllegalStateException(
                "No currency found for ${it.key}"
            )
        }

    override fun deposit(currency: Currency, amount: BigDecimal, source: AccountActionSource): HoldingsActionResult {
        // 强制操作
        if (source.isForce) {
            holdings[currency.uniqueId] =
                (holding(currency) + amount).coerceAtMost(holdingRange.endInclusive.toBigDecimal())
            log("(force)${holder.name}(${holder.uniqueId}) deposit $amount with currency ${currency.name}(${currency.uniqueId}) with source ${source.name}(reason:${source.reason})")
            holder.sendMail(
                Mail(
                    ComponentUtils.buildAccountNotificationMail(
                        title = "（强制）账户动账通知",
                        type = "实时交易存款",
                        account = this,
                        amount = amount,
                        currency = currency,
                        source = source,
                    )
                )
            )
            return HoldingsActionResultImpl(
                actionType = "（强制）实时交易存款",
                currency = currency,
                account = this,
                result = Success
            )
        }
        // 最大限额
        if (holding(currency) + amount > holdingRange.endInclusive.toBigDecimal()) {
            log("${holder.name}(${holder.uniqueId}) failed to deposit $amount with currency ${currency.name}(${currency.uniqueId}) with source ${source.name}(reason:${source.reason}) cause max holding is ${holdingRange.endInclusive}")
            return HoldingsActionResultImpl(
                actionType = "实时交易存款",
                currency = currency,
                account = this,
                result = MaxHoldings
            )
        }
        // 进行存款
        holdings[currency.uniqueId] = holding(currency) + amount
        log("${holder.name}(${holder.uniqueId}) deposit $amount with currency ${currency.name}(${currency.uniqueId}) with source ${source.name}(reason:${source.reason})")
        holder.sendMail(
            Mail(
                ComponentUtils.buildAccountNotificationMail(
                    title = "账户动账通知",
                    type = "实时交易存款",
                    account = this,
                    amount = amount,
                    currency = currency,
                    source = source,
                )
            )
        )
        return HoldingsActionResultImpl(
            actionType = "实时交易存款",
            currency = currency,
            account = this,
            result = Success
        )
    }

    override fun withdraw(currency: Currency, amount: BigDecimal, source: AccountActionSource): HoldingsActionResult {
        // 强制操作
        if (source.isForce) {
            holdings[currency.uniqueId] =
                (holding(currency) - amount).coerceAtLeast(holdingRange.start.toBigDecimal())
            log("(force)${holder.name}(${holder.uniqueId}) withdraw $amount with currency ${currency.name}(${currency.uniqueId}) with source ${source.name}(reason:${source.reason})")
            holder.sendMail(
                Mail(
                    ComponentUtils.buildAccountNotificationMail(
                        title = "（强制）账户动账通知",
                        type = "实时交易扣款",
                        account = this,
                        amount = amount,
                        currency = currency,
                        source = source,
                    )
                )
            )
            return HoldingsActionResultImpl(
                actionType = "（强制）实时交易扣款",
                currency = currency,
                account = this,
                result = Success
            )
        }
        // 钱不够
        if (!has(currency, amount)) {
            log("${holder.name}(${holder.uniqueId}) failed to withdraw $amount with currency ${currency.name}(${currency.uniqueId}) with source ${source.name}(reason:${source.reason}) cause holdings is not enough")
            return HoldingsActionResultImpl(
                actionType = "实时交易扣款",
                currency = currency,
                account = this,
                result = Insufficient
            )
        }
        // 进行扣款
        holdings[currency.uniqueId] = holding(currency) - amount
        log("${holder.name}(${holder.uniqueId}) withdraw $amount with currency ${currency.name}(${currency.uniqueId}) with source ${source.name}(reason:${source.reason})")
        holder.sendMail(
            Mail(
                ComponentUtils.buildAccountNotificationMail(
                    title = "账户动账通知",
                    type = "实时交易扣款",
                    account = this,
                    amount = amount,
                    currency = currency,
                    source = source,
                )
            )
        )
        return HoldingsActionResultImpl(
            actionType = "实时交易扣款",
            currency = currency,
            account = this,
            result = Success
        )
    }

    override fun transfer(
        receiving: Account,
        currency: Currency,
        amount: BigDecimal,
        source: AccountActionSource
    ): HoldingsMoveActionResult {
        // 强制操作
        if (source.isForce) {
            withdraw(currency, amount, source)
            receiving.deposit(currency, amount, source)
            log("(force)${holder.name}(${holder.uniqueId}) transfer $amount with currency ${currency.name}(${currency.uniqueId}) to ${receiving.holder.name}(${receiving.holder.uniqueId}) with source ${source.name}(reason:${source.reason})")
            return HoldingsMoveActionResultImpl(
                actionType = "（强制）实时交易转账",
                currency = currency,
                account = this,
                receivingAccount = receiving,
                moving = amount,
                result = Success
            )
        }
        // 自己向自己转账
        if (receiving == this) {
            log("${holder.name}(${holder.uniqueId}) failed to transfer $amount with currency ${currency.name}(${currency.uniqueId}) to ${receiving.holder.name}(${receiving.holder.uniqueId}) with source ${source.name}(reason:${source.reason}) cause transfer to self")
            return HoldingsMoveActionResultImpl(
                actionType = "实时交易转账",
                currency = currency,
                account = this,
                receivingAccount = receiving,
                moving = amount,
                result = Unsupported
            )
        }
        // 进行转账
        withdraw(currency, amount, source).also {
            // 本账户扣款未成功
            if (!it.result.isSuccess) {
                log("${holder.name}(${holder.uniqueId}) failed to transfer $amount with currency ${currency.name}(${currency.uniqueId}) to ${receiving.holder.name}(${receiving.holder.uniqueId}) with source ${source.name}(reason:${source.reason}) cause withdraw failed(reason:${it.result.reason}")
                return HoldingsMoveActionResultImpl(
                    actionType = "实时交易转账",
                    currency = currency,
                    account = this,
                    receivingAccount = receiving,
                    moving = amount,
                    result = it.result
                )
            }
        }
        receiving.deposit(currency, amount, source).also {
            // 对方账户收款未成功
            if (!it.result.isSuccess) {
                log("${holder.name}(${holder.uniqueId}) failed to transfer $amount with currency ${currency.name}(${currency.uniqueId}) to ${receiving.holder.name}(${receiving.holder.uniqueId}) with source ${source.name}(reason:${source.reason}) cause deposit failed(reason:${it.result.reason}")
                // 还原本账户扣款
                deposit(currency, amount, object : AccountActionSource {
                    override val operator: AccountOperator = SystemAccountOperator
                    override val name: String = "账户转账失败退回"
                    override val reason: String = "账户转账失败退回"
                })
                return HoldingsMoveActionResultImpl(
                    actionType = "实时交易转账",
                    currency = currency,
                    account = this,
                    receivingAccount = receiving,
                    moving = amount,
                    result = it.result
                )
            }
        }
        log("${holder.name}(${holder.uniqueId}) transfer $amount with currency ${currency.name}(${currency.uniqueId}) to ${receiving.holder.name}(${receiving.holder.uniqueId}) with source ${source.name}(reason:${source.reason})")
        return HoldingsMoveActionResultImpl(
            actionType = "实时交易转账",
            currency = currency,
            account = this,
            receivingAccount = receiving,
            moving = amount,
            result = Success
        )
    }

}