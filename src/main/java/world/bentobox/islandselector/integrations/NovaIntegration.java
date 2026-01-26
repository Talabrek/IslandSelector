package world.bentobox.islandselector.integrations;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import world.bentobox.islandselector.IslandSelector;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Integration with Nova plugin for custom block support.
 *
 * Nova stores custom block data separately from Minecraft's NBT system,
 * so WorldEdit cannot copy Nova blocks. This integration captures and
 * restores Nova blocks during island operations.
 *
 * Uses reflection to avoid hard dependency on Nova - if Nova isn't installed,
 * operations gracefully skip Nova block handling.
 */
public class NovaIntegration {

    private final IslandSelector addon;
    private final boolean available;
    private final ReflectionCache cache;
    private Class<?> novaClass;
    private Class<?> blockManagerClass;
    private Class<?> novaBlockStateClass;
    private Object blockManager;

    /**
     * Caches all reflection lookups for Nova API access.
     * Initialized once during NovaIntegration construction.
     * All fields are final for thread-safety.
     */
    private static class ReflectionCache {
        // Classes
        final Class<?> worldDataManagerClass;
        final Class<?> tileEntityClass;
        final Class<?> blockUtilsClass;
        final Class<?> contextClass;
        final Class<?> keyClass;
        final Class<?> novaRegistriesClass;
        final Class<?> novaBlockClass;
        final Class<?> novaBlockStateClass;

        // Singleton instances
        final Object worldDataManagerInstance;
        final Object blockRegistry;
        final Object emptyContext;

        // Methods
        final java.lang.reflect.Method getBlockStateMethod;
        final java.lang.reflect.Method getTileEntityMethod;
        final java.lang.reflect.Method getDropsMethod;
        final java.lang.reflect.Method getIdMethod;
        final java.lang.reflect.Method breakBlockMethod;
        final java.lang.reflect.Method placeBlockMethod;
        final java.lang.reflect.Method keyMethod;
        final java.lang.reflect.Method registryGetMethod;

        ReflectionCache() throws ReflectiveOperationException {
            // Load all classes
            this.worldDataManagerClass = Class.forName("xyz.xenondevs.nova.world.format.WorldDataManager");
            this.tileEntityClass = Class.forName("xyz.xenondevs.nova.world.block.tileentity.TileEntity");
            this.blockUtilsClass = Class.forName("xyz.xenondevs.nova.util.BlockUtils");
            this.contextClass = Class.forName("xyz.xenondevs.nova.context.Context");
            this.keyClass = Class.forName("net.kyori.adventure.key.Key");
            this.novaRegistriesClass = Class.forName("xyz.xenondevs.nova.registry.NovaRegistries");
            this.novaBlockClass = Class.forName("xyz.xenondevs.nova.world.block.NovaBlock");
            this.novaBlockStateClass = Class.forName("xyz.xenondevs.nova.world.block.state.NovaBlockState");

            // Get singleton instances
            this.worldDataManagerInstance = worldDataManagerClass.getField("INSTANCE").get(null);
            if (worldDataManagerInstance == null) {
                throw new IllegalStateException("Nova WorldDataManager INSTANCE is null");
            }

            this.blockRegistry = novaRegistriesClass.getField("BLOCK").get(null);
            if (blockRegistry == null) {
                throw new IllegalStateException("Nova block registry is null");
            }

            // Get Context.EMPTY (try field first, then method)
            Object tempContext;
            try {
                tempContext = contextClass.getDeclaredField("EMPTY").get(null);
            } catch (NoSuchFieldException e) {
                tempContext = contextClass.getMethod("empty").invoke(null);
            }
            if (tempContext == null) {
                throw new IllegalStateException("Cannot create Nova empty context");
            }
            this.emptyContext = tempContext;

            // Cache all methods
            this.getBlockStateMethod = worldDataManagerClass.getMethod("getBlockState", Location.class);
            this.getTileEntityMethod = worldDataManagerClass.getMethod("getTileEntity", Location.class);
            this.getDropsMethod = tileEntityClass.getMethod("getDrops", boolean.class);
            this.getIdMethod = novaBlockStateClass.getMethod("getId");
            this.breakBlockMethod = blockUtilsClass.getMethod("breakBlock", contextClass, Location.class, boolean.class);
            this.placeBlockMethod = blockUtilsClass.getMethod("placeBlock", contextClass, Location.class, novaBlockClass, boolean.class);
            this.keyMethod = keyClass.getMethod("key", String.class);
            this.registryGetMethod = blockRegistry.getClass().getMethod("get", keyClass);
        }
    }

