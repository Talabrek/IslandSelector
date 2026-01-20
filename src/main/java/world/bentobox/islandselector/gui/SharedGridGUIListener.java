package world.bentobox.islandselector.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.listeners.IslandCreateListener;
import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Shared listener for all MainGridGUI instances.
 * Registered once in IslandSelector.onEnable() to avoid memory leaks.
 * Routes events to the appropriate GUI instance via InventoryHolder pattern.
 */
public class SharedGridGUIListener implements Listener {

    private final IslandSelector addon;

    public SharedGridGUIListener(IslandSelector addon) {
        this.addon = addon;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof MainGridGUI)) {
            return;
        }

        MainGridGUI gui = (MainGridGUI) holder;

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
        if (slot == gui.getSearchSlot()) {
            player.closeInventory();
            gui.getAddon().getSearchListener().startSearch(player, gui);
            return;
        }
        if (slot == gui.getFindMyIslandSlot()) {
            gui.findMyIsland();
            return;
        }
        if (slot == gui.getSlotsSlot()) {
            if (gui.getAddon().getSettings().isSlotsEnabled() && gui.getAddon().isSchematicOperationsAvailable()) {
                player.closeInventory();
                new SlotSelectionGUI(gui.getAddon(), player).open();
            }
            return;
        }
        if (slot == gui.getCloseSlot()) {
            player.closeInventory();
            return;
        }

        // Handle grid slot clicks
        GridCoordinate coord = gui.getCoordinateForSlot(slot);
        if (coord != null) {
            handleGridClick(gui, player, coord, event.isRightClick());
        }
    }

    /**
     * Handle click on a grid location
     */
    private void handleGridClick(MainGridGUI gui, Player player, GridCoordinate coord, boolean rightClick) {
        var gridManager = gui.getAddon().getGridManager();
        var status = gridManager.getLocationStatus(coord);

        switch (status) {
            case AVAILABLE:
                handleAvailableClick(gui, player, coord);
                break;
            case OCCUPIED:
                if (rightClick) {
                    handleVisitClick(gui, player, coord);
                } else {
                    handleViewIslandInfo(gui, player, coord);
                }
                break;
            case RESERVED:
                var location = gridManager.getGridLocation(coord);
                if (location != null && location.isPurchasable()) {
                    handlePurchaseClick(gui, player, coord, location.getPurchasePrice());
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
    private void handleAvailableClick(MainGridGUI gui, Player player, GridCoordinate coord) {
        var gridManager = gui.getAddon().getGridManager();
        var playerIsland = gridManager.getPlayerIslandCoordinate(player.getUniqueId());
        IslandCreateListener createListener = gui.getAddon().getIslandCreateListener();

        if (playerIsland == null) {
            player.closeInventory();
            createListener.onLocationSelected(player, coord);
            new ConfirmationGUI(gui.getAddon(), player, coord, createListener, ConfirmationGUI.ActionType.CLAIM).open();
        } else {
            if (!gui.getAddon().isSchematicOperationsAvailable()) {
                return;
            }
            player.closeInventory();
            double relocCost = gui.getAddon().getSettings().getRelocationCost();
            new ConfirmationGUI(gui.getAddon(), player, coord, createListener,
                ConfirmationGUI.ActionType.RELOCATE, relocCost).open();
        }
    }

    /**
     * Handle right-click to visit an island
     */
    private void handleVisitClick(MainGridGUI gui, Player player, GridCoordinate coord) {
        var gridManager = gui.getAddon().getGridManager();
        gridManager.getLocationStatus(coord);
        var location = gridManager.getGridLocation(coord);

        if (location == null || location.getOwnerUUID() == null) {
            player.sendMessage("\u00A7cThis island doesn't exist.");
            return;
        }

        if (location.getOwnerUUID().equals(player.getUniqueId())) {
            player.closeInventory();
            player.sendMessage("\u00A7eTeleporting to your island...");
            world.bentobox.bentobox.BentoBox.getInstance().getIslandsManager()
                .homeTeleportAsync(gridManager.getBSkyBlockWorld(), player);
            return;
        }

        var islandsManager = world.bentobox.bentobox.BentoBox.getInstance().getIslandsManager();
        var island = islandsManager.getIsland(gridManager.getBSkyBlockWorld(), location.getOwnerUUID());
        if (island == null) {
            player.sendMessage("\u00A7cCould not find island data.");
            return;
        }

        if (island.isBanned(player.getUniqueId())) {
            player.sendMessage("\u00A7cYou are banned from this island!");
            return;
        }

        String ownerName = location.getOwnerName() != null ? location.getOwnerName() : "Unknown";

        org.bukkit.Location targetLoc = island.getSpawnPoint(org.bukkit.World.Environment.NORMAL);
        if (targetLoc == null) {
            targetLoc = island.getProtectionCenter();
        }
        if (targetLoc == null) {
            targetLoc = island.getCenter();
        }

        if (targetLoc != null) {
            player.closeInventory();
            player.sendMessage("\u00A7aTeleporting to " + ownerName + "'s island...");
            new SafeSpotTeleport.Builder(gui.getAddon().getPlugin())
                .entity(player)
                .location(targetLoc)
                .ifFail(() -> player.sendMessage("\u00A7cCould not find a safe spot on the island!"))
                .buildFuture();
        } else {
            player.sendMessage("\u00A7cCould not find a safe location on this island.");
        }
    }

    /**
     * Handle left-click to view island info
     */
    private void handleViewIslandInfo(MainGridGUI gui, Player player, GridCoordinate coord) {
        var gridManager = gui.getAddon().getGridManager();
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
    private void handlePurchaseClick(MainGridGUI gui, Player player, GridCoordinate coord, double price) {
        var gridManager = gui.getAddon().getGridManager();
        var playerIsland = gridManager.getPlayerIslandCoordinate(player.getUniqueId());

        if (playerIsland == null) {
            player.closeInventory();
            IslandCreateListener createListener = gui.getAddon().getIslandCreateListener();
            createListener.onLocationSelected(player, coord);
            new ConfirmationGUI(gui.getAddon(), player, coord, createListener,
                ConfirmationGUI.ActionType.PURCHASE, price).open();
        } else {
            if (!gui.getAddon().isSchematicOperationsAvailable()) {
                return;
            }
            player.closeInventory();
            IslandCreateListener createListener = gui.getAddon().getIslandCreateListener();
            new ConfirmationGUI(gui.getAddon(), player, coord, createListener,
                ConfirmationGUI.ActionType.PREMIUM_RELOCATE, price).open();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof MainGridGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof MainGridGUI) {
            MainGridGUI gui = (MainGridGUI) holder;
            gui.handleClose();
        }
    }
}
