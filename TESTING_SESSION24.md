# Session 24 Testing Guide - Grid Shrink Commands

## Overview
Session 24 implemented Tests #81-82: Admin commands to shrink the grid boundaries, with validation to prevent removing areas containing islands.

**Feature:** Grid shrinking allows servers to reduce their grid size when sections are no longer needed, but protects existing islands from being removed.

## Build Information
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **JAR Size:** 136K (+4K from Session 23)
- **Source Files:** 31 (+1 new: AdminShrinkCommand.java)
- **Compilation:** ✅ SUCCESS (0 errors, 1 non-critical warning)

## Quick Start (5 Minutes)

### Prerequisites
1. Complete Session 23 testing first (grid expansion)
2. Server with BentoBox, BSkyBlock, and FAWE installed
3. Fresh grid or test grid with known state

### Quick Smoke Test

```bash
# Step 1: Check current grid size
/islandselector admin version
# Expected: Shows current grid boundaries (e.g., 26x24 if Session 23 was tested)

# Step 2: Ensure no islands in eastern columns
# Create a test island at 0,0 (center)
/island create
# Select location 0,0 from GUI

# Step 3: Try shrinking from east (safe - no islands there)
/islandselector admin shrink east 5
# Expected: "Shrunk grid east by 5 - New size: 21x24"

# Step 4: Verify in config
# Check plugins/IslandSelector/config.yml
# Look for grid: max-x: (should have decreased by 5)

# Step 5: Test validation - try to shrink area with island
/islandselector admin shrink west 15
# Expected: ERROR: "Cannot shrink: 1 island(s) exist in the area that would be removed!"

# Step 6: Verify persistence
stop
# Restart server

/islandselector admin version
# Expected: Grid size matches what you set (persisted!)
```

If all steps work as expected, Tests #81 and #82 pass! ✅

## Detailed Test Cases

### Test #81: Admin command to shrink grid removes empty areas

**Command:** `/islandselector admin shrink <direction> <amount>`

**Test Scenario 1: Shrink from East (Columns)**
```bash
# Setup: Grid is 20x20, no islands in columns P-T (16-20)
/islandselector admin version
# Note current grid size

# Execute shrink
/islandselector admin shrink east 5

# Expected Results:
✅ Success message: "Shrunk grid east by 5 - New size: 15x20"
✅ Config.yml max-x decreased by 5
✅ GUI shows columns P-T are now locked (black glass)
✅ Cannot scroll to removed columns
```

**Test Scenario 2: Shrink from South (Rows)**
```bash
# Setup: Grid is 20x20, no islands in rows 16-20
/islandselector admin shrink south 5

# Expected Results:
✅ Success message: "Shrunk grid south by 5 - New size: 15x15"
✅ Config.yml max-z decreased by 5
✅ GUI shows rows 16-20 are now locked (black glass)
```

**Test Scenario 3: Shrink from West (Negative Columns)**
```bash
# Setup: Grid is -10 to 10, no islands at columns -10 to -6
/islandselector admin shrink west 5

# Expected Results:
✅ Success message: "Shrunk grid west by 5 - New size: 16x15"
✅ Config.yml min-x increased by 5 (from -10 to -5)
✅ Western columns locked out
```

**Test Scenario 4: Shrink from North (Negative Rows)**
```bash
# Setup: Grid is -10 to 10, no islands at rows -10 to -6
/islandselector admin shrink north 5

# Expected Results:
✅ Success message: "Shrunk grid north by 5 - New size: 16x10"
✅ Config.yml min-z increased by 5
✅ Northern rows locked out
```

### Test #82: Cannot shrink grid if islands exist in affected area

**Test Scenario 1: Island in shrink area**
```bash
# Setup: Player has island at column S (column 18)
/island create
# Select location S,5 from GUI

# Try to shrink removing column S
/islandselector admin shrink east 5

# Expected Results:
❌ Error message: "Cannot shrink: 1 island(s) exist in the area that would be removed!"
✅ Grid size unchanged
✅ config.yml unchanged
✅ Island still exists and accessible
```

