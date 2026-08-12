/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatMessageType
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.World$Environment
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package smorki.rtp.Manager;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import smorki.rtp.Hex;
import smorki.rtp.Manager.RTPZoneManager;
import smorki.rtp.RTP;

public class RTPManager {
    private final RTP plugin;
    private final Random random;
    private final ConcurrentHashMap<String, ConcurrentHashMap<UUID, Long>> cooldowns;
    private final HashMap<String, String> worldRegionCache;
    private final RTPZoneManager zoneManager;

    public RTPManager(RTP plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.cooldowns = new ConcurrentHashMap();
        this.worldRegionCache = new HashMap();
        this.zoneManager = new RTPZoneManager(plugin, this);
        this.loadWorldRegionCache();
    }

    private void loadWorldRegionCache() {
        ConfigurationSection section;
        this.worldRegionCache.clear();
        if (this.plugin.getConfig().contains("world-settings") && (section = this.plugin.getConfig().getConfigurationSection("world-settings")) != null) {
            for (String regionKey : section.getKeys(false)) {
                String worldName = this.plugin.getConfig().getString("world-settings." + regionKey + ".world");
                if (worldName == null || worldName.isEmpty()) continue;
                this.worldRegionCache.put(worldName, regionKey);
            }
        }
    }

    public RTPZoneManager getZoneManager() {
        return this.zoneManager;
    }

    private boolean hasBypass(Player player) {
        return player.isOp() || player.hasPermission("rtp.bypass");
    }

    public boolean canUseRTP(Player player, World world) {
        if (world == null) {
            return false;
        }
        if (this.hasBypass(player)) {
            return true;
        }
        RTPZoneManager.RTPZone zone = this.zoneManager.getZoneAtLocation(player);
        if (zone != null) {
            return this.canUseRTPInZone(player, zone);
        }
        String worldName = world.getName();
        UUID playerId = player.getUniqueId();
        ConcurrentHashMap worldCooldowns = this.cooldowns.computeIfAbsent(worldName, k -> new ConcurrentHashMap());
        Long lastUsed = (Long)worldCooldowns.get(playerId);
        if (lastUsed == null) {
            return true;
        }
        long currentTime = System.currentTimeMillis();
        long cooldownTime = (long)this.getCooldownTime(player) * 1000L;
        return currentTime - lastUsed >= cooldownTime;
    }

    public boolean canUseRTPInZone(Player player, RTPZoneManager.RTPZone zone) {
        if (zone == null) {
            return false;
        }
        if (this.hasBypass(player)) {
            return true;
        }
        int remaining = this.zoneManager.getRemainingSeconds(player, zone.getId());
        return remaining <= 0;
    }

    public long getRemainingCooldownInZone(Player player, RTPZoneManager.RTPZone zone) {
        if (zone == null || this.hasBypass(player)) {
            return 0L;
        }
        return (long)this.zoneManager.getRemainingSeconds(player, zone.getId()) * 1000L;
    }

    public long getRemainingCooldown(Player player, World world) {
        if (world == null || this.hasBypass(player)) {
            return 0L;
        }
        RTPZoneManager.RTPZone zone = this.zoneManager.getZoneAtLocation(player);
        if (zone != null) {
            return this.getRemainingCooldownInZone(player, zone);
        }
        String worldName = world.getName();
        UUID playerId = player.getUniqueId();
        ConcurrentHashMap<UUID, Long> worldCooldowns = this.cooldowns.get(worldName);
        if (worldCooldowns == null) {
            return 0L;
        }
        Long lastUsed = worldCooldowns.get(playerId);
        if (lastUsed == null) {
            return 0L;
        }
        long currentTime = System.currentTimeMillis();
        long cooldownTime = (long)this.getCooldownTime(player) * 1000L;
        long remaining = cooldownTime - (currentTime - lastUsed);
        return Math.max(0L, remaining);
    }

    public void setCooldown(Player player, World world) {
        if (world == null || this.hasBypass(player)) {
            return;
        }
        String worldName = world.getName();
        UUID playerId = player.getUniqueId();
        ConcurrentHashMap worldCooldowns = this.cooldowns.computeIfAbsent(worldName, k -> new ConcurrentHashMap());
        worldCooldowns.put(playerId, System.currentTimeMillis());
    }

    public int getCooldownTime(Player player) {
        if (this.hasBypass(player)) {
            return 0;
        }
        RTPZoneManager.RTPZone zone = this.zoneManager.getZoneAtLocation(player);
        if (zone != null) {
            return zone.getCooldownTime();
        }
        return this.plugin.getConfig().getInt("default-cooldown-seconds", 60);
    }

    public int getWaitTime(Player player) {
        return this.hasBypass(player) ? 0 : this.plugin.getConfig().getInt("wait-time-seconds", 5);
    }

