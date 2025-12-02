# Session 40 - Detailed Testing Procedures
## Slot Selection GUI & Slot Management System

**Session Type:** Discovery/Documentation
**Features:** Slot selection GUI, permission-based slot unlocking, slot switching with FAWE
**Build:** No changes (documentation only)
**JAR:** output/IslandSelector-1.0.0-SNAPSHOT.jar (180K)

---

## Overview

This session documents the **Slot Selection GUI system** and related slot management features that were already implemented in previous sessions but never verified by human testing.

The slot system allows players to maintain multiple separate islands in different "slots" at the same grid location. Only one slot is active (loaded in world) at a time. Players can switch between slots, with the current island being saved as a schematic and the new slot's island being loaded.

**Tests Covered:** Approximately Tests #63-80 from feature_list.json

---

## Prerequisites

### Server Requirements
- Paper/Spigot 1.20.x - 1.21.x
- BentoBox (latest)
- BSkyBlock addon
- **FastAsyncWorldEdit (FAWE)** - REQUIRED for slot switching
- Permissions plugin (LuckPerms recommended)

### Testing Requirements
- **Players:** 2 accounts (1 for basic tests, 2 for team member tests)
- **Time:** 20-30 minutes for full test suite
- **Permissions:** Ability to grant permissions for slot unlocking tests

### Setup Steps
1. Copy `output/IslandSelector-1.0.0-SNAPSHOT.jar` to `plugins/BentoBox/addons/`
2. Restart server
3. Verify no errors in console
4. Create at least one island before testing slots

---

## Test Suite

### Section 1: Slot Selection GUI Layout (Tests #63)

#### Test 1.1: GUI Opens with Correct Layout

**Objective:** Verify the slot selection GUI displays correctly

**Steps:**
1. Create an island if you haven't already
2. Execute command: `/islandselector slots`
3. Observe the GUI that opens

**Expected Results:**
- GUI opens (chest inventory)
- Title bar shows: "Your Island Slots"
- GUI has 3 rows (27 slots total)
- Slots visible at positions 2, 3, 4, 5, 6 (top row, center)
- Back button (ARROW) visible at slot 20 (bottom row, left side)
- Close button (BARRIER) visible at slot 24 (bottom row, right side)
- Empty spaces filled with black glass panes

**Pass Criteria:**
- ✅ GUI opens without errors
- ✅ Title is "Your Island Slots"
- ✅ 27 slots (3 rows)
- ✅ Slot items visible in correct positions
- ✅ Control buttons present

**Troubleshooting:**
- If GUI doesn't open: Check permission `islandselector.slots`
- If layout wrong: Check console for errors
- If items missing: Verify SlotSelectionGUI.java loaded correctly

---

### Section 2: Slot Display Types (Tests #64-67)

#### Test 2.1: Active Slot Display

**Objective:** Verify active slot displays with correct visuals

**Prerequisites:** Have an island created (it becomes slot 1, active by default)

**Steps:**
1. Open slots GUI: `/islandselector slots`
2. Locate slot 1 item (should be at inventory position 2)
3. Observe the item appearance
4. Hover over the item to see tooltip

**Expected Results:**
- **Material:** GRASS_BLOCK (or custom icon if set via `/islandselector seticon`)
- **Enchantment Glow:** Yes (shimmering effect)
- **Display Name:** Green and bold text (default: "Slot 1")
- **Lore:**
  - Line 1: "Level: 0" (or actual level if Level addon present)
  - Line 2: "Members: 1" (or actual team size)
  - Line 3: Empty
  - Line 4: "★ ACTIVE" in yellow

**Pass Criteria:**
- ✅ Enchantment glow visible
- ✅ Name is green and bold
- ✅ "★ ACTIVE" indicator present
- ✅ Level and members shown

**Troubleshooting:**
- No glow: Check SlotSelectionGUI.java line 95-98 (enchantment addition)
- Wrong color: Check line 90 (display name uses &a&l)
- Missing lore: Check lines 101-103

---

#### Test 2.2: Inactive Slot with Island Display

**Objective:** Verify inactive slots show correctly

**Prerequisites:** Have islands in both slot 1 and slot 2, with slot 1 active

