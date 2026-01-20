# Architecture

**Analysis Date:** 2026-01-20

## Pattern Overview

**Overall:** Autonomous Agent Harness + Generated Minecraft Plugin

This repository contains TWO distinct systems:

1. **Python Agent Harness** - Orchestrates autonomous code generation using Claude SDK
2. **Java Minecraft Plugin** - Generated BentoBox addon for island management (in `generations/island_selector/`)

**Key Characteristics:**
- Two-agent pattern: Initializer agent (session 1) + Coding agents (sessions 2+)
- Defense-in-depth security with command allowlisting
- Human-in-the-loop testing workflow (manual Minecraft server testing)
- Progress tracked via `feature_list.json` with 200+ manual test cases
- Generated plugin follows BentoBox addon architecture with manager pattern

## Layers

### Python Agent Harness

**Entry Point Layer:**
- Purpose: CLI interface and session orchestration
- Location: `autonomous_agent_demo.py`
- Contains: Argument parsing, main loop, async runner
- Depends on: `agent.py`
- Used by: Human operators running autonomous development

**Session Layer:**
- Purpose: Individual agent session lifecycle management
- Location: `agent.py`
- Contains: `run_agent_session()`, `run_autonomous_agent()` - message streaming, tool output handling
- Depends on: `client.py`, `prompts.py`, `progress.py`
- Used by: Entry point layer

**Client Layer:**
- Purpose: Claude SDK client configuration with security settings
- Location: `client.py`
- Contains: `create_client()` - security permissions, sandbox config, hook registration
- Depends on: `security.py`, `claude_code_sdk`
- Used by: Session layer

**Security Layer:**
- Purpose: Bash command validation and allowlisting
- Location: `security.py`
- Contains: `bash_security_hook()`, `extract_commands()`, validators for pkill/chmod/init.sh
- Depends on: None (standalone)
- Used by: Client layer (registered as PreToolUse hook)

**Prompts Layer:**
- Purpose: Agent prompt templates for different session types
- Location: `prompts.py`, `prompts/`
- Contains: `get_initializer_prompt()`, `get_coding_prompt()`, `copy_spec_to_project()`
- Depends on: File system (prompts directory)
- Used by: Session layer

### Generated Minecraft Plugin (BentoBox Addon)

**Addon Core Layer:**
- Purpose: Plugin lifecycle, dependency management, integration initialization
- Location: `generations/island_selector/src/main/java/world/bentobox/islandselector/IslandSelector.java`
- Contains: `onLoad()`, `onEnable()`, `onDisable()`, manager initialization
- Depends on: BentoBox API, all managers
- Used by: Bukkit/Paper server

**Manager Layer:**
- Purpose: Core business logic and data management
- Location: `generations/island_selector/src/main/java/world/bentobox/islandselector/managers/`
- Contains:
  - `GridManager.java` - Grid state, coordinate mapping, BSkyBlock sync
  - `SlotManager.java` - Player slots, cooldowns, schematic paths
  - `SlotSwitchManager.java` - Slot switching orchestration
  - `BackupManager.java`, `AutoBackupManager.java` - Backup operations
  - `RelocationManager.java` - Island relocation logic
  - `DimensionManager.java` - Multi-dimension world management
  - `MultiDimensionIslandCreator.java` - Cross-dimension island creation
- Depends on: Database layer, BentoBox API, WorldEdit integration
- Used by: Commands, GUIs, Listeners

**Database Layer:**
- Purpose: Persistent data storage using BentoBox database abstraction
- Location: `generations/island_selector/src/main/java/world/bentobox/islandselector/database/`
- Contains:
  - `GridLocationData.java` - Grid location persistence
  - `SlotData.java` - Player slot persistence
  - `RelocationData.java` - Relocation tracking
- Depends on: BentoBox database API
- Used by: Manager layer

**Command Layer:**
- Purpose: Player and admin command handling
- Location: `generations/island_selector/src/main/java/world/bentobox/islandselector/commands/`
- Contains: 25+ command classes (Admin*, player commands)
- Depends on: Manager layer
- Used by: Addon core (registered on enable)

**GUI Layer:**
- Purpose: Inventory-based user interfaces
- Location: `generations/island_selector/src/main/java/world/bentobox/islandselector/gui/`
- Contains:
  - `MainGridGUI.java` - Primary grid selection interface
  - `SlotSelectionGUI.java` - Slot management interface
  - `NeighborhoodGUI.java` - 3x3 neighbor view
  - `AdminGridGUI.java` - Admin grid management
  - Various confirmation and settings GUIs
- Depends on: Manager layer, BentoBox Panel API
- Used by: Commands, other GUIs

**Listener Layer:**
- Purpose: Event handling for BentoBox and Bukkit events
- Location: `generations/island_selector/src/main/java/world/bentobox/islandselector/listeners/`
- Contains:
  - `IslandCreateListener.java` - Intercepts island creation for grid selection
  - `PlayerConnectionListener.java` - Blueprint permissions on join
  - `SearchListener.java` - Chat-based search handling
- Depends on: Manager layer, GUI layer
- Used by: Addon core (registered on enable)

