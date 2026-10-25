# C04 — thiết lập Firestore, schema và cấp Admin

25/10/2026. Mã nguồn mới, CHƯA build/chạy/kiểm thử Rules. Người dùng tự làm các bước dưới; Codex không deploy hoặc sửa Console.

## 1. Publish Rules trước khi kiểm tra hồ sơ
1. Firebase Console → chọn project grandmaassist-a1dfa.
2. Firestore Database → database (default) → Rules.
3. Sao lưu nội dung đang dùng nếu có thay đổi riêng, đối chiếu với file trong repo.
4. Sao chép TOÀN BỘ firebase/firestore.rules vào editor, Publish.
5. Nếu Console báo lỗi cú pháp, gửi dòng lỗi đầu tiên; không thay bằng allow read, write: if true.
6. Không đổi Storage Rules; deny-all vẫn đúng cho C04. Không thay google-services.json hoặc Auth provider đang chạy.
7. Rules C04 chưa cho các nghiệp vụ C05 trở đi. Các trường xác minh mới phải bổ sung cùng Rules của luồng đó.

Nếu chưa Publish, sau login app có thể báo không có quyền đọc hồ sơ. Đây không phải tài khoản chưa chọn vai trò; dùng Đọc lại dữ liệu sau khi Publish.

## 2. Nhập danh mục khu vực DEMO
File nguồn: firebase/areas-demo.json. Đây là hướng dẫn seed, không phải định dạng import trực tiếp của Console.
Không có danh mục hành chính được chốt trong repo; các tên dưới là nhãn DEMO có chủ ý, không phải dữ liệu hành chính hiện hành/đầy đủ.

Firestore → Data → Start collection:
- Collection ID: areas.
- Document ID: DEMO_AREA_01. Trường name (string): Khu vực A (DEMO); active (boolean): true.
- Tạo DEMO_AREA_02: name = Khu vực B (DEMO), active = true.
- Tạo DEMO_AREA_03: name = Khu vực C (DEMO), active = true.
Không dùng document ID tự sinh. Không lưu chữ "true" dạng string.
App query active == true. Nếu chưa nhập, app hiện danh mục trống, cho tải lại và vẫn lưu nháp không có khu vực.
Tài khoản người dùng chỉ đọc khu vực đang hoạt động; không được ghi danh mục.
Sau khi thay danh mục, bấm Tải lại danh mục khu vực trong app.

## 3. Schema riêng tư
Tài khoản Auth hiện có chưa có User sẽ được chọn vai trò. Không tự đổi/xóa user Auth để thử.

users/{uid}:
- id: string UID bằng document ID; role: SEEKER hoặc EMPLOYER khi app tạo, cố định.
- accountStatus: ACTIVE khi app tạo, chỉ Console/quy trình quản trị tương lai được sửa.
- displayName: string tối đa 100; UI yêu cầu có tên khi lưu hồ sơ nghề nghiệp, gợi ý tên Auth lúc chọn vai trò.
- phoneE164: string từ claim phone_number, rỗng nếu chưa có.
- phoneVerificationMode: NONE hoặc FIREBASE_AUTH.
- phoneVerifiedAt: null nếu chưa có số; timestamp thời điểm GHI NHẬN ĐỒNG BỘ số hiện hành, không phải thời điểm SMS gốc.
- version: integer, bắt đầu 1, tăng đúng 1 trong mọi transaction cập nhật.
- createdAt, updatedAt: timestamp server.
- SEEKER có retirementStatus = NOT_SUBMITTED; EMPLOYER có identityStatus = NOT_SUBMITTED.
Không tự gán fullName/dateOfBirth, APPROVED hoặc VERIFIED; chưa có giấy tờ.
Không được list users, đổi role/createdAt/trạng thái xét duyệt hoặc xóa rồi tạo lại để đổi role.

jobSeekerProfiles/{uid}:
- userId: UID, areaCode: string (có thể rỗng ở nháp).
- skills: list string, tối đa 8 mục, không trùng, mỗi mục 1–60 ký tự.
- experienceSummary: string tối đa 1000.
- preferredShifts: list không trùng trong MORNING/AFTERNOON/EVENING.
- createdAt, updatedAt: timestamp server.
Chưa có họ tên pháp lý, ngày sinh, QR hoặc ngày tuổi tự khai.

employerProfiles/{uid}:
- userId, areaCode.
- employerType: INDIVIDUAL hoặc ORGANIZATION.
- organizationName: string tối đa 150, rỗng khi cá nhân, có nội dung khi tổ chức.
- introduction: string tối đa 1000.
- createdAt, updatedAt: timestamp server.

User và profile tối thiểu được tạo nguyên tử khi xác nhận vai trò; không có hai profile cho cùng UID.
Profile lấy vai trò từ User qua getAfter. Mọi cập nhật profile phải kèm tăng users.version trong cùng transaction.
Mỗi lần Save so sánh version đã đọc; nếu thiết bị khác đã sửa thì báo xung đột và yêu cầu đọc lại.
Transaction chọn vai trò retry không đổi vai trò đã có: tải lại vai trò được server chốt trước.
Mọi ghi ứng dụng dùng transaction online, không ghi batch/set offline để chờ tự gửi.
Client chỉ get hồ sơ của chính UID; Admin C04 chưa có quyền đọc tùy tiện hồ sơ người khác.

