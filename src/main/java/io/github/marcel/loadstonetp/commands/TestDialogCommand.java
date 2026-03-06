package io.github.marcel.loadstonetp.commands;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;

public final class TestDialogCommand {

    private TestDialogCommand() {}

    public static void register(Commands registrar) {
        registrar.register(
                Commands.literal("testdialog")
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            if (!(source.getSender() instanceof Player player)) {
                                source.getSender().sendMessage(
                                        Component.text("Only players can use this command!", NamedTextColor.RED)
                                );
                                return Command.SINGLE_SUCCESS;
                            }

                            Dialog dialog = Dialog.create(builder -> builder.empty()
                                    .base(DialogBase.builder(Component.text("Welcome to LoadstoneTP", NamedTextColor.GOLD))
                                            .body(List.of(
                                                    DialogBody.plainMessage(Component.text("This is a test dialog from LoadstoneTP!", NamedTextColor.WHITE))
                                            ))
                                            .canCloseWithEscape(true)
                                            .build())
                                    .type(DialogType.confirmation(
                                            ActionButton.create(
                                                    Component.text("Confirm"),
                                                    Component.text("Click to confirm"),
                                                    150,
                                                    DialogAction.customClick(
                                                            (view, audience) -> audience.sendMessage(
                                                                    Component.text("You confirmed the dialog!", NamedTextColor.GREEN)
                                                            ),
                                                            ClickCallback.Options.builder()
                                                                    .uses(1)
                                                                    .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                                                    .build()
                                                    )
                                            ),
                                            ActionButton.create(
                                                    Component.text("Cancel"),
                                                    Component.text("Click to cancel"),
                                                    150,
                                                    DialogAction.customClick(
                                                            (view, audience) -> audience.sendMessage(
                                                                    Component.text("You cancelled the dialog!", NamedTextColor.RED)
                                                            ),
                                                            ClickCallback.Options.builder()
                                                                    .uses(1)
                                                                    .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                                                    .build()
                                                    )
                                            )
                                    ))
                            );

                            player.showDialog(dialog);
                            return Command.SINGLE_SUCCESS;
                        })
                        .build(),
                "Opens a test dialog"
        );
    }
}