**Setup:**
```
# Create second island (see Test 5.2 for detailed steps)
# Or use admin commands to set up slot 2 with island data
```

**Steps:**
1. Ensure slot 2 has an island but is inactive
2. Open slots GUI
3. Locate slot 2 item
4. Compare to slot 1 (active)

**Expected Results:**
- **Material:** GRASS_BLOCK (or custom icon)
- **Enchantment Glow:** No (no shimmering)
- **Display Name:** White text (not bold)
- **Lore:**
  - Line 1: "Level: 0"
  - Line 2: "Members: 1"
  - Line 3: Empty
  - Line 4: "Click to switch to this slot" in yellow

**Pass Criteria:**
- ✅ No enchantment glow
- ✅ Name is white (not green)
- ✅ Shows "Click to switch" prompt
- ✅ Different from active slot appearance

---

#### Test 2.3: Empty Unlocked Slot Display

**Objective:** Verify empty slots show correctly

**Prerequisites:** Player has permission for 2+ slots but only 1 island

**Setup:**
```bash
# Give permission for 2 slots
lp user <username> permission set islandselector.slots.2 true

# Or via permissions.yml:
# permissions:
#   <username>:
#     - islandselector.slots.2
```

**Steps:**
1. Verify you have only 1 island (slot 1)
2. Open slots GUI
3. Locate slot 2 item
4. Observe appearance

**Expected Results:**
- **Material:** LIME_DYE (bright green dye item)
- **Display Name:** "Empty Slot" in green
- **Lore:**
  - Line 1: "This slot is available!"
  - Line 2: Empty
  - Line 3: "Click to create a new island" in yellow

**Pass Criteria:**
- ✅ Shows as LIME_DYE
- ✅ Green "Empty Slot" name
- ✅ "Click to create" prompt
- ✅ Clearly distinguishable from other slot types

---

#### Test 2.4: Locked Slot Display

**Objective:** Verify locked slots show correctly

**Prerequisites:** Player has permission for fewer than 5 slots

**Setup:**
```bash
# Remove all slot permissions
lp user <username> permission unset islandselector.slots.2
lp user <username> permission unset islandselector.slots.3
lp user <username> permission unset islandselector.slots.4
lp user <username> permission unset islandselector.slots.5

# Player should only have default (1 slot)
# Or config.yml default-slots: 1
```

**Steps:**
1. Open slots GUI
2. Locate slot 2 (or any slot beyond your permission)
3. Hover to see tooltip

**Expected Results:**
- **Material:** BARRIER (red X block)
- **Display Name:** "Locked Slot" in red
- **Lore:**
  - Line 1: "This slot is locked."
  - Line 2: Empty
  - Line 3: "Unlock with: islandselector.slots.X" (where X is slot number)

**Pass Criteria:**
- ✅ Shows as BARRIER item
- ✅ Red "Locked Slot" name
- ✅ Shows required permission
- ✅ Clearly indicates locked state

---

### Section 3: Permission System (Tests #68-70)

#### Test 3.1: Permission islandselector.slots.2

**Objective:** Verify 2-slot permission unlocks slots 1-2

**Steps:**
1. Remove all slot permissions from player
2. Give permission: `islandselector.slots.2`
3. Open slots GUI
4. Check which slots are unlocked/locked

**Expected Results:**
- **Slot 1:** Unlocked (active island or empty)
- **Slot 2:** Unlocked (empty or inactive island)
- **Slots 3-5:** Locked (BARRIER items)

**Pass Criteria:**
- ✅ First 2 slots accessible
- ✅ Remaining slots show as locked
- ✅ Locked slots have permission message

**Command:**
```bash
lp user <username> permission set islandselector.slots.2 true
```

---

#### Test 3.2: Permission islandselector.slots.5

**Objective:** Verify 5-slot permission unlocks all slots

**Steps:**
1. Give permission: `islandselector.slots.5`
2. Open slots GUI
3. Verify all 5 slots visible and accessible

**Expected Results:**
- **Slots 1-5:** All unlocked (no BARRIER items)
- **Appearance:** Combination of active/inactive/empty slots based on islands created
- **No locked slots**

**Pass Criteria:**
- ✅ All 5 slots accessible
- ✅ No BARRIER items visible
- ✅ Can interact with any slot

