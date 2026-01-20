package world.bentobox.islandselector.managers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.GridLocationData;
import world.bentobox.islandselector.models.DimensionConfig;
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Manages the grid of island locations with database persistence
 */
public class GridManager {

    private final IslandSelector addon;
    private final Map<String, GridLocation> gridLocations; // Key: coordinate string like "A1" - thread-safe
    private final Map<UUID, GridCoordinate> islandToCoord; // Maps island UUIDs to their coordinates - thread-safe
    private final Map<UUID, GridCoordinate> playerToCoord; // Maps player UUIDs to their coordinates - thread-safe

    // Multi-dimension support: Maps dimension key -> (island UUID -> grid coordinate) - thread-safe
    private final Map<String, Map<UUID, GridCoordinate>> dimensionIslandToCoord;

    // Database handler for persistence
    private final Database<GridLocationData> database;

    // BSkyBlock world reference (primary/overworld dimension)
    private World bskyblockWorld;

    public GridManager(IslandSelector addon) {
        this.addon = addon;
        this.gridLocations = new ConcurrentHashMap<>();
        this.islandToCoord = new ConcurrentHashMap<>();
        this.playerToCoord = new ConcurrentHashMap<>();
        this.dimensionIslandToCoord = new ConcurrentHashMap<>();

        // Initialize database
        this.database = new Database<>(addon, GridLocationData.class);

        // Load from database and sync with BSkyBlock
        loadFromDatabase();
        syncWithBSkyBlock();
    }

    /**
     * Load all grid location data from database
     */
    private void loadFromDatabase() {
        addon.log("Loading grid locations from database...");

        try {
            database.loadObjects().forEach(data -> {
                GridCoordinate coord = new GridCoordinate(data.getColumn(), data.getRow());
                GridLocation location = new GridLocation(coord);

                // Set status
                try {
                    location.setStatus(GridLocation.Status.valueOf(data.getStatus()));
                } catch (IllegalArgumentException e) {
                    location.setStatus(GridLocation.Status.AVAILABLE);
                }

                // Set owner info
                if (data.getOwnerUUIDAsUUID() != null) {
                    location.setOwnerUUID(data.getOwnerUUIDAsUUID());
                    location.setOwnerName(data.getOwnerName());
                    location.setIslandUUID(data.getIslandUUIDAsUUID());

                    // Update legacy lookup maps
                    if (data.getIslandUUIDAsUUID() != null) {
                        islandToCoord.put(data.getIslandUUIDAsUUID(), coord);
                    }
                    playerToCoord.put(data.getOwnerUUIDAsUUID(), coord);

                    // Load dimension island UUIDs
                    Map<String, String> dimIslands = data.getDimensionIslandUUIDs();
                    if (dimIslands != null && !dimIslands.isEmpty()) {
                        Map<String, UUID> dimensionUUIDs = new HashMap<>();
                        for (Map.Entry<String, String> entry : dimIslands.entrySet()) {
                            String dimensionKey = entry.getKey();
                            String uuidStr = entry.getValue();
                            if (uuidStr != null && !uuidStr.isEmpty()) {
                                try {
                                    UUID islandUUID = UUID.fromString(uuidStr);
                                    dimensionUUIDs.put(dimensionKey, islandUUID);

                                    // Update dimension lookup map
                                    dimensionIslandToCoord
                                            .computeIfAbsent(dimensionKey, k -> new HashMap<>())
                                            .put(islandUUID, coord);
                                } catch (IllegalArgumentException e) {
                                    // Log skipped records for debugging
                                    addon.logWarning("Skipped invalid UUID '" + uuidStr +
                                        "' for dimension " + dimensionKey + " at " + coord);
                                }
                            }
                        }
                        location.setDimensionIslandUUIDs(dimensionUUIDs);
                    }
                }

                // Set reserved status
                location.setReserved(data.isReserved());
                location.setBlocked(data.isBlocked());
                location.setPurchasePrice(data.getPurchasePrice());

                gridLocations.put(coord.toString(), location);
            });

            addon.log("Loaded " + gridLocations.size() + " grid locations from database.");
        } catch (Exception e) {
            addon.logError("Failed to load grid locations from database: " + e.getMessage());
        }
    }

