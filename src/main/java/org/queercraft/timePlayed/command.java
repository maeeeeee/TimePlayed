package org.queercraft.timePlayed;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class command implements CommandExecutor {
    private final QueryAPIAccessor queryAPI;

    public command(QueryAPIAccessor queryAPI) {
        this.queryAPI = queryAPI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /playtime <player>");
            return true;
        }

        String playerName = args[0];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            // Try to get offline player
            UUID playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
            sendPlaytime(sender, playerName, playerUUID);
        } else {
            sendPlaytime(sender, target.getName(), target.getUniqueId());
        }

        return true;
    }

    private void sendPlaytime(CommandSender sender, String playerName, UUID playerUUID) {
        long days30 = queryAPI.getPlaytimeLast30d(playerUUID);
        long days7 = queryAPI.getPlaytimeLast7d(playerUUID);

        sender.sendMessage("§6=== Playtime for " + playerName + " ===");
        sender.sendMessage("§aLast 7 days: §f" + formatTime(days7));
        sender.sendMessage("§aLast 30 days: §f" + formatTime(days30));
    }

    private String formatTime(long milliseconds) {
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60;

        return String.format("%dh %dm", hours, minutes);
    }
}