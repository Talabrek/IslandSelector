# IslandSelector - Manual Testing Instructions

## Build Information
- **Build Date:** Session 47 (Latest - Custom Event API)
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **JAR Size:** 200K
- **Status:** ✅ 44 tests verified, 98 tests pending manual verification

## Latest Sessions (38-47)

### Session 47: Custom Event API ⭐ NEW
📋 **See [SESSION47_FOR_HUMAN_TESTER.md](SESSION47_FOR_HUMAN_TESTER.md) for quick test guide**

**Features Ready:** 8 custom event API tests
- GridLocationClaimEvent - Fires when player claims a location
- SlotSwitchEvent - Fires when player switches slots
- IslandRelocateEvent - Fires when player relocates island
- All events are cancellable with custom messages
- Allows other plugins to hook into IslandSelector operations
- Events fire BEFORE operations begin

**Testing Time:** 20-30 minutes
**Players Required:** 2-3 accounts
**Special Requirements:** Test plugin (template provided in test guide)
**Complexity:** Advanced (requires basic Java/plugin development)

### Session 46: PlaceholderAPI Integration
📋 **See [SESSION46_FOR_HUMAN_TESTER.md](SESSION46_FOR_HUMAN_TESTER.md) for quick test guide**

**Features Ready:** 8 PlaceholderAPI tests
- %islandselector_location% - Player's grid coordinates
- %islandselector_slot_active_name% - Active slot name
- %islandselector_cooldown_switch% - Slot switch cooldown (formatted)
- %islandselector_available% - Count of available locations
- %islandselector_neighbors_online% - Count of online neighbors
- Integration with chat/scoreboard/menu plugins
- Graceful handling when PlaceholderAPI not installed

**Testing Time:** 15-20 minutes
**Players Required:** 2-3 accounts (for neighbor testing)
**Special Requirements:** PlaceholderAPI plugin (optional but needed for testing)

### Session 45: Command Tab Completion
📋 **See [SESSION45_FOR_HUMAN_TESTER.md](SESSION45_FOR_HUMAN_TESTER.md) for quick test guide**

**Features Ready:** 4 tab completion tests
- `/islandselector <TAB>` shows available subcommands
- `/islandselector admin <TAB>` shows admin subcommands
- Permission-based filtering (only shows commands you can use)
- Partial matching support (type "l" + TAB → "locate")

**Testing Time:** 5-10 minutes
**Players Required:** 1 account
**Special Requirements:** None

### Session 44: Neighborhood GUI Enhancements

**Features Ready:** 11 neighborhood GUI tests (137-147)
- Complete 3x3 neighborhood layout with proper button positioning
- Slots button added (opens Slot Selection GUI)
- Back button repositioned (returns to main grid)
- Close button repositioned
- Online/offline neighbor displays
- Empty location displays
- Grid edge handling
- Neighbor warp functionality (right-click)

**Testing Time:** 15-20 minutes
**Players Required:** 2-3 accounts (for neighbor testing)
**Special Requirements:** None

### Session 43: Player Information Commands
📋 **See [SESSION43_FOR_HUMAN_TESTER.md](SESSION43_FOR_HUMAN_TESTER.md) for quick test guide**

**Features Ready:** 8 player command tests
- `/islandselector locate` - Shows own island coordinates in chat
- `/islandselector find <player>` - Opens grid to find player island
- `/islandselector help` - Shows command list with descriptions
- Tab completion for find command
- Permission-based command filtering
- Error handling for invalid inputs

**Testing Time:** 10-15 minutes
**Players Required:** 2 accounts
**Special Requirements:** None

### Session 42: Neighbor Warp/Visit System
📋 **See [SESSION42_FOR_HUMAN_TESTER.md](SESSION42_FOR_HUMAN_TESTER.md) for quick test guide**

**Features Ready:** 4 neighbor warp tests
- Right-click neighbor in Neighborhood GUI to warp
- Warps to neighbor's island center/spawn
- Error handling for empty slots
- Error handling for reserved locations
- Left-click vs right-click behavior

**Testing Time:** 5-10 minutes
**Players Required:** 2 accounts (adjacent islands)
**Special Requirements:** None

### Session 41: Island Relocation System ⭐ NEW
📋 **See [SESSION41_FOR_HUMAN_TESTER.md](SESSION41_FOR_HUMAN_TESTER.md) for quick test guide**

