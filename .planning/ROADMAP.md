# Roadmap: IslandSelector

## Milestones

- ✅ **v1.0 Cleanup** - Phases 1-5 (shipped 2026-01-17)
- ✅ **v1.1 Simplification** - Phases 6-8 (shipped 2026-01-19)
- ✅ **v1.1.1 Bugfixes** - Phases 9-10 (shipped 2026-01-21)
- 🚧 **v1.2 Nova Integration** - Phases 11-15 (in progress)

## Phases

<details>
<summary>✅ v1.0 Cleanup (Phases 1-5) - SHIPPED 2026-01-17</summary>

### Phase 1: Foundation
**Goal**: Clean project structure established
**Plans**: 3 plans

Plans:
- [x] 01-01: Move source to standard Maven structure
- [x] 01-02: Clean up repository artifacts
- [x] 01-03: Verify build and structure

### Phase 2: Dependencies
**Goal**: Dependency management cleaned up
**Plans**: 2 plans

Plans:
- [x] 02-01: Audit and update dependencies
- [x] 02-02: Document dependency decisions

### Phase 3: Configuration
**Goal**: Configuration files standardized
**Plans**: 1 plan

Plans:
- [x] 03-01: Standardize config and locale files

### Phase 4: Code Organization
**Goal**: Source code organized by function
**Plans**: 2 plans

Plans:
- [x] 04-01: Organize packages and classes
- [x] 04-02: Remove unused code

### Phase 5: Documentation
**Goal**: Project documented for users and developers
**Plans**: 1 plan

Plans:
- [x] 05-01: Create comprehensive documentation

</details>

<details>
<summary>✅ v1.1 Simplification (Phases 6-8) - SHIPPED 2026-01-19</summary>

### Phase 6: Configuration Toggle
**Goal**: Slot system can be enabled/disabled via config
**Plans**: 1 plan

Plans:
- [x] 06-01: Add slots.enabled config option

### Phase 7: Command Enhancements
**Goal**: Command structure improved for user experience
**Plans**: 2 plans

Plans:
- [x] 07-01: Add /map command alias
- [x] 07-02: Block /islandselector slots when disabled

### Phase 8: GUI Cleanup
**Goal**: GUI simplified by removing neighbors feature
**Plans**: 2 plans

Plans:
- [x] 08-01: Remove neighbors button from main GUI
- [x] 08-02: Remove neighbors command and GUI class

</details>

<details>
<summary>✅ v1.1.1 Bugfixes (Phases 9-10) - SHIPPED 2026-01-21</summary>

### Phase 9: Level and Warp Fixes
**Goal**: Island level display and warp access work correctly
**Plans**: 3 plans

Plans:
- [x] 09-01: Fix level calculation to match /island level
- [x] 09-02: Restrict island visiting to allowed warps only
- [x] 09-03: Show warp availability status in GUI

### Phase 10: Relocation Safety
**Goal**: Island relocation only teleports island members
**Plans**: 1 plan

Plans:
- [x] 10-01: Fix relocation teleporting unrelated players

</details>

### 🚧 v1.2 Nova Integration (In Progress)

**Milestone Goal:** Preserve Nova machine state (inventory, energy, owner) during island operations

#### Phase 11: Foundation
**Goal**: Nova 0.17+ API integration functional with config control
**Depends on**: Phase 10
**Requirements**: NOVA-01, NOVA-14
**Success Criteria** (what must be TRUE):
  1. NovaIntegration detects Nova 0.17+ correctly using updated reflection paths
  2. Player sees "Nova integration enabled" message when available
  3. Config option `nova.enabled` controls whether Nova integration runs
  4. System gracefully degrades when Nova not installed or disabled
**Plans**: 1 plan

Plans:
- [x] 11-01-PLAN.md — Add Nova config toggle and verify 0.17+ API paths

#### Phase 12: Core Capture and Restore
**Goal**: Nova blocks captured and restored with proper lifecycle management
**Depends on**: Phase 11
**Requirements**: NOVA-02, NOVA-03, NOVA-04
**Success Criteria** (what must be TRUE):
  1. System captures all Nova blocks in island region before operations
  2. Nova blocks properly removed via BlockUtils.breakBlock() before WorldEdit operations
  3. Nova blocks restore at correct positions after WorldEdit paste completes
  4. Backing block state preserved (no blocks become air or vanilla materials)
**Plans**: 1 plan

Plans:
- [ ] 12-01-PLAN.md — Enhance capture with TileEntity drops, add removeNovaBlocks method

#### Phase 13: State Preservation
**Goal**: Nova machine inventories and owners preserved through operations
**Depends on**: Phase 12
**Requirements**: NOVA-05, NOVA-06, NOVA-07, NOVA-08
**Success Criteria** (what must be TRUE):
  1. Machine inventory contents preserved (items don't disappear)
  2. Machine owner preserved (ownership doesn't reset)
  3. Player sees "Preserved 23 Nova machines" feedback message after operations
  4. Player sees "3 machines couldn't be restored" error message if restoration fails
**Plans**: TBD

Plans:
- [ ] 13-01: TBD

#### Phase 14: Operation Integration
**Goal**: Nova integration hooks into all island operation workflows
**Depends on**: Phase 13
**Requirements**: NOVA-09, NOVA-10, NOVA-11, NOVA-12
**Success Criteria** (what must be TRUE):
  1. Island relocation preserves Nova machines (RelocationManager integration)
  2. Slot switching preserves Nova machines (SlotSwitchManager integration)
  3. Backup creation includes Nova machines (BackupManager integration)
  4. Multi-dimension islands process Nova blocks in all dimensions (overworld, nether, end)
**Plans**: TBD

Plans:
- [ ] 14-01: TBD

#### Phase 15: Performance and Polish
**Goal**: Nova integration optimized for production use
**Depends on**: Phase 14
**Requirements**: NOVA-13
**Success Criteria** (what must be TRUE):
  1. Nova block scanning completes in under 5 seconds for typical islands
  2. Reflection method calls cached (not re-looked-up on every operation)
  3. Large islands (100+ Nova blocks) process without server lag
**Plans**: TBD

Plans:
- [ ] 15-01: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → ... → 15

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Foundation | v1.0 | 3/3 | Complete | 2026-01-17 |
| 2. Dependencies | v1.0 | 2/2 | Complete | 2026-01-17 |
| 3. Configuration | v1.0 | 1/1 | Complete | 2026-01-17 |
| 4. Code Organization | v1.0 | 2/2 | Complete | 2026-01-17 |
| 5. Documentation | v1.0 | 1/1 | Complete | 2026-01-17 |
| 6. Configuration Toggle | v1.1 | 1/1 | Complete | 2026-01-19 |
| 7. Command Enhancements | v1.1 | 2/2 | Complete | 2026-01-19 |
| 8. GUI Cleanup | v1.1 | 2/2 | Complete | 2026-01-19 |
| 9. Level and Warp Fixes | v1.1.1 | 3/3 | Complete | 2026-01-21 |
| 10. Relocation Safety | v1.1.1 | 1/1 | Complete | 2026-01-21 |
| 11. Foundation | v1.2 | 1/1 | Complete | 2026-01-27 |
| 12. Core Capture and Restore | v1.2 | 0/1 | Not started | - |
| 13. State Preservation | v1.2 | 0/TBD | Not started | - |
| 14. Operation Integration | v1.2 | 0/TBD | Not started | - |
| 15. Performance and Polish | v1.2 | 0/TBD | Not started | - |
