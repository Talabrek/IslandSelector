# IslandSelector

## What This Is

A BentoBox addon for Minecraft that provides island selection and management features. The project uses a standard Maven structure with Java 17.

## Core Value

Players can visually select their island location from a grid-based GUI.

## Current State

**Shipped:** v1.1.1 Bugfixes (2026-01-21)
**Codebase:** 80 Java files, 28,922 LOC
**Build:** Maven, Java 17, compiles successfully

```
IslandSelector/
├── src/
│   ├── main/java/world/bentobox/islandselector/
│   └── main/resources/ (addon.yml, config.yml, locales/)
├── pom.xml
└── .git/
```

## Current Milestone: v1.2 Nova Integration

**Goal:** Preserve Nova machine state (inventory, energy, progress) when players relocate or switch island slots.

**Target features:**
- Full Nova tile entity state preservation during island operations
- Drop-based approach using Nova's own item serialization
- Integration with both relocation and slot switching workflows
- Multi-dimension support

**Technical approach:**
1. Use `TileEntityManager.getTileEntityAt()` to find Nova blocks
2. Use `TileEntity.getDrops(true)` to capture machine items with preserved state
3. Use `BlockManager.removeBlock()` to properly clean up before WorldEdit
4. After paste, re-place Nova blocks programmatically using `BlockManager.placeBlock()`
5. Use reflection to access Nova API (optional dependency pattern)

## Requirements

### Validated

**v1.0 Cleanup:**
- Java addon source code — 80 main + 3 test files
- Maven build configuration — pom.xml at root
- Resource files — addon.yml, config.yml, en-US.yml
- Standard project structure — src/ and pom.xml at root
- Clean repository — no Python scripts, backups, or nested git
- Build verification — mvn clean compile succeeds

**v1.1 Simplification:**
- Config option `slots.enabled` to enable/disable slot system
- Block `/islandselector slots` command when slots disabled
- Hide slots button in GUI when slots disabled
- Add `/map` command alias for `/islandselector`
- Remove neighbors GUI button from main GUI
- Remove `/islandselector neighbors` command and source files

**v1.1.1 Bugfixes:**
- ✓ Fix island level mismatch between /islandselector and /island level — v1.1.1 (uses overworld-only, non-cached API)
- ✓ Restrict island visiting to only allow warps when permitted — v1.1.1 (WarpIntegration created)
- ✓ Show warp availability status in GUI hover text — v1.1.1 (green checkmark "Has Warp" indicator)
- ✓ Fix relocation teleporting unrelated players to spawn — v1.1.1 (island.onIsland() checks added)

### Active

**v1.2 Nova Integration:**
- [ ] Rewrite NovaIntegration from scratch using current Nova 0.17+ API
- [ ] Capture Nova tile entities before island relocation using drop-based state preservation
- [ ] Remove Nova blocks properly before WorldEdit/FAWE operations
- [ ] Restore Nova blocks with preserved state after relocation completes
- [ ] Hook NovaIntegration into RelocationManager workflow
- [ ] Hook NovaIntegration into SlotSwitchManager workflow (slot save/restore)
- [ ] Add config option to enable/disable Nova integration
- [ ] Handle multi-dimension islands (capture/restore Nova blocks in all dimensions)

### Out of Scope

- Modifying slot system logic — only adding enable/disable toggle (v1.1 scope)
- Other GUI changes — only removing neighbors button (v1.1 scope)
- Warps addon as hard dependency — use reflection/optional integration
- Nova as hard dependency — use reflection/optional integration (v1.2 scope)
- Direct tile entity data manipulation — use drop-based approach instead (v1.2 scope)
- FAWE-Nova direct compatibility — Nova docs say FAWE incompatible, we work around it (v1.2 scope)

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Move src/ to root | Standard Maven project structure | Done (v1.0) |
| Delete nested .git | Only need one git repo at root | Done (v1.0) |
| Use git mv for moves | Preserves file history | Done (v1.0) |
| Replace agent README | Agent harness docs not relevant | Done (v1.0) |
| Default slotsEnabled to true | Existing servers continue working after update | Done (v1.1) |
| Map alias as fourth param | After islandselector, is, isgrid | Done (v1.1) |
| Keep neighbors_online placeholder | Useful for scoreboards independently of GUI | Done (v1.1) |
| Use overworld-only getFormattedIslandLevel | Matches Level addon /island level command | Done (v1.1.1) |
| Use reflection for WarpIntegration | Same pattern as LevelIntegration, no hard dependency | Done (v1.1.1) |
| Check location before relocation teleport | Capture state before async, pass through chain | Done (v1.1.1) |

## Milestones

See `.planning/MILESTONES.md` for shipped milestones.

---
*Last updated: 2026-01-26 after v1.2 milestone started*
