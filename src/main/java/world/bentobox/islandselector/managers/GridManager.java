package world.bentobox.islandselector.managers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Manages the grid of island locations with database persistence
 */
public class GridManager {

    private final IslandSelector addon;
    private final Map<String, GridLocation> gridLocations; // Key: coordinate string like "A1"
    private final Map<UUID, GridCoordinate> islandToCoord; // Maps island UUIDs to their coordinates
    private final Map<UUID, GridCoordinate> playerToCoord; // Maps player UUIDs to their coordinates

    // Database handler for persistence
    private final Database<GridLocationData> database;

    // BSkyBlock world reference
    private World bskyblockWorld;

    public GridManager(IslandSelector addon) {
        this.addon = addon;
        this.gridLocations = new HashMap<>();
        this.islandToCoord = new HashMap<>();
        this.playerToCoord = new HashMap<>();

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

                    // Update lookup maps
                    if (data.getIslandUUIDAsUUID() != null) {
                        islandToCoord.put(data.getIslandUUIDAsUUID(), coord);
                    }
                    playerToCoord.put(data.getOwnerUUIDAsUUID(), coord);
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
            location.occupy(ownerUUID, ownerName, island.getUniqueId() != null ?
                UUID.fromString(island.getUniqueId()) : null);

            // Update lookup maps
            if (island.getUniqueId() != null) {
                try {
                    islandToCoord.put(UUID.fromString(island.getUniqueId()), coord);
                } catch (IllegalArgumentException e) {
                    // Invalid UUID format, skip
                }
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
     */
    public boolean isWithinBounds(GridCoordinate coord) {
        return coord.getColumn() >= 0 &&
               coord.getColumn() < addon.getSettings().getGridWidth() &&
               coord.getRow() >= 0 &&
               coord.getRow() < addon.getSettings().getGridHeight();
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
     * Occupy a location with an island
     */
    public void occupyLocation(GridCoordinate coord, UUID ownerUUID, String ownerName, UUID islandUUID) {
        GridLocation location = getOrCreateGridLocation(coord);
        location.occupy(ownerUUID, ownerName, islandUUID);

        // Update lookup maps
        if (islandUUID != null) {
            islandToCoord.put(islandUUID, coord);
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
            // Remove from lookup maps
            if (location.getIslandUUID() != null) {
                islandToCoord.remove(location.getIslandUUID());
            }
            if (location.getOwnerUUID() != null) {
                playerToCoord.remove(location.getOwnerUUID());
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
}
