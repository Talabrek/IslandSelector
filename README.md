# IslandSelector - BentoBox Addon for BSkyBlock

A comprehensive BentoBox addon that revolutionizes the Skyblock experience with grid-based island selection and multi-slot island management.

## 🌟 Overview

IslandSelector replaces BSkyBlock's traditional automatic spiral island placement with two powerful features:

1. **Grid-Based Island Selection**: Players choose their island location from an interactive visual grid GUI, creating a sense of place and community.

2. **Multi-Slot Island System**: Like character slots in MMOs, players can maintain multiple separate islands in different "slots," switching between them seamlessly.

## ✨ Key Features

- 📍 **Visual Grid Interface**: Interactive 54-slot chest GUI displaying all island locations
- 🎮 **Multiple Island Slots**: Up to 5 separate islands per player (permission-based)
- 🔄 **Seamless Slot Switching**: Switch between islands with automatic save/load via FAWE
- 🚀 **Island Relocation**: Move your entire island to a new grid location
- 💎 **Premium Locations**: Reservable locations with pricing for monetization
- 👥 **Neighborhood View**: 3x3 GUI showing your immediate neighbors
- 🔍 **Advanced Search**: Filter and search features to find specific islands
- 🎨 **Full Customization**: Configurable GUIs, messages, costs, and cooldowns
- 💾 **Automatic Backups**: Slot data backed up on every switch
- 🔌 **Rich Integration**: Vault, PlaceholderAPI, and BSkyBlock Level addon support

## 📋 Requirements

### Required Dependencies
- **Paper/Spigot**: 1.20.x - 1.21.x
- **Java**: 17+
- **BentoBox**: Latest version
- **BSkyBlock**: Latest version
- **FastAsyncWorldEdit (FAWE)**: Latest version

### Optional Dependencies
- **Vault**: For economy integration (relocation costs, premium locations)
- **PlaceholderAPI**: For placeholders in scoreboards/chat
- **Level Addon**: For BSkyBlock island level display

## 🚀 Installation

1. Ensure all required dependencies are installed
2. Download IslandSelector JAR
3. Place in `plugins/BentoBox/addons/`
4. Restart server
5. Configure `plugins/IslandSelector/config.yml` as needed
6. Reload with `/islandselector admin reload`

## 🔨 Building from Source

```bash
# Clone the repository
git clone <repository-url>
cd IslandSelector

# Build with the included script
./init.sh

# Or build manually with Maven
mvn clean package

# Or with Gradle
./gradlew clean build

# JAR will be in target/ or build/libs/
```

## 🎮 Player Commands

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

**Aliases**: `/is`, `/iselector`, `/gridpick`, `/gp`

## 👑 Admin Commands

| Command | Description |
|---------|-------------|
| `/islandselector admin reserve <coords>` | Reserve a grid location |
| `/islandselector admin unreserve <coords>` | Remove reservation |
| `/islandselector admin setprice <coords> <amount>` | Set premium location price |
| `/islandselector admin clearprice <coords>` | Clear location price |
| `/islandselector admin expand <direction> <amount>` | Expand grid boundaries |
| `/islandselector admin shrink <direction> <amount>` | Shrink grid boundaries |
| `/islandselector admin relocate <player> <coords>` | Force relocate island |
| `/islandselector admin info <coords\|player>` | Show detailed information |
| `/islandselector admin purge <player>` | Purge player slot data |
| `/islandselector admin purge inactive <days>` | Purge inactive players |
| `/islandselector admin purge banned` | Purge banned players |
| `/islandselector admin backup <player> [slot]` | Force backup |
| `/islandselector admin resetcooldown <player> <type>` | Reset cooldown |
| `/islandselector admin reload` | Reload configuration |
| `/islandselector admin version` | Show version info |

## 🔐 Permissions

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

## 🎨 PlaceholderAPI Placeholders

### Grid Placeholders
- `%islandselector_location%` - Player's island coordinates (e.g., "C7")
- `%islandselector_location_column%` - Column letter only
- `%islandselector_location_row%` - Row number only
- `%islandselector_available%` - Count of available locations
- `%islandselector_total%` - Total grid locations
- `%islandselector_occupied%` - Count of occupied locations
- `%islandselector_neighbors_online%` - Online neighbor count

