package com.example.transit.statistics

import com.example.transit.TransitPlugin
import com.example.transit.model.Transaction
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

class StatisticsManager(private val plugin: TransitPlugin) {
    private val statistics = ConcurrentHashMap<String, Statistics>()

    fun updateStatistics(transaction: Transaction) {
        // Update system statistics
        updateStats("system", transaction.systemId, transaction)

        // Update station statistics
        updateStats("station", transaction.fromStation, transaction)
        transaction.toStation?.let { updateStats("station", it, transaction) }

        // Update route statistics if available
        plugin.routeManager.getRouteForStations(transaction.systemId, 
            transaction.fromStation, transaction.toStation)?.let { routeId ->
            updateStats("route", routeId, transaction)
        }
    }

    private fun updateStats(type: String, referenceId: String, transaction: Transaction) {
        val key = "${type}_${referenceId}"
        statistics.compute(key) { _, stats ->
            (stats ?: Statistics(type, referenceId)).apply {
                totalRevenue += transaction.amount
                totalTransactions++
                lastUpdated = LocalDateTime.now()
            }
        }
    }

    fun getStatistics(type: String, referenceId: String): Statistics? {
        return statistics["${type}_${referenceId}"]
    }

    fun getSystemRevenue(systemId: String, period: StatisticsPeriod = StatisticsPeriod.ALL_TIME): Double {
        return when (period) {
            StatisticsPeriod.ALL_TIME -> statistics["system_$systemId"]?.totalRevenue ?: 0.0
            StatisticsPeriod.DAILY -> calculatePeriodRevenue(systemId, 1)
            StatisticsPeriod.WEEKLY -> calculatePeriodRevenue(systemId, 7)
            StatisticsPeriod.MONTHLY -> calculatePeriodRevenue(systemId, 30)
        }
    }

    private fun calculatePeriodRevenue(systemId: String, days: Int): Double {
        val startTime = LocalDateTime.now().minusDays(days.toLong())
        return plugin.database.getTransactions(
            systemId = systemId,
            startTime = startTime
        ).sumOf { it.amount }
    }

    data class Statistics(
        val type: String,
        val referenceId: String,
        var totalRevenue: Double = 0.0,
        var totalTransactions: Int = 0,
        var lastUpdated: LocalDateTime = LocalDateTime.now()
    )

    enum class StatisticsPeriod {
        DAILY,
        WEEKLY,
        MONTHLY,
        ALL_TIME
    }
}