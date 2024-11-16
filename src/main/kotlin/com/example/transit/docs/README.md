# Transit Plugin API Documentation

## Overview
The Transit Plugin provides a comprehensive public API for other plugins to integrate with the transit system. This document outlines the available APIs and how to use them.

## Getting Started

### Plugin Dependency
Add the Transit plugin as a dependency in your `plugin.yml`:
```yaml
depend: [Transit]
```

### Accessing the API
```kotlin
val transitAPI = server.pluginManager.getPlugin("Transit")?.let { 
    (it as TransitPlugin).api 
}
```

## Available APIs

### Station Management
```kotlin
// Get station information
val station = transitAPI.getStation(systemId, stationName)

// Add new station
transitAPI.addStation(
    systemId = "Subway",
    name = "Central_Station",
    location = location,
    zone = "1"
)
```

### Route Management
```kotlin
// Get route information
val route = transitAPI.getRoute(routeId)

// Add station to route
transitAPI.addRouteStation(routeId, stationId)
```

### Transaction Management
```kotlin
// Get player's recent transactions
val transactions = transitAPI.getPlayerTransactions(playerId, limit = 10)

// Get system revenue
val revenue = transitAPI.getSystemRevenue(
    systemId = "Subway",
    period = StatisticsManager.StatisticsPeriod.MONTHLY
)
```

### Staff Management
```kotlin
// Check if player is staff
val isStaff = transitAPI.isStaffMember(player, systemId)
```

## Events
Your plugin can listen to various Transit events:

### Gate Entry/Exit
```kotlin
@EventHandler
fun onTransitGateEntry(event: TransitGateEntryEvent) {
    val player = event.player
    val station = event.station
    val fare = event.fareAmount
    
    // Custom handling
}

@EventHandler
fun onTransitGateExit(event: TransitGateExitEvent) {
    val player = event.player
    val fromStation = event.fromStation
    val toStation = event.toStation
    val fare = event.fareAmount
    
    // Custom handling
}
```

### Staff Payments
```kotlin
@EventHandler
fun onStaffPayment(event: TransitStaffPaymentEvent) {
    val player = event.player
    val amount = event.amount
    
    // Custom handling
}
```

## Example Integration

Here's a complete example of integrating with the Transit plugin:

```kotlin
class YourPlugin : JavaPlugin() {
    private lateinit var transitAPI: TransitAPI

    override fun onEnable() {
        // Get Transit API
        transitAPI = server.pluginManager.getPlugin("Transit")?.let { 
            (it as TransitPlugin).api 
        } ?: run {
            logger.severe("Transit plugin not found!")
            server.pluginManager.disablePlugin(this)
            return
        }

        // Register event listeners
        server.pluginManager.registerEvents(TransitEventListener(), this)
    }

    inner class TransitEventListener : Listener {
        @EventHandler
        fun onGateEntry(event: TransitGateEntryEvent) {
            // Your custom handling
        }
    }
}
```

## Best Practices

1. Always check if the Transit plugin is present before accessing the API
2. Use try-catch blocks when calling API methods
3. Handle events asynchronously when possible
4. Cache API results when appropriate
5. Use the provided events rather than polling for changes

## Support

For issues or questions:
1. Check the plugin's GitHub repository
2. Join our Discord server
3. Open an issue ticket
