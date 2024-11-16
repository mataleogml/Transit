package com.example.transit.command

import com.example.transit.TransitPlugin
import com.example.transit.model.FareType
import com.example.transit.model.TransitSystem
import com.example.transit.statistics.StatisticsManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.UUID

class TransitCommand(private val plugin: TransitPlugin) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].toLowerCase()) {
            "create" -> handleCreate(sender, args)
            "staff" -> handleStaff(sender, args)
            "stat" -> handleStats(sender, args)
            "config" -> handleConfig(sender, args)
            "info" -> handleInfo(sender, args)
            else -> sendHelp(sender)
        }
        return true
    }

    private fun handleCreate(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("transit.admin")) {
            sender.sendMessage("§cYou don't have permission to create transit systems!")
            return
        }

        if (args.size < 4) {
            sender.sendMessage("§cUsage: /transit create <id> <name> <faretype>")
            return
        }

        val id = args[1]
        val name = args[2]
        val fareType = try {
            FareType.valueOf(args[3].uppercase())
        } catch (e: IllegalArgumentException) {
            sender.sendMessage("§cInvalid fare type! Valid types: ${FareType.values().joinToString()}")
            return
        }

        val system = TransitSystem(
            id = id,
            name = name,
            fareType = fareType,
            fareData = mapOf(),
            maxFare = plugin.config.getDouble("systems.default.maxFare", 5.0)
        )

        if (plugin.configManager.addTransitSystem(system)) {
            sender.sendMessage("§aTransit system created successfully!")
        } else {
            sender.sendMessage("§cFailed to create transit system! ID may already be in use.")
        }
    }

    private fun handleStaff(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("transit.admin")) {
            sender.sendMessage("§cYou don't have permission to manage staff!")
            return
        }

        if (args.size < 4) {
            sender.sendMessage("§cUsage: /transit staff <add/remove/list> <system> [player]")
            return
        }

        val systemId = args[2]
        when (args[1].toLowerCase()) {
            "add" -> {
                val player = plugin.server.getPlayer(args[3])
                if (player == null) {
                    sender.sendMessage("§cPlayer not found!")
                    return
                }
                
                val salary = if (args.size > 4) args[4].toDoubleOrNull() ?: 1000.0 else 1000.0
                if (plugin.staffManager.addStaffMember(player.uniqueId, systemId, salary)) {
                    sender.sendMessage("§aStaff member added successfully!")
                } else {
                    sender.sendMessage("§cFailed to add staff member!")
                }
            }
            "remove" -> {
                val playerId = try {
                    UUID.fromString(args[3])
                } catch (e: IllegalArgumentException) {
                    plugin.server.getPlayer(args[3])?.uniqueId
                }
                
                if (playerId == null) {
                    sender.sendMessage("§cPlayer not found!")
                    return
                }
                
                if (plugin.staffManager.removeStaffMember(playerId, systemId)) {
                    sender.sendMessage("§aStaff member removed successfully!")
                } else {
                    sender.sendMessage("§cFailed to remove staff member!")
                }
            }
            "list" -> {
                val staffList = plugin.staffManager.getSystemStaff(systemId)
                if (staffList.isEmpty()) {
                    sender.sendMessage("§cNo staff members found for system $systemId")
                    return
                }
                
                sender.sendMessage("§6Staff members for system $systemId:")
                staffList.forEach { staff ->
                    val playerName = plugin.server.getOfflinePlayer(staff.playerId).name ?: staff.playerId
                    sender.sendMessage("§7- $playerName (Salary: $${staff.salary})")
                }
            }
        }
    }

    private fun handleStats(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /transit stat <system/station/route> <id> [period]")
            return
        }

        val period = if (args.size > 3) {
            try {
                StatisticsManager.StatisticsPeriod.valueOf(args[3].uppercase())
            } catch (e: IllegalArgumentException) {
                StatisticsManager.StatisticsPeriod.ALL_TIME
            }
        } else StatisticsManager.StatisticsPeriod.ALL_TIME

        when (args[1].toLowerCase()) {
            "system" -> {
                if (args.size < 3) {
                    sender.sendMessage("§cPlease specify a system ID!")
                    return
                }
                val systemId = args[2]
                val stats = plugin.statisticsManager.getSystemStatistics(systemId, period)
                displayStatistics(sender, "System", systemId, stats, period)
            }
            "station" -> {
                if (args.size < 3) {
                    sender.sendMessage("§cPlease specify a station ID!")
                    return
                }
                val stationId = args[2]
                val stats = plugin.statisticsManager.getStationStatistics(stationId)
                displayStatistics(sender, "Station", stationId, stats, period)
            }
            "route" -> {
                if (args.size < 3) {
                    sender.sendMessage("§cPlease specify a route ID!")
                    return
                }
                val routeId = args[2]
                val stats = plugin.statisticsManager.getRouteStatistics(routeId)
                displayStatistics(sender, "Route", routeId, stats, period)
            }
        }
    }

    private fun displayStatistics(
        sender: CommandSender, 
        type: String,
        id: String,
        stats: StatisticsManager.Statistics?,
        period: StatisticsManager.StatisticsPeriod
    ) {
        if (stats == null) {
            sender.sendMessage("§cNo statistics found for $type $id")
            return
        }

        sender.sendMessage("""
            §6$type Statistics ($id) - $period
            §7Total Revenue: §f${stats.totalRevenue.formatCurrency()}
            §7Total Transactions: §f${stats.totalTransactions}
            §7Average Transaction: §f${(stats.totalRevenue / stats.totalTransactions).formatCurrency()}
            §7Last Updated: §f${stats.lastUpdated}
        """.trimIndent())
    }

    private fun handleConfig(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("transit.admin")) {
            sender.sendMessage("§cYou don't have permission to manage configuration!")
            return
        }

        if (args.size < 2) {
            sender.sendMessage("§cUsage: /transit config <reload/save>")
            return
        }

        when (args[1].toLowerCase()) {
            "reload" -> {
                plugin.reloadConfig()
                sender.sendMessage("§aConfiguration reloaded successfully!")
            }
            "save" -> {
                plugin.saveConfig()
                sender.sendMessage("§aConfiguration saved successfully!")
            }
        }
    }

    private fun handleInfo(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /transit info <system>")
            return
        }

        val system = plugin.configManager.getTransitSystem(args[1])
        if (system == null) {
            sender.sendMessage("§cSystem not found!")
            return
        }

        sender.sendMessage("""
            §6System Information: §f${system.name}
            §7ID: §f${system.id}
            §7Fare Type: §f${system.fareType}
            §7Maximum Fare: §f${system.maxFare.formatCurrency()}
            §7Stations: §f${plugin.stationManager.getSystemStations(system.id).size}
            §7Routes: §f${plugin.routeManager.getSystemRoutes(system.id).size}
            §7Staff Members: §f${plugin.staffManager.getSystemStaff(system.id).size}
        """.trimIndent())
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("create", "staff", "stat", "config", "info")
                .filter { it.startsWith(args[0].toLowerCase()) }
            2 -> when (args[0].toLowerCase()) {
                "staff" -> listOf("add", "remove", "list")
                "stat" -> listOf("system", "station", "route")
                "config" -> listOf("reload", "save")
                "info", "stat" -> plugin.configManager.getSystems().map { it.id }
                else -> emptyList()
            }.filter { it.startsWith(args[1].toLowerCase()) }
            3 -> when {
                args[0].equals("staff", true) -> plugin.configManager.getSystems().map { it.id }
                args[0].equals("stat", true) -> when (args[1].toLowerCase()) {
                    "system" -> plugin.configManager.getSystems().map { it.id }
                    "station" -> plugin.stationManager.getAllStations().map { it.id }
                    "route" -> plugin.routeManager.getRoutes()
                    else -> emptyList()
                }
                else -> emptyList()
            }.filter { it.startsWith(args[2].toLowerCase()) }
            4 -> when {
                args[0].equals("stat", true) -> StatisticsManager.StatisticsPeriod.values()
                    .map { it.name.toLowerCase() }
                    .filter { it.startsWith(args[3].toLowerCase()) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("""
            §6Transit Commands:
            §f/transit create <id> <name> <faretype> - Create a transit system
            §f/transit staff <add/remove/list> <system> [player] - Manage staff
            §f/transit stat <system/station/route> <id> [period] - View statistics
            §f/transit config <reload/save> - Manage configuration
            §f/transit info <system> - View system information
        """.trimIndent())
    }

    private fun Double.formatCurrency(): String = "$${String.format("%.2f", this)}"
}