package io.github.marcel.loadstonetp.db;

import io.github.marcel.loadstonetp.model.Network;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

final class NetworkRepository {

    private final DatabaseSupport support;

    NetworkRepository(DatabaseSupport support) {
        this.support = support;
    }

    boolean addNetwork(String name, String ownerUuid, String worldFilter) {
        String sql = "INSERT INTO networks (name, owner_uuid, world_filter, permission_node) VALUES (?, ?, ?, NULL)";
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, ownerUuid);
            support.setNullableString(stmt, 3, worldFilter);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            support.logWarning("Failed to add network", e);
            return false;
        }
    }

    boolean deleteNetwork(int networkId) {
        boolean originalAutoCommit;
        try {
            originalAutoCommit = support.connection().getAutoCommit();
        } catch (SQLException e) {
            support.logWarning("Failed to get auto-commit state before deleting network", e);
            return false;
        }

        try {
            support.connection().setAutoCommit(false);

            String clearTeleportersSql = "UPDATE teleporters SET network_id = NULL WHERE network_id = ?";
            try (PreparedStatement clearStmt = support.connection().prepareStatement(clearTeleportersSql)) {
                clearStmt.setInt(1, networkId);
                clearStmt.executeUpdate();
            }

            String deleteNetworkSql = "DELETE FROM networks WHERE id = ?";
            int affectedRows;
            try (PreparedStatement deleteStmt = support.connection().prepareStatement(deleteNetworkSql)) {
                deleteStmt.setInt(1, networkId);
                affectedRows = deleteStmt.executeUpdate();
            }

            support.connection().commit();
            return affectedRows > 0;
        } catch (SQLException e) {
            try {
                support.connection().rollback();
            } catch (SQLException rollbackEx) {
                support.logWarning("Failed to rollback transaction when deleting network", rollbackEx);
            }
            support.logWarning("Failed to delete network", e);
            return false;
        } finally {
            try {
                support.connection().setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                support.logWarning("Failed to restore auto-commit state after deleting network", e);
            }
        }
    }

    List<Network> getNetworksByOwner(String ownerUuid) {
        List<Network> networks = new ArrayList<>();
        String sql = "SELECT id, name, owner_uuid, world_filter, permission_node FROM networks WHERE owner_uuid = ?";
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setString(1, ownerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    networks.add(support.mapNetwork(rs));
                }
            }
        } catch (SQLException e) {
            support.logWarning("Failed to get networks by owner", e);
        }
        return networks;
    }

    Network getNetwork(int networkId) {
        String sql = "SELECT id, name, owner_uuid, world_filter, permission_node FROM networks WHERE id = ?";
        try (PreparedStatement stmt = support.connection().prepareStatement(sql)) {
            stmt.setInt(1, networkId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return support.mapNetwork(rs);
                }
            }
        } catch (SQLException e) {
            support.logWarning("Failed to get network", e);
        }
        return null;
    }

    boolean renameNetwork(int networkId, String newName) {
        String sql = "UPDATE networks SET name = ? WHERE id = ?";
        return support.executeUpdate(sql, "Failed to rename network", stmt -> {
            stmt.setString(1, newName);
            stmt.setInt(2, networkId);
        });
    }

    boolean setNetworkPermissionNode(int networkId, String permissionNode) {
        String sql = "UPDATE networks SET permission_node = ? WHERE id = ?";
        return support.executeUpdate(sql, "Failed to set network permission node", stmt -> {
            support.setNullableString(stmt, 1, permissionNode == null ? null : permissionNode.trim());
            stmt.setInt(2, networkId);
        });
    }
}
