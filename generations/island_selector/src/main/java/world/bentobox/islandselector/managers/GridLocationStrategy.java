package world.bentobox.islandselector.managers;

import org.bukkit.Location;
import org.bukkit.World;

import world.bentobox.bentobox.managers.island.NewIslandLocationStrategy;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Custom location strategy that returns the grid location selected by the player.
 * This ensures the island is created at the correct grid position from the start.
 */
public class GridLocationStrategy implements NewIslandLocationStrategy {

    private final IslandSelector addon;
    private final GridCoordinate coordinate;
    private final World world;

    /**
     * Create a location strategy for a specific grid coordinate
     * @param addon The IslandSelector addon
     * @param coordinate The grid coordinate where the island should be created
     * @param world The world to create the island in
     */
    public GridLocationStrategy(IslandSelector addon, GridCoordinate coordinate, World world) {
        this.addon = addon;
        this.coordinate = coordinate;
        this.world = world;
    }

    @Override
    public Location getNextLocation(World world) {
        return calculateLocation(world);
    }

    /**
     * Calculate the world location for the grid coordinate
     */
    private Location calculateLocation(World targetWorld) {
        // BSkyBlock's "distance-between-islands" is the offset from center
        // Real spacing between island centers is distance * 2
        int spacing = addon.getIslandSpacing() * 2;

        int worldX = coordinate.getX() * spacing;
        int worldZ = coordinate.getZ() * spacing;

        // Use the provided world, or fall back to our stored world
        World w = targetWorld != null ? targetWorld : this.world;

        addon.log("GridLocationStrategy returning location: " + worldX + ", " + worldZ +
                 " for grid " + coordinate);

        // Y coordinate is typically at sea level or configured height
        // BentoBox will adjust this based on the blueprint
        return new Location(w, worldX, 64, worldZ);
    }

    /**
     * Get the grid coordinate this strategy will use
     */
    public GridCoordinate getCoordinate() {
        return coordinate;
    }
}
