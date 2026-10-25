# C07 — bàn giao gửi và duyệt tin tuyển

25/10/2026. **Đã triển khai mã nguồn, chờ người dùng cập nhật Console và nghiệm thu.** Nền HEAD `afd7891` — Soạn tin tuyển dụng, trước khi code có thay đổi tài liệu địa chỉ/kế hoạch đã được chốt, được giữ nguyên. Chưa Sync/build/test/lint/app/emulator/SMS/Rules tests; chưa Console/deploy/stage/commit/push.

## Kết quả

- Thêm workAddress vào nháp/form/xem trước và JobPost. Nhập ngay khi soạn, bắt buộc khi xem trước/gửi; chỉ ghi dữ liệu người thuê nhập. Nháp C06 cũ đọc thiếu trường thành rỗng, không xóa DB. Đây là địa chỉ công khai của tin OPEN, tách privateAddress của thỏa thuận và dữ liệu CCCD.
- Gửi duyệt rõ ràng sau xác nhận; lưu SQLite thành công trước gửi, transaction online kiểm quyền/OTP/hồ sơ/danh mục/nội dung. ID tin cố định từ nháp; lastSubmissionId theo baseVersion để retry đối chiếu thay vì tạo trùng. Giữ nháp sau gửi; nếu local khác nội dung đã gửi, báo phần sửa chưa thay tin server.
- Người thuê mở Tin đã gửi từ danh sách nháp: query đúng UID, tải lại/phân trang 20 tin, chi tiết nội dung/trạng thái/lý do thật. PENDING_REVIEW/OPEN chỉ xem; REJECTED có Sửa và gửi lại với đúng ID/baseVersion. Danh sách không tự cập nhật nền; tải lại để nhận kết quả Admin.
- Admin vào thẻ Duyệt tin tuyển hoặc chọn loại ở Xử lý; có liên kết qua lại với kiểm tra hồ sơ C05. Hàng chờ PENDING_REVIEW, chi tiết phân vùng; chấp thuận chỉ xác nhận, từ chối mới cần lý do. Ghi quyết định và history nguyên tử, kiểm version/hạn/quyền. Không tự duyệt tin mình.
- ViewModel giữ tác vụ/form qua xoay, chặn bấm lặp và callback khác UID. Tác vụ C07 timeout UI 45 giây, không coi timeout là rollback server. Back từ chi tiết không lệ thuộc việc tải lại mạng. Lý do chưa gửi có hỏi trước khi bỏ.
- Rules mở đúng jobs/public OPEN, chủ tin/Admin cho tin riêng; history riêng bất biến. Gửi lại tăng version và xóa quyết định hiện hành nhưng giữ sự kiện cũ. Hai index cho query owner và hàng chờ, không đổi Storage/Auth/Gradle.

## File mới

- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/domain/model/JobPost.java`: đọc model server, nội dung địa chỉ, nhãn trạng thái và so sánh nội dung retry.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/JobPostRepository.java`: token proof, query/cursor, get server, submit/decide transaction, history, timeout và lỗi ngắn.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/viewmodel/JobPostViewModel.java`: quyền vào màn, hàng chờ/danh sách/chi tiết, lý do và quyết định.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/jobs/JobPostActivity.java`, `app/src/main/res/layout/activity_job_posts.xml`: màn người thuê/Admin, vùng nội dung, trạng thái/hành động, điều hướng/insets.
- `docs/C07-PLAN.md`, `C07-ADDRESS-CHANGE.md`, `C07-CONSOLE-SCHEMA.md`, `C07-CHECKLIST.md`, báo cáo này.

## File sửa

- LocalJobDraft: hai trường JSON tùy chọn workAddress/serverBaseVersion, copy/validation/bounds. Không thay bảng SQLite hoặc DB version; không có DROP/migration giả.
- JobDraftViewModel/JobDraftActivity và activity_job_drafts.xml/job_draft_strings.xml: địa chỉ, lưu trước gửi, mở tin đã gửi, mở/sửa REJECTED, đường tới tài khoản khi thiếu số/hồ sơ.
- RoleHomePanel/ProfilePanel: thẻ Duyệt tin sống cho Admin, chọn loại xử lý, route về trang chủ/tài khoản/hồ sơ. Giữ thứ tự ActivityResult launcher C05 trước nháp, dùng lại launcher cho màn tin theo vai trò.
- VerificationActivity/activity_verification.xml: lối sang hàng chờ tin từ Admin C05; không đổi QR/upload/transaction duyệt hồ sơ.
- AndroidManifest.xml: Activity tin private exported=false.
- firebase/firestore.rules và firestore.indexes.json: jobs/history và hai index. Mọi Rules C04–C06/Storage được giữ.
- README, SETUP, C06-ACCEPTANCE: trạng thái mới và yêu cầu địa chỉ thay đề xuất cũ.

## Giới hạn thực tế và phần sau

Nháp vẫn local theo UID, không đồng bộ nhiều máy; tin đã gửi/history trên Firestore. Nội dung local đã dùng gửi không phải một tin server mới khi bấm retry, kể cả đã sửa local. Dùng Sửa và gửi lại từ tin REJECTED; sửa OPEN/đóng/ẩn là C11. Khi xác nhận mở bản REJECTED, form lấy nội dung server và revision local, thay bản local cùng ID khi người dùng lưu. Không tự merge hai nội dung.

C08 mới triển khai khách/người tìm việc xem tin OPEN và cache địa chỉ công khai. C12 đưa workAddress vào snapshot đúng jobVersion. C15 vẫn xác nhận địa điểm từng ca, không tự biến địa chỉ tin thành privateAddress đã được hai bên chốt. Không có map/GPS/geocoding hoặc xác minh địa chỉ tự động. Không có notification/push tin tuyển hay thống kê giả được thêm trong C07.

Version tăng cả ở quyết định và gửi lại. Danh mục active kiểm lại khi gửi/duyệt; tên danh mục là snapshot lúc gửi. Thay tên danh mục trong lúc đang chờ có thể yêu cầu từ chối và gửi lại trước khi chấp thuận. Phân trang dữ liệu đang thay đổi có thể cần tải lại để có danh sách mới nhất, không cam kết snapshot toàn danh sách giữa nhiều trang.

## Rà tĩnh và bàn giao

Đã đọc luồng/quyền/transaction, kiểm XML và khai báo ID/string/array/style, JSON index, kiểm dấu ngoặc/chuỗi Java bằng phân tích văn bản và git diff --check. Đây **không phải biên dịch Java, chạy Rules hoặc kiểm thử runtime**. Người dùng thực hiện [C07-CONSOLE-SCHEMA](C07-CONSOLE-SCHEMA.md), Sync/Run và [C07-CHECKLIST](C07-CHECKLIST.md). Ca khó chưa tái hiện và C02-F01 vẫn theo dõi riêng; không tự đánh dấu đạt.

Căn cứ API: [Firestore transactions](https://firebase.google.com/docs/firestore/manage-data/transactions), [quyền và query](https://firebase.google.com/docs/firestore/security/rules-query), [cursor phân trang](https://firebase.google.com/docs/firestore/query-data/query-cursors), [Tasks.withTimeout](https://developers.google.com/android/reference/com/google/android/gms/tasks/Tasks). Timeout bọc task không phải quyền hủy/rollback giao dịch server.
