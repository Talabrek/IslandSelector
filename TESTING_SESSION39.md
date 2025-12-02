# Testing Session 39 - Player Search Functionality

## Overview

**Session:** 39 (Discovery & Documentation)
**Feature:** Player Search in Grid GUI
**Status:** Implemented - Ready for Testing
**Tests Covered:** 4 search-related tests
**Estimated Testing Time:** 10-15 minutes
**Players Required:** 2-3 test accounts

---

## Pre-Test Setup

### Server Requirements
- BentoBox installed and working
- BSkyBlock addon installed and working
- IslandSelector addon: `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- At least 2 player accounts available

### Installation
1. Copy JAR to: `plugins/BentoBox/addons/IslandSelector-1.0.0-SNAPSHOT.jar`
2. Restart server or reload BentoBox
3. Verify load: `/bentobox version` shows IslandSelector

### Test Account Setup
You'll need:
- **Player A** ("TestPlayer") - Creates an island first
- **Player B** ("Searcher") - Uses search to find Player A
- **Player C** (optional) - For additional test cases

---

## Test Suite

### TEST #1: Search Button Display

**Category:** UI
**Priority:** High
**Files:** MainGridGUI.java

#### Test Steps

1. **Login as any player**
   ```
   Join Minecraft server
   ```

2. **Open grid GUI**
   ```
   /islandselector
   ```
   - Expected: 6-row chest GUI opens

3. **Locate search button**
   - Look at bottom row (row 6)
   - Expected: COMPASS item at slot 47 (bottom row, left side)

4. **Verify button appearance**
   - Material: Should be COMPASS item
   - Name: Should show "Search Player" (yellow/gold color)
   - Lore: Should show "Find a player's island" (gray color)

5. **Hover over compass**
   - Tooltip should be readable
   - No strange characters or formatting issues

#### Expected Results
✅ COMPASS appears at slot 47
✅ Display name: "Search Player" (yellow)
✅ Lore: "Find a player's island" (gray)
✅ Button is clickable (not a barrier or air)
✅ Consistent with other bottom row buttons

#### Pass/Fail Criteria
- **PASS:** Compass visible with correct name and lore
- **FAIL:** Wrong item, wrong name, wrong lore, or button missing

---

### TEST #2: Search Finds Player and Centers Grid

**Category:** Functional
**Priority:** Critical
**Files:** SearchListener.java, MainGridGUI.java

#### Pre-Test Setup

1. **Login as Player A** ("TestPlayer")
   ```
   Username: TestPlayer
   ```

2. **Create island** (if not already created)
   ```
   /island
   ```
   - Select a location from grid (e.g., B3)
   - Confirm and wait for island to be created

3. **Note island coordinates**
   ```
   /islandselector admin info B3
   ```
   - Record the grid coordinates (e.g., "B3")

4. **Login as Player B** ("Searcher")
   ```
   Username: Searcher (or any other player)
   ```

#### Test Steps

1. **Open grid GUI as Player B**
   ```
   /islandselector
   ```

2. **Note current viewport center**
   - Check which grid coordinates are visible
   - Grid might be centered on 0,0 initially

3. **Click search button**
   - Click the COMPASS item (slot 47)
   - Expected: GUI closes immediately

4. **Verify search instructions appear**
   - Check chat for messages:
     ```
     === Island Search ===
     Type a player name to search for their island.
     Type 'cancel' to cancel the search.
     Partial names are supported (e.g., 'Not' finds 'Notch')
     ```

5. **Type player name in chat**
   ```
   TestPlayer
   ```
   - Type EXACTLY: TestPlayer (case doesn't matter)
   - Press Enter

6. **Verify search input is not broadcast**
   - **As Player A:** Check chat - should NOT see "TestPlayer" message
   - Search input should be private

7. **Verify success message**
   - Player B should see:
     ```
     Found TestPlayer's island at B3!
     Centering view on their island...
     ```
   - (Coordinates will match where TestPlayer's island is)

8. **Verify GUI reopens**
   - Grid GUI should open automatically
   - Same GUI, not a new one

9. **Verify viewport centered**
   - Check if TestPlayer's island (player head) is visible in the 7x4 grid view
   - Viewport should be centered on or near TestPlayer's island coordinates

10. **Verify island is visible**
    - Look for player head item with "TestPlayer" name
    - Should be in the viewable area (28 center slots)

#### Expected Results
✅ Compass click closes GUI
✅ Search instructions appear in chat (4 lines)
✅ Chat input "TestPlayer" is NOT broadcast to other players
✅ Success message shows island coordinates
✅ GUI reopens automatically
✅ Viewport centered on TestPlayer's island
✅ TestPlayer's player head visible in grid
✅ No console errors

#### Pass/Fail Criteria
- **PASS:** All steps work, viewport centers, island visible
- **FAIL:** GUI doesn't close, no instructions, search doesn't find player, viewport doesn't change, errors in console

---

### TEST #3: Partial Name Matching

**Category:** Functional
**Priority:** High
**Files:** SearchListener.java

#### Pre-Test Setup

1. **Login as Player A** with a distinctive name
   ```
   Username: Notch (or NotchLike, NotchTheBuilder, etc.)
   ```
   - Any name starting with "Not" works

2. **Create island** (if not already created)
   ```
   /island
   ```
   - Select any location and create island

3. **Login as Player B** ("Searcher")

#### Test Steps

1. **Open grid GUI as Player B**
   ```
   /islandselector
   ```

2. **Click search button**
   - Click COMPASS (slot 47)
   - GUI closes, instructions appear

3. **Type partial name**
   ```
   Not
   ```
   - Type only "Not" (first 3 characters)
   - Press Enter

4. **Verify partial match works**
   - Should find "Notch" (or similar name starting with "Not")
   - Success message: "Found Notch's island at <coord>!"

5. **Verify viewport centered**
   - Grid should center on Notch's island
   - Notch's player head should be visible

#### Additional Partial Match Tests

**Test Case A: Case Insensitive**
```
Search: not (lowercase)
Expected: Finds "Notch"
```

**Test Case B: Capital Letters**
```
Search: NOT (uppercase)
Expected: Finds "Notch"
```

**Test Case C: Mixed Case**
```
Search: NoT (mixed)
Expected: Finds "Notch"
```

**Test Case D: Single Character**
```
Search: N
Expected: Finds first player starting with "N"
```

#### Expected Results
✅ "Not" finds "Notch" (or similar name)
✅ Case insensitive matching works
✅ Viewport centers on found island
✅ Success message shows correct player name
✅ Works for both online and offline players

#### Pass/Fail Criteria
- **PASS:** Partial name matches and finds player
- **FAIL:** Only exact match works, case sensitive, doesn't find player

---

### TEST #4: Player Not Found Error

**Category:** Functional
**Priority:** High
**Files:** SearchListener.java

#### Test Steps

1. **Open grid GUI**
   ```
   /islandselector
   ```

2. **Click search button**
   - Click COMPASS (slot 47)

3. **Type non-existent player name**
   ```
   NonexistentPlayer999
   ```
   - Use a name that definitely doesn't exist
   - Press Enter

4. **Verify error message**
   - Expected message:
     ```
     Player 'NonexistentPlayer999' not found or has no island.
     Make sure the player name is spelled correctly.
     ```
   - Red/gray colored text

5. **Verify GUI reopens**
   - Grid GUI should reopen automatically
   - Viewport should NOT change (stays where it was)

6. **Test: Player exists but no island**
   - Have a player join but DON'T create island
   - Search for that player's name
   - Expected: "Player <name> doesn't have an island."
   - Different error message than "not found"

#### Expected Results
✅ Error message for non-existent player
✅ Error message for player with no island (different message)
✅ Grid GUI reopens after error
✅ Viewport doesn't change
✅ No console errors

#### Pass/Fail Criteria
- **PASS:** Appropriate error messages shown, GUI reopens
- **FAIL:** No error message, crash, console errors, GUI doesn't reopen

---

## Additional Test Cases

### TEST #5: Cancel Search

**Steps:**
1. Open grid GUI
2. Click search button
3. Type: `cancel`
4. Expected: "Search cancelled." message
5. Expected: Grid GUI reopens
6. Expected: Viewport unchanged

---

### TEST #6: Search Offline Player

**Setup:**
1. Player "OfflineTest" creates island
2. Player "OfflineTest" disconnects from server

**Steps:**
1. Another player opens grid and searches
2. Type: `OfflineTest`
3. Expected: Finds island by scanning database
4. Expected: Success message and viewport centers

**Note:** This tests database lookup for offline players

---

### TEST #7: Multiple Search Sessions

**Steps:**
1. Player A opens grid, clicks search
2. Player B opens grid, clicks search
3. Both players type names at same time
4. Expected: Each search independent
5. Expected: No cross-talk between sessions

---

### TEST #8: Search Input Privacy

**Setup:**
1. Player A opens search
2. Player B watching chat

**Steps:**
1. Player A types: `SecretSearchTerm`
2. Player B should NOT see "SecretSearchTerm" in chat
3. Verify chat event is cancelled

---

## Edge Cases & Error Conditions

### Edge Case: Empty Input
- Type: ` ` (just spaces)
- Expected: Not found error (or handle gracefully)

### Edge Case: Special Characters
- Type: `Player@#$%`
- Expected: Not found error (no crash)

