package net.trilleo.mc.plugins.trihunt.commands.game

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.trilleo.mc.plugins.trihunt.registration.GUIManager
import net.trilleo.mc.plugins.trihunt.registration.PluginCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class UICommand : PluginCommand(
    name = "ui",
    description = "Open the main UI",
    usage = "/trihunt ui",
) {
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be used by players.")
            return true
        }
        GUIManager.open(sender, "main")
        sender.sendMessage(Component.text("Opened Trihunt main UI.").color(NamedTextColor.GREEN))
        return true
    }
}