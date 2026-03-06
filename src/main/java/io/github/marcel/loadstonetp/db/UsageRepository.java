package io.github.marcel.loadstonetp.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

final class UsageRepository {

    private final DatabaseSupport support;

    UsageRepository(DatabaseSupport support) {
        this.support = support;
    }

    boolean recordTeleporterUse(String playerUuid, int teleporterId) {
        String sql = """
                INSERT INTO teleporter_usage (player_uuid, teleporter_id, use_count, last_used_at)
                VALUES (?, ?, 1, ?)
                ON CONFLICT(player_uuid, teleporter_id)
                DO UPDATE SET use_count = use_count + 1, last_used_at = excluded.last_used_at
                """;
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, teleporterId);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            support.logWarning("Failed to record teleporter usage", e);
            return false;
        }
    }

    Map<Integer, Integer> getUsageCounts(String playerUuid) {
        Map<Integer, Integer> usageCounts = new HashMap<>();
        String sql = "SELECT teleporter_id, use_count FROM teleporter_usage WHERE player_uuid = ?";
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    usageCounts.put(rs.getInt("teleporter_id"), rs.getInt("use_count"));
                }
            }
        } catch (SQLException e) {
            support.logWarning("Failed to read usage counts", e);
        }
        return usageCounts;
    }
}
