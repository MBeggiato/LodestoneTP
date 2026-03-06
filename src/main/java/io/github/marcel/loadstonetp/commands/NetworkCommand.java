package io.github.marcel.loadstonetp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.marcel.loadstonetp.LodestoneTP;
import io.github.marcel.loadstonetp.dialogs.NetworkDialogs;
import io.github.marcel.loadstonetp.utils.ComponentFormatter;
import io.github.marcel.loadstonetp.utils.PermissionChecker;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import org.bukkit.entity.Player;

public final class NetworkCommand {

    private NetworkCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> buildNode(LodestoneTP plugin) {
        return Commands.literal("networks")
                .requires(source -> source.getSender() instanceof Player player
                        && PermissionChecker.canManageNetworks(player))
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();

                    if (!(source.getSender() instanceof Player player)) {
                        source.getSender().sendMessage(ComponentFormatter.error("This command can only be used by players!"));
                        return Command.SINGLE_SUCCESS;
                    }

                    player.showDialog(NetworkDialogs.createNetworkManagementDialog(plugin.getDatabaseManager(), player, plugin));
                    return Command.SINGLE_SUCCESS;
                });
    }
}
