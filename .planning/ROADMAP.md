# Roadmap: IslandSelector

## Milestones

- v1.0 MVP - Phases 1-4 (shipped)
- v1.1 Simplification - Phases 5-7 (shipped)
- **v1.1.1 Bugfixes** - Phases 8-10 (complete)

## Phases

### v1.1.1 Bugfixes (Complete)

**Milestone Goal:** Fix level display accuracy, warp-based visiting, and relocation safety

- [x] **Phase 8: Level Display** - Accurate island level in GUI
- [x] **Phase 9: Island Visiting** - Warp sign-based visit controls
- [x] **Phase 10: Relocation Safety** - Only teleport affected players

## Phase Details

### Phase 8: Level Display
**Goal**: Island level displayed in GUI matches actual level with no stale data
**Depends on**: Nothing (independent bugfix)
**Requirements**: LEVL-01, LEVL-02
**Success Criteria** (what must be TRUE):
  1. User sees same level number in GUI as `/island level` command shows
  2. After leveling up, GUI shows new level immediately without reopening
**Plans**: 1 plan

Plans:
- [x] 08-01-PLAN.md — Fix level display to use overworld-only, non-cached method

### Phase 9: Island Visiting
**Goal**: Right-click visit only works when island has active warp sign
**Depends on**: Nothing (independent bugfix)
**Requirements**: VIST-01, VIST-02, VIST-03, VIST-04
**Success Criteria** (what must be TRUE):
  1. User can only visit islands that have warp signs (right-click teleports)
  2. GUI shows warp indicator on islands that have warps, hides "Right-click to visit" hint when no warp
  3. Clicking island without warp shows clear "no warp" message instead of teleporting
**Plans**: 1 plan

Plans:
- [x] 09-01-PLAN.md — WarpIntegration + GUI warp indicator + visit restriction

### Phase 10: Relocation Safety
**Goal**: Relocation only affects players actually on the relocating island
**Depends on**: Nothing (independent bugfix)
**Requirements**: RELC-01, RELC-02
**Success Criteria** (what must be TRUE):
  1. Player standing on island being relocated is teleported to new location
  2. Player elsewhere in world (spawn, other island, nether) is not teleported
**Plans**: 1 plan

Plans:
- [x] 10-01-PLAN.md — Add island.onIsland() checks before relocation teleports

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 8. Level Display | v1.1.1 | 1/1 | Complete | 2026-01-20 |
| 9. Island Visiting | v1.1.1 | 1/1 | Complete | 2026-01-20 |
| 10. Relocation Safety | v1.1.1 | 1/1 | Complete | 2026-01-21 |

---
*Roadmap created: 2026-01-20*
*Milestone: v1.1.1 Bugfixes*
