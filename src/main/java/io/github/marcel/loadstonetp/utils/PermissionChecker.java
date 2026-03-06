package io.github.marcel.loadstonetp.utils;

import io.github.marcel.loadstonetp.model.Network;
import org.bukkit.entity.Player;

/**
 * Centralized permission checking logic for the plugin.
 * Eliminates duplicated permission checks and ensures consistency.
 * Provides convenient admin bypass logic in one place.
 */
public final class PermissionChecker {

    private PermissionChecker() {
        // Utility class - prevent instantiation
    }

    /**
     * Checks if a player is a plugin admin.
     * Admins have full access to all plugin features.
     * @param player The player to check
     * @return true if player is admin (OP or has lodestonetp.admin permission)
     */
    public static boolean isAdmin(Player player) {
        if (player == null) return false;
        return player.isOp() || player.hasPermission("lodestonetp.admin");
    }

    /**
     * Checks if a player can use basic teleporter features.
     * @param player The player to check
     * @return true if player has lodestonetp.use permission
     */
    public static boolean canUse(Player player) {
        if (player == null) return false;
        return isAdmin(player) || player.hasPermission("lodestonetp.use");
    }

    /**
     * Checks if a player can create new teleporters.
     * @param player The player to check
     * @return true if player has lodestonetp.create permission
     */
    public static boolean canCreate(Player player) {
        if (player == null) return false;
        return isAdmin(player) || player.hasPermission("lodestonetp.create");
    }

    /**
     * Checks if a player has a specific permission, with admin bypass.
     * @param player The player to check
     * @param permission The permission node to check (without "lodestonetp." prefix)
     * @return true if player has permission or is admin
     */
    public static boolean hasPermission(Player player, String permission) {
        if (player == null) return false;
        if (isAdmin(player)) return true;
        return player.hasPermission("lodestonetp." + permission);
    }

    /**
     * Checks if a player can access a network.
     * Admins can always access; otherwise checks network-specific permission.
     * @param player The player to check
     * @param network The network to check access for
     * @return true if player can access this network
     */
    public static boolean canAccessNetwork(Player player, Network network) {
        if (player == null) return false;
        if (isAdmin(player)) return true;
        
        // If network has a permission node, check it
        if (network != null && network.permissionNode() != null && !network.permissionNode().isBlank()) {
            return player.hasPermission(network.permissionNode());
        }
        
        // If no specific permission, allow access
        return true;
    }

    /**
     * Checks if a player can manage networks.
     * @param player The player to check
     * @return true if player can manage networks
     */
    public static boolean canManageNetworks(Player player) {
        if (player == null) return false;
        return isAdmin(player) || player.hasPermission("lodestonetp.manage_networks");
    }

    /**
     * Checks if a player can manage holograms.
     * @param player The player to check
     * @return true if player can manage holograms
     */
    public static boolean canManageHolograms(Player player) {
        if (player == null) return false;
        return isAdmin(player) || player.hasPermission("lodestonetp.manage.hologram");
    }

    /**
     * Checks if a player should bypass cooldowns.
     * Only admins bypass cooldowns.
     * @param player The player to check
     * @return true if player bypasses cooldowns
     */
    public static boolean bypassCooldown(Player player) {
        return isAdmin(player);
    }

    /**
     * Asserts that a player has a required permission.
     * @param player The player to check
     * @param permission The permission node (with "lodestonetp." prefix)
     * @throws IllegalArgumentException if player doesn't have permission
     */
    public static void assertPermission(Player player, String permission) throws IllegalArgumentException {
        if (player == null || !player.hasPermission(permission)) {
            throw new IllegalArgumentException("Insufficient permissions for: " + permission);
        }
    }

    /**
     * Gets a player's permission level as a string for debugging/logging.
     * @param player The player to check
     * @return A string describing the player's permission level
     */
    public static String getPermissionLevel(Player player) {
        if (player == null) return "UNKNOWN";
        if (isAdmin(player)) return "ADMIN";
        if (canCreate(player)) return "CREATOR";
        if (canUse(player)) return "USER";
        return "GUEST";
    }
}
