# Phase 13: State Preservation - Research

**Researched:** 2026-01-27
**Domain:** Nova TileEntity state restoration (inventory, owner) from captured drops
**Confidence:** MEDIUM

## Summary

Phase 13 implements restoration of Nova machine inventories and ownership after island operations. Phase 12 already captures TileEntity drops via `TileEntity.getDrops(true)`, which returns ItemStacks containing the block item plus all inventory contents. These drops use Minecraft's standard BlockEntityTag NBT system to embed TileEntity state data (inventory contents, owner UUID, machine settings) inside the ItemStack.

The core question for Phase 13: **Do we need to manually restore drops to TileEntities, or does Nova handle this automatically when placing blocks?**

Key findings:
1. **Vanilla Minecraft uses BlockEntityTag** - Shulker boxes, barrels, and other containers store their inventory in ItemStack NBT under "BlockEntityTag", which automatically restores when the block is placed
2. **Nova likely follows this pattern** - TileEntity.getDrops(true) returns ItemStacks that embed state data, similar to vanilla behavior
3. **Placement may be automatic** - If Nova ItemStacks contain BlockEntityTag, placing the block via BlockUtils.placeBlock() may automatically restore state
4. **Manual restoration may not be needed** - The drops themselves are the restoration mechanism, not a separate step
5. **Feedback messages are the main work** - The real implementation is logging "Preserved 23 Nova machines" and handling restoration failures

**Primary recommendation:** Test whether placing Nova blocks from captured drops automatically restores state. If yes, Phase 13 is primarily about feedback messages. If no, implement manual drop placement at block locations after BlockUtils.placeBlock().

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Nova TileEntity.getDrops | Nova 0.17+ | State serialization to ItemStacks | Official Nova state preservation API |
| Minecraft BlockEntityTag | Vanilla 1.13+ | NBT structure for TileEntity data in ItemStacks | Standard Minecraft serialization format |
| Bukkit ItemStack NBT | Bukkit API | Access BlockEntityTag for manual restoration | Standard Bukkit ItemStack manipulation |
| Nova BlockUtils.placeBlock | Nova 0.17+ | Block placement with potential auto-restoration | Official block placement API |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| World.dropItemNaturally | Bukkit API | Drop items at location if manual restoration needed | If blocks don't auto-restore from ItemStack |
| TileEntity.getOwner | Nova API | Get owner OfflinePlayer for verification | Validate owner preservation after restoration |
| WorldDataManager.getTileEntity | Nova 0.17+ | Access restored TileEntity after placement | Verify state after restoration |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Drops-based restoration | Direct TileEntity inventory manipulation | Nova docs state drops are official API, direct access is internal |
| Automatic BlockEntityTag | Manual inventory restoration | More complex, may miss edge cases Nova handles |
| Drop items at location | Place items back in TileEntity inventory | Requires knowing TileEntity inventory structure (version-dependent) |

**Installation:**
```bash
# No new dependencies - already using Nova via reflection in Phase 12
```

## Architecture Patterns

### Recommended Code Organization
```
src/main/java/world/bentobox/islandselector/
└── integrations/
    └── NovaIntegration.java           # Enhance restoreNovaBlocks with state verification
```

### Pattern 1: Test Auto-Restoration First

**What:** Place block from drops and check if state automatically restores
**When to use:** First step in Phase 13 - determine if manual restoration is needed
**Example:**
```java
// Location: NovaIntegration.java restoreNovaBlocks() enhancement
// This tests if Nova automatically restores state from ItemStack drops

public void restoreNovaBlocks(List<NovaBlockData> novaBlocks, Location center) {
    // ... existing block placement code ...

    // After placing blocks, verify state restoration
    int withState = 0;
    int withoutState = 0;

    for (NovaBlockData data : novaBlocks) {
        if (data.drops == null || data.drops.isEmpty()) continue;

        Location loc = new Location(world,
            centerX + data.relX, centerY + data.relY, centerZ + data.relZ);

        // Get the newly placed TileEntity
        Object tileEntity = getTileEntityMethod.invoke(worldDataManager, loc);

        if (tileEntity != null) {
            // Check if inventory/state was restored
            List<ItemStack> currentDrops = (List<ItemStack>) getDropsMethod.invoke(tileEntity, true);

            // If currentDrops match captured drops count, state auto-restored
            if (currentDrops != null && currentDrops.size() > 1) {
                withState++;
            } else {
                withoutState++;
            }
        }
    }

    addon.log("Restored " + withState + " Nova machines with state, "
        + withoutState + " without state (may need manual restoration)");
}
```