**Command:**
```bash
lp user <username> permission set islandselector.slots.5 true
```

---

#### Test 3.3: Permission Hierarchy

**Objective:** Verify higher permissions include lower

**Steps:**
1. Test with `slots.3` permission
   - Expected: 3 slots unlocked
2. Test with `slots.4` permission
   - Expected: 4 slots unlocked
3. Verify higher permission doesn't require lower
   - Give only `slots.5`, not `slots.2` or `slots.3`
   - Expected: All 5 slots unlocked

**Expected Results:**
- Permission level determines available slots
- Higher permission includes lower (slots.5 gives access to 1-5)
- Don't need all intermediate permissions

**Pass Criteria:**
- ✅ `slots.3` → 3 slots
- ✅ `slots.4` → 4 slots
- ✅ `slots.5` → 5 slots
- ✅ Hierarchy works correctly

---

#### Test 3.4: Clicking Locked Slot

**Objective:** Verify error message when clicking locked slot

**Prerequisites:** Have < 5 slots unlocked

**Steps:**
1. Open slots GUI
2. Click on a BARRIER (locked slot) item
3. Observe chat message

**Expected Results:**
- **Message 1:** "This slot is locked!" (red)
- **Message 2:** "You need permission: islandselector.slots.X" (gray)
- **Behavior:** No GUI opens, no action occurs
- **GUI:** Remains open

**Pass Criteria:**
- ✅ Error message displayed
- ✅ Shows required permission
- ✅ No switch or creation attempted
- ✅ GUI stays open

---

### Section 4: Slot Interactions (Tests #71-73)

#### Test 4.1: Clicking Empty Slot

**Objective:** Verify empty slot creation flow

**Prerequisites:**
- Have island in slot 1
- Have permission for slot 2
- Slot 2 is empty

**Steps:**
1. Open slots GUI
2. Click on empty slot 2 (LIME_DYE)
3. Observe result

**Current Expected Behavior:**
- **Message:** "Island creation in empty slots coming soon..."
- **Reason:** Island creation in empty slots not fully integrated yet

**Future Expected Behavior:**
- BSkyBlock blueprint selection GUI opens
- Player selects island type
- Island created in slot 2 at same grid location as slot 1

**Pass Criteria:**
- ✅ Clicking empty slot is handled (no error)
- ✅ Message displayed or creation flow starts
- ✅ No crashes or console errors

**Note:** This feature may show "coming soon" message - that's acceptable for current build.

---

#### Test 4.2: Same Grid Location for All Slots

**Objective:** Verify all slots share same grid location

**Prerequisites:**
- Have islands in multiple slots (if creation works)

**Steps:**
1. Note grid location of slot 1 island (e.g., H8)
2. Switch to slot 2
3. Check grid location of slot 2 island
4. Verify it's the same coordinates

**Expected Results:**
- **Slot 1 location:** H8 (example: world X=3500, Z=3500)
- **Slot 2 location:** H8 (same world X=3500, Z=3500)
- **Behavior:** Slots share location, only one loaded at a time

**Pass Criteria:**
- ✅ All slots at same grid coordinate
- ✅ Only one slot visible in world at a time
- ✅ Switching changes which island is loaded

---

#### Test 4.3: Clicking Inactive Slot

**Objective:** Verify switch confirmation opens

**Prerequisites:**
- Have islands in slot 1 (active) and slot 2 (inactive)

**Steps:**
1. Open slots GUI
2. Left-click on slot 2 (inactive island)
3. Observe new GUI

**Expected Results:**
- **Old GUI:** Closes
- **New GUI:** Opens (Slot Switch Confirmation)
- **Title:** "Confirm Slot Switch"
- **Content:**
  - Shows current slot name
  - Shows target slot name
  - Arrow or "→" indicating direction
  - Confirm button (GREEN WOOL)
  - Cancel button (RED WOOL)

**Pass Criteria:**
- ✅ Confirmation GUI opens
- ✅ Shows both slot names
- ✅ Confirm/Cancel buttons present
- ✅ No instant switch (confirmation required)

---

### Section 5: Slot Switching (Tests #74-78)

#### Test 5.1: Switch Confirmation GUI

**Objective:** Verify confirmation shows correct information

