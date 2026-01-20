# Architecture Patterns: Maven Project Restructure

**Domain:** Brownfield Maven Java project restructure
**Researched:** 2026-01-20
**Confidence:** HIGH (based on official Git documentation and verified patterns)

## Project Situation Analysis

### Current State
```
IslandSelector/                    # Parent repo (.git exists)
  .git/                            # Parent git repository (2 commits)
  generations/
    island_selector/               # Nested repo
      .git/                        # NESTED git repository (rich history)
      pom.xml                      # Maven project definition
      src/
        main/java/...              # Java source files
        test/java/...              # Test files
      target/                      # Build output (gitignored)
      *.py                         # Development scripts (cleanup candidates)
      *.java                       # Backup files (cleanup candidates)
      *.backup                     # Backup files (cleanup candidates)
```

### Target State
```
IslandSelector/                    # Single git repository
  .git/                            # Merged history (ideally)
  pom.xml                          # Maven at project root
  src/
    main/java/...
    test/java/...
  CLAUDE.md                        # Keep documentation
  README.md                        # Keep documentation
```

## Order of Operations (Recommended)

### Phase 1: Backup and Prepare (BEFORE any destructive operations)

**Step 1.1: Verify working state**
```bash
# In parent repo
git status
# In nested repo
cd generations/island_selector
git status
git log --oneline -5  # Document current state
```

**Step 1.2: Create safety backup**
```bash
# Copy entire project to backup location
xcopy /E /I "C:\path\to\IslandSelector" "C:\path\to\IslandSelector_backup"
```

**Rationale:** Always create a full backup before any restructure. This is your safety net if anything goes wrong.

### Phase 2: Handle Nested Git Repository

**Critical Decision: Preserve nested history or abandon it?**

| Option | Pros | Cons | Recommendation |
|--------|------|------|----------------|
| Abandon nested history | Simple, fast | Lose development history | Only if history is unimportant |
| Merge into parent | Preserves all history | Complex, requires git filter-repo | If history matters for auditing |
| Keep nested as submodule | Clean separation | Wrong pattern for this use case | Not recommended |

**Option A: Abandon Nested History (Simpler)**

If the nested repo history is not critical (e.g., prototype code, or parent will be the canonical source):

```bash
# Step 2A.1: Remove nested .git folder
rd /s /q "C:\...\generations\island_selector\.git"

# Step 2A.2: Move files to parent root
# (Covered in Phase 3)
```

**Option B: Merge Nested History into Parent (Preserves History)**

If the nested repo has valuable commit history you want to preserve:

```bash
# Step 2B.1: In parent repo, add nested as remote
cd "C:\...\IslandSelector"
git remote add nested-repo "generations/island_selector"

# Step 2B.2: Fetch and merge with history
git fetch nested-repo
git merge nested-repo/master --allow-unrelated-histories -m "Merge nested repo history"

# Step 2B.3: Remove the remote
git remote remove nested-repo

# Step 2B.4: Remove nested .git folder
rd /s /q "C:\...\generations\island_selector\.git"
```

**Recommendation:** Given the nested repo has meaningful commit history (bug fixes, feature development), Option B preserves development context. However, Option A is acceptable if parent repo will be authoritative going forward.

### Phase 3: Move Files to Project Root

**Step 3.1: Clean build artifacts first**
```bash
# Remove Maven build output (not tracked, safe to delete)
rd /s /q "C:\...\generations\island_selector\target"
```

**Step 3.2: Identify files to DELETE (not move)**
```
Files to delete (development artifacts, not needed):
- *.py scripts (check_*.py, find_*.py, add_method.py, etc.)
- *_backup.java files
- *.backup files
- output/ directory
- init.sh (initialization script, not needed)
- feature_list.json (spec file, optional)
- app_spec.txt (spec file, optional)
- .claude_settings.json (project-specific, review first)
```

**Step 3.3: Identify files to MOVE**
```
Files to move to project root:
- pom.xml
- src/ (entire directory)
- CLAUDE.md
- README.md
- DEVELOPMENT_NOTES.md
- TESTING.md (if exists, not backup)
- .gitignore (merge with parent's)
```

