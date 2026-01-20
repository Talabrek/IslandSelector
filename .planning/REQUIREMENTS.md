# Requirements: IslandSelector v1.1

**Defined:** 2026-01-20
**Core Value:** Players can visually select their island location from a grid-based GUI

## v1.1 Requirements

Requirements for the Simplification milestone. Each maps to roadmap phases.

### Config Toggle

- [x] **SLOT-01**: Config option `slots.enabled` exists in config.yml with default `true`
- [x] **SLOT-02**: When slots disabled, `/islandselector slots` command shows disabled message
- [x] **SLOT-03**: When slots disabled, slots button is hidden from main GUI

### Command Alias

- [x] **CMD-01**: `/map` command works as alias for `/islandselector`

### Neighbors Removal

- [x] **REM-01**: Neighbors button removed from main GUI
- [x] **REM-02**: `/islandselector neighbors` command removed
- [x] **REM-03**: NeighborhoodGUI source files removed or deprecated
- [x] **REM-04**: Related permissions removed from addon.yml
- [x] **REM-05**: Related locale keys cleaned up

## Future Requirements

None — v1.1 is a focused simplification milestone.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Modifying slot system logic | Only adding enable/disable toggle |
| Other config options | Not part of this milestone |
| Modifying other GUIs | Only removing neighbors button |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| SLOT-01 | Phase 5 | Complete |
| SLOT-02 | Phase 5 | Complete |
| SLOT-03 | Phase 5 | Complete |
| CMD-01 | Phase 6 | Complete |
| REM-01 | Phase 7 | Complete |
| REM-02 | Phase 7 | Complete |
| REM-03 | Phase 7 | Complete |
| REM-04 | Phase 7 | Complete |
| REM-05 | Phase 7 | Complete |

**Coverage:**
- v1.1 requirements: 9 total
- Mapped to phases: 9
- Unmapped: 0

---
*Requirements defined: 2026-01-20*
*Last updated: 2026-01-20 after roadmap creation*
