package net.trilleo.mc.plugins.trihunt.guis

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.trilleo.mc.plugins.trihunt.enums.FillMode
import net.trilleo.mc.plugins.trihunt.registration.PluginGUI
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory

class MainUI : PluginGUI(
    id = "main",
    title = Component.text("TriHunt Main UI").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
    rows = 5,
    fillMode = FillMode.LIGHT
) {
    override fun setup(player: Player, inventory: Inventory) {
    }

    override fun onClick(event: InventoryClickEvent) {
        event.isCancelled = true
    }
}