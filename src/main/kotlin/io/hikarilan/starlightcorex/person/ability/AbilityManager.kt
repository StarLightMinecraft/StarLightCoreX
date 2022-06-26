package io.hikarilan.starlightcorex.person.ability

import com.destroystokyo.paper.event.inventory.PrepareResultEvent
import io.hikarilan.starlightcorex.StarLightCoreX
import io.hikarilan.starlightcorex.person.TechnicalPlayer
import io.hikarilan.starlightcorex.storage.getStorageFor
import io.hikarilan.starlightcorex.utils.*
import io.hikarilan.starlightcorex.utils.GeneralUtils.registerListener
import io.papermc.paper.event.entity.EntityDyeEvent
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTameEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.SmithItemEvent
import org.bukkit.event.player.*
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.GrindstoneInventory
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.meta.CompassMeta
import kotlin.math.pow
import kotlin.random.Random

object AbilityManager : PluginInitializeModule, Listener {

    lateinit var allowedAbilities: List<String>

    val abilities = listOf<Ability<*>>(
        // 伐木工
        BooleanAbility("generic.lumberjack"),
        // 建筑师
        BooleanAbility("generic.builder"),
        // 矿工
        BooleanAbility("generic.miner"),
        // 机械师
        BooleanAbility("generic.mechanic"),
        // 爆破师
        BooleanAbility("generic.explosive"),
        // 音乐家
        BooleanAbility("generic.musician"),
        // 烧炼师
        BooleanAbility("generic.smelter"),
        // 农场主
        BooleanAbility("generic.farmer"),
        // 牧场主
        BooleanAbility("generic.rancher"),
        // 猎人
        BooleanAbility("generic.hunter"),
        // 怪物猎人
        BooleanAbility("generic.monster_hunter"),
        // 渔夫
        BooleanAbility("generic.fisherman"),
        // 染色师
        BooleanAbility("generic.dye"),
        // 铁路工程师
        BooleanAbility("generic.railway_engineer"),
        // 驯兽师
        BooleanAbility("generic.tamer"),
        // 附魔师
        BooleanAbility("generic.enchanter"),
        // 工匠
        BooleanAbility("generic.artisan"),
        // 酿造师
        BooleanAbility("generic.brewer"),
        // 糕点师
        BooleanAbility("generic.confectioner"),
        // 工具制造商
        BooleanAbility("generic.tool_maker"),
    )

    val Ability<*>.displayName: String
        get() {
            return when (this.key) {
                "generic.lumberjack" -> "伐木工"
                "generic.builder" -> "建筑师"
                "generic.miner" -> "矿工"
                "generic.mechanic" -> "机械师"
                "generic.explosive" -> "爆破师"
                "generic.musician" -> "音乐家"
                "generic.smelter" -> "烧炼师"
                "generic.farmer" -> "农场主"
                "generic.rancher" -> "牧场主"
                "generic.hunter" -> "猎人"
                "generic.monster_hunter" -> "怪物猎人"
                "generic.fisherman" -> "渔夫"
                "generic.dye" -> "染色师"
                "generic.railway_engineer" -> "铁路工程师"
                "generic.tamer" -> "驯兽师"
                "generic.enchanter" -> "附魔师"
                "generic.artisan" -> "工匠"
                "generic.brewer" -> "酿造师"
                "generic.confectioner" -> "糕点师"
                "generic.tool_maker" -> "工具制造商"
                else -> "未知"
            }
        }

