package net.trilleo.mc.plugins.trihunt.guis

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.trilleo.mc.plugins.trihunt.registration.PluginGUI
import net.trilleo.mc.plugins.trihunt.utils.itemStack
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag

class MainUI : PluginGUI(
    id = "main",
    title = Component.text { "TriHunt Main UI" }.color(NamedTextColor.GOLD),
    rows = 5
) {
    override fun setup(player: Player, inventory: Inventory) {
    }

    override fun onClick(event: InventoryClickEvent) {
        event.isCancelled = true
    }
}