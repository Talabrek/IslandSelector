# Feature Landscape

**Domain:** BentoBox Addon Configuration Changes
**Researched:** 2026-01-20
**Confidence:** HIGH (based on existing codebase patterns and BentoBox documentation)

## Context

This research covers implementing configuration toggles and command changes for the IslandSelector BentoBox addon:
1. Config option to disable the slot system (hide GUI button, block command)
2. `/map` command alias for `/islandselector`
3. Remove neighbors GUI and command entirely

---

## Table Stakes

Features that MUST be implemented correctly for these changes to work properly.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Config toggle for slot system | Server admins expect feature toggles to fully disable features they don't want | Low | Standard BentoBox pattern using `@ConfigEntry` |
| GUI button hidden when slots disabled | Showing a button that doesn't work is confusing UX | Low | Conditional check in `MainGridGUI.populateControlButtons()` |
| Command blocked when slots disabled | Running a disabled command must fail gracefully with clear message | Low | Check in `SlotsCommand.execute()` |
| Helpful error message when blocked | Players need to understand why a feature isn't available | Low | Message like "Slot system is disabled on this server" |
| `/map` command alias registration | Aliases are standard BentoBox feature via constructor | Low | Add "map" to `IslandSelectorCommand` constructor |
| Clean removal of neighbors code | Dead code should not remain in codebase | Medium | Remove files + all references |

## Table Stakes Details

### 1. Config Toggle Implementation

**Pattern from existing codebase (HIGH confidence):**

The addon already uses this pattern extensively. See `Settings.java`:
- `multiDimensionEnabled` (line 217) - feature toggle
- `backupsEnabled` (line 78) - feature toggle
- `vaultEnabled` (line 159) - integration toggle

**Required implementation:**
```java
@ConfigComment("Enable the multi-slot island system")
@ConfigComment("When disabled, players can only have one island (no slot switching)")
@ConfigEntry(path = "slots.enabled")
private boolean slotsEnabled = true;
```

With getter:
```java
public boolean isSlotsEnabled() {
    return slotsEnabled;
}
```

### 2. GUI Button Hiding

**Pattern from existing codebase (HIGH confidence):**

`MainGridGUI.java` line 722-728 already does conditional button display:
```java
// Slot Selection - only show if FAWE is available
if (addon.isSchematicOperationsAvailable()) {
    ItemStack slots = createButton(Material.CHEST, "&6Island Slots",
        "&7Manage your slots");
    inventory.setItem(BOT_SLOTS_SLOT, slots);
}
// If FAWE not available, slot 50 will be filled with filler later
```

**Required change:** Add `&& addon.getSettings().isSlotsEnabled()` to the condition.

### 3. Command Blocking

**Pattern from existing codebase (HIGH confidence):**

`SlotsCommand.java` lines 31-35 already has feature checking:
```java
// Check if FAWE is available
if (!addon.isSchematicOperationsAvailable()) {
    user.sendMessage("&cThis feature requires FastAsyncWorldEdit (FAWE) to be installed.");
    return false;
}
```

**Required addition:** Add similar check at start of `execute()`:
```java
// Check if slot system is enabled
if (!addon.getSettings().isSlotsEnabled()) {
    user.sendMessage("&cThe slot system is disabled on this server.");
    return false;
}
```

### 4. Command Alias

**Pattern from BentoBox (HIGH confidence):**

From `IslandSelectorCommand.java` line 19-21:
```java
public IslandSelectorCommand(IslandSelector addon) {
    super(addon, "islandselector", "is", "isgrid");
}
```

BentoBox `CompositeCommand` constructor accepts varargs for aliases. Simply add "map":
```java
super(addon, "islandselector", "is", "isgrid", "map");
```

