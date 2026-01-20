# Phase 5: Config Toggle - Research

**Researched:** 2026-01-20
**Domain:** BentoBox addon configuration toggles
**Confidence:** HIGH

## Summary

This phase adds a configuration option `slots.enabled` that allows server administrators to enable or disable the multi-slot island system. The implementation follows existing BentoBox patterns already used extensively in the IslandSelector addon.

The changes are minimal and well-understood: add a config field to Settings.java, add a check at the start of SlotsCommand.execute(), and wrap the slots button creation in MainGridGUI with the same check. The existing codebase already has 20+ examples of this exact pattern.

**Primary recommendation:** Use the existing `@ConfigEntry` pattern with `slots.enabled` path to maintain consistency with other slot settings. Check the toggle at runtime (not registration time) to support config reload without server restart.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| BentoBox Config API | N/A | Config toggle with `@ConfigEntry` annotation | Built-in, already used extensively in Settings.java |

### Supporting

No additional libraries needed. All functionality uses existing BentoBox APIs.

## Architecture Patterns

### Recommended Implementation Locations

```
src/main/java/world/bentobox/islandselector/
  Settings.java              # Add slotsEnabled field (lines 41-60 area, slots section)
  commands/
    SlotsCommand.java        # Add config check at start of execute() (line 28)
  gui/
    MainGridGUI.java         # Add config check to button rendering (line 722-728)
    SharedGridGUIListener.java  # Add config check for click handler (line 84-89)
```

### Pattern 1: Config Toggle Field

**What:** Boolean field with `@ConfigEntry` annotation
**When to use:** Any feature that admins should be able to enable/disable
**Example:**
```java
// Source: Existing pattern in Settings.java lines 76-78
@ConfigComment("Enable the multi-slot island system")
@ConfigComment("When disabled, /islandselector slots command and GUI button are hidden")
@ConfigEntry(path = "slots.enabled")
private boolean slotsEnabled = true;

public boolean isSlotsEnabled() {
    return slotsEnabled;
}

public void setSlotsEnabled(boolean slotsEnabled) {
    this.slotsEnabled = slotsEnabled;
}
```

### Pattern 2: Runtime Feature Check in Command

**What:** Check config at execution time, not registration time
**When to use:** When config reload should take effect immediately
**Example:**
```java
// Source: Existing FAWE check pattern in SlotsCommand.java lines 31-35
@Override
public boolean execute(User user, String label, List<String> args) {
    IslandSelector addon = (IslandSelector) getAddon();

    // Check if slot system is enabled
    if (!addon.getSettings().isSlotsEnabled()) {
        user.sendMessage("&cThe slot system is disabled on this server.");
        return false;
    }

    // Existing FAWE check follows...
}
```

### Pattern 3: Conditional GUI Button

**What:** Only render button when feature is enabled
**When to use:** When a GUI element corresponds to a toggleable feature
**Example:**
```java
// Source: Existing FAWE check pattern in MainGridGUI.java lines 722-728
// Slot Selection - only show if enabled AND FAWE is available
if (addon.getSettings().isSlotsEnabled() && addon.isSchematicOperationsAvailable()) {
    ItemStack slots = createButton(Material.CHEST, "&6Island Slots",
        "&7Manage your slots");
    inventory.setItem(BOT_SLOTS_SLOT, slots);
}
```

### Anti-Patterns to Avoid

- **Checking config only at command registration:** The `setup()` method runs once. If you use `setHidden()` there, config reload won't update it. Check in `execute()` instead.
- **Replacing FAWE check instead of combining:** The slots feature requires BOTH config enabled AND FAWE available. Use `&&` to combine checks.
- **Forgetting the click handler:** MainGridGUI can show a slot where the button was removed by filler. The click handler in SharedGridGUIListener must also check before opening SlotSelectionGUI.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Config file management | Custom YAML parsing | BentoBox `@ConfigEntry` | Handles serialization, comments, defaults automatically |
| Message formatting | String concatenation | `user.sendMessage()` with color codes | Consistent with existing messages |

**Key insight:** BentoBox's config system automatically handles YAML serialization, default values, and config comments. Never manually parse or write config files.

## Common Pitfalls

### Pitfall 1: Incomplete Toggle - GUI Button Still Clickable

**What goes wrong:** Adding the config check to MainGridGUI button rendering but forgetting SharedGridGUIListener. The button slot gets filled with filler but clicking slot 50 still works.
**Why it happens:** The button slot constant `BOT_SLOTS_SLOT = 50` doesn't change. SharedGridGUIListener checks if `slot == gui.getSlotsSlot()` regardless of what item is there.
**How to avoid:** Add config check in SharedGridGUIListener.java line 84-89:
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
**Warning signs:** Click on filler at slot 50 opens SlotSelectionGUI when slots are disabled.

### Pitfall 2: Config Reload Doesn't Apply to Open GUIs

**What goes wrong:** Admin reloads config, but player with GUI open sees old state.
**Why it happens:** GUIs cache items at render time. The button was rendered before reload.
**How to avoid:** This is acceptable behavior - document that players must close and reopen GUI. The existing addon already works this way for all settings.
**Warning signs:** Not really a bug, just expected behavior to document.

### Pitfall 3: Silent Failure When Feature Disabled

**What goes wrong:** Player runs `/islandselector slots`, nothing happens, no message.
**Why it happens:** Returning `false` from execute() without sending a message.
**How to avoid:** Always send a clear message before returning false:
```java
if (!addon.getSettings().isSlotsEnabled()) {
    user.sendMessage("&cThe slot system is disabled on this server.");
    return false;
}
```
**Warning signs:** User gets no feedback when command is blocked.

