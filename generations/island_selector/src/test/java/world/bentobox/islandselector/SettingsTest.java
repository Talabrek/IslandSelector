package world.bentobox.islandselector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Settings class.
 * Tests default values and getter/setter functionality.
 */
@DisplayName("Settings Tests")
class SettingsTest {

    private Settings settings;

    @BeforeEach
    void setUp() {
        settings = new Settings();
    }

    // ==================== GRID SETTINGS TESTS ====================

    @Test
    @DisplayName("Default gridMinX is -10")
    void testDefaultGridMinX() {
        assertEquals(-10, settings.getGridMinX());
    }

    @Test
    @DisplayName("Default gridMaxX is 10")
    void testDefaultGridMaxX() {
        assertEquals(10, settings.getGridMaxX());
    }

    @Test
    @DisplayName("Default gridMinZ is -10")
    void testDefaultGridMinZ() {
        assertEquals(-10, settings.getGridMinZ());
    }

    @Test
    @DisplayName("Default gridMaxZ is 10")
    void testDefaultGridMaxZ() {
        assertEquals(10, settings.getGridMaxZ());
    }

    @Test
    @DisplayName("Grid width calculation")
    void testGridWidth() {
        // Default: maxX(10) - minX(-10) + 1 = 21
        assertEquals(21, settings.getGridWidth());
    }

    @Test
    @DisplayName("Grid height calculation")
    void testGridHeight() {
        // Default: maxZ(10) - minZ(-10) + 1 = 21
        assertEquals(21, settings.getGridHeight());
    }

    @Test
    @DisplayName("Custom grid dimensions affect width/height")
    void testCustomGridDimensions() {
        settings.setGridMinX(-5);
        settings.setGridMaxX(5);
        settings.setGridMinZ(-3);
        settings.setGridMaxZ(3);

        assertEquals(11, settings.getGridWidth());  // 5 - (-5) + 1
        assertEquals(7, settings.getGridHeight());  // 3 - (-3) + 1
    }

    @Test
    @DisplayName("Grid setters work correctly")
    void testGridSetters() {
        settings.setGridMinX(-20);
        settings.setGridMaxX(20);
        settings.setGridMinZ(-15);
        settings.setGridMaxZ(15);

        assertEquals(-20, settings.getGridMinX());
        assertEquals(20, settings.getGridMaxX());
        assertEquals(-15, settings.getGridMinZ());
        assertEquals(15, settings.getGridMaxZ());
    }

    // ==================== SLOT SETTINGS TESTS ====================

    @Test
    @DisplayName("Default slots is 2")
    void testDefaultSlots() {
        assertEquals(2, settings.getDefaultSlots());
    }

    @Test
    @DisplayName("Max slots is 5")
    void testMaxSlots() {
        assertEquals(5, settings.getMaxSlots());
    }

    @Test
    @DisplayName("Switch cooldown default is 3600 seconds (1 hour)")
    void testSwitchCooldown() {
        assertEquals(3600, settings.getSwitchCooldown());
    }

    @Test
    @DisplayName("Default slot name contains placeholder")
    void testDefaultSlotName() {
        assertEquals("Island {number}", settings.getDefaultSlotName());
    }

    @Test
    @DisplayName("Default slot icon is GRASS_BLOCK")
    void testDefaultSlotIcon() {
        assertEquals("GRASS_BLOCK", settings.getDefaultSlotIcon());
    }

    @Test
    @DisplayName("Slot setters work correctly")
    void testSlotSetters() {
        settings.setDefaultSlots(3);
        settings.setMaxSlots(10);
        settings.setSwitchCooldown(7200);
        settings.setDefaultSlotName("My Island {number}");
        settings.setDefaultSlotIcon("DIAMOND_BLOCK");

        assertEquals(3, settings.getDefaultSlots());
        assertEquals(10, settings.getMaxSlots());
        assertEquals(7200, settings.getSwitchCooldown());
        assertEquals("My Island {number}", settings.getDefaultSlotName());
        assertEquals("DIAMOND_BLOCK", settings.getDefaultSlotIcon());
    }

    // ==================== RELOCATION SETTINGS TESTS ====================

    @Test
    @DisplayName("Relocation cooldown default is 604800 seconds (1 week)")
    void testRelocationCooldown() {
        assertEquals(604800, settings.getRelocationCooldown());
    }

    @Test
    @DisplayName("Relocation cost default is 0.0")
    void testRelocationCost() {
        assertEquals(0.0, settings.getRelocationCost(), 0.001);
    }

    @Test
    @DisplayName("Currency name default is 'coins'")
    void testCurrencyName() {
        assertEquals("coins", settings.getCurrencyName());
    }

