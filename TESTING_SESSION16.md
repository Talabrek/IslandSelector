# IslandSelector - Session 16 Testing Instructions

## Build Information
- **Session:** 16
- **Build Date:** 2025-12-02
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **JAR Size:** 120K
- **Status:** ✅ Visitor teleportation implemented

---

## What's New in Session 16

### 🎯 Feature: Visitor Teleportation During Slot Switch (Tests #71-72)

**Summary:**
Implemented automatic visitor detection and teleportation when a player switches island slots. This prevents visitors from being left behind when an island is swapped out.

**How it works:**
1. When a player switches slots, the system detects all visitors on the island
2. Visitors (non-team members) receive a warning message
3. After a configurable delay (default: 2 seconds), visitors are teleported away
4. Visitors with their own island are sent home
5. Visitors without an island are sent to world spawn

**Key Features:**
- ✅ Automatic visitor detection during slot switch
- ✅ Warning message before teleportation
- ✅ Configurable delay (40 ticks / 2 seconds default)
- ✅ Smart destination: visitor's island or spawn
- ✅ Team members are NOT teleported (they stay with the island)
- ✅ Notification messages to visitors

---

## Tests to Perform

### Test #71: Visitor Teleportation to Own Island

**Prerequisites:**
- Two players: Player A (island owner) and Player B (visitor with own island)
- Player A has at least 2 island slots with different islands

**Steps:**
1. Player B creates their own island first
2. Player B visits Player A's island (slot 1)
3. Player A switches from slot 1 to slot 2
4. Observe what happens to Player B

**Expected Results:**
- ✅ Player B sees warning message: "&eThe island owner is switching islands. You will be teleported in 2 seconds..."
- ✅ After 2 seconds, Player B is teleported to their own island
- ✅ Player B sees message: "&eYou have been teleported away as the island is switching slots."
- ✅ Player B is NOT on Player A's island anymore
- ✅ Player A and their team members remain on the new slot

---

### Test #72: Visitor Teleportation to Spawn

**Prerequisites:**
- Two players: Player A (island owner) and Player C (visitor WITHOUT own island)
- Player A has at least 2 island slots with different islands

**Steps:**
1. Player C joins the server but does NOT create an island
2. Player C visits Player A's island (slot 1)
3. Player A switches from slot 1 to slot 2
4. Observe what happens to Player C

**Expected Results:**
- ✅ Player C sees warning message: "&eThe island owner is switching islands. You will be teleported in 2 seconds..."
- ✅ After 2 seconds, Player C is teleported to world spawn
- ✅ Player C sees message: "&eYou have been teleported to spawn as the island is switching slots."
- ✅ Player C is NOT on Player A's island anymore

---

### Test #73: Team Members NOT Teleported Away (Regression)

**Prerequisites:**
- Player A (island owner) with a team member (Player D)
- At least 2 island slots

**Steps:**
1. Player A invites Player D to their team
2. Player D accepts and is on the island (slot 1)
3. Player A switches from slot 1 to slot 2
4. Observe what happens to Player D

**Expected Results:**
- ✅ Player D does NOT see visitor warning message
- ✅ Player D is teleported WITH the team to the new island (slot 2)
- ✅ Player D sees message: "&eYou have been teleported to the new island slot."
- ✅ Player D is on the new slot 2 island, not at spawn

---

### Test #74: Multiple Visitors Handled

**Prerequisites:**
- Player A (island owner)
- Players E, F, G (all visitors, some with islands, some without)
- At least 2 island slots

**Steps:**
1. Players E (has island), F (has island), G (no island) all visit Player A's island
2. All three are standing on Player A's island
3. Player A switches slots
4. Observe what happens to all visitors

**Expected Results:**
- ✅ All visitors (E, F, G) see warning message
- ✅ Player E teleported to their own island
- ✅ Player F teleported to their own island
- ✅ Player G teleported to world spawn
- ✅ All visitors receive appropriate notification messages
- ✅ None of the visitors remain on Player A's island

---

### Test #75: Configuration - Teleport Delay

**Prerequisites:**
- Access to config.yml
- Player A (owner) and Player B (visitor)

**Steps:**
1. Edit `config.yml` and set `visitors.teleport-delay: 100` (5 seconds)
2. Reload the addon: `/islandselector admin reload`
3. Player B visits Player A's island
4. Player A switches slots
5. Time how long before Player B is teleported

**Expected Results:**
- ✅ Player B sees warning message immediately
- ✅ Player B is teleported after 5 seconds (100 ticks), not 2 seconds
- ✅ Config setting is respected

---

### Test #76: Configuration - Warning Message

**Prerequisites:**
- Access to config.yml
- Player A (owner) and Player B (visitor)

**Steps:**
1. Edit `config.yml` and set `visitors.warning-message: "&c&lWARNING! Teleporting soon!"`
2. Reload the addon: `/islandselector admin reload`
3. Player B visits Player A's island
4. Player A switches slots
5. Check Player B's chat

**Expected Results:**
- ✅ Player B sees custom message: "WARNING! Teleporting soon!" (in red, bold)
- ✅ Custom message is shown instead of default

---

### Test #77: Configuration - Disable Warning Message

**Prerequisites:**
- Access to config.yml
- Player A (owner) and Player B (visitor)

**Steps:**
1. Edit `config.yml` and set `visitors.warning-message: ""`
2. Reload the addon: `/islandselector admin reload`
3. Player B visits Player A's island
4. Player A switches slots
5. Check Player B's chat

