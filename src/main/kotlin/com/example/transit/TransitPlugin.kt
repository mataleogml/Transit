package com.example.transit

import com.example.transit.api.TransitAPI
import com.example.transit.command.*
import com.example.transit.event.TransitListener
import com.example.transit.listener.GateListener
import com.example.transit.manager.*
import com.example.transit.statistics.StatisticsManager
import org.bukkit.plugin.java.JavaPlugin
import net.milkbowl.vault.economy.Economy
import org.bukkit.plugin.ServicePriority

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
    val api by lazy { TransitAPI(this) }
    
    lateinit var economy: Economy
        private set

    override fun onEnable() {
        try {
            // Save default config and messages
            saveDefaultConfig()
            try {
                saveResource("messages.yml", false)
            } catch (e: IllegalArgumentException) {
                // File already exists, ignore
            }
            reloadConfig()
            
            // Setup Vault
            if (!setupEconomy()) {
                logger.severe("Disabled due to no Vault dependency found!")
                isEnabled = false
                return
            }

            // Register API service
            server.servicesManager.register(
                TransitAPI::class.java,
                api,
                this,
                ServicePriority.Normal
            )

            // Register commands
            registerCommands()

            // Register events
            registerEvents()

            // Start auto-save task
            startAutoSave()

            logger.info("Transit plugin enabled successfully!")
        } catch (e: Exception) {
            logger.severe("Failed to enable Transit plugin: ${e.message}")
            e.printStackTrace()
            isEnabled = false
        }
    }

    private fun registerCommands() {
        getCommand("transit")?.let { cmd ->
            val executor = TransitCommand(this)
            cmd.setExecutor(executor)
            cmd.tabCompleter = executor
        }
        getCommand("station")?.let { cmd ->
            val executor = StationCommand(this)
            cmd.setExecutor(executor)
            cmd.tabCompleter = executor
        }
        getCommand("route")?.let { cmd ->
            val executor = RouteCommand(this)
            cmd.setExecutor(executor)
            cmd.tabCompleter = executor
        }
        getCommand("fare")?.let { cmd ->
            val executor = FareCommand(this)
            cmd.setExecutor(executor)
            cmd.tabCompleter = executor
        }
        getCommand("staff")?.let { cmd ->
            val executor = StaffCommand(this)
            cmd.setExecutor(executor)
            cmd.tabCompleter = executor
        }
        getCommand("stats")?.let { cmd ->
            val executor = StatisticsCommand(this)
            cmd.setExecutor(executor)
            cmd.tabCompleter = executor
        }
    }

    private fun registerEvents() {
        server.pluginManager.apply {
            registerEvents(GateListener(this@TransitPlugin), this@TransitPlugin)
            registerEvents(TransitListener(this@TransitPlugin), this@TransitPlugin)
        }
    }

    private fun setupEconomy(): Boolean {
        if (!isEnabled) {
            return false
        }
        
        val rsp = server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            logger.severe("No Vault economy service found! Make sure Vault is properly installed.")
            return false
        }
        
        economy = rsp.provider
        logger.info("Successfully hooked into Vault economy!")
        return true
    }

    private fun startAutoSave() {
        val saveInterval = config.getLong("settings.saveInterval", DEFAULT_SAVE_INTERVAL) * 20L // Convert seconds to ticks
        server.scheduler.runTaskTimer(this, Runnable {
            saveAllData()
        }, saveInterval, saveInterval)
    }

    fun reload() {
        try {
            // Reload config first
            reloadConfig()
            
            // Reload all managers
            configManager.reloadConfig()
            stationManager.reload()
            routeManager.reload()
            fareManager.reload()
            gateManager.reload()
            staffManager.reload()
            statisticsManager.reload()
            transactionManager.reload()
            
            // Cancel and restart scheduled tasks
            server.scheduler.cancelTasks(this)
            startAutoSave()
            
            logger.info("Transit plugin reloaded successfully!")
        } catch (e: Exception) {
            logger.severe("Failed to reload Transit plugin: ${e.message}")
            if (config.getBoolean("settings.debug", false)) {
                e.printStackTrace()
            }
        }
    }

    private fun saveAllData() {
        if (!isEnabled) return  // Don't save if plugin is disabled
        
        try {
            // Save all manager data
            stationManager.saveAll()
            routeManager.saveAll()
            gateManager.saveAll()
            staffManager.saveAll()
            statisticsManager.saveAll()
            transactionManager.saveAll()
            
            if (config.getBoolean("settings.debug", false)) {
                logger.info("Auto-save completed successfully")
            }
        } catch (e: Exception) {
            logger.severe("Failed to save plugin data: ${e.message}")
            if (config.getBoolean("settings.debug", false)) {
                e.printStackTrace()
            }
        }
    }

    override fun onDisable() {
        try {
            if (isEnabled) {  // Only save if we were properly enabled
                // Save all data before shutdown
                saveAllData()
            }
            
            // Cancel all scheduled tasks
            server.scheduler.cancelTasks(this)
            
            // Unregister API service
            server.servicesManager.unregisterAll(this)
            
            logger.info("Transit plugin disabled successfully!")
        } catch (e: Exception) {
            logger.severe("Error while disabling Transit plugin: ${e.message}")
            e.printStackTrace()
        }
    }

    companion object {
        const val PLUGIN_VERSION = "1.0.0"
        const val MINIMUM_BUKKIT_VERSION = "1.20"
        
        // Config defaults
        const val DEFAULT_SAVE_INTERVAL = 300L // 5 minutes
        const val DEFAULT_MAX_FARE = 10.0
        const val DEFAULT_BASE_FARE = 2.75
    }
}