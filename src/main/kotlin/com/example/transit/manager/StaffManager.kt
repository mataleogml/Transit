package com.example.transit.manager

import com.example.transit.TransitPlugin
import com.example.transit.model.PaymentPeriod
import com.example.transit.model.StaffMember
import com.example.transit.model.Transaction
import com.example.transit.model.TransactionType
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class StaffManager(private val plugin: TransitPlugin) {
    private val staffMembers = ConcurrentHashMap<String, MutableSet<StaffMember>>()
    private val staffFile = File(plugin.dataFolder, "staff.yml")
    private val config = YamlConfiguration.loadConfiguration(staffFile)

    init {
        loadStaffData()
        startPaymentScheduler()
    }

    fun addStaffMember(
        playerId: UUID,
        systemId: String,
        salary: Double,
        period: PaymentPeriod
    ): Boolean {
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
        return staffMembers[systemId]?.removeIf { it.playerId == playerId } == true
    }

    fun isStaff(playerId: UUID, systemId: String): Boolean {
        return staffMembers[systemId]?.any { it.playerId == playerId } == true
    }

    private fun startPaymentScheduler() {
        object : BukkitRunnable() {
            override fun run() {
                processPayments()
            }
        }.runTaskTimer(plugin, 72000L, 72000L) // Check every hour
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
        val systemBalance = plugin.transactionManager.getSystemBalance(systemId)
        if (systemBalance < staffMember.salary) {
            plugin.logger.warning("Insufficient system balance for staff payment: $systemId")
            return
        }

        val player = plugin.server.getOfflinePlayer(staffMember.playerId)
        plugin.economy.depositPlayer(player, staffMember.salary)

        // Log transaction
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

        // Update last paid time
        updateLastPaidTime(staffMember)

        // Notify if online
        plugin.server.getPlayer(staffMember.playerId)?.let { onlinePlayer ->
            onlinePlayer.sendMessage(
                "§aReceived staff payment: $${staffMember.salary} for system $systemId"
            )
        }
    }

    private fun updateLastPaidTime(staffMember: StaffMember) {
        staffMembers[staffMember.systemId]?.let { members ->
            members.remove(staffMember)
            members.add(staffMember.copy(lastPaid = LocalDateTime.now()))
        }
        saveStaffData()
    }

    private fun loadStaffData() {
        if (config.contains("staff")) {
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
                                paymentPeriod = PaymentPeriod.valueOf(it.getString("period", "MONTHLY")),
                                lastPaid = LocalDateTime.parse(it.getString("lastPaid"))
                            )
                        )
                    }
                }
                staffMembers[systemId] = members
            }
        }
    }

    private fun saveStaffMember(staffMember: StaffMember) {
        val path = "staff.${staffMember.systemId}.${staffMember.playerId}"
        config.set("$path.salary", staffMember.salary)
        config.set("$path.period", staffMember.paymentPeriod.name)
        config.set("$path.lastPaid", staffMember.lastPaid.toString())
        saveConfig()
    }

    fun saveAll() {
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
}