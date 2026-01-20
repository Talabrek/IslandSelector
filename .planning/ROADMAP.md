# Roadmap: IslandSelector Cleanup

## Overview

This cleanup transforms a cluttered BentoBox addon project into a standard Maven structure. The work flows through four phases: consolidate the nested git repository, remove development artifacts (Python scripts, backups), move essential files to the project root, and verify the build works. Each phase depends on the previous one completing successfully.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3, 4): Planned milestone work
- Decimal phases (1.1, 2.1): Urgent insertions (marked with INSERTED)

- [ ] **Phase 1: Git Repository Consolidation** - Merge nested git history and establish single clean repo
- [ ] **Phase 2: Artifact Removal** - Delete Python scripts, backups, and development debris
- [ ] **Phase 3: Project Restructure** - Move Maven project to repository root
- [ ] **Phase 4: Build Verification** - Confirm project compiles and all files are present

## Phase Details

### Phase 1: Git Repository Consolidation
**Goal**: Single clean git repository at project root with consolidated import commit
**Depends on**: Nothing (first phase)
**Requirements**: GIT-01, GIT-02, GIT-03
**Success Criteria** (what must be TRUE):
  1. Running `git log` from project root shows the full development history
  2. No `.git` folder exists inside `generations/island_selector/`
  3. `git status` works correctly and shows clean state (or expected pending changes)
**Plans**: 1 plan

Plans:
- [ ] 01-01-PLAN.md - Delete nested .git and commit files to root repository

### Phase 2: Artifact Removal
**Goal**: Project contains only essential Java source and resources
**Depends on**: Phase 1
**Requirements**: REM-01, REM-02, REM-03, REM-04, REM-05, REM-06, REM-07, REM-08
**Success Criteria** (what must be TRUE):
  1. No Python files (*.py) exist anywhere in the project
  2. No backup Java files (*_backup*.java) exist anywhere in the project
  3. No `__pycache__/`, `prompts/`, or other development artifact folders exist
  4. Only Java source files, Maven config, and documentation remain
**Plans**: TBD

Plans:
- [ ] 02-01: Delete Python scripts and related files
- [ ] 02-02: Delete backup files and misc artifacts

### Phase 3: Project Restructure
**Goal**: Standard Maven project structure at repository root
**Depends on**: Phase 2
**Requirements**: MOV-01, MOV-02, MOV-03
**Success Criteria** (what must be TRUE):
  1. `src/` directory exists directly under project root
  2. `pom.xml` exists directly under project root
  3. `generations/` folder no longer exists (empty after moves)
**Plans**: TBD

Plans:
- [ ] 03-01: Move essential files and remove empty directories

### Phase 4: Build Verification
**Goal**: Confirmed working Maven project ready for development
**Depends on**: Phase 3
**Requirements**: VER-01, VER-02, VER-03
**Success Criteria** (what must be TRUE):
  1. `mvn clean compile` succeeds without errors
  2. All 85 Java source files are present in `src/`
  3. Resource files (addon.yml, config.yml, en-US.yml) are present
**Plans**: TBD

Plans:
- [ ] 04-01: Verify build and file integrity

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Git Repository Consolidation | 0/1 | Planned | - |
| 2. Artifact Removal | 0/2 | Not started | - |
| 3. Project Restructure | 0/1 | Not started | - |
| 4. Build Verification | 0/1 | Not started | - |

---
*Roadmap created: 2026-01-20*
*Total phases: 4 | Total plans: 5 | Depth: quick*
