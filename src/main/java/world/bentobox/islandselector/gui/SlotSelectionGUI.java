package world.bentobox.islandselector.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.Settings;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.managers.SlotManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Slot Selection GUI for managing island slots
 * 27 slots (3 rows x 9 columns) layout
 */
public class SlotSelectionGUI implements InventoryHolder, Listener {

    private static final int SIZE = 27;
    private static final int BACK_SLOT = 20;
    private static final int CLOSE_SLOT = 24;

    // Slot display positions (center row)
    private static final int[] SLOT_POSITIONS = {2, 3, 4, 5, 6};

    private final IslandSelector addon;
    private final Player player;
    private final Settings settings;
    private Inventory inventory;

    public SlotSelectionGUI(IslandSelector addon, Player player) {
        this.addon = addon;
        this.player = player;
        this.settings = addon.getSettings();
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
        inventory = Bukkit.createInventory(this, SIZE, "Your Island Slots");
    }

    private void populateInventory() {
        inventory.clear();

        // Get player's available slot count
        int maxSlots = getPlayerMaxSlots();
        SlotManager slotManager = addon.getSlotManager();

        // Populate slot displays
        for (int i = 0; i < 5; i++) {
            int slotNumber = i + 1;
            int position = SLOT_POSITIONS[i];

            if (slotNumber <= maxSlots) {
                // Get slot data from manager
                SlotData slotData = slotManager.getSlot(player.getUniqueId(), slotNumber);

                if (slotData != null && slotData.isActive()) {
                    // Active slot
                    inventory.setItem(position, createActiveSlotItem(slotData));
                } else if (slotData != null && slotData.hasIsland()) {
                    // Inactive slot with island
                    inventory.setItem(position, createInactiveSlotItem(slotData));
                } else {
                    // Empty slot (unlocked but no island)
                    inventory.setItem(position, createEmptySlotItem(slotNumber));
                }
            } else {
                // Locked slot
                inventory.setItem(position, createLockedSlotItem(slotNumber));
            }
        }

        // Control buttons
        inventory.setItem(BACK_SLOT, createButton(Material.ARROW, "&eBack to Grid", "&7Return to grid view"));
        inventory.setItem(CLOSE_SLOT, createButton(Material.BARRIER, "&cClose", "&7Close this menu"));

        // Fill empty slots
        fillEmptySlots();
    }

    private int getPlayerMaxSlots() {
        // Check permissions for slot count
        if (player.hasPermission("islandselector.slots.5")) return 5;
        if (player.hasPermission("islandselector.slots.4")) return 4;
        if (player.hasPermission("islandselector.slots.3")) return 3;
        if (player.hasPermission("islandselector.slots.2")) return 2;
        return settings.getDefaultSlots();
    }

