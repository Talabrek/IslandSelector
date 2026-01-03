package world.bentobox.islandselector.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.models.DimensionConfig;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages multi-dimension island support.
 * Coordinates dimension configurations, world lookups, and location calculations
 * across all enabled dimensions.
 */
public class DimensionManager {

    private final IslandSelector addon;

    // Maps dimension key to Bukkit World
    private final Map<String, World> dimensionWorlds = new HashMap<>();

    // Reverse lookup: Bukkit World to dimension key
    private final Map<World, String> worldToDimensionKey = new HashMap<>();

    // Cached list of enabled dimensions
    private List<DimensionConfig> enabledDimensions = new ArrayList<>();

    // Whether multi-dimension is enabled and initialized
    private boolean initialized = false;

    public DimensionManager(IslandSelector addon) {
        this.addon = addon;
    }

    /**
     * Initialize the dimension manager.
     * Validates dimension configurations and loads world references.
     * Should be called after all worlds are loaded.
     */
    public void initialize() {
        dimensionWorlds.clear();
        worldToDimensionKey.clear();
        enabledDimensions.clear();

        if (!addon.getSettings().isMultiDimensionEnabled()) {
            addon.log("Multi-dimension support is disabled");
            initialized = false;
            return;
        }

        addon.log("Initializing multi-dimension support...");

        Map<String, DimensionConfig> configs = addon.getSettings().getDimensionConfigs();
        if (configs == null || configs.isEmpty()) {
            addon.logWarning("Multi-dimension is enabled but no dimensions are configured!");
            addon.logWarning("Add dimension configurations to config.yml under dimensions.worlds");
            initialized = false;
            return;
        }

        // Process each dimension configuration
        int loadedCount = 0;
        for (Map.Entry<String, DimensionConfig> entry : configs.entrySet()) {
            String dimensionKey = entry.getKey();
            DimensionConfig config = entry.getValue();

            // Ensure dimension key is set in config
            if (config.getDimensionKey() == null || config.getDimensionKey().isEmpty()) {
                config.setDimensionKey(dimensionKey);
            }

            if (!config.isEnabled()) {
                addon.log("  - " + dimensionKey + ": DISABLED");
                continue;
            }

            String worldName = config.getWorldName();
            if (worldName == null || worldName.isEmpty()) {
                addon.logWarning("  - " + dimensionKey + ": No world name configured, skipping");
                continue;
            }

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                addon.logWarning("  - " + dimensionKey + ": World '" + worldName + "' not found, skipping");
                continue;
            }

            // Register this dimension
            dimensionWorlds.put(dimensionKey, world);
            worldToDimensionKey.put(world, dimensionKey);
            enabledDimensions.add(config);
            loadedCount++;

            addon.log("  - " + dimensionKey + ": " + worldName + " (loaded)");
            addon.log("    Blueprint: " + config.getDefaultBlueprint());
            addon.log("    Create on claim: " + config.isCreateOnClaim());
        }

        if (loadedCount == 0) {
            addon.logWarning("No dimensions were loaded! Check your world names in config.yml");
            initialized = false;
            return;
        }

        // Verify primary dimension is loaded
        String primaryKey = addon.getSettings().getPrimaryDimension();
        if (!dimensionWorlds.containsKey(primaryKey)) {
            addon.logWarning("Primary dimension '" + primaryKey + "' is not loaded!");
            // Try to find a fallback
            if (!dimensionWorlds.isEmpty()) {
                String fallback = dimensionWorlds.keySet().iterator().next();
                addon.logWarning("Using '" + fallback + "' as primary dimension instead");
            }
        }

