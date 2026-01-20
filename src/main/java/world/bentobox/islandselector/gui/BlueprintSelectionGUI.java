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

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.listeners.IslandCreateListener;
import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blueprint Selection GUI - Shows available island schematics/blueprints.
 * Displayed after the player confirms their island location.
 *
 * Layout (27 slots, 3 rows or 54 slots for more blueprints):
 * Shows all available BlueprintBundles from BSkyBlock.
 */
public class BlueprintSelectionGUI implements InventoryHolder, Listener {

    private static final int SMALL_SIZE = 27;
    private static final int LARGE_SIZE = 54;

    private final IslandSelector addon;
    private final Player player;
    private final GridCoordinate coord;
    private final IslandCreateListener createListener;
    private Inventory inventory;

    // Map slot to blueprint bundle ID (ConcurrentHashMap for thread safety)
    private final Map<Integer, String> slotToBundleId = new ConcurrentHashMap<>();

    // Available bundles
    private List<BlueprintBundle> bundles;
    private GameModeAddon bskyblock;
    private BukkitTask cleanupTask;

    public BlueprintSelectionGUI(IslandSelector addon, Player player, GridCoordinate coord,
                                 IslandCreateListener createListener) {
        this.addon = addon;
        this.player = player;
        this.coord = coord;
        this.createListener = createListener;
    }

    public void open() {
        // Get BSkyBlock addon
        bskyblock = addon.getPlugin().getAddonsManager()
            .getGameModeAddons().stream()
            .filter(gm -> gm.getDescription().getName().equalsIgnoreCase("BSkyBlock"))
            .findFirst()
            .orElse(null);

        if (bskyblock == null) {
            player.sendMessage(colorize("&cError: BSkyBlock not found!"));
            createListener.cancelClaim(player);
            return;
        }

        // Get available blueprint bundles
        BlueprintsManager blueprintsManager = addon.getPlugin().getBlueprintsManager();
        bundles = new ArrayList<>(blueprintsManager.getBlueprintBundles(bskyblock).values());

        // If no bundles, use default without showing GUI
        if (bundles.isEmpty()) {
            addon.logWarning("No blueprint bundles found, using default");
            BlueprintBundle defaultBundle = blueprintsManager.getDefaultBlueprintBundle(bskyblock);
            if (defaultBundle != null) {
                createListener.confirmClaimWithBlueprint(player, coord, defaultBundle.getUniqueId());
            } else {
                createListener.confirmClaim(player, coord);
            }
            return;
        }

        // Always show the selection GUI, even if only one bundle
        // This provides a consistent user experience
        addon.log("Showing blueprint selection GUI with " + bundles.size() + " bundle(s)");

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
        slotToBundleId.clear();
        inventory = null;
    }

    private void createInventory() {
        int size = bundles.size() > 7 ? LARGE_SIZE : SMALL_SIZE;
        inventory = Bukkit.createInventory(this, size, "Select Island Type");
    }

