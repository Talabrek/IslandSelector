package world.bentobox.islandselector;

import world.bentobox.bentobox.api.configuration.ConfigComment;
import world.bentobox.bentobox.api.configuration.ConfigEntry;
import world.bentobox.bentobox.api.configuration.ConfigObject;
import world.bentobox.bentobox.api.configuration.StoreAt;
import world.bentobox.islandselector.models.DimensionConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Settings class for IslandSelector
 * Maps to config.yml structure
 */
@StoreAt(filename = "config.yml", path = "addons/IslandSelector")
public class Settings implements ConfigObject {

    // Grid Settings - Grid is centered at 0,0
    @ConfigComment("Minimum X coordinate (western boundary)")
    @ConfigEntry(path = "grid.min-x")
    private int gridMinX = -10;

    @ConfigComment("Maximum X coordinate (eastern boundary)")
    @ConfigEntry(path = "grid.max-x")
    private int gridMaxX = 10;

    @ConfigComment("Minimum Z coordinate (northern boundary)")
    @ConfigEntry(path = "grid.min-z")
    private int gridMinZ = -10;

    @ConfigComment("Maximum Z coordinate (southern boundary)")
    @ConfigEntry(path = "grid.max-z")
    private int gridMaxZ = 10;

    // Note: Island spacing is automatically detected from BSkyBlock's configuration

    // Slot Settings
    @ConfigComment("Default number of slots")
    @ConfigEntry(path = "slots.default-slots")
    private int defaultSlots = 2;

    @ConfigComment("Maximum number of slots")
    @ConfigEntry(path = "slots.max-slots")
    private int maxSlots = 5;

    @ConfigComment("Slot switch cooldown in seconds")
    @ConfigEntry(path = "slots.switch-cooldown")
    private int switchCooldown = 3600;

    @ConfigComment("Default slot name")
    @ConfigEntry(path = "slots.default-slot-name")
    private String defaultSlotName = "Island {number}";

    @ConfigComment("Default slot icon material")
    @ConfigEntry(path = "slots.default-slot-icon")
    private String defaultSlotIcon = "GRASS_BLOCK";

    @ConfigComment("")
    @ConfigComment("Enable the multi-slot island system")
    @ConfigComment("When disabled:")
    @ConfigComment("  - /islandselector slots command shows disabled message")
    @ConfigComment("  - Slots button is hidden from the main grid GUI")
    @ConfigComment("  - Existing slot data is preserved but inaccessible")
    @ConfigEntry(path = "slots.enabled")
    private boolean slotsEnabled = true;

    // Relocation Settings
    @ConfigComment("Relocation cooldown in seconds")
    @ConfigEntry(path = "relocation.cooldown")
    private int relocationCooldown = 604800;

    @ConfigComment("Relocation cost")
    @ConfigEntry(path = "relocation.cost")
    private double relocationCost = 0.0;

    @ConfigComment("Currency name")
    @ConfigEntry(path = "relocation.currency-name")
    private String currencyName = "coins";

    // Backup Settings
    @ConfigComment("Enable automatic backups")
    @ConfigEntry(path = "backups.enabled")
    private boolean backupsEnabled = true;

    @ConfigComment("Backup on slot switch")
    @ConfigEntry(path = "backups.on-switch")
    private boolean backupOnSwitch = true;

    @ConfigComment("Maximum backups per slot")
    @ConfigEntry(path = "backups.max-per-slot")
    private int maxBackupsPerSlot = 3;

    @ConfigComment("Backup on shutdown")
    @ConfigEntry(path = "backups.on-shutdown")
    private boolean backupOnShutdown = false;

    @ConfigComment("Enable periodic auto-backup for online players")
    @ConfigEntry(path = "backups.auto-backup-enabled")
    private boolean autoBackupEnabled = true;

    @ConfigComment("Auto-backup interval in minutes (default: 60 = 1 hour)")
    @ConfigEntry(path = "backups.auto-backup-interval")
    private int autoBackupInterval = 60;

