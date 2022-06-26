package io.hikarilan.starlightcorex.person

import io.hikarilan.starlightcorex.StarLightCoreX
import io.hikarilan.starlightcorex.generic.bossbar.AutoIncreasedValuedLocaledBossBar
import io.hikarilan.starlightcorex.person.events.HumanDeathEvent
import io.hikarilan.starlightcorex.politics.country.Country
import io.hikarilan.starlightcorex.storage.getAllStorageFor
import io.hikarilan.starlightcorex.storage.getOrCreateStorageFor
import io.hikarilan.starlightcorex.storage.getStorageFor
import io.hikarilan.starlightcorex.utils.*
import io.hikarilan.starlightcorex.utils.GeneralUtils.nextSign
import io.hikarilan.starlightcorex.utils.GeneralUtils.registerListener
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.entity.Boat
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*
import org.bukkit.event.player.PlayerRespawnEvent.RespawnFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.map.MapView
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoField
import java.util.*
import kotlin.math.roundToInt
import kotlin.random.Random

object PersonManager : PluginInitializeModule, Listener {

    private lateinit var plugin: StarLightCoreX

    private val respawning = mutableSetOf<Player>()

    private val recentlyJoined = mutableSetOf<Pair<LocalDateTime, Location>>()
    private val recentlyRespawned = mutableListOf<Human>()
    private val invulnerablePlayers = mutableSetOf<Player>()

    lateinit var scoreboard: Scoreboard
    private lateinit var mainTeam: Team
    lateinit var aliveObjective: Objective

    //private lateinit var task: BukkitTask
    private lateinit var aliveTask: BukkitTask

    override fun init(plugin: StarLightCoreX) {
        this.plugin = plugin
        registerListener(plugin)
        scoreboard = Bukkit.getScoreboardManager().newScoreboard
        mainTeam = scoreboard.let {
            it.getTeam("starlightcorex") ?: it.registerNewTeam("starlightcorex")
        }
        mainTeam.setCanSeeFriendlyInvisibles(false)
        mainTeam.setAllowFriendlyFire(true)
        mainTeam.setOption(Team.Option.DEATH_MESSAGE_VISIBILITY, Team.OptionStatus.ALWAYS)
        mainTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS)
        mainTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER)
        Bukkit.getOnlinePlayers().forEach {
            it.scoreboard = scoreboard
            mainTeam.addPlayer(it)
        }
//        aliveObjective = scoreboard.let {
//            it.getObjective("alive_time") ?: it.registerNewObjective(
//                "alive_time", "dummy", Component.text("存活时间（分钟）").color(
//                    primaryColor
//                )
//            )
//        }.also {
//            it.displaySlot = DisplaySlot.SIDEBAR
//        }
        aliveTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            Bukkit.getOnlinePlayers().mapNotNull { getStorageFor<TechnicalPlayer>(it.uniqueId) }.map { it.currentHuman }
                .filter { it.deathTime == null }
                .forEach {
                    it.aliveTimeSeconds++
                }
        }, 0, 1 * 20L)