    private ItemStack createActiveSlotItem(SlotData slotData) {
        // Use custom icon if set
        Material iconMaterial = Material.matchMaterial(slotData.getIconMaterial());
        if (iconMaterial == null) {
            iconMaterial = Material.GRASS_BLOCK;
        }

        ItemStack item = new ItemStack(iconMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&a&l" + slotData.getSlotName()));

        // Add glow
        Enchantment glow = org.bukkit.Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft("unbreaking"));
        if (glow != null) {
            meta.addEnchant(glow, 1, true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Level: &f0")); // TODO: Get from Level addon
        lore.add(colorize("&7Members: &f1")); // TODO: Get from island data
        lore.add("");
        lore.add(colorize("&e★ ACTIVE"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInactiveSlotItem(SlotData slotData) {
        // Use custom icon if set
        Material iconMaterial = Material.matchMaterial(slotData.getIconMaterial());
        if (iconMaterial == null) {
            iconMaterial = Material.GRASS_BLOCK;
        }

        ItemStack item = new ItemStack(iconMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&f" + slotData.getSlotName()));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Level: &f0")); // TODO: Get from Level addon
        lore.add(colorize("&7Members: &f1")); // TODO: Get from island data
        lore.add("");
        lore.add(colorize("&eClick to switch to this slot"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEmptySlotItem(int slotNumber) {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&aEmpty Slot"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7This slot is available!"));
        lore.add("");
        lore.add(colorize("&eClick to create a new island"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLockedSlotItem(int slotNumber) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&cLocked Slot"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7This slot is locked."));
        lore.add("");
        lore.add(colorize("&7Unlock with: &fislandselector.slots." + slotNumber));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createButton(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize(name));

        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(colorize(line));
        }
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
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

    private void handleSlotRightClick(Player clicker, int slotNumber) {
        int maxSlots = getPlayerMaxSlots();
        SlotManager slotManager = addon.getSlotManager();

        // Check if slot is locked
        if (slotNumber > maxSlots) {
            clicker.sendMessage(colorize("&cThis slot is locked! You cannot view settings for locked slots."));
            return;
        }

        // Get slot data
        SlotData targetSlot = slotManager.getSlot(clicker.getUniqueId(), slotNumber);

        // Check if slot has an island
        if (targetSlot == null || !targetSlot.hasIsland()) {
            clicker.sendMessage(colorize("&cThis slot is empty! Create an island first before viewing settings."));
            return;
        }

        // Open slot settings GUI
        clicker.closeInventory();
        new SlotSettingsGUI(addon, clicker, targetSlot).open();
    }

    private void handleSlotClick(Player clicker, int slotNumber) {
        int maxSlots = getPlayerMaxSlots();
        SlotManager slotManager = addon.getSlotManager();

        // Check if slot is locked
        if (slotNumber > maxSlots) {
            clicker.sendMessage(colorize("&cThis slot is locked!"));
            clicker.sendMessage(colorize("&7You need permission: &fislandselector.slots." + slotNumber));
            return;
        }

        // Get slot data
        SlotData targetSlot = slotManager.getSlot(clicker.getUniqueId(), slotNumber);
        SlotData activeSlot = slotManager.getActiveSlot(clicker.getUniqueId());

        // Check if clicking active slot
        if (targetSlot != null && targetSlot.isActive()) {
            clicker.sendMessage(colorize("&7This is already your active slot."));
            return;
        }

        // Check if slot has an island (inactive slot - switch)
        if (targetSlot != null && targetSlot.hasIsland()) {
            // Open switch confirmation GUI
            clicker.closeInventory();
            new SlotSwitchConfirmationGUI(addon, clicker, activeSlot, targetSlot).open();
            return;
        }

        // Empty slot - trigger island creation
        if (activeSlot == null || !activeSlot.hasIsland()) {
            // Player doesn't have any islands yet - can't create in empty slot
            clicker.sendMessage(colorize("&cYou must create your first island before using additional slots."));
            return;
        }

        // Start island creation flow for this slot
        clicker.closeInventory();
        clicker.sendMessage(colorize("&eIsland creation in empty slots coming soon..."));
        // TODO: Trigger BSkyBlock island creation and associate with this slot
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SlotSelectionGUI)) {
            return;
        }

        SlotSelectionGUI clickedGUI = (SlotSelectionGUI) event.getInventory().getHolder();
        if (!clickedGUI.equals(this)) {
            return;
        }

        event.setCancelled(true);

        if (event.getRawSlot() < 0 || event.getRawSlot() >= SIZE) {
            return;
        }

        int slot = event.getRawSlot();
        Player clicker = (Player) event.getWhoClicked();

        if (slot == BACK_SLOT) {
            clicker.closeInventory();
            new MainGridGUI(addon, clicker).open();
            return;
        }

        if (slot == CLOSE_SLOT) {
            clicker.closeInventory();
            return;
        }

        // Handle slot clicks
        for (int i = 0; i < SLOT_POSITIONS.length; i++) {
            if (slot == SLOT_POSITIONS[i]) {
                int slotNumber = i + 1;

                // Right-click opens settings GUI
                if (event.getClick() == ClickType.RIGHT) {
                    handleSlotRightClick(clicker, slotNumber);
                } else {
                    // Left-click does normal action (switch/create)
                    handleSlotClick(clicker, slotNumber);
                }
                break;
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SlotSelectionGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof SlotSelectionGUI &&
            event.getInventory().getHolder().equals(this)) {
            HandlerList.unregisterAll(this);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
