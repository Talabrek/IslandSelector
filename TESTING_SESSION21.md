# Testing Guide - Session 21

## Session Info
- **Session:** 21
- **Date:** Session 21
- **Build:** 128K JAR (29 source files, 2 new commands)
- **Tests Implemented:** 2 new admin commands
- **Status:** Ready for manual testing

## Build Information
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **Size:** 128K
- **Compilation:** ✅ SUCCESS (0 errors, 1 non-critical warning)

## New Features Implemented

### Test #75: Admin Unreserve Command
**Feature:** Admin command to unreserve a previously reserved location

**Command:** `/islandselector admin unreserve <coord>`

**Testing Steps:**
1. As admin, reserve a location: `/islandselector admin reserve 5,5`
2. Verify location shows as gray glass in grid GUI (reserved/blocked)
3. Run: `/islandselector admin unreserve 5,5`
4. Verify success message: "Unreserved location 5,5 - it is now available for claiming"
5. Open grid GUI and check location 5,5
6. Verify it now shows as green glass (available for claiming)
7. Attempt to claim the location as a regular player
8. Verify the claim works successfully

**Edge Cases to Test:**
- **Not reserved:** Try to unreserve a location that isn't reserved
  - Command: `/islandselector admin unreserve 10,10` (on unreserved location)
  - Expected: Error message "10,10 is not reserved"

- **Occupied location:** Try to unreserve a location with an existing island
  - Setup: Player has island at 0,0
  - Reserve it: `/islandselector admin reserve 0,0`
  - Try unreserve: `/islandselector admin unreserve 0,0`
  - Expected: Error message "Cannot unreserve 0,0 - it is currently occupied by a player"

- **Invalid coordinate:** Try with invalid format
  - Command: `/islandselector admin unreserve ABC`
  - Expected: Error message about invalid coordinate format

**Expected Behavior:**
- ✅ Unreserves the location
- ✅ Clears any purchase price
- ✅ Makes location available for claiming
- ✅ Shows appropriate error messages for edge cases
- ✅ Cannot unreserve occupied locations
- ✅ Changes persist after server restart

---

### Test #76: Admin Clear Price Command
**Feature:** Admin command to clear the purchase price from a premium location, converting it from purchasable to blocked

**Command:** `/islandselector admin clearprice <coord>`

**Testing Steps:**
1. As admin, set a premium location: `/islandselector admin setprice 6,6 30000`
2. Open grid GUI and verify 6,6 shows as GOLD_BLOCK with price tooltip
3. Run: `/islandselector admin clearprice 6,6`
4. Verify success message: "Cleared price for 6,6 - it is now blocked (not purchasable)"
5. Open grid GUI and check location 6,6
6. Verify it now shows as GRAY_STAINED_GLASS_PANE (reserved/blocked)
7. Hover over the location
8. Verify tooltip shows "🚫 Reserved" (not premium)
9. Attempt to click/purchase as a regular player
10. Verify it cannot be purchased (blocked)

**Edge Cases to Test:**
- **Not reserved:** Try to clear price from unreserved location
  - Command: `/islandselector admin clearprice 10,10` (on unreserved location)
  - Expected: Error message "10,10 is not reserved"

- **Already blocked (no price):** Try to clear price from blocked location
  - Setup: `/islandselector admin reserve 7,7` (no price set)
  - Command: `/islandselector admin clearprice 7,7`
  - Expected: Error message "7,7 does not have a purchase price set"

- **Invalid coordinate:** Try with invalid format
  - Command: `/islandselector admin clearprice XYZ`
  - Expected: Error message about invalid coordinate format

**Expected Behavior:**
- ✅ Clears the purchase price (sets to 0)
- ✅ Converts premium location to blocked
- ✅ Changes display from gold block to gray glass
- ✅ Players can no longer purchase the location
- ✅ Location remains reserved
- ✅ Shows appropriate error messages for edge cases
- ✅ Changes persist after server restart

---

## Complete Admin Command Suite (After Session 21)

After implementing these features, the complete admin command set is:

```
/islandselector admin reload             - Reload configuration
/islandselector admin reserve <coord>    - Reserve a location (blocked)
/islandselector admin unreserve <coord>  - Unreserve a location (NEW in Session 21)
/islandselector admin setprice <coord> <price> - Set premium location price
/islandselector admin clearprice <coord> - Clear premium price (NEW in Session 21)
/islandselector admin info <coord>       - Show location information
/islandselector admin version            - Show plugin version
```

**Recommended Testing Flow:**
1. Reserve a location → Verify it's blocked
2. Set a price → Verify it becomes premium (gold)
3. Clear the price → Verify it becomes blocked (gray)
4. Unreserve it → Verify it becomes available (green)
5. Claim it as player → Verify island creation works

This demonstrates the full lifecycle of location management.

---

## Regression Testing

Since we modified the admin command system, please verify these existing features still work:

### Existing Admin Commands (Quick Check)
1. `/islandselector admin version` - Should show plugin info
2. `/islandselector admin info 0,0` - Should show location details
3. `/islandselector admin reserve 1,1` - Should reserve a location
4. `/islandselector admin setprice 2,2 50000` - Should set a price
5. `/islandselector admin reload` - Should reload config

All should work without errors.

---

## Testing Summary

**New Features:** 2 admin commands (unreserve, clearprice)
**Files Modified:** 3 (AdminCommand.java, en-US.yml, plus 2 new command files)
**Critical Changes:** None (additive changes only)
**Breaking Changes:** None
**Requires Config Changes:** No

**Estimated Testing Time:** 15-20 minutes

---

## Known Issues
- None expected (these are simple admin commands using existing GridManager methods)

---

## Next Steps After Testing

If these tests pass:
- Mark Tests #75 and #76 as passing in feature_list.json
- Continue with remaining admin expansion features (Test #77+)

If these tests fail:
- Report specific issues to autonomous agent
- Provide error messages from console
- Describe unexpected behavior

---

## Notes for Human Tester

These commands use the existing `unreserveLocation()` and `setPurchasePrice()` methods in GridManager that were already implemented. We're just exposing them via admin commands.

The `unreserve` command is particularly important for server admins who want to open up previously reserved areas.

The `clearprice` command is useful for converting premium locations to blocked ones without completely unreserving them (maintains the "reserved" status but removes purchasability).

---

**Happy Testing! 🎮**
