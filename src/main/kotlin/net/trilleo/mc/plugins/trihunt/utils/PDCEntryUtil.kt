package net.trilleo.mc.plugins.trihunt.utils

import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin

class PDCEntryUtil {
    // Namespaced Keys
    class PDCKey(private val plugin: JavaPlugin) {
        val itemIdentifierKey = NamespacedKey(plugin, "itemIdentifier")
    }

    // Values
    class PDCValue {
        val mainItemIdentifier = "main-item"
    }
}