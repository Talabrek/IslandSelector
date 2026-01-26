# Architecture: Nova Integration for IslandSelector

**Domain:** BentoBox addon custom block integration
**Researched:** 2026-01-26
**Overall Confidence:** MEDIUM

## Executive Summary

Nova integration follows IslandSelector's established pattern: reflection-based integration classes that avoid hard dependencies, with capture/restore logic parallel to EntityStorage. The integration must hook into existing slot switching, relocation, and backup workflows at the same points where EntityStorage operates.

**Key Finding:** Nova stores custom block data outside Minecraft's NBT system, making it invisible to WorldEdit/FAWE. The integration must explicitly capture and restore Nova blocks during island operations, similar to how EntityStorage handles entities that FAWE misses.

## Current Architecture Overview

IslandSelector uses a modular integration architecture:

```
IslandSelector.java (main addon)
├── Initializes integrations on load
├── Provides accessor methods
└── No business logic in main class

integrations/
├── WorldEditIntegration - Detection + async wrappers for FAWE
├── NovaIntegration - Detection + reflection-based capture/restore (BROKEN)
├── LevelIntegration - Optional addon integration
└── PlaceholderAPIIntegration - Optional plugin integration

managers/
├── SlotSwitchManager - Orchestrates slot switch workflow
├── RelocationManager - Orchestrates island relocation workflow
├── BackupManager - Orchestrates backup creation
└── Others - Grid, Slot, Dimension management

utils/
├── SchematicUtils - Safe FAWE operations wrapper
└── EntityStorage - Entity capture/restore (FAWE can't handle these reliably)
```

**Integration Philosophy:**
- Use reflection to avoid hard dependencies
- Detect availability at runtime
- Graceful degradation if not available
- Operations are "best effort" - failures are logged but don't block the main operation

## Nova Block Storage Model

Nova stores custom block data in a separate world data system:

```
Minecraft World
├── Chunk data (blocks, biomes)
├── Tile entities (vanilla)
└── [Nova's separate storage]
    ├── WorldDataManager - Global manager
    ├── NovaBlockState per-block - Block ID + state
    └── TileEntity instances - Full state including inventory/data
```

**Why WorldEdit/FAWE Can't Copy Nova Blocks:**
- WorldEdit copies Minecraft's chunk data only
- Nova's block data is stored outside chunks
- When pasted, blocks appear as vanilla (often note blocks/mushrooms)
- Nova tile entity data (inventory, progress, etc.) is completely lost

## Required API Changes (Nova 0.17+)

The current NovaIntegration.java uses outdated Nova 0.16 API paths. Nova 0.17 changed the API significantly:

### Old API (0.16) - Current Code
```java
// BlockManager class exists
Object blockManager = Class.forName("xyz.xenondevs.nova.data.world.block.BlockManager");

// TileEntityManager exists
Object tileEntityManager = Class.forName("xyz.xenondevs.nova.world.block.tileentity.TileEntityManager");
```

### New API (0.17+) - Required
```java
// BlockManager removed, use BlockUtils
Class.forName("xyz.xenondevs.nova.util.BlockUtils");

// TileEntityManager removed, use WorldDataManager
Class.forName("xyz.xenondevs.nova.world.format.WorldDataManager");

// Get tile entity: WorldDataManager.INSTANCE.getTileEntity(location)
// Place block: BlockUtils.placeBlock(context, location, novaBlock, playEffects)
// Break block: BlockUtils.breakBlock(context, location, playEffects)
```

### Key Differences
- Managers removed in favor of utility classes
- TileEntity functions now prefixed with `stored...` instead of `get...`
- Block states now use Kotlin-style extension properties
- Tile entities may instantiate off main thread

