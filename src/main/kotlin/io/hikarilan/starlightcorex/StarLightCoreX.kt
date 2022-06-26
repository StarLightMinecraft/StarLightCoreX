package io.hikarilan.starlightcorex

import io.hikarilan.starlightcorex.generic.mechanism.Mechanisms
import io.hikarilan.starlightcorex.gui.GuiManager
import io.hikarilan.starlightcorex.person.PersonManager
import io.hikarilan.starlightcorex.person.ability.AbilityManager
import io.hikarilan.starlightcorex.storage.GenericStorage
import io.hikarilan.starlightcorex.utils.*
import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

class StarLightCoreX : JavaPlugin() {

    companion object {
        lateinit var instance: StarLightCoreX
    }

    private val initializeModules = listOf(
        MailUtils,
        GenericStorage,
        PersonManager,
        GlobalLogger,
        GuiManager,
        ConversationUtils,
        Tags,
        AbilityManager,
        Mechanisms
    )

    private var currentTick = 0L
    private lateinit var task: BukkitTask

    override fun onEnable() {
        instance = this
        saveDefaultConfig()
        initializeModules.forEach { it.init(this) }
        task = scheduler.runTaskTimer(this, Runnable {
            initializeModules.forEach { it.tick(this, currentTick) }
            currentTick++
        }, 0L, 1L)
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            Bukkit.getWorlds().forEach {
                it.worldBorder.size = 10000.0
            }
        }
    }

    override fun onDisable() {
        if (!task.isCancelled) {
            task.cancel()
            currentTick = 0
        }
        initializeModules.forEach { it.destroy(this) }
    }

}