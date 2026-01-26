package world.bentobox.islandselector.managers;

import org.bukkit.Location;
import org.bukkit.World;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.models.DimensionConfig;
import world.bentobox.islandselector.integrations.NovaIntegration.NovaBlockData;
import world.bentobox.islandselector.integrations.NovaIntegration.RestoreResult;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            if (center == null) {
                addon.logError("Island center is null for slot: " + slotData.getUniqueId());
                return false;
            }
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

        // Ensure player directory exists
        if (!playerDir.exists()) {
            playerDir.mkdirs();
        }

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
        // Ensure player directory exists
        if (!playerDir.exists()) {
            playerDir.mkdirs();
        }
        String filename = "slot-" + slotNumber + "-auto.schem";
        return new File(playerDir, filename);
    }

    /**
     * Get the backup file path for a specific slot and dimension.
     * Format: backups/{player-uuid}/slot-{number}-{dimension}-{timestamp}.schem
     * @param playerUUID The player's UUID
     * @param slotNumber The slot number
     * @param dimensionKey The dimension key (e.g., "overworld", "nether")
     * @return The backup file
     */
    private File getBackupFile(UUID playerUUID, int slotNumber, String dimensionKey) {
        File playerDir = new File(backupDir, playerUUID.toString());

        // Ensure player directory exists
        if (!playerDir.exists()) {
            playerDir.mkdirs();
        }

        // Create timestamp for unique filename
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String timestamp = dateFormat.format(new Date());

        String filename = "slot-" + slotNumber + "-" + dimensionKey + "-" + timestamp + ".schem";
        return new File(playerDir, filename);
    }

    /**
     * Get the auto-backup file path for a specific slot and dimension.
     * Format: backups/{player-uuid}/slot-{number}-{dimension}-auto.schem
     * @param playerUUID The player's UUID
     * @param slotNumber The slot number
     * @param dimensionKey The dimension key (e.g., "overworld", "nether")
     * @return The auto-backup file
     */
    private File getAutoBackupFile(UUID playerUUID, int slotNumber, String dimensionKey) {
        File playerDir = new File(backupDir, playerUUID.toString());
        // Ensure player directory exists
        if (!playerDir.exists()) {
            playerDir.mkdirs();
        }
        String filename = "slot-" + slotNumber + "-" + dimensionKey + "-auto.schem";
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
            try {
                if (backupFiles[i].delete()) {
                    addon.log("Deleted old backup: " + backupFiles[i].getName());
                } else {
                    addon.logError("Failed to delete old backup (file may be in use): " + backupFiles[i].getName());
                }
            } catch (SecurityException e) {
                addon.logError("Security exception deleting backup " + backupFiles[i].getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Check if Nova integration is available and enabled
     */
    private boolean isNovaEnabled() {
        return addon.getNovaIntegration() != null
            && addon.getNovaIntegration().isAvailable()
            && addon.getSettings().isNovaEnabled();
    }

    /**
     * Get the Nova blocks backup file for a specific backup schematic.
     * File is stored alongside the schematic with .nova extension.
     *
     * @param schematicFile The schematic backup file
     * @return The Nova blocks file (.nova)
     */
    private File getNovaBackupFile(File schematicFile) {
        String schematicPath = schematicFile.getAbsolutePath();
        String novaPath = schematicPath.replace(".schem", ".nova");
        return new File(novaPath);
    }

    /**
     * Capture Nova blocks for a backup operation.
     *
     * @param playerUUID Player's UUID
     * @param world The world containing the island
     * @param schematicFile The schematic file (used to derive Nova file path)
     * @return true if successful (or Nova not enabled)
     */
    private boolean captureAndSaveNovaBlocksForBackup(UUID playerUUID, World world, File schematicFile) {
        if (!isNovaEnabled()) {
            return true;
        }

        Island island = addon.getIslands().getIsland(world, playerUUID);
        if (island == null) {
            return true; // No island, nothing to capture
        }

        Location center = island.getCenter();
        if (center == null || center.getWorld() == null) {
            return true;
        }

        int islandSpacing = addon.getIslandSpacing();
        int protectionRange = island.getProtectionRange();
        int range = Math.max(islandSpacing / 2, protectionRange);

        // Capture Nova blocks
        List<NovaBlockData> novaBlocks = addon.getNovaIntegration().captureNovaBlocks(center, range);

        if (novaBlocks == null || novaBlocks.isEmpty()) {
            return true; // No Nova blocks to save
        }

        // Save to .nova file alongside schematic
        File novaFile = getNovaBackupFile(schematicFile);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(novaFile))) {
            oos.writeObject(novaBlocks);
            addon.log("Saved " + novaBlocks.size() + " Nova blocks to backup: " + novaFile.getName());
            return true;
        } catch (IOException e) {
            addon.logError("Failed to save Nova blocks for backup: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load and restore Nova blocks from a backup.
     *
     * @param playerUUID Player's UUID
     * @param world The world containing the island
     * @param schematicFile The schematic file (used to derive Nova file path)
     * @return RestoreResult with machine counts
     */
    @SuppressWarnings("unchecked")
    private RestoreResult loadAndRestoreNovaBlocksFromBackup(UUID playerUUID, World world, File schematicFile) {
        if (!isNovaEnabled()) {
            return new RestoreResult(0, 0);
        }

        File novaFile = getNovaBackupFile(schematicFile);
        if (!novaFile.exists()) {
            return new RestoreResult(0, 0); // No Nova blocks in this backup
        }

        Island island = addon.getIslands().getIsland(world, playerUUID);
        if (island == null || island.getCenter() == null) {
            return new RestoreResult(0, 0);
        }

        // Load Nova blocks from file
        List<NovaBlockData> novaBlocks;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(novaFile))) {
            Object obj = ois.readObject();
            if (obj instanceof List) {
                novaBlocks = (List<NovaBlockData>) obj;
            } else {
                addon.logWarning("Nova backup file has unexpected format");
                return new RestoreResult(0, 0);
            }
        } catch (IOException | ClassNotFoundException e) {
            addon.logError("Failed to load Nova blocks from backup: " + e.getMessage());
            return new RestoreResult(0, 0);
        }

        if (novaBlocks.isEmpty()) {
            return new RestoreResult(0, 0);
        }

        // Restore Nova blocks
        RestoreResult result = addon.getNovaIntegration().restoreNovaBlocks(novaBlocks, island.getCenter());
        addon.log("Restored Nova blocks from backup: " + result.machinesRestored + " machines restored");

        return result;
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

    // ============================
    // Multi-Dimension Backup Methods
    // ============================

    /**
     * Check if multi-dimension is enabled
     */
    private boolean isMultiDimensionEnabled() {
        DimensionManager dimManager = addon.getDimensionManager();
        return dimManager != null && dimManager.isEnabled();
    }

    /**
     * Get enabled dimensions
     */
    private List<DimensionConfig> getEnabledDimensions() {
        DimensionManager dimManager = addon.getDimensionManager();
        if (dimManager == null || !dimManager.isEnabled()) {
            return new ArrayList<>();
        }
        return dimManager.getEnabledDimensions();
    }

    /**
     * Create a backup of a specific slot across ALL enabled dimensions.
     * @param playerUUID The player's UUID
     * @param slotNumber The slot number to backup (1-based)
     * @return Map of dimension key to success status
     */
    public Map<String, Boolean> createBackupAllDimensions(UUID playerUUID, int slotNumber) {
        Map<String, Boolean> results = new HashMap<>();

        if (!isMultiDimensionEnabled()) {
            // Fallback to single-dimension backup
            boolean success = createBackup(playerUUID, slotNumber);
            results.put("overworld", success);
            return results;
        }

        SlotData slotData = addon.getSlotManager().getSlot(playerUUID, slotNumber);
        if (slotData == null) {
            addon.logError("Slot " + slotNumber + " not found for player " + playerUUID);
            return results;
        }

        List<DimensionConfig> dimensions = getEnabledDimensions();
        DimensionManager dimManager = addon.getDimensionManager();

        for (DimensionConfig config : dimensions) {
            String dimensionKey = config.getDimensionKey();
            World world = dimManager.getWorld(dimensionKey);

            if (world == null) {
                addon.logWarning("World not found for dimension " + dimensionKey + ", skipping backup");
                results.put(dimensionKey, false);
                continue;
            }

            boolean success = saveSlotToBackupForDimension(slotData, playerUUID, slotNumber, dimensionKey, world);
            results.put(dimensionKey, success);
        }

        addon.log("Multi-dimension backup complete for player " + playerUUID + " slot " + slotNumber +
                ": " + results.values().stream().filter(b -> b).count() + "/" + dimensions.size() + " succeeded");

        return results;
    }

    /**
     * Save a slot as a backup schematic file for a specific dimension
     */
    private boolean saveSlotToBackupForDimension(SlotData slotData, UUID playerUUID, int slotNumber,
                                                  String dimensionKey, World world) {
        try {
            Island island = addon.getIslands().getIsland(world, playerUUID);
            if (island == null) {
                addon.logWarning("Island not found in " + dimensionKey + " for player: " + playerUUID);
                return false;
            }

            Location center = island.getCenter();
            if (center.getWorld() == null) {
                addon.logError("Island world is null for dimension " + dimensionKey);
                return false;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            // Get dimension-specific backup file
            File backupFile = getBackupFile(playerUUID, slotNumber, dimensionKey);

            boolean success = addon.getSchematicUtils().copyAndSave(center, range, false, backupFile);

            if (success) {
                addon.log("Backup created for player " + playerUUID + " slot " + slotNumber +
                        " dimension " + dimensionKey + ": " + backupFile.getName());
            }

            // Clean up old backups for this dimension
            cleanOldBackupsForDimension(playerUUID, slotNumber, dimensionKey);

            return success;

        } catch (Exception e) {
            addon.logError("Failed to create backup for " + dimensionKey + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Create an auto-backup of a specific slot across ALL enabled dimensions.
     * @param playerUUID The player's UUID
     * @param slotNumber The slot number to backup (1-based)
     * @return Map of dimension key to success status
     */
    public Map<String, Boolean> createAutoBackupAllDimensions(UUID playerUUID, int slotNumber) {
        Map<String, Boolean> results = new HashMap<>();

        if (!isMultiDimensionEnabled()) {
            // Fallback to single-dimension backup
            boolean success = createAutoBackup(playerUUID, slotNumber);
            results.put("overworld", success);
            return results;
        }

        SlotData slotData = addon.getSlotManager().getSlot(playerUUID, slotNumber);
        if (slotData == null || !slotData.hasIsland()) {
            return results;
        }

        List<DimensionConfig> dimensions = getEnabledDimensions();
        DimensionManager dimManager = addon.getDimensionManager();

        for (DimensionConfig config : dimensions) {
            String dimensionKey = config.getDimensionKey();
            World world = dimManager.getWorld(dimensionKey);

            if (world == null) {
                results.put(dimensionKey, false);
                continue;
            }

            boolean success = saveSlotToAutoBackupForDimension(slotData, playerUUID, slotNumber, dimensionKey, world);
            results.put(dimensionKey, success);
        }

        return results;
    }

    /**
     * Save a slot as an auto-backup schematic file for a specific dimension
     */
    private boolean saveSlotToAutoBackupForDimension(SlotData slotData, UUID playerUUID, int slotNumber,
                                                      String dimensionKey, World world) {
        try {
            Island island = addon.getIslands().getIsland(world, playerUUID);
            if (island == null) {
                return false;
            }

            Location center = island.getCenter();
            if (center.getWorld() == null) {
                return false;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            // Get dimension-specific auto-backup file
            File backupFile = getAutoBackupFile(playerUUID, slotNumber, dimensionKey);

            boolean success = addon.getSchematicUtils().copyAndSave(center, range, false, backupFile);

            if (success) {
                addon.log("Auto-backup created for player " + playerUUID + " slot " + slotNumber +
                        " dimension " + dimensionKey);
            }

            return success;

        } catch (Exception e) {
            addon.logError("Failed to create auto-backup for " + dimensionKey + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Restore backups for ALL dimensions from a base backup file.
     * Automatically finds matching dimension backup files based on the base file's timestamp.
     * @param baseBackupFile The base backup file (used to determine timestamp)
     * @param playerUUID The player's UUID
     * @param slotNumber The slot number to restore to
     * @return Map of dimension key to success status
     */
    public Map<String, Boolean> restoreBackupAllDimensions(File baseBackupFile, UUID playerUUID, int slotNumber) {
        Map<String, Boolean> results = new HashMap<>();

        if (!isMultiDimensionEnabled()) {
            // Fallback to single-dimension restore
            boolean success = restoreBackup(baseBackupFile, playerUUID, slotNumber);
            results.put("overworld", success);
            return results;
        }

        SlotData slotData = addon.getSlotManager().getSlot(playerUUID, slotNumber);
        if (slotData == null) {
            addon.logError("Slot " + slotNumber + " not found for player " + playerUUID);
            return results;
        }

        // Extract timestamp from base file name
        String timestamp = extractTimestamp(baseBackupFile.getName());

        List<DimensionConfig> dimensions = getEnabledDimensions();
        DimensionManager dimManager = addon.getDimensionManager();

        for (DimensionConfig config : dimensions) {
            String dimensionKey = config.getDimensionKey();
            World world = dimManager.getWorld(dimensionKey);

            if (world == null) {
                addon.logWarning("World not found for dimension " + dimensionKey + ", skipping restore");
                results.put(dimensionKey, false);
                continue;
            }

            // Find the matching backup file for this dimension
            File dimensionBackup = findBackupFileForDimension(playerUUID, slotNumber, dimensionKey, timestamp);

            if (dimensionBackup == null || !dimensionBackup.exists()) {
                addon.logWarning("No backup found for dimension " + dimensionKey + " with timestamp " + timestamp);
                results.put(dimensionKey, false);
                continue;
            }

            boolean success = loadBackupToWorldForDimension(dimensionBackup, playerUUID, dimensionKey, world);
            results.put(dimensionKey, success);
        }

        addon.log("Multi-dimension restore complete for player " + playerUUID + " slot " + slotNumber +
                ": " + results.values().stream().filter(b -> b).count() + "/" + dimensions.size() + " succeeded");

        return results;
    }

    /**
     * Restore a specific backup file to a specific dimension
     * @param backupFile The backup file to restore
     * @param playerUUID The player's UUID
     * @param dimensionKey The dimension key
     * @param world The world for this dimension
     * @return true if successful
     */
    private boolean loadBackupToWorldForDimension(File backupFile, UUID playerUUID, String dimensionKey, World world) {
        try {
            Island island = addon.getIslands().getIsland(world, playerUUID);
            if (island == null) {
                addon.logError("Island not found in " + dimensionKey + " for player: " + playerUUID);
                return false;
            }

            Location center = island.getCenter();
            if (center.getWorld() == null) {
                addon.logError("Island world is null for dimension " + dimensionKey);
                return false;
            }

            boolean success = addon.getSchematicUtils().loadAndPaste(backupFile, center);
            if (success) {
                addon.log("Restored backup " + backupFile.getName() + " to dimension " + dimensionKey);
            }
            return success;

        } catch (Exception e) {
            addon.logError("Failed to restore backup to " + dimensionKey + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Extract timestamp from a backup filename.
     * Expected format: slot-{number}-{dimension}-{timestamp}.schem or slot-{number}-{timestamp}.schem
     */
    private String extractTimestamp(String filename) {
        if (filename == null || !filename.endsWith(".schem")) {
            return null;
        }

        // Remove .schem extension
        String base = filename.substring(0, filename.length() - 6);

        // Split by '-' and find the timestamp part (last part that looks like a timestamp)
        String[] parts = base.split("-");
        if (parts.length >= 3) {
            // Timestamp format: yyyyMMdd-HHmmss (15 chars)
            // Last two parts should be date and time
            String lastPart = parts[parts.length - 1];
            String secondLastPart = parts[parts.length - 2];

            // Check if it looks like a timestamp
            if (secondLastPart.length() == 8 && lastPart.length() == 6) {
                return secondLastPart + "-" + lastPart;
            }
        }

        return null;
    }

    /**
     * Find a backup file for a specific dimension with the given timestamp
     */
    private File findBackupFileForDimension(UUID playerUUID, int slotNumber, String dimensionKey, String timestamp) {
        File playerDir = getPlayerBackupDirectory(playerUUID);
        if (!playerDir.exists()) {
            return null;
        }

        // Look for exact match: slot-{number}-{dimension}-{timestamp}.schem
        String expectedName = "slot-" + slotNumber + "-" + dimensionKey + "-" + timestamp + ".schem";
        File exactMatch = new File(playerDir, expectedName);
        if (exactMatch.exists()) {
            return exactMatch;
        }

        // Fallback: look for legacy format without dimension key (for overworld)
        if ("overworld".equals(dimensionKey)) {
            String legacyName = "slot-" + slotNumber + "-" + timestamp + ".schem";
            File legacyMatch = new File(playerDir, legacyName);
            if (legacyMatch.exists()) {
                return legacyMatch;
            }
        }

        return null;
    }

    /**
     * Clean up old backups for a specific dimension
     */
    private void cleanOldBackupsForDimension(UUID playerUUID, int slotNumber, String dimensionKey) {
        int maxBackups = addon.getSettings().getMaxBackupsPerSlot();
        if (maxBackups <= 0) {
            return;
        }

        File playerDir = new File(backupDir, playerUUID.toString());
        if (!playerDir.exists()) {
            return;
        }

        // Find all backup files for this slot and dimension
        String prefix = "slot-" + slotNumber + "-" + dimensionKey + "-";
        File[] backupFiles = playerDir.listFiles((dir, name) ->
            name.startsWith(prefix) && name.endsWith(".schem") && !name.contains("-auto."));

        if (backupFiles == null || backupFiles.length <= maxBackups) {
            return;
        }

        // Sort by last modified time (oldest first)
        java.util.Arrays.sort(backupFiles, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));

        // Delete oldest backups to maintain the limit
        int toDelete = backupFiles.length - maxBackups;
        for (int i = 0; i < toDelete; i++) {
            if (backupFiles[i].delete()) {
                addon.log("Deleted old backup: " + backupFiles[i].getName());
            }
        }
    }

    /**
     * List all backup files for a specific player, slot, and dimension.
     * Returns files sorted by timestamp (newest first).
     * @param playerUUID The player's UUID
     * @param slotNumber The slot number
     * @param dimensionKey The dimension key
     * @return Array of backup files, or empty array if none found
     */
    public File[] listBackupsForDimension(UUID playerUUID, int slotNumber, String dimensionKey) {
        File playerDir = getPlayerBackupDirectory(playerUUID);
        if (!playerDir.exists()) {
            return new File[0];
        }

        String prefix = "slot-" + slotNumber + "-" + dimensionKey + "-";
        File[] backupFiles = playerDir.listFiles((dir, name) ->
            name.startsWith(prefix) && name.endsWith(".schem") && !name.contains("-auto."));

        if (backupFiles == null || backupFiles.length == 0) {
            return new File[0];
        }

        // Sort by last modified time (newest first)
        java.util.Arrays.sort(backupFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        return backupFiles;
    }

    /**
     * Get the auto-backup file for a player's slot in a specific dimension, if it exists.
     * @param playerUUID The player's UUID
     * @param slotNumber The slot number
     * @param dimensionKey The dimension key
     * @return The auto-backup file, or null if it doesn't exist
     */
    public File getAutoBackupForDimension(UUID playerUUID, int slotNumber, String dimensionKey) {
        File file = getAutoBackupFile(playerUUID, slotNumber, dimensionKey);
        return file.exists() ? file : null;
    }

    /**
     * Check if an auto-backup exists for all dimensions for a player's slot.
     * @return Map of dimension key to whether auto-backup exists
     */
    public Map<String, Boolean> hasAutoBackupAllDimensions(UUID playerUUID, int slotNumber) {
        Map<String, Boolean> results = new HashMap<>();

        if (!isMultiDimensionEnabled()) {
            results.put("overworld", hasAutoBackup(playerUUID, slotNumber));
            return results;
        }

        for (DimensionConfig config : getEnabledDimensions()) {
            String dimensionKey = config.getDimensionKey();
            File file = getAutoBackupFile(playerUUID, slotNumber, dimensionKey);
            results.put(dimensionKey, file.exists());
        }

        return results;
    }

    /**
     * Count backups for a specific slot across all dimensions.
     * @return Map of dimension key to backup count
     */
    public Map<String, Integer> countBackupsAllDimensions(UUID playerUUID, int slotNumber) {
        Map<String, Integer> results = new HashMap<>();

        if (!isMultiDimensionEnabled()) {
            results.put("overworld", countBackups(playerUUID, slotNumber));
            return results;
        }

        for (DimensionConfig config : getEnabledDimensions()) {
            String dimensionKey = config.getDimensionKey();
            File[] files = listBackupsForDimension(playerUUID, slotNumber, dimensionKey);
            results.put(dimensionKey, files.length);
        }

        return results;
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
