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
    private Class<?> novaClass;
    private Class<?> blockManagerClass;
    private Class<?> novaBlockStateClass;
    private Object blockManager;

    public NovaIntegration(IslandSelector addon) {
        this.addon = addon;
        this.available = detectNova();

        if (available) {
            addon.log("Nova integration enabled - custom block support active");
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
        return available;
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
     */
    public void restoreNovaBlocks(List<NovaBlockData> novaBlocks, Location center) {
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

        int restored = 0;

        try {
            // Get BlockUtils class for placing blocks
            Class<?> blockUtilsClass = Class.forName("xyz.xenondevs.nova.util.BlockUtils");

            // Get the block registry to look up blocks by ID
            Class<?> novaRegistriesClass = Class.forName("xyz.xenondevs.nova.registry.NovaRegistries");
            Object blockRegistry = novaRegistriesClass.getField("BLOCK").get(null);

            if (blockRegistry == null) {
                addon.logWarning("Nova block registry is null - cannot restore blocks");
                return;
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
                return;
            }

            if (novaContext == null) {
                addon.logWarning("Cannot create Nova context - skipping block restoration");
                return;
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

            addon.log("Restored " + restored + "/" + novaBlocks.size() + " Nova blocks");

        } catch (NoSuchFieldException e) {
            addon.logWarning("Nova API changed - field not found: " + e.getMessage());
        } catch (Exception e) {
            addon.logWarning("Failed to restore Nova blocks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Restore Nova blocks asynchronously
     */
    public void restoreNovaBlocksAsync(List<NovaBlockData> novaBlocks, Location center, Consumer<Boolean> callback) {
        if (!available || novaBlocks == null || novaBlocks.isEmpty()) {
            callback.accept(true);
            return;
        }

        // Nova block placement must happen on main thread
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            restoreNovaBlocks(novaBlocks, center);
            callback.accept(true);
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
}
