package com.example.transit.statistics

import com.example.transit.TransitPlugin
import com.example.transit.model.Transaction
import com.example.transit.model.TransactionType
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

class StatisticsManager(private val plugin: TransitPlugin) {
    private val statistics = ConcurrentHashMap<String, Statistics>()
    private val statisticsFile = File(plugin.dataFolder, "statistics.yml")
    private val config = YamlConfiguration.loadConfiguration(statisticsFile)

    init {
        loadStatistics()
        startAutoSave()
    }

    fun updateStatistics(transaction: Transaction) {
        when (transaction.type) {
            TransactionType.ENTRY, TransactionType.EXIT, TransactionType.FLAT_RATE -> {
                // Update system statistics
                updateStats("system", transaction.systemId, transaction, true)

                // Update station statistics
                updateStats("station", transaction.fromStation, transaction, true)
                transaction.toStation?.let { updateStats("station", it, transaction, true) }

                // Update route statistics if available
                updateRouteStatistics(transaction)
            }
            TransactionType.REFUND -> {
                // For refunds, subtract from the statistics
                updateStats("system", transaction.systemId, transaction, false)
                updateStats("station", transaction.fromStation, transaction, false)
                transaction.toStation?.let { updateStats("station", it, transaction, false) }
                updateRouteStatistics(transaction, false)
            }
            else -> {} // Other transaction types don't affect statistics
        }
    }

    private fun updateRouteStatistics(transaction: Transaction, add: Boolean = true) {
        val fromRoute = plugin.routeManager.getStationRoute(transaction.fromStation)
        val toRoute = transaction.toStation?.let { plugin.routeManager.getStationRoute(it) }
        
        if (fromRoute != null && fromRoute == toRoute) {
            updateStats("route", fromRoute, transaction, add)
        }
    }

    private fun updateStats(type: String, referenceId: String, transaction: Transaction, add: Boolean) {
        val key = "${type}_${referenceId}"
        statistics.compute(key) { _, stats ->
            (stats ?: Statistics(type, referenceId)).apply {
                val amount = if (add) transaction.amount else -transaction.amount
                totalRevenue += amount
                totalTransactions += if (add) 1 else -1
                lastUpdated = LocalDateTime.now()

                when {
                    transaction.type == TransactionType.ENTRY -> entryCount++
                    transaction.type == TransactionType.EXIT -> exitCount++
                    transaction.type == TransactionType.FLAT_RATE -> directFares++
                }

                // Update hourly statistics
                val hour = transaction.timestamp.hour
                hourlyStats[hour] = (hourlyStats[hour] ?: 0) + (if (add) 1 else -1)
            }
        }
        saveStatistics(key)
    }

    fun getSystemStatistics(systemId: String, period: StatisticsPeriod = StatisticsPeriod.ALL_TIME): Statistics? {
        val stats = statistics["system_$systemId"] ?: return null
        return when (period) {
            StatisticsPeriod.ALL_TIME -> stats
            else -> filterStatsByPeriod(stats, period)
        }
    }

    fun getStationStatistics(stationId: String, period: StatisticsPeriod = StatisticsPeriod.ALL_TIME): Statistics? {
        val stats = statistics["station_$stationId"] ?: return null
        return when (period) {
            StatisticsPeriod.ALL_TIME -> stats
            else -> filterStatsByPeriod(stats, period)
        }
    }

    fun getRouteStatistics(routeId: String, period: StatisticsPeriod = StatisticsPeriod.ALL_TIME): Statistics? {
        val stats = statistics["route_$routeId"] ?: return null
        return when (period) {
            StatisticsPeriod.ALL_TIME -> stats
            else -> filterStatsByPeriod(stats, period)
        }
    }

    private fun filterStatsByPeriod(stats: Statistics, period: StatisticsPeriod): Statistics {
        val transactions = plugin.transactionManager.getTransactions(
            startTime = when (period) {
                StatisticsPeriod.DAILY -> LocalDateTime.now().minusDays(1)
                StatisticsPeriod.WEEKLY -> LocalDateTime.now().minusWeeks(1)
                StatisticsPeriod.MONTHLY -> LocalDateTime.now().minusMonths(1)
                StatisticsPeriod.ALL_TIME -> null
            }
        )

        return Statistics(stats.type, stats.referenceId).apply {
            transactions.forEach { transaction ->
                totalRevenue += transaction.amount
                totalTransactions++
                
                when (transaction.type) {
                    TransactionType.ENTRY -> entryCount++
                    TransactionType.EXIT -> exitCount++
                    TransactionType.FLAT_RATE -> directFares++
                    else -> {}
                }

                val hour = transaction.timestamp.hour
                hourlyStats[hour] = (hourlyStats[hour] ?: 0) + 1
            }
            lastUpdated = LocalDateTime.now()
        }
    }

    private fun loadStatistics() {
        for (key in config.getKeys(false)) {
            val section = config.getConfigurationSection(key) ?: continue
            try {
                val type = section.getString("type") ?: continue
                val referenceId = section.getString("referenceId") ?: continue
                
                statistics[key] = Statistics(
                    type = type,
                    referenceId = referenceId,
                    totalRevenue = section.getDouble("totalRevenue"),
                    totalTransactions = section.getInt("totalTransactions"),
                    entryCount = section.getInt("entryCount"),
                    exitCount = section.getInt("exitCount"),
                    directFares = section.getInt("directFares"),
                    hourlyStats = section.getConfigurationSection("hourlyStats")?.let { hourly ->
                        hourly.getKeys(false).associate {
                            it.toInt() to hourly.getInt(it)
                        }
                    }?.toMutableMap() ?: mutableMapOf(),
                    lastUpdated = LocalDateTime.parse(section.getString("lastUpdated") 
                        ?: LocalDateTime.now().toString())
                )
            } catch (e: Exception) {
                plugin.logger.severe("Failed to load statistics for $key: ${e.message}")
            }
        }
    }

    private fun saveStatistics(key: String) {
        statistics[key]?.let { stats ->
            config.set("$key.type", stats.type)
            config.set("$key.referenceId", stats.referenceId)
            config.set("$key.totalRevenue", stats.totalRevenue)
            config.set("$key.totalTransactions", stats.totalTransactions)
            config.set("$key.entryCount", stats.entryCount)
            config.set("$key.exitCount", stats.exitCount)
            config.set("$key.directFares", stats.directFares)
            stats.hourlyStats.forEach { (hour, count) ->
                config.set("$key.hourlyStats.$hour", count)
            }
            config.set("$key.lastUpdated", stats.lastUpdated.toString())
        }
        saveConfig()
    }

    fun saveAll() {
        statistics.keys.forEach { saveStatistics(it) }
    }

    private fun saveConfig() {
        try {
            config.save(statisticsFile)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save statistics: ${e.message}")
        }
    }

    private fun startAutoSave() {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            saveAll()
        }, 6000L, 6000L) // Auto-save every 5 minutes
    }

    data class Statistics(
        val type: String,
        val referenceId: String,
        var totalRevenue: Double = 0.0,
        var totalTransactions: Int = 0,
        var entryCount: Int = 0,
        var exitCount: Int = 0,
        var directFares: Int = 0,
        val hourlyStats: MutableMap<Int, Int> = mutableMapOf(),
        var lastUpdated: LocalDateTime = LocalDateTime.now()
    )

    enum class StatisticsPeriod {
        DAILY,
        WEEKLY,
        MONTHLY,
        ALL_TIME
    }
}