package world.bentobox.islandselector.gui;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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

import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.island.NewIsland;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.Settings;
import world.bentobox.islandselector.database.SlotData;
import world.bentobox.islandselector.listeners.IslandCreateListener;
import world.bentobox.islandselector.managers.GridLocationStrategy;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI for homeless players to select a location to restore their saved island.
 * Uses the same styling as IslandClaimGUI for consistency.
 */
public class IslandRestoreGUI implements InventoryHolder, Listener {

    // GUI Layout Constants (same as IslandClaimGUI)
    private static final int ROWS = 6;
    private static final int COLS = 9;
    private static final int SIZE = ROWS * COLS;
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

    // Arrow head textures (same as IslandClaimGUI)
    private static final String ARROW_UP_URL = "http://textures.minecraft.net/texture/3040fe836a6c2fbd2c7a9c8ec6be5174fddf1ac20f55e366156fa5f712e10";
    private static final String ARROW_DOWN_URL = "http://textures.minecraft.net/texture/7437346d8bda78d525d19f540a95e4e79daeda795cbc5a13256236312cf";
    private static final String ARROW_LEFT_URL = "http://textures.minecraft.net/texture/bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9";
    private static final String ARROW_RIGHT_URL = "http://textures.minecraft.net/texture/19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf";

    private final IslandSelector addon;
    private final Player player;
    private final GridManager gridManager;
    private final Settings settings;
    private final IslandCreateListener createListener;
    private final SlotData slotData;
    private Inventory inventory;

    private int viewportX;
    private int viewportZ;
    private GridCoordinate selectedCoord = null;

