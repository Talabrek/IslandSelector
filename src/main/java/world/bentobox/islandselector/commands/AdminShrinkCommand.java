package world.bentobox.islandselector.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.Settings;
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Admin shrink command
 * /islandselector admin shrink <direction> <amount> - Shrinks the grid in a direction
 * Only allowed if no islands exist in the area being removed
 */
public class AdminShrinkCommand extends CompositeCommand {

    public AdminShrinkCommand(CompositeCommand parent) {
        super(parent, "shrink");
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin.expand");
        setDescription("commands.islandselector.admin.shrink.description");
        setParametersHelp("commands.islandselector.admin.shrink.parameters");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() < 2) {
            showHelp(this, user);
            return false;
        }

        String direction = args.get(0).toLowerCase();
        int amount;

        try {
            amount = Integer.parseInt(args.get(1));
        } catch (NumberFormatException e) {
            user.sendMessage("commands.islandselector.admin.shrink.invalid-amount", "[amount]", args.get(1));
            return false;
        }

        if (amount <= 0) {
            user.sendMessage("commands.islandselector.admin.shrink.invalid-amount", "[amount]", args.get(1));
            return false;
        }

        IslandSelector addon = (IslandSelector) getAddon();
        Settings settings = addon.getSettings();
        GridManager gridManager = addon.getGridManager();

        // Get current boundaries
        int oldMinX = settings.getGridMinX();
        int oldMaxX = settings.getGridMaxX();
        int oldMinZ = settings.getGridMinZ();
        int oldMaxZ = settings.getGridMaxZ();

        // Calculate new boundaries and affected area
        int newMinX = oldMinX;
        int newMaxX = oldMaxX;
        int newMinZ = oldMinZ;
        int newMaxZ = oldMaxZ;

        // Determine new boundaries based on direction
        switch (direction) {
            case "east":
                newMaxX = oldMaxX - amount;
                if (newMaxX < newMinX) {
                    user.sendMessage("commands.islandselector.admin.shrink.too-small");
                    return false;
                }
                break;
            case "west":
                newMinX = oldMinX + amount;
                if (newMinX > newMaxX) {
                    user.sendMessage("commands.islandselector.admin.shrink.too-small");
                    return false;
                }
                break;
            case "south":
                newMaxZ = oldMaxZ - amount;
                if (newMaxZ < newMinZ) {
                    user.sendMessage("commands.islandselector.admin.shrink.too-small");
                    return false;
                }
                break;
            case "north":
                newMinZ = oldMinZ + amount;
                if (newMinZ > newMaxZ) {
                    user.sendMessage("commands.islandselector.admin.shrink.too-small");
                    return false;
                }
                break;
            default:
                user.sendMessage("commands.islandselector.admin.shrink.invalid-direction", "[direction]", direction);
                return false;
        }

        // Check if any islands exist in the area that would be removed
        int islandsInArea = 0;
        for (GridLocation location : gridManager.getAllLocations()) {
            if (location.getStatus() == GridLocation.Status.OCCUPIED) {
                GridCoordinate coord = location.getCoordinate();
                int x = coord.getX();
                int z = coord.getZ();

                // Check if this island is in the area being removed
                boolean inRemovedArea = false;
                switch (direction) {
                    case "east":
                        // Removing columns > newMaxX
                        if (x > newMaxX) {
                            inRemovedArea = true;
                        }
                        break;
                    case "west":
                        // Removing columns < newMinX
                        if (x < newMinX) {
                            inRemovedArea = true;
                        }
                        break;
                    case "south":
                        // Removing rows > newMaxZ
                        if (z > newMaxZ) {
                            inRemovedArea = true;
                        }
                        break;
                    case "north":
                        // Removing rows < newMinZ
                        if (z < newMinZ) {
                            inRemovedArea = true;
                        }
                        break;
                }

                if (inRemovedArea) {
                    islandsInArea++;
                }
            }
        }

        // If islands exist in the affected area, prevent shrinking
        if (islandsInArea > 0) {
            user.sendMessage("commands.islandselector.admin.shrink.islands-in-area",
                "[count]", String.valueOf(islandsInArea));
            return false;
        }

        // Safe to shrink - update boundaries
        settings.setGridMinX(newMinX);
        settings.setGridMaxX(newMaxX);
        settings.setGridMinZ(newMinZ);
        settings.setGridMaxZ(newMaxZ);

        // Save config to persist changes
        addon.saveSettings();

        // Send success message
        int newWidth = settings.getGridWidth();
        int newHeight = settings.getGridHeight();
        user.sendMessage("commands.islandselector.admin.shrink.success",
            "[direction]", direction,
            "[amount]", String.valueOf(amount),
            "[width]", String.valueOf(newWidth),
            "[height]", String.valueOf(newHeight));

        return true;
    }
}
