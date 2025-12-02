package world.bentobox.islandselector.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Event fired when a player claims a grid location for their new island.
 * This event is called AFTER the player confirms their location selection
 * but BEFORE the island is actually created by BSkyBlock.
 *
 * Cancelling this event will prevent the island from being created at the selected location.
 */
public class GridLocationClaimEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final GridCoordinate gridCoordinate;
    private final int worldX;
    private final int worldZ;
    private String cancellationReason;

    /**
     * Create a new GridLocationClaimEvent
     *
     * @param player The player claiming the location
     * @param gridCoordinate The grid coordinate being claimed (e.g., "A1", "B5")
     * @param worldX The actual world X coordinate
     * @param worldZ The actual world Z coordinate
     */
    public GridLocationClaimEvent(Player player, GridCoordinate gridCoordinate, int worldX, int worldZ) {
        this.player = player;
        this.gridCoordinate = gridCoordinate;
        this.worldX = worldX;
        this.worldZ = worldZ;
    }

    /**
     * Get the player claiming the location
     *
     * @return The player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Get the grid coordinate being claimed
     *
     * @return The grid coordinate (e.g., "A1", "B5")
     */
    public GridCoordinate getGridCoordinate() {
        return gridCoordinate;
    }

    /**
     * Get the grid coordinate as a string
     *
     * @return The grid coordinate string (e.g., "A1", "B5")
     */
    public String getGridCoordinateString() {
        return gridCoordinate.toString();
    }

    /**
     * Get the world X coordinate where the island will be created
     *
     * @return The world X coordinate
     */
    public int getWorldX() {
        return worldX;
    }

    /**
     * Get the world Z coordinate where the island will be created
     *
     * @return The world Z coordinate
     */
    public int getWorldZ() {
        return worldZ;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Set a cancellation reason that can be displayed to the player
     *
     * @param reason The reason for cancellation
     */
    public void setCancellationReason(String reason) {
        this.cancellationReason = reason;
        this.cancelled = true;
    }

    /**
     * Get the cancellation reason (if any)
     *
     * @return The cancellation reason, or null if not set
     */
    public String getCancellationReason() {
        return cancellationReason;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
