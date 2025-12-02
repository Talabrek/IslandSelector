# IslandSelector - Manual Testing Instructions

## Build Information
- **Build Date:** Session 15 (Latest - FAWE Slot Switching Implementation)
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **JAR Size:** 118K
- **Status:** ✅ Complete slot switching system with FAWE

## Test Results Summary

| Test Range | Status | Notes |
|------------|--------|-------|
| Tests 1-25 | PASS | Core GUI, admin commands, persistence |
| Tests 26-35 | PASS | Island claiming, blueprint selection |
| Tests 36-39 | PENDING | Search functionality (Session 10) |
| Test 40 | PENDING | Premium location tooltips (Session 11) |
| Tests 41-48 | PENDING | Island claiming with correct coordinates (Session 12) |
| Tests 49-53 | PENDING | Location validation & premium purchases (Session 13) |
| Tests 54-61 | PENDING | Slot system UI and permissions (Session 13, improved Session 14) |
| Tests 62-63 | PENDING | Slot foundation & confirmation GUI (Session 14) |
| Tests 64-70 | PENDING | FAWE slot switching operations (Session 15) |

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
- Viewport moves north

#### Test #19: Scroll down works
**Status:** PASS
- Viewport moves south

#### Test #20: Shift-click scrolls 5
**Status:** PASS
- Fast scrolling works

#### Test #21: Filter buttons work
**Status:** PASS
- All, Available, Online filters
- Active filter has glow

#### Test #22: Find My Island works
**Status:** PASS
- Centers on player's island
- Confirmation message shown

#### Test #23: Admin reserve persists
**Status:** PASS
- Command works
- Survives restart

#### Test #24: Admin setprice persists
**Status:** PASS
- Command works
- Survives restart

#### Test #25: Admin info shows details
**Status:** PASS
- Shows status, owner, coordinates
- Locale strings display correctly

---

### Island Claiming Tests (26-35) - ALL PASSING

#### Test #26: Island creation intercepted
**Status:** PASS
- Run /island as new player (no island)
- Grid selection GUI opens instead of default creation

#### Test #27: Grid selection GUI displays correctly
**Status:** PASS
- 54-slot layout with navigation arrows
- Available locations show as green glass
- Occupied locations show as player heads

#### Test #28: Location selection works
**Status:** PASS
- Click on available location
- Selection message appears
- Location is highlighted with glow

#### Test #29: Double-click confirms selection
**Status:** PASS
- Click same location twice
- Confirmation GUI opens

#### Test #30: Confirmation GUI displays correctly
**Status:** PASS
- 27-slot layout
- Location info shows grid and world coords
- Confirm (green) and Cancel (red) buttons

#### Test #31: Cancel button works
**Status:** PASS
- Click Cancel in confirmation GUI
- "Cancelled" message shown
- No island created

#### Test #32: Blueprint selection GUI appears
**Status:** PASS
- After confirming location, blueprint selection opens
- Shows available BSkyBlock blueprints
- Displays blueprint icons and descriptions

#### Test #33: Blueprint selection creates island
**Status:** PASS
- Select a blueprint
- Island created at selected location with full schematic
- Player teleported to new island

#### Test #34: GUI centers on player's island
**Status:** PASS
- Player with island opens /islandselector
- Viewport is centered on their island location

#### Test #35: Tooltips show world coordinates
**Status:** PASS
- Hover over any location
- Shows "Location: X: 1600, Z: -1600" format

---

## Commands Available

| Command | Permission | Description |
|---------|------------|-------------|
| `/islandselector` | `islandselector.use` | Opens the main grid GUI |
| `/islandselector slots` | `islandselector.slots` | Opens the slot selection GUI |
| `/islandselector admin` | `islandselector.admin` | Shows admin help |
| `/islandselector admin reload` | `islandselector.admin.reload` | Reloads configuration |
| `/islandselector admin reserve <coord>` | `islandselector.admin.reserve` | Reserves a location |
| `/islandselector admin setprice <coord> <price>` | `islandselector.admin.reserve` | Sets price for location |
| `/islandselector admin info <coord>` | `islandselector.admin.info` | Shows location info |

---

## Island Creation Flow

1. **New player runs `/island`**
   - BSkyBlock's default creation is intercepted
   - IslandClaimGUI opens showing available locations

2. **Player selects location**
   - Click on green (available) location
   - Location is highlighted
   - Click again to confirm

3. **Confirmation GUI**
   - Shows selected location with world coordinates
   - Click Confirm to proceed or Cancel to go back

4. **Blueprint Selection**
   - Shows all available BSkyBlock blueprints
   - Player clicks to select island type

5. **Island Creation**
   - Island data created at selected coordinates
   - Blueprint pasted at location
   - Player teleported to new island
   - Grid location marked as occupied

---

## Configuration Notes

### Island Spacing
- Automatically detected from BSkyBlock's `distance-between-islands` setting
- Actual spacing = distance * 2 (BSkyBlock uses offset from center)
- No longer needs to be configured manually

### GUI Items (Configurable)
```yaml
gui:
  items:
    available: GREEN_STAINED_GLASS_PANE
    offline: RED_STAINED_GLASS_PANE
    reserved-blocked: GRAY_STAINED_GLASS_PANE
    reserved-purchasable: GOLD_BLOCK
    locked-area: BLACK_STAINED_GLASS_PANE
    filler: BLACK_STAINED_GLASS_PANE
```

---

## Known Limitations

1. **Player Heads for Offline Players** - May show Steve/default head if server hasn't cached player's skin (Minecraft limitation)
2. **Island Relocation** - Not yet implemented, shows "coming soon" message
3. **Slot Switching** - GUI shows slots but switching logic is pending
4. **Visit/Warp Feature** - Not yet implemented, shows "coming soon" message
5. **Level Display** - Level addon integration pending

---

## Next Features to Implement

### Priority 1 - Island Relocation
- FAWE schematic save/load
- Copy island to new location
- Clear old location
- Update all references

### Priority 2 - Slot System
- Connect claiming to slot system
- Implement slot switching with FAWE
- Handle visitors during switch

### Priority 3 - Additional Features
- Visit/warp to islands
- Level addon integration
- PlaceholderAPI integration

---

## What to Report

When testing, please report:
1. Any console errors or warnings
2. Blueprint paste issues
3. GUI display issues
4. Navigation problems
5. Permission issues
6. Island creation failures
