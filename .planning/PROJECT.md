# IslandSelector Cleanup

## What This Is

A project cleanup for the IslandSelector BentoBox addon. The goal is to remove unused Python scripts, backup files, and generated files, then restructure so the Java addon source lives at the project root.

## Core Value

Clean, organized project structure with only the necessary addon source code and resources.

## Requirements

### Validated

- Existing Java addon source code — working in generations/island_selector/src/
- Maven build configuration — pom.xml exists
- Resource files — addon.yml, config.yml, locales

### Active

- [ ] Remove all Python scripts from project root
- [ ] Remove Python cache and generated files
- [ ] Remove backup Java files (*_backup.java)
- [ ] Remove prompts/ folder and other tooling
- [ ] Move src/ from generations/island_selector/ to project root
- [ ] Move pom.xml to project root
- [ ] Remove empty generations/ folder after move
- [ ] Clean up nested .git folder

### Out of Scope

- Modifying Java source code logic — cleanup only
- Adding new features — not part of this project
- Changing build configuration — just moving files

## Context

The project accumulated Python scripts and backup files during development. The actual addon source is buried under `generations/island_selector/`. This cleanup will make the project structure standard for a Maven Java project.

**Current structure:**
- Python tooling at root (agent.py, client.py, etc.)
- Java source nested under generations/island_selector/src/
- Backup files scattered (GridManager_backup.java, etc.)
- Nested .git folder in generations/island_selector/

**Target structure:**
```
IslandSelector/
├── src/
│   ├── main/java/world/bentobox/islandselector/...
│   └── main/resources/*.yml
├── pom.xml
└── .git/
```

## Constraints

- **Scope**: Only this project directory — do not touch files outside IslandSelector/
- **Preservation**: Keep all Java source and resource files intact

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Move src/ to root | Standard Maven project structure | - Pending |
| Delete nested .git | Only need one git repo at root | - Pending |

---
*Last updated: 2026-01-20 after initialization*
