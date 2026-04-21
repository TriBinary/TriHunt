# TriHunt - Developer Guide

This guide explains how to create **commands**, **listeners**, **GUIs**, and work with the **configuration** system
using TriHunt's registration system. Commands, listeners, and GUIs all follow the same pattern: extend a base
class (or implement an interface), place the file in the correct package, and the plugin handles the rest
automatically at startup. The configuration system provides typed access to `config.yml` values.

## How Auto-Registration Works

TriHunt uses a `PackageScanner` to discover classes at runtime. When the plugin starts, it scans specific packages for
concrete (non-abstract) classes and registers them automatically. You never need to edit `plugin.yml` or manually wire
anything up.

| System        | Base Class / Interface    | Package                                    |
|:--------------|:--------------------------|:-------------------------------------------|
| Commands      | `PluginCommand`           | `net.trilleo.mc.plugins.trihunt.commands`  |
| Permissions   | *(derived from commands)* | *(automatic — no package needed)*          |
| Listeners     | `Listener`                | `net.trilleo.mc.plugins.trihunt.listeners` |
| GUIs          | `PluginGUI`               | `net.trilleo.mc.plugins.trihunt.guis`      |
| Configuration | `PluginConfig`            | `net.trilleo.mc.plugins.trihunt.config`    |

Subpackages are also scanned, so you can freely organize classes into folders like `commands/game/`,
`listeners/player/`, or `guis/menus/`.

## Constructor Requirements

Every command, listener, and GUI class must have one of the following constructors:

| Constructor                          | When to Use                                   |
|:-------------------------------------|:----------------------------------------------|
| No-arg constructor                   | When you don't need a reference to the plugin |
| Constructor accepting a `JavaPlugin` | When you need to access the plugin instance   |

The plugin instance is injected automatically when a `JavaPlugin` constructor is available.

---

## Commands

To create a command, extend `PluginCommand` and place the class anywhere inside the `commands` package or a subpackage.

By default every command is registered as a **sub-command** of `/trihunt` (alias `/th`). For example, a command with
`name = "reload"` becomes `/trihunt reload`. Set `isMainCommand = true` to register the command as a standalone
top-level command instead.

When a player types `/trihunt` in-game, tab-completion automatically lists all available sub-commands.

### Categories

Commands are automatically categorised based on their **subpackage** (folder) inside the `commands` package. The
category is used by the built-in `/trihunt help` command to group commands for display.

| Command Location                | Category |
|:--------------------------------|:---------|
| `commands/PingCommand.kt`       | General  |
| `commands/game/StartCommand.kt` | Game     |
| `commands/admin/BanCommand.kt`  | Admin    |

### Help Command

The plugin ships with a built-in `/trihunt help` command. It lists every registered command grouped by category, sorted
alphabetically within each group, and formatted with colours for readability. Every command should provide a meaningful
`description` so the help output is informative.

### PluginCommand Properties

| Property        | Type           | Default        | Description                                                              |
|:----------------|:---------------|:---------------|:-------------------------------------------------------------------------|
| `name`          | `String`       | *(required)*   | The command name (e.g. `"reload"` for `/trihunt reload`)                 |
| `description`   | `String`       | `""`           | A brief description shown in `/trihunt help` — always provide one        |
| `usage`         | `String`       | `"/<command>"` | Usage hint shown when the command fails                                  |
| `aliases`       | `List<String>` | `emptyList()`  | Alternative names for the command (applicable to main commands only)     |
| `permission`    | `String?`      | `null`         | Permission node required to use the command (auto-registered at startup) |
| `isMainCommand` | `Boolean`      | `false`        | When `true`, the command is registered as a standalone top-level command |

### Automatic Permission Registration

When the plugin starts, the `PermissionRegistrar` scans every registered command for a non-null `permission` value and
automatically registers it with Bukkit's `PluginManager`. This means:

* Permissions are visible to permission-management plugins (e.g. LuckPerms) without manual configuration.
* Each permission defaults to `PermissionDefault.OP` — only operators have it unless explicitly granted.
* The command's `description` is used as the permission description.
* Duplicate permissions (already registered by another source) are detected and skipped.

You do **not** need to declare permissions in `plugin.yml`; simply set the `permission` property on your command and the
system handles the rest.

### Methods to Override

| Method        | Required | Description                                      |
|:--------------|:---------|:-------------------------------------------------|
| `execute`     | Yes      | Called when a player or console runs the command |
| `tabComplete` | No       | Called when tab-completion is requested          |

### Example (Sub-Command)

