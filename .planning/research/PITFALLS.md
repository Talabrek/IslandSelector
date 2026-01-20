# Domain Pitfalls

**Domain:** BentoBox addon configuration, command, and feature removal changes
**Project:** IslandSelector modifications
**Researched:** 2026-01-20
**Focus:** Config toggle, command alias, feature removal

---

## Critical Pitfalls

Mistakes that cause functionality breaks, runtime errors, or confusing user experience.

### Pitfall 1: Incomplete Feature Toggle - GUI Button Still Visible

**What goes wrong:** Adding a config toggle like `slots-enabled: false` but only blocking the command. The GUI button in MainGridGUI remains visible and clickable, confusing users when nothing happens or they get an error.

**Why it happens:** Feature toggles need to be enforced at multiple entry points:
1. Command execution (`SlotsCommand.execute()`)
2. GUI button visibility (`MainGridGUI.populateControlButtons()`)
3. GUI click handlers (`SharedGridGUIListener.onInventoryClick()`)

**Consequences:**
- Users see "Island Slots" button but clicking does nothing
- Inconsistent experience between command and GUI
- Support tickets asking why slots feature is "broken"

**Prevention:**
When adding a feature toggle, grep for ALL references to the feature. Check:
- `SlotsCommand.java` - command execution (add check at line ~28)
- `MainGridGUI.java` lines 720-728 - button rendering (currently checks only FAWE availability)
- `SharedGridGUIListener.java` lines 84-89 - click handler

In MainGridGUI, the slots button rendering must check the new config:
```java
if (settings.isSlotsEnabled() && addon.isSchematicOperationsAvailable()) {
    // render button
}
```

In SharedGridGUIListener, the click handler must also check before opening SlotSelectionGUI.

**Detection:**
- Test disabled state by setting config to `false`
- Click everywhere in GUI to find orphaned buttons
- Run all related commands

**Applies to:** `slots-enabled` config toggle

---

### Pitfall 2: Config Reload Does Not Apply to Open GUIs

**What goes wrong:** Admin reloads config with `/islandselector admin reload`, but players with GUIs already open see stale state. Toggle changes don't apply until they close and reopen.

**Why it happens:** The `onReload()` method (IslandSelector.java lines 254-271) reloads the Settings object, but existing GUI instances may:
- Hold references to the old Settings object
- Have cached values during construction
- Never re-check settings after initial render

**Consequences:**
- Admin disables slots, but player with open GUI can still access it
- Race condition between config reload and user actions
- Difficult to debug "sometimes works, sometimes doesn't" reports

**Prevention:**
- Option A (Recommended): GUIs should always read fresh from `addon.getSettings()` at render/action time, not cache settings in constructor
- Option B: Close all open IslandSelector GUIs when config reloads (heavyweight)
- Option C: Document that config reload requires players to close GUIs

For this project, MainGridGUI already calls `addon.getSettings()` in methods - verify the toggle check follows this pattern.

**Detection:**
1. Open GUI as player
2. As admin, run reload command
3. Click feature that was just toggled - does it respect new config?

**Applies to:** `slots-enabled` config toggle, any future toggles

---

### Pitfall 3: Orphaned References After Feature Removal

**What goes wrong:** Removing the neighbors feature but leaving references scattered across the codebase:

**Known reference locations for neighbors:**
| File | Location | Type |
|------|----------|------|
| `addon.yml` | Lines 43-45 | Permission node |
| `en-US.yml` | Lines 65, 100-106, 113 | Locale strings |
| `PlaceholderAPIIntegration.java` | Lines 94-96, 186-228 | Placeholder + method |
| `MainGridGUI.java` | BOT_NEIGHBORHOOD_SLOT (line 68), populateControlButtons() | GUI constant + rendering |
| `SharedGridGUIListener.java` | Lines 79-82 | Click handler |
| `IslandSelectorCommand.java` | Line 37 | Subcommand registration |
| `NeighborsCommand.java` | Entire file | Command class |
| `NeighborhoodGUI.java` | Entire file | GUI class |

**Why it happens:** Features have tentacles throughout the codebase. Simple grep for class name misses:
- GUI slot constants
- Locale keys
- Permission nodes
- Placeholder identifiers
- Alternative spellings (neighbourhood vs neighborhood)

