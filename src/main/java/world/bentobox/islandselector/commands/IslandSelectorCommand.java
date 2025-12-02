package world.bentobox.islandselector.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.gui.MainGridGUI;

/**
 * Main command for IslandSelector addon
 * /islandselector - Opens the grid selection GUI
 */
public class IslandSelectorCommand extends CompositeCommand {

    public IslandSelectorCommand(IslandSelector addon) {
        super(addon, "islandselector", "is", "isgrid");
    }

    @Override
    public void setup() {
        setPermission("islandselector.use");
        setOnlyPlayer(true);
        setDescription("commands.islandselector.description");

        // Add subcommands - they use getAddon() to get addon reference
        new SlotsCommand(this);
        new AdminCommand(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Open the main grid GUI
        IslandSelector addon = (IslandSelector) getAddon();
        new MainGridGUI(addon, user.getPlayer()).open();
        return true;
    }
}
