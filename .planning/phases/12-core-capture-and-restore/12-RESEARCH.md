# Phase 12: Core Capture and Restore - Research

**Researched:** 2026-01-27
**Domain:** Nova 0.17+ block capture/restore lifecycle with WorldEdit FAWE workaround
**Confidence:** MEDIUM

## Summary

This phase implements the core Nova block capture and restore system required before island operations (relocation, slot switching, backups). Nova uses "backing blocks" (vanilla Minecraft blocks like mushroom blocks or note blocks) to implement custom blocks without client mods. WorldEdit/FAWE cannot copy Nova blocks because Nova stores block data separately from Minecraft's NBT system in WorldDataManager.

The existing NovaIntegration.java already implements a working capture/restore pattern but needs enhancement:
1. Uses WorldDataManager.getBlockState() to scan regions for Nova blocks (correct for 0.17+)
2. Stores block IDs and relative positions in NovaBlockData (drop-based approach not yet used)
3. Restores blocks via BlockUtils.placeBlock() with Context (correct for 0.17+)
4. Missing: Proper removal via BlockUtils.breakBlock() before WorldEdit operations
5. Missing: TileEntity.getDrops(includeSelf=true) for state preservation (inventory, owner, etc.)

Key findings:
- Nova 0.17+ removed BlockManager, use BlockUtils.breakBlock() and BlockUtils.placeBlock() static methods
- TileEntity.getDrops(includeSelf=true) returns all inventory items + the block itself as ItemStacks
- BlockUtils requires a Context object (use Context.EMPTY for non-player operations)
- Backing blocks (mushroom/note blocks) must be preserved - they become air if Nova removed without proper cleanup
- Lifecycle order: scan → capture drops → breakBlock → WorldEdit paste → placeBlock + restore drops