    /**
     * Save a grid location to database
     */
    public void saveGridLocation(GridCoordinate coord) {
        GridLocation location = gridLocations.get(coord.toString());
        if (location == null) {
            return;
        }

        GridLocationData data = new GridLocationData(coord.toString(), coord.getColumn(), coord.getRow());
        data.setStatus(location.getStatus().name());

        if (location.getOwnerUUID() != null) {
            data.setOwnerUUID(location.getOwnerUUID());
            data.setOwnerName(location.getOwnerName());
        }
        if (location.getIslandUUID() != null) {
            data.setIslandUUID(location.getIslandUUID());
        }

        // Save dimension island UUIDs
        Map<String, UUID> dimIslands = location.getDimensionIslandUUIDs();
        if (dimIslands != null && !dimIslands.isEmpty()) {
            Map<String, String> dimIslandStrings = new HashMap<>();
            for (Map.Entry<String, UUID> entry : dimIslands.entrySet()) {
                if (entry.getValue() != null) {
                    dimIslandStrings.put(entry.getKey(), entry.getValue().toString());
                }
            }
            data.setDimensionIslandUUIDs(dimIslandStrings);
        }

        data.setReserved(location.isReserved());
        data.setBlocked(location.isBlocked());
        data.setPurchasePrice(location.getPurchasePrice());

        database.saveObjectAsync(data);
    }

    /**
     * Delete a grid location from database
     */
    public void deleteGridLocation(GridCoordinate coord) {
        database.deleteID(coord.toString());
    }

    /**
     * Save all grid locations to database
     */
    public void saveAll() {
        addon.log("Saving all grid locations to database...");
        for (Map.Entry<String, GridLocation> entry : gridLocations.entrySet()) {
            GridCoordinate coord = GridCoordinate.parse(entry.getKey());
            if (coord != null) {
                saveGridLocation(coord);
            }
        }
        addon.log("Saved " + gridLocations.size() + " grid locations.");
    }

    /**
     * Sync grid locations with existing BSkyBlock islands
     */
    public void syncWithBSkyBlock() {
        BentoBox bentoBox = BentoBox.getInstance();
        if (bentoBox == null) {
            addon.logWarning("BentoBox not found - cannot sync islands");
            return;
        }

        // Get the BSkyBlock addon
        Optional<GameModeAddon> bSkyBlockOpt = bentoBox.getAddonsManager()
            .getGameModeAddons().stream()
            .filter(gm -> gm.getDescription().getName().equalsIgnoreCase("BSkyBlock"))
            .findFirst();

        if (bSkyBlockOpt.isEmpty()) {
            addon.logWarning("BSkyBlock not found - cannot sync islands");
            return;
        }

        GameModeAddon bSkyBlock = bSkyBlockOpt.get();
        this.bskyblockWorld = bSkyBlock.getOverWorld();

        if (bskyblockWorld == null) {
            addon.logWarning("BSkyBlock world not found - cannot sync islands");
            return;
        }

        IslandsManager islandsManager = bentoBox.getIslandsManager();

        // Get all islands in the BSkyBlock world
        addon.log("Syncing grid with BSkyBlock islands...");
        int synced = 0;
        int skipped = 0;

        // Iterate through all islands
        for (Island island : islandsManager.getIslands(bskyblockWorld)) {
            if (island == null || island.getCenter() == null) {
                continue;
            }

            // Calculate grid coordinate from island center
            Location center = island.getCenter();
            GridCoordinate coord = worldToGrid(center.getBlockX(), center.getBlockZ());

            if (coord == null || !isWithinBounds(coord)) {
                skipped++;
                continue;
            }

            // Check if this location already exists in our grid
            GridLocation existing = gridLocations.get(coord.toString());

            if (existing != null && existing.getStatus() == GridLocation.Status.OCCUPIED) {
                // Already tracked
                continue;
            }

            // Get island owner
            UUID ownerUUID = island.getOwner();
            if (ownerUUID == null) {
                skipped++;
                continue;
            }

            String ownerName = Bukkit.getOfflinePlayer(ownerUUID).getName();
            if (ownerName == null) {
                ownerName = "Unknown";
            }

            // Create or update grid location
            GridLocation location = getOrCreateGridLocation(coord);

            // Try to parse island unique ID as UUID (may not always be valid UUID format)
            UUID islandUUID = null;
            String islandIdStr = island.getUniqueId();
            if (islandIdStr != null && !islandIdStr.isEmpty()) {
                try {
                    islandUUID = UUID.fromString(islandIdStr);
                } catch (IllegalArgumentException e) {
                    // Island ID is not a valid UUID format - this is OK
                }
            }

            location.occupy(ownerUUID, ownerName, islandUUID);
            location.setIslandId(islandIdStr); // Store original ID string

            // Update lookup maps
            if (islandUUID != null) {
                islandToCoord.put(islandUUID, coord);
            }
            playerToCoord.put(ownerUUID, coord);

            // Save to database
            saveGridLocation(coord);
            synced++;
        }

        addon.log("Sync complete: " + synced + " islands synced, " + skipped + " skipped.");
        addon.log("Grid size: " + addon.getSettings().getGridWidth() + "x" + addon.getSettings().getGridHeight());
    }

