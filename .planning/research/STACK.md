# Technology Stack: BentoBox Config/Command Patterns

**Project:** IslandSelector - Config Toggle, Command Alias, Feature Removal
**Researched:** 2026-01-20
**Confidence:** HIGH (verified against existing codebase and official BentoBox documentation)

## Executive Summary

This research covers BentoBox-specific patterns for:
1. Adding config toggles to Settings.java
2. Adding command aliases via CompositeCommand constructor
3. Cleanly removing commands and GUI elements

All patterns are verified against the existing IslandSelector codebase which already implements these APIs correctly.

---

## 1. Config Toggle Pattern (slots-enabled)

### Pattern: @ConfigEntry Boolean Field

The existing `Settings.java` already uses this pattern extensively. Add a new boolean field with:

```java
@ConfigComment("Enable the island slots feature (requires FAWE)")
@ConfigComment("When disabled, the slots command and GUI button are hidden")
@ConfigEntry(path = "slots.enabled")
private boolean slotsEnabled = true;

public boolean isSlotsEnabled() {
    return slotsEnabled;
}

public void setSlotsEnabled(boolean slotsEnabled) {
    this.slotsEnabled = slotsEnabled;
}
```

**Why this location:** The slots settings already exist under `slots.*` path (see lines 42-60 in Settings.java). Adding `slots.enabled` maintains logical grouping.

**Confidence:** HIGH - This exact pattern is used 20+ times in the existing Settings.java (e.g., `backupsEnabled`, `vaultEnabled`, `debugEnabled`).

### Config Reload Behavior

The addon already handles config reload in `IslandSelector.onReload()`:
```java
settings = new Config<>(this, Settings.class).loadConfigObject();
```

No additional work needed for reload support.

---

## 2. Command Alias Pattern (/map alias)

### Pattern: CompositeCommand Constructor Aliases

The existing `IslandSelectorCommand.java` already demonstrates this pattern:

```java
public IslandSelectorCommand(IslandSelector addon) {
    super(addon, "islandselector", "is", "isgrid");
}
```

To add `/map` alias, simply add it to the constructor:

```java
public IslandSelectorCommand(IslandSelector addon) {
    super(addon, "islandselector", "is", "isgrid", "map");
}
```

**Why this works:** BentoBox's `CompositeCommand` accepts `String... aliases` as the final vararg parameter. All aliases are registered as top-level commands that invoke the same handler.

