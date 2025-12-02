# Session 36 - For Human Tester

## Quick Summary

**Feature:** Island Relocation
**Purpose:** Players can physically move their entire island to a new grid location
**Build Status:** ✅ SUCCESS
**Your Task:** Test island relocation functionality

---

## What Was Built

Complete island relocation system:

- **Physical relocation** using FAWE (no server lag)
- **Cooldown system** (configurable, default 7 days)
- **Cost system** (Vault integration, optional)
- **Confirmation GUI** with preview
- **Grid integration** (click empty location to relocate)
- **Team & visitor handling** (automatic teleportation)
- **BSkyBlock integration** (spawn points, warps)

---

## Quick 5-Minute Test

### Setup
1. Copy JAR: `output/IslandSelector-1.0.0-SNAPSHOT.jar` → `plugins/BentoBox/addons/`
2. Restart server
3. **IMPORTANT:** Make sure FAWE (FastAsyncWorldEdit) is installed!

### Test Steps

**1. Create an island:**
```
- Run: /island create
- Select location 0,0 in grid
- Build something (5 dirt blocks)
```

**2. Relocate:**
```
- Run: /islandselector
- Click empty location (e.g., 1,1)
- Verify confirmation GUI opens
- Click green CONFIRM button
- Watch progress messages
```

**3. Verify:**
```
- Go to new location - island should be there
- Go to old location - should be cleared/empty
- Run: /island go - should teleport to new location
```

**Expected:** ✅ Island moves successfully, old location cleared

---

## Full Testing

See `TESTING_SESSION36.md` for comprehensive test scenarios:
- 10 detailed test cases
- Cooldown testing
- Cost system testing
- Team member teleportation
- BSkyBlock integration
- And more...

---

## Configuration (Optional)

For faster testing, edit `config.yml`:

```yaml
relocation:
  cooldown: 60      # Set to 60 seconds instead of 7 days
  cost: 1000.0      # Set to test economy (or 0.0 for free)
```

Then run: `/islandselector admin reload`

---

## What to Check

✅ Island moves to new location
✅ Old location is completely cleared
✅ All blocks are preserved
✅ Chests/signs keep their contents
✅ No server lag during relocation
✅ Grid GUI updates correctly
✅ /island go works at new location
✅ Team members are teleported
✅ Progress messages are clear
✅ No console errors

---

## Report Format

```
=== Island Relocation Test Report ===

Basic Test: [PASS/FAIL]
- Island moved: Yes/No
- Old location cleared: Yes/No
- No errors: Yes/No

Issues found: [describe any problems]
Console errors: [paste if any]

Overall: [PASS/FAIL]
```

---

## File Info

**JAR:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
**Size:** 179K (+11K from Session 35)
**Changes:** +2 new files, +3 modified files

---

## Important Notes

- **Requires FAWE:** FastAsyncWorldEdit must be installed
- **Async operation:** No server lag, operations run in background
- **Data safety:** All blocks, items, and entities are preserved
- **Reversible:** Can relocate again after cooldown expires

---

## Next After This

**If This Passes:**
- Island relocation complete! 🎉
- All major features now implemented
- Can begin systematic testing of all features

**If This Fails:**
- Report issues with details
- We'll fix before moving forward

---

**Ready to test!** 🎯

**JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`

**Core Test:** Create island → Relocate → Verify

**Expected Time:** 5-10 minutes for basic test

**Required:** FastAsyncWorldEdit (FAWE) plugin must be installed!
