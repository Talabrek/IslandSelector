# Codebase Structure

**Analysis Date:** 2026-01-20

## Directory Layout

```
IslandSelector/
├── .claude/                    # Claude Code configuration
│   └── skills/                 # Custom skills definitions
├── .planning/                  # GSD planning documents
│   └── codebase/               # Codebase analysis documents
├── .vscode/                    # VS Code settings
├── prompts/                    # Agent prompt templates
│   ├── app_spec.txt            # Plugin specification (1990 lines)
│   ├── initializer_prompt.md   # First session instructions
│   └── coding_prompt.md        # Continuation session instructions
├── generations/                # Generated project outputs
│   └── island_selector/        # Generated Minecraft plugin
│       ├── src/main/java/      # Java source code
│       ├── src/main/resources/ # Plugin resources
│       ├── src/test/java/      # Unit tests
│       ├── target/             # Maven build output
│       └── pom.xml             # Maven configuration
├── __pycache__/                # Python bytecode cache
├── agent.py                    # Agent session logic
├── autonomous_agent_demo.py    # Main CLI entry point
├── client.py                   # Claude SDK client config
├── progress.py                 # Progress tracking utilities
├── prompts.py                  # Prompt loading utilities
├── security.py                 # Bash command security
├── test_security.py            # Security hook tests
├── requirements.txt            # Python dependencies
└── README.md                   # Project documentation
```

## Directory Purposes

**Root Directory (Python Agent Harness):**
- Purpose: Autonomous code generation orchestration
- Contains: Python modules for Claude SDK integration
- Key files: `autonomous_agent_demo.py`, `agent.py`, `client.py`, `security.py`

**`prompts/`:**
- Purpose: Agent instruction templates
- Contains: Markdown prompts, plugin specification
- Key files:
  - `app_spec.txt` - Complete IslandSelector plugin specification
  - `initializer_prompt.md` - Creates feature_list.json with 200+ test cases
  - `coding_prompt.md` - Implements features, prepares for manual testing

**`generations/island_selector/`:**
- Purpose: Generated Minecraft BentoBox addon
- Contains: Complete Java Maven project
- Key files:
  - `pom.xml` - Maven build config
  - `CLAUDE.md` - AI-readable project summary
  - `README.md` - User documentation
  - `DEVELOPMENT_NOTES.md` - Technical patterns

**`generations/island_selector/src/main/java/world/bentobox/islandselector/`:**
- Purpose: Plugin Java source code
- Contains: All addon classes organized by responsibility

**`generations/island_selector/src/main/resources/`:**
- Purpose: Plugin runtime resources
- Contains: `addon.yml`, `config.yml`, locale files

**`generations/island_selector/src/test/java/`:**
- Purpose: Unit tests for plugin
- Contains: JUnit test classes

## Key File Locations

**Entry Points:**
- `autonomous_agent_demo.py`: Python CLI entry point
- `generations/island_selector/src/main/java/world/bentobox/islandselector/IslandSelector.java`: Plugin main class

**Configuration:**
- `requirements.txt`: Python dependencies (claude-code-sdk)
- `generations/island_selector/pom.xml`: Maven build config, Java 17
- `generations/island_selector/src/main/resources/config.yml`: Plugin default config
- `generations/island_selector/src/main/resources/addon.yml`: BentoBox addon manifest

**Core Logic (Python):**
- `agent.py`: `run_agent_session()`, `run_autonomous_agent()`
- `client.py`: `create_client()` with security settings
- `security.py`: `bash_security_hook()`, `ALLOWED_COMMANDS`

**Core Logic (Plugin):**
- `generations/island_selector/src/main/java/world/bentobox/islandselector/managers/GridManager.java`: Grid state management
- `generations/island_selector/src/main/java/world/bentobox/islandselector/managers/SlotManager.java`: Slot management
- `generations/island_selector/src/main/java/world/bentobox/islandselector/managers/SlotSwitchManager.java`: Switch orchestration

**Testing:**
- `test_security.py`: Python security hook tests
- `generations/island_selector/src/test/java/world/bentobox/islandselector/`: JUnit tests

## Naming Conventions

**Files (Python):**
- snake_case for modules: `agent.py`, `security.py`, `test_security.py`
- Prompts use snake_case with extension: `initializer_prompt.md`, `coding_prompt.md`

