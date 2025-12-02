# IslandSelector - Session 10 Testing Instructions

## Build Information
- **Session:** 10 - Search Functionality
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **Status:** ✅ Search Feature Complete - Ready for Manual Testing

## What's New in Session 10

### Search Feature (COMPLETE)
The search functionality is now fully implemented! Players can search for other players' islands and automatically center the grid viewport on them.

**Implementation Details:**
- ✅ Search button (COMPASS) in grid GUI
- ✅ Chat-based search interface
- ✅ Exact name matching
- ✅ Partial name matching (e.g., "Not" finds "Notch")
- ✅ Online and offline player search
- ✅ Error handling for players not found
- ✅ Auto-centering viewport on found islands
- ✅ Cancel search functionality

---

## Search Feature Tests (NEEDS MANUAL TESTING)

### Test #36: Search button displays with compass item
**Status:** READY FOR TESTING
**Steps:**
1. Open grid GUI with `/islandselector`
2. Locate the search button in bottom row (slot 47)
3. Verify it shows as COMPASS item
4. Verify tooltip says "Search Player" and "Find a player's island"

**Expected Result:** Search button displays correctly with COMPASS icon

---

### Test #37: Search function finds player and centers grid on their island
**Status:** READY FOR TESTING
**Steps:**
1. Have two players on server (TestPlayer1 and TestPlayer2)
2. TestPlayer1 creates an island at any grid location (note the coordinates)
3. TestPlayer2 opens grid GUI
4. TestPlayer2 clicks the search button
5. TestPlayer2 types "TestPlayer1" in chat
6. Verify grid viewport centers on TestPlayer1's island
7. Verify TestPlayer1's island is visible in the viewport

**Expected Results:**
- Search prompt appears in chat with instructions
- After typing name, viewport automatically centers
- Success message shown: "Found TestPlayer1's island at [coords]!"
- Grid GUI reopens with centered view

---

### Test #38: Search function handles partial name matching
**Status:** READY FOR TESTING
**Steps:**
1. Have a player named "Notch" (or similar name) with an island
2. Another player opens grid GUI and clicks search
3. Enter partial name like "Not" (first 3 letters)
4. Verify it finds "Notch" and centers on their island

**Expected Results:**
- Partial name matching works
- First matching player is found
- Viewport centers correctly
- Success message shows full player name

---

### Test #39: Search function shows error for player not found
**Status:** READY FOR TESTING
**Steps:**
1. Open grid GUI
2. Click search button
3. Enter a player name that doesn't exist (e.g., "NonExistentPlayer123")
4. Verify an error message is shown

**Expected Results:**
- Error message: "Player 'NonExistentPlayer123' not found or has no island."
- Helpful hint: "Make sure the player name is spelled correctly."
- Grid GUI reopens (search doesn't close GUI)

---

### Test #40: Search cancel functionality
**Status:** BONUS TEST (Not in feature_list.json)
**Steps:**
1. Open grid GUI
2. Click search button
3. Type "cancel" in chat
4. Verify search is cancelled

**Expected Results:**
- Message: "Search cancelled."
- Grid GUI reopens
- No search performed

---

## How Search Works

### User Flow:
1. Player clicks COMPASS button in grid GUI
2. GUI closes, search prompt appears in chat
3. Player types a name in chat (chat is intercepted, not broadcast)
4. System searches for player:
   - First tries exact match (online players)
   - Then tries partial match (starts with)
   - Then searches all grid locations (offline players)
5. If found: viewport centers, GUI reopens, success message shown
6. If not found: error message, GUI reopens

### Technical Details:
- Uses `AsyncPlayerChatEvent` to capture input
- Chat messages during search are cancelled (not broadcast)
- Supports both online and offline players
- Searches grid locations by owner name for offline players
- Thread-safe with async to sync task switching

---

## Complete Test Command Reference

| Command | Description |
|---------|-------------|
| `/islandselector` | Open grid GUI |
| `/island` | Create island (triggers claim GUI for new players) |
| `/islandselector admin info A1` | Check location info |
| `/islandselector admin reserve A1` | Reserve a location |

---

## All Previously Passing Tests

All previous tests (1-35) should still pass:
- ✅ GUI layout and display
- ✅ Navigation (scrolling)
- ✅ Filters (All, Available, Online)
- ✅ Find My Island button
- ✅ Admin commands (reserve, setprice, info, reload)
- ✅ Island creation interception
- ✅ Grid location selection
- ✅ Blueprint selection
- ✅ Database persistence

---

## Known Limitations

The following features are **not yet implemented**:
1. Island Relocation - Shows "coming soon" message
2. Slot Switching - GUI shows slots but switching logic pending
3. Visit/Warp to islands - Not implemented yet
4. Level Display - Level addon integration pending
5. Economy integration - Vault integration pending

---

## What to Report

When testing search functionality, please report:
1. ✅ Does search button appear correctly?
2. ✅ Does search prompt show in chat when clicked?
3. ✅ Does exact name search work?
4. ✅ Does partial name search work?
5. ✅ Does offline player search work?
6. ✅ Does "not found" error show correctly?
7. ✅ Does viewport center correctly on found islands?
8. ✅ Does cancel work?
9. ❌ Any console errors during search?
10. ❌ Any chat broadcast issues?

---

## Next Steps After Testing

Once search tests pass, priority features for next session:
1. **Island Relocation** - Copy island to new location with FAWE
2. **Slot System** - Implement slot switching with schematic save/load
3. **Visit/Warp** - Click islands to visit them
4. **Level Integration** - Show island levels in tooltips
