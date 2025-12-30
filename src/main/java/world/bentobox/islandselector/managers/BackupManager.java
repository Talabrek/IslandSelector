package world.bentobox.islandselector.managers;

import org.bukkit.Location;
import org.bukkit.World;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Manager for handling island backups
 * Creates schematic backups of player island slots
 */
public class BackupManager {

    private final IslandSelector addon;
    private final File backupDir;

    public BackupManager(IslandSelector addon) {
        this.addon = addon;
        this.backupDir = new File(addon.getDataFolder(), "backups");

        // Create backup directory if it doesn't exist
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
    }

    /**
     * Create a backup of a specific slot for a player
     * @param playerUUID The player's UUID
     * @param slotNumber The slot number to backup (1-based)
     * @return true if backup was successful, false otherwise
     */
    public boolean createBackup(UUID playerUUID, int slotNumber) {
        SlotData slotData = addon.getSlotManager().getSlot(playerUUID, slotNumber);

        if (slotData == null) {
            addon.logError("Slot " + slotNumber + " not found for player " + playerUUID);
            return false;
        }

        // We no longer require slotData.getIslandUUID() - we get island directly by player UUID
        return saveSlotToBackup(slotData, playerUUID, slotNumber);
    }

    /**
     * Save a slot as a backup schematic file
     */
    private boolean saveSlotToBackup(SlotData slotData, UUID playerUUID, int slotNumber) {
        try {
            // Get island directly by player UUID (not by slot's stored island UUID)
            World bskyblockWorld = addon.getGridManager().getBSkyBlockWorld();
            if (bskyblockWorld == null) {
                addon.logError("BSkyBlock world not available for backup");
                return false;
            }

            Island island = addon.getIslands().getIsland(bskyblockWorld, playerUUID);
            if (island == null) {
                addon.logError("Island not found for player: " + playerUUID + " in slot: " + slotData.getUniqueId());
                return false;
            }

            // Get island bounds
            Location center = island.getCenter();
            World world = center.getWorld();
            if (world == null) {
                addon.logError("Island world is null for slot: " + slotData.getUniqueId());
                return false;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            // Get backup file path
            File backupFile = getBackupFile(playerUUID, slotNumber);

            // Use SchematicUtils for safe entity handling
            boolean success = addon.getSchematicUtils().copyAndSave(center, range, false, backupFile);

            if (success) {
                addon.log("Backup created for player " + playerUUID + " slot " + slotNumber + ": " + backupFile.getName());
            } else {
                return false;
            }

            // Clean up old backups if configured
            cleanOldBackups(playerUUID, slotNumber);

            return true;

        } catch (Exception e) {
            addon.logError("Failed to create backup for player " + playerUUID + " slot " + slotNumber + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get the backup file path for a specific slot
     * Format: backups/{player-uuid}/slot-{number}-{timestamp}.schem
     */
    private File getBackupFile(UUID playerUUID, int slotNumber) {
        File playerDir = new File(backupDir, playerUUID.toString());

        // Create timestamp for unique filename
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String timestamp = dateFormat.format(new Date());

        String filename = "slot-" + slotNumber + "-" + timestamp + ".schem";
        return new File(playerDir, filename);
    }

    /**
     * Get the auto-backup file path for a specific slot.
     * This is a single rolling backup that gets overwritten each interval.
     * Format: backups/{player-uuid}/slot-{number}-auto.schem
     */
    private File getAutoBackupFile(UUID playerUUID, int slotNumber) {
        File playerDir = new File(backupDir, playerUUID.toString());
        String filename = "slot-" + slotNumber + "-auto.schem";
        return new File(playerDir, filename);
    }

    /**
     * Create an auto-backup of a specific slot for a player.
     * This creates a single rolling backup file that gets overwritten each time.
     *
     * @param playerUUID The player's UUID
     * @param slotNumber The slot number to backup (1-based)
     * @return true if backup was successful, false otherwise
     */
    public boolean createAutoBackup(UUID playerUUID, int slotNumber) {
        SlotData slotData = addon.getSlotManager().getSlot(playerUUID, slotNumber);

        if (slotData == null || !slotData.hasIsland()) {
            return false;
        }

        return saveSlotToAutoBackup(slotData, playerUUID, slotNumber);
    }

    /**
     * Save a slot as an auto-backup schematic file (single rolling backup)
     */
    private boolean saveSlotToAutoBackup(SlotData slotData, UUID playerUUID, int slotNumber) {
        try {
            // Get island directly by player UUID (not by slot's stored island UUID)
            World bskyblockWorld = addon.getGridManager().getBSkyBlockWorld();
            if (bskyblockWorld == null) {
                addon.logError("BSkyBlock world not available for auto-backup");
                return false;
            }

            Island island = addon.getIslands().getIsland(bskyblockWorld, playerUUID);
            if (island == null) {
                // Player might be homeless - this is fine, just skip
                return false;
            }

            // Get island bounds
            Location center = island.getCenter();
            World world = center.getWorld();
            if (world == null) {
                return false;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            // Get auto-backup file path
            File backupFile = getAutoBackupFile(playerUUID, slotNumber);

            // Use SchematicUtils for safe entity handling
            boolean success = addon.getSchematicUtils().copyAndSave(center, range, false, backupFile);

            if (success) {
                addon.log("Auto-backup created for player " + playerUUID + " slot " + slotNumber);
            }
            return success;

        } catch (Exception e) {
            addon.logError("Failed to create auto-backup for player " + playerUUID + " slot " + slotNumber + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the auto-backup file for a player's slot, if it exists
     * @return The auto-backup file, or null if it doesn't exist
     */
    public File getAutoBackup(UUID playerUUID, int slotNumber) {
        File file = getAutoBackupFile(playerUUID, slotNumber);
        return file.exists() ? file : null;
    }

    /**
     * Check if an auto-backup exists for a player's slot
     */
    public boolean hasAutoBackup(UUID playerUUID, int slotNumber) {
        return getAutoBackupFile(playerUUID, slotNumber).exists();
    }

    /**
     * Clean up old backups based on retention settings
     */
    private void cleanOldBackups(UUID playerUUID, int slotNumber) {
        int maxBackups = addon.getSettings().getMaxBackupsPerSlot();
        if (maxBackups <= 0) {
            return; // No limit
        }

        File playerDir = new File(backupDir, playerUUID.toString());
        if (!playerDir.exists()) {
            return;
        }

        // Find all backup files for this slot
        File[] backupFiles = playerDir.listFiles((dir, name) ->
            name.startsWith("slot-" + slotNumber + "-") && name.endsWith(".schem"));

        if (backupFiles == null || backupFiles.length <= maxBackups) {
            return; // Within limit
        }

        // Sort by last modified time (oldest first)
        java.util.Arrays.sort(backupFiles, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));

        // Delete oldest backups to maintain the limit
        int toDelete = backupFiles.length - maxBackups;
        for (int i = 0; i < toDelete; i++) {
            if (backupFiles[i].delete()) {
                addon.log("Deleted old backup: " + backupFiles[i].getName());
            } else {
                addon.logError("Failed to delete old backup: " + backupFiles[i].getName());
            }
        }
    }

    /**
     * Get the backup directory
     */
    public File getBackupDirectory() {
        return backupDir;
    }

    /**
     * Get the backup directory for a specific player
     */
    public File getPlayerBackupDirectory(UUID playerUUID) {
        return new File(backupDir, playerUUID.toString());
    }

    /**
     * Count backups for a specific slot
     */
    public int countBackups(UUID playerUUID, int slotNumber) {
        File playerDir = getPlayerBackupDirectory(playerUUID);
        if (!playerDir.exists()) {
            return 0;
        }

        File[] backupFiles = playerDir.listFiles((dir, name) ->
            name.startsWith("slot-" + slotNumber + "-") && name.endsWith(".schem"));

        return backupFiles != null ? backupFiles.length : 0;
    }

    /**
     * List all backup files for a specific player and slot
     * Returns files sorted by timestamp (newest first)
     * @param playerUUID The player's UUID
     * @param slotNumber The slot number
     * @return Array of backup files, or empty array if none found
     */
    public File[] listBackups(UUID playerUUID, int slotNumber) {
        File playerDir = getPlayerBackupDirectory(playerUUID);
        if (!playerDir.exists()) {
            return new File[0];
        }

        File[] backupFiles = playerDir.listFiles((dir, name) ->
            name.startsWith("slot-" + slotNumber + "-") && name.endsWith(".schem"));

        if (backupFiles == null || backupFiles.length == 0) {
            return new File[0];
        }

        // Sort by last modified time (newest first)
        java.util.Arrays.sort(backupFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        return backupFiles;
    }

    /**
     * Restore a backup to a specific slot
     * This replaces the current island content with the backup
     * @param backupFile The backup file to restore from
     * @param playerUUID The player's UUID
     * @param slotNumber The slot number to restore to
     * @return true if restore was successful, false otherwise
     */
    public boolean restoreBackup(File backupFile, UUID playerUUID, int slotNumber) {
        if (!backupFile.exists()) {
            addon.logError("Backup file not found: " + backupFile.getAbsolutePath());
            return false;
        }

        SlotData slotData = addon.getSlotManager().getSlot(playerUUID, slotNumber);
        if (slotData == null) {
            addon.logError("Slot " + slotNumber + " not found for player " + playerUUID);
            return false;
        }

        // We no longer require slotData.getIslandUUID() - we get island directly by player UUID
        return loadBackupToWorld(backupFile, playerUUID, slotData);
    }

    /**
     * Load a backup schematic file to the world at the slot's island location
     * @param backupFile The backup file to restore from
     * @param playerUUID The player's UUID to find their island
     * @param slotData The slot data (for logging)
     */
    private boolean loadBackupToWorld(File backupFile, UUID playerUUID, SlotData slotData) {
        try {
            // Get island location directly by player UUID (not by slot's stored island UUID)
            World bskyblockWorld = addon.getGridManager().getBSkyBlockWorld();
            if (bskyblockWorld == null) {
                addon.logError("BSkyBlock world not available for restoring backup");
                return false;
            }

            Island island = addon.getIslands().getIsland(bskyblockWorld, playerUUID);
            if (island == null) {
                addon.logError("Island not found for player: " + playerUUID + " when restoring slot: " + slotData.getUniqueId());
                return false;
            }

            Location center = island.getCenter();
            if (center.getWorld() == null) {
                addon.logError("Island world is null for slot: " + slotData.getUniqueId());
                return false;
            }

            // Use SchematicUtils to load and paste
            boolean success = addon.getSchematicUtils().loadAndPaste(backupFile, center);
            if (success) {
                addon.log("Restored backup " + backupFile.getName() + " for slot: " + slotData.getUniqueId());
            }
            return success;

        } catch (Exception e) {
            addon.logError("Failed to restore backup " + backupFile.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
