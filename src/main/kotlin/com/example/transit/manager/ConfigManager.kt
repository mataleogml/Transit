package com.example.transit.manager

import com.example.transit.TransitPlugin
import com.example.transit.model.TransitSystem
import com.example.transit.model.FareType
import org.bukkit.configuration.ConfigurationSection

class ConfigManager(private val plugin: TransitPlugin) {
    private val systems = mutableMapOf<String, TransitSystem>()
    
    init {
        loadSystems()
    }

    fun reload() {
        systems.clear()
        loadSystems()
    }

    fun reloadConfig() {
        plugin.reloadConfig()
        reload()
    }

    fun getTransitSystem(id: String): TransitSystem? = systems[id]
    
    fun getSystems(): List<TransitSystem> = systems.values.toList()

    fun addTransitSystem(system: TransitSystem): Boolean {
        if (systems.containsKey(system.id)) return false
        
        systems[system.id] = system
        
        // Save to config
        val section = plugin.config.createSection("systems.${system.id}")
        section.set("name", system.name)
        section.set("fareType", system.fareType.name)
        section.set("maxFare", system.maxFare)
        
        // Save fare data based on type
        when (system.fareType) {
            FareType.FLAT -> {
                section.set("fare", system.fareData["fare"])
            }
            FareType.DISTANCE -> {
                section.set("baseRate", system.fareData["baseRate"])
                section.set("perBlock", system.fareData["perBlock"])
            }
            FareType.ZONE -> {
                val zonesSection = section.createSection("zones")
                val fareData = system.fareData
                
                // Save rings
                @Suppress("UNCHECKED_CAST")
                val rings = fareData["rings"] as? Map<String, Int>
                if (rings != null) {
                    val ringsSection = zonesSection.createSection("rings")
                    rings.forEach { (zone, ring) ->
                        ringsSection.set(zone, ring)
                    }
                }
                
                // Save groups
                @Suppress("UNCHECKED_CAST")
                val groups = fareData["groups"] as? Map<String, List<String>>
                if (groups != null) {
                    val groupsSection = zonesSection.createSection("groups")
                    groups.forEach { (name, zones) ->
                        groupsSection.set(name, zones)
                    }
                }
                
                // Save rules
                @Suppress("UNCHECKED_CAST")
                val rules = fareData["rules"] as? List<Map<String, Any>>
                if (rules != null) {
                    val rulesSection = zonesSection.createSection("rules")
                    rules.forEachIndexed { index, rule ->
                        val ruleSection = rulesSection.createSection(index.toString())
                        rule.forEach { (key, value) ->
                            ruleSection.set(key, value)
                        }
                    }
                }
            }
        }
        
        plugin.saveConfig()
        return true
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
        section.getConfigurationSection("zones")?.let { zones ->
            fareData["rings"] = loadZoneRings(zones)
            fareData["groups"] = loadZoneGroups(zones)
            fareData["rules"] = loadZoneRules(zones)
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