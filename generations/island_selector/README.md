# IslandSelector - BentoBox Addon for BSkyBlock

A comprehensive BentoBox addon that revolutionizes the Skyblock experience with grid-based island selection, multi-slot island management, and multi-dimension support.

## Overview

IslandSelector replaces BSkyBlock's traditional automatic spiral island placement with powerful features:

1. **Grid-Based Island Selection**: Players choose their island location from an interactive visual grid GUI, creating a sense of place and community.

2. **Multi-Slot Island System**: Like character slots in MMOs, players can maintain multiple separate islands in different "slots," switching between them seamlessly.

3. **Multi-Dimension Support**: Islands exist across multiple dimensions (overworld, nether, end) with synchronized grid coordinates.

## Key Features

- **Visual Grid Interface**: Interactive 54-slot chest GUI displaying all island locations
- **Multiple Island Slots**: Up to 5 separate islands per player (permission-based)
- **Seamless Slot Switching**: Switch between islands with automatic save/load via FAWE
- **Multi-Dimension Islands**: Synchronized islands across overworld, nether, and end
- **Island Relocation**: Move your entire island to a new grid location
- **Premium Locations**: Reservable locations with pricing for monetization
- **Neighborhood View**: 3x3 GUI showing your immediate neighbors
- **Advanced Search**: Filter and search features to find specific islands
- **Full Customization**: Configurable GUIs, messages, costs, and cooldowns
- **Automatic Backups**: Slot data backed up on every switch with configurable intervals
- **Rich Integration**: Vault, PlaceholderAPI, Level, Challenges, and Nova support
- **Custom Command Hooks**: Execute commands on relocation and slot switch

## Requirements

### Required Dependencies
- **Paper/Spigot**: 1.20.x - 1.21.x
- **Java**: 17+
- **BentoBox**: Latest version
- **BSkyBlock**: Latest version
- **FastAsyncWorldEdit (FAWE)**: Required for schematic operations

### Optional Dependencies
- **Vault**: For economy integration (relocation costs, premium locations)
- **PlaceholderAPI**: For placeholders in scoreboards/chat
- **Level Addon**: For BSkyBlock island level display
- **Challenges Addon**: For slot-specific challenge progress
- **Nova**: For custom block support in schematics

## Installation

1. Ensure all required dependencies are installed
2. Download IslandSelector JAR
3. Place in `plugins/BentoBox/addons/`
4. Restart server
5. Configure `addons/IslandSelector/config.yml` as needed
6. Reload with `/islandselector admin reload`

## Building from Source

```bash
# Clone the repository
git clone <repository-url>
cd IslandSelector

# Build with Maven
mvn clean package

# JAR will be in target/
```

## Player Commands

| Command | Description |
|---------|-------------|
| `/islandselector` | Open the grid selection GUI |
| `/islandselector slots` | Open slot selection GUI |
| `/islandselector locate` | Show your island's grid coordinates |
| `/islandselector neighbors` | Open neighborhood view GUI |
| `/islandselector find <player>` | Find and center on a player's island |
| `/islandselector setname <slot> <name>` | Rename a slot |
| `/islandselector seticon <slot>` | Set slot icon to held item |
| `/islandselector switch <slot>` | Switch to a different slot |
| `/islandselector delete <slot>` | Delete a slot |
| `/islandselector help` | Show command help |

**Aliases**: `/is`, `/isgrid`

## Admin Commands

| Command | Description |
|---------|-------------|
| `/islandselector admin reserve <coords>` | Reserve a grid location |
| `/islandselector admin unreserve <coords>` | Remove reservation |
| `/islandselector admin setprice <coords> <amount>` | Set premium location price |
| `/islandselector admin clearprice <coords>` | Clear location price |
| `/islandselector admin expand <direction> <amount>` | Expand grid boundaries |
| `/islandselector admin shrink <direction> <amount>` | Shrink grid boundaries |
| `/islandselector admin relocate <player> <coords>` | Force relocate island |
| `/islandselector admin remove <player>` | Remove island (preserves slot data) |
| `/islandselector admin restore <player> <slot> [file]` | Restore from backup |
| `/islandselector admin info <coords\|player>` | Show detailed information |
| `/islandselector admin purge <player>` | Purge player slot data |
| `/islandselector admin purge inactive <days>` | Purge inactive players |
| `/islandselector admin purge banned` | Purge banned players |
| `/islandselector admin backup <player> [slot]` | Force backup |
| `/islandselector admin resetcooldown <player>` | Reset cooldowns |
| `/islandselector admin reload` | Reload configuration |
| `/islandselector admin version` | Show version info |

## Permissions

### Player Permissions
- `islandselector.use` - Basic access (default: true)
- `islandselector.slots` - Use slot system (default: true)
- `islandselector.slots.2` - Access to 2 slots (default: true)
- `islandselector.slots.3` - Access to 3 slots (default: false)
- `islandselector.slots.4` - Access to 4 slots (default: false)
- `islandselector.slots.5` - Access to 5 slots (default: false)
- `islandselector.relocate` - Can relocate island (default: true)
- `islandselector.neighbors` - Use neighborhood view (default: true)
- `islandselector.purchase` - Can purchase premium locations (default: true)
- `islandselector.warp` - Can warp to islands from GUI (default: true)

### Bypass Permissions
- `islandselector.bypass.cooldown.switch` - Bypass slot switch cooldown
- `islandselector.bypass.cooldown.relocate` - Bypass relocation cooldown
- `islandselector.bypass.cost.relocate` - Bypass relocation cost
- `islandselector.bypass.cost.purchase` - Bypass premium location cost

