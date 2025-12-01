package world.bentobox.islandselector.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Admin reserve command
 * /islandselector admin reserve <coord> - Reserves a grid location
 */
public class AdminReserveCommand extends CompositeCommand {

    private final IslandSelector addon;

    public AdminReserveCommand(CompositeCommand parent, IslandSelector addon) {
        super(parent, "reserve");
        this.addon = addon;
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin.reserve");
        setDescription("commands.islandselector.admin.reserve.description");
        setParametersHelp("commands.islandselector.admin.reserve.parameters");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            showHelp(this, user);
            return false;
        }

        String coordStr = args.get(0).toUpperCase();
        GridCoordinate coord = GridCoordinate.parse(coordStr);

        if (coord == null) {
            user.sendMessage("commands.islandselector.admin.reserve.invalid-coord", "[coord]", coordStr);
            return false;
        }

        GridManager gridManager = addon.getGridManager();
        if (gridManager.isReserved(coord)) {
            user.sendMessage("commands.islandselector.admin.reserve.already-reserved", "[coord]", coordStr);
            return false;
        }

        gridManager.reserveLocation(coord, true); // Reserve as blocked by default
        user.sendMessage("commands.islandselector.admin.reserve.success", "[coord]", coordStr);
        return true;
    }
}