**Consequences:**
- Compile errors if class is deleted but references remain (imports)
- Runtime NPE if placeholder calls deleted method
- Orphaned permissions in addon.yml waste space and confuse admins
- Locale strings for non-existent features confuse translators

**Prevention:**
Search checklist before removing a feature:
```bash
grep -ri "NeighborhoodGUI" src/
grep -ri "neighbors" src/ --include="*.java" --include="*.yml"
grep -ri "neighbourhood" src/
grep -ri "neighborhood" src/
grep -ri "NEIGHBORHOOD" src/
```

**Detection:**
- Compile after removal - catches import/reference errors
- Run placeholder tests - catches runtime errors
- grep for the feature name variants

**Applies to:** Removing neighbors GUI and command

---

### Pitfall 4: Broken Back Navigation After Feature Removal

**What goes wrong:** NeighborhoodGUI has navigation buttons that create a GUI graph. Removing NeighborhoodGUI breaks inbound navigation.

**Navigation graph:**
```
MainGridGUI ----[slot 49]----> NeighborhoodGUI
     ^                              |
     |<----[slot 18 "Back"]---------|
     |                              |
     |<----[slot 20 "Slots"]------> SlotSelectionGUI
```

**Key locations:**
- MainGridGUI line 68: `BOT_NEIGHBORHOOD_SLOT = 49`
- MainGridGUI lines 717-720: "Neighborhood" button rendering
- SharedGridGUIListener lines 79-82: Click handler opens NeighborhoodGUI

**Why it happens:** Removing NeighborhoodGUI requires updating inbound links but these are in different files from the removed feature.

**Consequences:**
- Clicking "Neighborhood" button after removal causes NPE (class not found)
- Or silent failure if import is removed but button remains
- Users get confused by non-functional button

**Prevention:**
1. Map the navigation graph before removal
2. Update MainGridGUI to not render the button (or render filler)
3. Update SharedGridGUIListener to remove the click handler
4. Decide: Should slot 49 become empty/filler or be repurposed?

**Detection:**
- Navigate to every GUI and click every button
- Check SharedGridGUIListener for references to removed GUI class

**Applies to:** Removing neighbors GUI

---

## Moderate Pitfalls

Mistakes that cause confusion, technical debt, or minor bugs.

### Pitfall 5: Command Alias Conflicts

**What goes wrong:** Adding `/map` alias conflicts with existing server plugins.

BentoBox CompositeCommand constructor takes varargs for aliases:
```java
// Current (IslandSelectorCommand.java line 19)
super(addon, "islandselector", "is", "isgrid");

// Proposed
super(addon, "islandselector", "is", "isgrid", "map");
```

**Why it happens:** Short, memorable aliases like `/map` are commonly used by:
- Dynmap (`/map`, `/dynmap`)
- BlueMap (`/map`, `/bluemap`)
- squaremap, Pl3xMap
- Server-specific commands

**Consequences:**
- Command conflict warnings at startup
- Unpredictable behavior - which plugin gets the command?
- Player confusion when `/map` does unexpected thing

**Prevention:**
1. Before adding alias, check for conflicts on test server:
   ```
   /plugins
   /map
   ```
2. Use more specific aliases: `/ismap`, `/gridmap`, `/islandmap`
3. Document the alias in README so admins know it exists
4. Consider making alias configurable in config.yml

**Detection:**
- Server startup logs show "Command map is already defined by [plugin]"
- Testing `/map` produces wrong behavior

**Applies to:** Adding `/map` alias

---

### Pitfall 6: Locale Key Mismatch After Feature Changes

**What goes wrong:** Removing neighbors feature but not removing locale keys creates orphaned strings.

**Locale keys related to neighbors:**
| Key | Line | Purpose |
|-----|------|---------|
| `gui.controls.neighborhood` | 64 | Button name |
| `gui.controls.neighborhood-desc` | 65 | Button tooltip |
| `gui.neighborhood.title` | 102 | GUI title |
| `gui.neighborhood.your-island` | 103 | Center label |
| `gui.neighborhood.back` | 104 | Back button |
| `gui.neighborhood.slots` | 105 | Slots button |
| `gui.neighborhood.close` | 106 | Close button |
| `gui.confirmation.claim-neighbors` | 113 | May still be needed! |

