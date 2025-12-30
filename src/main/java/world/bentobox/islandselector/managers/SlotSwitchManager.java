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
import world.bentobox.islandselector.utils.CustomCommandExecutor;

import java.io.File;
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

                // Additional wait for FAWE to finish block operations
                Thread.sleep(2000);

                // Step 4: Load target slot schematic
                sendProgress(player, "&eLoading target island...");
                boolean loaded = loadSchematicToWorld(playerUUID, toSlot);
                if (!loaded) {
                    sendError(player, "&cFailed to load target island! Please contact an admin.");
                    return;
                }

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
     * This method loads chunks first to prevent falling through blocks.
     */
    private void teleportPlayersSafely(Island island, Player owner, Location targetLocation) {
        try {
            World world = targetLocation.getWorld();
            if (world == null) {
                addon.logError("Cannot teleport - world is null");
                return;
            }

            // First, load the chunk at the target location synchronously
            int chunkX = targetLocation.getBlockX() >> 4;
            int chunkZ = targetLocation.getBlockZ() >> 4;

            // Force load the chunk and surrounding chunks
            world.loadChunk(chunkX, chunkZ, true);
            world.loadChunk(chunkX + 1, chunkZ, true);
            world.loadChunk(chunkX - 1, chunkZ, true);
            world.loadChunk(chunkX, chunkZ + 1, true);
            world.loadChunk(chunkX, chunkZ - 1, true);

            // Wait a bit for chunks to fully generate, then teleport
            Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
                // Refresh chunks for the player to fix invisible blocks
                refreshChunksForPlayer(owner, targetLocation);

                // Use BentoBox SafeSpotTeleport for owner, targeting the home location
                new SafeSpotTeleport.Builder(addon.getPlugin())
                    .entity(owner)
                    .island(island)
                    .location(targetLocation)
                    .thenRun(() -> addon.log("Teleported " + owner.getName() + " to island home"))
                    .ifFail(() -> owner.sendMessage(colorize("&eCouldn't find safe spot - use /island go")))
                    .buildFuture();

                // Teleport team members if online using safe teleport to home
                for (UUID memberUUID : island.getMemberSet()) {
                    if (!memberUUID.equals(owner.getUniqueId())) {
                        Player member = Bukkit.getPlayer(memberUUID);
                        if (member != null && member.isOnline()) {
                            new SafeSpotTeleport.Builder(addon.getPlugin())
                                .entity(member)
                                .island(island)
                                .location(targetLocation)
                                .thenRun(() -> member.sendMessage(colorize("&eYou have been teleported to the new island slot.")))
                                .buildFuture();
                        }
                    }
                }

                // Handle visitors - teleport them away from the island
                teleportVisitorsAway(island, owner);

            }, 20L); // Wait 1 second for chunks to fully generate

        } catch (Exception e) {
            addon.logError("Failed to teleport players safely: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Refresh chunks around a location for a player to fix invisible blocks
     */
    private void refreshChunksForPlayer(Player player, Location center) {
        World world = center.getWorld();
        if (world == null) return;

        int chunkX = center.getBlockX() >> 4;
        int chunkZ = center.getBlockZ() >> 4;

        // Refresh a 3x3 chunk area around the center
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                org.bukkit.Chunk chunk = world.getChunkAt(chunkX + dx, chunkZ + dz);
                // Force the chunk to resend to the player
                chunk.load(true);
            }
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
     * Get the schematic file for a slot
     */
    private File getSchematicFile(SlotData slotData) {
        String path = addon.getSlotManager().getSlotSchematicPath(
            slotData.getPlayerUUIDAsUUID(),
            slotData.getSlotNumber()
        );
        return new File(path);
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
}
