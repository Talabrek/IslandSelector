package world.bentobox.islandselector.database;

import com.google.gson.annotations.Expose;
import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.Table;

import java.util.HashMap;
import java.util.Map;
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
     * Blueprint bundle name used when creating this slot's island.
     * Used for blueprint-specific challenges integration.
     */
    @Expose
    private String blueprintBundle;

    /**
     * Serialized island homes for this slot (legacy single-dimension).
     * Maps home name to serialized location string "world;x;y;z;yaw;pitch"
     * Preserved when switching slots so each slot has its own homes.
     */
    @Expose
    private Map<String, String> serializedHomes;

    // Multi-dimension fields

    /**
     * Island UUIDs per dimension (dimension key -> island UUID string)
     * Used for multi-dimension support where each dimension has its own island
     */
    @Expose
    private Map<String, String> dimensionIslandUUIDs;

    /**
     * Blueprint bundle per dimension (dimension key -> blueprint name)
     * Allows different blueprints for each dimension
     */
    @Expose
    private Map<String, String> dimensionBlueprints;

    /**
     * Serialized homes per dimension (dimension key -> (home name -> location string))
     * Each dimension can have its own set of named homes
     */
    @Expose
    private Map<String, Map<String, String>> dimensionSerializedHomes;

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

    public String getBlueprintBundle() {
        return blueprintBundle;
    }

    public void setBlueprintBundle(String blueprintBundle) {
        this.blueprintBundle = blueprintBundle;
    }

    /**
     * Get the serialized island homes for this slot
     */
    public Map<String, String> getSerializedHomes() {
        return serializedHomes != null ? serializedHomes : new HashMap<>();
    }

    /**
     * Set the serialized island homes for this slot
     */
    public void setSerializedHomes(Map<String, String> serializedHomes) {
        this.serializedHomes = serializedHomes;
    }

    /**
     * Check if this slot has saved homes
     */
    public boolean hasSerializedHomes() {
        return serializedHomes != null && !serializedHomes.isEmpty();
    }

    /**
     * Mark this slot as having an island (legacy single-dimension)
     */
    public void createIsland(UUID islandUUID, String gridCoordinate) {
        this.islandUUID = islandUUID != null ? islandUUID.toString() : null;
        this.gridCoordinate = gridCoordinate;
        this.hasIsland = true;
        // Also add to dimension map for overworld
        if (islandUUID != null) {
            if (dimensionIslandUUIDs == null) {
                dimensionIslandUUIDs = new HashMap<>();
            }
            dimensionIslandUUIDs.put("overworld", islandUUID.toString());
        }
    }

    /**
     * Mark this slot as having islands in multiple dimensions
     * @param dimensionIslands Map of dimension key to island UUID
     * @param gridCoordinate The grid coordinate for all islands
     */
    public void createIslands(Map<String, UUID> dimensionIslands, String gridCoordinate) {
        this.gridCoordinate = gridCoordinate;
        this.hasIsland = true;

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
    }

    /**
     * Clear island from this slot
     */
    public void clearIsland() {
        this.islandUUID = null;
        if (dimensionIslandUUIDs != null) {
            dimensionIslandUUIDs.clear();
        }
        this.hasIsland = false;
        // Keep gridCoordinate so we know where to place future islands
    }

    /**
     * Update last switch timestamp
     */
    public void updateSwitchTime() {
        this.lastSwitchTime = System.currentTimeMillis();
    }

    // Multi-dimension getters and setters

    /**
     * Get the dimension island UUIDs map
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
     */
    public void setDimensionIslandUUIDs(Map<String, String> dimensionIslandUUIDs) {
        this.dimensionIslandUUIDs = dimensionIslandUUIDs != null ? dimensionIslandUUIDs : new HashMap<>();
    }

    /**
     * Get the island UUID for a specific dimension
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
     */
    public void setIslandUUID(String dimensionKey, UUID islandUUID) {
        setIslandUUID(dimensionKey, islandUUID != null ? islandUUID.toString() : null);
    }

    /**
     * Get the dimension blueprints map
     */
    public Map<String, String> getDimensionBlueprints() {
        if (dimensionBlueprints == null) {
            dimensionBlueprints = new HashMap<>();
        }
        // Auto-migrate legacy field if present
        if (blueprintBundle != null && !blueprintBundle.isEmpty() &&
                !dimensionBlueprints.containsKey("overworld")) {
            dimensionBlueprints.put("overworld", blueprintBundle);
        }
        return dimensionBlueprints;
    }

    /**
     * Set the dimension blueprints map
     */
    public void setDimensionBlueprints(Map<String, String> dimensionBlueprints) {
        this.dimensionBlueprints = dimensionBlueprints != null ? dimensionBlueprints : new HashMap<>();
    }

    /**
     * Get the blueprint for a specific dimension
     */
    public String getDimensionBlueprint(String dimensionKey) {
        Map<String, String> blueprints = getDimensionBlueprints();
        return blueprints.get(dimensionKey);
    }

    /**
     * Set the blueprint for a specific dimension
     */
    public void setDimensionBlueprint(String dimensionKey, String blueprint) {
        if (dimensionBlueprints == null) {
            dimensionBlueprints = new HashMap<>();
        }
        if (blueprint != null) {
            dimensionBlueprints.put(dimensionKey, blueprint);
        } else {
            dimensionBlueprints.remove(dimensionKey);
        }
        // Also update legacy field if this is the overworld
        if ("overworld".equals(dimensionKey)) {
            this.blueprintBundle = blueprint;
        }
    }

    /**
     * Get the dimension serialized homes map
     */
    public Map<String, Map<String, String>> getDimensionSerializedHomes() {
        if (dimensionSerializedHomes == null) {
            dimensionSerializedHomes = new HashMap<>();
        }
        // Auto-migrate legacy field if present
        if (serializedHomes != null && !serializedHomes.isEmpty() &&
                !dimensionSerializedHomes.containsKey("overworld")) {
            dimensionSerializedHomes.put("overworld", new HashMap<>(serializedHomes));
        }
        return dimensionSerializedHomes;
    }

    /**
     * Set the dimension serialized homes map
     */
    public void setDimensionSerializedHomes(Map<String, Map<String, String>> dimensionSerializedHomes) {
        this.dimensionSerializedHomes = dimensionSerializedHomes != null ?
                dimensionSerializedHomes : new HashMap<>();
    }

    /**
     * Get the serialized homes for a specific dimension
     */
    public Map<String, String> getDimensionHomes(String dimensionKey) {
        Map<String, Map<String, String>> allHomes = getDimensionSerializedHomes();
        Map<String, String> homes = allHomes.get(dimensionKey);
        return homes != null ? homes : new HashMap<>();
    }

    /**
     * Set the serialized homes for a specific dimension
     */
    public void setDimensionHomes(String dimensionKey, Map<String, String> homes) {
        if (dimensionSerializedHomes == null) {
            dimensionSerializedHomes = new HashMap<>();
        }
        if (homes != null && !homes.isEmpty()) {
            dimensionSerializedHomes.put(dimensionKey, homes);
        } else {
            dimensionSerializedHomes.remove(dimensionKey);
        }
        // Also update legacy field if this is the overworld
        if ("overworld".equals(dimensionKey)) {
            this.serializedHomes = homes;
        }
    }

    /**
     * Check if this slot has any islands in any dimension
     */
    public boolean hasAnyIsland() {
        if (dimensionIslandUUIDs != null && !dimensionIslandUUIDs.isEmpty()) {
            return dimensionIslandUUIDs.values().stream()
                    .anyMatch(uuid -> uuid != null && !uuid.isEmpty());
        }
        return islandUUID != null && !islandUUID.isEmpty();
    }

    /**
     * Migrate legacy single-dimension fields to multi-dimension maps
     */
    private void migrateFromLegacy() {
        if (islandUUID != null && !islandUUID.isEmpty() &&
                !dimensionIslandUUIDs.containsKey("overworld")) {
            dimensionIslandUUIDs.put("overworld", islandUUID);
        }
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
