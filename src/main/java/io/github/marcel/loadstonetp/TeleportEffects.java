package io.github.marcel.loadstonetp;

import io.github.marcel.loadstonetp.model.Teleporter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Light;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeleportEffects {

    private final LodestoneTP plugin;
    private List<Location> teleporterLocations = Collections.emptyList();
    private BukkitTask ambientTask;
    private final Map<java.util.UUID, BukkitTask> warmupEffectTasks = new HashMap<>();

    public TeleportEffects(LodestoneTP plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("effects.enabled", true);
    }

    // --- Teleport Effects ---

    public void playDeparture(Location location) {
        if (!isEnabled()) return;

        World world = location.getWorld();
        if (world == null) return;

        FileConfiguration config = plugin.getConfig();
        Location center = location.clone().add(0, 0.5, 0);
        String intensity = config.getString("effects.particles.intensity", "normal");
        
        // Scale counts based on intensity
        int portalCount = getParticleCount(40, intensity);
        int reverseCount = getParticleCount(20, intensity);

        if (config.getBoolean("effects.sounds", true)) {
            world.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        if (config.getBoolean("effects.particles.enabled", true)) {
            // Ground/location particles
            world.spawnParticle(Particle.PORTAL, center, portalCount, 0.3, 0.8, 0.3, 0.5);
            world.spawnParticle(Particle.REVERSE_PORTAL, center, reverseCount, 0.2, 0.5, 0.2, 0.3);
            
            // Around player particles (spiral effect)
            for (int i = 0; i < 8; i++) {
                double angle = (i / 8.0) * Math.PI * 2;
                double x = Math.cos(angle) * 0.6;
                double z = Math.sin(angle) * 0.6;
                Location particleLoc = center.clone().add(x, 0.3 * i, z);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 3, 0.1, 0.1, 0.1, 0.1);
            }
        }
    }

    public void playArrival(Player player, Location location) {
        if (!isEnabled()) return;

        World world = location.getWorld();
        if (world == null) return;

        FileConfiguration config = plugin.getConfig();
        Location center = location.clone().add(0, 0.5, 0);
        String intensity = config.getString("effects.particles.intensity", "normal");
        
        // Scale counts based on intensity
        int reverseCount = getParticleCount(50, intensity);
        int endRodCount = getParticleCount(15, intensity);

        if (config.getBoolean("effects.sounds", true)) {
            world.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 0.8f);
        }

        if (config.getBoolean("effects.particles.enabled", true)) {
            // Ground/location particles
            world.spawnParticle(Particle.REVERSE_PORTAL, center, reverseCount, 0.4, 0.8, 0.4, 0.3);
            world.spawnParticle(Particle.END_ROD, center, endRodCount, 0.2, 0.3, 0.2, 0.05);
            
            // Burst around player (intense effect)
            int burstParticles = getParticleCount(25, intensity);
            Location playerCenter = player.getLocation().add(0, 1.0, 0);
            world.spawnParticle(Particle.REVERSE_PORTAL, playerCenter, burstParticles, 0.5, 0.8, 0.5, 0.4);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, playerCenter, getParticleCount(15, intensity), 0.6, 0.6, 0.6, 0.2);
        }
    }

    /**
     * Scales particle count based on configured intensity (low/normal/high).
     */
    private int getParticleCount(int baseCount, String intensity) {
        return switch (intensity.toLowerCase()) {
            case "low" -> (int) (baseCount * 0.5);
            case "high" -> (int) (baseCount * 1.5);
            default -> baseCount; // normal
        };
    }

    /**
     * Starts continuous particle effect around player during warmup/channeling.
     * Spawns a rotating ring of particles at player location.
     */
    public void startWarmupEffect(Player player) {
        if (!isEnabled() || !plugin.getConfig().getBoolean("effects.particles.enabled", true)) {
            return;
        }

        stopWarmupEffect(player.getUniqueId());

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopWarmupEffect(player.getUniqueId());
                return;
            }

            Location playerLoc = player.getLocation().add(0, 1.0, 0);
            World world = playerLoc.getWorld();
            if (world == null) return;

            String intensity = plugin.getConfig().getString("effects.particles.intensity", "normal");
            int particleCount = getParticleCount(8, intensity);

            // Rotating ring of particles
            for (int i = 0; i < particleCount; i++) {
                double angle = (System.currentTimeMillis() / 20.0 + i * (Math.PI * 2 / particleCount)) % (Math.PI * 2);
                double x = Math.cos(angle) * 0.8;
                double z = Math.sin(angle) * 0.8;
                Location particleLoc = playerLoc.clone().add(x, 0, z);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0.05);
            }
        }, 0L, 2L);

        warmupEffectTasks.put(player.getUniqueId(), task);
    }

    /**
     * Stops the warmup particle effect for a player.
     */
    public void stopWarmupEffect(java.util.UUID playerUuid) {
        BukkitTask task = warmupEffectTasks.remove(playerUuid);
        if (task != null) {
            task.cancel();
        }
    }

    public void startAmbientLoop() {
        stopAmbientLoop();
        refreshTeleporterLocations();

        boolean soundEnabled = plugin.getConfig().getBoolean("effects.ambient.enabled", true);
        boolean particlesEnabled = plugin.getConfig().getBoolean("effects.ambient.particles", true);
        if (!soundEnabled && !particlesEnabled) return;
        int intervalTicks = plugin.getConfig().getInt("effects.ambient.interval-ticks", 80);
        ambientTask = Bukkit.getScheduler().runTaskTimer(plugin, this::playAmbientSounds, 40L, intervalTicks);
    }

    public void stopAmbientLoop() {
        if (ambientTask != null) {
            ambientTask.cancel();
            ambientTask = null;
        }
    }

    public void refreshTeleporterLocations() {
        List<Teleporter> teleporters = plugin.getDatabaseManager().getAllTeleporters();
        List<Location> locations = new ArrayList<>(teleporters.size());
        for (Teleporter tp : teleporters) {
            World world = Bukkit.getWorld(tp.world());
            if (world != null) {
                locations.add(new Location(world, tp.x() + 0.5, tp.y() + 0.5, tp.z() + 0.5));
            }
        }
        this.teleporterLocations = locations;
    }

    private void playAmbientSounds() {
        if (!isEnabled()) return;

        FileConfiguration config = plugin.getConfig();
        boolean ambientSound = config.getBoolean("effects.ambient.enabled", true);
        boolean ambientParticles = config.getBoolean("effects.ambient.particles", true);

        if (!ambientSound && !ambientParticles) return;

        float volume = (float) config.getDouble("effects.ambient.volume", 0.4);
        double range = config.getDouble("effects.ambient.range", 8.0);
        double rangeSquared = range * range;

        for (Player player : Bukkit.getOnlinePlayers()) {
            Location playerLoc = player.getLocation();
            for (Location tpLoc : teleporterLocations) {
                if (!playerLoc.getWorld().equals(tpLoc.getWorld())) continue;
                if (playerLoc.distanceSquared(tpLoc) > rangeSquared) continue;

                if (ambientSound) {
                    player.playSound(tpLoc, Sound.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS, volume, 1.0f);
                }
                if (ambientParticles) {
                    player.spawnParticle(Particle.END_ROD, tpLoc, 3, 0.25, 0.1, 0.25, 0.02);
                    player.spawnParticle(Particle.REVERSE_PORTAL, tpLoc, 5, 0.3, 0.2, 0.3, 0.05);
                }
            }
        }
    }

    // --- Light Block Management ---

    /**
     * Places an invisible light block above teleporter (y+2 from lodestone).
     * Only places if target block is AIR and light is enabled in config.
     */
    public void placeLightBlock(Location lodestoneLoc) {
        if (!plugin.getConfig().getBoolean("effects.light.enabled", true)) return;
        int level = plugin.getConfig().getInt("effects.light.level", 10);
        level = Math.max(0, Math.min(15, level));

        World world = lodestoneLoc.getWorld();
        if (world == null) return;

        Block target = world.getBlockAt(
                lodestoneLoc.getBlockX(),
                lodestoneLoc.getBlockY() + 2,
                lodestoneLoc.getBlockZ()
        );

        if (target.getType() != Material.AIR) return;

        target.setType(Material.LIGHT);
        if (target.getBlockData() instanceof Light lightData) {
            lightData.setLevel(level);
            target.setBlockData(lightData);
        }
    }

    /**
     * Removes light block above teleporter (y+2 from lodestone).
     * Only removes if block at that position is actually a LIGHT block.
     */
    public void removeLightBlock(Location lodestoneLoc) {
        World world = lodestoneLoc.getWorld();
        if (world == null) return;

        Block target = world.getBlockAt(
                lodestoneLoc.getBlockX(),
                lodestoneLoc.getBlockY() + 2,
                lodestoneLoc.getBlockZ()
        );

        if (target.getType() == Material.LIGHT) {
            target.setType(Material.AIR);
        }
    }

    /**
     * Removes light block for a teleporter using its stored coordinates.
     */
    public void removeLightBlock(Teleporter tp) {
        World world = Bukkit.getWorld(tp.world());
        if (world == null) return;
        removeLightBlock(new Location(world, tp.x(), tp.y(), tp.z()));
    }

    /**
     * Scans all existing teleporters and ensures light blocks are placed.
     * Called on startup and reload for crash recovery.
     */
    public void restoreAllLightBlocks() {
        if (!plugin.getConfig().getBoolean("effects.light.enabled", true)) return;

        List<Teleporter> teleporters = plugin.getDatabaseManager().getAllTeleporters();
        for (Teleporter tp : teleporters) {
            World world = Bukkit.getWorld(tp.world());
            if (world == null) continue;
            placeLightBlock(new Location(world, tp.x(), tp.y(), tp.z()));
        }
    }

    /**
     * Removes all light blocks for all existing teleporters.
     * Called on plugin disable for clean shutdown.
     */
    public void removeAllLightBlocks() {
        List<Teleporter> teleporters = plugin.getDatabaseManager().getAllTeleporters();
        for (Teleporter tp : teleporters) {
            removeLightBlock(tp);
        }
    }
}
