package world.bentobox.islandselector.managers;

import org.bukkit.Bukkit;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.islandselector.IslandSelector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Integration with the Challenges addon to make challenge progress slot-specific.
 *
 * When a player switches slots, this manager:
 * 1. Saves their current challenge progress to a slot-specific backup
 * 2. Restores the challenge progress for the target slot (or clears if new slot)
 *
 * Challenge data is stored in: plugins/BentoBox/database/ChallengesPlayerData/
 * Each player has a file named {player-uuid}.json
 */
public class ChallengesIntegration {

    private final IslandSelector addon;
    private final File challengesDataDir;
    private final File slotChallengesDir;
    private boolean challengesAddonPresent = false;
    private boolean warnedAboutMissingMethod = false;

    public ChallengesIntegration(IslandSelector addon) {
        this.addon = addon;

        // BentoBox database location for Challenges player data
        this.challengesDataDir = new File(BentoBox.getInstance().getDataFolder(),
            "database/ChallengesPlayerData");

        // Our slot-specific backup location
        this.slotChallengesDir = new File(addon.getDataFolder(), "slot-challenges");

        // Create our backup directory if needed
        if (!slotChallengesDir.exists()) {
            slotChallengesDir.mkdirs();
        }

        // Check if Challenges addon is present
        challengesAddonPresent = BentoBox.getInstance().getAddonsManager()
            .getAddonByName("Challenges").isPresent();

        if (challengesAddonPresent) {
            addon.log("Challenges addon detected - slot-specific challenge progress enabled");
        } else {
            addon.log("Challenges addon not found - challenge integration disabled");
        }
    }

    /**
     * Check if Challenges integration is available
     */
    public boolean isEnabled() {
        return challengesAddonPresent && challengesDataDir.exists();
    }

