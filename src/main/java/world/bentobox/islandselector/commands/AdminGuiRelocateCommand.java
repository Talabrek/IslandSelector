package world.bentobox.islandselector.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.gui.AdminGridGUI;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.List;
import java.util.UUID;

/**
 * Internal command used by the admin GUI to enter relocation mode.
 * /islandselector admin gui-relocate <source-coord> <player-name>
 *
 * This command opens the admin GUI in "relocation mode" where the admin
 * can click on an available location to relocate the player's island.
 */
public class AdminGuiRelocateCommand extends CompositeCommand {

    public AdminGuiRelocateCommand(CompositeCommand parent) {
        super(parent, "gui-relocate");
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin.relocate");
        setOnlyPlayer(true);
        // Hidden command - not shown in help
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        IslandSelector addon = (IslandSelector) getAddon();

        // Check if FAWE is available
        if (!addon.isSchematicOperationsAvailable()) {
            user.sendMessage("&cThis feature requires FastAsyncWorldEdit (FAWE) to be installed.");
            return false;
        }

        if (args.size() < 2) {
            return false;
        }

        String coordStr = args.get(0);
        String playerName = args.get(1);

        // Parse source coordinate
        GridCoordinate sourceCoord = GridCoordinate.parse(coordStr);
        if (sourceCoord == null) {
            user.sendMessage("§cInvalid coordinate format.");
            return false;
        }
        GridManager gridManager = addon.getGridManager();

        // Verify the source location is occupied
        GridLocation.Status status = gridManager.getLocationStatus(sourceCoord);
        if (status != GridLocation.Status.OCCUPIED) {
            user.sendMessage("§cLocation " + sourceCoord + " is not occupied.");
            return false;
        }

        // Get the location info
        GridLocation location = gridManager.getGridLocation(sourceCoord);
        if (location == null || location.getOwnerUUID() == null) {
            user.sendMessage("§cCould not find island owner information.");
            return false;
        }

        UUID playerUUID = location.getOwnerUUID();
        String ownerName = location.getOwnerName() != null ? location.getOwnerName() : playerName;

        // Note: We allow relocation of offline players - the admin method handles this

        // Open admin GUI in relocation mode
        Player player = user.getPlayer();
        AdminGridGUI gui = new AdminGridGUI(addon, player);
        gui.open();
        gui.enterRelocationMode(sourceCoord, playerUUID, ownerName);

        user.sendMessage("§d§lRelocation Mode Active");
        user.sendMessage("§7Click on an §aavailable §7location to move §f" + ownerName + "§7's island.");
        user.sendMessage("§7Click §c[Cancel] §7or shift-click to exit relocation mode.");

        return true;
    }
}
