# Session 49: Admin Force Relocate Command

## Quick Summary
**Feature:** Admin command to force relocate a player's island to a new location
**Time Estimate:** 10-15 minutes
**Players Required:** 2 accounts (1 admin, 1 test player)
**Complexity:** Moderate (requires admin permissions, existing islands)
**Dependencies:** None

## What Was Implemented

### Admin Force Relocate Command
New admin command for forcibly relocating a player's island:

**Command:** `/islandselector admin relocate <player> <coordinate>`

**Features:**
- Bypasses confirmation GUI (immediate relocation)
- Bypasses economy cost (free for admin-initiated)
- Bypasses cooldown restrictions
- Works on any online player
- Validates target location is available
- Prevents relocation to occupied locations
- Notifies both admin and player
- Triggers IslandRelocateEvent for other plugins

**Permissions:** `islandselector.admin.relocate`

## Test Cases

### Test #1: Admin Force Relocate Player's Island
**Description:** Admin can force relocate a player's island without confirmation

**Setup:**
- Player "TestPlayer" has an island at location A1 (or any location)
- Admin account with `islandselector.admin.relocate` permission
- Target location B5 is available (empty)

**Steps:**
1. As admin, run: `/islandselector admin relocate TestPlayer B5`
2. Verify admin receives message:
   - "Force relocating TestPlayer's island from A1 to B5..."
   - "This bypasses confirmation, costs, and cooldowns."
   - "Relocation initiated successfully."
3. Verify TestPlayer receives notification:
   - "[Admin Notice]"
   - "Your island is being relocated by an administrator."
   - Shows old and new coordinates
4. Wait for relocation to complete (10-30 seconds depending on island size)
5. Verify TestPlayer is teleported to new island at B5
6. Visit B5 in-game and verify island blocks are present
7. Visit A1 and verify it's now empty/cleared
8. Open grid GUI and verify:
   - A1 shows as available (green glass)
   - B5 shows as occupied by TestPlayer

**Expected Result:**
- ✅ Relocation happens without confirmation GUI
- ✅ No cost charged to player
- ✅ Works even if player has cooldown
- ✅ Old location A1 cleared completely
- ✅ New location B5 has island
- ✅ Player teleported to new location
- ✅ Grid GUI updates correctly

**Pass Criteria:** ✅ Island successfully relocated by admin command

---

### Test #2: Cannot Relocate to Occupied Location
**Description:** Admin command prevents relocating to locations that already have islands

**Setup:**
- Player1 has island at C3
- Player2 has island at D4
- Admin account

**Steps:**
1. As admin, run: `/islandselector admin relocate Player1 D4`
2. Verify error message: "Target location D4 is already occupied."
3. Verify Player1's island remains at C3
4. Verify no relocation occurs

**Expected Result:**
- ✅ Clear error message about occupied location
- ✅ No relocation performed
- ✅ Player1's island unchanged

**Pass Criteria:** ✅ Cannot relocate to occupied locations

---

### Test #3: Cannot Relocate to Same Location
**Description:** Command prevents relocating to current location

**Setup:**
- Player has island at E5

**Steps:**
1. As admin, run: `/islandselector admin relocate Player E5`
2. Verify error message: "Player's island is already at E5"
3. Verify no unnecessary operations performed

**Expected Result:**
- ✅ Clear error message
- ✅ No relocation attempted

**Pass Criteria:** ✅ Graceful handling of same-location case

---

### Test #4: Cannot Relocate Player Without Island
**Description:** Command handles players who don't have islands

**Steps:**
1. As admin, run: `/islandselector admin relocate NewPlayer F6`
   (NewPlayer is someone who never created an island)
2. Verify error message: "Player NewPlayer doesn't have an island."
3. Verify no errors in console

**Expected Result:**
- ✅ Clear error message
- ✅ No exceptions or errors
- ✅ Graceful handling

**Pass Criteria:** ✅ Handles no-island case gracefully

---

