package world.bentobox.islandselector.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * Help command - shows list of available commands
 * /islandselector help - Displays command list with descriptions
 */
public class HelpCommand extends CompositeCommand {

    public HelpCommand(CompositeCommand parent) {
        super(parent, "help");
    }

    @Override
    public void setup() {
        setPermission("islandselector.help");
        setDescription("commands.islandselector.help.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Show header
        user.sendMessage("commands.islandselector.help.header");

        // Get parent command (IslandSelectorCommand)
        CompositeCommand parent = getParent();

        if (parent != null) {
            // Show all subcommands
            for (CompositeCommand subCommand : parent.getSubCommands().values()) {
                // Check if player has permission for this command
                if (subCommand.getPermission() == null || user.hasPermission(subCommand.getPermission())) {
                    String commandName = subCommand.getLabel();
                    String description = user.getTranslation(subCommand.getDescription());
                    String params = "";

                    // Get parameters if they exist
                    if (subCommand.getParameters() != null && !subCommand.getParameters().isEmpty()) {
                        params = " " + user.getTranslation(subCommand.getParameters());
                    }

                    // Format: /islandselector <command> [params] - Description
                    user.sendMessage("commands.islandselector.help.format",
                        "[command]", "/" + parent.getLabel() + " " + commandName + params,
                        "[description]", description);
                }
            }
        }

        // Show footer
        user.sendMessage("commands.islandselector.help.footer");

        return true;
    }
}
