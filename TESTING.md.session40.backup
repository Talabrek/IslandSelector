# IslandSelector - Manual Testing Instructions

## Build Information
- **Build Date:** Session 39 (Latest - Player Search Documentation)
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **JAR Size:** 180K
- **Status:** ✅ 44 tests verified, 11 tests pending manual verification

## Latest Sessions (38-39)

### Session 39: Player Search Functionality ⭐ NEW
📋 **See [SESSION39_FOR_HUMAN_TESTER.md](SESSION39_FOR_HUMAN_TESTER.md) for quick test guide**
📋 **See [TESTING_SESSION39.md](TESTING_SESSION39.md) for detailed testing**

**Features Ready:** 4 search-related tests
- Search button displays (COMPASS item)
- Search finds player by name
- Partial name matching ("Not" finds "Notch")
- Player not found error handling

**Testing Time:** 10-15 minutes
**Players Required:** 2-3 accounts

### Session 38: Island Claiming & Premium Locations
📋 **See [SESSION38_FOR_HUMAN_TESTER.md](SESSION38_FOR_HUMAN_TESTER.md) for quick test guide**
📋 **See [TESTING_SESSION38.md](TESTING_SESSION38.md) for detailed testing**

**Features Ready:** 7 island claiming tests
- New player intercepts /island command (CRITICAL TEST!)
- Click-to-select, double-click-to-confirm flow
- Confirmation GUI with Confirm/Cancel buttons
- Island created at selected grid coordinates
- Premium location tooltips (gold block with price)
- Grid coordinate mapping verification

**Testing Time:** 15-20 minutes
**Players Required:** 3-4 fresh accounts (can only claim once per account)

### Combined Sessions 38 + 39
**Total Tests Pending:** 11 tests (7 + 4)
**Combined Testing Time:** 20-30 minutes
**If All Pass:** Progress jumps from 44/225 to 51/225 tests

## Previous Sessions
📋 **See [TESTING_SESSION37.md](TESTING_SESSION37.md) for Session 37 testing guide**

Session 37 documented existing search functionality previously implemented.

📋 **See [TESTING_SESSION31.md](TESTING_SESSION31.md) for Session 31 testing guide**

Session 31 implemented the Slot Settings GUI, which opens when players right-click on island slots. Provides quick access to rename, change icon, and delete features with visual buttons and command instructions.

📋 **See [TESTING_SESSION30.md](TESTING_SESSION30.md) for Session 30 testing guide**

Session 30 implemented the `/islandselector delete` command with confirmation GUI for safely deleting island slots.

📋 **See [TESTING_SESSION29.md](TESTING_SESSION29.md) for Session 29 testing guide**

Session 29 implemented the `/islandselector seticon` command, allowing players to set custom icons for their island slots using held items.

📋 **See [TESTING_SESSION28.md](TESTING_SESSION28.md) for Session 28 testing guide**

Session 28 implemented the `/islandselector setname` command, allowing players to set custom names for their island slots (e.g., "Main Base", "Farm Island").

## Previous Sessions
📋 **See [TESTING_SESSION27.md](TESTING_SESSION27.md) for Session 27 testing guide**

Session 27 implemented admin reset cooldown command for clearing player slot switch cooldowns.

📋 **See [TESTING_SESSION26.md](TESTING_SESSION26.md) for Session 26 testing guide**

Session 26 enhanced the admin info command to support player lookups. Admins can now query detailed player island and slot information using `/islandselector admin info <player>`.

## Previous Sessions
📋 **See [TESTING_SESSION25.md](TESTING_SESSION25.md) for Session 25 testing guide**

