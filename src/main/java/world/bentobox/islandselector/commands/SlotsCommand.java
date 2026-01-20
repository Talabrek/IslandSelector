package world.bentobox.islandselector.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.gui.SlotSelectionGUI;

/**
 * Subcommand for slot management
 * /islandselector slots - Opens the slot selection GUI
 */
public class SlotsCommand extends CompositeCommand {

    public SlotsCommand(CompositeCommand parent) {
        super(parent, "slots", "slot");
    }

    @Override
    public void setup() {
        setPermission("islandselector.slots");
        setOnlyPlayer(true);
        setDescription("commands.islandselector.slots.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        IslandSelector addon = (IslandSelector) getAddon();

        // Check if slot system is enabled in config
        if (!addon.getSettings().isSlotsEnabled()) {
            user.sendMessage("&cThe slot system is disabled on this server.");
            return false;
        }

        // Check if FAWE is available
        if (!addon.isSchematicOperationsAvailable()) {
            user.sendMessage("&cThis feature requires FastAsyncWorldEdit (FAWE) to be installed.");
            return false;
        }

        // Open the slot selection GUI
        new SlotSelectionGUI(addon, user.getPlayer()).open();
        return true;
    }
}
