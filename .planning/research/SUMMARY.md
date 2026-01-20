# Project Research Summary

**Project:** IslandSelector Brownfield Cleanup
**Domain:** BentoBox Addon (Maven Java Project) - Project Restructure
**Researched:** 2026-01-20
**Confidence:** HIGH

## Executive Summary

This is a brownfield cleanup project, not a greenfield build. The IslandSelector BentoBox addon already exists as a complete Java/Maven project, but it is buried inside a `generations/island_selector/` subdirectory and surrounded by Python scaffolding files from an AI code generation tool. The goal is to restructure this into a standard Maven project at the repository root.

The recommended approach is a phased cleanup: first handle the nested git repository situation (the actual project history lives in `generations/island_selector/.git/`), then systematically remove development artifacts (Python scripts, backup files, build outputs), and finally flatten the Maven project structure to the repository root. The order of operations is critical - moving files before handling the nested `.git` directory will cause history loss or corruption.

Key risks are data loss from mishandling the nested git repository, broken builds from path reference issues in `pom.xml`, and Windows-specific file locking during deletion. All risks are mitigable with proper sequencing and verification steps. A full backup before starting is non-negotiable.

## Key Findings

### Target Project Structure

The standard BentoBox addon follows Maven conventions. After cleanup, the project root should contain only:

**Required files:**
- `pom.xml` - Maven project definition (Java 17+, Paper 1.20.4+, BentoBox 2.4.0+)
- `src/main/java/` - 82 production Java source files
- `src/main/resources/` - addon.yml, config.yml, locales/en-US.yml
- `src/test/java/` - 3 test files
- `.gitignore` - Standard Maven + Java ignores

**Keep (recommended):**
- `README.md` - Project documentation
- `CLAUDE.md` - Development context documentation
- `LICENSE` - EPL-2.0 or similar

**Remove (everything else):**
- Python files (17 files) - AI generation scaffolding
- Backup files (11 files) - Duplicated source code
- Nested `.git/` directory - Must be handled specially
- Shell scripts (3 files) - Development tooling
- `target/` directories - Build artifacts
- `__pycache__/` - Python bytecode
- `nul`, `prompts/`, `output/` - Misc artifacts

### Files Requiring Action

**From FEATURES.md - Summary table:**

| Category | Count | Action |
|----------|-------|--------|
| Essential Java files | 85 | KEEP |
| Essential resources | 3 | KEEP |
| Backup files | 11 | REMOVE |
| Python files | 17 | REMOVE |
| Nested .git | 1 dir | HANDLE FIRST |
| Shell scripts | 3 | REMOVE |
| Build artifacts | 1 dir | CLEAN |

### Architecture Approach

From ARCHITECTURE.md - the critical insight is that this project has a nested git repository inside `generations/island_selector/.git/` while the parent has minimal or no history. The restructure must either abandon the nested history (simpler) or merge it into the parent (preserves commits). Given this is a functioning addon with meaningful development history, preserving history is recommended but not mandatory.

**Order of operations:**
1. Backup entire project
2. Close all IDEs/editors
3. Enable Windows long paths if needed
4. Handle nested .git directory
5. Delete build artifacts and development scripts
6. Move essential files to root
7. Update .gitignore
8. Verify build
9. Commit atomically

### Critical Pitfalls

From PITFALLS.md - top 5 critical issues:

1. **Nested .git corruption** - Handle `.git` directory BEFORE moving files. Back up externally first.
2. **History loss from combined move+edit** - Commit file moves separately from content changes. Git needs >50% content similarity to detect renames.
3. **Build path breakage** - Maven `pom.xml` uses default paths (`src/main/java`). Must exist at root after move. Run `mvn clean package` immediately after restructure.
4. **Windows file locking** - Close all IDEs, kill Java processes before deletions. Use `-Dmaven.clean.failOnError=false` if needed.
5. **Backup files containing unique code** - Review `*_backup.java` files before deletion. Three files exist: `MainGridGUI_backup.java`, `GridManager_backup.java`, `NeighborhoodGUI_backup_session44.java`. Diff against current versions first.

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Preparation and Backup
**Rationale:** Safety net is non-negotiable for destructive operations
**Delivers:** External backup, documented current state
**Addresses:** Risk mitigation
**Avoids:** Pitfall 3 (nested .git data loss)

Tasks:
- Document current git state in both repos
- Create external backup of entire directory
- Close all IDEs and editors
- Verify current project builds (`mvn clean package`)
- Enable Windows long paths if needed

### Phase 2: Handle Git Repository Structure
**Rationale:** Must be resolved BEFORE any file moves to prevent corruption
**Delivers:** Single clean git repository at project root
**Addresses:** Nested repository problem
**Avoids:** Pitfall 3 (corrupted git state)

