# Session 23 Testing Guide - Grid Expansion Commands

## Build Information
- **Session:** 23
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **JAR Size:** 132K
- **Status:** ✅ Compilation successful

## Features Implemented

### Test #78: Admin command to expand grid east adds columns
**Command:** `/islandselector admin expand east <amount>`
**Purpose:** Expand the grid eastward (positive X direction) by adding new columns

### Test #79: Admin command to expand grid south adds rows
**Command:** `/islandselector admin expand south <amount>`
**Purpose:** Expand the grid southward (positive Z direction) by adding new rows

### Test #80: Grid expansion updates config.yml
**Purpose:** Verify that grid expansion persists across server restarts

## Quick Start (2 minutes)

```bash
# 1. Check current grid size
/islandselector admin version
# Note the grid size (should be 21x21 by default: -10 to 10)

# 2. Expand grid east by 5 columns
/islandselector admin expand east 5
# Expected: "Expanded grid east by 5 - New size: 26x21"

# 3. Expand grid south by 3 rows
/islandselector admin expand south 3
# Expected: "Expanded grid south by 3 - New size: 26x24"

# 4. Verify in GUI
/islandselector
# Scroll right - new columns K-O should be available (green glass)
# Scroll down - new rows should be available

# 5. Restart server and check persistence
# Stop and start server
/islandselector admin version
# Should show: Grid Size: 26x24 (persisted from config.yml)
```

If all checks pass, Tests #78-80 pass! ✅

---

## Detailed Testing

### Test #78: Expand Grid East

**Prerequisites:**
- Server running with IslandSelector installed
- Admin permissions (islandselector.admin.expand)

**Test Steps:**
1. Check current grid boundaries in config.yml
   - Default: min-x: -10, max-x: 10 (21 columns)
2. Execute command: `/islandselector admin expand east 5`
3. Verify success message shows correct new size
4. Open grid GUI: `/islandselector`
5. Scroll right to the easternmost edge
6. Verify new columns are available (green glass panes)
7. Try to claim a location in the new area
8. Verify island can be created in expanded area

**Expected Results:**
- ✅ Command succeeds with message: "Expanded grid east by 5 - New size: 26x21"
- ✅ Config.yml updated: max-x increases from 10 to 15
- ✅ GUI shows new columns as available (not locked)
- ✅ Can scroll to and view new columns
- ✅ Can claim islands in new columns
- ✅ Grid width calculation correct (old_max - old_min + 1 + amount)

**Edge Cases to Test:**
```bash
# Invalid direction
/islandselector admin expand northeast 5
# Expected: Error - "Invalid direction: northeast. Use: north, south, east, or west"

# Invalid amount (negative)
/islandselector admin expand east -5
# Expected: Error - "Invalid amount: -5. Must be a positive number."

# Invalid amount (not a number)
/islandselector admin expand east abc
# Expected: Error - "Invalid amount: abc. Must be a positive number."

# Zero amount
/islandselector admin expand east 0
# Expected: Error - "Invalid amount: 0. Must be a positive number."
```

---

### Test #79: Expand Grid South

**Prerequisites:**
- Same as Test #78

**Test Steps:**
1. Check current grid boundaries
   - Default: min-z: -10, max-z: 10 (21 rows)
2. Execute command: `/islandselector admin expand south 3`
3. Verify success message
4. Open grid GUI
5. Scroll down to southernmost edge
6. Verify new rows are available
7. Try to claim a location in the new rows

**Expected Results:**
- ✅ Command succeeds with message: "Expanded grid south by 3 - New size: [width]x24"
- ✅ Config.yml updated: max-z increases from 10 to 13
- ✅ GUI shows new rows as available
- ✅ Can scroll to and claim islands in new rows

**Additional Directions to Test:**
```bash
# Expand west (negative X direction)
/islandselector admin expand west 2
# Expected: min-x decreases from -10 to -12
# New size should show width increased by 2

# Expand north (negative Z direction)
/islandselector admin expand north 4
# Expected: min-z decreases from -10 to -14
# New size should show height increased by 4
```

---

### Test #80: Grid Expansion Persists to Config

**Prerequisites:**
- Complete Tests #78 and #79 first

**Test Steps:**
1. Expand grid in multiple directions:
   ```bash
   /islandselector admin expand east 5
   /islandselector admin expand south 3
   /islandselector admin expand west 2
   /islandselector admin expand north 4
   ```

2. Check config.yml file:
   - Navigate to `plugins/IslandSelector/config.yml`
   - Open the file and verify grid section:
   ```yaml
   grid:
     min-x: -12  # was -10, decreased by 2 (west expansion)
     max-x: 15   # was 10, increased by 5 (east expansion)
     min-z: -14  # was -10, decreased by 4 (north expansion)
     max-z: 13   # was 10, increased by 3 (south expansion)
   ```

3. Restart the server (full stop and start)

4. After restart, check grid size:
   ```bash
   /islandselector admin version
   # Should show: Grid Size: 28x28
   # Width: 15 - (-12) + 1 = 28
   # Height: 13 - (-14) + 1 = 28
   ```

5. Open GUI and verify boundaries:
   - Scroll to all edges (north, south, east, west)
   - Verify expanded areas are still available
   - Verify no locked areas in expanded regions

