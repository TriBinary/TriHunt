package net.trilleo.mc.plugins.trihunt.managers

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.trilleo.mc.plugins.trihunt.data.ServerDataManager
import net.trilleo.mc.plugins.trihunt.utils.TeamUtil
import net.trilleo.mc.plugins.trihunt.utils.sendPrefixed
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class GameManager(private val plugin: JavaPlugin) {
    fun checkCondition(player: Player): Boolean {
        var hasSpeedrunner = false
        var hasHunter = false

        for (player in Bukkit.getOnlinePlayers()) {
            if (TeamUtil.isInTeam(player, "speedrunner")) {
                hasSpeedrunner = true
            }
            if (TeamUtil.isInTeam(player, "hunter")) {
                hasHunter = true
            }
        }

        if (!hasSpeedrunner) {
            player.sendPrefixed("<dark_red>There must be at least 1 player in ${TeamUtil.getTeam("speedrunner")?.displayName}")
            return false
        }
        if (!hasHunter) {
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
            updatePlayerGameMode(player)
        }
    }

    fun prepareGame() {
        val serverData = ServerDataManager.get()

        serverData.set("gameStatus", "ready")

        for (player in plugin.server.onlinePlayers) {
            player.closeInventory()

            updatePluginItem(player)
            updatePlayerGameMode(player)
            updatePlayerEffects(player)
        }

        for (player in plugin.server.onlinePlayers) {
            player.playSound(
                Sound.sound(
                    Key.key("minecraft:entity.experience_orb.pickup"),
                    Sound.Source.MASTER,
                    1f,
                    1f
                )
            )
            if (TeamUtil.isInTeam(player, "speedrunner")) {
                player.sendPrefixed("<green>Game is ready! <yellow>Punch a hunter <green>to start")
            } else {
                player.sendPrefixed("<green>Game is ready! Wait for starting...")
            }
        }
    }

    fun startGame() {
        val serverData = ServerDataManager.get()

        serverData.set("gameStatus", "active")
        for (player in plugin.server.onlinePlayers) {
            var title: Title = Title.title(Component.text(""), Component.text(""))
            if (TeamUtil.isInTeam(player, "speedrunner")) {
                title = Title.title(
                    Component.text("Start").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                    Component.text("Escape the hunters!").color(NamedTextColor.DARK_GREEN)
                )
            }
            if (TeamUtil.isInTeam(player, "hunter")) {
                title = Title.title(
                    Component.text("Start").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                    Component.text("Hunt down the speedrunners!").color(NamedTextColor.DARK_RED)
                )
            }
            if (TeamUtil.isInTeam(player, "spectator")) {
                title = Title.title(
                    Component.text("Start").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                    Component.text("Enjoy the game!").color(NamedTextColor.DARK_GRAY)
                )
            }

            player.showTitle(title)
            player.playSound(Sound.sound(Key.key("minecraft:entity.ender_dragon.growl"), Sound.Source.MASTER, 1f, 1f))
        }
    }

    fun endGame(isSpeedrunnerWin: Boolean) {
        val serverData = ServerDataManager.get()

        serverData.set("gameStatus", "inactive")

        for (player in plugin.server.onlinePlayers) {
            updatePluginItem(player)
            updatePlayerGameMode(player)
            updatePlayerEffects(player)
        }

        if (isSpeedrunnerWin) {
            for (player in plugin.server.onlinePlayers) {
                val speedrunnerTitle = Title.title(
                    Component.text("You Win!").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                    Component.text("You have successfully slain the ender dragon").color(NamedTextColor.DARK_GREEN)
                )
                val hunterTitle = Title.title(
                    Component.text("You Lose!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                    Component.text("You failed to stop the speedrunners...").color(NamedTextColor.DARK_RED)
                )
                val spectatorTitle = Title.title(
                    Component.text("Game End!").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                    Component.text("The speedrunners won").color(NamedTextColor.GRAY)
                )

                if (TeamUtil.isInTeam(player, "speedrunner")) {
                    player.showTitle(speedrunnerTitle)
                }
                if (TeamUtil.isInTeam(player, "hunter")) {
                    player.showTitle(hunterTitle)
                }
                if (TeamUtil.isInTeam(player, "spectator")) {
                    player.showTitle(spectatorTitle)
                }

                player.playSound(
                    Sound.sound(
                        Key.key("minecraft:entity.experience_orb.pickup"),
                        Sound.Source.MASTER,
                        1f,
                        1f
                    )
                )
            }
        } else {
            for (player in plugin.server.onlinePlayers) {
                val speedrunnerTitle = Title.title(
                    Component.text("You Lose!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                    Component.text("You failed to slay the ender dragon...").color(NamedTextColor.DARK_RED)
                )
                val hunterTitle = Title.title(
                    Component.text("You Win!").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                    Component.text("You successfully stopped the speedrunners!").color(NamedTextColor.DARK_GREEN)
                )
                val spectatorTitle = Title.title(
                    Component.text("Game End!").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                    Component.text("The hunters won").color(NamedTextColor.GRAY)
                )

                if (TeamUtil.isInTeam(player, "speedrunner")) {
                    player.showTitle(speedrunnerTitle)
                }
                if (TeamUtil.isInTeam(player, "hunter")) {
                    player.showTitle(hunterTitle)
                }
                if (TeamUtil.isInTeam(player, "spectator")) {
                    player.showTitle(spectatorTitle)
                }

                player.playSound(
                    Sound.sound(
                        Key.key("minecraft:entity.experience_orb.pickup"),
                        Sound.Source.MASTER,
                        1f,
                        1f
                    )
                )
            }
        }
    }

    fun updatePluginItem(player: Player) {
        val serverData = ServerDataManager.get()

        ItemManager(plugin).clearPluginItems(player)

        if (serverData.getString("gameStatus") == "inactive") {
            val mainItem = ItemManager(plugin).createMainItem()

            if (player.inventory.getItem(8) == null) {
                player.inventory.setItem(8, mainItem)
            } else {
                player.inventory.addItem(mainItem)
            }
        }
        if (serverData.getString("gameStatus") in listOf("ready", "active")) {
            val compassItem = ItemManager(plugin).createCompassItem()

            if (TeamUtil.isInTeam(player, "hunter")) {
                if (player.inventory.getItem(8) == null) {
                    player.inventory.setItem(8, compassItem)
                } else {
                    player.inventory.addItem(compassItem)
                }
            }
        }
    }

    fun updatePlayerGameMode(player: Player) {
        val serverData = ServerDataManager.get()

        if (serverData.getString("gameStatus") in listOf("ready", "active")) {
            if (TeamUtil.getPlayerTeam(player)?.name == "spectator") {
                player.gameMode = GameMode.SPECTATOR
            } else {
                player.gameMode = GameMode.SURVIVAL
            }
        }
        if (serverData.getString("gameStatus") == "inactive") {
            player.gameMode = GameMode.ADVENTURE
        }
    }

    fun updatePlayerEffects(player: Player) {
        player.health = 20.0
        player.addPotionEffect(PotionEffect(PotionEffectType.SATURATION, 1, 100))
    }
}