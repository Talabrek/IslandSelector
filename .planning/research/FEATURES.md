# Feature Landscape: Nova Integration for Island Manipulation

**Domain:** Custom block preservation during island operations (relocation & slot switching)
**Researched:** 2026-01-26
**Confidence:** MEDIUM (based on official Nova docs, existing implementation, and ecosystem research)

## Executive Summary

Nova is a server-side modding framework that adds custom blocks (machines, storage, etc.) without client modifications using resource pack tricks. The critical problem: **Nova stores block data outside Minecraft's NBT system**, so WorldEdit/FAWE copy operations lose this data. When islands are relocated or slots are switched, Nova machines appear as vanilla backing blocks (note blocks, mushroom stems) without their custom functionality.

The project already implements a **capture-and-restore pattern** via `NovaIntegration.java` that:
1. Captures Nova block IDs and positions before WorldEdit operations
2. Restores Nova blocks at new positions after WorldEdit paste

This research identifies what features users expect from such integration, categorized by priority for the upcoming milestone.

---

## Table Stakes

Features users expect when Nova is installed. Missing these = Nova integration feels broken.

| Feature | Why Expected | Complexity | Current Status |
|---------|--------------|------------|----------------|
| **Block type preservation** | Nova blocks must remain Nova blocks after move | LOW | ✅ IMPLEMENTED via `captureNovaBlocks()` + `restoreNovaBlocks()` |
| **Block position accuracy** | Nova blocks must appear in correct relative positions | LOW | ✅ IMPLEMENTED via relative coordinates (relX, relY, relZ) |
| **Graceful degradation** | Operations work even when Nova not installed | LOW | ✅ IMPLEMENTED via `isAvailable()` check and reflection |
| **Version resilience** | Handle Nova API changes across versions | MEDIUM | ✅ PARTIALLY IMPLEMENTED (tries multiple API paths) |
| **Multi-dimension support** | Nova blocks preserved in nether/end during operations | MEDIUM | ⚠️ NEEDS VERIFICATION - integration exists but untested |

### Why These Are Table Stakes

**Block type preservation** - Without this, Nova machines disappear entirely or become vanilla blocks. This is catastrophic data loss.

**Position accuracy** - If a Pulverizer ends up 3 blocks away from where it was, users will assume the integration is buggy.

**Graceful degradation** - Servers without Nova should not experience errors or broken functionality. Integration must be optional.

**Version resilience** - Nova is actively developed. API changes between versions must not break the integration.

**Multi-dimension support** - IslandSelector supports multi-dimension islands. If Nova works in overworld but breaks in nether, users lose expensive machines.

---

## Differentiators

Features that make the integration excellent, not just functional. Not expected, but highly valued when present.

| Feature | Value Proposition | Complexity | Priority |
|---------|-------------------|------------|----------|
| **Inventory preservation** | Machines keep their items after move | HIGH | 🔴 CRITICAL |
| **Energy/progress preservation** | Furnaces keep smelting progress, machines keep energy | HIGH | 🟡 HIGH |
| **Machine upgrade preservation** | Speed/efficiency upgrades persist after move | HIGH | 🟡 HIGH |
| **Owner preservation** | Machine ownership data maintained | MEDIUM | 🟢 MEDIUM |
| **Visual feedback** | Show count of Nova blocks captured/restored | LOW | 🟢 MEDIUM |
| **Error reporting** | Warn players if Nova blocks couldn't be restored | LOW | 🟢 MEDIUM |
| **Async operation** | Non-blocking capture with chunk pre-loading | LOW | ✅ IMPLEMENTED |
| **Network preservation** | Cable networks and connections maintained | VERY HIGH | ⚫ NOT FEASIBLE |

### Why These Matter

**Inventory preservation** (CRITICAL):
- Most impactful missing feature
- Users store valuable items in Nova machines (Storage Units, Quarries, etc.)
- Losing inventories feels like data loss, not state reset
- Common scenario: Player has Pulverizer with 500 diamonds queued → relocates island → diamonds vanish

