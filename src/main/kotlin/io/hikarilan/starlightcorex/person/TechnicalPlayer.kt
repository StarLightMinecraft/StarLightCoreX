package io.hikarilan.starlightcorex.person

import io.hikarilan.starlightcorex.generic.RootSerializable
import io.hikarilan.starlightcorex.generic.Unique
import io.hikarilan.starlightcorex.generic.exception.PlayerNotOnlineException
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.conversations.Conversable
import org.bukkit.conversations.Conversation
import org.bukkit.conversations.ConversationAbandonedEvent
import org.bukkit.inventory.Inventory
import java.util.*

/**
 * 技术意义上的玩家
 *
 * 该对象直接与 Bukkit 的玩家对象挂钩
 *
 * **这是一个主要序列化对象，应当被直接存储**
 */
data class TechnicalPlayer(
    override val uniqueId: UUID,
    // 最后一个 Human 对象应为当前 Human 对象
    val humans: MutableList<Human> = mutableListOf(
        Human(upstream = uniqueId)
    )
) : Unique, RootSerializable, Conversable {

    constructor(player: org.bukkit.entity.Player) : this(player.uniqueId)

    val bukkitPlayer: OfflinePlayer
        get() = org.bukkit.Bukkit.getOfflinePlayer(uniqueId)

    val isOnline: Boolean
        get() = bukkitPlayer.isOnline

    val isOp: Boolean
        get() = bukkitPlayer.isOp

    val inventory: Inventory
        @Throws(PlayerNotOnlineException::class)
        get() = bukkitPlayer.player?.inventory ?: throw PlayerNotOnlineException(this)

    val currentHuman: Human
        get() = humans.last()

    val location: Location
        @Throws(PlayerNotOnlineException::class)
        get() = bukkitPlayer.player?.location ?: throw PlayerNotOnlineException(this)

    fun nextGeneration(): Human {
        val human = Human(upstream = uniqueId)
        humans.add(human)
        return human
    }

    fun sendMessage(message: Component) {
        bukkitPlayer.player?.sendMessage(message)
    }

    fun openInventory(inventory: Inventory) {
        bukkitPlayer.player?.openInventory(inventory)
    }

    fun closeInventory() {
        bukkitPlayer.player?.closeInventory()
    }

    fun modifyDisplayName(name: Component) {
        bukkitPlayer.player?.let {
            it.displayName(name)
            it.playerListName(name)
        }
    }

    var compassTarget:Location
        get() = bukkitPlayer.player?.compassTarget ?: throw PlayerNotOnlineException(this)
        set(value) {
            bukkitPlayer.player?.compassTarget = value
        }

    override fun isConversing(): Boolean = bukkitPlayer.player?.isConversing ?: false

    override fun acceptConversationInput(input: String) {
        bukkitPlayer.player?.acceptConversationInput(input)
    }

    override fun beginConversation(conversation: Conversation): Boolean =
        bukkitPlayer.player?.beginConversation(conversation) ?: false

    override fun abandonConversation(conversation: Conversation) {
        bukkitPlayer.player?.abandonConversation(conversation)
    }

    override fun abandonConversation(conversation: Conversation, details: ConversationAbandonedEvent) {
        bukkitPlayer.player?.abandonConversation(conversation, details)
    }

    override fun sendRawMessage(message: String) {
        bukkitPlayer.player?.sendRawMessage(message)
    }

    override fun sendRawMessage(sender: UUID?, message: String) {
        bukkitPlayer.player?.sendRawMessage(message)
    }

}