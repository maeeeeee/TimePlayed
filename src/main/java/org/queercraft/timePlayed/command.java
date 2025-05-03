package org.queercraft.timePlayed;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class command implements CommandExecutor {
    private final JavaPlugin plugin;
    private final BukkitScheduler scheduler;
    private QueryAPIAccessor queryAPI;
    private final Utils utils;
    private static final Logger logger = Logger.getLogger("TimePlayed");

    public command(QueryAPIAccessor queryAPI, Utils utils, BukkitScheduler scheduler, JavaPlugin plugin) {
        this.queryAPI = queryAPI;
        this.utils = utils;
        this.scheduler = scheduler;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        try {
            if (command.getName().equalsIgnoreCase("timeplayed") && args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("timeplayed.reload")) {
                    // Reload the configuration
                    plugin.reloadConfig();
                    sender.sendMessage("The config has been reloaded.");
                    return true;
                } else {
                    sender.sendMessage("You do not have permission to reload the config.");
                    return false;
                }
            } else if (command.getName().equalsIgnoreCase("joindate")) {
                commandJoindate(sender, args);
            } else if (command.getName().equalsIgnoreCase("playtime")) {
                scheduler.runTaskAsynchronously(plugin, () -> commandPlaytime(sender, args));
            } else if (command.getName().equalsIgnoreCase("realnameoffline")) {
                scheduler.runTaskAsynchronously(plugin, () -> commandRealname(sender, args));
            } else if (command.getName().equalsIgnoreCase("generatereport")) {
                scheduler.runTaskAsynchronously(plugin, () -> commandGeneratereport(sender, args));
            }
        } catch (java.lang.NumberFormatException e) {
            sender.sendMessage("§cThe requested username does not exist. You can use /realname to get the username of a nicknamed player if they are online! ");
            logger.warning("Exception type: " + e.getClass().getName());
            logger.warning("Message: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("An unexpected error occurred:");
            logger.severe("Exception type: " + e.getClass().getName());
            logger.severe("Message: " + e.getMessage());
            for (StackTraceElement stackTraceLine : e.getStackTrace()) {
                logger.severe("    at " + stackTraceLine);
            }
            sender.sendMessage("§cAn unexpected error occurred");
        }
        return true;
    }

    //TODO: extract common code from the following two methods into new universal executeCommand method

    private void commandJoindate(CommandSender sender, String[] args) {
        boolean extendedJoindates = plugin.getConfig().getBoolean("features.extendedJoindates");
        Player target;
        if (args.length > 1) {
            sender.sendMessage("§cUsage: /joindate <player>");
        } else if (args.length == 0) {
            //Run on self
            target = Bukkit.getPlayer(sender.getName());
            if (target == null) {
                sender.sendMessage("§cAn error occurred. If you are seeing this from console, you probably forgot to specify a player name.");
            } else {
                sendJoindate(sender, target.getName(), target.getUniqueId(), extendedJoindates);
            }
        } else {
            String playerName = args[0];
            target = Bukkit.getPlayer(playerName);
            if (target == null) {
                //player is not online
                UUID playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
                if (Utils.offlinePlayerExists(playerUUID)) {
                    //player is offline but exists
                    sendJoindate(sender, playerName, playerUUID, extendedJoindates);
                    return;
                }
                //playername is not known, check for nicknames
                List<String> nicknamedUuids = utils.getNicknamedPlayer(playerName, sender);
                if (nicknamedUuids == null || nicknamedUuids.isEmpty()) {
                    //not a nickname
                    sender.sendMessage("§cNo player with that nickname found");
                } else if (nicknamedUuids.size() == 1) {
                    //exactly one match found
                    UUID uuid = UUID.fromString(nicknamedUuids.getFirst());
                    if (Utils.isPlayerOnline(uuid)) {
                        //real username is online
                        sendJoindate(sender, playerName, uuid, extendedJoindates);
                    } else if (Utils.offlinePlayerExists(uuid)) {
                        //real username is offline
                        sendJoindate(sender, Bukkit.getOfflinePlayer(uuid).getName(), uuid, extendedJoindates);
                    } else {
                        //wtf how did we get here, should not be reachable but :shrug:
                        sender.sendMessage("§aPlayer is nicknamed, but real name could not be found automatically. Try again with their real username!");
                    }
                } else {
                    sender.sendMessage("§aMultiple matches found for nickname §f" + playerName + "§a:");
                    for (String uuid : nicknamedUuids) {
                        if (Utils.isPlayerOnline(UUID.fromString(uuid))) {
                            sender.sendMessage("§a" + Bukkit.getPlayer(UUID.fromString(uuid)).getName());
                        } else {
                            sender.sendMessage("§a" + Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName());
                        }
                    }
                    sender.sendMessage("§aPlease try again with the right username from this list");
                }
            } else {
                //player is online
                sendJoindate(sender, target.getName(), target.getUniqueId(), extendedJoindates);
            }
        }
    }

    private void commandPlaytime(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 1) {
            sender.sendMessage("§cUsage: /playtime <player>");
        } else if (args.length == 0) {
            //Run on self
            target = Bukkit.getPlayer(sender.getName());
            if (target == null) {
                sender.sendMessage("§cAn error occurred. If you are seeing this from console, you probably forgot to specify a player name.");
            } else {
                sendPlaytime(sender, target.getName(), target.getUniqueId());
            }
        } else {
            String playerName = args[0];
            target = Bukkit.getPlayer(playerName);
            if (target == null) {
                //player is not online
                UUID playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
                if (Utils.offlinePlayerExists(playerUUID)) {
                    //player is offline but exists
                    sendPlaytime(sender, playerName, playerUUID);
                    return;
                }
                //playername is not known, check for nicknames
                List<String> nicknamedUuids = utils.getNicknamedPlayer(playerName, sender);
                if (nicknamedUuids == null || nicknamedUuids.isEmpty()) {
                    //not a nickname
                    sender.sendMessage("§cNo player with that nickname found");
                } else if (nicknamedUuids.size() == 1) {
                    //exactly one match found
                    UUID uuid = UUID.fromString(nicknamedUuids.getFirst());
                    if (Utils.isPlayerOnline(uuid)) {
                        //real username is online
                        sendPlaytime(sender, playerName, uuid);
                    } else if (Utils.offlinePlayerExists(uuid)) {
                        //real username is offline
                        sendPlaytime(sender, Bukkit.getOfflinePlayer(uuid).getName(), uuid);
                    } else {
                        //wtf how did we get here, should not be reachable but :shrug:
                        sender.sendMessage("§aPlayer is nicknamed, but real name could not be found automatically. Try again with their real username!");
                    }
                } else {
                    sender.sendMessage("§aMultiple matches found for nickname §f" + playerName + "§a:");
                    for (String uuid : nicknamedUuids) {
                        if (Utils.isPlayerOnline(UUID.fromString(uuid))) {
                            sender.sendMessage("§a" + Bukkit.getPlayer(UUID.fromString(uuid)).getName());
                        } else {
                            sender.sendMessage("§a" + Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName());
                        }
                    }
                    sender.sendMessage("§aPlease try again with the right username from this list");
                }
            } else {
                //player is online
                sendPlaytime(sender, target.getName(), target.getUniqueId());
            }
        }
    }

    private void commandRealname(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /realnameoffline <player>");
        } else {
            String playerName = args[0];
            List<String> nicknamedUuids = utils.getNicknamedPlayer(playerName, sender);
            if (nicknamedUuids == null || nicknamedUuids.isEmpty()) {
                //not a nickname
                sender.sendMessage("§cNo player with that nickname found");
            } else if (nicknamedUuids.size() == 1) {
                //exactly one match found
                UUID uuid = UUID.fromString(nicknamedUuids.getFirst());
                if (Utils.isPlayerOnline(uuid)) {
                    //real username is online
                    sender.sendMessage(playerName + " §ais §f" + Bukkit.getPlayer(uuid).getName());
                } else if (Utils.offlinePlayerExists(uuid)) {
                    //real username is offline
                    sender.sendMessage(playerName + " §ais §f" + Bukkit.getOfflinePlayer(uuid).getName());
                } else {
                    //wtf how did we get here, should not be reachable but :shrug:
                    sender.sendMessage("§aPlayer is nicknamed, but real name could not be identified. This is not supposed to happen.");
                }
            } else {
                sender.sendMessage("§aMultiple matches found for nickname §f" + playerName + "§a:");
                for (String uuid : nicknamedUuids) {
                    if (Utils.isPlayerOnline(UUID.fromString(uuid))) {
                        sender.sendMessage("§a" + Bukkit.getPlayer(UUID.fromString(uuid)).getName());
                    } else {
                        sender.sendMessage("§a" + Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName());
                    }
                }
            }
        }
    }

    private void commandGeneratereport(CommandSender sender, String[] args) {
        //utils.generatePlaytimeReport(plugin, args[0],sender,queryAPI);
    }

    private void sendJoindate(CommandSender sender, String playerName, UUID playerUUID, boolean extendedJoindates) {
        try {
            String joindate = Utils.getJoinDate(playerUUID, extendedJoindates);
            sender.sendMessage(playerName + " §ajoined on: §f" + joindate);
        } catch (Exception e) {
            logger.severe("An unexpected error occurred:");
            logger.severe("Exception type: " + e.getClass().getName());
            logger.severe("Message: " + e.getMessage());
            for (StackTraceElement stackTraceLine : e.getStackTrace()) {
                logger.severe("    at " + stackTraceLine);
            }
            sender.sendMessage("§cAn unexpected error occurred");
        }
    }

    private void sendPlaytime(CommandSender sender, String playerName, UUID playerUUID) {
        try {
            long days30 = queryAPI.getPlaytimeThisMonth(playerUUID);
            long days7 = queryAPI.getPlaytimeThisWeek(playerUUID);
            long today = queryAPI.getPlaytimeToday(playerUUID);

            long totalPlaytime = Utils.getTotalPlaytime(playerUUID, plugin.getConfig().getBoolean("features.extendedPlaytime"));

            String joindate = Utils.getJoinDate(playerUUID, plugin.getConfig().getBoolean("features.extendedJoindates"));

            //For some reason the §f formatting here causes some of the times to not be visible in AMP. They do appear ingame so probably some weird AMP fuckery
            sender.sendMessage("§6=== Playtime for " + playerName + " ===");
            sender.sendMessage("§aDaily, Weekly and Monthly values update when you log in");
            sender.sendMessage("§aToday: §f" + Utils.formatTimeMillis(today));
            sender.sendMessage("§aThis Week: §f" + Utils.formatTimeMillis(days7));
            sender.sendMessage("§aThis Month: §f" + Utils.formatTimeMillis(days30));
            sender.sendMessage("§aTotal: §f" + Utils.formatTimeMillis(totalPlaytime / 20 * 1000L)); //totalPlaytime is in ticks, needs to be converted to milliseconds
            sender.sendMessage("§aJoined on: §f" + joindate);
        } catch (Exception e) {
            //DB connection went down, create new Accessor
            if (Objects.equals(e.getMessage(), "SQL Failure: database connection closed")) {
                logger.warning("Exception type: " + e.getClass().getName());
                logger.warning("Message: " + e.getMessage());
                logger.warning("DB Connection is down, creating new QueryAPIAccessor...");
                try {
                    Optional<QueryAPIAccessor> queryAPIOptional = new PlanHook().hookIntoPlan();
                    this.queryAPI = queryAPIOptional.get();
                } catch (Exception ex) {
                    logger.severe("An unexpected error occurred while trying to reestablish DB connection:");
                    logger.severe("Exception type: " + ex.getClass().getName());
                    logger.severe("Message: " + ex.getMessage());
                    for (StackTraceElement stackTraceLine : ex.getStackTrace()) {
                        logger.severe("    at " + stackTraceLine);
                    }
                }
                logger.warning("Successfully created new QueryAPIAccessor");
                sender.sendMessage("§cSomething went wrong. Please try again!");
            } else {
                logger.severe("An unexpected error occurred:");
                logger.severe("Exception type: " + e.getClass().getName());
                logger.severe("Message: " + e.getMessage());
                for (StackTraceElement stackTraceLine : e.getStackTrace()) {
                    logger.severe("    at " + stackTraceLine);
                }
                sender.sendMessage("§cAn unexpected error occurred");
            }
        }
    }
}