# Nova Integration Pitfalls

**Domain:** Nova custom block integration for world manipulation plugins
**Researched:** 2026-01-26
**Confidence:** MEDIUM

This document catalogs common mistakes when integrating Nova custom blocks with world manipulation systems like island relocation, slot switching, and backup/restore operations.

---

## Critical Pitfalls

Mistakes that cause data loss or require rewrites.

### Pitfall 1: Using WorldEdit/FAWE to Copy Nova Blocks

**What goes wrong:** WorldEdit and FastAsyncWorldEdit cannot copy Nova blocks. Nova stores custom block data separately from Minecraft's NBT system, so schematic operations will lose all Nova blocks, converting them to air or base Minecraft blocks.

**Why it happens:** Nova uses a custom NBT format (CBFCompoundTag) that WorldEdit doesn't recognize. The incompatibility is documented in [Nova GitHub Issue #252](https://github.com/xenondevs/Nova/issues/252). WorldEdit's type checking expects standard Minecraft tag types and throws `IllegalArgumentException: Don't know how to make native xyz.xenondevs.nova.data.serialization.cbf.CBFCompoundTag`.

**Consequences:**
- All Nova machines, custom blocks, and stored data disappear during island operations
- Player inventory items and energy stored in machines are lost
- No error messages - operations appear successful but data is silently lost

**Prevention:**
1. Implement separate Nova block capture system that runs alongside FAWE operations
2. Use Nova's `WorldDataManager.getBlockState()` to scan regions for Nova blocks
3. Store Nova block locations and IDs separately from schematics
4. Restore Nova blocks AFTER FAWE paste completes

**Detection:**
- Players report machines disappearing after relocation/slot switch
- Nova blocks present before operation, air blocks after
- Console shows no errors (silent data loss)

**Phase Impact:** Must be implemented in initial Nova integration phase. Cannot be retrofitted without causing data loss.

---

### Pitfall 2: Missing TileEntity Inventory State

**What goes wrong:** Nova blocks store inventory, energy, and progress state in TileEntity data. Capturing only the block ID without TileEntity state loses all stored items and machine progress.

**Why it happens:** The current implementation only captures block IDs via `NovaBlockState.getId()`. Nova TileEntities have complex state (inventories, energy storage, processing progress) that requires `TileEntity.getDrops(includeSelf=true)` to properly preserve.

**Consequences:**
- Machine inventories empty after restoration
- Energy storage reset to zero
- Processing progress (smelting, crafting) lost
- Upgrade modules and configurations reset

**Prevention:**
```java
// WRONG - Only captures block type
Object blockState = worldDataManager.getBlockState(location);
String blockId = blockState.getId();

// RIGHT - Capture full TileEntity state
Object tileEntity = tileEntityManager.getTileEntityAt(location);
if (tileEntity != null) {
    List<ItemStack> drops = tileEntity.getDrops(true); // Includes inventory + block
    // Store drops for restoration
}
```

**Detection:**
- Players report empty machines after slot switch
- Energy levels reset after relocation
- Crafting progress lost mid-operation

**Phase Impact:** Critical for Phase 2 (TileEntity state preservation). Current implementation incomplete.

---

### Pitfall 3: Not Using BlockManager for Removal

**What goes wrong:** Using Bukkit's `Block.setType(Material.AIR)` or WorldEdit operations to remove Nova blocks leaves orphaned TileEntity data in Nova's internal storage, causing memory leaks and ghost blocks.

**Why it happens:** Nova maintains separate data structures for TileEntity state. Only Nova's BlockManager knows how to properly clean up all associated data.

**Consequences:**
- Memory leaks from orphaned TileEntity data
- Ghost blocks that appear in Nova's data but not in world
- Chunk data corruption over time
- Nova's WorldDataManager size grows unbounded

**Prevention:**
```java
// WRONG - Leaves orphaned data
block.setType(Material.AIR);

// RIGHT - Use Nova's BlockManager
Class<?> blockManagerClass = Class.forName("xyz.xenondevs.nova.world.block.BlockManager");
Object blockManager = blockManagerClass.getField("INSTANCE").get(null);
Method removeBlockMethod = blockManagerClass.getMethod(
    "removeBlock",
    Location.class,
    Class.forName("xyz.xenondevs.nova.context.Context"),
    boolean.class // breakEffects
);
removeBlockMethod.invoke(blockManager, location, context, false);
```

**Detection:**
- Nova data files grow continuously
- Performance degrades over time
- `/nova debug` shows TileEntities at locations with air blocks

**Phase Impact:** Must be implemented in Phase 3 (proper cleanup). Current implementation does not remove Nova blocks before WorldEdit operations.

---

### Pitfall 4: Chunk Unload During Async Operations

