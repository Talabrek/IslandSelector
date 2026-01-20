package world.bentobox.islandselector.commands;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.managers.BackupManager;

/**
 * Admin restore command
 * /islandselector admin restore <player> <slot> [backup-file] - Restores a backup to a player's slot
 * If no backup file specified, restores the most recent backup
 */
public class AdminRestoreCommand extends CompositeCommand {

    private final BackupManager backupManager;

    public AdminRestoreCommand(CompositeCommand parent) {
        super(parent, "restore");
        this.backupManager = ((IslandSelector) getAddon()).getBackupManager();
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin.restore");
        setDescription("commands.islandselector.admin.restore.description");
        setParametersHelp("commands.islandselector.admin.restore.parameters");
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
            user.sendMessage("commands.islandselector.admin.restore.player-not-found", "[player]", playerName);
            return false;
        }

        UUID playerUUID = offlinePlayer.getUniqueId();

        // Parse slot number
        int slotNumber;
        try {
            slotNumber = Integer.parseInt(args.get(1));
            if (slotNumber < 1 || slotNumber > 10) {
                user.sendMessage("commands.islandselector.admin.restore.invalid-slot");
                return false;
            }
        } catch (NumberFormatException e) {
            user.sendMessage("commands.islandselector.admin.restore.invalid-slot");
            return false;
        }

        // Get backup file
        File backupFile;
        if (args.size() >= 3) {
            // Specific backup file requested
            String backupFilename = args.get(2);
            if (!backupFilename.endsWith(".schem")) {
                backupFilename += ".schem";
            }
            backupFile = new File(backupManager.getPlayerBackupDirectory(playerUUID), backupFilename);

            if (!backupFile.exists()) {
                user.sendMessage("commands.islandselector.admin.restore.backup-not-found", "[file]", backupFilename);
                return false;
            }
        } else {
            // Use most recent backup
            File[] backups = backupManager.listBackups(playerUUID, slotNumber);
            if (backups == null || backups.length == 0) {
                user.sendMessage("commands.islandselector.admin.restore.no-backups",
                    "[player]", playerName,
                    "[slot]", String.valueOf(slotNumber));
                return false;
            }
            backupFile = backups[0]; // Most recent (already sorted)
        }

        // Format backup timestamp for display
        SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String backupDate = displayFormat.format(new Date(backupFile.lastModified()));

        // Perform restore asynchronously to avoid blocking
        user.sendMessage("commands.islandselector.admin.restore.restoring",
            "[player]", playerName,
            "[slot]", String.valueOf(slotNumber),
            "[file]", backupFile.getName(),
            "[date]", backupDate);

        Bukkit.getScheduler().runTaskAsynchronously(getAddon().getPlugin(), () -> {
            boolean success = backupManager.restoreBackup(backupFile, playerUUID, slotNumber);

            // Send result back on main thread
            Bukkit.getScheduler().runTask(getAddon().getPlugin(), () -> {
                if (success) {
                    user.sendMessage("commands.islandselector.admin.restore.success",
                        "[player]", playerName,
                        "[slot]", String.valueOf(slotNumber),
                        "[file]", backupFile.getName());
                } else {
                    user.sendMessage("commands.islandselector.admin.restore.failed",
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
        } else if (args.size() == 3) {
            // Tab complete backup filenames for this player and slot
            String playerName = args.get(0);
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                return Optional.empty();
            }
            UUID playerUUID = offlinePlayer.getUniqueId();

            try {
                int slotNumber = Integer.parseInt(args.get(1));
                File[] backups = backupManager.listBackups(playerUUID, slotNumber);
                if (backups == null) {
                    return Optional.empty();
                }

                List<String> backupNames = new ArrayList<>();
                for (File backup : backups) {
                    backupNames.add(backup.getName());
                }
                return Optional.of(backupNames);
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
