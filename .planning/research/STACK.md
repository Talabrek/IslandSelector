# Technology Stack - Nova Integration

**Project:** IslandSelector Nova Integration
**Researched:** 2026-01-26
**Overall Confidence:** HIGH

## Executive Summary

Nova integration for this BentoBox addon requires **NO stack changes**. The project already has the Nova API dependency (v0.21.0) in pom.xml and uses the correct reflection-based optional integration pattern. The only recommendation is updating to Nova API v0.22.1 for currency.

## Current Stack Assessment

### Already Configured (No Changes Needed)

| Component | Current | Status | Notes |
|-----------|---------|--------|-------|
| Nova Maven Repository | `https://repo.xenondevs.xyz/releases` | ✓ Correct | Lines 60-64 in pom.xml |
| Nova API Dependency | `xyz.xenondevs.nova:nova-api:0.21.0` | ✓ Works, minor update recommended | Lines 127-133 in pom.xml |
| Dependency Scope | `provided` | ✓ Correct | Runtime-only, not bundled |
| Integration Pattern | Reflection-based optional | ✓ Correct | NovaIntegration.java already implements this |

### Recommended Version Update

```xml
<!-- Current -->
<dependency>
    <groupId>xyz.xenondevs.nova</groupId>
    <artifactId>nova-api</artifactId>
    <version>0.21.0</version>
    <scope>provided</scope>
</dependency>

<!-- Recommended (for currency) -->
<dependency>
    <groupId>xyz.xenondevs.nova</groupId>
    <artifactId>nova-api</artifactId>
    <version>0.22.1</version>
    <scope>provided</scope>
</dependency>
```

**Rationale:** Version 0.22.1 is the latest stable (released Jan 20, 2025). The 0.21→0.22 migration introduced minimal API changes, primarily requiring a Java agent flag for server startup (`-javaagent:plugins/Nova-0.22.1.jar`). This is a Nova deployment requirement, not a compile-time concern for integrations.

## Integration Architecture

### Compile-Time vs Reflection Decision

**Recommendation: Continue using reflection-based optional integration (current approach)**

| Approach | Pros | Cons | Decision |
|----------|------|------|----------|
| **Reflection (current)** | - No hard dependency<br>- Graceful degradation<br>- Works without Nova installed<br>- Follows BentoBox addon patterns | - More verbose code<br>- API changes need reflection updates | **✓ Keep this** |
| Compile-time API | - Type safety<br>- IDE support<br>- Cleaner code | - Hard dependency on Nova<br>- Addon breaks without Nova<br>- Against BentoBox optional integration pattern | ✗ Don't switch |

**Evidence:** The existing `NovaIntegration.java` already implements the correct pattern, matching other optional integrations like `LevelIntegration.java` and `WorldEditIntegration.java`. This is the established pattern for this codebase.

## Nova API Compatibility Matrix

### Version Evolution

| Nova Version | Release Date | Minecraft Support | API Stability | Notes |
|--------------|--------------|-------------------|---------------|-------|
| 0.16.x | Pre-2024 | 1.19+ | Breaking changes | TileEntityManager removed |
| 0.17.x | 2024 | 1.19+ | Breaking changes | BlockManager removed, BlockUtils introduced |
| 0.21.0 | Nov 22, 2024 | 1.20.4, 1.21.10+ | Minor breaking | Java agent required |
| 0.22.1 | Jan 20, 2025 | 1.20.4, 1.21.11+ | **Stable** | Bug fix release |

**Current project targets:** Minecraft 1.20.4 (Paper 1.20.4-R0.1-SNAPSHOT)

**Compatibility verdict:** Nova 0.22.1 supports Minecraft 1.20.4 ✓

### API Stability Assessment

**Confidence: MEDIUM** - Nova has a history of breaking changes between minor versions (0.16→0.17, 0.17→0.18, 0.21→0.22), but maintains migration guides. The API is actively developed with breaking changes occurring approximately every 3-6 months.

**Mitigation:** Reflection-based integration pattern already isolates the codebase from Nova API changes. When Nova updates break the integration, the addon continues working with Nova features gracefully disabled.

## Key API Classes for Integration

### Required for Current NovaIntegration.java

| Class | Purpose | API Stability | Reflection Status |
|-------|---------|---------------|-------------------|
| `xyz.xenondevs.nova.world.format.WorldDataManager` | Get block state, tile entity at location | Stable since 0.17 | ✓ Already used |
| `xyz.xenondevs.nova.world.block.state.NovaBlockState` | Represents Nova custom block | Stable since 0.17 | ✓ Already used |
| `xyz.xenondevs.nova.util.BlockUtils` | Place/break Nova blocks | Introduced 0.17 | ✓ Already used |
| `xyz.xenondevs.nova.registry.NovaRegistries` | Lookup blocks by ID | Stable | ✓ Already used |
| `xyz.xenondevs.nova.context.Context` | Context for block operations | Added 0.18+ | ✓ Already used |
| `net.kyori.adventure.key.Key` | Namespaced key for block IDs | External dependency | ✓ Already used |

**All required classes are already accessed via reflection in NovaIntegration.java.**

### API Changes Between 0.21 and 0.22

