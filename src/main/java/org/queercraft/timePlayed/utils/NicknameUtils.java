package org.queercraft.timePlayed.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NicknameUtils {

    private static final Logger logger = Logger.getLogger("TimePlayed");

    // Cache map for quick lookups
    private Map<String, String> nicknameCache = new HashMap<>();
    private final String CACHE_FILE = "nickname_cache.json";

    public void buildCache() {
        logger.info("Starting nickname cache refresh...");
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

    public synchronized List<String> getNicknamedPlayer(String nickname, CommandSender sender) {
        List<String> exactMatches = nicknameCache.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(nickname))
                .map(Map.Entry::getKey)
                .toList();
//        if (!exactMatches.isEmpty()) {
//            return exactMatches.stream().limit(10).collect(Collectors.toList());
//        }
        return exactMatches.stream().limit(10).collect(Collectors.toList());
//        sender.sendMessage("§a No exact matches found. Potential matches found for:");
//        // If no exact matches, get the fuzzy matches (limit to 5 results)
//        // Restrict the number of fuzzy matches to 5
//        return nicknameCache.entrySet().stream()
//                .filter(entry -> isSimilarNickname(entry.getValue(), nickname))
//                .map(Map.Entry::getKey)
//                .limit(5)  // Restrict the number of fuzzy matches to 5
//                .collect(Collectors.toList());
    }

    private boolean isSimilarNickname(String nickname, String target) {
        // Prevent returning matches if the target nickname is too short (e.g. "L")
        if (target.length() < 3) {
            return false;
        }

        // Levenshtein distance check
        LevenshteinDistance levenshtein = new LevenshteinDistance();
        int distance = levenshtein.apply(nickname, target);

        // fine tune similarity
        return distance <= 3;
    }
}
