package org.queercraft.timePlayed.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NicknameUtils {

    public class NicknameEntry {
        private String colored;
        private String plain;

        public NicknameEntry(String colored, String plain) {
            this.colored = colored;
            this.plain = plain;
        }

        public String getColored() {
            return colored;
        }

        public String getPlain() {
            return plain;
        }
    }

    private static final Logger logger = Logger.getLogger("TimePlayed");

    // Cache map for quick lookups
    private Map<String, NicknameEntry> nicknameCache = new HashMap<>();
    private final String CACHE_FILE = "plugins/TimePlayed/nickname_cache.json";

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

            Map<String, NicknameEntry> cache = new HashMap<>();

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
                                    cache.put(fileName, new NicknameEntry(nicknameWithColor, plainNickname));
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
            Type type = new TypeToken<Map<String, NicknameEntry>>() {
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

    public synchronized List<String> getNicknamedPlayers(String nickname) {
        List<String> partialMatches = nicknameCache.entrySet().stream()
                .filter(entry -> entry.getValue().getPlain().toLowerCase().contains(nickname.toLowerCase()))
                .map(Map.Entry::getKey)
                .toList();
        return new ArrayList<>(partialMatches);
    }

    public synchronized NicknameEntry getNicknameForUUID(UUID uuid) {
        return nicknameCache.getOrDefault(uuid.toString(), null);
    }
}