**Step 3.4: Execute moves (use git mv for history tracking)**
```bash
cd "C:\...\IslandSelector"

# Move Maven project files
git mv generations/island_selector/pom.xml ./pom.xml
git mv generations/island_selector/src ./src

# Move documentation
git mv generations/island_selector/CLAUDE.md ./CLAUDE.md
git mv generations/island_selector/README.md ./ADDON_README.md
git mv generations/island_selector/DEVELOPMENT_NOTES.md ./DEVELOPMENT_NOTES.md
```

**Note on `git mv`:** While `git mv` doesn't magically preserve history differently than manual moves, it:
1. Atomically stages the move
2. Signals clear intent
3. Prevents accidental staging of unrelated changes

### Phase 4: Clean Up Remaining Files

**Step 4.1: Delete unnecessary files from nested location**
```bash
# Delete development scripts
del "C:\...\generations\island_selector\*.py"

# Delete backup files
del "C:\...\generations\island_selector\*_backup.java"
del "C:\...\generations\island_selector\*.backup"

# Delete output directory
rd /s /q "C:\...\generations\island_selector\output"
```

**Step 4.2: Delete Python files in parent root**
```bash
# These appear to be AI generation scaffolding
del "C:\...\IslandSelector\agent.py"
del "C:\...\IslandSelector\client.py"
del "C:\...\IslandSelector\progress.py"
del "C:\...\IslandSelector\prompts.py"
del "C:\...\IslandSelector\security.py"
del "C:\...\IslandSelector\test_security.py"
del "C:\...\IslandSelector\autonomous_agent_demo.py"

# Delete Python cache
rd /s /q "C:\...\IslandSelector\__pycache__"

# Delete prompts directory
rd /s /q "C:\...\IslandSelector\prompts"

# Delete 'nul' file (Windows artifact)
del "C:\...\IslandSelector\nul"
```

**Step 4.3: Remove empty generations directory**
```bash
rd /s /q "C:\...\IslandSelector\generations"
```

### Phase 5: Merge .gitignore Files

**Current parent .gitignore content:**
```gitignore
# Need to verify - likely minimal
```

**Nested .gitignore content (from CLAUDE.md context):**
Should include Maven standard ignores:
```gitignore
# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties

# IDE
.idea/
*.iml
.project
.classpath
.settings/

# OS
.DS_Store
Thumbs.db
```

**Action:** Merge both .gitignore files into a comprehensive one at project root.

### Phase 6: Verify and Commit

**Step 6.1: Verify structure**
```bash
# Should show clean Maven project structure
dir /B "C:\...\IslandSelector"
# Expected: pom.xml, src/, CLAUDE.md, .git/, etc.

# Verify Maven builds
cd "C:\...\IslandSelector"
mvn clean compile -q
```

**Step 6.2: Commit restructure**
```bash
git add .
git commit -m "Restructure: flatten Maven project to root"
```

## Windows-Specific Considerations

### Long Path Issues

**Problem:** Windows has a 260-character path limit by default. Deep Maven packages like `world.bentobox.islandselector.commands` can hit this limit.

**Solution 1: Enable Git long paths**
```bash
git config --global core.longpaths true
```

**Solution 2: Enable Windows long paths (requires admin)**
```
Registry: HKLM\SYSTEM\CurrentControlSet\Control\FileSystem
Set: LongPathsEnabled = 1 (DWORD)
```

Or via Group Policy:
```
Computer Configuration > Administrative Templates > System > FileSystem
Enable: "Enable Win32 Long Paths"
```

**Recommendation:** Enable both settings before restructure to prevent issues.

### File Locking Issues

**Problem:** Windows may lock files that are open in IDEs or processes.

**Prevention:**
1. Close IDE (IntelliJ, Eclipse, VS Code) before file operations
2. Close any terminal sessions in the project directory
3. Stop any running Maven processes