        initialized = true;
        addon.log("Multi-dimension support initialized with " + loadedCount + " dimension(s)");
    }

    /**
     * Check if multi-dimension support is enabled and properly initialized
     */
    public boolean isEnabled() {
        return addon.getSettings().isMultiDimensionEnabled() && initialized;
    }

    /**
     * Get all enabled dimension configurations
     * @return List of enabled DimensionConfig objects
     */
    public List<DimensionConfig> getEnabledDimensions() {
        return new ArrayList<>(enabledDimensions);
    }

    /**
     * Get dimension configurations that should create islands on claim
     * @return List of DimensionConfig objects with createOnClaim = true
     */
    public List<DimensionConfig> getDimensionsForCreation() {
        return enabledDimensions.stream()
                .filter(DimensionConfig::isCreateOnClaim)
                .collect(Collectors.toList());
    }

    /**
     * Get the Bukkit World for a dimension
     * @param dimensionKey The dimension key
     * @return The World, or null if not found/loaded
     */
    public World getWorld(String dimensionKey) {
        return dimensionWorlds.get(dimensionKey);
    }

    /**
     * Get the dimension key for a Bukkit World
     * @param world The Bukkit World
     * @return The dimension key, or null if not a registered dimension world
     */
    public String getDimensionKey(World world) {
        return worldToDimensionKey.get(world);
    }

    /**
     * Check if a world is a registered dimension world
     * @param world The world to check
     * @return true if this world is a registered dimension
     */
    public boolean isDimensionWorld(World world) {
        return worldToDimensionKey.containsKey(world);
    }

    /**
     * Get the DimensionConfig for a dimension key
     * @param dimensionKey The dimension key
     * @return The DimensionConfig, or null if not found
     */
    public DimensionConfig getDimensionConfig(String dimensionKey) {
        return enabledDimensions.stream()
                .filter(dc -> dc.getDimensionKey().equals(dimensionKey))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the primary dimension key
     * @return The primary dimension key
     */
    public String getPrimaryDimensionKey() {
        String primary = addon.getSettings().getPrimaryDimension();
        // Fallback if primary is not loaded
        if (!dimensionWorlds.containsKey(primary) && !dimensionWorlds.isEmpty()) {
            return dimensionWorlds.keySet().iterator().next();
        }
        return primary;
    }

    /**
     * Get the primary dimension world
     * @return The primary dimension World, or null if not loaded
     */
    public World getPrimaryWorld() {
        return getWorld(getPrimaryDimensionKey());
    }

    /**
     * Get the primary dimension config
     * @return The primary DimensionConfig, or null if not found
     */
    public DimensionConfig getPrimaryDimensionConfig() {
        return getDimensionConfig(getPrimaryDimensionKey());
    }

    /**
     * Calculate the world location for a grid coordinate in a specific dimension.
     * All dimensions share the same grid coordinate system.
     *
     * @param coord The grid coordinate
     * @param dimensionKey The dimension key
     * @return The Location in that dimension's world, or null if dimension not loaded
     */
    public Location getLocationForDimension(GridCoordinate coord, String dimensionKey) {
        World world = getWorld(dimensionKey);
        if (world == null) {
            return null;
        }

        int spacing = addon.getIslandSpacing();
        int worldX = coord.getX() * spacing;
        int worldZ = coord.getZ() * spacing;

        // Use sea level or configured Y for spawn point
        int y = world.getSeaLevel() + 1;

        return new Location(world, worldX, y, worldZ);
    }

    /**
     * Calculate the world location for a grid coordinate in a specific dimension,
     * with a specific Y coordinate.
     *
     * @param coord The grid coordinate
     * @param dimensionKey The dimension key
     * @param y The Y coordinate
     * @return The Location in that dimension's world, or null if dimension not loaded
     */
    public Location getLocationForDimension(GridCoordinate coord, String dimensionKey, int y) {
        World world = getWorld(dimensionKey);
        if (world == null) {
            return null;
        }

        int spacing = addon.getIslandSpacing();
        int worldX = coord.getX() * spacing;
        int worldZ = coord.getZ() * spacing;

        return new Location(world, worldX, y, worldZ);
    }

    /**
     * Get locations for all enabled dimensions at a grid coordinate
     * @param coord The grid coordinate
     * @return Map of dimension key to Location
     */
    public Map<String, Location> getLocationsForAllDimensions(GridCoordinate coord) {
        Map<String, Location> locations = new HashMap<>();
        for (DimensionConfig config : enabledDimensions) {
            Location loc = getLocationForDimension(coord, config.getDimensionKey());
            if (loc != null) {
                locations.put(config.getDimensionKey(), loc);
            }
        }
        return locations;
    }

    /**
     * Get the default blueprint for a dimension
     * @param dimensionKey The dimension key
     * @return The default blueprint name, or "default" if not configured
     */
    public String getDefaultBlueprint(String dimensionKey) {
        DimensionConfig config = getDimensionConfig(dimensionKey);
        if (config != null && config.getDefaultBlueprint() != null) {
            return config.getDefaultBlueprint();
        }
        return "default";
    }

    /**
     * Get all dimension keys
     * @return List of all registered dimension keys
     */
    public List<String> getDimensionKeys() {
        return new ArrayList<>(dimensionWorlds.keySet());
    }

    /**
     * Get the number of enabled dimensions
     * @return Count of enabled dimensions
     */
    public int getDimensionCount() {
        return enabledDimensions.size();
    }

    /**
     * Reload dimension configurations from settings.
     * Call this when the config is reloaded.
     */
    public void reload() {
        addon.log("Reloading dimension configurations...");
        initialize();
    }
}
