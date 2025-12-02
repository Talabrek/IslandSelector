package world.bentobox.islandselector.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;

/**
 * Admin reload command
 * /islandselector admin reload - Reloads the configuration
 */
public class AdminReloadCommand extends CompositeCommand {

    public AdminReloadCommand(CompositeCommand parent) {
        super(parent, "reload");
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin.reload");
        setDescription("commands.islandselector.admin.reload.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        IslandSelector addon = (IslandSelector) getAddon();
        addon.onReload();
        user.sendMessage("commands.islandselector.admin.reload.success");
        return true;
    }
}