This command is registered as `/trihunt ping` (the default behavior):

```kotlin
package net.trilleo.mc.plugins.trihunt.commands

import net.trilleo.mc.plugins.trihunt.registration.PluginCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PingCommand : PluginCommand(
    name = "ping",
    description = "Check your latency",
    usage = "/trihunt ping",
    permission = "trihunt.ping"
) {
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be used by players.")
            return true
        }
        sender.sendMessage("Pong! Your ping is ${sender.ping}ms.")
        return true
    }
}
```

### Example with Tab Completion (Sub-Command)

This command is registered as `/trihunt team`:

```kotlin
package net.trilleo.mc.plugins.trihunt.commands.game

import net.trilleo.mc.plugins.trihunt.registration.PluginCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class TeamCommand : PluginCommand(
    name = "team",
    description = "Join a team",
    usage = "/trihunt team <hunters|runners>",
    permission = "trihunt.team"
) {
    private val teams = listOf("hunters", "runners")

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.isEmpty() || args[0] !in teams) {
            sender.sendMessage("Usage: /trihunt team <hunters|runners>")
            return false
        }
        sender.sendMessage("You joined the ${args[0]} team!")
        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return teams.filter { it.startsWith(args[0], ignoreCase = true) }
        }
        return emptyList()
    }
}
```

### Example with Plugin Instance (Sub-Command)

This command is registered as `/trihunt reload`:

```kotlin
package net.trilleo.mc.plugins.trihunt.commands

import net.trilleo.mc.plugins.trihunt.registration.PluginCommand
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class ReloadCommand(private val plugin: JavaPlugin) : PluginCommand(
    name = "reload",
    description = "Reload the plugin configuration",
    permission = "trihunt.reload"
) {
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val main = plugin as? net.trilleo.mc.plugins.trihunt.Main
        if (main == null) {
            sender.sendMessage("Error: Plugin instance type mismatch. Unable to reload configuration.")
            return true
        }
        main.pluginConfig.reload()
        sender.sendMessage("Configuration reloaded!")
        return true
    }
}
```

### Example (Main Command)

Set `isMainCommand = true` to register a standalone top-level command.
This command is registered as `/globaltool`:

```kotlin
package net.trilleo.mc.plugins.trihunt.commands

import net.trilleo.mc.plugins.trihunt.registration.PluginCommand
import org.bukkit.command.CommandSender

class GlobalToolCommand : PluginCommand(
    name = "globaltool",
    description = "A standalone top-level command",
    usage = "/globaltool",
    isMainCommand = true
) {
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        sender.sendMessage("Hello from /globaltool!")
        return true
    }
}
```

---

## Listeners

To create a listener, implement Bukkit's `Listener` interface and place the class anywhere inside the `listeners`
package or a subpackage.

### Methods

Annotate each event handler method with `@EventHandler`. The method must accept a single Bukkit event parameter.

### Example

```kotlin
package net.trilleo.mc.plugins.trihunt.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class JoinListener : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.joinMessage(
            net.kyori.adventure.text.Component.text("Welcome, ${event.player.name}!")
        )
    }
}
```

### Example with Subpackage and Plugin Instance

```kotlin
package net.trilleo.mc.plugins.trihunt.listeners.player

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.plugin.java.JavaPlugin

class DeathListener(private val plugin: JavaPlugin) : Listener {

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        plugin.logger.info("${event.player.name} has been eliminated!")
    }
}
```

---

## GUIs

To create a GUI (chest-based inventory menu), extend `PluginGUI` and place the class anywhere inside the `guis` package
or a subpackage.

### PluginGUI Properties

| Property   | Type        | Default         | Description                                                      |
|:-----------|:------------|:----------------|:-----------------------------------------------------------------|
| `id`       | `String`    | *(required)*    | Unique identifier used to open the GUI                           |
| `title`    | `Component` | *(required)*    | Title displayed at the top of the chest                          |
| `rows`     | `Int`       | `3`             | Number of rows (1–6, each row = 9 slots)                         |
| `fillMode` | `FillMode`  | `FillMode.NONE` | Controls how empty slots are pre-filled before `setup` is called |

#### FillMode values

| Value            | Filler item              | Description                                                                    |
|:-----------------|:-------------------------|:-------------------------------------------------------------------------------|
| `FillMode.NONE`  | *(none)*                 | No filler is placed; the inventory is left empty before `setup` is called      |
| `FillMode.LIGHT` | White stained glass pane | All slots are pre-filled with white glass before `setup` — override in `setup` |
| `FillMode.DARK`  | Black stained glass pane | All slots are pre-filled with black glass before `setup` — override in `setup` |

