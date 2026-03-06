package io.github.marcel.loadstonetp;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages custom advancements for LodestoneTP.
 * Loads advancement JSON on enable, removes on disable.
 */
@SuppressWarnings("deprecation")
public class AdvancementManager {

    private static final String NAMESPACE = "lodestonetp";
    private static final String CRITERIA = "requirement";

    private final Logger logger;
    private final List<NamespacedKey> registeredKeys = new ArrayList<>();
    private final Map<String, Advancement> advancementCache = new HashMap<>();

    public AdvancementManager(LodestoneTP plugin) {
        this.logger = plugin.getLogger();
    }

    /**
     * Register all custom advancements. Call from onEnable().
     */
    public void registerAll() {
        // Root advancement — creates the custom "LodestoneTP" tab
        register("root", buildJson(
                "minecraft:lodestone",
                "LodestoneTP",
                "Teleportation through lodestone technology",
                "task",
                null,
                "minecraft:block/polished_blackstone_bricks",
                false, false, false
        ));

        // First teleporter
        register("engineer", buildJson(
                "minecraft:polished_blackstone_bricks",
                "Engineer",
                "Build your first teleporter",
                "task",
                NAMESPACE + ":root",
                null,
                true, true, false
        ));

        // First teleport
        register("first_steps", buildJson(
                "minecraft:ender_pearl",
                "First Steps",
                "Teleport for the first time",
                "task",
                NAMESPACE + ":engineer",
                null,
                true, true, false
        ));

        // Cross-dimension teleport
        register("dimension_hopper", buildJson(
                "minecraft:end_portal_frame",
                "Dimension Hopper",
                "Teleport across dimensions",
                "goal",
                NAMESPACE + ":first_steps",
                null,
                true, true, false
        ));

        // Long distance teleport (10,000+ blocks)
        register("long_distance", buildJson(
                "minecraft:spyglass",
                "Long Distance",
                "Teleport over 10,000 blocks",
                "challenge",
                NAMESPACE + ":first_steps",
                null,
                true, true, false
        ));

        // Create 5 teleporters
        register("network_builder", buildJson(
                "minecraft:redstone",
                "Network Builder",
                "Create 5 teleporters",
                "goal",
                NAMESPACE + ":engineer",
                null,
                true, true, false
        ));

        // Create a private teleporter
        register("private_line", buildJson(
                "minecraft:iron_door",
                "Private Line",
                "Create a private teleporter",
                "task",
                NAMESPACE + ":engineer",
                null,
                true, true, false
        ));

        // Whitelist a player
        register("sharing_is_caring", buildJson(
                "minecraft:cake",
                "Sharing is Caring",
                "Whitelist a player on your teleporter",
                "task",
                NAMESPACE + ":private_line",
                null,
                true, true, false
        ));

        // Note: Do NOT call Bukkit.reloadData() here — on 1.21+ it wipes custom advancements
        logger.info("Registered " + registeredKeys.size() + " custom advancements (" + advancementCache.size() + " cached).");
    }

    /**
     * Remove all custom advancements. Call from onDisable().
     */
    public void unregisterAll() {
        for (NamespacedKey key : registeredKeys) {
            try {
                Bukkit.getUnsafe().removeAdvancement(key);
            } catch (Exception e) {
                logger.warning("Failed to remove advancement " + key + ": " + e.getMessage());
            }
        }
        if (!registeredKeys.isEmpty()) {
            Bukkit.reloadData();
        }
        registeredKeys.clear();
        advancementCache.clear();
    }

    /**
     * Grant an advancement to a player by key name (e.g. "engineer", "first_steps").
     * Returns true if the advancement was newly completed.
     */
    public boolean grant(Player player, String keyName) {
        Advancement advancement = advancementCache.get(keyName);
        if (advancement == null) {
            // Fallback to Bukkit lookup
            NamespacedKey key = new NamespacedKey(NAMESPACE, keyName);
            advancement = Bukkit.getAdvancement(key);
        }
        if (advancement == null) {
            logger.warning("Advancement not found (even in cache): " + keyName);
            return false;
        }

        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        if (progress.isDone()) {
            return false;
        }

        for (String criteria : progress.getRemainingCriteria()) {
            progress.awardCriteria(criteria);
        }
        return true;
    }

    /**
     * Check if a player has completed an advancement.
     */
    public boolean hasCompleted(Player player, String keyName) {
        Advancement advancement = advancementCache.get(keyName);
        if (advancement == null) {
            NamespacedKey key = new NamespacedKey(NAMESPACE, keyName);
            advancement = Bukkit.getAdvancement(key);
        }
        if (advancement == null) return false;
        return player.getAdvancementProgress(advancement).isDone();
    }

    private void register(String path, String json) {
        NamespacedKey key = new NamespacedKey(NAMESPACE, path);
        try {
            if (Bukkit.getAdvancement(key) != null) {
                Bukkit.getUnsafe().removeAdvancement(key);
            }
            Advancement adv = Bukkit.getUnsafe().loadAdvancement(key, json);
            registeredKeys.add(key);
            if (adv != null) {
                advancementCache.put(path, adv);
            } else {
                logger.warning("loadAdvancement returned null for " + key);
            }
        } catch (Exception e) {
            logger.warning("Failed to register advancement " + key + ": " + e.getMessage());
        }
    }

    private String buildJson(String icon, String title, String description, String frame,
                             String parent, String background,
                             boolean showToast, boolean announceToChat, boolean hidden) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"display\":{");
        sb.append("\"icon\":{\"id\":\"").append(icon).append("\"},");
        sb.append("\"title\":{\"text\":\"").append(escapeJson(title)).append("\"},");
        sb.append("\"description\":{\"text\":\"").append(escapeJson(description)).append("\"},");
        sb.append("\"frame\":\"").append(frame).append("\",");
        if (background != null) {
            sb.append("\"background\":\"").append(background).append("\",");
        }
        sb.append("\"show_toast\":").append(showToast).append(",");
        sb.append("\"announce_to_chat\":").append(announceToChat).append(",");
        sb.append("\"hidden\":").append(hidden);
        sb.append("},");
        if (parent != null) {
            sb.append("\"parent\":\"").append(parent).append("\",");
        }
        sb.append("\"criteria\":{\"").append(CRITERIA).append("\":{\"trigger\":\"minecraft:impossible\"}},");
        sb.append("\"requirements\":[[\"").append(CRITERIA).append("\"]]");
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
