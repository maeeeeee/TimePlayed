package org.queercraft.timePlayed.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.queercraft.timePlayed.utils.PlayerResolver;

import java.util.UUID;
import java.util.logging.Logger;

import static org.queercraft.timePlayed.utils.JoindateUtils.getJoinDate;

public class JoindateCommand extends SafeCommandExecutor {

    private final JavaPlugin plugin;
    private final BukkitScheduler scheduler;
    private final PlayerResolver resolver;
    private final boolean extended;

    public JoindateCommand(JavaPlugin plugin, BukkitScheduler scheduler, PlayerResolver resolver, boolean extended, Logger logger) {
        super(logger);
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.resolver = resolver;
        this.extended = extended;
    }

    protected boolean execute(CommandSender sender, Command command, String label, String[] args) {
        resolver.resolvePlayer(sender, args, "/joindate <player>", (s, name, uuid) -> {
            scheduler.runTaskAsynchronously(plugin, () -> {
                command(s, name, uuid, extended);
            });
        });
        return true;
    }

    public void command(CommandSender sender, String playerName, UUID playerUUID, boolean extended) {
        try {
            String joindate = getJoinDate(playerUUID, extended);
            sender.sendMessage(playerName + " §ajoined on: §f" + joindate);
        } catch (Exception e) {
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
