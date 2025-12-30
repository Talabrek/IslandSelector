package world.bentobox.islandselector.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manager for handling player slots
 */
public class SlotManager {

    private final IslandSelector addon;
    private final Database<SlotData> database;
    private final Map<String, SlotData> slotCache; // Key: uniqueId (playerUUID-slotNumber)
    private final Map<UUID, Integer> pendingSlotCreations; // Track which slot a new island should go into
    private final Map<UUID, Integer> pendingSlotRestorations; // Track which slot a homeless player wants to restore

    public SlotManager(IslandSelector addon) {
        this.addon = addon;
        this.database = new Database<>(addon, SlotData.class);
        this.slotCache = new HashMap<>();
        this.pendingSlotCreations = new HashMap<>();
        this.pendingSlotRestorations = new HashMap<>();
        loadAllSlots();
        // Sync will be called after GridManager is ready - see IslandSelector.onEnable()
    }

    /**
     * Sync slot data with existing islands in the grid.
     * Creates slot 1 for any player who has an island but no slot data.
     * This handles pre-existing islands when the addon is first installed.
     */
    public void syncWithExistingIslands() {
        addon.log("Syncing slot data with existing islands...");

        GridManager gridManager = addon.getGridManager();
        if (gridManager == null) {
            addon.logWarning("GridManager not ready - cannot sync slots");
            return;
        }

        World bskyblockWorld = gridManager.getBSkyBlockWorld();
        if (bskyblockWorld == null) {
            addon.logWarning("BSkyBlock world not found - cannot sync slots");
            return;
        }

        int created = 0;
        int existing = 0;

        // Iterate through all players who have islands in the grid
        for (UUID playerUUID : gridManager.getAllOccupiedPlayerUUIDs()) {
            // Check if this player already has slot data
            List<SlotData> playerSlots = getPlayerSlots(playerUUID);

            if (playerSlots.isEmpty()) {
                // Player has island but no slot data - create slot 1
                String gridCoord = gridManager.getPlayerGridCoordinate(playerUUID);

                // Get the island UUID from BentoBox
                Island island = addon.getIslands().getIsland(bskyblockWorld, playerUUID);
                UUID islandUUID = null;
                if (island != null) {
                    try {
                        islandUUID = UUID.fromString(island.getUniqueId());
                    } catch (IllegalArgumentException e) {
                        // Not a valid UUID format
                    }
                }

                // Create slot 1 and mark it as active
                SlotData slot1 = createOrGetSlot(playerUUID, 1);
                slot1.createIsland(islandUUID, gridCoord);
                slot1.setActive(true);
                saveSlot(slot1);

                String playerName = Bukkit.getOfflinePlayer(playerUUID).getName();
                addon.log("Created slot 1 for existing island owner: " + (playerName != null ? playerName : playerUUID) + " at " + gridCoord);
                created++;
            } else {
                existing++;
            }
        }

        addon.log("Slot sync complete: " + created + " slots created, " + existing + " already existed");
    }

    /**
     * Load all slots from database into cache
     */
    private void loadAllSlots() {
        slotCache.clear();
        List<SlotData> allSlots = database.loadObjects();
        for (SlotData slot : allSlots) {
            slotCache.put(slot.getUniqueId(), slot);
        }
        addon.log("Loaded " + allSlots.size() + " player slots from database");
    }

    /**
     * Get all slots for a player
     */
    public List<SlotData> getPlayerSlots(UUID playerUUID) {
        return slotCache.values().stream()
                .filter(slot -> playerUUID.equals(slot.getPlayerUUIDAsUUID()))
                .sorted(Comparator.comparingInt(SlotData::getSlotNumber))
                .collect(Collectors.toList());
    }

    /**
     * Get a specific slot for a player
     */
    public SlotData getSlot(UUID playerUUID, int slotNumber) {
        String uniqueId = playerUUID.toString() + "-" + slotNumber;
        return slotCache.get(uniqueId);
    }