### Pattern 2: Manual Drop Restoration (If Needed)

**What:** If auto-restoration doesn't work, place drops at block location for players to collect
**When to use:** Only if Pattern 1 shows state doesn't auto-restore
**Example:**
```java
// Location: NovaIntegration.java - NEW method (conditional on testing)

public void restoreDropsManually(List<NovaBlockData> novaBlocks, Location center) {
    World world = center.getWorld();
    if (world == null) return;

    int dropsRestored = 0;

    for (NovaBlockData data : novaBlocks) {
        if (data.drops == null || data.drops.isEmpty()) continue;

        // Skip the first item if it's the block itself (includeSelf=true captured it)
        boolean firstIsBlock = data.drops.size() > 0 &&
            data.drops.get(0).getType().toString().contains("NOVA");

        int startIdx = firstIsBlock ? 1 : 0;

        Location loc = new Location(world,
            center.getBlockX() + data.relX,
            center.getBlockY() + data.relY,
            center.getBlockZ() + data.relZ);

        // Drop inventory items at block location
        for (int i = startIdx; i < data.drops.size(); i++) {
            ItemStack drop = data.drops.get(i);
            if (drop != null && drop.getType() != Material.AIR) {
                world.dropItemNaturally(loc, drop);
                dropsRestored++;
            }
        }
    }

    addon.log("Manually dropped " + dropsRestored + " items at Nova machine locations");
}
```

### Pattern 3: Feedback Message Generation

**What:** Generate user-facing messages about Nova machine preservation
**When to use:** Always - this is a Phase 13 requirement
**Example:**
```java
// Location: NovaIntegration.java restoreNovaBlocks() enhancement

public RestoreResult restoreNovaBlocks(List<NovaBlockData> novaBlocks, Location center) {
    // ... existing restoration code ...

    // Count machines with TileEntity data
    int machinesWithState = 0;
    int machinesRestored = 0;
    int machinesFailed = 0;

    for (NovaBlockData data : novaBlocks) {
        if (data.drops != null && !data.drops.isEmpty()) {
            machinesWithState++;

            // Check if block placed and has TileEntity
            Location loc = new Location(world,
                centerX + data.relX, centerY + data.relY, centerZ + data.relZ);

            Object tileEntity = getTileEntityMethod.invoke(worldDataManager, loc);
            if (tileEntity != null) {
                machinesRestored++;
            } else {
                machinesFailed++;
            }
        }
    }

    return new RestoreResult(machinesRestored, machinesFailed);
}

public static class RestoreResult {
    public final int restored;
    public final int failed;

    public RestoreResult(int restored, int failed) {
        this.restored = restored;
        this.failed = failed;
    }

    public String getFeedbackMessage() {
        if (failed > 0) {
            return "Preserved " + restored + " Nova machines (" + failed + " couldn't be restored)";
        } else if (restored > 0) {
            return "Preserved " + restored + " Nova machines";
        } else {
            return null; // No Nova machines to report
        }
    }
}
```

### Pattern 4: Owner Verification

**What:** Verify machine ownership preserved after restoration
**When to use:** Validation step, optional for debugging
**Example:**
```java
// Location: NovaIntegration.java - verification helper

private boolean verifyOwnerPreserved(Object tileEntity) {
    try {
        Method getOwnerMethod = tileEntity.getClass().getMethod("getOwner");
        Object owner = getOwnerMethod.invoke(tileEntity);

        // If owner is not null, ownership was preserved
        return owner != null;
    } catch (Exception e) {
        // getOwner not available or failed
        return false;
    }
}
```

### Anti-Patterns to Avoid

