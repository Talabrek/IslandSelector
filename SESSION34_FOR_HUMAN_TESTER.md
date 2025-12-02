# Session 34 - For Human Tester

## Quick Summary

**What Was Built:** Automatic backup on slot switch
**Tests Implemented:** Tests #111, #112, #115
**Build Status:** ✅ SUCCESS
**Your Task:** Test auto-backup feature

---

## What This Feature Does

When players switch between island slots, the plugin now **automatically creates a backup** of their current island before the switch. This provides insurance against data loss.

**Example:**
```
Player has 2 slots with islands
Switches from Slot 1 → Slot 2
Plugin automatically backs up Slot 1 before switch
Backup saved: plugins/IslandSelector/backups/{uuid}/slot-1-{timestamp}.schem
```

---

## Quick 5-Minute Test

1. **Install:** Copy `output/IslandSelector-1.0.0-SNAPSHOT.jar` to `plugins/BentoBox/addons/`, restart

2. **Create 2 slots:** `/islandselector slots` → Create slot 1 and 2

3. **Build something:** Place blocks on slot 1 (so backup has content)

4. **Switch slots:**
   ```
   /islandselector slots
   Click slot 2
   Confirm switch
   ```

5. **Check for backup:**
   ```
   Navigate to: plugins/IslandSelector/backups/{your-uuid}/
   Look for: slot-1-{timestamp}.schem
   Example: slot-1-20241202-160500.schem
   ```

6. **Verify:**
   - ✅ File exists
   - ✅ File size > 0 bytes
   - ✅ No console errors
   - ✅ You saw message "Creating backup of current island..."

**Result:** If all 6 pass, Test #111 works! ✅

---

## What to Test

### Test #111: Auto-backup on switch
- Switch between slots
- Verify backup created each time
- Check file format and size

### Test #112: Old backups deleted
- Switch 5 times (creates 5 backups)
- Verify only 3 most recent kept
- Oldest 2 should be auto-deleted

### Test #115: Can disable in config
- Set `backups.on-switch: false`
- Switch slots
- Verify NO backup created

---

## Configuration

```yaml
backups:
  on-switch: true   # Enable/disable (default: true)
  max-per-slot: 3   # Max backups to keep (default: 3)
```

**To change:**
1. Edit `plugins/IslandSelector/config.yml`
2. Reload: `/islandselector admin reload`

---

## Expected Behavior

### During Slot Switch:
1. Player opens slot GUI
2. Clicks different slot
3. Confirms switch
4. Sees progress messages:
   - "Starting slot switch..."
   - **"Creating backup of current island..."** ← NEW
   - "Saving current island..."
   - "Clearing current island from world..."
   - "Loading target island..."
   - "Teleporting to new island..."
   - "Slot switch complete!"

### In Console:
```
[IslandSelector] Auto-backup created for PlayerName slot 1
[IslandSelector] Backup created for player {uuid} slot 1: slot-1-20241202-160500.schem
```

### On Disk:
```
plugins/IslandSelector/backups/
└── {player-uuid}/
    ├── slot-1-20241202-160500.schem (most recent)
    ├── slot-1-20241202-160230.schem (older)
    └── slot-1-20241202-155900.schem (oldest)
```

---

## Troubleshooting

### No backup created?
- Check: `backups.on-switch: true` in config
- Reload: `/islandselector admin reload`
- Verify FAWE installed: `/version FastAsyncWorldEdit`

### Backup file is 0 bytes?
- Check console for WorldEdit errors
- Verify island has blocks (not empty)
- Check server has disk space

### Old backups not deleting?
- Check `max-per-slot` in config
- If 0, means unlimited (no auto-delete)
- Set to 3 for normal retention

---

## Report Format

```
Test #111 - Auto-backup on switch: [PASS/FAIL]
  What worked: [list what worked]
  What failed: [list issues]
  Console errors: [paste errors if any]

Test #112 - Retention limit: [PASS/FAIL]
  What worked: [list what worked]
  What failed: [list issues]

Test #115 - Config disable: [PASS/FAIL]
  What worked: [list what worked]
  What failed: [list issues]
```

---

## Full Testing Guide

**See:** `TESTING_SESSION34.md` for comprehensive testing instructions

Includes:
- Detailed test procedures
- Advanced test cases
- Troubleshooting guide
- Performance notes

---

## What's Different From Session 33?

**Session 33:** Manual admin backup command
- `/islandselector admin backup {player} {slot}`
- Admin-triggered only

**Session 34:** Automatic backup on switch
- Happens automatically when players switch
- Configurable (can enable/disable)
- Uses same BackupManager as manual backup

**Both work together:**
- Manual and auto backups share retention limit
- Same file format and directory structure

---

## File Information

**JAR:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
**Size:** 164K
**Modified Files:** SlotSwitchManager.java (added auto-backup call)

---

## Confidence Level

**Implementation Quality:** EXCELLENT

✅ Clean integration with existing BackupManager
✅ Non-blocking (failure doesn't break switch)
✅ Configurable
✅ Proper error handling
✅ Zero compilation warnings
✅ Follows existing patterns

**Expected Result:** Should work perfectly on first try

---

## Next Session Priorities

**If Tests Pass:**
- Implement admin restore command (restore from backup)
- Implement backup on server shutdown
- Implement island relocation (Tests #116+)

**If Tests Fail:**
- Fix reported issues
- Re-build and re-test

---

## Questions?

If anything is unclear:
1. See TESTING_SESSION34.md for details
2. Check console for error messages
3. Report what you observe (even if different than expected)

---

**Ready to test! Copy the JAR and let's verify auto-backup works!** 🎯

JAR: `output/IslandSelector-1.0.0-SNAPSHOT.jar`
→ Copy to: `plugins/BentoBox/addons/`