    /**
     * Get the active slot for a player
     */
    public SlotData getActiveSlot(UUID playerUUID) {
        return slotCache.values().stream()
                .filter(slot -> playerUUID.equals(slot.getPlayerUUIDAsUUID()))
                .filter(SlotData::isActive)
                .findFirst()
                .orElse(null);
    }

    /**
     * Create or get a slot for a player
     */
    public SlotData createOrGetSlot(UUID playerUUID, int slotNumber) {
        SlotData slot = getSlot(playerUUID, slotNumber);
        if (slot == null) {
            slot = new SlotData(playerUUID, slotNumber);
            saveSlot(slot);
        }
        return slot;
    }

    /**
     * Initialize slots for a new player
     * Creates slot 1 as active when player creates their first island
     */
    public void initializePlayerSlots(UUID playerUUID, UUID islandUUID, String gridCoordinate) {
        initializePlayerSlots(playerUUID, islandUUID != null ? islandUUID.toString() : null, gridCoordinate);
    }

    /**
     * Initialize slots for a new player with a string island ID
     * Creates slot 1 as active when player creates their first island
     * @param playerUUID The player's UUID
     * @param islandId The full BentoBox island ID (e.g., "BSkyBlock6d68f389-...")
     * @param gridCoordinate The grid coordinate string
     */
    public void initializePlayerSlots(UUID playerUUID, String islandId, String gridCoordinate) {
        SlotData slot1 = createOrGetSlot(playerUUID, 1);
        slot1.setIslandUUID(islandId);
        slot1.setGridCoordinate(gridCoordinate);
        slot1.setHasIsland(true);
        slot1.setActive(true);
        saveSlot(slot1);
    }

    /**
     * Check if a player has any islands
     */
    public boolean hasAnyIsland(UUID playerUUID) {
        return slotCache.values().stream()
                .anyMatch(slot -> playerUUID.equals(slot.getPlayerUUIDAsUUID()) && slot.hasIsland());
    }

    /**
     * Get the number of slots with islands for a player
     */
    public int getIslandCount(UUID playerUUID) {
        return (int) slotCache.values().stream()
                .filter(slot -> playerUUID.equals(slot.getPlayerUUIDAsUUID()))
                .filter(SlotData::hasIsland)
                .count();
    }

    /**
     * Create an island in an empty slot
     */
    public void createIslandInSlot(UUID playerUUID, int slotNumber, UUID islandUUID) {
        SlotData slot = createOrGetSlot(playerUUID, slotNumber);

        // Get grid coordinate from first slot (all slots share same location)
        String gridCoordinate = getPlayerGridCoordinate(playerUUID);

        slot.createIsland(islandUUID, gridCoordinate);
        saveSlot(slot);
    }

    /**
     * Get the grid coordinate for a player's islands (all slots share same location)
     */
    public String getPlayerGridCoordinate(UUID playerUUID) {
        return slotCache.values().stream()
                .filter(slot -> playerUUID.equals(slot.getPlayerUUIDAsUUID()))
                .filter(SlotData::hasIsland)
                .map(SlotData::getGridCoordinate)
                .findFirst()
                .orElse(null);
    }

    /**
     * Switch active slot for a player
     * Returns true if switch was successful
     */
    public boolean switchSlot(UUID playerUUID, int targetSlotNumber) {
        SlotData currentActive = getActiveSlot(playerUUID);
        SlotData targetSlot = getSlot(playerUUID, targetSlotNumber);

        if (targetSlot == null || !targetSlot.hasIsland()) {
            return false;
        }

        // Deactivate current slot
        if (currentActive != null) {
            currentActive.setActive(false);
            currentActive.updateSwitchTime();
            saveSlot(currentActive);
        }

        // Activate target slot
        targetSlot.setActive(true);
        targetSlot.updateSwitchTime();
        saveSlot(targetSlot);

        return true;
    }

