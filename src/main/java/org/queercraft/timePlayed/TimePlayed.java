package org.queercraft.timePlayed;

import org.bukkit.plugin.java.JavaPlugin;

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
        getLogger().info("Enabling TimePlayed...");

        try {
            Optional<QueryAPIAccessor> queryAPIOptional = new PlanHook().hookIntoPlan();
            if (queryAPIOptional.isPresent()) {
                queryAPI = queryAPIOptional.get();
                Objects.requireNonNull(getCommand("playtime")).setExecutor(new command(queryAPI));
                Objects.requireNonNull(getCommand("joindate")).setExecutor(new command(queryAPI));
                Objects.requireNonNull(getCommand("realnameoffline")).setExecutor(new command(queryAPI));
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