package world.bentobox.islandselector.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.Settings;

/**
 * Admin expand command
 * /islandselector admin expand <direction> <amount> - Expands the grid in a direction
 */
public class AdminExpandCommand extends CompositeCommand {

    public AdminExpandCommand(CompositeCommand parent) {
        super(parent, "expand");
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin.expand");
        setDescription("commands.islandselector.admin.expand.description");
        setParametersHelp("commands.islandselector.admin.expand.parameters");
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
            user.sendMessage("commands.islandselector.admin.expand.invalid-amount", "[amount]", args.get(1));
            return false;
        }

        if (amount <= 0) {
            user.sendMessage("commands.islandselector.admin.expand.invalid-amount", "[amount]", args.get(1));
            return false;
        }

        IslandSelector addon = (IslandSelector) getAddon();
        Settings settings = addon.getSettings();

        // Get current boundaries
        int oldMinX = settings.getGridMinX();
        int oldMaxX = settings.getGridMaxX();
        int oldMinZ = settings.getGridMinZ();
        int oldMaxZ = settings.getGridMaxZ();

        // Expand based on direction
        switch (direction) {
            case "east":
                settings.setGridMaxX(oldMaxX + amount);
                break;
            case "west":
                settings.setGridMinX(oldMinX - amount);
                break;
            case "south":
                settings.setGridMaxZ(oldMaxZ + amount);
                break;
            case "north":
                settings.setGridMinZ(oldMinZ - amount);
                break;
            default:
                user.sendMessage("commands.islandselector.admin.expand.invalid-direction", "[direction]", direction);
                return false;
        }

        // Save config to persist changes
        addon.saveSettings();

        // Send success message
        int newWidth = settings.getGridWidth();
        int newHeight = settings.getGridHeight();
        user.sendMessage("commands.islandselector.admin.expand.success",
            "[direction]", direction,
            "[amount]", String.valueOf(amount),
            "[width]", String.valueOf(newWidth),
            "[height]", String.valueOf(newHeight));

        return true;
    }
}
