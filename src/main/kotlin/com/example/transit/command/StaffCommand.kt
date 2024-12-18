package com.example.transit.command

import com.example.transit.TransitPlugin
import com.example.transit.model.PaymentPeriod
import com.example.transit.model.StaffRole
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.UUID

class StaffCommand(private val plugin: TransitPlugin) : CommandExecutor, TabCompleter {
    
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
            "list" -> handleList(sender, args)
            "add" -> handleAdd(sender, args)
            "remove" -> handleRemove(sender, args)
            "role" -> handleRole(sender, args)
            "shift" -> handleShift(sender, args)
            "performance" -> handlePerformance(sender, args)
            "salary" -> handleSalary(sender, args)
            else -> sendHelp(sender)
        }
        return true
    }

    private fun handleList(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("transit.staff.list")) {
            sender.sendMessage("§cYou don't have permission to list staff members!")
            return
        }

        if (args.size < 2) {
            sender.sendMessage("§cUsage: /staff list <system>")
            return
        }

        val systemId = args[1]
        val staffList = plugin.staffManager.getSystemStaff(systemId)
        
        if (staffList.isEmpty()) {
            sender.sendMessage("§cNo staff members found for system $systemId")
            return
        }

        sender.sendMessage("§6Staff members for system $systemId:")
        staffList.forEach { staff ->
            val playerName = plugin.server.getOfflinePlayer(staff.playerId).name ?: staff.playerId
            sender.sendMessage("""
                §7- $playerName
                  §7Role: §f${staff.role}
                  §7Salary: §f$${staff.salary}
                  §7Payment Period: §f${staff.paymentPeriod}
            """.trimIndent())
        }
    }

    private fun handleAdd(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("transit.staff.add")) {
            sender.sendMessage("§cYou don't have permission to add staff members!")
            return
        }

        if (args.size < 4) {
            sender.sendMessage("§cUsage: /staff add <system> <player> <salary> [role] [period]")
            return
        }

        val systemId = args[1]
        val player = plugin.server.getPlayer(args[2])
        if (player == null) {
            sender.sendMessage("§cPlayer not found!")
            return
        }

        val salary = args[3].toDoubleOrNull()
        if (salary == null || salary <= 0) {
            sender.sendMessage("§cInvalid salary amount!")
            return
        }

        val role = if (args.size > 4) {
            try {
                StaffRole.valueOf(args[4].uppercase())
            } catch (e: IllegalArgumentException) {
                StaffRole.TRAINEE
            }
        } else StaffRole.TRAINEE

        val period = if (args.size > 5) {
            try {
                PaymentPeriod.valueOf(args[5].uppercase())
            } catch (e: IllegalArgumentException) {
                PaymentPeriod.MONTHLY
            }
        } else PaymentPeriod.MONTHLY

        if (plugin.staffManager.addStaffMember(player.uniqueId, systemId, role, salary, period)) {
            sender.sendMessage("§aStaff member added successfully!")
            player.sendMessage("§aYou have been added as a staff member for system $systemId")
        } else {
            sender.sendMessage("§cFailed to add staff member! They may already be staff.")
        }
    }

    private fun handleRemove(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("transit.staff.remove")) {
            sender.sendMessage("§cYou don't have permission to remove staff members!")
            return
        }

        if (args.size < 3) {
            sender.sendMessage("§cUsage: /staff remove <system> <player>")
            return
        }

        val systemId = args[1]
        val playerId = try {
            UUID.fromString(args[2])
        } catch (e: IllegalArgumentException) {
            plugin.server.getPlayer(args[2])?.uniqueId
        }

        if (playerId == null) {
            sender.sendMessage("§cPlayer not found!")
            return
        }

        if (plugin.staffManager.removeStaffMember(playerId, systemId)) {
            sender.sendMessage("§aStaff member removed successfully!")
            plugin.server.getPlayer(playerId)?.sendMessage(
                "§cYou have been removed from staff in system $systemId"
            )
        } else {
            sender.sendMessage("§cFailed to remove staff member!")
        }
    }

    private fun handleRole(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("transit.staff.role")) {
            sender.sendMessage("§cYou don't have permission to modify staff roles!")
            return
        }

        if (args.size < 4) {
            sender.sendMessage("§cUsage: /staff role <system> <player> <role>")
            return
        }

        val systemId = args[1]
        val player = plugin.server.getPlayer(args[2])
        if (player == null) {
            sender.sendMessage("§cPlayer not found!")
            return
        }

        val role = try {
            StaffRole.valueOf(args[3].uppercase())
        } catch (e: IllegalArgumentException) {
            sender.sendMessage("§cInvalid role! Valid roles: ${StaffRole.values().joinToString()}")
            return
        }

        if (plugin.staffManager.updateStaffRole(player.uniqueId, systemId, role)) {
            sender.sendMessage("§aStaff role updated successfully!")
            player.sendMessage("§aYour role has been updated to $role in system $systemId")
        } else {
            sender.sendMessage("§cFailed to update staff role!")
        }
    }

    private fun handleShift(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("§cThis command can only be used by players!")
            return
        }

        if (args.size < 3) {
            sender.sendMessage("§cUsage: /staff shift <start/end> <system>")
            return
        }

        val systemId = args[2]
        when (args[1].toLowerCase()) {
            "start" -> {
                if (plugin.staffManager.startShift(sender.uniqueId, systemId)) {
                    sender.sendMessage("§aShift started successfully!")
                } else {
                    sender.sendMessage("§cFailed to start shift! You may already have an active shift.")
                }
            }
            "end" -> {
                if (plugin.staffManager.endShift(sender.uniqueId)) {
                    sender.sendMessage("§aShift ended successfully!")
                } else {
                    sender.sendMessage("§cFailed to end shift! No active shift found.")
                }
            }
            else -> sender.sendMessage("§cUsage: /staff shift <start/end> <system>")
        }
    }

    private fun handlePerformance(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("transit.staff.performance")) {
            sender.sendMessage("§cYou don't have permission to view performance!")
            return
        }

        if (args.size < 2) {
            sender.sendMessage("§cUsage: /staff performance <player>")
            return
        }

        val player = plugin.server.getPlayer(args[1])
        if (player == null) {
            sender.sendMessage("§cPlayer not found!")
            return
        }

        val performance = plugin.staffManager.getStaffPerformance(player.uniqueId)
        if (performance == null) {
            sender.sendMessage("§cNo performance data found for this player!")
            return
        }

        sender.sendMessage("""
            §6Performance Report for ${player.name}:
            §7Transactions: §f${performance.transactions}
            §7Customer Interactions: §f${performance.customerInteractions}
            §7Incident Count: §f${performance.incidentCount}
            §7Last Evaluation: §f${performance.lastEvaluation}
        """.trimIndent())
    }

    private fun handleSalary(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("transit.staff.salary")) {
            sender.sendMessage("§cYou don't have permission to modify salaries!")
            return
        }

        if (args.size < 4) {
            sender.sendMessage("§cUsage: /staff salary <system> <player> <amount>")
            return
        }

        val systemId = args[1]
        val player = plugin.server.getPlayer(args[2])
        if (player == null) {
            sender.sendMessage("§cPlayer not found!")
            return
        }

        val salary = args[3].toDoubleOrNull()
        if (salary == null || salary <= 0) {
            sender.sendMessage("§cInvalid salary amount!")
            return
        }

        if (plugin.staffManager.updateSalary(player.uniqueId, systemId, salary)) {
            sender.sendMessage("§aSalary updated successfully!")
            player.sendMessage("§aYour salary has been updated to $$salary")
        } else {
            sender.sendMessage("§cFailed to update salary!")
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        return when (args.size) {
            1 -> listOf("list", "add", "remove", "role", "shift", "performance", "salary")
                .filter { it.startsWith(args[0].toLowerCase()) }
            2 -> when (args[0].toLowerCase()) {
                "list", "add", "remove", "role", "salary" -> 
                    plugin.configManager.getSystems().map { it.id }
                "shift" -> listOf("start", "end")
                "performance" -> plugin.server.onlinePlayers.map { it.name }
                else -> emptyList()
            }.filter { it.startsWith(args[1].toLowerCase()) }
            3 -> when (args[0].toLowerCase()) {
                "add", "remove", "role", "salary" -> 
                    plugin.server.onlinePlayers.map { it.name }
                "shift" -> plugin.configManager.getSystems().map { it.id }
                else -> emptyList()
            }.filter { it.startsWith(args[2].toLowerCase()) }
            4 -> when (args[0].toLowerCase()) {
                "role" -> StaffRole.values().map { it.name.toLowerCase() }
                else -> emptyList()
            }.filter { it.startsWith(args[3].toLowerCase()) }
            else -> emptyList()
        }
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("""
            §6Staff Management Commands:
            §f/staff list <system> - List all staff members
            §f/staff add <system> <player> <salary> [role] [period] - Add staff member
            §f/staff remove <system> <player> - Remove staff member
            §f/staff role <system> <player> <role> - Change staff role
            §f/staff shift <start/end> <system> - Manage shifts
            §f/staff performance <player> - View performance stats
            §f/staff salary <system> <player> <amount> - Update salary
        """.trimIndent())
    }
}