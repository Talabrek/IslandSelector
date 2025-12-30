package world.bentobox.islandselector.gui;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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

import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.utils.GridCoordinate;
import world.bentobox.islandselector.gui.SlotSelectionGUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Neighborhood View GUI - Shows 3x3 grid of player's island and neighbors
 * 27 slots (3 rows x 9 columns) layout
 */
public class NeighborhoodGUI implements InventoryHolder, Listener {

    private static final int SIZE = 27;
    private static final int BACK_SLOT = 18;
    private static final int SLOTS_SLOT = 20;
    private static final int CLOSE_SLOT = 26;

    // 3x3 neighborhood positions (center of GUI)
    // Layout:  [NW][N][NE]
    //          [W][C][E]
    //          [SW][S][SE]
    private static final int[] NEIGHBOR_POSITIONS = {
        3, 4, 5,    // NW, N, NE (row 1)
        12, 13, 14, // W, Center, E (row 2)
        21, 22, 23  // SW, S, SE (row 3) - but 22 is slots button, so adjust
    };

    // Adjusted positions
    private static final int NW_SLOT = 3;
    private static final int N_SLOT = 4;
    private static final int NE_SLOT = 5;
    private static final int W_SLOT = 12;
    private static final int CENTER_SLOT = 13;
    private static final int E_SLOT = 14;
    private static final int SW_SLOT = 11; // Moved to avoid conflicts
    private static final int S_SLOT = 13;  // This conflicts with center, need adjustment
    private static final int SE_SLOT = 15;

    private final IslandSelector addon;
    private final Player player;
    private final GridManager gridManager;
    private Inventory inventory;
    private GridCoordinate playerIsland;

    public NeighborhoodGUI(IslandSelector addon, Player player) {
        this.addon = addon;
        this.player = player;
        this.gridManager = addon.getGridManager();
    }

    /**
     * Create and open the GUI
     */
    public void open() {
        // Get player's island coordinate
        playerIsland = gridManager.getPlayerIslandCoordinate(player.getUniqueId());

        if (playerIsland == null) {
            player.sendMessage(colorize("&cYou need an island to view your neighborhood!"));
            return;
        }

        createInventory();
        populateInventory();
        player.openInventory(inventory);
        Bukkit.getPluginManager().registerEvents(this, addon.getPlugin());
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(this, SIZE, "Your Neighborhood");
    }

    private void populateInventory() {
        inventory.clear();

        if (playerIsland == null) {
            return;
        }

        // Populate 3x3 neighborhood
        // Row 1: NW, N, NE at slots 3, 4, 5
        populateNeighborSlot(3, playerIsland.offset(-1, -1), "NW");
        populateNeighborSlot(4, playerIsland.offset(0, -1), "N");
        populateNeighborSlot(5, playerIsland.offset(1, -1), "NE");

        // Row 2: W, Center (player), E at slots 12, 13, 14
        populateNeighborSlot(12, playerIsland.offset(-1, 0), "W");
        populateCenterSlot(13); // Player's island
        populateNeighborSlot(14, playerIsland.offset(1, 0), "E");

        // Row 3: SW, S, SE at slots 21, 22, 23
        populateNeighborSlot(21, playerIsland.offset(-1, 1), "SW");
        populateNeighborSlot(22, playerIsland.offset(0, 1), "S");
        populateNeighborSlot(23, playerIsland.offset(1, 1), "SE");

        // Control buttons (bottom row)
        // Row 3: [SW=21][S=22][SE=23][empty][empty][empty][Back=18][empty][empty]
        // Adjust: [empty][empty][empty][empty][empty][Back=18][empty][empty][Slots=22][empty][empty][empty][Close=26]
        // Simpler: Bottom row control buttons
        inventory.setItem(BACK_SLOT, createButton(Material.FILLED_MAP, "&eBack to Grid", "&7Return to main grid view"));
        inventory.setItem(SLOTS_SLOT, createButton(Material.CHEST, "&eSlot Selection", "&7Manage your island slots"));
        inventory.setItem(CLOSE_SLOT, createButton(Material.BARRIER, "&cClose", "&7Close this menu"));

        // Fill empty slots
        fillEmptySlots();
    }

    private void populateCenterSlot(int slot) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();