    public NovaIntegration(IslandSelector addon) {
        this.addon = addon;
        this.available = detectNova();

        ReflectionCache tempCache = null;
        if (available) {
            try {
                tempCache = new ReflectionCache();
            } catch (Exception e) {
                addon.logWarning("Failed to initialize Nova reflection cache: " + e.getMessage());
            }
        }
        this.cache = tempCache;

        if (available && cache != null) {
            addon.log("Nova integration enabled - custom block support active");
        } else if (available && cache == null) {
            addon.logWarning("Nova detected but reflection cache failed - integration disabled");
        }
    }

    /**
     * Check if Nova plugin is available
     */
    private boolean detectNova() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Nova")) {
            return false;
        }

        try {
            // Try to load Nova classes via reflection
            novaClass = Class.forName("xyz.xenondevs.nova.Nova");

            // Try different API versions
            try {
                // Nova 0.17+ API
                blockManagerClass = Class.forName("xyz.xenondevs.nova.world.block.BlockManager");
            } catch (ClassNotFoundException e) {
                // Try older API location
                try {
                    blockManagerClass = Class.forName("xyz.xenondevs.nova.data.world.block.BlockManager");
                } catch (ClassNotFoundException e2) {
                    addon.logWarning("Nova detected but BlockManager class not found - custom blocks may not copy correctly");
                    return false;
                }
            }

            try {
                novaBlockStateClass = Class.forName("xyz.xenondevs.nova.world.block.state.NovaBlockState");
            } catch (ClassNotFoundException e) {
                try {
                    novaBlockStateClass = Class.forName("xyz.xenondevs.nova.data.world.block.state.NovaBlockState");
                } catch (ClassNotFoundException e2) {
                    addon.logWarning("Nova detected but NovaBlockState class not found");
                    return false;
                }
            }

            // Debug logging for API version detection
            if (addon.getSettings().isDebugEnabled()) {
                addon.log("Nova API detected - using " +
                    (blockManagerClass.getName().contains("world.block") ? "0.17+" : "pre-0.17") + " paths");
            }

            return true;
        } catch (Exception e) {
            addon.logWarning("Failed to initialize Nova integration: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if Nova integration is available
     */
    public boolean isAvailable() {
        return available && cache != null;
    }

    /**
     * Capture all Nova blocks in a region.
     * Returns a list of NovaBlockData that can be serialized.
     *
     * @param center Center of the region
     * @param range Range from center
     * @return List of captured Nova block data
     */
    public List<NovaBlockData> captureNovaBlocks(Location center, int range) {
        List<NovaBlockData> novaBlocks = new ArrayList<>();

        if (!available) {
            return novaBlocks;
        }

        World world = center.getWorld();
        if (world == null) {
            return novaBlocks;
        }

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        try {
            // Use Nova's WorldDataManager to check blocks
            Class<?> worldDataManagerClass = Class.forName("xyz.xenondevs.nova.world.format.WorldDataManager");
            Object worldDataManager = worldDataManagerClass.getField("INSTANCE").get(null);

            if (worldDataManager == null) {
                addon.logWarning("Nova WorldDataManager INSTANCE is null - cannot capture Nova blocks");
                return novaBlocks;
            }

            // Get the getBlockState method
            java.lang.reflect.Method getBlockStateMethod = worldDataManagerClass.getMethod(
                "getBlockState", Location.class
            );

            // Get getTileEntity method for drop capture
            java.lang.reflect.Method getTileEntityMethod = worldDataManagerClass.getMethod(
                "getTileEntity", Location.class
            );

            // Get TileEntity class for getDrops method
            Class<?> tileEntityClass = Class.forName("xyz.xenondevs.nova.world.block.tileentity.TileEntity");
            java.lang.reflect.Method getDropsMethod = tileEntityClass.getMethod("getDrops", boolean.class);

            int blocksWithTileData = 0;

            for (int x = centerX - range; x <= centerX + range; x++) {
                for (int z = centerZ - range; z <= centerZ + range; z++) {
                    for (int y = minY; y < maxY; y++) {
                        Location loc = new Location(world, x, y, z);

                        try {
                            // Check if this location has a Nova block
                            Object blockState = getBlockStateMethod.invoke(worldDataManager, loc);

                            if (blockState != null) {
                                // Get the block ID
                                java.lang.reflect.Method getIdMethod = blockState.getClass().getMethod("getId");
                                Object blockId = getIdMethod.invoke(blockState);

                                // Capture TileEntity drops if this is a tile entity block
                                List<ItemStack> drops = null;
                                try {
                                    Object tileEntity = getTileEntityMethod.invoke(worldDataManager, loc);
                                    if (tileEntity != null) {
                                        @SuppressWarnings("unchecked")
                                        List<ItemStack> capturedDrops = (List<ItemStack>) getDropsMethod.invoke(tileEntity, true);
                                        drops = capturedDrops;
                                        blocksWithTileData++;
                                    }
                                } catch (Exception te) {
                                    // Not a tile entity or getDrops failed - drops stays null
                                }

                                // Store relative position, block ID, and drops
                                NovaBlockData data = new NovaBlockData(
                                    x - centerX, y - centerY, z - centerZ,
                                    blockId.toString(),
                                    drops
                                );
                                novaBlocks.add(data);
                            }
                        } catch (Exception e) {
                            // Skip blocks that fail - they may not be Nova blocks
                        }
                    }
                }
            }

            addon.log("Captured " + novaBlocks.size() + " Nova blocks (" + blocksWithTileData + " with tile entity data)");

        } catch (Exception e) {
            addon.logWarning("Failed to capture Nova blocks: " + e.getMessage());
        }

        return novaBlocks;
    }

    /**
     * Capture Nova blocks asynchronously (loads chunks first)
     */
    public void captureNovaBlocksAsync(Location center, int range, Consumer<List<NovaBlockData>> callback) {
        if (!available) {
            callback.accept(new ArrayList<>());
            return;
        }

        World world = center.getWorld();
        if (world == null) {
            callback.accept(new ArrayList<>());
            return;
        }

        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();

        // Calculate chunks to load
        int minChunkX = (centerX - range) >> 4;
        int maxChunkX = (centerX + range) >> 4;
        int minChunkZ = (centerZ - range) >> 4;
        int maxChunkZ = (centerZ + range) >> 4;

        List<CompletableFuture<org.bukkit.Chunk>> chunkFutures = new ArrayList<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                chunkFutures.add(world.getChunkAtAsync(cx, cz));
            }
        }

        CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]))
            .orTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .thenRun(() -> {
                // Chunks loaded, now capture on main thread
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    List<NovaBlockData> blocks = captureNovaBlocks(center, range);
                    callback.accept(blocks);
                });
            })
            .exceptionally(throwable -> {
                // Timeout or error occurred - invoke callback with empty list to prevent hang
                addon.logWarning("Nova block capture timed out or failed: " + throwable.getMessage());
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    callback.accept(new ArrayList<>());
                });
                return null;
            });
    }

    /**
     * Restore Nova blocks at a new location
     *
     * @param novaBlocks List of Nova block data to restore
     * @param center Center location to paste at
     * @return RestoreResult with machine restoration counts
     */
    public RestoreResult restoreNovaBlocks(List<NovaBlockData> novaBlocks, Location center) {
        if (!available || novaBlocks == null || novaBlocks.isEmpty()) {
            return new RestoreResult(0, 0);
        }

        World world = center.getWorld();
        if (world == null) {
            return new RestoreResult(0, 0);
        }

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        int restored = 0;

        try {
            // Get BlockUtils class for placing blocks
            Class<?> blockUtilsClass = Class.forName("xyz.xenondevs.nova.util.BlockUtils");

            // Get the block registry to look up blocks by ID
            Class<?> novaRegistriesClass = Class.forName("xyz.xenondevs.nova.registry.NovaRegistries");
            Object blockRegistry = novaRegistriesClass.getField("BLOCK").get(null);

            if (blockRegistry == null) {
                addon.logWarning("Nova block registry is null - cannot restore blocks");
                return new RestoreResult(0, 0);
            }

            java.lang.reflect.Method getMethod = blockRegistry.getClass().getMethod("get",
                Class.forName("net.kyori.adventure.key.Key"));

            // Try to get a default Context from Nova
            Object novaContext = null;
            try {
                Class<?> contextClass = Class.forName("xyz.xenondevs.nova.context.Context");
                // Try to get the EMPTY or DEFAULT context
                try {
                    java.lang.reflect.Field emptyField = contextClass.getDeclaredField("EMPTY");
                    novaContext = emptyField.get(null);
                } catch (NoSuchFieldException e1) {
                    // Try alternative approaches
                    try {
                        java.lang.reflect.Method emptyMethod = contextClass.getMethod("empty");
                        novaContext = emptyMethod.invoke(null);
                    } catch (NoSuchMethodException e2) {
                        // Context not available - will skip block placement
                    }
                }
            } catch (ClassNotFoundException e) {
                addon.logWarning("Nova Context class not found - cannot restore blocks");
                return new RestoreResult(0, 0);
            }

            if (novaContext == null) {
                addon.logWarning("Cannot create Nova context - skipping block restoration");
                return new RestoreResult(0, 0);
            }

            final Object finalContext = novaContext;

            for (NovaBlockData data : novaBlocks) {
                try {
                    int x = centerX + data.relX;
                    int y = centerY + data.relY;
                    int z = centerZ + data.relZ;
                    Location loc = new Location(world, x, y, z);

                    // Parse the block ID (format: namespace:path)
                    Class<?> keyClass = Class.forName("net.kyori.adventure.key.Key");
                    java.lang.reflect.Method keyMethod = keyClass.getMethod("key", String.class);
                    Object key = keyMethod.invoke(null, data.blockId);

                    // Get the NovaBlock from registry
                    Object novaBlock = getMethod.invoke(blockRegistry, key);

                    if (novaBlock != null) {
                        // Place the block using BlockUtils with proper context
                        java.lang.reflect.Method placeBlockMethod = blockUtilsClass.getMethod(
                            "placeBlock",
                            Class.forName("xyz.xenondevs.nova.context.Context"),
                            Location.class,
                            Class.forName("xyz.xenondevs.nova.world.block.NovaBlock"),
                            boolean.class
                        );

                        placeBlockMethod.invoke(null, finalContext, loc, novaBlock, false);
                        restored++;
                    }
                } catch (Exception e) {
                    // Skip blocks that fail to restore
                }
            }

            // Count machine restorations (blocks with TileEntity data)
            int machinesRestored = 0;
            int machinesFailed = 0;

            try {
                // Get WorldDataManager to verify TileEntity restoration
                Class<?> worldDataManagerClass = Class.forName("xyz.xenondevs.nova.world.format.WorldDataManager");
                Object worldDataManager = worldDataManagerClass.getField("INSTANCE").get(null);
                java.lang.reflect.Method getTileEntityMethod = worldDataManagerClass.getMethod("getTileEntity", Location.class);

                // Check each block that had TileEntity data (drops captured)
                for (NovaBlockData data : novaBlocks) {
                    if (data.drops != null && !data.drops.isEmpty()) {
                        try {
                            int x = centerX + data.relX;
                            int y = centerY + data.relY;
                            int z = centerZ + data.relZ;
                            Location loc = new Location(world, x, y, z);

                            Object tileEntity = getTileEntityMethod.invoke(worldDataManager, loc);
                            if (tileEntity != null) {
                                machinesRestored++;
                            } else {
                                machinesFailed++;
                            }
                        } catch (Exception e) {
                            // Failed to verify - count as failure
                            machinesFailed++;
                        }
                    }
                }
            } catch (Exception e) {
                // Reflection setup failed - cannot count machines
                addon.logWarning("Failed to verify machine restoration: " + e.getMessage());
            }

            addon.log("Restored " + restored + "/" + novaBlocks.size() + " Nova blocks");
            if (machinesRestored > 0 || machinesFailed > 0) {
                addon.log("Nova machines: " + machinesRestored + " restored, " + machinesFailed + " failed");
            }

            return new RestoreResult(machinesRestored, machinesFailed);

        } catch (NoSuchFieldException e) {
            addon.logWarning("Nova API changed - field not found: " + e.getMessage());
            return new RestoreResult(0, 0);
        } catch (Exception e) {
            addon.logWarning("Failed to restore Nova blocks: " + e.getMessage());
            e.printStackTrace();
            return new RestoreResult(0, 0);
        }
    }

    /**
     * Restore Nova blocks asynchronously
     */
    public void restoreNovaBlocksAsync(List<NovaBlockData> novaBlocks, Location center, Consumer<RestoreResult> callback) {
        if (!available || novaBlocks == null || novaBlocks.isEmpty()) {
            callback.accept(new RestoreResult(0, 0));
            return;
        }

        // Nova block placement must happen on main thread
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            RestoreResult result = restoreNovaBlocks(novaBlocks, center);
            callback.accept(result);
        });
    }

    /**
     * Remove Nova blocks before WorldEdit operations.
     * Must be called AFTER captureNovaBlocks and BEFORE WorldEdit paste.
     * Uses BlockUtils.breakBlock() for proper Nova lifecycle cleanup.
     *
     * @param novaBlocks List of Nova blocks captured earlier
     * @param center Center location where blocks were captured
     */
    public void removeNovaBlocks(List<NovaBlockData> novaBlocks, Location center) {
        if (!available || novaBlocks == null || novaBlocks.isEmpty()) {
            return;
        }

        World world = center.getWorld();
        if (world == null) {
            return;
        }

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        int removed = 0;

        try {
            // Get BlockUtils class
            Class<?> blockUtilsClass = Class.forName("xyz.xenondevs.nova.util.BlockUtils");

            // Get Context.EMPTY for automated operations (no player involved)
            Class<?> contextClass = Class.forName("xyz.xenondevs.nova.context.Context");
            Object emptyContext = null;

            // Try EMPTY field first (companion object in Kotlin)
            try {
                java.lang.reflect.Field emptyField = contextClass.getDeclaredField("EMPTY");
                emptyContext = emptyField.get(null);
            } catch (NoSuchFieldException e1) {
                // Try empty() method as fallback
                try {
                    java.lang.reflect.Method emptyMethod = contextClass.getMethod("empty");
                    emptyContext = emptyMethod.invoke(null);
                } catch (NoSuchMethodException e2) {
                    addon.logWarning("Cannot get empty Nova context - skipping block removal");
                    return;
                }
            }

            if (emptyContext == null) {
                addon.logWarning("Nova context is null - skipping block removal");
                return;
            }

            // Get breakBlock method: breakBlock(Context, Location, boolean breakEffects)
            java.lang.reflect.Method breakBlockMethod = blockUtilsClass.getMethod(
                "breakBlock",
                contextClass,
                Location.class,
                boolean.class
            );

            final Object finalContext = emptyContext;

            for (NovaBlockData data : novaBlocks) {
                try {
                    Location loc = new Location(world,
                        centerX + data.relX,
                        centerY + data.relY,
                        centerZ + data.relZ);

                    // Break the block silently (no effects, no drops - we already captured them)
                    breakBlockMethod.invoke(null, finalContext, loc, false);
                    removed++;

                } catch (Exception e) {
                    // Skip blocks that fail - may have been removed already or aren't Nova blocks
                    if (addon.getSettings().isDebugEnabled()) {
                        addon.log("Failed to remove Nova block at " + data.relX + "," + data.relY + "," + data.relZ + ": " + e.getMessage());
                    }
                }
            }

            addon.log("Removed " + removed + "/" + novaBlocks.size() + " Nova blocks before WorldEdit operation");

        } catch (ClassNotFoundException e) {
            addon.logWarning("Nova BlockUtils class not found - cannot remove blocks: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            addon.logWarning("Nova breakBlock method not found - API may have changed: " + e.getMessage());
        } catch (Exception e) {
            addon.logWarning("Failed to remove Nova blocks: " + e.getMessage());
        }
    }

    /**
     * Remove Nova blocks asynchronously (ensures chunks are loaded first)
     */
    public void removeNovaBlocksAsync(List<NovaBlockData> novaBlocks, Location center, Consumer<Boolean> callback) {
        if (!available || novaBlocks == null || novaBlocks.isEmpty()) {
            callback.accept(true);
            return;
        }

        World world = center.getWorld();
        if (world == null) {
            callback.accept(false);
            return;
        }

        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();

        // Find the range from captured blocks
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (NovaBlockData data : novaBlocks) {
            minX = Math.min(minX, centerX + data.relX);
            maxX = Math.max(maxX, centerX + data.relX);
            minZ = Math.min(minZ, centerZ + data.relZ);
            maxZ = Math.max(maxZ, centerZ + data.relZ);
        }

        // Calculate chunks to load
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        List<CompletableFuture<org.bukkit.Chunk>> chunkFutures = new ArrayList<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                chunkFutures.add(world.getChunkAtAsync(cx, cz));
            }
        }

        CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]))
            .orTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .thenRun(() -> {
                // Chunks loaded, now remove on main thread
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    removeNovaBlocks(novaBlocks, center);
                    callback.accept(true);
                });
            })
            .exceptionally(throwable -> {
                addon.logWarning("Nova block removal timed out: " + throwable.getMessage());
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    callback.accept(false);
                });
                return null;
            });
    }

    /**
     * Data class for storing Nova block information
     */
    public static class NovaBlockData implements Serializable {
        private static final long serialVersionUID = 2L;

        public final int relX, relY, relZ;
        public final String blockId;
        public final List<ItemStack> drops;

        public NovaBlockData(int relX, int relY, int relZ, String blockId, List<ItemStack> drops) {
            this.relX = relX;
            this.relY = relY;
            this.relZ = relZ;
            this.blockId = blockId;
            this.drops = drops;
        }

        // Backward compatibility constructor (for existing data without drops)
        public NovaBlockData(int relX, int relY, int relZ, String blockId) {
            this(relX, relY, relZ, blockId, null);
        }
    }

    /**
     * Result of Nova block restoration operation.
     * Tracks successful and failed machine (TileEntity) restorations.
     */
    public static class RestoreResult {
        public final int machinesRestored;
        public final int machinesFailed;

        public RestoreResult(int machinesRestored, int machinesFailed) {
            this.machinesRestored = machinesRestored;
            this.machinesFailed = machinesFailed;
        }

        /**
         * Generate user-facing feedback message about machine preservation.
         * @return Feedback message or null if no machines to report
         */
        public String getFeedbackMessage() {
            if (machinesFailed > 0) {
                return "Preserved " + machinesRestored + " Nova machines (" + machinesFailed + " couldn't be restored)";
            } else if (machinesRestored > 0) {
                return "Preserved " + machinesRestored + " Nova machines";
            } else {
                return null; // No machines to report
            }
        }

        /**
         * Check if any machines failed to restore
         */
        public boolean hasFailures() {
            return machinesFailed > 0;
        }

        /**
         * Check if any machines were successfully restored
         */
        public boolean hasSuccesses() {
            return machinesRestored > 0;
        }
    }
}
