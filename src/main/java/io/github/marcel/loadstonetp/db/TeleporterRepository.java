package io.github.marcel.loadstonetp.db;

import io.github.marcel.loadstonetp.model.Teleporter;
import org.bukkit.Location;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

final class TeleporterRepository {

    private final DatabaseSupport support;

    TeleporterRepository(DatabaseSupport support) {
        this.support = support;
    }

    boolean addTeleporter(String name, Location location, float yaw, String ownerUuid, boolean isPublic) {
        String sql = "INSERT INTO teleporters (name, world, x, y, z, yaw, owner_uuid, is_public, cooldown_override, network_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL)";
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, location.getWorld().getName());
            stmt.setInt(3, location.getBlockX());
            stmt.setInt(4, location.getBlockY());
            stmt.setInt(5, location.getBlockZ());
            stmt.setFloat(6, yaw);
            stmt.setString(7, ownerUuid);
            stmt.setInt(8, isPublic ? 1 : 0);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            support.logWarning("Failed to add teleporter", e);
            return false;
        }
    }

    Teleporter getTeleporterAt(Location location) {
        String sql = "SELECT id, name, world, x, y, z, yaw, owner_uuid, is_public, cooldown_override, network_id, linked_teleporter_id FROM teleporters WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setString(1, location.getWorld().getName());
            stmt.setInt(2, location.getBlockX());
            stmt.setInt(3, location.getBlockY());
            stmt.setInt(4, location.getBlockZ());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return support.mapTeleporter(rs);
                }
            }
        } catch (SQLException e) {
            support.logWarning("Failed to query teleporter", e);
        }
        return null;
    }

    boolean removeTeleporter(int id) {
        String sql = "DELETE FROM teleporters WHERE id = ?";
        return support.executeUpdate(sql, "Failed to remove teleporter", stmt -> stmt.setInt(1, id));
    }

    List<Teleporter> getAllTeleporters() {
        List<Teleporter> teleporters = new ArrayList<>();
        String sql = "SELECT id, name, world, x, y, z, yaw, owner_uuid, is_public, cooldown_override, network_id, linked_teleporter_id FROM teleporters";
        try (Statement stmt = support.connection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                teleporters.add(support.mapTeleporter(rs));
            }
        } catch (SQLException e) {
            support.logWarning("Failed to list teleporters", e);
        }
        return teleporters;
    }

    int getTeleporterCountByOwner(String ownerUuid) {
        String sql = "SELECT COUNT(*) FROM teleporters WHERE owner_uuid = ?";
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setString(1, ownerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            support.logWarning("Failed to count teleporters by owner", e);
        }
        return 0;
    }

    boolean setPublic(int teleporterId, boolean isPublic) {
        String sql = "UPDATE teleporters SET is_public = ? WHERE id = ?";
        return support.executeUpdate(sql, "Failed to update visibility", stmt -> {
            stmt.setInt(1, isPublic ? 1 : 0);
            stmt.setInt(2, teleporterId);
        });
    }

    boolean setCooldownOverride(int teleporterId, Integer cooldownSeconds) {
        String sql = "UPDATE teleporters SET cooldown_override = ? WHERE id = ?";
        return support.executeUpdate(sql, "Failed to update cooldown override", stmt -> {
            support.setNullableInteger(stmt, 1, cooldownSeconds);
            stmt.setInt(2, teleporterId);
        });
    }

    boolean isNameTaken(String name) {
        String sql = "SELECT 1 FROM teleporters WHERE name = ?";
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            support.logWarning("Failed to check name", e);
            return false;
        }
    }

    boolean setTeleporterNetwork(int teleporterId, Integer networkId) {
        String sql = "UPDATE teleporters SET network_id = ? WHERE id = ?";
        return support.executeUpdate(sql, "Failed to update teleporter network", stmt -> {
            support.setNullableInteger(stmt, 1, networkId);
            stmt.setInt(2, teleporterId);
        });
    }

    boolean setLinkedTeleporter(int teleporterId, Integer linkedTeleporterId) {
        String sql = "UPDATE teleporters SET linked_teleporter_id = ? WHERE id = ?";
        return support.executeUpdate(sql, "Failed to update linked teleporter", stmt -> {
            support.setNullableInteger(stmt, 1, linkedTeleporterId);
            stmt.setInt(2, teleporterId);
        });
    }

    Teleporter getTeleporter(int teleporterId) {
        String sql = "SELECT id, name, world, x, y, z, yaw, owner_uuid, is_public, cooldown_override, network_id, linked_teleporter_id FROM teleporters WHERE id = ?";
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setInt(1, teleporterId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return support.mapTeleporter(rs);
                }
            }
        } catch (SQLException e) {
            support.logWarning("Failed to get teleporter by id", e);
        }
        return null;
    }
}
