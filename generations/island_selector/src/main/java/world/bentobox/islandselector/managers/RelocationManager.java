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

        // Capture admin UUID for safe access in async callbacks
        final UUID adminUUID = adminPlayer.getUniqueId();

        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            Bukkit.getPluginManager().callEvent(event);

            // Re-fetch admin player - they may have disconnected
            Player currentAdmin = Bukkit.getPlayer(adminUUID);

            if (event.isCancelled()) {
                addon.log("Admin IslandRelocateEvent cancelled for " + targetName);
                if (currentAdmin != null && currentAdmin.isOnline()) {
                    if (event.getCancellationReason() != null) {
                        currentAdmin.sendMessage("§c" + event.getCancellationReason());
                    } else {
                        currentAdmin.sendMessage("§cIsland relocation cancelled by another plugin.");
                    }
                }
                return;
            }

            // Perform relocation asynchronously (pass UUID instead of player reference)
            performAdminRelocationAsync(adminUUID, targetUUID, targetName, fromCoord, toCoord);
        });
    }

    /**
     * Internal method to perform admin relocation asynchronously
     */
    private void performAdminRelocationAsync(UUID adminUUID, UUID targetUUID, String targetName,
                                              GridCoordinate fromCoord, GridCoordinate toCoord) {
        // If target player is online, teleport them to safety first
        Player targetPlayer = Bukkit.getPlayer(targetUUID);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                // Re-fetch in case they logged off
                Player currentTarget = Bukkit.getPlayer(targetUUID);
                if (currentTarget == null || !currentTarget.isOnline()) {
                    return;
                }

                World spawnWorld = Bukkit.getWorld("world");
                if (spawnWorld == null && !Bukkit.getWorlds().isEmpty()) {
                    spawnWorld = Bukkit.getWorlds().get(0);
                }
                if (spawnWorld == null) {
                    addon.logWarning("Cannot teleport player during relocation - no spawn world available");
                    return;
                }
                Location serverSpawn = spawnWorld.getSpawnLocation();
                // Use safe teleport for server spawn
                new SafeSpotTeleport.Builder(addon.getPlugin())
                    .entity(currentTarget)
                    .location(serverSpawn)
                    .buildFuture();
                currentTarget.sendMessage(colorize("&6&l[Admin Notice]"));
                currentTarget.sendMessage(colorize("&eYour island is being relocated by an administrator."));
                currentTarget.sendMessage(colorize("&7Please wait while your island is moved..."));
            });
        }

        // Run relocation async
        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            performAdminRelocationWork(adminUUID, targetUUID, targetName, fromCoord, toCoord);
        });
    }

    /**
     * The actual admin relocation work (runs asynchronously)
     */
    private void performAdminRelocationWork(UUID adminUUID, UUID targetUUID, String targetName,
                                             GridCoordinate fromCoord, GridCoordinate toCoord) {
        try {
            sendProgressToAdmin(adminUUID, "&eStarting island relocation for " + targetName + "...");

            World bskyblockWorld = addon.getGridManager().getBSkyBlockWorld();
            if (bskyblockWorld == null) {
                sendErrorToAdmin(adminUUID, "&cBSkyBlock world not available!");
                return;
            }

            Island island = addon.getIslands().getIsland(bskyblockWorld, targetUUID);
            if (island == null) {
                sendErrorToAdmin(adminUUID, "&cCould not find island for " + targetName + "!");
                return;
            }

            Location oldCenter = island.getCenter();
            if (oldCenter == null || oldCenter.getWorld() == null) {
                sendErrorToAdmin(adminUUID, "&cInvalid island location!");
                return;
            }

            // Capture island homes relative to old center (so we can restore them at new location)
            final java.util.Map<String, RelativeHome> capturedHomes = captureHomesRelativeToCenter(island, oldCenter);
            addon.log("Admin relocation: Captured " + capturedHomes.size() + " island homes");

            // Save island as schematic
            sendProgressToAdmin(adminUUID, "&eSaving island...");
            Object clipboard = saveIslandToClipboard(island, oldCenter);
            if (clipboard == null) {
                sendErrorToAdmin(adminUUID, "&cFailed to save island!");
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
            sendProgressToAdmin(adminUUID, "&eClearing old location...");
            clearIslandBlocks(island, oldCenter);
            Thread.sleep(1000);

            // Paste island at new location
            sendProgressToAdmin(adminUUID, "&ePasting island at new location...");
            pasteIslandFromClipboard(clipboard, newCenter);
            Thread.sleep(1000);

            // Update BSkyBlock island data (on main thread)
            final int finalNewWorldX = newWorldX;
            final int finalNewWorldZ = newWorldZ;

            // Note: Uses reflection to access private BentoBox fields for cache management.
            // If reflection fails (e.g., due to BentoBox version change), warnings are logged
            // but the island data is still saved. Cache will refresh on server restart.
            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                sendProgressToAdmin(adminUUID, "&eUpdating island data...");

                var islandsManager = BentoBox.getInstance().getIslandsManager();

                // Remove from cache
                try {
                    java.lang.reflect.Field cacheField = islandsManager.getClass().getDeclaredField("islandCache");
                    cacheField.setAccessible(true);
                    Object islandCache = cacheField.get(islandsManager);
                    if (islandCache != null) {
                        java.lang.reflect.Method deleteMethod = islandCache.getClass().getMethod("deleteIslandFromCache", Island.class);
                        deleteMethod.invoke(islandCache, island);
                    }
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
                    if (islandCache != null) {
                        java.lang.reflect.Method addMethod = islandCache.getClass().getMethod("addIsland", Island.class);
                        addMethod.invoke(islandCache, island);
                    }
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
                    Player currentAdmin = Bukkit.getPlayer(adminUUID);
                    if (currentAdmin != null && currentAdmin.isOnline()) {
                        sendSuccess(currentAdmin, "&a&lIsland relocated successfully!");
                        currentAdmin.sendMessage(colorize("&7Player: &f" + targetName));
                        currentAdmin.sendMessage(colorize("&7From: &f" + fromCoord + " &7→ &f" + toCoord));
                        currentAdmin.sendMessage(colorize("&7New coordinates: &fX: " + finalNewWorldX + ", Z: " + finalNewWorldZ));
                    }

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
            sendErrorToAdmin(adminUUID, "&cAn error occurred during relocation.");
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
                // CRITICAL: Find a home that is in the TARGET world, not just any default home.
                // getHome("") returns the default home which could be in ANY dimension (nether, end).
                Location homeLocation = null;
                java.util.Map<String, Location> allHomes = island.getHomes();
                if (allHomes != null && !allHomes.isEmpty()) {
                    for (Location homeLoc : allHomes.values()) {
                        if (homeLoc != null && homeLoc.getWorld() != null
                            && homeLoc.getWorld().equals(world)) {
                            homeLocation = homeLoc;
                            break; // Use first home found in target world
                        }
                    }
                }

                // Fallback to spawn point
                if (homeLocation == null) {
                    homeLocation = island.getSpawnPoint(org.bukkit.World.Environment.NORMAL);
                }
                if (homeLocation == null) {
                    homeLocation = newCenter;
                }

                // Use BentoBox SafeSpotTeleport for safe async teleportation to home
                // IMPORTANT: Do NOT use .island() as it searches across ALL dimensions
                new SafeSpotTeleport.Builder(addon.getPlugin())
                    .entity(targetPlayer)
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

        // Capture player's origin world BEFORE teleporting to spawn
        // This allows us to return them to the same dimension after relocation
        final World originWorld = player.getWorld();

        // Step 0: Teleport player to SERVER spawn BEFORE starting relocation (on main thread)
        // This prevents the player from falling into the void during the move
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            // Use server spawn (first world's spawn) not the current world's spawn
            World spawnWorld = Bukkit.getWorld("world");
            if (spawnWorld == null && !Bukkit.getWorlds().isEmpty()) {
                spawnWorld = Bukkit.getWorlds().get(0);
            }
            if (spawnWorld == null) {
                player.sendMessage(colorize("&cCannot relocate - no spawn world available!"));
                return;
            }
            Location serverSpawn = spawnWorld.getSpawnLocation();
            // Use safe teleport for server spawn
            new SafeSpotTeleport.Builder(addon.getPlugin())
                .entity(player)
                .location(serverSpawn)
                .buildFuture();
            sendProgress(player, "&eTeleported to spawn for safety during relocation...");

            // Now run the rest asynchronously, passing origin world
            Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
                performRelocationWork(player, playerUUID, fromCoord, toCoord, originWorld);
            });
        });
    }

    /**
     * The actual relocation work (runs asynchronously after player is teleported to spawn)
     * Uses callback-based async operations to prevent server freezes.
     *
     * @param originWorld The world the player was in before relocation (to return them there)
     */
    private void performRelocationWork(Player player, UUID playerUUID, GridCoordinate fromCoord, GridCoordinate toCoord, World originWorld) {
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

            // Capture island homes from ALL dimensions (main + enabled dimensions)
            final Map<String, Map<String, RelativeHome>> allCapturedHomes = captureAllDimensionHomes(playerUUID, oldCenter);
            addon.log("Captured homes from " + allCapturedHomes.size() + " dimensions for relocation");

            // Capture spawn points (used by /island command)
            final CapturedSpawnPoints capturedSpawnPoints = captureSpawnPoints(island, oldCenter);
            addon.log("Captured " + capturedSpawnPoints.spawnPoints.size() + " spawn points for relocation");

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

                                // Step 5: Relocate dimension blocks (nether/end)
                                sendProgress(player, "&eRelocating dimension islands...");
                                relocateDimensionBlocksAsync(oldCenter, newCenter, finalEntityRange, dimSuccess -> {
                                    // Step 6: Update BSkyBlock island data (on main thread)
                                    Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                                        finishRelocation(player, playerUUID, island, oldCenter, newCenter, fromCoord, toCoord, newWorldX, newWorldZ, allCapturedHomes, capturedSpawnPoints, originWorld);
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });
    }

    /**
     * Finish the relocation process - update BentoBox data and teleport player
     *
     * @param allCapturedHomes Map of dimension key to captured homes for all dimensions
     * @param capturedSpawnPoints Captured spawn points for all environments
     * @param originWorld The world the player was in before relocation (to return them there)
     */
    private void finishRelocation(Player player, UUID playerUUID, Island island, Location oldCenter,
                                   Location newCenter, GridCoordinate fromCoord, GridCoordinate toCoord,
                                   int newWorldX, int newWorldZ, Map<String, Map<String, RelativeHome>> allCapturedHomes,
                                   CapturedSpawnPoints capturedSpawnPoints, World originWorld) {
        try {
            sendProgress(player, "&eUpdating island data...");

            // CRITICAL: BentoBox caches islands by location in an IslandGrid.
            // Simply calling setCenter() does NOT update the cache!
            // We must: 1) Remove from cache, 2) Update center, 3) Re-add to cache

            // Note: Uses reflection to access private BentoBox fields for cache management.
            // If reflection fails, warnings are logged but island data is still saved.
            var islandsManager = BentoBox.getInstance().getIslandsManager();

            // Remove island from BentoBox's location cache at OLD location
            try {
                java.lang.reflect.Field cacheField = islandsManager.getClass().getDeclaredField("islandCache");
                cacheField.setAccessible(true);
                Object islandCache = cacheField.get(islandsManager);

                if (islandCache != null) {
                    java.lang.reflect.Method deleteMethod = islandCache.getClass().getMethod("deleteIslandFromCache", Island.class);
                    deleteMethod.invoke(islandCache, island);
                    addon.log("Removed island from BentoBox cache at old location");
                }
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

            // Restore spawn points at their relative positions to the new center
            // This is critical for /island command to work correctly
            if (capturedSpawnPoints != null && !capturedSpawnPoints.spawnPoints.isEmpty()) {
                restoreSpawnPoints(island, newCenter, capturedSpawnPoints);
                addon.log("Restored " + capturedSpawnPoints.spawnPoints.size() + " spawn points at new location");
            } else {
                // No captured spawn points - set default spawn at center
                island.setSpawnPoint(org.bukkit.World.Environment.NORMAL, newCenter);
                addon.log("Set default spawn point at island center");
            }

            // CRITICAL: Re-add island to cache BEFORE restoring homes!
            // restoreAllDimensionHomes() calls getIsland() which needs to return our modified island.
            // If we don't re-add first, getIsland() loads a stale copy from the database with OLD data.
            try {
                java.lang.reflect.Field cacheField = islandsManager.getClass().getDeclaredField("islandCache");
                cacheField.setAccessible(true);
                Object islandCache = cacheField.get(islandsManager);

                if (islandCache != null) {
                    java.lang.reflect.Method addMethod = islandCache.getClass().getMethod("addIsland", Island.class);
                    addMethod.invoke(islandCache, island);
                    addon.log("Re-added island to BentoBox cache at new location");
                }
            } catch (Exception e) {
                addon.logWarning("Could not re-add island to cache: " + e.getMessage());
            }

            // Restore island homes at their relative positions to the new center (ALL dimensions)
            // Note: This calls getIsland() internally, which now returns our cached modified island
            if (allCapturedHomes != null && !allCapturedHomes.isEmpty()) {
                restoreAllDimensionHomes(playerUUID, newCenter, allCapturedHomes);
            } else {
                // No captured homes - just add default home at center for main island
                island.getHomes().clear();
                island.addHome("home", newCenter);
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

            // Load chunks and teleport players safely (to their origin dimension if multi-dimension)
            sendProgress(player, "&eLoading chunks and teleporting...");
            teleportPlayersSafely(island, player, newCenter, originWorld);

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

    // ==================== DIMENSION BLOCK RELOCATION ====================

    /**
     * Relocate blocks in BSkyBlock's native sub-dimensions (nether and end).
     * This is needed because BSkyBlock stores dimension islands at specific coordinates:
     * - Nether: X/8, Z/8 (1:8 scaling with overworld)
     * - End: Same X,Z as overworld (1:1)
     *
     * @param oldCenter The old island center in overworld
     * @param newCenter The new island center in overworld
     * @param range The island range for block operations
     * @param callback Called when all dimensions are relocated
     */
    private void relocateDimensionBlocksAsync(Location oldCenter, Location newCenter, int range,
                                               java.util.function.Consumer<Boolean> callback) {
        var bskyblock = addon.getBSkyBlockAddon();
        if (bskyblock == null) {
            addon.log("No BSkyBlock addon - skipping dimension block relocation");
            callback.accept(true);
            return;
        }

        World netherWorld = bskyblock.getNetherWorld();
        World endWorld = bskyblock.getEndWorld();

        // If no dimension worlds, nothing to do
        if (netherWorld == null && endWorld == null) {
            addon.log("No nether/end worlds - skipping dimension block relocation");
            callback.accept(true);
            return;
        }

        addon.log("Starting dimension block relocation for nether/end");

        // Chain: relocate nether first, then end
        Runnable relocateEnd = () -> {
            if (endWorld != null) {
                sendProgressToAllOnline("&eRelocating End dimension...");
                // End uses 1:1 scaling
                Location oldEndCenter = new Location(endWorld,
                    oldCenter.getX(), oldCenter.getY(), oldCenter.getZ());
                Location newEndCenter = new Location(endWorld,
                    newCenter.getX(), newCenter.getY(), newCenter.getZ());

                relocateSingleDimensionAsync(oldEndCenter, newEndCenter, range, success -> {
                    if (success) {
                        addon.log("End dimension blocks relocated successfully");
                    } else {
                        addon.logWarning("Failed to relocate End dimension blocks");
                    }
                    callback.accept(true); // Continue even if dimension relocation fails
                });
            } else {
                callback.accept(true);
            }
        };

        if (netherWorld != null) {
            sendProgressToAllOnline("&eRelocating Nether dimension...");
            // BSkyBlock places islands at the SAME coordinates in all dimensions
            // The 1:8 nether scaling only applies to vanilla portal mechanics, NOT island placement
            Location oldNetherCenter = new Location(netherWorld,
                oldCenter.getX(), oldCenter.getY(), oldCenter.getZ());
            Location newNetherCenter = new Location(netherWorld,
                newCenter.getX(), newCenter.getY(), newCenter.getZ());

            // Use same range as overworld (BSkyBlock uses same island size in all dimensions)
            relocateSingleDimensionAsync(oldNetherCenter, newNetherCenter, range, success -> {
                if (success) {
                    addon.log("Nether dimension blocks relocated successfully");
                } else {
                    addon.logWarning("Failed to relocate Nether dimension blocks");
                }
                // Continue to End regardless of nether success
                relocateEnd.run();
            });
        } else {
            relocateEnd.run();
        }
    }

    /**
     * Relocate blocks in a single dimension (copy -> clear -> paste).
     *
     * @param oldCenter The old center location in this dimension
     * @param newCenter The new center location in this dimension
     * @param range The range for block operations
     * @param callback Called when relocation is complete
     */
    private void relocateSingleDimensionAsync(Location oldCenter, Location newCenter, int range,
                                               java.util.function.Consumer<Boolean> callback) {
        World world = oldCenter.getWorld();
        if (world == null || newCenter.getWorld() == null) {
            addon.logWarning("Invalid world for dimension relocation");
            callback.accept(false);
            return;
        }

        addon.log("Relocating blocks in " + world.getName() + " from " +
                oldCenter.getBlockX() + "," + oldCenter.getBlockZ() + " to " +
                newCenter.getBlockX() + "," + newCenter.getBlockZ());

        // Capture entities in this dimension
        final java.util.List<Object> capturedEntities = addon.getEntityStorage().captureEntitiesInMemory(world, oldCenter, range);
        addon.log("Captured " + capturedEntities.size() + " entities in " + world.getName());

        // Step 1: Copy blocks to clipboard
        addon.getSchematicUtils().copyToClipboardAsync(oldCenter, range, true, clipboard -> {
            if (clipboard == null) {
                addon.logWarning("Failed to copy blocks in " + world.getName());
                callback.accept(false);
                return;
            }

            // Step 2: Remove entities from old location
            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                addon.getEntityStorage().removeEntities(world, oldCenter, range);

                // Step 3: Clear old blocks
                addon.getSchematicUtils().clearRegionAsync(oldCenter, range, clearSuccess -> {
                    if (!clearSuccess) {
                        addon.logWarning("Failed to clear old blocks in " + world.getName());
                        // Continue anyway to paste at new location
                    }

                    // Step 4: Paste at new location
                    addon.getSchematicUtils().pasteFromClipboardAsync(clipboard, newCenter, pasteSuccess -> {
                        if (!pasteSuccess) {
                            addon.logWarning("Failed to paste blocks in " + world.getName());
                            callback.accept(false);
                            return;
                        }

                        // Step 5: Restore entities at new location
                        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                            int restoredCount = addon.getEntityStorage().restoreEntitiesInMemory(capturedEntities, newCenter);
                            addon.log("Restored " + restoredCount + " entities in " + world.getName());
                            callback.accept(true);
                        });
                    });
                });
            });
        });
    }

    /**
     * Send a progress message to the relocating player (if we have reference).
     * This is a best-effort message since we may not have the player reference in all contexts.
     */
    private void sendProgressToAllOnline(String message) {
        // This is called during async dimension relocation where we don't have player reference
        // We just log instead
        addon.log(message.replace("&e", "").replace("&a", ""));
    }

    /**
     * Check if a world is one of BSkyBlock's native worlds (overworld, nether, end).
     * BSkyBlock handles these as sub-worlds of the same island - we should not
     * treat them as separate dimensions for teleportation purposes.
     *
     * @param world The world to check
     * @return true if this is a BSkyBlock-managed native world
     */
    private boolean isBSkyBlockNativeWorld(World world) {
        if (world == null) {
            return false;
        }

        var bskyblock = addon.getBSkyBlockAddon();
        if (bskyblock == null) {
            return false;
        }

        // Check by world object reference
        World overworld = bskyblock.getOverWorld();
        World nether = bskyblock.getNetherWorld();
        World end = bskyblock.getEndWorld();

        if (world.equals(overworld) || world.equals(nether) || world.equals(end)) {
            return true;
        }

        // Fallback: Check by world name pattern
        // getNetherWorld() or getEndWorld() may return null if those dimensions are
        // disabled in BSkyBlock config, but the world might still exist with the
        // standard naming convention: bskyblock_world, bskyblock_world_nether, bskyblock_world_the_end
        if (overworld != null) {
            String baseName = overworld.getName();
            String worldName = world.getName();
            if (worldName.equals(baseName) ||
                worldName.equals(baseName + "_nether") ||
                worldName.equals(baseName + "_the_end")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Safely teleport player and team members to new island location.
     * This method loads the chunk first to prevent falling through blocks.
     *
     * @param island The main dimension island
     * @param owner The island owner
     * @param newCenter The new center location in main dimension
     * @param originWorld The world the player was in before relocation (to return them there)
     */
    private void teleportPlayersSafely(Island island, Player owner, Location newCenter, World originWorld) {
        try {
            // Determine target world and island based on origin dimension
            World targetWorld = newCenter.getWorld();
            Island targetIsland = island;
            Location targetHome = null;

            // Check if player was in a CUSTOM dimension world (not BSkyBlock's native worlds)
            // BSkyBlock's native worlds (overworld, nether, end) are sub-worlds of the same island
            // and should always teleport to the main island home, not dimension-specific teleportation
            if (originWorld != null && addon.getSettings().isMultiDimensionEnabled() && addon.getDimensionManager() != null) {
                // Skip dimension-based teleportation for BSkyBlock's native worlds
                if (!isBSkyBlockNativeWorld(originWorld)) {
                    DimensionManager dimManager = addon.getDimensionManager();
                    String originDimKey = dimManager.getDimensionKey(originWorld);

                    if (originDimKey != null) {
                        // Player was in a custom dimension world - use that dimension's island
                        Island dimIsland = addon.getIslands().getIsland(originWorld, owner.getUniqueId());
                        if (dimIsland != null) {
                            targetIsland = dimIsland;
                            targetWorld = originWorld;
                            targetHome = dimIsland.getHome("");
                            addon.log("Player was in custom dimension '" + originDimKey + "' - teleporting back there");
                        }
                    }
                } else {
                    addon.log("Player was in BSkyBlock native world - teleporting to main island home");
                }
            }

            if (targetWorld == null) {
                targetWorld = newCenter.getWorld();
            }
            if (targetWorld == null) {
                addon.logError("Cannot teleport - world is null");
                return;
            }

            // First, load the chunk at the target location synchronously
            Location chunkLoadLocation = targetHome != null ? targetHome : newCenter;
            int chunkX = chunkLoadLocation.getBlockX() >> 4;
            int chunkZ = chunkLoadLocation.getBlockZ() >> 4;

            // Force load the chunk and surrounding chunks
            targetWorld.loadChunk(chunkX, chunkZ, true);
            targetWorld.loadChunk(chunkX + 1, chunkZ, true);
            targetWorld.loadChunk(chunkX - 1, chunkZ, true);
            targetWorld.loadChunk(chunkX, chunkZ + 1, true);
            targetWorld.loadChunk(chunkX, chunkZ - 1, true);

            // Final variables for lambda
            final Island finalTargetIsland = targetIsland;
            final World finalTargetWorld = targetWorld;

            // Wait a bit for chunks to fully generate, then teleport
            Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
                // CRITICAL: Find a home that is in the TARGET world, not just any default home.
                // getHome("") returns the default home which could be in ANY dimension (nether, end).
                // We must filter to find a home in the target world (typically overworld).
                Location homeLocation = null;
                java.util.Map<String, Location> allHomes = finalTargetIsland.getHomes();
                if (allHomes != null && !allHomes.isEmpty()) {
                    for (Location homeLoc : allHomes.values()) {
                        if (homeLoc != null && homeLoc.getWorld() != null
                            && homeLoc.getWorld().equals(finalTargetWorld)) {
                            homeLocation = homeLoc;
                            break; // Use first home found in target world
                        }
                    }
                }

                // Fallback to spawn point in target world
                if (homeLocation == null) {
                    homeLocation = finalTargetIsland.getSpawnPoint(finalTargetWorld.getEnvironment());
                }

                // Final fallback to island center or newCenter
                if (homeLocation == null) {
                    Location dimCenter = finalTargetIsland.getCenter();
                    homeLocation = dimCenter != null ? dimCenter : newCenter;
                }

                // Use BentoBox SafeSpotTeleport for owner, targeting home location
                // IMPORTANT: Do NOT use .island() as it searches across ALL dimensions (nether, end)
                // and may teleport to End if it finds a "safer" spot there
                new SafeSpotTeleport.Builder(addon.getPlugin())
                    .entity(owner)
                    .location(homeLocation)
                    .thenRun(() -> addon.log("Teleported " + owner.getName() + " to relocated island home in " + finalTargetWorld.getName()))
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

    // Helper methods for sending messages to admin by UUID (safe for async use)
    private void sendProgressToAdmin(UUID adminUUID, String message) {
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            Player admin = Bukkit.getPlayer(adminUUID);
            if (admin != null && admin.isOnline()) {
                admin.sendMessage(colorize(message));
            }
        });
    }

    private void sendErrorToAdmin(UUID adminUUID, String message) {
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            Player admin = Bukkit.getPlayer(adminUUID);
            if (admin != null && admin.isOnline()) {
                admin.sendMessage(colorize(message));
            }
        });
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
     * IMPORTANT: Only captures homes that are in the SAME world as the center.
     * This is critical because BSkyBlock shares one Island object across all dimensions
     * (overworld, nether, end), and island.getHomes() returns ALL homes from ALL dimensions.
     * We must filter to only capture homes in the current dimension's world.
     *
     * @param island The island to capture homes from
     * @param center The island center location
     * @return Map of home names to relative positions (only homes in center's world)
     */
    private java.util.Map<String, RelativeHome> captureHomesRelativeToCenter(Island island, Location center) {
        java.util.Map<String, RelativeHome> captured = new java.util.HashMap<>();

        if (island == null || center == null || center.getWorld() == null) {
            return captured;
        }

        World centerWorld = center.getWorld();
        java.util.Map<String, Location> homes = island.getHomes();
        if (homes == null || homes.isEmpty()) {
            return captured;
        }

        for (java.util.Map.Entry<String, Location> entry : homes.entrySet()) {
            Location homeLoc = entry.getValue();
            // CRITICAL: Only capture homes that are in the SAME world as the center
            // This prevents cross-world coordinate corruption when BSkyBlock shares one Island object
            if (homeLoc != null && homeLoc.getWorld() != null
                && homeLoc.getWorld().equals(centerWorld)) {
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
     * IMPORTANT: Only removes/replaces homes that are in the SAME world as newCenter.
     * This is critical because BSkyBlock shares one Island object across all dimensions
     * (overworld, nether, end). If we clear ALL homes, we would delete homes from other
     * dimensions that we're not currently restoring.
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

        // CRITICAL: Only remove homes that are in THIS world
        // BSkyBlock shares one Island object across all dimensions, so we must NOT
        // delete homes from other dimensions (nether, end) when restoring overworld homes
        java.util.Map<String, Location> existingHomes = island.getHomes();
        if (existingHomes != null && !existingHomes.isEmpty()) {
            java.util.List<String> homesToRemove = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, Location> entry : existingHomes.entrySet()) {
                Location homeLoc = entry.getValue();
                // Only remove homes in the SAME world we're restoring to
                if (homeLoc != null && homeLoc.getWorld() != null
                    && homeLoc.getWorld().equals(world)) {
                    homesToRemove.add(entry.getKey());
                }
            }
            for (String homeName : homesToRemove) {
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

    /**
     * Data class to hold all captured spawn points for relocation.
     * BentoBox's /island command uses spawn points, not homes.
     */
    private static class CapturedSpawnPoints {
        final Map<World.Environment, RelativeHome> spawnPoints = new HashMap<>();
    }

    /**
     * Capture spawn points for all environments relative to the island center.
     * This is needed because /island uses getSpawnPoint(), not getHome().
     *
     * @param island The island to capture spawn points from
     * @param center The island center location
     * @return CapturedSpawnPoints containing relative positions for each environment
     */
    private CapturedSpawnPoints captureSpawnPoints(Island island, Location center) {
        CapturedSpawnPoints captured = new CapturedSpawnPoints();

        if (island == null || center == null) {
            return captured;
        }

        for (World.Environment env : World.Environment.values()) {
            Location spawnPoint = island.getSpawnPoint(env);
            if (spawnPoint != null && spawnPoint.getWorld() != null) {
                RelativeHome relSpawn = new RelativeHome();
                relSpawn.relX = spawnPoint.getX() - center.getX();
                relSpawn.relY = spawnPoint.getY() - center.getY();
                relSpawn.relZ = spawnPoint.getZ() - center.getZ();
                relSpawn.yaw = spawnPoint.getYaw();
                relSpawn.pitch = spawnPoint.getPitch();

                captured.spawnPoints.put(env, relSpawn);
                addon.log("Captured spawn point for " + env + " relative to center");
            }
        }

        return captured;
    }

    /**
     * Restore spawn points at the new center location.
     *
     * @param island The island to restore spawn points to
     * @param newCenter The new island center
     * @param captured The captured spawn points
     */
    private void restoreSpawnPoints(Island island, Location newCenter, CapturedSpawnPoints captured) {
        if (island == null || newCenter == null || captured == null) {
            return;
        }

        World mainWorld = newCenter.getWorld();
        if (mainWorld == null) {
            return;
        }

        for (Map.Entry<World.Environment, RelativeHome> entry : captured.spawnPoints.entrySet()) {
            World.Environment env = entry.getKey();
            RelativeHome relSpawn = entry.getValue();

            // Determine the correct world for this environment
            World targetWorld = mainWorld;
            var bskyblock = addon.getBSkyBlockAddon();
            if (bskyblock != null) {
                if (env == World.Environment.NETHER && bskyblock.getNetherWorld() != null) {
                    targetWorld = bskyblock.getNetherWorld();
                } else if (env == World.Environment.THE_END && bskyblock.getEndWorld() != null) {
                    targetWorld = bskyblock.getEndWorld();
                }
            }

            // Calculate new spawn location at relative position
            // BSkyBlock uses the SAME coordinates in all dimensions (1:1 scaling)
            // The 1:8 nether scaling only applies to vanilla portal mechanics, NOT island placement
            double newX = newCenter.getX() + relSpawn.relX;
            double newY = newCenter.getY() + relSpawn.relY;
            double newZ = newCenter.getZ() + relSpawn.relZ;

            Location newSpawn = new Location(
                targetWorld,
                newX, newY, newZ,
                relSpawn.yaw, relSpawn.pitch
            );

            island.setSpawnPoint(env, newSpawn);
            addon.log("Restored spawn point for " + env + " at " + newSpawn.getBlockX() + "," + newSpawn.getBlockZ());
        }
    }

    /**
     * Capture homes from all dimensions (main + BSkyBlock native + custom dimension islands).
     * This ensures homes in nether/end/custom dimensions are also relocated.
     *
     * CRITICAL: BSkyBlock shares ONE Island object for overworld, nether, and end.
     * We must capture homes from all three BSkyBlock native worlds, even though they
     * are NOT in dimManager.getEnabledDimensions(). Otherwise nether/end homes will
     * still point to old coordinates after relocation.
     *
     * @param playerUUID The player's UUID
     * @param mainCenter The main dimension island center
     * @return Map of dimension key to captured homes (dimension key -> home name -> RelativeHome)
     */
    private Map<String, Map<String, RelativeHome>> captureAllDimensionHomes(UUID playerUUID, Location mainCenter) {
        Map<String, Map<String, RelativeHome>> allHomes = new HashMap<>();

        // Always capture main dimension
        World mainWorld = addon.getGridManager().getBSkyBlockWorld();
        Island mainIsland = null;
        if (mainWorld != null) {
            mainIsland = addon.getIslands().getIsland(mainWorld, playerUUID);
            if (mainIsland != null) {
                Map<String, RelativeHome> mainHomes = captureHomesRelativeToCenter(mainIsland, mainCenter);
                if (!mainHomes.isEmpty()) {
                    allHomes.put("main", mainHomes);
                    addon.log("Captured " + mainHomes.size() + " homes from main dimension");
                }
            }
        }

        // CRITICAL: Capture BSkyBlock native dimension homes (nether, end)
        // These are NOT in dimManager.getEnabledDimensions() but share the same Island object.
        // If we don't capture these, nether/end homes will point to old coordinates after relocation.
        var bskyblock = addon.getBSkyBlockAddon();
        if (bskyblock != null && mainIsland != null) {
            World netherWorld = bskyblock.getNetherWorld();
            World endWorld = bskyblock.getEndWorld();

            // Capture nether homes (same Island object, different world filter)
            if (netherWorld != null) {
                // BSkyBlock uses same coordinates in all dimensions (1:1 scaling)
                Location netherCenter = new Location(netherWorld,
                    mainCenter.getX(), mainCenter.getY(), mainCenter.getZ());
                Map<String, RelativeHome> netherHomes = captureHomesRelativeToCenter(mainIsland, netherCenter);
                if (!netherHomes.isEmpty()) {
                    allHomes.put("bskyblock_nether", netherHomes);
                    addon.log("Captured " + netherHomes.size() + " homes from BSkyBlock nether");
                }
            }

            // Capture end homes (same Island object, different world filter)
            if (endWorld != null) {
                Location endCenter = new Location(endWorld,
                    mainCenter.getX(), mainCenter.getY(), mainCenter.getZ());
                Map<String, RelativeHome> endHomes = captureHomesRelativeToCenter(mainIsland, endCenter);
                if (!endHomes.isEmpty()) {
                    allHomes.put("bskyblock_end", endHomes);
                    addon.log("Captured " + endHomes.size() + " homes from BSkyBlock end");
                }
            }
        }

        // If multi-dimension enabled, capture from each custom dimension
        // CRITICAL: Skip custom dimensions that are BSkyBlock native worlds - these are already
        // handled above through "main", "bskyblock_nether", "bskyblock_end" keys.
        // Processing them twice would cause homes to be captured with wrong relative coordinates.
        if (addon.getSettings().isMultiDimensionEnabled() && addon.getDimensionManager() != null) {
            DimensionManager dimManager = addon.getDimensionManager();
            for (DimensionConfig config : dimManager.getEnabledDimensions()) {
                String dimKey = config.getDimensionKey();
                World dimWorld = dimManager.getWorld(dimKey);
                if (dimWorld != null) {
                    // Skip custom dimensions that point to BSkyBlock native worlds
                    if (isBSkyBlockNativeWorld(dimWorld)) {
                        addon.log("Skipping custom dimension '" + dimKey + "' - is BSkyBlock native world");
                        continue;
                    }

                    Island dimIsland = addon.getIslands().getIsland(dimWorld, playerUUID);
                    if (dimIsland != null) {
                        Location dimCenter = dimIsland.getCenter();
                        if (dimCenter != null) {
                            Map<String, RelativeHome> dimHomes = captureHomesRelativeToCenter(dimIsland, dimCenter);
                            if (!dimHomes.isEmpty()) {
                                allHomes.put(dimKey, dimHomes);
                                addon.log("Captured " + dimHomes.size() + " homes from dimension: " + dimKey);
                            }
                        }
                    }
                }
            }
        }

        return allHomes;
    }

    /**
     * Restore captured homes to all dimensions (main + BSkyBlock native + custom dimension islands).
     * Each dimension's homes are restored at the appropriate relative positions.
     *
     * CRITICAL: BSkyBlock shares ONE Island object for overworld, nether, and end.
     * We must restore homes to all three BSkyBlock native worlds using the same Island object.
     *
     * @param playerUUID The player's UUID
     * @param newMainCenter The new main dimension island center
     * @param allCapturedHomes Map of dimension key to captured homes
     */
    private void restoreAllDimensionHomes(UUID playerUUID, Location newMainCenter,
                                           Map<String, Map<String, RelativeHome>> allCapturedHomes) {
        if (allCapturedHomes == null || allCapturedHomes.isEmpty()) {
            return;
        }

        // Restore main dimension
        World mainWorld = addon.getGridManager().getBSkyBlockWorld();
        Island mainIsland = null;
        if (mainWorld != null && allCapturedHomes.containsKey("main")) {
            mainIsland = addon.getIslands().getIsland(mainWorld, playerUUID);
            if (mainIsland != null) {
                int restored = restoreHomesAtNewCenter(mainIsland, newMainCenter, allCapturedHomes.get("main"));
                addon.log("Restored " + restored + " homes in main dimension");
            }
        }

        // CRITICAL: Restore BSkyBlock native dimension homes (nether, end)
        // These use the SAME Island object as the main dimension.
        var bskyblock = addon.getBSkyBlockAddon();
        if (bskyblock != null && mainIsland != null) {
            // Restore nether homes
            if (allCapturedHomes.containsKey("bskyblock_nether")) {
                World netherWorld = bskyblock.getNetherWorld();
                if (netherWorld != null) {
                    // BSkyBlock uses same coordinates in all dimensions (1:1 scaling)
                    Location newNetherCenter = new Location(netherWorld,
                        newMainCenter.getX(), newMainCenter.getY(), newMainCenter.getZ());
                    int restored = restoreHomesAtNewCenter(mainIsland, newNetherCenter,
                        allCapturedHomes.get("bskyblock_nether"));
                    addon.log("Restored " + restored + " homes in BSkyBlock nether");
                }
            }

            // Restore end homes
            if (allCapturedHomes.containsKey("bskyblock_end")) {
                World endWorld = bskyblock.getEndWorld();
                if (endWorld != null) {
                    Location newEndCenter = new Location(endWorld,
                        newMainCenter.getX(), newMainCenter.getY(), newMainCenter.getZ());
                    int restored = restoreHomesAtNewCenter(mainIsland, newEndCenter,
                        allCapturedHomes.get("bskyblock_end"));
                    addon.log("Restored " + restored + " homes in BSkyBlock end");
                }
            }
        }

        // If multi-dimension enabled, restore to each custom dimension
        // CRITICAL: Skip custom dimensions that are BSkyBlock native worlds - these are already
        // handled above. Processing them twice would OVERWRITE the homes we just restored!
        if (addon.getSettings().isMultiDimensionEnabled() && addon.getDimensionManager() != null) {
            DimensionManager dimManager = addon.getDimensionManager();
            for (DimensionConfig config : dimManager.getEnabledDimensions()) {
                String dimKey = config.getDimensionKey();
                if (allCapturedHomes.containsKey(dimKey)) {
                    World dimWorld = dimManager.getWorld(dimKey);
                    if (dimWorld != null) {
                        // Skip custom dimensions that point to BSkyBlock native worlds
                        if (isBSkyBlockNativeWorld(dimWorld)) {
                            addon.log("Skipping restore for custom dimension '" + dimKey + "' - is BSkyBlock native world");
                            continue;
                        }

                        Island dimIsland = addon.getIslands().getIsland(dimWorld, playerUUID);
                        if (dimIsland != null) {
                            // Calculate new center for this dimension (same X/Z as main, keep dimension's Y)
                            Location dimCenter = dimIsland.getCenter();
                            if (dimCenter != null) {
                                Location newDimCenter = new Location(
                                    dimWorld,
                                    newMainCenter.getX(),
                                    dimCenter.getY(),
                                    newMainCenter.getZ()
                                );
                                int restored = restoreHomesAtNewCenter(dimIsland, newDimCenter, allCapturedHomes.get(dimKey));
                                addon.log("Restored " + restored + " homes in dimension: " + dimKey);
                            }
                        }
                    }
                }
            }
        }
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
