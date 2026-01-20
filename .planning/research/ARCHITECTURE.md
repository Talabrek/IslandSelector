# Architecture Patterns for IslandSelector Config/Command Changes

**Domain:** BentoBox addon configuration, command registration, feature removal
**Researched:** 2026-01-20
**Confidence:** HIGH (based on direct codebase analysis)

## Overview

This document describes how to integrate three changes into the existing IslandSelector addon architecture:

1. **Config Toggle:** Add `slots-enabled` to Settings.java with checks in GUI and command
2. **Command Alias:** Add `/map` alias for the main command
3. **Feature Removal:** Remove NeighborhoodGUI and related references

## Current Architecture Summary

### Configuration Pattern (Settings.java)

The addon uses BentoBox's ConfigObject pattern:
- `Settings.java` implements `ConfigObject` interface
- Configuration entries use `@ConfigEntry(path = "...")` annotations
- Config comments use `@ConfigComment("...")` annotations
- File is stored at `addons/IslandSelector/config.yml` via `@StoreAt` annotation

**Existing toggle example (line 166-167 in Settings.java):**
```java
@ConfigComment("Enable Level addon integration")
@ConfigEntry(path = "integration.level-addon.enabled")
private boolean levelAddonEnabled = true;
```

### Command Registration Pattern

Commands are registered in `IslandSelectorCommand.java`:
- Main command: `new IslandSelectorCommand(addon)` in `IslandSelector.registerCommands()`
- Aliases defined in constructor: `super(addon, "islandselector", "is", "isgrid")`
- Subcommands registered in `setup()` method by instantiation

**Current aliases (line 20):**
```java
super(addon, "islandselector", "is", "isgrid");
```

### Feature Integration Points

The NeighborhoodGUI feature has the following integration points:

| File | Integration | Lines |
|------|-------------|-------|
| `IslandSelectorCommand.java` | Subcommand registration | Line 37 |
| `NeighborsCommand.java` | Command implementation | Entire file |
| `NeighborhoodGUI.java` | GUI implementation | Entire file |
| `SharedGridGUIListener.java` | Button click handler | Lines 79-82 |
| `MainGridGUI.java` | Button creation | Lines 717-720 |
| `addon.yml` | Permission definition | Lines 43-45 |
| `en-US.yml` | Locale strings | Lines 64-65, 100-106, 113 |
| `PlaceholderAPIIntegration.java` | Placeholder (optional) | Lines 30, 94-95, 187-188 |

---

## Change 1: Add `slots-enabled` Config Toggle

### Files to Modify

| File | Change | Purpose |
|------|--------|---------|
| `Settings.java` | Add field, getter, setter | Config storage |
| `SlotsCommand.java` | Add config check | Block command when disabled |
| `SharedGridGUIListener.java` | Add config check | Hide/disable button |
| `MainGridGUI.java` | Conditionally show button | Hide button when disabled |

### Implementation Pattern

**1. Settings.java - Add after line 56 (under slots section):**
```java
@ConfigComment("Enable slot management feature")
@ConfigComment("When false, /islandselector slots command and GUI button are disabled")
@ConfigEntry(path = "slots.enabled")
private boolean slotsEnabled = true;

// Add getter/setter in the slots section (around line 310)
public boolean isSlotsEnabled() {
    return slotsEnabled;
}

public void setSlotsEnabled(boolean slotsEnabled) {
    this.slotsEnabled = slotsEnabled;
}
```

**2. SlotsCommand.java - Add check at start of execute() (before line 32):**
```java
@Override
public boolean execute(User user, String label, List<String> args) {
    IslandSelector addon = (IslandSelector) getAddon();

    // Check if slots feature is enabled
    if (!addon.getSettings().isSlotsEnabled()) {
        user.sendMessage("&cSlot management is disabled on this server.");
        return false;
    }

    // Existing FAWE check...
```

