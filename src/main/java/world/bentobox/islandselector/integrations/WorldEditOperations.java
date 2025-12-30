package world.bentobox.islandselector.integrations;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import world.bentobox.islandselector.IslandSelector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * FastAsyncWorldEdit operations for island management.
 *
 * This class is isolated to prevent NoClassDefFoundError when FAWE is not installed.
 * All methods assume FAWE is available and run asynchronously.
 *
 * IMPORTANT: Entity handling is DISABLED in FAWE operations!
 * FAWE has bugs with villager NBT data that cause villagers to lose their
 * professions/trades. Instead, EntityStorage handles all entity operations
 * separately with proper Bukkit serialization.
 *
 * FAWE handles:
 * - Async block operations
 * - NBT data preservation for BLOCKS (tile entities like chests, signs)
 * - Biome copying
 *
 * EntityStorage handles:
 * - All entity saving/loading (villagers, animals, item frames, etc.)
 */
public class WorldEditOperations {

    /**
     * Copy a region and save it to a schematic file.
     * BLOCKS ONLY - entities are handled separately by EntityStorage.
     */
    public static boolean copyAndSave(IslandSelector addon, Location center, int range,
                                       boolean copyBiomes, File file) {
        World world = center.getWorld();
        if (world == null) {
            return false;
        }

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        BlockVector3 min = BlockVector3.at(
            center.getX() - range,
            world.getMinHeight(),
            center.getZ() - range
        );
        BlockVector3 max = BlockVector3.at(
            center.getX() + range,
            world.getMaxHeight() - 1,
            center.getZ() + range
        );

        CuboidRegion region = new CuboidRegion(weWorld, min, max);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(BlockVector3.at(center.getX(), center.getY(), center.getZ()));

        try {
            // IMPORTANT: copyEntities = FALSE - EntityStorage handles entities separately
            copyToClipboardInternal(weWorld, region, clipboard, false, copyBiomes);
            addon.log("Successfully copied region blocks (entities handled by EntityStorage)");
        } catch (Exception e) {
            addon.logError("Failed to copy region: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        // Save to file (sponge schematic format preserves NBT for blocks)
        try {
            file.getParentFile().mkdirs();
            try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(
                    new FileOutputStream(file))) {
                writer.write(clipboard);
            }
            return true;
        } catch (Exception e) {
            addon.logError("Failed to save schematic to file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load a schematic file and paste it at a location.
     * BLOCKS ONLY - entities are handled separately by EntityStorage.
     */
    public static boolean loadAndPaste(IslandSelector addon, File file, Location center) {
        if (!file.exists()) {
            addon.logError("Schematic file not found: " + file.getAbsolutePath());
            return false;
        }

        World world = center.getWorld();
        if (world == null) {
            return false;
        }

        try {
            // Load clipboard from file (sponge format preserves NBT for blocks)
            Clipboard clipboard;
            try (FileInputStream fis = new FileInputStream(file)) {
                clipboard = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(fis).read();
            }

            // Paste blocks to world - ENTITIES DISABLED (EntityStorage handles them)
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                    .world(weWorld)
                    .maxBlocks(-1)
                    .build()) {
                Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(center.getX(), center.getY(), center.getZ()))
                    .ignoreAirBlocks(false)
                    .copyEntities(false)  // DISABLED - EntityStorage handles entities
                    .copyBiomes(true)
                    .build();

                Operations.complete(operation);
            }

            addon.log("Successfully pasted schematic blocks (entities handled by EntityStorage)");
            return true;

        } catch (Exception e) {
            addon.logError("Failed to load and paste schematic: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Clear all blocks in a region (set to air).
     * NOTE: Entity removal must be done on the main thread BEFORE calling this method.
     * Use clearRegionWithEntities() for the full operation with entity removal.
     */
    public static boolean clearRegion(IslandSelector addon, Location center, int range) {
        World world = center.getWorld();
        if (world == null) {
            return false;
        }

        try {
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            BlockVector3 min = BlockVector3.at(
                center.getX() - range,
                world.getMinHeight(),
                center.getZ() - range
            );
            BlockVector3 max = BlockVector3.at(
                center.getX() + range,
                world.getMaxHeight() - 1,
                center.getZ() + range
            );

            CuboidRegion region = new CuboidRegion(weWorld, min, max);

            // Clear blocks only - entity removal must be done on main thread before this
            try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                    .world(weWorld)
                    .maxBlocks(-1)
                    .build()) {
                editSession.setBlocks((com.sk89q.worldedit.regions.Region) region, BlockTypes.AIR.getDefaultState());
            }

            return true;

        } catch (Exception e) {
            addon.logError("Failed to clear region: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Copy a region to an in-memory clipboard.
     * BLOCKS ONLY - entities are handled separately by EntityStorage.
     */
    public static Object copyToClipboard(IslandSelector addon, Location center, int range,
                                          boolean copyBiomes) {
        World world = center.getWorld();
        if (world == null) {
            return null;
        }

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        BlockVector3 min = BlockVector3.at(
            center.getX() - range,
            world.getMinHeight(),
            center.getZ() - range
        );
        BlockVector3 max = BlockVector3.at(
            center.getX() + range,
            world.getMaxHeight() - 1,
            center.getZ() + range
        );

        CuboidRegion region = new CuboidRegion(weWorld, min, max);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(BlockVector3.at(center.getX(), center.getY(), center.getZ()));

        try {
            // IMPORTANT: copyEntities = FALSE - EntityStorage handles entities separately
            copyToClipboardInternal(weWorld, region, clipboard, false, copyBiomes);
            addon.log("Successfully copied region blocks to clipboard (entities handled by EntityStorage)");
            return clipboard;
        } catch (Exception e) {
            addon.logError("Failed to copy region to clipboard: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Paste from an in-memory clipboard to a location.
     * BLOCKS ONLY - entities are handled separately by EntityStorage.
     */
    public static boolean pasteFromClipboard(IslandSelector addon, Object clipboardObj,
                                              Location center) {
        if (!(clipboardObj instanceof Clipboard)) {
            addon.logError("Invalid clipboard object");
            return false;
        }

        World world = center.getWorld();
        if (world == null) {
            return false;
        }

        Clipboard clipboard = (Clipboard) clipboardObj;
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);

        try {
            try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                    .world(weWorld)
                    .maxBlocks(-1)
                    .build()) {
                Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(center.getX(), center.getY(), center.getZ()))
                    .ignoreAirBlocks(false)
                    .copyEntities(false)  // DISABLED - EntityStorage handles entities
                    .copyBiomes(true)
                    .build();

                Operations.complete(operation);
            }

            addon.log("Successfully pasted from clipboard blocks (entities handled by EntityStorage)");
            return true;

        } catch (Exception e) {
            addon.logError("Failed to paste from clipboard: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if an exception is related to entity paste errors (null NBT)
     */
    private static boolean isEntityPasteError(Throwable e) {
        Throwable current = e;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                if (message.contains("getNbt()") && message.contains("null")) {
                    return true;
                }
                if (message.contains("BaseEntity") && message.contains("null")) {
                    return true;
                }
            }
            if (current instanceof NullPointerException) {
                for (StackTraceElement element : current.getStackTrace()) {
                    if (element.getMethodName().contains("createEntity") ||
                        element.getClassName().contains("EntityCopy") ||
                        element.getClassName().contains("EntityVisitor")) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Internal method to perform the actual copy operation
     */
    private static void copyToClipboardInternal(com.sk89q.worldedit.world.World weWorld,
                                                  CuboidRegion region,
                                                  BlockArrayClipboard clipboard,
                                                  boolean copyEntities,
                                                  boolean copyBiomes) throws Exception {
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            ForwardExtentCopy copy = new ForwardExtentCopy(
                editSession, region, clipboard, region.getMinimumPoint()
            );
            copy.setCopyingEntities(copyEntities);
            copy.setCopyingBiomes(copyBiomes);
            Operations.complete(copy);
        }
    }

    /**
     * Check if an exception is related to entity copy errors
     */
    private static boolean isEntityCopyError(Throwable e) {
        Throwable current = e;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                if (message.contains("Asynchronous") && message.contains("getEntities")) {
                    return true;
                }
                if (message.contains("Cannot invoke") && message.contains("getClass")) {
                    return true;
                }
                if (current instanceof IllegalStateException && message.contains("Async")) {
                    return true;
                }
            }
            for (StackTraceElement element : current.getStackTrace()) {
                if (element.getMethodName().contains("getEntities") ||
                    element.getMethodName().contains("getFullEntities")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
