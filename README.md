# Dependency Screamer

> IntelliJ IDEA plugin that checks local `pom.xml` dependencies against a Nexus Repository and warns when newer versions are available.

![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ_IDEA-2024.3+-blue?logo=intellijidea)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple?logo=kotlin)
![License](https://img.shields.io/badge/License-MIT-green)

## Features

- **POM parsing** — extracts Maven dependencies including property-based versions (`${jackson.version}`)
- **Nexus integration** — queries configurable Nexus Repository REST API with pagination support
- **Group filter** — only scan dependencies matching a configurable group prefix (e.g. `com.endava`)
- **Tool Window** — dedicated bottom panel with animated spinner, summary badges, and card-based results
- **Setup wizard** — guided configuration screen that hides after initial setup
- **Clickable Nexus links** — outdated dependencies link directly to the artifact in Nexus browser
- **Code annotations** — gutter icons and warnings on outdated `<version>` tags in `pom.xml`
- **Quick-fix** — one-click update to latest version
- **Secure credentials** — passwords stored via IntelliJ `PasswordSafe`
- **Fully async** — all network calls run on background threads, no UI blocking

## Requirements

- IntelliJ IDEA 2024.3+ (build 243+)
- JDK 17+
- Access to a Nexus Repository Manager 3 instance

## Installation

### From source

```bash
git clone https://github.com/Puk3p/dependency-screamer.git
cd dependency-screamer
chmod +x scripts/setup-hooks.sh
./scripts/setup-hooks.sh
./gradlew buildPlugin
```

The built plugin ZIP is in `build/distributions/`. Install it via **Settings > Plugins > Install Plugin from Disk**.

### Development

```bash
./gradlew runIde
```

This launches a sandbox IntelliJ instance with the plugin loaded.

## Configuration

Open **Settings > Tools > Dependency Screamer** and configure:

| Field | Description | Example |
|---|---|---|
| **Nexus URL** | Base URL of your Nexus instance | `https://nexus.example.com` |
| **Repositories** | Comma-separated repository names | `maven-releases, maven-snapshots` |
| **Username** | Nexus username (optional) | `deploy-user` |
| **Password** | Stored securely via PasswordSafe | `••••••••` |
| **Group filter** | Only scan matching group prefixes (comma-separated) | `com.endava, com.myorg` |
| **Ignore snapshots** | Skip SNAPSHOT versions when finding latest | `true` |
| **Timeout** | HTTP timeout in seconds | `10` |

## Usage

1. Open the **Dependency Screamer** tool window (bottom bar icon)
2. If not configured, follow the setup wizard
3. Click **Scan** to analyze `pom.xml` dependencies
4. Results appear as cards grouped by status:
   - **Outdated** (orange) — newer version available, with link to Nexus
   - **Up to date** (green) — already on latest
   - **Errors** (red) — could not fetch from Nexus
   - **Unresolved** (gray) — property-based version that couldn't be resolved

## Architecture

Clean Architecture with strict layer separation:

```
src/main/kotlin/com/nexusversionguard/
├── domain/            # Models + ports (interfaces)
│   ├── model/         # MavenDependency, NexusConfig, VersionInfo, etc.
│   └── port/          # DependencySource, ArtifactRepositoryClient, etc.
├── application/       # Use cases / orchestration
│   └── service/       # DependencyAnalysisService, ServiceProvider
├── infrastructure/    # Concrete implementations
│   ├── parser/        # PomXmlDependencySource, MavenPropertyResolver
│   ├── client/        # NexusRepositoryClient (HTTP + pagination)
│   ├── version/       # SemanticVersionComparator (Maven artifact)
│   └── settings/      # NexusGuardSettings (PersistentStateComponent)
└── ui/                # IntelliJ platform integration
    ├── toolwindow/    # Tool Window panel with cards + animations
    ├── inspection/    # XML inspection for outdated deps
    ├── annotator/     # Gutter icon annotator
    ├── quickfix/      # Update version quick-fix
    ├── settings/      # Settings UI panel + configurable
    ├── startup/       # Post-startup scan activity
    └── icons/         # Custom SVG icon
```

## Build & Test

```bash
./gradlew compileKotlin       # Compile
./gradlew test                # Unit tests
./gradlew spotlessCheck       # Code formatting check
./gradlew spotlessApply       # Auto-fix formatting
./gradlew spotbugsMain        # Static analysis
./gradlew buildPlugin         # Build distributable ZIP
./gradlew verifyPluginStructure  # Verify plugin descriptor
./gradlew runIde              # Launch sandbox IDE
```

## CI/CD

GitHub Actions pipeline (`.github/workflows/build.yml`) runs on every push/PR to `main`:

1. **Spotless** — code formatting check
2. **Compile** — Kotlin compilation
3. **SpotBugs** — static analysis
4. **Tests** — unit test suite
5. **Build** — plugin ZIP artifact
6. **Verify** — plugin structure validation
7. **Compatibility** — plugin verifier (PRs only)

Build artifacts are uploaded and retained for 14 days.

## Git Hooks

Set up via `scripts/setup-hooks.sh`:

| Hook | Checks |
|---|---|
| **pre-commit** | Merge conflicts, secrets, formatting, compilation, TODO markers |
| **commit-msg** | Conventional Commits format |
| **pre-push** | Branch naming, Spotless, compile, SpotBugs, tests, plugin structure |

## Tech Stack

- **Language**: Kotlin 1.9
- **Build**: Gradle Kotlin DSL + IntelliJ Platform Plugin 2.1
- **Platform**: IntelliJ Platform SDK 2024.3.1
- **HTTP**: Java 17 HttpClient (async)
- **JSON**: Gson
- **Versioning**: Apache Maven Artifact (ComparableVersion)
- **Formatting**: Spotless + ktlint
- **Static analysis**: SpotBugs
- **Testing**: JUnit 4 + Mockito-Kotlin + AssertJ

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for branch naming, commit conventions, and architecture guidelines.

## License

[MIT License](LICENSE) — Copyright (c) 2026 George Lupu
