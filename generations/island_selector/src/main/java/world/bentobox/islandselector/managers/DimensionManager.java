package world.bentobox.islandselector.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.models.DimensionConfig;
import world.bentobox.islandselector.utils.GridCoordinate;
import world.bentobox.islandselector.utils.WorldGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Manages multi-dimension island support.
 * Coordinates dimension configurations, world lookups, and location calculations
 * across all enabled dimensions.
 */
public class DimensionManager {

    private final IslandSelector addon;
    private final WorldGenerator worldGenerator;

    // Maps dimension key to world NAME (not World reference to avoid stale refs)
    private final Map<String, String> dimensionWorldNames = new HashMap<>();

    // Cached world references - cleared on reload to avoid stale references
    private final Map<String, World> dimensionWorldCache = new HashMap<>();

    // Cached list of enabled dimensions
    private List<DimensionConfig> enabledDimensions = new ArrayList<>();

    // Whether multi-dimension is enabled and initialized
    private boolean initialized = false;

    public DimensionManager(IslandSelector addon) {
        this.addon = addon;
        this.worldGenerator = new WorldGenerator(addon);
    }

    /**
     * Initialize the dimension manager.
     * Validates dimension configurations and loads world references.
     * Should be called after all worlds are loaded.
     */
    public void initialize() {
        dimensionWorldNames.clear();
        dimensionWorldCache.clear();
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
                // Try to create the world if configured to do so
                if (config.isCreateIfMissing()) {
                    addon.log("  - " + dimensionKey + ": World '" + worldName + "' not found, attempting to create...");
                    world = worldGenerator.getOrCreateWorld(config);
                    if (world == null) {
                        addon.logWarning("  - " + dimensionKey + ": Failed to create world '" + worldName + "', skipping");
                        continue;
                    }
                } else {
                    addon.logWarning("  - " + dimensionKey + ": World '" + worldName + "' not found and createIfMissing is false, skipping");
                    continue;
                }
            }

            // Register this dimension by NAME (not World reference to avoid stale refs)
            dimensionWorldNames.put(dimensionKey, worldName);
            dimensionWorldCache.put(dimensionKey, world); // Cache for quick access
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
        if (!dimensionWorldNames.containsKey(primaryKey)) {
            addon.logWarning("Primary dimension '" + primaryKey + "' is not loaded!");
            // Try to find a fallback
            if (!dimensionWorldNames.isEmpty()) {
                String fallback = dimensionWorldNames.keySet().iterator().next();
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
     * Get the Bukkit World for a dimension.
     * Always looks up by world name to avoid stale World references.
     * @param dimensionKey The dimension key
     * @return The World, or null if not found/loaded
     */
    public World getWorld(String dimensionKey) {
        String worldName = dimensionWorldNames.get(dimensionKey);
        if (worldName == null) {
            return null;
        }
        // Always look up fresh to avoid stale World references
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            dimensionWorldCache.put(dimensionKey, world); // Update cache
        }
        return world;
    }

    /**
     * Get the dimension key for a Bukkit World
     * @param world The Bukkit World
     * @return The dimension key, or null if not a registered dimension world
     */
    public String getDimensionKey(World world) {
        if (world == null) {
            return null;
        }
        String worldName = world.getName();
        // Search by world name to avoid stale reference issues
        for (Map.Entry<String, String> entry : dimensionWorldNames.entrySet()) {
            if (entry.getValue().equals(worldName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Check if a world is a registered dimension world
     * @param world The world to check
     * @return true if this world is a registered dimension
     */
    public boolean isDimensionWorld(World world) {
        return getDimensionKey(world) != null;
    }

    /**
     * Get the DimensionConfig for a dimension key
     * @param dimensionKey The dimension key
     * @return The DimensionConfig, or null if not found
     */
    public DimensionConfig getDimensionConfig(String dimensionKey) {
        if (dimensionKey == null) {
            return null;
        }
        return enabledDimensions.stream()
                .filter(dc -> dc.getDimensionKey() != null && dc.getDimensionKey().equals(dimensionKey))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the primary dimension key
     * @return The primary dimension key
     */
    public String getPrimaryDimensionKey() {
        String primary = addon.getSettings().getPrimaryDimension();
        // Check if primary exists
        if (dimensionWorldNames.containsKey(primary)) {
            return primary;
        }
        // Fallback if primary is not loaded
        if (!dimensionWorldNames.isEmpty()) {
            return dimensionWorldNames.keySet().iterator().next();
        }
        // No dimensions loaded
        return null;
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

        // Must use spacing * 2 to match GridManager and GridLocationStrategy calculations
        int spacing = addon.getIslandSpacing() * 2;
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

        // Must use spacing * 2 to match GridManager and GridLocationStrategy calculations
        int spacing = addon.getIslandSpacing() * 2;
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
        return new ArrayList<>(dimensionWorldNames.keySet());
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