    /**
     * Get grid location data for a coordinate
     */
    public GridLocation getGridLocation(GridCoordinate coord) {
        return gridLocations.get(coord.toString());
    }

    /**
     * Get or create grid location for a coordinate
     */
    public GridLocation getOrCreateGridLocation(GridCoordinate coord) {
        return gridLocations.computeIfAbsent(coord.toString(), k -> new GridLocation(coord));
    }

    /**
     * Check if a coordinate is within the current grid boundaries
     * Grid uses min/max X and Z values (supports negative coordinates)
     */
    public boolean isWithinBounds(GridCoordinate coord) {
        return coord.getX() >= addon.getSettings().getGridMinX() &&
               coord.getX() <= addon.getSettings().getGridMaxX() &&
               coord.getZ() >= addon.getSettings().getGridMinZ() &&
               coord.getZ() <= addon.getSettings().getGridMaxZ();
    }

    /**
     * Check if a coordinate is reserved
     */
    public boolean isReserved(GridCoordinate coord) {
        GridLocation location = getGridLocation(coord);
        return location != null && location.isReserved();
    }

    /**
     * Reserve a grid location
     * @param coord The coordinate to reserve
     * @param blocked True if blocked (not purchasable), false if can be purchased
     */
    public void reserveLocation(GridCoordinate coord, boolean blocked) {
        GridLocation location = getOrCreateGridLocation(coord);
        location.setReserved(true);
        location.setBlocked(blocked);
        location.setStatus(GridLocation.Status.RESERVED);
        saveGridLocation(coord);
    }

    /**
     * Unreserve a location
     */
    public void unreserveLocation(GridCoordinate coord) {
        GridLocation location = getGridLocation(coord);
        if (location != null) {
            location.setReserved(false);
            location.setBlocked(false);
            location.setPurchasePrice(0);
            if (location.getOwnerUUID() == null) {
                location.setStatus(GridLocation.Status.AVAILABLE);
            }
            saveGridLocation(coord);
        }
    }

    /**
     * Set the purchase price for a reserved location
     */
    public void setPurchasePrice(GridCoordinate coord, double price) {
        GridLocation location = getOrCreateGridLocation(coord);
        location.setPurchasePrice(price);
        if (price > 0) {
            location.setBlocked(false);
            location.setReserved(true);
            location.setStatus(GridLocation.Status.RESERVED);
        }
        saveGridLocation(coord);
    }

    /**
     * Check if a location is available for claiming
     */
    public boolean isAvailable(GridCoordinate coord) {
        if (!isWithinBounds(coord)) {
            return false;
        }
        GridLocation location = getGridLocation(coord);
        return location == null || location.isAvailable();
    }

    /**
     * Get all player UUIDs who have occupied grid locations
     */
    public Set<UUID> getAllOccupiedPlayerUUIDs() {
        return new HashSet<>(playerToCoord.keySet());
    }

    /**
     * Get the grid coordinate string for a player's island
     * @param playerUUID The player's UUID
     * @return Grid coordinate string like "0,0" or null if not found
     */
    public String getPlayerGridCoordinate(UUID playerUUID) {
        GridCoordinate coord = playerToCoord.get(playerUUID);
        return coord != null ? coord.toString() : null;
    }

    /**
     * Get the status of a grid location
     */
    public GridLocation.Status getLocationStatus(GridCoordinate coord) {
        if (!isWithinBounds(coord)) {
            return GridLocation.Status.LOCKED;
        }

        GridLocation location = getGridLocation(coord);
        if (location == null) {
            return GridLocation.Status.AVAILABLE;
        }
        return location.getStatus();
    }