    /**
     * Delete a slot and its island
     */
    public void deleteSlot(UUID playerUUID, int slotNumber) {
        String uniqueId = playerUUID.toString() + "-" + slotNumber;
        SlotData slot = slotCache.get(uniqueId);

        if (slot != null) {
            slotCache.remove(uniqueId);
            database.deleteID(uniqueId);
        }
    }

    /**
     * Update slot name
     */
    public void setSlotName(UUID playerUUID, int slotNumber, String name) {
        SlotData slot = getSlot(playerUUID, slotNumber);
        if (slot != null) {
            slot.setSlotName(name);
            saveSlot(slot);
        }
    }

    /**
     * Update slot icon
     */
    public void setSlotIcon(UUID playerUUID, int slotNumber, String materialName) {
        SlotData slot = getSlot(playerUUID, slotNumber);
        if (slot != null) {
            slot.setIconMaterial(materialName);
            saveSlot(slot);
        }
    }

    /**
     * Update slot blueprint bundle name
     * Used for blueprint-specific challenges integration
     */
    public void setBlueprintBundle(UUID playerUUID, int slotNumber, String blueprintBundle) {
        SlotData slot = getSlot(playerUUID, slotNumber);
        if (slot != null) {
            slot.setBlueprintBundle(blueprintBundle);
            saveSlot(slot);
        }
    }

    /**
     * Get the blueprint bundle name for a slot
     */
    public String getBlueprintBundle(UUID playerUUID, int slotNumber) {
        SlotData slot = getSlot(playerUUID, slotNumber);
        return slot != null ? slot.getBlueprintBundle() : null;
    }

    /**
     * Check if player can switch slots (cooldown check)
     */
    public boolean canSwitch(UUID playerUUID) {
        SlotData activeSlot = getActiveSlot(playerUUID);
        if (activeSlot == null) {
            return true;
        }

        long cooldown = addon.getSettings().getSwitchCooldown() * 1000L; // Convert to milliseconds
        long timeSinceSwitch = System.currentTimeMillis() - activeSlot.getLastSwitchTime();

        return timeSinceSwitch >= cooldown;
    }

    /**
     * Get remaining cooldown time in seconds
     */
    public long getRemainingCooldown(UUID playerUUID) {
        SlotData activeSlot = getActiveSlot(playerUUID);
        if (activeSlot == null) {
            return 0;
        }

        long cooldown = addon.getSettings().getSwitchCooldown() * 1000L;
        long timeSinceSwitch = System.currentTimeMillis() - activeSlot.getLastSwitchTime();
        long remaining = cooldown - timeSinceSwitch;

        return remaining > 0 ? remaining / 1000 : 0; // Return seconds
    }

    /**
     * Save a slot to database
     */
    public void saveSlot(SlotData slot) {
        slotCache.put(slot.getUniqueId(), slot);
        database.saveObjectAsync(slot);
    }

    /**
     * Get slot schematic file path
     */
    public String getSlotSchematicPath(UUID playerUUID, int slotNumber) {
        return addon.getDataFolder().getAbsolutePath() + "/slots/" + playerUUID.toString() + "/slot-" + slotNumber + ".schem";
    }

    /**
     * Reset switch cooldown for a player (admin command)
     */
    public void resetSwitchCooldown(UUID playerUUID) {
        SlotData activeSlot = getActiveSlot(playerUUID);
        if (activeSlot != null) {
            // Set last switch time to 0, which makes cooldown expired
            activeSlot.setLastSwitchTime(0);
            saveSlot(activeSlot);
        }
    }

    /**
     * Reload all slots from database
     */
    public void reload() {
        loadAllSlots();
    }

