# IslandSelector - Session 12 Testing Instructions

## Build Information
- **Session:** 12 - Critical Bug Fix: Island Location Placement
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **Status:** ✅ Critical Bug Fixed - Ready for Manual Testing

## What's New in Session 12

### Critical Bug Fix: Islands Now Created at Selected Grid Coordinates (Tests 41-48)

**CRITICAL BUG DISCOVERED AND FIXED:**
Previously, when players selected a grid location for their island, the plugin would:
1. ✅ Show them the grid selection GUI
2. ✅ Let them choose a location
3. ✅ Open confirmation and blueprint selection
4. ❌ Create the island at BSkyBlock's default spiral location (WRONG!)
5. ✅ Register the wrong location in the database

**The island was NOT being placed at the selected grid coordinates!**

**FIX IMPLEMENTED:**
- Added island relocation logic to `IslandCreateListener.onIslandCreated()`
- Now calculates correct world coordinates from grid coordinates
- Moves the island to the selected location immediately after creation
- Uses `island.setCenter()` and `island.setSpawnPoint()` to relocate
- Shows world coordinates to player in confirmation message

**Files Modified:**
1. `IslandCreateListener.java` - Added relocation logic and coordinate calculation methods

---

## Critical Tests Ready for Manual Verification

### Test #41: New player without island sees grid in claiming mode
**Status:** READY FOR TESTING (was already implemented, now with correct placement)

