# Session 15 - Slot Switching with FAWE Implementation

## Build Information
- **Session:** 15 (FAWE Slot Switching)
- **Build Date:** December 2, 2025
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **JAR Size:** 118K (increased from 112K in Session 14)
- **Build Status:** ✅ SUCCESS
- **Source Files:** 26 (+1 new: SlotSwitchManager.java)

## What Was Implemented

### Core Feature: Slot Switching with FAWE
This session implemented the complete slot switching system using FastAsyncWorldEdit (FAWE) for schematic operations.

### New Files Created
1. **SlotSwitchManager.java** - Handles all FAWE operations for slot switching
   - Schematic save/load operations
   - Island block clearing
   - Player teleportation
   - Async processing for performance

### Modified Files
1. **IslandSelector.java** - Added SlotSwitchManager initialization and getter
2. **SlotSwitchConfirmationGUI.java** - Integrated with SlotSwitchManager to actually perform switches

### Key Implementation Details

#### Slot Switching Workflow
1. **Validation** - Check cooldowns and permissions
2. **Save Current Island** - Save active slot to schematic file using FAWE
3. **Clear World** - Remove all blocks from current island location
4. **Load Target Island** - Paste target slot schematic to world
5. **Update Database** - Update active slot markers in SlotData
6. **Teleport Players** - Move owner and team members to new island
7. **Notify Players** - Send progress messages throughout process

#### Technical Features
- **Async Processing** - All FAWE operations run asynchronously to avoid server lag
- **Progress Messages** - Players receive updates at each stage
- **Error Handling** - Graceful failure with error messages
- **Schematic Format** - Uses SPONGE_SCHEMATIC format for compatibility
- **File Organization** - Schematics stored in `plugins/BentoBox/addons/IslandSelector/slots/<playerUUID>/slot-<number>.schem`

#### Island Size Detection
- Uses cached island spacing from BSkyBlock config
- Calculates region based on protection range or spacing (whichever is larger)
- Copies full height (minHeight to maxHeight)

---

## Tests Ready for Manual Testing

### Tests #64-70: Slot Switching with FAWE

#### Test #64: Active island saved to schematic file on switch
**Status:** READY FOR TESTING
**Testing Steps:**
1. Create an island with some builds (house, farm, etc.)
2. Create a second slot and build something different
3. Switch from slot 1 to slot 2
4. Check server folder: `plugins/BentoBox/addons/IslandSelector/slots/<your-uuid>/slot-1.schem` exists
5. Verify file is not empty (should be several KB or more)

**Expected Result:** Schematic file is created and contains island data

---

#### Test #65: Target island loaded from schematic on switch
**Status:** READY FOR TESTING
**Testing Steps:**
1. Have two slots with different islands
2. Switch from slot 1 to slot 2
3. Verify slot 2's island appears in the world
4. Verify all blocks, chests, entities are present
5. Switch back to slot 1
6. Verify slot 1's island is restored correctly

**Expected Result:** Islands switch correctly between slots, all blocks preserved

---

#### Test #66: Old island cleared from world after switch
**Status:** READY FOR TESTING
**Testing Steps:**
1. Build a large island on slot 1 (house, trees, etc.)
2. Create slot 2 (empty or minimal build)
3. Switch to slot 2
4. Fly to where slot 1's island was
5. Verify all blocks are cleared (air/void)

**Expected Result:** No blocks remain from previous slot

---

#### Test #67: Player teleported to new island spawn after switch
**Status:** READY FOR TESTING
**Testing Steps:**
1. Stand on your slot 1 island
2. Switch to slot 2 (different island)
3. Verify you are immediately teleported to slot 2's spawn
4. Verify spawn point is correct

**Expected Result:** Instant teleport to new slot's spawn point

---

#### Test #68: Team members teleported with island switch
**Status:** READY FOR TESTING
**Testing Steps:**
1. Invite another player to your island team
2. Both players stand on slot 1 island
3. Owner switches to slot 2
4. Verify team member is also teleported to slot 2

**Expected Result:** All online team members teleported together

---

#### Test #69: Progress messages shown during switch operation
**Status:** READY FOR TESTING
**Testing Steps:**
1. Initiate a slot switch
2. Watch chat messages
3. Verify you see messages like:
   - "Starting slot switch..."
   - "Saving current island..."
   - "Clearing current island from world..."
   - "Loading target island..."
   - "Teleporting to new island..."
   - "Slot switch complete! Welcome to [slot name]"

