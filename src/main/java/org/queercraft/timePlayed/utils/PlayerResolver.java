package org.queercraft.timePlayed.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class PlayerResolver {
    private final NicknameUtils nicknameUtils;

    public PlayerResolver(NicknameUtils nicknameUtils) {
        this.nicknameUtils = nicknameUtils;
    }

    public interface ResolvedPlayerCallback {
        void handle(CommandSender sender, String name, UUID uuid);
    }

    public void resolvePlayer(CommandSender sender, String[] args, String usage, ResolvedPlayerCallback callback) {
        boolean isSelf = args.length == 0;
        if (args.length > 1) {
            sender.sendMessage("§cUsage: " + usage);
            return;
        }

        String playerName = isSelf ? sender.getName() : args[0];
        Player target = Bukkit.getPlayer(playerName);

        if (target != null) {
            callback.handle(sender, target.getName(), target.getUniqueId());
            return;
        } else if (isSelf) {
            sender.sendMessage("§cAn error occurred. If you are seeing this from console, you probably forgot to specify a player name.");
        }

        UUID playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        if (PlayerUtils.offlinePlayerExists(playerUUID)) {
            callback.handle(sender, playerName, playerUUID);
            return;
        }

        List<String> nicknamedUuids = nicknameUtils.getNicknamedPlayer(playerName, sender);
        if (nicknamedUuids == null || nicknamedUuids.isEmpty()) {
            sender.sendMessage("§cNo player with that nickname found");
        } else if (nicknamedUuids.size() == 1) {
            UUID uuid = UUID.fromString(nicknamedUuids.getFirst());
            if (PlayerUtils.isPlayerOnline(uuid)) {
                callback.handle(sender, playerName, uuid);
            } else if (PlayerUtils.offlinePlayerExists(uuid)) {
                String realName = Bukkit.getOfflinePlayer(uuid).getName();
                callback.handle(sender, realName, uuid);
            } else {
                sender.sendMessage("§aPlayer is nicknamed, but real name could not be found automatically. Try again with their real username!");
            }
        } else {
            sender.sendMessage("§aMultiple matches found for nickname §f" + playerName + "§a:");
            for (String uuid : nicknamedUuids) {
                UUID uid = UUID.fromString(uuid);
                String name = PlayerUtils.isPlayerOnline(uid)
                        ? Bukkit.getPlayer(uid).getName()
                        : Bukkit.getOfflinePlayer(uid).getName();
                sender.sendMessage("§a" + name);
            }
            sender.sendMessage("§aPlease try again with the right username from this list");
        }
    }
}
