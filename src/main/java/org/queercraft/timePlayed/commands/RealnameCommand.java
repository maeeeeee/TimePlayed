package org.queercraft.timePlayed.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.queercraft.timePlayed.utils.NicknameUtils;
import org.queercraft.timePlayed.utils.PlayerUtils;

import java.util.ArrayList;
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

    public void command(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) {
            sender.sendMessage("§cUsage: /realnameoffline <player>");
        } else {
            String playerName = args[0];
            boolean all = args.length == 2 && args[1].equalsIgnoreCase("all");
            boolean debug = args.length == 2 && args[1].equalsIgnoreCase("debug");
            List<String> partiallyMatchedUUIDs = nicknameUtils.getNicknamedPlayers(playerName);
            if (partiallyMatchedUUIDs == null || partiallyMatchedUUIDs.isEmpty()) {
                //No matches found
                sender.sendMessage("§cNo player with that nickname found");
            } else if (partiallyMatchedUUIDs.size() == 1) {
                //Exactly one partial or full match found
                UUID uuid = UUID.fromString(partiallyMatchedUUIDs.getFirst());
                String realName = PlayerUtils.isPlayerOnline(uuid) ? Bukkit.getPlayer(uuid).getName() : Bukkit.getOfflinePlayer(uuid).getName();
                NicknameUtils.NicknameEntry entry = nicknameUtils.getNicknameForUUID(uuid);
                if (entry.getPlain().equalsIgnoreCase(playerName)) {
                    //Full match
                    sender.sendMessage(entry.getColored() + " §ais §f" + realName);
                } else {
                    //Partial match
                    sender.sendMessage("§aPartial match for " + playerName + " §afound:");
                    sender.sendMessage(entry.getColored() + " §ais §f" + realName);
                }
            } else {
                //Multiple partial or full matches found
                List<UUID> onlineMatches = new ArrayList<>();
                List<UUID> offlineMatches = new ArrayList<>();
                for (String uuid : partiallyMatchedUUIDs) {
                    UUID uid = UUID.fromString(uuid);
                    if (PlayerUtils.isPlayerOnline(uid)) {
                        onlineMatches.add(uid);
                    } else {
                        offlineMatches.add(uid);
                    }
                }

                sender.sendMessage("§aShowing online matches for §f" + playerName + " §a(exact and partial matches):");
                if (!onlineMatches.isEmpty()) {

                    for (UUID onlineMatch : onlineMatches) {
                        //Print exact matches first
                        NicknameUtils.NicknameEntry entry = nicknameUtils.getNicknameForUUID(onlineMatch);
                        if (entry.getPlain().equalsIgnoreCase(playerName)) {
                            sender.sendMessage(entry.getColored() + " §ais §f" + Bukkit.getPlayer(onlineMatch).getName());
                        }
                    }
                    for (UUID onlineMatch : onlineMatches) {
                        //Then print partial matches
                        NicknameUtils.NicknameEntry entry = nicknameUtils.getNicknameForUUID(onlineMatch);
                        if (!entry.getPlain().equalsIgnoreCase(playerName)) {
                            sender.sendMessage(entry.getColored() + " §ais §f" + Bukkit.getPlayer(onlineMatch).getName());
                        }
                    }
                }else{
                    sender.sendMessage("§cNo online matches found.");
                }
                if (!offlineMatches.isEmpty()) {

                    List<UUID> exactMatches = new ArrayList<>();
                    List<UUID> partialMatches = new ArrayList<>();
                    for (UUID offlineMatch : offlineMatches) {
                        //Sort offline matches by exactness
                        if (nicknameUtils.getNicknameForUUID(offlineMatch).getPlain().equalsIgnoreCase(playerName)) {
                            exactMatches.add(offlineMatch);
                        } else {
                            partialMatches.add(offlineMatch);
                        }
                    }

                    if (all) {
                        sender.sendMessage("§aShowing offline matches for §f" + playerName + " §a(exact and partial matches):");
                    } else {
                        sender.sendMessage("§aShowing offline matches for §f" + playerName + " §a(exact matches only):");
                    }

                    if(exactMatches.isEmpty() && !all){
                        sender.sendMessage("§cNo exact offline matches found.");
                    }else{
                        //Show all exact matches
                        for (UUID exactMatch : exactMatches) {
                            sender.sendMessage(nicknameUtils.getNicknameForUUID(exactMatch).getColored() + " §ais §f" + Bukkit.getOfflinePlayer(exactMatch).getName());
                        }
                    }

                    if (all) {
                        if (args[0].length() < 3 && !partialMatches.isEmpty()) {
                            sender.sendMessage("§aYou have provided a very short search term (less than 3 characters). This will match way too many partial nicknames and is therefore not supported when using \"all\".");
                        } else {
                            for (UUID partialMatch : partialMatches) {
                                sender.sendMessage(nicknameUtils.getNicknameForUUID(partialMatch).getColored() + " §ais §f" + Bukkit.getOfflinePlayer(partialMatch).getName());
                            }
                        }
                    }
                    if (!all) {
                        sender.sendMessage(partialMatches.size() + " §apartial offline matches were found. To show them, add \"all\" behind your command.");
                    }
                }
                if (debug) {
                    sender.sendMessage("onlineMatches:\n" + onlineMatches + "\nofflineMatches:\n" + offlineMatches + "\npartialMatches:");
                    for (String uuid : partiallyMatchedUUIDs) {
                        sender.sendMessage(nicknameUtils.getNicknameForUUID(UUID.fromString(uuid)).getPlain());
                    }
                }
            }
        }
    }
}
