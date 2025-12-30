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
 * Main Grid GUI for island selection
 * 54 slots (6 rows x 9 columns) layout:
 * - Row 1: [UP][UP][control][control][control][control][control][UP][UP]
 * - Row 2: [LEFT][grid 7 slots][RIGHT]
 * - Row 3: [LEFT][grid 7 slots][RIGHT]
 * - Row 4: [LEFT][grid 7 slots][RIGHT]
 * - Row 5: [LEFT][grid 7 slots][RIGHT]
 * - Row 6: [DN][DN][control][control][control][control][control][DN][DN]
 */
public class MainGridGUI implements InventoryHolder {

    // GUI Layout Constants
    private static final int ROWS = 6;
    private static final int COLS = 9;
    private static final int SIZE = ROWS * COLS; // 54 slots

    // Grid viewport: 7 columns x 4 rows = 28 visible island slots
    private static final int GRID_COLS = 7;
    private static final int GRID_ROWS = 4;

    // Navigation arrow positions - corners and edges for up/down
    private static final int[] ARROW_UP_SLOTS = {0, 1, 7, 8};
    private static final int[] ARROW_DOWN_SLOTS = {45, 46, 52, 53};
    private static final int[] ARROW_LEFT_SLOTS = {9, 18, 27, 36};
    private static final int[] ARROW_RIGHT_SLOTS = {17, 26, 35, 44};

    // Top control bar (slots 2-6)
    private static final int TOP_INFO_SLOT = 4;  // Centered info display

    // Bottom control bar (slots 47-51)
    private static final int BOT_SEARCH_SLOT = 47;
    private static final int BOT_FIND_MY_ISLAND_SLOT = 48;
    private static final int BOT_NEIGHBORHOOD_SLOT = 49;
    private static final int BOT_SLOTS_SLOT = 50;
    private static final int BOT_CLOSE_SLOT = 51;

    // Arrow head textures (URLs for minecraft textures)
    private static final String ARROW_UP_URL = "http://textures.minecraft.net/texture/3040fe836a6c2fbd2c7a9c8ec6be5174fddf1ac20f55e366156fa5f712e10";
    private static final String ARROW_DOWN_URL = "http://textures.minecraft.net/texture/7437346d8bda78d525d19f540a95e4e79daeda795cbc5a13256236312cf";
    private static final String ARROW_LEFT_URL = "http://textures.minecraft.net/texture/bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9";
    private static final String ARROW_RIGHT_URL = "http://textures.minecraft.net/texture/19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf";

    private final IslandSelector addon;
    private final Player player;
    private final GridManager gridManager;
    private final Settings settings;
    private Inventory inventory;

    // Viewport position (top-left corner of visible grid in grid coordinates)
    private int viewportX;
    private int viewportZ;

