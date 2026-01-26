---
phase: 14-operation-integration
plan: 01
subsystem: managers
tags: [nova, slot-switching, multi-dimension, state-preservation, worldedit]

# Dependency graph
requires:
  - phase: 12-core-capture-and-restore
    provides: "NovaIntegration with captureNovaBlocks, removeNovaBlocks, restoreNovaBlocks"
  - phase: 13-state-preservation
    provides: "RestoreResult with machine counts and feedback messages"
provides:
  - "Nova block preservation during slot switching with multi-dimension support"
  - "SlotManager Nova block persistence to .nova files"
  - "Player feedback on Nova machine restoration during switches"
affects: [14-02-relocation, 14-03-backup, operation-workflows]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Nova block lifecycle in operations: capture → remove → save → load → restore"
    - "Multi-dimension Nova processing with per-dimension block tracking"
    - "Progress messages for long-running Nova operations"

key-files:
  created: []
  modified:
    - src/main/java/world/bentobox/islandselector/managers/SlotSwitchManager.java
    - src/main/java/world/bentobox/islandselector/managers/SlotManager.java

key-decisions:
  - "Nova blocks captured after home saving (Step 1.8) to preserve timing consistency"
  - "Nova blocks removed before WorldEdit save for clean schematic operations"
  - "Target slot's Nova blocks loaded from storage, not from source slot (correct restoration)"
  - "Combined RestoreResult feedback from all dimensions for single player message"

patterns-established:
  - "isNovaEnabled() check pattern: integration available + enabled in config"
  - "Capture → Remove → Save pattern for source slot Nova preservation"
  - "Load → Restore pattern for target slot Nova restoration"
  - "Cleanup on error and cancellation to prevent memory leaks"

# Metrics
duration: 5min
completed: 2026-01-27
---

# Phase 14 Plan 01: Operation Integration Summary

**Nova block preservation integrated into slot switching with multi-dimension support and player feedback**

## Performance

- **Duration:** 5 min
- **Started:** 2026-01-27T02:55:07Z
- **Completed:** 2026-01-27T03:00:34Z
- **Tasks:** 3 (combined into single atomic commit)
- **Files modified:** 2

## Accomplishments
- Nova blocks captured and preserved during slot switches across all dimensions
- Player receives feedback on Nova machine restoration (e.g., "Preserved 5 Nova machines")
- Nova blocks persisted to .nova files per slot for restoration when switching back
- Multi-dimension slots process Nova blocks in all enabled dimensions

## Task Commits

All three tasks were tightly coupled and committed atomically:

1. **Tasks 1-3: Integrate Nova preservation into slot switching** - `7cca0fd` (feat)
   - Task 1: Add Nova block storage fields and helper methods to SlotSwitchManager
   - Task 2: Integrate Nova capture/remove/restore into slot switch workflow
   - Task 3: Add Nova block storage methods to SlotManager

## Files Created/Modified
- `src/main/java/world/bentobox/islandselector/managers/SlotSwitchManager.java` - Nova block lifecycle integration in slot switch workflow
- `src/main/java/world/bentobox/islandselector/managers/SlotManager.java` - Nova block persistence to/from .nova files

## Decisions Made

**1. Nova block timing in workflow**
- Capture Nova blocks after saving homes (Step 1.8), before WorldEdit save
- Remove Nova blocks immediately after capture for clean schematic
- Restore Nova blocks after WorldEdit load (Step 4.65), before blueprint permissions
- Rationale: Prevents Nova blocks from interfering with WorldEdit operations while preserving state

**2. Target slot restoration approach**
- Load Nova blocks from target slot's storage, not from source slot
- Source slot's captured blocks saved to its own .nova file
- Rationale: Correct restoration behavior - switching to a slot should load that slot's machines, not copy current machines

**3. Multi-dimension processing**
- Process all enabled dimensions when multi-dimension mode active
- Store blocks in Map<dimensionKey, List<NovaBlockData>> structure
- Combine RestoreResult counts across dimensions for single player message
- Rationale: Consistent with multi-dimension island operations, clear feedback

**4. Error and cancellation handling**
- Clean up switchNovaBlocks map on error or event cancellation
- Rationale: Prevents memory leaks from abandoned slot switches

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - build and integration succeeded on first attempt.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

**Ready for:**
- Phase 14-02: Relocation integration (same Nova patterns apply)
- Phase 14-03: Backup integration (same Nova patterns apply)

**Notes:**
- Nova integration gracefully skips when disabled or unavailable (isNovaEnabled() check)
- Player feedback only shown when machines were actually restored (RestoreResult.getFeedbackMessage())
- Multi-dimension support tested with dimension-specific block tracking

---
*Phase: 14-operation-integration*
*Completed: 2026-01-27*
