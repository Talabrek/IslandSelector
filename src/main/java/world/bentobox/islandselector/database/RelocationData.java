package world.bentobox.islandselector.database;

import com.google.gson.annotations.Expose;
import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.Table;

import java.util.UUID;

/**
 * Database object for tracking player island relocation history and cooldowns
 * Each record represents a player's relocation data
 */
@Table(name = "IslandRelocations")
public class RelocationData implements DataObject {

    /**
     * Unique ID for this record - uses player UUID
     */
    @Expose
    private String uniqueId;

    /**
     * UUID of the player
     */
    @Expose
    private String playerUUID;

    /**
     * Timestamp of last relocation (milliseconds since epoch)
     */
    @Expose
    private long lastRelocationTime = 0;

    /**
     * Previous grid coordinate (before last relocation)
     */
    @Expose
    private String previousCoordinate;

    /**
     * Current grid coordinate (after last relocation)
     */
    @Expose
    private String currentCoordinate;

    /**
     * Total number of relocations this player has performed
     */
    @Expose
    private int totalRelocations = 0;

    /**
     * Default constructor for database serialization
     */
    public RelocationData() {
    }

    /**
     * Constructor for creating new relocation data
     */
    public RelocationData(String playerUUID) {
        this.uniqueId = playerUUID;
        this.playerUUID = playerUUID;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(String playerUUID) {
        this.playerUUID = playerUUID;
    }

    public long getLastRelocationTime() {
        return lastRelocationTime;
    }

    public void setLastRelocationTime(long lastRelocationTime) {
        this.lastRelocationTime = lastRelocationTime;
    }

    public String getPreviousCoordinate() {
        return previousCoordinate;
    }

    public void setPreviousCoordinate(String previousCoordinate) {
        this.previousCoordinate = previousCoordinate;
    }

    public String getCurrentCoordinate() {
        return currentCoordinate;
    }

    public void setCurrentCoordinate(String currentCoordinate) {
        this.currentCoordinate = currentCoordinate;
    }

    public int getTotalRelocations() {
        return totalRelocations;
    }

    public void setTotalRelocations(int totalRelocations) {
        this.totalRelocations = totalRelocations;
    }

    /**
     * Increment the relocation counter
     */
    public void incrementRelocations() {
        this.totalRelocations++;
    }

    /**
     * Record a new relocation
     */
    public void recordRelocation(String from, String to) {
        this.previousCoordinate = from;
        this.currentCoordinate = to;
        this.lastRelocationTime = System.currentTimeMillis();
        incrementRelocations();
    }

    /**
     * Check if the player is currently on cooldown
     * @param cooldownSeconds Cooldown duration in seconds
     * @return true if on cooldown, false otherwise
     */
    public boolean isOnCooldown(int cooldownSeconds) {
        if (lastRelocationTime == 0) {
            return false; // Never relocated before
        }
        long cooldownMillis = cooldownSeconds * 1000L;
        long timeSinceLastRelocation = System.currentTimeMillis() - lastRelocationTime;
        return timeSinceLastRelocation < cooldownMillis;
    }

    /**
     * Get remaining cooldown time in seconds
     * @param cooldownSeconds Cooldown duration in seconds
     * @return Remaining seconds, or 0 if not on cooldown
     */
    public long getRemainingCooldown(int cooldownSeconds) {
        if (!isOnCooldown(cooldownSeconds)) {
            return 0;
        }
        long cooldownMillis = cooldownSeconds * 1000L;
        long timeSinceLastRelocation = System.currentTimeMillis() - lastRelocationTime;
        long remainingMillis = cooldownMillis - timeSinceLastRelocation;
        return remainingMillis / 1000; // Convert to seconds
    }
}
