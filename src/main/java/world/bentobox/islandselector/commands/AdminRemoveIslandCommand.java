package world.bentobox.islandselector.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.managers.IslandRemovalManager;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Admin command to remove a player's island from the world while preserving their slot data.
 *
 * /islandselector admin remove <player>
 *
 * This command:
 * 1. Saves the player's current active island to their slot schematic
 * 2. Clears all blocks from the island location
 * 3. Unregisters the island from BentoBox
 * 4. Frees up the grid location for other players
 *
 * The player's slot data is preserved, so they can place their island at a new
 * location if they log in again.
 */
public class AdminRemoveIslandCommand extends CompositeCommand {

    public AdminRemoveIslandCommand(CompositeCommand parent) {
        super(parent, "remove");
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin.remove");
        setParametersHelp("commands.islandselector.admin.remove.parameters");
        setDescription("commands.islandselector.admin.remove.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Validate arguments
        if (args.size() != 1) {
            user.sendMessage("§cUsage: /islandselector admin remove <player>");
            user.sendMessage("§7Removes a player's island from the world while preserving slot data.");
            user.sendMessage("§7The player can place their island at a new location if they log in.");
            return false;
        }

        String playerName = args.get(0);

        // Find target player UUID
        UUID playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        if (playerUUID == null) {
            user.sendMessage("§cPlayer not found: " + playerName);
            return false;
        }

        IslandSelector addon = (IslandSelector) getAddon();
        GridManager gridManager = addon.getGridManager();
        World bskyblockWorld = gridManager.getBSkyBlockWorld();

        if (bskyblockWorld == null) {
            user.sendMessage("§cBSkyBlock world is not available.");
            return false;
        }

        // Check if player has an island
        Island island = addon.getIslands().getIsland(bskyblockWorld, playerUUID);
        if (island == null) {
            user.sendMessage("§cPlayer " + playerName + " doesn't have an island.");
            return false;
        }

        // Get the player's grid coordinate
        GridCoordinate coord = gridManager.getPlayerIslandCoordinate(playerUUID);
        if (coord == null) {
            user.sendMessage("§cCould not find grid coordinate for " + playerName + "'s island.");
            return false;
        }

        // Get active slot
        SlotData activeSlot = addon.getSlotManager().getActiveSlot(playerUUID);
        if (activeSlot == null) {
            user.sendMessage("§cPlayer " + playerName + " has no active slot data.");
            return false;
        }

        // Warn if player is online
        Player targetPlayer = Bukkit.getPlayer(playerUUID);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            user.sendMessage("§6Warning: " + playerName + " is currently online!");
            user.sendMessage("§7They will be teleported to spawn when their island is removed.");
        }

        // Confirm the action
        user.sendMessage("§e§lRemoving island for " + playerName + "...");
        user.sendMessage("§7Grid location: " + coord.toString());
        user.sendMessage("§7Active slot: " + activeSlot.getSlotNumber() + " (" + activeSlot.getSlotName() + ")");

        // Execute removal through IslandRemovalManager
        IslandRemovalManager removalManager = addon.getIslandRemovalManager();
        removalManager.removeIsland(playerUUID, user, (success) -> {
            if (success) {
                user.sendMessage("§a§lIsland successfully removed!");
                user.sendMessage("§aGrid location " + coord.toString() + " is now available.");
                user.sendMessage("§a" + playerName + "'s island has been saved to slot " + activeSlot.getSlotNumber());
                user.sendMessage("§7If they log in, they can select a new location to place their island.");
            } else {
                user.sendMessage("§cFailed to remove island. Check console for errors.");
            }
        });

        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.size() == 1) {
            // Tab complete with player names who have islands
            IslandSelector addon = (IslandSelector) getAddon();
            List<String> playerNames = new ArrayList<>();

            // Add online players
            Bukkit.getOnlinePlayers().forEach(p -> playerNames.add(p.getName()));

            // Could also add offline players with islands, but that would require
            // iterating through all slot data which could be expensive

            return Optional.of(playerNames);
        }
        return Optional.empty();
    }
}
