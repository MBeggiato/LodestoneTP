package io.github.marcel.loadstonetp.db;

import io.github.marcel.loadstonetp.model.Network;
import io.github.marcel.loadstonetp.model.Teleporter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

final class DatabaseSupport {

    @FunctionalInterface
    interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }

    private final Connection connection;
    private final Logger logger;

    DatabaseSupport(Connection connection, Logger logger) {
        this.connection = connection;
        this.logger = logger;
    }

    Connection connection() {
        return connection;
    }

    Logger logger() {
        return logger;
    }

    Integer getNullableInteger(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column) != null ? rs.getInt(column) : null;
    }

    Teleporter mapTeleporter(ResultSet rs) throws SQLException {
        return new Teleporter(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("world"),
                rs.getInt("x"),
                rs.getInt("y"),
                rs.getInt("z"),
                rs.getFloat("yaw"),
                rs.getString("owner_uuid"),
                rs.getBoolean("is_public"),
                getNullableInteger(rs, "cooldown_override"),
                getNullableInteger(rs, "network_id"),
                getNullableInteger(rs, "linked_teleporter_id")
        );
    }

    Network mapNetwork(ResultSet rs) throws SQLException {
        return new Network(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("owner_uuid"),
                rs.getString("world_filter"),
                rs.getString("permission_node")
        );
    }

    void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
            return;
        }
        stmt.setInt(index, value);
    }

    void setNullableString(PreparedStatement stmt, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            stmt.setNull(index, Types.VARCHAR);
            return;
        }
        stmt.setString(index, value);
    }

    void logWarning(String message, SQLException e) {
        logger.log(Level.WARNING, message, e);
    }

    boolean executeUpdate(String sql, String errorMessage, StatementBinder binder) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            binder.bind(stmt);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logWarning(errorMessage, e);
            return false;
        }
    }
}
