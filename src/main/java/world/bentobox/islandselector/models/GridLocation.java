package world.bentobox.islandselector.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Represents a location on the island grid and its current status
 */
public class GridLocation {

    public enum Status {
        AVAILABLE,    // Empty and can be claimed
        OCCUPIED,     // Has an island and owner
        RESERVED,     // Reserved by admin (blocked or purchasable)
        LOCKED        // Outside current grid boundaries
    }

    private final GridCoordinate coordinate;
    private Status status;
    private UUID ownerUUID;
    private String ownerName;
    private UUID islandUUID;  // Legacy single-dimension field (overworld)
    private String islandId;  // BentoBox island unique ID (String, not UUID)
    private boolean reserved;
    private boolean blocked;      // If reserved, is it blocked (not purchasable)?
    private double purchasePrice; // If reserved and purchasable, the price

    // Multi-dimension support
    private Map<String, UUID> dimensionIslandUUIDs = new HashMap<>();
    private Map<String, String> dimensionIslandIds = new HashMap<>();

    public GridLocation(GridCoordinate coordinate) {
        this.coordinate = coordinate;
        this.status = Status.AVAILABLE;
        this.reserved = false;
        this.blocked = false;
        this.purchasePrice = 0;
    }

    public GridCoordinate getCoordinate() {
        return coordinate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public UUID getIslandUUID() {
        return islandUUID;
    }

    public void setIslandUUID(UUID islandUUID) {
        this.islandUUID = islandUUID;
    }

    public String getIslandId() {
        return islandId;
    }

    public void setIslandId(String islandId) {
        this.islandId = islandId;
    }

    public boolean isReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
        if (reserved) {
            this.status = Status.RESERVED;
        }
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean isPurchasable() {
        return reserved && !blocked && purchasePrice > 0;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
        if (purchasePrice > 0) {
            this.blocked = false; // If has a price, it's purchasable
        }
    }

    /**
     * Check if this location is available for claiming
     */
    public boolean isAvailable() {
        return status == Status.AVAILABLE && !reserved;
    }

    /**
     * Mark this location as occupied by a player
     */
    public void occupy(UUID ownerUUID, String ownerName, UUID islandUUID) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.islandUUID = islandUUID;
        this.status = Status.OCCUPIED;
        this.reserved = false; // Clear any reservation
        this.blocked = false;
        this.purchasePrice = 0;
    }

    /**
     * Mark this location as occupied with multi-dimension support
     * @param ownerUUID The owner's UUID
     * @param ownerName The owner's name
     * @param dimensionIslands Map of dimension key to island UUID
     */
    public void occupy(UUID ownerUUID, String ownerName, Map<String, UUID> dimensionIslands) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.status = Status.OCCUPIED;
        this.reserved = false;
        this.blocked = false;
        this.purchasePrice = 0;

