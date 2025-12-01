package world.bentobox.islandselector;

import org.bukkit.Bukkit;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.islandselector.commands.IslandSelectorCommand;
import world.bentobox.islandselector.managers.GridManager;

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

        // Initialize managers
        gridManager = new GridManager(this);

        // Register commands
        registerCommands();

        log("IslandSelector enabled successfully!");
        log("Version: " + getDescription().getVersion());
        log("Grid Size: " + settings.getGridWidth() + "x" + settings.getGridHeight());
        log("Max Slots: " + settings.getMaxSlots());
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
        // Check for BentoBox (should always be present)
        if (!Bukkit.getPluginManager().isPluginEnabled("BentoBox")) {
            logError("BentoBox is not installed or enabled!");
            return false;
        }

        // Check for BSkyBlock
        if (!Bukkit.getPluginManager().isPluginEnabled("BSkyBlock")) {
            logError("BSkyBlock is not installed or enabled!");
            return false;
        }

        // Check for FastAsyncWorldEdit
        if (!Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit")) {
            logError("FastAsyncWorldEdit (FAWE) is not installed or enabled!");
            logError("IslandSelector requires FAWE for schematic operations.");
            return false;
        }

        log("All required dependencies found.");

        // Check optional dependencies
        checkOptionalDependency("Vault", "Economy integration disabled");
        checkOptionalDependency("PlaceholderAPI", "Placeholder integration disabled");
        checkOptionalDependency("Level", "Island level display disabled");

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
}
