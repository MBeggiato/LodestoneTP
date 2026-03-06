package io.github.marcel.loadstonetp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.marcel.loadstonetp.LodestoneTP;
import io.github.marcel.loadstonetp.utils.ComponentFormatter;
import io.github.marcel.loadstonetp.utils.PermissionChecker;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public final class HologramCommand {

    private HologramCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> buildNode(LodestoneTP plugin) {
        return Commands.literal("hologram")
                .requires(source -> {
                    if (source.getSender() instanceof Player player) {
                        return PermissionChecker.isAdmin(player)
                                || player.hasPermission("lodestonetp.manage.hologram");
                    }
                    return source.getSender().hasPermission("lodestonetp.admin") ||
                           source.getSender().hasPermission("lodestonetp.manage.hologram");
                })
                .then(Commands.literal("cleanup")
                    .executes(ctx -> {
                        CommandSourceStack source = ctx.getSource();
                        
                        if (!(source.getSender() instanceof Player player)) {
                            source.getSender().sendMessage(ComponentFormatter.error("This command can only be used by players!"));
                            return Command.SINGLE_SUCCESS;
                        }

                        int removed = plugin.getLodestoneListener().getStructureHologram().cleanupStrayEntities();
                        
                        player.sendMessage(ComponentFormatter.success("Hologram cleanup complete - ")
                            .append(ComponentFormatter.info(removed + " entities removed.")));
                        
                        return Command.SINGLE_SUCCESS;
                    })
                );
    }
}