## 4. Số liên hệ
Khi đọc hồ sơ, app refresh ID token và so sánh claim với FirebaseUser.
Khi phát hiện số Auth khác User và form không có thay đổi chưa lưu, app thử transaction sync rồi đọc lại.
Nếu form đang sửa, giữ form; Save sẽ đối chiếu claim và ghi hồ sơ/liên hệ cùng transaction hoặc người dùng có thể bỏ thay đổi để sync riêng.
Rules kiểm tra phoneE164 == request.auth.token.phone_number (hoặc rỗng khi claim không có).
Khi Auth đã đổi nhưng Firestore lỗi, không tự gửi lại OTP, không đổi lại số Auth; báo chưa đồng bộ và yêu cầu đọc lại.
phoneVerifiedAt chỉ đổi khi số đổi; giữ timestamp khi số không đổi.
User riêng không phải bằng chứng độc lập về quyền ứng tuyển: các luồng sau vẫn phải đối chiếu claim hiện hành.
Các giao dịch cũ, retirementStatus, identityStatus không tự đổi trong C04.

## 5. Cấp Admin riêng qua Console
Chỉ người quản trị project thực hiện, dùng tài khoản thử thuộc quyền kiểm soát. Console dùng quyền quản trị và BỎ QUA Security Rules; thao tác Console không chứng minh Rules cho client an toàn.

Chuẩn bị tài khoản Firebase Auth riêng chưa chọn SEEKER/EMPLOYER. Lấy UID riêng tư tại Authentication → Users.
Tạo cả hai tài liệu sau bằng Console, dùng chính UID làm document ID:

adminAccess/{uid}:
- enabled: boolean true.

users/{uid}:
- id: string UID.
- role: string ADMIN.
- accountStatus: string ACTIVE.
- displayName: tên hiển thị quản trị (string).
- createdAt, updatedAt: timestamp thời điểm cấp.

Không cần tạo profile người tìm việc/người thuê cho Admin. Không chuyển tài khoản đã có nghiệp vụ bằng cách xóa profile/đổi role để thử.
Nếu đã bật adminAccess nhưng thiếu User ADMIN, app báo cấu hình chưa đủ thay vì tự tạo role.
Admin C04 chỉ thấy thông tin quản trị, tài khoản/phương thức và đăng xuất. Chưa có dashboard/duyệt hồ sơ.
Client không được tạo/sửa adminAccess hoặc tạo role ADMIN. Admin không có đặc quyền ghi mọi tài liệu.
Để thu hồi, quản trị viên thay enabled thành false; User ADMIN không có grant tương ứng sẽ báo cấu hình cần kiểm tra, không tự chuyển sang vai trò thường.

## 6. Ranh giới
C05: camera/CCCD/QR, họ tên-ngày sinh pháp lý, ảnh và xét duyệt. C04 chỉ có thông báo giải thích trên hồ sơ SEEKER.
C22: giọng đọc/cài đặt. Không có nút nghe hoặc taskbar giả.
Không có dữ liệu hồ sơ riêng trong SQLite/preferences/SavedStateHandle; chỉ bộ nhớ ViewModel qua xoay.
Process bị hủy có thể mất bản sửa chưa lưu; mở lại đọc server. Không hứa khôi phục form bền vững.
Chỉ đọc bộ nhớ Firebase Auth/SDK theo thiết kế; Firestore vẫn cache bộ nhớ.

## Sửa dữ liệu khu vực theo ảnh kiểm tra — 25/10/2026

Người dùng đã mở được chọn vai trò và xác nhận Tiếp tục. Ảnh Console cho thấy document areas/DEMO_AREA_01 đang có field tên “Khu vực A (DEMO)” với giá trị boolean true. Đây KHÔNG phải cấu trúc app đọc.

Trong MỖI document khu vực cần HAI field riêng:
| Field (cột tên) | Type | Value |
| --- | --- | --- |
| name | string | Khu vực A (DEMO) (hoặc B/C tương ứng) |
| active | boolean | true |

Giữ document ID DEMO_AREA_01/02/03. Thêm name và active trước; field nhập nhầm có thể xóa sau khi đối chiếu. Không xóa document hoặc collection.
Query vẫn là areas.whereEqualTo("active", true), Rules vẫn chỉ đọc khu vực active. Tài liệu thiếu active không khớp query; đổi UI hay nới Rules không sửa được dữ liệu này.
Sau lưu Console, bấm Tải lại khu vực trong app. Không cần Publish lại Rules vì đợt sửa này không đổi Rules.
Danh sách tải thành công chưa được xác nhận sau khi sửa seed. Codex không sửa Console hoặc tự chạy app.
