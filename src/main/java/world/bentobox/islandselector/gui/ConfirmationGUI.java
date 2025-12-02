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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.listeners.IslandCreateListener;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.ArrayList;
import java.util.List;

/**
 * Confirmation GUI for island claiming, purchasing, and relocation.
 *
 * Layout (27 slots, 3 rows):
 * Row 1: [filler][filler][filler][INFO][filler][filler][filler][filler][filler]
 * Row 2: [filler][filler][CONFIRM][filler][filler][filler][CANCEL][filler][filler]
 * Row 3: [filler][filler][filler][filler][filler][filler][filler][filler][filler]
 */
public class ConfirmationGUI implements InventoryHolder, Listener {

    private static final int SIZE = 27;
    private static final int INFO_SLOT = 4;
    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;

    public enum ActionType {
        CLAIM,      // Claiming a new island
        PURCHASE,   // Purchasing a premium location
        RELOCATE    // Relocating an existing island
    }

    private final IslandSelector addon;
    private final Player player;
    private final GridCoordinate coord;
    private final IslandCreateListener createListener;
    private final ActionType actionType;
    private final double price; // For purchase/relocate
    private Inventory inventory;

    /**
     * Constructor for claim confirmation (no price)
     */
    public ConfirmationGUI(IslandSelector addon, Player player, GridCoordinate coord,
                          IslandCreateListener createListener, ActionType actionType) {
        this(addon, player, coord, createListener, actionType, 0);
    }

    /**
     * Constructor with price (for purchase/relocate)
     */
    public ConfirmationGUI(IslandSelector addon, Player player, GridCoordinate coord,
                          IslandCreateListener createListener, ActionType actionType, double price) {
        this.addon = addon;
        this.player = player;
        this.coord = coord;
        this.createListener = createListener;
        this.actionType = actionType;
        this.price = price;
    }

    public void open() {
        createInventory();
        populateInventory();
        player.openInventory(inventory);
        Bukkit.getPluginManager().registerEvents(this, addon.getPlugin());
    }

    private void createInventory() {
        String title = getTitle();
        inventory = Bukkit.createInventory(this, SIZE, title);
    }

    private String getTitle() {
        switch (actionType) {
            case CLAIM:
                return "Confirm Island Location";
            case PURCHASE:
                return "Confirm Purchase";
            case RELOCATE:
                return "Confirm Relocation";
            default:
                return "Confirm Action";
        }
    }

    private void populateInventory() {
        // Fill with glass panes
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Info item
        inventory.setItem(INFO_SLOT, createInfoItem());

        // Confirm button
        inventory.setItem(CONFIRM_SLOT, createConfirmItem());

        // Cancel button
        inventory.setItem(CANCEL_SLOT, createCancelItem());
    }

