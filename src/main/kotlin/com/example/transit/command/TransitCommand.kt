package com.example.transit.command

import com.example.transit.TransitPlugin
import com.example.transit.model.*
import com.example.transit.statistics.StatisticsManager
import com.example.transit.statistics.StatisticsManager.StatisticsPeriod
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.util.UUID

class TransitCommand(private val plugin: TransitPlugin) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
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
        when (args[1].lowercase()) {
            "add" -> {
                if (args.size < 4) {
                    sender.sendMessage("§cUsage: /transit staff add <system> <player> [salary] [role]")
                    return
                }
                
                val player = plugin.server.getPlayer(args[3])
                if (player == null) {
                    sender.sendMessage("§cPlayer not found!")
                    return
                }
                
                val salary = if (args.size > 4) args[4].toDoubleOrNull() ?: 1000.0 else 1000.0
                val role = if (args.size > 5) {
                    try {
                        StaffRole.valueOf(args[5].uppercase())
                    } catch (e: IllegalArgumentException) {
                        StaffRole.TRAINEE
                    }
                } else StaffRole.TRAINEE

                if (plugin.staffManager.addStaffMember(player.uniqueId, systemId, role, salary)) {
                    sender.sendMessage("§aStaff member added successfully!")
                    player.sendMessage("§aYou have been added as a staff member for $systemId")
                } else {
                    sender.sendMessage("§cFailed to add staff member! They may already be staff.")
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
                    plugin.server.getPlayer(playerId)?.sendMessage(
                        "§cYou have been removed from staff in system $systemId"
                    )
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
                    val playerName = plugin.server.getOfflinePlayer(staff.playerId).name ?: staff.playerId.toString()
                    sender.sendMessage("""
                        §7- $playerName
                          §7Role: §f${staff.role}
                          §7Salary: §f$${staff.salary}
                          §7Payment Period: §f${staff.paymentPeriod}
                    """.trimIndent())
                }
            }
        }
    }

    private fun handleStats(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("transit.stats")) {
            sender.sendMessage("§cYou don't have permission to view statistics!")
            return
        }

        if (args.size < 3) {
            sender.sendMessage("§cUsage: /transit stat <system/station/route> <id> [period]")
            return
        }

        val period = if (args.size > 3) {
            try {
                StatisticsPeriod.valueOf(args[3].uppercase())
            } catch (e: IllegalArgumentException) {
                StatisticsPeriod.ALL_TIME
            }
        } else StatisticsPeriod.ALL_TIME

        when (args[1].lowercase()) {
            "system" -> {
                val systemId = args[2]
                val stats = plugin.statisticsManager.getSystemStatistics(systemId, period)
                displayStatistics(sender, "System", systemId, stats, period)
            }
            "station" -> {
                val stationId = args[2]
                val stats = plugin.statisticsManager.getStationStatistics(stationId, period)
                displayStatistics(sender, "Station", stationId, stats, period)
            }
            "route" -> {
                val routeId = args[2]
                val stats = plugin.statisticsManager.getRouteStatistics(routeId, period)
                displayStatistics(sender, "Route", routeId, stats, period)
            }
            else -> sender.sendMessage("§cInvalid statistics type! Use: system, station, or route")
        }
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

        when (args[1].lowercase()) {
            "reload" -> {
                plugin.reloadConfig()
                sender.sendMessage("§aConfiguration reloaded successfully!")
            }
            "save" -> {
                plugin.saveConfig()
                sender.sendMessage("§aConfiguration saved successfully!")
            }
            else -> sender.sendMessage("§cInvalid config action! Use: reload or save")
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

    private fun displayStatistics(
        sender: CommandSender,
        type: String,
        id: String,
        stats: StatisticsManager.Statistics?,
        period: StatisticsPeriod
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

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        return when (args.size) {
            1 -> listOf("create", "staff", "stat", "config", "info")
                .filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "staff" -> listOf("add", "remove", "list")
                "stat" -> listOf("system", "station", "route")
                "config" -> listOf("reload", "save")
                "info", "stat" -> plugin.configManager.getSystems().map { it.id }
                else -> emptyList()
            }.filter { it.startsWith(args[1].lowercase()) }
            3 -> when {
                args[0].equals("staff", true) -> plugin.configManager.getSystems().map { it.id }
                args[0].equals("stat", true) -> when (args[1].lowercase()) {
                    "system" -> plugin.configManager.getSystems().map { it.id }
                    "station" -> plugin.stationManager.getAllStations().map { it.id }
                    "route" -> plugin.routeManager.getRoutes()
                    else -> emptyList()
                }
                else -> emptyList()
            }.filter { it.startsWith(args[2].lowercase()) }
            4 -> when {
                args[0].equals("stat", true) -> StatisticsPeriod.values()
                    .map { it.name.lowercase() }
                    .filter { it.startsWith(args[3].lowercase()) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }

    private fun Double.formatCurrency(): String = "$${String.format("%.2f", this)}"

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
}