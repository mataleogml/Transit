package com.example.transit.util

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import java.text.DecimalFormat
import kotlin.math.roundToInt

fun Location.toStorableString(): String {
    return "${world?.name},${x},${y},${z},${yaw},${pitch}"
}

fun String.toLocation(): Location? {
    val parts = split(",")
    if (parts.size != 6) return null
    
    return try {
        Location(
            Bukkit.getWorld(parts[0]),
            parts[1].toDouble(),
            parts[2].toDouble(),
            parts[3].toDouble(),
            parts[4].toFloat(),
            parts[5].toFloat()
        )
    } catch (e: Exception) {
        null
    }
}

fun ConfigurationSection.getLocationList(path: String): List<Location> {
    return getStringList(path).mapNotNull { it.toLocation() }
}

fun Double.roundToDecimals(decimals: Int): Double {
    val factor = 10.0.pow(decimals)
    return (this * factor).roundToInt() / factor
}

fun Double.formatCurrency(): String {
    return DecimalFormat("$#,##0.00").format(this)
}

private fun Double.pow(n: Int): Double = Math.pow(this, n.toDouble())

fun String.sanitizeFileName(): String {
    return this.replace(Regex("[^a-zA-Z0-9.-]"), "_")
}

fun Location.distanceTo(other: Location): Double {
    return if (world?.name == other.world?.name) {
        distance(other)
    } else {
        Double.POSITIVE_INFINITY
    }
}