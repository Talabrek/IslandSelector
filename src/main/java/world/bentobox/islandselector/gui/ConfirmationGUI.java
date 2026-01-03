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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
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
        CLAIM,              // Claiming a new island
        PURCHASE,           // Purchasing a premium location (no existing island)
        RELOCATE,           // Relocating an existing island (to normal location)
        PREMIUM_RELOCATE    // Relocating an existing island to a premium location
    }

    private final IslandSelector addon;
    private final Player player;
    private final GridCoordinate coord;
    private final IslandCreateListener createListener;
    private final ActionType actionType;
    private final double price; // For purchase/relocate
    private Inventory inventory;
    private BukkitTask cleanupTask;

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

        // Schedule cleanup task as fallback (30 minutes)
        cleanupTask = Bukkit.getScheduler().runTaskLater(addon.getPlugin(), this::cleanup, 20 * 60 * 30);
    }

    private void cleanup() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        HandlerList.unregisterAll(this);
        inventory = null;
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
            case PREMIUM_RELOCATE:
                return "Confirm Premium Relocation";
            default:
                return "Confirm Action";
        }
    }

    private void populateInventory() {
        // Fill with glass panes
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }

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
                // Format price with commas (e.g., $50,000 instead of $50000.00)
                String formattedPrice = String.format("%,d", (int) price);
                lore.add(colorize("&7Price: &a$" + formattedPrice));
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
                    // Format price with commas (e.g., $50,000 instead of $50000.00)
                    String formattedRelocatePrice = String.format("%,d", (int) price);
                    lore.add(colorize("&7Cost: &a$" + formattedRelocatePrice));
                }
                lore.add("");
                lore.add(colorize("&c&lWarning: &cYour island will be moved!"));
                lore.add(colorize("&7All visitors will be teleported away."));
                break;

            case PREMIUM_RELOCATE:
                mat = Material.GOLD_BLOCK;
                name = "&6&lPremium Relocation";
                lore.add("");
                lore.add(colorize("&7New Location: &f" + coord.toString()));
                lore.add(colorize("&7World Coordinates:"));
                lore.add(colorize("&7  X: &f" + worldX));
                lore.add(colorize("&7  Z: &f" + worldZ));
                lore.add("");
                String formattedPremiumPrice = String.format("%,d", (int) price);
                lore.add(colorize("&7Premium Cost: &a$" + formattedPremiumPrice));
                lore.add("");
                lore.add(colorize("&eThis is a premium location!"));
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
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(colorize(name));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createConfirmItem() {
        ItemStack item = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(colorize("&a&l✓ CONFIRM"));

        List<String> lore = new ArrayList<>();
        switch (actionType) {
            case CLAIM:
                lore.add(colorize("&7Click to create your island"));
                lore.add(colorize("&7at " + coord.toString()));
                break;
            case PURCHASE:
                lore.add(colorize("&7Click to purchase for"));
                lore.add(colorize("&a$" + String.format("%,d", (int) price)));
                break;
            case RELOCATE:
                lore.add(colorize("&7Click to relocate your island"));
                if (price > 0) {
                    lore.add(colorize("&7Cost: &a$" + String.format("%,d", (int) price)));
                }
                break;
            case PREMIUM_RELOCATE:
                lore.add(colorize("&7Click to relocate to premium"));
                lore.add(colorize("&7Cost: &a$" + String.format("%,d", (int) price)));
                break;
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCancelItem() {
        ItemStack item = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
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
                // Verify location is still available FIRST (before charging)
                if (!addon.getGridManager().isAvailable(coord)) {
                    player.sendMessage(colorize("&cThis location is no longer available!"));
                    createListener.cancelClaim(player);
                    return;
                }
                // Now check if player has enough money and charge them
                if (!checkAndChargeMoney()) {
                    return;
                }
                // Open blueprint selection GUI for premium locations too
                new BlueprintSelectionGUI(addon, player, coord, createListener).open();
                break;

            case RELOCATE:
                // Verify location is still available
                if (!addon.getGridManager().isAvailable(coord)) {
                    player.sendMessage(colorize("&cThis location is no longer available!"));
                    return;
                }

                // Check cooldown
                if (!addon.getRelocationManager().canRelocate(player.getUniqueId())) {
                    long remaining = addon.getRelocationManager().getRemainingCooldown(player.getUniqueId());
                    String timeStr = addon.getRelocationManager().formatCooldownTime(remaining);
                    player.sendMessage(colorize("&cYou must wait &e" + timeStr + " &cbefore relocating again!"));
                    return;
                }

                // Check if player can afford it
                if (!addon.getRelocationManager().canAffordRelocation(player)) {
                    double cost = addon.getSettings().getRelocationCost();
                    player.sendMessage(colorize("&cYou need &a$" + String.format("%.2f", cost) + " &cto relocate!"));
                    return;
                }

                // Charge the player
                if (!addon.getRelocationManager().chargePlayer(player)) {
                    player.sendMessage(colorize("&cFailed to charge relocation cost!"));
                    return;
                }

                // Get player's current island location
                GridCoordinate fromCoord = addon.getGridManager().getPlayerIslandCoordinate(player.getUniqueId());
                if (fromCoord == null) {
                    player.sendMessage(colorize("&cYou don't have an island to relocate!"));
                    return;
                }

                // Start the relocation
                player.sendMessage(colorize("&aStarting island relocation..."));
                addon.getRelocationManager().relocateIsland(player, fromCoord, coord);
                break;

            case PREMIUM_RELOCATE:
                // Verify location is still purchasable
                var gridLocation = addon.getGridManager().getGridLocation(coord);
                if (gridLocation == null || !gridLocation.isPurchasable()) {
                    player.sendMessage(colorize("&cThis premium location is no longer available!"));
                    return;
                }

                // Check cooldown
                if (!addon.getRelocationManager().canRelocate(player.getUniqueId())) {
                    long premiumRemaining = addon.getRelocationManager().getRemainingCooldown(player.getUniqueId());
                    String premiumTimeStr = addon.getRelocationManager().formatCooldownTime(premiumRemaining);
                    player.sendMessage(colorize("&cYou must wait &e" + premiumTimeStr + " &cbefore relocating again!"));
                    return;
                }

                // Check and charge the premium price
                if (!checkAndChargeMoney()) {
                    return;
                }

                // Get player's current island location
                GridCoordinate premiumFromCoord = addon.getGridManager().getPlayerIslandCoordinate(player.getUniqueId());
                if (premiumFromCoord == null) {
                    player.sendMessage(colorize("&cYou don't have an island to relocate!"));
                    return;
                }

                // Remove the premium/reserved status from the location so relocation can proceed
                addon.getGridManager().unreserveLocation(coord);

                // Start the relocation
                player.sendMessage(colorize("&aStarting premium island relocation..."));
                addon.getRelocationManager().relocateIsland(player, premiumFromCoord, coord);
                break;
        }
    }

    private boolean checkAndChargeMoney() {
        // Check for bypass permission based on action type
        String bypassPerm = null;
        if (actionType == ActionType.PURCHASE || actionType == ActionType.PREMIUM_RELOCATE) {
            bypassPerm = "islandselector.bypass.cost.purchase";
        } else if (actionType == ActionType.RELOCATE) {
            bypassPerm = "islandselector.bypass.cost.relocate";
        }

        // If player has bypass permission, skip payment
        if (bypassPerm != null && player.hasPermission(bypassPerm)) {
            player.sendMessage(colorize("&7(Cost bypassed due to permission)"));
            return true;
        }

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

    /**
     * Refund money to the player (used when location becomes unavailable after payment)
     */
    private void refundPlayer(double amount) {
        if (amount <= 0) {
            return;
        }

        // Check for bypass permission - if bypassed, no refund needed
        String bypassPerm = null;
        if (actionType == ActionType.PURCHASE || actionType == ActionType.PREMIUM_RELOCATE) {
            bypassPerm = "islandselector.bypass.cost.purchase";
        }
        if (bypassPerm != null && player.hasPermission(bypassPerm)) {
            return; // Player didn't pay, so no refund needed
        }

        // Check for Vault
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return;
        }

        try {
            var rsp = Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            if (rsp == null) {
                return;
            }

            var economy = rsp.getProvider();
            var result = economy.depositPlayer(player, amount);
            if (result.transactionSuccess()) {
                player.sendMessage(colorize("&aRefunded &f$" + String.format("%.2f", amount)));
            } else {
                addon.logError("Failed to refund player " + player.getName() + ": " + result.errorMessage);
            }
        } catch (Exception e) {
            addon.logError("Error refunding player: " + e.getMessage());
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
            cleanup();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer().equals(player)) {
            cleanup();
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
