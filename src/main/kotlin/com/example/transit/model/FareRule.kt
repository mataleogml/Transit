package com.example.transit.fare

import org.bukkit.configuration.ConfigurationSection

class FareRule(config: ConfigurationSection) {
    val fromPattern: String? = config.getString("from")
    val toPattern: String? = config.getString("to")
    val fare: Double = config.getDouble("fare")
    val ringDifference: Int? = if (config.contains("ringDifference")) config.getInt("ringDifference") else null
    val fromGroup: String? = config.getString("fromGroup")
    val toGroup: String? = config.getString("toGroup")
    val crossGroup: Boolean = config.getBoolean("crossGroup", false)

    fun matches(fromZone: String, toZone: String): Boolean {
        // Check exact zone patterns if specified
        if (fromPattern != null && toPattern != null) {
            val fromMatches = fromZone.matches(fromPattern.toRegex())
            val toMatches = toZone.matches(toPattern.toRegex())
            if (fromMatches && toMatches) return true
        }
        
        // Check group matching if specified
        if (fromGroup != null && toGroup != null) {
            return matchesGroups(fromZone, toZone)
        }
        
        return false
    }

    fun matchesRingDifference(difference: Int): Boolean {
        return ringDifference != null && ringDifference == difference
    }
    
    private fun matchesGroups(fromZone: String, toZone: String): Boolean {
        // This should be implemented based on your zone group configuration
        // For now, return false as we need the zone group configuration
        return false
    }
    
    override fun toString(): String {
        return "FareRule(from=$fromPattern, to=$toPattern, fare=$fare, ringDiff=$ringDifference)"
    }
}