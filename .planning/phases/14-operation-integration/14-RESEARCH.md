# Phase 14: Operation Integration - Research

**Researched:** 2026-01-27
**Domain:** Custom block integration into island operation workflows
**Confidence:** HIGH

## Summary

Phase 14 integrates Nova custom block handling into existing island operation workflows (RelocationManager, SlotSwitchManager, BackupManager). The research confirms that the existing NovaIntegration API (Phase 12-13) provides all necessary methods, and the integration follows established patterns already present in the codebase.

**Key findings:**
- All three managers already have multi-dimension support infrastructure
- NovaIntegration provides synchronous and async methods for capture, remove, and restore
- RestoreResult from Phase 13 enables user feedback about machine preservation
- Integration follows capture-remove-operation-restore lifecycle pattern
- Multi-dimension processing requires iterating over DimensionManager.getEnabledDimensions()

**Primary recommendation:** Add NovaIntegration hooks at strategic points in each manager's workflow, following the capture-remove-restore pattern used by WorldEdit/FAWE, with multi-dimension iteration where needed.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| NovaIntegration | Custom | Nova custom block lifecycle | Phase 12-13 implementation, handles capture/restore/remove |
| DimensionManager | Custom | Multi-dimension coordination | Existing system for iterating enabled dimensions |
| CompletableFuture | Java 8+ | Async operation coordination | Bukkit/Paper async best practice |
| BukkitScheduler | Bukkit API | Main thread synchronization | Required for block/entity operations |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| RestoreResult | Custom | Machine restoration feedback | Return from restore operations for user messages |
| NovaBlockData | Custom | Serializable Nova block state | Store/load captured Nova blocks |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Custom Nova API | Direct reflection calls | NovaIntegration already abstracts reflection and version detection |
| Sequential dimension ops | Parallel dimension ops | Sequential is simpler and safer for block operations |
| Inline integration | Event-based hooks | Inline is more explicit and easier to trace |

**Installation:**
No external dependencies - all components already exist in codebase.

## Architecture Patterns

### Recommended Integration Points

**RelocationManager workflow:**
```
1. Before move: captureNovaBlocks() in source dimension(s)
2. Before move: removeNovaBlocks() in source dimension(s)
3. After move: restoreNovaBlocks() in target dimension(s)
4. Send RestoreResult feedback to player
```

**SlotSwitchManager workflow:**
```
1. During save (Step 2): captureNovaBlocks() in all dimensions
2. During save (Step 2): removeNovaBlocks() in all dimensions
3. During load (Step 4): restoreNovaBlocks() in all dimensions
4. Send RestoreResult feedback to player
```

**BackupManager workflow:**
```
1. During backup: captureNovaBlocks() in all dimensions
2. Store NovaBlockData with schematic files
3. During restore: restoreNovaBlocks() in all dimensions
4. Send RestoreResult feedback to player
```

### Pattern 1: Capture-Remove-Restore Lifecycle
**What:** Three-phase pattern for preserving Nova blocks during WorldEdit operations
**When to use:** Any operation that uses WorldEdit/FAWE to move/copy blocks
**Example:**
```java
// Phase 1: Capture (before WorldEdit operation)
List<NovaBlockData> novaBlocks = addon.getNovaIntegration().captureNovaBlocks(center, range);

// Phase 2: Remove (before WorldEdit paste, to avoid conflicts)
addon.getNovaIntegration().removeNovaBlocks(novaBlocks, center);

// Phase 3: WorldEdit operation happens here
addon.getSchematicUtils().loadAndPaste(schematicFile, center);

// Phase 4: Restore (after WorldEdit completes)
RestoreResult result = addon.getNovaIntegration().restoreNovaBlocks(novaBlocks, newCenter);

// Phase 5: Send feedback to player
String message = result.getFeedbackMessage();
if (message != null) {
    player.sendMessage(message);
}
```

### Pattern 2: Multi-Dimension Iteration
**What:** Process Nova blocks in all enabled dimensions for an island
**When to use:** When multi-dimension support is enabled
**Example:**
```java
private boolean isMultiDimensionEnabled() {
    DimensionManager dimManager = addon.getDimensionManager();
    return dimManager != null && dimManager.isEnabled();
}

private void processAllDimensionNovaBlocks(UUID playerUUID, SlotData slotData) {
    if (!isMultiDimensionEnabled()) {
        // Single-dimension: process overworld only
        processSingleDimension(playerUUID, slotData);
        return;
    }

    // Multi-dimension: iterate all enabled dimensions
    DimensionManager dimManager = addon.getDimensionManager();
    for (DimensionConfig config : dimManager.getEnabledDimensions()) {
        World world = dimManager.getWorld(config.getDimensionKey());
        if (world != null) {
            Island island = addon.getIslands().getIsland(world, playerUUID);
            if (island != null) {
                processDimension(island, config.getDimensionKey());
            }
        }
    }
}
```

