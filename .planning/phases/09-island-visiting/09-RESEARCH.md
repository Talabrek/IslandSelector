# Phase 9: Island Visiting - Research

**Researched:** 2026-01-20
**Domain:** BentoBox Warps addon integration
**Confidence:** HIGH

## Summary

This phase implements conditional island visiting based on warp sign availability. The BentoBox Warps addon provides a well-documented API that can be accessed via the `request()` method or directly through `getWarpSignsManager()`. The existing codebase already has patterns for reflection-based addon integration (LevelIntegration, ChallengesIntegration) that should be followed.

The key insight is that the Warps addon exposes a `hasWarp(UUID, World)` method through its WarpSignsManager that returns whether a player has an active warp sign. This is the primary check needed for VIST-01 through VIST-04.

**Primary recommendation:** Create a `WarpIntegration` class following the LevelIntegration pattern, using reflection to optionally integrate with the Warps addon. Modify `SharedGridGUIListener.handleVisitClick()` to check warp availability before teleporting, and modify `MainGridGUI.createOccupiedItem()` to show/hide warp indicators.

## Standard Stack

The implementation uses existing BentoBox/Java patterns:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| BentoBox Warps | 1.11.2+ | Warp sign management | Official BentoBox addon for warps |
| Java Reflection | JDK | Optional dependency access | Avoids hard dependency on Warps |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| BentoBox API | 1.18.0+ | Addon discovery | Check if Warps is installed |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Reflection | Direct dependency | Hard dependency breaks when Warps not installed |
| request() API | Direct manager access | request() is more abstracted but harder to use with reflection |

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/world/bentobox/islandselector/
├── managers/
│   └── WarpIntegration.java     # NEW: Warps addon integration
├── gui/
│   ├── SharedGridGUIListener.java  # MODIFY: Add warp check to handleVisitClick()
│   └── MainGridGUI.java            # MODIFY: Add warp indicator to createOccupiedItem()
└── IslandSelector.java              # MODIFY: Initialize WarpIntegration
```

### Pattern 1: Reflection-Based Addon Integration
**What:** Access optional BentoBox addon APIs through reflection to avoid hard dependencies
**When to use:** When integrating with addons that may not be installed
**Example:**
```java
// Source: Existing LevelIntegration.java pattern
public class WarpIntegration {
    private final IslandSelector addon;
    private volatile boolean warpAddonPresent = false;
    private volatile boolean warpAddonWorking = true;

    public WarpIntegration(IslandSelector addon) {
        this.addon = addon;
        // Check if Warps addon is present
        warpAddonPresent = BentoBox.getInstance().getAddonsManager()
            .getAddonByName("Warps").isPresent();

        if (warpAddonPresent) {
            addon.log("Warps addon detected - warp checking enabled");
        }
    }

    public boolean isEnabled() {
        return warpAddonPresent && warpAddonWorking;
    }

    public boolean hasWarp(UUID playerUUID) {
        if (!isEnabled()) {
            return false;
        }
        try {
            var warpsAddon = BentoBox.getInstance().getAddonsManager()
                .getAddonByName("Warps");
            if (warpsAddon.isEmpty()) {
                return false;
            }
            Object warp = warpsAddon.get();

            // Get WarpSignsManager
            Method getManager = findMethod(warp.getClass(), "getWarpSignsManager");
            if (getManager != null) {
                getManager.setAccessible(true);
                Object manager = getManager.invoke(warp);
                if (manager != null) {
                    World world = addon.getGridManager().getBSkyBlockWorld();
                    Method hasWarpMethod = findMethod(manager.getClass(),
                        "hasWarp", World.class, UUID.class);
                    if (hasWarpMethod != null) {
                        hasWarpMethod.setAccessible(true);
                        Object result = hasWarpMethod.invoke(manager, world, playerUUID);
                        return result instanceof Boolean && (Boolean) result;
                    }
                }
            }
        } catch (LinkageError e) {
            warpAddonWorking = false;
        } catch (Exception e) {
            // Silent fail
        }
        return false;
    }
}
```

### Pattern 2: Conditional GUI Lore
**What:** Show/hide lore lines based on feature availability
**When to use:** When GUI elements depend on external addon state
**Example:**
```java
// In createOccupiedItem(), conditionally add warp indicator
if (!isOwnIsland) {
    lore.add("");
    if (addon.getWarpIntegration().isEnabled()) {
        boolean hasWarp = addon.getWarpIntegration().hasWarp(ownerUUID);
        if (hasWarp) {
            lore.add(colorize("&a\u2713 Has Warp"));  // checkmark
            lore.add(colorize("&e\u25b6 Right-click to visit"));  // arrow
        }
        // No "Right-click to visit" hint when no warp
    } else {
        // Warps addon not installed - keep existing behavior
        lore.add(colorize("&e\u25b6 Right-click to visit"));
    }
}
```

### Anti-Patterns to Avoid
- **Hard dependency on Warps:** Don't add Warps to addon.yml depend or import Warps classes directly
- **Caching warp status:** Warp signs can be placed/removed at any time, so always check live
- **Blocking on warp check:** The reflection call is fast, but wrap in try-catch to avoid blocking

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Warp sign detection | Scanning for signs | Warps addon API | Handles all edge cases, permissions |
| Safe teleportation | Direct teleport | SafeSpotTeleport (already used) | Finds safe landing spots |
| Addon detection | Plugin.isEnabled() | BentoBox AddonsManager.getAddonByName() | Correct for BentoBox addons |

**Key insight:** The Warps addon already handles all the complexity of warp sign management. We just need to query whether a warp exists.

## Common Pitfalls

### Pitfall 1: Assuming Warps is Always Installed
**What goes wrong:** NullPointerException or ClassNotFoundException at runtime
**Why it happens:** Warps is optional - many servers don't use it
**How to avoid:** Check `isEnabled()` before every warp-related operation
**Warning signs:** Code that imports `world.bentobox.warps.*` directly

### Pitfall 2: Checking Warp After Teleport Decision
**What goes wrong:** User clicks, teleport starts, then warp check fails
**Why it happens:** Checking warp status too late in the flow
**How to avoid:** Check warp status BEFORE showing "Right-click to visit" hint, and again before teleporting
**Warning signs:** Race conditions between GUI display and click handling

### Pitfall 3: Wrong Method Signature for hasWarp
**What goes wrong:** NoSuchMethodException from reflection
**Why it happens:** Warps API uses `hasWarp(World, UUID)` - world comes first
**How to avoid:** Verify parameter order matches Warps addon source
**Warning signs:** Reflection call returns null when warp exists

### Pitfall 4: Using request() API vs Direct Manager
**What goes wrong:** More complex reflection with Map parameters
**Why it happens:** The `request()` method requires a metadata Map with specific keys
**How to avoid:** Use `getWarpSignsManager()` directly - simpler and more direct
**Warning signs:** Complex Map construction for simple boolean check

## Code Examples

Verified patterns from official sources and existing codebase:

### Check if Warps Addon is Installed
```java
// Source: Existing pattern in LevelIntegration.java
boolean warpsPresent = BentoBox.getInstance().getAddonsManager()
    .getAddonByName("Warps").isPresent();
