package org.queercraft.timePlayed.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.queercraft.timePlayed.utils.NicknameUtils;
import org.queercraft.timePlayed.utils.PlayerUtils;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class RealnameCommand extends SafeCommandExecutor {

    private final JavaPlugin plugin;
    private final BukkitScheduler scheduler;
    private final NicknameUtils nicknameUtils;

    public RealnameCommand(JavaPlugin plugin, BukkitScheduler scheduler, NicknameUtils nicknameUtils, Logger logger) {
        super(logger);
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.nicknameUtils = nicknameUtils;
    }

    public boolean execute(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
        scheduler.runTaskAsynchronously(plugin, () -> command(sender, args));
        return true;
    }

    public void command(CommandSender sender, String[] args){
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /realnameoffline <player>");
        } else {
            String playerName = args[0];
            List<String> nicknamedUuids = nicknameUtils.getNicknamedPlayer(playerName, sender);
            if (nicknamedUuids == null || nicknamedUuids.isEmpty()) {
                sender.sendMessage("§cNo player with that nickname found");
            } else if (nicknamedUuids.size() == 1) {
                UUID uuid = UUID.fromString(nicknamedUuids.getFirst());
                String realName = PlayerUtils.isPlayerOnline(uuid)
                        ? Bukkit.getPlayer(uuid).getName()
                        : Bukkit.getOfflinePlayer(uuid).getName();
                sender.sendMessage(playerName + " §ais §f" + realName);
            } else {
                sender.sendMessage("§aMultiple matches found for nickname §f" + playerName + "§a:");
                for (String uuid : nicknamedUuids) {
                    UUID uid = UUID.fromString(uuid);
                    String name = PlayerUtils.isPlayerOnline(uid)
                            ? Bukkit.getPlayer(uid).getName()
                            : Bukkit.getOfflinePlayer(uid).getName();
                    sender.sendMessage("§a" + name);
                }
            }
        }
    }
}
