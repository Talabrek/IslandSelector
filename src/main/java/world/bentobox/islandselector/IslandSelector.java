package world.bentobox.islandselector;

import org.bukkit.Bukkit;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.islandselector.commands.IslandSelectorCommand;
import world.bentobox.islandselector.listeners.IslandCreateListener;
import world.bentobox.islandselector.listeners.SearchListener;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.managers.SlotManager;
import world.bentobox.islandselector.managers.SlotSwitchManager;

import java.util.Optional;

/**
 * IslandSelector - BentoBox Addon
 * Grid-based island selection and multi-slot system for BSkyBlock
 *
 * @author Claude Code
 */
public class IslandSelector extends Addon {

    private static IslandSelector instance;
    private Settings settings;
    private GridManager gridManager;
    private SlotManager slotManager;
    private SlotSwitchManager slotSwitchManager;
    private IslandCreateListener islandCreateListener;
    private SearchListener searchListener;
    private int islandSpacing = -1; // Cached from BSkyBlock

    @Override
    public void onLoad() {
        // Save instance for static access
        instance = this;

        // Load configuration
        saveDefaultConfig();
        settings = new Config<>(this, Settings.class).loadConfigObject();
        if (settings == null) {
            // Settings failed to load, use defaults
            settings = new Settings();
            logError("Failed to load config.yml - using defaults");
        }

        log("IslandSelector loading...");
    }

    @Override
    public void onEnable() {
        // Check for required dependencies
        if (!checkDependencies()) {
            logError("Missing required dependencies! Disabling addon.");
            setState(State.DISABLED);
            return;
        }

        // Detect island spacing from BSkyBlock
        detectIslandSpacing();

        // Initialize managers
        gridManager = new GridManager(this);
        slotManager = new SlotManager(this);
        slotSwitchManager = new SlotSwitchManager(this);

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();

        log("IslandSelector enabled successfully!");
        log("Version: " + getDescription().getVersion());
        log("Grid Size: " + settings.getGridWidth() + "x" + settings.getGridHeight());
        log("Island Spacing: " + islandSpacing + " blocks (from BSkyBlock)");
        log("Max Slots: " + settings.getMaxSlots());
    }

    /**
     * Detect island spacing from BSkyBlock's configuration
     */
    private void detectIslandSpacing() {
        Optional<GameModeAddon> bskyblock = getPlugin().getAddonsManager()
            .getGameModeAddons().stream()
            .filter(gm -> gm.getDescription().getName().equalsIgnoreCase("BSkyBlock"))
            .findFirst();

        if (bskyblock.isPresent()) {
            WorldSettings worldSettings = bskyblock.get().getWorldSettings();
            if (worldSettings != null) {
                islandSpacing = worldSettings.getIslandDistance();
                log("Detected island spacing from BSkyBlock: " + islandSpacing + " blocks");
            } else {
                islandSpacing = 400; // Fallback default
                logWarning("Could not get BSkyBlock world settings, using default spacing: " + islandSpacing);
            }
        } else {
            islandSpacing = 400; // Fallback default
            logWarning("BSkyBlock not found, using default spacing: " + islandSpacing);
        }
    }

    /**
     * Register addon commands
     */
    private void registerCommands() {
        // Register main command as standalone
        new IslandSelectorCommand(this);

        // Also try to hook into BSkyBlock's command
        Optional<GameModeAddon> bskyblock = getPlugin().getAddonsManager()
            .getGameModeAddons().stream()
            .filter(gm -> gm.getDescription().getName().equalsIgnoreCase("BSkyBlock"))
            .findFirst();

        bskyblock.ifPresent(gm -> {
            log("Hooking into BSkyBlock commands...");
            // The IslandSelectorCommand is standalone, accessible via /islandselector
        });
    }

    /**
     * Register addon listeners
     */
    private void registerListeners() {
        // Register island creation interceptor
        islandCreateListener = new IslandCreateListener(this);
        registerListener(islandCreateListener);
        log("Registered island creation interceptor");

        // Register search listener
        searchListener = new SearchListener(this);
        Bukkit.getPluginManager().registerEvents(searchListener, getPlugin());
    }

    @Override
    public void onDisable() {
        // Save all grid data before shutting down
        if (gridManager != null) {
            log("Saving grid data...");
            gridManager.saveAll();
        }
        log("IslandSelector disabled.");
    }

    @Override
    public void onReload() {
        // Reload configuration
        settings = new Config<>(this, Settings.class).loadConfigObject();
        if (settings == null) {
            settings = new Settings();
            logError("Failed to reload config.yml");
        }
        log("Configuration reloaded.");
    }

    /**
     * Check if all required dependencies are present
     */
    private boolean checkDependencies() {
        // Check for BSkyBlock addon (it's a BentoBox addon, not a Bukkit plugin)
        boolean hasBSkyBlock = getPlugin().getAddonsManager()
            .getGameModeAddons().stream()
            .anyMatch(gm -> gm.getDescription().getName().equalsIgnoreCase("BSkyBlock"));

        if (!hasBSkyBlock) {
            logError("BSkyBlock addon is not installed or enabled!");
            return false;
        }

        // Check for FastAsyncWorldEdit (this IS a Bukkit plugin)
        if (!Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit")) {
            logError("FastAsyncWorldEdit (FAWE) is not installed or enabled!");
            logError("IslandSelector requires FAWE for schematic operations.");
            return false;
        }

        log("All required dependencies found.");

        // Check optional Bukkit plugin dependencies
        checkOptionalDependency("Vault", "Economy integration disabled");
        checkOptionalDependency("PlaceholderAPI", "Placeholder integration disabled");

        // Check optional BentoBox addon dependencies
        boolean hasLevel = getPlugin().getAddonsManager()
            .getAddonByName("Level").isPresent();
        if (hasLevel) {
            log("Level addon found - integration enabled");
        } else {
            log("Level addon not found - Island level display disabled");
        }

        return true;
    }

    /**
     * Check for optional dependency and log if not found
     */
    private void checkOptionalDependency(String pluginName, String disabledMessage) {
        if (Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
            log(pluginName + " found - integration enabled");
        } else {
            log(pluginName + " not found - " + disabledMessage);
        }
    }

    /**
     * Get the addon instance
     */
    public static IslandSelector getInstance() {
        return instance;
    }

    /**
     * Get the settings
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * Get the grid manager
     */
    public GridManager getGridManager() {
        return gridManager;
    }

    /**
     * Get the slot manager
     */
    public SlotManager getSlotManager() {
        return slotManager;
    }

    /**
     * Get the slot switch manager
     */
    public SlotSwitchManager getSlotSwitchManager() {
        return slotSwitchManager;
    }

    /**
     * Get the island spacing (distance between islands)
     * This is detected from BSkyBlock's configuration
     */
    public int getIslandSpacing() {
        return islandSpacing;
    }

    /**
     * Get the island create listener
     */
    public IslandCreateListener getIslandCreateListener() {
        return islandCreateListener;
    }

    /**
     * Get the search listener
     */
    public SearchListener getSearchListener() {
        return searchListener;
    }

    /**
     * Get the Bukkit server
     */
    public org.bukkit.Server getServer() {
        return Bukkit.getServer();
    }
}
