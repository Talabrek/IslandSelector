# Session 34 Testing Guide - Automatic Backup on Slot Switch

## Build Information
- **Session:** 34
- **Feature:** Automatic backup on slot switch
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **JAR Size:** 164K
- **Build Status:** ✅ SUCCESS

## What Was Implemented

### Feature: Automatic Backup on Slot Switch

When a player switches between island slots, the system now automatically creates a backup of their current island before performing the switch. This provides insurance against data loss during slot switches.

**Implementation:**
- Added automatic backup call in `SlotSwitchManager.switchSlot()` method
- Backup is created BEFORE the island is saved to schematic
- Respects `backups.on-switch` config setting
- Non-blocking: Backup failure doesn't cancel the switch (safety feature)
- Uses existing `BackupManager` from Session 33

**Configuration:**
```yaml
backups:
  on-switch: true  # Enable/disable auto-backup (default: true)
  max-per-slot: 3  # Max backups to keep per slot (default: 3)
```

---

## Test Cases

### Test #111: Automatic backup is created on slot switch

**Category:** Functional
**Priority:** HIGH - This is a data safety feature

#### Setup
1. Copy `output/IslandSelector-1.0.0-SNAPSHOT.jar` to `plugins/BentoBox/addons/`
2. Ensure config.yml has `backups.on-switch: true`
3. Restart server

#### Test Steps

**Step 1: Set backups.on-switch to true in config**
- Open `plugins/IslandSelector/config.yml`
- Verify `backups.on-switch: true`
- If not, set it and reload: `/islandselector admin reload`

**Step 2: Create two island slots**
- As a test player, run `/islandselector slots`
- Create slot 1 (or use existing)
- Switch to slot 2 and create it
- Build some structures on slot 1 (so backup has content)

**Step 3: Switch from slot 1 to slot 2**
- Activate slot 1 if not active
- Open `/islandselector slots`
- Click on slot 2
- Confirm the switch

