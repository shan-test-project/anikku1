---
name: Android Build Environment
description: Replit container resets wipe the Android SDK installation; OpenJDK 17 is required (not GraalVM)
---

**Rule:** The Android SDK at `/home/runner/android-sdk` is wiped on every Replit container reset. Before triggering `Build Debug APK`, always verify `ls /home/runner/android-sdk/platforms/` exists and reinstall if missing.

**Why:** Replit containers are ephemeral. The SDK is not part of the committed project.

**How to apply:** Run these commands before building after any container reset:
1. `mkdir -p /home/runner/android-sdk/cmdline-tools && curl -s -o /tmp/cmdtools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" && unzip -q /tmp/cmdtools.zip -d /home/runner/android-sdk/cmdline-tools && mv /home/runner/android-sdk/cmdline-tools/cmdline-tools /home/runner/android-sdk/cmdline-tools/latest`
2. `export ANDROID_HOME=/home/runner/android-sdk && export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH && yes | sdkmanager --licenses > /dev/null && yes | sdkmanager --install "platform-tools" "platforms;android-35" "build-tools;35.0.1"`

**JDK:** Must use OpenJDK 17 (set via `org.gradle.java.home` in `gradle.properties`). GraalVM CE 22.3 is the system default but incompatible with AGP's jlink transform.

**Heap:** `org.gradle.jvmargs` must be at least `-Xmx3g` — 1500m causes Java heap space OOM during `packageDebug`. Already set to 3g in gradle.properties.

**Workflow:** Use a Replit console workflow (not bash) to run the build — it can run >2 minutes without timing out. Name it "Build Debug APK".

**APK output:** Split by ABI. Use `app-arm64-v8a-debug.apk` (≈110MB) for most modern phones. Universal is ~290MB. To deliver, compress with `gzip -k` since `.apk` can't be presented directly — present the `.gz` file.
