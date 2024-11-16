package com.example.transit.manager

import com.example.transit.TransitPlugin
import com.example.transit.model.Gate
import com.example.transit.conversation.GateSetupConversation
import com.example.transit.util.toLocation
import com.example.transit.util.toStorableString
import org.bukkit.Location
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class GateManager(private val plugin: TransitPlugin) {
    private val gates = mutableMapOf<String, Gate>()
    private val gatesFile = File(plugin.dataFolder, "gates.yml")
    private val config = YamlConfiguration.loadConfiguration(gatesFile)

    init {
        loadGates()
    }

    fun startGateCreation(player: Player, location: Location) {
        GateSetupConversation(plugin, location, player).start()
    }

    fun registerGate(gate: Gate) {
        gates[gate.id] = gate
        saveGate(gate)
        updateGateSign(gate)
    }

    fun removeGate(location: Location) {
        val gateId = getGateIdFromLocation(location)
        gates.remove(gateId)
        config.set(gateId, null)
        saveConfig()
    }

    fun getGateAtLocation(location: Location): Gate? {
        val gateId = getGateIdFromLocation(location)
        return gates[gateId]
    }

    fun handleGateInteraction(player: Player, sign: Sign) {
        val gate = getGateAtLocation(sign.location) ?: return
        if (!gate.enabled) {
            player.sendMessage("§cThis gate is currently disabled!")
            return
        }

        val station = plugin.stationManager.getStation(gate.stationId) ?: return
        if (!station.status.isActive()) {
            player.sendMessage("§cThis station is currently closed!")
            return
        }

        val activeJourney = plugin.fareManager.getActiveJourney(player.uniqueId)
        if (activeJourney == null) {
            // Handle entry
            if (plugin.fareManager.handleTapIn(player, gate.systemId, gate.stationId)) {
                updateGateSign(gate)
                playGateEffect(gate, true)
            }
        } else {
            // Handle exit
            if (plugin.fareManager.handleTapOut(player, gate.systemId, gate.stationId)) {
                updateGateSign(gate)
                playGateEffect(gate, true)
            } else {
                playGateEffect(gate, false)
            }
        }
    }

    private fun loadGates() {
        for (gateId in config.getKeys(false)) {
            val section = config.getConfigurationSection(gateId) ?: continue
            try {
                gates[gateId] = Gate(
                    id = gateId,
                    location = section.getString("location")?.toLocation() ?: continue,
                    systemId = section.getString("systemId") ?: "",
                    stationId = section.getString("stationId") ?: "",
                    enabled = section.getBoolean("enabled", true)
                )
            } catch (e: Exception) {
                plugin.logger.severe("Failed to load gate $gateId: ${e.message}")
            }
        }
    }

    private fun saveGate(gate: Gate) {
        config.set("${gate.id}.location", gate.location.toStorableString())
        config.set("${gate.id}.systemId", gate.systemId)
        config.set("${gate.id}.stationId", gate.stationId)
        config.set("${gate.id}.enabled", gate.enabled)
        saveConfig()
    }

    private fun updateGateSign(gate: Gate) {
        val sign = gate.location.block.state as? Sign ?: return
        val station = plugin.stationManager.getStation(gate.stationId)

        sign.setLine(0, "§1[Fare]")
        sign.setLine(1, station?.name ?: "Unknown Station")
        sign.setLine(2, if (gate.enabled) "§aActive" else "§cDisabled")
        sign.setLine(3, station?.status?.name ?: "UNKNOWN")
        sign.update()
    }

    private fun getGateIdFromLocation(location: Location): String {
        return "${location.blockX}_${location.blockY}_${location.blockZ}"
    }

    private fun playGateEffect(gate: Gate, success: Boolean) {
        if (!plugin.config.getBoolean("gates.enableEffects", true)) return
        
        val location = gate.location
        // Play sound effect
        location.world?.playSound(
            location,
            if (success) org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING 
            else org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS,
            1f,
            if (success) 2f else 0.5f
        )

        // Show particles
        location.world?.spawnParticle(
            if (success) org.bukkit.Particle.VILLAGER_HAPPY 
            else org.bukkit.Particle.VILLAGER_ANGRY,
            location.add(0.5, 1.0, 0.5),
            5,
            0.2,
            0.2,
            0.2,
            0.0
        )
    }

    fun saveAll() {
        gates.values.forEach { saveGate(it) }
    }

    private fun saveConfig() {
        try {
            config.save(gatesFile)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save gates: ${e.message}")
        }
    }
}