package io.hikarilan.starlightcorex.utils

import io.hikarilan.starlightcorex.StarLightCoreX
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import java.io.InputStream

object Tags : PluginInitializeModule {

    override fun init(plugin: StarLightCoreX) {
        BUILDER_CAN_BUILD_BLOCKS = resolveMaterialTags(
            plugin.getResource("tags/blocks/builder_can_build.json")
                ?: throw IllegalStateException("Could not find builder_can_build.json")
        )
        MECHANIC_CAN_BUILD_BLOCKS = resolveMaterialTags(
            plugin.getResource("tags/blocks/mechanic_can_build.json")
                ?: throw IllegalStateException("Could not find mechanic_can_build.json")
        )
        EXPLOSIVE_CAN_BUILD_BLOCKS = resolveMaterialTags(
            plugin.getResource("tags/blocks/explosive_can_build.json")
                ?: throw IllegalStateException("Could not find explosive_can_build.json")
        )
        MUSICIAN_CAN_BUILD_BLOCKS = resolveMaterialTags(
            plugin.getResource("tags/blocks/musician_can_build.json")
                ?: throw IllegalStateException("Could not find musician_can_build.json")
        )
        FARMER_CAN_BUILD_BLOCKS = resolveMaterialTags(
            plugin.getResource("tags/blocks/farmer_can_build.json")
                ?: throw IllegalStateException("Could not find farmer_can_build.json")
        )
        GLASS_BLOCKS = resolveMaterialTags(
            plugin.getResource("tags/blocks/glass.json")
                ?: throw IllegalStateException("Could not find glass.json")
        )
        GLASS_PANE_BLOCKS = resolveMaterialTags(
            plugin.getResource("tags/blocks/glass_pane.json")
                ?: throw IllegalStateException("Could not find glass_pane.json")
        )
        DYE_ITEMS = resolveMaterialTags(
            plugin.getResource("tags/items/dye.json")
                ?: throw IllegalStateException("Could not find dye.json"),
            registry = Tag.REGISTRY_ITEMS
        )
        TOOL_MAKER_CAN_CRAFT_ITEMS = resolveMaterialTags(
            plugin.getResource("tags/items/tool_maker_can_craft.json")
                ?: throw IllegalStateException("Could not find tool_maker_can_craft.json"),
            registry = Tag.REGISTRY_ITEMS
        )
    }

    lateinit var BUILDER_CAN_BUILD_BLOCKS: Set<Material>
        private set

    lateinit var MECHANIC_CAN_BUILD_BLOCKS: Set<Material>
        private set

    lateinit var EXPLOSIVE_CAN_BUILD_BLOCKS: Set<Material>
        private set

    lateinit var MUSICIAN_CAN_BUILD_BLOCKS: Set<Material>
        private set

    lateinit var FARMER_CAN_BUILD_BLOCKS: Set<Material>
        private set

    lateinit var GLASS_BLOCKS: Set<Material>
        private set

    lateinit var GLASS_PANE_BLOCKS: Set<Material>
        private set

    lateinit var DYE_ITEMS: Set<Material>
        private set

    lateinit var TOOL_MAKER_CAN_CRAFT_ITEMS: Set<Material>
        private set

    private fun resolveMaterialTags(stream: InputStream, registry: String = Tag.REGISTRY_BLOCKS): Set<Material> {
        return gson.fromJson(stream.reader(), Set::class.java)
            .mapNotNull { it as String }
            .flatMap {
                if (it.startsWith("#")) {
                    Bukkit.getTag(
                        registry,
                        NamespacedKey.minecraft(it.substring(1)),
                        Material::class.java
                    )?.values ?: setOf()
                } else {
                    setOf(Material.getMaterial(it))
                }
            }
            .filterNotNull()
            .toSet()
    }

}