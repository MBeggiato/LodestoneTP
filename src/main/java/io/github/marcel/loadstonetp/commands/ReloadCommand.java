package io.github.marcel.loadstonetp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.marcel.loadstonetp.LodestoneTP;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class ReloadCommand {

    private ReloadCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> buildNode(LodestoneTP plugin) {
        return Commands.literal("reload")
                .requires(source -> {
                    // Allow access if sender is OP OR has the permission
                    if (source.getSender() instanceof org.bukkit.entity.Player player) {
                        return player.isOp() || player.hasPermission("lodestonetp.admin");
                    }
                    return source.getSender().hasPermission("lodestonetp.admin");
                })
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();

                    plugin.reloadConfig();

                    int cooldownSeconds = plugin.getConfig().getInt("cooldown.seconds", 10);
                    plugin.getCooldownManager().setCooldownSeconds(cooldownSeconds);
                    plugin.getTeleportEffects().startAmbientLoop();
                    plugin.getTeleportEffects().restoreAllLightBlocks();

                    source.getSender().sendMessage(
                            Component.text("LodestoneTP config reloaded!", NamedTextColor.GREEN)
                    );
                    plugin.getLogger().info("Config reloaded by " + source.getSender().getName());

                    return Command.SINGLE_SUCCESS;
                });
    }
}
