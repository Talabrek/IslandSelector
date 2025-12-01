package world.bentobox.islandselector.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.Settings;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main Grid GUI for island selection
 * 54 slots (6 rows x 9 columns) layout:
 * - Row 1: Scroll up arrows + grid (top row)
 * - Row 2-4: Scroll left/right + grid (middle rows)
 * - Row 5: Scroll down arrows + grid (bottom row)
 * - Row 6: Control buttons
 */
public class MainGridGUI implements InventoryHolder {

    // GUI Layout Constants
    private static final int ROWS = 6;
    private static final int COLS = 9;
    private static final int SIZE = ROWS * COLS; // 54 slots

    // Grid viewport: 7 columns x 4 rows = 28 visible island slots
    private static final int GRID_COLS = 7;
    private static final int GRID_ROWS = 4;

    // Slot positions for navigation arrows
    private static final int[] SCROLL_UP_SLOTS = {0, 1, 7, 8};
    private static final int[] SCROLL_DOWN_SLOTS = {36, 37, 43, 44};
    private static final int[] SCROLL_LEFT_SLOTS = {9, 18, 27};
    private static final int[] SCROLL_RIGHT_SLOTS = {17, 26, 35};

    // Control button positions (bottom row)
    private static final int FILTER_ALL_SLOT = 45;
    private static final int FILTER_AVAILABLE_SLOT = 46;
    private static final int FILTER_ONLINE_SLOT = 47;
    private static final int SEARCH_SLOT = 48;
    private static final int FIND_MY_ISLAND_SLOT = 49;
    private static final int NEIGHBORHOOD_SLOT = 50;
    private static final int SLOTS_SLOT = 51;
    private static final int FILLER_SLOT = 52;
    private static final int CLOSE_SLOT = 53;

    // Grid display slots (calculated positions)
    private static final int[][] GRID_SLOTS = {
        {2, 3, 4, 5, 6, 10, 11},     // First row of grid (includes wrap from row 1 to 2)
        {12, 13, 14, 15, 16, 19, 20}, // Second row of grid
        {21, 22, 23, 24, 25, 28, 29}, // Third row
        {30, 31, 32, 33, 34, 38, 39}  // Fourth row (includes slots from row 5)
    };

    // Adjusted grid slots for proper layout
    // Row 1 (slots 0-8): slots 2-6 are grid
    // Row 2 (slots 9-17): slots 10-16 are grid
    // Row 3 (slots 18-26): slots 19-25 are grid
    // Row 4 (slots 27-35): slots 28-34 are grid
    // Row 5 (slots 36-44): slots 38-42 are grid
    private static final int[] GRID_SLOT_POSITIONS = {
        // Row 0 (top grid row in GUI row 1)
        2, 3, 4, 5, 6,  // Only 5 slots in first row due to arrows
        // Actually let's recalculate for 7 columns x 4 rows
        // I need to map 28 grid positions to actual inventory slots
    };

    private final IslandSelector addon;
    private final Player player;
    private final GridManager gridManager;
    private final Settings settings;
    private Inventory inventory;

    // Viewport position (top-left corner of visible grid)
    private int viewportColumn = 0;
    private int viewportRow = 0;

    // Current filter
    private FilterType filter = FilterType.ALL;

    public enum FilterType {
        ALL, AVAILABLE, ONLINE
    }

    public MainGridGUI(IslandSelector addon, Player player) {
        this.addon = addon;
        this.player = player;
        this.gridManager = addon.getGridManager();
        this.settings = addon.getSettings();
    }

    /**
     * Create and open the GUI
     */
    public void open() {
        createInventory();
        populateInventory();
        player.openInventory(inventory);

        // Register click handler
        Bukkit.getPluginManager().registerEvents(new GridGUIListener(this), addon.getPlugin());
    }

    /**
     * Create the inventory with dynamic title
     */
    private void createInventory() {
        String title = buildTitle();
        inventory = Bukkit.createInventory(this, SIZE, title);
    }

    /**
     * Build the GUI title with current viewport coordinates
     */
    private String buildTitle() {
        GridCoordinate topLeft = new GridCoordinate(viewportColumn, viewportRow);
        GridCoordinate bottomRight = new GridCoordinate(
            Math.min(viewportColumn + GRID_COLS - 1, settings.getGridWidth() - 1),
            Math.min(viewportRow + GRID_ROWS - 1, settings.getGridHeight() - 1)
        );
        return "Grid View: " + topLeft.toString() + " - " + bottomRight.toString();
    }

