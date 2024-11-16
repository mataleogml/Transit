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

        // Header
        player.sendMessage("§8§l" + "=".repeat(20))
        player.sendMessage("§6§l${route.name}")
        player.sendMessage("§8§l" + "=".repeat(20))

        // Stations with distances
        var totalDistance = 0.0
        route.stations.forEachIndexed { index, stationId ->
            val station = plugin.stationManager.getStation(stationId) ?: return@forEachIndexed
            
            // Create interactive station component
            val stationComponent = TextComponent("§f• ${station.name}")
            
            // Add hover text showing interchanges
            val interchanges = plugin.routeManager.getInterchangeRoutes(route.id, stationId)
            if (interchanges.isNotEmpty()) {
                val hoverText = ComponentBuilder("§6Interchanges:\n")
                interchanges.forEach { interchange ->
                    hoverText.append("§f- ${interchange.name}\n")
                }
                stationComponent.hoverEvent = HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    hoverText.create()
                )
            }

            // Add click event for reordering
            stationComponent.clickEvent = ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/route select ${route.id} $index"
            )

            player.spigot().sendMessage(stationComponent)

            // Show distance to next station
            if (index < route.stations.size - 1) {
                val nextStation = plugin.stationManager.getStation(route.stations[index + 1])
                if (nextStation != null) {
                    val distance = station.location.distance(nextStation.location)
                    totalDistance += distance
                    player.sendMessage("§7   ${String.format("%.1f", distance)} blocks ↓")
                }
            }
        }

        // Footer
        player.sendMessage("§8§l" + "=".repeat(20))
        player.sendMessage("§7Total Distance: §f${String.format("%.1f", totalDistance)} blocks")
    }

    private fun handleReorder(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("§cUsage: /route reorder <route>")
            return
        }

        if (!player.hasPermission("transit.admin")) {
            player.sendMessage("§cYou don't have permission to reorder routes!")
            return
        }

        val route = plugin.routeManager.getRoute(args[1]) ?: run {
            player.sendMessage("§cRoute not found!")
            return
        }

        reorderingSessions[player] = ReorderSession(route.id, mutableListOf())
        player.sendMessage("§6Click stations in the desired order:")
        handleShowRoute(player, arrayOf("show", route.id))
    }

    private fun handleSelect(player: Player, args: Array<out String>) {
        if (args.size < 3) return

        val session = reorderingSessions[player] ?: return
        if (session.routeId != args[1]) return

        val index = args[2].toIntOrNull() ?: return
        val route = plugin.routeManager.getRoute(session.routeId) ?: return

        if (index >= route.stations.size) return

        session.selectedIndices.add(index)

        if (session.selectedIndices.size == route.stations.size) {
            // Reordering complete
            val newOrder = session.selectedIndices.map { route.stations[it] }
            plugin.routeManager.reorderStations(route.id, newOrder)
            player.sendMessage("§aRoute reordered successfully!")
            reorderingSessions.remove(player)
            handleShowRoute(player, arrayOf("show", route.id))
        } else {
            player.sendMessage("§6Select next station (${session.selectedIndices.size}/${route.stations.size})")
        }
    }

    private data class ReorderSession(
        val routeId: String,
        val selectedIndices: MutableList<Int>
    )
}