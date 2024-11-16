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

    private fun handleAddStation(player: Player, args: Array<out String>) {
        if (!player.hasPermission("transit.admin")) {
            player.sendMessage("§cYou don't have permission to add stations!")
            return
        }

        if (args.size < 3) {
            player.sendMessage("§cUsage: /station add <system> <name> [zone]")
            return
        }

        val system = args[1]
        val name = args[2]
        val zone = if (args.size > 3) args[3] else "1"

        val station = Station(
            id = "${system}_${name.toLowerCase()}",
            name = name,
            systemId = system,
            location = player.location,
            zone = zone
        )

        plugin.stationManager.addStation(station)
        player.sendMessage("§aStation $name added to system $system in zone $zone")
    }

    private fun handleStationStatus(player: Player, args: Array<out String>, status: StationStatus) {
        if (!player.hasPermission("transit.admin")) {
            player.sendMessage("§cYou don't have permission to modify stations!")
            return
        }

        if (args.size < 3) {
            player.sendMessage("§cUsage: /station ${args[0]} <system> <station>")
            return
        }

        val systemId = args[1]
        val stationName = args[2]
        val stationId = "${systemId}_${stationName.toLowerCase()}"

        if (plugin.stationManager.updateStationStatus(stationId, status)) {
            val statusText = if (status == StationStatus.ACTIVE) "enabled" else "disabled"
            player.sendMessage("§aStation $stationName has been $statusText")
        } else {
            player.sendMessage("§cStation not found!")
        }
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