package com.example.transit.fare

import org.bukkit.configuration.ConfigurationSection
import kotlin.math.abs

class ZoneFareCalculator(config: ConfigurationSection) {
    private val zoneRings = mutableMapOf<String, Int>()
    private val zoneGroups = mutableMapOf<String, Set<String>>()
    private val fareRules = mutableListOf<FareRule>()
    private val defaultFare: Double
    
    init {
        loadZoneConfiguration(config)
        defaultFare = config.getDouble("defaultFare", 5.0)
    }

    fun calculateFare(fromZone: String, toZone: String): Double {
        // Try exact zone match first
        fareRules.firstOrNull { rule -> 
            rule.matches(fromZone, toZone)
        }?.let { return it.fare }

        // Try ring-based calculation
        if (zoneRings.containsKey(fromZone) && zoneRings.containsKey(toZone)) {
            val ringDifference = abs(zoneRings[fromZone]!! - zoneRings[toZone]!!)
            fareRules.firstOrNull { rule -> 
                rule.matchesRingDifference(ringDifference)
            }?.let { return it.fare }
        }

        // Try group-based rules
        val fromGroups = findZoneGroups(fromZone)
        val toGroups = findZoneGroups(toZone)
        if (fromGroups.isNotEmpty() && toGroups.isNotEmpty()) {
            fareRules.firstOrNull { rule ->
                fromGroups.contains(rule.fromGroup) && toGroups.contains(rule.toGroup)
            }?.let { return it.fare }
        }

        return defaultFare
    }

    private fun loadZoneConfiguration(config: ConfigurationSection) {
        // Load zone rings
        config.getConfigurationSection("rings")?.let { ringsSection ->
            for (zone in ringsSection.getKeys(false)) {
                zoneRings[zone] = ringsSection.getInt(zone)
            }
        }

        // Load zone groups
        config.getConfigurationSection("groups")?.let { groupsSection ->
            for (groupName in groupsSection.getKeys(false)) {
                zoneGroups[groupName] = groupsSection.getStringList(groupName).toSet()
            }
        }

        // Load fare rules
        config.getConfigurationSection("rules")?.let { rulesSection ->
            for (ruleKey in rulesSection.getKeys(false)) {
                val ruleSection = rulesSection.getConfigurationSection(ruleKey)
                ruleSection?.let {
                    fareRules.add(FareRule(it))
                }
            }
        }
    }

    private fun findZoneGroups(zone: String): Set<String> {
        return zoneGroups.filterValues { it.contains(zone) }.keys
    }

    fun getZoneRing(zone: String): Int? = zoneRings[zone]
    
    fun isZoneInGroup(zone: String, group: String): Boolean {
        return zoneGroups[group]?.contains(zone) ?: false
    }
}