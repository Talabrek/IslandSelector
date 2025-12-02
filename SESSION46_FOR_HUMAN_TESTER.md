# Session 46: PlaceholderAPI Integration

## Quick Summary
**Features:** PlaceholderAPI placeholders for island information
**Time Estimate:** 15-20 minutes
**Players Required:** 2-3 accounts (for neighbor testing)
**Complexity:** Medium
**Dependencies:** PlaceholderAPI plugin REQUIRED

## What Was Implemented

### PlaceholderAPI Expansion
- Complete PlaceholderAPI integration with 5 custom placeholders
- Automatic registration on startup (if PlaceholderAPI is installed)
- Graceful handling when PlaceholderAPI is not present
- All placeholders update in real-time

### Placeholders Implemented
1. `%islandselector_location%` - Player's grid coordinates (e.g., "Q17")
2. `%islandselector_slot_active_name%` - Active slot name
3. `%islandselector_cooldown_switch%` - Slot switch cooldown (formatted time or "Ready")
4. `%islandselector_available%` - Count of available grid locations
5. `%islandselector_neighbors_online%` - Count of online neighbors (0-8)

## Test Cases

### Test #1: PlaceholderAPI Integration Registered
**Description:** PlaceholderAPI integration registers successfully on startup

**Steps:**
1. Ensure PlaceholderAPI is installed on the server
2. Start server with IslandSelector
3. Check server console for "PlaceholderAPI integration registered successfully!"
4. Run `/islandselector admin version`
5. Verify it shows PlaceholderAPI version with "(Placeholders enabled)"

**Expected Result:**
- Console shows successful registration
- Version command confirms PlaceholderAPI is active
- No errors in console

**Pass Criteria:** ✅ PlaceholderAPI registers successfully

---

### Test #2: %islandselector_location% Placeholder
**Description:** Shows player's island grid coordinates

**Steps:**
1. Create an island at a known location (e.g., grid coordinate "B3")
2. Install a plugin that displays placeholders (e.g., DeluxeMenus, Essentials chat format, or Placeholder Viewer)
3. Use the placeholder `%islandselector_location%`
4. Verify it displays your grid coordinate (e.g., "B3")
5. Test with a player who has NO island
6. Verify it displays "None"

**Expected Result:**
- Players with islands see their grid coordinates (e.g., "Q17")
- Players without islands see "None"
- Format matches the grid coordinate system (Letter + Number)

**Pass Criteria:** ✅ Displays correct grid coordinates

