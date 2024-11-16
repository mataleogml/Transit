package com.example.transit.event

import com.example.transit.model.Station
import com.example.transit.model.Transaction
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.Cancellable

class TransitGateEntryEvent(
    val player: Player,
    val station: Station,
    val fareAmount: Double
) : Event(), Cancellable {
    private var cancelled = false
    
    override fun isCancelled() = cancelled
    override fun setCancelled(cancel: Boolean) { cancelled = cancel }
    
    companion object {
        private val handlers = HandlerList()
        @JvmStatic
        fun getHandlerList() = handlers
    }
    override fun getHandlers() = getHandlerList()
}

class TransitGateExitEvent(
    val player: Player,
    val fromStation: Station,
    val toStation: Station,
    val fareAmount: Double,
    val transaction: Transaction
) : Event(), Cancellable {
    private var cancelled = false
    
    override fun isCancelled() = cancelled
    override fun setCancelled(cancel: Boolean) { cancelled = cancel }
    
    companion object {
        private val handlers = HandlerList()
        @JvmStatic
        fun getHandlerList() = handlers
    }
    override fun getHandlers() = getHandlerList()
}

class TransitStaffPaymentEvent(
    val player: Player,
    val systemId: String,
    val amount: Double
) : Event(), Cancellable {
    private var cancelled = false
    
    override fun isCancelled() = cancelled
    override fun setCancelled(cancel: Boolean) { cancelled = cancel }
    
    companion object {
        private val handlers = HandlerList()
        @JvmStatic
        fun getHandlerList() = handlers
    }
    override fun getHandlers() = getHandlerList()
}