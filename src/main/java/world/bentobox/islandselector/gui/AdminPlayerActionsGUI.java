package world.bentobox.islandselector.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import org.bukkit.inventory.meta.SkullMeta;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Admin GUI for managing a specific player's island.
 * Provides options like teleport, view slots, relocate, remove, etc.
 */
public class AdminPlayerActionsGUI implements InventoryHolder, Listener {

    private static final int SIZE = 27; // 3 rows

    // Slot positions
    private static final int PLAYER_HEAD_SLOT = 4;
    private static final int TELEPORT_SLOT = 10;
    private static final int VIEW_SLOTS_SLOT = 11;
    private static final int RELOCATE_SLOT = 12;
    private static final int RESET_COOLDOWN_SLOT = 14;
    private static final int REMOVE_ISLAND_SLOT = 15;
    private static final int BACK_SLOT = 22;

    private final IslandSelector addon;
    private final Player adminPlayer;
    private final GridCoordinate coord;
    private final GridLocation location;
    private final UUID ownerUUID;
    private final String ownerName;
    private Inventory inventory;

    public AdminPlayerActionsGUI(IslandSelector addon, Player adminPlayer, GridCoordinate coord, GridLocation location) {
        this.addon = addon;
        this.adminPlayer = adminPlayer;
        this.coord = coord;
        this.location = location;
        this.ownerUUID = location.getOwnerUUID();
        this.ownerName = location.getOwnerName() != null ? location.getOwnerName() : "Unknown";
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        inventory = Bukkit.createInventory(this, SIZE, "§6§lAdmin: " + ownerName);
        populateInventory();
        Bukkit.getPluginManager().registerEvents(this, addon.getPlugin());
        adminPlayer.openInventory(inventory);
    }

    private void populateInventory() {
        // Fill with glass panes
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Player head with info
        inventory.setItem(PLAYER_HEAD_SLOT, createPlayerHead());

        // Teleport button
        inventory.setItem(TELEPORT_SLOT, createItem(
            Material.ENDER_PEARL,
            "§a§lTeleport to Island",
            List.of(
                "§7Teleport to this player's island",
                "",
                "§7Location: §f" + coord.toString(),
                "",
                "§e▶ Click to teleport"
            )
        ));

        // View Slots button
        inventory.setItem(VIEW_SLOTS_SLOT, createItem(
            Material.CHEST,
            "§e§lView Player Slots",
            List.of(
                "§7View this player's slot information",
                "",
                "§7Shows all slots and their status",
                "",
                "§e▶ Click to view slots"
            )
        ));

        // Relocate button - only show if FAWE is available
        boolean isOnline = Bukkit.getPlayer(ownerUUID) != null;
        if (addon.isSchematicOperationsAvailable()) {
            inventory.setItem(RELOCATE_SLOT, createItem(
                Material.COMPASS,
                "§d§lRelocate Island",
                List.of(
                    "§7Move this island to a new location",
                    "",
                    "§7Current: §f" + coord.toString(),
                    "§7Player: " + (isOnline ? "§aOnline" : "§7Offline"),
                    "",
                    "§e▶ Click to enter relocation mode"
                )
            ));
        }
        // If FAWE not available, slot will be filled with filler

        // Reset Cooldown button
        inventory.setItem(RESET_COOLDOWN_SLOT, createItem(
            Material.CLOCK,
            "§b§lReset Cooldowns",
            List.of(
                "§7Reset this player's cooldowns",
                "",
                "§7Resets:",
                "§7 - Slot switch cooldown",
                "§7 - Relocation cooldown",
                "",
                "§e▶ Click to reset"
            )
        ));

        // Remove Island button
        inventory.setItem(REMOVE_ISLAND_SLOT, createItem(
            Material.TNT,
            "§c§lRemove Island",
            List.of(
                "§7Remove this player's island",
                "",
                "§cWarning: §7This will:",
                "§7 - Save island to player's slot",
                "§7 - Clear all blocks",
                "§7 - Free up the grid location",
                "",
                "§c§lShift+Click to confirm"
            )
        ));

        // Back button
        inventory.setItem(BACK_SLOT, createItem(
            Material.ARROW,
            "§7§lBack to Grid",
            List.of("§7Return to the admin grid view")
        ));
    }

    private ItemStack createPlayerHead() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            // Set the skull owner
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(ownerUUID);
            meta.setOwningPlayer(offlinePlayer);

            meta.setDisplayName("§f§l" + ownerName);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7UUID: §f" + ownerUUID.toString().substring(0, 8) + "...");
            lore.add("§7Grid: §f" + coord.toString());

