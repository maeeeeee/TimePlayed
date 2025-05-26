package org.queercraft.timePlayed.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.queercraft.timePlayed.PlanHook;
import org.queercraft.timePlayed.QueryAPIAccessor;
import org.queercraft.timePlayed.utils.PlayerResolver;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.queercraft.timePlayed.utils.FormatUtils.formatTimeMillis;
import static org.queercraft.timePlayed.utils.JoindateUtils.getJoinDate;
import static org.queercraft.timePlayed.utils.PlaytimeUtils.getTotalPlaytime;

public class PlaytimeCommand extends SafeCommandExecutor {

    private final JavaPlugin plugin;
    private final BukkitScheduler scheduler;
    private final PlayerResolver resolver;
    private QueryAPIAccessor queryAPI;

    public PlaytimeCommand(JavaPlugin plugin, BukkitScheduler scheduler, PlayerResolver resolver, QueryAPIAccessor queryAPI, Logger logger) {
        super(logger);
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.resolver = resolver;
        this.queryAPI = queryAPI;
    }

    @Override
    protected boolean execute(CommandSender sender, Command command, String label, String[] args) {
        resolver.resolvePlayer(sender, args, "/playtime <player>", (s, name, uuid) -> {
            scheduler.runTaskAsynchronously(plugin, () -> {
                command(s, name, uuid);
            });
        });
        return true;
    }

    public void command(CommandSender sender, String playerName, UUID playerUUID) {
        try {
            boolean extendedPlaytime = plugin.getConfig().getBoolean("features.extendedPlaytime");
            long days30 = queryAPI.getPlaytimeThisMonth(playerUUID);
            long days7 = queryAPI.getPlaytimeThisWeek(playerUUID);
            long today = queryAPI.getPlaytimeToday(playerUUID);

            long totalPlaytime = getTotalPlaytime(playerUUID, extendedPlaytime);
            long totalPlaytimeNova5 = getTotalPlaytime(playerUUID, false);

            String joindate = getJoinDate(playerUUID, plugin.getConfig().getBoolean("features.extendedJoindates"));

            //For some reason the §f formatting here causes some of the times to not be visible in AMP. They do appear ingame so probably some weird AMP fuckery
            sender.sendMessage("§6=== Playtime for " + playerName + " ===");
            sender.sendMessage("§aDaily, Weekly and Monthly values update when you log in");
            sender.sendMessage("§aToday: §f" + formatTimeMillis(today));
            sender.sendMessage("§aThis Week: §f" + formatTimeMillis(days7));
            sender.sendMessage("§aThis Month: §f" + formatTimeMillis(days30));
            sender.sendMessage("§aThis Nova: §f" + formatTimeMillis(totalPlaytimeNova5 / 20 * 1000L));
            sender.sendMessage("§aTotal: §f" + formatTimeMillis(totalPlaytime / 20 * 1000L)); //totalPlaytime is in ticks, needs to be converted to milliseconds
            sender.sendMessage("§aJoined on: §f" + joindate);

        } catch (Exception e) {
            //DB connection went down, create new Accessor
            if (Objects.equals(e.getMessage(), "SQL Failure: database connection closed")) {
                logger.warning("Exception type: " + e.getClass().getName());
                logger.warning("Message: " + e.getMessage());
                logger.warning("DB Connection is down, creating new QueryAPIAccessor...");
                try {
                    Optional<QueryAPIAccessor> queryAPIOptional = new PlanHook().hookIntoPlan();
                    this.queryAPI = queryAPIOptional.get();
                } catch (Exception ex) {
                    logger.severe("An unexpected error occurred while trying to reestablish DB connection:");
                    logger.severe("Exception type: " + ex.getClass().getName());
                    logger.severe("Message: " + ex.getMessage());
                    for (StackTraceElement stackTraceLine : ex.getStackTrace()) {
                        logger.severe("    at " + stackTraceLine);
                    }
                }
                logger.warning("Successfully created new QueryAPIAccessor");
                sender.sendMessage("§cSomething went wrong. Please try again!");
            } else {
                logger.severe("An unexpected error occurred:");
                logger.severe("Exception type: " + e.getClass().getName());
                logger.severe("Message: " + e.getMessage());
                for (StackTraceElement stackTraceLine : e.getStackTrace()) {
                    logger.severe("    at " + stackTraceLine);
                }
                sender.sendMessage("§cAn unexpected error occurred");
            }
        }
    }
}
