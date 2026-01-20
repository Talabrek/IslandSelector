# Technology Stack: BentoBox Addon Project Structure

**Project:** IslandSelector (BentoBox Addon)
**Researched:** 2026-01-20
**Confidence:** HIGH (verified against official BentoBox repositories)

## Target Project Structure

The standard 2025 BentoBox addon follows Maven conventions with specific BentoBox requirements.

### Root Directory Structure

```
IslandSelector/
├── .github/                    # Optional: CI/CD workflows
│   └── workflows/
│       └── build.yml
├── src/
│   └── main/
│       ├── java/
│       │   └── world/bentobox/islandselector/
│       │       └── [Java source files]
│       └── resources/
│           ├── addon.yml       # REQUIRED: BentoBox addon descriptor
│           ├── config.yml      # Default configuration
│           └── locales/
│               └── en-US.yml   # Language files
├── .gitignore                  # REQUIRED: Git ignore rules
├── LICENSE                     # Recommended: License file
├── README.md                   # Recommended: Project documentation
└── pom.xml                     # REQUIRED: Maven build configuration
```

### Files That SHOULD Exist at Root

| File | Required | Purpose |
|------|----------|---------|
| `pom.xml` | YES | Maven project configuration |
| `.gitignore` | YES | Exclude build artifacts, IDE files |
| `README.md` | Recommended | Project documentation |
| `LICENSE` | Recommended | License (EPL-2.0 common for BentoBox) |
| `CLAUDE.md` | Optional | AI assistant context file |

### Files That Should NOT Exist at Root

| Item | Reason |
|------|--------|
| `target/` | Build output (gitignored) |
| `*.jar` | Build artifacts (gitignored) |
| `*.py`, `*.sh` scripts | Development tools (not part of addon) |
| `generations/` | AI generation artifacts (not part of final project) |
| `__pycache__/` | Python bytecode (wrong ecosystem) |
| `nul` | Empty/junk file |
| Backup files (`*_backup.java`) | Should not be tracked |

## Maven Directory Structure

### src/main/java

Standard Maven source directory. Package structure for BentoBox addons:

```
src/main/java/world/bentobox/islandselector/
├── IslandSelector.java         # Main addon class (extends Addon)
├── Settings.java               # Configuration (implements ConfigObject)
├── commands/                   # Command implementations
├── database/                   # BentoBox database models (@Table)
├── events/                     # Custom Bukkit events
├── gui/                        # Inventory GUIs
├── integrations/               # External plugin hooks
├── listeners/                  # Bukkit event listeners
├── managers/                   # Business logic managers
├── models/                     # Data models/POJOs
└── utils/                      # Utility classes
```

### src/main/resources

BentoBox-specific resources:

```
src/main/resources/
├── addon.yml                   # REQUIRED: Addon descriptor
├── config.yml                  # Default configuration (optional)
└── locales/
    └── en-US.yml               # Default locale
```

### src/test/java (Optional)

Test structure mirrors main:

```
src/test/java/world/bentobox/islandselector/
├── [Test classes]
```

## Required Configuration Files

### addon.yml (REQUIRED)

The BentoBox addon descriptor. Mandatory fields:

```yaml
name: IslandSelector
main: world.bentobox.islandselector.IslandSelector
version: ${project.version}
api-version: 1.15.4
authors: [Author Name]
description: Brief description

# Optional
softdepend: Level
permissions:
  addon.permission:
    description: Permission description
    default: true
```