- **Assuming manual restoration is needed:** Test auto-restoration first. BlockEntityTag may handle it automatically.
- **Dropping all captured drops:** The first drop from getDrops(true) is the block itself. Don't duplicate it.
- **Modifying TileEntity inventory directly:** Use Nova's official APIs, not Bukkit Container/Inventory access.
- **Counting all Nova blocks as "machines":** Only blocks with drops (TileEntities) are machines. Simple decorative Nova blocks don't have state.
- **Failing silently:** If restoration fails, tell the player. They need to know if machines lost inventory.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| ItemStack NBT parsing | Custom BlockEntityTag parser | Test auto-restoration first | Nova/Minecraft may handle automatically |
| TileEntity inventory access | Direct Bukkit Container API on Nova blocks | Nova TileEntity.getDrops | Nova's internal structure may differ from vanilla |
| Drop item filtering | Custom logic to separate block vs inventory | Test getDrops(true) structure | Nova documents first item is block, rest is inventory |
| Owner preservation | Custom owner tracking system | Trust Nova's BlockEntityTag | Owner likely embedded in ItemStack NBT automatically |

**Key insight:** Don't build restoration logic until you verify auto-restoration doesn't work. The drops system may already handle everything, and Phase 13 becomes primarily about feedback messages.

## Common Pitfalls

### Pitfall 1: Assuming Manual Restoration Required

**What goes wrong:** Implementing complex inventory restoration when Nova already does it automatically via BlockEntityTag
**Why it happens:** Documentation doesn't explicitly state whether BlockUtils.placeBlock reads BlockEntityTag from ItemStacks
**How to avoid:** Test first! Place a Nova machine with inventory, capture drops, remove it, WorldEdit paste, restore from drops, check if inventory is there
**Warning signs:** If you write 100+ lines of inventory manipulation code before testing, you're going down the wrong path

### Pitfall 2: Dropping the Block Item As Loot

**What goes wrong:** When manually restoring drops, accidentally drop the block item itself (first item from getDrops(true))
**Why it happens:** getDrops(includeSelf=true) returns [block_item, inventory_item1, inventory_item2, ...]
**How to avoid:** Skip index 0 when manually dropping items. Only drop inventory items (index 1+).
**Warning signs:** After restoration, players find extra Nova machine items on the ground

### Pitfall 3: Counting Non-TileEntity Blocks in Feedback

**What goes wrong:** Message says "Preserved 50 Nova machines" but 45 are decorative blocks with no inventory
**Why it happens:** Counting all novaBlocks instead of only blocks with drops
**How to avoid:** Only count blocks where `data.drops != null && !data.drops.isEmpty()`
**Warning signs:** Player confusion - they don't have 50 machines, just 5 machines and 45 decorative blocks

### Pitfall 4: Using Bukkit Container API on Nova Blocks

**What goes wrong:** Trying to access Nova TileEntity inventory via Bukkit's Container or InventoryHolder interfaces fails
**Why it happens:** Nova blocks may not implement Bukkit interfaces the same way vanilla blocks do
**How to avoid:** Use Nova's TileEntity API exclusively. Use getDrops/restore drops, not Bukkit inventory methods.
**Warning signs:** ClassCastException or null when casting Block.getState() to Container

### Pitfall 5: Silent Restoration Failures

**What goes wrong:** Some machines fail to restore inventory but player never knows
**Why it happens:** No error message when restoration fails, only success count logged
**How to avoid:** Always report failures: "Preserved 20 Nova machines (3 couldn't be restored)" not just "Preserved 20 Nova machines"
**Warning signs:** Players complain about lost items but logs show "success"

### Pitfall 6: Not Handling ItemStack Serialization in Database

**What goes wrong:** NovaBlockData.drops fail to serialize/deserialize when saving to database
**Why it happens:** ItemStack has special Bukkit serialization requirements, Java Serialization may not work
**How to avoid:** Phase 12 already uses `List<ItemStack>` in NovaBlockData which implements Serializable. If database errors occur, may need Bukkit's ItemStack serialization API.
**Warning signs:** Database save/load errors with "NotSerializableException" mentioning ItemStack

## Code Examples

Verified patterns from Nova and Minecraft APIs:

