package io.github.marcel.loadstonetp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.marcel.loadstonetp.LodestoneTP;
import io.github.marcel.loadstonetp.dialogs.AdminDialogs;
import io.github.marcel.loadstonetp.utils.ComponentFormatter;
import io.github.marcel.loadstonetp.utils.PermissionChecker;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import org.bukkit.entity.Player;
public final class AdminCommand {

    private AdminCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> buildNode(LodestoneTP plugin) {
        return Commands.literal("admin")
                .requires(source -> source.getSender() instanceof Player player
                        ? PermissionChecker.isAdmin(player)
                        : source.getSender().hasPermission("lodestonetp.admin"))
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();

                    if (!(source.getSender() instanceof Player player)) {
                        source.getSender().sendMessage(ComponentFormatter.error("This command can only be used by players!"));
                        return Command.SINGLE_SUCCESS;
                    }

                    player.showDialog(AdminDialogs.createMainPanel(plugin));
                    return Command.SINGLE_SUCCESS;
                });
    }
}
