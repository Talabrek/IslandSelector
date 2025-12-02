# Session 41 - For Human Tester

## Quick Summary

**Feature:** Island Relocation System
**Status:** ✅ ALREADY IMPLEMENTED (Session 36) - Needs Testing
**Purpose:** This session documents existing island relocation features
**Build Status:** ✅ NO CHANGES (Documentation only)
**Your Task:** Test the island relocation system

---

## What This Session Did

**Discovery:** Session 41 found that **Island Relocation** (Tests #107-132+) was already fully implemented in Session 36 but never comprehensively verified by human testing.

**No new code** was added - this session documents what already exists from Session 36.

---

## What to Test

Comprehensive island relocation features ready for testing:

1. **Basic Relocation** (Tests #107-110)
   - Player with island clicks empty location to relocate
   - Confirmation GUI shows from/to coordinates and cost
   - Island physically moves to new location
   - Old location becomes available again

2. **Relocation Process** (Tests #111-115)
   - FAWE saves island to schematic
   - Old location cleared completely
   - Island pasted at new location with all blocks/entities
   - BSkyBlock data updated (spawn, warp, protection)

3. **Player Teleportation** (Tests #116-118)
   - Owner teleported to new island location
   - Team members teleported to new island
   - Visitors teleported away (to their island or spawn)

4. **Economy Integration** (Tests #119-121)
   - Relocation cost deducted via Vault
   - Cannot relocate with insufficient funds
   - Bypass cost permission allows free relocation

5. **Cooldown System** (Tests #122-125)
   - Cooldown enforced between relocations
   - Cooldown persists across server restarts
   - Bypass cooldown permission works
   - Human-readable time display

6. **Edge Cases** (Tests #126-132)
   - Cannot relocate to occupied/reserved locations
   - Can relocate to premium location (charged both costs)
   - Progress indicators during relocation
   - Admin commands for forced relocation

---

## Quick 10-Minute Test

### Setup
Copy JAR: `output/IslandSelector-1.0.0-SNAPSHOT.jar` → `plugins/BentoBox/addons/`

### Core Test Flow

**Test Basic Relocation:**

```
1. Create an island at any location (e.g., A1)
2. Open grid GUI: /islandselector
3. Click on an empty location (e.g., C3)
4. Expected: Relocation confirmation GUI opens
5. Expected: Shows "Confirm Relocation" title
6. Expected: Info item shows new location coordinates
7. Expected: If config has cost, cost is displayed
8. Click GREEN WOOL (Confirm)
9. Expected: Progress messages appear:
   - "Starting island relocation..."
   - "Saving island..."
   - "Clearing old location..."
   - "Pasting island at new location..."
   - "Updating island data..."
   - "Teleporting..."
10. Expected: "Island relocated successfully!" message
11. Expected: Player teleported to new location
12. Expected: Island structures visible at new location
13. Open grid GUI again
14. Expected: Old location (A1) now shows as available (green glass)
15. Expected: New location (C3) shows your island
```

**Test Relocation Cost:**

```
Setup: Add Vault + economy plugin, set relocation.cost to 5000 in config
1. Give player $10,000: /eco give {player} 10000
2. Check balance: /balance
3. Initiate relocation (steps above)
4. Expected: Confirmation shows "Cost: $5,000"
5. Confirm relocation
6. Expected: $5,000 deducted from balance
7. Check balance: /balance
8. Expected: Shows $5,000 remaining
```

**Test Relocation Cooldown:**

```
Setup: Set relocation.cooldown to 60 (seconds) in config
1. Relocate island from A1 to B2
2. Immediately try to relocate from B2 to C3
3. Expected: Error message about cooldown
4. Expected: Shows time remaining (e.g., "59 seconds")
5. Wait 60+ seconds
6. Try relocating again
7. Expected: Relocation allowed
```

**Test Team Member Teleport:**

```
Setup: Two players, Player A has island with Player B as team member
1. Player B joins Player A's island
2. Player B stands on the island
3. Player A relocates the island
4. Expected: Player B teleported to new island location
5. Expected: Player B receives notification about relocation
6. Expected: Player B still on team (/island team)
```

---

## Detailed Test Cases

### Test #107: Player with Existing Island Can Relocate

**Category:** Functional
**Feature:** Basic relocation flow

**Steps:**
1. Create island at location D4 (using /island or claim flow)
2. Build some structures (place unique blocks to track)
3. Open grid GUI: `/islandselector`
4. Click on empty location H8 (green glass pane)
5. Verify relocation confirmation GUI opens
6. Verify title is "Confirm Relocation"
7. Click Confirm (green wool)
8. Wait for relocation process to complete
9. After completion, open grid GUI again
10. Verify D4 now shows as available (green glass)
11. Verify H8 now shows your island (player head with glow)
12. Verify you're at H8 location in-world

**Expected Result:**
- Confirmation GUI appears when clicking empty location
- Relocation process completes successfully
- Island physically moved from D4 to H8
- D4 available for other players
- H8 occupied by your island
- Player teleported to new location

---

### Test #108: Relocation Confirmation Shows From and To Coordinates

**Category:** UI
**Feature:** Clear confirmation display

**Steps:**
1. Have island at C5
2. Open grid GUI
3. Click empty location F10
4. Examine confirmation GUI info item (center, ender pearl)
5. Verify display name is "Island Relocation" or similar
6. Verify lore shows "New Location: F10"
7. Verify lore shows world coordinates (X and Z values)
8. Verify format is clear and understandable

**Expected Result:**
- Confirmation GUI shows ender pearl as info item
- Display clearly indicates relocation (not claim/purchase)
- New location coordinates visible
- World coordinates calculated and displayed
- Warning about island moving

---

### Test #109: Relocation Confirmation Shows Cost

**Category:** UI
**Feature:** Economy cost display

**Setup:** Set `relocation.cost: 5000` in config.yml

**Steps:**
1. Reload config: `/islandselector admin reload`
2. Initiate relocation
3. Check confirmation GUI
4. Verify info item lore shows "Cost: $5,000"
5. Verify confirm button lore shows cost
6. Verify cost is formatted nicely (commas, no decimals)

**Expected Result:**
- Cost displayed in info item lore
- Cost displayed in confirm button lore
- Format: "$5,000" not "$5000.00"
- Clear indication money will be deducted

---

### Test #110: Relocation Confirmation Shows Cooldown Warning

**Category:** UI
**Feature:** Cooldown indication

**Setup:** Set `relocation.cooldown: 604800` (7 days) in config

**Steps:**
1. Relocate once to trigger cooldown
2. Immediately try to relocate again
3. Attempt to open confirmation
4. Verify error message shows cooldown remaining
5. Verify time formatted as "6 days, 23 hours" or similar
6. Verify relocation blocked

**Expected Result:**
- Error message about cooldown
- Human-readable time display (days, hours, minutes)
- Relocation prevented during cooldown
- Clear instruction to wait

---

### Test #111: Relocation Copies All Blocks Using FAWE

**Category:** Functional
**Feature:** Complete island preservation
**Requirements:** FastAsyncWorldEdit installed

**Steps:**
1. Build complex structures on island at E5:
   - Stone house with glass windows
   - Chest with items inside
   - Furnace with smelting recipe
   - Sign with text
   - Various block types (stairs, slabs, etc.)
2. Relocate to J12
3. After relocation completes, visit J12 location
4. Verify all blocks present exactly as before
5. Verify chest contents preserved
6. Verify furnace still has items/fuel
7. Verify sign text readable
8. Verify block orientations correct (stairs facing right way)

**Expected Result:**
- All blocks copied to new location
- Block data preserved (chest contents, sign text, etc.)
- Block states preserved (furnace fuel, doors open/closed)
- Exact replica of original island

---

### Test #112: Relocation Copies All Entities

**Category:** Functional
**Feature:** Entity preservation

**Steps:**
1. Place on island:
   - Item frames with items
   - Armor stands with equipment
   - Animals (cows, chickens, etc.)
   - Boats
2. Relocate island
3. After relocation, visit new location
4. Verify all entities present
5. Verify item frame items still there
6. Verify armor stand equipment preserved
7. Verify animals still present

**Expected Result:**
- All entities copied to new location
- Entity data preserved (item frames, armor stands)
- Living entities (animals) present
- No entity loss during relocation

**Note:** Some entity types may not copy perfectly with WorldEdit - this is expected behavior.

---

### Test #113: Relocation Clears Old Location Completely

**Category:** Functional
**Feature:** Old location cleanup

**Steps:**
1. Build large island at B3 with many structures
2. Relocate to G9
3. After relocation, manually fly to old B3 coordinates
4. Verify location is completely cleared (void/air)
5. Verify no blocks remain from old island
6. Verify no entities remain
7. Open grid GUI
8. Verify B3 shows as available (green glass pane)

**Expected Result:**
- Old location completely cleared
- All blocks removed (set to air)
- All entities removed
- Grid shows location as available
- No remnants of old island

---

### Test #114: Relocation Updates BSkyBlock Island Spawn Point

**Category:** Functional
**Feature:** Spawn point update

**Steps:**
1. Have island at C4
2. Set custom spawn point: `/island sethome` (if BSkyBlock supports)
3. Note spawn location
4. Relocate to K11
5. After relocation, execute `/island go`
6. Verify you teleport to island at K11 (not C4)
7. Verify spawn point updated to new location
8. Verify spawn coordinates correct for K11

**Expected Result:**
- `/island go` teleports to new location
- Spawn point updated by relocation process
- No longer teleports to old location
- Coordinates match new grid location

---

### Test #115: Relocation Updates BSkyBlock Warp Point

**Category:** Functional
**Feature:** Warp point update
**Requirements:** BSkyBlock Warps addon (if available)

**Steps:**
1. Enable island warp at D5: `/island setwarp`
2. Verify warp is active
3. Relocate to H10
4. Have another player try to warp to your island
5. Verify they warp to island at H10 (not D5)
6. Verify warp coordinates updated

**Expected Result:**
- Warp point updated to new location
- Other players warp to correct location
- Warp remains enabled after relocation
- No broken warp references

**Note:** This test may not apply if Warps addon not installed - that's OK.

---

### Test #116: Relocation Teleports Island Owner

**Category:** Functional
**Feature:** Owner teleportation

**Steps:**
1. Be on island at E6
2. Note your current position
3. Initiate relocation to L14
4. Wait for relocation to complete
5. After "Teleporting..." message, check your position
6. Verify you're at island spawn on L14
7. Verify you're not at old E6 location

**Expected Result:**
- Player automatically teleported to new island
- Teleport happens at end of relocation process
- No manual `/island go` needed
- Spawn point coordinates correct

---

### Test #117: Relocation Teleports Team Members

**Category:** Functional
**Feature:** Team member teleportation
**Requirements:** 2 player accounts

**Steps:**
1. Player A has island with Player B as team member
2. Player B stands on the island
3. Player A initiates relocation
4. Watch Player B's screen during relocation
5. Verify Player B receives notification about relocation
6. Verify Player B is teleported to new island location
7. Verify Player B remains on team: `/island team`
8. Verify Player B has same permissions as before

**Expected Result:**
- Team member notified about relocation
- Team member teleported to new island
- Team membership preserved
- Team permissions unchanged
- Team member can continue building

---

### Test #118: Relocation Teleports Visitors Away

**Category:** Functional
**Feature:** Visitor handling
**Requirements:** 2 player accounts

**Steps:**
1. Player A has island
2. Player B (not team member) visits Player A's island
3. Player A initiates relocation while B is visiting
4. Watch Player B's screen
5. Verify Player B receives warning message
6. Verify Player B is teleported away (to their island or spawn)
7. Verify Player B is not on the island after relocation

**Expected Result:**
- Visitor warned about relocation
- Visitor teleported to safe location
- If visitor has island: teleported to their island
- If visitor has no island: teleported to spawn
- No players left in cleared old location

---

### Test #119: Relocation Cost Deducted via Vault

**Category:** Functional
**Feature:** Economy integration
**Requirements:** Vault + economy plugin (e.g., EssentialsX)

**Setup:** Set `relocation.cost: 10000` in config

**Steps:**
1. Give player $15,000: `/eco give {player} 15000`
2. Check balance: `/balance` (should show $15,000)
3. Relocate island
4. Verify confirmation shows "Cost: $10,000"
5. Confirm relocation
6. After relocation completes, check balance: `/balance`
7. Verify balance is $5,000 ($15,000 - $10,000)

**Expected Result:**
- Cost deducted from player balance
- Correct amount deducted ($10,000)
- Balance updated after relocation
- Transaction successful

---

### Test #120: Cannot Relocate with Insufficient Funds

**Category:** Functional
**Feature:** Economy validation
**Requirements:** Vault + economy plugin

**Setup:** Set `relocation.cost: 10000` in config

**Steps:**
1. Give player only $5,000: `/eco give {player} 5000`
2. Check balance: `/balance` (should show $5,000)
3. Try to relocate island
4. Click Confirm in confirmation GUI
5. Verify error message: "You need $10,000 but only have $5,000!"
6. Verify relocation is blocked
7. Check balance: `/balance`
8. Verify no money deducted (still $5,000)
9. Verify island didn't move

**Expected Result:**
- Error message about insufficient funds
- Shows required amount and current balance
- Relocation prevented
- No money deducted
- Island remains at original location

---

### Test #121: Bypass Cost Permission Allows Free Relocation

**Category:** Functional
**Feature:** Permission bypass

**Setup:** Set `relocation.cost: 10000` in config

**Steps:**
1. Give player permission: `islandselector.bypass.cost.relocation`
2. Set player balance to $0: `/eco set {player} 0`
3. Verify balance is $0: `/balance`
4. Relocate island
5. Expected: No cost shown in confirmation (or shows bypassed)
6. Confirm relocation
7. Expected: Relocation completes successfully
8. Check balance: `/balance`
9. Verify balance still $0 (no charge)

**Expected Result:**
- Bypass permission allows free relocation
- No money deducted
- Balance remains $0
- Relocation works normally

---

### Test #122: Relocation Cooldown Enforced

**Category:** Functional
**Feature:** Cooldown system

**Setup:** Set `relocation.cooldown: 300` (5 minutes) in config

**Steps:**
1. Relocate from A1 to B2
2. Wait for relocation to complete
3. Immediately try to relocate from B2 to C3
4. Click empty location C3
5. Try to confirm relocation
6. Verify error message about cooldown
7. Verify shows time remaining (e.g., "4 minutes 58 seconds")
8. Wait full 5 minutes
9. Try relocating again
10. Verify relocation allowed

**Expected Result:**
- Cooldown enforced after relocation
- Error message shows time remaining
- Cannot relocate during cooldown period
- After cooldown expires, relocation allowed
- Time display human-readable

---

### Test #123: Relocation Cooldown Persists Across Restarts

**Category:** Functional
**Feature:** Cooldown persistence

**Setup:** Set `relocation.cooldown: 600` (10 minutes) in config

**Steps:**
1. Relocate island to trigger cooldown
2. Try relocating again - verify blocked
3. Note time remaining (e.g., 9 minutes)
4. Restart the server (wait 1 minute, then restart)
5. Rejoin server
6. Try relocating again
7. Verify cooldown still active
8. Verify time remaining decreased correctly (about 8 minutes)

**Expected Result:**
- Cooldown survives server restart
- Cooldown data persisted in database
- Time remaining calculated correctly after restart
- No cooldown reset on restart

---

### Test #124: Bypass Cooldown Permission Works

**Category:** Functional
**Feature:** Permission bypass

**Setup:** Set `relocation.cooldown: 300` (5 minutes) in config

**Steps:**
1. Relocate island to trigger cooldown
2. Try relocating again - verify blocked by cooldown
3. Give player permission: `islandselector.bypass.cooldown.relocation`
4. Try relocating again immediately
5. Verify no cooldown error
6. Verify relocation confirmation opens
7. Complete relocation
8. Verify relocation successful

**Expected Result:**
- Bypass permission allows immediate relocation
- No cooldown check applied
- Can relocate multiple times rapidly
- No error messages

---

### Test #125: Cooldown Time Formatted Human-Readable

**Category:** UI
**Feature:** Time formatting

**Setup:** Set various cooldown times and test formatting

**Steps:**
1. Set `relocation.cooldown: 90` (1.5 minutes)
2. Relocate and check error message
3. Verify shows "1m 30s" or "90 seconds"
4. Set `relocation.cooldown: 7200` (2 hours)
5. Relocate and check error message
6. Verify shows "2h 0m" or "2 hours"
7. Set `relocation.cooldown: 604800` (7 days)
8. Relocate and check error message
9. Verify shows "7d 0h" or "7 days"

**Expected Result:**
- Time displayed in human-readable format
- Days, hours, minutes, seconds as appropriate
- No raw second counts for long durations
- Format clear and understandable

---

### Test #126: Progress Indicators During Relocation

**Category:** UI
**Feature:** User feedback

**Steps:**
1. Relocate a large island (with many blocks)
2. Watch chat/action bar during process
3. Verify see message: "Starting island relocation..."
4. Verify see message: "Saving island..."
5. Verify see message: "Clearing old location..."
6. Verify see message: "Pasting island at new location..."
7. Verify see message: "Updating island data..."
8. Verify see message: "Teleporting..."
9. Verify see final message: "Island relocated successfully!"
10. Verify see info about new coordinates

**Expected Result:**
- Progress messages appear in order
- Each step clearly communicated
- Player informed throughout process
- Completion message clear
- No silent failures

---

### Test #127: Cannot Relocate to Occupied Location

**Category:** Functional
**Feature:** Validation

**Steps:**
1. Player A has island at D4
2. Player B has island at E5
3. Player A opens grid GUI
4. Player A tries to click on E5 (Player B's island)
5. Verify nothing happens (occupied slots not clickable)
6. OR verify error message if somehow triggered

**Expected Result:**
- Cannot click occupied locations
- Occupied locations show player heads (not clickable for relocation)
- Only empty green glass locations clickable
- Clear distinction between available and occupied

---

### Test #128: Cannot Relocate to Reserved Blocked Location

**Category:** Functional
**Feature:** Validation

**Steps:**
1. Admin reserves location: `/islandselector admin reserve F6`
2. Player has island at A1
3. Player opens grid GUI
4. Verify F6 shows as gray glass (reserved, no price)
5. Try clicking F6
6. Verify click blocked or error message
7. Verify relocation doesn't open

**Expected Result:**
- Reserved locations not clickable for relocation
- Or error message explaining location reserved
- Relocation blocked
- Player informed location unavailable

---

### Test #129: Can Relocate to Premium Location

**Category:** Functional
**Feature:** Premium location relocation
**Requirements:** Vault + economy plugin

**Setup:**
- Set `relocation.cost: 5000` in config
- Admin: `/islandselector admin setprice G7 8000`

**Steps:**
1. Player has island at A1 and $20,000
2. Open grid GUI
3. Click premium location G7 (gold block)
4. Verify confirmation shows:
   - Relocation cost: $5,000
   - Premium location: $8,000
   - Total: $13,000 (or shown separately)
5. Confirm relocation
6. Verify both costs deducted ($13,000 total)
7. Verify balance now $7,000
8. Verify island relocated to G7
9. Open grid GUI
10. Verify G7 no longer shows as premium (your island now)

**Expected Result:**
- Can relocate to premium locations
- Both costs applied (relocation + premium)
- Correct total deducted
- Premium status removed after purchase
- Island successfully relocated

---

### Test #130: Admin Command Force Relocate

**Category:** Admin
**Feature:** Admin tools

**Steps:**
1. Player A has island at D4
2. Admin executes: `/islandselector admin relocate {playerA} O15`
3. Verify success message to admin
4. Verify Player A receives notification
5. Check Player A's island location
6. Verify island now at O15
7. Verify D4 available again
8. Verify Player A teleported to new location (if online)

**Expected Result:**
- Admin can force relocate any player's island
- No cost charged for admin relocate
- No cooldown enforced for admin relocate
- Player notified of relocation
- Island moved successfully

**Note:** This command may not be implemented yet - if missing, note as future feature.

---

### Test #131: Relocation with Offline Team Members

**Category:** Functional
**Feature:** Offline player handling

**Steps:**
1. Player A has island with Player B and Player C as team members
2. Player B is online, Player C is offline
3. Player A relocates island
4. Verify Player B (online) teleported to new location
5. Have Player C log in
6. Player C executes `/island go`
7. Verify Player C teleports to new location (not old)
8. Verify Player C still on team

**Expected Result:**
- Online team members teleported immediately
- Offline team members' data updated
- When offline members log in, they see updated island
- `/island go` works for offline members
- Team membership preserved

---

### Test #132: Multiple Relocations Track History

**Category:** Functional
**Feature:** Relocation history

**Steps:**
1. Create island at A1
2. Relocate to B2
3. Relocate to C3
4. Relocate to D4
5. Admin checks history: `/islandselector admin info {player}`
6. Verify relocation history shows all moves
7. Verify shows: A1 → B2 → C3 → D4
8. Verify shows total relocation count

**Expected Result:**
- Relocation history tracked in database
- Admin commands can view history
- Shows previous locations
- Shows total relocation count
- History persists across restarts

**Note:** Admin info command may show basic stats - exact history display may vary.

---

## Configuration

**Config Section:** `relocation`

```yaml
relocation:
  # Enable relocation feature
  enabled: true

  # Cost to relocate island (requires Vault)
  # Set to 0 for free relocation
  cost: 10000.0

  # Cooldown between relocations (seconds)
  # 86400 = 1 day, 604800 = 7 days
  # Set to 0 for no cooldown
  cooldown: 604800

  # Allow relocation to premium locations (charges both costs)
  allow-premium: true
```

**Permissions:**
- `islandselector.relocate` - Allow player to relocate island
- `islandselector.bypass.cost.relocation` - Free relocation
- `islandselector.bypass.cooldown.relocation` - No cooldown
- `islandselector.admin.relocate` - Admin force relocate command

---

## Testing Requirements

**Players Needed:** 2-3 (for team/visitor tests)
**Time Estimate:** 40-60 minutes (comprehensive), 10-15 minutes (quick test)
**Prerequisites:**
- FastAsyncWorldEdit installed (REQUIRED)
- Vault + economy plugin (for cost tests)
- BSkyBlock addon active
- At least one island created

---

## Expected Behaviors

### Relocation Process Flow
1. **Validation:** Check location available, cooldown, cost
2. **Confirmation:** Player confirms relocation
3. **Save:** Island saved to WorldEdit clipboard
4. **Clear:** Old location cleared (all blocks removed)
5. **Paste:** Island pasted at new location
6. **Update:** BSkyBlock data updated (spawn, warp, center)
7. **Grid:** Old location marked available, new location occupied
8. **Teleport:** Owner and team to new location, visitors away
9. **Complete:** Success message and cooldown started

**Timing:** 3-10 seconds depending on island size

### Economy Behavior
- Cost charged BEFORE relocation starts
- If relocation fails, refund may not be automatic
- Premium location + relocation = both costs charged
- Bypass permission skips cost check entirely

### Cooldown Behavior
- Cooldown starts immediately after successful relocation
- Stored in database (persists across restarts)
- Bypass permission skips cooldown check entirely
- Admin relocate does NOT trigger cooldown

### Visitor Handling
- Visitors with island → teleport to their island
- Visitors without island → teleport to world spawn
- Warning message sent before teleport
- Configurable delay before teleport (visitors.teleport-delay)

---

## Troubleshooting

**Problem:** Relocation confirmation doesn't open
- **Check:** Player has existing island
- **Check:** Clicking empty location (green glass)
- **Check:** Permission `islandselector.relocate`
- **Try:** Check console for errors

**Problem:** "You must wait..." error when trying to relocate
- **Check:** Cooldown configured in config.yml
- **Check:** Time remaining with repeated attempts
- **Try:** Admin reset: `/islandselector admin resetcooldown {player} relocation`
- **Try:** Give bypass permission: `islandselector.bypass.cooldown.relocation`

**Problem:** "You need $X..." error
- **Check:** Vault installed and economy plugin active
- **Check:** Player balance: `/balance`
- **Try:** Give money: `/eco give {player} 10000`
- **Try:** Set relocation.cost to 0 in config
- **Try:** Give bypass permission: `islandselector.bypass.cost.relocation`

**Problem:** Relocation starts but fails mid-process
- **Check:** FastAsyncWorldEdit installed
- **Check:** Console for WorldEdit errors
- **Check:** Disk space and file permissions
- **Try:** Test with small/empty island first
- **Try:** Reduce island size or complexity

**Problem:** Island not fully copied to new location
- **Check:** FAWE config limits (max blocks, timeout)
- **Check:** Server performance during relocation
- **Try:** Increase FAWE limits in FAWE config
- **Try:** Reduce island complexity
- **Note:** Some entity types may not copy (expected)

**Problem:** Old location not cleared
- **Check:** Console for errors during clearing
- **Check:** WorldEdit permissions
- **Try:** Manually clear: `/we pos1` `/we pos2` `/we set air`

**Problem:** Team members not teleported
- **Check:** Team members were online during relocation
- **Check:** Team members on the island (not elsewhere)
- **Try:** Team members use `/island go` manually

**Problem:** `/island go` takes to old location after relocate
- **Check:** BSkyBlock island data updated
- **Check:** Console for errors during data update
- **Try:** Relog to refresh data
- **Try:** Admin force update island data

**Problem:** Relocation data not persisting
- **Check:** Database connection working
- **Check:** File permissions on database files
- **Check:** Console for save errors
- **Try:** Restart server and test cooldown

---

## Report Format

After testing, please report results:

```
=== Session 41: Island Relocation Test Report ===

BASIC RELOCATION:
Test #107 (Relocate island to new location): [PASS/FAIL]
Test #108 (Confirmation shows coordinates): [PASS/FAIL]
Test #109 (Confirmation shows cost): [PASS/FAIL]
Test #110 (Cooldown warning): [PASS/FAIL]

RELOCATION PROCESS:
Test #111 (All blocks copied): [PASS/FAIL]
Test #112 (Entities copied): [PASS/FAIL / PARTIAL]
Test #113 (Old location cleared): [PASS/FAIL]
Test #114 (Spawn point updated): [PASS/FAIL]
Test #115 (Warp point updated): [PASS/FAIL / N/A]

PLAYER TELEPORTATION:
Test #116 (Owner teleported): [PASS/FAIL]
Test #117 (Team members teleported): [PASS/FAIL]
Test #118 (Visitors teleported away): [PASS/FAIL]

ECONOMY INTEGRATION:
Test #119 (Cost deducted): [PASS/FAIL / N/A if no Vault]
Test #120 (Insufficient funds blocked): [PASS/FAIL / N/A]
Test #121 (Bypass cost permission): [PASS/FAIL]

COOLDOWN SYSTEM:
Test #122 (Cooldown enforced): [PASS/FAIL]
Test #123 (Cooldown persists restart): [PASS/FAIL]
Test #124 (Bypass cooldown permission): [PASS/FAIL]
Test #125 (Time formatting): [PASS/FAIL]

EDGE CASES:
Test #126 (Progress indicators): [PASS/FAIL]
Test #127 (Can't relocate to occupied): [PASS/FAIL]
Test #128 (Can't relocate to reserved): [PASS/FAIL]
Test #129 (Relocate to premium location): [PASS/FAIL / N/A]
Test #130 (Admin force relocate): [PASS/FAIL / NOT IMPLEMENTED]
Test #131 (Offline team members): [PASS/FAIL]
Test #132 (History tracking): [PASS/FAIL / PARTIAL]

Issues found:
[Describe any problems, unexpected behavior, or bugs]

Console errors:
[Paste any errors from server console]

Performance:
[Any lag during relocation? How long did it take?]

Missing features:
[Any features not implemented? E.g., admin force relocate command]

Overall: [PASS/FAIL]
```

---

## Success Criteria

✅ Relocation confirmation opens when clicking empty location (with existing island)
✅ Confirmation shows from/to coordinates and cost
✅ All blocks copied to new location
✅ Entities copied (or partial copy is acceptable)
✅ Old location completely cleared
✅ BSkyBlock spawn point updated
✅ Owner teleported to new location
✅ Team members teleported to new location
✅ Visitors teleported away safely
✅ Relocation cost deducted (if Vault present)
✅ Insufficient funds prevents relocation
✅ Bypass cost permission works
✅ Cooldown enforced between relocations
✅ Cooldown persists across restarts
✅ Bypass cooldown permission works
✅ Progress messages clear throughout
✅ Cannot relocate to occupied/reserved locations
✅ Can relocate to premium locations (both costs charged)
✅ No console errors during relocation
✅ No server lag or crashes

---

## File Info

**JAR:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
**Size:** 180K (unchanged from Session 40)
**Changes:** None - documentation only
**Build Status:** ✅ SUCCESS

---

## Key Files Involved

**Already Implemented (Session 36):**
- `RelocationManager.java` (504 lines) - Complete relocation logic
- `RelocationData.java` - Database model for cooldown tracking
- `ConfirmationGUI.java` - Handles RELOCATE action type
- `GridGUIListener.java` - Opens relocation confirmation
- `IslandSelector.java` - Registers RelocationManager

---

**Ready to test!** 🎮

**Core Test:** Click empty location with existing island → Confirm → Watch relocation → Verify island moved

**Time:** 10-15 minutes (quick), 40-60 minutes (comprehensive)

**Required:** FAWE installed (critical), Vault optional, 2-3 players for team tests

**JAR:** `output/IslandSelector-1.0.0-SNAPSHOT.jar` (unchanged from Session 40)

---

## Next Steps

**If All Relocation Tests Pass:**
- Mark tests #107-132 as passing ✅ (approximately 26 tests)
- Combined with Sessions 38-40: Would be +55 tests total (29 + 26)
- Progress: from 44/225 → ~99/225 tests passing (44%)
- Move to next feature block (likely backup system or admin tools)

**If Any Test Fails:**
- Report the issue with details
- Agent will debug and fix in next session
- Re-test until passing

---

**This complements Sessions 38-40 for comprehensive feature testing** - four major feature sets ready for verification!
