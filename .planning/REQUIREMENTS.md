# Requirements: IslandSelector v1.1

**Defined:** 2026-01-20
**Core Value:** Players can visually select their island location from a grid-based GUI

## v1.1 Requirements

Requirements for the Simplification milestone. Each maps to roadmap phases.

### Config Toggle

- [ ] **SLOT-01**: Config option `slots.enabled` exists in config.yml with default `true`
- [ ] **SLOT-02**: When slots disabled, `/islandselector slots` command shows disabled message
- [ ] **SLOT-03**: When slots disabled, slots button is hidden from main GUI

### Command Alias

- [ ] **CMD-01**: `/map` command works as alias for `/islandselector`

### Neighbors Removal

- [ ] **REM-01**: Neighbors button removed from main GUI
- [ ] **REM-02**: `/islandselector neighbors` command removed
- [ ] **REM-03**: NeighborhoodGUI source files removed or deprecated
- [ ] **REM-04**: Related permissions removed from addon.yml
- [ ] **REM-05**: Related locale keys cleaned up

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
| SLOT-01 | TBD | Pending |
| SLOT-02 | TBD | Pending |
| SLOT-03 | TBD | Pending |
| CMD-01 | TBD | Pending |
| REM-01 | TBD | Pending |
| REM-02 | TBD | Pending |
| REM-03 | TBD | Pending |
| REM-04 | TBD | Pending |
| REM-05 | TBD | Pending |

**Coverage:**
- v1.1 requirements: 9 total
- Mapped to phases: 0 (pending roadmap)
- Unmapped: 9

---
*Requirements defined: 2026-01-20*
*Last updated: 2026-01-20 after initial definition*
