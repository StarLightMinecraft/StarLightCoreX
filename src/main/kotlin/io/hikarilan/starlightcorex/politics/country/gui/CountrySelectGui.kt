package io.hikarilan.starlightcorex.politics.country.gui

import io.hikarilan.starlightcorex.generic.result.Response
import io.hikarilan.starlightcorex.gui.PageScrollableGui
import io.hikarilan.starlightcorex.gui.exitButton
import io.hikarilan.starlightcorex.person.Human
import io.hikarilan.starlightcorex.politics.country.Country
import io.hikarilan.starlightcorex.storage.getAllStorageFor
import io.hikarilan.starlightcorex.storage.getOrCreateStorageFor
import io.hikarilan.starlightcorex.utils.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.*

class CountrySelectGui(
    owner: Human,
    proxied: Human,
) : PageScrollableGui(
    owner,
    proxied,
    title = Component.text("管理，加入或创建一个国家").color(primaryColor)
) {

    override val elementsBuilder: MutableList<GuiElement>.() -> Unit = {
        owner.countries.forEach { country ->
            add(GuiElement(item = ItemStack(Material.BLUE_BANNER).apply {
                editMeta {
                    it.displayName(Component.text(country.name))
                }
            }) {
                CountryManageGui(owner, proxied, country).openGUI()
            })
        }
    }

    override val fixedBuilder: MutableMap<Int, GuiElement>.() -> Unit = {
        put(45, GuiElement(item = ItemStack(Material.ARROW, 1).apply {
            editMeta {
                it.displayName(Component.text("加入国家"))
                it.lore(buildList {
                    add(
                        Component.text("加入一个已存在的国家").color(NamedTextColor.WHITE)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                })
            }
        }, close = true) {
            buildConversation(proxied) {
                add(ConversationSection(
                    prompt = {
                        Component.text("请输入要加入的国家名称，或国家标识符").color(secondaryColor)
                    }, validate = { input, _ ->
                        getAllStorageFor<Country>().any { it.uniqueId.toString() == input || it.name == input }
                    }, acceptInput = { input, _ ->
                        mapOf("country" to getAllStorageFor<Country>().find { it.uniqueId.toString() == input || it.name == input }!!)
                    }
                ))
                add(
                    ConversationSection(
                        prompt = { Component.text("正在处理中，请稍后").color(secondaryColor) },
                        acceptInput = { _, context ->
                            mapOf("result" to (context["country"] as Country).attemptToJoin(owner))
                        },
                        skipWhen = { true },
                    )
                )
                add(messageConversationSection {
                    val result = it["result"] as Response
                    if (result.isSuccess) {
                        Component.text(result.reason).color(NamedTextColor.GREEN)
                    } else {
                        Component.text("加入失败，理由：").append(Component.text(result.reason)).color(NamedTextColor.RED)
                    }
                })
            }.begin()
        })
        put(46, GuiElement(item = ItemStack(Material.COD, 1).apply {
            editMeta {
                it.displayName(Component.text("创建国家"))
                it.lore(buildList {
                    add(Component.text("创建一个新的国家").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))
                    add(
                        Component.text("注意，这将立即创建一个新的国家并自动加入")
                            .color(NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                    add(
                        Component.text("无法撤销，请谨慎操作")
                            .color(NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                })
            }
        }, close = true) {
            UUID.randomUUID().let { getOrCreateStorageFor(it) { Country(uniqueId = it) } }.apply {
                this.addMember(owner)
                this.scopedGroup[owner.uniqueId] = "ruling"
                proxied.sendMessage(
                    Component.text("已成功创建 ")
                        .append(Component.text(name))
                        .color(secondaryColorVariant)
                )
            }
        })
        exitButton()
    }

    init {
        initInventory()
    }

}