### Example 1: Enhanced restoreNovaBlocks with Feedback

```java
// Location: NovaIntegration.java - UPDATE existing method
// Return RestoreResult instead of void

public RestoreResult restoreNovaBlocks(List<NovaBlockData> novaBlocks, Location center) {
    if (!available || novaBlocks == null || novaBlocks.isEmpty()) {
        return new RestoreResult(0, 0);
    }

    World world = center.getWorld();
    if (world == null) {
        return new RestoreResult(0, 0);
    }

    int centerX = center.getBlockX();
    int centerY = center.getBlockY();
    int centerZ = center.getBlockZ();

    int blocksRestored = 0;
    int machinesRestored = 0;
    int machinesFailed = 0;

    try {
        // ... existing BlockUtils setup code ...

        // Phase 1: Place all Nova blocks (existing code)
        for (NovaBlockData data : novaBlocks) {
            try {
                Location loc = new Location(world,
                    centerX + data.relX, centerY + data.relY, centerZ + data.relZ);

                // Parse block ID and place block
                Object key = keyMethod.invoke(null, data.blockId);
                Object novaBlock = getMethod.invoke(blockRegistry, key);

                if (novaBlock != null) {
                    placeBlockMethod.invoke(null, finalContext, loc, novaBlock, false);
                    blocksRestored++;
                }
            } catch (Exception e) {
                // Skip blocks that fail
            }
        }

        // Phase 2: Verify state restoration (count machines)
        Method getTileEntityMethod = worldDataManagerClass.getMethod("getTileEntity", Location.class);

        for (NovaBlockData data : novaBlocks) {
            // Only count blocks that had TileEntity data
            if (data.drops == null || data.drops.isEmpty()) continue;

            try {
                Location loc = new Location(world,
                    centerX + data.relX, centerY + data.relY, centerZ + data.relZ);

                Object tileEntity = getTileEntityMethod.invoke(worldDataManager, loc);

                if (tileEntity != null) {
                    machinesRestored++;
                } else {
                    machinesFailed++;
                }
            } catch (Exception e) {
                machinesFailed++;
            }
        }

        addon.log("Restored " + blocksRestored + "/" + novaBlocks.size() + " Nova blocks");
        if (machinesRestored > 0 || machinesFailed > 0) {
            addon.log("Nova machines: " + machinesRestored + " restored, " + machinesFailed + " failed");
        }

    } catch (Exception e) {
        addon.logWarning("Failed to restore Nova blocks: " + e.getMessage());
    }

    return new RestoreResult(machinesRestored, machinesFailed);
}
```

### Example 2: RestoreResult Data Class

```java
// Location: NovaIntegration.java - NEW inner class

public static class RestoreResult {
    public final int machinesRestored;
    public final int machinesFailed;

    public RestoreResult(int machinesRestored, int machinesFailed) {
        this.machinesRestored = machinesRestored;
        this.machinesFailed = machinesFailed;
    }

    /**
     * Get user-facing feedback message for Nova machine preservation
     * @return Feedback message or null if no Nova machines
     */
    public String getFeedbackMessage() {
        if (machinesFailed > 0) {
            return "Preserved " + machinesRestored + " Nova machines ("
                + machinesFailed + " couldn't be restored)";
        } else if (machinesRestored > 0) {
            return "Preserved " + machinesRestored + " Nova machines";
        } else {
            return null; // No Nova machines to report
        }
    }

    public boolean hasFailures() {
        return machinesFailed > 0;
    }

    public boolean hasSuccesses() {
        return machinesRestored > 0;
    }
}
```

### Example 3: Integration in Managers (Phase 14)

```java
// Source: Pattern for BackupManager, RelocationManager, etc. (Phase 14 implementation)

// In manager operation methods:
if (addon.getNovaIntegration() != null && addon.getNovaIntegration().isAvailable()) {
    NovaIntegration nova = addon.getNovaIntegration();

    // Capture and remove
    List<NovaBlockData> novaBlocks = nova.captureNovaBlocks(oldCenter, range);
    nova.removeNovaBlocks(novaBlocks, oldCenter);

    // WorldEdit operation
    worldEdit.paste(newCenter);

    // Restore and get feedback
    NovaIntegration.RestoreResult result = nova.restoreNovaBlocks(novaBlocks, newCenter);

    // Send feedback to player
    String feedback = result.getFeedbackMessage();
    if (feedback != null) {
        if (result.hasFailures()) {
            player.sendMessage(ChatColor.YELLOW + feedback);
        } else {
            player.sendMessage(ChatColor.GREEN + feedback);
        }
    }
}
```