### Methods to Override

| Method    | Required | Description                                           |
|:----------|:---------|:------------------------------------------------------|
| `setup`   | Yes      | Populate the inventory with items before it opens     |
| `onClick` | No       | Handle click events (clicks are cancelled by default) |
| `onClose` | No       | Handle cleanup when the GUI is closed                 |

### Opening a GUI

Use `GUIManager.open(player, id)` to open a registered GUI for a player:

```kotlin
import net.trilleo.mc.plugins.trihunt.registration.GUIManager

// Returns true if the GUI was found and opened, false otherwise
GUIManager.open(player, "settings")
```

### Example

```kotlin
package net.trilleo.mc.plugins.trihunt.guis

import net.trilleo.mc.plugins.trihunt.registration.FillMode
import net.trilleo.mc.plugins.trihunt.registration.PluginGUI
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class SettingsGUI : PluginGUI(
    id = "settings",
    title = Component.text("Settings"),
    rows = 3,
    fillMode = FillMode.DARK
) {
    override fun setup(player: Player, inventory: Inventory) {
        val compass = ItemStack(Material.COMPASS)
        val meta = compass.itemMeta
        meta.displayName(Component.text("Tracker"))
        compass.itemMeta = meta
        inventory.setItem(13, compass)
    }

    override fun onClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        if (event.slot == 13) {
            player.sendMessage("Tracker selected!")
        }
    }
}
```

### Opening a GUI from a Command

A common pattern is opening a GUI when a player runs a command. This command is registered as `/trihunt settings`:

```kotlin
package net.trilleo.mc.plugins.trihunt.commands

import net.trilleo.mc.plugins.trihunt.registration.GUIManager
import net.trilleo.mc.plugins.trihunt.registration.PluginCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class SettingsCommand : PluginCommand(
    name = "settings",
    description = "Open the settings menu",
    permission = "trihunt.settings"
) {
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be used by players.")
            return true
        }
        GUIManager.open(sender, "settings")
        return true
    }
}
```

---

## Paged GUIs

To create a multi-page inventory menu with automatic navigation, extend `PagedPluginGUI` and place the class anywhere
inside the `guis` package or a subpackage. `PagedPluginGUI` is a subclass of `PluginGUI` that handles page state per
player and renders **Previous** / **Next** buttons automatically.

The bottom row of the inventory is reserved for navigation controls. Content slots are every slot except the last row.
For example, a 6-row GUI provides 45 content slots per page (rows 1–5).

### PagedPluginGUI Properties

`PagedPluginGUI` inherits all properties from `PluginGUI`:

| Property | Type        | Default      | Description                              |
|:---------|:------------|:-------------|:-----------------------------------------|
| `id`     | `String`    | *(required)* | Unique identifier used to open the GUI   |
| `title`  | `Component` | *(required)* | Title displayed at the top of the chest  |
| `rows`   | `Int`       | `6`          | Number of rows (2–6, each row = 9 slots) |

### Methods to Override

| Method           | Required | Description                                                      |
|:-----------------|:---------|:-----------------------------------------------------------------|
| `getItems`       | Yes      | Return the full list of items to paginate for a player           |
| `onContentClick` | No       | Handle clicks on content slots (clicks are cancelled by default) |

You do **not** need to override `setup`, `onClick`, or `onClose` — `PagedPluginGUI` handles them internally for
pagination. If you need custom close logic, override `onClose` and call `super.onClose(event)` to ensure page state
is cleaned up.

### Navigation Layout

The last row of the inventory contains:

| Slot (in last row) | Item  | Description                                  |
|:-------------------|:------|:---------------------------------------------|
| 0                  | Arrow | **Previous Page** — hidden on the first page |
| 4                  | Paper | **Page indicator** — displays "Page X/Y"     |
| 8                  | Arrow | **Next Page** — hidden on the last page      |

### Example

```kotlin
package net.trilleo.mc.plugins.trihunt.guis

import net.trilleo.mc.plugins.trihunt.registration.PagedPluginGUI
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

class RewardsGUI : PagedPluginGUI(
    id = "rewards",
    title = Component.text("Rewards"),
    rows = 6
) {
    override fun getItems(player: Player): List<ItemStack> {
        return List(100) { index ->
            val item = ItemStack(Material.DIAMOND)
            val meta = item.itemMeta
            meta.displayName(Component.text("Reward #${index + 1}"))
            item.itemMeta = meta
            item
        }
    }

    override fun onContentClick(event: InventoryClickEvent, page: Int) {
        val player = event.whoClicked as? Player ?: return
        player.sendMessage("You clicked slot ${event.slot} on page ${page + 1}!")
    }
}
```

