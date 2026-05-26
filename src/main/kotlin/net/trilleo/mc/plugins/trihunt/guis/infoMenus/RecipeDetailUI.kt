package net.trilleo.mc.plugins.trihunt.guis.infoMenus

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.trilleo.mc.plugins.trihunt.enums.FillMode
import net.trilleo.mc.plugins.trihunt.registration.GUIManager
import net.trilleo.mc.plugins.trihunt.registration.PluginGUI
import net.trilleo.mc.plugins.trihunt.utils.itemStack
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.*
import java.util.*

/**
 * Displays the full details of a single plugin recipe in a 6-row inventory.
 *
 * The layout adapts to the recipe type:
 * - **Crafting** (shaped / shapeless) – 3×3 ingredient grid on the left, result on the right.
 * - **Cooking** (furnace / blast / smoker / campfire) – input on the left with a fuel
 *   indicator below it, result on the right.
 * - **Stonecutting** – single input on the left, result on the right.
 * - **Smithing** – template, base, and addition slots left-to-right, result on the right.
 *
 * A **Back** button (slot 45) returns to [RecipeBookUI] at the exact page the player
 * was on when they opened this view.  A **Close** button (slot 53) closes the inventory.
 *
 * State is injected before opening via the [setPendingRecipe] companion function.
 */
