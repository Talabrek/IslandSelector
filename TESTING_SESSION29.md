# Session 29 - Testing Guide: Slot Icon Command

## Build Information
- **Session:** 29
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **JAR Size:** 144K
- **Build Status:** ✅ SUCCESS (34 source files compiled)

## Feature Implemented

### Command: `/islandselector seticon <slot>`

Sets a custom icon for an island slot based on the item currently held in the player's hand.

**Similar to:** Session 28's `/islandselector setname` command (companion feature)

---

## Quick Test (3 minutes)

1. Copy JAR to `plugins/BentoBox/addons/`
2. Restart server
3. Create an island: `/island`
4. Hold a diamond pickaxe in your hand
5. Run: `/islandselector seticon 1`
6. Expected: Success message (green) "Set icon for slot 1 to diamond pickaxe"
7. Run: `/islandselector slots`
8. Expected: Slot 1 shows diamond pickaxe icon instead of grass block

---

## Comprehensive Test Plan

### Prerequisites
- Server running with BentoBox, BSkyBlock, FAWE
- Player with at least one island slot created
- Permission: `islandselector.seticon` (default: all players)

---

### Test #1: Set slot icon with held item (Test #96)

**Purpose:** Verify that holding an item and running the command sets the slot icon

**Steps:**
1. Join server and create an island if you don't have one: `/island`
2. Hold a **diamond pickaxe** in your main hand
3. Open slot selection GUI to see current icon: `/islandselector slots`
   - Note: Slot 1 should currently show as GRASS_BLOCK icon
4. Close the GUI
5. Run: `/islandselector seticon 1`
6. Observe the chat message

**Expected Results:**
- ✅ Success message appears: "&aSet icon for slot 1 to diamond pickaxe" (green)
- ✅ No errors in console

**Then verify it persisted:**
7. Run: `/islandselector slots`
8. Look at slot 1 in the GUI

**Expected Results:**
- ✅ Slot 1 now displays with diamond pickaxe icon (not grass block)
- ✅ Icon shows correctly in the slot GUI

---

### Test #2: Error when no item held (Test #97)

**Purpose:** Verify graceful error handling when player has empty hand

**Steps:**
1. Ensure your main hand is **empty** (no item)
2. Run: `/islandselector seticon 1`
3. Observe the chat message

