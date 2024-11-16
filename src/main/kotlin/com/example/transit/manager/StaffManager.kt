package com.example.transit.manager

import com.example.transit.TransitPlugin
import com.example.transit.model.PaymentPeriod
import com.example.transit.model.StaffMember
import com.example.transit.model.Transaction
import com.example.transit.model.TransactionType
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class StaffManager(private val plugin: TransitPlugin) {
    private val staffMembers = ConcurrentHashMap<String, MutableSet<StaffMember>>()
    private val staffFile = File(plugin.dataFolder, "staff.yml")
    private val config = YamlConfiguration.loadConfiguration(staffFile)
    private val pendingPayments = ConcurrentHashMap<UUID, MutableList<Payment>>()

    init {
        loadStaffData()
        startPaymentScheduler()
    }

    fun getSystemStaff(systemId: String): Set<StaffMember> {
        return staffMembers[systemId]?.toSet() ?: emptySet()
    }

    fun addStaffMember(
        playerId: UUID,
        systemId: String,
        salary: Double,
        period: PaymentPeriod = PaymentPeriod.MONTHLY
    ): Boolean {
        if (isStaff(playerId, systemId)) return false

        val staffMember = StaffMember(
            playerId = playerId,
            systemId = systemId,
            salary = salary,
            paymentPeriod = period
        )
        
        staffMembers.getOrPut(systemId) { mutableSetOf() }.add(staffMember)
        saveStaffMember(staffMember)
        return true
    }

    fun removeStaffMember(playerId: UUID, systemId: String): Boolean {
        return staffMembers[systemId]?.removeIf { 
            it.playerId == playerId 
        }?.also { 
            if (it) saveStaffData() 
        } ?: false
    }

    fun isStaff(playerId: UUID, systemId: String): Boolean {
        return staffMembers[systemId]?.any { it.playerId == playerId } ?: false
    }

    fun isStaffAnywhere(playerId: UUID): Boolean {
        return staffMembers.values.any { members ->
            members.any { it.playerId == playerId }
        }
    }

    fun updateSalary(playerId: UUID, systemId: String, newSalary: Double): Boolean {
        staffMembers[systemId]?.find { it.playerId == playerId }?.let { member ->
            val updatedMember = member.copy(salary = newSalary)
            staffMembers[systemId]?.remove(member)
            staffMembers[systemId]?.add(updatedMember)
            saveStaffMember(updatedMember)
            return true
        }
        return false
    }

    private fun startPaymentScheduler() {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            processPayments()
        }, 72000L, 72000L) // Check every hour
    }

    private fun processPayments() {
        val now = LocalDateTime.now()
        staffMembers.forEach { (systemId, members) ->
            members.forEach { staffMember ->
                if (shouldProcessPayment(staffMember, now)) {
                    processStaffPayment(staffMember, systemId)
                }
            }
        }
    }

    private fun shouldProcessPayment(staffMember: StaffMember, now: LocalDateTime): Boolean {
        val daysSinceLastPaid = ChronoUnit.DAYS.between(staffMember.lastPaid, now)
        return when (staffMember.paymentPeriod) {
            PaymentPeriod.DAILY -> daysSinceLastPaid >= 1
            PaymentPeriod.WEEKLY -> daysSinceLastPaid >= 7
            PaymentPeriod.MONTHLY -> daysSinceLastPaid >= 30
        }
    }

    private fun processStaffPayment(staffMember: StaffMember, systemId: String) {
        val player = plugin.server.getPlayer(staffMember.playerId)
        
        if (player != null && player.isOnline) {
            payStaff(player, staffMember, systemId)
        } else {
            queuePayment(staffMember)
        }
    }

    private fun payStaff(player: Player, staffMember: StaffMember, systemId: String) {
        val systemBalance = plugin.transactionManager.getSystemBalance(systemId)
        if (systemBalance < staffMember.salary) {
            plugin.logger.warning("Insufficient system balance for staff payment: $systemId")
            return
        }

        if (plugin.economy.depositPlayer(player, staffMember.salary).transactionSuccess()) {
            plugin.transactionManager.logTransaction(
                Transaction(
                    playerId = staffMember.playerId,
                    systemId = systemId,
                    fromStation = "STAFF_PAYMENT",
                    toStation = null,
                    amount = staffMember.salary,
                    type = TransactionType.STAFF_PAYMENT
                )
            )

            updateLastPaidTime(staffMember)
            player.sendMessage("§aReceived staff payment: $${staffMember.salary} for system $systemId")
        }
    }

    private fun queuePayment(staffMember: StaffMember) {
        pendingPayments.getOrPut(staffMember.playerId) { mutableListOf() }
            .add(Payment(staffMember.systemId, staffMember.salary))
    }

    fun checkPendingPayments(player: Player) {
        pendingPayments[player.uniqueId]?.let { payments ->
            payments.forEach { payment ->
                if (plugin.economy.depositPlayer(player, payment.amount).transactionSuccess()) {
                    plugin.transactionManager.logTransaction(
                        Transaction(
                            playerId = player.uniqueId,
                            systemId = payment.systemId,
                            fromStation = "STAFF_PAYMENT_DELAYED",
                            toStation = null,
                            amount = payment.amount,
                            type = TransactionType.STAFF_PAYMENT
                        )
                    )
                    player.sendMessage("§aReceived pending staff payment: $${payment.amount}")
                }
            }
            pendingPayments.remove(player.uniqueId)
        }
    }

    private fun updateLastPaidTime(staffMember: StaffMember) {
        staffMembers[staffMember.systemId]?.let { members ->
            members.remove(staffMember)
            members.add(staffMember.copy(lastPaid = LocalDateTime.now()))
            saveStaffData()
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
                            salary = it.getDouble("salary"),
                            paymentPeriod = PaymentPeriod.valueOf(it.getString("period", "MONTHLY")!!),
                            lastPaid = LocalDateTime.parse(it.getString("lastPaid") 
                                ?: LocalDateTime.now().toString())
                        )
                    )
                }
            }
            staffMembers[systemId] = members
        }
    }

    private fun saveStaffMember(staffMember: StaffMember) {
        val path = "staff.${staffMember.systemId}.${staffMember.playerId}"
        config.set("$path.salary", staffMember.salary)
        config.set("$path.period", staffMember.paymentPeriod.name)
        config.set("$path.lastPaid", staffMember.lastPaid.toString())
        saveConfig()
    }

    fun saveStaffData() {
        config.set("staff", null) // Clear existing data
        staffMembers.forEach { (systemId, members) ->
            members.forEach { staffMember ->
                saveStaffMember(staffMember)
            }
        }
    }

    private fun saveConfig() {
        try {
            config.save(staffFile)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save staff data: ${e.message}")
        }
    }

    private data class Payment(
        val systemId: String,
        val amount: Double
    )
}