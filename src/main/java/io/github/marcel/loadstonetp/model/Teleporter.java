package io.github.marcel.loadstonetp.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public record Teleporter(int id, String name, String world, int x, int y, int z, float yaw, String ownerUuid, boolean isPublic, Integer cooldownOverride, Integer networkId, Integer linkedTeleporterId) {

    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        if (w == null) return null;

        // Spawn player 1.5 blocks in front of the teleporter (based on creator's facing)
        double rad = Math.toRadians(yaw);
        double offsetX = -Math.sin(rad) * 1.5;
        double offsetZ = Math.cos(rad) * 1.5;

        // Y is the lodestone level; player stands on the block below it
        Location loc = new Location(w, x + 0.5 + offsetX, y, z + 0.5 + offsetZ);
        loc.setYaw(yaw + 180); // Face toward the teleporter
        return loc;
    }
}
