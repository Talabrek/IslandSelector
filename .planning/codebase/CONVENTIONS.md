# Coding Conventions

**Analysis Date:** 2026-01-20

## Naming Patterns

**Files:**
- `snake_case.py` for all Python modules
- Descriptive names reflecting primary purpose: `security.py`, `progress.py`, `prompts.py`
- Test files prefixed with `test_`: `test_security.py`

**Functions:**
- `snake_case` for all function names
- Verb-first naming: `create_client()`, `load_prompt()`, `extract_commands()`
- Async functions prefixed implicitly by `async def`: `run_agent_session()`, `bash_security_hook()`
- Boolean return helpers: `test_hook()`, validation functions return tuples

**Variables:**
- `snake_case` for local variables and parameters
- `SCREAMING_SNAKE_CASE` for module-level constants: `ALLOWED_COMMANDS`, `AUTO_CONTINUE_DELAY_SECONDS`, `DEFAULT_MODEL`

**Types:**
- Type hints used consistently throughout
- Return types specified: `-> tuple[int, int]`, `-> ClaudeSDKClient`
- `Optional[]` for nullable parameters
- `Path` from pathlib preferred over string paths

## Code Style

**Formatting:**
- No automated formatter configured (VSCode auto-format disabled in `.vscode/settings.json`)
- 4-space indentation
- Line length: approximately 88-100 characters (flexible)
- Blank lines between top-level functions

**Linting:**
- No explicit linter configuration detected
- Type hints serve as documentation and light validation

## Import Organization

**Order:**
1. Standard library imports (`asyncio`, `json`, `os`, `shlex`, `argparse`, `sys`)
2. Third-party imports (`claude_code_sdk`)
3. Local imports (`from security import ...`, `from client import ...`)

**Pattern observed in `agent.py`:**
```python
import asyncio
from pathlib import Path
from typing import Optional

from claude_code_sdk import ClaudeSDKClient

from client import create_client
from progress import print_session_header, print_progress_summary
from prompts import get_initializer_prompt, get_coding_prompt, copy_spec_to_project
```

**Path Aliases:**
- None configured - relative imports within project root

## Error Handling

**Patterns:**
- Try/except at operation boundaries with specific exception types
- Fail-safe defaults when parsing fails (return empty list to trigger block)
- Tuple returns for validation: `(is_allowed, reason_if_blocked)`

**Example from `security.py`:**
```python
try:
    tokens = shlex.split(command_string)
except ValueError:
    return False, "Could not parse chmod command"
```

**Example from `progress.py`:**
```python
try:
    with open(tests_file, "r") as f:
        tests = json.load(f)
except (json.JSONDecodeError, IOError):
    return 0, 0
```

**Strategy:**
- Catch specific exceptions, not bare `except:`
- Return safe defaults rather than propagating errors in utility functions
- Print error messages for user-facing operations in `agent.py`

## Logging

**Framework:** Standard `print()` statements

**Patterns:**
- Status messages: `print(f"Created security settings at {settings_file}")`
- Progress output: `print(f"\nProgress: {passing}/{total} tests passing ({percentage:.1f}%)")`
- Tool feedback: `print(f"\n[Tool: {block.name}]", flush=True)`
- Section headers with visual separators: `print("=" * 70)`

**When to Log:**
- Session start/end
- Tool invocations (truncated input)
- Errors with context
- Progress milestones

## Comments

**When to Comment:**
- Module-level docstrings explaining purpose (required)
- Function docstrings with Args/Returns (required for public functions)
- Inline comments for non-obvious security logic

**Docstring Format (Google-style):**
```python
def validate_chmod_command(command_string: str) -> tuple[bool, str]:
    """
    Validate chmod commands - only allow making files executable with +x.

    Returns:
        Tuple of (is_allowed, reason_if_blocked)
    """
```

**Module Docstrings:**
```python
"""
Security Hooks for Autonomous Coding Agent
==========================================

Pre-tool-use hooks that validate bash commands for security.
Uses an allowlist approach - only explicitly permitted commands can run.
"""
```

## Function Design

**Size:**
- Functions typically 10-50 lines
- Larger functions (~100 lines) for main loops with clear internal structure

**Parameters:**
- Use `Path` objects for file paths, not strings
- Default to `None` for optional parameters with `Optional[]` type
- Named parameters for clarity in complex signatures

**Return Values:**
- Tuples for multiple return values: `tuple[str, str]`, `tuple[bool, str]`
- Empty dict `{}` for "allow" decisions in hooks
- Dict with `"decision": "block"` for block decisions

## Module Design

**Exports:**
- Direct imports from modules: `from security import bash_security_hook`
- No `__all__` definitions (all public functions importable)

**Barrel Files:**
- Not used - each module imports directly from source

**Module Organization:**
- One clear purpose per module
- Constants at top, then helper functions, then main API functions
- Entry point function (`main()`) at bottom

## Async Patterns

**When to Use:**
- SDK interactions (`async with client:`)
- I/O-bound operations that may block
- Hook callbacks registered with SDK

**Pattern:**
```python
async with client:
    status, response = await run_agent_session(client, prompt, project_dir)
```

**Running Async Code:**
```python
asyncio.run(
    run_autonomous_agent(
        project_dir=project_dir,
        model=args.model,
        max_iterations=args.max_iterations,
    )
)
```

## Configuration Patterns

**Constants:**
- Module-level constants for configuration: `DEFAULT_MODEL`, `AUTO_CONTINUE_DELAY_SECONDS`
- Sets for allowlists: `ALLOWED_COMMANDS = {"ls", "cat", ...}`

**External Config:**
- JSON files for runtime configuration (`.claude_settings.json`)
- Markdown files for prompts (`prompts/coding_prompt.md`)

---

*Convention analysis: 2026-01-20*
