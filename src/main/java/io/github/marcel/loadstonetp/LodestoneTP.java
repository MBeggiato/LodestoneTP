package io.github.marcel.loadstonetp;

import io.github.marcel.loadstonetp.commands.ReloadCommand;
import io.github.marcel.loadstonetp.commands.AdminCommand;
import io.github.marcel.loadstonetp.commands.NetworkCommand;
import io.github.marcel.loadstonetp.commands.HologramCommand;
import io.github.marcel.loadstonetp.db.DatabaseManager;
import io.github.marcel.loadstonetp.listeners.LodestoneInteractListener;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.plugin.java.JavaPlugin;

public class LodestoneTP extends JavaPlugin {

    private DatabaseManager databaseManager;
    private CooldownManager cooldownManager;
    private AdvancementManager advancementManager;
    private TeleportEffects teleportEffects;
    private WarmupManager warmupManager;
    private LodestoneInteractListener lodestoneInteractListener;

    private String pluginVersion() {
        return getPluginMeta().getVersion();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        databaseManager = new DatabaseManager(getDataFolder(), getLogger());

        int cooldownSeconds = getConfig().getInt("cooldown.seconds", 10);
        cooldownManager = new CooldownManager(cooldownSeconds);
        advancementManager = new AdvancementManager(this);
        advancementManager.registerAll();

        teleportEffects = new TeleportEffects(this);
        teleportEffects.startAmbientLoop();
        teleportEffects.restoreAllLightBlocks();

        warmupManager = new WarmupManager(this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(
                    Commands.literal("lodestonetp")
                            .then(ReloadCommand.buildNode(this))
                            .then(AdminCommand.buildNode(this))
                            .then(NetworkCommand.buildNode(this))
                            .then(HologramCommand.buildNode(this))
                            .build(),
                    "LodestoneTP commands"
            );
        });
        lodestoneInteractListener = new LodestoneInteractListener(databaseManager, this);
        getServer().getPluginManager().registerEvents(lodestoneInteractListener, this);
        getServer().getPluginManager().registerEvents(warmupManager, this);
        getLogger().info("╔════════════════════════════════════════╗");
        getLogger().info("║     LodestoneTP v" + pluginVersion() + " loaded!           ║");
        getLogger().info("║  Teleporter Network Plugin for Paper  ║");
        getLogger().info("╚════════════════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        if (teleportEffects != null) {
            teleportEffects.removeAllLightBlocks();
            teleportEffects.stopAmbientLoop();
        }
        if (advancementManager != null) {
            advancementManager.unregisterAll();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("LodestoneTP v" + pluginVersion() + " has been disabled!");
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public AdvancementManager getAdvancementManager() {
        return advancementManager;
    }

    public TeleportEffects getTeleportEffects() {
        return teleportEffects;
    }

    public WarmupManager getWarmupManager() {
        return warmupManager;
    }

    public LodestoneInteractListener getLodestoneListener() {
        return lodestoneInteractListener;
    }
}
