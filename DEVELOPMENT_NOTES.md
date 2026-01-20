# IslandSelector - Development Notes

This document consolidates key technical learnings and patterns discovered during development.

## Build & Deploy

```bash
# Build the addon
mvn clean package -q

# Output JAR
target/IslandSelector-1.0.0.jar

# Install
Copy JAR to plugins/BentoBox/addons/
```

## Dependencies

| Dependency | Required | Purpose |
|------------|----------|---------|
| BentoBox | Yes | Core addon framework |
| BSkyBlock | Yes | Skyblock gamemode |
| FastAsyncWorldEdit | Yes | Schematic operations (slots, backups, relocation) |
| Vault | No | Economy for premium locations |
| PlaceholderAPI | No | Placeholder integration |
| Level | No | Island level display |
| Challenges | No | Slot-specific challenge progress |
| Nova | No | Custom block support in schematics |

## Key Technical Patterns

### BentoBox Integration

**Settings Class:**
- MUST implement `ConfigObject` interface
- Use `@ConfigEntry` annotations for YAML mapping
- BentoBox handles serialization automatically

**Database:**
- Use `@Table` annotations for database objects
- `island.getUniqueId()` returns `String`, not `UUID`
- BentoBox database abstraction supports YAML, JSON, MySQL, MongoDB, SQLite

**Commands:**
- Extend `CompositeCommand` for command groups
- Use `User.getInstance(player)` for BentoBox user wrapper
- Tab completion via `tabComplete()` method

### Grid Coordinate System

```
Grid Format: "X,Z" (e.g., "0,0", "-5,3", "10,-2")
Grid is centered at origin (0,0), supports negative coordinates

World-to-grid: gridCoord = round(worldCoord / spacing)
Grid-to-world: worldCoord = gridCoord * spacing
Default spacing: 400 blocks (from BSkyBlock config)
```

### GUI Best Practices

1. **Player heads:** Use `SkullMeta`, not just `ItemMeta`
2. **In-place updates:** Call `player.updateInventory()` instead of close/reopen
3. **Event order:** Always call `getLocationStatus()` before `getGridLocation()` (registers islands)
4. **Listeners:** Register per-instance, unregister on close

### FAWE Schematic Operations

**Saving islands:**
```java
// 1. Create region from island bounds
CuboidRegion region = new CuboidRegion(min, max);

// 2. Create clipboard
BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

// 3. Copy blocks + entities
EditSession editSession = WorldEdit.getInstance().newEditSession(world);
ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, min);
copy.setCopyingEntities(true);
Operations.complete(copy);

// 4. Save to file
ClipboardFormat format = BuiltInClipboardFormat.SPONGE_SCHEMATIC;
try (ClipboardWriter writer = format.getWriter(outputStream)) {
    writer.write(clipboard);
}
```

**Loading islands:**
```java
// 1. Load schematic
ClipboardFormat format = ClipboardFormats.findByFile(file);
Clipboard clipboard = format.getReader(inputStream).read();

// 2. Paste at location
EditSession editSession = WorldEdit.getInstance().newEditSession(world);
Operation operation = new ClipboardHolder(clipboard)
    .createPaste(editSession)
    .to(location)
    .ignoreAirBlocks(false)
    .build();
Operations.complete(operation);
```

### Multi-Dimension Support

**Key concept:** All dimensions share the same grid coordinate.
- If island is at `-1,0` in overworld, it's also `-1,0` in nether/end
- Slot switching saves/loads ALL dimensions simultaneously
- Relocation moves island in ALL dimensions

**Dimension config in `config.yml`:**
```yaml
dimensions:
  enabled: true
  dimension-configs:
    overworld:
      world-name: "bskyblock_world"
      default-blueprint: "default"
      create-on-claim: true
    nether:
      world-name: "bskyblock_world_nether"
      default-blueprint: "nether_island"
```

## Common Gotchas

### Island Creation Flow
1. Player uses `/island` command
2. `IslandCreateListener` intercepts before BentoBox creates island
3. Player selects location in GUI
4. Create island using `NewIsland.builder()` with custom `LocationStrategy`
5. Register island in GridManager

