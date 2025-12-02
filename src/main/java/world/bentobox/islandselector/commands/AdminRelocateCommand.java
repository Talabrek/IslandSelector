package world.bentobox.islandselector.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.managers.RelocationManager;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Admin relocate command
 * /islandselector admin relocate <player> <coordinate>
 * Forces relocation of a player's island to a new location (bypasses confirmation, cost, and cooldown)
 */
public class AdminRelocateCommand extends CompositeCommand {

    public AdminRelocateCommand(CompositeCommand parent) {
        super(parent, "relocate");
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin.relocate");
        setParametersHelp("commands.islandselector.admin.relocate.parameters");
        setDescription("commands.islandselector.admin.relocate.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Validate arguments
        if (args.size() != 2) {
            showHelp(this, user);
            return false;
        }

        String playerName = args.get(0);
        String coordStr = args.get(1);

        // Parse target coordinate
        GridCoordinate targetCoord = GridCoordinate.parse(coordStr);
        if (targetCoord == null) {
            user.sendMessage("§cInvalid coordinate format. Use format like '5,3' or 'A1'");
            return false;
        }

        // Find target player UUID
        UUID playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        if (playerUUID == null) {
            user.sendMessage("§cPlayer not found: " + playerName);
            return false;
        }

        IslandSelector addon = (IslandSelector) getAddon();
        GridManager gridManager = addon.getGridManager();
        RelocationManager relocationManager = addon.getRelocationManager();

        // Check if player has an island
        GridCoordinate currentCoord = gridManager.getPlayerIslandCoordinate(playerUUID);
        if (currentCoord == null) {
            user.sendMessage("§cPlayer " + playerName + " doesn't have an island.");
            return false;
        }

        // Check if target location is available
        var status = gridManager.getLocationStatus(targetCoord);
        switch (status) {
            case OCCUPIED:
                user.sendMessage("§cTarget location " + targetCoord.toString() + " is already occupied.");
                return false;
            case RESERVED:
                // Admin can relocate to reserved locations
                break;
            case LOCKED:
                user.sendMessage("§cTarget location " + targetCoord.toString() + " is locked.");
                return false;
            case AVAILABLE:
                // Good to go
                break;
        }

        // Check if trying to relocate to same location
        if (currentCoord.equals(targetCoord)) {
            user.sendMessage("§cPlayer's island is already at " + targetCoord.toString());
            return false;
        }

        // Get the player (online or create offline player)
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            // Player is offline - we can still relocate but need to handle differently
            user.sendMessage("§cPlayer " + playerName + " is offline. Cannot relocate offline players at this time.");
            user.sendMessage("§7Reason: Relocation requires the player to be online for teleportation.");
            return false;
        }

        // Get the island object to verify it exists
        Island island = addon.getIslands().getIsland(player.getWorld(), playerUUID);
        if (island == null) {
            user.sendMessage("§cCould not find island data for " + playerName);
            return false;
        }

        // Perform the relocation (bypasses all checks - it's an admin command)
        user.sendMessage("§eForce relocating " + playerName + "'s island from " +
                        currentCoord.toString() + " to " + targetCoord.toString() + "...");
        user.sendMessage("§7This bypasses confirmation, costs, and cooldowns.");

        // Notify the player
        player.sendMessage("§6§l[Admin Notice]");
        player.sendMessage("§eYour island is being relocated by an administrator.");
        player.sendMessage("§eFrom: " + currentCoord.toString() + " §7→ §eTo: " + targetCoord.toString());
        player.sendMessage("§7Please wait while your island is moved...");

        // Execute relocation through RelocationManager
        // Note: This will trigger the IslandRelocateEvent which other plugins can listen to
        relocationManager.relocateIsland(player, currentCoord, targetCoord);

        // Note: Success message is sent by RelocationManager after async operation completes
        // But we send an immediate confirmation to the admin
        user.sendMessage("§aRelocation initiated successfully.");
        user.sendMessage("§7The player will be notified when the operation completes.");

        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.size() == 1) {
            // Tab complete online player names
            List<String> playerNames = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> playerNames.add(p.getName()));
            return Optional.of(playerNames);
        } else if (args.size() == 2) {
            // Tab complete with coordinate format hints
            List<String> hints = new ArrayList<>();
            hints.add("0,0");
            hints.add("5,5");
            hints.add("A1");
            hints.add("B2");
            return Optional.of(hints);
        }
        return Optional.empty();
    }
}