### Test #5: Cannot Relocate Offline Players
**Description:** Command requires player to be online (for teleportation)

**Setup:**
- OfflinePlayer has an island at G7
- OfflinePlayer is not logged in

**Steps:**
1. As admin, run: `/islandselector admin relocate OfflinePlayer H8`
2. Verify error message:
   - "Player OfflinePlayer is offline."
   - "Cannot relocate offline players at this time."
   - "Reason: Relocation requires the player to be online for teleportation."
3. Verify OfflinePlayer's island remains at G7

**Expected Result:**
- ✅ Clear error explaining limitation
- ✅ No relocation performed
- ✅ Island data unchanged

**Pass Criteria:** ✅ Offline players cannot be relocated (safety)

---

### Test #6: Bypasses Economy Cost
**Description:** Admin relocation doesn't charge the player money

**Setup:**
- Vault and economy plugin installed
- Relocation cost set to $10,000 in config
- Player has less than $10,000 in balance
- Player has island at I9

**Steps:**
1. Check player's balance (should be < $10,000)
2. As admin, run: `/islandselector admin relocate Player J10`
3. Wait for relocation to complete
4. Check player's balance again
5. Verify balance is unchanged (no charge)
6. Verify relocation still succeeded

**Expected Result:**
- ✅ Relocation completes successfully
- ✅ Player's money is NOT deducted
- ✅ Works even though player couldn't normally afford it

**Pass Criteria:** ✅ Admin relocation is free (bypasses cost)

---

### Test #7: Bypasses Cooldown Restrictions
**Description:** Admin can relocate even if player has active cooldown

**Setup:**
- Relocation cooldown set to 7 days in config
- Player just relocated 1 minute ago (has active cooldown)
- Player has island at K11

**Steps:**
1. Have player try to relocate normally
2. Verify they get cooldown error
3. As admin, run: `/islandselector admin relocate Player L12`
4. Verify relocation proceeds despite cooldown
5. Wait for completion
6. Verify relocation succeeded

**Expected Result:**
- ✅ Admin command bypasses cooldown
- ✅ Relocation completes successfully
- ✅ No cooldown errors shown to admin

**Pass Criteria:** ✅ Admin relocation bypasses cooldowns

---

### Test #8: Can Relocate to Reserved Locations
**Description:** Admin can force relocate to reserved (blocked) locations

**Setup:**
- Location M13 is reserved (run `/islandselector admin reserve M13`)
- Player has island at M14

**Steps:**
1. Verify M13 is reserved by opening grid GUI (should show barrier)
2. As admin, run: `/islandselector admin relocate Player M13`
3. Verify relocation proceeds (no error about reservation)
4. Wait for completion
5. Verify island now at M13
6. Open grid GUI and verify M13 shows player's island

**Expected Result:**
- ✅ Admin can relocate to reserved locations
- ✅ Reservation doesn't block admin command
- ✅ Relocation completes successfully

**Pass Criteria:** ✅ Reserved locations accessible to admin command

---

### Test #9: Cannot Relocate to Locked Locations
**Description:** Even admin cannot relocate to locked grid areas

**Setup:**
- Grid expansion configured to lock certain areas
- Location Z99 is locked/outside grid bounds
- Player has island at N14

**Steps:**
1. As admin, run: `/islandselector admin relocate Player Z99`
2. Verify error message: "Target location Z99 is locked."
3. Verify no relocation occurs
4. Verify player remains at N14

**Expected Result:**
- ✅ Locked locations are off-limits even to admin
- ✅ Clear error message
- ✅ No relocation performed

**Pass Criteria:** ✅ Locked areas cannot be relocated to

---

### Test #10: Tab Completion Works
**Description:** Tab completion suggests player names and coordinate hints

**Steps:**
1. As admin, type: `/islandselector admin relocate ` and press TAB
2. Verify online player names appear as suggestions
3. Type first few letters of a player name and press TAB
4. Verify it completes to that player
5. After player name, press TAB again
6. Verify coordinate format hints appear (0,0 / 5,5 / A1 / B2)

