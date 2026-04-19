package net.trilleo.mc.plugins.trihunt

import net.trilleo.mc.plugins.trihunt.registration.CommandRegistrar
import net.trilleo.mc.plugins.trihunt.registration.GUIManager
import net.trilleo.mc.plugins.trihunt.registration.ListenerRegistrar
import net.trilleo.mc.plugins.trihunt.registration.PermissionRegistrar
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {
    override fun onEnable() {
        // Register commands, listeners and GUIs
        logger.info("Registering commands...")
        CommandRegistrar.registerAll(this)
        logger.info("Registering permissions...")
        PermissionRegistrar.registerAll(this)
        logger.info("Registering listeners...")
        ListenerRegistrar.registerAll(this)
        logger.info("Registering GUIs...")
        GUIManager.registerAll(this)

        logger.info("Plugin enabled!")
    }

    override fun onDisable() {
        logger.info("Plugin disabled!")
    }
}