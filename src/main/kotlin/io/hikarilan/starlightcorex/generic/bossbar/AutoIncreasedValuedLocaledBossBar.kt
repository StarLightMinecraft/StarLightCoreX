package io.hikarilan.starlightcorex.generic.bossbar

import io.hikarilan.starlightcorex.StarLightCoreX
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

/**
 * 自动增减值的带值的本地化 BossBar
 *
 * @param speed 增减速度，单位 ticks
 * @param reverse 是否反转增减（默认为减少）
 */
class AutoIncreasedValuedLocaledBossBar(
    name: Component,
    color: BossBar.Color,
    overlay: BossBar.Overlay,
    flags: Set<BossBar.Flag> = mutableSetOf(),
    private var speed: Long = 1,
    private var reverse: Boolean = false,
    maxValue: Long,
    value: Long = if (reverse) 0 else maxValue,
    private val onFinishProgress: (AutoIncreasedValuedLocaledBossBar) -> Unit = {},
    private val destroyWhenFinish: Boolean = true,
) : ValuedLocaledBossBar(name, color, overlay, flags, maxValue, value) {

    private var once = false

    private lateinit var task: BukkitTask

    fun speed() = speed

    fun speed(speed: Long) {
        this.speed = speed.coerceAtLeast(0)
    }

    fun reverse() = reverse

    fun reverse(reverse: Boolean) {
        this.reverse = reverse
    }

    private fun startAutoIncrease() {
        task = Bukkit.getScheduler().runTaskTimer(StarLightCoreX.instance, Runnable {
            value(value() + speed * if (reverse) 1 else -1)
        }, 0, 1)
    }

    fun pause() {
        if (!task.isCancelled) {
            task.cancel()
        }
    }

    fun resume() {
        if (task.isCancelled) {
            startAutoIncrease()
        }
    }

    fun destroy() {
        removeAll()
        if (!task.isCancelled) {
            task.cancel()
        }
    }

    private fun registerBossBarListener() {
        bossBar.addListener(object : BossBar.Listener {
            override fun bossBarProgressChanged(bar: BossBar, oldProgress: Float, newProgress: Float) {
                if (!once && ((reverse && newProgress == BossBar.MAX_PROGRESS) || (!reverse && newProgress == BossBar.MIN_PROGRESS))) {
                    once = true
                    onFinishProgress.invoke(this@AutoIncreasedValuedLocaledBossBar)
                    if (destroyWhenFinish) {
                        destroy()
                    }
                }
            }
        })
    }

    init {
        registerBossBarListener()
        startAutoIncrease()
    }


}