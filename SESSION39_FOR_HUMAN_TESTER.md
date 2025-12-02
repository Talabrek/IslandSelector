# Session 39 - For Human Tester

## Quick Summary

**Feature:** Player Search Functionality
**Status:** ✅ ALREADY IMPLEMENTED - Needs Testing
**Purpose:** This session documents existing player search features
**Build Status:** ✅ NO CHANGES (Documentation only)
**Your Task:** Test the player search functionality in the grid GUI

---

## What This Session Did

**Discovery:** Session 39 found that **Player Search** functionality was already fully implemented in previous sessions but never verified by human testing.

**No new code** was added - this session documents what already exists.

---

## What to Test

Four search-related features ready for testing:

1. **Search Button Display** - Compass button in grid GUI bottom row
2. **Search Finds Player** - Exact name search centers on player's island
3. **Partial Name Matching** - Search handles partial names (e.g., "Not" finds "Notch")
4. **Player Not Found** - Error message when player doesn't exist

---

## Quick 5-Minute Test

### Setup
Copy JAR: `output/IslandSelector-1.0.0-SNAPSHOT.jar` → `plugins/BentoBox/addons/`
(Note: JAR is unchanged from Session 38 - 180K size)

### Core Test Flow

**Test Search Button Display:**

```
1. Open grid GUI: /islandselector
2. Look at bottom row (slots 47-51)
3. Expected: COMPASS item at slot 47
4. Hover over compass
5. Expected: "Search Player" tooltip with "Find a player's island" description
```

**Test Search Function:**

```
Setup:
1. Have 2 players on server
2. Player 1 (TestPlayer) creates island
3. Player 2 opens grid GUI

Test:
1. Player 2: Click compass button
2. GUI closes, chat instructions appear
3. Expected messages:
   - "=== Island Search ==="
   - "Type a player name to search..."
   - "Type 'cancel' to cancel..."
   - "Partial names are supported..."
4. Type: TestPlayer
5. Expected: Grid GUI reopens, centered on TestPlayer's island
6. Expected: Success message with island coordinates
```

**Test Partial Name:**

```
1. Player has island (name: Notch)
2. Another player opens grid, clicks search
3. Type: Not
4. Expected: Finds "Notch" and centers on their island
```

**Test Not Found:**

```
1. Open grid GUI, click search
2. Type: NonexistentPlayer999
3. Expected: Error message "Player 'NonexistentPlayer999' not found or has no island"
4. Expected: Grid GUI reopens
```

---

## Detailed Test Cases

### Test #1: Search Button Displays with Compass Item

**Category:** UI
**File:** MainGridGUI.java (lines 548-550)

**Steps:**
1. Open grid GUI with `/islandselector`
2. Locate the search button in bottom row
3. Verify it shows as COMPASS item
4. Verify tooltip indicates it's for searching players

**Expected Result:**
- Compass appears at slot 47 (bottom left area)
- Tooltip shows "Search Player" in yellow/gold
- Subtitle shows "Find a player's island" in gray

**Implementation Details:**
- Material: `COMPASS`
- Display Name: `&eSearch Player`
- Lore: `&7Find a player's island`
- Slot: `BOT_SEARCH_SLOT` (47)

---

### Test #2: Search Function Finds Player and Centers Grid

**Category:** Functional
**File:** SearchListener.java (lines 87-148)

**Setup:**
1. Have a player named 'TestPlayer' with an island at any location
2. Open grid GUI as a different player

**Steps:**
1. Click the search button (compass)
2. GUI closes, search instructions appear in chat
3. Type 'TestPlayer' in chat
4. Verify grid viewport centers on TestPlayer's island
5. Verify TestPlayer's island is visible in the viewport

