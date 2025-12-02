package world.bentobox.islandselector.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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

import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.models.GridLocation;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.ArrayList;
import java.util.List;

/**
 * Neighborhood View GUI - Shows 3x3 grid of player's island and neighbors
 * 27 slots (3 rows x 9 columns) layout
 */
public class NeighborhoodGUI implements InventoryHolder, Listener {

    private static final int SIZE = 27;
    private static final int BACK_SLOT = 20;
    private static final int SLOTS_SLOT = 22;
    private static final int CLOSE_SLOT = 24;

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

        // Control buttons (adjusted positions)
        inventory.setItem(0, createButton(Material.FILLED_MAP, "&eBack to Grid", "&7Return to grid view"));
        inventory.setItem(8, createButton(Material.BARRIER, "&cClose", "&7Close this menu"));

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

        if (slot == 0) { // Back to grid
            clicker.closeInventory();
            new MainGridGUI(addon, clicker).open();
            return;
        }

        if (slot == 8) { // Close
            clicker.closeInventory();
            return;
        }
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