**Expected Result:**
- ✅ Player names tab-complete
- ✅ Coordinate hints shown
- ✅ Makes command easier to use

**Pass Criteria:** ✅ Tab completion functions correctly

---

## Configuration

No configuration changes needed - command is available to admins with the permission.

## Required Permissions

```yaml
islandselector.admin.relocate:
  description: "Force relocate player islands"
  default: op
```

## Command Usage

```
/islandselector admin relocate <player> <coordinate>

Examples:
  /islandselector admin relocate Notch 5,3
  /islandselector admin relocate Steve A1
  /islandselector admin relocate Alex 10,-5
```

**Arguments:**
- `<player>` - Name of online player (must be online)
- `<coordinate>` - Target location in format "X,Z" or "A1" style

**Notes:**
- Player must be online (for teleportation)
- Target must be available or reserved (not occupied or locked)
- Bypasses confirmation, costs, and cooldowns
- Triggers IslandRelocateEvent

## Known Limitations

**Offline Players:**
- Cannot relocate offline players (they need to be online for teleportation)
- This is intentional - prevents issues with player location sync

**Locked Locations:**
- Even admins cannot relocate to locked grid areas
- Locked areas are intentionally off-limits

**Large Islands:**
- Relocation of large islands (100k+ blocks) takes time
- Operation is async so won't lag server
- Both admin and player notified when complete

## Troubleshooting

**"Player not found" error:**
- Check player name spelling
- Player must have logged in at least once (have a UUID)

**"Player is offline" error:**
- Player must be online for relocation
- Have them log in, then retry command

**"Target location is already occupied" error:**
- Another player's island is at that location
- Choose a different target coordinate

**"Player doesn't have an island" error:**
- Player never created an island
- They need to run `/island` first

## Success Criteria

- ✅ Can force relocate online player's island
- ✅ Bypasses confirmation GUI
- ✅ Bypasses economy costs
- ✅ Bypasses cooldown restrictions
- ✅ Validates target location is available
- ✅ Prevents relocating to occupied locations
- ✅ Cannot relocate offline players
- ✅ Can relocate to reserved locations
- ✅ Cannot relocate to locked locations
- ✅ Tab completion works
- ✅ Both admin and player notified
- ✅ Old location cleared, new location populated
- ✅ Grid GUI updates correctly

## Test Report Template

```
Test #1 - Force Relocate Player: [PASS/FAIL]
Notes:

Test #2 - Cannot Relocate to Occupied: [PASS/FAIL]
Notes:

Test #3 - Cannot Relocate to Same Location: [PASS/FAIL]
Notes:

Test #4 - Player Without Island: [PASS/FAIL]
Notes:

Test #5 - Cannot Relocate Offline Players: [PASS/FAIL]
Notes:

Test #6 - Bypasses Economy Cost: [PASS/FAIL]
Notes:

Test #7 - Bypasses Cooldown: [PASS/FAIL]
Notes:

Test #8 - Can Relocate to Reserved: [PASS/FAIL]
Notes:

Test #9 - Cannot Relocate to Locked: [PASS/FAIL]
Notes:

Test #10 - Tab Completion: [PASS/FAIL]
Notes:

Overall: [PASS/FAIL]
Issues Found:

```

## Additional Notes

**Why This Command is Useful:**
- Server admins can help players who made location mistakes
- Resolve conflicts or disputes over island locations
- Move problem islands causing issues
- Administrative control over island placement
- Emergency relocations for griefing resolution

**Safety Features:**
- Requires explicit admin permission
- Validates all target locations
- Cannot be used on offline players (prevents confusion)
- Cannot overwrite existing islands
- Notifies affected player immediately
- Full audit trail via IslandRelocateEvent

**Best Practices:**
- Always check target location is truly available first
- Warn player before relocating (unless emergency)
- Use `/islandselector admin info <coord>` to verify locations
- Test with small islands before relocating large ones
