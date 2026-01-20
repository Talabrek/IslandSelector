# Technology Stack

**Analysis Date:** 2026-01-20

## Languages

**Primary:**
- Python 3.x - Autonomous agent harness and orchestration (`agent.py`, `client.py`, `security.py`, etc.)

**Secondary:**
- Java 17+ - Target output: Minecraft plugin development (generated in `generations/` directory)
- Markdown - Prompts and documentation (`prompts/*.md`)

## Runtime

**Environment:**
- Python 3.x (no specific version locked)
- Requires Claude Code SDK connection to Claude account

**Package Manager:**
- pip (Python)
- Lockfile: Not present (only `requirements.txt` with single dependency)

## Frameworks

**Core:**
- `claude-code-sdk` >=0.0.25 - Anthropic's Claude Agent SDK for autonomous coding sessions

**Testing:**
- Built-in Python unittest-style tests (`test_security.py`)
- Manual testing workflow for generated Minecraft plugins

**Build/Dev:**
- asyncio - Async runtime for agent sessions
- shlex - Command parsing for security validation
- pathlib - File path management
- argparse - CLI argument handling

## Key Dependencies

**Critical:**
- `claude-code-sdk` >=0.0.25 - Core dependency enabling Claude agent interactions, tool use, and session management

**Infrastructure:**
- Standard library only beyond the SDK (json, asyncio, shutil, re, os)

## Configuration

**Environment:**
- Claude Code must be connected to a Claude account
- No `.env` files used
- Configuration is code-based in `client.py`

**Build:**
- No build step for the Python harness
- Generated projects use Maven or Gradle (configured via prompts)

**Key Config Files:**
- `.claude/settings.local.json` - Claude Code permissions (bash, web fetch allowlists)
- `.vscode/settings.json` - Editor settings (disabled auto-formatting for Java)
- Generated: `.claude_settings.json` - Security settings written per-project

## Platform Requirements

**Development:**
- Python 3.x
- Claude Code CLI connected to Claude account
- For generated plugins: JDK 17+, Maven or Gradle

**Production:**
- This is a development harness, not deployed to production
- Generated plugins target: Paper/Spigot Minecraft servers 1.20.x - 1.21.x

## Project Purpose

This codebase is an **autonomous coding harness** that uses Claude Agent SDK to build Minecraft plugins. The harness:

1. Uses a two-agent pattern (initializer + coding agent)
2. Implements defense-in-depth security (sandbox, filesystem restrictions, bash allowlist)
3. Generates complete Minecraft plugins in the `generations/` directory
4. Supports iterative development with human-in-the-loop manual testing

**Current Target Project:**
- IslandSelector - A BentoBox addon for BSkyBlock (Minecraft Skyblock plugin)
- Defined in `prompts/app_spec.txt`

---

*Stack analysis: 2026-01-20*
