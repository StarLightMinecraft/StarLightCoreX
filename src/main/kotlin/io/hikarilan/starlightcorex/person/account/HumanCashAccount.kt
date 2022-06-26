package io.hikarilan.starlightcorex.person.account

import io.hikarilan.starlightcorex.economy.account.*
import io.hikarilan.starlightcorex.economy.currency.Currency
import io.hikarilan.starlightcorex.economy.result.*
import io.hikarilan.starlightcorex.generic.result.Success
import io.hikarilan.starlightcorex.generic.result.Unsupported
import io.hikarilan.starlightcorex.person.Human
import io.hikarilan.starlightcorex.storage.getStorageFor
import io.hikarilan.starlightcorex.utils.*
import java.math.BigDecimal
import java.util.*
import kotlin.math.absoluteValue

/**
 * 玩家现金账户
 *
 * 这个账户不支持在玩家不在线的情况下执行操作（除非这个操作来自一个 ForceActionSource）
 */
data class HumanCashAccount(
    private val upstream: UUID,
    override val uniqueId: UUID = UUID.randomUUID(),
    private val holdings: MutableMap<UUID, BigDecimal> = mutableMapOf()
) : DirectAccount {

    override val name: String
        get() = "${holder.name} 的现金账户"

    override val holder: Human
        get() = getStorageFor<Human>(upstream) ?: throw IllegalStateException("No human found for $upstream")

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
                        title = "（强制）现金账户动账通知",
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
        // 玩家不在线
        if (!holder.isOnline()) {
            log("${holder.name}(${holder.uniqueId}) failed to deposit $amount with currency ${currency.name}(${currency.uniqueId}) with source ${source.name}(reason:${source.reason}) cause player is offline")
            return HoldingsActionResultImpl(
                actionType = "实时交易存款",
                currency = currency,
                account = this,
                result = Unsupported,
                reason = "账户所有者不在线，转账失败"
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
        // 玩家在线，进行现金存款
        holdings[currency.uniqueId] = holding(currency) + amount
        log("${holder.name}(${holder.uniqueId}) deposit $amount with currency ${currency.name}(${currency.uniqueId}) with source ${source.name}(reason:${source.reason})")
        holder.sendMail(
            Mail(
                ComponentUtils.buildAccountNotificationMail(
                    title = "现金账户动账通知",
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
                        title = "（强制）现金账户动账通知",
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
        // 玩家不在线
        if (!holder.isOnline()) {
            log("${holder.name}(${holder.uniqueId}) failed to withdraw $amount with currency ${currency.name}(${currency.uniqueId}) with source ${source.name}(reason:${source.reason}) cause player is offline")
            return HoldingsActionResultImpl(
                actionType = "实时交易扣款",
                currency = currency,
                account = this,
                result = Unsupported,
                reason = "账户所有者不在线，转账失败"
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
        // 玩家在线，进行现金扣款
        holdings[currency.uniqueId] = holding(currency) - amount
        log("${holder.name}(${holder.uniqueId}) withdraw $amount with currency ${currency.name}(${currency.uniqueId}) with source ${source.name}(reason:${source.reason})")
        holder.sendMail(
            Mail(
                ComponentUtils.buildAccountNotificationMail(
                    title = "现金账户动账通知",
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
        // 向玩家现金账户转账但玩家不在线
        if (receiving is HumanCashAccount && !receiving.holder.isOnline()) {
            log("${holder.name}(${holder.uniqueId}) failed to transfer $amount with currency ${currency.name}(${currency.uniqueId}) to ${receiving.holder.name}(${receiving.holder.uniqueId}) with source ${source.name}(reason:${source.reason}) cause receiving player is offline")
            return HoldingsMoveActionResultImpl(
                actionType = "实时交易转账",
                currency = currency,
                account = this,
                receivingAccount = receiving,
                moving = amount,
                result = Unsupported,
                reason = "账户所有者不在线，转账失败"
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
                result = Unsupported,
                reason = "您不能给自己转账"
            )
        }
        // 距离太远
        if (receiving is HumanCashAccount && (receiving.holder.location.world != this.holder.location.world || receiving.holder.location.distanceSquared(this.holder.location) >= 20 * 20)) {
            log("${holder.name}(${holder.uniqueId}) failed to transfer $amount with currency ${currency.name}(${currency.uniqueId}) to ${receiving.holder.name}(${receiving.holder.uniqueId}) with source ${source.name}(reason:${source.reason}) cause too far")
            return HoldingsMoveActionResultImpl(
                actionType = "实时交易转账",
                currency = currency,
                account = this,
                receivingAccount = receiving,
                moving = amount,
                result = Unsupported,
                reason = "对方距离过远，无法转账"
            )
        }
        // 进行现金转账
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