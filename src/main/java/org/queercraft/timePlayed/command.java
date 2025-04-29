package org.queercraft.timePlayed;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class command implements CommandExecutor {
    private QueryAPIAccessor queryAPI;
    private static final Logger logger = Logger.getLogger("TimePlayed");

    public command(QueryAPIAccessor queryAPI) {
        this.queryAPI = queryAPI;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        try {
            if (command.getName().equalsIgnoreCase("joindate")) {
                commandJoindate(sender, args);
            } else if (command.getName().equalsIgnoreCase("playtime")) {
                commandPlaytime(sender, args);
            } else {
                commandRealname(sender, args);
            }
        } catch (java.lang.NumberFormatException e) {
            sender.sendMessage("§cThe requested username does not exist. You can use /realname to get the username of a nicknamed player if they are online! ");
            logger.warning("Exception type: " + e.getClass().getName());
            logger.warning("Message: " + e.getMessage());
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
        return true;
    }

    //TODO: extract common code from the following two methods into new universal executeCommand method

    private void commandJoindate(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 1) {
            sender.sendMessage("§cUsage: /joindate <player>");
        } else if (args.length == 0) {
            //Run on self
            target = Bukkit.getPlayer(sender.getName());
            assert target != null;
            sendJoindate(sender, target.getName(), target.getUniqueId());
        } else {
            String playerName = args[0];
            target = Bukkit.getPlayer(playerName);
            if (target == null) {
                //player is not online
                UUID playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
                if (Utils.offlinePlayerExists(playerUUID)) {
                    //player is offline but exists
                    sendJoindate(sender, playerName, playerUUID);
                    return;
                }
                String nicknamedUuid = Utils.getNicknamedPlayer(playerName);
                if (nicknamedUuid != null) {
                    //player is nicknamed
                    sender.sendMessage("No player by that username found, trying nicknames. Result may be wrong if nickname exists twice.");
                    UUID uuid = UUID.fromString(nicknamedUuid);
                    if (Utils.isPlayerOnline(uuid)) {
                        //real username is online
                        sendJoindate(sender, playerName, uuid);
                    } else if (Utils.offlinePlayerExists(uuid)) {
                        //real username is offline
                        sendJoindate(sender, Bukkit.getOfflinePlayer(uuid).getName(), uuid);
                    } else {
                        //wtf how did we get here, should not be reachable but :shrug:
                        sender.sendMessage("Player is nicknamed, but real name could not be found automatically. Try again with their real username!");
                    }
                } else {
                    sender.sendMessage("§cThis user does not exist");
                }
            } else {
                //player is online
                sendJoindate(sender, target.getName(), target.getUniqueId());
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
            assert target != null;
            sendPlaytime(sender, target.getName(), target.getUniqueId());
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
                String nicknamedUuid = Utils.getNicknamedPlayer(playerName);
                if (nicknamedUuid != null) {
                    //player is nicknamed
                    sender.sendMessage("No player by that username found, trying nicknames. Result may be wrong if nickname exists twice.");
                    UUID uuid = UUID.fromString(nicknamedUuid);
                    if (Utils.isPlayerOnline(uuid)) {
                        //real username is online
                        sendPlaytime(sender, playerName, uuid);
                    } else if (Utils.offlinePlayerExists(uuid)) {
                        //real username is offline
                        sendPlaytime(sender, Bukkit.getOfflinePlayer(uuid).getName(), uuid);
                    } else {
                        //wtf how did we get here, should not be reachable but :shrug:
                        sender.sendMessage("Player is nicknamed, but real name could not be found automatically. Try again with their real username!");
                    }
                } else {
                    sender.sendMessage("§cThis user does not exist");
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
            String nickName = args[0];
            String nicknamedUuid = Utils.getNicknamedPlayer(nickName);
            if (nicknamedUuid != null) {
                //player is nicknamed
                sender.sendMessage("§aOffline nickname found. Result may be wrong if nickname exists twice.");
                UUID uuid = UUID.fromString(nicknamedUuid);
                if (Utils.isPlayerOnline(uuid)) {
                    //real username is online
                    sender.sendMessage(nickName + "§ais §f" + Bukkit.getPlayer(uuid).getName());
                } else if (Utils.offlinePlayerExists(uuid)) {
                    //real username is offline
                    sender.sendMessage(nickName + " §ais offline and might be §f" + Bukkit.getOfflinePlayer(uuid).getName());
                } else {
                    //wtf how did we get here, should not be reachable but :shrug:
                    sender.sendMessage("§aNickname was found, but the corresponding player could not be identified.");
                }
            } else {
                sender.sendMessage("§cNo username found for this nickname");
            }
        }
    }

    private void sendJoindate(CommandSender sender, String playerName, UUID playerUUID) {
        String joindate = Utils.getJoinDate(playerUUID);
        sender.sendMessage(playerName + " §ajoined on: §f" + joindate);
    }

    private void sendPlaytime(CommandSender sender, String playerName, UUID playerUUID) {
        long days30 = queryAPI.getPlaytimeThisMonth(playerUUID);
        long days7 = queryAPI.getPlaytimeThisWeek(playerUUID);
        long today = queryAPI.getPlaytimeToday(playerUUID);

        long totalPlaytime = Utils.getTotalPlaytime(playerUUID);
        String joindate = Utils.getJoinDate(playerUUID);

        //For some reason the §f formatting here causes some of the times to not be visible in AMP. They do appear ingame so probably some weird AMP fuckery
        sender.sendMessage("§6=== Playtime for " + playerName + " ===");
        sender.sendMessage("§aDaily, Weekly and Monthly values update when you log in");
        sender.sendMessage("§aToday: §f" + Utils.formatTimeMillis(today));
        sender.sendMessage("§aThis Week: §f" + Utils.formatTimeMillis(days7));
        sender.sendMessage("§aThis Month: §f" + Utils.formatTimeMillis(days30));
        sender.sendMessage("§aTotal: §f" + Utils.formatTimeMillis(totalPlaytime / 20 * 1000L)); //totalPlaytime is in ticks, needs to be converted to milliseconds
        sender.sendMessage("§aJoined on: §f" + joindate);
    }


}