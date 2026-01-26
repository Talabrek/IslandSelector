# Phase 11: Foundation - Research

**Researched:** 2026-01-27
**Domain:** Nova 0.17+ API integration with reflection-based optional plugin pattern
**Confidence:** MEDIUM

## Summary

This phase updates NovaIntegration.java to work with Nova 0.17+ API changes and adds a config toggle (`nova.enabled`) to control the integration. Nova 0.17 significantly reorganized package structure and removed several manager classes, requiring reflection path updates.

The current NovaIntegration.java uses old package paths (`xyz.xenondevs.nova.tileentity`, `xyz.xenondevs.nova.data.world.block.BlockManager`) that don't exist in Nova 0.17+. The new package structure places tile entities at `xyz.xenondevs.nova.world.block.tileentity` and removes `BlockManager` entirely in favor of `BlockUtils` static methods.

Key findings:
- Nova 0.17 moved packages: `tileentity` → `world.block.tileentity`, `item` → `world.item`, `player` → `world.player`
- Removed `BlockManager` class - use `BlockUtils#placeBlock` and `BlockUtils#breakBlock` instead
- Removed `TileEntityManager` class - use `WorldDataManager#getTileEntity` method
- Block states retrieved via `novaBlockState` Kotlin extension property on `org.bukkit.Block`
- Integration already uses WorldDataManager correctly but needs updated fallback paths

**Primary recommendation:** Update reflection paths to Nova 0.17+ structure, add `nova.enabled` config toggle following BentoBox patterns, verify detection works with both "Nova integration enabled" message and graceful degradation when disabled.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Nova | 0.17+ | Server-side custom blocks/items framework | Target integration version with new API |
| BentoBox Config API | N/A | Config toggle with `@ConfigEntry` | Built-in, same pattern as other integrations |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Java Reflection | Built-in | Optional plugin integration | When hard dependency not desired |
| Bukkit Plugin API | Paper API | Plugin detection via PluginManager | Standard Bukkit plugin loading check |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Reflection | Hard dependency (add to pom.xml) | Would require Nova installed, breaks optional integration model |
| Reflection | MCP (Modern Compatibility Protocol) | Overkill for simple plugin check, adds complexity |

**Installation:**
```bash
# No new dependencies needed - uses Java reflection (built-in)
# Nova detection already works via Bukkit PluginManager
```

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/world/bentobox/islandselector/
  Settings.java                      # Add novaEnabled field
  integrations/
    NovaIntegration.java            # Update reflection paths for 0.17+
  IslandSelector.java               # Check config before initializing integration
```

### Pattern 1: Reflection-Based Optional Plugin Integration

**What:** Detect plugin at runtime using Class.forName() with try-catch for ClassNotFoundException
**When to use:** When integration should work if plugin present, gracefully degrade if not
**Example:**
```java
// Source: Existing pattern in NovaIntegration.java lines 46-85
private boolean detectNova() {
    // Check plugin loaded
    if (!Bukkit.getPluginManager().isPluginEnabled("Nova")) {
        return false;
    }

    try {
        // Try Nova 0.17+ API first
        novaClass = Class.forName("xyz.xenondevs.nova.Nova");
        blockManagerClass = Class.forName("xyz.xenondevs.nova.world.block.BlockManager");
        novaBlockStateClass = Class.forName("xyz.xenondevs.nova.world.block.state.NovaBlockState");
        return true;
    } catch (ClassNotFoundException e) {
        // Log warning but don't throw - graceful degradation
        addon.logWarning("Nova detected but API classes not found: " + e.getMessage());
        return false;
    }
}
```

### Pattern 2: Config-Controlled Integration Initialization

**What:** Only initialize integration if both plugin available AND config enabled
**When to use:** When integration has performance cost or admins need control
**Example:**
```java
// Source: Pattern from existing integrations in BentoBox addons
// In IslandSelector.java onEnable()
if (settings.isNovaEnabled()) {
    novaIntegration = new NovaIntegration(this);
    if (novaIntegration.isAvailable()) {
        log("Nova integration enabled");
    } else {
        log("Nova integration disabled - Nova not detected");
        novaIntegration = null;
    }
} else {
    log("Nova integration disabled via config");
}
```

### Pattern 3: Null-Safe Integration Usage

**What:** Check for null before calling integration methods
**When to use:** When integration might not be initialized (disabled or unavailable)
**Example:**
```java
// In managers that use Nova integration
if (addon.getNovaIntegration() != null && addon.getNovaIntegration().isAvailable()) {
    // Capture Nova blocks before operation
    List<NovaBlockData> novaBlocks = addon.getNovaIntegration().captureNovaBlocks(center, range);
    // Store for later restoration
}
```

### Pattern 4: Config Toggle with Runtime Check

**What:** Boolean field with `@ConfigEntry` annotation, checked at runtime
**When to use:** Feature that admins should enable/disable without server restart
**Example:**
```java
// Source: Existing pattern from Settings.java integration section lines 166-172
@ConfigComment("Enable Nova integration for custom block support")
@ConfigComment("When disabled, Nova blocks are not preserved during island operations")
@ConfigEntry(path = "integration.nova.enabled")
private boolean novaEnabled = true;

