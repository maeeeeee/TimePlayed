package org.queercraft.timePlayed.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.queercraft.timePlayed.QueryAPIAccessor;
import org.queercraft.timePlayed.utils.ReportUtils;

import java.util.logging.Logger;

public class TimeplayedCommand extends SafeCommandExecutor{

    private final JavaPlugin plugin;
    private final BukkitScheduler scheduler;
    private final ReportUtils reportUtils;
    private final QueryAPIAccessor queryAPI;

    public TimeplayedCommand(JavaPlugin plugin, BukkitScheduler scheduler, ReportUtils reportUtils, QueryAPIAccessor queryAPI, Logger logger) {
        super(logger);
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.queryAPI = queryAPI;
        this.reportUtils = reportUtils;
    }

    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        scheduler.runTaskAsynchronously(plugin, () -> command(sender, args));
        return true;
    }

    public void command(CommandSender sender, String[] args){
        String arg = args.length > 0 ? args[0].toLowerCase() : "";

        switch (arg) {
            case "":
                sender.sendMessage("Usage: /timeplayed <command>");
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            case "generatereport":
                handlegenerateReportCommand(sender, args, plugin.getConfig().getBoolean("features.generateReports"));
                break;
            default:
                break;
        }
    }
    public void handleReloadCommand(CommandSender sender){
        if (sender.hasPermission("timeplayed.reload")) {
            plugin.reloadConfig();
            sender.sendMessage("The config has been reloaded.");
        } else {
            sender.sendMessage("You do not have permission to reload the config.");
        }
    }
    public void handlegenerateReportCommand(CommandSender sender, String[] args, boolean enabled) {
        if (sender.hasPermission("timeplayed.generatereport")) {
            if (enabled){
                reportUtils.generatePlaytimeReport(plugin, args[1],sender,queryAPI);
            }else sender.sendMessage("Feature is not enabled.");
        } else {
            sender.sendMessage("You do not have permission to use this command.");
        }
    }
}
