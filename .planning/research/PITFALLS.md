# Domain Pitfalls: Java Project Cleanup and Restructuring

**Domain:** BentoBox addon project cleanup (brownfield)
**Researched:** 2026-01-20
**Context:** Moving files from `generations/island_selector/` to root, deleting Python scaffolding files and backups

---

## Critical Pitfalls

Mistakes that cause data loss, broken builds, or require significant recovery effort.

### Pitfall 1: Deleting Files Still Referenced by Build Configuration

**What goes wrong:** Deleting files that `pom.xml` or other build configurations reference, causing build failures that are not immediately obvious.

**Why it happens:** The build configuration may reference paths relative to the project root. When moving files, these references become invalid. Additionally, backup files (like `*_backup.java`) might be accidentally included in the build if naming patterns match.

**Consequences:**
- `mvn clean package` fails with cryptic errors
- IDE loses project configuration
- CI/CD pipelines break

**Prevention:**
1. Review `pom.xml` thoroughly before moving files
2. Check `<sourceDirectory>`, `<resources>`, and plugin configurations for path assumptions
3. Run `mvn clean package` immediately after restructuring to catch issues early
4. Note: Current pom.xml uses default Maven paths (`src/main/java`, `src/main/resources`) - these must exist at root level after move

**Detection:** Build fails with "source directory does not exist" or "cannot find symbol" errors

---

### Pitfall 2: Breaking Git History with Combined Move+Edit Commits

**What goes wrong:** Moving files and editing them in the same commit causes Git to treat the operation as delete+add instead of rename, losing file history.

