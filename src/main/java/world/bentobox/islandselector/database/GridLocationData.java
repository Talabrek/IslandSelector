package world.bentobox.islandselector.database;

import com.google.gson.annotations.Expose;
import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.Table;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Database object for storing grid location data
 * Each record represents one grid location and its status
 */
@Table(name = "GridLocations")
public class GridLocationData implements DataObject {

    /**
     * Unique ID for this record - uses coordinate string (e.g., "A1", "B7")
     */
    @Expose
    private String uniqueId;

    /**
     * Column index (0-indexed)
     */
    @Expose
    private int column;

    /**
     * Row index (0-indexed)
     */
    @Expose
    private int row;

    /**
     * Status of this location: AVAILABLE, OCCUPIED, RESERVED, LOCKED
     */
    @Expose
    private String status = "AVAILABLE";

    /**
     * UUID of the island owner (null if not occupied)
     */
    @Expose
    private String ownerUUID;

    /**
     * Name of the island owner
     */
    @Expose
    private String ownerName;

    /**
     * UUID of the BSkyBlock island at this location (legacy single-dimension field)
     * For backwards compatibility - new code should use dimensionIslandUUIDs
     */
    @Expose
    private String islandUUID;

    /**
     * Island UUIDs per dimension (dimension key -> island UUID string)
     * Used for multi-dimension support where each dimension has its own island
     */
    @Expose
    private Map<String, String> dimensionIslandUUIDs = new HashMap<>();

    /**
     * Whether this location is reserved by admin
     */
    @Expose
    private boolean reserved = false;

    /**
     * Whether reserved location is blocked (not purchasable)
     */
    @Expose
    private boolean blocked = false;

    /**
     * Purchase price for reserved purchasable locations
     */
    @Expose
    private double purchasePrice = 0.0;

    /**
     * Default constructor for database serialization
     */
    public GridLocationData() {
    }