### Removing Islands Properly
```java
// WRONG - leaves player-island association
addon.getIslands().deleteIsland(island, false, playerUUID);

// RIGHT - fully disassociates player first
for (UUID member : island.getMemberSet()) {
    addon.getIslands().removePlayer(world, member);
}
addon.getIslands().deleteIsland(island, false, playerUUID);
addon.getIslands().getIslandCache().deleteIslandFromCache(island);
```

### Teleport Timing with Schematics
- Don't teleport immediately after schematic paste
- Use delay: `Bukkit.getScheduler().runTaskLater(plugin, () -> teleport(), 60L);`
- For safety, teleport to spawn first, then to island after paste

### Config YAML Type Tags
BentoBox uses SnakeYAML which adds type tags like:
```yaml
overworld: !!world.bentobox.islandselector.models.DimensionConfig
```
This is normal - don't try to use Bukkit's `ConfigurationSerializable` (causes ClassNotFoundException on Paper/Purpur due to classloader isolation).

### Slot Restoration State
After restoring a slot, always clear the pending state:
```java
addon.getSlotManager().clearPendingSlotRestoration(playerUUID);
```
Otherwise, subsequent `/island` commands will fail with "active restoration in progress".

## File Structure

```
src/main/java/world/bentobox/islandselector/
├── IslandSelector.java      # Main addon class
├── Settings.java            # Configuration (implements ConfigObject)
├── commands/                # Player and admin commands
├── database/                # BentoBox database models
│   ├── GridLocationData.java
│   └── SlotData.java
├── gui/                     # Inventory GUIs
│   ├── MainGridGUI.java
│   ├── SlotSelectionGUI.java
│   └── NeighborhoodGUI.java
├── integrations/            # External plugin integrations
│   ├── WorldEditIntegration.java
│   ├── NovaIntegration.java
│   └── PlaceholderAPIIntegration.java
├── listeners/               # Event listeners
│   └── IslandCreateListener.java
├── managers/                # Core business logic
│   ├── GridManager.java
│   ├── SlotManager.java
│   ├── SlotSwitchManager.java
│   ├── BackupManager.java
│   ├── RelocationManager.java
│   ├── DimensionManager.java
│   └── MultiDimensionIslandCreator.java
├── models/                  # Data models
│   ├── GridLocation.java
│   └── DimensionConfig.java
└── utils/                   # Utility classes
    ├── GridCoordinate.java
    ├── SchematicUtils.java
    └── EntityStorage.java
```

## Testing Commands

**Player:**
```
/islandselector          - Open grid GUI
/islandselector slots    - Open slot selection
/islandselector locate   - Show own coordinates
/islandselector neighbors - Open neighborhood view
```

**Admin:**
```
/islandselector admin info 0,0      - Location info
/islandselector admin info <player> - Player info
/islandselector admin reload        - Reload config
/islandselector admin remove <player> - Remove player's island
/islandselector admin relocate <player> <coord> - Force relocate
```

## Performance Notes

- All schematic operations are async (no main thread blocking)
- GUI updates use `updateInventory()` for efficiency
- Grid cache prevents redundant database queries
- Slot switching can take 5-15 seconds for large islands (expected)
- Relocation can take 10-30 seconds for complex builds (expected)

## Integration Patterns

### Nova Integration (Custom Blocks)

Nova blocks are handled separately from FAWE schematics since WorldEdit doesn't natively support them.

```java
// In NovaIntegration.java
// 1. Capture Nova blocks in a region
public List<NovaBlockData> captureNovaBlocks(Location min, Location max) {
    // Uses reflection to access Nova API
    // Stores block type, location, and state data
}

// 2. Restore Nova blocks at new location
public void restoreNovaBlocks(List<NovaBlockData> blocks, Location offset) {
    // Async chunk loading
    // Places Nova blocks via Nova API
}
```

Key points:
- Uses reflection to avoid hard dependency
- Async block capture/restore with chunk loading
- Serializable `NovaBlockData` class for storage
- Gracefully skips if Nova not installed

### Challenges Integration (Slot-Specific Progress)

Each slot maintains separate challenge progress.

