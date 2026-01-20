# Phase 10: Relocation Safety - Research

**Researched:** 2026-01-21
**Domain:** Bukkit/BentoBox player teleportation and island location detection
**Confidence:** HIGH

## Summary

This phase addresses a bug where island relocation teleports players who are NOT on the island being relocated. The current implementation unconditionally teleports:
1. The island owner to spawn at relocation start
2. The island owner to new location at relocation end
3. All online team members to new location (regardless of where they are)

The fix requires checking if each player is actually standing on the island before teleporting them. BentoBox provides `island.onIsland(Location)` which works across all dimensions (overworld, nether, end).

**Primary recommendation:** Add `island.onIsland(player.getLocation())` check before any teleportation to ensure only players physically on the island are affected.

## Standard Stack

This phase uses existing APIs already in the codebase - no new dependencies required.

### Core APIs (Already Available)

| API | Source | Purpose | Usage |
|-----|--------|---------|-------|
| `Island.onIsland(Location)` | BentoBox | Check if location is within island's protected area | Filter which players to teleport |
| `Island.getMemberSet()` | BentoBox | Get all island team member UUIDs | Iterate team members to check locations |
| `Bukkit.getOnlinePlayers()` | Bukkit | Get all online players | Find visitors on island |
| `SafeSpotTeleport.Builder` | BentoBox | Safe async teleportation | Already used in codebase |

### Existing Pattern in Codebase

The `SlotSwitchManager.teleportVisitorsAway()` method already demonstrates the correct pattern:

```java
// From SlotSwitchManager.java line 817
if (island.onIsland(player.getLocation())) {
    // This player IS on the island - teleport them
    handleVisitorTeleport(player, island);
}
```

## Architecture Patterns

### Pattern 1: Location Check Before Teleport

**What:** Always verify player location before teleporting during relocation
**When to use:** Before ANY teleportation in relocation flow
**Example:**
```java
// Check if player is actually on the island being relocated
if (island.onIsland(player.getLocation())) {
    // Player IS on the island - teleport to safety
    new SafeSpotTeleport.Builder(addon.getPlugin())
        .entity(player)
        .location(safeLocation)
        .buildFuture();
} else {
    // Player is elsewhere - do nothing
    addon.log("Skipping teleport for " + player.getName() + " - not on island");
}
```

### Pattern 2: Capture Location State Before Async Work

**What:** Record which players need teleportation BEFORE starting async operations
**When to use:** At the start of relocation, before any blocks are moved
**Why:** Player locations can change during async operations; must capture state at decision time
**Example:**
```java
// Capture list of players to teleport at START of relocation
List<Player> playersToTeleport = new ArrayList<>();
for (UUID memberUUID : island.getMemberSet()) {
    Player member = Bukkit.getPlayer(memberUUID);
    if (member != null && member.isOnline() && island.onIsland(member.getLocation())) {
        playersToTeleport.add(member);
    }
}
// Also check for visitors
for (Player player : Bukkit.getOnlinePlayers()) {
    if (!island.getMemberSet().contains(player.getUniqueId())
        && island.onIsland(player.getLocation())) {
        playersToTeleport.add(player);
    }
}
// Now proceed with relocation, using playersToTeleport list
```

### Anti-Patterns to Avoid

- **Unconditional teleport based on membership:** Teleporting all team members regardless of location
- **Late location checking:** Checking player location after async block operations (player may have moved)
- **Owner-only assumption:** Only checking/teleporting the owner, ignoring team members and visitors who may be on island

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Island boundary check | Custom distance calculation | `island.onIsland(location)` | Handles protection range, multi-dimension |
| Safe teleportation | Direct `player.teleport()` | `SafeSpotTeleport.Builder` | Async, finds safe ground |
| Team member lookup | Custom query | `island.getMemberSet()` | BentoBox maintains this |

## Common Pitfalls

### Pitfall 1: Checking Location After Blocks Cleared

**What goes wrong:** Player falls into void between location check and teleport
**Why it happens:** Async block operations take time; player's ground may be deleted before teleport executes
**How to avoid:**
1. Check locations FIRST
2. Teleport affected players to safety BEFORE starting block operations
3. Or capture players at start, teleport regardless of current position
**Warning signs:** Players occasionally fall into void during relocation

### Pitfall 2: Missing Dimension Support

**What goes wrong:** Player in nether not teleported even though they're on the island's nether
**Why it happens:** Only checking overworld, or using wrong world reference
**How to avoid:** `island.onIsland()` already handles all dimensions (overworld, nether, end) automatically
**Warning signs:** Inconsistent behavior between dimensions

### Pitfall 3: Not Handling Visitors

**What goes wrong:** Visitors standing on island fall into void
**Why it happens:** Only checking team members, not all online players
**How to avoid:** Iterate `Bukkit.getOnlinePlayers()` and check each with `island.onIsland()`
**Warning signs:** Visitors complain about falling during someone else's relocation

