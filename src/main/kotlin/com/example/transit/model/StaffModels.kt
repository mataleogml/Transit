// src/main/kotlin/com/example/transit/model/StaffModels.kt
package com.example.transit.model

import java.time.LocalDateTime
import java.util.UUID

enum class StaffRole(val permissions: Set<StaffPermission>) {
    SUPERVISOR(setOf(
        StaffPermission.MANAGE_GATES,
        StaffPermission.MANAGE_FARES,
        StaffPermission.VIEW_STATISTICS,
        StaffPermission.MANAGE_STAFF,
        StaffPermission.OVERRIDE_GATES,
        StaffPermission.REFUND_TRANSACTIONS
    )),
    OPERATOR(setOf(
        StaffPermission.VIEW_STATISTICS,
        StaffPermission.OVERRIDE_GATES
    )),
    TRAINEE(setOf(
        StaffPermission.VIEW_STATISTICS
    ))
}

enum class StaffPermission {
    MANAGE_GATES,
    MANAGE_FARES,
    VIEW_STATISTICS,
    MANAGE_STAFF,
    OVERRIDE_GATES,
    REFUND_TRANSACTIONS
}

data class StaffMember(
    val playerId: UUID,
    val systemId: String,
    val role: StaffRole,
    val salary: Double,
    val paymentPeriod: PaymentPeriod,
    val lastPaid: LocalDateTime = LocalDateTime.now(),
    val hireDate: LocalDateTime = LocalDateTime.now()
)

data class StaffPerformance(
    val playerId: UUID,
    val shiftHistory: MutableList<StaffShift>,
    var transactions: Int = 0,
    var customerInteractions: Int = 0,
    var incidentCount: Int = 0,
    var averageResponseTime: Double = 0.0,
    var lastEvaluation: LocalDateTime = LocalDateTime.now()
) {
    fun calculateBonus(): Double {
        var bonus = 0.0
        if (transactions > 1000) bonus += 100.0
        if (transactions > 2000) bonus += 200.0
        if (averageResponseTime < 30.0) bonus += 100.0
        bonus += customerInteractions * 0.5
        return bonus
    }
}

data class StaffShift(
    val startTime: LocalDateTime,
    val systemId: String,
    val staffId: UUID,
    var endTime: LocalDateTime? = null,
    var transactions: Int = 0,
    var incidents: Int = 0,
    private val incidentLog: MutableList<ShiftIncident> = mutableListOf()
) {
    fun addIncident(severity: IncidentSeverity) {
        incidentLog.add(ShiftIncident(severity, LocalDateTime.now()))
    }

    fun getIncidents(): List<ShiftIncident> = incidentLog.toList()
}

data class ShiftIncident(
    val severity: IncidentSeverity,
    val timestamp: LocalDateTime
)

enum class IncidentSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}