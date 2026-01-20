package world.bentobox.islandselector.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        new SwitchCommand(this);
        new SetNameCommand(this);
        new SetIconCommand(this);
        new DeleteCommand(this);
        new LocateCommand(this);
        new FindCommand(this);
        new NeighborsCommand(this);
        new HelpCommand(this);
        new AdminCommand(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Open the main grid GUI
        IslandSelector addon = (IslandSelector) getAddon();
        new MainGridGUI(addon, user.getPlayer()).open();
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.size() == 1) {
            // Return list of available subcommands that user has permission for
            List<String> subcommandLabels = new ArrayList<>();

            for (CompositeCommand subcommand : getSubCommands().values()) {
                // Check if user has permission for this subcommand
                if (subcommand.getPermission() == null || user.hasPermission(subcommand.getPermission())) {
                    // Add the primary label
                    String label = subcommand.getLabel();
                    if (label.toLowerCase().startsWith(args.get(0).toLowerCase())) {
                        subcommandLabels.add(label);
                    }
                }
            }

            return Optional.of(subcommandLabels);
        }

        return Optional.empty();
    }
}
