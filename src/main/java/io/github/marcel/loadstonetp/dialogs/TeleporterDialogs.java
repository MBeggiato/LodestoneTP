package io.github.marcel.loadstonetp.dialogs;

import io.github.marcel.loadstonetp.CooldownManager;
import io.github.marcel.loadstonetp.LodestoneTP;
import io.github.marcel.loadstonetp.db.DatabaseManager;
import io.github.marcel.loadstonetp.model.Teleporter;
import io.github.marcel.loadstonetp.utils.ComponentFormatter;
import io.github.marcel.loadstonetp.utils.PermissionChecker;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


public final class TeleporterDialogs {

    private static final String NAME_INPUT_KEY = "teleporter_name";
    private static final String PLAYER_INPUT_KEY = "player_name";
    private static final ClickCallback.Options CLICK_OPTIONS = ClickCallback.Options.builder()
        .uses(1)
        .lifetime(ClickCallback.DEFAULT_LIFETIME)
        .build();
    private static final ClickCallback.Options NOOP_CLICK_OPTIONS = ClickCallback.Options.builder()
        .uses(0)
        .lifetime(ClickCallback.DEFAULT_LIFETIME)
        .build();

    private TeleporterDialogs() {}

    private static ActionButton actionButton(Component label, Component tooltip, int width, DialogAction action) {
        return ActionButton.create(label, tooltip, width, action);
    }

    private static ActionButton actionButton(Component label, Component tooltip, int width, DialogActionCallback callback) {
        return actionButton(label, tooltip, width, DialogAction.customClick(callback, CLICK_OPTIONS));
    }

