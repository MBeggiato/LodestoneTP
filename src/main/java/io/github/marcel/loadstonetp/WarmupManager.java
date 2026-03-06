package io.github.marcel.loadstonetp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WarmupManager implements Listener {

    private final LodestoneTP plugin;
    private final Map<UUID, WarmupState> activeWarmups = new HashMap<>();

    public WarmupManager(LodestoneTP plugin) {
        this.plugin = plugin;
    }

    public boolean shouldUseWarmup(Player player) {
        if (!plugin.getConfig().getBoolean("warmup.enabled", false)) {
            return false;
        }
        if (plugin.getConfig().getInt("warmup.seconds", 0) <= 0) {
            return false;
        }
        String bypassPermission = plugin.getConfig().getString("warmup.bypass-permission", "lodestonetp.warmup.bypass");
        return bypassPermission == null || bypassPermission.isBlank() || !player.hasPermission(bypassPermission);
    }

    public int getWarmupSeconds() {
        return Math.max(0, plugin.getConfig().getInt("warmup.seconds", 0));
    }

    public void startWarmup(Player player, Runnable onComplete) {
        cancelWarmup(player.getUniqueId(), false);

        int warmupSeconds = getWarmupSeconds();
        UUID playerUuid = player.getUniqueId();
        WarmupState state = new WarmupState(warmupSeconds, onComplete);
        
        // Start visual effect around player during warmup
        plugin.getTeleportEffects().startWarmupEffect(player);
        
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancelWarmup(playerUuid, false);
                return;
            }

            if (!activeWarmups.containsKey(playerUuid)) {
                return;
            }

            WarmupState currentState = activeWarmups.get(playerUuid);
            if (currentState.remainingSeconds <= 0) {
                activeWarmups.remove(playerUuid);
                plugin.getTeleportEffects().stopWarmupEffect(playerUuid);
                if (currentState.task != null) {
                    currentState.task.cancel();
                }
                currentState.onComplete.run();
                return;
            }

            player.sendActionBar(Component.text("Teleport in " + currentState.remainingSeconds + "s...", NamedTextColor.GOLD));
            currentState.remainingSeconds--;
        }, 0L, 20L);

        state.task = task;
        activeWarmups.put(playerUuid, state);
    }

    public void cancelWarmup(UUID playerUuid, boolean notifyPlayer) {
        WarmupState state = activeWarmups.remove(playerUuid);
        if (state == null) {
            return;
        }
        if (state.task != null) {
            state.task.cancel();
        }
        
        // Stop visual effect
        plugin.getTeleportEffects().stopWarmupEffect(playerUuid);

        if (!notifyPlayer) {
            return;
        }

        Player player = plugin.getServer().getPlayer(playerUuid);
        if (player == null) {
            return;
        }

        String cancelMessage = plugin.getConfig().getString("warmup.cancel-message", "Teleport canceled because you moved.");
        player.sendMessage(Component.text(cancelMessage, NamedTextColor.RED));

        String cancelSound = plugin.getConfig().getString("warmup.cancel-sound", "minecraft:block.note_block.bass");
        if (cancelSound != null && !cancelSound.isBlank()) {
            player.playSound(player.getLocation(), cancelSound, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        WarmupState state = activeWarmups.get(playerUuid);
        if (state == null) {
            return;
        }

        if (!plugin.getConfig().getBoolean("warmup.cancel-on-move", true)) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        // Only cancel on significant position changes (> 0.1 blocks)
        // This allows head rotation and small natural position adjustments
        double deltaX = Math.abs(to.getX() - from.getX());
        double deltaY = Math.abs(to.getY() - from.getY());
        double deltaZ = Math.abs(to.getZ() - from.getZ());
        
        if (deltaX > 0.1 || deltaY > 0.1 || deltaZ > 0.1) {
            cancelWarmup(playerUuid, true);
        }
    }

    private static final class WarmupState {
        private int remainingSeconds;
        private final Runnable onComplete;
        private BukkitTask task;

        private WarmupState(int remainingSeconds, Runnable onComplete) {
            this.remainingSeconds = remainingSeconds;
            this.onComplete = onComplete;
        }
    }
}
