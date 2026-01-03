package world.bentobox.islandselector.managers;

import org.bukkit.World;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.models.DimensionConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Integration with the Level addon to retrieve island levels.
 * Uses defensive reflection to avoid class loading issues when other addons
 * (like Visit) are not present.
 */
public class LevelIntegration {

    private final IslandSelector addon;
    private volatile boolean levelAddonPresent = false;
    private volatile boolean levelAddonWorking = true; // Track if we've had errors

    // Cache for aggregated levels (expires after 60 seconds)
    private static final long CACHE_EXPIRY_MS = 60_000;
    private final Map<UUID, CachedLevel> levelCache = new ConcurrentHashMap<>();

    /**
     * Cached level data with timestamp
     */
    private static class CachedLevel {
        final long level;
        final long timestamp;

        CachedLevel(long level) {
            this.level = level;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp >= CACHE_EXPIRY_MS;
        }
    }

    public LevelIntegration(IslandSelector addon) {
        this.addon = Objects.requireNonNull(addon, "addon cannot be null");

        // Check if Level addon is present
        levelAddonPresent = BentoBox.getInstance().getAddonsManager()
            .getAddonByName("Level").isPresent();

        if (levelAddonPresent) {
            addon.log("Level addon detected - island level display enabled");
        } else {
            addon.log("Level addon not found - island level display disabled");
        }
    }

    /**
     * Check if Level integration is available
     */
    public boolean isEnabled() {
        return levelAddonPresent && levelAddonWorking;
    }

