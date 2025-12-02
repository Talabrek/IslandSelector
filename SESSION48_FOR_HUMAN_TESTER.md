# Session 48: Admin Purge Commands

## Quick Summary
**Features:** Admin commands to purge slot data for players (specific, inactive, or banned)
**Time Estimate:** 20-30 minutes
**Players Required:** 2-3 accounts (including test accounts that can be banned/made inactive)
**Complexity:** Advanced (requires admin permissions, testing with offline players)
**Dependencies:** None

## What Was Implemented

### Admin Purge System
Three new admin commands for removing slot data:

1. **`/islandselector admin purge <player>`** - Purge specific player's slot data
2. **`/islandselector admin purge inactive <days> [--dry-run]`** - Purge players inactive for X days
3. **`/islandselector admin purge banned [--dry-run]`** - Purge banned players

All purge operations:
- Require confirmation (two-step process for safety)
- Remove slot metadata from database
- Delete schematic files from disk
- Write audit log to `plugins/IslandSelector/audit/purge_<timestamp>.log`
- Cannot be performed on online players (safety check)
- Support dry-run mode for preview

## Test Cases

### Test #1: Admin Purge Specific Player Command
**Description:** Admin can purge all slot data for a specific offline player

**Setup:**
- Create a test player account (e.g., "TestPlayer1")
- Have TestPlayer1 create an island with 1-2 slots
- TestPlayer1 logs out (must be offline for purge)
- Log in as admin with `islandselector.admin.purge` permission

**Steps:**
1. Verify TestPlayer1 has slot data: `/islandselector admin info <TestPlayer1's location>`
2. Run: `/islandselector admin purge TestPlayer1`
3. Verify confirmation message appears showing:
   - Player name: "TestPlayer1"
   - Number of slots to be deleted
   - Warning about permanence
   - Confirmation command to run
4. Copy and run the confirmation command
5. Verify success message shows purge completed
6. Check file system: `plugins/IslandSelector/slots/<TestPlayer1-UUID>/`
7. Verify schematic files are deleted
8. Check `plugins/IslandSelector/audit/` for purge log file
9. Verify audit log contains details of the purge operation
10. Try `/islandselector admin info` on TestPlayer1's old location
11. Verify no slot data exists for TestPlayer1

**Expected Result:**
- ✅ Confirmation required before purge
- ✅ All slot metadata removed from database
- ✅ All schematic files deleted from disk
- ✅ Player's slot directory removed if empty
- ✅ Audit log created with operation details
- ✅ Success message shown to admin

**Pass Criteria:** ✅ Player's slot data completely purged with confirmation and audit

---

### Test #2: Cannot Purge Online Player
**Description:** Purge command blocks operations on currently online players (safety check)

**Setup:**
- Have a player online with island data
- Admin with purge permission

**Steps:**
1. Verify target player is online on the server
2. Run: `/islandselector admin purge <OnlinePlayer>`
3. Verify error message: "Player is online"
4. Verify purge is blocked (no confirmation shown)
5. Have the player log out
6. Run same command again
7. Verify this time confirmation appears

**Expected Result:**
- ✅ Online players cannot be purged (safety check)
- ✅ Clear error message explaining why
- ✅ Offline players can be purged normally

**Pass Criteria:** ✅ Online player purge blocked with clear error message

---

### Test #3: Purge Player With No Data
**Description:** Graceful handling when trying to purge player with no slot data

**Steps:**
1. Run: `/islandselector admin purge NonExistentPlayer`
2. Verify error message: "Player has no slot data"
3. Verify no confirmation command shown
4. Verify no audit log created

**Expected Result:**
- ✅ Clear message that player has no data
- ✅ No unnecessary files or database operations
- ✅ No errors or exceptions

**Pass Criteria:** ✅ Graceful handling of no-data case

---

### Test #4: Purge Inactive Players (Dry Run)
**Description:** Preview which players would be purged without actually deleting data

**Setup:**
- Have several test players with varying last-played times:
  - Player A: Played today (active)
  - Player B: Played 50 days ago (inactive 50d)
  - Player C: Played 100 days ago (inactive 100d)
- All players must be offline

**Steps:**
1. Run: `/islandselector admin purge inactive 60 --dry-run`
2. Verify preview message shows:
   - Total count of players who would be purged (should include Player C only)
   - List of affected players (up to 10 shown)
   - Each player shows: name, slot count, days since login
