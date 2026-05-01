package net.trilleo.mc.plugins.trihunt.guis.configMenus

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.trilleo.mc.plugins.trihunt.data.ServerDataManager
import net.trilleo.mc.plugins.trihunt.enums.FillMode
import net.trilleo.mc.plugins.trihunt.registration.GUIManager
import net.trilleo.mc.plugins.trihunt.registration.PluginGUI
import net.trilleo.mc.plugins.trihunt.utils.itemStack
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.plugin.java.JavaPlugin

class GameModeUI(private val plugin: JavaPlugin) : PluginGUI(
    id = "gamemode",
    title = Component.text("TriHunt Modes").color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD),
    rows = 4,
    fillMode = FillMode.LIGHT
) {
    val slotIndex: Map<String, Int> = mapOf(
        "backButtonSlot" to 30,
        "closeButtonSlot" to 31
    )
    val modeIndex: Map<String, Int> = mapOf(
        "specialModesSlot" to 12,
        "bossModesSlot" to 14
    )

    fun refreshModes(inventory: Inventory) {
        val serverData = ServerDataManager.get()

        val specialModesButton = when (serverData.getString("specialModes", "regular")) {
            "regular" -> itemStack(Material.DIAMOND_SWORD) {
                name("<bold><gold>Special Modes")
                lore(
                    "   ",
                    "<white>Selected: <green>Regular",
                    "   ",
                    "<gray>The vanilla Manhunt experience"
                )
                flag(ItemFlag.HIDE_ATTRIBUTES)
            }

            "infested" -> itemStack(Material.REDSTONE_TORCH) {
                name("<bold><gold>Special Modes")
                lore(
                    "   ",
                    "<white>Selected: <red>Infested",
                    "   ",
                    "<gray>If a speedrunner die, he will become a hunter"
                )
            }

            else -> {
                itemStack(Material.GRAY_STAINED_GLASS_PANE) {
                    name("<bold><gray>NULL")
                }
            }
        }
        val bossModesButton = when (serverData.getString("bossModes", "ender-dragon")) {
            "ender-dragon" -> itemStack(Material.DRAGON_HEAD) {
                name("<bold><red>Boss Modes")
                lore(
                    "   ",
                    "<white>Selected: <bold><#7b2cbf>Ender Dragon",
                    "   ",
                    "<gray>Speedrunners need to kill <#7b2cbf>Ender Dragon <gray>to win"
                )
            }

            "wither" -> itemStack(Material.WITHER_SKELETON_SKULL) {
                name("<bold><red>Boss Modes")
                lore(
                    "   ",
                    "<white>Selected: <bold><dark_gray>Wither",
                    "   ",
                    "<gray>Speedrunners need to kill <dark_gray>Wither <gray>to win"
                )
            }

            "warden" -> itemStack(Material.SCULK_SHRIEKER) {
                name("<bold><red>Boss Modes")
                lore(
                    "   ",
                    "<white>Selected: <bold><dark_green>Warden",
                    "   ",
                    "<gray>Speedrunners need to kill <dark_green>Warden <gray>to win"
                )
            }

            else -> {
                itemStack(Material.GRAY_STAINED_GLASS_PANE) {
                    name("<bold><gray>NULL")
                }
            }
        }

        inventory.setItem(modeIndex.getValue("specialModesSlot"), specialModesButton)
        inventory.setItem(modeIndex.getValue("bossModesSlot"), bossModesButton)
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

        refreshModes(inventory)
    }

    override fun onClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val player = event.whoClicked as Player
        if (event.slot in slotIndex.values) {
            player.playSound(
                Sound.sound(Key.key("minecraft:ui.button.click"), Sound.Source.UI, 1f, 1f)
            )
        }
        if (event.slot in modeIndex.values) {
            player.playSound(
                Sound.sound(Key.key("minecraft:entity.experience_orb.pickup"), Sound.Source.UI, 1f, 1f)
            )
        }

        if (event.slot == slotIndex.getValue("closeButtonSlot")) {
            player.closeInventory()
        }
        if (event.slot == slotIndex.getValue("backButtonSlot")) {
            GUIManager.open(player, "settings")
        }

        if (event.slot == modeIndex.getValue("specialModesSlot")) {
            val serverData = ServerDataManager.get()
            val nextMode = when (serverData.getString("specialModes", "regular")) {
                "regular" -> "infested"
                "infested" -> "regular"

                else -> "regular"
            }

            serverData.set("specialModes", nextMode)
            refreshModes(event.inventory)
        }
        if (event.slot == modeIndex.getValue("bossModesSlot")) {
            val serverData = ServerDataManager.get()
            val nextMode = when (serverData.getString("bossModes", "ender-dragon")) {
                "ender-dragon" -> "wither"
                "wither" -> "warden"
                "warden" -> "ender-dragon"

                else -> "ender-dragon"
            }

            serverData.set("bossModes", nextMode)
            refreshModes(event.inventory)
        }
    }
}