**Expected Results:**
- ✅ Player B does NOT see warning message
- ✅ Player B is still teleported after the delay
- ✅ Player B only sees the post-teleport message

---

## Regression Testing

### ⚠️ CRITICAL: Verify Previous Features Still Work

Since we modified `SlotSwitchManager.java`, please verify:

1. **Basic Slot Switching (Tests #64-70 from Session 15)**
   - Island owner can switch between slots
   - Current island is saved as schematic
   - Target island is loaded from schematic
   - Owner is teleported to new island
   - Blocks are preserved correctly

2. **Team Member Teleportation**
   - Team members are teleported with the island
   - Team members receive notification
   - Team members remain on the team

3. **Progress Messages**
   - Owner sees progress messages during switch
   - "Starting slot switch..."
   - "Saving current island..."
   - "Clearing current island from world..."
   - "Loading target island..."
   - "Teleporting to new island..."
   - "Slot switch complete!"

---

## Configuration File

The visitor settings in `config.yml`:

```yaml
# ==========================================
# VISITORS
# ==========================================
visitors:
  # Delay before teleporting visitors (in ticks, 20 = 1 second)
  # Gives them time to read the notification
  teleport-delay: 40

  # Message shown to visitors before teleport
  # Set to empty string to disable
  warning-message: "&eThe island owner is switching islands. You will be teleported in 2 seconds..."
```

---

## Edge Cases to Test

### Edge Case 1: Visitor Logs Out During Delay
**Test:** Player switches slot, visitor logs out during 2-second delay
**Expected:** No errors in console, visitor not teleported (they're offline)

### Edge Case 2: Visitor Already at Destination
**Test:** Visitor is already at their own island when owner switches slots
**Expected:** Visitor still teleported (position updated), no errors

### Edge Case 3: No Visitors Present
**Test:** Owner switches slots with no visitors on the island
**Expected:** Switch completes normally, no errors, no unnecessary processing

### Edge Case 4: Visitor Joins During Switch
**Test:** Visitor joins the island while switch is in progress
**Expected:** Visitor is NOT teleported (they weren't there when switch started)

---

## Known Issues / Limitations

None identified yet - this is the first implementation. Human tester should watch for:
- Console errors during visitor teleportation
- Visitors getting stuck or not teleporting
- Team members accidentally being treated as visitors
- Teleportation destination issues (invalid locations)

---

## Commands for Testing

| Command | Description |
|---------|-------------|
| `/islandselector slots` | Open slot GUI |
| `/islandselector admin reload` | Reload config after changes |
| `/island team invite <player>` | Add team member (for regression test) |
| `/island team kick <player>` | Remove team member |
| `/island home` | Return to your island |
| `/spawn` | Go to world spawn |

---

## What to Report

### ✅ If Tests Pass:
- Confirm which test numbers passed (71-77)
- Note any observations about timing, messages, etc.

### ❌ If Tests Fail:
- Which specific test failed
- What was expected vs what happened
- Console errors (full stack trace if available)
- Steps to reproduce the issue

### 🐛 Bugs or Issues:
- Describe the issue in detail
- Console errors
- Which players were affected (owner, visitor, team member)
- Whether it's consistent or intermittent

---

## Technical Details

### Code Changes
- **File Modified:** `SlotSwitchManager.java`
- **New Methods:**
  - `teleportVisitorsAway(Island island, Player owner)` - Detects and initiates visitor teleportation
  - `handleVisitorTeleport(Player visitor, Island island)` - Handles individual visitor teleportation with delay
  - `getVisitorTeleportDestination(Player visitor)` - Determines where to teleport visitor

### Logic Flow
1. During slot switch, after team members are teleported
2. System iterates through all online players
3. Filters out owner and team members
4. Checks if player is on the island (`island.onIsland()`)
5. For each visitor found:
   - Sends warning message
   - Schedules delayed teleportation task
   - Determines destination (own island or spawn)
   - Teleports after delay
   - Sends confirmation message

### Configuration Integration
- Reads `visitors.teleport-delay` from config
- Reads `visitors.warning-message` from config
- Both settings can be reloaded with `/islandselector admin reload`

---

## Build Status

✅ **Compilation:** SUCCESS
✅ **JAR Size:** 120K
✅ **Source Files:** 26 (no new files, 1 modified)
✅ **No Errors:** Clean build

---

## Next Session Priorities

After testing confirms Tests #71-72 pass:

**Priority 1:** Island Creation in Empty Slots (Test #62 completion)
- Trigger BSkyBlock island creation for empty slot
- Ensure new island uses same grid coordinate

**Priority 2:** Slot Management Commands
- `/islandselector setname <slot> <name>`
- `/islandselector seticon <slot>`
- `/islandselector switch <slot>`
- `/islandselector delete <slot>`

**Priority 3:** Search Functionality (Tests #28-31)
- Implement player search in grid GUI
- Search button with compass item
- Partial name matching
- Error handling for not found

---

## Notes for Human Tester

🔥 **Important:** This feature requires testing with at least 2 players to fully verify. Consider:
- Using a second account
- Using a friend to help test
- Using a bot or NPC plugin (though real players are best)

📝 **Documentation:** Take screenshots or record video if possible - it helps see what actually happens vs what should happen.

⏱️ **Timing:** The default 2-second delay is intentional - it gives visitors time to read the warning. Feel free to test with different delays (config).

---

Thank you for testing! 🙏
