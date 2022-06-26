package io.hikarilan.starlightcorex.economy.account.gui

import io.hikarilan.starlightcorex.economy.account.Account
import io.hikarilan.starlightcorex.economy.account.AccountActionSource
import io.hikarilan.starlightcorex.economy.account.AccountOperator
import io.hikarilan.starlightcorex.economy.currency.Currency
import io.hikarilan.starlightcorex.economy.result.HoldingsMoveActionResult
import io.hikarilan.starlightcorex.gui.PageScrollableGui
import io.hikarilan.starlightcorex.gui.exitButton
import io.hikarilan.starlightcorex.person.Human
import io.hikarilan.starlightcorex.person.TechnicalPlayer
import io.hikarilan.starlightcorex.storage.getAllStorageFor
import io.hikarilan.starlightcorex.utils.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.math.BigDecimal

class CurrencyTransferGui(
    owner: Human,
    proxied: Human,
    private val account: Account,
    private val currency: Currency
) : PageScrollableGui(
    owner, proxied, title = Component.text("${currency.name}转账 - 选择转账账户").color(primaryColor)
) {

    var to: Account? = null

    var amount: BigDecimal = BigDecimal.ZERO

    var result: HoldingsMoveActionResult? = null

    override val elementsBuilder: MutableList<GuiElement>.() -> Unit = {
        getAllStorageFor<TechnicalPlayer>().filter { it.currentHuman != owner && it.isOnline }.forEach { player ->
            add(GuiElement(ItemStack(Material.PLAYER_HEAD, 1).apply {
                editMeta(SkullMeta::class.java) {
                    it.owningPlayer = player.bukkitPlayer
                    it.displayName(Component.text(player.currentHuman.account.name))
                }
            }, close = true) {
                to = player.currentHuman.account
                transferConversation.begin()
            })
        }
    }

    override val fixedBuilder: MutableMap<Int, GuiElement>.() -> Unit = {
        put(45, GuiElement(item = ItemStack(Material.PAPER, 1).apply {
            editMeta {
                it.displayName(Component.text("手动键入转账账户标识符"))
            }
        }, close = true) {
            transferConversation.begin()
        })
        exitButton()
    }

    private val transferConversation = buildConversation(proxied) {
        add(ConversationSection(prompt = {
            Component.text("请输入要转账的账户标识符")
                .color(secondaryColor)
        }, validate = { input, _ ->
            to != null || getAllStorageFor<Account>().any { it.uniqueId.toString() == input }
        }, acceptInput = { input, _ ->
            if (to == null) {
                to = getAllStorageFor<Account>().find { it.uniqueId.toString() == input }
            }
            mapOf()
        }, skipWhen = { _ -> to != null }))
        add(ConversationSection(prompt = {
            Component.text("请输入要转账的金额")
                .color(secondaryColor)
        }, validate = { input, _ ->
            amount != BigDecimal.ZERO || input?.toBigDecimalOrNull() != null
        }, acceptInput = { input, _ ->
            if (amount == BigDecimal.ZERO) {
                amount = input!!.toBigDecimal()
            }
            mapOf()
        }, skipWhen = { _ -> amount != BigDecimal.ZERO }))
        add(
            ConversationSection(
                prompt = { Component.text("正在处理中，请稍后").color(secondaryColor) },
                acceptInput = { _, _ ->
                    result = account.transfer(to!!, currency, amount,
                        object : AccountActionSource {
                            override val operator: AccountOperator = proxied
                            override val name: String = proxied.name
                            override val reason: String = "现金账户转账"
                        })
                    mapOf()
                },
                skipWhen = { true },
            )
        )
        add(messageConversationSection { _ ->
            if (result?.isSuccess == true) {
                Component.text("转账成功，转账金额：")
                    .append(Component.text(amount.toString()))
                    .append(Component.text("，扣款后余额："))
                    .append(Component.text(result?.holding.toString()))
                    .color(NamedTextColor.GREEN)
            } else {
                Component.text("扣款失败，原因：")
                    .append(Component.text(result?.reason ?: "未知"))
                    .color(NamedTextColor.RED)
            }
        })
    }

    init {
        initInventory()
    }

}