    /**
     * Called when a player switches FROM a slot.
     * Saves their current challenge progress to the slot-specific backup.
     *
     * @param playerUUID The player's UUID
     * @param fromSlot The slot number they're switching from
     */
    public void saveSlotChallenges(UUID playerUUID, int fromSlot) {
        if (!isEnabled()) {
            return;
        }

        try {
            File playerChallengeFile = new File(challengesDataDir, playerUUID.toString() + ".json");

            if (!playerChallengeFile.exists()) {
                addon.log("No challenge data found for player " + playerUUID + " - nothing to save");
                return;
            }

            // Create player's slot challenges directory
            File playerSlotDir = new File(slotChallengesDir, playerUUID.toString());
            if (!playerSlotDir.exists()) {
                playerSlotDir.mkdirs();
            }

            // Copy current challenge data to slot backup using atomic write via temp file
            File slotBackup = new File(playerSlotDir, "slot-" + fromSlot + ".json");
            File tempFile = new File(playerSlotDir, "slot-" + fromSlot + ".json.tmp");

            // Write to temp file first, then atomic rename
            Files.copy(playerChallengeFile.toPath(), tempFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
            Files.move(tempFile.toPath(), slotBackup.toPath(),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            addon.log("Saved challenge progress for " + playerUUID + " slot " + fromSlot);

        } catch (IOException e) {
            addon.logError("Failed to save challenge progress for slot " + fromSlot + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Called when a player switches TO a slot.
     * Restores their challenge progress from the slot-specific backup,
     * or clears progress if no backup exists (new slot).
     *
     * @param playerUUID The player's UUID
     * @param toSlot The slot number they're switching to
     */
    public void restoreSlotChallenges(UUID playerUUID, int toSlot) {
        if (!isEnabled()) {
            return;
        }

        try {
            File playerChallengeFile = new File(challengesDataDir, playerUUID.toString() + ".json");
            File playerSlotDir = new File(slotChallengesDir, playerUUID.toString());
            File slotBackup = new File(playerSlotDir, "slot-" + toSlot + ".json");

            if (slotBackup.exists()) {
                // Restore challenge data from slot backup using atomic write via temp file
                File tempFile = new File(challengesDataDir, playerUUID.toString() + ".json.tmp");
                Files.copy(slotBackup.toPath(), tempFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
                Files.move(tempFile.toPath(), playerChallengeFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                addon.log("Restored challenge progress for " + playerUUID + " slot " + toSlot);
            } else {
                // No backup for this slot - create empty/fresh challenge data
                // We delete the current file so Challenges addon creates fresh data
                if (playerChallengeFile.exists()) {
                    playerChallengeFile.delete();
                    addon.log("Cleared challenge progress for " + playerUUID + " (new slot " + toSlot + ")");
                }
            }

            // Force Challenges addon to reload player data
            reloadChallengesPlayerData(playerUUID);

        } catch (IOException e) {
            addon.logError("Failed to restore challenge progress for slot " + toSlot + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Force the Challenges addon to reload a player's data from the database.
     * This is necessary after we modify the file directly.
     */
    private void reloadChallengesPlayerData(UUID playerUUID) {
        try {
            // Get the Challenges addon
            var challengesAddon = BentoBox.getInstance().getAddonsManager()
                .getAddonByName("Challenges");

            if (challengesAddon.isEmpty()) {
                return;
            }

            Object challenges = challengesAddon.get();

            // Use reflection to access the ChallengesManager and reload player data
            // This is necessary because Challenges addon doesn't expose a public reload method
            java.lang.reflect.Method getManagerMethod = challenges.getClass()
                .getMethod("getChallengesManager");
            Object manager = getManagerMethod.invoke(challenges);

            if (manager != null) {
                // Try to find a method to reload/clear player data cache
                // The method name varies by Challenges version
                try {
                    // Try loadPlayerData method
                    java.lang.reflect.Method loadMethod = manager.getClass()
                        .getMethod("loadPlayerData", UUID.class);
                    loadMethod.invoke(manager, playerUUID);
                    addon.log("Reloaded Challenges player data via loadPlayerData");
                } catch (NoSuchMethodException e1) {
                    try {
                        // Try alternative method - clear from cache so it reloads
                        java.lang.reflect.Method removeMethod = manager.getClass()
                            .getMethod("removeFromCache", UUID.class);
                        removeMethod.invoke(manager, playerUUID);
                        addon.log("Cleared Challenges player data from cache");
                    } catch (NoSuchMethodException e2) {
                        // Last resort - just log that manual reload may be needed
                        addon.logWarning("Could not automatically reload Challenges data - " +
                            "player may need to rejoin for changes to take effect");
                    }
                }
            }
        } catch (Exception e) {
            addon.logWarning("Failed to reload Challenges player data: " + e.getMessage());
        }
    }

    /**
     * Initialize challenge data for a player's first slot.
     * This should be called when a player creates their first island.
     *
     * @param playerUUID The player's UUID
     * @param slotNumber The slot number (should be 1 for first island)
     */
    public void initializeSlotChallenges(UUID playerUUID, int slotNumber) {
        if (!isEnabled()) {
            return;
        }

        // For the first slot, we don't need to do anything special
        // The Challenges addon will create fresh data automatically
        addon.log("Initialized challenge tracking for " + playerUUID + " slot " + slotNumber);
    }

    /**
     * Copy challenge progress from one slot to another.
     * Useful if we want to allow players to "clone" their progress.
     *
     * @param playerUUID The player's UUID
     * @param fromSlot Source slot
     * @param toSlot Destination slot
     * @return true if successful
     */
    public boolean copySlotChallenges(UUID playerUUID, int fromSlot, int toSlot) {
        if (!isEnabled()) {
            return false;
        }

        try {
            File playerSlotDir = new File(slotChallengesDir, playerUUID.toString());
            File sourceBackup = new File(playerSlotDir, "slot-" + fromSlot + ".json");
            File destBackup = new File(playerSlotDir, "slot-" + toSlot + ".json");

            if (!sourceBackup.exists()) {
                addon.logWarning("Cannot copy challenges - source slot " + fromSlot + " has no data");
                return false;
            }

            if (!playerSlotDir.exists()) {
                playerSlotDir.mkdirs();
            }

            // Use atomic write via temp file
            File tempFile = new File(playerSlotDir, "slot-" + toSlot + ".json.tmp");
            Files.copy(sourceBackup.toPath(), tempFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
            Files.move(tempFile.toPath(), destBackup.toPath(),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            addon.log("Copied challenge progress from slot " + fromSlot + " to slot " + toSlot +
                " for " + playerUUID);
            return true;

        } catch (IOException e) {
            addon.logError("Failed to copy challenge progress: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete challenge progress for a specific slot.
     * Called when a slot's island is deleted/reset.
     *
     * @param playerUUID The player's UUID
     * @param slotNumber The slot to clear
     */
    public void deleteSlotChallenges(UUID playerUUID, int slotNumber) {
        if (!isEnabled()) {
            return;
        }

        File playerSlotDir = new File(slotChallengesDir, playerUUID.toString());
        File slotBackup = new File(playerSlotDir, "slot-" + slotNumber + ".json");

        if (slotBackup.exists()) {
            slotBackup.delete();
            addon.log("Deleted challenge progress for " + playerUUID + " slot " + slotNumber);
        }
    }

    /**
     * Check if a slot has saved challenge progress
     *
     * @param playerUUID The player's UUID
     * @param slotNumber The slot to check
     * @return true if saved progress exists
     */
    public boolean hasSlotChallenges(UUID playerUUID, int slotNumber) {
        File playerSlotDir = new File(slotChallengesDir, playerUUID.toString());
        File slotBackup = new File(playerSlotDir, "slot-" + slotNumber + ".json");
        return slotBackup.exists();
    }

    /**
     * Get the number of completed challenges for a player.
     * Uses reflection to access the Challenges addon.
     *
     * @param playerUUID The player's UUID
     * @return Number of completed challenges, or -1 if unable to retrieve
     */
    public int getCompletedChallengeCount(UUID playerUUID) {
        if (!challengesAddonPresent) {
            return -1;
        }

        try {
            var challengesAddon = BentoBox.getInstance().getAddonsManager()
                .getAddonByName("Challenges");

            if (challengesAddon.isEmpty()) {
                return -1;
            }

            Object challenges = challengesAddon.get();

            // Get ChallengesManager
            java.lang.reflect.Method getManagerMethod = challenges.getClass()
                .getMethod("getChallengesManager");
            Object manager = getManagerMethod.invoke(challenges);

            if (manager == null) {
                return -1;
            }

            // Try to get the player data and count completed challenges
            // Method signature: getCompletedChallenges(User user) or similar
            try {
                // First try to get player data object
                java.lang.reflect.Method getAllChallengesMet = manager.getClass()
                    .getMethod("getAllChallengeDoneList", UUID.class, org.bukkit.World.class);

                // Get the BSkyBlock world
                org.bukkit.World world = addon.getGridManager().getBSkyBlockWorld();
                if (world == null) {
                    return -1;
                }

                Object result = getAllChallengesMet.invoke(manager, playerUUID, world);
                if (result instanceof java.util.List) {
                    return ((java.util.List<?>) result).size();
                }
            } catch (NoSuchMethodException e1) {
                // Try alternative method
                try {
                    java.lang.reflect.Method getCompletedChallenges = manager.getClass()
                        .getMethod("getCompletedChallenges", UUID.class);
                    Object result = getCompletedChallenges.invoke(manager, playerUUID);
                    if (result instanceof java.util.Set) {
                        return ((java.util.Set<?>) result).size();
                    }
                } catch (NoSuchMethodException e2) {
                    // Only warn once to avoid log spam
                    if (!warnedAboutMissingMethod) {
                        addon.logWarning("Could not find method to get completed challenges - challenge count feature not supported by your Challenges addon version");
                        warnedAboutMissingMethod = true;
                    }
                }
            }
        } catch (Exception e) {
            // Silently return -1 on errors to avoid log spam
        }

        return -1;
    }

    /**
     * Get the total number of challenges available.
     *
     * @return Total number of challenges, or -1 if unable to retrieve
     */
    public int getTotalChallengeCount() {
        if (!challengesAddonPresent) {
            return -1;
        }

        try {
            var challengesAddon = BentoBox.getInstance().getAddonsManager()
                .getAddonByName("Challenges");

            if (challengesAddon.isEmpty()) {
                return -1;
            }

            Object challenges = challengesAddon.get();

            // Get ChallengesManager
            java.lang.reflect.Method getManagerMethod = challenges.getClass()
                .getMethod("getChallengesManager");
            Object manager = getManagerMethod.invoke(challenges);

            if (manager == null) {
                return -1;
            }

            // Try to get all challenges
            try {
                // Get the BSkyBlock world
                org.bukkit.World world = addon.getGridManager().getBSkyBlockWorld();
                if (world == null) {
                    return -1;
                }

                java.lang.reflect.Method getAllChallenges = manager.getClass()
                    .getMethod("getAllChallenges", org.bukkit.World.class);
                Object result = getAllChallenges.invoke(manager, world);
                if (result instanceof java.util.List) {
                    return ((java.util.List<?>) result).size();
                }
            } catch (NoSuchMethodException e1) {
                // Try alternative method
                try {
                    java.lang.reflect.Method getChallengeCount = manager.getClass()
                        .getMethod("getChallengeCount");
                    Object result = getChallengeCount.invoke(manager);
                    if (result instanceof Integer) {
                        return (Integer) result;
                    }
                } catch (NoSuchMethodException e2) {
                    // Only warn once to avoid log spam
                    if (!warnedAboutMissingMethod) {
                        addon.logWarning("Could not find method to get total challenge count - challenge count feature not supported by your Challenges addon version");
                        warnedAboutMissingMethod = true;
                    }
                }
            }
        } catch (Exception e) {
            addon.logWarning("Error getting total challenge count: " + e.getMessage());
        }

        return -1;
    }
}
