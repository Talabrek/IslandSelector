package world.bentobox.islandselector.commands;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.managers.SlotManager;
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Admin info command
 * /islandselector admin info <coord> - Shows detailed info about a grid location
 * /islandselector admin info <player> - Shows detailed info about a player's islands
 */
public class AdminInfoCommand extends CompositeCommand {

    public AdminInfoCommand(CompositeCommand parent) {
        super(parent, "info");
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin.info");
        setDescription("commands.islandselector.admin.info.description");
        setParametersHelp("commands.islandselector.admin.info.parameters");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            showHelp(this, user);
            return false;
        }

        String input = args.get(0);

        // Try to parse as coordinate first
        GridCoordinate coord = GridCoordinate.parse(input);

        if (coord != null) {
            // It's a coordinate - show location info
            return showLocationInfo(user, coord, input);
        } else {
            // It's a player name - show player info
            return showPlayerInfo(user, input);
        }
    }

    /**
     * Show information about a grid location
     */
    private boolean showLocationInfo(User user, GridCoordinate coord, String coordStr) {
        IslandSelector addon = (IslandSelector) getAddon();
        GridManager gridManager = addon.getGridManager();
        GridLocation location = gridManager.getGridLocation(coord);

        user.sendMessage("commands.islandselector.admin.info.header", "[coord]", coordStr);

        if (location == null) {
            user.sendMessage("commands.islandselector.admin.info.empty");
        } else {
            user.sendMessage("commands.islandselector.admin.info.status",
                "[status]", location.getStatus().toString());

            if (location.getOwnerUUID() != null) {
                user.sendMessage("commands.islandselector.admin.info.owner",
                    "[owner]", location.getOwnerName());
            }

            if (location.isReserved()) {
                if (location.isPurchasable()) {
                    user.sendMessage("commands.islandselector.admin.info.purchasable",
                        "[price]", String.format("%.2f", location.getPurchasePrice()));
                } else {
                    user.sendMessage("commands.islandselector.admin.info.blocked");
                }
            }
        }

        // Show world coordinates (grid 0,0 = world 0,0)
        int worldX = coord.getX() * addon.getIslandSpacing();
        int worldZ = coord.getZ() * addon.getIslandSpacing();
        user.sendMessage("commands.islandselector.admin.info.world-coords",
            "[x]", String.valueOf(worldX),
            "[z]", String.valueOf(worldZ));

        return true;
    }

    /**
     * Show information about a player's islands and slots
     */
    private boolean showPlayerInfo(User user, String playerName) {
        IslandSelector addon = (IslandSelector) getAddon();
        SlotManager slotManager = addon.getSlotManager();

        // Get player UUID from name
        @SuppressWarnings("deprecation")
        UUID playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();

        if (playerUUID == null) {
            user.sendMessage("commands.islandselector.admin.info.player-not-found", "[player]", playerName);
            return false;
        }

        // Get player's slots
        List<SlotData> slots = slotManager.getPlayerSlots(playerUUID);

        if (slots.isEmpty()) {
            user.sendMessage("commands.islandselector.admin.info.player-no-data", "[player]", playerName);
            return true;
        }

        // Header
        user.sendMessage("commands.islandselector.admin.info.player-header", "[player]", playerName);

        // Grid location
        String gridCoord = slotManager.getPlayerGridCoordinate(playerUUID);
        if (gridCoord != null) {
            user.sendMessage("commands.islandselector.admin.info.player-location", "[location]", gridCoord);

            // World coordinates
            GridCoordinate coord = GridCoordinate.parse(gridCoord);
            if (coord != null) {
                int worldX = coord.getX() * addon.getIslandSpacing();
                int worldZ = coord.getZ() * addon.getIslandSpacing();
                user.sendMessage("commands.islandselector.admin.info.player-world-coords",
                    "[x]", String.valueOf(worldX),
                    "[z]", String.valueOf(worldZ));
            }
        } else {
            user.sendMessage("commands.islandselector.admin.info.player-no-island");
        }

        // Active slot
        SlotData activeSlot = slotManager.getActiveSlot(playerUUID);
        if (activeSlot != null) {
            user.sendMessage("commands.islandselector.admin.info.player-active-slot",
                "[slot]", String.valueOf(activeSlot.getSlotNumber()),
                "[name]", activeSlot.getSlotName() != null ? activeSlot.getSlotName() : "Slot " + activeSlot.getSlotNumber());
        }

        // Total slots info
        int totalSlots = slots.size();
        int islandCount = slotManager.getIslandCount(playerUUID);
        user.sendMessage("commands.islandselector.admin.info.player-slot-count",
            "[used]", String.valueOf(islandCount),
            "[total]", String.valueOf(totalSlots));

        // List all slots
        user.sendMessage("commands.islandselector.admin.info.player-slots-list");
        for (SlotData slot : slots) {
            String slotStatus = slot.isActive() ? "ACTIVE" : "inactive";
            String slotName = slot.getSlotName() != null ? slot.getSlotName() : "Slot " + slot.getSlotNumber();
            String hasIsland = slot.hasIsland() ? "Has Island" : "Empty";

            user.sendMessage("commands.islandselector.admin.info.player-slot-entry",
                "[number]", String.valueOf(slot.getSlotNumber()),
                "[name]", slotName,
                "[status]", slotStatus,
                "[island]", hasIsland);
        }

        // Cooldown status
        long remainingCooldown = slotManager.getRemainingCooldown(playerUUID);
        if (remainingCooldown > 0) {
            long seconds = remainingCooldown / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            String cooldownStr;
            if (days > 0) {
                cooldownStr = days + "d " + (hours % 24) + "h";
            } else if (hours > 0) {
                cooldownStr = hours + "h " + (minutes % 60) + "m";
            } else if (minutes > 0) {
                cooldownStr = minutes + "m " + (seconds % 60) + "s";
            } else {
                cooldownStr = seconds + "s";
            }

            user.sendMessage("commands.islandselector.admin.info.player-cooldown",
                "[time]", cooldownStr);
        } else {
            user.sendMessage("commands.islandselector.admin.info.player-no-cooldown");
        }

        return true;
    }
}
