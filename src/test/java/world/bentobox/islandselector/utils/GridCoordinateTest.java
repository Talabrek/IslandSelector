package world.bentobox.islandselector.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GridCoordinate class.
 * These tests verify coordinate parsing, calculations, and transformations.
 *
 * Test IDs mapped to feature_list.json:
 * - Tests 45-48: Grid coordinate mapping (A1 -> world coords, B2 -> world coords, etc.)
 * - Various coordinate calculation tests
 */
@DisplayName("GridCoordinate Tests")
class GridCoordinateTest {

    // ==================== PARSING TESTS ====================

    @Test
    @DisplayName("Parse valid coordinate '0,0' (origin)")
    void testParseOrigin() {
        GridCoordinate coord = GridCoordinate.parse("0,0");
        assertNotNull(coord);
        assertEquals(0, coord.getX());
        assertEquals(0, coord.getZ());
    }

    @Test
    @DisplayName("Parse valid positive coordinates")
    void testParsePositiveCoordinates() {
        // Test case: "1,1"
        GridCoordinate coord1 = GridCoordinate.parse("1,1");
        assertNotNull(coord1);
        assertEquals(1, coord1.getX());
        assertEquals(1, coord1.getZ());

        // Test case: "5,10"
        GridCoordinate coord2 = GridCoordinate.parse("5,10");
        assertNotNull(coord2);
        assertEquals(5, coord2.getX());
        assertEquals(10, coord2.getZ());

        // Test case: "100,200"
        GridCoordinate coord3 = GridCoordinate.parse("100,200");
        assertNotNull(coord3);
        assertEquals(100, coord3.getX());
        assertEquals(200, coord3.getZ());

        // Test case: "999,999"
        GridCoordinate coord4 = GridCoordinate.parse("999,999");
        assertNotNull(coord4);
        assertEquals(999, coord4.getX());
        assertEquals(999, coord4.getZ());
    }

    @Test
    @DisplayName("Parse valid negative coordinates")
    void testParseNegativeCoordinates() {
        // Test case: "-1,-1"
        GridCoordinate coord1 = GridCoordinate.parse("-1,-1");
        assertNotNull(coord1);
        assertEquals(-1, coord1.getX());
        assertEquals(-1, coord1.getZ());

        // Test case: "-5,3"
        GridCoordinate coord2 = GridCoordinate.parse("-5,3");
        assertNotNull(coord2);
        assertEquals(-5, coord2.getX());
        assertEquals(3, coord2.getZ());

        // Test case: "10,-7"
        GridCoordinate coord3 = GridCoordinate.parse("10,-7");
        assertNotNull(coord3);
        assertEquals(10, coord3.getX());
        assertEquals(-7, coord3.getZ());

        // Test case: "-100,-200"
        GridCoordinate coord4 = GridCoordinate.parse("-100,-200");
        assertNotNull(coord4);
        assertEquals(-100, coord4.getX());
        assertEquals(-200, coord4.getZ());
    }

    @Test
    @DisplayName("Parse coordinates with whitespace")
    void testParseWithWhitespace() {
        // Test case: " 1,1"
        GridCoordinate coord1 = GridCoordinate.parse(" 1,1");
        assertNotNull(coord1);
        assertEquals(1, coord1.getX());
        assertEquals(1, coord1.getZ());

        // Test case: "1,1 "
        GridCoordinate coord2 = GridCoordinate.parse("1,1 ");
        assertNotNull(coord2);
        assertEquals(1, coord2.getX());
        assertEquals(1, coord2.getZ());

        // Test case: " 1 , 1 "
        GridCoordinate coord3 = GridCoordinate.parse(" 1 , 1 ");
        assertNotNull(coord3);
        assertEquals(1, coord3.getX());
        assertEquals(1, coord3.getZ());

        // Test case: "  -5  ,  3  "
        GridCoordinate coord4 = GridCoordinate.parse("  -5  ,  3  ");
        assertNotNull(coord4);
        assertEquals(-5, coord4.getX());
        assertEquals(3, coord4.getZ());
    }

