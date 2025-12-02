package world.bentobox.islandselector.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * Admin command for IslandSelector
 * /islandselector admin - Admin command suite
 */
public class AdminCommand extends CompositeCommand {

    public AdminCommand(CompositeCommand parent) {
        super(parent, "admin");
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin");
        setDescription("commands.islandselector.admin.description");

        // Add admin subcommands - they use getAddon() to get addon reference
        new AdminReloadCommand(this);
        new AdminReserveCommand(this);
        new AdminUnreserveCommand(this);
        new AdminSetPriceCommand(this);
        new AdminClearPriceCommand(this);
        new AdminInfoCommand(this);
        new AdminVersionCommand(this);
        new AdminExpandCommand(this);
        new AdminShrinkCommand(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        showHelp(this, user);
        return true;
    }
}
