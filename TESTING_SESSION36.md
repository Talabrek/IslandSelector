# Session 36 - Island Relocation Testing

## Quick Summary

**Feature:** Island Relocation
**Purpose:** Players can move their entire island to a new grid location
**Build Status:** ✅ SUCCESS
**Your Task:** Test island relocation functionality

---

## What Was Built

Island relocation system that allows players to physically move their island to a different grid location:

### Core Features:
1. **Island Relocation** - Complete block-by-block copy to new location
2. **Cooldown System** - Configurable cooldown between relocations (default: 7 days)
3. **Cost System** - Optional Vault economy integration for relocation fees
4. **Confirmation GUI** - Preview location and costs before relocating
5. **Grid Integration** - Click empty locations in grid GUI to relocate

### Technical Details:
- Uses FAWE (FastAsyncWorldEdit) for async operations (no server lag)
- Copies all blocks, entities, chests, signs, etc.
- Clears old location (makes it available again)
- Updates BSkyBlock island data (spawn, warp points)
- Teleports owner, team members, and visitors
- Tracks relocation history in database

---

## Quick 10-Minute Test

### Setup
1. Copy JAR: `output/IslandSelector-1.0.0-SNAPSHOT.jar` → `plugins/BentoBox/addons/`
2. Restart server
3. **Important:** Have FAWE (FastAsyncWorldEdit) installed!

### Test Steps

**1. Create an island:**
```
- Join server
- Run: /island create
- Select a grid location (e.g., 0,0)
- Build something simple (e.g., 5 blocks of dirt, place a sign)
```

**2. Try to relocate:**
```
- Run: /islandselector
- Click on an empty grid location (e.g., 1,1)
- Verify confirmation GUI opens showing:
  * New location coordinates
  * Relocation cost (if configured)
  * Warning about island moving
```

**3. Confirm relocation:**
```
- Click green "CONFIRM" button
- Watch chat for progress messages:
  * "Starting island relocation..."
  * "Saving island..."
  * "Clearing old location..."
  * "Pasting island at new location..."
  * "Updating island data..."
  * "Island relocated successfully!"
```

**4. Verify:**
```
- Check your new location - island should be there
- Check old location - should be empty/cleared
- Run: /island go - should teleport to new location
- Open grid GUI - old location should show as available
```

**Expected:** ✅ Island moves successfully, old location is cleared

---

## What to Test

### Test #1: Basic Relocation
**Steps:**
1. Create island at location A (0,0)
2. Build a simple structure (5 blocks)
3. Open grid GUI, click empty location B (1,1)
4. Confirm relocation
5. Wait for completion (progress messages)
6. Check location B - island should be there with your 5 blocks
7. Check location A - should be empty/cleared
8. Open grid GUI - location A should show as green (available)

**Expected:** Island moves completely, old location cleared

