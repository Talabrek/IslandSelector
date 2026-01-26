---
phase: 14-operation-integration
plan: 02
subsystem: island-operations
tags: [nova, relocation, multi-dimension, state-preservation]

requires:
  - 12-01 (Core Nova capture/restore methods)
  - 13-01 (RestoreResult feedback system)

provides:
  - Nova block preservation during island relocation
  - Player feedback about preserved machines
  - Multi-dimension Nova handling in relocation

affects:
  - Future relocation features (Nova blocks preserved automatically)
  - Multi-dimension relocation operations (Nova processed in all dimensions)

tech-stack:
  added: []
  patterns:
    - "Nova capture/remove/restore lifecycle in relocation workflow"
    - "Error cleanup pattern: relocationNovaBlocks.remove() on all error paths"
    - "Player feedback routing: admin vs target player messages"

key-files:
  created: []
  modified:
    - src/main/java/world/bentobox/islandselector/managers/RelocationManager.java

decisions:
  - id: relocation-nova-lifecycle
    choice: "Capture before WorldEdit copy, remove after capture, restore after paste"
    rationale: "Prevents WorldEdit from corrupting Nova data, ensures proper state transfer"
    alternatives: []

  - id: relocation-nova-storage
    choice: "Store in relocationNovaBlocks map (per-dimension) during relocation"
    rationale: "Temporary storage between source copy and target paste, multi-dimension support"
    alternatives: []

  - id: relocation-error-cleanup
    choice: "Clean up captured Nova blocks on all error and exception paths"
    rationale: "Prevents memory leaks from failed relocations"
    alternatives: []

  - id: relocation-admin-feedback
    choice: "Route feedback to target player if online, otherwise admin player"
    rationale: "Admin relocations work for offline players, feedback goes to appropriate recipient"
    alternatives: []

metrics:
  duration: "12 minutes"
  completed: 2026-01-26
  tasks: 2
  commits: 2
  files: 1
  tests-added: 0
  tests-updated: 0

verification:
  - Build passes: ✓ mvn clean package successful
  - Imports added: ✓ NovaBlockData, RestoreResult
  - Helper methods: ✓ isNovaEnabled, captureNovaBlocksForRelocation, restoreNovaBlocksForRelocation
  - Player workflow: ✓ Capture/remove before saveIslandToClipboardAsync
  - Player workflow: ✓ Restore after entity restoration
  - Admin workflow: ✓ Capture/remove before saveIslandToClipboard
  - Admin workflow: ✓ Restore after pasteIslandFromClipboard
  - Error cleanup: ✓ relocationNovaBlocks.remove() on all error paths
  - Progress messages: ✓ "Capturing Nova machines..." and "Restoring Nova machines..."
  - Player feedback: ✓ RestoreResult.getFeedbackMessage() sent to player
  - Multi-dimension: ✓ Processes all enabled dimensions
---

# Phase 14 Plan 02: RelocationManager Integration Summary

**One-liner:** Island relocation preserves Nova machines (inventory, owner, energy) with player feedback

## What Was Built

Integrated Nova block preservation into island relocation workflow (both player and admin relocations).

**Core functionality:**
- Nova blocks captured from source island before WorldEdit copy
- Nova blocks removed after capture to prevent WorldEdit data corruption
- Nova blocks restored at target location after WorldEdit paste
- Player receives feedback message about preserved machines
- Multi-dimension support: processes Nova blocks in all enabled dimensions
- Graceful skipping when Nova disabled or unavailable

**Player workflow integration:**
1. Progress message: "Capturing Nova machines..."
2. `captureNovaBlocksForRelocation()` - captures from all dimensions
3. `removeNovaBlocksForRelocation()` - removes from source
4. WorldEdit copy/clear/paste operations
5. Entity restoration
6. Progress message: "Restoring Nova machines..."
7. `restoreNovaBlocksForRelocation()` - restores at target, sends feedback

**Admin workflow integration:**
- Same lifecycle as player workflow
- Routes feedback to target player if online, otherwise admin player
- Works for offline player relocations

**Error handling:**
- Cleanup on save failure
- Cleanup on clear failure
- Cleanup on paste failure
- Cleanup on exception catch
- Prevents memory leaks from failed relocations

## Changes Made

### src/main/java/world/bentobox/islandselector/managers/RelocationManager.java

**Imports added:**
```java
import world.bentobox.islandselector.integrations.NovaIntegration.NovaBlockData;
import world.bentobox.islandselector.integrations.NovaIntegration.RestoreResult;
import java.util.concurrent.ConcurrentHashMap;
```

**Field added:**
```java
// Track Nova blocks during relocation (per-dimension)
private final Map<UUID, Map<String, List<NovaBlockData>>> relocationNovaBlocks = new ConcurrentHashMap<>();
```

**Helper methods added:**
- `isMultiDimensionEnabled()` - check if multi-dimension support enabled
- `isNovaEnabled()` - check if Nova integration available and enabled
- `captureNovaBlocksForRelocation(UUID, Player)` - capture from all dimensions
- `captureNovaBlocksForDimension(UUID, World)` - capture from single dimension
- `removeNovaBlocksForRelocation(UUID)` - remove from all dimensions
- `removeNovaBlocksForDimension(UUID, World, List)` - remove from single dimension
- `restoreNovaBlocksForRelocation(UUID, Player, Location)` - restore to all dimensions with feedback

**Player workflow integration (performRelocationWork):**
- Before `saveIslandToClipboardAsync`: capture and remove Nova blocks
- After `pasteIslandFromClipboardAsync` + entity restoration: restore Nova blocks
- Error paths: cleanup on save/clear/paste failures