    public void applyTeleportEffects(Player player) {
        ConfigurationSection effectsSection;
        if (player == null) {
            return;
        }
        if (this.plugin.getConfig().contains("effects-on-random-teleport") && (effectsSection = this.plugin.getConfig().getConfigurationSection("effects-on-random-teleport")) != null) {
            for (String key : effectsSection.getKeys(false)) {
                String path = "effects-on-random-teleport." + key;
                String typeStr = this.plugin.getConfig().getString(path + ".type");
                int level = this.plugin.getConfig().getInt(path + ".level", 1);
                int seconds = this.plugin.getConfig().getInt(path + ".seconds", 5);
                if (typeStr == null || typeStr.isEmpty()) continue;
                try {
                    PotionEffectType effectType = PotionEffectType.getByName((String)typeStr);
                    if (effectType != null) {
                        int potionLevel = Math.max(0, level - 1);
                        PotionEffect effect = new PotionEffect(effectType, seconds * 20, potionLevel, true, false, true);
                        player.addPotionEffect(effect);
                        continue;
                    }
                    this.plugin.getLogger().warning("Unknown potion effect type: " + typeStr);
                }
                catch (Exception e) {
                    this.plugin.getLogger().warning("Failed to apply effect " + typeStr + ": " + e.getMessage());
                }
            }
        }
    }

    public CompletableFuture<Location> findSafeLocationAsync(World world) {
        CompletableFuture<Location> future = new CompletableFuture<Location>();
        if (world == null) {
            future.complete(null);
            return future;
        }
        String regionKey = this.getRegionKeyForWorld(world.getName());
        if (regionKey == null) {
            future.complete(null);
            return future;
        }
        int maxAttempts = this.getPregenerateLocationAmount();
        int minX = this.getWorldMinX(regionKey);
        int maxX = this.getWorldMaxX(regionKey);
        int minZ = this.getWorldMinZ(regionKey);
        int maxZ = this.getWorldMaxZ(regionKey);
        this.findSafeLocationRecursive(world, minX, maxX, minZ, maxZ, maxAttempts, 0, future);
        return future;
    }

    private void findSafeLocationRecursive(World world, int minX, int maxX, int minZ, int maxZ, int maxAttempts, int attempt, CompletableFuture<Location> future) {
        if (attempt >= maxAttempts) {
            future.complete(null);
            return;
        }
        int x = minX + this.random.nextInt(Math.max(1, maxX - minX));
        int z = minZ + this.random.nextInt(Math.max(1, maxZ - minZ));
        Location tempLocation = new Location(world, (double)x, 0.0, (double)z);
        Bukkit.getServer().getRegionScheduler().run((Plugin)this.plugin, tempLocation, task -> {
            Location location;
            int y = world.getEnvironment() == World.Environment.NETHER ? this.findSafeNetherY(world, x, z) : world.getHighestBlockYAt(x, z) + 1;
            if (y != -1 && this.isLocationSafe(location = new Location(world, (double)x + 0.5, (double)y, (double)z + 0.5))) {
                future.complete(location);
                return;
            }
            this.findSafeLocationRecursive(world, minX, maxX, minZ, maxZ, maxAttempts, attempt + 1, future);
        });
    }

    private int findSafeNetherY(World world, int x, int z) {
        for (int y = 100; y > 30; --y) {
            if (!this.isNetherLocationSafe(world, x, y, z)) continue;
            return y;
        }
        return -1;
    }

    private boolean isNetherLocationSafe(World world, int x, int y, int z) {
        Material blockAtFeet = world.getBlockAt(x, y, z).getType();
        Material blockAtHead = world.getBlockAt(x, y + 1, z).getType();
        Material blockBelow = world.getBlockAt(x, y - 1, z).getType();
        return !blockAtFeet.isSolid() && !blockAtHead.isSolid() && blockBelow.isSolid() && blockBelow != Material.BEDROCK && blockBelow != Material.LAVA && blockAtFeet != Material.LAVA && blockAtHead != Material.LAVA;
    }

    public boolean isLocationSafe(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        Material blockUnder = world.getBlockAt(x, y - 1, z).getType();
        Material feetBlock = world.getBlockAt(x, y, z).getType();
        Material headBlock = world.getBlockAt(x, y + 1, z).getType();
        if (feetBlock.isSolid() || headBlock.isSolid() || !blockUnder.isSolid() || blockUnder == Material.BEDROCK) {
            return false;
        }
        if (this.isDangerousBlock(feetBlock) || this.isDangerousBlock(headBlock) || this.isDangerousBlock(blockUnder)) {
            return false;
        }
        return !this.isBlacklistedBlock(feetBlock) && !this.isBlacklistedBlock(headBlock) && !this.isBlacklistedBlock(blockUnder);
    }

