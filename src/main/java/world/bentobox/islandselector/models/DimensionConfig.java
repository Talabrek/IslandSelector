package world.bentobox.islandselector.models;

/**
 * Configuration for a single dimension/world in the multi-dimension island system.
 * Each dimension can have its own world, blueprint, and settings.
 */
public class DimensionConfig {

    /**
     * Unique key for this dimension (e.g., "overworld", "nether", "the_end", "water_world")
     */
    private String dimensionKey;

    /**
     * Bukkit world name for this dimension (e.g., "bskyblock_world", "bskyblock_world_nether")
     */
    private String worldName;

    /**
     * Whether this dimension is enabled
     */
    private boolean enabled = true;

    /**
     * Default blueprint bundle to use when creating islands in this dimension
     */
    private String defaultBlueprint = "default";

    /**
     * Whether to create an island in this dimension immediately when player claims a location
     * If false, island is only created when player first visits this dimension
     */
    private boolean createOnClaim = true;

    /**
     * Display name for this dimension in GUIs
     */
    private String displayName;

    /**
     * Material name for the icon representing this dimension in GUIs
     */
    private String iconMaterial = "GRASS_BLOCK";

    /**
     * Default constructor for serialization
     */
    public DimensionConfig() {
    }

    /**
     * Create a new dimension config
     * @param dimensionKey Unique key for this dimension
     * @param worldName Bukkit world name
     */
    public DimensionConfig(String dimensionKey, String worldName) {
        this.dimensionKey = dimensionKey;
        this.worldName = worldName;
        this.displayName = formatDisplayName(dimensionKey);
    }

    /**
     * Create a new dimension config with all settings
     */
    public DimensionConfig(String dimensionKey, String worldName, boolean enabled,
                           String defaultBlueprint, boolean createOnClaim) {
        this.dimensionKey = dimensionKey;
        this.worldName = worldName;
        this.enabled = enabled;
        this.defaultBlueprint = defaultBlueprint;
        this.createOnClaim = createOnClaim;
        this.displayName = formatDisplayName(dimensionKey);
    }

    /**
     * Format dimension key into a readable display name
     */
    private String formatDisplayName(String key) {
        if (key == null || key.isEmpty()) {
            return "Unknown";
        }
        // Convert snake_case to Title Case
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(part.substring(0, 1).toUpperCase());
            if (part.length() > 1) {
                sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    // Getters and Setters

    public String getDimensionKey() {
        return dimensionKey;
    }

    public void setDimensionKey(String dimensionKey) {
        this.dimensionKey = dimensionKey;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultBlueprint() {
        return defaultBlueprint;
    }

    public void setDefaultBlueprint(String defaultBlueprint) {
        this.defaultBlueprint = defaultBlueprint;
    }

    public boolean isCreateOnClaim() {
        return createOnClaim;
    }

    public void setCreateOnClaim(boolean createOnClaim) {
        this.createOnClaim = createOnClaim;
    }

    public String getDisplayName() {
        if (displayName == null || displayName.isEmpty()) {
            return formatDisplayName(dimensionKey);
        }
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getIconMaterial() {
        return iconMaterial;
    }

    public void setIconMaterial(String iconMaterial) {
        this.iconMaterial = iconMaterial;
    }

    @Override
    public String toString() {
        return "DimensionConfig{" +
                "dimensionKey='" + dimensionKey + '\'' +
                ", worldName='" + worldName + '\'' +
                ", enabled=" + enabled +
                ", defaultBlueprint='" + defaultBlueprint + '\'' +
                ", createOnClaim=" + createOnClaim +
                '}';
    }
}