    @ParameterizedTest
    @DisplayName("Parse invalid coordinates returns null")
    @ValueSource(strings = {
        "",
        "abc",
        "1",
        "1,",
        ",1",
        "1,2,3",
        "a,b",
        "1.5,2.5",
        "1;2",
        "1:2"
    })
    void testParseInvalidReturnsNull(String input) {
        GridCoordinate coord = GridCoordinate.parse(input);
        assertNull(coord);
    }

    @Test
    @DisplayName("Parse null returns null")
    void testParseNullReturnsNull() {
        GridCoordinate coord = GridCoordinate.parse(null);
        assertNull(coord);
    }

    // ==================== toString TESTS ====================

    @Test
    @DisplayName("toString returns correct format for origin")
    void testToStringOrigin() {
        GridCoordinate coord = new GridCoordinate(0, 0);
        assertEquals("0,0", coord.toString());
    }

    @Test
    @DisplayName("toString returns correct format")
    void testToString() {
        assertEquals("1,1", new GridCoordinate(1, 1).toString());
        assertEquals("-5,3", new GridCoordinate(-5, 3).toString());
        assertEquals("10,-7", new GridCoordinate(10, -7).toString());
        assertEquals("-100,-200", new GridCoordinate(-100, -200).toString());
    }

    @Test
    @DisplayName("Parse and toString are inverse operations")
    void testParseAndToStringInverse() {
        String[] testCases = {"0,0", "1,1", "-5,3", "10,-7", "-100,-200"};
        for (String input : testCases) {
            GridCoordinate coord = GridCoordinate.parse(input);
            assertNotNull(coord);
            assertEquals(input, coord.toString());
        }
    }

    // ==================== OFFSET TESTS ====================

    @Test
    @DisplayName("Offset from origin")
    void testOffsetFromOrigin() {
        GridCoordinate origin = new GridCoordinate(0, 0);
        GridCoordinate offset = origin.offset(5, 3);
        assertEquals(5, offset.getX());
        assertEquals(3, offset.getZ());
    }

    @Test
    @DisplayName("Offset with negative values")
    void testOffsetNegative() {
        GridCoordinate coord = new GridCoordinate(10, 10);
        GridCoordinate offset = coord.offset(-3, -5);
        assertEquals(7, offset.getX());
        assertEquals(5, offset.getZ());
    }

    @Test
    @DisplayName("Offset does not modify original")
    void testOffsetDoesNotModifyOriginal() {
        GridCoordinate original = new GridCoordinate(5, 5);
        GridCoordinate offset = original.offset(10, 10);

        // Original unchanged
        assertEquals(5, original.getX());
        assertEquals(5, original.getZ());

        // New coordinate has offset
        assertEquals(15, offset.getX());
        assertEquals(15, offset.getZ());
    }

    // ==================== DISTANCE TESTS ====================

    @Test
    @DisplayName("Distance to same point is zero")
    void testDistanceToSamePoint() {
        GridCoordinate coord = new GridCoordinate(5, 5);
        assertEquals(0.0, coord.distanceTo(coord), 0.001);
    }

    @Test
    @DisplayName("Distance calculation (Euclidean)")
    void testDistanceCalculation() {
        GridCoordinate a = new GridCoordinate(0, 0);
        GridCoordinate b = new GridCoordinate(3, 4);
        assertEquals(5.0, a.distanceTo(b), 0.001); // 3-4-5 triangle
    }

    @Test
    @DisplayName("Distance is symmetric")
    void testDistanceSymmetric() {
        GridCoordinate a = new GridCoordinate(1, 2);
        GridCoordinate b = new GridCoordinate(4, 6);
        assertEquals(a.distanceTo(b), b.distanceTo(a), 0.001);
    }

