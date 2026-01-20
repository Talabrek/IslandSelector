---
phase: 05-config-toggle
plan: 01
subsystem: configuration
tags: [config, slots, toggle, settings]

dependency-graph:
  requires: [v1.0-cleanup]
  provides: [slots-config-toggle]
  affects: [slots-command, main-grid-gui]

tech-stack:
  added: []
  patterns: [config-driven-feature-toggle]

key-files:
  created: []
  modified:
    - src/main/java/world/bentobox/islandselector/Settings.java
    - src/main/java/world/bentobox/islandselector/commands/SlotsCommand.java
    - src/main/java/world/bentobox/islandselector/gui/MainGridGUI.java
    - src/main/java/world/bentobox/islandselector/gui/SharedGridGUIListener.java

decisions:
  - id: config-default-true
    summary: Default slotsEnabled to true for backward compatibility
    rationale: Existing servers should continue working after update

metrics:
  duration: 5 minutes
  completed: 2026-01-20
---

# Phase 05 Plan 01: Config Toggle Summary

**One-liner:** Added slots.enabled config toggle to enable/disable multi-slot island system, with checks in command and GUI

## What Was Built

### Config Field (Settings.java)
- Added `slotsEnabled` boolean field with default `true`
- Added `@ConfigEntry(path = "slots.enabled")` annotation for BentoBox config system
- Added `isSlotsEnabled()` getter and `setSlotsEnabled()` setter methods
- Added detailed `@ConfigComment` documentation explaining the toggle behavior

### Command Check (SlotsCommand.java)
- Added config check before the existing FAWE check in `execute()` method
- When slots disabled, shows message: "&cThe slot system is disabled on this server."
- Returns false to block command execution

### GUI Button Visibility (MainGridGUI.java)
- Modified slot button conditional from `isSchematicOperationsAvailable()` to `isSlotsEnabled() && isSchematicOperationsAvailable()`
- Slot button is now hidden when either condition is false

### Click Handler (SharedGridGUIListener.java)
- Modified slot click handler to check both `isSlotsEnabled()` and `isSchematicOperationsAvailable()`
- Prevents opening SlotSelectionGUI when slots are disabled (defense in depth)

## Commit History

| Task | Commit | Description |
|------|--------|-------------|
| 1 | 1cf42dd | Add slotsEnabled config field to Settings.java |
| 2 | 3dc9d12 | Add slots config toggle checks to command and GUI |

## Verification Results

1. `mvn clean package -q` - SUCCESS
2. JAR file created at `target/IslandSelector-1.0.0-SNAPSHOT.jar` - YES
3. All four files contain `isSlotsEnabled` references - VERIFIED
4. Config path is `slots.enabled` - VERIFIED
5. Default value is `true` - VERIFIED

## Deviations from Plan

None - plan executed exactly as written.

## Success Criteria Checklist

- [x] Settings.java has slotsEnabled field with @ConfigEntry(path = "slots.enabled")
- [x] Settings.java has isSlotsEnabled() getter returning the field
- [x] SlotsCommand.execute() checks isSlotsEnabled() before FAWE check
- [x] SlotsCommand shows "&cThe slot system is disabled on this server." when disabled
- [x] MainGridGUI only renders slot button when isSlotsEnabled() AND isSchematicOperationsAvailable()
- [x] SharedGridGUIListener only opens SlotSelectionGUI when isSlotsEnabled() AND isSchematicOperationsAvailable()
- [x] Build succeeds: `mvn clean package -q`

## Next Phase Readiness

**Phase 6 (Command Alias):** Ready to proceed - independent of this phase
**Phase 7 (Neighbors Removal):** Ready to proceed - independent of this phase

---
*Summary generated: 2026-01-20*
