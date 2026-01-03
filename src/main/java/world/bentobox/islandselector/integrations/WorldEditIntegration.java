package world.bentobox.islandselector.integrations;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import world.bentobox.islandselector.IslandSelector;

import java.io.File;
import java.util.function.Consumer;

/**
 * Integration layer for FastAsyncWorldEdit (FAWE) operations.
 *
 * This addon REQUIRES FastAsyncWorldEdit for island operations like:
 * - Slot switching (saving/loading island schematics)
 * - Backups
 * - Island relocation
 *
 * Standard WorldEdit is NOT supported due to performance issues with
 * large island operations and incompatibility with plugins like Nova.
 */
public class WorldEditIntegration {

    private final IslandSelector addon;
    private final boolean faweAvailable;

    public WorldEditIntegration(IslandSelector addon) {
        this.addon = addon;
        this.faweAvailable = Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit");

        if (faweAvailable) {
            addon.log("FastAsyncWorldEdit detected - island operations enabled");
        } else {
            addon.logWarning("==============================================");
            addon.logWarning("FastAsyncWorldEdit is NOT installed!");
            addon.logWarning("The following features are DISABLED:");
            addon.logWarning("  - Slot switching");
            addon.logWarning("  - Island backups");
            addon.logWarning("  - Island relocation");
            addon.logWarning("Install FAWE to enable these features.");
            addon.logWarning("Download: https://www.spigotmc.org/resources/fastasyncworldedit.13932/");
            addon.logWarning("==============================================");
        }
    }

    /**
     * Check if FAWE is available
     */
    public boolean isAvailable() {
        return faweAvailable;
    }

    /**
     * Check if FAWE is being used (always true when available)
     */
    public boolean isFAWE() {
        return faweAvailable;
    }

