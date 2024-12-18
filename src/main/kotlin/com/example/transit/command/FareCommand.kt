package com.example.transit.command

import com.example.transit.TransitPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class FareCommand(private val plugin: TransitPlugin) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /fare <system/refund> [uuid]")
            return true
        }

        when (args[0].lowercase()) {
            "refund" -> handleRefund(sender, args)
            else -> handleSystemBalance(sender, args[0])
        }
        return true
    }

    private fun handleRefund(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /fare refund <uuid>")
            return
        }

        if (plugin.fareManager.refundTransaction(args[1])) {
            sender.sendMessage("§aTransaction refunded successfully!")
        } else {
            sender.sendMessage("§cFailed to refund transaction!")
        }
    }

    private fun handleSystemBalance(sender: CommandSender, systemId: String) {
        val balance = plugin.transactionManager.getSystemBalance(systemId)
        sender.sendMessage("§6System Balance for $systemId: §f$${balance}")
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (args.isEmpty()) return emptyList()
        
        return when (args.size) {
            1 -> listOf("refund").filter { it.startsWith(args[0].lowercase()) }
            else -> emptyList()
        }
    }
}