    /**
     * Check if the owner of a location is online
     */
    public boolean isOwnerOnline(GridCoordinate coord) {
        GridLocation location = getGridLocation(coord);
        if (location == null || location.getOwnerUUID() == null) {
            return false;
        }
        Player player = Bukkit.getPlayer(location.getOwnerUUID());
        return player != null && player.isOnline();
    }

    /**
     * Occupy a location with an island (legacy single-dimension)
     */
    public void occupyLocation(GridCoordinate coord, UUID ownerUUID, String ownerName, UUID islandUUID) {
        GridLocation location = getOrCreateGridLocation(coord);
        location.occupy(ownerUUID, ownerName, islandUUID);

        // Update lookup maps
        if (islandUUID != null) {
            islandToCoord.put(islandUUID, coord);
            // Also add to overworld dimension map for consistency
            dimensionIslandToCoord
                    .computeIfAbsent("overworld", k -> new HashMap<>())
                    .put(islandUUID, coord);
        }
        if (ownerUUID != null) {
            playerToCoord.put(ownerUUID, coord);
        }

        // Save to database
        saveGridLocation(coord);
    }

    /**
     * Occupy a location with islands in multiple dimensions
     * @param coord The grid coordinate
     * @param ownerUUID The owner's UUID
     * @param ownerName The owner's name
     * @param dimensionIslands Map of dimension key to island UUID
     */
    public void occupyLocation(GridCoordinate coord, UUID ownerUUID, String ownerName,
                               Map<String, UUID> dimensionIslands) {
        GridLocation location = getOrCreateGridLocation(coord);
        location.occupy(ownerUUID, ownerName, dimensionIslands);

        // Update dimension lookup maps
        if (dimensionIslands != null) {
            for (Map.Entry<String, UUID> entry : dimensionIslands.entrySet()) {
                String dimensionKey = entry.getKey();
                UUID islandUUID = entry.getValue();
                if (islandUUID != null) {
                    dimensionIslandToCoord
                            .computeIfAbsent(dimensionKey, k -> new HashMap<>())
                            .put(islandUUID, coord);
                    // Also add to legacy map if this is overworld
                    if ("overworld".equals(dimensionKey)) {
                        islandToCoord.put(islandUUID, coord);
                    }
                }
            }
        }

        if (ownerUUID != null) {
            playerToCoord.put(ownerUUID, coord);
        }

        // Save to database
        saveGridLocation(coord);
    }

    /**
     * Clear a location (make it available again)
     */
    public void clearLocation(GridCoordinate coord) {
        GridLocation location = getGridLocation(coord);
        if (location != null) {
            // Remove from legacy lookup maps
            if (location.getIslandUUID() != null) {
                islandToCoord.remove(location.getIslandUUID());
            }
            if (location.getOwnerUUID() != null) {
                playerToCoord.remove(location.getOwnerUUID());
            }

            // Remove from dimension lookup maps
            Map<String, UUID> dimIslands = location.getDimensionIslandUUIDs();
            if (dimIslands != null) {
                for (Map.Entry<String, UUID> entry : dimIslands.entrySet()) {
                    String dimensionKey = entry.getKey();
                    UUID islandUUID = entry.getValue();
                    if (islandUUID != null) {
                        Map<UUID, GridCoordinate> dimMap = dimensionIslandToCoord.get(dimensionKey);
                        if (dimMap != null) {
                            dimMap.remove(islandUUID);
                        }
                    }
                }
            }

            location.clear();
            saveGridLocation(coord);
        }
    }

    /**
     * Get the coordinate for a player's island
     */
    public GridCoordinate getPlayerIslandCoordinate(UUID playerUUID) {
        // First check our cache
        GridCoordinate cached = playerToCoord.get(playerUUID);
        if (cached != null) {
            return cached;
        }

        // Search through grid locations
        for (Map.Entry<String, GridLocation> entry : gridLocations.entrySet()) {
            GridLocation location = entry.getValue();
            if (playerUUID.equals(location.getOwnerUUID())) {
                GridCoordinate coord = location.getCoordinate();
                playerToCoord.put(playerUUID, coord); // Cache it
                return coord;
            }
        }

        // Try to find from BSkyBlock
        BentoBox bentoBox = BentoBox.getInstance();
        if (bentoBox != null && bskyblockWorld != null) {
            Island island = bentoBox.getIslandsManager().getIsland(bskyblockWorld, playerUUID);
            if (island != null && island.getCenter() != null) {
                Location center = island.getCenter();
                GridCoordinate coord = worldToGrid(center.getBlockX(), center.getBlockZ());
                if (coord != null && isWithinBounds(coord)) {
                    playerToCoord.put(playerUUID, coord);
                    return coord;
                }
            }
        }

        return null;
    }

