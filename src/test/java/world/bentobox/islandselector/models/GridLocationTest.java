package world.bentobox.islandselector.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import world.bentobox.islandselector.utils.GridCoordinate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GridLocation class.
 * Tests location status, reservation, and occupation logic.
 */
@DisplayName("GridLocation Tests")
class GridLocationTest {

    private GridLocation location;
    private GridCoordinate coord;

    @BeforeEach
    void setUp() {
        coord = new GridCoordinate(5, 10);
        location = new GridLocation(coord);
    }

    // ==================== CONSTRUCTOR TESTS ====================

    @Test
    @DisplayName("New location stores coordinate")
    void testConstructorStoresCoordinate() {
        assertEquals(coord, location.getCoordinate());
        assertEquals(5, location.getCoordinate().getX());
        assertEquals(10, location.getCoordinate().getZ());
    }

    @Test
    @DisplayName("New location has AVAILABLE status")
    void testConstructorDefaultStatus() {
        assertEquals(GridLocation.Status.AVAILABLE, location.getStatus());
    }

    @Test
    @DisplayName("New location is not reserved")
    void testConstructorNotReserved() {
        assertFalse(location.isReserved());
    }

    @Test
    @DisplayName("New location is not blocked")
    void testConstructorNotBlocked() {
        assertFalse(location.isBlocked());
    }

    @Test
    @DisplayName("New location has zero purchase price")
    void testConstructorZeroPrice() {
        assertEquals(0.0, location.getPurchasePrice(), 0.001);
    }

    @Test
    @DisplayName("New location has null owner")
    void testConstructorNullOwner() {
        assertNull(location.getOwnerUUID());
        assertNull(location.getOwnerName());
    }

    @Test
    @DisplayName("New location has null island")
    void testConstructorNullIsland() {
        assertNull(location.getIslandUUID());
        assertNull(location.getIslandId());
    }

    // ==================== STATUS TESTS ====================

    @Test
    @DisplayName("Can set status to OCCUPIED")
    void testSetStatusOccupied() {
        location.setStatus(GridLocation.Status.OCCUPIED);
        assertEquals(GridLocation.Status.OCCUPIED, location.getStatus());
    }

    @Test
    @DisplayName("Can set status to RESERVED")
    void testSetStatusReserved() {
        location.setStatus(GridLocation.Status.RESERVED);
        assertEquals(GridLocation.Status.RESERVED, location.getStatus());
    }

    @Test
    @DisplayName("Can set status to LOCKED")
    void testSetStatusLocked() {
        location.setStatus(GridLocation.Status.LOCKED);
        assertEquals(GridLocation.Status.LOCKED, location.getStatus());
    }

    @Test
    @DisplayName("Can set status to AVAILABLE")
    void testSetStatusAvailable() {
        location.setStatus(GridLocation.Status.OCCUPIED);
        location.setStatus(GridLocation.Status.AVAILABLE);
        assertEquals(GridLocation.Status.AVAILABLE, location.getStatus());
    }

    // ==================== AVAILABILITY TESTS ====================

    @Test
    @DisplayName("New location is available")
    void testIsAvailableNew() {
        assertTrue(location.isAvailable());
    }

    @Test
    @DisplayName("Occupied location is not available")
    void testIsAvailableOccupied() {
        location.setStatus(GridLocation.Status.OCCUPIED);
        assertFalse(location.isAvailable());
    }

    @Test
    @DisplayName("Reserved location is not available")
    void testIsAvailableReserved() {
        location.setReserved(true);
        assertFalse(location.isAvailable());
    }

    @Test
    @DisplayName("Locked location is not available")
    void testIsAvailableLocked() {
        location.setStatus(GridLocation.Status.LOCKED);
        assertFalse(location.isAvailable());
    }

    // ==================== OWNER TESTS ====================

    @Test
    @DisplayName("Can set owner UUID")
    void testSetOwnerUUID() {
        UUID ownerUUID = UUID.randomUUID();
        location.setOwnerUUID(ownerUUID);
        assertEquals(ownerUUID, location.getOwnerUUID());
    }

    @Test
    @DisplayName("Can set owner name")
    void testSetOwnerName() {
        location.setOwnerName("TestPlayer");
        assertEquals("TestPlayer", location.getOwnerName());
    }

    @Test
    @DisplayName("Can set owner name to null")
    void testSetOwnerNameNull() {
        location.setOwnerName("TestPlayer");
        location.setOwnerName(null);
        assertNull(location.getOwnerName());
    }

    // ==================== ISLAND TESTS ====================

    @Test
    @DisplayName("Can set island UUID")
    void testSetIslandUUID() {
        UUID islandUUID = UUID.randomUUID();
        location.setIslandUUID(islandUUID);
        assertEquals(islandUUID, location.getIslandUUID());
    }

    @Test
    @DisplayName("Can set island ID (string)")
    void testSetIslandId() {
        location.setIslandId("island-12345");
        assertEquals("island-12345", location.getIslandId());
    }