**Integration Layer:**
- Purpose: External plugin integrations
- Location: `generations/island_selector/src/main/java/world/bentobox/islandselector/integrations/`
- Contains:
  - `WorldEditIntegration.java` - FAWE schematic operations
  - `PlaceholderAPIIntegration.java` - PAPI placeholders
  - `NovaIntegration.java` - Custom block support
- Depends on: Manager layer, external plugin APIs
- Used by: Manager layer

## Data Flow

### Agent Session Flow (Python):

1. `autonomous_agent_demo.py` parses CLI args, calls `run_autonomous_agent()`
2. Session loop creates fresh `ClaudeSDKClient` per iteration via `create_client()`
3. Security settings written to `.claude_settings.json` in project dir
4. Prompt selected: initializer (first run) or coding (continuation)
5. `client.query()` sends prompt, `client.receive_response()` streams results
6. Tool use blocks trigger `bash_security_hook()` for Bash commands
7. Progress tracked via `feature_list.json`, session notes in `claude-progress.txt`

### Island Claiming Flow (Plugin):

1. Player runs `/island create` (BSkyBlock command)
2. `IslandCreateListener` intercepts `IslandCreateEvent`
3. `MainGridGUI` opens showing available locations
4. Player clicks available slot, `IslandClaimGUI` confirms
5. `GridManager.occupyLocation()` marks grid position
6. BentoBox creates island at calculated world coordinates
7. `SlotManager.initializePlayerSlots()` creates slot 1
8. Grid and slot data persisted via BentoBox database

### Slot Switch Flow (Plugin):

1. Player selects slot in `SlotSelectionGUI`
2. `SlotSwitchManager` orchestrates the switch:
   - Teleports visitors away
   - Saves current island via FAWE schematic
   - Clears location
   - Loads target slot schematic
   - Updates slot activation state
3. `SlotManager` updates cooldowns and persistence
4. Player teleported to restored island

**State Management:**
- Python: Stateless sessions; state in filesystem (`feature_list.json`, git commits)
- Plugin: ConcurrentHashMap caches + BentoBox database for persistence

## Key Abstractions

**GridCoordinate (Plugin):**
- Purpose: Represents a position in the island grid
- Examples: `generations/island_selector/src/main/java/world/bentobox/islandselector/utils/GridCoordinate.java`
- Pattern: Immutable value object with X,Z integers, bidirectional world coordinate conversion

**GridLocation (Plugin):**
- Purpose: Metadata for a single grid cell (status, owner, reservation)
- Examples: `generations/island_selector/src/main/java/world/bentobox/islandselector/models/GridLocation.java`
- Pattern: Mutable entity with status enum (AVAILABLE, OCCUPIED, RESERVED, LOCKED)

**SlotData (Plugin):**
- Purpose: Player's island slot state (active, island ID, homes, cooldowns)
- Examples: `generations/island_selector/src/main/java/world/bentobox/islandselector/database/SlotData.java`
- Pattern: BentoBox DataObject with @Table annotation, JSON serializable

**Settings (Plugin):**
- Purpose: Plugin configuration loaded from config.yml
- Examples: `generations/island_selector/src/main/java/world/bentobox/islandselector/Settings.java`
- Pattern: Implements BentoBox `ConfigObject` interface

## Entry Points

**Python Agent (CLI):**
- Location: `autonomous_agent_demo.py`
- Triggers: `python autonomous_agent_demo.py --project-dir ./project`
- Responsibilities: Parse args, run async agent loop, handle interrupts

**Minecraft Plugin:**
- Location: `generations/island_selector/src/main/java/world/bentobox/islandselector/IslandSelector.java`
- Triggers: BentoBox addon loading on server start
- Responsibilities: Initialize managers, register commands/listeners, sync with BSkyBlock

**Main Player Command:**
- Location: `generations/island_selector/src/main/java/world/bentobox/islandselector/commands/IslandSelectorCommand.java`
- Triggers: `/islandselector` or `/is`
- Responsibilities: Route to subcommands, open grid GUI by default

## Error Handling

**Strategy:** Fail-safe with logging

**Python Patterns:**
- Try/catch in `run_agent_session()` returns `("error", str(e))` tuple
- Security hook returns block decision on parse failures (fail-safe)
- Auto-retry with fresh session on errors

**Plugin Patterns:**
- Managers log warnings/errors via `addon.log()`, `addon.logError()`
- FAWE operations wrapped with availability checks
- Graceful degradation when optional dependencies missing (Vault, PAPI, Level addon)

## Cross-Cutting Concerns

**Logging:**
- Python: `print()` statements with formatting
- Plugin: BentoBox addon logging (`addon.log()`, `addon.logWarning()`, `addon.logError()`)

**Validation:**
- Python: Command allowlist in `security.py` with shlex parsing
- Plugin: Permission checks via BentoBox API, coordinate bounds validation

**Authentication:**
- Python: Claude API via claude_code_sdk (assumes claude-code CLI configured)
- Plugin: Minecraft player UUIDs, BentoBox permission system

**Concurrency:**
- Python: Async/await with asyncio
- Plugin: ConcurrentHashMap for thread-safe caches, async database saves

---

*Architecture analysis: 2026-01-20*
