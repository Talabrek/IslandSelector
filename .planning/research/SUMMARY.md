# Project Research Summary

**Project:** IslandSelector v1.1 Simplification
**Domain:** BentoBox addon configuration and feature management
**Researched:** 2026-01-20
**Confidence:** HIGH

## Executive Summary

The v1.1 simplification milestone involves three well-understood changes to the IslandSelector BentoBox addon: adding a config toggle to disable the slot system, adding a `/map` command alias, and removing the neighbors GUI/command entirely. The existing codebase already demonstrates all required patterns for these changes, making this a straightforward refactoring task with high confidence.

The recommended approach is to implement these as three independent changes that can be developed in parallel but should be tested in sequence. The config toggle uses the exact same `@ConfigEntry` pattern already used 20+ times in Settings.java. The command alias requires a single-line addition to the existing varargs constructor. The neighbors removal is the most involved, requiring updates to 6+ files, but follows standard BentoBox patterns with no complex dependencies.

The primary risk is incomplete removal of the neighbors feature, leaving orphaned references in GUI code, locale files, or placeholder integrations. A secondary risk is the `/map` alias conflicting with mapping plugins like Dynmap. Both are easily mitigated with thorough grep searching and pre-implementation conflict checking.

## Key Findings

### Recommended Stack

No new technologies required. All changes use existing BentoBox APIs and patterns already demonstrated in the codebase.

**Core technologies (unchanged):**
- **BentoBox Config API:** Settings.java with `@ConfigEntry` annotations - well-established pattern
- **BentoBox CompositeCommand:** Command registration with varargs aliases - single line change
- **Standard Java:** File deletion and reference cleanup for feature removal

### Expected Features

**Must have (table stakes):**
- Config toggle fully disables slots: command blocked AND GUI button hidden
- Clear error message when attempting disabled command
- `/map` alias works identically to `/islandselector`
- Clean removal of neighbors with no orphaned references

**Should have (competitive):**
- Tab completion filters disabled commands
- Descriptive config comments explaining what disabling does
- Startup log confirming feature state

**Defer (v2+):**
- Configurable command aliases (hardcoded is fine for now)
- Runtime toggle without server restart (current reload already works)

### Architecture Approach

The changes fit cleanly into the existing architecture with no structural modifications. The config toggle adds one field to Settings.java and guards in two entry points (SlotsCommand.execute() and MainGridGUI button rendering). The command alias is purely additive. The neighbors removal is subtractive with no ripple effects on other features.

**Major components affected:**
1. **Settings.java** - Add `slotsEnabled` boolean field with getter
2. **IslandSelectorCommand.java** - Add "map" alias, remove NeighborsCommand registration
3. **MainGridGUI.java** - Conditionally render slots button, remove neighborhood button
4. **SlotsCommand.java** - Add config check before execution
5. **SharedGridGUIListener.java** - Remove neighborhood click handler
6. **Files to delete** - NeighborsCommand.java, NeighborhoodGUI.java

### Critical Pitfalls

1. **Incomplete Feature Toggle** - Adding config toggle but only blocking command leaves GUI button visible and clickable. Must check BOTH SlotsCommand.execute() AND MainGridGUI button rendering AND SharedGridGUIListener click handler.

2. **Orphaned References After Removal** - Neighbors feature has tentacles in 8+ files. Search must include all spelling variants: `neighbors`, `neighbourhood`, `neighborhood`, `NeighborhoodGUI`, `NEIGHBORHOOD`.

3. **Command Alias Conflicts** - `/map` commonly used by Dynmap, BlueMap, and other mapping plugins. Test for conflicts before committing; consider `/ismap` if conflicts exist.

4. **Both Checks Needed for Slots Button** - MainGridGUI already conditionally shows slots button based on FAWE availability. New code must be `settings.isSlotsEnabled() && addon.isSchematicOperationsAvailable()` - don't replace the FAWE check.

5. **Placeholder Removal Decision** - `%islandselector_neighbors_online%` placeholder works independently of GUI. Decide whether to remove (breaking change for users) or keep (works without GUI but creates inconsistency).

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Config Toggle
**Rationale:** Self-contained change, establishes pattern for future feature toggles, lowest risk
**Delivers:** `slots.enabled` config option that disables command and hides GUI button
**Addresses:** "Config option to disable slot system" requirement
**Avoids:** Pitfall #1 (incomplete toggle) by checking all entry points

