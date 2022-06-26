package io.hikarilan.starlightcorex.gui

import io.hikarilan.starlightcorex.StarLightCoreX
import io.hikarilan.starlightcorex.person.TechnicalPlayer
import io.hikarilan.starlightcorex.person.gui.PlayerOtherGui
import io.hikarilan.starlightcorex.person.gui.PlayerSelfGui
import io.hikarilan.starlightcorex.storage.getStorageFor
import io.hikarilan.starlightcorex.utils.GeneralUtils.registerListener
import io.hikarilan.starlightcorex.utils.PluginInitializeModule
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent

object GuiManager : PluginInitializeModule, Listener {

    override fun init(plugin: StarLightCoreX) {
        registerListener(plugin)
    }

    @EventHandler
    private fun onShiftClickSelf(e: PlayerInteractEvent) {
        if (!e.player.isSneaking || e.action != Action.RIGHT_CLICK_AIR || e.player.inventory.itemInOffHand.type != Material.AIR) return
        getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
            PlayerSelfGui(it, it).openGUI()
        }
    }

    @EventHandler
    private fun onShiftClickOther(e: PlayerInteractEntityEvent) {
        if (!e.player.isSneaking || e.rightClicked !is Player) return
        getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
            PlayerOtherGui(
                it,
                it,
                getStorageFor<TechnicalPlayer>(e.rightClicked.uniqueId)?.currentHuman ?: return
            ).openGUI()
        }
    }

    @EventHandler
    private fun onClickInventory(e: InventoryClickEvent) {
        if (e.clickedInventory?.holder is GuiInventoryHolder || e.inventory.holder is GuiInventoryHolder) {
            e.isCancelled = true
        }
        if (e.clickedInventory?.holder !is GuiInventoryHolder) return
        val holder = e.clickedInventory?.holder as GuiInventoryHolder
        holder.gui.preformAction(e, e.slot)
    }
}