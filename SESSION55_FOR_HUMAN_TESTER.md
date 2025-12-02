# Session 55 - Testing Guide for Human Tester

## Build Information

**Build Status:** BUILD SUCCESS
**JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
**JAR Size:** 216K
**Source Files:** 54 Java files

## Session 55 Summary

This session performed a comprehensive review of the codebase to verify implementation completeness.

### Key Findings:

1. **All Core Features Are Implemented**
   - Grid GUI with scrolling, filtering, search
   - Slot management (create, switch, delete, rename, change icon)
   - Island claiming and relocation
   - Neighborhood GUI with neighbor warping
   - Admin commands (reserve, unreserve, setprice, clearprice, expand, shrink, purge, etc.)
   - PlaceholderAPI integration (5 placeholders)
   - Tab completion for all commands
   - Custom events (GridLocationClaimEvent, SlotSwitchEvent, IslandRelocateEvent)

2. **Features Verified This Session:**
   - AdminVersionCommand - shows all dependency versions correctly
   - PlaceholderAPIIntegration - all 5 placeholders implemented
   - AdminUnreserveCommand - properly unreserves locations
   - AdminClearPriceCommand - removes price from reserved locations
   - All admin subcommands registered correctly

---

## Current Test Status

| Category | Passing | Pending Verification | Total |
|----------|---------|---------------------|-------|
| Verified | 44 | - | 44 |
| Sessions 38-54 | - | ~150 | ~150 |
| Edge Cases/Performance | - | ~31 | ~31 |
| **Total** | 44 | **181** | **225** |

---

## Priority Tests for Verification

### HIGH PRIORITY - Core Functionality (Tests 27-60)

#### Search Function (Tests 27-30)
1. **Test 27**: Search button displays with compass item
2. **Test 28**: Search finds player and centers grid on their island
3. **Test 29**: Search handles partial name matching
4. **Test 30**: Search shows error for player not found

**Testing Steps:**
1. Open grid GUI with `/islandselector`
2. Click the compass (Search Player) button in bottom row
3. GUI closes and chat prompts for player name
4. Type a player name (or partial name)
5. Grid reopens centered on that player's island

#### Premium Locations (Tests 33-34)
1. **Test 33**: Hover over premium location shows price
2. **Test 34**: New player sees grid in claiming mode

#### Island Claiming (Tests 35-42)
These tests verify the full island creation flow when a player selects a grid location.

### MEDIUM PRIORITY - Slot Management (Tests 43-85)

#### Slot GUI Tests (Tests 43-50)
1. Slot Selection GUI layout (27 slots, 3 rows)
2. Active slot shows enchantment glow
3. Inactive slots show switch prompt
4. Empty slots show create prompt
5. Locked slots show barrier

#### Slot Operations (Tests 51-70)
1. Click empty slot starts island creation
2. Click inactive slot opens switch confirmation
3. Slot switch saves current island as schematic
4. Slot switch loads target slot schematic
5. Slot switch cooldown enforcement

### LOWER PRIORITY - Admin Commands (Tests 127-165)

#### Admin Commands Already Tested:
- `/islandselector admin reserve <coord>` - PASSING
- `/islandselector admin setprice <coord> <price>` - PASSING
- `/islandselector admin reload` - PASSING
- `/islandselector admin info <coord>` - PASSING

#### Admin Commands Pending:
- `/islandselector admin unreserve <coord>`
- `/islandselector admin clearprice <coord>`
- `/islandselector admin expand <direction> <amount>`
- `/islandselector admin shrink <direction> <amount>`
- `/islandselector admin relocate <player> <coord>`
- `/islandselector admin purge <player|inactive|banned>`
- `/islandselector admin resetcooldown <player> <switch|relocate|all>`
- `/islandselector admin backup <player> <slot>`
- `/islandselector admin restore <player> <slot>`
- `/islandselector admin version`

---

## Quick Command Reference

```bash
# Player Commands
/islandselector              - Open grid GUI
/islandselector slots        - Open slot selection GUI
/islandselector locate       - Show your grid coordinates
/islandselector find <player> - Find and center on player's island
/islandselector neighbors    - Open neighborhood GUI
/islandselector help         - Show all commands
/islandselector switch <slot> - Switch to specific slot
/islandselector delete <slot> - Delete a slot
/islandselector setname <slot> <name> - Rename a slot
/islandselector seticon <slot> - Set slot icon to held item

# Admin Commands
/islandselector admin version   - Show version info
/islandselector admin reload    - Reload configuration
/islandselector admin info <coord|player> - Show location/player info
/islandselector admin reserve <coord> - Reserve location
/islandselector admin unreserve <coord> - Unreserve location
/islandselector admin setprice <coord> <price> - Set premium price
/islandselector admin clearprice <coord> - Remove price (make blocked)
/islandselector admin expand <direction> <amount> - Expand grid
/islandselector admin shrink <direction> <amount> - Shrink grid
/islandselector admin relocate <player> <coord> - Force relocate
/islandselector admin purge <player|inactive|banned> - Purge data
/islandselector admin resetcooldown <player> <type> - Reset cooldowns
/islandselector admin backup <player> <slot> - Force backup
/islandselector admin restore <player> <slot> - Restore from backup
```

---

## PlaceholderAPI Placeholders

If PlaceholderAPI is installed, these placeholders are available:

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%islandselector_location%` | Grid coordinates | `Q17` or `None` |
| `%islandselector_slot_active_name%` | Active slot name | `Main Base` or `Slot 1` |
| `%islandselector_cooldown_switch%` | Switch cooldown | `45m 30s` or `Ready` |
| `%islandselector_available%` | Available locations count | `150` |
| `%islandselector_neighbors_online%` | Online neighbors count | `3` |

---

## Testing Time Estimate

- **Quick Verification (High Priority):** 30-45 minutes
- **Full Test Suite:** 2-3 hours

---

## Previous Sessions Pending Verification

All tests from the following sessions are implemented and ready:
- Session 38: Island Claiming Flow
- Session 39: Slot Management GUI
- Session 40: Slot Operations (switch, save, load)
- Session 41: Island Relocation
- Session 42: Neighbor Warp/Visit
- Session 43: Player Commands (locate, find, help)
- Session 44: Neighborhood GUI Enhancements
- Session 45: Tab Completion
- Session 46: PlaceholderAPI Integration
- Session 47: Custom Event API
- Session 48: Admin Purge Commands
- Session 49: Admin Force Relocate
- Session 50-51: Player Search (via GUI and command)
- Session 52: Comprehensive Documentation
- Session 53: /islandselector switch command
- Session 54: Find command centering, Island visit functionality

---

## Notes

- The build compiles successfully with no errors
- Only deprecation warnings in SlotSwitchManager.java (minor, cosmetic)
- All 54 source files compile cleanly
- Features are complete; testing is the bottleneck to marking tests as passing