**Test Scenario 2: Multiple islands in shrink area**
```bash
# Setup: 3 different players have islands at columns Q, R, S
# (Use /islandselector admin info Q,1 to check occupancy)

# Try to shrink removing those columns
/islandselector admin shrink east 8

# Expected Results:
❌ Error: "Cannot shrink: 3 island(s) exist in the area that would be removed!"
✅ Shows count of affected islands
✅ Shrink blocked
```

**Test Scenario 3: Shrink succeeds when islands are cleared**
```bash
# Setup: Same as above - islands at Q, R, S

# Delete those islands
/island delete (as each player, or as admin)

# Now try shrink again
/islandselector admin shrink east 8

# Expected Results:
✅ Success message: Shrink completes
✅ Previously occupied area now locked
```

## Edge Cases and Error Handling

### Test: Invalid Direction
```bash
/islandselector admin shrink northeast 5
# Expected: "Invalid direction: northeast. Use: north, south, east, or west"
```

### Test: Invalid Amount (Negative)
```bash
/islandselector admin shrink east -5
# Expected: "Invalid amount: -5. Must be a positive number."
```

### Test: Invalid Amount (Not a Number)
```bash
/islandselector admin shrink east abc
# Expected: "Invalid amount: abc. Must be a positive number."
```

### Test: Shrink Too Much (Would Invert Grid)
```bash
# Setup: Grid is 10x10 (min-x: -5, max-x: 5)
/islandselector admin shrink east 12

# Expected: "Cannot shrink: The grid would become too small or inverted!"
# (Because max-x would become -7, which is less than min-x of -5)
```

### Test: Missing Arguments
```bash
/islandselector admin shrink
# Expected: Shows help/usage message with parameters
```

### Test: Shrink by Zero
```bash
/islandselector admin shrink east 0
# Expected: "Invalid amount: 0. Must be a positive number."
```

## Config Persistence Testing

### Test: Changes Persist Across Restart
```bash
# Step 1: Note original grid size
/islandselector admin version
# Example: Grid Size: 21x21

# Step 2: Shrink grid
/islandselector admin shrink east 3
/islandselector admin shrink south 2
# New size: 18x19

# Step 3: Check config file
# Open plugins/IslandSelector/config.yml
# Verify max-x and max-z decreased

# Step 4: Restart server
stop
# Start server again

# Step 5: Verify persistence
/islandselector admin version
# Expected: Grid Size: 18x19 (matches pre-restart)

# Step 6: Open GUI
/islandselector
# Verify locked areas still locked
```

## Integration Testing

### Test: Shrink + Expand Cycle
```bash
# Expand grid
/islandselector admin expand east 10
# Size increases

# Shrink grid back
/islandselector admin shrink east 10
# Size returns to original

# Verify config values match original
```

### Test: Shrink with Reserved Locations
```bash
# Reserve a location in area to be shrunk
/islandselector admin reserve 15,15
/islandselector admin setprice 15,15 10000

# Try to shrink that area (no island, but is reserved)
/islandselector admin shrink east 5

# Expected: ✅ Shrink succeeds (reservations don't block)
# Note: Reservation data may remain but location is locked
```

## Regression Testing

After testing shrink, verify these still work:

**Expand Commands (Session 23)**
```bash
✅ /islandselector admin expand east 5
✅ /islandselector admin expand south 3
✅ Expansion persists
```

**Core Commands**
```bash
✅ /islandselector - GUI opens
✅ /islandselector admin version - Shows correct grid size
✅ /islandselector admin info 0,0 - Shows location info
✅ Grid scrolling works
✅ Island creation works in valid areas
✅ Locked areas show black glass
```

## Expected Behavior Summary

