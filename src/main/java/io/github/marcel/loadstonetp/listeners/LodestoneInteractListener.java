package io.github.marcel.loadstonetp.listeners;

import io.github.marcel.loadstonetp.LodestoneTP;
import io.github.marcel.loadstonetp.db.DatabaseManager;
import io.github.marcel.loadstonetp.dialogs.TeleporterDialogs;
import io.github.marcel.loadstonetp.effects.StructureHologram;
import io.github.marcel.loadstonetp.model.Teleporter;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class LodestoneInteractListener implements Listener {

    private final DatabaseManager db;
    private final LodestoneTP plugin;
    private final StructureHologram structureHologram;

    public LodestoneInteractListener(DatabaseManager db, LodestoneTP plugin) {
        this.db = db;
        this.plugin = plugin;
        this.structureHologram = new StructureHologram(plugin);
    }

    @EventHandler
    public void onLodestoneLeftClick(PlayerInteractEvent event) {
        if (!event.getAction().isLeftClick()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.LODESTONE) return;
        if (!event.getPlayer().isSneaking()) return; // Only on Shift+Click

        event.setCancelled(true);

        Player player = event.getPlayer();

        // If hologram already active, dismiss it (second Shift+Click)
        if (structureHologram.hasActive(player)) {
            structureHologram.removeForPlayer(player);
            player.sendMessage(Component.text("Hologram hidden.", NamedTextColor.GRAY));
            return;
        }

        // Show structure preview on Shift+Click
        structureHologram.showTeleporterStructure(player, event.getClickedBlock().getLocation());

        player.sendMessage(Component.text("Hologram shown. Shift+Click again to hide.", NamedTextColor.AQUA));
    }

    @EventHandler
    public void onLodestoneRightClick(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.LODESTONE) return;

        Player player = event.getPlayer();
        
        // Check if player is trying to place a building block (allow block placement)
        ItemStack buildingBlock = player.getInventory().getItemInMainHand();
        if (buildingBlock.getType() == Material.LODESTONE || buildingBlock.getType() == Material.POLISHED_BLACKSTONE_BRICKS) {
            // Player is building - allow normal block placement
            return;
        }

        // This is a teleporter interaction - cancel the right-click
        event.setCancelled(true);

        // Remove hologram when right-clicking (interacting with the structure)
        if (structureHologram.hasActive(player)) {
            structureHologram.removeForPlayer(player);
        }

        if (!player.hasPermission("lodestonetp.use")) {
            player.sendMessage(Component.text("You don't have permission to use teleporters!", NamedTextColor.RED));
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        boolean validStructure = isValidMultiblock(clickedBlock);

        Teleporter existing = db.getTeleporterAt(clickedBlock.getLocation());

        if (existing != null) {
            if (!validStructure) {
                destroyTeleporter(existing, player);
                return;
            }

            String playerUuid = player.getUniqueId().toString();
            boolean isOwner = playerUuid.equals(existing.ownerUuid());
            boolean isAdmin = player.isOp() || player.hasPermission("lodestonetp.admin");
            boolean isPublic = existing.isPublic();
            boolean hasAccess = db.hasAccess(existing.id(), playerUuid);

            boolean hasNetworkPermission = true;
            if (!isAdmin && !(player.isOp() || player.hasPermission("lodestonetp.network.bypass")) && existing.networkId() != null) {
                io.github.marcel.loadstonetp.model.Network network = db.getNetwork(existing.networkId());
                if (network != null && network.permissionNode() != null && !network.permissionNode().isBlank()) {
                    hasNetworkPermission = player.hasPermission(network.permissionNode());
                }
            }

            if ((isOwner || isAdmin || isPublic || hasAccess) && hasNetworkPermission) {
                if (TeleporterDialogs.tryAutoTeleportLinked(db, existing, player, plugin)) {
                    return;
                }
                Dialog dialog = TeleporterDialogs.createTeleportDialog(db, existing, player, plugin);
                player.showDialog(dialog);
            } else {
                Dialog dialog = TeleporterDialogs.createPrivateNoticeDialog(existing);
                player.showDialog(dialog);
            }
            return;
        }

        if (!validStructure) {
            player.sendMessage(
                    Component.text("Invalid teleporter setup! ", NamedTextColor.RED)
                            .append(Component.text("Place polished blackstone bricks above and below the lodestone.", NamedTextColor.GRAY))
            );
            return;
        }

        if (!player.hasPermission("lodestonetp.create")) {
            player.sendMessage(Component.text("You don't have permission to create teleporters!", NamedTextColor.RED));
            return;
        }

        if (plugin.getConfig().getBoolean("creation-fee.enabled", true)) {
            String materialName = plugin.getConfig().getString("creation-fee.material", "ENDER_PEARL");
            Material requiredMaterial = Material.matchMaterial(materialName);
            if (requiredMaterial == null) requiredMaterial = Material.ENDER_PEARL;
            int requiredAmount = plugin.getConfig().getInt("creation-fee.amount", 1);

            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand.getType() != requiredMaterial || itemInHand.getAmount() < requiredAmount) {
                String displayName = requiredMaterial.name().replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
                player.sendMessage(
                        Component.text("Right-click with ", NamedTextColor.GOLD)
                                .append(Component.text(requiredAmount + "x ", NamedTextColor.WHITE))
                                .append(Component.text(displayName, NamedTextColor.LIGHT_PURPLE))
                                .append(Component.text(" to activate this teleporter!", NamedTextColor.GOLD))
                );
                return;
            }

            itemInHand.setAmount(itemInHand.getAmount() - requiredAmount);
        }

        float playerYaw = player.getLocation().getYaw();
        boolean defaultPublic = plugin.getConfig().getBoolean("defaults.public", true);
        String ownerUuid = player.getUniqueId().toString();
        Dialog dialog = TeleporterDialogs.createNewTeleporterDialog(db, clickedBlock.getLocation(), playerYaw, ownerUuid, defaultPublic, plugin);
        player.showDialog(dialog);
    }

    private boolean isValidMultiblock(Block lodestone) {
        Block above = lodestone.getRelative(0, 1, 0);
        Block below = lodestone.getRelative(0, -1, 0);
        return above.getType() == Material.POLISHED_BLACKSTONE_BRICKS
                && below.getType() == Material.POLISHED_BLACKSTONE_BRICKS;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        Material type = broken.getType();
        Player player = event.getPlayer();

        // Check if broken block is a lodestone (center of teleporter)
        if (type == Material.LODESTONE) {
            Teleporter teleporter = db.getTeleporterAt(broken.getLocation());
            if (teleporter != null) {
                destroyTeleporter(teleporter, player);
            } else {
                // Lodestone destroyed - remove hologram since center is gone
                if (structureHologram.hasActive(player)) {
                    structureHologram.removeForPlayer(player);
                }
            }
            return;
        }

        // Check if broken block is a polished blackstone brick (part of structure)
        if (type == Material.POLISHED_BLACKSTONE_BRICKS) {
            // Check if this brick is part of a teleporter (above or below a lodestone)
            Block lodestoneAbove = broken.getRelative(0, -1, 0);
            Block lodestoneBelow = broken.getRelative(0, 1, 0);

            // Check lodestone above this brick
            if (lodestoneAbove.getType() == Material.LODESTONE) {
                Teleporter teleporter = db.getTeleporterAt(lodestoneAbove.getLocation());
                if (teleporter != null) {
                    destroyTeleporter(teleporter, player);
                    return;
                } else {
                    // Update hologram if player has one active
                    if (structureHologram.hasActive(player)) {
                        structureHologram.refreshStructure(player, lodestoneAbove.getLocation());
                    }
                    return;
                }
            }

            // Check lodestone below this brick
            if (lodestoneBelow.getType() == Material.LODESTONE) {
                Teleporter teleporter = db.getTeleporterAt(lodestoneBelow.getLocation());
                if (teleporter != null) {
                    destroyTeleporter(teleporter, player);
                } else {
                    // Update hologram if player has one active
                    if (structureHologram.hasActive(player)) {
                        structureHologram.refreshStructure(player, lodestoneBelow.getLocation());
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlock();
        Player player = event.getPlayer();

        // Only care about polished blackstone bricks or lodestones
        if (placed.getType() != Material.POLISHED_BLACKSTONE_BRICKS && placed.getType() != Material.LODESTONE) {
            return;
        }

        // Check if this block is placed next to a lodestone
        Block lodestoneAbove = placed.getRelative(0, -1, 0);
        Block lodestoneBelow = placed.getRelative(0, 1, 0);
        Block lodestoneCenter = placed;

        // If block is lodestone, check neighbors
        if (placed.getType() == Material.LODESTONE) {
            lodestoneCenter = placed;
        } else if (lodestoneAbove.getType() == Material.LODESTONE) {
            // Brick above lodestone
            lodestoneCenter = lodestoneAbove;
        } else if (lodestoneBelow.getType() == Material.LODESTONE) {
            // Brick below lodestone
            lodestoneCenter = lodestoneBelow;
        } else {
            return; // Not part of a structure
        }

        // Update hologram if player has one active
        if (structureHologram.hasActive(player)) {
            structureHologram.refreshStructure(player, lodestoneCenter.getLocation());
        }
    }

    /**
     * Destroys a teleporter, removes it from the database, and cleans up effects.
     */
    private void destroyTeleporter(Teleporter teleporter, Player player) {
        db.removeTeleporter(teleporter.id());
        plugin.getTeleportEffects().removeLightBlock(teleporter);
        plugin.getTeleportEffects().refreshTeleporterLocations();
        player.sendMessage(
                Component.text("Teleporter ", NamedTextColor.RED)
                        .append(Component.text("\"" + teleporter.name() + "\"", NamedTextColor.GOLD))
                        .append(Component.text(" has been destroyed!", NamedTextColor.RED))
        );
    }

    /**
     * Returns the StructureHologram instance for managing hologram displays.
     */
    public StructureHologram getStructureHologram() {
        return structureHologram;
    }
}