    /**
     * Populate the inventory with items
     */
    private void populateInventory() {
        // Clear inventory
        inventory.clear();

        // Add navigation arrows
        populateNavigationArrows();

        // Add grid display
        populateGrid();

        // Add control buttons
        populateControlButtons();

        // Fill empty slots with filler
        fillEmptySlots();
    }

    /**
     * Populate navigation arrows
     */
    private void populateNavigationArrows() {
        ItemStack upArrow = createArrowItem(Material.ARROW, "&eScroll Up", "&7Click to scroll up", "&7Shift+Click to jump 5 rows");
        ItemStack downArrow = createArrowItem(Material.ARROW, "&eScroll Down", "&7Click to scroll down", "&7Shift+Click to jump 5 rows");
        ItemStack leftArrow = createArrowItem(Material.ARROW, "&eScroll Left", "&7Click to scroll left", "&7Shift+Click to jump 5 columns");
        ItemStack rightArrow = createArrowItem(Material.ARROW, "&eScroll Right", "&7Click to scroll right", "&7Shift+Click to jump 5 columns");

        // Disable arrows at boundaries
        boolean canScrollUp = viewportRow > 0;
        boolean canScrollDown = viewportRow + GRID_ROWS < settings.getGridHeight();
        boolean canScrollLeft = viewportColumn > 0;
        boolean canScrollRight = viewportColumn + GRID_COLS < settings.getGridWidth();

        for (int slot : SCROLL_UP_SLOTS) {
            inventory.setItem(slot, canScrollUp ? upArrow : createDisabledArrow("&7Cannot scroll up"));
        }
        for (int slot : SCROLL_DOWN_SLOTS) {
            inventory.setItem(slot, canScrollDown ? downArrow : createDisabledArrow("&7Cannot scroll down"));
        }
        for (int slot : SCROLL_LEFT_SLOTS) {
            inventory.setItem(slot, canScrollLeft ? leftArrow : createDisabledArrow("&7Cannot scroll left"));
        }
        for (int slot : SCROLL_RIGHT_SLOTS) {
            inventory.setItem(slot, canScrollRight ? rightArrow : createDisabledArrow("&7Cannot scroll right"));
        }
    }

    /**
     * Populate the grid display area
     */
    private void populateGrid() {
        // Grid slots mapping: 7 columns x 4 rows
        // Layout in inventory:
        // Row 1: [UP][UP][G][G][G][G][G][UP][UP]  -> slots 2-6 are grid
        // Row 2: [L][G][G][G][G][G][G][G][R]      -> slots 10-16 are grid
        // Row 3: [L][G][G][G][G][G][G][G][R]      -> slots 19-25 are grid
        // Row 4: [L][G][G][G][G][G][G][G][R]      -> slots 28-34 are grid
        // Row 5: [DN][DN][G][G][G][G][G][DN][DN]  -> slots 38-42 are grid

        int[][] gridLayout = {
            {2, 3, 4, 5, 6, -1, -1},       // Top row: only 5 grid slots (2-6), -1 means skip
            {10, 11, 12, 13, 14, 15, 16},  // Full 7 grid slots
            {19, 20, 21, 22, 23, 24, 25},  // Full 7 grid slots
            {28, 29, 30, 31, 32, 33, 34},  // Full 7 grid slots
        };

        // Actually, let's use the specified layout from spec:
        // 7 columns x 4 rows = 28 island slots
        // Proper mapping based on spec
        int[][] properGridLayout = {
            {10, 11, 12, 13, 14, 15, 16},  // Row 2: grid columns
            {19, 20, 21, 22, 23, 24, 25},  // Row 3: grid columns
            {28, 29, 30, 31, 32, 33, 34},  // Row 4: grid columns
            {38, 39, 40, 41, 42, -1, -1},  // Row 5: partial grid (5 slots: 38-42)
        };

        // Revised to match spec better - let's use a simpler approach
        // The center 7x4 area should be:
        // Rows 1-4, columns 1-7 (0-indexed: rows 0-3, columns 1-7)
        // But with navigation on edges

        // Simplified: map grid positions directly
        for (int gridRow = 0; gridRow < GRID_ROWS; gridRow++) {
            for (int gridCol = 0; gridCol < GRID_COLS; gridCol++) {
                int worldCol = viewportColumn + gridCol;
                int worldRow = viewportRow + gridRow;

                GridCoordinate coord = new GridCoordinate(worldCol, worldRow);
                int slot = getGridSlot(gridRow, gridCol);

                if (slot >= 0 && slot < SIZE) {
                    ItemStack item = createGridLocationItem(coord);
                    inventory.setItem(slot, item);
                }
            }
        }
    }

