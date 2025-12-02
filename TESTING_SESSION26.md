# Session 26 Testing Guide - Admin Info Player Lookup Enhancement

## Overview
Session 26 enhanced the `/islandselector admin info` command to support player lookups in addition to coordinate lookups. Admins can now query detailed information about any player's islands, slots, and cooldowns.

**Build Status:** ✅ SUCCESS
**JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
**JAR Size:** 136K
**Compilation:** 31 source files, 0 errors, 1 deprecation warning (pre-existing)

---

## Test #87: Admin info command shows player island and slot details

**Feature:** Enhanced admin info command to accept player names
**Command:** `/islandselector admin info <player>`
**Permission:** `islandselector.admin.info`

### Test Steps

#### 1. Test with player who has an island

**Setup:**
```bash
# As player (e.g., "Notch"):
/island create
# Select location (e.g., 0,0)
# Wait for island creation to complete
```

**Test:**
```bash
# As admin:
/islandselector admin info Notch
```

**Expected Output:**
```
=== Player Info: Notch ===
Grid Location: 0,0
World Coordinates: 0, 0
Active Slot: #1 - Slot 1
Slots: 1/5 with islands
--- Slot Details ---
  #1: Slot 1 - ACTIVE - Has Island
  #2: Slot 2 - inactive - Empty
  #3: Slot 3 - inactive - Empty
  #4: Slot 4 - inactive - Empty
  #5: Slot 5 - inactive - Empty
Switch Cooldown: None
```

**Verification:**
- ✅ Shows player name in header
- ✅ Shows correct grid location
- ✅ Shows world coordinates (based on grid location × spacing)
- ✅ Shows active slot number and name
- ✅ Shows slot count (1/5 with islands)
- ✅ Lists all 5 slots with details
- ✅ Active slot marked as "ACTIVE"
- ✅ Occupied slot shows "Has Island"
- ✅ Empty slots show "Empty"
- ✅ Cooldown status shown

---

#### 2. Test with player who has multiple islands (multiple slots)

**Setup:**
```bash
# As player "Jeb":
/island create
# Select location (e.g., 5,5)

# Create second slot:
# (Assuming slot creation is working - may need admin command)
# Have island in slot 2 with custom name

# Set custom name for slot 1:
/islandselector setname 1 Main Base

# Set custom name for slot 2:
/islandselector setname 2 Farm Island
```

**Test:**
```bash
# As admin:
/islandselector admin info Jeb
```

**Expected Output:**
```
=== Player Info: Jeb ===
Grid Location: 5,5
World Coordinates: 2500, 2500
Active Slot: #1 - Main Base
Slots: 2/5 with islands
--- Slot Details ---
  #1: Main Base - ACTIVE - Has Island
  #2: Farm Island - inactive - Has Island
  #3: Slot 3 - inactive - Empty
  #4: Slot 4 - inactive - Empty
  #5: Slot 5 - inactive - Empty
Switch Cooldown: None
```

**Verification:**
- ✅ Custom slot names are displayed
- ✅ Slot count shows 2/5
- ✅ Both slots show "Has Island"
- ✅ Only active slot marked as "ACTIVE"

---

#### 3. Test with player who has active cooldown

**Setup:**
```bash
# As player "Steve":
/island create
# Create island in slot 1

# Switch to another slot (if slot switching is working)
# This should trigger cooldown

# OR as admin, manually set cooldown:
# (Requires modifying database or waiting for natural cooldown)
```

**Test:**
```bash
# As admin:
/islandselector admin info Steve
```

**Expected Output:**
```
=== Player Info: Steve ===
Grid Location: -3,7
World Coordinates: -1500, 3500
Active Slot: #2 - Slot 2
Slots: 1/5 with islands
--- Slot Details ---
  #1: Slot 1 - inactive - Has Island
  #2: Slot 2 - ACTIVE - Empty
  ... (etc)
Switch Cooldown: 6d 23h remaining
```

**Verification:**
- ✅ Cooldown time displayed in human-readable format
- ✅ Format: days/hours/minutes/seconds as appropriate
- ✅ Shows "remaining" indicator

---

#### 4. Test with player who has never created an island

**Setup:**
```bash
# Create a new player account (e.g., "NewPlayer")
# Join server but do NOT create an island
```

**Test:**
```bash
# As admin:
/islandselector admin info NewPlayer
```

**Expected Output:**
```
Player 'NewPlayer' has no IslandSelector data
```

**Verification:**
- ✅ Clear message that player has no data
- ✅ No error or stack trace
- ✅ Command completes successfully

---

#### 5. Test with player name that doesn't exist

**Test:**
```bash
# As admin:
/islandselector admin info FakePlayerXYZ123
```

**Expected Output:**
```
Player 'FakePlayerXYZ123' has no IslandSelector data
```

**Note:** Bukkit's `getOfflinePlayer()` always returns a player object, so this will show "no data" rather than "not found".

**Verification:**
- ✅ No crash or error
- ✅ Appropriate message displayed