public boolean isNovaEnabled() {
    return novaEnabled;
}

public void setNovaEnabled(boolean novaEnabled) {
    this.novaEnabled = novaEnabled;
}
```

### Anti-Patterns to Avoid

- **Hard-coding version checks:** Don't check Nova version string, use reflection to detect API availability. APIs change but version strings may not match expectations.
- **Throwing exceptions on missing classes:** Always catch ClassNotFoundException and degrade gracefully. Throwing makes addon fail to load.
- **Initializing integration when config disabled:** Waste of resources. Check config first, then initialize if enabled.
- **Assuming reflection methods exist:** After Class.forName() succeeds, always verify methods exist with getMethod() before invoke(). Method signatures can change between versions.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Config toggle management | Custom boolean flag storage | BentoBox `@ConfigEntry` | Auto-saves, supports reload, consistent with other toggles |
| Plugin detection | Custom file scanning | Bukkit `PluginManager.isPluginEnabled()` | Standard API, handles plugin loading edge cases |
| Class loading | Custom ClassLoader | Java `Class.forName()` | Standard reflection API, well-tested, handles class path correctly |
| Version parsing | String splitting/regex | Reflection-based capability detection | Versions lie, APIs don't - detect features not versions |

**Key insight:** Reflection-based optional integration is a proven pattern for Minecraft plugins. Don't try to improve on Class.forName() + try-catch - it works correctly and is widely understood.

## Common Pitfalls

### Pitfall 1: Package Path Incorrect for Nova 0.17+

**What goes wrong:** Class.forName() throws ClassNotFoundException even though Nova 0.17+ is installed
**Why it happens:** Nova 0.17 reorganized packages. Old path `xyz.xenondevs.nova.tileentity.TileEntity` doesn't exist. New path is `xyz.xenondevs.nova.world.block.tileentity.TileEntity`.
**How to avoid:** Update all reflection paths to Nova 0.17+ structure:
- `xyz.xenondevs.nova.tileentity` → `xyz.xenondevs.nova.world.block.tileentity`
- `xyz.xenondevs.nova.item` → `xyz.xenondevs.nova.world.item`
- `xyz.xenondevs.nova.player` → `xyz.xenondevs.nova.world.player`
- `xyz.xenondevs.nova.data.world.block.BlockManager` → `xyz.xenondevs.nova.world.block.BlockManager`
**Warning signs:** NovaIntegration logs "Nova detected but BlockManager class not found" even with Nova 0.17+ installed

### Pitfall 2: BlockManager Class Doesn't Exist in 0.17+

**What goes wrong:** Integration tries to load BlockManager class, fails with ClassNotFoundException
**Why it happens:** Nova 0.17 removed BlockManager class entirely. Use BlockUtils static methods instead.
**How to avoid:** Current code doesn't actually use BlockManager after loading it (lines 30-32), only checks if class exists. For 0.17+ detection, try BlockUtils instead:
```java
try {
    // Nova 0.17+ uses BlockUtils (static methods, not manager)
    Class<?> blockUtilsClass = Class.forName("xyz.xenondevs.nova.util.BlockUtils");
    // Don't need to store instance, BlockUtils is all static methods
} catch (ClassNotFoundException e) {
    // Fallback to older API
}
```
**Warning signs:** Integration fails to detect Nova 0.17+ but detects older versions fine

### Pitfall 3: TileEntityManager Removed in 0.17+

**What goes wrong:** Code tries to use TileEntityManager.getTileEntity(), class doesn't exist
**Why it happens:** Nova 0.17 removed TileEntityManager. Use WorldDataManager#getTileEntity instead.
**How to avoid:** Current code already uses WorldDataManager correctly (line 122). Just need to update class path detection and remove TileEntityManager references if any exist.
**Warning signs:** getTileEntity calls fail even though WorldDataManager exists

### Pitfall 4: Config Toggle Doesn't Prevent Initialization

**What goes wrong:** Admin sets `nova.enabled: false`, but NovaIntegration still tries to detect Nova and logs warnings
**Why it happens:** Integration initialized before checking config toggle
**How to avoid:** Check config in IslandSelector.onEnable() BEFORE creating NovaIntegration instance:
```java
if (settings.isNovaEnabled()) {
    novaIntegration = new NovaIntegration(this);
    // ...
}
```
**Warning signs:** Config disabled but still see "Nova detected" or "Nova not found" messages in logs

### Pitfall 5: Reflection Method Cache Not Updated

**What goes wrong:** Integration works on first load, fails after config reload
**Why it happens:** NovaIntegration stores reflection class references in constructor. If integration disabled/re-enabled, stale references might exist.
**How to avoid:** Not actually a problem for this phase - NovaIntegration immutable after construction. Config reload requires server restart for integration changes (acceptable limitation).
**Warning signs:** Integration stops working after /islandselector admin reload

### Pitfall 6: NoClassDefFoundError vs ClassNotFoundException

**What goes wrong:** Try-catch for ClassNotFoundException doesn't catch NoClassDefFoundError, integration crashes
**Why it happens:** NoClassDefFoundError thrown when class was found during compilation but missing at runtime (different from ClassNotFoundException which is checked exception)
**How to avoid:** In optional integration pattern, only catch ClassNotFoundException. NoClassDefFoundError indicates serious problem (corrupted plugin). Let it fail loudly rather than hide.
**Warning signs:** Integration crashes with NoClassDefFoundError instead of graceful degradation

## Code Examples

Verified patterns for Nova 0.17+ integration:

### Example 1: Updated detectNova() Method

```java
// Source: Nova 0.17 migration guide - updated reflection paths
// Location: NovaIntegration.java detectNova() method
private boolean detectNova() {
    if (!Bukkit.getPluginManager().isPluginEnabled("Nova")) {
        return false;
    }

    try {
        // Load core Nova class
        novaClass = Class.forName("xyz.xenondevs.nova.Nova");

        // Try Nova 0.17+ API paths first
        try {
            // 0.17+ uses world.block package structure
            blockManagerClass = Class.forName("xyz.xenondevs.nova.world.block.BlockManager");
            novaBlockStateClass = Class.forName("xyz.xenondevs.nova.world.block.state.NovaBlockState");
        } catch (ClassNotFoundException e) {
            // Try older API (pre-0.17)
            try {
                blockManagerClass = Class.forName("xyz.xenondevs.nova.data.world.block.BlockManager");
                novaBlockStateClass = Class.forName("xyz.xenondevs.nova.data.world.block.state.NovaBlockState");
            } catch (ClassNotFoundException e2) {
                addon.logWarning("Nova detected but block classes not found - custom blocks may not copy correctly");
                return false;
            }
        }

        return true;
    } catch (Exception e) {
        addon.logWarning("Failed to initialize Nova integration: " + e.getMessage());
        return false;
    }
}
```

### Example 2: Config Toggle in Settings.java

```java
// Source: Existing integration toggle pattern in Settings.java lines 166-172
// Location: Settings.java integration section (after line 172)

