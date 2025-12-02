package world.bentobox.islandselector.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
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
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.Settings;
import world.bentobox.islandselector.listeners.IslandCreateListener;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Island Claim GUI - Grid selector for new island creation.
 * Shows only available locations and allows players to select where
 * their new island will be created.
 *
 * 54 slots (6 rows x 9 columns) layout:
 * - Row 1: [UP][UP][info][info][title][info][info][UP][UP]
 * - Row 2: [LEFT][grid 7 slots][RIGHT]
 * - Row 3: [LEFT][grid 7 slots][RIGHT]
 * - Row 4: [LEFT][grid 7 slots][RIGHT]
 * - Row 5: [LEFT][grid 7 slots][RIGHT]
 * - Row 6: [DN][DN][empty][empty][cancel][empty][empty][DN][DN]
 */
public class IslandClaimGUI implements InventoryHolder, Listener {

    // GUI Layout Constants
    private static final int ROWS = 6;
    private static final int COLS = 9;
    private static final int SIZE = ROWS * COLS; // 54 slots

    // Grid viewport: 7 columns x 4 rows = 28 visible island slots
    private static final int GRID_COLS = 7;
    private static final int GRID_ROWS = 4;

    // Navigation arrow positions
    private static final int[] ARROW_UP_SLOTS = {0, 1, 7, 8};
    private static final int[] ARROW_DOWN_SLOTS = {45, 46, 52, 53};
    private static final int[] ARROW_LEFT_SLOTS = {9, 18, 27, 36};
    private static final int[] ARROW_RIGHT_SLOTS = {17, 26, 35, 44};

    // Control slots
    private static final int TITLE_SLOT = 4;
    private static final int CANCEL_SLOT = 49;

    // Arrow head textures
    private static final String ARROW_UP_URL = "http://textures.minecraft.net/texture/3040fe836a6c2fbd2c7a9c8ec6be5174fddf1ac20f55e366156fa5f712e10";
    private static final String ARROW_DOWN_URL = "http://textures.minecraft.net/texture/7437346d8bda78d525d19f540a95e4e79daeda795cbc5a13256236312cf";
    private static final String ARROW_LEFT_URL = "http://textures.minecraft.net/texture/bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9";
    private static final String ARROW_RIGHT_URL = "http://textures.minecraft.net/texture/19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf";

    private final IslandSelector addon;
    private final Player player;
    private final GridManager gridManager;
    private final Settings settings;
    private final IslandCreateListener createListener;
    private Inventory inventory;

    // Viewport position
    private int viewportX;
    private int viewportZ;

    // Currently selected coordinate (before confirmation)
    private GridCoordinate selectedCoord = null;

    public IslandClaimGUI(IslandSelector addon, Player player, IslandCreateListener createListener) {
        this.addon = addon;
        this.player = player;
        this.gridManager = addon.getGridManager();
        this.settings = addon.getSettings();
        this.createListener = createListener;

        // Initialize viewport centered at 0,0
        this.viewportX = -GRID_COLS / 2;
        this.viewportZ = -GRID_ROWS / 2;
    }

    /**
     * Create and open the GUI
     */
    public void open() {
        createInventory();
        populateInventory();
        player.openInventory(inventory);

        // Register this as a listener
        Bukkit.getPluginManager().registerEvents(this, addon.getPlugin());
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(this, SIZE, "Select Island Location");
    }

    private void populateInventory() {
        inventory.clear();
        populateNavigationArrows();
        populateGrid();
        populateControls();
        fillEmptySlots();
    }

