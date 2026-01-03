package world.bentobox.islandselector.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.managers.SlotManager;

/**
 * Seticon command - allows players to set custom icons for their island slots
 * /islandselector seticon [slot]
 */
public class SetIconCommand extends CompositeCommand {

    private final IslandSelector addon;

    public SetIconCommand(CompositeCommand parent) {
        super(parent, "seticon");
        this.addon = (IslandSelector) getAddon();
    }

    @Override
    public void setup() {
        setPermission("islandselector.seticon");
        setOnlyPlayer(true);
        setDescription("commands.islandselector.seticon.description");
        setParametersHelp("commands.islandselector.seticon.parameters");
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
            user.sendMessage("commands.islandselector.seticon.invalid-slot");
            return false;
        }

        // Validate slot number range
        int maxSlots = addon.getSettings().getMaxSlots();
        if (slotNumber < 1 || slotNumber > maxSlots) {
            user.sendMessage("commands.islandselector.seticon.invalid-slot-range",
                "[min]", "1",
                "[max]", String.valueOf(maxSlots));
            return false;
        }

        // Check if slot exists for this player
        SlotManager slotManager = addon.getSlotManager();
        SlotData slot = slotManager.getSlot(user.getUniqueId(), slotNumber);

        if (slot == null) {
            user.sendMessage("commands.islandselector.seticon.slot-not-found", "[slot]", String.valueOf(slotNumber));
            return false;
        }

        // Check if player is holding an item
        Player player = user.getPlayer();
        if (player == null) {
            return false;
        }
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            user.sendMessage("commands.islandselector.seticon.no-item");
            return false;
        }

        // Get the material name
        String materialName = heldItem.getType().name();

        // Set the slot icon
        slotManager.setSlotIcon(user.getUniqueId(), slotNumber, materialName);

        // Send success message
        user.sendMessage("commands.islandselector.seticon.success",
            "[slot]", String.valueOf(slotNumber),
            "[icon]", materialName.toLowerCase().replace('_', ' '));

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
        }

        return Optional.of(options);
    }
}
