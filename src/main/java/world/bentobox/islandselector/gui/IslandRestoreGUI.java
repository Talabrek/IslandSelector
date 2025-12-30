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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI for homeless players to select a location to restore their saved island.
 *
 * Similar to IslandClaimGUI but:
 * - Shows a message explaining they're restoring their island
 * - After selecting a location, pastes their saved schematic instead of using a blueprint
 */
public class IslandRestoreGUI implements InventoryHolder, Listener {

    // GUI Layout Constants (same as IslandClaimGUI)
    private static final int ROWS = 6;
    private static final int COLS = 9;
    private static final int SIZE = ROWS * COLS;
    private static final int GRID_COLS = 7;
    private static final int GRID_ROWS = 4;

    private static final int[] ARROW_UP_SLOTS = {0, 1, 7, 8};
    private static final int[] ARROW_DOWN_SLOTS = {45, 46, 52, 53};
    private static final int[] ARROW_LEFT_SLOTS = {9, 18, 27, 36};
    private static final int[] ARROW_RIGHT_SLOTS = {17, 26, 35, 44};

    private static final int TITLE_SLOT = 4;
    private static final int CANCEL_SLOT = 49;

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
        inventory = Bukkit.createInventory(this, SIZE, "§6§lRestore Island - Select Location");
        populateInventory();
        Bukkit.getPluginManager().registerEvents(this, addon.getPlugin());
        player.openInventory(inventory);
    }

    private void populateInventory() {
        // Fill with background
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Title
        inventory.setItem(TITLE_SLOT, createItem(Material.COMPASS, "§e§lSelect New Location",
            List.of(
                "§7Choose where to place your",
                "§7restored island.",
                "",
                "§fRestoring: §e" + slotData.getSlotName(),
                "",
                "§aGreen = Available"
            )));

        // Navigation arrows
        populateArrows();

        // Grid
        populateGrid();

        // Cancel button
        inventory.setItem(CANCEL_SLOT, createItem(Material.BARRIER, "§c§lCancel",
            List.of("§7Close this menu")));
    }

    private void populateArrows() {
        boolean canScrollUp = viewportZ > settings.getGridMinZ();
        boolean canScrollDown = viewportZ + GRID_ROWS <= settings.getGridMaxZ();
        boolean canScrollLeft = viewportX > settings.getGridMinX();
        boolean canScrollRight = viewportX + GRID_COLS <= settings.getGridMaxX();

        ItemStack upArrow = canScrollUp ?
            createItem(Material.ARROW, "§a▲ Scroll Up", List.of("§7Click to scroll up")) :
            createItem(Material.BARRIER, "§7▲ Top Edge", null);

        ItemStack downArrow = canScrollDown ?
            createItem(Material.ARROW, "§a▼ Scroll Down", List.of("§7Click to scroll down")) :
            createItem(Material.BARRIER, "§7▼ Bottom Edge", null);

        ItemStack leftArrow = canScrollLeft ?
            createItem(Material.ARROW, "§a◄ Scroll Left", List.of("§7Click to scroll left")) :
            createItem(Material.BARRIER, "§7◄ Left Edge", null);

        ItemStack rightArrow = canScrollRight ?
            createItem(Material.ARROW, "§a► Scroll Right", List.of("§7Click to scroll right")) :
            createItem(Material.BARRIER, "§7► Right Edge", null);

        for (int slot : ARROW_UP_SLOTS) inventory.setItem(slot, upArrow);
        for (int slot : ARROW_DOWN_SLOTS) inventory.setItem(slot, downArrow);
        for (int slot : ARROW_LEFT_SLOTS) inventory.setItem(slot, leftArrow);
        for (int slot : ARROW_RIGHT_SLOTS) inventory.setItem(slot, rightArrow);
    }

    private void populateGrid() {
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int gridX = viewportX + col;
                int gridZ = viewportZ + row;
                GridCoordinate coord = new GridCoordinate(gridX, gridZ);

                int invSlot = (row + 1) * 9 + col + 1;
                ItemStack item = createGridItem(coord);
                inventory.setItem(invSlot, item);
            }
        }
    }

    private ItemStack createGridItem(GridCoordinate coord) {
        if (!gridManager.isWithinBounds(coord)) {
            return createItem(Material.BLACK_STAINED_GLASS_PANE, "§8Out of bounds", null);
        }

        GridLocation.Status status = gridManager.getLocationStatus(coord);
        GridLocation location = gridManager.getGridLocation(coord);

        Material material;
        String name;
        List<String> lore = new ArrayList<>();

        switch (status) {
            case AVAILABLE:
                material = Material.GREEN_STAINED_GLASS_PANE;
                name = "§a§lAvailable";
                lore.add("§7Coordinate: §f" + coord.toString());
                lore.add("");
                lore.add("§e▶ Click to select this location");
                break;

            case OCCUPIED:
                material = Material.RED_STAINED_GLASS_PANE;
                name = "§c§lOccupied";
                lore.add("§7Coordinate: §f" + coord.toString());
                if (location != null && location.getOwnerName() != null) {
                    lore.add("§7Owner: §f" + location.getOwnerName());
                }
                break;

            case RESERVED:
                material = Material.YELLOW_STAINED_GLASS_PANE;
                name = "§6§lReserved";
                lore.add("§7Coordinate: §f" + coord.toString());
                break;

            case LOCKED:
            default:
                material = Material.GRAY_STAINED_GLASS_PANE;
                name = "§8§lLocked";
                lore.add("§7Coordinate: §f" + coord.toString());
                break;
        }

        return createItem(material, name, lore);
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
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player clicker = (Player) event.getWhoClicked();
        if (!clicker.equals(player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;

        // Handle navigation
        if (containsSlot(ARROW_UP_SLOTS, slot)) {
            scroll(0, -1);
            return;
        }
        if (containsSlot(ARROW_DOWN_SLOTS, slot)) {
            scroll(0, 1);
            return;
        }
        if (containsSlot(ARROW_LEFT_SLOTS, slot)) {
            scroll(-1, 0);
            return;
        }
        if (containsSlot(ARROW_RIGHT_SLOTS, slot)) {
            scroll(1, 0);
            return;
        }

        // Handle cancel
        if (slot == CANCEL_SLOT) {
            player.closeInventory();
            player.sendMessage("§7Island restoration cancelled.");
            addon.getSlotManager().clearPendingSlotRestoration(player.getUniqueId());
            return;
        }

        // Handle grid click
        GridCoordinate coord = getCoordFromSlot(slot);
        if (coord != null) {
            handleGridClick(coord);
        }
    }

    private void scroll(int dx, int dz) {
        int newX = viewportX + dx;
        int newZ = viewportZ + dz;

        if (newX >= settings.getGridMinX() && newX + GRID_COLS - 1 <= settings.getGridMaxX()) {
            viewportX = newX;
        }
        if (newZ >= settings.getGridMinZ() && newZ + GRID_ROWS - 1 <= settings.getGridMaxZ()) {
            viewportZ = newZ;
        }

        populateInventory();
    }

    private GridCoordinate getCoordFromSlot(int slot) {
        int row = slot / 9 - 1;
        int col = slot % 9 - 1;

        if (row < 0 || row >= GRID_ROWS || col < 0 || col >= GRID_COLS) {
            return null;
        }

        return new GridCoordinate(viewportX + col, viewportZ + row);
    }

    private boolean containsSlot(int[] slots, int slot) {
        for (int s : slots) {
            if (s == slot) return true;
        }
        return false;
    }

    private void handleGridClick(GridCoordinate coord) {
        if (!gridManager.isWithinBounds(coord)) {
            return;
        }

        GridLocation.Status status = gridManager.getLocationStatus(coord);
        if (status != GridLocation.Status.AVAILABLE) {
            player.sendMessage("§cThis location is not available.");
            return;
        }

        // Location is available - confirm and restore
        confirmRestoration(coord);
    }

    private void confirmRestoration(GridCoordinate coord) {
        player.closeInventory();
        player.sendMessage("§a§lRestoring your island to " + coord.toString() + "...");
        player.sendMessage("§7Please wait while your island is being placed.");

        // Perform restoration async
        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            try {
                restoreIslandAtLocation(coord);
            } catch (Exception e) {
                addon.logError("Failed to restore island: " + e.getMessage());
                e.printStackTrace();
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    player.sendMessage("§cFailed to restore your island. Please contact an administrator.");
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

                // Build island with a minimal blueprint (we'll paste our own data)
                // NewIsland.builder().build() is ASYNC - we need to wait for completion
                NewIsland.builder()
                    .player(user)
                    .addon(addon.getBSkyBlockAddon())
                    .reason(IslandEvent.Reason.CREATE)
                    .locationStrategy(locationStrategy)
                    .noPaste() // Don't paste a blueprint - we'll paste the saved schematic
                    .build();

                // Poll for island creation with increasing delays
                // BentoBox's NewIsland.build() is async, so we need to wait for it
                pollForIslandCreation(playerUUID, bskyblockWorld, coord, schematicFile, 0);

            } catch (Exception e) {
                addon.logError("Failed to create island for restoration: " + e.getMessage());
                e.printStackTrace();
                player.sendMessage("§cFailed to create island. Please try again.");
            }
        });
    }

    /**
     * Poll for island creation with increasing delays.
     * BentoBox's NewIsland.build() is async and may take several ticks to complete.
     */
    private void pollForIslandCreation(UUID playerUUID, World bskyblockWorld, GridCoordinate coord,
                                        File schematicFile, int attempt) {
        // Maximum attempts: 10 (total wait: ~10 seconds)
        int maxAttempts = 10;
        int delayTicks = 20; // 1 second between attempts

        if (attempt >= maxAttempts) {
            addon.logError("Island creation timed out after " + maxAttempts + " attempts for player " + playerUUID);
            player.sendMessage("§cIsland creation timed out. Please try again or contact an administrator.");
            return;
        }

        Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
            Island island = addon.getIslands().getIsland(bskyblockWorld, playerUUID);

            if (island == null) {
                addon.log("Island not yet created, attempt " + (attempt + 1) + "/" + maxAttempts);
                // Retry
                pollForIslandCreation(playerUUID, bskyblockWorld, coord, schematicFile, attempt + 1);
                return;
            }

            addon.log("Island created successfully for player " + playerUUID + " at attempt " + (attempt + 1));

            // Island exists! Now paste the schematic
            Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
                try {
                    pasteSchematic(schematicFile, island.getCenter());

                    // Update slot data on main thread
                    Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                        finishRestoration(playerUUID, island, coord);
                    });

                } catch (Exception e) {
                    addon.logError("Failed to paste schematic: " + e.getMessage());
                    e.printStackTrace();
                    Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                        player.sendMessage("§cFailed to paste your island data.");
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

            // Get teleport location
            Location spawn = island.getSpawnPoint(World.Environment.NORMAL);
            if (spawn == null) spawn = island.getCenter();
            Location finalSpawn = spawn.clone();

            // Force load chunks around the island center to ensure blocks are rendered
            Location center = island.getCenter();
            if (center != null && center.getWorld() != null) {
                World world = center.getWorld();
                int chunkX = center.getBlockX() >> 4;
                int chunkZ = center.getBlockZ() >> 4;

                // Load chunks in a 3x3 area
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        world.getChunkAt(chunkX + dx, chunkZ + dz).load(true);
                    }
                }
            }

            // Wait longer (3 seconds) for chunks to fully load and FAWE to finish
            player.sendMessage("§7Preparing island... please wait.");
            Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> {
                // Use BentoBox SafeSpotTeleport for safe async teleportation
                new SafeSpotTeleport.Builder(addon.getPlugin())
                    .entity(player)
                    .location(finalSpawn)
                    .thenRun(() -> {
                        player.sendMessage("§a§lIsland Restored!");
                        player.sendMessage("§7Your island has been placed at " + coord.toString());
                    })
                    .ifFail(() -> {
                        player.sendMessage("§eIsland restored but couldn't find safe spot - teleporting to spawn.");
                        player.sendMessage("§7Your island has been placed at " + coord.toString());
                    })
                    .buildFuture();
            }, 60L); // 3 seconds

        } catch (Exception e) {
            addon.logError("Failed to finish restoration: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cFailed to complete restoration.");
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