    private void populateNavigationArrows() {
        boolean canScrollUp = viewportZ > settings.getGridMinZ();
        boolean canScrollDown = viewportZ + GRID_ROWS - 1 < settings.getGridMaxZ();
        boolean canScrollLeft = viewportX > settings.getGridMinX();
        boolean canScrollRight = viewportX + GRID_COLS - 1 < settings.getGridMaxX();

        // Up arrows
        ItemStack upArrow = canScrollUp ?
            createArrowHead(ARROW_UP_URL, "&eScroll Up (North)", "&7Click: Scroll 1", "&7Shift+Click: Jump 5") :
            createDisabledArrow("&7Can't scroll further north");
        for (int slot : ARROW_UP_SLOTS) {
            inventory.setItem(slot, upArrow);
        }

        // Down arrows
        ItemStack downArrow = canScrollDown ?
            createArrowHead(ARROW_DOWN_URL, "&eScroll Down (South)", "&7Click: Scroll 1", "&7Shift+Click: Jump 5") :
            createDisabledArrow("&7Can't scroll further south");
        for (int slot : ARROW_DOWN_SLOTS) {
            inventory.setItem(slot, downArrow);
        }

        // Left arrows
        ItemStack leftArrow = canScrollLeft ?
            createArrowHead(ARROW_LEFT_URL, "&eScroll Left (West)", "&7Click: Scroll 1", "&7Shift+Click: Jump 5") :
            createDisabledArrow("&7Can't scroll further west");
        for (int slot : ARROW_LEFT_SLOTS) {
            inventory.setItem(slot, leftArrow);
        }

        // Right arrows
        ItemStack rightArrow = canScrollRight ?
            createArrowHead(ARROW_RIGHT_URL, "&eScroll Right (East)", "&7Click: Scroll 1", "&7Shift+Click: Jump 5") :
            createDisabledArrow("&7Can't scroll further east");
        for (int slot : ARROW_RIGHT_SLOTS) {
            inventory.setItem(slot, rightArrow);
        }
    }

