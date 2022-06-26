package io.hikarilan.starlightcorex.gui

import io.hikarilan.starlightcorex.person.Human
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

fun MutableMap<Int, GuiBase.GuiElement>.exitButton(slot: Int = 53, action: (InventoryClickEvent) -> Unit = { }) {
    put(slot, GuiBase.GuiElement(item = ItemStack(Material.BARRIER, 1).apply {
        editMeta {
            it.displayName(Component.text("退出").color(NamedTextColor.RED))
        }
    }, close = true, action = action))
}

fun MutableMap<Int, GuiBase.GuiElement>.adminButton(
    slot: Int = 52, owner: Human, action: (InventoryClickEvent) -> Unit
) {
    if (owner.hasPermission("starlightcorex.admin")) {
        put(slot, GuiBase.GuiElement(item = ItemStack(Material.COBWEB, 1).apply {
            editMeta {
                it.displayName(Component.text("管理员操作").color(NamedTextColor.DARK_RED))
            }
        }, action = action))
    }
}

abstract class GuiBase(
    val owner: Human,
    val proxied: Human,
    val title: Component,
) {

    abstract val builder: MutableMap<Int, GuiElement>.() -> Unit

    val holder by lazy { GuiInventoryHolderDefaultImpl(this) }

    protected open val elements by lazy { buildMap(builder) }

    protected fun initInventory() {
        holder.inventory.clear()
        elements.forEach { (k, v) ->
            holder.inventory.setItem(k, v.item)
        }
    }

    open fun reloadInventory(){
        holder.inventory.clear()
        buildMap(builder).forEach { (k, v) ->
            holder.inventory.setItem(k, v.item)
        }
        openGUI()
    }

    open fun openGUI() {
        proxied.openGUI(this)
    }

    open fun closeGUI() {
        proxied.closeGUI()
    }

    /**
     * @return close inventory
     */
    fun preformAction(event: InventoryClickEvent, slot: Int) {
        elements[slot]?.also {
            if (it.close) {
                proxied.closeGUI()
            }
            it.action.invoke(event)
        }
    }

    inner class GuiInventoryHolderDefaultImpl(
        override val gui: GuiBase
    ) : GuiInventoryHolder {

        private val pInventory: Inventory = Bukkit.createInventory(this, 6 * 9, title)

        override fun getInventory(): Inventory = pInventory

    }

    data class GuiElement(
        val item: ItemStack,
        val close: Boolean = false,
        val action: (InventoryClickEvent) -> Unit = { },
    )

}

interface GuiInventoryHolder : InventoryHolder {

    val gui: GuiBase

}