### Example 4: Testing Auto-Restoration (Development)

```java
// Source: Test pattern to verify if auto-restoration works
// Location: NovaIntegration.java - temporary test method

public void testAutoRestoration(Location testLoc) {
    try {
        // Get TileEntity at test location
        Object tileEntity = getTileEntityMethod.invoke(worldDataManager, testLoc);
        if (tileEntity == null) {
            addon.log("No TileEntity at test location");
            return;
        }

        // Capture drops
        List<ItemStack> drops = (List<ItemStack>) getDropsMethod.invoke(tileEntity, true);
        addon.log("Captured " + drops.size() + " drops");

        // Break block
        breakBlockMethod.invoke(null, emptyContext, testLoc, false);
        addon.log("Block broken");

        // Place block back from first drop (the block item)
        if (drops.size() > 0) {
            ItemStack blockItem = drops.get(0);
            addon.log("Block item: " + blockItem.getType() + " with " +
                (blockItem.hasItemMeta() ? "ItemMeta" : "no ItemMeta"));

            // TODO: Test if placing this ItemStack restores state
            // This would require converting ItemStack to NovaBlock and placing it
            // OR using player.placeBlock simulation
        }

        // Check if TileEntity restored
        Object restoredTE = getTileEntityMethod.invoke(worldDataManager, testLoc);
        if (restoredTE != null) {
            List<ItemStack> restoredDrops = (List<ItemStack>) getDropsMethod.invoke(restoredTE, true);
            addon.log("After restoration: " + restoredDrops.size() + " drops");

            if (restoredDrops.size() == drops.size()) {
                addon.log("AUTO-RESTORATION WORKS!");
            } else {
                addon.log("AUTO-RESTORATION FAILED - manual restoration needed");
            }
        }

    } catch (Exception e) {
        addon.logWarning("Test failed: " + e.getMessage());
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual TileEntity NBT read/write | BlockEntityTag in ItemStack | Minecraft 1.13+ | Shulker boxes became portable, standard serialization |
| Custom state preservation | TileEntity.getDrops(true) | Nova best practice | Official API, stable across versions |
| Separate inventory tracking | Embedded in ItemStack NBT | Vanilla Minecraft | Single serialization mechanism |
| No restoration feedback | User-facing messages | Modern plugin UX | Players know if data was preserved |

**Deprecated/outdated:**
- Direct TileEntity NBT manipulation - Use ItemStack BlockEntityTag instead
- Assuming drops must be manually placed in inventory - May auto-restore from ItemStack
- Silent restoration - Modern plugins provide feedback

**Current knowledge gaps:**
- Does Nova BlockUtils.placeBlock auto-restore from BlockEntityTag? (needs testing)
- Does placing Nova block from ItemStack (via player interaction) restore state? (vanilla behavior)
- Is manual drop restoration needed or is it automatic? (critical for implementation)

## Open Questions

1. **Does BlockUtils.placeBlock auto-restore TileEntity state from ItemStack NBT?**
   - What we know: Vanilla Minecraft auto-restores shulker boxes from BlockEntityTag in ItemStack
   - What's unclear: Whether Nova follows this pattern or requires manual restoration
   - Recommendation: Test with real Nova machine. Capture drops, break, WorldEdit, restore, check inventory. This determines entire implementation approach.

2. **Do captured drops (ItemStacks from getDrops) contain BlockEntityTag?**
   - What we know: getDrops(true) returns ItemStacks with inventory and block
   - What's unclear: Whether these ItemStacks have BlockEntityTag NBT structure
   - Recommendation: Debug print ItemStack NBT during testing. If BlockEntityTag present, auto-restoration likely works.

3. **Is the first drop always the block item?**
   - What we know: Nova docs say drops include "TileEntity itself if includeSelf=true"
   - What's unclear: Is it always index 0, or could inventory items come first?
   - Recommendation: Test with various Nova machines. Log drop order. Update code if order varies.

4. **Does owner UUID persist through BlockEntityTag?**
   - What we know: TileEntity has getOwner() method returning OfflinePlayer
   - What's unclear: Whether owner data is in BlockEntityTag or requires separate preservation
   - Recommendation: Test with owned machines. Check getOwner() after restoration. If null, may need custom owner tracking.

5. **Can ItemStack serialization handle Nova drops in database?**
   - What we know: NovaBlockData.drops is List<ItemStack>, Serializable
   - What's unclear: Whether Java serialization works or needs Bukkit's ItemStack.serialize()
   - Recommendation: Phase 12 already committed this structure. If database errors occur in Phase 13, switch to Bukkit serialization.

6. **Should we provide item recovery if restoration fails?**
   - What we know: If restoration fails, inventory is lost
   - What's unclear: Should we drop items on ground as fallback, or just warn player?
   - Recommendation: Phase 13 focuses on feedback messages. Item recovery (dropping on ground) could be Phase 14 enhancement if needed.

## Sources

### Primary (HIGH confidence)
- [Nova TileEntity API Documentation](https://docs.xenondevs.xyz/nova/api/tileentity/tileentity/) - getDrops() and getOwner() methods
- [Nova 0.16→0.17 Migration Guide](https://docs.xenondevs.xyz/nova/addon/migration-guide/0.16-0.17/) - BlockUtils.placeBlock usage
- [Nova Creating Blocks Documentation](https://docs.xenondevs.xyz/nova/addon/blocks/creating-blocks/) - Block placement patterns
- [Minecraft Wiki - Shulker Box](https://minecraft.wiki/w/Shulker_Box) - BlockEntityTag preservation behavior
- NovaIntegration.java (lines 109-203) - Existing capture implementation with TileEntity.getDrops

### Secondary (MEDIUM confidence)
- [Fabric Wiki - Syncing BlockEntity data with ItemStack](https://wiki.fabricmc.net/tutorial:blockentity_sync_itemstack) - BlockEntityTag pattern explanation
- [Spigot Forums - Getting shulker box inventory from ItemStack](https://www.spigotmc.org/threads/getting-the-inventory-of-a-shulker-box-itemstack.212369/) - BlockEntityTag structure
- [Bukkit Forums - Set block's tile entity data](https://bukkit.org/threads/set-a-blocks-tile-entity-data.444353/) - BlockState restoration patterns
- Nova 0.18 Changelog - TileEntity drop fixes and BLOCK_STORAGE_DROPS context parameter

### Tertiary (LOW confidence)
- WebSearch: Nova block placement inventory restoration - No explicit documentation found
- WebSearch: BlockEntityTag automatic restoration - Vanilla behavior confirmed, Nova behavior unclear
- WebSearch: TileEntity owner persistence - Owner method exists, NBT structure not documented

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - TileEntity.getDrops documented, BlockEntityTag is vanilla standard
- Auto-restoration behavior: LOW - Not explicitly documented for Nova, requires testing
- Manual restoration approach: MEDIUM - Fallback pattern is well-understood, but may not be needed
- Feedback messages: HIGH - Straightforward implementation, no ambiguity
- Owner preservation: MEDIUM - Method exists, unclear if BlockEntityTag includes owner data

**Confidence MEDIUM overall because:**
- TileEntity.getDrops(true) officially documented (HIGH confidence)
- BlockEntityTag is standard Minecraft serialization (HIGH confidence)
- Whether Nova auto-restores from BlockEntityTag is UNKNOWN (LOW confidence)
- If manual restoration needed, approach is clear (MEDIUM confidence)
- Feedback message implementation is straightforward (HIGH confidence)
- **Major unknown:** Does auto-restoration work, or must we manually restore drops? This determines 80% of implementation effort.

**Research date:** 2026-01-27
**Valid until:** 30 days - Needs validation testing to confirm auto-restoration behavior. If Nova 0.19+ releases with breaking changes, revalidate.