**What goes wrong:** Nova TileEntity state can be lost if chunks unload during async capture operations. Nova 0.18 fixed an issue where "tile entity data was not saved properly" and "energy side configuration of tile entities would reset on every chunk load."

**Why it happens:** Async operations take time (5-30 seconds for large islands). If chunks unload during capture, TileEntity state may not be fully serialized to Nova's storage yet, resulting in partial or missing data.

**Consequences:**
- Intermittent data loss (works sometimes, fails other times)
- Hard to reproduce bugs
- Players lose progress seemingly at random

**Prevention:**
1. Pre-load all chunks before starting capture
2. Keep chunks loaded during entire operation
3. Add timeout protection (30 seconds max)
4. Verify chunks still loaded before each block access

```java
// Current implementation already does this correctly
List<CompletableFuture<Chunk>> chunkFutures = new ArrayList<>();
for (int cx = minChunkX; cx <= maxChunkX; cx++) {
    for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
        chunkFutures.add(world.getChunkAtAsync(cx, cz));
    }
}
CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]))
    .orTimeout(30, TimeUnit.SECONDS)
    .thenRun(() -> {
        // Perform capture on main thread
    });
```

**Detection:**
- Players report "sometimes machines keep items, sometimes they don't"
- No consistent reproduction steps
- Issues more common on busy servers (more chunk unloads)

**Phase Impact:** Already handled in current implementation. Verify in testing phase.

---

## Moderate Pitfalls

Mistakes that cause delays or technical debt.

### Pitfall 5: Missing Context Parameter for Block Placement

**What goes wrong:** Nova's `BlockUtils.placeBlock()` requires a `Context` parameter. Using null or wrong context causes blocks to place without proper initialization, missing drop tables, storage configurations, or network endpoints.

**Why it happens:** Nova 0.17+ migration changed API to require Context for all block operations. The Context provides environment information needed for proper block initialization.

**Consequences:**
- Blocks place but don't function correctly
- Drop tables not initialized (blocks drop nothing when broken)
- Network-connected blocks don't connect to adjacent blocks
- Storage configurations missing

**Prevention:**
```java
// WRONG - No context
BlockUtils.placeBlock(location, novaBlock, false);

// RIGHT - Provide proper context
Class<?> contextClass = Class.forName("xyz.xenondevs.nova.context.Context");
Object context = contextClass.getMethod("empty").invoke(null);
BlockUtils.placeBlock(context, location, novaBlock, false);
```

**Detection:**
- Nova blocks place but are "non-interactive"
- Breaking Nova blocks drops nothing
- Logistics pipes don't connect after restoration

**Phase Impact:** Fix in Phase 2 (proper restoration). Current implementation attempts to find Context but uses unsafe fallback.

---

### Pitfall 6: API Version Detection Without Fallbacks

**What goes wrong:** Nova API location changed between versions (0.16 → 0.17). Code that only checks one location fails on older/newer Nova versions.

**Why it happens:** Nova underwent major refactoring in 0.17:
- `xyz.xenondevs.nova.data.world.block.BlockManager` → `xyz.xenondevs.nova.world.block.BlockManager`
- Plugin-API (nova-api module) converted from Kotlin to Java
- Early initialization phases moved to Bootstrap phase

**Consequences:**
- Integration works on some server versions but not others
- Hard to diagnose ("works on my server")
- Support burden from version mismatches

**Prevention:**
```java
// Current implementation already does this correctly
try {
    blockManagerClass = Class.forName("xyz.xenondevs.nova.world.block.BlockManager");
} catch (ClassNotFoundException e) {
    // Try older API location
    try {
        blockManagerClass = Class.forName("xyz.xenondevs.nova.data.world.block.BlockManager");
    } catch (ClassNotFoundException e2) {
        addon.logWarning("Nova detected but BlockManager class not found");
        return false;
    }
}
```

**Detection:**
- `ClassNotFoundException` in logs
- Integration works on test server but fails on production
- Different Nova versions between environments

**Phase Impact:** Already handled in current implementation. Document in compatibility matrix.

---

### Pitfall 7: Synchronous Operations on Main Thread

**What goes wrong:** Nova block operations (especially `TileEntity.getDrops()`) can be expensive. Running synchronously on main thread during large island operations causes server freezes.

**Why it happens:** Scanning 400x400 region with 256 height = 40.96M blocks to check. Even if only 1% are Nova blocks, that's 409K TileEntity queries.

**Consequences:**
- Server TPS drops during relocation/slot switch
- Players experience lag
- Timeouts on slower servers

**Prevention:**
1. Async chunk loading (already implemented)
2. Batch block checking (100 blocks per tick)
3. Progress reporting to prevent timeout detection
4. Offload scanning to async thread where safe

**Current Implementation Status:** Chunk loading is async, but block scanning runs synchronously after chunks load. Could be optimized further.