### Opening a Paged GUI from a Command

Paged GUIs are opened the same way as regular GUIs, using `GUIManager.open(player, id)`:

```kotlin
package net.trilleo.mc.plugins.trihunt.commands

import net.trilleo.mc.plugins.trihunt.registration.GUIManager
import net.trilleo.mc.plugins.trihunt.registration.PluginCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class RewardsCommand : PluginCommand(
    name = "rewards",
    description = "Browse available rewards",
    permission = "trihunt.rewards"
) {
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be used by players.")
            return true
        }
        GUIManager.open(sender, "rewards")
        return true
    }
}
```

---

## Adventure Library

Paper bundles the [Kyori Adventure](https://docs.advntr.dev/) library, so no extra dependency is required. Adventure
replaces the legacy Bukkit chat API and provides rich, structured text through immutable `Component` objects, as well
as APIs for titles, boss bars, sounds, and more.

### Component

`Component` is the core type. All text displayed to players must be a `Component`. The most common factory is
`Component.text(...)`, which accepts an optional colour and decoration inline:

```kotlin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

// Plain text
val plain = Component.text("Hello, world!")

// Coloured text
val coloured = Component.text("Hello, world!", NamedTextColor.GREEN)

// Bold + coloured text
val bold = Component.text("Hello, world!", NamedTextColor.GOLD, TextDecoration.BOLD)
```

#### Additional Component Factory Methods

| Factory                            | Description                                                              |
|:-----------------------------------|:-------------------------------------------------------------------------|
| `Component.empty()`                | A component with no content — useful as a neutral base to `.append()` to |
| `Component.newline()`              | A line-break component                                                   |
| `Component.space()`                | A single space                                                           |
| `Component.text(String)`           | Plain text component                                                     |
| `Component.translatable(String)`   | A Minecraft translation key (e.g. `"block.minecraft.dirt"`)              |
| `Component.keybind(String)`        | Displays the key bound to an action (e.g. `"key.jump"`)                  |
| `Component.join(separator, parts)` | Joins a list of components with a separator between each one             |

##### `Component.translatable` Example

`Component.translatable` renders using the player's own client language:

```kotlin
import net.kyori.adventure.text.Component

// Displays the item's translated name in the player's language
val dirtName = Component.translatable("block.minecraft.dirt")
sender.sendMessage(dirtName)
```

##### `Component.keybind` Example

`Component.keybind` renders as the key the player has bound to a given action:

```kotlin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

// Shows "Press [Space] to jump!" where [Space] adapts to the player's key binding
val hint = Component.text("Press ", NamedTextColor.GRAY)
    .append(Component.keybind("key.jump", NamedTextColor.YELLOW))
    .append(Component.text(" to jump!", NamedTextColor.GRAY))
sender.sendMessage(hint)
```

##### `Component.join` Example

```kotlin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.NamedTextColor

val items = listOf(
    Component.text("Sword", NamedTextColor.RED),
    Component.text("Shield", NamedTextColor.BLUE),
    Component.text("Bow", NamedTextColor.GREEN)
)
// "Sword, Shield, Bow"
val list = Component.join(JoinConfiguration.separator(Component.text(", ")), items)
sender.sendMessage(list)
```

#### NamedTextColor

`NamedTextColor` exposes the 16 standard Minecraft colours as constants:

| Constant       | In-game appearance |
|:---------------|:-------------------|
| `BLACK`        | Black              |
| `DARK_BLUE`    | Dark Blue          |
| `DARK_GREEN`   | Dark Green         |
| `DARK_AQUA`    | Dark Aqua          |
| `DARK_RED`     | Dark Red           |
| `DARK_PURPLE`  | Dark Purple        |
| `GOLD`         | Gold               |
| `GRAY`         | Gray               |
| `DARK_GRAY`    | Dark Gray          |
| `BLUE`         | Blue               |
| `GREEN`        | Green              |
| `AQUA`         | Aqua               |
| `RED`          | Red                |
| `LIGHT_PURPLE` | Light Purple       |
| `YELLOW`       | Yellow             |
| `WHITE`        | White              |

#### TextColor (Hex / RGB)

For colours beyond the 16 named constants, use `TextColor`:

```kotlin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

// From a hex string
val orange = TextColor.fromHexString("#FF8C00")!!
val msg = Component.text("This is orange!", orange)

// From RGB values (0–255 each)
val custom = TextColor.color(135, 206, 235) // sky blue
val sky = Component.text("Sky blue text", custom)
```

#### TextDecoration

`TextDecoration` applies visual styles to a component:

| Constant        | Effect              |
|:----------------|:--------------------|
| `BOLD`          | Bold text           |
| `ITALIC`        | Italic text         |
| `UNDERLINED`    | Underlined text     |
| `STRIKETHROUGH` | Strikethrough text  |
| `OBFUSCATED`    | Obfuscated (matrix) |

Decorations can be combined by chaining `.decorate(...)` calls:

```kotlin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

val fancy = Component.text("Important!", NamedTextColor.RED)
    .decorate(TextDecoration.BOLD)
    .decorate(TextDecoration.UNDERLINED)
```

### Style

`Style` bundles a colour, decorations, click event, and hover event into a reusable object. Apply it to a component
with `.style(Style)` or pass it directly to `Component.text`:

```kotlin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration

val headerStyle = Style.style(
    NamedTextColor.GOLD,
    TextDecoration.BOLD
)

val header = Component.text("TriHunt", headerStyle)
sender.sendMessage(header)
```

Build a `Style` with multiple properties using the builder:

```kotlin
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration

val linkStyle = Style.style { builder ->
    builder.color(NamedTextColor.AQUA)
    builder.decoration(TextDecoration.UNDERLINED, true)
    builder.clickEvent(ClickEvent.openUrl("https://papermc.io"))
    builder.hoverEvent(HoverEvent.showText(Component.text("Visit Paper docs")))
}
```

### Chaining Components

Use `.append(Component)` to concatenate multiple styled segments into one message:

```kotlin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

val message = Component.text("[TriHunt] ", NamedTextColor.GOLD, TextDecoration.BOLD)
    .append(Component.text("Welcome to the server!", NamedTextColor.YELLOW))

sender.sendMessage(message)
```

### Sending Messages

Both `CommandSender` (players and the console) and `Player` accept a `Component` directly via `sendMessage`:

```kotlin
// From a command
sender.sendMessage(Component.text("Command executed!", NamedTextColor.GREEN))

// From a listener
event.player.sendMessage(Component.text("You joined!", NamedTextColor.AQUA))
```

To broadcast a message to every online player, use the Bukkit server instance:

```kotlin
import org.bukkit.Bukkit

Bukkit.broadcast(Component.text("Server announcement!", NamedTextColor.GOLD))
```

### ClickEvent

A `ClickEvent` makes a component interactive when clicked in the chat window. Attach one with `.clickEvent(...)`:

```kotlin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

// Run a command when clicked
val runCmd = Component.text("[Click to teleport]", NamedTextColor.GREEN)
    .clickEvent(ClickEvent.runCommand("/tp spawn"))

// Pre-fill the chat bar with a command (player still has to press Enter)
val suggest = Component.text("[Click to reply]", NamedTextColor.YELLOW)
    .clickEvent(ClickEvent.suggestCommand("/msg Steve "))

// Open a URL in the player's browser
val link = Component.text("[Open website]", NamedTextColor.AQUA, TextDecoration.UNDERLINED)
    .clickEvent(ClickEvent.openUrl("https://papermc.io"))

// Copy text to the player's clipboard
val copy = Component.text("[Copy server IP]", NamedTextColor.GRAY)
    .clickEvent(ClickEvent.copyToClipboard("play.example.com"))

sender.sendMessage(runCmd)
```

#### ClickEvent Actions

| Factory method                       | Effect                                      |
|:-------------------------------------|:--------------------------------------------|
| `ClickEvent.runCommand(String)`      | Executes the command as the player          |
| `ClickEvent.suggestCommand(String)`  | Places the string in the player's chat bar  |
| `ClickEvent.openUrl(String)`         | Opens a URL in the player's default browser |
| `ClickEvent.copyToClipboard(String)` | Copies the string to the player's clipboard |
| `ClickEvent.changePage(Int)`         | Changes the page of an open book            |

### HoverEvent

A `HoverEvent` displays a tooltip when the player hovers over the component. Attach one with `.hoverEvent(...)`:

```kotlin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

// Show a text tooltip
val withHover = Component.text("Hover over me!", NamedTextColor.GREEN)
    .hoverEvent(
        HoverEvent.showText(
            Component.text("This is a tooltip!", NamedTextColor.GRAY)
        )
    )

// Show an item tooltip (displays the item's name, lore, and stats)
val diamond = ItemStack(Material.DIAMOND)
val withItemHover = Component.text("A diamond", NamedTextColor.AQUA)
    .hoverEvent(diamond.asHoverEvent())

sender.sendMessage(withHover)
```

#### HoverEvent Actions

| Factory method                   | Effect                                 |
|:---------------------------------|:---------------------------------------|
| `HoverEvent.showText(Component)` | Shows a rich-text tooltip              |
| `ItemStack.asHoverEvent()`       | Shows the item's name, lore, and stats |
| `Entity.asHoverEvent()`          | Shows the entity's name and UUID       |

### MiniMessage

[MiniMessage](https://docs.advntr.dev/minimessage/index.html) is a string-based format that lets you express rich
text with lightweight tags. The `ItemStack` DSL uses it internally, and you can use it anywhere you need to parse
user-facing strings (e.g. from `config.yml`) into `Component` objects.

```kotlin
import net.kyori.adventure.text.minimessage.MiniMessage

val mm = MiniMessage.miniMessage()

// Colour
val red = mm.deserialize("<red>This is red text")

// Bold + gradient
val fancy = mm.deserialize("<bold><gradient:gold:yellow>Fancy Title</gradient></bold>")

// Multiple colours in one line
val mixed = mm.deserialize("<green>Success: <white>operation completed")

sender.sendMessage(fancy)
```

#### MiniMessage with Placeholders

Use `TagResolver` to inject dynamic values into a MiniMessage string at runtime:

```kotlin
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder

val mm = MiniMessage.miniMessage()

val message = mm.deserialize(
    "<gold>Welcome, <player>! You have <coins> coins.",
    Placeholder.unparsed("player", player.name),
    Placeholder.unparsed("coins", "500")
)
player.sendMessage(message)
```

#### Common MiniMessage Tags

| Tag                              | Effect                                          |
|:---------------------------------|:------------------------------------------------|
| `<color_name>` / `<red>`         | Named colour (same names as `NamedTextColor`)   |
| `<#RRGGBB>`                      | Hex colour                                      |
| `<bold>`, `<b>`                  | Bold                                            |
| `<italic>`, `<i>`                | Italic                                          |
| `<underlined>`, `<u>`            | Underline                                       |
| `<strikethrough>`, `<st>`        | Strikethrough                                   |
| `<obfuscated>`, `<obf>`          | Obfuscated                                      |
| `<gradient:color1:color2>`       | Smooth gradient between two or more colours     |
| `<rainbow>`                      | Full rainbow gradient across the text           |
| `<reset>`                        | Reset all active styles                         |
| `<newline>` / `<br>`             | Line break                                      |
| `<click:run_command:/cmd>`       | Clickable text that runs a command              |
| `<click:suggest_command:/cmd>`   | Clickable text that fills the chat bar          |
| `<click:open_url:https://...>`   | Clickable text that opens a URL                 |
| `<click:copy_to_clipboard:text>` | Clickable text that copies to clipboard         |
| `<hover:show_text:'tooltip'>`    | Text shown when the cursor hovers over the line |
| `<keybind:key.jump>`             | Renders the player's bound key for an action    |
| `<lang:block.minecraft.dirt>`    | Renders a Minecraft translation key             |

### Title

Display a large on-screen title and subtitle to a player with the Adventure `Title` API:

```kotlin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import java.time.Duration

val title = Title.title(
    Component.text("Game Over", NamedTextColor.RED),           // main title
    Component.text("You were eliminated!", NamedTextColor.GRAY), // subtitle
    Title.Times.times(
        Duration.ofMillis(500),   // fade-in
        Duration.ofSeconds(3),    // stay
        Duration.ofMillis(500)    // fade-out
    )
)

player.showTitle(title)
```

To clear an active title before it finishes:

```kotlin
player.clearTitle()
```

To reset the title display timings back to their defaults:

```kotlin
player.resetTitle()
```

### Action Bar

The action bar is the text that appears just above the hotbar. It disappears on its own after a couple of seconds.

```kotlin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

player.sendActionBar(
    Component.text("⚔ 10 kills", NamedTextColor.GOLD)
)
```

### Boss Bar

A boss bar is the coloured progress bar shown at the top of the screen. Create one, customise it, then add players:

```kotlin
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

val bar = BossBar.bossBar(
    Component.text("Boss Fight!", NamedTextColor.RED), // name
    1.0f,                                               // progress (0.0–1.0)
    BossBar.Color.RED,                                  // bar colour
    BossBar.Overlay.PROGRESS                            // bar style
)

// Show to a player
player.showBossBar(bar)

// Update the progress (e.g. based on boss HP)
bar.progress(0.5f)

// Update the title
bar.name(Component.text("50% HP remaining", NamedTextColor.YELLOW))

// Hide from a player
player.hideBossBar(bar)
```

#### BossBar.Color Options

| Constant | Bar colour |
|:---------|:-----------|
| `PINK`   | Pink       |
| `BLUE`   | Blue       |
| `RED`    | Red        |
| `GREEN`  | Green      |
| `YELLOW` | Yellow     |
| `PURPLE` | Purple     |
| `WHITE`  | White      |

#### BossBar.Overlay Options

| Constant     | Appearance                         |
|:-------------|:-----------------------------------|
| `PROGRESS`   | Solid bar (no notches)             |
| `NOTCHED_6`  | Bar split into 6 notched segments  |
| `NOTCHED_10` | Bar split into 10 notched segments |
| `NOTCHED_12` | Bar split into 12 notched segments |
| `NOTCHED_20` | Bar split into 20 notched segments |

### Sound

Play a sound to a player at their location using the Adventure `Sound` API:

```kotlin
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.key.Key

// Play a named sound at the player's position
player.playSound(
    Sound.sound(
        Key.key("minecraft:entity.player.levelup"), // sound key
        Sound.Source.PLAYER,                         // source category
        1.0f,                                        // volume
        1.0f                                         // pitch
    )
)
```

You can also use `net.kyori.adventure.sound.Sound.Source` to control which Minecraft audio channel the sound plays on:

| Source    | Channel shown in game settings |
|:----------|:-------------------------------|
| `MASTER`  | Master                         |
| `MUSIC`   | Music                          |
| `RECORD`  | Jukebox/Note Blocks            |
| `WEATHER` | Weather                        |
| `BLOCK`   | Blocks                         |
| `HOSTILE` | Hostile Creatures              |
| `NEUTRAL` | Friendly Creatures             |
| `PLAYER`  | Players                        |
| `AMBIENT` | Ambient/Environment            |
| `VOICE`   | Voice/Speech                   |

Stop all sounds currently playing for a player:

```kotlin
import net.kyori.adventure.sound.SoundStop

player.stopSound(SoundStop.all())
```

### Tab-List Header and Footer

Set the header and footer shown in the player list (Tab key):

```kotlin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

player.sendPlayerListHeaderAndFooter(
    Component.text("TriHunt Server", NamedTextColor.GOLD, TextDecoration.BOLD),
    Component.text("${player.ping}ms", NamedTextColor.GRAY)
)
```

To clear the header and footer, pass empty components:

```kotlin
player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty())
```

---

## ItemStack Builder DSL

Building `ItemStack` instances with custom names, lore, enchantments, and flags normally requires verbose
boilerplate. The `itemStack` DSL in `net.trilleo.mc.plugins.trihunt.utils` lets you create fully configured items in a
single expression. All text is parsed through
[MiniMessage](https://docs.advntr.dev/minimessage/index.html), so rich formatting tags like `<bold>`, `<red>`,
and `<gradient>` work out of the box.

### Before (vanilla API)

```kotlin
val item = ItemStack(Material.DIAMOND_SWORD)
val meta = item.itemMeta
meta.displayName(MiniMessage.miniMessage().deserialize("<bold><gradient:gold:yellow>Excalibur</gradient></bold>"))
meta.lore(
    listOf(
        MiniMessage.miniMessage().deserialize("<gray>A legendary blade"),
        MiniMessage.miniMessage().deserialize("<gray>Damage: <red>+20")
    )
)
meta.addEnchant(Enchantment.SHARPNESS, 5, true)
meta.isUnbreakable = true
meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
item.itemMeta = meta
```

### After (using the DSL)

```kotlin
import net.trilleo.mc.plugins.trihunt.utils.itemStack

val item = itemStack(Material.DIAMOND_SWORD) {
    name("<bold><gradient:gold:yellow>Excalibur</gradient></bold>")
    lore("<gray>A legendary blade", "<gray>Damage: <red>+20")
    enchant(Enchantment.SHARPNESS, 5)
    unbreakable(true)
    flag(ItemFlag.HIDE_ENCHANTS)
}
```

### Builder Methods

| Method            | Signature                   | Description                                     |
|:------------------|:----------------------------|:------------------------------------------------|
| `name`            | `name(String)`              | Set the display name (MiniMessage)              |
| `lore`            | `lore(vararg String)`       | Set lore lines (each parsed with MiniMessage)   |
| `enchant`         | `enchant(Enchantment, Int)` | Add an enchantment at the given level           |
| `unbreakable`     | `unbreakable(Boolean)`      | Make the item unbreakable                       |
| `amount`          | `amount(Int)`               | Set the stack size                              |
| `flag`            | `flag(vararg ItemFlag)`     | Add one or more item flags                      |
| `customModelData` | `customModelData(Int)`      | Set the custom model data value                 |
| `meta`            | `meta(ItemMeta.() -> Unit)` | Escape hatch for direct `ItemMeta` manipulation |

### Escape Hatch Example

For advanced use-cases not covered by the builder methods, the `meta` block gives you direct access to the
`ItemMeta`. Any changes made inside `meta` are applied **after** all other builder properties, so they take
precedence:

```kotlin
import net.trilleo.mc.plugins.trihunt.utils.itemStack

val head = itemStack(Material.PLAYER_HEAD) {
    name("<yellow>Custom Head")
    meta {
        // 'this' is the ItemMeta — cast and use any Paper API method
        (this as org.bukkit.inventory.meta.SkullMeta)
            .owningPlayer = org.bukkit.Bukkit.getOfflinePlayer("Notch")
    }
}
```

---

## Configuration

TriHunt provides a typed configuration wrapper — `PluginConfig` — around the standard Bukkit `config.yml`. It
lives in the `net.trilleo.mc.plugins.trihunt.config` package and is created automatically when the plugin starts.

### How It Works

1. On first run, the default `config.yml` bundled inside the JAR (`src/main/resources/config.yml`) is copied to the
   plugin's data folder.
2. `PluginConfig` loads the YAML values into memory and exposes them through typed getter methods.
3. At any time you can call `reload()` to re-read the file from disk, picking up changes made while the server is
   running.

The plugin's `Main` class exposes the instance as `pluginConfig`:

```kotlin
class Main : JavaPlugin() {
    lateinit var pluginConfig: PluginConfig
        private set

    override fun onEnable() {
        pluginConfig = PluginConfig(this)
        // ...
    }
}
```

### Default config.yml

Place default values in `src/main/resources/config.yml`. They are copied to the server's plugin data folder on first
run:

```yaml
# TriHunt Configuration

# A friendly prefix shown before plugin messages
message-prefix: "[TriHunt]"
```

### Typed Getters

`PluginConfig` provides the following typed getter methods. Each method accepts a YAML path and a default value that
is returned when the key is absent or has the wrong type:

| Method          | Signature                           | Description                                       |
|:----------------|:------------------------------------|:--------------------------------------------------|
| `getString`     | `getString(path, default = "")`     | Returns a `String` value                          |
| `getInt`        | `getInt(path, default = 0)`         | Returns an `Int` value                            |
| `getDouble`     | `getDouble(path, default = 0.0)`    | Returns a `Double` value                          |
| `getBoolean`    | `getBoolean(path, default = false)` | Returns a `Boolean` value                         |
| `getStringList` | `getStringList(path)`               | Returns a `List<String>` (empty list if absent)   |
| `contains`      | `contains(path)`                    | Returns `true` when the path exists in the config |

### Reloading

Call `reload()` to re-read `config.yml` from disk without restarting the server. The method copies any new default
keys into the file, saves it, and refreshes the in-memory values:

```kotlin
pluginConfig.reload()
```

The built-in `/trihunt reload` command already calls this method.

### Accessing the Config from a Command

Cast the injected `JavaPlugin` to `Main` to reach `pluginConfig`:

```kotlin
package net.trilleo.mc.plugins.trihunt.commands

import net.trilleo.mc.plugins.trihunt.Main
import net.trilleo.mc.plugins.trihunt.registration.PluginCommand
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class PrefixCommand(private val plugin: JavaPlugin) : PluginCommand(
    name = "prefix",
    description = "Show the configured message prefix",
    permission = "trihunt.prefix"
) {
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val main = plugin as? Main ?: return true
        val prefix = main.pluginConfig.getString("message-prefix", "[TriHunt]")
        sender.sendMessage("Current prefix: $prefix")
        return true
    }
}
```

### Accessing the Config from a Listener

The same pattern works for listeners — accept a `JavaPlugin` constructor parameter and cast to `Main`:

```kotlin
package net.trilleo.mc.plugins.trihunt.listeners

import net.trilleo.mc.plugins.trihunt.Main
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin

class WelcomeListener(private val plugin: JavaPlugin) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val main = plugin as? Main ?: return
        val prefix = main.pluginConfig.getString("message-prefix", "[TriHunt]")
        event.player.sendMessage("$prefix Welcome, ${event.player.name}!")
    }
}
```
