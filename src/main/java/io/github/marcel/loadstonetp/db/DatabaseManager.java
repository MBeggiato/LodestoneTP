package io.github.marcel.loadstonetp.db;

import io.github.marcel.loadstonetp.model.Network;
import io.github.marcel.loadstonetp.model.Teleporter;
import org.bukkit.Location;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DatabaseManager {

    private final Logger logger;
    private final Connection connection;
    private final TeleporterRepository teleporterRepository;
    private final TeleporterAccessRepository teleporterAccessRepository;
    private final NetworkRepository networkRepository;
    private final FavoriteRepository favoriteRepository;
    private final UsageRepository usageRepository;

    public DatabaseManager(File dataFolder, Logger logger) {
        this.logger = logger;

        Connection initializedConnection = null;
        try {
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            String url = "jdbc:sqlite:" + new File(dataFolder, "teleporters.db").getAbsolutePath();
            initializedConnection = DriverManager.getConnection(url);
            try (Statement stmt = initializedConnection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
            new DatabaseMigrator(initializedConnection, logger).runMigrations();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize database", e);
        }

        this.connection = initializedConnection;

        DatabaseSupport support = new DatabaseSupport(this.connection, logger);
        this.teleporterRepository = new TeleporterRepository(support);
        this.teleporterAccessRepository = new TeleporterAccessRepository(support);
        this.networkRepository = new NetworkRepository(support);
        this.favoriteRepository = new FavoriteRepository(support);
        this.usageRepository = new UsageRepository(support);
    }

    TeleporterRepository teleporters() {
        return teleporterRepository;
    }

    TeleporterAccessRepository access() {
        return teleporterAccessRepository;
    }

    NetworkRepository networks() {
        return networkRepository;
    }

    FavoriteRepository favorites() {
        return favoriteRepository;
    }

    UsageRepository usage() {
        return usageRepository;
    }

    public boolean addTeleporter(String name, Location location, float yaw, String ownerUuid, boolean isPublic) {
        return teleporterRepository.addTeleporter(name, location, yaw, ownerUuid, isPublic);
    }

    public Teleporter getTeleporterAt(Location location) {
        return teleporterRepository.getTeleporterAt(location);
    }

    public boolean removeTeleporter(int id) {
        return teleporterRepository.removeTeleporter(id);
    }

    public List<Teleporter> getAllTeleporters() {
        return teleporterRepository.getAllTeleporters();
    }

    public List<Teleporter> getAccessibleTeleporters(String playerUuid) {
        return teleporterAccessRepository.getAccessibleTeleporters(playerUuid);
    }

    public int getTeleporterCountByOwner(String ownerUuid) {
        return teleporterRepository.getTeleporterCountByOwner(ownerUuid);
    }

    public boolean setPublic(int teleporterId, boolean isPublic) {
        return teleporterRepository.setPublic(teleporterId, isPublic);
    }

    public boolean setCooldownOverride(int teleporterId, Integer cooldownSeconds) {
        return teleporterRepository.setCooldownOverride(teleporterId, cooldownSeconds);
    }

    public boolean addAccess(int teleporterId, String playerUuid) {
        return teleporterAccessRepository.addAccess(teleporterId, playerUuid);
    }

    public boolean removeAccess(int teleporterId, String playerUuid) {
        return teleporterAccessRepository.removeAccess(teleporterId, playerUuid);
    }

    public List<String> getAccessList(int teleporterId) {
        return teleporterAccessRepository.getAccessList(teleporterId);
    }

    public boolean hasAccess(int teleporterId, String playerUuid) {
        return teleporterAccessRepository.hasAccess(teleporterId, playerUuid);
    }

    public boolean isNameTaken(String name) {
        return teleporterRepository.isNameTaken(name);
    }

    public boolean addNetwork(String name, String ownerUuid, String worldFilter) {
        return networkRepository.addNetwork(name, ownerUuid, worldFilter);
    }

    public boolean deleteNetwork(int networkId) {
        return networkRepository.deleteNetwork(networkId);
    }

    public List<Network> getNetworksByOwner(String ownerUuid) {
        return networkRepository.getNetworksByOwner(ownerUuid);
    }

    public Network getNetwork(int networkId) {
        return networkRepository.getNetwork(networkId);
    }

    public boolean setTeleporterNetwork(int teleporterId, Integer networkId) {
        return teleporterRepository.setTeleporterNetwork(teleporterId, networkId);
    }

    public boolean renameNetwork(int networkId, String newName) {
        return networkRepository.renameNetwork(networkId, newName);
    }

    public boolean setNetworkPermissionNode(int networkId, String permissionNode) {
        return networkRepository.setNetworkPermissionNode(networkId, permissionNode);
    }

    public boolean addFavorite(String playerUuid, int teleporterId) {
        return favoriteRepository.addFavorite(playerUuid, teleporterId);
    }

    public boolean removeFavorite(String playerUuid, int teleporterId) {
        return favoriteRepository.removeFavorite(playerUuid, teleporterId);
    }

    public boolean isFavorite(String playerUuid, int teleporterId) {
        return favoriteRepository.isFavorite(playerUuid, teleporterId);
    }

    public List<Teleporter> getFavoriteTeleporters(String playerUuid) {
        return favoriteRepository.getFavoriteTeleporters(playerUuid);
    }

    public boolean recordTeleporterUse(String playerUuid, int teleporterId) {
        return usageRepository.recordTeleporterUse(playerUuid, teleporterId);
    }

    public Map<Integer, Integer> getUsageCounts(String playerUuid) {
        return usageRepository.getUsageCounts(playerUuid);
    }

    public Set<Integer> getFavoriteIds(String playerUuid) {
        return favoriteRepository.getFavoriteIds(playerUuid);
    }

    public boolean setLinkedTeleporter(int teleporterId, Integer linkedTeleporterId) {
        return teleporterRepository.setLinkedTeleporter(teleporterId, linkedTeleporterId);
    }

    public Teleporter getTeleporter(int teleporterId) {
        return teleporterRepository.getTeleporter(teleporterId);
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
