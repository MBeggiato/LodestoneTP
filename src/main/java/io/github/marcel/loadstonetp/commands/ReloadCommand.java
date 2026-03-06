package io.github.marcel.loadstonetp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.marcel.loadstonetp.LodestoneTP;
import io.github.marcel.loadstonetp.utils.ComponentFormatter;
import io.github.marcel.loadstonetp.utils.PermissionChecker;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public final class ReloadCommand {

    private ReloadCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> buildNode(LodestoneTP plugin) {
        return Commands.literal("reload")
                .requires(source -> source.getSender() instanceof Player player
                        ? PermissionChecker.isAdmin(player)
                        : source.getSender().hasPermission("lodestonetp.admin"))
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();

                    plugin.reloadConfig();

                    int cooldownSeconds = plugin.getConfig().getInt("cooldown.seconds", 10);
                    plugin.getCooldownManager().setCooldownSeconds(cooldownSeconds);
                    plugin.getTeleportEffects().startAmbientLoop();
                    plugin.getTeleportEffects().restoreAllLightBlocks();

                        source.getSender().sendMessage(ComponentFormatter.success("LodestoneTP config reloaded!"));
                    plugin.getLogger().info("Config reloaded by " + source.getSender().getName());

                    return Command.SINGLE_SUCCESS;
                });
    }
}
