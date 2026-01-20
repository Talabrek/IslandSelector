package world.bentobox.islandselector.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandselector.IslandSelector;

/**
 * Admin version command
 * /islandselector admin version - Shows plugin version and dependency information
 */
public class AdminVersionCommand extends CompositeCommand {

    public AdminVersionCommand(CompositeCommand parent) {
        super(parent, "version");
    }

    @Override
    public void setup() {
        setPermission("islandselector.admin.version");
        setDescription("commands.islandselector.admin.version.description");
        setOnlyPlayer(false); // Can be run from console
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        IslandSelector addon = (IslandSelector) getAddon();

        // Header
        user.sendMessage("&6====================================");
        user.sendMessage("&eIslandSelector - Version Information");
        user.sendMessage("&6====================================");
        user.sendMessage("");

        // IslandSelector version
        String addonVersion = addon.getDescription().getVersion();
        user.sendMessage("&a✓ IslandSelector: &f" + addonVersion);

        // BentoBox version
        String bentoboxVersion = addon.getPlugin().getDescription().getVersion();
        user.sendMessage("&a✓ BentoBox: &f" + bentoboxVersion);

        // BSkyBlock version
        String bskyblockVersion = getBSkyBlockVersion(addon);
        if (bskyblockVersion != null) {
            user.sendMessage("&a✓ BSkyBlock: &f" + bskyblockVersion);
        } else {
            user.sendMessage("&c✗ BSkyBlock: &cNot Found");
        }

        // FastAsyncWorldEdit version
        String faweVersion = getFAWEVersion();
        if (faweVersion != null) {
            user.sendMessage("&a✓ FastAsyncWorldEdit: &f" + faweVersion);
        } else {
            user.sendMessage("&c✗ FastAsyncWorldEdit: &cNot Found");
        }

        user.sendMessage("");
        user.sendMessage("&7Optional Dependencies:");

        // Vault
        String vaultVersion = getPluginVersion("Vault");
        if (vaultVersion != null) {
            user.sendMessage("&a✓ Vault: &f" + vaultVersion + " &7(Economy features enabled)");
        } else {
            user.sendMessage("&7- Vault: &8Not installed &7(Economy features disabled)");
        }

        // PlaceholderAPI
        String papiVersion = getPluginVersion("PlaceholderAPI");
        if (papiVersion != null) {
            user.sendMessage("&a✓ PlaceholderAPI: &f" + papiVersion + " &7(Placeholders enabled)");
        } else {
            user.sendMessage("&7- PlaceholderAPI: &8Not installed &7(Placeholders disabled)");
        }

        // Level addon
        String levelVersion = getLevelAddonVersion(addon);
        if (levelVersion != null) {
            user.sendMessage("&a✓ Level: &f" + levelVersion + " &7(Level display enabled)");
        } else {
            user.sendMessage("&7- Level: &8Not installed &7(Level display disabled)");
        }

        user.sendMessage("");
        user.sendMessage("&6====================================");

        return true;
    }

    /**
     * Get BSkyBlock version
     */
    private String getBSkyBlockVersion(IslandSelector addon) {
        return addon.getPlugin().getAddonsManager()
            .getGameModeAddons().stream()
            .filter(gm -> gm.getDescription().getName().equalsIgnoreCase("BSkyBlock"))
            .findFirst()
            .map(gm -> gm.getDescription().getVersion())
            .orElse(null);
    }

    /**
     * Get FAWE version
     */
    private String getFAWEVersion() {
        // Try FastAsyncWorldEdit first
        Plugin fawe = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
        if (fawe != null) {
            return fawe.getDescription().getVersion();
        }

        // Fallback to WorldEdit
        Plugin worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (worldEdit != null) {
            return worldEdit.getDescription().getVersion() + " (WorldEdit)";
        }

        return null;
    }

    /**
     * Get plugin version by name
     */
    private String getPluginVersion(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null ? plugin.getDescription().getVersion() : null;
    }

    /**
     * Get Level addon version
     */
    private String getLevelAddonVersion(IslandSelector addon) {
        return addon.getPlugin().getAddonsManager()
            .getAddonByName("Level")
            .map(Addon::getDescription)
            .map(desc -> desc.getVersion())
            .orElse(null);
    }
}
