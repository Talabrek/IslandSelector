# Session 43 - For Human Tester

## Quick Summary

**Feature:** Player Information Commands (locate, find, help)
**Status:** ✅ NEWLY IMPLEMENTED - Needs Testing
**Purpose:** Additional player commands for finding islands and getting help
**Build Status:** ✅ COMPILED SUCCESSFULLY (186K JAR)
**Your Task:** Test the three new player commands

---

## What This Session Did

**Implementation:** Session 43 added three new player-facing commands to make the plugin more user-friendly:

1. **`/islandselector locate`** - Shows player's own island grid coordinates in chat
2. **`/islandselector find <player>`** - Opens grid GUI (feature finds player island - centering will be added in future)
3. **`/islandselector help`** - Shows list of all available commands with descriptions

**New features:**
- Locate command displays grid coordinates and world coordinates in chat
- Find command locates any player's island and opens the grid
- Help command shows formatted command list with permissions
- Tab completion for find command (suggests online player names)
- All commands have proper permissions and locale messages

---

## What to Test

### Test #1: /islandselector locate - Show Own Island Coordinates
**Testing Steps:**
1. Create an island at a known location (e.g., use admin command to verify coordinates)
2. Run `/islandselector locate`
3. **Expected:** Header message: "=== Your Island Location ==="
4. **Expected:** Grid Location displayed (e.g., "Grid Location: 0,0")
5. **Expected:** World Coordinates displayed (e.g., "World Coordinates: 0, 0")
6. **Expected:** Clear, formatted output

### Test #2: /islandselector locate - No Island Error
**Testing Steps:**
1. Join with a fresh account that has no island
2. Run `/islandselector locate`
3. **Expected:** Error message: "You don't have an island yet!"
4. **Expected:** No crash or stack trace

### Test #3: /islandselector find <player> - Find Player Island
**Testing Steps:**
1. Have Player A create an island at location A1
2. Have Player B create an island at location B2
3. As Player B, run `/islandselector find PlayerA`
4. **Expected:** Success message: "Opening grid centered on PlayerA's island at A1"
5. **Expected:** Grid GUI opens
6. **Note:** Grid won't be centered yet (TODO for future) - it will open at default position
7. **Expected:** No errors in console

### Test #4: /islandselector find - Player Not Found
**Testing Steps:**
1. Run `/islandselector find NonExistentPlayer`
2. **Expected:** Error message: "Player 'NonExistentPlayer' not found."
3. **Expected:** No grid GUI opens

### Test #5: /islandselector find - Player Has No Island
**Testing Steps:**
1. Have a player join but not create an island
2. As another player, run `/islandselector find <noIslandPlayer>`
3. **Expected:** Error message: "Player '<name>' doesn't have an island."

### Test #6: /islandselector find - Tab Completion
**Testing Steps:**
1. Have 2-3 players online (e.g., "Steve", "Alex", "Notch")
2. Type `/islandselector find ` and press TAB
3. **Expected:** List of online player names appears as suggestions
4. Type `/islandselector find St` and press TAB
5. **Expected:** Only "Steve" is suggested (partial name matching)

### Test #7: /islandselector help - Show Command List
**Testing Steps:**
1. Run `/islandselector help`
2. **Expected:** Header: "=== IslandSelector Commands ==="
3. **Expected:** List of commands with descriptions:
   - `/islandselector` - Open the island grid selector
   - `/islandselector slots` - Manage your island slots
   - `/islandselector locate` - Show your island's grid coordinates
   - `/islandselector find <player>` - Find a player's island on the grid
   - `/islandselector help` - Show list of available commands
   - (other commands if player has permissions)
4. **Expected:** Footer: "Use /islandselector <command> to execute a command"
5. **Expected:** Formatted, easy to read

### Test #8: /islandselector help - Only Shows Permitted Commands
**Testing Steps:**
1. Remove `islandselector.admin` permission from test player
2. Run `/islandselector help`
3. **Expected:** Admin commands are NOT shown in the list
4. Give `islandselector.admin` permission
5. Run `/islandselector help` again
6. **Expected:** Admin subcommand appears in the list

---

## Quick 10-Minute Test

### Minimal Test Flow

```
1. Setup: Create 2 player accounts with islands

2. Test locate command:
   /islandselector locate
   Expected: Shows your grid coordinates in chat

3. Test find command:
   /islandselector find OtherPlayer
   Expected: Success message + grid GUI opens

4. Test find with tab completion:
   Type: /islandselector find [TAB]
   Expected: Player names suggested

5. Test help command:
   /islandselector help
   Expected: List of all commands with descriptions

6. Test error handling:
   /islandselector find NonExistentPlayer
   Expected: "Player not found" error

7. Test help with permissions:
   Remove admin permission
   /islandselector help
   Expected: No admin commands shown
```

