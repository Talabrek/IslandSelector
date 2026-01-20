package world.bentobox.islandselector.utils;

import org.bukkit.Location;
import world.bentobox.islandselector.IslandSelector;

import java.io.File;
import java.util.function.Consumer;

/**
 * Utility class for handling schematic operations.
 *
 * This class delegates to the WorldEditIntegration for actual operations.
 * It provides a simpler interface for common schematic tasks.
 *
 * When WorldEdit/FAWE is not available, operations will return false
 * and log appropriate warnings.
 *
 * Async methods are provided to prevent server freezes when using
 * standard WorldEdit (non-FAWE). These should be preferred over
 * synchronous methods for large operations.
 */
public class SchematicUtils {

    private final IslandSelector addon;

    public SchematicUtils(IslandSelector addon) {
        this.addon = addon;
    }

    /**
     * Check if schematic operations are available.
     * Returns false if WorldEdit/FAWE is not installed.
     */
    public boolean isAvailable() {
        return addon.getWorldEditIntegration() != null
            && addon.getWorldEditIntegration().isAvailable();
    }

    // ==================== ASYNC METHODS (PREFERRED) ====================

    /**
     * Copy a region and save it directly to a file (async).
     * This is the preferred method to prevent server freezes.
     *
     * @param center The center location
     * @param range The range from center
     * @param copyBiomes Whether to copy biomes
     * @param file The file to save to
     * @param callback Called with true if successful, false otherwise
     */
    public void copyAndSaveAsync(Location center, int range, boolean copyBiomes, File file, Consumer<Boolean> callback) {
        if (!isAvailable()) {
            addon.logWarning("Schematic operations not available - WorldEdit/FAWE not installed");
            callback.accept(false);
            return;
        }
        addon.getWorldEditIntegration().copyAndSaveAsync(center, range, copyBiomes, file, callback);
    }

    /**
     * Load a schematic file and paste it at a location (async).
     * This is the preferred method to prevent server freezes.
     *
     * @param file The schematic file to load
     * @param center The center location to paste at
     * @param callback Called with true if successful, false otherwise
     */
    public void loadAndPasteAsync(File file, Location center, Consumer<Boolean> callback) {
        if (!isAvailable()) {
            addon.logWarning("Schematic operations not available - WorldEdit/FAWE not installed");
            callback.accept(false);
            return;
        }
        addon.getWorldEditIntegration().loadAndPasteAsync(file, center, callback);
    }

    /**
     * Clear all blocks in a region (async).
     * This is the preferred method to prevent server freezes.
     *
     * @param center The center location of the region
     * @param range The range from center to clear
     * @param callback Called with true if successful, false otherwise
     */
    public void clearRegionAsync(Location center, int range, Consumer<Boolean> callback) {
        if (!isAvailable()) {
            addon.logWarning("Schematic operations not available - WorldEdit/FAWE not installed");
            callback.accept(false);
            return;
        }
        addon.getWorldEditIntegration().clearRegionAsync(center, range, callback);
    }

    /**
     * Copy a region to an in-memory clipboard (async).
     * This is the preferred method to prevent server freezes.
     *
     * @param center The center location
     * @param range The range from center
     * @param copyBiomes Whether to copy biomes
     * @param callback Called with clipboard object if successful, null otherwise
     */
    public void copyToClipboardAsync(Location center, int range, boolean copyBiomes, Consumer<Object> callback) {
        if (!isAvailable()) {
            addon.logWarning("Schematic operations not available - WorldEdit/FAWE not installed");
            callback.accept(null);
            return;
        }
        addon.getWorldEditIntegration().copyToClipboardAsync(center, range, copyBiomes, callback);
    }

    /**
     * Paste from an in-memory clipboard to a location (async).
     * This is the preferred method to prevent server freezes.
     *
     * @param clipboard The clipboard object from copyToClipboard
     * @param center The center location to paste at
     * @param callback Called with true if successful, false otherwise
     */
    public void pasteFromClipboardAsync(Object clipboard, Location center, Consumer<Boolean> callback) {
        if (!isAvailable()) {
            addon.logWarning("Schematic operations not available - WorldEdit/FAWE not installed");
            callback.accept(false);
            return;
        }
        addon.getWorldEditIntegration().pasteFromClipboardAsync(clipboard, center, callback);
    }

    // ==================== SYNC METHODS (USE SPARINGLY) ====================

    /**
     * Copy a region and save it directly to a file (synchronous).
     * WARNING: May cause server freeze with standard WorldEdit. Use async version instead.
     *
     * @param center The center location
     * @param range The range from center
     * @param copyBiomes Whether to copy biomes
     * @param file The file to save to
     * @return true if successful, false otherwise
     */
    public boolean copyAndSave(Location center, int range, boolean copyBiomes, File file) {
        if (!isAvailable()) {
            addon.logWarning("Schematic operations not available - WorldEdit/FAWE not installed");
            return false;
        }
        return addon.getWorldEditIntegration().copyAndSave(center, range, copyBiomes, file);
    }

    /**
     * Load a schematic file and paste it at a location (synchronous).
     * WARNING: May cause server freeze with standard WorldEdit. Use async version instead.
     *
     * @param file The schematic file to load
     * @param center The center location to paste at
     * @return true if successful, false otherwise
     */
    public boolean loadAndPaste(File file, Location center) {
        if (!isAvailable()) {
            addon.logWarning("Schematic operations not available - WorldEdit/FAWE not installed");
            return false;
        }
        return addon.getWorldEditIntegration().loadAndPaste(file, center);
    }

    /**
     * Clear all blocks in a region (synchronous).
     * WARNING: May cause server freeze with standard WorldEdit. Use async version instead.
     *
     * @param center The center location of the region
     * @param range The range from center to clear
     * @return true if successful, false otherwise
     */
    public boolean clearRegion(Location center, int range) {
        if (!isAvailable()) {
            addon.logWarning("Schematic operations not available - WorldEdit/FAWE not installed");
            return false;
        }
        return addon.getWorldEditIntegration().clearRegion(center, range);
    }

    /**
     * Copy a region to an in-memory clipboard (synchronous).
     * WARNING: May cause server freeze with standard WorldEdit. Use async version instead.
     *
     * @param center The center location
     * @param range The range from center
     * @param copyBiomes Whether to copy biomes
     * @return An opaque clipboard object, or null if failed
     */
    public Object copyToClipboard(Location center, int range, boolean copyBiomes) {
        if (!isAvailable()) {
            addon.logWarning("Schematic operations not available - WorldEdit/FAWE not installed");
            return null;
        }
        return addon.getWorldEditIntegration().copyToClipboard(center, range, copyBiomes);
    }

    /**
     * Paste from an in-memory clipboard to a location (synchronous).
     * WARNING: May cause server freeze with standard WorldEdit. Use async version instead.
     *
     * @param clipboard The clipboard object from copyToClipboard
     * @param center The center location to paste at
     * @return true if successful, false otherwise
     */
    public boolean pasteFromClipboard(Object clipboard, Location center) {
        if (!isAvailable()) {
            addon.logWarning("Schematic operations not available - WorldEdit/FAWE not installed");
            return false;
        }
        return addon.getWorldEditIntegration().pasteFromClipboard(clipboard, center);
    }
}