**Sources:**
- [Nova 0.16 to 0.17 Migration Guide](https://docs.xenondevs.xyz/nova/addon/migration-guide/0.16-0.17/)
- [Nova GitHub Repository](https://github.com/xenondevs/Nova)

## Integration Points with Existing Components

### 1. SlotSwitchManager Integration

**Current Flow:**
```
switchSlot()
  → performSlotSwitchAsync()
    → saveAllDimensionIslands()
      → SchematicUtils.copyAndSave()
      → EntityStorage.saveEntities()
    → clearAllDimensionIslands()
      → WorldEditIntegration.removeEntities()
      → SchematicUtils.clearRegion()
    → loadAllDimensionIslands()
      → SchematicUtils.loadAndPaste()
      → EntityStorage.loadEntities()
```

**Nova Integration Points:**
```
switchSlot()
  → performSlotSwitchAsync()
    → saveAllDimensionIslands()
      → SchematicUtils.copyAndSave()
      → EntityStorage.saveEntities()
      → NovaIntegration.captureNovaBlocks() ← ADD HERE
    → clearAllDimensionIslands()
      → WorldEditIntegration.removeEntities()
      → SchematicUtils.clearRegion()
      → NovaIntegration.removeNovaBlocks() ← ADD HERE
    → loadAllDimensionIslands()
      → SchematicUtils.loadAndPaste()
      → EntityStorage.loadEntities()
      → NovaIntegration.restoreNovaBlocks() ← ADD HERE
```

**Threading Requirements:**
- `captureNovaBlocks()` - Can run async (WorldDataManager is thread-safe for reads)
- `removeNovaBlocks()` - Must run on main thread (modifies world state)
- `restoreNovaBlocks()` - Must run on main thread (places blocks)

### 2. RelocationManager Integration

**Current Flow (in-memory capture):**
```
relocateIsland()
  → captureIslandData()
    → entities = EntityStorage.captureEntitiesInMemory()
  → removeBlocks()
    → WorldEditIntegration.removeEntities()
    → SchematicUtils.clearRegion()
  → pasteAtNewLocation()
    → SchematicUtils.pasteFromClipboard()
    → EntityStorage.restoreEntitiesInMemory(entities)
```

**Nova Integration Points:**
```
relocateIsland()
  → captureIslandData()
    → entities = EntityStorage.captureEntitiesInMemory()
    → novaBlocks = NovaIntegration.captureNovaBlocksInMemory() ← ADD HERE
  → removeBlocks()
    → WorldEditIntegration.removeEntities()
    → SchematicUtils.clearRegion()
    → NovaIntegration.removeNovaBlocks() ← ADD HERE
  → pasteAtNewLocation()
    → SchematicUtils.pasteFromClipboard()
    → EntityStorage.restoreEntitiesInMemory(entities)
    → NovaIntegration.restoreNovaBlocksInMemory(novaBlocks) ← ADD HERE
```

**Key Difference:** Relocation uses in-memory capture (no file I/O) because the source and destination are in the same operation. SlotSwitch/Backup use file-based storage because data persists across operations.

### 3. BackupManager Integration

**Current Flow:**
```
createBackup()
  → saveSlotToBackup()
    → SchematicUtils.copyAndSave()
    → (EntityStorage handled by SchematicUtils)

restoreBackup()
  → loadBackupToWorld()
    → SchematicUtils.loadAndPaste()
    → (EntityStorage handled by SchematicUtils)
```

**Nova Integration Points:**
```
createBackup()
  → saveSlotToBackup()
    → SchematicUtils.copyAndSave()
    → NovaIntegration.saveNovaBlocks(schematicFile) ← ADD HERE

restoreBackup()
  → loadBackupToWorld()
    → SchematicUtils.loadAndPaste()
    → NovaIntegration.loadNovaBlocks(schematicFile) ← ADD HERE
```

**Note:** BackupManager calls for both single-dimension and multi-dimension cases. Nova integration must handle dimension-specific file paths.

## New Components vs Modifications

### Components to Modify

**1. NovaIntegration.java (MAJOR REWRITE)**
- Update reflection paths for Nova 0.17 API
- Fix `captureNovaBlocks()` to use WorldDataManager.getTileEntity()
- Fix `restoreNovaBlocks()` to use BlockUtils.placeBlock()
- Add `removeNovaBlocks()` method (currently missing)
- Add in-memory capture/restore for relocation
- Improve serialization to capture full tile entity state (not just block ID)

**2. SlotSwitchManager.java (MINOR ADDITIONS)**
- Add Nova capture after entity save (lines ~365)
- Add Nova removal after entity removal (lines ~481)
- Add Nova restore after entity restore (lines ~433)
- Handle dimension-aware Nova file paths
- Error handling: log failures but continue operation

**3. RelocationManager.java (MINOR ADDITIONS)**
- Add in-memory Nova capture (parallel to entity capture)
- Add Nova removal in clearRegion
- Add Nova restore in pasteRegion
- Pass captured Nova blocks through the workflow

**4. BackupManager.java (MINOR ADDITIONS)**
- Add Nova block save after schematic save
- Add Nova block load after schematic load
- Handle multi-dimension Nova file paths
- No change to backup retention/cleanup logic

### New Components Needed

**None.** The existing NovaIntegration.java class provides the right architecture. It needs API updates and feature additions, not replacement.

### Shared Utility: NovaBlockData Storage

Nova blocks need to be serialized to files (for backups/slot switch) and held in memory (for relocation).

**Storage Format (File-Based):**
```
Format: .nova.dat file alongside .schem
Path: slots/uuid/slot-1.schem → slots/uuid/slot-1.nova.dat
Encoding: Gzipped Java serialization
Content: List<NovaBlockData>
  - relX, relY, relZ (int) - Relative position
  - blockId (String) - Nova block identifier
  - tileEntityData (byte[]) - Serialized tile entity state (if applicable)
```

**Current Implementation Problem:**
The existing `NovaBlockData` class only stores `blockId` (String). It doesn't capture tile entity state (inventories, progress bars, stored energy, etc.). This must be expanded.

**Required Data Structure:**
```java
class NovaBlockData implements Serializable {
    int relX, relY, relZ;           // Position relative to center
    String blockId;                  // Nova block type ID
    byte[] tileEntityData;           // Serialized tile entity state (nullable)
    String blockStateData;           // Block state properties (nullable)
}
```

## Data Flow During Operations

### Slot Switch Data Flow

```
SAVE PHASE (Async Thread)
┌─────────────────────────────────────┐
│ island @ grid (5,3)                 │
│ ├─ FAWE copies blocks → slot-1.schem│
│ ├─ EntityStorage → slot-1.entities │
│ └─ NovaIntegration → slot-1.nova.dat│
└─────────────────────────────────────┘
              ↓
CLEAR PHASE (Main Thread)
┌─────────────────────────────────────┐
│ island @ grid (5,3)                 │
│ ├─ Remove entities                  │
│ ├─ FAWE clears blocks               │
│ └─ NovaIntegration removes Nova blocks
└─────────────────────────────────────┘
              ↓
LOAD PHASE (Async Thread)
┌─────────────────────────────────────┐
│ island @ grid (5,3)                 │
│ ├─ FAWE pastes slot-2.schem         │
│ ├─ EntityStorage ← slot-2.entities  │
│ └─ NovaIntegration ← slot-2.nova.dat│
└─────────────────────────────────────┘
```

### Relocation Data Flow

```
CAPTURE PHASE (Main Thread)
┌─────────────────────────────────────┐
│ Source island @ grid (5,3)          │
│ ├─ FAWE copies to clipboard         │
│ ├─ EntityStorage → List<Entity>    │
│ └─ NovaIntegration → List<NovaBlock>│
└─────────────────────────────────────┘
              ↓
CLEAR PHASE (Main Thread)
┌─────────────────────────────────────┐
│ Source island @ grid (5,3)          │
│ ├─ Remove entities                  │
│ ├─ FAWE clears blocks               │
│ └─ NovaIntegration removes Nova blocks
└─────────────────────────────────────┘
              ↓
PASTE PHASE (Async/Main)
┌─────────────────────────────────────┐
│ Destination island @ grid (10,-2)   │
│ ├─ FAWE pastes from clipboard       │
│ ├─ EntityStorage ← List<Entity>     │
│ └─ NovaIntegration ← List<NovaBlock>│
└─────────────────────────────────────┘
```

**Key Insight:** Nova blocks follow the same capture → clear → restore pattern as entities. The integration hooks into existing workflows at the same points.

## Threading and Synchronization

### Thread Safety Requirements

**WorldDataManager (Nova):**
- Read operations (getTileEntity): Thread-safe, can run async
- Write operations (place/remove blocks): Main thread only

**BlockUtils (Nova):**
- All operations: Main thread only (modifies world state)

**IslandSelector Operations:**
- Capture: Can run async (reads only)
- Remove: Must run on main thread (modifies world)
- Restore: Must run on main thread (modifies world)

### Execution Flow in SlotSwitchManager

```
Main Thread: Fire event, teleport to spawn
     ↓
Async Thread: Save operations
     ├─ FAWE schematic save (async)
     ├─ Entity save (sync via callSyncMethod)
     └─ Nova capture (async - WorldDataManager reads are safe)
     ↓
Main Thread: Clear operations (via CompletableFuture)
     ├─ Entity removal (main thread)
     ├─ FAWE clear (async)
     └─ Nova removal (main thread)
     ↓
Async Thread: Load operations
     ├─ FAWE schematic paste (async)
     ↓
Main Thread: Restore operations
     ├─ Entity restore (main thread)
     └─ Nova restore (main thread)
     ↓
Main Thread: Teleport players, completion
```

**Implementation Pattern:**
```java
// In async context, schedule main-thread Nova operations:
CompletableFuture<List<NovaBlockData>> captureNovaFuture = new CompletableFuture<>();
Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
    List<NovaBlockData> blocks = novaIntegration.captureNovaBlocks(center, range);
    captureNovaFuture.complete(blocks);
});
List<NovaBlockData> novaBlocks = captureNovaFuture.get(30, TimeUnit.SECONDS);
```

## Error Handling Strategy

**Philosophy:** Nova integration is a "best effort" feature. Failures should be logged but must not break the primary operation (slot switch, relocation, backup).

### Failure Scenarios

**1. Nova Plugin Not Installed**
- Detection: `novaIntegration.isAvailable()` returns false
- Handling: Skip all Nova operations silently
- User Impact: None (Nova blocks weren't being copied anyway)

**2. Nova API Changed (Future Version)**
- Detection: ClassNotFoundException during reflection
- Handling: Log warning, disable integration
- User Impact: Log message suggesting addon update

**3. Capture Fails During Operation**
- Detection: captureNovaBlocks() returns empty list or throws exception
- Handling: Log warning, continue with operation
- User Impact: Nova blocks not copied (same as current behavior)

**4. Restore Fails During Operation**
- Detection: restoreNovaBlocks() throws exception or returns false
- Handling: Log warning, continue with operation
- User Impact: Nova blocks not restored, but vanilla blocks + entities are

**5. Tile Entity Data Corruption**
- Detection: Deserialization exception when loading .nova.dat
- Handling: Skip corrupted blocks, restore others
- User Impact: Some Nova blocks missing, most restored

### Logging Strategy

```java
// Initialization
addon.log("Nova detected - custom block support enabled");

// Operation start
addon.log("Capturing Nova blocks for slot switch...");

// Success
addon.log("Captured 47 Nova blocks (12 with tile entities)");

// Partial failure
addon.logWarning("Failed to capture 3 Nova blocks - continuing anyway");

// Complete failure
addon.logError("Nova capture failed completely: " + ex.getMessage());
// → Still continue with slot switch

// API incompatibility
addon.logError("Nova API incompatible - disabling Nova integration");
```

## Suggested Build Order

The integration touches three managers in a dependent chain. Build order matters to enable incremental testing.

### Phase 1: Core Integration (NovaIntegration.java)
**Goal:** Update to Nova 0.17 API, basic capture/restore working

1. Update detection logic for Nova 0.17 class paths
2. Implement `captureNovaBlocks()` using WorldDataManager.getTileEntity()
3. Implement `restoreNovaBlocks()` using BlockUtils.placeBlock()
4. Add `removeNovaBlocks()` method (new)
5. Expand NovaBlockData to include tile entity state
6. Add file I/O methods (saveToFile/loadFromFile)
7. Add in-memory methods (for relocation)

**Testing:** Standalone unit tests with mock island regions

**Deliverable:** NovaIntegration.isAvailable() returns true, capture/restore methods work

### Phase 2: Backup Integration (BackupManager.java)
**Goal:** Nova blocks saved/restored in backups

1. Add NovaIntegration.saveNovaBlocks() call after SchematicUtils.copyAndSave()
2. Add NovaIntegration.loadNovaBlocks() call after SchematicUtils.loadAndPaste()
3. Handle multi-dimension file paths (.nova.dat per dimension)
4. Update backup cleanup to include .nova.dat files

**Testing:** Create backup → verify .nova.dat exists → restore backup → verify Nova blocks present

**Deliverable:** Backups include Nova block data, restoration works

### Phase 3: Slot Switch Integration (SlotSwitchManager.java)
**Goal:** Nova blocks preserved during slot switching

1. Add Nova capture in saveIslandToSchematic() (after entity save)
2. Add Nova removal in clearIslandBlocksAndEntities() (after entity removal)
3. Add Nova restore in loadSchematicToWorld() (after entity restore)
4. Handle multi-dimension Nova operations in saveAllDimensionIslands/loadAllDimensionIslands
5. Add error handling and progress messages

**Testing:** Build test islands with Nova blocks → switch slots → verify Nova blocks move

**Deliverable:** Slot switching preserves Nova blocks completely

### Phase 4: Relocation Integration (RelocationManager.java)
**Goal:** Nova blocks preserved during island relocation

1. Add Nova in-memory capture in captureRegion()
2. Add Nova removal in removeBlocks()
3. Add Nova restore in pasteAtNewLocation()
4. Pass Nova block list through workflow (parallel to entities)

**Testing:** Create island with Nova blocks → relocate → verify blocks at new location

**Deliverable:** Relocation preserves Nova blocks completely

### Phase 5: Testing and Refinement
**Goal:** Edge case handling, performance validation

1. Test with large numbers of Nova blocks (100+ blocks)
2. Test with complex Nova tile entities (machines with inventories)
3. Test async timeout scenarios
4. Performance profiling (capture/restore timing)
5. Multi-dimension test coverage

**Testing:** Stress tests, edge cases, performance benchmarks

**Deliverable:** Robust integration handling edge cases gracefully

## Performance Considerations

### Scanning Performance

**Current Approach (NovaIntegration.java lines 135-161):**
```java
// Triple nested loop - scans entire island volume
for (int x = centerX - range; x <= centerX + range; x++) {
    for (int z = centerZ - range; z <= centerZ + range; z++) {
        for (int y = minY; y < maxY; y++) {
            // Check every block for Nova blocks
        }
    }
}
```

**Performance Impact:**
- Island size: 400x400 blocks = 160,000 blocks per dimension
- Height range: -64 to 320 = 384 blocks
- Total blocks scanned: 61,440,000 per dimension
- At ~1µs per check: ~60 seconds per dimension (unacceptable)

**Optimization Strategy:**
1. Use Nova's internal registry if available (ask Nova for all blocks in region)
2. If not, maintain a cache of known Nova block locations
3. Async chunk loading before scan
4. Parallel scanning across chunks
5. Early exit if no Nova blocks found in first few chunks

**Target:** < 5 seconds for typical island with 50 Nova blocks

### Serialization Performance

**Tile Entity Data:**
- Complex tile entities may have large NBT structures
- Compression (GZIP) is essential for file storage
- In-memory operations skip compression for speed

**File Size Estimates:**
- 50 Nova blocks without tile entities: ~5KB
- 50 Nova blocks with tile entities: ~50-500KB (depends on inventories)
- Compressed: ~10-50KB typical

### Threading Strategy

- **Capture:** Async with chunk preloading
- **Save to file:** Async (I/O heavy)
- **Remove:** Main thread (world modification)
- **Restore:** Main thread (world modification)
- **Load from file:** Async (I/O), then sync restore

## Risk Factors

### HIGH Risk

**1. Nova API Stability**
- **Risk:** Nova 0.18+ may change API again
- **Mitigation:** Use try-catch around all reflection, version detection
- **Impact:** Integration breaks on Nova update

**2. TileEntity Serialization**
- **Risk:** Nova's internal tile entity format may not be serializable via standard Java
- **Mitigation:** Use Nova's own serialization if available, fallback to partial state
- **Impact:** Complex Nova blocks may lose state

### MEDIUM Risk

**3. Performance with Large Islands**
- **Risk:** Scanning 60M blocks takes too long
- **Mitigation:** Optimize scanning, async operations, progress feedback
- **Impact:** Slot switch/relocation takes minutes instead of seconds

**4. Multi-Dimension Complexity**
- **Risk:** Nova tile entities may reference blocks in other dimensions
- **Mitigation:** Capture all dimensions atomically, restore in order
- **Impact:** Cross-dimension Nova structures may break

### LOW Risk

**5. Block Placement Order**
- **Risk:** Nova blocks may depend on other Nova blocks being placed first
- **Mitigation:** Place in Y-order (bottom to top), check Nova docs for dependencies
- **Impact:** Some Nova multi-block structures may not restore correctly

**6. Chunk Loading Race Conditions**
- **Risk:** Nova blocks placed in unloaded chunks may not initialize
- **Mitigation:** Pre-load chunks before restoration, add delay after paste
- **Impact:** Nova blocks appear but don't function

## Dependencies

### Hard Dependencies
- BentoBox (provided by parent addon)
- BSkyBlock (required by IslandSelector)
- FAWE (required by IslandSelector for schematic operations)

### Soft Dependencies
- Nova plugin (detected at runtime via reflection)
  - Minimum version: 0.17 (API changed significantly)
  - Recommended: Latest stable
  - Detection method: Class.forName() with try-catch

### Integration Order
1. WorldEditIntegration must initialize first (FAWE detection)
2. NovaIntegration initializes after WorldEditIntegration
3. Managers depend on both integrations being available

## Alternative Approaches Considered

### Approach 1: WorldEdit Native Nova Support
**Description:** Wait for WorldEdit/FAWE to add native Nova support
**Pros:** No custom code needed, guaranteed compatibility
**Cons:** No ETA, may never happen, doesn't solve the problem
**Verdict:** Not viable

### Approach 2: Nova Schematic Format Extension
**Description:** Create a new schematic format that includes Nova data
**Pros:** Clean separation, could be shared with other plugins
**Cons:** Requires coordinating with WorldEdit/Nova teams, major undertaking
**Verdict:** Too complex for this use case

### Approach 3: Database-Based Storage
**Description:** Store Nova block data in BentoBox database instead of .nova.dat files
**Pros:** Cleaner for large numbers of blocks, better query capabilities
**Cons:** Increases database size, harder to debug, doesn't match existing pattern
**Verdict:** Over-engineered for typical usage (50-100 blocks per island)

### Approach 4: Hook Nova Events Instead of Capture/Restore
**Description:** Listen to Nova block place/break events and maintain a live registry
**Pros:** No scanning needed, always up-to-date
**Cons:** Event overhead, memory overhead, doesn't handle existing islands
**Verdict:** Good optimization for future, not suitable for initial implementation

**Selected Approach:** File-based capture/restore parallel to EntityStorage (matches existing architecture)

## Sources

Research based on:
- [Nova 0.16 to 0.17 Migration Guide](https://docs.xenondevs.xyz/nova/addon/migration-guide/0.16-0.17/) - API changes
- [Nova GitHub Repository](https://github.com/xenondevs/Nova) - Source code reference
- [Nova Modrinth Page](https://modrinth.com/plugin/nova-framework) - Plugin overview
- IslandSelector codebase analysis - Existing patterns
- FAWE documentation - WorldEdit limitations
- EntityStorage.java implementation - Parallel pattern reference
