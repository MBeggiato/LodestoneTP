package io.github.marcel.loadstonetp.db;

import io.github.marcel.loadstonetp.model.Teleporter;
import org.bukkit.Location;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {

    private static final int CURRENT_VERSION = 9;

    private final Logger logger;
    private Connection connection;

    public DatabaseManager(File dataFolder, Logger logger) {
        this.logger = logger;
        try {
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            String url = "jdbc:sqlite:" + new File(dataFolder, "teleporters.db").getAbsolutePath();
            connection = DriverManager.getConnection(url);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
            runMigrations();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize database", e);
        }
    }

    // ── Migration system ──────────────────────────────────────────────

    private void runMigrations() throws SQLException {
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
            // Seed with version 0 if empty
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM schema_version")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    // Detect if this is an existing database (teleporters table exists) or brand new
                    int detectedVersion = detectExistingVersion();
                    stmt.execute("INSERT INTO schema_version (version) VALUES (" + detectedVersion + ")");
                }
            }
        }
    }

    /**
     * Detects the schema version of an existing database that was created before
     * the migration system was introduced. Returns 0 for brand new databases.
     */
    private int detectExistingVersion() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Check if teleporters table exists at all
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='teleporters'")) {
                if (!rs.next()) return 0; // Brand new database
            }
            // Table exists — check which columns are present
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
                if (hasNetworkId) return 5;     // Has all columns including network
                if (hasCooldownOverride) return 4; // Has cooldown_override
                if (hasOwnerUuid) return 3;        // Has owner/access
                if (hasYaw) return 2;               // Has yaw but not owner/access
                return 1;                           // Has base table only
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

    /** V1: Initial teleporters table */
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

    /** V2: Add yaw column for directional spawning */
    private void migrateToV2() throws SQLException {
        logger.info("Running migration V2: Add yaw column...");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE teleporters ADD COLUMN yaw REAL NOT NULL DEFAULT 0");
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column")) throw e;
        }
    }

    /** V3: Add ownership, visibility, and access control */
    private void migrateToV3() throws SQLException {
        logger.info("Running migration V3: Add ownership, visibility, and access control...");
        try (Statement stmt = connection.createStatement()) {
            try {
                stmt.execute("ALTER TABLE teleporters ADD COLUMN owner_uuid TEXT");
            } catch (SQLException e) {
                if (!e.getMessage().contains("duplicate column")) throw e;
            }
            try {
                stmt.execute("ALTER TABLE teleporters ADD COLUMN is_public INTEGER NOT NULL DEFAULT 1");
            } catch (SQLException e) {
                if (!e.getMessage().contains("duplicate column")) throw e;
            }
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

    /** V4: Add per-teleporter cooldown override */
    private void migrateToV4() throws SQLException {
        logger.info("Running migration V4: Add per-teleporter cooldown override...");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE teleporters ADD COLUMN cooldown_override INTEGER");
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column")) throw e;
        }
    }

    /** V5: Add networks table and network_id to teleporters */
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
            try {
                stmt.execute("ALTER TABLE teleporters ADD COLUMN network_id INTEGER");
            } catch (SQLException e) {
                if (!e.getMessage().contains("duplicate column")) throw e;
            }
        }
    }

    /** V6: Add favorites table for per-player favorites */
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

    /** V7: Add linked_teleporter_id for A↔B teleporter linking */
    private void migrateToV7() throws SQLException {
        logger.info("Running migration V7: Add linked_teleporter_id...");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE teleporters ADD COLUMN linked_teleporter_id INTEGER");
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column")) throw e;
        }
    }

    /** V8: Add per-player usage stats for sorting by most used */
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

    /** V9: Add optional per-network permission nodes */
    private void migrateToV9() throws SQLException {
        logger.info("Running migration V9: Add network permission node...");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE networks ADD COLUMN permission_node TEXT");
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column")) throw e;
        }
    }

    public boolean addTeleporter(String name, Location location, float yaw, String ownerUuid, boolean isPublic) {
        String sql = "INSERT INTO teleporters (name, world, x, y, z, yaw, owner_uuid, is_public, cooldown_override, network_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
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
            logger.log(Level.WARNING, "Failed to add teleporter: " + e.getMessage());
            return false;
        }
    }

    public Teleporter getTeleporterAt(Location location) {
        String sql = "SELECT id, name, world, x, y, z, yaw, owner_uuid, is_public, cooldown_override, network_id, linked_teleporter_id FROM teleporters WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, location.getWorld().getName());
            stmt.setInt(2, location.getBlockX());
            stmt.setInt(3, location.getBlockY());
            stmt.setInt(4, location.getBlockZ());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Integer cooldownOverride = rs.getObject("cooldown_override") != null ? rs.getInt("cooldown_override") : null;
                    Integer networkId = rs.getObject("network_id") != null ? rs.getInt("network_id") : null;
                    Integer linkedTeleporterId = rs.getObject("linked_teleporter_id") != null ? rs.getInt("linked_teleporter_id") : null;
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
                            cooldownOverride,
                            networkId,
                            linkedTeleporterId
                    );
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to query teleporter", e);
        }
        return null;
    }

    public boolean removeTeleporter(int id) {
        String sql = "DELETE FROM teleporters WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to remove teleporter", e);
            return false;
        }
    }

    public List<Teleporter> getAllTeleporters() {
        List<Teleporter> teleporters = new ArrayList<>();
        String sql = "SELECT id, name, world, x, y, z, yaw, owner_uuid, is_public, cooldown_override, network_id, linked_teleporter_id FROM teleporters";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Integer cooldownOverride = rs.getObject("cooldown_override") != null ? rs.getInt("cooldown_override") : null;
                Integer networkId = rs.getObject("network_id") != null ? rs.getInt("network_id") : null;
                Integer linkedTeleporterId = rs.getObject("linked_teleporter_id") != null ? rs.getInt("linked_teleporter_id") : null;
                teleporters.add(new Teleporter(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("world"),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"),
                        rs.getFloat("yaw"),
                        rs.getString("owner_uuid"),
                        rs.getBoolean("is_public"),
                        cooldownOverride,
                        networkId,
                        linkedTeleporterId
                ));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to list teleporters", e);
        }
        return teleporters;
    }

    public List<Teleporter> getAccessibleTeleporters(String playerUuid) {
        List<Teleporter> teleporters = new ArrayList<>();
        String sql = """
                SELECT DISTINCT t.id, t.name, t.world, t.x, t.y, t.z, t.yaw, t.owner_uuid, t.is_public, t.cooldown_override, t.network_id, t.linked_teleporter_id
                FROM teleporters t
                LEFT JOIN teleporter_access a ON t.id = a.teleporter_id
                WHERE t.is_public = 1 OR t.owner_uuid = ? OR a.player_uuid = ?
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Integer cooldownOverride = rs.getObject("cooldown_override") != null ? rs.getInt("cooldown_override") : null;
                    Integer networkId = rs.getObject("network_id") != null ? rs.getInt("network_id") : null;
                    Integer linkedTeleporterId = rs.getObject("linked_teleporter_id") != null ? rs.getInt("linked_teleporter_id") : null;
                    teleporters.add(new Teleporter(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("world"),
                            rs.getInt("x"),
                            rs.getInt("y"),
                            rs.getInt("z"),
                            rs.getFloat("yaw"),
                            rs.getString("owner_uuid"),
                            rs.getBoolean("is_public"),
                            cooldownOverride,
                            networkId,
                            linkedTeleporterId
                    ));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to get accessible teleporters", e);
        }
        return teleporters;
    }

    public int getTeleporterCountByOwner(String ownerUuid) {
        String sql = "SELECT COUNT(*) FROM teleporters WHERE owner_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ownerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to count teleporters by owner", e);
        }
        return 0;
    }

    public boolean setPublic(int teleporterId, boolean isPublic) {
        String sql = "UPDATE teleporters SET is_public = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, isPublic ? 1 : 0);
            stmt.setInt(2, teleporterId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to update visibility", e);
            return false;
        }
    }

    public boolean setCooldownOverride(int teleporterId, Integer cooldownSeconds) {
        String sql = "UPDATE teleporters SET cooldown_override = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (cooldownSeconds == null) {
                stmt.setNull(1, Types.INTEGER);
            } else {
                stmt.setInt(1, cooldownSeconds);
            }
            stmt.setInt(2, teleporterId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to update cooldown override", e);
            return false;
        }
    }

    public boolean addAccess(int teleporterId, String playerUuid) {
        String sql = "INSERT OR IGNORE INTO teleporter_access (teleporter_id, player_uuid) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, teleporterId);
            stmt.setString(2, playerUuid);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to add access", e);
            return false;
        }
    }

    public boolean removeAccess(int teleporterId, String playerUuid) {
        String sql = "DELETE FROM teleporter_access WHERE teleporter_id = ? AND player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, teleporterId);
            stmt.setString(2, playerUuid);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to remove access", e);
            return false;
        }
    }

    public List<String> getAccessList(int teleporterId) {
        List<String> uuids = new ArrayList<>();
        String sql = "SELECT player_uuid FROM teleporter_access WHERE teleporter_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, teleporterId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    uuids.add(rs.getString("player_uuid"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to get access list", e);
        }
        return uuids;
    }

    public boolean hasAccess(int teleporterId, String playerUuid) {
        String sql = "SELECT 1 FROM teleporter_access WHERE teleporter_id = ? AND player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, teleporterId);
            stmt.setString(2, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to check access", e);
            return false;
        }
    }

    public boolean isNameTaken(String name) {
        String sql = "SELECT 1 FROM teleporters WHERE name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to check name", e);
            return false;
        }
    }

    // ── Network Management ────────────────────────────────────────────────

    public boolean addNetwork(String name, String ownerUuid, String worldFilter) {
        String sql = "INSERT INTO networks (name, owner_uuid, world_filter, permission_node) VALUES (?, ?, ?, NULL)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, ownerUuid);
            if (worldFilter == null) {
                stmt.setNull(3, Types.VARCHAR);
            } else {
                stmt.setString(3, worldFilter);
            }
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to add network: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteNetwork(int networkId) {
        boolean originalAutoCommit;
        try {
            originalAutoCommit = connection.getAutoCommit();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to get auto-commit state before deleting network", e);
            return false;
        }

        try {
            connection.setAutoCommit(false);

            // First, unassign teleporters from this network
            String clearTeleportersSql = "UPDATE teleporters SET network_id = NULL WHERE network_id = ?";
            try (PreparedStatement clearStmt = connection.prepareStatement(clearTeleportersSql)) {
                clearStmt.setInt(1, networkId);
                clearStmt.executeUpdate();
            }

            // Then, delete the network itself
            String deleteNetworkSql = "DELETE FROM networks WHERE id = ?";
            int affectedRows;
            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteNetworkSql)) {
                deleteStmt.setInt(1, networkId);
                affectedRows = deleteStmt.executeUpdate();
            }

            connection.commit();
            return affectedRows > 0;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                logger.log(Level.WARNING, "Failed to rollback transaction when deleting network", rollbackEx);
            }
            logger.log(Level.WARNING, "Failed to delete network", e);
            return false;
        } finally {
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Failed to restore auto-commit state after deleting network", e);
            }
        }
    }

    public List<io.github.marcel.loadstonetp.model.Network> getNetworksByOwner(String ownerUuid) {
        List<io.github.marcel.loadstonetp.model.Network> networks = new ArrayList<>();
        String sql = "SELECT id, name, owner_uuid, world_filter, permission_node FROM networks WHERE owner_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ownerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String worldFilter = rs.getString("world_filter");
                    String permissionNode = rs.getString("permission_node");
                    networks.add(new io.github.marcel.loadstonetp.model.Network(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("owner_uuid"),
                            worldFilter,
                            permissionNode
                    ));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to get networks by owner", e);
        }
        return networks;
    }

    public io.github.marcel.loadstonetp.model.Network getNetwork(int networkId) {
        String sql = "SELECT id, name, owner_uuid, world_filter, permission_node FROM networks WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, networkId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String worldFilter = rs.getString("world_filter");
                    String permissionNode = rs.getString("permission_node");
                    return new io.github.marcel.loadstonetp.model.Network(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("owner_uuid"),
                            worldFilter,
                            permissionNode
                    );
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to get network", e);
        }
        return null;
    }

    public boolean setTeleporterNetwork(int teleporterId, Integer networkId) {
        String sql = "UPDATE teleporters SET network_id = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (networkId == null) {
                stmt.setNull(1, Types.INTEGER);
            } else {
                stmt.setInt(1, networkId);
            }
            stmt.setInt(2, teleporterId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to update teleporter network", e);
            return false;
        }
    }

    public boolean renameNetwork(int networkId, String newName) {
        String sql = "UPDATE networks SET name = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newName);
            stmt.setInt(2, networkId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to rename network", e);
            return false;
        }
    }

    public boolean setNetworkPermissionNode(int networkId, String permissionNode) {
        String sql = "UPDATE networks SET permission_node = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (permissionNode == null || permissionNode.isBlank()) {
                stmt.setNull(1, Types.VARCHAR);
            } else {
                stmt.setString(1, permissionNode.trim());
            }
            stmt.setInt(2, networkId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to set network permission node", e);
            return false;
        }
    }

    // ── Favorites Management ───────────────────────────────────────────────

    public boolean addFavorite(String playerUuid, int teleporterId) {
        String sql = "INSERT OR IGNORE INTO favorites (player_uuid, teleporter_id) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, teleporterId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to add favorite: " + e.getMessage());
            return false;
        }
    }

    public boolean removeFavorite(String playerUuid, int teleporterId) {
        String sql = "DELETE FROM favorites WHERE player_uuid = ? AND teleporter_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, teleporterId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to remove favorite: " + e.getMessage());
            return false;
        }
    }

    public boolean isFavorite(String playerUuid, int teleporterId) {
        String sql = "SELECT 1 FROM favorites WHERE player_uuid = ? AND teleporter_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, teleporterId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to check favorite", e);
            return false;
        }
    }

    public List<Teleporter> getFavoriteTeleporters(String playerUuid) {
        List<Teleporter> teleporters = new ArrayList<>();
        String sql = """
                SELECT DISTINCT t.id, t.name, t.world, t.x, t.y, t.z, t.yaw, t.owner_uuid, t.is_public, t.cooldown_override, t.network_id, t.linked_teleporter_id
                FROM teleporters t
                INNER JOIN favorites f ON t.id = f.teleporter_id
                WHERE f.player_uuid = ?
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Integer cooldownOverride = rs.getObject("cooldown_override") != null ? rs.getInt("cooldown_override") : null;
                    Integer networkId = rs.getObject("network_id") != null ? rs.getInt("network_id") : null;
                    Integer linkedTeleporterId = rs.getObject("linked_teleporter_id") != null ? rs.getInt("linked_teleporter_id") : null;
                    teleporters.add(new Teleporter(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("world"),
                            rs.getInt("x"),
                            rs.getInt("y"),
                            rs.getInt("z"),
                            rs.getFloat("yaw"),
                            rs.getString("owner_uuid"),
                            rs.getBoolean("is_public"),
                            cooldownOverride,
                            networkId,
                            linkedTeleporterId
                    ));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to get favorite teleporters", e);
        }
        return teleporters;
    }

    // ── Usage Stats ───────────────────────────────────────────────────────────

    public boolean recordTeleporterUse(String playerUuid, int teleporterId) {
        String sql = """
                INSERT INTO teleporter_usage (player_uuid, teleporter_id, use_count, last_used_at)
                VALUES (?, ?, 1, ?)
                ON CONFLICT(player_uuid, teleporter_id)
                DO UPDATE SET use_count = use_count + 1, last_used_at = excluded.last_used_at
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, teleporterId);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to record teleporter usage", e);
            return false;
        }
    }

    public Map<Integer, Integer> getUsageCounts(String playerUuid) {
        Map<Integer, Integer> usageCounts = new HashMap<>();
        String sql = "SELECT teleporter_id, use_count FROM teleporter_usage WHERE player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    usageCounts.put(rs.getInt("teleporter_id"), rs.getInt("use_count"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to read usage counts", e);
        }
        return usageCounts;
    }

    public Set<Integer> getFavoriteIds(String playerUuid) {
        Set<Integer> favoriteIds = new HashSet<>();
        String sql = "SELECT teleporter_id FROM favorites WHERE player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    favoriteIds.add(rs.getInt("teleporter_id"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to read favorite IDs", e);
        }
        return favoriteIds;
    }

    // ── Teleporter Linking ──────────────────────────────────────────────────────

    public boolean setLinkedTeleporter(int teleporterId, Integer linkedTeleporterId) {
        String sql = "UPDATE teleporters SET linked_teleporter_id = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (linkedTeleporterId == null) {
                stmt.setNull(1, Types.INTEGER);
            } else {
                stmt.setInt(1, linkedTeleporterId);
            }
            stmt.setInt(2, teleporterId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to update linked teleporter", e);
            return false;
        }
    }

    public Teleporter getTeleporter(int teleporterId) {
        String sql = "SELECT id, name, world, x, y, z, yaw, owner_uuid, is_public, cooldown_override, network_id, linked_teleporter_id FROM teleporters WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, teleporterId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Integer cooldownOverride = rs.getObject("cooldown_override") != null ? rs.getInt("cooldown_override") : null;
                    Integer networkId = rs.getObject("network_id") != null ? rs.getInt("network_id") : null;
                    Integer linkedTeleporterId = rs.getObject("linked_teleporter_id") != null ? rs.getInt("linked_teleporter_id") : null;
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
                            cooldownOverride,
                            networkId,
                            linkedTeleporterId
                    );
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to get teleporter by id", e);
        }
        return null;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to close database", e);
        }
    }
}
