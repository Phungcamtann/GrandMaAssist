# Checklist Kiểm tra Tính năng C11: Quản lý tin đã đăng

## 1. Firebase Rules & Data Layer
- [x] Cập nhật `firebase/firestore.rules`:
  - [x] Hỗ trợ Employer đóng tin (`OPEN -> CLOSED`).
  - [x] Hỗ trợ Employer sửa tin đã duyệt (`OPEN -> PENDING_REVIEW`).
  - [x] Hỗ trợ Admin ẩn tin vi phạm (`OPEN/CLOSED -> HIDDEN`).
  - [x] Cập nhật `jobHistoryCreate` cho các action `'CLOSED'` và `'HIDDEN'`.
- [x] Cập nhật `JobPostRepository.java`:
  - [x] Thêm phương thức `close(String uid, JobPost expected)`.
  - [x] Thêm phương thức `hide(String uid, JobPost expected, String reason)`.
  - [x] Cập nhật `submit()` cho phép sửa tin từ trạng thái `OPEN`.
- [x] Cập nhật `JobDraftViewModel.java`:
  - [x] Cho phép mở form chỉnh sửa nội dung tin đang ở trạng thái `OPEN`.

## 2. ViewModel & Giao diện (UI)
- [x] Cập nhật `JobPostViewModel.java`:
  - [x] Thêm `closePost()`.
  - [x] Thêm `hidePost()`.
  - [x] Quản lý trạng thái nhập lý do linh hoạt qua `reasonAction`.
- [x] Cập nhật `activity_job_posts.xml`:
  - [x] Thêm nút "Đóng tin" (`jpClose`).
  - [x] Thêm nút "Ẩn tin" (`jpHide`).
  - [x] Cập nhật nhãn động cho khung nhập lý do (`jpReasonTitle`, `jpConfirmAction`).
- [x] Cập nhật `JobPostActivity.java`:
  - [x] Gắn sự kiện click và hộp thoại xác nhận cho `jpClose`.
  - [x] Xử lý mở màn hình sửa tin cho cả tin `OPEN` và `REJECTED`.
  - [x] Xử lý sự kiện click `jpHide` và xác nhận ẩn tin có lý do.
  - [x] Cập nhật render hiển thị nút theo vai trò và trạng thái tin.