### Edge Case: Very Long Name
- Type: (paste 100+ character string)
- Expected: Not found error (no crash or buffer overflow)

### Edge Case: GUI Closed During Search
- Click search, close game before typing
- Expected: Search session cleaned up on disconnect

---

## Performance Tests

### Test: Search Speed
- Click search, type name, measure time to GUI reopen
- Expected: < 1 second for online players
- Expected: < 2 seconds for offline players

### Test: Multiple Islands
- Have 10+ islands on grid
- Search for specific player
- Expected: Finds correct player among many

---

## Console Monitoring

### Watch For These Errors

**While testing, monitor console for:**

```
[ERROR] SearchListener
[ERROR] NullPointerException
[ERROR] ConcurrentModificationException
[ERROR] IllegalStateException: Asynchronous
[WARN] Task ... generated an exception
```

**Expected console output (normal):**
```
(No errors - search should be silent in console)
```

---

## Debugging Failed Tests

### If Search Button Not Visible

**Check:**
1. Correct GUI? (main grid, not slots)
2. Slot 47 specifically
3. Console for GUI creation errors

**Debug:**
```
/islandselector admin debug
(If debug command exists)
```

---

### If Search Doesn't Find Player

**Check:**
1. Player name spelling (exact)
2. Player actually has island
3. Island registered in GridManager

