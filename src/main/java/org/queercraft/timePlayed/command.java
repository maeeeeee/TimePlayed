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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class command implements CommandExecutor {
    private QueryAPIAccessor queryAPI;
    private static final Logger logger = Logger.getLogger("TimePlayed");

    public command(QueryAPIAccessor queryAPI) {
        this.queryAPI = queryAPI;
    }

    /*TODO: Everything in here needs to be refactored to hell and back.
    If anyone gets to this before I do, I suggest moving everything after sendPlaytime into a Util class and
    extracting all those offline/nickname checks into standalone methods.
    */

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

    private void sendJoindate(CommandSender sender, String playerName, UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        String localName = playerName;
        UUID localUuid = playerUUID;
        if (player == null) {
            //username is not online, could be offline, unknown or nicknamed
            if (Bukkit.getOfflinePlayer(playerName).getFirstPlayed() == 0) {
                //username is not known, could be nicknamed
                if (getNicknamedPlayer(playerName) != null) {
                    player = Bukkit.getPlayer(getNicknamedPlayer(playerName));
                    localName = player.getName();
                    localUuid = UUID.fromString(getNicknamedPlayer(playerName));
                }
            }
        }
        String joindate = getJoinDate(localUuid, localName);
        sender.sendMessage(localName + " §ajoined on: §f" + joindate);
    }

    private void sendPlaytime(CommandSender sender, String playerName, UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        String localName = playerName;
        UUID localUuid = playerUUID;
        if (player == null) {
            //username is not online, could be offline, unknown or nicknamed
            if (Bukkit.getOfflinePlayer(playerName).getFirstPlayed() == 0) {
                //username is not known, could be nicknamed
                if (getNicknamedPlayer(playerName) != null) {
                    player = Bukkit.getPlayer(getNicknamedPlayer(playerName));
                    localName = player.getName();
                    localUuid = UUID.fromString(getNicknamedPlayer(playerName));
                }
            }
        }

        long days30 = queryAPI.getPlaytimeThisMonth(localUuid);
        long days7 = queryAPI.getPlaytimeThisWeek(localUuid);
        long today = queryAPI.getPlaytimeToday(localUuid);

        long totalPlaytime = getTotalPlaytime(localUuid, playerName);
        String joindate = getJoinDate(localUuid, playerName);

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

    public long getTotalPlaytime(UUID uuid, String playerName) {
        Player player = Bukkit.getPlayer(uuid);
        UUID localUuid = uuid;

        if (player == null) {
            //username is not online, could be offline, unknown or nicknamed
            if (Bukkit.getOfflinePlayer(playerName).getFirstPlayed() == 0) {
                //username is not known, could be nicknamed
                if (getNicknamedPlayer(playerName) != null) {
                    player = Bukkit.getPlayer(getNicknamedPlayer(playerName));
                    localUuid = UUID.fromString(getNicknamedPlayer(playerName));
                }
            }
        }

        //Read player's statistics from .json
        File playerStatistics = new File(worldFolder, localUuid + ".json");

        //Player is online
        if (Bukkit.getPlayer(localUuid) != null) return getOnlineStatistic(Bukkit.getPlayer(localUuid));

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

    public String getJoinDate(UUID uuid, String playerName) {
        Player player = Bukkit.getPlayer(uuid);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Calendar calendar = Calendar.getInstance();

        if (player == null) {
            //username not online, could be offline, unknown or nicknamed
            long joinTimestamp = Bukkit.getOfflinePlayer(uuid).getFirstPlayed();
            if (joinTimestamp == 0) {
                //username not known, check nickname
                if (getNicknamedPlayer(playerName) != null) {
                    //nickname found
                    UUID foundUUID = UUID.fromString(getNicknamedPlayer(playerName));
                    player = Bukkit.getPlayer(foundUUID);
                    if (player == null) {
                        //player is offline
                        joinTimestamp = Bukkit.getOfflinePlayer(playerName).getFirstPlayed();
                    } else if (player.hasPlayedBefore()) {
                        //player is online
                        joinTimestamp = player.getFirstPlayed();
                    } else {
                        //what the fuck happened how did we get here
                        return "Player is nicknamed, but real name could not be found automatically. Try again with their real username!";
                    }
                } else return "Has never joined"; //player not known and not nicknamed
            } else {
                //username is offline
                joinTimestamp = Bukkit.getOfflinePlayer(uuid).getFirstPlayed();
            }
            calendar.setTimeInMillis(joinTimestamp);

            return simpleDateFormat.format(calendar.getTime());
        } else if (player.hasPlayedBefore()) {
            //username is online
            long joinTimestamp = player.getFirstPlayed();
            if (joinTimestamp == 0) {
                //not supposed to happen
                return "Has never joined";
            }
            calendar.setTimeInMillis(joinTimestamp);
            return simpleDateFormat.format(calendar.getTime());
        }
        //idk if this is even reachable
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

    public String getNicknamedPlayer(String playerName) {
        logger.warning("DEBUG getNicknamedPlayer called with: " + playerName);
        //This method is rather expensive since it searches through all .yml files in /Essentials/userdata, should only be called deliberately
        //TODO: handle both kinds of color coding

        // Convert the userName into a regex pattern: .*u.*s.*e.*r.*N.*a.*m.*e to match through color coding
        String userNameRegex = playerName.chars()
                .mapToObj(c -> ".*" + Pattern.quote(String.valueOf((char) c)))
                .reduce((a, b) -> a + b)
                .orElse(".*");

        // Full regex pattern to match "nickname: userName"
        String regexPattern = "nickname:\\s*" + userNameRegex;

        try {
            // Relative path to Essentials/userdata directory
            Path dirPath = Paths.get("plugins/Essentials/userdata");

            // Ensure the directory exists
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                throw new IllegalArgumentException("Invalid directory path: " + dirPath.toAbsolutePath());
            }

            // Compile the regex pattern
            Pattern pattern = Pattern.compile(regexPattern);

            // Traverse the directory to find .yml files
            return Files.walk(dirPath)
                    .filter(path -> path.toString().endsWith(".yml") && Files.isRegularFile(path))
                    .map(path -> {
                        try {
                            // Read the file's content
                            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

                            // Check if any line matches the regex pattern
                            for (String line : lines) {
                                Matcher matcher = pattern.matcher(line);
                                if (matcher.find()) {
                                    String fileName = path.getFileName().toString();
                                    logger.warning("DEBUG getNicknamedPlayer found match with: " + fileName);
                                    return fileName.contains(".")
                                            ? fileName.substring(0, fileName.lastIndexOf('.'))
                                            : fileName;
                                }
                            }
                        } catch (IOException e) {
                            logger.severe("Failed to read file: " + path.getFileName());
                        }
                        return null;
                    })
                    .filter(Objects::nonNull) // Remove nulls
                    .findFirst() // Stop at the first match
                    .orElse(null); // Return null if no match is found

        } catch (IOException e) {
            logger.severe("Failed to traverse directory: plugins/Essentials/userdata");
        }
        return null;
    }

}