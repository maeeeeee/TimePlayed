package org.queercraft.timePlayed.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public abstract class SafeCommandExecutor implements CommandExecutor {

    protected final Logger logger;

    public SafeCommandExecutor(Logger logger) {
        this.logger = logger;
    }

    @Override
    public final boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        try {
            return execute(sender, command, label, args);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cThe requested username does not exist. You can use /realname to get the username of a nicknamed player if they are online!");
            logger.warning("Exception type: " + e.getClass().getName());
            logger.warning("Message: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("An unexpected error occurred:");
            logger.severe("Exception type: " + e.getClass().getName());
            logger.severe("Message: " + e.getMessage());
            for (StackTraceElement stackTraceLine : e.getStackTrace()) {
                logger.severe("    at " + stackTraceLine);
            }
            sender.sendMessage("§cAn unexpected error occurred");
        }
        return true;
    }

    protected abstract boolean execute(CommandSender sender, Command command, String label, String[] args);
}