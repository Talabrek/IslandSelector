package world.bentobox.islandselector.managers;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;

import org.bukkit.Location;
import org.bukkit.World;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
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

        if (slotData.getIslandUUID() == null) {
            addon.logError("Slot " + slotNumber + " has no island for player " + playerUUID);
            return false;
        }

        return saveSlotToBackup(slotData, playerUUID, slotNumber);
    }

    /**
     * Save a slot as a backup schematic file
     */
    private boolean saveSlotToBackup(SlotData slotData, UUID playerUUID, int slotNumber) {
        try {
            // Get island from BentoBox
            Island island = addon.getIslands().getIslandById(slotData.getIslandUUID()).orElse(null);
            if (island == null) {
                addon.logError("Island not found for slot: " + slotData.getUniqueId());
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

            // Create WorldEdit region
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            BlockVector3 min = BlockVector3.at(
                center.getX() - range,
                world.getMinHeight(),
                center.getZ() - range
            );
            BlockVector3 max = BlockVector3.at(
                center.getX() + range,
                world.getMaxHeight() - 1,
                center.getZ() + range
            );

            CuboidRegion region = new CuboidRegion(weWorld, min, max);

            // Create clipboard
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            clipboard.setOrigin(BlockVector3.at(center.getX(), center.getY(), center.getZ()));

            // Copy blocks to clipboard
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                ForwardExtentCopy copy = new ForwardExtentCopy(
                    editSession, region, clipboard, region.getMinimumPoint()
                );
                copy.setCopyingEntities(true);
                copy.setCopyingBiomes(false);
                Operations.complete(copy);
            }

            // Save to backup file
            File backupFile = getBackupFile(playerUUID, slotNumber);
            File playerBackupDir = backupFile.getParentFile();

            if (!playerBackupDir.exists()) {
                playerBackupDir.mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(backupFile);
                 ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(fos)) {
                writer.write(clipboard);
            }

            addon.log("Backup created for player " + playerUUID + " slot " + slotNumber + ": " + backupFile.getName());

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
}