    /**
     * Constructor with coordinate
     */
    public GridLocationData(String coordString, int column, int row) {
        this.uniqueId = coordString;
        this.column = column;
        this.row = row;
        this.status = "AVAILABLE";
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(String ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public UUID getOwnerUUIDAsUUID() {
        if (ownerUUID == null || ownerUUID.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(ownerUUID);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID != null ? ownerUUID.toString() : null;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getIslandUUID() {
        return islandUUID;
    }

    public void setIslandUUID(String islandUUID) {
        this.islandUUID = islandUUID;
    }

    public UUID getIslandUUIDAsUUID() {
        if (islandUUID == null || islandUUID.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(islandUUID);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setIslandUUID(UUID islandUUID) {
        this.islandUUID = islandUUID != null ? islandUUID.toString() : null;
    }

    // Multi-dimension island UUID methods

    /**
     * Get the map of dimension island UUIDs
     * @return Map of dimension key to island UUID string
     */
    public Map<String, String> getDimensionIslandUUIDs() {
        if (dimensionIslandUUIDs == null) {
            dimensionIslandUUIDs = new HashMap<>();
        }
        // Auto-migrate legacy field if present
        migrateFromLegacy();
        return dimensionIslandUUIDs;
    }

    /**
     * Set the dimension island UUIDs map
     * @param dimensionIslandUUIDs Map of dimension key to island UUID string
     */
    public void setDimensionIslandUUIDs(Map<String, String> dimensionIslandUUIDs) {
        this.dimensionIslandUUIDs = dimensionIslandUUIDs != null ? dimensionIslandUUIDs : new HashMap<>();
    }

    /**
     * Get the island UUID for a specific dimension
     * @param dimensionKey The dimension key (e.g., "overworld", "nether")
     * @return The island UUID string, or null if not set
     */
    public String getIslandUUID(String dimensionKey) {
        if (dimensionIslandUUIDs == null) {
            dimensionIslandUUIDs = new HashMap<>();
        }
        migrateFromLegacy();
        return dimensionIslandUUIDs.get(dimensionKey);
    }

    /**
     * Set the island UUID for a specific dimension
     * @param dimensionKey The dimension key
     * @param islandUUID The island UUID string
     */
    public void setIslandUUID(String dimensionKey, String islandUUID) {
        if (dimensionIslandUUIDs == null) {
            dimensionIslandUUIDs = new HashMap<>();
        }
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
     * Set the island UUID for a specific dimension using UUID object
     * @param dimensionKey The dimension key
     * @param islandUUID The island UUID
     */
    public void setIslandUUID(String dimensionKey, UUID islandUUID) {
        setIslandUUID(dimensionKey, islandUUID != null ? islandUUID.toString() : null);
    }

    /**
     * Get the island UUID for a specific dimension as UUID object
     * @param dimensionKey The dimension key
     * @return The island UUID, or null if not set or invalid
     */
    public UUID getIslandUUIDAsUUID(String dimensionKey) {
        String uuid = getIslandUUID(dimensionKey);
        if (uuid == null || uuid.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Clear the island UUID for a specific dimension
     * @param dimensionKey The dimension key
     */
    public void clearIslandUUID(String dimensionKey) {
        if (dimensionIslandUUIDs != null) {
            dimensionIslandUUIDs.remove(dimensionKey);
        }
        if ("overworld".equals(dimensionKey)) {
            this.islandUUID = null;
        }
    }

    /**
     * Check if this location has an island in any dimension
     * @return true if at least one dimension has an island
     */
    public boolean hasAnyIsland() {
        if (dimensionIslandUUIDs != null && !dimensionIslandUUIDs.isEmpty()) {
            return dimensionIslandUUIDs.values().stream()
                    .anyMatch(uuid -> uuid != null && !uuid.isEmpty());
        }
        return islandUUID != null && !islandUUID.isEmpty();
    }

    /**
     * Migrate legacy single islandUUID to dimension map
     * Called automatically when accessing dimension methods
     */
    private void migrateFromLegacy() {
        if (islandUUID != null && !islandUUID.isEmpty() &&
                !dimensionIslandUUIDs.containsKey("overworld")) {
            dimensionIslandUUIDs.put("overworld", islandUUID);
        }
    }

    public boolean isReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    /**
     * Check if this location is purchasable
     */
    public boolean isPurchasable() {
        return reserved && !blocked && purchasePrice > 0;
    }

    /**
     * Check if this location is available for claiming
     */
    public boolean isAvailable() {
        return "AVAILABLE".equals(status) && !reserved;
    }

    /**
     * Mark this location as occupied (legacy single-dimension)
     */
    public void occupy(UUID ownerUUID, String ownerName, UUID islandUUID) {
        this.ownerUUID = ownerUUID != null ? ownerUUID.toString() : null;
        this.ownerName = ownerName;
        this.islandUUID = islandUUID != null ? islandUUID.toString() : null;
        // Also set in dimension map for overworld
        if (islandUUID != null) {
            if (dimensionIslandUUIDs == null) {
                dimensionIslandUUIDs = new HashMap<>();
            }
            dimensionIslandUUIDs.put("overworld", islandUUID.toString());
        }
        this.status = "OCCUPIED";
        this.reserved = false;
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
        this.ownerUUID = ownerUUID != null ? ownerUUID.toString() : null;
        this.ownerName = ownerName;

        // Set all dimension island UUIDs
        if (dimensionIslandUUIDs == null) {
            dimensionIslandUUIDs = new HashMap<>();
        }
        dimensionIslandUUIDs.clear();

        if (dimensionIslands != null) {
            for (Map.Entry<String, UUID> entry : dimensionIslands.entrySet()) {
                if (entry.getValue() != null) {
                    dimensionIslandUUIDs.put(entry.getKey(), entry.getValue().toString());
                }
            }
            // Set legacy field from overworld
            UUID overworldIsland = dimensionIslands.get("overworld");
            this.islandUUID = overworldIsland != null ? overworldIsland.toString() : null;
        }

        this.status = "OCCUPIED";
        this.reserved = false;
        this.blocked = false;
        this.purchasePrice = 0;
    }

    /**
     * Clear the occupation and make available
     */
    public void clear() {
        this.ownerUUID = null;
        this.ownerName = null;
        this.islandUUID = null;
        if (dimensionIslandUUIDs != null) {
            dimensionIslandUUIDs.clear();
        }
        this.status = "AVAILABLE";
    }

    @Override
    public String toString() {
        return "GridLocationData{" +
                "uniqueId='" + uniqueId + '\'' +
                ", column=" + column +
                ", row=" + row +
                ", status='" + status + '\'' +
                ", ownerName='" + ownerName + '\'' +
                ", reserved=" + reserved +
                '}';
    }
}
