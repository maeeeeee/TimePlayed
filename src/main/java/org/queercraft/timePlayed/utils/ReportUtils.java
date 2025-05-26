package org.queercraft.timePlayed.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.queercraft.timePlayed.QueryAPIAccessor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.logging.Logger;

public class ReportUtils {
    private static final Logger logger = Logger.getLogger("TimePlayed");
    public void generatePlaytimeReport(JavaPlugin plugin, String month, CommandSender sender, QueryAPIAccessor queryAPI) {
        String date;
        if (month.equals("this")) {
            date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MMMM"));
        } else if (month.equals("last")) {
            LocalDate now = LocalDate.now();
            LocalDate lastMonth = now.minusMonths(1); // Go to the previous month
            date = lastMonth.format(DateTimeFormatter.ofPattern("yyyy_MMMM"));
        } else {
            sender.sendMessage("Invalid input, please retry with either \"this\" or \"last\"");
            return;
        }

        // Get the current year
        String currentYear = String.valueOf(LocalDate.now().getYear());

        // Define the directory structure: Reports/<Year>
        File yearFolder = new File(plugin.getDataFolder(), "Reports" + File.separator + currentYear);

        if (!yearFolder.exists() && !yearFolder.mkdirs()) {
            sender.sendMessage("Failed to create the directory for the report. Please check file permissions.");
            return;
        }

        File outputFile = new File(yearFolder, date + "_playtime-report.txt");

        sender.sendMessage("§aGenerating report for §f" + month + "§a month. This will take a long time (>20 minutes)");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("Username, Playtime, UUID");
            writer.newLine();

            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                UUID playerUUID = player.getUniqueId();
                String username = player.getName();
                long playtime = queryAPI.getPlaytimeLastMonth(playerUUID);

                // Write to the file
                writer.write(String.format("%s, %s, %s", username, FormatUtils.formatTimeMillis(playtime), playerUUID));
                writer.newLine();
            }
        } catch (IOException e) {
            logger.severe("Failed to generate playtime report: " + e.getMessage());
            logger.severe("Exception type: " + e.getClass().getName());
            logger.severe("Message: " + e.getMessage());
            for (StackTraceElement stackTraceLine : e.getStackTrace()) {
                logger.severe("    at " + stackTraceLine);
            }
        }

        // Schedule back to the main thread to log completion
        Bukkit.getScheduler().runTask(plugin, () ->
                sender.sendMessage("Playtime report successfully generated at " + outputFile.getAbsolutePath())
        );
    }
}
