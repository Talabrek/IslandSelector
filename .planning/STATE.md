# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-26)

**Core value:** Players can visually select their island location from a grid-based GUI
**Current focus:** v1.2 Nova Integration - Phase 12 (Core Capture and Restore)

## Current Position

Milestone: v1.2 Nova Integration
Phase: 12 of 15 (Core Capture and Restore)
Plan: 1 of 1 complete
Status: Phase complete - ready for Phase 13
Last activity: 2026-01-27 — Completed 12-01-PLAN.md (TileEntity drop capture and block removal)

Progress: [████████████░░░░░░░░] 80% (12/15 phases complete from all milestones)

## Performance Metrics

**Velocity:**
- Total plans completed: 16 (5 v1.0 + 4 v1.1 + 5 v1.1.1 + 2 v1.2)
- Average duration: 3.5 minutes
- Total execution time: ~56 minutes

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
- Phase 13: State Preservation (inventory, owner, feedback) - 4 requirements
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

### Pending Todos

None.

### Blockers/Concerns

None.

## Session Continuity

Last session: 2026-01-27T16:39:37Z
Stopped at: Completed 12-01-PLAN.md (TileEntity drop capture and block removal)
Resume file: None
Resume: Begin Phase 13 (State Preservation)

---
*State updated: 2026-01-27 after Phase 12 completion*
