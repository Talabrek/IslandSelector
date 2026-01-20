package world.bentobox.islandselector.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.managers.BackupManager;

/**
 * Admin backup command
 * /islandselector admin backup <player> <slot> - Creates a backup of a player's slot
 */
public class AdminBackupCommand extends CompositeCommand {

    private final BackupManager backupManager;

    public AdminBackupCommand(CompositeCommand parent) {
        super(parent, "backup");
        this.backupManager = ((IslandSelector) getAddon()).getBackupManager();
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin.backup");
        setDescription("commands.islandselector.admin.backup.description");
        setParametersHelp("commands.islandselector.admin.backup.parameters");
        setOnlyPlayer(false); // Can be run from console
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Check if FAWE is available
        IslandSelector addon = (IslandSelector) getAddon();
        if (!addon.isSchematicOperationsAvailable()) {
            user.sendMessage("&cThis feature requires FastAsyncWorldEdit (FAWE) to be installed.");
            return false;
        }

        if (args.size() < 2) {
            showHelp(this, user);
            return false;
        }

        // Get player UUID
        String playerName = args.get(0);
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

        // Check if player has actually played on this server
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            user.sendMessage("commands.islandselector.admin.backup.player-not-found", "[player]", playerName);
            return false;
        }

        UUID playerUUID = offlinePlayer.getUniqueId();

        // Parse slot number
        int slotNumber;
        try {
            slotNumber = Integer.parseInt(args.get(1));
            if (slotNumber < 1 || slotNumber > 10) {
                user.sendMessage("commands.islandselector.admin.backup.invalid-slot");
                return false;
            }
        } catch (NumberFormatException e) {
            user.sendMessage("commands.islandselector.admin.backup.invalid-slot");
            return false;
        }

        // Create backup asynchronously to avoid blocking
        user.sendMessage("commands.islandselector.admin.backup.creating",
            "[player]", playerName,
            "[slot]", String.valueOf(slotNumber));

        Bukkit.getScheduler().runTaskAsynchronously(getAddon().getPlugin(), () -> {
            boolean success = backupManager.createBackup(playerUUID, slotNumber);

            // Send result back on main thread
            Bukkit.getScheduler().runTask(getAddon().getPlugin(), () -> {
                if (success) {
                    int backupCount = backupManager.countBackups(playerUUID, slotNumber);
                    user.sendMessage("commands.islandselector.admin.backup.success",
                        "[player]", playerName,
                        "[slot]", String.valueOf(slotNumber),
                        "[count]", String.valueOf(backupCount));
                } else {
                    user.sendMessage("commands.islandselector.admin.backup.failed",
                        "[player]", playerName,
                        "[slot]", String.valueOf(slotNumber));
                }
            });
        });

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
            // Tab complete slot numbers
            List<String> slots = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                slots.add(String.valueOf(i));
            }
            return Optional.of(slots);
        }
        return Optional.empty();
    }
}
