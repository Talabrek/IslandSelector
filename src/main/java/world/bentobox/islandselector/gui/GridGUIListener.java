package world.bentobox.islandselector.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import world.bentobox.islandselector.listeners.IslandCreateListener;
import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Listener for handling clicks in the Grid GUI
 */
public class GridGUIListener implements Listener {

    private final MainGridGUI gui;

    public GridGUIListener(MainGridGUI gui) {
        this.gui = gui;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if this is our GUI
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof MainGridGUI)) {
            return;
        }

        MainGridGUI clickedGUI = (MainGridGUI) holder;
        if (!clickedGUI.equals(gui)) {
            return;
        }

        // Cancel the event to prevent item movement
        event.setCancelled(true);

        // Ignore clicks outside inventory
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        boolean shift = event.isShiftClick();

        // Handle navigation arrows
        if (gui.isScrollUpSlot(slot)) {
            gui.scrollUp(shift);
            return;
        }
        if (gui.isScrollDownSlot(slot)) {
            gui.scrollDown(shift);
            return;
        }
        if (gui.isScrollLeftSlot(slot)) {
            gui.scrollLeft(shift);
            return;
        }
        if (gui.isScrollRightSlot(slot)) {
            gui.scrollRight(shift);
            return;
        }
        // Handle control buttons
        if (slot == gui.getFilterAllSlot()) {
            gui.setFilter(MainGridGUI.FilterType.ALL);
            return;
        }
        if (slot == gui.getFilterAvailableSlot()) {
            gui.setFilter(MainGridGUI.FilterType.AVAILABLE);
            return;
        }
        if (slot == gui.getFilterOnlineSlot()) {
            gui.setFilter(MainGridGUI.FilterType.ONLINE);
            return;
        }
        if (slot == gui.getSearchSlot()) {
            // TODO: Implement search functionality
            player.sendMessage("\u00A7eSearch feature coming soon!");
            return;
        }
        if (slot == gui.getFindMyIslandSlot()) {
            gui.findMyIsland();
            return;
        }
        if (slot == gui.getNeighborhoodSlot()) {
            player.closeInventory();
            new NeighborhoodGUI(gui.getAddon(), player).open();
            return;
        }
        if (slot == gui.getSlotsSlot()) {
            player.closeInventory();
            new SlotSelectionGUI(gui.getAddon(), player).open();
            return;
        }
        if (slot == gui.getCloseSlot()) {
            player.closeInventory();
            return;
        }

        // Handle grid slot clicks
        GridCoordinate coord = gui.getCoordinateForSlot(slot);
        if (coord != null) {
            handleGridClick(player, coord, event.isRightClick());
        }
    }

    /**
     * Handle click on a grid location
     */
    private void handleGridClick(Player player, GridCoordinate coord, boolean rightClick) {
        // Get the location status
        var gridManager = gui.getAddon().getGridManager();
        var status = gridManager.getLocationStatus(coord);

        switch (status) {
            case AVAILABLE:
                // Handle claiming or relocation
                handleAvailableClick(player, coord);
                break;
            case OCCUPIED:
                // Handle visiting (right-click) or viewing info
                if (rightClick) {
                    handleVisitClick(player, coord);
                } else {
                    handleViewIslandInfo(player, coord);
                }
                break;
            case RESERVED:
                // Handle purchase if purchasable
                var location = gridManager.getGridLocation(coord);
                if (location != null && location.isPurchasable()) {
                    handlePurchaseClick(player, coord, location.getPurchasePrice());
                } else {
                    player.sendMessage("\u00A7cThis location is reserved and not available.");
                }
                break;
            case LOCKED:
                player.sendMessage("\u00A77This area is not yet available.");
                break;
        }
    }

    /**
     * Handle click on available location
     */
    private void handleAvailableClick(Player player, GridCoordinate coord) {
        var gridManager = gui.getAddon().getGridManager();
        var playerIsland = gridManager.getPlayerIslandCoordinate(player.getUniqueId());

        if (playerIsland == null) {
            // Player doesn't have an island - initiate claiming
            player.closeInventory();
            IslandCreateListener createListener = gui.getAddon().getIslandCreateListener();
            // Register this as a pending claim
            createListener.onLocationSelected(player, coord);
            // Open confirmation GUI
            new ConfirmationGUI(gui.getAddon(), player, coord, createListener, ConfirmationGUI.ActionType.CLAIM).open();
        } else {
            // Player has an island - initiate relocation
            player.sendMessage("\u00A7aYou selected location \u00A7f" + coord.toString() + "\u00A7a for relocation!");
            player.sendMessage("\u00A77Island relocation coming soon...");
            // TODO: Implement confirmation GUI and actual relocation
        }
    }

    /**
     * Handle right-click to visit an island
     */
    private void handleVisitClick(Player player, GridCoordinate coord) {
        var gridManager = gui.getAddon().getGridManager();
        // Ensure status is checked first (may register island from BSkyBlock)
        gridManager.getLocationStatus(coord);
        var location = gridManager.getGridLocation(coord);

        if (location == null || location.getOwnerUUID() == null) {
            player.sendMessage("\u00A7cThis island doesn't exist.");
            return;
        }

        // Check if it's the player's own island
        if (location.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage("\u00A7eTeleporting to your island...");
            // TODO: Teleport to own island
            return;
        }

        // TODO: Check if warp is open and teleport
        player.sendMessage("\u00A77Visiting islands coming soon...");
    }

    /**
     * Handle left-click to view island info
     */
    private void handleViewIslandInfo(Player player, GridCoordinate coord) {
        var gridManager = gui.getAddon().getGridManager();
        // Ensure status is checked first (may register island from BSkyBlock)
        gridManager.getLocationStatus(coord);
        var location = gridManager.getGridLocation(coord);

        if (location == null) {
            player.sendMessage("\u00A77No island information available.");
            return;
        }

        player.sendMessage("\u00A76=== Island Info: " + coord.toString() + " ===");
        if (location.getOwnerName() != null) {
            player.sendMessage("\u00A77Owner: \u00A7f" + location.getOwnerName());
        }
        if (gridManager.isOwnerOnline(coord)) {
            player.sendMessage("\u00A7aOwner is online");
        } else {
            player.sendMessage("\u00A7cOwner is offline");
        }
    }

    /**
     * Handle click on purchasable reserved location
     */
    private void handlePurchaseClick(Player player, GridCoordinate coord, double price) {
        var gridManager = gui.getAddon().getGridManager();
        var playerIsland = gridManager.getPlayerIslandCoordinate(player.getUniqueId());

        if (playerIsland == null) {
            // Player doesn't have an island - initiate premium claim
            player.closeInventory();
            IslandCreateListener createListener = gui.getAddon().getIslandCreateListener();
            createListener.onLocationSelected(player, coord);
            new ConfirmationGUI(gui.getAddon(), player, coord, createListener,
                ConfirmationGUI.ActionType.PURCHASE, price).open();
        } else {
            // Player already has island - can't purchase another location for new island
            player.sendMessage("\u00A7cYou already have an island. Use relocation to move.");
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        // Prevent dragging in our GUI
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof MainGridGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Unregister listener when GUI closes
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof MainGridGUI && holder.equals(gui)) {
            HandlerList.unregisterAll(this);
        }
    }
}
