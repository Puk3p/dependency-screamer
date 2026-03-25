# Dependency Screamer

> IntelliJ IDEA plugin that checks local `pom.xml` dependencies against Nexus Repository and warns when newer versions are available.

## Features

- Parses Maven dependencies from `pom.xml`, including property-based versions (`${jackson.version}`)
- Queries configurable Nexus Repository for latest available versions
- Highlights outdated dependencies with warnings and gutter icons
- Quick-fix to update to latest version
- Configurable: Nexus URL, repositories, authentication, snapshot filtering, timeouts
- Fully async — no UI blocking

## Setup

```bash
git clone https://github.com/Puk3p/dependency-screamer.git
cd dependency-screamer
chmod +x scripts/setup-hooks.sh
./scripts/setup-hooks.sh
```

## Build

```bash
./gradlew buildPlugin
```

## Run (dev)

```bash
./gradlew runIde
```

## Test

```bash
./gradlew test
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for branch naming, commit conventions, and architecture guidelines.

## License

TBD
