# IslandSelector - Manual Testing Instructions

## Build Information
- **Build Date:** Session 3
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **Size:** ~68KB

## Features Implemented This Session

This session implemented **Database Persistence** for grid locations:

1. **GridLocationData** - Database model for storing grid location data using BentoBox's database abstraction
2. **Database Loading** - Grid locations are loaded from database on startup
3. **Database Saving** - Grid locations are saved to database when modified and on shutdown
4. **BSkyBlock Sync** - Existing BSkyBlock islands are automatically synced to the grid on startup
5. **Data Persistence** - Reserved locations and admin changes persist across server restarts

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

### Test #3: Database tables/collections are created on first startup
**Status:** Ready for testing (NEW)
**Test Steps:**
1. Start server with IslandSelector
2. Check BentoBox database location (YAML/JSON files in database folder or SQL database)
3. Verify IslandSelector data structures exist for grid locations (GridLocations table/files)
4. Check console for "Loading grid locations from database..." message

**Expected:** Database structures are created, loading message appears in console

---

### Test #4: FastAsyncWorldEdit dependency is detected and verified
**Status:** Ready for testing
**Test Steps:**
1. Install FAWE on the server
2. Start server with IslandSelector
3. Check console for FAWE detection message
4. Verify no warnings about missing FAWE

**Expected:** Console shows "All required dependencies found" or similar

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

### Test #10: Occupied islands with online owners show player heads
**Status:** Ready for testing
**Test Steps:**
1. Have a player create an island using BSkyBlock (`/is create`)
2. Ensure that player is online
3. Open the grid GUI with a different account
4. Locate the occupied island slot
5. Verify it displays as the island owner's player head

**Expected:** Online player's island shows their player head

---

### Test #11: Occupied islands with offline owners show red stained glass
**Status:** Ready for testing
**Test Steps:**
1. Have a player create an island
2. Have that player disconnect from the server
3. Join with a different account and open the grid GUI
4. Locate the offline player's island slot
5. Verify it displays as RED_STAINED_GLASS_PANE

**Expected:** Offline player's island shows red glass pane

---

### Test #12: Player's own island displays with enchantment glow effect
**Status:** Ready for testing
**Test Steps:**
1. Create an island as a player
2. Open the grid GUI
3. Locate your own island in the grid
4. Verify it has an enchantment glow effect
5. Verify tooltip shows "Your Island"

**Expected:** Own island has glow effect and special tooltip

---

### Test #13: Reserved blocked locations show as gray stained glass pane
**Status:** Ready for testing
**Test Steps:**
1. As admin, reserve a location with `/islandselector admin reserve A1`
2. Do not set a price (leave it blocked)
3. Open grid GUI as a player
4. Locate position A1
5. Verify it shows as GRAY_STAINED_GLASS_PANE with "Reserved" tooltip

**Expected:** Reserved blocked location shows gray glass

---

### Test #14: Reserved purchasable locations show as gold blocks
**Status:** Ready for testing
**Test Steps:**
1. As admin, reserve a location and set price: `/islandselector admin setprice B2 50000`
2. Open grid GUI as a player
3. Locate position B2
4. Verify it shows as GOLD_BLOCK
5. Verify tooltip shows "Premium Location" with price

**Expected:** Premium location shows as gold block with price

---

### Test #15: Reserved locations persist across server restarts (NEW)
**Status:** Ready for testing
**Test Steps:**
1. As admin, reserve location A1: `/islandselector admin reserve A1`
2. Set a price on B2: `/islandselector admin setprice B2 25000`
3. Restart the server
4. Open grid GUI
5. Verify A1 is still reserved (gray glass)
6. Verify B2 is still premium with price $25,000

**Expected:** Reserved locations persist after restart

---

### Test #16: BSkyBlock islands are synced to grid on startup (NEW)
**Status:** Ready for testing
**Test Steps:**
1. Create an island using standard BSkyBlock (`/is create`)
2. Note the island location
3. Restart the server
4. Check console for "Syncing grid with BSkyBlock islands..." message
5. Open grid GUI and verify the island appears in the correct grid position

**Expected:** Existing BSkyBlock islands appear in grid after sync

---

### Test #17: Grid data saves on server shutdown (NEW)
**Status:** Ready for testing
**Test Steps:**
1. Make some changes (reserve locations, etc.)
2. Stop the server gracefully
3. Check console for "Saving grid data..." message
4. Check database files exist in BentoBox database folder
5. Restart and verify data persisted