    private val String.failedInfo: Component
        get() {
            return when (this) {
                "generic.lumberjack" -> Component.text("你尝试挥动斧头，然而你从未从事过伐木工作，因此并没有得到什么。").color(secondaryColorVariant)
                "generic.builder" -> Component.text("你尝试将你的房子装修的漂亮一点，但你并没有接受过建筑专业的教育，你冥思苦想后，仍然不知道该如何装修才行。")
                    .color(secondaryColorVariant)
                "generic.miner" -> Component.text("你试图使用矿镐挖矿，但很显然这种体力活并不是一个从来没有挖过矿的人能做到的，想了想你还是放弃了。")
                    .color(secondaryColorVariant)
                "generic.mechanic" -> Component.text("你看了看眼前的机械部件，虽然你非常想拼出刚大木，但现在的你显然不能做不出来——以后也不见得能做出来。")
                    .color(secondaryColorVariant)
                "generic.explosive" -> Component.text("这玩意儿……还是算了吧……").color(secondaryColorVariant)
                "generic.musician" -> Component.text("你正想演奏一首美妙的音乐，但想想你的歌声就如同扭动的海参一样，还是算了吧……")
                    .color(secondaryColorVariant)
                "generic.smelter" -> Component.text("虽然你也曾听说过铁匠的工作，但为了避免自己的手也被熔融掉，这种活还是交给会干的人干吧。")
                    .color(secondaryColorVariant)
                "generic.farmer" -> Component.text("“烦死了。”当你尝试干了一会儿农活后，你才发现原来农活并不是那么好干的，折腾了这么久，结果你什么都没有得到。")
                    .color(secondaryColorVariant)
                "generic.rancher" -> Component.text("这是……羊？还是猪？还是牛？算了，还是交给能分辨得清这些动物的人来做这些吧。")
                    .color(secondaryColorVariant)
                "generic.hunter" -> Component.text("这是内脏？是心？还是肺？你忙活了半天，除了鲜血让你自己感到更加恶心以外，并没有得到什么。")
                    .color(secondaryColorVariant)
                "generic.monster_hunter" -> Component.text("救命啊！这种怪物还是交给有经验的人来处理吧！").color(secondaryColorVariant)
                "generic.fisherman" -> Component.text("你看着眼前的水域，想想你曾经钓了两个小时的鱼，却什么都没有得到，还是算了吧……")
                    .color(secondaryColorVariant)
                "generic.dye" -> Component.text("“显然染色这种事情还是要交给专业人士。”你想想，然后丢掉了你手上一片漆黑和花花绿绿的东西。")
                    .color(secondaryColorVariant)
                "generic.railway_engineer" -> Component.text("让你们说铁路是男人的浪漫，但很显然这种浪漫并不属于你。").color(secondaryColorVariant)
                "generic.tamer" -> Component.text("如果不想被这些野生动物吃了的话，还是不要这么做了吧……").color(secondaryColorVariant)
                "generic.enchanter" -> Component.text("你看你眼前强大的魔法，你自知你是一个唯物主义者，这种不现实的东西还是让唯心主义者去做吧。")
                    .color(secondaryColorVariant)
                "generic.artisan" -> Component.text("大锤！80！80！你不断这样喊着，但这种技术活你还是什么都做不了。").color(secondaryColorVariant)
                "generic.brewer" -> Component.text("“胡乱制作药品可是违法的。”你喃喃道。").color(secondaryColorVariant)
                "generic.confectioner" -> Component.text("能做出这么难吃的东西，想必一定能毁灭世界了吧……").color(secondaryColorVariant)
                "generic.tool_maker" -> Component.text("虽然你多少听说过乐高，但工具的装配却不仅仅是这么简单").color(secondaryColorVariant)
                else -> Component.text("")
            }
        }

    override fun init(plugin: StarLightCoreX) {
        registerListener(plugin)
        allowedAbilities = plugin.config.getStringList("allowed_abilities")
    }

    fun generateAbilities(
        list: List<Ability<*>> = allowedAbilities.mapNotNull { name -> abilities.find { it.key == name } },
        number: Int = rollAbilitiesMax()
    ): List<Ability<*>> {
        return list
            .shuffled()
            .subList(0, number)
            .map { ability ->
                if (ability is DoubleAbility) ability.copy(value = Random.nextDouble(ability.value))
                else ability
            }
            .onEach { ability ->
                ability.ambiguousName = ability.displayName.let {
                    val select = Random.nextInt(it.length)
                    "…".repeat(
                        it.substring(
                            0,
                            select
                        ).length
                    ) + it[select] + "…".repeat(it.substring(select + 1).length)
                }
            }
    }

