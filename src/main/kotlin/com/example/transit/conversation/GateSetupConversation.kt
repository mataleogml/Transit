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

    private inner class SystemPrompt : ValidatingPrompt() {
        override fun getPromptText(context: ConversationContext): String {
            val systems = plugin.configManager.getSystems().map { it.id }
            return if (lastSystem != null) {
                "§6Enter system ID or press Enter to use last system (${lastSystem}):\n" +
                "§7Available systems: ${systems.joinToString(", ")}"
            } else {
                "§6Enter system ID:\n§7Available systems: ${systems.joinToString(", ")}"
            }
        }

        override fun isInputValid(context: ConversationContext, input: String): Boolean {
            val systemId = if (input.isEmpty()) lastSystem else input
            return systemId?.let { plugin.configManager.getTransitSystem(it) != null } ?: false
        }

        override fun acceptValidatedInput(context: ConversationContext, input: String): Prompt {
            val systemId = if (input.isEmpty()) lastSystem!! else input
            context.setSessionData("system", systemId)
            lastSystem = systemId
            return StationPrompt()
        }

        override fun getFailedValidationText(context: ConversationContext, invalidInput: String): String {
            return "§cInvalid system ID! Please try again."
        }
    }

    private inner class StationPrompt : ValidatingPrompt() {
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

        override fun isInputValid(context: ConversationContext, input: String): Boolean {
            val systemId = context.getSessionData("system") as String
            val stationId = if (input.isEmpty()) lastStation else input
            return stationId?.let { 
                plugin.stationManager.getStation("${systemId}_${it.toLowerCase()}") != null 
            } ?: false
        }

        override fun acceptValidatedInput(context: ConversationContext, input: String): Prompt? {
            val systemId = context.getSessionData("system") as String
            val stationId = if (input.isEmpty()) lastStation!! else input
            
            // Create the gate
            createGate(
                systemId,
                "${systemId}_${stationId.toLowerCase()}"
            )
            
            context.forWhom.sendRawMessage("§aFare gate created successfully!")
            lastStation = stationId
            return null
        }

        override fun getFailedValidationText(context: ConversationContext, invalidInput: String): String {
            return "§cInvalid station name! Please try again."
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
        
        // Update the sign text
        val sign = location.block.state as? org.bukkit.block.Sign
        sign?.let {
            val station = plugin.stationManager.getStation(stationId)
            it.setLine(0, "§1[Fare]")
            it.setLine(1, station?.name ?: "Unknown Station")
            it.setLine(2, "§aActive")
            it.setLine(3, station?.zone ?: "")
            it.update()
        }
    }
}