Session 25 discovered that Player Search functionality (Tests #83-86) was already fully implemented. Created comprehensive testing documentation for manual verification.

## Previous Session
📋 **See [TESTING_SESSION24.md](TESTING_SESSION24.md) for Session 24 testing guide**

Session 24 implemented Tests #81-82: Admin grid shrink commands (shrink grid with island protection validation).

**Quick Test:**
```
/islandselector admin version
```

## Previous Sessions
📋 **See [TESTING_SESSION18.md](TESTING_SESSION18.md) for Tests #28-74 comprehensive guide**

## Test Results Summary

| Test Range | Status | Notes |
|------------|--------|-------|
| Tests 1-25 | PASS | Core GUI, admin commands, persistence |
| Tests 26-35 | PASS | Island claiming, blueprint selection |
| Tests 36-39 | PENDING | Search functionality |

---

## Features Implemented

### Core Features (Session 1-5)
1. **Grid-based GUI** - 54-slot chest GUI with 7x4 island grid display
2. **BSkyBlock Integration** - Auto-syncs existing islands on startup
3. **Database Persistence** - Grid locations persist across restarts
4. **Admin Commands** - Reserve, setprice, info, reload commands
5. **Navigation** - Scroll arrows with shift-click for fast scrolling
6. **Filters** - All, Available, Online filter options
7. **Find My Island** - Centers viewport on player's island
8. **Configurable GUI Items** - All display items configurable in config.yml

### Island Claiming (Session 6-8)
9. **Island Creation Interception** - Intercepts /island command for new players
10. **Grid Location Selection** - Players choose where to create their island
11. **Blueprint/Schematic Selection** - Players choose island type from available blueprints
12. **Confirmation GUI** - Confirm location before creation
13. **Island Creation at Selected Location** - Islands created at player's chosen coordinates

### Session 8 Improvements
- Fixed blueprint paste (was only creating single block)
- GUI now centers on player's island when opened
- Tooltips show world coordinates (X: 1600, Z: -1600) instead of grid coords
- Fixed "feature not available" message when clicking available locations
- Blueprint selection GUI always shows (even with single blueprint)

### Session 9 - Compilation Fixes (THIS SESSION)
14. **IslandCreateListener** - Recreated complete event listener for island creation
15. **Fixed Grid Coordinate Conversion** - Corrected worldToGrid/getWorldX/getWorldZ methods
16. **Build Restored** - Removed duplicate/incomplete files causing compilation errors

**⚠️ IMPORTANT: Regression testing required!**
All previous features should still work, but need verification after these fixes:
- Island creation interception
- Grid location selection
- Blueprint selection
- Island appearing at correct grid coordinates

---

## Required Dependencies

1. **Paper/Spigot 1.20.x - 1.21.x** server
2. **BentoBox** (latest version)
3. **BSkyBlock** addon for BentoBox
4. **FastAsyncWorldEdit (FAWE)** (required for schematic operations)

## Installation

1. Copy `target/IslandSelector-1.0.0.jar` to `plugins/BentoBox/addons/`
2. Restart the server
3. Verify no errors in console during startup

---

## Test Cases

### Core GUI Tests (1-25) - ALL PASSING

#### Test #1: Plugin loads successfully
**Status:** PASS
- Plugin loads without errors
- Shows version info in console
- Detects island spacing from BSkyBlock

#### Test #2: Config.yml created with defaults
**Status:** PASS
- Config file exists with all sections
- GUI item settings configurable

#### Test #3: Database tables created
**Status:** PASS
- GridLocations data structures exist
- Loading message appears in console

#### Test #4: FAWE dependency detected
**Status:** PASS
- Console shows dependencies found

#### Test #5: /islandselector command works
**Status:** PASS
- GUI opens without errors

#### Test #6: Main grid GUI layout correct
**Status:** PASS
- 54 slots with proper layout
- Arrows, grid area, control buttons

#### Test #7: GUI title displays correctly
**Status:** PASS
- Static title "Island Grid Selector"
- Viewport info in Grid Info button

#### Test #8: 7x4 grid display
**Status:** PASS
- 28 grid slots in center area

#### Test #9: Available locations show green glass
**Status:** PASS
- Configurable via config.yml

#### Test #10: Online owners show player heads
**Status:** PASS
- Correct player skin displayed

#### Test #11: Offline owners show player heads
**Status:** PASS
- Player heads for all occupied islands
- Note: May show Steve if server hasn't cached skin

#### Test #12: Own island has glow effect
**Status:** PASS
- Enchantment glow applied
- "Your Island" tooltip

#### Test #13: Reserved blocked = gray glass
**Status:** PASS
- `/islandselector admin reserve <coord>` works
- Configurable via config.yml

#### Test #14: Reserved purchasable = gold block
**Status:** PASS
- `/islandselector admin setprice <coord> <price>` works
- Configurable via config.yml

#### Test #15: Reserved locations persist
**Status:** PASS
- Data survives server restart

#### Test #16: BSkyBlock sync works
**Status:** PASS
- Islands detected and displayed
- Only shows in correct single slot

#### Test #17: Data saves on shutdown
**Status:** PASS
- "Saving grid data..." message appears

#### Test #18: Scroll up works
**Status:** PASS
