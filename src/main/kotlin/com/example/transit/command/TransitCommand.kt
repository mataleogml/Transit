package com.example.transit.command

import com.example.transit.TransitPlugin
import com.example.transit.model.PaymentPeriod
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TransitCommand(private val plugin: TransitPlugin) : CommandExecutor, TabCompleter {
    
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].toLowerCase()) {
            "staff" -> handleStaffCommand(sender, args)
            "stat" -> handleStatCommand(sender, args)
            "fare" -> handleFareCommand(sender, args)
            else -> sendHelp(sender)
        }

        return true
    }

    private fun handleStaffCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("transit.admin")) {
            sender.sendMessage("§cYou don't have permission to manage staff!")
            return
        }

        when {
            args.size < 4 -> sender.sendMessage("§cUsage: /transit staff <add/remove/salary> <system> <player>")
            args[1].equals("add", true) -> {
                val player = Bukkit.getPlayer(args[3])
                if (player == null) {
                    sender.sendMessage("§cPlayer not found!")
                    return
                }
                plugin.staffManager.addStaffMember(player.uniqueId, args[2], 0.0, PaymentPeriod.MONTHLY)
                sender.sendMessage("§aAdded ${player.name} as staff for ${args[2]}")
            }
            args[1].equals("salary", true) -> {
                if (args.size < 6) {
                    sender.sendMessage("§cUsage: /transit staff salary <system> <player> <amount> [period]")
                    return
                }
                val player = Bukkit.getPlayer(args[3])
                if (player == null) {
                    sender.sendMessage("§cPlayer not found!")
                    return
                }
                val amount = args[4].toDoubleOrNull()
                if (amount == null) {
                    sender.sendMessage("§cInvalid amount!")
                    return
                }
                val period = if (args.size > 5) {
                    try {
                        PaymentPeriod.valueOf(args[5].toUpperCase())
                    } catch (e: IllegalArgumentException) {
                        PaymentPeriod.MONTHLY
                    }
                } else PaymentPeriod.MONTHLY

                plugin.staffManager.addStaffMember(player.uniqueId, args[2], amount, period)
                sender.sendMessage("§aSet ${player.name}'s salary to $${amount} per ${period.name.toLowerCase()}")
            }
            args[1].equals("remove", true) -> {
                val player = Bukkit.getPlayer(args[3])
                if (player == null) {
                    sender.sendMessage("§cPlayer not found!")
                    return
                }
                plugin.staffManager.removeStaffMember(player.uniqueId, args[2])
                sender.sendMessage("§aRemoved ${player.name} as staff from ${args[2]}")
            }
        }
    }

    private fun handleStatCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("transit.admin") && !sender.hasPermission("transit.staff")) {
            sender.sendMessage("§cYou don't have permission to view statistics!")
            return
        }

        if (args.size < 3) {
            sender.sendMessage("§cUsage: /transit stat <system> <route/station>")
            return
        }

        val systemId = args[1]
        val target = args[2]

        plugin.statisticsManager.getStatistics(systemId, target)?.let { stats ->
            sender.sendMessage("§8§l" + "=".repeat(20))
            sender.sendMessage("§6§lStatistics for $target")
            sender.sendMessage("§7Total Revenue: §f$${stats.totalRevenue}")
            sender.sendMessage("§7Total Transactions: §f${stats.totalTransactions}")
            sender.sendMessage("§7Average Fare: §f$${stats.averageFare}")
            sender.sendMessage("§8§l" + "=".repeat(20))
        } ?: sender.sendMessage("§cNo statistics found!")
    }

    private fun handleFareCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("transit.admin")) {
            sender.sendMessage("§cYou don't have permission to manage fares!")
            return
        }

        if (args.size < 2) {
            sender.sendMessage("§cUsage: /transit fare <system/refund> [transactionId]")
            return
        }

        when (args[1].toLowerCase()) {
            "refund" -> {
                if (args.size < 3) {
                    sender.sendMessage("§cUsage: /transit fare refund <transactionId>")
                    return
                }
                if (plugin.fareManager.refundTransaction(args[2])) {
                    sender.sendMessage("§aTransaction refunded successfully!")
                } else {
                    sender.sendMessage("§cFailed to refund transaction!")
                }
            }
            else -> {
                val systemBalance = plugin.transactionManager.getSystemBalance(args[1])
                sender.sendMessage("§6System Balance for ${args[1]}: §f$${systemBalance}")
            }
        }
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§6Transit System Commands:")
        sender.sendMessage("§f/transit staff add <system> <player> §7- Add staff member")
        sender.sendMessage("§f/transit staff salary <system> <player> <amount> [period] §7- Set staff salary")
        sender.sendMessage("§f/transit staff remove <system> <player> §7- Remove staff member")
        sender.sendMessage("§f/transit stat <system> <route/station> §7- View statistics")
        sender.sendMessage("§f/transit fare <system> §7- View system balance")
        sender.sendMessage("§f/transit fare refund <transactionId> §7- Refund transaction")
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        return when {
            args.isEmpty() -> listOf("staff", "stat", "fare")
            args.size == 1 -> listOf("staff", "stat", "fare").filter { it.startsWith(args[0].toLowerCase()) }
            args.size == 2 -> when (args[0].toLowerCase()) {
                "staff" -> listOf("add", "remove", "salary").filter { it.startsWith(args[1].toLowerCase()) }
                "fare" -> listOf("refund").filter { it.startsWith(args[1].toLowerCase()) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}