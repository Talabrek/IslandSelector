# IslandSelector - BentoBox Addon

## Project Overview
This is a BentoBox addon for BSkyBlock that provides:
- Grid-based island selection system
- Multi-slot island management (up to 5 islands per player)
- Multi-dimension support (overworld, nether, end)

Players visually select their island location from a GUI showing a map of all islands.

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
│   ├── GridLocationData.java
│   └── SlotData.java
├── gui/                     # Inventory GUIs
│   ├── MainGridGUI.java
│   ├── SlotSelectionGUI.java
│   ├── NeighborhoodGUI.java
│   └── IslandRestoreGUI.java
├── integrations/            # External plugin integrations
│   ├── WorldEditIntegration.java
│   ├── NovaIntegration.java
│   └── PlaceholderAPIIntegration.java
├── listeners/               # Event listeners
│   └── IslandCreateListener.java
├── managers/                # Core business logic
│   ├── GridManager.java
│   ├── SlotManager.java
│   ├── SlotSwitchManager.java
│   ├── BackupManager.java
│   ├── RelocationManager.java
│   ├── DimensionManager.java
│   ├── MultiDimensionIslandCreator.java
│   ├── ChallengesIntegration.java
│   └── LevelIntegration.java
├── models/                  # Data models
│   ├── GridLocation.java
│   └── DimensionConfig.java
└── utils/                   # Utility classes
    ├── GridCoordinate.java
    ├── SchematicUtils.java
    └── EntityStorage.java
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
- Island spacing is detected from BSkyBlock config

### Multi-Dimension Support
- When enabled, islands exist in all configured dimensions simultaneously
- All dimensions share the same grid coordinate
- Slot switching saves/loads ALL dimensions
- Relocation moves islands in ALL dimensions

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
- `/islandselector neighbors` - Open neighborhood view
- `/islandselector admin reload` - Reload config
- `/islandselector admin info 0,0` - Show location info
- `/islandselector admin remove <player>` - Remove player's island

## Documentation
- `README.md` - Full documentation for users
- `DEVELOPMENT_NOTES.md` - Technical patterns and gotchas for developers
- `app_spec.txt` - Original project specification

<use_parallel_tool_calls>
If you intend to call multiple tools and there are no dependencies between the tool calls, make all of the independent tool calls in parallel. Prioritize calling tools simultaneously whenever the actions can be done in parallel rather than sequentially. For example, when reading 3 files, run 3 tool calls in parallel to read all 3 files into context at the same time. Maximize use of parallel tool calls where possible to increase speed and efficiency. However, if some tool calls depend on previous calls to inform dependent values like the parameters, do NOT call these tools in parallel and instead call them sequentially. Never use placeholders or guess missing parameters in tool calls.
</use_parallel_tool_calls>

<investigate_before_answering>
Never speculate about code you have not opened. If the user references a specific file, you MUST read the file before answering. Make sure to investigate and read relevant files BEFORE answering questions about the codebase. Never make any claims about code before investigating unless you are certain of the correct answer - give grounded and hallucination-free answers.
</investigate_before_answering>

<default_to_action>
By default, implement changes rather than only suggesting them. If the user's intent is unclear, infer the most useful likely action and proceed, using tools to discover any missing details instead of guessing. Try to infer the user's intent about whether a tool call (e.g., file edit or read) is intended or not, and act accordingly.
</default_to_action>

<balance_verbosity>
After completing a task that involves tool use, provide a quick summary of the work you've done
</balance_verbosity>