    // GUI Settings
    @ConfigComment("GUI scroll amount")
    @ConfigEntry(path = "gui.scroll-amount")
    private int scrollAmount = 1;

    @ConfigComment("GUI scroll amount with shift")
    @ConfigEntry(path = "gui.scroll-amount-shift")
    private int scrollAmountShift = 5;

    @ConfigComment("Active slot glow effect")
    @ConfigEntry(path = "gui.active-slot-glow")
    private boolean activeSlotGlow = true;

    // GUI Item Settings
    @ConfigComment("Item for available locations")
    @ConfigEntry(path = "gui.items.available")
    private String itemAvailable = "GREEN_STAINED_GLASS_PANE";

    @ConfigComment("Item for offline player islands")
    @ConfigEntry(path = "gui.items.offline")
    private String itemOffline = "RED_STAINED_GLASS_PANE";

    @ConfigComment("Item for reserved blocked locations")
    @ConfigEntry(path = "gui.items.reserved-blocked")
    private String itemReservedBlocked = "GRAY_STAINED_GLASS_PANE";

    @ConfigComment("Item for reserved purchasable locations")
    @ConfigEntry(path = "gui.items.reserved-purchasable")
    private String itemReservedPurchasable = "GOLD_BLOCK";

    @ConfigComment("Item for locked areas")
    @ConfigEntry(path = "gui.items.locked-area")
    private String itemLockedArea = "BLACK_STAINED_GLASS_PANE";

    @ConfigComment("Item for filler slots")
    @ConfigEntry(path = "gui.items.filler")
    private String itemFiller = "BLACK_STAINED_GLASS_PANE";

    // Visitor Settings
    @ConfigComment("Visitor teleport delay in ticks")
    @ConfigEntry(path = "visitors.teleport-delay")
    private int visitorTeleportDelay = 40;

    @ConfigComment("Visitor warning message")
    @ConfigEntry(path = "visitors.warning-message")
    private String visitorWarningMessage = "&eThe island owner is switching islands. You will be teleported in 2 seconds...";

    // Performance Settings
    @ConfigComment("Cache duration in seconds")
    @ConfigEntry(path = "performance.cache-duration")
    private int cacheDuration = 60;

    @ConfigComment("Maximum concurrent schematic operations")
    @ConfigEntry(path = "performance.max-concurrent-operations")
    private int maxConcurrentOperations = 2;

    // Integration Settings
    @ConfigComment("Enable Vault integration")
    @ConfigEntry(path = "integration.vault.enabled")
    private boolean vaultEnabled = true;

    @ConfigComment("Enable PlaceholderAPI integration")
    @ConfigEntry(path = "integration.placeholderapi.enabled")
    private boolean placeholderAPIEnabled = true;

    @ConfigComment("Enable Level addon integration")
    @ConfigEntry(path = "integration.level-addon.enabled")
    private boolean levelAddonEnabled = true;

    // Custom Command Settings - Relocation
    @ConfigComment("Enable custom commands after relocation")
    @ConfigEntry(path = "custom-commands.relocation.enabled")
    private boolean relocationCommandsEnabled = false;

    @ConfigComment("Commands to run after relocation")
    @ConfigEntry(path = "custom-commands.relocation.commands")
    private List<String> relocationCommands = new ArrayList<>();

    @ConfigComment("Execution scope for relocation commands: CONSOLE, PLAYER, or PLAYER_OP")
    @ConfigEntry(path = "custom-commands.relocation.scope")
    private String relocationCommandScope = "CONSOLE";

    // Custom Command Settings - Slot Switch
    @ConfigComment("Enable custom commands after slot switch")
    @ConfigEntry(path = "custom-commands.slot-switch.enabled")
    private boolean slotSwitchCommandsEnabled = false;

    @ConfigComment("Commands to run after slot switch")
    @ConfigEntry(path = "custom-commands.slot-switch.commands")
    private List<String> slotSwitchCommands = new ArrayList<>();

    @ConfigComment("Execution scope for slot switch commands: CONSOLE, PLAYER, or PLAYER_OP")
    @ConfigEntry(path = "custom-commands.slot-switch.scope")
    private String slotSwitchCommandScope = "CONSOLE";