**Why it happens:** Git uses content similarity heuristics (>50% match) to detect renames. Large edits combined with moves fall below this threshold. [Git does not store "rename" as a fundamental object](https://thelinuxcode.com/git-move-files-practical-renames-refactors-and-history-preservation-in-2026/) - it infers renames by comparing snapshots.

**Consequences:**
- `git log --follow` stops working at the move point
- `git blame` shows all lines as "new" at the move commit
- Historical context for code decisions is lost

**Prevention:**
1. **Separate renames from content changes** - commit moves first, then make any needed edits in a second commit
2. Use `git mv` for explicit intent (stages automatically, prevents accidental overwrites)
3. Verify with `git diff --staged --stat` - it should show "renamed" not "deleted/added"
4. For this project: Move the entire `generations/island_selector/` contents to root in one clean commit, no edits

**Detection:** `git status` shows deletions and additions instead of renames after staging

---

### Pitfall 3: Losing the Nested .git Directory Data

**What goes wrong:** The project has a `.git` directory inside `generations/island_selector/`. Moving files carelessly could either:
- Overwrite the root project's git state
- Lose commit history from the nested repo
- Create a corrupted git state

**Why it happens:** The current structure has:
- Root level: No `.git` (not a git repo at root)
- `generations/island_selector/.git/`: Contains the actual git repository

This is an unusual structure that requires careful handling.

**Consequences:**
- Complete loss of version control history
- Corrupted repository state
- Unable to push/pull from remotes

**Prevention:**
1. **First:** Move the `.git` directory from `generations/island_selector/` to root
2. **Then:** Update git's working directory awareness
3. **Then:** Move source files
4. OR: Initialize fresh repo at root and use `git filter-repo` to preserve history
5. Back up the entire `generations/island_selector/.git/` directory before any operations

**Detection:** `git status` fails or shows unexpected state after move

---

### Pitfall 4: Windows File Locking During Deletion

**What goes wrong:** Files cannot be deleted because another process has them locked. Common on Windows with:
- IDE (VS Code, IntelliJ) indexing files
- Java processes with loaded JARs
- Python interpreter with cached `.pyc` files

**Why it happens:** [Windows locks files that are in use](https://forum.inductiveautomation.com/t/unblock-and-delete-files-that-have-been-locked-by-java/60194), preventing deletion until the handle is released. [Even after closing Java applications, Introspector can maintain locks on JAR files](https://bugs.openjdk.org/browse/JDK-8231454).

**Consequences:**
- Maven clean fails: "Failed to delete target directory"
- Cannot delete backup `.java` files
- Cannot remove `__pycache__` directories

**Prevention:**
1. Close all IDEs and terminals before restructuring
2. For Maven: Use `-Dmaven.clean.failOnError=false` if needed
3. For Python cache: Delete `__pycache__` directories first (they lock `.pyc` files)
4. Use `taskkill /F /IM java.exe` if Java processes linger
5. Wait a few seconds after closing apps before attempting deletions

**Detection:** "Access denied" or "file in use" errors during deletion

---

## Moderate Pitfalls

Mistakes that cause delays, confusion, or technical debt.

### Pitfall 5: Forgetting to Update Import Statements

**What goes wrong:** After moving Java files, the package declarations and import statements may reference old locations.

**Why it happens:** Moving files changes their filesystem path but not their internal package declarations. Java requires package declarations to match directory structure.

**Consequences:**
- Compilation errors: "package does not exist"
- Runtime errors: `ClassNotFoundException`
- IDE shows errors in all files

**Prevention:**
1. In this project, the package structure (`world.bentobox.islandselector`) is already correct
2. The issue is the files are nested too deep - moving to root maintains the correct package
3. Verify: After move, `src/main/java/world/bentobox/islandselector/IslandSelector.java` should have `package world.bentobox.islandselector;`
4. No import changes needed if moving entire `src/` tree together

**Detection:** IDE shows red underlines on package declarations after move

---

### Pitfall 6: Orphaning Configuration Files

**What goes wrong:** Moving Java source but forgetting associated configuration files:
- `src/main/resources/addon.yml` (BentoBox addon descriptor)
- `src/main/resources/config.yml` (default configuration)
- `src/main/resources/locales/*.yml` (language files)

**Why it happens:** Focus on `.java` files while forgetting the `resources/` directory that must accompany them.

**Consequences:**
- Addon fails to load: "addon.yml not found"
- Missing default configuration
- Missing translations

**Prevention:**
1. Move the entire `src/` directory structure, not just `src/main/java/`
2. Verify `src/main/resources/` is included
3. Check for any resources outside the standard Maven structure

**Detection:** Plugin fails to enable with "Cannot load addon" errors

---

### Pitfall 7: Leaving Hidden State in Build Directories

**What goes wrong:** Old `target/` directories or compiled classes from the old location interfere with the new build.

**Why it happens:** [When a build behaves differently after a clean, that's hidden state](https://thelinuxcode.com/maven-build-lifecycle-a-practical-2026-guide/). Old `.class` files may be picked up, or stale artifacts may mask missing dependencies.

**Consequences:**
- Build appears to work but uses stale classes
- "Works on my machine" failures in CI
- Mysterious runtime behavior

**Prevention:**
1. Delete ALL `target/` directories before moving: both at root and nested
2. Run `mvn clean` before AND after restructuring
3. Clear IDE caches: IntelliJ "Invalidate Caches", VS Code delete `.vscode/` cache
4. Delete `*.class` files if found outside `target/`

**Detection:** Build succeeds locally but fails in clean CI environment

---

### Pitfall 8: Removing Backup Files That Are Actually Needed

**What goes wrong:** Files named `*_backup.java` or `*_backup_session*.java` might contain important code that was preserved intentionally.

**Why it happens:** The project contains:
- `MainGridGUI_backup.java`
- `GridManager_backup.java`
- `NeighborhoodGUI_backup_session44.java`

These may have been created during development to preserve working versions before risky changes.

**Consequences:**
- Loss of potentially important fallback code
- Cannot recover if current versions have regressions

**Prevention:**
1. **Review each backup file before deletion**
2. Compare backup content with current version using `diff`
3. If backup contains unique code, consider:
   - Merging it into the main file
   - Creating a proper git branch/tag for the backup state
   - Documenting what the backup preserved
4. Only delete after confirming the main version is complete and stable

**Detection:** Functionality regression after deleting "backup" files

---

## Minor Pitfalls

Mistakes that cause annoyance but are easily fixable.

### Pitfall 9: Leaving Python Artifacts Behind

**What goes wrong:** Python scaffolding cleanup misses:
- `__pycache__/` directories
- `.pyc` and `.pyo` compiled files
- Virtual environment directories

**Why it happens:** These directories/files are often hidden or ignored by file browsers.

**Consequences:**
- Repository clutter
- Unnecessary files in commits
- Confusion about project type

**Prevention:**
1. Delete `__pycache__/` directories:
   ```powershell
   Get-ChildItem -Path . -Include __pycache__ -Recurse -Directory | Remove-Item -Recurse -Force
   ```
2. Delete `.pyc` files:
   ```powershell
   Get-ChildItem -Path . -Include *.pyc -Recurse | Remove-Item -Force
   ```
3. Check `.gitignore` is updated to exclude Python artifacts going forward
4. Files to remove in this project: `*.py`, `requirements.txt`, `prompts/`, `__pycache__/`

**Detection:** `git status` shows unexpected Python files after cleanup

---

### Pitfall 10: Forgetting to Update .gitignore

**What goes wrong:** After restructuring, the `.gitignore` patterns no longer match the new file locations.

**Why it happens:** The current `.gitignore` ignores `generations/` which won't be relevant after moving files to root.

**Consequences:**
- Build artifacts get committed
- IDE files get committed
- Noise in repository

**Prevention:**
1. Review `.gitignore` after restructuring
2. Add standard Java/Maven ignores:
   ```
   target/
   *.class
   *.jar
   *.log
   .idea/
   *.iml
   .vscode/
   ```
3. Remove patterns that no longer apply (like `generations/`)

**Detection:** `git status` shows files that should be ignored

---

### Pitfall 11: IDE Project Files Pointing to Wrong Locations

**What goes wrong:** IntelliJ `.idea/` or Eclipse `.project` files contain absolute or relative paths to the old structure.

**Why it happens:** IDEs cache project configuration including source roots, module paths, and build configurations.

**Consequences:**
- IDE cannot find sources
- Build within IDE fails
- Must reconfigure project

**Prevention:**
1. Delete IDE configuration files before moving:
   - `.idea/` directory
   - `*.iml` files
   - `.project`, `.classpath` (Eclipse)
   - `.vscode/` (VS Code)
2. Re-import project as Maven project after restructuring
3. Let IDE regenerate from `pom.xml`

**Detection:** IDE shows "Project SDK not defined" or cannot resolve symbols

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| Moving .git directory | Pitfall 3: Corrupted git state | Back up .git first, test after move |
| Deleting Python files | Pitfall 9: Leftover __pycache__ | Use recursive PowerShell command |
| Deleting backup .java files | Pitfall 8: Loss of needed code | Review diff before deletion |
| Moving pom.xml to root | Pitfall 1: Broken build paths | Run mvn clean package immediately |
| Final cleanup | Pitfall 10: Bad .gitignore | Review and update patterns |

---

## Pre-Restructuring Checklist

Before starting any file operations:

- [ ] Back up the entire `generations/island_selector/.git/` directory externally
- [ ] Close all IDEs (VS Code, IntelliJ, Eclipse)
- [ ] Close all terminal windows in the project directory
- [ ] Kill any running Java processes: `taskkill /F /IM java.exe`
- [ ] Document the current working state (does `mvn clean package` succeed?)
- [ ] Review the three `*_backup*.java` files - determine if they contain unique code
- [ ] List all files that will be deleted (Python scripts, backup files)

## Post-Restructuring Checklist

After completing file operations:

- [ ] Verify git status shows moves, not deletes/adds
- [ ] Run `mvn clean package` - must succeed
- [ ] Verify `target/IslandSelector-*.jar` is created
- [ ] Update `.gitignore` for new structure
- [ ] Delete orphaned directories (`generations/`, `__pycache__/`)
- [ ] Commit with clear message: "chore: restructure project to root level"

---

## Sources

- [Git Move Files: History Preservation in 2026](https://thelinuxcode.com/git-move-files-practical-renames-refactors-and-history-preservation-in-2026/) - Git rename detection heuristics
- [Maven Build Lifecycle 2026](https://thelinuxcode.com/maven-build-lifecycle-a-practical-2026-guide/) - Hidden state in builds
- [Windows File Locking with Java](https://forum.inductiveautomation.com/t/unblock-and-delete-files-that-have-been-locked-by-java/60194) - File locking issues
- [OpenJDK JAR Locking Bug](https://bugs.openjdk.org/browse/JDK-8231454) - Introspector leak causing file locks
- [Always move and rename Git files in isolated commits](https://www.theserverside.com/blog/Coffee-Talk-Java-News-Stories-and-Opinions/Always-move-and-rename-Git-files-in-an-isolated-commit) - Best practice for preserving history
- [Remove __pycache__ folders](https://bobbyhadz.com/blog/python-remove-pycache-folders-and-pyc-files) - Python cleanup methods
- [Maven Common Bugs](https://maven.apache.org/plugin-developers/common-bugs.html) - Official Maven pitfall documentation