    /**
     * Copy a region to a clipboard and save it to a file asynchronously.
     * Runs fully async with FAWE.
     *
     * @param center The center location of the region
     * @param range The range from center to copy (creates a cube of 2*range)
     * @param copyBiomes Whether to copy biome data
     * @param file The file to save the schematic to
     * @param callback Called with true if successful, false otherwise
     */
    public void copyAndSaveAsync(Location center, int range, boolean copyBiomes, File file, Consumer<Boolean> callback) {
        if (!faweAvailable) {
            addon.logWarning("Cannot save schematic - FastAsyncWorldEdit not installed");
            callback.accept(false);
            return;
        }

        // FAWE runs truly async
        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            try {
                boolean result = WorldEditOperations.copyAndSave(addon, center, range, copyBiomes, file);
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> callback.accept(result));
            } catch (Exception e) {
                addon.logError("FAWE operation failed: " + e.getMessage());
                e.printStackTrace();
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> callback.accept(false));
            }
        });
    }

    /**
     * Synchronous version - use async version when possible
     */
    public boolean copyAndSave(Location center, int range, boolean copyBiomes, File file) {
        if (!faweAvailable) {
            addon.logWarning("Cannot save schematic - FastAsyncWorldEdit not installed");
            return false;
        }

        try {
            return WorldEditOperations.copyAndSave(addon, center, range, copyBiomes, file);
        } catch (NoClassDefFoundError | Exception e) {
            addon.logError("FAWE operation failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load a schematic file and paste it at a location asynchronously.
     * Runs fully async with FAWE.
     *
     * @param file The schematic file to load
     * @param center The center location to paste at
     * @param callback Called with true if successful, false otherwise
     */
    public void loadAndPasteAsync(File file, Location center, Consumer<Boolean> callback) {
        if (!faweAvailable) {
            addon.logWarning("Cannot paste schematic - FastAsyncWorldEdit not installed");
            callback.accept(false);
            return;
        }

        // FAWE runs truly async
        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            try {
                boolean result = WorldEditOperations.loadAndPaste(addon, file, center);
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> callback.accept(result));
            } catch (Exception e) {
                addon.logError("FAWE operation failed: " + e.getMessage());
                e.printStackTrace();
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> callback.accept(false));
            }
        });
    }

    /**
     * Synchronous version - use async version when possible
     */
    public boolean loadAndPaste(File file, Location center) {
        if (!faweAvailable) {
            addon.logWarning("Cannot paste schematic - FastAsyncWorldEdit not installed");
            return false;
        }

        try {
            return WorldEditOperations.loadAndPaste(addon, file, center);
        } catch (NoClassDefFoundError | Exception e) {
            addon.logError("FAWE operation failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Clear all blocks in a region asynchronously.
     * Removes entities on main thread first, then clears blocks async with FAWE.
     *
     * @param center The center location of the region
     * @param range The range from center to clear
     * @param callback Called with true if successful, false otherwise
     */
    public void clearRegionAsync(Location center, int range, Consumer<Boolean> callback) {
        if (!faweAvailable) {
            addon.logWarning("Cannot clear region - FastAsyncWorldEdit not installed");
            callback.accept(false);
            return;
        }

        // Remove entities on main thread first (cannot be done async)
        removeEntitiesInRegion(center.getWorld(), center, range);

        // Then clear blocks async with FAWE
        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            try {
                boolean result = WorldEditOperations.clearRegion(addon, center, range);
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> callback.accept(result));
            } catch (Exception e) {
                addon.logError("FAWE operation failed: " + e.getMessage());
                e.printStackTrace();
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> callback.accept(false));
            }
        });
    }

    /**
     * Remove all non-player entities in a region.
     * Must be called from main thread.
     *
     * @return The number of entities removed
     */
    public int removeEntitiesInRegion(World world, Location center, int range) {
        if (world == null) return 0;

        int minX = center.getBlockX() - range;
        int maxX = center.getBlockX() + range;
        int minZ = center.getBlockZ() - range;
        int maxZ = center.getBlockZ() + range;

        // Collect entities to remove first to avoid ConcurrentModificationException
        java.util.List<Entity> entitiesToRemove = new java.util.ArrayList<>();
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Player) continue;

            Location loc = entity.getLocation();
            if (loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ) {
                entitiesToRemove.add(entity);
            }
        }

        // Now remove collected entities
        for (Entity entity : entitiesToRemove) {
            entity.remove();
        }

        addon.log("Removed " + entitiesToRemove.size() + " entities in region around " + center.getBlockX() + "," + center.getBlockZ());
        return entitiesToRemove.size();
    }

    /**
     * Synchronous version - use async version when possible.
     * Removes entities first, then clears blocks.
     */
    public boolean clearRegion(Location center, int range) {
        if (!faweAvailable) {
            addon.logWarning("Cannot clear region - FastAsyncWorldEdit not installed");
            return false;
        }

        try {
            // Remove entities first (must be on main thread)
            removeEntitiesInRegion(center.getWorld(), center, range);
            return WorldEditOperations.clearRegion(addon, center, range);
        } catch (NoClassDefFoundError | Exception e) {
            addon.logError("FAWE operation failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Copy a region to an in-memory clipboard asynchronously.
     *
     * @param center The center location of the region
     * @param range The range from center to copy
     * @param copyBiomes Whether to copy biome data
     * @param callback Called with clipboard object if successful, null otherwise
     */
    public void copyToClipboardAsync(Location center, int range, boolean copyBiomes, Consumer<Object> callback) {
        if (!faweAvailable) {
            addon.logWarning("Cannot copy to clipboard - FastAsyncWorldEdit not installed");
            callback.accept(null);
            return;
        }

        // FAWE runs truly async
        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            try {
                Object result = WorldEditOperations.copyToClipboard(addon, center, range, copyBiomes);
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> callback.accept(result));
            } catch (Exception e) {
                addon.logError("FAWE operation failed: " + e.getMessage());
                e.printStackTrace();
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> callback.accept(null));
            }
        });
    }

    /**
     * Synchronous version - use async version when possible
     */
    public Object copyToClipboard(Location center, int range, boolean copyBiomes) {
        if (!faweAvailable) {
            addon.logWarning("Cannot copy to clipboard - FastAsyncWorldEdit not installed");
            return null;
        }

        try {
            return WorldEditOperations.copyToClipboard(addon, center, range, copyBiomes);
        } catch (NoClassDefFoundError | Exception e) {
            addon.logError("FAWE operation failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Paste from an in-memory clipboard to a location asynchronously.
     *
     * @param clipboard The clipboard object from copyToClipboard
     * @param center The center location to paste at
     * @param callback Called with true if successful, false otherwise
     */
    public void pasteFromClipboardAsync(Object clipboard, Location center, Consumer<Boolean> callback) {
        if (!faweAvailable || clipboard == null) {
            addon.logWarning("Cannot paste from clipboard - FastAsyncWorldEdit not installed or clipboard is null");
            callback.accept(false);
            return;
        }

        // FAWE runs truly async
        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            try {
                boolean result = WorldEditOperations.pasteFromClipboard(addon, clipboard, center);
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> callback.accept(result));
            } catch (Exception e) {
                addon.logError("FAWE operation failed: " + e.getMessage());
                e.printStackTrace();
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> callback.accept(false));
            }
        });
    }

    /**
     * Synchronous version - use async version when possible
     */
    public boolean pasteFromClipboard(Object clipboard, Location center) {
        if (!faweAvailable || clipboard == null) {
            addon.logWarning("Cannot paste from clipboard - FastAsyncWorldEdit not installed or clipboard is null");
            return false;
        }

        try {
            return WorldEditOperations.pasteFromClipboard(addon, clipboard, center);
        } catch (NoClassDefFoundError | Exception e) {
            addon.logError("FAWE operation failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get the name of the implementation being used
     */
    public String getImplementationName() {
        return faweAvailable ? "FastAsyncWorldEdit" : "None (FAWE required)";
    }
}
