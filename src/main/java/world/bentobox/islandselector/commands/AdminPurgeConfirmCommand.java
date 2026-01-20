package world.bentobox.islandselector.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.managers.SlotManager;

/**
 * Confirmation subcommand for purge operations
 * /islandselector admin purge confirm <uuid> - Confirm single player purge
 * /islandselector admin purge confirm-list <uuid,uuid,uuid> - Confirm batch purge
 */
public class AdminPurgeConfirmCommand extends CompositeCommand {

    private final SlotManager slotManager;
    private final IslandSelector addon;

    public AdminPurgeConfirmCommand(CompositeCommand parent) {
        super(parent, "confirm", "confirm-list");
        this.addon = (IslandSelector) getAddon();
        this.slotManager = addon.getSlotManager();
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin.purge");
        setDescription("commands.islandselector.admin.purge.confirm.description");
        setParametersHelp("commands.islandselector.admin.purge.confirm.parameters");
        setOnlyPlayer(false);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            user.sendMessage("commands.islandselector.admin.purge.confirm-usage");
            return false;
        }

        // Parse UUID(s)
        List<UUID> playersToPurge = new ArrayList<>();
        String uuidArg = args.get(0);

        // Check if it's a comma-separated list
        if (uuidArg.contains(",")) {
            String[] uuids = uuidArg.split(",");
            for (String uuidStr : uuids) {
                try {
                    playersToPurge.add(UUID.fromString(uuidStr.trim()));
                } catch (IllegalArgumentException e) {
                    user.sendMessage("commands.islandselector.admin.purge.invalid-uuid", "[uuid]", uuidStr);
                    return false;
                }
            }
        } else {
            // Single UUID
            try {
                playersToPurge.add(UUID.fromString(uuidArg));
            } catch (IllegalArgumentException e) {
                user.sendMessage("commands.islandselector.admin.purge.invalid-uuid", "[uuid]", uuidArg);
                return false;
            }
        }

        // Verify players are offline
        for (UUID uuid : playersToPurge) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            if (player.isOnline()) {
                user.sendMessage("commands.islandselector.admin.purge.player-online",
                    "[player]", player.getName() != null ? player.getName() : uuid.toString());
                return false;
            }
        }

        // Execute purge asynchronously
        user.sendMessage("commands.islandselector.admin.purge.executing",
            "[count]", String.valueOf(playersToPurge.size()));

        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            // Use array to make counters effectively final for nested lambda
            final int[] counters = {0, 0}; // [0] = successCount, [1] = failCount
            List<String> auditLog = new ArrayList<>();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());

            auditLog.add("=".repeat(60));
            auditLog.add("PURGE OPERATION - " + timestamp);
            auditLog.add("Initiated by: " + user.getName());
            auditLog.add("Total players: " + playersToPurge.size());
            auditLog.add("=".repeat(60));

            for (UUID uuid : playersToPurge) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                String playerName = player.getName() != null ? player.getName() : "Unknown";

                List<SlotData> slots = slotManager.getPlayerSlots(uuid);
                int slotCount = slots.size();
                int schematicCount = slotManager.countPlayerSchematics(uuid);

                boolean success = slotManager.purgePlayerData(uuid);

                if (success) {
                    counters[0]++;
                    auditLog.add("[SUCCESS] " + playerName + " (" + uuid + ")");
                } else {
                    counters[1]++;
                    auditLog.add("[FAILED] " + playerName + " (" + uuid + ")");
                }

                auditLog.add("  - Slots: " + slotCount);
                auditLog.add("  - Schematics: " + schematicCount);
                auditLog.add("  - Result: " + (success ? "PURGED" : "FAILED"));
                auditLog.add("");
            }

            auditLog.add("=".repeat(60));
            auditLog.add("SUMMARY");
            auditLog.add("Success: " + counters[0]);
            auditLog.add("Failed: " + counters[1]);
            auditLog.add("=".repeat(60));

            // Write audit log to file
            writeAuditLog(auditLog);

            // Report back to user on main thread
            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                user.sendMessage("commands.islandselector.admin.purge.complete",
                    "[success]", String.valueOf(counters[0]),
                    "[failed]", String.valueOf(counters[1]));

                if (counters[1] > 0) {
                    user.sendMessage("commands.islandselector.admin.purge.check-logs");
                }

                user.sendMessage("commands.islandselector.admin.purge.audit-logged");
            });
        });

        return true;
    }

    /**
     * Write audit log to file
     */
    private void writeAuditLog(List<String> logLines) {
        File auditDir = new File(addon.getDataFolder(), "audit");
        if (!auditDir.exists()) {
            auditDir.mkdirs();
        }

        SimpleDateFormat fileFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String filename = "purge_" + fileFormat.format(new Date()) + ".log";
        File auditFile = new File(auditDir, filename);

        try (FileWriter writer = new FileWriter(auditFile, true)) {
            for (String line : logLines) {
                writer.write(line + "\n");
            }
            addon.log("Audit log written to: " + auditFile.getAbsolutePath());
        } catch (IOException e) {
            addon.logError("Failed to write audit log: " + e.getMessage());
        }
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        // No tab completion for UUIDs
        return Optional.empty();
    }
}