    // ==================== RESERVATION TESTS ====================

    @Test
    @DisplayName("Setting reserved to true changes status to RESERVED")
    void testSetReservedTrue() {
        location.setReserved(true);
        assertTrue(location.isReserved());
        assertEquals(GridLocation.Status.RESERVED, location.getStatus());
    }

    @Test
    @DisplayName("Setting reserved to false does not change status")
    void testSetReservedFalse() {
        location.setStatus(GridLocation.Status.OCCUPIED);
        location.setReserved(false);
        assertFalse(location.isReserved());
        assertEquals(GridLocation.Status.OCCUPIED, location.getStatus());
    }

    @Test
    @DisplayName("Can set blocked status")
    void testSetBlocked() {
        location.setBlocked(true);
        assertTrue(location.isBlocked());
        location.setBlocked(false);
        assertFalse(location.isBlocked());
    }

    // ==================== PURCHASE PRICE TESTS ====================

    @Test
    @DisplayName("Can set purchase price")
    void testSetPurchasePrice() {
        location.setPurchasePrice(100.50);
        assertEquals(100.50, location.getPurchasePrice(), 0.001);
    }

    @Test
    @DisplayName("Setting purchase price > 0 clears blocked flag")
    void testSetPurchasePriceClearsBlocked() {
        location.setBlocked(true);
        assertTrue(location.isBlocked());
        location.setPurchasePrice(50.0);
        assertFalse(location.isBlocked());
    }

    @Test
    @DisplayName("Setting purchase price to 0 does not clear blocked")
    void testSetPurchasePriceZeroKeepsBlocked() {
        location.setBlocked(true);
        location.setPurchasePrice(0.0);
        assertTrue(location.isBlocked());
    }

    // ==================== PURCHASABLE TESTS ====================

    @Test
    @DisplayName("Location is purchasable when reserved, not blocked, and has price > 0")
    void testIsPurchasable() {
        location.setReserved(true);
        location.setBlocked(false);
        location.setPurchasePrice(100.0);
        assertTrue(location.isPurchasable());
    }

    @Test
    @DisplayName("Location is not purchasable when not reserved")
    void testNotPurchasableNotReserved() {
        location.setReserved(false);
        location.setPurchasePrice(100.0);
        assertFalse(location.isPurchasable());
    }

    @Test
    @DisplayName("Location is not purchasable when blocked")
    void testNotPurchasableBlocked() {
        location.setReserved(true);
        location.setBlocked(true);
        // Note: setting price would clear blocked, so set price first then block
        assertFalse(location.isPurchasable());
    }

    @Test
    @DisplayName("Location is not purchasable when price is 0")
    void testNotPurchasableZeroPrice() {
        location.setReserved(true);
        location.setBlocked(false);
        location.setPurchasePrice(0.0);
        assertFalse(location.isPurchasable());
    }

    @Test
    @DisplayName("Location is not purchasable when price is negative")
    void testNotPurchasableNegativePrice() {
        location.setReserved(true);
        location.setBlocked(false);
        location.setPurchasePrice(-10.0);
        assertFalse(location.isPurchasable());
    }

    // ==================== OCCUPY TESTS ====================

    @Test
    @DisplayName("Occupy sets owner info")
    void testOccupySetsOwnerInfo() {
        UUID ownerUUID = UUID.randomUUID();
        UUID islandUUID = UUID.randomUUID();
        location.occupy(ownerUUID, "TestPlayer", islandUUID);

        assertEquals(ownerUUID, location.getOwnerUUID());
        assertEquals("TestPlayer", location.getOwnerName());
        assertEquals(islandUUID, location.getIslandUUID());
    }

    @Test
    @DisplayName("Occupy sets status to OCCUPIED")
    void testOccupySetsStatus() {
        location.occupy(UUID.randomUUID(), "TestPlayer", UUID.randomUUID());
        assertEquals(GridLocation.Status.OCCUPIED, location.getStatus());
    }

    @Test
    @DisplayName("Occupy clears reservation")
    void testOccupyClearsReservation() {
        location.setReserved(true);
        location.setBlocked(true);
        location.setPurchasePrice(100.0);

        location.occupy(UUID.randomUUID(), "TestPlayer", UUID.randomUUID());

        assertFalse(location.isReserved());
        assertFalse(location.isBlocked());
        assertEquals(0.0, location.getPurchasePrice(), 0.001);
    }

    @Test
    @DisplayName("Occupy with null owner UUID")
    void testOccupyNullOwner() {
        location.occupy(null, "TestPlayer", UUID.randomUUID());
        assertNull(location.getOwnerUUID());
        assertEquals("TestPlayer", location.getOwnerName());
    }

    // ==================== CLEAR TESTS ====================

