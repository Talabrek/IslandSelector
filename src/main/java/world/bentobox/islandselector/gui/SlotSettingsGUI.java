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
 * Slot Settings GUI for managing individual slot settings
 * Opened by right-clicking a slot in the Slot Selection GUI
 * 27 slots (3 rows x 9 columns) layout
 */
public class SlotSettingsGUI implements InventoryHolder, Listener {

    private static final int SIZE = 27;

    // Slot display positions
    private static final int SLOT_PREVIEW = 4; // Center of first row
    private static final int RENAME_BUTTON = 11; // Left button
    private static final int CHANGE_ICON_BUTTON = 13; // Middle button
    private static final int DELETE_BUTTON = 15; // Right button
    private static final int BACK_BUTTON = 22; // Bottom row

    private final IslandSelector addon;
    private final Player player;
    private final SlotData slotData;
    private Inventory inventory;

    public SlotSettingsGUI(IslandSelector addon, Player player, SlotData slotData) {
        this.addon = addon;
        this.player = player;
        this.slotData = slotData;
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
        String title = colorize("&8Slot Settings: &f" + slotData.getSlotName());
        inventory = Bukkit.createInventory(this, SIZE, title);
    }

    private void populateInventory() {
        inventory.clear();

        // Slot preview (center of first row)
        inventory.setItem(SLOT_PREVIEW, createSlotPreview());

        // Rename button
        inventory.setItem(RENAME_BUTTON, createRenameButton());

        // Change icon button
        inventory.setItem(CHANGE_ICON_BUTTON, createChangeIconButton());

        // Delete button
        inventory.setItem(DELETE_BUTTON, createDeleteButton());

        // Back button
        inventory.setItem(BACK_BUTTON, createBackButton());

        // Fill empty slots
        fillEmptySlots();
    }

    private ItemStack createSlotPreview() {
        // Use the slot's custom icon
        Material iconMaterial = Material.matchMaterial(slotData.getIconMaterial());
        if (iconMaterial == null) {
            iconMaterial = Material.GRASS_BLOCK;
        }

        ItemStack item = new ItemStack(iconMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&e&l" + slotData.getSlotName()));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Slot #" + slotData.getSlotNumber()));
        lore.add("");
        if (slotData.isActive()) {
            lore.add(colorize("&a✔ Currently Active"));
        } else {
            lore.add(colorize("&7Inactive"));
        }
        lore.add("");
        lore.add(colorize("&7Configure this slot using"));
        lore.add(colorize("&7the buttons below."));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRenameButton() {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&e&lRename Slot"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Current name: &f" + slotData.getSlotName()));
        lore.add("");
        lore.add(colorize("&eClick to rename this slot"));
        lore.add("");
        lore.add(colorize("&7Use command:"));
        lore.add(colorize("&f/islandselector setname <slot> <name>"));
        lore.add("");
        lore.add(colorize("&7Example:"));
        lore.add(colorize("&f/islandselector setname " + slotData.getSlotNumber() + " &aMain Base"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createChangeIconButton() {
        ItemStack item = new ItemStack(Material.ITEM_FRAME);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&e&lChange Icon"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Current icon: &f" + slotData.getIconMaterial()));
        lore.add("");
        lore.add(colorize("&eClick to change slot icon"));
        lore.add("");
        lore.add(colorize("&7Use command:"));
        lore.add(colorize("&f/islandselector seticon <slot>"));
        lore.add("");
        lore.add(colorize("&7Hold an item and run:"));
        lore.add(colorize("&f/islandselector seticon " + slotData.getSlotNumber()));
        lore.add("");
        lore.add(colorize("&7The held item will become"));
        lore.add(colorize("&7your slot's icon!"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDeleteButton() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&c&lDelete Slot"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Delete this slot permanently."));
        lore.add("");
        lore.add(colorize("&c&l⚠ WARNING ⚠"));
        lore.add(colorize("&cThis cannot be undone!"));
        lore.add(colorize("&cAll progress will be lost!"));
        lore.add("");
        lore.add(colorize("&eClick to delete this slot"));
        lore.add("");
        lore.add(colorize("&7Use command:"));
        lore.add(colorize("&f/islandselector delete " + slotData.getSlotNumber()));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&e← Back to Slots"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Return to slot selection"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillEmptySlots() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
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
        if (!(event.getInventory().getHolder() instanceof SlotSettingsGUI)) {
            return;
        }

        SlotSettingsGUI clickedGUI = (SlotSettingsGUI) event.getInventory().getHolder();
        if (!clickedGUI.equals(this)) {
            return;
        }

        event.setCancelled(true);

        if (event.getRawSlot() < 0 || event.getRawSlot() >= SIZE) {
            return;
        }

        int slot = event.getRawSlot();
        Player clicker = (Player) event.getWhoClicked();

        if (slot == BACK_BUTTON) {
            clicker.closeInventory();
            new SlotSelectionGUI(addon, clicker).open();
            return;
        }

        if (slot == RENAME_BUTTON) {
            clicker.closeInventory();
            clicker.sendMessage(colorize("&eUse command: &f/islandselector setname " + slotData.getSlotNumber() + " <name>"));
            clicker.sendMessage(colorize("&7Example: &f/islandselector setname " + slotData.getSlotNumber() + " Main Base"));
            return;
        }

        if (slot == CHANGE_ICON_BUTTON) {
            clicker.closeInventory();
            clicker.sendMessage(colorize("&eHold an item and use: &f/islandselector seticon " + slotData.getSlotNumber()));
            clicker.sendMessage(colorize("&7The item you're holding will become your slot's icon!"));
            return;
        }

        if (slot == DELETE_BUTTON) {
            clicker.closeInventory();
            clicker.sendMessage(colorize("&eUse command: &f/islandselector delete " + slotData.getSlotNumber()));
            clicker.sendMessage(colorize("&c&lWarning: This will permanently delete the slot!"));
            return;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SlotSettingsGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof SlotSettingsGUI &&
            event.getInventory().getHolder().equals(this)) {
            HandlerList.unregisterAll(this);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
