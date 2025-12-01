package world.bentobox.islandselector.utils;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a coordinate on the island grid (e.g., A1, B7, AA12)
 * Column is the letter part (A = 0, B = 1, etc.)
 * Row is the number part (1 = 0, 2 = 1, etc.)
 */
public class GridCoordinate {

    // Pattern for parsing coordinates like A1, B7, AA12, etc.
    private static final Pattern COORD_PATTERN = Pattern.compile("^([A-Z]+)(\\d+)$");

    private final int column;  // 0-indexed (A = 0, B = 1, etc.)
    private final int row;     // 0-indexed (1 = 0, 2 = 1, etc.)

    public GridCoordinate(int column, int row) {
        if (column < 0 || row < 0) {
            throw new IllegalArgumentException("Column and row must be non-negative");
        }
        this.column = column;
        this.row = row;
    }

    /**
     * Parse a coordinate string like "A1", "B7", "AA12"
     * @param coordStr The coordinate string
     * @return GridCoordinate or null if invalid
     */
    public static GridCoordinate parse(String coordStr) {
        if (coordStr == null || coordStr.isEmpty()) {
            return null;
        }

        Matcher matcher = COORD_PATTERN.matcher(coordStr.toUpperCase().trim());
        if (!matcher.matches()) {
            return null;
        }

        String columnStr = matcher.group(1);
        String rowStr = matcher.group(2);

        try {
            int column = lettersToColumn(columnStr);
            int row = Integer.parseInt(rowStr) - 1; // Convert 1-indexed to 0-indexed

            if (row < 0) {
                return null;
            }

            return new GridCoordinate(column, row);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Convert letter(s) to column index (A = 0, B = 1, ..., Z = 25, AA = 26, etc.)
     */
    private static int lettersToColumn(String letters) {
        int result = 0;
        for (int i = 0; i < letters.length(); i++) {
            result = result * 26 + (letters.charAt(i) - 'A' + 1);
        }
        return result - 1; // Make 0-indexed
    }

    /**
     * Convert column index to letter(s) (0 = A, 1 = B, ..., 25 = Z, 26 = AA, etc.)
     */
    private static String columnToLetters(int column) {
        StringBuilder result = new StringBuilder();
        column++; // Make 1-indexed for calculation
        while (column > 0) {
            column--;
            result.insert(0, (char) ('A' + (column % 26)));
            column /= 26;
        }
        return result.toString();
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    /**
     * Get the column as letters (A, B, ..., Z, AA, AB, etc.)
     */
    public String getColumnLetters() {
        return columnToLetters(column);
    }

    /**
     * Get the row as a 1-indexed number for display
     */
    public int getDisplayRow() {
        return row + 1;
    }

    /**
     * Get the coordinate string (e.g., "A1", "B7", "AA12")
     */
    @Override
    public String toString() {
        return getColumnLetters() + getDisplayRow();
    }

    /**
     * Get a coordinate offset from this one
     */
    public GridCoordinate offset(int columnOffset, int rowOffset) {
        int newColumn = column + columnOffset;
        int newRow = row + rowOffset;
        if (newColumn < 0 || newRow < 0) {
            return null;
        }
        return new GridCoordinate(newColumn, newRow);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GridCoordinate that = (GridCoordinate) o;
        return column == that.column && row == that.row;
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, row);
    }
}
