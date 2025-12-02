# SESSION 30 - Testing Instructions

## Features Implemented This Session

### Test #103: Command /islandselector delete initiates slot deletion with confirmation
**Status:** ✅ Implemented, ⏳ Awaiting Manual Testing

**New Files Created:**
- `DeleteCommand.java` - Command handler for /islandselector delete
- `SlotDeleteConfirmationGUI.java` - Confirmation GUI with warning messages

**Files Modified:**
- `IslandSelectorCommand.java` - Registered delete command
- `en-US.yml` - Added delete command locale messages

---

## Quick Test (3 minutes)

1. Copy JAR to `plugins/BentoBox/addons/`
2. Restart server
3. Create at least 2 island slots (use `/islandselector slots`)
4. Run: `/islandselector delete 2`
5. **Expected:** Confirmation GUI opens with red warning
6. Click green "CANCEL" button
7. **Expected:** Returns to slot selection, slot not deleted
8. Run: `/islandselector delete 2` again
9. Click red "DELETE SLOT" button
10. **Expected:** Slot 2 is deleted permanently

---

## Comprehensive Test Plan

### Test 1: Basic Delete Command - Non-Active Slot
**Steps:**
1. Create 3 island slots (slots 1, 2, 3)
2. Ensure slot 1 is active (current island)
3. Run: `/islandselector delete 2`
4. Verify confirmation GUI opens
5. GUI should show:
   - Title: "Confirm Slot Deletion"
   - Red barrier item with warning text
   - "This action is PERMANENT!" message
   - Shows you have 3 slots, will have 2 remaining
6. Click red "DELETE SLOT" button
7. Verify slot 2 is deleted
8. Run `/islandselector slots`
9. Verify only slots 1 and 3 exist
10. Verify slot 1 is still active

**Expected Result:** ✅ Non-active slot deleted without switching

---

### Test 2: Delete Active Slot (with other slots available)
**Steps:**
1. Create 2 island slots (slots 1, 2)
2. Ensure slot 1 is active
3. Run: `/islandselector delete 1`
4. Verify warning message: "⚠ Warning: This is your currently active slot!"
5. Confirm deletion in GUI
6. Verify message: "This is your active slot. Finding another slot to switch to..."
7. Verify message: "Switching to slot #2..."
8. Wait for switch to complete (~5 seconds)
9. Verify message: "Slot #1 has been deleted!"
10. Run `/islandselector slots`
11. Verify only slot 2 exists
12. Verify slot 2 is now active

**Expected Result:** ✅ Automatically switches to another slot before deleting

---

### Test 3: Delete Last Remaining Slot
**Steps:**
1. Create only 1 island slot (slot 1)
2. Build some structures on the island
3. Run: `/islandselector delete 1`
4. Verify GUI warning: "⚠ This is your only slot!"
5. Verify GUI message: "Deleting it will remove your island from the grid completely."
6. Confirm deletion
7. Verify message: "Your island has been deleted from the world."
8. Verify message: "Your grid location is now available."
9. Check the world - island should be deleted
10. Open grid GUI - location should show as available (green glass)
11. Verify you can claim a new island

**Expected Result:** ✅ Island removed from world and grid, player can start fresh

---

### Test 4: Canceling Deletion
**Steps:**
1. Create 2 slots
2. Run: `/islandselector delete 2`
3. Confirmation GUI opens
4. Click green "CANCEL" button
5. Verify message: "Slot deletion cancelled."
6. Verify returns to slot selection GUI
7. Verify slot 2 still exists
8. Verify no changes made

**Expected Result:** ✅ Cancellation works, no data lost

---

### Test 5: Invalid Slot Number
**Steps:**
1. Run: `/islandselector delete 99`
2. Verify error: "Slot number must be between 1 and 5"
3. Run: `/islandselector delete 0`
4. Verify error: "Slot number must be between 1 and 5"
5. Run: `/islandselector delete -1`
6. Verify error: "Slot number must be between 1 and 5"

**Expected Result:** ✅ Validation prevents invalid slot numbers

---

### Test 6: Non-Existent Slot
**Steps:**
1. Create only slot 1
2. Run: `/islandselector delete 3`
3. Verify error: "You don't have an island in slot 3!"
4. Verify no GUI opens
5. Verify slot 1 remains unchanged

**Expected Result:** ✅ Cannot delete slots that don't exist

---

### Test 7: Invalid Arguments
**Steps:**
1. Run: `/islandselector delete`
2. Verify shows help/usage message
3. Run: `/islandselector delete abc`
4. Verify error: "Invalid slot number. Must be a number."
5. Run: `/islandselector delete 1 2`
6. Verify shows help/usage message (too many args)

**Expected Result:** ✅ Proper error handling for invalid input

