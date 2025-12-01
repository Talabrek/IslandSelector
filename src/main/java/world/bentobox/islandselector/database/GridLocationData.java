package world.bentobox.islandselector.database;

import com.google.gson.annotations.Expose;
import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.Table;

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
     * UUID of the BSkyBlock island at this location
     */
    @Expose
    private String islandUUID;

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
     * Mark this location as occupied
     */
    public void occupy(UUID ownerUUID, String ownerName, UUID islandUUID) {
        this.ownerUUID = ownerUUID != null ? ownerUUID.toString() : null;
        this.ownerName = ownerName;
        this.islandUUID = islandUUID != null ? islandUUID.toString() : null;
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