**Primary recommendation:** Extend NovaIntegration to capture TileEntity drops before removal, properly remove blocks via BlockUtils.breakBlock(), and restore both blocks and drops after WorldEdit operations complete. Use drop-based state preservation (Nova's own serialization) rather than direct tile entity data manipulation.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Nova | 0.17+ | Server-side custom blocks/items framework | Target integration version with WorldDataManager API |
| Java Reflection | Built-in | Optional Nova integration | Avoid hard dependency, graceful degradation |
| WorldDataManager | Nova 0.17+ | Block state queries in regions | Replaced TileEntityManager, official 0.17+ API |
| BlockUtils | Nova 0.17+ | Place/break Nova blocks | Replaced BlockManager, official 0.17+ API |
| TileEntity | Nova API | State serialization via getDrops() | Nova's own serialization for inventory/owner/state |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Context | Nova 0.17+ | Required for BlockUtils operations | Empty context for automated operations |
| NovaRegistries.BLOCK | Nova API | Block lookup by namespaced ID | Restore blocks from captured IDs |
| Adventure Key | Nova dependency | Parse block IDs (namespace:path) | Convert string IDs to Key objects |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Drop-based preservation | Direct TileEntity NBT access | Nova docs state drops are official serialization, NBT is internal |
| BlockUtils.breakBlock() | Direct backing block removal | Loses Nova lifecycle hooks, breaks state cleanup |
| WorldDataManager scan | Chunk entity iteration | Misses Nova blocks that aren't entities, slower |

**Installation:**
```bash
# No new dependencies - already using Nova via reflection
# Current NovaIntegration.java lines 46-91 handles detection
```

## Architecture Patterns

### Recommended Code Organization
```
src/main/java/world/bentobox/islandselector/
├── integrations/
│   └── NovaIntegration.java           # Extend with capture/remove/restore methods
└── managers/
    ├── RelocationManager.java         # Will call Nova integration (Phase 14)
    ├── SlotSwitchManager.java         # Will call Nova integration (Phase 14)
    └── BackupManager.java             # Will call Nova integration (Phase 14)
```

### Pattern 1: Region-Based Block Scanning

**What:** Iterate all blocks in island region, check each with WorldDataManager.getBlockState()
**When to use:** Before any WorldEdit operation on an island
**Example:**
```java
// Source: NovaIntegration.java lines 108-176 (current implementation)
// Enhancement needed: Also capture TileEntity drops

for (int x = centerX - range; x <= centerX + range; x++) {
    for (int z = centerZ - range; z <= centerZ + range; z++) {
        for (int y = minY; y < maxY; y++) {
            Location loc = new Location(world, x, y, z);

            // Check if this location has a Nova block
            Object blockState = getBlockStateMethod.invoke(worldDataManager, loc);

            if (blockState != null) {
                // Get block ID
                Object blockId = getIdMethod.invoke(blockState);

                // NEW: Get TileEntity if this is a tile entity block
                Object tileEntity = getTileEntityMethod.invoke(worldDataManager, loc);
                List<ItemStack> drops = null;
                if (tileEntity != null) {
                    // Capture drops (includes inventory + block item)
                    drops = (List<ItemStack>) getDropsMethod.invoke(tileEntity, true);
                }

                // Store relative position, block ID, and drops
                NovaBlockData data = new NovaBlockData(
                    x - centerX, y - centerY, z - centerZ,
                    blockId.toString(),
                    drops  // NEW: serialize drops for restoration
                );
                novaBlocks.add(data);
            }
        }
    }
}
```

### Pattern 2: Proper Block Removal Lifecycle

**What:** Remove Nova blocks via BlockUtils.breakBlock() BEFORE WorldEdit operations
**When to use:** After capturing block data, before WorldEdit paste/copy
**Example:**
```java
// Source: Nova 0.17 migration guide - BlockUtils.breakBlock usage
// NEW method needed in NovaIntegration.java

public void removeNovaBlocks(List<NovaBlockData> novaBlocks, Location center) {
    Class<?> blockUtilsClass = Class.forName("xyz.xenondevs.nova.util.BlockUtils");
    Class<?> contextClass = Class.forName("xyz.xenondevs.nova.context.Context");

    // Get empty context for automated operations (no player involved)
    Object emptyContext = contextClass.getField("EMPTY").get(null);

    // Get breakBlock method
    Method breakBlockMethod = blockUtilsClass.getMethod(
        "breakBlock",
        contextClass,
        Location.class,
        boolean.class  // breakEffects parameter
    );

    for (NovaBlockData data : novaBlocks) {
        Location loc = new Location(world,
            center.getBlockX() + data.relX,
            center.getBlockY() + data.relY,
            center.getBlockZ() + data.relZ);

        // Break the Nova block properly (no effects, automated)
        breakBlockMethod.invoke(null, emptyContext, loc, false);
    }
}
```

### Pattern 3: Block + Drop Restoration

**What:** Restore blocks via BlockUtils.placeBlock(), then restore drops to TileEntity
**When to use:** After WorldEdit paste completes
**Example:**
```java
// Source: Nova docs - BlockUtils.placeBlock + TileEntity drop restoration
// Enhancement to existing NovaIntegration.java restoreNovaBlocks() lines 234-336

// First pass: Place all Nova blocks
for (NovaBlockData data : novaBlocks) {
    Location loc = new Location(world,
        centerX + data.relX, centerY + data.relY, centerZ + data.relZ);

    // Place the block (existing code lines 295-320)
    Object novaBlock = getMethod.invoke(blockRegistry, key);
    placeBlockMethod.invoke(null, finalContext, loc, novaBlock, false);
}

// Second pass: Restore drops to TileEntities
for (NovaBlockData data : novaBlocks) {
    if (data.drops == null || data.drops.isEmpty()) continue;

    Location loc = new Location(world,
        centerX + data.relX, centerY + data.relY, centerZ + data.relZ);

    // Get the newly placed TileEntity
    Object tileEntity = getTileEntityMethod.invoke(worldDataManager, loc);

    if (tileEntity != null) {
        // Restore inventory from drops (Phase 13 will implement this)
        // For now, just place the block - inventory restoration comes later
    }
}
```

### Pattern 4: Async Chunk Loading Before Operations

**What:** Load all chunks in region before scanning/removing/restoring
**When to use:** For large islands or when operations span multiple chunks
**Example:**
```java
// Source: NovaIntegration.java lines 181-226 (existing async pattern)
// This pattern already works correctly - keep it

CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]))
    .orTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
    .thenRun(() -> {
        // Chunks loaded, now perform operation on main thread
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            // Call capture/remove/restore here
        });
    });
```

### Anti-Patterns to Avoid

- **Removing backing blocks directly:** Don't use Bukkit Block.setType(AIR) on Nova blocks. Must use BlockUtils.breakBlock() to trigger Nova lifecycle.
- **Skipping removal step:** Don't capture → WorldEdit → restore without removing first. WorldEdit will copy the backing block (mushroom/note block) but lose Nova data.
- **Synchronous chunk loading in scan loop:** Causes main thread lag. Load all chunks first with async API.
- **Using null Context:** BlockUtils requires Context object. Use Context.EMPTY for automated operations, not null.
- **Restoring drops to wrong block:** Must place block FIRST, then restore drops to the TileEntity. Can't restore drops before block exists.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Nova block state serialization | Custom NBT reading/writing | TileEntity.getDrops(includeSelf=true) | Nova's official serialization, handles all state types |
| Block removal lifecycle | Direct backing block deletion | BlockUtils.breakBlock() | Triggers Nova cleanup hooks, prevents backing block corruption |
| Block placement lifecycle | Direct backing block placement + data | BlockUtils.placeBlock() | Handles backing state selection, display entity spawning |
| Region iteration optimization | Custom chunk batching | Existing async pattern lines 181-226 | Already handles chunk loading, timeout, main thread safety |
| Block ID parsing | String splitting on ":" | Adventure Key.key(String) | Handles namespace parsing, validation, edge cases |

**Key insight:** Nova provides official serialization via drops system. Don't try to access internal TileEntity data structures - they're Kotlin-specific and version-dependent. Drops are stable API.

## Common Pitfalls

### Pitfall 1: Skipping BlockUtils.breakBlock() Before WorldEdit

**What goes wrong:** WorldEdit copies backing blocks (mushroom/note blocks) but Nova data lost, blocks become vanilla
**Why it happens:** WorldEdit only knows about Minecraft blocks, not Nova's separate data system
**How to avoid:** Always call removeNovaBlocks() after capture, before WorldEdit operations. Lifecycle: scan → capture → remove → WorldEdit → restore
**Warning signs:** After island move, Nova blocks appear as mushroom blocks or note blocks instead of custom blocks

### Pitfall 2: Using Wrong Context Type

**What goes wrong:** BlockUtils.breakBlock() or placeBlock() throws IllegalArgumentException or NullPointerException
**Why it happens:** Context has multiple types (player-based, item-based, empty). Automated operations need Context.EMPTY.
**How to avoid:** For non-player operations (backups, relocation, slot switching), use Context.EMPTY via reflection:
```java
Class<?> contextClass = Class.forName("xyz.xenondevs.nova.context.Context");
Object emptyContext = contextClass.getField("EMPTY").get(null);
```
**Warning signs:** NullPointerException when calling BlockUtils methods, or "Context required" error messages

### Pitfall 3: TileEntity.getDrops() Returns Null for Non-TileEntity Blocks

**What goes wrong:** NullPointerException when calling getDrops() on blocks that aren't tile entities
**Why it happens:** Not all Nova blocks have tile entities. Simple blocks (decorative) don't have inventory/state.
**How to avoid:** Check if WorldDataManager.getTileEntity() returns non-null before calling getDrops():
```java
Object tileEntity = getTileEntityMethod.invoke(worldDataManager, loc);
if (tileEntity != null) {
    List<ItemStack> drops = (List<ItemStack>) getDropsMethod.invoke(tileEntity, true);
    // Store drops
}
```
**Warning signs:** Sporadic NullPointerException during capture, only on some Nova blocks

### Pitfall 4: Backing Block Becomes Air After Removal

**What goes wrong:** After BlockUtils.breakBlock(), backing block location becomes air, but block should be replaced by WorldEdit
**Why it happens:** This is EXPECTED behavior. BlockUtils.breakBlock() removes both Nova data and backing block. WorldEdit will paste new blocks there.
**How to avoid:** This is not a bug - it's the correct lifecycle. Don't try to preserve backing blocks. Flow: remove (becomes air) → WorldEdit paste (places new blocks) → restore Nova data.
**Warning signs:** None - if backing blocks become air after removal, that's correct. If they DON'T become air, breakBlock() didn't work.

### Pitfall 5: Restoring Blocks Before WorldEdit Paste

**What goes wrong:** Nova blocks placed, then WorldEdit overwrites them with schematic blocks
**Why it happens:** Wrong operation order
**How to avoid:** Lifecycle MUST be: capture → remove → WorldEdit paste → restore. Never restore before paste.
**Warning signs:** Nova blocks don't appear after operation, or appear briefly then vanish

### Pitfall 6: includeSelf=false Loses Block Item

**What goes wrong:** TileEntity inventory restored but block itself not present in drops
**Why it happens:** getDrops(false) only returns inventory contents, not the block item
**How to avoid:** Always use getDrops(true) - the "self" is the Nova block item. Need it to restore the block correctly.
**Warning signs:** After restoration, Nova machines have inventory but are gone (destroyed during operation)

### Pitfall 7: Scanning Y-Axis from 0 Instead of minHeight

**What goes wrong:** In 1.18+ with expanded world height, blocks below Y=0 not captured
**Why it happens:** Hardcoded Y loop starting at 0
**How to avoid:** Use world.getMinHeight() and world.getMaxHeight() (existing code lines 123-124 does this correctly)
**Warning signs:** Nova blocks in negative Y coordinates not preserved

## Code Examples

Verified patterns from Nova 0.17+ API:

### Example 1: Enhanced Capture with TileEntity Drops

```java
// Location: NovaIntegration.java captureNovaBlocks() method
// Enhancement to existing lines 108-176

public List<NovaBlockData> captureNovaBlocks(Location center, int range) {
    List<NovaBlockData> novaBlocks = new ArrayList<>();

    if (!available) return novaBlocks;

    try {
        // Get WorldDataManager (existing code)
        Class<?> worldDataManagerClass = Class.forName("xyz.xenondevs.nova.world.format.WorldDataManager");
        Object worldDataManager = worldDataManagerClass.getField("INSTANCE").get(null);

        Method getBlockStateMethod = worldDataManagerClass.getMethod("getBlockState", Location.class);
        Method getTileEntityMethod = worldDataManagerClass.getMethod("getTileEntity", Location.class);

        // NEW: Get TileEntity class for getDrops() method
        Class<?> tileEntityClass = Class.forName("xyz.xenondevs.nova.world.block.tileentity.TileEntity");
        Method getDropsMethod = tileEntityClass.getMethod("getDrops", boolean.class);

        // Scan region (existing loop structure)
        for (int x = centerX - range; x <= centerX + range; x++) {
            for (int z = centerZ - range; z <= centerZ + range; z++) {
                for (int y = minY; y < maxY; y++) {
                    Location loc = new Location(world, x, y, z);

                    Object blockState = getBlockStateMethod.invoke(worldDataManager, loc);
                    if (blockState != null) {
                        // Get block ID (existing)
                        Method getIdMethod = blockState.getClass().getMethod("getId");
                        Object blockId = getIdMethod.invoke(blockState);

                        // NEW: Capture drops if this is a TileEntity
                        List<ItemStack> drops = null;
                        Object tileEntity = getTileEntityMethod.invoke(worldDataManager, loc);
                        if (tileEntity != null) {
                            drops = (List<ItemStack>) getDropsMethod.invoke(tileEntity, true);
                        }

                        NovaBlockData data = new NovaBlockData(
                            x - centerX, y - centerY, z - centerZ,
                            blockId.toString(),
                            drops  // NEW: store drops
                        );
                        novaBlocks.add(data);
                    }
                }
            }
        }

        addon.log("Captured " + novaBlocks.size() + " Nova blocks with state data");

    } catch (Exception e) {
        addon.logWarning("Failed to capture Nova blocks: " + e.getMessage());
    }

    return novaBlocks;
}
```

### Example 2: NEW Remove Method Using BlockUtils.breakBlock()

```java
// Location: NovaIntegration.java - NEW METHOD
// Call this AFTER capture, BEFORE WorldEdit operations

public void removeNovaBlocks(List<NovaBlockData> novaBlocks, Location center) {
    if (!available || novaBlocks == null || novaBlocks.isEmpty()) {
        return;
    }

    World world = center.getWorld();
    if (world == null) return;

    int removed = 0;

    try {
        // Load BlockUtils class
        Class<?> blockUtilsClass = Class.forName("xyz.xenondevs.nova.util.BlockUtils");

        // Get Context.EMPTY for automated operations
        Class<?> contextClass = Class.forName("xyz.xenondevs.nova.context.Context");
        Object emptyContext = contextClass.getField("EMPTY").get(null);

        // Get breakBlock method signature
        Method breakBlockMethod = blockUtilsClass.getMethod(
            "breakBlock",
            contextClass,
            Location.class,
            boolean.class  // breakEffects - false for silent removal
        );

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        for (NovaBlockData data : novaBlocks) {
            try {
                Location loc = new Location(world,
                    centerX + data.relX,
                    centerY + data.relY,
                    centerZ + data.relZ);

                // Break the block properly (silent, no drops - we already captured them)
                breakBlockMethod.invoke(null, emptyContext, loc, false);
                removed++;

            } catch (Exception e) {
                // Skip blocks that fail - may have been removed already
                if (addon.getSettings().isDebugEnabled()) {
                    addon.log("Failed to remove Nova block at " + data.relX + "," + data.relY + "," + data.relZ);
                }
            }
        }

        addon.log("Removed " + removed + "/" + novaBlocks.size() + " Nova blocks before WorldEdit operation");

    } catch (Exception e) {
        addon.logWarning("Failed to remove Nova blocks: " + e.getMessage());
    }
}
```

### Example 3: Enhanced NovaBlockData with Drops

```java
// Location: NovaIntegration.java - UPDATE EXISTING INNER CLASS (lines 357-369)

public static class NovaBlockData implements Serializable {
    private static final long serialVersionUID = 2L;  // Increment version

    public final int relX, relY, relZ;
    public final String blockId;
    public final List<ItemStack> drops;  // NEW: TileEntity drops for state preservation

    // NEW constructor with drops
    public NovaBlockData(int relX, int relY, int relZ, String blockId, List<ItemStack> drops) {
        this.relX = relX;
        this.relY = relY;
        this.relZ = relZ;
        this.blockId = blockId;
        this.drops = drops;
    }

    // Keep old constructor for backward compatibility
    public NovaBlockData(int relX, int relY, int relZ, String blockId) {
        this(relX, relY, relZ, blockId, null);
    }
}
```

### Example 4: Proper Operation Lifecycle Order

```java
// Source: Integration pattern for managers (Phase 14 will implement)
// This shows the CORRECT order - reference for Phase 14

// In RelocationManager, SlotSwitchManager, BackupManager:
if (addon.getNovaIntegration() != null) {
    NovaIntegration nova = addon.getNovaIntegration();

    // STEP 1: Capture (includes drops)
    List<NovaBlockData> novaBlocks = nova.captureNovaBlocks(oldCenter, range);

    // STEP 2: Remove (clean up before WorldEdit)
    nova.removeNovaBlocks(novaBlocks, oldCenter);

    // STEP 3: WorldEdit operation (copy/paste schematic)
    worldEdit.copyIsland(oldCenter, range);
    worldEdit.pasteIsland(newCenter);

    // STEP 4: Restore (place blocks + drops at new location)
    nova.restoreNovaBlocks(novaBlocks, newCenter);
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| BlockManager.removeBlock() | BlockUtils.breakBlock() | Nova 0.17 (2024) | Static method, requires Context parameter |
| Direct backing block removal | BlockUtils.breakBlock() lifecycle | Nova 0.17+ | Proper cleanup, prevents backing block corruption |
| Manual TileEntity NBT access | TileEntity.getDrops(true) | Nova best practice | Official serialization API, stable across versions |
| TileEntityManager.getTileEntity() | WorldDataManager.getTileEntity() | Nova 0.17 (2024) | Manager removed, use WorldDataManager method |
| Skip removal before WorldEdit | Capture → Remove → WorldEdit → Restore | Nova + WorldEdit integration pattern | Prevents data loss, correct lifecycle |

**Deprecated/outdated:**
- Direct manipulation of backing blocks (mushroom/note blocks) - Use BlockUtils lifecycle instead
- Assuming Nova blocks persist through WorldEdit - They don't, must capture/restore
- BlockManager class and methods - Removed in 0.17, use BlockUtils static methods

**Current code status:**
- NovaIntegration.java has working capture (lines 108-176) and restore (lines 234-336)
- Missing: TileEntity.getDrops() capture for state preservation
- Missing: removeNovaBlocks() method using BlockUtils.breakBlock()
- NovaBlockData needs drops field added
- Pattern already correct: async chunk loading, reflection-based API access

## Open Questions

1. **Does Context.EMPTY exist in all Nova 0.17+ versions?**
   - What we know: Nova 0.17 migration guide mentions Context required for BlockUtils
   - What's unclear: Is EMPTY field guaranteed to exist, or might some versions use different factory method?
   - Recommendation: Try Context.EMPTY first, fall back to empty() method if field not found. Document requirement.

2. **Are drops guaranteed serializable for storage?**
   - What we know: TileEntity.getDrops() returns List<ItemStack>, ItemStack is Bukkit class
   - What's unclear: Can ItemStack serialize via Java Serialization, or need custom serialization?
   - Recommendation: Test serialization in Phase 13. May need to convert to Base64 or use Bukkit serialization API.

3. **Does breakBlock() affect nearby Nova blocks?**
   - What we know: Breaking some block types triggers neighbor updates
   - What's unclear: Does BlockUtils.breakBlock() trigger neighbor updates that might break adjacent Nova blocks?
   - Recommendation: Remove blocks in reverse order (high Y to low Y) to minimize cascade effects. Test with machines next to each other.

4. **What happens if WorldEdit paste fails after removal?**
   - What we know: Blocks removed before paste, restore happens after
   - What's unclear: If paste fails or is cancelled, blocks are gone with no restore
   - Recommendation: Phase 12 focuses on happy path. Error recovery in Phase 14 (operation integration). Consider storing drops before removal for rollback.

5. **Do all Nova TileEntities support getDrops()?**
   - What we know: Nova documentation shows getDrops() on TileEntity base class
   - What's unclear: Do all subclasses properly implement it, or might some return empty list?
   - Recommendation: Treat null or empty drops as valid (simple blocks with no state). Don't fail operation if drops empty.

## Sources

### Primary (HIGH confidence)
- [Nova 0.16→0.17 Migration Guide](https://docs.xenondevs.xyz/nova/addon/migration-guide/0.16-0.17/) - BlockManager removed, use BlockUtils
- [Nova TileEntity API Documentation](https://docs.xenondevs.xyz/nova/api/tileentity/tileentity/) - getDrops(includeSelf) method
- [Nova Creating Blocks Documentation](https://docs.xenondevs.xyz/nova/addon/blocks/creating-blocks/) - Context requirement, BlockUtils usage
- NovaIntegration.java (lines 108-336) - Current implementation patterns
- Phase 11 RESEARCH.md - Nova 0.17+ package paths, API changes

### Secondary (MEDIUM confidence)
- [WorldDataManager Usage Patterns](https://docs.xenondevs.xyz/nova/addon/migration-guide/0.16-0.17/) - getTileEntity and getBlockState methods
- NovaIntegration.java async chunk loading pattern (lines 181-226) - Proven working implementation
- STATE.md v1.2 research findings - Drop-based approach recommendation

### Tertiary (LOW confidence)
- WebSearch: Nova backing blocks (mushroom/note blocks) - Community discussions about backing state system
- WebSearch: Nova WorldEdit incompatibility - FAWE compatibility issues mentioned in Nova v0.13 changelog
- WebSearch: BlockUtils API usage - Limited documentation, relies on migration guide references

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Nova 0.17+ API officially documented, methods verified in migration guide
- Architecture: MEDIUM - Lifecycle order derived from Nova docs + WorldEdit integration requirements, not explicitly documented
- Pitfalls: MEDIUM - Based on API understanding and common integration mistakes, not tested in practice
- Drop serialization: LOW - ItemStack serialization requirements not verified
- Context types: MEDIUM - Context.EMPTY field existence not verified in all 0.17+ versions

**Confidence MEDIUM overall because:**
- Official Nova migration guide confirms BlockUtils.breakBlock() and BlockUtils.placeBlock() (HIGH confidence)
- TileEntity.getDrops() documented in Nova API docs (HIGH confidence)
- Lifecycle order (capture → remove → WorldEdit → restore) is logical but not explicitly documented (MEDIUM confidence)
- Current NovaIntegration.java provides working patterns for capture/restore (HIGH confidence)
- But: Drop serialization for storage not verified (LOW confidence)
- But: Context.EMPTY field existence not verified across all 0.17+ versions (MEDIUM confidence)
- But: Haven't tested removal → WorldEdit → restore cycle in actual environment (need validation)

**Research date:** 2026-01-27
**Valid until:** 60 days - Nova 0.17+ API stable based on migration guides, but should revalidate if Nova 0.19+ releases with breaking changes