    // Debug Settings
    @ConfigComment("Enable debug logging")
    @ConfigEntry(path = "debug.enabled")
    private boolean debugEnabled = false;

    @ConfigComment("Log schematic operations")
    @ConfigEntry(path = "debug.log-schematics")
    private boolean logSchematics = false;

    @ConfigComment("Log GUI interactions")
    @ConfigEntry(path = "debug.log-gui")
    private boolean logGUI = false;

    // Multi-Dimension Settings
    @ConfigComment("")
    @ConfigComment("==========================================")
    @ConfigComment("MULTI-DIMENSION SETTINGS")
    @ConfigComment("==========================================")
    @ConfigComment("Enable multi-dimension island support.")
    @ConfigComment("When enabled, players can have islands in multiple dimensions")
    @ConfigComment("(e.g., overworld, nether, end, custom worlds) all at the same grid location.")
    @ConfigEntry(path = "dimensions.enabled")
    private boolean multiDimensionEnabled = false;

    @ConfigComment("")
    @ConfigComment("Dimension configurations.")
    @ConfigComment("Each dimension is stored as a serialized string with format:")
    @ConfigComment("world-name;enabled;default-blueprint;create-on-claim;display-name;icon-material;generator-type;environment;create-if-missing;world-seed")
    @ConfigEntry(path = "dimensions.worlds")
    private Map<String, String> dimensionConfigsRaw = new HashMap<>();

    @ConfigComment("")
    @ConfigComment("Primary dimension key - the main world dimension.")
    @ConfigComment("This is the dimension used for grid selection GUI and as the 'home' dimension.")
    @ConfigEntry(path = "dimensions.primary-dimension")
    private String primaryDimension = "overworld";

    // Getters and Setters

    public int getGridMinX() {
        return gridMinX;
    }

    public void setGridMinX(int gridMinX) {
        this.gridMinX = gridMinX;
        // Ensure gridMaxX is at least gridMinX
        if (this.gridMaxX < gridMinX) {
            this.gridMaxX = gridMinX;
        }
    }

    public int getGridMaxX() {
        return gridMaxX;
    }

    public void setGridMaxX(int gridMaxX) {
        // Ensure gridMaxX is at least gridMinX
        this.gridMaxX = Math.max(this.gridMinX, gridMaxX);
    }

    public int getGridMinZ() {
        return gridMinZ;
    }

    public void setGridMinZ(int gridMinZ) {
        this.gridMinZ = gridMinZ;
        // Ensure gridMaxZ is at least gridMinZ
        if (this.gridMaxZ < gridMinZ) {
            this.gridMaxZ = gridMinZ;
        }
    }

    public int getGridMaxZ() {
        return gridMaxZ;
    }

    public void setGridMaxZ(int gridMaxZ) {
        // Ensure gridMaxZ is at least gridMinZ
        this.gridMaxZ = Math.max(this.gridMinZ, gridMaxZ);
    }

    /**
     * Get the grid width (number of columns)
     */
    public int getGridWidth() {
        return gridMaxX - gridMinX + 1;
    }

    /**
     * Get the grid height (number of rows)
     */
    public int getGridHeight() {
        return gridMaxZ - gridMinZ + 1;
    }


    public int getDefaultSlots() {
        return defaultSlots;
    }

    public void setDefaultSlots(int defaultSlots) {
        // Ensure at least 1 default slot
        this.defaultSlots = Math.max(1, defaultSlots);
    }

    public int getMaxSlots() {
        return maxSlots;
    }

    public void setMaxSlots(int maxSlots) {
        // Ensure at least 1 slot is available
        this.maxSlots = Math.max(1, maxSlots);
    }

    public int getSwitchCooldown() {
        return switchCooldown;
    }

    public void setSwitchCooldown(int switchCooldown) {
        this.switchCooldown = switchCooldown;
    }

    public String getDefaultSlotName() {
        return defaultSlotName;
    }