```java
// In ChallengesIntegration.java
// Save/restore challenge progress per slot
public void saveSlotChallenges(UUID playerUUID, int slot);
public void restoreSlotChallenges(UUID playerUUID, int slot);

// Data stored in: plugins/IslandSelector/slot-challenges/
```

Key points:
- Challenge progress saved on slot switch
- Progress restored when switching back to slot
- Fresh challenges for new empty slots
- Auto-detected if Challenges addon present

### Level Integration

Displays island levels in GUIs using reflection for version compatibility.

```java
// In LevelIntegration.java
public long getIslandLevel(UUID playerUUID);
public String getFormattedIslandLevel(UUID playerUUID);  // "1.2K", "1.5M", etc.
```

### PlaceholderAPI Integration

Only 5 placeholders are implemented:

| Placeholder | Method |
|------------|--------|
| `%islandselector_location%` | Player's grid coord |
| `%islandselector_slot_active_name%` | Active slot name |
| `%islandselector_cooldown_switch%` | Switch cooldown (formatted) |
| `%islandselector_available%` | Available location count |
| `%islandselector_neighbors_online%` | Online neighbor count (0-8) |

### Custom Command Execution

Execute commands on relocation or slot switch:

```java
// In CustomCommandExecutor.java
// Placeholders available:
// {player}, {uuid}, {from_coord}, {to_coord}, {world_x}, {world_z}
// {from_slot}, {slot} (for slot switch)

// Scopes: CONSOLE, PLAYER, PLAYER_OP
```

## Anti-Patterns and Common Bugs

This section documents bugs discovered during code review. Use this as a checklist to avoid similar issues.

### 1. ItemMeta Null Checks (26 instances fixed)

**Problem:** `ItemStack.getItemMeta()` can return null, but code often continues without checking.

```java
// BAD - NullPointerException if meta is null
ItemStack item = new ItemStack(Material.DIAMOND);
ItemMeta meta = item.getItemMeta();
meta.setDisplayName("Diamond"); // NPE!

// GOOD - Check for null
ItemStack item = new ItemStack(Material.DIAMOND);
ItemMeta meta = item.getItemMeta();
if (meta == null) {
    return item;
}
meta.setDisplayName("Diamond");
item.setItemMeta(meta);
```

**Where it happens:** Every GUI class that creates items with custom names/lore.

### 2. Island.getCenter() Null Check

**Problem:** `island.getCenter()` can return null for islands without a center defined.

```java
// BAD - NPE if center is null
Island island = islandsManager.getIsland(world, playerUUID);
int x = island.getCenter().getBlockX(); // NPE!

// GOOD - Check for null
Island island = islandsManager.getIsland(world, playerUUID);
if (island == null) return;
Location center = island.getCenter();
if (center == null) return;
int x = center.getBlockX();
```

### 3. Reflection Results Not Null-Checked

**Problem:** When using reflection to access BentoBox internals, the returned objects may be null.

```java
// BAD - NPE if cache field returns null
Object islandCache = cacheField.get(islandsManager);
Method deleteMethod = islandCache.getClass().getMethod(...); // NPE!

// GOOD - Check for null
Object islandCache = cacheField.get(islandsManager);
if (islandCache != null) {
    Method deleteMethod = islandCache.getClass().getMethod(...);
    deleteMethod.invoke(islandCache, island);
}
```

### 4. Unsynchronized HashMap Access from Multiple Threads

**Problem:** Plain HashMap accessed from async callbacks causes ConcurrentModificationException.

```java
// BAD - Race condition in multi-threaded access
private final Map<UUID, Integer> pendingSlotCreations = new HashMap<>();

// GOOD - Use ConcurrentHashMap
private final Map<UUID, Integer> pendingSlotCreations = new ConcurrentHashMap<>();
```

**Where it happens:** Any map accessed from both main thread and async callbacks (GridManager, SlotManager).

### 5. GUI Slot Constant Conflicts

**Problem:** Two different GUI elements assigned to the same slot position.

```java
// BAD - Both use slot 13, one will overwrite the other!
private static final int CENTER_SLOT = 13;
private static final int S_SLOT = 13;

// GOOD - Use unique slots
private static final int CENTER_SLOT = 13;
private static final int S_SLOT = 22;
```

