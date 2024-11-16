package com.example.transit

import com.example.transit.manager.*
import org.bukkit.plugin.java.JavaPlugin
import net.milkbowl.vault.economy.Economy

class TransitPlugin : JavaPlugin() {
    // Using lazy initialization for managers
    val configManager by lazy { ConfigManager(this) }
    val stationManager by lazy { StationManager(this) }
    val routeManager by lazy { RouteManager(this) }
    val fareManager by lazy { FareManager(this, economy) }
    val gateManager by lazy { GateManager(this) }
    val staffManager by lazy { StaffManager(this) }
    val statisticsManager by lazy { StatisticsManager(this) }
    val transactionManager by lazy { TransactionManager(this) }
    
    lateinit var economy: Economy
        private set

    override fun onEnable() {
        // Save default config
        saveDefaultConfig()
        
        // Setup Vault
        if (!setupEconomy()) {
            logger.severe("Disabled due to no Vault dependency found!")
            server.pluginManager.disablePlugin(this)
            return
        }

        // Register commands
        registerCommands()

        // Register events
        registerEvents()

        logger.info("Transit plugin enabled!")
    }

    private fun setupEconomy(): Boolean {
        if (server.pluginManager.getPlugin("Vault") == null) {
            return false
        }
        
        val rsp = server.servicesManager.getRegistration(Economy::class.java)
        if (rsp != null) {
            economy = rsp.provider
            return true
        }
        return false
    }

    private fun registerCommands() {
        getCommand("transit")?.setExecutor(TransitCommand(this))
        getCommand("station")?.setExecutor(StationCommand(this))
        getCommand("route")?.setExecutor(RouteCommand(this))
        getCommand("fare")?.setExecutor(FareCommand(this))
    }

    private fun registerEvents() {
        server.pluginManager.apply {
            registerEvents(GateListener(this@TransitPlugin), this@TransitPlugin)
            registerEvents(TransitListener(this@TransitPlugin), this@TransitPlugin)
        }
    }

    override fun onDisable() {
        // Save all data
        stationManager.saveAll()
        routeManager.saveAll()
        staffManager.saveAll()
        statisticsManager.saveAll()
        transactionManager.saveAll()

        logger.info("Transit plugin disabled!")
    }
}