**Expected Result:** Clear progress feedback throughout the process

---

#### Test #70: Slot switch fails gracefully with error message
**Status:** READY FOR TESTING
**Testing Steps:**
1. Try switching to a slot that doesn't have a schematic yet (empty slot with no island)
2. Verify error message appears
3. Verify current island remains intact
4. Verify database is not corrupted

**Expected Result:** Error message shown, no data lost

---

## Regression Testing

### Critical: Previous Features Must Still Work

After implementing slot switching, verify these existing features:

1. **Island Creation (Tests #41-48)** - Creating first island should still work
2. **Slot Selection GUI (Tests #54-61)** - GUI should display correctly
3. **Grid GUI** - Main grid GUI should still function
4. **Admin Commands** - All admin commands should work
5. **Database Persistence** - Slot data should persist across restarts

---

## Configuration Notes

### Relevant Config Settings (config.yml)

```yaml
slots:
  switch-cooldown: 3600    # 1 hour default
  default-slot-name: "Island {number}"

backups:
  on-switch: true          # Backup on every slot switch
  max-per-slot: 3          # Keep 3 backups per slot
```

### Permissions

- `islandselector.bypass.cooldown.switch` - Bypass switch cooldown
- `islandselector.bypass.cost.relocate` - Bypass relocation costs (future feature)
- `islandselector.slots.2` - Allow 2 slots
- `islandselector.slots.3` - Allow 3 slots
- `islandselector.slots.4` - Allow 4 slots
- `islandselector.slots.5` - Allow 5 slots (max)

---

## Known Limitations / Future Enhancements

1. **Visitor Handling** - Visitors are not yet teleported away when island switches
2. **Backup System** - Automatic backups not yet implemented (config option exists)
3. **Large Island Performance** - Very large islands may take several seconds to switch
4. **Action Bar Messages** - Currently using chat messages instead of action bar for compatibility

---

## Testing Checklist

Before marking tests as passing, verify:

- [ ] Test #64: Schematic file creation
- [ ] Test #65: Schematic loading
- [ ] Test #66: Island clearing
- [ ] Test #67: Player teleportation
- [ ] Test #68: Team member teleportation
- [ ] Test #69: Progress messages
- [ ] Test #70: Error handling

**Regression Tests:**
- [ ] Island creation still works
- [ ] Slot GUI still works
- [ ] Grid GUI still works
- [ ] Database persistence works
- [ ] No console errors on startup

---

## Common Issues / Troubleshooting

### Issue: "Failed to save schematic" error
**Cause:** FAWE not installed or permissions issue
**Fix:**
1. Install FastAsyncWorldEdit
2. Check server has write permissions to plugins folder

### Issue: Island blocks not clearing
**Cause:** WorldEdit region size mismatch
**Fix:** Check BSkyBlock island spacing configuration matches

### Issue: Teleport to wrong location
**Cause:** Island spawn point not set
**Fix:** Island will teleport to center if spawn not set (this is expected)

### Issue: Schematic file very large
**Cause:** Large protection range or island spacing
**Fix:** This is normal for large islands; consider reducing protection range

---

## File Locations

**Plugin JAR:**
```
output/IslandSelector-1.0.0-SNAPSHOT.jar
→ Copy to: plugins/BentoBox/addons/
```

**Schematic Storage:**
```
plugins/BentoBox/addons/IslandSelector/slots/<playerUUID>/slot-<number>.schem
```

**Database Files:**
```
plugins/BentoBox/database/IslandSelector/
  - GridLocationData/
  - SlotData/
```

---

## Next Session Priorities

After human testing of slot switching:

1. **Visitor Teleportation** - Teleport visitors away from island during switch
2. **Backup System** - Implement automatic backups before switching
3. **Island Creation in Empty Slots (Test #62)** - Allow creating new islands in empty slots
4. **Slot Management Commands** - /islandselector setname, seticon, switch, delete
5. **Performance Optimization** - Investigate faster clearing/pasting methods

---

## Session 15 Complete

**Status:** ✅ READY FOR MANUAL TESTING

**Summary:** Implemented complete FAWE-based slot switching system with schematic save/load, island clearing, and player teleportation.

**Next:** Human tester should verify all 7 tests (#64-70) and regression tests on a Minecraft server with FAWE installed.