**Admin workflow integration (performAdminRelocationWork):**
- Before `saveIslandToClipboard`: capture and remove Nova blocks
- After `pasteIslandFromClipboard`: restore Nova blocks with feedback routing
- Error paths: cleanup on save failure and exception catch

## Deviations from Plan

None - plan executed exactly as written.

## Testing Evidence

**Build verification:**
```
mvn clean package -q
[INFO] BUILD SUCCESS
[INFO] Total time:  10.351 s
```

**Message integration verification:**
```
grep "Capturing Nova machines"
372:  sendProgressToAdmin(adminUUID, "&eCapturing Nova machines...");
752:  sendProgress(player, "&eCapturing Nova machines...");

grep "Restoring Nova machines"
421:  sendProgressToAdmin(adminUUID, "&eRestoring Nova machines...");
800:  sendProgress(player, "&eRestoring Nova machines...");
```

**Error cleanup verification:**
```
grep "relocationNovaBlocks.remove"
393:  relocationNovaBlocks.remove(targetUUID);  // admin save failure
433:  relocationNovaBlocks.remove(targetUUID);  // admin offline restore
566:  relocationNovaBlocks.remove(targetUUID);  // admin exception catch
761:  relocationNovaBlocks.remove(playerUUID); // player save failure
777:  relocationNovaBlocks.remove(playerUUID); // player clear failure
787:  relocationNovaBlocks.remove(playerUUID); // player paste failure
1794: Map<String, List<NovaBlockData>> dimensionBlocks = relocationNovaBlocks.remove(playerUUID); // restore cleanup
```

## Decisions Made

**1. Nova lifecycle integration points**
- **Decision:** Capture before WorldEdit copy, remove after capture, restore after paste
- **Rationale:** Prevents WorldEdit from corrupting Nova data, ensures clean state transfer
- **Impact:** Nova blocks survive relocation with full state (inventory, owner, energy)

**2. Temporary storage structure**
- **Decision:** Use `relocationNovaBlocks` map with per-dimension lists
- **Rationale:** Supports multi-dimension relocations, isolates data per player
- **Impact:** Memory efficient, thread-safe (ConcurrentHashMap)

**3. Error cleanup strategy**
- **Decision:** Clean up captured Nova blocks on all error and exception paths
- **Rationale:** Prevents memory leaks from failed relocations
- **Impact:** No orphaned data after errors, safe async operation

**4. Admin feedback routing**
- **Decision:** Send feedback to target player if online, otherwise admin player
- **Rationale:** Admin relocations work for offline players, feedback reaches appropriate recipient
- **Impact:** Clear communication about preservation for both scenarios

## Next Phase Readiness

**Blockers:** None

**Concerns:** None

**Dependencies satisfied:**
- ✓ NovaIntegration.captureNovaBlocks() available (Phase 12)
- ✓ NovaIntegration.removeNovaBlocks() available (Phase 12)
- ✓ NovaIntegration.restoreNovaBlocks() available (Phase 12)
- ✓ RestoreResult.getFeedbackMessage() available (Phase 13)

**Ready for:** Phase 14-03 (BackupManager integration)

## Commits

1. **cbb2885** - feat(14-02): add Nova block storage and helper methods to RelocationManager
   - Added NovaBlockData and RestoreResult imports
   - Added relocationNovaBlocks map for temporary storage
   - Added helper methods: isMultiDimensionEnabled, isNovaEnabled
   - Added captureNovaBlocksForRelocation with multi-dimension support
   - Added removeNovaBlocksForRelocation for cleanup
   - Added restoreNovaBlocksForRelocation with player feedback

2. **c07e010** - feat(14-02): integrate Nova handling into relocation workflow
   - Player workflow: capture/remove before WorldEdit copy
   - Player workflow: restore after paste and entity restoration
   - Admin workflow: capture/remove before WorldEdit copy
   - Admin workflow: restore after paste with feedback routing
   - Error handling: cleanup on all error and exception paths
   - Progress messages: "Capturing Nova machines..." and "Restoring Nova machines..."

## Files Modified

- `src/main/java/world/bentobox/islandselector/managers/RelocationManager.java` (+275 lines)
  - Imports: NovaBlockData, RestoreResult, ConcurrentHashMap
  - Field: relocationNovaBlocks map
  - Methods: 7 Nova helper methods
  - Integration: player and admin relocation workflows
  - Error handling: 7 cleanup points

## Knowledge Captured

**Technical patterns:**
- Nova capture/remove/restore lifecycle in relocation workflow
- Error cleanup pattern: relocationNovaBlocks.remove() on all error paths
- Player feedback routing: target player vs admin player messages
- Multi-dimension iteration: DimensionManager.getEnabledDimensions()

**Implementation insights:**
- Capture must happen BEFORE WorldEdit copy (prevents corruption)
- Remove must happen AFTER capture (blocks need to exist during capture)
- Restore must happen AFTER paste AND after island center update (needs new center)
- Cleanup must happen on ALL error paths (prevents memory leaks)
- Admin relocations need special feedback routing (target may be offline)

**Integration points established:**
- Player relocation: before saveIslandToClipboardAsync
- Player relocation: after entity restoration, before dimension relocation
- Admin relocation: before saveIslandToClipboard
- Admin relocation: after pasteIslandFromClipboard
- Error paths: save/clear/paste failures, exception catch

---
*Phase 14 Plan 02 completed successfully on 2026-01-26*
*Duration: 12 minutes*
*All success criteria met ✓*