class RecipeDetailUI : PluginGUI(
    id = "recipe-detail",
    title = Component.text("Recipe Details").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
    rows = 6,
    fillMode = FillMode.LIGHT
) {

    private data class RecipeViewState(val recipe: Recipe, val sourcePage: Int)

    private val activeRecipes = mutableMapOf<UUID, RecipeViewState>()

    // -------------------------------------------------------------------------
    // Slot constants (6-row inventory: rows 1-5 = content, row 6 = navigation)
    //
    //  Row 1 : slots  0 –  8   (filler)
    //  Row 2 : slots  9 – 17   (crafting grid row 1)
    //  Row 3 : slots 18 – 26   (crafting grid row 2 / inputs / arrow / result)
    //  Row 4 : slots 27 – 35   (crafting grid row 3 / fuel)
    //  Row 5 : slots 36 – 44   (filler)
    //  Row 6 : slots 45 – 53   (navigation)
    // -------------------------------------------------------------------------

    companion object {
        // Crafting / shapeless grid (rows 2–4, columns 2–4)
        val GRID = listOf(
            listOf(10, 11, 12),
            listOf(19, 20, 21),
            listOf(28, 29, 30)
        )
        val GRID_FLAT = listOf(10, 11, 12, 19, 20, 21, 28, 29, 30)

        // Shared for cooking / stonecutting / smithing
        const val SLOT_INPUT = 20       // row 3, col 3

        // Cooking-specific
        const val SLOT_FUEL = 29        // row 4, col 3 (below input)

        // Smithing-specific
        const val SLOT_TEMPLATE = 19    // row 3, col 2
        const val SLOT_BASE = 20        // row 3, col 3
        const val SLOT_ADDITION = 21    // row 3, col 4

        // Shared right-side
        const val SLOT_ARROW = 23       // row 3, col 6
        const val SLOT_RESULT = 25      // row 3, col 8

        // Navigation row
        const val SLOT_BACK = 45
        const val SLOT_TYPE_INDICATOR = 49
        const val SLOT_CLOSE = 53

        private val pendingRecipes = mutableMapOf<UUID, Pair<Recipe, Int>>()

        /**
         * Stores the recipe and source-page that [RecipeDetailUI] should display
         * for [player] the next time its [setup] is called.
         *
         * Must be called before [GUIManager.open] with id `"recipe-detail"`.
         *
         * @param player     the player about to open the detail view
         * @param recipe     the recipe to display
         * @param sourcePage the zero-based page in [RecipeBookUI] the player came from
         */
        fun setPendingRecipe(player: Player, recipe: Recipe, sourcePage: Int) {
            pendingRecipes[player.uniqueId] = Pair(recipe, sourcePage)
        }
    }

    // -------------------------------------------------------------------------
    // PluginGUI overrides
    // -------------------------------------------------------------------------

    override fun setup(player: Player, inventory: Inventory) {
        val (recipe, sourcePage) = pendingRecipes.remove(player.uniqueId) ?: return
        activeRecipes[player.uniqueId] = RecipeViewState(recipe, sourcePage)

        // Navigation row: gray glass pane background
        val navFiller = itemStack(Material.GRAY_STAINED_GLASS_PANE) {
            name(" ")
            hideTooltip(true)
        }
        for (slot in 45..53) inventory.setItem(slot, navFiller)

        inventory.setItem(SLOT_BACK, itemStack(Material.ARROW) {
            name("<bold><gray>Back")
        })
        inventory.setItem(SLOT_CLOSE, itemStack(Material.BARRIER) {
            name("<bold><red>Close")
        })

        val (typeLabel, typeMaterial) = getTypeInfo(recipe)
        inventory.setItem(SLOT_TYPE_INDICATOR, itemStack(typeMaterial) {
            name("<bold><white>$typeLabel")
            lore("<dark_gray>──────────────", "<gray>Recipe Type")
        })

        inventory.setItem(SLOT_ARROW, itemStack(Material.ARROW) {
            name("<dark_gray>→")
            hideTooltip(true)
        })
        inventory.setItem(SLOT_RESULT, recipe.result.clone())

        // Recipe-type-specific ingredient layout
        when (recipe) {
            is ShapedRecipe -> renderShaped(recipe, inventory)
            is ShapelessRecipe -> renderShapeless(recipe, inventory)
            is FurnaceRecipe -> renderCooking(recipe, inventory, Material.COAL)
            is BlastingRecipe -> renderCooking(recipe, inventory, Material.COAL_BLOCK)
            is SmokingRecipe -> renderCooking(recipe, inventory, Material.OAK_LOG)
            is CampfireRecipe -> renderCooking(recipe, inventory, Material.STICK)
            is StonecuttingRecipe -> renderStonecutting(recipe, inventory)
            is SmithingTransformRecipe -> renderSmithing(recipe, inventory)
        }
    }

    override fun onClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        val slot = event.rawSlot
        if (slot < 0 || slot >= rows * 9) return

        when (slot) {
            SLOT_BACK -> {
                val state = activeRecipes[player.uniqueId] ?: return
                player.playSound(Sound.sound(Key.key("minecraft:ui.button.click"), Sound.Source.UI, 1f, 1f))
                GUIManager.openAtPage(player, "recipe-book", state.sourcePage)
            }

            SLOT_CLOSE -> {
                player.playSound(Sound.sound(Key.key("minecraft:ui.button.click"), Sound.Source.UI, 1f, 1f))
                player.closeInventory()
            }
        }
    }

    override fun onClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        activeRecipes.remove(player.uniqueId)
    }

    // -------------------------------------------------------------------------
    // Recipe-type renderers
    // -------------------------------------------------------------------------

    /** Places a 3×3 shaped crafting grid into [inventory]. */
    private fun renderShaped(recipe: ShapedRecipe, inventory: Inventory) {
        val shape = recipe.shape
        val choiceMap = recipe.choiceMap
        for (row in 0..2) {
            val rowStr = if (row < shape.size) shape[row] else ""
            for (col in 0..2) {
                val char = if (col < rowStr.length) rowStr[col] else ' '
                val item = recipeChoiceToItemStack(choiceMap[char]) ?: continue
                inventory.setItem(GRID[row][col], item)
            }
        }
    }

    /** Places up to 9 shapeless ingredients into the 3×3 grid slots. */
    private fun renderShapeless(recipe: ShapelessRecipe, inventory: Inventory) {
        for ((i, choice) in recipe.choiceList.withIndex()) {
            if (i >= GRID_FLAT.size) break
            val item = recipeChoiceToItemStack(choice) ?: continue
            inventory.setItem(GRID_FLAT[i], item)
        }
    }

    /**
     * Places the cooking input and a fuel indicator into [inventory].
     * [fuelMaterial] visually communicates which fuel is typical for the station.
     */
    private fun renderCooking(recipe: CookingRecipe<*>, inventory: Inventory, fuelMaterial: Material) {
        recipeChoiceToItemStack(recipe.inputChoice)?.let { inventory.setItem(SLOT_INPUT, it) }
        inventory.setItem(SLOT_FUEL, itemStack(fuelMaterial) {
            name("<gray>Fuel")
            lore("<dark_gray>Place fuel in this slot")
        })
    }

    /** Places the stonecutting input into [inventory]. */
    private fun renderStonecutting(recipe: StonecuttingRecipe, inventory: Inventory) {
        recipeChoiceToItemStack(recipe.inputChoice)?.let { inventory.setItem(SLOT_INPUT, it) }
    }

    /** Places the smithing template, base, and addition into [inventory]. */
    private fun renderSmithing(recipe: SmithingTransformRecipe, inventory: Inventory) {
        recipeChoiceToItemStack(recipe.template)?.let { inventory.setItem(SLOT_TEMPLATE, it) }
        recipeChoiceToItemStack(recipe.base)?.let { inventory.setItem(SLOT_BASE, it) }
        recipeChoiceToItemStack(recipe.addition)?.let { inventory.setItem(SLOT_ADDITION, it) }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Converts a [RecipeChoice] to a representative [ItemStack].
     * Returns `null` for empty (null) choices.
     */
    private fun recipeChoiceToItemStack(choice: RecipeChoice?): ItemStack? = when (choice) {
        is RecipeChoice.MaterialChoice -> ItemStack(choice.choices.firstOrNull() ?: return null)
        is RecipeChoice.ExactChoice -> choice.choices.firstOrNull()
        else -> null
    }

    /** Returns the human-readable label and representative [Material] for the recipe station. */
    private fun getTypeInfo(recipe: Recipe): Pair<String, Material> = when (recipe) {
        is ShapedRecipe -> "Crafting Table" to Material.CRAFTING_TABLE
        is ShapelessRecipe -> "Crafting Table" to Material.CRAFTING_TABLE
        is FurnaceRecipe -> "Furnace" to Material.FURNACE
        is BlastingRecipe -> "Blast Furnace" to Material.BLAST_FURNACE
        is SmokingRecipe -> "Smoker" to Material.SMOKER
        is CampfireRecipe -> "Campfire" to Material.CAMPFIRE
        is StonecuttingRecipe -> "Stonecutter" to Material.STONECUTTER
        is SmithingTransformRecipe -> "Smithing Table" to Material.SMITHING_TABLE
        else -> "Unknown" to Material.BARRIER
    }
}
