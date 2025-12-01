package world.bentobox.islandselector.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;

/**
 * Admin command for IslandSelector
 * /islandselector admin - Admin command suite
 */
public class AdminCommand extends CompositeCommand {

    private final IslandSelector addon;

    public AdminCommand(CompositeCommand parent, IslandSelector addon) {
        super(parent, "admin");
        this.addon = addon;
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin");
        setDescription("commands.islandselector.admin.description");

        // Add admin subcommands
        new AdminReloadCommand(this, addon);
        new AdminReserveCommand(this, addon);
        new AdminSetPriceCommand(this, addon);
        new AdminInfoCommand(this, addon);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        showHelp(this, user);
        return true;
    }
}
