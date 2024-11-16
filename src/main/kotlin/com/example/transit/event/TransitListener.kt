package com.example.transit.event

import com.example.transit.TransitPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.PluginDisableEvent
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.event.server.ServerLoadEvent

class TransitListener(private val plugin: TransitPlugin) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        // Check for pending staff payments
        if (plugin.staffManager.isStaffAnywhere(event.player.uniqueId)) {
            plugin.staffManager.checkPendingPayments(event.player)
        }

        // Check for incomplete journeys
        plugin.fareManager.checkIncompleteJourneys(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // Handle any active journeys
        plugin.fareManager.handlePlayerQuit(event.player)
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
}