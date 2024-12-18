package com.example.transit.command

import com.example.transit.TransitPlugin
import com.example.transit.statistics.StatisticsManager
import com.example.transit.statistics.StatisticsManager.StatisticsPeriod
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class StatisticsCommand(private val plugin: TransitPlugin) : CommandExecutor, TabCompleter {
    
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission("transit.stats")) {
            sender.sendMessage("§cYou don't have permission to view statistics!")
            return true
        }

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].toLowerCase()) {
            "system" -> handleSystemStats(sender, args)
            "station" -> handleStationStats(sender, args)
            "route" -> handleRouteStats(sender, args)
            "report" -> handleReport(sender, args)
            "peaks" -> handlePeakTimes(sender, args)
            "export" -> handleExport(sender, args)
            else -> sendHelp(sender)
        }
        return true
    }

    private fun handleSystemStats(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /stats system <systemId> [period]")
            return
        }

        val systemId = args[1]
        val period = if (args.size > 2) {
            try {
                StatisticsPeriod.valueOf(args[2].uppercase())
            } catch (e: IllegalArgumentException) {
                StatisticsPeriod.ALL_TIME
            }
        } else StatisticsPeriod.ALL_TIME

        val stats = plugin.statisticsManager.getSystemStatistics(systemId, period)
        if (stats == null) {
            sender.sendMessage("§cNo statistics found for system $systemId")
            return
        }

        sender.sendMessage("""
            §6System Statistics - $systemId ($period)
            §7Revenue: §f$${stats.totalRevenue}
            §7Transactions: §f${stats.totalTransactions}
            §7Average Fare: §f$${stats.averageFare}
            §7Entry Count: §f${stats.entryCount}
            §7Exit Count: §f${stats.exitCount}
            §7Direct Fares: §f${stats.directFares}
            §7Last Updated: §f${stats.lastUpdated}
            
            §6Hourly Breakdown:
            ${formatHourlyStats(stats.hourlyStats)}
        """.trimIndent())
    }

    private fun handleStationStats(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /stats station <stationId> [period]")
            return
        }

        val stationId = args[1]
        val period = if (args.size > 2) {
            try {
                StatisticsPeriod.valueOf(args[2].uppercase())
            } catch (e: IllegalArgumentException) {
                StatisticsPeriod.ALL_TIME
            }
        } else StatisticsPeriod.ALL_TIME

        val stats = plugin.statisticsManager.getStationStatistics(stationId, period)
        if (stats == null) {
            sender.sendMessage("§cNo statistics found for station $stationId")
            return
        }

        val station = plugin.stationManager.getStation(stationId)
        sender.sendMessage("""
            §6Station Statistics - ${station?.name ?: stationId} ($period)
            §7Revenue: §f$${stats.totalRevenue}
            §7Transactions: §f${stats.totalTransactions}
            §7Average Fare: §f$${stats.averageFare}
            §7Entry Count: §f${stats.entryCount}
            §7Exit Count: §f${stats.exitCount}
            §7Last Updated: §f${stats.lastUpdated}
            
            §6Hourly Usage:
            ${formatHourlyStats(stats.hourlyStats)}
        """.trimIndent())
    }

    private fun handleRouteStats(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /stats route <routeId> [period]")
            return
        }

        val routeId = args[1]
        val period = if (args.size > 2) {
            try {
                StatisticsPeriod.valueOf(args[2].uppercase())
            } catch (e: IllegalArgumentException) {
                StatisticsPeriod.ALL_TIME
            }
        } else StatisticsPeriod.ALL_TIME

        val stats = plugin.statisticsManager.getRouteStatistics(routeId, period)
        if (stats == null) {
            sender.sendMessage("§cNo statistics found for route $routeId")
            return
        }

        val route = plugin.routeManager.getRoute(routeId)
        sender.sendMessage("""
            §6Route Statistics - ${route?.name ?: routeId} ($period)
            §7Revenue: §f$${stats.totalRevenue}
            §7Transactions: §f${stats.totalTransactions}
            §7Average Fare: §f$${stats.averageFare}
            §7Last Updated: §f${stats.lastUpdated}
            
            §6Hourly Usage:
            ${formatHourlyStats(stats.hourlyStats)}
        """.trimIndent())
    }

    private fun handleReport(sender: CommandSender, args: Array<out String>) {
        if (args.size < 4) {
            sender.sendMessage("§cUsage: /stats report <systemId> <startDate> <endDate>")
            sender.sendMessage("§cDate format: YYYY-MM-DD")
            return
        }

        val systemId = args[1]
        val startDate = try {
            LocalDate.parse(args[2], dateFormat)
        } catch (e: DateTimeParseException) {
            sender.sendMessage("§cInvalid start date format. Use YYYY-MM-DD")
            return
        }

        val endDate = try {
            LocalDate.parse(args[3], dateFormat)
        } catch (e: DateTimeParseException) {
            sender.sendMessage("§cInvalid end date format. Use YYYY-MM-DD")
            return
        }

        val report = plugin.statisticsManager.generateReport(systemId, startDate, endDate)
        
        sender.sendMessage("""
            §6System Report - $systemId
            §7Period: §f${report.period.startDate} to ${report.period.endDate}
            
            §6Overall Statistics:
            §7Total Revenue: §f$${report.totalRevenue}
            §7Total Transactions: §f${report.totalTransactions}
            §7Average Transaction: §f$${report.averageTransactionValue}
            
            §6Peak Hours:
            ${formatPeakHours(report.peakHours)}
            
            §6Busiest Stations:
            ${formatBusyStations(report.busyStations)}
            
            §6Popular Routes:
            ${formatPopularRoutes(report.popularRoutes)}
        """.trimIndent())
    }

    private fun handlePeakTimes(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /stats peaks <systemId>")
            return
        }

        val systemId = args[1]
        val peakHours = plugin.statisticsManager.getPeakHours(systemId)
        
        if (peakHours.isEmpty()) {
            sender.sendMessage("§cNo peak time data available for system $systemId")
            return
        }

        sender.sendMessage("§6Peak Hours for $systemId:")
        peakHours.forEach { peak ->
            sender.sendMessage("§7${peak.hour}:00 - ${peak.hour + 1}:00: §f${peak.transactions} transactions")
        }
    }

    private fun handleExport(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("§cUsage: /stats export <systemId> <type>")
            sender.sendMessage("§cTypes: csv, json")
            return
        }

        sender.sendMessage("§cExport functionality not yet implemented")
    }

    private fun formatHourlyStats(stats: Map<Int, Int>): String {
        return stats.entries
            .sortedBy { it.key }
            .joinToString("\n") { (hour, count) ->
                "§7${hour}:00 - ${hour + 1}:00: §f$count"
            }
    }

    private fun formatPeakHours(peakHours: List<StatisticsManager.PeakHour>): String {
        return peakHours.joinToString("\n") { peak ->
            "§7${peak.hour}:00 - ${peak.hour + 1}:00: §f${peak.transactions} transactions"
        }
    }

    private fun formatBusyStations(stations: List<StatisticsManager.BusyStation>): String {
        return stations.take(5).joinToString("\n") { station ->
            "§7${station.name}: §f${station.usageCount} uses"
        }
    }

    private fun formatPopularRoutes(routes: List<StatisticsManager.PopularRoute>): String {
        return routes.take(5).joinToString("\n") { route ->
            val fromStation = plugin.stationManager.getStation(route.fromStationId)?.name ?: route.fromStationId
            val toStation = plugin.stationManager.getStation(route.toStationId)?.name ?: route.toStationId
            "§7$fromStation → $toStation: §f${route.usageCount} trips (avg: $${route.averageFare})"
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        return when (args.size) {
            1 -> listOf("system", "station", "route", "report", "peaks", "export")
                .filter { it.startsWith(args[0].toLowerCase()) }
            2 -> when (args[0].toLowerCase()) {
                "system", "report", "peaks" -> plugin.configManager.getSystems().map { it.id }
                "station" -> plugin.stationManager.getAllStations().map { it.id }
                "route" -> plugin.routeManager.getRoutes()
                "export" -> plugin.configManager.getSystems().map { it.id }
                else -> emptyList()
            }.filter { it.startsWith(args[1].toLowerCase()) }
            3 -> when (args[0].toLowerCase()) {
                "system", "station", "route" -> 
                    StatisticsPeriod.values().map { it.name.toLowerCase() }
                "export" -> listOf("csv", "json")
                else -> emptyList()
            }.filter { it.startsWith(args[2].toLowerCase()) }
            else -> emptyList()
        }
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("""
            §6Statistics Commands:
            §f/stats system <systemId> [period] - View system statistics
            §f/stats station <stationId> [period] - View station statistics
            §f/stats route <routeId> [period] - View route statistics
            §f/stats report <systemId> <startDate> <endDate> - Generate detailed report
            §f/stats peaks <systemId> - View peak usage times
            §f/stats export <systemId> <type> - Export statistics
            
            §7Periods: daily, weekly, monthly, all_time
        """.trimIndent())
    }
}