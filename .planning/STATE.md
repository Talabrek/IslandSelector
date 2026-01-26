# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-26)

**Core value:** Players can visually select their island location from a grid-based GUI
**Current focus:** v1.2 Nova Integration - Phase 14 (Operation Integration)

## Current Position

Milestone: v1.2 Nova Integration
Phase: 14 of 15 (Operation Integration)
Plan: 3 of 4 complete (14-01, 14-02, 14-03)
Status: In progress
Last activity: 2026-01-27 — Completed 14-03-PLAN.md (BackupManager integration)

Progress: [█████████████░░░░░░░] 87% (13/15 phases complete from all milestones)

## Performance Metrics

**Velocity:**
- Total plans completed: 19 (5 v1.0 + 4 v1.1 + 5 v1.1.1 + 5 v1.2)
- Average duration: 4.0 minutes
- Total execution time: ~74 minutes

## Accumulated Context

### Decisions

Full decision log in PROJECT.md Key Decisions table.

**v1.2 research findings:**
- Nova 0.17+ removed TileEntityManager, use WorldDataManager#getTileEntity
- Nova docs state FAWE incompatible — work around via drop-based approach
- Use TileEntity.getDrops(true) for state preservation (Nova's own serialization)
- Use BlockManager.placeBlock() and removeBlock() for proper Nova block handling
- Package moved: xyz.xenondevs.nova.tileentity → xyz.xenondevs.nova.world.block.tileentity

**v1.2 roadmap structure:**
- 5 phases derived from 14 requirements (quick depth)
- Phase 11: Foundation (API update, config) - 2 requirements ✓ COMPLETE
- Phase 12: Core Capture/Restore (capture, cleanup, restore) - 3 requirements ✓ COMPLETE
- Phase 13: State Preservation (inventory, owner, feedback) - 4 requirements ✓ COMPLETE
- Phase 14: Operation Integration (3 managers + multi-dimension) - 4 requirements
- Phase 15: Performance/Polish (optimization) - 1 requirement

**Phase 11 decisions:**
- Nova integration config flag defaults to true (opt-out, not opt-in)
- Three-state logging: enabled/disabled/unavailable for clear admin feedback
- Debug logging shows detected API version (0.17+ vs pre-0.17 paths)

**Phase 12 decisions:**
- Store drops in NovaBlockData class (cohesive state data, simpler serialization)
- Use TileEntity.getDrops(true) for state capture (Nova's official serialization)
- Use Context.EMPTY for automated removal (proper API usage, no player context)
- Silent removal via breakEffects=false (no duplicate effects/drops)

**Phase 13 decisions:**
- RestoreResult as public static inner class (cohesion with NovaIntegration)
- Only count blocks with drops as machines (TileEntity blocks only)
- Separate tracking of machinesRestored and machinesFailed
- getFeedbackMessage returns null when no machines (callers can skip message)

**Phase 14 decisions (14-01 SlotSwitchManager):**
- Nova blocks captured after home saving (Step 1.8) for timing consistency
- Nova blocks removed before WorldEdit save for clean schematic operations
- Target slot's Nova blocks loaded from storage (not copied from source)
- Combined RestoreResult feedback from all dimensions for single player message

**Phase 14 decisions (14-03 BackupManager):**
- Nova blocks stored in separate .nova files alongside .schem files
- Capture/restore happens AFTER schematic operation succeeds (fail-safe)
- Machine restoration feedback logged to console only (not propagated to player)

### Pending Todos

None.

### Blockers/Concerns

None.

## Session Continuity

Last session: 2026-01-27T02:56:30Z
Stopped at: Completed 14-03-PLAN.md (BackupManager integration)
Resume file: None
Resume: Continue Phase 14 (remaining plan: 14-04 Multi-dimension)

---
*State updated: 2026-01-27 after 14-03 completion*
