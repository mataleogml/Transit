package com.example.transit.command

import com.example.transit.TransitPlugin
import com.example.transit.model.Station
import com.example.transit.model.StationStatus
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class StationCommand(private val plugin: TransitPlugin) : CommandExecutor, TabCompleter {
    
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
            args[0].equals("add", true) -> handleAddStation(sender, args)
            args[0].equals("remove", true) -> handleRemoveStation(sender, args)
            args[0].equals("enable", true) -> handleStationStatus(sender, args, StationStatus.ACTIVE)
            args[0].equals("disable", true) -> handleStationStatus(sender, args, StationStatus.DISABLED)
            args[0].equals("maintenance", true) -> handleStationStatus(sender, args, StationStatus.MAINTENANCE)
            args[0].equals("tp", true) -> handleTeleport(sender, args)
            args[0].equals("list", true) -> handleList(sender, args)
            else -> sendHelp(sender)
        }
        return true
    }

    private fun handleAddStation(player: Player, args: Array<out String>) {
        if (!player.hasPermission("transit.admin")) {
            player.sendMessage("§cYou don't have permission to create stations!")
            return
        }

        if (args.size < 3) {
            player.sendMessage("§cUsage: /station add <system> <name> [zone]")
            return
        }

        val systemId = args[1]
        val stationName = args[2]
        val zone = if (args.size > 3) args[3] else "1"

        // Check if system exists
        if (plugin.configManager.getTransitSystem(systemId) == null) {
            player.sendMessage("§cTransit system not found!")
            return
        }

        // Create station
        val station = Station(
            id = "${systemId}_${stationName.lowercase()}",
            name = stationName,
            systemId = systemId,
            location = player.location,
            zone = zone
        )

        if (plugin.stationManager.addStation(station)) {
            player.sendMessage("§aStation $stationName created successfully!")
        } else {
            player.sendMessage("§cFailed to create station! It may already exist.")
        }
    }

    private fun handleRemoveStation(player: Player, args: Array<out String>) {
        if (!player.hasPermission("transit.admin")) {
            player.sendMessage("§cYou don't have permission to remove stations!")
            return
        }

        if (args.size < 3) {
            player.sendMessage("§cUsage: /station remove <system> <name>")
            return
        }

        val systemId = args[1]
        val stationName = args[2]
        val stationId = "${systemId}_${stationName.lowercase()}"

        if (plugin.stationManager.removeStation(stationId)) {
            player.sendMessage("§aStation $stationName removed from system $systemId")
        } else {
            player.sendMessage("§cStation not found!")
        }
    }

    private fun handleStationStatus(player: Player, args: Array<out String>, newStatus: StationStatus) {
        if (!player.hasPermission("transit.admin")) {
            player.sendMessage("§cYou don't have permission to modify stations!")
            return
        }

        if (args.size < 3) {
            player.sendMessage("§cUsage: /station ${args[0].lowercase()} <system> <name>")
            return
        }

        val systemId = args[1]
        val stationName = args[2]
        val stationId = "${systemId}_${stationName.lowercase()}"

        if (plugin.stationManager.updateStationStatus(stationId, newStatus)) {
            val statusText = when (newStatus) {
                StationStatus.ACTIVE -> "enabled"
                StationStatus.DISABLED -> "disabled"
                StationStatus.MAINTENANCE -> "put in maintenance"
            }
            player.sendMessage("§aStation $stationName has been $statusText!")
        } else {
            player.sendMessage("§cStation not found!")
        }
    }

    private fun handleTeleport(player: Player, args: Array<out String>) {
        if (!player.hasPermission("transit.tp")) {
            player.sendMessage("§cYou don't have permission to teleport to stations!")
            return
        }

        if (args.size < 3) {
            player.sendMessage("§cUsage: /station tp <system> <name>")
            return
        }

        val systemId = args[1]
        val stationName = args[2]
        val stationId = "${systemId}_${stationName.lowercase()}"

        val station = plugin.stationManager.getStation(stationId)
        if (station != null) {
            player.teleport(station.location)
            player.sendMessage("§aTeleported to station ${station.name}")
        } else {
            player.sendMessage("§cStation not found!")
        }
    }

private fun handleList(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("§cUsage: /station list <system>")
            return
        }

        val systemId = args[1]
        val stations = plugin.stationManager.getSystemStations(systemId)
        
        if (stations.isEmpty()) {
            player.sendMessage("§cNo stations found for system $systemId")
            return
        }

        player.sendMessage("§6Stations in system $systemId:")
        stations.forEach { station ->
            val statusColor = when (station.status) {
                StationStatus.ACTIVE -> "§a"
                StationStatus.DISABLED -> "§c"
                StationStatus.MAINTENANCE -> "§e"
            }
            player.sendMessage("§7- ${station.name} (Zone: ${station.zone}) $statusColor${station.status}")
        }
    }

    private fun sendHelp(player: Player) {
        player.sendMessage("""
            §6Station Commands:
            §f/station add <system> <name> [zone] - Create a new station
            §f/station remove <system> <name> - Remove a station
            §f/station enable <system> <name> - Enable a station
            §f/station disable <system> <name> - Disable a station
            §f/station maintenance <system> <name> - Set station to maintenance
            §f/station tp <system> <name> - Teleport to a station
            §f/station list <system> - List all stations in a system
        """.trimIndent())
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        return when {
            args.isEmpty() -> listOf("add", "remove", "enable", "disable", "maintenance", "tp", "list")
            args.size == 1 -> listOf("add", "remove", "enable", "disable", "maintenance", "tp", "list")
                .filter { it.startsWith(args[0].lowercase()) }
            args.size == 2 -> plugin.configManager.getSystems()
                .map { it.id }
                .filter { it.startsWith(args[1].lowercase()) }
            args.size == 3 -> when (args[0].lowercase()) {
                "tp", "enable", "disable", "maintenance", "remove" -> plugin.stationManager
                    .getSystemStations(args[1])
                    .map { it.name }
                    .filter { it.startsWith(args[2].lowercase()) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}