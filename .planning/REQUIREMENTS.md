# Requirements: IslandSelector v1.2

**Defined:** 2026-01-26
**Core Value:** Players can visually select their island location from a grid-based GUI

## v1.2 Requirements

Requirements for Nova Integration milestone. Each maps to roadmap phases.

### Core Integration

- [x] **NOVA-01**: NovaIntegration updated to Nova 0.17+ API (new package paths, WorldDataManager)
- [x] **NOVA-02**: System captures all Nova tile entities in island region before operations
- [x] **NOVA-03**: System restores Nova blocks at new location after operations complete
- [x] **NOVA-04**: System removes Nova blocks properly via BlockManager.removeBlock() before WorldEdit
- [x] **NOVA-05**: Machine inventory contents preserved during capture/restore cycle
- [x] **NOVA-06**: Machine owner preserved during capture/restore cycle
- [x] **NOVA-07**: Player sees visual feedback message showing Nova machines preserved count
- [x] **NOVA-08**: Player sees error message if Nova machines fail to restore

### Operation Integration

- [x] **NOVA-09**: RelocationManager calls NovaIntegration before/after island move
- [x] **NOVA-10**: SlotSwitchManager calls NovaIntegration during slot save/load
- [x] **NOVA-11**: BackupManager calls NovaIntegration during backup/restore
- [x] **NOVA-12**: Nova integration handles multi-dimension islands (all dimensions processed)
- [x] **NOVA-13**: Nova block scanning optimized with caching for performance

### Configuration

- [x] **NOVA-14**: Config option `nova.enabled` to enable/disable Nova integration

## Future Requirements

Deferred to later milestones. Tracked but not in current roadmap.

### Advanced State Preservation

- **NOVA-F01**: Machine energy level preserved during operations
- **NOVA-F02**: Machine progress preserved during operations
- **NOVA-F03**: Machine upgrades preserved during operations (Simple Upgrades addon)

### Advanced Configuration

- **NOVA-F04**: Per-operation toggles (nova.relocation, nova.slots, nova.backup)
- **NOVA-F05**: Configurable scan range for Nova blocks

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Nova as hard dependency | Use reflection for optional integration, same as other addons |
| Direct tile entity data manipulation | Use drop-based approach via getDrops(true) instead |
| FAWE-Nova direct compatibility | Nova docs say FAWE incompatible; we work around it |
| Network auto-reconnection | Too complex; document manual reconnection instead |
| Perfect state replication | Some state loss acceptable (e.g., tick progress) |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| NOVA-01 | Phase 11 | Complete |
| NOVA-02 | Phase 12 | Complete |
| NOVA-03 | Phase 12 | Complete |
| NOVA-04 | Phase 12 | Complete |
| NOVA-05 | Phase 13 | Complete |
| NOVA-06 | Phase 13 | Complete |
| NOVA-07 | Phase 13 | Complete |
| NOVA-08 | Phase 13 | Complete |
| NOVA-09 | Phase 14 | Complete |
| NOVA-10 | Phase 14 | Complete |
| NOVA-11 | Phase 14 | Complete |
| NOVA-12 | Phase 14 | Complete |
| NOVA-13 | Phase 15 | Complete |
| NOVA-14 | Phase 11 | Complete |

**Coverage:**
- v1.2 requirements: 14 total
- Mapped to phases: 14/14 ✓
- Unmapped: 0

---
*Requirements defined: 2026-01-26*
*Last updated: 2026-01-27 after Phase 15 completion*
