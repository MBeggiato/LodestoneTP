package io.github.marcel.loadstonetp.dialogs;

import io.github.marcel.loadstonetp.LodestoneTP;
import io.github.marcel.loadstonetp.db.DatabaseManager;
import io.github.marcel.loadstonetp.model.Network;
import io.github.marcel.loadstonetp.model.Teleporter;
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

import java.util.ArrayList;
import java.util.List;

public final class NetworkDialogs {

    public static Dialog createNetworkManagementDialog(DatabaseManager db, Player player, LodestoneTP plugin) {
        String playerUuid = player.getUniqueId().toString();
        List<Network> networks = db.getNetworksByOwner(playerUuid);

        Component title = Component.text("My Networks", NamedTextColor.GOLD);

        List<ActionButton> buttons = new ArrayList<>();

        for (Network network : networks) {
            buttons.add(ActionButton.create(
                    Component.text(network.name()),
                    buildNetworkSummary(network),
                    150,
                    DialogAction.customClick(
                            (view, audience) -> {
                                if (!(audience instanceof Player p)) return;
                                p.showDialog(createNetworkActionDialog(db, network, p, plugin));
                            },
                            ClickCallback.Options.builder()
                                    .uses(1)
                                    .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                    .build()
                    )
            ));
        }

        buttons.add(ActionButton.create(
                Component.text("+ Create Network"),
                Component.text("Create a new network to organize teleporters"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            p.showDialog(createNetworkDialog(db, playerUuid, plugin));
                        },
                        ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                .build()
                )
        ));

        ActionButton exitButton = ActionButton.create(
                Component.text("Close"),
                Component.text("Close network management"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {},
                        ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                .build()
                )
        );

