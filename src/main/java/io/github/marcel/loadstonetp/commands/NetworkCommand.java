package io.github.marcel.loadstonetp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.marcel.loadstonetp.LodestoneTP;
import io.github.marcel.loadstonetp.dialogs.NetworkDialogs;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;

public final class NetworkCommand {

    private NetworkCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> buildNode(LodestoneTP plugin) {
        return Commands.literal("networks")
                .requires(source -> {
                    if (!(source.getSender() instanceof Player player)) {
                        return false;
                    }
                    // Allow access if player is OP OR has manage_networks/admin permission
                    return player.isOp() || player.hasPermission("lodestonetp.manage_networks") || player.hasPermission("lodestonetp.admin");
                })
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();

                    if (!(source.getSender() instanceof Player player)) {
                        source.getSender().sendMessage(
                                Component.text("This command can only be used by players!", NamedTextColor.RED)
                        );
                        return Command.SINGLE_SUCCESS;
                    }

                    player.showDialog(NetworkDialogs.createNetworkManagementDialog(plugin.getDatabaseManager(), player, plugin));
                    return Command.SINGLE_SUCCESS;
                });
    }
}
