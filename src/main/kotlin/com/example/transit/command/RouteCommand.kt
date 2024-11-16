package com.example.transit.command

import com.example.transit.TransitPlugin
import com.example.transit.model.Route
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class RouteCommand(private val plugin: TransitPlugin) : CommandExecutor, TabCompleter {
    
    private val reorderingSessions = mutableMapOf<Player, ReorderSession>()

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cThis command can only be used by players!")
            return true
        }

        when {
            args.isEmpty() -> {
                sendHelp(sender)
                return true
            }
            args[0].equals("add", true) -> handleAddRoute(sender, args)
            args[0].equals("show", true) -> handleShowRoute(sender, args)
            args[0].equals("addstation", true) -> handleAddStation(sender, args)
            args[0].equals("reorder", true) -> handleReorder(sender, args)
            args[0].equals("select", true) -> handleSelect(sender, args)
            args[0].equals("remove", true) -> handleRemoveRoute(sender, args)
            else -> sendHelp(sender)
        }
        return true
    }

    private fun handleShowRoute(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("§cUsage: /route show <route>")
            return
        }
        
        val route = plugin.routeManager.getRoute(args[1]) ?: run {
            player.sendMessage("§cRoute not found!")
            return
        }
        
        // Send route header
        player.sendMessage("§6Route: ${route.name}")
        player.sendMessage("§7System: ${route.systemId}")
        
        // Send interactive station list
        route.stations.forEachIndexed { index, stationId ->
            val station = plugin.stationManager.getStation(stationId)
            val component = TextComponent("§7${index + 1}. ${station?.name ?: stationId}")
            
            // Add teleport action
            component.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/station tp ${route.systemId} ${station?.name}")
            component.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                ComponentBuilder("§eClick to teleport to this station").create())
            
            player.spigot().sendMessage(component)
        }
    }

    private fun handleReorder(player: Player, args: Array<out String>) {
        if (!player.hasPermission("transit.admin")) {
            player.sendMessage("§cYou don't have permission to reorder routes!")
            return
        }

        if (args.size < 2) {
            player.sendMessage("§cUsage: /route reorder <route>")
            return
        }
        
        val route = plugin.routeManager.getRoute(args[1]) ?: run {
            player.sendMessage("§cRoute not found!")
            return
        }

        reorderingSessions[player] = ReorderSession(args[1], mutableListOf())
        player.sendMessage("""
            §aReordering route ${route.name}
            §7Click station signs in the desired order
            §7Use /route select to confirm the new order
            §7Use /route reorder cancel to cancel
        """.trimIndent())
    }

    private fun handleSelect(player: Player, args: Array<out String>) {
        val session = reorderingSessions[player] ?: run {
            player.sendMessage("§cNo active reordering session!")
            return
        }
        
        if (args.size > 1 && args[1].equals("cancel", true)) {
            reorderingSessions.remove(player)
            player.sendMessage("§cReordering cancelled!")
            return
        }
        
        val route = plugin.routeManager.getRoute(session.routeId) ?: return
        val newOrder = session.selectedIndices.map { route.stations[it] }
        
        if (plugin.routeManager.reorderStations(session.routeId, newOrder)) {
            player.sendMessage("§aRoute stations reordered successfully!")
        } else {
            player.sendMessage("§cFailed to reorder stations!")
        }
        
        reorderingSessions.remove(player)
    }

    private fun handleRemoveRoute(player: Player, args: Array<out String>) {
        if (!player.hasPermission("transit.admin")) {
            player.sendMessage("§cYou don't have permission to remove routes!")
            return
        }

        if (args.size < 2) {
            player.sendMessage("§cUsage: /route remove <route>")
            return
        }

        if (plugin.routeManager.removeRoute(args[1])) {
            player.sendMessage("§aRoute removed successfully!")
        } else {
            player.sendMessage("§cFailed to remove route!")
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (sender !is Player) return emptyList()
        
        return when (args.size) {
            1 -> listOf("add", "show", "addstation", "reorder", "select", "remove")
                .filter { it.startsWith(args[0].toLowerCase()) }
            2 -> when (args[0].toLowerCase()) {
                "add" -> plugin.configManager.getSystems().map { it.id }
                "show", "reorder", "remove" -> plugin.routeManager.getRoutes()
                "addstation" -> plugin.routeManager.getRoutes()
                else -> emptyList()
            }.filter { it.startsWith(args[1].toLowerCase()) }
            3 -> when (args[0].toLowerCase()) {
                "addstation" -> plugin.stationManager.getSystemStations(args[1])
                    .map { it.id }
                    .filter { it.startsWith(args[2].toLowerCase()) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }

    private data class ReorderSession(
        val routeId: String,
        val selectedIndices: MutableList<Int>
    )

    private fun sendHelp(player: Player) {
        player.sendMessage("""
            §6Route Commands:
            §f/route add <system> <name> - Create a new route
            §f/route show <route> - Show route details
            §f/route addstation <route> <station> - Add station to route
            §f/route reorder <route> - Reorder stations in route
            §f/route remove <route> - Remove a route
        """.trimIndent())
    }
}