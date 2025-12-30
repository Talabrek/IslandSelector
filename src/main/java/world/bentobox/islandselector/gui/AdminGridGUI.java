package world.bentobox.islandselector.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.Settings;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Admin Grid GUI for managing the island grid
 * Similar to MainGridGUI but with admin-specific actions:
 * - Left-click occupied: Show island info
 * - Right-click occupied: Admin actions menu (teleport, remove island, view slots)
 * - Left-click available: Reserve location
 * - Right-click available: Set premium price
 * - Left-click reserved: Unreserve
 * - Right-click reserved: Modify price
 *
 * 54 slots (6 rows x 9 columns) layout:
 * - Row 1: [UP][UP][control][control][control][control][control][UP][UP]
 * - Row 2: [LEFT][grid 7 slots][RIGHT]
 * - Row 3: [LEFT][grid 7 slots][RIGHT]
 * - Row 4: [LEFT][grid 7 slots][RIGHT]
 * - Row 5: [LEFT][grid 7 slots][RIGHT]
 * - Row 6: [DN][DN][control][control][control][control][control][DN][DN]
 */
public class AdminGridGUI implements InventoryHolder {

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

    // Top control bar (slots 2-6)
    private static final int TOP_FILTER_ALL_SLOT = 2;
    private static final int TOP_FILTER_OCCUPIED_SLOT = 3;
    private static final int TOP_FILTER_RESERVED_SLOT = 4;
    private static final int TOP_INFO_SLOT = 5;
    private static final int TOP_STATS_SLOT = 6;

    // Bottom control bar (slots 47-51)
    private static final int BOT_SEARCH_SLOT = 47;
    private static final int BOT_JUMP_SLOT = 48;
    private static final int BOT_RELOAD_SLOT = 49;
    private static final int BOT_BACKUP_SLOT = 50;
    private static final int BOT_CLOSE_SLOT = 51;

    // Arrow head textures
    private static final String ARROW_UP_URL = "http://textures.minecraft.net/texture/3040fe836a6c2fbd2c7a9c8ec6be5174fddf1ac20f55e366156fa5f712e10";
    private static final String ARROW_DOWN_URL = "http://textures.minecraft.net/texture/7437346d8bda78d525d19f540a95e4e79daeda795cbc5a13256236312cf";
    private static final String ARROW_LEFT_URL = "http://textures.minecraft.net/texture/bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9";
    private static final String ARROW_RIGHT_URL = "http://textures.minecraft.net/texture/19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf";

    private final IslandSelector addon;
    private final Player player;
    private final GridManager gridManager;
    private final Settings settings;
    private Inventory inventory;

    // Viewport position
    private int viewportX;
    private int viewportZ;

    // Current filter
    private FilterType filter = FilterType.ALL;

    // Relocation mode - when admin is selecting a new location for an island
    private boolean relocationMode = false;
    private GridCoordinate relocationSource = null;
    private UUID relocationPlayerUUID = null;
    private String relocationPlayerName = null;

    public enum FilterType {
        ALL, OCCUPIED, RESERVED
    }

    public AdminGridGUI(IslandSelector addon, Player player) {
        this.addon = addon;
        this.player = player;
        this.gridManager = addon.getGridManager();
        this.settings = addon.getSettings();

        // Start at center of grid
        this.viewportX = -GRID_COLS / 2;
        this.viewportZ = -GRID_ROWS / 2;

        // Clamp to grid boundaries
        clampViewport();
    }

    private void clampViewport() {
        this.viewportX = Math.max(settings.getGridMinX(), this.viewportX);
        this.viewportX = Math.min(settings.getGridMaxX() - GRID_COLS + 1, this.viewportX);
        this.viewportZ = Math.max(settings.getGridMinZ(), this.viewportZ);
        this.viewportZ = Math.min(settings.getGridMaxZ() - GRID_ROWS + 1, this.viewportZ);
    }

    public void open() {
        createInventory();
        populateInventory();
        player.openInventory(inventory);

        // Register click handler
        Bukkit.getPluginManager().registerEvents(new AdminGridGUIListener(this), addon.getPlugin());
    }

    public void reopen() {
        populateInventory();
        player.openInventory(inventory);
        Bukkit.getPluginManager().registerEvents(new AdminGridGUIListener(this), addon.getPlugin());
    }

    private void createInventory() {
        String title = relocationMode ?
            "§6§lRelocate: " + relocationPlayerName :
            "§4§lAdmin Grid Manager";
        inventory = Bukkit.createInventory(this, SIZE, title);
    }