**Alternative Testing Method:**
Use `/papi parse me %islandselector_location%` command (requires PlaceholderAPI's `/papi` command)

---

### Test #3: %islandselector_slot_active_name% Placeholder
**Description:** Shows the name of the player's active slot

**Steps:**
1. Have an island in slot 1 (default name: "Slot 1")
2. Use placeholder `%islandselector_slot_active_name%`
3. Verify it displays "Slot 1"
4. Rename slot 1 to "Mining Hub" using `/islandselector setname 1 Mining Hub`
5. Verify placeholder now displays "Mining Hub"
6. Test with a player who has NO island
7. Verify it displays "No Slot"

**Expected Result:**
- Shows custom slot name if set
- Shows "Slot X" if no custom name
- Shows "No Slot" for players without islands
- Updates immediately after renaming

**Pass Criteria:** ✅ Displays active slot name correctly

---

### Test #4: %islandselector_cooldown_switch% Placeholder
**Description:** Shows remaining cooldown for slot switching in human-readable format

**Steps:**
1. Have at least 2 slots with islands
2. Configure a switch cooldown in config.yml (e.g., `slots.switch-cooldown: 300` for 5 minutes)
3. Reload config: `/islandselector admin reload`
4. Use placeholder `%islandselector_cooldown_switch%`
5. Verify it displays "Ready" (no active cooldown)
6. Switch slots: `/islandselector switch 2`
7. Immediately check the placeholder again
8. Verify it shows formatted time like "4m 59s" or "5m"
9. Wait a minute and check again
10. Verify time decreases (e.g., now shows "3m 58s")
11. After cooldown expires, verify it shows "Ready" again

**Expected Result:**
- Shows "Ready" when no cooldown active
- Shows formatted time when on cooldown:
  - Seconds only: "45s"
  - Minutes + seconds: "4m 30s"
  - Hours + minutes: "1h 30m"
  - Days + hours: "2d 5h"
- Time decreases as cooldown progresses
- Shows "Ready" after cooldown expires

**Pass Criteria:** ✅ Cooldown time displays and updates correctly

---

### Test #5: %islandselector_available% Placeholder
**Description:** Shows count of available (empty) grid locations

**Steps:**
1. Use placeholder `%islandselector_available%`
2. Note the number displayed (e.g., "985")
3. Have a new player create an island
4. Check the placeholder again
5. Verify the number decreased by 1 (e.g., now "984")
6. Have another player create an island
7. Verify it decreased by 1 again (e.g., now "983")

**Expected Result:**
- Shows a number representing available locations
- Number decreases when islands are created
- Number increases when islands are deleted
- Updates in real-time

**Pass Criteria:** ✅ Available location count is accurate

**Note:** The count represents "tracked - occupied" locations, not theoretical max

---

### Test #6: %islandselector_neighbors_online% Placeholder
**Description:** Shows count of online neighbors (players with adjacent islands)

**Steps:**
1. Player A creates island at location C5
2. Player B creates island adjacent to Player A (e.g., C6, right next to C5)
3. Player C creates island also adjacent to Player A (e.g., D5, below C5)
4. Player A uses placeholder `%islandselector_neighbors_online%`
5. With all 3 players online, verify Player A sees "2" (2 online neighbors)
6. Have Player B log off
7. Verify Player A now sees "1" (only Player C is online)
8. Have Player B log back in
9. Verify Player A sees "2" again
10. Test with a player who has NO neighbors
11. Verify they see "0"

**Expected Result:**
- Shows "0" when no neighbors
- Shows count of online neighbors (1-8)
- Updates when neighbors log in/out
- Only counts direct neighbors (8 adjacent grid positions)
- Does NOT count diagonal-only neighbors

**Pass Criteria:** ✅ Online neighbor count is accurate and updates

---

### Test #7: Placeholders Work in Chat, Scoreboard, and Menus
**Description:** Placeholders work in various plugins that support PlaceholderAPI

**Steps:**
1. Configure a plugin to use IslandSelector placeholders:
   - **EssentialsX Chat:** Set nickname or chat format to include placeholder
   - **TAB Plugin:** Add placeholder to tab list or nametag
   - **Scoreboard Plugin:** Add placeholder to scoreboard
   - **DeluxeMenus:** Add placeholder to custom menu
2. Verify the placeholder displays correctly in that plugin
3. Test that placeholder updates (e.g., move to different island, rename slot, etc.)
4. Verify update is reflected in the plugin

**Expected Result:**
- Placeholders work in all PlaceholderAPI-compatible plugins
- Values update dynamically
- No errors or "Unknown placeholder" messages

**Pass Criteria:** ✅ Placeholders integrate with other plugins

---

### Test #8: Works Without PlaceholderAPI Installed
**Description:** Plugin works normally when PlaceholderAPI is not installed

**Steps:**
1. Remove PlaceholderAPI from plugins folder
2. Restart server
3. Check console for "PlaceholderAPI not found - Placeholders disabled" log
4. Verify IslandSelector loads normally without errors
5. Run `/islandselector admin version`
6. Verify it shows "PlaceholderAPI: Not installed (Placeholders disabled)"
7. Test basic IslandSelector features (open GUI, etc.)
8. Verify everything works normally

**Expected Result:**
- Plugin loads successfully without PlaceholderAPI
- Console shows appropriate log message
- Version command reflects PlaceholderAPI is not installed
- All other features work normally
- No errors or crashes

**Pass Criteria:** ✅ Plugin gracefully handles missing PlaceholderAPI

---

## Quick Test Flow (10 minutes)

**Prerequisites:**
- Install PlaceholderAPI
- Have 2 player accounts ready
- Both players should have islands (preferably adjacent)

**5-Minute Quick Test:**

1. **Registration Test** (1 min)
   - Start server
   - Check console for "PlaceholderAPI integration registered successfully!"
   - Run `/islandselector admin version`

2. **Basic Placeholders** (2 min)
   - Use `/papi parse me %islandselector_location%`
   - Use `/papi parse me %islandselector_slot_active_name%`
   - Use `/papi parse me %islandselector_available%`
   - Verify all show reasonable values

3. **Cooldown Placeholder** (1 min)
   - Check: `/papi parse me %islandselector_cooldown_switch%` (should be "Ready")
   - Switch slots (if you have multiple)
   - Check again (should show time like "4m 59s")

4. **Neighbor Placeholder** (1 min)
   - Have 2 players with adjacent islands
   - Both online: `/papi parse me %islandselector_neighbors_online%`
   - One logs off, check again
   - Verify count changes

## Configuration

No configuration changes needed for basic PlaceholderAPI integration.

**Optional: Configure Switch Cooldown** (for testing cooldown placeholder)
```yaml
# config.yml
slots:
  switch-cooldown: 300  # 5 minutes in seconds
```

## Known Limitations

**Placeholder Update Frequency:**
- Most placeholders update on request (real-time)
- Neighbor count may cache for 1-2 seconds
- This is normal and prevents excessive lookups

**Grid Coordinate Format:**
- Shows format like "Q17" (column letter + row number)
- For very large grids, uses multiple letters (AA, AB, etc.)
- Negative coordinates not shown in placeholder (plugin enforces positive grid)

## Troubleshooting

**"Unknown placeholder" error:**
- Verify PlaceholderAPI is installed and enabled
- Check console for "PlaceholderAPI integration registered successfully!"
- Try `/papi reload` to refresh PlaceholderAPI
- Restart server if needed

**Placeholder shows "None" or "No Slot":**
- This is correct for players without islands
- Verify player has actually created an island

**Neighbor count always shows 0:**
- Ensure neighbors are actually adjacent (share an edge)
- Diagonal-only islands are NOT neighbors
- Check if neighbors are online

**Cooldown shows "Ready" after switching:**
- Check config.yml for `slots.switch-cooldown` value
- If set to 0, cooldown is disabled
- Reload config after changes

## Success Criteria

- ✅ PlaceholderAPI integration registers on startup
- ✅ %islandselector_location% displays grid coordinates
- ✅ %islandselector_slot_active_name% displays slot name
- ✅ %islandselector_cooldown_switch% shows formatted time
- ✅ %islandselector_available% shows location count
- ✅ %islandselector_neighbors_online% shows neighbor count
- ✅ Placeholders work in other plugins (chat, scoreboard, etc.)
- ✅ Plugin works without PlaceholderAPI installed

## Test Report Template

```
Test #1 - PlaceholderAPI Registration: [PASS/FAIL]
Notes:

Test #2 - Location Placeholder: [PASS/FAIL]
Notes:

Test #3 - Slot Name Placeholder: [PASS/FAIL]
Notes:

Test #4 - Cooldown Placeholder: [PASS/FAIL]
Notes:

Test #5 - Available Count Placeholder: [PASS/FAIL]
Notes:

Test #6 - Neighbors Online Placeholder: [PASS/FAIL]
Notes:

Test #7 - Integration with Other Plugins: [PASS/FAIL]
Notes:

Test #8 - Works Without PlaceholderAPI: [PASS/FAIL]
Notes:

Overall: [PASS/FAIL]
Issues Found:

```

## Additional Notes

**PlaceholderAPI Commands:**
- `/papi parse me <placeholder>` - Test placeholder for yourself
- `/papi parse <player> <placeholder>` - Test placeholder for another player
- `/papi reload` - Reload PlaceholderAPI
- `/papi ecloud download IslandSelector` - NOT needed (built-in expansion)

**This is a built-in expansion:** IslandSelector includes its own PlaceholderAPI expansion. You do NOT need to download anything from eCloud.
