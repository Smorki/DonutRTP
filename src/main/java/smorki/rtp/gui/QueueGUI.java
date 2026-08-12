/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.threadedregions.scheduler.ScheduledTask
 *  net.md_5.bungee.api.ChatMessageType
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scoreboard.Scoreboard
 *  org.bukkit.scoreboard.ScoreboardManager
 *  org.bukkit.scoreboard.Team
 *  org.bukkit.scoreboard.Team$Option
 *  org.bukkit.scoreboard.Team$OptionStatus
 */
package smorki.rtp.gui;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import smorki.rtp.Hex;
import smorki.rtp.RTP;

public class QueueGUI
implements Listener {
    private final RTP plugin;
    private FileConfiguration queueConfig;
    private final Map<String, List<UUID>> queues;
    private final Map<UUID, String> playerQueue;
    private final Map<String, Object> activeTasks;
    private final Map<String, Integer> countdowns;
    private final Map<UUID, Object> glowingTasks;
    private final Map<UUID, QueueStatus> playerStatus;
    private final Map<UUID, String> pendingTeleports;
    private final Map<UUID, Object> cooldownTasks;

    public QueueGUI(RTP plugin) {
        this.plugin = plugin;
        this.queues = new ConcurrentHashMap<String, List<UUID>>();
        this.playerQueue = new ConcurrentHashMap<UUID, String>();
        this.activeTasks = new ConcurrentHashMap<String, Object>();
        this.countdowns = new ConcurrentHashMap<String, Integer>();
        this.glowingTasks = new ConcurrentHashMap<UUID, Object>();
        this.playerStatus = new ConcurrentHashMap<UUID, QueueStatus>();
        this.pendingTeleports = new ConcurrentHashMap<UUID, String>();
        this.cooldownTasks = new ConcurrentHashMap<UUID, Object>();
        this.loadQueueConfig();
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate((Plugin)plugin, task -> this.cleanupEmptyQueues(), 600L, 600L);
    }

    private void loadQueueConfig() {
        File queueFile;
        boolean created;
        File guiFolder = new File(this.plugin.getDataFolder(), "gui");
        if (guiFolder.exists() || !(created = guiFolder.mkdirs())) {
            // empty if block
        }
        if ((queueFile = new File(guiFolder, "queue.yml")).exists()) {
            this.queueConfig = YamlConfiguration.loadConfiguration((File)queueFile);
            this.initializeQueues();
        } else {
            this.createDefaultQueueConfig(queueFile);
            this.queueConfig = YamlConfiguration.loadConfiguration((File)queueFile);
            this.initializeQueues();
        }
    }

    private void createDefaultQueueConfig(File queueFile) {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("title", (Object)"&8\u0280\u1d1b\u1d18\u01eb\u1d1c\u1d07\u1d1c\u1d07");
            config.set("rows", (Object)3);
            config.set("worlds.overworld.slot", (Object)11);
            config.set("worlds.overworld.world", (Object)"world");
            config.set("worlds.overworld.material", (Object)"GRASS_BLOCK");
            config.set("worlds.overworld.name", (Object)"&#00FF89\u1d0f\u1d20\u1d07\u0280\u1d21\u1d0f\u0280\u029f\u1d05");
            config.set("worlds.overworld.lore", Arrays.asList("", "&bInformation:", "&fPlay 1v1s against other players", "&fIn different &#00FF89worlds.", "", "&#00FF89&l\u23fa &fPlayers in queue: &#00FF89%queue-amount%/2", "", "&#E0E319&l\u279f &#E0E319&l&nCLICK&#E0E319 to queue"));
            config.set("worlds.nether.slot", (Object)13);
            config.set("worlds.nether.world", (Object)"world_nether");
            config.set("worlds.nether.material", (Object)"NETHERRACK");
            config.set("worlds.nether.name", (Object)"&#00FF89\u0274\u1d07\u1d1b\u029c\u1d07\u0280");
            config.set("worlds.nether.lore", Arrays.asList("", "&bInformation:", "&fPlay 1v1s against other players", "&fIn different &#00FF89worlds.", "", "&#00FF89&l\u23fa &fPlayers in queue: &#00FF89%queue-amount%/2", "", "&#E0E319&l\u279f &#E0E319&l&nCLICK&#E0E319 to queue"));
            config.set("worlds.end.slot", (Object)15);
            config.set("worlds.end.world", (Object)"world_the_end");
            config.set("worlds.end.material", (Object)"END_STONE");
            config.set("worlds.end.name", (Object)"&#00FF89\u1d07\u0274\u1d05");
            config.set("worlds.end.lore", Arrays.asList("", "&bInformation:", "&fPlay 1v1s against other players", "&fIn different &#00FF89worlds.", "", "&#00FF89&l\u23fa &fPlayers in queue: &#00FF89%queue-amount%/2", "", "&#E0E319&l\u279f &#E0E319&l&nCLICK&#E0E319 to queue"));
            config.set("queue-settings.players-per-match", (Object)2);
            config.set("queue-settings.countdown-seconds", (Object)10);
            config.set("queue-settings.max-wait-time-seconds", (Object)120);
            config.set("queue-settings.arena-distance", (Object)10);
            config.set("queue-settings.glowing-duration", (Object)6);
            config.set("queue-settings.post-match-cooldown", (Object)5);
            config.save(queueFile);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void initializeQueues() {
        ConfigurationSection cs;
        this.queues.clear();
        this.countdowns.clear();
        if (this.queueConfig != null && this.queueConfig.contains("worlds") && (cs = this.queueConfig.getConfigurationSection("worlds")) != null) {
            for (String worldKey : cs.getKeys(false)) {
                String worldName = this.queueConfig.getString("worlds." + worldKey + ".world");
                if (worldName == null) continue;
                this.queues.put(worldName, new ArrayList());
                this.countdowns.put(worldName, this.queueConfig.getInt("queue-settings.countdown-seconds", 10));
            }
        }
    }

    public void openQueueGUI(Player player) {
        try {
            ConfigurationSection cs;
            if (this.playerStatus.getOrDefault(player.getUniqueId(), QueueStatus.IN_QUEUE) == QueueStatus.COOLDOWN) {
                player.sendMessage(this.getMessage("messages.rtp-cooldown-message"));
                return;
            }
            if (this.playerStatus.getOrDefault(player.getUniqueId(), QueueStatus.IN_QUEUE) == QueueStatus.IN_MATCH) {
                player.sendMessage(this.getMessage("messages.rtp-already-in-match"));
                return;
            }
            String title = Hex.translateAllColorCodes(this.queueConfig.getString("title", "&8\u0280\u1d1b\u1d18\u01eb\u1d1c\u1d07\u1d1c\u1d07"));
            int rows = this.queueConfig.getInt("rows", 3);
            Inventory gui = Bukkit.createInventory(null, (int)(rows * 9), (String)title);
            if (this.queueConfig != null && this.queueConfig.contains("worlds") && (cs = this.queueConfig.getConfigurationSection("worlds")) != null) {
                for (String worldKey : cs.getKeys(false)) {
                    this.setupQueueItem(gui, worldKey);
                }
            }
            player.openInventory(gui);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void setupQueueItem(Inventory gui, String worldKey) {
        ItemStack item;
        ItemMeta meta;
        String path = "worlds." + worldKey;
        int slot = this.queueConfig.getInt(path + ".slot");
        String worldName = this.queueConfig.getString(path + ".world");
        Material material = Material.getMaterial((String)this.queueConfig.getString(path + ".material", "GRASS_BLOCK"));
        String itemName = Hex.translateAllColorCodes(this.queueConfig.getString(path + ".name", ""));
        if (material == null) {
            material = Material.GRASS_BLOCK;
        }
        if ((meta = (item = new ItemStack(material)).getItemMeta()) == null) {
            return;
        }
        meta.setDisplayName(itemName);
        ArrayList<String> lore = new ArrayList<String>();
        List configLore = this.queueConfig.getStringList(path + ".lore");
        for (Object obj : configLore) {
            String line = (String) obj;
            if (line.contains("%queue-amount%")) {
                int queueSize = this.getQueueSize(worldName);
                int requiredPlayers = this.queueConfig.getInt("queue-settings.players-per-match", 2);
                line = line.replace("%queue-amount%", String.valueOf(queueSize));
                line = line.replace("/2", "/" + requiredPlayers);
            }
            lore.add(Hex.translateAllColorCodes(line));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        if (slot >= 0 && slot < gui.getSize()) {
            gui.setItem(slot, item);
        }
    }

    private int getQueueSize(String worldName) {
        List<UUID> queue = this.queues.get(worldName);
        return queue != null ? queue.size() : 0;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String expectedTitle;
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getWhoClicked();
        String title = ChatColor.stripColor((String)event.getView().getTitle());
        if (!title.equals(expectedTitle = ChatColor.stripColor((String)Hex.translateAllColorCodes(this.queueConfig.getString("title", "&8\u0280\u1d1b\u1d18\u01eb\u1d1c\u1d07\u1d1c\u1d07"))))) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }
        String worldKey = this.getWorldKeyFromSlot(slot);
        if (worldKey == null) {
            return;
        }
        this.handleQueueClick(player, worldKey);
    }

    private String getWorldKeyFromSlot(int slot) {
        if (this.queueConfig == null || !this.queueConfig.contains("worlds")) {
            return null;
        }
        ConfigurationSection cs = this.queueConfig.getConfigurationSection("worlds");
        if (cs == null) {
            return null;
        }
        for (String worldKey : cs.getKeys(false)) {
            if (this.queueConfig.getInt("worlds." + worldKey + ".slot") != slot) continue;
            return worldKey;
        }
        return null;
    }

    private void handleQueueClick(Player player, String worldKey) {
        String worldName = this.queueConfig.getString("worlds." + worldKey + ".world");
        if (worldName == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        QueueStatus status = this.playerStatus.getOrDefault(playerId, QueueStatus.IN_QUEUE);
        if (status == QueueStatus.MATCH_FOUND || status == QueueStatus.TELEPORTING) {
            player.sendMessage(this.getMessage("messages.rtp-cannot-leave-match-found"));
            player.closeInventory();
            return;
        }
        if (status == QueueStatus.COOLDOWN) {
            player.sendMessage(this.getMessage("messages.rtp-cooldown-message"));
            player.closeInventory();
            return;
        }
        if (this.playerQueue.containsKey(playerId)) {
            String currentQueue = this.playerQueue.get(playerId);
            if (currentQueue.equals(worldName)) {
                this.leaveQueue(player, worldName);
            } else {
                this.leaveQueue(player, currentQueue);
                this.joinQueue(player, worldName);
            }
        } else {
            this.joinQueue(player, worldName);
        }
        player.closeInventory();
    }

    private void joinQueue(Player player, String worldName) {
        List queue;
        UUID playerId = player.getUniqueId();
        if (this.playerStatus.getOrDefault(playerId, QueueStatus.IN_QUEUE) == QueueStatus.COOLDOWN) {
            player.sendMessage(this.getMessage("messages.rtp-cooldown-message"));
            return;
        }
        if (this.playerStatus.getOrDefault(playerId, QueueStatus.IN_QUEUE) == QueueStatus.IN_MATCH) {
            player.sendMessage(this.getMessage("messages.rtp-already-in-match"));
            return;
        }
        if (this.playerQueue.containsKey(playerId)) {
            String oldWorld = this.playerQueue.get(playerId);
            this.leaveQueue(player, oldWorld);
        }
        if (!(queue = this.queues.computeIfAbsent(worldName, k -> new ArrayList())).contains(playerId)) {
            queue.add(playerId);
            this.playerQueue.put(playerId, worldName);
            this.playerStatus.put(playerId, QueueStatus.IN_QUEUE);
            this.sendJoinMessages(player, queue.size());
            if (queue.size() == 1) {
                this.broadcastFirstPlayerJoin(player);
            }
            this.checkAndStartMatchmaking(worldName);
        }
    }

    private void sendJoinMessages(Player player, int queueSize) {
        int requiredPlayers = this.queueConfig.getInt("queue-settings.players-per-match", 2);
        int playersNeeded = requiredPlayers - queueSize;
        String actionBar = this.getActionBar("rtp-queue-action-bar").replace("%players%", String.valueOf(playersNeeded)).replace("1", String.valueOf(playersNeeded)).replace("(/rtpqueue)", "(/rtpqueue)");
        this.sendActionBar(player, actionBar);
        String title = this.getMessage("messages.rtp-join-queue-title");
        String subTitle = this.getMessage("messages.rtp-join-queue-sub-title");
        player.sendTitle(title, subTitle, 10, 40, 10);
        player.sendMessage(this.getMessage("messages.rtp-join-queue-message"));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
    }

    private void broadcastFirstPlayerJoin(Player player) {
        List broadcastMessages = this.plugin.getLangConfig().getStringList("messages.rtp-first-join-queue-broadcast");
        for (Object obj : broadcastMessages) {
            String message = (String) obj;
            String formattedMessage = Hex.translateAllColorCodes(message.replace("%player%", player.getName()).replace("/rtpqueue", "/rtpqueue"));
            Bukkit.broadcastMessage((String)formattedMessage);
        }
    }

    private void leaveQueue(Player player, String worldName) {
        UUID playerId = player.getUniqueId();
        QueueStatus status = this.playerStatus.getOrDefault(playerId, QueueStatus.IN_QUEUE);
        if (status == QueueStatus.MATCH_FOUND || status == QueueStatus.TELEPORTING) {
            player.sendMessage(this.getMessage("messages.rtp-cannot-leave-match-found"));
            return;
        }
        if (status == QueueStatus.IN_MATCH) {
            player.sendMessage(this.getMessage("messages.rtp-cannot-leave-in-match"));
            return;
        }
        List<UUID> queue = this.queues.get(worldName);
        if (queue != null) {
            queue.remove(playerId);
            if (queue.isEmpty() && this.activeTasks.containsKey(worldName)) {
                Object task = this.activeTasks.remove(worldName);
                if (task != null) {
                    this.cancelTask(task);
                }
                this.countdowns.remove(worldName);
            }
        }
        this.playerQueue.remove(playerId);
        this.playerStatus.remove(playerId);
        this.sendQuitMessages(player);
    }

    private void cancelTask(Object task) {
        if (task instanceof ScheduledTask) {
            ((ScheduledTask)task).cancel();
        }
    }

    private void sendQuitMessages(Player player) {
        this.sendActionBar(player, this.getActionBar("rtp-quit-queue-action-bar"));
        String title = this.getMessage("messages.rtp-quit-queue-title");
        String subTitle = this.getMessage("messages.rtp-quit-queue-sub-title");
        player.sendTitle(title, subTitle, 10, 40, 10);
        player.sendMessage(this.getMessage("messages.rtp-quit-queue-message"));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
    }

    private void checkAndStartMatchmaking(String worldName) {
        List<UUID> queue = this.queues.get(worldName);
        if (queue == null) {
            return;
        }
        int requiredPlayers = this.queueConfig.getInt("queue-settings.players-per-match", 2);
        if (queue.size() >= requiredPlayers && !this.activeTasks.containsKey(worldName)) {
            this.startCountdown(worldName);
        }
    }

    private void startCountdown(String worldName) {
        int initialCountdown = this.queueConfig.getInt("queue-settings.countdown-seconds", 10);
        this.countdowns.put(worldName, initialCountdown);
        String finalWorldName = worldName;
        ScheduledTask task = this.plugin.getServer().getGlobalRegionScheduler().runAtFixedRate((Plugin)this.plugin, scheduledTask -> {
            List<UUID> queue = this.queues.get(finalWorldName);
            if (queue == null || queue.size() < 2) {
                this.stopCountdown(finalWorldName);
                return;
            }
            int currentCountdown = this.countdowns.getOrDefault(finalWorldName, initialCountdown);
            if (currentCountdown <= 0) {
                for (UUID playerId : queue) {
                    this.playerStatus.put(playerId, QueueStatus.MATCH_FOUND);
                    this.pendingTeleports.put(playerId, finalWorldName);
                }
                this.plugin.getServer().getGlobalRegionScheduler().execute((Plugin)this.plugin, () -> this.findArenaAndStartMatch(finalWorldName));
                this.stopCountdown(finalWorldName);
                return;
            }
            for (UUID playerId : queue) {
                Player player = Bukkit.getPlayer((UUID)playerId);
                if (player == null || !player.isOnline()) continue;
                String message = this.getMessage("messages.rtp-queue-countdown-message").replace("%seconds%", String.valueOf(currentCountdown));
                player.sendMessage(message);
                String actionBar = this.getActionBar("rtp-queue-countdown-action-bar").replace("%seconds%", String.valueOf(currentCountdown));
                this.sendActionBar(player, actionBar);
                if (currentCountdown > 3) continue;
                String title = this.getMessage("messages.rtp-queue-countdown-title");
                String subTitle = this.getMessage("messages.rtp-queue-countdown-sub-title").replace("%seconds%", String.valueOf(currentCountdown));
                player.sendTitle(title, subTitle, 0, 20, 0);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            }
            this.countdowns.put(finalWorldName, currentCountdown - 1);
        }, 1L, 20L);
        this.activeTasks.put(worldName, task);
    }

    private void stopCountdown(String worldName) {
        Object task = this.activeTasks.remove(worldName);
        if (task != null) {
            this.cancelTask(task);
        }
        this.countdowns.remove(worldName);
    }

    private void findArenaAndStartMatch(String worldName) {
        List<UUID> queue = this.queues.get(worldName);
        if (queue == null || queue.size() < 2) {
            return;
        }
        World world = Bukkit.getWorld((String)worldName);
        if (world == null) {
            for (UUID playerId : queue) {
                this.playerStatus.remove(playerId);
                this.pendingTeleports.remove(playerId);
            }
            return;
        }
        this.plugin.getRtpManager().findSafeLocationAsync(world).thenAccept(player1Location -> {
            if (player1Location == null) {
                for (UUID playerId : queue) {
                    Player player = Bukkit.getPlayer((UUID)playerId);
                    if (player == null) continue;
                    this.playerStatus.remove(playerId);
                    this.pendingTeleports.remove(playerId);
                }
                return;
            }
            int arenaDistance = this.queueConfig.getInt("queue-settings.arena-distance", 10);
            Location player2Location = this.findSafeLocationNearby(world, (Location)player1Location, arenaDistance);
            if (player2Location == null) {
                for (UUID playerId : queue) {
                    Player player = Bukkit.getPlayer((UUID)playerId);
                    if (player == null) continue;
                    this.playerStatus.remove(playerId);
                    this.pendingTeleports.remove(playerId);
                }
                return;
            }
            ArrayList<Player> matchPlayers = new ArrayList<Player>();
            for (int i = 0; i < Math.min(2, queue.size()); ++i) {
                Player player = Bukkit.getPlayer((UUID)((UUID)queue.get(i)));
                if (player == null || !player.isOnline()) continue;
                matchPlayers.add(player);
            }
            if (matchPlayers.size() < 2) {
                for (UUID playerId : queue) {
                    this.playerStatus.remove(playerId);
                    this.pendingTeleports.remove(playerId);
                }
                return;
            }
            Location finalPlayer1Loc = player1Location;
            Location finalPlayer2Loc = player2Location;
            ArrayList finalMatchPlayers = new ArrayList(matchPlayers);
            String finalWorldName = worldName;
            for (Object obj : finalMatchPlayers) {
                Player player = (Player) obj;
                player.getScheduler().run((Plugin)this.plugin, scheduledTask -> this.teleportPlayersToArena(finalMatchPlayers, finalPlayer1Loc, finalPlayer2Loc, finalWorldName), null);
            }
        });
    }

    private Location findSafeLocationNearby(World world, Location center, int distance) {
        Random random = new Random();
        for (int i = 0; i < 10; ++i) {
            Location location;
            int z;
            int offsetX = random.nextInt(distance * 2) - distance;
            int offsetZ = random.nextInt(distance * 2) - distance;
            int x = center.getBlockX() + offsetX;
            int y = world.getHighestBlockYAt(x, z = center.getBlockZ() + offsetZ) + 1;
            if (y <= world.getMinHeight() || y >= world.getMaxHeight() || !this.isLocationSafe(location = new Location(world, (double)x + 0.5, (double)y, (double)z + 0.5))) continue;
            return location;
        }
        return null;
    }

    private boolean isLocationSafe(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        Material blockBelow = world.getBlockAt(x, y - 1, z).getType();
        Material blockAtFeet = world.getBlockAt(x, y, z).getType();
        Material blockAtHead = world.getBlockAt(x, y + 1, z).getType();
        return blockBelow.isSolid() && !blockAtFeet.isSolid() && !blockAtHead.isSolid() && blockBelow != Material.LAVA && blockBelow != Material.WATER && blockAtFeet != Material.LAVA && blockAtFeet != Material.WATER && blockAtHead != Material.LAVA && blockAtHead != Material.WATER;
    }

    private void teleportPlayersToArena(List<Player> players, Location loc1, Location loc2, String worldName) {
        Player player1 = players.get(0);
        Player player2 = players.get(1);
        for (Player player : players) {
            this.playerStatus.put(player.getUniqueId(), QueueStatus.TELEPORTING);
            this.pendingTeleports.put(player.getUniqueId(), worldName);
        }
        loc1.setDirection(loc2.clone().subtract(loc1).toVector());
        loc2.setDirection(loc1.clone().subtract(loc2).toVector());
        player1.teleportAsync(loc1);
        player2.teleportAsync(loc2);
        Player finalPlayer1 = player1;
        Player finalPlayer2 = player2;
        String finalWorldName = worldName;
        ArrayList<Player> finalPlayers = new ArrayList<Player>(players);
        if (!finalPlayers.isEmpty()) {
            ((Player)finalPlayers.get(0)).getScheduler().runDelayed((Plugin)this.plugin, scheduledTask -> {
                for (Player player : finalPlayers) {
                    this.playerStatus.put(player.getUniqueId(), QueueStatus.IN_MATCH);
                    this.leaveQueueInternal(player, finalWorldName);
                    this.startPostMatchCooldown(player);
                }
                this.applyGlowingEffect(finalPlayer1);
                this.applyGlowingEffect(finalPlayer2);
                for (Player player : finalPlayers) {
                    String title = this.getMessage("messages.rtp-queue-teleporting-title");
                    String subTitle = this.getMessage("messages.rtp-queue-teleporting-sub-title");
                    player.sendTitle(title, subTitle, 10, 40, 10);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
            }, null, 2L);
        }
    }

    private void leaveQueueInternal(Player player, String worldName) {
        UUID playerId = player.getUniqueId();
        List<UUID> queue = this.queues.get(worldName);
        if (queue != null) {
            queue.remove(playerId);
            if (queue.isEmpty() && this.activeTasks.containsKey(worldName)) {
                Object task = this.activeTasks.remove(worldName);
                if (task != null) {
                    this.cancelTask(task);
                }
                this.countdowns.remove(worldName);
            }
        }
        this.playerQueue.remove(playerId);
        this.pendingTeleports.remove(playerId);
    }

    private void startPostMatchCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        this.playerStatus.put(playerId, QueueStatus.COOLDOWN);
        int cooldownSeconds = this.queueConfig.getInt("queue-settings.post-match-cooldown", 5);
        ScheduledTask task = player.getScheduler().runDelayed((Plugin)this.plugin, scheduledTask -> {
            this.playerStatus.remove(playerId);
            this.cooldownTasks.remove(playerId);
        }, null, (long)cooldownSeconds * 20L);
        this.cooldownTasks.put(playerId, task);
    }

    private void applyGlowingEffect(Player player) {
        int glowingDuration = this.queueConfig.getInt("queue-settings.glowing-duration", 6);
        UUID pid = player.getUniqueId();
        Object existing = this.glowingTasks.remove(pid);
        if (existing != null) {
            this.cancelTask(existing);
        }
        if (glowingDuration <= 0) {
            if (player.isOnline()) {
                player.setGlowing(false);
                try {
                    PotionEffectType glow = PotionEffectType.GLOWING;
                    if (glow != null) {
                        player.removePotionEffect(glow);
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            return;
        }
        player.getScheduler().run((Plugin)this.plugin, scheduledTask -> {
            try {
                player.setGlowing(true);
                try {
                    PotionEffectType glow = PotionEffectType.GLOWING;
                    if (glow != null) {
                        player.addPotionEffect(new PotionEffect(glow, glowingDuration * 20, 0, true, false, true));
                    }
                }
                catch (Exception glow) {
                    // empty catch block
                }
                player.getScheduler().runDelayed((Plugin)this.plugin, removeTask -> {
                    Object t = this.glowingTasks.remove(pid);
                    if (t != null) {
                        this.cancelTask(t);
                    }
                    if (player.isOnline()) {
                        player.setGlowing(false);
                        try {
                            PotionEffectType glow = PotionEffectType.GLOWING;
                            if (glow != null) {
                                player.removePotionEffect(glow);
                            }
                        }
                        catch (Exception exception) {
                            // empty catch block
                        }
                    }
                }, null, (long)glowingDuration * 20L);
            }
            catch (Exception e) {
                try {
                    this.applyGlowingViaScoreboard(player, glowingDuration);
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        }, null);
    }

    private void applyGlowingViaScoreboard(Player player, int durationSeconds) {
        Player finalPlayer = player;
        try {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            if (manager == null) {
                return;
            }
            Scoreboard board = manager.getMainScoreboard();
            Team team = board.getTeam("RTP_Glowing");
            if (team == null) {
                team = board.registerNewTeam("RTP_Glowing");
            }
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            team.setColor(ChatColor.YELLOW);
            team.addEntry(finalPlayer.getName());
            Team teamRef = team;
            player.getScheduler().runDelayed((Plugin)this.plugin, scheduledTask -> {
                if (finalPlayer.isOnline()) {
                    teamRef.removeEntry(finalPlayer.getName());
                }
                if (teamRef.getEntries().isEmpty()) {
                    teamRef.unregister();
                }
            }, null, (long)durationSeconds * 20L);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        block6: {
            Player player = event.getEntity();
            UUID playerId = player.getUniqueId();
            Object gt = this.glowingTasks.remove(playerId);
            if (gt != null) {
                this.cancelTask(gt);
            }
            try {
                if (!player.isOnline()) break block6;
                player.setGlowing(false);
                try {
                    PotionEffectType glow = PotionEffectType.GLOWING;
                    if (glow != null) {
                        player.removePotionEffect(glow);
                    }
                }
                catch (Exception exception) {}
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private void cleanupEmptyQueues() {
        Iterator<Map.Entry<String, List<UUID>>> iterator = this.queues.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<UUID>> entry = iterator.next();
            if (!entry.getValue().isEmpty()) continue;
            iterator.remove();
            this.countdowns.remove(entry.getKey());
            Object task = this.activeTasks.remove(entry.getKey());
            if (task == null) continue;
            this.cancelTask(task);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.playerQuit(event.getPlayer());
    }

    public void playerQuit(Player player) {
        block11: {
            Object gt;
            UUID playerId = player.getUniqueId();
            QueueStatus status = this.playerStatus.get(playerId);
            if (status == QueueStatus.MATCH_FOUND || status == QueueStatus.TELEPORTING || status == QueueStatus.IN_MATCH) {
                int penaltyCooldown = 30;
                this.playerStatus.put(playerId, QueueStatus.COOLDOWN);
                ScheduledTask task = player.getScheduler().runDelayed((Plugin)this.plugin, scheduledTask -> {
                    this.playerStatus.remove(playerId);
                    this.cooldownTasks.remove(playerId);
                }, null, (long)penaltyCooldown * 20L);
                this.cooldownTasks.put(playerId, task);
                for (String worldName : this.pendingTeleports.values()) {
                    List<UUID> queue = this.queues.get(worldName);
                    if (queue == null) continue;
                    for (UUID otherId : queue) {
                        Player otherPlayer;
                        if (otherId.equals(playerId) || (otherPlayer = Bukkit.getPlayer((UUID)otherId)) == null) continue;
                        otherPlayer.sendMessage(this.getMessage("messages.rtp-opponent-left"));
                    }
                }
            }
            if (this.playerQueue.containsKey(playerId)) {
                String worldName = this.playerQueue.get(playerId);
                this.leaveQueueInternal(player, worldName);
            }
            this.playerStatus.remove(playerId);
            this.pendingTeleports.remove(playerId);
            Object ct = this.cooldownTasks.remove(playerId);
            if (ct != null) {
                this.cancelTask(ct);
            }
            if ((gt = this.glowingTasks.remove(playerId)) != null) {
                this.cancelTask(gt);
            }
            try {
                if (!player.isOnline()) break block11;
                player.setGlowing(false);
                try {
                    PotionEffectType glow = PotionEffectType.GLOWING;
                    if (glow != null) {
                        player.removePotionEffect(glow);
                    }
                }
                catch (Exception exception) {}
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public void endMatchForPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        this.playerStatus.remove(playerId);
        this.pendingTeleports.remove(playerId);
        Object ct = this.cooldownTasks.remove(playerId);
        if (ct != null) {
            this.cancelTask(ct);
        }
        this.startPostMatchCooldown(player);
    }

    private void sendActionBar(Player player, String message) {
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText((String)Hex.translateAllColorCodes(message)));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private String getMessage(String path) {
        String message = this.plugin.getLangConfig().getString(path, "&c" + path);
        return Hex.translateAllColorCodes(message);
    }

    private String getActionBar(String path) {
        String message = this.plugin.getLangConfig().getString("action-bars." + path, this.plugin.getLangConfig().getString("messages." + path, "&c" + path));
        return Hex.translateAllColorCodes(message);
    }

    public void reloadQueueConfig() {
        this.loadQueueConfig();
    }

    public QueueStatus getPlayerStatus(UUID playerId) {
        return this.playerStatus.getOrDefault(playerId, QueueStatus.IN_QUEUE);
    }

    public boolean isPlayerInMatch(UUID playerId) {
        QueueStatus status = this.playerStatus.get(playerId);
        return status == QueueStatus.IN_MATCH || status == QueueStatus.MATCH_FOUND || status == QueueStatus.TELEPORTING;
    }

    public static enum QueueStatus {
        IN_QUEUE,
        MATCH_FOUND,
        TELEPORTING,
        IN_MATCH,
        COOLDOWN;

    }
}

