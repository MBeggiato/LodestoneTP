package io.github.marcel.loadstonetp.utils;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Location handling utilities for consistent formatting and calculations.
 * Centralizes location-based operations that are used across the plugin.
 */
public final class LocationUtils {

    private LocationUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Formats a location as a human-readable string.
     * Format: "World: x.0, y.0, z.0"
     * @param location The location to format
     * @return Formatted location string, or "Unknown" if null
     */
    public static String format(Location location) {
        if (location == null) return "Unknown";
        
        String world = location.getWorld() != null ? location.getWorld().getName() : "???";
        double x = Math.round(location.getX() * 10.0) / 10.0;
        double y = Math.round(location.getY() * 10.0) / 10.0;
        double z = Math.round(location.getZ() * 10.0) / 10.0;
        
        return String.format("%s: %.1f, %.1f, %.1f", world, x, y, z);
    }

    /**
     * Formats block coordinates as a compact string.
     * Format: "x, y, z"
     * @param location The location to format
     * @return Formatted coordinates string
     */
    public static String getBlockString(Location location) {
        if (location == null) return "unknown";
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    /**
     * Calculates the distance between two locations.
     * If locations are in different worlds, returns -1.
     * @param from Starting location
     * @param to Ending location
     * @return Distance in blocks, or -1 if different worlds
     */
    public static double distance(Location from, Location to) {
        if (from == null || to == null) return -1;
        if (!from.getWorld().equals(to.getWorld())) return -1;
        return from.distance(to);
    }

    /**
     * Checks if a location is within a certain block radius of a center point.
     * @param center The center location
     * @param radius The radius in blocks
     * @param location The location to check
     * @return true if location is within radius of center
     */
    public static boolean isWithinRadius(Location center, double radius, Location location) {
        if (center == null || location == null) return false;
        if (!center.getWorld().equals(location.getWorld())) return false;
        return center.distance(location) <= radius;
    }

    /**
     * Checks if a location is in a specific world.
     * @param location The location to check
     * @param world The world to check for
     * @return true if location is in the specified world
     */
    public static boolean isInWorld(Location location, World world) {
        if (location == null || world == null) return false;
        return location.getWorld().equals(world);
    }

    /**
     * Creates a safe location copy to avoid reference issues.
     * Copies location, yaw, and pitch.
     * @param location The location to copy
     * @return A copy of the location
     */
    public static Location copy(Location location) {
        if (location == null) return null;
        Location copy = location.clone();
        copy.setYaw(location.getYaw());
        copy.setPitch(location.getPitch());
        return copy;
    }

    /**
     * Formats location with world name, useful for display purposes.
     * Format: "LocationName (World: x.0, y.0, z.0)"
     * @param name Display name for the location
     * @param location The location to format
     * @return Formatted string
     */
    public static String formatNamed(String name, Location location) {
        if (location == null) return name + " (Unknown)";
        return name + " (" + format(location) + ")";
    }

    /**
     * Gets the center of a block (adds 0.5 to x and z, ground level on y).
     * Useful for spawning entities at block centers.
     * @param location The block location
     * @return Location at the center of the block
     */
    public static Location getBlockCenter(Location location) {
        if (location == null) return null;
        Location center = location.clone();
        center.setX(center.getBlockX() + 0.5);
        center.setZ(center.getBlockZ() + 0.5);
        return center;
    }

    /**
     * Checks if two locations refer to the same block.
     * Ignores yaw, pitch, and sub-block position.
     * @param loc1 First location
     * @param loc2 Second location
     * @return true if both locations are the same block
     */
    public static boolean isSameBlock(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return false;
        return loc1.getWorld().equals(loc2.getWorld()) &&
               loc1.getBlockX() == loc2.getBlockX() &&
               loc1.getBlockY() == loc2.getBlockY() &&
               loc1.getBlockZ() == loc2.getBlockZ();
    }

    /**
     * Offsets a location by relative coordinates.
     * Does not modify the original location.
     * @param location The base location
     * @param dx X offset
     * @param dy Y offset
     * @param dz Z offset
     * @return New offset location
     */
    public static Location offset(Location location, int dx, int dy, int dz) {
        if (location == null) return null;
        Location offset = location.clone();
        offset.add(dx, dy, dz);
        return offset;
    }
}