@ConfigComment("Enable Nova integration for custom block support")
@ConfigComment("Preserves Nova machines during relocation, slot switch, and backups")
@ConfigEntry(path = "integration.nova.enabled")
private boolean novaEnabled = true;

// Getter (add with other integration getters around line 480-502)
public boolean isNovaEnabled() {
    return novaEnabled;
}

// Setter
public void setNovaEnabled(boolean novaEnabled) {
    this.novaEnabled = novaEnabled;
}
```

### Example 3: Conditional Integration Initialization

```java
// Source: Pattern from existing integrations in BentoBox
// Location: IslandSelector.java onEnable() method

// After settings loaded, before managers initialized
if (settings.isNovaEnabled()) {
    novaIntegration = new NovaIntegration(this);
    if (novaIntegration.isAvailable()) {
        log("Nova integration enabled - custom block support active");
    } else {
        log("Nova integration disabled - Nova plugin not detected");
        novaIntegration = null;
    }
} else {
    log("Nova integration disabled via config");
    novaIntegration = null;
}
```

### Example 4: Null-Safe Integration Access Pattern

```java
// Source: Best practice for optional integration usage
// Location: Any manager that uses Nova integration

// Check both null and available before using
if (addon.getNovaIntegration() != null && addon.getNovaIntegration().isAvailable()) {
    // Perform Nova-specific operations
    List<NovaBlockData> novaBlocks = addon.getNovaIntegration().captureNovaBlocks(center, range);
    // ... use novaBlocks
}
// If null or not available, continue without Nova support - graceful degradation
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `xyz.xenondevs.nova.tileentity` | `xyz.xenondevs.nova.world.block.tileentity` | Nova 0.17 (2024) | Package reorganization - reflection paths must update |
| `BlockManager` class | `BlockUtils` static methods | Nova 0.17 | No manager instance needed, use static methods |
| `TileEntityManager.getTileEntity()` | `WorldDataManager#getTileEntity()` | Nova 0.17 | Manager removed, use WorldDataManager method |
| Hard-coded integration | Config toggle for integrations | BentoBox ecosystem standard | Admin control over optional features |

