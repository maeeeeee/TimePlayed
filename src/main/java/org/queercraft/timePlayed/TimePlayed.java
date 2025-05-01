package org.queercraft.timePlayed;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/*
 * THIS PLUGIN WAS BROUGHT TO YOU BY A SEVERE CASE OF STREP THROAT,
 * GITHUB COPILOT,
 * AND THE MOST CLOGGED SINUSES EVER
 * IT COMES WITH NO WARRANTY
 * GOOD LUCK!
 */
public final class TimePlayed extends JavaPlugin {
    private QueryAPIAccessor queryAPI;

    @Override
    public void onEnable() {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        getLogger().info("Enabling TimePlayed...");
        Utils utils = new Utils();
        scheduler.runTaskAsynchronously(this, utils::buildCache);
        utils.loadCache();
        scheduler.runTaskAsynchronously(this, () -> utils.loadFirstJoinData(this.getDataFolder()));
        // Schedule periodic cache refresh every 10 minutes (12000 ticks)
        scheduler.runTaskTimerAsynchronously(this, utils::buildCache, 0L, 12000L);

        try {
            Optional<QueryAPIAccessor> queryAPIOptional = new PlanHook().hookIntoPlan();
            if (queryAPIOptional.isPresent()) {
                queryAPI = queryAPIOptional.get();
                Objects.requireNonNull(getCommand("playtime")).setExecutor(new command(queryAPI, utils, scheduler, this));
                Objects.requireNonNull(getCommand("joindate")).setExecutor(new command(queryAPI, utils, scheduler, this));
                Objects.requireNonNull(getCommand("realnameoffline")).setExecutor(new command(queryAPI, utils, scheduler, this));
                //Objects.requireNonNull(getCommand("generatereport")).setExecutor(new command(queryAPI, utils, scheduler, this));
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
            Arrays.stream(e.getStackTrace())
                    .map(StackTraceElement::toString)
                    .forEach(stackTraceLine -> getLogger().severe("    at " + stackTraceLine));
            getServer().getPluginManager().disablePlugin(this);
        }

    }

    @Override
    public void onDisable() {
        getLogger().info("TimePlayed disabled!");
    }
}