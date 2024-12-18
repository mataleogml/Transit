package com.example.transit.manager

import com.example.transit.TransitPlugin
import com.example.transit.model.*
import com.example.transit.fare.ZoneFareCalculator
import net.milkbowl.vault.economy.Economy
import org.bukkit.entity.Player
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class FareManager(
    private val plugin: TransitPlugin,
    private val economy: Economy
) {
    private val activeTaps = ConcurrentHashMap<UUID, TapData>()
    private val activeJourneys = ConcurrentHashMap<UUID, JourneyData>()
    private var zoneFareCalculator: ZoneFareCalculator = ZoneFareCalculator(
        plugin.config.getConfigurationSection("systems") 
            ?: throw IllegalStateException("Missing systems configuration")
    )
    
    fun getActiveJourney(playerId: UUID): JourneyData? = activeJourneys[playerId]

    fun clearActiveJourney(playerId: UUID) {
        activeJourneys.remove(playerId)
    }

    fun reload() {
        activeTaps.clear()
        activeJourneys.clear()
        // Reinitialize the zoneFareCalculator with fresh config
        zoneFareCalculator = ZoneFareCalculator(plugin.config.getConfigurationSection("systems") 
            ?: throw IllegalStateException("Missing systems configuration"))
    }

    fun handleTapIn(player: Player, systemId: String, stationId: String): Boolean {
        val station = plugin.stationManager.getStation(stationId) ?: return false
        val system = plugin.configManager.getTransitSystem(systemId) ?: return false

        when (system.fareType) {
            FareType.FLAT -> handleFlatFare(player, system, station)
            else -> {
                activeJourneys[player.uniqueId] = JourneyData(
                    systemId = systemId,
                    startStation = stationId,
                    startLocation = player.location,
                    startTime = LocalDateTime.now()
                )
                activeTaps[player.uniqueId] = TapData(
                    systemId = systemId,
                    stationId = stationId,
                    timestamp = LocalDateTime.now()
                )
                player.sendMessage("§aTapped in at ${station.name}")
            }
        }
        return true
    }

    fun handleTapOut(player: Player, systemId: String, stationId: String): Boolean {
        val tapData = activeTaps[player.uniqueId] ?: return false
        val system = plugin.configManager.getTransitSystem(systemId) ?: return false
        
        val fare = calculateFare(tapData, stationId, system)
        if (chargeFare(player, fare)) {
            activeTaps.remove(player.uniqueId)
            activeJourneys.remove(player.uniqueId)
            
            // Log transaction
            plugin.transactionManager.logTransaction(
                Transaction(
                    playerId = player.uniqueId,
                    systemId = systemId,
                    fromStation = tapData.stationId,
                    toStation = stationId,
                    amount = fare,
                    type = TransactionType.EXIT
                )
            )
            
            player.sendMessage("§aFare charged: $${fare}")
            return true
        }
        return false
    }

    fun chargeMaximumFare(player: Player, system: TransitSystem) {
        val fare = system.maxFare
        if (chargeFare(player, fare)) {
            plugin.transactionManager.logTransaction(
                Transaction(
                    playerId = player.uniqueId,
                    systemId = system.id,
                    fromStation = "MAX_FARE_CHARGE",
                    toStation = null,
                    amount = fare,
                    type = TransactionType.FLAT_RATE
                )
            )
            player.sendMessage("§cMaximum fare charged: $${fare}")
        }
    }

    private fun handleFlatFare(player: Player, system: TransitSystem, station: Station) {
        val fare = system.fareData["fare"] as Double
        if (chargeFare(player, fare)) {
            plugin.transactionManager.logTransaction(
                Transaction(
                    playerId = player.uniqueId,
                    systemId = system.id,
                    fromStation = station.id,
                    toStation = null,
                    amount = fare,
                    type = TransactionType.FLAT_RATE
                )
            )
            player.sendMessage("§aFare charged: $${fare}")
        }
    }

    private fun calculateFare(tapData: TapData, exitStationId: String, system: TransitSystem): Double {
        val entryStation = plugin.stationManager.getStation(tapData.stationId)!!
        val exitStation = plugin.stationManager.getStation(exitStationId)!!

        return when (system.fareType) {
            FareType.ZONE -> zoneFareCalculator.calculateFare(entryStation.zone, exitStation.zone)
            FareType.DISTANCE -> {
                val distance = entryStation.location.distance(exitStation.location)
                val baseRate = system.fareData["baseRate"] as Double
                val perBlock = system.fareData["perBlock"] as Double
                minOf(baseRate + (distance * perBlock), system.maxFare)
            }
            FareType.FLAT -> system.fareData["fare"] as Double
        }.coerceAtMost(system.maxFare)
    }

    private fun chargeFare(player: Player, amount: Double): Boolean {
        return if (economy.has(player, amount)) {
            economy.withdrawPlayer(player, amount)
            true
        } else {
            player.sendMessage("§cInsufficient funds! Need: $$amount")
            false
        }
    }

    fun refundTransaction(transactionId: String): Boolean {
        val transaction = plugin.transactionManager.getTransaction(transactionId) ?: return false
        val player = plugin.server.getOfflinePlayer(transaction.playerId)
        
        economy.depositPlayer(player, transaction.amount)
        plugin.transactionManager.logTransaction(
            transaction.copy(
                id = UUID.randomUUID().toString(),
                type = TransactionType.REFUND,
                timestamp = LocalDateTime.now()
            )
        )
        return true
    }

    data class TapData(
        val systemId: String,
        val stationId: String,
        val timestamp: LocalDateTime
    )
}