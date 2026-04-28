package net.trilleo.mc.plugins.trihunt.managers

import net.trilleo.mc.plugins.trihunt.data.ServerDataManager
import net.trilleo.mc.plugins.trihunt.utils.TeamUtil
import net.trilleo.mc.plugins.trihunt.utils.sendPrefixed
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class GameManager(private val plugin: JavaPlugin) {
    fun checkCondition(player: Player): Boolean {
        if (TeamUtil.getTeam("speedrunner")?.memberCount == 0) {
            player.sendPrefixed("<dark_red>There must be at least 1 player in ${TeamUtil.getTeam("speedrunner")?.displayName}")
            return false
        }
        if (TeamUtil.getTeam("hunter")?.memberCount == 0) {
            player.sendPrefixed("<dark_red>There must be at least 1 player in ${TeamUtil.getTeam("hunter")?.displayName}")
            return false
        }
        return true
    }

    fun initiateGame() {
        val serverData = ServerDataManager.get()

        serverData.set("gameStatus", "inactive")
        for (player in plugin.server.onlinePlayers) {
            updatePluginItem(player)
        }
    }

    fun prepareGame() {
        val serverData = ServerDataManager.get()

        serverData.set("gameStatus", "ready")

        for (player in plugin.server.onlinePlayers) {
            updatePluginItem(player)
            updatePlayerGameMode(player)
        }
    }

    fun updatePluginItem(player: Player) {
        val serverData = ServerDataManager.get()

        ItemManager(plugin).clearPluginItems(player)

        if (serverData.getString("gameStatus") == "inactive") {
            val mainItem = ItemManager(plugin).createMainItem()

            if (player.inventory.getItem(0) == null) {
                player.inventory.setItem(0, mainItem)
            } else {
                player.inventory.addItem(mainItem)
            }
        }
        if (serverData.getString("gameStatus") in listOf("ready", "active")) {
            val compassItem = ItemManager(plugin).createCompassItem()

            if (player.inventory.getItem(0) == null) {
                player.inventory.setItem(0, compassItem)
            } else {
                player.inventory.addItem(compassItem)
            }
        }
    }

    fun updatePlayerGameMode(player: Player) {
        if (TeamUtil.getPlayerTeam(player)?.name == "spectator") {
            player.gameMode = GameMode.SPECTATOR
        } else {
            player.gameMode = GameMode.SURVIVAL
        }
    }
}