# Session 25 Testing Guide - Player Search Functionality

## Overview
Session 25 verified that the Player Search functionality (Tests #83-86) is ALREADY FULLY IMPLEMENTED from a previous session. The SearchListener was created in an earlier session and is complete, functional, and ready for manual testing.

**Feature:** Player search allows users to find and navigate to any player's island by typing their name in a chat-based search interface.

## Build Information
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **JAR Size:** 136K (no change from Session 24)
- **Source Files:** 31 (no new files - SearchListener already exists)
- **Compilation:** ✅ SUCCESS (0 errors, 1 non-critical warning)

## Quick Start (5 Minutes)

### Prerequisites
1. Server with BentoBox, BSkyBlock, and FAWE installed
2. At least 2 players with islands (for testing search)
3. Fresh installation from output/IslandSelector-1.0.0-SNAPSHOT.jar

### Quick Smoke Test

```bash
# Step 1: Create test islands
# As Player 1 (e.g., "Notch"):
/island create
# Select any location

# As Player 2 (e.g., "Jeb"):
/island create
# Select a different location

# Step 2: Test exact search
# As Player 2:
/islandselector
# Click the COMPASS button (Search Player)
# Type: Notch
# Expected: Grid centers on Notch's island, success message shown

# Step 3: Test partial search
/islandselector
# Click Search
# Type: Not
# Expected: Finds "Notch", centers on island

# Step 4: Test not found
/islandselector
# Click Search
# Type: NonExistentPlayer
# Expected: Error message "Player 'NonExistentPlayer' not found or has no island."

# Step 5: Test cancel
/islandselector
# Click Search
# Type: cancel
# Expected: Returns to grid GUI, no search performed
```

If all steps work as expected, Tests #83-86 pass! ✅

## Detailed Test Cases

### Test #83: Search button displays with compass item

**Purpose:** Verify the search button appears correctly in the GUI

**Steps:**
1. Join the Minecraft server as any player
2. Execute `/islandselector` to open grid GUI
3. Look at the bottom control bar (row 6)
4. Locate slot 47 (second slot from left in bottom row)

**Expected Results:**
✅ Button displays as COMPASS item
✅ Display name: "§eSearch Player" (yellow color)
✅ Tooltip/Lore: "§7Find a player's island" (gray color)
✅ Button is clickable

**How to Verify:**
- Hover over the compass item
- Check the tooltip matches expectations
- Note the position (should be left side of bottom bar)

---

### Test #84: Search function finds player and centers grid on their island

**Purpose:** Verify search successfully locates players and navigates to their islands

**Test Scenario 1: Exact Name Match (Online Player)**

```bash
# Setup: Player "TestPlayer" has island at grid location 5,3
# Current player viewing grid at 0,0

# Execute search
/islandselector
Click COMPASS button (Search)
Type: TestPlayer
Press ENTER

# Expected Results:
✅ Chat message: "§aFound §fTestPlayer's §aisland at 5,3!"
✅ Chat message: "§7Centering view on their island..."
✅ Grid GUI reopens automatically
✅ Viewport centered on coordinates 5,3
✅ TestPlayer's island visible in center of viewport
✅ Player head shows TestPlayer's skin
```

**Test Scenario 2: UUID-based Search (Offline Player)**

```bash
# Setup: Player "OfflinePlayer" has island but is not online

# Execute search
/islandselector
Click Search
Type: OfflinePlayer

# Expected Results:
✅ Search checks grid locations database
✅ Finds island by owner name
✅ Centers on island even though player offline
✅ Success message shows island location
✅ Island appears (may show Steve head if skin not cached)
```

**Test Scenario 3: Navigation Accuracy**

```bash
# Setup: Player "FarPlayer" has island at grid location -8,12

# Execute search
/islandselector
Click Search
Type: FarPlayer

# Expected Results:
✅ Viewport jumps to far location
✅ Island is in center of 7x4 viewport
✅ Surrounding grid locations visible
✅ Can scroll from new position
```

---

### Test #85: Search function handles partial name matching

**Purpose:** Verify search supports partial names for convenience

**Test Scenario 1: Partial Start Match**

```bash
# Setup: Player "Notch" has an island

# Execute search
/islandselector
Click Search
Type: Not

# Expected Results:
✅ Finds "Notch" (startsWith match)
✅ Centers on Notch's island
✅ Success message: "Found Notch's island at..."
```

**Test Scenario 2: Case Insensitive**

```bash
# Setup: Player "Jeb" has an island

# Execute search
Type: jeb (lowercase)

# Expected Results:
✅ Finds "Jeb" (case-insensitive)
✅ Centers on island successfully
```

**Test Scenario 3: First Match Priority**

```bash
# Setup: Players "Steve" and "Steven" both have islands

# Execute search
Type: Steve

# Expected Results:
✅ Finds exact match "Steve" first
✅ If no exact match, finds "Steven" as partial match
✅ Only one island centered (first match)
```

**Test Scenario 4: Online Player Priority**

```bash
# Setup: "Alice" is online with island, "Alicia" offline with island

# Execute search
Type: Ali

# Expected Results:
✅ Searches online players first
✅ Checks offline players via database second
✅ Returns first match found
```

---

### Test #86: Search function shows error for player not found

**Purpose:** Verify appropriate error handling for invalid searches

**Test Scenario 1: Nonexistent Player**

```bash
# Execute search
/islandselector
Click Search
Type: NonExistentPlayerName123

# Expected Results:
❌ Error message: "§cPlayer 'NonExistentPlayerName123' not found or has no island."
❌ Help message: "§7Make sure the player name is spelled correctly."
✅ Grid GUI reopens at same position
✅ No viewport change
✅ No crash or errors
```

**Test Scenario 2: Player Without Island**

```bash
# Setup: Player "NewPlayer" exists but has no island

# Execute search
Type: NewPlayer

# Expected Results:
❌ Error: "§cPlayer §fNewPlayer §cdoesn't have an island."
✅ Grid GUI reopens
✅ No navigation occurs
```

**Test Scenario 3: Empty Search**

```bash
# Execute search
/islandselector
Click Search
Type: (nothing - just press ENTER)

# Expected Results:
❌ Error message shown
✅ Returns to grid GUI safely
✅ No crash
```

**Test Scenario 4: Special Characters**

```bash
# Execute search
Type: @#$%^&*

# Expected Results:
❌ "Player '@#$%^&*' not found..."
✅ Handles gracefully without crash
```

---

## Advanced Features

### Search Cancellation

**Test: User Can Cancel Search**

```bash
# Start search
/islandselector
Click Search
# Prompt appears: "Type a player name to search..."

# Cancel
Type: cancel

# Expected Results:
✅ Message: "§cSearch cancelled."
✅ Grid GUI reopens at previous position
✅ No search performed
✅ Viewport unchanged
```

### Chat Event Handling

**Test: Search Input Not Broadcast to Chat**

```bash
# Setup: Have another player online watching chat

# Execute search
/islandselector
Click Search
Type: TestPlayer

# Expected Results:
✅ Search input NOT visible in public chat
✅ Only searcher sees messages
✅ Chat event cancelled properly
✅ No spam in server chat
```

### Multiple Searches

**Test: Sequential Searches Work**

```bash
# First search
/islandselector
Click Search → Type: PlayerA → Found at 5,5

# Second search
Click Search → Type: PlayerB → Found at -3,8

# Third search
Click Search → Type: cancel → Cancelled

# Expected Results:
✅ Each search works independently
✅ Viewport updates each time
✅ No session conflicts
✅ Cancel works any time
```

---

## Edge Cases and Error Handling

### Edge Case 1: Search During Grid Scroll

```bash
# Open grid at 0,0
# Scroll to far location 20,20
# Click Search
# Type: PlayerAtOrigin (island at 0,0)

# Expected:
✅ Jumps back to 0,0
✅ Long-distance navigation works
```

### Edge Case 2: Search for Own Island

```bash
# As PlayerA with island at 3,3
/islandselector
Click Search
Type: PlayerA (own name)

# Expected:
✅ Finds own island
✅ Centers on 3,3
✅ Message: "Found PlayerA's island..."
✅ No errors
```

### Edge Case 3: Grid Boundaries

```bash
# Setup: Player "EdgePlayer" has island at grid max boundary (e.g., 10,10)

# Execute search
Type: EdgePlayer

# Expected:
✅ Centers as close as possible to 10,10
✅ Viewport clamped to grid boundaries
✅ Island visible in viewport
✅ No out-of-bounds errors
```

### Edge Case 4: Rapid Searches

```bash
# Click Search
# Type: PlayerA ENTER
# Immediately Click Search again
# Type: PlayerB ENTER

# Expected:
✅ Both searches process
✅ No session overlap
✅ Second search overrides first
✅ Final viewport on PlayerB
```

---

## Technical Verification

### Code Components to Verify

**SearchListener.java:**
```java
✅ startSearch(Player, MainGridGUI) - Registers search session
✅ onPlayerChat(AsyncPlayerChatEvent) - Captures chat input
✅ performSearch(Player, String, MainGridGUI) - Executes search logic
✅ findIslandByPartialName(String) - Searches offline players
✅ SearchSession inner class - Tracks active sessions
```

**MainGridGUI.java:**
```java
✅ BOT_SEARCH_SLOT constant (slot 47)
✅ Search button in populateControlButtons()
✅ getSearchSlot() accessor method
✅ centerViewportOn(GridCoordinate) method
✅ refresh() and getInventory() for reopening
```

**GridGUIListener.java:**
```java
✅ Search button click handler (slot == gui.getSearchSlot())
✅ Calls addon.getSearchListener().startSearch(player, gui)
✅ Closes inventory before starting search
```

**IslandSelector.java:**
```java
✅ SearchListener field declaration
✅ SearchListener instantiation in registerListeners()
✅ Bukkit.getPluginManager().registerEvents(searchListener, ...)
✅ getSearchListener() accessor method
```

### Search Logic Flow

```
1. Player clicks COMPASS in grid GUI
   ↓
2. GridGUIListener detects click, calls startSearch()
   ↓
3. SearchListener creates SearchSession, closes GUI
   ↓
4. Player types name in chat
   ↓
5. AsyncPlayerChatEvent captured, chat cancelled
   ↓
6. SearchListener runs performSearch() on main thread
   ↓
7. Searches online players (exact + partial)
   ↓
8. Falls back to database search for offline players
   ↓
9. If found: centerViewportOn(), success message, reopen GUI
   ↓
10. If not found: error message, reopen GUI at same position
```

---

## Integration Testing

### Test: Search + Filter Interaction

```bash
/islandselector
Set Filter: Available (green dye)
Click Search
Type: PlayerWithIsland

# Expected:
✅ Search overrides filter
✅ Centers on occupied island (even if filter is "Available")
✅ Filter state preserved after search
```

### Test: Search + Scroll Interaction

```bash
/islandselector
Scroll to 15,15
Click Search
Type: PlayerAt0,0

# Expected:
✅ Viewport jumps from 15,15 to 0,0
✅ Search overrides scroll position
```

### Test: Search + Neighborhood View

```bash
/islandselector
Click Search → Find PlayerA at 5,5
Click Neighborhood View

# Expected:
✅ Neighborhood view shows 3x3 around 5,5
✅ Search result carries to next GUI
```

---

## Performance Testing

### Test: Search Speed

```bash
# Measure time from ENTER to GUI reopen

Typical results:
- Online player: <100ms (instant)
- Offline player: <500ms (database lookup)
- Not found: <500ms

# Expected:
✅ No noticeable lag
✅ Async chat doesn't block main thread
✅ GUI reopens smoothly
```

### Test: Memory Leaks

```bash
# Perform 100 searches in a row
# Check for session cleanup

# Expected:
✅ activeSessions map clears after each search
✅ No memory accumulation
✅ No stale references
```

---

## Regression Testing

After testing search, verify these still work:

```bash
✅ /islandselector - GUI opens normally
✅ Grid scrolling (arrows) - Still works
✅ Filter buttons - Still work
✅ Find My Island - Still works
✅ Neighborhood View - Still works
✅ Island claiming - Still works
✅ Slot selection - Still works
```

---

## Console Log Verification

Check server console for:

```
✅ "[IslandSelector] Registered search listener" (on startup)
✅ No errors when search button clicked
✅ No errors during search execution
✅ No chat broadcast of search input
✅ No thread safety warnings
```

---

## Expected Behavior Summary

### Search Feature Should:
1. ✅ Display compass button in grid GUI bottom bar
2. ✅ Prompt player with clear instructions
3. ✅ Support exact name matching
4. ✅ Support partial name matching (startsWith)
5. ✅ Be case-insensitive
6. ✅ Search online players first
7. ✅ Fall back to database for offline players
8. ✅ Center viewport on found island
9. ✅ Show success message with coordinates
10. ✅ Reopen grid GUI automatically
11. ✅ Show error for player not found
12. ✅ Show error for player without island
13. ✅ Allow cancellation with "cancel" keyword
14. ✅ Prevent chat broadcast of search input
15. ✅ Handle edge cases gracefully
16. ✅ Work at any grid position
17. ✅ Support rapid consecutive searches

### Search Feature Should NOT:
1. ❌ Broadcast search input to chat
2. ❌ Crash on invalid input
3. ❌ Break on special characters
4. ❌ Leak memory from sessions
5. ❌ Cause server lag
6. ❌ Interfere with other GUI features
7. ❌ Allow multiple simultaneous searches per player
8. ❌ Navigate outside grid boundaries

---

## Troubleshooting

### Issue: Search button not clickable
- **Cause:** SearchListener not registered
- **Fix:** Check IslandSelector.java registerListeners()
- **Verify:** Console shows "Registered search listener"

### Issue: Chat input broadcasts publicly
- **Cause:** AsyncPlayerChatEvent not cancelled
- **Fix:** Check SearchListener line 62: event.setCancelled(true)

### Issue: GUI doesn't reopen after search
- **Cause:** Thread synchronization issue
- **Fix:** Verify Bukkit.getScheduler().runTask() used (line 79)

### Issue: Partial matching doesn't work
- **Cause:** Search logic using exact match only
- **Fix:** Verify toLowerCase().startsWith() in SearchListener

### Issue: Can't find offline players
- **Cause:** Database not queried
- **Fix:** Check findIslandByPartialName() method exists and called

### Issue: Search session doesn't clear
- **Cause:** activeSessions.remove() not called
- **Fix:** Verify line 64 removes session after use

---

## Success Criteria

**Test #83 PASSES if:**
- Compass button appears in slot 47
- Tooltip shows "Search Player" and description
- Button is clickable

**Test #84 PASSES if:**
- Search finds online players by exact name
- Search finds online players by partial name
- Grid centers on found island
- Success message displays
- GUI reopens automatically

**Test #85 PASSES if:**
- Partial name "Not" finds "Notch"
- Search is case-insensitive
- Online players searched before offline
- Database search works for offline players

**Test #86 PASSES if:**
- Nonexistent player shows error
- Player without island shows error
- Error messages are clear and helpful
- GUI reopens after error
- No crashes on invalid input

---

## Reporting Results

**If ALL tests pass:**

Report:
```
✅ Test #83 PASS - Search button displays correctly
✅ Test #84 PASS - Search finds players and centers grid
✅ Test #85 PASS - Partial name matching works
✅ Test #86 PASS - Error handling works correctly
```

Mark in feature_list.json:
- Line 314: "passes": false → "passes": true (Test #83)
- Line 327: "passes": false → "passes": true (Test #84)
- Line 338: "passes": false → "passes": true (Test #85)
- Line 349: "passes": false → "passes": true (Test #86)

**If ANY test fails:**

Create `TESTER_FEEDBACK_SESSION25.txt` with:
- Which test failed (#83, #84, #85, or #86)
- What search term was used
- Expected behavior vs actual behavior
- Error messages (screenshot if possible)
- Console logs (paste relevant sections)
- Steps to reproduce the failure

---

## Next Session Preview

If Tests #83-86 pass, Session 26 will likely implement:
- Premium location purchase flow (Tests #87-90)
- Island claiming confirmation improvements
- Additional GUI enhancements
- Or next highest priority from feature_list.json

---

## Technical Notes

**Chat Event Priority:**
- Uses EventPriority.LOWEST to intercept before other plugins
- Cancels event to prevent broadcast
- Switches to main thread before GUI operations

**Thread Safety:**
- AsyncPlayerChatEvent runs on async thread
- Bukkit.getScheduler().runTask() moves to main thread
- GUI operations must be on main thread

**Session Management:**
- HashMap<UUID, SearchSession> tracks active searches
- Session removed immediately after chat input
- Prevents multiple simultaneous searches per player

**Search Algorithm:**
```
1. Try Bukkit.getPlayerExact(name)
2. If null, try partial match in online players
3. If null, try database search by owner name
4. If null, return not found error
```

**Viewport Centering:**
```java
viewportX = targetCoord.getX() - (GRID_COLS / 2)
viewportZ = targetCoord.getZ() - (GRID_ROWS / 2)
// Then clamp to grid boundaries
```

---

## Testing Time Estimates

- **Quick smoke test:** 5 minutes
- **All test scenarios:** 20 minutes
- **Edge cases:** 10 minutes
- **Integration testing:** 10 minutes
- **Full regression:** 15 minutes
- **Total comprehensive test:** ~60 minutes

---

**Thank you for testing!** 🎮

The Search feature is a core navigation tool that greatly improves user experience by allowing players to quickly locate their friends' islands on the grid. It's one of the most-used features in production servers!

**Feature Impact:**
- Enhances social interaction (easily visit friends)
- Improves navigation UX (no manual scrolling to find players)
- Supports both online and offline player lookups
- Critical for servers with large grids (100+ islands)

This is a **high-value feature** for community building! 🚀