//        getAllStorageFor<Human>().associateWith { it.aliveTimeSeconds }.forEach {
//            aliveObjective.getScore(it.key.name).score = it.value / 60
//        }
//        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
//            Bukkit.getOnlinePlayers().mapNotNull { getStorageFor<TechnicalPlayer>(it.uniqueId) }.map { it.currentHuman }
//                .associateWith { it.aliveTimeSeconds }.forEach {
//                    aliveObjective.getScore(it.key.name).score = it.value / 60
//                }
//        }, 0, 1L)
    }

    override fun tick(plugin: StarLightCoreX, currentTick: Long) {
        if (currentTick % (10 * 20L) != 0L) return
        Bukkit.getOnlinePlayers()
            .mapNotNull { getStorageFor<TechnicalPlayer>(uniqueId = it.uniqueId)?.currentHuman }
            .filter {
                it.enableCompanion
                        && it.companionHuman != null
                        && it.companionHuman?.isOnline() == true
                        && it.companionHuman == it.companionHuman?.technicalPlayer?.currentHuman
            }
            .forEach {
                if (it.aliveTimeSeconds >= 100 * 60) {
                    it.enableCompanion = false
                }
                it.sendMessage(
                    Component.text(
                        "同行者羁绊：据您 ${
                            it.location.distance(it.companionHuman!!.location).toInt()
                        }米，位于向北 ${
                            GeneralUtils.getDirectionBetween(
                                it.location,
                                it.companionHuman!!.location
                            ).roundToInt()
                        }° 角方向"
                    ).color(
                        secondaryColorVariant
                    )
                )
            }
    }

    override fun destroy(plugin: StarLightCoreX) {
        if (!aliveTask.isCancelled) {
            aliveTask.cancel()
        }
//        if (!task.isCancelled) {
//            task.cancel()
//        }
        scoreboard.entries.forEach { scoreboard.resetScores(it) }
    }

    @EventHandler
    private fun onAnnounceAdvancements(e: PlayerAdvancementDoneEvent) {
        getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also { human ->
            e.message()?.let {
                return@let (if (it is TranslatableComponent) it.args(
                    it.args().toMutableList().apply {
                        set(0, Component.text(human.name))
                    }) else it)
                    .replaceText { config ->
                        config.matchLiteral(human.technicalPlayer.bukkitPlayer.name)
                            .replacement(Component.text(human.name))
                    }
            }.also { it?.let { d -> e.message(Component.text("🥇").append(d)) } }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private fun onChat(e: AsyncChatEvent) {
        e.isCancelled = true
        e.viewers()
            .filter { it is Player && it.world == e.player.world && it.location.distanceSquared(e.player.location) <= 50 * 50 }
            .forEach {
                it.sendMessage(
                    Component.text("💬").append(e.player.displayName())
                        .append(Component.text(" >> ").color(NamedTextColor.WHITE)).append(e.message())
                )
            }
        Bukkit.getConsoleSender().sendMessage(
            e.player.displayName().append(Component.text("(")).append(e.player.name()).append(Component.text(")"))
                .append(Component.text(" >> ").color(NamedTextColor.WHITE)).append(e.message())
        )
    }

    @EventHandler
    private fun onJoin(e: PlayerJoinEvent) {
        var isFirstJoin = false
        if (getStorageFor<TechnicalPlayer>(e.player.uniqueId) == null) {
            e.player.teleport(e.player.world.spawnLocation.apply { this.y = 256.0 })
            e.player.sendMessage(
                Component.text("在经过长途跋涉后，你终于来到了这片令你魂牵梦绕的地方，即使你从未意识到接下来发生的一切有多么的可怕... “欢迎来到新大陆，旧大陆人”")
                    .color(primaryColorVariant)
            )
            isFirstJoin = true
        }
        getOrCreateStorageFor(e.player.uniqueId) { TechnicalPlayer(e.player) }.currentHuman.also {
            if (isFirstJoin) {
                respawn(it)
                it.updateCompass()
            }
            it.syncName()
            e.joinMessage(
                Component.text("\uD83D\uDCCB").append(Component.text("${it.name} 加入了服务器").color(primaryColor))
            )
            invulnerablePlayers.add(e.player)
            e.player.allowFlight = true
            e.player.isFlying = true
            e.player.isInvulnerable = true
            AutoIncreasedValuedLocaledBossBar(
                name = Component.text("无敌").color(primaryColor),
                color = BossBar.Color.RED,
                overlay = BossBar.Overlay.PROGRESS,
                maxValue = 5 * 20L,
                onFinishProgress = {
                    e.player.isFlying = false
                    e.player.allowFlight = false
                    e.player.isInvulnerable = false
                    invulnerablePlayers.remove(e.player)
                }
            ).also { bossbar -> bossbar.addPlayer(it) }
        }
        e.player.scoreboard = scoreboard
        mainTeam.addPlayer(e.player)
    }

    @EventHandler
    private fun onQuit(e: PlayerQuitEvent) {
        getOrCreateStorageFor(e.player.uniqueId) { TechnicalPlayer(e.player) }.currentHuman.also {
            e.quitMessage(
                Component.text("\uD83D\uDCCB").append(Component.text("${it.name} 离开了服务器").color(primaryColor))
            )
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private fun onDeath(e: PlayerDeathEvent) {
        getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also { human ->
            HumanDeathEvent(human).callEvent()
            human.deathTime = System.currentTimeMillis()
            human.deathInventory = e.player.inventory.contents?.toList() // Suppress
            human.deathEnderChestInventory = e.player.enderChest.contents?.toList() // Suppress
            e.player.enderChest.clear()
            human.abandonConversation()
            log(
                (e.deathMessage()
                    ?: Component.empty()).append(Component.text("(human: ${human.name} with uniqueId ${human.uniqueId})"))
            )
            e.deathMessage()?.let {
                return@let (if (it is TranslatableComponent) it.args(
                    it.args().toMutableList().apply {
                        set(0, Component.text(human.name))
                        if (e.player.killer != null && this.size > 1) {
                            set(
                                1,
                                Component.text(
                                    getStorageFor<TechnicalPlayer>(e.player.killer!!.uniqueId)?.currentHuman?.name
                                        ?: return@apply
                                )
                            )
                        }
                    }) else it)
                    .replaceText { config ->
                        config.matchLiteral(human.technicalPlayer.bukkitPlayer.name)
                            .replacement(Component.text(human.name))
                    }
                    .replaceText { config ->
                        config.matchLiteral(human.technicalPlayer.bukkitPlayer.player?.killer.toString()).replacement(
                            Component.text(
                                getStorageFor<TechnicalPlayer>(
                                    human.technicalPlayer.bukkitPlayer.player?.killer?.uniqueId ?: UUID.randomUUID()
                                )?.currentHuman?.name ?: ""
                            )
                        )
                    }
            }.also {
                it?.let { d ->
                    e.deathMessage(
                        Component.text("\uD83D\uDC80").append(d).color(NamedTextColor.GRAY)
                    )
                }
            }
            human.sendMessage(Component.text("你死了，你曾经存活了 ${human.aliveTimeSeconds / 60} 分钟").color(NamedTextColor.RED))
        }
        e.itemsToKeep.clear()
        e.drops.removeIf { Random.nextBoolean() }
        e.setShouldDropExperience(false)
    }

    @EventHandler
    private fun onInteract(e: PlayerInteractEvent) {
        if (respawning.contains(e.player)) {
            e.isCancelled = true
        }
        if (invulnerablePlayers.contains(e.player)) {
            e.isCancelled = true
        }
    }

    @EventHandler
    private fun onMove(e: PlayerMoveEvent) {
        if (respawning.contains(e.player)) {
            e.isCancelled = true
        }
        if (invulnerablePlayers.contains(e.player)) {
            e.isCancelled = true
        }
        val player = getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman ?: return
        getAllStorageFor<Country>().filter { country ->
            country.regions.any { it.checkIn(e.from) } && country.regions.none() {
                it.checkIn(
                    e.to
                )
            }
        }.forEach { country ->
            // 出境
            country.leaveMessage?.let { player.sendMessage(it) }
        }
        getAllStorageFor<Country>().filter { country ->
            country.regions.any { it.checkIn(e.to) } && country.regions.none() {
                it.checkIn(
                    e.from
                )
            }
        }.forEach { country ->
            // 入境
            country.enterMessage?.let { player.sendMessage(it) }
        }
/*        if (e.hasChangedPosition() && !e.to.world.worldBorder.isInside(e.to)) {
            e.player.sendMessage(Component.text("前面的区域，以后再来探索吧").color(primaryColor))
            e.player.velocity = e.player.location.direction.clone().normalize().multiply(-1)
            //e.isCancelled = true
            if (e.player.vehicle != null) {
                e.player.vehicle!!.velocity = e.player.location.direction.clone().normalize().multiply(-1)
            }
        }*/
    }

/*    @EventHandler
    private fun onEntityMove(e:EntityMoveEvent){
        if (e.hasChangedPosition() && !e.to.world.worldBorder.isInside(e.to)) {
            e.entity.velocity = e.entity.location.direction.clone().normalize().multiply(-1)
        }
    }*/

    @EventHandler
    private fun onRespawn(e: PlayerRespawnEvent) {
        if (RespawnFlag.END_PORTAL in e.respawnFlags) return
        e.respawnLocation = e.player.world.spawnLocation.apply { this.y = 256.0 }
        getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.also {
            it.nextGeneration()
            it.currentHuman.syncName()
            it.currentHuman.updateCompass()
            respawn(it.currentHuman)
        }
        e.player.scoreboard = scoreboard
        mainTeam.addPlayer(e.player)
    }

//    private fun respawn(player: Player, isFirst: Boolean) {
//        // 第一次
//        if (isFirst) {
//            recentlyJoined.find {
//                it.first[ChronoField.DAY_OF_YEAR] == LocalDateTime.now()[ChronoField.DAY_OF_YEAR]
//                        && LocalDateTime.now()[ChronoField.MINUTE_OF_DAY] - it.first[ChronoField.MINUTE_OF_DAY] <= 10
//            }?.let { randomRespawn(player, it.second) } ?: let {
//                recentlyJoined.add(Pair(LocalDateTime.now(), randomRespawn(player)))
//            }
//        }
//        // 不是第一次，在附近重生
//        else if (Random.nextDouble() <= 0.5) {
//            recentlyJoined.find {
//                it.first[ChronoField.DAY_OF_YEAR] == LocalDateTime.now()[ChronoField.DAY_OF_YEAR]
//                        && LocalDateTime.now()[ChronoField.MINUTE_OF_DAY] == it.first[ChronoField.MINUTE_OF_DAY]
//            }?.let { randomRespawn(player, it.second, 100.0) } ?: let {
//                recentlyJoined.add(Pair(LocalDateTime.now(), randomRespawn(player)))
//            }
//        }
//        // 不是第一次，但同时重生
//        else if (Random.nextDouble() <= 0.1) {
//            recentlyJoined.find {
//                it.first[ChronoField.DAY_OF_YEAR] == LocalDateTime.now()[ChronoField.DAY_OF_YEAR]
//                        && LocalDateTime.now()[ChronoField.MINUTE_OF_DAY] == it.first[ChronoField.MINUTE_OF_DAY]
//            }?.let { randomRespawn(player, it.second) } ?: let {
//                recentlyJoined.add(Pair(LocalDateTime.now(), randomRespawn(player)))
//            }
//        }
//        // 随机重生
//        else {
//            randomRespawn(player)
//        }
//    }

    private fun respawn(human: Human) {
        val birthTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(human.birthTime), ZoneId.systemDefault())
        recentlyRespawned.lastOrNull()?.let {
            it != human && LocalDateTime.ofInstant(Instant.ofEpochMilli(it.birthTime), ZoneId.systemDefault())
                .let { otherBirthTime ->
                    otherBirthTime[ChronoField.DAY_OF_YEAR] == birthTime[ChronoField.DAY_OF_YEAR]
                            && otherBirthTime[ChronoField.MINUTE_OF_DAY] == birthTime[ChronoField.MINUTE_OF_DAY]
                }
        }.takeIf { it == true }?.let {
            human.companionHuman = recentlyRespawned.last()
        }
        randomRespawn(human.technicalPlayer.bukkitPlayer.player!!)
        recentlyRespawned.add(human)
    }

    private fun randomRespawn(
        player: Player,
        loc: Location = GeneralUtils.randomTeleportSafe(),
        radius: Double = -1.0
    ): Location {
        val teleport = if (radius <= 0) loc else loc.add(
            Random.nextDouble(radius) * Random.nextSign(),
            0.0,
            Random.nextDouble(radius) * Random.nextSign()
        ).toHighestLocation(HeightMap.MOTION_BLOCKING).add(0.0, 1.0, 0.0)
        val gameMode = player.gameMode
        player.gameMode = GameMode.SPECTATOR
        scheduler.runTaskAsynchronously(plugin) { _ ->
            player.sendMessage(Component.text("正在准备传送...").color(primaryColor))
            respawning.add(player)
            scheduler.runTask(plugin) { _ ->
                player.teleportAsync(teleport).whenComplete { t, _ ->
                    player.gameMode = gameMode
                    if (t) {
                        if (teleport.block.getRelative(BlockFace.DOWN).isLiquid) {
                            player.inventory.addItem(ItemStack(Material.FILLED_MAP).apply {
                                editMeta(MapMeta::class.java) {
                                    it.displayName(Component.text("航海地图"))
                                    it.mapView = Bukkit.createMap(player.world).apply {
                                        scale = MapView.Scale.NORMAL
                                        centerX = teleport.blockX
                                        centerZ = teleport.blockZ
                                        isTrackingPosition = true
                                        isUnlimitedTracking = false
                                    }
                                }
                            })
                            teleport.world.spawn(teleport, Boat::class.java) {
                                it.addPassenger(player)
                            }
                        }
                        log("${player.name}(uniqueId:${player.uniqueId}) has been respawn to ${teleport.blockX},${teleport.blockY},${teleport.blockZ}")
                        player.sendMessage(
                            Component.text("您已重生至")
                                .append(
                                    Component.text(if (player.isOp) "${teleport.blockX},${teleport.blockY},${teleport.blockZ}" else "某处随机位置")
                                        .color(NamedTextColor.RED)
                                )
                                .append(Component.text("，新的人生开始了"))
                                .color(primaryColorVariant)
                        )
                    } else {
                        player.sendMessage(Component.text("传送失败，请联系管理员").color(NamedTextColor.RED))
                    }
                    respawning.remove(player)
                }
            }
        }
        return teleport
    }
}