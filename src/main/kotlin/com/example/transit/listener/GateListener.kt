package com.example.transit.listener

import com.example.transit.TransitPlugin
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
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

        // Start gate creation conversation
        plugin.gateManager.startGateCreation(event.player, event.block.location)
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
        plugin.gateManager.handleGateInteraction(event.player, sign)
    }
}