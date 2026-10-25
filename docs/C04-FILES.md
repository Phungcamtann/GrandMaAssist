# File thuộc phạm vi C04

25/10/2026 — **người dùng đã duyệt toàn bộ bản hiện tại**, gồm điều hướng/Hồ sơ thống nhất và sửa sau pull; nền local/GitHub hiện ở `11fcdeb`. Xem [C05-ACCEPTANCE](C05-ACCEPTANCE.md). Các mục bàn giao ban đầu và trạng thái chờ phía dưới được giữ làm lịch sử, không thay thế chốt mới.
Nền: commit 211730a (C03), working tree sạch trước khi triển khai. C01/C02/C03 đã được người dùng duyệt.
Lần này người dùng đính kèm yêu cầu triển khai trực tiếp; Codex thực hiện trong repo, không chuyển sang C05.

## File mới
- app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/domain/model/ProfileDraft.java
- app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/ProfileRepository.java
- app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/viewmodel/ProfileViewModel.java
- app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/profile/ProfilePanel.java
- app/src/main/res/layout/profile_content.xml
- app/src/main/res/layout/profile_spinner_item.xml
- app/src/main/res/values/profile_strings.xml
- app/src/main/res/values/profile_styles.xml
- app/src/main/res/color/role_text.xml
- app/src/main/res/drawable/role_card.xml
- app/src/main/res/drawable/ic_role_work.xml
- app/src/main/res/drawable/ic_role_hire.xml
- firebase/areas-demo.json
- docs/C04-FILES.md
- docs/C04-CHECKLIST.md
- docs/C04-CONSOLE-SCHEMA.md

## File sửa
- app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/MainActivity.java: nối panel/ViewModel hồ sơ, giữ Auth và picker C01–C03.
- app/src/main/res/layout/activity_main.xml: thêm include hồ sơ và đường quay lại từ tài khoản.
- firebase/firestore.rules: User/profile/danh mục/Admin bootstrap; mặc định từ chối ngoài phạm vi.
- docs/SETUP.md và README.md: trạng thái C04 và liên kết bàn giao.

Không đổi dependencies, Firebase JSON, Storage Rules, asset/logo/font đã duyệt hoặc .idea.
Không sửa AuthRepository/AccountMethodsFlow/PhoneSignInFlow/AuthViewModel và thuật toán Auth C01–C03.

## Hành vi và thiết kế
- Chưa có User trên server: chọn hai role một cột, mặc định chưa chọn, có xác nhận vai trò cố định.
- Tạo User + đúng profile nguyên tử trong transaction. Retry đọc role đã có, không ghi đè role.
- User/profile được đọc bằng transaction để có cùng snapshot version; không coi lỗi đọc là tài liệu không tồn tại.
- Lưu và sync contact đều transaction online. Save kiểm tra version đã đọc, không ghi đè im lặng chỉnh sửa từ thiết bị khác.
- Form giữ trong ViewModel; không ghi profile vào SQLite/preferences/saved state. C04 không thêm khôi phục form bền qua process chết.
- ProfilePanel thuộc Activity, không đưa Activity vào repository/ViewModel.
- Tên hiển thị riêng với họ tên pháp lý. SEEKER chỉ có lời giải thích danh tính chưa xét; không có field ngày sinh tự nhập/camera/QR/giới tính/sức khỏe.
- EMPLOYER có loại cá nhân/tổ chức, tên tổ chức, giới thiệu/khu vực. Cả hai được lưu nháp chưa đủ nghề nghiệp.
- Dùng logo/màu/font sẵn có; hai icon line nội bộ cho hai thẻ, không vẽ lại logo. Không banner/giọng đọc/taskbar giả trên hồ sơ.
- Google chưa có phone vẫn chọn role/lưu nháp. Thêm/đổi số nối C03 thật.
- Token phone_number được force refresh và so sánh FirebaseUser trước ghi; Rules kiểm tra độc lập.
- Form sạch: phát hiện Auth đổi số thì sync User rồi đọc lại. Form có nháp: giữ nháp; Save đối chiếu phone cùng transaction hoặc người dùng bỏ nháp để sync riêng.
- Auth đã đổi và Firestore lỗi: số Auth không bị rollback; UI báo chưa đồng bộ, retry đọc/sync, không gửi lại OTP.
- Sau Save đã được server xác nhận nhưng đọc lại lỗi: yêu cầu đọc lại dữ liệu, không lặp Auth.
- Các nút tài khoản/đăng xuất được chặn khi ghi profile đang xử lý; Back giải thích chờ server. Process chết vẫn cần đọc lại, không giả rollback server.
- Admin có adminAccess enabled + User ADMIN do Console cấp: không chọn role thường, chỉ xem thông tin quản trị/tài khoản. Không có quyền Admin toàn cục trong client Rules.

