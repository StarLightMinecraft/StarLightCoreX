package io.hikarilan.starlightcorex.generic.bossbar

import io.hikarilan.starlightcorex.StarLightCoreX
import io.hikarilan.starlightcorex.person.Human
import io.hikarilan.starlightcorex.person.events.HumanDeathEvent
import io.hikarilan.starlightcorex.utils.GeneralUtils.registerListener
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * 带值的本地化 BossBar
 *
 * @param value 初始化值
 * @param maxValue 最大值
 */
open class ValuedLocaledBossBar(
    name: Component,
    color: BossBar.Color,
    overlay: BossBar.Overlay,
    flags: Set<BossBar.Flag> = mutableSetOf(),
    private var maxValue: Long,
    private var value: Long,
) : Listener {

    protected val bossBar: BossBar = BossBar.bossBar(name, value.toFloat() / maxValue, color, overlay, flags)

    fun value() = value

    fun value(value: Long) {
        this.value = value.coerceIn(0..maxValue)
        updateProgress()
    }

    fun maxValue() = maxValue

    fun maxValue(maxValue: Long) {
        this.maxValue = value.coerceAtLeast(0)
        updateProgress()
    }

    private fun updateProgress() {
        bossBar.progress(value.toFloat() / maxValue)
    }

    val players = mutableListOf<Human>()

    fun addPlayer(player: Human) {
        players.add(player)
        player.technicalPlayer.bukkitPlayer.player?.showBossBar(bossBar)
    }

    fun removePlayer(player: Human) {
        players.remove(player)
        player.technicalPlayer.bukkitPlayer.player?.hideBossBar(bossBar)
    }

    fun removeAll() {
        players.forEach {
            it.technicalPlayer.bukkitPlayer.player?.hideBossBar(bossBar)
        }
        players.clear()
    }

    @EventHandler
    fun onDeath(e: HumanDeathEvent) {
        removePlayer(e.human)
    }

    init {
        registerListener(StarLightCoreX.instance)
    }

}