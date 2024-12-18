package com.example.transit.api

import com.example.transit.TransitPlugin
import com.example.transit.model.*
import com.example.transit.statistics.StatisticsManager
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID

class TransitAPI(private val plugin: TransitPlugin) {
    
    fun getSystemBalance(systemId: String): Double {
        return plugin.transactionManager.getSystemBalance(systemId)
    }

    fun isStaffMember(player: Player, systemId: String): Boolean {
        return plugin.staffManager.isStaff(player.uniqueId, systemId)
    }

    fun getStation(systemId: String, stationName: String): Station? {
        return plugin.stationManager.getStation("${systemId}_${stationName.toLowerCase()}")
    }

    fun getRoute(routeId: String): Route? {
        return plugin.routeManager.getRoute(routeId)
    }

    fun getPlayerTransactions(
        playerId: UUID,
        limit: Int = 10
    ): List<Transaction> {
        return plugin.transactionManager.getTransactionsByPlayer(playerId)
            .take(limit)
    }

    fun getSystemRevenue(
        systemId: String,
        period: StatisticsManager.StatisticsPeriod = StatisticsManager.StatisticsPeriod.ALL_TIME
    ): Double {
        return plugin.statisticsManager.getSystemStatistics(systemId, period)?.totalRevenue ?: 0.0
    }

    fun getStationStatistics(stationId: String): StatisticsManager.Statistics? {
        return plugin.statisticsManager.getStationStatistics(stationId)
    }

    fun getRouteStatistics(routeId: String): StatisticsManager.Statistics? {
        return plugin.statisticsManager.getRouteStatistics(routeId)
    }

    fun addStation(
        systemId: String,
        name: String,
        location: Location,
        zone: String = "1"
    ): Boolean {
        val station = Station(
            id = "${systemId}_${name.toLowerCase()}",
            name = name,
            systemId = systemId,
            location = location,
            zone = zone
        )
        return try {
            plugin.stationManager.addStation(station)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun addRouteStation(routeId: String, stationId: String): Boolean {
        return plugin.routeManager.addStationToRoute(routeId, stationId)
    }

    fun removeStation(systemId: String, stationName: String): Boolean {
        val stationId = "${systemId}_${stationName.toLowerCase()}"
        return plugin.stationManager.removeStation(stationId)
    }

    fun updateStationStatus(stationId: String, status: StationStatus): Boolean {
        return plugin.stationManager.updateStationStatus(stationId, status)
    }

    fun getNearbyStations(location: Location, radius: Double): List<Station> {
        return plugin.stationManager.getNearbyStations(location, radius)
    }

    fun getActiveJourney(playerId: UUID): JourneyData? {
        return plugin.fareManager.getActiveJourney(playerId)
    }

    fun getSystemStaff(systemId: String): Set<StaffMember> {
        return plugin.staffManager.getSystemStaff(systemId)
    }
}