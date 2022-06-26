package io.hikarilan.starlightcorex.gui

import io.hikarilan.starlightcorex.person.Human
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

abstract class PageScrollableGui(
    owner: Human,
    proxied: Human,
    title: Component,
    private val pageSize: Int = 45,
    private val previousButtonIndex: Int = 48,
    private val nextButtonIndex: Int = 50,
) : GuiBase(owner, proxied, title) {

    override val builder: MutableMap<Int, GuiElement>.() -> Unit = {}

    protected abstract val elementsBuilder: MutableList<GuiElement>.() -> Unit
    protected abstract val fixedBuilder: MutableMap<Int, GuiElement>.() -> Unit

    private var currentPage = 0

    private val previousButton = GuiElement(item = ItemStack(Material.RED_WOOL).apply {
        editMeta { it.displayName(Component.text("上一页")) }
    }, close = true) {
        previousPage()
    }

    private val nextButton = GuiElement(item = ItemStack(Material.GREEN_WOOL).apply {
        editMeta { it.displayName(Component.text("下一页")) }
    }, close = true) {
        nextPage()
    }

    private val pages: List<List<GuiElement>> by lazy {
        buildList(elementsBuilder).let { ele ->
            ele.groupBy { ele.indexOf(it) / pageSize }.toSortedMap().values.toList()
        }.let { it.ifEmpty { listOf(emptyList()) } }
    }

    final override val elements: Map<Int, GuiElement>
        get() = pages[currentPage].associateBy({ pages[currentPage].indexOf(it) }, { it }).toMutableMap().apply {
            if (currentPage > 0) {
                put(previousButtonIndex, previousButton)
            }
            if (currentPage < pages.size - 1) {
                put(nextButtonIndex, nextButton)
            }
            putAll(buildMap(fixedBuilder))
        }.toMap()


    final override fun reloadInventory() {
        holder.inventory.clear()
        val pages = buildList(elementsBuilder).let { ele ->
            ele.groupBy { ele.indexOf(it) / pageSize }.toSortedMap().values.toList()
        }.let { it.ifEmpty { listOf(emptyList()) } }
        pages[currentPage].associateBy({ pages[currentPage].indexOf(it) }, { it }).toMutableMap().apply {
            if (currentPage > 0) {
                put(previousButtonIndex, previousButton)
            }
            if (currentPage < pages.size - 1) {
                put(nextButtonIndex, nextButton)
            }
            putAll(buildMap(fixedBuilder))
        }.toMap().forEach { (k, v) ->
            holder.inventory.setItem(k, v.item)
        }
        openGUI()
    }

    private fun previousPage() {
        currentPage--
        initInventory()
        openGUI()
    }

    private fun nextPage() {
        currentPage++
        initInventory()
        openGUI()
    }
}