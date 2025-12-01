package world.bentobox.islandselector.models;

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
    private UUID islandUUID;
    private boolean reserved;
    private boolean blocked;     // If reserved, is it blocked (not purchasable)?
    private double purchasePrice; // If reserved and purchasable, the price

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
     * Clear the occupation and make available
     */
    public void clear() {
        this.ownerUUID = null;
        this.ownerName = null;
        this.islandUUID = null;
        this.status = Status.AVAILABLE;
    }
}
