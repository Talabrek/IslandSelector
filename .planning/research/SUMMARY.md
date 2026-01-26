# Nova Integration Research Summary

**Project:** IslandSelector v1.2 Nova Integration Milestone
**Date:** 2026-01-27
**Synthesis of:** STACK.md, FEATURES.md, ARCHITECTURE.md, PITFALLS.md

---

## Executive Summary

Nova is a server-side modding framework that adds custom blocks (machines, storage, cables) to Minecraft without client modifications. The critical challenge is that **Nova stores block data outside Minecraft's NBT system**, making this data invisible to WorldEdit/FAWE operations. When islands are relocated or slots are switched, Nova machines appear as vanilla backing blocks without their custom functionality, inventories, or energy state.

IslandSelector already has a basic integration (`NovaIntegration.java`) that captures and restores Nova block types and positions using a reflection-based pattern. This prevents catastrophic data loss (blocks disappearing entirely), but the integration is incomplete - it only preserves block types, not machine state (inventories, energy, progress). The v1.2 milestone aims to enhance this integration to preserve full TileEntity state, making Nova support production-ready.

The path forward is clear: **no stack changes are required** (Nova API v0.21.0 already present in pom.xml), but the current NovaIntegration.java implementation needs updates to Nova 0.17+ API patterns, TileEntity state preservation, and proper cleanup. The integration must follow the established EntityStorage pattern - parallel capture/restore logic that hooks into SlotSwitchManager, RelocationManager, and BackupManager at the same points where entity handling occurs.

---

## Key Findings

### From STACK.md

**Stack Assessment: NO CHANGES NEEDED**

The project is already correctly configured:
- Nova Maven repository configured: `https://repo.xenondevs.xyz/releases`
- Nova API dependency present: `xyz.xenondevs.nova:nova-api:0.21.0`
- Dependency scope correct: `provided` (runtime-only, not bundled)
- Integration pattern correct: Reflection-based optional integration

**Optional version update:**
- Nova API 0.21.0 → 0.22.1 (released Jan 20, 2025)
- Rationale: Latest stable, minimal API changes, better bug fixes
- Impact: No breaking changes expected, but requires validation testing
- Note: Nova 0.22.x requires Java agent flag for server startup (`-javaagent:plugins/Nova-0.22.1.jar`)

**Key technology decisions validated:**
- Continue using reflection-based integration (matches BentoBox ecosystem patterns)
- Avoid hard Nova dependency (graceful degradation is critical)
- Don't add Kotlin runtime separately (provided transitively)
- Don't change scope from `provided` to `compile`

**Compatibility confirmed:**
- Minecraft 1.20.4 (project target) supported by Nova 0.22.1
- Java 17 requirement met
- No version conflicts with Paper API, BentoBox, FAWE

**API stability assessment: MEDIUM**
- Nova has history of breaking changes between minor versions (0.16→0.17, 0.17→0.18)
- API changes occur approximately every 3-6 months
- Mitigation: Reflection pattern already isolates codebase from changes
- Current NovaIntegration.java tries multiple API paths for version resilience

### From FEATURES.md

**Current Implementation: Basic MVP Complete**

Table stakes (must-haves) mostly implemented:
- ✓ Block type preservation - Nova blocks remain Nova blocks after operations
- ✓ Block position accuracy - Relative coordinates maintained
- ✓ Graceful degradation - Works without Nova installed
- ✓ Version resilience - Tries multiple API paths
- ⚠ Multi-dimension support - Exists but needs verification

**Critical Missing Features (High User Impact):**

1. **Inventory preservation (CRITICAL)** - Most impactful gap
   - Current state: Machine inventories empty after operations
   - User expectation: "Switching slots is like switching save files - everything should be as I left it"
   - Impact: Feels like data loss, not state reset
   - Scenario: Player has Pulverizer with 500 diamonds queued → relocates → diamonds vanish
   - Complexity: HIGH - requires TileEntity API research and serialization

