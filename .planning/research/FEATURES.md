# Feature Landscape: Maven Java Project Cleanup

**Domain:** BentoBox Addon (Maven Java Project) - Brownfield Cleanup
**Researched:** 2026-01-20
**Confidence:** HIGH (based on direct file system analysis)

## Table Stakes (MUST KEEP)

Files and directories essential to the project. Removing these breaks the build or loses source code.

| File/Pattern | Why Keep | Location |
|--------------|----------|----------|
| `pom.xml` | Maven build configuration - defines dependencies, build process, project metadata | `generations/island_selector/pom.xml` |
| `src/main/java/**/*.java` | Production source code - the actual addon implementation | 82 Java files across commands, gui, managers, etc. |
| `src/main/resources/` | Runtime resources - config.yml, addon.yml, locales | `config.yml`, `addon.yml`, `locales/en-US.yml` |
| `src/test/java/**/*.java` | Test source code - unit tests | 3 test files |
| `.gitignore` | Git configuration for the main project | Root level |
| `README.md` | Project documentation (in generations/island_selector) | Essential for understanding the addon |
| `CLAUDE.md` | Development context documentation | Useful for ongoing development |

### Standard Maven Directory Structure

```
project-root/
  pom.xml                    # KEEP - Build definition
  src/
    main/
      java/                  # KEEP - All production Java source
      resources/             # KEEP - All runtime resources
    test/
      java/                  # KEEP - All test source
      resources/             # KEEP - Test resources (if any)
```

## Remove: Backup Files

Backup files that duplicate source code. These indicate development iteration but should not be in version control.

| Pattern | Count | Specific Files |
|---------|-------|----------------|
| `*_backup.java` | 3 | `MainGridGUI_backup.java`, `GridManager_backup.java`, `NeighborhoodGUI_backup_session44.java` |
| `*.java.bak` | 1 | `src/main/java/.../gui/MainGridGUI.java.bak` |
| `*.java.backup` | 3 | `IslandSelectorCommand.java.backup`, `AdminCommand.java.backup`, `IslandSelector.java.backup` |
| `*.yml.bak` | 2 | `locales/en-US.yml.bak` (src and target) |
| `*.md.backup` | 2 | `TESTING.md.backup`, `TESTING.md.session40.backup` |

**Total backup files: 11**

## Remove: Python Scripts and Artifacts

Python tooling scripts that are not part of the Java project.

### Root Level Python Files (REMOVE)
| File | Purpose (inferred) |
|------|-------------------|
| `agent.py` | AI agent tooling |
| `client.py` | AI client tooling |
| `progress.py` | Progress tracking |
| `prompts.py` | Prompt management |
| `security.py` | Security utilities |
| `test_security.py` | Security tests |
| `autonomous_agent_demo.py` | Demo script |
| `requirements.txt` | Python dependencies |

### Inner Project Python Files (REMOVE from generations/island_selector/)
| File | Purpose (inferred) |
|------|-------------------|
| `add_method.py` | Code generation helper |
| `check_tests.py` | Test checking script |
| `find_tests.py` | Test discovery |
| `check_neighborhood.py` | Debug script |
| `check_session42.py` | Session debug |
| `find_simple_tests.py` | Test filtering |
| `find_later_tests.py` | Test filtering |
| `find_permission_tests.py` | Test filtering |
| `get_neighborhood_tests.py` | Test extraction |

### Python Cache (REMOVE)
| Directory | Contents |
|-----------|----------|
| `__pycache__/` | 5 compiled .pyc files |

**Total Python items: 17 files + 1 directory**

## Remove: Nested Git Repository

A nested `.git` folder inside `generations/island_selector/` creates a git-within-git scenario that causes issues.

| Item | Issue |
|------|-------|
| `generations/island_selector/.git/` | Nested git repo - conflicts with parent .git |

**Action:** Remove entire `.git` folder from `generations/island_selector/`

## Remove: Build Artifacts (Optional)

Maven's `target/` directory contains compiled output. Should be gitignored, can be regenerated with `mvn clean package`.

