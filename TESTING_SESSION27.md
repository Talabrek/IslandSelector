# Testing Instructions - Session 27

## Session Summary
**Session 27** implemented admin cooldown reset commands for controlling slot switch cooldowns.

**Tests Implemented:**
- Test #93: Admin reset cooldown switch command

**Related Tests (for future implementation):**
- Test #94: Admin reset cooldown relocate (structure in place, needs relocation system)
- Test #95: Admin reset cooldown all (structure in place)

---

## Build Information
- **Build Status:** ✅ SUCCESS
- **JAR Location:** `target/IslandSelector-1.0.0-SNAPSHOT.jar` (136K)
- **Compilation:** Clean, no errors

---

## Test #93: Admin Reset Cooldown Switch Command

### Description
Admin command to reset a player's slot switch cooldown, allowing them to switch slots immediately even if they're still on cooldown.

### Implementation Details
**Files Modified:**
- `src/main/java/world/bentobox/islandselector/commands/AdminResetCooldownCommand.java` (NEW)
- `src/main/java/world/bentobox/islandselector/commands/AdminCommand.java` (registered new command)
- `src/main/java/world/bentobox/islandselector/managers/SlotManager.java` (added `resetSwitchCooldown()` method)
- `src/main/resources/locales/en-US.yml` (added command messages)

**Command Syntax:**
```
/islandselector admin resetcooldown <player> <switch|relocate|all>
```

**Permission:**
```
islandselector.admin.resetcooldown
```

**Tab Completion:**
- Argument 1: Online player names
- Argument 2: switch, relocate, all

### Testing Steps

#### Setup
1. Copy `target/IslandSelector-1.0.0-SNAPSHOT.jar` to `plugins/BentoBox/addons/`
2. Restart your test server
3. Ensure you have admin permissions (`islandselector.admin.resetcooldown` or `islandselector.admin`)

#### Test #93: Reset Switch Cooldown

**Step 1: Create a cooldown scenario**
1. Join server as a test player (e.g., "TestPlayer")
2. Run `/islandselector slots` to open slot GUI
3. Create islands in multiple slots (slot 1 and slot 2)
4. Switch from slot 1 to slot 2
   - This triggers a cooldown
5. Try to switch back to slot 1 immediately
   - Expected: Error message about cooldown
   - Note the time remaining

**Step 2: Reset the cooldown**
1. As admin, run: `/islandselector admin resetcooldown TestPlayer switch`
2. Expected output: `&aReset switch cooldown for TestPlayer` (green success message)
3. Tab completion should work:
   - `/islandselector admin resetcooldown <TAB>` shows online players
   - `/islandselector admin resetcooldown TestPlayer <TAB>` shows: switch, relocate, all

**Step 3: Verify cooldown is reset**
1. As TestPlayer, run `/islandselector slots` again
2. Try to switch to slot 1
3. Expected: Switch succeeds immediately (no cooldown error)
4. Verify island loads correctly at the grid location

**Step 4: Test offline player**
1. Have TestPlayer log out
2. As admin, run: `/islandselector admin resetcooldown TestPlayer switch`
3. Expected: Command succeeds (works for offline players too)
4. Have TestPlayer log back in and verify they can switch without cooldown

**Step 5: Test error handling**
1. Try invalid cooldown type: `/islandselector admin resetcooldown TestPlayer invalid`
   - Expected: Error message about invalid type
2. Try non-existent player: `/islandselector admin resetcooldown FakePlayer123 switch`
   - Expected: Works (Bukkit.getOfflinePlayer() doesn't validate existence)
   - Note: This is acceptable behavior - resets nothing if player never existed

**Step 6: Test permission**
1. Remove admin permission from a player
2. Have them try: `/islandselector admin resetcooldown TestPlayer switch`
3. Expected: Permission denied error

---

## Future Tests (Not Yet Implemented)

### Test #94: Reset Relocate Cooldown
- Command: `/islandselector admin resetcooldown <player> relocate`
- Status: Command structure exists, but relocation system not implemented yet
- Will work when relocation feature is added

### Test #95: Reset All Cooldowns
- Command: `/islandselector admin resetcooldown <player> all`
- Status: Command structure exists
- Currently resets switch cooldown; will reset both when relocation is implemented

---

## Expected Behavior Summary

| Command | Expected Result |
|---------|-----------------|
| `/islandselector admin resetcooldown TestPlayer switch` | Clears switch cooldown, player can switch immediately |
| `/islandselector admin resetcooldown TestPlayer relocate` | (Structure exists, relocation not implemented) |
| `/islandselector admin resetcooldown TestPlayer all` | Clears all cooldowns |
| Invalid type | Error: "Invalid cooldown type. Use: switch, relocate, or all" |
| No permission | Error: "You don't have permission to do that" |

---

## Configuration Notes

**Switch Cooldown Setting:**
```yaml
# config.yml
slots:
  switch-cooldown: 300  # Cooldown in seconds (default 5 minutes)
```

The cooldown is stored as `lastSwitchTime` (timestamp) in the database. The reset command sets this to 0, making the cooldown expired.

---

## Known Limitations

1. **Relocation cooldown:** Command accepts "relocate" and "all" types, but relocation system isn't implemented yet
2. **Validation:** Command doesn't validate if player actually exists (uses Bukkit.getOfflinePlayer which always returns a value)
3. **Feedback:** No confirmation message sent to the target player (only admin sees success)

These are acceptable for current implementation and can be enhanced later.

---

## Success Criteria

✅ Test #93 passes if:
1. Admin can reset a player's switch cooldown
2. Player can immediately switch slots after cooldown is reset
3. Command works for both online and offline players
4. Tab completion works correctly
5. Permission system works
6. Error handling is appropriate
7. No errors in server console

---

## Notes for Human Tester

- This command is useful for:
  - Support tickets when players get stuck on cooldown
  - Testing slot switching functionality
  - VIP perks (allow immediate switches for donors)
  - Special events where cooldowns should be waived

- The command is safe to use - it only resets a timestamp, doesn't modify any actual island data

- Future enhancement: Could add a confirmation message to the target player when their cooldown is reset

---

## Next Steps

After Test #93 is verified:
- Consider implementing Tests #94-95 fully when relocation system is added
- Could implement Test #88-92 (Admin purge commands) next
- Or implement other admin utility commands
- Or work on PlaceholderAPI integration (Tests #96+)