**Detection:**
- `/timings` shows high CPU in IslandSelector during operations
- Players report "server freezes when I relocate"
- TPS drops from 20 to 10-15 during operations

**Phase Impact:** Optimization phase (Phase 4). Not critical for MVP but needed for production.

---

### Pitfall 8: Serialization Format Not Future-Proof

**What goes wrong:** Current `NovaBlockData` only stores block ID and position. No version field. If Nova changes block ID format or adds required metadata, old serialized data becomes incompatible.

**Why it happens:** Simple serialization design didn't account for schema evolution.

**Consequences:**
- Can't migrate old slot data after Nova updates
- Players lose saved slots after server updates
- No way to detect or handle incompatible data

**Prevention:**
```java
public static class NovaBlockData implements Serializable {
    private static final long serialVersionUID = 2L; // Bump version

    public final int version = 2; // Schema version
    public final int relX, relY, relZ;
    public final String blockId;
    public final Map<String, Object> metadata; // Future extensibility

    // Add migration logic
    private Object readResolve() {
        if (version == 1) {
            // Migrate from v1 to v2
        }
        return this;
    }
}
```

**Detection:**
- `InvalidClassException` when loading old slot data
- Players report "cannot restore slot after update"

**Phase Impact:** Fix in Phase 5 (schema versioning). Breaking change requires data migration.

---

## Minor Pitfalls

Mistakes that cause annoyance but are fixable.

### Pitfall 9: No Verification of Restored Blocks

**What goes wrong:** Current implementation counts `restored++` for each block placed but doesn't verify the block actually placed successfully. Nova block placement can fail silently if registry lookup fails or block is no longer registered.

**Why it happens:** Missing error handling in restoration loop.

**Consequences:**
- Misleading success messages ("Restored 50/50 blocks" when only 30 succeeded)
- Players don't know which blocks failed to restore
- No way to diagnose restoration failures

**Prevention:**
```java
Object result = placeBlockMethod.invoke(null, finalContext, loc, novaBlock, false);
// Check if placement succeeded
Object verifyState = worldDataManager.getBlockState(loc);
if (verifyState != null && verifyState.getId().equals(data.blockId)) {
    restored++;
} else {
    failed.add(data.blockId);
}
```

**Detection:**
- Player reports match log messages (says restored 50, player sees 30)
- Specific Nova blocks consistently missing after restoration

**Phase Impact:** Quality improvement (Phase 6). Not critical but improves user experience.

---

### Pitfall 10: Reflection Method Caching Not Implemented

**What goes wrong:** Every block operation re-looks up reflection methods. For 1000 Nova blocks, that's 1000 redundant `Class.forName()` and `getMethod()` calls.

**Why it happens:** Reflection lookups happen inside loops instead of being cached at initialization.

**Consequences:**
- Slower operations (each reflection lookup takes ~0.1ms)
- For 1000 blocks: +100ms overhead
- CPU waste on repeated class loading

**Prevention:**
```java
// Cache at initialization
private Method placeBlockMethod;
private Method getBlockStateMethod;
private Object blockRegistry;

private void initializeReflectionCache() {
    Class<?> blockUtilsClass = Class.forName("xyz.xenondevs.nova.util.BlockUtils");
    this.placeBlockMethod = blockUtilsClass.getMethod("placeBlock", ...);
    // Cache other methods
}

// Use cached methods in loops
for (NovaBlockData data : novaBlocks) {
    placeBlockMethod.invoke(null, ...); // No lookup!
}
```

**Detection:**
- Profiling shows high time in `Class.forName()`
- Operations slower than expected on large islands

**Phase Impact:** Optimization phase (Phase 4). Easy win for performance.

---

### Pitfall 11: No Rate Limiting for Block Placement

**What goes wrong:** Placing hundreds of Nova blocks at once (synchronously on main thread) can cause TPS drops. Nova's block placement triggers multiple events and updates per block.

**Why it happens:** Current restoration runs all placements in single tick.

**Consequences:**
- TPS spikes during restoration
- Players experience momentary lag
- Server watchdog warnings on slower servers

**Prevention:**
```java
// Batch block placement: 50 blocks per tick
int batchSize = 50;
List<List<NovaBlockData>> batches = Lists.partition(novaBlocks, batchSize);

AtomicInteger batchIndex = new AtomicInteger(0);
BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    int index = batchIndex.getAndIncrement();
    if (index >= batches.size()) {
        task.cancel();
        callback.accept(true);
        return;
    }

    List<NovaBlockData> batch = batches.get(index);
    for (NovaBlockData data : batch) {
        // Place block
    }
}, 0L, 1L); // 1 tick between batches
```

**Detection:**
- TPS drops from 20 to 18-19 during restoration
- Watchdog warnings: "Task took Xms"

**Phase Impact:** Optimization phase (Phase 4). Not critical for small islands.

---

### Pitfall 12: Silent Failures in Reflection

