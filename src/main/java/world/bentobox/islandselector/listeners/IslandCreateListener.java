package world.bentobox.islandselector.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandCreatedEvent;
import world.bentobox.bentobox.api.events.island.IslandDeleteEvent;
import world.bentobox.bentobox.api.events.island.IslandPreCreateEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.island.NewIsland;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.events.GridLocationClaimEvent;
import world.bentobox.islandselector.gui.IslandClaimGUI;
import world.bentobox.islandselector.managers.DimensionManager;
import world.bentobox.islandselector.managers.GridLocationStrategy;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.managers.MultiDimensionIslandCreator;
import world.bentobox.islandselector.managers.SlotManager;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Listener that intercepts island creation and allows players to choose their location.
 * Also handles island deletion to update the grid.
 */
public class IslandCreateListener implements Listener {

    private final IslandSelector addon;
    private final GridManager gridManager;
    private final SlotManager slotManager;

    // Track pending island claims (player UUID -> grid coordinate)
    // Note: Cleaned up by onPlayerQuit() when player disconnects
    private final Map<UUID, GridCoordinate> pendingClaims = new HashMap<>();

    // Track players who should skip the GUI (already selected a location)
    private final Map<UUID, String> confirmedBlueprints = new HashMap<>();

    // Track players who are resetting their island (should keep same location)
    private final Map<UUID, GridCoordinate> pendingResets = new HashMap<>();

    // Track old island IDs that need to be deleted after reset completes
    private final Map<UUID, String> pendingOldIslandDeletions = new HashMap<>();

    // Track players whose islands are being removed by admin (skip slot data clearing in onIslandDelete)
    private final Set<UUID> pendingAdminRemovals = new HashSet<>();

    public IslandCreateListener(IslandSelector addon) {
        this.addon = addon;
        this.gridManager = addon.getGridManager();
        this.slotManager = addon.getSlotManager();
    }

