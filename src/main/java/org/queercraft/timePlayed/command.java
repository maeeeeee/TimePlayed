package org.queercraft.timePlayed;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.util.UUID;

public class command implements CommandExecutor {
    private final QueryAPIAccessor queryAPI;

    public command(QueryAPIAccessor queryAPI) {
        this.queryAPI = queryAPI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        try {
            if (args.length > 1) {
                sender.sendMessage("§cUsage: /playtime <player>");
                return true;
            }else if(args.length == 0){
                //Run on self
                target = Bukkit.getPlayer(sender.getName());
            }else{
                String playerName = args[0];
                target = Bukkit.getPlayer(playerName);
            }

            if (target == null && args.length == 1) {
                // Try to get offline player
                String playerName = args[0];
                UUID playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
                sendPlaytime(sender, playerName, playerUUID);
            } else {
                sendPlaytime(sender, target.getName(), target.getUniqueId());
            }
        } catch (Exception e) {
            sender.sendMessage("§cThe requested username does not exist. You can use /realname to get the username of a nicknamed player if they are online! ");
        }

        return true;
    }

    private void sendPlaytime(CommandSender sender, String playerName, UUID playerUUID) {
        long total = queryAPI.getPlaytimeTotal(playerUUID);
        long days30 = queryAPI.getPlaytimeLast30d(playerUUID);
        long days7 = queryAPI.getPlaytimeLast7d(playerUUID);
        long today = queryAPI.getPlaytimeToday(playerUUID);
        //For some reason the §f formatting here causes some of the times to not be visible in AMP. They do appear ingame so probably some weird AMP fuckery
        sender.sendMessage("§6=== Playtime for " + playerName + " ===");
        sender.sendMessage("§aToday: §f" + formatTime(today));
        sender.sendMessage("§aLast 7 days: §f" + formatTime(days7));
        sender.sendMessage("§aLast 30 days: §f" + formatTime(days30));
        sender.sendMessage("§aTotal: §f" + formatTime(total));
    }

    private String formatTime(long milliseconds) {
        long totalMinutes = (milliseconds / 1000) / 60;
        long days = (totalMinutes / 60) / 24;
        long hours = (totalMinutes - (days*24*60)) / 60;
        long minutes = (totalMinutes - (days*24*60)) - (hours*60);

        StringBuilder timeString = new StringBuilder();

        if (days > 0) {
            timeString.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            timeString.append(hours).append("h ");
        }
        if (minutes > 0 || timeString.length() == 0) {
            timeString.append(minutes).append("m");
        }

        return timeString.toString();
    }

}