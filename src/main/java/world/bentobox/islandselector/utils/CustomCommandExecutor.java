package world.bentobox.islandselector.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import world.bentobox.islandselector.IslandSelector;

import java.util.List;
import java.util.Map;

/**
 * Utility class for executing custom commands after relocation or slot switch.
 * Supports different execution scopes: CONSOLE, PLAYER, PLAYER_OP
 */
public class CustomCommandExecutor {

    /**
     * Execution scope for commands
     */
    public enum CommandScope {
        /** Run command from console with full permissions */
        CONSOLE,
        /** Run command as the player with their normal permissions */
        PLAYER,
        /** Run command as the player with temporary OP permissions */
        PLAYER_OP
    }

    private final IslandSelector addon;

    public CustomCommandExecutor(IslandSelector addon) {
        this.addon = addon;
    }

    /**
     * Execute a list of commands with placeholder replacement.
     * Must be called from the main thread.
     *
     * @param player The player context for the commands
     * @param commands List of commands to execute (without leading slash)
     * @param scopeString The execution scope as a string
     * @param placeholders Map of placeholder keys to values (without braces)
     */
    public void executeCommands(Player player, List<String> commands, String scopeString, Map<String, String> placeholders) {
        if (commands == null || commands.isEmpty() || player == null) {
            return;
        }

        CommandScope scope = parseScope(scopeString);

        // Ensure we're on the main thread
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(addon.getPlugin(), () ->
                executeCommandsInternal(player, commands, scope, placeholders));
        } else {
            executeCommandsInternal(player, commands, scope, placeholders);
        }
    }

    /**
     * Internal method to execute commands (must be on main thread)
     */
    private void executeCommandsInternal(Player player, List<String> commands, CommandScope scope, Map<String, String> placeholders) {
        for (String command : commands) {
            if (command == null || command.trim().isEmpty()) {
                continue;
            }

            // Replace placeholders
            String processedCommand = replacePlaceholders(command, placeholders);

            // Remove leading slash if present
            if (processedCommand.startsWith("/")) {
                processedCommand = processedCommand.substring(1);
            }

            addon.log("Executing custom command: " + processedCommand + " (scope: " + scope + ")");

            try {
                switch (scope) {
                    case CONSOLE:
                        executeAsConsole(processedCommand);
                        break;
                    case PLAYER:
                        executeAsPlayer(player, processedCommand);
                        break;
                    case PLAYER_OP:
                        executeAsPlayerOp(player, processedCommand);
                        break;
                }
            } catch (Exception e) {
                addon.logError("Failed to execute custom command '" + processedCommand + "': " + e.getMessage());
            }
        }
    }

    /**
     * Execute command from console
     */
    private void executeAsConsole(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    /**
     * Execute command as the player with their normal permissions
     */
    private void executeAsPlayer(Player player, String command) {
        player.performCommand(command);
    }

    /**
     * Execute command as the player with temporary OP permissions.
     * OP is granted only for the duration of the command execution.
     */
    private void executeAsPlayerOp(Player player, String command) {
        boolean wasOp = player.isOp();
        try {
            if (!wasOp) {
                player.setOp(true);
            }
            player.performCommand(command);
        } finally {
            if (!wasOp) {
                player.setOp(false);
            }
        }
    }

    /**
     * Replace placeholders in a command string
     */
    private String replacePlaceholders(String command, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return command;
        }

        String result = command;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /**
     * Parse scope string to enum, defaulting to CONSOLE if invalid
     */
    private CommandScope parseScope(String scopeString) {
        if (scopeString == null || scopeString.trim().isEmpty()) {
            return CommandScope.CONSOLE;
        }

        try {
            return CommandScope.valueOf(scopeString.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            addon.logWarning("Invalid command scope '" + scopeString + "', defaulting to CONSOLE");
            return CommandScope.CONSOLE;
        }
    }

    /**
     * Create placeholders map for relocation commands
     */
    public static Map<String, String> createRelocationPlaceholders(Player player, String fromCoord, String toCoord, int worldX, int worldZ) {
        return Map.of(
            "player", player.getName(),
            "uuid", player.getUniqueId().toString(),
            "from_coord", fromCoord != null ? fromCoord : "",
            "to_coord", toCoord != null ? toCoord : "",
            "world_x", String.valueOf(worldX),
            "world_z", String.valueOf(worldZ)
        );
    }

    /**
     * Create placeholders map for slot switch commands
     */
    public static Map<String, String> createSlotSwitchPlaceholders(Player player, int fromSlot, int toSlot, int worldX, int worldZ) {
        return Map.of(
            "player", player.getName(),
            "uuid", player.getUniqueId().toString(),
            "from_slot", String.valueOf(fromSlot),
            "slot", String.valueOf(toSlot),
            "world_x", String.valueOf(worldX),
            "world_z", String.valueOf(worldZ)
        );
    }
}
