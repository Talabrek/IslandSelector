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

import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;

import java.util.ArrayList;
import java.util.List;

/**
 * Confirmation GUI for slot switching
 */
public class SlotSwitchConfirmationGUI implements InventoryHolder, Listener {

    private static final int SIZE = 27;
    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;

    private final IslandSelector addon;
    private final Player player;
    private final SlotData fromSlot;
    private final SlotData toSlot;
    private Inventory inventory;

    public SlotSwitchConfirmationGUI(IslandSelector addon, Player player, SlotData fromSlot, SlotData toSlot) {
        this.addon = addon;
        this.player = player;
        this.fromSlot = fromSlot;
        this.toSlot = toSlot;
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
        inventory = Bukkit.createInventory(this, SIZE, "Confirm Slot Switch");
    }

    private void populateInventory() {
        inventory.clear();

        // Info item
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(colorize("&eSwitch Island Slots"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7From: &f" + fromSlot.getSlotName()));
        lore.add(colorize("&7To: &f" + toSlot.getSlotName()));
        lore.add("");
        lore.add(colorize("&7Your current island will be saved"));
        lore.add(colorize("&7and the target slot will be loaded."));

        // Check cooldown
        long remainingCooldown = addon.getSlotManager().getRemainingCooldown(player.getUniqueId());
        if (remainingCooldown > 0 && !player.hasPermission("islandselector.bypass.cooldown.switch")) {
            lore.add("");
            lore.add(colorize("&c⚠ Cooldown: " + formatTime(remainingCooldown)));
        }

        infoMeta.setLore(lore);
        info.setItemMeta(infoMeta);
        inventory.setItem(4, info);

        // Confirm button
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(colorize("&a&lCONFIRM"));

        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(colorize("&7Click to switch slots"));

        confirmMeta.setLore(confirmLore);
        confirm.setItemMeta(confirmMeta);
        inventory.setItem(CONFIRM_SLOT, confirm);

        // Cancel button
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(colorize("&c&lCANCEL"));

        List<String> cancelLore = new ArrayList<>();
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

    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + " minute" + (minutes == 1 ? "" : "s");
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            return hours + " hour" + (hours == 1 ? "" : "s");
        } else {
            long days = seconds / 86400;
            long hours = (seconds % 86400) / 3600;
            return days + " day" + (days == 1 ? "" : "s") + (hours > 0 ? ", " + hours + " hour" + (hours == 1 ? "" : "s") : "");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SlotSwitchConfirmationGUI)) {
            return;
        }

        SlotSwitchConfirmationGUI clickedGUI = (SlotSwitchConfirmationGUI) event.getInventory().getHolder();
        if (!clickedGUI.equals(this)) {
            return;
        }

        event.setCancelled(true);

        if (event.getRawSlot() < 0 || event.getRawSlot() >= SIZE) {
            return;
        }

        int slot = event.getRawSlot();
        Player clicker = (Player) event.getWhoClicked();

        if (slot == CONFIRM_SLOT) {
            handleConfirm(clicker);
        } else if (slot == CANCEL_SLOT) {
            handleCancel(clicker);
        }
    }

    private void handleConfirm(Player player) {
        // Check cooldown (unless bypassed)
        if (!player.hasPermission("islandselector.bypass.cooldown.switch")) {
            long remaining = addon.getSlotManager().getRemainingCooldown(player.getUniqueId());
            if (remaining > 0) {
                player.sendMessage(colorize("&cYou must wait " + formatTime(remaining) + " before switching slots again."));
                player.closeInventory();
                return;
            }
        }

        player.closeInventory();
        player.sendMessage(colorize("&eSlot switching coming soon..."));
        // TODO: Implement actual slot switching with FAWE
    }

    private void handleCancel(Player player) {
        player.closeInventory();
        new SlotSelectionGUI(addon, player).open();
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SlotSwitchConfirmationGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof SlotSwitchConfirmationGUI &&
            event.getInventory().getHolder().equals(this)) {
            HandlerList.unregisterAll(this);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