**Implementation points:**
- Settings.java: Add field with `@ConfigEntry(path = "slots.enabled")`, default `true`
- SlotsCommand.java: Check `getSettings().isSlotsEnabled()` at start of execute()
- MainGridGUI.java: Add `&& addon.getSettings().isSlotsEnabled()` to existing FAWE check
- SharedGridGUIListener.java: Add same check before opening SlotSelectionGUI

### Phase 2: Command Alias
**Rationale:** Single-line change, completely independent, trivial verification
**Delivers:** `/map` as working alias for `/islandselector`
**Addresses:** "/map command alias" requirement
**Avoids:** Pitfall #5 (alias conflicts) by testing before deployment

**Implementation points:**
- IslandSelectorCommand.java line 20: Change `super(addon, "islandselector", "is", "isgrid")` to `super(addon, "islandselector", "is", "isgrid", "map")`

### Phase 3: Neighbors Removal
**Rationale:** Most files affected, benefits from stable base, requires comprehensive cleanup
**Delivers:** Clean codebase with neighbors feature completely removed
**Addresses:** "Remove neighbors GUI and command entirely" requirement
**Avoids:** Pitfalls #2 (orphaned refs), #4 (broken nav), #6-8 (locale/perms/placeholders)

**Implementation points:**
- Remove subcommand registration from IslandSelectorCommand.java
- Remove button creation from MainGridGUI.java
- Remove click handler from SharedGridGUIListener.java
- Delete NeighborsCommand.java
- Delete NeighborhoodGUI.java
- Remove permission from addon.yml
- Remove locale strings from en-US.yml
- Remove placeholder from PlaceholderAPIIntegration.java (or keep - decide)

### Phase Ordering Rationale

- **Phase 1 before 2:** No technical dependency, but establishes config patterns
- **Phase 2 independent:** Can actually be done in parallel with Phase 1
- **Phase 3 last:** Multi-file changes benefit from a clean, tested base; if something breaks, easier to isolate cause

### Research Flags

Phases with standard patterns (skip additional research):
- **Phase 1 (Config Toggle):** Well-documented - Settings.java already has 20+ examples of this exact pattern
- **Phase 2 (Command Alias):** Well-documented - CompositeCommand varargs constructor is standard BentoBox
- **Phase 3 (Neighbors Removal):** Standard Java file deletion - just need comprehensive grep

No phases need deeper research. All patterns are already demonstrated in the codebase.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | No new tech, all existing BentoBox patterns |
| Features | HIGH | Requirements are unambiguous, patterns exist in codebase |
| Architecture | HIGH | Direct analysis of actual source files |
| Pitfalls | HIGH | Identified from real codebase integration points |

**Overall confidence:** HIGH

### Gaps to Address

- **Alias conflict check:** Need to verify `/map` availability on target servers before deployment. Consider providing alternative aliases in documentation if conflicts exist.

- **Placeholder decision:** Research did not prescribe whether to remove or keep `%islandselector_neighbors_online%`. Recommend: remove it along with the feature for consistency, but document in changelog as breaking change.

- **Locale file completeness:** Only en-US.yml analyzed. If other locales exist, they need same cleanup.

## Sources

### Primary (HIGH confidence)
- **IslandSelector codebase direct analysis** - Settings.java, SlotsCommand.java, MainGridGUI.java, SharedGridGUIListener.java, IslandSelectorCommand.java
- **BentoBox CompositeCommand source** - https://github.com/BentoBoxWorld/BentoBox/blob/master/src/main/java/world/bentobox/bentobox/api/commands/CompositeCommand.java
- **BentoBox Config API docs** - https://docs.bentobox.world/en/latest/BentoBox/Config-API/

### Secondary (MEDIUM confidence)
- **BentoBox commands reference** - https://docs.bentobox.world/en/latest/BentoBox/Commands/

---
*Research completed: 2026-01-20*
*Ready for roadmap: yes*
