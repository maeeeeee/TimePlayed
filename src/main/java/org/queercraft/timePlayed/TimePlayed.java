package org.queercraft.timePlayed;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.queercraft.timePlayed.commands.JoindateCommand;
import org.queercraft.timePlayed.commands.PlaytimeCommand;
import org.queercraft.timePlayed.commands.RealnameCommand;
import org.queercraft.timePlayed.commands.TimeplayedCommand;
import org.queercraft.timePlayed.utils.*;


import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public final class TimePlayed extends JavaPlugin {
    private QueryAPIAccessor queryAPI;

    @Override
    public void onEnable() {
        NicknameUtils nicknameUtils = new NicknameUtils();
        JoindateUtils joindateUtils = new JoindateUtils();
        PlaytimeUtils playtimeUtils = new PlaytimeUtils();
        ReportUtils reportUtils = new ReportUtils();
        BukkitScheduler scheduler = Bukkit.getScheduler();
        PlayerResolver resolver = new PlayerResolver(nicknameUtils);
        Logger logger = getLogger();
        boolean extendedJoindates = getConfig().getBoolean("features.extendedJoindates");
        logger.info("Enabling TimePlayed...");

        saveDefaultConfig();
        FileConfiguration config = getConfig();

        boolean extendedPlaytimeEnabled = config.getBoolean("features.extendedPlaytime");
        boolean extendedJoindatesEnabled = config.getBoolean("features.extendedJoindates");
        boolean generateReportsEnabled = config.getBoolean("features.generateReports");

        if (extendedPlaytimeEnabled) getLogger().info("Extended playtime enabled.");
        else getLogger().info("Extended playtime disabled.");
        if (extendedJoindatesEnabled) getLogger().info("Extended joindates enabled.");
        else getLogger().info("Extended joindates disabled.");
        if (generateReportsEnabled) getLogger().info("Generate reports enabled.");
        else getLogger().info("Generate reports disabled.");


        scheduler.runTaskAsynchronously(this, nicknameUtils::buildCache);
        nicknameUtils.loadCache();
        scheduler.runTaskAsynchronously(this, () -> joindateUtils.loadFirstJoinData(this.getDataFolder()));
        scheduler.runTaskAsynchronously(this, () -> playtimeUtils.loadExtendedPlaytimeData(this.getDataFolder()));

        // Schedule periodic cache refresh every 10 minutes (12000 ticks)
        scheduler.runTaskTimerAsynchronously(this, nicknameUtils::buildCache, 0L, 12000L);

        try {
            Optional<QueryAPIAccessor> queryAPIOptional = new PlanHook().hookIntoPlan();
            if (queryAPIOptional.isPresent()) {
                queryAPI = queryAPIOptional.get();
                Objects.requireNonNull(getCommand("timeplayed")).setExecutor(new TimeplayedCommand(this, scheduler, reportUtils, queryAPI, logger));
                Objects.requireNonNull(getCommand("playtime")).setExecutor(new PlaytimeCommand(this, scheduler, resolver, queryAPI, logger));
                Objects.requireNonNull(getCommand("joindate")).setExecutor(new JoindateCommand(this, scheduler, resolver, extendedJoindates, logger));
                Objects.requireNonNull(getCommand("realnameoffline")).setExecutor(new RealnameCommand(this, scheduler, nicknameUtils, logger));
            } else {
                getLogger().warning("Failed to hook into Plan, disabling TimePlayed...");
                getServer().getPluginManager().disablePlugin(this);
            }
        } catch (NoClassDefFoundError planIsNotInstalled) {
            getLogger().warning("Could not find a Plan installation, disabling TimePlayed...");
            getServer().getPluginManager().disablePlugin(this);
        } catch (Exception e) {
            getLogger().severe("An unexpected error occurred while enabling TimePlayed:");
            getLogger().severe("Exception type: " + e.getClass().getName());
            getLogger().severe("Message: " + e.getMessage());
            Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).forEach(stackTraceLine -> getLogger().severe("    at " + stackTraceLine));
            getServer().getPluginManager().disablePlugin(this);
        }

    }

    @Override
    public void onDisable() {
        getLogger().info("TimePlayed disabled!");
    }
}