    public IslandRestoreGUI(IslandSelector addon, Player player, IslandCreateListener createListener, SlotData slotData) {
        this.addon = addon;
        this.player = player;
        this.gridManager = addon.getGridManager();
        this.settings = addon.getSettings();
        this.createListener = createListener;
        this.slotData = slotData;

        this.viewportX = -GRID_COLS / 2;
        this.viewportZ = -GRID_ROWS / 2;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        inventory = Bukkit.createInventory(this, SIZE, "Restore Island - Select Location");
        populateInventory();
        Bukkit.getPluginManager().registerEvents(this, addon.getPlugin());
        player.openInventory(inventory);
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
            lore.add(colorize("&a✓ Selected for restoration"));
            lore.add(colorize("&eClick again to confirm"));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOccupiedItem(GridCoordinate coord, GridLocation location) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

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
        // Title/info in top center - use NETHER_STAR like IslandClaimGUI
        ItemStack title = new ItemStack(Material.NETHER_STAR);
        ItemMeta titleMeta = title.getItemMeta();
        titleMeta.setDisplayName(colorize("&a&lRestore Island - Select Location"));

        List<String> titleLore = new ArrayList<>();
        titleLore.add("");
        titleLore.add(colorize("&7Click on a &agreen &7location"));
        titleLore.add(colorize("&7to place your restored island!"));
        titleLore.add("");
        titleLore.add(colorize("&fRestoring: &e" + slotData.getSlotName()));
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
        cancelLore.add(colorize("&7Click to cancel restoration"));
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

    // Navigation methods with shift support
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player clicker = (Player) event.getWhoClicked();
        if (!clicker.equals(player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;

        boolean shift = event.isShiftClick();

        // Navigation
        if (isScrollUpSlot(slot)) { scrollUp(shift); return; }
        if (isScrollDownSlot(slot)) { scrollDown(shift); return; }
        if (isScrollLeftSlot(slot)) { scrollLeft(shift); return; }
        if (isScrollRightSlot(slot)) { scrollRight(shift); return; }

        // Handle cancel
        if (slot == CANCEL_SLOT) {
            player.closeInventory();
            player.sendMessage(colorize("&cIsland restoration cancelled."));
            addon.getSlotManager().clearPendingSlotRestoration(player.getUniqueId());
            return;
        }

        // Handle grid click
        GridCoordinate coord = getCoordinateForSlot(slot);
        if (coord != null) {
            handleGridClick(coord);
        }
    }

    private void handleGridClick(GridCoordinate coord) {
        if (!gridManager.isWithinBounds(coord)) {
            return;
        }

        GridLocation.Status status = gridManager.getLocationStatus(coord);
        if (status != GridLocation.Status.AVAILABLE) {
            player.sendMessage(colorize("&cThis location is not available."));
            return;
        }

        // Double-click to confirm (like IslandClaimGUI)
        if (selectedCoord != null && selectedCoord.equals(coord)) {
            // Second click - confirm restoration
            confirmRestoration(coord);
        } else {
            // First click - select
            selectedCoord = coord;
            player.sendMessage(colorize("&aSelected location " + coord.toString() + ". Click again to confirm!"));
            refresh();
        }
    }

    private void confirmRestoration(GridCoordinate coord) {
        player.closeInventory();
        player.sendMessage(colorize("&a&lRestoring your island to " + coord.toString() + "..."));
        player.sendMessage(colorize("&7Please wait while your island is being placed."));

        // IMPORTANT: Teleport player to spawn FIRST to prevent BentoBox's teleport from conflicting
        Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        player.teleport(spawn);
        player.sendMessage(colorize("&7Preparing your island..."));

        // Perform restoration async
        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            try {
                restoreIslandAtLocation(coord);
            } catch (Exception e) {
                addon.logError("Failed to restore island: " + e.getMessage());
                e.printStackTrace();
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    player.sendMessage(colorize("&cFailed to restore your island. Please contact an administrator."));
                });
            }
        });
    }

    private void restoreIslandAtLocation(GridCoordinate coord) throws Exception {
        UUID playerUUID = player.getUniqueId();

        // Get the schematic file
        String schematicPath = addon.getSlotManager().getSlotSchematicPath(playerUUID, slotData.getSlotNumber());
        File schematicFile = new File(schematicPath);

        if (!schematicFile.exists()) {
            throw new Exception("Schematic file not found: " + schematicPath);
        }

        // First, create a BentoBox island at this location
        World bskyblockWorld = gridManager.getBSkyBlockWorld();
        if (bskyblockWorld == null) {
            throw new Exception("BSkyBlock world not available");
        }

        // Create island using BentoBox NewIsland builder with custom location strategy
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            try {
                User user = User.getInstance(player);
                GridLocationStrategy locationStrategy = new GridLocationStrategy(addon, coord, bskyblockWorld);

                addon.log("Starting island creation for restoration at " + coord + " for player " + playerUUID);

                // Build island with a minimal blueprint - noPaste prevents BentoBox from pasting AND teleporting
                NewIsland.builder()
                    .player(user)
                    .addon(addon.getBSkyBlockAddon())
                    .reason(IslandEvent.Reason.CREATE)
                    .locationStrategy(locationStrategy)
                    .noPaste() // Don't paste a blueprint - we'll paste the saved schematic
                    .build();

                // Poll for island creation with increasing delays
                pollForIslandCreation(playerUUID, bskyblockWorld, coord, schematicFile, 0);

            } catch (Exception e) {
                addon.logError("Failed to create island for restoration: " + e.getMessage());
                e.printStackTrace();
                player.sendMessage(colorize("&cFailed to create island. Please try again."));
            }
        });
    }

    /**
     * Poll for island creation with increasing delays.
     */
    private void pollForIslandCreation(UUID playerUUID, World bskyblockWorld, GridCoordinate coord,
                                        File schematicFile, int attempt) {
        int maxAttempts = 10;
        int delayTicks = 20;

        if (attempt >= maxAttempts) {
            addon.logError("Island creation timed out after " + maxAttempts + " attempts for player " + playerUUID);
            player.sendMessage(colorize("&cIsland creation timed out. Please try again or contact an administrator."));
            return;
        }

        Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
            Island island = addon.getIslands().getIsland(bskyblockWorld, playerUUID);

            if (island == null) {
                addon.log("Island not yet created, attempt " + (attempt + 1) + "/" + maxAttempts);
                pollForIslandCreation(playerUUID, bskyblockWorld, coord, schematicFile, attempt + 1);
                return;
            }

            addon.log("Island created successfully for player " + playerUUID + " at attempt " + (attempt + 1));

            // Island exists! Now paste the schematic
            Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
                try {
                    pasteSchematic(schematicFile, island.getCenter());

                    // Update slot data and teleport on main thread
                    Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                        finishRestoration(playerUUID, island, coord);
                    });

                } catch (Exception e) {
                    addon.logError("Failed to paste schematic: " + e.getMessage());
                    e.printStackTrace();
                    Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                        player.sendMessage(colorize("&cFailed to paste your island data."));
                    });
                }
            });
        }, delayTicks);
    }

    /**
     * Finish the restoration process after schematic is pasted
     */
    private void finishRestoration(UUID playerUUID, Island island, GridCoordinate coord) {
        try {
            UUID islandUUID = null;
            try {
                islandUUID = UUID.fromString(island.getUniqueId());
            } catch (Exception e) {
                // Island ID not a UUID - that's fine
            }

            addon.getSlotManager().restoreSlot(playerUUID, slotData.getSlotNumber(), islandUUID, coord.toString());
            gridManager.occupyLocation(coord, playerUUID, player.getName(), islandUUID);

            // IMPORTANT: Clear the pending restoration state so future /island commands work correctly
            addon.getSlotManager().clearPendingSlotRestoration(playerUUID);

            // Get teleport location
            Location spawn = island.getSpawnPoint(World.Environment.NORMAL);
            if (spawn == null) spawn = island.getCenter();
            Location finalSpawn = spawn.clone();

            // Force load chunks around the island center
            Location center = island.getCenter();
            if (center != null && center.getWorld() != null) {
                World world = center.getWorld();
                int chunkX = center.getBlockX() >> 4;
                int chunkZ = center.getBlockZ() >> 4;

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        world.getChunkAt(chunkX + dx, chunkZ + dz).load(true);
                    }
                }
            }

            addon.log("Restored slot " + slotData.getSlotNumber() + " for " + playerUUID + " at " + coord);

            // Wait for chunks to load and FAWE to finish pasting, then teleport
            player.sendMessage(colorize("&7Finalizing island... teleporting in 3 seconds."));
            Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
                // Use BentoBox SafeSpotTeleport for safe async teleportation
                new SafeSpotTeleport.Builder(addon.getPlugin())
                    .entity(player)
                    .location(finalSpawn)
                    .thenRun(() -> {
                        player.sendMessage(colorize("&a&lIsland Restored!"));
                        player.sendMessage(colorize("&7Your island has been placed at " + coord.toString()));
                    })
                    .ifFail(() -> {
                        // Fallback - teleport directly to center
                        player.teleport(island.getCenter().add(0.5, 1, 0.5));
                        player.sendMessage(colorize("&a&lIsland Restored!"));
                        player.sendMessage(colorize("&7Your island has been placed at " + coord.toString()));
                    })
                    .buildFuture();
            }, 60L); // 3 seconds delay

        } catch (Exception e) {
            addon.logError("Failed to finish restoration: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(colorize("&cFailed to complete restoration."));
        }
    }

    private void pasteSchematic(File schematicFile, Location center) throws Exception {
        World world = center.getWorld();
        if (world == null) {
            throw new Exception("World is null");
        }

        // Load clipboard from file
        Clipboard clipboard = BuiltInClipboardFormat.SPONGE_SCHEMATIC.load(schematicFile);

        // Paste to world
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            Operation operation = new ClipboardHolder(clipboard)
                .createPaste(editSession)
                .to(BlockVector3.at(center.getX(), center.getY(), center.getZ()))
                .ignoreAirBlocks(false)
                .build();

            Operations.complete(operation);
        }

        addon.log("Pasted restoration schematic at " + center.getBlockX() + ", " + center.getBlockZ());
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
