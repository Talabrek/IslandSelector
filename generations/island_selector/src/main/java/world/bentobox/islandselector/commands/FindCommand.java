package world.bentobox.islandselector.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.gui.MainGridGUI;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Find command - opens grid GUI centered on a specific player's island
 * /islandselector find <player> - Opens grid centered on player's island
 */
public class FindCommand extends CompositeCommand {

    public FindCommand(CompositeCommand parent) {
        super(parent, "find");
    }

    @Override
    public void setup() {
        setPermission("islandselector.find");
        setDescription("commands.islandselector.find.description");
        setParametersHelp("commands.islandselector.find.parameters");
        setOnlyPlayer(true);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            showHelp(this, user);
            return false;
        }

        String targetName = args.get(0);
        IslandSelector addon = (IslandSelector) getAddon();

        // Try to find the player (online or offline)
        UUID targetUUID = getPlayers().getUUID(targetName);

        if (targetUUID == null) {
            user.sendMessage("commands.islandselector.find.player-not-found", "[player]", targetName);
            return false;
        }

        // Get target player's island
        Island island = getIslands().getIsland(getWorld(), targetUUID);

        if (island == null) {
            user.sendMessage("commands.islandselector.find.no-island", "[player]", targetName);
            return false;
        }

        GridManager gridManager = addon.getGridManager();

        // Check if island has a valid center
        Location center = island.getCenter();
        if (center == null) {
            user.sendMessage("commands.islandselector.find.error");
            return false;
        }

        // Convert world coordinates to grid coordinates
        int worldX = center.getBlockX();
        int worldZ = center.getBlockZ();
        GridCoordinate gridCoord = gridManager.worldToGrid(worldX, worldZ);

        if (gridCoord == null) {
            user.sendMessage("commands.islandselector.find.error");
            return false;
        }

        // Open grid GUI centered on the target player's island
        Player player = user.getPlayer();
        if (player == null) {
            user.sendMessage("commands.islandselector.find.error");
            return false;
        }
        MainGridGUI gui = new MainGridGUI(addon, player);
        gui.open();

        // Center the viewport on the found island
        gui.centerViewportOn(gridCoord);

        user.sendMessage("commands.islandselector.find.success",
            "[player]", targetName,
            "[coords]", gridCoord.toString());

        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.size() == 1) {
            // Return list of online player names
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                String name = player.getName();
                if (name.toLowerCase().startsWith(args.get(0).toLowerCase())) {
                    playerNames.add(name);
                }
            }
            return Optional.of(playerNames);
        }
        return Optional.empty();
    }
}
