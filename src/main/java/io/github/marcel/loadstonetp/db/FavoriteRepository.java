package io.github.marcel.loadstonetp.db;

import io.github.marcel.loadstonetp.model.Teleporter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class FavoriteRepository {

    private final DatabaseSupport support;

    FavoriteRepository(DatabaseSupport support) {
        this.support = support;
    }

    boolean addFavorite(String playerUuid, int teleporterId) {
        String sql = "INSERT OR IGNORE INTO favorites (player_uuid, teleporter_id) VALUES (?, ?)";
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, teleporterId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            support.logWarning("Failed to add favorite", e);
            return false;
        }
    }

    boolean removeFavorite(String playerUuid, int teleporterId) {
        String sql = "DELETE FROM favorites WHERE player_uuid = ? AND teleporter_id = ?";
        return support.executeUpdate(sql, "Failed to remove favorite", stmt -> {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, teleporterId);
        });
    }

    boolean isFavorite(String playerUuid, int teleporterId) {
        String sql = "SELECT 1 FROM favorites WHERE player_uuid = ? AND teleporter_id = ?";
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, teleporterId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            support.logWarning("Failed to check favorite", e);
            return false;
        }
    }

    List<Teleporter> getFavoriteTeleporters(String playerUuid) {
        List<Teleporter> teleporters = new ArrayList<>();
        String sql = """
                SELECT DISTINCT t.id, t.name, t.world, t.x, t.y, t.z, t.yaw, t.owner_uuid, t.is_public, t.cooldown_override, t.network_id, t.linked_teleporter_id
                FROM teleporters t
                INNER JOIN favorites f ON t.id = f.teleporter_id
                WHERE f.player_uuid = ?
                """;
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    teleporters.add(support.mapTeleporter(rs));
                }
            }
        } catch (SQLException e) {
            support.logWarning("Failed to get favorite teleporters", e);
        }
        return teleporters;
    }

    Set<Integer> getFavoriteIds(String playerUuid) {
        Set<Integer> favoriteIds = new HashSet<>();
        String sql = "SELECT teleporter_id FROM favorites WHERE player_uuid = ?";
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    favoriteIds.add(rs.getInt("teleporter_id"));
                }
            }
        } catch (SQLException e) {
            support.logWarning("Failed to read favorite IDs", e);
        }
        return favoriteIds;
    }
}