    public void setDefaultSlotName(String defaultSlotName) {
        this.defaultSlotName = defaultSlotName;
    }

    public String getDefaultSlotIcon() {
        return defaultSlotIcon;
    }

    public void setDefaultSlotIcon(String defaultSlotIcon) {
        this.defaultSlotIcon = defaultSlotIcon;
    }

    public boolean isSlotsEnabled() {
        return slotsEnabled;
    }

    public void setSlotsEnabled(boolean slotsEnabled) {
        this.slotsEnabled = slotsEnabled;
    }

    public int getRelocationCooldown() {
        return relocationCooldown;
    }

    public void setRelocationCooldown(int relocationCooldown) {
        this.relocationCooldown = relocationCooldown;
    }

    public double getRelocationCost() {
        return relocationCost;
    }

    public void setRelocationCost(double relocationCost) {
        this.relocationCost = relocationCost;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }

    public boolean isBackupsEnabled() {
        return backupsEnabled;
    }

    public void setBackupsEnabled(boolean backupsEnabled) {
        this.backupsEnabled = backupsEnabled;
    }

    public boolean isBackupOnSwitch() {
        return backupOnSwitch;
    }

    public void setBackupOnSwitch(boolean backupOnSwitch) {
        this.backupOnSwitch = backupOnSwitch;
    }

    public int getMaxBackupsPerSlot() {
        return maxBackupsPerSlot;
    }

    public void setMaxBackupsPerSlot(int maxBackupsPerSlot) {
        this.maxBackupsPerSlot = maxBackupsPerSlot;
    }

    public boolean isBackupOnShutdown() {
        return backupOnShutdown;
    }

    public void setBackupOnShutdown(boolean backupOnShutdown) {
        this.backupOnShutdown = backupOnShutdown;
    }

    public boolean isAutoBackupEnabled() {
        return autoBackupEnabled;
    }

    public void setAutoBackupEnabled(boolean autoBackupEnabled) {
        this.autoBackupEnabled = autoBackupEnabled;
    }

    public int getAutoBackupInterval() {
        return autoBackupInterval;
    }

    public void setAutoBackupInterval(int autoBackupInterval) {
        this.autoBackupInterval = autoBackupInterval;
    }

    public int getScrollAmount() {
        return scrollAmount;
    }

    public void setScrollAmount(int scrollAmount) {
        // Ensure at least 1 for scroll amount
        this.scrollAmount = Math.max(1, scrollAmount);
    }

    public int getScrollAmountShift() {
        return scrollAmountShift;
    }

    public void setScrollAmountShift(int scrollAmountShift) {
        // Ensure at least 1 for shift scroll amount
        this.scrollAmountShift = Math.max(1, scrollAmountShift);
    }

    public boolean isActiveSlotGlow() {
        return activeSlotGlow;
    }

    public void setActiveSlotGlow(boolean activeSlotGlow) {
        this.activeSlotGlow = activeSlotGlow;
    }

    public int getVisitorTeleportDelay() {
        return visitorTeleportDelay;
    }

    public void setVisitorTeleportDelay(int visitorTeleportDelay) {
        this.visitorTeleportDelay = visitorTeleportDelay;
    }

    public String getVisitorWarningMessage() {
        return visitorWarningMessage;
    }

    public void setVisitorWarningMessage(String visitorWarningMessage) {
        this.visitorWarningMessage = visitorWarningMessage;
    }

    public int getCacheDuration() {
        return cacheDuration;
    }

    public void setCacheDuration(int cacheDuration) {
        this.cacheDuration = cacheDuration;
    }

    public int getMaxConcurrentOperations() {
        return maxConcurrentOperations;
    }

    public void setMaxConcurrentOperations(int maxConcurrentOperations) {
        this.maxConcurrentOperations = maxConcurrentOperations;
    }

    public boolean isVaultEnabled() {
        return vaultEnabled;
    }

