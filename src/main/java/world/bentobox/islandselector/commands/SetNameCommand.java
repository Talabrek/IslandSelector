package world.bentobox.islandselector.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.managers.SlotManager;

/**
 * Setname command - allows players to set custom names for their island slots
 * /islandselector setname <slot> <name...>
 */
public class SetNameCommand extends CompositeCommand {

    private final IslandSelector addon;

    public SetNameCommand(CompositeCommand parent) {
        super(parent, "setname");
        this.addon = (IslandSelector) getAddon();
    }

    @Override
    public void setup() {
        setPermission("islandselector.setname");
        setOnlyPlayer(true);
        setDescription("commands.islandselector.setname.description");
        setParametersHelp("commands.islandselector.setname.parameters");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Require at least 2 arguments: slot number and name
        if (args.size() < 2) {
            showHelp(this, user);
            return false;
        }

        // Parse slot number
        int slotNumber;
        try {
            slotNumber = Integer.parseInt(args.get(0));
        } catch (NumberFormatException e) {
            user.sendMessage("commands.islandselector.setname.invalid-slot");
            return false;
        }

        // Validate slot number range
        int maxSlots = addon.getSettings().getMaxSlots();
        if (slotNumber < 1 || slotNumber > maxSlots) {
            user.sendMessage("commands.islandselector.setname.invalid-slot-range",
                "[min]", "1",
                "[max]", String.valueOf(maxSlots));
            return false;
        }

        // Check if slot exists for this player
        SlotManager slotManager = addon.getSlotManager();
        SlotData slot = slotManager.getSlot(user.getUniqueId(), slotNumber);

        if (slot == null) {
            user.sendMessage("commands.islandselector.setname.slot-not-found", "[slot]", String.valueOf(slotNumber));
            return false;
        }

        // Combine remaining args as the slot name
        String newName = String.join(" ", args.subList(1, args.size()));

        // Validate name length
        if (newName.length() > 32) {
            user.sendMessage("commands.islandselector.setname.name-too-long");
            return false;
        }

        if (newName.trim().isEmpty()) {
            user.sendMessage("commands.islandselector.setname.name-empty");
            return false;
        }

        // Set the slot name
        slotManager.setSlotName(user.getUniqueId(), slotNumber, newName);

        // Send success message
        user.sendMessage("commands.islandselector.setname.success",
            "[slot]", String.valueOf(slotNumber),
            "[name]", newName);

        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        List<String> options = new ArrayList<>();

        if (args.size() == 1) {
            // Suggest slot numbers 1-5 (or max slots from config)
            int maxSlots = addon.getSettings().getMaxSlots();
            for (int i = 1; i <= maxSlots; i++) {
                options.add(String.valueOf(i));
            }
        } else if (args.size() == 2) {
            // Suggest some example names
            options.add("Main_Base");
            options.add("Farm_Island");
            options.add("Mining_Island");
            options.add("Creative_Build");
        }

        return Optional.of(options);
    }
}