    public MainGridGUI(IslandSelector addon, Player player) {
        this.addon = addon;
        this.player = player;
        this.gridManager = addon.getGridManager();
        this.settings = addon.getSettings();

        // Check if player has an island - center on their island if so
        GridCoordinate playerIsland = gridManager.getPlayerIslandCoordinate(player.getUniqueId());
        if (playerIsland != null) {
            // Center viewport on player's island
            this.viewportX = playerIsland.getX() - GRID_COLS / 2;
            this.viewportZ = playerIsland.getZ() - GRID_ROWS / 2;
        } else {
            // No island - center at 0,0
            this.viewportX = -GRID_COLS / 2;
            this.viewportZ = -GRID_ROWS / 2;
        }

        // Clamp to grid boundaries
        this.viewportX = Math.max(settings.getGridMinX(), this.viewportX);
        this.viewportX = Math.min(settings.getGridMaxX() - GRID_COLS + 1, this.viewportX);
        this.viewportZ = Math.max(settings.getGridMinZ(), this.viewportZ);
        this.viewportZ = Math.min(settings.getGridMaxZ() - GRID_ROWS + 1, this.viewportZ);
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
     * Reopen the GUI after it was closed (e.g., for search input).
     * This re-registers the click handler that was unregistered on close.
     */
    public void reopen() {
        populateInventory();
        player.openInventory(inventory);

        // Re-register click handler (was unregistered when inventory closed)
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
     * Build the GUI title
     */
    private String buildTitle() {
        return "Island Grid Selector";
    }

    /**
     * Populate the inventory with items
     */
    private void populateInventory() {
        // Clear inventory
        inventory.clear();

        // Add navigation arrows
        populateNavigationArrows();

        // Add grid display (7 columns x 4 rows in the middle)
        populateGrid();

        // Add control buttons (top and bottom bars)
        populateControlButtons();

        // Fill empty slots with filler
        fillEmptySlots();
    }

    /**
     * Populate navigation arrows with player head textures
     */
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

    /**
     * Create a player head with custom texture for arrow display
     */
    private ItemStack createArrowHead(String textureUrl, String name, String... loreLines) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        // Set the texture using profile
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

    /**
     * Create a disabled arrow (barrier block)
     */
    private ItemStack createDisabledArrow(String tooltip) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize(tooltip));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Populate the grid display area (7 columns x 4 rows)
     * Grid occupies slots 10-16, 19-25, 28-34, 37-43
     */
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

    /**
     * Get the inventory slot for a grid position
     * Grid is 7 columns x 4 rows in the center
     */
    private int getGridSlot(int gridRow, int gridCol) {
        // Row 2 (inventory row 1): slots 10-16 (7 grid slots)
        // Row 3 (inventory row 2): slots 19-25 (7 grid slots)
        // Row 4 (inventory row 3): slots 28-34 (7 grid slots)
        // Row 5 (inventory row 4): slots 37-43 (7 grid slots)
        switch (gridRow) {
            case 0: return 10 + gridCol; // Slots 10-16
            case 1: return 19 + gridCol; // Slots 19-25
            case 2: return 28 + gridCol; // Slots 28-34
            case 3: return 37 + gridCol; // Slots 37-43
            default: return -1;
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

        // Get status first - this may register the island from BSkyBlock
        GridLocation.Status status = gridManager.getLocationStatus(coord);
        // Get location after status check (may have been created)
        GridLocation location = gridManager.getGridLocation(coord);

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
     * Parse a material name from config, with fallback
     */
    private Material parseMaterial(String name, Material fallback) {
        try {
            Material mat = Material.valueOf(name.toUpperCase());
            return mat;
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /**
     * Get world coordinates string for a grid coordinate
     */
    private String getWorldCoordsString(GridCoordinate coord) {
        int worldX = gridManager.getWorldX(coord);
        int worldZ = gridManager.getWorldZ(coord);
        return "X: " + worldX + ", Z: " + worldZ;
    }

    /**
     * Create item for available location
     */
    private ItemStack createAvailableItem(GridCoordinate coord) {
        Material mat = parseMaterial(settings.getItemAvailable(), Material.GREEN_STAINED_GLASS_PANE);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&a" + coord.toString() + " - Available"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Location: &f" + getWorldCoordsString(coord)));
        lore.add("");

        // Check if player has an island
        GridCoordinate playerIsland = gridManager.getPlayerIslandCoordinate(player.getUniqueId());
        if (playerIsland == null) {
            lore.add(colorize("&eClick to claim this location"));
        } else if (addon.isSchematicOperationsAvailable()) {
            // Only show relocate option if FAWE is installed
            lore.add(colorize("&eClick to relocate here"));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create item for occupied location with detailed island info
     */
    private ItemStack createOccupiedItem(GridCoordinate coord, GridLocation location) {
        boolean isOwnerOnline = gridManager.isOwnerOnline(coord);
        boolean isOwnIsland = location != null && location.getOwnerUUID() != null &&
            location.getOwnerUUID().equals(player.getUniqueId());

        UUID ownerUUID = location != null ? location.getOwnerUUID() : null;
        String ownerName = location != null && location.getOwnerName() != null ? location.getOwnerName() : "Unknown";

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

        if (isOwnIsland) {
            meta.setDisplayName(colorize("&a&l" + coord.toString() + " - Your Island"));
            // Add enchantment glow
            Enchantment glow = org.bukkit.Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft("unbreaking"));
            if (glow != null) {
                meta.addEnchant(glow, 1, true);
            }
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.setDisplayName(colorize("&f" + coord.toString() + " - " + ownerName));
        }

        List<String> lore = new ArrayList<>();

        // World coordinates
        lore.add(colorize("&7Location: &f" + getWorldCoordsString(coord)));
        lore.add("");

        if (isOwnIsland) {
            lore.add(colorize("&6Your Island"));
        } else {
            lore.add(colorize("&7Owner: &f" + ownerName));
            if (isOwnerOnline) {
                lore.add(colorize("&a● Online"));
            } else {
                lore.add(colorize("&c○ Offline"));
                // Show how long they've been offline
                if (ownerUUID != null) {
                    String offlineDuration = getOfflineDuration(ownerUUID);
                    if (offlineDuration != null) {
                        lore.add(colorize("&7Last seen: &f" + offlineDuration + " ago"));
                    }
                }
            }
        }

        // Get active slot info
        if (ownerUUID != null) {
            SlotData activeSlot = addon.getSlotManager().getActiveSlot(ownerUUID);
            if (activeSlot != null) {
                lore.add("");
                lore.add(colorize("&7Active Slot: &e" + activeSlot.getSlotName()));

                // Show blueprint if set
                if (activeSlot.getBlueprintBundle() != null && !activeSlot.getBlueprintBundle().isEmpty()) {
                    lore.add(colorize("&7Blueprint: &f" + formatBlueprintName(activeSlot.getBlueprintBundle())));
                }
            }
        }

        // Island creation date and other BentoBox data
        Island island = getIslandForLocation(location);
        if (island != null) {
            lore.add("");

            // Creation date
            long createdTime = island.getCreatedDate();
            if (createdTime > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
                String createdDate = sdf.format(new Date(createdTime));
                lore.add(colorize("&7Created: &f" + createdDate));
            }

            // Team members count
            int teamSize = island.getMemberSet().size();
            if (teamSize > 1) {
                lore.add(colorize("&7Team Members: &f" + teamSize));
            }

            // Protection range
            int protectionRange = island.getProtectionRange();
            lore.add(colorize("&7Protection: &f" + protectionRange + " blocks"));
        }

        // Island level from Level addon
        if (ownerUUID != null && addon.getLevelIntegration().isEnabled()) {
            String levelStr = addon.getLevelIntegration().getFormattedIslandLevel(ownerUUID);
            if (!levelStr.equals("N/A")) {
                lore.add(colorize("&7Island Level: &b" + levelStr));
            }
        }

        // Challenges completed from Challenges addon
        if (ownerUUID != null && addon.getChallengesIntegration().isEnabled()) {
            int completed = addon.getChallengesIntegration().getCompletedChallengeCount(ownerUUID);
            int total = addon.getChallengesIntegration().getTotalChallengeCount();
            if (completed >= 0 && total > 0) {
                lore.add(colorize("&7Challenges: &d" + completed + "/" + total));
            } else if (completed >= 0) {
                lore.add(colorize("&7Challenges: &d" + completed + " completed"));
            }
        }

        // Action hint
        if (!isOwnIsland) {
            lore.add("");
            lore.add(colorize("&e▶ Right-click to visit"));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Get a BentoBox Island object for a GridLocation
     */
    private Island getIslandForLocation(GridLocation location) {
        if (location == null || location.getIslandId() == null) {
            return null;
        }

        try {
            // Try to get island by ID
            return BentoBox.getInstance().getIslands().getIslandById(location.getIslandId()).orElse(null);
        } catch (Exception e) {
            // Fallback - try getting by owner UUID
            if (location.getOwnerUUID() != null && addon.getGridManager().getBSkyBlockWorld() != null) {
                return BentoBox.getInstance().getIslands()
                    .getIsland(addon.getGridManager().getBSkyBlockWorld(), location.getOwnerUUID());
            }
        }
        return null;
    }

    /**
     * Format the time since a player was last online
     */
    private String getOfflineDuration(UUID playerUUID) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
        long lastPlayed = offlinePlayer.getLastPlayed();

        if (lastPlayed == 0) {
            return null; // Never played or data unavailable
        }

        long now = System.currentTimeMillis();
        long diff = now - lastPlayed;

        if (diff < 0) {
            return null;
        }

        long days = TimeUnit.MILLISECONDS.toDays(diff);
        if (days > 365) {
            long years = days / 365;
            return years + (years == 1 ? " year" : " years");
        } else if (days > 30) {
            long months = days / 30;
            return months + (months == 1 ? " month" : " months");
        } else if (days > 0) {
            return days + (days == 1 ? " day" : " days");
        }

        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        if (hours > 0) {
            return hours + (hours == 1 ? " hour" : " hours");
        }

        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        if (minutes > 0) {
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }

        return "moments";
    }

    /**
     * Format a blueprint bundle name to be more readable
     * e.g., "my_cool_island" -> "My Cool Island"
     */
    private String formatBlueprintName(String bundleName) {
        if (bundleName == null || bundleName.isEmpty()) {
            return bundleName;
        }

        // Replace underscores with spaces and capitalize each word
        String[] words = bundleName.replace("_", " ").replace("-", " ").split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }
        return result.toString();
    }

    /**
     * Create item for purchasable reserved location
     */
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
        lore.add(colorize("&eClick to purchase"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create item for blocked reserved location
     */
    private ItemStack createBlockedItem(GridCoordinate coord) {
        Material mat = parseMaterial(settings.getItemReservedBlocked(), Material.GRAY_STAINED_GLASS_PANE);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&7" + coord.toString() + " - Reserved"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Location: &f" + getWorldCoordsString(coord)));
        lore.add("");
        lore.add(colorize("&cThis location is not available"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create item for locked area
     */
    private ItemStack createLockedItem(GridCoordinate coord) {
        Material mat = parseMaterial(settings.getItemLockedArea(), Material.BLACK_STAINED_GLASS_PANE);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&8" + coord.toString() + " - Locked"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Location: &f" + getWorldCoordsString(coord)));
        lore.add("");
        lore.add(colorize("&8Locked Area"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }


    /**
     * Populate control buttons (top bar and bottom bar)
     */
    private void populateControlButtons() {
        // Top bar - Grid Info (centered at slot 4)
        GridCoordinate topLeft = new GridCoordinate(viewportX, viewportZ);
        GridCoordinate bottomRight = new GridCoordinate(
            viewportX + GRID_COLS - 1,
            viewportZ + GRID_ROWS - 1
        );
        int occupied = gridManager.getOccupiedLocationCount();
        int total = settings.getGridWidth() * settings.getGridHeight();
        ItemStack info = createButton(Material.FILLED_MAP, "&eViewing: &f" + topLeft + " to " + bottomRight,
            "&7Total grid slots: &f" + total,
            "&7Occupied: &f" + occupied,
            "&7Available: &f" + (total - occupied));
        inventory.setItem(TOP_INFO_SLOT, info);

        // Bottom bar controls (slots 47-51)
        // Search
        ItemStack search = createButton(Material.COMPASS, "&eSearch Player",
            "&7Find a player's island");
        inventory.setItem(BOT_SEARCH_SLOT, search);

        // Find My Island
        ItemStack findMyIsland = createButton(Material.ENDER_EYE, "&dFind My Island",
            "&7Center view on your island");
        inventory.setItem(BOT_FIND_MY_ISLAND_SLOT, findMyIsland);

        // Neighborhood View
        ItemStack neighborhood = createButton(Material.FILLED_MAP, "&bNeighborhood",
            "&7View your neighbors");
        inventory.setItem(BOT_NEIGHBORHOOD_SLOT, neighborhood);

        // Slot Selection - only show if FAWE is available
        if (addon.isSchematicOperationsAvailable()) {
            ItemStack slots = createButton(Material.CHEST, "&6Island Slots",
                "&7Manage your slots");
            inventory.setItem(BOT_SLOTS_SLOT, slots);
        }
        // If FAWE not available, slot 50 will be filled with filler later

        // Close
        ItemStack close = createButton(Material.BARRIER, "&cClose",
            "&7Close this menu");
        inventory.setItem(BOT_CLOSE_SLOT, close);
    }

    /**
     * Fill empty slots with filler item from config
     */
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

    // Helper methods

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
     * Handle scroll up (north, decreasing Z)
     */
    public void scrollUp(boolean shift) {
        int amount = shift ? settings.getScrollAmountShift() : settings.getScrollAmount();
        viewportZ = Math.max(settings.getGridMinZ(), viewportZ - amount);
        refresh();
    }

    /**
     * Handle scroll down (south, increasing Z)
     */
    public void scrollDown(boolean shift) {
        int amount = shift ? settings.getScrollAmountShift() : settings.getScrollAmount();
        int maxZ = settings.getGridMaxZ() - GRID_ROWS + 1;
        viewportZ = Math.min(maxZ, viewportZ + amount);
        refresh();
    }

    /**
     * Handle scroll left (west, decreasing X)
     */
    public void scrollLeft(boolean shift) {
        int amount = shift ? settings.getScrollAmountShift() : settings.getScrollAmount();
        viewportX = Math.max(settings.getGridMinX(), viewportX - amount);
        refresh();
    }

    /**
     * Handle scroll right (east, increasing X)
     */
    public void scrollRight(boolean shift) {
        int amount = shift ? settings.getScrollAmountShift() : settings.getScrollAmount();
        int maxX = settings.getGridMaxX() - GRID_COLS + 1;
        viewportX = Math.min(maxX, viewportX + amount);
        refresh();
    }

    /**
     * Center view on player's island
     */
    public void findMyIsland() {
        GridCoordinate myIsland = gridManager.getPlayerIslandCoordinate(player.getUniqueId());
        if (myIsland != null) {
            // Center the viewport on the player's island
            viewportX = myIsland.getX() - GRID_COLS / 2;
            viewportZ = myIsland.getZ() - GRID_ROWS / 2;

            // Clamp to grid boundaries
            viewportX = Math.max(settings.getGridMinX(), viewportX);
            viewportX = Math.min(settings.getGridMaxX() - GRID_COLS + 1, viewportX);
            viewportZ = Math.max(settings.getGridMinZ(), viewportZ);
            viewportZ = Math.min(settings.getGridMaxZ() - GRID_ROWS + 1, viewportZ);

            refresh();
            player.sendMessage(colorize("&aCentered on your island at " + myIsland.toString()));
        } else {
            player.sendMessage(colorize("&cYou don't have an island yet!"));
        }
    }

    /**
     * Refresh the GUI contents without closing
     * This preserves cursor position but cannot update the title
     */
    public void refresh() {
        // Just update contents in place - preserves cursor position
        populateInventory();
        player.updateInventory();
    }

    /**
     * Center the viewport on a specific grid coordinate
     * Used by search and other features that need to navigate to a location
     */
    public void centerViewportOn(GridCoordinate coord) {
        // Center the viewport on the target coordinate
        viewportX = coord.getX() - GRID_COLS / 2;
        viewportZ = coord.getZ() - GRID_ROWS / 2;

        // Clamp to grid boundaries
        viewportX = Math.max(settings.getGridMinX(), viewportX);
        viewportX = Math.min(settings.getGridMaxX() - GRID_COLS + 1, viewportX);
        viewportZ = Math.max(settings.getGridMinZ(), viewportZ);
        viewportZ = Math.min(settings.getGridMaxZ() - GRID_ROWS + 1, viewportZ);

        // Refresh the display
        refresh();
    }

    /**
     * Get the grid coordinate for an inventory slot
     */
    public GridCoordinate getCoordinateForSlot(int slot) {
        // Check if this slot is in the grid area
        int gridRow = -1;
        int gridCol = -1;

        // Row 2: slots 10-16 (7 grid slots)
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
        // Row 5: slots 37-43
        else if (slot >= 37 && slot <= 43) {
            gridRow = 3;
            gridCol = slot - 37;
        }

        if (gridRow >= 0 && gridCol >= 0) {
            int coordX = viewportX + gridCol;
            int coordZ = viewportZ + gridRow;
            return new GridCoordinate(coordX, coordZ);
        }

        return null;
    }

    /**
     * Check if a slot is a scroll up button
     */
    public boolean isScrollUpSlot(int slot) {
        for (int s : ARROW_UP_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    /**
     * Check if a slot is a scroll down button
     */
    public boolean isScrollDownSlot(int slot) {
        for (int s : ARROW_DOWN_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    /**
     * Check if a slot is a scroll left button
     */
    public boolean isScrollLeftSlot(int slot) {
        for (int s : ARROW_LEFT_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    /**
     * Check if a slot is a scroll right button
     */
    public boolean isScrollRightSlot(int slot) {
        for (int s : ARROW_RIGHT_SLOTS) {
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

    public int getSearchSlot() {
        return BOT_SEARCH_SLOT;
    }

    public int getFindMyIslandSlot() {
        return BOT_FIND_MY_ISLAND_SLOT;
    }

    public int getNeighborhoodSlot() {
        return BOT_NEIGHBORHOOD_SLOT;
    }

    public int getSlotsSlot() {
        return BOT_SLOTS_SLOT;
    }

    public int getCloseSlot() {
        return BOT_CLOSE_SLOT;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
