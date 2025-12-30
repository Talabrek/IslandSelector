package world.bentobox.islandselector;

import org.bukkit.Bukkit;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.islandselector.commands.IslandSelectorCommand;
import world.bentobox.islandselector.integrations.NovaIntegration;
import world.bentobox.islandselector.integrations.PlaceholderAPIIntegration;
import world.bentobox.islandselector.integrations.WorldEditIntegration;
import world.bentobox.islandselector.listeners.IslandCreateListener;
import world.bentobox.islandselector.listeners.PlayerConnectionListener;
import world.bentobox.islandselector.listeners.SearchListener;
import world.bentobox.islandselector.managers.AutoBackupManager;
import world.bentobox.islandselector.managers.BackupManager;
import world.bentobox.islandselector.managers.BlueprintChallengesManager;
import world.bentobox.islandselector.managers.ChallengesIntegration;
import world.bentobox.islandselector.managers.GridManager;
import world.bentobox.islandselector.managers.IslandRemovalManager;
import world.bentobox.islandselector.managers.LevelIntegration;
import world.bentobox.islandselector.managers.RelocationManager;
import world.bentobox.islandselector.managers.SlotManager;
import world.bentobox.islandselector.managers.SlotSwitchManager;
import world.bentobox.islandselector.utils.EntityStorage;
import world.bentobox.islandselector.utils.SchematicUtils;

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
    private BackupManager backupManager;
    private AutoBackupManager autoBackupManager;
    private RelocationManager relocationManager;
    private IslandRemovalManager islandRemovalManager;
    private ChallengesIntegration challengesIntegration;
    private LevelIntegration levelIntegration;
    private BlueprintChallengesManager blueprintChallengesManager;
    private WorldEditIntegration worldEditIntegration;
    private NovaIntegration novaIntegration;
    private SchematicUtils schematicUtils;
    private EntityStorage entityStorage;
    private IslandCreateListener islandCreateListener;
    private SearchListener searchListener;
    private PlaceholderAPIIntegration placeholderAPI;
    private world.bentobox.islandselector.utils.CustomCommandExecutor customCommandExecutor;
    private int islandSpacing = -1; // Cached from BSkyBlock
    private GameModeAddon bskyblockAddon; // Reference to BSkyBlock addon

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

        // Initialize WorldEdit/FAWE integration (before other managers that depend on it)
        worldEditIntegration = new WorldEditIntegration(this);

        // Initialize Nova integration for custom block support
        novaIntegration = new NovaIntegration(this);

        // Initialize managers
        gridManager = new GridManager(this);
        slotManager = new SlotManager(this);
        slotSwitchManager = new SlotSwitchManager(this);
        backupManager = new BackupManager(this);
        autoBackupManager = new AutoBackupManager(this);
        relocationManager = new RelocationManager(this);
        islandRemovalManager = new IslandRemovalManager(this);
        customCommandExecutor = new world.bentobox.islandselector.utils.CustomCommandExecutor(this);
        challengesIntegration = new ChallengesIntegration(this);
        levelIntegration = new LevelIntegration(this);
        blueprintChallengesManager = new BlueprintChallengesManager(this);
        schematicUtils = new SchematicUtils(this);
        entityStorage = new EntityStorage(this);

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();

        // Register PlaceholderAPI integration
        registerPlaceholderAPI();

        // Start auto-backup scheduler
        autoBackupManager.start();

        // Sync slot data with existing islands (handles pre-existing islands)
        slotManager.syncWithExistingIslands();

        log("IslandSelector enabled successfully!");
        log("Version: " + getDescription().getVersion());
        log("Grid Size: " + settings.getGridWidth() + "x" + settings.getGridHeight());
        log("Island Spacing: " + islandSpacing + " blocks (from BSkyBlock)");
        log("Max Slots: " + settings.getMaxSlots());
        log("FAWE Integration: " + worldEditIntegration.getImplementationName());
        if (!worldEditIntegration.isAvailable()) {
            logWarning("Schematic features (slot switching, backups, relocation) are DISABLED");
            logWarning("Install FastAsyncWorldEdit to enable these features");
        }
        log("Nova Integration: " + (novaIntegration.isAvailable() ? "Enabled" : "Not available"));
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
            bskyblockAddon = bskyblock.get();
            WorldSettings worldSettings = bskyblockAddon.getWorldSettings();
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

        // Register player connection listener for blueprint permissions
        PlayerConnectionListener connectionListener = new PlayerConnectionListener(this);
        Bukkit.getPluginManager().registerEvents(connectionListener, getPlugin());
        log("Registered player connection listener for blueprint permissions");
    }

    @Override
    public void onDisable() {
        // Stop auto-backup scheduler
        if (autoBackupManager != null) {
            autoBackupManager.stop();
        }

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

        log("All required dependencies found.");

        // Check for FAWE (required for island operations)
        if (Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit")) {
            log("FastAsyncWorldEdit found - island operations enabled");
        } else {
            logWarning("FastAsyncWorldEdit NOT found!");
            logWarning("Slot switching, backups, and relocation features are DISABLED.");
            logWarning("Install FAWE to enable these features: https://www.spigotmc.org/resources/fastasyncworldedit.13932/");
        }

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
     * Register PlaceholderAPI expansion if available
     */
    private void registerPlaceholderAPI() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                placeholderAPI = new PlaceholderAPIIntegration(this);
                if (placeholderAPI.register()) {
                    log("PlaceholderAPI integration registered successfully!");
                } else {
                    logWarning("Failed to register PlaceholderAPI expansion");
                }
            } catch (Exception e) {
                logError("Error registering PlaceholderAPI: " + e.getMessage());
            }
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
     * Save the current settings to config.yml
     */
    public void saveSettings() {
        if (settings != null) {
            new Config<>(this, Settings.class).saveConfigObject(settings);
            log("Settings saved to config.yml");
        }
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
     * Get the custom command executor
     */
    public world.bentobox.islandselector.utils.CustomCommandExecutor getCustomCommandExecutor() {
        return customCommandExecutor;
    }

    /**
     * Get the island spacing (distance between islands)
     * This is detected from BSkyBlock's configuration
     */
    public int getIslandSpacing() {
        return islandSpacing;
    }

    /**
     * Get the BSkyBlock addon
     * Used for creating islands via NewIsland.builder()
     */
    public GameModeAddon getBSkyBlockAddon() {
        return bskyblockAddon;
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
     * Get the backup manager
     */
    public BackupManager getBackupManager() {
        return backupManager;
    }

    /**
     * Get the auto-backup manager
     */
    public AutoBackupManager getAutoBackupManager() {
        return autoBackupManager;
    }

    /**
     * Get the relocation manager
     */
    public RelocationManager getRelocationManager() {
        return relocationManager;
    }

    /**
     * Get the island removal manager
     */
    public IslandRemovalManager getIslandRemovalManager() {
        return islandRemovalManager;
    }

    /**
     * Get the Challenges integration manager
     */
    public ChallengesIntegration getChallengesIntegration() {
        return challengesIntegration;
    }

    /**
     * Get the Blueprint Challenges manager for permission-based challenge filtering
     */
    public BlueprintChallengesManager getBlueprintChallengesManager() {
        return blueprintChallengesManager;
    }

    /**
     * Get the Level addon integration manager
     */
    public LevelIntegration getLevelIntegration() {
        return levelIntegration;
    }

    /**
     * Get the Bukkit server
     */
    public org.bukkit.Server getServer() {
        return Bukkit.getServer();
    }

    /**
     * Get the schematic utilities for safe WorldEdit/FAWE operations
     */
    public SchematicUtils getSchematicUtils() {
        return schematicUtils;
    }

    /**
     * Get the WorldEdit/FAWE integration layer
     */
    public WorldEditIntegration getWorldEditIntegration() {
        return worldEditIntegration;
    }

    /**
     * Get the Nova integration for custom block support
     */
    public NovaIntegration getNovaIntegration() {
        return novaIntegration;
    }

    /**
     * Check if schematic operations are available (FAWE installed)
     */
    public boolean isSchematicOperationsAvailable() {
        return worldEditIntegration != null && worldEditIntegration.isAvailable();
    }

    /**
     * Get the entity storage for saving/loading entities separately from FAWE
     */
    public EntityStorage getEntityStorage() {
        return entityStorage;
    }
}
