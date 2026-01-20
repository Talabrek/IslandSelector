package world.bentobox.islandselector.managers;

import org.bukkit.World;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.islandselector.IslandSelector;

import java.util.Objects;
import java.util.UUID;

/**
 * Integration with the Warps addon to check if players have warp signs.
 * Uses defensive reflection to avoid class loading issues.
 */
public class WarpIntegration {

    private final IslandSelector addon;
    private volatile boolean warpAddonPresent = false;
    private volatile boolean warpAddonWorking = true;

    public WarpIntegration(IslandSelector addon) {
        this.addon = Objects.requireNonNull(addon, "addon cannot be null");

        // Check if Warps addon is present
        warpAddonPresent = BentoBox.getInstance().getAddonsManager()
            .getAddonByName("Warps").isPresent();

        if (warpAddonPresent) {
            addon.log("Warps addon detected - warp checking enabled");
        } else {
            addon.log("Warps addon not found - warp checking disabled (visits unrestricted)");
        }
    }

    /**
     * Check if Warps integration is available
     */
    public boolean isEnabled() {
        return warpAddonPresent && warpAddonWorking;
    }

    /**
     * Check if a player has an active warp sign.
     * Uses reflection to access the Warps addon.
     *
     * @param playerUUID The player's UUID
     * @return true if the player has a warp, false otherwise (or if Warps is not enabled)
     */
    public boolean hasWarp(UUID playerUUID) {
        if (!warpAddonPresent || !warpAddonWorking) {
            // If Warps addon not present, return false (no restriction - allows visit)
            return false;
        }

        if (playerUUID == null) {
            return false;
        }

        try {
            var warpAddon = BentoBox.getInstance().getAddonsManager()
                .getAddonByName("Warps");

            if (warpAddon.isEmpty()) {
                return false;
            }

            Object warps = warpAddon.get();

            // Get the BSkyBlock world
            GridManager gm = addon.getGridManager();
            if (gm == null) {
                return false;
            }
            World world = gm.getBSkyBlockWorld();
            if (world == null) {
                return false;
            }

            // Try to get WarpSignsManager and check for warp
            try {
                java.lang.reflect.Method getWarpSignsManager = findMethod(warps.getClass(), "getWarpSignsManager");
                if (getWarpSignsManager != null) {
                    getWarpSignsManager.setAccessible(true);
                    Object manager = getWarpSignsManager.invoke(warps);

                    if (manager != null) {
                        // Method signature is hasWarp(World, UUID) - World comes first
                        java.lang.reflect.Method hasWarpMethod = findMethod(manager.getClass(),
                            "hasWarp", World.class, UUID.class);
                        if (hasWarpMethod != null) {
                            hasWarpMethod.setAccessible(true);
                            Object result = hasWarpMethod.invoke(manager, world, playerUUID);
                            if (result instanceof Boolean) {
                                return (Boolean) result;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Could not check warp status
            }

        } catch (LinkageError e) {
            // This can happen if the Warps addon references classes that aren't installed
            addon.logWarning("Warps addon has missing dependencies, disabling integration: " + e.getMessage());
            warpAddonWorking = false;
            return false;
        } catch (Exception e) {
            // Don't spam logs, just return false
        }

        return false;
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
}
