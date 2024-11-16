package com.example.transit.listener

import com.example.transit.TransitPlugin
import com.example.transit.model.Gate
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerInteractEvent

class GateListener(private val plugin: TransitPlugin) : Listener {
    
    @EventHandler
    fun onSignCreate(event: SignChangeEvent) {
        if (event.getLine(0)?.equals("[Fare]", true) != true) return
        
        if (!event.player.hasPermission("transit.admin")) {
            event.player.sendMessage("§cYou don't have permission to create fare gates!")
            event.isCancelled = true
            return
        }

        // Format the sign
        event.setLine(0, "§1[Fare]")
        
        // Start gate creation conversation
        plugin.gateManager.startGateCreation(event.player, event.block.location)
    }

    @EventHandler
    fun onSignBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.type != Material.OAK_WALL_SIGN && 
            block.type != Material.OAK_SIGN) return
            
        val sign = block.state as? Sign ?: return
        if (sign.getLine(0) != "§1[Fare]") return
        
        if (!event.player.hasPermission("transit.admin")) {
            event.player.sendMessage("§cYou don't have permission to remove fare gates!")
            event.isCancelled = true
            return
        }
        
        // Remove the gate
        plugin.gateManager.removeGate(block.location)
        event.player.sendMessage("§aFare gate removed!")
    }

    @EventHandler
    fun onGateInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        if (block.type != Material.OAK_WALL_SIGN && 
            block.type != Material.OAK_SIGN) return
        
        val sign = block.state as? Sign ?: return
        if (sign.getLine(0) != "§1[Fare]") return

        event.isCancelled = true
        
        // Handle gate interaction
        val gate = plugin.gateManager.getGateAtLocation(block.location)
        if (gate != null) {
            handleGateInteraction(event.player, gate)
        }
    }
    
    private fun handleGateInteraction(player: org.bukkit.entity.Player, gate: Gate) {
        // Check if player has an active journey
        val activeJourney = plugin.fareManager.getActiveJourney(player.uniqueId)
        
        if (activeJourney == null) {
            // Start new journey
            if (plugin.fareManager.handleTapIn(player, gate.systemId, gate.stationId)) {
                playGateEffect(gate, true)
            }
        } else {
            // End journey
            if (plugin.fareManager.handleTapOut(player, gate.systemId, gate.stationId)) {
                playGateEffect(gate, true)
            } else {
                playGateEffect(gate, false)
            }
        }
    }
    
    private fun playGateEffect(gate: Gate, success: Boolean) {
        val location = gate.location
        if (plugin.config.getBoolean("gates.soundEffects", true)) {
            location.world?.playSound(
                location,
                if (success) org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING else org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS,
                1f,
                if (success) 2f else 0.5f
            )
        }
        
        if (plugin.config.getBoolean("gates.particleEffects", true)) {
            location.world?.spawnParticle(
                if (success) org.bukkit.Particle.VILLAGER_HAPPY else org.bukkit.Particle.VILLAGER_ANGRY,
                location.add(0.5, 1.0, 0.5),
                5,
                0.2,
                0.2,
                0.2,
                0.0
            )
        }
    }
}