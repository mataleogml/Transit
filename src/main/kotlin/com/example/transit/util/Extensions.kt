package com.example.transit.util

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
            org.bukkit.Bukkit.getWorld(parts[0]),
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

fun Double.format(decimals: Int = 2): String {
    return DecimalFormat("#,##0.${"0".repeat(decimals)}").format(this)
}

fun ConfigurationSection.getLocationList(path: String): List<Location> {
    return getStringList(path).mapNotNull { it.toLocation() }
}

fun Double.roundToDecimals(decimals: Int): Double {
    val factor = 10.0.pow(decimals)
    return (this * factor).roundToInt() / factor
}

private fun Double.pow(n: Int): Double = Math.pow(this, n.toDouble())