    /**
     * Intercept island creation BEFORE it happens (pre-create phase).
     * If player hasn't chosen a location yet, cancel and show grid GUI.
     * If player has chosen a location, let BSkyBlock create it - we'll register it after.
     * If player is "homeless" (has saved slot data but no island), show restoration GUI.
     * If player is resetting (already has island at a grid location), let it proceed at same location.
     *
     * NOTE: We use IslandPreCreateEvent because it fires BEFORE the island is created.
     * IslandCreateEvent fires DURING creation when it's too late to cancel cleanly.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onIslandPreCreate(IslandPreCreateEvent event) {
        addon.log("=== IslandPreCreateEvent triggered ===");

        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        if (player == null) {
            addon.log("Player is null for UUID: " + event.getPlayerUUID());
            return;
        }

        UUID playerUUID = player.getUniqueId();
        addon.log("Processing IslandPreCreateEvent for player: " + player.getName());

        // Check if this player has a confirmed claim
        if (pendingClaims.containsKey(playerUUID)) {
            // Player has selected a location - let island creation proceed
            // We'll move it in the IslandCreatedEvent handler
            addon.log("Allowing island creation for " + player.getName() + " - will relocate after creation");
            return;
        }

        // Check if player is actively restoring (from IslandRestoreGUI)
        // This prevents the homeless check from blocking the restoration process
        if (slotManager.hasPendingSlotRestoration(playerUUID)) {
            addon.log("Allowing island creation for " + player.getName() + " - active restoration in progress");
            return;
        }

        // Check if player is resetting (already has a pending reset tracked)
        if (pendingResets.containsKey(playerUUID)) {
            addon.log("Allowing island reset for " + player.getName() + " - reset in progress at same location");
            return;
        }

        // Check if this is part of a multi-dimension creation (addon-initiated)
        MultiDimensionIslandCreator multiDimCreator = addon.getMultiDimensionIslandCreator();
        if (multiDimCreator != null && multiDimCreator.hasPendingCreation(playerUUID)) {
            addon.log("Allowing IslandPreCreateEvent for " + player.getName() + " - part of multi-dimension creation");
            return;
        }

        // Check if player already has an island at a grid location (this is a reset, not a new creation)
        // This handles /island restart - player should keep the same location
        String existingGridCoord = slotManager.getPlayerGridCoordinate(playerUUID);
        if (existingGridCoord != null) {
            // Player has an existing grid location - this is likely a reset
            GridCoordinate coord = GridCoordinate.parse(existingGridCoord);
            if (coord != null) {
                // Check if they actually have a BentoBox island
                World bskyWorld = gridManager.getBSkyBlockWorld();
                if (bskyWorld == null) {
                    addon.logWarning("BSkyBlock world not available during reset check");
                    return;
                }
                Island existingIsland = BentoBox.getInstance().getIslands()
                    .getIsland(bskyWorld, playerUUID);

                if (existingIsland != null) {
                    // Player is resetting their island - cancel BentoBox's default reset
                    // and handle it ourselves to keep the same location
                    event.setCancelled(true);
                    addon.log("Player " + player.getName() + " is resetting island at " + existingGridCoord + " - handling reset manually");

                    // Store pending reset and confirm claim at same location
                    pendingResets.put(playerUUID, coord);
                    pendingClaims.put(playerUUID, coord);

                    // Perform the reset ourselves at the same location
                    Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                        performResetAtLocation(player, coord, existingIsland);
                    });
                    return;
                } else {
                    // BentoBox already deleted the island (happens during /island restart flow)
                    // but player still has slot data - this IS a reset, create at same location
                    event.setCancelled(true);
                    addon.log("Player " + player.getName() + " is resetting island at " + existingGridCoord + " - island already deleted by BentoBox, creating at same location");

                    // Get the old island UUID from slot data so we can clean up the database file
                    SlotData activeSlot = slotManager.getActiveSlot(playerUUID);
                    String oldIslandUUID = activeSlot != null ? activeSlot.getIslandUUID() : null;
                    addon.log("Old island ID from slot data: " + oldIslandUUID);

                    // Store pending reset and confirm claim at same location
                    pendingResets.put(playerUUID, coord);
                    pendingClaims.put(playerUUID, coord);

                    // Store old island ID for deletion AFTER the new island is created
                    // (BentoBox may re-save the file if we delete too early)
                    if (oldIslandUUID != null && !oldIslandUUID.isEmpty()) {
                        pendingOldIslandDeletions.put(playerUUID, oldIslandUUID);
                    }

                    // Create the island at the same location using our location strategy
                    Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                        createResetIslandAtLocation(player, coord);
                    });
                    return;
                }
            }
        }

        // Debug homeless check
        addon.log("Checking homeless status for " + player.getName() + "...");
        boolean isHomeless = addon.getIslandRemovalManager().isHomeless(playerUUID);
        addon.log("isHomeless result: " + isHomeless);

        // Check if player is "homeless" - has saved slot data but no island
        if (isHomeless) {
            event.setCancelled(true);
            addon.log("Player " + player.getName() + " is homeless - showing restoration GUI");

            // Open the slot restoration GUI instead of new island creation
            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                new world.bentobox.islandselector.gui.SlotRestorationGUI(addon, player, this).open();
            });
            return;
        }

        // Player hasn't chosen a location yet - cancel creation and show GUI
        event.setCancelled(true);
        addon.log("Cancelled island pre-creation for " + player.getName() + " - showing grid selection");

        // Open the island claim GUI
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            new IslandClaimGUI(addon, player, IslandCreateListener.this).open();
        });
    }

    /**
     * Handle island creation completion - register the island in our grid.
     * The island is already at the correct location thanks to GridLocationStrategy.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandCreated(IslandCreatedEvent event) {
        UUID playerUUID = event.getPlayerUUID();
        Player player = Bukkit.getPlayer(playerUUID);
        Island island = event.getIsland();
        World islandWorld = island != null && island.getCenter() != null ? island.getCenter().getWorld() : null;

        // Check if this is part of a multi-dimension creation
        MultiDimensionIslandCreator multiDimCreator = addon.getMultiDimensionIslandCreator();
        if (multiDimCreator != null && multiDimCreator.hasPendingCreation(playerUUID)) {
            boolean handled = multiDimCreator.onIslandCreated(playerUUID, island, islandWorld);
            if (handled) {
                addon.log("Island creation handled by MultiDimensionIslandCreator");
                return;
            }
        }

        // Check if this was a grid-based claim
        GridCoordinate coord = pendingClaims.remove(playerUUID);
        String blueprintBundle = confirmedBlueprints.remove(playerUUID);

        // Check if this was a reset (not a new creation)
        boolean isReset = pendingResets.remove(playerUUID) != null;

        if (coord != null) {
            String ownerName = player != null ? player.getName() : "Unknown";

            // Calculate the world coordinates for this grid position
            int worldX = calculateWorldX(coord);
            int worldZ = calculateWorldZ(coord);

            // Get the full BentoBox island ID (e.g., "BSkyBlock6d68f389-0cb4-4422-9619-66e82dba77f4")
            String bentoBoxIslandId = island.getUniqueId();

            // Try to extract UUID for grid manager (which expects UUID), but keep full ID for slot data
            UUID islandUUID = null;
            try {
                islandUUID = UUID.fromString(bentoBoxIslandId);
            } catch (IllegalArgumentException e) {
                // Island ID has a prefix (like "BSkyBlock"), try to extract the UUID part
                if (bentoBoxIslandId != null && bentoBoxIslandId.contains("-")) {
                    // Find the UUID part (starts after any non-UUID prefix)
                    int uuidStart = bentoBoxIslandId.indexOf('-') - 8; // UUID format: 8-4-4-4-12
                    if (uuidStart > 0) {
                        String uuidPart = bentoBoxIslandId.substring(uuidStart);
                        try {
                            islandUUID = UUID.fromString(uuidPart);
                        } catch (IllegalArgumentException e2) {
                            // Still not a valid UUID, leave as null
                        }
                    }
                }
            }

            gridManager.occupyLocation(coord, playerUUID, ownerName, islandUUID);

            if (isReset) {
                // For resets, just update the existing slot's island UUID
                // Store the FULL BentoBox island ID so we can delete the file on next reset
                SlotData activeSlot = slotManager.getActiveSlot(playerUUID);
                if (activeSlot != null) {
                    activeSlot.setIslandUUID(bentoBoxIslandId);
                    slotManager.saveSlot(activeSlot);
                    addon.log("Updated active slot with new island ID: " + bentoBoxIslandId);
                }
            } else {
                // Initialize slot 1 for this player's first island
                // Store the FULL BentoBox island ID so we can delete the file on next reset
                slotManager.initializePlayerSlots(playerUUID, bentoBoxIslandId, coord.toString());

                // Store the blueprint bundle in the slot data for blueprint-specific challenges
                if (blueprintBundle != null && !blueprintBundle.isEmpty()) {
                    slotManager.setBlueprintBundle(playerUUID, 1, blueprintBundle);
                    addon.log("Set blueprint bundle '" + blueprintBundle + "' for slot 1");

                    // Apply blueprint permissions for challenges if player is online
                    if (player != null) {
                        addon.getBlueprintChallengesManager().updateBlueprintPermissions(player, blueprintBundle);
                    }

                    // Ensure nether/end blueprints are pasted (NewIsland.builder() with custom
                    // locationStrategy may not trigger nether/end pasting automatically)
                    ensureNetherEndBlueprints(island, blueprintBundle);
                }

                addon.log("Initialized slot 1 for player " + ownerName);
            }

            addon.log("Registered island at grid " + coord + " for " + ownerName + (isReset ? " (reset)" : " (new)"));

            // Verify the island was created at the correct location
            Location center = island.getCenter();
            if (center != null) {
                addon.log("Island center: " + center.getBlockX() + ", " + center.getBlockZ() +
                         " (expected: " + worldX + ", " + worldZ + ")");
            }

            // If this was a reset, delete the old island file after a delay
            // (delay ensures BentoBox has finished any async save operations)
            if (isReset) {
                String oldIslandId = pendingOldIslandDeletions.remove(playerUUID);
                if (oldIslandId != null && !oldIslandId.equals(bentoBoxIslandId)) {
                    addon.log("Scheduling deletion of old island file: " + oldIslandId);
                    Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
                        deleteIslandDatabaseFile(oldIslandId);
                    }, 60L); // Wait 3 seconds for BentoBox to finish any async saves
                }
            }

            if (player != null) {
                if (isReset) {
                    player.sendMessage("§a§lIsland Reset Complete!");
                    player.sendMessage("§7Your island has been reset at location §f" + coord.toString());
                } else {
                    player.sendMessage("§a§lIsland Created!");
                    player.sendMessage("§7Your island has been created at location §f" + coord.toString());
                    player.sendMessage("§7World coordinates: §fX: " + worldX + ", Z: " + worldZ);
                }
            }
        }
    }

    /**
     * Calculate world X coordinate from grid coordinate
     */
    private int calculateWorldX(GridCoordinate coord) {
        // BSkyBlock's "distance-between-islands" is the offset from center
        // Real spacing between island centers is distance * 2
        int spacing = addon.getIslandSpacing() * 2;
        return coord.getX() * spacing;
    }

