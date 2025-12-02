package world.bentobox.islandselector.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.ArrayList;
import java.util.List;

/**
 * Confirmation GUI for slot deletion
 */
public class SlotDeleteConfirmationGUI implements InventoryHolder, Listener {

    private static final int SIZE = 27;
    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;

    private final IslandSelector addon;
    private final Player player;
    private final SlotData slot;
    private Inventory inventory;

    public SlotDeleteConfirmationGUI(IslandSelector addon, Player player, SlotData slot) {
        this.addon = addon;
        this.player = player;
        this.slot = slot;
    }

    /**
     * Create and open the GUI
     */
    public void open() {
        createInventory();
        populateInventory();
        player.openInventory(inventory);
        Bukkit.getPluginManager().registerEvents(this, addon.getPlugin());
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(this, SIZE, "Confirm Slot Deletion");
    }

    private void populateInventory() {
        inventory.clear();

        // Warning/info item
        ItemStack info = new ItemStack(Material.BARRIER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(colorize("&c&lWARNING: Delete Slot"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Slot: &f#" + slot.getSlotNumber() + " - " + slot.getSlotName()));
        lore.add("");
        lore.add(colorize("&c&lThis action is PERMANENT!"));
        lore.add(colorize("&7Your island and all progress"));
        lore.add(colorize("&7in this slot will be lost forever!"));
        lore.add("");

        // Check if this is the only slot
        int totalSlots = addon.getSlotManager().getIslandCount(player.getUniqueId());
        if (totalSlots == 1) {
            lore.add(colorize("&e⚠ This is your only slot!"));
            lore.add(colorize("&7Deleting it will remove your"));
            lore.add(colorize("&7island from the grid completely."));
        } else {
            lore.add(colorize("&7You have " + totalSlots + " total slots."));
            lore.add(colorize("&7After deletion, you will have " + (totalSlots - 1) + " remaining."));
        }

        infoMeta.setLore(lore);
        info.setItemMeta(infoMeta);
        inventory.setItem(4, info);

        // Confirm button
        ItemStack confirm = new ItemStack(Material.RED_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(colorize("&c&lDELETE SLOT"));

        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(colorize("&7Click to permanently delete"));
        confirmLore.add(colorize("&7this slot and its island"));
        confirmLore.add("");
        confirmLore.add(colorize("&c&lTHIS CANNOT BE UNDONE!"));

        confirmMeta.setLore(confirmLore);
        confirm.setItemMeta(confirmMeta);
        inventory.setItem(CONFIRM_SLOT, confirm);

        // Cancel button
        ItemStack cancel = new ItemStack(Material.LIME_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(colorize("&a&lCANCEL"));

        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(colorize("&7Keep your slot safe"));
        cancelLore.add(colorize("&7Return to slot selection"));

        cancelMeta.setLore(cancelLore);
        cancel.setItemMeta(cancelMeta);
        inventory.setItem(CANCEL_SLOT, cancel);

        // Fill empty slots
        fillEmptySlots();
    }

    private void fillEmptySlots() {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for (int i = 0; i < SIZE; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    private String colorize(String text) {
        return text.replace("&", "\u00A7");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SlotDeleteConfirmationGUI)) {
            return;
        }

        SlotDeleteConfirmationGUI clickedGUI = (SlotDeleteConfirmationGUI) event.getInventory().getHolder();
        if (!clickedGUI.equals(this)) {
            return;
        }

        event.setCancelled(true);

        if (event.getRawSlot() < 0 || event.getRawSlot() >= SIZE) {
            return;
        }

        int clickedSlot = event.getRawSlot();
        Player clicker = (Player) event.getWhoClicked();

        if (clickedSlot == CONFIRM_SLOT) {
            handleConfirm(clicker);
        } else if (clickedSlot == CANCEL_SLOT) {
            handleCancel(clicker);
        }
    }

    private void handleConfirm(Player player) {
        player.closeInventory();

        // Get the slot number and UUID
        int slotNumber = slot.getSlotNumber();
        int totalSlots = addon.getSlotManager().getIslandCount(player.getUniqueId());
        SlotData activeSlotData = addon.getSlotManager().getActiveSlot(player.getUniqueId());
        int activeSlot = activeSlotData != null ? activeSlotData.getSlotNumber() : -1;

        player.sendMessage(colorize("&cDeleting slot #" + slotNumber + "..."));

        // If this is the only slot, delete the island from BSkyBlock
        if (totalSlots == 1) {
            // Get the island from BSkyBlock
            GridManager gridManager = addon.getGridManager();
            Island island = addon.getIslands().getIsland(gridManager.getBSkyBlockWorld(), player.getUniqueId());

            if (island != null) {
                // Delete island from BSkyBlock (this also removes from world)
                addon.getIslands().deleteIsland(island, true, player.getUniqueId());
                player.sendMessage(colorize("&7Your island has been deleted from the world."));
            }

            // Remove from grid
            GridCoordinate playerCoord = gridManager.getPlayerIslandCoordinate(player.getUniqueId());
            if (playerCoord != null) {
                gridManager.clearLocation(playerCoord);
                player.sendMessage(colorize("&7Your grid location is now available."));
            }
        } else if (slotNumber == activeSlot) {
            // Deleting the active slot - need to switch to another slot first
            player.sendMessage(colorize("&eThis is your active slot. Finding another slot to switch to..."));

            // Find the first available slot that isn't the one being deleted
            SlotData targetSlot = null;
            for (int i = 1; i <= addon.getSettings().getMaxSlots(); i++) {
                if (i != slotNumber) {
                    SlotData candidateSlot = addon.getSlotManager().getSlot(player.getUniqueId(), i);
                    if (candidateSlot != null) {
                        targetSlot = candidateSlot;
                        break;
                    }
                }
            }

            if (targetSlot != null) {
                // Switch to the target slot
                player.sendMessage(colorize("&7Switching to slot #" + targetSlot.getSlotNumber() + "..."));
                addon.getSlotSwitchManager().switchSlot(player, slot, targetSlot);

                // Schedule deletion after switch completes (give it 5 seconds)
                final int finalSlotNumber = slotNumber;
                Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
                    addon.getSlotManager().deleteSlot(player.getUniqueId(), finalSlotNumber);
                    player.sendMessage(colorize("&aSlot #" + finalSlotNumber + " has been deleted!"));
                    player.sendMessage(colorize("&7You now have " + (totalSlots - 1) + " slot(s) remaining."));
                }, 100L); // 5 seconds
            }
        } else {
            // Not the active slot - can delete immediately
            addon.getSlotManager().deleteSlot(player.getUniqueId(), slotNumber);
            player.sendMessage(colorize("&aSlot #" + slotNumber + " has been deleted!"));
            player.sendMessage(colorize("&7You now have " + (totalSlots - 1) + " slot(s) remaining."));
        }
    }

    private void handleCancel(Player player) {
        player.closeInventory();
        player.sendMessage(colorize("&7Slot deletion cancelled."));
        new SlotSelectionGUI(addon, player).open();
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SlotDeleteConfirmationGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof SlotDeleteConfirmationGUI &&
            event.getInventory().getHolder().equals(this)) {
            HandlerList.unregisterAll(this);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
