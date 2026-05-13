# Device Info – Android Auto

**Tác giả:** [vdt2210](https://github.com/vdt2210)

**Ngôn ngữ:** [English](README.md) | Tiếng Việt (trang này)

Ứng dụng Android hiển thị thông tin của thiết bị trên **điện thoại** và **Android Auto**.

Google thường yêu cầu ứng dụng Android Auto phải được cài qua **Google Play**. Tuy nhiên, vẫn có cách cài APK bằng [ứng dụng bên thứ 3](#ứng-dụng-cài-apk-bên-thứ-3-có-thể-tham-khảo).

---

## Hướng dẫn cài đặt

### Yêu cầu thiết bị

- **Android 11 trở lên** (API 30+)

### Cài trên điện thoại

1. Tải **APK bản phát hành** mới nhất tại **[GitHub Releases](https://github.com/vdt2210/DeviceInfoAuto/releases)**.
2. Dùng [ứng dụng bên thứ 3](#ứng-dụng-cài-apk-bên-thứ-3-có-thể-tham-khảo) và làm theo hướng dẫn của họ để cài APK.

### Thêm ứng dụng trên Android Auto

1. Cho phép ứng dụng hiện trên **màn hình xe**: **Cài đặt hệ thống** → tìm **Android Auto** → **Tùy chỉnh trình khởi chạy** (hoặc tên tương tự) → bật **Device Info** nếu ứng dụng chưa được bật. **Trường hợp không tìm thấy ứng dụng trong danh sách ứng dụng, hãy tiếp tục các bước tiếp theo**.
2. Bật **chế độ nhà phát triển** của Android Auto: **Android Auto** → chạm nhiều lần vào dòng **Phiên bản** cho đến khi có thông báo bật chế độ nhà phát triển → **OK**.
3. Trong **Android Auto**: menu góc phải trên cùng (ba chấm) → **Cài đặt nhà phát triển** → bật **Nguồn không xác định**. Không bật thì ứng dụng cài ngoài Play thường không hiện trên Android Auto. Quay lại **bước 1** để thêm ứng dụng vào Android Auto.
4. Trường hợp đã làm theo hướng dẫn mà ứng dụng vẫn không có trong danh sách của Android Auto, APK có thể chưa được cài đúng cách; hãy gỡ và cài lại theo đúng hướng dẫn của ứng dụng bên thứ 3.

---

## Ứng dụng cài APK bên thứ 3 có thể tham khảo

- **[fcaronte/KingInstaller](https://github.com/fcaronte/KingInstaller)**

### Miễn trừ trách nhiệm và rủi ro bảo mật

- Khi dùng các công cụ bên thứ 3, bạn tự chịu trách nhiệm với rủi ro về **an toàn dữ liệu, bảo mật thiết bị và tính ổn định hệ thống**.
- Chỉ tải APK từ nguồn tin cậy, kiểm tra kỹ phiên bản trước khi cài, và tránh cài trên thiết bị chứa dữ liệu nhạy cảm nếu chưa đánh giá rủi ro.

---

## Dành cho nhà phát triển

### Yêu cầu project

**Máy tính**

- **Android Studio** (Ladybug trở lên)
- **JDK 17**
- **Android SDK Platform API 36**
- **[Desktop Head Unit (DHU)](https://developer.android.com/training/cars/testing/dhu)**

**Thiết bị Android**

- **Android 11+**
- **Android Auto**

**Tham chiếu Gradle (`app/build.gradle.kts`)**

- `compileSdk` → **36**
- `minSdk` → **30** (Android 11+)
- `targetSdk` → **34**
- `namespace` → `com.deviceinfo.auto`
- `applicationId` → `com.vdt2210.deviceinfo`

### Hướng dẫn cho nhà phát triển

1. **Clone** repo này về máy.
2. Mở thư mục project trong **Android Studio** và **Sync Project with Gradle Files**.
3. Chạy cấu hình **app** trên **thiết bị thật** có **Android 11+** (Android Auto thường không thử đủ trên emulator thông thường).
4. Để thử **giao diện xe**: bật chế độ nhà phát triển **Android Auto** và **Nguồn không xác định** trên điện thoại; kết nối **DHU** trên PC nếu dùng — [Android Auto — DHU](https://developer.android.com/training/cars/testing/dhu).
5. Để tạo **APK release đã ký**, cấu hình ký app trong Android Studio hoặc Gradle, rồi chạy `./gradlew assembleRelease`.

## Cấu trúc project

- `MainActivity` – Giao diện điện thoại (RecyclerView)
- `DeviceInfoCarAppService` – Car app service (IOT)
- `DeviceInfoSession` – Session màn hình Android Auto
- `DeviceInfoScreen` – UI xe (`ListTemplate`)
- `DeviceInfoProvider` – Đọc pin, bộ nhớ, RAM, thông tin hệ thống

## Build từ mã nguồn (tùy chọn)

```bash
cd DeviceInfoAuto
./gradlew assembleDebug
```

Kết quả: `app/build/outputs/apk/debug/app-debug.apk` (hoặc `./gradlew assembleRelease` nếu tự ký bản release).

## Host validator

Ứng dụng dùng `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` để mọi host Android Auto đều có thể chạy ứng dụng khi phát triển và thử sideload. Bản production phát hành rộng (ví dụ qua Google Play) nên cân nhắc thu hẹp host validator.