    private fun rollAbilitiesMax(): Int {
        return RandomHelperWithValues<Int>().apply {
            addElement(RandomElement(0.5, 1))
            (1..allowedAbilities.size).forEachIndexed { idx, it -> addElement(RandomElement(0.1 * 0.2.pow(idx+1), it)) }
        }.invoke()
    }

    @EventHandler
    private fun onDye(e: EntityDyeEvent) {
        e.isCancelled = true
        getStorageFor<TechnicalPlayer>(e.player?.uniqueId ?: return)?.currentHuman?.also {
            if (it.hasAbility("generic.dye")) {
                e.isCancelled = false
                return
            }
            it.sendMessage("generic.dye".failedInfo)
        }
    }

    @EventHandler
    private fun onPrepareResultEvent(e: PrepareResultEvent) {
        val originResult = e.result
        if (e.inventory is GrindstoneInventory) {
            e.result = null
            val viewer = e.inventory.viewers.mapNotNull { getStorageFor<TechnicalPlayer>(it.uniqueId)?.currentHuman }
            if (viewer.any { it.hasAbility("generic.enchanter") }) {
                e.result = originResult
                return
            }
            viewer.forEach { it.sendMessage("generic.enchanter".failedInfo) }
        }
        if (e.inventory is AnvilInventory) {
            e.result = null
            val viewer = e.inventory.viewers.mapNotNull { getStorageFor<TechnicalPlayer>(it.uniqueId)?.currentHuman }
            if ((e.inventory as AnvilInventory).secondItem?.type == Material.ENCHANTED_BOOK) {
                if (viewer.any { it.hasAbility("generic.enchanter") }) {
                    e.result = originResult
                    return
                }
                viewer.forEach { it.sendMessage("generic.enchanter".failedInfo) }
            } else {
                if (viewer.any { it.hasAbility("generic.artisan") }) {
                    e.result = originResult
                    return
                }
                viewer.forEach { it.sendMessage("generic.artisan".failedInfo) }
            }
        }
    }

    @EventHandler
    private fun onEnchantItem(e: EnchantItemEvent) {
        e.isCancelled = true
        getStorageFor<TechnicalPlayer>(e.enchanter.uniqueId)?.currentHuman?.also {
            if (it.hasAbility("generic.enchanter")) {
                e.isCancelled = false
                return
            }
            it.sendMessage("generic.enchanter".failedInfo)
        }
    }

    @EventHandler
    private fun onSmithingItem(e: SmithItemEvent) {
        e.isCancelled = true
        getStorageFor<TechnicalPlayer>(e.whoClicked.uniqueId)?.currentHuman?.also {
            if (it.hasAbility("generic.artisan")) {
                e.isCancelled = false
                return
            }
            it.sendMessage("generic.artisan".failedInfo)
        }
    }

    @EventHandler
    private fun onTame(e: EntityTameEvent) {
        if (e.owner !is Player) return
        e.isCancelled = true
        getStorageFor<TechnicalPlayer>(e.owner.uniqueId)?.currentHuman?.also {
            if (it.hasAbility("generic.tamer")) {
                e.isCancelled = false
                return
            }
            it.sendMessage("generic.tamer".failedInfo)
        }
    }