**Prerequisites:**
- Rename slot 1: `/islandselector setname Main Base`
- Rename slot 2: `/islandselector setname Farm Island`
- Have islands in both slots

**Steps:**
1. Initiate switch from slot 1 to slot 2
2. Examine confirmation GUI carefully

**Expected Results:**
- **Title:** "Confirm Slot Switch"
- **Display:** Shows "Main Base" → "Farm Island"
- **Warnings:**
  - Visitors will be teleported
  - Cooldown message (if applicable)
- **Buttons:**
  - GREEN WOOL labeled "Confirm"
  - RED WOOL labeled "Cancel"

**Pass Criteria:**
- ✅ Slot names displayed correctly
- ✅ Direction clear (from → to)
- ✅ Warnings present
- ✅ Buttons functional

---

#### Test 5.2: Slot Switch - Save Current Island

**Objective:** Verify current island is saved as schematic

**Prerequisites:**
- Have structures on slot 1 island (place some unique blocks)
- Ready to switch to slot 2

**Steps:**
1. Build recognizable structures on slot 1
   - Example: 5-block tall pillar of diamond blocks
2. Note file system before switch
3. Initiate switch to slot 2
4. Click Confirm
5. Wait for "Saving current island..." message
6. After switch, check file system

**File Check:**
```
Navigate to: plugins/IslandSelector/slots/{your-uuid}/
Look for: slot-1.schem
```

**Expected Results:**
- **File exists:** `slot-1.schem`
- **File size:** > 0 bytes (typically 100KB-5MB)
- **Format:** FAWE schematic format
- **Progress message:** "Saving current island..."
- **Console:** No WorldEdit errors

**Pass Criteria:**
- ✅ Schematic file created
- ✅ File has content (not 0 bytes)
- ✅ Progress message shown
- ✅ No errors in console

**Troubleshooting:**
- File not created: Check FAWE installed
- 0 bytes: WorldEdit save failed, check console
- Permission denied: Check file write permissions on plugins folder

---

#### Test 5.3: Slot Switch - Clear and Load Islands

**Objective:** Verify island clearing and loading works

**Prerequisites:**
- Have distinct structures in slot 1 and slot 2
- Slot 1 active

**Setup:**
```
Slot 1: Build a RED CONCRETE tower
Slot 2: Build a BLUE CONCRETE tower (after switch, before switching back)
```

**Steps:**
1. Stand on slot 1 island, observe RED tower
2. Switch to slot 2
3. Wait for progress messages
4. After switch, observe the island

**Expected Results:**
- **Progress messages:**
  1. "Starting slot switch..."
  2. "Saving current island..."
  3. "Creating backup..." (if configured)
  4. "Clearing current island from world..."
  5. "Loading target island..."
  6. "Teleporting to new island..."
  7. "Slot switch complete! Welcome to [slot name]"

- **Visual changes:**
  - RED tower disappears (cleared)
  - BLUE tower appears (loaded)
  - Island at same location, different content

**Timing:**
- **Total:** 3-5 seconds for typical island
- **May be longer** for large islands with many blocks

**Pass Criteria:**
- ✅ Old structures removed
- ✅ New structures loaded
- ✅ All progress messages appear
- ✅ No server lag or freeze
- ✅ Switch completes successfully

**Troubleshooting:**
- Stuck on "Saving": FAWE may be slow, wait up to 30 seconds
- Stuck on "Loading": Check console for paste errors
- Lag: Normal for large islands, check server TPS
- Partial load: FAWE async paste, may take extra seconds to complete

---

#### Test 5.4: Slot Switch - Player Teleportation

**Objective:** Verify player is teleported to new island

**Steps:**
1. Stand on slot 1 island
2. Note your position
3. Switch to slot 2
4. After switch completes, check your position

**Expected Results:**
- **Teleport occurs** after island loads
- **Destination:** Slot 2 island spawn point
- **Message:** "Teleporting to new island..."
- **Final message:** "Slot switch complete! Welcome to [slot name]"

**Pass Criteria:**
- ✅ Player teleported automatically
- ✅ Teleported to island spawn (not random location)
- ✅ Can move and interact normally after teleport

---

#### Test 5.5: Slot Switch - Team Member Teleportation

