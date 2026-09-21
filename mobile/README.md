# Mobile

Android Compose app in `composeApp`. Gateway from the emulator: `http://10.0.2.2:8080`.

On Ubuntu you can test **Android only**. iOS needs macOS + Xcode (`iosApp/` is not in the tree yet).

Approved layout from [kmp-architect](91d4d619-25da-4b0f-8cdc-1ba7d5e9e33e) for the next extract:

```
mobile/
  shared/      # KMP library: domain, data, Compose UI, SQLDelight
  androidApp/  # thin host
  iosApp/      # Swift host + GMSServices.provideAPIKey (macOS only)
```

Gate iOS targets with `HostManager.hostIsMac`.

Offline: enqueue mutations with `clientOperationId`, then `POST /api/v1/sync/push`. Google Maps via expect/actual — never a static satellite PNG.

## Toolchain

- **Gradle 9.7.1** (repo-root wrapper). The mobile Gradle project is **separate** — use `./gradlew -p mobile`, not `:mobile:…`.
- **JDK 26** for the Gradle daemon and `jvmToolchain` (Foojay resolver in `mobile/settings.gradle.kts` downloads it when missing).
- **Android `jvmTarget` 26** — AGP 9.4 D8 accepted class file major 70. `minSdk 26` is **Android 8.0**, not Java 26; keep it unless a library requires a bump.
- Kotlin **2.4.20** (Compose compiler + serialization plugins match). AGP **9.4.0**, `compileSdk`/`targetSdk` **37**.
- Details: [ADR-002](../docs/adr/002-kmp-mobile.md).

## Ubuntu: SDK, emulator, tests

JDK 26 (Temurin or Foojay). The mobile Gradle project is **separate** — use `./gradlew -p mobile`, not `:mobile:…`.

### 1. KVM (fast emulator)

```bash
sudo apt update
sudo apt install -y qemu-kvm unzip wget curl
ls -l /dev/kvm
sudo usermod -aG kvm "$USER"
# log out and back in (or: newgrp kvm)
```

### 2. Android SDK (command-line tools)

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
mkdir -p "$ANDROID_HOME/cmdline-tools"
cd /tmp
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q commandlinetools-linux-11076708_latest.zip -d /tmp/android-cmdline
mv /tmp/android-cmdline/cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
```

Add to `~/.bashrc`:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator"
```

Then:

```bash
source ~/.bashrc
yes | sdkmanager --licenses
sdkmanager \
  "platform-tools" \
  "emulator" \
  "platforms;android-37" \
  "build-tools;36.0.0" \
  "system-images;android-35;google_apis;x86_64"
```

Point Gradle at the SDK (`local.properties` is gitignored). With `-p mobile`, AGP reads `mobile/local.properties`:

```bash
echo "sdk.dir=$HOME/Android/Sdk" > mobile/local.properties
```

### 3. AVD

```bash
echo "no" | avdmanager create avd \
  -n pf_api35 \
  -k "system-images;android-35;google_apis;x86_64" \
  --device "pixel_6"

emulator -avd pf_api35 -netdelay none -netspeed full
adb devices   # emulator-5554
```

Confirm KVM with `emulator -accel-check` (`KVM … is installed and usable`).

### 4. Unit tests and APK

From the repo root:

```bash
./gradlew -p mobile :composeApp:testDebugUnitTest
./gradlew -p mobile :composeApp:assembleDebug
```

### 5. Run against the local gateway

```bash
cp .env.example .env   # if needed
./scripts/compose-up.sh --build   # profile core: auth, farm, gateway, web
# gateway already up: curl -sS http://localhost:8080/actuator/health

./gradlew -p mobile :composeApp:installDebug
adb shell am start -n com.precisionfarming.mobile/.MainActivity
```

Demo login (debug): `manager@precisionfarming.demo` / `Precision@123`.

Physical phone: `10.0.2.2` is emulator-only. Use the host LAN IP, or change the base URL in `composeApp/src/main/kotlin/com/precisionfarming/mobile/data/Api.kt`.

Maps stay a status screen until `ANDROID_GOOGLE_MAPS_API_KEY` is wired into the Android build.

### Android Studio (optional)

Open the `mobile/` folder (not the monorepo root), install API 37 + an AVD in SDK Manager, run `composeApp`.