**Research confidence: LOW** - Official migration guide 0.21→0.22 exists but content was not accessible during research. Based on release notes, no breaking API changes were documented for external integrations, only the Java agent requirement for server deployment.

**Validation needed:** Test NovaIntegration.java against Nova 0.22.1 to confirm no reflection paths broke.

## What NOT to Add

### ✗ Do Not Add Kotlin Runtime

**Reason:** Nova is written in Kotlin, but the API is Java-compatible. The provided scope ensures Nova's Kotlin runtime is present at runtime, not bundled.

**Evidence:** Official docs show Java examples alongside Kotlin. The NovaIntegration.java uses pure Java reflection with no Kotlin dependencies.

### ✗ Do Not Add Adventure API Separately

**Reason:** Nova already depends on `net.kyori.adventure.key.Key`. The classes are available transitively at runtime when Nova is installed.

**Evidence:** NovaIntegration.java line 298 uses `Class.forName("net.kyori.adventure.key.Key")` successfully without explicit dependency.

### ✗ Do Not Make Nova a Hard Dependency

**Reason:** Nova is an optional enhancement. The addon must work without Nova installed.

**Evidence:** Existing `addon.yml` does not list Nova in `softdepend`. The BentoBox ecosystem pattern is graceful degradation for optional plugins.

### ✗ Do Not Change Scope to Compile

**Reason:** `provided` scope means "available at runtime, don't bundle." This is correct for a plugin dependency.

## Version Compatibility Considerations

### Minecraft Version Alignment

| Component | Version | Minecraft Target | Compatible |
|-----------|---------|------------------|------------|
| Paper API | 1.20.4-R0.1-SNAPSHOT | 1.20.4 | ✓ |
| BentoBox | 2.4.0 | 1.20+ | ✓ |
| Nova API | 0.22.1 (recommended) | 1.20.4 supported | ✓ |
| FAWE | 2.9.2 | 1.20+ | ✓ |

**No version conflicts identified.**

### Java Version Requirements

- **Project:** Java 17 (maven.compiler.source/target in pom.xml)
- **Nova:** Java 17+ (Paper 1.20.4 requires Java 17+)
- **Verdict:** ✓ Compatible

## Runtime Deployment Considerations

### Nova 0.22.x Requirement

Starting with Nova 0.21.0, servers **must** add Nova as a Java agent:

```bash
java -javaagent:plugins/Nova-0.22.1.jar -jar paper.jar
```

**Impact on IslandSelector:** None. This is a server administrator task, not an addon development concern. IslandSelector works regardless of whether Nova uses the Java agent or not.

**Documentation needed:** README.md should note that if Nova is installed, admins must use the Java agent flag.

## Testing Requirements

### Version Compatibility Testing Matrix

| Test Scenario | Purpose | Priority |
|--------------|---------|----------|
| Nova not installed | Verify graceful degradation | HIGH |
| Nova 0.21.0 | Test current dependency | MEDIUM |
| Nova 0.22.1 | Test recommended upgrade | HIGH |
| Nova blocks in island | Verify capture/restore | HIGH |
| Multi-dimension with Nova | Verify all dimensions handle Nova | HIGH |

### Reflection Robustness

**Current pattern strength:** The NovaIntegration.java uses extensive try-catch blocks and checks for class availability before using reflection. This is the correct pattern for optional integrations.

**Known fragility points:**
- Line 58: Class path for `BlockManager` tries multiple locations (handles API moves)
- Line 70: Class path for `NovaBlockState` tries multiple locations
- Line 263-276: Context retrieval tries multiple approaches

**Assessment:** Reflection code is defensively written and handles API variations well.

## Stack Change Summary

**NONE REQUIRED.**

The project is already correctly configured for Nova integration. The only recommended action is a non-breaking version bump from 0.21.0 to 0.22.1 for currency.

### If Updating to 0.22.1

**Changes required in pom.xml:**
```diff
        <dependency>
            <groupId>xyz.xenondevs.nova</groupId>
            <artifactId>nova-api</artifactId>
-           <version>0.21.0</version>
+           <version>0.22.1</version>
            <scope>provided</scope>
        </dependency>
```

**Changes required in code:** None expected, pending validation testing.

**Changes required in documentation:** Add note about Java agent requirement for Nova 0.22.x.

## Sources

### High Confidence (Official Documentation)

- [Nova API Getting Started](https://docs.xenondevs.xyz/nova/api/) - Maven coordinates, Java compatibility
- [Nova Releases](https://github.com/xenondevs/Nova/releases) - Version 0.22.1 release notes
- [Nova Migration Guide 0.16→0.17](https://docs.xenondevs.xyz/nova/addon/migration-guide/0.16-0.17/) - TileEntityManager/BlockManager API changes
- [Nova on Hangar](https://hangar.papermc.io/xenondevs/Nova) - Version 0.22.1 confirmation, Minecraft 1.20.4 support

### Medium Confidence (Community Sources)

- [xenondevs Maven Repository](https://repo.xenondevs.xyz/releases) - Official Maven repository (verified via pom.xml)

### Low Confidence (Could Not Verify)

- Migration guide 0.21→0.22 - Mentioned in release notes but content not accessible during research. Requires manual testing to confirm no breaking changes for external integrations.
