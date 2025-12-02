# IslandSelector - Session 17 Testing Instructions
## Search Functionality

## Build Information
- **Session:** 17
- **Feature:** Player Search Functionality
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **JAR Size:** 120K
- **Status:** ✅ Search functionality already implemented, ready for testing

---

## What Was Discovered in This Session

**IMPORTANT:** The search functionality was already fully implemented in a previous session!

The following components were found to be complete:
1. ✅ Search button (compass) displays in grid GUI bottom bar
2. ✅ SearchListener class handles chat-based search input
3. ✅ Partial name matching is supported
4. ✅ Viewport centering on found islands
5. ✅ Error handling for players not found
6. ✅ Support for both online and offline players
7. ✅ Cancellation support

This session verified the implementation and prepared testing instructions.

---

## Features to Test

### Test #28: Search Button Display
**Category:** UI
**Status:** Ready for testing

**Steps:**
1. Join the Minecraft server
2. Run `/islandselector` to open the grid GUI
3. Look at the bottom row of the GUI
4. Find the search button (should be a COMPASS item)

**Expected Results:**
- ✅ Compass item appears in slot 47 (bottom bar, position 2)
- ✅ Display name: "&eSearch Player" (yellow text)
- ✅ Tooltip: "&7Find a player's island" (gray text)
- ✅ Button is clickable

---

### Test #29: Search Function - Find Player by Name
**Category:** Functional
**Status:** Ready for testing

**Setup:**
1. Create two player accounts (or have a friend help)
2. Have Player A create an island at any location (e.g., F10)
3. Log in as Player B

**Steps:**
1. As Player B, open grid GUI with `/islandselector`
2. Click the search button (compass)
3. GUI should close and prompt for input
4. Type Player A's exact name in chat (e.g., "TestPlayer")
5. Press Enter

**Expected Results:**
- ✅ Chat message: "§e§l=== Island Search ===" with instructions
- ✅ Instructions mention typing player name or "cancel"
- ✅ Instructions mention partial names are supported
- ✅ After entering name, see: "§aFound §f[Name]§'s island at [coordinate]!"
- ✅ Grid GUI reopens automatically
- ✅ Viewport is centered on Player A's island
- ✅ Player A's island is visible in the grid
- ✅ Your search input is NOT broadcast to other players

---

### Test #30: Search Function - Partial Name Matching
**Category:** Functional
**Status:** Ready for testing

**Setup:**
1. Have a player named "Notch" with an island (or use any player name)

**Steps:**
1. Open grid GUI
2. Click search button
3. Type partial name (e.g., "Not" for "Notch")
4. Press Enter

**Expected Results:**
- ✅ System finds "Notch" even though you only typed "Not"
- ✅ Message: "§aFound §fNotch's island at [coordinate]!"
- ✅ Viewport centers on their island
- ✅ Grid GUI reopens with their island visible

**Additional Tests:**
- Try searching "Test" when player is named "TestPlayer" - should match
- Try searching "Ste" when player is named "Steve" - should match
- Partial matching is case-insensitive ("not" should find "Notch")

---

### Test #31: Search Function - Player Not Found Error
**Category:** Functional
**Status:** Ready for testing

**Steps:**
1. Open grid GUI
2. Click search button
3. Type a player name that doesn't exist: "NonExistentPlayer123"
4. Press Enter

**Expected Results:**
- ✅ Error message: "§cPlayer 'NonExistentPlayer123' not found or has no island."
- ✅ Helpful message: "§7Make sure the player name is spelled correctly."
- ✅ Grid GUI reopens automatically
- ✅ Viewport position unchanged from before search

---

## Additional Test Cases

### Test #32: Search for Player Without Island
**Steps:**
1. Have a player join the server but NOT create an island
2. Search for that player by name

**Expected Results:**
- ✅ Message: "§cPlayer §f[Name] §cdoesn't have an island."
- ✅ Grid GUI reopens
- ✅ No error in console

---

### Test #33: Search Cancellation
**Steps:**
1. Open grid GUI
2. Click search button
3. Type "cancel" (case insensitive)
4. Press Enter

**Expected Results:**
- ✅ Message: "§cSearch cancelled."
- ✅ Grid GUI reopens immediately
- ✅ Viewport position unchanged

---

### Test #34: Search for Offline Player
**Steps:**
1. Have a player create an island then log out
2. Log in as a different player
3. Search for the offline player by name

**Expected Results:**
- ✅ System finds the offline player's island
- ✅ Message: "§aFound island at [coordinate]!" (slightly different message)
- ✅ Viewport centers on their island
- ✅ Grid shows their island with "○ Offline" indicator

---

### Test #35: Search During Filter Active
**Steps:**
1. Open grid GUI
2. Set filter to "Available" or "Online"
3. Click search button
4. Search for a player
5. Verify found island is visible

