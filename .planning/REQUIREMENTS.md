# Requirements: IslandSelector Cleanup

**Defined:** 2026-01-20
**Core Value:** Clean, organized project structure with only necessary addon source code

## v1 Requirements

### Git Structure
- [ ] **GIT-01**: Merge nested .git history into root repository
- [ ] **GIT-02**: Remove nested .git folder after history merge
- [ ] **GIT-03**: Verify all historical commits accessible from root repo

### File Removal
- [ ] **REM-01**: Delete all Python files from root (agent.py, client.py, progress.py, prompts.py, test_security.py, autonomous_agent_demo.py)
- [ ] **REM-02**: Delete requirements.txt from root
- [ ] **REM-03**: Delete prompts/ folder
- [ ] **REM-04**: Delete __pycache__/ folder
- [ ] **REM-05**: Delete Python scripts from generations/island_selector/ (add_method.py, etc.)
- [ ] **REM-06**: Delete backup Java files (MainGridGUI_backup.java, GridManager_backup.java, NeighborhoodGUI_backup_session44.java)
- [ ] **REM-07**: Delete shell scripts (init.sh)
- [ ] **REM-08**: Delete misc files (nul, app_spec.txt, feature_list.json)

### File Movement
- [ ] **MOV-01**: Move src/ from generations/island_selector/ to project root
- [ ] **MOV-02**: Move pom.xml from generations/island_selector/ to project root
- [ ] **MOV-03**: Remove empty generations/ folder after moves

### Verification
- [ ] **VER-01**: Run mvn clean compile to verify project builds
- [ ] **VER-02**: Verify all 85 Java source files present
- [ ] **VER-03**: Verify resource files present (addon.yml, config.yml, en-US.yml)

## v2 Requirements

(None - cleanup is complete after v1)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Modifying Java source code | Cleanup only, no logic changes |
| Adding new features | Not part of cleanup scope |
| Changing build configuration | Just moving pom.xml, not editing it |
| Creating documentation | Focus on file cleanup only |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| GIT-01 | Phase 1 | Pending |
| GIT-02 | Phase 1 | Pending |
| GIT-03 | Phase 1 | Pending |
| REM-01 | Phase 2 | Pending |
| REM-02 | Phase 2 | Pending |
| REM-03 | Phase 2 | Pending |
| REM-04 | Phase 2 | Pending |
| REM-05 | Phase 2 | Pending |
| REM-06 | Phase 2 | Pending |
| REM-07 | Phase 2 | Pending |
| REM-08 | Phase 2 | Pending |
| MOV-01 | Phase 3 | Pending |
| MOV-02 | Phase 3 | Pending |
| MOV-03 | Phase 3 | Pending |
| VER-01 | Phase 4 | Pending |
| VER-02 | Phase 4 | Pending |
| VER-03 | Phase 4 | Pending |

**Coverage:**
- v1 requirements: 17 total
- Mapped to phases: 17
- Unmapped: 0

---
*Requirements defined: 2026-01-20*
*Last updated: 2026-01-20 after initial definition*
