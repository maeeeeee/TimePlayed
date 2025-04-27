package org.queercraft.timePlayed;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class command implements CommandExecutor {
    private QueryAPIAccessor queryAPI;
    private static final Logger logger = Logger.getLogger(command.class.getName());

    public command(QueryAPIAccessor queryAPI) {
        this.queryAPI = queryAPI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        try {
            if (command.getName().equalsIgnoreCase("joindate")) {
                if (args.length > 1) {
                    sender.sendMessage("§cUsage: /joindate <player>");
                    return true;
                } else if (args.length == 0) {
                    //Run on self
                    target = Bukkit.getPlayer(sender.getName());
                } else {
                    String playerName = args[0];
                    target = Bukkit.getPlayer(playerName);
                }
                if (target == null && args.length == 1) {
                    // Try to get offline player
                    String playerName = args[0];
                    UUID playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
                    sendJoindate(sender, playerName, playerUUID);
                } else {
                    sendJoindate(sender, target.getName(), target.getUniqueId());
                }
            } else {
                if (args.length > 1) {
                    sender.sendMessage("§cUsage: /playtime <player>");
                    return true;
                } else if (args.length == 0) {
                    //Run on self
                    target = Bukkit.getPlayer(sender.getName());
                } else {
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
            }
        } catch (java.lang.NumberFormatException e) {
            sender.sendMessage("§cThe requested username does not exist. You can use /realname to get the username of a nicknamed player if they are online! ");
            logger.warning("Exception type: " + e.getClass().getName());
            logger.warning("Message: " + e.getMessage());
        } catch (Exception e) {
            //DB connection went down, create new Accessor
            if(Objects.equals(e.getMessage(), "SQL Failure: database connection closed")){
                logger.warning("Exception type: " + e.getClass().getName());
                logger.warning("Message: " + e.getMessage());
                logger.warning("DB Connection is down, creating new QueryAPIAccessor...");
                try {
                    Optional<QueryAPIAccessor> queryAPIOptional = new PlanHook().hookIntoPlan();
                    this.queryAPI = queryAPIOptional.get();
                }catch (Exception ex){
                    logger.severe("An unexpected error occurred while trying to reestablish DB connection:");
                    logger.severe("Exception type: " + ex.getClass().getName());
                    logger.severe("Message: " + ex.getMessage());
                    for (StackTraceElement stackTraceLine : ex.getStackTrace()) {
                        logger.severe("    at " + stackTraceLine);
                    }
                }
                logger.warning("Successfully created new QueryAPIAccessor");
                sender.sendMessage("§cSomething went wrong. Please try again!");
            } else{
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

    private void sendJoindate(CommandSender sender, String playerName, UUID playerUUID) {
        String joindate = getJoinDate(playerUUID);
        sender.sendMessage(playerName + " §ajoined on: §f" + joindate);
    }

    private void sendPlaytime(CommandSender sender, String playerName, UUID playerUUID) {
        long days30 = queryAPI.getPlaytimeThisMonth(playerUUID);
        long days7 = queryAPI.getPlaytimeThisWeek(playerUUID);
        long today = queryAPI.getPlaytimeToday(playerUUID);

        long totalPlaytime = getTotalPlaytime(playerUUID);
        String joindate = getJoinDate(playerUUID);

        //For some reason the §f formatting here causes some of the times to not be visible in AMP. They do appear ingame so probably some weird AMP fuckery
        sender.sendMessage("§6=== Playtime for " + playerName + " ===");
        sender.sendMessage("§aDaily, Weekly and Monthly values update when you log in");
        sender.sendMessage("§aToday: §f" + formatTimeMillis(today));
        sender.sendMessage("§aThis Week: §f" + formatTimeMillis(days7));
        sender.sendMessage("§aThis Month: §f" + formatTimeMillis(days30));
        sender.sendMessage("§aTotal: §f" + formatTimeMillis(totalPlaytime / 20 * 1000L)); //totalPlaytime is in ticks, needs to be converted to milliseconds
        sender.sendMessage("§aJoined on: §f" + joindate);
    }

    //Set world/stats/ folder
    File worldFolder = new File(Bukkit.getServer().getWorlds().get(0).getWorldFolder(), "stats");

    public long getTotalPlaytime(UUID uuid) {
        //Read player's statistics from .json
        File playerStatistics = new File(worldFolder, uuid + ".json");

        //Player is online
        if (Bukkit.getPlayer(uuid) != null) return getOnlineStatistic(Bukkit.getPlayer(uuid));

        //Player is offline
        if (playerStatistics.exists()) {
            try {
                JsonObject jsonObject = new Gson()
                        .fromJson(new FileReader(playerStatistics), JsonObject.class);
                JsonObject pilot = (JsonObject) jsonObject.get("stats");
                JsonObject passenger = (JsonObject) pilot.get("minecraft:custom");

                //Read playtime stat
                if (passenger.get("minecraft:play_time") == null) return passenger
                        .get("minecraft:play_one_minute")
                        .getAsLong();
                return passenger.get("minecraft:play_time").getAsLong();
            } catch (Exception e) {
                logger.warning("Error trying to fetch total playtime.");
                logger.warning("Exception type: " + e.getClass().getName());
                logger.warning("Message: " + e.getMessage());
                for (StackTraceElement stackTraceLine : e.getStackTrace()) {
                    logger.warning("    at " + stackTraceLine);
                }
            }
        }
        return 0;
    }

    public String getJoinDate(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Calendar calendar = Calendar.getInstance();

        if (player == null) {
            long joinTimestamp = Bukkit.getOfflinePlayer(uuid).getFirstPlayed();
            if(joinTimestamp == 0){
                return "Has never joined";
            }
            calendar.setTimeInMillis(joinTimestamp);

            return simpleDateFormat.format(calendar.getTime());
        } else if (player.hasPlayedBefore()) {
            long joinTimestamp = player.getFirstPlayed();
            if(joinTimestamp == 0){
                return "Has never joined";
            }
            calendar.setTimeInMillis(joinTimestamp);
            return simpleDateFormat.format(calendar.getTime());
        }

        return "Has never joined";
    }

    public long getOnlineStatistic(Player player) {
        return player.getStatistic(Statistic.PLAY_ONE_MINUTE);
    }

    private String formatTimeMillis(long milliseconds) {
        long totalMinutes = (milliseconds / 1000) / 60;
        long days = (totalMinutes / 60) / 24;
        long hours = (totalMinutes - (days * 24 * 60)) / 60;
        long minutes = (totalMinutes - (days * 24 * 60)) - (hours * 60);

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