# Session 54 - Testing Guide for Human Tester

## Build Information

**Build Status:** BUILD SUCCESS
**JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
**JAR Size:** 216K
**Source Files:** 54 Java files

## Features Implemented/Fixed in This Session

### 1. FindCommand - Now Properly Centers Viewport

**Previous Behavior:** `/islandselector find <player>` opened the grid GUI but did NOT center on the target player's island.

**New Behavior:** The command now properly centers the viewport on the found player's island location.

**Command:** `/islandselector find <player>` or `/is find <player>`

### 2. Island Visit Functionality - Fully Implemented

**Previous Behavior:** Right-clicking an occupied island in the grid GUI showed "Visiting islands coming soon...".

**New Behavior:** Right-clicking now teleports the player to that island!

**Features Implemented:**
- Right-click your own island: Teleports to your island home
- Right-click another player's island: Teleports to their island spawn or center
- Ban check: Players banned from an island cannot teleport there
- GUI auto-closes before teleport for clean UX

---

## Test Cases

### Test 1: Find Command Centers on Target Island
**Steps:**
1. Have two players (Player1 and Player2) with islands
2. Player1 opens grid GUI with `/islandselector`
3. Player1 runs `/islandselector find Player2`
4. Verify: Grid GUI opens
5. Verify: Viewport is centered on Player2's island
6. Verify: Player2's island is visible in the center of the grid

**Expected Result:** The grid GUI should show Player2's island in or near the center of the viewport.

### Test 2: Find Command with Partial Name
**Steps:**
1. Have a player named "TestPlayer123" with an island
2. Run `/islandselector find Test`
3. Verify: Finds TestPlayer123 and centers on their island

**Expected Result:** Partial name matching works, grid centers on found island.

### Test 3: Find Command - Player Not Found
**Steps:**
1. Run `/islandselector find NonExistentPlayer`
2. Verify: Error message "Player 'NonExistentPlayer' not found or has no island"

**Expected Result:** Clear error message when player doesn't exist.

### Test 4: Find Command - Player Has No Island
**Steps:**
1. Have a player who has never created an island
2. Run `/islandselector find <that player>`
3. Verify: Error message about player not having an island

**Expected Result:** Clear error message when player has no island.

---

### Test 5: Visit Own Island (Right-Click)
**Steps:**
1. Open grid GUI with `/islandselector`
2. Find your own island in the grid (with enchantment glow)
3. Right-click your island
4. Verify: GUI closes
5. Verify: Message "Teleporting to your island..."
6. Verify: Player is teleported to their island home

**Expected Result:** Player teleports to their own island.

### Test 6: Visit Another Player's Island (Right-Click)
**Steps:**
1. Player1 and Player2 both have islands
2. Player1 opens grid GUI
3. Player1 finds Player2's island in the grid
4. Player1 right-clicks Player2's island
5. Verify: GUI closes
6. Verify: Message "Teleporting to Player2's island..."
7. Verify: Player1 is teleported to Player2's island

**Expected Result:** Player can visit other islands by right-clicking.

### Test 7: Visit Banned Player's Island
**Steps:**
1. Player2 bans Player1 from their island (using BSkyBlock command)
2. Player1 opens grid GUI
3. Player1 right-clicks Player2's island
4. Verify: Error message "You are banned from this island!"
5. Verify: Player1 is NOT teleported

**Expected Result:** Banned players cannot visit island.

### Test 8: Left-Click Shows Island Info (NOT Teleport)
**Steps:**
1. Open grid GUI
2. Left-click another player's island
3. Verify: Message shows island info (owner name, online status)
4. Verify: Player is NOT teleported

**Expected Result:** Left-click shows info, right-click visits.

---

## Quick Command Reference

```
# Commands tested this session
/islandselector find <player>  - Find and center on player's island
/is find <player>              - Shorthand version

# Related existing commands
/islandselector                - Open grid GUI (right-click to visit)
/islandselector locate         - Show your own coordinates
/islandselector neighbors      - Open neighborhood GUI
```

---

## Testing Time

**Estimated:** 15-20 minutes
**Players Required:** 2 players with islands (for visit testing)

## Notes

- The visit functionality uses BSkyBlock's spawn point if set, otherwise teleports to island center
- The find command now properly uses `centerViewportOn()` method
- Both features integrate with BentoBox's IslandsManager for proper island data access

---

## Previous Sessions Still Pending Testing

If not yet tested, the following sessions also need verification:
- Session 53: /islandselector switch command
- Session 51: /islandselector neighbors command
- Session 45-50: Various commands and features

See SESSION52_FOR_HUMAN_TESTER.md for the comprehensive testing guide covering Sessions 38-52.