**Key points:**
- Do NOT list `BentoBox` in dependencies (it's implicit)
- Use `${project.version}` for Maven resource filtering
- `api-version` should match minimum BentoBox API version

### pom.xml Structure

Key sections for a BentoBox addon:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>

    <groupId>world.bentobox</groupId>
    <artifactId>IslandSelector</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
        <bentobox.version>2.4.0</bentobox.version>
        <paper.version>1.20.4-R0.1-SNAPSHOT</paper.version>
    </properties>

    <repositories>
        <repository>
            <id>papermc</id>
            <url>https://repo.papermc.io/repository/maven-public/</url>
        </repository>
        <repository>
            <id>codemc-repo</id>
            <url>https://repo.codemc.io/repository/maven-public/</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>io.papermc.paper</groupId>
            <artifactId>paper-api</artifactId>
            <version>${paper.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>world.bentobox</groupId>
            <artifactId>bentobox</artifactId>
            <version>${bentobox.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
            </resource>
        </resources>
        <plugins>
            <!-- compiler, shade, surefire -->
        </plugins>
    </build>
</project>
```

### .gitignore (Standard Maven + BentoBox)

```gitignore
# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties

# Java
*.class
*.jar
*.war
*.log
hs_err_pid*

# IDE - IntelliJ
.idea/
*.iml
*.iws
*.ipr
out/

# IDE - Eclipse
.classpath
.project
.settings/
bin/

# IDE - VS Code
.vscode/

# OS
.DS_Store
Thumbs.db
*~

# BentoBox specific
*.bak
```

## Cleanup Actions Required

Based on the current project state at `generations/island_selector/`, the following cleanup is needed:

### Files to KEEP (move to root)

From `generations/island_selector/`:
- `pom.xml` - Move to root
- `src/` directory - Move to root
- `.gitignore` - Move to root (update as needed)
- `README.md` - Move to root
- `CLAUDE.md` - Move to root

### Files to DELETE (from root)

Current root contains AI generation scaffolding that should be removed:
- `agent.py`
- `client.py`
- `progress.py`
- `prompts.py`
- `security.py`
- `test_security.py`
- `autonomous_agent_demo.py`
- `requirements.txt`
- `prompts/` directory
- `__pycache__/` directory
- `generations/` directory (after extracting useful content)
- `nul` file

### Files to DELETE (from generations/island_selector)

Development artifacts that shouldn't be in final project:
- `app_spec.txt` - Original specification
- `feature_list.json` - Generation metadata
- `init.sh` - Generation script
- `output/` directory
- `*.py` scripts (check_*.py, find_*.py, add_method.py, update_testing.sh)
- `*_backup*.java` files
- `TESTING.md.backup`, `TESTING.md.session40.backup`
- `DEVELOPMENT_NOTES.md` (merge useful content into README if needed)
- `.claude_settings.json`
- Inner `.git/` directory (use outer .git)

## Recommended Stack Versions (2025)

| Technology | Version | Notes |
|------------|---------|-------|
| Java | 17 (minimum) | BentoBox requires Java 17+, Java 21 recommended |
| Paper API | 1.20.4+ | Paper preferred over Spigot for BentoBox |
| BentoBox | 2.4.0+ | Latest stable release |
| Maven | 3.9+ | Build tool |
| JUnit | 5.10+ | Testing |
| Mockito | 5.8+ | Mocking |
| MockBukkit | Latest for MC version | Minecraft plugin testing |

## Final Target Structure

After cleanup, the project root should contain:

```
IslandSelector/
├── .git/                       # Version control
├── .gitignore                  # Git ignore rules
├── .planning/                  # Project planning (optional, can gitignore)
├── CLAUDE.md                   # AI context file (optional)
├── LICENSE                     # EPL-2.0 or other
├── README.md                   # Project documentation
├── pom.xml                     # Maven configuration
└── src/
    ├── main/
    │   ├── java/
    │   │   └── world/bentobox/islandselector/
    │   │       └── [85 Java source files]
    │   └── resources/
    │       ├── addon.yml
    │       ├── config.yml
    │       └── locales/
    │           └── en-US.yml
    └── test/
        └── java/
            └── world/bentobox/islandselector/
                └── [Test files]
```

## Sources

- [BentoBox Official Documentation](https://docs.bentobox.world/en/latest/Tutorials/api/Create-an-addon/) - HIGH confidence
- [BentoBox GitHub Repository](https://github.com/BentoBoxWorld/BentoBox) - HIGH confidence
- [Level Addon Repository](https://github.com/BentoBoxWorld/Level) - HIGH confidence (reference implementation)
- [Warps Addon Repository](https://github.com/BentoBoxWorld/Warps) - HIGH confidence (reference implementation)
- [ExampleAddon Repository](https://github.com/BONNePlayground/ExampleAddon) - MEDIUM confidence (community example)
- [GitHub Maven .gitignore Template](https://github.com/github/gitignore/blob/main/Maven.gitignore) - HIGH confidence