    /**
     * Get all player UUIDs that have slot data
     */
    public Set<UUID> getAllPlayersWithSlots() {
        return slotCache.values().stream()
                .map(SlotData::getPlayerUUIDAsUUID)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Purge all slot data for a player
     * Removes database entries and deletes schematic files
     * Should only be called for offline players
     * @return true if purge was successful
     */
    public boolean purgePlayerData(UUID playerUUID) {
        List<SlotData> slots = getPlayerSlots(playerUUID);

        if (slots.isEmpty()) {
            return false;
        }

        boolean success = true;

        // Delete all slots from database and cache
        for (SlotData slot : slots) {
            String uniqueId = slot.getUniqueId();
            slotCache.remove(uniqueId);
            database.deleteID(uniqueId);

            // Delete schematic file
            File schematicFile = new File(getSlotSchematicPath(playerUUID, slot.getSlotNumber()));
            if (schematicFile.exists()) {
                if (!schematicFile.delete()) {
                    addon.logError("Failed to delete schematic file: " + schematicFile.getAbsolutePath());
                    success = false;
                }
            }
        }

        // Delete player's slot directory if it's empty
        File playerSlotDir = new File(addon.getDataFolder(), "slots/" + playerUUID.toString());
        if (playerSlotDir.exists() && playerSlotDir.isDirectory()) {
            String[] files = playerSlotDir.list();
            if (files != null && files.length == 0) {
                playerSlotDir.delete();
            }
        }

        return success;
    }

    /**
     * Get the total number of schematic files for a player
     */
    public int countPlayerSchematics(UUID playerUUID) {
        File playerSlotDir = new File(addon.getDataFolder(), "slots/" + playerUUID.toString());
        if (!playerSlotDir.exists() || !playerSlotDir.isDirectory()) {
            return 0;
        }

        String[] files = playerSlotDir.list((dir, name) -> name.endsWith(".schem"));
        return files != null ? files.length : 0;
    }

    /**
     * Set pending slot creation - called when player clicks empty slot in slot GUI
     * The next island they create will be associated with this slot number
     */
    public void setPendingSlotCreation(UUID playerUUID, int slotNumber) {
        pendingSlotCreations.put(playerUUID, slotNumber);
        addon.log("Set pending slot creation for " + playerUUID + " to slot " + slotNumber);
    }

    /**
     * Get and clear pending slot creation for a player
     * Returns null if no pending creation, otherwise returns the slot number
     */
    public Integer getPendingSlotCreation(UUID playerUUID) {
        return pendingSlotCreations.remove(playerUUID);
    }

    /**
     * Check if player has a pending slot creation
     */
    public boolean hasPendingSlotCreation(UUID playerUUID) {
        return pendingSlotCreations.containsKey(playerUUID);
    }

    /**
     * Clear pending slot creation without returning it
     */
    public void clearPendingSlotCreation(UUID playerUUID) {
        pendingSlotCreations.remove(playerUUID);
    }

    /**
     * Initialize a specific slot for a player when they create an island in that slot
     * @param playerUUID The player's UUID
     * @param slotNumber The slot number to initialize
     * @param islandUUID The island UUID (can be null if island ID is not a UUID)
     * @param gridCoordinate The grid coordinate string
     */
    public void initializeSlot(UUID playerUUID, int slotNumber, UUID islandUUID, String gridCoordinate) {
        SlotData slot = createOrGetSlot(playerUUID, slotNumber);
        slot.createIsland(islandUUID, gridCoordinate);

        // If this is the first slot with an island, make it active
        // Otherwise keep the current active slot
        SlotData currentActive = getActiveSlot(playerUUID);
        if (currentActive == null) {
            slot.setActive(true);
        }

        saveSlot(slot);
        addon.log("Initialized slot " + slotNumber + " for " + playerUUID + " at " + gridCoordinate);
    }

    /**
     * Set pending slot restoration - called when a homeless player selects a slot to restore
     * The next location they select will be used to restore this slot
     */
    public void setPendingSlotRestoration(UUID playerUUID, int slotNumber) {
        pendingSlotRestorations.put(playerUUID, slotNumber);
        addon.log("Set pending slot restoration for " + playerUUID + " to slot " + slotNumber);
    }

    /**
     * Get and clear pending slot restoration for a player
     * Returns null if no pending restoration, otherwise returns the slot number
     */
    public Integer getPendingSlotRestoration(UUID playerUUID) {
        return pendingSlotRestorations.remove(playerUUID);
    }

    /**
     * Check if player has a pending slot restoration
     */
    public boolean hasPendingSlotRestoration(UUID playerUUID) {
        return pendingSlotRestorations.containsKey(playerUUID);
    }

    /**
     * Clear pending slot restoration without returning it
     */
    public void clearPendingSlotRestoration(UUID playerUUID) {
        pendingSlotRestorations.remove(playerUUID);
    }

    /**
     * Restore a slot after the player has selected a location.
     * This updates the slot data to mark it as having an island again.
     *
     * @param playerUUID The player's UUID
     * @param slotNumber The slot number being restored
     * @param islandUUID The new island UUID from BentoBox
     * @param gridCoordinate The grid coordinate where the island is placed
     */
    public void restoreSlot(UUID playerUUID, int slotNumber, UUID islandUUID, String gridCoordinate) {
        SlotData slot = getSlot(playerUUID, slotNumber);
        if (slot == null) {
            addon.logError("Cannot restore slot " + slotNumber + " - slot data not found for " + playerUUID);
            return;
        }

        slot.createIsland(islandUUID, gridCoordinate);
        slot.setActive(true);

        // Deactivate other slots
        for (SlotData otherSlot : getPlayerSlots(playerUUID)) {
            if (otherSlot.getSlotNumber() != slotNumber && otherSlot.isActive()) {
                otherSlot.setActive(false);
                saveSlot(otherSlot);
            }
        }

        saveSlot(slot);
        addon.log("Restored slot " + slotNumber + " for " + playerUUID + " at " + gridCoordinate);
    }

    /**
     * Save island homes as RELATIVE offsets from island center.
     * This allows homes to be correctly restored even if the island moves.
     *
     * @param playerUUID The player's UUID
     * @param slotNumber The slot number
     * @param island The island to get homes from
     */
    public void saveIslandHomes(UUID playerUUID, int slotNumber, Island island) {
        SlotData slot = getSlot(playerUUID, slotNumber);
        if (slot == null || island == null) {
            return;
        }

        Map<String, Location> homes = island.getHomes();
        if (homes == null || homes.isEmpty()) {
            slot.setSerializedHomes(null);
            saveSlot(slot);
            return;
        }

        Location islandCenter = island.getCenter();
        if (islandCenter == null) {
            addon.logWarning("Cannot save homes - island has no center");
            return;
        }

        Map<String, String> serialized = new HashMap<>();
        for (Map.Entry<String, Location> entry : homes.entrySet()) {
            String serializedLoc = serializeLocationAsOffset(entry.getValue(), islandCenter);
            if (serializedLoc != null) {
                serialized.put(entry.getKey(), serializedLoc);
            }
        }

        slot.setSerializedHomes(serialized);
        saveSlot(slot);
        addon.log("Saved " + serialized.size() + " island homes as relative offsets for " + playerUUID + " slot " + slotNumber);
    }

    /**
     * Restore island homes from slot data.
     * Homes are stored as relative offsets and converted to absolute coordinates
     * based on the current island center.
     *
     * @param playerUUID The player's UUID
     * @param slotNumber The slot number
     * @param island The island to restore homes to
     */
    public void restoreIslandHomes(UUID playerUUID, int slotNumber, Island island) {
        if (island == null) {
            return;
        }

        // ALWAYS clear existing homes first to prevent cross-slot contamination
        Map<String, Location> existingHomes = island.getHomes();
        if (existingHomes != null && !existingHomes.isEmpty()) {
            // Get all home names and remove them
            List<String> homeNames = new ArrayList<>(existingHomes.keySet());
            for (String homeName : homeNames) {
                island.removeHome(homeName);
            }
            addon.log("Cleared " + homeNames.size() + " existing homes before restoration");
        }

        SlotData slot = getSlot(playerUUID, slotNumber);
        if (slot == null || !slot.hasSerializedHomes()) {
            addon.log("No saved homes to restore for " + playerUUID + " slot " + slotNumber);
            return;
        }

        Location islandCenter = island.getCenter();
        if (islandCenter == null) {
            addon.logWarning("Cannot restore homes - island has no center");
            return;
        }

        Map<String, String> serialized = slot.getSerializedHomes();
        int restoredCount = 0;

        for (Map.Entry<String, String> entry : serialized.entrySet()) {
            Location loc = deserializeLocationFromOffset(entry.getValue(), islandCenter);
            if (loc != null) {
                island.addHome(entry.getKey(), loc);
                restoredCount++;
            }
        }

        addon.log("Restored " + restoredCount + " island homes for " + playerUUID + " slot " + slotNumber);
    }

    /**
     * Serialize a Location as a RELATIVE offset from island center.
     * Format: "offsetX;offsetY;offsetZ;yaw;pitch"
     */
    private String serializeLocationAsOffset(Location loc, Location islandCenter) {
        if (loc == null || loc.getWorld() == null || islandCenter == null) {
            return null;
        }
        // Calculate offset from island center
        double offsetX = loc.getX() - islandCenter.getX();
        double offsetY = loc.getY() - islandCenter.getY();
        double offsetZ = loc.getZ() - islandCenter.getZ();

        return offsetX + ";" +
               offsetY + ";" +
               offsetZ + ";" +
               loc.getYaw() + ";" +
               loc.getPitch();
    }

    /**
     * Deserialize a Location from relative offset format.
     * Format: "offsetX;offsetY;offsetZ;yaw;pitch"
     * Converts to absolute coordinates based on current island center.
     */
    private Location deserializeLocationFromOffset(String serialized, Location islandCenter) {
        if (serialized == null || serialized.isEmpty() || islandCenter == null) {
            return null;
        }

        String[] parts = serialized.split(";");

        // Handle both old absolute format (6 parts with world) and new relative format (5 parts)
        try {
            if (parts.length >= 6) {
                // Old absolute format: "world;x;y;z;yaw;pitch" - convert to relative
                // Check if first part looks like a world name (not a number)
                try {
                    Double.parseDouble(parts[0]);
                    // First part is a number, this is new format with 6 parts (shouldn't happen)
                } catch (NumberFormatException e) {
                    // First part is world name - old absolute format
                    // Just use as-is for backwards compatibility, but log warning
                    World world = islandCenter.getWorld();
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    double z = Double.parseDouble(parts[3]);
                    float yaw = Float.parseFloat(parts[4]);
                    float pitch = Float.parseFloat(parts[5]);

                    addon.logWarning("Found old absolute home format - will be converted to relative on next save");
                    return new Location(world, x, y, z, yaw, pitch);
                }
            }

            if (parts.length < 5) {
                return null;
            }

            // New relative offset format: "offsetX;offsetY;offsetZ;yaw;pitch"
            double offsetX = Double.parseDouble(parts[0]);
            double offsetY = Double.parseDouble(parts[1]);
            double offsetZ = Double.parseDouble(parts[2]);
            float yaw = Float.parseFloat(parts[3]);
            float pitch = Float.parseFloat(parts[4]);

            // Convert offset to absolute coordinates
            double x = islandCenter.getX() + offsetX;
            double y = islandCenter.getY() + offsetY;
            double z = islandCenter.getZ() + offsetZ;

            return new Location(islandCenter.getWorld(), x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            addon.logError("Failed to deserialize home offset: " + serialized);
            return null;
        }
    }
}
