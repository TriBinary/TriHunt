package net.trilleo.mc.plugins.trihunt.listeners.game

import net.trilleo.mc.plugins.trihunt.data.ServerDataManager
import net.trilleo.mc.plugins.trihunt.managers.GameManager
import net.trilleo.mc.plugins.trihunt.utils.PDCEntryUtil
import net.trilleo.mc.plugins.trihunt.utils.PDCUtil
import net.trilleo.mc.plugins.trihunt.utils.TeamUtil
import net.trilleo.mc.plugins.trihunt.utils.sendPrefixed
import org.bukkit.GameMode
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class GameListener(private val plugin: JavaPlugin) : Listener {
    @EventHandler
    fun onPunch(event: EntityDamageByEntityEvent) {
        if (event.damager is Player && event.entity is Player) {
            val serverData = ServerDataManager.get()

            val damager = event.damager as Player
            val receiver = event.entity as Player

            if (serverData.getString("gameStatus") == "inactive") {
                event.isCancelled = true
            }
            if (serverData.getString("gameStatus") == "ready") {
                if (TeamUtil.isInTeam(damager, "speedrunner") && TeamUtil.isInTeam(receiver, "hunter")) {
                    GameManager(plugin).startGame()
                } else {
                    event.isCancelled = true
                }
            }
        }
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val player = event.player

        for (item in event.drops) {
            if (item != null && PDCUtil.get(
                    item,
                    PDCEntryUtil.PDCKey(plugin).itemIdentifierKey,
                    PersistentDataType.STRING
                ) in listOf(
                    PDCEntryUtil.PDCValue().mainItemIdentifier,
                    PDCEntryUtil.PDCValue().compassItemIdentifier
                )
            ) {
                item.amount = 0
            }
        }

        if (TeamUtil.isInTeam(player, "speedrunner")) {
            if (TeamUtil.getTeam("speedrunner")?.memberCount == 1) {
                GameManager(plugin).endGame(false)
                event.isCancelled = true
            } else {
                TeamUtil.addPlayer(player, "spectator")
                GameManager(plugin).updatePlayerGameMode(player)
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        val player = event.player

        GameManager(plugin).updatePluginItem(player)
    }

    // Detect dragon death
    @EventHandler
    fun onDragonDeath(event: EntityDeathEvent) {
        val serverData = ServerDataManager.get()

        if (serverData.getString("gameStatus") == "active") {
            if (serverData.getString("bossModes", "ender-dragon") == "ender-dragon" && event.entityType == EntityType.ENDER_DRAGON) {
                GameManager(plugin).endGame(true)
            }
            if (serverData.getString("bossModes", "ender-dragon") == "wither" && event.entityType == EntityType.WITHER) {
                GameManager(plugin).endGame(true)
            }
             if (serverData.getString("bossModes", "ender-dragon") == "warden" && event.entityType == EntityType.WARDEN) {
                GameManager(plugin).endGame(true)
             }
        }
    }

    // Detect crouch cancel
    @EventHandler
    fun onSneak(event: PlayerToggleSneakEvent) {
        val serverData = ServerDataManager.get()
        val player = event.player

        if (serverData.getString("gameStatus") == "ready" && TeamUtil.isInTeam(player, "speedrunner")) {
            GameManager(plugin).cancelGame()
        }
    }

    // Detect midway join
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val serverData = ServerDataManager.get()

        if (serverData.getString("gameStatus") in listOf("ready", "active")) {
            if (TeamUtil.isInTeam(player, "spectator") || TeamUtil.getPlayerTeam(player) == null) {
                player.gameMode = GameMode.SPECTATOR
                player.sendPrefixed("<gray>The game is currently in progress so you were put in spectator mode")
            }
        }
    }
}