    /**
     * Get the island level for a player.
     * Uses reflection to access the Level addon.
     *
     * @param playerUUID The island owner's UUID
     * @return The island level, or -1 if unable to retrieve
     */
    public long getIslandLevel(UUID playerUUID) {
        if (!levelAddonPresent || !levelAddonWorking) {
            return -1;
        }

        try {
            var levelAddon = BentoBox.getInstance().getAddonsManager()
                .getAddonByName("Level");

            if (levelAddon.isEmpty()) {
                return -1;
            }

            Object level = levelAddon.get();

            // Get the BSkyBlock world
            GridManager gm = addon.getGridManager();
            if (gm == null) {
                return -1;
            }
            World world = gm.getBSkyBlockWorld();
            if (world == null) {
                return -1;
            }

            // Try to get island level using the Level addon API
            // Use getDeclaredMethod instead of getMethod to avoid scanning parent classes
            // which might reference unavailable addons like Visit

            // Try the main addon class first
            try {
                java.lang.reflect.Method getIslandLevel = findMethod(level.getClass(),
                    "getIslandLevel", World.class, UUID.class);
                if (getIslandLevel != null) {
                    getIslandLevel.setAccessible(true);
                    Object result = getIslandLevel.invoke(level, world, playerUUID);
                    if (result instanceof Long) {
                        return (Long) result;
                    }
                    if (result instanceof Number) {
                        return ((Number) result).longValue();
                    }
                }
            } catch (Exception e) {
                // Try alternative method
            }

            // Try getting through manager
            try {
                java.lang.reflect.Method getManager = findMethod(level.getClass(), "getManager");
                if (getManager != null) {
                    getManager.setAccessible(true);
                    Object manager = getManager.invoke(level);

                    if (manager != null) {
                        java.lang.reflect.Method getIslandLevel = findMethod(manager.getClass(),
                            "getIslandLevel", World.class, UUID.class);
                        if (getIslandLevel != null) {
                            getIslandLevel.setAccessible(true);
                            Object result = getIslandLevel.invoke(manager, world, playerUUID);
                            if (result instanceof Long) {
                                return (Long) result;
                            }
                            if (result instanceof Number) {
                                return ((Number) result).longValue();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Could not get level through manager
            }

        } catch (LinkageError e) {
            // This can happen if the Level addon references classes from other addons
            // that aren't installed (like VisitAddon). Disable integration.
            addon.logWarning("Level addon has missing dependencies, disabling integration: " + e.getMessage());
            levelAddonWorking = false;
            return -1;
        } catch (Exception e) {
            // Don't spam logs, just return -1
        }

        return -1;
    }

    /**
     * Find a method by name and parameter types, searching only declared methods
     * to avoid triggering class loading of parent/interface methods.
     */
    private java.lang.reflect.Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            // First try getDeclaredMethod on this class only
            return clazz.getDeclaredMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            // Try parent classes manually, but be careful
            Class<?> parent = clazz.getSuperclass();
            if (parent != null && !parent.equals(Object.class)) {
                try {
                    return findMethod(parent, name, paramTypes);
                } catch (Exception e2) {
                    // Not found in parent
                }
            }
        } catch (LinkageError e) {
            // Class loading issue - skip this class
        }
        return null;
    }

    /**
     * Get the formatted island level string (with formatting like "1.2K" for 1200)
     *
     * @param playerUUID The island owner's UUID
     * @return Formatted level string, or "N/A" if unavailable
     */
    public String getFormattedIslandLevel(UUID playerUUID) {
        try {
            long level = getIslandLevel(playerUUID);
            if (level < 0) {
                return "N/A";
            }

            // Format large numbers
            if (level >= 1_000_000_000) {
                return String.format("%.1fB", level / 1_000_000_000.0);
            } else if (level >= 1_000_000) {
                return String.format("%.1fM", level / 1_000_000.0);
            } else if (level >= 1_000) {
                return String.format("%.1fK", level / 1_000.0);
            } else {
                return String.valueOf(level);
            }
        } catch (LinkageError e) {
            levelAddonWorking = false;
            return "N/A";
        }
    }

    /**
     * Get the island level for a specific world.
     *
     * @param playerUUID The island owner's UUID
     * @param world The world to get level for
     * @return The island level, or -1 if unable to retrieve
     */
    public long getIslandLevelForWorld(UUID playerUUID, World world) {
        if (!levelAddonPresent || !levelAddonWorking || world == null) {
            return -1;
        }

        try {
            var levelAddon = BentoBox.getInstance().getAddonsManager()
                .getAddonByName("Level");

            if (levelAddon.isEmpty()) {
                return -1;
            }

            Object level = levelAddon.get();

            // Try main addon class first
            try {
                java.lang.reflect.Method getIslandLevel = findMethod(level.getClass(),
                    "getIslandLevel", World.class, UUID.class);
                if (getIslandLevel != null) {
                    getIslandLevel.setAccessible(true);
                    Object result = getIslandLevel.invoke(level, world, playerUUID);
                    if (result instanceof Long) {
                        return (Long) result;
                    }
                    if (result instanceof Number) {
                        return ((Number) result).longValue();
                    }
                }
            } catch (Exception e) {
                // Try alternative method
            }

            // Try through manager
            try {
                java.lang.reflect.Method getManager = findMethod(level.getClass(), "getManager");
                if (getManager != null) {
                    getManager.setAccessible(true);
                    Object manager = getManager.invoke(level);

                    if (manager != null) {
                        java.lang.reflect.Method getIslandLevel = findMethod(manager.getClass(),
                            "getIslandLevel", World.class, UUID.class);
                        if (getIslandLevel != null) {
                            getIslandLevel.setAccessible(true);
                            Object result = getIslandLevel.invoke(manager, world, playerUUID);
                            if (result instanceof Long) {
                                return (Long) result;
                            }
                            if (result instanceof Number) {
                                return ((Number) result).longValue();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Could not get level through manager
            }

        } catch (LinkageError e) {
            levelAddonWorking = false;
            return -1;
        } catch (Exception e) {
            // Silent fail
        }

        return -1;
    }

    /**
     * Get aggregated island level from all enabled dimensions.
     * Uses caching to avoid repeated reflection calls.
     *
     * @param playerUUID The island owner's UUID
     * @return Sum of levels from all dimensions, or -1 if unable to retrieve any
     */
    public long getAggregatedIslandLevel(UUID playerUUID) {
        if (!levelAddonPresent || !levelAddonWorking) {
            return -1;
        }

        // Check if multi-dimension is enabled
        DimensionManager dimManager = addon.getDimensionManager();
        if (dimManager == null || !dimManager.isEnabled()) {
            // Fall back to single world level
            return getIslandLevel(playerUUID);
        }

        // Check cache first
        CachedLevel cached = levelCache.get(playerUUID);
        if (cached != null && !cached.isExpired()) {
            return cached.level;
        }

        // Calculate aggregated level
        long totalLevel = 0;
        boolean anySuccess = false;

        List<DimensionConfig> dimensions = dimManager.getEnabledDimensions();
        if (dimensions == null || dimensions.isEmpty()) {
            // Fall back to single world level if no dimensions configured
            return getIslandLevel(playerUUID);
        }

        for (DimensionConfig config : dimensions) {
            World world = dimManager.getWorld(config.getDimensionKey());
            if (world != null) {
                long dimLevel = getIslandLevelForWorld(playerUUID, world);
                if (dimLevel >= 0) {
                    totalLevel += dimLevel;
                    anySuccess = true;
                }
            }
        }

        long result = anySuccess ? totalLevel : -1;

        // Only cache successful results to avoid stale failure values
        if (result >= 0) {
            levelCache.put(playerUUID, new CachedLevel(result));
        }

        return result;
    }

    /**
     * Get cached aggregated level if available, otherwise calculate and cache.
     *
     * @param playerUUID The island owner's UUID
     * @return Aggregated level or -1
     */
    public long getCachedAggregatedLevel(UUID playerUUID) {
        CachedLevel cached = levelCache.get(playerUUID);
        if (cached != null && !cached.isExpired()) {
            return cached.level;
        }
        return getAggregatedIslandLevel(playerUUID);
    }

    /**
     * Get levels broken down by dimension.
     *
     * @param playerUUID The island owner's UUID
     * @return Map of dimension key to level
     */
    public Map<String, Long> getLevelsByDimension(UUID playerUUID) {
        Map<String, Long> levels = new HashMap<>();

        if (!levelAddonPresent || !levelAddonWorking) {
            return levels;
        }

        DimensionManager dimManager = addon.getDimensionManager();
        if (dimManager == null || !dimManager.isEnabled()) {
            // Single world mode - use primary world
            GridManager gm = addon.getGridManager();
            if (gm != null) {
                World world = gm.getBSkyBlockWorld();
                if (world != null) {
                    long level = getIslandLevel(playerUUID);
                    if (level >= 0) {
                        levels.put("primary", level);
                    }
                }
            }
            return levels;
        }

        // Get level for each dimension
        List<DimensionConfig> dimensions = dimManager.getEnabledDimensions();
        if (dimensions == null || dimensions.isEmpty()) {
            return levels;
        }

        for (DimensionConfig config : dimensions) {
            World world = dimManager.getWorld(config.getDimensionKey());
            if (world != null) {
                long level = getIslandLevelForWorld(playerUUID, world);
                if (level >= 0) {
                    levels.put(config.getDimensionKey(), level);
                }
            }
        }

        return levels;
    }

    /**
     * Get formatted aggregated level string (with K/M/B suffixes).
     *
     * @param playerUUID The island owner's UUID
     * @return Formatted level string, or "N/A" if unavailable
     */
    public String getFormattedAggregatedLevel(UUID playerUUID) {
        try {
            long level = getAggregatedIslandLevel(playerUUID);
            if (level < 0) {
                return "N/A";
            }
            return formatLevel(level);
        } catch (LinkageError e) {
            levelAddonWorking = false;
            return "N/A";
        }
    }

    /**
     * Format a level value with K/M/B suffixes.
     *
     * @param level The level value
     * @return Formatted string
     */
    private String formatLevel(long level) {
        if (level >= 1_000_000_000) {
            return String.format("%.1fB", level / 1_000_000_000.0);
        } else if (level >= 1_000_000) {
            return String.format("%.1fM", level / 1_000_000.0);
        } else if (level >= 1_000) {
            return String.format("%.1fK", level / 1_000.0);
        } else {
            return String.valueOf(level);
        }
    }

    /**
     * Invalidate the cached level for a player.
     * Call this when island changes occur.
     *
     * @param playerUUID The player whose cache to invalidate
     */
    public void invalidateCache(UUID playerUUID) {
        levelCache.remove(playerUUID);
    }

    /**
     * Clear all cached levels.
     */
    public void clearCache() {
        levelCache.clear();
    }

    /**
     * Get the island's initial level (value when first scanned)
     *
     * @param island The island
     * @return Initial level, or -1 if unavailable
     */
    public long getInitialIslandLevel(Island island) {
        if (!levelAddonPresent || !levelAddonWorking || island == null) {
            return -1;
        }

        try {
            var levelAddon = BentoBox.getInstance().getAddonsManager()
                .getAddonByName("Level");

            if (levelAddon.isEmpty()) {
                return -1;
            }

            Object level = levelAddon.get();

            // Try to get initial level through manager
            java.lang.reflect.Method getManager = findMethod(level.getClass(), "getManager");
            if (getManager != null) {
                getManager.setAccessible(true);
                Object manager = getManager.invoke(level);

                if (manager != null) {
                    java.lang.reflect.Method getInitialLevel = findMethod(manager.getClass(),
                        "getInitialLevel", Island.class);
                    if (getInitialLevel != null) {
                        getInitialLevel.setAccessible(true);
                        Object result = getInitialLevel.invoke(manager, island);
                        if (result instanceof Long) {
                            return (Long) result;
                        }
                        if (result instanceof Number) {
                            return ((Number) result).longValue();
                        }
                    }
                }
            }
        } catch (LinkageError e) {
            levelAddonWorking = false;
        } catch (Exception e) {
            // Silent fail - initial level is optional info
        }

        return -1;
    }
}
