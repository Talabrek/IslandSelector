package world.bentobox.islandselector.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Admin set price command
 * /islandselector admin setprice <coord> <price> - Sets the price for a reserved location
 */
public class AdminSetPriceCommand extends CompositeCommand {

    private final IslandSelector addon;

    public AdminSetPriceCommand(CompositeCommand parent, IslandSelector addon) {
        super(parent, "setprice");
        this.addon = addon;
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin.reserve");
        setDescription("commands.islandselector.admin.setprice.description");
        setParametersHelp("commands.islandselector.admin.setprice.parameters");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() < 2) {
            showHelp(this, user);
            return false;
        }

        String coordStr = args.get(0).toUpperCase();
        GridCoordinate coord = GridCoordinate.parse(coordStr);

        if (coord == null) {
            user.sendMessage("commands.islandselector.admin.setprice.invalid-coord", "[coord]", coordStr);
            return false;
        }

        double price;
        try {
            price = Double.parseDouble(args.get(1));
            if (price < 0) {
                user.sendMessage("commands.islandselector.admin.setprice.invalid-price");
                return false;
            }
        } catch (NumberFormatException e) {
            user.sendMessage("commands.islandselector.admin.setprice.invalid-price");
            return false;
        }

        GridManager gridManager = addon.getGridManager();

        // If not already reserved, reserve it first
        if (!gridManager.isReserved(coord)) {
            gridManager.reserveLocation(coord, false);
        }

        gridManager.setPurchasePrice(coord, price);
        user.sendMessage("commands.islandselector.admin.setprice.success",
            "[coord]", coordStr,
            "[price]", String.format("%.2f", price));
        return true;
    }
}
