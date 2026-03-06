package io.github.marcel.loadstonetp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.marcel.loadstonetp.LodestoneTP;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class HologramCommand {

    private HologramCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> buildNode(LodestoneTP plugin) {
        return Commands.literal("hologram")
                .requires(source -> {
                    // Allow access if player is admin or has manage permission
                    if (source.getSender() instanceof Player player) {
                        return player.isOp() || player.hasPermission("lodestonetp.admin") || 
                               player.hasPermission("lodestonetp.manage.hologram");
                    }
                    return source.getSender().hasPermission("lodestonetp.admin") || 
                           source.getSender().hasPermission("lodestonetp.manage.hologram");
                })
                .then(Commands.literal("cleanup")
                    .executes(ctx -> {
                        CommandSourceStack source = ctx.getSource();
                        
                        if (!(source.getSender() instanceof Player player)) {
                            source.getSender().sendMessage(
                                    Component.text("This command can only be used by players!", NamedTextColor.RED)
                            );
                            return Command.SINGLE_SUCCESS;
                        }

                        // Run cleanup
                        int removed = plugin.getLodestoneListener().getStructureHologram().cleanupStrayEntities();
                        
                        player.sendMessage(
                                Component.text("✓ Hologram cleanup complete - ", NamedTextColor.GREEN)
                                        .append(Component.text(removed + " entities removed.", NamedTextColor.AQUA))
                        );
                        
                        return Command.SINGLE_SUCCESS;
                    })
                );
    }
}
