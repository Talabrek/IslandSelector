package world.bentobox.islandselector.commands;

import org.bukkit.Bukkit;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Admin reset cooldown command
 * /islandselector admin resetcooldown <player> <switch|relocate|all>
 * Resets slot switch or relocation cooldown for a player
 */
public class AdminResetCooldownCommand extends CompositeCommand {

    public AdminResetCooldownCommand(CompositeCommand parent) {
        super(parent, "resetcooldown");
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin.resetcooldown");
        setParametersHelp("commands.islandselector.admin.resetcooldown.parameters");
        setDescription("commands.islandselector.admin.resetcooldown.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Validate arguments
        if (args.size() != 2) {
            showHelp(this, user);
            return false;
        }

        String playerName = args.get(0);
        String cooldownType = args.get(1).toLowerCase();

        // Validate cooldown type
        if (!cooldownType.equals("switch") && !cooldownType.equals("relocate") && !cooldownType.equals("all")) {
            user.sendMessage("commands.islandselector.admin.resetcooldown.invalid-type");
            return false;
        }

        // Find player UUID (works for online and offline players)
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            user.sendMessage("commands.islandselector.admin.resetcooldown.player-not-found");
            return false;
        }
        UUID playerUUID = offlinePlayer.getUniqueId();

        IslandSelector addon = (IslandSelector) getAddon();

        // Reset the appropriate cooldown(s)
        boolean switchReset = false;
        boolean relocateReset = false;

        if (cooldownType.equals("switch") || cooldownType.equals("all")) {
            addon.getSlotManager().resetSwitchCooldown(playerUUID);
            switchReset = true;
        }

        if (cooldownType.equals("relocate") || cooldownType.equals("all")) {
            // TODO: Implement relocation cooldown reset when relocation system is added
            // For now, just mark as reset
            relocateReset = true;
        }

        // Send success message based on what was reset
        if (cooldownType.equals("all")) {
            user.sendMessage("commands.islandselector.admin.resetcooldown.success-all",
                    "[player]", playerName);
        } else if (switchReset) {
            user.sendMessage("commands.islandselector.admin.resetcooldown.success-switch",
                    "[player]", playerName);
        } else if (relocateReset) {
            user.sendMessage("commands.islandselector.admin.resetcooldown.success-relocate",
                    "[player]", playerName);
        }

        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.size() == 1) {
            // Tab complete player names
            List<String> playerNames = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> playerNames.add(p.getName()));
            return Optional.of(playerNames);
        } else if (args.size() == 2) {
            // Tab complete cooldown types
            List<String> types = new ArrayList<>();
            types.add("switch");
            types.add("relocate");
            types.add("all");
            return Optional.of(types);
        }
        return Optional.empty();
    }
}