3. Verify "Dry run complete" message
4. Verify NO confirmation command shown (dry run doesn't proceed)
5. Check database - verify Player C's data still exists
6. Check file system - verify Player C's schematics still exist
7. Verify no audit log created

**Expected Result:**
- ✅ Shows accurate preview of affected players
- ✅ Only includes players inactive > 60 days
- ✅ No data actually deleted in dry-run mode
- ✅ Clear "dry run complete" message

**Pass Criteria:** ✅ Accurate preview without deleting data

---

### Test #5: Purge Inactive Players (Real Purge)
**Description:** Actually purge slot data for players inactive beyond threshold

**Setup:**
- Same setup as Test #4
- Player C offline and inactive 100+ days

**Steps:**
1. Run: `/islandselector admin purge inactive 90`
2. Verify preview shows Player C in the list
3. Verify confirmation command is provided
4. Copy and run the confirmation command
5. Verify "Purge executing" message
6. Wait for completion message
7. Verify success message shows purged count
8. Check Player C's slot data is gone (database & files)
9. Verify Player A and Player B data remains intact
10. Check audit log in `plugins/IslandSelector/audit/`
11. Verify log contains:
    - Operation timestamp
    - Admin who initiated purge
    - List of purged players with details
    - Success/failure status for each

**Expected Result:**
- ✅ Only players inactive > 90 days purged
- ✅ Active players' data unchanged
- ✅ Comprehensive audit log created
- ✅ Clear success/failure reporting

**Pass Criteria:** ✅ Inactive players purged, active players safe, audit logged

---

### Test #6: Purge Banned Players (Dry Run)
**Description:** Preview banned players' data for purging

**Setup:**
- Have 2-3 test players
- Ban one of them using `/ban TestPlayer2`
- Ensure banned player has slot data
- Other players unbanned

**Steps:**
1. Run: `/islandselector admin purge banned --dry-run`
2. Verify preview shows:
   - Count of banned players with slot data
   - List of banned players (TestPlayer2 should appear)
   - Each shows: name, slot count
3. Verify unbanned players NOT in the list
4. Verify "Dry run complete" message
5. Check TestPlayer2's data still exists (not deleted in dry run)

**Expected Result:**
- ✅ Shows only banned players with slot data
- ✅ Unbanned players not included
- ✅ No data deleted in dry run

**Pass Criteria:** ✅ Accurate preview of banned players only

---

### Test #7: Purge Banned Players (Real Purge)
**Description:** Actually purge slot data for banned players

**Setup:**
- TestPlayer2 is banned and has slot data
- TestPlayer1 is NOT banned and has slot data

**Steps:**
1. Run: `/islandselector admin purge banned`
2. Verify preview shows TestPlayer2 only
3. Verify confirmation command provided
4. Run confirmation command
5. Wait for completion
6. Verify success message
7. Check TestPlayer2's slot data is gone
8. Verify TestPlayer1's data unchanged (unbanned player)
9. Verify audit log created
10. Verify audit log shows TestPlayer2 purged

**Expected Result:**
- ✅ Only banned players' data purged
- ✅ Unbanned players' data safe
- ✅ Audit log documents operation

**Pass Criteria:** ✅ Banned players purged, unbanned players safe

---

### Test #8: Purge With Many Players (Pagination)
**Description:** Preview handles large numbers of affected players gracefully

**Setup:**
- Have 15+ inactive/banned players with slot data (or simulate)

**Steps:**
1. Run: `/islandselector admin purge inactive 30 --dry-run`
2. If more than 10 players affected, verify:
   - First 10 players shown in detail
   - Message like "... and X more players"
   - Total count accurate
3. Run actual purge (confirm command)
4. Verify all players purged, not just the 10 shown

**Expected Result:**
- ✅ Preview shows first 10 with "and X more" message
- ✅ Total count accurate
- ✅ All affected players purged (not just displayed ones)

**Pass Criteria:** ✅ Handles large player counts correctly

---

### Test #9: Confirmation Command Security
**Description:** Confirmation commands can only be run by admins with proper permissions

**Steps:**
1. As admin, generate a purge confirmation command
2. Log out or switch to non-admin player
3. Try to run the confirmation command
4. Verify permission denied error
5. Log back as admin and run confirmation
6. Verify it works

**Expected Result:**
- ✅ Non-admins cannot execute confirmation commands
- ✅ Permission system properly enforced

**Pass Criteria:** ✅ Confirmation requires admin permission

---

### Test #10: Audit Log Format and Content
**Description:** Audit logs contain comprehensive, useful information

**Setup:**
- Perform any purge operation (single player or batch)

**Steps:**
1. Complete a purge operation
2. Navigate to `plugins/IslandSelector/audit/`
3. Open the latest `purge_<timestamp>.log` file
4. Verify log contains:
   - Clear header with timestamp
   - Admin who initiated the operation
   - Total player count
   - For each player:
     * Player name and UUID
     * Number of slots purged
     * Number of schematic files deleted
     * Success or failure status
   - Summary section with success/failure counts
5. Verify log is human-readable
6. Verify timestamps use clear format (YYYY-MM-DD HH:MM:SS)
7. Verify file names include timestamp (for multiple operations)

**Expected Result:**
- ✅ Comprehensive audit trail
- ✅ Human-readable format
- ✅ Unique filenames with timestamps
- ✅ All relevant information logged

**Pass Criteria:** ✅ Audit logs are complete, clear, and useful for record-keeping

---

## Configuration

No configuration changes needed - purge commands are always available to admins with proper permissions.

## Required Permissions

```yaml
islandselector.admin.purge:
  description: "Access to purge commands"
  default: op
```

## Tab Completion

The purge command supports tab completion:
- `/islandselector admin purge <tab>` - Shows: inactive, banned, [player names]
- `/islandselector admin purge inactive <tab>` - Shows: 30, 60, 90, 180, 365
- `/islandselector admin purge inactive 90 <tab>` - Shows: --dry-run

## Command Usage

```
/islandselector admin purge <player>
  - Purge specific offline player's slot data
  - Requires confirmation

/islandselector admin purge inactive <days> [--dry-run]
  - Purge players who haven't logged in for X days
  - --dry-run: Preview only, don't delete
  - Requires confirmation (unless dry-run)

/islandselector admin purge banned [--dry-run]
  - Purge all banned players' slot data
  - --dry-run: Preview only, don't delete
  - Requires confirmation (unless dry-run)
```

## Known Limitations

**Safety Checks:**
- Cannot purge online players (intentional safety feature)
- Always requires two-step confirmation (no instant purge)
- Dry-run doesn't require confirmation (it's just a preview)

