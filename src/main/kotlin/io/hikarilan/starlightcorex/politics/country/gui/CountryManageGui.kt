package io.hikarilan.starlightcorex.politics.country.gui

import io.hikarilan.starlightcorex.economy.account.AccountActionSource
import io.hikarilan.starlightcorex.economy.account.AccountOperator
import io.hikarilan.starlightcorex.economy.account.SystemAccountOperator
import io.hikarilan.starlightcorex.economy.account.gui.AccountManageGui
import io.hikarilan.starlightcorex.economy.currency.CountryLocalCurrency
import io.hikarilan.starlightcorex.economy.currency.CountryRemoteCurrency
import io.hikarilan.starlightcorex.economy.currency.Currency
import io.hikarilan.starlightcorex.economy.currency.LocalCurrency
import io.hikarilan.starlightcorex.gui.GuiBase
import io.hikarilan.starlightcorex.gui.PageScrollableGui
import io.hikarilan.starlightcorex.gui.exitButton
import io.hikarilan.starlightcorex.person.Human
import io.hikarilan.starlightcorex.politics.country.Country
import io.hikarilan.starlightcorex.region.Region
import io.hikarilan.starlightcorex.storage.getAllStorageFor
import io.hikarilan.starlightcorex.storage.getStorageFor
import io.hikarilan.starlightcorex.utils.*
import io.hikarilan.starlightcorex.utils.ComponentUtils.toMiniMessage
import io.hikarilan.starlightcorex.utils.GeneralUtils.format
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal
import java.time.Instant

