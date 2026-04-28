package net.trilleo.mc.plugins.trihunt.commands.game

import net.trilleo.mc.plugins.trihunt.data.ServerDataManager
import net.trilleo.mc.plugins.trihunt.managers.GameManager
import net.trilleo.mc.plugins.trihunt.registration.PluginCommand
import net.trilleo.mc.plugins.trihunt.utils.sendPrefixed
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class InitiateCommand(private val plugin: JavaPlugin) : PluginCommand(
    name = "initiate",
    description = "Reset game status",
    permission = "trihunt.initiate"
) {
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        GameManager(plugin).initiateGame()

        if (sender is Player) {
            sender.sendPrefixed("<green>Game status set to <bold>inactive")
        } else {
            sender.sendMessage("Game status set to inactive")
        }

        return true
    }
}