package world.bentobox.islandselector.managers;

import org.bukkit.Bukkit;
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

    public SlotManager(IslandSelector addon) {
        this.addon = addon;
        this.database = new Database<>(addon, SlotData.class);
        this.slotCache = new HashMap<>();
        loadAllSlots();
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
                .filter(slot -> slot.getPlayerUUIDAsUUID().equals(playerUUID))
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
                .filter(slot -> slot.getPlayerUUIDAsUUID().equals(playerUUID))
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
        SlotData slot1 = createOrGetSlot(playerUUID, 1);
        slot1.createIsland(islandUUID, gridCoordinate);
        slot1.setActive(true);
        saveSlot(slot1);
    }

    /**
     * Check if a player has any islands
     */
    public boolean hasAnyIsland(UUID playerUUID) {
        return slotCache.values().stream()
                .anyMatch(slot -> slot.getPlayerUUIDAsUUID().equals(playerUUID) && slot.hasIsland());
    }

    /**
     * Get the number of slots with islands for a player
     */
    public int getIslandCount(UUID playerUUID) {
        return (int) slotCache.values().stream()
                .filter(slot -> slot.getPlayerUUIDAsUUID().equals(playerUUID))
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
                .filter(slot -> slot.getPlayerUUIDAsUUID().equals(playerUUID))
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
}