    /**
     * Calculate world Z coordinate from grid coordinate
     */
    private int calculateWorldZ(GridCoordinate coord) {
        // BSkyBlock's "distance-between-islands" is the offset from center
        // Real spacing between island centers is distance * 2
        int spacing = addon.getIslandSpacing() * 2;
        return coord.getZ() * spacing;
    }

    /**
     * Perform island reset at the same grid location.
     * This deletes the old island and creates a new one at the same spot.
     */
    private void performResetAtLocation(Player player, GridCoordinate coord, Island oldIsland) {
        UUID playerUUID = player.getUniqueId();
        World world = gridManager.getBSkyBlockWorld();
        User user = User.getInstance(player);

        addon.log("Performing manual reset for " + player.getName() + " at " + coord);

        // Delete the old island
        try {
            BentoBox.getInstance().getIslands().deleteIsland(oldIsland, true, playerUUID);
            addon.log("Deleted old island for " + player.getName());
        } catch (Exception e) {
            addon.logError("Failed to delete old island: " + e.getMessage());
            player.sendMessage("§cFailed to reset island. Please try again.");
            pendingClaims.remove(playerUUID);
            pendingResets.remove(playerUUID);
            return;
        }

        // Wait a moment for deletion to complete, then create new island
        Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
            // Create island at the same grid location using our custom location strategy
            GridLocationStrategy locationStrategy = new GridLocationStrategy(addon, coord, world);

            try {
                NewIsland.builder()
                    .player(user)
                    .addon(addon.getBSkyBlockAddon())
                    .reason(IslandEvent.Reason.RESET)
                    .locationStrategy(locationStrategy)
                    .build();

                addon.log("Initiated island reset at grid " + coord + " for " + player.getName());

            } catch (Exception e) {
                addon.logError("Failed to create reset island: " + e.getMessage());
                player.sendMessage("§cFailed to create new island. Please contact an admin.");
                pendingClaims.remove(playerUUID);
                pendingResets.remove(playerUUID);
            }
        }, 20L); // Wait 1 second for deletion to complete
    }

    /**
     * Create a reset island at the specified grid location.
     * Used when BentoBox has already deleted the old island before firing IslandPreCreateEvent.
     * @param player The player resetting their island
     * @param coord The grid coordinate to create the island at
     */
    private void createResetIslandAtLocation(Player player, GridCoordinate coord) {
        UUID playerUUID = player.getUniqueId();
        World world = gridManager.getBSkyBlockWorld();
        User user = User.getInstance(player);

        addon.log("Creating reset island for " + player.getName() + " at " + coord + " (old island already deleted)");

        // Calculate world coordinates for this grid position
        int spacing = addon.getIslandSpacing() * 2;
        int worldX = coord.getX() * spacing;
        int worldZ = coord.getZ() * spacing;
        Location targetLocation = new Location(world, worldX, 64, worldZ);

        // Check if there's still an island at this location in BentoBox's cache
        // and forcibly remove it from the cache/grid before creating the new island
        Island existingAtLocation = BentoBox.getInstance().getIslands().getIslandAt(targetLocation).orElse(null);
        if (existingAtLocation != null) {
            String islandId = existingAtLocation.getUniqueId();
            addon.log("Found existing island at location: " + islandId + " - removing from cache");
            try {
                // Directly remove from cache/grid - this clears the grid slot
                BentoBox.getInstance().getIslands().getIslandCache().deleteIslandFromCache(existingAtLocation);
                addon.log("Removed island from cache");
                // Note: We don't delete the file here - it will be deleted after the new island is created
                // to prevent BentoBox from re-saving it
            } catch (Exception e) {
                addon.logWarning("Failed to remove island from cache: " + e.getMessage());
            }
        }

        // Create island at the same grid location using our custom location strategy
        GridLocationStrategy locationStrategy = new GridLocationStrategy(addon, coord, world);

        try {
            // Use CREATE reason instead of RESET - RESET has special handling that may conflict
            // with our custom grid management
            Island newIsland = NewIsland.builder()
                .player(user)
                .addon(addon.getBSkyBlockAddon())
                .reason(IslandEvent.Reason.CREATE)
                .locationStrategy(locationStrategy)
                .build();

            if (newIsland != null) {
                addon.log("Initiated island reset at grid " + coord + " for " + player.getName());
            } else {
                addon.logError("NewIsland.builder().build() returned null for " + player.getName());
                player.sendMessage("§cFailed to create new island. The grid location may still be blocked.");
                pendingClaims.remove(playerUUID);
                pendingResets.remove(playerUUID);
            }

        } catch (Exception e) {
            addon.logError("Failed to create reset island: " + e.getMessage());
            player.sendMessage("§cFailed to create new island. Please contact an admin.");
            pendingClaims.remove(playerUUID);
            pendingResets.remove(playerUUID);
        }
    }

    /**
     * Delete an island's database file by its ID.
     * Handles both full BentoBox island IDs (like "BSkyBlock6d68f389-...") and plain UUIDs.
     */
    private void deleteIslandDatabaseFile(String islandId) {
        if (islandId == null || islandId.isEmpty()) {
            addon.log("deleteIslandDatabaseFile called with null/empty ID");
            return;
        }

        addon.log("Attempting to delete island database file for ID: " + islandId);

        // Try the ID as-is first (for full BentoBox IDs like "BSkyBlockXXX")
        java.io.File islandFile = new java.io.File(
            addon.getPlugin().getDataFolder().getParentFile(),
            "BentoBox/database/Island/" + islandId + ".json"
        );

        addon.log("Looking for file: " + islandFile.getAbsolutePath());

        if (islandFile.exists()) {
            boolean deleted = islandFile.delete();
            if (deleted) {
                addon.log("Deleted island database file " + islandId + ".json");
            } else {
                addon.logWarning("Failed to delete island database file " + islandId + ".json");
            }
        } else {
            addon.log("File not found at primary path");
            // Try with BSkyBlock prefix if the ID looks like a UUID
            if (!islandId.startsWith("BSkyBlock")) {
                java.io.File prefixedFile = new java.io.File(
                    addon.getPlugin().getDataFolder().getParentFile(),
                    "BentoBox/database/Island/BSkyBlock" + islandId + ".json"
                );
                addon.log("Trying prefixed path: " + prefixedFile.getAbsolutePath());
                if (prefixedFile.exists()) {
                    boolean deleted = prefixedFile.delete();
                    if (deleted) {
                        addon.log("Deleted island database file BSkyBlock" + islandId + ".json");
                    } else {
                        addon.logWarning("Failed to delete island database file BSkyBlock" + islandId + ".json");
                    }
                } else {
                    addon.log("Prefixed file also not found");
                }
            }
        }
    }

    /**
     * Handle island deletion - clear from grid and slot data
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandDelete(IslandDeleteEvent event) {
        Island island = event.getIsland();
        if (island == null || island.getCenter() == null) {
            return;
        }

        // Find this island's grid coordinate
        Location center = island.getCenter();
        GridCoordinate coord = gridManager.worldToGrid(center.getBlockX(), center.getBlockZ());

        if (coord != null && gridManager.isWithinBounds(coord)) {
            // Don't clear the location if this is part of a reset - the player is keeping this location
            // Create defensive copy to avoid ConcurrentModificationException
            boolean isReset = new java.util.ArrayList<>(pendingResets.values()).stream().anyMatch(c -> c.equals(coord));
            if (isReset) {
                addon.log("Skipping grid location clear for " + coord + " - reset in progress");
                return;
            }

            gridManager.clearLocation(coord);
            addon.log("Cleared grid location " + coord + " due to island deletion");
        }

        // Clear slot data for the island owner so they can create a fresh island
        // Skip this for admin removals - markPlayerAsHomeless handles it more thoroughly
        UUID ownerUUID = island.getOwner();
        if (ownerUUID != null) {
            // Skip if this is an admin removal - the IslandRemovalManager will handle slot data
            if (pendingAdminRemovals.contains(ownerUUID)) {
                addon.log("Skipping slot data clearing for " + ownerUUID + " - admin removal in progress");
                return;
            }

            SlotData activeSlot = slotManager.getActiveSlot(ownerUUID);
            if (activeSlot != null && activeSlot.hasIsland()) {
                // Check if this slot's island UUID matches the deleted island
                String slotIslandUUID = activeSlot.getIslandUUID();
                String deletedIslandUUID = island.getUniqueId();

                if (deletedIslandUUID.equals(slotIslandUUID) || slotIslandUUID == null) {
                    // Clear the slot data so player can create a new island
                    activeSlot.setHasIsland(false);
                    activeSlot.setIslandUUID((String) null);
                    activeSlot.setGridCoordinate(null);
                    slotManager.saveSlot(activeSlot);
                    addon.log("Cleared slot data for " + ownerUUID + " due to island deletion");
                }
            }
        }
    }

    /**
     * Called when a player confirms a claim with a blueprint selection
     */
    public void confirmClaimWithBlueprint(Player player, GridCoordinate coord, String blueprintBundleKey) {
        UUID playerUUID = player.getUniqueId();

        // Calculate world coordinates
        int worldX = calculateWorldX(coord);
        int worldZ = calculateWorldZ(coord);

        // Fire GridLocationClaimEvent
        GridLocationClaimEvent event = new GridLocationClaimEvent(player, coord, worldX, worldZ);
        Bukkit.getPluginManager().callEvent(event);

        // Check if event was cancelled
        if (event.isCancelled()) {
            addon.log("GridLocationClaimEvent cancelled for " + player.getName() + " at " + coord);
            if (event.getCancellationReason() != null) {
                player.sendMessage("§c" + event.getCancellationReason());
            } else {
                player.sendMessage("§cYou cannot claim this location.");
            }
            // Clean up pending maps to prevent player from being locked into wrong location
            pendingClaims.remove(playerUUID);
            confirmedBlueprints.remove(playerUUID);
            return;
        }

        addon.log("Player " + player.getName() + " confirmed claim at " + coord +
                 " with blueprint " + blueprintBundleKey);

        // Check if multi-dimension mode is enabled AND has custom dimensions
        // If only BSkyBlock-native dimensions (overworld/nether/end), use single-dimension flow
        // because BSkyBlock handles nether/end automatically via blueprint bundles
        DimensionManager dimManager = addon.getDimensionManager();
        MultiDimensionIslandCreator multiDimCreator = addon.getMultiDimensionIslandCreator();

        if (dimManager != null && dimManager.isEnabled() &&
                multiDimCreator != null && multiDimCreator.hasCustomDimensions()) {
            // Use multi-dimension island creator for custom dimensions
            addon.log("Using multi-dimension island creation (has custom dimensions)");
            multiDimCreator.createIslandsForAllDimensions(
                    player, coord, blueprintBundleKey, (createdIslands) -> {
                        addon.log("Multi-dimension island creation completed: " + createdIslands.size() + " islands");
                    });
            return;
        }

        // Single dimension mode OR only BSkyBlock-native dimensions
        // BSkyBlock handles overworld/nether/end automatically via blueprint bundle
        addon.log("Using single-dimension flow (BSkyBlock handles nether/end via blueprint bundle)");
        pendingClaims.put(playerUUID, coord);
        confirmedBlueprints.put(playerUUID, blueprintBundleKey);

        // Create island at the selected grid location using NewIsland.builder()
        createIslandAtLocation(player, coord, blueprintBundleKey);
    }

    /**
     * Called when a player confirms a claim without specifying blueprint
     */
    public void confirmClaim(Player player, GridCoordinate coord) {
        UUID playerUUID = player.getUniqueId();

        // Calculate world coordinates
        int worldX = calculateWorldX(coord);
        int worldZ = calculateWorldZ(coord);

        // Fire GridLocationClaimEvent
        GridLocationClaimEvent event = new GridLocationClaimEvent(player, coord, worldX, worldZ);
        Bukkit.getPluginManager().callEvent(event);

        // Check if event was cancelled
        if (event.isCancelled()) {
            addon.log("GridLocationClaimEvent cancelled for " + player.getName() + " at " + coord);
            if (event.getCancellationReason() != null) {
                player.sendMessage("§c" + event.getCancellationReason());
            } else {
                player.sendMessage("§cYou cannot claim this location.");
            }
            // Clean up pending maps to prevent player from being locked into wrong location
            pendingClaims.remove(playerUUID);
            confirmedBlueprints.remove(playerUUID);
            return;
        }

        addon.log("Player " + player.getName() + " confirmed claim at " + coord);

        // Check if multi-dimension mode is enabled AND has custom dimensions
        // If only BSkyBlock-native dimensions (overworld/nether/end), use single-dimension flow
        // because BSkyBlock handles nether/end automatically via blueprint bundles
        DimensionManager dimManager = addon.getDimensionManager();
        MultiDimensionIslandCreator multiDimCreator = addon.getMultiDimensionIslandCreator();

        if (dimManager != null && dimManager.isEnabled() &&
                multiDimCreator != null && multiDimCreator.hasCustomDimensions()) {
            // Use multi-dimension island creator for custom dimensions
            addon.log("Using multi-dimension island creation (has custom dimensions)");
            multiDimCreator.createIslandsForAllDimensions(
                    player, coord, null, (createdIslands) -> {
                        addon.log("Multi-dimension island creation completed: " + createdIslands.size() + " islands");
                    });
            return;
        }

        // Single dimension mode OR only BSkyBlock-native dimensions
        // BSkyBlock handles overworld/nether/end automatically via blueprint bundle
        addon.log("Using single-dimension flow (BSkyBlock handles nether/end via blueprint bundle)");
        pendingClaims.put(playerUUID, coord);

        // Create island at the selected grid location using NewIsland.builder()
        createIslandAtLocation(player, coord, null);
    }

    /**
     * Create an island at the specified grid location using BentoBox's NewIsland API.
     * This ensures the island is created at the correct position from the start.
     */
    private void createIslandAtLocation(Player player, GridCoordinate coord, String blueprintBundleKey) {
        World world = gridManager.getBSkyBlockWorld();
        if (world == null) {
            player.sendMessage("§cError: BSkyBlock world not available.");
            pendingClaims.remove(player.getUniqueId());
            confirmedBlueprints.remove(player.getUniqueId());
            return;
        }

        User user = User.getInstance(player);

        // Create our custom location strategy that returns the grid coordinate
        GridLocationStrategy locationStrategy = new GridLocationStrategy(addon, coord, world);

        try {
            // Build the island creation request with our custom location strategy
            NewIsland.Builder builder = NewIsland.builder()
                    .player(user)
                    .addon(addon.getBSkyBlockAddon())
                    .reason(IslandEvent.Reason.CREATE)
                    .locationStrategy(locationStrategy);

            // Add blueprint if specified
            if (blueprintBundleKey != null && !blueprintBundleKey.isEmpty()) {
                builder.name(blueprintBundleKey);
            }

            // Execute the island creation
            builder.build();

            addon.log("Initiated island creation at grid " + coord + " for " + player.getName());

        } catch (Exception e) {
            addon.logError("Failed to create island: " + e.getMessage());
            player.sendMessage("§cFailed to create island. Please try again.");
            pendingClaims.remove(player.getUniqueId());
            confirmedBlueprints.remove(player.getUniqueId());
        }
    }

    /**
     * Called when a player selects a location (but hasn't confirmed yet)
     * This is called when they click once on a location
     */
    public void onLocationSelected(Player player, GridCoordinate coord) {
        addon.log("Player " + player.getName() + " selected location " + coord);
        // This doesn't add to pendingClaims - that happens on confirm
    }

    /**
     * Called when a player cancels their claim
     */
    public void cancelClaim(Player player) {
        UUID playerUUID = player.getUniqueId();
        pendingClaims.remove(playerUUID);
        confirmedBlueprints.remove(playerUUID);

        addon.log("Player " + player.getName() + " cancelled their claim");
    }

    /**
     * Check if a player has a pending claim
     */
    public boolean hasPendingClaim(UUID playerUUID) {
        return pendingClaims.containsKey(playerUUID);
    }

    /**
     * Get a player's pending claim coordinate
     */
    public GridCoordinate getPendingClaim(UUID playerUUID) {
        return pendingClaims.get(playerUUID);
    }

    /**
     * Mark a player as having an admin removal in progress.
     * This prevents onIslandDelete from clearing slot data (markPlayerAsHomeless handles it).
     */
    public void markAdminRemoval(UUID playerUUID) {
        pendingAdminRemovals.add(playerUUID);
    }

    /**
     * Unmark a player's admin removal (called after removal completes).
     */
    public void unmarkAdminRemoval(UUID playerUUID) {
        pendingAdminRemovals.remove(playerUUID);
    }

    /**
     * Check if a player has an admin removal in progress.
     */
    public boolean hasAdminRemoval(UUID playerUUID) {
        return pendingAdminRemovals.contains(playerUUID);
    }

    /**
     * Clean up all pending data for a player.
     * Called on disconnect to prevent memory leaks.
     */
    public void cleanupPlayer(UUID playerUUID) {
        pendingClaims.remove(playerUUID);
        confirmedBlueprints.remove(playerUUID);
        pendingResets.remove(playerUUID);
        pendingOldIslandDeletions.remove(playerUUID);
        pendingAdminRemovals.remove(playerUUID);
    }

    /**
     * Handle player disconnect - clean up any pending claim data.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        cleanupPlayer(playerUUID);
    }

    /**
     * Ensure nether and end blueprints are pasted for the island.
     * This is needed because NewIsland.builder() with custom locationStrategy
     * may not properly trigger nether/end blueprint pasting.
     *
     * @param island The created island
     * @param blueprintBundleKey The blueprint bundle key to use
     */
    private void ensureNetherEndBlueprints(Island island, String blueprintBundleKey) {
        GameModeAddon bskyblock = addon.getBSkyBlockAddon();
        if (bskyblock == null) {
            addon.log("BSkyBlock not available, skipping nether/end paste");
            return;
        }

        WorldSettings settings = bskyblock.getWorldSettings();
        BlueprintsManager blueprintsManager = addon.getPlugin().getBlueprintsManager();
        BlueprintBundle bundle = blueprintsManager.getBlueprintBundles(bskyblock).get(blueprintBundleKey);

        if (bundle == null) {
            addon.log("Blueprint bundle '" + blueprintBundleKey + "' not found, skipping nether/end paste");
            return;
        }

        // Check if there are nether/end blueprints to paste
        boolean hasNetherBlueprint = settings.isNetherGenerate() && settings.isNetherIslands()
                && bskyblock.getNetherWorld() != null
                && bundle.getBlueprint(World.Environment.NETHER) != null;

        boolean hasEndBlueprint = settings.isEndGenerate() && settings.isEndIslands()
                && bskyblock.getEndWorld() != null
                && bundle.getBlueprint(World.Environment.THE_END) != null;

        if (!hasNetherBlueprint && !hasEndBlueprint) {
            addon.log("No nether/end blueprints to paste for bundle '" + blueprintBundleKey + "'");
            return;
        }

        addon.log("Scheduling nether/end blueprint paste for island at " + island.getCenter()
                + " (nether=" + hasNetherBlueprint + ", end=" + hasEndBlueprint + ")");

        // Schedule the paste after a short delay to ensure overworld is fully created
        // Using BlueprintsManager.paste() which handles all dimensions correctly
        // Parameters: addon, island, bundleKey, callback, useDefaultChest
        Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
            blueprintsManager.paste(bskyblock, island, blueprintBundleKey, () -> {
                addon.log("Nether/end blueprint paste complete for island at " + island.getCenter());
            }, false); // false = don't use default chest (keep existing items)
        }, 40L); // Wait 2 seconds for overworld creation to complete
    }
}
