package com.example.transit

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransitPluginTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: TransitPlugin

    @BeforeAll
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(TransitPlugin::class.java)
    }

    @AfterAll
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `test station creation`() {
        val player = server.addPlayer()
        
        // Execute station creation command
        server.executeCommand(player, "station add Subway Central_Station")
        
        // Verify station was created
        val station = plugin.stationManager.getStation("Subway_central_station")
        assertNotNull(station)
        assertEquals("Central_Station", station?.name)
        assertEquals("Subway", station?.systemId)
    }

    @Test
    fun `test fare calculation`() {
        val player = server.addPlayer()
        val startStation = createTestStation("Subway", "Start", "1")
        val endStation = createTestStation("Subway", "End", "2")
        
        val fare = plugin.fareManager.calculateFare(
            plugin.configManager.getTransitSystem("Subway")!!,
            startStation,
            endStation
        )
        
        assertTrue(fare > 0)
    }

    private fun createTestStation(systemId: String, name: String, zone: String) = 
        plugin.stationManager.addStation(
            Station(
                id = "${systemId}_${name.toLowerCase()}",
                name = name,
                systemId = systemId,
                location = server.addPlayer().location,
                zone = zone
            )
        )
}