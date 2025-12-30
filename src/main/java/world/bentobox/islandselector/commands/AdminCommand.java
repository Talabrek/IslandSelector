package world.bentobox.islandselector.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.entity.Player;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.gui.AdminGridGUI;

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
        new AdminResetCooldownCommand(this);
        new AdminBackupCommand(this);
        new AdminRestoreCommand(this);
        new AdminRelocateCommand(this);
        new AdminRemoveIslandCommand(this);
        new AdminGuiRelocateCommand(this);

        // Purge command with confirmation subcommand
        AdminPurgeCommand purgeCommand = new AdminPurgeCommand(this);
        new AdminPurgeConfirmCommand(purgeCommand);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // If no args, open the admin GUI
        if (args.isEmpty()) {
            Player player = user.getPlayer();
            if (player != null) {
                IslandSelector addon = (IslandSelector) getAddon();
                new AdminGridGUI(addon, player).open();
                return true;
            }
        }

        // Otherwise show help
        showHelp(this, user);
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.size() == 1) {
            // Return list of available admin subcommands that user has permission for
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
