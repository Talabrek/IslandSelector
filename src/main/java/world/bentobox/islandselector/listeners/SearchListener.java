package world.bentobox.islandselector.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.gui.MainGridGUI;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles player search functionality for finding islands
 */
public class SearchListener implements Listener {

    private final IslandSelector addon;
    private final Map<UUID, SearchSession> activeSessions;

    public SearchListener(IslandSelector addon) {
        this.addon = addon;
        this.activeSessions = new HashMap<>();
    }

    /**
     * Start a search session for a player
     */
    public void startSearch(Player player, MainGridGUI gui) {
        // Create search session
        SearchSession session = new SearchSession(player, gui);
        activeSessions.put(player.getUniqueId(), session);

        // Send instructions
        player.sendMessage("§e§l=== Island Search ===");
        player.sendMessage("§7Type a player name to search for their island.");
        player.sendMessage("§7Type §ccancel §7to cancel the search.");
        player.sendMessage("§7Partial names are supported (e.g., 'Not' finds 'Notch')");
    }

    /**
     * Handle chat input during search
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check if player has an active search session
        if (!activeSessions.containsKey(playerId)) {
            return;
        }

        // Cancel the chat event (don't broadcast search input)
        event.setCancelled(true);

        SearchSession session = activeSessions.remove(playerId);
        String input = event.getMessage().trim();

        // Handle cancellation
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage("§cSearch cancelled.");
            // Reopen the GUI
            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                session.getGui().refresh();
                player.openInventory(session.getGui().getInventory());
            });
            return;
        }

        // Perform search
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            performSearch(player, input, session.getGui());
        });
    }

    /**
     * Perform the actual search for a player's island
     */
    private void performSearch(Player searcher, String searchTerm, MainGridGUI gui) {
        GridManager gridManager = addon.getGridManager();

        // Try to find matching players
        Player exactMatch = Bukkit.getPlayerExact(searchTerm);
        Player partialMatch = null;

        // If no exact match, try partial match
        if (exactMatch == null) {
            String lowerSearch = searchTerm.toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(lowerSearch)) {
                    partialMatch = online;
                    break;
                }
            }
        }

        Player targetPlayer = exactMatch != null ? exactMatch : partialMatch;

        // If still no match, check offline players via BSkyBlock
        if (targetPlayer == null) {
            // Try to find island by checking all grid locations
            GridCoordinate found = findIslandByPartialName(searchTerm);
            if (found != null) {
                // Found an island, center on it
                gui.centerViewportOn(found);
                searcher.sendMessage("§aFound island at " + found.toString() + "!");
                searcher.sendMessage("§7Centering view on their island...");
                gui.refresh();
                searcher.openInventory(gui.getInventory());
                return;
            }

            // Not found
            searcher.sendMessage("§cPlayer '" + searchTerm + "' not found or has no island.");
            searcher.sendMessage("§7Make sure the player name is spelled correctly.");
            // Reopen the GUI
            gui.refresh();
            searcher.openInventory(gui.getInventory());
            return;
        }

        // Found a player, check if they have an island
        GridCoordinate islandCoord = gridManager.getPlayerIslandCoordinate(targetPlayer.getUniqueId());
        if (islandCoord == null) {
            searcher.sendMessage("§cPlayer §f" + targetPlayer.getName() + " §cdoesn't have an island.");
            // Reopen the GUI
            gui.refresh();
            searcher.openInventory(gui.getInventory());
            return;
        }

        // Success! Center viewport on the found island
        gui.centerViewportOn(islandCoord);
        searcher.sendMessage("§aFound §f" + targetPlayer.getName() + "'s §aisland at " + islandCoord.toString() + "!");
        searcher.sendMessage("§7Centering view on their island...");

        // Reopen the GUI with the new viewport
        gui.refresh();
        searcher.openInventory(gui.getInventory());
    }

    /**
     * Search for an island by partial owner name (for offline players)
     */
    private GridCoordinate findIslandByPartialName(String partialName) {
        GridManager gridManager = addon.getGridManager();
        String lowerSearch = partialName.toLowerCase();

        // Check all known grid locations
        for (var location : gridManager.getAllLocations()) {
            String ownerName = location.getOwnerName();
            if (ownerName != null && ownerName.toLowerCase().startsWith(lowerSearch)) {
                return location.getCoordinate();
            }
        }

        return null;
    }

    /**
     * Cancel any active search session for a player
     */
    public void cancelSearch(Player player) {
        activeSessions.remove(player.getUniqueId());
    }

    /**
     * Unregister all sessions and listeners
     */
    public void shutdown() {
        activeSessions.clear();
        HandlerList.unregisterAll(this);
    }

    /**
     * Inner class to track a search session
     */
    private static class SearchSession {
        private final Player player;
        private final MainGridGUI gui;

        public SearchSession(Player player, MainGridGUI gui) {
            this.player = player;
            this.gui = gui;
        }

        public Player getPlayer() {
            return player;
        }

        public MainGridGUI getGui() {
            return gui;
        }
    }
}