**Objective:** Verify team members are teleported

**Prerequisites:**
- 2 player accounts
- Player 1 (island owner) with 2+ slots
- Player 2 added to team

**Setup:**
```bash
# Player 1
/island team invite Player2

# Player 2
/island team accept
```

**Steps:**
1. Player 2: Stand on Player 1's slot 1 island
2. Player 1: Switch to slot 2
3. Player 2: Observe what happens

**Expected Results:**
- **Player 2 is teleported** to slot 2 island spawn
- **Player 2 gets notification** about slot switch
- **Team membership intact** - Player 2 still on team
- **Player 2 can interact** with slot 2 island

**Pass Criteria:**
- ✅ Team member teleported
- ✅ Team member notified
- ✅ Team structure preserved
- ✅ No permission or access errors

**Troubleshooting:**
- Team member not teleported: Check they were physically on the island
- Permission errors: Verify team membership intact
- Teleported to spawn: Check island spawn point is set

---

### Section 6: Additional Features (Tests #79-80)

#### Test 6.1: Clicking Active Slot

**Objective:** Verify clicking active slot shows appropriate message

**Steps:**
1. Open slots GUI
2. Click on the slot with enchantment glow (active slot)
3. Observe result

**Expected Results:**
- **Message:** "This is already your active slot." (gray)
- **Behavior:** No switch confirmation opens
- **GUI:** Remains open

**Pass Criteria:**
- ✅ Informative message displayed
- ✅ No unnecessary confirmation GUI
- ✅ No errors

---

#### Test 6.2: Right-Click Slot for Settings

**Objective:** Verify right-click opens slot settings GUI

**Prerequisites:** Have an island in slot 1

**Steps:**
1. Open slots GUI
2. **Right-click** on slot 1 (not left-click)
3. Observe new GUI

**Expected Results:**
- **Old GUI:** Closes
- **New GUI:** Opens (Slot Settings GUI)
- **Title:** "Slot Settings"
- **Options:**
  - Rename slot button
  - Change icon button
  - Delete slot button
  - Back button

**Pass Criteria:**
- ✅ Settings GUI opens on right-click
- ✅ Settings options visible
- ✅ Back button returns to slots GUI

**Related Tests:**
- Detailed slot settings tests covered in Session 31
- See `TESTING_SESSION31.md` for full settings GUI testing

---

### Section 7: Edge Cases and Error Handling

#### Test 7.1: Switch with No Target Island

**Objective:** Verify error handling when target slot has no island

**Setup:**
- Slot 2 should be truly empty (no schematic file)

**Steps:**
1. Try to switch to empty slot 2
2. Observe behavior

**Expected Results:**
- **Option 1:** Switch fails with error message
- **Option 2:** Creates new island in slot 2
- **Behavior:** Graceful handling, no crash

**Pass Criteria:**
- ✅ No server crash
- ✅ Clear error or success message
- ✅ Player not left in broken state

---

#### Test 7.2: Switch Without FAWE

**Objective:** Verify error handling when FAWE missing

**Setup:**
- Remove FastAsyncWorldEdit plugin temporarily
- Restart server

**Steps:**
1. Try to switch slots
2. Observe behavior

**Expected Results:**
- **Error message:** "FAWE not found" or similar
- **Behavior:** Switch cancelled gracefully
- **Console:** Warning about missing FAWE

**Pass Criteria:**
- ✅ Clear error message to player
- ✅ No crash or data loss
- ✅ Console shows FAWE missing warning

**Restore:**
- Re-add FastAsyncWorldEdit
- Restart server

---

#### Test 7.3: Rapid Switch Attempts

**Objective:** Verify cooldown system works

**Prerequisites:** Config has switch-cooldown > 0

**Steps:**
1. Switch from slot 1 to slot 2
2. Immediately try to switch back to slot 1
3. Observe result

**Expected Results:**
- **Message:** "You must wait before switching again."
- **Cooldown shown:** Time remaining (e.g., "4 minutes 30 seconds")
- **Behavior:** Switch blocked

**Bypass Test:**
```bash
# Give bypass permission
lp user <username> permission set islandselector.bypass.cooldown.switch true

# Try rapid switch again
# Expected: No cooldown, switch works immediately
```