### Slot Placeholders
- `%islandselector_slot_active%` - Active slot number
- `%islandselector_slot_active_name%` - Active slot name
- `%islandselector_slot_count%` - Unlocked slot count
- `%islandselector_slot_used%` - Slots with islands
- `%islandselector_slot_available%` - Empty unlocked slots

### Cooldown Placeholders
- `%islandselector_cooldown_switch%` - Switch cooldown remaining (formatted)
- `%islandselector_cooldown_switch_seconds%` - Switch cooldown in seconds
- `%islandselector_cooldown_relocate%` - Relocate cooldown remaining (formatted)
- `%islandselector_cooldown_relocate_seconds%` - Relocate cooldown in seconds

## ⚙️ Configuration

The plugin is highly configurable via `config.yml`. Key settings include:

- **Grid Settings**: Size, spacing, origin coordinates
- **Slot Settings**: Default slots, cooldowns, naming
- **Relocation Settings**: Costs, cooldowns, currency
- **Reserved Locations**: Blocked and premium locations
- **Backup Settings**: Auto-backup, retention limits
- **GUI Settings**: Item materials, colors, scroll speed
- **Visitor Handling**: Teleport delays, messages
- **Performance**: Cache duration, concurrent operations
- **Integration**: Enable/disable Vault, PlaceholderAPI, Level addon

See `config.yml` for complete documentation.

## 📖 How It Works

### Grid System
- Islands are placed on a configurable grid (default 20x20)
- Each grid position has alphanumeric coordinates (A1, B7, C12, etc.)
- Grid coordinates map to world coordinates via configurable formula
- Players choose their location instead of random assignment

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

### Schematic Storage
- Slot schematics: `plugins/IslandSelector/slots/{player-uuid}/slot-{number}.schem`
- Backups: `plugins/IslandSelector/backups/{player-uuid}/slot-{number}-{timestamp}.schem`

## 🧪 Testing

This plugin uses **manual testing** only (no automated tests for Minecraft plugins).

Test against `feature_list.json` which contains 200+ detailed test cases covering:
- Grid GUI functionality
- Slot management
- Island claiming and relocation
- Admin commands
- Permissions
- Integration with dependencies
- Edge cases and error handling

See `feature_list.json` for complete test specifications.

## 🤝 Development

### Project Structure
```
IslandSelector/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/yourname/islandselector/
│   │   │       ├── IslandSelector.java          (Main addon class)
│   │   │       ├── managers/                     (Grid, Slot, etc.)
│   │   │       ├── gui/                          (GUI panels)
│   │   │       ├── database/                     (Data models)
│   │   │       ├── commands/                     (Command handlers)
│   │   │       ├── listeners/                    (Event listeners)
│   │   │       └── utils/                        (Utilities)
│   │   └── resources/
│   │       ├── addon.yml                         (Addon metadata)
│   │       ├── config.yml                        (Default config)
│   │       └── locales/                          (Language files)
│   └── test/
├── feature_list.json                              (Test specifications)
├── init.sh                                        (Build script)
├── pom.xml or build.gradle                        (Build config)
└── README.md
```

### Contributing
1. Fork the repository
2. Create a feature branch
3. Implement your changes
4. Test against `feature_list.json`
5. Submit a pull request

## 📝 License

[Specify your license here]

## 🐛 Bug Reports & Feature Requests

Please use the issue tracker on the repository.

When reporting bugs, include:
- Server version (Paper/Spigot, MC version)
- BentoBox version
- BSkyBlock version
- FAWE version
- IslandSelector version
- Steps to reproduce
- Error messages/logs

## 💬 Support

[Specify support channels - Discord, forums, etc.]

## 🎯 Roadmap

See `app_spec.txt` section "future_considerations" for planned features.

## 🙏 Credits

- Built for BentoBox addon framework
- Uses FastAsyncWorldEdit for efficient schematic operations
- Integrates with BSkyBlock gamemode

---

**Made with ❤️ for the Skyblock community**