    /**
     * Get the inventory slot for a grid position
     */
    private int getGridSlot(int gridRow, int gridCol) {
        // Layout based on spec:
        // Row 1 (slots 0-8): arrows and partial grid
        // Row 2 (slots 9-17): left arrow at 9, grid at 10-16, right arrow at 17
        // Row 3 (slots 18-26): left arrow at 18, grid at 19-25, right arrow at 26
        // Row 4 (slots 27-35): left arrow at 27, grid at 28-34, right arrow at 35
        // Row 5 (slots 36-44): arrows and partial grid

        // For simplicity, use center area rows 2-5 for the 4 grid rows
        // Each row has 7 grid slots (columns 1-7 in each row)

        switch (gridRow) {
            case 0: return 10 + gridCol; // Row 2: slots 10-16
            case 1: return 19 + gridCol; // Row 3: slots 19-25
            case 2: return 28 + gridCol; // Row 4: slots 28-34
            case 3:
                if (gridCol < 5) {
                    return 38 + gridCol; // Row 5: slots 38-42 (only 5 slots available)
                }
                return -1; // No slot available
            default:
                return -1;
        }
    }

    /**
     * Create an item representing a grid location
     */
    private ItemStack createGridLocationItem(GridCoordinate coord) {
        // Check if within bounds
        if (!gridManager.isWithinBounds(coord)) {
            return createLockedItem(coord);
        }

        GridLocation location = gridManager.getGridLocation(coord);
        GridLocation.Status status = gridManager.getLocationStatus(coord);

        // Apply filter
        if (filter != FilterType.ALL) {
            if (filter == FilterType.AVAILABLE && status != GridLocation.Status.AVAILABLE) {
                return createFilteredOutItem(coord);
            }
            if (filter == FilterType.ONLINE && !gridManager.isOwnerOnline(coord)) {
                return createFilteredOutItem(coord);
            }
        }

        switch (status) {
            case AVAILABLE:
                return createAvailableItem(coord);
            case OCCUPIED:
                return createOccupiedItem(coord, location);
            case RESERVED:
                if (location != null && location.isPurchasable()) {
                    return createPurchasableItem(coord, location);
                } else {
                    return createBlockedItem(coord);
                }
            case LOCKED:
            default:
                return createLockedItem(coord);
        }
    }

