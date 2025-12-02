# Session 52 - Testing Guide for Human Tester

## Status Overview

**Build Status:** BUILD SUCCESS
**JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
**JAR Size:** 216K
**Source Files:** 53 Java files

**Tests Status:**
- Verified Passing: 44 tests
- Pending Human Verification: 137+ tests (Sessions 38-51)
- Total: 225 tests

## CRITICAL: What Needs Testing

The plugin has many features implemented and ready for testing. The human tester needs to verify these features work correctly on a real Minecraft server.

### Priority 1: Core Features (Sessions 38-41)

#### Session 38: Island Claiming (CRITICAL!)
**Tests 45-51 in feature_list.json**

1. **New player intercept** - When a new player runs `/island`, they should see the grid GUI instead of auto-creating an island
2. **Click to select** - Clicking an empty green location should open confirmation
3. **Confirmation GUI** - Shows coordinates, neighbors, Confirm/Cancel buttons
4. **Island creation** - After confirming, island is created at selected coordinates
5. **Premium locations** - Gold blocks show price, require payment

**Testing Time:** 15-20 minutes
**Players Needed:** 3-4 fresh accounts (can only claim once per account)

#### Session 39: Player Search (4 tests)
**Tests around 305-349**

1. Click compass icon in bottom row of grid GUI
2. Type player name in chat
3. Verify partial name matching works ("Not" finds "Notch")
4. Verify grid centers on found island
5. Verify error message for players not found

**Testing Time:** 10-15 minutes

#### Session 40: Slot Selection (18 tests)
**Tests 52-73**

1. `/islandselector slots` opens 27-slot GUI
2. Active slot has green glow
3. Empty slots show dark glass
4. Locked slots show red glass
5. Permission `islandselector.slots.N` unlocks slot N
6. Clicking inactive slot shows switch confirmation
7. Right-click opens slot settings

**Testing Time:** 20-30 minutes

#### Session 41: Island Relocation (26 tests)
**Tests 74-99**

1. Player with existing island clicks empty location
2. Confirmation shows from/to coordinates and cost
3. FAWE copies island blocks and entities
4. Old location is cleared
5. Player is teleported to new location
6. Team members are teleported
7. Cooldown is enforced (configurable)

**Testing Time:** 40-60 minutes
**Requires:** FastAsyncWorldEdit (FAWE)

### Priority 2: Quality of Life (Sessions 42-51)

#### Session 42: Neighbor Warp (4 tests)
- Right-click neighbor in Neighborhood GUI to warp to their island

#### Session 43: Player Commands (8 tests)
- `/islandselector locate` - Shows your coordinates
- `/islandselector find <player>` - Opens grid centered on player
- `/islandselector help` - Shows command list

#### Session 44: Neighborhood GUI (11 tests)
- 3x3 layout showing your island and 8 neighbors
- Online neighbors show player heads
- Offline neighbors show red glass
- Empty locations show green glass

#### Session 45: Tab Completion (4 tests)
- Tab completion for all commands
- Permission-based filtering

#### Session 46: PlaceholderAPI (8 tests)
- `%islandselector_location%` - Grid coordinates
- `%islandselector_slot_active_name%` - Active slot name
- `%islandselector_cooldown_switch%` - Cooldown time
- **Requires:** PlaceholderAPI plugin

#### Session 47: Custom Events (8 tests)
- `GridLocationClaimEvent` - Fires when claiming
- `SlotSwitchEvent` - Fires when switching
- `IslandRelocateEvent` - Fires when relocating
- All events can be cancelled
- **Requires:** Test plugin (advanced testing)

#### Session 48: Admin Purge (10 tests)
- `/islandselector admin purge <player>` - Purge specific player
- `/islandselector admin purge inactive <days>` - Purge inactive players
- `/islandselector admin purge banned` - Purge banned players
- `--dry-run` flag shows preview
- Two-step confirmation for safety

#### Session 49: Admin Force Relocate (10 tests)
- `/islandselector admin relocate <player> <coordinate>`
- Bypasses costs and cooldowns
- Admin can move any player's island

#### Session 50: Search Documentation (4 tests)
- Already covered in Session 39

#### Session 51: Neighbors Command (6 tests)
- `/islandselector neighbors` opens Neighborhood GUI directly
- Aliases: `neighbours`, `neighbourhood`, `neighborhood`

## Quick Test Commands

### Player Commands
```
/islandselector          - Open main grid GUI
/islandselector slots    - Open slot selection GUI
/islandselector neighbors - Open neighborhood GUI
/islandselector locate   - Show your island coordinates
/islandselector find <player> - Find player's island
/islandselector help     - Show all commands
/is                      - Alias for /islandselector
```

### Admin Commands
```
/islandselector admin version   - Show version info
/islandselector admin info 0,0  - Show location info
/islandselector admin info <player> - Show player's slot info
/islandselector admin reload    - Reload configuration
/islandselector admin reserve 0,0 - Reserve location (blocked)
/islandselector admin setprice 0,0 1000 - Make premium location
/islandselector admin unreserve 0,0 - Unreserve location
/islandselector admin resetcooldown <player> switch - Clear switch cooldown
/islandselector admin purge <player> - Purge player slot data
/islandselector admin relocate <player> <coord> - Force relocate
```

## Installation

1. Copy `output/IslandSelector-1.0.0-SNAPSHOT.jar` to `plugins/BentoBox/addons/`
2. **Required Dependencies:**
   - BentoBox (core)
   - BSkyBlock (game mode)
   - FastAsyncWorldEdit (for slots and relocation)
3. **Optional Dependencies:**
   - Vault + economy plugin (for costs)
   - PlaceholderAPI (for placeholders)
   - Level addon (for island levels)
4. Restart server
5. Check console for successful addon loading

## Test Result Reporting

After testing, please report:

1. **PASS** - Feature works as expected
2. **FAIL** - Feature doesn't work (provide details)
3. **PARTIAL** - Partially working (describe what works/doesn't)

For each test:
- Test number from feature_list.json
- Pass/Fail status
- Any error messages or unexpected behavior
- Console errors (if any)

## Estimated Testing Time

| Priority | Tests | Time |
|----------|-------|------|
| Priority 1 (Core) | 55 tests | 90-120 minutes |
| Priority 2 (QoL) | 82 tests | 60-90 minutes |
| **Total** | **137 tests** | **2.5-3.5 hours** |

## Notes

- Tests can be spread across multiple testing sessions
- Start with Priority 1 (Core Features) for maximum impact
- Session 38 (Island Claiming) is the most critical to test
- FAWE is required for slot and relocation features
- Fresh player accounts needed for claiming tests (one-time operation)

---

**Happy Testing!**

If all tests pass, we'll be at approximately 181/225 tests (80% complete)!
