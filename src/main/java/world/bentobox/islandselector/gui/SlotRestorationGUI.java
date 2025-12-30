package world.bentobox.islandselector.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.listeners.IslandCreateListener;
import world.bentobox.islandselector.managers.IslandRemovalManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * GUI for homeless players to select which saved slot to restore.
 *
 * This GUI is shown when:
 * - Player uses /island but has no island in the world
 * - Player has saved slot data (their island was previously removed by admin)
 *
 * The player can:
 * - See all their slots with saved data
 * - Select a slot to restore
 * - After selecting a slot, they choose a grid location to place it
 */
public class SlotRestorationGUI implements InventoryHolder, Listener {

    private static final int SIZE = 27; // 3 rows

    private final IslandSelector addon;
    private final Player player;
    private final IslandCreateListener createListener;
    private final IslandRemovalManager removalManager;
    private Inventory inventory;
    private List<SlotData> slotsWithData;

    // Track which slot index maps to which inventory slot
    private final int[] slotMapping = new int[5]; // Max 5 slots

    public SlotRestorationGUI(IslandSelector addon, Player player, IslandCreateListener createListener) {
        this.addon = addon;
        this.player = player;
        this.createListener = createListener;
        this.removalManager = addon.getIslandRemovalManager();

        // Get slots with saved schematics
        this.slotsWithData = removalManager.getSlotsWithSchematics(player.getUniqueId());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        inventory = Bukkit.createInventory(this, SIZE, "§6§lRestore Your Island");
        populateInventory();

        // Register listener
        Bukkit.getPluginManager().registerEvents(this, addon.getPlugin());

        player.openInventory(inventory);
    }

    private void populateInventory() {
        // Fill with background
        ItemStack background = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, background);
        }

        // Title/info in top row
        inventory.setItem(4, createItem(Material.BEACON, "§e§lRestore Your Island",
            List.of(
                "§7Your island was removed but your",
                "§7progress has been saved!",
                "",
                "§fSelect a slot below to restore",
                "§fthen choose a new location."
            )));

        // Display slots with saved data in middle row
        // Center them based on how many we have
        int startSlot = 9 + (9 - Math.min(slotsWithData.size(), 5)) / 2; // Center in row 2

        for (int i = 0; i < slotsWithData.size() && i < 5; i++) {
            SlotData slot = slotsWithData.get(i);
            int invSlot = startSlot + i;
            slotMapping[i] = invSlot;

            ItemStack item = createSlotItem(slot);
            inventory.setItem(invSlot, item);
        }

        // Cancel button in bottom row
        inventory.setItem(22, createItem(Material.BARRIER, "§c§lCancel",
            List.of("§7Close this menu")));
    }

    private ItemStack createSlotItem(SlotData slot) {
        // Try to use the configured icon, or default to chest
        Material material = Material.CHEST;
        try {
            material = Material.valueOf(slot.getIconMaterial());
        } catch (Exception e) {
            // Keep default
        }

        List<String> lore = new ArrayList<>();
        lore.add("§7Slot " + slot.getSlotNumber());
        lore.add("");

        // Get schematic file info
        String schematicPath = addon.getSlotManager().getSlotSchematicPath(
            player.getUniqueId(), slot.getSlotNumber());
        File schematicFile = new File(schematicPath);

        if (schematicFile.exists()) {
            long sizeKB = schematicFile.length() / 1024;
            long lastModified = schematicFile.lastModified();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String dateStr = sdf.format(new Date(lastModified));

            lore.add("§aData saved!");
            lore.add("§7Size: §f" + sizeKB + " KB");
            lore.add("§7Saved: §f" + dateStr);
        } else {
            lore.add("§cNo saved data found");
        }

        // Blueprint info if available
        if (slot.getBlueprintBundle() != null && !slot.getBlueprintBundle().isEmpty()) {
            lore.add("");
            lore.add("§7Blueprint: §f" + slot.getBlueprintBundle());
        }

        lore.add("");
        lore.add("§e▶ Click to restore this slot");

        return createItem(material, "§6§l" + slot.getSlotName(), lore);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player clicker = (Player) event.getWhoClicked();
        if (!clicker.equals(player)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= SIZE) {
            return;
        }

        // Check if cancel button was clicked
        if (slot == 22) {
            player.closeInventory();
            player.sendMessage("§7Island restoration cancelled.");
            return;
        }

        // Check if a slot was clicked
        for (int i = 0; i < slotsWithData.size(); i++) {
            if (slotMapping[i] == slot) {
                SlotData selectedSlot = slotsWithData.get(i);
                selectSlotForRestoration(selectedSlot);
                return;
            }
        }
    }

    private void selectSlotForRestoration(SlotData slotData) {
        player.closeInventory();

        player.sendMessage("§a§lSlot " + slotData.getSlotNumber() + " selected!");
        player.sendMessage("§7Now choose a location to place your island.");

        // Store which slot they want to restore - we'll use this after they pick a location
        addon.getSlotManager().setPendingSlotRestoration(player.getUniqueId(), slotData.getSlotNumber());

        // Open the grid selection GUI
        Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
            new IslandRestoreGUI(addon, player, createListener, slotData).open();
        }, 5L);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() == this) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            HandlerList.unregisterAll(this);
        }
    }
}