    @Test
    @DisplayName("Relocation setters work correctly")
    void testRelocationSetters() {
        settings.setRelocationCooldown(86400);
        settings.setRelocationCost(1000.0);
        settings.setCurrencyName("dollars");

        assertEquals(86400, settings.getRelocationCooldown());
        assertEquals(1000.0, settings.getRelocationCost(), 0.001);
        assertEquals("dollars", settings.getCurrencyName());
    }

    // ==================== BACKUP SETTINGS TESTS ====================

    @Test
    @DisplayName("Backups enabled by default")
    void testBackupsEnabled() {
        assertTrue(settings.isBackupsEnabled());
    }

    @Test
    @DisplayName("Backup on switch enabled by default")
    void testBackupOnSwitch() {
        assertTrue(settings.isBackupOnSwitch());
    }

    @Test
    @DisplayName("Max backups per slot default is 3")
    void testMaxBackupsPerSlot() {
        assertEquals(3, settings.getMaxBackupsPerSlot());
    }

    @Test
    @DisplayName("Backup on shutdown disabled by default")
    void testBackupOnShutdown() {
        assertFalse(settings.isBackupOnShutdown());
    }

    @Test
    @DisplayName("Backup setters work correctly")
    void testBackupSetters() {
        settings.setBackupsEnabled(false);
        settings.setBackupOnSwitch(false);
        settings.setMaxBackupsPerSlot(5);
        settings.setBackupOnShutdown(true);

        assertFalse(settings.isBackupsEnabled());
        assertFalse(settings.isBackupOnSwitch());
        assertEquals(5, settings.getMaxBackupsPerSlot());
        assertTrue(settings.isBackupOnShutdown());
    }

    // ==================== GUI SETTINGS TESTS ====================

    @Test
    @DisplayName("Scroll amount default is 1")
    void testScrollAmount() {
        assertEquals(1, settings.getScrollAmount());
    }

    @Test
    @DisplayName("Scroll amount shift default is 5")
    void testScrollAmountShift() {
        assertEquals(5, settings.getScrollAmountShift());
    }

    @Test
    @DisplayName("Active slot glow enabled by default")
    void testActiveSlotGlow() {
        assertTrue(settings.isActiveSlotGlow());
    }

    @Test
    @DisplayName("GUI item materials have correct defaults")
    void testGuiItemDefaults() {
        assertEquals("GREEN_STAINED_GLASS_PANE", settings.getItemAvailable());
        assertEquals("RED_STAINED_GLASS_PANE", settings.getItemOffline());
        assertEquals("GRAY_STAINED_GLASS_PANE", settings.getItemReservedBlocked());
        assertEquals("GOLD_BLOCK", settings.getItemReservedPurchasable());
        assertEquals("BLACK_STAINED_GLASS_PANE", settings.getItemLockedArea());
        assertEquals("BLACK_STAINED_GLASS_PANE", settings.getItemFiller());
    }

    @Test
    @DisplayName("GUI setters work correctly")
    void testGuiSetters() {
        settings.setScrollAmount(2);
        settings.setScrollAmountShift(10);
        settings.setActiveSlotGlow(false);
        settings.setItemAvailable("LIME_STAINED_GLASS_PANE");
        settings.setItemOffline("ORANGE_STAINED_GLASS_PANE");
        settings.setItemReservedBlocked("LIGHT_GRAY_STAINED_GLASS_PANE");
        settings.setItemReservedPurchasable("IRON_BLOCK");
        settings.setItemLockedArea("BEDROCK");
        settings.setItemFiller("GRAY_STAINED_GLASS_PANE");

        assertEquals(2, settings.getScrollAmount());
        assertEquals(10, settings.getScrollAmountShift());
        assertFalse(settings.isActiveSlotGlow());
        assertEquals("LIME_STAINED_GLASS_PANE", settings.getItemAvailable());
        assertEquals("ORANGE_STAINED_GLASS_PANE", settings.getItemOffline());
        assertEquals("LIGHT_GRAY_STAINED_GLASS_PANE", settings.getItemReservedBlocked());
        assertEquals("IRON_BLOCK", settings.getItemReservedPurchasable());
        assertEquals("BEDROCK", settings.getItemLockedArea());
        assertEquals("GRAY_STAINED_GLASS_PANE", settings.getItemFiller());
    }

    // ==================== VISITOR SETTINGS TESTS ====================

    @Test
    @DisplayName("Visitor teleport delay default is 40 ticks (2 seconds)")
    void testVisitorTeleportDelay() {
        assertEquals(40, settings.getVisitorTeleportDelay());
    }

    @Test
    @DisplayName("Visitor warning message has default value")
    void testVisitorWarningMessage() {
        assertTrue(settings.getVisitorWarningMessage().contains("switching islands"));
    }