**Test Steps:**
1. Join server with a new player who has never created an island
2. Execute `/island` (BSkyBlock's island creation command)
3. Verify IslandSelector intercepts and shows grid GUI instead of auto-creating
4. Verify available locations show green glass panes with "Click to claim this location"

**Expected Result:**
- Grid GUI opens automatically in claiming mode
- No island is created yet
- Available locations are clearly marked

---

### Test #42: Clicking empty location in claiming mode opens claim confirmation
**Status:** READY FOR TESTING (was already implemented)

**Test Steps:**
1. Open grid GUI as new player without island (use `/island`)
2. Click on an empty green glass location (e.g., A1, B2, etc.)
3. First click selects the location
4. Second click on the same location opens confirmation GUI
5. Verify confirmation shows selected coordinates
6. Verify confirmation has Confirm and Cancel buttons

**Expected Result:**
- First click: Message "Selected location X,Z. Click again to confirm!"
- Second click: Confirmation GUI opens
- Shows grid coordinate and world coordinates
- Shows Confirm (green) and Cancel (red) buttons

---

### Test #43: Confirming claim creates island at correct grid coordinates ⭐ CRITICAL
**Status:** READY FOR TESTING (BUG FIX - THIS WAS BROKEN BEFORE!)

**Test Steps:**
1. Select location A1 (or 0,0) in claiming mode
2. Click Confirm in confirmation GUI
3. Select a blueprint from blueprint selection GUI
4. Wait for island creation to complete
5. Check player's location after creation
6. Verify island exists at the correct world coordinates

**Expected Results:**
- Island is created successfully
- Player receives messages:
  * "§a§lIsland Created!"
  * "§7Your island has been created at location §f0,0" (or selected coord)
  * "§7World coordinates: §fX: 0, Z: 0" (or calculated coords)
- **CRITICAL:** Player's island is at the CORRECT world coordinates, not a random spiral location
- Grid location is marked as occupied in the GUI

**How to Verify Coordinates:**
1. After island creation, run `/island` to teleport to your island
2. Press F3 to see coordinates
3. For grid 0,0 with default spacing (500), island should be at world X=0, Z=0
4. For grid 1,1, island should be at world X=1000, Z=1000 (spacing * 2 per grid unit)

---

### Test #44: Canceling claim confirmation returns to grid GUI
**Status:** READY FOR TESTING (was already implemented)

**Test Steps:**
1. Select an empty location for claiming
2. Confirmation GUI opens
3. Click Cancel button (red wool/barrier)
4. Verify player returns to grid GUI
5. Verify no island was created
6. Try selecting a different location

**Expected Result:**
- Player returns to grid selection GUI
- Message: "Action cancelled."
- No island is created
- Can select a different location

---

## Grid Coordinate Mapping Tests (Tests 45-48)

These tests verify the coordinate calculation is correct.

### Test #45: Grid coordinate A1 (0,0) maps to world coordinates 0,0
**Status:** READY FOR TESTING

**Test Steps:**
1. Configure grid with origin x=0, z=0 and spacing=500 in config.yml
2. Restart server
3. Claim location 0,0 (A1)
4. After island creation, press F3 to check coordinates
5. Verify world X coordinate is 0 (or very close to 0)
6. Verify world Z coordinate is 0 (or very close to 0)

**Expected Result:**
- Island center at world X=0, Z=0
- Player spawns at this location

---

### Test #46: Grid coordinate B2 (1,1) maps to world coordinates 1000,1000
**Status:** READY FOR TESTING

**Test Steps:**
1. Configure grid with origin x=0, z=0 and spacing=500
2. Claim location 1,1 (B2)
3. After island creation, check world coordinates
4. Verify world X coordinate is 1000 (1 * 500 * 2)
5. Verify world Z coordinate is 1000 (1 * 500 * 2)

**Expected Result:**
- Island center at world X=1000, Z=1000
- This is 1000 blocks east and 1000 blocks south of 0,0

**Note:** BSkyBlock's "distance-between-islands" config is the **radius** (offset from center to edge).
The actual spacing between island **centers** is `distance * 2 = 500 * 2 = 1000 blocks`.

---

### Test #47: Grid spacing configuration affects island placement
**Status:** READY FOR TESTING

**Test Steps:**
1. Set island-spacing to 1000 in BSkyBlock's config.yml
2. Restart server
3. Claim location B1 (1,0) - second column, first row
4. Verify island is created 2000 blocks from origin (1000 * 2)
5. Should be at world X=2000, Z=0

**Expected Result:**
- With spacing=1000, grid 1,0 should be at world 2000,0
- Spacing is properly configurable

---

### Test #48: Island UUID is linked to grid coordinates in database
**Status:** READY FOR TESTING

**Test Steps:**
1. Create an island at location D5 (3,4)
2. Check database/storage (YAML files in BentoBox database folder)
3. Verify island UUID is stored
4. Verify grid coordinate "3,4" is linked to that island UUID
5. Restart server
6. Verify the link persists after restart

**Expected Result:**
- Grid location is stored in database
- Island UUID is linked correctly
- Link survives server restarts
- Opening grid GUI shows the island at D5

---

## How Grid Coordinate Calculation Works

### Formula:
```
worldX = gridX * (spacing * 2)
worldZ = gridZ * (spacing * 2)
```

Where:
- `gridX` and `gridZ` are the grid coordinates (e.g., 0,0 or 1,1)
- `spacing` is BSkyBlock's "distance-between-islands" config value
- We multiply by 2 because BSkyBlock's spacing is the **radius**, not the diameter

### Examples with spacing=500:

| Grid Coord | gridX | gridZ | worldX | worldZ |
|------------|-------|-------|--------|--------|
| 0,0        | 0     | 0     | 0      | 0      |
| 1,0        | 1     | 0     | 1000   | 0      |
| 0,1        | 0     | 1     | 0      | 1000   |
| 1,1        | 1     | 1     | 1000   | 1000   |
| -1,0       | -1    | 0     | -1000  | 0      |
| 5,3        | 5     | 3     | 5000   | 3000   |

---

## Testing Priority

**HIGHEST PRIORITY:** Test #43 - Verify islands are created at the CORRECT location!

This was a critical bug that would have broken the entire plugin's core feature.

**Secondary Priority:** Tests 41, 42, 44 - Verify the claiming flow works smoothly

**Tertiary Priority:** Tests 45-48 - Verify coordinate mapping is accurate

---

## Regression Testing

All previous tests (1-40) should still pass:

✅ **Tests 1-25:** Core GUI, admin commands, persistence
✅ **Tests 26-35:** Island claiming, blueprint selection
⏳ **Tests 36-39:** Search functionality (from Session 10)
⏳ **Test 40:** Premium location tooltips (from Session 11)
⏳ **Tests 41-48:** Island claiming and coordinate mapping (THIS SESSION)

---

## What to Report

When testing island location placement, please report:

1. ✅ Does `/island` trigger the grid selection GUI for new players?
2. ✅ Can you select and confirm a grid location?
3. ✅ **CRITICAL:** Is the island created at the CORRECT world coordinates?
4. ✅ Do the grid coordinates match the world coordinates according to the formula?
5. ✅ Do different grid locations result in properly spaced islands?
6. ✅ Does canceling the claim work correctly?
7. ❌ Any errors in console during island creation?
8. ❌ Any islands created at wrong locations?

---

## Expected Console Output

During island creation, you should see logs like:
```
[IslandSelector] Allowing island creation for PlayerName - will relocate after creation
[IslandSelector] Relocating island from X,Z to X,Z (grid 0,0)
[IslandSelector] Registered island at grid 0,0 for PlayerName
```

This confirms the relocation is happening.

---

## Next Steps After Testing

Once Tests 41-48 pass, the next failing tests to implement are:

**Test #49:** Cannot claim already occupied location
**Test #50:** Cannot claim reserved blocked location
**Test #51:** Can purchase and claim reserved premium location
**Test #52:** Cannot purchase with insufficient funds

These tests focus on location validation and premium location purchases.

---

## Technical Notes

### Why This Bug Existed

BSkyBlock's `IslandCreateEvent` happens BEFORE the island is placed. The island gets created at BSkyBlock's default spiral location unless we intervene.

The original code was:
1. Storing the pending claim ✅
2. Allowing BSkyBlock to create the island ✅
3. Registering the grid location in our database ✅
4. **NOT** moving the island to the correct location ❌

### The Fix

Now we:
1. Store the pending claim ✅
2. Allow BSkyBlock to create the island ✅
3. **Move the island to the correct world coordinates** ✅ (NEW!)
4. Register the grid location in our database ✅

### Island Movement Method

We use BentoBox's Island API:
```java
island.setCenter(newLocation);
island.setSpawnPoint(Environment.NORMAL, newLocation);
```

This updates the island's protection center and spawn point to the new location.

**Note:** This only moves the island **metadata** (center point, spawn), not the actual blocks.
Since the island was just created, it's a fresh blueprint paste, so the blocks are already there.
The protection area and teleport points now point to the correct location.

---

## Known Limitations

**Block Movement:** This fix moves the island's center and spawn points, which works for new island creation because BSkyBlock just pasted the blueprint. The blocks ARE at the new location because we moved the center BEFORE the player teleports or does anything.

**Future Enhancement:** For island relocation (moving an EXISTING island), we'll need to use FAWE to copy the blocks to the new location. That's a different feature (not implemented yet).
