package world.bentobox.islandselector;

import world.bentobox.bentobox.api.configuration.ConfigComment;
import world.bentobox.bentobox.api.configuration.ConfigEntry;
import world.bentobox.bentobox.api.configuration.ConfigObject;
import world.bentobox.bentobox.api.configuration.StoreAt;

import java.util.ArrayList;
import java.util.List;

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

    // Getters and Setters

    public int getGridMinX() {
        return gridMinX;
    }

    public void setGridMinX(int gridMinX) {
        this.gridMinX = gridMinX;
    }

    public int getGridMaxX() {
        return gridMaxX;
    }

    public void setGridMaxX(int gridMaxX) {
        this.gridMaxX = gridMaxX;
    }

    public int getGridMinZ() {
        return gridMinZ;
    }

    public void setGridMinZ(int gridMinZ) {
        this.gridMinZ = gridMinZ;
    }

    public int getGridMaxZ() {
        return gridMaxZ;
    }

    public void setGridMaxZ(int gridMaxZ) {
        this.gridMaxZ = gridMaxZ;
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
}