**Expected Results:**
- ✅ Error message: "&cYou must be holding an item to set as the icon!" (red)
- ✅ No success message
- ✅ No errors in console
- ✅ Slot icon unchanged (still diamond pickaxe from Test #1)

---

### Test #3: Different icons for different slots

**Purpose:** Verify each slot can have a unique icon

**Steps:**
1. Create a second island slot (if you only have one):
   - `/islandselector slots`
   - Click empty slot 2 to create a new island
2. Hold a **golden apple** in your hand
3. Run: `/islandselector seticon 2`
4. Hold an **emerald** in your hand
5. Run: `/islandselector seticon 1`
6. Open slot GUI: `/islandselector slots`

**Expected Results:**
- ✅ Slot 1 shows emerald icon
- ✅ Slot 2 shows golden apple icon
- ✅ Each slot has its own unique icon

---

### Test #4: Invalid slot number

**Purpose:** Verify error handling for invalid slot numbers

**Steps:**
1. Hold any item
2. Run: `/islandselector seticon abc`
3. Observe message

**Expected Results:**
- ✅ Error: "&cInvalid slot number. Must be a number."

**Then test out of range:**
4. Run: `/islandselector seticon 0`
5. Run: `/islandselector seticon 99`

**Expected Results:**
- ✅ Error: "&cSlot number must be between 1 and [max]"
- ✅ Shows correct max from config (default: 5)

---

### Test #5: Slot doesn't exist yet

**Purpose:** Verify error when trying to set icon for non-existent slot

**Steps:**
1. Hold any item
2. Run: `/islandselector seticon 5` (assuming you only have slots 1-2)
3. Observe message

**Expected Results:**
- ✅ Error: "&cYou don't have an island in slot 5 yet!"
- ✅ Helpful message explaining the slot doesn't exist

---

### Test #6: Tab completion

**Purpose:** Verify tab completion suggests slot numbers

**Steps:**
1. Type: `/islandselector seticon ` (with space, don't press enter)
2. Press TAB key
3. Observe suggestions

**Expected Results:**
- ✅ Shows slot numbers: 1, 2, 3, 4, 5 (up to max slots in config)
- ✅ Suggestions cycle when pressing TAB multiple times

---

### Test #7: Various item types

**Purpose:** Verify different materials work as icons

**Steps:**
Test with these items:
1. Hold **oak planks** → `/islandselector seticon 1`
2. Check slots GUI → Should show oak planks
3. Hold **beacon** → `/islandselector seticon 1`
4. Check slots GUI → Should show beacon
5. Hold **player head** → `/islandselector seticon 1`
6. Check slots GUI → Should show player head
7. Hold **enchanted diamond sword** → `/islandselector seticon 1`
8. Check slots GUI → Should show diamond sword (enchantment optional)

**Expected Results:**
- ✅ All standard Minecraft items work as icons
- ✅ Icon changes immediately
- ✅ GUI reflects the change

---

### Test #8: Persistence after server restart

**Purpose:** Verify icon settings survive server restart

**Steps:**
1. Set slot 1 icon to **nether star**:
   - Hold nether star
   - `/islandselector seticon 1`
2. Set slot 2 icon to **totem of undying**:
   - Hold totem
   - `/islandselector seticon 2`
3. Verify in GUI: `/islandselector slots`
4. **Stop the server**
5. **Start the server**
6. Rejoin and run: `/islandselector slots`

**Expected Results:**
- ✅ Slot 1 still shows nether star icon
- ✅ Slot 2 still shows totem of undying icon
- ✅ Icons persist across restarts (saved to database)

---

### Test #9: Permission check

**Purpose:** Verify permission requirement

**Steps:**
1. Remove permission from your player:
   - Op yourself: `/op <yourname>`
   - Run: `/lp user <yourname> unset islandselector.seticon`
   (Or use your permission plugin's syntax)
2. Hold any item
3. Run: `/islandselector seticon 1`

**Expected Results:**
- ✅ Permission denied message
- ✅ Command doesn't execute

**Restore permission:**
4. Run: `/lp user <yourname> set islandselector.seticon true`
5. Run: `/islandselector seticon 1`

**Expected Results:**
- ✅ Command works again

---

### Test #10: Integration with setname command

**Purpose:** Verify seticon and setname work together

**Steps:**
1. Set custom name: `/islandselector setname 1 Diamond Mine`
2. Hold diamond ore
3. Set custom icon: `/islandselector seticon 1 `
4. Open slots GUI: `/islandselector slots`

**Expected Results:**
- ✅ Slot 1 shows:
  - Custom name: "Diamond Mine"
  - Custom icon: Diamond ore
- ✅ Both customizations apply together
- ✅ Display shows both name and icon correctly

---

### Test #11: Help command shows seticon

**Purpose:** Verify command appears in help

**Steps:**
1. Run: `/islandselector help`
2. Look for seticon in the command list

**Expected Results:**
- ✅ `/islandselector seticon <slot>` appears in list
- ✅ Description: "Set a custom icon for an island slot"
- ✅ Shows correct syntax

---

## Edge Cases to Watch For

### ⚠️ Potential Issues

1. **Material name validation**
   - Does it handle invalid materials gracefully?
   - Currently stores whatever material name from held item

2. **AIR material**
   - ✅ Already handled - checks for AIR and null

3. **Offhand vs main hand**
   - Implementation uses main hand only (correct)

4. **Icon display in GUI**
   - Verify SlotSelectionGUI actually uses the iconMaterial field
   - Should already be implemented

5. **Database save**
   - Uses SlotManager.setSlotIcon() which should save automatically
   - Verify with restart test

---

## Test Results Checklist

Mark each test as you complete it:

- [ ] Test #1: Set slot icon with held item
- [ ] Test #2: Error when no item held
- [ ] Test #3: Different icons for different slots
- [ ] Test #4: Invalid slot number
- [ ] Test #5: Slot doesn't exist
- [ ] Test #6: Tab completion
- [ ] Test #7: Various item types
- [ ] Test #8: Persistence after restart
- [ ] Test #9: Permission check
- [ ] Test #10: Integration with setname
- [ ] Test #11: Help command

---

## What to Report

### ✅ If Everything Works
Report: "Test #96-97 PASSED - seticon command works perfectly"

### ❌ If Issues Found
Report each issue with:
- Which test failed (Test #1, #2, etc.)
- What you did (exact steps)
- What you expected
- What actually happened
- Any console errors

### Example Issue Report
```
Test #1 FAILED
Steps: Held diamond pickaxe, ran /islandselector seticon 1
Expected: Success message and icon change
Actual: Error "NullPointerException" in console
Console: [ERROR] at SlotManager.setSlotIcon line 189
```

---

## Technical Implementation Details

### Files Changed
- ✅ **NEW:** `SetIconCommand.java` (command implementation)
- ✅ **MODIFIED:** `IslandSelectorCommand.java` (registered command)
- ✅ **MODIFIED:** `en-US.yml` (added 6 locale messages)

### Existing Infrastructure Used
- ✅ `SlotManager.setSlotIcon()` - Already implemented
- ✅ `SlotData.iconMaterial` field - Database storage
- ✅ `SlotData.setIconMaterial()` / `getIconMaterial()` - Getters/setters

### Command Details
- **Command:** `/islandselector seticon <slot>`
- **Aliases:** `/is seticon`
- **Permission:** `islandselector.seticon`
- **Player only:** Yes
- **Arguments:** Exactly 1 (slot number)
- **Tab completion:** Slot numbers 1-5

### Validation Logic
1. ✅ Argument count (must be exactly 1)
2. ✅ Slot number format (must be integer)
3. ✅ Slot number range (1 to maxSlots from config)
4. ✅ Slot existence (player must have that slot)
5. ✅ Item in hand (must be holding non-AIR item)

### Success Flow
1. Parse slot number from args
2. Get SlotData from SlotManager
3. Get held item material name
4. Call `SlotManager.setSlotIcon(uuid, slot, material)`
5. Send success message with slot number and icon name
6. SlotManager saves to database automatically

---

## Related Tests for Future Sessions

These related tests are **NOT** implemented yet:

- **Test #90:** Slot settings GUI has Change Icon button
- **Test #91:** Clicking Change Icon in GUI (GUI-based approach)
- **Test #98:** Slot icon persistence (covered by Test #8 above)

The GUI-based approach (right-clicking slots to open settings) is a different implementation that may come in future sessions. The command approach implemented here should satisfy the core functionality requirements.

---

## Estimated Testing Time

- Quick test: **3 minutes**
- Comprehensive test (all 11 tests): **20-25 minutes**
- Edge cases and exploration: **+5 minutes**

**Total:** ~30 minutes for thorough testing

---

## Next Steps After Testing

### If Tests Pass ✅
- Mark Test #96-97 as passing in feature_list.json
- Report success to developer
- Ready to implement next features

### If Tests Fail ❌
- Report specific failures with details
- Developer will fix issues
- Re-test after fixes

---

## Session 29 Summary

**Feature:** Slot icon customization via command
**Complexity:** Low (similar to setname command)
**Build Status:** ✅ SUCCESS
**Ready for Testing:** ✅ YES

This command allows players to personalize their island slots with custom icons, making it easier to visually identify different islands at a glance. Combined with the setname command from Session 28, players can now fully customize how their slots appear in the GUI.