    public void setVaultEnabled(boolean vaultEnabled) {
        this.vaultEnabled = vaultEnabled;
    }

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }

    public void setPlaceholderAPIEnabled(boolean placeholderAPIEnabled) {
        this.placeholderAPIEnabled = placeholderAPIEnabled;
    }

    public boolean isLevelAddonEnabled() {
        return levelAddonEnabled;
    }

    public void setLevelAddonEnabled(boolean levelAddonEnabled) {
        this.levelAddonEnabled = levelAddonEnabled;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public boolean isLogSchematics() {
        return logSchematics;
    }

    public void setLogSchematics(boolean logSchematics) {
        this.logSchematics = logSchematics;
    }

    public boolean isLogGUI() {
        return logGUI;
    }

    public void setLogGUI(boolean logGUI) {
        this.logGUI = logGUI;
    }

    // GUI Item Getters and Setters

    public String getItemAvailable() {
        return itemAvailable;
    }

    public void setItemAvailable(String itemAvailable) {
        this.itemAvailable = itemAvailable;
    }

    public String getItemOffline() {
        return itemOffline;
    }

    public void setItemOffline(String itemOffline) {
        this.itemOffline = itemOffline;
    }

    public String getItemReservedBlocked() {
        return itemReservedBlocked;
    }

    public void setItemReservedBlocked(String itemReservedBlocked) {
        this.itemReservedBlocked = itemReservedBlocked;
    }

    public String getItemReservedPurchasable() {
        return itemReservedPurchasable;
    }

    public void setItemReservedPurchasable(String itemReservedPurchasable) {
        this.itemReservedPurchasable = itemReservedPurchasable;
    }

    public String getItemLockedArea() {
        return itemLockedArea;
    }

    public void setItemLockedArea(String itemLockedArea) {
        this.itemLockedArea = itemLockedArea;
    }

    public String getItemFiller() {
        return itemFiller;
    }

    public void setItemFiller(String itemFiller) {
        this.itemFiller = itemFiller;
    }

    // Custom Command Getters and Setters

    public boolean isRelocationCommandsEnabled() {
        return relocationCommandsEnabled;
    }

    public void setRelocationCommandsEnabled(boolean relocationCommandsEnabled) {
        this.relocationCommandsEnabled = relocationCommandsEnabled;
    }

    public List<String> getRelocationCommands() {
        return relocationCommands;
    }

    public void setRelocationCommands(List<String> relocationCommands) {
        this.relocationCommands = relocationCommands;
    }

    public String getRelocationCommandScope() {
        return relocationCommandScope;
    }

    public void setRelocationCommandScope(String relocationCommandScope) {
        this.relocationCommandScope = relocationCommandScope;
    }

    public boolean isSlotSwitchCommandsEnabled() {
        return slotSwitchCommandsEnabled;
    }

    public void setSlotSwitchCommandsEnabled(boolean slotSwitchCommandsEnabled) {
        this.slotSwitchCommandsEnabled = slotSwitchCommandsEnabled;
    }

    public List<String> getSlotSwitchCommands() {
        return slotSwitchCommands;
    }

    public void setSlotSwitchCommands(List<String> slotSwitchCommands) {
        this.slotSwitchCommands = slotSwitchCommands;
    }

    public String getSlotSwitchCommandScope() {
        return slotSwitchCommandScope;
    }

    public void setSlotSwitchCommandScope(String slotSwitchCommandScope) {
        this.slotSwitchCommandScope = slotSwitchCommandScope;
    }

    // Multi-Dimension Getters and Setters

    public boolean isMultiDimensionEnabled() {
        return multiDimensionEnabled;
    }

    public void setMultiDimensionEnabled(boolean multiDimensionEnabled) {
        this.multiDimensionEnabled = multiDimensionEnabled;
    }

    /**
     * Serialize a DimensionConfig to a semicolon-delimited string.
     * Format: world-name;enabled;default-blueprint;create-on-claim;display-name;icon-material;generator-type;environment;create-if-missing;world-seed
     */
    private String serializeDimensionConfig(DimensionConfig config) {
        return String.join(";",
            nullSafe(config.getWorldName()),
            String.valueOf(config.isEnabled()),
            nullSafe(config.getDefaultBlueprint()),
            String.valueOf(config.isCreateOnClaim()),
            nullSafe(config.getDisplayName()),
            nullSafe(config.getIconMaterial()),
            nullSafe(config.getGeneratorType()),
            nullSafe(config.getEnvironment()),
            String.valueOf(config.isCreateIfMissing()),
            config.getWorldSeed() != null ? String.valueOf(config.getWorldSeed()) : ""
        );
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    /**
     * Deserialize a semicolon-delimited string to a DimensionConfig.
     */
    private DimensionConfig deserializeDimensionConfig(String key, String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return null;
        }
        String[] parts = serialized.split(";", -1); // -1 to keep trailing empty strings
        if (parts.length < 9) {
            return null; // Invalid format
        }
        DimensionConfig config = new DimensionConfig();
        config.setDimensionKey(key);
        config.setWorldName(parts[0].isEmpty() ? null : parts[0]);
        config.setEnabled(Boolean.parseBoolean(parts[1]));
        config.setDefaultBlueprint(parts[2].isEmpty() ? "default" : parts[2]);
        config.setCreateOnClaim(Boolean.parseBoolean(parts[3]));
        config.setDisplayName(parts[4].isEmpty() ? null : parts[4]);
        config.setIconMaterial(parts[5].isEmpty() ? "GRASS_BLOCK" : parts[5]);
        config.setGeneratorType(parts[6].isEmpty() ? "void" : parts[6]);
        config.setEnvironment(parts[7].isEmpty() ? "normal" : parts[7]);
        config.setCreateIfMissing(Boolean.parseBoolean(parts[8]));
        if (parts.length > 9 && !parts[9].isEmpty()) {
            try {
                config.setWorldSeed(Long.parseLong(parts[9]));
            } catch (NumberFormatException e) {
                // Leave as null
            }
        }
        return config;
    }

    /**
     * Get the raw dimension configuration strings (for BentoBox YAML serialization).
     * Use getDimensionConfigs() for code that needs DimensionConfig objects.
     */
    public Map<String, String> getDimensionConfigsRaw() {
        return dimensionConfigsRaw;
    }

    /**
     * Set the raw dimension configuration strings (for BentoBox YAML deserialization).
     * Use setDimensionConfigs() for code that has DimensionConfig objects.
     */
    public void setDimensionConfigsRaw(Map<String, String> dimensionConfigsRaw) {
        this.dimensionConfigsRaw = dimensionConfigsRaw;
    }

    /**
     * Get dimension configurations converted from serialized strings.
     */
    public Map<String, DimensionConfig> getDimensionConfigs() {
        Map<String, DimensionConfig> result = new HashMap<>();
        if (dimensionConfigsRaw != null) {
            for (Map.Entry<String, String> entry : dimensionConfigsRaw.entrySet()) {
                DimensionConfig config = deserializeDimensionConfig(entry.getKey(), entry.getValue());
                if (config != null) {
                    result.put(entry.getKey(), config);
                }
            }
        }
        return result;
    }

    /**
     * Set dimension configurations, serializing to strings for YAML.
     */
    public void setDimensionConfigs(Map<String, DimensionConfig> dimensionConfigs) {
        this.dimensionConfigsRaw = new HashMap<>();
        if (dimensionConfigs != null) {
            for (Map.Entry<String, DimensionConfig> entry : dimensionConfigs.entrySet()) {
                this.dimensionConfigsRaw.put(entry.getKey(), serializeDimensionConfig(entry.getValue()));
            }
        }
    }

    public String getPrimaryDimension() {
        return primaryDimension;
    }

    public void setPrimaryDimension(String primaryDimension) {
        this.primaryDimension = primaryDimension;
    }

    /**
     * Get a specific dimension configuration by key
     * @param dimensionKey The dimension key (e.g., "overworld", "nether")
     * @return The dimension config, or null if not found
     */
    public DimensionConfig getDimensionConfig(String dimensionKey) {
        if (dimensionConfigsRaw == null || dimensionKey == null) {
            return null;
        }
        String serialized = dimensionConfigsRaw.get(dimensionKey);
        return deserializeDimensionConfig(dimensionKey, serialized);
    }

    /**
     * Get all enabled dimension configurations
     * @return List of enabled dimension configs
     */
    public List<DimensionConfig> getEnabledDimensions() {
        if (dimensionConfigsRaw == null || dimensionConfigsRaw.isEmpty()) {
            return new ArrayList<>();
        }
        return getDimensionConfigs().values().stream()
                .filter(DimensionConfig::isEnabled)
                .collect(Collectors.toList());
    }

    /**
     * Get the primary dimension configuration
     * @return The primary dimension config, or null if not configured
     */
    public DimensionConfig getPrimaryDimensionConfig() {
        return getDimensionConfig(primaryDimension);
    }

    /**
     * Check if a dimension key exists in the configuration
     * @param dimensionKey The dimension key to check
     * @return true if the dimension is configured
     */
    public boolean hasDimension(String dimensionKey) {
        return dimensionConfigsRaw != null && dimensionConfigsRaw.containsKey(dimensionKey);
    }

    /**
     * Check if a dimension is enabled
     * @param dimensionKey The dimension key to check
     * @return true if the dimension is configured and enabled
     */
    public boolean isDimensionEnabled(String dimensionKey) {
        DimensionConfig config = getDimensionConfig(dimensionKey);
        return config != null && config.isEnabled();
    }

    /**
     * Get the world name for a dimension
     * @param dimensionKey The dimension key
     * @return The Bukkit world name, or null if dimension not configured
     */
    public String getDimensionWorldName(String dimensionKey) {
        DimensionConfig config = getDimensionConfig(dimensionKey);
        return config != null ? config.getWorldName() : null;
    }

    /**
     * Get the default blueprint for a dimension
     * @param dimensionKey The dimension key
     * @return The blueprint bundle name, or "default" if not configured
     */
    public String getDimensionBlueprint(String dimensionKey) {
        DimensionConfig config = getDimensionConfig(dimensionKey);
        return config != null ? config.getDefaultBlueprint() : "default";
    }

    /**
     * Initialize default dimension configurations if none exist.
     * Called during setup to provide sensible defaults.
     */
    public void initializeDefaultDimensions() {
        if (dimensionConfigsRaw == null) {
            dimensionConfigsRaw = new HashMap<>();
        }
        if (dimensionConfigsRaw.isEmpty()) {
            Map<String, DimensionConfig> defaults = new HashMap<>();

            // Add default overworld dimension
            DimensionConfig overworld = new DimensionConfig("overworld", "bskyblock_world");
            overworld.setDisplayName("Overworld");
            overworld.setIconMaterial("GRASS_BLOCK");
            overworld.setDefaultBlueprint("default");
            overworld.setGeneratorType("void");
            overworld.setEnvironment("normal");
            overworld.setCreateIfMissing(true);
            defaults.put("overworld", overworld);

            // Add default nether dimension (disabled by default)
            DimensionConfig nether = new DimensionConfig("nether", "bskyblock_world_nether");
            nether.setEnabled(false);
            nether.setDisplayName("Nether");
            nether.setIconMaterial("NETHERRACK");
            nether.setDefaultBlueprint("default_nether");
            nether.setGeneratorType("void");
            nether.setEnvironment("nether");
            nether.setCreateIfMissing(true);
            defaults.put("nether", nether);

            // Add default end dimension (disabled by default)
            DimensionConfig theEnd = new DimensionConfig("the_end", "bskyblock_world_the_end");
            theEnd.setEnabled(false);
            theEnd.setDisplayName("The End");
            theEnd.setIconMaterial("END_STONE");
            theEnd.setDefaultBlueprint("default_end");
            theEnd.setGeneratorType("void");
            theEnd.setEnvironment("the_end");
            theEnd.setCreateIfMissing(true);
            defaults.put("the_end", theEnd);

            // Use setter to convert to raw maps
            setDimensionConfigs(defaults);
        }
    }
}
