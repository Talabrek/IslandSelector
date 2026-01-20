# Phase 7: Neighbors Removal - Research

**Researched:** 2026-01-20
**Domain:** BentoBox addon feature removal (neighbors/neighborhood GUI and command)
**Confidence:** HIGH

## Summary

This research covers the complete removal of the neighbors feature from the IslandSelector addon. The neighbors feature allows players to view a 3x3 grid of their island and surrounding islands. The feature is being removed as part of v1.1 simplification.

The removal is straightforward but requires careful attention to ALL reference points. The neighbors feature has integration points in:
- 2 source files to delete (NeighborsCommand.java, NeighborhoodGUI.java)
- 3 source files to modify (IslandSelectorCommand.java, MainGridGUI.java, SharedGridGUIListener.java)
- 1 resource file to modify (addon.yml)
- 1 locale file to clean up (en-US.yml)
- 1 integration file with decision point (PlaceholderAPIIntegration.java)
- 1 config file to clean up (config.yml - neighborhood button reference)

**Primary recommendation:** Delete feature files, remove all references, and decide whether to keep or remove the `neighbors_online` placeholder (recommend KEEP - it's computationally independent).

## Standard Stack

This is a feature removal task, not a new feature. The "stack" here is the removal pattern for BentoBox addons.

### Core Operations

| Operation | Tool/Pattern | Notes |
|-----------|--------------|-------|
| Delete source files | File system delete | NeighborsCommand.java, NeighborhoodGUI.java |
| Remove imports | Edit Java files | All files importing deleted classes |
| Remove command registration | Edit IslandSelectorCommand | Line 37 |
| Remove GUI button | Edit MainGridGUI | BOT_NEIGHBORHOOD_SLOT constant and button code |
| Remove click handler | Edit SharedGridGUIListener | Lines 79-82 |
| Remove permission | Edit addon.yml | islandselector.neighbors section |
| Remove locale keys | Edit en-US.yml | neighborhood section and related keys |

### Alternatives Considered

| Decision | Option A | Option B | Recommendation |
|----------|----------|----------|----------------|
| File removal | Delete files completely | Comment out / deprecate | **Delete** - cleaner, no dead code |
| Placeholder | Remove `neighbors_online` | Keep independent | **Keep** - still useful, works without GUI |
| Locale keys | Remove immediately | Deprecate for 1 release | **Remove** - feature fully gone |

## Architecture Patterns

### Feature Integration Map

The neighbors feature integrates at these points:

```
IslandSelectorCommand.java (line 37)
    |
    +---> new NeighborsCommand(this)
              |
              +---> Opens NeighborhoodGUI.java

MainGridGUI.java (slot 49)
    |
    +---> Neighborhood button (BOT_NEIGHBORHOOD_SLOT)
              |
              +---> SharedGridGUIListener handles click
                        |
                        +---> new NeighborhoodGUI(addon, player).open()
```

### GUI Slot Layout Affected

```
MainGridGUI bottom control bar (slots 47-51):
[Search:47] [FindMyIsland:48] [Neighborhood:49] [Slots:50] [Close:51]
                                    ^^^^
                                  REMOVE
```

After removal, slot 49 should become filler (handled by `fillEmptySlots()` automatically).

### Files to Delete

| File | Path | Lines | Purpose |
|------|------|-------|---------|
| NeighborsCommand.java | `src/main/java/world/bentobox/islandselector/commands/` | 47 | `/neighbors` command |
| NeighborhoodGUI.java | `src/main/java/world/bentobox/islandselector/gui/` | 512 | 3x3 neighborhood view GUI |

### Files to Modify

| File | Changes Required |
|------|------------------|
| `IslandSelectorCommand.java` | Remove line 37: `new NeighborsCommand(this);` |
| `MainGridGUI.java` | Remove `BOT_NEIGHBORHOOD_SLOT` constant (line 68), remove button creation in `populateControlButtons()` (lines 717-719), remove `getNeighborhoodSlot()` getter (line 980) |
| `SharedGridGUIListener.java` | Remove import, remove click handler lines 79-82 |
| `addon.yml` | Remove permission block lines 43-46 (`islandselector.neighbors`) |
| `en-US.yml` | Remove neighborhood-related keys (lines 64-65, 100-106) |
| `config.yml` | Remove `neighborhood: FILLED_MAP` line (122) from gui.buttons |
| `PlaceholderAPIIntegration.java` | Decision: Remove or keep `neighbors_online` placeholder |

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Finding all references | Manual search | `grep -ri "neighbor"` with variants | Catches all spellings |
| Empty slot handling | Custom filler code | Existing `fillEmptySlots()` | Already handles nulls |
| Import cleanup | Manual deletion | IDE "Organize Imports" or grep | Catches transitive imports |

**Key insight:** Don't try to manually track all references. Use comprehensive grep patterns with multiple spellings.

## Common Pitfalls

### Pitfall 1: Incomplete Reference Removal

**What goes wrong:** Deleting NeighborhoodGUI.java but leaving import in SharedGridGUIListener. Compile error.

**Why it happens:** Java imports are easy to miss when removing features.

**How to avoid:** After deleting files, run `mvn clean compile` immediately. Fix any import errors.

**Warning signs:** "cannot find symbol" compile errors.

### Pitfall 2: Spelling Variants

**What goes wrong:** Searching for `neighbor` but missing `neighbourhood` (British spelling used in command aliases).

**Why it happens:** The codebase uses both spellings. See NeighborsCommand.java line 19:
```java
super(parent, "neighbors", "neighbourhood", "neighborhood");
```

**How to avoid:** Search for ALL variants:
- `neighbor`
- `neighbours`
- `neighbourhood`
- `neighborhood`
- `NeighborhoodGUI`
- `NeighborsCommand`
- `NEIGHBORHOOD`

**Warning signs:** Feature "still works" after supposed removal via unexpected alias.

### Pitfall 3: GUI Navigation Broken

**What goes wrong:** SharedGridGUIListener still tries to open NeighborhoodGUI after class is deleted.

**Why it happens:** Click handler code references deleted class.

**How to avoid:** Remove the click handler block in SharedGridGUIListener (lines 79-82):
```java
// REMOVE THIS BLOCK:
if (slot == gui.getNeighborhoodSlot()) {
    player.closeInventory();
    new NeighborhoodGUI(gui.getAddon(), player).open();
    return;
}
```

**Warning signs:** NPE or compile error when clicking where neighborhood button was.

### Pitfall 4: Orphaned Permission

**What goes wrong:** Permission `islandselector.neighbors` left in addon.yml but command removed.

**Why it happens:** Permissions are separate from code, easy to forget.

**How to avoid:** Remove from addon.yml lines 43-46:
```yaml
# REMOVE:
islandselector.neighbors:
  description: Can use neighborhood view
  default: true
```

**Warning signs:** Permission exists but does nothing.

### Pitfall 5: Placeholder Decision

**What goes wrong:** Either removing placeholder without noting impact, or keeping stale code.

**Why it happens:** `%islandselector_neighbors_online%` placeholder is mathematically independent of the GUI - it calculates online players in adjacent grid cells. This works without the NeighborhoodGUI.

**How to avoid:** Make explicit decision:
- **RECOMMENDED: Keep** - Placeholder is useful for scoreboards/tab. Document in changelog that it remains functional.
- **Alternative: Remove** - Delete lines 94-96 and method getOnlineNeighborsCount() (186-228). Note in changelog.

**Warning signs:** Server admins complain about broken scoreboards (if removed without notice).

## Code Examples

### Removal from IslandSelectorCommand.java

```java
// src/main/java/.../commands/IslandSelectorCommand.java
// BEFORE (line 37):
new NeighborsCommand(this);

// AFTER:
// Line deleted entirely
```

### Removal from MainGridGUI.java

```java
// src/main/java/.../gui/MainGridGUI.java

// REMOVE constant (line 68):
private static final int BOT_NEIGHBORHOOD_SLOT = 49;

// REMOVE from populateControlButtons() (lines 717-719):
ItemStack neighborhood = createButton(Material.FILLED_MAP, "&bNeighborhood",
    "&7View your neighbors");
inventory.setItem(BOT_NEIGHBORHOOD_SLOT, neighborhood);

// REMOVE getter (line 980):
public int getNeighborhoodSlot() {
    return BOT_NEIGHBORHOOD_SLOT;
}
```

### Removal from SharedGridGUIListener.java

```java
// src/main/java/.../gui/SharedGridGUIListener.java

// REMOVE import (top of file):
import world.bentobox.islandselector.gui.NeighborhoodGUI;

// REMOVE click handler (lines 79-82):
if (slot == gui.getNeighborhoodSlot()) {
    player.closeInventory();
    new NeighborhoodGUI(gui.getAddon(), player).open();
    return;
}
```

### Removal from addon.yml

```yaml
# src/main/resources/addon.yml

# REMOVE lines 43-46:
  islandselector.neighbors:
    description: Can use neighborhood view
    default: true
```

### Removal from en-US.yml

```yaml
# src/main/resources/locales/en-US.yml

# REMOVE lines 64-65 (under gui.grid.controls):
      neighborhood: "&aNeighborhood View"
      neighborhood-desc: "&7View your neighbors"

# REMOVE lines 100-106 (entire neighborhood section):
  # Neighborhood GUI
  neighborhood:
    title: "Your Neighborhood"
    your-island: "&a&l YOUR ISLAND"
    back: "&aBack to Grid"
    slots: "&eSlot Selection"
    close: "&cClose"
```

### Removal from config.yml

```yaml
# src/main/resources/config.yml

# REMOVE line 122 from gui.buttons section:
    neighborhood: FILLED_MAP
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Neighbors GUI existed | Neighbors GUI removed | v1.1 | Simpler addon, less maintenance |

**Deprecated/outdated:**
- `islandselector.neighbors` permission: No longer needed after removal
- Neighborhood locale keys: Orphaned, should be removed

## Open Questions

1. **PlaceholderAPI `neighbors_online` placeholder**
   - What we know: Placeholder works independently of NeighborhoodGUI
   - What's unclear: Do any servers actively use this placeholder?
   - Recommendation: **Keep** the placeholder. It provides value (online neighbor count) without the GUI. Document in changelog. LOW risk to keep, MEDIUM risk to remove (breaks scoreboards).

2. **CLAUDE.md and DEVELOPMENT_NOTES.md references**
   - What we know: These doc files reference NeighborhoodGUI
   - What's unclear: Are these auto-generated or manually maintained?
   - Recommendation: Update documentation references. CLAUDE.md line 32 lists NeighborhoodGUI.java in project structure. DEVELOPMENT_NOTES.md line 182 also lists it.

## Sources

### Primary (HIGH confidence)
- Direct codebase analysis:
  - `NeighborsCommand.java` - Full file read
  - `NeighborhoodGUI.java` - Full file read
  - `MainGridGUI.java` - Full file read (997 lines)
  - `SharedGridGUIListener.java` - Full file read (279 lines)
  - `IslandSelectorCommand.java` - Full file read (72 lines)
  - `addon.yml` - Full file read (124 lines)
  - `en-US.yml` - Full file read (362 lines)
  - `config.yml` - Full file read (232 lines)
  - `PlaceholderAPIIntegration.java` - Full file read (263 lines)

### Secondary (MEDIUM confidence)
- Prior research files:
  - `.planning/research/FEATURES.md` - Feature removal checklist
  - `.planning/research/PITFALLS.md` - Pitfall #3, #4, #6, #7, #8

## Metadata

**Confidence breakdown:**
- Files to delete: HIGH - Direct file paths confirmed
- Files to modify: HIGH - Exact line numbers identified
- Pitfalls: HIGH - Based on prior research and code analysis
- Placeholder decision: MEDIUM - Depends on external usage patterns

**Research date:** 2026-01-20
**Valid until:** Indefinite (one-time removal task)

---

## Implementation Checklist for Planner

Files to DELETE:
- [ ] `src/main/java/world/bentobox/islandselector/commands/NeighborsCommand.java`
- [ ] `src/main/java/world/bentobox/islandselector/gui/NeighborhoodGUI.java`

Files to MODIFY (code):
- [ ] `IslandSelectorCommand.java` - Remove line 37
- [ ] `MainGridGUI.java` - Remove constant, button, getter
- [ ] `SharedGridGUIListener.java` - Remove import, remove click handler

Files to MODIFY (resources):
- [ ] `addon.yml` - Remove permission
- [ ] `en-US.yml` - Remove locale keys
- [ ] `config.yml` - Remove gui.buttons.neighborhood

Files to OPTIONALLY MODIFY:
- [ ] `PlaceholderAPIIntegration.java` - Decision: keep or remove neighbors_online

Documentation to UPDATE:
- [ ] `CLAUDE.md` - Remove NeighborhoodGUI from project structure
- [ ] `DEVELOPMENT_NOTES.md` - Remove NeighborhoodGUI reference

Verification:
- [ ] `mvn clean compile` succeeds
- [ ] `/islandselector neighbors` returns unknown command
- [ ] Main GUI has no neighborhood button (slot 49 is filler)
- [ ] grep finds no remaining references
