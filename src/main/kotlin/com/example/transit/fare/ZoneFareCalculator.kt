package com.example.transit.fare

import org.bukkit.configuration.ConfigurationSection
import kotlin.math.abs

class ZoneFareCalculator(config: ConfigurationSection) {
    private val zoneRings = mutableMapOf<String, Int>()
    private val zoneGroups = mutableMapOf<String, Set<String>>()
    private val fareRules = mutableListOf<FareRule>()
    private val defaultFare: Double
    private val peakHours: Set<Int>
    private val peakMultiplier: Double
    private val specialFares = mutableMapOf<String, Double>() // For student, senior rates etc
    
    init {
        defaultFare = config.getDouble("defaultFare", 5.0)
        peakHours = config.getIntegerList("peakHours").toSet()
        peakMultiplier = config.getDouble("peakMultiplier", 1.5)
        loadZoneConfiguration(config)
        loadSpecialFares(config)
    }

    fun calculateFare(
        fromZone: String, 
        toZone: String, 
        hour: Int = -1,
        fareType: String = "STANDARD"
    ): Double {
        // Try exact zone match first
        var baseFare = fareRules.firstOrNull { rule -> 
            rule.matches(fromZone, toZone)
        }?.fare

        // Try ring-based calculation if no exact match
        if (baseFare == null && zoneRings.containsKey(fromZone) && zoneRings.containsKey(toZone)) {
            val ringDifference = abs(zoneRings[fromZone]!! - zoneRings[toZone]!!)
            baseFare = fareRules.firstOrNull { rule -> 
                rule.matchesRingDifference(ringDifference)
            }?.fare
        }

        // Try group-based rules if still no match
        if (baseFare == null) {
            val fromGroups = findZoneGroups(fromZone)
            val toGroups = findZoneGroups(toZone)
            if (fromGroups.isNotEmpty() && toGroups.isNotEmpty()) {
                baseFare = fareRules.firstOrNull { rule ->
                    fromGroups.contains(rule.fromGroup) && toGroups.contains(rule.toGroup)
                }?.fare
            }
        }

        // Use default fare if no rules match
        baseFare = baseFare ?: defaultFare

        // Apply peak hour multiplier if applicable
        if (hour in peakHours) {
            baseFare *= peakMultiplier
        }

        // Apply special fare rate if applicable
        val specialRate = specialFares[fareType]
        if (specialRate != null) {
            baseFare *= specialRate
        }

        return baseFare
    }

    fun isPeakHour(hour: Int): Boolean = hour in peakHours

    fun getAvailableFareTypes(): Set<String> = specialFares.keys + "STANDARD"

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
                    fareRules.add(FareRule(
                        fromPattern = it.getString("from"),
                        toPattern = it.getString("to"),
                        fare = it.getDouble("fare"),
                        ringDifference = if (it.contains("ringDifference")) it.getInt("ringDifference") else null,
                        fromGroup = it.getString("fromGroup"),
                        toGroup = it.getString("toGroup"),
                        crossGroup = it.getBoolean("crossGroup", false),
                        timeRestriction = loadTimeRestriction(it)
                    ))
                }
            }
        }
    }

    private fun loadSpecialFares(config: ConfigurationSection) {
        config.getConfigurationSection("specialFares")?.let { specialSection ->
            for (fareType in specialSection.getKeys(false)) {
                specialFares[fareType] = specialSection.getDouble(fareType)
            }
        }
    }

    private fun loadTimeRestriction(config: ConfigurationSection): TimeRestriction? {
        return if (config.contains("timeRestriction")) {
            val timeSection = config.getConfigurationSection("timeRestriction")
            TimeRestriction(
                startHour = timeSection?.getInt("startHour") ?: 0,
                endHour = timeSection?.getInt("endHour") ?: 23,
                daysOfWeek = timeSection?.getIntegerList("daysOfWeek")?.toSet() ?: setOf()
            )
        } else null
    }

    private fun findZoneGroups(zone: String): Set<String> {
        return zoneGroups.filterValues { it.contains(zone) }.keys
    }

    fun getZoneRing(zone: String): Int? = zoneRings[zone]
    
    fun isZoneInGroup(zone: String, group: String): Boolean {
        return zoneGroups[group]?.contains(zone) ?: false
    }

    data class TimeRestriction(
        val startHour: Int,
        val endHour: Int,
        val daysOfWeek: Set<Int>
    )

    data class FareRule(
        val fromPattern: String?,
        val toPattern: String?,
        val fare: Double,
        val ringDifference: Int?,
        val fromGroup: String?,
        val toGroup: String?,
        val crossGroup: Boolean,
        val timeRestriction: TimeRestriction?
    ) {
        fun matches(fromZone: String, toZone: String): Boolean {
            if (fromPattern == null || toPattern == null) return false
            return fromZone.matches(fromPattern.toRegex()) && 
                   toZone.matches(toPattern.toRegex())
        }

        fun matchesRingDifference(difference: Int): Boolean {
            return ringDifference != null && ringDifference == difference
        }
    }
}