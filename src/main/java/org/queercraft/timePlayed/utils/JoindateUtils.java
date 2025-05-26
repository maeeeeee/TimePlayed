package org.queercraft.timePlayed.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.queercraft.timePlayed.utils.PlayerUtils.isPlayerOnline;
import static org.queercraft.timePlayed.utils.PlayerUtils.offlinePlayerExists;

public class JoindateUtils {
    private static final Logger logger = Logger.getLogger("TimePlayed");

    private static final Map<UUID, Long> firstJoinTimestamps = new ConcurrentHashMap<>();

    public void loadFirstJoinData(File dataFolder) {
        File file = new File(dataFolder, "joindatesNova4and5.txt");
        logger.info("Loading joindate data from Nova 4 and Nova 5...");
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    UUID uuid = UUID.fromString(parts[0]);
                    long timestamp = Long.parseLong(parts[1]);
                    firstJoinTimestamps.put(uuid, timestamp);
                }
            }
            logger.info("Successfully loaded joindate data from Nova 4 and Nova 5!");
        } catch (IOException e) {
            logger.warning("Error trying to load past Nova join data");
            logger.warning("Exception type: " + e.getClass().getName());
            logger.warning("Message: " + e.getMessage());
            for (StackTraceElement stackTraceLine : e.getStackTrace()) {
                logger.warning("    at " + stackTraceLine);
            }
        }
    }

    public static String getJoinDate(UUID uuid, boolean extendedJoindates) {
        Player player = Bukkit.getPlayer(uuid);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Calendar calendar = Calendar.getInstance();
        long firstPlayed;
        if (isPlayerOnline(uuid)) {
            assert player != null;
            firstPlayed = player.getFirstPlayed();
        } else if (offlinePlayerExists(uuid)) {
            firstPlayed = Bukkit.getOfflinePlayer(uuid).getFirstPlayed();
        } else return "Has never joined";
        if (extendedJoindates && firstJoinTimestamps.containsKey(uuid)) {
            long recordedTimestamp = firstJoinTimestamps.get(uuid);
            if (recordedTimestamp < firstPlayed) {
                firstPlayed = recordedTimestamp; // Replace with the earlier timestamp
            }
        }
        if (firstPlayed < 1000000000000L) calendar.setTimeInMillis(firstPlayed * 1000);
        else calendar.setTimeInMillis(firstPlayed);
        return simpleDateFormat.format(calendar.getTime());
    }
}
