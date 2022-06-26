package io.hikarilan.starlightcorex.person.gui

import io.hikarilan.starlightcorex.StarLightCoreX
import io.hikarilan.starlightcorex.economy.account.gui.AccountManageGui
import io.hikarilan.starlightcorex.gui.GuiBase
import io.hikarilan.starlightcorex.gui.adminButton
import io.hikarilan.starlightcorex.person.Human
import io.hikarilan.starlightcorex.utils.GeneralUtils.registerListener
import io.hikarilan.starlightcorex.utils.primaryColor
import io.hikarilan.starlightcorex.utils.primaryColorVariant
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack

class PlayerOtherGui(
    owner: Human,
    proxied: Human,
    private val other: Human,
) : GuiBase(
    owner = owner,
    proxied = proxied,
    title = Component.text("与 ").append(Component.text(other.name)).append(Component.text(" 交互")).color(primaryColor)
), Listener {

    override val builder: MutableMap<Int, GuiElement>.() -> Unit = {
        put(0, GuiElement(
                item = (other.technicalPlayer.bukkitPlayer.player?.itemOnCursor?.takeIf { !it.type.isAir } ?: ItemStack(Material.CARROT)).clone()
                    .apply {
                        editMeta { it.displayName(Component.text("搜身")) }
                    }) {
                other.sendMessage(Component.text("有人正在搜查你的身体...").color(primaryColorVariant))
                proxied.technicalPlayer.bukkitPlayer.player?.openInventory(
                    other.technicalPlayer.bukkitPlayer.player?.inventory ?: return@GuiElement
                )
            })
        adminButton(owner = owner, action = {
            owner.openGUI(PlayerSelfGui(owner = other, proxied = proxied))
        })
    }

    @EventHandler
    private fun onFarAway(e: PlayerMoveEvent) {
        if (!e.hasChangedPosition()) return
        if (e.player != other.technicalPlayer.bukkitPlayer) return
        if (owner != proxied && !owner.isOnline()) {
            return
        }
        if (owner.location.world != other.location.world || owner.location.distanceSquared(other.location) > 5 * 5) {
            closeGUI()
        }
    }

    @EventHandler
    private fun onHit(e: EntityDamageByEntityEvent) {
        if (e.entity != owner.technicalPlayer.bukkitPlayer) return
        closeGUI()
    }

    override fun closeGUI() {
        super.closeGUI()
        HandlerList.unregisterAll(this)
    }

    init {
        registerListener(StarLightCoreX.instance)
        initInventory()
    }

}