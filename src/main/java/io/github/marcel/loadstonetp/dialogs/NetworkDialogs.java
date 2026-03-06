package io.github.marcel.loadstonetp.dialogs;

import io.github.marcel.loadstonetp.LodestoneTP;
import io.github.marcel.loadstonetp.db.DatabaseManager;
import io.github.marcel.loadstonetp.model.Network;
import io.github.marcel.loadstonetp.model.Teleporter;
import io.github.marcel.loadstonetp.utils.ComponentFormatter;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class NetworkDialogs {

    private static final ClickCallback.Options CLICK_OPTIONS = ClickCallback.Options.builder()
            .uses(1)
            .lifetime(Duration.ofMinutes(5))
            .build();

    private NetworkDialogs() {}

    private static ActionButton openDialogButton(Component label, Component tooltip, int width, Supplier<Dialog> dialogSupplier) {
        return ActionButton.create(
                label,
                tooltip,
                width,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player player) {
                                player.showDialog(dialogSupplier.get());
                            }
                        },
                        CLICK_OPTIONS
                )
        );
    }

    private static ActionButton actionButton(Component label, Component tooltip, int width, DialogAction action) {
        return ActionButton.create(label, tooltip, width, action);
    }

    private static ActionButton backButton(String tooltip, Supplier<Dialog> dialogSupplier) {
        return openDialogButton(Component.text("Back"), Component.text(tooltip), 150, dialogSupplier);
    }

    private static ActionButton cancelButton(String tooltip, Supplier<Dialog> dialogSupplier) {
        return openDialogButton(Component.text("Cancel"), Component.text(tooltip), 150, dialogSupplier);
    }

    private static ActionButton closeButton(String tooltip) {
        return actionButton(
                Component.text("Close"),
                Component.text(tooltip),
                150,
                DialogAction.customClick((view, audience) -> {}, CLICK_OPTIONS)
        );
    }

    private static ActionButton saveButton(Component tooltip, int width, DialogAction action) {
        return actionButton(Component.text("Save"), tooltip, width, action);
    }

    private static ActionButton deleteButton(String label, String tooltip, int width, DialogAction action) {
        return actionButton(Component.text(label, NamedTextColor.RED), Component.text(tooltip), width, action);
    }

    private static DialogBody messageBody(Component component) {
        return DialogBody.plainMessage(component);
    }

    public static Dialog createNetworkManagementDialog(DatabaseManager db, Player player, LodestoneTP plugin) {
        String playerUuid = player.getUniqueId().toString();
        List<Network> networks = db.getNetworksByOwner(playerUuid);

        Component title = Component.text("My Networks", NamedTextColor.GOLD);

        List<ActionButton> buttons = new ArrayList<>();

        for (Network network : networks) {
            buttons.add(openDialogButton(
                    Component.text(network.name()),
                    buildNetworkSummary(network),
                    150,
                    () -> createNetworkActionDialog(db, network, player, plugin)
            ));
        }

        buttons.add(openDialogButton(
                Component.text("+ Create Network"),
                Component.text("Create a new network to organize teleporters"),
                150,
                () -> createNetworkDialog(db, playerUuid, plugin)
        ));

        ActionButton exitButton = closeButton("Close network management");

        if (networks.isEmpty()) {
            return Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(title)
                            .body(List.of(messageBody(ComponentFormatter.neutral("You haven't created any networks yet."))))
                            .canCloseWithEscape(true)
                            .build())
                    .type(DialogType.multiAction(List.of(
                            openDialogButton(
                                    Component.text("+ Create Network"),
                                    Component.text("Create a new network to organize teleporters"),
                                    150,
                                    () -> createNetworkDialog(db, playerUuid, plugin)
                            )), exitButton, 1))
            );
        }

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(
                                messageBody(Component.text("Select a network to manage.", NamedTextColor.WHITE))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, exitButton, 1))
        );
    }

    public static Dialog createNetworkDialog(DatabaseManager db, String ownerUuid, LodestoneTP plugin) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Create Network", NamedTextColor.GOLD))
                        .body(List.of(
                                messageBody(Component.text("Create a new network to organize your teleporters.", NamedTextColor.WHITE)),
                                messageBody(ComponentFormatter.neutral("Leave world filter empty to allow all worlds."))
                        ))
                        .inputs(List.of(
                                DialogInput.text("network_name", Component.text("Network Name"))
                                        .initial("")
                                        .maxLength(32)
                                        .width(200)
                                        .build(),
                                DialogInput.text("world_filter", Component.text("World Filter (optional)"))
                                        .initial("")
                                        .maxLength(32)
                                        .width(200)
                                        .build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        actionButton(
                                Component.text("Create"),
                                Component.text("Create the network"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (!(audience instanceof Player p)) return;
                                            String name = view.getText("network_name");
                                            String worldFilter = view.getText("world_filter");

                                            if (name == null || name.isBlank()) {
                                                                                                p.sendMessage(ComponentFormatter.error("Network name cannot be empty!"));
                                                return;
                                            }

                                            if (worldFilter != null && worldFilter.isBlank()) {
                                                worldFilter = null;
                                            }

                                            if (db.addNetwork(name.trim(), ownerUuid, worldFilter)) {
                                                                                                p.sendMessage(ComponentFormatter.success("Network \"" + name.trim() + "\" created!"));
                                                p.showDialog(createNetworkManagementDialog(db, p, plugin));
                                            } else {
                                                                                                p.sendMessage(ComponentFormatter.error("Failed to create network (name may be taken)!"));
                                            }
                                        },
                                                                                CLICK_OPTIONS
                                )
                        ),
                                                actionButton(
                                                                Component.text("Cancel"),
                                                                Component.text("Cancel network creation"),
                                                                150,
                                                                DialogAction.customClick(
                                                                                (view, audience) -> {
                                                                                        if (audience instanceof Player p) {
                                                                                                p.showDialog(createNetworkManagementDialog(db, p, plugin));
                                                                                        }
                                                                                },
                                                                                CLICK_OPTIONS
                                                                )
                                                )
                ))
        );
    }

    public static Dialog createNetworkActionDialog(DatabaseManager db, Network network, Player player, LodestoneTP plugin) {
        Component title = Component.text("Network: ", NamedTextColor.GOLD)
                .append(Component.text(network.name(), NamedTextColor.WHITE));

        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(openDialogButton(
                Component.text("Rename"),
                Component.text("Change the network name"),
                150,
                () -> createRenameNetworkDialog(db, network, player, plugin)
        ));

        buttons.add(openDialogButton(
                Component.text("Set Permission"),
                network.permissionNode() != null && !network.permissionNode().isBlank()
                        ? Component.text("Current: " + network.permissionNode())
                        : Component.text("No permission required"),
                150,
                () -> createPermissionNodeDialog(db, network, player, plugin)
        ));

        buttons.add(openDialogButton(
                Component.text("Delete Network", NamedTextColor.RED),
                Component.text("Permanently delete this network", NamedTextColor.RED),
                150,
                () -> createDeleteNetworkDialog(db, network, player, plugin)
        ));

        ActionButton backButton = backButton("Return to networks list", () -> createNetworkManagementDialog(db, player, plugin));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(
                                messageBody(
                                        buildNetworkSummary(network).color(NamedTextColor.GRAY)
                                )
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, backButton, 1))
        );
    }

    public static Dialog createRenameNetworkDialog(DatabaseManager db, Network network, Player player, LodestoneTP plugin) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Rename Network", NamedTextColor.GOLD))
                        .body(List.of(
                                messageBody(Component.text("Rename the network.", NamedTextColor.WHITE))
                        ))
                        .inputs(List.of(
                                DialogInput.text("new_name", Component.text("New Network Name"))
                                        .initial(network.name())
                                        .maxLength(32)
                                        .width(200)
                                        .build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        saveButton(
                                Component.text("Save the new name"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (!(audience instanceof Player p)) return;
                                            String newName = view.getText("new_name");

                                            if (newName == null || newName.isBlank()) {
                                                p.sendMessage(ComponentFormatter.error("Network name cannot be empty!"));
                                                return;
                                            }

                                            if (db.renameNetwork(network.id(), newName.trim())) {
                                                p.sendMessage(ComponentFormatter.success("Network renamed to \"" + newName.trim() + "\"!"));
                                                Network updated = new Network(
                                                        network.id(), newName.trim(),
                                                        network.ownerUuid(), network.worldFilter(), network.permissionNode()
                                                );
                                                p.showDialog(createNetworkActionDialog(db, updated, p, plugin));
                                            } else {
                                                p.sendMessage(ComponentFormatter.error("Failed to rename network (name may be taken)!"));
                                            }
                                        },
                                        CLICK_OPTIONS
                                )
                        ),
                        cancelButton("Cancel and go back", () -> createNetworkActionDialog(db, network, player, plugin))
                ))
        );
    }

    public static Dialog createDeleteNetworkDialog(DatabaseManager db, Network network, Player player, LodestoneTP plugin) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Delete Network?", NamedTextColor.RED))
                        .body(List.of(
                                messageBody(Component.text("Are you sure you want to delete '" + network.name() + "'?", NamedTextColor.WHITE)),
                                messageBody(ComponentFormatter.error("Teleporters in this network will be unassigned."))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        deleteButton(
                                "Yes, Delete",
                                "Permanently delete the network",
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (!(audience instanceof Player p)) return;
                                            if (db.deleteNetwork(network.id())) {
                                                p.sendMessage(ComponentFormatter.success("Network deleted successfully!"));
                                                p.showDialog(createNetworkManagementDialog(db, p, plugin));
                                            } else {
                                                p.sendMessage(ComponentFormatter.error("Failed to delete network!"));
                                            }
                                        },
                                        CLICK_OPTIONS
                                )
                        ),
                        cancelButton("Cancel deletion", () -> createNetworkActionDialog(db, network, player, plugin))
                ))
        );
    }

    public static Dialog createAssignNetworkDialog(DatabaseManager db, Teleporter teleporter, Player player, LodestoneTP plugin) {
        String playerUuid = player.getUniqueId().toString();
        List<Network> networks = db.getNetworksByOwner(playerUuid);

        Component title = Component.text("Assign to Network", NamedTextColor.GOLD)
                .append(Component.text(": " + teleporter.name(), NamedTextColor.WHITE));

        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(actionButton(
                Component.text("None"),
                Component.text("Remove from network"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            if (db.setTeleporterNetwork(teleporter.id(), null)) {
                                p.sendMessage(ComponentFormatter.success("Teleporter removed from network!"));
                                Teleporter updated = new Teleporter(
                                        teleporter.id(), teleporter.name(), teleporter.world(),
                                        teleporter.x(), teleporter.y(), teleporter.z(), teleporter.yaw(),
                                        teleporter.ownerUuid(), teleporter.isPublic(),
                                        teleporter.cooldownOverride(), null, teleporter.linkedTeleporterId()
                                );
                                p.showDialog(TeleporterDialogs.createAccessManagementDialog(db, updated, p, plugin));
                            } else {
                                                                p.sendMessage(ComponentFormatter.error("Failed to update teleporter!"));
                            }
                        },
                                                CLICK_OPTIONS
                )
        ));

        for (Network network : networks) {
                        buttons.add(actionButton(
                    Component.text(network.name()),
                    buildNetworkSummary(network),
                    150,
                    DialogAction.customClick(
                            (view, audience) -> {
                                if (!(audience instanceof Player p)) return;
                                if (db.setTeleporterNetwork(teleporter.id(), network.id())) {
                                                                        p.sendMessage(ComponentFormatter.success("Teleporter assigned to network \"" + network.name() + "\"!"));
                                    Teleporter updated = new Teleporter(
                                            teleporter.id(), teleporter.name(), teleporter.world(),
                                            teleporter.x(), teleporter.y(), teleporter.z(), teleporter.yaw(),
                                            teleporter.ownerUuid(), teleporter.isPublic(),
                                            teleporter.cooldownOverride(), network.id(), teleporter.linkedTeleporterId()
                                    );
                                    p.showDialog(TeleporterDialogs.createAccessManagementDialog(db, updated, p, plugin));
                                } else {
                                    p.sendMessage(ComponentFormatter.error("Failed to update teleporter!"));
                                }
                            },
                            CLICK_OPTIONS
                    )
            ));
        }

        ActionButton cancelButton = cancelButton(
                "Cancel assignment",
                () -> TeleporterDialogs.createAccessManagementDialog(db, teleporter, player, plugin)
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(
                                messageBody(Component.text("Select a network to assign this teleporter to.", NamedTextColor.WHITE))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, cancelButton, 1))
        );
    }

    public static Dialog createPermissionNodeDialog(DatabaseManager db, Network network, Player player, LodestoneTP plugin) {
        String currentPermission = network.permissionNode() != null ? network.permissionNode() : "";
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Network Permission", NamedTextColor.GOLD))
                        .body(List.of(
                                messageBody(Component.text("Set a permission node required to use this network.", NamedTextColor.WHITE)),
                                messageBody(ComponentFormatter.neutral("Leave empty to allow everyone who can access the teleporter."))
                        ))
                        .inputs(List.of(
                                DialogInput.text("permission_node", Component.text("Permission Node"))
                                        .initial(currentPermission)
                                        .maxLength(128)
                                        .width(220)
                                        .build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        saveButton(
                                Component.text("Save network permission"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (!(audience instanceof Player p)) return;
                                            String permissionNode = view.getText("permission_node");
                                            if (permissionNode != null && permissionNode.isBlank()) {
                                                permissionNode = null;
                                            }
                                            if (db.setNetworkPermissionNode(network.id(), permissionNode)) {
                                                                                                p.sendMessage(ComponentFormatter.success("Network permission updated."));
                                                Network updated = new Network(
                                                        network.id(),
                                                        network.name(),
                                                        network.ownerUuid(),
                                                        network.worldFilter(),
                                                        permissionNode
                                                );
                                                p.showDialog(createNetworkActionDialog(db, updated, p, plugin));
                                            } else {
                                                p.sendMessage(ComponentFormatter.error("Failed to update network permission."));
                                            }
                                        },
                                        CLICK_OPTIONS
                                )
                        ),
                        cancelButton("Cancel and go back", () -> createNetworkActionDialog(db, network, player, plugin))
                ))
        );
    }

    private static Component buildNetworkSummary(Network network) {
        String worldPart = network.worldFilter() != null ? "World: " + network.worldFilter() : "All worlds";
        String permissionPart = network.permissionNode() != null && !network.permissionNode().isBlank()
                ? "Permission: " + network.permissionNode()
                : "Permission: none";
        return Component.text(worldPart + " • " + permissionPart);
    }
}
