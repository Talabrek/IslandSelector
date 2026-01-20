package world.bentobox.islandselector.managers;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.island.NewIsland;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.models.DimensionConfig;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Orchestrates island creation across multiple dimensions.
 * Creates islands sequentially in each enabled dimension at the same grid coordinate.
 */
public class MultiDimensionIslandCreator {

    private final IslandSelector addon;

    // Track pending multi-dimension creations
    private final Map<UUID, CreationContext> pendingCreations = new HashMap<>();

    public MultiDimensionIslandCreator(IslandSelector addon) {
        this.addon = addon;
    }

    /**
     * Context for a multi-dimension island creation operation
     */
    public static class CreationContext {
        private final GridCoordinate coord;
        private final String blueprintBundleKey;
        private final Queue<DimensionConfig> pendingDimensions;
        private final Map<String, UUID> createdIslands;
        private final Map<String, String> createdIslandIds;
        private final Consumer<Map<String, UUID>> onComplete;
        private String currentDimension;
        private boolean isReset;

        public CreationContext(GridCoordinate coord, String blueprintBundleKey,
                               List<DimensionConfig> dimensions, Consumer<Map<String, UUID>> onComplete) {
            this.coord = coord;
            this.blueprintBundleKey = blueprintBundleKey;
            this.pendingDimensions = new LinkedList<>(dimensions);
            this.createdIslands = new HashMap<>();
            this.createdIslandIds = new HashMap<>();
            this.onComplete = onComplete;
            this.isReset = false;
        }

        public GridCoordinate getCoord() {
            return coord;
        }

        public String getBlueprintBundleKey() {
            return blueprintBundleKey;
        }

        public String getCurrentDimension() {
            return currentDimension;
        }

        public void setCurrentDimension(String currentDimension) {
            this.currentDimension = currentDimension;
        }

        public Map<String, UUID> getCreatedIslands() {
            return createdIslands;
        }

        public Map<String, String> getCreatedIslandIds() {
            return createdIslandIds;
        }

        public boolean isReset() {
            return isReset;
        }

        public void setReset(boolean reset) {
            isReset = reset;
        }

        public void addCreatedIsland(String dimensionKey, UUID islandUUID, String islandId) {
            createdIslands.put(dimensionKey, islandUUID);
            createdIslandIds.put(dimensionKey, islandId);
        }

        public DimensionConfig pollNextDimension() {
            return pendingDimensions.poll();
        }

        public boolean hasMoreDimensions() {
            return !pendingDimensions.isEmpty();
        }

        public void complete() {
            if (onComplete != null) {
                onComplete.accept(createdIslands);
            }
        }
    }

    /**
     * Check if a player has a pending multi-dimension creation
     */
    public boolean hasPendingCreation(UUID playerUUID) {
        return pendingCreations.containsKey(playerUUID);
    }

