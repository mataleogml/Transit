package com.example.transit.conversation

import com.example.transit.TransitPlugin
import com.example.transit.model.Gate
import org.bukkit.Location
import org.bukkit.conversations.*
import org.bukkit.entity.Player

class GateSetupConversation(
    private val plugin: TransitPlugin,
    private val location: Location,
    private val player: Player
) {
    private var lastSystem: String? = null
    private var lastStation: String? = null

    fun start() {
        val factory = ConversationFactory(plugin)
            .withModality(true)
            .withFirstPrompt(SystemPrompt())
            .withEscapeSequence("cancel")
            .withTimeout(60)
            .withLocalEcho(false)
            .addConversationAbandonedListener { event ->
                if (!event.gracefulExit()) {
                    player.sendMessage("§cGate setup cancelled!")
                }
            }

        factory.buildConversation(player).begin()
    }

    private inner class SystemPrompt : StringPrompt() {
        override fun getPromptText(context: ConversationContext): String {
            val systems = plugin.configManager.getSystems().map { it.id }
            return if (lastSystem != null) {
                "§6Enter system ID or press Enter to use last system (${lastSystem}):\n" +
                "§7Available systems: ${systems.joinToString(", ")}"
            } else {
                "§6Enter system ID:\n§7Available systems: ${systems.joinToString(", ")}"
            }
        }

        override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
            val systemId = if (input.isNullOrBlank()) lastSystem else input
            
            if (systemId == null || !plugin.configManager.getTransitSystem(systemId)?.let { true } ?: false) {
                context.forWhom.sendRawMessage("§cInvalid system ID!")
                return this
            }

            context.setSessionData("system", systemId)
            lastSystem = systemId
            return StationPrompt()
        }
    }

    private inner class StationPrompt : StringPrompt() {
        override fun getPromptText(context: ConversationContext): String {
            val systemId = context.getSessionData("system") as String
            val stations = plugin.stationManager.getSystemStations(systemId).map { it.name }
            return if (lastStation != null) {
                "§6Enter station name or press Enter to use last station (${lastStation}):\n" +
                "§7Available stations: ${stations.joinToString(", ")}"
            } else {
                "§6Enter station name:\n§7Available stations: ${stations.joinToString(", ")}"
            }
        }

        override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
            val systemId = context.getSessionData("system") as String
            val stationId = if (input.isNullOrBlank()) lastStation else input
            
            if (stationId == null || !plugin.stationManager.getStation("${systemId}_${stationId.toLowerCase()}")?.let { true } ?: false) {
                context.forWhom.sendRawMessage("§cInvalid station name!")
                return this
            }

            context.setSessionData("station", stationId)
            lastStation = stationId
            
            // Create the gate
            createGate(
                systemId,
                "${systemId}_${stationId.toLowerCase()}"
            )
            
            context.forWhom.sendRawMessage("§aFare gate created successfully!")
            return null
        }
    }

    private fun createGate(systemId: String, stationId: String) {
        val gate = Gate(
            id = "${systemId}_${location.blockX}_${location.blockY}_${location.blockZ}",
            location = location,
            systemId = systemId,
            stationId = stationId
        )
        plugin.gateManager.registerGate(gate)
    }
}