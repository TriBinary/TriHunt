package net.trilleo.mc.plugins.trihunt.guis

import net.trilleo.mc.plugins.trihunt.registration.PluginGUI
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory

class MainUI : PluginGUI(
    id = "main",
    title = "TriHunt Main Menu",
    rows = 5
) {
    override fun setup(player: Player, inventory: Inventory) {
    }

    override fun onClick(event: InventoryClickEvent) {
    }
}