package world.bentobox.islandselector.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.gui.NeighborhoodGUI;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Command to open the Neighborhood GUI
 * /islandselector neighbors - Opens the neighborhood view showing your 8 neighbors
 */
public class NeighborsCommand extends CompositeCommand {

    public NeighborsCommand(CompositeCommand parent) {
        super(parent, "neighbors", "neighbourhood", "neighborhood");
    }

    @Override
    public void setup() {
        setPermission("islandselector.use");
        setOnlyPlayer(true);
        setDescription("commands.islandselector.neighbors.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        IslandSelector addon = (IslandSelector) getAddon();
        GridManager gridManager = addon.getGridManager();

        // Check if player has an island
        GridCoordinate playerIsland = gridManager.getPlayerIslandCoordinate(user.getUniqueId());

        if (playerIsland == null) {
            user.sendMessage("&cYou don't have an island yet!");
            user.sendMessage("&7Create an island first with /island create");
            return false;
        }

        // Open the Neighborhood GUI
        new NeighborhoodGUI(addon, user.getPlayer()).open();
        return true;
    }
}
