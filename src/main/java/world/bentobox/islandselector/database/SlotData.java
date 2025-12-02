package world.bentobox.islandselector.database;

import com.google.gson.annotations.Expose;
import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.Table;

import java.util.UUID;

/**
 * Database object for storing player slot data
 * Each record represents one slot for a player
 */
@Table(name = "PlayerSlots")
public class SlotData implements DataObject {

    /**
     * Unique ID for this record - uses "playerUUID-slotNumber" format
     */
    @Expose
    private String uniqueId;

    /**
     * UUID of the player who owns this slot
     */
    @Expose
    private String playerUUID;

    /**
     * Slot number (1-5)
     */
    @Expose
    private int slotNumber;

    /**
     * Custom name for this slot (e.g., "Main Base", "Farm Island")
     */
    @Expose
    private String slotName;

    /**
     * Custom icon material name for this slot
     */
    @Expose
    private String iconMaterial;

    /**
     * UUID of the BSkyBlock island in this slot (null if empty)
     */
    @Expose
    private String islandUUID;

    /**
     * Grid coordinate where this slot's island is located (e.g., "0,0")
     * All slots for a player share the same grid location
     */
    @Expose
    private String gridCoordinate;

    /**
     * Whether this slot has an island
     */
    @Expose
    private boolean hasIsland = false;

    /**
     * Whether this is the currently active slot
     */
    @Expose
    private boolean active = false;

    /**
     * Timestamp of last switch (for cooldown tracking)
     */
    @Expose
    private long lastSwitchTime = 0;

    /**
     * Default constructor for database serialization
     */
    public SlotData() {
    }

    /**
     * Constructor for new slot
     */
    public SlotData(UUID playerUUID, int slotNumber) {
        this.playerUUID = playerUUID.toString();
        this.slotNumber = slotNumber;
        this.uniqueId = playerUUID.toString() + "-" + slotNumber;
        this.slotName = "Island " + slotNumber;
        this.iconMaterial = "GRASS_BLOCK";
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(String playerUUID) {
        this.playerUUID = playerUUID;
    }

    public UUID getPlayerUUIDAsUUID() {
        if (playerUUID == null || playerUUID.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(playerUUID);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID != null ? playerUUID.toString() : null;
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    public String getSlotName() {
        return slotName != null ? slotName : "Island " + slotNumber;
    }

    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    public String getIconMaterial() {
        return iconMaterial != null ? iconMaterial : "GRASS_BLOCK";
    }

    public void setIconMaterial(String iconMaterial) {
        this.iconMaterial = iconMaterial;
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

    public String getGridCoordinate() {
        return gridCoordinate;
    }

    public void setGridCoordinate(String gridCoordinate) {
        this.gridCoordinate = gridCoordinate;
    }

    public boolean hasIsland() {
        return hasIsland;
    }

    public void setHasIsland(boolean hasIsland) {
        this.hasIsland = hasIsland;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getLastSwitchTime() {
        return lastSwitchTime;
    }

    public void setLastSwitchTime(long lastSwitchTime) {
        this.lastSwitchTime = lastSwitchTime;
    }

    /**
     * Mark this slot as having an island
     */
    public void createIsland(UUID islandUUID, String gridCoordinate) {
        this.islandUUID = islandUUID != null ? islandUUID.toString() : null;
        this.gridCoordinate = gridCoordinate;
        this.hasIsland = true;
    }

    /**
     * Clear island from this slot
     */
    public void clearIsland() {
        this.islandUUID = null;
        this.hasIsland = false;
        // Keep gridCoordinate so we know where to place future islands
    }

    /**
     * Update last switch timestamp
     */
    public void updateSwitchTime() {
        this.lastSwitchTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "SlotData{" +
                "uniqueId='" + uniqueId + '\'' +
                ", playerUUID='" + playerUUID + '\'' +
                ", slotNumber=" + slotNumber +
                ", slotName='" + slotName + '\'' +
                ", hasIsland=" + hasIsland +
                ", active=" + active +
                '}';
    }
}
