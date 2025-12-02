package world.bentobox.islandselector.utils;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a coordinate on the island grid with (0,0) at the center.
 * Supports negative coordinates in all directions.
 * Display format: "X,Z" (e.g., "0,0", "-5,3", "10,-7")
 */
public class GridCoordinate {

    // Pattern for parsing coordinates like "0,0", "-5,3", "10,-7"
    private static final Pattern COORD_PATTERN = Pattern.compile("^(-?\\d+),(-?\\d+)$");

    private final int x;  // Column (positive = east, negative = west)
    private final int z;  // Row (positive = south, negative = north)

    public GridCoordinate(int x, int z) {
        this.x = x;
        this.z = z;
    }

    /**
     * Parse a coordinate string like "0,0", "-5,3", "10,-7"
     * @param coordStr The coordinate string
     * @return GridCoordinate or null if invalid
     */
    public static GridCoordinate parse(String coordStr) {
        if (coordStr == null || coordStr.isEmpty()) {
            return null;
        }

        // Remove spaces
        String cleaned = coordStr.replaceAll("\\s+", "");

        Matcher matcher = COORD_PATTERN.matcher(cleaned);
        if (!matcher.matches()) {
            return null;
        }

        try {
            int x = Integer.parseInt(matcher.group(1));
            int z = Integer.parseInt(matcher.group(2));
            return new GridCoordinate(x, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get the X coordinate (column)
     */
    public int getX() {
        return x;
    }

    /**
     * Get the Z coordinate (row)
     */
    public int getZ() {
        return z;
    }

    /**
     * Legacy method - returns X for compatibility
     */
    public int getColumn() {
        return x;
    }

    /**
     * Legacy method - returns Z for compatibility
     */
    public int getRow() {
        return z;
    }

    /**
     * Get the coordinate string (e.g., "0,0", "-5,3")
     */
    @Override
    public String toString() {
        return x + "," + z;
    }

    /**
     * Get a coordinate offset from this one
     */
    public GridCoordinate offset(int xOffset, int zOffset) {
        return new GridCoordinate(x + xOffset, z + zOffset);
    }

    /**
     * Calculate distance to another coordinate
     */
    public double distanceTo(GridCoordinate other) {
        int dx = other.x - this.x;
        int dz = other.z - this.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Calculate Manhattan distance to another coordinate
     */
    public int manhattanDistanceTo(GridCoordinate other) {
        return Math.abs(other.x - this.x) + Math.abs(other.z - this.z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GridCoordinate that = (GridCoordinate) o;
        return x == that.x && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
}
