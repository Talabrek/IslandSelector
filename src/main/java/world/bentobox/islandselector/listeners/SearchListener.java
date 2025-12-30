package world.bentobox.islandselector.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.gui.AdminGridGUI;
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

    // Admin-specific pending actions
    private final Map<UUID, AdminGridGUI> pendingAdminSearch;
    private final Map<UUID, AdminGridGUI> pendingAdminJump;
    private final Map<UUID, GridCoordinate> pendingAdminPriceSet;

    public SearchListener(IslandSelector addon) {
        this.addon = addon;
        this.activeSessions = new HashMap<>();
        this.pendingAdminSearch = new HashMap<>();
        this.pendingAdminJump = new HashMap<>();
        this.pendingAdminPriceSet = new HashMap<>();
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

        String input = event.getMessage().trim();

        // Check for admin price set
        if (pendingAdminPriceSet.containsKey(playerId)) {
            event.setCancelled(true);
            GridCoordinate coord = pendingAdminPriceSet.remove(playerId);

            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage("§cPrice setting cancelled.");
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    new AdminGridGUI(addon, player).open();
                });
                return;
            }

            // Parse price
            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                handlePriceSet(player, coord, input);
            });
            return;
        }

        // Check for admin search
        if (pendingAdminSearch.containsKey(playerId)) {
            event.setCancelled(true);
            AdminGridGUI gui = pendingAdminSearch.remove(playerId);

            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage("§cSearch cancelled.");
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> gui.reopen());
                return;
            }

            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                performAdminSearch(player, input, gui);
            });
            return;
        }

        // Check for admin jump
        if (pendingAdminJump.containsKey(playerId)) {
            event.setCancelled(true);
            AdminGridGUI gui = pendingAdminJump.remove(playerId);

            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage("§cJump cancelled.");
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> gui.reopen());
                return;
            }

            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                handleAdminJump(player, input, gui);
            });
            return;
        }

        // Check if player has an active search session (regular GUI)
        if (!activeSessions.containsKey(playerId)) {
            return;
        }

        // Cancel the chat event (don't broadcast search input)
        event.setCancelled(true);

        SearchSession session = activeSessions.remove(playerId);

        // Handle cancellation
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage("§cSearch cancelled.");
            // Reopen the GUI (must re-register listener since it was unregistered on close)
            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                session.getGui().reopen();
            });
            return;
        }

        // Perform search
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            performSearch(player, input, session.getGui());
        });
    }

    private void handlePriceSet(Player player, GridCoordinate coord, String input) {
        try {
            // Remove any non-numeric characters except decimal point
            String cleanInput = input.replaceAll("[^0-9.]", "");
            double price = Double.parseDouble(cleanInput);

            if (price <= 0) {
                player.sendMessage("§cPrice must be greater than 0.");
                new AdminGridGUI(addon, player).open();
                return;
            }

            // Set the price
            addon.getGridManager().setPurchasePrice(coord, price);
            player.sendMessage("§aSet price for §f" + coord + " §ato §a$" + String.format("%,.0f", price));

            new AdminGridGUI(addon, player).open();
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid price format. Please enter a number.");
            new AdminGridGUI(addon, player).open();
        }
    }

    private void performAdminSearch(Player player, String searchTerm, AdminGridGUI gui) {
        GridManager gridManager = addon.getGridManager();

        // Try to find matching players
        Player exactMatch = Bukkit.getPlayerExact(searchTerm);
        Player partialMatch = null;

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

        if (targetPlayer == null) {
            GridCoordinate found = findIslandByPartialName(searchTerm);
            if (found != null) {
                gui.centerViewportOn(found);
                player.sendMessage("§aFound island at " + found.toString() + "!");
                gui.reopen();
                return;
            }

            player.sendMessage("§cPlayer '" + searchTerm + "' not found or has no island.");
            gui.reopen();
            return;
        }

        GridCoordinate islandCoord = gridManager.getPlayerIslandCoordinate(targetPlayer.getUniqueId());
        if (islandCoord == null) {
            player.sendMessage("§cPlayer §f" + targetPlayer.getName() + " §cdoesn't have an island.");
            gui.reopen();
            return;
        }

        gui.centerViewportOn(islandCoord);
        player.sendMessage("§aFound §f" + targetPlayer.getName() + "'s §aisland at " + islandCoord.toString() + "!");
        gui.reopen();
    }

    private void handleAdminJump(Player player, String input, AdminGridGUI gui) {
        GridCoordinate coord = GridCoordinate.parse(input);
        if (coord == null) {
            player.sendMessage("§cInvalid coordinate format. Use §fX,Z §c(e.g., §f0,0 §cor §f-5,3§c)");
            gui.reopen();
            return;
        }

        gui.centerViewportOn(coord);
        player.sendMessage("§aJumped to coordinate §f" + coord.toString());
        gui.reopen();
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
                gui.reopen();
                return;
            }

            // Not found
            searcher.sendMessage("§cPlayer '" + searchTerm + "' not found or has no island.");
            searcher.sendMessage("§7Make sure the player name is spelled correctly.");
            // Reopen the GUI (must re-register listener since it was unregistered on close)
            gui.reopen();
            return;
        }

        // Found a player, check if they have an island
        GridCoordinate islandCoord = gridManager.getPlayerIslandCoordinate(targetPlayer.getUniqueId());
        if (islandCoord == null) {
            searcher.sendMessage("§cPlayer §f" + targetPlayer.getName() + " §cdoesn't have an island.");
            // Reopen the GUI (must re-register listener since it was unregistered on close)
            gui.reopen();
            return;
        }

        // Success! Center viewport on the found island
        gui.centerViewportOn(islandCoord);
        searcher.sendMessage("§aFound §f" + targetPlayer.getName() + "'s §aisland at " + islandCoord.toString() + "!");
        searcher.sendMessage("§7Centering view on their island...");

        // Reopen the GUI with the new viewport (must re-register listener since it was unregistered on close)
        gui.reopen();
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
     * Set pending admin search
     */
    public void setPendingAdminSearch(UUID playerId, AdminGridGUI gui) {
        pendingAdminSearch.put(playerId, gui);
    }

    /**
     * Set pending admin jump to coordinate
     */
    public void setPendingAdminJump(UUID playerId, AdminGridGUI gui) {
        pendingAdminJump.put(playerId, gui);
    }

    /**
     * Set pending admin price set
     */
    public void setPendingAdminPriceSet(UUID playerId, GridCoordinate coord) {
        pendingAdminPriceSet.put(playerId, coord);
    }

    /**
     * Unregister all sessions and listeners
     */
    public void shutdown() {
        activeSessions.clear();
        pendingAdminSearch.clear();
        pendingAdminJump.clear();
        pendingAdminPriceSet.clear();
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
