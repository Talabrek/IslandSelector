---
phase: 10-relocation-safety
plan: 01
subsystem: gameplay
tags: [relocation, teleport, bskyblock, safety, location-check]

# Dependency graph
requires:
  - phase: 01-grid-foundation
    provides: "Grid coordinate system and island management"
provides:
  - "Location-based teleport filtering for relocation"
  - "RELC-01: Players on island teleported to new location"
  - "RELC-02: Players elsewhere not affected by relocation"
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "island.onIsland(player.getLocation()) check before teleport"
    - "Capture on-island state before async operations"
    - "Pass flags through async callback chains"

key-files:
  created: []
  modified:
    - "src/main/java/world/bentobox/islandselector/managers/RelocationManager.java"

key-decisions:
  - "Check player location at start, before any async operations"
  - "Team members checked at teleport time (may have moved during async)"
  - "Owner checked at start (captured in flag for duration)"

patterns-established:
  - "Location safety pattern: Always check onIsland before teleporting during relocation"
  - "Flag passing pattern: Capture state before async, pass through chain"

# Metrics
duration: 4min
completed: 2026-01-20
---

# Phase 10 Plan 1: Relocation Safety Summary

**Location-based teleport filtering for relocation - only teleports players actually on the island being relocated**

## Performance

- **Duration:** 4 min
- **Started:** 2026-01-20T15:15:00Z
- **Completed:** 2026-01-20T15:19:50Z
- **Tasks:** 3
- **Files modified:** 1

## Accomplishments
- Added `island.onIsland(player.getLocation())` check to player relocation flow
- Added location check for team members during teleportPlayersSafely
- Added location check to admin relocation flow
- Players at spawn or on other islands are no longer unexpectedly teleported

## Task Commits

Each task was committed atomically:

1. **Task 1: Add location check to player relocation flow** - `45c4417` (feat)
2. **Task 2: Add location check for owner and team members** - `cb1e9a0` (feat)
3. **Task 3: Add location check to admin relocation flow** - `c78d8f3` (feat)

## Files Created/Modified
- `src/main/java/world/bentobox/islandselector/managers/RelocationManager.java` - Added onIsland checks at 3 key points

## Decisions Made
- **Owner check timing:** Capture `playerIsOnIsland` flag at the start of relocation, before any async operations, to avoid race conditions
- **Team member check timing:** Check team members' locations at teleport time (inside the delayed task) since they may have moved during the async island copy/paste operations
- **Admin relocation:** Same pattern as player relocation - capture state before async, pass flag through chain

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - all three tasks completed successfully with no compilation errors.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Relocation safety fix complete
- v1.1.1 milestone complete (all 3 phases: 08, 09, 10)
- Ready for testing on server

### Test Scenarios (documented for server testing)
| Scenario | Expected Behavior |
|----------|-------------------|
| Player on island -> relocates | Teleported to new location |
| Player at spawn -> relocates | Stays at spawn (RELC-02) |
| Team member on island -> owner relocates | Teleported to new location |
| Team member at spawn -> owner relocates | Stays at spawn (RELC-02) |
| Admin relocates player on island | Player teleported to new location |
| Admin relocates player at spawn | Player stays at spawn (RELC-02) |

---
*Phase: 10-relocation-safety*
*Completed: 2026-01-20*
