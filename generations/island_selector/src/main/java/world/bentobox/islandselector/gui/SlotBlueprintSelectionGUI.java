package world.bentobox.islandselector.gui;

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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blueprint Selection GUI for creating a new island in an empty slot.
 * The island is created at the SAME location as the player's existing island.
 *
 * This GUI is shown when a player clicks on an empty slot in the slot selection GUI.
 */
public class SlotBlueprintSelectionGUI implements InventoryHolder, Listener {

    private static final int SMALL_SIZE = 27;
    private static final int LARGE_SIZE = 54;

    private final IslandSelector addon;
    private final Player player;
    private final int targetSlotNumber;
    private final SlotData activeSlot;
    private Inventory inventory;

    // Map slot to blueprint bundle ID (ConcurrentHashMap for thread safety)
    private final Map<Integer, String> slotToBundleId = new ConcurrentHashMap<>();

    // Available bundles
    private List<BlueprintBundle> bundles;
    private GameModeAddon bskyblock;
    private BukkitTask cleanupTask;

    public SlotBlueprintSelectionGUI(IslandSelector addon, Player player, int targetSlotNumber, SlotData activeSlot) {
        this.addon = addon;
        this.player = player;
        this.targetSlotNumber = targetSlotNumber;
        this.activeSlot = activeSlot;
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
            addon.getSlotManager().clearPendingSlotCreation(player.getUniqueId());
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
                createIslandInSlot(defaultBundle.getUniqueId());
            } else {
                player.sendMessage(colorize("&cNo island blueprints available!"));
                addon.getSlotManager().clearPendingSlotCreation(player.getUniqueId());
            }
            return;
        }

        addon.log("Showing slot blueprint selection GUI with " + bundles.size() + " bundle(s) for slot " + targetSlotNumber);

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
        inventory = Bukkit.createInventory(this, size, "Select Island Type for Slot " + targetSlotNumber);
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

        // Add blueprints
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
            slotToBundleId.put(slot, bundleKey);

            col++;
            if (col >= 7) {
                col = 0;
                slot += 3; // Move to next row
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
            cancelLore.add(colorize("&7Click to cancel"));
            cancelMeta.setLore(cancelLore);
            cancel.setItemMeta(cancelMeta);
        }
        inventory.setItem(cancelSlot, cancel);
    }

    private ItemStack createBlueprintItem(BlueprintBundle bundle) {
        Material iconMat = bundle.getIcon();
        if (iconMat == null || iconMat == Material.AIR) {
            iconMat = Material.GRASS_BLOCK;
        }

        ItemStack item = new ItemStack(iconMat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String displayName = bundle.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = bundle.getUniqueId();
        }
        meta.setDisplayName(colorize("&a&l" + displayName));

        List<String> lore = new ArrayList<>();
        lore.add("");

        List<String> description = bundle.getDescription();
        if (description != null && !description.isEmpty()) {
            for (String descLine : description) {
                if (descLine != null && !descLine.isEmpty()) {
                    lore.add(colorize("&7" + descLine));
                }
            }
            lore.add("");
        }

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
        lore.add(colorize("&e&lClick to create this island in slot " + targetSlotNumber + "!"));

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
        if (!(holder instanceof SlotBlueprintSelectionGUI) || !holder.equals(this)) {
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

        // Find the bundle for display name
        String bundleName = bundleId;
        for (BlueprintBundle bundle : bundles) {
            if (bundle.getUniqueId().equals(bundleId)) {
                bundleName = bundle.getDisplayName() != null ? bundle.getDisplayName() : bundleId;
                break;
            }
        }

        player.sendMessage(colorize("&aSelected island type: &f" + bundleName));
        player.sendMessage(colorize("&eCreating island in slot " + targetSlotNumber + "..."));

        // Create the island in the slot
        createIslandInSlot(bundleId);
    }

    /**
     * Create a new island in the target slot at the same location as the active island.
     * This saves the current island first, then creates the new one.
     */
    private void createIslandInSlot(String blueprintBundleId) {
        // Store UUID to avoid stale player references in async callbacks
        final UUID playerUUID = player.getUniqueId();

        // Get the player's current island
        Island currentIsland = addon.getIslands().getIsland(
            addon.getGridManager().getBSkyBlockWorld(),
            playerUUID
        );

        if (currentIsland == null) {
            player.sendMessage(colorize("&cError: Could not find your current island!"));
            addon.getSlotManager().clearPendingSlotCreation(playerUUID);
            return;
        }

        // Get the grid coordinate from the active slot (all slots share this location)
        String gridCoordinate = activeSlot.getGridCoordinate();

        // Step 0: Teleport player to SERVER spawn for safety during the operation
        World spawnWorld = Bukkit.getWorld("world");
        if (spawnWorld == null && !Bukkit.getWorlds().isEmpty()) {
            spawnWorld = Bukkit.getWorlds().get(0);
        }
        if (spawnWorld == null) {
            player.sendMessage(colorize("&cCannot create slot - no spawn world available!"));
            return;
        }
        Location serverSpawn = spawnWorld.getSpawnLocation();
        // Use safe teleport for server spawn
        new SafeSpotTeleport.Builder(addon.getPlugin())
            .entity(player)
            .location(serverSpawn)
            .buildFuture();
        player.sendMessage(colorize("&eTeleported to spawn for safety during slot creation..."));

        // Step 1: Save the current island to the active slot's schematic
        player.sendMessage(colorize("&eSaving current island..."));

        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            try {
                // Save current island to active slot schematic
                boolean saved = saveCurrentIslandToSlot(currentIsland, activeSlot);
                if (!saved) {
                    Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                        // Re-fetch player to avoid stale reference
                        Player freshPlayer = Bukkit.getPlayer(playerUUID);
                        if (freshPlayer != null && freshPlayer.isOnline()) {
                            freshPlayer.sendMessage(colorize("&cFailed to save current island!"));
                        }
                        addon.getSlotManager().clearPendingSlotCreation(playerUUID);
                    });
                    return;
                }

                // Step 2: Clear the current island blocks
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    // Re-fetch player to avoid stale reference
                    Player freshPlayer = Bukkit.getPlayer(playerUUID);
                    if (freshPlayer != null && freshPlayer.isOnline()) {
                        freshPlayer.sendMessage(colorize("&eClearing island area..."));
                    }
                });

                clearIslandBlocks(currentIsland);
                Thread.sleep(2000); // Wait for clearing

                // Step 3: Create the new island using BSkyBlock's blueprint pasting
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    // Re-fetch player to avoid stale reference
                    Player freshPlayer = Bukkit.getPlayer(playerUUID);
                    if (freshPlayer != null && freshPlayer.isOnline()) {
                        freshPlayer.sendMessage(colorize("&eCreating new island..."));
                    }
                    pasteNewIslandInSlot(currentIsland, blueprintBundleId, gridCoordinate, playerUUID);
                });

            } catch (Exception e) {
                addon.logError("Error creating island in slot: " + e.getMessage());
                e.printStackTrace();
                Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                    // Re-fetch player to avoid stale reference
                    Player freshPlayer = Bukkit.getPlayer(playerUUID);
                    if (freshPlayer != null && freshPlayer.isOnline()) {
                        freshPlayer.sendMessage(colorize("&cAn error occurred. Please contact an admin."));
                    }
                    addon.getSlotManager().clearPendingSlotCreation(playerUUID);
                });
            }
        });
    }

    /**
     * Save the current island to a schematic file for the given slot
     */
    private boolean saveCurrentIslandToSlot(Island island, SlotData slotData) {
        try {
            Location center = island.getCenter();
            World world = center.getWorld();
            if (world == null) {
                return false;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            // Get schematic file path
            java.io.File schematicFile = new java.io.File(
                addon.getSlotManager().getSlotSchematicPath(slotData.getPlayerUUIDAsUUID(), slotData.getSlotNumber())
            );

            final int finalRange = range;
            final Location finalCenter = center;
            final World finalWorld = world;

            // Step 1: Save entities SEPARATELY (must be on main thread)
            try {
                Bukkit.getScheduler().callSyncMethod(addon.getPlugin(), () -> {
                    int saved = addon.getEntityStorage().saveEntities(finalWorld, finalCenter, finalRange, schematicFile);
                    addon.log("Saved " + saved + " entities for slot: " + slotData.getUniqueId());
                    return saved;
                }).get(); // Wait for completion
            } catch (Exception e) {
                addon.logWarning("Failed to save entities: " + e.getMessage());
            }

            // Step 2: Save island homes for this slot
            addon.getSlotManager().saveIslandHomes(slotData.getPlayerUUIDAsUUID(), slotData.getSlotNumber(), island);

            // Step 3: Use SchematicUtils for block saving (without entities - we handle those separately)
            boolean success = addon.getSchematicUtils().copyAndSave(center, range, false, schematicFile);

            if (success) {
                addon.log("Saved island schematic for slot: " + slotData.getUniqueId());
            }
            return success;

        } catch (Exception e) {
            addon.logError("Failed to save schematic: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Clear island blocks AND entities.
     * Entities are removed on main thread FIRST, then blocks are cleared.
     */
    private void clearIslandBlocks(Island island) {
        try {
            Location center = island.getCenter();
            World world = center.getWorld();
            if (world == null) {
                return;
            }

            int islandSpacing = addon.getIslandSpacing();
            int protectionRange = island.getProtectionRange();
            int range = Math.max(islandSpacing / 2, protectionRange);

            final int finalRange = range;
            final Location finalCenter = center;
            final World finalWorld = world;

            // Step 1: Remove entities FIRST on main thread
            try {
                Bukkit.getScheduler().callSyncMethod(addon.getPlugin(), () -> {
                    int removed = addon.getEntityStorage().removeEntities(finalWorld, finalCenter, finalRange);
                    addon.log("Removed " + removed + " entities during slot creation clearing");
                    return removed;
                }).get(); // Wait for completion
            } catch (Exception e) {
                addon.logWarning("Failed to remove entities: " + e.getMessage());
            }

            // Step 2: Clear blocks using FAWE
            com.sk89q.worldedit.world.World weWorld = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world);
            com.sk89q.worldedit.math.BlockVector3 min = com.sk89q.worldedit.math.BlockVector3.at(
                center.getX() - range,
                world.getMinHeight(),
                center.getZ() - range
            );
            com.sk89q.worldedit.math.BlockVector3 max = com.sk89q.worldedit.math.BlockVector3.at(
                center.getX() + range,
                world.getMaxHeight() - 1,
                center.getZ() + range
            );

            com.sk89q.worldedit.regions.CuboidRegion region = new com.sk89q.worldedit.regions.CuboidRegion(weWorld, min, max);

            try (com.sk89q.worldedit.EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(weWorld)) {
                for (com.sk89q.worldedit.math.BlockVector3 pos : region) {
                    editSession.setBlock(pos, com.sk89q.worldedit.world.block.BlockTypes.AIR.getDefaultState());
                }
                editSession.flushQueue();
            }

            addon.log("Cleared island blocks for slot creation");

        } catch (Exception e) {
            addon.logError("Failed to clear island blocks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Paste a new island from blueprint at the same location
     * @param island The island to paste the blueprint at
     * @param blueprintBundleId The blueprint bundle ID to use
     * @param gridCoordinate The grid coordinate for the slot
     * @param playerUUID The player's UUID (to avoid stale player references in callbacks)
     */
    private void pasteNewIslandInSlot(Island island, String blueprintBundleId, String gridCoordinate, UUID playerUUID) {
        try {
            // Get the blueprint manager
            BlueprintsManager blueprintsManager = addon.getPlugin().getBlueprintsManager();
            Location center = island.getCenter();

            // Paste the blueprint at the island center
            // Parameters: addon, island, bundleId, task (runnable), useDefaultChest (boolean)
            blueprintsManager.paste(bskyblock, island, blueprintBundleId, () -> {
                // Completion callback - runs when paste is complete
                addon.log("Blueprint pasted for slot " + targetSlotNumber);

                // Deactivate the old active slot
                SlotData oldActiveSlot = addon.getSlotManager().getActiveSlot(playerUUID);
                if (oldActiveSlot != null) {
                    oldActiveSlot.setActive(false);
                    addon.getSlotManager().saveSlot(oldActiveSlot);
                    addon.log("Deactivated old slot " + oldActiveSlot.getSlotNumber());
                }

                // Register and activate this new slot
                SlotData newSlot = addon.getSlotManager().createOrGetSlot(playerUUID, targetSlotNumber);

                // All slots share the SAME BentoBox island - use the island UUID from the active slot
                // This is crucial because BentoBox only has ONE island per player, and we switch
                // between different slot schematics at that same island location
                String sharedIslandUUID = activeSlot.getIslandUUID();
                addon.log("Using shared island UUID: " + sharedIslandUUID + " for slot " + targetSlotNumber);

                // Store the island info - pass null for UUID and use the string directly
                newSlot.setIslandUUID(sharedIslandUUID);
                newSlot.setGridCoordinate(gridCoordinate);
                newSlot.setHasIsland(true);
                newSlot.setActive(true); // Set the new slot as active

                // IMPORTANT: Clear serialized homes for new slot - it should start fresh
                // Don't inherit homes from the previous slot
                newSlot.setSerializedHomes(new java.util.HashMap<>());

                addon.getSlotManager().saveSlot(newSlot);
                addon.log("Created and activated slot " + targetSlotNumber);

                // Clear island homes from the BentoBox island object
                // New slots should start fresh without inherited homes
                java.util.Map<String, Location> existingHomes = island.getHomes();
                if (existingHomes != null && !existingHomes.isEmpty()) {
                    java.util.List<String> homeNames = new java.util.ArrayList<>(existingHomes.keySet());
                    for (String homeName : homeNames) {
                        island.removeHome(homeName);
                    }
                    addon.log("Cleared " + homeNames.size() + " inherited homes for new slot " + targetSlotNumber);
                }

                // Add only the default home at the island center/spawn
                Location spawnPoint = island.getSpawnPoint(org.bukkit.World.Environment.NORMAL);
                if (spawnPoint != null) {
                    island.addHome("home", spawnPoint);
                } else {
                    island.addHome("home", center);
                }

                // Clear the pending creation flag
                addon.getSlotManager().clearPendingSlotCreation(playerUUID);

                // Refresh chunks around the island to fix invisible blocks
                Location islandCenter = island.getCenter();
                if (islandCenter != null && islandCenter.getWorld() != null) {
                    World islandWorld = islandCenter.getWorld();
                    int chunkX = islandCenter.getBlockX() >> 4;
                    int chunkZ = islandCenter.getBlockZ() >> 4;

                    // Refresh a 3x3 chunk area around the island center
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            islandWorld.refreshChunk(chunkX + dx, chunkZ + dz);
                        }
                    }
                }

                // Re-fetch player to avoid stale reference in callback
                Player freshPlayer = Bukkit.getPlayer(playerUUID);
                if (freshPlayer != null && freshPlayer.isOnline()) {
                    // Notify player
                    freshPlayer.sendMessage(colorize("&a&lSlot " + targetSlotNumber + " created successfully!"));
                    freshPlayer.sendMessage(colorize("&7Your new island is ready at the same location."));
                    freshPlayer.sendMessage(colorize("&7Use &e/islandselector slots&7 to switch between islands."));

                    // Teleport player to the island using safe teleport
                    new SafeSpotTeleport.Builder(addon.getPlugin())
                        .entity(freshPlayer)
                        .island(island)
                        .ifFail(() -> {
                            Player p = Bukkit.getPlayer(playerUUID);
                            if (p != null && p.isOnline()) {
                                p.sendMessage(colorize("&eCouldn't find safe spot - you may need to use /island go"));
                            }
                        })
                        .buildFuture();
                }
            }, true); // true = use default chest contents

        } catch (Exception e) {
            addon.logError("Failed to paste blueprint for slot: " + e.getMessage());
            e.printStackTrace();
            // Re-fetch player to avoid stale reference
            Player freshPlayer = Bukkit.getPlayer(playerUUID);
            if (freshPlayer != null && freshPlayer.isOnline()) {
                freshPlayer.sendMessage(colorize("&cFailed to create island. Please contact an admin."));
            }
            addon.getSlotManager().clearPendingSlotCreation(playerUUID);
        }
    }

    private void handleCancel() {
        player.closeInventory();
        addon.getSlotManager().clearPendingSlotCreation(player.getUniqueId());
        player.sendMessage(colorize("&cSlot creation cancelled."));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SlotBlueprintSelectionGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof SlotBlueprintSelectionGUI && holder.equals(this)) {
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
