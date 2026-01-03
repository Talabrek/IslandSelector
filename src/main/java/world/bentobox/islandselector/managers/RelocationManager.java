package world.bentobox.islandselector.managers;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.RelocationData;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.events.IslandRelocateEvent;
import world.bentobox.islandselector.models.DimensionConfig;
import world.bentobox.islandselector.utils.CustomCommandExecutor;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manager for handling island relocation operations
 */
public class RelocationManager {

    private final IslandSelector addon;
    private final Database<RelocationData> database;
    private Economy economy;

    public RelocationManager(IslandSelector addon) {
        this.addon = addon;
        this.database = new Database<>(addon, RelocationData.class);
        setupEconomy();
    }

    /**
     * Setup Vault economy if available
     */
    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            addon.log("Vault not found - relocation costs will be disabled");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            addon.log("Vault economy provider not found");
            return;
        }

        economy = rsp.getProvider();
        addon.log("Vault economy hooked successfully");
    }

    /**
     * Check if a player can relocate (cooldown and permissions)
     */
    public boolean canRelocate(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            return false;
        }

        // Check bypass permission
        if (player.hasPermission("islandselector.bypass.cooldown.relocation")) {
            return true;
        }

        // Check cooldown
        RelocationData data = getRelocationData(playerUUID);
        return !data.isOnCooldown(addon.getSettings().getRelocationCooldown());
    }

    /**
     * Get remaining cooldown time for a player
     */
    public long getRemainingCooldown(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.hasPermission("islandselector.bypass.cooldown.relocation")) {
            return 0;
        }

        RelocationData data = getRelocationData(playerUUID);
        return data.getRemainingCooldown(addon.getSettings().getRelocationCooldown());
    }

    /**
     * Check if relocation is available (requires WorldEdit/FAWE)
     */
    public boolean isAvailable() {
        return addon.isSchematicOperationsAvailable();
    }

    /**
     * Get relocation data for a player (creates new if doesn't exist)
     */
    public RelocationData getRelocationData(UUID playerUUID) {
        String uuid = playerUUID.toString();

        // Check if data exists before trying to load (avoids "file not found" error log)
        if (database.objectExists(uuid)) {
            RelocationData data = database.loadObject(uuid);
            if (data != null) {
                return data;
            }
        }

        // Create new relocation data for this player
        return new RelocationData(uuid);
    }

    /**
     * Check if a player can afford the relocation cost
     */
    public boolean canAffordRelocation(Player player) {
        double cost = addon.getSettings().getRelocationCost();
        if (cost <= 0) {
            return true; // Free relocation
        }

        // Check bypass permission
        if (player.hasPermission("islandselector.bypass.cost.relocation")) {
            return true;
        }

        // Check economy
        if (economy == null) {
            return true; // No economy, consider it free
        }

        return economy.has(player, cost);
    }

    /**
     * Charge the player for relocation
     */
    public boolean chargePlayer(Player player) {
        double cost = addon.getSettings().getRelocationCost();
        if (cost <= 0) {
            return true; // Free relocation
        }

        // Check bypass permission
        if (player.hasPermission("islandselector.bypass.cost.relocation")) {
            return true;
        }

        // Charge via economy
        if (economy == null) {
            return true; // No economy, consider it free
        }

        if (!economy.has(player, cost)) {
            return false;
        }

        economy.withdrawPlayer(player, cost);
        return true;
    }

    /**
     * Perform island relocation asynchronously
     */
    public void relocateIsland(Player player, GridCoordinate fromCoord, GridCoordinate toCoord) {
        UUID playerUUID = player.getUniqueId();

        // Calculate cost and world coordinates for the event
        double cost = addon.getSettings().getRelocationCost();
        boolean bypassCost = player.hasPermission("islandselector.bypass.cost.relocate");
        if (bypassCost) {
            cost = 0;
        }

        int fromWorldX = calculateWorldX(fromCoord);
        int fromWorldZ = calculateWorldZ(fromCoord);
        int toWorldX = calculateWorldX(toCoord);
        int toWorldZ = calculateWorldZ(toCoord);

        // Fire IslandRelocateEvent on main thread BEFORE starting the async operation
        IslandRelocateEvent event = new IslandRelocateEvent(
            player,
            fromCoord,
            toCoord,
            fromWorldX,
            fromWorldZ,
            toWorldX,
            toWorldZ,
            cost
        );

        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            Bukkit.getPluginManager().callEvent(event);

            // Check if event was cancelled
            if (event.isCancelled()) {
                addon.log("IslandRelocateEvent cancelled for " + player.getName() +
                         " from " + fromCoord + " to " + toCoord);
                if (event.getCancellationReason() != null) {
                    player.sendMessage("§c" + event.getCancellationReason());
                } else {
                    player.sendMessage("§cIsland relocation cancelled.");
                }
                return;
            }

            // Event not cancelled - proceed with relocation asynchronously
            performRelocationAsync(player, fromCoord, toCoord);
        });
    }

    /**
     * Admin method to relocate an offline (or online) player's island.
     * Progress messages are sent to the admin who initiated the relocation.
     *
     * @param adminPlayer The admin executing the command
     * @param targetUUID The UUID of the island owner (may be offline)
     * @param targetName The name of the island owner
     * @param fromCoord Source grid coordinate
     * @param toCoord Destination grid coordinate
     */
    public void adminRelocateIsland(Player adminPlayer, UUID targetUUID, String targetName,
                                     GridCoordinate fromCoord, GridCoordinate toCoord) {
        // Calculate world coordinates
        int fromWorldX = calculateWorldX(fromCoord);
        int fromWorldZ = calculateWorldZ(fromCoord);
        int toWorldX = calculateWorldX(toCoord);
        int toWorldZ = calculateWorldZ(toCoord);

        // Fire event (admin initiated, so we skip cost)
        Player targetPlayer = Bukkit.getPlayer(targetUUID);
        IslandRelocateEvent event = new IslandRelocateEvent(
            targetPlayer, // May be null for offline players
            fromCoord,
            toCoord,
            fromWorldX,
            fromWorldZ,
            toWorldX,
            toWorldZ,
            0 // No cost for admin relocations
        );

        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                addon.log("Admin IslandRelocateEvent cancelled for " + targetName);
                if (event.getCancellationReason() != null) {
                    adminPlayer.sendMessage("§c" + event.getCancellationReason());
                } else {
                    adminPlayer.sendMessage("§cIsland relocation cancelled by another plugin.");
                }
                return;
            }

            // Perform relocation asynchronously
            performAdminRelocationAsync(adminPlayer, targetUUID, targetName, fromCoord, toCoord);
        });
    }

    /**
     * Internal method to perform admin relocation asynchronously
     */
    private void performAdminRelocationAsync(Player adminPlayer, UUID targetUUID, String targetName,
                                              GridCoordinate fromCoord, GridCoordinate toCoord) {
        // If target player is online, teleport them to safety first
        Player targetPlayer = Bukkit.getPlayer(targetUUID);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                Location serverSpawn = Bukkit.getWorlds().get(0).getSpawnLocation();
                // Use safe teleport for server spawn
                new SafeSpotTeleport.Builder(addon.getPlugin())
                    .entity(targetPlayer)
                    .location(serverSpawn)
                    .buildFuture();
                targetPlayer.sendMessage(colorize("&6&l[Admin Notice]"));
                targetPlayer.sendMessage(colorize("&eYour island is being relocated by an administrator."));
                targetPlayer.sendMessage(colorize("&7Please wait while your island is moved..."));
            });
        }

        // Run relocation async
        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            performAdminRelocationWork(adminPlayer, targetUUID, targetName, fromCoord, toCoord);
        });
    }

    /**
     * The actual admin relocation work (runs asynchronously)
     */
    private void performAdminRelocationWork(Player adminPlayer, UUID targetUUID, String targetName,
                                             GridCoordinate fromCoord, GridCoordinate toCoord) {
        try {
            sendProgress(adminPlayer, "&eStarting island relocation for " + targetName + "...");

            World bskyblockWorld = addon.getGridManager().getBSkyBlockWorld();
            if (bskyblockWorld == null) {
                sendError(adminPlayer, "&cBSkyBlock world not available!");
                return;
            }

            Island island = addon.getIslands().getIsland(bskyblockWorld, targetUUID);
            if (island == null) {
                sendError(adminPlayer, "&cCould not find island for " + targetName + "!");
                return;
            }

            Location oldCenter = island.getCenter();
            if (oldCenter == null || oldCenter.getWorld() == null) {
                sendError(adminPlayer, "&cInvalid island location!");
                return;
            }

            // Capture island homes relative to old center (so we can restore them at new location)
            final java.util.Map<String, RelativeHome> capturedHomes = captureHomesRelativeToCenter(island, oldCenter);
            addon.log("Admin relocation: Captured " + capturedHomes.size() + " island homes");

            // Save island as schematic
            sendProgress(adminPlayer, "&eSaving island...");
            Object clipboard = saveIslandToClipboard(island, oldCenter);
            if (clipboard == null) {
                sendError(adminPlayer, "&cFailed to save island!");
                return;
            }

            // Calculate new center location
            int newWorldX = calculateWorldX(toCoord);
            int newWorldZ = calculateWorldZ(toCoord);
            Location newCenter = new Location(
                oldCenter.getWorld(),
                newWorldX,
                oldCenter.getY(),
                newWorldZ,
                oldCenter.getYaw(),
                oldCenter.getPitch()
            );

            // Clear old location
            sendProgress(adminPlayer, "&eClearing old location...");
            clearIslandBlocks(island, oldCenter);
            Thread.sleep(1000);

            // Paste island at new location
            sendProgress(adminPlayer, "&ePasting island at new location...");
            pasteIslandFromClipboard(clipboard, newCenter);
            Thread.sleep(1000);

            // Update BSkyBlock island data (on main thread)
            final int finalNewWorldX = newWorldX;
            final int finalNewWorldZ = newWorldZ;

            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                sendProgress(adminPlayer, "&eUpdating island data...");

                var islandsManager = BentoBox.getInstance().getIslandsManager();

                // Remove from cache
                try {
                    java.lang.reflect.Field cacheField = islandsManager.getClass().getDeclaredField("islandCache");
                    cacheField.setAccessible(true);
                    Object islandCache = cacheField.get(islandsManager);
                    java.lang.reflect.Method deleteMethod = islandCache.getClass().getMethod("deleteIslandFromCache", Island.class);
                    deleteMethod.invoke(islandCache, island);
                } catch (Exception e) {
                    addon.logWarning("Could not remove island from cache: " + e.getMessage());
                }

                // Update center
                island.setCenter(newCenter);

                // Reset protection center
                try {
                    java.lang.reflect.Field locationField = Island.class.getDeclaredField("location");
                    locationField.setAccessible(true);
                    locationField.set(island, null);
                } catch (Exception e) {
                    addon.logWarning("Could not reset protection center: " + e.getMessage());
                }

                // Update spawn and restore homes at new location
                island.setSpawnPoint(org.bukkit.World.Environment.NORMAL, newCenter);

                // Restore island homes at their relative positions to the new center
                if (capturedHomes != null && !capturedHomes.isEmpty()) {
                    int restoredHomes = restoreHomesAtNewCenter(island, newCenter, capturedHomes);
                    addon.log("Admin relocation: Restored " + restoredHomes + " island homes at new location");
                } else {
                    // No captured homes - just add default home at center
                    island.getHomes().clear();
                    island.addHome("home", newCenter);
                }

                // Re-add to cache
                try {
                    java.lang.reflect.Field cacheField = islandsManager.getClass().getDeclaredField("islandCache");
                    cacheField.setAccessible(true);
                    Object islandCache = cacheField.get(islandsManager);
                    java.lang.reflect.Method addMethod = islandCache.getClass().getMethod("addIsland", Island.class);
                    addMethod.invoke(islandCache, island);
                } catch (Exception e) {
                    addon.logWarning("Could not re-add island to cache: " + e.getMessage());
                }

                // Save to database
                island.setChanged();
                islandsManager.saveIsland(island);

                // Update grid tracking
                addon.getGridManager().clearLocation(fromCoord);

                // Check if multi-dimension is enabled
                DimensionManager dimManager = addon.getDimensionManager();
                if (dimManager != null && dimManager.isEnabled()) {
                    // Get all dimension islands for this player and update grid with multi-dimension data
                    Map<String, UUID> dimensionIslands = collectDimensionIslands(targetUUID, dimManager);
                    addon.getGridManager().occupyLocation(toCoord, targetUUID, targetName, dimensionIslands);
                } else {
                    // Single dimension mode
                    UUID islandUUID = null;
                    try {
                        islandUUID = UUID.fromString(island.getUniqueId());
                    } catch (IllegalArgumentException e) {
                        // Not a UUID
                    }
                    addon.getGridManager().occupyLocation(toCoord, targetUUID, targetName, islandUUID);
                }

                // Update ALL slot data with new grid coordinate (all slots share the same physical location)
                var allSlots = addon.getSlotManager().getPlayerSlots(targetUUID);
                for (var slot : allSlots) {
                    slot.setGridCoordinate(toCoord.toString());
                    addon.getSlotManager().saveSlot(slot);
                }
                addon.log("Updated " + allSlots.size() + " slots with new grid coordinate: " + toCoord);

                // Teleport target player if online
                Player targetPlayer = Bukkit.getPlayer(targetUUID);
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    teleportPlayersSafelyForAdmin(island, targetPlayer, newCenter);
                }

                // Final success message
                Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
                    sendSuccess(adminPlayer, "&a&lIsland relocated successfully!");
                    adminPlayer.sendMessage(colorize("&7Player: &f" + targetName));
                    adminPlayer.sendMessage(colorize("&7From: &f" + fromCoord + " &7→ &f" + toCoord));
                    adminPlayer.sendMessage(colorize("&7New coordinates: &fX: " + finalNewWorldX + ", Z: " + finalNewWorldZ));

                    // Notify target if online
                    Player target = Bukkit.getPlayer(targetUUID);
                    if (target != null && target.isOnline()) {
                        target.sendMessage(colorize("&a&lYour island has been relocated!"));
                        target.sendMessage(colorize("&7New location: &f" + toCoord));

                        // Execute custom commands for the target player (admin relocation)
                        executeRelocationCommands(target, fromCoord.toString(), toCoord.toString(), finalNewWorldX, finalNewWorldZ);
                    }
                }, 40L);
            });

        } catch (Exception e) {
            addon.logError("Error during admin island relocation for " + targetName + ": " + e.getMessage());
            e.printStackTrace();
            sendError(adminPlayer, "&cAn error occurred during relocation.");
        }
    }

    /**
     * Teleport player safely for admin relocation (doesn't handle visitors differently)
     */
    private void teleportPlayersSafelyForAdmin(Island island, Player targetPlayer, Location newCenter) {
        try {
            World world = newCenter.getWorld();
            if (world == null) return;

            int chunkX = newCenter.getBlockX() >> 4;
            int chunkZ = newCenter.getBlockZ() >> 4;

            world.loadChunk(chunkX, chunkZ, true);
            world.loadChunk(chunkX + 1, chunkZ, true);
            world.loadChunk(chunkX - 1, chunkZ, true);
            world.loadChunk(chunkX, chunkZ + 1, true);
            world.loadChunk(chunkX, chunkZ - 1, true);

            Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
                // Get the default home location (same as /island home)
                Location homeLocation = island.getHome("");
                if (homeLocation == null) {
                    homeLocation = island.getSpawnPoint(org.bukkit.World.Environment.NORMAL);
                }
                if (homeLocation == null) {
                    homeLocation = newCenter;
                }

                // Use BentoBox SafeSpotTeleport for safe async teleportation to home
                new SafeSpotTeleport.Builder(addon.getPlugin())
                    .entity(targetPlayer)
                    .island(island)
                    .location(homeLocation)
                    .thenRun(() -> addon.log("Teleported " + targetPlayer.getName() + " to relocated island home"))
                    .ifFail(() -> targetPlayer.sendMessage(colorize("&eCouldn't find safe spot - use /island go")))
                    .buildFuture();
            }, 20L);

        } catch (Exception e) {
            addon.logError("Failed to teleport player: " + e.getMessage());
        }
    }

    /**
     * Internal method to perform the actual relocation asynchronously
     */
    private void performRelocationAsync(Player player, GridCoordinate fromCoord, GridCoordinate toCoord) {
        UUID playerUUID = player.getUniqueId();

        // Step 0: Teleport player to SERVER spawn BEFORE starting relocation (on main thread)
        // This prevents the player from falling into the void during the move
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            // Use server spawn (first world's spawn) not the current world's spawn
            Location serverSpawn = Bukkit.getWorlds().get(0).getSpawnLocation();
            // Use safe teleport for server spawn
            new SafeSpotTeleport.Builder(addon.getPlugin())
                .entity(player)
                .location(serverSpawn)
                .buildFuture();
            sendProgress(player, "&eTeleported to spawn for safety during relocation...");

            // Now run the rest asynchronously
            Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
                performRelocationWork(player, playerUUID, fromCoord, toCoord);
            });
        });
    }

    /**
     * The actual relocation work (runs asynchronously after player is teleported to spawn)
     * Uses callback-based async operations to prevent server freezes.
     */
    private void performRelocationWork(Player player, UUID playerUUID, GridCoordinate fromCoord, GridCoordinate toCoord) {
        // Step 1: Get the island (on main thread for BentoBox API access)
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            sendProgress(player, "&eStarting island relocation...");

            // Use BSkyBlock world directly
            World bskyblockWorld = addon.getGridManager().getBSkyBlockWorld();
            if (bskyblockWorld == null) {
                sendError(player, "&cBSkyBlock world not available!");
                return;
            }

            Island island = addon.getIslands().getIsland(bskyblockWorld, playerUUID);
            if (island == null) {
                sendError(player, "&cCould not find your island!");
                return;
            }

            Location oldCenter = island.getCenter();
            if (oldCenter == null || oldCenter.getWorld() == null) {
                sendError(player, "&cInvalid island location!");
                return;
            }

            // Calculate new center location
            int newWorldX = calculateWorldX(toCoord);
            int newWorldZ = calculateWorldZ(toCoord);
            Location newCenter = new Location(
                oldCenter.getWorld(),
                newWorldX,
                oldCenter.getY(),
                newWorldZ,
                oldCenter.getYaw(),
                oldCenter.getPitch()
            );

            // Calculate range for entity operations
            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int entityRange = Math.max(islandSpacing / 2, protectionRange);
            final int finalEntityRange = entityRange;

            // Step 2: Save island to clipboard AND capture entities (async for clipboard, sync for entities)
            sendProgress(player, "&eSaving island...");

            // First, capture entities on main thread using EntityStorage for full data preservation
            // This captures villager professions, trades, health, tameable status, etc.
            final java.util.List<Object> capturedEntities = addon.getEntityStorage().captureEntitiesInMemory(oldCenter.getWorld(), oldCenter, entityRange);

            // Capture island homes relative to old center (so we can restore them at new location)
            final java.util.Map<String, RelativeHome> capturedHomes = captureHomesRelativeToCenter(island, oldCenter);
            addon.log("Captured " + capturedHomes.size() + " island homes for relocation");

            saveIslandToClipboardAsync(island, oldCenter, clipboard -> {
                if (clipboard == null) {
                    sendError(player, "&cFailed to save island!");
                    return;
                }

                // Step 3: Clear old location and remove entities (async for blocks)
                sendProgress(player, "&eClearing old location...");

                // Remove entities on main thread first
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    addon.getEntityStorage().removeEntities(oldCenter.getWorld(), oldCenter, finalEntityRange);

                    // Now clear blocks async
                    clearIslandBlocksAsync(island, oldCenter, clearSuccess -> {
                        if (!clearSuccess) {
                            sendError(player, "&cFailed to clear old location!");
                            return;
                        }

                        // Step 4: Paste at new location (async)
                        sendProgress(player, "&ePasting island at new location...");
                        pasteIslandFromClipboardAsync(clipboard, newCenter, pasteSuccess -> {
                            if (!pasteSuccess) {
                                sendError(player, "&cFailed to paste island at new location!");
                                return;
                            }

                            // Step 4.5: Restore entities at new location (on main thread)
                            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                                sendProgress(player, "&eRestoring entities...");
                                // Use EntityStorage for full data restoration (villager professions, trades, etc.)
                                int restoredCount = addon.getEntityStorage().restoreEntitiesInMemory(capturedEntities, newCenter);
                                addon.log("Restored " + restoredCount + " entities at new location");

                                // Step 5: Update BSkyBlock island data (still on main thread)
                                finishRelocation(player, playerUUID, island, oldCenter, newCenter, fromCoord, toCoord, newWorldX, newWorldZ, capturedHomes);
                            });
                        });
                    });
                });
            });
        });
    }

    /**
     * Finish the relocation process - update BentoBox data and teleport player
     */
    private void finishRelocation(Player player, UUID playerUUID, Island island, Location oldCenter,
                                   Location newCenter, GridCoordinate fromCoord, GridCoordinate toCoord,
                                   int newWorldX, int newWorldZ, java.util.Map<String, RelativeHome> capturedHomes) {
        try {
            sendProgress(player, "&eUpdating island data...");

            // CRITICAL: BentoBox caches islands by location in an IslandGrid.
            // Simply calling setCenter() does NOT update the cache!
            // We must: 1) Remove from cache, 2) Update center, 3) Re-add to cache

            var islandsManager = BentoBox.getInstance().getIslandsManager();

            // Remove island from BentoBox's location cache at OLD location
            try {
                java.lang.reflect.Field cacheField = islandsManager.getClass().getDeclaredField("islandCache");
                cacheField.setAccessible(true);
                Object islandCache = cacheField.get(islandsManager);

                java.lang.reflect.Method deleteMethod = islandCache.getClass().getMethod("deleteIslandFromCache", Island.class);
                deleteMethod.invoke(islandCache, island);
                addon.log("Removed island from BentoBox cache at old location");
            } catch (Exception e) {
                addon.logWarning("Could not remove island from cache: " + e.getMessage());
            }

            // Update the island's center location
            island.setCenter(newCenter);
            addon.log("Updated island center to " + newCenter);

            // Update the protection center using reflection
            try {
                java.lang.reflect.Field locationField = Island.class.getDeclaredField("location");
                locationField.setAccessible(true);
                locationField.set(island, null); // null = use center as protection center
                addon.log("Reset protection center to follow island center");
            } catch (Exception e) {
                addon.logWarning("Could not reset protection center: " + e.getMessage());
            }

            // Update spawn point and restore homes at new location
            island.setSpawnPoint(org.bukkit.World.Environment.NORMAL, newCenter);

            // Restore island homes at their relative positions to the new center
            if (capturedHomes != null && !capturedHomes.isEmpty()) {
                int restoredHomes = restoreHomesAtNewCenter(island, newCenter, capturedHomes);
                addon.log("Restored " + restoredHomes + " island homes at new location");
            } else {
                // No captured homes - just add default home at center
                island.getHomes().clear();
                island.addHome("home", newCenter);
            }

            // Re-add island to BentoBox's location cache at NEW location
            try {
                java.lang.reflect.Field cacheField = islandsManager.getClass().getDeclaredField("islandCache");
                cacheField.setAccessible(true);
                Object islandCache = cacheField.get(islandsManager);

                java.lang.reflect.Method addMethod = islandCache.getClass().getMethod("addIsland", Island.class);
                addMethod.invoke(islandCache, island);
                addon.log("Re-added island to BentoBox cache at new location");
            } catch (Exception e) {
                addon.logWarning("Could not re-add island to cache: " + e.getMessage());
            }

            // Save to database
            island.setChanged();
            islandsManager.saveIsland(island);
            addon.log("Saved island to BentoBox database");

            // Update our grid location tracking
            addon.getGridManager().clearLocation(fromCoord);

            // Check if multi-dimension is enabled
            DimensionManager dimManager = addon.getDimensionManager();
            if (dimManager != null && dimManager.isEnabled()) {
                // Get all dimension islands for this player and update grid with multi-dimension data
                Map<String, UUID> dimensionIslands = collectDimensionIslands(playerUUID, dimManager);
                addon.getGridManager().occupyLocation(toCoord, playerUUID, player.getName(), dimensionIslands);
            } else {
                // Single dimension mode
                UUID islandUUID = null;
                try {
                    islandUUID = UUID.fromString(island.getUniqueId());
                } catch (IllegalArgumentException e) {
                    // Island ID is not a UUID
                }
                addon.getGridManager().occupyLocation(toCoord, playerUUID, player.getName(), islandUUID);
            }

            // Update ALL slot data with new grid coordinate (all slots share the same physical location)
            var allSlots = addon.getSlotManager().getPlayerSlots(playerUUID);
            for (var slot : allSlots) {
                slot.setGridCoordinate(toCoord.toString());
                addon.getSlotManager().saveSlot(slot);
            }
            addon.log("Updated " + allSlots.size() + " slots with new grid coordinate: " + toCoord);

            // Update relocation data
            RelocationData data = getRelocationData(playerUUID);
            data.recordRelocation(fromCoord.toString(), toCoord.toString());
            database.saveObjectAsync(data);

            // Load chunks and teleport players safely
            sendProgress(player, "&eLoading chunks and teleporting...");
            teleportPlayersSafely(island, player, newCenter);

            // Complete (sent after teleport delay)
            final int finalNewWorldX = newWorldX;
            final int finalNewWorldZ = newWorldZ;
            Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
                sendSuccess(player, "&a&lIsland relocated successfully!");
                player.sendMessage(colorize("&7From: &f" + fromCoord.toString() + " &7→ &f" + toCoord.toString()));
                player.sendMessage(colorize("&7New coordinates: &fX: " + finalNewWorldX + ", Z: " + finalNewWorldZ));

                // Execute custom commands after relocation
                executeRelocationCommands(player, fromCoord.toString(), toCoord.toString(), finalNewWorldX, finalNewWorldZ);
            }, 40L); // 2 second delay after teleport

        } catch (Exception e) {
            addon.logError("Error during island relocation for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            sendError(player, "&cAn error occurred during relocation. Please contact an admin.");
        }
    }

    /**
     * Save island to a clipboard (opaque object)
     */
    private Object saveIslandToClipboard(Island island, Location center) {
        try {
            World world = center.getWorld();
            if (world == null) {
                return null;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            // Use SchematicUtils for safe entity handling
            Object clipboard = addon.getSchematicUtils().copyToClipboard(center, range, true);

            if (clipboard != null) {
                addon.log("Saved island to clipboard for relocation");
            }
            return clipboard;

        } catch (Exception e) {
            addon.logError("Failed to save island to clipboard: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Paste island from clipboard to new location
     */
    private void pasteIslandFromClipboard(Object clipboard, Location newCenter) {
        try {
            if (clipboard == null) {
                addon.logError("Cannot paste - clipboard is null");
                return;
            }

            // Use SchematicUtils to paste
            boolean success = addon.getSchematicUtils().pasteFromClipboard(clipboard, newCenter);
            if (success) {
                addon.log("Pasted island to new location");
            }

        } catch (Exception e) {
            addon.logError("Failed to paste island: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Clear island blocks from old location
     */
    private void clearIslandBlocks(Island island, Location center) {
        try {
            World world = center.getWorld();
            if (world == null) {
                return;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            // Use SchematicUtils to clear the region
            boolean success = addon.getSchematicUtils().clearRegion(center, range);
            if (success) {
                addon.log("Cleared island blocks at old location");
            }

        } catch (Exception e) {
            addon.logError("Failed to clear island blocks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== ASYNC VERSIONS ====================

    /**
     * Save island to a clipboard asynchronously
     */
    private void saveIslandToClipboardAsync(Island island, Location center, java.util.function.Consumer<Object> callback) {
        try {
            World world = center.getWorld();
            if (world == null) {
                callback.accept(null);
                return;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            // Use SchematicUtils async method
            addon.getSchematicUtils().copyToClipboardAsync(center, range, true, clipboard -> {
                if (clipboard != null) {
                    addon.log("Saved island to clipboard for relocation");
                }
                callback.accept(clipboard);
            });

        } catch (Exception e) {
            addon.logError("Failed to save island to clipboard: " + e.getMessage());
            e.printStackTrace();
            callback.accept(null);
        }
    }

    /**
     * Paste island from clipboard to new location asynchronously
     */
    private void pasteIslandFromClipboardAsync(Object clipboard, Location newCenter, java.util.function.Consumer<Boolean> callback) {
        try {
            if (clipboard == null) {
                addon.logError("Cannot paste - clipboard is null");
                callback.accept(false);
                return;
            }

            // Use SchematicUtils async method
            addon.getSchematicUtils().pasteFromClipboardAsync(clipboard, newCenter, success -> {
                if (success) {
                    addon.log("Pasted island to new location");
                }
                callback.accept(success);
            });

        } catch (Exception e) {
            addon.logError("Failed to paste island: " + e.getMessage());
            e.printStackTrace();
            callback.accept(false);
        }
    }

    /**
     * Clear island blocks from old location asynchronously
     */
    private void clearIslandBlocksAsync(Island island, Location center, java.util.function.Consumer<Boolean> callback) {
        try {
            World world = center.getWorld();
            if (world == null) {
                callback.accept(false);
                return;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            // Use SchematicUtils async method
            addon.getSchematicUtils().clearRegionAsync(center, range, success -> {
                if (success) {
                    addon.log("Cleared island blocks at old location");
                }
                callback.accept(success);
            });

        } catch (Exception e) {
            addon.logError("Failed to clear island blocks: " + e.getMessage());
            e.printStackTrace();
            callback.accept(false);
        }
    }

    /**
     * Safely teleport player and team members to new island location.
     * This method loads the chunk first to prevent falling through blocks.
     */
    private void teleportPlayersSafely(Island island, Player owner, Location newCenter) {
        try {
            World world = newCenter.getWorld();
            if (world == null) {
                addon.logError("Cannot teleport - world is null");
                return;
            }

            // First, load the chunk at the new center synchronously
            int chunkX = newCenter.getBlockX() >> 4;
            int chunkZ = newCenter.getBlockZ() >> 4;

            // Force load the chunk and surrounding chunks
            world.loadChunk(chunkX, chunkZ, true);
            world.loadChunk(chunkX + 1, chunkZ, true);
            world.loadChunk(chunkX - 1, chunkZ, true);
            world.loadChunk(chunkX, chunkZ + 1, true);
            world.loadChunk(chunkX, chunkZ - 1, true);

            // Wait a bit for chunks to fully generate, then teleport
            Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
                // Get the default home location (same as /island home)
                Location homeLocation = island.getHome("");
                if (homeLocation == null) {
                    homeLocation = island.getSpawnPoint(org.bukkit.World.Environment.NORMAL);
                }
                if (homeLocation == null) {
                    homeLocation = newCenter;
                }

                // Use BentoBox SafeSpotTeleport for owner, targeting home location
                new SafeSpotTeleport.Builder(addon.getPlugin())
                    .entity(owner)
                    .island(island)
                    .location(homeLocation)
                    .thenRun(() -> addon.log("Teleported " + owner.getName() + " to relocated island home"))
                    .ifFail(() -> owner.sendMessage(colorize("&eCouldn't find safe spot - use /island go")))
                    .buildFuture();

                // Teleport team members if online using safe teleport to home
                final Location finalHomeLocation = homeLocation;
                for (UUID memberUUID : island.getMemberSet()) {
                    if (!memberUUID.equals(owner.getUniqueId())) {
                        Player member = Bukkit.getPlayer(memberUUID);
                        if (member != null && member.isOnline()) {
                            new SafeSpotTeleport.Builder(addon.getPlugin())
                                .entity(member)
                                .island(island)
                                .location(finalHomeLocation)
                                .thenRun(() -> member.sendMessage(colorize("&eYour island has been relocated to a new location!")))
                                .buildFuture();
                        }
                    }
                }

                // Handle visitors - teleport them away from the OLD location
                handleVisitorsAfterRelocation(island, owner);

            }, 20L); // Wait 1 second for chunks to fully generate

        } catch (Exception e) {
            addon.logError("Failed to teleport players: " + e.getMessage());
            e.printStackTrace();
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
     * Handle visitors after relocation - teleport them away from the old (now cleared) location
     */
    private void handleVisitorsAfterRelocation(Island island, Player owner) {
        Location islandCenter = island.getCenter();
        if (islandCenter == null || islandCenter.getWorld() == null) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Skip island members
            if (player.getUniqueId().equals(owner.getUniqueId()) ||
                island.getMemberSet().contains(player.getUniqueId())) {
                continue;
            }

            // Check if player was on the OLD location (which is now cleared)
            // We can't easily detect this, but we can check if they're in the void
            Location loc = player.getLocation();
            if (loc.getWorld() != null && loc.getWorld().equals(islandCenter.getWorld())) {
                // Check if player is falling or in an unsafe location
                if (loc.getY() < loc.getWorld().getMinHeight() + 10 ||
                    !loc.getBlock().getType().isSolid() &&
                    !loc.clone().add(0, -1, 0).getBlock().getType().isSolid()) {

                    // They might be falling - teleport to spawn safely
                    Location spawn = loc.getWorld().getSpawnLocation();
                    new SafeSpotTeleport.Builder(addon.getPlugin())
                        .entity(player)
                        .location(spawn)
                        .thenRun(() -> player.sendMessage(colorize("&eThe island you were visiting has been relocated.")))
                        .buildFuture();
                }
            }
        }
    }

    /**
     * Legacy method - kept for compatibility but redirects to safe version
     */
    private void teleportPlayers(Island island, Player owner, Location newCenter) {
        teleportPlayersSafely(island, owner, newCenter);
    }

    /**
     * Calculate world X coordinate from grid coordinate
     */
    private int calculateWorldX(GridCoordinate coord) {
        int spacing = addon.getIslandSpacing() * 2;
        return coord.getX() * spacing;
    }

    /**
     * Calculate world Z coordinate from grid coordinate
     */
    private int calculateWorldZ(GridCoordinate coord) {
        int spacing = addon.getIslandSpacing() * 2;
        return coord.getZ() * spacing;
    }

    /**
     * Reset a player's relocation cooldown (admin command)
     */
    public void resetCooldown(UUID playerUUID) {
        RelocationData data = getRelocationData(playerUUID);
        data.setLastRelocationTime(0);
        database.saveObjectAsync(data);
    }

    /**
     * Format cooldown time as a human-readable string
     */
    public String formatCooldownTime(long seconds) {
        if (seconds <= 0) {
            return "Ready";
        }

        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (secs > 0 || sb.length() == 0) {
            sb.append(secs).append("s");
        }

        return sb.toString().trim();
    }

    // Helper methods for sending messages
    private void sendProgress(Player player, String message) {
        Bukkit.getScheduler().runTask(addon.getPlugin(), () ->
            player.sendMessage(colorize(message))
        );
    }

    private void sendSuccess(Player player, String message) {
        Bukkit.getScheduler().runTask(addon.getPlugin(), () ->
            player.sendMessage(colorize(message))
        );
    }

    private void sendError(Player player, String message) {
        Bukkit.getScheduler().runTask(addon.getPlugin(), () ->
            player.sendMessage(colorize(message))
        );
    }

    private String colorize(String text) {
        return text.replace("&", "\u00A7");
    }

    /**
     * Execute custom commands after relocation completes.
     * Commands are configured in config.yml under custom-commands.relocation
     */
    private void executeRelocationCommands(Player player, String fromCoord, String toCoord, int worldX, int worldZ) {
        if (!addon.getSettings().isRelocationCommandsEnabled()) {
            return;
        }

        var commands = addon.getSettings().getRelocationCommands();
        if (commands == null || commands.isEmpty()) {
            return;
        }

        Map<String, String> placeholders = CustomCommandExecutor.createRelocationPlaceholders(
            player, fromCoord, toCoord, worldX, worldZ
        );

        addon.getCustomCommandExecutor().executeCommands(
            player,
            commands,
            addon.getSettings().getRelocationCommandScope(),
            placeholders
        );
    }

    // ==================== HOME CAPTURE FOR RELOCATION ====================

    /**
     * Capture all island homes relative to the island center.
     * Homes are stored with their offset from the center, so they can be
     * restored at the same relative position after relocation.
     *
     * @param island The island to capture homes from
     * @param center The island center location
     * @return Map of home names to relative positions
     */
    private java.util.Map<String, RelativeHome> captureHomesRelativeToCenter(Island island, Location center) {
        java.util.Map<String, RelativeHome> captured = new java.util.HashMap<>();

        if (island == null || center == null) {
            return captured;
        }

        java.util.Map<String, Location> homes = island.getHomes();
        if (homes == null || homes.isEmpty()) {
            return captured;
        }

        for (java.util.Map.Entry<String, Location> entry : homes.entrySet()) {
            Location homeLoc = entry.getValue();
            if (homeLoc != null && homeLoc.getWorld() != null) {
                RelativeHome relHome = new RelativeHome();
                relHome.relX = homeLoc.getX() - center.getX();
                relHome.relY = homeLoc.getY() - center.getY();
                relHome.relZ = homeLoc.getZ() - center.getZ();
                relHome.yaw = homeLoc.getYaw();
                relHome.pitch = homeLoc.getPitch();

                captured.put(entry.getKey(), relHome);
            }
        }

        return captured;
    }

    /**
     * Restore captured homes at the new island center.
     * Each home is placed at its original relative position to the new center.
     *
     * @param island The island to restore homes to
     * @param newCenter The new island center location
     * @param capturedHomes Map of home names to relative positions
     * @return Number of homes restored
     */
    private int restoreHomesAtNewCenter(Island island, Location newCenter, java.util.Map<String, RelativeHome> capturedHomes) {
        if (island == null || newCenter == null || capturedHomes == null || capturedHomes.isEmpty()) {
            return 0;
        }

        World world = newCenter.getWorld();
        if (world == null) {
            return 0;
        }

        // Clear all existing homes first
        java.util.Map<String, Location> existingHomes = island.getHomes();
        if (existingHomes != null && !existingHomes.isEmpty()) {
            java.util.List<String> homeNames = new java.util.ArrayList<>(existingHomes.keySet());
            for (String homeName : homeNames) {
                island.removeHome(homeName);
            }
        }

        int restored = 0;
        for (java.util.Map.Entry<String, RelativeHome> entry : capturedHomes.entrySet()) {
            RelativeHome relHome = entry.getValue();

            // Calculate absolute position at new center
            Location newHomeLoc = new Location(
                world,
                newCenter.getX() + relHome.relX,
                newCenter.getY() + relHome.relY,
                newCenter.getZ() + relHome.relZ,
                relHome.yaw,
                relHome.pitch
            );

            island.addHome(entry.getKey(), newHomeLoc);
            restored++;
        }

        return restored;
    }

    /**
     * Simple data class to hold relative home position for relocation.
     * Not serialized to disk - only used for in-memory relocation.
     */
    private static class RelativeHome {
        double relX, relY, relZ;
        float yaw, pitch;
    }

    // ==================== MULTI-DIMENSION SUPPORT ====================

    /**
     * Collect all dimension island UUIDs for a player.
     * Used when updating grid location with multi-dimension data.
     *
     * @param playerUUID The player's UUID
     * @param dimManager The DimensionManager instance
     * @return Map of dimension key to island UUID
     */
    private Map<String, UUID> collectDimensionIslands(UUID playerUUID, DimensionManager dimManager) {
        Map<String, UUID> dimensionIslands = new HashMap<>();

        for (DimensionConfig config : dimManager.getEnabledDimensions()) {
            String dimensionKey = config.getDimensionKey();
            World world = dimManager.getWorld(dimensionKey);

            if (world != null) {
                Island island = addon.getIslands().getIsland(world, playerUUID);
                if (island != null) {
                    String islandIdStr = island.getUniqueId();
                    UUID islandUUID = parseIslandUUID(islandIdStr);
                    if (islandUUID != null) {
                        dimensionIslands.put(dimensionKey, islandUUID);
                    }
                }
            }
        }

        return dimensionIslands;
    }

    /**
     * Parse an island ID string to a UUID.
     * Handles both plain UUIDs and prefixed BentoBox IDs (like "BSkyBlock6d68f389-...")
     *
     * @param islandIdStr The island ID string
     * @return The UUID, or null if not parseable
     */
    private UUID parseIslandUUID(String islandIdStr) {
        if (islandIdStr == null || islandIdStr.isEmpty()) {
            return null;
        }

        try {
            return UUID.fromString(islandIdStr);
        } catch (IllegalArgumentException e) {
            // Island ID has a prefix (like "BSkyBlock"), try to extract the UUID part
            if (islandIdStr.contains("-")) {
                int uuidStart = islandIdStr.indexOf('-') - 8; // UUID format: 8-4-4-4-12
                if (uuidStart > 0) {
                    String uuidPart = islandIdStr.substring(uuidStart);
                    try {
                        return UUID.fromString(uuidPart);
                    } catch (IllegalArgumentException e2) {
                        // Still not a valid UUID
                    }
                }
            }
        }
        return null;
    }
}
