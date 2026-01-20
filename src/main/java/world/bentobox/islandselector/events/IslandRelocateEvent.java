package world.bentobox.islandselector.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import world.bentobox.islandselector.utils.GridCoordinate;

/**
 * Event fired when a player relocates their island from one grid location to another.
 * This event is called AFTER the player confirms the relocation
 * but BEFORE the island copying and teleportation begins.
 *
 * Cancelling this event will prevent the relocation from happening.
 */
public class IslandRelocateEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final GridCoordinate fromCoordinate;
    private final GridCoordinate toCoordinate;
    private final int fromWorldX;
    private final int fromWorldZ;
    private final int toWorldX;
    private final int toWorldZ;
    private final double cost;
    private String cancellationReason;

    /**
     * Create a new IslandRelocateEvent
     *
     * @param player The player relocating their island
     * @param fromCoordinate The current grid coordinate
     * @param toCoordinate The target grid coordinate
     * @param fromWorldX The current world X coordinate
     * @param fromWorldZ The current world Z coordinate
     * @param toWorldX The target world X coordinate
     * @param toWorldZ The target world Z coordinate
     * @param cost The cost of relocation (0 if free or bypassed)
     */
    public IslandRelocateEvent(Player player, GridCoordinate fromCoordinate, GridCoordinate toCoordinate,
                               int fromWorldX, int fromWorldZ, int toWorldX, int toWorldZ, double cost) {
        this.player = player;
        this.fromCoordinate = fromCoordinate;
        this.toCoordinate = toCoordinate;
        this.fromWorldX = fromWorldX;
        this.fromWorldZ = fromWorldZ;
        this.toWorldX = toWorldX;
        this.toWorldZ = toWorldZ;
        this.cost = cost;
    }

    /**
     * Get the player relocating their island
     *
     * @return The player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Get the current grid coordinate
     *
     * @return The from grid coordinate
     */
    public GridCoordinate getFromCoordinate() {
        return fromCoordinate;
    }

    /**
     * Get the target grid coordinate
     *
     * @return The to grid coordinate
     */
    public GridCoordinate getToCoordinate() {
        return toCoordinate;
    }

    /**
     * Get the current grid coordinate as a string
     *
     * @return The from coordinate string (e.g., "A1")
     */
    public String getFromCoordinateString() {
        return fromCoordinate.toString();
    }

    /**
     * Get the target grid coordinate as a string
     *
     * @return The to coordinate string (e.g., "B5")
     */
    public String getToCoordinateString() {
        return toCoordinate.toString();
    }

    /**
     * Get the current world X coordinate
     *
     * @return The from world X
     */
    public int getFromWorldX() {
        return fromWorldX;
    }

    /**
     * Get the current world Z coordinate
     *
     * @return The from world Z
     */
    public int getFromWorldZ() {
        return fromWorldZ;
    }

    /**
     * Get the target world X coordinate
     *
     * @return The to world X
     */
    public int getToWorldX() {
        return toWorldX;
    }

    /**
     * Get the target world Z coordinate
     *
     * @return The to world Z
     */
    public int getToWorldZ() {
        return toWorldZ;
    }

    /**
     * Get the cost of this relocation
     *
     * @return The cost in economy currency, or 0 if free/bypassed
     */
    public double getCost() {
        return cost;
    }

    /**
     * Check if this relocation is free (no cost)
     *
     * @return true if cost is 0
     */
    public boolean isFree() {
        return cost == 0;
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