```

### Get WarpSignsManager via Reflection
```java
// Source: Warps addon API (GitHub)
// The Warp class has: public WarpSignsManager getWarpSignsManager()
var warpsAddon = BentoBox.getInstance().getAddonsManager().getAddonByName("Warps");
if (warpsAddon.isPresent()) {
    Object warp = warpsAddon.get();
    Method getManager = warp.getClass().getMethod("getWarpSignsManager");
    Object manager = getManager.invoke(warp);
    // manager is WarpSignsManager
}
```

### Check if Player Has Warp
```java
// Source: WarpSignsManager API
// Method: public boolean hasWarp(World world, UUID uuid)
World bskyWorld = addon.getGridManager().getBSkyBlockWorld();
Method hasWarp = manager.getClass().getMethod("hasWarp", World.class, UUID.class);
boolean result = (Boolean) hasWarp.invoke(manager, bskyWorld, playerUUID);
```

### Existing handleVisitClick() to Modify
```java
// Source: SharedGridGUIListener.java lines 156-207
// Current behavior: Always teleports to any occupied island
// New behavior: Check hasWarp() first, show message if no warp

private void handleVisitClick(MainGridGUI gui, Player player, GridCoordinate coord) {
    // ... existing validation ...

    // NEW: Check for warp before teleporting
    WarpIntegration warpIntegration = gui.getAddon().getWarpIntegration();
    if (warpIntegration.isEnabled()) {
        if (!warpIntegration.hasWarp(location.getOwnerUUID())) {
            player.sendMessage("\u00A7cThis island doesn't have a warp sign.");
            return;
        }
    }

    // ... existing teleport logic ...
}
```

### Existing createOccupiedItem() Lore Section to Modify
```java
// Source: MainGridGUI.java lines 516-520
// Current:
//     if (!isOwnIsland) {
//         lore.add("");
//         lore.add(colorize("&e\u25b6 Right-click to visit"));
//     }
//
// New: Conditionally show based on warp availability
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Direct class import | Reflection-based | Always for optional deps | Prevents NoClassDefFoundError |
| Plugin.isEnabled() | AddonsManager.getAddonByName() | BentoBox addon pattern | Correct addon lifecycle |

**Deprecated/outdated:**
- WarpListEvent: Deprecated in Warps addon, use direct API instead

## Open Questions

Things that couldn't be fully resolved:

1. **Warp location vs just existence**
   - What we know: `hasWarp()` returns boolean, `getWarpLocation()` returns Location
   - What's unclear: Should we use warp location for teleport instead of island spawn?
   - Recommendation: Use `hasWarp()` for check, but teleport to warp location if available (enhancement)

2. **Multi-world warp handling**
   - What we know: Warps uses World parameter, we have multi-dimension support
   - What's unclear: Does player need warp in each dimension?
   - Recommendation: Check warp in primary BSkyBlock world only (simplest approach)

## Sources

### Primary (HIGH confidence)
- BentoBox Warps GitHub repository - https://github.com/BentoBoxWorld/Warps
- Warp.java source code - Method signatures for `getWarpSignsManager()` and `request()` API
- WarpSignsManager.java source code - Method signature `hasWarp(World, UUID)`
- Existing LevelIntegration.java in codebase - Reflection pattern for optional addon integration

### Secondary (MEDIUM confidence)
- BentoBox documentation - https://docs.bentobox.world/en/latest/addons/Warps/
- WebFetch of Warps addon documentation and API events

### Tertiary (LOW confidence)
- None - all findings verified with primary sources

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Official BentoBox addon with documented API
- Architecture: HIGH - Following existing codebase patterns exactly
- Pitfalls: HIGH - Verified against actual Warps source code

**Research date:** 2026-01-20
**Valid until:** 2026-02-20 (stable addon, API unlikely to change)
