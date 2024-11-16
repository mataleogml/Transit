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

    fun addRoute(route: Route) {
        routes[route.id] = route
        saveRoute(route)
    }

    fun getRoute(id: String): Route? = routes[id]

    fun getSystemRoutes(systemId: String): List<Route> =
        routes.values.filter { it.systemId == systemId }

    fun addStationToRoute(routeId: String, stationId: String): Boolean {
        return routes[routeId]?.let { route ->
            if (!route.stations.contains(stationId)) {
                route.stations.add(stationId)
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

    private fun loadRoutes() {
        config.getKeys(false).forEach { id ->
            val section = config.getConfigurationSection(id) ?: return@forEach
            
            routes[id] = Route(
                id = id,
                name = section.getString("name") ?: "",
                systemId = section.getString("system") ?: "",
                stations = section.getStringList("stations").toMutableList()
            )
        }
    }

    private fun saveRoute(route: Route) {
        config.apply {
            set("${route.id}.name", route.name)
            set("${route.id}.system", route.systemId)
            set("${route.id}.stations", route.stations)
        }
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