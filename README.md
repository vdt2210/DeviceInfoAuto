# Device Info - Android Auto

**Author:** [vdt2210](https://github.com/vdt2210)

**Language:** English (this page) | [Tiếng Việt](README.vi.md)

Android app that displays device information on **the phone** (MainActivity) and **Android Auto** (Car App Library): battery, charging status, temperature, voltage, storage, RAM, manufacturer, model, Android version, security patch, and build.

## Requirements

- Android Studio Ladybug or newer (or compatible IDE)
- Android SDK 34
- Min SDK 30 (Android 11+)
- A device or emulator with Android Auto, or the [Android Auto for phone screens](https://support.google.com/androidauto/answer/6345494) app

## Project structure

- `MainActivity` - Phone UI (RecyclerView list of device info)
- `DeviceInfoCarAppService` - Car app service (IOT category)
- `DeviceInfoSession` - Session that creates the main screen for Android Auto
- `DeviceInfoScreen` - Android Auto screen (`ListTemplate` with device info rows)
- `DeviceInfoProvider` - Reads battery, temperature, storage, RAM, and system info from the device

## Displayed information

- **Battery** - Level (%), charging status, voltage (V)
- **Battery temperature** - °C (from `BatteryManager`)
- **Battery health** - Good, Cold, Overheat, etc.
- **Storage** - Free / total internal storage (GB)
- **RAM** - Free / total (GB)
- **Manufacturer** - Device manufacturer
- **Model** - Device model name
- **System** - Android version and API level
- **Security patch** - Android security patch level
- **Build** - Build ID

## How to run

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run on a physical device (Android Auto is not fully supported on emulator) or use **Desktop Head Unit** for testing:
   - Enable Developer options on the phone.
   - In **Android Auto** app settings, enable **Unknown sources** and add your development machine.
   - Install and run [Desktop Head Unit](https://developer.android.com/training/cars/apps/auto#desktop_head_unit) on your PC and connect the phone.

4. On the phone: **Settings - Apps - Android Auto - Customize launcher** and enable **Device Info** so it appears in the Android Auto app list.

**Why the app might not appear (release APK / Customize launcher):** Apps built with the **Android for Cars App Library** are not allowed from "unknown sources" on Android Auto. A debug or sideloaded APK will **not** show in the car's app list. To see the app on a real head unit you must distribute it via **Google Play** (and meet car app policies). For development, use **Desktop Head Unit (DHU)** on your PC with the phone connected via USB; the app can show there when the project is run from Android Studio.

## Build from command line

```bash
cd DeviceInfoAuto
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Testing without a car

- Use **Android Auto for phone screens** on the phone and open the app from the launcher, or
- Use **Desktop Head Unit** on a computer with the phone connected via USB and Android Auto in developer mode.

## Android Auto readiness (checklist)

The app is configured so it **can** be added to Android Auto when distributed through approved channels (e.g. Google Play):

| Requirement                                                                     | Status                                     |
| ------------------------------------------------------------------------------- | ------------------------------------------ |
| `CarAppService` with `androidx.car.app.CarAppService` action                    | Yes - `DeviceInfoCarAppService`            |
| Intent-filter category (e.g. IOT)                                               | Yes - IOT                                  |
| `android:exported="true"` on the service                                        | Yes                                        |
| Meta-data `com.google.android.gms.car.application` - `@xml/automotive_app_desc` | Yes                                        |
| `automotive_app_desc.xml` with `<uses name="template" />`                       | Yes                                        |
| `androidx.car.app.minCarApiLevel` in manifest                                   | Yes - set to `1`                           |
| `HostValidator` (e.g. allow host for testing)                                   | Yes - `ALLOW_ALL_HOSTS_VALIDATOR`          |
| Uses Car App Library templates (e.g. `ListTemplate`)                            | Yes - `ListTemplate` in `DeviceInfoScreen` |

The app will appear on a **real car** only when installed from the Play Store (or other approved source). For **debug builds**, use **Desktop Head Unit (DHU)** to test.

**Display:** The app uses `ListTemplate` so it shows **full screen** by default. **Split view with map** is determined by the head unit / Android Auto (depends on car and version); the user can switch between the app and the map from the car UI.

## Host validator

The app uses `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` so any Android Auto host can run it. For production you may want to restrict this to your own host validator.