    private void populateInventory() {
        // Fill with glass panes
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        // Add blueprints - need to use the map key, not bundle.getUniqueId()
        // because paste() expects the map key
        BlueprintsManager blueprintsManager = addon.getPlugin().getBlueprintsManager();
        Map<String, BlueprintBundle> bundleMap = blueprintsManager.getBlueprintBundles(bskyblock);

        int slot = 10; // Start at row 2
        int col = 0;
        for (Map.Entry<String, BlueprintBundle> entry : bundleMap.entrySet()) {
            if (slot >= inventory.getSize()) break;

            String bundleKey = entry.getKey();
            BlueprintBundle bundle = entry.getValue();

            ItemStack item = createBlueprintItem(bundle);
            inventory.setItem(slot, item);
            // Store the MAP KEY, not the unique ID - paste() expects the map key
            slotToBundleId.put(slot, bundleKey);
            addon.log("Mapped slot " + slot + " to bundle key: " + bundleKey);

            col++;
            if (col >= 7) {
                col = 0;
                slot += 3; // Move to next row (skip edge slots)
            } else {
                slot++;
            }
        }

        // Cancel button at bottom center
        int cancelSlot = inventory.getSize() == LARGE_SIZE ? 49 : 22;
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(colorize("&c&lCancel"));
            List<String> cancelLore = new ArrayList<>();
            cancelLore.add(colorize("&7Click to cancel island creation"));
            cancelMeta.setLore(cancelLore);
            cancel.setItemMeta(cancelMeta);
        }
        inventory.setItem(cancelSlot, cancel);
    }

    private ItemStack createBlueprintItem(BlueprintBundle bundle) {
        // Try to get the icon from the bundle, default to grass block
        Material iconMat = bundle.getIcon();
        if (iconMat == null || iconMat == Material.AIR) {
            iconMat = Material.GRASS_BLOCK;
        }

        ItemStack item = new ItemStack(iconMat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        // Display name
        String displayName = bundle.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = bundle.getUniqueId();
        }
        meta.setDisplayName(colorize("&a&l" + displayName));

        List<String> lore = new ArrayList<>();
        lore.add("");

        // Description - getDescription() returns List<String>
        List<String> description = bundle.getDescription();
        if (description != null && !description.isEmpty()) {
            for (String descLine : description) {
                if (descLine != null && !descLine.isEmpty()) {
                    lore.add(colorize("&7" + descLine));
                }
            }
            lore.add("");
        }

        // Show which blueprints are included
        lore.add(colorize("&7Includes:"));
        if (bundle.getBlueprint(org.bukkit.World.Environment.NORMAL) != null) {
            lore.add(colorize("  &a✓ &fOverworld island"));
        }
        if (bundle.getBlueprint(org.bukkit.World.Environment.NETHER) != null) {
            lore.add(colorize("  &a✓ &fNether island"));
        }
        if (bundle.getBlueprint(org.bukkit.World.Environment.THE_END) != null) {
            lore.add(colorize("  &a✓ &fEnd island"));
        }

        lore.add("");
        lore.add(colorize("&e&lClick to select this island type!"));

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
        if (!(holder instanceof BlueprintSelectionGUI) || !holder.equals(this)) {
            return;
        }

        event.setCancelled(true);

        if (event.getRawSlot() < 0 || event.getRawSlot() >= inventory.getSize()) {
            return;
        }

        int slot = event.getRawSlot();

        // Check if it's a blueprint slot
        if (slotToBundleId.containsKey(slot)) {
            String bundleId = slotToBundleId.get(slot);
            handleBlueprintSelection(bundleId);
            return;
        }

        // Check cancel button
        int cancelSlot = inventory.getSize() == LARGE_SIZE ? 49 : 22;
        if (slot == cancelSlot) {
            handleCancel();
        }
    }

    private void handleBlueprintSelection(String bundleId) {
        player.closeInventory();

        // Verify location is still available
        if (!addon.getGridManager().isAvailable(coord)) {
            player.sendMessage(colorize("&cThis location is no longer available!"));
            createListener.cancelClaim(player);
            return;
        }

        // Find the bundle for display name
        String bundleName = bundleId;
        for (BlueprintBundle bundle : bundles) {
            if (bundle.getUniqueId().equals(bundleId)) {
                bundleName = bundle.getDisplayName() != null ? bundle.getDisplayName() : bundleId;
                break;
            }
        }

        player.sendMessage(colorize("&aSelected island type: &f" + bundleName));

        // Proceed with claiming using the selected blueprint
        createListener.confirmClaimWithBlueprint(player, coord, bundleId);
    }

    private void handleCancel() {
        player.closeInventory();
        createListener.cancelClaim(player);
        player.sendMessage(colorize("&cIsland creation cancelled."));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BlueprintSelectionGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BlueprintSelectionGUI && holder.equals(this)) {
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
