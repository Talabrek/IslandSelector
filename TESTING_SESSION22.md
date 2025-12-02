# IslandSelector - Session 22 Testing Guide

## Session 22 Overview
- **Build Date:** Session 22 (Admin Reserve Validation)
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **JAR Size:** 132K (was 128K)
- **Tests Implemented:** Test #77
- **Status:** ✅ Ready for Manual Testing

---

## What's New in Session 22

Session 22 implements enhanced validation for the admin reserve command:

### Test #77: Cannot Reserve Already Occupied Location

**Feature:** The `/islandselector admin reserve` command now properly validates that a location isn't already occupied before reserving it.

**Why This Matters:**
- Prevents accidental reservation of player islands
- Protects existing islands from administrative errors
- Provides clear error messages to admins

**What Changed:**
- Added occupation check before allowing reservation
- New error message when trying to reserve occupied location
- Improved command flow and validation

---

## Installation

1. **Backup Current Setup** (if upgrading)
   ```bash
   # Backup your current JAR
   cp plugins/BentoBox/addons/IslandSelector.jar plugins/BentoBox/addons/IslandSelector.jar.backup
   ```

2. **Install New JAR**
   ```bash
   # Copy new JAR
   cp output/IslandSelector-1.0.0-SNAPSHOT.jar plugins/BentoBox/addons/IslandSelector.jar
   ```

3. **Restart Server**
   - Stop server
   - Start server
   - Check console for successful loading

---

## Test Cases

### Test #77: Cannot Reserve Already Occupied Location

**Category:** Functional
**Priority:** HIGH
**Estimated Time:** 3 minutes

#### Test Steps:

**Step 1: Create a test island**
```
1. Join as a player (e.g., "TestPlayer")
2. Run: /island
3. Select a location (e.g., coordinate 5,5)
4. Create the island
5. Verify island is created successfully
```

**Step 2: Attempt to reserve the occupied location**
```
1. As admin, run: /islandselector admin reserve 5,5
2. Verify you receive error message
3. Expected message: "Cannot reserve 5,5 - it is currently occupied by a player's island"
```

**Step 3: Verify reservation did not succeed**
```
1. Run: /islandselector admin info 5,5
2. Verify location shows as occupied (not reserved)
3. Verify owner is TestPlayer
4. Open grid GUI and verify 5,5 shows player head (not gray glass)
```

**Step 4: Verify can reserve empty locations (regression test)**
```
1. Run: /islandselector admin reserve 10,10
2. Verify success message
3. Open grid GUI
4. Verify 10,10 shows as gray glass (reserved)
5. This confirms the fix didn't break normal reservation
```

#### Expected Results:

✅ **Pass Criteria:**
- Attempting to reserve occupied location shows error message
- Error message clearly states location is occupied
- Reservation command fails (returns error, doesn't reserve)
- Location remains occupied by player
- Can still reserve empty locations normally

❌ **Fail Criteria:**
- Command succeeds when it should fail
- No error message shown
- Location gets reserved despite being occupied
- Player's island is affected
- Cannot reserve empty locations anymore

#### Edge Cases to Test:

**Edge Case 1: Try reserving just-claimed location**
```
1. Create new island at 7,7
2. Immediately run: /islandselector admin reserve 7,7
3. Should fail with occupied error
```

**Edge Case 2: Reserve then unreserve then reserve again**
```
1. Reserve empty location 8,8
2. Unreserve it: /islandselector admin unreserve 8,8
3. Reserve it again: /islandselector admin reserve 8,8
4. Should succeed (location is empty)
```

**Edge Case 3: Multiple players' islands**
```
1. Player1 has island at 5,5
2. Player2 has island at 6,6
3. Try reserving 5,5 → should fail
4. Try reserving 6,6 → should fail
5. Both should show appropriate errors
```

---

## Regression Testing

Since we modified an existing command, verify these still work:

### Existing Features to Verify:

**1. Normal Reserve Command:**
```bash
/islandselector admin reserve 15,15
# Should succeed for empty locations
```

**2. Reserve Already Reserved:**
```bash
/islandselector admin reserve 15,15
/islandselector admin reserve 15,15  # Try again
# Should show "already reserved" error
```

**3. Reserve with Price (SetPrice):**
```bash
/islandselector admin reserve 16,16
/islandselector admin setprice 16,16 50000
# Should work normally
```

**4. Admin Info Command:**
```bash
/islandselector admin info 5,5
# Should show occupation details
```

**5. Grid GUI Display:**
```bash
/islandselector
# - Occupied locations → player heads
# - Reserved locations → gray glass
# - Premium locations → gold blocks
```

---

## Error Messages

### New Error Message (Test #77):
```
&cCannot reserve [coord] - it is currently occupied by a player's island
```

Example:
```
Cannot reserve 5,5 - it is currently occupied by a player's island
```

### Existing Error Messages (should still work):
```
[coord] is already reserved
Invalid coordinate format: [coord]. Use format like 0,0 or -1,2
```

---

## Console Output

When the command fails, you should see:
```
[IslandSelector] Admin attempted to reserve occupied location 5,5
```
(This is normal - just logging the admin action)

No errors or warnings should appear in console.

---

## Quick Smoke Test (2 minutes)

For quick verification:

```bash
# 1. Create island at 5,5
/island  # Select 5,5

# 2. Try to reserve it (should fail)
/islandselector admin reserve 5,5
# Expected: "Cannot reserve 5,5 - it is currently occupied by a player's island"

# 3. Reserve empty location (should work)
/islandselector admin reserve 20,20
# Expected: "Reserved location 20,20"
```

If both work correctly, Test #77 passes! ✅

---

## Reporting Results

### If Test Passes:
Reply with:
```
✅ Test #77 PASS - Cannot reserve occupied location works correctly
```

### If Test Fails:
Create `TESTER_FEEDBACK_SESSION22.txt` with:
```
❌ Test #77 FAIL

Issue: [Describe what went wrong]

Steps to reproduce:
1. [What you did]
2. [What happened]
3. [What should have happened]

Error messages: [Copy any errors from console]

Additional notes: [Any other observations]
```

---

## Summary

**Session 22** adds a simple but important validation:
- Admins cannot accidentally reserve player islands
- Clear error messaging for admin actions
- Protects player islands from admin mistakes

**Total Features Ready for Testing:**
- 38 tests pending manual verification (37 from previous sessions + 1 new)
- Test #77 is the newest addition

**Next Session:**
If Test #77 passes, Session 23 will implement Tests #78-80 (Grid expansion/shrinking commands).

---

## Questions?

- Check `SESSION22_NOTES.txt` for implementation details
- Check console logs for any errors
- Test on a non-production server first
- Leave feedback in `TESTER_FEEDBACK_SESSION22.txt` if issues found

**Happy Testing! 🧪**