            // Check if online
            Player onlinePlayer = Bukkit.getPlayer(ownerUUID);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                lore.add("§7Status: §aOnline");
            } else {
                lore.add("§7Status: §7Offline");
            }

            // Get slot info
            List<SlotData> slots = addon.getSlotManager().getPlayerSlots(ownerUUID);
            lore.add("");
            lore.add("§7Slots: §f" + slots.size());

            SlotData activeSlot = addon.getSlotManager().getActiveSlot(ownerUUID);
            if (activeSlot != null) {
                lore.add("§7Active Slot: §f" + activeSlot.getSlotNumber());
            }

            // Get island info from BentoBox
            Island island = BentoBox.getInstance().getIslands()
                .getIsland(addon.getGridManager().getBSkyBlockWorld(), ownerUUID);
            if (island != null) {
                lore.add("");
                lore.add("§7Protection Range: §f" + island.getProtectionRange());
                lore.add("§7Members: §f" + island.getMemberSet().size());
            }

            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
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

        Player player = (Player) event.getWhoClicked();
        if (!player.equals(adminPlayer)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= SIZE) {
            return;
        }

        boolean isShiftClick = event.isShiftClick();

        switch (slot) {
            case TELEPORT_SLOT:
                handleTeleport();
                break;
            case VIEW_SLOTS_SLOT:
                handleViewSlots();
                break;
            case RELOCATE_SLOT:
                if (addon.isSchematicOperationsAvailable()) {
                    handleRelocate();
                }
                break;
            case RESET_COOLDOWN_SLOT:
                handleResetCooldown();
                break;
            case REMOVE_ISLAND_SLOT:
                handleRemoveIsland(isShiftClick);
                break;
            case BACK_SLOT:
                handleBack();
                break;
        }
    }

    private void handleTeleport() {
        adminPlayer.closeInventory();

        Island island = BentoBox.getInstance().getIslands()
            .getIsland(addon.getGridManager().getBSkyBlockWorld(), ownerUUID);

        if (island != null) {
            adminPlayer.sendMessage("§aTeleporting to §f" + ownerName + "§a's island at §f" + coord + "§a...");
            // Use BentoBox SafeSpotTeleport for safe async teleportation
            new SafeSpotTeleport.Builder(addon.getPlugin())
                .entity(adminPlayer)
                .island(island)
                .ifFail(() -> adminPlayer.sendMessage("§cCould not find a safe teleport location."))
                .buildFuture();
        } else {
            adminPlayer.sendMessage("§cIsland data not found in BentoBox.");
        }
    }

    private void handleViewSlots() {
        adminPlayer.closeInventory();

        List<SlotData> slots = addon.getSlotManager().getPlayerSlots(ownerUUID);

        adminPlayer.sendMessage("");
        adminPlayer.sendMessage("§6§l═══ Slots for " + ownerName + " ═══");
        adminPlayer.sendMessage("§7Grid Location: §f" + coord);
        adminPlayer.sendMessage("");

        if (slots.isEmpty()) {
            adminPlayer.sendMessage("§7No slots found for this player.");
        } else {
            for (SlotData slot : slots) {
                String status = slot.isActive() ? "§a[ACTIVE]" : "§7[inactive]";
                String hasIsland = slot.hasIsland() ? "§aHas Island" : "§7No Island";
                adminPlayer.sendMessage("§e Slot " + slot.getSlotNumber() + " " + status);
                adminPlayer.sendMessage("§7   " + hasIsland + " | Grid: " +
                    (slot.getGridCoordinate() != null ? slot.getGridCoordinate() : "None"));
            }
        }

        adminPlayer.sendMessage("");
        adminPlayer.sendMessage("§7Use §f/islandselector admin §7to return to grid.");
    }

    private void handleRelocate() {
        adminPlayer.closeInventory();

        // Open admin grid GUI in relocation mode
        AdminGridGUI gui = new AdminGridGUI(addon, adminPlayer);
        gui.open();
        gui.enterRelocationMode(coord, ownerUUID, ownerName);

        adminPlayer.sendMessage("§d§lRelocation Mode Active");
        adminPlayer.sendMessage("§7Click on an §aavailable §7location to move §f" + ownerName + "§7's island.");
        adminPlayer.sendMessage("§7Click §c[Cancel] §7or shift-click to exit relocation mode.");
    }

    private void handleResetCooldown() {
        // Reset relocation cooldown
        addon.getRelocationManager().resetCooldown(ownerUUID);

        adminPlayer.sendMessage("§aReset cooldowns for §f" + ownerName);

        // Notify the player if online
        Player targetPlayer = Bukkit.getPlayer(ownerUUID);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage("§aYour cooldowns have been reset by an administrator.");
        }

        // Refresh the GUI
        populateInventory();
    }

    private void handleRemoveIsland(boolean isShiftClick) {
        if (!isShiftClick) {
            adminPlayer.sendMessage("§cShift+Click to confirm island removal.");
            return;
        }

        adminPlayer.closeInventory();
        adminPlayer.sendMessage("§eRemoving island for §f" + ownerName + "§e...");

        // Execute island removal
        addon.getIslandRemovalManager().removeIsland(ownerUUID,
            world.bentobox.bentobox.api.user.User.getInstance(adminPlayer),
            success -> {
                if (success) {
                    adminPlayer.sendMessage("§aIsland removed successfully for §f" + ownerName);
                    adminPlayer.sendMessage("§7The island data has been saved to their slot.");
                } else {
                    adminPlayer.sendMessage("§cFailed to remove island for §f" + ownerName);
                }
            });
    }

    private void handleBack() {
        adminPlayer.closeInventory();

        // Reopen the admin grid GUI
        AdminGridGUI gui = new AdminGridGUI(addon, adminPlayer);
        gui.open();
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