**Debug:**
```java
// Check console during search for debug messages
// SearchListener should log search attempts
```

---

### If Chat Broadcasts Search Input

**Check:**
1. SearchListener registered?
2. Event priority correct? (LOWEST)
3. Event handler enabled?

**Look for:**
```
Caused by: SearchListener not registered
```

---

## Test Data Recording

### Test Results Table

| Test # | Description | Status | Notes |
|--------|-------------|--------|-------|
| 1 | Search button displays | ⬜ | Compass at slot 47 |
| 2 | Search finds player | ⬜ | Viewport centers |
| 3 | Partial name matching | ⬜ | "Not" finds "Notch" |
| 4 | Player not found error | ⬜ | Error message shown |
| 5 | Cancel search | ⬜ | Returns to GUI |
| 6 | Offline player search | ⬜ | Database lookup |
| 7 | Multiple sessions | ⬜ | No cross-talk |
| 8 | Input privacy | ⬜ | Chat not broadcast |

✅ = Pass | ❌ = Fail | ⬜ = Not Tested | ⚠️ = Partial

---

## Success Metrics

### All Tests Pass If:
- ✅ 4/4 core tests pass
- ✅ No console errors
- ✅ All edge cases handled gracefully
- ✅ Performance acceptable (< 2 seconds)

### Partial Success If:
- ⚠️ 3/4 core tests pass
- ⚠️ Minor visual issues (formatting)
- ⚠️ Edge cases have issues but core works