**Expected Results:**
- ✅ Config.yml is updated immediately after each expand command
- ✅ Grid boundaries persist after server restart
- ✅ GUI reflects expanded boundaries after restart
- ✅ Islands created in expanded areas survive restart
- ✅ No data loss or corruption

---

## Full Scenario Test

**Complete workflow to verify all three tests:**

```bash
# 1. Initial state check
/islandselector admin version
# Note: Grid Size: 21x21

# 2. Create island in original area (for reference)
/island
# Select location 0,0 and create island

# 3. Expand grid east
/islandselector admin expand east 5
# Verify: New size 26x21

# 4. Create island in new eastern area
/islandselector
# Scroll right, select location 12,0 (in expanded area)
# Create second player's island there

# 5. Expand grid south
/islandselector admin expand south 3
# Verify: New size 26x24

# 6. Create island in new southern area
# Select location 0,12 (in expanded area)

# 7. Check config.yml
# Verify: min-x: -10, max-x: 15, min-z: -10, max-z: 13

# 8. Restart server
stop
# Wait for shutdown, then start server

# 9. Verify persistence
/islandselector admin version
# Should still show: Grid Size: 26x24

# 10. Verify islands exist
/islandselector
# Both islands at 0,0 and 12,0 should still be there
# Southern expansion area should still be available

# 11. Verify can still claim in expanded areas
# Create another island in expanded region
```

**If all steps work:** Tests #78, #79, and #80 all PASS ✅

---

## Troubleshooting

### Expansion doesn't show in GUI
- Try closing and reopening the GUI
- Check console for errors
- Verify admin has permission: `islandselector.admin.expand`

### Config not saving
- Check file permissions on `plugins/IslandSelector/config.yml`
- Check console for save errors
- Verify disk space available

### Expansion not persisting after restart
- Verify config.yml was actually modified (check timestamps)
- Check for errors during server startup
- Verify config isn't being overwritten by another plugin

### New areas show as "locked"
- This would be a bug - expanded areas should be AVAILABLE
- Check GridManager.isWithinBounds() logic
- Report to developer with config.yml contents

---

## Permission Requirements

```yaml
islandselector.admin.expand:
  description: Allows expanding grid boundaries
  default: op
```

---

## Console Messages to Expect

```
[IslandSelector] Settings saved to config.yml
```

After restart:
```
[IslandSelector] IslandSelector enabled successfully!
[IslandSelector] Version: 1.0.0-SNAPSHOT
[IslandSelector] Grid Size: 26x24   # Updated values
[IslandSelector] Island Spacing: 400 blocks (from BSkyBlock)
```

---

## Success Criteria

**Test #78 PASSES if:**
- ✅ `/islandselector admin expand east <amount>` works
- ✅ Success message shows correct new grid size
- ✅ Config.yml max-x value increases
- ✅ GUI shows new eastern columns as available
- ✅ Can create islands in expanded eastern area

**Test #79 PASSES if:**
- ✅ `/islandselector admin expand south <amount>` works
- ✅ Also works for north, east, west directions
- ✅ Config.yml boundaries update correctly
- ✅ GUI shows new areas as available
- ✅ Error messages work for invalid input

**Test #80 PASSES if:**
- ✅ Grid expansion updates config.yml immediately
- ✅ Grid size persists after server restart
- ✅ Expanded areas remain available after restart
- ✅ Islands in expanded areas survive restart
- ✅ No data corruption or loss

---

## Testing Time Estimate

- **Quick smoke test:** 5 minutes
- **Full test all 3 features:** 15 minutes
- **Edge cases and persistence:** 25 minutes
- **Complete scenario test:** 35 minutes

---

## Regression Testing

After testing expansion, verify these still work:
- ✅ `/islandselector admin reserve <coord>` - Reserve locations
- ✅ `/islandselector admin setprice <coord> <price>` - Set prices
- ✅ `/islandselector admin info <coord>` - View location info
- ✅ `/islandselector admin version` - Shows correct grid size
- ✅ `/islandselector` - GUI opens and displays correctly
- ✅ Island creation in non-expanded areas
- ✅ Grid scrolling in all directions

---

## Reporting Results

**If ALL tests pass:**
Create feedback file: `TESTER_FEEDBACK_SESSION23.txt`
```
✅ Test #78 PASS - Expand east works perfectly
✅ Test #79 PASS - All directions (north/south/east/west) work
✅ Test #80 PASS - Config persistence works, survives restart

Grid expansion feature is production-ready!
```

**If ANY test fails:**
Create feedback file with:
- Which test failed (#78, #79, or #80)
- What command was run
- What went wrong (error message, unexpected behavior)
- Console errors (if any)
- Config.yml contents (the grid section)
- Steps to reproduce

---

## Notes for Human Tester

This is the **first dynamic grid feature** - previous admin commands only worked with static data.

Grid expansion:
- Changes the fundamental boundaries of the world
- Affects what players can see and claim
- Must persist correctly or players lose access

This is a **critical feature** for server growth:
- Start with small grid (10x10)
- Expand as player base grows
- No need to pre-allocate huge grid

Take your time testing the persistence - it's the most important part! 🎮