    /**
     * Check if there are any custom (non-BSkyBlock-native) dimensions configured.
     * Custom dimensions are worlds beyond BSkyBlock's overworld/nether/end that
     * require manual island creation.
     *
     * @return true if there are custom dimensions that need manual island creation
     */
    public boolean hasCustomDimensions() {
        DimensionManager dimManager = addon.getDimensionManager();
        if (dimManager == null || !dimManager.isEnabled()) {
            return false;
        }

        List<DimensionConfig> allDimensions = dimManager.getDimensionsForCreation();
        for (DimensionConfig dim : allDimensions) {
            if (!isBSkyBlockNativeDimension(dim)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the creation context for a player
     */
    public CreationContext getCreationContext(UUID playerUUID) {
        return pendingCreations.get(playerUUID);
    }

    /**
     * Start creating islands in all enabled dimensions for a player.
     *
     * @param player The player creating islands
     * @param coord The grid coordinate for all islands
     * @param blueprintBundleKey The blueprint to use (dimension-specific blueprints can override)
     * @param onComplete Callback when all dimensions are complete
     */
    public void createIslandsForAllDimensions(Player player, GridCoordinate coord,
                                               String blueprintBundleKey, Consumer<Map<String, UUID>> onComplete) {
        createIslandsForAllDimensions(player, coord, blueprintBundleKey, false, onComplete);
    }

    /**
     * Start creating islands in all enabled dimensions for a player.
     *
     * @param player The player creating islands
     * @param coord The grid coordinate for all islands
     * @param blueprintBundleKey The blueprint to use (dimension-specific blueprints can override)
     * @param isReset Whether this is a reset operation
     * @param onComplete Callback when all dimensions are complete
     */
    public void createIslandsForAllDimensions(Player player, GridCoordinate coord,
                                               String blueprintBundleKey, boolean isReset,
                                               Consumer<Map<String, UUID>> onComplete) {
        UUID playerUUID = player.getUniqueId();
        DimensionManager dimManager = addon.getDimensionManager();

        if (dimManager == null || !dimManager.isEnabled()) {
            addon.logWarning("MultiDimensionIslandCreator called but DimensionManager is not enabled");
            if (onComplete != null) {
                onComplete.accept(new HashMap<>());
            }
            return;
        }

        // Get dimensions that should be created on claim
        List<DimensionConfig> allDimensions = dimManager.getDimensionsForCreation();
        if (allDimensions.isEmpty()) {
            addon.logWarning("No dimensions configured for creation on claim");
            if (onComplete != null) {
                onComplete.accept(new HashMap<>());
            }
            return;
        }

        // Filter out BSkyBlock-native dimensions (overworld, nether, end)
        // BSkyBlock handles these automatically via blueprint bundles
        List<DimensionConfig> customDimensions = allDimensions.stream()
                .filter(dim -> !isBSkyBlockNativeDimension(dim))
                .collect(Collectors.toList());

        addon.log("Multi-dimension check: " + allDimensions.size() + " total dimensions, " +
                customDimensions.size() + " custom dimensions (non-BSkyBlock)");

        // If no custom dimensions, don't use multi-dimension creator
        // BSkyBlock will handle nether/end via the blueprint bundle automatically
        if (customDimensions.isEmpty()) {
            addon.log("No custom dimensions - BSkyBlock will handle all dimensions via blueprint bundle");
            if (onComplete != null) {
                onComplete.accept(new HashMap<>());
            }
            return;
        }

        addon.log("Starting multi-dimension island creation for " + player.getName() +
                " at " + coord + " across " + customDimensions.size() + " custom dimensions");

        // Create the context with only custom dimensions
        CreationContext context = new CreationContext(coord, blueprintBundleKey, customDimensions, onComplete);
        context.setReset(isReset);
        pendingCreations.put(playerUUID, context);

        // Start with the first custom dimension
        createNextDimension(player);
    }

    /**
     * Create the island in the next pending dimension
     */
    private void createNextDimension(Player player) {
        UUID playerUUID = player.getUniqueId();
        CreationContext context = pendingCreations.get(playerUUID);

        if (context == null) {
            addon.logError("No creation context found for " + player.getName());
            return;
        }

        DimensionConfig nextDim = context.pollNextDimension();
        if (nextDim == null) {
            // All dimensions complete
            addon.log("All dimensions created for " + player.getName());
            finishCreation(player);
            return;
        }

        context.setCurrentDimension(nextDim.getDimensionKey());

        DimensionManager dimManager = addon.getDimensionManager();
        World world = dimManager.getWorld(nextDim.getDimensionKey());

        if (world == null) {
            addon.logWarning("World not found for dimension " + nextDim.getDimensionKey() + ", skipping");
            // Try the next dimension
            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> createNextDimension(player));
            return;
        }

        // Determine blueprint - use dimension-specific if it exists, otherwise fallback to selected
        String blueprint = context.getBlueprintBundleKey(); // Default to user-selected blueprint
        String dimBlueprint = nextDim.getDefaultBlueprint();

        // Only use dimension-specific blueprint if it exists in BSkyBlock
        if (dimBlueprint != null && !dimBlueprint.isEmpty() && !dimBlueprint.equals("default")) {
            var blueprintsManager = addon.getPlugin().getBlueprintsManager();
            var bskyblock = addon.getBSkyBlockAddon();
            if (bskyblock != null && blueprintsManager.getBlueprintBundles(bskyblock).containsKey(dimBlueprint)) {
                blueprint = dimBlueprint;
            }
        }

        addon.log("Creating island in dimension " + nextDim.getDimensionKey() +
                " (world: " + world.getName() + ") with blueprint: " + blueprint);

        // Create the island using our grid location strategy
        createIslandInDimension(player, context.getCoord(), world, blueprint, context.isReset());
    }

    /**
     * Create an island in a specific dimension
     */
    private void createIslandInDimension(Player player, GridCoordinate coord, World world,
                                          String blueprintBundleKey, boolean isReset) {
        User user = User.getInstance(player);

        // Create our custom location strategy for this dimension's world
        GridLocationStrategy locationStrategy = new GridLocationStrategy(addon, coord, world);

        try {
            NewIsland.Builder builder = NewIsland.builder()
                    .player(user)
                    .addon(addon.getBSkyBlockAddon())
                    .reason(isReset ? IslandEvent.Reason.RESET : IslandEvent.Reason.CREATE)
                    .locationStrategy(locationStrategy);

            // Add blueprint if specified
            if (blueprintBundleKey != null && !blueprintBundleKey.isEmpty()) {
                builder.name(blueprintBundleKey);
            }

            // Execute the island creation
            builder.build();

            addon.log("Initiated island creation in " + world.getName() + " at grid " + coord);

        } catch (Exception e) {
            addon.logError("Failed to create island in " + world.getName() + ": " + e.getMessage());
            e.printStackTrace();
            // Continue to next dimension
            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> createNextDimension(player));
        }
    }

    /**
     * Called when an island is created - register it to the correct dimension.
     * Returns true if this was part of a multi-dimension creation.
     *
     * @param playerUUID The player's UUID
     * @param island The created island
     * @param world The world the island was created in
     * @return true if this was a multi-dimension creation
     */
    public boolean onIslandCreated(UUID playerUUID, Island island, World world) {
        CreationContext context = pendingCreations.get(playerUUID);
        if (context == null) {
            return false;
        }

        String dimensionKey = context.getCurrentDimension();
        if (dimensionKey == null) {
            addon.logWarning("Island created but no current dimension tracked for " + playerUUID);
            return true;
        }

        // Parse island UUID
        String islandIdStr = island.getUniqueId();
        UUID islandUUID = parseIslandUUID(islandIdStr);

        context.addCreatedIsland(dimensionKey, islandUUID, islandIdStr);
        addon.log("Registered island in dimension " + dimensionKey + ": " + islandIdStr);

        // Create next dimension after a short delay
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
                createNextDimension(player);
            }, 10L); // Small delay between creations
        } else {
            // Player offline, abort remaining creations
            addon.logWarning("Player went offline during multi-dimension creation");
            finishCreation(null, playerUUID);
        }

        return true;
    }

    /**
     * Finish the multi-dimension creation process
     */
    private void finishCreation(Player player) {
        if (player == null) {
            return;
        }
        finishCreation(player, player.getUniqueId());
    }

    /**
     * Finish the multi-dimension creation process
     */
    private void finishCreation(Player player, UUID playerUUID) {
        CreationContext context = pendingCreations.remove(playerUUID);
        if (context == null) {
            return;
        }

        Map<String, UUID> createdIslands = context.getCreatedIslands();
        Map<String, String> createdIslandIds = context.getCreatedIslandIds();

        // Update GridManager with all dimension islands
        GridManager gridManager = addon.getGridManager();
        if (gridManager == null) {
            addon.logError("GridManager not available during island creation finish");
            context.complete();
            return;
        }
        String ownerName = player != null ? player.getName() : "Unknown";

        gridManager.occupyLocation(context.getCoord(), playerUUID, ownerName, createdIslands);
        addon.log("Registered all dimension islands at grid " + context.getCoord());

        // Update SlotManager with dimension islands
        SlotManager slotManager = addon.getSlotManager();
        if (slotManager == null) {
            addon.logError("SlotManager not available during island creation finish");
            context.complete();
            return;
        }

        if (context.isReset()) {
            // For resets, update the existing slot
            SlotData activeSlot = slotManager.getActiveSlot(playerUUID);
            if (activeSlot != null) {
                for (Map.Entry<String, String> entry : createdIslandIds.entrySet()) {
                    activeSlot.setIslandUUID(entry.getKey(), entry.getValue());
                }
                slotManager.saveSlot(activeSlot);
            }
        } else {
            // For new creations, initialize slots with all dimension islands
            slotManager.initializePlayerSlotsMultiDimension(playerUUID, createdIslandIds,
                    context.getCoord().toString());

            // Store blueprint bundle for challenges integration
            String blueprint = context.getBlueprintBundleKey();
            if (blueprint != null && !blueprint.isEmpty()) {
                slotManager.setBlueprintBundle(playerUUID, 1, blueprint);
                if (player != null) {
                    BlueprintChallengesManager bcm = addon.getBlueprintChallengesManager();
                    if (bcm != null) {
                        bcm.updateBlueprintPermissions(player, blueprint);
                    }
                }
            }
        }

        // Notify completion
        context.complete();

        // Send message to player
        if (player != null) {
            int dimensionCount = createdIslands.size();
            if (context.isReset()) {
                player.sendMessage("§a§lIsland Reset Complete!");
                player.sendMessage("§7Your islands have been reset across " + dimensionCount + " dimension(s)");
            } else {
                player.sendMessage("§a§lIslands Created!");
                player.sendMessage("§7Your islands have been created at location §f" + context.getCoord().toString());
                player.sendMessage("§7Active dimensions: §f" + dimensionCount);
            }
        }
    }

    /**
     * Cancel a pending multi-dimension creation
     */
    public void cancelCreation(UUID playerUUID) {
        CreationContext context = pendingCreations.remove(playerUUID);
        if (context != null) {
            addon.log("Cancelled multi-dimension creation for " + playerUUID);
        }
    }

    /**
     * Check if a world is one of BSkyBlock's native worlds (overworld, nether, end).
     * BSkyBlock handles these automatically via blueprint bundles - we should not
     * manually create separate islands in these worlds.
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
     * Check if a dimension config refers to a BSkyBlock native world
     *
     * @param dimConfig The dimension config
     * @return true if this dimension is BSkyBlock-managed
     */
    private boolean isBSkyBlockNativeDimension(DimensionConfig dimConfig) {
        DimensionManager dimManager = addon.getDimensionManager();
        if (dimManager == null) {
            return false;
        }
        World world = dimManager.getWorld(dimConfig.getDimensionKey());
        return isBSkyBlockNativeWorld(world);
    }

    /**
     * Parse an island ID string to a UUID
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
