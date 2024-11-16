package com.example.transit.config

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File

class Messages(plugin: Plugin) {
    private val messages: YamlConfiguration

    init {
        val messageFile = File(plugin.dataFolder, "messages.yml")
        if (!messageFile.exists()) {
            plugin.saveResource("messages.yml", false)
        }
        messages = YamlConfiguration.loadConfiguration(messageFile)
    }

    fun get(key: String, params: Map<String, String> = emptyMap()): String {
        var message = messages.getString(key) ?: return "§cMissing message: $key"
        params.forEach { (key, value) ->
            message = message.replace("{$key}", value)
        }
        return message.replace("&", "§")
    }
}