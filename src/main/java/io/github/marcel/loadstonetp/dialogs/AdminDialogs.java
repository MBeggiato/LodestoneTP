package io.github.marcel.loadstonetp.dialogs;

import io.github.marcel.loadstonetp.LodestoneTP;
import io.github.marcel.loadstonetp.db.DatabaseManager;
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

    private AdminDialogs() {}

    private static ClickCallback.Options clickOptions() {
        return ClickCallback.Options.builder()
                .uses(1)
                .lifetime(Duration.ofMinutes(5))
                .build();
    }

    private static void broadcastAdmin(Player actor, String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isOp() || p.hasPermission("lodestonetp.admin")) {
                p.sendMessage(Component.text("[LodestoneTP] ", NamedTextColor.GOLD)
                        .append(Component.text("Admin " + actor.getName() + " " + message, NamedTextColor.YELLOW)));
            }
        }
    }

    private static Dialog createErrorDialog(String message, LodestoneTP plugin, java.util.function.Supplier<Dialog> returnDialog) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Error", NamedTextColor.RED))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text(message, NamedTextColor.RED))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.notice(
                        ActionButton.create(
                                Component.text("Back"),
                                Component.text("Return to previous screen"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(returnDialog.get());
                                            }
                                        },
                                        clickOptions()
                                )
                        )
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

        buttons.add(ActionButton.create(
                Component.text("⚙ Cooldown Settings"),
                Component.text("Currently: " + (cooldownEnabled ? "enabled" : "disabled") + ", " + cooldownSeconds + " seconds"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createCooldownDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text("💰 Cost Settings"),
                Component.text("Currently: " + (costEnabled ? "enabled" : "disabled") + ", type: " + costType),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createCostDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text("🗼 Manage Teleporters"),
                Component.text("View and manage all teleporters"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createTeleporterListDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text("🌐 Default Visibility"),
                Component.text("Currently: " + (defaultPublic ? "Public" : "Private")),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createDefaultVisibilityDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        boolean effectsEnabled = plugin.getConfig().getBoolean("effects.enabled", true);
        buttons.add(ActionButton.create(
                Component.text("✨ Effects Settings"),
                Component.text("Currently: " + (effectsEnabled ? "enabled" : "disabled")),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createEffectsDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        boolean feeEnabled = plugin.getConfig().getBoolean("creation-fee.enabled", true);
        String feeMaterial = plugin.getConfig().getString("creation-fee.material", "ENDER_PEARL");
        int feeAmount = plugin.getConfig().getInt("creation-fee.amount", 1);
        buttons.add(ActionButton.create(
                Component.text("💎 Creation Fee"),
                Component.text("Currently: " + (feeEnabled ? feeAmount + "x " + feeMaterial : "disabled")),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createCreationFeeDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        ActionButton exitButton = ActionButton.create(
                Component.text("Close"),
                Component.text("Close the admin panel"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {},
                        clickOptions()
                )
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("LodestoneTP Admin Panel", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Manage all plugin settings from here.", NamedTextColor.WHITE))
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

        buttons.add(ActionButton.create(
                Component.text(enabled ? "Disable Cooldown" : "Enable Cooldown", enabled ? NamedTextColor.RED : NamedTextColor.GREEN),
                Component.text("Toggle cooldown " + (enabled ? "off" : "on")),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            boolean newValue = !enabled;
                            plugin.getConfig().set("cooldown.enabled", newValue);
                            plugin.saveConfig();
                            plugin.reloadConfig();
                            broadcastAdmin(p, "changed cooldown to " + (newValue ? "enabled" : "disabled"));
                            p.showDialog(createCooldownDialog(plugin));
                        },
                        clickOptions()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text("Set Seconds (" + seconds + ")"),
                Component.text("Change the cooldown duration"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createCooldownSecondsInputDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        ActionButton backButton = ActionButton.create(
                Component.text("Back"),
                Component.text("Return to admin panel"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createMainPanel(plugin));
                            }
                        },
                        clickOptions()
                )
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Cooldown Settings", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Status: " + (enabled ? "Enabled" : "Disabled"), enabled ? NamedTextColor.GREEN : NamedTextColor.RED)),
                                DialogBody.plainMessage(Component.text("Duration: " + seconds + " seconds", NamedTextColor.GRAY))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, backButton, 1))
        );
    }

    public static Dialog createCooldownSecondsInputDialog(LodestoneTP plugin) {
        int current = plugin.getConfig().getInt("cooldown.seconds", 10);

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Set Cooldown Seconds", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Enter the cooldown duration in seconds.", NamedTextColor.WHITE))
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
                        ActionButton.create(
                                Component.text("Save"),
                                Component.text("Save the cooldown duration"),
                                150,
                                DialogAction.customClick(
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
                                                plugin.saveConfig();
                                                plugin.reloadConfig();
                                                plugin.getCooldownManager().setCooldownSeconds(value);
                                                broadcastAdmin(p, "changed cooldown to " + value + " seconds");
                                                p.showDialog(createCooldownDialog(plugin));
                                            } catch (NumberFormatException e) {
                                                p.showDialog(createErrorDialog("Invalid number! Enter a positive integer.", plugin, () -> createCooldownSecondsInputDialog(plugin)));
                                            }
                                        },
                                        clickOptions()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel"),
                                Component.text("Cancel and go back"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(createCooldownDialog(plugin));
                                            }
                                        },
                                        clickOptions()
                                )
                        )
                ))
        );
    }

    // ── Cost Settings ────────────────────────────────────────────────

    public static Dialog createCostDialog(LodestoneTP plugin) {
        boolean enabled = plugin.getConfig().getBoolean("cost.enabled", false);
        String costType = plugin.getConfig().getString("cost.type", "xp_levels");

        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(ActionButton.create(
                Component.text(enabled ? "Disable Cost" : "Enable Cost", enabled ? NamedTextColor.RED : NamedTextColor.GREEN),
                Component.text("Toggle cost " + (enabled ? "off" : "on")),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            boolean newValue = !enabled;
                            plugin.getConfig().set("cost.enabled", newValue);
                            plugin.saveConfig();
                            plugin.reloadConfig();
                            broadcastAdmin(p, "changed cost to " + (newValue ? "enabled" : "disabled"));
                            p.showDialog(createCostDialog(plugin));
                        },
                        clickOptions()
                )
        ));

        // Cost type buttons
        for (String type : new String[]{"xp_levels", "item", "distance"}) {
            boolean isActive = type.equals(costType);
            buttons.add(ActionButton.create(
                    Component.text(type + (isActive ? " ✓" : ""), isActive ? NamedTextColor.GREEN : NamedTextColor.WHITE),
                    Component.text(isActive ? "Currently active" : "Switch to " + type),
                    200,
                    DialogAction.customClick(
                            (view, audience) -> {
                                if (!(audience instanceof Player p)) return;
                                plugin.getConfig().set("cost.type", type);
                                plugin.saveConfig();
                                plugin.reloadConfig();
                                broadcastAdmin(p, "changed cost type to " + type);
                                p.showDialog(createCostDialog(plugin));
                            },
                            clickOptions()
                    )
            ));
        }

        // Sub-panel button for the active cost type
        buttons.add(ActionButton.create(
                Component.text("Configure " + costType, NamedTextColor.GOLD),
                Component.text("Edit settings for " + costType),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            switch (costType) {
                                case "xp_levels" -> p.showDialog(createXpLevelsConfigDialog(plugin));
                                case "item" -> p.showDialog(createItemConfigDialog(plugin));
                                case "distance" -> p.showDialog(createDistanceConfigDialog(plugin));
                                default -> p.showDialog(createCostDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        ActionButton backButton = ActionButton.create(
                Component.text("Back"),
                Component.text("Return to admin panel"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createMainPanel(plugin));
                            }
                        },
                        clickOptions()
                )
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Cost Settings", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Status: " + (enabled ? "Enabled" : "Disabled"), enabled ? NamedTextColor.GREEN : NamedTextColor.RED)),
                                DialogBody.plainMessage(Component.text("Type: " + costType, NamedTextColor.GRAY))
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
                .base(DialogBase.builder(Component.text("XP Levels Cost", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Set the number of XP levels required per teleport.", NamedTextColor.WHITE))
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
                        ActionButton.create(
                                Component.text("Save"),
                                Component.text("Save XP level cost"),
                                150,
                                DialogAction.customClick(
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
                                                plugin.saveConfig();
                                                plugin.reloadConfig();
                                                broadcastAdmin(p, "changed XP level cost to " + value);
                                                p.showDialog(createCostDialog(plugin));
                                            } catch (NumberFormatException e) {
                                                p.showDialog(createErrorDialog("Invalid number! Enter a positive integer.", plugin, () -> createXpLevelsConfigDialog(plugin)));
                                            }
                                        },
                                        clickOptions()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel"),
                                Component.text("Cancel and go back"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(createCostDialog(plugin));
                                            }
                                        },
                                        clickOptions()
                                )
                        )
                ))
        );
    }

    // ── Item Config ──────────────────────────────────────────────────

    public static Dialog createItemConfigDialog(LodestoneTP plugin) {
        String material = plugin.getConfig().getString("cost.item.material", "ENDER_PEARL");
        int amount = plugin.getConfig().getInt("cost.item.amount", 1);

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Item Cost", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Set the item material and amount required per teleport.", NamedTextColor.WHITE)),
                                DialogBody.plainMessage(Component.text("Current: " + amount + "x " + material, NamedTextColor.GRAY))
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
                        ActionButton.create(
                                Component.text("Save"),
                                Component.text("Save item cost settings"),
                                150,
                                DialogAction.customClick(
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
                                                plugin.saveConfig();
                                                plugin.reloadConfig();
                                                broadcastAdmin(p, "changed item cost to " + amtValue + "x " + mat.name());
                                                p.showDialog(createCostDialog(plugin));
                                            } catch (NumberFormatException e) {
                                                p.showDialog(createErrorDialog("Invalid amount! Enter a positive integer.", plugin, () -> createItemConfigDialog(plugin)));
                                            }
                                        },
                                        clickOptions()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel"),
                                Component.text("Cancel and go back"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(createCostDialog(plugin));
                                            }
                                        },
                                        clickOptions()
                                )
                        )
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
            buttons.add(ActionButton.create(
                    Component.text("Currency: " + curr + (isActive ? " ✓" : ""), isActive ? NamedTextColor.GREEN : NamedTextColor.WHITE),
                    Component.text(isActive ? "Currently active" : "Switch currency to " + curr),
                    200,
                    DialogAction.customClick(
                            (view, audience) -> {
                                if (!(audience instanceof Player p)) return;
                                plugin.getConfig().set("cost.distance.currency", curr);
                                plugin.saveConfig();
                                plugin.reloadConfig();
                                broadcastAdmin(p, "changed distance currency to " + curr);
                                p.showDialog(createDistanceConfigDialog(plugin));
                            },
                            clickOptions()
                    )
            ));
        }

        // Cross-world cost button
        buttons.add(ActionButton.create(
                Component.text("Cross-World Cost (" + crossWorldCost + ")"),
                Component.text("Set the flat cost for cross-world teleports"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createCrossWorldCostInputDialog(plugin));
                            }
                        },
                        clickOptions()
                )
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

                    buttons.add(ActionButton.create(
                            Component.text("Tier: ≤" + distLabel + " blocks → " + tierCost),
                            Component.text("Edit this distance tier"),
                            200,
                            DialogAction.customClick(
                                    (view, audience) -> {
                                        if (audience instanceof Player p) {
                                            p.showDialog(createTierEditDialog(plugin, tierIndex));
                                        }
                                    },
                                    clickOptions()
                            )
                    ));
                }
            }
        }

        // Add tier button
        buttons.add(ActionButton.create(
                Component.text("+ Add Tier", NamedTextColor.GREEN),
                Component.text("Add a new distance tier"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createTierAddDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        ActionButton backButton = ActionButton.create(
                Component.text("Back"),
                Component.text("Return to cost settings"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createCostDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Distance Cost", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Currency: " + currency, NamedTextColor.GRAY)),
                                DialogBody.plainMessage(Component.text("Cross-World Cost: " + crossWorldCost, NamedTextColor.GRAY)),
                                DialogBody.plainMessage(Component.text("Tiers: " + (tiers != null ? tiers.size() : 0), NamedTextColor.GRAY))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, backButton, 2))
        );
    }

    public static Dialog createCrossWorldCostInputDialog(LodestoneTP plugin) {
        int current = plugin.getConfig().getInt("cost.distance.cross-world-cost", 10);

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Cross-World Cost", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Set the flat cost for teleporting between worlds.", NamedTextColor.WHITE))
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
                        ActionButton.create(
                                Component.text("Save"),
                                Component.text("Save cross-world cost"),
                                150,
                                DialogAction.customClick(
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
                                                plugin.saveConfig();
                                                plugin.reloadConfig();
                                                broadcastAdmin(p, "changed cross-world cost to " + value);
                                                p.showDialog(createDistanceConfigDialog(plugin));
                                            } catch (NumberFormatException e) {
                                                p.showDialog(createErrorDialog("Invalid number! Enter a non-negative integer.", plugin, () -> createCrossWorldCostInputDialog(plugin)));
                                            }
                                        },
                                        clickOptions()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel"),
                                Component.text("Cancel and go back"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(createDistanceConfigDialog(plugin));
                                            }
                                        },
                                        clickOptions()
                                )
                        )
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
        buttons.add(ActionButton.create(
                Component.text("Edit Values"),
                Component.text("Change max-distance and cost"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createTierValuesInputDialog(plugin, tierIndex, finalMaxDist, finalCost));
                            }
                        },
                        clickOptions()
                )
        ));

        // Delete tier
        buttons.add(ActionButton.create(
                Component.text("Delete Tier", NamedTextColor.RED),
                Component.text("Remove this tier"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            p.showDialog(createTierDeleteConfirmDialog(plugin, tierIndex));
                        },
                        clickOptions()
                )
        ));

        ActionButton backButton = ActionButton.create(
                Component.text("Back"),
                Component.text("Return to distance settings"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createDistanceConfigDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        );

        String distLabel = currentMaxDist == -1 ? "unlimited" : String.valueOf(currentMaxDist);
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Edit Tier", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Max Distance: " + distLabel + " blocks", NamedTextColor.GRAY)),
                                DialogBody.plainMessage(Component.text("Cost: " + finalCost, NamedTextColor.GRAY))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, backButton, 1))
        );
    }

    public static Dialog createTierValuesInputDialog(LodestoneTP plugin, int tierIndex, int currentMaxDist, int currentCost) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Edit Tier Values", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Set max-distance (-1 for unlimited) and cost.", NamedTextColor.WHITE))
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
                        ActionButton.create(
                                Component.text("Save"),
                                Component.text("Save tier values"),
                                150,
                                DialogAction.customClick(
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
                                        },
                                        clickOptions()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel"),
                                Component.text("Cancel and go back"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(createTierEditDialog(plugin, tierIndex));
                                            }
                                        },
                                        clickOptions()
                                )
                        )
                ))
        );
    }

    public static Dialog createTierDeleteConfirmDialog(LodestoneTP plugin, int tierIndex) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Delete Tier?", NamedTextColor.RED))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Are you sure you want to delete this tier? This cannot be undone.", NamedTextColor.WHITE))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Yes, Delete", NamedTextColor.RED),
                                Component.text("Permanently delete this tier"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (!(audience instanceof Player p)) return;
                                            removeTierAtIndex(plugin, tierIndex);
                                            broadcastAdmin(p, "deleted a distance tier");
                                            p.showDialog(createDistanceConfigDialog(plugin));
                                        },
                                        clickOptions()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel"),
                                Component.text("Cancel deletion"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(createTierEditDialog(plugin, tierIndex));
                                            }
                                        },
                                        clickOptions()
                                )
                        )
                ))
        );
    }

    public static Dialog createTierAddDialog(LodestoneTP plugin) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Add Distance Tier", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Set max-distance (-1 for unlimited) and cost for the new tier.", NamedTextColor.WHITE))
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
                        ActionButton.create(
                                Component.text("Add"),
                                Component.text("Add this tier"),
                                150,
                                DialogAction.customClick(
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
                                        },
                                        clickOptions()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel"),
                                Component.text("Cancel and go back"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(createDistanceConfigDialog(plugin));
                                            }
                                        },
                                        clickOptions()
                                )
                        )
                ))
        );
    }

    // ── Tier Helper Methods ──────────────────────────────────────────

    @SuppressWarnings("unchecked")
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
        plugin.saveConfig();
        plugin.reloadConfig();
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
            ActionButton backButton = ActionButton.create(
                    Component.text("Back"),
                    Component.text("Return to admin panel"),
                    150,
                    DialogAction.customClick(
                            (view, audience) -> {
                                if (audience instanceof Player p) {
                                    p.showDialog(createMainPanel(plugin));
                                }
                            },
                            clickOptions()
                    )
            );

            return Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(Component.text("Manage Teleporters", NamedTextColor.GOLD))
                            .body(List.of(
                                    DialogBody.plainMessage(Component.text("Total teleporters: 0", NamedTextColor.GRAY)),
                                    DialogBody.plainMessage(Component.text("No teleporters exist yet.", NamedTextColor.GRAY))
                            ))
                            .canCloseWithEscape(true)
                            .build())
                    .type(DialogType.multiAction(List.of(), backButton, 2))
            );
        }

        List<ActionButton> buttons = new ArrayList<>();

        for (Teleporter tp : teleporters) {
            String ownerName = resolvePlayerName(tp.ownerUuid());
            Component tooltip = Component.text(tp.world() + " (" + tp.x() + ", " + tp.y() + ", " + tp.z() + ")")
                    .append(Component.text(" | Owner: " + ownerName, NamedTextColor.GRAY))
                    .append(Component.text(" | " + (tp.isPublic() ? "Public" : "Private"), tp.isPublic() ? NamedTextColor.GREEN : NamedTextColor.RED));

            buttons.add(ActionButton.create(
                    Component.text(tp.name()),
                    tooltip,
                    200,
                    DialogAction.customClick(
                            (view, audience) -> {
                                if (audience instanceof Player p) {
                                    p.showDialog(createTeleporterActionDialog(plugin, tp));
                                }
                            },
                            clickOptions()
                    )
            ));
        }

        ActionButton backButton = ActionButton.create(
                Component.text("Back"),
                Component.text("Return to admin panel"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createMainPanel(plugin));
                            }
                        },
                        clickOptions()
                )
        );

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text("Total teleporters: " + teleporters.size(), NamedTextColor.GRAY)));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Manage Teleporters", NamedTextColor.GOLD))
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
        buttons.add(ActionButton.create(
                Component.text("🔴 Delete", NamedTextColor.RED),
                Component.text("Delete this teleporter permanently"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createAdminDeleteConfirmDialog(plugin, tp));
                            }
                        },
                        clickOptions()
                )
        ));

        // Toggle visibility
        buttons.add(ActionButton.create(
                Component.text(tp.isPublic() ? "🔒 Make Private" : "🔓 Make Public", tp.isPublic() ? NamedTextColor.YELLOW : NamedTextColor.GREEN),
                Component.text("Toggle visibility (currently " + (tp.isPublic() ? "Public" : "Private") + ")"),
                200,
                DialogAction.customClick(
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
                        },
                        clickOptions()
                )
        ));

        // Teleport here
        buttons.add(ActionButton.create(
                Component.text("🚀 Teleport Here", NamedTextColor.AQUA),
                Component.text("Teleport to this teleporter"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            var location = tp.toLocation();
                            if (location == null) {
                                p.sendMessage(Component.text("World '" + tp.world() + "' is not loaded!", NamedTextColor.RED));
                                return;
                            }
                            plugin.getTeleportEffects().playDeparture(p.getLocation());
                            p.teleport(location);
                            plugin.getTeleportEffects().playArrival(p, location);
                            p.sendMessage(Component.text("Teleported to ", NamedTextColor.GREEN)
                                    .append(Component.text("\"" + tp.name() + "\"", NamedTextColor.GOLD)));
                        },
                        clickOptions()
                )
        ));

        // View owner
        buttons.add(ActionButton.create(
                Component.text("👤 Owner: " + ownerName),
                Component.text("UUID: " + (tp.ownerUuid() != null ? tp.ownerUuid() : "Unknown")),
                200,
                DialogAction.customClick(
                        (view, audience) -> {},
                        clickOptions()
                )
        ));

        ActionButton backButton = ActionButton.create(
                Component.text("Back"),
                Component.text("Return to teleporter list"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createTeleporterListDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Teleporter: ", NamedTextColor.GOLD)
                                .append(Component.text(tp.name(), NamedTextColor.WHITE)))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Location: " + tp.world() + " (" + tp.x() + ", " + tp.y() + ", " + tp.z() + ")", NamedTextColor.GRAY)),
                                DialogBody.plainMessage(Component.text("Visibility: " + (tp.isPublic() ? "Public" : "Private"), tp.isPublic() ? NamedTextColor.GREEN : NamedTextColor.RED)),
                                DialogBody.plainMessage(Component.text("Owner: " + ownerName, NamedTextColor.GRAY))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, backButton, 2))
        );
    }

    public static Dialog createAdminDeleteConfirmDialog(LodestoneTP plugin, Teleporter tp) {
        DatabaseManager db = plugin.getDatabaseManager();

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Delete Teleporter?", NamedTextColor.RED))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Are you sure you want to delete '" + tp.name() + "'? This cannot be undone.", NamedTextColor.WHITE))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Yes, Delete", NamedTextColor.RED),
                                Component.text("Permanently delete this teleporter"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (!(audience instanceof Player p)) return;
                                            if (db.removeTeleporter(tp.id())) {
                                                broadcastAdmin(p, "deleted teleporter '" + tp.name() + "'");
                                                plugin.getTeleportEffects().removeLightBlock(tp);
                                                plugin.getTeleportEffects().refreshTeleporterLocations();
                                                p.showDialog(createTeleporterListDialog(plugin));
                                            } else {
                                                p.sendMessage(Component.text("Failed to delete teleporter!", NamedTextColor.RED));
                                            }
                                        },
                                        clickOptions()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel"),
                                Component.text("Cancel deletion"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(createTeleporterActionDialog(plugin, tp));
                                            }
                                        },
                                        clickOptions()
                                )
                        )
                ))
        );
    }

    // ── Default Visibility ───────────────────────────────────────────

    public static Dialog createDefaultVisibilityDialog(LodestoneTP plugin) {
        boolean defaultPublic = plugin.getConfig().getBoolean("defaults.public", true);

        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(ActionButton.create(
                Component.text(defaultPublic ? "Switch to Private" : "Switch to Public", defaultPublic ? NamedTextColor.YELLOW : NamedTextColor.GREEN),
                Component.text("New teleporters will be " + (defaultPublic ? "private" : "public") + " by default"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            boolean newValue = !defaultPublic;
                            plugin.getConfig().set("defaults.public", newValue);
                            plugin.saveConfig();
                            plugin.reloadConfig();
                            broadcastAdmin(p, "changed default visibility to " + (newValue ? "public" : "private"));
                            p.showDialog(createDefaultVisibilityDialog(plugin));
                        },
                        clickOptions()
                )
        ));

        ActionButton backButton = ActionButton.create(
                Component.text("Back"),
                Component.text("Return to admin panel"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createMainPanel(plugin));
                            }
                        },
                        clickOptions()
                )
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Default Visibility", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("New teleporters default to: " + (defaultPublic ? "Public" : "Private"),
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

        buttons.add(ActionButton.create(
                Component.text(effectsEnabled ? "Disable All Effects" : "Enable All Effects",
                        effectsEnabled ? NamedTextColor.RED : NamedTextColor.GREEN),
                Component.text("Master toggle for all effects"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            plugin.getConfig().set("effects.enabled", !effectsEnabled);
                            plugin.saveConfig();
                            plugin.reloadConfig();
                            broadcastAdmin(p, (effectsEnabled ? "disabled" : "enabled") + " all effects");
                            p.showDialog(createEffectsDialog(plugin));
                        },
                        clickOptions()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text(soundsEnabled ? "Disable Teleport Sounds" : "Enable Teleport Sounds",
                        soundsEnabled ? NamedTextColor.YELLOW : NamedTextColor.GREEN),
                Component.text("Sound effects when teleporting"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            plugin.getConfig().set("effects.sounds", !soundsEnabled);
                            plugin.saveConfig();
                            plugin.reloadConfig();
                            broadcastAdmin(p, (soundsEnabled ? "disabled" : "enabled") + " teleport sounds");
                            p.showDialog(createEffectsDialog(plugin));
                        },
                        clickOptions()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text(particlesEnabled ? "Disable Teleport Particles" : "Enable Teleport Particles",
                        particlesEnabled ? NamedTextColor.YELLOW : NamedTextColor.GREEN),
                Component.text("Particle effects when teleporting"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            plugin.getConfig().set("effects.particles", !particlesEnabled);
                            plugin.saveConfig();
                            plugin.reloadConfig();
                            broadcastAdmin(p, (particlesEnabled ? "disabled" : "enabled") + " teleport particles");
                            p.showDialog(createEffectsDialog(plugin));
                        },
                        clickOptions()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text("🔊 Ambient Settings"),
                Component.text("Configure ambient sound and particles at teleporters"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createAmbientDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text("💡 Light Settings"),
                Component.text("Configure light emission above teleporters"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createLightDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        ActionButton backButton = ActionButton.create(
                Component.text("Back"),
                Component.text("Return to admin panel"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createMainPanel(plugin));
                            }
                        },
                        clickOptions()
                )
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Effects Settings", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Master: " + (effectsEnabled ? "Enabled" : "Disabled") +
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

        buttons.add(ActionButton.create(
                Component.text(ambientEnabled ? "Disable Ambient Sound" : "Enable Ambient Sound",
                        ambientEnabled ? NamedTextColor.RED : NamedTextColor.GREEN),
                Component.text("Looping sound at teleporter locations"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            plugin.getConfig().set("effects.ambient.enabled", !ambientEnabled);
                            plugin.saveConfig();
                            plugin.reloadConfig();
                            plugin.getTeleportEffects().startAmbientLoop();
                            broadcastAdmin(p, (ambientEnabled ? "disabled" : "enabled") + " ambient sound");
                            p.showDialog(createAmbientDialog(plugin));
                        },
                        clickOptions()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text(ambientParticles ? "Disable Ambient Particles" : "Enable Ambient Particles",
                        ambientParticles ? NamedTextColor.YELLOW : NamedTextColor.GREEN),
                Component.text("Floating particles at teleporter locations"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            plugin.getConfig().set("effects.ambient.particles", !ambientParticles);
                            plugin.saveConfig();
                            plugin.reloadConfig();
                            plugin.getTeleportEffects().startAmbientLoop();
                            broadcastAdmin(p, (ambientParticles ? "disabled" : "enabled") + " ambient particles");
                            p.showDialog(createAmbientDialog(plugin));
                        },
                        clickOptions()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text("Set Volume (" + volume + ")"),
                Component.text("Current: " + volume + " (0.0 - 1.0)"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createAmbientVolumeInputDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text("Set Range (" + range + ")"),
                Component.text("Current: " + range + " blocks"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createAmbientRangeInputDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text("Set Interval (" + interval + ")"),
                Component.text("Current: " + interval + " ticks (" + (interval / 20.0) + "s)"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createAmbientIntervalInputDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        ActionButton backButton = ActionButton.create(
                Component.text("Back"),
                Component.text("Return to effects settings"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createEffectsDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Ambient Settings", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text(
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
                .base(DialogBase.builder(Component.text("Set Ambient Volume", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Enter a value between 0.0 and 1.0", NamedTextColor.WHITE))
                        ))
                        .inputs(List.of(
                                DialogInput.text(AMBIENT_VOLUME_INPUT_KEY,
                                        Component.text("Volume")).initial(String.valueOf(current)).maxLength(5).width(200).build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Save"),
                                Component.text("Save volume setting"),
                                150,
                                DialogAction.customClick(
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
                                                plugin.saveConfig();
                                                plugin.reloadConfig();
                                                plugin.getTeleportEffects().startAmbientLoop();
                                                broadcastAdmin(p, "set ambient volume to " + val);
                                                p.showDialog(createAmbientDialog(plugin));
                                            } catch (NumberFormatException e) {
                                                p.showDialog(createErrorDialog("Invalid volume. Enter a number between 0.0 and 1.0.", plugin, () -> createAmbientVolumeInputDialog(plugin)));
                                            }
                                        },
                                        clickOptions()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Back"),
                                Component.text("Return to ambient settings"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(createAmbientDialog(plugin));
                                            }
                                        },
                                        clickOptions()
                                )
                        )
                ))
        );
    }

    private static Dialog createAmbientRangeInputDialog(LodestoneTP plugin) {
        double current = plugin.getConfig().getDouble("effects.ambient.range", 8.0);
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Set Ambient Range", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Enter distance in blocks (1-64)", NamedTextColor.WHITE))
                        ))
                        .inputs(List.of(
                                DialogInput.text(AMBIENT_RANGE_INPUT_KEY,
                                        Component.text("Range")).initial(String.valueOf(current)).maxLength(5).width(200).build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Save"),
                                Component.text("Save range setting"),
                                150,
                                DialogAction.customClick(
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
                                                plugin.saveConfig();
                                                plugin.reloadConfig();
                                                plugin.getTeleportEffects().startAmbientLoop();
                                                broadcastAdmin(p, "set ambient range to " + val + " blocks");
                                                p.showDialog(createAmbientDialog(plugin));
                                            } catch (NumberFormatException e) {
                                                p.showDialog(createErrorDialog("Invalid range. Enter a number between 1 and 64.", plugin, () -> createAmbientRangeInputDialog(plugin)));
                                            }
                                        },
                                        clickOptions()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Back"),
                                Component.text("Return to ambient settings"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(createAmbientDialog(plugin));
                                            }
                                        },
                                        clickOptions()
                                )
                        )
                ))
        );
    }

    private static Dialog createAmbientIntervalInputDialog(LodestoneTP plugin) {
        int current = plugin.getConfig().getInt("effects.ambient.interval-ticks", 80);
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Set Ambient Interval", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Enter interval in ticks (20 = 1 second)", NamedTextColor.WHITE))
                        ))
                        .inputs(List.of(
                                DialogInput.text(AMBIENT_INTERVAL_INPUT_KEY,
                                        Component.text("Ticks")).initial(String.valueOf(current)).maxLength(5).width(200).build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Save"),
                                Component.text("Save interval setting"),
                                150,
                                DialogAction.customClick(
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
                                                plugin.saveConfig();
                                                plugin.reloadConfig();
                                                plugin.getTeleportEffects().startAmbientLoop();
                                                broadcastAdmin(p, "set ambient interval to " + val + " ticks (" + (val / 20.0) + "s)");
                                                p.showDialog(createAmbientDialog(plugin));
                                            } catch (NumberFormatException e) {
                                                p.showDialog(createErrorDialog("Invalid interval. Enter a number between 20 and 6000.", plugin, () -> createAmbientIntervalInputDialog(plugin)));
                                            }
                                        },
                                        clickOptions()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Back"),
                                Component.text("Return to ambient settings"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(createAmbientDialog(plugin));
                                            }
                                        },
                                        clickOptions()
                                )
                        )
                ))
        );
    }


    // ── Light Emission ──────────────────────────────────────────────

    public static Dialog createLightDialog(LodestoneTP plugin) {
        boolean lightEnabled = plugin.getConfig().getBoolean("effects.light.enabled", true);
        int level = plugin.getConfig().getInt("effects.light.level", 10);

        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(ActionButton.create(
                Component.text(lightEnabled ? "Disable Light" : "Enable Light",
                        lightEnabled ? NamedTextColor.RED : NamedTextColor.GREEN),
                Component.text("Toggle light emission above teleporters"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            plugin.getConfig().set("effects.light.enabled", !lightEnabled);
                            plugin.saveConfig();
                            plugin.reloadConfig();
                            if (!lightEnabled) {
                                plugin.getTeleportEffects().restoreAllLightBlocks();
                            } else {
                                plugin.getTeleportEffects().removeAllLightBlocks();
                            }
                            broadcastAdmin(p, (lightEnabled ? "disabled" : "enabled") + " teleporter light emission");
                            p.showDialog(createLightDialog(plugin));
                        },
                        clickOptions()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text("Set Light Level (" + level + ")"),
                Component.text("Current: " + level + "/15"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createLightLevelInputDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text("Back", NamedTextColor.GRAY),
                Component.text("Return to Effects Settings"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createEffectsDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        return Dialog.create(builder -> builder
                .empty()
                .base(DialogBase.builder(Component.text("💡 Light Settings", NamedTextColor.YELLOW))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text(
                                        "Status: " + (lightEnabled ? "Enabled" : "Disabled") +
                                        " | Level: " + level + "/15",
                                        lightEnabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons,
                        ActionButton.create(Component.text("Close", NamedTextColor.GRAY),
                                Component.text("Close dialog"), 100, DialogAction.customClick(
                                        (view, audience) -> { /* closes dialog */ },
                                        clickOptions())),
                        1))
        );
    }

    private static Dialog createLightLevelInputDialog(LodestoneTP plugin) {
        int current = plugin.getConfig().getInt("effects.light.level", 10);

        return Dialog.create(builder -> builder
                .empty()
                .base(DialogBase.builder(Component.text("Set Light Level", NamedTextColor.YELLOW))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text(
                                        "Current: " + current + ". Enter a value from 0 to 15.",
                                        NamedTextColor.GRAY))
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
                        ActionButton.create(
                                Component.text("Save", NamedTextColor.GREEN),
                                Component.text("Apply light level"),
                                100,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (!(audience instanceof Player p)) return;
                                            String input = view.getText(LIGHT_LEVEL_INPUT_KEY);
                                            try {
                                                int val = Integer.parseInt(input.trim());
                                                if (val < 0 || val > 15) {
                                                    p.sendMessage(Component.text("Light level must be 0-15.", NamedTextColor.RED));
                                                } else {
                                                    plugin.getConfig().set("effects.light.level", val);
                                                    plugin.saveConfig();
                                                    plugin.reloadConfig();
                                                    plugin.getTeleportEffects().removeAllLightBlocks();
                                                    plugin.getTeleportEffects().restoreAllLightBlocks();
                                                    broadcastAdmin(p, "set light level to " + val);
                                                }
                                            } catch (NumberFormatException e) {
                                                p.sendMessage(Component.text("Invalid number.", NamedTextColor.RED));
                                            }
                                            p.showDialog(createLightDialog(plugin));
                                        },
                                        clickOptions()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel", NamedTextColor.GRAY),
                                Component.text("Go back"),
                                100,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(createLightDialog(plugin));
                                            }
                                        },
                                        clickOptions()
                                )
                        )
                ))
        );
    }
    // ── Creation Fee ──────────────────────────────────────────────────

    public static Dialog createCreationFeeDialog(LodestoneTP plugin) {
        boolean feeEnabled = plugin.getConfig().getBoolean("creation-fee.enabled", true);
        String materialName = plugin.getConfig().getString("creation-fee.material", "ENDER_PEARL");
        int amount = plugin.getConfig().getInt("creation-fee.amount", 1);

        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(ActionButton.create(
                Component.text(feeEnabled ? "Disable Creation Fee" : "Enable Creation Fee",
                        feeEnabled ? NamedTextColor.RED : NamedTextColor.GREEN),
                Component.text("Toggle item cost to create teleporters"),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (!(audience instanceof Player p)) return;
                            plugin.getConfig().set("creation-fee.enabled", !feeEnabled);
                            plugin.saveConfig();
                            plugin.reloadConfig();
                            broadcastAdmin(p, (feeEnabled ? "disabled" : "enabled") + " creation fee");
                            p.showDialog(createCreationFeeDialog(plugin));
                        },
                        clickOptions()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text("Set Material (" + materialName + ")"),
                Component.text("Current: " + materialName),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createCreationFeeMaterialInputDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        buttons.add(ActionButton.create(
                Component.text("Set Amount (" + amount + ")"),
                Component.text("Current: " + amount),
                200,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createCreationFeeAmountInputDialog(plugin));
                            }
                        },
                        clickOptions()
                )
        ));

        ActionButton backButton = ActionButton.create(
                Component.text("Back"),
                Component.text("Return to admin panel"),
                150,
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                p.showDialog(createMainPanel(plugin));
                            }
                        },
                        clickOptions()
                )
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Creation Fee", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text(
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
                .base(DialogBase.builder(Component.text("Set Creation Fee Material", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Enter a valid Bukkit material name (e.g. ENDER_PEARL, DIAMOND)", NamedTextColor.WHITE))
                        ))
                        .inputs(List.of(
                                DialogInput.text(CREATION_FEE_MATERIAL_INPUT_KEY,
                                        Component.text("Material")).initial(current).maxLength(50).width(250).build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Save"),
                                Component.text("Save material setting"),
                                150,
                                DialogAction.customClick(
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
                                            plugin.saveConfig();
                                            plugin.reloadConfig();
                                            broadcastAdmin(p, "set creation fee material to " + val);
                                            p.showDialog(createCreationFeeDialog(plugin));
                                        },
                                        clickOptions()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Back"),
                                Component.text("Return to creation fee settings"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(createCreationFeeDialog(plugin));
                                            }
                                        },
                                        clickOptions()
                                )
                        )
                ))
        );
    }

    private static Dialog createCreationFeeAmountInputDialog(LodestoneTP plugin) {
        int current = plugin.getConfig().getInt("creation-fee.amount", 1);
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Set Creation Fee Amount", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("Enter amount (1-64)", NamedTextColor.WHITE))
                        ))
                        .inputs(List.of(
                                DialogInput.text(CREATION_FEE_AMOUNT_INPUT_KEY,
                                        Component.text("Amount")).initial(String.valueOf(current)).maxLength(3).width(200).build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Save"),
                                Component.text("Save amount setting"),
                                150,
                                DialogAction.customClick(
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
                                                plugin.saveConfig();
                                                plugin.reloadConfig();
                                                broadcastAdmin(p, "set creation fee amount to " + val);
                                                p.showDialog(createCreationFeeDialog(plugin));
                                            } catch (NumberFormatException e) {
                                                p.showDialog(createErrorDialog("Invalid amount. Enter a number between 1 and 64.", plugin, () -> createCreationFeeAmountInputDialog(plugin)));
                                            }
                                        },
                                        clickOptions()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Back"),
                                Component.text("Return to creation fee settings"),
                                150,
                                DialogAction.customClick(
                                        (view, audience) -> {
                                            if (audience instanceof Player p) {
                                                p.showDialog(createCreationFeeDialog(plugin));
                                            }
                                        },
                                        clickOptions()
                                )
                        )
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