        if (networks.isEmpty()) {
            return Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(title)
                            .body(List.of(
                                    DialogBody.plainMessage(Component.text("You haven't created any networks yet.", NamedTextColor.GRAY))
                            ))
                            .canCloseWithEscape(true)
                            .build())
                    .type(DialogType.multiAction(List.of(
                            ActionButton.create(
                                    Component.text("+ Create Network"),
                                    Component.text("Create a new network to organize teleporters"),
                                    150,
                                    DialogAction.customClick(
                                            (view, audience) -> {
                                                if (!(audience instanceof Player p)) return;
                                                p.showDialog(createNetworkDialog(db, playerUuid, plugin));
                                            },
                                            ClickCallback.Options.builder()
                                                    .uses(1)
                                                    .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                                    .build()
                                    )
                    )), exitButton, 1))
            );
        }

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Select a network to manage.", NamedTextColor.WHITE))
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
                                DialogBody.plainMessage(Component.text("Create a new network to organize your teleporters.", NamedTextColor.WHITE)),
                                DialogBody.plainMessage(Component.text("Leave world filter empty to allow all worlds.", NamedTextColor.GRAY))
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
                        ActionButton.create(
                                Component.text("Create"),
                                Component.text("Create the network"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (!(audience instanceof Player p)) return;
                                            String name = view.getText("network_name");
                                            String worldFilter = view.getText("world_filter");

                                            if (name == null || name.isBlank()) {
                                                p.sendMessage(Component.text("Network name cannot be empty!", NamedTextColor.RED));
                                                return;
                                            }

                                            if (worldFilter != null && worldFilter.isBlank()) {
                                                worldFilter = null;
                                            }

                                            if (db.addNetwork(name.trim(), ownerUuid, worldFilter)) {
                                                p.sendMessage(Component.text("Network \"" + name.trim() + "\" created!", NamedTextColor.GREEN));
                                                p.showDialog(createNetworkManagementDialog(db, p, plugin));
                                            } else {
                                                p.sendMessage(Component.text("Failed to create network (name may be taken)!", NamedTextColor.RED));
                                            }
                                        },
                                        ClickCallback.Options.builder()
                                                .uses(1)
                                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                                .build()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel"),
                                Component.text("Cancel network creation"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(createNetworkManagementDialog(db, p, plugin));
                                            }
                                        },
                                        ClickCallback.Options.builder()
                                                .uses(1)
                                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                                .build()
                                )
                        )
                ))
        );
    }

    public static Dialog createNetworkActionDialog(DatabaseManager db, Network network, Player player, LodestoneTP plugin) {
        Component title = Component.text("Network: ", NamedTextColor.GOLD)
                .append(Component.text(network.name(), NamedTextColor.WHITE));

        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(ActionButton.create(
                Component.text("Rename"),
                Component.text("Change the network name"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            p.showDialog(createRenameNetworkDialog(db, network, p, plugin));
                        },
                        ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                .build()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text("Set Permission"),
                network.permissionNode() != null && !network.permissionNode().isBlank()
                        ? Component.text("Current: " + network.permissionNode())
                        : Component.text("No permission required"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            p.showDialog(createPermissionNodeDialog(db, network, p, plugin));
                        },
                        ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                .build()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text("Delete Network"),
                Component.text("Permanently delete this network", NamedTextColor.RED),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            p.showDialog(createDeleteNetworkDialog(db, network, p, plugin));
                        },
                        ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                .build()
                )
        ));

        ActionButton backButton = ActionButton.create(
                Component.text("Back"),
                Component.text("Return to networks list"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            p.showDialog(createNetworkManagementDialog(db, p, plugin));
                        },
                        ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                .build()
                )
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(
                                DialogBody.plainMessage(
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
                                DialogBody.plainMessage(Component.text("Rename the network.", NamedTextColor.WHITE))
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
                        ActionButton.create(
                                Component.text("Save"),
                                Component.text("Save the new name"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (!(audience instanceof Player p)) return;
                                            String newName = view.getText("new_name");

                                            if (newName == null || newName.isBlank()) {
                                                p.sendMessage(Component.text("Network name cannot be empty!", NamedTextColor.RED));
                                                return;
                                            }

                                            if (db.renameNetwork(network.id(), newName.trim())) {
                                                p.sendMessage(Component.text("Network renamed to \"" + newName.trim() + "\"!", NamedTextColor.GREEN));
                                                Network updated = new Network(
                                                        network.id(), newName.trim(),
                                                        network.ownerUuid(), network.worldFilter(), network.permissionNode()
                                                );
                                                p.showDialog(createNetworkActionDialog(db, updated, p, plugin));
                                            } else {
                                                p.sendMessage(Component.text("Failed to rename network (name may be taken)!", NamedTextColor.RED));
                                            }
                                        },
                                        ClickCallback.Options.builder()
                                                .uses(1)
                                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                                .build()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel"),
                                Component.text("Cancel and go back"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(createNetworkActionDialog(db, network, p, plugin));
                                            }
                                        },
                                        ClickCallback.Options.builder()
                                                .uses(1)
                                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                                .build()
                                )
                        )
                ))
        );
    }

    public static Dialog createDeleteNetworkDialog(DatabaseManager db, Network network, Player player, LodestoneTP plugin) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Delete Network?", NamedTextColor.RED))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Are you sure you want to delete '" + network.name() + "'?", NamedTextColor.WHITE)),
                                DialogBody.plainMessage(Component.text("Teleporters in this network will be unassigned.", NamedTextColor.RED))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Yes, Delete", NamedTextColor.RED),
                                Component.text("Permanently delete the network"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (!(audience instanceof Player p)) return;
                                            if (db.deleteNetwork(network.id())) {
                                                p.sendMessage(Component.text("Network deleted successfully!", NamedTextColor.GREEN));
                                                p.showDialog(createNetworkManagementDialog(db, p, plugin));
                                            } else {
                                                p.sendMessage(Component.text("Failed to delete network!", NamedTextColor.RED));
                                            }
                                        },
                                        ClickCallback.Options.builder()
                                                .uses(1)
                                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                                .build()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel"),
                                Component.text("Cancel deletion"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(createNetworkActionDialog(db, network, p, plugin));
                                            }
                                        },
                                        ClickCallback.Options.builder()
                                                .uses(1)
                                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                                .build()
                                )
                        )
                ))
        );
    }

    public static Dialog createAssignNetworkDialog(DatabaseManager db, Teleporter teleporter, Player player, LodestoneTP plugin) {
        String playerUuid = player.getUniqueId().toString();
        List<Network> networks = db.getNetworksByOwner(playerUuid);

        Component title = Component.text("Assign to Network", NamedTextColor.GOLD)
                .append(Component.text(": " + teleporter.name(), NamedTextColor.WHITE));

        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(ActionButton.create(
                Component.text("None"),
                Component.text("Remove from network"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            if (db.setTeleporterNetwork(teleporter.id(), null)) {
                                p.sendMessage(Component.text("Teleporter removed from network!", NamedTextColor.GREEN));
                                Teleporter updated = new Teleporter(
                                        teleporter.id(), teleporter.name(), teleporter.world(),
                                        teleporter.x(), teleporter.y(), teleporter.z(), teleporter.yaw(),
                                        teleporter.ownerUuid(), teleporter.isPublic(),
                                        teleporter.cooldownOverride(), null, teleporter.linkedTeleporterId()
                                );
                                p.showDialog(TeleporterDialogs.createAccessManagementDialog(db, updated, p, plugin));
                            } else {
                                p.sendMessage(Component.text("Failed to update teleporter!", NamedTextColor.RED));
                            }
                        },
                        ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                .build()
                )
        ));

        for (Network network : networks) {
            buttons.add(ActionButton.create(
                    Component.text(network.name()),
                    buildNetworkSummary(network),
                    150,
                    DialogAction.customClick(
                            (view, audience) -> {
                                if (!(audience instanceof Player p)) return;
                                if (db.setTeleporterNetwork(teleporter.id(), network.id())) {
                                    p.sendMessage(Component.text("Teleporter assigned to network \"" + network.name() + "\"!", NamedTextColor.GREEN));
                                    Teleporter updated = new Teleporter(
                                            teleporter.id(), teleporter.name(), teleporter.world(),
                                            teleporter.x(), teleporter.y(), teleporter.z(), teleporter.yaw(),
                                            teleporter.ownerUuid(), teleporter.isPublic(),
                                            teleporter.cooldownOverride(), network.id(), teleporter.linkedTeleporterId()
                                    );
                                    p.showDialog(TeleporterDialogs.createAccessManagementDialog(db, updated, p, plugin));
                                } else {
                                    p.sendMessage(Component.text("Failed to update teleporter!", NamedTextColor.RED));
                                }
                            },
                            ClickCallback.Options.builder()
                                    .uses(1)
                                    .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                    .build()
                    )
            ));
        }

        ActionButton cancelButton = ActionButton.create(
                Component.text("Cancel"),
                Component.text("Cancel assignment"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(TeleporterDialogs.createAccessManagementDialog(db, teleporter, p, plugin));
                            }
                        },
                        ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                .build()
                )
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Select a network to assign this teleporter to.", NamedTextColor.WHITE))
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
                                DialogBody.plainMessage(Component.text("Set a permission node required to use this network.", NamedTextColor.WHITE)),
                                DialogBody.plainMessage(Component.text("Leave empty to allow everyone who can access the teleporter.", NamedTextColor.GRAY))
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
                        ActionButton.create(
                                Component.text("Save"),
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
                                                p.sendMessage(Component.text("Network permission updated.", NamedTextColor.GREEN));
                                                Network updated = new Network(
                                                        network.id(),
                                                        network.name(),
                                                        network.ownerUuid(),
                                                        network.worldFilter(),
                                                        permissionNode
                                                );
                                                p.showDialog(createNetworkActionDialog(db, updated, p, plugin));
                                            } else {
                                                p.sendMessage(Component.text("Failed to update network permission.", NamedTextColor.RED));
                                            }
                                        },
                                        ClickCallback.Options.builder()
                                                .uses(1)
                                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                                .build()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel"),
                                Component.text("Cancel and go back"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (!(audience instanceof Player p)) return;
                                            p.showDialog(createNetworkActionDialog(db, network, p, plugin));
                                        },
                                        ClickCallback.Options.builder()
                                                .uses(1)
                                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                                .build()
                                )
                        )
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