    private ItemStack createArrowHead(String textureUrl, String name, String... loreLines) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(textureUrl));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (Exception e) {
            // Fallback - just use a regular head
        }

        meta.setDisplayName(colorize(name));
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(colorize(line));
        }
        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createDisabledArrow(String tooltip) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize(tooltip));
        item.setItemMeta(meta);
        return item;
    }

    private void populateGrid() {
        for (int gridRow = 0; gridRow < GRID_ROWS; gridRow++) {
            for (int gridCol = 0; gridCol < GRID_COLS; gridCol++) {
                int coordX = viewportX + gridCol;
                int coordZ = viewportZ + gridRow;
                GridCoordinate coord = new GridCoordinate(coordX, coordZ);
                int slot = getGridSlot(gridRow, gridCol);

                if (slot >= 0 && slot < SIZE) {
                    ItemStack item = createGridItem(coord);
                    inventory.setItem(slot, item);
                }
            }
        }
    }

    private int getGridSlot(int gridRow, int gridCol) {
        switch (gridRow) {
            case 0: return 10 + gridCol;
            case 1: return 19 + gridCol;
            case 2: return 28 + gridCol;
            case 3: return 37 + gridCol;
            default: return -1;
        }
    }

    private ItemStack createGridItem(GridCoordinate coord) {
        if (!gridManager.isWithinBounds(coord)) {
            return createLockedItem(coord);
        }

        GridLocation.Status status = gridManager.getLocationStatus(coord);
        GridLocation location = gridManager.getGridLocation(coord);

        switch (status) {
            case AVAILABLE:
                return createAvailableItem(coord);
            case OCCUPIED:
                return createOccupiedItem(coord, location);
            case RESERVED:
                if (location != null && location.isPurchasable()) {
                    return createPurchasableItem(coord, location);
                }
                return createBlockedItem(coord);
            case LOCKED:
            default:
                return createLockedItem(coord);
        }
    }

    private Material parseMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private String getWorldCoordsString(GridCoordinate coord) {
        int worldX = gridManager.getWorldX(coord);
        int worldZ = gridManager.getWorldZ(coord);
        return "X: " + worldX + ", Z: " + worldZ;
    }

    private ItemStack createAvailableItem(GridCoordinate coord) {
        Material mat = parseMaterial(settings.getItemAvailable(), Material.GREEN_STAINED_GLASS_PANE);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&a" + coord.toString() + " - Available"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Location: &f" + getWorldCoordsString(coord)));
        lore.add("");
        lore.add(colorize("&e&lClick to select this location!"));

        // Check if this is the currently selected coord
        if (selectedCoord != null && selectedCoord.equals(coord)) {
            meta.setDisplayName(colorize("&a&l" + coord.toString() + " - SELECTED"));
            addGlow(item, meta);
            lore.add("");
            lore.add(colorize("&a✓ Selected for your island"));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOccupiedItem(GridCoordinate coord, GridLocation location) {
        // Always use player head for occupied islands
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        // Set the owner for the skull texture using OfflinePlayer
        if (location != null && location.getOwnerUUID() != null) {
            org.bukkit.OfflinePlayer owner = Bukkit.getOfflinePlayer(location.getOwnerUUID());
            meta.setOwningPlayer(owner);
        }

        String ownerName = location != null && location.getOwnerName() != null ?
            location.getOwnerName() : "Unknown";
        meta.setDisplayName(colorize("&c" + coord.toString() + " - " + ownerName));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Location: &f" + getWorldCoordsString(coord)));
        lore.add(colorize("&7Owner: &f" + ownerName));
        lore.add("");
        lore.add(colorize("&cThis location is taken"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPurchasableItem(GridCoordinate coord, GridLocation location) {
        Material mat = parseMaterial(settings.getItemReservedPurchasable(), Material.GOLD_BLOCK);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&6⭐ Premium Location: " + coord.toString()));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Location: &f" + getWorldCoordsString(coord)));
        lore.add("");
        // Format price with commas (e.g., $50,000 instead of $50000.00)
        String formattedPrice = String.format("%,d", (int) location.getPurchasePrice());
        lore.add(colorize("&7Price: &a$" + formattedPrice));
        lore.add("");
        lore.add(colorize("&eClick to purchase and claim!"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBlockedItem(GridCoordinate coord) {
        Material mat = parseMaterial(settings.getItemReservedBlocked(), Material.GRAY_STAINED_GLASS_PANE);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&7" + coord.toString() + " - Reserved"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Location: &f" + getWorldCoordsString(coord)));
        lore.add("");
        lore.add(colorize("&cThis location is reserved"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLockedItem(GridCoordinate coord) {
        Material mat = parseMaterial(settings.getItemLockedArea(), Material.BLACK_STAINED_GLASS_PANE);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&8" + coord.toString() + " - Locked"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Location: &f" + getWorldCoordsString(coord)));
        lore.add(colorize("&8Outside grid boundaries"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void populateControls() {
        // Title/info in top center
        ItemStack title = new ItemStack(Material.NETHER_STAR);
        ItemMeta titleMeta = title.getItemMeta();
        titleMeta.setDisplayName(colorize("&a&lSelect Your Island Location"));

        List<String> titleLore = new ArrayList<>();
        titleLore.add("");
        titleLore.add(colorize("&7Click on a &agreen &7location"));
        titleLore.add(colorize("&7to place your new island there!"));
        titleLore.add("");
        if (selectedCoord != null) {
            titleLore.add(colorize("&eSelected: &f" + selectedCoord.toString()));
            titleLore.add(colorize("&aClick again to confirm"));
        } else {
            titleLore.add(colorize("&7No location selected yet"));
        }

        titleMeta.setLore(titleLore);
        title.setItemMeta(titleMeta);
        inventory.setItem(TITLE_SLOT, title);

        // Cancel button at bottom
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(colorize("&c&lCancel"));

        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(colorize("&7Click to cancel island creation"));
        cancelMeta.setLore(cancelLore);
        cancel.setItemMeta(cancelMeta);
        inventory.setItem(CANCEL_SLOT, cancel);
    }

    private void fillEmptySlots() {
        Material mat = parseMaterial(settings.getItemFiller(), Material.BLACK_STAINED_GLASS_PANE);
        ItemStack filler = new ItemStack(mat);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for (int i = 0; i < SIZE; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    private void addGlow(ItemStack item, ItemMeta meta) {
        Enchantment glow = org.bukkit.Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft("unbreaking"));
        if (glow != null) {
            meta.addEnchant(glow, 1, true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    private String colorize(String text) {
        return text.replace("&", "\u00A7");
    }

    // Navigation methods
    public void scrollUp(boolean shift) {
        int amount = shift ? settings.getScrollAmountShift() : settings.getScrollAmount();
        viewportZ = Math.max(settings.getGridMinZ(), viewportZ - amount);
        refresh();
    }

    public void scrollDown(boolean shift) {
        int amount = shift ? settings.getScrollAmountShift() : settings.getScrollAmount();
        int maxZ = settings.getGridMaxZ() - GRID_ROWS + 1;
        viewportZ = Math.min(maxZ, viewportZ + amount);
        refresh();
    }

    public void scrollLeft(boolean shift) {
        int amount = shift ? settings.getScrollAmountShift() : settings.getScrollAmount();
        viewportX = Math.max(settings.getGridMinX(), viewportX - amount);
        refresh();
    }

    public void scrollRight(boolean shift) {
        int amount = shift ? settings.getScrollAmountShift() : settings.getScrollAmount();
        int maxX = settings.getGridMaxX() - GRID_COLS + 1;
        viewportX = Math.min(maxX, viewportX + amount);
        refresh();
    }

    private void refresh() {
        populateInventory();
        player.updateInventory();
    }

    // Slot checking methods
    private boolean isScrollUpSlot(int slot) {
        for (int s : ARROW_UP_SLOTS) if (s == slot) return true;
        return false;
    }

    private boolean isScrollDownSlot(int slot) {
        for (int s : ARROW_DOWN_SLOTS) if (s == slot) return true;
        return false;
    }

    private boolean isScrollLeftSlot(int slot) {
        for (int s : ARROW_LEFT_SLOTS) if (s == slot) return true;
        return false;
    }

    private boolean isScrollRightSlot(int slot) {
        for (int s : ARROW_RIGHT_SLOTS) if (s == slot) return true;
        return false;
    }

    private GridCoordinate getCoordinateForSlot(int slot) {
        int gridRow = -1;
        int gridCol = -1;

        if (slot >= 10 && slot <= 16) { gridRow = 0; gridCol = slot - 10; }
        else if (slot >= 19 && slot <= 25) { gridRow = 1; gridCol = slot - 19; }
        else if (slot >= 28 && slot <= 34) { gridRow = 2; gridCol = slot - 28; }
        else if (slot >= 37 && slot <= 43) { gridRow = 3; gridCol = slot - 37; }

        if (gridRow >= 0 && gridCol >= 0) {
            return new GridCoordinate(viewportX + gridCol, viewportZ + gridRow);
        }
        return null;
    }

    // Event Handlers
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof IslandClaimGUI) || !holder.equals(this)) {
            return;
        }

        event.setCancelled(true);

        if (event.getRawSlot() < 0 || event.getRawSlot() >= SIZE) {
            return;
        }

        int slot = event.getRawSlot();
        boolean shift = event.isShiftClick();

        // Navigation
        if (isScrollUpSlot(slot)) { scrollUp(shift); return; }
        if (isScrollDownSlot(slot)) { scrollDown(shift); return; }
        if (isScrollLeftSlot(slot)) { scrollLeft(shift); return; }
        if (isScrollRightSlot(slot)) { scrollRight(shift); return; }

        // Cancel button
        if (slot == CANCEL_SLOT) {
            player.closeInventory();
            createListener.cancelClaim(player);
            player.sendMessage(colorize("&cIsland creation cancelled."));
            return;
        }

        // Grid clicks
        GridCoordinate coord = getCoordinateForSlot(slot);
        if (coord != null) {
            handleGridClick(coord);
        }
    }

    private void handleGridClick(GridCoordinate coord) {
        GridLocation.Status status = gridManager.getLocationStatus(coord);

        switch (status) {
            case AVAILABLE:
                handleAvailableClick(coord);
                break;
            case RESERVED:
                GridLocation location = gridManager.getGridLocation(coord);
                if (location != null && location.isPurchasable()) {
                    handlePurchasableClick(coord, location);
                } else {
                    player.sendMessage(colorize("&cThis location is reserved."));
                }
                break;
            case OCCUPIED:
                player.sendMessage(colorize("&cThis location is already taken."));
                break;
            case LOCKED:
                player.sendMessage(colorize("&8This area is not available."));
                break;
        }
    }

    private void handleAvailableClick(GridCoordinate coord) {
        if (selectedCoord != null && selectedCoord.equals(coord)) {
            // Second click on same location - confirm
            player.closeInventory();
            new ConfirmationGUI(addon, player, coord, createListener, ConfirmationGUI.ActionType.CLAIM).open();
        } else {
            // First click - select
            selectedCoord = coord;
            createListener.onLocationSelected(player, coord);
            player.sendMessage(colorize("&aSelected location " + coord.toString() + ". Click again to confirm!"));
            refresh();
        }
    }

    private void handlePurchasableClick(GridCoordinate coord, GridLocation location) {
        if (selectedCoord != null && selectedCoord.equals(coord)) {
            // Second click - confirm purchase
            player.closeInventory();
            new ConfirmationGUI(addon, player, coord, createListener,
                ConfirmationGUI.ActionType.PURCHASE, location.getPurchasePrice()).open();
        } else {
            // First click - select
            selectedCoord = coord;
            createListener.onLocationSelected(player, coord);
            player.sendMessage(colorize("&6Selected premium location " + coord.toString() +
                " (&a$" + String.format("%.2f", location.getPurchasePrice()) + "&6). Click again to purchase!"));
            refresh();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof IslandClaimGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof IslandClaimGUI && holder.equals(this)) {
            HandlerList.unregisterAll(this);
            // Don't auto-cancel here - player might be going to confirmation GUI
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
