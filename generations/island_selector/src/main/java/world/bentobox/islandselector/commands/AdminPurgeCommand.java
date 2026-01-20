package world.bentobox.islandselector.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.managers.SlotManager;

/**
 * Admin purge command - Remove slot data for specific players or groups
 * /islandselector admin purge <player> - Purge specific player
 * /islandselector admin purge inactive <days> [--dry-run] - Purge inactive players
 * /islandselector admin purge banned [--dry-run] - Purge banned players
 */
public class AdminPurgeCommand extends CompositeCommand {

    private final SlotManager slotManager;
    private final IslandSelector addon;

    public AdminPurgeCommand(CompositeCommand parent) {
        super(parent, "purge");
        this.addon = (IslandSelector) getAddon();
        this.slotManager = addon.getSlotManager();
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin.purge");
        setDescription("commands.islandselector.admin.purge.description");
        setParametersHelp("commands.islandselector.admin.purge.parameters");
        setOnlyPlayer(false); // Can be run from console
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            showHelp(this, user);
            return false;
        }

        String subcommand = args.get(0).toLowerCase();

        switch (subcommand) {
            case "inactive":
                return handlePurgeInactive(user, args);
            case "banned":
                return handlePurgeBanned(user, args);
            default:
                // Treat as player name
                return handlePurgePlayer(user, args.get(0));
        }
    }

    /**
     * Purge a specific player's slot data
     */
    private boolean handlePurgePlayer(User user, String playerName) {
        // Check if player is online
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        UUID playerUUID = offlinePlayer.getUniqueId();

        if (offlinePlayer.isOnline()) {
            user.sendMessage("commands.islandselector.admin.purge.player-online", "[player]", playerName);
            return false;
        }

        // Check if player has any slot data
        List<SlotData> slots = slotManager.getPlayerSlots(playerUUID);
        if (slots.isEmpty()) {
            user.sendMessage("commands.islandselector.admin.purge.no-data", "[player]", playerName);
            return false;
        }

        // Send confirmation message
        user.sendMessage("commands.islandselector.admin.purge.player-confirm",
            "[player]", playerName,
            "[slots]", String.valueOf(slots.size()));
        user.sendMessage("commands.islandselector.admin.purge.confirm-warning");
        user.sendMessage("commands.islandselector.admin.purge.confirm-command",
            "[command]", "/islandselector admin purge confirm " + playerUUID.toString());

        return true;
    }

    /**
     * Purge inactive players (not logged in for X days)
     */
    private boolean handlePurgeInactive(User user, List<String> args) {
        if (args.size() < 2) {
            user.sendMessage("commands.islandselector.admin.purge.inactive-usage");
            return false;
        }

        // Parse days parameter
        int days;
        try {
            days = Integer.parseInt(args.get(1));
            if (days < 1) {
                user.sendMessage("commands.islandselector.admin.purge.invalid-days");
                return false;
            }
        } catch (NumberFormatException e) {
            user.sendMessage("commands.islandselector.admin.purge.invalid-days");
            return false;
        }

        // Check for dry-run flag
        boolean dryRun = args.size() > 2 && args.get(2).equalsIgnoreCase("--dry-run");

        // Calculate cutoff time
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);

        // Find inactive players with slot data
        List<UUID> inactivePlayers = new ArrayList<>();
        Set<UUID> allPlayersWithSlots = slotManager.getAllPlayersWithSlots();

        for (UUID playerUUID : allPlayersWithSlots) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);

            // Skip online players
            if (offlinePlayer.isOnline()) {
                continue;
            }

            // Check last seen time
            long lastPlayed = offlinePlayer.getLastPlayed();
            if (lastPlayed > 0 && lastPlayed < cutoffTime) {
                inactivePlayers.add(playerUUID);
            }
        }

        if (inactivePlayers.isEmpty()) {
            user.sendMessage("commands.islandselector.admin.purge.inactive-none", "[days]", String.valueOf(days));
            return true;
        }

        // Show preview
        user.sendMessage("commands.islandselector.admin.purge.inactive-found",
            "[count]", String.valueOf(inactivePlayers.size()),
            "[days]", String.valueOf(days));

        // List first 10 players
        int showCount = Math.min(10, inactivePlayers.size());
        for (int i = 0; i < showCount; i++) {
            UUID uuid = inactivePlayers.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            int slotCount = slotManager.getPlayerSlots(uuid).size();
            long daysSinceLogin = (System.currentTimeMillis() - op.getLastPlayed()) / (24L * 60L * 60L * 1000L);
            user.sendMessage("commands.islandselector.admin.purge.player-entry",
                "[player]", op.getName() != null ? op.getName() : "Unknown",
                "[slots]", String.valueOf(slotCount),
                "[days]", String.valueOf(daysSinceLogin));
        }

        if (inactivePlayers.size() > 10) {
            user.sendMessage("commands.islandselector.admin.purge.more-players",
                "[count]", String.valueOf(inactivePlayers.size() - 10));
        }

        if (dryRun) {
            user.sendMessage("commands.islandselector.admin.purge.dry-run-complete");
            return true;
        }

        // Provide confirmation command
        String playerListStr = inactivePlayers.stream()
            .map(UUID::toString)
            .collect(Collectors.joining(","));
        user.sendMessage("commands.islandselector.admin.purge.confirm-warning");
        user.sendMessage("commands.islandselector.admin.purge.confirm-command",
            "[command]", "/islandselector admin purge confirm-list " + playerListStr);

        return true;
    }

    /**
     * Purge banned players
     */
    private boolean handlePurgeBanned(User user, List<String> args) {
        // Check for dry-run flag
        boolean dryRun = args.size() > 1 && args.get(1).equalsIgnoreCase("--dry-run");

        // Find banned players with slot data
        List<UUID> bannedPlayers = new ArrayList<>();
        Set<UUID> allPlayersWithSlots = slotManager.getAllPlayersWithSlots();

        for (UUID playerUUID : allPlayersWithSlots) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);

            // Skip online players
            if (offlinePlayer.isOnline()) {
                continue;
            }

            // Check if banned
            if (offlinePlayer.isBanned()) {
                bannedPlayers.add(playerUUID);
            }
        }

        if (bannedPlayers.isEmpty()) {
            user.sendMessage("commands.islandselector.admin.purge.banned-none");
            return true;
        }

        // Show preview
        user.sendMessage("commands.islandselector.admin.purge.banned-found",
            "[count]", String.valueOf(bannedPlayers.size()));

        // List all banned players (usually not too many)
        for (UUID uuid : bannedPlayers) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            int slotCount = slotManager.getPlayerSlots(uuid).size();
            user.sendMessage("commands.islandselector.admin.purge.player-entry",
                "[player]", op.getName() != null ? op.getName() : "Unknown",
                "[slots]", String.valueOf(slotCount),
                "[days]", "N/A");
        }

        if (dryRun) {
            user.sendMessage("commands.islandselector.admin.purge.dry-run-complete");
            return true;
        }

        // Provide confirmation command
        String playerListStr = bannedPlayers.stream()
            .map(UUID::toString)
            .collect(Collectors.joining(","));
        user.sendMessage("commands.islandselector.admin.purge.confirm-warning");
        user.sendMessage("commands.islandselector.admin.purge.confirm-command",
            "[command]", "/islandselector admin purge confirm-list " + playerListStr);

        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.size() == 1) {
            List<String> options = new ArrayList<>();
            options.add("inactive");
            options.add("banned");
            // Add player names
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
            return Optional.of(options);
        } else if (args.size() == 2 && args.get(0).equalsIgnoreCase("inactive")) {
            // Suggest common day values
            List<String> days = List.of("30", "60", "90", "180", "365");
            return Optional.of(days);
        } else if (args.size() == 3 && (args.get(0).equalsIgnoreCase("inactive") || args.get(0).equalsIgnoreCase("banned"))) {
            return Optional.of(List.of("--dry-run"));
        }
        return Optional.empty();
    }
}
