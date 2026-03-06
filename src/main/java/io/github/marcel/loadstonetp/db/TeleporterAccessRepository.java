package io.github.marcel.loadstonetp.db;

import io.github.marcel.loadstonetp.model.Teleporter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

final class TeleporterAccessRepository {

    private final DatabaseSupport support;

    TeleporterAccessRepository(DatabaseSupport support) {
        this.support = support;
    }

    boolean addAccess(int teleporterId, String playerUuid) {
        String sql = "INSERT OR IGNORE INTO teleporter_access (teleporter_id, player_uuid) VALUES (?, ?)";
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setInt(1, teleporterId);
            stmt.setString(2, playerUuid);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            support.logWarning("Failed to add access", e);
            return false;
        }
    }

    boolean removeAccess(int teleporterId, String playerUuid) {
        String sql = "DELETE FROM teleporter_access WHERE teleporter_id = ? AND player_uuid = ?";
        return support.executeUpdate(sql, "Failed to remove access", stmt -> {
            stmt.setInt(1, teleporterId);
            stmt.setString(2, playerUuid);
        });
    }

    List<String> getAccessList(int teleporterId) {
        List<String> uuids = new ArrayList<>();
        String sql = "SELECT player_uuid FROM teleporter_access WHERE teleporter_id = ?";
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setInt(1, teleporterId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    uuids.add(rs.getString("player_uuid"));
                }
            }
        } catch (SQLException e) {
            support.logWarning("Failed to get access list", e);
        }
        return uuids;
    }

    boolean hasAccess(int teleporterId, String playerUuid) {
        String sql = "SELECT 1 FROM teleporter_access WHERE teleporter_id = ? AND player_uuid = ?";
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setInt(1, teleporterId);
            stmt.setString(2, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            support.logWarning("Failed to check access", e);
            return false;
        }
    }

    List<Teleporter> getAccessibleTeleporters(String playerUuid) {
        List<Teleporter> teleporters = new ArrayList<>();
        String sql = """
                SELECT DISTINCT t.id, t.name, t.world, t.x, t.y, t.z, t.yaw, t.owner_uuid, t.is_public, t.cooldown_override, t.network_id, t.linked_teleporter_id
                FROM teleporters t
                LEFT JOIN teleporter_access a ON t.id = a.teleporter_id
                WHERE t.is_public = 1 OR t.owner_uuid = ? OR a.player_uuid = ?
                """;
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    teleporters.add(support.mapTeleporter(rs));
                }
            }
        } catch (SQLException e) {
            support.logWarning("Failed to get accessible teleporters", e);
        }
        return teleporters;
    }
}
