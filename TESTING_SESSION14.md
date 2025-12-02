# IslandSelector - Session 14 Testing Instructions

## Build Information
- **Session:** 14
- **Date:** Session 14 (Slot System Foundation & Confirmation GUIs)
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **JAR Size:** 112K (increased from 104K)
- **Build Status:** ✅ SUCCESS

## Session 14 Summary

### What Was Implemented

This session focused on implementing the **foundational slot system infrastructure** and **slot switching confirmation GUI**:

1. **SlotData Database Model (NEW)**
   - Complete database model for storing player slot information
   - Tracks slot number, custom name, custom icon, island UUID, active status
   - Cooldown tracking for slot switches
   - Grid coordinate tracking (all slots share same location)

2. **SlotManager (NEW)**
   - Complete manager for all slot operations
   - Create/get slots, track active slots
   - Initialize player slots on first island creation
   - Cooldown checking and bypass permission support
   - Database persistence and caching

3. **Slot Selection GUI - Updated**
   - Now uses real SlotManager data instead of hardcoded demo data
   - Displays actual slot information from database
   - Shows custom slot names and icons
   - Proper handling of active/inactive/empty/locked slots

4. **Slot Switch Confirmation GUI (NEW)**
   - Shows which slots are being switched (from → to)
   - Displays cooldown warnings if applicable
   - Confirm/Cancel buttons
   - Time formatting for cooldown display
   - Permission bypass support

5. **Empty Slot Handling**
   - Clicking empty slot shows appropriate message
   - Prevents creating islands in empty slots before first island
   - Lays foundation for future island creation in empty slots

6. **Integration with Island Creation**
   - IslandCreateListener now initializes slot 1 automatically
   - First island is always placed in slot 1 and marked active
   - Slot data persisted to database on island creation

### Tests Ready for Manual Verification

#### Foundation Tests (Tests #54-61) - PREVIOUSLY IMPLEMENTED, NOW FULLY FUNCTIONAL

These tests were UI-only in Session 13. Now they use real data from SlotManager:

✅ **Test #54:** Slot Selection GUI opens with correct layout
✅ **Test #55:** Active slot displays with enchantment glow and ACTIVE indicator
✅ **Test #56:** Inactive slot with island shows custom icon and switch prompt
✅ **Test #57:** Empty unlocked slot displays with lime dye and create prompt
✅ **Test #58:** Locked slot displays with barrier and unlock requirement
✅ **Test #59:** Player with islandselector.slots.2 permission has access to 2 slots
✅ **Test #60:** Player with islandselector.slots.5 permission has access to 5 slots
✅ **Test #61:** Clicking locked slot shows error message about requirements

#### New Tests (Tests #62-63) - NEWLY IMPLEMENTED

✅ **Test #62:** Clicking empty unlocked slot starts island creation flow (PARTIAL)
- **Status:** Foundation implemented, but full island creation not yet functional
- **What Works:** Empty slot click detection, error messages
- **What's Coming:** Actual BSkyBlock island creation in empty slots

✅ **Test #63:** Clicking inactive slot opens switch confirmation GUI (COMPLETE)
- **Status:** Fully implemented and ready for testing
- **What Works:** Switch confirmation GUI, cooldown warnings, permission checks

---

## Detailed Testing Steps

### Prerequisites
1. Fresh Minecraft server with BentoBox, BSkyBlock, and FAWE installed
2. IslandSelector addon installed in `plugins/BentoBox/addons/`
3. Server fully started and addons loaded

### Test #62: Empty Slot Island Creation (Partial)

**Setup:**
```bash
/lp user <player> permission set islandselector.slots.2 true
```

**Test Steps:**
1. Join as a player with 2 slots permission
2. Create your first island using `/island create`
3. Select a grid location and complete island creation
4. After creation, run `/islandselector slots`
5. Verify slot 1 shows as active with glow
6. Verify slot 2 shows as LIME_DYE (empty slot)
7. Click on empty slot 2
8. Verify message: "&eIsland creation in empty slots coming soon..."

**Expected Result:**
- Empty slot detected correctly
- Appropriate message shown
- No errors in console
- GUI remains stable

**Note:** Full island creation in empty slots will be implemented in future sessions.

### Test #63: Slot Switch Confirmation GUI

**Setup:**
```bash
# This test requires having 2 islands in different slots
# For now, this can only be tested once multi-slot creation works
# OR manually via database manipulation
```

