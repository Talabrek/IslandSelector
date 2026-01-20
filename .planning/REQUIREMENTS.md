# Requirements: IslandSelector v1.1.1

**Defined:** 2026-01-20
**Core Value:** Players can visually select their island location from a grid-based GUI

## v1.1.1 Requirements

Bugfix patch release. All requirements are defect corrections.

### Level Display

- [x] **LEVL-01**: Island level in GUI matches `/island level` command output exactly
- [x] **LEVL-02**: Level updates reflect immediately (no stale cached values)

### Island Visiting

- [ ] **VIST-01**: Right-clicking an island only teleports if owner has active warp sign
- [ ] **VIST-02**: GUI hides "Right-click to visit" hint when island has no warp sign
- [ ] **VIST-03**: GUI shows warp availability indicator (✓ has warp / no indicator if none)
- [ ] **VIST-04**: Clicking island without warp shows "This island doesn't have a warp" message

### Relocation Safety

- [ ] **RELC-01**: Relocation only teleports players who were actually on the old island location
- [ ] **RELC-02**: Players elsewhere in the world are not affected by relocation

## Future Requirements

None — patch release only.

## Out of Scope

| Feature | Reason |
|---------|--------|
| New features | Patch release — bugfixes only |
| Warps addon as hard dependency | Use reflection/optional integration |
| Visit without warp | User requirement: warp sign required |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| LEVL-01 | Phase 8 | Complete |
| LEVL-02 | Phase 8 | Complete |
| VIST-01 | Phase 9 | Pending |
| VIST-02 | Phase 9 | Pending |
| VIST-03 | Phase 9 | Pending |
| VIST-04 | Phase 9 | Pending |
| RELC-01 | Phase 10 | Pending |
| RELC-02 | Phase 10 | Pending |

**Coverage:**
- v1.1.1 requirements: 8 total
- Mapped to phases: 8
- Unmapped: 0 ✓

---
*Requirements defined: 2026-01-20*
*Last updated: 2026-01-20 after Phase 8 completion*
