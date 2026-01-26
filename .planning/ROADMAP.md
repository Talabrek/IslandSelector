# Roadmap: IslandSelector

## Milestones

- ✅ **v1.0 Cleanup** - Phases 1-5 (shipped 2026-01-17)
- ✅ **v1.1 Simplification** - Phases 6-8 (shipped 2026-01-19)
- ✅ **v1.1.1 Bugfixes** - Phases 9-10 (shipped 2026-01-21)
- ✅ **v1.2 Nova Integration** - Phases 11-15 (shipped 2026-01-27) → [archive](milestones/v1.2-ROADMAP.md)

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

<details>
<summary>✅ v1.2 Nova Integration (Phases 11-15) - SHIPPED 2026-01-27</summary>

### Phase 11: Foundation
**Goal**: Nova 0.17+ API integration functional with config control
**Plans**: 1 plan

Plans:
- [x] 11-01: Add Nova config toggle and verify 0.17+ API paths

### Phase 12: Core Capture and Restore
**Goal**: Nova blocks captured and restored with proper lifecycle management
**Plans**: 1 plan

Plans:
- [x] 12-01: Enhance capture with TileEntity drops, add removeNovaBlocks method

### Phase 13: State Preservation
**Goal**: Nova machine inventories and owners preserved through operations
**Plans**: 1 plan

Plans:
- [x] 13-01: Add RestoreResult return type with feedback message generation

### Phase 14: Operation Integration
**Goal**: Nova integration hooks into all island operation workflows
**Plans**: 3 plans

Plans:
- [x] 14-01: Integrate Nova preservation into SlotSwitchManager workflow
- [x] 14-02: Integrate Nova preservation into RelocationManager workflow
- [x] 14-03: Integrate Nova preservation into BackupManager workflow

### Phase 15: Performance and Polish
**Goal**: Nova integration optimized for production use
**Plans**: 1 plan

Plans:
- [x] 15-01: Cache reflection lookups and add performance timing

</details>

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
| 12. Core Capture and Restore | v1.2 | 1/1 | Complete | 2026-01-27 |
| 13. State Preservation | v1.2 | 1/1 | Complete | 2026-01-27 |
| 14. Operation Integration | v1.2 | 3/3 | Complete | 2026-01-27 |
| 15. Performance and Polish | v1.2 | 1/1 | Complete | 2026-01-27 |
