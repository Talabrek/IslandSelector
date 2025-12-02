package world.bentobox.islandselector.managers;

import com.fastasyncworldedit.core.FaweAPI;
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

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.events.SlotSwitchEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Manager for handling slot switching operations with FAWE
 */
public class SlotSwitchManager {

    private final IslandSelector addon;

    public SlotSwitchManager(IslandSelector addon) {
        this.addon = addon;
    }

    /**
     * Perform a complete slot switch operation
     * This is run asynchronously to avoid blocking the server
     */
    public void switchSlot(Player player, SlotData fromSlot, SlotData toSlot) {
        UUID playerUUID = player.getUniqueId();

        // Fire SlotSwitchEvent on main thread BEFORE starting the async operation
        SlotSwitchEvent event = new SlotSwitchEvent(
            player,
            fromSlot.getSlotNumber(),
            toSlot.getSlotNumber(),
            fromSlot.getSlotName(),
            toSlot.getSlotName()
        );
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            Bukkit.getPluginManager().callEvent(event);

            // Check if event was cancelled
            if (event.isCancelled()) {
                addon.log("SlotSwitchEvent cancelled for " + player.getName() +
                         " from slot " + fromSlot.getSlotNumber() + " to " + toSlot.getSlotNumber());
                if (event.getCancellationReason() != null) {
                    player.sendMessage("§c" + event.getCancellationReason());
                } else {
                    player.sendMessage("§cSlot switch cancelled.");
                }
                return;
            }

            // Event not cancelled - proceed with the switch asynchronously
            performSlotSwitchAsync(player, fromSlot, toSlot);
        });
    }

    /**
     * Internal method to perform the actual slot switch asynchronously
     */
    private void performSlotSwitchAsync(Player player, SlotData fromSlot, SlotData toSlot) {
        UUID playerUUID = player.getUniqueId();

        // Run asynchronously to avoid blocking main thread
        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            try {
                // Step 1: Send progress message
                sendProgress(player, "&eStarting slot switch...");

                // Step 1.5: Create backup if configured
                if (addon.getSettings().isBackupOnSwitch()) {
                    sendProgress(player, "&eCreating backup of current island...");
                    boolean backed = addon.getBackupManager().createBackup(playerUUID, fromSlot.getSlotNumber());
                    if (backed) {
                        addon.log("Auto-backup created for " + player.getName() + " slot " + fromSlot.getSlotNumber());
                    } else {
                        addon.logError("Auto-backup failed for " + player.getName() + " slot " + fromSlot.getSlotNumber() + " - continuing switch anyway");
                        // Don't cancel switch if backup fails - it's a safety feature, not critical
                    }
                }

                // Step 2: Save current island to schematic
                sendProgress(player, "&eSaving current island...");
                boolean saved = saveIslandToSchematic(fromSlot);
                if (!saved) {
                    sendError(player, "&cFailed to save current island! Switch cancelled.");
                    return;
                }

                // Step 3: Clear current island blocks from world (back to main thread)
                sendProgress(player, "&eClearing current island from world...");
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    clearIslandBlocks(fromSlot);
                });

                // Wait for clearing to complete
                Thread.sleep(2000);

                // Step 4: Load target slot schematic
                sendProgress(player, "&eLoading target island...");
                boolean loaded = loadSchematicToWorld(toSlot);
                if (!loaded) {
                    sendError(player, "&cFailed to load target island! Please contact an admin.");
                    return;
                }

                // Wait for pasting to complete
                Thread.sleep(1000);

                // Step 5: Update database (back to main thread)
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    // Update active status
                    addon.getSlotManager().switchSlot(playerUUID, toSlot.getSlotNumber());

                    // Step 6: Teleport player and team members
                    sendProgress(player, "&eTeleporting to new island...");
                    teleportPlayersToSlot(player, toSlot);

                    // Step 7: Send completion message
                    sendSuccess(player, "&aSlot switch complete! Welcome to &e" + toSlot.getSlotName());
                });

            } catch (Exception e) {
                addon.logError("Error during slot switch for " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
                sendError(player, "&cAn error occurred during slot switch. Please contact an admin.");
            }
        });
    }

    /**
     * Save an island to a schematic file
     */
    private boolean saveIslandToSchematic(SlotData slotData) {
        try {
            // Get island from BentoBox
            Island island = addon.getIslands().getIslandById(slotData.getIslandUUID()).orElse(null);
            if (island == null) {
                addon.logError("Island not found for slot: " + slotData.getUniqueId());
                return false;
            }

            // Get island bounds
            Location center = island.getCenter();
            World world = center.getWorld();
            if (world == null) {
                return false;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            // Create WorldEdit region
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

            // Create clipboard
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            clipboard.setOrigin(BlockVector3.at(center.getX(), center.getY(), center.getZ()));

            // Copy blocks to clipboard
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                ForwardExtentCopy copy = new ForwardExtentCopy(
                    editSession, region, clipboard, region.getMinimumPoint()
                );
                copy.setCopyingEntities(true);
                copy.setCopyingBiomes(false);
                Operations.complete(copy);
            }

            // Save to file
            File schematicFile = getSchematicFile(slotData);
            schematicFile.getParentFile().mkdirs();

            try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(schematicFile))) {
                writer.write(clipboard);
            }

            addon.log("Saved island schematic for slot: " + slotData.getUniqueId());
            return true;

        } catch (Exception e) {
            addon.logError("Failed to save schematic for slot " + slotData.getUniqueId() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load a schematic file to the world
     */
    private boolean loadSchematicToWorld(SlotData slotData) {
        try {
            // Get schematic file
            File schematicFile = getSchematicFile(slotData);
            if (!schematicFile.exists()) {
                addon.logError("Schematic file not found for slot: " + slotData.getUniqueId());
                return false;
            }

            // Get island location
            Island island = addon.getIslands().getIslandById(slotData.getIslandUUID()).orElse(null);
            if (island == null) {
                addon.logError("Island not found for slot: " + slotData.getUniqueId());
                return false;
            }

            Location center = island.getCenter();
            World world = center.getWorld();
            if (world == null) {
                return false;
            }

            // Load clipboard from file
            Clipboard clipboard;
            try {
                clipboard = BuiltInClipboardFormat.SPONGE_SCHEMATIC.load(schematicFile);
            } catch (IOException e) {
                addon.logError("Failed to load schematic file: " + e.getMessage());
                return false;
            }

            // Paste to world
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(center.getX(), center.getY(), center.getZ()))
                    .ignoreAirBlocks(false)
                    .build();

                Operations.complete(operation);
            }

            addon.log("Loaded island schematic for slot: " + slotData.getUniqueId());
            return true;

        } catch (Exception e) {
            addon.logError("Failed to load schematic for slot " + slotData.getUniqueId() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Clear island blocks from the world (set to air or void)
     */
    private void clearIslandBlocks(SlotData slotData) {
        try {
            // Get island
            Island island = addon.getIslands().getIslandById(slotData.getIslandUUID()).orElse(null);
            if (island == null) {
                addon.logError("Island not found for clearing: " + slotData.getUniqueId());
                return;
            }

            Location center = island.getCenter();
            World world = center.getWorld();
            if (world == null) {
                return;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            // Create WorldEdit region
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

            // Clear blocks (set to air)
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                // Use replaceBlocks to avoid ambiguity
                for (BlockVector3 pos : region) {
                    editSession.setBlock(pos, BlockTypes.AIR.getDefaultState());
                }
                editSession.flushQueue();
            }

            addon.log("Cleared island blocks for slot: " + slotData.getUniqueId());

        } catch (Exception e) {
            addon.logError("Failed to clear island blocks for slot " + slotData.getUniqueId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Teleport player and team members to the new slot's island
     */
    private void teleportPlayersToSlot(Player owner, SlotData slotData) {
        try {
            // Get island
            Island island = addon.getIslands().getIslandById(slotData.getIslandUUID()).orElse(null);
            if (island == null) {
                addon.logError("Island not found for teleport: " + slotData.getUniqueId());
                return;
            }

            Location spawnPoint = island.getSpawnPoint(org.bukkit.World.Environment.NORMAL);
            if (spawnPoint == null) {
                spawnPoint = island.getCenter();
            }

            // Teleport owner
            owner.teleport(spawnPoint);

            // Teleport team members if online
            for (UUID memberUUID : island.getMemberSet()) {
                if (!memberUUID.equals(owner.getUniqueId())) {
                    Player member = Bukkit.getPlayer(memberUUID);
                    if (member != null && member.isOnline()) {
                        member.teleport(spawnPoint);
                        member.sendMessage(colorize("&eYou have been teleported to the new island slot."));
                    }
                }
            }

            // Handle visitors - teleport them away from the island
            teleportVisitorsAway(island, owner);

        } catch (Exception e) {
            addon.logError("Failed to teleport players for slot " + slotData.getUniqueId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Teleport visitors away from the island during slot switch
     * Visitors are non-team members who are currently on the island
     */
    private void teleportVisitorsAway(Island island, Player owner) {
        try {
            // Get all online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Skip if player is the owner or a team member
                if (player.getUniqueId().equals(owner.getUniqueId()) ||
                    island.getMemberSet().contains(player.getUniqueId())) {
                    continue;
                }

                // Check if player is on the island
                if (island.onIsland(player.getLocation())) {
                    // This is a visitor - teleport them away
                    handleVisitorTeleport(player, island);
                }
            }

        } catch (Exception e) {
            addon.logError("Failed to teleport visitors: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle teleporting a single visitor away from the island
     */
    private void handleVisitorTeleport(Player visitor, Island island) {
        // Get config settings
        int teleportDelay = addon.getSettings().getVisitorTeleportDelay();
        String warningMessage = addon.getSettings().getVisitorWarningMessage();

        // Send warning message if configured
        if (warningMessage != null && !warningMessage.isEmpty()) {
            visitor.sendMessage(colorize(warningMessage));
        }

        // Schedule teleportation after delay
        Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
            // Determine where to teleport the visitor
            Location destination = getVisitorTeleportDestination(visitor);

            if (destination != null) {
                visitor.teleport(destination);
                visitor.sendMessage(colorize("&eYou have been teleported away as the island is switching slots."));
            } else {
                // Fallback: teleport to world spawn
                visitor.teleport(visitor.getWorld().getSpawnLocation());
                visitor.sendMessage(colorize("&eYou have been teleported to spawn as the island is switching slots."));
            }
        }, teleportDelay);
    }

    /**
     * Determine where to teleport a visitor
     * Priority: Their own island > Their team's island > World spawn
     */
    private Location getVisitorTeleportDestination(Player visitor) {
        // Try to get visitor's own island
        Island visitorIsland = addon.getIslands().getIsland(visitor.getWorld(), visitor.getUniqueId());

        if (visitorIsland != null) {
            // Visitor has an island - send them home
            Location spawnPoint = visitorIsland.getSpawnPoint(org.bukkit.World.Environment.NORMAL);
            if (spawnPoint != null) {
                return spawnPoint;
            }
            return visitorIsland.getCenter();
        }

        // Visitor has no island - send to world spawn
        return visitor.getWorld().getSpawnLocation();
    }

    /**
     * Get the schematic file for a slot
     */
    private File getSchematicFile(SlotData slotData) {
        String path = addon.getSlotManager().getSlotSchematicPath(
            slotData.getPlayerUUIDAsUUID(),
            slotData.getSlotNumber()
        );
        return new File(path);
    }

    /**
     * Send progress message to player
     */
    private void sendProgress(Player player, String message) {
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            // Send as chat message for compatibility
            player.sendMessage(colorize(message));
        });
    }

    /**
     * Send error message to player
     */
    private void sendError(Player player, String message) {
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            player.sendMessage(colorize(message));
        });
    }

    /**
     * Send success message to player
     */
    private void sendSuccess(Player player, String message) {
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            player.sendMessage(colorize(message));
        });
    }

    private String colorize(String text) {
        return text.replace("&", "\u00A7");
    }
}
