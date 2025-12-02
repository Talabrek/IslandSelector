package world.bentobox.islandselector.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.api.events.island.IslandCreateEvent;
import world.bentobox.bentobox.api.events.island.IslandCreatedEvent;
import world.bentobox.bentobox.api.events.island.IslandDeleteEvent;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.gui.IslandClaimGUI;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener that intercepts island creation and allows players to choose their location.
 * Also handles island deletion to update the grid.
 */
public class IslandCreateListener implements Listener {

    private final IslandSelector addon;
    private final GridManager gridManager;

    // Track pending island claims (player UUID -> grid coordinate)
    private final Map<UUID, GridCoordinate> pendingClaims = new HashMap<>();

    // Track players who should skip the GUI (already selected a location)
    private final Map<UUID, String> confirmedBlueprints = new HashMap<>();

    public IslandCreateListener(IslandSelector addon) {
        this.addon = addon;
        this.gridManager = addon.getGridManager();
    }

    /**
     * Intercept island creation BEFORE it happens.
     * If player hasn't chosen a location yet, cancel and show grid GUI.
     * If player has chosen a location, let BSkyBlock create it - we'll register it after.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onIslandCreate(IslandCreateEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        if (player == null) {
            return;
        }

        UUID playerUUID = player.getUniqueId();

        // Check if this player has a confirmed claim
        if (pendingClaims.containsKey(playerUUID)) {
            // Player has selected a location - let island creation proceed
            // We'll move it in the IslandCreatedEvent handler
            addon.log("Allowing island creation for " + player.getName() + " - will relocate after creation");
            return;
        }

        // Player hasn't chosen a location yet - cancel creation and show GUI
        event.setCancelled(true);
        addon.log("Cancelled default island creation for " + player.getName() + " - showing grid selection");

        // Open the island claim GUI
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            new IslandClaimGUI(addon, player, IslandCreateListener.this).open();
        });
    }

    /**
     * Handle island creation completion - register in grid
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandCreated(IslandCreatedEvent event) {
        UUID playerUUID = event.getPlayerUUID();
        Player player = Bukkit.getPlayer(playerUUID);

        // Check if this was a grid-based claim
        GridCoordinate coord = pendingClaims.remove(playerUUID);
        confirmedBlueprints.remove(playerUUID);

        if (coord != null) {
            // Register this island in the grid
            Island island = event.getIsland();
            String ownerName = player != null ? player.getName() : "Unknown";

            UUID islandUUID = null;
            try {
                islandUUID = UUID.fromString(island.getUniqueId());
            } catch (IllegalArgumentException e) {
                // Island ID is not a UUID, leave as null
            }

            gridManager.occupyLocation(coord, playerUUID, ownerName, islandUUID);

            addon.log("Registered island at grid " + coord + " for " + ownerName);

            if (player != null) {
                player.sendMessage("§a§lIsland Created!");
                player.sendMessage("§7Your island has been created at location §f" + coord.toString());
            }
        }
    }

    /**
     * Handle island deletion - clear from grid
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandDelete(IslandDeleteEvent event) {
        Island island = event.getIsland();
        if (island == null || island.getCenter() == null) {
            return;
        }

        // Find this island's grid coordinate
        Location center = island.getCenter();
        GridCoordinate coord = gridManager.worldToGrid(center.getBlockX(), center.getBlockZ());

        if (coord != null && gridManager.isWithinBounds(coord)) {
            gridManager.clearLocation(coord);
            addon.log("Cleared grid location " + coord + " due to island deletion");
        }
    }

    /**
     * Called when a player confirms a claim with a blueprint selection
     */
    public void confirmClaimWithBlueprint(Player player, GridCoordinate coord, String blueprintBundleKey) {
        UUID playerUUID = player.getUniqueId();

        // Store the pending claim
        pendingClaims.put(playerUUID, coord);
        confirmedBlueprints.put(playerUUID, blueprintBundleKey);

        addon.log("Player " + player.getName() + " confirmed claim at " + coord +
                 " with blueprint " + blueprintBundleKey);

        // Trigger island creation via BSkyBlock
        // The blueprint selection is handled by BSkyBlock's BlueprintsManager
        player.performCommand("island create " + blueprintBundleKey);
    }

    /**
     * Called when a player confirms a claim without specifying blueprint
     */
    public void confirmClaim(Player player, GridCoordinate coord) {
        UUID playerUUID = player.getUniqueId();

        // Store the pending claim
        pendingClaims.put(playerUUID, coord);

        addon.log("Player " + player.getName() + " confirmed claim at " + coord);

        // Trigger island creation via BSkyBlock
        player.performCommand("island create");
    }

    /**
     * Called when a player selects a location (but hasn't confirmed yet)
     * This is called when they click once on a location
     */
    public void onLocationSelected(Player player, GridCoordinate coord) {
        addon.log("Player " + player.getName() + " selected location " + coord);
        // This doesn't add to pendingClaims - that happens on confirm
    }

    /**
     * Called when a player cancels their claim
     */
    public void cancelClaim(Player player) {
        UUID playerUUID = player.getUniqueId();
        pendingClaims.remove(playerUUID);
        confirmedBlueprints.remove(playerUUID);

        addon.log("Player " + player.getName() + " cancelled their claim");
    }

    /**
     * Check if a player has a pending claim
     */
    public boolean hasPendingClaim(UUID playerUUID) {
        return pendingClaims.containsKey(playerUUID);
    }

    /**
     * Get a player's pending claim coordinate
     */
    public GridCoordinate getPendingClaim(UUID playerUUID) {
        return pendingClaims.get(playerUUID);
    }
}