    /**
     * Get the coordinate for an island UUID
     */
    public GridCoordinate getIslandCoordinate(UUID islandUUID) {
        return islandToCoord.get(islandUUID);
    }

    /**
     * Convert grid coordinate to world X coordinate
     * Grid (0,0) corresponds to world (0, 0)
     * BSkyBlock places islands at multiples of (distance * 2)
     */
    public int getWorldX(GridCoordinate coord) {
        return coord.getX() * addon.getIslandSpacing() * 2;
    }

    /**
     * Convert grid coordinate to world Z coordinate
     * Grid (0,0) corresponds to world (0, 0)
     * BSkyBlock places islands at multiples of (distance * 2)
     */
    public int getWorldZ(GridCoordinate coord) {
        return coord.getZ() * addon.getIslandSpacing() * 2;
    }

    /**
     * Convert world coordinates to grid coordinate
     * World (0, 0) corresponds to grid (0,0)
     * BSkyBlock places islands at multiples of (distance * 2)
     */
    public GridCoordinate worldToGrid(int worldX, int worldZ) {
        // BSkyBlock's "distance-between-islands" is actually the offset from center
        // Real spacing between island centers is distance * 2
        int actualSpacing = addon.getIslandSpacing() * 2;

        // Prevent division by zero if spacing is invalid
        if (actualSpacing <= 0) {
            addon.logWarning("Invalid island spacing detected (" + actualSpacing + "), defaulting to grid (0,0)");
            return new GridCoordinate(0, 0);
        }

        // Use floor division to get the grid coordinate
        // This handles both positive and negative coordinates correctly
        int gridX = Math.floorDiv(worldX, actualSpacing);
        int gridZ = Math.floorDiv(worldZ, actualSpacing);

        return new GridCoordinate(gridX, gridZ);
    }

    /**
     * Get the BSkyBlock world
     */
    public World getBSkyBlockWorld() {
        return bskyblockWorld;
    }

    /**
     * Get total number of tracked grid locations
     */
    public int getTrackedLocationCount() {
        return gridLocations.size();
    }

    /**
     * Get number of occupied locations
     */
    public int getOccupiedLocationCount() {
        return (int) gridLocations.values().stream()
            .filter(loc -> loc.getStatus() == GridLocation.Status.OCCUPIED)
            .count();
    }

    /**
     * Get number of reserved locations
     */
    public int getReservedLocationCount() {
        return (int) gridLocations.values().stream()
            .filter(GridLocation::isReserved)
            .count();
    }

    /**
     * Get all tracked grid locations
     * Used for searching and iteration
     * @return Collection of all grid locations
     */
    public Collection<GridLocation> getAllLocations() {
        return gridLocations.values();
    }

    // ========== Multi-Dimension Support Methods ==========

    /**
     * Get the island coordinate for a specific dimension
     * @param dimensionKey The dimension key
     * @param islandUUID The island UUID
     * @return The grid coordinate, or null if not found
     */
    public GridCoordinate getIslandCoordinate(String dimensionKey, UUID islandUUID) {
        Map<UUID, GridCoordinate> dimMap = dimensionIslandToCoord.get(dimensionKey);
        if (dimMap != null) {
            return dimMap.get(islandUUID);
        }
        // Fallback to legacy map for overworld
        if ("overworld".equals(dimensionKey)) {
            return islandToCoord.get(islandUUID);
        }
        return null;
    }

    /**
     * Get all island UUIDs for a grid coordinate across all dimensions
     * @param coord The grid coordinate
     * @return Map of dimension key to island UUID
     */
    public Map<String, UUID> getDimensionIslands(GridCoordinate coord) {
        GridLocation location = getGridLocation(coord);
        if (location != null) {
            return location.getDimensionIslandUUIDs();
        }
        return new HashMap<>();
    }

