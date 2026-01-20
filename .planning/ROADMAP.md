# Roadmap: IslandSelector

## Milestones

- **v1.0 Cleanup** - Phases 1-4 (shipped 2026-01-20)
- **v1.1 Simplification** - Phases 5-7 (in progress)

## Phases

<details>
<summary>v1.0 Cleanup (Phases 1-4) - SHIPPED 2026-01-20</summary>

Phases 1-4 completed prior to this roadmap. See MILESTONES.md for details.

</details>

### v1.1 Simplification (In Progress)

**Milestone Goal:** Streamline the addon by adding slot system configurability, command alias, and removing unused neighbors feature.

#### Phase 5: Config Toggle

**Goal**: Server admins can enable/disable the slot system via configuration
**Depends on**: Phase 4 (v1.0 complete)
**Requirements**: SLOT-01, SLOT-02, SLOT-03
**Success Criteria** (what must be TRUE):
  1. Config file contains `slots.enabled` option with default `true`
  2. When `slots.enabled: false`, running `/islandselector slots` shows disabled message
  3. When `slots.enabled: false`, slots button does not appear in main GUI
  4. When `slots.enabled: true`, existing slot functionality works unchanged
**Plans**: 1 plan

Plans:
- [x] 05-01-PLAN.md - Add slots.enabled config toggle with command/GUI checks

#### Phase 6: Command Alias

**Goal**: Players can use `/map` as a shortcut for `/islandselector`
**Depends on**: Nothing (independent of Phase 5)
**Requirements**: CMD-01
**Success Criteria** (what must be TRUE):
  1. Running `/map` opens the island selector GUI
  2. Running `/map` with any subcommand works identically to `/islandselector`
**Plans**: 1 plan

Plans:
- [x] 06-01-PLAN.md - Add /map command alias to IslandSelectorCommand constructor

#### Phase 7: Neighbors Removal

**Goal**: Neighbors feature is completely removed from the addon
**Depends on**: Nothing (independent of Phases 5-6)
**Requirements**: REM-01, REM-02, REM-03, REM-04, REM-05
**Success Criteria** (what must be TRUE):
  1. Main GUI does not show neighbors button
  2. Running `/islandselector neighbors` returns unknown command error
  3. No neighbors-related permissions exist in addon.yml
  4. No orphaned neighbors locale keys cleaned up
**Plans**: 1 plan

Plans:
- [ ] 07-01-PLAN.md - Delete neighbors source files and remove all references

## Progress

**Execution Order:**
Phases 5, 6, 7 can execute in any order (no dependencies between them).

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1-4 | v1.0 | - | Complete | 2026-01-20 |
| 5. Config Toggle | v1.1 | 1/1 | Complete | 2026-01-20 |
| 6. Command Alias | v1.1 | 1/1 | Complete | 2026-01-20 |
| 7. Neighbors Removal | v1.1 | 0/1 | Not started | - |

---
*Roadmap created: 2026-01-20*
*Last updated: 2026-01-20*