**Features Ready:** 26 relocation-related tests
- Basic relocation flow (click empty location with existing island)
- Confirmation GUI shows from/to coordinates and cost
- FAWE-powered island copying (blocks + entities)
- Old location cleared completely
- BSkyBlock data updates (spawn, warp, protection)
- Player teleportation (owner, team, visitors)
- Economy integration (Vault cost, bypass permission)
- Cooldown system (enforcement, persistence, bypass)
- Progress indicators during relocation
- Edge cases (occupied, reserved, premium locations)

**Testing Time:** 40-60 minutes (comprehensive), 10-15 minutes (quick)
**Players Required:** 2-3 accounts (for team/visitor tests)
**Special Requirements:** FastAsyncWorldEdit REQUIRED, Vault optional

### Session 40: Slot Selection System
📋 **See [SESSION40_FOR_HUMAN_TESTER.md](SESSION40_FOR_HUMAN_TESTER.md) for quick test guide**

**Features Ready:** 18 slot-related tests
- Slot selection GUI layout
- Active/inactive/empty/locked slot displays
- Permission-based slot unlocking
- Slot switching with confirmation
- Save/clear/load island schematics
- Team member teleportation

**Testing Time:** 20-30 minutes
**Players Required:** 2 accounts (for team tests)

### Session 39: Player Search Functionality
📋 **See [SESSION39_FOR_HUMAN_TESTER.md](SESSION39_FOR_HUMAN_TESTER.md) for quick test guide**

**Features Ready:** 4 search-related tests
- Search button displays (COMPASS item)
- Search finds player by name
- Partial name matching ("Not" finds "Notch")
- Player not found error handling

**Testing Time:** 10-15 minutes
**Players Required:** 2-3 accounts

### Session 38: Island Claiming & Premium Locations
📋 **See [SESSION38_FOR_HUMAN_TESTER.md](SESSION38_FOR_HUMAN_TESTER.md) for quick test guide**

**Features Ready:** 7 island claiming tests
- New player intercepts /island command (CRITICAL TEST!)
- Click-to-select, double-click-to-confirm flow
- Confirmation GUI with Confirm/Cancel buttons
- Island created at selected grid coordinates
- Premium location tooltips (gold block with price)
- Grid coordinate mapping verification

**Testing Time:** 15-20 minutes
**Players Required:** 3-4 fresh accounts (can only claim once per account)

### Combined Sessions 38-44
**Total Tests Pending:** 78 tests (7 + 4 + 18 + 26 + 4 + 8 + 11)
**Combined Testing Time:** 120-165 minutes (comprehensive)
**If All Pass:** Progress jumps from 44/225 to 122/225 tests (54% complete - over halfway!)

---

## Quick Test Commands

In-game commands to test:
```
/islandselector          - Open grid GUI
/islandselector slots    - Open slot selection
/islandselector help     - Show all commands
```

Admin commands:
```
/islandselector admin version  - Check plugin version
/islandselector admin info A1  - Check location info
/islandselector admin reload   - Reload configuration
```

---

## Installation

1. Copy `output/IslandSelector-1.0.0-SNAPSHOT.jar` to `plugins/BentoBox/addons/`
2. Ensure dependencies are installed:
   - ✅ **BentoBox** (required)
   - ✅ **BSkyBlock** (required)
   - ✅ **FastAsyncWorldEdit** (required for slots & relocation)
   - 🔷 **Vault** (optional - for economy features)
   - 🔷 **Economy Plugin** (optional - e.g., EssentialsX)
3. Restart server
4. Verify no errors in console

---

## Testing Priority

**High Priority (Core Features):**
1. Session 38 - Island Claiming (7 tests) - Critical gameplay flow
2. Session 41 - Island Relocation (26 tests) - Major feature

**Medium Priority:**
3. Session 40 - Slot System (18 tests) - Advanced feature
4. Session 42 - Neighbor Warp (4 tests) - Quality of life
5. Session 39 - Player Search (4 tests) - Quality of life

---

## Previous Sessions

