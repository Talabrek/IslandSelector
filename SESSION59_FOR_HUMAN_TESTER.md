# Session 59 - Human Tester Verification Guide

## Current Status

- **Tests Passing:** 44/225 (19.6%)
- **Tests Pending:** 181 (need manual verification)
- **Build Status:** SUCCESS (220K JAR)
- **Feature Status:** **COMPLETE** - All features implemented in code

## JAR Location

```
output/IslandSelector-1.0.0-SNAPSHOT.jar
```

Copy to: `plugins/BentoBox/addons/`

---

## Priority Test Groups

### GROUP 1: Search Functionality (Tests 28-31) - QUICK WIN
**Expected Testing Time:** 10 minutes

These tests verify the search button and functionality in the grid GUI.

| Test # | Description | Status |
|--------|-------------|--------|
| 28 | Search button displays with compass item | Pending |
| 29 | Search function finds player and centers grid | Pending |
| 30 | Search function handles partial name matching | Pending |
| 31 | Search shows error for player not found | Pending |

**Testing Steps:**
1. Open grid GUI (`/islandselector`)
2. Find COMPASS item at slot 47 (bottom row, middle)
3. Click it - GUI should close and chat prompts for player name
4. Type a player name who has an island
5. Verify grid reopens centered on their island
6. Repeat with partial name (e.g., "Not" for "Notch")
7. Try non-existent player name, verify error message

---

### GROUP 2: Premium Locations (Test 44) - ADMIN FEATURE
**Expected Testing Time:** 10 minutes

| Test # | Description | Status |
|--------|-------------|--------|
| 44 | Premium location shows price and purchase option | Pending |

**Testing Steps:**
1. As admin, run:
   ```
   /islandselector admin reserve 1,1
   /islandselector admin setprice 1,1 50000
   ```
2. Open grid GUI as regular player
3. Navigate to location 1,1
4. Verify:
   - Shows as GOLD_BLOCK
   - Tooltip shows "Premium Location: 1,1"
   - Tooltip shows "Price: $50,000"
   - Tooltip shows "Click to purchase"

---

### GROUP 3: Island Claiming Flow (Tests 45-58) - CORE FUNCTIONALITY
**Expected Testing Time:** 30 minutes

| Test # | Description | Status |
|--------|-------------|--------|
| 45 | New player sees grid in claiming mode | Pending |
| 46 | Clicking empty location opens claim confirmation | Pending |
| 47 | Confirming claim creates island at correct coordinates | Pending |
| 48 | Canceling claim returns to grid GUI | Pending |
| 49 | Grid coordinate A1 maps to correct world coordinates | Pending |
| 50 | Grid coordinate B2 maps to correct world coordinates | Pending |
| 51 | Grid spacing configuration affects placement | Pending |
| 52 | Island UUID is linked to grid coordinates in database | Pending |
| 53 | Cannot claim already occupied location | Pending |
| 54 | Cannot claim reserved blocked location | Pending |
| 55 | Can purchase premium location with sufficient funds | Pending |
| 56 | Cannot purchase premium location with insufficient funds | Pending |
| 57 | Purchase bypass permission allows free claiming | Pending |

**Testing Steps:**
1. Join as NEW player (never had island before)
2. Run `/island` - should open grid selector (not auto-create)
3. Click green empty location - confirmation GUI should open
4. Click Cancel - returns to grid
5. Click green location again, then Confirm - island creates
6. Verify world coordinates match grid position

---

### GROUP 4: Slot System GUI (Tests 59-76) - MULTI-ISLAND FEATURE
**Expected Testing Time:** 45 minutes

| Test # | Description | Status |
|--------|-------------|--------|
| 59 | Slot Selection GUI opens with correct layout | Pending |
| 60 | Active slot shows enchantment glow | Pending |
| 61 | Inactive slot shows switch prompt | Pending |
| 62 | Empty unlocked slot shows lime dye | Pending |
| 63 | Locked slot shows barrier | Pending |
| 64 | Player with slots.2 has 2 slots | Pending |
| 65 | Player with slots.5 has 5 slots | Pending |
| 66 | Clicking locked slot shows error | Pending |
| 67 | Clicking empty slot starts island creation | Pending |
| 68 | New island shares same grid location | Pending |

**Testing Steps:**
1. Run `/islandselector slots` - should open 27-slot GUI
2. Verify active slot has glow effect
3. Verify empty slots show LIME_DYE
4. Verify locked slots show BARRIER
5. Test clicking different slot types

---

## Quick Smoke Test Commands

Run these to verify core functionality works:

```
/islandselector          # Opens grid GUI
/islandselector slots    # Opens slot GUI
/islandselector locate   # Shows your coordinates
/islandselector help     # Shows all commands
/islandselector neighbors # Opens neighborhood GUI

/islandselector admin version  # Shows plugin version
/islandselector admin info 0,0 # Check location info
```

---

## Dependencies Required

| Dependency | Required | Notes |
|------------|----------|-------|
| BentoBox | Yes | Core framework |
| BSkyBlock | Yes | Island gamemode |
| FastAsyncWorldEdit | Yes | Schematic operations |
| Vault | No | Economy tests only |
| PlaceholderAPI | No | Placeholder tests only |

---

## How to Mark Tests as Passing

After verifying a test works:

1. Open `feature_list.json`
2. Find the test by description
3. Change `"passes": false` to `"passes": true`
4. Save the file

---

## Expected Results

If all tests pass:
- Current: 44 tests
- After this session: 44 + ~30 = ~74 tests (33%)

Focus on **Groups 1-2** first (Search + Premium) for quick wins, then move to Groups 3-4 for core functionality.

---

## Notes

1. All features are implemented in code - these tests just need verification
2. If a test fails, note the specific issue so it can be fixed
3. Some tests require Vault for economy features
4. Some tests require multiple player accounts

Good luck testing!