**Data Removed:**
- Slot metadata (database entries)
- Schematic files (.schem files)
- Does NOT remove BSkyBlock island data (that's handled by BSkyBlock itself)

**Performance:**
- Purge operations run asynchronously to avoid lag
- Large batch purges may take time (progress reported when complete)

## Troubleshooting

**"Player is online" error:**
- Player must be offline to purge (safety check)
- Have them log out, then retry

**"Player has no slot data" error:**
- Player never used IslandSelector slot system
- Nothing to purge

**No confirmation command appears:**
- Check you have islandselector.admin.purge permission
- Dry-run mode doesn't show confirmation (it's preview only)

**Purge seems stuck:**
- Operation runs asynchronously - wait for completion message
- Check server console for errors
- Check server isn't frozen/lagging

## Success Criteria

- ✅ Can purge specific offline player's slot data
- ✅ Cannot purge online players (blocked)
- ✅ Inactive player purge works with configurable days threshold
- ✅ Banned player purge works correctly
- ✅ Dry-run mode previews without deleting
- ✅ Two-step confirmation required for all real purges
- ✅ Audit logs created for all purge operations
- ✅ Database entries removed
- ✅ Schematic files deleted from disk
- ✅ Tab completion works

## Test Report Template

```
Test #1 - Purge Specific Player: [PASS/FAIL]
Notes:

Test #2 - Cannot Purge Online Player: [PASS/FAIL]
Notes:

Test #3 - Purge Player With No Data: [PASS/FAIL]
Notes:

Test #4 - Purge Inactive (Dry Run): [PASS/FAIL]
Notes:

Test #5 - Purge Inactive (Real): [PASS/FAIL]
Notes:

Test #6 - Purge Banned (Dry Run): [PASS/FAIL]
Notes:

Test #7 - Purge Banned (Real): [PASS/FAIL]
Notes:

Test #8 - Many Players Pagination: [PASS/FAIL]
Notes:

Test #9 - Confirmation Security: [PASS/FAIL]
Notes:

Test #10 - Audit Log Quality: [PASS/FAIL]
Notes:

Overall: [PASS/FAIL]
Issues Found:

```

## Additional Notes

**For Server Admins:**

These commands are powerful tools for managing server storage:
- Clean up data from players who stopped playing
- Remove data from banned troublemakers
- Preview operations before executing (dry-run)
- Full audit trail for accountability

**Best Practices:**
- Always use --dry-run first to preview
- Check audit logs after major purge operations
- Backup database before mass purges (optional but recommended)
- Don't purge players who might return (be generous with inactive days)

**Why Two-Step Confirmation?**
- Purge is permanent and cannot be undone
- Prevents accidental deletions
- Gives admin time to review what will be deleted
- Follows safety-first design principle

**Audit Logs:**
- Stored permanently in `plugins/IslandSelector/audit/`
- Use for server records, moderation logs, compliance
- Each purge operation gets its own timestamped file
- Logs survive server restarts (file-based)