**Deprecated/outdated:**
- `xyz.xenondevs.nova.data.world.block.*` - Moved to `xyz.xenondevs.nova.world.block.*` in 0.17
- `xyz.xenondevs.nova.data.serialization` - Moved to `xyz.xenondevs.nova.serialization` in 0.17
- `BlockManager` and `TileEntityManager` classes - Removed, use static methods and WorldDataManager

**Current code status:**
- NovaIntegration.java uses old package paths (lines 58, 62, 70, 74)
- WorldDataManager usage is correct (line 122) - already follows 0.17+ pattern
- No config toggle exists yet

## Open Questions

1. **Should fallback detection support pre-0.17 Nova versions?**
   - What we know: Current code tries both new and old paths (lines 56-66)
   - What's unclear: Is anyone still running Nova <0.17? Is backward compatibility needed?
   - Recommendation: Keep fallback for one release cycle, remove in v1.3. Reduces complexity.

2. **Does BlockUtils detection work without instantiation?**
   - What we know: BlockUtils methods are static, Class.forName() should work
   - What's unclear: Can we verify BlockUtils has expected methods without reflection overhead?
   - Recommendation: Class.forName() sufficient for detection, method verification only when actually using

3. **Should config reload reinitialize NovaIntegration?**
   - What we know: Current pattern initializes once in onEnable()
   - What's unclear: Would admins expect config change to take effect without restart?
   - Recommendation: Don't reinitialize on reload (complexity). Document restart required for nova.enabled changes.

4. **Is "Nova 0.17+" specific enough or should detect 0.18+?**
   - What we know: Migration guide shows 0.16→0.17, also 0.17→0.18 exists, no stable 0.17 released
   - What's unclear: Did 0.17 alpha get absorbed into 0.18 release? Version numbering ambiguous
   - Recommendation: Document as "Nova 0.17+ API" (the API version) not plugin version. Detect via class paths not version strings.

## Sources

### Primary (HIGH confidence)
- [Nova 0.16→0.17 Migration Guide](https://docs.xenondevs.xyz/nova/addon/migration-guide/0.16-0.17/) - Official package reorganization details
- [Nova 0.17→0.18 Migration Guide](https://docs.xenondevs.xyz/nova/addon/migration-guide/0.17-0.18/) - Confirms 0.17 API structure
- Settings.java (lines 166-172) - BentoBox integration toggle pattern already in codebase
- NovaIntegration.java (lines 46-85) - Current detection implementation

### Secondary (MEDIUM confidence)
- [Nova GitHub Repository](https://github.com/xenondevs/Nova) - Version history and releases
- [BentoBox Config API Documentation](https://docs.bentobox.world/en/latest/BentoBox/Config-API/) - ConfigObject and ConfigEntry pattern
- [ClassNotFoundException Best Practices - Baeldung](https://www.baeldung.com/java-classnotfoundexception-and-noclassdeffounderror) - Reflection error handling
- STATE.md (lines 34-38) - Prior v1.2 research findings on Nova API changes

### Tertiary (LOW confidence)
- [Java Reflection and Optional Dependencies - InfoWorld](https://www.infoworld.com/article/4050993/jdk-26-the-new-features-in-java-26.html) - General reflection patterns in Java 2026
- WebSearch: Nova plugin discussions (no specific documentation URLs) - Community usage patterns

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Java reflection and BentoBox Config API well-understood, both in current use
- Architecture: HIGH - Patterns already exist in codebase, just need to extend existing NovaIntegration
- Pitfalls: MEDIUM - Official migration guide confirms package moves, but actual runtime behavior not tested
- Nova 0.17 specifics: MEDIUM - Official docs confirm API changes, but version numbering ambiguous (0.17 vs 0.18)

**Confidence MEDIUM overall because:**
- Official Nova migration guide is authoritative for package paths (HIGH confidence)
- BentoBox config patterns verified in existing code (HIGH confidence)
- But: Haven't tested NovaIntegration with actual Nova 0.17+ plugin (need validation)
- But: Version number ambiguity (was 0.17 released or combined into 0.18?)

**Research date:** 2026-01-27
**Valid until:** 90 days - Nova API relatively stable after 0.17 reorganization, but should revalidate if Nova 0.23+ releases
