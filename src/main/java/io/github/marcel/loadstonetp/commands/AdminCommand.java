package io.github.marcel.loadstonetp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.marcel.loadstonetp.LodestoneTP;
import io.github.marcel.loadstonetp.dialogs.AdminDialogs;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;
public final class AdminCommand {

    private AdminCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> buildNode(LodestoneTP plugin) {
        return Commands.literal("admin")
                .requires(source -> {
                    // Allow access if player is OP OR has the permission
                    if (source.getSender() instanceof Player player) {
                        return player.isOp() || player.hasPermission("lodestonetp.admin");
                    }
                    return source.getSender().hasPermission("lodestonetp.admin");
                })
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();

                    if (!(source.getSender() instanceof Player player)) {
                        source.getSender().sendMessage(
                                Component.text("This command can only be used by players!", NamedTextColor.RED)
                        );
                        return Command.SINGLE_SUCCESS;
                    }

                    player.showDialog(AdminDialogs.createMainPanel(plugin));
                    return Command.SINGLE_SUCCESS;
                });
    }
}
