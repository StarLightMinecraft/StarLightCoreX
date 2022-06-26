package io.hikarilan.starlightcorex.generic.mechanism

import io.hikarilan.starlightcorex.StarLightCoreX
import io.hikarilan.starlightcorex.person.TechnicalPlayer
import io.hikarilan.starlightcorex.storage.getStorageFor
import io.hikarilan.starlightcorex.utils.GeneralUtils.registerListener
import io.hikarilan.starlightcorex.utils.PluginInitializeModule
import io.papermc.paper.event.player.PlayerTradeEvent
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.AbstractVillager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.entity.ExpBottleEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object Mechanisms : PluginInitializeModule, Listener {

    override fun init(plugin: StarLightCoreX) {
        registerListener(plugin)
    }

    @EventHandler
    private fun onThrowExpBottle(e: ExpBottleEvent) {
        val exp = e.entity.item.itemMeta.persistentDataContainer.get(
            NamespacedKey(StarLightCoreX.instance, "exp"),
            PersistentDataType.INTEGER
        ) ?: return
        e.experience = exp
        e.entity.location.world.dropItem(e.entity.location, ItemStack(Material.GLASS_BOTTLE, e.entity.item.amount))
    }

    @EventHandler
    private fun onCraftExpBottle(e: PlayerInteractEvent) {
        if (e.action != Action.RIGHT_CLICK_BLOCK) return
        if (e.hand != EquipmentSlot.HAND) return
        if (e.item?.type != Material.GLASS_BOTTLE) return
        if (e.clickedBlock?.type != Material.GRINDSTONE) return
        e.isCancelled = true
        if (e.player.totalExperience < 20) {
            e.player.sendMessage(Component.text("经验不足").color(NamedTextColor.RED))
            return
        }
        e.player.inventory.setItem(EquipmentSlot.HAND, e.item!!.subtract())
        e.player.giveExp(-20)
        e.player.inventory.addItem(ItemStack(Material.EXPERIENCE_BOTTLE, 1).apply {
            editMeta {
                it.displayName(Component.text("平庸的附魔之瓶").color(NamedTextColor.YELLOW))
                it.persistentDataContainer.set(
                    NamespacedKey(StarLightCoreX.instance, "exp"),
                    PersistentDataType.INTEGER,
                    15
                )
            }
        })
        e.player.world.playSound(
            Sound.sound(org.bukkit.Sound.BLOCK_ANVIL_USE, Sound.Source.BLOCK, 1.0f, 1.0f),
            e.clickedBlock!!.location.x,
            e.clickedBlock!!.location.y,
            e.clickedBlock!!.location.z
        )
    }

    @EventHandler
    private fun onDuplicateBook(e: PlayerInteractEvent) {
        if (e.action != Action.RIGHT_CLICK_BLOCK) return
        if (e.hand != EquipmentSlot.HAND) return
        if (e.item?.type != Material.WRITTEN_BOOK) return
        if (e.clickedBlock?.type != Material.CARTOGRAPHY_TABLE) return
        e.isCancelled = true
        if (!e.player.inventory.contains(Material.WRITABLE_BOOK)) {
            e.player.sendMessage(Component.text("需要额外的书与笔才能复制书本").color(NamedTextColor.RED))
            return
        }
        if (e.player.totalExperience < 1) {
            e.player.sendMessage(Component.text("经验不足").color(NamedTextColor.RED))
            return
        }
        e.player.giveExp(-1)
        e.player.inventory.removeItem(ItemStack(Material.WRITABLE_BOOK, 1))
        e.player.inventory.addItem(ItemStack(Material.WRITTEN_BOOK).apply { itemMeta = e.item!!.itemMeta })
    }

    @EventHandler
    private fun onDuplicateMap(e: PlayerInteractEvent) {
        if (e.action != Action.RIGHT_CLICK_BLOCK) return
        if (e.hand != EquipmentSlot.HAND) return
        if (e.item?.type != Material.FILLED_MAP) return
        if (e.clickedBlock?.type != Material.CARTOGRAPHY_TABLE) return
        e.isCancelled = true
        if (!e.player.inventory.contains(Material.MAP)) {
            e.player.sendMessage(Component.text("需要额外的空地图才能复制地图").color(NamedTextColor.RED))
            return
        }
        if (e.player.totalExperience < 1) {
            e.player.sendMessage(Component.text("经验不足").color(NamedTextColor.RED))
            return
        }
        e.player.giveExp(-1)
        e.player.inventory.removeItem(ItemStack(Material.MAP, 1))
        e.player.inventory.addItem(ItemStack(Material.FILLED_MAP).apply { itemMeta = e.item!!.itemMeta })
    }

    @EventHandler
    private fun onPlayerTrade(e: PlayerTradeEvent) {
        if (e.trade.result.type != Material.EMERALD) return
        val dataContainer = e.villager.persistentDataContainer
        // 0 FOR FALSE, 1 FOR TRUE
        val isActive =
            dataContainer.get(NamespacedKey(StarLightCoreX.instance, "active"), PersistentDataType.INTEGER) ?: 1
        val tradeTime =
            dataContainer[NamespacedKey(StarLightCoreX.instance, "trade_time"), PersistentDataType.INTEGER] ?: 0
        if (isActive == 0) {
            e.isCancelled = true
            e.player.sendMessage(Component.text("无法交易，因为此商人需要重新激活").color(NamedTextColor.RED))
        }
        if (tradeTime > 64) {
            e.isCancelled = true
            e.player.sendMessage(Component.text("此商人已无法进行任何交易").color(NamedTextColor.RED))
        }
        dataContainer[NamespacedKey(StarLightCoreX.instance, "trade_time"), PersistentDataType.INTEGER] = tradeTime + 1
        if (tradeTime + 1 % 16 == 0) {
            dataContainer[NamespacedKey(StarLightCoreX.instance, "active"), PersistentDataType.INTEGER] = 0
            e.player.sendMessage(Component.text("由于过度交易，此商人已被锁定。需要重新激活才能继续进行交易").color(NamedTextColor.RED))
        }
    }

    @EventHandler
    private fun onRightClickVillager(e: PlayerInteractAtEntityEvent) {
        val villager = e.rightClicked as? AbstractVillager ?: return
        val player = getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman ?: return
        if (!player.hasAbility("generic.tamer")) return
        val dataContainer = villager.persistentDataContainer
        val isActive =
            dataContainer.get(NamespacedKey(StarLightCoreX.instance, "active"), PersistentDataType.INTEGER) ?: 1
        if (isActive == 1) return
        e.isCancelled = true
        e.player.world.playSound(
            Sound.sound(org.bukkit.Sound.ENTITY_VILLAGER_YES, Sound.Source.VOICE, 1.0f, 1.0f),
            e.player.location.x, e.player.location.y, e.player.location.z
        )
        dataContainer[NamespacedKey(StarLightCoreX.instance, "active"), PersistentDataType.INTEGER] = 1
        e.player.sendMessage(Component.text("商人已重新激活").color(NamedTextColor.GREEN))
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private fun onTame(e: EntityBreedEvent) {
        if (e.father !is AbstractVillager || e.mother !is AbstractVillager) return
        val fatherTrade = e.father.persistentDataContainer[NamespacedKey(
            StarLightCoreX.instance,
            "trade_time"
        ), PersistentDataType.INTEGER] ?: 0
        val motherTrade = e.mother.persistentDataContainer[NamespacedKey(
            StarLightCoreX.instance,
            "trade_time"
        ), PersistentDataType.INTEGER] ?: 0
        e.entity.persistentDataContainer[NamespacedKey(
            StarLightCoreX.instance,
            "trade_time"
        ), PersistentDataType.INTEGER] = (fatherTrade / 2 + motherTrade / 2) / 2
    }
}