package world.bentobox.islandselector.managers;

import org.bukkit.World;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandselector.IslandSelector;

import java.util.UUID;

/**
 * Integration with the Level addon to retrieve island levels.
 * Uses defensive reflection to avoid class loading issues when other addons
 * (like Visit) are not present.
 */
public class LevelIntegration {

    private final IslandSelector addon;
    private boolean levelAddonPresent = false;
    private boolean levelAddonWorking = true; // Track if we've had errors

    public LevelIntegration(IslandSelector addon) {
        this.addon = addon;

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
            World world = addon.getGridManager().getBSkyBlockWorld();
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
