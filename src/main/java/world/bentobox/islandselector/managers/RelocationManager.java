package world.bentobox.islandselector.managers;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.RelocationData;
import world.bentobox.islandselector.events.IslandRelocateEvent;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.UUID;

/**
 * Manager for handling island relocation operations
 */
public class RelocationManager {

    private final IslandSelector addon;
    private final Database<RelocationData> database;
    private Economy economy;

    public RelocationManager(IslandSelector addon) {
        this.addon = addon;
        this.database = new Database<>(addon, RelocationData.class);
        setupEconomy();
    }

    /**
     * Setup Vault economy if available
     */
    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            addon.log("Vault not found - relocation costs will be disabled");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            addon.log("Vault economy provider not found");
            return;
        }

        economy = rsp.getProvider();
        addon.log("Vault economy hooked successfully");
    }

    /**
     * Check if a player can relocate (cooldown and permissions)
     */
    public boolean canRelocate(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            return false;
        }

        // Check bypass permission
        if (player.hasPermission("islandselector.bypass.cooldown.relocation")) {
            return true;
        }

        // Check cooldown
        RelocationData data = getRelocationData(playerUUID);
        return !data.isOnCooldown(addon.getSettings().getRelocationCooldown());
    }

    /**
     * Get remaining cooldown time for a player
     */
    public long getRemainingCooldown(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.hasPermission("islandselector.bypass.cooldown.relocation")) {
            return 0;
        }

        RelocationData data = getRelocationData(playerUUID);
        return data.getRemainingCooldown(addon.getSettings().getRelocationCooldown());
    }

    /**
     * Get relocation data for a player (creates new if doesn't exist)
     */
    public RelocationData getRelocationData(UUID playerUUID) {
        String uuid = playerUUID.toString();
        RelocationData data = database.loadObject(uuid);
        if (data == null) {
            data = new RelocationData(uuid);
        }
        return data;
    }

    /**
     * Check if a player can afford the relocation cost
     */
    public boolean canAffordRelocation(Player player) {
        double cost = addon.getSettings().getRelocationCost();
        if (cost <= 0) {
            return true; // Free relocation
        }

        // Check bypass permission
        if (player.hasPermission("islandselector.bypass.cost.relocation")) {
            return true;
        }

        // Check economy
        if (economy == null) {
            return true; // No economy, consider it free
        }

        return economy.has(player, cost);
    }

    /**
     * Charge the player for relocation
     */
    public boolean chargePlayer(Player player) {
        double cost = addon.getSettings().getRelocationCost();
        if (cost <= 0) {
            return true; // Free relocation
        }

        // Check bypass permission
        if (player.hasPermission("islandselector.bypass.cost.relocation")) {
            return true;
        }

        // Charge via economy
        if (economy == null) {
            return true; // No economy, consider it free
        }

        if (!economy.has(player, cost)) {
            return false;
        }

        economy.withdrawPlayer(player, cost);
        return true;
    }

    /**
     * Perform island relocation asynchronously
     */
    public void relocateIsland(Player player, GridCoordinate fromCoord, GridCoordinate toCoord) {
        UUID playerUUID = player.getUniqueId();

        // Calculate cost and world coordinates for the event
        double cost = addon.getSettings().getRelocationCost();
        boolean bypassCost = player.hasPermission("islandselector.bypass.cost.relocate");
        if (bypassCost) {
            cost = 0;
        }

        int fromWorldX = calculateWorldX(fromCoord);
        int fromWorldZ = calculateWorldZ(fromCoord);
        int toWorldX = calculateWorldX(toCoord);
        int toWorldZ = calculateWorldZ(toCoord);

        // Fire IslandRelocateEvent on main thread BEFORE starting the async operation
        IslandRelocateEvent event = new IslandRelocateEvent(
            player,
            fromCoord,
            toCoord,
            fromWorldX,
            fromWorldZ,
            toWorldX,
            toWorldZ,
            cost
        );

        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            Bukkit.getPluginManager().callEvent(event);

            // Check if event was cancelled
            if (event.isCancelled()) {
                addon.log("IslandRelocateEvent cancelled for " + player.getName() +
                         " from " + fromCoord + " to " + toCoord);
                if (event.getCancellationReason() != null) {
                    player.sendMessage("§c" + event.getCancellationReason());
                } else {
                    player.sendMessage("§cIsland relocation cancelled.");
                }
                return;
            }

            // Event not cancelled - proceed with relocation asynchronously
            performRelocationAsync(player, fromCoord, toCoord);
        });
    }

    /**
     * Internal method to perform the actual relocation asynchronously
     */
    private void performRelocationAsync(Player player, GridCoordinate fromCoord, GridCoordinate toCoord) {
        UUID playerUUID = player.getUniqueId();

        // Run asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            try {
                // Step 1: Get the island
                sendProgress(player, "&eStarting island relocation...");

                Island island = addon.getIslands().getIsland(player.getWorld(), playerUUID);
                if (island == null) {
                    sendError(player, "&cCould not find your island!");
                    return;
                }

                Location oldCenter = island.getCenter();
                if (oldCenter == null || oldCenter.getWorld() == null) {
                    sendError(player, "&cInvalid island location!");
                    return;
                }

                // Step 2: Save island as schematic
                sendProgress(player, "&eSaving island...");
                Clipboard clipboard = saveIslandToClipboard(island, oldCenter);
                if (clipboard == null) {
                    sendError(player, "&cFailed to save island!");
                    return;
                }

                // Step 3: Calculate new center location
                int newWorldX = calculateWorldX(toCoord);
                int newWorldZ = calculateWorldZ(toCoord);
                Location newCenter = new Location(
                    oldCenter.getWorld(),
                    newWorldX,
                    oldCenter.getY(),
                    newWorldZ,
                    oldCenter.getYaw(),
                    oldCenter.getPitch()
                );

                // Step 4: Clear old location
                sendProgress(player, "&eClearing old location...");
                clearIslandBlocks(island, oldCenter);
                Thread.sleep(1000); // Wait for clearing

                // Step 5: Paste island at new location
                sendProgress(player, "&ePasting island at new location...");
                pasteIslandFromClipboard(clipboard, newCenter);
                Thread.sleep(1000); // Wait for pasting

                // Step 6: Update BSkyBlock island data (on main thread)
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    sendProgress(player, "&eUpdating island data...");

                    // Update island center and spawn
                    island.setCenter(newCenter);
                    island.setSpawnPoint(org.bukkit.World.Environment.NORMAL, newCenter);

                    // Update grid location
                    addon.getGridManager().clearLocation(fromCoord);

                    UUID islandUUID = null;
                    try {
                        islandUUID = UUID.fromString(island.getUniqueId());
                    } catch (IllegalArgumentException e) {
                        // Island ID is not a UUID
                    }

                    addon.getGridManager().occupyLocation(toCoord, playerUUID, player.getName(), islandUUID);

                    // Update relocation data
                    RelocationData data = getRelocationData(playerUUID);
                    data.recordRelocation(fromCoord.toString(), toCoord.toString());
                    database.saveObjectAsync(data);

                    // Step 7: Teleport players
                    sendProgress(player, "&eTeleporting...");
                    teleportPlayers(island, player, newCenter);

                    // Step 8: Complete
                    sendSuccess(player, "&a&lIsland relocated successfully!");
                    player.sendMessage(colorize("&7From: &f" + fromCoord.toString() + " &7→ &f" + toCoord.toString()));
                    player.sendMessage(colorize("&7New coordinates: &fX: " + newWorldX + ", Z: " + newWorldZ));
                });

            } catch (Exception e) {
                addon.logError("Error during island relocation for " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
                sendError(player, "&cAn error occurred during relocation. Please contact an admin.");
            }
        });
    }

    /**
     * Save island to a WorldEdit clipboard
     */
    private Clipboard saveIslandToClipboard(Island island, Location center) {
        try {
            World world = center.getWorld();
            if (world == null) {
                return null;
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
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            clipboard.setOrigin(BlockVector3.at(center.getX(), center.getY(), center.getZ()));

            // Copy blocks to clipboard
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                ForwardExtentCopy copy = new ForwardExtentCopy(
                    editSession, region, clipboard, region.getMinimumPoint()
                );
                copy.setCopyingEntities(true);
                copy.setCopyingBiomes(true);
                Operations.complete(copy);
            }

            addon.log("Saved island to clipboard for relocation");
            return clipboard;

        } catch (Exception e) {
            addon.logError("Failed to save island to clipboard: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Paste island from clipboard to new location
     */
    private void pasteIslandFromClipboard(Clipboard clipboard, Location newCenter) {
        try {
            World world = newCenter.getWorld();
            if (world == null) {
                return;
            }

            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(newCenter.getX(), newCenter.getY(), newCenter.getZ()))
                    .ignoreAirBlocks(false)
                    .copyEntities(true)
                    .copyBiomes(true)
                    .build();

                Operations.complete(operation);
            }

            addon.log("Pasted island to new location");

        } catch (Exception e) {
            addon.logError("Failed to paste island: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Clear island blocks from old location
     */
    private void clearIslandBlocks(Island island, Location center) {
        try {
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
                for (BlockVector3 pos : region) {
                    editSession.setBlock(pos, BlockTypes.AIR.getDefaultState());
                }
                editSession.flushQueue();
            }

            addon.log("Cleared island blocks at old location");

        } catch (Exception e) {
            addon.logError("Failed to clear island blocks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Teleport player and team members to new island location
     */
    private void teleportPlayers(Island island, Player owner, Location newCenter) {
        try {
            // Teleport owner
            owner.teleport(newCenter);

            // Teleport team members if online
            for (UUID memberUUID : island.getMemberSet()) {
                if (!memberUUID.equals(owner.getUniqueId())) {
                    Player member = Bukkit.getPlayer(memberUUID);
                    if (member != null && member.isOnline()) {
                        member.teleport(newCenter);
                        member.sendMessage(colorize("&eYour island has been relocated to a new location!"));
                    }
                }
            }

            // Handle visitors - teleport them away
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getUniqueId().equals(owner.getUniqueId()) ||
                    island.getMemberSet().contains(player.getUniqueId())) {
                    continue;
                }

                // Check if player is on the island (now cleared)
                Location loc = player.getLocation();
                double distance = loc.distance(island.getCenter());
                int protectionRange = island.getProtectionRange();

                if (distance < protectionRange) {
                    // Teleport to spawn
                    player.teleport(loc.getWorld().getSpawnLocation());
                    player.sendMessage(colorize("&eThe island you were visiting has been relocated."));
                }
            }

        } catch (Exception e) {
            addon.logError("Failed to teleport players: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Calculate world X coordinate from grid coordinate
     */
    private int calculateWorldX(GridCoordinate coord) {
        int spacing = addon.getIslandSpacing() * 2;
        return coord.getX() * spacing;
    }

    /**
     * Calculate world Z coordinate from grid coordinate
     */
    private int calculateWorldZ(GridCoordinate coord) {
        int spacing = addon.getIslandSpacing() * 2;
        return coord.getZ() * spacing;
    }

    /**
     * Reset a player's relocation cooldown (admin command)
     */
    public void resetCooldown(UUID playerUUID) {
        RelocationData data = getRelocationData(playerUUID);
        data.setLastRelocationTime(0);
        database.saveObjectAsync(data);
    }

    /**
     * Format cooldown time as a human-readable string
     */
    public String formatCooldownTime(long seconds) {
        if (seconds <= 0) {
            return "Ready";
        }

        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (secs > 0 || sb.length() == 0) {
            sb.append(secs).append("s");
        }

        return sb.toString().trim();
    }

    // Helper methods for sending messages
    private void sendProgress(Player player, String message) {
        Bukkit.getScheduler().runTask(addon.getPlugin(), () ->
            player.sendMessage(colorize(message))
        );
    }

    private void sendSuccess(Player player, String message) {
        Bukkit.getScheduler().runTask(addon.getPlugin(), () ->
            player.sendMessage(colorize(message))
        );
    }

    private void sendError(Player player, String message) {
        Bukkit.getScheduler().runTask(addon.getPlugin(), () ->
            player.sendMessage(colorize(message))
        );
    }

    private String colorize(String text) {
        return text.replace("&", "\u00A7");
    }
}
