package io.hikarilan.starlightcorex.person

import io.hikarilan.starlightcorex.economy.account.AccountHolder
import io.hikarilan.starlightcorex.economy.account.AccountOperator
import io.hikarilan.starlightcorex.person.account.HumanCashAccount
import io.hikarilan.starlightcorex.generic.Nameable
import io.hikarilan.starlightcorex.generic.PermissibleDefaultImpl
import io.hikarilan.starlightcorex.generic.Unique
import io.hikarilan.starlightcorex.gui.GuiBase
import io.hikarilan.starlightcorex.person.ability.Ability
import io.hikarilan.starlightcorex.person.ability.AbilityHolderDefaultImpl
import io.hikarilan.starlightcorex.person.ability.AbilityManager
import io.hikarilan.starlightcorex.politics.country.Country
import io.hikarilan.starlightcorex.storage.getAllStorageFor
import io.hikarilan.starlightcorex.storage.getStorageFor
import io.hikarilan.starlightcorex.utils.DirectMailReceiver
import io.hikarilan.starlightcorex.utils.Mail
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.conversations.Conversable
import org.bukkit.conversations.Conversation
import org.bukkit.conversations.ConversationAbandonedEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.math.min

/**
 * 人类对象
 *
 * 本插件主要交互对象，用于进行大部分交互操作
 *
 * **该对象不应被直接序列化，而应跟随其上游对象一起序列化**
 * @see [TechnicalPlayer]
 */
class Human(
    // 这是 Human 的 UniqueId，不是 TechnicalPlayer 的 UUID
    override val uniqueId: UUID = UUID.randomUUID(),
    // TechnicalPlayer 的 UUID
    private val upstream: UUID,
    pName: String = "公民#${uniqueId.toString().substringAfterLast('-')}",
    override val account: HumanCashAccount = HumanCashAccount(uniqueId),
    override val permissions: MutableSet<String> = mutableSetOf(),
    override val groupPermission: MutableMap<String, MutableList<String>> = mutableMapOf(),
    override var group: String? = null,
    override val receivedMails: MutableList<Mail> = mutableListOf(),
    override var abilitiesList: List<Ability<*>> = AbilityManager.generateAbilities(),
) : Unique, Nameable, AccountHolder, AccountOperator, PermissibleDefaultImpl, DirectMailReceiver, Conversable,
    AbilityHolderDefaultImpl {

    val candidateAbilities: List<Ability<*>> = AbilityManager.generateAbilities(
        list = AbilityManager.allowedAbilities.mapNotNull { name -> AbilityManager.abilities.find { it.key == name } } - abilitiesList.toSet(),
        number = min((AbilityManager.allowedAbilities.mapNotNull { name -> AbilityManager.abilities.find { it.key == name } } - abilitiesList.toSet()).size,
            3)
    )
    var hasSelectedCandidateAbilities: Boolean = false

    @Transient
    var conversation: Conversation? = null
        private set

    var nameChangedTime = 0
        private set

    val birthTime = System.currentTimeMillis()

    var deathTime: Long? = null
    var deathInventory: List<ItemStack?>? = null
    var deathEnderChestInventory: List<ItemStack?>? = null

    var aliveTimeSeconds: Int = 0

    private var rawCompanionHuman: UUID? = null

    var companionHuman: Human?
        get() {
            return getStorageFor<Human>(rawCompanionHuman ?: return null)
        }
        set(value) {
            rawCompanionHuman = value?.uniqueId
        }

    var enableCompanion: Boolean = false

    override var name: String = pName
        set(value) {
            PersonManager.scoreboard.resetScores(field)
            field = value
            nameChangedTime++
            syncName()
        }

    val countries: List<Country>
        get() = getAllStorageFor<Country>().filter { it.hasMember(this) }

    val location: Location
        get() = getStorageFor<TechnicalPlayer>(upstream)?.location
            ?: throw IllegalStateException("TechnicalPlayer not found")

    val technicalPlayer: TechnicalPlayer
        get() = getStorageFor<TechnicalPlayer>(upstream)
            ?: throw IllegalStateException("TechnicalPlayer $upstream not found")

    override fun sendMessage(message: Component) {
        if (!isCurrentHuman()) return
        technicalPlayer.sendMessage(message)
    }

    override fun isOnline(): Boolean = technicalPlayer.isOnline

    override fun hasPermission(permission: String): Boolean {
        return if (technicalPlayer.isOp) true else super.hasPermission(permission)
    }

    private fun isCurrentHuman(): Boolean = technicalPlayer.currentHuman == this

    fun openGUI(gui: GuiBase) {
        if (!isCurrentHuman()) return
        technicalPlayer.openInventory(gui.holder.inventory)
    }

    fun closeGUI() {
        if (!isCurrentHuman()) return
        technicalPlayer.closeInventory()
    }

    fun syncName() {
        technicalPlayer.modifyDisplayName(Component.text(name))
    }

    fun updateCompass() {
        getAllStorageFor<Country>().randomOrNull()?.regions?.randomOrNull()
            ?.let {
                technicalPlayer.compassTarget =
                    Bukkit.getWorld(it.world)!!.getChunkAt(it.x, it.z).getBlock(7, 64, 7).location
            }
    }

    override fun isConversing(): Boolean {
        if (!isCurrentHuman()) return false
        return technicalPlayer.isConversing
    }

    override fun acceptConversationInput(input: String) {
        if (!isCurrentHuman()) return
        technicalPlayer.acceptConversationInput(input)
    }

    override fun beginConversation(conversation: Conversation): Boolean {
        if (!isCurrentHuman()) return false
        this.conversation = conversation
        return technicalPlayer.beginConversation(conversation)
    }

    fun abandonConversation(details: ConversationAbandonedEvent? = null) {
        if (details != null) abandonConversation(conversation ?: return, details)
        else abandonConversation(conversation ?: return)
    }

    override fun abandonConversation(conversation: Conversation) = technicalPlayer.abandonConversation(conversation)

    override fun abandonConversation(conversation: Conversation, details: ConversationAbandonedEvent) =
        technicalPlayer.abandonConversation(conversation, details)

    override fun sendRawMessage(message: String) {
        if (!isCurrentHuman()) return
        technicalPlayer.sendRawMessage(message)
    }

    override fun sendRawMessage(sender: UUID?, message: String) {
        if (!isCurrentHuman()) return
        technicalPlayer.sendRawMessage(sender, message)
    }

}
