package net.trilleo.mc.plugins.trihunt.listeners.interaction

import net.trilleo.mc.plugins.trihunt.registration.GUIManager
import net.trilleo.mc.plugins.trihunt.utils.PDCEntryUtil
import net.trilleo.mc.plugins.trihunt.utils.PDCUtil
import net.trilleo.mc.plugins.trihunt.utils.itemStack
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class MainItemListener(private val plugin: JavaPlugin) : Listener {
    private fun createMainItem(): ItemStack {
        val mainItem = itemStack(Material.NETHER_STAR) {
            name("<bold><gold>TriHunt Menu")
            lore(
                "   ",
                "<gray>[Right Click] to open menu"
            )
            enchant(Enchantment.BINDING_CURSE, 1)
            flag(ItemFlag.HIDE_ENCHANTS)
            pdc(
                PDCEntryUtil.PDCKey(plugin).itemIdentifierKey,
                PersistentDataType.STRING,
                PDCEntryUtil.PDCValue().mainItemIdentifier
            )
        }
        return mainItem
    }

    // Give player main item on join
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        for (item in player.inventory.contents) {
            if (item != null && PDCUtil.get(
                    item,
                    PDCEntryUtil.PDCKey(plugin).itemIdentifierKey,
                    PersistentDataType.STRING
                ) == PDCEntryUtil.PDCValue().mainItemIdentifier
            ) {
                return
            }
        }

        val mainItem = createMainItem()

        if (player.inventory.getItem(0) == null) {
            player.inventory.setItem(0, mainItem)
        } else {
            player.inventory.addItem(mainItem)
        }
    }

    // Detect menu opening
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item
        if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
            if (item != null && PDCUtil.get(
                    item,
                    PDCEntryUtil.PDCKey(plugin).itemIdentifierKey,
                    PersistentDataType.STRING
                ) == PDCEntryUtil.PDCValue().mainItemIdentifier
            ) {
                event.isCancelled = true
                GUIManager.open(player, "main")
            }
        }
    }

    // Detect main item dropping
    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val item = event.itemDrop.itemStack
        if (PDCUtil.get(
                item,
                PDCEntryUtil.PDCKey(plugin).itemIdentifierKey,
                PersistentDataType.STRING
            ) == PDCEntryUtil.PDCValue().mainItemIdentifier
        ) {
            event.isCancelled = true
        }
    }
}