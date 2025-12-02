# Testing Session 18 - Comprehensive Test Consolidation

## Session Overview
This session consolidates all implemented features that are ready for manual testing. No new code was written - this is a verification session to document what's ready for the human tester.

## Build Information
- **Session:** 18
- **Build Status:** ✅ SUCCESS
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **JAR Size:** 120K
- **Compilation:** 26 source files, 0 errors

## Summary

**Tests Already Passing:** 44/225 (19.6%)
**Tests Ready for Manual Testing:** 34 tests (Tests #28-35, #40-74)
**Tests Remaining:** 147

This session discovered that many tests from previous sessions are fully implemented but haven't been manually verified yet. The human tester should work through these in order.

---

## Priority 1: Test #40 - Premium Location Tooltips

**Test Number:** 40
**Category:** UI
**Description:** Hovering over premium location shows price and purchase option

### Implementation Status
✅ **FULLY IMPLEMENTED** - Code verified in `MainGridGUI.java` lines 429-447

### Test Steps
1. Admin sets a premium location: `/islandselector admin setprice D4 50000`
2. Open grid GUI as player
3. Hover over D4 (gold block)
4. Verify tooltip shows **"⭐ Premium Location: D4"**
5. Verify tooltip shows **"Price: $50,000"** (with comma formatting)
6. Verify tooltip shows **"Click to purchase"**

### Expected Result
```
⭐ Premium Location: D4
Location: X: 1500, Z: 1500

Price: $50,000

Click to purchase
```

### Code Reference
```java
// MainGridGUI.java, createPurchasableItem() method
meta.setDisplayName(colorize("&6⭐ Premium Location: " + coord.toString()));
String formattedPrice = String.format("%,d", (int) location.getPurchasePrice());
lore.add(colorize("&7Price: &a$" + formattedPrice));
lore.add(colorize("&eClick to purchase"));
```

### Notes
- Price is formatted with commas (e.g., 50,000 not 50000)
- Gold block icon from config (configurable)
- Requires Vault economy plugin for pricing to work
- Without Vault, premium locations might be free

---

## Priority 2: Tests #28-35 - Search Functionality

**Implementation:** Session 17
**Status:** Ready for testing
**File:** `SearchListener.java`

These tests verify the player search feature that centers the grid view on another player's island.

### Test #28: Search button displays
1. Open `/islandselector`
2. Check bottom bar for compass item
3. Verify tooltip says "Search Player"

### Test #29: Find player by name
1. Have Player A create an island
2. As Player B, click search button
3. Type Player A's name in chat
4. Verify viewport centers on their island

### Test #30: Partial name matching
1. Have player "Notch" with island
2. Click search, type "Not"
3. Verify finds "Notch" and centers

### Test #31: Player not found error
1. Click search
2. Type "NonExistentPlayer123"
3. Verify error message shown
4. Verify GUI reopens

### Additional Search Tests

**Test #32: Player without island**
- Search for player who hasn't created island
- Verify appropriate error message

**Test #33: Search cancellation**
- Click search, type "cancel"
- Verify search cancelled

**Test #34: Offline player search**
- Search for offline player with island
- Verify island found

**Test #35: Search with filter active**
- Enable filter (Available/Online)
- Perform search
- Verify found island visible

### Important Notes
- Search uses CHAT INPUT (GUI closes when you type)
- Your search input should NOT be broadcast to other players
- Partial matching is case-insensitive
- Prefix matching only ("Not" finds "Notch", but "otch" doesn't)

---

## Priority 3: Tests #41-48 - Island Claiming & Coordinate Mapping

**Implementation:** Sessions 6-12
**Status:** Ready for testing
**Files:** `IslandClaimGUI.java`, `IslandCreateListener.java`, `GridManager.java`

### Test #41: New player sees claiming mode
1. Join as new player (no island)
2. Execute `/island`
3. Verify grid GUI opens instead of auto-creating island
4. Verify available locations show "Click to claim this location"

### Test #42: Click empty location opens confirmation
1. Open grid as new player
2. Click green glass (empty location)
3. Verify confirmation GUI opens
4. Verify shows selected coordinates
5. Verify shows Confirm and Cancel buttons

### Test #43: Confirm claim creates island
1. Select location C7
2. Click Confirm
3. Verify island creation flow starts
4. Verify island exists at world coordinates matching C7
5. Verify C7 marked as occupied
6. Verify player teleported to island

### Test #44: Cancel returns to grid
1. Select empty location
2. Confirmation opens
3. Click Cancel
4. Verify returns to grid GUI
5. Verify no island created

### Test #45: Grid coordinate A1 → world (0, 0)
1. Configure grid with origin x=0, z=0 and spacing=500
2. Claim location A1
3. Verify island center is at world X=0, Z=0

### Test #46: Grid coordinate B2 → world (500, 500)
1. Same config as above
2. Claim location B2
3. Verify world X=500, Z=500

### Test #47: Grid spacing configuration works
1. Set island-spacing to 1000 in config
2. Restart server
3. Claim location B1
4. Verify island at 1000 blocks from origin (not 500)

### Test #48: Database links island UUID to coordinates
1. Create island at D5
2. Check database
3. Verify island UUID stored
4. Verify D5 linked to UUID
5. Verify persists after restart

---

## Priority 4: Tests #49-53 - Location Validation & Premium Purchases

**Implementation:** Session 13
**Status:** Ready for testing
**Files:** `GridGUIListener.java`, economy integration

### Test #49: Cannot claim occupied location
1. Player1 claims E5
2. Player2 tries to claim E5
3. Verify blocked with error

### Test #50: Cannot claim blocked reserved location
1. Admin reserves F6 without price
2. Player tries to claim F6
3. Verify blocked with message

### Test #51: Purchase premium location (sufficient funds)
1. Admin: `/islandselector admin setprice G7 10000`
2. Give player $15,000
3. Player clicks G7
4. Confirm purchase
5. Verify $10,000 deducted
6. Verify island created at G7
7. Verify G7 no longer reserved

### Test #52: Cannot purchase (insufficient funds)
1. Premium location price $50,000
2. Player has $1,000
3. Try to purchase
4. Verify error message
5. Verify no money deducted
6. Verify location remains reserved

### Test #53: Bypass permission (free premium locations)
1. Set premium price $50,000
2. Give player permission `islandselector.bypass.cost.purchase`
3. Player has $0
4. Claim premium location
5. Verify island created for free
6. Verify balance remains $0

---

## Priority 5: Tests #54-63 - Slot System UI & Foundation

**Implementation:** Sessions 13-14
**Status:** Ready for testing
**Files:** `SlotSelectionGUI.java`, `SlotSwitchConfirmationGUI.java`

### Test #54: Slot Selection GUI layout
1. Execute `/islandselector slots`
2. Verify 27-slot chest (3 rows)
3. Verify title "Your Island Slots"
4. Verify slots 2-6 in first row show slot items
5. Verify bottom row has Back and Close buttons

### Test #55: Active slot displays with glow
1. Create island (becomes slot 1, active)
2. Open slot GUI
3. Locate slot 1
4. Verify enchantment glow
5. Verify "★ ACTIVE" indicator
6. Verify name is green and bold

### Test #56: Inactive slot with island
1. Create islands in slots 1 and 2
2. Ensure slot 1 active
3. Open slot GUI
4. Locate slot 2 (inactive)
5. Verify custom icon (grass block) without glow
6. Verify shows "Click to switch"

### Test #57: Empty slot display
1. Have slot 1 with island
2. Slot 2 is empty
3. Open slot GUI
4. Locate slot 2
5. Verify shows barrier icon
6. Verify tooltip "Empty Slot"
7. Verify "Click to create island here"

### Test #58: Locked slot (no permission)
1. Default 2 slots per player
2. Open slot GUI
3. Check slots 3-5
4. Verify barrier or locked icon
5. Verify message "Requires permission" or similar

### Test #59: Slot rename works
1. Create island in slot 1
2. Click slot in GUI
3. Type new name in chat
4. Verify name updates
5. Verify persists after restart

### Test #60: Slot rename shows in GUI
1. Rename slot 1 to "Main Base"
2. Reopen slot GUI
3. Verify displays "Main Base" instead of "Slot 1"

### Test #61: Slot status tooltips
1. Hover over active slot
2. Verify shows: active indicator, island name, creation date
3. Hover over inactive slot
4. Verify shows: island name, "Click to switch"

### Test #62: Create island in empty slot
1. Have island in slot 1
2. Click empty slot 2
3. Select create option
4. Verify opens island claiming GUI
5. Create island at chosen location
6. Verify slot 2 now has island

### Test #63: Switch confirmation GUI
1. Click inactive slot with island
2. Verify confirmation GUI opens
3. Verify shows current and target slot
4. Verify Confirm and Cancel buttons

---

## Priority 6: Tests #64-70 - FAWE Slot Switching

**Implementation:** Session 15
**Status:** Ready for testing
**Files:** `SlotSwitchManager.java`, FAWE integration

### Test #64: Confirm switch saves current island
1. Build structures on slot 1
2. Switch to slot 2
3. Confirm
4. Check `plugins/IslandSelector/slots/{uuid}/`
5. Verify `slot-1.schem` exists
6. Verify non-zero file size

### Test #65: Slot switch clears location
1. Island with structures in slot 1
2. Switch to slot 2
3. Check grid location in world
4. Verify slot 1 structures removed (void)
5. Verify slot 2 structures present

### Test #66: Slot switch loads target schematic
1. Build unique structures in slots 1 and 2
2. Switch slot 1 → slot 2
3. Verify slot 2 structures appear
4. Switch back to slot 1
5. Verify slot 1 structures reappear exactly

### Test #67: Owner teleported to island spawn
1. Be on slot 1 island
2. Switch to slot 2
3. After switch, verify teleported to slot 2 spawn

### Test #68: Team members teleported
1. Add team member
2. Team member on island
3. Owner switches slots
4. Verify team member teleported to new slot spawn
5. Verify remains on team

### Test #69: Empty slot creates empty island
1. Switch from slot 1 (with island) to empty slot 2
2. Verify empty void island created
3. Verify can build on it
4. Verify saved when switching back

### Test #70: Slot switch cooldown enforced
1. Set switch-cooldown to 60 seconds
2. Switch slots
3. Try to switch again immediately
4. Verify error about cooldown
5. Verify shows time remaining

---

## Priority 7: Tests #71-74 - Visitor Teleportation

**Implementation:** Session 16
**Status:** Ready for testing
**Files:** `SlotSwitchManager.java`, visitor detection

### Test #71: Visitors detected during switch
1. Have visitor on island
2. Switch slots
3. Verify visitor detected
4. Verify warning message shown

### Test #72: Visitors teleported (with island)
1. Visitor on island (has own island)
2. Owner switches slots
3. Verify visitor teleported to their island
4. Verify delay of ~2 seconds (configurable)

### Test #73: Visitors teleported (no island)
1. Visitor without island on your island
2. Switch slots
3. Verify visitor teleported to spawn
4. Verify warning shown first

### Test #74: Team members NOT teleported as visitors
1. Team member on island
2. Switch slots
3. Verify team member teleported to NEW slot (not away)
4. Verify treated as team, not visitor

---

## Testing Workflow

### Recommended Order
1. **Start with Test #40** (Premium tooltips) - Quick visual test
2. **Test #28-35** (Search) - Requires multiple players
3. **Test #41-48** (Island claiming) - Requires fresh players
4. **Test #49-53** (Validation & purchases) - Requires Vault plugin
5. **Test #54-63** (Slot UI) - Foundation for slot system
6. **Test #64-70** (Slot switching) - Requires FAWE
7. **Test #71-74** (Visitor teleportation) - Requires multiple players

### Prerequisites
- Paper/Spigot 1.20.x - 1.21.x server
- BentoBox (latest)
- BSkyBlock addon
- FastAsyncWorldEdit (FAWE) - **REQUIRED** for slot switching
- Vault (optional - for economy features)
- PlaceholderAPI (optional)

### Testing Tips
1. **Use multiple accounts** for visitor/search tests
2. **Check console** for errors after each test
3. **Test both success and failure cases**
4. **Verify data persists after restart**
5. **Check file system** for schematics/backups where applicable

---

## Reporting Results

### What to Report

**For each test, note:**
- Test number (e.g., Test #40)
- Result: PASS or FAIL
- If FAIL: Expected vs actual behavior
- Any console errors (full stack trace)
- Screenshots if helpful

**Example:**
```
Test #40: PASS ✅
- Premium tooltip displays correctly
- Price formatted with commas: $50,000
- All expected lines present

Test #42: FAIL ❌
- Expected: Confirmation GUI opens when clicking empty location
- Actual: Nothing happens, no GUI opens
- Console error: NullPointerException in GridGUIListener line 123
- Screenshot attached
```

---

## Known Limitations

1. **Player heads** for offline players may show Steve if skin not cached
2. **Island relocation** - Not yet implemented (Tests #75+)
3. **Visit/warp** - Partly implemented
4. **Level display** - Level addon integration pending
5. **Neighborhood view** - Basic implementation exists

---

## Next Features to Implement (After Testing)

Based on the app spec, these major features remain:

1. **Island Relocation** (Tests #75-90) - Move existing island to new grid location
2. **Visit/Warp System** (Tests #91-100) - Visit other players' islands from GUI
3. **Neighborhood View** (Tests #101-110) - 3x3 view of adjacent islands
4. **Level Integration** (Tests #111-120) - Display island levels from Level addon
5. **PlaceholderAPI** (Tests #121-130) - Placeholders for other plugins
6. **Advanced Slot Features** (Tests #131-150) - Backups, slot names, etc.
7. **Admin Tools** (Tests #151-170) - Force relocate, delete slots, etc.
8. **Economy Integration** (Tests #171-180) - Costs for relocation/switching
9. **Permissions** (Tests #181-200) - Fine-grained permission control
10. **Additional Features** (Tests #201-225) - Edge cases, polish

---

## Session 18 Complete

This session:
- ✅ Reviewed all implemented code
- ✅ Verified Test #40 implementation exists
- ✅ Documented 34 tests ready for manual verification
- ✅ Created comprehensive testing guide
- ✅ Build successful (120K JAR)

**No code changes were made** - this was a documentation and verification session.

The human tester should now work through Tests #28-74 systematically and report results. Once these tests pass, we can proceed with implementing the next batch of features (island relocation and beyond).

---

**Happy Testing! 🎮**
