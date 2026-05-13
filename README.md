# Device Info – Android Auto

**Author:** [vdt2210](https://github.com/vdt2210)

**Language:** English (this page) | [Tiếng Việt](README.vi.md)

This Android app shows device information on **the phone** and **Android Auto**.

Google usually requires Android Auto apps to be installed via **Google Play**. However, there is still a way to install the APK using a [third-party app](#third-party-apps-you-can-use-to-install-apks).

---

## Installation guide

### Device requirements

- **Android 11 or later** (API 30+)

### Install on phone

1. Download the latest **release APK** from **[GitHub Releases](https://github.com/vdt2210/DeviceInfoAuto/releases)**.
2. Use a [third-party app](#third-party-apps-you-can-use-to-install-apks) and follow their instructions to install the APK.

### Add the app to Android Auto

1. Allow the app to appear on the **car head unit**: **System settings** → find **Android Auto** → **Customize launcher** (or a similar name) → enable **Device Info** if the app is not enabled yet. **If you cannot find the app in the app list, continue with the next steps**.
2. Enable **Developer mode** for Android Auto: **Android Auto** → tap the **Version** row repeatedly until a message appears to enable developer mode → **OK**.
3. In **Android Auto**: top-right menu (three dots) → **Developer settings** → enable **Unknown sources**. If you do not enable this, apps installed outside Play usually do not appear in Android Auto. Go back to **step 1** to add the app to Android Auto.
4. If you have followed the guide but the app still does not appear in Android Auto’s list, the APK may not have been installed correctly; uninstall it and install again following the third-party app’s instructions.

---

## Third-party apps you can use to install APKs

- **[fcaronte/KingInstaller](https://github.com/fcaronte/KingInstaller)**

### Disclaimer and security risks

- When using third-party tools, you are responsible for risks related to **data safety, device security, and system stability**.
- Only download APKs from trusted sources, verify versions carefully before installing, and avoid installing on devices that hold sensitive data if you have not assessed the risk.

---

## For developers

### Project requirements

**Computer**

- **Android Studio** (Ladybug or newer)
- **JDK 17**
- **Android SDK Platform API 36**
- **[Desktop Head Unit (DHU)](https://developer.android.com/training/cars/testing/dhu)**

**Android device**

- **Android 11+**
- **Android Auto**

**Gradle reference (`app/build.gradle.kts`)**

- `compileSdk` → **36**
- `minSdk` → **30** (Android 11+)
- `targetSdk` → **34**
- `namespace` → `com.deviceinfo.auto`
- `applicationId` → `com.vdt2210.deviceinfo`

### Developer guide

1. **Clone** this repository to your machine.
2. Open the project folder in **Android Studio** and **Sync Project with Gradle Files**.
3. Run the **app** configuration on a **physical device** with **Android 11+** (Android Auto is usually not fully testable on a typical emulator).
4. To try the **car UI**: enable **Android Auto** **Developer mode** and **Unknown sources** on the phone; connect **DHU** on the PC if you use it — [Android Auto — DHU](https://developer.android.com/training/cars/testing/dhu).
5. To build a **signed release APK**, configure app signing in Android Studio or Gradle, then run `./gradlew assembleRelease`.

## Project structure

- `MainActivity` – Phone UI (RecyclerView)
- `DeviceInfoCarAppService` – Car app service (IOT)
- `DeviceInfoSession` – Session for the Android Auto screen
- `DeviceInfoScreen` – Car UI (`ListTemplate`)
- `DeviceInfoProvider` – Reads battery, storage, RAM, and system information

## Build from source (optional)

```bash
cd DeviceInfoAuto
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk` (or `./gradlew assembleRelease` if you sign the release build yourself).

## Host validator

The app uses `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` so any Android Auto host can run the app during development and sideload testing. For a production build distributed widely (for example via Google Play), consider narrowing the host validator.
