package world.bentobox.islandselector.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.UUID;

/**
 * Shared listener for all AdminGridGUI instances.
 * Registered once in IslandSelector.onEnable() to avoid memory leaks.
 * Routes events to the appropriate GUI instance via InventoryHolder pattern.
 */
public class SharedAdminGridGUIListener implements Listener {

    private final IslandSelector addon;
    private final GridManager gridManager;

    public SharedAdminGridGUIListener(IslandSelector addon) {
        this.addon = addon;
        this.gridManager = addon.getGridManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AdminGridGUI)) {
            return;
        }

        AdminGridGUI gui = (AdminGridGUI) holder;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (!player.equals(gui.getPlayer())) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        ClickType clickType = event.getClick();
        boolean isShift = clickType.isShiftClick();
        boolean isRight = clickType.isRightClick();
        boolean isLeft = clickType.isLeftClick();

        // Navigation arrows
        if (gui.isScrollUpSlot(slot)) {
            gui.scrollUp(isShift);
            return;
        }
        if (gui.isScrollDownSlot(slot)) {
            gui.scrollDown(isShift);
            return;
        }
        if (gui.isScrollLeftSlot(slot)) {
            gui.scrollLeft(isShift);
            return;
        }
        if (gui.isScrollRightSlot(slot)) {
            gui.scrollRight(isShift);
            return;
        }

        // Filter buttons
        if (slot == gui.getFilterAllSlot()) {
            gui.setFilter(AdminGridGUI.FilterType.ALL);
            return;
        }
        if (slot == gui.getFilterOccupiedSlot()) {
            gui.setFilter(AdminGridGUI.FilterType.OCCUPIED);
            return;
        }
        if (slot == gui.getFilterReservedSlot()) {
            gui.setFilter(AdminGridGUI.FilterType.RESERVED);
            return;
        }

        // Bottom control buttons
        if (slot == gui.getSearchSlot()) {
            handleSearch(gui, player);
            return;
        }
        if (slot == gui.getJumpSlot()) {
            handleJumpToCoordinate(gui, player);
            return;
        }
        if (slot == gui.getReloadSlot()) {
            handleReload(gui, player);
            return;
        }
        if (slot == gui.getBackupSlot()) {
            handleBackupAll(player);
            return;
        }
        if (slot == gui.getCloseSlot()) {
            if (gui.isRelocationMode()) {
                player.sendMessage("§cRelocation cancelled.");
                gui.exitRelocationMode();
            } else {
                player.closeInventory();
            }
            return;
        }

        // Grid slot clicked
        GridCoordinate coord = gui.getCoordinateForSlot(slot);
        if (coord != null) {
            handleGridClick(gui, player, coord, isLeft, isRight, isShift);
        }
    }

    private void handleGridClick(AdminGridGUI gui, Player player, GridCoordinate coord,
                                  boolean isLeft, boolean isRight, boolean isShift) {
        if (!gridManager.isWithinBounds(coord)) {
            player.sendMessage("§7This location is outside the grid boundaries.");
            return;
        }

        if (gui.isRelocationMode()) {
            handleRelocationModeClick(gui, player, coord, isShift);
            return;
        }

        GridLocation.Status status = gridManager.getLocationStatus(coord);
        GridLocation location = gridManager.getGridLocation(coord);

        switch (status) {
            case AVAILABLE:
                handleAvailableClick(gui, player, coord, isLeft, isRight);
                break;
            case OCCUPIED:
                handleOccupiedClick(gui, player, coord, location, isLeft, isRight, isShift);
                break;
            case RESERVED:
                handleReservedClick(gui, player, coord, location, isLeft, isRight, isShift);
                break;
            default:
                player.sendMessage("§7Cannot interact with this location.");
                break;
        }
    }

    private void handleRelocationModeClick(AdminGridGUI gui, Player player, GridCoordinate coord, boolean isShift) {
        if (isShift) {
            player.sendMessage("§cRelocation cancelled.");
            gui.exitRelocationMode();
            return;
        }

        GridLocation.Status status = gridManager.getLocationStatus(coord);

        if (status == GridLocation.Status.OCCUPIED) {
            player.sendMessage("§cCannot relocate to an occupied location.");
            return;
        }

        if (status == GridLocation.Status.LOCKED) {
            player.sendMessage("§cCannot relocate to a locked location.");
            return;
        }

        if (coord.equals(gui.getRelocationSource())) {
            player.sendMessage("§cCannot relocate to the same location.");
            return;
        }

        UUID targetPlayerUUID = gui.getRelocationPlayerUUID();
        String targetPlayerName = gui.getRelocationPlayerName();
        GridCoordinate sourceCoord = gui.getRelocationSource();

        player.sendMessage("§eRelocating " + targetPlayerName + "'s island from " +
                          sourceCoord.toString() + " to " + coord.toString() + "...");

        addon.getRelocationManager().adminRelocateIsland(player, targetPlayerUUID, targetPlayerName, sourceCoord, coord);
        gui.exitRelocationMode();
    }

    private void handleAvailableClick(AdminGridGUI gui, Player player, GridCoordinate coord,
                                       boolean isLeft, boolean isRight) {
        if (isLeft) {
            gridManager.reserveLocation(coord, true);
            player.sendMessage("§aReserved location §f" + coord + " §a(blocked)");
            gui.refresh();
        } else if (isRight) {
            player.closeInventory();
            player.sendMessage("§e§lSet Premium Price");
            player.sendMessage("§7Enter the price for §f" + coord + "§7:");
            player.sendMessage("§7Type the price in chat, or §ccancel §7to abort.");
            addon.getSearchListener().setPendingAdminPriceSet(player.getUniqueId(), coord);
        }
    }

    private void handleOccupiedClick(AdminGridGUI gui, Player player, GridCoordinate coord,
                                      GridLocation location, boolean isLeft, boolean isRight, boolean isShift) {
        UUID ownerUUID = location != null ? location.getOwnerUUID() : null;
        String ownerName = location != null && location.getOwnerName() != null ? location.getOwnerName() : "Unknown";

        if (isShift) {
            if (ownerUUID != null) {
                Island island = BentoBox.getInstance().getIslands()
                    .getIsland(gridManager.getBSkyBlockWorld(), ownerUUID);
                if (island != null) {
                    player.sendMessage("§aTeleporting to §f" + ownerName + "§a's island at §f" + coord + "§a...");
                    new SafeSpotTeleport.Builder(addon.getPlugin())
                        .entity(player)
                        .island(island)
                        .ifFail(() -> player.sendMessage("§cCould not find a safe teleport location."))
                        .buildFuture();
                } else {
                    player.sendMessage("§cIsland data not found in BentoBox.");
                }
            }
        } else if (isLeft || isRight) {
            player.closeInventory();
            new AdminPlayerActionsGUI(addon, player, coord, location).open();
        }
    }

    private void handleReservedClick(AdminGridGUI gui, Player player, GridCoordinate coord,
                                      GridLocation location, boolean isLeft, boolean isRight, boolean isShift) {
        boolean isPurchasable = location != null && location.isPurchasable();

        if (isShift) {
            gridManager.unreserveLocation(coord);
            player.sendMessage("§aUnreserved location §f" + coord);
            gui.refresh();
        } else if (isPurchasable) {
            if (isLeft) {
                player.closeInventory();
                player.sendMessage("§e§lChange Premium Price");
                player.sendMessage("§7Current price: §a$" + String.format("%,d", (int) location.getPurchasePrice()));
                player.sendMessage("§7Enter new price for §f" + coord + "§7:");
                player.sendMessage("§7Type the price in chat, or §ccancel §7to abort.");
                addon.getSearchListener().setPendingAdminPriceSet(player.getUniqueId(), coord);
            } else if (isRight) {
                gridManager.reserveLocation(coord, true);
                player.sendMessage("§aRemoved price from §f" + coord + " §a(now blocked)");
                gui.refresh();
            }
        } else {
            if (isLeft) {
                gridManager.unreserveLocation(coord);
                player.sendMessage("§aUnreserved location §f" + coord);
                gui.refresh();
            } else if (isRight) {
                player.closeInventory();
                player.sendMessage("§e§lSet Premium Price");
                player.sendMessage("§7Enter the price for §f" + coord + "§7:");
                player.sendMessage("§7Type the price in chat, or §ccancel §7to abort.");
                addon.getSearchListener().setPendingAdminPriceSet(player.getUniqueId(), coord);
            }
        }
    }

    private void handleSearch(AdminGridGUI gui, Player player) {
        player.closeInventory();
        player.sendMessage("§e§lAdmin Search");
        player.sendMessage("§7Enter a player name to find their island:");
        player.sendMessage("§7Type the name in chat, or §ccancel §7to abort.");
        addon.getSearchListener().setPendingAdminSearch(player.getUniqueId(), gui);
    }

    private void handleJumpToCoordinate(AdminGridGUI gui, Player player) {
        player.closeInventory();
        player.sendMessage("§d§lJump to Coordinate");
        player.sendMessage("§7Enter grid coordinates (e.g., §f0,0 §7or §f-5,3§7):");
        player.sendMessage("§7Type the coordinates in chat, or §ccancel §7to abort.");
        addon.getSearchListener().setPendingAdminJump(player.getUniqueId(), gui);
    }

    private void handleReload(AdminGridGUI gui, Player player) {
        player.sendMessage("§eSyncing grid with BSkyBlock...");
        gridManager.syncWithBSkyBlock();
        player.sendMessage("§aGrid sync complete!");
        gui.refresh();
    }

    private void handleBackupAll(Player player) {
        player.sendMessage("§eCreating backups for all online players...");

        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            var activeSlot = addon.getSlotManager().getActiveSlot(online.getUniqueId());
            if (activeSlot != null && activeSlot.hasIsland()) {
                boolean success = addon.getBackupManager().createAutoBackup(
                    online.getUniqueId(), activeSlot.getSlotNumber());
                if (success) count++;
            }
        }

        player.sendMessage("§aCreated §f" + count + " §abackups.");
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof AdminGridGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof AdminGridGUI) {
            AdminGridGUI gui = (AdminGridGUI) holder;
            gui.handleClose();
        }
    }
}