## Review tĩnh và giới hạn
Đã đọc schema v3, source Auth/layout/style và báo cáo cũ; tham khảo tài liệu chính thức Firestore transactions/rules-fields.
Đã đọc lại diff và nguồn mới, rà transaction/profile version/contact/role; phân tích XML và đối chiếu các tên string C04 bằng đọc file; không thiếu string C04 trong lượt rà.
Đã dùng git diff --check để xem whitespace. Đây là review tĩnh, không phải build/test/kiểm thử Rules.
Không chạy Gradle, compiler, lint, emulator, unit test, test Rules, SMS hoặc deploy. Chưa có kết quả runtime/UI/Firestore/Rules/cạnh tranh/hồi quy.
Không stage/commit/push/merge. Người dùng tự kiểm tra theo C04-CHECKLIST trước khi chốt.

## Thiết lập bắt buộc trước thử
1. Publish toàn bộ firebase/firestore.rules đúng project.
2. Seed ba area DEMO theo C04-CONSOLE-SCHEMA; không phải dữ liệu hành chính thật.
3. Android Studio Sync/Run và kiểm tra hai UID thử, giữ role cố định.
4. Nếu cần Admin, dùng tài khoản thử riêng và bootstrap theo hướng dẫn; không tự chuyển người dùng có nghiệp vụ.
5. Rules dùng getAfter/allowlist/version; chỉ sau kiểm tra quyền thực tế mới có thể khẳng định các ca quyền đạt.

C05 sẽ bổ sung identityProfileVersion/họ tên-ngày sinh đọc QR, trạng thái xét duyệt và các quyền tương ứng cùng schema/Rules. Không coi metadata NOT_SUBMITTED hay tên hiển thị là quyền ứng tuyển.
C02-F01 và các ca chưa xác minh trước đây giữ nguyên.

## Chỉnh giao diện và danh mục khu vực — 25/10/2026

Người dùng xác nhận vào được chọn vai trò và chỉ đi tiếp sau khi chọn rồi bấm Tiếp tục. Chưa nghiệm thu các phần khác C04.

Ảnh Console: areas/DEMO_AREA_01 có tên field “Khu vực A (DEMO)” = true thay vì hai field name (string) và active (boolean). Nguyên nhân dữ liệu sai schema đã có bằng chứng ở document này; các document B/C cần người dùng đối chiếu tương tự. Xem hướng dẫn bổ sung trong C04-CONSOLE-SCHEMA.md. Query và Rules giữ nguyên; không dùng fallback field tùy ý hoặc mở quyền để che lỗi seed.

Thay đổi lần này:
- profile_strings.xml: rút gọn nhãn/lỗi/thông báo, bỏ thuật ngữ Firebase/token/Rules khỏi giao diện người dùng.
- profile_content.xml: bỏ heading danh tính thừa; hint kỹ năng ngắn; không hiện giải thích độ đầy đủ hồ sơ thường trực.
- ProfilePanel.java: chỉ hiện Tải lại hồ sơ khi cần phục hồi; chỉ hiện Tải lại khu vực khi trống/lỗi; ẩn thông báo Auth thường lệ trên form; không lặp đoạn đồng bộ khi contact khớp; tên tổ chức chỉ hiện khi chọn tổ chức.
- ProfileViewModel.java: quản lý trạng thái đang tải khu vực và sequence của từng yêu cầu, bỏ kết quả cũ; lỗi tải không giữ danh sách cũ như dữ liệu mới.
- ProfileRepository.java: vẫn query active == true và đọc name; document có active đúng nhưng name thiếu/sai kiểu báo lỗi schema thay vì bỏ qua im lặng.
- docs/C04-CONSOLE-SCHEMA.md, C04-CHECKLIST.md, C04-FILES.md và SETUP.md: bổ sung kết quả người dùng và cách sửa seed.

Chỉ review tĩnh, không build/test/lint/emulator/SMS/Console/stage/commit/push.
Người dùng: sửa name/active trên cả ba document → Sync/Run bản mới → Tải lại khu vực → chọn và lưu → mở lại để đối chiếu. Kiểm tra cả SEEKER/EMPLOYER, lỗi danh mục và giao diện font lớn. Các kết quả này đang chờ người dùng xác nhận.

## Xác nhận nghiệm thu C04 — 25/10/2026

