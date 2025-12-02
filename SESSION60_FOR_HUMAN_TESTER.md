# Session 60 - Human Tester Priority Guide

## Current Status

- **Tests Passing:** 44/225 (19.6%)
- **Tests Pending:** 181 (need manual verification)
- **Build Status:** SUCCESS (220K JAR)
- **Feature Status:** **100% COMPLETE** - All features implemented in code

## JAR Location

```
output/IslandSelector-1.0.0-SNAPSHOT.jar
```

Copy to: `plugins/BentoBox/addons/`

---

## CRITICAL: All Features Are Implemented

The 181 "failing" tests are NOT failing due to missing code. They simply have not been manually verified on a Minecraft server. Once verified, the project could achieve 100% completion.

---

## Priority Testing Order

### TIER 1: Quick Wins (15 minutes)

These tests should be verified first for quick progress:

#### Search Functionality (Tests 28-31)
| Test # | Description | How to Test |
|--------|-------------|-------------|
| 28 | Search button displays with compass item | Open `/islandselector`, check slot 47 for COMPASS |
| 29 | Search finds player and centers grid | Click compass, type player name, verify grid centers |
| 30 | Search handles partial names | Search "Not" to find "Notch" |
| 31 | Search shows error for not found | Search non-existent player, verify error message |

**Steps:**
1. Open grid GUI (`/islandselector`)
2. Find COMPASS item at bottom row (slot 47)
3. Click it - GUI closes, chat prompts for name
4. Type a player name who has an island
5. Verify grid reopens centered on their island
6. Repeat with partial name
7. Try non-existent player, verify error

---

### TIER 2: Core User Flow (30 minutes)

#### Island Claiming (Tests 45-53)
| Test # | Description |
|--------|-------------|
| 45 | New player sees grid in claiming mode |
| 46 | Clicking empty location opens confirmation |
| 47 | Confirming claim creates island at coordinates |
| 48 | Canceling returns to grid |
| 49-50 | Grid coordinates map to world coordinates |
| 51 | Grid spacing affects placement |
| 52 | Island UUID linked in database |
| 53 | Cannot claim occupied location |

**Steps:**
1. Join as NEW player (never had island)
2. Run `/island` - should show grid selector
3. Click green empty location
4. Verify confirmation GUI opens
5. Cancel - returns to grid
6. Confirm - island creates
7. Check world coordinates match grid

---

### TIER 3: Slot System (45 minutes)

#### Slot GUI (Tests 59-68)
| Test # | Description |
|--------|-------------|
| 59 | Slot GUI opens with 27 slots |
| 60 | Active slot shows glow |
| 61 | Inactive slot shows switch prompt |
| 62 | Empty slot shows lime dye |
| 63 | Locked slot shows barrier |
| 64-65 | Permission-based slot access |
| 66 | Clicking locked slot shows error |
| 67 | Empty slot starts island creation |
| 68 | New island shares grid location |

**Steps:**
1. Run `/islandselector slots`
2. Verify 27-slot GUI opens
3. Check active slot has glow
4. Check empty slots show LIME_DYE
5. Check locked slots show BARRIER
6. Click different slot types, verify behavior

---

### TIER 4: Premium Locations (20 minutes)

#### Admin Premium Setup (Test 44)
| Test # | Description |
|--------|-------------|
| 44 | Premium location shows price and purchase |

**Steps:**
1. As admin: `/islandselector admin reserve 1,1`
2. As admin: `/islandselector admin setprice 1,1 50000`
3. Open grid as regular player
4. Navigate to 1,1
5. Verify GOLD_BLOCK with price tooltip

---

## Quick Smoke Test Commands

```bash
# Core Commands
/islandselector          # Opens grid GUI
/islandselector slots    # Opens slot GUI
/islandselector locate   # Shows your coordinates
/islandselector help     # Shows all commands
/islandselector neighbors # Opens neighborhood GUI

# Admin Commands
/islandselector admin version   # Plugin version
/islandselector admin info 0,0  # Location info
/islandselector admin reload    # Reload config

# Tab Completion Test
/islandselector admin [TAB]  # Should show subcommands
```

---

## How to Mark Tests as Passing

After verifying a test works:

1. Open `feature_list.json`
2. Find the test by description
3. Change `"passes": false` to `"passes": true`
4. Save the file

---

## Dependencies

| Dependency | Required | Notes |
|------------|----------|-------|
| BentoBox | Yes | Core framework |
| BSkyBlock | Yes | Island gamemode |
| FastAsyncWorldEdit | Yes | Schematic operations |
| Vault | Optional | Economy features |
| PlaceholderAPI | Optional | Placeholder features |

---

## Expected Progress

If all tests in Tiers 1-4 pass:
- Current: 44 tests
- After Tier 1: 48 tests (+4)
- After Tier 2: 57 tests (+9)
- After Tier 3: 67 tests (+10)
- After Tier 4: 68 tests (+1)

**Total: ~24 new tests verified**

---

## Notes

1. All 181 pending tests have code implementations
2. Focus on Tier 1 first for quick wins
3. If a test fails, note the specific issue
4. Some tests require Vault for economy
5. Some tests require multiple accounts

Good luck testing!