**3. MainGridGUI.java - Modify slot button creation (lines 722-728):**
```java
// Slot Selection - only show if enabled and FAWE is available
if (addon.getSettings().isSlotsEnabled() && addon.isSchematicOperationsAvailable()) {
    ItemStack slots = createButton(Material.CHEST, "&6Island Slots",
        "&7Manage your slots");
    inventory.setItem(BOT_SLOTS_SLOT, slots);
}
// If disabled or FAWE not available, slot 50 will be filled with filler later
```

**4. SharedGridGUIListener.java - Add config check (lines 84-89):**
```java
if (slot == gui.getSlotsSlot()) {
    if (gui.getAddon().getSettings().isSlotsEnabled() &&
        gui.getAddon().isSchematicOperationsAvailable()) {
        player.closeInventory();
        new SlotSelectionGUI(gui.getAddon(), player).open();
    }
    return;
}
```

### Config Output

Results in `config.yml`:
```yaml
slots:
  # Enable slot management feature
  # When false, /islandselector slots command and GUI button are disabled
  enabled: true
  # Default number of slots
  default-slots: 2
```

---

## Change 2: Add `/map` Command Alias

### Files to Modify

| File | Change | Purpose |
|------|--------|---------|
| `IslandSelectorCommand.java` | Add alias to constructor | Register `/map` |

### Implementation

**IslandSelectorCommand.java - Modify line 20:**
```java
// Before:
super(addon, "islandselector", "is", "isgrid");

// After:
super(addon, "islandselector", "is", "isgrid", "map");
```

### Notes

- BentoBox handles alias registration automatically
- No addon.yml changes needed (aliases are not permissions)
- The first argument is the primary label, others are aliases
- All aliases share the same permission (`islandselector.use`)

---

## Change 3: Remove NeighborhoodGUI Feature

### Files to Delete

| File | Reason |
|------|--------|
| `src/main/java/.../gui/NeighborhoodGUI.java` | Feature implementation |
| `src/main/java/.../commands/NeighborsCommand.java` | Command implementation |

### Files to Modify

| File | Change | Lines |
|------|--------|-------|
| `IslandSelectorCommand.java` | Remove subcommand registration | Line 37 |
| `SharedGridGUIListener.java` | Remove button click handler | Lines 79-82 |
| `MainGridGUI.java` | Remove button creation | Lines 717-720 |
| `addon.yml` | Remove permission definition | Lines 43-45 |
| `en-US.yml` | Remove locale strings | Lines 64-65, 100-106 |
| `PlaceholderAPIIntegration.java` | Remove placeholder (optional) | Lines 94-95 |

### Implementation Details

**1. IslandSelectorCommand.java - Remove line 37:**
```java
// Delete this line:
new NeighborsCommand(this);
```

**2. SharedGridGUIListener.java - Remove lines 79-82:**
```java
// Delete these lines:
if (slot == gui.getNeighborhoodSlot()) {
    player.closeInventory();
    new NeighborhoodGUI(gui.getAddon(), player).open();
    return;
}
```

Also remove the import at top of file (if present).

**3. MainGridGUI.java - Remove lines 717-720:**
```java
// Delete these lines:
// Neighborhood View
ItemStack neighborhood = createButton(Material.FILLED_MAP, "&bNeighborhood",
    "&7View your neighbors");
inventory.setItem(BOT_NEIGHBORHOOD_SLOT, neighborhood);
```

Also remove or repurpose the constant:
```java
// Line 67 - can leave or remove (unused constant is harmless)
private static final int BOT_NEIGHBORHOOD_SLOT = 49;
```

And remove the getter method:
```java
// Lines 980-982 - remove:
public int getNeighborhoodSlot() {
    return BOT_NEIGHBORHOOD_SLOT;
}
```

**4. addon.yml - Remove lines 43-45:**
```yaml
# Delete these lines:
islandselector.neighbors:
  description: Can use neighborhood view
  default: true
```