**Source:** [BentoBox CompositeCommand.java](https://github.com/BentoBoxWorld/BentoBox/blob/master/src/main/java/world/bentobox/bentobox/api/commands/CompositeCommand.java)

### 5. Neighbors Removal

**Files to delete:**
- `src/main/java/world/bentobox/islandselector/gui/NeighborhoodGUI.java`
- `src/main/java/world/bentobox/islandselector/commands/NeighborsCommand.java`

**References to remove:**
- `IslandSelectorCommand.java` line 37: `new NeighborsCommand(this);`
- `MainGridGUI.java`: `BOT_NEIGHBORHOOD_SLOT` constant and related code
- `SharedGridGUIListener.java`: neighborhood button click handling
- `addon.yml`: `islandselector.neighbors` permission

---

## Nice-to-Haves

Features that would improve the implementation but aren't strictly required.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Tab completion respects disabled commands | Cleaner UX - don't show commands that won't work | Low | Override `tabComplete()` to filter |
| Config comment explaining implications | Helps admins understand what disabling does | Trivial | Add `@ConfigComment` lines |
| Reload applies toggle immediately | Avoid server restart for config changes | Low | Already have reload command that reloads settings |
| Log message when feature is disabled | Helps debugging/confirmation | Trivial | Add log in `onEnable()` |
| Permission check before config check | Allow admins to override via permission | Low | Optional bypass permission |

### Nice-to-Have Details

**Tab completion filtering:**
```java
@Override
public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
    // Filter out disabled commands from suggestions
    if (args.size() == 1) {
        List<String> suggestions = new ArrayList<>();
        for (CompositeCommand sub : getSubCommands().values()) {
            // Skip slots if disabled
            if (sub.getLabel().equals("slots") && !addon.getSettings().isSlotsEnabled()) {
                continue;
            }
            // ... rest of logic
        }
    }
}
```

**Config comments:**
```java
@ConfigComment("")
@ConfigComment("==========================================")
@ConfigComment("SLOT SYSTEM SETTINGS")
@ConfigComment("==========================================")
@ConfigComment("Enable the multi-slot island system.")
@ConfigComment("When DISABLED:")
@ConfigComment("  - Players can only have one island")
@ConfigComment("  - /islandselector slots command is blocked")
@ConfigComment("  - Slots button is hidden from grid GUI")
@ConfigComment("  - Slot switching features are unavailable")
@ConfigEntry(path = "slots.enabled")
private boolean slotsEnabled = true;
```

---

## Anti-Features

Features to explicitly NOT implement. Common mistakes in this type of change.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Silently failing commands | Confuses players - they don't know why it doesn't work | Always send a clear message explaining the feature is disabled |
| Hiding command from help but still executable | Inconsistent behavior frustrates users | Either fully disable (block execution) or fully enable |
| Breaking existing slot data when disabled | Players who had slots would lose data | Keep slot data intact; just prevent new slot operations |
| Removing neighbors without updating translations | Leaves orphaned translation keys | Also remove from `locales/` if present |
| Hardcoding the alias | Admins may want to customize | Consider making it configurable (optional nice-to-have) |
| Leaving dead imports/references | Causes compilation warnings, confusion | Clean up all imports and references |
| Removing permission from addon.yml only | Permission still referenced in code | Remove from both addon.yml AND code |

### Anti-Feature Details

**Do NOT do this - silent failure:**
```java
// BAD: Silently does nothing
if (!addon.getSettings().isSlotsEnabled()) {
    return false;  // No message!
}
```

**Do NOT do this - data destruction:**
```java
// BAD: Deleting player slot data when feature disabled
if (!addon.getSettings().isSlotsEnabled()) {
    slotManager.deleteAllSlots();  // NEVER DO THIS
}
```

**Do NOT do this - partial removal:**
```java
// BAD: Removing command registration but leaving GUI button
// setup() {
//     // new NeighborsCommand(this);  // Commented out
// }
// But MainGridGUI still has the button!
```

---

## Feature Dependencies

```
Config Toggle (slots.enabled)
    |
    +---> SlotsCommand (checks toggle before executing)
    |
    +---> MainGridGUI (checks toggle before showing button)
    |
    +---> SlotSelectionGUI (checks toggle in various places)
    |
    +---> Tab completion (filters "slots" from suggestions)

Command Alias (/map)
    |
    +---> No dependencies, additive change

Neighbors Removal
    |
    +---> NeighborsCommand.java (DELETE)
    +---> NeighborhoodGUI.java (DELETE)
    +---> IslandSelectorCommand.java (remove registration)
    +---> MainGridGUI.java (remove button)
    +---> SharedGridGUIListener.java (remove click handler)
    +---> addon.yml (remove permission)
```

---

## Affected Files Summary

### Files to Modify

| File | Change |
|------|--------|
| `Settings.java` | Add `slotsEnabled` field with getter |
| `IslandSelectorCommand.java` | Add "map" alias, remove NeighborsCommand |
| `SlotsCommand.java` | Add enabled check at start of execute() |
| `MainGridGUI.java` | Add enabled check for slots button, remove neighborhood button |
| `SharedGridGUIListener.java` | Remove neighborhood click handling |
| `addon.yml` | Remove `islandselector.neighbors` permission |

### Files to Delete

| File | Reason |
|------|--------|
| `NeighborsCommand.java` | Feature removed |
| `NeighborhoodGUI.java` | Feature removed |

---

## User Experience Considerations

### When Slots Are Disabled

**Command attempt:**
```
Player: /islandselector slots
Server: The slot system is disabled on this server.
```

**GUI experience:**
- Slot button simply doesn't appear in bottom control bar
- No indication it was ever there (clean absence)

**Help command:**
- `/islandselector help` should NOT list "slots" subcommand
- Requires filtering in help output

### When Using /map Alias

**Expected behavior:**
- `/map` works exactly like `/islandselector`
- `/map slots` works like `/islandselector slots`
- Tab completion works for both
- Help shows both aliases

---

## Implementation Checklist

- [ ] Add `slotsEnabled` config option to Settings.java
- [ ] Add enabled check to SlotsCommand.execute()
- [ ] Add enabled check to MainGridGUI button display
- [ ] Add "map" alias to IslandSelectorCommand constructor
- [ ] Remove NeighborsCommand registration from IslandSelectorCommand
- [ ] Remove neighborhood button from MainGridGUI
- [ ] Remove neighborhood click handling from SharedGridGUIListener
- [ ] Delete NeighborsCommand.java
- [ ] Delete NeighborhoodGUI.java
- [ ] Remove islandselector.neighbors permission from addon.yml
- [ ] Test: /islandselector slots blocked when disabled
- [ ] Test: Slots button hidden when disabled
- [ ] Test: /map opens grid GUI
- [ ] Test: /map slots works (when enabled)
- [ ] Test: neighbors command no longer exists
- [ ] Test: Build succeeds with no compilation errors

---

## Sources

- **Existing codebase patterns:** Settings.java, SlotsCommand.java, MainGridGUI.java (HIGH confidence)
- **BentoBox CompositeCommand:** [GitHub](https://github.com/BentoBoxWorld/BentoBox/blob/master/src/main/java/world/bentobox/bentobox/api/commands/CompositeCommand.java) (HIGH confidence)
- **BentoBox addon documentation:** [docs.bentobox.world](https://docs.bentobox.world) (MEDIUM confidence)
