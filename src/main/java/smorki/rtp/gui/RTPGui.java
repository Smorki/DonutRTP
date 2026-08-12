/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatMessageType
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package smorki.rtp.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import smorki.rtp.Hex;
import smorki.rtp.Manager.RTPManager;
import smorki.rtp.RTP;

public class RTPGui
implements Listener {
    private final RTP plugin;
    private final RTPManager rtpManager;
    private FileConfiguration guiConfig;
    private final Map<UUID, Object> playerTasks;
    private final Map<UUID, Location> startLocations;
    private final Set<UUID> teleportingPlayers;

    public RTPGui(RTP plugin, RTPManager rtpManager) {
        this.plugin = plugin;
        this.rtpManager = rtpManager;
        this.playerTasks = new ConcurrentHashMap<UUID, Object>();
        this.startLocations = new ConcurrentHashMap<UUID, Location>();
        this.teleportingPlayers = ConcurrentHashMap.newKeySet();
        this.loadGuiConfig();
    }

    private void loadGuiConfig() {
        File guiFile;
        boolean created;
        File guiFolder = new File(this.plugin.getDataFolder(), "gui");
        if (guiFolder.exists() || !(created = guiFolder.mkdirs())) {
            // empty if block
        }
        if ((guiFile = new File(guiFolder, "rtp.yml")).exists()) {
            this.guiConfig = YamlConfiguration.loadConfiguration((File)guiFile);
        } else {
            this.createDefaultGuiConfig(guiFile);
            this.guiConfig = YamlConfiguration.loadConfiguration((File)guiFile);
        }
    }

    private void createDefaultGuiConfig(File guiFile) {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("title", (Object)"&8\u0280\u1d00\u0274\u1d05\u1d0f\u1d0d \u1d1b\u1d07\u029f\u1d07\u1d18\u1d0f\u0280\u1d1b");
            config.set("rows", (Object)3);
            config.set("worlds.overworld.slot", (Object)11);
            config.set("worlds.overworld.world", (Object)"world");
            config.set("worlds.overworld.material", (Object)"GRASS_BLOCK");
            config.set("worlds.overworld.name", (Object)"&#00FF89\u1d0f\u1d20\u1d07\u0280\u1d21\u1d0f\u0280\u029f\u1d05");
            config.set("worlds.overworld.lore", Arrays.asList("&fClick to random teleport", "", "&7Player (&#00A0FC%players%&7)"));
            config.set("worlds.overworld.permission", (Object)"");
            config.set("worlds.nether.slot", (Object)13);
            config.set("worlds.nether.world", (Object)"world_nether");
            config.set("worlds.nether.material", (Object)"NETHERRACK");
            config.set("worlds.nether.name", (Object)"&#00FF89\u0274\u1d07\u1d1b\u029c\u1d07\u0280");
            config.set("worlds.nether.lore", Arrays.asList("&fClick to random teleport", "", "&7Player (&#00A0FC%players%&7)"));
            config.set("worlds.nether.permission", (Object)"");
            config.set("worlds.end.slot", (Object)15);
            config.set("worlds.end.world", (Object)"world_the_end");
            config.set("worlds.end.material", (Object)"END_STONE");
            config.set("worlds.end.name", (Object)"&#00FF89\u1d07\u0274\u1d05");
            config.set("worlds.end.lore", Arrays.asList("&fClick to random teleport", "", "&7Player (&#00A0FC%players%&7)"));
            config.set("worlds.end.permission", (Object)"");
            config.save(guiFile);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public void openRTPGui(Player player) {
        if (this.teleportingPlayers.contains(player.getUniqueId())) {
            player.sendMessage(this.rtpManager.getMessage("teleport-on-cooldown-message", "%wait_time%", "kurz"));
            return;
        }
        try {
            String title = Hex.translateAllColorCodes(this.guiConfig.getString("title", "&8\u0280\u1d00\u0274\u1d05\u1d0f\u1d0d \u1d1b\u1d07\u029f\u1d07\u1d18\u1d0f\u0280\u1d1b"));
            int rows = this.guiConfig.getInt("rows", 3);
            Inventory gui = Bukkit.createInventory(null, (int)(rows * 9), (String)title);
            if (this.guiConfig.contains("worlds")) {
                for (String worldKey : this.guiConfig.getConfigurationSection("worlds").getKeys(false)) {
                    this.setupGuiItem(gui, player, worldKey);
                }
            }
            player.openInventory(gui);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void setupGuiItem(Inventory gui, Player player, String worldKey) {
        String path = "worlds." + worldKey;
        int slot = this.guiConfig.getInt(path + ".slot");
        String worldName = this.guiConfig.getString(path + ".world");
        Material material = Material.getMaterial((String)this.guiConfig.getString(path + ".material", "GRASS_BLOCK"));
        String itemName = Hex.translateAllColorCodes(this.guiConfig.getString(path + ".name", ""));
        String permission = this.guiConfig.getString(path + ".permission", "");
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            return;
        }
        World world = Bukkit.getWorld((String)worldName);
        if (world == null) {
            return;
        }
        boolean worldAccessible = this.rtpManager.isWorldEnabled(worldName);
        int playerCount = world.getPlayers().size();
        ItemStack item = this.createWorldItem(material, itemName, worldKey, playerCount);
        if (item != null && slot >= 0 && slot < gui.getSize()) {
            gui.setItem(slot, item);
        }
    }

    private ItemStack createWorldItem(Material material, String name, String worldKey, int playerCount) {
        ItemStack item;
        ItemMeta meta;
        if (material == null) {
            material = Material.GRASS_BLOCK;
        }
        if ((meta = (item = new ItemStack(material)).getItemMeta()) == null) {
            return item;
        }
        meta.setDisplayName(name);
        ArrayList<String> lore = new ArrayList<String>();
        List configLore = this.guiConfig.getStringList("worlds." + worldKey + ".lore");
        for (Object obj : configLore) {
            String line = (String) obj;
            String processedLine = line.replace("%players%", String.valueOf(playerCount));
            lore.add(Hex.translateAllColorCodes(processedLine));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void reloadGuiConfig() {
        this.loadGuiConfig();
    }

    private String getWorldFromSlot(int slot) {
        if (!this.guiConfig.contains("worlds")) {
            return null;
        }
        for (String worldKey : this.guiConfig.getConfigurationSection("worlds").getKeys(false)) {
            if (this.guiConfig.getInt("worlds." + worldKey + ".slot") != slot) continue;
            return this.guiConfig.getString("worlds." + worldKey + ".world");
        }
        return null;
    }

    private String getWorldKeyFromSlot(int slot) {
        if (!this.guiConfig.contains("worlds")) {
            return null;
        }
        for (String worldKey : this.guiConfig.getConfigurationSection("worlds").getKeys(false)) {
            if (this.guiConfig.getInt("worlds." + worldKey + ".slot") != slot) continue;
            return worldKey;
        }
        return null;
    }

    private boolean hasPermissionForWorld(Player player, String worldKey) {
        String permission = this.guiConfig.getString("worlds." + worldKey + ".permission", "");
        return permission.isEmpty() || player.hasPermission(permission);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String configTitle;
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getWhoClicked();
        String title = event.getView().getTitle();
        String strippedTitle = ChatColor.stripColor((String)title);
        if (!strippedTitle.equals(configTitle = ChatColor.stripColor((String)Hex.translateAllColorCodes(this.guiConfig.getString("title", "&8\u0280\u1d00\u0274\u1d05\u1d0f\u1d0d \u1d1b\u1d07\u029f\u1d07\u1d18\u1d0f\u0280\u1d1b"))))) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }
        String worldName = this.getWorldFromSlot(slot);
        String worldKey = this.getWorldKeyFromSlot(slot);
        if (worldName == null || worldKey == null) {
            return;
        }
        this.handleWorldClick(player, worldName, worldKey);
    }

    private void handleWorldClick(Player player, String worldName, String worldKey) {
        if (this.teleportingPlayers.contains(player.getUniqueId())) {
            player.sendMessage(this.rtpManager.getMessage("teleport-on-cooldown-message", "%wait_time%", "kurz"));
            player.closeInventory();
            return;
        }
        this.rtpManager.playSound(player, "button_click");
        World world = Bukkit.getWorld((String)worldName);
        if (world == null) {
            player.sendMessage(this.rtpManager.getMessage("no-world-permission-message"));
            player.closeInventory();
            return;
        }
        if (!this.hasPermissionForWorld(player, worldKey) || !this.rtpManager.isWorldEnabled(worldName)) {
            player.sendMessage(this.rtpManager.getMessage("no-world-permission-message"));
            player.closeInventory();
            return;
        }
        if (!this.rtpManager.canUseRTP(player, world)) {
            long remaining = this.rtpManager.getRemainingCooldown(player, world);
            long seconds = Math.max(1L, remaining / 1000L);
            player.sendMessage(this.rtpManager.getMessage("teleport-on-cooldown-message", "%wait_time%", String.valueOf(seconds)));
            player.closeInventory();
            return;
        }
        player.closeInventory();
        this.startTeleport(player, world);
    }

    private void startTeleport(final Player player, final World world) {
        this.rtpManager.playSound(player, "teleporting");
        final UUID playerId = player.getUniqueId();
        this.teleportingPlayers.add(playerId);
        final int teleportDelay = this.rtpManager.getWaitTime(player);
        double maxDistance = this.plugin.getConfig().getDouble("teleport-distance", 0.3);
        final double maxDistanceSquared = maxDistance * maxDistance;
        final Location startLocation = player.getLocation().clone();
        final String startWorldName = startLocation.getWorld().getName();
        final Object[] taskHolder = new Object[1];
        Runnable countdownTask = new Runnable(){
            int count;
            boolean firstRun;
            Location lastValidLocation;
            String lastWorldName;
            boolean isCancelled;
            {
                this.count = teleportDelay;
                this.firstRun = true;
                this.lastValidLocation = startLocation.clone();
                this.lastWorldName = startWorldName;
                this.isCancelled = false;
            }

            private void cancelTask() {
                if (this.isCancelled) {
                    return;
                }
                this.isCancelled = true;
                if (taskHolder[0] != null) {
                    RTPGui.this.plugin.cancelTask(taskHolder[0]);
                    taskHolder[0] = null;
                }
                RTPGui.this.teleportingPlayers.remove(playerId);
                RTPGui.this.startLocations.remove(playerId);
            }

            @Override
            public void run() {
                if (this.isCancelled) {
                    return;
                }
                if (this.firstRun) {
                    this.firstRun = false;
                    int currentCount = this.count;
                    RTPGui.this.plugin.runAtPlayer(player, () -> {
                        if (teleportDelay > 0) {
                            RTPGui.this.rtpManager.sendCooldownMessage(player, currentCount);
                        }
                    });
                    return;
                }
                if (!RTPGui.this.teleportingPlayers.contains(playerId) || !player.isOnline()) {
                    this.cancelTask();
                    return;
                }
                Location currentLocation = player.getLocation();
                String currentWorldName = currentLocation.getWorld().getName();
                if (!currentWorldName.equals(this.lastWorldName)) {
                    this.lastWorldName = currentWorldName;
                    this.lastValidLocation = currentLocation.clone();
                } else {
                    double deltaZ;
                    double deltaY;
                    double deltaX = currentLocation.getX() - this.lastValidLocation.getX();
                    double distanceSquared = deltaX * deltaX + (deltaY = currentLocation.getY() - this.lastValidLocation.getY()) * deltaY + (deltaZ = currentLocation.getZ() - this.lastValidLocation.getZ()) * deltaZ;
                    if (distanceSquared > maxDistanceSquared) {
                        RTPGui.this.plugin.runAtPlayer(player, () -> {
                            RTPGui.this.rtpManager.playSound(player, "teleport_fail");
                            RTPGui.this.sendActionBar(player, RTPGui.this.rtpManager.getActionBar("teleporting-cancel-message"));
                            player.sendMessage(RTPGui.this.rtpManager.getMessage("teleporting-cancel-message"));
                        });
                        this.cancelTask();
                        return;
                    }
                    this.lastValidLocation = currentLocation.clone();
                }
                --this.count;
                if (this.count <= 0) {
                    RTPGui.this.performTeleport(player, world, playerId);
                    this.cancelTask();
                    return;
                }
                int currentCount = this.count;
                RTPGui.this.plugin.runAtPlayer(player, () -> {
                    RTPGui.this.rtpManager.sendCooldownMessage(player, currentCount);
                    RTPGui.this.rtpManager.playSound(player, "teleporting");
                });
            }
        };
        taskHolder[0] = this.plugin.runGlobalTimer(countdownTask, 1L, 20L);
    }

    private void performTeleport(Player player, World world, UUID playerId) {
        this.rtpManager.findSafeLocationAsync(world).thenAccept(safeLocation -> this.plugin.runAtPlayer(player, () -> {
            if (safeLocation != null) {
                player.teleportAsync(safeLocation).thenAccept(success -> {
                    if (success.booleanValue()) {
                        this.rtpManager.playSound(player, "teleport_success");
                        this.rtpManager.setCooldown(player, world);
                        this.sendActionBar(player, this.rtpManager.getActionBar("teleported-message"));
                        player.sendMessage(this.rtpManager.getMessage("teleported-message"));
                        this.rtpManager.applyTeleportEffects(player);
                    } else {
                        this.rtpManager.playSound(player, "teleport_fail");
                        this.sendActionBar(player, this.rtpManager.getActionBar("fail-message"));
                        player.sendMessage(this.rtpManager.getMessage("fail-message"));
                    }
                    this.teleportingPlayers.remove(playerId);
                    this.startLocations.remove(playerId);
                });
            } else {
                this.rtpManager.playSound(player, "teleport_fail");
                this.sendActionBar(player, this.rtpManager.getActionBar("fail-message"));
                player.sendMessage(this.rtpManager.getMessage("fail-message"));
                this.teleportingPlayers.remove(playerId);
                this.startLocations.remove(playerId);
            }
        }));
    }

    private void sendActionBar(Player player, String message) {
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText((String)message));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