### Pitfall 4: Wrong Default Value

**What goes wrong:** Setting `slotsEnabled = false` as default causes existing servers to lose slot functionality on upgrade.
**Why it happens:** Not considering backward compatibility.
**How to avoid:** Default must be `true` to match current behavior. Servers that don't want slots can disable it.
**Warning signs:** Existing servers report "slots stopped working after update."

## Code Examples

### Example 1: Complete Settings.java Addition

```java
// Location: After line 60 (end of current slots section)
// Source: Pattern from existing toggles in Settings.java

@ConfigComment("")
@ConfigComment("Enable the multi-slot island system")
@ConfigComment("When disabled:")
@ConfigComment("  - /islandselector slots command shows disabled message")
@ConfigComment("  - Slots button is hidden from the main grid GUI")
@ConfigComment("  - Existing slot data is preserved but inaccessible")
@ConfigEntry(path = "slots.enabled")
private boolean slotsEnabled = true;

// Getter (add with other slot getters around line 300)
public boolean isSlotsEnabled() {
    return slotsEnabled;
}

// Setter (add with other slot setters)
public void setSlotsEnabled(boolean slotsEnabled) {
    this.slotsEnabled = slotsEnabled;
}
```

### Example 2: Complete SlotsCommand.java Change

```java
// Location: Start of execute() method, before line 31
// Source: Pattern from existing FAWE check

@Override
public boolean execute(User user, String label, List<String> args) {
    IslandSelector addon = (IslandSelector) getAddon();

    // Check if slot system is enabled in config
    if (!addon.getSettings().isSlotsEnabled()) {
        user.sendMessage("&cThe slot system is disabled on this server.");
        return false;
    }

    // Check if FAWE is available (existing check)
    if (!addon.isSchematicOperationsAvailable()) {
        user.sendMessage("&cThis feature requires FastAsyncWorldEdit (FAWE) to be installed.");
        return false;
    }

    // Open the slot selection GUI
    new SlotSelectionGUI(addon, user.getPlayer()).open();
    return true;
}
```

### Example 3: Complete MainGridGUI.java Change

```java
// Location: Lines 722-728, modify existing code
// Source: Extend existing FAWE check pattern

// Slot Selection - only show if enabled AND FAWE is available
if (addon.getSettings().isSlotsEnabled() && addon.isSchematicOperationsAvailable()) {
    ItemStack slots = createButton(Material.CHEST, "&6Island Slots",
        "&7Manage your slots");
    inventory.setItem(BOT_SLOTS_SLOT, slots);
}
// If disabled or FAWE not available, slot 50 will be filled with filler later
```

### Example 4: Complete SharedGridGUIListener.java Change

```java
// Location: Lines 84-89, modify existing code
// Source: Extend existing FAWE check pattern

if (slot == gui.getSlotsSlot()) {
    // Only open if slots are enabled AND FAWE is available
    if (gui.getAddon().getSettings().isSlotsEnabled() &&
        gui.getAddon().isSchematicOperationsAvailable()) {
        player.closeInventory();
        new SlotSelectionGUI(gui.getAddon(), player).open();
    }
    return;
}
```

## Exact Line Numbers

For precise implementation, here are the exact modification points:

| File | Line(s) | Change Description |
|------|---------|-------------------|
| Settings.java | After line 60 | Add `slotsEnabled` field with annotations |
| Settings.java | Around line 310 | Add `isSlotsEnabled()` getter |
| Settings.java | Around line 315 | Add `setSlotsEnabled()` setter |
| SlotsCommand.java | Line 28-30 | Insert config check before FAWE check |
| MainGridGUI.java | Line 722 | Change `if (addon.isSchematicOperationsAvailable())` to `if (addon.getSettings().isSlotsEnabled() && addon.isSchematicOperationsAvailable())` |
| SharedGridGUIListener.java | Line 85 | Change `if (gui.getAddon().isSchematicOperationsAvailable())` to `if (gui.getAddon().getSettings().isSlotsEnabled() && gui.getAddon().isSchematicOperationsAvailable())` |

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| N/A - new feature | `@ConfigEntry` with runtime checks | N/A | Consistent with existing patterns |

## Open Questions

None - this is a straightforward implementation using well-established patterns already in the codebase.

## Sources

### Primary (HIGH confidence)
- `Settings.java` (lines 1-848) - 20+ examples of ConfigEntry boolean toggles with identical pattern
- `SlotsCommand.java` (lines 28-35) - Existing FAWE check pattern to extend
- `MainGridGUI.java` (lines 722-728) - Existing conditional button rendering pattern
- `SharedGridGUIListener.java` (lines 84-89) - Existing FAWE check in click handler

### Secondary (MEDIUM confidence)
- `.planning/research/STACK.md` - Prior project research on config patterns
- `.planning/research/FEATURES.md` - Prior project research on toggle implementation
- `.planning/research/ARCHITECTURE.md` - Prior project research on file locations
- `.planning/research/PITFALLS.md` - Prior project research on common mistakes

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Using existing BentoBox APIs already in codebase
- Architecture: HIGH - Following exact patterns from existing Settings.java
- Pitfalls: HIGH - Based on direct code analysis of existing checks

**Research date:** 2026-01-20
**Valid until:** Indefinite - BentoBox config API is stable
