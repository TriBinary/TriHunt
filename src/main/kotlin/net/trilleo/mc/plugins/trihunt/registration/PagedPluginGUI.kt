package net.trilleo.mc.plugins.trihunt.registration

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.UUID

/**
 * A [PluginGUI] subclass that automatically splits content across
 * multiple pages with **Previous** and **Next** navigation buttons.
 *
 * Extend this class and place the subclass anywhere inside the
 * `net.trilleo.mc.plugins.trihunt.guis` package (or any subpackage) to
 * have it automatically discovered and registered at startup.
 *
 * The bottom row of the inventory is reserved for navigation controls.
 * Content slots are every slot **except** the last row. For example, a
 * 6-row GUI provides 45 content slots per page (rows 1–5).
 *
 * The class must have either:
 * - A no-arg constructor, **or**
 * - A constructor that accepts a single `JavaPlugin` parameter (the plugin
 *   instance will be injected automatically).
 *
 * Example:
 * ```kotlin
 * package net.trilleo.mc.plugins.trihunt.guis
 *
 * import org.bukkit.Material
 * import org.bukkit.inventory.ItemStack
 *
 * class RewardsGUI : PagedPluginGUI(
 *     id = "rewards",
 *     title = Component.text("Rewards"),
 *     rows = 6,
 *     fillMode = FillMode.NONE
 * ) {
 *     override fun getItems(player: Player): List<ItemStack> {
 *         return List(100) { index ->
 *             val item = ItemStack(Material.DIAMOND)
 *             val meta = item.itemMeta
 *             meta.displayName(Component.text("Reward #${index + 1}"))
 *             item.itemMeta = meta
 *             item
 *         }
 *     }
 * }
 * ```
 */
abstract class PagedPluginGUI(
    id: String,
    title: Component,
    rows: Int = 6,
    fillMode: FillMode = FillMode.NONE
) : PluginGUI(id, title, rows, fillMode) {

    /** Tracks the current page for each player viewing this GUI. */
    private val playerPages = mutableMapOf<UUID, Int>()

    /**
     * Returns all items that should be distributed across pages for the
     * given player. The list may be of any size; items are automatically
     * split into pages of [contentSlots] each.
     *
     * @param player the player the GUI is being opened for
     * @return the full list of items to paginate
     */
    abstract fun getItems(player: Player): List<ItemStack>

    /**
     * Called when a player clicks a **content slot** (not a navigation
     * button). Override to add custom click handling.
     *
     * Clicks are cancelled by default to prevent item theft.
     *
     * @param event the inventory click event
     * @param page  the page the player is currently viewing (zero-based)
     */
    open fun onContentClick(event: InventoryClickEvent, page: Int) {}

    /** The number of usable content slots per page (all rows except the last). */
    private val contentSlots: Int
        get() = (rows - 1) * ROW_SIZE

    /** The first slot index of the navigation row (the last row). */
    private val navRowStart: Int
        get() = contentSlots

    // ----- PluginGUI overrides ------------------------------------------------

    override fun setup(player: Player, inventory: Inventory) {
        playerPages[player.uniqueId] = 0
        renderPage(player, inventory, 0)
    }

    override fun onClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        val page = playerPages[player.uniqueId] ?: return
        val slot = event.rawSlot

        // Ignore clicks outside the GUI inventory
        if (slot < 0 || slot >= rows * ROW_SIZE) return

        when (slot) {
            navRowStart + PREVIOUS_OFFSET -> {
                if (page > 0) openPage(player, event.inventory, page - 1)
            }
            navRowStart + NEXT_OFFSET -> {
                val totalPages = totalPages(getItems(player).size)
                if (page < totalPages - 1) openPage(player, event.inventory, page + 1)
            }
            else -> {
                if (slot < contentSlots) onContentClick(event, page)
            }
        }
    }

    override fun onClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        playerPages.remove(player.uniqueId)
    }

    // ----- Internal helpers ---------------------------------------------------

    /**
     * Calculates the total number of pages based on the given [itemCount].
     */
    private fun totalPages(itemCount: Int): Int {
        if (itemCount == 0) return 1
        return (itemCount + contentSlots - 1) / contentSlots
    }

    /** Switches the player to the given [page] and re-renders the inventory. */
    private fun openPage(player: Player, inventory: Inventory, page: Int) {
        playerPages[player.uniqueId] = page
        renderPage(player, inventory, page)
    }

    /** Clears the inventory and populates it with the items for [page]. */
    private fun renderPage(player: Player, inventory: Inventory, page: Int) {
        inventory.clear()

        val items = getItems(player)
        val totalPages = totalPages(items.size)
        val start = page * contentSlots
        val end = minOf(start + contentSlots, items.size)

        for (i in start until end) {
            inventory.setItem(i - start, items[i])
        }

        // Navigation row – fill all slots with gray stained glass panes first
        for (offset in 0 until ROW_SIZE) {
            inventory.setItem(navRowStart + offset, createNavItem(Material.GRAY_STAINED_GLASS_PANE, Component.empty()))
        }

        // Place navigation items on top of the filler
        if (page > 0) {
            inventory.setItem(navRowStart + PREVIOUS_OFFSET, createNavItem(
                Material.ARROW,
                Component.text("Previous Page", NamedTextColor.YELLOW)
            ))
        }

        inventory.setItem(navRowStart + PAGE_INDICATOR_OFFSET, createNavItem(
            Material.PAPER,
            Component.text("Page ${page + 1}/$totalPages", NamedTextColor.WHITE)
        ))

        if (page < totalPages - 1) {
            inventory.setItem(navRowStart + NEXT_OFFSET, createNavItem(
                Material.ARROW,
                Component.text("Next Page", NamedTextColor.YELLOW)
            ))
        }
    }

    /** Creates a navigation item with the given material and display name. */
    private fun createNavItem(material: Material, name: Component): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(name)
        item.itemMeta = meta
        return item
    }

    companion object {
        private const val ROW_SIZE = 9
        private const val PREVIOUS_OFFSET = 0
        private const val PAGE_INDICATOR_OFFSET = 4
        private const val NEXT_OFFSET = 8
    }
}
