package com.example.transit.manager

import com.example.transit.TransitPlugin
import com.example.transit.model.Transaction
import com.example.transit.model.TransactionType
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TransactionManager(private val plugin: TransitPlugin) {
    private val transactions = ConcurrentHashMap<String, Transaction>()
    private val systemBalances = ConcurrentHashMap<String, Double>()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    
    init {
        loadTransactions()
        loadSystemBalances()
    }
    
    fun reload() {
        transactions.clear()
        systemBalances.clear()
        loadTransactions()
        loadSystemBalances()
    }

    fun logTransaction(transaction: Transaction) {
        transactions[transaction.id] = transaction
        updateSystemBalance(transaction)
        saveTransaction(transaction)
        updateStatistics(transaction)
    }

    fun getTransaction(id: String): Transaction? = transactions[id]

    fun getSystemBalance(systemId: String): Double = systemBalances.getOrDefault(systemId, 0.0)

    fun getTransactionsBySystem(systemId: String): List<Transaction> =
        transactions.values.filter { it.systemId == systemId }

    fun getTransactionsByPlayer(playerId: UUID): List<Transaction> =
        transactions.values.filter { it.playerId == playerId }

    private fun updateSystemBalance(transaction: Transaction) {
        when (transaction.type) {
            TransactionType.ENTRY, TransactionType.EXIT, TransactionType.FLAT_RATE -> {
                systemBalances.merge(transaction.systemId, transaction.amount, Double::plus)
            }
            TransactionType.REFUND, TransactionType.STAFF_PAYMENT -> {
                systemBalances.merge(transaction.systemId, -transaction.amount, Double::plus)
            }
            else -> {} // Handle other transaction types
        }
    }

    private fun saveTransaction(transaction: Transaction) {
        val month = transaction.timestamp.format(dateFormatter)
        val file = getTransactionFile(month)
        val config = YamlConfiguration.loadConfiguration(file)

        config.set("${transaction.id}.playerId", transaction.playerId.toString())
        config.set("${transaction.id}.systemId", transaction.systemId)
        config.set("${transaction.id}.fromStation", transaction.fromStation)
        config.set("${transaction.id}.toStation", transaction.toStation)
        config.set("${transaction.id}.amount", transaction.amount)
        config.set("${transaction.id}.type", transaction.type.name)
        config.set("${transaction.id}.timestamp", transaction.timestamp.toString())

        try {
            config.save(file)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save transaction ${transaction.id}: ${e.message}")
        }
    }

    private fun getTransactionFile(month: String): File {
        val folder = File(plugin.dataFolder, "transactions")
        if (!folder.exists()) folder.mkdirs()
        return File(folder, "$month.yml")
    }

    private fun loadTransactions() {
        val folder = File(plugin.dataFolder, "transactions")
        if (!folder.exists()) return

        folder.listFiles { file -> file.name.endsWith(".yml") }?.forEach { file ->
            val config = YamlConfiguration.loadConfiguration(file)
            
            for (id in config.getKeys(false)) {
                val section = config.getConfigurationSection(id) ?: continue
                try {
                    val transaction = Transaction(
                        id = id,
                        playerId = UUID.fromString(section.getString("playerId") ?: continue),
                        systemId = section.getString("systemId") ?: continue,
                        fromStation = section.getString("fromStation") ?: "",  // Default to empty string if null
                        toStation = section.getString("toStation"),  // Can be null
                        amount = section.getDouble("amount"),
                        type = TransactionType.valueOf(section.getString("type") ?: TransactionType.ENTRY.name),
                        timestamp = LocalDateTime.parse(section.getString("timestamp") ?: continue)
                    )
                    transactions[id] = transaction
                    updateSystemBalance(transaction)
                } catch (e: Exception) {
                    plugin.logger.severe("Failed to load transaction $id: ${e.message}")
                }
            }
        }
    }

    private fun loadSystemBalances() {
        val file = File(plugin.dataFolder, "balances.yml")
        if (!file.exists()) return
        
        val config = YamlConfiguration.loadConfiguration(file)
        val balancesSection = config.getConfigurationSection("balances") ?: return
        
        for (systemId in balancesSection.getKeys(false)) {
            systemBalances[systemId] = balancesSection.getDouble(systemId)
        }
    }

    private fun updateStatistics(transaction: Transaction) {
        plugin.statisticsManager.updateStatistics(transaction)
    }

    fun saveAll() {
        transactions.values.forEach { saveTransaction(it) }
        saveSystemBalances()
    }

    private fun saveSystemBalances() {
        val file = File(plugin.dataFolder, "balances.yml")
        val config = YamlConfiguration.loadConfiguration(file)

        systemBalances.forEach { (systemId, balance) ->
            config.set("balances.$systemId", balance)
        }

        try {
            config.save(file)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save system balances: ${e.message}")
        }
    }
}