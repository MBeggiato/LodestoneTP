package io.github.marcel.loadstonetp.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

final class DatabaseMigrator {

    private static final int CURRENT_VERSION = 9;

    private final Connection connection;
    private final Logger logger;

    DatabaseMigrator(Connection connection, Logger logger) {
        this.connection = connection;
        this.logger = logger;
    }

    void runMigrations() throws SQLException {
        ensureSchemaVersionTable();
        int currentVersion = getSchemaVersion();
        if (currentVersion < CURRENT_VERSION) {
            logger.info("Database at version " + currentVersion + ", migrating to version " + CURRENT_VERSION + "...");
            if (currentVersion < 1) migrateToV1();
            if (currentVersion < 2) migrateToV2();
            if (currentVersion < 3) migrateToV3();
            if (currentVersion < 4) migrateToV4();
            if (currentVersion < 5) migrateToV5();
            if (currentVersion < 6) migrateToV6();
            if (currentVersion < 7) migrateToV7();
            if (currentVersion < 8) migrateToV8();
            if (currentVersion < 9) migrateToV9();
            setSchemaVersion(CURRENT_VERSION);
            logger.info("Database migration complete (now at version " + CURRENT_VERSION + ").");
        }
    }

    private void ensureSchemaVersionTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS schema_version (
                        version INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM schema_version")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO schema_version (version) VALUES (" + detectExistingVersion() + ")");
                }
            }
        }
    }

    private int detectExistingVersion() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='teleporters'")) {
                if (!rs.next()) {
                    return 0;
                }
            }

            try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(teleporters)")) {
                boolean hasYaw = false;
                boolean hasOwnerUuid = false;
                boolean hasCooldownOverride = false;
                boolean hasNetworkId = false;
                boolean hasLinkedTeleporterId = false;
                while (rs.next()) {
                    String colName = rs.getString("name");
                    if ("yaw".equals(colName)) hasYaw = true;
                    if ("owner_uuid".equals(colName)) hasOwnerUuid = true;
                    if ("cooldown_override".equals(colName)) hasCooldownOverride = true;
                    if ("network_id".equals(colName)) hasNetworkId = true;
                    if ("linked_teleporter_id".equals(colName)) hasLinkedTeleporterId = true;
                }
                if (hasLinkedTeleporterId) return 7;
                if (hasNetworkId) return 5;
                if (hasCooldownOverride) return 4;
                if (hasOwnerUuid) return 3;
                if (hasYaw) return 2;
                return 1;
            }
        }
    }

    private int getSchemaVersion() throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version FROM schema_version LIMIT 1")) {
            return rs.next() ? rs.getInt("version") : 0;
        }
    }

    private void setSchemaVersion(int version) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE schema_version SET version = ?")) {
            stmt.setInt(1, version);
            stmt.executeUpdate();
        }
    }

    private void migrateToV1() throws SQLException {
        logger.info("Running migration V1: Create teleporters table...");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS teleporters (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE,
                        world TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        UNIQUE(world, x, y, z)
                    )
                    """);
        }
    }

    private void migrateToV2() throws SQLException {
        logger.info("Running migration V2: Add yaw column...");
        try (Statement stmt = connection.createStatement()) {
            executeIgnoringDuplicateColumn(stmt, "ALTER TABLE teleporters ADD COLUMN yaw REAL NOT NULL DEFAULT 0");
        }
    }

    private void migrateToV3() throws SQLException {
        logger.info("Running migration V3: Add ownership, visibility, and access control...");
        try (Statement stmt = connection.createStatement()) {
            executeIgnoringDuplicateColumn(stmt, "ALTER TABLE teleporters ADD COLUMN owner_uuid TEXT");
            executeIgnoringDuplicateColumn(stmt, "ALTER TABLE teleporters ADD COLUMN is_public INTEGER NOT NULL DEFAULT 1");
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS teleporter_access (
                        teleporter_id INTEGER NOT NULL,
                        player_uuid TEXT NOT NULL,
                        PRIMARY KEY (teleporter_id, player_uuid),
                        FOREIGN KEY (teleporter_id) REFERENCES teleporters(id) ON DELETE CASCADE
                    )
                    """);
        }
    }

    private void migrateToV4() throws SQLException {
        logger.info("Running migration V4: Add per-teleporter cooldown override...");
        try (Statement stmt = connection.createStatement()) {
            executeIgnoringDuplicateColumn(stmt, "ALTER TABLE teleporters ADD COLUMN cooldown_override INTEGER");
        }
    }

    private void migrateToV5() throws SQLException {
        logger.info("Running migration V5: Add networks table and network_id...");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS networks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE,
                        owner_uuid TEXT NOT NULL,
                        world_filter TEXT
                    )
                    """);
            executeIgnoringDuplicateColumn(stmt, "ALTER TABLE teleporters ADD COLUMN network_id INTEGER");
        }
    }

    private void migrateToV6() throws SQLException {
        logger.info("Running migration V6: Add favorites table...");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS favorites (
                        player_uuid TEXT NOT NULL,
                        teleporter_id INTEGER NOT NULL,
                        PRIMARY KEY (player_uuid, teleporter_id),
                        FOREIGN KEY (teleporter_id) REFERENCES teleporters(id) ON DELETE CASCADE
                    )
                    """);
        }
    }

    private void migrateToV7() throws SQLException {
        logger.info("Running migration V7: Add linked_teleporter_id...");
        try (Statement stmt = connection.createStatement()) {
            executeIgnoringDuplicateColumn(stmt, "ALTER TABLE teleporters ADD COLUMN linked_teleporter_id INTEGER");
        }
    }

    private void migrateToV8() throws SQLException {
        logger.info("Running migration V8: Add teleporter usage stats table...");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS teleporter_usage (
                        player_uuid TEXT NOT NULL,
                        teleporter_id INTEGER NOT NULL,
                        use_count INTEGER NOT NULL DEFAULT 0,
                        last_used_at INTEGER,
                        PRIMARY KEY (player_uuid, teleporter_id),
                        FOREIGN KEY (teleporter_id) REFERENCES teleporters(id) ON DELETE CASCADE
                    )
                    """);
        }
    }

    private void migrateToV9() throws SQLException {
        logger.info("Running migration V9: Add network permission node...");
        try (Statement stmt = connection.createStatement()) {
            executeIgnoringDuplicateColumn(stmt, "ALTER TABLE networks ADD COLUMN permission_node TEXT");
        }
    }

    private void executeIgnoringDuplicateColumn(Statement stmt, String sql) throws SQLException {
        try {
            stmt.execute(sql);
        } catch (SQLException e) {
            if (!isDuplicateColumnError(e)) {
                throw e;
            }
        }
    }

    private boolean isDuplicateColumnError(SQLException e) {
        return e.getMessage() != null && e.getMessage().contains("duplicate column");
    }
}
