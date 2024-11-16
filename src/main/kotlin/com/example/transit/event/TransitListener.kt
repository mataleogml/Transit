package com.example.transit.event

import com.example.transit.TransitPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.ServerLoadEvent
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TransitListener(private val plugin: TransitPlugin) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        // Check for pending staff payments
        if (plugin.staffManager.isStaffAnywhere(event.player.uniqueId)) {
            plugin.staffManager.checkPendingPayments(event.player)
        }

        // Check for incomplete journeys
        checkIncompleteJourneys(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        handlePlayerQuit(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onServerLoad(event: ServerLoadEvent) {
        if (event.type == ServerLoadEvent.LoadType.RELOAD) {
            plugin.logger.warning("Plugin reloaded - Some features may not work correctly until a full restart")
        }
    }

    @EventHandler
    fun onPluginDisable(event: PluginDisableEvent) {
        if (event.plugin.name == "Vault") {
            plugin.logger.severe("Vault plugin disabled - Transit plugin may not function correctly!")
        }
    }

    private fun checkIncompleteJourneys(player: org.bukkit.entity.Player) {
        val activeJourney = plugin.fareManager.getActiveJourney(player.uniqueId)
        if (activeJourney != null) {
            val timeSinceStart = ChronoUnit.MINUTES.between(
                activeJourney.startTime,
                LocalDateTime.now()
            )
            
            if (timeSinceStart > plugin.config.getLong("settings.maxTapDuration", 120)) {
                // Journey exceeded maximum duration, apply maximum fare
                val system = plugin.configManager.getTransitSystem(activeJourney.systemId)
                if (system != null) {
                    plugin.fareManager.chargeMaximumFare(player, system)
                }
                plugin.fareManager.clearActiveJourney(player.uniqueId)
            }
        }
    }

    private fun handlePlayerQuit(player: org.bukkit.entity.Player) {
        val activeJourney = plugin.fareManager.getActiveJourney(player.uniqueId)
        if (activeJourney != null) {
            val system = plugin.configManager.getTransitSystem(activeJourney.systemId)
            if (system != null) {
                // Player quit with active journey, charge maximum fare
                plugin.fareManager.chargeMaximumFare(player, system)
            }
            plugin.fareManager.clearActiveJourney(player.uniqueId)
        }
    }
}