### Test #2: Cooldown System
**Steps:**
1. Relocate your island once (Test #1)
2. Immediately try to relocate again
3. Click another empty location in grid GUI
4. Try to confirm

**Expected:** Error message "You must wait X time before relocating again!"

### Test #3: Cost System (if Vault is installed)
**Config:** Set `relocation.cost: 1000` in config.yml and reload
**Steps:**
1. Give yourself $500: `/eco give YourName 500`
2. Try to relocate
3. Click empty location, click confirm
4. Should see error: "You need $1,000.00 to relocate!"
5. Give yourself $1000 more: `/eco give YourName 1000`
6. Try again - should succeed and deduct $1000

**Expected:** Cost checking works, money deducted on successful relocation

### Test #4: Bypass Permissions
**Steps:**
1. Give yourself bypass: `/lp user YourName permission set islandselector.bypass.cooldown.relocation`
2. Try to relocate immediately after previous relocation
3. Should work (no cooldown)
4. Give bypass for cost: `/lp user YourName permission set islandselector.bypass.cost.relocation`
5. Try to relocate with $0
6. Should work (no cost)

**Expected:** Bypass permissions work correctly

### Test #5: Complex Island Relocation
**Steps:**
1. Build a more complex island:
   - Multiple types of blocks
   - Place a chest with items inside
   - Place signs with text
   - Place item frames with items
   - Tame an animal (optional)
2. Relocate island
3. Verify at new location:
   - All blocks are correct
   - Chest still has items
   - Signs still have text
   - Item frames still have items
   - Animal is still there (if applicable)

**Expected:** All blocks, tile entities, and entities are copied correctly

### Test #6: Team Member Teleportation
**Steps:**
1. Invite a friend to your island team
2. Have them stand on your island
3. Relocate your island
4. Check if team member was teleported to new location

**Expected:** Team members are teleported with the island

### Test #7: Visitor Handling
**Steps:**
1. Have a visitor (non-team player) visit your island
2. Have them stand on your island
3. Relocate your island
4. Check what happens to the visitor

**Expected:** Visitor is teleported away (to spawn or their island)

### Test #8: Grid Updates
**Steps:**
1. Note your starting location (e.g., 2,2)
2. Relocate to new location (e.g., 3,3)
3. Open grid GUI
4. Check location 2,2 - should be green (available)
5. Check location 3,3 - should show your island

**Expected:** Grid properly updates old and new locations

### Test #9: BSkyBlock Integration
**Steps:**
1. Before relocating, set your island warp: `/island setwarp`
2. Have a friend try to warp: `/island warp YourName`
3. Relocate your island
4. Have friend try warp again

**Expected:** Warp should work at new location (or may need to be set again)

### Test #10: Admin Reset Cooldown
**Steps:**
1. Relocate your island (starts cooldown)
2. Try to relocate again - should be blocked
3. Admin runs: `/islandselector admin resetcooldown YourName`
4. Try to relocate again

**Expected:** Cooldown is cleared, can relocate immediately

---

## Expected Behavior

### During Relocation:
```
Messages you should see:
"Starting island relocation..."
"Saving island..."
"Clearing old location..."
"Pasting island at new location..."
"Updating island data..."
"Teleporting..."
"Island relocated successfully!"
"From: 0,0 → 1,1"
"New coordinates: X: 500, Z: 500"
```

### Console:
```
[IslandSelector] Saved island to clipboard for relocation
[IslandSelector] Cleared island blocks at old location
[IslandSelector] Pasted island to new location
```

### Relocation should NOT:
- Cause server lag (FAWE handles it async)
- Lose any blocks or items
- Leave blocks at old location
- Break BSkyBlock island data
- Crash the server

---

## Configuration

**Config.yml settings (relocation section):**
```yaml
relocation:
  cooldown: 604800  # 7 days in seconds (set to 60 for testing)
  cost: 0.0         # Set to 1000.0 to test costs
  currency-name: "coins"
```

**For Testing:** Set cooldown to 60 seconds to test faster!

---

## Permissions

```
islandselector.bypass.cooldown.relocation  # Bypass relocation cooldown
islandselector.bypass.cost.relocation      # Bypass relocation cost
```

---

## Commands Reference

```bash
# Player commands
/islandselector              # Open grid GUI, click location to relocate

# Admin commands
/islandselector admin resetcooldown <player>  # Reset relocation cooldown
/islandselector admin reload                  # Reload config
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Could not find your island!" | Make sure you have an island created |
| "This location is no longer available!" | Someone claimed it first, choose another |
| Relocation hangs/freezes | Check FAWE is installed and working |
| Island doesn't appear at new location | Check console for errors, may need FAWE update |
| Old location not cleared | Check console, may be permissions issue |
| Server lags during relocation | Should NOT happen with FAWE - check FAWE config |

---

## Report Format

```
=== Island Relocation Test Report ===

Basic Relocation: [PASS/FAIL]
  - Island moved: Yes/No
  - Old location cleared: Yes/No
  - Blocks preserved: Yes/No
  - Items/entities preserved: Yes/No
  - Issues: [describe any problems]

Cooldown System: [PASS/FAIL]
  - Cooldown enforced: Yes/No
  - Time display correct: Yes/No
  - Bypass permission works: Yes/No

Cost System: [PASS/FAIL]
  - Cost checking works: Yes/No
  - Money deducted: Yes/No
  - Bypass permission works: Yes/No

Grid Integration: [PASS/FAIL]
  - Old location shows available: Yes/No
  - New location shows occupied: Yes/No
  - Confirmation GUI works: Yes/No

BSkyBlock Integration: [PASS/FAIL]
  - /island go works: Yes/No
  - Spawn point updated: Yes/No
  - Team members teleported: Yes/No

Performance: [PASS/FAIL]
  - No server lag: Yes/No
  - Async operation: Yes/No
  - Completes in reasonable time: Yes/No

Overall: [PASS/FAIL]
Console Errors: [paste if any]
```

---

## What Makes This Pass?

**Minimum Requirements:**
✅ Can relocate island to empty location
✅ All blocks are copied to new location
✅ Old location is cleared
✅ No console errors
✅ Command doesn't crash server

**Full Pass:**
✅ All above +
✅ Cooldown system works
✅ Cost system works (if Vault installed)
✅ Tile entities (chests, signs) preserved
✅ Entities (item frames, animals) preserved
✅ Grid GUI updates correctly
✅ Team members teleported
✅ Visitors handled properly
✅ No server lag during operation
✅ BSkyBlock integration works (/island go, etc.)

---

## File Info

**JAR:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
**Size:** 179K (increased 11K from Session 35)
**Changes:** +3 new files, +2 modified files

**New Files:**
- RelocationData.java (database model for cooldowns)
- RelocationManager.java (relocation logic with FAWE)

**Modified Files:**
- ConfirmationGUI.java (handle RELOCATE action)
- GridGUIListener.java (open relocation confirmation)
- IslandSelector.java (register RelocationManager)

---

## Why This Feature Matters

**Player Benefits:**
- Choose better location after learning the server
- Move away from inactive neighbors
- Upgrade to premium locations
- Fix mistakes in initial location choice

**Server Benefits:**
- Monetization opportunity (charge for relocations)
- Player retention (flexibility = happy players)
- Premium location sales
- Dynamic world management

**Technical Achievement:**
- Complex FAWE integration
- Async operations (no lag)
- Complete data preservation
- Cooldown/economy systems

---

## Next After This

**If This Passes:**
- Island relocation feature complete! 🎉
- All major features now implemented
- Focus shifts to testing/bug fixes
- Possible enhancements:
  * Backup before relocation
  * Relocation preview
  * Undo relocation

**If This Fails:**
- Report issues clearly with steps to reproduce
- Note what works and what doesn't
- Check console for error messages
- We'll fix before moving on

---

## Questions?

**Not sure how to test?**
- Follow the "Quick 10-Minute Test" section
- That covers the essential functionality

**Want more thorough testing?**
- Go through all 10 test cases above
- Test edge cases and error conditions

**Something broken?**
- Note exactly what you did
- Copy any console errors
- Report what you expected vs. what happened
- Include configuration settings used

---

**Ready to test!** 🎯

**JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`

**Core Test:** Create island → Relocate → Verify

**Expected Time:** 10-20 minutes

**Required:** FastAsyncWorldEdit (FAWE) must be installed!
