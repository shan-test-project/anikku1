# Anikku Build Instructions

This document explains how to build the Anikku debug APK.

## Prerequisites

- **Java**: GraalVM CE 22.3 (Java 19 / JVM 17 compatible)
- **Android SDK**: API 26–35
  - Install location: `/home/runner/workspace/android-sdk/`
  - Required packages: `platforms;android-35`, `build-tools;35.0.0`
- **Gradle**: 8.14.3 (via wrapper — no separate install needed)

## Quick Build (Local)

```bash
# Set Android SDK path
export ANDROID_HOME=/home/runner/workspace/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

# Build debug APK
./gradlew assembleDebug --no-daemon
```

The output APK is at:
```
app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

## GitHub Actions CI Build (using GITHUB_PERSONAL_ACCESS_TOKEN)

If you want CI to build APKs automatically on every push/PR, create `.github/workflows/build.yml` in your repo:

```yaml
name: Build Debug APK

on:
  push:
    branches: [main, master]
  pull_request:
    branches: [main, master]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '19'
          distribution: 'graalvm'

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3
        with:
          cmdline-tools-version: '12266719'

      - name: Build Debug APK
        run: ./gradlew assembleDebug --no-daemon

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: anikku-debug-apk
          path: app/build/outputs/apk/debug/*.apk
```

### Using GITHUB_PERSONAL_ACCESS_TOKEN in CI

The token `GITHUB_PERSONAL_ACCESS_TOKEN` is used for GitHub API operations (e.g., posting PR comments, triggering reviews, uploading releases). It is **not** needed for the Gradle build itself.

To use it in GitHub Actions:

1. Go to **Settings → Secrets and variables → Actions** in your GitHub repo
2. Add a repository secret named `GITHUB_PERSONAL_ACCESS_TOKEN`
3. Reference it in workflows:

```yaml
env:
  GITHUB_TOKEN: ${{ secrets.GITHUB_PERSONAL_ACCESS_TOKEN }}
```

### Example: Auto-upload APK to GitHub Releases

```yaml
- name: Create Release
  if: github.event_name == 'push' && github.ref == 'refs/heads/main'
  uses: softprops/action-gh-release@v2
  with:
    files: app/build/outputs/apk/debug/*.apk
    tag_name: nightly-${{ github.run_number }}
    body: "Nightly build ${{ github.sha }}"
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_PERSONAL_ACCESS_TOKEN }}
```

## Useful Commands

```bash
# Clean build artifacts
./gradlew clean --no-daemon

# Run unit tests
./gradlew test --no-daemon

# Check for dependency updates
./gradlew dependencyUpdates --no-daemon

# Build release APK (requires signing config)
./gradlew assembleRelease --no-daemon
```

## Project Structure

- `app/` — Main Android application module
- `core/` — Core utilities
- `data/` — Data layer (repositories, SQLDelight schemas)
- `domain/` — Business logic
- `presentation-core/` — Reusable Compose components
- `source-api/` — Extension API definitions
- `i18n-ank/` — Anikku-specific internationalization strings
- `buildSrc/` — Custom Gradle build plugins and config
