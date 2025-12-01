package world.bentobox.islandselector.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Manages the grid of island locations
 */
public class GridManager {

    private final IslandSelector addon;
    private final Map<String, GridLocation> gridLocations; // Key: coordinate string like "A1"
    private final Map<UUID, GridCoordinate> islandToCoord; // Maps island UUIDs to their coordinates

    public GridManager(IslandSelector addon) {
        this.addon = addon;
        this.gridLocations = new HashMap<>();
        this.islandToCoord = new HashMap<>();

        // Initialize from BSkyBlock islands
        syncWithBSkyBlock();
    }

    /**
     * Sync grid locations with existing BSkyBlock islands
     */
    public void syncWithBSkyBlock() {
        BentoBox bentoBox = BentoBox.getInstance();
        if (bentoBox == null) return;

        // Get the BSkyBlock addon
        Optional<?> bSkyBlock = bentoBox.getAddonsManager().getAddonByName("BSkyBlock");
        if (bSkyBlock.isEmpty()) {
            addon.logWarning("BSkyBlock not found - cannot sync islands");
            return;
        }

        IslandsManager islandsManager = bentoBox.getIslandsManager();

        // For each existing island, calculate its grid position and add it
        // This is a simplified version - in production you'd iterate all islands
        addon.log("Grid manager initialized. Grid size: " +
            addon.getSettings().getGridWidth() + "x" +
            addon.getSettings().getGridHeight());
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
        islandToCoord.put(islandUUID, coord);
    }

    /**
     * Get the coordinate for a player's island
     */
    public GridCoordinate getPlayerIslandCoordinate(UUID playerUUID) {
        BentoBox bentoBox = BentoBox.getInstance();
        if (bentoBox == null) return null;

        // Get player's island
        IslandsManager islandsManager = bentoBox.getIslandsManager();
        // Find the island and return its coordinate
        for (Map.Entry<String, GridLocation> entry : gridLocations.entrySet()) {
            GridLocation location = entry.getValue();
            if (playerUUID.equals(location.getOwnerUUID())) {
                return location.getCoordinate();
            }
        }
        return null;
    }

    /**
     * Convert grid coordinate to world X coordinate
     */
    public int getWorldX(GridCoordinate coord) {
        return coord.getColumn() * addon.getSettings().getIslandSpacing() +
               addon.getSettings().getGridOriginX();
    }

    /**
     * Convert grid coordinate to world Z coordinate
     */
    public int getWorldZ(GridCoordinate coord) {
        return coord.getRow() * addon.getSettings().getIslandSpacing() +
               addon.getSettings().getGridOriginZ();
    }

    /**
     * Convert world coordinates to grid coordinate
     */
    public GridCoordinate worldToGrid(int worldX, int worldZ) {
        int column = (worldX - addon.getSettings().getGridOriginX()) / addon.getSettings().getIslandSpacing();
        int row = (worldZ - addon.getSettings().getGridOriginZ()) / addon.getSettings().getIslandSpacing();

        if (column < 0 || row < 0) {
            return null;
        }
        return new GridCoordinate(column, row);
    }
}
