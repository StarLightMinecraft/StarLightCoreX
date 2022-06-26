package io.hikarilan.starlightcorex.economy.account.gui

import io.hikarilan.starlightcorex.economy.account.Account
import io.hikarilan.starlightcorex.economy.account.AccountActionSource
import io.hikarilan.starlightcorex.economy.account.AccountOperator
import io.hikarilan.starlightcorex.economy.currency.Currency
import io.hikarilan.starlightcorex.economy.result.HoldingsActionResult
import io.hikarilan.starlightcorex.gui.GuiBase
import io.hikarilan.starlightcorex.gui.PageScrollableGui
import io.hikarilan.starlightcorex.gui.adminButton
import io.hikarilan.starlightcorex.gui.exitButton
import io.hikarilan.starlightcorex.person.Human
import io.hikarilan.starlightcorex.storage.getAllStorageFor
import io.hikarilan.starlightcorex.utils.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.math.BigDecimal
import kotlin.math.absoluteValue

class AccountManageGui(
    owner: Human,
    proxied: Human,
    private val account: Account
) : PageScrollableGui(
    owner,
    proxied = proxied,
    title = Component.text(account.name).color(primaryColor)
) {

    class Admin(
        owner: Human, proxied: Human, private val account: Account
    ) : GuiBase(
        owner = owner,
        title = Component.text("GM").color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD),
        proxied = proxied
    ) {

        override val builder: MutableMap<Int, GuiElement>.() -> Unit = {
            put(0, GuiElement(item = ItemStack(Material.GOLD_NUGGET, 1).apply {
                editMeta { it.displayName(Component.text("强制存款").color(secondaryColorVariant)) }
            }, close = true) {
                buildConversation(forWhom = proxied) {
                    add(ConversationSection(prompt = {
                        Component.text("请输入要强制存款的货币名称或标识符，可用的货币：")
                            .color(secondaryColor)
                            .append(
                                Component.join(
                                    JoinConfiguration.commas(true),
                                    getAllStorageFor<Currency>().map { Component.text(it.name) })
                            )
                    }, validate = { input, _ ->
                        getAllStorageFor<Currency>().any { it.name == input || it.uniqueId.toString() == input }
                    }, acceptInput = { input, _ ->
                        mapOf("currency" to getAllStorageFor<Currency>().find { it.name == input || it.uniqueId.toString() == input }!!)
                    }))
                    add(ConversationSection(prompt = {
                        Component.text("请输入要强制存款的金额").color(secondaryColor)
                    }, validate = { input, _ ->
                        input?.toBigDecimalOrNull() != null
                    }, acceptInput = { input, _ ->
                        mapOf("amount" to input!!.toBigDecimal())
                    }))
                    add(
                        ConversationSection(
                            prompt = { Component.text("正在处理中，请稍后").color(secondaryColor) },
                            acceptInput = { _, context ->
                                mapOf(
                                    "result" to account.deposit(
                                        context["currency"] as Currency,
                                        context["amount"] as BigDecimal,
                                        object : AccountActionSource {
                                            override val operator: AccountOperator = proxied
                                            override val name: String = "GM"
                                            override val reason: String = operator.name
                                            override val isForce: Boolean = true
                                        })
                                )
                            },
                            skipWhen = { true },
                        )
                    )
                    add(messageConversationSection { context ->
                        val result = (context["result"] as HoldingsActionResult)
                        if (result.isSuccess) {
                            Component.text("存款成功，存款金额：")
                                .append(Component.text((context["amount"] as BigDecimal).toString()))
                                .append(Component.text("，存款后余额："))
                                .append(Component.text(result.holding.toString()))
                                .color(NamedTextColor.GREEN)
                        } else {
                            Component.text("存款失败，原因：")
                                .append(Component.text(result.reason))
                                .color(NamedTextColor.RED)
                        }
                    })
                }.begin()
            })
            put(1, GuiElement(item = ItemStack(Material.GOLD_NUGGET, 1).apply {
                editMeta { it.displayName(Component.text("强制扣款").color(secondaryColorVariant)) }
            }, close = true) {
                buildConversation(forWhom = proxied) {
                    add(ConversationSection(prompt = {
                        Component.text("请输入要强制扣款的货币名称或标识符，可用的货币：")
                            .color(secondaryColor)
                            .append(
                                Component.join(
                                    JoinConfiguration.commas(true),
                                    getAllStorageFor<Currency>().map { Component.text(it.name) })
                            )
                    }, validate = { input, _ ->
                        getAllStorageFor<Currency>().any { it.name == input || it.uniqueId.toString() == input }
                    }, acceptInput = { input, _ ->
                        mapOf("currency" to getAllStorageFor<Currency>().find { it.name == input || it.uniqueId.toString() == input }!!)
                    }))
                    add(ConversationSection(prompt = {
                        Component.text("请输入要强制扣款的金额").color(secondaryColor)
                    }, validate = { input, _ ->
                        input?.toBigDecimalOrNull() != null
                    }, acceptInput = { input, _ ->
                        mapOf("amount" to input!!.toBigDecimal())
                    }))
                    add(
                        ConversationSection(
                            prompt = { Component.text("正在处理中，请稍后").color(secondaryColor) },
                            acceptInput = { _, context ->
                                mapOf(
                                    "result" to account.withdraw(
                                        context["currency"] as Currency,
                                        context["amount"] as BigDecimal,
                                        object : AccountActionSource {
                                            override val operator: AccountOperator = proxied
                                            override val name: String = "GM"
                                            override val reason: String = operator.name
                                            override val isForce: Boolean = true
                                        })
                                )
                            },
                            skipWhen = { true },
                        )
                    )
                    add(messageConversationSection { context ->
                        val result = (context["result"] as HoldingsActionResult)
                        if (result.isSuccess) {
                            Component.text("扣款成功，扣款金额：")
                                .append(Component.text((context["amount"] as BigDecimal).toString()))
                                .append(Component.text("，扣款后余额："))
                                .append(Component.text(result.holding.toString()))
                                .color(NamedTextColor.GREEN)
                        } else {
                            Component.text("扣款失败，原因：")
                                .append(Component.text(result.reason))
                                .color(NamedTextColor.RED)
                        }
                    })
                }.begin()
            }
            )
            exitButton()
        }

        init {
            initInventory()
        }

    }

    override val elementsBuilder: MutableList<GuiElement>.() -> Unit = {
        account.holdings().filterValues { it != BigDecimal.ZERO }.forEach { entry ->
            add(GuiElement(item = ItemStack(Material.GOLD_NUGGET, 1).apply {
                editMeta {
                    it.displayName(Component.text(entry.key.name))
                    it.lore(buildList {
                        add(
                            Component.text("余额：${entry.key.symbol}${entry.key.format(entry.value)}").color(primaryColor)
                                .decoration(TextDecoration.ITALIC, false)
                        )
                        add(Component.text("----------").decoration(TextDecoration.ITALIC, false))
                        add(
                            Component.text("点击以发起${entry.key.name}转账").color(secondaryColorVariant)
                                .decoration(TextDecoration.ITALIC, false)
                        )
                    })
                }
            }) {
                CurrencyTransferGui(owner, proxied, account, entry.key).openGUI()
            })
        }
    }
    override val fixedBuilder: MutableMap<Int, GuiElement>.() -> Unit = {
        put(45, GuiElement(item = ItemStack(Material.PLAYER_HEAD, 1).apply {
            editMeta(SkullMeta::class.java) { meta ->
                meta.owningPlayer = owner.technicalPlayer.bukkitPlayer
                meta.displayName(Component.text("账户信息总览").color(primaryColorVariant))
                meta.lore(buildList {
                    add(
                        Component.text("账户名称: ${account.name}").color(primaryColor)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                    add(
                        Component.text("账户标识符: ${account.uniqueId}").color(primaryColor)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                    add(
                        Component.text("单币种存款最大限额: ${account.holdingRange.endInclusive}")
                            .color(primaryColor)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                    add(
                        Component.text(
                            "单币种可透支额度: ${
                                if (account.holdingRange.start < 0) {
                                    account.holdingRange.start.absoluteValue
                                } else {
                                    0
                                }
                            }"
                        ).color(primaryColor)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                })
            }
        }, close = false))
        adminButton(owner = owner, action = {
            owner.openGUI(Admin(owner, proxied, account))
        })
        exitButton()
    }

    init {
        initInventory()
    }

}