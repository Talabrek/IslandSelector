package world.bentobox.islandselector.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a player switches from one island slot to another.
 * This event is called AFTER the player confirms the switch
 * but BEFORE the schematic save/load operations begin.
 *
 * Cancelling this event will prevent the slot switch from happening.
 */
public class SlotSwitchEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final int fromSlot;
    private final int toSlot;
    private final String fromSlotName;
    private final String toSlotName;
    private String cancellationReason;

    /**
     * Create a new SlotSwitchEvent
     *
     * @param player The player switching slots
     * @param fromSlot The current slot number (1-5)
     * @param toSlot The target slot number (1-5)
     * @param fromSlotName The name of the current slot
     * @param toSlotName The name of the target slot
     */
    public SlotSwitchEvent(Player player, int fromSlot, int toSlot, String fromSlotName, String toSlotName) {
        this.player = player;
        this.fromSlot = fromSlot;
        this.toSlot = toSlot;
        this.fromSlotName = fromSlotName;
        this.toSlotName = toSlotName;
    }

    /**
     * Get the player switching slots
     *
     * @return The player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Get the slot number being switched from
     *
     * @return The from slot number (1-5)
     */
    public int getFromSlot() {
        return fromSlot;
    }

    /**
     * Get the slot number being switched to
     *
     * @return The to slot number (1-5)
     */
    public int getToSlot() {
        return toSlot;
    }

    /**
     * Get the name of the slot being switched from
     *
     * @return The from slot name
     */
    public String getFromSlotName() {
        return fromSlotName;
    }

    /**
     * Get the name of the slot being switched to
     *
     * @return The to slot name
     */
    public String getToSlotName() {
        return toSlotName;
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
