package world.bentobox.islandselector.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.gui.SlotDeleteConfirmationGUI;
import world.bentobox.islandselector.managers.SlotManager;

/**
 * Delete command - allows players to delete their island slots
 * /islandselector delete [slot]
 */
public class DeleteCommand extends CompositeCommand {

    private final IslandSelector addon;

    public DeleteCommand(CompositeCommand parent) {
        super(parent, "delete");
        this.addon = (IslandSelector) getAddon();
    }

    @Override
    public void setup() {
        setPermission("islandselector.delete");
        setOnlyPlayer(true);
        setDescription("commands.islandselector.delete.description");
        setParametersHelp("commands.islandselector.delete.parameters");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Require exactly 1 argument: slot number
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }

        // Parse slot number
        int slotNumber;
        try {
            slotNumber = Integer.parseInt(args.get(0));
        } catch (NumberFormatException e) {
            user.sendMessage("commands.islandselector.delete.invalid-slot");
            return false;
        }

        // Validate slot number range
        int maxSlots = addon.getSettings().getMaxSlots();
        if (slotNumber < 1 || slotNumber > maxSlots) {
            user.sendMessage("commands.islandselector.delete.invalid-slot-range",
                "[min]", "1",
                "[max]", String.valueOf(maxSlots));
            return false;
        }

        // Check if slot exists for this player
        SlotManager slotManager = addon.getSlotManager();
        SlotData slot = slotManager.getSlot(user.getUniqueId(), slotNumber);

        if (slot == null) {
            user.sendMessage("commands.islandselector.delete.slot-not-found", "[slot]", String.valueOf(slotNumber));
            return false;
        }

        // Check if this is the currently active slot
        SlotData activeSlotData = slotManager.getActiveSlot(user.getUniqueId());
        int activeSlot = activeSlotData != null ? activeSlotData.getSlotNumber() : -1;
        if (slotNumber == activeSlot) {
            // Get total slot count
            int totalSlots = slotManager.getIslandCount(user.getUniqueId());
            if (totalSlots > 1) {
                // Has other slots - warn but allow
                user.sendMessage("commands.islandselector.delete.warning-active-slot");
            }
            // If only one slot, deletion will be handled in confirmation GUI
        }

        // Open deletion confirmation GUI
        new SlotDeleteConfirmationGUI(addon, user.getPlayer(), slot).open();

        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        List<String> options = new ArrayList<>();

        if (args.size() == 1) {
            // Suggest slot numbers that exist for this player
            SlotManager slotManager = addon.getSlotManager();
            int maxSlots = addon.getSettings().getMaxSlots();

            for (int i = 1; i <= maxSlots; i++) {
                SlotData slot = slotManager.getSlot(user.getUniqueId(), i);
                if (slot != null) {
                    options.add(String.valueOf(i));
                }
            }
        }

        return Optional.of(options);
    }
}