    private boolean isDangerousBlock(Material material) {
        return material == Material.LAVA || material == Material.FIRE;
    }

    private boolean isBlacklistedBlock(Material material) {
        List blacklistedBlocks = this.plugin.getConfig().getStringList("blacklisted-blocks");
        return blacklistedBlocks.contains(material.name());
    }

    private String getRegionKeyForWorld(String worldName) {
        return this.worldRegionCache.get(worldName);
    }

    public int getAllowedWalkRange() {
        return this.plugin.getConfig().getInt("allowed-walk-range", 3);
    }

    public int getPregenerateLocationAmount() {
        return this.plugin.getConfig().getInt("pregenerate-location-amount", 10);
    }

    public int getWorldMinX(String regionKey) {
        return this.plugin.getConfig().getInt("world-settings." + regionKey + ".min-x", -50000);
    }

    public int getWorldMaxX(String regionKey) {
        return this.plugin.getConfig().getInt("world-settings." + regionKey + ".max-x", 50000);
    }

    public int getWorldMinZ(String regionKey) {
        return this.plugin.getConfig().getInt("world-settings." + regionKey + ".min-z", -50000);
    }

    public int getWorldMaxZ(String regionKey) {
        return this.plugin.getConfig().getInt("world-settings." + regionKey + ".max-z", 50000);
    }

    public boolean isWorldEnabled(String worldName) {
        return worldName != null && this.worldRegionCache.containsKey(worldName);
    }

    public void reloadConfig() {
        this.plugin.reloadConfig();
        this.loadWorldRegionCache();
        this.zoneManager.reload();
    }

    public String getMessage(String path) {
        String message = this.plugin.getLangConfig().getString("messages." + path, "&c" + path);
        return Hex.translateAllColorCodes(message);
    }

    public String getMessage(String path, String placeholder, String replacement) {
        return this.getMessage(path).replace(placeholder, replacement);
    }

    public String getActionBar(String path) {
        String message = this.plugin.getLangConfig().getString("action-bars." + path, this.plugin.getLangConfig().getString("messages." + path, "&c" + path));
        return Hex.translateAllColorCodes(message);
    }

    public String getActionBar(String path, String placeholder, String replacement) {
        return this.getActionBar(path).replace(placeholder, replacement);
    }

    public boolean sendCooldownMessage(Player player, long time) {
        String formatted;
        List messages;
        if (player == null) {
            return false;
        }
        boolean anySent = false;
        String timeStr = String.valueOf(time);
        if (this.plugin.getLangConfig().getBoolean("cooldown.module.chat.enabled", true)) {
            messages = this.plugin.getLangConfig().getStringList("cooldown.module.chat.message");
            for (Object obj : messages) {
                String message = (String) obj;
                formatted = Hex.translateAllColorCodes(message.replace("%time%", timeStr));
                player.sendMessage(formatted);
            }
            anySent = true;
        }
        if (this.plugin.getLangConfig().getBoolean("cooldown.module.actionbar.enabled", true)) {
            messages = this.plugin.getLangConfig().getStringList("cooldown.module.actionbar.message");
            for (Object obj : messages) {
                String message = (String) obj;
                formatted = Hex.translateAllColorCodes(message.replace("%time%", timeStr));
                this.sendActionBar(player, formatted);
            }
            anySent = true;
        }
        if (this.plugin.getLangConfig().getBoolean("cooldown.module.title.enabled", false)) {
            String subtitle;
            List titles = this.plugin.getLangConfig().getStringList("cooldown.module.title.message");
            List subtitles = this.plugin.getLangConfig().getStringList("cooldown.module.subtitle.message");
            String title = titles.isEmpty() ? "" : Hex.translateAllColorCodes(((String)titles.get(0)).replace("%time%", timeStr));
            String string = subtitle = subtitles.isEmpty() ? "" : Hex.translateAllColorCodes(((String)subtitles.get(0)).replace("%time%", timeStr));
            if (!title.isEmpty() || !subtitle.isEmpty()) {
                player.sendTitle(title, subtitle, 10, 40, 10);
                anySent = true;
            }
        }
        return anySent;
    }

    private void sendActionBar(Player player, String message) {
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText((String)message));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public void playSound(Player player, String soundType) {
        if (player == null || soundType == null) {
            return;
        }
        String soundPath = "sound." + soundType;
        if (!this.plugin.getConfig().contains(soundPath)) {
            return;
        }
        String soundName = this.plugin.getConfig().getString(soundPath);
        if (soundName == null || soundName.isEmpty() || soundName.equalsIgnoreCase("none")) {
            return;
        }
        try {
            player.playSound(player.getLocation(), soundName, 1.0f, 1.0f);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

