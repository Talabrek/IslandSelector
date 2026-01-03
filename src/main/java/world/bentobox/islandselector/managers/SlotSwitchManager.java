package world.bentobox.islandselector.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.events.SlotSwitchEvent;
import world.bentobox.islandselector.models.DimensionConfig;
import world.bentobox.islandselector.utils.CustomCommandExecutor;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manager for handling slot switching operations.
 * Uses WorldEdit/FAWE when available, otherwise operations are disabled.
 */
public class SlotSwitchManager {

    private final IslandSelector addon;

    public SlotSwitchManager(IslandSelector addon) {
        this.addon = addon;
    }

    /**
     * Check if slot switching is available (requires WorldEdit/FAWE)
     */
    public boolean isAvailable() {
        return addon.isSchematicOperationsAvailable();
    }

    /**
     * Perform a complete slot switch operation
     * This is run asynchronously to avoid blocking the server
     */
    public void switchSlot(Player player, SlotData fromSlot, SlotData toSlot) {
        // Check if FAWE is available
        if (!isAvailable()) {
            player.sendMessage(colorize("&cSlot switching is not available!"));
            player.sendMessage(colorize("&cFastAsyncWorldEdit (FAWE) is required for this feature."));
            player.sendMessage(colorize("&7Ask your server admin to install FAWE."));
            return;
        }

        UUID playerUUID = player.getUniqueId();

        // Fire SlotSwitchEvent on main thread BEFORE starting the async operation
        SlotSwitchEvent event = new SlotSwitchEvent(
            player,
            fromSlot.getSlotNumber(),
            toSlot.getSlotNumber(),
            fromSlot.getSlotName(),
            toSlot.getSlotName()
        );
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            Bukkit.getPluginManager().callEvent(event);

            // Check if event was cancelled
            if (event.isCancelled()) {
                addon.log("SlotSwitchEvent cancelled for " + player.getName() +
                         " from slot " + fromSlot.getSlotNumber() + " to " + toSlot.getSlotNumber());
                if (event.getCancellationReason() != null) {
                    player.sendMessage("§c" + event.getCancellationReason());
                } else {
                    player.sendMessage("§cSlot switch cancelled.");
                }
                return;
            }

            // Event not cancelled - proceed with the switch asynchronously
            performSlotSwitchAsync(player, fromSlot, toSlot);
        });
    }

    /**
     * Internal method to perform the actual slot switch asynchronously
     */
    private void performSlotSwitchAsync(Player player, SlotData fromSlot, SlotData toSlot) {
        UUID playerUUID = player.getUniqueId();

        // Step 0: Teleport player to SERVER spawn for safety during the switch
        Location serverSpawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        // Use safe teleport for server spawn
        new SafeSpotTeleport.Builder(addon.getPlugin())
            .entity(player)
            .location(serverSpawn)
            .buildFuture();
        sendProgress(player, "&eTeleported to spawn for safety during slot switch...");

        // Run asynchronously to avoid blocking main thread
        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            try {
                // Step 1: Send progress message
                sendProgress(player, "&eStarting slot switch...");

                // Step 1.5: Create backup if configured
                if (addon.getSettings().isBackupOnSwitch()) {
                    sendProgress(player, "&eCreating backup of current island...");
                    boolean backed = addon.getBackupManager().createBackup(playerUUID, fromSlot.getSlotNumber());
                    if (backed) {
                        addon.log("Auto-backup created for " + player.getName() + " slot " + fromSlot.getSlotNumber());
                    } else {
                        addon.logError("Auto-backup failed for " + player.getName() + " slot " + fromSlot.getSlotNumber() + " - continuing switch anyway");
                        // Don't cancel switch if backup fails - it's a safety feature, not critical
                    }
                }

                // Step 1.6: Save challenge progress for the current slot (Challenges addon integration)
                ChallengesIntegration challenges = addon.getChallengesIntegration();
                if (challenges.isEnabled()) {
                    sendProgress(player, "&eSaving challenge progress...");
                    challenges.saveSlotChallenges(playerUUID, fromSlot.getSlotNumber());
                }

                // Step 1.7: Save island homes for the current slot
                sendProgress(player, "&eSaving island homes...");
                World bskyblockWorld = addon.getGridManager().getBSkyBlockWorld();
                Island fromIsland = addon.getIslands().getIsland(bskyblockWorld, playerUUID);
                if (fromIsland != null) {
                    addon.getSlotManager().saveIslandHomes(playerUUID, fromSlot.getSlotNumber(), fromIsland);
                }

                // Step 2: Save current island to schematic
                sendProgress(player, "&eSaving current island...");
                boolean saved = saveIslandToSchematic(playerUUID, fromSlot);
                if (!saved) {
                    sendError(player, "&cFailed to save current island! Switch cancelled.");
                    return;
                }

                // Step 3: Clear current island blocks and entities from world
                // This MUST happen on main thread and complete before loading new schematic
                sendProgress(player, "&eClearing current island from world...");

                // TODO BUG: Race condition - Using wait/notify with Bukkit scheduler is fragile.
                // If the scheduled task runs before we enter the wait(), we miss the notify().
                // Also, InterruptedException is not properly handled.
                // Fix: Use CompletableFuture or a callback-based approach instead.
                // Use a synchronization mechanism to wait for clearing
                final boolean[] clearSuccess = {false};
                final Object clearLock = new Object();

                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    synchronized (clearLock) {
                        clearSuccess[0] = clearIslandBlocksAndEntities(playerUUID, fromSlot);
                        clearLock.notify();
                    }
                });

                // Wait for clearing to complete (with timeout)
                synchronized (clearLock) {
                    clearLock.wait(10000); // 10 second timeout
                }

                if (!clearSuccess[0]) {
                    sendError(player, "&cFailed to clear current island! Please contact an admin.");
                    return;
                }

                // TODO BUG: Thread.sleep() is a fragile way to wait for FAWE operations.
                // FAWE operations are async and may take longer than 2 seconds on large islands.
                // Fix: Use FAWE's EditSession callbacks or CompletableFuture to properly wait for completion.
                // Additional wait for FAWE to finish block operations
                Thread.sleep(2000);

                // Step 4: Load target slot schematic
                sendProgress(player, "&eLoading target island...");
                boolean loaded = loadSchematicToWorld(playerUUID, toSlot);
                if (!loaded) {
                    sendError(player, "&cFailed to load target island! Please contact an admin.");
                    return;
                }

                // TODO BUG: Same Thread.sleep() issue as above - FAWE paste is async
                // Wait for pasting to complete
                Thread.sleep(1500);

                // Step 4.5: Restore challenge progress for the target slot (Challenges addon integration)
                if (challenges.isEnabled()) {
                    sendProgress(player, "&eRestoring challenge progress...");
                    challenges.restoreSlotChallenges(playerUUID, toSlot.getSlotNumber());
                }

                // Step 4.6: Restore island homes for the target slot
                sendProgress(player, "&eRestoring island homes...");
                Island toIsland = addon.getIslands().getIsland(bskyblockWorld, playerUUID);
                if (toIsland != null) {
                    addon.getSlotManager().restoreIslandHomes(playerUUID, toSlot.getSlotNumber(), toIsland);
                }

                // Step 4.7: Update blueprint permissions for the target slot
                // This must run on main thread as it modifies player permissions
                String blueprintName = toSlot.getBlueprintBundle();
                if (blueprintName != null && !blueprintName.isEmpty()) {
                    Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                        addon.getBlueprintChallengesManager().updateBlueprintPermissions(player, blueprintName);
                    });
                }

                // Step 5: Update database (back to main thread)
                final int fromSlotNumber = fromSlot.getSlotNumber();
                final int toSlotNumber = toSlot.getSlotNumber();
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    // Update active status
                    addon.getSlotManager().switchSlot(playerUUID, toSlotNumber);

                    // Step 6: Teleport player and team members
                    sendProgress(player, "&eTeleporting to new island...");
                    teleportPlayersToSlot(playerUUID, player, toSlot);

                    // Step 7: Send completion message and execute custom commands
                    sendSuccess(player, "&aSlot switch complete! Welcome to &e" + toSlot.getSlotName());

                    // Execute custom commands after slot switch (with delay to ensure teleport completes)
                    Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
                        executeSlotSwitchCommands(player, fromSlotNumber, toSlotNumber);
                    }, 30L); // 1.5 second delay after completion message
                });

            } catch (Exception e) {
                addon.logError("Error during slot switch for " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
                sendError(player, "&cAn error occurred during slot switch. Please contact an admin.");
            }
        });
    }

    /**
     * Save an island to a schematic file
     * @param playerUUID The player's UUID to find their island
     * @param slotData The slot data for file naming
     */
    private boolean saveIslandToSchematic(UUID playerUUID, SlotData slotData) {
        try {
            // Get island directly by player UUID (not by slot's stored island UUID)
            World bskyblockWorld = addon.getGridManager().getBSkyBlockWorld();
            if (bskyblockWorld == null) {
                addon.logError("BSkyBlock world not available for saving schematic");
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
                return false;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            // Get schematic file path
            File schematicFile = getSchematicFile(slotData);

            // Step 1: Save entities SEPARATELY (FAWE entity handling is unreliable)
            // This must be done on main thread - schedule it
            final int finalRange = range;
            final Location finalCenter = center;
            final World finalWorld = world;

            // Save entities synchronously if we're on main thread, otherwise schedule
            if (Bukkit.isPrimaryThread()) {
                int entitiesSaved = addon.getEntityStorage().saveEntities(finalWorld, finalCenter, finalRange, schematicFile);
                addon.log("Saved " + entitiesSaved + " entities for slot: " + slotData.getUniqueId());
            } else {
                // We need to save entities on main thread - use a future
                try {
                    Bukkit.getScheduler().callSyncMethod(addon.getPlugin(), () -> {
                        int saved = addon.getEntityStorage().saveEntities(finalWorld, finalCenter, finalRange, schematicFile);
                        addon.log("Saved " + saved + " entities for slot: " + slotData.getUniqueId());
                        return saved;
                    }).get(); // Wait for completion
                } catch (Exception e) {
                    addon.logWarning("Failed to save entities: " + e.getMessage());
                }
            }

            // Step 2: Save blocks using FAWE (without entities - we handle those separately)
            boolean success = addon.getSchematicUtils().copyAndSave(center, range, true, schematicFile);

            if (success) {
                addon.log("Saved island schematic for slot: " + slotData.getUniqueId());
            }
            return success;

        } catch (Exception e) {
            addon.logError("Failed to save schematic for slot " + slotData.getUniqueId() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load a schematic file to the world
     * @param playerUUID The player's UUID to find their island location
     * @param slotData The slot data for file naming
     */
    private boolean loadSchematicToWorld(UUID playerUUID, SlotData slotData) {
        try {
            // Get schematic file
            File schematicFile = getSchematicFile(slotData);
            if (!schematicFile.exists()) {
                addon.logError("Schematic file not found for slot: " + slotData.getUniqueId());
                return false;
            }

            // Get island location directly by player UUID (not by slot's stored island UUID)
            World bskyblockWorld = addon.getGridManager().getBSkyBlockWorld();
            if (bskyblockWorld == null) {
                addon.logError("BSkyBlock world not available for loading schematic");
                return false;
            }

            Island island = addon.getIslands().getIsland(bskyblockWorld, playerUUID);
            if (island == null) {
                addon.logError("Island not found for player: " + playerUUID + " in slot: " + slotData.getUniqueId());
                return false;
            }

            Location center = island.getCenter();
            if (center.getWorld() == null) {
                return false;
            }

            // Step 1: Use SchematicUtils to load and paste the schematic (blocks only)
            boolean success = addon.getSchematicUtils().loadAndPaste(schematicFile, center);
            if (!success) {
                addon.logError("Failed to paste schematic for slot: " + slotData.getUniqueId());
                return false;
            }
            addon.log("Loaded island schematic for slot: " + slotData.getUniqueId());

            // Step 2: Restore entities SEPARATELY (must be on main thread)
            final Location finalCenter = center;
            final World finalWorld = center.getWorld();

            if (Bukkit.isPrimaryThread()) {
                int entitiesRestored = addon.getEntityStorage().loadEntities(finalWorld, finalCenter, schematicFile);
                addon.log("Restored " + entitiesRestored + " entities for slot: " + slotData.getUniqueId());
            } else {
                // Schedule entity restoration on main thread
                try {
                    Bukkit.getScheduler().callSyncMethod(addon.getPlugin(), () -> {
                        int restored = addon.getEntityStorage().loadEntities(finalWorld, finalCenter, schematicFile);
                        addon.log("Restored " + restored + " entities for slot: " + slotData.getUniqueId());
                        return restored;
                    }).get(); // Wait for completion
                } catch (Exception e) {
                    addon.logWarning("Failed to restore entities: " + e.getMessage());
                }
            }

            return true;

        } catch (Exception e) {
            addon.logError("Failed to load schematic for slot " + slotData.getUniqueId() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Clear island blocks AND entities from the world.
     * Must be called from main thread for entity removal.
     *
     * @param playerUUID The player's UUID to find their island
     * @param slotData The slot data (for logging)
     * @return true if successful, false otherwise
     */
    private boolean clearIslandBlocksAndEntities(UUID playerUUID, SlotData slotData) {
        try {
            // Get island directly by player UUID (not by slot's stored island UUID)
            World bskyblockWorld = addon.getGridManager().getBSkyBlockWorld();
            if (bskyblockWorld == null) {
                addon.logError("BSkyBlock world not available for clearing");
                return false;
            }

            Island island = addon.getIslands().getIsland(bskyblockWorld, playerUUID);
            if (island == null) {
                addon.logError("Island not found for player: " + playerUUID + " when clearing: " + slotData.getUniqueId());
                return false;
            }

            Location center = island.getCenter();
            World world = center.getWorld();
            if (world == null) {
                return false;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            // Step 1: Remove entities FIRST (must be on main thread - we're already here)
            addon.log("Removing entities in region for slot: " + slotData.getUniqueId());
            int removedEntities = addon.getWorldEditIntegration().removeEntitiesInRegion(world, center, range);
            addon.log("Removed " + removedEntities + " entities for slot: " + slotData.getUniqueId());

            // Step 2: Clear blocks using FAWE
            boolean success = addon.getSchematicUtils().clearRegion(center, range);
            if (success) {
                addon.log("Cleared island blocks for slot: " + slotData.getUniqueId());
            }

            return success;

        } catch (Exception e) {
            addon.logError("Failed to clear island for slot " + slotData.getUniqueId() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Teleport player and team members to the new slot's island
     * @param playerUUID The player's UUID to find their island
     * @param owner The player who owns the island
     * @param slotData The slot data (for logging)
     */
    private void teleportPlayersToSlot(UUID playerUUID, Player owner, SlotData slotData) {
        try {
            // Get island directly by player UUID (not by slot's stored island UUID)
            World bskyblockWorld = addon.getGridManager().getBSkyBlockWorld();
            if (bskyblockWorld == null) {
                addon.logError("BSkyBlock world not available for teleporting");
                return;
            }

            Island island = addon.getIslands().getIsland(bskyblockWorld, playerUUID);
            if (island == null) {
                addon.logError("Island not found for player: " + playerUUID + " when teleporting: " + slotData.getUniqueId());
                return;
            }

            // Use the default home location (same as /island home)
            // Empty string "" is the default home name in BentoBox
            Location targetLocation = island.getHome("");
            if (targetLocation == null) {
                targetLocation = island.getSpawnPoint(org.bukkit.World.Environment.NORMAL);
            }
            if (targetLocation == null) {
                targetLocation = island.getCenter();
            }

            // Safely teleport with chunk loading
            teleportPlayersSafely(island, owner, targetLocation);

        } catch (Exception e) {
            addon.logError("Failed to teleport players for slot " + slotData.getUniqueId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Safely teleport player and team members to island home location.
     * This method loads chunks first to prevent falling through blocks,
     * and uses a chunk refresh technique to fix invisible blocks after FAWE paste.
     */
    private void teleportPlayersSafely(Island island, Player owner, Location targetLocation) {
        try {
            World world = targetLocation.getWorld();
            if (world == null) {
                addon.logError("Cannot teleport - world is null");
                return;
            }

            // Pre-load all chunks in the island area
            int islandSpacing = addon.getIslandSpacing();
            int range = islandSpacing / 2;
            int chunkRange = (range >> 4) + 1;
            int centerChunkX = targetLocation.getBlockX() >> 4;
            int centerChunkZ = targetLocation.getBlockZ() >> 4;

            for (int dx = -chunkRange; dx <= chunkRange; dx++) {
                for (int dz = -chunkRange; dz <= chunkRange; dz++) {
                    world.loadChunk(centerChunkX + dx, centerChunkZ + dz, true);
                }
            }

            // Wait for chunks to fully generate and FAWE to complete, then teleport
            Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
                // Step 1: Teleport players using the chunk refresh teleport technique
                teleportWithChunkRefresh(owner, island, targetLocation);

                // Teleport team members with the same technique
                for (UUID memberUUID : island.getMemberSet()) {
                    if (!memberUUID.equals(owner.getUniqueId())) {
                        Player member = Bukkit.getPlayer(memberUUID);
                        if (member != null && member.isOnline()) {
                            teleportWithChunkRefresh(member, island, targetLocation);
                            member.sendMessage(colorize("&eYou have been teleported to the new island slot."));
                        }
                    }
                }

                // Handle visitors - teleport them away from the island
                teleportVisitorsAway(island, owner);

                // Schedule additional chunk refresh for nearby players after teleports complete
                Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
                    refreshChunksForPlayer(owner, targetLocation);
                }, 20L);

            }, 30L); // Wait 1.5 seconds for chunks to fully generate

        } catch (Exception e) {
            addon.logError("Failed to teleport players safely: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Teleport a player with chunk refresh to fix invisible blocks.
     * This works by briefly teleporting the player far away to force chunk unload,
     * then teleporting them to the destination. This ensures the client fetches fresh chunk data.
     */
    private void teleportWithChunkRefresh(Player player, Island island, Location destination) {
        World world = destination.getWorld();
        if (world == null) {
            addon.logError("Cannot teleport - world is null");
            return;
        }

        // Create a temporary location far from the island to force chunk unload
        // Move player 1000 blocks away temporarily
        Location tempLocation = destination.clone().add(1000, 0, 1000);
        tempLocation.setY(world.getHighestBlockYAt(tempLocation) + 10);

        // First teleport: move player far away (forces client to unload island chunks)
        player.teleport(tempLocation);

        // Second teleport: after a short delay, teleport to actual destination
        // The client will request fresh chunk data since the chunks are now "new" to it
        Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
            // Use SafeSpotTeleport for the final destination
            new SafeSpotTeleport.Builder(addon.getPlugin())
                .entity(player)
                .island(island)
                .location(destination)
                .thenRun(() -> {
                    addon.log("Teleported " + player.getName() + " to island home with chunk refresh");
                    // Send one more chunk refresh after landing
                    Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
                        sendChunkUpdatesToPlayer(player, destination);
                    }, 10L);
                })
                .ifFail(() -> player.sendMessage(colorize("&eCouldn't find safe spot - use /island go")))
                .buildFuture();
        }, 5L); // Very short delay - just enough for client to register the position change
    }

    /**
     * Send chunk updates to a player for the area around a location.
     * This uses block state updates which are more reliable than refreshChunk.
     */
    private void sendChunkUpdatesToPlayer(Player player, Location center) {
        World world = center.getWorld();
        if (world == null) return;

        int islandSpacing = addon.getIslandSpacing();
        int range = islandSpacing / 2;
        int chunkRange = (range >> 4) + 1;
        int centerChunkX = center.getBlockX() >> 4;
        int centerChunkZ = center.getBlockZ() >> 4;

        // For each chunk in the island area
        for (int dx = -chunkRange; dx <= chunkRange; dx++) {
            for (int dz = -chunkRange; dz <= chunkRange; dz++) {
                org.bukkit.Chunk chunk = world.getChunkAt(centerChunkX + dx, centerChunkZ + dz);

                // Check if player can see this chunk
                int playerChunkX = player.getLocation().getBlockX() >> 4;
                int playerChunkZ = player.getLocation().getBlockZ() >> 4;
                int viewDistance = Bukkit.getViewDistance();

                if (Math.abs(chunk.getX() - playerChunkX) <= viewDistance &&
                    Math.abs(chunk.getZ() - playerChunkZ) <= viewDistance) {

                    // Try to force chunk resend
                    try {
                        world.refreshChunk(chunk.getX(), chunk.getZ());
                    } catch (Exception ignored) {
                        // refreshChunk is deprecated but still works on some versions
                    }
                }
            }
        }
    }

    /**
     * Refresh chunks around a location for a player and nearby players to fix invisible blocks.
     * This is necessary after FAWE schematic pasting which doesn't always send proper chunk updates.
     */
    private void refreshChunksForPlayer(Player player, Location center) {
        World world = center.getWorld();
        if (world == null) return;

        // Get island bounds for proper chunk coverage
        int islandSpacing = addon.getIslandSpacing();
        int range = islandSpacing / 2;
        int chunkRange = (range >> 4) + 2; // Convert to chunks and add buffer

        int centerChunkX = center.getBlockX() >> 4;
        int centerChunkZ = center.getBlockZ() >> 4;

        // Collect all chunks in the island area
        java.util.List<org.bukkit.Chunk> chunksToRefresh = new java.util.ArrayList<>();
        for (int dx = -chunkRange; dx <= chunkRange; dx++) {
            for (int dz = -chunkRange; dz <= chunkRange; dz++) {
                org.bukkit.Chunk chunk = world.getChunkAt(centerChunkX + dx, centerChunkZ + dz);
                chunk.load(true);
                chunksToRefresh.add(chunk);
            }
        }

        // Schedule chunk refresh for all nearby players after a short delay
        // This gives FAWE time to finish any pending operations
        Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
            refreshChunksForNearbyPlayers(world, center, chunksToRefresh);
        }, 10L); // 0.5 second delay

        // Do another refresh after a longer delay for any stragglers
        Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
            refreshChunksForNearbyPlayers(world, center, chunksToRefresh);
        }, 40L); // 2 second delay
    }

    /**
     * Refresh chunks for all players near the given location.
     * Uses multiple techniques to ensure clients receive updated chunk data.
     */
    private void refreshChunksForNearbyPlayers(World world, Location center, java.util.List<org.bukkit.Chunk> chunks) {
        int refreshRadius = addon.getIslandSpacing() + 100; // Island size + view distance buffer

        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) < refreshRadius * refreshRadius) {
                forceChunkResendToPlayer(player, chunks);
            }
        }
    }

    /**
     * Force resend chunk data to a specific player.
     * Uses Paper's native chunk resend if available, otherwise falls back to workarounds.
     */
    private void forceChunkResendToPlayer(Player player, java.util.List<org.bukkit.Chunk> chunks) {
        try {
            for (org.bukkit.Chunk chunk : chunks) {
                // Check if player can see this chunk (within view distance)
                int playerChunkX = player.getLocation().getBlockX() >> 4;
                int playerChunkZ = player.getLocation().getBlockZ() >> 4;
                int viewDistance = Bukkit.getViewDistance();

                if (Math.abs(chunk.getX() - playerChunkX) <= viewDistance &&
                    Math.abs(chunk.getZ() - playerChunkZ) <= viewDistance) {

                    // Method 1: Try Paper's chunk refresh (most reliable)
                    try {
                        // Paper 1.19+ has player.getWorld().refreshChunk() but it's deprecated
                        // Instead, use the chunk's addPluginChunkTicket to force reload
                        boolean hadTicket = chunk.isForceLoaded();
                        if (!hadTicket) {
                            chunk.addPluginChunkTicket(addon.getPlugin());
                        }

                        // Force chunk to regenerate lighting and resend
                        chunk.getWorld().refreshChunk(chunk.getX(), chunk.getZ());

                        if (!hadTicket) {
                            // Schedule ticket removal
                            Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
                                chunk.removePluginChunkTicket(addon.getPlugin());
                            }, 5L);
                        }
                    } catch (Exception e) {
                        // Fallback: deprecated but still works on some versions
                        chunk.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
                    }
                }
            }
        } catch (Exception e) {
            addon.logWarning("Chunk refresh failed: " + e.getMessage());
        }
    }

    /**
     * Find a safe location to teleport to (standing on a solid block)
     */
    private Location findSafeLocation(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return center;
        }

        int x = center.getBlockX();
        int z = center.getBlockZ();

        // Start from a reasonable height and work down to find solid ground
        int startY = Math.min(center.getBlockY() + 10, world.getMaxHeight() - 2);

        for (int y = startY; y > world.getMinHeight(); y--) {
            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            Location below = new Location(world, x, y - 1, z);
            Location above = new Location(world, x, y + 1, z);

            // Check if we can stand here: solid below, air at feet and head
            if (below.getBlock().getType().isSolid() &&
                !loc.getBlock().getType().isSolid() &&
                !above.getBlock().getType().isSolid()) {

                return loc;
            }
        }

        // Fallback: return center with a higher Y to be safe
        return new Location(world, x + 0.5, 100, z + 0.5);
    }

    /**
     * Teleport visitors away from the island during slot switch
     * Visitors are non-team members who are currently on the island
     */
    private void teleportVisitorsAway(Island island, Player owner) {
        try {
            // Get all online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Skip if player is the owner or a team member
                if (player.getUniqueId().equals(owner.getUniqueId()) ||
                    island.getMemberSet().contains(player.getUniqueId())) {
                    continue;
                }

                // Check if player is on the island
                if (island.onIsland(player.getLocation())) {
                    // This is a visitor - teleport them away
                    handleVisitorTeleport(player, island);
                }
            }

        } catch (Exception e) {
            addon.logError("Failed to teleport visitors: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle teleporting a single visitor away from the island
     */
    private void handleVisitorTeleport(Player visitor, Island island) {
        // Get config settings
        int teleportDelay = addon.getSettings().getVisitorTeleportDelay();
        String warningMessage = addon.getSettings().getVisitorWarningMessage();

        // Send warning message if configured
        if (warningMessage != null && !warningMessage.isEmpty()) {
            visitor.sendMessage(colorize(warningMessage));
        }

        // Schedule teleportation after delay
        Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
            // Determine where to teleport the visitor
            Location destination = getVisitorTeleportDestination(visitor);

            if (destination != null) {
                // Use safe teleport for visitor destination
                new SafeSpotTeleport.Builder(addon.getPlugin())
                    .entity(visitor)
                    .location(destination)
                    .thenRun(() -> visitor.sendMessage(colorize("&eYou have been teleported away as the island is switching slots.")))
                    .ifFail(() -> {
                        // Fallback: teleport to world spawn
                        new SafeSpotTeleport.Builder(addon.getPlugin())
                            .entity(visitor)
                            .location(visitor.getWorld().getSpawnLocation())
                            .buildFuture();
                        visitor.sendMessage(colorize("&eYou have been teleported to spawn as the island is switching slots."));
                    })
                    .buildFuture();
            } else {
                // Fallback: teleport to world spawn
                new SafeSpotTeleport.Builder(addon.getPlugin())
                    .entity(visitor)
                    .location(visitor.getWorld().getSpawnLocation())
                    .thenRun(() -> visitor.sendMessage(colorize("&eYou have been teleported to spawn as the island is switching slots.")))
                    .buildFuture();
            }
        }, teleportDelay);
    }

    /**
     * Determine where to teleport a visitor
     * Priority: Their own island > Their team's island > World spawn
     */
    private Location getVisitorTeleportDestination(Player visitor) {
        // Try to get visitor's own island
        Island visitorIsland = addon.getIslands().getIsland(visitor.getWorld(), visitor.getUniqueId());

        if (visitorIsland != null) {
            // Visitor has an island - send them home
            Location spawnPoint = visitorIsland.getSpawnPoint(org.bukkit.World.Environment.NORMAL);
            if (spawnPoint != null) {
                return spawnPoint;
            }
            return visitorIsland.getCenter();
        }

        // Visitor has no island - send to world spawn
        return visitor.getWorld().getSpawnLocation();
    }

    /**
     * Get the schematic file for a slot (legacy single-dimension)
     */
    private File getSchematicFile(SlotData slotData) {
        String path = addon.getSlotManager().getSlotSchematicPath(
            slotData.getPlayerUUIDAsUUID(),
            slotData.getSlotNumber()
        );
        return new File(path);
    }

    /**
     * Get the schematic file for a slot in a specific dimension
     * @param slotData The slot data
     * @param dimensionKey The dimension key (e.g., "overworld", "nether")
     * @return The schematic file for this dimension
     */
    private File getSchematicFile(SlotData slotData, String dimensionKey) {
        String path = addon.getSlotManager().getSlotSchematicPath(
            slotData.getPlayerUUIDAsUUID(),
            slotData.getSlotNumber(),
            dimensionKey
        );
        return new File(path);
    }

    /**
     * Check if multi-dimension mode is enabled
     */
    private boolean isMultiDimensionEnabled() {
        DimensionManager dimManager = addon.getDimensionManager();
        return dimManager != null && dimManager.isEnabled();
    }

    /**
     * Get the list of enabled dimensions for slot switching
     */
    private List<DimensionConfig> getEnabledDimensions() {
        DimensionManager dimManager = addon.getDimensionManager();
        if (dimManager != null && dimManager.isEnabled()) {
            return dimManager.getEnabledDimensions();
        }
        return List.of();
    }

    /**
     * Send progress message to player
     */
    private void sendProgress(Player player, String message) {
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            // Send as chat message for compatibility
            player.sendMessage(colorize(message));
        });
    }

    /**
     * Send error message to player
     */
    private void sendError(Player player, String message) {
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            player.sendMessage(colorize(message));
        });
    }

    /**
     * Send success message to player
     */
    private void sendSuccess(Player player, String message) {
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            player.sendMessage(colorize(message));
        });
    }

    private String colorize(String text) {
        return text.replace("&", "\u00A7");
    }

    /**
     * Execute custom commands after slot switch completes.
     * Commands are configured in config.yml under custom-commands.slot-switch
     */
    private void executeSlotSwitchCommands(Player player, int fromSlot, int toSlot) {
        if (!addon.getSettings().isSlotSwitchCommandsEnabled()) {
            return;
        }

        var commands = addon.getSettings().getSlotSwitchCommands();
        if (commands == null || commands.isEmpty()) {
            return;
        }

        // Get island center for world coordinates
        World bskyblockWorld = addon.getGridManager().getBSkyBlockWorld();
        int worldX = 0;
        int worldZ = 0;
        if (bskyblockWorld != null) {
            Island island = addon.getIslands().getIsland(bskyblockWorld, player.getUniqueId());
            if (island != null && island.getCenter() != null) {
                worldX = island.getCenter().getBlockX();
                worldZ = island.getCenter().getBlockZ();
            }
        }

        Map<String, String> placeholders = CustomCommandExecutor.createSlotSwitchPlaceholders(
            player, fromSlot, toSlot, worldX, worldZ
        );

        addon.getCustomCommandExecutor().executeCommands(
            player,
            commands,
            addon.getSettings().getSlotSwitchCommandScope(),
            placeholders
        );
    }

    // ==================== MULTI-DIMENSION SUPPORT ====================

    /**
     * Save island schematic for a specific dimension.
     *
     * @param playerUUID The player's UUID
     * @param slotData The slot data
     * @param dimensionKey The dimension key
     * @param world The dimension world
     * @return true if save was successful
     */
    private boolean saveIslandToSchematicForDimension(UUID playerUUID, SlotData slotData,
                                                       String dimensionKey, World world) {
        try {
            Island island = addon.getIslands().getIsland(world, playerUUID);
            if (island == null) {
                addon.log("No island found for player in dimension " + dimensionKey);
                return true; // Not an error - player may not have island in this dimension
            }

            Location center = island.getCenter();
            if (center == null || center.getWorld() == null) {
                return true;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            File schematicFile = getSchematicFile(slotData, dimensionKey);

            // Ensure parent directory exists
            schematicFile.getParentFile().mkdirs();

            // Save entities if on main thread
            final int finalRange = range;
            final Location finalCenter = center;
            final World finalWorld = world;

            if (Bukkit.isPrimaryThread()) {
                addon.getEntityStorage().saveEntities(finalWorld, finalCenter, finalRange, schematicFile);
            } else {
                try {
                    Bukkit.getScheduler().callSyncMethod(addon.getPlugin(), () -> {
                        addon.getEntityStorage().saveEntities(finalWorld, finalCenter, finalRange, schematicFile);
                        return true;
                    }).get();
                } catch (Exception e) {
                    addon.logWarning("Failed to save entities for dimension " + dimensionKey + ": " + e.getMessage());
                }
            }

            // Save blocks
            boolean success = addon.getSchematicUtils().copyAndSave(center, range, true, schematicFile);
            if (success) {
                addon.log("Saved island schematic for " + dimensionKey + ": " + slotData.getUniqueId());
            }
            return success;

        } catch (Exception e) {
            addon.logError("Failed to save schematic for dimension " + dimensionKey + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Load island schematic for a specific dimension.
     *
     * @param playerUUID The player's UUID
     * @param slotData The slot data
     * @param dimensionKey The dimension key
     * @param world The dimension world
     * @return true if load was successful
     */
    private boolean loadSchematicForDimension(UUID playerUUID, SlotData slotData,
                                               String dimensionKey, World world) {
        try {
            File schematicFile = getSchematicFile(slotData, dimensionKey);
            if (!schematicFile.exists()) {
                addon.log("No schematic file for dimension " + dimensionKey + " - skipping");
                return true; // Not an error - may not have saved this dimension yet
            }

            Island island = addon.getIslands().getIsland(world, playerUUID);
            if (island == null) {
                addon.logError("Island not found for player in dimension " + dimensionKey);
                return false;
            }

            Location center = island.getCenter();
            if (center == null || center.getWorld() == null) {
                return false;
            }

            // Load and paste blocks
            boolean success = addon.getSchematicUtils().loadAndPaste(schematicFile, center);
            if (!success) {
                addon.logError("Failed to paste schematic for dimension " + dimensionKey);
                return false;
            }

            // Restore entities
            final Location finalCenter = center;
            final World finalWorld = world;

            if (Bukkit.isPrimaryThread()) {
                addon.getEntityStorage().loadEntities(finalWorld, finalCenter, schematicFile);
            } else {
                try {
                    Bukkit.getScheduler().callSyncMethod(addon.getPlugin(), () -> {
                        addon.getEntityStorage().loadEntities(finalWorld, finalCenter, schematicFile);
                        return true;
                    }).get();
                } catch (Exception e) {
                    addon.logWarning("Failed to restore entities for dimension " + dimensionKey + ": " + e.getMessage());
                }
            }

            addon.log("Loaded island schematic for " + dimensionKey + ": " + slotData.getUniqueId());
            return true;

        } catch (Exception e) {
            addon.logError("Failed to load schematic for dimension " + dimensionKey + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Clear island blocks and entities for a specific dimension.
     * Must be called from main thread.
     *
     * @param playerUUID The player's UUID
     * @param slotData The slot data
     * @param dimensionKey The dimension key
     * @param world The dimension world
     * @return true if clear was successful
     */
    private boolean clearIslandForDimension(UUID playerUUID, SlotData slotData,
                                             String dimensionKey, World world) {
        try {
            Island island = addon.getIslands().getIsland(world, playerUUID);
            if (island == null) {
                return true; // No island in this dimension
            }

            Location center = island.getCenter();
            if (center == null || center.getWorld() == null) {
                return true;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            // Remove entities
            addon.getWorldEditIntegration().removeEntitiesInRegion(world, center, range);

            // Clear blocks
            boolean success = addon.getSchematicUtils().clearRegion(center, range);
            if (success) {
                addon.log("Cleared island for " + dimensionKey + ": " + slotData.getUniqueId());
            }

            return success;

        } catch (Exception e) {
            addon.logError("Failed to clear island for dimension " + dimensionKey + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Save all dimension islands to schematics.
     *
     * @param playerUUID The player's UUID
     * @param slotData The slot data
     * @return true if all saves were successful
     */
    public boolean saveAllDimensionIslands(UUID playerUUID, SlotData slotData) {
        if (!isMultiDimensionEnabled()) {
            return saveIslandToSchematic(playerUUID, slotData);
        }

        boolean success = true;
        DimensionManager dimManager = addon.getDimensionManager();

        for (DimensionConfig config : dimManager.getEnabledDimensions()) {
            World world = dimManager.getWorld(config.getDimensionKey());
            if (world != null) {
                if (!saveIslandToSchematicForDimension(playerUUID, slotData, config.getDimensionKey(), world)) {
                    success = false;
                }
            }
        }

        return success;
    }

    /**
     * Load all dimension islands from schematics.
     *
     * @param playerUUID The player's UUID
     * @param slotData The slot data
     * @return true if all loads were successful
     */
    public boolean loadAllDimensionIslands(UUID playerUUID, SlotData slotData) {
        if (!isMultiDimensionEnabled()) {
            return loadSchematicToWorld(playerUUID, slotData);
        }

        boolean success = true;
        DimensionManager dimManager = addon.getDimensionManager();

        for (DimensionConfig config : dimManager.getEnabledDimensions()) {
            World world = dimManager.getWorld(config.getDimensionKey());
            if (world != null) {
                if (!loadSchematicForDimension(playerUUID, slotData, config.getDimensionKey(), world)) {
                    success = false;
                }
            }
        }

        return success;
    }

    /**
     * Clear all dimension islands.
     * Must be called from main thread.
     *
     * @param playerUUID The player's UUID
     * @param slotData The slot data
     * @return true if all clears were successful
     */
    public boolean clearAllDimensionIslands(UUID playerUUID, SlotData slotData) {
        if (!isMultiDimensionEnabled()) {
            return clearIslandBlocksAndEntities(playerUUID, slotData);
        }

        boolean success = true;
        DimensionManager dimManager = addon.getDimensionManager();

        for (DimensionConfig config : dimManager.getEnabledDimensions()) {
            World world = dimManager.getWorld(config.getDimensionKey());
            if (world != null) {
                if (!clearIslandForDimension(playerUUID, slotData, config.getDimensionKey(), world)) {
                    success = false;
                }
            }
        }

        return success;
    }
}
