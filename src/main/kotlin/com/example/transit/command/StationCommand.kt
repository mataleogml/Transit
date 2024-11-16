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
            args[0].equals("tp", true) -> handleTeleport(sender, args)
            args[0].equals("list", true) -> handleList(sender, args)
            else -> sendHelp(sender)
        }
        return true
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
        val stationId = "${systemId}_${stationName.toLowerCase()}"

        if (plugin.stationManager.removeStation(stationId)) {
            player.sendMessage("§aStation $stationName removed from system $systemId")
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
        val stationId = "${systemId}_${stationName.toLowerCase()}"

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
            player.sendMessage("§7- ${station.name} (Zone: ${station.zone})")
        }
    }

    private fun sendHelp(player: Player) {
        player.sendMessage("""
            §6Station Commands:
            §f/station add <system> <name> [zone] - Create a new station
            §f/station remove <system> <name> - Remove a station
            §f/station enable <system> <name> - Enable a station
            §f/station disable <system> <name> - Disable a station
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
            args.isEmpty() -> listOf("add", "remove", "enable", "disable", "tp", "list")
            args.size == 1 -> listOf("add", "remove", "enable", "disable", "tp", "list")
                .filter { it.startsWith(args[0].toLowerCase()) }
            args.size == 2 -> plugin.configManager.getSystems().map { it.id }
                .filter { it.startsWith(args[1].toLowerCase()) }
            args.size == 3 -> when (args[0].toLowerCase()) {
                "tp", "enable", "disable", "remove" -> plugin.stationManager
                    .getSystemStations(args[1])
                    .map { it.name }
                    .filter { it.startsWith(args[2].toLowerCase()) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}