### Shrink Command Should:
1. ✅ Reduce grid boundaries in specified direction
2. ✅ Update config.yml immediately
3. ✅ Persist changes across restart
4. ✅ Block shrink if islands exist in affected area
5. ✅ Show count of islands preventing shrink
6. ✅ Prevent grid inversion (min > max)
7. ✅ Work in all 4 directions (north/south/east/west)
8. ✅ Handle errors gracefully with clear messages
9. ✅ Make shrunk areas appear as locked (black glass)
10. ✅ Prevent claiming in locked areas

### Shrink Command Should NOT:
1. ❌ Delete existing islands
2. ❌ Corrupt config.yml
3. ❌ Allow negative grid sizes
4. ❌ Allow shrinking below 1x1 minimum
5. ❌ Cause server crashes
6. ❌ Lose data on restart

## Troubleshooting

### Issue: "Cannot find symbol" errors during build
- **Cause:** Import path incorrect
- **Fix:** Verify GridLocation is imported from `models`, not `database`

### Issue: Shrink not persisting across restart
- **Cause:** saveSettings() not called or config.yml locked
- **Fix:** Check file permissions on config.yml
- **Verify:** Look for "Saving settings..." in console

### Issue: GUI still shows shrunk area as available
- **Cause:** GridManager not checking updated boundaries
- **Fix:** Verify GridManager.isWithinBounds() uses Settings values
- **Workaround:** /islandselector admin reload

### Issue: Error says islands exist but area looks empty
- **Cause:** Database has stale data
- **Solution:** Use /islandselector admin info <coord> to verify
- **Fix:** May need database cleanup

## Performance Notes

- Shrink operation is instant (no world modification)
- Only updates config file and boundaries
- No server lag expected
- Works with grids of any size

## Testing Time Estimates

- **Quick smoke test:** 5 minutes
- **All test scenarios:** 15 minutes
- **Edge cases:** 10 minutes
- **Full regression:** 15 minutes
- **Total comprehensive test:** ~45 minutes

## Success Criteria

**Test #81 PASSES if:**
- Shrink command works in all 4 directions
- Grid boundaries update correctly
- Config.yml persists changes
- Locked areas show as black glass in GUI

**Test #82 PASSES if:**
- Shrink blocked when islands exist in area
- Error message shows island count
- Grid unchanged when shrink fails
- Shrink succeeds after islands removed

## Reporting Results

**If ALL tests pass:**
Report:
```
✅ Test #81 PASS - Grid shrink command works in all directions
✅ Test #82 PASS - Shrink validation prevents removing islands
```

**If ANY test fails:**
Create `TESTER_FEEDBACK_SESSION24.txt` with:
- Test number that failed (#81 or #82)
- Command executed
- Expected behavior
- Actual behavior
- Error messages (if any)
- Console logs (if errors)
- Config.yml grid section contents
- Steps to reproduce

## Next Session Preview

If Tests #81-82 pass, Session 25 will likely implement:
- Admin force relocate command (Test #85)
- Admin info command improvements (Test #86)
- Additional admin tools
- Or next highest priority from feature_list.json

## Technical Notes

**Grid Coordinate System:**
- Center: (0,0)
- East = positive X (max-x)
- West = negative X (min-x)
- South = positive Z (max-z)
- North = negative Z (min-z)

**Shrinking Directions:**
- East: Decreases max-x (removes rightmost columns)
- West: Increases min-x (removes leftmost columns)
- South: Decreases max-z (removes bottom rows)
- North: Increases min-z (removes top rows)

**Validation Logic:**
```java
// For each occupied island:
// If shrinking east and island.x > new_max_x: BLOCK
// If shrinking west and island.x < new_min_x: BLOCK
// If shrinking south and island.z > new_max_z: BLOCK
// If shrinking north and island.z < new_min_z: BLOCK
```

---

**Thank you for testing!** 🎮

Grid shrinking complements expansion (Session 23) to provide complete grid boundary management for server admins.
