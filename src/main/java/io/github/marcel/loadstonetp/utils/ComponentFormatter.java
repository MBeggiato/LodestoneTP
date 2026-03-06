package io.github.marcel.loadstonetp.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Centralized text formatting utilities for consistent messaging across the plugin.
 * Eliminates duplicated Component building patterns.
 */
public final class ComponentFormatter {

    private ComponentFormatter() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a red error message component.
     */
    public static Component error(String message) {
        return Component.text(message, NamedTextColor.RED);
    }

    /**
     * Creates a green success message component.
     */
    public static Component success(String message) {
        return Component.text(message, NamedTextColor.GREEN);
    }

    /**
     * Creates a yellow warning message component.
     */
    public static Component warning(String message) {
        return Component.text(message, NamedTextColor.YELLOW);
    }

    /**
     * Creates an aqua info message component.
     */
    public static Component info(String message) {
        return Component.text(message, NamedTextColor.AQUA);
    }

    /**
     * Creates a gray neutral message component.
     */
    public static Component neutral(String message) {
        return Component.text(message, NamedTextColor.GRAY);
    }

    /**
     * Creates a gold emphasis message component.
     */
    public static Component emphasis(String message) {
        return Component.text(message, NamedTextColor.GOLD);
    }

    /**
     * Creates a progress bar display (e.g., "5/10" in white).
     * @param current Current value
     * @param max Maximum value
     * @return Component showing "current/max"
     */
    public static Component progress(int current, int max) {
        return Component.text(current + "/" + max, NamedTextColor.WHITE);
    }

    /**
     * Formats a cost display (e.g., "5x Diamond").
     * @param amount Amount of currency
     * @param currency Currency material name (e.g., "Diamond", "Ender Pearl")
     * @return Formatted component with amount in white, currency in light purple
     */
    public static Component formatCost(int amount, String currency) {
        return Component.text(amount + "x ", NamedTextColor.WHITE)
                .append(Component.text(currency, NamedTextColor.LIGHT_PURPLE));
    }

    /**
     * Creates a permission denied message with consistent formatting.
     */
    public static Component permissionDenied(String action) {
        return Component.text("You don't have permission to ", NamedTextColor.RED)
                .append(Component.text(action + "!", NamedTextColor.RED));
    }

    /**
     * Creates a coordinate display (e.g., "123, 64, 456").
     */
    public static Component coordinates(int x, int y, int z) {
        return Component.text(x + ", " + y + ", " + z, NamedTextColor.GRAY);
    }

    /**
     * Creates a world name display in aqua.
     */
    public static Component worldName(String name) {
        return Component.text("World: " + name, NamedTextColor.AQUA);
    }

    /**
     * Creates a teleporter name display with emphasis.
     */
    public static Component teleporterName(String name) {
        return Component.text("\"" + name + "\"", NamedTextColor.GOLD);
    }

    /**
     * Creates a labeled value pair (e.g., "Owner: Player123").
     */
    public static Component labeledValue(String label, String value) {
        return Component.text(label + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE));
    }

    /**
     * Creates styled number (thousands separator aware).
     */
    public static Component number(long value) {
        return Component.text(String.format("%,d", value), NamedTextColor.WHITE);
    }

    /**
     * Creates a separator line (e.g., "─────────────").
     */
    public static Component separator(NamedTextColor color) {
        return Component.text("─────────────", color);
    }
}
