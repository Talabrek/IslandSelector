package world.bentobox.islandselector;

import world.bentobox.bentobox.api.configuration.ConfigComment;
import world.bentobox.bentobox.api.configuration.ConfigEntry;
import world.bentobox.bentobox.api.configuration.StoreAt;

/**
 * Settings class for IslandSelector
 * Maps to config.yml structure
 */
@StoreAt(filename = "config.yml", path = "addons/IslandSelector")
public class Settings {

    // Grid Settings
    @ConfigComment("Grid starting width (columns)")
    @ConfigEntry(path = "grid.width")
    private int gridWidth = 20;

    @ConfigComment("Grid starting height (rows)")
    @ConfigEntry(path = "grid.height")
    private int gridHeight = 20;

    @ConfigComment("Maximum grid width")
    @ConfigEntry(path = "grid.max-width")
    private int gridMaxWidth = 100;

    @ConfigComment("Maximum grid height")
    @ConfigEntry(path = "grid.max-height")
    private int gridMaxHeight = 100;

    @ConfigComment("Grid origin X coordinate")
    @ConfigEntry(path = "grid.origin.x")
    private int gridOriginX = 0;

    @ConfigComment("Grid origin Z coordinate")
    @ConfigEntry(path = "grid.origin.z")
    private int gridOriginZ = 0;

    @ConfigComment("Distance between islands in blocks")
    @ConfigEntry(path = "grid.island-spacing")
    private int islandSpacing = 500;

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

    // Getters and Setters

    public int getGridWidth() {
        return gridWidth;
    }

    public void setGridWidth(int gridWidth) {
        this.gridWidth = gridWidth;
    }

    public int getGridHeight() {
        return gridHeight;
    }

    public void setGridHeight(int gridHeight) {
        this.gridHeight = gridHeight;
    }

    public int getGridMaxWidth() {
        return gridMaxWidth;
    }

    public void setGridMaxWidth(int gridMaxWidth) {
        this.gridMaxWidth = gridMaxWidth;
    }

    public int getGridMaxHeight() {
        return gridMaxHeight;
    }

    public void setGridMaxHeight(int gridMaxHeight) {
        this.gridMaxHeight = gridMaxHeight;
    }

    public int getGridOriginX() {
        return gridOriginX;
    }

    public void setGridOriginX(int gridOriginX) {
        this.gridOriginX = gridOriginX;
    }

    public int getGridOriginZ() {
        return gridOriginZ;
    }

    public void setGridOriginZ(int gridOriginZ) {
        this.gridOriginZ = gridOriginZ;
    }

    public int getIslandSpacing() {
        return islandSpacing;
    }

    public void setIslandSpacing(int islandSpacing) {
        this.islandSpacing = islandSpacing;
    }

    public int getDefaultSlots() {
        return defaultSlots;
    }

    public void setDefaultSlots(int defaultSlots) {
        this.defaultSlots = defaultSlots;
    }

    public int getMaxSlots() {
        return maxSlots;
    }

    public void setMaxSlots(int maxSlots) {
        this.maxSlots = maxSlots;
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

    public int getScrollAmount() {
        return scrollAmount;
    }

    public void setScrollAmount(int scrollAmount) {
        this.scrollAmount = scrollAmount;
    }

    public int getScrollAmountShift() {
        return scrollAmountShift;
    }

    public void setScrollAmountShift(int scrollAmountShift) {
        this.scrollAmountShift = scrollAmountShift;
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
}
