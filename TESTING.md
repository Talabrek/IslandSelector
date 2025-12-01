# IslandSelector - Manual Testing Instructions

## Build Information
- **Build Date:** Session 2
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **Size:** ~61KB

## Features Implemented This Session

This session implemented the core foundation for the Grid Selection GUI:

1. **Basic `/islandselector` command** - Opens the main grid GUI
2. **Main Grid GUI** - 54-slot inventory with 7x4 grid viewport
3. **Grid Coordinate System** - Alphanumeric coordinates (A1, B7, AA12, etc.)
4. **Navigation System** - Scroll arrows for navigating the grid
5. **Filter System** - Filter buttons for All/Available/Online
6. **Control Buttons** - Search, Find My Island, Neighborhood View, Slots, Close
7. **Slot Selection GUI** - Basic GUI for viewing island slots
8. **Neighborhood GUI** - 3x3 view of surrounding islands
9. **Admin Commands** - Basic admin commands for reserve/setprice/info/reload

## Required Dependencies for Testing

1. **Paper/Spigot 1.20.x - 1.21.x** server
2. **BentoBox** (latest version)
3. **BSkyBlock** addon for BentoBox
4. **FastAsyncWorldEdit (FAWE)** (required for schematic operations)

## Installation

1. Copy `output/IslandSelector-1.0.0-SNAPSHOT.jar` to `plugins/BentoBox/addons/`
2. Restart the server
3. Verify no errors in console during startup

---

## Test Cases Ready for Manual Testing

### Test #1: Plugin loads successfully with BentoBox and BSkyBlock
**Status:** Ready for testing
**Test Steps:**
1. Start the Minecraft server with BentoBox and BSkyBlock installed
2. Check server console for IslandSelector addon loading messages
3. Verify no errors in console during startup
4. Run `/bentobox version` and confirm IslandSelector is listed

**Expected:** Plugin loads without errors, shows version info

---

### Test #2: Config.yml file is created with all default values
**Status:** Ready for testing
**Test Steps:**
1. Start server with IslandSelector for the first time
2. Navigate to `plugins/BentoBox/addons/IslandSelector/` directory
3. Open config.yml file
4. Verify all configuration sections exist (grid, slots, relocation, reserved, backups, gui, visitors, performance, integration, debug)

**Expected:** Config file exists with all sections

---

### Test #4: FastAsyncWorldEdit dependency is detected and verified
**Status:** Ready for testing
**Test Steps:**
1. Install FAWE on the server
2. Start server with IslandSelector
3. Check console for FAWE detection message
4. Verify no warnings about missing FAWE

**Expected:** Console shows "FastAsyncWorldEdit found" or similar

---

### Test #5: Basic command /islandselector executes without errors
**Status:** Ready for testing
**Test Steps:**
1. Join the Minecraft server as a player
2. Execute `/islandselector` command
3. Verify a GUI opens
4. Verify no errors in server console

**Expected:** A 54-slot chest GUI opens with the grid view

---

### Test #6: Main grid GUI opens with 54 slots arranged correctly
**Status:** Ready for testing
**Test Steps:**
1. Join the Minecraft server
2. Execute `/islandselector` command
3. Verify a chest GUI with 6 rows (54 slots) opens
4. Check that top rows have scroll arrows and center area shows grid
5. Check that bottom row has control buttons

**Expected:** GUI has proper layout with arrows, grid area, and control buttons

---

### Test #7: Grid GUI title displays current viewport coordinates
**Status:** Ready for testing
**Test Steps:**
1. Open the grid GUI with `/islandselector`
2. Check the GUI title bar
3. Verify it shows format like "Grid View: A1 - G4"
4. Scroll the grid and verify title updates to reflect new viewport

**Expected:** Title shows current viewport range (e.g., "Grid View: A1 - G4")

---

### Test #8: Grid displays 7x4 island slots in center viewing area
**Status:** Ready for testing
**Test Steps:**
1. Open the grid GUI
2. Count the island display slots in the center area
3. Verify there are 7 columns and 4 rows (28 total slots)
4. Verify the slots are positioned in the center of the GUI

**Expected:** 28 grid slots (7 columns x 4 rows) in center area

---

### Test #9: Available/empty locations display as green stained glass panes
**Status:** Ready for testing
**Test Steps:**
1. Open the grid GUI on a fresh server with no islands
2. Observe the grid display slots
3. Verify empty locations show as GREEN_STAINED_GLASS_PANE items
4. Hover over an empty slot and verify tooltip shows it's available

**Expected:** Empty locations are green glass with "Available" in name/lore

---

