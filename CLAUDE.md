# IslandSelector - BentoBox Addon

## Project Overview
This is a BentoBox addon for BSkyBlock that provides a grid-based island selection system. Players can visually select their island location from a GUI showing a map of all islands.

## Development Context
This project is developed using a hybrid approach:
- **Autonomous agents** run via `autonomous_agent_demo.py` for feature implementation
- **Manual Claude Code sessions** for debugging and testing with a human tester

Both session types MUST maintain the shared progress file.

## Critical: Progress Tracking

### Progress File: `claude-progress.txt`
This file tracks ALL work done across sessions. It is the shared memory between autonomous agents and manual debugging sessions.

**At the START of every session:**
1. Read `claude-progress.txt` to understand previous work
2. Check the "NEXT STEPS" section for priorities
3. Review any "KNOWN ISSUES" that need fixing

**At the END of every session:**
1. Update `claude-progress.txt` with a new session entry
2. Document: what was fixed, what was implemented, what issues remain
3. Update "NEXT STEPS" for the next session
4. Include "KEY LEARNINGS" for any gotchas discovered

### Session Entry Format
```
=================================================
SESSION N - [TYPE] ([Date or Context])
=================================================

Session Date: Session N
Agent Role: [Bug Fixes / Feature Implementation / etc.]

=================================================
COMPLETED TASKS - SESSION N
=================================================

✅ 1. TASK NAME
   - Issue: [What was wrong]
   - Fix: [What was done]
   - File: [Files modified]

=================================================
KNOWN ISSUES
=================================================

⚠️ ISSUE NAME
   - Description
   - Status
   - Next steps

=================================================
NEXT STEPS FOR FUTURE AGENTS
=================================================

PRIORITY 1: [Task]
  □ Step 1
  □ Step 2
```

## Build Commands
```bash
# Build the addon
mvn clean package -q

# Output JAR location
target/IslandSelector-1.0.0.jar
```

## Project Structure
```
src/main/java/world/bentobox/islandselector/
├── IslandSelector.java      # Main addon class
├── Settings.java            # Configuration (implements ConfigObject!)
├── commands/                # Player and admin commands
├── database/                # BentoBox database models
├── gui/                     # Inventory GUIs
├── managers/                # GridManager for island tracking
├── models/                  # Data models
└── utils/                   # GridCoordinate utilities
```

## Key Technical Notes

### BentoBox Integration
- Settings class MUST implement `ConfigObject` interface
- Don't list BentoBox in addon.yml depend (it's implicit)
- Use BentoBox database abstraction with @Table annotations
- island.getUniqueId() returns String, not UUID

### Grid Coordinate System
- Grid is centered at (0,0), supports negative coordinates
- Format: "X,Z" (e.g., "0,0", "-5,3", "10,-2")
- World-to-grid: `gridCoord = round(worldCoord / spacing)`
- Default spacing: 500 blocks

### GUI Best Practices
- Always call `getLocationStatus()` before `getGridLocation()` (registers islands)
- Use `player.updateInventory()` for in-place updates, don't close/reopen
- Use `SkullMeta` for player heads, not just `ItemMeta`
- Register listeners per-instance, unregister on close

## Testing
After building, copy JAR to `plugins/BentoBox/addons/` on test server.

Key commands:
- `/islandselector` - Open grid GUI
- `/islandselector slots` - Open slot GUI
- `/islandselector admin reload` - Reload config
- `/islandselector admin info 0,0` - Show location info

## Current Status
See `claude-progress.txt` for detailed session history and current priorities.
