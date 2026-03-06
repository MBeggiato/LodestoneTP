package io.github.marcel.loadstonetp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages per-player teleport cooldowns in memory.
 * Cooldowns reset on server restart.
 */
public class CooldownManager {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private int cooldownSeconds;

    public CooldownManager(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    /**
     * Check if a player is on cooldown.
     * @return remaining seconds, or 0 if not on cooldown
     */
    public int getRemainingCooldown(UUID playerUuid) {
        return getRemainingCooldown(playerUuid, null);
    }

    /**
     * Check if a player is on cooldown with optional per-teleporter override.
     * @return remaining seconds, or 0 if not on cooldown
     */
    public int getRemainingCooldown(UUID playerUuid, Integer teleporterCooldownOverride) {
        Long lastUsed = cooldowns.get(playerUuid);
        if (lastUsed == null) {
            return 0;
        }
        int effectiveCooldown = teleporterCooldownOverride != null ? teleporterCooldownOverride : cooldownSeconds;
        long elapsed = (System.currentTimeMillis() - lastUsed) / 1000;
        long remaining = effectiveCooldown - elapsed;
        return remaining > 0 ? (int) remaining : 0;
    }

    /**
     * Check if a player is on cooldown.
     */
    public boolean isOnCooldown(UUID playerUuid) {
        return isOnCooldown(playerUuid, null);
    }

    /**
     * Check if a player is on cooldown with optional per-teleporter override.
     */
    public boolean isOnCooldown(UUID playerUuid, Integer teleporterCooldownOverride) {
        return getRemainingCooldown(playerUuid, teleporterCooldownOverride) > 0;
    }

    /**
     * Record that a player just teleported (starts their cooldown).
     */
    public void setCooldown(UUID playerUuid) {
        cooldowns.put(playerUuid, System.currentTimeMillis());
    }

    /**
     * Get the configured cooldown duration in seconds.
     */
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    /**
     * Update the cooldown duration (e.g. after config reload).
     */
    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }
}
