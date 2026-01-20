package world.bentobox.islandselector.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.gui.SlotSwitchConfirmationGUI;
import world.bentobox.islandselector.managers.SlotManager;

/**
 * Switch command - allows players to switch to a specific slot directly
 * /islandselector switch [slot]
 */
public class SwitchCommand extends CompositeCommand {

    private final IslandSelector addon;

    public SwitchCommand(CompositeCommand parent) {
        super(parent, "switch");
        this.addon = (IslandSelector) getAddon();
    }

    @Override
    public void setup() {
        setPermission("islandselector.slots");
        setOnlyPlayer(true);
        setDescription("commands.islandselector.switch.description");
        setParametersHelp("commands.islandselector.switch.parameters");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Check if FAWE is available
        if (!addon.isSchematicOperationsAvailable()) {
            user.sendMessage("&cThis feature requires FastAsyncWorldEdit (FAWE) to be installed.");
            return false;
        }

        // Require exactly 1 argument: slot number
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }

        // Parse slot number
        int targetSlotNumber;
        try {
            targetSlotNumber = Integer.parseInt(args.get(0));
        } catch (NumberFormatException e) {
            user.sendMessage("commands.islandselector.switch.invalid-slot");
            return false;
        }

        // Validate slot number range
        int maxSlots = addon.getSettings().getMaxSlots();
        if (targetSlotNumber < 1 || targetSlotNumber > maxSlots) {
            user.sendMessage("commands.islandselector.switch.invalid-slot-range",
                "[min]", "1",
                "[max]", String.valueOf(maxSlots));
            return false;
        }

        // Check if player has permission for this slot
        int playerMaxSlots = getPlayerMaxSlots(user);
        if (targetSlotNumber > playerMaxSlots) {
            user.sendMessage("commands.islandselector.switch.slot-locked",
                "[slot]", String.valueOf(targetSlotNumber));
            return false;
        }

        SlotManager slotManager = addon.getSlotManager();

        // Get player's current active slot
        SlotData activeSlot = slotManager.getActiveSlot(user.getUniqueId());
        if (activeSlot == null || !activeSlot.hasIsland()) {
            user.sendMessage("commands.islandselector.switch.no-island");
            return false;
        }

        // Check if target slot is already active
        if (activeSlot.getSlotNumber() == targetSlotNumber) {
            user.sendMessage("commands.islandselector.switch.already-active",
                "[slot]", String.valueOf(targetSlotNumber));
            return false;
        }

        // Get target slot
        SlotData targetSlot = slotManager.getSlot(user.getUniqueId(), targetSlotNumber);

        // Check if target slot has an island
        if (targetSlot == null || !targetSlot.hasIsland()) {
            user.sendMessage("commands.islandselector.switch.empty-slot",
                "[slot]", String.valueOf(targetSlotNumber));
            return false;
        }

        // Open switch confirmation GUI
        new SlotSwitchConfirmationGUI(addon, user.getPlayer(), activeSlot, targetSlot).open();
        return true;
    }

    /**
     * Get the maximum number of slots a player has access to based on permissions
     */
    private int getPlayerMaxSlots(User user) {
        if (user.hasPermission("islandselector.slots.5")) return 5;
        if (user.hasPermission("islandselector.slots.4")) return 4;
        if (user.hasPermission("islandselector.slots.3")) return 3;
        if (user.hasPermission("islandselector.slots.2")) return 2;
        return addon.getSettings().getDefaultSlots();
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        List<String> options = new ArrayList<>();

        if (args.size() == 1) {
            // Suggest slot numbers that the player has access to and that have islands
            SlotManager slotManager = addon.getSlotManager();
            int maxSlots = getPlayerMaxSlots(user);
            SlotData activeSlot = slotManager.getActiveSlot(user.getUniqueId());
            int activeSlotNum = activeSlot != null ? activeSlot.getSlotNumber() : -1;

            for (int i = 1; i <= maxSlots; i++) {
                // Don't suggest the currently active slot
                if (i == activeSlotNum) {
                    continue;
                }
                SlotData slot = slotManager.getSlot(user.getUniqueId(), i);
                if (slot != null && slot.hasIsland()) {
                    String slotStr = String.valueOf(i);
                    if (slotStr.startsWith(args.get(0))) {
                        options.add(slotStr);
                    }
                }
            }
        }

        return Optional.of(options);
    }
}