        // Set all dimension island UUIDs
        this.dimensionIslandUUIDs.clear();
        if (dimensionIslands != null) {
            this.dimensionIslandUUIDs.putAll(dimensionIslands);
            // Set legacy field from overworld
            UUID overworldIsland = dimensionIslands.get("overworld");
            this.islandUUID = overworldIsland;
        }
    }

    /**
     * Clear the occupation and make available
     */
    public void clear() {
        this.ownerUUID = null;
        this.ownerName = null;
        this.islandUUID = null;
        this.islandId = null;
        this.dimensionIslandUUIDs.clear();
        this.dimensionIslandIds.clear();
        this.status = Status.AVAILABLE;
    }

    // Multi-dimension support methods

    /**
     * Get the map of dimension island UUIDs
     * @return Map of dimension key to island UUID
     */
    public Map<String, UUID> getDimensionIslandUUIDs() {
        // Auto-migrate legacy field if present
        if (islandUUID != null && !dimensionIslandUUIDs.containsKey("overworld")) {
            dimensionIslandUUIDs.put("overworld", islandUUID);
        }
        return dimensionIslandUUIDs;
    }

    /**
     * Set the dimension island UUIDs map
     * @param dimensionIslandUUIDs Map of dimension key to island UUID
     */
    public void setDimensionIslandUUIDs(Map<String, UUID> dimensionIslandUUIDs) {
        this.dimensionIslandUUIDs = dimensionIslandUUIDs != null ? dimensionIslandUUIDs : new HashMap<>();
    }

    /**
     * Get the island UUID for a specific dimension
     * @param dimensionKey The dimension key (e.g., "overworld", "nether")
     * @return The island UUID, or null if not set
     */
    public UUID getIslandUUID(String dimensionKey) {
        // Auto-migrate legacy field if present
        if (islandUUID != null && !dimensionIslandUUIDs.containsKey("overworld")) {
            dimensionIslandUUIDs.put("overworld", islandUUID);
        }
        return dimensionIslandUUIDs.get(dimensionKey);
    }

    /**
     * Set the island UUID for a specific dimension
     * @param dimensionKey The dimension key
     * @param islandUUID The island UUID
     */
    public void setIslandUUID(String dimensionKey, UUID islandUUID) {
        if (islandUUID != null) {
            dimensionIslandUUIDs.put(dimensionKey, islandUUID);
        } else {
            dimensionIslandUUIDs.remove(dimensionKey);
        }
        // Also update legacy field if this is the overworld
        if ("overworld".equals(dimensionKey)) {
            this.islandUUID = islandUUID;
        }
    }

    /**
     * Get the map of dimension island IDs (BentoBox String IDs)
     * @return Map of dimension key to island ID
     */
    public Map<String, String> getDimensionIslandIds() {
        // Auto-migrate legacy field if present
        if (islandId != null && !dimensionIslandIds.containsKey("overworld")) {
            dimensionIslandIds.put("overworld", islandId);
        }
        return dimensionIslandIds;
    }

    /**
     * Set the dimension island IDs map
     * @param dimensionIslandIds Map of dimension key to island ID
     */
    public void setDimensionIslandIds(Map<String, String> dimensionIslandIds) {
        this.dimensionIslandIds = dimensionIslandIds != null ? dimensionIslandIds : new HashMap<>();
    }

    /**
     * Get the island ID for a specific dimension
     * @param dimensionKey The dimension key
     * @return The island ID, or null if not set
     */
    public String getIslandId(String dimensionKey) {
        // Auto-migrate legacy field if present
        if (islandId != null && !dimensionIslandIds.containsKey("overworld")) {
            dimensionIslandIds.put("overworld", islandId);
        }
        return dimensionIslandIds.get(dimensionKey);
    }

    /**
     * Set the island ID for a specific dimension
     * @param dimensionKey The dimension key
     * @param islandId The island ID
     */
    public void setIslandId(String dimensionKey, String islandId) {
        if (islandId != null) {
            dimensionIslandIds.put(dimensionKey, islandId);
        } else {
            dimensionIslandIds.remove(dimensionKey);
        }
        // Also update legacy field if this is the overworld
        if ("overworld".equals(dimensionKey)) {
            this.islandId = islandId;
        }
    }

    /**
     * Check if this location has an island in any dimension
     * @return true if at least one dimension has an island
     */
    public boolean hasAnyIsland() {
        if (!dimensionIslandUUIDs.isEmpty()) {
            return dimensionIslandUUIDs.values().stream()
                    .anyMatch(uuid -> uuid != null);
        }
        return islandUUID != null;
    }

    /**
     * Clear the island for a specific dimension
     * @param dimensionKey The dimension key
     */
    public void clearDimensionIsland(String dimensionKey) {
        dimensionIslandUUIDs.remove(dimensionKey);
        dimensionIslandIds.remove(dimensionKey);
        if ("overworld".equals(dimensionKey)) {
            this.islandUUID = null;
            this.islandId = null;
        }
    }
}