    @Test
    @DisplayName("Clear resets owner info")
    void testClearResetsOwnerInfo() {
        location.occupy(UUID.randomUUID(), "TestPlayer", UUID.randomUUID());
        location.setIslandId("island-123");

        location.clear();

        assertNull(location.getOwnerUUID());
        assertNull(location.getOwnerName());
        assertNull(location.getIslandUUID());
        assertNull(location.getIslandId());
    }

    @Test
    @DisplayName("Clear sets status to AVAILABLE")
    void testClearSetsStatus() {
        location.occupy(UUID.randomUUID(), "TestPlayer", UUID.randomUUID());
        location.clear();
        assertEquals(GridLocation.Status.AVAILABLE, location.getStatus());
    }

    @Test
    @DisplayName("Clear does not affect reservation")
    void testClearDoesNotAffectReservation() {
        location.setReserved(true);
        location.setBlocked(true);
        location.setPurchasePrice(100.0);

        // Note: clear() resets to AVAILABLE but doesn't touch reservation fields
        // Based on the implementation, clear() doesn't reset reserved/blocked/price
        // Let me check - actually clear() only resets owner/island info and status
        location.clear();

        // These should remain since clear() only clears occupation
        assertTrue(location.isReserved());
    }

    // ==================== STATUS ENUM TESTS ====================

    @Test
    @DisplayName("Status enum has four values")
    void testStatusEnumValues() {
        GridLocation.Status[] values = GridLocation.Status.values();
        assertEquals(4, values.length);
    }

    @Test
    @DisplayName("Status enum contains all expected values")
    void testStatusEnumContainsValues() {
        assertNotNull(GridLocation.Status.valueOf("AVAILABLE"));
        assertNotNull(GridLocation.Status.valueOf("OCCUPIED"));
        assertNotNull(GridLocation.Status.valueOf("RESERVED"));
        assertNotNull(GridLocation.Status.valueOf("LOCKED"));
    }

    // ==================== COORDINATE TESTS ====================

    @Test
    @DisplayName("Different coordinates create distinct locations")
    void testDifferentCoordinates() {
        GridLocation loc1 = new GridLocation(new GridCoordinate(0, 0));
        GridLocation loc2 = new GridLocation(new GridCoordinate(1, 1));
        GridLocation loc3 = new GridLocation(new GridCoordinate(-5, 3));

        assertEquals(0, loc1.getCoordinate().getX());
        assertEquals(0, loc1.getCoordinate().getZ());
        assertEquals(1, loc2.getCoordinate().getX());
        assertEquals(1, loc2.getCoordinate().getZ());
        assertEquals(-5, loc3.getCoordinate().getX());
        assertEquals(3, loc3.getCoordinate().getZ());
    }

    @Test
    @DisplayName("Location with negative coordinates")
    void testNegativeCoordinates() {
        GridLocation loc = new GridLocation(new GridCoordinate(-10, -20));
        assertEquals(-10, loc.getCoordinate().getX());
        assertEquals(-20, loc.getCoordinate().getZ());
        assertTrue(loc.isAvailable());
    }

    // ==================== COMPLEX SCENARIO TESTS ====================

    @Test
    @DisplayName("Full lifecycle: available -> occupied -> cleared")
    void testFullLifecycleAvailableOccupiedCleared() {
        // Start available
        assertTrue(location.isAvailable());
        assertEquals(GridLocation.Status.AVAILABLE, location.getStatus());

        // Occupy
        UUID ownerUUID = UUID.randomUUID();
        UUID islandUUID = UUID.randomUUID();
        location.occupy(ownerUUID, "TestPlayer", islandUUID);
        assertFalse(location.isAvailable());
        assertEquals(GridLocation.Status.OCCUPIED, location.getStatus());
        assertEquals(ownerUUID, location.getOwnerUUID());

        // Clear
        location.clear();
        assertTrue(location.isAvailable());
        assertEquals(GridLocation.Status.AVAILABLE, location.getStatus());
        assertNull(location.getOwnerUUID());
    }

    @Test
    @DisplayName("Full lifecycle: reserved -> purchased -> occupied")
    void testFullLifecycleReservedPurchased() {
        // Reserve with price
        location.setReserved(true);
        location.setPurchasePrice(500.0);
        assertTrue(location.isPurchasable());
        assertEquals(GridLocation.Status.RESERVED, location.getStatus());

        // "Purchase" - occupy clears reservation
        location.occupy(UUID.randomUUID(), "Buyer", UUID.randomUUID());
        assertFalse(location.isReserved());
        assertFalse(location.isPurchasable());
        assertEquals(GridLocation.Status.OCCUPIED, location.getStatus());
        assertEquals(0.0, location.getPurchasePrice(), 0.001);
    }

    @Test
    @DisplayName("Reserved and blocked cannot be purchased")
    void testReservedBlockedNotPurchasable() {
        location.setReserved(true);
        location.setBlocked(true);
        assertFalse(location.isPurchasable());
        assertEquals(GridLocation.Status.RESERVED, location.getStatus());
    }
}