### Pattern 3: Async-to-Sync Coordination
**What:** Use async methods for chunk loading, then sync for Nova operations
**When to use:** Island operations already async (relocation, slot switch)
**Example:**
```java
// From async thread: use async capture (loads chunks first)
addon.getNovaIntegration().captureNovaBlocksAsync(center, range, (novaBlocks) -> {
    // Callback runs on main thread - safe for removal
    addon.getNovaIntegration().removeNovaBlocks(novaBlocks, center);

    // Continue with async island operation...
});
```

### Anti-Patterns to Avoid
- **Capturing after WorldEdit paste:** Nova blocks are already gone - capture BEFORE paste
- **Restoring before WorldEdit:** Nova blocks will be overwritten - restore AFTER paste
- **Forgetting multi-dimension:** When enabled, all dimensions must be processed
- **Ignoring RestoreResult:** Users need feedback about machine preservation failures
- **Mixing sync/async incorrectly:** Nova operations must be on main thread

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Dimension iteration | Manual world lookups | DimensionManager.getEnabledDimensions() | Already handles enabled check, world lookup, config access |
| Async chunk loading | Manual CompletableFuture chains | NovaIntegration.captureNovaBlocksAsync() | Already loads chunks with timeout handling |
| Nova block detection | Direct reflection to WorldDataManager | NovaIntegration.captureNovaBlocks() | Already abstracts reflection, version detection, error handling |
| Machine counting | Custom TileEntity iteration | RestoreResult.machinesRestored | Phase 13 already counts machines during restore |
| User feedback | Manual message construction | RestoreResult.getFeedbackMessage() | Already formats success/failure messages |

**Key insight:** NovaIntegration (Phase 12-13) already provides all necessary methods. Don't reimplement capture/remove/restore logic - call the existing methods at the right points in the workflow.

## Common Pitfalls

### Pitfall 1: Wrong Operation Order
**What goes wrong:** Calling restore before WorldEdit paste, or capture after paste
**Why it happens:** Unclear lifecycle - WorldEdit overwrites blocks
**How to avoid:** Always follow capture → remove → WorldEdit operation → restore order
**Warning signs:** Nova machines missing after operation, duplicate machines, or errors about "block already exists"

### Pitfall 2: Forgetting Multi-Dimension
**What goes wrong:** Nova blocks only processed in overworld, lost in nether/end
**Why it happens:** Assuming single-dimension when multi-dimension is enabled
**How to avoid:** Check isMultiDimensionEnabled() and iterate all dimensions
**Warning signs:** Player reports machines missing in nether/end after slot switch

### Pitfall 3: Async Safety Violations
**What goes wrong:** ConcurrentModificationException or "Accessing block off main thread" errors
**Why it happens:** Nova block operations must be on main thread
**How to avoid:** Use async methods for chunk loading, but actual capture/remove/restore on main thread
**Warning signs:** Random crashes during island operations, inconsistent behavior

### Pitfall 4: Not Sending Feedback
**What goes wrong:** Silent failures - user doesn't know if machines were preserved
**Why it happens:** Forgetting to check RestoreResult and send message
**How to avoid:** Always call getFeedbackMessage() and send to player if not null
**Warning signs:** Users asking "did my machines copy?" in support channels

### Pitfall 5: Processing Disabled Integration
**What goes wrong:** Errors when Nova plugin not installed or disabled in config
**Why it happens:** Not checking NovaIntegration.isAvailable() first
**How to avoid:** Guard all Nova calls with availability check
**Warning signs:** Errors in console when Nova not installed

## Code Examples

Verified patterns from existing code and integration design:

### RelocationManager Integration
```java
// Source: RelocationManager.java (to be added in Phase 14)

// In relocateIsland() method, BEFORE WorldEdit copy:

// Check if Nova integration is available
if (addon.getNovaIntegration().isAvailable() && addon.getSettings().isNovaEnabled()) {
    // Capture Nova blocks from source location
    List<NovaBlockData> novaBlocks = addon.getNovaIntegration()
        .captureNovaBlocks(sourceCenter, range);

    // Remove Nova blocks from source (before WorldEdit copies)
    addon.getNovaIntegration().removeNovaBlocks(novaBlocks, sourceCenter);

    // Store for later restoration at target
    relocData.setNovaBlocks(novaBlocks);
}

// After WorldEdit paste to target location:

if (relocData.getNovaBlocks() != null && !relocData.getNovaBlocks().isEmpty()) {
    // Restore Nova blocks at new location
    RestoreResult result = addon.getNovaIntegration()
        .restoreNovaBlocks(relocData.getNovaBlocks(), targetCenter);

    // Send feedback to player
    String message = result.getFeedbackMessage();
    if (message != null) {
        player.sendMessage("§a" + message);
    }

    // Log warnings if failures occurred
    if (result.hasFailures()) {
        addon.logWarning("Some Nova machines failed to restore during relocation");
    }
}
```

### SlotSwitchManager Integration
```java
// Source: SlotSwitchManager.java (to be added in Phase 14)

// In performSlotSwitchAsync(), during save phase (Step 2):

// Capture Nova blocks before saving schematic
if (addon.getNovaIntegration().isAvailable() && addon.getSettings().isNovaEnabled()) {
    addon.getNovaIntegration().captureNovaBlocksAsync(center, range, (novaBlocks) -> {
        // Store with slot data for later restoration
        addon.getSlotManager().saveNovaBlocks(playerUUID, fromSlot.getSlotNumber(), novaBlocks);

        // Remove Nova blocks before WorldEdit save
        addon.getNovaIntegration().removeNovaBlocks(novaBlocks, center);

        // Continue with schematic save...
    });
}

// In performSlotSwitchAsync(), during load phase (Step 4):

// Restore Nova blocks after loading schematic
if (addon.getNovaIntegration().isAvailable() && addon.getSettings().isNovaEnabled()) {
    List<NovaBlockData> novaBlocks = addon.getSlotManager()
        .loadNovaBlocks(playerUUID, toSlot.getSlotNumber());

    if (novaBlocks != null && !novaBlocks.isEmpty()) {
        RestoreResult result = addon.getNovaIntegration()
            .restoreNovaBlocks(novaBlocks, center);

        // Send feedback
        String message = result.getFeedbackMessage();
        if (message != null) {
            sendProgress(player, "§a" + message);
        }
    }
}
```

### BackupManager Integration
```java
// Source: BackupManager.java (to be added in Phase 14)

// In createBackup() method:

// Capture Nova blocks along with backup
if (addon.getNovaIntegration().isAvailable() && addon.getSettings().isNovaEnabled()) {
    List<NovaBlockData> novaBlocks = addon.getNovaIntegration()
        .captureNovaBlocks(center, range);

    // Save Nova blocks to separate file alongside schematic
    File novaFile = new File(backupFile.getParent(),
        backupFile.getName().replace(".schem", ".nova"));
    saveNovaBlocks(novaBlocks, novaFile);
}

// In restoreBackup() method:

// Restore Nova blocks after loading schematic
if (addon.getNovaIntegration().isAvailable() && addon.getSettings().isNovaEnabled()) {
    File novaFile = new File(backupFile.getParent(),
        backupFile.getName().replace(".schem", ".nova"));

    if (novaFile.exists()) {
        List<NovaBlockData> novaBlocks = loadNovaBlocks(novaFile);

        RestoreResult result = addon.getNovaIntegration()
            .restoreNovaBlocks(novaBlocks, center);

        // Log result
        String message = result.getFeedbackMessage();
        if (message != null) {
            addon.log("Backup restore: " + message);
        }
    }
}
```