Tasks:
- Decision: Abandon nested history OR merge into parent
- If abandoning: Delete `generations/island_selector/.git/`
- If merging: Use `git remote add` + merge with `--allow-unrelated-histories`
- Verify `git status` works correctly after

### Phase 3: Remove Development Artifacts
**Rationale:** Clean before moving - reduces complexity and potential file lock issues
**Delivers:** Clean nested directory with only essential files
**Avoids:** Pitfall 9 (leftover Python artifacts), Pitfall 4 (file locking)

Tasks (in order):
1. Delete `__pycache__/` directories
2. Delete Python files (*.py, requirements.txt)
3. Delete shell scripts (*.sh)
4. Delete `prompts/` directory
5. Delete `nul` file
6. Delete `target/` directories
7. Review and delete backup files (after comparing with originals)
8. Delete misc files (feature_list.json, app_spec.txt, .claude_settings.json)

### Phase 4: Flatten Project Structure
**Rationale:** With artifacts removed, move clean Maven project to root
**Delivers:** Standard Maven project structure at repository root
**Addresses:** Proper project layout
**Avoids:** Pitfall 2 (history loss from combined operations)

Tasks:
- Move `pom.xml` to root
- Move `src/` directory to root (preserves test files and resources)
- Move `CLAUDE.md`, `README.md`, `DEVELOPMENT_NOTES.md` to root
- Remove empty `generations/` directory
- Do NOT edit any files during this phase

### Phase 5: Configuration and Verification
**Rationale:** Ensure project is functional before committing
**Delivers:** Working Maven project, proper ignore rules
**Avoids:** Pitfall 1 (broken build), Pitfall 10 (bad .gitignore)

Tasks:
- Merge/update `.gitignore` for new structure
- Run `mvn clean package` - must succeed
- Verify JAR is created
- Re-import project in IDE (let it regenerate from pom.xml)
- Run tests if applicable

### Phase 6: Final Commit
**Rationale:** Atomic commit after verification ensures clean state
**Delivers:** Committed, clean project structure

Tasks:
- `git add .`
- Commit with message: "chore: restructure project - flatten Maven layout to root"
- Verify commit looks clean (moves, not delete+add)

### Phase Ordering Rationale

- **Backup first** because all subsequent operations are destructive
- **Git structure second** because file moves behave differently with nested repos
- **Delete before move** because it reduces complexity and file counts
- **Move as single phase** to preserve Git's rename detection
- **Verify before commit** because catching issues before commit is far easier than fixing after
- **Atomic commit last** to maintain clean project history

### Research Flags

Phases with standard patterns (no additional research needed):
- **Phase 1 (Backup):** Standard backup procedures
- **Phase 3 (Cleanup):** Standard file deletion
- **Phase 5 (Verify):** Standard Maven build verification
- **Phase 6 (Commit):** Standard git operations

Phases requiring care but not research:
- **Phase 2 (Git structure):** Well-documented but requires decision on history preservation
- **Phase 4 (Flatten):** Standard but order-dependent - follow exact sequence

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Verified against official BentoBox repositories and documentation |
| Features | HIGH | Direct filesystem analysis, clear categorization |
| Architecture | HIGH | Git and Maven patterns are well-documented |
| Pitfalls | HIGH | Based on official documentation and real-world case studies |

**Overall confidence:** HIGH

### Gaps to Address

- **Backup file review:** The three `*_backup.java` files should be diffed against current versions before deletion. This is a manual review step.
- **History preservation decision:** Requires human decision whether nested repo history is worth preserving. Research provides both approaches.
- **Windows environment specifics:** Long path enabling may require admin privileges. Have fallback plan if registry changes are not possible.

## Sources

### Primary (HIGH confidence)
- [BentoBox Official Documentation](https://docs.bentobox.world/en/latest/Tutorials/api/Create-an-addon/) - Addon structure requirements
- [BentoBox GitHub Repository](https://github.com/BentoBoxWorld/BentoBox) - Reference implementation
- [Level Addon Repository](https://github.com/BentoBoxWorld/Level) - Reference addon
- [Maven Getting Started Guide](https://maven.apache.org/guides/getting-started/) - Standard directory layout
- [Git Documentation](https://git-scm.com/docs/git-clean) - Git clean for nested repos

### Secondary (MEDIUM confidence)
- [Git Move Files: History Preservation in 2026](https://thelinuxcode.com/git-move-files-practical-renames-refactors-and-history-preservation-in-2026/) - Rename detection heuristics
- [Microsoft Long Paths Documentation](https://learn.microsoft.com/en-us/windows/win32/fileio/maximum-file-path-limitation) - Windows path limits

### Tertiary (LOW confidence)
- Community examples and forum posts - Supplementary context only

---
*Research completed: 2026-01-20*
*Ready for roadmap: yes*
