package world.bentobox.islandselector.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Admin unreserve command
 * /islandselector admin unreserve <coord> - Unreserves a grid location
 */
public class AdminUnreserveCommand extends CompositeCommand {

    public AdminUnreserveCommand(CompositeCommand parent) {
        super(parent, "unreserve");
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin.unreserve");
        setDescription("commands.islandselector.admin.unreserve.description");
        setParametersHelp("commands.islandselector.admin.unreserve.parameters");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            showHelp(this, user);
            return false;
        }

        String coordStr = args.get(0);
        GridCoordinate coord = GridCoordinate.parse(coordStr);

        if (coord == null) {
            user.sendMessage("commands.islandselector.admin.unreserve.invalid-coord", "[coord]", coordStr);
            return false;
        }

        IslandSelector addon = (IslandSelector) getAddon();
        GridManager gridManager = addon.getGridManager();

        // Check if location is actually reserved
        if (!gridManager.isReserved(coord)) {
            user.sendMessage("commands.islandselector.admin.unreserve.not-reserved", "[coord]", coordStr);
            return false;
        }

        // Check if location is occupied - cannot unreserve an occupied location
        GridLocation location = gridManager.getGridLocation(coord);
        if (location != null && location.getOwnerUUID() != null) {
            user.sendMessage("commands.islandselector.admin.unreserve.occupied", "[coord]", coordStr);
            return false;
        }

        gridManager.unreserveLocation(coord);
        user.sendMessage("commands.islandselector.admin.unreserve.success", "[coord]", coordStr);
        return true;
    }
}