**Expected Result:**
- Chat event is cancelled (search input doesn't broadcast)
- Grid GUI reopens automatically
- Viewport centered on TestPlayer's island location
- Success message: "Found TestPlayer's island at X,Z!"
- Message: "Centering view on their island..."

**Implementation Details:**
- Search uses `Bukkit.getPlayerExact()` for exact match
- Falls back to online players with `startsWith()` for partial match
- If offline, searches through `GridManager.getAllLocations()` by owner name
- Centers viewport using `MainGridGUI.centerViewportOn(GridCoordinate)`
- Async chat event → sync task for GUI operations

---

### Test #3: Search Function Handles Partial Name Matching

**Category:** Functional
**File:** SearchListener.java (lines 95-103, 153-166)

**Setup:**
1. Have a player named 'Notch' with an island
2. Open grid GUI as another player

**Steps:**
1. Click search button
2. Enter partial name like 'Not'
3. Verify it finds 'Notch' and centers on their island

**Expected Result:**
- Partial match works for online players
- Partial match works for offline players (searches GridLocationData)
- Case-insensitive matching (uses `.toLowerCase()`)
- First match wins if multiple players match

**Implementation Details:**
- Online players: `player.getName().toLowerCase().startsWith(lowerSearch)`
- Offline players: `ownerName.toLowerCase().startsWith(lowerSearch)`
- Searches all known grid locations from database

---

### Test #4: Search Function Shows Error for Player Not Found

**Category:** Functional
**File:** SearchListener.java (lines 121-127)

**Steps:**
1. Open grid GUI
2. Click search button
3. Enter a player name that doesn't exist (e.g., "NonexistentPlayer999")
4. Verify an error message is shown indicating player not found

**Expected Result:**
- Error message: "Player 'NonexistentPlayer999' not found or has no island."
- Help message: "Make sure the player name is spelled correctly."
- Grid GUI reopens automatically
- No viewport change

**Implementation Details:**
- Checks online players first
- Checks offline players via grid location owner names
- If no match found, shows error and reopens GUI
- If player exists but has no island, shows different message:
  "Player TestPlayer doesn't have an island."

---

## Advanced Test Cases

### Test: Cancel Search

**Steps:**
1. Open grid GUI, click search
2. Type: cancel
3. Expected: "Search cancelled." message
4. Expected: Grid GUI reopens

**Implementation:** SearchListener.java lines 68-75

---

### Test: Search Offline Player

**Setup:**
1. Player "OfflinePlayer" creates island
2. OfflinePlayer disconnects
3. Another player searches

**Steps:**
1. Click search
2. Type: OfflinePlayer
3. Expected: Finds island by scanning GridLocationData
4. Expected: Centers on island successfully

**Implementation:** SearchListener.java lines 108-119, 153-166

---

### Test: Player With No Island

**Setup:**
1. Player "NoIslandPlayer" on server (no island created)
2. Another player searches

**Steps:**
1. Click search
2. Type: NoIslandPlayer
3. Expected: "Player NoIslandPlayer doesn't have an island."
4. Expected: Grid GUI reopens

**Implementation:** SearchListener.java lines 132-137

---

## Configuration Notes

**No config required** - Search works out of the box

**Permissions:**
- No specific permission required to use search
- Players need basic `islandselector.island.view` permission

---

## Admin Commands for Testing

No admin commands needed for search testing.

**Helper commands:**
```bash
# Check if player has island
/islandselector admin info <coord>

# Verify search listener is registered
/bentobox version
(Check that IslandSelector is loaded)
```

---

## Expected Behaviors

### Search Flow
1. Player opens grid GUI (`/islandselector`)
2. Player clicks COMPASS button (slot 47)
3. GUI closes, chat listener activates
4. Instructions appear in chat
5. Player types name in chat
6. Chat event is cancelled (not broadcast to other players)
7. Search performed on main thread
8. GUI reopens with centered viewport (if found)
9. Success or error message shown

### Chat Interception
- Search input is **not broadcast** to other players
- Uses `AsyncPlayerChatEvent` with `LOWEST` priority
- Event cancelled if player has active search session
- Session removed after search completes or cancelled

### Name Matching
- **Exact match:** "Notch" → finds "Notch"
- **Partial match:** "Not" → finds "Notch"
- **Case insensitive:** "notch", "NOTCH", "NoTcH" all find "Notch"
- **Online priority:** Checks online players first, then offline
- **First match:** If multiple matches, first one is used

---

## Report Format

After testing, please report results:

```
=== Session 39: Player Search Test Report ===

Test #1 (Search button displays): [PASS/FAIL]
Test #2 (Search finds player): [PASS/FAIL]
Test #3 (Partial name matching): [PASS/FAIL]
Test #4 (Player not found error): [PASS/FAIL]

Additional Tests:
- Cancel search: [PASS/FAIL]
- Search offline player: [PASS/FAIL]
- Player with no island: [PASS/FAIL]

Issues found:
[Describe any problems, unexpected behavior, or bugs]

Console errors:
[Paste any errors from server console]

Overall: [PASS/FAIL]
```

---

## Important Notes

- **Multiple players required** - Need at least 2 players to test search
- **Search input is private** - Chat is cancelled, not visible to others
- **Offline player search** - Works by scanning database, not just online players
- **Case insensitive** - "notch" and "NOTCH" both work
- **Partial matching** - "Not" finds "Notch"
- **Type 'cancel'** - Exits search mode and reopens GUI

---

## Troubleshooting

**Problem:** Search button not visible
- **Check:** Open `/islandselector` (main grid GUI, not slots GUI)
- **Check:** Look at slot 47 (bottom row, left side)
- **Check:** Compass should be there between navigation arrows

**Problem:** Click compass does nothing
- **Check:** Console for errors
- **Check:** SearchListener registered? Look for registration message on startup
- **Check:** Permission issues? (unlikely, should work by default)

**Problem:** Typing search term broadcasts to chat
- **Check:** Did search session start? (should see instructions in chat)
- **Check:** Console for event handler errors
- **Check:** Try clicking compass again

**Problem:** Search says "not found" but player exists
- **Check:** Does player actually have an island? (might be online but no island)
- **Check:** Spelling of name (case doesn't matter, but spelling does)
- **Check:** Try exact full name instead of partial

**Problem:** GUI doesn't reopen after search
- **Check:** Console for errors (inventory open failures)
- **Check:** Try `/islandselector` manually to reopen
- **Check:** Server TPS (lag might delay GUI opening)

---

## Next After This

**If All Search Tests Pass:**
- Mark tests #41-44 as passing ✅ (approximate test numbers)
- Progress: ~48/225 tests passing
- Move to next feature block (likely island claiming Tests #44-50)

**If Any Test Fails:**
- Report the issue with details
- Agent will debug and fix
- Re-test until passing

---

## File Info

**JAR:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
**Size:** 180K (unchanged from Session 38)
**Changes:** None - documentation only
**Build Status:** ✅ SUCCESS

---

## Key Files Involved

**Already Implemented:**
- `SearchListener.java` (204 lines) - Chat interception and search logic
- `MainGridGUI.java` (lines 548-550) - Search button creation
- `GridGUIListener.java` (lines 82-86) - Search button click handler
- `IslandSelector.java` (lines 140-141) - Listener registration
- `GridManager.java` - getAllLocations(), getPlayerIslandCoordinate()

---

**Ready to test!** 🔍

**Core Test:** Open grid → Click compass → Type player name → Grid centers on their island

**Time:** 5-10 minutes

**Required:** 2+ players on server

**JAR:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`

---

## Success Criteria

✅ Compass button visible in grid GUI bottom row
✅ Click compass closes GUI and shows search instructions
✅ Type exact player name finds their island
✅ Type partial name (e.g., "Not") finds player (e.g., "Notch")
✅ Type non-existent name shows error message
✅ Search input is not broadcast to other players
✅ Grid GUI reopens automatically after search
✅ Viewport centered on found island
✅ Type "cancel" exits search mode
✅ No console errors during search operations

**This complements Session 38's island claiming tests** - two major feature sets ready for verification!
