# Phase 3: Project Restructure - Research

**Researched:** 2026-01-20
**Domain:** Git repository restructuring, Maven project layout
**Confidence:** HIGH

## Summary

This phase involves moving the Java Maven project from `generations/island_selector/` to the repository root to achieve a standard Maven project structure. The primary challenges are:

1. **Git history preservation** - Using `git mv` to maintain file history during moves
2. **File conflicts** - Handling duplicate README.md and .gitignore files
3. **Build artifact cleanup** - Excluding the `target/` directory from moves
4. **Post-move verification** - Ensuring Maven builds succeed after restructure

The restructure is straightforward because the pom.xml uses standard relative paths (`src/main/resources`) that will work identically after the move. No path modifications are required in pom.xml.

**Primary recommendation:** Use `git mv` for all file moves to preserve history, delete the old root-level README.md (which is about the autonomous agent harness, not this project), and merge .gitignore files to combine all necessary patterns.

## Standard Stack

### Core Tools
| Tool | Purpose | Why Standard |
|------|---------|--------------|
| git mv | Move files with history preservation | Built-in Git command, tracks renames |
| rmdir | Remove empty directories | Standard cleanup after moves |
| mvn | Maven build verification | Confirms project structure is correct |

### Supporting
| Tool | Purpose | When to Use |
|------|---------|-------------|
| git status | Verify staging state | Before and after each operation |
| git diff --cached | Review staged changes | Verify moves are correct |
| mvn clean package -q | Build verification | After all moves complete |

## Architecture Patterns

### Standard Maven Project Structure (Target State)
```
IslandSelector/                     # Repository root
├── pom.xml                         # Project definition
├── src/
│   ├── main/
│   │   ├── java/                   # Java source files
│   │   └── resources/              # Resources (addon.yml, config.yml)
│   └── test/
│       └── java/                   # Test source files
├── README.md                       # Project documentation
├── CLAUDE.md                       # AI assistant notes
├── DEVELOPMENT_NOTES.md            # Developer notes
├── .gitignore                      # Combined ignore patterns
├── .claude/                        # Claude settings
├── .planning/                      # Planning documents
└── .vscode/                        # VS Code settings
```

### Pattern 1: Git Move with History Preservation
**What:** Use `git mv` instead of filesystem move
**When to use:** Always for tracked files
**Example:**
```bash
# Move entire directory
git mv generations/island_selector/src ./src

# Move single file
git mv generations/island_selector/pom.xml ./pom.xml
```

### Pattern 2: Merge .gitignore Files
**What:** Combine ignore patterns from both locations
**When to use:** When both root and subdirectory have .gitignore
**Strategy:**
1. Read root .gitignore patterns (agent-related)
2. Read subdirectory .gitignore patterns (Java/Maven)
3. Keep subdirectory patterns at root (standard Java project)
4. Remove agent-specific patterns (no longer needed)
5. Add any still-needed patterns from root

### Pattern 3: Replace Non-Project README
**What:** Replace root README.md about agent harness with project README.md
**When to use:** When root README describes different project
**Current state:**
- Root README.md: Describes "Autonomous Minecraft Plugin Development" harness (200 lines)
- generations/island_selector/README.md: Describes IslandSelector addon (306 lines)
**Action:** Delete root, move inner README

### Anti-Patterns to Avoid
- **Filesystem mv without git:** Loses file history, shows as delete+create
- **Moving target/ directory:** Wastes time and bloats commits, target is regenerated
- **Forgetting .bak files:** Leave behind backup files that clutter root
- **Not verifying build:** Structural issues only caught by Maven

## Don't Hand-Roll

| Problem | Don't Do | Use Instead | Why |
|---------|----------|-------------|-----|
| Preserving file history | `cp`/`mv` shell commands | `git mv` | Git tracks renames natively |
| Merge ignore files | Manual line-by-line copy | Analyze both, create combined | Miss patterns easily |
| Delete directory trees | Manual recursive delete | `git rm -r` then `rmdir` | Ensures proper staging |

## Common Pitfalls

### Pitfall 1: Moving Build Artifacts
**What goes wrong:** Moving `target/` directory, bloating commit with binaries
**Why it happens:** Recursively moving entire directory without exclusion
**How to avoid:** Move specific directories/files, not the parent
**Warning signs:** Commit shows .class files, .jar files, large commit size

### Pitfall 2: Losing Git History
**What goes wrong:** Files show 0 commits history after move
**Why it happens:** Using filesystem operations instead of git mv
**How to avoid:** Always use `git mv` for tracked files
**Warning signs:** `git log --follow` shows no history before move

### Pitfall 3: Forgetting Backup Files
**What goes wrong:** .bak files and temporary files moved to root
**Why it happens:** Moving entire directories blindly
**How to avoid:** Inventory files before moving, exclude .bak files
**Verification:** `ls *.bak` at root should be empty after phase

