package org.queercraft.timePlayed;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final Logger logger = Logger.getLogger("TimePlayed");

    //Set world/stats/ folder
    private static final File worldFolder = new File(Bukkit.getServer().getWorlds().getFirst().getWorldFolder(), "stats");

    public static boolean isPlayerOnline(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null;
    }

    public static boolean offlinePlayerExists(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return Bukkit.getOfflinePlayer(uuid).getFirstPlayed() != 0;
        }
        return false;
    }

    public static long getTotalPlaytime(UUID uuid) {
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

    public static String getJoinDate(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Calendar calendar = Calendar.getInstance();
        if (isPlayerOnline(uuid)) {
            assert player != null;
            calendar.setTimeInMillis(player.getFirstPlayed());
            return simpleDateFormat.format(calendar.getTime());
        } else if (offlinePlayerExists(uuid)) {
            calendar.setTimeInMillis(Bukkit.getOfflinePlayer(uuid).getFirstPlayed());
            return simpleDateFormat.format(calendar.getTime());
        } else return "Has never joined";
    }

    public static long getOnlineStatistic(Player player) {
        return player.getStatistic(Statistic.PLAY_ONE_MINUTE);
    }

    static String formatTimeMillis(long milliseconds) {
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
        if (minutes > 0 || timeString.isEmpty()) {
            timeString.append(minutes).append("m");
        }

        return timeString.toString();
    }

    public static String getNicknamedPlayer(String playerName) {
        //This method is rather expensive since it searches through all .yml files in /Essentials/userdata, should only be called deliberately

        // Pattern to match color coding in Essentials userdata file (§x)
        String userNameRegex = playerName.chars()
                .mapToObj(c -> "(?:§x(?:§[a-fA-F0-9]){6})?" + Pattern.quote(String.valueOf((char) c)))
                .reduce((a, b) -> a + b)
                .orElse("");

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
                                    logger.info("DEBUG getNicknamedPlayer found match with: " + fileName);
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