    /**
     * Create item for available location (green stained glass)
     */
    private ItemStack createAvailableItem(GridCoordinate coord) {
        ItemStack item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&a" + coord.toString() + " - Available"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Location: &f" + coord.toString()));
        lore.add("");

        // Check if player has an island
        GridCoordinate playerIsland = gridManager.getPlayerIslandCoordinate(player.getUniqueId());
        if (playerIsland == null) {
            lore.add(colorize("&eClick to claim this location"));
        } else {
            lore.add(colorize("&eClick to relocate here"));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create item for occupied location
     */
    private ItemStack createOccupiedItem(GridCoordinate coord, GridLocation location) {
        boolean isOwnerOnline = gridManager.isOwnerOnline(coord);
        boolean isOwnIsland = location.getOwnerUUID() != null &&
            location.getOwnerUUID().equals(player.getUniqueId());

        ItemStack item;
        if (isOwnerOnline) {
            // Player head for online owners
            item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            if (location.getOwnerUUID() != null) {
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(location.getOwnerUUID()));
            }
            item.setItemMeta(skullMeta);
        } else {
            // Red glass for offline owners
            item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        }

        ItemMeta meta = item.getItemMeta();

        if (isOwnIsland) {
            meta.setDisplayName(colorize("&a&l" + coord.toString() + " - Your Island"));
            // Add enchantment glow using registry
            Enchantment glow = org.bukkit.Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft("unbreaking"));
            if (glow != null) {
                meta.addEnchant(glow, 1, true);
            }
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            String ownerName = location.getOwnerName() != null ? location.getOwnerName() : "Unknown";
            meta.setDisplayName(colorize("&f" + coord.toString() + " - " + ownerName));
        }

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Location: &f" + coord.toString()));

        if (isOwnIsland) {
            lore.add(colorize("&6Your Island"));
        } else {
            String ownerName = location.getOwnerName() != null ? location.getOwnerName() : "Unknown";
            lore.add(colorize("&7Owner: &f" + ownerName));
            if (isOwnerOnline) {
                lore.add(colorize("&a Online"));
            } else {
                lore.add(colorize("&c Offline"));
            }
            lore.add("");
            lore.add(colorize("&7Right-click to visit (if warp open)"));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create item for purchasable reserved location (gold block)
     */
    private ItemStack createPurchasableItem(GridCoordinate coord, GridLocation location) {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&6Premium Location: " + coord.toString()));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Location: &f" + coord.toString()));
        lore.add("");
        lore.add(colorize("&7Price: &a$" + String.format("%.2f", location.getPurchasePrice())));
        lore.add("");
        lore.add(colorize("&eClick to purchase"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create item for blocked reserved location (gray glass)
     */
    private ItemStack createBlockedItem(GridCoordinate coord) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&7" + coord.toString() + " - Reserved"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Location: &f" + coord.toString()));
        lore.add("");
        lore.add(colorize("&c This location is not available"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create item for locked area (black glass)
     */
    private ItemStack createLockedItem(GridCoordinate coord) {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&8" + coord.toString() + " - Locked"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Location: &f" + coord.toString()));
        lore.add("");
        lore.add(colorize("&8 Locked Area"));
        lore.add(colorize("&7This area is not yet available"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create item for filtered out location
     */
    private ItemStack createFilteredOutItem(GridCoordinate coord) {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&7" + coord.toString()));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7(Filtered out)"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Populate control buttons in bottom row
     */
    private void populateControlButtons() {
        // Filter: All
        ItemStack filterAll = createButton(Material.PAPER, "&fFilter: All",
            filter == FilterType.ALL ? "&aCurrently selected" : "&7Click to show all locations");
        if (filter == FilterType.ALL) {
            addGlow(filterAll);
        }
        inventory.setItem(FILTER_ALL_SLOT, filterAll);

        // Filter: Available
        ItemStack filterAvailable = createButton(Material.GREEN_DYE, "&aFilter: Available",
            filter == FilterType.AVAILABLE ? "&aCurrently selected" : "&7Click to show only available locations");
        if (filter == FilterType.AVAILABLE) {
            addGlow(filterAvailable);
        }
        inventory.setItem(FILTER_AVAILABLE_SLOT, filterAvailable);

        // Filter: Online
        ItemStack filterOnline = createButton(Material.LIME_DYE, "&aFilter: Online",
            filter == FilterType.ONLINE ? "&aCurrently selected" : "&7Click to show only islands with online owners");
        if (filter == FilterType.ONLINE) {
            addGlow(filterOnline);
        }
        inventory.setItem(FILTER_ONLINE_SLOT, filterOnline);

        // Search
        ItemStack search = createButton(Material.COMPASS, "&eSearch Player",
            "&7Click to search for a player's island");
        inventory.setItem(SEARCH_SLOT, search);

        // Find My Island
        ItemStack findMyIsland = createButton(Material.ENDER_EYE, "&dFind My Island",
            "&7Click to center view on your island");
        inventory.setItem(FIND_MY_ISLAND_SLOT, findMyIsland);

        // Neighborhood View
        ItemStack neighborhood = createButton(Material.FILLED_MAP, "&bNeighborhood View",
            "&7View your island's neighbors");
        inventory.setItem(NEIGHBORHOOD_SLOT, neighborhood);

        // Slot Selection
        ItemStack slots = createButton(Material.CHEST, "&6Island Slots",
            "&7Manage your island slots");
        inventory.setItem(SLOTS_SLOT, slots);

        // Filler
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        inventory.setItem(FILLER_SLOT, filler);

        // Close
        ItemStack close = createButton(Material.BARRIER, "&cClose",
            "&7Click to close this menu");
        inventory.setItem(CLOSE_SLOT, close);
    }

    /**
     * Fill empty slots with black glass filler
     */
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

    // Helper methods

    private ItemStack createArrowItem(Material material, String name, String... loreLines) {
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

    private ItemStack createDisabledArrow(String message) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize(message));
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

    private void addGlow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        Enchantment glow = org.bukkit.Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft("unbreaking"));
        if (glow != null) {
            meta.addEnchant(glow, 1, true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    private String colorize(String text) {
        return text.replace("&", "\u00A7");
    }

    // Public methods for handling clicks

    /**
     * Handle scroll up
     */
    public void scrollUp(boolean shift) {
        int amount = shift ? settings.getScrollAmountShift() : settings.getScrollAmount();
        viewportRow = Math.max(0, viewportRow - amount);
        refresh();
    }

    /**
     * Handle scroll down
     */
    public void scrollDown(boolean shift) {
        int amount = shift ? settings.getScrollAmountShift() : settings.getScrollAmount();
        int maxRow = Math.max(0, settings.getGridHeight() - GRID_ROWS);
        viewportRow = Math.min(maxRow, viewportRow + amount);
        refresh();
    }

    /**
     * Handle scroll left
     */
    public void scrollLeft(boolean shift) {
        int amount = shift ? settings.getScrollAmountShift() : settings.getScrollAmount();
        viewportColumn = Math.max(0, viewportColumn - amount);
        refresh();
    }

    /**
     * Handle scroll right
     */
    public void scrollRight(boolean shift) {
        int amount = shift ? settings.getScrollAmountShift() : settings.getScrollAmount();
        int maxCol = Math.max(0, settings.getGridWidth() - GRID_COLS);
        viewportColumn = Math.min(maxCol, viewportColumn + amount);
        refresh();
    }

    /**
     * Set filter and refresh
     */
    public void setFilter(FilterType newFilter) {
        this.filter = newFilter;
        refresh();
    }

    /**
     * Center view on player's island
     */
    public void findMyIsland() {
        GridCoordinate myIsland = gridManager.getPlayerIslandCoordinate(player.getUniqueId());
        if (myIsland != null) {
            // Center the viewport on the player's island
            viewportColumn = Math.max(0, myIsland.getColumn() - GRID_COLS / 2);
            viewportRow = Math.max(0, myIsland.getRow() - GRID_ROWS / 2);

            // Clamp to grid boundaries
            int maxCol = Math.max(0, settings.getGridWidth() - GRID_COLS);
            int maxRow = Math.max(0, settings.getGridHeight() - GRID_ROWS);
            viewportColumn = Math.min(viewportColumn, maxCol);
            viewportRow = Math.min(viewportRow, maxRow);

            refresh();
            player.sendMessage(colorize("&aCentered on your island at " + myIsland.toString()));
        } else {
            player.sendMessage(colorize("&cYou don't have an island yet!"));
        }
    }

    /**
     * Refresh the GUI with new inventory
     */
    public void refresh() {
        // Close and reopen with new title
        player.closeInventory();
        createInventory();
        populateInventory();
        player.openInventory(inventory);
    }

    /**
     * Get the grid coordinate for an inventory slot
     */
    public GridCoordinate getCoordinateForSlot(int slot) {
        // Check if this slot is in the grid area
        int gridRow = -1;
        int gridCol = -1;

        // Row 2: slots 10-16
        if (slot >= 10 && slot <= 16) {
            gridRow = 0;
            gridCol = slot - 10;
        }
        // Row 3: slots 19-25
        else if (slot >= 19 && slot <= 25) {
            gridRow = 1;
            gridCol = slot - 19;
        }
        // Row 4: slots 28-34
        else if (slot >= 28 && slot <= 34) {
            gridRow = 2;
            gridCol = slot - 28;
        }
        // Row 5: slots 38-42
        else if (slot >= 38 && slot <= 42) {
            gridRow = 3;
            gridCol = slot - 38;
        }

        if (gridRow >= 0 && gridCol >= 0) {
            int worldCol = viewportColumn + gridCol;
            int worldRow = viewportRow + gridRow;
            return new GridCoordinate(worldCol, worldRow);
        }

        return null;
    }

    /**
     * Check if a slot is a scroll up button
     */
    public boolean isScrollUpSlot(int slot) {
        for (int s : SCROLL_UP_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    /**
     * Check if a slot is a scroll down button
     */
    public boolean isScrollDownSlot(int slot) {
        for (int s : SCROLL_DOWN_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    /**
     * Check if a slot is a scroll left button
     */
    public boolean isScrollLeftSlot(int slot) {
        for (int s : SCROLL_LEFT_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    /**
     * Check if a slot is a scroll right button
     */
    public boolean isScrollRightSlot(int slot) {
        for (int s : SCROLL_RIGHT_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    // Getters

    public Player getPlayer() {
        return player;
    }

    public IslandSelector getAddon() {
        return addon;
    }

    public int getFilterAllSlot() {
        return FILTER_ALL_SLOT;
    }

    public int getFilterAvailableSlot() {
        return FILTER_AVAILABLE_SLOT;
    }

    public int getFilterOnlineSlot() {
        return FILTER_ONLINE_SLOT;
    }

    public int getSearchSlot() {
        return SEARCH_SLOT;
    }

    public int getFindMyIslandSlot() {
        return FIND_MY_ISLAND_SLOT;
    }

    public int getNeighborhoodSlot() {
        return NEIGHBORHOOD_SLOT;
    }

    public int getSlotsSlot() {
        return SLOTS_SLOT;
    }

    public int getCloseSlot() {
        return CLOSE_SLOT;
    }

    public FilterType getFilter() {
        return filter;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