**Energy/progress preservation** (HIGH):
- Machines from [Nova Machines addon](https://www.spigotmc.org/resources/nova-addon-machines.102712/) (Furnace Generators, Pulverizers, Quarries) have energy buffers and processing progress
- Less critical than inventories (users accept some state reset on major operations)
- But noticeable quality difference - professional integration preserves state

**Machine upgrade preservation** (HIGH):
- Nova machines support Simple Upgrades addon for speed/efficiency boosts
- Upgrades are expensive (crafted/purchased)
- Losing upgrades on relocation feels like a penalty for moving

**Owner preservation** (MEDIUM):
- Nova blocks track ownership via `getOwner()` ([Nova API docs](https://docs.xenondevs.xyz/nova/api/tileentity/tileentity/))
- Affects multi-player islands where ownership determines permissions
- Lower priority than inventories/energy but important for teams

**Visual feedback** (MEDIUM):
- Simple UX win: "✓ Preserved 23 Nova machines with inventories"
- Builds confidence that integration is working
- Low effort, high perceived quality

**Error reporting** (MEDIUM):
- Currently failures are silent to players (only logged to console)
- Players should know if something went wrong: "⚠ 3 Nova machines couldn't be restored"
- Prevents confusion when machines are missing

**Async operation** (IMPLEMENTED):
- Already done via `captureNovaBlocksAsync()` ([NovaIntegration.java](C:\Users\Administrator\Desktop\VSCode\IslandSelector\src\main\java\world\bentobox\islandselector\integrations\NovaIntegration.java) lines 175-220)
- Pre-loads chunks before capture to avoid missing blocks
- Critical for reliability

**Network preservation** (NOT FEASIBLE):
- Nova Logistics addon provides cable networks for item/energy transport
- Networks are complex spatial structures with connection state
- Attempting automatic reconnection would be extremely complex and error-prone
- **Recommendation:** Document that networks need manual re-wiring after moves

---

## Anti-Features

Features to explicitly NOT build. Common mistakes in this domain.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **Perfect state replication** | Impossible without deep Nova API cooperation. Tile entity internal state (tick counters, animations, internal buffers) is opaque and not exposed via API. | Focus on preserving **user-visible state**: inventories, energy levels, upgrades, ownership. Accept that animations/tick counters/internal timers will reset. Document this limitation. |
| **Schematic data embedding** | WorldEdit/FAWE schematics use NBT format. Nova uses a separate data storage system. Attempting to embed Nova data in FAWE schematics creates format incompatibility and corruption risk. | Keep Nova data separate (as currently done with `NovaBlockData` serialized list) and restore post-paste in a second pass. |
| **Manual block-by-block copy** | Replacing WorldEdit's efficient bulk copy with slow Nova block-by-block placement would destroy performance for large islands. | Use WorldEdit for vanilla blocks (fast), then overlay Nova-specific data restoration (targeted). |
| **Hard Nova dependency** | Requiring Nova to be installed breaks servers that don't use Nova and creates plugin conflicts. | Continue using reflection-based soft dependency (current approach is correct). |
| **Network auto-reconnection** | Nova Logistics cable networks are spatial graphs with connection state. Attempting to automatically reconnect cables across potentially large distances (relocated islands) is complex, error-prone, and may produce unexpected connections. | Document that networks need manual re-wiring. Possibly add helper command to clear broken connections. |
| **GUI-based restoration** | Making players manually select what to restore adds friction and is error-prone (players forget, partial restores create confusion). | Automatic restoration (current approach correct). Provide feedback about what was restored. |
| **Drop-based restoration** | Original research plan suggested using TileEntity.getDrops() to get items, then re-placing blocks with item data. This is more complex than needed and loses structure. | Use Nova API to read state, restore blocks, then populate state via API if available. |

### Why These Are Anti-Features

**Perfect state replication**: Based on [Nova TileEntity API research](https://docs.xenondevs.xyz/nova/api/tileentity/tileentity/), the API exposes `getOwner()` and `getDrops(boolean includeSelf)`, but not methods to directly read internal machine state like tick progress or animation frames. Attempting to replicate these would require reflection into internal Nova classes, which is fragile and breaks across versions.

**Schematic data embedding**: Research shows [WorldEdit/FAWE has issues with tile entities](https://github.com/IntellectualSites/FastAsyncWorldEdit/issues/517) even for vanilla blocks. Adding custom data formats would compound this. The [IA-Edit plugin](https://github.com/EpicPlayerA10/IA-Edit) for ItemsAdder shows the pattern: use `-e` flag for entities but custom blocks still require separate handling.

**Manual block-by-block copy**: Nova blocks are placed via `BlockUtils.placeBlock()` which is not batched. For a typical island with 50-200 Nova machines, this is acceptable. For 10,000+ blocks, it would be unacceptably slow.

**Hard Nova dependency**: BentoBox ecosystem pattern is soft dependencies via reflection. The [existing NovaIntegration.java](C:\Users\Administrator\Desktop\VSCode\IslandSelector\src\main\java\world\bentobox\islandselector\integrations\NovaIntegration.java) correctly implements this pattern.

---

## Feature Dependencies

Current implementation provides foundation for enhancements:

```
IMPLEMENTED (Table Stakes):
├─ Block type preservation
│   └─ captureNovaBlocks() captures block IDs
│   └─ restoreNovaBlocks() re-places via BlockUtils
├─ Position accuracy
│   └─ NovaBlockData stores relative coords
├─ Graceful degradation
│   └─ Soft dependency via reflection
│   └─ isAvailable() checks
├─ Async chunk loading
│   └─ captureNovaBlocksAsync() with CompletableFuture
└─ Version resilience (partial)
    └─ Tries xyz.xenondevs.nova.world.* and .data.world.* paths

MISSING (Differentiators):
├─ Inventory preservation
│   └─ BLOCKED: Need Nova API method to read/write TileEntity inventory
│   └─ getDrops() exists but is destructive
│   └─ Need non-destructive inventory access
├─ Energy/progress preservation
│   └─ BLOCKED: Need Nova's custom NBT-equivalent storage format
│   └─ Not exposed in current API surface
├─ Upgrade preservation
│   └─ BLOCKED: Need Simple Upgrades addon API research
│   └─ Unknown how upgrades are stored
├─ Owner preservation
│   └─ BLOCKED: Have getOwner() but need setOwner() or equivalent
│   └─ May require placing block with specific Context
└─ Visual feedback & error reporting
    └─ NOT BLOCKED: Just need to add messages to player
```

---

## User Expectations by Scenario

### Scenario 1: Slot Switching (Most Common)

**User mental model**: "Switching slots is like switching save files in a video game - everything should be exactly as I left it"

**Current reality**:
- ✅ Nova blocks appear correctly
- ❌ Inventories are empty
- ❌ Energy/progress is reset
- ❌ Upgrades are lost

**Acceptable compromise**: Energy/progress reset is tolerable (users understand it's a major operation), but **inventories must persist** or it feels like data loss, not a save/load.

**Priority**: Inventory preservation is CRITICAL for this scenario.

### Scenario 2: Island Relocation

**User mental model**: "Moving my island should feel like teleportation, not rebuilding"

**Current reality**: Same as slot switching

**Acceptable compromise**: Same as slot switching. Users tolerate MORE state loss for relocation (it's rarer and more dramatic), but inventory loss is still unacceptable.

**Priority**: Inventory preservation is CRITICAL. Energy/upgrades are HIGH but tolerable if documented.

### Scenario 3: Multi-Dimension Operations

**User mental model**: "If it works in overworld, it should work in nether/end"

**Current reality**:
- NovaIntegration exists
- RelocationManager and SlotSwitchManager handle multi-dimension
- But no explicit testing for Nova + multi-dimension

**Risk**: Users may lose expensive machines in nether/end if integration fails there

**Priority**: Verification is MEDIUM (likely works, but must be tested)

---

## Feature Complexity Assessment

| Feature | Dev Effort | Testing Effort | Risk | API Research Needed |
|---------|-----------|----------------|------|---------------------|
| Visual feedback | 1 hour | 1 hour | None | No |
| Error reporting | 2 hours | 2 hours | None | No |
| Multi-dimension verification | 2 hours | 4 hours | Low | No |
| Owner preservation | 4 hours | 2 hours | Medium | Yes - need setOwner equivalent |
| Inventory preservation | 16 hours | 8 hours | High | Yes - need inventory read/write API |
| Energy preservation | 16 hours | 8 hours | High | Yes - need custom storage format |
| Upgrade preservation | 8 hours | 4 hours | Medium | Yes - need Simple Upgrades API |
| Network preservation | 40+ hours | 20+ hours | VERY HIGH | Yes - complex spatial graph |

**Effort notes**:
- **Visual feedback**: Just add player messages with counts
- **Error reporting**: Catch failures, show warning to player
- **Multi-dimension verification**: Test existing code with Nova blocks in nether/end
- **Owner preservation**: Research if BlockUtils.placeBlock() accepts owner Context parameter
- **Inventory preservation**: Requires API method discovery, serialization format, test with various machine types
- **Energy preservation**: Same as inventory but may not be exposed via API at all
- **Network preservation**: Spatial graph analysis, connection validation, cross-dimension handling - essentially building a Nova Logistics mini-plugin

---

## MVP Recommendation

**Current implementation (v1.1.1) IS the MVP** - it solves the critical problem (blocks disappearing completely).

**For next milestone (v1.2), prioritize**:

### Phase 1: Quick Wins (Low-hanging fruit)
1. **Visual feedback** - "✓ Preserved 23 Nova machines"
2. **Error reporting** - "⚠ 3 machines couldn't be restored"
3. **Multi-dimension verification** - Test + document current behavior

**Rationale**: Low effort, immediate UX improvement, builds user confidence

### Phase 2: API Research (Exploratory)
4. **Nova API deep-dive** - Find inventory/energy/owner access methods
5. **Simple Upgrades research** - Understand upgrade storage format

**Rationale**: Must be done before implementation, may discover blockers

### Phase 3: Critical Features (If APIs exist)
6. **Inventory preservation** - Most user-visible impact
7. **Owner preservation** - If simple (Context-based)

**Rationale**: Transforms integration from "basic" to "production-ready"

### Deferred to v1.3+:
- **Energy/progress preservation** - Nice to have, but users accept reset
- **Upgrade preservation** - Affects fewer players (requires addon)
- **Network preservation** - Too complex, document manual re-wiring instead

---

## Implementation Gaps Analysis

Based on existing `NovaIntegration.java`:

### Currently Missing:

1. **Inventory capture** ([line 102-170](C:\Users\Administrator\Desktop\VSCode\IslandSelector\src\main\java\world\bentobox\islandselector\integrations\NovaIntegration.java#L102-L170))
   - `NovaBlockData` only stores blockId
   - No inventory ItemStack list
   - No serialization of inventory contents

2. **Inventory restore** ([line 228-330](C:\Users\Administrator\Desktop\VSCode\IslandSelector\src\main\java\world\bentobox\islandselector\integrations\NovaIntegration.java#L228-L330))
   - `restoreNovaBlocks()` places blocks
   - No inventory population after placement
   - No method to access placed block's TileEntity

3. **Player feedback** (entire file)
   - Operations are silent to player
   - Success/failure only logged to console
   - No "X Nova blocks preserved" message

4. **Error visibility** ([line 317-319](C:\Users\Administrator\Desktop\VSCode\IslandSelector\src\main\java\world\bentobox\islandselector\integrations\NovaIntegration.java#L317-L319))
   - Failures in restore loop caught but silently skipped
   - Player never knows if restoration was partial

5. **Owner restoration** ([line 289-320](C:\Users\Administrator\Desktop\VSCode\IslandSelector\src\main\java\world\bentobox\islandselector\integrations\NovaIntegration.java#L289-L320))
   - `getOwner()` method exists in TileEntity API
   - Not captured during captureNovaBlocks()
   - Not restored during restoreNovaBlocks()

### Research Needed:

**Priority 1: How to access Nova TileEntity inventory (CRITICAL)**

Questions:
- Is there a non-destructive inventory read method?
- Nova docs show `getDrops(boolean includeSelf)` returns ItemStacks ([TileEntity API](https://docs.xenondevs.xyz/nova/api/tileentity/tileentity/))
- Can inventory be set on a placed block?
- Do we get TileEntity reference after `BlockUtils.placeBlock()`?

Research approach:
- Check Nova GitHub for inventory access examples
- Look for NovaBlock or NovaBlockState inventory methods
- Check if WorldDataManager exposes inventory access
- Contact Nova developers if API is not public

**Priority 2: How to preserve ownership (HIGH)**

Questions:
- How to capture owner UUID during captureNovaBlocks()?
- How to set owner during restoreNovaBlocks()?
- Does `BlockUtils.placeBlock(Context, Location, NovaBlock, boolean)` Context parameter include owner?
- Is there a separate setOwner method?

Research approach:
- Review Context class for owner field
- Check BlockUtils for owner-setting methods
- Look for player-placement simulation in Nova

**Priority 3: How to access energy/progress data (MEDIUM)**

Questions:
- Where does Nova store custom block state (energy levels, progress)?
- Is it in WorldDataManager?
- Is it exposed via TileEntity or NovaBlockState?
- Can it be serialized separately?

Research approach:
- Inspect NovaBlockState class for state getters
- Check if energy is stored in a standard format
- May not be feasible if internal-only

**Priority 4: Simple Upgrades addon API (MEDIUM)**

Questions:
- How are upgrades stored? (NBT-like? Separate database?)
- Is there an API to read/write upgrades?
- Are upgrades part of the block or stored globally?

Research approach:
- Check Simple Upgrades addon source if available
- Contact addon developer
- May need to treat as separate integration

---

## Sources

### HIGH Confidence:
- [Nova TileEntity Documentation](https://docs.xenondevs.xyz/nova/api/tileentity/tileentity/) - Official API reference showing TileEntity.getDrops(), getOwner(), and limitations
- [Nova TileEntityManager Documentation](https://docs.xenondevs.xyz/nova/api/tileentity/tileentitymanager/) - Official API reference for getTileEntityAt()
- Existing implementation: [NovaIntegration.java](C:\Users\Administrator\Desktop\VSCode\IslandSelector\src\main\java\world\bentobox\islandselector\integrations\NovaIntegration.java) - Current capture/restore pattern, lines 1-365
- [IslandSelector README.md](C:\Users\Administrator\Desktop\VSCode\IslandSelector\README.md) - Documents multi-dimension support, slot switching, relocation features

### MEDIUM Confidence:
- [Nova Machines Addon](https://www.spigotmc.org/resources/nova-addon-machines.102712/) - Confirms machines exist (Pulverizers, Generators, Quarries) requiring inventory/energy preservation
- [Nova GitHub Repository](https://github.com/xenondevs/Nova) - Active development, server-side framework
- [Nova Changelog](https://modrinth.com/plugin/nova-framework/changelog) - Recent fixes show BlockUtils#placeBlock behavior and getDrops() context parameters (BLOCK_DROPS, BLOCK_STORAGE_DROPS, BLOCK_EXP_DROPS)
- [FAWE Tile Entity Issues](https://github.com/IntellectualSites/FastAsyncWorldEdit/issues/517) - Documents that WorldEdit/FAWE cannot handle custom tile entities, validating need for separate handling

### LOW Confidence (WebSearch only, needs verification):
- Nova blocks support Simple Upgrades addon (mentioned in SpigotMC addon description)
- Nova Logistics addon provides cable networks (mentioned in [SpigotMC](https://www.spigotmc.org/resources/nova-addon-logistics.102713/))
- BlockBehavior#getDrops context parameters (from Nova changelog)

### Context from Related Integrations:
- [IA-Edit for ItemsAdder](https://github.com/EpicPlayerA10/IA-Edit) - Similar problem space (ItemsAdder custom blocks + WorldEdit), uses `-e` flag and separate handling pattern
- [Minecraft TileEntity NBT patterns](https://minecraft.fandom.com/wiki/Tutorials/Command_NBT_tags) - Shows what data is typically preserved (inventory, energy, progress)

### Verification Needed:
- Nova TileEntity inventory access methods (non-destructive read/write)
- Nova energy/progress data storage format and API exposure
- Simple Upgrades addon API for reading/writing upgrade data
- Owner-setting method in BlockUtils or Context system
- Multi-dimension behavior of current NovaIntegration (untested)