### Failure If:
- ❌ 2 or more core tests fail
- ❌ Console errors or exceptions
- ❌ Search doesn't work at all
- ❌ Chat broadcasts search input (privacy issue!)

---

## Post-Test Checklist

After completing all tests:

- [ ] All test results recorded
- [ ] Console errors documented (if any)
- [ ] Screenshots taken (if issues found)
- [ ] feature_list.json updated (if tests pass)
- [ ] Report submitted to development team

---

## Report Template

```markdown
# Session 39 Test Report - Player Search

**Tester:** [Your Name]
**Date:** [Date]
**Server:** [Server Details]
**Players Used:** [Player Names]

## Core Tests

### Test #1: Search Button Display
Status: [PASS/FAIL]
Notes: [Details]

### Test #2: Search Finds Player
Status: [PASS/FAIL]
Notes: [Details]

### Test #3: Partial Name Matching
Status: [PASS/FAIL]
Notes: [Details]

### Test #4: Player Not Found Error
Status: [PASS/FAIL]
Notes: [Details]

## Additional Tests

- Cancel search: [PASS/FAIL]
- Offline player: [PASS/FAIL]
- Input privacy: [PASS/FAIL]

## Issues Found

1. [Issue description]
   - Steps to reproduce
   - Expected vs Actual
   - Console errors (if any)

2. [Issue description]
   ...

## Console Errors

```
[Paste any errors here]
```

## Overall Assessment

**Pass/Fail:** [PASS/FAIL]
**Confidence:** [High/Medium/Low]
**Recommendations:** [Any suggestions]

## Screenshots

[Attach screenshots if needed]
```

---

## Next Steps After Testing

### If All Tests Pass:
1. Mark tests as `"passes": true` in feature_list.json
2. Update test count: ~48/225 passing
3. Move to next feature block (island claiming tests)

### If Any Tests Fail:
1. Document exact failure conditions
2. Provide console errors
3. Agent will debug and fix
4. Re-test after fix is deployed

---

## Technical Implementation Details

For debugging purposes, here's how search works:

### Flow Diagram
```
Player clicks COMPASS
  ↓
GridGUIListener.onInventoryClick() (line 82)
  ↓
player.closeInventory()
  ↓
searchListener.startSearch(player, gui)
  ↓
SearchListener creates SearchSession
  ↓
Instructions sent to player chat
  ↓
Player types name in chat
  ↓
AsyncPlayerChatEvent triggered
  ↓
SearchListener.onPlayerChat() (line 51)
  ↓
event.setCancelled(true) - no broadcast
  ↓
SearchSession removed from map
  ↓
Async → Sync task conversion
  ↓
performSearch() on main thread
  ↓
Try exact match: Bukkit.getPlayerExact()
  ↓
If null, try partial: online players startsWith()
  ↓
If null, try offline: GridManager.getAllLocations()
  ↓
If found: gui.centerViewportOn(coord)
  ↓
gui.refresh() + player.openInventory()
  ↓
Success message or error message
```

### Key Classes
- `SearchListener.java` - Main search logic
- `GridGUIListener.java` - Button click handler
- `MainGridGUI.java` - GUI creation and viewport
- `GridManager.java` - Location database

### Key Methods
- `searchListener.startSearch()` - Start search session
- `searchListener.onPlayerChat()` - Handle chat input
- `performSearch()` - Execute search logic
- `findIslandByPartialName()` - Offline player search
- `gui.centerViewportOn()` - Move viewport

---

## Testing Environment

**Recommended Setup:**
- Minecraft 1.20.1+ (or compatible version)
- BentoBox latest
- BSkyBlock latest
- IslandSelector 1.0.0-SNAPSHOT (Session 39)

**Optional Plugins:**
- None required for search functionality

**Test World:**
- Fresh world recommended
- At least 2-3 islands created
- Mix of online and offline players

---

**Happy Testing!** 🔍

**Estimated Time:** 10-15 minutes for core tests
**Difficulty:** Easy - Simple click and type
**Players:** 2-3 recommended

Good luck! Report any issues you find.
