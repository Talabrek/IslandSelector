package world.bentobox.islandselector.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import world.bentobox.islandselector.IslandSelector;

import java.util.*;

/**
 * Manages blueprint-specific challenge permissions.
 *
 * This allows different island blueprints to have different sets of challenges.
 * Challenges can be configured with required permissions like:
 *   islandselector.blueprint.desert
 *   islandselector.blueprint.jungle
 *   islandselector.blueprint.classic
 *
 * When a player switches blueprints (slots), their permissions are updated
 * so they can only see/complete challenges for their current blueprint.
 *
 * Setup Instructions:
 * 1. In your challenge definitions, add required permissions:
 *    requirements:
 *      required-permissions:
 *        - "islandselector.blueprint.desert"
 *
 * 2. In IslandSelector config, map blueprints to permission groups:
 *    blueprint-challenges:
 *      desert:
 *        - "islandselector.blueprint.desert"
 *      jungle:
 *        - "islandselector.blueprint.jungle"
 *      classic:
 *        - "islandselector.blueprint.classic"
 *
 * 3. When players switch to an island with blueprint "desert", they automatically
 *    get the "islandselector.blueprint.desert" permission.
 */
public class BlueprintChallengesManager {

    private final IslandSelector addon;

    // Permission prefix for blueprint challenges
    private static final String PERMISSION_PREFIX = "islandselector.blueprint.";

    // Track permission attachments per player (for cleanup)
    private final Map<UUID, PermissionAttachment> playerAttachments = new HashMap<>();

    // Blueprint to permissions mapping (loaded from config)
    private final Map<String, Set<String>> blueprintPermissions = new HashMap<>();

    public BlueprintChallengesManager(IslandSelector addon) {
        this.addon = addon;
        loadBlueprintPermissions();
    }

    /**
     * Load blueprint-to-permission mappings from config
     */
    private void loadBlueprintPermissions() {
        // Default mappings - these can be overridden in config
        // The key is the blueprint bundle name, value is the set of permissions to grant

        // Check if config section exists
        // For now, we'll use a simple naming convention:
        // Blueprint "desert" -> permission "islandselector.blueprint.desert"

        addon.log("BlueprintChallengesManager initialized");
        addon.log("Permission prefix: " + PERMISSION_PREFIX);
        addon.log("To use blueprint-specific challenges:");
        addon.log("  1. Add required-permissions to your challenges in Challenges addon");
        addon.log("  2. Use permissions like: islandselector.blueprint.<blueprint-name>");
        addon.log("  3. Players will automatically get permissions based on their island blueprint");
    }

    /**
     * Update a player's blueprint challenge permissions when they switch blueprints.
     *
     * @param player The player
     * @param blueprintName The blueprint bundle name they're switching to (can be null to clear)
     */
    public void updateBlueprintPermissions(Player player, String blueprintName) {
        UUID playerUUID = player.getUniqueId();

        // Remove any existing blueprint permissions
        clearBlueprintPermissions(player);

        if (blueprintName == null || blueprintName.isEmpty()) {
            addon.log("Cleared blueprint permissions for " + player.getName());
            return;
        }

        // Create permission attachment for this player
        PermissionAttachment attachment = player.addAttachment(addon.getPlugin());
        playerAttachments.put(playerUUID, attachment);

        // Grant the permission for this blueprint
        String permission = PERMISSION_PREFIX + blueprintName.toLowerCase();
        attachment.setPermission(permission, true);

        // Also grant any custom permissions mapped to this blueprint
        Set<String> customPerms = blueprintPermissions.get(blueprintName.toLowerCase());
        if (customPerms != null) {
            for (String perm : customPerms) {
                attachment.setPermission(perm, true);
            }
        }

        addon.log("Granted blueprint permission '" + permission + "' to " + player.getName());

        // Recalculate permissions
        player.recalculatePermissions();
    }

    /**
     * Clear all blueprint-related permissions from a player
     */
    public void clearBlueprintPermissions(Player player) {
        UUID playerUUID = player.getUniqueId();

        // Remove existing attachment if any
        PermissionAttachment existing = playerAttachments.remove(playerUUID);
        if (existing != null) {
            try {
                player.removeAttachment(existing);
            } catch (IllegalArgumentException e) {
                // Attachment was already removed
            }
        }

        // Also scan and remove any lingering blueprint permissions
        // (in case they were granted by other means)
        // Copy to avoid ConcurrentModificationException when modifying permissions during iteration
        Set<PermissionAttachmentInfo> perms = new java.util.HashSet<>(player.getEffectivePermissions());
        for (PermissionAttachmentInfo info : perms) {
            if (info.getPermission().startsWith(PERMISSION_PREFIX) && info.getAttachment() != null) {
                try {
                    info.getAttachment().unsetPermission(info.getPermission());
                } catch (Exception e) {
                    // Ignore errors during cleanup
                }
            }
        }

        player.recalculatePermissions();
    }

    /**
     * Get the current blueprint permission for a player
     *
     * @param player The player
     * @return The blueprint name they have permission for, or null if none
     */
    public String getCurrentBlueprintPermission(Player player) {
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String perm = info.getPermission();
            if (perm.startsWith(PERMISSION_PREFIX) && info.getValue()) {
                return perm.substring(PERMISSION_PREFIX.length());
            }
        }
        return null;
    }

    /**
     * Check if a player has permission for a specific blueprint's challenges
     *
     * @param player The player
     * @param blueprintName The blueprint name
     * @return true if player has permission
     */
    public boolean hasBlueprintPermission(Player player, String blueprintName) {
        String permission = PERMISSION_PREFIX + blueprintName.toLowerCase();
        return player.hasPermission(permission);
    }

    /**
     * Register a custom permission mapping for a blueprint.
     * This allows a single blueprint to grant multiple permissions.
     *
     * @param blueprintName The blueprint bundle name
     * @param permissions Set of permissions to grant
     */
    public void registerBlueprintPermissions(String blueprintName, Set<String> permissions) {
        blueprintPermissions.put(blueprintName.toLowerCase(), new HashSet<>(permissions));
        addon.log("Registered custom permissions for blueprint '" + blueprintName + "': " + permissions);
    }

    /**
     * Called when a player disconnects - cleanup their permission attachment
     */
    public void onPlayerQuit(Player player) {
        PermissionAttachment attachment = playerAttachments.remove(player.getUniqueId());
        if (attachment != null) {
            try {
                player.removeAttachment(attachment);
            } catch (Exception e) {
                // Player already disconnected
            }
        }
    }

    /**
     * Get all registered blueprint names
     */
    public Set<String> getRegisteredBlueprints() {
        return new HashSet<>(blueprintPermissions.keySet());
    }

    /**
     * Get the permission prefix used for blueprint challenges
     */
    public String getPermissionPrefix() {
        return PERMISSION_PREFIX;
    }
}