### Pitfall 4: Race Condition with Player Movement

**What goes wrong:** Player steps onto island AFTER location check but BEFORE blocks cleared
**Why it happens:** There's a window between decision and execution
**How to avoid:** This is a very edge case; acceptable risk. Alternative: lock island access during relocation
**Warning signs:** Very rare reports of unexpected falls

## Code Examples

### Existing Pattern (SlotSwitchManager.java lines 806-827)
```java
// Source: SlotSwitchManager.java
private void teleportVisitorsAway(Island island, Player owner) {
    try {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Skip if player is the owner or a team member
            if (player.getUniqueId().equals(owner.getUniqueId()) ||
                island.getMemberSet().contains(player.getUniqueId())) {
                continue;
            }

            // Check if player is on the island
            if (island.onIsland(player.getLocation())) {
                // This is a visitor - teleport them away
                handleVisitorTeleport(player, island);
            }
        }
    } catch (Exception e) {
        addon.logError("Failed to teleport visitors: " + e.getMessage());
    }
}
```

### Required Fix Pattern for RelocationManager

```java
// Before teleporting to spawn, check if player is actually on the island
private void performRelocationAsync(Player player, GridCoordinate fromCoord, GridCoordinate toCoord) {
    UUID playerUUID = player.getUniqueId();

    // Get the island BEFORE checking location
    World bskyblockWorld = addon.getGridManager().getBSkyBlockWorld();
    Island island = addon.getIslands().getIsland(bskyblockWorld, playerUUID);

    // Capture origin world for dimension-aware return
    final World originWorld = player.getWorld();

    // Check if player is actually on their island
    final boolean playerIsOnIsland = island != null && island.onIsland(player.getLocation());

    Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
        if (playerIsOnIsland) {
            // Player IS on island - teleport to safety
            Location serverSpawn = getServerSpawn();
            new SafeSpotTeleport.Builder(addon.getPlugin())
                .entity(player)
                .location(serverSpawn)
                .buildFuture();
            sendProgress(player, "&eTeleported to spawn for safety during relocation...");
        } else {
            // Player is NOT on island - skip initial teleport
            addon.log("Skipping pre-relocation teleport - " + player.getName() + " not on island");
        }

        // Continue with async relocation work, passing the flag
        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            performRelocationWork(player, playerUUID, fromCoord, toCoord, originWorld, playerIsOnIsland);
        });
    });
}
```

## State of the Art

| Old Approach | Current Approach | Impact |
|--------------|------------------|--------|
| Teleport all team members | Check `island.onIsland()` first | Only affected players teleported |
| Owner always teleported | Check owner location too | Owner at spawn stays at spawn |
| Visitors handled reactively | Proactively check all players | Prevents falling into void |

**Key insight:** The `island.onIsland(Location)` method is the single source of truth for "is this player standing on this island" across all dimensions.

## Files to Modify

Based on grep analysis, these are the specific locations requiring changes:

### RelocationManager.java

1. **`performRelocationAsync()`** (line ~565)
   - Add `island.onIsland(player.getLocation())` check before teleport to spawn
   - Pass flag to indicate if player needs final teleport

2. **`performAdminRelocationAsync()`** (line ~279)
   - Add `island.onIsland(targetPlayer.getLocation())` check before teleport to spawn
   - Only teleport online target player if they're on the island

3. **`teleportPlayersSafely()`** (line ~1236)
   - Add `island.onIsland()` check before teleporting owner
   - Add `island.onIsland()` check for each team member before teleporting

4. **`teleportPlayersSafelyForAdmin()`** (line ~510)
   - Add `island.onIsland()` check before teleporting target player

5. **`handleVisitorsAfterRelocation()`** (line ~1391) - Already correct
   - This method already handles visitors properly by checking their current state

## Open Questions

None. The BentoBox API provides all needed functionality.

## Sources

### Primary (HIGH confidence)
- BentoBox GitHub Island.java - `onIsland()` method analysis
  - https://github.com/BentoBoxWorld/BentoBox/blob/develop/src/main/java/world/bentobox/bentobox/database/objects/Island.java
- BentoBox GitHub IslandsManager.java - Location checking methods
  - https://github.com/BentoBoxWorld/BentoBox/blob/develop/src/main/java/world/bentobox/bentobox/managers/IslandsManager.java
- Existing codebase: `SlotSwitchManager.teleportVisitorsAway()` demonstrates working pattern

### Secondary (MEDIUM confidence)
- BentoBox Documentation: https://docs.bentobox.world/

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Using existing BentoBox APIs already in codebase
- Architecture: HIGH - Pattern demonstrated in SlotSwitchManager.java
- Pitfalls: HIGH - Based on code analysis and BentoBox documentation

**Research date:** 2026-01-21
**Valid until:** 90 days (stable BentoBox API, no expected changes)
