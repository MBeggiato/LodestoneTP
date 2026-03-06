package io.github.marcel.loadstonetp.effects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Displays a holographic preview of required multiblock structures in the world.
 * Uses BlockDisplay entities to show the exact blocks needed, with optional labels.
 * Holograms persist until the structure is completed or the player dismisses them.
 */
public class StructureHologram {

    private final JavaPlugin plugin;
    private final NamespacedKey hologramKey;
    private final Map<UUID, List<BlockDisplay>> playerBlockDisplays = new HashMap<>();

    public StructureHologram(JavaPlugin plugin) {
        this.plugin = plugin;
        this.hologramKey = new NamespacedKey(plugin, "lodestonetp_hologram");
    }

    /**
     * Shows a holographic preview of the Teleporter structure.
     * Only shows blocks that are missing or incorrect - already placed blocks are hidden.
     * @param player The player to show it to
     * @param center The center location (Lodestone position)
     */
    public void showTeleporterStructure(Player player, Location center) {
        if (!plugin.getConfig().getBoolean("effects.structure-hologram.enabled", true)) {
            return;
        }

        removeForPlayer(player);

        if (center.getWorld() == null) return;

        playerBlockDisplays.put(player.getUniqueId(), new ArrayList<>());

        boolean missingLodestone = !isBlockCorrect(center, Material.LODESTONE);
        boolean missingAbove = !isBlockCorrect(center.clone().add(0, 1, 0), Material.POLISHED_BLACKSTONE_BRICKS);
        boolean missingBelow = !isBlockCorrect(center.clone().add(0, -1, 0), Material.POLISHED_BLACKSTONE_BRICKS);

        if (!missingLodestone && !missingAbove && !missingBelow) {
            return;
        }

        if (missingLodestone) {
            showBlock(player, center, Material.LODESTONE);
        }

        if (missingAbove) {
            Location above = center.clone().add(0, 1, 0);
            showBlock(player, above, Material.POLISHED_BLACKSTONE_BRICKS);
        }

        if (missingBelow) {
            Location below = center.clone().add(0, -1, 0);
            showBlock(player, below, Material.POLISHED_BLACKSTONE_BRICKS);
        }
    }

    /**
     * Checks if a block at the given location matches the required material.
     * @param location The location to check
     * @param material The required material
     * @return true if the block is correct, false if it's missing or wrong material
     */
    private boolean isBlockCorrect(Location location, Material material) {
        if (location.getBlock() == null) return false;
        return location.getBlock().getType() == material;
    }

    /**
     * Shows a single block as a hologram.
     */
    private void showBlock(Player player, Location location, Material material) {
        if (location.getWorld() == null) return;

        BlockDisplay blockDisplay = (BlockDisplay) location.getWorld()
                .spawnEntity(location.clone(), org.bukkit.entity.EntityType.BLOCK_DISPLAY);

        blockDisplay.setBlock(material.createBlockData());
        blockDisplay.setGlowing(true);
        blockDisplay.setGlowColorOverride(org.bukkit.Color.fromARGB(255, 100, 200, 255)); // Cyan glow
        blockDisplay.setViewRange(64);

        blockDisplay.getPersistentDataContainer().set(hologramKey, PersistentDataType.BYTE, (byte) 1);

        playerBlockDisplays.get(player.getUniqueId()).add(blockDisplay);
    }

    /**
     * Refreshes the hologram when blocks are placed/removed.
     * This re-checks the structure and updates what's displayed.
     */
    public void refreshStructure(Player player, Location center) {
        // Simply remove and re-show - this checks current block states
        removeForPlayer(player);
        showTeleporterStructure(player, center);
    }

    /**
     * Removes all hologram entities for a specific player.
     */
    public void removeForPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        List<BlockDisplay> blocks = playerBlockDisplays.getOrDefault(uuid, new ArrayList<>());
        blocks.forEach(org.bukkit.entity.Entity::remove);
        playerBlockDisplays.remove(uuid);
    }

    /**
     * Removes all hologram entities globally.
     */
    public void removeAll() {
        playerBlockDisplays.values().forEach(list -> list.forEach(org.bukkit.entity.Entity::remove));
        playerBlockDisplays.clear();
    }

    /**
     * Returns whether a player currently has active holograms.
     */
    public boolean hasActive(Player player) {
        return playerBlockDisplays.containsKey(player.getUniqueId()) && 
               !playerBlockDisplays.get(player.getUniqueId()).isEmpty();
    }

    /**
     * Returns the number of currently active hologram entities for a player.
     */
    public int getActiveCount(Player player) {
        return playerBlockDisplays.getOrDefault(player.getUniqueId(), new ArrayList<>()).size();
    }

    /**
     * Cleans up all stray BlockDisplay entities from all worlds.
     * Only removes entities that were created by this plugin (marked with persistent data).
     * Use this to remove orphaned holograms that weren't properly cleaned up.
     * @return The number of entities removed
     */
    public int cleanupStrayEntities() {
        int removed = 0;

        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity instanceof BlockDisplay) {
                    if (entity.getPersistentDataContainer().has(hologramKey, PersistentDataType.BYTE)) {
                        entity.remove();
                        removed++;
                    }
                }
            }
        }

        return removed;
    }
}