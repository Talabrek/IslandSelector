package world.bentobox.islandselector.commands;

import java.util.List;

import org.bukkit.World;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Locate command - shows player's grid coordinates in chat
 * /islandselector locate - Displays grid location and world coordinates
 */
public class LocateCommand extends CompositeCommand {

    public LocateCommand(CompositeCommand parent) {
        super(parent, "locate");
    }

    @Override
    public void setup() {
        setPermission("islandselector.locate");
        setDescription("commands.islandselector.locate.description");
        setOnlyPlayer(true);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        IslandSelector addon = (IslandSelector) getAddon();

        // Get player's island from BSkyBlock
        Island island = getIslands().getIsland(getWorld(), user);

        if (island == null) {
            user.sendMessage("commands.islandselector.locate.no-island");
            return false;
        }
        GridManager gridManager = addon.getGridManager();

        // Convert world coordinates to grid coordinates
        int worldX = island.getCenter().getBlockX();
        int worldZ = island.getCenter().getBlockZ();
        GridCoordinate gridCoord = gridManager.worldToGrid(worldX, worldZ);

        if (gridCoord == null) {
            user.sendMessage("commands.islandselector.locate.error");
            return false;
        }

        // Send location information to player
        user.sendMessage("commands.islandselector.locate.header");
        user.sendMessage("commands.islandselector.locate.grid-location",
            "[coords]", gridCoord.toString());
        user.sendMessage("commands.islandselector.locate.world-coords",
            "[x]", String.valueOf(worldX),
            "[z]", String.valueOf(worldZ));

        return true;
    }
}
