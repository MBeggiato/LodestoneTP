package io.github.marcel.loadstonetp.dialogs;

import io.github.marcel.loadstonetp.LodestoneTP;
import io.github.marcel.loadstonetp.db.DatabaseManager;
import io.github.marcel.loadstonetp.model.Teleporter;
import io.github.marcel.loadstonetp.utils.ComponentFormatter;
import io.github.marcel.loadstonetp.utils.PermissionChecker;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class AdminDialogs {

    private static final String SECONDS_INPUT_KEY = "cooldown_seconds";
    private static final String XP_LEVELS_INPUT_KEY = "xp_levels";
    private static final String ITEM_MATERIAL_INPUT_KEY = "item_material";
    private static final String ITEM_AMOUNT_INPUT_KEY = "item_amount";
    private static final String TIER_MAX_DISTANCE_INPUT_KEY = "tier_max_distance";
    private static final String TIER_COST_INPUT_KEY = "tier_cost";
    private static final String CROSS_WORLD_COST_INPUT_KEY = "cross_world_cost";
    private static final String CREATION_FEE_MATERIAL_INPUT_KEY = "creation_fee_material";
    private static final String CREATION_FEE_AMOUNT_INPUT_KEY = "creation_fee_amount";
    private static final String AMBIENT_VOLUME_INPUT_KEY = "ambient_volume";
    private static final String AMBIENT_RANGE_INPUT_KEY = "ambient_range";
    private static final String AMBIENT_INTERVAL_INPUT_KEY = "ambient_interval";
    private static final String LIGHT_LEVEL_INPUT_KEY = "light_level";
    private static final ClickCallback.Options CLICK_OPTIONS = ClickCallback.Options.builder()
            .uses(1)
            .lifetime(Duration.ofMinutes(5))
            .build();

    private AdminDialogs() {}

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

    private static ActionButton actionButton(Component label, Component tooltip, int width, DialogActionCallback callback) {
        return actionButton(label, tooltip, width, DialogAction.customClick(callback, CLICK_OPTIONS));
    }

    private static ActionButton saveButton(Component tooltip, int width, DialogActionCallback callback) {
        return actionButton(Component.text("Save"), tooltip, width, callback);
    }

    private static ActionButton cancelButton(String tooltip, Supplier<Dialog> dialogSupplier) {
        return openDialogButton(Component.text("Cancel"), Component.text(tooltip), 150, dialogSupplier);
    }

    private static ActionButton deleteButton(String label, String tooltip, int width, DialogActionCallback callback) {
        return actionButton(Component.text(label, NamedTextColor.RED), Component.text(tooltip), width, callback);
    }

    private static ActionButton backButton(String tooltip, Supplier<Dialog> dialogSupplier) {
        return openDialogButton(Component.text("Back"), Component.text(tooltip), 150, dialogSupplier);
    }

    private static ActionButton closeButton(String tooltip) {
        return ActionButton.create(
                Component.text("Close"),
                Component.text(tooltip),
                150,
                DialogAction.customClick((view, audience) -> {}, CLICK_OPTIONS)
        );
    }

        private static ActionButton noopButton(Component label, Component tooltip, int width) {
                return actionButton(label, tooltip, width, (view, audience) -> {});
        }

    private static DialogBody messageBody(Component component) {
        return DialogBody.plainMessage(component);
    }

    private static Component title(String text) {
        return ComponentFormatter.emphasis(text);
    }

    private static Component teleporterTitle(Teleporter teleporter) {
        return ComponentFormatter.emphasis("Teleporter: ")
                .append(Component.text(teleporter.name(), NamedTextColor.WHITE));
    }

    private static Component teleporterLocation(Teleporter teleporter) {
        return ComponentFormatter.neutral(
                "Location: " + teleporter.world() + " (" + teleporter.x() + ", " + teleporter.y() + ", " + teleporter.z() + ")"
        );
    }

    private static Component teleporterVisibility(Teleporter teleporter) {
        return Component.text(
                "Visibility: " + (teleporter.isPublic() ? "Public" : "Private"),
                teleporter.isPublic() ? NamedTextColor.GREEN : NamedTextColor.RED
        );
    }

    private static Component teleporterTooltip(Teleporter teleporter, String ownerName) {
        return ComponentFormatter.neutral(teleporter.world() + " (" + teleporter.x() + ", " + teleporter.y() + ", " + teleporter.z() + ")")
                .append(ComponentFormatter.neutral(" | Owner: "))
                .append(Component.text(ownerName, NamedTextColor.WHITE))
                .append(ComponentFormatter.neutral(" | "))
                .append(Component.text(teleporter.isPublic() ? "Public" : "Private",
                        teleporter.isPublic() ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private static void persistConfig(LodestoneTP plugin) {
        plugin.saveConfig();
        plugin.reloadConfig();
    }

    private static void broadcastAdmin(Player actor, String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
                        if (PermissionChecker.isAdmin(p)) {
                                p.sendMessage(ComponentFormatter.emphasis("[LodestoneTP] ")
                                                .append(ComponentFormatter.warning("Admin " + actor.getName() + " " + message)));
            }
        }
    }

    private static Dialog createErrorDialog(String message, LodestoneTP plugin, java.util.function.Supplier<Dialog> returnDialog) {
        return Dialog.create(builder -> builder.empty()
                                .base(DialogBase.builder(ComponentFormatter.error("Error"))
                        .body(List.of(
                                                                messageBody(ComponentFormatter.error(message))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.notice(
                                                backButton("Return to previous screen", returnDialog)
                ))
        );
    }

    // ── Main Admin Panel ─────────────────────────────────────────────

    public static Dialog createMainPanel(LodestoneTP plugin) {
        boolean cooldownEnabled = plugin.getConfig().getBoolean("cooldown.enabled", true);
        int cooldownSeconds = plugin.getConfig().getInt("cooldown.seconds", 10);
        boolean costEnabled = plugin.getConfig().getBoolean("cost.enabled", false);
        String costType = plugin.getConfig().getString("cost.type", "xp_levels");
        boolean defaultPublic = plugin.getConfig().getBoolean("defaults.public", true);

        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(openDialogButton(
                Component.text("⚙ Cooldown Settings"),
                Component.text("Currently: " + (cooldownEnabled ? "enabled" : "disabled") + ", " + cooldownSeconds + " seconds"),
                200,
                () -> createCooldownDialog(plugin)
        ));

        buttons.add(openDialogButton(
                Component.text("💰 Cost Settings"),
                Component.text("Currently: " + (costEnabled ? "enabled" : "disabled") + ", type: " + costType),
                200,
                () -> createCostDialog(plugin)
        ));

        buttons.add(openDialogButton(
                Component.text("🗼 Manage Teleporters"),
                Component.text("View and manage all teleporters"),
                200,
                () -> createTeleporterListDialog(plugin)
        ));

        buttons.add(openDialogButton(
                Component.text("🌐 Default Visibility"),
                Component.text("Currently: " + (defaultPublic ? "Public" : "Private")),
                200,
                () -> createDefaultVisibilityDialog(plugin)
        ));

        boolean effectsEnabled = plugin.getConfig().getBoolean("effects.enabled", true);
        buttons.add(openDialogButton(
                Component.text("✨ Effects Settings"),
                Component.text("Currently: " + (effectsEnabled ? "enabled" : "disabled")),
                200,
                () -> createEffectsDialog(plugin)
        ));

        boolean feeEnabled = plugin.getConfig().getBoolean("creation-fee.enabled", true);
        String feeMaterial = plugin.getConfig().getString("creation-fee.material", "ENDER_PEARL");
        int feeAmount = plugin.getConfig().getInt("creation-fee.amount", 1);
        buttons.add(openDialogButton(
                Component.text("💎 Creation Fee"),
                Component.text("Currently: " + (feeEnabled ? feeAmount + "x " + feeMaterial : "disabled")),
                200,
                () -> createCreationFeeDialog(plugin)
        ));

        ActionButton exitButton = closeButton("Close the admin panel");

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("LodestoneTP Admin Panel"))
                        .body(List.of(
                                messageBody(Component.text("Manage all plugin settings from here.", NamedTextColor.WHITE))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, exitButton, 2))
        );
    }

    // ── Cooldown Settings ────────────────────────────────────────────

    public static Dialog createCooldownDialog(LodestoneTP plugin) {
        boolean enabled = plugin.getConfig().getBoolean("cooldown.enabled", true);
        int seconds = plugin.getConfig().getInt("cooldown.seconds", 10);

        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(actionButton(
                Component.text(enabled ? "Disable Cooldown" : "Enable Cooldown", enabled ? NamedTextColor.RED : NamedTextColor.GREEN),
                Component.text("Toggle cooldown " + (enabled ? "off" : "on")),
                200,
                (view, audience) -> {
                    if (!(audience instanceof Player p)) return;
                    boolean newValue = !enabled;
                    plugin.getConfig().set("cooldown.enabled", newValue);
                    persistConfig(plugin);
                    broadcastAdmin(p, "changed cooldown to " + (newValue ? "enabled" : "disabled"));
                    p.showDialog(createCooldownDialog(plugin));
                }
        ));

        buttons.add(openDialogButton(
                Component.text("Set Seconds (" + seconds + ")"),
                Component.text("Change the cooldown duration"),
                200,
                () -> createCooldownSecondsInputDialog(plugin)
        ));

        ActionButton backButton = backButton("Return to admin panel", () -> createMainPanel(plugin));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Cooldown Settings"))
                        .body(List.of(
                                messageBody(Component.text("Status: " + (enabled ? "Enabled" : "Disabled"), enabled ? NamedTextColor.GREEN : NamedTextColor.RED)),
                                messageBody(ComponentFormatter.neutral("Duration: " + seconds + " seconds"))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, backButton, 1))
        );
    }

    public static Dialog createCooldownSecondsInputDialog(LodestoneTP plugin) {
        int current = plugin.getConfig().getInt("cooldown.seconds", 10);

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Set Cooldown Seconds"))
                        .body(List.of(
                                messageBody(Component.text("Enter the cooldown duration in seconds.", NamedTextColor.WHITE))
                        ))
                        .inputs(List.of(
                                DialogInput.text(SECONDS_INPUT_KEY, Component.text("Seconds"))
                                        .initial(String.valueOf(current))
                                        .maxLength(10)
                                        .width(200)
                                        .build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                                                saveButton(
                                                                Component.text("Save the cooldown duration"),
                                                                150,
                                                                (view, audience) -> {
                                                                        if (!(audience instanceof Player p)) return;
                                                                        String input = view.getText(SECONDS_INPUT_KEY);
                                                                        if (input == null || input.isBlank()) {
                                                                                p.showDialog(createErrorDialog("Seconds cannot be empty!", plugin, () -> createCooldownSecondsInputDialog(plugin)));
                                                                                return;
                                                                        }
                                                                        try {
                                                                                int value = Integer.parseInt(input.trim());
                                                                                if (value <= 0) {
                                                                                        p.showDialog(createErrorDialog("Seconds must be a positive number!", plugin, () -> createCooldownSecondsInputDialog(plugin)));
                                                                                        return;
                                                                                }
                                                                                plugin.getConfig().set("cooldown.seconds", value);
                                                                                persistConfig(plugin);
                                                                                plugin.getCooldownManager().setCooldownSeconds(value);
                                                                                broadcastAdmin(p, "changed cooldown to " + value + " seconds");
                                                                                p.showDialog(createCooldownDialog(plugin));
                                                                        } catch (NumberFormatException e) {
                                                                                p.showDialog(createErrorDialog("Invalid number! Enter a positive integer.", plugin, () -> createCooldownSecondsInputDialog(plugin)));
                                                                        }
                                                                }
                                                ),
                        openDialogButton(Component.text("Cancel"), Component.text("Cancel and go back"), 150, () -> createCooldownDialog(plugin))
                ))
        );
    }

    // ── Cost Settings ────────────────────────────────────────────────

    public static Dialog createCostDialog(LodestoneTP plugin) {
        boolean enabled = plugin.getConfig().getBoolean("cost.enabled", false);
        String costType = plugin.getConfig().getString("cost.type", "xp_levels");

        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(actionButton(
                Component.text(enabled ? "Disable Cost" : "Enable Cost", enabled ? NamedTextColor.RED : NamedTextColor.GREEN),
                Component.text("Toggle cost " + (enabled ? "off" : "on")),
                200,
                (view, audience) -> {
                    if (!(audience instanceof Player p)) return;
                    boolean newValue = !enabled;
                    plugin.getConfig().set("cost.enabled", newValue);
                    persistConfig(plugin);
                    broadcastAdmin(p, "changed cost to " + (newValue ? "enabled" : "disabled"));
                    p.showDialog(createCostDialog(plugin));
                }
        ));

        // Cost type buttons
        for (String type : new String[]{"xp_levels", "item", "distance"}) {
            boolean isActive = type.equals(costType);
                        buttons.add(actionButton(
                                        Component.text(type + (isActive ? " ✓" : ""), isActive ? NamedTextColor.GREEN : NamedTextColor.WHITE),
                                        Component.text(isActive ? "Currently active" : "Switch to " + type),
                                        200,
                                        (view, audience) -> {
                                                if (!(audience instanceof Player p)) return;
                                                plugin.getConfig().set("cost.type", type);
                                                persistConfig(plugin);
                                                broadcastAdmin(p, "changed cost type to " + type);
                                                p.showDialog(createCostDialog(plugin));
                                        }
                        ));
        }

        // Sub-panel button for the active cost type
                buttons.add(actionButton(
                                Component.text("Configure " + costType, NamedTextColor.GOLD),
                                Component.text("Edit settings for " + costType),
                                200,
                                (view, audience) -> {
                                        if (!(audience instanceof Player p)) return;
                                        switch (costType) {
                                                case "xp_levels" -> p.showDialog(createXpLevelsConfigDialog(plugin));
                                                case "item" -> p.showDialog(createItemConfigDialog(plugin));
                                                case "distance" -> p.showDialog(createDistanceConfigDialog(plugin));
                                                default -> p.showDialog(createCostDialog(plugin));
                                        }
                                }
                ));

        ActionButton backButton = backButton("Return to admin panel", () -> createMainPanel(plugin));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Cost Settings"))
                        .body(List.of(
                                messageBody(Component.text("Status: " + (enabled ? "Enabled" : "Disabled"), enabled ? NamedTextColor.GREEN : NamedTextColor.RED)),
                                messageBody(ComponentFormatter.neutral("Type: " + costType))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, backButton, 2))
        );
    }

    // ── XP Levels Config ─────────────────────────────────────────────

    public static Dialog createXpLevelsConfigDialog(LodestoneTP plugin) {
        int current = plugin.getConfig().getInt("cost.xp-levels", 3);

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("XP Levels Cost"))
                        .body(List.of(
                                messageBody(Component.text("Set the number of XP levels required per teleport.", NamedTextColor.WHITE))
                        ))
                        .inputs(List.of(
                                DialogInput.text(XP_LEVELS_INPUT_KEY, Component.text("XP Levels"))
                                        .initial(String.valueOf(current))
                                        .maxLength(10)
                                        .width(200)
                                        .build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        saveButton(
                                Component.text("Save XP level cost"),
                                150,
                                                                (view, audience) -> {
                                                                        if (!(audience instanceof Player p)) return;
                                                                        String input = view.getText(XP_LEVELS_INPUT_KEY);
                                                                        if (input == null || input.isBlank()) {
                                                                                p.showDialog(createErrorDialog("XP levels cannot be empty!", plugin, () -> createXpLevelsConfigDialog(plugin)));
                                                                                return;
                                                                        }
                                                                        try {
                                                                                int value = Integer.parseInt(input.trim());
                                                                                if (value <= 0) {
                                                                                        p.showDialog(createErrorDialog("XP levels must be a positive number!", plugin, () -> createXpLevelsConfigDialog(plugin)));
                                                                                        return;
                                                                                }
                                                                                plugin.getConfig().set("cost.xp-levels", value);
                                                                                persistConfig(plugin);
                                                                                broadcastAdmin(p, "changed XP level cost to " + value);
                                                                                p.showDialog(createCostDialog(plugin));
                                                                        } catch (NumberFormatException e) {
                                                                                p.showDialog(createErrorDialog("Invalid number! Enter a positive integer.", plugin, () -> createXpLevelsConfigDialog(plugin)));
                                                                        }
                                                                }
                        ),
                        cancelButton("Cancel and go back", () -> createCostDialog(plugin))
                ))
        );
    }

    // ── Item Config ──────────────────────────────────────────────────

    public static Dialog createItemConfigDialog(LodestoneTP plugin) {
        String material = plugin.getConfig().getString("cost.item.material", "ENDER_PEARL");
        int amount = plugin.getConfig().getInt("cost.item.amount", 1);

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Item Cost"))
                        .body(List.of(
                                messageBody(Component.text("Set the item material and amount required per teleport.", NamedTextColor.WHITE)),
                                messageBody(ComponentFormatter.neutral("Current: " + amount + "x " + material))
                        ))
                        .inputs(List.of(
                                DialogInput.text(ITEM_MATERIAL_INPUT_KEY, Component.text("Material"))
                                        .initial(material)
                                        .maxLength(64)
                                        .width(200)
                                        .build(),
                                DialogInput.text(ITEM_AMOUNT_INPUT_KEY, Component.text("Amount"))
                                        .initial(String.valueOf(amount))
                                        .maxLength(10)
                                        .width(200)
                                        .build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        saveButton(
                                Component.text("Save item cost settings"),
                                150,
                                                                (view, audience) -> {
                                                                        if (!(audience instanceof Player p)) return;

                                                                        String matInput = view.getText(ITEM_MATERIAL_INPUT_KEY);
                                                                        String amtInput = view.getText(ITEM_AMOUNT_INPUT_KEY);

                                                                        if (matInput == null || matInput.isBlank()) {
                                                                                p.showDialog(createErrorDialog("Material cannot be empty!", plugin, () -> createItemConfigDialog(plugin)));
                                                                                return;
                                                                        }

                                                                        Material mat = Material.matchMaterial(matInput.trim());
                                                                        if (mat == null) {
                                                                                p.showDialog(createErrorDialog("Invalid material: " + matInput.trim() + "! Use Minecraft material names like ENDER_PEARL.", plugin, () -> createItemConfigDialog(plugin)));
                                                                                return;
                                                                        }

                                                                        if (amtInput == null || amtInput.isBlank()) {
                                                                                p.showDialog(createErrorDialog("Amount cannot be empty!", plugin, () -> createItemConfigDialog(plugin)));
                                                                                return;
                                                                        }

                                                                        try {
                                                                                int amtValue = Integer.parseInt(amtInput.trim());
                                                                                if (amtValue <= 0) {
                                                                                        p.showDialog(createErrorDialog("Amount must be a positive number!", plugin, () -> createItemConfigDialog(plugin)));
                                                                                        return;
                                                                                }

                                                                                plugin.getConfig().set("cost.item.material", mat.name());
                                                                                plugin.getConfig().set("cost.item.amount", amtValue);
                                                                                persistConfig(plugin);
                                                                                broadcastAdmin(p, "changed item cost to " + amtValue + "x " + mat.name());
                                                                                p.showDialog(createCostDialog(plugin));
                                                                        } catch (NumberFormatException e) {
                                                                                p.showDialog(createErrorDialog("Invalid amount! Enter a positive integer.", plugin, () -> createItemConfigDialog(plugin)));
                                                                        }
                                                                }
                        ),
                                                cancelButton("Cancel and go back", () -> createCostDialog(plugin))
                ))
        );
    }

    // ── Distance Config ──────────────────────────────────────────────

    public static Dialog createDistanceConfigDialog(LodestoneTP plugin) {
        String currency = plugin.getConfig().getString("cost.distance.currency", "xp_levels");
        int crossWorldCost = plugin.getConfig().getInt("cost.distance.cross-world-cost", 10);
        List<?> tiers = plugin.getConfig().getList("cost.distance.tiers");

        List<ActionButton> buttons = new ArrayList<>();

        // Currency selector buttons
        for (String curr : new String[]{"xp_levels", "item"}) {
            boolean isActive = curr.equals(currency);
            buttons.add(actionButton(
                    Component.text("Currency: " + curr + (isActive ? " ✓" : ""), isActive ? NamedTextColor.GREEN : NamedTextColor.WHITE),
                    Component.text(isActive ? "Currently active" : "Switch currency to " + curr),
                    200,
                    (view, audience) -> {
                        if (!(audience instanceof Player p)) return;
                        plugin.getConfig().set("cost.distance.currency", curr);
                        persistConfig(plugin);
                        broadcastAdmin(p, "changed distance currency to " + curr);
                        p.showDialog(createDistanceConfigDialog(plugin));
                    }
            ));
        }

        // Cross-world cost button
        buttons.add(openDialogButton(
                Component.text("Cross-World Cost (" + crossWorldCost + ")"),
                Component.text("Set the flat cost for cross-world teleports"),
                200,
                () -> createCrossWorldCostInputDialog(plugin)
        ));

        // Tier buttons
        if (tiers != null) {
            for (int i = 0; i < tiers.size(); i++) {
                Object tierObj = tiers.get(i);
                if (tierObj instanceof Map<?, ?> tier) {
                    Object maxDistObj = tier.get("max-distance");
                    Object costObj = tier.get("cost");
                    int maxDist = maxDistObj instanceof Number n ? n.intValue() : -1;
                    int tierCost = costObj instanceof Number n ? n.intValue() : 0;
                    String distLabel = maxDist == -1 ? "unlimited" : String.valueOf(maxDist);
                    final int tierIndex = i;

                                        buttons.add(openDialogButton(
                            Component.text("Tier: ≤" + distLabel + " blocks → " + tierCost),
                            Component.text("Edit this distance tier"),
                            200,
                                                        () -> createTierEditDialog(plugin, tierIndex)
                    ));
                }
            }
        }

        // Add tier button
        buttons.add(openDialogButton(
                Component.text("+ Add Tier", NamedTextColor.GREEN),
                Component.text("Add a new distance tier"),
                200,
                () -> createTierAddDialog(plugin)
        ));

        ActionButton backButton = backButton("Return to cost settings", () -> createCostDialog(plugin));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Distance Cost"))
                        .body(List.of(
                                messageBody(ComponentFormatter.neutral("Currency: " + currency)),
                                messageBody(ComponentFormatter.neutral("Cross-World Cost: " + crossWorldCost)),
                                messageBody(ComponentFormatter.neutral("Tiers: " + (tiers != null ? tiers.size() : 0)))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, backButton, 2))
        );
    }

    public static Dialog createCrossWorldCostInputDialog(LodestoneTP plugin) {
        int current = plugin.getConfig().getInt("cost.distance.cross-world-cost", 10);

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Cross-World Cost"))
                        .body(List.of(
                                messageBody(Component.text("Set the flat cost for teleporting between worlds.", NamedTextColor.WHITE))
                        ))
                        .inputs(List.of(
                                DialogInput.text(CROSS_WORLD_COST_INPUT_KEY, Component.text("Cost"))
                                        .initial(String.valueOf(current))
                                        .maxLength(10)
                                        .width(200)
                                        .build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                                                saveButton(
                                                                Component.text("Save cross-world cost"),
                                                                150,
                                                                (view, audience) -> {
                                                                        if (!(audience instanceof Player p)) return;
                                                                        String input = view.getText(CROSS_WORLD_COST_INPUT_KEY);
                                                                        if (input == null || input.isBlank()) {
                                                                                p.showDialog(createErrorDialog("Cost cannot be empty!", plugin, () -> createCrossWorldCostInputDialog(plugin)));
                                                                                return;
                                                                        }
                                                                        try {
                                                                                int value = Integer.parseInt(input.trim());
                                                                                if (value < 0) {
                                                                                        p.showDialog(createErrorDialog("Cost must be non-negative!", plugin, () -> createCrossWorldCostInputDialog(plugin)));
                                                                                        return;
                                                                                }
                                                                                plugin.getConfig().set("cost.distance.cross-world-cost", value);
                                                                                persistConfig(plugin);
                                                                                broadcastAdmin(p, "changed cross-world cost to " + value);
                                                                                p.showDialog(createDistanceConfigDialog(plugin));
                                                                        } catch (NumberFormatException e) {
                                                                                p.showDialog(createErrorDialog("Invalid number! Enter a non-negative integer.", plugin, () -> createCrossWorldCostInputDialog(plugin)));
                                                                        }
                                                                }
                                                ),
                        cancelButton("Cancel and go back", () -> createDistanceConfigDialog(plugin))
                ))
        );
    }

    // ── Tier Edit Dialog ─────────────────────────────────────────────

    public static Dialog createTierEditDialog(LodestoneTP plugin, int tierIndex) {
        List<?> tiers = plugin.getConfig().getList("cost.distance.tiers");
        if (tiers == null || tierIndex >= tiers.size()) {
            return createDistanceConfigDialog(plugin);
        }

        Object tierObj = tiers.get(tierIndex);
        int currentMaxDist = -1;
        int currentCost = 0;
        if (tierObj instanceof Map<?, ?> tier) {
            Object maxDistObj = tier.get("max-distance");
            Object costObj = tier.get("cost");
            currentMaxDist = maxDistObj instanceof Number n ? n.intValue() : -1;
            currentCost = costObj instanceof Number n ? n.intValue() : 0;
        }

        final int finalMaxDist = currentMaxDist;
        final int finalCost = currentCost;

        List<ActionButton> buttons = new ArrayList<>();

        // Edit max-distance and cost
        buttons.add(openDialogButton(
                Component.text("Edit Values"),
                Component.text("Change max-distance and cost"),
                200,
                () -> createTierValuesInputDialog(plugin, tierIndex, finalMaxDist, finalCost)
        ));

        // Delete tier
        buttons.add(openDialogButton(
                Component.text("Delete Tier", NamedTextColor.RED),
                Component.text("Remove this tier"),
                200,
                () -> createTierDeleteConfirmDialog(plugin, tierIndex)
        ));

        ActionButton backButton = backButton("Return to distance settings", () -> createDistanceConfigDialog(plugin));

        String distLabel = currentMaxDist == -1 ? "unlimited" : String.valueOf(currentMaxDist);
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Edit Tier"))
                        .body(List.of(
                                messageBody(ComponentFormatter.neutral("Max Distance: " + distLabel + " blocks")),
                                messageBody(ComponentFormatter.neutral("Cost: " + finalCost))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, backButton, 1))
        );
    }

    public static Dialog createTierValuesInputDialog(LodestoneTP plugin, int tierIndex, int currentMaxDist, int currentCost) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Edit Tier Values"))
                        .body(List.of(
                                messageBody(Component.text("Set max-distance (-1 for unlimited) and cost.", NamedTextColor.WHITE))
                        ))
                        .inputs(List.of(
                                DialogInput.text(TIER_MAX_DISTANCE_INPUT_KEY, Component.text("Max Distance"))
                                        .initial(String.valueOf(currentMaxDist))
                                        .maxLength(10)
                                        .width(200)
                                        .build(),
                                DialogInput.text(TIER_COST_INPUT_KEY, Component.text("Cost"))
                                        .initial(String.valueOf(currentCost))
                                        .maxLength(10)
                                        .width(200)
                                        .build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                                                saveButton(
                                                                Component.text("Save tier values"),
                                                                150,
                                                                (view, audience) -> {
                                                                        if (!(audience instanceof Player p)) return;

                                                                        String distInput = view.getText(TIER_MAX_DISTANCE_INPUT_KEY);
                                                                        String costInput = view.getText(TIER_COST_INPUT_KEY);

                                                                        if (distInput == null || distInput.isBlank() || costInput == null || costInput.isBlank()) {
                                                                                p.showDialog(createErrorDialog("Both fields are required!", plugin, () -> createTierValuesInputDialog(plugin, tierIndex, currentMaxDist, currentCost)));
                                                                                return;
                                                                        }

                                                                        try {
                                                                                int maxDist = Integer.parseInt(distInput.trim());
                                                                                int cost = Integer.parseInt(costInput.trim());

                                                                                if (maxDist < -1 || maxDist == 0) {
                                                                                        p.showDialog(createErrorDialog("Max distance must be -1 (unlimited) or a positive integer!", plugin, () -> createTierValuesInputDialog(plugin, tierIndex, currentMaxDist, currentCost)));
                                                                                        return;
                                                                                }
                                                                                if (cost < 0) {
                                                                                        p.showDialog(createErrorDialog("Cost must be non-negative!", plugin, () -> createTierValuesInputDialog(plugin, tierIndex, currentMaxDist, currentCost)));
                                                                                        return;
                                                                                }

                                                                                saveTierAtIndex(plugin, tierIndex, maxDist, cost);
                                                                                broadcastAdmin(p, "updated distance tier (max: " + maxDist + ", cost: " + cost + ")");
                                                                                p.showDialog(createDistanceConfigDialog(plugin));
                                                                        } catch (NumberFormatException e) {
                                                                                p.showDialog(createErrorDialog("Invalid number! Enter integers only.", plugin, () -> createTierValuesInputDialog(plugin, tierIndex, currentMaxDist, currentCost)));
                                                                        }
                                                                }
                                                ),
                        cancelButton("Cancel and go back", () -> createTierEditDialog(plugin, tierIndex))
                ))
        );
    }

    public static Dialog createTierDeleteConfirmDialog(LodestoneTP plugin, int tierIndex) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(ComponentFormatter.error("Delete Tier?"))
                        .body(List.of(
                                messageBody(Component.text("Are you sure you want to delete this tier? This cannot be undone.", NamedTextColor.WHITE))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        deleteButton(
                                "Yes, Delete",
                                "Permanently delete this tier",
                                150,
                                (view, audience) -> {
                                    if (!(audience instanceof Player p)) return;
                                    removeTierAtIndex(plugin, tierIndex);
                                    broadcastAdmin(p, "deleted a distance tier");
                                    p.showDialog(createDistanceConfigDialog(plugin));
                                }
                        ),
                        cancelButton("Cancel deletion", () -> createTierEditDialog(plugin, tierIndex))
                ))
        );
    }

    public static Dialog createTierAddDialog(LodestoneTP plugin) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Add Distance Tier"))
                        .body(List.of(
                                messageBody(Component.text("Set max-distance (-1 for unlimited) and cost for the new tier.", NamedTextColor.WHITE))
                        ))
                        .inputs(List.of(
                                DialogInput.text(TIER_MAX_DISTANCE_INPUT_KEY, Component.text("Max Distance"))
                                        .initial("1000")
                                        .maxLength(10)
                                        .width(200)
                                        .build(),
                                DialogInput.text(TIER_COST_INPUT_KEY, Component.text("Cost"))
                                        .initial("0")
                                        .maxLength(10)
                                        .width(200)
                                        .build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                                                actionButton(
                                                                Component.text("Add"),
                                                                Component.text("Add this tier"),
                                                                150,
                                                                (view, audience) -> {
                                                                        if (!(audience instanceof Player p)) return;

                                                                        String distInput = view.getText(TIER_MAX_DISTANCE_INPUT_KEY);
                                                                        String costInput = view.getText(TIER_COST_INPUT_KEY);

                                                                        if (distInput == null || distInput.isBlank() || costInput == null || costInput.isBlank()) {
                                                                                p.showDialog(createErrorDialog("Both fields are required!", plugin, () -> createTierAddDialog(plugin)));
                                                                                return;
                                                                        }

                                                                        try {
                                                                                int maxDist = Integer.parseInt(distInput.trim());
                                                                                int cost = Integer.parseInt(costInput.trim());

                                                                                if (maxDist < -1 || maxDist == 0) {
                                                                                        p.showDialog(createErrorDialog("Max distance must be -1 (unlimited) or a positive integer!", plugin, () -> createTierAddDialog(plugin)));
                                                                                        return;
                                                                                }
                                                                                if (cost < 0) {
                                                                                        p.showDialog(createErrorDialog("Cost must be non-negative!", plugin, () -> createTierAddDialog(plugin)));
                                                                                        return;
                                                                                }

                                                                                addNewTier(plugin, maxDist, cost);
                                                                                broadcastAdmin(p, "added distance tier (max: " + maxDist + ", cost: " + cost + ")");
                                                                                p.showDialog(createDistanceConfigDialog(plugin));
                                                                        } catch (NumberFormatException e) {
                                                                                p.showDialog(createErrorDialog("Invalid number! Enter integers only.", plugin, () -> createTierAddDialog(plugin)));
                                                                        }
                                                                }
                                                ),
                                                cancelButton("Cancel and go back", () -> createDistanceConfigDialog(plugin))
                ))
        );
    }

    // ── Tier Helper Methods ──────────────────────────────────────────

    private static List<Map<String, Object>> loadTiers(LodestoneTP plugin) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<?> raw = plugin.getConfig().getList("cost.distance.tiers");
        if (raw != null) {
            for (Object obj : raw) {
                if (obj instanceof Map<?, ?> map) {
                    Map<String, Object> tier = new HashMap<>();
                    tier.put("max-distance", map.get("max-distance"));
                    tier.put("cost", map.get("cost"));
                    result.add(tier);
                }
            }
        }
        return result;
    }

    private static void saveTiers(LodestoneTP plugin, List<Map<String, Object>> tiers) {
        // Sort: tiers with -1 always last, others ascending by max-distance
        tiers.sort((a, b) -> {
            int aMax = a.get("max-distance") instanceof Number n ? n.intValue() : -1;
            int bMax = b.get("max-distance") instanceof Number n ? n.intValue() : -1;
            if (aMax == -1 && bMax == -1) return 0;
            if (aMax == -1) return 1;
            if (bMax == -1) return -1;
            return Integer.compare(aMax, bMax);
        });

        plugin.getConfig().set("cost.distance.tiers", tiers);
        persistConfig(plugin);
    }

    private static void saveTierAtIndex(LodestoneTP plugin, int index, int maxDistance, int cost) {
        List<Map<String, Object>> tiers = loadTiers(plugin);
        if (index >= 0 && index < tiers.size()) {
            Map<String, Object> tier = tiers.get(index);
            tier.put("max-distance", maxDistance);
            tier.put("cost", cost);
            saveTiers(plugin, tiers);
        }
    }

    private static void removeTierAtIndex(LodestoneTP plugin, int index) {
        List<Map<String, Object>> tiers = loadTiers(plugin);
        if (index >= 0 && index < tiers.size()) {
            tiers.remove(index);
            saveTiers(plugin, tiers);
        }
    }

    private static void addNewTier(LodestoneTP plugin, int maxDistance, int cost) {
        List<Map<String, Object>> tiers = loadTiers(plugin);
        Map<String, Object> newTier = new HashMap<>();
        newTier.put("max-distance", maxDistance);
        newTier.put("cost", cost);
        tiers.add(newTier);
        saveTiers(plugin, tiers);
    }

    // ── Teleporter Management ────────────────────────────────────────

    public static Dialog createTeleporterListDialog(LodestoneTP plugin) {
        DatabaseManager db = plugin.getDatabaseManager();
        List<Teleporter> teleporters = db.getAllTeleporters();

        if (teleporters.isEmpty()) {
            // Empty state dialog
                        ActionButton backButton = backButton("Return to admin panel", () -> createMainPanel(plugin));

            return Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(title("Manage Teleporters"))
                            .body(List.of(
                                    messageBody(ComponentFormatter.neutral("Total teleporters: 0")),
                                    messageBody(ComponentFormatter.neutral("No teleporters exist yet."))
                            ))
                            .canCloseWithEscape(true)
                            .build())
                    .type(DialogType.multiAction(List.of(), backButton, 2))
            );
        }

        List<ActionButton> buttons = new ArrayList<>();

        for (Teleporter tp : teleporters) {
            String ownerName = resolvePlayerName(tp.ownerUuid());
            buttons.add(openDialogButton(
                    Component.text(tp.name()),
                    teleporterTooltip(tp, ownerName),
                    200,
                    () -> createTeleporterActionDialog(plugin, tp)
            ));
        }

        ActionButton backButton = backButton("Return to admin panel", () -> createMainPanel(plugin));

        List<DialogBody> body = new ArrayList<>();
        body.add(messageBody(ComponentFormatter.neutral("Total teleporters: " + teleporters.size())));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Manage Teleporters"))
                        .body(body)
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, backButton, 2))
        );
    }

    public static Dialog createTeleporterActionDialog(LodestoneTP plugin, Teleporter tp) {
        DatabaseManager db = plugin.getDatabaseManager();
        String ownerName = resolvePlayerName(tp.ownerUuid());

        List<ActionButton> buttons = new ArrayList<>();

        // Delete button
        buttons.add(openDialogButton(
                Component.text("🔴 Delete", NamedTextColor.RED),
                Component.text("Delete this teleporter permanently"),
                200,
                () -> createAdminDeleteConfirmDialog(plugin, tp)
        ));

        // Toggle visibility
        buttons.add(actionButton(
                Component.text(tp.isPublic() ? "🔒 Make Private" : "🔓 Make Public", tp.isPublic() ? NamedTextColor.YELLOW : NamedTextColor.GREEN),
                Component.text("Toggle visibility (currently " + (tp.isPublic() ? "Public" : "Private") + ")"),
                200,
                (view, audience) -> {
                    if (!(audience instanceof Player p)) return;
                    boolean newValue = !tp.isPublic();
                    db.setPublic(tp.id(), newValue);
                    broadcastAdmin(p, "set teleporter '" + tp.name() + "' to " + (newValue ? "public" : "private"));
                    Teleporter updated = new Teleporter(
                            tp.id(), tp.name(), tp.world(),
                            tp.x(), tp.y(), tp.z(), tp.yaw(),
                            tp.ownerUuid(), newValue, tp.cooldownOverride(), tp.networkId(), tp.linkedTeleporterId()
                    );
                    p.showDialog(createTeleporterActionDialog(plugin, updated));
                }
        ));

        // Teleport here
        buttons.add(actionButton(
                Component.text("🚀 Teleport Here", NamedTextColor.AQUA),
                Component.text("Teleport to this teleporter"),
                200,
                (view, audience) -> {
                    if (!(audience instanceof Player p)) return;
                    var location = tp.toLocation();
                    if (location == null) {
                        p.sendMessage(ComponentFormatter.error("World '" + tp.world() + "' is not loaded!"));
                        return;
                    }
                    plugin.getTeleportEffects().playDeparture(p.getLocation());
                    p.teleport(location);
                    plugin.getTeleportEffects().playArrival(p, location);
                    p.sendMessage(ComponentFormatter.success("Teleported to ")
                            .append(ComponentFormatter.teleporterName(tp.name())));
                }
        ));

        buttons.add(noopButton(
                Component.text("👤 Owner: " + ownerName),
                Component.text("UUID: " + (tp.ownerUuid() != null ? tp.ownerUuid() : "Unknown")),
                200
        ));

        ActionButton backButton = backButton("Return to teleporter list", () -> createTeleporterListDialog(plugin));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(teleporterTitle(tp))
                        .body(List.of(
                                messageBody(teleporterLocation(tp)),
                                messageBody(teleporterVisibility(tp)),
                                messageBody(ComponentFormatter.neutral("Owner: " + ownerName))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, backButton, 2))
        );
    }

    public static Dialog createAdminDeleteConfirmDialog(LodestoneTP plugin, Teleporter tp) {
        DatabaseManager db = plugin.getDatabaseManager();

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(ComponentFormatter.error("Delete Teleporter?"))
                        .body(List.of(
                                messageBody(Component.text("Are you sure you want to delete '" + tp.name() + "'? This cannot be undone.", NamedTextColor.WHITE))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                                                deleteButton(
                                                                "Yes, Delete",
                                                                "Permanently delete this teleporter",
                                                                150,
                                                                (view, audience) -> {
                                                                        if (!(audience instanceof Player p)) return;
                                                                        if (db.removeTeleporter(tp.id())) {
                                                                                broadcastAdmin(p, "deleted teleporter '" + tp.name() + "'");
                                                                                plugin.getTeleportEffects().removeLightBlock(tp);
                                                                                plugin.getTeleportEffects().refreshTeleporterLocations();
                                                                                p.showDialog(createTeleporterListDialog(plugin));
                                                                        } else {
                                                                                p.sendMessage(ComponentFormatter.error("Failed to delete teleporter!"));
                                                                        }
                                                                }
                                                ),
                                                cancelButton("Cancel deletion", () -> createTeleporterActionDialog(plugin, tp))
                ))
        );
    }

    // ── Default Visibility ───────────────────────────────────────────

    public static Dialog createDefaultVisibilityDialog(LodestoneTP plugin) {
        boolean defaultPublic = plugin.getConfig().getBoolean("defaults.public", true);

        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(actionButton(
                Component.text(defaultPublic ? "Switch to Private" : "Switch to Public", defaultPublic ? NamedTextColor.YELLOW : NamedTextColor.GREEN),
                Component.text("New teleporters will be " + (defaultPublic ? "private" : "public") + " by default"),
                200,
                (view, audience) -> {
                    if (!(audience instanceof Player p)) return;
                    boolean newValue = !defaultPublic;
                    plugin.getConfig().set("defaults.public", newValue);
                    persistConfig(plugin);
                    broadcastAdmin(p, "changed default visibility to " + (newValue ? "public" : "private"));
                    p.showDialog(createDefaultVisibilityDialog(plugin));
                }
        ));

        ActionButton backButton = backButton("Return to admin panel", () -> createMainPanel(plugin));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Default Visibility"))
                        .body(List.of(
                                messageBody(Component.text("New teleporters default to: " + (defaultPublic ? "Public" : "Private"),
                                        defaultPublic ? NamedTextColor.GREEN : NamedTextColor.RED))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, backButton, 1))
        );
    }

    // ── Effects Settings ─────────────────────────────────────────────

    public static Dialog createEffectsDialog(LodestoneTP plugin) {
        boolean effectsEnabled = plugin.getConfig().getBoolean("effects.enabled", true);
        boolean soundsEnabled = plugin.getConfig().getBoolean("effects.sounds", true);
        boolean particlesEnabled = plugin.getConfig().getBoolean("effects.particles", true);

        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(actionButton(
                Component.text(effectsEnabled ? "Disable All Effects" : "Enable All Effects",
                        effectsEnabled ? NamedTextColor.RED : NamedTextColor.GREEN),
                Component.text("Master toggle for all effects"),
                200,
                (view, audience) -> {
                    if (!(audience instanceof Player p)) return;
                    plugin.getConfig().set("effects.enabled", !effectsEnabled);
                    persistConfig(plugin);
                    broadcastAdmin(p, (effectsEnabled ? "disabled" : "enabled") + " all effects");
                    p.showDialog(createEffectsDialog(plugin));
                }
        ));

        buttons.add(actionButton(
                Component.text(soundsEnabled ? "Disable Teleport Sounds" : "Enable Teleport Sounds",
                        soundsEnabled ? NamedTextColor.YELLOW : NamedTextColor.GREEN),
                Component.text("Sound effects when teleporting"),
                200,
                (view, audience) -> {
                    if (!(audience instanceof Player p)) return;
                    plugin.getConfig().set("effects.sounds", !soundsEnabled);
                    persistConfig(plugin);
                    broadcastAdmin(p, (soundsEnabled ? "disabled" : "enabled") + " teleport sounds");
                    p.showDialog(createEffectsDialog(plugin));
                }
        ));

        buttons.add(actionButton(
                Component.text(particlesEnabled ? "Disable Teleport Particles" : "Enable Teleport Particles",
                        particlesEnabled ? NamedTextColor.YELLOW : NamedTextColor.GREEN),
                Component.text("Particle effects when teleporting"),
                200,
                (view, audience) -> {
                    if (!(audience instanceof Player p)) return;
                    plugin.getConfig().set("effects.particles", !particlesEnabled);
                    persistConfig(plugin);
                    broadcastAdmin(p, (particlesEnabled ? "disabled" : "enabled") + " teleport particles");
                    p.showDialog(createEffectsDialog(plugin));
                }
        ));

        buttons.add(openDialogButton(
                Component.text("🔊 Ambient Settings"),
                Component.text("Configure ambient sound and particles at teleporters"),
                200,
                () -> createAmbientDialog(plugin)
        ));

        buttons.add(openDialogButton(
                Component.text("💡 Light Settings"),
                Component.text("Configure light emission above teleporters"),
                200,
                () -> createLightDialog(plugin)
        ));

        ActionButton backButton = backButton("Return to admin panel", () -> createMainPanel(plugin));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Effects Settings"))
                        .body(List.of(
                                messageBody(Component.text("Master: " + (effectsEnabled ? "Enabled" : "Disabled") +
                                        " | Sounds: " + (soundsEnabled ? "On" : "Off") +
                                        " | Particles: " + (particlesEnabled ? "On" : "Off") +
                                        " | Light: " + (plugin.getConfig().getBoolean("effects.light.enabled", true) ? "On" : "Off"),
                                        effectsEnabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, backButton, 2))
        );
    }

    // ── Ambient Settings ──────────────────────────────────────────────

    public static Dialog createAmbientDialog(LodestoneTP plugin) {
        boolean ambientEnabled = plugin.getConfig().getBoolean("effects.ambient.enabled", true);
        boolean ambientParticles = plugin.getConfig().getBoolean("effects.ambient.particles", true);
        double volume = plugin.getConfig().getDouble("effects.ambient.volume", 0.4);
        double range = plugin.getConfig().getDouble("effects.ambient.range", 8.0);
        int interval = plugin.getConfig().getInt("effects.ambient.interval-ticks", 80);

        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(actionButton(
                Component.text(ambientEnabled ? "Disable Ambient Sound" : "Enable Ambient Sound",
                        ambientEnabled ? NamedTextColor.RED : NamedTextColor.GREEN),
                Component.text("Looping sound at teleporter locations"),
                200,
                (view, audience) -> {
                    if (!(audience instanceof Player p)) return;
                    plugin.getConfig().set("effects.ambient.enabled", !ambientEnabled);
                    persistConfig(plugin);
                    plugin.getTeleportEffects().startAmbientLoop();
                    broadcastAdmin(p, (ambientEnabled ? "disabled" : "enabled") + " ambient sound");
                    p.showDialog(createAmbientDialog(plugin));
                }
        ));

        buttons.add(actionButton(
                Component.text(ambientParticles ? "Disable Ambient Particles" : "Enable Ambient Particles",
                        ambientParticles ? NamedTextColor.YELLOW : NamedTextColor.GREEN),
                Component.text("Floating particles at teleporter locations"),
                200,
                (view, audience) -> {
                    if (!(audience instanceof Player p)) return;
                    plugin.getConfig().set("effects.ambient.particles", !ambientParticles);
                    persistConfig(plugin);
                    plugin.getTeleportEffects().startAmbientLoop();
                    broadcastAdmin(p, (ambientParticles ? "disabled" : "enabled") + " ambient particles");
                    p.showDialog(createAmbientDialog(plugin));
                }
        ));

        buttons.add(openDialogButton(
                Component.text("Set Volume (" + volume + ")"),
                Component.text("Current: " + volume + " (0.0 - 1.0)"),
                200,
                () -> createAmbientVolumeInputDialog(plugin)
        ));

        buttons.add(openDialogButton(
                Component.text("Set Range (" + range + ")"),
                Component.text("Current: " + range + " blocks"),
                200,
                () -> createAmbientRangeInputDialog(plugin)
        ));

        buttons.add(openDialogButton(
                Component.text("Set Interval (" + interval + ")"),
                Component.text("Current: " + interval + " ticks (" + (interval / 20.0) + "s)"),
                200,
                () -> createAmbientIntervalInputDialog(plugin)
        ));

        ActionButton backButton = backButton("Return to effects settings", () -> createEffectsDialog(plugin));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Ambient Settings"))
                        .body(List.of(
                                messageBody(Component.text(
                                        "Sound: " + (ambientEnabled ? "On" : "Off") +
                                        " | Particles: " + (ambientParticles ? "On" : "Off") +
                                        " | Vol: " + volume + " | Range: " + range + "b | Every " + (interval / 20.0) + "s",
                                        NamedTextColor.WHITE))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, backButton, 2))
        );
    }

    private static Dialog createAmbientVolumeInputDialog(LodestoneTP plugin) {
        double current = plugin.getConfig().getDouble("effects.ambient.volume", 0.4);
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Set Ambient Volume"))
                        .body(List.of(
                                messageBody(Component.text("Enter a value between 0.0 and 1.0", NamedTextColor.WHITE))
                        ))
                        .inputs(List.of(
                                DialogInput.text(AMBIENT_VOLUME_INPUT_KEY,
                                        Component.text("Volume")).initial(String.valueOf(current)).maxLength(5).width(200).build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                                                saveButton(
                                                                Component.text("Save volume setting"),
                                                                150,
                                                                (view, audience) -> {
                                                                        if (!(audience instanceof Player p)) return;
                                                                        String input = view.getText(AMBIENT_VOLUME_INPUT_KEY);
                                                                        if (input == null || input.isBlank()) {
                                                                                p.showDialog(createErrorDialog("Please enter a value.", plugin, () -> createAmbientVolumeInputDialog(plugin)));
                                                                                return;
                                                                        }
                                                                        try {
                                                                                double val = Double.parseDouble(input.trim());
                                                                                if (val < 0.0 || val > 1.0) throw new NumberFormatException();
                                                                                plugin.getConfig().set("effects.ambient.volume", val);
                                                                                persistConfig(plugin);
                                                                                plugin.getTeleportEffects().startAmbientLoop();
                                                                                broadcastAdmin(p, "set ambient volume to " + val);
                                                                                p.showDialog(createAmbientDialog(plugin));
                                                                        } catch (NumberFormatException e) {
                                                                                p.showDialog(createErrorDialog("Invalid volume. Enter a number between 0.0 and 1.0.", plugin, () -> createAmbientVolumeInputDialog(plugin)));
                                                                        }
                                                                }
                                                ),
                        backButton("Return to ambient settings", () -> createAmbientDialog(plugin))
                ))
        );
    }

    private static Dialog createAmbientRangeInputDialog(LodestoneTP plugin) {
        double current = plugin.getConfig().getDouble("effects.ambient.range", 8.0);
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Set Ambient Range"))
                        .body(List.of(
                                messageBody(Component.text("Enter distance in blocks (1-64)", NamedTextColor.WHITE))
                        ))
                        .inputs(List.of(
                                DialogInput.text(AMBIENT_RANGE_INPUT_KEY,
                                        Component.text("Range")).initial(String.valueOf(current)).maxLength(5).width(200).build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                                                saveButton(
                                                                Component.text("Save range setting"),
                                                                150,
                                                                (view, audience) -> {
                                                                        if (!(audience instanceof Player p)) return;
                                                                        String input = view.getText(AMBIENT_RANGE_INPUT_KEY);
                                                                        if (input == null || input.isBlank()) {
                                                                                p.showDialog(createErrorDialog("Please enter a value.", plugin, () -> createAmbientRangeInputDialog(plugin)));
                                                                                return;
                                                                        }
                                                                        try {
                                                                                double val = Double.parseDouble(input.trim());
                                                                                if (val < 1 || val > 64) throw new NumberFormatException();
                                                                                plugin.getConfig().set("effects.ambient.range", val);
                                                                                persistConfig(plugin);
                                                                                plugin.getTeleportEffects().startAmbientLoop();
                                                                                broadcastAdmin(p, "set ambient range to " + val + " blocks");
                                                                                p.showDialog(createAmbientDialog(plugin));
                                                                        } catch (NumberFormatException e) {
                                                                                p.showDialog(createErrorDialog("Invalid range. Enter a number between 1 and 64.", plugin, () -> createAmbientRangeInputDialog(plugin)));
                                                                        }
                                                                }
                                                ),
                        backButton("Return to ambient settings", () -> createAmbientDialog(plugin))
                ))
        );
    }

    private static Dialog createAmbientIntervalInputDialog(LodestoneTP plugin) {
        int current = plugin.getConfig().getInt("effects.ambient.interval-ticks", 80);
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Set Ambient Interval"))
                        .body(List.of(
                                messageBody(Component.text("Enter interval in ticks (20 = 1 second)", NamedTextColor.WHITE))
                        ))
                        .inputs(List.of(
                                DialogInput.text(AMBIENT_INTERVAL_INPUT_KEY,
                                        Component.text("Ticks")).initial(String.valueOf(current)).maxLength(5).width(200).build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                                                saveButton(
                                                                Component.text("Save interval setting"),
                                                                150,
                                                                (view, audience) -> {
                                                                        if (!(audience instanceof Player p)) return;
                                                                        String input = view.getText(AMBIENT_INTERVAL_INPUT_KEY);
                                                                        if (input == null || input.isBlank()) {
                                                                                p.showDialog(createErrorDialog("Please enter a value.", plugin, () -> createAmbientIntervalInputDialog(plugin)));
                                                                                return;
                                                                        }
                                                                        try {
                                                                                int val = Integer.parseInt(input.trim());
                                                                                if (val < 20 || val > 6000) throw new NumberFormatException();
                                                                                plugin.getConfig().set("effects.ambient.interval-ticks", val);
                                                                                persistConfig(plugin);
                                                                                plugin.getTeleportEffects().startAmbientLoop();
                                                                                broadcastAdmin(p, "set ambient interval to " + val + " ticks (" + (val / 20.0) + "s)");
                                                                                p.showDialog(createAmbientDialog(plugin));
                                                                        } catch (NumberFormatException e) {
                                                                                p.showDialog(createErrorDialog("Invalid interval. Enter a number between 20 and 6000.", plugin, () -> createAmbientIntervalInputDialog(plugin)));
                                                                        }
                                                                }
                                                ),
                        backButton("Return to ambient settings", () -> createAmbientDialog(plugin))
                ))
        );
    }


    // ── Light Emission ──────────────────────────────────────────────

    public static Dialog createLightDialog(LodestoneTP plugin) {
        boolean lightEnabled = plugin.getConfig().getBoolean("effects.light.enabled", true);
        int level = plugin.getConfig().getInt("effects.light.level", 10);

        List<ActionButton> buttons = new ArrayList<>();

                buttons.add(actionButton(
                                Component.text(lightEnabled ? "Disable Light" : "Enable Light",
                                                lightEnabled ? NamedTextColor.RED : NamedTextColor.GREEN),
                                Component.text("Toggle light emission above teleporters"),
                                200,
                                (view, audience) -> {
                                        if (!(audience instanceof Player p)) return;
                                        plugin.getConfig().set("effects.light.enabled", !lightEnabled);
                                        persistConfig(plugin);
                                        if (!lightEnabled) {
                                                plugin.getTeleportEffects().restoreAllLightBlocks();
                                        } else {
                                                plugin.getTeleportEffects().removeAllLightBlocks();
                                        }
                                        broadcastAdmin(p, (lightEnabled ? "disabled" : "enabled") + " teleporter light emission");
                                        p.showDialog(createLightDialog(plugin));
                                }
                ));

        buttons.add(openDialogButton(
                Component.text("Set Light Level (" + level + ")"),
                Component.text("Current: " + level + "/15"),
                200,
                () -> createLightLevelInputDialog(plugin)
        ));

        buttons.add(backButton("Return to Effects Settings", () -> createEffectsDialog(plugin)));

        return Dialog.create(builder -> builder
                .empty()
                .base(DialogBase.builder(Component.text("💡 Light Settings", NamedTextColor.YELLOW))
                        .body(List.of(
                                messageBody(Component.text(
                                        "Status: " + (lightEnabled ? "Enabled" : "Disabled") +
                                        " | Level: " + level + "/15",
                                        lightEnabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons,
                        closeButton("Close dialog"),
                        1))
        );
    }

    private static Dialog createLightLevelInputDialog(LodestoneTP plugin) {
        int current = plugin.getConfig().getInt("effects.light.level", 10);

        return Dialog.create(builder -> builder
                .empty()
                .base(DialogBase.builder(Component.text("Set Light Level", NamedTextColor.YELLOW))
                        .body(List.of(
                                messageBody(ComponentFormatter.neutral(
                                        "Current: " + current + ". Enter a value from 0 to 15."))
                        ))
                        .inputs(List.of(
                                DialogInput.text(LIGHT_LEVEL_INPUT_KEY, Component.text("Light Level"))
                                        .initial(String.valueOf(current))
                                        .maxLength(2)
                                        .width(100)
                                        .build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                                                actionButton(
                                                                Component.text("Save", NamedTextColor.GREEN),
                                                                Component.text("Apply light level"),
                                                                100,
                                                                (view, audience) -> {
                                                                        if (!(audience instanceof Player p)) return;
                                                                        String input = view.getText(LIGHT_LEVEL_INPUT_KEY);
                                                                        try {
                                                                                int val = Integer.parseInt(input.trim());
                                                                                if (val < 0 || val > 15) {
                                                                                        p.sendMessage(ComponentFormatter.error("Light level must be 0-15."));
                                                                                } else {
                                                                                        plugin.getConfig().set("effects.light.level", val);
                                                                                        persistConfig(plugin);
                                                                                        plugin.getTeleportEffects().removeAllLightBlocks();
                                                                                        plugin.getTeleportEffects().restoreAllLightBlocks();
                                                                                        broadcastAdmin(p, "set light level to " + val);
                                                                                }
                                                                        } catch (NumberFormatException e) {
                                                                                p.sendMessage(ComponentFormatter.error("Invalid number."));
                                                                        }
                                                                        p.showDialog(createLightDialog(plugin));
                                                                }
                                                ),
                                                openDialogButton(Component.text("Cancel", NamedTextColor.GRAY), Component.text("Go back"), 100,
                                                                () -> createLightDialog(plugin))
                ))
        );
    }
    // ── Creation Fee ──────────────────────────────────────────────────

    public static Dialog createCreationFeeDialog(LodestoneTP plugin) {
        boolean feeEnabled = plugin.getConfig().getBoolean("creation-fee.enabled", true);
        String materialName = plugin.getConfig().getString("creation-fee.material", "ENDER_PEARL");
        int amount = plugin.getConfig().getInt("creation-fee.amount", 1);

        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(actionButton(
                Component.text(feeEnabled ? "Disable Creation Fee" : "Enable Creation Fee",
                        feeEnabled ? NamedTextColor.RED : NamedTextColor.GREEN),
                Component.text("Toggle item cost to create teleporters"),
                200,
                (view, audience) -> {
                    if (!(audience instanceof Player p)) return;
                    plugin.getConfig().set("creation-fee.enabled", !feeEnabled);
                    persistConfig(plugin);
                    broadcastAdmin(p, (feeEnabled ? "disabled" : "enabled") + " creation fee");
                    p.showDialog(createCreationFeeDialog(plugin));
                }
        ));

        buttons.add(openDialogButton(
                Component.text("Set Material (" + materialName + ")"),
                Component.text("Current: " + materialName),
                200,
                () -> createCreationFeeMaterialInputDialog(plugin)
        ));

        buttons.add(openDialogButton(
                Component.text("Set Amount (" + amount + ")"),
                Component.text("Current: " + amount),
                200,
                () -> createCreationFeeAmountInputDialog(plugin)
        ));

        ActionButton backButton = backButton("Return to admin panel", () -> createMainPanel(plugin));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Creation Fee"))
                        .body(List.of(
                                messageBody(Component.text(
                                        feeEnabled ? "Requires " + amount + "x " + materialName + " to create a teleporter" : "Creation fee is disabled",
                                        feeEnabled ? NamedTextColor.GREEN : NamedTextColor.YELLOW))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, backButton, 1))
        );
    }

    private static Dialog createCreationFeeMaterialInputDialog(LodestoneTP plugin) {
        String current = plugin.getConfig().getString("creation-fee.material", "ENDER_PEARL");
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Set Creation Fee Material"))
                        .body(List.of(
                                messageBody(Component.text("Enter a valid Bukkit material name (e.g. ENDER_PEARL, DIAMOND)", NamedTextColor.WHITE))
                        ))
                        .inputs(List.of(
                                DialogInput.text(CREATION_FEE_MATERIAL_INPUT_KEY,
                                        Component.text("Material")).initial(current).maxLength(50).width(250).build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                                                saveButton(
                                                                Component.text("Save material setting"),
                                                                150,
                                                                (view, audience) -> {
                                                                        if (!(audience instanceof Player p)) return;
                                                                        String input = view.getText(CREATION_FEE_MATERIAL_INPUT_KEY);
                                                                        if (input == null || input.isBlank()) {
                                                                                p.showDialog(createErrorDialog("Please enter a material name.", plugin, () -> createCreationFeeMaterialInputDialog(plugin)));
                                                                                return;
                                                                        }
                                                                        String val = input.toUpperCase().trim();
                                                                        if (Material.matchMaterial(val) == null) {
                                                                                p.showDialog(createErrorDialog("Unknown material: " + val, plugin, () -> createCreationFeeMaterialInputDialog(plugin)));
                                                                                return;
                                                                        }
                                                                        plugin.getConfig().set("creation-fee.material", val);
                                                                        persistConfig(plugin);
                                                                        broadcastAdmin(p, "set creation fee material to " + val);
                                                                        p.showDialog(createCreationFeeDialog(plugin));
                                                                }
                                                ),
                        backButton("Return to creation fee settings", () -> createCreationFeeDialog(plugin))
                ))
        );
    }

    private static Dialog createCreationFeeAmountInputDialog(LodestoneTP plugin) {
        int current = plugin.getConfig().getInt("creation-fee.amount", 1);
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title("Set Creation Fee Amount"))
                        .body(List.of(
                                messageBody(Component.text("Enter amount (1-64)", NamedTextColor.WHITE))
                        ))
                        .inputs(List.of(
                                DialogInput.text(CREATION_FEE_AMOUNT_INPUT_KEY,
                                        Component.text("Amount")).initial(String.valueOf(current)).maxLength(3).width(200).build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                                                saveButton(
                                                                Component.text("Save amount setting"),
                                                                150,
                                                                (view, audience) -> {
                                                                        if (!(audience instanceof Player p)) return;
                                                                        String input = view.getText(CREATION_FEE_AMOUNT_INPUT_KEY);
                                                                        if (input == null || input.isBlank()) {
                                                                                p.showDialog(createErrorDialog("Please enter an amount.", plugin, () -> createCreationFeeAmountInputDialog(plugin)));
                                                                                return;
                                                                        }
                                                                        try {
                                                                                int val = Integer.parseInt(input.trim());
                                                                                if (val < 1 || val > 64) throw new NumberFormatException();
                                                                                plugin.getConfig().set("creation-fee.amount", val);
                                                                                persistConfig(plugin);
                                                                                broadcastAdmin(p, "set creation fee amount to " + val);
                                                                                p.showDialog(createCreationFeeDialog(plugin));
                                                                        } catch (NumberFormatException e) {
                                                                                p.showDialog(createErrorDialog("Invalid amount. Enter a number between 1 and 64.", plugin, () -> createCreationFeeAmountInputDialog(plugin)));
                                                                        }
                                                                }
                                                ),
                        backButton("Return to creation fee settings", () -> createCreationFeeDialog(plugin))
                ))
        );
    }

    // ── Utilities ────────────────────────────────────────────────────

    private static String resolvePlayerName(String uuid) {
        if (uuid == null) return "Unknown";
        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            String name = player.getName();
            return name != null ? name : uuid.substring(0, 8);
        } catch (IllegalArgumentException e) {
            return uuid.substring(0, Math.min(8, uuid.length()));
        }
    }
}