    private ItemStack createInfoItem() {
        Material mat;
        String name;
        List<String> lore = new ArrayList<>();

        GridManager gridManager = addon.getGridManager();
        int worldX = gridManager.getWorldX(coord);
        int worldZ = gridManager.getWorldZ(coord);

        switch (actionType) {
            case CLAIM:
                mat = Material.GRASS_BLOCK;
                name = "&a&lNew Island Location";
                lore.add("");
                lore.add(colorize("&7Grid Location: &f" + coord.toString()));
                lore.add(colorize("&7World Coordinates:"));
                lore.add(colorize("&7  X: &f" + worldX));
                lore.add(colorize("&7  Z: &f" + worldZ));
                lore.add("");
                lore.add(colorize("&eYour island will be created here!"));
                break;

            case PURCHASE:
                mat = Material.GOLD_BLOCK;
                name = "&6&lPremium Location";
                lore.add("");
                lore.add(colorize("&7Grid Location: &f" + coord.toString()));
                lore.add(colorize("&7World Coordinates:"));
                lore.add(colorize("&7  X: &f" + worldX));
                lore.add(colorize("&7  Z: &f" + worldZ));
                lore.add("");
                lore.add(colorize("&7Price: &a$" + String.format("%.2f", price)));
                lore.add("");
                lore.add(colorize("&eThis premium location costs extra!"));
                break;

            case RELOCATE:
                mat = Material.ENDER_PEARL;
                name = "&b&lIsland Relocation";
                lore.add("");
                lore.add(colorize("&7New Location: &f" + coord.toString()));
                lore.add(colorize("&7World Coordinates:"));
                lore.add(colorize("&7  X: &f" + worldX));
                lore.add(colorize("&7  Z: &f" + worldZ));
                if (price > 0) {
                    lore.add("");
                    lore.add(colorize("&7Cost: &a$" + String.format("%.2f", price)));
                }
                lore.add("");
                lore.add(colorize("&c&lWarning: &cYour island will be moved!"));
                lore.add(colorize("&7All visitors will be teleported away."));
                break;

            default:
                mat = Material.PAPER;
                name = "&fAction";
                lore.add(colorize("&7Location: &f" + coord.toString()));
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize(name));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createConfirmItem() {
        ItemStack item = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&a&l✓ CONFIRM"));

        List<String> lore = new ArrayList<>();
        switch (actionType) {
            case CLAIM:
                lore.add(colorize("&7Click to create your island"));
                lore.add(colorize("&7at " + coord.toString()));
                break;
            case PURCHASE:
                lore.add(colorize("&7Click to purchase for"));
                lore.add(colorize("&a$" + String.format("%.2f", price)));
                break;
            case RELOCATE:
                lore.add(colorize("&7Click to relocate your island"));
                if (price > 0) {
                    lore.add(colorize("&7Cost: &a$" + String.format("%.2f", price)));
                }
                break;
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCancelItem() {
        ItemStack item = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&c&l✗ CANCEL"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Click to cancel"));
        lore.add(colorize("&7and return to selection"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String colorize(String text) {
        return text.replace("&", "\u00A7");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof ConfirmationGUI) || !holder.equals(this)) {
            return;
        }

        event.setCancelled(true);

        if (event.getRawSlot() < 0 || event.getRawSlot() >= SIZE) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot == CONFIRM_SLOT) {
            handleConfirm();
        } else if (slot == CANCEL_SLOT) {
            handleCancel();
        }
    }

    private void handleConfirm() {
        player.closeInventory();

        switch (actionType) {
            case CLAIM:
                // Verify location is still available
                if (!addon.getGridManager().isAvailable(coord)) {
                    player.sendMessage(colorize("&cThis location is no longer available!"));
                    createListener.cancelClaim(player);
                    return;
                }
                // Open blueprint selection GUI
                new BlueprintSelectionGUI(addon, player, coord, createListener).open();
                break;

            case PURCHASE:
                // Check if player has enough money
                if (!checkAndChargeMoney()) {
                    return;
                }
                // Verify location is still available
                if (!addon.getGridManager().isAvailable(coord)) {
                    // TODO: Refund the player
                    player.sendMessage(colorize("&cThis location is no longer available!"));
                    createListener.cancelClaim(player);
                    return;
                }
                // Open blueprint selection GUI for premium locations too
                new BlueprintSelectionGUI(addon, player, coord, createListener).open();
                break;

            case RELOCATE:
                // Check if player has enough money (if cost > 0)
                if (price > 0 && !checkAndChargeMoney()) {
                    return;
                }
                // TODO: Implement relocation logic
                player.sendMessage(colorize("&eIsland relocation coming soon!"));
                createListener.cancelClaim(player);
                break;
        }
    }

    private boolean checkAndChargeMoney() {
        // Check for Vault
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            player.sendMessage(colorize("&cEconomy system not available!"));
            return false;
        }

        try {
            // Get economy through Vault
            var rsp = Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            if (rsp == null) {
                player.sendMessage(colorize("&cNo economy plugin found!"));
                return false;
            }

            var economy = rsp.getProvider();
            double balance = economy.getBalance(player);

            if (balance < price) {
                player.sendMessage(colorize("&cYou need &a$" + String.format("%.2f", price) +
                    "&c but only have &a$" + String.format("%.2f", balance) + "&c!"));
                return false;
            }

            // Charge the player
            var result = economy.withdrawPlayer(player, price);
            if (result.transactionSuccess()) {
                player.sendMessage(colorize("&aPaid &f$" + String.format("%.2f", price)));
                return true;
            } else {
                player.sendMessage(colorize("&cTransaction failed: " + result.errorMessage));
                return false;
            }
        } catch (Exception e) {
            addon.logError("Economy error: " + e.getMessage());
            player.sendMessage(colorize("&cAn error occurred with the economy system."));
            return false;
        }
    }

    private void handleCancel() {
        player.closeInventory();
        createListener.cancelClaim(player);
        player.sendMessage(colorize("&cAction cancelled."));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ConfirmationGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof ConfirmationGUI && holder.equals(this)) {
            HandlerList.unregisterAll(this);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
