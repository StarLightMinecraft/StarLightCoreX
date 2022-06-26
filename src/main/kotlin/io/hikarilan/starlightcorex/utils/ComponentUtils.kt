package io.hikarilan.starlightcorex.utils

import io.hikarilan.starlightcorex.economy.account.Account
import io.hikarilan.starlightcorex.economy.account.AccountActionSource
import io.hikarilan.starlightcorex.economy.currency.Currency
import io.hikarilan.starlightcorex.utils.GeneralUtils.format
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import java.math.BigDecimal
import java.time.Instant

object ComponentUtils {

    fun buildAccountNotificationMail(
        title: String,
        time: Instant = Instant.now(),
        type: String,
        account: Account,
        currency: Currency,
        amount: BigDecimal,
        source: AccountActionSource
    ): Component {
        return Component.text(title).color(primaryColor).decorate(TextDecoration.BOLD).append(Component.newline())
            .append(
                Component.text("交易时间:").color(secondaryColor)
                    .append(Component.text(time.format()).color(secondaryColorVariant))
            ).append(Component.newline())
            .append(
                Component.text("交易类型:").color(secondaryColor).append(Component.text(type).color(secondaryColorVariant))
            ).append(Component.newline())
            .append(
                Component.text("交易账户:").color(secondaryColor)
                    .append(Component.text(account.name).color(secondaryColorVariant))
            ).append(Component.newline())
            .append(
                Component.text("交易金额:").color(secondaryColor)
                    .append(Component.text("${currency.name}$amount${currency.symbol}").color(secondaryColorVariant))
            ).append(Component.newline())
            .append(
                Component.text("备注:").color(secondaryColor).append(
                    Component.text("${source.name}-${source.reason}-${source.operator.name}")
                        .color(secondaryColorVariant)
                )
            )

    }

    fun String.toMiniMessage(): Component {
        return MiniMessage.miniMessage().deserialize(this)
    }

}