**What goes wrong:** Reflection exceptions caught with empty catch blocks or generic "Failed to restore" messages don't provide actionable information.

**Why it happens:** Reflection is fragile and can fail for many reasons (class not found, method signature changed, security manager, etc.).

**Consequences:**
- Difficult debugging ("it just doesn't work")
- No way to diagnose which reflection call failed
- Can't distinguish between "Nova not installed" and "Nova API changed"

**Prevention:**
```java
// BAD
try {
    // Reflection operations
} catch (Exception e) {
    addon.logWarning("Failed to restore Nova blocks");
}

// GOOD
try {
    // Reflection operations
} catch (ClassNotFoundException e) {
    addon.logWarning("Nova class not found - API may have changed: " + e.getMessage());
    addon.logWarning("Please report this to IslandSelector developers with Nova version");
} catch (NoSuchMethodException e) {
    addon.logWarning("Nova method not found - expected: " + e.getMessage());
} catch (IllegalAccessException e) {
    addon.logWarning("Cannot access Nova API - security issue: " + e.getMessage());
}
```

**Detection:**
- Generic error messages in logs
- Users report "doesn't work" without details
- Multiple possible root causes

**Phase Impact:** Quality improvement (Phase 6). Helps support and debugging.

---

## Phase-Specific Warnings

| Phase | Likely Pitfall | Mitigation |
|-------|---------------|------------|
| Phase 1: Basic Integration | Pitfall #1 (FAWE incompatibility) | Implement separate Nova capture system |
| Phase 2: TileEntity State | Pitfall #2 (Missing inventory), Pitfall #5 (Context) | Use `TileEntity.getDrops()`, proper Context |
| Phase 3: Cleanup | Pitfall #3 (BlockManager removal) | Call Nova's removeBlock before FAWE operations |
| Phase 4: Optimization | Pitfall #7 (Sync operations), Pitfall #10 (Caching) | Batch operations, cache reflection |
| Phase 5: Data Schema | Pitfall #8 (Serialization) | Add versioning to NovaBlockData |
| Phase 6: Quality | Pitfall #9 (Verification), Pitfall #12 (Error handling) | Verify placements, detailed logging |

---

## Testing Checklist

Before marking Nova integration complete, verify:

- [ ] Nova blocks survive island relocation (all types tested)
- [ ] Machine inventories preserved during slot switch
- [ ] Energy levels maintained after backup/restore
- [ ] Processing progress (furnaces, etc.) preserved
- [ ] Multiple Nova addons tested (Machines, Logistics, etc.)
- [ ] Chunk unload during operation doesn't cause data loss
- [ ] Works with both Nova 0.16 and 0.17+ APIs
- [ ] No memory leaks after 100 relocations
- [ ] TPS remains above 19 during operations
- [ ] Error messages provide actionable information

---

## Sources

**HIGH Confidence:**
- [Nova GitHub Issue #252 - FAWE Incompatibility](https://github.com/xenondevs/Nova/issues/252)
- [Nova 0.17-0.18 Migration Guide](https://docs.xenondevs.xyz/nova/addon/migration-guide/0.17-0.18/)
- [Nova TileEntity Documentation](https://docs.xenondevs.xyz/nova/api/tileentity/tileentity/)

**MEDIUM Confidence:**
- [Nova 0.18 Changelog - TileEntity save fix](https://hangar.papermc.io/xenondevs/Nova/versions/0.18)
- [Nova 0.16-0.17 Migration - API refactoring](https://docs.xenondevs.xyz/nova/addon/migration-guide/0.16-0.17/)
- [FAWE Race Condition Discussion](https://www.spigotmc.org/threads/async-race-conditions.439592/)

**LOW Confidence (requires validation):**
- General Minecraft TileEntity chunk unload behavior patterns
- Performance characteristics based on typical island sizes
- Reflection caching impact estimates

---

## Recommended Implementation Order

Based on severity and dependencies:

1. **Phase 1:** Separate Nova capture (Pitfall #1) - CRITICAL
2. **Phase 2:** TileEntity state preservation (Pitfall #2) - CRITICAL
3. **Phase 3:** Proper cleanup via BlockManager (Pitfall #3) - CRITICAL
4. **Phase 2:** Context parameter handling (Pitfall #5) - HIGH
5. **Phase 1:** Version fallbacks (Pitfall #6) - Already done
6. **Phase 4:** Async optimization (Pitfall #7) - MEDIUM
7. **Phase 5:** Schema versioning (Pitfall #8) - MEDIUM
8. **Phase 4:** Reflection caching (Pitfall #10) - LOW
9. **Phase 6:** Verification & error handling (Pitfall #9, #12) - LOW
10. **Phase 4:** Rate limiting (Pitfall #11) - LOW

Critical items must be done before release. Medium items should be done for production. Low items are quality-of-life improvements.