class CountryManageGui(
    owner: Human,
    proxied: Human,
    private val country: Country,
) : GuiBase(owner, proxied, title = Component.text("管理 ").append(Component.text(country.name)).color(primaryColor)) {

    override val builder: MutableMap<Int, GuiElement>.() -> Unit = {
        put(0, GuiElement(item = ItemStack(Material.PLAYER_HEAD).apply {
            editMeta {
                it.displayName(Component.text("国家信息总览").color(primaryColor))
                it.lore(buildList {
                    add(
                        Component.text("名称: ${country.name}").color(primaryColor)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                    add(
                        Component.text("国家标识符: ${country.uniqueId}").color(primaryColor)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                    add(
                        Component.text("国家人数: ${country.members.size}").color(primaryColor)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                })
            }
        }))
        if (country.hasPermission(owner, "country.check_join_pending")) {
            put(1, GuiElement(item = ItemStack(Material.PAPER).apply {
                editMeta { it.displayName(Component.text("出入籍管理").color(primaryColor)) }
            }) {
                CountryJoinPendingGui().openGUI()
            })
        }
        if (country.hasPermission(owner, "country.regions_access")) {
            put(2, GuiElement(item = ItemStack(Material.MAP).apply {
                editMeta { it.displayName(Component.text("领土管理").color(primaryColor)) }
            }) {
                CountryRegionGui().openGUI()
            })
        }
        if (country.hasPermission(owner, "country.regions_access")) {
            put(3, GuiElement(item = ItemStack(Material.GOLD_INGOT).apply {
                editMeta { it.displayName(Component.text("货币管理").color(primaryColor)) }
            }) {
                CountryCurrencyGui()
            })
        }
        if (country.hasPermission(owner, "country.settings_access")) {
            put(45, GuiElement(item = ItemStack(Material.BOW, 1).apply {
                editMeta { it.displayName(Component.text("国家设置").color(primaryColor)) }
            }) {
                CountrySettingsGui().openGUI()
            })
        }
        exitButton()
    }

    init {
        initInventory()
    }

    inner class CountryCurrencyGui : GuiBase(owner, proxied, title = Component.text("货币管理").color(primaryColor)) {

        override val builder: MutableMap<Int, GuiElement>.() -> Unit = {
            if (country.currency == null) {
                put(0, GuiElement(ItemStack(Material.GOLD_INGOT).apply {
                    editMeta {
                        it.displayName(Component.text("未发行货币").color(primaryColor))
                        it.lore(buildList {
                            add(
                                Component.text("左键点击以发行新货币").color(secondaryColorVariant)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                            add(
                                Component.text("右键点击以使用他国已发行货币作为本国货币").color(secondaryColorVariant)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        })
                    }
                }, close = true) { event ->
                    if (event.isLeftClick) {
                        buildConversation(forWhom = proxied) {
                            add(ConversationSection(prompt = {
                                Component.text("请输入要创建的货币名称")
                                    .color(secondaryColor)
                            }, validate = { input, _ -> input?.isNotBlank() == true }, acceptInput = { input, _ ->
                                mapOf("name" to input!!.trim())
                            }))
                            add(ConversationSection(prompt = {
                                Component.text("请输入要创建的货币标识，如$")
                                    .color(secondaryColor)
                            }, validate = { input, _ -> input?.isNotBlank() == true }, acceptInput = { input, _ ->
                                mapOf("symbol" to input!!.trim())
                            }))
                            add(messageConversationSection { context ->
                                val currency =
                                    CountryLocalCurrency(context["name"] as String, context["symbol"] as String)
                                country.currency = currency
                                Component.text("已创建货币：").append(Component.text(currency.name)).color(primaryColor)
                            })
                        }.begin()
                    } else {
                        buildConversation(forWhom = proxied) {
                            add(ConversationSection(prompt = {
                                Component.text("请输入要关联到的货币名称或标识符")
                                    .color(secondaryColor)
                            }, validate = { input, _ ->
                                getAllStorageFor<Currency>().any { it.type == Currency.Type.LOCAL && (it.name == input || it.uniqueId.toString() == input) }
                            }, acceptInput = { input, _ ->
                                mapOf("currency" to getAllStorageFor<Currency>().find { it.name == input || it.uniqueId.toString() == input }!!)
                            }))
                            add(messageConversationSection { context ->
                                val currency = (context["currency"] as LocalCurrency)
                                country.currency = CountryRemoteCurrency(currency.uniqueId)
                                Component.text("已关联到货币：").append(Component.text(currency.name)).color(primaryColor)
                            })
                        }.begin()
                    }
                })
            } else {
                put(0, GuiElement(ItemStack(Material.GOLD_INGOT).apply {
                    editMeta {
                        it.displayName(Component.text("货币总览").color(primaryColor))
                        it.lore(buildList {
                            add(
                                Component.text("货币名称：").append(Component.text(country.currency!!.name))
                                    .color(secondaryColorVariant)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                            add(
                                Component.text("货币标识：").append(Component.text(country.currency!!.symbol))
                                    .color(secondaryColorVariant)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                            add(
                                Component.text("货币类型：").append(
                                    when (country.currency!!.type) {
                                        Currency.Type.LOCAL -> Component.text("本国货币")
                                        Currency.Type.REMOTE -> Component.text("他国货币")
                                    }
                                ).color(secondaryColorVariant)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                            add(
                                Component.text("货币标识符：").append(Component.text(country.currency!!.uniqueId.toString()))
                                    .color(secondaryColorVariant)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        })
                    }
                }))
            }
            put(1, GuiElement(ItemStack(Material.EMERALD).apply {
                editMeta { it.displayName(Component.text("国库管理").color(primaryColor)) }
            }) {
                AccountManageGui(owner, proxied, country.account).openGUI()
            })
            if (country.currency is LocalCurrency) {
                put(2, GuiElement(ItemStack(Material.PAPER).apply {
                    editMeta {
                        it.displayName(Component.text("发行货币").color(primaryColor))
                        it.lore(buildList {
                            add(
                                Component.text("发行任意单位的货币").append(Component.text(country.currency!!.name))
                                    .color(secondaryColorVariant)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                            add(
                                Component.text("新发行的货币将被添加到您的国库账户中").append(Component.text(country.currency!!.name))
                                    .color(secondaryColorVariant)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                            add(
                                Component.text("然后，您便可通过国库转账系统分发您的货币").append(Component.text(country.currency!!.name))
                                    .color(secondaryColorVariant)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        })
                    }
                }) {
                    buildConversation(forWhom = proxied) {
                        add(ConversationSection(prompt = {
                            Component.text("请输入要发行的货币数量")
                                .color(secondaryColor)
                        }, validate = { input, _ ->
                            input?.toBigDecimalOrNull() != null
                        }, acceptInput = { input, _ ->
                            mapOf("amount" to input!!.toBigDecimal())
                        }))
                        add(messageConversationSection { context ->
                            val amount = (context["amount"] as BigDecimal)
                            country.account.deposit(country.currency!!, amount, object : AccountActionSource {
                                override val operator: AccountOperator = SystemAccountOperator
                                override val name: String = "发行货币"
                                override val reason: String = "发行本国货币"
                            })
                            Component.text("已发行货币：")
                                .append(Component.text(country.currency!!.name))
                                .append(Component.text(country.currency!!.format(amount)))
                                .append(Component.text(country.currency!!.symbol))
                                .color(primaryColor)
                        })
                    }.begin()
                })
            }
        }

        init {
            initInventory()
        }

    }

    inner class CountrySettingsGui :
        PageScrollableGui(owner, proxied, title = Component.text("国家设置").color(primaryColor)) {

        override val elementsBuilder: MutableList<GuiElement>.() -> Unit = {
            add(GuiElement(item = ItemStack(Material.PAPER).apply {
                editMeta { it.displayName(Component.text("修改国家名称").color(primaryColor)) }
            }, close = true) {
                buildConversation(proxied) {
                    add(ConversationSection(prompt = {
                        Component.text("请输入新的名称").color(secondaryColor)
                    }, validate = { input, _ ->
                        input?.isNotBlank() == true
                    }, acceptInput = { input, _ ->
                        mapOf("name" to input!!)
                    }))
                    add(messageConversationSection {
                        log("${proxied.name} has changed the country ${country.name}(${owner.uniqueId})'s name to ${it["name"]}")
                        country.name = it["name"] as String
                        Component.text("已更改国家名称为: ${it["name"]}").color(primaryColor)
                    })
                }.begin()
            })
            // 设置出入境提示
            add(GuiElement(item = ItemStack(Material.GREEN_WOOL).apply {
                editMeta {
                    it.displayName(Component.text("设置入境提示").color(primaryColor))
                    it.lore(buildList {
                        add(Component.text("可使用 {country} 代表国家名称").color(secondaryColor))
                        add(Component.text("----------").decoration(TextDecoration.ITALIC, false))
                        add(Component.text("左键点击以输入入境提示").color(secondaryColor))
                        add(Component.text("右键点击以清除入境提示").color(secondaryColor))
                    })
                }
            }, close = true) {
                if (it.isRightClick) {
                    country.enterMessage = null
                    proxied.sendMessage(Component.text("已清除入境提示语"))
                } else {
                    buildConversation(proxied) {
                        add(ConversationSection(
                            prompt = {
                                Component.text("请输入入境提示语，支持 MiniMessage 语法").color(secondaryColor)
                            },
                            validate = { input, _ ->
                                input?.isNotBlank() == true
                            },
                            acceptInput = { input, _ ->
                                mapOf("message" to input!!.toMiniMessage())
                            }
                        ))
                        add(messageConversationSection { context ->
                            country.enterMessage = context["message"] as Component
                            Component.text("已更改入境提示语为: ")
                                .append(context["message"] as Component)
                                .color(primaryColor)
                        })
                    }.begin()
                }
            })
            add(GuiElement(item = ItemStack(Material.RED_WOOL).apply {
                editMeta {
                    it.displayName(Component.text("设置出境提示").color(primaryColor))
                    it.lore(buildList {
                        add(Component.text("可使用 {country} 代表国家名称").color(secondaryColor))
                        add(Component.text("----------").decoration(TextDecoration.ITALIC, false))
                        add(Component.text("左键点击以输入出境提示").color(secondaryColor))
                        add(Component.text("右键点击以清除出境提示").color(secondaryColor))
                    })
                }
            }, close = true) {
                if (it.isRightClick) {
                    country.leaveMessage = null
                    proxied.sendMessage(Component.text("已清除出境提示语"))
                } else {
                    buildConversation(proxied) {
                        add(ConversationSection(
                            prompt = {
                                Component.text("请输入出境提示语，支持 MiniMessage 语法").color(secondaryColor)
                            },
                            validate = { input, _ ->
                                input?.isNotBlank() == true
                            },
                            acceptInput = { input, _ ->
                                mapOf("message" to input!!.toMiniMessage())
                            }
                        ))
                        add(messageConversationSection { context ->
                            country.leaveMessage = context["message"] as Component
                            Component.text("已更改出境提示语为: ")
                                .append(context["message"] as Component)
                                .color(primaryColor)
                        })
                    }.begin()
                }
            })
            add(GuiElement(item = ItemStack(Material.REDSTONE).apply {
                editMeta {
                    it.displayName(Component.text("是否允许他人加入本国家").color(primaryColor))
                    it.lore(buildList {
                        add(Component.text("当前状态: ${country.isAllowJoin}"))
                        add(Component.text("----------").decoration(TextDecoration.ITALIC, false))
                        add(Component.text("点击以切换设置").color(secondaryColor))
                    })
                }
            }) {
                country.isAllowJoin = !country.isAllowJoin
                proxied.sendMessage(Component.text("状态已切换为: ${country.isAllowJoin}"))
                reloadInventory()
            })
            if (country.isAllowJoin) {
                add(GuiElement(item = ItemStack(Material.REDSTONE).apply {
                    editMeta {
                        it.displayName(Component.text("加入国家时是否需要审核").color(primaryColor))
                        it.lore(buildList {
                            add(Component.text("当前状态: ${country.isJoinNeedPending}"))
                            add(Component.text("----------").decoration(TextDecoration.ITALIC, false))
                            add(Component.text("点击以切换设置").color(secondaryColor))
                        })
                    }
                }) {
                    country.isJoinNeedPending = !country.isJoinNeedPending
                    proxied.sendMessage(Component.text("状态已切换为: ${country.isJoinNeedPending}"))
                    reloadInventory()
                })
            }
        }

        override val fixedBuilder: MutableMap<Int, GuiElement>.() -> Unit = {
            exitButton()
        }

        init {
            initInventory()
        }

    }

    inner class CountryRegionGui : GuiBase(owner, proxied, title = Component.text("领土管理").color(primaryColor)) {

        override val builder: MutableMap<Int, GuiElement>.() -> Unit = {
            put(1, GuiElement(item = ItemStack(Material.GRASS_BLOCK).apply {
                editMeta { it.displayName(Component.text("将脚下的区块设置为领土").color(primaryColorVariant)) }
            }, close = true) {
                if (country.regions.contains(Region(owner.location.chunk))) {
                    proxied.sendMessage(Component.text("该区块已是国家领土，无须重复设置").color(NamedTextColor.RED))
                    return@GuiElement
                }
                val chunk = owner.location.chunk
                    (-1..1).flatMap {x-> (-1..1).map { z-> proxied.location.world.getChunkAt(chunk.x+x,chunk.z+z) } }
                        .filter { c-> country.regions.none { it.checkIn(c) } }
                        .forEach {
                            country.regions.add(Region(it))
                        }
                proxied.sendMessage(Component.text("已成功将脚下的区块设置为领土").color(primaryColor))
            })
            exitButton()
        }

        init {
            initInventory()
        }
    }

    inner class CountryJoinPendingGui :
        PageScrollableGui(owner, proxied, title = Component.text("出入籍管理").color(primaryColor)) {

        override val elementsBuilder: MutableList<GuiElement>.() -> Unit = {
            country.joinPending.mapKeys { getStorageFor<Human>(it.key)!! }.forEach { player ->
                add(GuiElement(item = ItemStack(Material.PLAYER_HEAD).apply {
                    editMeta {
                        it.displayName(Component.text(player.key.name).color(primaryColorVariant))
                        it.lore(
                            listOf(
                                Component.text("公民标识符：${player.key.uniqueId}").color(primaryColor)
                                    .decoration(TextDecoration.ITALIC, false),
                                Component.text("申请时间：${Instant.ofEpochMilli(player.value).format()}")
                                    .decoration(TextDecoration.ITALIC, false),
                                Component.text("----------").decoration(TextDecoration.ITALIC, false),
                                Component.text("按下鼠标左键以通过该申请").color(secondaryColor),
                                Component.text("按下鼠标右键以拒绝该申请").color(secondaryColor),
                                Component.text("按下鼠标中键以忽略该申请").color(secondaryColor)
                            )
                        )
                    }
                }) {
                    when (it.click) {
                        ClickType.LEFT -> {
                            country.joinPending.remove(player.key.uniqueId)
                            country.addMember(player.key)
                            proxied.sendMessage(Component.text("已通过公民入籍申请：${player.key.name}").color(primaryColor))
                            player.key.sendMail(
                                Mail(
                                    Component.text(
                                        "恭喜您！您已于 ${
                                            Instant.now().format()
                                        }通过 ${country.name} 的入籍申请，您现在是${country.name}的公民了！"
                                    ).color(primaryColor), expiredWhen = Long.MAX_VALUE
                                )
                            )
                        }
                        ClickType.RIGHT -> {
                            country.joinPending.remove(player.key.uniqueId)
                            proxied.sendMessage(Component.text("已拒绝公民入籍申请：${player.key.name}").color(primaryColor))
                            player.key.sendMail(
                                Mail(
                                    Component.text(
                                        "很抱歉的通知您，您 ${country.name} 的入籍申请在 ${
                                            Instant.now().format()
                                        } 已被拒绝"
                                    ).color(NamedTextColor.RED)
                                )
                            )
                        }
                        ClickType.MIDDLE -> {
                            country.joinPending.remove(player.key.uniqueId)
                            proxied.sendMessage(Component.text("已忽略公民入籍申请：${player.key.name}").color(primaryColor))
                        }
                        else -> {}
                    }
                    reloadInventory()
                })
            }
        }
        override val fixedBuilder: MutableMap<Int, GuiElement>.() -> Unit = {
            exitButton()
        }

        init {
            initInventory()
        }

    }

}