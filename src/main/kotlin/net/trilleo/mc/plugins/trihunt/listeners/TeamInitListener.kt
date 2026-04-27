package net.trilleo.mc.plugins.trihunt.listeners

import net.trilleo.mc.plugins.trihunt.utils.TeamUtil
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class TeamInitListener : Listener {
    // Add player to Spectator Team on default
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (TeamUtil.getPlayerTeam(player) == null) {
            TeamUtil.addPlayer(player, "spectator")
        }
    }
}