# IslandSelector - Manual Testing Instructions

## Build Information
- **Build Date:** Session 25 (Latest - Search Feature Documentation)
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **JAR Size:** 136K
- **Status:** ✅ 47 tests ready for manual verification (Tests #28-86)

## Latest Session
📋 **See [TESTING_SESSION25.md](TESTING_SESSION25.md) for Session 25 testing guide**

Session 25 discovered that Player Search functionality (Tests #83-86) was already fully implemented in a previous session. Created comprehensive testing documentation for manual verification.

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