### Multi-Dimension Processing
```java
// Source: Pattern for all three managers

private void processAllDimensionsNova(UUID playerUUID, SlotData slotData,
                                      NovaOperationType opType) {
    if (!addon.getNovaIntegration().isAvailable() ||
        !addon.getSettings().isNovaEnabled()) {
        return;
    }

    if (!isMultiDimensionEnabled()) {
        // Single dimension: process overworld only
        processSingleDimensionNova(playerUUID, slotData, "overworld", opType);
        return;
    }

    // Multi-dimension: process all enabled dimensions
    DimensionManager dimManager = addon.getDimensionManager();
    for (DimensionConfig config : dimManager.getEnabledDimensions()) {
        String dimensionKey = config.getDimensionKey();
        World world = dimManager.getWorld(dimensionKey);

        if (world != null) {
            Island island = addon.getIslands().getIsland(world, playerUUID);
            if (island != null) {
                processSingleDimensionNova(playerUUID, slotData, dimensionKey, opType);
            }
        }
    }
}

private void processSingleDimensionNova(UUID playerUUID, SlotData slotData,
                                        String dimensionKey, NovaOperationType opType) {
    // Get island in this dimension
    World world = dimensionKey.equals("overworld")
        ? addon.getGridManager().getBSkyBlockWorld()
        : addon.getDimensionManager().getWorld(dimensionKey);

    if (world == null) return;

    Island island = addon.getIslands().getIsland(world, playerUUID);
    if (island == null) return;

    Location center = island.getCenter();
    int range = Math.max(addon.getIslandSpacing() / 2, island.getProtectionRange());

    // Perform operation based on type
    switch (opType) {
        case CAPTURE:
            List<NovaBlockData> blocks = addon.getNovaIntegration()
                .captureNovaBlocks(center, range);
            // Store blocks...
            break;
        case REMOVE:
            // Load blocks from storage...
            addon.getNovaIntegration().removeNovaBlocks(storedBlocks, center);
            break;
        case RESTORE:
            // Load blocks from storage...
            RestoreResult result = addon.getNovaIntegration()
                .restoreNovaBlocks(storedBlocks, center);
            // Handle result...
            break;
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| No custom block support | Nova integration via capture/restore | Phase 12-13 | Custom machines can now be preserved |
| Manual TileEntity counting | RestoreResult with machine counts | Phase 13 | Users get feedback about preservation |
| WorldDataManager only | WorldDataManager + TileEntity.getDrops() | Phase 13 | Better state preservation |
| No multi-dimension support | DimensionManager iteration | Phase 11 | All dimensions processed |

**Deprecated/outdated:**
- Direct reflection to Nova classes: Use NovaIntegration methods instead
- TileEntityManager: Removed in Nova 0.17+, use WorldDataManager.getTileEntity()
- Single-dimension assumptions: Check isMultiDimensionEnabled() first

## Open Questions

No significant gaps - the integration is straightforward given existing infrastructure.

**Minor considerations:**
1. **Serialization format for NovaBlockData in backups**
   - What we know: NovaBlockData implements Serializable with serialVersionUID = 2L
   - What's unclear: Should we use Java serialization or custom format?
   - Recommendation: Use Java serialization (ObjectOutputStream) - simplest and already Serializable

2. **Error handling when Nova blocks fail to restore**
   - What we know: RestoreResult tracks failures, getFeedbackMessage() shows partial success
   - What's unclear: Should operations fail completely or continue with warnings?
   - Recommendation: Continue with warnings (already implemented) - partial success better than total failure

3. **Performance impact of processing all dimensions**
   - What we know: Each dimension requires chunk loading and block iteration
   - What's unclear: Could cause lag on large islands with 3+ dimensions
   - Recommendation: Already async, just monitor performance - optimize if needed

## Sources

### Primary (HIGH confidence)
- Existing codebase: NovaIntegration.java (Phase 12-13 implementation)
- Existing codebase: RelocationManager.java (multi-dimension pattern)
- Existing codebase: SlotSwitchManager.java (async workflow pattern)
- Existing codebase: BackupManager.java (multi-dimension backup pattern)
- Existing codebase: DimensionManager.java (dimension iteration pattern)
- [Nova TileEntity API Documentation](https://docs.xenondevs.xyz/nova/api/tileentity/tileentity/) - getDrops() signature
- [Nova Setup Documentation](https://docs.xenondevs.xyz/nova/admin/setup/) - Compatibility warnings

### Secondary (MEDIUM confidence)
- [SpigotMC MultiThreading Guide](https://www.spigotmc.org/wiki/multithreading-completable-futures/) - CompletableFuture patterns for Bukkit
- [BentoBox Documentation](https://docs.bentobox.world/) - Addon integration patterns
- [Chunker Plugin - Modrinth](https://modrinth.com/plugin/chunker) - Async chunk loading patterns on Paper

### Tertiary (LOW confidence)
- [Nova Plugin - Modrinth](https://modrinth.com/plugin/nova-framework) - General Nova information
- [Nova GitHub Repository](https://github.com/xenondevs/Nova) - Open source reference
- [IA-Edit GitHub](https://github.com/EpicPlayerA10/IA-Edit) - Example of WorldEdit + custom blocks integration (ItemsAdder)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All components exist in codebase, patterns established
- Architecture: HIGH - Existing managers show exact integration pattern needed
- Pitfalls: MEDIUM - Based on common async/block operation mistakes, not Nova-specific evidence
- Multi-dimension: HIGH - All three managers already implement multi-dimension support

**Research date:** 2026-01-27
**Valid until:** 60 days (stable APIs, existing infrastructure, no fast-moving dependencies)
