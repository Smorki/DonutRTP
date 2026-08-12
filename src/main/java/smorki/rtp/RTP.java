/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.threadedregions.scheduler.ScheduledTask
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package smorki.rtp;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import smorki.rtp.Manager.RTPManager;
import smorki.rtp.gui.QueueGUI;
import smorki.rtp.gui.RTPGui;

public class RTP
extends JavaPlugin
implements TabCompleter {
    private RTPManager rtpManager;
    private RTPGui rtpGui;
    private QueueGUI queueGUI;
    private FileConfiguration langConfig;
    private final ConcurrentHashMap<Integer, ScheduledTask> runningTasks = new ConcurrentHashMap();
    private final AtomicInteger taskCounter = new AtomicInteger(0);

    public void onEnable() {
        boolean created;
        boolean created2;
        if (this.getDataFolder().exists() || !(created2 = this.getDataFolder().mkdirs())) {
            // empty if block
        }
        this.getLogger().info("");
        this.getLogger().info("  ╔══════════════════════════════════════════════════╗");
        this.getLogger().info("  ║                                                  ║");
        this.getLogger().info("  ║        D O N U T     R T P     v" + this.getDescription().getVersion() + "              ║");
        this.getLogger().info("  ║             engineered by smorki                 ║");
        this.getLogger().info("  ║                                                  ║");
        this.getLogger().info("  ╚══════════════════════════════════════════════════╝");
        this.getLogger().info("");
        this.saveDefaultConfig();
        this.saveDefaultLangConfig();
        File guiFolder = new File(this.getDataFolder(), "gui");
        if (guiFolder.exists() || !(created = guiFolder.mkdirs())) {
            // empty if block
        }
        this.rtpManager = new RTPManager(this);
        this.rtpGui = new RTPGui(this, this.rtpManager);
        this.queueGUI = new QueueGUI(this);
        this.getServer().getPluginManager().registerEvents((Listener)this.rtpGui, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)this.queueGUI, (Plugin)this);
        this.getCommand("rtp").setExecutor((CommandExecutor)this);
        this.getCommand("rtp").setTabCompleter((TabCompleter)this);
        PluginCommand queueCommand = this.getCommand("rtpqueue");
        if (queueCommand != null) {
            queueCommand.setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(this.rtpManager.getMessage("player-only"));
                    return true;
                }
                Player player = (Player)sender;
                if (!player.hasPermission("rtp.use")) {
                    player.sendMessage(this.rtpManager.getMessage("messages.no-permission"));
                    return true;
                }
                this.queueGUI.openQueueGUI(player);
                return true;
            });
        }
    }

    private void saveDefaultLangConfig() {
        File langFile = new File(this.getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            this.saveResource("lang.yml", false);
        }
        this.reloadLangConfig();
    }

    public void reloadLangConfig() {
        File langFile = new File(this.getDataFolder(), "lang.yml");
        this.langConfig = YamlConfiguration.loadConfiguration((File)langFile);
    }

    public FileConfiguration getLangConfig() {
        return this.langConfig;
    }

    public void runAtPlayer(Player player, Runnable task) {
        if (player.isOnline()) {
            player.getScheduler().run((Plugin)this, scheduledTask -> task.run(), null);
        }
    }

    public Object runGlobalTimer(Runnable task, long delay, long period) {
        int taskId = this.taskCounter.incrementAndGet();
        ScheduledTask scheduledTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate((Plugin)this, scheduledTask1 -> task.run(), Math.max(1L, delay), period);
        this.runningTasks.put(taskId, scheduledTask);
        return taskId;
    }

    public void cancelTask(Object task) {
        if (task == null) {
            return;
        }
        if (task instanceof Integer) {
            Integer taskId = (Integer)task;
            ScheduledTask scheduledTask = this.runningTasks.remove(taskId);
            if (scheduledTask != null) {
                scheduledTask.cancel();
            }
        } else if (task instanceof ScheduledTask) {
            ((ScheduledTask)task).cancel();
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("rtp")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("rtp.admin")) {
                    sender.sendMessage(this.rtpManager.getMessage("messages.no-permission"));
                    return true;
                }
                this.reloadConfig();
                this.reloadLangConfig();
                this.rtpGui.reloadGuiConfig();
                this.queueGUI.reloadQueueConfig();
                this.rtpManager.reloadConfig();
                if (sender instanceof Player) {
                    this.rtpManager.playSound((Player)sender, "reload");
                }
                sender.sendMessage(this.rtpManager.getActionBar("reload"));
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(this.rtpManager.getMessage("messages.player-only"));
                return true;
            }
            Player player = (Player)sender;
            if (!player.hasPermission("rtp.use")) {
                sender.sendMessage(this.rtpManager.getMessage("messages.no-permission"));
                return true;
            }
            this.rtpGui.openRTPGui(player);
            return true;
        }
        return false;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> completions = new ArrayList<String>();
        if (command.getName().equalsIgnoreCase("rtp") && args.length == 1 && sender.hasPermission("rtp.admin") && "reload".startsWith(args[0].toLowerCase())) {
            completions.add("reload");
        }
        return completions;
    }

    public void onDisable() {
        for (ScheduledTask task : this.runningTasks.values()) {
            if (task == null) continue;
            task.cancel();
        }
        this.runningTasks.clear();
        if (this.queueGUI != null) {
            for (Player player : this.getServer().getOnlinePlayers()) {
                this.queueGUI.playerQuit(player);
            }
        }
    }

    public RTPManager getRtpManager() {
        return this.rtpManager;
    }

    public RTPGui getRtpGui() {
        return this.rtpGui;
    }

    public QueueGUI getQueueGUI() {
        return this.queueGUI;
    }
}

