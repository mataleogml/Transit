package com.example.transit.manager

import com.example.transit.TransitPlugin
import com.example.transit.model.*
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

import java.util.concurrent.ConcurrentHashMap

class StaffManager(private val plugin: TransitPlugin) {
    private val staffMembers = ConcurrentHashMap<String, MutableSet<StaffMember>>()
    private val staffPerformance = ConcurrentHashMap<UUID, StaffPerformance>()
    private val activeShifts = ConcurrentHashMap<UUID, StaffShift>()
    private val pendingPayments = ConcurrentHashMap<UUID, MutableList<PendingPayment>>()
    private val staffFile = File(plugin.dataFolder, "staff.yml")
    private val config = YamlConfiguration.loadConfiguration(staffFile)

    init {
        loadStaffData()
        startPaymentScheduler()
        startPerformanceTracker()
    }

    fun reload() {
        staffMembers.clear()
        staffPerformance.clear()
        activeShifts.clear()
        pendingPayments.clear()
        loadStaffData()
        startPaymentScheduler()
        startPerformanceTracker()
    }

    fun addStaffMember(
        playerId: UUID,
        systemId: String,
        role: StaffRole,
        salary: Double,
        period: PaymentPeriod = PaymentPeriod.MONTHLY
    ): Boolean {
        if (isStaff(playerId, systemId)) return false

        val staffMember = StaffMember(
            playerId = playerId,
            systemId = systemId,
            role = role,
            salary = salary,
            paymentPeriod = period,
            lastPaid = LocalDateTime.now(),
            hireDate = LocalDateTime.now()
        )
        
        staffMembers.getOrPut(systemId) { mutableSetOf() }.add(staffMember)
        initializePerformance(playerId)
        saveStaffMember(staffMember)
        return true
    }

    private fun initializePerformance(playerId: UUID) {
        staffPerformance[playerId] = StaffPerformance(
            playerId = playerId,
            shiftHistory = mutableListOf(),
            transactions = 0,
            customerInteractions = 0,
            incidentCount = 0,
            averageResponseTime = 0.0,
            lastEvaluation = LocalDateTime.now()
        )
    }

    fun removeStaffMember(playerId: UUID, systemId: String): Boolean {
        return staffMembers[systemId]?.let { members ->
            val removed = members.removeIf { it.playerId == playerId }
            if (removed) {
                saveStaffData()
                staffPerformance.remove(playerId)
            }
            removed
        } ?: false
    }

    fun updateStaffRole(playerId: UUID, systemId: String, newRole: StaffRole): Boolean {
        return staffMembers[systemId]?.find { it.playerId == playerId }?.let { member ->
            val updatedMember = member.copy(role = newRole)
            staffMembers[systemId]?.remove(member)
            staffMembers[systemId]?.add(updatedMember)
            saveStaffMember(updatedMember)
            true
        } ?: false
    }

    fun updateSalary(playerId: UUID, systemId: String, newSalary: Double): Boolean {
        return staffMembers[systemId]?.find { it.playerId == playerId }?.let { member ->
            val updatedMember = member.copy(salary = newSalary)
            staffMembers[systemId]?.remove(member)
            staffMembers[systemId]?.add(updatedMember)
            saveStaffMember(updatedMember)
            true
        } ?: false
    }

    fun startShift(playerId: UUID, systemId: String): Boolean {
        if (!isStaff(playerId, systemId)) return false
        if (activeShifts.containsKey(playerId)) return false

        val shift = StaffShift(
            startTime = LocalDateTime.now(),
            systemId = systemId,
            staffId = playerId
        )
        activeShifts[playerId] = shift
        staffPerformance[playerId]?.shiftHistory?.add(shift)
        return true
    }

    fun endShift(playerId: UUID): Boolean {
        val shift = activeShifts.remove(playerId) ?: return false
        shift.endTime = LocalDateTime.now()
        calculateShiftPay(playerId, shift)
        return true
    }

    private fun calculateShiftPay(playerId: UUID, shift: StaffShift) {
        val member = staffMembers[shift.systemId]?.find { it.playerId == playerId } ?: return
        val hoursWorked = ChronoUnit.HOURS.between(shift.startTime, shift.endTime)
        val hourlyRate = member.salary / (member.paymentPeriod.days * 8) // Assuming 8-hour workday
        val basePay = hourlyRate * hoursWorked
        
        // Calculate performance bonus
        val performance = staffPerformance[playerId]
        val bonus = performance?.calculateBonus() ?: 0.0
        
        // Create pending payment
        val totalPay = basePay + bonus
        pendingPayments.getOrPut(playerId) { mutableListOf() }.add(
            PendingPayment(
                playerId = playerId,
                systemId = shift.systemId,
                amount = totalPay,
                type = PaymentType.SHIFT_PAY
            )
        )
    }

    fun isStaff(playerId: UUID, systemId: String): Boolean {
        return staffMembers[systemId]?.any { it.playerId == playerId } ?: false
    }

    fun isStaffAnywhere(playerId: UUID): Boolean {
        return staffMembers.values.any { members -> 
            members.any { it.playerId == playerId }
        }
    }

    fun hasPermission(playerId: UUID, systemId: String, permission: StaffPermission): Boolean {
        return staffMembers[systemId]?.find { it.playerId == playerId }?.let { member ->
            member.role.permissions.contains(permission)
        } ?: false
    }

