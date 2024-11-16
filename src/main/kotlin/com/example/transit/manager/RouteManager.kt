package com.example.transit.manager

import com.example.transit.TransitPlugin
import com.example.transit.model.Route
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class RouteManager(private val plugin: TransitPlugin) {
    private val routes = mutableMapOf<String, Route>()
    private val routesFile: File
        get() = File(plugin.dataFolder, "routes.yml")
    private val config = YamlConfiguration.loadConfiguration(routesFile)

    init {
        loadRoutes()
    }

    private fun loadRoutes() {
        for (routeId in config.getKeys(false)) {
            val section = config.getConfigurationSection(routeId) ?: continue
            try {
                routes[routeId] = Route(
                    id = routeId,
                    name = section.getString("name") ?: continue,
                    systemId = section.getString("systemId") ?: continue,
                    stations = section.getStringList("stations").toMutableList()
                )
            } catch (e: Exception) {
                plugin.logger.severe("Failed to load route $routeId: ${e.message}")
            }
        }
    }

    fun addRoute(route: Route): Boolean {
        if (routes.containsKey(route.id)) return false
        routes[route.id] = route
        saveRoute(route)
        return true
    }

    fun removeRoute(routeId: String): Boolean {
        if (!routes.containsKey(routeId)) return false
        routes.remove(routeId)
        config.set(routeId, null)
        saveConfig()
        return true
    }

    fun getRoute(id: String): Route? = routes[id]

    fun getRoutes(): List<String> = routes.keys.toList()

    fun getSystems(): List<String> = routes.values.map { it.systemId }.distinct()

    fun getSystemRoutes(systemId: String): List<Route> =
        routes.values.filter { it.systemId == systemId }

    fun getStationRoutes(stationId: String): List<Route> =
        routes.values.filter { it.stations.contains(stationId) }

    fun addStationToRoute(routeId: String, stationId: String): Boolean {
        return routes[routeId]?.let { route ->
            if (!route.stations.contains(stationId)) {
                route.stations.add(stationId)
                saveRoute(route)
                true
            } else false
        } ?: false
    }

    fun removeStationFromRoute(routeId: String, stationId: String): Boolean {
        return routes[routeId]?.let { route ->
            if (route.stations.remove(stationId)) {
                saveRoute(route)
                true
            } else false
        } ?: false
    }

    fun reorderStations(routeId: String, newOrder: List<String>): Boolean {
        return routes[routeId]?.let { route ->
            if (route.stations.toSet() == newOrder.toSet()) {
                route.stations.clear()
                route.stations.addAll(newOrder)
                saveRoute(route)
                true
            } else false
        } ?: false
    }

    private fun saveRoute(route: Route) {
        config.set("${route.id}.name", route.name)
        config.set("${route.id}.systemId", route.systemId)
        config.set("${route.id}.stations", route.stations)
        saveConfig()
    }

    fun saveAll() {
        routes.values.forEach { saveRoute(it) }
    }

    private fun saveConfig() {
        try {
            config.save(routesFile)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save routes: ${e.message}")
        }
    }
}