    /**
     * Get the island UUID for a specific dimension at a grid coordinate
     * @param coord The grid coordinate
     * @param dimensionKey The dimension key
     * @return The island UUID, or null if not found
     */
    public UUID getIslandUUID(GridCoordinate coord, String dimensionKey) {
        GridLocation location = getGridLocation(coord);
        if (location != null) {
            return location.getIslandUUID(dimensionKey);
        }
        return null;
    }

    /**
     * Set the island UUID for a specific dimension at a grid coordinate
     * @param coord The grid coordinate
     * @param dimensionKey The dimension key
     * @param islandUUID The island UUID
     */
    public void setDimensionIsland(GridCoordinate coord, String dimensionKey, UUID islandUUID) {
        GridLocation location = getOrCreateGridLocation(coord);
        location.setIslandUUID(dimensionKey, islandUUID);

        // Update dimension lookup map
        if (islandUUID != null) {
            dimensionIslandToCoord
                    .computeIfAbsent(dimensionKey, k -> new HashMap<>())
                    .put(islandUUID, coord);
        } else {
            Map<UUID, GridCoordinate> dimMap = dimensionIslandToCoord.get(dimensionKey);
            if (dimMap != null) {
                // Find and remove the old island UUID
                dimMap.values().removeIf(c -> c.equals(coord));
            }
        }

        saveGridLocation(coord);
    }

    /**
     * Clear the island for a specific dimension at a grid coordinate
     * @param coord The grid coordinate
     * @param dimensionKey The dimension key
     */
    public void clearDimensionIsland(GridCoordinate coord, String dimensionKey) {
        GridLocation location = getGridLocation(coord);
        if (location != null) {
            UUID oldUUID = location.getIslandUUID(dimensionKey);
            if (oldUUID != null) {
                Map<UUID, GridCoordinate> dimMap = dimensionIslandToCoord.get(dimensionKey);
                if (dimMap != null) {
                    dimMap.remove(oldUUID);
                }
                if ("overworld".equals(dimensionKey)) {
                    islandToCoord.remove(oldUUID);
                }
            }
            location.clearDimensionIsland(dimensionKey);
            saveGridLocation(coord);
        }
    }

    /**
     * Check if multi-dimension mode is enabled
     * @return true if multi-dimension is enabled
     */
    public boolean isMultiDimensionEnabled() {
        DimensionManager dimManager = addon.getDimensionManager();
        return dimManager != null && dimManager.isEnabled();
    }

    /**
     * Sync islands from all dimension worlds (if multi-dimension is enabled)
     */
    public void syncAllDimensionWorlds() {
        if (!isMultiDimensionEnabled()) {
            return;
        }

        DimensionManager dimManager = addon.getDimensionManager();
        List<DimensionConfig> dimensions = dimManager.getEnabledDimensions();

        BentoBox bentoBox = BentoBox.getInstance();
        if (bentoBox == null) {
            return;
        }

        IslandsManager islandsManager = bentoBox.getIslandsManager();

        addon.log("Syncing grid with all dimension worlds...");

        for (DimensionConfig config : dimensions) {
            World world = dimManager.getWorld(config.getDimensionKey());
            if (world == null) {
                continue;
            }

            int synced = 0;
            for (Island island : islandsManager.getIslands(world)) {
                if (island == null || island.getCenter() == null || island.getOwner() == null) {
                    continue;
                }

                Location center = island.getCenter();
                GridCoordinate coord = worldToGrid(center.getBlockX(), center.getBlockZ());

                if (coord == null || !isWithinBounds(coord)) {
                    continue;
                }

                // Get existing location or check if we need to track this island
                GridLocation location = getGridLocation(coord);
                if (location != null && location.getIslandUUID(config.getDimensionKey()) != null) {
                    continue; // Already tracked
                }

                // Parse island UUID
                UUID islandUUID = null;
                String islandIdStr = island.getUniqueId();
                if (islandIdStr != null && !islandIdStr.isEmpty()) {
                    try {
                        islandUUID = UUID.fromString(islandIdStr);
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                if (islandUUID != null) {
                    setDimensionIsland(coord, config.getDimensionKey(), islandUUID);
                    synced++;
                }
            }

            if (synced > 0) {
                addon.log("  - " + config.getDimensionKey() + ": " + synced + " islands synced");
            }
        }
    }
}
