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

    private final IslandSelector addon;

    public SlotsCommand(CompositeCommand parent, IslandSelector addon) {
        super(parent, "slots", "slot");
        this.addon = addon;
    }

    @Override
    public void setup() {
        setPermission("islandselector.slots");
        setOnlyPlayer(true);
        setDescription("commands.islandselector.slots.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Open the slot selection GUI
        new SlotSelectionGUI(addon, user.getPlayer()).open();
        return true;
    }
}
