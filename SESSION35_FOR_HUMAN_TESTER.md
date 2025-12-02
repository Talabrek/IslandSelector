# Session 35 - For Human Tester

## Quick Summary

**Feature:** Admin Restore Command
**Purpose:** Restore player islands from backup files
**Build Status:** ✅ SUCCESS
**Your Task:** Test island restoration functionality

---

## What Was Built

An admin command to restore a player's island from a backup:
```
/islandselector admin restore <player> <slot> [backup-file]
```

**Auto-selects most recent backup if no filename provided.**

---

## Quick 5-Minute Test

### Setup
1. Copy JAR: `output/IslandSelector-1.0.0-SNAPSHOT.jar` → `plugins/BentoBox/addons/`
2. Restart server

### Test Steps

**1. Create a backup:**
```
- Build something on your island (e.g., 5 dirt blocks)
- Run: /islandselector admin backup YourName 1
- See success message
```

**2. Make changes:**
```
- Destroy what you built
- Place something different (e.g., diamond block)
```

**3. Restore:**
```
- Run: /islandselector admin restore YourName 1
- Wait for "Successfully restored" message
- Check island
```

**Expected:** ✅ Original build (5 dirt) is back, diamond block is gone

---

## What to Test

### Test #1: Basic Restore
- Create backup → make changes → restore → verify original state restored

### Test #2: Tab Completion
- Type command and press TAB at each stage
- Should suggest: players → slot numbers → backup files

### Test #3: Error Handling
- Try restoring slot with no backups
- Try invalid player name
- Try invalid slot number
- Verify clear error messages

### Test #4: Specific Backup File
```
/islandselector admin restore YourName 1 slot-1-20241202-161500.schem
```
- Verify can restore old backups (not just most recent)

---

## Expected Behavior

**During Restore:**
```
Admin runs: /islandselector admin restore Notch 1

Messages:
"Restoring backup for Notch slot 1..."
"File: slot-1-20241202-161500.schem"
"Backup date: 2024-12-02 16:15:00"

[wait 2-3 seconds]

"Successfully restored slot-1-20241202-161500.schem for Notch slot 1"
```

**Console:**
```
[IslandSelector] Restored backup slot-1-20241202-161500.schem for slot: {uuid}
```

**On Island:**
- All blocks match backup state
- Chests have original contents
- Signs show original text
- Entities (item frames, armor stands) restored

---

## Commands Reference

```bash
# Restore most recent backup
/islandselector admin restore <player> <slot>

# Restore specific backup
/islandselector admin restore <player> <slot> <filename.schem>

# Examples
/islandselector admin restore Notch 1
/islandselector admin restore Notch 1 slot-1-20241202-161500.schem
```

---

## Integration with Existing Features

**Works with backups from:**
1. Manual backup command (Session 33)
2. Auto-backup on slot switch (Session 34)

**All backups in:**
```
plugins/IslandSelector/backups/{player-uuid}/
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "No backups found" | Create backup first with `/islandselector admin backup` |
| "Backup file not found" | Check filename (use tab completion) |
| Island not changing | Re-log or teleport away and back |
| Console errors | Check FAWE is installed and working |

---

## Report Format

```
=== Restore Command Test Report ===

Basic Restore: [PASS/FAIL]
  - Backup created: Yes/No
  - Restore executed: Yes/No
  - Original state restored: Yes/No
  - Issues: [describe any problems]

Tab Completion: [PASS/FAIL]
  - Player names: Works/Broken
  - Slot numbers: Works/Broken
  - Backup files: Works/Broken

Error Handling: [PASS/FAIL]
  - No backups error: Clear/Unclear
  - Invalid player error: Clear/Unclear
  - Invalid slot error: Clear/Unclear

Specific Backup Restore: [PASS/FAIL]
  - Can select old backup: Yes/No
  - Correct version restored: Yes/No

Performance: [PASS/FAIL]
  - Server lag during restore: Yes/No
  - Restore completes in reasonable time: Yes/No

Overall: [PASS/FAIL]
Console Errors: [paste if any]
```

---

## What Makes This Pass?

**Minimum Requirements:**
✅ Can restore most recent backup
✅ Island blocks match backup state
✅ No console errors
✅ Command doesn't crash server

**Full Pass:**
✅ All above +
✅ Tab completion works
✅ Error messages are clear
✅ Can restore specific old backups
✅ No server lag during restore
✅ Tile entities (chests, signs) restored

---

## File Info

**JAR:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
**Size:** 168K
**Changes:** +1 new command, +3 new methods in BackupManager

---

## Why This Feature Matters

**Player Protection:**
- Accidents happen (TNT, creeper, lava)
- Griefing recovery
- Rollback after bad decisions

**Admin Tools:**
- Help players recover from mistakes
- Testing and development
- Server maintenance

**Completes Backup System:**
- Session 33: Manual backup ✅
- Session 34: Auto-backup ✅
- Session 35: Restore ✅

---

## Next After This

**If This Passes:**
- Backup/restore system complete!
- Next: Island relocation (major feature)
- Or: Backup on shutdown (minor enhancement)

**If This Fails:**
- Report issues clearly
- We'll fix before moving on

---

## Questions?

**Not sure how to test?**
- Follow the "Quick 5-Minute Test" section
- That covers the essential functionality

**Want more thorough testing?**
- See `TESTING_SESSION35.md` for 10 detailed test cases

**Something broken?**
- Note exactly what you did
- Copy any console errors
- Report what you expected vs. what happened

---

**Ready to test!** 🎯

**JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`

**Core Test:** Backup → Change → Restore → Verify

**Expected Time:** 5-10 minutes