### Pitfall 4: Broken Build Paths
**What goes wrong:** Maven cannot find source files
**Why it happens:** pom.xml has non-standard source paths
**How to avoid:** Verify pom.xml uses standard paths (it does: `src/main/resources`)
**Verification:** `mvn clean package -q` succeeds

### Pitfall 5: Empty Directory Left Behind
**What goes wrong:** `generations/island_selector/` still exists but empty
**Why it happens:** Git only tracks files, not directories
**How to avoid:** Explicitly remove empty directories with `rmdir`
**Success criteria:** `generations/` directory no longer exists

## Code Examples

### Full Move Sequence (Recommended Order)
```bash
# Step 1: Move src/ directory (largest component)
git mv generations/island_selector/src ./src

# Step 2: Move pom.xml
git mv generations/island_selector/pom.xml ./pom.xml

# Step 3: Move documentation files
git mv generations/island_selector/README.md ./README.md  # Replaces agent README
git mv generations/island_selector/CLAUDE.md ./CLAUDE.md
git mv generations/island_selector/DEVELOPMENT_NOTES.md ./DEVELOPMENT_NOTES.md

# Step 4: Handle .gitignore (merge, not replace - see below)

# Step 5: Remove remaining generated/temp files (not worth moving)
# .bak files should be deleted, not moved
```

### .gitignore Merge Strategy
Current root .gitignore:
```gitignore
# Agent-generated output directories
# Note: generations/island_selector/ is explicitly tracked (BentoBox addon source)
generations/*
!generations/island_selector/

# Log files
logs/
```

Inner .gitignore: Standard Java/Maven patterns (34 lines)

**Target .gitignore:** Use inner patterns as base, remove references to generations/
(The inner .gitignore already has comprehensive Java/Maven patterns)

### Verification Commands
```bash
# Check structure is correct
ls -la pom.xml src/

# Verify src/main/java structure
ls src/main/java/world/bentobox/islandselector/

# Verify Maven build works
mvn clean package -q

# Check for leftover files
ls generations/ 2>/dev/null || echo "generations/ successfully removed"
```

## State of the Art

| Current State | Target State | Impact |
|---------------|--------------|--------|
| Non-standard nested structure | Standard Maven at root | IDE auto-detection works |
| Two README files (different projects) | Single project README | Clear documentation |
| Two .gitignore files | Single merged .gitignore | No redundant patterns |
| Agent harness artifacts in git | Clean project structure | Professional appearance |

**Note:** The pom.xml in this project already uses standard Maven conventions:
- `<sourceDirectory>` not specified (defaults to src/main/java)
- `<resources>` uses relative `src/main/resources`
- No hardcoded paths that would break after move

## Files to Move

From `generations/island_selector/`:

### Core Maven Files (Required)
- `pom.xml` -> `./pom.xml`
- `src/` -> `./src/` (entire directory tree)

### Documentation Files
- `README.md` -> `./README.md` (replaces agent harness README)
- `CLAUDE.md` -> `./CLAUDE.md`
- `DEVELOPMENT_NOTES.md` -> `./DEVELOPMENT_NOTES.md`

### Files to Merge
- `.gitignore` - merge with root, use inner as base

### Files to NOT Move (Delete or Ignore)
- `target/` - build artifacts, regenerated by Maven
- `*.bak` files - temporary files, should be deleted
- Inner `.gitignore` after merge - contents merged to root

## Post-Move Verification Checklist

1. **Structure verification:**
   - [ ] `pom.xml` exists at root
   - [ ] `src/main/java/world/bentobox/islandselector/IslandSelector.java` exists
   - [ ] `src/main/resources/addon.yml` exists
   - [ ] `src/test/java/` exists with test files

2. **Cleanup verification:**
   - [ ] `generations/` directory no longer exists
   - [ ] No `.bak` files at root level
   - [ ] `target/` not committed (regenerated by build)

3. **Build verification:**
   - [ ] `mvn clean package -q` completes without errors
   - [ ] JAR produced in `target/`

4. **History verification:**
   - [ ] `git log --follow src/main/java/world/bentobox/islandselector/IslandSelector.java` shows full history

## Open Questions

1. **Root-level "nul" file:**
   - Observation: There's a file named `nul` (155 bytes) at root
   - This appears to be an erroneous file (Windows reserved filename artifact)
   - Recommendation: Delete as part of cleanup

## Sources

### Primary (HIGH confidence)
- Direct file inspection of current project structure
- Git documentation for `git mv` behavior (well-established)
- Maven Standard Directory Layout (convention-based, well-established)

### Secondary (MEDIUM confidence)
- Analysis of pom.xml confirming standard paths used

## Metadata

**Confidence breakdown:**
- File moves: HIGH - Standard git operations, well documented
- Build verification: HIGH - Maven conventions well understood
- .gitignore merge: HIGH - Both files readable, patterns clear
- History preservation: HIGH - git mv is designed for this

**Research date:** 2026-01-20
**Valid until:** Indefinite (git and Maven conventions are stable)