### Admin Permissions
- `islandselector.admin` - All admin commands (parent permission)
- `islandselector.admin.reserve` - Manage reserved locations
- `islandselector.admin.expand` - Expand/shrink grid
- `islandselector.admin.relocate` - Force relocate islands
- `islandselector.admin.info` - View detailed info
- `islandselector.admin.purge` - Purge player data
- `islandselector.admin.backup` - Force backups
- `islandselector.admin.reload` - Reload config

## PlaceholderAPI Placeholders

| Placeholder | Description | Example |
|------------|-------------|---------|
| `%islandselector_location%` | Player's grid coordinates | "0,0", "-5,3" |
| `%islandselector_slot_active_name%` | Active slot name | "Island 1" |
| `%islandselector_cooldown_switch%` | Switch cooldown remaining | "45m 30s" or "Ready" |
| `%islandselector_available%` | Count of available locations | "125" |
| `%islandselector_neighbors_online%` | Online neighbor count (0-8) | "3" |

## Configuration

The plugin is highly configurable via `config.yml`. Key settings include:

### Grid Settings
- Grid boundaries (min/max X and Z coordinates)
- Island spacing (synced from BSkyBlock)

### Slot Settings
- Default and maximum slots per player
- Slot switch cooldown
- Default slot naming and icons

### Multi-Dimension Settings
- Enable/disable multi-dimension support
- Configure worlds for overworld, nether, end
- Primary dimension selection

### Relocation Settings
- Cooldown duration
- Cost (requires Vault)
- Currency display name

### Backup Settings
- Auto-backup on slot switch
- Auto-backup intervals
- Maximum backups per slot
- Backup on server shutdown

### Custom Commands
Execute custom commands on events:
```yaml
custom-commands:
  relocation:
    enabled: true
    commands:
      - "broadcast {player} relocated to {to_coord}!"
    scope: CONSOLE  # CONSOLE, PLAYER, or PLAYER_OP
  slot-switch:
    enabled: true
    commands:
      - "eco give {player} 100"
    scope: CONSOLE
```

### GUI Settings
- Item materials for different states
- Scroll speed and amounts
- Active slot glow effect

### Integration Toggles
- Vault economy
- PlaceholderAPI
- Level addon
- (Nova and Challenges are auto-detected)

See `config.yml` for complete documentation.

## How It Works

### Grid System
- Islands are placed on a configurable grid (default -10 to +10 on X and Z)
- Grid coordinates use numeric format: `X,Z` (e.g., "0,0", "-5,3", "10,-2")
- Grid coordinates map to world coordinates via island spacing
- Players choose their location instead of random assignment

### Multi-Dimension System
- When multi-dimension is enabled, islands exist in all configured dimensions
- All dimensions share the same grid coordinate (e.g., -1,0 in overworld = -1,0 in nether)
- Slot switching saves/loads all dimensions simultaneously
- Relocation moves islands in all dimensions

### Slot System
- Each player can have multiple island "slots" (like character slots)
- All slots share the same grid location
- Only one slot is active (loaded in world) at a time
- Switching slots:
  1. Current island saved as FAWE schematic
  2. Location cleared
  3. Target slot's schematic loaded
  4. Player teleported to new island
- Each slot has independent:
  - Blocks and entities
  - Island level
  - Team members
  - Warps and settings
  - Challenge progress (if Challenges addon installed)

### Schematic Storage
- Slot schematics: `addons/IslandSelector/slots/{player-uuid}/slot-{number}.schem`
- Multi-dimension: `addons/IslandSelector/slots/{player-uuid}/slot-{number}-{dimension}.schem`
- Backups: `addons/IslandSelector/backups/{player-uuid}/slot-{number}-{timestamp}.schem`

## Project Structure

```
src/main/java/world/bentobox/islandselector/
├── IslandSelector.java          # Main addon class
├── Settings.java                # Configuration
├── commands/                    # Player and admin commands
├── database/                    # Data models (GridLocationData, SlotData)
├── gui/                         # GUI panels
├── integrations/                # External plugin integrations
│   ├── PlaceholderAPIIntegration.java
│   ├── WorldEditIntegration.java
│   └── NovaIntegration.java
├── listeners/                   # Event listeners
├── managers/                    # Core business logic
│   ├── GridManager.java
│   ├── SlotManager.java
│   ├── SlotSwitchManager.java
│   ├── BackupManager.java
│   ├── RelocationManager.java
│   ├── DimensionManager.java
│   ├── ChallengesIntegration.java
│   └── LevelIntegration.java
├── models/                      # Data models
└── utils/                       # Utilities
```

## Development

### Contributing
1. Fork the repository
2. Create a feature branch
3. Implement your changes
4. Test thoroughly
5. Submit a pull request

## License

[Specify your license here]

## Bug Reports & Feature Requests

Please use the issue tracker on the repository.

When reporting bugs, include:
- Server version (Paper/Spigot, MC version)
- BentoBox version
- BSkyBlock version
- FAWE version
- IslandSelector version
- Steps to reproduce
- Error messages/logs

## Support

[Specify support channels - Discord, forums, etc.]

## Credits

- Built for BentoBox addon framework
- Uses FastAsyncWorldEdit for efficient schematic operations
- Integrates with BSkyBlock gamemode

---

**Made for the Skyblock community**
