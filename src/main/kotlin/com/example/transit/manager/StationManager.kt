package com.example.transit.manager

import com.example.transit.TransitPlugin
import com.example.transit.model.Station
import com.example.transit.model.StationStatus
import com.example.transit.util.toLocation
import com.example.transit.util.toStorableString
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class StationManager(private val plugin: TransitPlugin) {
    private val stations = mutableMapOf<String, Station>()
    private val stationsFile = File(plugin.dataFolder, "stations.yml")
    private val config = YamlConfiguration.loadConfiguration(stationsFile)

    init {
        loadStations()
    }

    fun addStation(station: Station): Boolean {
        if (stations.containsKey(station.id)) return false
        stations[station.id] = station
        saveStation(station)
        return true
    }

    fun removeStation(id: String): Boolean {
        if (!stations.containsKey(id)) return false
        
        // Check if station is used in any routes
        val routes = plugin.routeManager.getStationRoutes(id)
        if (routes.isNotEmpty()) {
            routes.forEach { route ->
                plugin.routeManager.removeStationFromRoute(route.id, id)
            }
        }

        // Remove any gates at this station
        stations[id]?.let { station ->
            plugin.gateManager.removeGatesForStation(station.id)
        }

        stations.remove(id)
        config.set(id, null)
        saveConfig()
        return true
    }

    fun getStation(id: String): Station? = stations[id]

    fun getAllStations(): List<Station> = stations.values.toList()

    fun getSystemStations(systemId: String): List<Station> =
        stations.values.filter { it.systemId == systemId }

    fun getStationsByZone(systemId: String, zone: String): List<Station> =
        stations.values.filter { it.systemId == systemId && it.zone == zone }

    fun getNearbyStations(location: org.bukkit.Location, radius: Double): List<Station> =
        stations.values.filter {
            it.location.world?.name == location.world?.name &&
            it.location.distance(location) <= radius
        }

    fun updateStationStatus(stationId: String, status: StationStatus): Boolean {
        return stations[stationId]?.let { station ->
            station.status = status
            // Update any gates at this station
            plugin.gateManager.updateStationGates(stationId)
            saveStation(station)
            true
        } ?: false
    }

    fun updateStationZone(stationId: String, newZone: String): Boolean {
        return stations[stationId]?.let { station ->
            val updatedStation = station.copy(zone = newZone)
            stations[stationId] = updatedStation
            saveStation(updatedStation)
            true
        } ?: false
    }

    fun relocateStation(stationId: String, newLocation: org.bukkit.Location): Boolean {
        return stations[stationId]?.let { station ->
            val updatedStation = station.copy(location = newLocation)
            stations[stationId] = updatedStation
            saveStation(updatedStation)
            true
        } ?: false
    }

    private fun loadStations() {
        for (stationId in config.getKeys(false)) {
            val section = config.getConfigurationSection(stationId) ?: continue
            try {
                stations[stationId] = Station(
                    id = stationId,
                    name = section.getString("name") ?: "",
                    systemId = section.getString("systemId") ?: "",
                    location = section.getString("location")?.toLocation() ?: continue,
                    zone = section.getString("zone") ?: "1",
                    status = StationStatus.valueOf(section.getString("status") ?: "ACTIVE")
                )
            } catch (e: Exception) {
                plugin.logger.severe("Failed to load station $stationId: ${e.message}")
            }
        }
    }

    private fun saveStation(station: Station) {
        config.set("${station.id}.name", station.name)
        config.set("${station.id}.systemId", station.systemId)
        config.set("${station.id}.location", station.location.toStorableString())
        config.set("${station.id}.zone", station.zone)
        config.set("${station.id}.status", station.status.name)
        saveConfig()
    }

    fun saveAll() {
        stations.values.forEach { saveStation(it) }
    }

    private fun saveConfig() {
        try {
            config.save(stationsFile)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save stations: ${e.message}")
        }
    }
}