**Why it happens:** BentoBox uses locale paths like `commands.islandselector.neighbors.description` - these are strings, not compile-time checked.

**Consequences:**
- Orphaned strings in locale file (minor - just clutter)
- Missing strings cause "[missing translation]" in-game
- Locale file grows with dead keys over time
- `claim-neighbors` might still be used in ConfirmationGUI - verify before removing!

**Prevention:**
1. After removal, grep locale file for feature name
2. Check if `claim-neighbors` is used elsewhere before removing
3. Consider keeping locale strings for one release cycle (deprecated)

**Detection:**
- grep locale file for removed feature name
- Test in-game for "[missing translation]" messages

**Applies to:** Removing neighbors feature

---

### Pitfall 7: Permission Node Removal Breaking Server Configs

**What goes wrong:** Removing `islandselector.neighbors` permission from addon.yml (lines 43-45). Server admins who granted this permission to groups now have orphaned permission in their LuckPerms/GroupManager config.

**Current permission in addon.yml:**
```yaml
islandselector.neighbors:
  description: Can use neighborhood view
  default: true
```

**Why it happens:** Permission removal is a breaking change for server configurations.

**Consequences:**
- LuckPerms warnings about unknown permissions (depends on config)
- Admin confusion when permission check shows "undefined"
- Documentation/wiki references outdated permissions

**Prevention:**
1. Document permission removal in changelog/release notes
2. Consider deprecation period: keep permission but make it no-op
3. Communicate to server admins in update notes
4. Add migration note: "Remove `islandselector.neighbors` from your permission configs"

**Detection:**
- Check addon.yml for removed feature's permissions
- Review LuckPerms verbose output after removal

**Applies to:** Removing neighbors feature

---

### Pitfall 8: PlaceholderAPI Placeholder Removal

**What goes wrong:** `PlaceholderAPIIntegration` provides `%islandselector_neighbors_online%` placeholder (lines 94-96, method 186-228). Removing neighbors feature should address this.

**Current implementation:**
```java
// Line 94-96
if (identifier.equals("neighbors_online")) {
    return String.valueOf(getOnlineNeighborsCount(playerUUID));
}

// Lines 186-228: getOnlineNeighborsCount() method
```

**Decision point:** The placeholder calculates online players in 8 adjacent grid locations. This could work without the NeighborhoodGUI - it's mathematically independent.

**Options:**
1. **Remove entirely:** Delete lines 94-96 and method 186-228
2. **Keep independent:** Document that it works without the GUI
3. **Deprecate:** Return "0" or empty string with console warning

**Consequences:**
- If removed without updating: Runtime error when placeholder requested
- If kept: Works but creates inconsistency (no GUI but placeholder exists)
- Server admins using placeholder in scoreboard/tab get broken display

**Prevention:**
1. Decide: Remove placeholder or keep it independent of GUI?
2. If removing: Delete the handler and method
3. If keeping: Document in changelog that it's independent
4. Either way: Note in changelog

**Detection:**
- Test `%islandselector_neighbors_online%` placeholder after removal
- Check PlaceholderAPIIntegration for references to removed feature

**Applies to:** Removing neighbors feature

---

## Minor Pitfalls

Mistakes that cause annoyance but are easily fixable.

### Pitfall 9: Inconsistent Config Key Naming

**What goes wrong:** New config key doesn't follow existing naming patterns.

**Existing patterns in Settings.java:**
```java
@ConfigEntry(path = "slots.default-slots")     // slots group
@ConfigEntry(path = "slots.max-slots")         // slots group
@ConfigEntry(path = "slots.switch-cooldown")   // slots group
@ConfigEntry(path = "backups.enabled")         // feature toggle in category
@ConfigEntry(path = "integration.vault.enabled") // integration toggle
```

**Good naming:** `slots.enabled` - follows existing `slots.*` group and `*.enabled` pattern
**Bad naming:** `slots-enabled` - inconsistent with nested structure

**Consequences:**
- Config file looks inconsistent
- Harder to find related settings
- Future confusion about where to add new settings

**Prevention:**
Use: `@ConfigEntry(path = "slots.enabled")` - consistent with existing patterns