    @Test
    @DisplayName("Visitor setters work correctly")
    void testVisitorSetters() {
        settings.setVisitorTeleportDelay(60);
        settings.setVisitorWarningMessage("&cMoving soon!");

        assertEquals(60, settings.getVisitorTeleportDelay());
        assertEquals("&cMoving soon!", settings.getVisitorWarningMessage());
    }

    // ==================== PERFORMANCE SETTINGS TESTS ====================

    @Test
    @DisplayName("Cache duration default is 60 seconds")
    void testCacheDuration() {
        assertEquals(60, settings.getCacheDuration());
    }

    @Test
    @DisplayName("Max concurrent operations default is 2")
    void testMaxConcurrentOperations() {
        assertEquals(2, settings.getMaxConcurrentOperations());
    }

    @Test
    @DisplayName("Performance setters work correctly")
    void testPerformanceSetters() {
        settings.setCacheDuration(120);
        settings.setMaxConcurrentOperations(4);

        assertEquals(120, settings.getCacheDuration());
        assertEquals(4, settings.getMaxConcurrentOperations());
    }

    // ==================== INTEGRATION SETTINGS TESTS ====================

    @Test
    @DisplayName("Vault integration enabled by default")
    void testVaultEnabled() {
        assertTrue(settings.isVaultEnabled());
    }

    @Test
    @DisplayName("PlaceholderAPI integration enabled by default")
    void testPlaceholderAPIEnabled() {
        assertTrue(settings.isPlaceholderAPIEnabled());
    }

    @Test
    @DisplayName("Level addon integration enabled by default")
    void testLevelAddonEnabled() {
        assertTrue(settings.isLevelAddonEnabled());
    }

    @Test
    @DisplayName("Integration setters work correctly")
    void testIntegrationSetters() {
        settings.setVaultEnabled(false);
        settings.setPlaceholderAPIEnabled(false);
        settings.setLevelAddonEnabled(false);

        assertFalse(settings.isVaultEnabled());
        assertFalse(settings.isPlaceholderAPIEnabled());
        assertFalse(settings.isLevelAddonEnabled());
    }

    // ==================== DEBUG SETTINGS TESTS ====================

    @Test
    @DisplayName("Debug disabled by default")
    void testDebugEnabled() {
        assertFalse(settings.isDebugEnabled());
    }

    @Test
    @DisplayName("Log schematics disabled by default")
    void testLogSchematics() {
        assertFalse(settings.isLogSchematics());
    }

    @Test
    @DisplayName("Log GUI disabled by default")
    void testLogGUI() {
        assertFalse(settings.isLogGUI());
    }

    @Test
    @DisplayName("Debug setters work correctly")
    void testDebugSetters() {
        settings.setDebugEnabled(true);
        settings.setLogSchematics(true);
        settings.setLogGUI(true);

        assertTrue(settings.isDebugEnabled());
        assertTrue(settings.isLogSchematics());
        assertTrue(settings.isLogGUI());
    }

    // ==================== BOUNDARY CONDITION TESTS ====================

    @Test
    @DisplayName("Grid can handle large positive values")
    void testLargePositiveGrid() {
        settings.setGridMinX(0);
        settings.setGridMaxX(1000);
        settings.setGridMinZ(0);
        settings.setGridMaxZ(1000);

        assertEquals(1001, settings.getGridWidth());
        assertEquals(1001, settings.getGridHeight());
    }

    @Test
    @DisplayName("Grid can handle large negative values")
    void testLargeNegativeGrid() {
        settings.setGridMinX(-1000);
        settings.setGridMaxX(-1);
        settings.setGridMinZ(-1000);
        settings.setGridMaxZ(-1);

        assertEquals(1000, settings.getGridWidth());
        assertEquals(1000, settings.getGridHeight());
    }

    @Test
    @DisplayName("Single cell grid (min equals max)")
    void testSingleCellGrid() {
        settings.setGridMinX(0);
        settings.setGridMaxX(0);
        settings.setGridMinZ(0);
        settings.setGridMaxZ(0);

        assertEquals(1, settings.getGridWidth());
        assertEquals(1, settings.getGridHeight());
    }

    @Test
    @DisplayName("Zero cooldown is valid")
    void testZeroCooldown() {
        settings.setSwitchCooldown(0);
        settings.setRelocationCooldown(0);

        assertEquals(0, settings.getSwitchCooldown());
        assertEquals(0, settings.getRelocationCooldown());
    }

    @Test
    @DisplayName("Zero cost is valid")
    void testZeroCost() {
        settings.setRelocationCost(0.0);
        assertEquals(0.0, settings.getRelocationCost(), 0.001);
    }

    @Test
    @DisplayName("Large cost values work")
    void testLargeCost() {
        settings.setRelocationCost(1000000.50);
        assertEquals(1000000.50, settings.getRelocationCost(), 0.001);
    }
}
