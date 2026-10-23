# File thuộc phạm vi C01

Danh sách để người dùng review và tự stage/commit. Codex chưa stage/commit và chưa chạy build/test/lint/emulator. Người dùng đã xác nhận đăng nhập Google, hủy đăng nhập, khôi phục phiên và duyệt giao diện mới; bản sửa đăng xuất dùng worker đã được người dùng xác nhận hết treo, Logcat có provider xác nhận thành công. Không tự đánh dấu đạt các nhánh timeout/thử lại hoặc biến thể chưa được xác nhận riêng.

Không bao gồm `.idea/appInsightsSettings.xml`, `.idea/misc.xml` và `.idea/deviceManager.xml` có sẵn; file cuối đã staged trước C01. Không dùng stage tất cả.

| Trạng thái so với HEAD | File |
| --- | --- |
| Sửa | `app/build.gradle.kts` |
| Mới (chưa tracked) | `app/src/main/assets/licenses/bevietnampro-OFL.txt` |
| Mới (chưa tracked) | `app/src/main/assets/licenses/googlesans-OFL.txt` |
| Mới (chưa tracked) | `app/src/main/assets/licenses/googlesans-TRADEMARKS.md` |
| Mới (chưa tracked) | `app/src/main/assets/licenses/plusjakartasans-OFL.txt` |
| Mới (chưa tracked) | `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/AuthRepository.java` |
| Sửa | `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/GrandMaAssistApp.java` |
| Sửa | `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/MainActivity.java` |
| Mới (chưa tracked) | `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/viewmodel/AuthViewModel.java` |
| Mới (chưa tracked) | `app/src/main/res/color/button_primary_text.xml` |
| Mới (chưa tracked) | `app/src/main/res/drawable/account_card.xml` |
| Mới (chưa tracked) | `app/src/main/res/drawable/button_google.xml` |
| Mới (chưa tracked) | `app/src/main/res/drawable/button_primary.xml` |
| Mới (chưa tracked) | `app/src/main/res/drawable/button_secondary.xml` |
| Mới (chưa tracked) | `app/src/main/res/drawable/brand_bar.xml` |
| Mới (chưa tracked) | `app/src/main/res/drawable/brand_logo_surface.xml` |
| Sửa | `app/src/main/res/drawable/ic_launcher_background.xml` |
| Sửa | `app/src/main/res/drawable/ic_launcher_foreground.xml` |
| Mới (chưa tracked) | `app/src/main/res/drawable-nodpi/google_g.png` |
| Mới (chưa tracked) | `app/src/main/res/drawable-nodpi/grandma_app_icon.png` |
| Mới (chưa tracked) | `app/src/main/res/drawable-nodpi/grandma_primary_logo.png` |
| Mới (chưa tracked) | `app/src/main/res/drawable-nodpi/grandma_banner.jpg` |
| Mới (chưa tracked) | `app/src/main/res/font/be_vietnam_pro_regular.ttf` |
| Mới (chưa tracked) | `app/src/main/res/font/be_vietnam_pro_semibold.ttf` |
| Mới (chưa tracked) | `app/src/main/res/font/google_sans_medium.xml` |
| Mới (chưa tracked) | `app/src/main/res/font/google_sans_variable.ttf` |
| Mới (chưa tracked) | `app/src/main/res/font/plus_jakarta_sans_bold.xml` |
| Mới (chưa tracked) | `app/src/main/res/font/plus_jakarta_sans_variable.ttf` |
| Sửa | `app/src/main/res/layout/activity_main.xml` |
| Sửa | `app/src/main/res/mipmap-anydpi/ic_launcher.xml` |
| Sửa | `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml` |
| Sửa | `app/src/main/res/values/colors.xml` |
| Mới (chưa tracked) | `app/src/main/res/values/dimens.xml` |
| Sửa | `app/src/main/res/values/strings.xml` |
| Mới (chưa tracked) | `app/src/main/res/values/styles.xml` |
| Sửa | `app/src/main/res/values/themes.xml` |
| Sửa | `app/src/main/res/values-night/themes.xml` |
| Mới (chưa tracked) | `docs/assets/C01-ASSETS.md` |
| Mới (chưa tracked) | `docs/C01-CHECKLIST.md` |
| Mới (chưa tracked) | `docs/C01-FILES.md` |
| Sửa | `docs/SETUP.md` |
| Sửa | `gradle/libs.versions.toml` |
| Sửa | `README.md` |


## Review tĩnh đã thực hiện

