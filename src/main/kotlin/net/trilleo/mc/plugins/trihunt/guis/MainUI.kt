package net.trilleo.mc.plugins.trihunt.guis

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.trilleo.mc.plugins.trihunt.enums.FillMode
import net.trilleo.mc.plugins.trihunt.registration.PluginGUI
import net.trilleo.mc.plugins.trihunt.utils.itemStack
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.SkullMeta
import java.util.UUID

class MainUI : PluginGUI(
    id = "main",
    title = Component.text("TriHunt Main UI").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
    rows = 5,
    fillMode = FillMode.LIGHT
) {
    override fun setup(player: Player, inventory: Inventory) {
        val startButton = itemStack(Material.GREEN_CONCRETE) {
            name("<bold><gradient:green:dark_green>Start</gradient></bold>")
            lore(" ", "<gray>Start the Manhunt")
            enchant(Enchantment.KNOCKBACK, 1)
            flag(ItemFlag.HIDE_ENCHANTS)
        }
        val creditsButton = itemStack(Material.PLAYER_HEAD) {
            name("<bold><gold>Credits")
            meta {
                (this as SkullMeta)
                    .owningPlayer = Bukkit.getPlayer(UUID.fromString("28468a45-b78c-4968-9782-f4f893216066"))
            }
        }

        inventory.setItem(22, startButton)
        inventory.setItem(24, creditsButton)
    }

    override fun onClick(event: InventoryClickEvent) {
        event.isCancelled = true
    }
}