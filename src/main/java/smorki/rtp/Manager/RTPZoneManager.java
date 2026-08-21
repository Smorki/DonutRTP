package smorki.rtp.Manager;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import smorki.rtp.Hex;
import smorki.rtp.RTP;

public class RTPZoneManager implements Listener {
    private final RTP plugin;
    private final RTPManager rtpManager;
    private final Map<String, RTPZone> rtpZones;
    private final Map<String, ZoneGlobalTimer> zoneTimers;
    private final Map<UUID, String> playerZone;
    private final Random random;
    private RTPExpansion expansion;

    public RTPZoneManager(RTP plugin, RTPManager rtpManager) {
        this.plugin = plugin;
        this.rtpManager = rtpManager;
        this.rtpZones = new HashMap<>();
        this.zoneTimers = new ConcurrentHashMap<>();
        this.playerZone = new ConcurrentHashMap<>();
        this.random = new Random();
        this.loadRTPZones();
        this.registerPlaceholderAPI();
        this.registerEvents();
        this.startAllZoneTimers();
    }

    private void registerEvents() {
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    private void registerPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.expansion = new RTPExpansion();
            this.expansion.register();
        }
    }

    private void loadRTPZones() {
        this.rtpZones.clear();
        if (!this.plugin.getConfig().contains("rtp-zones")) {
            return;
        }
        ConfigurationSection zonesSection = this.plugin.getConfig().getConfigurationSection("rtp-zones");
        if (zonesSection == null) {
            return;
        }
        for (String zoneId : zonesSection.getKeys(false)) {
            String path = "rtp-zones." + zoneId;
            boolean enabled = this.plugin.getConfig().getBoolean(path + ".enabled", true);
            if (!enabled) continue;
            String zoneRegion = this.plugin.getConfig().getString(path + ".zone-region");
            String zoneWorld = this.plugin.getConfig().getString(path + ".zone-world");
            int cooldownTime = this.plugin.getConfig().getInt(path + ".cooldown-time", 60);
            int minimumPlayers = this.plugin.getConfig().getInt(path + ".minimum-players", 1);
            List<String> rtpWorlds = this.plugin.getConfig().getStringList(path + ".rtp-worlds");
            if (zoneRegion == null || zoneWorld == null || rtpWorlds.isEmpty()) continue;
            RTPZone zone = new RTPZone(zoneId, zoneRegion, zoneWorld, cooldownTime, minimumPlayers, rtpWorlds);
            this.rtpZones.put(zoneId, zone);
        }
    }

    private void startAllZoneTimers() {
        for (RTPZone zone : this.rtpZones.values()) {
            if (this.zoneTimers.containsKey(zone.getId())) continue;
            this.startGlobalTimer(zone);
        }
    }

    private void startGlobalTimer(RTPZone zone) {
        String zoneId = zone.getId();
        int cooldownSeconds = zone.getCooldownTime();
        int[] remaining = new int[]{cooldownSeconds};
        ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this.plugin, scheduledTask -> {
            if (remaining[0] <= 0) {
                ZoneGlobalTimer timer = this.zoneTimers.get(zoneId);
                if (timer != null) {
                    for (UUID pid : timer.playersInZone) {
                        Player p = Bukkit.getPlayer(pid);
                        if (p == null || !p.isOnline()) continue;
                        this.teleportPlayerFromZone(p, zone);
                    }
                }
                remaining[0] = zone.getCooldownTime();
                ZoneGlobalTimer t = this.zoneTimers.get(zoneId);
                if (t != null) {
                    t.remainingSeconds = remaining[0];
                }
            } else {
                remaining[0] = remaining[0] - 1;
                ZoneGlobalTimer timer = this.zoneTimers.get(zoneId);
                if (timer != null) {
                    timer.remainingSeconds = remaining[0];
                }
            }
        }, 20L, 20L);
        ZoneGlobalTimer newTimer = new ZoneGlobalTimer(zone, task, cooldownSeconds);
        this.zoneTimers.put(zoneId, newTimer);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.updatePlayerZone(event.getPlayer());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        player.getScheduler().runDelayed(this.plugin, scheduledTask -> this.updatePlayerZone(player), null, 2L);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        // OPTİMİZASYON: Yalnızca oyuncu FARKLI BİR BLOĞA geçtiğinde kontrol et (yaw/pitch ve sub-block hareketlerini yok say)
        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ() &&
                Objects.equals(from.getWorld(), to.getWorld())) {
            return;
        }

        this.updatePlayerZone(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ZoneGlobalTimer timer;
        UUID playerId = event.getPlayer().getUniqueId();
        String zoneId = this.playerZone.remove(playerId);
        if (zoneId != null && (timer = this.zoneTimers.get(zoneId)) != null) {
            timer.playersInZone.remove(playerId);
        }
    }

    private void updatePlayerZone(Player player) {
        UUID playerId = player.getUniqueId();
        RTPZone zone = this.getZoneAtLocation(player);
        String newZoneId = zone != null ? zone.getId() : null;
        String oldZoneId = this.playerZone.get(playerId);

        if (Objects.equals(newZoneId, oldZoneId)) {
            return;
        }

        if (oldZoneId != null) {
            ZoneGlobalTimer oldTimer = this.zoneTimers.get(oldZoneId);
            if (oldTimer != null) {
                oldTimer.playersInZone.remove(playerId);
                String leaveMessage = this.plugin.getLangConfig().getString("messages.zone-leave-message", "&fYou left the &c%zone% Zone&f!");
                leaveMessage = leaveMessage.replace("%zone%", zone != null ? zone.getZoneRegion() : oldZoneId);
                player.sendMessage(Hex.translateAllColorCodes(leaveMessage));
            }
        }

        if (newZoneId != null) {
            this.playerZone.put(playerId, newZoneId);
            ZoneGlobalTimer timer = this.zoneTimers.get(newZoneId);
            if (timer != null) {
                timer.playersInZone.add(playerId);
                String enterMessage = this.plugin.getLangConfig().getString("messages.zone-enter-message", "&fYou entered the &c%zone% Zone&f!");
                enterMessage = enterMessage.replace("%zone%", zone.getZoneRegion());
                player.sendMessage(Hex.translateAllColorCodes(enterMessage));
            } else {
                RTPZone newZone = this.getRTPZone(newZoneId);
                if (newZone != null) {
                    this.startGlobalTimer(newZone);
                    ZoneGlobalTimer newTimer = this.zoneTimers.get(newZoneId);
                    if (newTimer != null) {
                        newTimer.playersInZone.add(playerId);
                    }
                }
            }
        } else {
            this.playerZone.remove(playerId);
        }
    }

    private void teleportPlayerFromZone(Player player, RTPZone zone) {
        List<String> worlds = zone.getRtpWorlds();
        if (worlds.isEmpty()) {
            return;
        }
        String worldName = worlds.get(this.random.nextInt(worlds.size()));
        org.bukkit.World targetWorld = Bukkit.getWorld(worldName);
        if (targetWorld == null) {
            return;
        }
        int arenaDistance = this.plugin.getConfig().getInt("rtp-zones." + zone.getId() + ".arena-distance", 10);
        int glowingDuration = this.plugin.getConfig().getInt("rtp-zones." + zone.getId() + ".glowing-duration", 6);
        this.rtpManager.findSafeLocationAsync(targetWorld).thenAccept(centerLocation -> {
            if (centerLocation == null) {
                player.getScheduler().run(this.plugin, task -> {}, null);
                return;
            }
            this.findLocationInRadius(player, targetWorld, centerLocation, arenaDistance, zone, glowingDuration);
        });
    }

    private void findLocationInRadius(Player player, org.bukkit.World world, Location center, int radius, RTPZone zone, int glowingDuration) {
        for (int attempt = 0; attempt < 20; ++attempt) {
            int offsetX = this.random.nextInt(radius * 2) - radius;
            int offsetZ = this.random.nextInt(radius * 2) - radius;
            int x = center.getBlockX() + offsetX;
            int z = center.getBlockZ() + offsetZ;
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location location = new Location(world, (double)x + 0.5, (double)y, (double)z + 0.5);

            if (y <= world.getMinHeight() || y >= world.getMaxHeight() || !this.rtpManager.isLocationSafe(location)) continue;

            player.getScheduler().run(this.plugin, task -> player.teleportAsync(location).thenAccept(success -> {
                if (Boolean.TRUE.equals(success)) {
                    String teleportedMessage = this.plugin.getLangConfig().getString("messages.teleported-message", "&aSuccessfully teleported");
                    this.rtpManager.playSound(player, "teleport_success");
                    player.sendMessage(Hex.translateAllColorCodes(teleportedMessage));
                    this.applyGlowingEffect(player, glowingDuration);
                }
            }), null);
            return;
        }
        player.getScheduler().run(this.plugin, task -> player.teleportAsync(center).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                String teleportedMessage = this.plugin.getLangConfig().getString("messages.teleported-message", "&aSuccessfully teleported");
                this.rtpManager.playSound(player, "teleport_success");
                player.sendMessage(Hex.translateAllColorCodes(teleportedMessage));
                this.applyGlowingEffect(player, glowingDuration);
            }
        }), null);
    }

    private void applyGlowingEffect(Player player, int glowingDuration) {
        if (glowingDuration <= 0) {
            return;
        }
        player.getScheduler().run(this.plugin, task -> {
            player.setGlowing(true);
            try {
                PotionEffectType glow = PotionEffectType.GLOWING;
                if (glow != null) {
                    player.addPotionEffect(new PotionEffect(glow, glowingDuration * 20, 0, true, false, true));
                }
            } catch (Exception ignored) {}

            player.getScheduler().runDelayed(this.plugin, removeTask -> {
                if (player.isOnline()) {
                    player.setGlowing(false);
                    try {
                        PotionEffectType glow = PotionEffectType.GLOWING;
                        if (glow != null) {
                            player.removePotionEffect(glow);
                        }
                    } catch (Exception ignored) {}
                }
            }, null, (long)glowingDuration * 20L);
        }, null);
    }

    public int getRemainingSeconds(Player player, String zoneId) {
        ZoneGlobalTimer timer = this.zoneTimers.get(zoneId);
        if (timer != null) {
            return Math.max(0, timer.remainingSeconds);
        }
        return 0;
    }

    public Map<String, RTPZone> getRTPZones() {
        return this.rtpZones;
    }

    public RTPZone getRTPZone(String zoneId) {
        return this.rtpZones.get(zoneId);
    }

    /**
     * OPTİMİZASYON: WorldGuard sorgusu döngünün dışına çıkarıldı.
     * Artık kaç tane zone olursa olsun tek 1 WorldGuard lookup yapılır.
     */
    public RTPZone getZoneAtLocation(Player player) {
        if (this.rtpZones.isEmpty()) return null;

        Location loc = player.getLocation();
        org.bukkit.World bukkitWorld = loc.getWorld();
        if (bukkitWorld == null) return null;

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            World editWorld = BukkitAdapter.adapt(bukkitWorld);
            RegionManager regionManager = container.get(editWorld);
            if (regionManager == null) return null;

            BlockVector3 vector = BukkitAdapter.asBlockVector(loc);
            ApplicableRegionSet regions = regionManager.getApplicableRegions(vector);

            Set<String> regionIdsAtLoc = new HashSet<>();
            for (ProtectedRegion region : regions) {
                regionIdsAtLoc.add(region.getId().toLowerCase(Locale.ROOT));
            }

            if (regionIdsAtLoc.isEmpty()) return null;

            for (RTPZone zone : this.rtpZones.values()) {
                if (zone.getZoneWorld().equalsIgnoreCase(bukkitWorld.getName()) &&
                        regionIdsAtLoc.contains(zone.getZoneRegion().toLowerCase(Locale.ROOT))) {
                    return zone;
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    public void reload() {
        for (ZoneGlobalTimer timer : this.zoneTimers.values()) {
            if (timer.task == null) continue;
            timer.task.cancel();
        }
        this.zoneTimers.clear();
        this.playerZone.clear();
        this.loadRTPZones();
        this.startAllZoneTimers();
        if (this.expansion != null) {
            this.expansion.close();
            this.expansion = new RTPExpansion();
            this.expansion.register();
        }
    }

    private class RTPExpansion extends PlaceholderExpansion {
        private RTPExpansion() {}

        @NotNull
        @Override
        public String getIdentifier() {
            return "donutrtp";
        }

        @NotNull
        @Override
        public String getAuthor() {
            return "Smorki";
        }

        @NotNull
        @Override
        public String getVersion() {
            return RTPZoneManager.this.plugin.getDescription().getVersion();
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Nullable
        @Override
        public String onPlaceholderRequest(Player player, @NotNull String identifier) {
            if (player == null) {
                return "";
            }
            if (identifier.startsWith("zone_countdown_")) {
                String zoneId = identifier.substring("zone_countdown_".length());
                int remaining = RTPZoneManager.this.getRemainingSeconds(player, zoneId);
                return String.valueOf(remaining);
            }
            if (identifier.startsWith("zone_name_")) {
                String zoneId = identifier.substring("zone_name_".length());
                RTPZone zone = RTPZoneManager.this.getRTPZone(zoneId);
                if (zone != null) {
                    return zone.getZoneRegion();
                }
                return "unknown";
            }

            // OPTİMİZASYON: Placeholder requestlerinde WorldGuard çağırmak yerine cache Map'imizden okunur.
            if (identifier.equals("current_zone")) {
                String zoneId = RTPZoneManager.this.playerZone.get(player.getUniqueId());
                return zoneId != null ? zoneId : "none";
            }
            if (identifier.equals("current_zone_cooldown")) {
                String zoneId = RTPZoneManager.this.playerZone.get(player.getUniqueId());
                if (zoneId != null) {
                    return String.valueOf(RTPZoneManager.this.getRemainingSeconds(player, zoneId));
                }
                return "0";
            }
            return null;
        }

        public void close() {
            unregister();
        }
    }

    public static class RTPZone {
        private final String id;
        private final String zoneRegion;
        private final String zoneWorld;
        private final int cooldownTime;
        private final int minimumPlayers;
        private final List<String> rtpWorlds;

        public RTPZone(String id, String zoneRegion, String zoneWorld, int cooldownTime, int minimumPlayers, List<String> rtpWorlds) {
            this.id = id;
            this.zoneRegion = zoneRegion;
            this.zoneWorld = zoneWorld;
            this.cooldownTime = cooldownTime;
            this.minimumPlayers = minimumPlayers;
            this.rtpWorlds = rtpWorlds;
        }

        public String getId() {
            return this.id;
        }

        public String getZoneRegion() {
            return this.zoneRegion;
        }

        public String getZoneWorld() {
            return this.zoneWorld;
        }

        public int getCooldownTime() {
            return this.cooldownTime;
        }

        public int getMinimumPlayers() {
            return this.minimumPlayers;
        }

        public List<String> getRtpWorlds() {
            return this.rtpWorlds;
        }
    }

    private static class ZoneGlobalTimer {
        final RTPZone zone;
        final ScheduledTask task;
        int remainingSeconds;
        final Set<UUID> playersInZone;

        ZoneGlobalTimer(RTPZone zone, ScheduledTask task, int remainingSeconds) {
            this.zone = zone;
            this.task = task;
            this.remainingSeconds = remainingSeconds;
            this.playersInZone = ConcurrentHashMap.newKeySet();
        }
    }
}