**Pass Criteria:**
- ✅ Cooldown enforced
- ✅ Time remaining shown
- ✅ Bypass permission works

---

### Section 8: Performance and Stress Tests

#### Test 8.1: Large Island Switch

**Objective:** Verify performance with large island

**Setup:**
- Build a very large island in slot 1
- Example: 100x100 area filled with varied blocks

**Steps:**
1. Note server TPS before switch
2. Switch to slot 2
3. Monitor server performance during switch
4. Check TPS after switch

**Expected Results:**
- **Switch time:** 5-15 seconds (acceptable for large islands)
- **TPS:** May drop briefly, should recover
- **Completion:** Switch completes successfully
- **No crashes** or memory errors

**Pass Criteria:**
- ✅ Large island saves successfully
- ✅ Large island loads successfully
- ✅ Server remains stable
- ✅ TPS recovers after switch

**Monitoring:**
```
/tps  # Check server TPS
/plugins  # Verify all plugins loaded
# Console: Watch for OutOfMemory errors
```

---

#### Test 8.2: Multiple Players Switching

**Objective:** Verify multiple simultaneous switches work

**Prerequisites:** 2+ players with multi-slot islands

**Steps:**
1. Player 1: Start switch from slot 1 to slot 2
2. Player 2: Immediately start switch from slot 1 to slot 2
3. Wait for both to complete

**Expected Results:**
- **Both switches complete** successfully
- **No conflicts** between operations
- **Each player** teleported to their own island
- **Schematics** saved correctly for both

**Pass Criteria:**
- ✅ Concurrent switches supported
- ✅ No data corruption
- ✅ Both players successful
- ✅ No server errors

---

## Configuration Testing

### Config Options

Test these configuration values in `config.yml`:

```yaml
slots:
  default-slots: 1  # Test with 1, 2, 3
  max-slots: 5
  switch-cooldown: 300  # Test with 0, 60, 300
  backup-on-switch: true  # Test true and false
```

#### Config Test 1: Default Slots

**Steps:**
1. Set `default-slots: 2`
2. Reload: `/islandselector admin reload`
3. Remove all slot permissions from player
4. Open slots GUI

**Expected:** Slots 1-2 unlocked by default, 3-5 locked

---

#### Config Test 2: Switch Cooldown

**Steps:**
1. Set `switch-cooldown: 60` (1 minute)
2. Reload config
3. Switch slots
4. Try to switch back immediately

**Expected:** Cooldown message with ~60 seconds remaining

---

#### Config Test 3: Backup on Switch

**Steps:**
1. Set `backup-on-switch: true`
2. Reload config
3. Switch slots
4. Check `plugins/IslandSelector/backups/{uuid}/`

**Expected:** Backup file created with timestamp

---

## Report Template