- Đọc diff Java/XML/Gradle/tài liệu và từng file mới; rà soát trạng thái hủy, lỗi, xoay, đăng xuất và callback cũ.
- Đọc XML và đối chiếu các tham chiếu tài nguyên cục bộ với file/value nguồn. `default_web_client_id` là resource do Google Services sinh từ JSON, phải được người dùng xác minh sau Sync.
- Đối chiếu chữ ký API bất đồng bộ với source artifact AndroidX Credentials 1.6.0; đối chiếu Google button flow/Firebase với tài liệu chính thức.
- Đọc kích thước/xem PNG gốc và đối chiếu SHA-256 của bản sao logo/icon với nguồn Downloads. Font/giấy phép đã đóng gói.
- Review whitespace trong Git diff; không chạy compiler, lint, test hoặc kiểm thử tự động.

## Chưa xác minh

Các nhánh timeout/thử lại/callback trễ và biến thể chữ lớn/TalkBack/launcher/mạng/vòng đời/lỗi chưa được xác nhận cụ thể vẫn nằm trong [C01-CHECKLIST](C01-CHECKLIST.md). Giao diện đã duyệt và đăng xuất với provider xác nhận đã hết treo; không suy rộng thành đạt mọi ca thử. Rules/Console không được thay đổi.

## Riêng lần chỉnh giao diện theo ảnh mẫu

Chỉ sửa/thêm 11 file sau; danh sách tổng C01 ở trên vẫn gồm các file từ lần triển khai chức năng trước:

- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/values/styles.xml`
- `app/src/main/res/values/dimens.xml`
- `app/src/main/res/drawable/brand_bar.xml` (mới)
- `app/src/main/res/drawable/brand_logo_surface.xml` (mới)
- `app/src/main/res/drawable-nodpi/grandma_banner.jpg` (mới)
- `docs/SETUP.md`
- `docs/C01-CHECKLIST.md`
- `docs/C01-FILES.md`
- `docs/assets/C01-ASSETS.md`
- `README.md`

Không sửa Java, AuthRepository/AuthViewModel/Credential Manager, Gradle, Firebase hoặc .idea trong lần chỉnh giao diện. Giữ nguyên toàn bộ ID UI cũ. Nền/màu, font binary/font XML và drawable nút Google có sẵn được giữ; chỉ cập nhật cách style tiêu đề yêu cầu weight 700. Đã đọc metadata font, XML và diff; không chạy app để xác nhận hiển thị.

## Bản sửa chờ dọn phiên đăng xuất

- `AuthRepository.java`: bản timeout đầu tiên đã được người dùng xác nhận vẫn treo. Bản sửa tiếp theo chuyển API dọn phiên và hủy sang worker tối đa hai luồng, không xếp hàng; timeout/cập nhật state chạy main, giữ chống callback trễ. Logcat `GrandMaAuth` chỉ ghi mốc cố định, không dữ liệu tài khoản hoặc credential. Người dùng đã xác nhận bản worker hết treo, log provider confirmed; không suy ra nhánh timeout/thử lại đã đạt.
- `app/src/main/res/values/strings.xml`: thông báo riêng khi chưa có xác nhận dọn phiên trong thời gian chờ.
- `docs/SETUP.md`, `docs/C01-CHECKLIST.md`, `docs/C01-FILES.md`, `docs/assets/C01-ASSETS.md`, `README.md`: ghi nhận duyệt giao diện, lỗi và các bước kiểm tra bản sửa.

Không đổi bố cục, AuthViewModel, MainActivity, dependency hoặc Firebase. Codex chỉ review tĩnh; người dùng đã chạy và xác nhận bản sửa hết treo với callback provider thành công. Nhánh timeout/thử lại chưa có xác nhận riêng.

## Cập nhật phê duyệt và phần theo dõi bổ sung

Người dùng đã xác nhận C01 ổn và duyệt. Các nhánh khó tái hiện cùng biến thể tiếp cận/thiết bị được gom tại [Danh sách 1 — Các nhánh chưa xác minh riêng](C01-CHECKLIST.md#danh-sách-1--các-nhánh-chưa-xác-minh-riêng-theo-dõi-sau-c01). Đây là các mục theo dõi, không phải lỗi đã xác nhận; TTS thuộc C22.

Người dùng chọn tự commit toàn bộ trạng thái hiện tại sau khi xem diff, thay cho cách chọn riêng file C01. Danh sách file phía trên vẫn có giá trị truy vết; các ghi chú cũ về chọn file phản ánh đề xuất trước quyết định này. Codex không stage, commit hoặc push.