    private static ActionButton openDialogButton(Component label, Component tooltip, int width, java.util.function.Supplier<Dialog> dialogSupplier) {
        return actionButton(
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

    private static ActionButton backButton(String tooltip, java.util.function.Supplier<Dialog> dialogSupplier) {
        return openDialogButton(Component.text("Back"), Component.text(tooltip), 150, dialogSupplier);
    }

    private static ActionButton closeButton(String tooltip) {
        return actionButton(
                Component.text("Close"),
                Component.text(tooltip),
                200,
                DialogAction.customClick((view, audience) -> {}, CLICK_OPTIONS)
        );
    }

    private static ActionButton noopButton(Component label, Component tooltip, int width) {
        return actionButton(label, tooltip, width, DialogAction.customClick((view, audience) -> {}, NOOP_CLICK_OPTIONS));
    }

    private static DialogBody messageBody(Component component) {
        return DialogBody.plainMessage(component);
    }

    private static Component teleporterTitle(String prefix, Teleporter teleporter) {
        return Component.text(prefix, NamedTextColor.GOLD)
                .append(Component.text(teleporter.name(), NamedTextColor.WHITE));
    }

    private static Component teleporterCoordinates(Teleporter teleporter, NamedTextColor color) {
        return Component.text(
                teleporter.world() + " (" + teleporter.x() + ", " + teleporter.y() + ", " + teleporter.z() + ")",
                color
        );
    }

    private static Component teleporterTooltip(Teleporter from, Teleporter to, LodestoneTP plugin) {
        return Component.text(to.world() + " (" + to.x() + ", " + to.y() + ", " + to.z() + ") — ")
                .append(getCostPreview(from, to, plugin));
    }

    private static double horizontalDistance(Teleporter from, Teleporter to) {
        double dx = from.x() - to.x();
        double dz = from.z() - to.z();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static int resolveDistanceCost(Teleporter from, Teleporter to, LodestoneTP plugin) {
        if (!from.world().equals(to.world())) {
            return plugin.getConfig().getInt("cost.distance.cross-world-cost", 10);
        }

        List<?> tiers = plugin.getConfig().getList("cost.distance.tiers");
        if (tiers == null) {
            return 0;
        }

        double distance = horizontalDistance(from, to);
        for (Object tierObj : tiers) {
            if (tierObj instanceof java.util.Map<?, ?> tier) {
                Object maxDistObj = tier.get("max-distance");
                Object costObj = tier.get("cost");
                int maxDistance = maxDistObj instanceof Number n ? n.intValue() : -1;
                int tierCost = costObj instanceof Number n ? n.intValue() : 0;
                if (maxDistance == -1 || distance <= maxDistance) {
                    return tierCost;
                }
            }
        }
        return 0;
    }

    private static String formatDistanceText(Teleporter from, Teleporter to) {
        if (!from.world().equals(to.world())) {
            return "Cross-dimension (" + from.world() + " → " + to.world() + ")";
        }

        return (int) Math.round(horizontalDistance(from, to)) + " blocks";
    }

    public static Dialog createNewTeleporterDialog(DatabaseManager db, Location lodestoneLocation, float playerYaw, String ownerUuid, boolean defaultPublic, LodestoneTP plugin) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Create Teleporter", NamedTextColor.GOLD))
                        .body(List.of(
                        messageBody(Component.text("Enter a unique name for this teleporter.", NamedTextColor.WHITE)),
                        messageBody(ComponentFormatter.neutral(defaultPublic ? "Visibility: Public" : "Visibility: Private"))
                        ))
                        .inputs(List.of(
                                DialogInput.text(NAME_INPUT_KEY, Component.text("Teleporter Name"))
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
                                Component.text("Create the teleporter"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (!(audience instanceof Player player)) return;

                                            String name = view.getText(NAME_INPUT_KEY);
                                            if (name == null || name.isBlank()) {
                                                player.sendMessage(ComponentFormatter.error("Teleporter name cannot be empty!"));
                                                return;
                                            }

                                            name = name.trim();

                                            if (db.isNameTaken(name)) {
                                                player.sendMessage(ComponentFormatter.error("A teleporter with that name already exists!"));
                                                return;
                                            }

                                            if (db.addTeleporter(name, lodestoneLocation, playerYaw, ownerUuid, defaultPublic)) {
                                                player.sendMessage(ComponentFormatter.success("Teleporter ")
                                                        .append(ComponentFormatter.teleporterName(name))
                                                        .append(ComponentFormatter.success(" created!")));

                                                plugin.getTeleportEffects().refreshTeleporterLocations();
                                                plugin.getTeleportEffects().placeLightBlock(lodestoneLocation);
                                                // Advancements
                                                if (plugin.getAdvancementManager() != null) {
                                                    plugin.getAdvancementManager().grant(player, "root");
                                                    plugin.getAdvancementManager().grant(player, "engineer");
                                                    if (!defaultPublic) {
                                                        plugin.getAdvancementManager().grant(player, "private_line");
                                                    }
                                                    int count = db.getTeleporterCountByOwner(ownerUuid);
                                                    if (count >= 5) {
                                                        plugin.getAdvancementManager().grant(player, "network_builder");
                                                    }
                                                }
                                            } else {
                                                player.sendMessage(ComponentFormatter.error("Failed to create teleporter. A teleporter may already exist at this location."));
                                            }
                                        },
                                        CLICK_OPTIONS
                                )
                        ),
                        actionButton(
                            Component.text("Cancel"),
                            Component.text("Cancel creation"),
                            150,
                            DialogAction.customClick(
                                (view, audience) -> {
                                    if (audience instanceof Player player) {
                                    player.sendMessage(ComponentFormatter.neutral("Teleporter creation cancelled."));
                                    }
                                },
                                CLICK_OPTIONS
                            )
                        )
                ))
        );
    }

    public static Dialog createOwnerManagementDialog(DatabaseManager db, Teleporter current, Player player, LodestoneTP plugin) {
        Component title = teleporterTitle("Teleporter: ", current);

        String playerUuid = player.getUniqueId().toString();
        List<Teleporter> accessible = db.getAccessibleTeleporters(playerUuid);

        List<ActionButton> buttons = new ArrayList<>();

        // Destination buttons first — the primary content
        for (Teleporter tp : accessible) {
            if (tp.id() == current.id()) continue;

            Component tooltip = teleporterTooltip(current, tp, plugin);

            buttons.add(ActionButton.create(
                    Component.text(tp.name()),
                    tooltip,
                    200,
                    DialogAction.customClick(
                            (view, audience) -> {
                                if (!(audience instanceof Player p)) return;
                                Dialog confirmDialog = createTeleportConfirmDialog(db, current, tp, p, plugin);
                                p.showDialog(confirmDialog);
                            },
                                CLICK_OPTIONS
                    )
            ));
        }

        // Management buttons — secondary actions after destinations
        buttons.add(ActionButton.create(
                Component.text("Manage Access", NamedTextColor.GOLD),
                Component.text("Manage visibility and whitelist"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            Dialog accessDialog = createAccessManagementDialog(db, current, p, plugin);
                            p.showDialog(accessDialog);
                        },
                        CLICK_OPTIONS
                )
        ));

        buttons.add(ActionButton.create(
                Component.text("Delete", NamedTextColor.RED),
                Component.text("Delete this teleporter"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            Dialog deleteDialog = createDeleteConfirmationDialog(db, current, plugin);
                            p.showDialog(deleteDialog);
                        },
                        CLICK_OPTIONS
                )
        ));

        ActionButton exitButton = closeButton("Close the menu");

        List<DialogBody> body = new ArrayList<>();
        body.add(messageBody(ComponentFormatter.neutral("Visibility: " + (current.isPublic() ? "Public" : "Private"))));
        if (buttons.size() <= 2) {
            body.add(messageBody(ComponentFormatter.neutral("No other teleporters available yet.")));
        }

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .body(body)
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, exitButton, 2))
        );
    }

    private enum SortMode {
        ALPHABETICAL,
        DISTANCE,
        MOST_USED;

        private SortMode next() {
            return switch (this) {
                case ALPHABETICAL -> DISTANCE;
                case DISTANCE -> MOST_USED;
                case MOST_USED -> ALPHABETICAL;
            };
        }

        private String displayName() {
            return switch (this) {
                case ALPHABETICAL -> "Alphabetical";
                case DISTANCE -> "Distance";
                case MOST_USED -> "Most Used";
            };
        }
    }

    public static Dialog createTeleportDialog(DatabaseManager db, Teleporter current, Player player, LodestoneTP plugin) {
        return createTeleportDialog(db, current, player, plugin, SortMode.ALPHABETICAL);
    }

    private static Dialog createTeleportDialog(DatabaseManager db, Teleporter current, Player player, LodestoneTP plugin, SortMode sortMode) {
        String playerUuid = player.getUniqueId().toString();
        boolean isOwner = playerUuid.equals(current.ownerUuid());
        boolean isAdmin = PermissionChecker.isAdmin(player);

        List<Teleporter> baseAccessible = isAdmin
                ? db.getAllTeleporters()
                : db.getAccessibleTeleporters(playerUuid);

        List<Teleporter> accessible = baseAccessible.stream()
                .filter(tp -> tp.id() != current.id())
                .filter(tp -> hasNetworkPermission(db, tp, player))
                .toList();

        List<Teleporter> favorites = db.getFavoriteTeleporters(playerUuid).stream()
                .filter(tp -> tp.id() != current.id())
                .filter(tp -> hasNetworkPermission(db, tp, player))
                .toList();
        Set<Integer> favoriteIds = favorites.stream().map(Teleporter::id).collect(Collectors.toSet());

        Map<Integer, Integer> usageCounts = db.getUsageCounts(playerUuid);
        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(ActionButton.create(
                Component.text("Sort: " + sortMode.displayName(), NamedTextColor.AQUA),
                Component.text("Switch to " + sortMode.next().displayName()),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            p.showDialog(createTeleportDialog(db, current, p, plugin, sortMode.next()));
                        },
                        CLICK_OPTIONS
                )
        ));

        List<Teleporter> sortedFavorites = sortTeleporters(favorites, sortMode, current, usageCounts);
        if (!sortedFavorites.isEmpty()) {
            buttons.add(createGroupButton(db, current, player, plugin, sortedFavorites, "★ Favorites", NamedTextColor.GOLD, sortMode));
        }

        Map<Integer, List<Teleporter>> networkMap = new HashMap<>();
        for (Teleporter tp : accessible) {
            if (favoriteIds.contains(tp.id())) continue;
            if (tp.networkId() != null) {
                networkMap.computeIfAbsent(tp.networkId(), key -> new ArrayList<>()).add(tp);
            }
        }

        Set<Integer> categorizedIds = new HashSet<>(favoriteIds);
        for (Map.Entry<Integer, List<Teleporter>> entry : networkMap.entrySet()) {
            io.github.marcel.loadstonetp.model.Network network = db.getNetwork(entry.getKey());
            if (network == null) continue;
            List<Teleporter> sortedNetworkTeleporters = sortTeleporters(entry.getValue(), sortMode, current, usageCounts);
            buttons.add(createGroupButton(db, current, player, plugin, sortedNetworkTeleporters, network.name(), NamedTextColor.AQUA, sortMode));
            categorizedIds.addAll(sortedNetworkTeleporters.stream().map(Teleporter::id).toList());
        }

        List<Teleporter> uncategorized = accessible.stream()
                .filter(tp -> !categorizedIds.contains(tp.id()))
                .toList();
        List<Teleporter> sortedUncategorized = sortTeleporters(uncategorized, sortMode, current, usageCounts);
        if (!sortedUncategorized.isEmpty()) {
            buttons.add(createGroupButton(db, current, player, plugin, sortedUncategorized, "Other Teleporters", NamedTextColor.GRAY, sortMode));
        }

        buttons.add(openDialogButton(
                Component.text("Manage Favorites", NamedTextColor.GOLD),
                Component.text("Toggle favorite status"),
                200,
                () -> createFavoriteManagementDialog(db, current, player, plugin, sortMode)
        ));

        if (isOwner || isAdmin) {
            buttons.add(openDialogButton(
                    Component.text("Manage Access", NamedTextColor.GOLD),
                    Component.text("Manage visibility and whitelist"),
                    200,
                    () -> createAccessManagementDialog(db, current, player, plugin)
            ));

            buttons.add(openDialogButton(
                    Component.text("Delete", NamedTextColor.RED),
                    Component.text("Delete this teleporter"),
                    200,
                    () -> createDeleteConfirmationDialog(db, current, plugin)
            ));
        }

        List<DialogBody> body = new ArrayList<>();
        body.add(messageBody(ComponentFormatter.neutral("Visibility: " + (current.isPublic() ? "Public" : "Private"))));
        body.add(messageBody(ComponentFormatter.neutral("Sort: " + sortMode.displayName())));
        if (current.linkedTeleporterId() != null) {
            Teleporter linked = db.getTeleporter(current.linkedTeleporterId());
            body.add(messageBody(Component.text("Linked: ↔ " + (linked != null ? linked.name() : "unknown"), NamedTextColor.AQUA)));
        }
        if (buttons.size() <= 2) {
            body.add(messageBody(ComponentFormatter.neutral("No other teleporters available.")));
        }

        Component title = teleporterTitle("Teleporter: ", current);

        ActionButton exitButton = closeButton("Close the menu");

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .body(body)
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, exitButton, 2))
        );
    }

    private static ActionButton createGroupButton(DatabaseManager db, Teleporter current, Player player, LodestoneTP plugin, List<Teleporter> teleporters, String groupName, NamedTextColor color, SortMode sortMode) {
        return ActionButton.create(
                Component.text(groupName + " (" + teleporters.size() + ")", color),
                Component.text("Open group"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            p.showDialog(createGroupDialog(db, current, p, plugin, teleporters, groupName, sortMode));
                        },
                    CLICK_OPTIONS
                )
        );
    }

    private static Dialog createGroupDialog(DatabaseManager db, Teleporter current, Player player, LodestoneTP plugin, List<Teleporter> teleporters, String groupName, SortMode sortMode) {
        String playerUuid = player.getUniqueId().toString();
        Set<Integer> favoriteIds = db.getFavoriteIds(playerUuid);
        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(noopButton(
                Component.text((current.linkedTeleporterId() != null ? "↔ " : "") + current.name(), NamedTextColor.GRAY),
                Component.text("You are here"),
                160
        ));

        buttons.add(noopButton(Component.text(" "), Component.text(" "), 40));

        for (Teleporter tp : teleporters) {
            boolean favorite = favoriteIds.contains(tp.id());
            String name = (favorite ? "★ " : "") + (tp.linkedTeleporterId() != null ? "↔ " : "") + tp.name();
            Component tooltip = teleporterTooltip(current, tp, plugin);

            buttons.add(ActionButton.create(
                    Component.text(name),
                    tooltip,
                160,
                    DialogAction.customClick(
                            (view, audience) -> {
                                if (!(audience instanceof Player p)) return;
                                p.showDialog(createTeleportConfirmDialog(db, current, tp, p, plugin));
                            },
                                CLICK_OPTIONS
                    )
            ));

            buttons.add(ActionButton.create(
                Component.text(favorite ? "🗑" : "★", favorite ? NamedTextColor.RED : NamedTextColor.YELLOW),
                Component.text(favorite ? "Remove from favorites" : "Add to favorites"),
                40,
                    DialogAction.customClick(
                            (view, audience) -> {
                                if (!(audience instanceof Player p)) return;
                                if (db.isFavorite(playerUuid, tp.id())) {
                                    db.removeFavorite(playerUuid, tp.id());
                                } else {
                                    db.addFavorite(playerUuid, tp.id());
                                }
                                p.showDialog(createGroupDialog(db, current, p, plugin, teleporters, groupName, sortMode));
                            },
                                CLICK_OPTIONS
                    )
            ));
        }

        ActionButton backButton = backButton("Return to main menu", () -> createTeleportDialog(db, current, player, plugin, sortMode));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(groupName, NamedTextColor.AQUA))
                .body(List.of(messageBody(Component.text("Left: teleport • Right: favorite/remove", NamedTextColor.WHITE))))
                        .canCloseWithEscape(true)
                        .build())
            .type(DialogType.multiAction(buttons, backButton, 2))
        );
    }

    private static Dialog createFavoriteManagementDialog(DatabaseManager db, Teleporter current, Player player, LodestoneTP plugin, SortMode sortMode) {
        String playerUuid = player.getUniqueId().toString();
        List<Teleporter> accessible = db.getAccessibleTeleporters(playerUuid).stream()
                .filter(tp -> tp.id() != current.id())
                .filter(tp -> hasNetworkPermission(db, tp, player))
                .toList();
        List<Teleporter> sorted = sortTeleporters(accessible, sortMode, current, db.getUsageCounts(playerUuid));
        Set<Integer> favoriteIds = db.getFavoriteIds(playerUuid);

        List<ActionButton> buttons = new ArrayList<>();
        for (Teleporter tp : sorted) {
            boolean favorite = favoriteIds.contains(tp.id());
            buttons.add(ActionButton.create(
                    Component.text((favorite ? "★ " : "☆ ") + tp.name(), favorite ? NamedTextColor.GOLD : NamedTextColor.GRAY),
                    Component.text("Toggle favorite"),
                    200,
                    DialogAction.customClick(
                            (view, audience) -> {
                                if (!(audience instanceof Player p)) return;
                                if (favoriteIds.contains(tp.id())) {
                                    db.removeFavorite(playerUuid, tp.id());
                                } else {
                                    db.addFavorite(playerUuid, tp.id());
                                }
                                p.showDialog(createFavoriteManagementDialog(db, current, p, plugin, sortMode));
                            },
                                CLICK_OPTIONS
                    )
            ));
        }

        ActionButton backButton = backButton("Return to teleporter menu", () -> createTeleportDialog(db, current, player, plugin, sortMode));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Manage Favorites", NamedTextColor.GOLD))
                .body(List.of(messageBody(Component.text("Toggle favorites for destinations.", NamedTextColor.WHITE))))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, backButton, 1))
        );
    }

    private static List<Teleporter> sortTeleporters(List<Teleporter> teleporters, SortMode sortMode, Teleporter current, Map<Integer, Integer> usageCounts) {
        Comparator<Teleporter> comparator = switch (sortMode) {
            case ALPHABETICAL -> Comparator.comparing(tp -> tp.name().toLowerCase(java.util.Locale.ROOT));
            case DISTANCE -> Comparator
                    .comparingDouble((Teleporter tp) -> {
                        if (!tp.world().equals(current.world())) return Double.MAX_VALUE;
                        double dx = tp.x() - current.x();
                        double dz = tp.z() - current.z();
                        return Math.sqrt(dx * dx + dz * dz);
                    })
                    .thenComparing(tp -> tp.name().toLowerCase(java.util.Locale.ROOT));
            case MOST_USED -> Comparator
                    .comparingInt((Teleporter tp) -> usageCounts.getOrDefault(tp.id(), 0))
                    .reversed()
                    .thenComparing(tp -> tp.name().toLowerCase(java.util.Locale.ROOT));
        };
        return teleporters.stream().sorted(comparator).toList();
    }

    private static boolean hasNetworkPermission(DatabaseManager db, Teleporter tp, Player player) {
        if (PermissionChecker.isAdmin(player) || player.hasPermission("lodestonetp.network.bypass")) {
            return true;
        }
        if (tp.networkId() == null) {
            return true;
        }
        io.github.marcel.loadstonetp.model.Network network = db.getNetwork(tp.networkId());
        if (network == null || network.permissionNode() == null || network.permissionNode().isBlank()) {
            return true;
        }
        return player.hasPermission(network.permissionNode());
    }

    public static Dialog createAccessManagementDialog(DatabaseManager db, Teleporter teleporter, Player player, LodestoneTP plugin) {
        Component title = teleporterTitle("Access: ", teleporter);

        String visibilityStatus = teleporter.isPublic() ? "Public" : "Private";

        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(ActionButton.create(
                Component.text("Toggle " + (teleporter.isPublic() ? "Private" : "Public")),
                Component.text("Switch visibility to " + (teleporter.isPublic() ? "private" : "public")),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            boolean newValue = !teleporter.isPublic();
                            db.setPublic(teleporter.id(), newValue);
                            p.sendMessage(ComponentFormatter.success("Teleporter is now " + (newValue ? "Public" : "Private")));
                            Teleporter updated = new Teleporter(
                                    teleporter.id(), teleporter.name(), teleporter.world(),
                                    teleporter.x(), teleporter.y(), teleporter.z(), teleporter.yaw(),
                                    teleporter.ownerUuid(), newValue, teleporter.cooldownOverride(), teleporter.networkId(), teleporter.linkedTeleporterId()
                            );
                            Dialog refreshed = createAccessManagementDialog(db, updated, p, plugin);
                            p.showDialog(refreshed);
                        },
                        CLICK_OPTIONS
                )
        ));

                if (PermissionChecker.isAdmin(player) || player.hasPermission("lodestonetp.manage_cooldowns")) {
            buttons.add(ActionButton.create(
                    Component.text("Set Cooldown"),
                    Component.text(teleporter.cooldownOverride() != null ? "Override: " + teleporter.cooldownOverride() + "s" : "Use global cooldown"),
                    150,
                    DialogAction.customClick(
                            (view, audience) -> {
                                if (!(audience instanceof Player p)) return;
                                p.showDialog(createCooldownOverrideDialog(db, teleporter, p, plugin));
                            },
                                CLICK_OPTIONS
                    )
            ));
        }

        buttons.add(ActionButton.create(
                Component.text("Assign Network"),
                Component.text(teleporter.networkId() != null ? "Assigned to network" : "Not assigned to network"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            p.showDialog(NetworkDialogs.createAssignNetworkDialog(db, teleporter, p, plugin));
                        },
                        CLICK_OPTIONS
                )
        ));

        if (teleporter.linkedTeleporterId() != null) {
            Teleporter linked = db.getTeleporter(teleporter.linkedTeleporterId());
            buttons.add(ActionButton.create(
                    Component.text("Unlink"),
                    Component.text("Unlink from " + (linked != null ? linked.name() : "unknown")),
                    150,
                    DialogAction.customClick(
                            (view, audience) -> {
                                if (!(audience instanceof Player p)) return;
                                Dialog confirmDialog = createUnlinkConfirmDialog(db, teleporter, p, plugin);
                                p.showDialog(confirmDialog);
                            },
                                CLICK_OPTIONS
                    )
            ));
        } else {
            buttons.add(ActionButton.create(
                    Component.text("Link Teleporter"),
                    Component.text("Link to another teleporter"),
                    150,
                    DialogAction.customClick(
                            (view, audience) -> {
                                if (!(audience instanceof Player p)) return;
                                p.showDialog(createLinkTeleporterDialog(db, teleporter, p, plugin));
                            },
                                CLICK_OPTIONS
                    )
            ));
        }

        buttons.add(openDialogButton(
            Component.text("Add Player"),
            Component.text("Add a player to the whitelist"),
            150,
            () -> createAddPlayerDialog(db, teleporter, player, plugin)
        ));

        List<String> accessUuids = db.getAccessList(teleporter.id());
        for (String uuid : accessUuids) {
            String playerName = resolvePlayerName(uuid);
                buttons.add(actionButton(
                    Component.text(playerName + " \u2715", NamedTextColor.RED),
                    Component.text("Remove " + playerName + " from whitelist"),
                    200,
                    (view, audience) -> {
                    if (!(audience instanceof Player p)) return;
                    db.removeAccess(teleporter.id(), uuid);
                    p.sendMessage(ComponentFormatter.warning("Removed " + playerName + " from whitelist."));
                    p.showDialog(createAccessManagementDialog(db, teleporter, p, plugin));
                    }
                ));
        }

            ActionButton backButton = backButton("Return to management menu", () -> createTeleportDialog(db, teleporter, player, plugin));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(
                                messageBody(ComponentFormatter.neutral("Visibility: " + visibilityStatus))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, backButton, 1))
        );
    }

    public static Dialog createDeleteConfirmationDialog(DatabaseManager db, Teleporter teleporter, LodestoneTP plugin) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Delete Teleporter?", NamedTextColor.RED))
                        .body(List.of(
                                messageBody(Component.text("Are you sure you want to delete '" + teleporter.name() + "'? This cannot be undone.", NamedTextColor.WHITE))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                            actionButton(
                                Component.text("Yes, Delete", NamedTextColor.RED),
                                Component.text("Permanently delete this teleporter"),
                                150,
                                (view, audience) -> {
                                    if (!(audience instanceof Player p)) return;
                                    if (db.removeTeleporter(teleporter.id())) {
                                    p.sendMessage(ComponentFormatter.success("Teleporter ")
                                        .append(ComponentFormatter.teleporterName(teleporter.name()))
                                        .append(ComponentFormatter.success(" has been deleted.")));
                                    plugin.getTeleportEffects().refreshTeleporterLocations();
                                    } else {
                                    p.sendMessage(ComponentFormatter.error("Failed to delete teleporter!"));
                                    }
                                }
                            ),
                            actionButton(
                                Component.text("Cancel"),
                                Component.text("Cancel deletion"),
                                150,
                                (view, audience) -> {
                                    if (audience instanceof Player p) {
                                    p.sendMessage(ComponentFormatter.neutral("Deletion cancelled."));
                                    }
                                }
                            )
                ))
        );
    }

    public static Dialog createAddPlayerDialog(DatabaseManager db, Teleporter teleporter, Player owner, LodestoneTP plugin) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Add Player to Whitelist", NamedTextColor.GOLD))
                        .body(List.of(
                                messageBody(Component.text("Enter the name of the player to add.", NamedTextColor.WHITE))
                        ))
                        .inputs(List.of(
                                DialogInput.text(PLAYER_INPUT_KEY, Component.text("Player Name"))
                                        .initial("")
                                        .maxLength(16)
                                        .width(200)
                                        .build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        actionButton(
                                Component.text("Add"),
                                Component.text("Add player to whitelist"),
                                150,
                                (view, audience) -> {
                                    if (!(audience instanceof Player p)) return;

                                    String name = view.getText(PLAYER_INPUT_KEY);
                                    if (name == null || name.isBlank()) {
                                        p.sendMessage(ComponentFormatter.error("Player name cannot be empty!"));
                                        return;
                                    }

                                    name = name.trim();
                                    OfflinePlayer target = Bukkit.getOfflinePlayer(name);

                                    if (!target.hasPlayedBefore() && !target.isOnline()) {
                                        p.sendMessage(ComponentFormatter.error("Player '" + name + "' has never joined this server!"));
                                        return;
                                    }

                                    String targetUuid = target.getUniqueId().toString();
                                    if (db.addAccess(teleporter.id(), targetUuid)) {
                                        p.sendMessage(ComponentFormatter.success("Added " + target.getName() + " to the whitelist."));

                                        if (plugin.getAdvancementManager() != null) {
                                            plugin.getAdvancementManager().grant(p, "sharing_is_caring");
                                        }
                                    } else {
                                        p.sendMessage(ComponentFormatter.error("Failed to add player to whitelist!"));
                                    }

                                    p.showDialog(createAccessManagementDialog(db, teleporter, p, plugin));
                                }
                        ),
                        openDialogButton(
                                Component.text("Cancel"),
                                Component.text("Cancel adding player"),
                                150,
                                () -> createAccessManagementDialog(db, teleporter, owner, plugin)
                        )
                ))
        );
    }

    public static Dialog createPrivateNoticeDialog(Teleporter teleporter) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Private Teleporter", NamedTextColor.RED))
                        .body(List.of(
                                messageBody(Component.text("This teleporter is private. Ask the owner for access.", NamedTextColor.GRAY))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.notice())
        );
    }

    /**
     * Creates a teleport confirmation dialog showing from/to details, distance, and cost.
     */
    public static Dialog createTeleportConfirmDialog(DatabaseManager db, Teleporter from, Teleporter to, Player player, LodestoneTP plugin) {
        String distanceText = formatDistanceText(from, to);

        Component costPreview = getCostPreview(from, to, plugin);

        List<DialogBody> body = new ArrayList<>();
        body.add(messageBody(Component.text("From: ", NamedTextColor.GRAY).append(Component.text(from.name(), NamedTextColor.WHITE))
            .append(Component.text(" ")).append(teleporterCoordinates(from, NamedTextColor.DARK_GRAY))));
        body.add(messageBody(Component.text("To: ", NamedTextColor.GRAY).append(Component.text(to.name(), NamedTextColor.WHITE))
            .append(Component.text(" ")).append(teleporterCoordinates(to, NamedTextColor.DARK_GRAY))));
        body.add(messageBody(Component.text("Distance: ", NamedTextColor.GRAY).append(Component.text(distanceText, NamedTextColor.AQUA))));
        body.add(messageBody(Component.text("Cost: ", NamedTextColor.GRAY).append(costPreview)));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Confirm Teleport", NamedTextColor.GOLD))
                        .body(body)
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        actionButton(
                            Component.text("Teleport", NamedTextColor.GREEN),
                            Component.text("Teleport to " + to.name()),
                            150,
                            (view, audience) -> {
                                if (!(audience instanceof Player p)) return;
                                executeTeleport(db, from, to, p, plugin, false);
                            }
                        ),
                        openDialogButton(
                            Component.text("Cancel"),
                            Component.text("Go back"),
                            150,
                            () -> createTeleportDialog(db, from, player, plugin)
                        )
                ))
        );
    }

    public static boolean tryAutoTeleportLinked(DatabaseManager db, Teleporter from, Player player, LodestoneTP plugin) {
        if (from.linkedTeleporterId() == null) {
            return false;
        }

        Teleporter linked = db.getTeleporter(from.linkedTeleporterId());
        if (linked == null) {
            return false;
        }

        if (!hasNetworkPermission(db, linked, player)) {
            player.sendMessage(ComponentFormatter.error("You don't have permission for this linked destination."));
            return true;
        }

        executeTeleport(db, from, linked, player, plugin, true);
        return true;
    }

    private static void executeTeleport(DatabaseManager db, Teleporter from, Teleporter to, Player player, LodestoneTP plugin, boolean linkedAutoTeleport) {
        executeTeleport(db, from, to, player, plugin, linkedAutoTeleport, false);
    }

    private static void executeTeleport(DatabaseManager db, Teleporter from, Teleporter to, Player player, LodestoneTP plugin, boolean linkedAutoTeleport, boolean skipWarmup) {
        boolean isAdmin = PermissionChecker.isAdmin(player);

        if (!hasNetworkPermission(db, to, player)) {
            player.sendMessage(ComponentFormatter.error("You don't have permission to use this destination."));
            return;
        }

        if (!skipWarmup && !isAdmin && plugin.getWarmupManager().shouldUseWarmup(player)) {
            int warmupSeconds = plugin.getWarmupManager().getWarmupSeconds();
            player.sendMessage(ComponentFormatter.warning("Teleport warmup started (" + warmupSeconds + "s). Don't move."));
            plugin.getWarmupManager().startWarmup(player, () -> executeTeleport(db, from, to, player, plugin, linkedAutoTeleport, true));
            return;
        }

        if (!isAdmin && plugin.getConfig().getBoolean("cooldown.enabled", true)) {
            CooldownManager cm = plugin.getCooldownManager();
            int remaining = cm.getRemainingCooldown(player.getUniqueId(), to.cooldownOverride());
            if (remaining > 0) {
                player.sendMessage(ComponentFormatter.error("Teleport on cooldown! Wait " + remaining + " seconds."));
                return;
            }
        }

        if (!isAdmin && !checkAndApplyCost(player, from, to, plugin)) {
            return;
        }

        var location = to.toLocation();
        if (location == null) {
            player.sendMessage(ComponentFormatter.error("World '" + to.world() + "' is not loaded!"));
            return;
        }

        plugin.getTeleportEffects().playDeparture(player.getLocation());

        player.teleportAsync(location).thenAccept(success -> {
            if (!success) {
                player.sendMessage(ComponentFormatter.error("Teleportation failed!"));
                return;
            }

            if (!isAdmin && plugin.getConfig().getBoolean("cooldown.enabled", true)) {
                plugin.getCooldownManager().setCooldown(player.getUniqueId());
            }

            plugin.getTeleportEffects().playArrival(player, location);
            db.recordTeleporterUse(player.getUniqueId().toString(), to.id());

                Component prefix = linkedAutoTeleport
                    ? ComponentFormatter.success("Linked teleport to ")
                    : ComponentFormatter.success("Teleported to ");
                player.sendMessage(prefix.append(ComponentFormatter.teleporterName(to.name())));

            if (plugin.getAdvancementManager() != null) {
                plugin.getAdvancementManager().grant(player, "root");
                plugin.getAdvancementManager().grant(player, "first_steps");
                boolean isCrossWorld = !from.world().equals(to.world());
                if (isCrossWorld) {
                    plugin.getAdvancementManager().grant(player, "dimension_hopper");
                } else {
                    int dist = (int) Math.round(horizontalDistance(from, to));
                    if (dist >= 10000) {
                        plugin.getAdvancementManager().grant(player, "long_distance");
                    }
                }
            }
        });
    }

    private static String resolvePlayerName(String uuid) {
        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            String name = player.getName();
            return name != null ? name : uuid.substring(0, 8);
        } catch (IllegalArgumentException e) {
            return uuid.substring(0, Math.min(8, uuid.length()));
        }
    }

    /**
     * Checks if the player can afford the teleport cost and deducts it.
     * Returns true if the player can proceed, false if they cannot afford it.
     */
    private static boolean checkAndApplyCost(Player player, Teleporter from, Teleporter to, LodestoneTP plugin) {
        if (!plugin.getConfig().getBoolean("cost.enabled", false)) return true;

        String costType = plugin.getConfig().getString("cost.type", "xp_levels");

        if ("distance".equals(costType)) {
            return checkAndApplyDistanceCost(player, from, to, plugin);
        } else if ("xp_levels".equals(costType)) {
            int required = plugin.getConfig().getInt("cost.xp-levels", 3);
            if (player.getLevel() < required) {
                player.sendMessage(ComponentFormatter.error("Not enough XP! Need " + required + " levels."));
                return false;
            }
            player.setLevel(player.getLevel() - required);
            player.sendMessage(ComponentFormatter.neutral("Spent " + required + " XP levels."));
            return true;
        } else if ("item".equals(costType)) {
            String materialName = plugin.getConfig().getString("cost.item.material", "ENDER_PEARL");
            int amount = plugin.getConfig().getInt("cost.item.amount", 1);
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                player.sendMessage(ComponentFormatter.error("Teleport cost misconfigured! Contact an admin."));
                return false;
            }
            if (!player.getInventory().containsAtLeast(new ItemStack(material), amount)) {
                player.sendMessage(ComponentFormatter.error("Not enough items! Need " + amount + "x " + material.name() + "."));
                return false;
            }
            player.getInventory().removeItem(new ItemStack(material, amount));
            player.sendMessage(ComponentFormatter.neutral("Spent " + amount + "x " + material.name() + "."));
            return true;
        }
        return true;
    }

    /**
     * Distance-based cost: calculates block distance between teleporters,
     * looks up the matching tier, and charges the configured currency.
     */
    private static boolean checkAndApplyDistanceCost(Player player, Teleporter from, Teleporter to, LodestoneTP plugin) {
        String currency = plugin.getConfig().getString("cost.distance.currency", "xp_levels");
        int cost = resolveDistanceCost(from, to, plugin);

        if (cost <= 0) return true; // Free!

        // Apply the cost using the configured currency
        if ("xp_levels".equals(currency)) {
            if (player.getLevel() < cost) {
                player.sendMessage(ComponentFormatter.error("Not enough XP! Need " + cost + " levels."));
                return false;
            }
            player.setLevel(player.getLevel() - cost);
            player.sendMessage(ComponentFormatter.neutral("Spent " + cost + " XP levels."));
        } else if ("item".equals(currency)) {
            String materialName = plugin.getConfig().getString("cost.item.material", "ENDER_PEARL");
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                player.sendMessage(ComponentFormatter.error("Teleport cost misconfigured! Contact an admin."));
                return false;
            }
            if (!player.getInventory().containsAtLeast(new ItemStack(material), cost)) {
                player.sendMessage(ComponentFormatter.error("Not enough items! Need " + cost + "x " + material.name() + "."));
                return false;
            }
            player.getInventory().removeItem(new ItemStack(material, cost));
            player.sendMessage(ComponentFormatter.neutral("Spent " + cost + "x " + material.name() + "."));
        }
        return true;
    }


    /**
     * Returns a Component describing what the teleport will cost, without deducting.
     */
    private static Component getCostPreview(Teleporter from, Teleporter to, LodestoneTP plugin) {
        if (!plugin.getConfig().getBoolean("cost.enabled", false)) {
            return Component.text("Free", NamedTextColor.GREEN);
        }

        String costType = plugin.getConfig().getString("cost.type", "xp_levels");

        if ("distance".equals(costType)) {
            String currency = plugin.getConfig().getString("cost.distance.currency", "xp_levels");
            int cost = resolveDistanceCost(from, to, plugin);

            if (cost <= 0) return Component.text("Free", NamedTextColor.GREEN);
            return formatCostPreview(cost, currency, plugin);
        } else if ("xp_levels".equals(costType)) {
            int cost = plugin.getConfig().getInt("cost.xp-levels", 3);
            return Component.text(cost + " XP levels", NamedTextColor.GOLD);
        } else if ("item".equals(costType)) {
            String materialName = plugin.getConfig().getString("cost.item.material", "ENDER_PEARL");
            int amount = plugin.getConfig().getInt("cost.item.amount", 1);
            return Component.text(amount + "x " + materialName, NamedTextColor.GOLD);
        }
        return Component.text("Unknown cost", NamedTextColor.RED);
    }

    private static Component formatCostPreview(int cost, String currency, LodestoneTP plugin) {
        if ("xp_levels".equals(currency)) {
            return Component.text(cost + " XP levels", NamedTextColor.GOLD);
        } else if ("item".equals(currency)) {
            String materialName = plugin.getConfig().getString("cost.item.material", "ENDER_PEARL");
            return Component.text(cost + "x " + materialName, NamedTextColor.GOLD);
        }
        return Component.text(cost + " (unknown currency)", NamedTextColor.RED);
    }

    public static Dialog createCooldownOverrideDialog(DatabaseManager db, Teleporter teleporter, Player player, LodestoneTP plugin) {
        String currentValue = teleporter.cooldownOverride() != null ? String.valueOf(teleporter.cooldownOverride()) : "";
        String globalValue = String.valueOf(plugin.getConfig().getInt("cooldown.seconds", 10));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Set Cooldown Override", NamedTextColor.GOLD))
                        .body(List.of(
                                messageBody(Component.text("Set a custom cooldown for this teleporter.", NamedTextColor.WHITE)),
                                messageBody(Component.text("Leave empty to use global cooldown (" + globalValue + "s)", NamedTextColor.GRAY))
                        ))
                        .inputs(List.of(
                                DialogInput.text("cooldown_value", Component.text("Cooldown Seconds"))
                                        .initial(currentValue)
                                        .maxLength(10)
                                        .width(200)
                                        .build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        actionButton(
                                Component.text("Save"),
                                Component.text("Save the cooldown override"),
                                150,
                                (view, audience) -> {
                                    if (!(audience instanceof Player p)) return;
                                    String input = view.getText("cooldown_value");
                                    Integer newOverride = null;
                                    if (input != null && !input.isBlank()) {
                                        try {
                                            int value = Integer.parseInt(input.trim());
                                            if (value < 0) {
                                                p.sendMessage(Component.text("Cooldown cannot be negative!", NamedTextColor.RED));
                                                return;
                                            }
                                            newOverride = value;
                                        } catch (NumberFormatException e) {
                                            p.sendMessage(Component.text("Invalid number! Enter a non-negative integer.", NamedTextColor.RED));
                                            return;
                                        }
                                    }
                                    if (db.setCooldownOverride(teleporter.id(), newOverride)) {
                                        String message = newOverride != null
                                                ? "Cooldown override set to " + newOverride + " seconds"
                                                : "Cooldown override cleared (using global)";
                                        p.sendMessage(Component.text(message, NamedTextColor.GREEN));
                                        Teleporter updated = new Teleporter(
                                                teleporter.id(), teleporter.name(), teleporter.world(),
                                                teleporter.x(), teleporter.y(), teleporter.z(), teleporter.yaw(),
                                                teleporter.ownerUuid(), teleporter.isPublic(), newOverride, teleporter.networkId(), teleporter.linkedTeleporterId()
                                        );
                                        p.showDialog(createAccessManagementDialog(db, updated, p, plugin));
                                    } else {
                                        p.sendMessage(ComponentFormatter.error("Failed to update cooldown override!"));
                                    }
                                }
                        ),
                        openDialogButton(
                                Component.text("Cancel"),
                                Component.text("Cancel and go back"),
                                150,
                                () -> createAccessManagementDialog(db, teleporter, player, plugin)
                        )
                ))
        );
    }

    public static Dialog createLinkTeleporterDialog(DatabaseManager db, Teleporter teleporter, Player player, LodestoneTP plugin) {
        List<Teleporter> accessible = db.getAccessibleTeleporters(player.getUniqueId().toString());
        List<ActionButton> buttons = new ArrayList<>();

        for (Teleporter tp : accessible) {
            if (tp.id() == teleporter.id()) continue;
            if (tp.linkedTeleporterId() != null) continue;

            Component tooltip = Component.text(tp.world() + " (" + tp.x() + ", " + tp.y() + ", " + tp.z() + ")");

            final Teleporter finalTp = tp;
            buttons.add(actionButton(
                    Component.text(tp.name()),
                    tooltip,
                    200,
                    (view, audience) -> {
                        if (!(audience instanceof Player p)) return;
                        if (db.setLinkedTeleporter(teleporter.id(), finalTp.id())
                                && db.setLinkedTeleporter(finalTp.id(), teleporter.id())) {
                            p.sendMessage(ComponentFormatter.success("Teleporters linked successfully!"));
                            p.showDialog(createAccessManagementDialog(db, teleporter, p, plugin));
                        } else {
                            p.sendMessage(ComponentFormatter.error("Failed to link teleporters!"));
                        }
                    }
            ));
        }

        Component title = Component.text("Link: ", NamedTextColor.GOLD)
                .append(Component.text(teleporter.name(), NamedTextColor.WHITE));

        if (buttons.isEmpty()) {
            return Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(title)
                            .body(List.of(
                                        messageBody(Component.text("No available teleporters to link.", NamedTextColor.GRAY))
                            ))
                            .canCloseWithEscape(true)
                            .build())
                    .type(DialogType.notice())
            );
        }

                        ActionButton exitButton = openDialogButton(
                            Component.text("Cancel"),
                            Component.text("Cancel linking"),
                            150,
                            () -> createAccessManagementDialog(db, teleporter, player, plugin)
                        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(
                                messageBody(Component.text("Select a teleporter to link with.", NamedTextColor.WHITE))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, exitButton, 1))
        );
    }

    public static Dialog createUnlinkConfirmDialog(DatabaseManager db, Teleporter teleporter, Player player, LodestoneTP plugin) {
        Teleporter linked = db.getTeleporter(teleporter.linkedTeleporterId());
        String linkedName = linked != null ? linked.name() : "unknown";

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Unlink Teleporters", NamedTextColor.GOLD))
                        .body(List.of(
                                messageBody(Component.text("Are you sure you want to unlink " + teleporter.name() + " from " + linkedName + "?", NamedTextColor.WHITE))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                            actionButton(
                                Component.text("Unlink"),
                                Component.text("Unlink these teleporters"),
                                150,
                                (view, audience) -> {
                                    if (!(audience instanceof Player p)) return;
                                    int linkedId = teleporter.linkedTeleporterId();
                                    if (linkedId > 0) {
                                    db.setLinkedTeleporter(teleporter.id(), null);
                                    db.setLinkedTeleporter(linkedId, null);
                                    p.sendMessage(ComponentFormatter.success("Teleporters unlinked!"));
                                    }
                                    p.showDialog(createAccessManagementDialog(db, teleporter, p, plugin));
                                }
                            ),
                            openDialogButton(
                                Component.text("Cancel"),
                                Component.text("Keep teleporters linked"),
                                150,
                                () -> createAccessManagementDialog(db, teleporter, player, plugin)
                            )
                ))
        );
    }
}