---

#### 6. Test original coordinate lookup still works

**Setup:**
```bash
# Have player "Notch" with island at 0,0
```

**Test:**
```bash
# As admin:
/islandselector admin info 0,0
```

**Expected Output:**
```
=== Location 0,0 ===
Status: OCCUPIED
Owner: Notch
World coordinates: 0, 0
```

**Verification:**
- ✅ Original coordinate lookup functionality preserved
- ✅ Shows location status
- ✅ Shows owner if occupied
- ✅ Shows world coordinates

---

#### 7. Test command detects coordinate vs player name correctly

**Edge Cases:**

**Test 7a: Number-like player name**
```bash
# If a player is named "123", test:
/islandselector admin info 123
# Should try to parse as coord first, fail, then treat as player name
```

**Test 7b: Coordinate format**
```bash
/islandselector admin info 5,10
# Should recognize as coordinate and show location info
```

**Test 7c: Player name with comma (unlikely but possible)**
```bash
# Minecraft doesn't allow commas in usernames, so this shouldn't occur
```

**Verification:**
- ✅ Command correctly differentiates coordinates from player names
- ✅ Coordinates (format: X,Z) are parsed first
- ✅ Anything that doesn't parse as coordinate is treated as player name

---

#### 8. Test with offline player

**Setup:**
```bash
# Have player "OfflineGuy" create an island
# Player logs out
```

**Test:**
```bash
# As admin (while player is offline):
/islandselector admin info OfflineGuy
```

**Expected Output:**
```
=== Player Info: OfflineGuy ===
Grid Location: 8,-2
World Coordinates: 4000, -1000
Active Slot: #1 - Slot 1
Slots: 1/5 with islands
--- Slot Details ---
  #1: Slot 1 - ACTIVE - Has Island
  ... (etc)
Switch Cooldown: None
```

**Verification:**
- ✅ Works for offline players
- ✅ Shows same info as online players
- ✅ Uses cached/database data

---

## Testing Time Estimates

- **Quick smoke test:** 5 minutes (Tests #1, #6)
- **Comprehensive test:** 20 minutes (All tests)
- **Edge case testing:** 10 minutes (Tests #4-5, #7)
- **Total:** ~35 minutes

---

## Integration Testing

### Test with existing admin commands

```bash
# Verify all admin commands still work:
/islandselector admin version
/islandselector admin reload
/islandselector admin reserve 0,0
/islandselector admin unreserve 0,0
/islandselector admin setprice 1,1 10000
/islandselector admin clearprice 1,1
/islandselector admin expand east 5
/islandselector admin shrink east 5
/islandselector admin info 0,0      # Coordinate lookup
/islandselector admin info Notch    # NEW: Player lookup
```

**Verification:**
- ✅ All existing commands work
- ✅ Info command works for both coordinates and players
- ✅ No conflicts or regressions

---

## Expected Issues / Notes

### Known Limitations

1. **Player name parsing:** If a player has a name that looks like a coordinate (e.g., "5,10"), the command will interpret it as a coordinate first. This is extremely unlikely in Minecraft.

2. **Offline player lookup:** Uses Bukkit's `getOfflinePlayer()` which is deprecated but necessary for offline lookups. Works correctly but may show deprecation warning in code.

3. **No island data:** Players who have never used IslandSelector will show "no data" message. This is expected behavior.

4. **Cooldown format:** Cooldown display is approximate and formatted for readability (e.g., "6d 23h" instead of "604800000ms").

---

## Success Criteria

Test #87 passes if:

1. ✅ Command accepts player names as argument
2. ✅ Shows player's grid location correctly
3. ✅ Shows number of slots and active slot
4. ✅ Shows slot names (custom or default)
5. ✅ Shows slot status (active/inactive, has island/empty)
6. ✅ Shows cooldown status with human-readable time
7. ✅ Works for online and offline players
8. ✅ Handles players with no data gracefully
9. ✅ Original coordinate lookup still works
10. ✅ No errors or crashes

---

## Feature Benefits

**For Server Admins:**
- Quickly check player island status without joining as them
- Diagnose slot-related issues
- Verify cooldowns before resetting them
- Audit player data
- Support tickets easier to resolve

**For Debugging:**
- Comprehensive player state view
- All relevant data in one command
- Helps identify slot switching issues
- Cooldown status immediately visible

**For Server Management:**
- Monitor player engagement (how many slots used)
- Identify inactive players
- Plan grid expansion based on player distribution
- Track premium location sales

---

## Command Reference

### Syntax
```bash
/islandselector admin info <coord|player>
```

### Examples
```bash
# Look up location:
/islandselector admin info 0,0
/islandselector admin info -5,10
/islandselector admin info A1

# Look up player:
/islandselector admin info Notch
/islandselector admin info Steve
/islandselector admin info jeb_
```

### Permission
```yaml
islandselector.admin.info: true
```

---

**Thank you for testing!** 🎮

This enhancement makes the admin info command significantly more useful for server management and player support.
