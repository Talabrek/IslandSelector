# Session 38 - For Human Tester

## Quick Summary

**Feature:** Island Claiming & Premium Locations
**Status:** ✅ ALREADY IMPLEMENTED - Needs Testing
**Purpose:** This session documents existing island claiming and premium location features
**Build Status:** ✅ NO CHANGES (Documentation only)
**Your Task:** Test island claiming flow and premium locations

---

## What This Session Did

**Discovery:** Session 38 found that **Island Claiming** and **Premium Location** features (Tests #44-50+) were already fully implemented in previous sessions but never verified by human testing.

**No new code** was added - this session documents what already exists.

---

## What to Test

Seven feature groups ready for testing:

1. **Test #44:** Premium location hover tooltip (price displayed)
2. **Tests #45-48:** Island claiming flow for new players
3. **Tests #49-50:** Grid coordinate mapping verification
4. **Tests #51-54:** Additional claiming edge cases

---

## Quick 5-Minute Test

### Setup
Copy JAR: `output/IslandSelector-1.0.0-SNAPSHOT.jar` → `plugins/BentoBox/addons/`
(Note: JAR is unchanged from Session 37 - 180K size)

### Core Test Flow

**Test Island Claiming (Most Important):**

```
1. Create fresh player account (never made island)
2. Run: /island
3. Expected: Grid GUI opens (NOT auto-created island!)
4. Click any green glass (available location)
5. Click SAME location again
6. Expected: Confirmation GUI opens
7. Click GREEN WOOL (Confirm)
8. Expected: Island created at selected grid location!
```

**Test Premium Location:**

```
Admin:
1. /islandselector admin setprice 5,5 50000
2. Verify success message

Player:
1. /islandselector
2. Hover over location 5,5
3. Expected: Gold block with "⭐ Premium Location" and price
```

---

## Full Testing

See `TESTING_SESSION38.md` for:
- Detailed test cases (10+ tests)
- Step-by-step instructions
- Expected results
- Edge cases
- Report template

---

## What to Check

✅ New players see grid GUI (not auto-island)
✅ Click location once → it highlights/selects
✅ Click same location twice → confirmation GUI
✅ Confirm button creates island at selected location
✅ Cancel button returns to grid
✅ Premium locations show GOLD BLOCK with price
✅ Grid coordinates match world coordinates
✅ Cannot claim occupied locations
✅ Cannot claim reserved (blocked) locations
✅ Can purchase premium if has money

---

## Critical Tests

### Test #45: New Player Intercept ⭐ HIGH PRIORITY
**Most important feature!**

**Steps:**
1. Fresh player (no island ever)
2. Run: `/island`
3. Expected: Grid GUI opens instead of auto-island creation

**Why Important:** This is the core feature - grid selection REPLACES automatic placement!

---

### Test #46: Click Opens Confirmation
**Steps:**
1. In grid GUI from Test #45
2. Click empty green glass location
3. Click SAME location again
4. Expected: Confirmation GUI opens with Confirm/Cancel buttons

---

### Test #47: Confirm Creates Island
**Steps:**
1. In confirmation GUI from Test #46
2. Click green wool (Confirm button)
3. Expected: Island created at selected grid coordinates
4. Expected: Success message with location

---

### Test #48: Cancel Returns to Grid
**Steps:**
1. Open grid, select location, open confirmation
2. Click red wool (Cancel button)
3. Expected: Returns to grid GUI
4. Expected: No island created

---

### Test #44: Premium Location Tooltip
**Steps:**
1. Admin: `/islandselector admin setprice 5,5 50000`
2. Player: Open grid GUI
3. Hover over 5,5 (should be gold block)
4. Expected: Tooltip shows "⭐ Premium Location: 5,5"
5. Expected: Tooltip shows "Price: $50,000"
6. Expected: Tooltip shows "Click to purchase"

---

### Test #49: Grid Coordinate A1 Mapping
**Steps:**
1. Configure default spacing (500 blocks)
2. Create island at 0,0 (grid center)
3. Check island center location
4. Expected: World X = 0
5. Expected: World Z = 0

---

### Test #50: Grid Coordinate B2 Mapping
**Steps:**
1. Default spacing (500 blocks)
2. Create island at 1,1
3. Check island center location
4. Expected: World X = 1000 (1 * 500 * 2)
5. Expected: World Z = 1000 (1 * 500 * 2)

**Note:** BSkyBlock spacing is doubled (250 becomes 500 actual)

---

## Configuration Notes

**Default Settings (config.yml):**
```yaml
grid:
  origin-x: 0
  origin-z: 0
  max-x: 50
  max-z: 50
  min-x: -50
  min-z: -50
```

**BSkyBlock Spacing:**
- Check: `plugins/BentoBox/BSkyBlock/config.yml`
- Setting: `island.distance-between-islands: 250`
- Actual spacing: 500 blocks (doubled by BentoBox)

---

## Admin Commands for Testing

```bash
# Set premium location
/islandselector admin setprice <coord> <price>
/islandselector admin setprice 5,5 50000

# Clear premium price
/islandselector admin clearprice <coord>

# Reserve blocked location
/islandselector admin reserve <coord>

# Unreserve location
/islandselector admin unreserve <coord>

# Check location info
/islandselector admin info <coord>
/islandselector admin info 0,0
```

---

## Expected Behaviors

### Island Creation Intercept
- **Before IslandSelector:** `/island` → auto-creates at random spiral location
- **With IslandSelector:** `/island` → grid GUI opens for selection

### Claiming Flow
1. Player runs `/island`
2. Grid GUI opens showing available locations (green glass)
3. Player clicks location → it gets highlighted
4. Player clicks SAME location → confirmation GUI opens
5. Player clicks Confirm → island created at selected spot
6. Grid updates to show occupied location (player head)

### Premium Locations
- Admin sets price: `/islandselector admin setprice D4 50000`
- Location appears as GOLD BLOCK in grid
- Tooltip shows "⭐ Premium Location" with price
- Click opens purchase confirmation (if has funds)
- After purchase, island is created normally

### Reserved Locations
- Admin reserves: `/islandselector admin reserve B2`
- Location appears as RED STAINED GLASS
- Tooltip shows "Reserved - Not Available"
- Cannot be claimed by players
- Only admin can unreserve

---

## Report Format

After testing, please report results:

```
=== Session 38: Island Claiming & Premium Locations Test Report ===

Test #44 (Premium tooltip): [PASS/FAIL]
Test #45 (Grid intercepts /island): [PASS/FAIL]
Test #46 (Click opens confirmation): [PASS/FAIL]
Test #47 (Confirm creates island): [PASS/FAIL]
Test #48 (Cancel returns to grid): [PASS/FAIL]
Test #49 (Grid coord 0,0 mapping): [PASS/FAIL]
Test #50 (Grid coord 1,1 mapping): [PASS/FAIL]

Issues found:
[Describe any problems, unexpected behavior, or bugs]

Console errors:
[Paste any errors from server console]

Overall: [PASS/FAIL]
```

---

## Important Notes

- **Fresh account required** - Must test with player who has NEVER created island
- **Single test only** - Once you create island, that account can't test again
- **World coordinates** - Use F3 debug screen to check island center
- **Spacing calculation** - Real spacing = BSkyBlock distance × 2
- **Premium requires Vault** - If no Vault, premium features won't work
- **Confirmation is double-click** - Click once selects, click again confirms

---

## Troubleshooting

**Problem:** `/island` still auto-creates island (doesn't show GUI)
- **Check:** IslandSelector loaded? `/bentobox version`
- **Check:** Console for errors during startup
- **Check:** Try `/islandselector` command instead

**Problem:** Premium locations show as regular green glass
- **Check:** Did admin set price? `/islandselector admin info 5,5`
- **Check:** Vault installed? (optional but needed for economy)
- **Check:** Economy plugin (EssentialsX, CMI, etc.) installed?

**Problem:** Island created at wrong coordinates
- **Check:** BSkyBlock island spacing setting
- **Check:** IslandSelector grid origin in config.yml
- **Calculate:** worldCoord = gridCoord × spacing × 2

**Problem:** Can't click locations in grid
- **Check:** Permission: `islandselector.island.create` (default true)
- **Check:** Console for errors when clicking
- **Check:** Player already has island? (Use fresh account)

---

## Next After This

**If Tests #44-50 All Pass:**
- Mark tests as passing ✅
- Progress: 51/225 tests passing (was 44)
- Move to next failing test block (around Test #51+)

**If Any Test Fails:**
- Report the issue with details
- Agent will debug and fix
- Re-test until passing

---

## File Info

**JAR:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
**Size:** 180K (unchanged from Session 37)
**Changes:** None - documentation only
**Build Status:** ✅ SUCCESS

---

## Key Files Involved

**Already Implemented:**
- `IslandCreateListener.java` - Intercepts `/island` command
- `IslandClaimGUI.java` - Grid GUI for location selection
- `ConfirmationGUI.java` - Confirm/cancel dialog
- `MainGridGUI.java` - Premium location tooltips
- `AdminSetPriceCommand.java` - Admin command for pricing
- `GridManager.java` - Grid coordinate calculations

---

**Ready to test!** 🎯

**Core Test:** Fresh player → `/island` → Grid opens → Select → Confirm → Island created

**Time:** 5-10 minutes

**Required:** Fresh player account (never created island)

**JAR:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`

---

## Success Criteria

✅ Grid GUI replaces automatic island creation
✅ Click-to-select, double-click-to-confirm works
✅ Confirmation dialog has working Confirm/Cancel buttons
✅ Island created at chosen grid coordinates
✅ Premium locations show gold block with price tooltip
✅ Grid coordinates match expected world coordinates
✅ No console errors during claiming process

**This is a MAJOR feature set** - island claiming is core functionality!
