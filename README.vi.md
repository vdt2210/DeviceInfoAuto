# Device Info – Android Auto

**Tác giả:** [vdt2210](https://github.com/vdt2210)

Ứng dụng Android hiển thị thông tin thiết bị trên **điện thoại** (MainActivity) và **Android Auto** (Car App Library): pin, trạng thái sạc, nhiệt độ, điện áp, bộ nhớ, RAM, hãng, model, phiên bản Android, security patch và build.

## Yêu cầu

- Android Studio Ladybug trở lên (hoặc IDE tương thích)
- Android SDK 34
- Min SDK 30 (Android 11+)
- Thiết bị hoặc emulator có Android Auto, hoặc ứng dụng [Android Auto for phone screens](https://support.google.com/androidauto/answer/6345494)

## Cấu trúc project

- `MainActivity` – Giao diện điện thoại (danh sách RecyclerView)
- `DeviceInfoCarAppService` – Car app service (category IOT)
- `DeviceInfoSession` – Session tạo màn hình chính cho Android Auto
- `DeviceInfoScreen` – Màn hình Android Auto (`ListTemplate` với các dòng thông tin)
- `DeviceInfoProvider` – Đọc pin, nhiệt độ, bộ nhớ, RAM và thông tin hệ thống

## Thông tin hiển thị

- **Pin** – Mức (%), trạng thái sạc, điện áp (V)
- **Nhiệt độ pin** – °C (từ `BatteryManager`)
- **Sức khỏe pin** – Good, Cold, Overheat, v.v.
- **Bộ nhớ** – Trống / tổng bộ nhớ trong (GB)
- **RAM** – Trống / tổng (GB)
- **Hãng** – Nhà sản xuất thiết bị
- **Model** – Tên model thiết bị
- **Hệ thống** – Phiên bản Android và API level
- **Security patch** – Mức security patch của Android
- **Build** – Build ID

## Chạy ứng dụng

1. Mở project trong Android Studio.
2. Sync Gradle.
3. Chạy trên thiết bị thật (Android Auto không hỗ trợ đầy đủ trên emulator) hoặc dùng **Desktop Head Unit** để test:
   - Bật **Tùy chọn nhà phát triển** trên điện thoại.
   - Trong cài đặt app **Android Auto**, bật **Nguồn không xác định** và thêm máy phát triển.
   - Cài và chạy [Desktop Head Unit](https://developer.android.com/training/cars/apps/auto#desktop_head_unit) trên PC, kết nối điện thoại.

4. Trên điện thoại: **Cài đặt → Ứng dụng → Android Auto → Tùy chỉnh launcher** và bật **Device Info** để app xuất hiện trong danh sách Android Auto.

**Vì sao app không hiện (APK release / Tùy chỉnh launcher):** Ứng dụng dùng **Android for Cars App Library** không được phép từ "nguồn không xác định" trên Android Auto. APK debug hoặc sideload **sẽ không** hiện trong danh sách app trên xe. Để thấy app trên head unit thật cần phân phối qua **Google Play** (và đáp ứng chính sách car app). Khi phát triển, dùng **Desktop Head Unit (DHU)** trên PC với điện thoại cắm USB; app có thể hiện khi chạy từ Android Studio.

## Build từ dòng lệnh

```bash
cd DeviceInfoAuto
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Test không cần xe

- Dùng **Android Auto for phone screens** trên điện thoại và mở app từ launcher, hoặc
- Dùng **Desktop Head Unit** trên máy tính, điện thoại cắm USB và bật chế độ nhà phát triển Android Auto.

## Sẵn sàng Android Auto (checklist)

App được cấu hình để **có thể** thêm vào Android Auto khi phân phối qua kênh được duyệt (ví dụ Google Play):

| Yêu cầu | Trạng thái |
|---------|------------|
| `CarAppService` với action `androidx.car.app.CarAppService` | Có – `DeviceInfoCarAppService` |
| Category intent-filter (ví dụ IOT) | Có – IOT |
| `android:exported="true"` trên service | Có |
| Meta-data `com.google.android.gms.car.application` → `@xml/automotive_app_desc` | Có |
| `automotive_app_desc.xml` với `<uses name="template" />` | Có |
| `androidx.car.app.minCarApiLevel` trong manifest | Có – `1` |
| `HostValidator` (ví dụ cho phép host khi test) | Có – `ALLOW_ALL_HOSTS_VALIDATOR` |
| Dùng template Car App Library (ví dụ `ListTemplate`) | Có – `ListTemplate` trong `DeviceInfoScreen` |

App chỉ hiện trên **xe thật** khi cài từ Play Store (hoặc nguồn được duyệt). Với **bản debug**, dùng **Desktop Head Unit (DHU)** để test.

**Hiển thị Android Auto:** App dùng `ListTemplate` nên mặc định **full màn**. Chế độ **chia đôi với bản đồ** do head unit / Android Auto quyết định (tùy xe và phiên bản); người dùng có thể chuyển giữa app và bản đồ từ giao diện xe.

## Host validator

App dùng `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` nên mọi host Android Auto đều chạy được. Trong production nên giới hạn bằng host validator riêng.