```
=== SESSION 40 DETAILED TEST REPORT ===

Test Date: [Date]
Tester: [Username]
Server Version: [Paper/Spigot version]
BentoBox Version: [Version]
FAWE Version: [Version]

SECTION 1: GUI LAYOUT
✅ Test 1.1 - GUI Opens Correctly: [PASS/FAIL]

SECTION 2: SLOT DISPLAYS
✅ Test 2.1 - Active Slot Display: [PASS/FAIL]
✅ Test 2.2 - Inactive Slot Display: [PASS/FAIL]
✅ Test 2.3 - Empty Slot Display: [PASS/FAIL]
✅ Test 2.4 - Locked Slot Display: [PASS/FAIL]

SECTION 3: PERMISSIONS
✅ Test 3.1 - slots.2 Permission: [PASS/FAIL]
✅ Test 3.2 - slots.5 Permission: [PASS/FAIL]
✅ Test 3.3 - Permission Hierarchy: [PASS/FAIL]
✅ Test 3.4 - Clicking Locked Slot: [PASS/FAIL]

SECTION 4: INTERACTIONS
✅ Test 4.1 - Clicking Empty Slot: [PASS/FAIL/EXPECTED MESSAGE]
✅ Test 4.2 - Same Grid Location: [PASS/FAIL]
✅ Test 4.3 - Clicking Inactive Slot: [PASS/FAIL]

SECTION 5: SLOT SWITCHING
✅ Test 5.1 - Switch Confirmation: [PASS/FAIL]
✅ Test 5.2 - Saves to Schematic: [PASS/FAIL]
  - Schematic file created: [YES/NO]
  - File size: [SIZE]
✅ Test 5.3 - Clear and Load: [PASS/FAIL]
  - Switch time: [SECONDS]
✅ Test 5.4 - Player Teleport: [PASS/FAIL]
✅ Test 5.5 - Team Member Teleport: [PASS/FAIL]

SECTION 6: ADDITIONAL
✅ Test 6.1 - Click Active Slot: [PASS/FAIL]
✅ Test 6.2 - Right-Click Settings: [PASS/FAIL]

SECTION 7: EDGE CASES
✅ Test 7.1 - No Target Island: [PASS/FAIL]
✅ Test 7.2 - Without FAWE: [PASS/FAIL/SKIPPED]
✅ Test 7.3 - Rapid Switch (Cooldown): [PASS/FAIL]

SECTION 8: PERFORMANCE
✅ Test 8.1 - Large Island Switch: [PASS/FAIL]
  - Island size: [BLOCKS]
  - Switch time: [SECONDS]
  - TPS impact: [BEFORE] → [DURING] → [AFTER]
✅ Test 8.2 - Multiple Players: [PASS/FAIL]

CONFIGURATION TESTS
✅ Config Test 1 - Default Slots: [PASS/FAIL]
✅ Config Test 2 - Switch Cooldown: [PASS/FAIL]
✅ Config Test 3 - Backup on Switch: [PASS/FAIL]

ISSUES FOUND:
[List any bugs, errors, unexpected behavior]

CONSOLE ERRORS:
[Paste any relevant console errors]

SCHEMATIC FILES:
[List created .schem files and their sizes]

PERFORMANCE NOTES:
[Any lag, delays, or performance issues]

OVERALL ASSESSMENT: [PASS/FAIL]

RECOMMENDATIONS:
[Any suggestions for improvements]
```

---

## Success Criteria Summary

**PASS if:**
- ✅ All GUI elements display correctly
- ✅ All slot types render properly (active/inactive/empty/locked)
- ✅ Permission system works accurately
- ✅ Slot switching completes successfully
- ✅ Schematics save and load correctly
- ✅ Players teleported appropriately
- ✅ No server crashes or data corruption
- ✅ No critical console errors

**ACCEPTABLE WARNINGS:**
- ⚠️ "Island creation in empty slots coming soon" message (expected)
- ⚠️ Slight TPS drop during large island switch (expected)
- ⚠️ 3-5 second delay for slot switch (normal)

**FAIL if:**
- ❌ GUI doesn't open or crashes
- ❌ Slot switching fails or corrupts data
- ❌ Schematics not saved (data loss)
- ❌ Server crashes during switch
- ❌ Critical WorldEdit errors
- ❌ Players lose island data

---

## Quick Reference

**Commands:**
- `/islandselector slots` - Open slot selection GUI
- `/islandselector setname <name>` - Rename active slot
- `/islandselector seticon` - Change slot icon (hold item)
- `/islandselector delete` - Delete active slot

**Permissions:**
- `islandselector.slots` - Use slots command
- `islandselector.slots.2` - Unlock 2 slots
- `islandselector.slots.3` - Unlock 3 slots
- `islandselector.slots.4` - Unlock 4 slots
- `islandselector.slots.5` - Unlock 5 slots
- `islandselector.bypass.cooldown.switch` - Bypass switch cooldown

**Files:**
- Schematics: `plugins/IslandSelector/slots/{uuid}/slot-{N}.schem`
- Backups: `plugins/IslandSelector/backups/{uuid}/slot-{N}-{timestamp}.schem`

**Time Estimates:**
- Basic GUI tests: 5 minutes
- Permission tests: 5 minutes
- Slot switching tests: 10 minutes
- Edge cases: 5 minutes
- Performance tests: 5 minutes
- **Total:** 20-30 minutes

---

**End of Detailed Testing Procedures**

For quick testing, see `SESSION40_FOR_HUMAN_TESTER.md`
For previous slot management tests, see `TESTING_SESSION28.md`, `TESTING_SESSION29.md`, `TESTING_SESSION30.md`, `TESTING_SESSION31.md`