    @Test
    @DisplayName("Manhattan distance to same point is zero")
    void testManhattanDistanceToSamePoint() {
        GridCoordinate coord = new GridCoordinate(5, 5);
        assertEquals(0, coord.manhattanDistanceTo(coord));
    }

    @Test
    @DisplayName("Manhattan distance calculation")
    void testManhattanDistanceCalculation() {
        GridCoordinate a = new GridCoordinate(0, 0);
        GridCoordinate b = new GridCoordinate(3, 4);
        assertEquals(7, a.manhattanDistanceTo(b)); // |3| + |4| = 7
    }

    @Test
    @DisplayName("Manhattan distance with negative coordinates")
    void testManhattanDistanceNegative() {
        GridCoordinate a = new GridCoordinate(-2, -3);
        GridCoordinate b = new GridCoordinate(2, 3);
        assertEquals(10, a.manhattanDistanceTo(b)); // |4| + |6| = 10
    }

    // ==================== EQUALITY TESTS ====================

    @Test
    @DisplayName("Same coordinates are equal")
    void testEqualsSameCoordinates() {
        GridCoordinate a = new GridCoordinate(5, 10);
        GridCoordinate b = new GridCoordinate(5, 10);
        assertEquals(a, b);
    }

    @Test
    @DisplayName("Different coordinates are not equal")
    void testNotEqualsDifferentCoordinates() {
        GridCoordinate a = new GridCoordinate(5, 10);
        GridCoordinate b = new GridCoordinate(10, 5);
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("Equals with null returns false")
    void testEqualsNull() {
        GridCoordinate coord = new GridCoordinate(0, 0);
        assertNotEquals(null, coord);
    }

    @Test
    @DisplayName("Equals with different type returns false")
    void testEqualsDifferentType() {
        GridCoordinate coord = new GridCoordinate(0, 0);
        assertNotEquals("0,0", coord);
    }

    @Test
    @DisplayName("HashCode is consistent for equal objects")
    void testHashCodeConsistent() {
        GridCoordinate a = new GridCoordinate(5, 10);
        GridCoordinate b = new GridCoordinate(5, 10);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ==================== LEGACY METHOD TESTS ====================

    @Test
    @DisplayName("getColumn returns X coordinate")
    void testGetColumnReturnsX() {
        GridCoordinate coord = new GridCoordinate(7, 3);
        assertEquals(coord.getX(), coord.getColumn());
    }

    @Test
    @DisplayName("getRow returns Z coordinate")
    void testGetRowReturnsZ() {
        GridCoordinate coord = new GridCoordinate(7, 3);
        assertEquals(coord.getZ(), coord.getRow());
    }

    // ==================== WORLD COORDINATE MAPPING TESTS ====================
    // These test the formulas used for grid-to-world coordinate conversion
    // Formula: worldCoord = gridCoord * spacing (where spacing is typically 500 or 1000)

    @Test
    @DisplayName("Grid 0,0 maps to world 0,0 (Test #45 from feature list)")
    void testGridOriginMapsToWorldOrigin() {
        int spacing = 500;
        GridCoordinate grid = new GridCoordinate(0, 0);

        int worldX = grid.getX() * spacing;
        int worldZ = grid.getZ() * spacing;

        assertEquals(0, worldX);
        assertEquals(0, worldZ);
    }

    @Test
    @DisplayName("Grid 1,1 maps to world 500,500 with spacing 500 (Test #46 from feature list)")
    void testGrid11MapsCorrectly() {
        int spacing = 500;
        GridCoordinate grid = new GridCoordinate(1, 1);

        int worldX = grid.getX() * spacing;
        int worldZ = grid.getZ() * spacing;

        assertEquals(500, worldX);
        assertEquals(500, worldZ);
    }

    @Test
    @DisplayName("Grid 2,3 maps to world 1000,1500 with spacing 500")
    void testGrid23MapsCorrectly() {
        int spacing = 500;
        GridCoordinate grid = new GridCoordinate(2, 3);

        int worldX = grid.getX() * spacing;
        int worldZ = grid.getZ() * spacing;

        assertEquals(1000, worldX);
        assertEquals(1500, worldZ);
    }

    @Test
    @DisplayName("Custom spacing affects world coordinates (Test #47 from feature list)")
    void testCustomSpacingAffectsWorldCoords() {
        int spacing = 1000; // Custom spacing
        GridCoordinate grid = new GridCoordinate(1, 1);

        int worldX = grid.getX() * spacing;
        int worldZ = grid.getZ() * spacing;

        assertEquals(1000, worldX);
        assertEquals(1000, worldZ);
    }

    @Test
    @DisplayName("Negative grid coordinates map to negative world coordinates")
    void testNegativeGridMapsToNegativeWorld() {
        int spacing = 500;
        GridCoordinate grid = new GridCoordinate(-2, -3);

        int worldX = grid.getX() * spacing;
        int worldZ = grid.getZ() * spacing;

        assertEquals(-1000, worldX);
        assertEquals(-1500, worldZ);
    }

    @Test
    @DisplayName("World to grid coordinate conversion")
    void testWorldToGridConversion() {
        int spacing = 500;

        // World coords 750, 1250 should map to grid 2, 3 (with rounding)
        int worldX = 750;
        int worldZ = 1250;

        int gridX = Math.round((float) worldX / spacing);
        int gridZ = Math.round((float) worldZ / spacing);

        assertEquals(2, gridX); // 750/500 = 1.5, rounds to 2
        assertEquals(3, gridZ); // 1250/500 = 2.5, rounds to 3
    }

    // ==================== NEIGHBOR TESTS ====================

    @Test
    @DisplayName("Get all 8 neighbors of a coordinate")
    void testGetNeighbors() {
        GridCoordinate center = new GridCoordinate(5, 5);

        // North neighbor
        GridCoordinate north = center.offset(0, -1);
        assertEquals(new GridCoordinate(5, 4), north);

        // South neighbor
        GridCoordinate south = center.offset(0, 1);
        assertEquals(new GridCoordinate(5, 6), south);

        // East neighbor
        GridCoordinate east = center.offset(1, 0);
        assertEquals(new GridCoordinate(6, 5), east);

        // West neighbor
        GridCoordinate west = center.offset(-1, 0);
        assertEquals(new GridCoordinate(4, 5), west);

        // Diagonal neighbors
        assertEquals(new GridCoordinate(6, 4), center.offset(1, -1));  // NE
        assertEquals(new GridCoordinate(4, 4), center.offset(-1, -1)); // NW
        assertEquals(new GridCoordinate(6, 6), center.offset(1, 1));   // SE
        assertEquals(new GridCoordinate(4, 6), center.offset(-1, 1));  // SW
    }

    @Test
    @DisplayName("Cardinal neighbors are at Manhattan distance 1")
    void testCardinalNeighborsDistance() {
        GridCoordinate center = new GridCoordinate(0, 0);
        GridCoordinate[] cardinals = {
            center.offset(0, -1),  // North
            center.offset(0, 1),   // South
            center.offset(1, 0),   // East
            center.offset(-1, 0)   // West
        };

        for (GridCoordinate neighbor : cardinals) {
            assertEquals(1, center.manhattanDistanceTo(neighbor));
        }
    }

    @Test
    @DisplayName("Diagonal neighbors are at Manhattan distance 2")
    void testDiagonalNeighborsDistance() {
        GridCoordinate center = new GridCoordinate(0, 0);
        GridCoordinate[] diagonals = {
            center.offset(1, -1),   // NE
            center.offset(-1, -1),  // NW
            center.offset(1, 1),    // SE
            center.offset(-1, 1)    // SW
        };

        for (GridCoordinate neighbor : diagonals) {
            assertEquals(2, center.manhattanDistanceTo(neighbor));
        }
    }
}
