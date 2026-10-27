# Danh sách File Triển khai Tính năng C11: Quản lý tin đã đăng

## 1. Firebase Rules & Data Layer
- `firebase/firestore.rules`:
  - Mở rộng hàm `jobHistoryCreate`: Bổ sung chấp nhận các action `'CLOSED'` (Người thuê đóng tin) và `'HIDDEN'` (Admin ẩn tin), đồng thời cho phép Người thuê sửa tin từ trạng thái `OPEN` bên cạnh `REJECTED`.
  - Mở rộng quy tắc `allow update` trên `/jobs/{id}`:
    + Người thuê đóng tin `OPEN -> CLOSED`: Chỉ cho phép thay đổi 3 trường `['status', 'version', 'updatedAt']`.
    + Người thuê sửa tin `OPEN -> PENDING_REVIEW`: Yêu cầu cấu trúc dữ liệu hợp lệ (`submittedJob`), tạo bản ghi lịch sử `SUBMITTED`.
    + Admin ẩn tin `(OPEN / CLOSED) -> HIDDEN`: Yêu cầu phải có lý do hợp lệ (`reviewReason`), ghi nhận `reviewedBy`, `reviewedAt`, `updatedAt`, và tạo bản ghi lịch sử `HIDDEN`.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/JobPostRepository.java`:
  - `close(String uid, JobPost expected)`: Đóng tin tuyển dụng thông qua Firestore Transaction, chuyển trạng thái `OPEN -> CLOSED`, tăng version và ghi nhận history.
  - `hide(String uid, JobPost expected, String reason)`: Admin ẩn tin tuyển dụng vi phạm qua Firestore Transaction, chuyển trạng thái sang `HIDDEN`, ghi nhận lý do và snapshot history.
  - `submit(String uid, LocalJobDraft original)`: Mở rộng kiểm tra cho phép sửa và gửi lại tin từ trạng thái `OPEN` (bên cạnh `REJECTED`).
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/viewmodel/JobDraftViewModel.java`:
  - Cập nhật `openRejected(id)` cho phép mở tin ở cả 2 trạng thái `REJECTED` và `OPEN` để chỉnh sửa nội dung và gửi duyệt lại.

## 2. ViewModel & Giao diện (UI)
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/viewmodel/JobPostViewModel.java`:
  - Quản lý trạng thái nhập lý do linh hoạt qua `reasonAction` ("REJECT" hoặc "HIDE").
  - Thêm phương thức `closePost()`: Gọi repo đóng tin tuyển dụng.
  - Thêm phương thức `hidePost()`: Kiểm tra tính hợp lệ của lý do ẩn tin (tối đa 1.000 ký tự) và gọi repo ẩn tin.
- `app/src/main/res/layout/activity_job_posts.xml`:
  - Cập nhật khung `jpReasonSection` dùng linh hoạt cho cả Từ chối duyệt và Ẩn tin (`jpReasonTitle`, `jpConfirmAction`).
  - Thêm nút "Ẩn tin tuyển dụng" (`jpHide`) và "Đóng tin (Ngừng nhận đơn)" (`jpClose`).
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/jobs/JobPostActivity.java`:
  - Xử lý click `jpClose`: Mở AlertDialog xác nhận đóng tin trước khi thực thi.
  - Xử lý click `jpHide`: Mở khung nhập lý do ẩn tin.
  - Xử lý click `jpEdit`: Mở hộp thoại xác nhận sửa tin (với thông báo phù hợp cho tin OPEN hoặc REJECTED) rồi mở `JobDraftActivity`.
  - Hiển thị card lý do động: "Lý do ẩn tin" khi tin là `HIDDEN` hoặc "Lý do từ chối" khi tin là `REJECTED`.
  - Phân quyền hiển thị chính xác các nút bấm theo vai trò (EMPLOYER vs ADMIN) và trạng thái tin tuyển dụng.
