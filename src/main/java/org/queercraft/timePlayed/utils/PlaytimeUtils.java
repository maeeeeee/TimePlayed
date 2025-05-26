package org.queercraft.timePlayed.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PlaytimeUtils {
    private static final Logger logger = Logger.getLogger("TimePlayed");
    private static final Map<UUID, Long> oldPlaytime = new ConcurrentHashMap<>();

    //Set world/stats/ folder
    private static final File worldFolder = new File(Bukkit.getServer().getWorlds().getFirst().getWorldFolder(), "stats");


    public void loadExtendedPlaytimeData(File dataFolder) {
        File file = new File(dataFolder, "playtimeNova4and5.txt");
        logger.info("Loading playtime data from Nova 4 and Nova 5...");
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    UUID uuid = UUID.fromString(parts[0]);
                    long playtime = Long.parseLong(parts[1]);
                    oldPlaytime.put(uuid, playtime);
                }
            }
            logger.info("Successfully loaded playtime data from Nova 4 and Nova 5!");
        } catch (IOException e) {
            logger.warning("Error trying to load past Nova playtime data");
            logger.warning("Exception type: " + e.getClass().getName());
            logger.warning("Message: " + e.getMessage());
            for (StackTraceElement stackTraceLine : e.getStackTrace()) {
                logger.warning("    at " + stackTraceLine);
            }
        }
    }

    public static long getTotalPlaytime(UUID uuid, boolean extendedPlaytime) {
        //Read player's statistics from .json
        File playerStatistics = new File(worldFolder, uuid + ".json");

        long totalPlaytime = 0;

        //Player is online
        if (Bukkit.getPlayer(uuid) != null) totalPlaytime = getOnlineStatistic(Bukkit.getPlayer(uuid));

        //Player is offline
        if (playerStatistics.exists()) {
            try {
                JsonObject jsonObject = new Gson()
                        .fromJson(new FileReader(playerStatistics), JsonObject.class);
                JsonObject pilot = (JsonObject) jsonObject.get("stats");
                JsonObject passenger = (JsonObject) pilot.get("minecraft:custom");

                //Read playtime stat
                if (passenger.get("minecraft:play_time") == null) totalPlaytime = passenger
                        .get("minecraft:play_one_minute")
                        .getAsLong();
                totalPlaytime = passenger.get("minecraft:play_time").getAsLong();
            } catch (Exception e) {
                logger.warning("Error trying to fetch total playtime.");
                logger.warning("Exception type: " + e.getClass().getName());
                logger.warning("Message: " + e.getMessage());
                for (StackTraceElement stackTraceLine : e.getStackTrace()) {
                    logger.warning("    at " + stackTraceLine);
                }
            }
        }
        if (extendedPlaytime && oldPlaytime.containsKey(uuid)) {
            totalPlaytime += oldPlaytime.get(uuid);
        }
        return totalPlaytime;
    }

    public static long getOnlineStatistic(Player player) {
        return player.getStatistic(Statistic.PLAY_ONE_MINUTE);
    }
}
