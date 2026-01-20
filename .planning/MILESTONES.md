# Project Milestones: IslandSelector

## v1.1.1 Bugfixes (Shipped: 2026-01-21)

**Delivered:** Fixed three independent bugs: level display accuracy, warp sign-based visit controls, and relocation safety.

**Phases completed:** 8-10 (3 plans total)

**Key accomplishments:**

- Fixed island level display to match `/island level` command (overworld-only, non-cached)
- Created WarpIntegration for conditional visit controls based on warp sign availability
- Added location checks to relocation to prevent teleporting players who aren't on the island
- Zero breaking changes — backward compatible with existing setups
- All fixes follow established codebase patterns (reflection-based addon integration)

**Stats:**

- 80 Java source files (+1 WarpIntegration.java)
- 28,922 lines of Java code
- 3 phases, 3 plans, 9 tasks
- 2-day completion (2026-01-20 → 2026-01-21)

**Git range:** `2a787bb` → `9f78beb`

**What's next:** Planning next milestone.

---

## v1.1 Simplification (Shipped: 2026-01-20)

**Delivered:** Streamlined addon with configurable slot system, /map command alias, and neighbors feature removed.

**Phases completed:** 5-7 (3 plans total)

**Key accomplishments:**

- Added `slots.enabled` config toggle with command blocking and GUI button hiding
- Added `/map` as convenient alias for `/islandselector` command
- Removed unused neighbors feature (559 lines deleted)
- Defense-in-depth design with checks at command, GUI render, and click handler levels
- Maintained backward compatibility with `true` default for slot system

**Stats:**

- 80 Java source files (2 deleted)
- 28,699 lines of Java code
- 3 phases, 3 plans
- Same-day completion (2026-01-20)

**Git range:** `1cf42dd` → `7dd9fa9`

**What's next:** Planning next milestone.

---

## v1.0 Cleanup (Shipped: 2026-01-20)

**Delivered:** Transformed cluttered BentoBox addon project into clean, standard Maven structure.

**Phases completed:** 1-4 (5 plans total)

**Key accomplishments:**

- Consolidated nested git repository into single root repository
- Removed all Python scripts, backups, and development artifacts
- Restructured Maven project to standard layout at repository root
- Verified build succeeds with 80 source files compiling successfully

**Stats:**

- 83 Java source files
- 29,251 lines of Java code
- 4 phases, 5 plans
- Same-day completion (2026-01-20)

**Git range:** `edb40dd` → `bd6dbc1`

**What's next:** Project is ready for development. No further cleanup needed.

---