| Directory | Contents | Size |
|-----------|----------|------|
| `generations/island_selector/target/` | .class files, compiled resources, test reports | 100+ files |

**Note:** If not already in .gitignore, add `target/` pattern. Removal is safe - `mvn clean package` regenerates everything.

## Remove: Shell Scripts (Development Tooling)

Shell scripts used during development but not part of the addon.

| File | Purpose |
|------|---------|
| `init.sh` | Project initialization |
| `find_next_test.sh` | Test discovery |
| `update_testing.sh` | Testing helper |

**Total: 3 shell scripts**

## Remove: Miscellaneous Clutter

| File | Reason |
|------|--------|
| `nul` | Empty file (Windows artifact from `> nul`) |
| `prompts/` directory | AI tooling (app_spec.txt, initializer_prompt.md, coding_prompt.md) |
| `generations/` wrapper | Consider flattening - the actual project is inside |
| `feature_list.json` | Development tracking file, not runtime |
| `.claude_settings.json` | AI tool settings |
| `app_spec.txt` (in generations/island_selector) | Original spec, could keep for reference or archive |
| `DEVELOPMENT_NOTES.md` | Keep if valuable, or archive |
| `output/` directory | Build output folder |

## Anti-Features (DO NOT REMOVE)

Files that might look like clutter but are essential.

| File/Pattern | Why Keep |
|--------------|----------|
| `.vscode/settings.json` | IDE configuration (optional but harmless) |
| `.claude/` directory | Claude Code context (useful for AI-assisted development) |
| `.planning/` directory | GSD planning files (active use) |
| `CLAUDE.md` | Development context (valuable) |

## Cleanup Priority Order

### Phase 1: Safe Removal (No Risk)
1. `__pycache__/` - Python cache, zero value
2. `nul` file - Empty artifact
3. All `*_backup*` files - Duplicates of existing code
4. All `*.bak` files - Duplicates
5. All `*.backup` files - Duplicates

### Phase 2: Python Cleanup (Verify Not Needed)
1. Root-level `.py` files
2. `requirements.txt`
3. Python scripts in `generations/island_selector/`

### Phase 3: Git Structure
1. Remove `generations/island_selector/.git/` - Nested repo

### Phase 4: Tooling Scripts
1. Shell scripts (`*.sh`)
2. `prompts/` directory

### Phase 5: Optional Restructure
1. Consider flattening: Move `generations/island_selector/*` to root
2. Remove empty `generations/` wrapper
3. Clean up `target/` if not gitignored

## Verification Checklist

Before removing any file category, verify:

- [ ] No `.java` files in src/ are flagged for removal
- [ ] `pom.xml` is preserved
- [ ] `src/main/resources/` contents preserved
- [ ] `src/test/java/` contents preserved
- [ ] `.gitignore` preserved
- [ ] Project still builds: `mvn clean package`

## Cleanup Commands Reference

```bash
# Remove backup files
rm -f *_backup.java
rm -f **/*.bak
rm -f **/*.backup

# Remove Python files (from project root)
rm -f *.py
rm -f requirements.txt
rm -rf __pycache__/

# Remove nested git
rm -rf generations/island_selector/.git/

# Remove shell scripts
rm -f generations/island_selector/*.sh

# Remove nul artifact
rm -f nul

# Clean Maven build
cd generations/island_selector && mvn clean
```

## Summary Table

| Category | Count | Action |
|----------|-------|--------|
| Essential Java files | 85 | KEEP |
| Essential resources | 3 | KEEP |
| Essential config | 2 | KEEP |
| Backup files | 11 | REMOVE |
| Python files | 17 | REMOVE |
| Python cache | 1 dir | REMOVE |
| Nested .git | 1 dir | REMOVE |
| Shell scripts | 3 | REMOVE |
| Windows artifacts | 1 | REMOVE |
| Build artifacts (target/) | 1 dir | CLEAN/GITIGNORE |

## Sources

- Direct file system analysis of `C:\Users\Administrator\Desktop\VSCode\IslandSelector`
- Maven standard directory layout conventions
- Git best practices for repository structure
