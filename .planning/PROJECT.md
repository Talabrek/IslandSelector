# IslandSelector

## What This Is

A BentoBox addon for Minecraft that provides island selection and management features. The project uses a standard Maven structure with Java 17.

## Core Value

Players can visually select their island location from a grid-based GUI.

## Current Milestone: v1.1 Simplification

**Goal:** Streamline the addon by adding configurability and removing unused features.

**Target features:**
- Config option to disable slot system (hides GUI button, blocks command)
- /map command alias for /islandselector
- Remove neighbors GUI and command entirely

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

- [ ] Config option to enable/disable slot system
- [ ] Block /islandselector slots command when slots disabled
- [ ] Hide slots button in GUI when slots disabled
- [ ] Add /map command alias for /islandselector
- [ ] Remove neighbors GUI button from main GUI
- [ ] Remove /islandselector neighbors command

### Out of Scope

- Modifying slot system logic — only adding enable/disable toggle (v1.1 scope)
- Other GUI changes — only removing neighbors button (v1.1 scope)

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
*Last updated: 2026-01-20 after v1.1 milestone start*
