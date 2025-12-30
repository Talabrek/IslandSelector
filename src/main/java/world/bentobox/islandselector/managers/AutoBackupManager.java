package world.bentobox.islandselector.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;

import java.util.UUID;

/**
 * Manager for automatic periodic backups of online players' active islands.
 * Creates a single rolling backup per player that gets overwritten each interval.
 */
public class AutoBackupManager {

    private final IslandSelector addon;
    private BukkitTask backupTask;

    public AutoBackupManager(IslandSelector addon) {
        this.addon = addon;
    }

    /**
     * Start the auto-backup scheduler
     */
    public void start() {
        if (!addon.getSettings().isAutoBackupEnabled()) {
            addon.log("Auto-backup is disabled in config");
            return;
        }

        // Convert minutes to ticks (20 ticks = 1 second)
        int intervalMinutes = addon.getSettings().getAutoBackupInterval();
        long intervalTicks = intervalMinutes * 60L * 20L;

        // Schedule repeating task
        backupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            addon.getPlugin(),
            this::performAutoBackups,
            intervalTicks, // Initial delay
            intervalTicks  // Repeat interval
        );

        addon.log("Auto-backup scheduler started (interval: " + intervalMinutes + " minutes)");
    }

    /**
     * Stop the auto-backup scheduler
     */
    public void stop() {
        if (backupTask != null && !backupTask.isCancelled()) {
            backupTask.cancel();
            backupTask = null;
            addon.log("Auto-backup scheduler stopped");
        }
    }

    /**
     * Perform auto-backups for all online players
     */
    private void performAutoBackups() {
        addon.log("Starting periodic auto-backup for online players...");

        int backupCount = 0;
        int failCount = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerUUID = player.getUniqueId();

            // Get player's active slot
            SlotData activeSlot = addon.getSlotManager().getActiveSlot(playerUUID);
            if (activeSlot == null || !activeSlot.hasIsland()) {
                continue;
            }

            try {
                // Create auto-backup (single rolling backup)
                boolean success = createAutoBackup(playerUUID, activeSlot.getSlotNumber());
                if (success) {
                    backupCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                addon.logError("Auto-backup failed for " + player.getName() + ": " + e.getMessage());
                failCount++;
            }
        }

        addon.log("Periodic auto-backup complete: " + backupCount + " succeeded, " + failCount + " failed");
    }

    /**
     * Create an auto-backup for a player's active slot.
     * This creates a single rolling backup file that gets overwritten each time.
     *
     * @param playerUUID The player's UUID
     * @param slotNumber The slot number to backup
     * @return true if backup was successful
     */
    public boolean createAutoBackup(UUID playerUUID, int slotNumber) {
        return addon.getBackupManager().createAutoBackup(playerUUID, slotNumber);
    }

    /**
     * Check if auto-backup is currently running
     */
    public boolean isRunning() {
        return backupTask != null && !backupTask.isCancelled();
    }

    /**
     * Trigger an immediate auto-backup for all online players
     * Can be called by admin command
     */
    public void triggerImmediateBackup() {
        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), this::performAutoBackups);
    }
}
