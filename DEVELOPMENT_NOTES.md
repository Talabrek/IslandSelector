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
