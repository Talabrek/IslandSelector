package world.bentobox.islandselector.integrations;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.managers.SlotManager;
import world.bentobox.islandselector.managers.SlotSwitchManager;
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * PlaceholderAPI integration for IslandSelector
 *
 * Provides placeholders:
 * - %islandselector_location% - Player's grid coordinates (e.g., "Q17")
 * - %islandselector_slot_active_name% - Active slot name
 * - %islandselector_cooldown_switch% - Slot switch cooldown (formatted time or "Ready")
 * - %islandselector_available% - Count of available grid locations
 * - %islandselector_neighbors_online% - Count of online neighbors
 */
public class PlaceholderAPIIntegration extends PlaceholderExpansion {

    private final IslandSelector addon;
    private final GridManager gridManager;
    private final SlotManager slotManager;
    private final SlotSwitchManager switchManager;

    public PlaceholderAPIIntegration(IslandSelector addon) {
        this.addon = addon;
        this.gridManager = addon.getGridManager();
        this.slotManager = addon.getSlotManager();
        this.switchManager = addon.getSlotSwitchManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "islandselector";
    }

    @Override
    public @NotNull String getAuthor() {
        return addon.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return addon.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Keep registered after reload
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        UUID playerUUID = player.getUniqueId();

        // %islandselector_location%
        if (identifier.equals("location")) {
            return getPlayerLocation(playerUUID);
        }

        // %islandselector_slot_active_name%
        if (identifier.equals("slot_active_name")) {
            return getActiveSlotName(playerUUID);
        }

        // %islandselector_cooldown_switch%
        if (identifier.equals("cooldown_switch")) {
            return getSwitchCooldown(playerUUID);
        }

        // %islandselector_available%
        if (identifier.equals("available")) {
            return String.valueOf(getAvailableLocationsCount());
        }

        // %islandselector_neighbors_online%
        if (identifier.equals("neighbors_online")) {
            return String.valueOf(getOnlineNeighborsCount(playerUUID));
        }

        // Return empty string for unknown placeholders to prevent NPE in some PAPI versions
        return "";
    }

    /**
     * Get player's island grid location
     * @return Grid coordinate (e.g., "Q17") or "None"
     */
    private String getPlayerLocation(UUID playerUUID) {
        World bskyWorld = gridManager.getBSkyBlockWorld();
        if (bskyWorld == null) {
            return "None";
        }

        Island island = addon.getIslands().getIsland(bskyWorld, playerUUID);
        if (island == null) {
            return "None";
        }

        Location center = island.getCenter();
        if (center == null) {
            return "Unknown";
        }

        GridCoordinate coord = gridManager.worldToGrid(center.getBlockX(), center.getBlockZ());
        if (coord == null) {
            return "Unknown";
        }

        return coord.toString();
    }

    /**
     * Get active slot name
     * @return Slot name or "No Slot"
     */
    private String getActiveSlotName(UUID playerUUID) {
        SlotData activeSlot = slotManager.getActiveSlot(playerUUID);
        if (activeSlot == null) {
            return "No Slot";
        }

        String name = activeSlot.getSlotName();
        if (name == null || name.isEmpty()) {
            return "Slot " + activeSlot.getSlotNumber();
        }

        return name;
    }

    /**
     * Get slot switch cooldown
     * @return Formatted time (e.g., "45m 30s") or "Ready"
     */
    private String getSwitchCooldown(UUID playerUUID) {
        SlotData activeSlot = slotManager.getActiveSlot(playerUUID);
        if (activeSlot == null) {
            return "Ready";
        }

        long lastSwitch = activeSlot.getLastSwitchTime();
        if (lastSwitch == 0) {
            return "Ready";
        }

        int cooldownSeconds = addon.getSettings().getSwitchCooldown();
        long cooldownMs = cooldownSeconds * 1000L;
        long elapsed = System.currentTimeMillis() - lastSwitch;

        if (elapsed >= cooldownMs) {
            return "Ready";
        }

        long remainingMs = cooldownMs - elapsed;
        return formatTime(remainingMs);
    }

    /**
     * Get count of available (empty) grid locations
     * @return Number of available locations
     */
    private int getAvailableLocationsCount() {
        int totalTracked = gridManager.getTrackedLocationCount();
        int occupied = gridManager.getOccupiedLocationCount();
        return totalTracked - occupied;
    }

    /**
     * Get count of online neighbors
     * @return Number of online neighbors (0-8)
     */
    private int getOnlineNeighborsCount(UUID playerUUID) {
        Island island = addon.getIslands().getIsland(gridManager.getBSkyBlockWorld(), playerUUID);
        if (island == null) {
            return 0;
        }

        Location center = island.getCenter();
        if (center == null) {
            return 0;
        }

        GridCoordinate playerCoord = gridManager.worldToGrid(center.getBlockX(), center.getBlockZ());
        if (playerCoord == null) {
            return 0;
        }

        int onlineCount = 0;

        // Check all 8 neighboring positions
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue; // Skip self

                GridCoordinate neighborCoord = new GridCoordinate(
                    playerCoord.getX() + dx,
                    playerCoord.getZ() + dz
                );

                GridLocation neighborLocation = gridManager.getGridLocation(neighborCoord);
                if (neighborLocation != null && neighborLocation.getOwnerUUID() != null) {
                    if (Bukkit.getPlayer(neighborLocation.getOwnerUUID()) != null) {
                        onlineCount++;
                    }
                }
            }
        }

        return onlineCount;
    }

    /**
     * Format milliseconds into human-readable time
     * @param ms Milliseconds
     * @return Formatted time (e.g., "1h 30m", "45m 30s", "5s")
     */
    private String formatTime(long ms) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        long hours = TimeUnit.MILLISECONDS.toHours(ms);
        long days = TimeUnit.MILLISECONDS.toDays(ms);

        if (days > 0) {
            long remainingHours = hours % 24;
            if (remainingHours > 0) {
                return days + "d " + remainingHours + "h";
            }
            return days + "d";
        } else if (hours > 0) {
            long remainingMinutes = minutes % 60;
            if (remainingMinutes > 0) {
                return hours + "h " + remainingMinutes + "m";
            }
            return hours + "h";
        } else if (minutes > 0) {
            long remainingSeconds = seconds % 60;
            if (remainingSeconds > 0) {
                return minutes + "m " + remainingSeconds + "s";
            }
            return minutes + "m";
        } else {
            return seconds + "s";
        }
    }
}
