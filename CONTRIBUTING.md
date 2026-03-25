# Contributing to Dependency Screamer

## Getting Started

```bash
# Clone the repository
git clone https://github.com/Puk3p/dependency-screamer.git
cd dependency-screamer

# Set up git hooks
chmod +x scripts/setup-hooks.sh
./scripts/setup-hooks.sh
```

## Branch Naming Convention

All branches must follow this pattern:

| Prefix       | Purpose                        | Example                        |
|--------------|--------------------------------|--------------------------------|
| `feat/`      | New feature                    | `feat/nexus-client`            |
| `fix/`       | Bug fix                        | `fix/version-comparison-bug`   |
| `refactor/`  | Code refactoring               | `refactor/extract-parser`      |
| `chore/`     | Maintenance, deps, config      | `chore/update-gradle`          |
| `docs/`      | Documentation only             | `docs/add-readme`              |
| `test/`      | Adding or updating tests       | `test/comparator-edge-cases`   |
| `ci/`        | CI/CD changes                  | `ci/add-lint-step`             |
| `release/`   | Release preparation            | `release/1.0.0`                |
| `hotfix/`    | Urgent production fix          | `hotfix/null-pointer-crash`    |

Protected branches: `main`, `develop`

## Commit Message Convention

We use [Conventional Commits](https://www.conventionalcommits.org/).

```
<type>(<scope>): <description>
```

### Types

| Type         | Description                          |
|--------------|--------------------------------------|
| `feat`       | New feature                          |
| `fix`        | Bug fix                              |
| `docs`       | Documentation changes                |
| `style`      | Formatting, no logic change          |
| `refactor`   | Code restructuring, no feature/fix   |
| `test`       | Adding or updating tests             |
| `chore`      | Build, config, tooling               |
| `ci`         | CI/CD pipeline changes               |
| `perf`       | Performance improvement              |
| `build`      | Build system changes                 |

### Examples

```
feat(parser): add Maven property resolution
fix(client): handle Nexus timeout gracefully
chore: update Gradle to 8.5
test(comparator): add edge cases for SNAPSHOT versions
ci: add plugin verification step
```

## Git Hooks

Hooks are in `.githooks/` and activated via `scripts/setup-hooks.sh`.

| Hook          | What it does                                          |
|---------------|-------------------------------------------------------|
| `pre-commit`  | Compile check, conflict markers, secrets scan         |
| `commit-msg`  | Validates Conventional Commits format                 |
| `pre-push`    | Branch naming, tests, plugin structure verification   |

## Build & Test

```bash
# Build plugin
./gradlew buildPlugin

# Run tests
./gradlew test

# Verify plugin structure
./gradlew verifyPluginStructure

# Run IDE with plugin loaded
./gradlew runIde
```

## Architecture

The project follows **Clean Architecture** with strict **SOLID** principles:

```
src/main/kotlin/com/nexusversionguard/
├── domain/          # Models + Interfaces (ports)
│   ├── model/       # Data classes (no dependencies)
│   └── port/        # Contracts/interfaces
├── application/     # Use cases / orchestration
│   └── service/     # Business logic services
├── infrastructure/  # Concrete implementations
│   ├── parser/      # pom.xml parsing
│   ├── client/      # Nexus HTTP client
│   ├── version/     # Version comparison logic
│   └── settings/    # Persistent state
└── ui/              # IntelliJ UI integration
    ├── toolwindow/  # Tool Window panel, cards, animations
    ├── inspection/  # XML inspections
    ├── annotator/   # Gutter icons
    ├── quickfix/    # Auto-fix actions
    ├── settings/    # Config UI panel
    ├── startup/     # Post-startup activity
    └── icons/       # Custom SVG icon
```

## Code Guidelines

- One responsibility per class
- Depend on abstractions, not implementations
- Keep UI, parsing, and business logic decoupled
- All network calls must be async/background-safe
- No hardcoded values
- Test all business logic
