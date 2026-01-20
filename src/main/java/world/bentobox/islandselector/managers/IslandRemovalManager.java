package world.bentobox.islandselector.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Manager for removing islands from the world while preserving player slot data.
 *
 * When an island is removed:
 * 1. The island is saved to the player's active slot schematic
 * 2. All blocks at the island location are cleared
 * 3. The island is unregistered from BentoBox
 * 4. The grid location is freed up
 * 5. The player's slot data is updated to mark them as "homeless" but with saved data
 *
 * This allows admins to clear inactive player islands to free up space,
 * while allowing those players to restore their islands if they return.
 */
public class IslandRemovalManager {

    private final IslandSelector addon;
    // Track ongoing removals to prevent concurrent removal of same player's island
    private final Set<UUID> pendingRemovals = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public IslandRemovalManager(IslandSelector addon) {
        this.addon = addon;
    }

    /**
     * Remove a player's island from the world, preserving their slot data.
     *
     * @param playerUUID The player's UUID
     * @param executor The user executing the command (for feedback)
     * @param callback Called with true if successful, false otherwise
     */
    public void removeIsland(UUID playerUUID, User executor, Consumer<Boolean> callback) {
        // Prevent concurrent removal of same player's island
        if (!pendingRemovals.add(playerUUID)) {
            executor.sendMessage("§cIsland removal already in progress for this player.");
            callback.accept(false);
            return;
        }

        World bskyblockWorld = addon.getGridManager().getBSkyBlockWorld();
        if (bskyblockWorld == null) {
            pendingRemovals.remove(playerUUID);
            callback.accept(false);
            return;
        }

        // Get the island
        Island island = addon.getIslands().getIsland(bskyblockWorld, playerUUID);
        if (island == null) {
            pendingRemovals.remove(playerUUID);
            callback.accept(false);
            return;
        }

        // Get active slot
        SlotData activeSlot = addon.getSlotManager().getActiveSlot(playerUUID);
        if (activeSlot == null) {
            pendingRemovals.remove(playerUUID);
            callback.accept(false);
            return;
        }

        // Get grid coordinate
        GridCoordinate coord = addon.getGridManager().getPlayerIslandCoordinate(playerUUID);

        // Teleport player to spawn if online using safe teleport
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            World spawnWorld = Bukkit.getWorld("world");
            if (spawnWorld == null && !Bukkit.getWorlds().isEmpty()) {
                spawnWorld = Bukkit.getWorlds().get(0);
            }
            if (spawnWorld == null) {
                addon.logWarning("Cannot teleport player during island removal - no spawn world available");
                return;
            }
            Location spawn = spawnWorld.getSpawnLocation();
            // Capture UUID to safely re-fetch player in callback
            final UUID teleportedPlayerUUID = playerUUID;
            new SafeSpotTeleport.Builder(addon.getPlugin())
                .entity(player)
                .location(spawn)
                .thenRun(() -> {
                    // Re-fetch player - they may have disconnected during teleport
                    Player currentPlayer = Bukkit.getPlayer(teleportedPlayerUUID);
                    if (currentPlayer != null && currentPlayer.isOnline()) {
                        currentPlayer.sendMessage("§6§l[Admin Notice]");
                        currentPlayer.sendMessage("§eYour island has been removed by an administrator.");
                        currentPlayer.sendMessage("§7Your island data has been saved. Use §f/island §7to place it at a new location.");
                    }
                })
                .buildFuture();
        }

        // Mark this as an admin removal to prevent onIslandDelete from clearing slot data prematurely
        addon.getIslandCreateListener().markAdminRemoval(playerUUID);