**Simulated Test (for development):**
1. Create island in slot 1
2. Manually create SlotData entry for slot 2 in database
3. Open `/islandselector slots`
4. Click on slot 2
5. Verify Switch Confirmation GUI opens
6. Verify GUI shows "From: Island 1" and "To: Island 2"
7. Verify Confirm and Cancel buttons present
8. Click Cancel
9. Verify returns to slot selection GUI

**Expected Result:**
- Confirmation GUI displays correctly
- Slot names shown properly
- Buttons functional
- Cancel returns to slot selection

**Note:** Full testing requires Test #62 to be complete.

### Test #54-61: Slot GUI with Real Data

**Setup:**
```bash
/lp user <player> permission set islandselector.slots.3 true
```

**Test Steps:**
1. Create an island using `/island create`
2. Select grid location and complete creation
3. Run `/islandselector slots`
4. Verify GUI opens (Test #54)
5. Verify slot 1 shows with glow effect (Test #55)
6. Verify slot 1 shows "★ ACTIVE" in lore (Test #55)
7. Verify slot 1 name is green and bold (Test #55)
8. Verify slot 2-3 show as LIME_DYE (empty, unlocked) (Test #57)
9. Verify slots 4-5 show as BARRIER (locked) (Test #58)
10. Click on locked slot 5 (Test #61)
11. Verify error: "&cThis slot is locked!"
12. Verify second message: "&7You need permission: &fislandselector.slots.5"

**Expected Result:**
- All slot states display correctly
- Active slot has glow and correct formatting
- Empty slots show lime dye
- Locked slots show barrier with permission requirement
- Error messages clear and helpful

---

## Testing Checklist

### Session 14 New Features
- [ ] SlotData database integration works
- [ ] SlotManager initializes on island creation
- [ ] Slot 1 created as active for new players
- [ ] Slot selection GUI shows real data
- [ ] Empty slot click shows appropriate message
- [ ] Switch confirmation GUI opens correctly
- [ ] Cooldown display works
- [ ] Permission bypass for cooldowns works

### Regression Testing
- [ ] Grid GUI still opens correctly
- [ ] Island creation at selected coordinates (Test #43)
- [ ] Search functionality (Tests #36-39)
- [ ] Premium location tooltips (Test #40)
- [ ] Location validation (Tests #49-53)

---

## Known Limitations

### Not Yet Implemented (Expected)
1. **Actual Slot Switching** - Clicking Confirm shows "coming soon" message
   - FAWE schematic save/load not yet implemented
   - Visitor teleportation not yet implemented
   - This is planned for Session 15

2. **Island Creation in Empty Slots** - Shows "coming soon" message
   - BSkyBlock integration for multi-slot creation not yet implemented
   - This is planned for Session 15

3. **Custom Slot Names/Icons** - Database structure exists but commands not implemented
   - `/islandselector setname` command not yet added
   - `/islandselector seticon` command not yet added
   - This is planned for future sessions

4. **Slot Deletion** - Not yet implemented
   - Database structure supports it
   - Confirmation GUI and logic coming in future sessions

---

## Database Verification

### Check SlotData in Database

If using YAML database (default):
```bash
# Check if slot data was created
cat plugins/BentoBox/database/PlayerSlots/<player-uuid>-1.yml
```

You should see:
```yaml
uniqueId: "<uuid>-1"
playerUUID: "<uuid>"
slotNumber: 1
slotName: "Island 1"
iconMaterial: "GRASS_BLOCK"
islandUUID: "<island-uuid>"
gridCoordinate: "0,0"
hasIsland: true
active: true
lastSwitchTime: 0
```

---

## Commands for Testing

### Player Commands
```bash
# Open grid GUI
/islandselector

# Open slot selection
/islandselector slots

# Create new island (triggers grid selection for new players)
/island create
```

### Permission Setup
```bash
# Grant 2 slots
/lp user <player> permission set islandselector.slots.2 true

# Grant 3 slots
/lp user <player> permission set islandselector.slots.3 true

# Grant 5 slots
/lp user <player> permission set islandselector.slots.5 true

# Grant cooldown bypass
/lp user <player> permission set islandselector.bypass.cooldown.switch true
```

### Admin Commands
```bash
# Check plugin version
/islandselector admin version

# Check grid location info
/islandselector admin info 0,0

# Reload config
/islandselector admin reload
```

---

## Configuration Options (New)

### Slot System Config
Located in `plugins/IslandSelector/config.yml`:

```yaml
slots:
  default-slots: 2             # Default number of slots per player
  max-slots: 5                 # Maximum slots available
  switch-cooldown: 3600        # Cooldown in seconds (1 hour default)
  default-slot-name: "Island {number}"  # Default naming pattern
  default-slot-icon: "GRASS_BLOCK"      # Default icon material
```

### Important Settings
- `switch-cooldown: 3600` - 1 hour cooldown between switches (can be set to 0 for testing)
- `default-slots: 2` - Players start with 2 slots available
- `max-slots: 5` - System supports up to 5 slots per player

---

## Session 14 Changes Summary

### New Files Created
1. **SlotData.java** (database/SlotData.java)
   - Complete database model for player slots
   - All necessary fields and methods
   - BentoBox database integration

2. **SlotManager.java** (managers/SlotManager.java)
   - Manager for all slot operations
   - Database caching and persistence
   - Cooldown tracking and bypass support

3. **SlotSwitchConfirmationGUI.java** (gui/SlotSwitchConfirmationGUI.java)
   - Confirmation dialog for slot switches
   - Cooldown warnings
   - Confirm/Cancel functionality

4. **TESTING_SESSION14.md** (this file)
   - Comprehensive testing instructions

### Files Modified
1. **IslandSelector.java**
   - Added SlotManager initialization
   - Added getter for SlotManager

2. **SlotSelectionGUI.java**
   - Updated to use real SlotManager data
   - Implemented slot click handling
   - Switch confirmation integration
   - Empty slot detection

3. **IslandCreateListener.java**
   - Added slot initialization on island creation
   - Slot 1 auto-created as active
   - SlotManager integration

---

## Next Steps for Future Sessions

### Priority 1: Slot Switching Implementation (Session 15)
- [ ] Implement FAWE schematic save for active island
- [ ] Implement schematic load for target slot
- [ ] Clear active slot's blocks from world
- [ ] Teleport owner and team members to new slot
- [ ] Teleport visitors away from island
- [ ] Handle edge cases and errors

### Priority 2: Island Creation in Empty Slots
- [ ] Trigger BSkyBlock island creation for empty slots
- [ ] Ensure island uses same grid coordinate
- [ ] Associate new island with correct slot number
- [ ] Handle blueprint selection for additional islands

### Priority 3: Slot Management Commands
- [ ] `/islandselector setname <slot> <name>` command
- [ ] `/islandselector seticon <slot>` command (uses held item)
- [ ] `/islandselector delete <slot>` command with confirmation
- [ ] `/islandselector switch <slot>` command

### Priority 4: Advanced Features
- [ ] Island level integration with Level addon
- [ ] Team member count display
- [ ] Slot settings GUI (rename, change icon, delete buttons)
- [ ] Backup system for schematics
- [ ] Cooldown bypass permissions testing

---

## Build Status

```
✅ Compilation: SUCCESS
✅ 25 source files compiled (+3 new files)
✅ JAR size: 112K (up from 104K)
✅ No errors or warnings
✅ All dependencies resolved
```

---

## Summary

**Session 14 Status:** ✅ COMPLETE

**What's New:**
- Complete slot system database foundation
- SlotManager for all slot operations
- Switch confirmation GUI
- Slot selection GUI now uses real data
- Auto-initialization of slot 1 on island creation

**Tests Implemented:**
- Test #62: Empty slot handling (foundation)
- Test #63: Switch confirmation GUI (complete)
- Tests #54-61: Now use real data (improved)

**Build:** ✅ SUCCESS (112K JAR)

**What's Working:**
- Slot database persistence
- Slot GUI with real data
- Permission-based slot unlocking
- Cooldown tracking
- Switch confirmation GUI

**What's Coming Next:**
- Actual slot switching with FAWE (Test #64-67)
- Island creation in empty slots (Test #62 completion)
- Visitor teleportation during switches
- Slot management commands

**Next Session:** Implement FAWE-based slot switching (save/clear/load operations)

---

## Important Notes for Human Tester

1. **Database Initialization:** The first time you create an island, check the database to verify SlotData was created correctly.

2. **Testing Multi-Slot Functionality:** Since slot switching and empty slot creation aren't fully implemented yet, some tests can only be partially verified. Full testing will be possible in Session 15.

3. **Console Output:** Watch the console for log messages like "Initialized slot 1 for player <name>" to verify the system is working.

4. **Cooldown Testing:** Set `switch-cooldown: 0` in config for easier testing, or use the bypass permission.

5. **Database Location:** Default YAML database is in `plugins/BentoBox/database/PlayerSlots/`

---

*End of Session 14 Testing Documentation*