**5. en-US.yml - Remove relevant sections:**
```yaml
# Delete lines 64-65:
neighborhood: "&aNeighborhood View"
neighborhood-desc: "&7View your neighbors"

# Delete lines 100-106:
# Neighborhood GUI
neighborhood:
  title: "Your Neighborhood"
  your-island: "&a&l YOUR ISLAND"
  back: "&aBack to Grid"
  slots: "&eSlot Selection"
  close: "&cClose"
```

**6. PlaceholderAPIIntegration.java - Remove neighbors placeholder (optional):**
```java
// Lines 94-95 - Delete:
if (identifier.equals("neighbors_online")) {
    return String.valueOf(getOnlineNeighborsCount(player));
}

// Lines 187-200 - Delete the getOnlineNeighborsCount method
```

---

## Suggested Build Order

### Phase 1: Config Toggle (Independent)

1. Add field, getter, setter to `Settings.java`
2. Add check to `SlotsCommand.java`
3. Modify button logic in `MainGridGUI.java`
4. Add check to `SharedGridGUIListener.java`
5. **Test:** Reload config, verify toggle works

### Phase 2: Command Alias (Independent)

1. Add `"map"` to alias list in `IslandSelectorCommand.java`
2. **Test:** Verify `/map` opens grid GUI

### Phase 3: Feature Removal (Dependent on compile success)

1. Remove subcommand registration from `IslandSelectorCommand.java`
2. Remove button handler from `SharedGridGUIListener.java`
3. Remove button creation from `MainGridGUI.java`
4. Remove getter method from `MainGridGUI.java`
5. Delete `NeighborsCommand.java`
6. Delete `NeighborhoodGUI.java`
7. Remove permission from `addon.yml`
8. Remove locale strings from `en-US.yml`
9. (Optional) Remove placeholder from `PlaceholderAPIIntegration.java`
10. **Test:** Verify compile, verify grid GUI works without neighborhood button

### Build Order Rationale

- **Phase 1 first:** Self-contained, lowest risk, quick to verify
- **Phase 2 second:** Single-line change, trivial verification
- **Phase 3 last:** Most files affected, compile must succeed, requires careful cleanup

---

## Dependencies Between Changes

```
Config Toggle (Phase 1)      Command Alias (Phase 2)
       |                            |
       v                            v
    (independent)              (independent)

Feature Removal (Phase 3)
       |
       v
    (must compile after)
```

- **No dependencies between Phase 1 and Phase 2** - can be done in parallel
- **Phase 3 should be done last** - has most impact, benefits from stable base

---

## Testing Checklist

### Config Toggle
- [ ] `slots.enabled: true` allows `/islandselector slots` command
- [ ] `slots.enabled: false` blocks command with message
- [ ] `slots.enabled: false` hides GUI button
- [ ] Config reload picks up changes

### Command Alias
- [ ] `/map` opens grid GUI
- [ ] `/map` tab completion works
- [ ] `/islandselector` still works
- [ ] `/is` and `/isgrid` aliases still work

### Feature Removal
- [ ] Project compiles without errors
- [ ] Grid GUI opens without neighborhood button
- [ ] No errors in console on startup
- [ ] `/islandselector neighbors` returns unknown command
- [ ] Clicking slot 49 (former neighborhood) does nothing or has filler

---

## Risk Assessment

| Change | Risk | Mitigation |
|--------|------|------------|
| Config Toggle | LOW | Self-contained, well-established pattern |
| Command Alias | LOW | Single line, BentoBox handles registration |
| Feature Removal | MEDIUM | Multiple files, must remove all references to compile |

---

## Sources

- Direct codebase analysis (HIGH confidence)
- `IslandSelector.java` - Main addon class, line 134: `registerCommands()`
- `Settings.java` - Configuration class implementing ConfigObject
- `IslandSelectorCommand.java` - Command registration patterns
- `SharedGridGUIListener.java` - GUI event handling patterns
- `MainGridGUI.java` - GUI layout and button constants
- `addon.yml` - Permission definitions
- `en-US.yml` - Locale string organization