**Expected:** "Saving grid data..." message appears, data persists

---

### Test #18: Scroll up arrows move viewport north by 1 row
**Status:** Ready for testing
**Test Steps:**
1. Open grid GUI starting at viewport A1-G4
2. Scroll down first to position A2-G5 (so you can scroll up)
3. Click one of the scroll up arrow buttons
4. Verify GUI title updates to show viewport moved up
5. Verify grid content shifts appropriately

**Expected:** Title changes from e.g., "A2 - G5" to "A1 - G4"

---

### Test #19: Scroll down arrows move viewport south by 1 row
**Status:** Ready for testing
**Test Steps:**
1. Open grid GUI
2. Click one of the scroll down arrow buttons
3. Verify GUI title updates to show viewport moved down
4. Verify grid content shifts appropriately

**Expected:** Title shows viewport shifted down by 1 row

---

### Test #20: Shift-click scrolls multiple rows/columns
**Status:** Ready for testing
**Test Steps:**
1. Open grid GUI
2. Shift+click a scroll arrow
3. Verify viewport jumps 5 rows/columns instead of 1

**Expected:** Shift-click scrolls 5 instead of 1

---

### Test #21: Filter buttons work correctly
**Status:** Ready for testing
**Test Steps:**
1. Open grid GUI
2. Click "Filter: All" button - verify all locations shown
3. Click "Filter: Available" button - verify only available locations shown
4. Click "Filter: Online" button - verify only online players' islands shown
5. Verify the active filter has enchantment glow

**Expected:** Filters change what's displayed, active filter glows

---

### Test #22: Find My Island button works
**Status:** Ready for testing
**Test Steps:**
1. Create an island as a player
2. Open grid GUI
3. Click "Find My Island" button
4. Verify viewport centers on your island location
5. Verify message appears confirming location

**Expected:** Viewport moves to center on player's island

---

### Test #23: Admin reserve command works and persists
**Status:** Ready for testing
**Test Steps:**
1. As an operator, run `/islandselector admin reserve C3`
2. Verify success message appears
3. Open grid GUI and verify C3 is reserved (gray glass)
4. Restart server
5. Verify C3 is still reserved after restart

**Expected:** Location reserved and persists across restart

---

### Test #24: Admin setprice command works and persists
**Status:** Ready for testing
**Test Steps:**
1. As an operator, run `/islandselector admin setprice D4 100000`
2. Verify success message appears
3. Open grid GUI and verify D4 shows as gold block with price
4. Restart server
5. Verify D4 still shows price after restart

**Expected:** Price set and persists across restart

---

### Test #25: Admin info command shows location details
**Status:** Ready for testing
**Test Steps:**
1. Reserve a location and set price: `/islandselector admin setprice E5 50000`
2. Run `/islandselector admin info E5`
3. Verify output shows coordinate, status, price, and other info

**Expected:** Location info displayed correctly

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

## New Features in This Session

### Database Persistence
- Grid location data now persists across server restarts
- Uses BentoBox's database abstraction (supports YAML, JSON, MySQL, SQLite, MongoDB)
- Reserved locations and prices persist
- Occupied locations are tracked in database

### BSkyBlock Sync
- On startup, existing BSkyBlock islands are automatically synced to the grid
- Islands are mapped to grid coordinates based on their world position
- Console shows sync progress and count

### Data Saving
- Data is saved automatically when changes are made
- All data is saved on server shutdown
- Uses async saving for better performance

---

## Known Limitations (This Session)

1. **Island claiming not yet functional** - Grid shows locations but actual island creation at specific coordinates is not yet implemented
2. **Island relocation not yet functional** - Confirmation and FAWE operations pending
3. **Slot switching not yet functional** - GUI shows slots but switching logic is pending
4. **Warp/visit functionality not yet connected** - Placeholder messages shown
5. **Search functionality not yet implemented** - Shows "coming soon" message
6. **Level display not yet integrated** - Level addon integration pending

---

## What to Report

When testing, please report:
1. Any console errors or warnings
2. Database loading/saving issues
3. Islands not appearing in grid after sync
4. Reserved locations not persisting
5. GUI display issues
6. Navigation problems
7. Permission issues

---

## Next Features to Implement

1. **Island Claiming** - Allow players to claim islands at specific grid locations
2. **Claim Confirmation GUI** - Confirm location selection before creating island
3. Island relocation with FAWE
4. Slot switching system
5. Warp integration
6. Level addon integration
7. Search functionality
