# IslandSelector

## What This Is

A BentoBox addon for Minecraft that provides island selection and management features. The project uses a standard Maven structure with Java 17.

## Core Value

Players can visually select their island location from a grid-based GUI.

## Current State

**Shipped:** v1.1 Simplification (2026-01-20)
**Codebase:** 80 Java files, 28,699 LOC
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

**v1.0 Cleanup:**
- Java addon source code — 80 main + 3 test files
- Maven build configuration — pom.xml at root
- Resource files — addon.yml, config.yml, en-US.yml
- Standard project structure — src/ and pom.xml at root
- Clean repository — no Python scripts, backups, or nested git
- Build verification — mvn clean compile succeeds

**v1.1 Simplification:**
- Config option `slots.enabled` to enable/disable slot system
- Block `/islandselector slots` command when slots disabled
- Hide slots button in GUI when slots disabled
- Add `/map` command alias for `/islandselector`
- Remove neighbors GUI button from main GUI
- Remove `/islandselector neighbors` command and source files

### Active

(Planning next milestone)

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
| Default slotsEnabled to true | Existing servers continue working after update | Done (v1.1) |
| Map alias as fourth param | After islandselector, is, isgrid | Done (v1.1) |
| Keep neighbors_online placeholder | Useful for scoreboards independently of GUI | Done (v1.1) |

## Milestones

See `.planning/MILESTONES.md` for shipped milestones.

---
*Last updated: 2026-01-20 after v1.1 milestone completion*