    @EventHandler
    private fun onCraft(e: CraftItemEvent) {
        if ((e.recipe is ShapedRecipe && (e.recipe as ShapedRecipe).ingredientMap.values.any {
                Tags.DYE_ITEMS.contains(it?.type)
            })
            || e.recipe is ShapelessRecipe && (e.recipe as ShapelessRecipe).ingredientList.any {
                Tags.DYE_ITEMS.contains(it?.type)
            }
        ) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.whoClicked.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.dye")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.dye".failedInfo)
            }
        }
        if (e.recipe.result.type.isEdible) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.whoClicked.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.confectioner")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.confectioner".failedInfo)
            }
        }
        if (e.recipe.result.type in Tags.TOOL_MAKER_CAN_CRAFT_ITEMS) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.whoClicked.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.tool_maker")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.tool_maker".failedInfo)
            }
        }
    }

    @EventHandler
    private fun onFish(e: PlayerFishEvent) {
        e.isCancelled = true
        getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
            if (it.hasAbility("generic.fisherman")) {
                e.isCancelled = false
                return
            }
            it.sendMessage("generic.fisherman".failedInfo)
        }
    }

    @EventHandler
    private fun onBucketEntity(e: PlayerBucketEntityEvent) {
        if (e.entity is Fish) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.fisherman")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.fisherman".failedInfo)
            }
        }
    }

    @EventHandler
    private fun onUseItem(e: PlayerInteractEvent) {
        if (!e.hasItem()) return
        if (e.action != Action.RIGHT_CLICK_AIR && e.action != Action.RIGHT_CLICK_BLOCK) return
        if (e.item?.type?.key?.value()?.endsWith("hoe", true) == true) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.farmer")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.farmer".failedInfo)
            }
        } else if (Tags.DYE_ITEMS.contains(e.item?.type ?: return) && Tag.SIGNS.isTagged(
                e.clickedBlock?.type ?: return
            )
        ) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.dye")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.dye".failedInfo)
            }
        } else if (e.item?.type == Material.HONEYCOMB
            && e.clickedBlock?.type?.key?.value()?.contains("copper", true) == true
        ) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.builder")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.builder".failedInfo)
            }
        }
    }

    @EventHandler
    private fun onShearEntity(e: PlayerShearEntityEvent) {
        if (e.entity is Animals) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.rancher")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.rancher".failedInfo)
            }
        }
    }

    @EventHandler
    private fun onFillMilk(e: PlayerBucketFillEvent) {
        if (e.itemStack?.type == Material.MILK_BUCKET) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.rancher")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.rancher".failedInfo)
            }
        }
    }

    @EventHandler
    private fun onEnterVehicle(e: VehicleEnterEvent) {
        if (e.entered !is Player) return
        if (e.vehicle is AbstractHorse) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.entered.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.monster_hunter") || it.hasAbility("generic.hunter")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.monster_hunter".failedInfo)
            }
        }
    }

    @EventHandler
    private fun onProjectileDamageEntity(e: ProjectileHitEvent) {
        if (e.entity.shooter !is Player) return
        if (e.hitEntity is Animals) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>((e.entity.shooter as Player).uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.rancher") || it.hasAbility("generic.hunter")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.hunter".failedInfo)
            }
        }
        if (e.hitEntity is Monster) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>((e.entity.shooter as Player).uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.monster_hunter")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.monster_hunter".failedInfo)
            }
        }
    }

    @EventHandler
    private fun onDamageEntity(e: EntityDamageByEntityEvent) {
        if (e.damager !is Player) return
        if (e.entity is Animals) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.damager.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.rancher") || it.hasAbility("generic.hunter")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.hunter".failedInfo)
            }
        }
        if (e.entity is Monster) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.damager.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.monster_hunter")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.monster_hunter".failedInfo)
            }
        }
        if (e.entity is Fish) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.damager.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.fisherman")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.fisherman".failedInfo)
            }
        }
    }

    @EventHandler
    private fun onBreedEntity(e: EntityBreedEvent) {
        if (e.breeder !is Player) return
        e.isCancelled = true
        getStorageFor<TechnicalPlayer>((e.breeder as Player).uniqueId)?.currentHuman?.also {
            if (it.hasAbility("generic.rancher")) {
                e.isCancelled = false
                return
            }
            it.sendMessage("generic.rancher".failedInfo)
        }
    }

    @EventHandler
    private fun onBreakBlock(e: BlockBreakEvent) {
        if (Tag.LOGS.isTagged(e.block.type)) {
            e.isDropItems = false
            getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.lumberjack")) {
                    e.isDropItems = true
                    return
                }
                it.sendMessage("generic.lumberjack".failedInfo)
            }
        }
        if (Tag.STONE_ORE_REPLACEABLES.isTagged(e.block.type)
            || Tag.DEEPSLATE_ORE_REPLACEABLES.isTagged(e.block.type)
            || Tag.COAL_ORES.isTagged(e.block.type)
            || Tag.COPPER_ORES.isTagged(e.block.type)
            || Tag.DIAMOND_ORES.isTagged(e.block.type)
            || Tag.EMERALD_ORES.isTagged(e.block.type)
            || Tag.GOLD_ORES.isTagged(e.block.type)
            || Tag.IRON_ORES.isTagged(e.block.type)
            || Tag.LAPIS_ORES.isTagged(e.block.type)
            || Tag.REDSTONE_ORES.isTagged(e.block.type)
        ) {
            e.isDropItems = false
            getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.miner")) {
                    e.isDropItems = true
                    return
                }
                it.sendMessage("generic.miner".failedInfo)
            }
        }
        if (Tags.FARMER_CAN_BUILD_BLOCKS.contains(e.block.type)) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.farmer")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.farmer".failedInfo)
            }
        }
    }

    @EventHandler
    private fun onPlaceBlock(e: BlockPlaceEvent) {
        if (Tags.BUILDER_CAN_BUILD_BLOCKS.contains(e.blockPlaced.type)) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.builder")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.builder".failedInfo)
            }
        }
        if (Tags.MECHANIC_CAN_BUILD_BLOCKS.contains(e.blockPlaced.type)) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.mechanic")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.mechanic".failedInfo)
            }
        }
        if (Tags.EXPLOSIVE_CAN_BUILD_BLOCKS.contains(e.blockPlaced.type)) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.explosive")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.explosive".failedInfo)
            }
        }
        if (Tags.MUSICIAN_CAN_BUILD_BLOCKS.contains(e.blockPlaced.type)) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.musician")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.musician".failedInfo)
            }
        }
        if (Tags.FARMER_CAN_BUILD_BLOCKS.contains(e.blockPlaced.type)) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.farmer")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.farmer".failedInfo)
            }
        }
        if (Tag.RAILS.isTagged(e.blockPlaced.type)) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.railway_engineer")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.railway_engineer".failedInfo)
            }
        }
    }

    @EventHandler
    private fun onUseMachine(e: InventoryClickEvent) {
        if (e.inventory.type == InventoryType.FURNACE
            || e.inventory.type == InventoryType.BLAST_FURNACE
            || e.inventory.type == InventoryType.SMOKER
        ) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.whoClicked.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.smelter")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.smelter".failedInfo)
            }
        }
        if (e.inventory.type == InventoryType.STONECUTTER) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.whoClicked.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.artisan")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.artisan".failedInfo)
            }
        }
        if (e.inventory.type == InventoryType.LOOM) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.whoClicked.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.dye")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.dye".failedInfo)
            }
        }
        if (e.inventory.type == InventoryType.CARTOGRAPHY) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.whoClicked.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.hunter")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.hunter".failedInfo)
            }
        }
        if (e.inventory.type == InventoryType.BREWING) {
            e.isCancelled = true
            getStorageFor<TechnicalPlayer>(e.whoClicked.uniqueId)?.currentHuman?.also {
                if (it.hasAbility("generic.brewer")) {
                    e.isCancelled = false
                    return
                }
                it.sendMessage("generic.brewer".failedInfo)
            }
        }
    }

}