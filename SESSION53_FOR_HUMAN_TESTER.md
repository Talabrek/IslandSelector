# Session 53 - Testing Guide for Human Tester

## Build Information

**Build Status:** BUILD SUCCESS
**JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
**JAR Size:** 216K
**Source Files:** 54 Java files (+1 from Session 52)

## New Feature Implemented

### `/islandselector switch [slot]` Command

This session implements the **SwitchCommand** - a convenience command that allows players to switch directly to a specific slot number without opening the GUI.

**Command:** `/islandselector switch <slot>` or `/is switch <slot>`

## Test Cases

### Test 1: Basic Switch Command
**Steps:**
1. Have a player with an island in slot 1 (active) and slot 2 (with island)
2. Run `/islandselector switch 2`
3. Verify the SlotSwitchConfirmationGUI opens
4. Confirm the switch
5. Verify player is now on slot 2

**Expected:** Confirmation GUI opens, switch completes successfully

### Test 2: Switch to Already Active Slot
**Steps:**
1. Player has slot 1 as active
2. Run `/islandselector switch 1`
3. Verify message: "Slot 1 is already your active slot."

**Expected:** Command rejects with informative message, no GUI opens

### Test 3: Switch to Empty Slot
**Steps:**
1. Player has slot 1 active, slot 2 is empty (no island)
2. Run `/islandselector switch 2`
3. Verify error: "Slot 2 is empty! Create an island in this slot first."

**Expected:** Command rejects with error message

### Test 4: Switch to Locked Slot
**Steps:**
1. Player has permission for only 2 slots
2. Run `/islandselector switch 3`
3. Verify error: "Slot 3 is locked! You need permission to access this slot."

**Expected:** Command rejects due to permission

### Test 5: Invalid Slot Number
**Steps:**
1. Run `/islandselector switch abc`
2. Verify error: "Invalid slot number. Must be a number."
3. Run `/islandselector switch 0`
4. Verify error about range
5. Run `/islandselector switch 6` (assuming max 5)
6. Verify error about range

**Expected:** Command validates input and shows appropriate errors

### Test 6: Tab Completion
**Steps:**
1. Type `/islandselector switch ` and press Tab
2. Verify only slot numbers with islands (and not currently active) are suggested

**Expected:** Tab completion shows valid switch targets

### Test 7: No Island
**Steps:**
1. Player has no island at all
2. Run `/islandselector switch 2`
3. Verify error: "You must have an island before you can switch slots!"

**Expected:** Command rejects for players without islands

## Quick Command Reference

```
# New command this session
/islandselector switch <slot>  - Switch directly to slot number
/is switch 2                   - Shorthand version

# Related existing commands
/islandselector slots          - Open GUI (click to switch)
/islandselector delete <slot>  - Delete a slot
/islandselector setname <slot> <name> - Rename slot
```

## Permission

The switch command uses the same permission as slots: `islandselector.slots`

## Testing Time

**Estimated:** 10-15 minutes
**Players Required:** 1 player with multiple slots

## Notes

- The switch command opens the same confirmation GUI as clicking in the Slot Selection GUI
- This is a convenience feature for players who prefer commands over GUIs
- All validation (cooldowns, permissions, slot availability) is checked before opening confirmation
