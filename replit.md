# Anikku

A full-featured anime discovery and streaming Android application, forked from Aniyomi (itself a fork of Tachiyomi). Discover and watch anime, cartoons, series, and more on your Android device.

## Project Overview

- **Type**: Native Android application (Kotlin + Jetpack Compose)
- **Min SDK**: Android 8.0 (API 26)
- **Compile SDK**: 35
- **Build System**: Gradle 8.14.3 with Kotlin DSL
- **Language**: Kotlin (JVM 17 target)
- **UI**: Jetpack Compose (Material 3)
- **Database**: SQLDelight

## Build Instructions

This is a native Android app — it **cannot** run in the Replit web preview pane. It must be built into an APK and installed on an Android device or emulator.

### Build a Debug APK

Use the "Build Debug APK" workflow (or run in shell):
```bash
./gradlew assembleDebug --no-daemon
```

The output APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Build a Release APK

```bash
./gradlew assembleRelease --no-daemon
```

Note: Release builds require signing configuration.

### Other Useful Commands

```bash
# Clean build artifacts
./gradlew clean --no-daemon

# Run unit tests
./gradlew test --no-daemon

# Check for dependency updates
./gradlew dependencyUpdates --no-daemon
```

## Project Structure

- `app/` — Main Android application module
- `core/` — Core utilities (common, archive)
- `core-metadata/` — Metadata handling
- `data/` — Data layer (repositories, SQLDelight schemas)
- `domain/` — Business logic and use cases
- `presentation-core/` — Reusable Compose components
- `presentation-widget/` — Android home screen widgets
- `source-api/` — Extension API definitions
- `source-local/` — Local media source
- `i18n*` — Internationalization modules
- `telemetry/` — Crash reporting (Firebase/No-op)
- `buildSrc/` — Custom Gradle build plugins and config

## Environment

- Java: GraalVM CE 22.3 (Java 19 / compatible with JVM 17 target)
- Gradle: 8.14.3 (via wrapper)

## User Preferences

- Keep build commands clean with `--no-daemon` flag for Replit's environment.