    fun getSystemStaff(systemId: String): Set<StaffMember> {
        return staffMembers[systemId]?.toSet() ?: emptySet()
    }

    fun getStaffPerformance(playerId: UUID): StaffPerformance? {
        return staffPerformance[playerId]
    }

    fun checkPendingPayments(player: Player) {
        pendingPayments[player.uniqueId]?.let { payments ->
            payments.removeAll { payment ->
                processPayment(player, payment)
            }
        }
    }

    private fun processPayment(player: Player, payment: PendingPayment): Boolean {
        val systemBalance = plugin.transactionManager.getSystemBalance(payment.systemId)
        if (systemBalance < payment.amount) return false

        if (plugin.economy.depositPlayer(player, payment.amount).transactionSuccess()) {
            plugin.transactionManager.logTransaction(
                Transaction(
                    playerId = payment.playerId,
                    systemId = payment.systemId,
                    fromStation = "STAFF_PAYMENT",
                    toStation = null,
                    amount = payment.amount,
                    type = TransactionType.STAFF_PAYMENT
                )
            )
            return true
        }
        return false
    }

    private fun startPaymentScheduler() {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            processPayments()
        }, 72000L, 72000L) // Every hour
    }

    private fun processPayments() {
        val now = LocalDateTime.now()
        staffMembers.values.flatten().forEach { member ->
            val lastPaid = member.lastPaid
            val daysUntilPayment = when (member.paymentPeriod) {
                PaymentPeriod.DAILY -> 1
                PaymentPeriod.WEEKLY -> 7
                PaymentPeriod.MONTHLY -> 30
            }
            
            if (ChronoUnit.DAYS.between(lastPaid, now) >= daysUntilPayment) {
                pendingPayments.getOrPut(member.playerId) { mutableListOf() }.add(
                    PendingPayment(
                        playerId = member.playerId,
                        systemId = member.systemId,
                        amount = member.salary,
                        type = PaymentType.REGULAR_SALARY
                    )
                )
            }
        }
    }

    private fun startPerformanceTracker() {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            updateAllPerformanceMetrics()
        }, 36000L, 36000L) // Every 30 minutes
    }

    private fun updateAllPerformanceMetrics() {
        staffPerformance.values.forEach { performance ->
            // Update performance metrics
            val activeShift = activeShifts[performance.playerId]
            if (activeShift != null) {
                performance.transactions = activeShift.transactions
                performance.incidentCount = activeShift.incidents
                
                // Calculate average response time from incident log
                val incidents = activeShift.getIncidents()
                if (incidents.isNotEmpty()) {
                    val totalResponseTime = incidents.sumOf { 
                        ChronoUnit.MINUTES.between(it.timestamp, LocalDateTime.now()).toDouble()
                    }
                    performance.averageResponseTime = totalResponseTime / incidents.size
                }
            }
            
            performance.lastEvaluation = LocalDateTime.now()
        }
    }

    private fun loadStaffData() {
        if (!config.contains("staff")) return
        
        config.getConfigurationSection("staff")?.getKeys(false)?.forEach { systemId ->
            val members = mutableSetOf<StaffMember>()
            config.getConfigurationSection("staff.$systemId")?.getKeys(false)?.forEach { playerIdStr ->
                val section = config.getConfigurationSection("staff.$systemId.$playerIdStr")
                section?.let {
                    members.add(
                        StaffMember(
                            playerId = UUID.fromString(playerIdStr),
                            systemId = systemId,
                            role = StaffRole.valueOf(it.getString("role", "TRAINEE")!!),
                            salary = it.getDouble("salary"),
                            paymentPeriod = PaymentPeriod.valueOf(it.getString("period", "MONTHLY")!!),
                            lastPaid = LocalDateTime.parse(it.getString("lastPaid") 
                                ?: LocalDateTime.now().toString()),
                            hireDate = LocalDateTime.parse(it.getString("hireDate") 
                                ?: LocalDateTime.now().toString())
                        )
                    )
                }
            }
            staffMembers[systemId] = members
        }
    }

    private fun saveStaffData() {
        config.set("staff", null) // Clear existing data
        staffMembers.entries.forEach { (systemId, members) ->
            members.forEach { staffMember ->
                saveStaffMember(staffMember)
            }
        }
        saveConfig()
    }

    private fun saveStaffMember(staffMember: StaffMember) {
        val path = "staff.${staffMember.systemId}.${staffMember.playerId}"
        config.set("$path.role", staffMember.role.name)
        config.set("$path.salary", staffMember.salary)
        config.set("$path.period", staffMember.paymentPeriod.name)
        config.set("$path.lastPaid", staffMember.lastPaid.toString())
        config.set("$path.hireDate", staffMember.hireDate.toString())
        saveConfig()
    }

    fun saveAll() {
        saveStaffData()
    }

    private fun saveConfig() {
        try {
            config.save(staffFile)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save staff data: ${e.message}")
        }
    }

    private data class PendingPayment(
        val playerId: UUID,
        val systemId: String,
        val amount: Double,
        val type: PaymentType,
        val timestamp: LocalDateTime = LocalDateTime.now()
    )

    private enum class PaymentType {
        REGULAR_SALARY,
        SHIFT_PAY,
        PERFORMANCE_BONUS
    }
}