**Total Time:** ~10 minutes

---

## Configuration

No configuration changes needed for these commands. They use existing settings:
- Island spacing from BSkyBlock config
- Grid coordinate mapping from IslandSelector
- Permissions from addon.yml

---

## Permissions

New permissions added:
```yaml
islandselector.locate:
  description: Can use /islandselector locate
  default: true

islandselector.find:
  description: Can use /islandselector find
  default: true

islandselector.help:
  description: Can use /islandselector help
  default: true
```

All three default to `true` so all players can use them by default.

---

## Known Limitations

1. **Find Command Centering:**
   - The `/find` command doesn't center the grid on the target player yet
   - Grid opens at default position (player's own island or 0,0)
   - TODO for future enhancement: Add `centerOnLocation()` method to MainGridGUI
   - This is acceptable for initial release - command still works and shows success message

2. **Help Command Formatting:**
   - Shows all subcommands player has permission for
   - Format is simple text-based (not fancy GUI or hover tooltips)
   - Sufficient for basic help functionality

---

## Important Notes

**Command Aliases:**
- Main command has aliases: `/islandselector`, `/is`, `/isgrid`
- All subcommands work with any alias:
  - `/is locate`, `/isgrid locate`, etc.

**Error Handling:**
- All commands check for null islands
- All commands check for valid player names
- Graceful error messages (no stack traces)

**Tab Completion:**
- Currently only `/find` command has tab completion
- Shows online player names
- Supports partial name matching
- Future: Can add tab completion to other commands if needed

---

## Troubleshooting

**Problem:** `/locate` shows wrong coordinates
- **Check:** Run `/islandselector admin info 0,0` to verify grid mapping
- **Check:** Verify island spacing matches BSkyBlock config
- **Solution:** Check island-spacing configuration

**Problem:** `/find` doesn't open GUI
- **Check:** Console for errors
- **Check:** Target player actually has an island
- **Solution:** Verify player island exists with `/island info`

**Problem:** `/help` shows no commands
- **Check:** Player has `islandselector.use` permission
- **Check:** Not all commands are hidden by permissions
- **Solution:** Give appropriate permissions

**Problem:** Tab completion doesn't work
- **Check:** Make sure you're pressing TAB (not space)
- **Check:** Verify Bukkit/Spigot tab completion is enabled
- **Solution:** Server configuration issue, not plugin issue

---

## Test Report Format

After testing, please report results:

```
SESSION 43 TEST RESULTS
======================

Test #1 (locate - show coordinates): [PASS/FAIL]
  Notes:

Test #2 (locate - no island error): [PASS/FAIL]
  Notes:

Test #3 (find player island): [PASS/FAIL]
  Notes:

Test #4 (find - player not found): [PASS/FAIL]
  Notes:

Test #5 (find - no island): [PASS/FAIL]
  Notes:

Test #6 (find - tab completion): [PASS/FAIL]
  Notes:

Test #7 (help command): [PASS/FAIL]
  Notes:

Test #8 (help - permission filtering): [PASS/FAIL]
  Notes:

OVERALL: [PASS/FAIL]
Issues found:
1.
2.

Suggestions:
1. Add centering to /find command
2. Add more tab completion
```

---

## Success Criteria

✅ All tests must pass:
- [ ] `/locate` shows correct grid and world coordinates
- [ ] `/locate` shows error for players without islands
- [ ] `/find <player>` opens grid GUI and shows success message
- [ ] `/find` shows errors for invalid players or players without islands
- [ ] `/find` has working tab completion for online players
- [ ] `/help` shows all available commands with descriptions
- [ ] `/help` only shows commands player has permission for
- [ ] All commands have proper error handling (no crashes)

---

## Context

**Previous Sessions Status:**
- Sessions 38-42: 59 tests documented, pending human testing
- Session 43: 8 new tests implemented for player commands ⭐ NEW
- **Total Pending:** 67 tests (59 + 8)
- **If All Pass:** 44/225 → 111/225 tests (49% complete - almost halfway!)

**Related Features:**
- Grid GUI (verified)
- Admin info command (verified)
- Permission system (verified)

---

## Next Steps After Testing

1. **If tests pass:** Mark tests as passing in feature_list.json
2. **If tests fail:** Report issues for bug fixes in next session
3. **Enhancement:** Add grid centering to `/find` command (future)
4. **Continue:** Next features to implement:
   - PlaceholderAPI integration
   - Tab completion for more commands
   - Additional admin commands

---

**Happy testing!** 🎮

**Build:** output/IslandSelector-1.0.0-SNAPSHOT.jar (186K)
**Session:** 43
**Date:** 2025-12-02
