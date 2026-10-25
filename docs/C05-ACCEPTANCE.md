# Chốt C04/C05 và nền chuyển sang C06

25/10/2026 — người dùng xác nhận **đã duyệt hết tất cả bản hiện tại**, đồng thời yêu cầu kiểm tra commit và chuẩn bị C06. Đây là nghiệm thu theo xác nhận của người dùng, không phải Codex tự chạy lại.

## Commit đã đối chiếu

| Commit | Nội dung đã kiểm tra |
|---|---|
| `2552a04` | Mã C05/QR/hồ sơ/xét duyệt, điều hướng vai trò và Hồ sơ thống nhất |
| `a63734e` | Fix_Gradle: khôi phục file Gradle gốc, thêm ML Kit đúng module app, 19 chuỗi và hai style còn thiếu; báo cáo sửa sau pull |
| `11fcdeb` | Update misc.xml: khai báo XML và JDK jbr-21 trong cấu hình Android Studio |

HEAD/main/origin/main và main trên GitHub cùng `11fcdebd6db0b70cab9d3289121b2a7f352edd7d` tại thời điểm kiểm tra bằng `git ls-remote`. Working tree sạch trước khi Codex cập nhật các tài liệu nghiệm thu/chuẩn bị C06 lần này. Không fetch/pull/reset hoặc sửa code để thực hiện đối chiếu.

Root Gradle đúng cấu trúc apply false/Google Services 4.5.0, ML Kit 17.3.0 nằm trong app/build.gradle.kts, resource của Hồ sơ thống nhất đã nằm trong commit. `.idea/misc.xml` hiện có thay đổi của thành viên được commit, không tự sửa lại JDK.

## Kết quả nghiệm thu

Người dùng đã duyệt chức năng C04/C05 hiện có: điều hướng theo vai trò/Admin, Hồ sơ thống nhất, QR/ảnh/gửi, xem/xét duyệt và các đợt sửa thao tác quyết định; xác nhận mới cũng chốt phần sửa sau pull và giao diện Hồ sơ cuối. C05-D01–D06 đã được ghi nhận từ xác nhận trước; C04-N01–N07/P01–P06 ghi nhận theo chốt tổng thể hiện tại.

Các trạng thái chờ nghiệm thu bản ứng dụng/Hồ sơ/sửa Gradle trong lịch sử được thay thế bằng bản chốt này. Không tự thêm thông tin thiết bị/API/log hoặc số lần thử chưa được người dùng cung cấp. Ô ca kỹ thuật chưa có bằng chứng riêng trong checklist vẫn phục vụ theo dõi, không làm sai lệch việc người dùng đã chốt bản ứng dụng.

## Các mục tiếp tục theo dõi

C02-F01 (ngôn ngữ/tên ứng dụng trong SMS), timeout/callback trễ/cạnh tranh/biến thể TalkBack/thiết bị/quyền và kiểm thử âm Rules chưa tái hiện được giữ ở báo cáo/checklist tương ứng. Không tự ghi đã giải quyết chỉ từ nghiệm thu tổng thể. TTS thuộc C22; các mục nghiệp vụ Sắp có giữ kế hoạch đã chốt.

Codex chỉ kiểm tra Git/cấu hình/nguồn và cập nhật tài liệu; không Sync/build/test/lint/app/emulator/SMS, sửa Firebase Console/deploy hoặc stage/commit/push. Người dùng tiếp tục tự kiểm tra và tự commit từng luồng.

## Chuyển sang C06

Phạm vi là nháp tin tuyển cục bộ và xem trước; gửi/duyệt tin thuộc C07. Người dùng chốt nháp đã lưu giữ cho đúng UID đăng nhập lại, tài khoản khác không thấy. Kế hoạch và thứ tự trong [C06-PLAN](C06-PLAN.md). Chưa triển khai code C06 trong đợt chuẩn bị này.