        // Set the player's head texture
        skullMeta.setOwningPlayer(player);

        skullMeta.setDisplayName(colorize("&a&l" + playerIsland.toString() + " - Your Island"));

        // Add glow
        Enchantment glow = org.bukkit.Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft("unbreaking"));
        if (glow != null) {
            skullMeta.addEnchant(glow, 1, true);
        }
        skullMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&6This is your island!"));
        lore.add(colorize("&7Location: &f" + playerIsland.toString()));

        skullMeta.setLore(lore);
        item.setItemMeta(skullMeta);
        inventory.setItem(slot, item);
    }

    private void populateNeighborSlot(int slot, GridCoordinate coord, String direction) {
        if (coord == null) {
            // Out of bounds
            inventory.setItem(slot, createEmptyNeighborItem(direction, "Edge of map"));
            return;
        }

        if (!gridManager.isWithinBounds(coord)) {
            inventory.setItem(slot, createEmptyNeighborItem(direction, "Beyond grid"));
            return;
        }

        // Get status first - this may register the island from BSkyBlock
        GridLocation.Status status = gridManager.getLocationStatus(coord);
        // Get location after status check (may have been created)
        GridLocation location = gridManager.getGridLocation(coord);

        switch (status) {
            case AVAILABLE:
                inventory.setItem(slot, createAvailableNeighborItem(coord, direction));
                break;
            case OCCUPIED:
                inventory.setItem(slot, createOccupiedNeighborItem(coord, location, direction));
                break;
            case RESERVED:
                inventory.setItem(slot, createReservedNeighborItem(coord, direction));
                break;
            default:
                inventory.setItem(slot, createEmptyNeighborItem(direction, "Locked"));
        }
    }

    private ItemStack createAvailableNeighborItem(GridCoordinate coord, String direction) {
        ItemStack item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&a" + coord.toString() + " - Empty (" + direction + ")"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7No neighbor here"));
        lore.add(colorize("&7This location is available"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOccupiedNeighborItem(GridCoordinate coord, GridLocation location, String direction) {
        boolean online = gridManager.isOwnerOnline(coord);
        ItemStack item;

        if (online) {
            item = new ItemStack(Material.PLAYER_HEAD);
        } else {
            item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        }

        ItemMeta meta = item.getItemMeta();
        String ownerName = location != null && location.getOwnerName() != null ?
            location.getOwnerName() : "Unknown";
        meta.setDisplayName(colorize("&f" + coord.toString() + " - " + ownerName + " (" + direction + ")"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Owner: &f" + ownerName));
        if (online) {
            lore.add(colorize("&a Online"));
        } else {
            lore.add(colorize("&c Offline"));
        }
        lore.add("");
        lore.add(colorize("&7Right-click to visit"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createReservedNeighborItem(GridCoordinate coord, String direction) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&7" + coord.toString() + " - Reserved (" + direction + ")"));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7This location is reserved"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEmptyNeighborItem(String direction, String reason) {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&7" + direction + " - " + reason));

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

    private String colorize(String text) {
        return text.replace("&", "\u00A7");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof NeighborhoodGUI)) {
            return;
        }

        NeighborhoodGUI clickedGUI = (NeighborhoodGUI) event.getInventory().getHolder();
        if (!clickedGUI.equals(this)) {
            return;
        }

        event.setCancelled(true);

        if (event.getRawSlot() < 0 || event.getRawSlot() >= SIZE) {
            return;
        }

        int slot = event.getRawSlot();
        Player clicker = (Player) event.getWhoClicked();

        if (slot == BACK_SLOT) { // Back to grid
            clicker.closeInventory();
            new MainGridGUI(addon, clicker).open();
            return;
        }

        if (slot == SLOTS_SLOT) { // Slot Selection
            clicker.closeInventory();
            new SlotSelectionGUI(addon, clicker).open();
            return;
        }

        if (slot == CLOSE_SLOT) { // Close
            clicker.closeInventory();
            return;
        }

        // Handle neighbor island clicks (for visiting/warping)
        if (isNeighborSlot(slot)) {
            handleNeighborClick(clicker, slot, event.isRightClick());
        }
    }

    private boolean isNeighborSlot(int slot) {
        // Neighbor slots are at positions 3, 4, 5, 12, 14, 21, 22, 23
        // (slot 13 is the center/player's island)
        return (slot >= 3 && slot <= 5) ||
               slot == 12 || slot == 14 ||
               (slot >= 21 && slot <= 23);
    }

    private void handleNeighborClick(Player player, int slot, boolean rightClick) {
        // Only handle right-clicks for warping
        if (!rightClick) {
            return;
        }

        // Get the coordinate for this slot
        GridCoordinate coord = getCoordinateForSlot(slot);
        if (coord == null) {
            return;
        }

        // Check if location is occupied
        GridLocation.Status status = gridManager.getLocationStatus(coord);
        if (status != GridLocation.Status.OCCUPIED) {
            player.sendMessage(colorize("&cNo island at this location!"));
            return;
        }

        // Get the island from BSkyBlock
        Island island = getIslandAtCoordinate(coord);
        if (island == null) {
            player.sendMessage(colorize("&cCannot find island at this location!"));
            return;
        }

        // Check if island has a public warp sign enabled
        // In BentoBox, warps are managed through the Warps addon
        // If island is private (not allowing visitors), we can't warp
        // For now, just try to get the island center/spawn for warping
        Location warpLocation = island.getProtectionCenter();
        if (warpLocation == null) {
            warpLocation = island.getCenter();
        }

        if (warpLocation == null) {
            player.sendMessage(colorize("&cCannot find warp location for this island!"));
            return;
        }

        // Close GUI and use safe teleport
        player.closeInventory();
        player.sendMessage(colorize("&aWarping to neighbor's island..."));

        // Use BentoBox SafeSpotTeleport for safe async teleportation
        new SafeSpotTeleport.Builder(addon.getPlugin())
            .entity(player)
            .island(island)
            .ifFail(() -> player.sendMessage(colorize("&cCould not find a safe spot on this island!")))
            .buildFuture();
    }

    private GridCoordinate getCoordinateForSlot(int slot) {
        if (playerIsland == null) {
            return null;
        }

        // Map slot numbers to offset coordinates
        switch (slot) {
            case 3:  return playerIsland.offset(-1, -1); // NW
            case 4:  return playerIsland.offset(0, -1);  // N
            case 5:  return playerIsland.offset(1, -1);  // NE
            case 12: return playerIsland.offset(-1, 0);  // W
            case 14: return playerIsland.offset(1, 0);   // E
            case 21: return playerIsland.offset(-1, 1);  // SW
            case 22: return playerIsland.offset(0, 1);   // S
            case 23: return playerIsland.offset(1, 1);   // SE
            default: return null;
        }
    }

    private Island getIslandAtCoordinate(GridCoordinate coord) {
        // Get the world coordinates for this grid coordinate
        // Use the addon's getIslandSpacing() method which gets it from BSkyBlock config
        int spacing = addon.getIslandSpacing() * 2; // Actual spacing between centers
        int worldX = coord.getColumn() * spacing;
        int worldZ = coord.getRow() * spacing;

        // Get BSkyBlock world
        World world = getBSkyBlockWorld();
        if (world == null) {
            return null;
        }

        Location location = new Location(world, worldX, 100, worldZ);

        // Get island at this location from BentoBox
        return addon.getPlugin().getIslandsManager().getIslandAt(location).orElse(null);
    }

    private World getBSkyBlockWorld() {
        // Get BSkyBlock game mode addon
        world.bentobox.bentobox.api.addons.GameModeAddon gameModeAddon = null;
        for (world.bentobox.bentobox.api.addons.Addon registeredAddon : addon.getPlugin().getAddonsManager().getAddons()) {
            if (registeredAddon instanceof world.bentobox.bentobox.api.addons.GameModeAddon) {
                world.bentobox.bentobox.api.addons.GameModeAddon gm = (world.bentobox.bentobox.api.addons.GameModeAddon) registeredAddon;
                if (gm.getDescription().getName().equals("BSkyBlock")) {
                    gameModeAddon = gm;
                    break;
                }
            }
        }

        if (gameModeAddon == null) {
            return null;
        }

        return gameModeAddon.getOverWorld();
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof NeighborhoodGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof NeighborhoodGUI &&
            event.getInventory().getHolder().equals(this)) {
            HandlerList.unregisterAll(this);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
