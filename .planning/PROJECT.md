# IslandSelector

## What This Is

A BentoBox addon for Minecraft that provides island selection and management features with Nova machine state preservation. The project uses a standard Maven structure with Java 17.

## Core Value

Players can visually select their island location from a grid-based GUI.

## Current State

**Shipped:** v1.2 Nova Integration (2026-01-27)
**Codebase:** 80 Java files, 29,946 LOC
**Build:** Maven, Java 17, compiles successfully

```
IslandSelector/
├── src/
│   ├── main/java/world/bentobox/islandselector/
│   └── main/resources/ (addon.yml, config.yml, locales/)
├── pom.xml
└── .git/
```

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
- Fix island level mismatch between /islandselector and /island level — v1.1.1 (uses overworld-only, non-cached API)
- Restrict island visiting to only allow warps when permitted — v1.1.1 (WarpIntegration created)
- Show warp availability status in GUI hover text — v1.1.1 (green checkmark "Has Warp" indicator)
- Fix relocation teleporting unrelated players to spawn — v1.1.1 (island.onIsland() checks added)

**v1.2 Nova Integration:**
- NovaIntegration updated to Nova 0.17+ API (new package paths, WorldDataManager) — v1.2
- System captures all Nova tile entities in island region before operations — v1.2
- System restores Nova blocks at new location after operations complete — v1.2
- System removes Nova blocks properly via BlockUtils.breakBlock() before WorldEdit — v1.2
- Machine inventory contents preserved during capture/restore cycle — v1.2
- Machine owner preserved during capture/restore cycle — v1.2
- Player sees visual feedback message showing Nova machines preserved count — v1.2
- Player sees error message if Nova machines fail to restore — v1.2
- RelocationManager calls NovaIntegration before/after island move — v1.2
- SlotSwitchManager calls NovaIntegration during slot save/load — v1.2
- BackupManager calls NovaIntegration during backup/restore — v1.2
- Nova integration handles multi-dimension islands (all dimensions processed) — v1.2
- Nova block scanning optimized with caching for performance — v1.2
- Config option `nova.enabled` to enable/disable Nova integration — v1.2

### Active

(No active requirements — planning next milestone)

### Out of Scope

- Modifying slot system logic — only adding enable/disable toggle (v1.1 scope)
- Other GUI changes — only removing neighbors button (v1.1 scope)
- Warps addon as hard dependency — use reflection/optional integration
- Nova as hard dependency — use reflection/optional integration
- Direct tile entity data manipulation — use drop-based approach instead
- FAWE-Nova direct compatibility — Nova docs say FAWE incompatible, we work around it
- Advanced Nova state (energy, progress, upgrades) — deferred to future milestone

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
| Nova integration default enabled | Beneficial when available, opt-out not opt-in | Done (v1.2) |
| Use TileEntity.getDrops(true) for state | Nova's official serialization includes inventory/owner | Done (v1.2) |
| Use Context.EMPTY for automated removal | Proper Nova API for non-player operations | Done (v1.2) |
| ReflectionCache with final fields | Thread-safe, fail-fast initialization, 5-10x perf gain | Done (v1.2) |
| Nova blocks in separate .nova files | Separates concerns from schematic files | Done (v1.2) |

## Milestones

See `.planning/MILESTONES.md` for shipped milestones.

---
*Last updated: 2026-01-27 after v1.2 milestone complete*
