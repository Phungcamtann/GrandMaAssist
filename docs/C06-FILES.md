# C06 — nháp tin tuyển và xem trước

25/10/2026 — **người dùng đã cập nhật, kiểm tra ở các vai trò và duyệt C06**. Xem [C06-ACCEPTANCE](C06-ACCEPTANCE.md) để đối chiếu địa chỉ chính xác theo v3 và giới hạn của xác nhận nghiệm thu. Codex không chạy build/test. Nền triển khai `11fcdeb`; tài liệu nghiệm thu C04/C05 và kế hoạch C06 đã có thay đổi chưa commit trước khi code, được giữ nguyên.

## Kết quả

- EMPLOYER có nút Soạn tin tuyển và tab Tin của tôi hoạt động: danh sách SQLite thật, tạo/mở/sửa/lưu/xóa nháp. SEEKER vẫn có các mục việc Sắp có; Admin giữ luồng duyệt hồ sơ C05.
- Form chia bốn vùng: công việc; khu vực/lịch; thể lực; tiền/hạn nhận. Lưu nháp đang soạn dở, báo đã lưu chỉ sau khi SQLite hoàn tất. Xem trước mới kiểm đầy đủ; lỗi sát trường và cuộn tới lỗi đầu.
- Xem trước là nội dung đang soạn, có nhãn Bản nháp · Chưa gửi duyệt, chia ba thẻ. Quay lại sửa giữ dữ liệu. Không có nút gửi/đã đăng giả; không ghi job lên Firestore.
- SQLite lưu theo ownerUid + UUID, revision CAS cho sửa/xóa; DB dùng executor dùng chung ngoài UI. Save lặp không tạo ID khác; lỗi SQLite không báo thành công. Transaction giữ bản lưu cũ nếu ghi thất bại.
- Nháp đã lưu giữ sau logout đúng lựa chọn người dùng; UID khác không thấy, đúng UID trở lại thấy nháp. Form chưa lưu giữ qua xoay bằng ViewModel, không giữ qua process death và không tự save khi render/đóng app.
- Xác nhận EMPLOYER/ACTIVE online lần đầu; ghi marker local để mở nháp offline nhanh ở các lần sau. Có đường Nháp tin trên máy trên Main khi đang tải role/lỗi mạng và đã có marker đúng UID. Dữ liệu local không cấp quyền gửi; khi kiểm tra online trả lỗi quyền/role thì không cho soạn.
- Đọc danh mục active từ server, snapshot SQLite cập nhật nguyên tử, offline dùng cache có nhãn. Chưa từng tải không bịa danh mục. Mục inactive/mất sau refresh không tự thay ID, xem trước cần chọn lại.
- Theo schema v3: ca MORNING/AFTERNOON/EVENING/FLEXIBLE; đơn vị HOUR/SESSION/DAY; ba loại thể lực và lựa chọn chủ động không có. Tiền lưu ô đang gõ và khi xem trước parse số nguyên Long dương, không Double; hạn tương lai dùng giờ máy để hỗ trợ form, C07 kiểm giờ server.
- Lưu/bỏ/tiếp tục khi rời form chưa lưu; route qua Main/C05 được nối, không mất bản nháp âm thầm. Callback SQL/Firebase kiểm UID/generation/request; VM không giữ Activity trong callback điều hướng sau lưu.
- Khai báo loại job_drafts.db và sidecar khỏi backup/chuyển máy. Không thay chính sách dữ liệu cũ ngoài DB C06.

## File mới

- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/domain/model/LocalJobDraft.java`: model/payload và validator dùng lại được cho C07.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/local/JobDraftStore.java`: SQLiteOpenHelper/DAO schema v1, khóa chủ tài khoản, CAS và cache danh mục.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/JobDraftRepository.java`: executor DB, xác nhận quyền soạn, đọc server/cache danh mục; không ghi Firestore.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/viewmodel/JobDraftViewModel.java`: danh sách/form/xem trước, nháp chưa lưu, callback và điều hướng sau lưu.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/jobs/JobDraftActivity.java`: binding native Java, date/time picker, lỗi/trạng thái và dialog.
- `app/src/main/res/layout/activity_job_drafts.xml`, `app/src/main/res/values/job_draft_strings.xml`, `job_draft_styles.xml`.
- `firebase/job-categories-demo.json`.
- `docs/C06-FILES.md`, `C06-CHECKLIST.md`, `C06-CONSOLE-SCHEMA.md`.

## File sửa

RoleNavigationBar/RoleHomePanel mở WORK chỉ cho EMPLOYER; ProfilePanel thêm launcher riêng, nối Soạn tin/Tin của tôi/entry offline và xử lý trang trở về; giữ thứ tự đăng ký launcher C05 cũ. ProfileViewModel probe marker UID trên executor khi tải role hoặc lỗi mạng. VerificationActivity nối route WORK sau khi hỏi bỏ nháp nếu cần.

AndroidManifest thêm JobDraftActivity private; profile_content thêm entry local; backup_rules/data_extraction_rules thêm loại trừ DB C06. Firestore Rules chỉ thêm đọc jobCategories active/list limit100; client write false, jobs vẫn default-deny. Cập nhật README/SETUP/C06-PLAN và báo cáo; không sửa root/app Gradle, versions, Google Services JSON, SHA, Auth, Storage Rules hoặc các transaction C05.

## Giới hạn và chi tiết hành vi

Nháp chỉ trên một máy. Thiết bị khác không nhận nháp; xóa dữ liệu/gỡ app có thể mất. Marker đã xác nhận role phục vụ local có thể cũ khi offline; mọi quyền server của C07 phải xác minh lại. Profile/Firebase Auth cũ vẫn giữ cơ chế phiên SDK; không có đăng nhập mới khi offline. Main cho đọc nháp local trong lúc chỉ đang kiểm tra phiên, không bỏ bước xác thực của nghiệp vụ.

Lần đầu hoặc sau khi xóa cache cần online để có danh mục; thiếu danh mục vẫn lưu nội dung dở nhưng chưa xem trước đầy đủ. Cập nhật danh mục và role là các lần đọc, không phải listener liên tục. Mở Main lại có thể hiển thị lỗi mạng của hồ sơ server trong khi nháp local vẫn dùng được qua entry riêng.

Chỉ có migration khởi tạo schema v1; chưa có schema cũ cần chuyển. onUpgrade không DROP mà từ chối phiên bản chưa có migration để giữ dữ liệu, bản tăng DB version sau phải viết migration tường minh. Chưa kiểm thử nâng/hạ phiên bản, lỗi đầy đĩa, callback trễ hay các biến thể thiết bị bằng runtime.

## Bàn giao

Đã rà nguồn/XML/ID/string/style/seed/diff. Không chạy Sync/Gradle/build/test/lint/emulator/app/SMS hoặc kiểm thử Rules. Không Console/deploy/stage/commit/push. Các kết quả hiện tại là rà tĩnh, chưa xác nhận runtime/giao diện C06 đạt.

Người dùng làm [C06-CONSOLE-SCHEMA](C06-CONSOLE-SCHEMA.md) rồi [C06-CHECKLIST](C06-CHECKLIST.md). C04/C05 đã duyệt vẫn được giữ; C02-F01 và các ca kỹ thuật theo dõi cũ không tự được giải quyết bởi C06. C07 gửi/duyệt tin và C22 TTS chưa triển khai.

Căn cứ API: [SQLiteOpenHelper](https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper) cho tạo/migration DB và [Android Auto Backup](https://developer.android.com/identity/data/autobackup) cho cấu hình exclude riêng cloud backup/device transfer. Không dựa vào Firestore disk cache để thay SQLite.
