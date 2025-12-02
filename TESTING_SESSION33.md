# Testing Guide - Session 33

## Features Implemented in This Session

### Test #114: Admin Backup Command

**Implementation Status:** ✅ Complete, ready for testing

**What Was Implemented:**
- New `/islandselector admin backup <player> <slot>` command
- BackupManager class for creating island schematics
- Automatic backup retention (deletes old backups based on config)
- Async backup creation (doesn't lag server)
- Tab completion for player names and slot numbers

**Files Created/Modified:**
1. `BackupManager.java` - New manager for backup operations
2. `AdminBackupCommand.java` - New admin command
3. `AdminCommand.java` - Registered new backup command
4. `IslandSelector.java` - Added BackupManager initialization

---

## Test #114: Admin Command to Force Backup Works Correctly

### Prerequisites
- Minecraft server running with BentoBox + BSkyBlock + FAWE
- IslandSelector addon installed
- At least one player with an island in a slot
- Op or admin permissions

### Testing Steps

#### Step 1: Execute Backup Command
```
/islandselector admin backup <PlayerName> 1
```

**What to check:**
- Command executes without errors
- No console errors appear
- Message appears: "Creating backup for..."

#### Step 2: Verify Success Message
After a few seconds, you should see:
```
Successfully created backup for [PlayerName] slot 1
Total backups for slot: 1
```

#### Step 3: Check Backup Directory
Navigate to:
```
plugins/IslandSelector/backups/{player-uuid}/
```

**What to check:**
- Directory exists for the player
- File exists: `slot-1-{timestamp}.schem`
- File size is greater than 0 bytes
- Timestamp format is `YYYYMMDD-HHMMSS`

#### Step 4: Verify Backup File Contents
The `.schem` file should:
- Be a valid WorldEdit schematic file
- Contain the player's island blocks
- Include entities (armor stands, item frames, etc.)

---

## Additional Tests

### Test 2: Multiple Backups for Same Slot

**Steps:**
1. Run `/islandselector admin backup <player> 1` three times
2. Check the backup directory
3. Verify three backup files exist with different timestamps
4. Verify message shows "Total backups for slot: 3"

**Expected Result:** Each backup creates a new file ✅

---

### Test 3: Backup Retention Limit

**Prerequisites:**
- Set `backups.max-per-slot: 3` in config.yml
- Reload or restart server

**Steps:**
1. Create 5 backups for the same slot
   ```
   /islandselector admin backup <player> 1
   /islandselector admin backup <player> 1
   /islandselector admin backup <player> 1
   /islandselector admin backup <player> 1
   /islandselector admin backup <player> 1
   ```
2. Check backup directory after each command
3. Verify only 3 most recent backups remain
4. Verify oldest backups were deleted

**Expected Result:** Only 3 backup files remain (oldest deleted) ✅

---

### Test 4: Backup Different Slots

**Steps:**
1. Create backup of slot 1: `/islandselector admin backup <player> 1`
2. Create backup of slot 2: `/islandselector admin backup <player> 2`
3. Check backup directory

**Expected Result:**
- Two files exist: `slot-1-*.schem` and `slot-2-*.schem`
- Each slot's backups are tracked separately

---

### Test 5: Backup Non-Existent Slot

**Steps:**
1. Try to backup a slot that doesn't exist
   ```
   /islandselector admin backup <player> 99
   ```

**Expected Result:**
- Error message: "Slot 99 has no island for player..."
- No backup file created
- No console errors

---

### Test 6: Backup Empty Slot

**Steps:**
1. Find a player with an empty slot (slot created but no island)
2. Try to backup that slot
   ```
   /islandselector admin backup <player> 2
   ```

**Expected Result:**
- Error message indicating slot has no island
- No backup file created

---

### Test 7: Tab Completion

**Steps:**
1. Type `/islandselector admin backup ` and press TAB
2. Type `/islandselector admin backup Player1 ` and press TAB

**Expected Result:**
- First TAB shows list of online player names
- Second TAB shows slot numbers (1-10)

---

### Test 8: Console Execution

**Steps:**
1. Run command from server console (not in-game)
   ```
   islandselector admin backup Player1 1
   ```

**Expected Result:**
- Command works from console ✅
- Success/error messages appear in console
- Backup created successfully

---

### Test 9: Permission Check

**Prerequisites:**
- Player without `islandselector.admin.backup` permission

**Steps:**
1. Try to run backup command as non-admin player
   ```
   /islandselector admin backup Player1 1
   ```

**Expected Result:**
- Permission error message
- No backup created

---

### Test 10: Large Island Backup

**Prerequisites:**
- Player with large, complex island (many blocks, entities)

**Steps:**
1. Build a large island with:
   - Many different block types
   - Chests with items
   - Item frames, armor stands
   - Animals, villagers
2. Run backup command
3. Watch server TPS (should not drop)
4. Verify backup completes

**Expected Result:**
- Backup completes successfully
- Server does not lag (async operation)
- All blocks and entities preserved in schematic

---

## Edge Cases

### Edge Case 1: Invalid Player Name
```
/islandselector admin backup FakePlayer999 1
```
**Expected:** Error message, no crash

### Edge Case 2: Invalid Slot Number
```
/islandselector admin backup Player1 abc
```
**Expected:** "Invalid slot number" error

### Edge Case 3: Slot Number Out of Range
```
/islandselector admin backup Player1 999
```
**Expected:** "Invalid slot" error (must be 1-10)

### Edge Case 4: Disk Full Scenario
**Note:** Hard to test, but backup should fail gracefully with error message

---

## Configuration Testing

### Config Option: backups.max-per-slot

**Test Values:**
- `0` (unlimited) - Should keep all backups
- `1` - Should keep only most recent backup
- `3` (default) - Should keep 3 most recent
- `10` - Should keep 10 most recent

**Steps for each value:**
1. Set value in config.yml
2. Reload: `/islandselector admin reload`
3. Create multiple backups
4. Verify retention limit is enforced

---

## Performance Testing

### Test: Backup Performance

**Setup:**
1. Create 5 different islands with varying sizes
2. Time each backup creation

**Measure:**
- Time taken for backup
- Server TPS during backup
- File size of resulting schematic

**Expected:**
- Small island: <2 seconds
- Medium island: 2-5 seconds
- Large island: 5-10 seconds
- Server TPS: Should remain above 18 (async operation)

---

## Integration Testing

### Integration Test 1: Backup + Slot Switch

**Steps:**
1. Create backup of slot 1
2. Switch from slot 1 to slot 2
3. Verify backup exists and wasn't corrupted
4. Load backup file in WorldEdit to verify contents

**Expected:** Backup is valid and uncorrupted ✅

---

### Integration Test 2: Backup + Island Deletion

**Steps:**
1. Create backup of slot 1
2. Delete the island
3. Verify backup file still exists
4. Verify backup can still be read

**Expected:** Backup persists after island deletion ✅

---

## Troubleshooting

### Issue: "Failed to create backup"

**Possible Causes:**
1. FAWE not installed
2. World not found
3. Island doesn't exist
4. Disk full
5. Permission issues on backup directory

**Solutions:**
- Check console for detailed error
- Verify FAWE is installed: `/version FastAsyncWorldEdit`
- Check island exists: `/islandselector admin info <location>`
- Verify disk space
- Check file permissions on `plugins/IslandSelector/backups/`

### Issue: Backups Not Deleting

**Possible Causes:**
1. `max-per-slot` set to 0 (unlimited)
2. File permission issues
3. Backups in different directories

**Solutions:**
- Check config: `backups.max-per-slot`
- Check file permissions
- Verify all backups are in correct directory

### Issue: Server Lag During Backup

**Note:** This should NOT happen (async operation)

**If it does occur:**
- Check TPS: `/tps`
- Check CPU usage on server
- Report bug with console logs

---

## Success Criteria

Test #114 PASSES if:
- ✅ Command executes: `/islandselector admin backup <player> <slot>`
- ✅ Success message appears
- ✅ Backup file created in correct directory
- ✅ File is valid .schem format
- ✅ File size > 0 bytes
- ✅ No console errors
- ✅ Backup retention limit works
- ✅ Server doesn't lag during backup
- ✅ Tab completion works
- ✅ Console execution works

---

## Reporting Results

### If Test Passes ✅

Report:
```
Test #114: PASS
- Command works correctly
- Backups created successfully
- Retention limit enforced
- No performance issues
- File: slot-1-{timestamp}.schem verified
```

### If Test Fails ❌

Report:
```
Test #114: FAIL
- Issue: [Describe what went wrong]
- Steps to reproduce: [Exact steps]
- Error message: [Copy exact error]
- Console log: [Paste relevant errors]
- Expected: [What should happen]
- Actual: [What actually happened]
```

---

## Quick Reference

### Commands
```bash
# Create backup
/islandselector admin backup <player> <slot>

# Check if backup exists
# Navigate to: plugins/IslandSelector/backups/{uuid}/

# Test with console
islandselector admin backup Player1 1
```

### File Locations
```
Backup Directory: plugins/IslandSelector/backups/
Player Backups: plugins/IslandSelector/backups/{player-uuid}/
Backup Format: slot-{number}-{timestamp}.schem
Example: slot-1-20241202-155230.schem
```

### Config Settings
```yaml
backups:
  enabled: true
  on-switch: true
  max-per-slot: 3  # Max backups to keep per slot (0 = unlimited)
  on-shutdown: false
```

### Permissions
```
islandselector.admin.backup - Allows using backup command
islandselector.admin - Parent permission for all admin commands
```

---

## Estimated Testing Time

- **Quick Test (Steps 1-4):** 5 minutes
- **Full Test Suite (All tests):** 30-40 minutes
- **Edge Cases + Config Testing:** +15 minutes
- **Performance Testing:** +10 minutes

**Total Comprehensive Test:** ~1 hour

---

## Notes for Human Tester

**What's New:**
- This is the FIRST backup-related feature implemented
- Automatic backups on slot switch are NOT yet implemented
- This command allows admins to manually create backups
- Future sessions will implement automatic backup system

**Why This Feature:**
- Allows admins to backup player islands before maintenance
- Useful for troubleshooting
- Foundation for automatic backup system
- Safety net for players

**What's Next (Future Sessions):**
- Automatic backup on slot switch (Test #111)
- Backup on server shutdown (config option)
- Admin restore command (restore from backup)
- Backup pruning commands

---

## Build Information

**Build Status:** ✅ SUCCESS
**JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
**JAR Size:** 164K (increased from 156K due to BackupManager)
**Files Added:** 2 (BackupManager.java, AdminBackupCommand.java)
**Compilation Errors:** 0
**Warnings:** 0 (except expected deprecation)

---

**Session 33 Complete**
**Feature:** Admin Backup Command
**Status:** Ready for Manual Testing
**Next:** Awaiting human tester verification
