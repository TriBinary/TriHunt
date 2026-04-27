package net.trilleo.mc.plugins.trihunt.guis.mainMenus

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.trilleo.mc.plugins.trihunt.enums.FillMode
import net.trilleo.mc.plugins.trihunt.registration.GUIManager
import net.trilleo.mc.plugins.trihunt.registration.PluginGUI
import net.trilleo.mc.plugins.trihunt.utils.TeamUtil
import net.trilleo.mc.plugins.trihunt.utils.itemStack
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag

class TeamSelectUI : PluginGUI(
    id = "team-select",
    title = Component.text("Team").color(NamedTextColor.DARK_BLUE).decorate(TextDecoration.BOLD),
    rows = 6,
    fillMode = FillMode.LIGHT
) {
    val slotIndex: Map<String, Int> = mapOf(
        "backButtonSlot" to 48,
        "closeButtonSlot" to 49
    )
    val infoIndex: Map<String, Int> = mapOf(
        "infoButtonSlot" to 13
    )
    val teamIndex: Map<String, Int> = mapOf(
        "speedrunnerSlot" to 29,
        "hunterSlot" to 31,
        "spectatorSlot" to 33
    )

    fun refreshInventory(player: Player, inventory: Inventory) {
        val infoButton = itemStack(Material.BOOK) {
            name("<bold><white>Select your team")
            lore(
                "   ",
                "<white>Current Team: ${TeamUtil.getPlayerTeam(player)?.displayName ?: "<dark_gray>None"}"
            )
        }
        val speedrunnerButton = itemStack(Material.GREEN_WOOL) {
            name("<bold><dark_green>Speedrunner")
            lore(
                "   ",
                if (TeamUtil.isInTeam(player, "speedrunner")) "<green>Selected" else "<yellow>Click to select"
            )
            if (TeamUtil.isInTeam(player, "speedrunner")) {
                enchant(Enchantment.KNOCKBACK, 1)
                flag(ItemFlag.HIDE_ENCHANTS)
            }
        }
        val hunterButton = itemStack(Material.RED_WOOL) {
            name("<bold><dark_red>Hunter")
            lore(
                "   ",
                if (TeamUtil.isInTeam(player, "hunter")) "<green>Selected" else "<yellow>Click to select"
            )
            if (TeamUtil.isInTeam(player, "hunter")) {
                enchant(Enchantment.KNOCKBACK, 1)
                flag(ItemFlag.HIDE_ENCHANTS)
            }
        }
        val spectatorButton = itemStack(Material.GRAY_WOOL) {
            name("<bold><gray>Spectator")
            lore(
                "   ",
                if (TeamUtil.isInTeam(player, "spectator")) "<green>Selected" else "<yellow>Click to select"
            )
            if (TeamUtil.isInTeam(player, "spectator")) {
                enchant(Enchantment.KNOCKBACK, 1)
                flag(ItemFlag.HIDE_ENCHANTS)
            }
        }

        inventory.setItem(infoIndex.getValue("infoButtonSlot"), infoButton)
        inventory.setItem(teamIndex.getValue("speedrunnerSlot"), speedrunnerButton)
        inventory.setItem(teamIndex.getValue("hunterSlot"), hunterButton)
        inventory.setItem(teamIndex.getValue("spectatorSlot"), spectatorButton)
    }

    override fun setup(player: Player, inventory: Inventory) {
        val closeButton = itemStack(Material.BARRIER) {
            name("<bold><red>Close")
        }
        val backButton = itemStack(Material.ARROW) {
            name("<bold><gray>Back")
        }

        inventory.setItem(slotIndex.getValue("backButtonSlot"), backButton)
        inventory.setItem(slotIndex.getValue("closeButtonSlot"), closeButton)

        refreshInventory(player, inventory)
    }

    override fun onClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val player = event.whoClicked as Player
        if (event.slot in slotIndex.values) {
            player.playSound(
                Sound.sound(Key.key("minecraft:ui.button.click"), Sound.Source.UI, 1f, 1f)
            )
        }
        if (event.slot in teamIndex.values) {
            if (event.currentItem?.containsEnchantment(Enchantment.KNOCKBACK) == false) {
                player.playSound(
                    Sound.sound(Key.key("minecraft:entity.experience_orb.pickup"), Sound.Source.UI, 1f, 1f)
                )
            }
        }

        if (event.slot == slotIndex.getValue("closeButtonSlot")) {
            player.closeInventory()
        }
        if (event.slot == slotIndex.getValue("backButtonSlot")) {
            GUIManager.open(player, "main")
        }

        if (event.slot == teamIndex.getValue("speedrunnerSlot")) {
            TeamUtil.addPlayer(player, "speedrunner")
            refreshInventory(player, event.inventory)
        }
        if (event.slot == teamIndex.getValue("hunterSlot")) {
            TeamUtil.addPlayer(player, "hunter")
            refreshInventory(player, event.inventory)
        }
        if (event.slot == teamIndex.getValue("spectatorSlot")) {
            TeamUtil.addPlayer(player, "spectator")
            refreshInventory(player, event.inventory)
        }
    }
}