# Phase 6: Command Alias - Research

**Researched:** 2026-01-20
**Domain:** BentoBox command aliases, Bukkit command registration
**Confidence:** HIGH

## Summary

This phase requires adding `/map` as an alias for the existing `/islandselector` command. Research confirms this is a trivial one-line change in BentoBox addons.

The `IslandSelectorCommand` class already extends `CompositeCommand` and defines aliases in its constructor. Currently it uses `"islandselector", "is", "isgrid"`. Adding `"map"` as a fourth alias is the complete implementation.

BentoBox's `CompositeCommand` handles all alias registration automatically through the constructor varargs. No additional configuration, registration, or plugin.yml changes are needed.

**Primary recommendation:** Add `"map"` to the `IslandSelectorCommand` constructor aliases.

## Standard Stack

The established approach for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| BentoBox API | 2.4.0 | Command framework | Already in use, CompositeCommand handles everything |

### Supporting
No additional libraries needed.

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Constructor alias | Bukkit commands.yml | Requires server admin config, not bundled with addon |
| Constructor alias | Manual CommandMap registration | Unnecessary complexity, BentoBox handles it |

**Installation:**
No additional dependencies required.

## Architecture Patterns

### Current Command Structure
```java
public class IslandSelectorCommand extends CompositeCommand {
    public IslandSelectorCommand(IslandSelector addon) {
        super(addon, "islandselector", "is", "isgrid");
        // Primary: /islandselector
        // Alias 1: /is
        // Alias 2: /isgrid
    }
}
```

### Pattern: Adding Aliases via Constructor
**What:** BentoBox's `CompositeCommand` accepts aliases as varargs in the constructor.
**When to use:** Always - this is the standard BentoBox pattern.
**Example:**
```java
// Source: BentoBox CompositeCommand API
// Add "map" as fourth parameter
super(addon, "islandselector", "is", "isgrid", "map");
```

### How Alias Registration Works
The `CompositeCommand` constructor:
1. Stores aliases via `setAliases(new ArrayList<>(Arrays.asList(aliases)))`
2. Registers with BentoBox's CommandsManager: `plugin.getCommandsManager().registerCommand(this)`
3. BentoBox handles Bukkit command map registration automatically

### Anti-Patterns to Avoid
- **Manual Bukkit CommandMap registration:** BentoBox handles this automatically
- **Creating a separate MapCommand class:** Unnecessary duplication; use alias
- **Plugin.yml command entries:** BentoBox addons don't use plugin.yml for commands

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Command alias | Separate command class | Constructor alias parameter | BentoBox handles all registration |
| Tab completion for alias | Custom tab completer | BentoBox CompositeCommand | Inherits automatically |
| Permission for alias | New permission | Existing permission | Alias shares parent command's permission |
| Subcommand routing | Manual argument parsing | BentoBox subcommand system | Already works with all aliases |

**Key insight:** The alias is not a separate command - it's an alternative invocation path to the same command. All subcommands (`/map slots`, `/map admin`, etc.) work automatically.

## Common Pitfalls

### Pitfall 1: Potential Conflict with Other Plugins
**What goes wrong:** Another plugin might already register `/map`
**Why it happens:** Some map-related plugins (rare) or custom plugins might use this command
**How to avoid:** The conflict is handled by Bukkit's priority system. BentoBox-registered commands take precedence over later registrations. If conflict occurs, server admins can use `commands.yml` to redirect.
**Warning signs:** Console warning about command already registered; players reporting `/map` doesn't work

### Pitfall 2: Case Sensitivity
**What goes wrong:** Alias might not work with mixed case
**Why it happens:** Bukkit normalizes commands to lowercase
**How to avoid:** Always use lowercase in alias definition (BentoBox does this automatically)
**Warning signs:** N/A - BentoBox handles this

### Pitfall 3: Forgetting Reload Testing
**What goes wrong:** Alias might not survive server reload
**Why it happens:** Commands need to be re-registered on reload
**How to avoid:** BentoBox handles reload properly - no special action needed
**Warning signs:** Alias works on fresh start but fails after `/bentobox reload`

## Code Examples

Verified pattern from existing codebase:

### Current Implementation (before change)
```java
// Source: src/main/java/world/bentobox/islandselector/commands/IslandSelectorCommand.java
public class IslandSelectorCommand extends CompositeCommand {
    public IslandSelectorCommand(IslandSelector addon) {
        super(addon, "islandselector", "is", "isgrid");
    }
    // ...
}
```

### Required Change (after change)
```java
// Add "map" as the fourth alias
public class IslandSelectorCommand extends CompositeCommand {
    public IslandSelectorCommand(IslandSelector addon) {
        super(addon, "islandselector", "is", "isgrid", "map");
    }
    // ...
}
```

### Verification Commands
```
# All of these should open the same grid GUI:
/islandselector
/is
/isgrid
/map

# All of these should work identically:
/islandselector slots
/map slots

/islandselector admin reload
/map admin reload

/islandselector help
/map help
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual Bukkit command registration | BentoBox CompositeCommand | BentoBox 1.x | Standardized addon command handling |

**Deprecated/outdated:**
- Direct Bukkit CommandExecutor: Still works but not idiomatic for BentoBox addons
- plugin.yml command definitions: Not used by BentoBox addons

## Open Questions

No open questions - this is a well-understood, minimal change.

## Sources

### Primary (HIGH confidence)
- [BentoBox CompositeCommand.java](https://github.com/BentoBoxWorld/BentoBox/blob/master/src/main/java/world/bentobox/bentobox/api/commands/CompositeCommand.java) - Constructor signature and alias handling
- Existing codebase: `IslandSelectorCommand.java` - Current implementation pattern

### Secondary (MEDIUM confidence)
- [BentoBox Creating a Game Mode](https://docs.bentobox.world/en/latest/BentoBox/Creating-a-Game-Mode/) - Command registration patterns
- [Bukkit Command Conflict Handling](https://bukkit.fandom.com/wiki/Commands.yml) - How conflicts are resolved

### Tertiary (LOW confidence)
- Web search: Verified `/map` is not a vanilla Minecraft command
- Web search: Dynmap uses `/dynmap`, not `/map` - low conflict risk

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Existing pattern in codebase, verified with BentoBox source
- Architecture: HIGH - Single line change, well-documented API
- Pitfalls: HIGH - Standard Bukkit command registration, well understood

**Research date:** 2026-01-20
**Valid until:** Indefinite - BentoBox CompositeCommand API is stable
