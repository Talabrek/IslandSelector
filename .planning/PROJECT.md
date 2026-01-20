# IslandSelector

## What This Is

A BentoBox addon for Minecraft that provides island selection and management features. The project uses a standard Maven structure with Java 17.

## Core Value

Clean, organized BentoBox addon ready for development.

## Current State

**Shipped:** v1.0 Cleanup (2026-01-20)
**Codebase:** 83 Java files, 29,251 LOC
**Build:** Maven, Java 17, compiles successfully

```
IslandSelector/
├── src/
│   ├── main/java/world/bentobox/islandselector/
│   └── main/resources/ (addon.yml, config.yml, locales/)
├── pom.xml
└── .git/
```

## Requirements

### Validated

- Java addon source code — 80 main + 3 test files (v1.0)
- Maven build configuration — pom.xml at root (v1.0)
- Resource files — addon.yml, config.yml, en-US.yml (v1.0)
- Standard project structure — src/ and pom.xml at root (v1.0)
- Clean repository — no Python scripts, backups, or nested git (v1.0)
- Build verification — mvn clean compile succeeds (v1.0)

### Active

(None — cleanup complete, project ready for development)

### Out of Scope

- Modifying Java source code logic — cleanup only (v1.0 scope)
- Adding new features — not part of cleanup project

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Move src/ to root | Standard Maven project structure | Done (v1.0) |
| Delete nested .git | Only need one git repo at root | Done (v1.0) |
| Use git mv for moves | Preserves file history | Done (v1.0) |
| Replace agent README | Agent harness docs not relevant | Done (v1.0) |

## Milestones

See `.planning/MILESTONES.md` for shipped milestones.

---
*Last updated: 2026-01-20 after v1.0 milestone*
