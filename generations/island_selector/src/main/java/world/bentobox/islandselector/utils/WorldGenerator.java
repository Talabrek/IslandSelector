package world.bentobox.islandselector.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.models.DimensionConfig;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for creating and managing worlds based on DimensionConfig settings.
 * Supports void world generation, vanilla generation, and custom plugin generators.
 */
public class WorldGenerator {

    private final IslandSelector addon;

    public WorldGenerator(IslandSelector addon) {
        this.addon = Objects.requireNonNull(addon, "addon cannot be null");
    }

    /**
     * Gets an existing world or creates it if it doesn't exist and createIfMissing is true.
     *
     * @param config The dimension configuration
     * @return The world, or null if it doesn't exist and shouldn't be created
     */
    public World getOrCreateWorld(DimensionConfig config) {
        if (config == null || config.getWorldName() == null) {
            addon.logWarning("Cannot create world: null config or world name");
            return null;
        }

        String worldName = config.getWorldName();

        // Check if world already exists
        World existingWorld = Bukkit.getWorld(worldName);
        if (existingWorld != null) {
            addon.log("World already loaded: " + worldName);
            return existingWorld;
        }

        // Check if we should create it
        if (!config.isCreateIfMissing()) {
            addon.logWarning("World '" + worldName + "' not found and createIfMissing is false");
            return null;
        }

        // Create the world on the main thread
        return createWorld(config);
    }

    /**
     * Asynchronously ensures a world is loaded, creating it if necessary.
     * The actual world creation happens on the main thread.
     *
     * @param config The dimension configuration
     * @return CompletableFuture that resolves to the world or null on failure
     */
    public CompletableFuture<World> getOrCreateWorldAsync(DimensionConfig config) {
        CompletableFuture<World> future = new CompletableFuture<>();

        if (Bukkit.isPrimaryThread()) {
            // Already on main thread
            try {
                World world = getOrCreateWorld(config);
                future.complete(world);
            } catch (Exception e) {
                addon.logError("Failed to create world: " + e.getMessage());
                future.completeExceptionally(e);
            }
        } else {
            // Schedule on main thread
            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                try {
                    World world = getOrCreateWorld(config);
                    future.complete(world);
                } catch (Exception e) {
                    addon.logError("Failed to create world: " + e.getMessage());
                    future.completeExceptionally(e);
                }
            });
        }

        return future;
    }

    /**
     * Creates a world based on the dimension configuration.
     * MUST be called from the main thread.
     *
     * @param config The dimension configuration
     * @return The created world, or null on failure
     * @throws IllegalStateException if not called from the main thread
     */
    private World createWorld(DimensionConfig config) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("World creation must be called from the main thread");
        }
        if (config == null || config.getWorldName() == null) {
            addon.logWarning("Cannot create world: null config or world name in createWorld");
            return null;
        }
        String worldName = config.getWorldName();

        addon.log("Creating world: " + worldName + " with generator: " + config.getGeneratorType());

        try {
            WorldCreator creator = new WorldCreator(worldName);

            // Set environment
            World.Environment env = parseEnvironment(config.getEnvironment());
            creator.environment(env);

            // Set seed if specified
            if (config.getWorldSeed() != null) {
                creator.seed(config.getWorldSeed());
            }

            // Set generator based on type
            String generatorType = config.getGeneratorType();
            if (generatorType == null || generatorType.isEmpty() || "void".equalsIgnoreCase(generatorType)) {
                creator.generator(new VoidChunkGenerator());
                addon.log("Using void generator for world: " + worldName);
            } else if ("normal".equalsIgnoreCase(generatorType)) {
                // Use default vanilla generator (don't set any generator)
                addon.log("Using vanilla generator for world: " + worldName);
            } else {
                // Try to get custom generator from another plugin
                ChunkGenerator customGenerator = getCustomGenerator(generatorType, worldName);
                if (customGenerator != null) {
                    creator.generator(customGenerator);
                    addon.log("Using custom generator '" + generatorType + "' for world: " + worldName);
                } else {
                    addon.logWarning("Custom generator '" + generatorType + "' not found, falling back to void");
                    creator.generator(new VoidChunkGenerator());
                }
            }

            // Create the world
            World world = creator.createWorld();
            if (world != null) {
                addon.log("Successfully created world: " + worldName);

                // Configure world settings for skyblock
                world.setSpawnFlags(true, true);
                world.setKeepSpawnInMemory(false);

            } else {
                addon.logError("Failed to create world: " + worldName);
            }

            return world;

        } catch (Exception e) {
            addon.logError("Error creating world '" + worldName + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses environment string to Bukkit Environment enum.
     *
     * @param environment The environment string
     * @return The corresponding Environment, defaults to NORMAL
     */
    private World.Environment parseEnvironment(String environment) {
        if (environment == null || environment.isEmpty()) {
            return World.Environment.NORMAL;
        }

        switch (environment.toLowerCase()) {
            case "nether":
            case "the_nether":
                return World.Environment.NETHER;
            case "end":
            case "the_end":
                return World.Environment.THE_END;
            case "normal":
            case "overworld":
                return World.Environment.NORMAL;
            default:
                addon.logWarning("Unrecognized environment '" + environment + "', defaulting to NORMAL");
                return World.Environment.NORMAL;
        }
    }

    /**
     * Attempts to get a custom chunk generator from another plugin.
     *
     * @param generatorName The plugin name providing the generator
     * @param worldName The world name to generate for
     * @return The ChunkGenerator, or null if not found
     */
    private ChunkGenerator getCustomGenerator(String generatorName, String worldName) {
        // Parse generator:id format (e.g., "Terra:my_pack")
        String pluginName = generatorName;
        String generatorId = null;

        if (generatorName.contains(":")) {
            String[] parts = generatorName.split(":", 2);
            pluginName = parts[0];
            generatorId = parts[1];
        }

        Plugin generatorPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (generatorPlugin == null) {
            addon.logWarning("Generator plugin '" + pluginName + "' is not loaded");
            return null;
        }

        if (!generatorPlugin.isEnabled()) {
            addon.logWarning("Generator plugin '" + pluginName + "' is not enabled");
            return null;
        }

        try {
            return generatorPlugin.getDefaultWorldGenerator(worldName, generatorId);
        } catch (Exception e) {
            addon.logWarning("Failed to get generator from '" + pluginName + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * A void chunk generator that creates completely empty chunks.
     * Used for skyblock-style worlds where islands are pasted in.
     */
    public static class VoidChunkGenerator extends ChunkGenerator {

        @Override
        public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
            // Return empty chunk data
            return createChunkData(world);
        }

        @Override
        public boolean canSpawn(World world, int x, int z) {
            return true;
        }
    }
}
