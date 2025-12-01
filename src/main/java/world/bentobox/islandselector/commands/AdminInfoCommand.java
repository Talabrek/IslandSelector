package world.bentobox.islandselector.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Admin info command
 * /islandselector admin info <coord> - Shows detailed info about a grid location
 */
public class AdminInfoCommand extends CompositeCommand {

    private final IslandSelector addon;

    public AdminInfoCommand(CompositeCommand parent, IslandSelector addon) {
        super(parent, "info");
        this.addon = addon;
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

        String coordStr = args.get(0).toUpperCase();
        GridCoordinate coord = GridCoordinate.parse(coordStr);

        if (coord == null) {
            user.sendMessage("commands.islandselector.admin.info.invalid-coord", "[coord]", coordStr);
            return false;
        }

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

        // Show world coordinates
        int worldX = coord.getColumn() * addon.getSettings().getIslandSpacing() + addon.getSettings().getGridOriginX();
        int worldZ = coord.getRow() * addon.getSettings().getIslandSpacing() + addon.getSettings().getGridOriginZ();
        user.sendMessage("commands.islandselector.admin.info.world-coords",
            "[x]", String.valueOf(worldX),
            "[z]", String.valueOf(worldZ));

        return true;
    }
}
