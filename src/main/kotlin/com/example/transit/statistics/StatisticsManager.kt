package com.example.transit.statistics

import com.example.transit.TransitPlugin
import com.example.transit.model.Station
import com.example.transit.model.Transaction
import java.time.LocalDate
import java.time.LocalDateTime
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class StatisticsManager(private val plugin: TransitPlugin) {

    private val statisticsCache = mutableMapOf<String, Statistics>()
    private val statisticsFile = File(plugin.dataFolder, "statistics.yml")
    private val config = YamlConfiguration.loadConfiguration(statisticsFile)

    fun reload() {
        statisticsCache.clear()
        loadStatistics()
    }

    fun saveAll() {
        statisticsCache.forEach { (id, stats) -> saveStatistics(id, stats) }
    }

    fun getSystemStatistics(systemId: String, period: StatisticsPeriod = StatisticsPeriod.ALL_TIME): Statistics? {
        return statisticsCache["system_$systemId"]
    }

    fun getStationStatistics(stationId: String, period: StatisticsPeriod = StatisticsPeriod.ALL_TIME): Statistics? {
        return statisticsCache["station_$stationId"]
    }

    fun getRouteStatistics(routeId: String, period: StatisticsPeriod = StatisticsPeriod.ALL_TIME): Statistics? {
        return statisticsCache["route_$routeId"]
    }

    fun updateStatistics(transaction: Transaction) {
        // Update system statistics
        updateEntityStatistics("system_${transaction.systemId}", transaction)
        
        // Update station statistics
        updateEntityStatistics("station_${transaction.fromStation}", transaction)
        if (transaction.toStation != null) {
            updateEntityStatistics("station_${transaction.toStation}", transaction)
        }
    }

    fun generateReport(systemId: String, startDate: LocalDate, endDate: LocalDate): Report {
        // Implementation for generating reports
        return Report(
            period = ReportPeriod(startDate, endDate),
            totalRevenue = 0.0,
            totalTransactions = 0,
            averageTransactionValue = 0.0,
            peakHours = listOf(),
            busyStations = listOf(),
            popularRoutes = listOf()
        )
    }

    fun getPeakHours(systemId: String): List<PeakHour> {
        return statisticsCache["system_$systemId"]?.hourlyStats?.map { 
            PeakHour(it.key, it.value)
        }?.sortedByDescending { it.transactions } ?: listOf()
    }

    private fun updateEntityStatistics(entityId: String, transaction: Transaction) {
        val stats = statisticsCache.getOrPut(entityId) { Statistics() }
        stats.apply {
            totalRevenue += transaction.amount
            totalTransactions++
            lastUpdated = LocalDateTime.now()
            
            val hour = transaction.timestamp.hour
            hourlyStats[hour] = hourlyStats.getOrDefault(hour, 0) + 1
        }
        saveStatistics(entityId, stats)
    }

    private fun loadStatistics() {
        for (entityId in config.getKeys(false)) {
            val section = config.getConfigurationSection(entityId) ?: continue
            try {
                statisticsCache[entityId] = Statistics(
                    totalRevenue = section.getDouble("totalRevenue"),
                    totalTransactions = section.getInt("totalTransactions"),
                    entryCount = section.getInt("entryCount"),
                    exitCount = section.getInt("exitCount"),
                    directFares = section.getInt("directFares"),
                    lastUpdated = LocalDateTime.parse(section.getString("lastUpdated") 
                        ?: LocalDateTime.now().toString()),
                    hourlyStats = section.getConfigurationSection("hourlyStats")?.let { hourlySection ->
    hourlySection.getKeys(false).associate {
        it.toInt() to hourlySection.getInt(it)
    }.toMutableMap()
} ?: mutableMapOf()
                )
            } catch (e: Exception) {
                plugin.logger.severe("Failed to load statistics for $entityId: ${e.message}")
            }
        }
    }

    private fun saveStatistics(entityId: String, stats: Statistics) {
        config.set("$entityId.totalRevenue", stats.totalRevenue)
        config.set("$entityId.totalTransactions", stats.totalTransactions)
        config.set("$entityId.entryCount", stats.entryCount)
        config.set("$entityId.exitCount", stats.exitCount)
        config.set("$entityId.directFares", stats.directFares)
        config.set("$entityId.lastUpdated", stats.lastUpdated.toString())
        
        stats.hourlyStats.forEach { (hour, count) ->
            config.set("$entityId.hourlyStats.$hour", count)
        }
        
        try {
            config.save(statisticsFile)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save statistics: ${e.message}")
        }
    }

    data class Statistics(
        var totalRevenue: Double = 0.0,
        var totalTransactions: Int = 0,
        var entryCount: Int = 0,
        var exitCount: Int = 0,
        var directFares: Int = 0,
        var lastUpdated: LocalDateTime = LocalDateTime.now(),
        var hourlyStats: MutableMap<Int, Int> = mutableMapOf()
    ) {
        val averageFare: Double
            get() = if (totalTransactions > 0) totalRevenue / totalTransactions else 0.0
    }

    data class Report(
        val period: ReportPeriod,
        val totalRevenue: Double,
        val totalTransactions: Int,
        val averageTransactionValue: Double,
        val peakHours: List<PeakHour>,
        val busyStations: List<BusyStation>,
        val popularRoutes: List<PopularRoute>
    )

    data class ReportPeriod(
        val startDate: LocalDate,
        val endDate: LocalDate
    )

    data class PeakHour(
        val hour: Int,
        val transactions: Int
    )

    data class BusyStation(
        val name: String,
        val usageCount: Int
    )

    data class PopularRoute(
        val fromStationId: String,
        val toStationId: String,
        val usageCount: Int,
        val averageFare: Double
    )

    enum class StatisticsPeriod {
        DAILY,
        WEEKLY,
        MONTHLY,
        ALL_TIME
    }
}