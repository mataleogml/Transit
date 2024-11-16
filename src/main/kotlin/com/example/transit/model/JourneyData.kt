package com.example.transit.model

import org.bukkit.Location
import java.time.LocalDateTime

data class JourneyData(
    val systemId: String,
    val startStation: String,
    val startLocation: Location,
    val startTime: LocalDateTime = LocalDateTime.now()
)