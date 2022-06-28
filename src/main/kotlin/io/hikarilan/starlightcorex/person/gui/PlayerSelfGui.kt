package io.hikarilan.starlightcorex.person.gui

import io.hikarilan.starlightcorex.economy.account.gui.AccountManageGui
import io.hikarilan.starlightcorex.gui.GuiBase
import io.hikarilan.starlightcorex.gui.exitButton
import io.hikarilan.starlightcorex.person.Human
import io.hikarilan.starlightcorex.person.TechnicalPlayer
import io.hikarilan.starlightcorex.person.ability.AbilityManager.displayName
import io.hikarilan.starlightcorex.politics.country.gui.CountrySelectGui
import io.hikarilan.starlightcorex.storage.getStorageFor
import io.hikarilan.starlightcorex.utils.*
import io.hikarilan.starlightcorex.utils.ComponentUtils.toMiniMessage
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import kotlin.math.roundToInt

class PlayerSelfGui(
    owner: Human,
    proxied: Human,
) : GuiBase(owner = owner, title = Component.text("个人管理面板").color(primaryColor), proxied = proxied) {

    companion object{

        fun broadcastMessageSmall(
            player: Player,
            owner: Human,
            message: String,
            proxied: Human
        ) {
            if (player.foodLevel >= 2) {
                Bukkit.getConsoleSender().sendMessage(
                    Component.text("(500M)喊话 ").append(Component.text(owner.name))
                        .append(Component.text("("))
                        .append(Component.text(owner.technicalPlayer.bukkitPlayer.name ?: ""))
                        .append(Component.text(")"))
                        .append(Component.text(" >> ").color(NamedTextColor.WHITE))
                        .append(message.toMiniMessage())
                )
                Bukkit.getOnlinePlayers()
                    .filter {
                        it.location.world == player.location.world && it.location.distanceSquared(
                            player.location
                        ) <= 500 * 500
                    }
                    .mapNotNull { getStorageFor<TechnicalPlayer>(it.uniqueId) }.forEach {
                        val base =
                            Component.text("\uD83D\uDD0A").append(Component.text(owner.name))
                                .append(Component.text(" >> ").color(NamedTextColor.WHITE))
                                .append(message.toMiniMessage())
                        if (it.currentHuman == owner) {
                            it.sendMessage(base)
                        } else {
                            it.sendMessage(
                                base.append(
                                    Component.text(
                                        "（来自向北 ${
                                            GeneralUtils.getDirectionBetween(
                                                player.location,
                                                it.bukkitPlayer.player!!.location
                                            ).roundToInt()
                                        }° 角方向）"
                                    ).color(
                                        secondaryColorVariant
                                    )
                                )
                            )
                        }
                        it.bukkitPlayer.player?.playSound(
                            Sound.sound(
                                org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                                Sound.Source.PLAYER,
                                MAX_VOLUME,
                                DEFAULT_PITCH
                            ),
                            player.location.x,
                            player.location.y,
                            player.location.z
                        )
                    }
                player.foodLevel -= 2
            } else proxied.sendMessage(Component.text("饥饿度不足").color(secondaryColor))
        }

        fun broadcastMessageLarge(
            player: Player,
            owner: Human,
            message: String,
            proxied: Human
        ) {
            if (player.foodLevel >= 10) {
                Bukkit.getConsoleSender().sendMessage(
                    Component.text("(全服)喊话 ").append(Component.text(owner.name))
                        .append(Component.text("("))
                        .append(Component.text(owner.technicalPlayer.bukkitPlayer.name ?: ""))
                        .append(Component.text(")"))
                        .append(Component.text(" >> ").color(NamedTextColor.WHITE))
                        .append(message.toMiniMessage())
                )
                Bukkit.getOnlinePlayers()
                    .mapNotNull { getStorageFor<TechnicalPlayer>(it.uniqueId) }
                    .forEach {
                        it.sendMessage(
                            Component.text("\uD83D\uDD0A").append(Component.text(owner.name))
                                .append(Component.text(" >> ").color(NamedTextColor.WHITE))
                                .append(message.toMiniMessage())
                        )
                        it.bukkitPlayer.player?.playSound(
                            Sound.sound(
                                org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                                Sound.Source.PLAYER,
                                MAX_VOLUME,
                                DEFAULT_PITCH
                            ),
                            player.location.x,
                            player.location.y,
                            player.location.z
                        )
                    }
                player.foodLevel -= 10
            } else proxied.sendMessage(Component.text("饥饿度不足").color(secondaryColor))
        }

    }

    override fun openGUI() {
        if (/*序列化兼容性*/owner.candidateAbilities==null || owner.hasSelectedCandidateAbilities || owner.candidateAbilities.isEmpty()) {
            super.openGUI()
        } else {
            AbilitySelectGui().openGUI()
        }
    }

    override val builder: MutableMap<Int, GuiElement>.() -> Unit = {
        put(0, GuiElement(item = ItemStack(Material.PLAYER_HEAD, 1).apply {
            editMeta(SkullMeta::class.java) { meta ->
                meta.owningPlayer = owner.technicalPlayer.bukkitPlayer
                meta.displayName(Component.text("个人信息总览").color(primaryColorVariant))
                meta.lore(buildList {
                    add(
                        Component.text("名称: ${owner.name}").color(primaryColor).decoration(TextDecoration.ITALIC, false)
                    )
                    add(
                        Component.text("个人标识符: ${owner.uniqueId}").color(primaryColor)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                    add(
                        Component.text("所属国家:")
                            .append(
                                Component.join(
                                    JoinConfiguration.commas(true),
                                    owner.countries.map { Component.text(it.name) }.takeIf { it.isNotEmpty() } ?: setOf(
                                        Component.text("无")
                                    )
                                )
                            )
                            .color(primaryColor)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                    add(
                        Component.text("能力: ")
                            .append(Component.join(JoinConfiguration.commas(true), owner.getAbilities().map {
                                if (proxied.hasPermission("starlightcorex.admin")) {
                                    Component.text("${it.displayName}(${it.key})").color(NamedTextColor.RED)
                                } else if (owner.technicalPlayer.humans.size <= 100) {
                                    Component.text(it.displayName)
                                } else {
                                    Component.text(it.ambiguousName)
                                }
                            }))
                            .color(primaryColor)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                    if (proxied.hasPermission("starlightcorex.admin")) {
                        add(
                            Component.text("玩家技术用户名: ${owner.technicalPlayer.bukkitPlayer.name}")
                                .color(NamedTextColor.RED)
                        )
                        add(
                            Component.text("玩家技术标识符: ${owner.technicalPlayer.bukkitPlayer.uniqueId}")
                                .color(NamedTextColor.RED)
                        )
                    }
                    add(Component.text("----------").decoration(TextDecoration.ITALIC, false))
                    add(
                        Component.text("点击以更改名称（仅一次）").color(secondaryColorVariant)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                })
            }
        }, close = true) {
            if (owner.nameChangedTime > 0) {
                proxied.sendMessage(Component.text("名称只能修改一次").color(NamedTextColor.RED))
            } else {
                buildConversation(proxied) {
                    add(ConversationSection(prompt = {
                        Component.text("请输入新的名称").color(secondaryColor)
                    }, validate = { input, _ ->
                        input?.isNotBlank() == true
                    }, acceptInput = { input, _ ->
                        mapOf("name" to input!!)
                    }))
                    add(messageConversationSection {
                        log("${proxied.name} has changed ${owner.name}(${owner.uniqueId})'s name to ${it["name"]}")
                        owner.name = it["name"] as String
                        Component.text("已更改名称为: ${it["name"]}").color(primaryColor)
                    })
                }.begin()
            }
        })
        put(1, GuiElement(item = ItemStack(Material.GOLD_INGOT, 1).apply {
            editMeta {
                it.displayName(Component.text("现金账户管理").color(primaryColorVariant))
            }
        }) {
            AccountManageGui(owner, proxied, owner.account).openGUI()
        })
        put(2, GuiElement(item = ItemStack(Material.WHITE_BANNER).apply {
            editMeta {
                it.displayName(Component.text("国家管理").color(primaryColorVariant))
            }
        }) {
            CountrySelectGui(owner, proxied).openGUI()
        })
        put(8, GuiElement(item = ItemStack(Material.MAP).apply {
            editMeta {
                it.displayName(Component.text("启用或关闭“同行者羁绊”").color(primaryColor))
                it.lore(buildList {
                    add(
                        Component.text("您将能获得与您在同一分钟内重生的前一位玩家的大致位置")
                            .color(primaryColor)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                    add(Component.text("在当前人生的 100 分钟内有效")
                        .color(primaryColor)
                        .decoration(TextDecoration.ITALIC, false))
                    add(
                        Component.text("当前状态：${owner.enableCompanion}")
                            .color(primaryColorVariant)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                    if (owner.enableCompanion) {
                        add(
                            Component.text("同行者：${owner.companionHuman?.name}")
                                .color(primaryColorVariant)
                                .decoration(TextDecoration.ITALIC, false)
                        )
                    }
                    add(Component.text("----------").decoration(TextDecoration.ITALIC, false))
                    add(
                        Component.text("点击以切换状态")
                            .color(secondaryColorVariant)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                })
            }
        }) {
            if (!owner.enableCompanion && owner.aliveTimeSeconds >= 100 * 60) {
                proxied.sendMessage(Component.text("由于时间太过久远，与同行者的羁绊似乎完全消失了...").color(NamedTextColor.RED))
            } else if (!owner.enableCompanion && owner.companionHuman == null) {
                proxied.sendMessage(Component.text("无法启用同行者羁绊，找不到与您相关的同行者").color(NamedTextColor.RED))
            }
            owner.enableCompanion = !owner.enableCompanion && owner.companionHuman != null
            reloadInventory()
        })
        put(18, GuiElement(item = ItemStack(Material.DRAGON_HEAD).apply {
            editMeta {
                it.displayName(Component.text("大声喊话").color(primaryColor))
                it.lore(buildList {
                    add(
                        Component.text("消耗 2 点饥饿度，对半径 500 米以内的其他人喊话")
                            .color(primaryColor)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                    add(
                        Component.text("这将向他人揭示您的大致位置")
                            .color(primaryColorVariant)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                })
            }
        }, close = true) {
            owner.technicalPlayer.bukkitPlayer.player?.let { player ->
                buildConversation(proxied) {
                    add(ConversationSection(
                        prompt = { _ -> Component.text("请输入要喊话的内容").color(secondaryColor) },
                        acceptInput = { input, _ ->
                            input?.let { i ->
                                broadcastMessageSmall(player, owner, i, proxied)
                            }
                            mapOf()
                        }
                    ))
                }.begin()
            }
        })
        put(19, GuiElement(item = ItemStack(Material.DRAGON_HEAD).apply {
            editMeta {
                it.displayName(Component.text("吼破嗓子的喊话").color(primaryColor))
                it.lore(buildList {
                    add(
                        Component.text("消耗 10 点饥饿度，对所有其他人喊话").color(primaryColor)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                })
            }
        }, close = true) {
            owner.technicalPlayer.bukkitPlayer.player?.let { player ->
                buildConversation(proxied) {
                    add(ConversationSection(
                        prompt = { _ -> Component.text("请输入要喊话的内容").color(secondaryColor) },
                        acceptInput = { input, _ ->
                            input?.let { i ->
                                broadcastMessageLarge(player, owner, i, proxied)
                            }
                            mapOf()
                        }
                    ))
                }.begin()
            }
        })
        exitButton()
    }

    init {
        initInventory()
    }

    inner class AbilitySelectGui : GuiBase(owner, proxied, title = Component.text("选择一个能力").color(primaryColor)) {

        override val builder: MutableMap<Int, GuiElement>.() -> Unit = {
            owner.candidateAbilities.forEachIndexed { idx, ability ->
                put(19 + idx * 3, GuiElement(item = ItemStack(Material.PAPER).apply {
                    editMeta { meta ->
                        meta.displayName(
                            if (owner.technicalPlayer.humans.size <= 100) {
                                Component.text(ability.displayName).color(primaryColorVariant)
                            } else {
                                Component.text(ability.ambiguousName).color(primaryColorVariant)
                            }
                        )
                    }
                }) {
                    owner.abilitiesList = owner.abilitiesList.toMutableList().apply { add(ability) }.toList()
                    owner.hasSelectedCandidateAbilities = true
                    this@PlayerSelfGui.reloadInventory()
                })
            }
        }

        init {
            initInventory()
        }

    }

}