**Expected Results:**
- ✅ Search works regardless of active filter
- ✅ Found island is visible even if it would normally be filtered out
- ✅ After search, filter is still active

---

## How the Search System Works

### User Flow:
1. Player clicks compass (search button) in grid GUI
2. GUI closes and SearchListener registers a "search session"
3. Player types name in chat
4. Chat event is intercepted and cancelled (not broadcast)
5. System searches for matching player:
   - First tries exact name match among online players
   - Then tries partial name match among online players
   - Then searches all grid locations for offline players
6. If found: Centers viewport and reopens GUI
7. If not found: Shows error and reopens GUI

### Technical Details:
- **Chat-based input:** Uses Minecraft chat for search input
- **Async handling:** Chat event is async, but GUI operations run on main thread
- **Partial matching:** Uses `startsWith()` for partial name matching
- **Case insensitive:** Converts names to lowercase before comparing
- **No broadcast:** Chat input is cancelled so other players don't see it

---

## Configuration Options

Currently no configuration options for search functionality.
Search behavior is controlled by the SearchListener class.

Possible future config options:
- Enable/disable search feature
- Minimum name length for search
- Search radius limit
- Search cooldown

---

## Regression Testing

After testing search functionality, verify these still work:

### Core GUI Functions:
- ✅ Grid navigation (arrows, scroll)
- ✅ Filters (All, Available, Online)
- ✅ Find My Island button
- ✅ Clicking grid locations
- ✅ Close button

### Other Bottom Bar Buttons:
- ✅ Find My Island (ender eye) - should center on your island
- ✅ Neighborhood (map) - should open neighborhood view
- ✅ Island Slots (chest) - should open slot selection
- ✅ Close (barrier) - should close GUI

---

## Known Limitations

1. **Search requires typing:** Players must type the name in chat, not in a GUI text field (Minecraft limitation)
2. **Offline player search:** Only searches players who have registered islands (in database)
3. **No fuzzy matching:** Only prefix matching (e.g., "Not" finds "Notch", but "otch" doesn't)
4. **Single result:** If multiple players have similar names, returns first match only

---

## Potential Issues to Watch For

### Issue #1: Chat Input Broadcast
**Symptom:** Search input appears in public chat
**Cause:** Event.setCancelled(true) not working or called too late
**Expected:** Search input should NOT be visible to other players

### Issue #2: GUI Doesn't Reopen
**Symptom:** After search, GUI stays closed
**Cause:** Async/sync thread issue with inventory operations
**Expected:** GUI should automatically reopen after search completes

### Issue #3: Viewport Doesn't Center
**Symptom:** Found island isn't centered in view
**Cause:** centerViewportOn() not clamping to grid boundaries correctly
**Expected:** Found island should be in center of 7x4 grid display

### Issue #4: Partial Match Too Greedy
**Symptom:** Searching "S" matches first player with "S" in name
**Cause:** Working as designed (partial prefix matching)
**Expected:** More specific names should match more precisely

---

## Testing Checklist

Before marking tests as passing, verify:

- [ ] Test #28: Search button displays correctly
- [ ] Test #29: Search finds player by exact name
- [ ] Test #30: Partial name matching works
- [ ] Test #31: Error shown for player not found
- [ ] Test #32: Handles player without island
- [ ] Test #33: Cancel search works
- [ ] Test #34: Finds offline players
- [ ] Test #35: Search works with filters active
- [ ] Chat input is not broadcast to other players
- [ ] No console errors during any search operation
- [ ] GUI reopens correctly after all search operations
- [ ] Viewport centering is accurate

---

## Reporting Results

After testing, please report:

### ✅ What Worked:
- List which tests (28-35) passed completely
- Note any surprising or impressive behavior

### ⚠️ What Needs Fixes:
- List which tests failed
- Describe expected vs actual behavior
- Include any error messages from console

### 🐛 Bugs Found:
- Describe the issue in detail
- Steps to reproduce
- Console errors (full stack trace if available)
- Screenshots if helpful

---

## Next Steps After Testing

Once search functionality is verified:

### Priority 1: Premium Location Purchase
- Test #40: Premium location tooltip
- Tests #52-53: Purchase with economy integration

### Priority 2: Island Creation in Empty Slots
- Test #62: Create island in empty slot
- Verify slot shares same grid coordinate

### Priority 3: Slot Management Commands
- Commands to rename/delete/switch slots
- Permission-based slot unlocking
- Backup system for slot switches

---

## Questions?

If anything is unclear:
1. Check SESSION17_NOTES.txt for technical details
2. Check SearchListener.java source code (well-commented)
3. Check console for error messages
4. Report any issues found

---

## Summary

**This session verified search functionality is already complete.**

All 4 core search tests (28-31) are implemented and ready for testing.
Additional edge cases (32-35) are also covered.

The search system uses chat-based input, supports partial matching,
handles both online and offline players, and includes proper error handling.

**Ready for manual testing on Minecraft server!** 🎮
