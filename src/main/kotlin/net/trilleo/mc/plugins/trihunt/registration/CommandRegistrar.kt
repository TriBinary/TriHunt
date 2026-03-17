package net.trilleo.mc.plugins.trihunt.registration

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandMap
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

/**
 * Discovers all concrete [PluginCommand] subclasses inside the `commands`
 * package (and its subpackages) and registers them with the server.
 *
 * Commands are split into two categories based on [PluginCommand.isMainCommand]:
 *
 * * **Sub-commands** (`isMainCommand = false`, the default) – registered under
 *   the `/trihunt` parent command (alias `/th`).  A command with
 *   `name = "reload"` becomes `/trihunt reload`.
 * * **Main commands** (`isMainCommand = true`) – registered directly on the
 *   server [CommandMap] as standalone top-level commands.
 *
 * The `/trihunt` command provides tab-completion for all registered
 * sub-commands automatically.
 */
object CommandRegistrar {

    private const val COMMANDS_PACKAGE = "net.trilleo.mc.plugins.trihunt.commands"

    /** All registered sub-commands, keyed by their name (lower-case). */
    private val subCommands = mutableMapOf<String, PluginCommand>()

    /**
     * Scans the commands package, instantiates every [PluginCommand] found,
     * and registers it either as a sub-command of `/trihunt` or as a
     * standalone main command.
     */
    fun registerAll(plugin: JavaPlugin) {
        val commandClasses = PackageScanner.findClasses(
            plugin, COMMANDS_PACKAGE, PluginCommand::class.java
        )
        val commandMap = getCommandMap()

        var subCount = 0
        var mainCount = 0

        for (commandClass in commandClasses) {
            try {
                val command = instantiate(commandClass, plugin)
                if (command.isMainCommand) {
                    val bukkitCommand = createBukkitCommand(command)
                    commandMap.register(plugin.name.lowercase(), bukkitCommand)
                    plugin.logger.info("Registered main command: /${command.name}")
                    mainCount++
                } else {
                    subCommands[command.name.lowercase()] = command
                    plugin.logger.info("Registered sub-command: /trihunt ${command.name}")
                    subCount++
                }
            } catch (e: Exception) {
                plugin.logger.severe(
                    "Failed to register command ${commandClass.simpleName}: ${e.message}"
                )
            }
        }

        // Register the /trihunt parent command (alias /th)
        val parentCommand = createParentCommand(plugin)
        commandMap.register(plugin.name.lowercase(), parentCommand)

        plugin.logger.info(
            "Registered $subCount sub-command(s) and $mainCount main command(s)"
        )
    }

    /** Resolves the server [CommandMap] via reflection for broad compatibility. */
    private fun getCommandMap(): CommandMap {
        val server = Bukkit.getServer()
        val method = server.javaClass.getMethod("getCommandMap")
        return method.invoke(server) as CommandMap
    }

    /**
     * Tries to create an instance of [clazz] using a constructor that accepts
     * a [JavaPlugin]; falls back to a no-arg constructor.
     */
    private fun instantiate(clazz: Class<out PluginCommand>, plugin: JavaPlugin): PluginCommand {
        return try {
            clazz.getDeclaredConstructor(JavaPlugin::class.java).newInstance(plugin)
        } catch (_: NoSuchMethodException) {
            try {
                clazz.getDeclaredConstructor().newInstance()
            } catch (_: NoSuchMethodException) {
                throw IllegalArgumentException(
                    "${clazz.simpleName} must declare either a no-arg constructor " +
                        "or a constructor accepting a single JavaPlugin parameter"
                )
            }
        }
    }

    /**
     * Creates the `/trihunt` parent command that dispatches to sub-commands
     * and provides tab-completion.
     */
    private fun createParentCommand(plugin: JavaPlugin): Command {
        return object : Command(
            "trihunt",
            "Main command for the TriHunt plugin",
            "/trihunt <subcommand> [args]",
            listOf("th")
        ) {
            override fun execute(
                sender: CommandSender,
                commandLabel: String,
                args: Array<out String>
            ): Boolean {
                if (args.isEmpty()) {
                    sender.sendMessage("Usage: /trihunt <subcommand>")
                    sender.sendMessage(
                        "Available sub-commands: ${subCommands.keys.sorted().joinToString(", ")}"
                    )
                    return true
                }

                val subName = args[0].lowercase()
                val subCommand = subCommands[subName]
                if (subCommand == null) {
                    sender.sendMessage("Unknown sub-command: ${args[0]}")
                    sender.sendMessage(
                        "Available sub-commands: ${subCommands.keys.sorted().joinToString(", ")}"
                    )
                    return true
                }

                // Check permission
                subCommand.permission?.let { perm ->
                    if (!sender.hasPermission(perm)) {
                        sender.sendMessage("You do not have permission to use this command.")
                        return true
                    }
                }

                val subArgs = args.drop(1).toTypedArray()
                return subCommand.execute(sender, subArgs)
            }

            override fun tabComplete(
                sender: CommandSender,
                alias: String,
                args: Array<out String>
            ): List<String> {
                if (args.size == 1) {
                    // Complete the sub-command name
                    return subCommands.keys
                        .filter { it.startsWith(args[0].lowercase()) }
                        .sorted()
                }
                if (args.size >= 2) {
                    // Delegate to the sub-command's tab completion
                    val subName = args[0].lowercase()
                    val subCommand = subCommands[subName] ?: return emptyList()
                    val subArgs = args.drop(1).toTypedArray()
                    return subCommand.tabComplete(sender, subArgs)
                }
                return emptyList()
            }
        }
    }

    /** Wraps a [PluginCommand] in a Bukkit [Command] suitable for the command map. */
    private fun createBukkitCommand(command: PluginCommand): Command {
        return object : Command(
            command.name,
            command.description,
            command.usage,
            command.aliases
        ) {
            init {
                command.permission?.let { permission = it }
            }

            override fun execute(
                sender: CommandSender,
                commandLabel: String,
                args: Array<out String>
            ): Boolean = command.execute(sender, args)

            override fun tabComplete(
                sender: CommandSender,
                alias: String,
                args: Array<out String>
            ): List<String> = command.tabComplete(sender, args)
        }
    }
}