**Detection:**
- Review Settings.java patterns before adding new field
- Config file visual inspection

**Applies to:** `slots-enabled` config toggle

---

### Pitfall 10: Default Value Choices for New Config

**What goes wrong:** Adding `slots.enabled` with default `true` or `false` without considering existing servers.

**Upgrade scenarios:**
- **Default `true`:** Matches current behavior - slots work - backward compatible
- **Default `false`:** Existing servers upgrade and slots feature suddenly disappears

**Prevention:**
- Default should match current behavior for backward compatibility
- `slots.enabled: true` preserves current functionality for upgrades
- Document the new setting in changelog

**Detection:**
- Consider upgrade scenario before committing default value

**Applies to:** `slots-enabled` config toggle

---

### Pitfall 11: Slots Button Uses FAWE Check - Need Both Checks

**What goes wrong:** MainGridGUI already conditionally shows slots button based on FAWE availability (lines 722-728). Adding a config toggle means TWO conditions must be true.

**Current code:**
```java
// Slot Selection - only show if FAWE is available
if (addon.isSchematicOperationsAvailable()) {
    ItemStack slots = createButton(Material.CHEST, "&6Island Slots", ...);
    inventory.setItem(BOT_SLOTS_SLOT, slots);
}
```

**Needed code:**
```java
// Slot Selection - only show if enabled AND FAWE is available
if (addon.getSettings().isSlotsEnabled() && addon.isSchematicOperationsAvailable()) {
    // ...
}
```

**Why it matters:** Don't replace the FAWE check - both conditions matter. Slots require both:
1. Admin has enabled the feature (new toggle)
2. FAWE is installed (existing requirement)

**Applies to:** `slots-enabled` config toggle

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| Config toggle (slots-enabled) | #1 Incomplete toggle, #2 Reload issues, #11 Both checks needed | Check ALL entry points (command + GUI + listener), verify both FAWE and config are checked |
| Command alias (/map) | #5 Alias conflicts | Test for conflicts, consider server-specific prefix |
| Feature removal (neighbors) | #3 Orphaned refs, #4 Broken nav, #6 Locale, #7 Perms, #8 Placeholders | Comprehensive grep with all spelling variants, map navigation graph |

---

## Pre-Implementation Checklist

Before making these changes, verify:

**For Config Toggle:**
- [ ] Settings.java: Add field with `@ConfigEntry(path = "slots.enabled")` and default `true`
- [ ] SlotsCommand.java: Check `getSettings().isSlotsEnabled()` before executing
- [ ] MainGridGUI.java: Check settings AND FAWE before rendering button
- [ ] SharedGridGUIListener.java: Check settings before opening SlotSelectionGUI
- [ ] Test reload behavior with GUI open

**For Command Alias:**
- [ ] Check `/map` availability on test server
- [ ] Consider more specific alias if conflict exists
- [ ] Document alias in README/changelog

**For Neighbors Removal:**
- [ ] Grep all variations: `neighbors`, `neighbourhood`, `neighborhood`, `NeighborhoodGUI`, `NeighborsCommand`, `NEIGHBORHOOD`
- [ ] List files to modify: IslandSelectorCommand, MainGridGUI, SharedGridGUIListener
- [ ] List files to delete: NeighborsCommand.java, NeighborhoodGUI.java
- [ ] Decide on placeholder: remove or keep independent?
- [ ] Decide on locale: remove or deprecate?
- [ ] Update addon.yml permissions
- [ ] Update en-US.yml locales
- [ ] Test GUI navigation after removal

---

## Sources

- Direct code analysis of IslandSelector codebase
- Settings.java: Existing config patterns (lines 42-59 for slots group)
- MainGridGUI.java: GUI button rendering (lines 68, 717-728)
- SharedGridGUIListener.java: Click handlers (lines 79-94)
- IslandSelectorCommand.java: Subcommand registration (line 37)
- PlaceholderAPIIntegration.java: Placeholder definitions (lines 94-96, 186-228)
- addon.yml: Permission definitions (lines 43-45)
- en-US.yml: Locale strings (lines 64-65, 100-106, 113)
- BentoBox CompositeCommand API: Alias handling via constructor varargs
- HIGH confidence - based on actual project code review