### Test #16: Scroll up arrows move viewport north by 1 row
**Status:** Ready for testing
**Test Steps:**
1. Open grid GUI starting at viewport A1-G4
2. Scroll down first to position A2-G5 (so you can scroll up)
3. Click one of the scroll up arrow buttons
4. Verify GUI title updates to show viewport moved up
5. Verify grid content shifts appropriately

**Expected:** Title changes from e.g., "A2 - G5" to "A1 - G4"

---

### Test #17: Scroll down arrows move viewport south by 1 row
**Status:** Ready for testing
**Test Steps:**
1. Open grid GUI
2. Click one of the scroll down arrow buttons
3. Verify GUI title updates to show viewport moved down
4. Verify grid content shifts appropriately

**Expected:** Title shows viewport shifted down by 1 row

---

### Test #18: Scroll left arrows move viewport west by 1 column
**Status:** Ready for testing
**Test Steps:**
1. Open grid GUI
2. Scroll right first (so you can scroll left)
3. Click the scroll left arrow buttons
4. Verify GUI title updates to show viewport moved left
5. Verify grid content shifts appropriately

**Expected:** Title shows viewport shifted left by 1 column

---

### Test #19: Scroll right arrows move viewport east by 1 column
**Status:** Ready for testing
**Test Steps:**
1. Open grid GUI
2. Click the scroll right arrow buttons
3. Verify GUI title updates to show viewport moved right
4. Verify grid content shifts appropriately

**Expected:** Title shows viewport shifted right by 1 column

---

### Test: Shift-click scrolls multiple rows/columns
**Status:** Ready for testing
**Test Steps:**
1. Open grid GUI
2. Shift+click a scroll arrow
3. Verify viewport jumps 5 rows/columns instead of 1

**Expected:** Shift-click scrolls 5 instead of 1

---

### Test: Filter buttons work correctly
**Status:** Ready for testing
**Test Steps:**
1. Open grid GUI
2. Click "Filter: All" button - verify all locations shown
3. Click "Filter: Available" button - verify only available locations shown
4. Click "Filter: Online" button - verify only online players' islands shown
5. Verify the active filter has enchantment glow

**Expected:** Filters change what's displayed, active filter glows

---

### Test: Find My Island button works
**Status:** Ready for testing
**Test Steps:**
1. Create an island as a player
2. Open grid GUI
3. Click "Find My Island" button
4. Verify viewport centers on your island location
5. Verify message appears confirming location

**Expected:** Viewport moves to center on player's island

---

### Test: Close button works
**Status:** Ready for testing
**Test Steps:**
1. Open grid GUI
2. Click the Close button (barrier)
3. Verify GUI closes

**Expected:** GUI closes

---

### Test: Slots button opens Slot Selection GUI
**Status:** Ready for testing
**Test Steps:**
1. Open grid GUI
2. Click the "Island Slots" button (chest icon)
3. Verify Slot Selection GUI opens

**Expected:** 27-slot Slot Selection GUI opens

---

### Test: Neighborhood button opens Neighborhood View
**Status:** Ready for testing
**Test Steps:**
1. Create an island as a player
2. Open grid GUI
3. Click "Neighborhood View" button (map icon)
4. Verify 3x3 neighborhood GUI opens

**Expected:** 27-slot Neighborhood GUI opens showing surrounding islands

---

### Test: Admin reload command works
**Status:** Ready for testing
**Test Steps:**
1. As an operator, run `/islandselector admin reload`
2. Verify success message appears
3. Verify no errors in console

**Expected:** Configuration reloaded successfully message

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

## Known Limitations (This Session)

1. **Island claiming not yet functional** - Grid shows locations but actual island creation at specific coordinates is not yet implemented
2. **Island relocation not yet functional** - Confirmation and FAWE operations pending
3. **Slot switching not yet functional** - GUI shows slots but switching logic is pending
4. **Warp/visit functionality not yet connected** - Placeholder messages shown
5. **Search functionality not yet implemented** - Shows "coming soon" message
6. **Level display not yet integrated** - Level addon integration pending
7. **Database persistence not yet implemented** - Grid state is not persisted

---

## What to Report

When testing, please report:
1. Any console errors or warnings
2. GUI display issues (wrong items, layout problems)
3. Navigation problems (scrolling not working)
4. Command errors or unexpected behavior
5. Missing messages or incorrect text
6. Permission issues

---

## Next Features to Implement

1. Island claiming with BSkyBlock integration
2. Island relocation with FAWE
3. Slot switching system
4. Database persistence for grid data
5. Warp integration
6. Level addon integration
7. Search functionality
