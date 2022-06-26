package io.hikarilan.starlightcorex.utils

import io.hikarilan.starlightcorex.StarLightCoreX
import io.hikarilan.starlightcorex.person.Human
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.conversations.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.EventExecutor
import java.util.StringJoiner


fun buildConversation(
    forWhom: Conversable,
    prefix: (ConversationContext) -> Component = { Component.text("[StarLightCoreX] ") },
    builder: MutableList<ConversationSection>.() -> Unit
): Conversation {

    val sections = buildList(builder)

    var idx = -1

    fun nextSection(): Prompt? {
        idx++
        if (idx >= sections.size) {
            return Prompt.END_OF_CONVERSATION
        }
        return object : Prompt {
            val section = sections[idx]

            override fun getPromptText(context: ConversationContext): String {
                return GeneralUtils.legacyComponentSerializer.serialize(section.prompt(context.allSessionData))
            }

            override fun blocksForInput(context: ConversationContext): Boolean {
                return !section.skipWhen(context.allSessionData)
            }

            override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
                return if (section.validate(input, context.allSessionData)) {
                    section.acceptInput(input, context.allSessionData)
                        .forEach { (k, v) -> context.setSessionData(k, v) }
                    nextSection()
                } else {
                    forWhom.sendRawMessage(
                        GeneralUtils.legacyComponentSerializer.serialize(
                            Component.text("输入有误，请重新输入").color(NamedTextColor.RED)
                        )
                    )
                    this
                }
            }

        }
    }

    return ConversationUtils.conversationFactory
        .withPrefix { GeneralUtils.legacyComponentSerializer.serialize(prefix(it)) }
        .withFirstPrompt(nextSection())
        .buildConversation(forWhom)
}

object ConversationUtils : PluginInitializeModule {

    lateinit var conversationFactory: ConversationFactory

    override fun init(plugin: StarLightCoreX) {
        conversationFactory = ConversationFactory(plugin).withEscapeSequence("exit").withTimeout(60)
    }

}

fun openAnvilInput(
    forWhom: Human,
    title: Component
) {
    val anvil =
        (forWhom.technicalPlayer.bukkitPlayer.player?.openAnvil(null, true)?.topInventory ?: return) as AnvilInventory
    anvil.firstItem = ItemStack(Material.RED_WOOL).apply {
        editMeta { it.displayName(title) }
    }
    anvil.repairCostAmount = 0
    Bukkit.getPluginManager().registerEvents(object : Listener {
        @EventHandler
        fun onClick(event: InventoryClickEvent) {
            if (event.inventory !== anvil) {
                return
            }
            if (event.clickedInventory == null) {
                return
            }
            event.isCancelled = true
            if (event.slot != 2 || event.currentItem == null) {
                return
            }
            val input = (event.inventory as AnvilInventory).renameText
            forWhom.acceptConversationInput(input ?: "")
            HandlerList.unregisterAll(this)
            anvil.close()
        }

        @EventHandler
        fun onClose(event: InventoryCloseEvent) {
            if (event.inventory !== anvil) {
                return
            }
            forWhom.abandonConversation()
            HandlerList.unregisterAll(this)
        }
    }, StarLightCoreX.instance)
}

data class ConversationSection(
    val prompt: (Map<Any, Any>) -> Component,
    val validate: (String?, Map<Any, Any>) -> Boolean = { _, _ -> true },
    val acceptInput: (String?, Map<Any, Any>) -> Map<Any, Any>,
    val skipWhen: (Map<Any, Any>) -> Boolean = { false },
)

fun messageConversationSection(
    message: (Map<Any, Any>) -> Component
) = ConversationSection(message, { _, _ -> true }, { _, _ -> emptyMap() }, { true })
