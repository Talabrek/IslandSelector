# Session 31 Testing Guide - Slot Settings GUI (Right-Click Menu)

## Overview
Session 31 implements the Slot Settings GUI feature, which allows players to right-click on any island slot in the Slot Selection GUI to open a settings menu. This provides quick access to slot management features like renaming, changing icons, and deleting slots.

**Tests Implemented:** Tests #88-91
- Test #88: Right-clicking slot opens slot settings GUI
- Test #89: Slot settings GUI displays current slot preview
- Test #90: Slot settings GUI has Rename button with name tag
- Test #91: Slot settings GUI has Change Icon button with item frame

**Build Status:** ✅ SUCCESS
**JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar` (155K)

---

## Quick Test (5 minutes)

### Prerequisites
- Server with BentoBox and BSkyBlock
- Player with at least 1 island slot created

### Steps
1. Copy JAR to `plugins/BentoBox/addons/`
2. Restart server
3. Run: `/islandselector slots`
4. Right-click on your island slot
5. Expected: Settings GUI opens with 3 buttons (Rename, Change Icon, Delete)
6. Click "Back to Slots" button
7. Expected: Returns to slot selection GUI

---

## Detailed Test Cases

### Test #88: Right-clicking slot opens slot settings GUI

**Category:** Functional
**Priority:** HIGH
**Estimated Time:** 3 minutes

**Setup:**
1. Join server as a player
2. Ensure you have at least 1 island created in slot 1

**Steps:**
1. Run command: `/islandselector slots`
2. Verify slot selection GUI opens
3. Right-click on slot 1 (your island slot)
4. Verify slot settings GUI opens
5. Check GUI title shows: "Slot Settings: [Slot Name]"

**Expected Results:**
- ✅ Right-click opens settings GUI
- ✅ Title shows "Slot Settings: [Your Slot Name]"
- ✅ GUI has 27 slots (3 rows)
- ✅ No errors in console

**Error Cases to Test:**
- Right-click empty slot → Error message: "This slot is empty! Create an island first before viewing settings."
- Right-click locked slot → Error message: "This slot is locked! You cannot view settings for locked slots."

---

### Test #89: Slot settings GUI displays current slot preview

**Category:** UI
**Priority:** MEDIUM
**Estimated Time:** 2 minutes

**Steps:**
1. Open slot settings for slot 1 (right-click in slot GUI)
2. Check slot 4 of the GUI (center of first row)
3. Verify it shows the slot's current icon (grass block by default)
4. Hover over the preview item
5. Verify tooltip shows the slot's current name
6. Verify tooltip shows "Slot #1"
7. Verify tooltip shows "✔ Currently Active" if it's the active slot

**Expected Results:**
- ✅ Preview item appears in center of first row
- ✅ Preview uses slot's custom icon (if set)
- ✅ Preview shows slot name in golden text
- ✅ Preview shows slot number
- ✅ Preview shows active status if applicable

**Visual Check:**
- Item should be centered and prominent
- Name should be clear and readable
- Active status should be highlighted in green

---

### Test #90: Slot settings GUI has Rename button with name tag

**Category:** UI
**Priority:** MEDIUM
**Estimated Time:** 2 minutes

**Steps:**
1. Open slot settings GUI
2. Locate Rename button (slot 11, left button in second row)
3. Verify it shows as NAME_TAG item
4. Hover over it to see tooltip
5. Verify tooltip shows "Rename Slot" as title
6. Verify tooltip shows current slot name
7. Verify tooltip shows command syntax
8. Click the Rename button
9. Verify GUI closes and shows command instructions

**Expected Results:**
- ✅ Rename button is NAME_TAG item
- ✅ Title: "Rename Slot"
- ✅ Shows current name
- ✅ Shows example command: `/islandselector setname <slot> <name>`
- ✅ Clicking button closes GUI
- ✅ Shows command instructions in chat

**Tooltip Content Check:**
- Current name displayed
- Command syntax shown
- Example provided
- Clear instructions

---

### Test #91: Slot settings GUI has Change Icon button with item frame

**Category:** UI
**Priority:** MEDIUM
**Estimated Time:** 2 minutes

**Steps:**
1. Open slot settings GUI
2. Locate Change Icon button (slot 13, middle button in second row)
3. Verify it shows as ITEM_FRAME item
4. Hover over it to see tooltip
5. Verify tooltip shows "Change Icon" as title
6. Verify tooltip shows current icon material
7. Verify tooltip shows command syntax
8. Verify tooltip explains to hold an item
9. Click the Change Icon button
10. Verify GUI closes and shows command instructions

**Expected Results:**
- ✅ Change Icon button is ITEM_FRAME item
- ✅ Title: "Change Icon"
- ✅ Shows current icon material (e.g., "GRASS_BLOCK")
- ✅ Shows example command: `/islandselector seticon <slot>`
- ✅ Explains to hold item when running command
- ✅ Clicking button closes GUI
- ✅ Shows command instructions in chat

**Tooltip Content Check:**
- Current icon material displayed
- Command syntax shown
- Clear explanation about holding items
- Example provided

---

## Additional Test: Delete Slot Button (Bonus)

Although not part of Tests #88-91, the GUI also includes a Delete button for completeness.

**Steps:**
1. Open slot settings GUI
2. Locate Delete Slot button (slot 15, right button in second row)
3. Verify it shows as TNT item
4. Verify title: "Delete Slot" in red
5. Verify warning text: "⚠ WARNING ⚠"
6. Verify text: "This cannot be undone!"
7. Click Delete button
8. Verify GUI closes and shows delete command instructions

**Expected Results:**
- ✅ Delete button is TNT item
- ✅ Title in red with warnings
- ✅ Multiple warning messages
- ✅ Shows delete command
- ✅ Clicking shows instructions

---

## Integration Tests

### Test: Back Button Navigation

**Steps:**
1. Open slot selection GUI: `/islandselector slots`
2. Right-click on slot 1
3. Settings GUI opens
4. Click "Back to Slots" button (arrow in bottom row)
5. Verify returns to slot selection GUI

**Expected Results:**
- ✅ Back button works
- ✅ Returns to previous GUI
- ✅ Slot selection GUI still shows correctly
- ✅ No memory leaks or listener issues

---

### Test: Multiple Slots Settings

**Prerequisites:**
- Player with 2+ island slots created

**Steps:**
1. Open slot selection GUI
2. Right-click slot 1
3. Verify settings show "Slot Settings: [Slot 1 Name]"
4. Click Back button
5. Right-click slot 2
6. Verify settings show "Slot Settings: [Slot 2 Name]"
7. Verify correct preview and buttons for slot 2

**Expected Results:**
- ✅ Each slot opens its own settings
- ✅ Preview shows correct slot info
- ✅ Commands shown use correct slot number
- ✅ No cross-contamination between slots

---

### Test: Command Instructions Work

**Steps:**
1. Open slot settings for slot 1
2. Click Rename button
3. Follow the command instructions shown
4. Run: `/islandselector setname 1 Test Base`
5. Open slot settings again
6. Verify preview shows new name "Test Base"

**Expected Results:**
- ✅ Command instructions are correct
- ✅ Commands work as shown
- ✅ Changes reflect in settings GUI
- ✅ Integration with commands is seamless

---

## Edge Cases

### Edge Case 1: Empty Slot Right-Click
**Setup:** Player has 2 slots available, but only 1 island
**Action:** Right-click on empty slot 2
**Expected:** Error message: "This slot is empty! Create an island first before viewing settings."

### Edge Case 2: Locked Slot Right-Click
**Setup:** Player has permission for 2 slots, slot 3 is locked
**Action:** Right-click on locked slot 3 (barrier icon)
**Expected:** Error message: "This slot is locked! You cannot view settings for locked slots."

### Edge Case 3: Active vs Inactive Slots
**Setup:** Slot 1 is active, slot 2 is inactive
**Action 1:** Right-click slot 1
**Expected:** Preview shows "✔ Currently Active" in green
**Action 2:** Right-click slot 2
**Expected:** Preview shows "Inactive" in gray

---

## Performance Tests

### Test: Rapid GUI Opening
**Steps:**
1. Open slot selection GUI
2. Right-click slot 1 (opens settings)
3. Click back (returns to slots)
4. Immediately right-click slot 1 again
5. Repeat 10 times rapidly

**Expected Results:**
- ✅ No lag or delay
- ✅ No memory leaks
- ✅ Event listeners properly cleaned up
- ✅ GUIs open smoothly every time

---

## Validation Checklist

### GUI Layout
- [ ] Settings GUI has 27 slots (3 rows × 9 columns)
- [ ] Title shows "Slot Settings: [Slot Name]"
- [ ] Preview item in center of first row (slot 4)
- [ ] Three buttons in second row (slots 11, 13, 15)
- [ ] Back button in bottom row (slot 22)
- [ ] Gray glass panes fill empty slots

### Visual Design
- [ ] Preview item uses slot's custom icon
- [ ] Preview name is in golden/yellow text
- [ ] Button names are clear and descriptive
- [ ] Rename button: NAME_TAG
- [ ] Change Icon button: ITEM_FRAME
- [ ] Delete button: TNT (red text)
- [ ] Back button: ARROW

### Button Functionality
- [ ] All buttons clickable
- [ ] Clicking buttons closes GUI
- [ ] Commands shown in chat
- [ ] Instructions are clear
- [ ] Examples are helpful

### Integration
- [ ] Right-click detection works
- [ ] Left-click still switches/creates normally
- [ ] Back button returns to slot GUI
- [ ] Settings show correct slot data
- [ ] Works for all slot numbers (1-5)

### Error Handling
- [ ] Empty slot right-click handled
- [ ] Locked slot right-click handled
- [ ] Error messages are clear
- [ ] No console errors
- [ ] No crashes or exceptions

### Persistence
- [ ] Settings GUI survives server restart
- [ ] Slot data loads correctly
- [ ] Custom names persist
- [ ] Custom icons persist
- [ ] Active status correct

---

## Known Limitations

1. **Command-based actions:** The buttons show command instructions rather than performing actions directly. This is by design to maintain consistency with the existing command system.

2. **Level and Members:** Preview shows placeholder values ("Level: 0", "Members: 1") as Level addon integration is not yet implemented.

3. **No inline editing:** Players cannot rename or change icons directly in the GUI - they must use commands. This is intentional to maintain the existing command workflow.

---

## Success Criteria

For tests to PASS, all of these must be true:

✅ **Test #88:** Right-clicking a slot with an island opens settings GUI
✅ **Test #89:** Settings GUI displays slot preview with correct icon and name
✅ **Test #90:** Rename button shows with NAME_TAG and helpful instructions
✅ **Test #91:** Change Icon button shows with ITEM_FRAME and helpful instructions

✅ **Bonus:** Delete button also works correctly
✅ **Integration:** Back button returns to slot selection
✅ **Error handling:** Empty and locked slots handled gracefully
✅ **Performance:** No memory leaks, smooth operation

---

## Reporting Results

### If Tests Pass
Report: "Tests #88-91 PASSED - Slot settings GUI works perfectly"

### If Tests Fail
Report which specific test failed and provide:
- Which test number (#88-91)
- What was expected vs what happened
- Any error messages in console
- Screenshots if visual issues

### Example Failure Report
"Test #88 FAILED - Right-clicking slot 1 does nothing, settings GUI doesn't open. Console shows: [error message]. Expected settings GUI to open."

---

## Next Steps After Testing

**If all tests pass:**
- Mark Tests #88-91 as passing in feature_list.json
- Proceed to next feature implementation
- Recommended: Implement Delete Slot button functionality (Test #92)

**If any test fails:**
- Report specific failures
- Developer will fix issues
- Re-test after fixes

---

## Files Modified

**New Files:**
- `SlotSettingsGUI.java` - New GUI for slot settings menu

**Modified Files:**
- `SlotSelectionGUI.java` - Added right-click detection and handler

**Total Changes:**
- 1 new file created (267 lines)
- 1 file modified (30 lines added)
- 0 locale changes (all messages hardcoded in GUI)

---

## Technical Details

### GUI Layout (27 slots)

```
Row 1:  [  ] [  ] [  ] [  ] [PREVIEW] [  ] [  ] [  ] [  ]
Row 2:  [  ] [  ] [RENAME] [  ] [ICON] [  ] [DELETE] [  ] [  ]
Row 3:  [  ] [  ] [  ] [  ] [BACK] [  ] [  ] [  ] [  ]
```

- Slot 4: Preview (slot icon and info)
- Slot 11: Rename button (NAME_TAG)
- Slot 13: Change Icon button (ITEM_FRAME)
- Slot 15: Delete Slot button (TNT)
- Slot 22: Back button (ARROW)
- All other slots: Gray glass pane filler

### Click Types Handled
- **Left-click:** Normal action (switch/create) - existing behavior
- **Right-click:** Open settings GUI - new behavior
- **Shift-click:** Same as left-click
- **Middle-click:** Ignored

### Event Flow
1. Player opens slot selection GUI
2. Player right-clicks on a slot
3. Click type detected as RIGHT
4. `handleSlotRightClick()` called
5. Validates slot is not empty/locked
6. Creates and opens `SlotSettingsGUI`
7. Settings GUI shows slot info and buttons
8. Player clicks a button
9. GUI closes, command instructions shown
10. Player uses command to make changes

---

## Testing Time Estimates

- **Quick Test:** 5 minutes
- **All Core Tests (#88-91):** 15 minutes
- **All Tests + Edge Cases:** 25 minutes
- **Full Suite + Performance:** 35 minutes

**Recommended:** Start with Quick Test (5 min) to verify basic functionality, then run full suite if needed.

---

## Questions or Issues?

If you encounter any problems during testing:
1. Check console for error messages
2. Verify all dependencies are installed (BentoBox, BSkyBlock, FAWE)
3. Ensure player has at least 1 island created
4. Try restarting the server
5. Report exact steps to reproduce the issue

---

**Happy Testing! 🎮**
