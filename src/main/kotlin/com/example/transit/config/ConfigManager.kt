package com.example.transit.config

import com.example.transit.TransitPlugin
import com.example.transit.model.FareType
import com.example.transit.model.TransitSystem
import org.bukkit.configuration.ConfigurationSection
import java.io.File

class ConfigManager(private val plugin: TransitPlugin) {
    private val systems = mutableMapOf<String, TransitSystem>()
    private val messages = Messages(plugin)

    init {
        loadSystems()
    }

    fun getTransitSystem(id: String): TransitSystem? = systems[id]
    
    fun getSystems(): List<TransitSystem> = systems.values.toList()

    fun getMessage(key: String, vararg params: Pair<String, String>): String {
        return messages.get(key, params.toMap())
    }

    private fun loadSystems() {
        val systemsSection = plugin.config.getConfigurationSection("systems") ?: return
        
        for (systemId in systemsSection.getKeys(false)) {
            val section = systemsSection.getConfigurationSection(systemId) ?: continue
            
            try {
                systems[systemId] = TransitSystem(
                    id = systemId,
                    name = section.getString("name") ?: systemId,
                    fareType = FareType.valueOf(section.getString("fareType", "FLAT")!!.uppercase()),
                    fareData = loadFareData(section),
                    maxFare = section.getDouble("maxFare", 5.0)
                )
            } catch (e: Exception) {
                plugin.logger.severe("Failed to load system $systemId: ${e.message}")
            }
        }
    }

    private fun loadFareData(section: ConfigurationSection): Map<String, Any> {
        return when (FareType.valueOf(section.getString("fareType", "FLAT")!!.uppercase())) {
            FareType.ZONE -> loadZoneFareData(section)
            FareType.FLAT -> mapOf("fare" to section.getDouble("fare", 2.75))
            FareType.DISTANCE -> mapOf(
                "baseRate" to section.getDouble("baseRate", 2.0),
                "perBlock" to section.getDouble("perBlock", 0.01)
            )
        }
    }

    private fun loadZoneFareData(section: ConfigurationSection): Map<String, Any> {
        val fareData = mutableMapOf<String, Any>()
        
        section.getConfigurationSection("zones")?.let { zonesSection ->
            fareData["rings"] = loadZoneRings(zonesSection)
            fareData["groups"] = loadZoneGroups(zonesSection)
            fareData["rules"] = loadZoneRules(zonesSection)
        }
        
        return fareData
    }

    private fun loadZoneRings(section: ConfigurationSection): Map<String, Int> {
        val rings = mutableMapOf<String, Int>()
        section.getConfigurationSection("rings")?.let { ringsSection ->
            for (zone in ringsSection.getKeys(false)) {
                rings[zone] = ringsSection.getInt(zone)
            }
        }
        return rings
    }

    private fun loadZoneGroups(section: ConfigurationSection): Map<String, List<String>> {
        val groups = mutableMapOf<String, List<String>>()
        section.getConfigurationSection("groups")?.let { groupsSection ->
            for (group in groupsSection.getKeys(false)) {
                groups[group] = groupsSection.getStringList(group)
            }
        }
        return groups
    }

    private fun loadZoneRules(section: ConfigurationSection): List<Map<String, Any>> {
        val rules = mutableListOf<Map<String, Any>>()
        section.getConfigurationSection("rules")?.let { rulesSection ->
            for (rule in rulesSection.getKeys(false)) {
                val ruleSection = rulesSection.getConfigurationSection(rule) ?: continue
                rules.add(mapOf(
                    "from" to (ruleSection.getString("from") ?: ""),
                    "to" to (ruleSection.getString("to") ?: ""),
                    "fare" to ruleSection.getDouble("fare"),
                    "ringDifference" to ruleSection.getInt("ringDifference", -1),
                    "fromGroup" to (ruleSection.getString("fromGroup") ?: ""),
                    "toGroup" to (ruleSection.getString("toGroup") ?: ""),
                    "crossGroup" to ruleSection.getBoolean("crossGroup", false)
                ))
            }
        }
        return rules
    }
}