    private void populateInventory() {
        inventory.clear();
        populateNavigationArrows();
        populateGrid();
        populateControlButtons();
        fillEmptySlots();
    }

    private void populateNavigationArrows() {
        boolean canScrollUp = viewportZ > settings.getGridMinZ();
        boolean canScrollDown = viewportZ + GRID_ROWS - 1 < settings.getGridMaxZ();
        boolean canScrollLeft = viewportX > settings.getGridMinX();
        boolean canScrollRight = viewportX + GRID_COLS - 1 < settings.getGridMaxX();

        ItemStack upArrow = canScrollUp ?
            createArrowHead(ARROW_UP_URL, "&eScroll Up (North)", "&7Click: Scroll 1", "&7Shift+Click: Jump 5") :
            createDisabledArrow("&7Can't scroll further north");
        for (int slot : ARROW_UP_SLOTS) {
            inventory.setItem(slot, upArrow);
        }

        ItemStack downArrow = canScrollDown ?
            createArrowHead(ARROW_DOWN_URL, "&eScroll Down (South)", "&7Click: Scroll 1", "&7Shift+Click: Jump 5") :
            createDisabledArrow("&7Can't scroll further south");
        for (int slot : ARROW_DOWN_SLOTS) {
            inventory.setItem(slot, downArrow);
        }

        ItemStack leftArrow = canScrollLeft ?
            createArrowHead(ARROW_LEFT_URL, "&eScroll Left (West)", "&7Click: Scroll 1", "&7Shift+Click: Jump 5") :
            createDisabledArrow("&7Can't scroll further west");
        for (int slot : ARROW_LEFT_SLOTS) {
            inventory.setItem(slot, leftArrow);
        }

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
            // Fallback
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
        ItemStack item = new ItemStack(Material.BARRIER);
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
                    ItemStack item = createGridLocationItem(coord);
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

    private ItemStack createGridLocationItem(GridCoordinate coord) {
        if (!gridManager.isWithinBounds(coord)) {
            return createLockedItem(coord);
        }

        GridLocation.Status status = gridManager.getLocationStatus(coord);
        GridLocation location = gridManager.getGridLocation(coord);

        // Apply filter
        if (filter != FilterType.ALL) {
            if (filter == FilterType.OCCUPIED && status != GridLocation.Status.OCCUPIED) {
                return createFilteredOutItem(coord);
            }
            if (filter == FilterType.RESERVED && status != GridLocation.Status.RESERVED) {
                return createFilteredOutItem(coord);
            }
        }

        switch (status) {
            case AVAILABLE:
                return createAvailableItem(coord);
            case OCCUPIED:
                return createOccupiedItem(coord, location);
            case RESERVED:
                return createReservedItem(coord, location);
            case LOCKED:
            default:
                return createLockedItem(coord);
        }
    }

    private String getWorldCoordsString(GridCoordinate coord) {
        int worldX = gridManager.getWorldX(coord);
        int worldZ = gridManager.getWorldZ(coord);
        return "X: " + worldX + ", Z: " + worldZ;
    }

    private ItemStack createAvailableItem(GridCoordinate coord) {
        ItemStack item;
        ItemMeta meta;

        if (relocationMode) {
            // In relocation mode, show as a target location
            item = new ItemStack(Material.ENDER_PEARL);
            meta = item.getItemMeta();
            meta.setDisplayName(colorize("&b" + coord.toString() + " - Move Here"));

            List<String> lore = new ArrayList<>();
            lore.add(colorize("&7Location: &f" + getWorldCoordsString(coord)));
            lore.add("");
            lore.add(colorize("&7Relocating: &f" + relocationPlayerName));
            lore.add(colorize("&7From: &f" + relocationSource.toString()));
            lore.add("");
            lore.add(colorize("&a[Click] &7Move island here"));
            lore.add(colorize("&c[Shift+Click] &7Cancel relocation"));

            meta.setLore(lore);
            // Add glow effect
            Enchantment glow = org.bukkit.Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft("unbreaking"));
            if (glow != null) {
                meta.addEnchant(glow, 1, true);
            }
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            meta = item.getItemMeta();
            meta.setDisplayName(colorize("&a" + coord.toString() + " - Available"));

            List<String> lore = new ArrayList<>();
            lore.add(colorize("&7Location: &f" + getWorldCoordsString(coord)));
            lore.add("");
            lore.add(colorize("&e[Left-Click] &7Reserve this location"));
            lore.add(colorize("&e[Right-Click] &7Set as premium (with price)"));

            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOccupiedItem(GridCoordinate coord, GridLocation location) {
        UUID ownerUUID = location != null ? location.getOwnerUUID() : null;
        String ownerName = location != null && location.getOwnerName() != null ? location.getOwnerName() : "Unknown";
        boolean isOwnerOnline = gridManager.isOwnerOnline(coord);

        ItemStack item;
        ItemMeta meta;

        // Use player head for online players, red stained glass for offline
        if (isOwnerOnline) {
            item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            if (ownerUUID != null) {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
                skullMeta.setOwningPlayer(owner);
            }
            meta = skullMeta;
        } else {
            // Offline player - use red stained glass
            item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            meta = item.getItemMeta();
        }

        meta.setDisplayName(colorize("&c" + coord.toString() + " - " + ownerName));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Location: &f" + getWorldCoordsString(coord)));
        lore.add("");
        lore.add(colorize("&7Owner: &f" + ownerName));

        if (isOwnerOnline) {
            lore.add(colorize("&a● Online"));
        } else {
            lore.add(colorize("&c○ Offline"));
            if (ownerUUID != null) {
                String offlineDuration = getOfflineDuration(ownerUUID);
                if (offlineDuration != null) {
                    lore.add(colorize("&7Last seen: &f" + offlineDuration + " ago"));
                }
            }
        }

        // Get active slot info
        if (ownerUUID != null) {
            SlotData activeSlot = addon.getSlotManager().getActiveSlot(ownerUUID);
            if (activeSlot != null) {
                lore.add("");
                lore.add(colorize("&7Active Slot: &e" + activeSlot.getSlotName()));
                int slotCount = addon.getSlotManager().getIslandCount(ownerUUID);
                lore.add(colorize("&7Total Slots: &f" + slotCount + "/" + settings.getMaxSlots()));
            }
        }

        // Island info
        Island island = getIslandForLocation(location);
        if (island != null) {
            lore.add("");
            long createdTime = island.getCreatedDate();
            if (createdTime > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
                lore.add(colorize("&7Created: &f" + sdf.format(new Date(createdTime))));
            }
            int teamSize = island.getMemberSet().size();
            lore.add(colorize("&7Team Size: &f" + teamSize));
        }

        lore.add("");
        lore.add(colorize("&e[Left-Click] &7Teleport to island"));
        lore.add(colorize("&e[Right-Click] &7Admin actions menu"));
        lore.add(colorize("&e[Shift+Click] &7Remove island"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createReservedItem(GridCoordinate coord, GridLocation location) {
        boolean isPurchasable = location != null && location.isPurchasable();

        ItemStack item = new ItemStack(isPurchasable ? Material.GOLD_BLOCK : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        if (isPurchasable) {
            String formattedPrice = String.format("%,d", (int) location.getPurchasePrice());
            meta.setDisplayName(colorize("&6" + coord.toString() + " - Premium $" + formattedPrice));
        } else {
            meta.setDisplayName(colorize("&7" + coord.toString() + " - Reserved"));
        }

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Location: &f" + getWorldCoordsString(coord)));
        lore.add("");

        if (isPurchasable) {
            lore.add(colorize("&7Price: &a$" + String.format("%,d", (int) location.getPurchasePrice())));
            lore.add("");
            lore.add(colorize("&e[Left-Click] &7Change price"));
            lore.add(colorize("&e[Right-Click] &7Remove price (block)"));
            lore.add(colorize("&e[Shift+Click] &7Unreserve"));
        } else {
            lore.add(colorize("&cBlocked - Not purchasable"));
            lore.add("");
            lore.add(colorize("&e[Left-Click] &7Unreserve"));
            lore.add(colorize("&e[Right-Click] &7Set price (make purchasable)"));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLockedItem(GridCoordinate coord) {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&8" + coord.toString() + " - Outside Grid"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Location: &f" + getWorldCoordsString(coord)));
        lore.add("");
        lore.add(colorize("&8Outside current grid boundaries"));
        lore.add(colorize("&7Use /islandselector admin expand"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

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

    private void populateControlButtons() {
        // Top bar controls
        ItemStack filterAll = createButton(Material.PAPER, "&fFilter: All",
            filter == FilterType.ALL ? "&aCurrently selected" : "&7Show all locations");
        if (filter == FilterType.ALL) addGlow(filterAll);
        inventory.setItem(TOP_FILTER_ALL_SLOT, filterAll);

        ItemStack filterOccupied = createButton(Material.PLAYER_HEAD, "&cFilter: Occupied",
            filter == FilterType.OCCUPIED ? "&aCurrently selected" : "&7Show occupied only");
        if (filter == FilterType.OCCUPIED) addGlow(filterOccupied);
        inventory.setItem(TOP_FILTER_OCCUPIED_SLOT, filterOccupied);

        ItemStack filterReserved = createButton(Material.GOLD_INGOT, "&6Filter: Reserved",
            filter == FilterType.RESERVED ? "&aCurrently selected" : "&7Show reserved only");
        if (filter == FilterType.RESERVED) addGlow(filterReserved);
        inventory.setItem(TOP_FILTER_RESERVED_SLOT, filterReserved);

        // Grid Info
        GridCoordinate topLeft = new GridCoordinate(viewportX, viewportZ);
        GridCoordinate bottomRight = new GridCoordinate(viewportX + GRID_COLS - 1, viewportZ + GRID_ROWS - 1);
        inventory.setItem(TOP_INFO_SLOT, createButton(Material.FILLED_MAP,
            "&eViewing: &f" + topLeft + " to " + bottomRight,
            "&7Grid: " + settings.getGridMinX() + "," + settings.getGridMinZ() +
                " to " + settings.getGridMaxX() + "," + settings.getGridMaxZ()));

        // Stats
        int occupied = gridManager.getOccupiedLocationCount();
        int total = settings.getGridWidth() * settings.getGridHeight();
        int reserved = gridManager.getReservedLocationCount();
        inventory.setItem(TOP_STATS_SLOT, createButton(Material.BOOK,
            "&bGrid Statistics",
            "&7Total Slots: &f" + total,
            "&7Occupied: &c" + occupied,
            "&7Reserved: &6" + reserved,
            "&7Available: &a" + (total - occupied - reserved)));

        // Bottom bar controls - different in relocation mode
        if (relocationMode) {
            // Relocation mode controls
            inventory.setItem(BOT_SEARCH_SLOT, createButton(Material.ENDER_EYE, "&dRelocating: &f" + relocationPlayerName,
                "&7From: &f" + relocationSource.toString(),
                "&7Click an &aavailable &7location to move"));

            inventory.setItem(BOT_JUMP_SLOT, createButton(Material.ENDER_PEARL, "&dJump to Coordinate",
                "&7Go to specific grid location"));

            inventory.setItem(BOT_RELOAD_SLOT, createButton(Material.GRAY_STAINED_GLASS_PANE, " "));

            inventory.setItem(BOT_BACKUP_SLOT, createButton(Material.GRAY_STAINED_GLASS_PANE, " "));

            inventory.setItem(BOT_CLOSE_SLOT, createButton(Material.BARRIER, "&cCancel Relocation",
                "&7Exit relocation mode"));
        } else {
            // Normal controls
            inventory.setItem(BOT_SEARCH_SLOT, createButton(Material.COMPASS, "&eSearch Player",
                "&7Find a player's island"));

            inventory.setItem(BOT_JUMP_SLOT, createButton(Material.ENDER_PEARL, "&dJump to Coordinate",
                "&7Go to specific grid location"));

            inventory.setItem(BOT_RELOAD_SLOT, createButton(Material.COMMAND_BLOCK, "&aReload Grid",
                "&7Sync with BSkyBlock islands"));

            inventory.setItem(BOT_BACKUP_SLOT, createButton(Material.CHEST, "&6Backup All",
                "&7Create backup of all online", "&7players' active slots"));

            inventory.setItem(BOT_CLOSE_SLOT, createButton(Material.BARRIER, "&cClose",
                "&7Close this menu"));
        }
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

    private Island getIslandForLocation(GridLocation location) {
        if (location == null || location.getIslandId() == null) {
            return null;
        }
        try {
            return BentoBox.getInstance().getIslands().getIslandById(location.getIslandId()).orElse(null);
        } catch (Exception e) {
            if (location.getOwnerUUID() != null && addon.getGridManager().getBSkyBlockWorld() != null) {
                return BentoBox.getInstance().getIslands()
                    .getIsland(addon.getGridManager().getBSkyBlockWorld(), location.getOwnerUUID());
            }
        }
        return null;
    }

    private String getOfflineDuration(UUID playerUUID) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
        long lastPlayed = offlinePlayer.getLastPlayed();
        if (lastPlayed == 0) return null;

        long diff = System.currentTimeMillis() - lastPlayed;
        if (diff < 0) return null;

        long days = TimeUnit.MILLISECONDS.toDays(diff);
        if (days > 365) return (days / 365) + " years";
        if (days > 30) return (days / 30) + " months";
        if (days > 0) return days + " days";

        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        if (hours > 0) return hours + " hours";

        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        if (minutes > 0) return minutes + " minutes";

        return "moments";
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

    public void setFilter(FilterType newFilter) {
        this.filter = newFilter;
        refresh();
    }

    public void centerViewportOn(GridCoordinate coord) {
        viewportX = coord.getX() - GRID_COLS / 2;
        viewportZ = coord.getZ() - GRID_ROWS / 2;
        clampViewport();
        refresh();
    }

    public void refresh() {
        populateInventory();
        player.updateInventory();
    }

    public GridCoordinate getCoordinateForSlot(int slot) {
        int gridRow = -1;
        int gridCol = -1;

        if (slot >= 10 && slot <= 16) {
            gridRow = 0;
            gridCol = slot - 10;
        } else if (slot >= 19 && slot <= 25) {
            gridRow = 1;
            gridCol = slot - 19;
        } else if (slot >= 28 && slot <= 34) {
            gridRow = 2;
            gridCol = slot - 28;
        } else if (slot >= 37 && slot <= 43) {
            gridRow = 3;
            gridCol = slot - 37;
        }

        if (gridRow >= 0 && gridCol >= 0) {
            return new GridCoordinate(viewportX + gridCol, viewportZ + gridRow);
        }
        return null;
    }

    // Slot check methods
    public boolean isScrollUpSlot(int slot) {
        for (int s : ARROW_UP_SLOTS) if (s == slot) return true;
        return false;
    }

    public boolean isScrollDownSlot(int slot) {
        for (int s : ARROW_DOWN_SLOTS) if (s == slot) return true;
        return false;
    }

    public boolean isScrollLeftSlot(int slot) {
        for (int s : ARROW_LEFT_SLOTS) if (s == slot) return true;
        return false;
    }

    public boolean isScrollRightSlot(int slot) {
        for (int s : ARROW_RIGHT_SLOTS) if (s == slot) return true;
        return false;
    }

    // Getters
    public Player getPlayer() { return player; }
    public IslandSelector getAddon() { return addon; }
    public FilterType getFilter() { return filter; }

    public int getFilterAllSlot() { return TOP_FILTER_ALL_SLOT; }
    public int getFilterOccupiedSlot() { return TOP_FILTER_OCCUPIED_SLOT; }
    public int getFilterReservedSlot() { return TOP_FILTER_RESERVED_SLOT; }
    public int getSearchSlot() { return BOT_SEARCH_SLOT; }
    public int getJumpSlot() { return BOT_JUMP_SLOT; }
    public int getReloadSlot() { return BOT_RELOAD_SLOT; }
    public int getBackupSlot() { return BOT_BACKUP_SLOT; }
    public int getCloseSlot() { return BOT_CLOSE_SLOT; }

    // Relocation mode methods
    public boolean isRelocationMode() { return relocationMode; }
    public GridCoordinate getRelocationSource() { return relocationSource; }
    public UUID getRelocationPlayerUUID() { return relocationPlayerUUID; }
    public String getRelocationPlayerName() { return relocationPlayerName; }

    /**
     * Enter relocation mode - allows admin to select a new location for a player's island
     * Requires FAWE to be available
     */
    public void enterRelocationMode(GridCoordinate source, UUID playerUUID, String playerName) {
        // Check if FAWE is available
        if (!addon.isSchematicOperationsAvailable()) {
            player.sendMessage("§cRelocation requires FastAsyncWorldEdit (FAWE) to be installed.");
            return;
        }

        this.relocationMode = true;
        this.relocationSource = source;
        this.relocationPlayerUUID = playerUUID;
        this.relocationPlayerName = playerName;
        // Recreate inventory with new title and register new listener
        createInventory();
        populateInventory();
        player.openInventory(inventory);
        // Register listener for the new inventory (old one was unregistered on close)
        Bukkit.getPluginManager().registerEvents(new AdminGridGUIListener(this), addon.getPlugin());
    }

    /**
     * Exit relocation mode
     */
    public void exitRelocationMode() {
        this.relocationMode = false;
        this.relocationSource = null;
        this.relocationPlayerUUID = null;
        this.relocationPlayerName = null;
        // Recreate inventory with normal title and register new listener
        createInventory();
        populateInventory();
        player.openInventory(inventory);
        // Register listener for the new inventory (old one was unregistered on close)
        Bukkit.getPluginManager().registerEvents(new AdminGridGUIListener(this), addon.getPlugin());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
