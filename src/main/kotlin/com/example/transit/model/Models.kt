// Models.kt
package com.example.transit.model

import org.bukkit.Location
import java.time.LocalDateTime
import java.util.UUID

// Enums
enum class FareType {
    ZONE,
    FLAT,
    DISTANCE
}

enum class PaymentPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}

enum class StationStatus {
    ACTIVE,
    DISABLED,
    MAINTENANCE
}

// Data Classes
data class Station(
    val id: String,
    val name: String,
    val systemId: String,
    val location: Location,
    val zone: String,
    var status: StationStatus = StationStatus.ACTIVE
)

data class Route(
    val id: String,
    val name: String,
    val systemId: String,
    val stations: MutableList<String> = mutableListOf()
)

data class TransitSystem(
    val id: String,
    val name: String,
    val fareType: FareType,
    val fareData: Map<String, Any>,
    val maxFare: Double
)

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val playerId: UUID,
    val systemId: String,
    val fromStation: String,
    val toStation: String?,
    val amount: Double,
    val type: TransactionType,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class StaffMember(
    val playerId: UUID,
    val systemId: String,
    val salary: Double,
    val paymentPeriod: PaymentPeriod,
    val lastPaid: LocalDateTime = LocalDateTime.now()
)

data class Gate(
    val id: String,
    val location: Location,
    val systemId: String,
    val stationId: String,
    var enabled: Boolean = true
)

data class ZoneGroup(
    val name: String,
    val zones: Set<String>
)