**If locked files encountered:**
```bash
# Find what's using the file (requires handle.exe from Sysinternals)
handle.exe "path\to\file"

# Or just close all applications and retry
```

### Line Endings

**Problem:** Git on Windows may convert line endings, causing spurious changes.

**Solution:** Ensure consistent config:
```bash
git config --global core.autocrlf true
```

### Command Syntax

**Use Windows equivalents:**
| Unix | Windows |
|------|---------|
| `rm -rf dir` | `rd /s /q dir` |
| `rm file` | `del file` |
| `mv src dest` | `move src dest` |
| `cp -r src dest` | `xcopy /E /I src dest` |
| `mkdir -p path` | `mkdir path` (fails if exists, use `if not exist`) |

Or use Git Bash for Unix-style commands.

## Anti-Patterns to Avoid

### Anti-Pattern 1: Moving before deleting nested .git

**What happens:** Parent git sees nested .git as a submodule-like entity, causing confusion.

**Prevention:** Always delete nested `.git` folder BEFORE moving files to parent tracking.

### Anti-Pattern 2: Deleting target/ with git rm

**What happens:** `git rm` fails because target/ is gitignored and not tracked.

**Prevention:** Use regular delete commands (`rd /s /q`) for untracked directories.

### Anti-Pattern 3: Committing mid-restructure

**What happens:** Partial commits leave project in broken state if something goes wrong.

**Prevention:** Complete all file operations, verify build works, THEN commit everything in one atomic commit.

### Anti-Pattern 4: Moving files individually instead of directories

**What happens:** Slow, error-prone, may miss files.

**Prevention:** Move entire directories (`git mv generations/island_selector/src ./src`) rather than individual files.

### Anti-Pattern 5: Not verifying build after restructure

**What happens:** Broken project committed, team blocked.

**Prevention:** Always run `mvn clean compile` before committing restructure.

## Quick Reference: Safe Operations Order

```
1. BACKUP entire project (non-negotiable)
2. CLOSE all IDEs and editors
3. ENABLE long paths (Windows)
4. DELETE nested .git folder
5. DELETE untracked build artifacts (target/)
6. DELETE unnecessary files (scripts, backups)
7. MOVE essential files to root (git mv)
8. DELETE empty nested directory
9. MERGE .gitignore files
10. VERIFY build (mvn clean compile)
11. COMMIT atomically
```

## Sources

**Git History Preservation:**
- [Git Move Files: Practical Renames and History Preservation](https://thelinuxcode.com/git-move-files-practical-renames-refactors-and-history-preservation-in-2026/)
- [Moving Files Between Git Repositories](https://gbayer.com/development/moving-files-from-one-git-repository-to-another-preserving-history/)
- [Git: Move Files Preserving History](https://gist.github.com/trongthanh/2779392)

**Nested Git Repositories:**
- [How to Remove a Nested Git Repo](https://medium.com/@mariam.nakanyike/how-to-remove-a-git-repo-nested-inside-another-one-6e53fb7f0163)
- [Using git clean for Nested Repositories](https://major.io/p/using-git-clean-to-remove-subdirectories-containing-git-repositories/)
- [Git Clean Documentation](https://git-scm.com/docs/git-clean)

**Windows Long Paths:**
- [Solving Windows Path Length Limitations in Git](https://www.shadynagy.com/solving-windows-path-length-limitations-in-git/)
- [Fixing Git Filename Too Long Errors](https://www.devopsroles.com/fixing-the-git-filename-too-long-error/)
- [Microsoft: Maximum Path Length Limitation](https://learn.microsoft.com/en-us/windows/win32/fileio/maximum-file-path-limitation)
- [Long Paths in Git on Windows (2025)](https://www.devlead.se/posts/2025/2025-02-19-git-windows-long-paths)

**Maven Project Structure:**
- [Understanding Maven Directory Structure](https://medium.com/@pvprasanth474/understanding-the-maven-directory-structure-a-simple-guide-7c7d636ec614)
- [Maven Getting Started Guide](https://maven.apache.org/guides/getting-started/)