**Files (Java Plugin):**
- PascalCase for classes: `GridManager.java`, `SlotSelectionGUI.java`
- Package path follows reverse domain: `world.bentobox.islandselector`

**Directories:**
- Python: lowercase with underscores if needed
- Java: Standard Maven layout (`src/main/java/`, `src/test/java/`)
- Plugin packages by responsibility: `commands/`, `gui/`, `managers/`, `database/`, `listeners/`, `integrations/`, `models/`, `utils/`

**Classes (Java Plugin):**
- Commands: `*Command.java` (e.g., `AdminReloadCommand.java`, `SlotsCommand.java`)
- GUIs: `*GUI.java` (e.g., `MainGridGUI.java`, `SlotSelectionGUI.java`)
- Listeners: `*Listener.java` (e.g., `IslandCreateListener.java`)
- Managers: `*Manager.java` (e.g., `GridManager.java`, `SlotManager.java`)
- Database entities: `*Data.java` (e.g., `SlotData.java`, `GridLocationData.java`)
- Events: `*Event.java` (e.g., `SlotSwitchEvent.java`, `IslandRelocateEvent.java`)
- Integrations: `*Integration.java` (e.g., `WorldEditIntegration.java`)

## Where to Add New Code

**New Python Feature (Agent Harness):**
- Primary code: Add module in root directory
- Import in: `agent.py` or `autonomous_agent_demo.py`
- Tests: Create `test_*.py` file

**New Plugin Command:**
- Implementation: `generations/island_selector/src/main/java/world/bentobox/islandselector/commands/`
- Registration: Add in `IslandSelectorCommand.java` or `AdminCommand.java`
- Pattern: Extend `CompositeCommand` or `ConfirmableCommand`

**New Plugin GUI:**
- Implementation: `generations/island_selector/src/main/java/world/bentobox/islandselector/gui/`
- Pattern: Inventory-based with click handlers, close handlers
- Register listener: In constructor or use shared listener pattern

**New Plugin Manager:**
- Implementation: `generations/island_selector/src/main/java/world/bentobox/islandselector/managers/`
- Initialize in: `IslandSelector.onEnable()` after dependencies
- Add getter: Add `getXxxManager()` method to `IslandSelector.java`

**New Database Entity:**
- Implementation: `generations/island_selector/src/main/java/world/bentobox/islandselector/database/`
- Pattern: Implement BentoBox `DataObject`, use `@Table` annotation
- Database instance: Create `Database<YourData>` in managing class

**New Plugin Event:**
- Implementation: `generations/island_selector/src/main/java/world/bentobox/islandselector/events/`
- Pattern: Extend `Event` or BentoBox event classes
- Fire: Use `Bukkit.getPluginManager().callEvent()`

**Utilities:**
- Python: Add to root or create `utils.py`
- Java: `generations/island_selector/src/main/java/world/bentobox/islandselector/utils/`

## Special Directories

**`generations/`:**
- Purpose: Output directory for generated projects
- Generated: Yes - by autonomous agent
- Committed: Yes - contains the actual plugin code

**`generations/island_selector/target/`:**
- Purpose: Maven build output (compiled JAR)
- Generated: Yes - by `mvn package`
- Committed: No (should be in .gitignore)

**`__pycache__/`:**
- Purpose: Python bytecode cache
- Generated: Yes - by Python interpreter
- Committed: No

**`.planning/codebase/`:**
- Purpose: GSD codebase analysis documents
- Generated: Yes - by GSD map-codebase command
- Committed: Yes - provides context for future development

**`prompts/`:**
- Purpose: Agent instruction templates
- Generated: No - manually authored
- Committed: Yes - defines agent behavior

## File Counts by Area

**Python Agent Harness:**
- 6 Python modules
- 1 requirements.txt
- 1 README.md

**Plugin Source (generations/island_selector/src/main/java/):**
- Commands: 25+ classes
- GUI: 15+ classes
- Managers: 12 classes
- Database: 3 classes
- Listeners: 3 classes
- Integrations: 3 classes
- Models: 2 classes
- Utils: 4 classes
- Events: 3 classes
- Core: 2 classes (IslandSelector, Settings)

**Plugin Resources:**
- `addon.yml` - BentoBox manifest
- `config.yml` - Default configuration
- `locales/en-US.yml` - English messages

---

*Structure analysis: 2026-01-20*