2. **Energy/progress preservation (HIGH)** - Noticeable quality difference
   - Current state: Energy levels and processing progress reset
   - User expectation: Acceptable for relocations (major operation), but missed opportunity
   - Impact: Professional integration vs basic integration
   - Complexity: HIGH - may not be exposed via API

3. **Machine upgrade preservation (HIGH)** - Affects invested resources
   - Current state: Speed/efficiency upgrades lost
   - User expectation: Upgrades are expensive, losing them feels punitive
   - Impact: Players reluctant to relocate with upgraded machines
   - Complexity: HIGH - requires Simple Upgrades addon API research

**Quick Win Features (Low Effort, High Value):**

1. **Visual feedback (MEDIUM priority)** - Build user confidence
   - Example: "✓ Preserved 23 Nova machines with inventories"
   - Complexity: LOW - just player messages
   - Impact: Immediate UX improvement

2. **Error reporting (MEDIUM priority)** - Prevent confusion
   - Current: Failures silent to players (console-only)
   - Example: "⚠ 3 Nova machines couldn't be restored"
   - Complexity: LOW - catch failures, show counts
   - Impact: Users know when something went wrong

3. **Multi-dimension verification (MEDIUM priority)** - Validate existing code
   - Current: Integration exists but untested with Nova in nether/end
   - Complexity: LOW - testing only
   - Impact: Prevents catastrophic loss in nether/end

