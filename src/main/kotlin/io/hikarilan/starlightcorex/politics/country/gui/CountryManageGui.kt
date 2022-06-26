package io.hikarilan.starlightcorex.politics.country.gui

import io.hikarilan.starlightcorex.gui.GuiBase
import io.hikarilan.starlightcorex.gui.PageScrollableGui
import io.hikarilan.starlightcorex.gui.exitButton
import io.hikarilan.starlightcorex.person.Human
import io.hikarilan.starlightcorex.politics.country.Country
import io.hikarilan.starlightcorex.region.Region
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

    inner class CountryCurrencyGui: GuiBase(owner, proxied, title = Component.text("货币管理").color(primaryColor)) {

        override val builder: MutableMap<Int, GuiElement>.() -> Unit = {

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
                    buildConversation(proxied){
                        add(ConversationSection(
                            prompt = {
                                Component.text("请输入入境提示语，支持 MiniMessage 语法").color(secondaryColor)
                            },
                            validate = { input, _ ->
                                input?.isNotBlank() == true
                            },
                            acceptInput = { input,_->
                                mapOf("message" to input!!.toMiniMessage())
                            }
                        ))
                        add(messageConversationSection {context->
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
                    buildConversation(proxied){
                        add(ConversationSection(
                            prompt = {
                                Component.text("请输入出境提示语，支持 MiniMessage 语法").color(secondaryColor)
                            },
                            validate = { input, _ ->
                                input?.isNotBlank() == true
                            },
                            acceptInput = { input,_->
                                mapOf("message" to input!!.toMiniMessage())
                            }
                        ))
                        add(messageConversationSection {context->
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
            }){
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
                if (owner.isOnline()) {
                    country.regions.add(Region(owner.location.chunk))
                } else {
                    country.regions.add(Region(proxied.location.chunk))
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