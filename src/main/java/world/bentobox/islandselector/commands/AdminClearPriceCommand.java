package world.bentobox.islandselector.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Admin clear price command
 * /islandselector admin clearprice <coord> - Clears the purchase price from a reserved location, making it blocked
 */
public class AdminClearPriceCommand extends CompositeCommand {

    public AdminClearPriceCommand(CompositeCommand parent) {
        super(parent, "clearprice");
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin.clearprice");
        setDescription("commands.islandselector.admin.clearprice.description");
        setParametersHelp("commands.islandselector.admin.clearprice.parameters");
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
            user.sendMessage("commands.islandselector.admin.clearprice.invalid-coord", "[coord]", coordStr);
            return false;
        }

        IslandSelector addon = (IslandSelector) getAddon();
        GridManager gridManager = addon.getGridManager();

        // Check if location is reserved
        if (!gridManager.isReserved(coord)) {
            user.sendMessage("commands.islandselector.admin.clearprice.not-reserved", "[coord]", coordStr);
            return false;
        }

        // Get the current location to check if it has a price
        GridLocation location = gridManager.getGridLocation(coord);
        if (location == null || location.getPurchasePrice() <= 0) {
            user.sendMessage("commands.islandselector.admin.clearprice.no-price", "[coord]", coordStr);
            return false;
        }

        // Clear the price by setting it to 0, which will make the location blocked
        gridManager.setPurchasePrice(coord, 0);
        // Explicitly set it as blocked
        gridManager.reserveLocation(coord, true);

        user.sendMessage("commands.islandselector.admin.clearprice.success", "[coord]", coordStr);
        return true;
    }
}
