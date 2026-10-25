# Sửa cấu hình sau khi nhận commit C05

25/10/2026 — commit đã kiểm tra: `2552a04a1b2d09b7504146093be32ec75e5d7fbc` (`SCAN_QR_và_Luồng_Duyệt_Hồ_Sơ`). HEAD/main/origin/main tại máy và nhánh main trên GitHub đều trùng SHA này tại thời điểm kiểm tra bằng `git ls-remote`. Working tree trước sửa chỉ có `.idea/misc.xml` thay đổi; giữ nguyên file đó.

## Kết quả đối chiếu

Người dùng báo sau pull lỗi tại `build.gradle.kts` dòng 1, chưa cung cấp phần thông báo nguyên nhân đầy đủ. Đối chiếu nội dung commit với nền `fce2c26` xác định các lỗi cấu hình cụ thể:

| File | Trong commit nhận về | Đã sửa ở working tree |
|---|---|---|
| `build.gradle.kts` ở gốc dự án | Bị thay bằng nội dung module app: áp plugin Android/Google Services trực tiếp, có android/dependencies; mất khai báo phiên bản Google Services và apply false | Khôi phục cấu trúc khai báo plugin dùng chung, Android apply false, Google Services version 4.5.0 apply false đúng nền đã chạy |
| `app/build.gradle.kts` | Chưa có thư viện QR, trong khi khai báo ML Kit nằm nhầm trong file gốc | Thêm barcode-scanning 17.3.0 đúng module app |
| `app/src/main/res/values/profile_strings.xml` | Chưa có 19 chuỗi mà ProfilePanel/layout Hồ sơ thống nhất đã tham chiếu | Khôi phục 19 chuỗi tiêu đề/trạng thái/nút |
| `app/src/main/res/values/profile_styles.xml` | Chưa có hai style ProfileSection/ProfileSectionHeading đã được layout dùng | Khôi phục hai style kế thừa nền thẻ/heading hiện có |
| `README.md` | Chưa có cập nhật phạm vi/bàn giao C05 và Hồ sơ thống nhất tương ứng các báo cáo đã có | Cập nhật mô tả hiện hành và liên kết báo cáo |

Các lớp Java, layout điều hướng/Hồ sơ/C05, Rules và báo cáo C05 hiện có trong commit. Kết quả này không khẳng định mọi byte của lần chuyển máy đều đã được đối chiếu với một bản sao lưu riêng; các chênh lệch nêu trên có bằng chứng trực tiếp từ Git/nội dung file và tham chiếu resource.

## Phạm vi bản sửa

Chỉ sửa vị trí/cấu trúc Gradle và khôi phục resource đã có trong bản giao trước. Giữ nguyên AGP, Gradle wrapper, Firebase BoM, Google Services, ML Kit và các phiên bản thư viện. Không sửa Google Services JSON, SHA, Firebase Rules, dữ liệu Console, JDK của Android Studio hoặc nghiệp vụ đã duyệt.

Không đặt nội dung `app/build.gradle.kts` vào `build.gradle.kts` gốc: hai file có cùng tên nhưng khác nhiệm vụ. Khi chuyển bản sửa sang máy khác, giữ đúng đường dẫn tương đối của cả bốn file cấu hình/resource trong bảng; kèm các tài liệu thay đổi nếu bàn giao báo cáo. Không chép đè toàn bộ module lên file gốc chỉ vì cùng tên.

## Xác minh và bàn giao

Codex chỉ rà tĩnh XML, ID/string/style tham chiếu và diff; không chạy Gradle Sync/build/test/lint/app hoặc commit/push/deploy. Vì chưa có log nguyên nhân đầy đủ và chưa Sync, chưa khẳng định đây là lỗi duy nhất của môi trường máy.

Người dùng: Sync Project with Gradle Files rồi Run. Nếu còn lỗi, lấy đoạn từ `What went wrong`/`Caused by` hoặc lỗi đầu tiên trong Sync/Build Output; dòng `Build file ... line: 1` chỉ là vị trí báo lỗi. Bản sửa này không cần Publish lại Firebase Rules.

Các ca quyết định C05-D01–D06 đã được người dùng duyệt trước đó vẫn được ghi nhận. Phần Hồ sơ thống nhất C04-P01–P06 và kiểm tra bản sửa sau pull cần người dùng xác nhận riêng.
