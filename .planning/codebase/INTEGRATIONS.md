# External Integrations

**Analysis Date:** 2026-01-20

## APIs & External Services

**Claude Agent SDK:**
- Purpose: Core integration for autonomous coding with Claude
- SDK/Client: `claude-code-sdk` (Python package)
- Auth: Requires Claude Code CLI connected to Claude account (not env var based)
- Key classes used:
  - `ClaudeSDKClient` - Main client for agent sessions
  - `ClaudeCodeOptions` - Configuration (model, tools, hooks, cwd, settings)
  - `HookMatcher` - Security hook registration for tool validation
- Implementation: `client.py`

**Claude Code Built-in Tools:**
- Tools enabled: Read, Write, Edit, Glob, Grep, Bash
- Configured in: `client.py` BUILTIN_TOOLS list
- Security: Bash commands validated via `bash_security_hook` in `security.py`

## Data Storage

**Databases:**
- None for the harness itself
- Generated plugins may use BentoBox's database abstraction (YAML, JSON, MySQL, MongoDB, SQLite)

**File Storage:**
- Local filesystem only
- Project outputs: `generations/{project-name}/`
- Schematics (for generated plugin): `generations/{project}/slots/{uuid}/slot-{number}.schem`
- Backups (for generated plugin): `generations/{project}/backups/{uuid}/`

**Caching:**
- None for the harness
- Generated plugins implement in-memory caching for island/grid data

## Authentication & Identity

**Auth Provider:**
- Claude Code CLI authentication (implicit via SDK)
- No custom auth implementation

**Generated Plugin Auth:**
- BentoBox permission system
- Vault economy integration (optional)

## Monitoring & Observability

**Error Tracking:**
- Console output with try/except handling
- Tool use results printed with status indicators ([Done], [BLOCKED], [Error])

**Logs:**
- stdout logging for session progress
- Tool invocations logged with input preview (truncated to 200 chars)
- No persistent log files for harness (generated plugins may have configurable debug logging)

## CI/CD & Deployment

**Hosting:**
- Local development only
- No cloud deployment

**CI Pipeline:**
- None configured
- Manual execution via `python autonomous_agent_demo.py`

## Environment Configuration

**Required env vars:**
- None explicitly required
- Claude Code CLI must be authenticated

**Secrets location:**
- No secrets stored
- Claude auth handled by Claude Code CLI

## Webhooks & Callbacks

**Incoming:**
- None

**Outgoing:**
- None

## Security Integrations

**Bash Command Allowlist:**
- Location: `security.py` ALLOWED_COMMANDS set
- Purpose: Validates all bash commands before execution
- Allowed categories:
  - File inspection: ls, cat, head, tail, wc, grep
  - File operations: cp, mkdir, rm, mv, chmod
  - Java/Minecraft: mvn, mvnw, gradle, gradlew, java, javac
  - Version control: git
  - Process management: ps, lsof, sleep, pkill
  - Script execution: init.sh
- Extra validation for: pkill, chmod, init.sh

**Sandbox Configuration:**
- OS-level bash isolation enabled
- Filesystem restricted to project directory only
- Configured via `.claude_settings.json` written per-project

## Web Fetch Permissions

**Allowed Domains (from `.claude/settings.local.json`):**
- `docs.xenondevs.xyz` - InvUI library documentation
- `github.com` - Source code reference
- `madelinemiller.dev` - Adventure/MiniMessage docs
- `docs.enginehub.org` - WorldEdit/FAWE documentation
- `docs.bentobox.world` - BentoBox addon documentation
- `hub.spigotmc.org` - Spigot API documentation
- `www.spigotmc.org` - Spigot resources

## Generated Plugin Integrations

The generated Minecraft plugin (IslandSelector) integrates with:

**Required:**
- BentoBox - Addon framework, database abstraction, GUI panels
- BSkyBlock - Skyblock gamemode addon
- FastAsyncWorldEdit (FAWE) - Schematic operations (async)

**Optional:**
- Vault - Economy integration for costs and premium locations
- PlaceholderAPI - Placeholders for scoreboards, chat, etc.
- Level Addon - Island level display in GUI

---

*Integration audit: 2026-01-20*
