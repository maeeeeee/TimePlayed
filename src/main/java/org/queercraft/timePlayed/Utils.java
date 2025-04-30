package org.queercraft.timePlayed;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {
    private static final Logger logger = Logger.getLogger("TimePlayed");

    // Cache map for quick lookups
    private Map<String, String> nicknameCache = new HashMap<>();
    private final String CACHE_FILE = "nickname_cache.json";

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

    public void buildCache() {
        logger.info("Starting cache refresh...");
        Path dirPath = Paths.get("plugins/Essentials/userdata");
        Gson gson = new Gson();

        // Regex to match and clean color-coded nicknames
        Pattern nicknamePattern = Pattern.compile("nickname:\\s*(.*)");
        Pattern colorCodePattern = Pattern.compile(
                "§x(?:§[a-fA-F0-9]){6}|§[0-9a-fA-Fklmnor]"
        );

        try {
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                logger.severe("Invalid directory path: " + dirPath.toAbsolutePath());
                return;
            }

            Map<String, String> cache = new HashMap<>();

            // Traverse the directory to find .yml files
            Files.walk(dirPath)
                    .filter(path -> path.toString().endsWith(".yml") && Files.isRegularFile(path))
                    .forEach(path -> {
                        try {
                            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                            for (String line : lines) {
                                Matcher matcher = nicknamePattern.matcher(line);
                                if (matcher.find()) {
                                    String nicknameWithColor = matcher.group(1).trim();

                                    // Remove color codes
                                    String plainNickname = colorCodePattern.matcher(nicknameWithColor).replaceAll("");

                                    String fileName = path.getFileName().toString();
                                    fileName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;

                                    // Add to cache
                                    cache.put(fileName, plainNickname);
                                    break; // Stop after finding the nickname
                                }
                            }
                        } catch (IOException e) {
                            logger.severe("Failed to read file: " + path.getFileName());
                            logger.severe("Exception type: " + e.getClass().getName());
                            logger.severe("Message: " + e.getMessage());
                            for (StackTraceElement stackTraceLine : e.getStackTrace()) {
                                logger.severe("    at " + stackTraceLine);
                            }
                        }
                    });

            // Save cache to JSON file
            try (Writer writer = new FileWriter(CACHE_FILE)) {
                gson.toJson(cache, writer);
            }

            // Update in-memory cache
            nicknameCache = cache;
            logger.info("Cache built successfully.");

        } catch (IOException e) {
            logger.severe("Failed to build cache.");
            logger.severe("Exception type: " + e.getClass().getName());
            logger.severe("Message: " + e.getMessage());
            for (StackTraceElement stackTraceLine : e.getStackTrace()) {
                logger.severe("    at " + stackTraceLine);
            }
        }
    }

    /**
     * Loads the cache from the JSON file into memory.
     */
    public void loadCache() {
        Gson gson = new Gson();
        try (Reader reader = new FileReader(CACHE_FILE)) {
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            nicknameCache = gson.fromJson(reader, type);
            logger.info("Cache loaded.");
        } catch (FileNotFoundException e) {
            logger.warning("Cache file not found. Building a new cache.");
            buildCache();
        } catch (IOException e) {
            logger.severe("Failed to load cache.");
            logger.severe("Exception type: " + e.getClass().getName());
            logger.severe("Message: " + e.getMessage());
            for (StackTraceElement stackTraceLine : e.getStackTrace()) {
                logger.severe("    at " + stackTraceLine);
            }
        }
    }

    /**
     * Searches for a file by nickname in the in-memory cache.
     *
     * @param nickname the nickname to search for
     * @return the file names without extension (the player's uuid) that are associated with this nickname, or null if not found
     */
    public synchronized List<String> getNicknamedPlayer(String nickname) {
        return nicknameCache.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(nickname))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