**Confidence:** HIGH - Verified via [BentoBox CompositeCommand source](https://github.com/BentoBoxWorld/BentoBox/blob/master/src/main/java/world/bentobox/bentobox/api/commands/CompositeCommand.java).

### Alternative: Subcommand Aliases

For subcommands, the pattern is identical:
```java
// Existing in NeighborsCommand.java:
super(parent, "neighbors", "neighbourhood", "neighborhood");

// Existing in SlotsCommand.java:
super(parent, "slots", "slot");
```

---

## 3. Feature Removal Pattern (neighbors command/GUI)

### Step A: Remove Command Registration

In `IslandSelectorCommand.setup()`, remove or comment out the line:

```java
// REMOVE THIS LINE:
new NeighborsCommand(this);
```

**Why delete vs hide:** BentoBox's `setHidden(boolean)` only hides from help/tab-complete but the command still executes. For true removal, don't register the command at all.

### Step B: Remove GUI Button

In `MainGridGUI.java`, the neighborhood button is created around line 717-720:

```java
// REMOVE THIS:
ItemStack neighborhood = createButton(Material.FILLED_MAP, "&bNeighborhood",
    "&7View your neighbors");
inventory.setItem(BOT_NEIGHBORHOOD_SLOT, neighborhood);
```

Also remove the slot constant and getter method (`BOT_NEIGHBORHOOD_SLOT`, `getNeighborhoodSlot()`).

### Step C: Remove Click Handler

In `SharedGridGUIListener.java`, remove the handler around line 79-82:

```java
// REMOVE THIS:
if (slot == gui.getNeighborhoodSlot()) {
    player.closeInventory();
    new NeighborhoodGUI(gui.getAddon(), player).open();
    return;
}
```

### Step D: Delete or Keep Files

**Option 1 (Recommended):** Keep `NeighborhoodGUI.java` and `NeighborsCommand.java` files but remove all references. This allows easy restoration if the feature is wanted later.

**Option 2:** Delete the files entirely for cleaner codebase.

### Step E: Remove Permission (Optional)

In `addon.yml`, the permission `islandselector.neighbors` can be removed:

```yaml
# REMOVE THIS:
islandselector.neighbors:
  description: Can use neighborhood view
  default: true
```

**Confidence:** HIGH - The removal pattern follows standard Java/BentoBox practices. No special API needed.

---

## 4. Conditional Feature Display (slots-enabled check)

### Pattern: Check Config at Runtime

The existing codebase already demonstrates this pattern for FAWE availability:

**In SlotsCommand.java:**
```java
@Override
public boolean execute(User user, String label, List<String> args) {
    IslandSelector addon = (IslandSelector) getAddon();
    if (!addon.isSchematicOperationsAvailable()) {
        user.sendMessage("&cThis feature requires FastAsyncWorldEdit...");
        return false;
    }
    // ...
}
```

**For slots-enabled toggle, use the same pattern:**
```java
@Override
public boolean execute(User user, String label, List<String> args) {
    IslandSelector addon = (IslandSelector) getAddon();
    if (!addon.getSettings().isSlotsEnabled()) {
        user.sendMessage("&cSlots feature is disabled in config.");
        return false;
    }
    // existing FAWE check...
}
```

### Hiding Command from Tab-Complete

To also hide the command when disabled, use `setHidden()` in setup() with a config check:

```java
@Override
public void setup() {
    setPermission("islandselector.slots");
    setOnlyPlayer(true);
    setDescription("commands.islandselector.slots.description");

    // Hide from tab-complete when disabled
    IslandSelector addon = (IslandSelector) getAddon();
    if (!addon.getSettings().isSlotsEnabled()) {
        setHidden(true);
    }
}
```

**Note:** `setHidden()` is called once at registration. If you need runtime toggle without restart, only check in `execute()`.

### Hiding GUI Button

In `MainGridGUI.java`, wrap the button creation with a config check:

```java
// Slot Selection - only show if enabled AND FAWE is available
if (addon.getSettings().isSlotsEnabled() && addon.isSchematicOperationsAvailable()) {
    ItemStack slots = createButton(Material.CHEST, "&eSlot Selection",
        "&7Manage your island slots");
    inventory.setItem(BOT_SLOTS_SLOT, slots);
}
```

**Confidence:** HIGH - Exact same pattern used for FAWE check already in the codebase.

---

## Files to Modify

| File | Changes |
|------|---------|
| `Settings.java` | Add `slotsEnabled` field with annotations, getter, setter |
| `IslandSelectorCommand.java` | Add "map" to constructor aliases, remove `new NeighborsCommand(this)` |
| `SlotsCommand.java` | Add config check in `execute()`, optionally `setHidden()` in `setup()` |
| `MainGridGUI.java` | Remove neighborhood button, add config check for slots button |
| `SharedGridGUIListener.java` | Remove neighborhood click handler |
| `addon.yml` | Remove `islandselector.neighbors` permission |

## Files to Delete (Optional)

| File | Rationale |
|------|-----------|
| `NeighborsCommand.java` | Feature removed |
| `NeighborhoodGUI.java` | Feature removed |

---

## Sources

- [BentoBox Config API Documentation](https://docs.bentobox.world/en/latest/BentoBox/Config-API/) - ConfigObject, ConfigEntry patterns
- [BentoBox CompositeCommand Source](https://github.com/BentoBoxWorld/BentoBox/blob/master/src/main/java/world/bentobox/bentobox/api/commands/CompositeCommand.java) - Constructor signatures with aliases
- [BentoBox Commands Reference](https://docs.bentobox.world/en/latest/BentoBox/Commands/) - Command structure overview
- Existing `Settings.java` (lines 1-848) - 20+ examples of ConfigEntry boolean toggles
- Existing `IslandSelectorCommand.java` (line 20) - Alias pattern: `super(addon, "islandselector", "is", "isgrid")`
- Existing `SlotsCommand.java` (lines 31-35) - Runtime feature availability check pattern
- Existing `MainGridGUI.java` (lines 717-720, 722-726) - Conditional GUI button pattern