📋 **See [TESTING_SESSION37.md](TESTING_SESSION37.md)** - Session 37 search documentation
📋 **See [TESTING_SESSION31.md](TESTING_SESSION31.md)** - Session 31 slot settings GUI
📋 **See [TESTING_SESSION30.md](TESTING_SESSION30.md)** - Session 30 slot deletion
📋 **See [TESTING_SESSION29.md](TESTING_SESSION29.md)** - Session 29 custom slot icons
📋 **See [TESTING_SESSION28.md](TESTING_SESSION28.md)** - Session 28 slot renaming
📋 **See [TESTING_SESSION27.md](TESTING_SESSION27.md)** - Session 27 admin reset cooldown
📋 **See [TESTING_SESSION26.md](TESTING_SESSION26.md)** - Session 26 admin player lookups
📋 **See [TESTING_SESSION25.md](TESTING_SESSION25.md)** - Session 25 player search discovery

---

## Test Results Summary

| Test Range | Status | Notes |
|------------|--------|-------|
| Tests 1-44 | ✅ PASS | Core GUI, admin commands, persistence verified |
| Tests 45-51 | ⏳ PENDING | Island claiming (Session 38) |
| Tests 52-55 | ⏳ PENDING | Player search (Session 39) |
| Tests 56-73 | ⏳ PENDING | Slot selection (Session 40) |
| Tests 74-99 | ⏳ PENDING | Island relocation (Session 41) |
| Tests 133-136 | ⏳ PENDING | Neighbor warp (Session 42) |
| Tests 137-147 | ⏳ PENDING | Neighborhood GUI (Session 44) ⭐ NEW |
| Tests 100-132, 148+ | ❌ FAIL | Not yet tested |

**Current Status:** 44/225 tests verified, 78 tests pending manual verification

---

## Features Verified

### ✅ Core Features (Tests 1-44)
- Grid-based GUI with 7x4 island display
- BSkyBlock integration and auto-sync
- Database persistence across restarts
- Admin commands (reserve, setprice, info, reload, version)
- Navigation (scroll arrows, shift-click fast scroll)
- Filters (All, Available, Online)
- Find My Island button
- Configurable GUI items
- Player head displays with online status
- Reserved locations (blocked and premium)
- Grid coordinate system
- Configuration management
- Permission system

### ⏳ Pending Verification (Tests 45-99)

**Island Claiming (Session 38):**
- Intercept /island command for new players
- Click-to-select location in grid GUI
- Confirmation GUI with location details
- Island creation at selected coordinates
- Premium location purchase
- Neighbor information display

**Player Search (Session 39):**
- Search button in GUI (compass)
- Search by player name
- Partial name matching
- Player not found handling

**Slot System (Session 40):**
- Slot selection GUI (27 slots)
- Active/inactive/empty/locked slot displays
- Permission-based slot unlocking (slots.2, slots.3, etc.)
- Slot switching confirmation
- FAWE schematic save/load
- Team member teleportation on switch
- Slot management (rename, icon, delete)
- Right-click slot settings menu

**Island Relocation (Session 41):**
- Click empty location to relocate
- Relocation confirmation GUI
- FAWE-powered island copying
- Old location clearing
- BSkyBlock data updates
- Owner and team teleportation
- Visitor removal
- Economy integration (cost, bypass)
- Cooldown system (enforcement, persistence, bypass)
- Progress indicators
- Edge case handling

---

## Known Issues

⚠️ **Empty Slot Island Creation**
- May show "coming soon" message
- Direct creation from empty slot might not be fully integrated
- Workaround: Create islands via /island command first
- Status: Acceptable for current build

⚠️ **Large Island Performance**
- Slot switching can take 5-15 seconds for large islands
- Relocation can take 10-30 seconds for complex builds
- FAWE operations are intensive but async (no server lag)
- Status: Expected behavior, not a bug

⚠️ **FAWE Dependency**
- Slot switching and relocation REQUIRE FAWE
- Should show clear error if missing
- Status: Human tester should verify error handling

⚠️ **Entity Copying Limitations**
- Some entity types may not copy perfectly (WorldEdit limitation)
- Item frames, armor stands usually work
- Complex entities (villagers with trades) may have issues
- Status: Expected WorldEdit behavior

---

## Contact & Support

For issues, bugs, or questions:
1. Check console for error messages
2. Verify all dependencies installed
3. Test with minimal setup first
4. Report detailed reproduction steps

---

**Happy testing!** 🎮

**Next Step:** Copy JAR to test server and follow SESSION41_FOR_HUMAN_TESTER.md (or start with Session 38)