### 6. Stream Operations on Mutable Collections

**Problem:** Streaming over a collection that could be modified during iteration.

```java
// BAD - ConcurrentModificationException if pendingResets is modified
boolean isReset = pendingResets.values().stream().anyMatch(c -> c.equals(coord));

// GOOD - Create defensive copy first
boolean isReset = new ArrayList<>(pendingResets.values()).stream()
    .anyMatch(c -> c.equals(coord));
```

### 7. Player Online Status Not Checked Before Commands

**Problem:** Player could disconnect between check and command execution.

```java
// BAD - Player might be offline when performCommand runs
public void executeAsPlayer(Player player, String command) {
    player.performCommand(command); // May fail if player disconnected
}

// GOOD - Double-check online status
public void executeAsPlayer(Player player, String command) {
    if (!player.isOnline()) {
        addon.logWarning("Player offline, cannot execute command");
        return;
    }
    player.performCommand(command);
}
```

### 8. Thread.sleep() in Async Code

**Problem:** Using Thread.sleep() blocks the async worker thread.

```java
// BAD - Blocks thread pool worker
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    doSomething();
    Thread.sleep(2000); // Blocks the worker!
    doSomethingElse();
});

// GOOD - Use scheduler delays
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    doSomething();
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        doSomethingElse();
    }, 40L); // 2 seconds = 40 ticks
});
```

### 9. Stale Player References in Async Callbacks

**Problem:** Storing Player reference and using it later in async callback.

```java
// BAD - Player object could be stale if they reconnect
private final Player player; // Stored during GUI creation
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    // ... async work ...
    Bukkit.getScheduler().runTask(plugin, () -> {
        player.sendMessage("Done!"); // Might be stale!
    });
});

// GOOD - Store UUID, look up fresh reference
private final UUID playerUUID;
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    // ... async work ...
    Bukkit.getScheduler().runTask(plugin, () -> {
        Player freshPlayer = Bukkit.getPlayer(playerUUID);
        if (freshPlayer != null) {
            freshPlayer.sendMessage("Done!");
        }
    });
});
```

### 10. Check-Then-Act Race Conditions (Economy)

**Problem:** Checking balance then withdrawing in separate operations.

```java
// BAD - Race condition: balance could change between check and withdraw
if (economy.has(player, cost)) {
    // Another thread could withdraw here!
    economy.withdrawPlayer(player, cost);
}

// BETTER - Use atomic withdraw that returns success/failure
EconomyResponse response = economy.withdrawPlayer(player, cost);
if (response.transactionSuccess()) {
    // Proceed
} else {
    // Handle insufficient funds
}
```

### 11. Event Listeners Not Unregistered

**Problem:** Registering listener per-GUI instance but never unregistering.

```java
// BAD - Memory leak: listener stays registered forever
public void open() {
    Bukkit.getPluginManager().registerEvents(this, addon.getPlugin());
    player.openInventory(inventory);
}

// GOOD - Unregister on close
@EventHandler
public void onInventoryClose(InventoryCloseEvent event) {
    if (event.getInventory().getHolder() == this) {
        HandlerList.unregisterAll(this);
    }
}
```

### 12. Synchronized Method Returns Internal Reference

**Problem:** Synchronized getter returns the internal collection, allowing external modification.

```java
// BAD - Caller can modify internal map
public synchronized Map<String, String> getDimensionIslandUUIDs() {
    return dimensionIslandUUIDs; // Caller can modify!
}

// GOOD - Return defensive copy
public synchronized Map<String, String> getDimensionIslandUUIDs() {
    return new HashMap<>(dimensionIslandUUIDs);
}
```

## Bug Fix Summary

| Round | Bugs Fixed | Categories |
|-------|------------|------------|
| Round 1 | 25+ | Race conditions, memory leaks, async issues |
| Round 2 | 40+ | Null safety, thread safety, logic errors |

**Key Files Most Affected:**
- All GUI classes (ItemMeta null checks)
- GridManager, SlotManager (ConcurrentHashMap)
- RelocationManager (reflection null checks)
- IslandCreateListener (defensive copies)
