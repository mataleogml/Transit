package com.example.transit.database

import com.example.transit.TransitPlugin
import com.example.transit.model.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.SQLException
import java.time.LocalDateTime

class DatabaseManager(private val plugin: TransitPlugin) {
    private var dataSource: HikariDataSource? = null
    
    init {
        if (plugin.config.getString("database.type")?.uppercase() == "MYSQL") {
            setupHikari()
            createTables()
        }
    }

    private fun setupHikari() {
        val config = HikariConfig().apply {
            jdbcUrl = buildJdbcUrl()
            username = plugin.config.getString("database.mysql.username")
            password = plugin.config.getString("database.mysql.password")
            maximumPoolSize = 10
            minimumIdle = 2
            idleTimeout = 300000 // 5 minutes
            connectionTimeout = 10000 // 10 seconds
            validationTimeout = 5000 // 5 seconds
        }

        try {
            dataSource = HikariDataSource(config)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to initialize database connection: ${e.message}")
        }
    }

    private fun buildJdbcUrl(): String {
        val host = plugin.config.getString("database.mysql.host", "localhost")
        val port = plugin.config.getInt("database.mysql.port", 3306)
        val database = plugin.config.getString("database.mysql.database", "transit")
        val prefix = plugin.config.getString("database.mysql.prefix", "transit_")
        
        return "jdbc:mysql://$host:$port/$database?useSSL=false&serverTimezone=UTC"
    }

    private fun createTables() {
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS ${getTableName("transactions")} (
                id VARCHAR(36) PRIMARY KEY,
                player_id VARCHAR(36) NOT NULL,
                system_id VARCHAR(50) NOT NULL,
                from_station VARCHAR(50) NOT NULL,
                to_station VARCHAR(50),
                amount DECIMAL(10,2) NOT NULL,
                type VARCHAR(20) NOT NULL,
                timestamp DATETIME NOT NULL,
                INDEX idx_player (player_id),
                INDEX idx_system (system_id),
                INDEX idx_timestamp (timestamp)
            )
        """)

        executeUpdate("""
            CREATE TABLE IF NOT EXISTS ${getTableName("statistics")} (
                id VARCHAR(36) PRIMARY KEY,
                type VARCHAR(20) NOT NULL,
                reference_id VARCHAR(50) NOT NULL,
                total_revenue DECIMAL(10,2) NOT NULL DEFAULT 0,
                total_transactions INT NOT NULL DEFAULT 0,
                last_updated DATETIME NOT NULL,
                INDEX idx_type_ref (type, reference_id)
            )
        """)
    }

    fun saveTransaction(transaction: Transaction) {
        executeUpdate("""
            INSERT INTO ${getTableName("transactions")} 
            (id, player_id, system_id, from_station, to_station, amount, type, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """) { stmt ->
            stmt.setString(1, transaction.id)
            stmt.setString(2, transaction.playerId.toString())
            stmt.setString(3, transaction.systemId)
            stmt.setString(4, transaction.fromStation)
            stmt.setString(5, transaction.toStation)
            stmt.setDouble(6, transaction.amount)
            stmt.setString(7, transaction.type.name)
            stmt.setString(8, transaction.timestamp.toString())
        }
    }

    fun getTransactions(
        systemId: String? = null,
        playerId: String? = null,
        startTime: LocalDateTime? = null,
        endTime: LocalDateTime? = null
    ): List<Transaction> {
        val conditions = mutableListOf<String>()
        val params = mutableListOf<Pair<Int, Any>>()
        var paramIndex = 1

        systemId?.let {
            conditions.add("system_id = ?")
            params.add(paramIndex++ to it)
        }
        playerId?.let {
            conditions.add("player_id = ?")
            params.add(paramIndex++ to it)
        }
        startTime?.let {
            conditions.add("timestamp >= ?")
            params.add(paramIndex++ to it.toString())
        }
        endTime?.let {
            conditions.add("timestamp <= ?")
            params.add(paramIndex++ to it.toString())
        }

        val whereClause = if (conditions.isNotEmpty()) 
            "WHERE ${conditions.joinToString(" AND ")}" else ""

        return executeQuery("""
            SELECT * FROM ${getTableName("transactions")} 
            $whereClause 
            ORDER BY timestamp DESC
        """) { stmt ->
            params.forEach { (index, value) ->
                stmt.setObject(index, value)
            }
        } { rs ->
            Transaction(
                id = rs.getString("id"),
                playerId = java.util.UUID.fromString(rs.getString("player_id")),
                systemId = rs.getString("system_id"),
                fromStation = rs.getString("from_station"),
                toStation = rs.getString("to_station"),
                amount = rs.getDouble("amount"),
                type = TransactionType.valueOf(rs.getString("type")),
                timestamp = LocalDateTime.parse(rs.getString("timestamp"))
            )
        }
    }

    private fun getTableName(base: String): String {
        val prefix = plugin.config.getString("database.mysql.prefix", "transit_")
        return "$prefix$base"
    }

    private fun executeUpdate(sql: String, prepare: (java.sql.PreparedStatement) -> Unit = {}) {
        try {
            dataSource?.connection?.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    prepare(stmt)
                    stmt.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to execute update: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun <T> executeQuery(
        sql: String,
        prepare: (java.sql.PreparedStatement) -> Unit = {},
        map: (java.sql.ResultSet) -> T
    ): List<T> {
        val results = mutableListOf<T>()
        try {
            dataSource?.connection?.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    prepare(stmt)
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        results.add(map(rs))
                    }
                }
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to execute query: ${e.message}")
            e.printStackTrace()
        }
        return results
    }
}