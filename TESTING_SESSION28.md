# Testing Instructions - Session 28

## Session Summary
**Session 28** implemented the `/islandselector setname` command for setting custom names on island slots.

**Test Implemented:**
- Slot naming command (from feature_list.json around line 1222)

**Related Features:**
- Integrates with existing SlotManager.setSlotName() method
- Custom slot names display in Slot Selection GUI
- Names persist across server restarts

---

## Build Information
- **Build Status:** ✅ SUCCESS
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar` (144K)
- **Compilation:** 33 source files, 0 errors
- **New Files:** SetNameCommand.java

---

## Test: Command /islandselector setname changes slot name

### Description
Players can set custom names for their island slots using the `/islandselector setname` command. This helps players organize their multiple islands with meaningful names instead of just "Island 1", "Island 2", etc.

### Implementation Details

**Files Created:**
- `src/main/java/world/bentobox/islandselector/commands/SetNameCommand.java` (NEW - 114 lines)

**Files Modified:**
- `src/main/java/world/bentobox/islandselector/commands/IslandSelectorCommand.java` (registered new command)
- `src/main/resources/locales/en-US.yml` (added 7 new messages)

**Command Syntax:**
```
/islandselector setname <slot> <name...>
```

**Aliases:**
```
/is setname <slot> <name...>
/isgrid setname <slot> <name...>
```

**Permission:**
```
islandselector.setname
```

**Tab Completion:**
- Argument 1: Slot numbers (1, 2, 3, 4, 5)
- Argument 2: Example names (Main_Base, Farm_Island, Mining_Island, Creative_Build)

**Validation:**
- Slot number must be a valid integer
- Slot number must be between 1 and max_slots (configurable, default 5)
- Slot must exist for the player (player must have created an island in that slot)
- Name cannot be empty
- Name maximum 32 characters
- Name can contain spaces (automatically joined from multiple arguments)

**Integration:**
- Uses existing `SlotManager.setSlotName()` method
- Names are stored in `SlotData` database objects
- Names persist across server restarts
- Names display in Slot Selection GUI

---

## Testing Steps

### Setup
1. Copy `output/IslandSelector-1.0.0-SNAPSHOT.jar` to `plugins/BentoBox/addons/`
2. Restart your test server
3. Ensure you have the permission `islandselector.setname` (or `islandselector.*`)

---

### Test 1: Basic Slot Naming

**Prerequisite:** Create islands in at least 2 slots

1. Create an island in slot 1 (if not already done):
   ```
   /islandselector slots
   [Click on Slot 1, confirm creation if needed]
   ```

2. Set a custom name for slot 1:
   ```
   /islandselector setname 1 Main Base
   ```

3. **Expected Result:**
   - Success message: `Set name for slot 1 to 'Main Base'` (green)
   - No errors in console

4. Verify the name was saved:
   ```
   /islandselector slots
   ```

5. **Expected Result:**
   - Slot 1 shows "Main Base" instead of "Island 1"
   - Name is visible in the slot GUI tooltip

---

### Test 2: Name Multiple Slots

1. Create islands in slots 1 and 2 (if not done)

2. Set names for both slots:
   ```
   /islandselector setname 1 Main Base
   /islandselector setname 2 Farm Island
   ```

3. **Expected Result:**
   - Both commands show success messages
   - Each slot is named correctly

4. Open slot GUI and verify:
   ```
   /islandselector slots
   ```

5. **Expected Result:**
   - Slot 1: "Main Base"
   - Slot 2: "Farm Island"
   - Default names for empty slots: "Island 3", "Island 4", "Island 5"

---

### Test 3: Name Persistence (Server Restart)

1. Set slot names as in Test 2

2. Restart the server:
   ```
   /stop
   [Start server again]
   ```

3. Rejoin and check slot names:
   ```
   /islandselector slots
   ```

4. **Expected Result:**
   - Custom names persist after restart
   - Slot 1 still shows "Main Base"
   - Slot 2 still shows "Farm Island"

---

### Test 4: Tab Completion

1. Type the command and press TAB:
   ```
   /islandselector setname <TAB>
   ```

2. **Expected Result:**
   - Shows slot numbers: 1, 2, 3, 4, 5

3. Type a slot number and press TAB:
   ```
   /islandselector setname 1 <TAB>
   ```

4. **Expected Result:**
   - Shows example names: Main_Base, Farm_Island, Mining_Island, Creative_Build

---

### Test 5: Error Cases

#### 5.1: Slot doesn't exist
```
/islandselector setname 3 Test Island
```
**Expected:** Error message: "You don't have an island in slot 3 yet!" (red)

#### 5.2: Invalid slot number (non-numeric)
```
/islandselector setname abc Test Island
```
**Expected:** Error message: "Invalid slot number. Must be a number." (red)

#### 5.3: Slot number out of range
```
/islandselector setname 99 Test Island
```
**Expected:** Error message: "Slot number must be between 1 and 5" (red)

#### 5.4: Empty name
```
/islandselector setname 1
```
**Expected:** Shows command help/usage

#### 5.5: Name too long (over 32 characters)
```
/islandselector setname 1 This_is_a_very_long_island_name_that_exceeds_the_maximum_length
```
**Expected:** Error message: "Slot name is too long! Maximum 32 characters." (red)

---

### Test 6: Special Characters in Names

1. Test with spaces:
   ```
   /islandselector setname 1 My Awesome Island
   ```
   **Expected:** Success, name is "My Awesome Island"

2. Test with underscores:
   ```
   /islandselector setname 1 Main_Base_V2
   ```
   **Expected:** Success, name is "Main_Base_V2"

3. Test with numbers:
   ```
   /islandselector setname 1 Island 2024
   ```
   **Expected:** Success, name is "Island 2024"

4. Test with emojis (if server supports):
   ```
   /islandselector setname 1 🏝️ Paradise
   ```
   **Expected:** Success or handled gracefully

---

### Test 7: Integration with Slot Switching

1. Set names for 2 slots:
   ```
   /islandselector setname 1 Main Base
   /islandselector setname 2 Farm Island
   ```

2. Switch between slots:
   ```
   /islandselector slots
   [Click on slot 2 to switch]
   ```

3. **Expected Result:**
   - Switching message shows custom name: "Welcome to Farm Island"
   - No errors
   - Names remain correct after switching

---

## Verification Checklist

- [ ] Command executes without errors
- [ ] Success message displays correctly
- [ ] Slot name shows in Slot Selection GUI
- [ ] Names persist after server restart
- [ ] Tab completion works for slot numbers
- [ ] Tab completion works for name suggestions
- [ ] Error messages for invalid slot numbers
- [ ] Error messages for non-existent slots
- [ ] Error messages for empty names
- [ ] Error messages for names over 32 characters
- [ ] Names with spaces work correctly
- [ ] Slot switching shows custom names
- [ ] Permission check works (denied without permission)
- [ ] No console errors during any operation

---

## Expected Behavior Summary

**Success Case:**
- Player has an island in the specified slot
- Slot number is 1-5 (or within configured max)
- Name is 1-32 characters
- Name is saved to database
- Name displays in Slot Selection GUI
- Name persists across restarts

**Failure Cases:**
- Slot doesn't exist → Error message
- Invalid slot number → Error message
- Slot out of range → Error message
- No name provided → Show help
- Name too long → Error message
- No permission → Permission denied

---

## Known Integration Points

1. **SlotManager.setSlotName():** Uses existing manager method
2. **SlotData:** Stores name in database
3. **SlotSelectionGUI:** Displays custom names (already implemented)
4. **Slot Switching:** Shows custom name in switch messages (already implemented)

---

## Notes for Developers

- The command accepts multi-word names (joins args with spaces)
- Validation happens before database update
- Names are optional - slots default to "Island X" if no custom name
- Max name length is hardcoded to 32 characters (could be made configurable)
- Permission is required: `islandselector.setname`

---

## Related Commands

- `/islandselector slots` - View all slots and their names
- `/islandselector admin info <player>` - View player's slot names (as admin)

---

## Feature Significance

This feature greatly improves the multi-slot island system UX:
- Players can organize islands by purpose (Main, Farm, Mining, etc.)
- Makes slot selection more intuitive
- Reduces confusion when switching between multiple islands
- Supports creative/roleplay scenarios with themed islands

**Estimated Testing Time:** 15-20 minutes
**Complexity:** Low (command-only feature, uses existing infrastructure)
**Risk:** Low (no world modifications, pure data)