**Anti-Features (Don't Build):**

- **Perfect state replication** - Impossible without deep Nova cooperation. Focus on user-visible state only.
- **Schematic data embedding** - WorldEdit format incompatibility and corruption risk. Keep separate storage.
- **Manual block-by-block copy** - Performance disaster. Use WorldEdit for bulk, Nova for overlay.
- **Network auto-reconnection** - Too complex, error-prone. Document manual re-wiring instead.
- **GUI-based restoration** - Adds friction. Automatic is better.

**User Scenarios:**

- **Slot switching (most common)**: Inventory preservation is CRITICAL. Energy/progress reset is tolerable if documented.
- **Island relocation**: Same as slot switching. Users tolerate more state loss but inventory loss still unacceptable.
- **Multi-dimension operations**: If it works in overworld, must work in nether/end. Verification needed.

### From ARCHITECTURE.md

**Current Architecture: Sound Foundation**

IslandSelector uses modular integration pattern:
```
integrations/
├── NovaIntegration - Detection + reflection-based capture/restore (NEEDS UPDATES)
├── WorldEditIntegration - FAWE async wrappers
└── Others - Optional plugin integrations

managers/
├── SlotSwitchManager - Orchestrates slot switching
├── RelocationManager - Orchestrates island moves
├── BackupManager - Orchestrates backup creation
└── Others - Grid, dimension, etc.

utils/
├── SchematicUtils - FAWE operations wrapper
└── EntityStorage - Entity capture/restore (parallel pattern for Nova)
```

**Nova Storage Model:**

Nova stores data separately from Minecraft chunks:
```
Minecraft World
├── Chunk data (blocks, biomes)
├── Tile entities (vanilla)
└── [Nova's separate storage] ← FAWE can't copy this
    ├── WorldDataManager - Global manager
    ├── NovaBlockState - Block ID + state
    └── TileEntity instances - Inventory/energy/progress
```

**Why WorldEdit/FAWE Can't Copy Nova Blocks:**
- WorldEdit copies Minecraft chunk data only
- Nova data stored outside chunks in custom format
- When pasted, blocks appear as vanilla backing blocks
- TileEntity data completely lost

**Required API Updates (Nova 0.17+ Changes):**

Current code uses outdated Nova 0.16 API:
```java
// OLD (0.16) - Current code
BlockManager class exists
TileEntityManager exists

// NEW (0.17+) - Required
BlockManager removed → use BlockUtils
TileEntityManager removed → use WorldDataManager
```

**Integration Points Identified:**

1. **SlotSwitchManager:**
   - saveAllDimensionIslands() → Add NovaIntegration.captureNovaBlocks()
   - clearAllDimensionIslands() → Add NovaIntegration.removeNovaBlocks()
   - loadAllDimensionIslands() → Add NovaIntegration.restoreNovaBlocks()

2. **RelocationManager:**
   - captureIslandData() → Add NovaIntegration.captureNovaBlocksInMemory()
   - removeBlocks() → Add NovaIntegration.removeNovaBlocks()
   - pasteAtNewLocation() → Add NovaIntegration.restoreNovaBlocksInMemory()

3. **BackupManager:**
   - saveSlotToBackup() → Add NovaIntegration.saveNovaBlocks()
   - loadBackupToWorld() → Add NovaIntegration.loadNovaBlocks()

**Data Storage Format:**

Current `NovaBlockData` insufficient - only stores block ID:
```java
// CURRENT (incomplete)
class NovaBlockData {
    int relX, relY, relZ;
    String blockId;
}

// REQUIRED (full state)
class NovaBlockData {
    int relX, relY, relZ;
    String blockId;
    byte[] tileEntityData;  // Serialized inventory/energy/progress
    String blockStateData;  // Block properties
}
```

**Threading Requirements:**

- `captureNovaBlocks()` - Can run async (WorldDataManager reads are thread-safe)
- `removeNovaBlocks()` - Must run on main thread (modifies world)
- `restoreNovaBlocks()` - Must run on main thread (places blocks)

**Performance Considerations:**

- Island size: 400x400 blocks = 160,000 blocks per dimension
- Height range: -64 to 320 = 384 blocks
- Total scan volume: 61.4M blocks per dimension
- Typical Nova blocks per island: 50-200
- Target scan time: < 5 seconds
- Strategy: Async chunk loading (already implemented), batch operations

**Components to Modify:**

1. **NovaIntegration.java (MAJOR REWRITE)**
   - Update reflection paths for Nova 0.17 API
   - Fix capture to use WorldDataManager.getTileEntity()
   - Fix restore to use BlockUtils.placeBlock()
   - Add removeNovaBlocks() method (currently missing)
   - Add in-memory capture/restore for relocation
   - Expand serialization to capture full TileEntity state

2. **SlotSwitchManager.java (MINOR ADDITIONS)**
   - Add Nova capture/remove/restore calls at identified points
   - Handle dimension-aware file paths
   - Error handling: log failures but continue

3. **RelocationManager.java (MINOR ADDITIONS)**
   - Add in-memory Nova capture parallel to entities
   - Pass captured data through workflow

4. **BackupManager.java (MINOR ADDITIONS)**
   - Add Nova save/load after schematic operations
   - Handle multi-dimension file paths

**No new components needed** - existing architecture is sound.

### From PITFALLS.md

**Critical Pitfalls (Data Loss or Rewrites Required):**

1. **Using WorldEdit/FAWE to copy Nova blocks**
   - Problem: WorldEdit can't handle Nova's custom NBT format (CBFCompoundTag)
   - Consequence: All Nova blocks silently lost during operations
   - Prevention: Separate Nova capture system (already implemented)
   - Phase impact: MUST be maintained in all phases

2. **Missing TileEntity inventory state**
   - Problem: Current code only captures block IDs, not TileEntity data
   - Consequence: Inventories empty, energy reset, progress lost, upgrades gone
   - Prevention: Use `TileEntity.getDrops(true)` to capture full state
   - Phase impact: CRITICAL for Phase 2 (state preservation)

3. **Not using BlockManager for removal**
   - Problem: Using `Block.setType(AIR)` leaves orphaned TileEntity data
   - Consequence: Memory leaks, ghost blocks, chunk data corruption
   - Prevention: Use Nova's BlockManager.removeBlock() for cleanup
   - Phase impact: CRITICAL for Phase 3 (proper cleanup)

4. **Chunk unload during async operations**
   - Problem: TileEntity state lost if chunks unload during capture
   - Consequence: Intermittent data loss, hard-to-reproduce bugs
   - Prevention: Pre-load chunks, keep loaded during operation (already implemented)
   - Phase impact: Already handled, verify in testing

**Moderate Pitfalls (Delays or Technical Debt):**

5. **Missing Context parameter for block placement**
   - Problem: Blocks place without proper initialization
   - Consequence: Drop tables missing, network connections fail
   - Prevention: Provide proper Context to BlockUtils.placeBlock()
   - Phase impact: Fix in Phase 2 (proper restoration)

6. **API version detection without fallbacks**
   - Problem: Code only checks one API location
   - Consequence: Works on some Nova versions but not others
   - Prevention: Try multiple class paths (already implemented)
   - Phase impact: Already handled, document in compatibility matrix

7. **Synchronous operations on main thread**
   - Problem: Large island scans cause server freezes
   - Consequence: TPS drops, player lag, timeouts
   - Prevention: Batch operations, progress reporting
   - Phase impact: Optimization phase (Phase 4)

8. **Serialization format not future-proof**
   - Problem: No version field in NovaBlockData
   - Consequence: Can't migrate after Nova updates
   - Prevention: Add schema versioning
   - Phase impact: Phase 5 (schema versioning)

**Minor Pitfalls (Annoyance, Fixable):**

9. **No verification of restored blocks**
   - Problem: Counts success without checking actual placement
   - Prevention: Verify block state after placement
   - Phase impact: Quality improvement (Phase 6)

10. **Reflection method caching not implemented**
    - Problem: Redundant class lookups on every operation
    - Prevention: Cache methods at initialization
    - Phase impact: Optimization (Phase 4)

11. **No rate limiting for block placement**
    - Problem: Hundreds of blocks at once causes TPS spikes
    - Prevention: Batch placements (50 blocks per tick)
    - Phase impact: Optimization (Phase 4)

12. **Silent failures in reflection**
    - Problem: Generic error messages, hard to diagnose
    - Prevention: Detailed catch blocks with actionable messages
    - Phase impact: Quality improvement (Phase 6)

**Phase-Specific Warnings:**

| Phase | Critical Pitfall | Mitigation |
|-------|-----------------|------------|
| Phase 1: Basic Integration | #1 FAWE incompatibility | Separate Nova capture (already done) |
| Phase 2: TileEntity State | #2 Missing inventory, #5 Context | Use getDrops(), proper Context |
| Phase 3: Cleanup | #3 BlockManager removal | Call removeBlock before FAWE |
| Phase 4: Optimization | #7 Sync operations, #10 Caching | Batch ops, cache reflection |
| Phase 5: Data Schema | #8 Serialization | Add versioning to NovaBlockData |
| Phase 6: Quality | #9 Verification, #12 Error handling | Verify placements, detailed logging |

---

## Recommended Stack

**No changes required.** Current stack is correctly configured:

- **Nova API:** 0.21.0 (optional update to 0.22.1 for currency features)
- **Dependency scope:** `provided` (correct - runtime-only, not bundled)
- **Integration pattern:** Reflection-based optional integration (correct pattern)
- **Java version:** 17 (compatible with Nova and Paper 1.20.4)
- **Minecraft target:** 1.20.4 (supported by Nova 0.22.1)

**If updating to Nova 0.22.1:**
```xml
<dependency>
    <groupId>xyz.xenondevs.nova</groupId>
    <artifactId>nova-api</artifactId>
    <version>0.22.1</version>
    <scope>provided</scope>
</dependency>
```

**Documentation note:** README.md should mention that servers running Nova 0.22.x require the Java agent flag: `-javaagent:plugins/Nova-0.22.1.jar`

---

## Expected Features

### Table Stakes (Must Have)

| Feature | Status | Phase |
|---------|--------|-------|
| Block type preservation | ✓ Implemented | - |
| Block position accuracy | ✓ Implemented | - |
| Graceful degradation | ✓ Implemented | - |
| Version resilience | ✓ Implemented | - |
| Multi-dimension support | ⚠ Needs verification | Phase 1 |

### Differentiators (Should Have)

| Feature | Priority | Complexity | Phase |
|---------|----------|------------|-------|
| Inventory preservation | CRITICAL | HIGH | Phase 2-3 |
| Energy/progress preservation | HIGH | HIGH | Phase 3-4 (if APIs exist) |
| Machine upgrade preservation | HIGH | HIGH | Phase 4 (if APIs exist) |
| Owner preservation | MEDIUM | MEDIUM | Phase 3 |
| Visual feedback | MEDIUM | LOW | Phase 1 |
| Error reporting | MEDIUM | LOW | Phase 1 |

### Deferred to Future Versions

- Energy/progress preservation (if API not available)
- Upgrade preservation (requires Simple Upgrades addon research)
- Network preservation (too complex - document manual re-wiring)

### Anti-Features (Explicitly Don't Build)

- Perfect state replication (impossible without Nova cooperation)
- Schematic data embedding (WorldEdit format incompatibility)
- Manual block-by-block copy (performance disaster)
- Network auto-reconnection (too complex and error-prone)
- GUI-based restoration (adds unnecessary friction)

---

## Architecture Approach

### Component Build Order

**Phase 1: Foundation & Quick Wins (1-2 weeks)**

Goal: Update to Nova 0.17+ API, basic capture/restore working, quick wins

Tasks:
1. Update detection logic for Nova 0.17+ class paths
2. Implement `captureNovaBlocks()` using WorldDataManager.getTileEntity()
3. Implement `restoreNovaBlocks()` using BlockUtils.placeBlock()
4. Add `removeNovaBlocks()` method (currently missing)
5. Expand NovaBlockData class to include TileEntity state fields
6. Add file I/O methods (saveToFile/loadFromFile)
7. Add in-memory methods (for relocation)
8. Add visual feedback ("✓ Preserved 23 Nova machines")
9. Add error reporting ("⚠ 3 machines couldn't be restored")
10. Verify multi-dimension support

Testing: Standalone unit tests with mock island regions

Deliverable: NovaIntegration.isAvailable() returns true, capture/restore methods work

**Phase 2: Backup Integration (2-3 weeks)**

Goal: Nova blocks saved/restored in backups, TileEntity state preservation

Tasks:
1. Add NovaIntegration.saveNovaBlocks() call after SchematicUtils.copyAndSave()
2. Add NovaIntegration.loadNovaBlocks() call after SchematicUtils.loadAndPaste()
3. Handle multi-dimension file paths (.nova.dat per dimension)
4. Update backup cleanup to include .nova.dat files
5. Research TileEntity inventory access API
6. Implement inventory capture using TileEntity.getDrops() or equivalent
7. Implement owner preservation if Context supports it
8. Add proper Context parameter handling

Testing: Create backup → verify .nova.dat exists → restore → verify blocks + inventories present

Deliverable: Backups include Nova block data with inventory state

**Phase 3: Production Operations (2 weeks)**

Goal: Nova blocks preserved during slot switching and relocation

Tasks:
1. Add Nova capture in saveIslandToSchematic() (after entity save)
2. Add Nova removal in clearIslandBlocksAndEntities() (after entity removal)
3. Add Nova restore in loadSchematicToWorld() (after entity restore)
4. Handle multi-dimension operations in saveAllDimensionIslands/loadAllDimensionIslands
5. Add Nova in-memory capture in captureRegion() (RelocationManager)
6. Add Nova removal in removeBlocks()
7. Add Nova restore in pasteAtNewLocation()
8. Implement proper cleanup via BlockManager.removeBlock()
9. Add error handling and progress messages

Testing: Build islands with Nova blocks → switch slots → verify blocks + inventories move

Deliverable: Slot switching and relocation preserve Nova blocks with inventories

**Phase 4: Performance & Polish (1-2 weeks)**

Goal: Optimization and advanced features

Tasks:
1. Implement reflection method caching
2. Add batch block placement (50 blocks per tick)
3. Performance profiling (capture/restore timing)
4. Stress testing (100+ Nova blocks)
5. Async operation optimization
6. Research Simple Upgrades addon API
7. Implement energy/progress preservation (if API discovered)
8. Implement upgrade preservation (if Simple Upgrades researched)

Testing: Stress tests, edge cases, performance benchmarks

Deliverable: Robust, performant integration

**Phase 5: Robustness (1 week)**

Goal: Production-ready quality

Tasks:
1. Add schema versioning to NovaBlockData
2. Implement verification of restored blocks
3. Add detailed error messages with actionable information
4. Edge case handling
5. Migration logic for future API changes
6. Documentation updates

Testing: Edge cases, migration scenarios

Deliverable: Production-ready Nova integration

### Total Estimated Timeline: 7-10 weeks

### Data Flow Pattern

**Follows EntityStorage Pattern:**

```
SLOT SWITCH:
Save Phase:
  FAWE schematic → slot-1.schem
  EntityStorage → slot-1.entities
  NovaIntegration → slot-1.nova.dat ← ADD THIS

Clear Phase:
  Remove entities
  FAWE clear
  NovaIntegration remove ← ADD THIS

Load Phase:
  FAWE paste ← slot-2.schem
  EntityStorage ← slot-2.entities
  NovaIntegration ← slot-2.nova.dat ← ADD THIS
```

**Key Insight:** Nova integration hooks into existing workflows at the same points as EntityStorage, using the same capture → clear → restore pattern.

### Threading Strategy

- Capture: Async (WorldDataManager reads are thread-safe)
- Save to file: Async (I/O heavy)
- Remove: Main thread (world modification)
- Restore: Main thread (world modification)
- Load from file: Async I/O → Main thread restore

### Error Handling Philosophy

"Best effort" - Nova integration failures should log warnings but not break primary operations (slot switch, relocation, backup). Users should get clear error messages when something fails, not silent data loss.

---

## Critical Pitfalls to Watch

**Top 5 Pitfalls Ranked by Impact:**

### 1. Missing TileEntity Inventory State (CRITICAL - Pitfall #2)

**Impact:** Users lose valuable items stored in Nova machines

**When it bites:**
- Phase 2 (TileEntity state preservation)
- When inventory capture not implemented

**Prevention:**
```java
// WRONG - Only captures block type
String blockId = blockState.getId();

// RIGHT - Capture full TileEntity state
Object tileEntity = tileEntityManager.getTileEntityAt(location);
List<ItemStack> drops = tileEntity.getDrops(true); // Includes inventory
```

**Why it's critical:** Losing inventories feels like data loss, not state reset. Users will perceive the feature as broken.

### 2. Not Using BlockManager for Removal (CRITICAL - Pitfall #3)

**Impact:** Memory leaks, ghost blocks, chunk corruption

**When it bites:**
- Phase 3 (cleanup implementation)
- Every island operation that clears Nova blocks

**Prevention:**
Use Nova's `BlockManager.removeBlock()` instead of `Block.setType(AIR)` to properly clean up TileEntity data.

**Why it's critical:** Silent data corruption that compounds over time. Server performance degrades, debugging is difficult.

### 3. WorldEdit/FAWE Incompatibility (CRITICAL - Pitfall #1)

**Impact:** All Nova blocks silently lost

**When it bites:**
- Any phase if not maintained properly
- If someone tries to "simplify" by using FAWE only

**Prevention:**
Maintain separate Nova capture system. Never rely on FAWE to handle Nova blocks.

**Why it's critical:** Already implemented, but must be maintained. Regression would cause catastrophic data loss.

### 4. Missing Context Parameter (MODERATE - Pitfall #5)

**Impact:** Blocks place but don't function correctly

**When it bites:**
- Phase 2 (restoration implementation)
- When BlockUtils.placeBlock() called without Context

**Prevention:**
Always provide proper Context to block placement operations:
```java
Context context = Context.empty();
BlockUtils.placeBlock(context, location, novaBlock, false);
```

**Why it's important:** Blocks appear correct but lack functionality. Confusing user experience.

### 5. Chunk Unload During Async Operations (MODERATE - Pitfall #4)

**Impact:** Intermittent data loss, hard-to-reproduce bugs

**When it bites:**
- Any async operation phase
- More common on busy servers with aggressive chunk unloading

**Prevention:**
Pre-load all chunks, keep loaded during operation, add timeouts (already implemented - verify in testing).

**Why it's important:** Already handled, but must verify through testing. Intermittent bugs are the hardest to diagnose.

---

## Implications for Roadmap

### Suggested Phase Structure

**Phase 1: Foundation & Quick Wins (1-2 weeks)**
- Rationale: API updates are blocking, quick wins build confidence
- Delivers: Nova 0.17+ API, basic capture/restore, visual feedback, error reporting
- Features: Visual feedback, error reporting, multi-dimension verification
- Pitfalls: #1 (maintain FAWE separation), #6 (API version fallbacks)
- Research needed: None - patterns established

**Phase 2: Core State Preservation (2-3 weeks)**
- Rationale: Inventory preservation is most impactful feature
- Delivers: TileEntity inventory + owner preservation, backup integration
- Features: Inventory preservation (CRITICAL), owner preservation
- Pitfalls: #2 (inventory state), #5 (Context parameter)
- Research needed: TileEntity API deep-dive, Context parameters, serialization

**Phase 3: Production Operations (2 weeks)**
- Rationale: Slot switching most common, proper cleanup prevents leaks
- Delivers: Slot switch + relocation integration, proper cleanup
- Features: All table stakes complete
- Pitfalls: #3 (BlockManager removal), #4 (chunk unload verification)
- Research needed: None - architecture clear

**Phase 4: Performance & Polish (1-2 weeks)**
- Rationale: Operations work but may be slow, polish for production
- Delivers: Optimization, advanced features (energy/upgrades if APIs exist)
- Features: Energy/progress preservation, upgrade preservation
- Pitfalls: #7 (sync ops), #10 (caching), #11 (rate limiting)
- Research needed: Simple Upgrades addon API (if pursuing upgrades)

**Phase 5: Robustness (1 week)**
- Rationale: Must be production-ready before release
- Delivers: Schema versioning, verification, detailed errors
- Features: All must-haves complete
- Pitfalls: #8 (serialization), #9 (verification), #12 (error handling)
- Research needed: None - implementation patterns clear

### Research Flags

**Phases needing `/gsd:research-phase`:**

- **Phase 2:** API Deep-Dive (TileEntity inventory read/write, Context parameters, serialization format)
- **Phase 4:** Simple Upgrades Research (optional - addon API)

**Phases with well-documented patterns (skip research):**

- Phase 1, Phase 3, Phase 5

---

## Confidence Assessment

| Area | Confidence | Source Quality | Notes |
|------|------------|---------------|-------|
| Stack | HIGH | Official Nova docs, existing pom.xml | Nova API confirmed, no changes needed |
| Features | MEDIUM | Nova docs, user scenarios, existing code | User expectations clear, API details uncertain |
| Architecture | MEDIUM | Codebase analysis, Nova migration guides | Patterns established, TileEntity serialization uncertain |
| Pitfalls | MEDIUM | Nova GitHub issues, changelog, migration docs | Critical issues documented, edge cases inferred |

### Overall Confidence: MEDIUM

**Strengths:**
- Official Nova documentation for API basics
- Existing NovaIntegration.java provides working foundation
- Clear architectural patterns (EntityStorage parallel)
- Well-documented pitfalls from Nova GitHub

**Gaps to Address:**

1. **TileEntity inventory access methods (Phase 2 blocker)** - Deep API research needed
2. **Energy/progress data exposure (Phase 4 uncertainty)** - Treat as "best effort"
3. **Simple Upgrades addon integration (Phase 4 uncertainty)** - Research at Phase 4, defer if no API
4. **Multi-dimension + Nova interaction (Phase 1 verification)** - Test early

**Risk Assessment:**
- LOW risk: Stack, foundation work, backup integration
- MEDIUM risk: TileEntity state preservation (API uncertainty)
- HIGH risk: Energy/progress preservation (may not be feasible)

**Recommended approach:** Build incrementally with validation gates, focus on inventory preservation (highest impact), defer nice-to-haves if APIs don't exist.

---

## Sources

### HIGH Confidence

**Stack Research:**
- [Nova API Getting Started](https://docs.xenondevs.xyz/nova/api/)
- [Nova Releases](https://github.com/xenondevs/Nova/releases)
- [Nova on Hangar](https://hangar.papermc.io/xenondevs/Nova)
- [xenondevs Maven Repository](https://repo.xenondevs.xyz/releases)
- IslandSelector pom.xml

**Architecture Research:**
- [Nova 0.16 to 0.17 Migration Guide](https://docs.xenondevs.xyz/nova/addon/migration-guide/0.16-0.17/)
- [Nova 0.17 to 0.18 Migration Guide](https://docs.xenondevs.xyz/nova/addon/migration-guide/0.17-0.18/)
- IslandSelector codebase (NovaIntegration.java, EntityStorage.java, managers)

**Pitfalls Research:**
- [Nova GitHub Issue #252](https://github.com/xenondevs/Nova/issues/252)
- [Nova TileEntity Documentation](https://docs.xenondevs.xyz/nova/api/tileentity/tileentity/)
- [Nova TileEntityManager Documentation](https://docs.xenondevs.xyz/nova/api/tileentity/tileentitymanager/)
- [Nova 0.18 Changelog](https://hangar.papermc.io/xenondevs/Nova/versions/0.18)

### MEDIUM Confidence

**Features Research:**
- [Nova Machines Addon](https://www.spigotmc.org/resources/nova-addon-machines.102712/)
- [Nova GitHub Repository](https://github.com/xenondevs/Nova)
- [Nova Changelog](https://modrinth.com/plugin/nova-framework/changelog)
- [FAWE Tile Entity Issues](https://github.com/IntellectualSites/FastAsyncWorldEdit/issues/517)
- IslandSelector README.md

### LOW Confidence (Verification Needed)

- Simple Upgrades addon API (mentioned but not researched)
- Nova Logistics addon cable networks (mentioned but not researched)
- Energy/progress data API exposure (unknown if public API exists)
- Nova TileEntity non-destructive inventory access (getDrops() is destructive)
- Multi-dimension behavior with current NovaIntegration (untested)
- Migration guide 0.21→0.22 (mentioned but content not accessible)

---

## Ready for Roadmap Creation

This summary provides:
- Clear technology stack (no changes needed)
- Prioritized feature list (critical vs nice-to-have)
- Phased architecture approach (5 phases, 7-10 weeks)
- Critical pitfalls with prevention strategies
- Research flags for Phase 2 and Phase 4
- Honest confidence assessment with identified gaps

The roadmapper should:
1. Use suggested phase structure as foundation
2. Schedule API research at Phase 2 start
3. Treat energy/progress preservation as "if feasible"
4. Flag inventory preservation as critical path
5. Plan for testing and verification throughout
6. Build incrementally with validation gates

**Next step:** Create detailed roadmap with tasks, acceptance criteria, and milestones based on this research.

---
*Research completed: 2026-01-27*
*Ready for roadmap: yes*