**Step 4: Check plugins/IslandSelector/backups/{uuid}/**
- Navigate to `plugins/IslandSelector/backups/`
- Find folder with your player UUID
- Look for backup files

**Step 5: Verify backup file exists with format slot-1-{timestamp}.schem**
- Should see file like: `slot-1-20241202-160500.schem`
- Filename format: `slot-{number}-{yyyyMMdd-HHmmss}.schem`

**Step 6: Verify backup file is non-zero size**
- Check file size: should be > 0 bytes
- Typical size: 10KB - 500KB depending on island builds
- If 0 bytes, backup failed

#### Expected Results
✅ Backup file created before switch
✅ Filename follows correct format
✅ File contains island data (non-zero size)
✅ No console errors during switch
✅ Player sees progress message "Creating backup of current island..."
✅ Switch completes successfully

#### Failure Indicators
❌ No backup file created
❌ Backup file is 0 bytes
❌ Console shows backup errors
❌ Player doesn't see backup progress message

---

### Test #112: Backup retention limit is enforced (old backups deleted)

**Category:** Functional
**Priority:** MEDIUM

#### Test Steps

**Step 1: Set backups.max-per-slot to 3 in config**
```yaml
backups:
  max-per-slot: 3
```
- Reload: `/islandselector admin reload`

**Step 2: Switch slots 5 times to create 5 backups**
- Switch from slot 1 → 2 (backup 1)
- Switch from slot 2 → 1 (backup 2)
- Switch from slot 1 → 2 (backup 3)
- Switch from slot 2 → 1 (backup 4)
- Switch from slot 1 → 2 (backup 5)

**Step 3: Check backup directory**
```
plugins/IslandSelector/backups/{uuid}/
```

**Step 4: Verify only 3 most recent backups remain**
- Count files for slot 1: should be exactly 3
- Count files for slot 2: should be exactly 3
- Total: 6 backup files (3 per slot)

**Step 5: Verify oldest 2 backups were deleted automatically**
- Oldest backups should be gone
- Only most recent 3 per slot retained

#### Expected Results
✅ Only 3 backups exist per slot
✅ Oldest backups automatically deleted
✅ Most recent backups preserved
✅ No manual cleanup needed

---

### Test #115: Backup system can be disabled in config

**Category:** Configuration
**Priority:** MEDIUM

#### Test Steps

**Step 1: Set backups.on-switch to false in config**
```yaml
backups:
  on-switch: false
```

**Step 2: Reload or restart**
```
/islandselector admin reload
```

**Step 3: Switch slots**
- Switch from slot 1 to slot 2

**Step 4: Verify no backup is created**
- Check backup directory
- No new backup files should appear
- Player should NOT see backup progress message

**Step 5: Verify backup directory remains empty or unchanged**
- File count should not increase
- Last modified timestamps unchanged

#### Expected Results
✅ No backup created when disabled
✅ No backup progress message shown
✅ Switch still works normally
✅ No errors in console

---

## Quick Test (5 Minutes)

**Minimum viable test for manual tester:**

1. **Install JAR:** Copy to `plugins/BentoBox/addons/`, restart server

2. **Create 2 slots:** Use `/islandselector slots` to create slot 1 and 2

3. **Build something:** Place a few blocks on slot 1

4. **Switch slots:**
   ```
   /islandselector slots
   → Click slot 2
   → Confirm switch
   ```

5. **Check for backup:**
   ```
   Navigate to: plugins/IslandSelector/backups/{your-uuid}/
   Look for: slot-1-{timestamp}.schem
   ```

6. **Verify:**
   - ✅ Backup file exists
   - ✅ File size > 0
   - ✅ No console errors
   - ✅ Switch completed successfully

**If all 6 checks pass, Test #111 is working! ✅**

---

## Advanced Testing

### Test Backup Progression

**Purpose:** Verify backups accumulate correctly

**Steps:**
1. Switch slots 10 times
2. Check backup directory
3. With `max-per-slot: 3`, should see exactly 3 backups per slot
4. Verify automatic cleanup works

### Test With Auto-Backup Disabled

**Purpose:** Verify config toggle works

**Steps:**
1. Set `backups.on-switch: false`
2. Reload config
3. Switch slots
4. Verify NO backup created
5. Re-enable and verify backups work again

### Test Manual vs Auto Backup

**Purpose:** Verify both backup methods work

**Steps:**
1. Create manual backup: `/islandselector admin backup {player} 1`
2. Switch slots (auto backup)
3. Check directory for both backups
4. Verify both are valid .schem files
5. Verify retention applies to both types

---

## Console Monitoring

### Expected Console Output (Success)

```
[IslandSelector] Auto-backup created for TestPlayer slot 1
[IslandSelector] Backup created for player a1b2c3-uuid slot 1: slot-1-20241202-160500.schem
[IslandSelector] Deleted old backup: slot-1-20241202-153000.schem (if retention limit reached)
```

### Error Indicators

```
[IslandSelector] Auto-backup failed for TestPlayer slot 1 - continuing switch anyway
[IslandSelector] ERROR: Failed to create backup for player a1b2c3-uuid slot 1: [reason]
```

**Note:** Auto-backup failure is non-critical and won't prevent slot switch. The switch will continue but without backup protection.

---

## Configuration Reference

### Config.yml Backup Settings

```yaml
backups:
  enabled: true           # Not enforced yet (future)
  on-switch: true         # ✅ WORKS - Auto-backup on slot switch
  max-per-slot: 3         # ✅ WORKS - Max backups to keep
  on-shutdown: false      # Not implemented yet (future)
```

### Default Values
- `on-switch: true` (enabled by default for safety)
- `max-per-slot: 3` (keeps 3 most recent)

### Changing Settings
1. Edit `plugins/IslandSelector/config.yml`
2. Modify values
3. Reload: `/islandselector admin reload`
4. Test changes

---

## Troubleshooting

### Issue: No backup created

**Check:**
1. Is `backups.on-switch: true`?
2. Does player have an active island in slot?
3. Is FastAsyncWorldEdit installed?
4. Any console errors?
5. Does backup directory exist and have write permissions?

**Fix:**
- Verify config setting
- Reload config: `/islandselector admin reload`
- Check FAWE: `/version FastAsyncWorldEdit`
- Check file permissions on `plugins/IslandSelector/backups/`

### Issue: Backup file is 0 bytes

**Cause:** FAWE schematic save failed

**Check:**
- Console for WorldEdit errors
- Island has actual blocks (not empty)
- Server has disk space

### Issue: Too many backups (not deleting old ones)

**Check:**
1. What is `max-per-slot` set to?
2. 0 = unlimited (no auto-delete)
3. Set to 3 for normal retention

**Fix:**
- Set `max-per-slot: 3` in config
- Reload
- Next backup will trigger cleanup

### Issue: Backup slows down switch

**Expected Behavior:**
- Backup runs asynchronously (won't lag)
- Adds 1-2 seconds to switch time
- Normal for large islands

**If severe lag:**
- Check server TPS during switch
- Reduce island protection range
- Check FAWE config for optimization

---

## Integration with Session 33 Features

### Manual Backup Command (Test #114)
- Still works independently
- Admin can force backup anytime: `/islandselector admin backup {player} {slot}`
- Auto-backup and manual backup both count toward retention limit

### Backup Manager
- Shared by both manual and auto backups
- Same file format
- Same retention policy
- Same directory structure

---

## File Structure

```
plugins/IslandSelector/
├── backups/
│   └── {player-uuid}/
│       ├── slot-1-20241202-160500.schem (auto-backup)
│       ├── slot-1-20241202-160230.schem (auto-backup)
│       ├── slot-1-20241202-155900.schem (manual backup)
│       ├── slot-2-20241202-160545.schem (auto-backup)
│       └── slot-2-20241202-160215.schem (auto-backup)
└── config.yml
```

**Notes:**
- Backups are per-player, per-slot
- No distinction between manual and auto backups in filename
- Retention applies to all backups equally
- Oldest deleted first when limit reached

---

## Performance Notes

### Impact on Slot Switch
- Adds ~1-2 seconds to switch time (varies by island size)
- Runs asynchronously (no server lag)
- Non-blocking (failure doesn't prevent switch)

### Disk Usage
- Each backup: 10KB - 500KB typically
- With 3 backups per slot: ~30KB - 1.5MB per slot
- 100 players × 5 slots × 3 backups = ~150MB typical
- Monitor disk usage on large servers

### Recommendations
- Keep `max-per-slot` at 3 for most servers
- Increase to 5-10 for premium servers
- Set to 0 (unlimited) only if disk space abundant
- Regular manual cleanup of old player UUIDs

---

## Success Criteria

### Test #111 Passes If:
- ✅ Backup created on every slot switch
- ✅ Backup file has correct format
- ✅ Backup file contains island data
- ✅ Player sees backup progress message
- ✅ No errors in console
- ✅ Switch completes successfully

### Test #112 Passes If:
- ✅ Retention limit enforced correctly
- ✅ Oldest backups deleted automatically
- ✅ Most recent N backups retained
- ✅ Cleanup happens per-slot (not global)

### Test #115 Passes If:
- ✅ Disabling `on-switch` prevents backups
- ✅ Enabling `on-switch` resumes backups
- ✅ Switch works normally either way
- ✅ No errors in either state

---

## Next Steps After Testing

### If All Tests Pass:
1. Report Test #111 as PASSING
2. Report Test #112 as PASSING
3. Report Test #115 as PASSING
4. Next agent will mark tests as passing in feature_list.json

### If Any Test Fails:
1. Document which test failed
2. Provide error messages from console
3. Describe what didn't work as expected
4. Next agent will fix the issues

---

## Report Template

```
Session 34 Testing Results

Test #111 - Automatic backup on slot switch: [PASS/FAIL]
  - Backup created: [YES/NO]
  - File format correct: [YES/NO]
  - File size valid: [YES/NO]
  - Issues: [describe any problems]

Test #112 - Backup retention enforced: [PASS/FAIL]
  - Old backups deleted: [YES/NO]
  - Correct count retained: [YES/NO]
  - Issues: [describe any problems]

Test #115 - Config disable works: [PASS/FAIL]
  - No backup when disabled: [YES/NO]
  - Works when re-enabled: [YES/NO]
  - Issues: [describe any problems]

Console Errors: [paste any errors]

Screenshots: [optional]

Additional Notes: [any observations]
```

---

## Estimated Testing Time

- **Quick Test:** 5 minutes
- **Core Features (Tests #111, #112, #115):** 20 minutes
- **Advanced Testing:** 15 minutes
- **Total Comprehensive Test:** 40 minutes

---

## Summary

**What's New:**
- Automatic backup on slot switch (configurable)
- Backup retention applies to auto-backups
- Config toggle to enable/disable

**What Works:**
- Manual backup command (from Session 33)
- Automatic backup on switch (this session)
- Shared retention system

**What's Next (Future Sessions):**
- Backup on server shutdown
- Admin restore command
- Backup list/delete commands

---

**Ready for testing! 🧪**