---

### Test 8: Tab Completion
**Steps:**
1. Create slots 1, 2, and 4 (skip 3)
2. Type: `/islandselector delete <TAB>`
3. Verify suggestions show: 1, 2, 4
4. Verify suggestion 3 is NOT shown (doesn't exist)
5. Select 2 from tab completion
6. Verify command becomes: `/islandselector delete 2`

**Expected Result:** ✅ Tab completion only shows existing slots

---

### Test 9: Permission Check
**Steps:**
1. Remove permission: `islandselector.delete` from a player
2. Player runs: `/islandselector delete 1`
3. Verify permission error message
4. Give permission back
5. Player runs command again
6. Verify it works

**Expected Result:** ✅ Permission system works correctly

---

### Test 10: Persistence After Restart
**Steps:**
1. Create 3 slots
2. Delete slot 2
3. Verify only slots 1 and 3 exist
4. Restart server
5. Run `/islandselector slots`
6. Verify only slots 1 and 3 exist (slot 2 still deleted)
7. Verify slot data persists correctly

**Expected Result:** ✅ Deletion persists through server restart

---

### Test 11: Multiple Deletes in Sequence
**Steps:**
1. Create 5 slots (slots 1-5)
2. Ensure slot 1 is active
3. Delete slot 5: `/islandselector delete 5`
4. Confirm deletion
5. Verify slot 5 deleted
6. Delete slot 4: `/islandselector delete 4`
7. Confirm deletion
8. Verify slot 4 deleted
9. Delete slot 3: `/islandselector delete 3`
10. Confirm deletion
11. Verify only slots 1 and 2 remain
12. Verify slot 1 still active

**Expected Result:** ✅ Can delete multiple slots sequentially

---

## Key Commands for Testing

```
/islandselector delete 1         # Delete slot 1
/islandselector delete 2         # Delete slot 2
/islandselector delete <TAB>     # Test tab completion
/islandselector delete 99        # Test invalid slot
/islandselector delete abc       # Test non-number
/islandselector slots            # View remaining slots
```

---

## What to Look For

### ✅ Success Indicators:
- Confirmation GUI opens with clear warning messages
- Cancel button works and preserves slot
- Confirm button deletes slot permanently
- Active slot switches before deletion if needed
- Last slot deletion removes island from world
- Grid location becomes available after deletion
- Tab completion only shows existing slots
- Permission system enforced

### ❌ Failure Indicators:
- GUI doesn't open
- Slot deleted without confirmation
- Active slot deleted without switching
- Console errors during deletion
- Slot still exists after deletion
- Island not removed from world when last slot deleted
- Data corruption or crashes
- Tab completion shows non-existent slots

---

## Report Format

**If tests PASS:**
```
Test #103 PASSED - /islandselector delete works correctly
- All 11 test scenarios completed successfully
- No errors or issues found
```

**If tests FAIL:**
```
Test #103 FAILED - Issue found
Test scenario: [which test failed]
Expected: [what should happen]
Actual: [what actually happened]
Console errors: [paste any errors]
```

---

## Technical Implementation Details

### New Command: `/islandselector delete <slot>`
- Permission: `islandselector.delete`
- Validates slot number (1-maxSlots)
- Checks if slot exists for player
- Warns if deleting active slot
- Opens confirmation GUI

### Confirmation GUI Features:
- 27-slot GUI (3 rows)
- Red barrier icon with warnings
- Shows slot count and impact
- Red "DELETE SLOT" button (slot 11)
- Green "CANCEL" button (slot 15)
- Different messages for only slot vs multiple slots

### Deletion Logic:
1. **Non-active slot:** Deletes immediately
2. **Active slot with others:** Switches to another slot first, then deletes after 5 seconds
3. **Last remaining slot:** Deletes island from BSkyBlock and clears grid location

### Database Operations:
- Uses `SlotManager.deleteSlot()` to remove from database
- Uses `GridManager.clearLocation()` to free grid spot
- Uses `addon.getIslands().deleteIsland()` to remove from world

---

## Estimated Testing Time

- Quick test: **3 minutes**
- Full test (all 11 scenarios): **25-30 minutes**
- Recommended: Run at least tests 1-7 for thorough validation

---

## Next Steps After Testing

**If test PASSES:**
- Mark Test #103 as `"passes": true` in feature_list.json
- Proceed to next unimplemented feature

**If test FAILS:**
- Report specific failure details
- Developer will fix issues
- Re-build and re-test

**Recommended next features:**
1. Test search functionality (Tests #83-86) - already implemented
2. Slot settings GUI - right-click menu (Tests #88-91)
3. Admin backup command (Tests #114-117)
4. Admin purge commands (Tests #118-121)