        // Run async operations
        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            try {
                // Step 1: Save island to slot schematic
                executor.sendMessage("§7Saving island to slot schematic...");
                boolean saved = saveIslandToSlotSchematic(playerUUID, activeSlot, island);
                if (!saved) {
                    Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                        addon.getIslandCreateListener().unmarkAdminRemoval(playerUUID);
                        executor.sendMessage("§cFailed to save island schematic. Aborting removal.");
                        callback.accept(false);
                    });
                    return;
                }

                // Step 2: Clear island blocks (back to main thread for chunk access)
                executor.sendMessage("§7Clearing island blocks...");

                // Use CompletableFuture for proper async coordination
                CompletableFuture<Boolean> clearFuture = new CompletableFuture<>();
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    try {
                        clearIslandBlocks(island);
                        clearFuture.complete(true);
                    } catch (Exception e) {
                        clearFuture.completeExceptionally(e);
                    }
                });

                // Wait for clearing to complete with proper timeout handling
                boolean clearSuccess;
                try {
                    clearSuccess = clearFuture.get(30, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    addon.logError("Timeout waiting for island clearing");
                    Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                        pendingRemovals.remove(playerUUID);
                        addon.getIslandCreateListener().unmarkAdminRemoval(playerUUID);
                        executor.sendMessage("§cTimeout clearing island blocks. Please try again.");
                        callback.accept(false);
                    });
                    return;
                } catch (Exception e) {
                    addon.logError("Error during island clearing: " + e.getMessage());
                    Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                        pendingRemovals.remove(playerUUID);
                        addon.getIslandCreateListener().unmarkAdminRemoval(playerUUID);
                        executor.sendMessage("§cFailed to clear island blocks. Please try again.");
                        callback.accept(false);
                    });
                    return;
                }

                // Brief delay to ensure block updates propagate
                Thread.sleep(500);

                // Step 3: Unregister island from BentoBox (main thread)
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    executor.sendMessage("§7Unregistering island from BentoBox...");

                    // Get a fresh reference to the island to ensure we're working with current data
                    Island currentIsland = addon.getIslands().getIsland(bskyblockWorld, playerUUID);

                    if (currentIsland != null) {
                        // Get all members before we start removing
                        java.util.Set<UUID> allMembers = new java.util.HashSet<>(currentIsland.getMemberSet());
                        addon.log("Island has " + allMembers.size() + " members to remove");

                        // Use IslandsManager.removePlayer to properly remove each member
                        // This clears all internal player-island associations in BentoBox
                        for (UUID member : allMembers) {
                            addon.getIslands().removePlayer(bskyblockWorld, member);
                            addon.log("Removed member " + member + " from island via IslandsManager");
                        }

                        // Now delete the island - it should be empty of members
                        addon.getIslands().deleteIsland(currentIsland, false, playerUUID); // false - blocks already cleared
                        addon.log("Deleted island from BentoBox");

                        // Force remove from cache as well to be absolutely sure
                        try {
                            addon.getIslands().getIslandCache().deleteIslandFromCache(currentIsland);
                            addon.log("Removed island from BentoBox cache");
                        } catch (Exception e) {
                            addon.logWarning("Could not remove island from cache: " + e.getMessage());
                        }
                    } else {
                        addon.log("Island already null in BentoBox - may have been deleted");
                        // Even if island is null, try to remove player association just to be safe
                        addon.getIslands().removePlayer(bskyblockWorld, playerUUID);
                        addon.log("Called removePlayer just in case");
                    }

                    // Step 4: Clear grid location
                    if (coord != null) {
                        addon.getGridManager().clearLocation(coord);
                    }

                    // Step 5: Update slot data to mark as homeless
                    markPlayerAsHomeless(playerUUID, activeSlot);

                    // Unmark the admin removal and clear pending state
                    addon.getIslandCreateListener().unmarkAdminRemoval(playerUUID);
                    pendingRemovals.remove(playerUUID);

                    addon.log("Removed island for " + playerUUID + " at " + coord);

                    callback.accept(true);
                });

            } catch (Exception e) {
                addon.logError("Failed to remove island for " + playerUUID + ": " + e.getMessage());
                e.printStackTrace();
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    addon.getIslandCreateListener().unmarkAdminRemoval(playerUUID);
                    pendingRemovals.remove(playerUUID);
                    callback.accept(false);
                });
            }
        });
    }

    /**
     * Save an island to the slot schematic file
     */
    private boolean saveIslandToSlotSchematic(UUID playerUUID, SlotData slotData, Island island) {
        try {
            Location center = island.getCenter();
            if (center == null) {
                addon.logError("Island center is null for player: " + playerUUID);
                return false;
            }
            World world = center.getWorld();
            if (world == null) {
                addon.logError("Island world is null for player: " + playerUUID);
                return false;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            // Get slot schematic file path
            String schematicPath = addon.getSlotManager().getSlotSchematicPath(
                playerUUID, slotData.getSlotNumber()
            );
            File schematicFile = new File(schematicPath);

            // Use SchematicUtils for safe entity handling
            boolean success = addon.getSchematicUtils().copyAndSave(center, range, false, schematicFile);

            if (success) {
                addon.log("Saved island to slot schematic: " + schematicFile.getName());
            }
            return success;

        } catch (Exception e) {
            addon.logError("Failed to save island to slot schematic: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Clear all blocks in the island area
     */
    private void clearIslandBlocks(Island island) {
        try {
            Location center = island.getCenter();
            if (center == null) {
                addon.logWarning("Cannot clear island blocks - center is null");
                return;
            }
            World world = center.getWorld();
            if (world == null) {
                addon.logWarning("Cannot clear island blocks - world is null");
                return;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            // Use SchematicUtils to clear the region
            boolean success = addon.getSchematicUtils().clearRegion(center, range);
            if (success) {
                addon.log("Cleared island blocks at " + center.getBlockX() + ", " + center.getBlockZ());
            }

        } catch (Exception e) {
            addon.logError("Failed to clear island blocks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Mark a player as homeless but with saved slot data.
     * This updates ALL slot data to indicate they need to select a new location.
     * We clear all slots (not just active) to prevent stale data from causing issues.
     */
    private void markPlayerAsHomeless(UUID playerUUID, SlotData activeSlot) {
        // Clear ALL slots for this player to prevent stale data issues
        for (SlotData slot : addon.getSlotManager().getPlayerSlots(playerUUID)) {
            // Clear the grid coordinate - they no longer have a location
            slot.setGridCoordinate(null);
            // Mark the slot as not having an island in the world, but schematic data is still saved
            slot.setHasIsland(false);
            // Clear island UUID since we deleted it from BentoBox
            slot.setIslandUUID((String) null);
            // Save each slot
            addon.getSlotManager().saveSlot(slot);
        }

        addon.log("Marked player " + playerUUID + " as homeless (all slots cleared, schematic data preserved)");
    }

    /**
     * Check if a player is homeless (has slot data but no island in world)
     */
    public boolean isHomeless(UUID playerUUID) {
        SlotData activeSlot = addon.getSlotManager().getActiveSlot(playerUUID);
        addon.log("[isHomeless] Player: " + playerUUID + ", activeSlot: " + (activeSlot != null ? activeSlot.getUniqueId() : "null"));

        if (activeSlot == null) {
            addon.log("[isHomeless] No active slot found - returning false");
            return false;
        }

        boolean hasIsland = activeSlot.hasIsland();
        boolean hasSchematic = hasSlotSchematic(playerUUID, activeSlot.getSlotNumber());
        addon.log("[isHomeless] hasIsland: " + hasIsland + ", hasSchematic: " + hasSchematic);

        // Has slot data but no island means they're homeless
        boolean result = !hasIsland && hasSchematic;
        addon.log("[isHomeless] Result: " + result);
        return result;
    }

    /**
     * Check if a slot has a saved schematic file
     */
    public boolean hasSlotSchematic(UUID playerUUID, int slotNumber) {
        String schematicPath = addon.getSlotManager().getSlotSchematicPath(playerUUID, slotNumber);
        return new File(schematicPath).exists();
    }

    /**
     * Get all slots with saved schematics for a player
     */
    public java.util.List<SlotData> getSlotsWithSchematics(UUID playerUUID) {
        java.util.List<SlotData> slotsWithData = new java.util.ArrayList<>();
        for (SlotData slot : addon.getSlotManager().getPlayerSlots(playerUUID)) {
            if (hasSlotSchematic(playerUUID, slot.getSlotNumber())) {
                slotsWithData.add(slot);
            }
        }
        return slotsWithData;
    }
}
