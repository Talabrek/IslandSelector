# Session 51: Neighbors Command

## Quick Summary
**Feature:** `/islandselector neighbors` command to open the Neighborhood GUI
**Time Estimate:** 5-10 minutes
**Players Required:** 2-3 accounts (to have neighbors)
**Complexity:** Simple
**Dependencies:** Players must have islands created already

## What Was Implemented

### New Command: `/islandselector neighbors`
A new command that opens the Neighborhood GUI directly, without needing to go through the grid GUI first.

**Aliases:**
- `/islandselector neighbors`
- `/islandselector neighbourhood`
- `/islandselector neighborhood`
- `/is neighbors` (using the short alias)

**Features:**
- Opens the 3x3 neighborhood view GUI
- Shows your island in the center with a glow effect
- Shows all 8 adjacent positions (NW, N, NE, W, E, SW, S, SE)
- Online neighbors display with player heads
- Offline neighbors show with red glass
- Empty adjacent locations show with green glass
- Right-click neighbors to warp to their island (if they have warps enabled)
- Displays Slots button for quick access to slot selection

**Error Handling:**
- Shows friendly message if player doesn't have an island yet
- Suggests using `/island create` to create an island first

## Test Cases

### Test #1: Basic Command Opens Neighborhood GUI
**Description:** Verify the command opens the correct GUI

**Setup:**
- Player has an island

**Steps:**
1. Run `/islandselector neighbors`
2. Verify a GUI opens with title "Your Neighborhood"
3. Verify it has 3 rows (27 slots)
4. Verify your island is shown in the center with a glow effect
5. Verify 8 surrounding slots show neighbor positions

**Expected Result:**
- ✅ GUI opens successfully
- ✅ Shows 3x3 neighborhood layout
- ✅ Center shows your island with glow

**Pass Criteria:** ✅ Command opens correct GUI

---

### Test #2: Command Alias Works
**Description:** Verify aliases work

**Steps:**
1. Try `/is neighbors`
2. Try `/islandselector neighbourhood` (British spelling)
3. Try `/islandselector neighborhood` (American spelling)

**Expected Result:**
- ✅ All aliases open the same GUI

**Pass Criteria:** ✅ All aliases function correctly

---

### Test #3: Player Without Island Gets Error
**Description:** Verify proper error message for new players

**Setup:**
- New player with no island

**Steps:**
1. Run `/islandselector neighbors`
2. Verify error message appears in chat

**Expected:**
```
You don't have an island yet!
Create an island first with /island create
```

**Pass Criteria:** ✅ Friendly error message shown

---

### Test #4: Shows Neighbors Correctly
**Description:** Verify neighbors are displayed properly

**Setup:**
- Player A has island at location 0,0
- Player B has island at location 1,0 (east of A)
- Player B is online

**Steps:**
1. As Player A, run `/islandselector neighbors`
2. Verify east position (right of center) shows Player B's head
3. Hover over Player B's head
4. Verify tooltip shows Player B's name

**Expected Result:**
- ✅ Online neighbor shown as player head
- ✅ Tooltip shows neighbor information

**Pass Criteria:** ✅ Neighbors displayed correctly

---

### Test #5: Shows Empty Positions
**Description:** Verify empty adjacent positions are indicated

**Setup:**
- Player has island at 5,5 (middle of grid)
- No other players have islands adjacent

**Steps:**
1. Run `/islandselector neighbors`
2. Verify all 8 surrounding positions show green glass

**Expected Result:**
- ✅ Empty positions shown as green stained glass
- ✅ Tooltip indicates location is available

**Pass Criteria:** ✅ Empty positions clearly visible

---

### Test #6: Tab Completion Shows Command
**Description:** Verify the command appears in tab completion

**Steps:**
1. Type `/islandselector ` and press TAB
2. Look for "neighbors" in the suggestions

**Expected Result:**
- ✅ "neighbors" appears in tab completion list

**Pass Criteria:** ✅ Tab completion works

---

## Related Tests from feature_list.json

This command enables testing of these existing tests:
- **Test line 1630:** "Neighborhood GUI opens with 27 slots and 3x3 layout"
- **Test line 1641:** "Neighborhood GUI shows 8 surrounding neighbor positions"
- **Test line 1652:** "Neighborhood GUI shows online neighbor with player head"
- **Test line 1664:** "Neighborhood GUI shows offline neighbor with red glass"
- **Test line 1675:** "Neighborhood GUI shows empty adjacent locations with green glass"

All these tests required the `/islandselector neighbors` command to be available.

## Files Created/Modified

**Created:**
- `src/main/java/world/bentobox/islandselector/commands/NeighborsCommand.java` (NEW)

**Modified:**
- `src/main/java/world/bentobox/islandselector/commands/IslandSelectorCommand.java` (added NeighborsCommand)

## Build Information
- **JAR Size:** 216K (up from 212K)
- **Status:** BUILD SUCCESS
- **Source Files:** 53

## Test Report Template

```
Session 51: Neighbors Command Tests
Date: ____________
Tester: ____________

Test #1 - Basic Command Opens GUI: [PASS/FAIL]
Notes:

Test #2 - Command Aliases Work: [PASS/FAIL]
Notes:

Test #3 - Error for No Island: [PASS/FAIL]
Notes:

Test #4 - Shows Neighbors Correctly: [PASS/FAIL]
Notes:

Test #5 - Shows Empty Positions: [PASS/FAIL]
Notes:

Test #6 - Tab Completion: [PASS/FAIL]
Notes:

Overall: [PASS/FAIL]
Issues Found:

```
