package net.trilleo.mc.plugins.trihunt.listeners.game

import net.trilleo.mc.plugins.trihunt.data.ServerDataManager
import net.trilleo.mc.plugins.trihunt.managers.GameManager
import net.trilleo.mc.plugins.trihunt.utils.TeamUtil
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
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
}