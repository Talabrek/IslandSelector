---
phase: 07
plan: 01
subsystem: gui
tags: [cleanup, simplification, feature-removal]
dependency-graph:
  requires: []
  provides:
    - clean-codebase-no-neighbors
  affects: []
tech-stack:
  added: []
  patterns: []
file-tracking:
  key-files:
    created: []
    modified:
      - src/main/java/world/bentobox/islandselector/commands/IslandSelectorCommand.java
      - src/main/java/world/bentobox/islandselector/gui/MainGridGUI.java
      - src/main/java/world/bentobox/islandselector/gui/SharedGridGUIListener.java
      - src/main/resources/addon.yml
      - src/main/resources/locales/en-US.yml
      - src/main/resources/config.yml
    deleted:
      - src/main/java/world/bentobox/islandselector/commands/NeighborsCommand.java
      - src/main/java/world/bentobox/islandselector/gui/NeighborhoodGUI.java
decisions: []
metrics:
  duration: 3 minutes
  completed: 2026-01-20
---

# Phase 7 Plan 1: Neighbors Removal Summary

**One-liner:** Complete removal of neighborhood GUI feature including command, GUI class, permissions, and locale keys

## What Was Built

Removed the neighborhood/neighbors feature from the IslandSelector addon as part of v1.1 simplification:

1. **Deleted source files:**
   - `NeighborsCommand.java` - The `/islandselector neighbors` command handler
   - `NeighborhoodGUI.java` - The 3x3 neighborhood view GUI (512 lines)

2. **Removed code references:**
   - Removed subcommand registration from `IslandSelectorCommand.java`
   - Removed `BOT_NEIGHBORHOOD_SLOT` constant and neighborhood button creation from `MainGridGUI.java`
   - Removed `getNeighborhoodSlot()` getter method from `MainGridGUI.java`
   - Removed neighborhood click handler from `SharedGridGUIListener.java`

3. **Cleaned up resource files:**
   - Removed `islandselector.neighbors` permission from `addon.yml`
   - Removed neighborhood control button locale keys from `en-US.yml`
   - Removed neighborhood GUI section (title, your-island, back, slots, close) from `en-US.yml`
   - Removed `neighborhood: FILLED_MAP` button config from `config.yml`

## Commits

| Commit | Type | Description |
|--------|------|-------------|
| e21a9d5 | feat | Remove neighbors feature source files |
| 139a9f8 | chore | Remove neighbors feature from resource files |

## Key Implementation Details

- The `neighbors_online` placeholder in `PlaceholderAPIIntegration.java` was intentionally **kept** - it calculates online players in adjacent grid cells independently of the GUI and remains useful for scoreboards/tab lists
- The `claim-neighbors` locale key in confirmation GUI was **kept** - it shows neighbor count during island claiming and is unrelated to the neighborhood feature
- Slot 49 in the main GUI (formerly neighborhood button) will now be auto-filled by `fillEmptySlots()` with filler glass pane

## Verification Results

- Build: SUCCESS (JAR created in target/)
- No remaining references to `NeighborhoodGUI`, `NeighborsCommand`, or `getNeighborhoodSlot` in Java files
- No remaining references to `neighborhood` in resource files (except unrelated `claim-neighbors`)
- Permission `islandselector.neighbors` removed from addon.yml

## Deviations from Plan

None - plan executed exactly as written.

## Success Criteria Status

- [x] REM-01: Main GUI slot 49 auto-filled by `fillEmptySlots()` - no neighbors button
- [x] REM-02: `/islandselector neighbors` returns unknown command (subcommand not registered)
- [x] REM-03: NeighborhoodGUI.java and NeighborsCommand.java deleted
- [x] REM-04: `islandselector.neighbors` permission removed from addon.yml
- [x] REM-05: Neighborhood locale keys removed from en-US.yml
- [x] Project compiles and builds successfully

## Next Steps

Phase 7 (Neighbors Removal) is complete. This completes the v1.1 Simplification milestone:
- Phase 5: Config toggle for slots (COMPLETE)
- Phase 6: Command alias (COMPLETE)
- Phase 7: Neighbors removal (COMPLETE)