**Trạng thái hiện hành: C04 đã được người dùng kiểm tra và phê duyệt; phần commit do người dùng thực hiện.**
Người dùng xác nhận đã sửa dữ liệu khu vực và thử lại các yêu cầu trong checklist, tất cả hoạt động ổn. Các mục C04-01 đến C04-23 và C04-R01 đến C04-R08 được ghi nhận đạt theo xác nhận tổng thể này. Không có thêm thông tin thiết bị/API hoặc bằng chứng từng ca được cung cấp; không tự bổ sung kết quả chi tiết hay khẳng định Codex đã kiểm thử độc lập.
Xác nhận này thay thế các trạng thái chờ kiểm tra/chưa nghiệm thu C04 trong phần lịch sử phía trên. Lỗi danh mục khu vực và đợt rút gọn giao diện đã được người dùng kiểm tra lại và duyệt.
Codex chỉ cập nhật báo cáo; không chạy build/test/lint/emulator/SMS, không sửa Console/deploy hoặc stage/commit/push. C02-F01 và các mục theo dõi từ luồng trước vẫn giữ nguyên. CCCD/QR thuộc C05; giọng đọc thuộc C22, chưa được tính là đã triển khai trong C04.

## Bổ sung trang chính/điều hướng C04 và cửa vào Admin C05 — 25/10/2026

Theo ảnh và yêu cầu mới, đã bổ sung trang chính riêng theo SEEKER/EMPLOYER/ADMIN, Tài khoản người thuê/Admin, topbar xanh logo + vai trò một dòng, thanh điều hướng dùng chung với C05. Sau chọn vai trò hoặc khôi phục phiên, tài khoản có vai trò vào trang chính thay vì form hồ sơ. Admin vào Tổng quan → Kiểm tra hồ sơ hoặc tab Xử lý; chưa đủ grant/User thì báo rõ, không tự cấp quyền.

Người dùng đã chốt giữ mục chưa triển khai tại vị trí mẫu, khóa và ghi Sắp có. Thẻ/tabs việc làm, tin tuyển, ca làm, trợ giúp, báo cáo/tranh chấp/quá hạn/thống kê hiện chưa mở nghiệp vụ. Không dùng số lượng DEMO. Các form nghề nghiệp/liên kết phương thức và bảo vệ nháp được nối lại vào điều hướng mới.

File mới: hai lớp `ui/navigation/RoleNavigationBar.java`, `RoleHomePanel.java`; bốn layout `role_topbar`, `role_navigation`, `role_home_content`, `role_account_content`; strings/styles/drawables `navigation_*` và `ic_nav_*`. Sửa MainActivity, ProfilePanel, ProfileViewModel, ProfileRepository, activity_main/profile_content; phía C05 sửa VerificationActivity/ViewModel/layout. Danh sách và giới hạn đầy đủ trong [C04-C05-NAVIGATION](C04-C05-NAVIGATION.md).

Không đổi Rules/schema/cấu hình Firebase cho đợt này. Đã rà tĩnh, chưa chạy build/test/app hoặc commit/push. Các ca C04-N01–N07 trong C04-CHECKLIST đang chờ người dùng; phê duyệt C04 cũ không tự phê duyệt bổ sung này.

## Hồ sơ thống nhất theo đề xuất người dùng — 25/10/2026

Người dùng đã kiểm tra và duyệt các ca bản sửa quyết định C05; tiếp tục đề xuất gộp lối vào Hồ sơ/xác minh. Đã phân vùng form Thông tin hồ sơ/Công việc/Số điện thoại, giữ Lưu và các chức năng hiện có. Khối Xác minh có một dòng trạng thái server và một nút theo trạng thái. Trang chủ/Tài khoản người dùng bỏ nút Hồ sơ xác minh riêng; Admin giữ lối Kiểm tra hồ sơ.

ProfileRepository.Snapshot đọc thêm trạng thái xác minh, chỉ hiện đã xác minh khi đúng role/version/pointer; ProfileViewModel refresh khi mở/foreground, giữ nháp và kiểm callback UID/request. ProfilePanel nối nút động; C05 Activity/ViewModel mở thẳng request hiện hành từ cờ boolean, Quay lại về Hồ sơ. Người dùng đã duyệt vẫn có thể chủ động Cập nhật giấy tờ bên trong màn xem, không tự đưa vào gửi mới. Không đưa giấy tờ riêng lên trang Hồ sơ thường.

Sửa MainActivity, ProfileRepository/ProfileViewModel/ProfilePanel/RoleHomePanel; profile_content/role_home_content/role_account_content, profile strings/styles; VerificationActivity/ViewModel, activity_verification và verification_strings. Báo cáo và bảng trạng thái trong [C04-C05-NAVIGATION](C04-C05-NAVIGATION.md). Không đổi Rules/schema/Console/QR/upload/quyết định; không build/test/app/stage/commit/push. C04-P01–P06 đang chờ người dùng xác minh bản UI mới.
