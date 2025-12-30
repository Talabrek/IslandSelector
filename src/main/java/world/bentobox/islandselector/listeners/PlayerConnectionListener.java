package world.bentobox.islandselector.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import world.bentobox.islandselector.IslandSelector;
import world.bentobox.islandselector.database.SlotData;

/**
 * Listener for player connection events to manage blueprint permissions.
 *
 * When a player joins, their active slot's blueprint permissions are applied.
 * When a player quits, their permission attachments are cleaned up.
 */
public class PlayerConnectionListener implements Listener {

    private final IslandSelector addon;

    public PlayerConnectionListener(IslandSelector addon) {
        this.addon = addon;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if player has slots
        SlotData activeSlot = addon.getSlotManager().getActiveSlot(player.getUniqueId());
        if (activeSlot == null) {
            return;
        }

        // Apply blueprint permissions for the active slot
        String blueprintName = activeSlot.getBlueprintBundle();
        if (blueprintName != null && !blueprintName.isEmpty()) {
            addon.getBlueprintChallengesManager().updateBlueprintPermissions(player, blueprintName);
            addon.log("Applied blueprint permissions '" + blueprintName + "' for " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Clean up permission attachments
        addon.getBlueprintChallengesManager().onPlayerQuit(player);
    }
}
