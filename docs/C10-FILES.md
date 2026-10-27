# Danh sách File Triển khai Tính năng C10: Lưu việc quan tâm (Saved Jobs)

## 1. Core Logic & Data Layer (Part 1)
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/domain/model/SavedJob.java`:
  - Model Value Object bất biến chứa thông tin việc đã lưu: `seekerId`, `jobId`, `savedAt`, `job` (snapshot `PublicJob`), `jobStatus`.
  - Hỗ trợ các helper logic: `isOpen()`, `isClosed()`, `isExpired()`.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/local/SavedJobStore.java`:
  - SQLite database `saved_jobs.db` với bảng `cached_saved_jobs`.
  - Phân vùng theo `owner_uid` đảm bảo bảo mật và dọn sạch khi đăng xuất/đổi tài khoản.
  - Lưu payload snapshot JSON của `PublicJob` để hỗ trợ hiển thị đầy đủ thông tin khi ngoại tuyến.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/SavedJobRepository.java`:
  - Tầng Repository phối hợp đồng bộ giữa Firestore (`users/{uid}/savedJobs/{jobId}`) và SQLite Store.
  - Cung cấp: `saveJob()`, `unsaveJob()`, `isJobSaved()`, `getSavedJobs()`.
  - Tự động fallback đọc SQLite khi mất kết nối mạng.
- `firebase/firestore.rules`:
  - Phân quyền cho `users/{uid}/savedJobs/{jobId}`: Chỉ chủ sở hữu `isOwner(uid)` mới được đọc, tạo hoặc xóa.
  - Ràng buộc cấu trúc tài liệu: `seekerId == uid`, `jobId == jobId`, `savedAt == request.time`. Chặn chỉnh sửa `allow update: if false`.
- `app/src/main/res/xml/backup_rules.xml` & `data_extraction_rules.xml`:
  - Thêm cấu hình loại trừ `saved_jobs.db` khỏi backup đám mây để bảo vệ quyền riêng tư.

## 2. ViewModel, Giao diện (UI) & Điều hướng (Part 2)
- `app/src/main/res/values/saved_jobs_strings.xml`:
  - Toàn bộ chuỗi ngôn ngữ cho tính năng Lưu việc (tiêu đề, nhãn tab, badge trạng thái, hộp thoại xác nhận).
- `app/src/main/res/layout/item_saved_job.xml`:
  - Card bo góc 16dp hiển thị công việc đã lưu kèm badge trạng thái ("Còn nhận đơn", "Hết hạn", "Đã đóng tin"), tên người tuyển, khu vực, tiền công, ngày lưu và nút "Bỏ lưu".
- `app/src/main/res/layout/activity_saved_jobs.xml`:
  - Màn hình chính "Việc đã lưu": Topbar, tiêu đề, nút làm mới, 3 tab lọc ("Tất cả", "Còn nhận", "Đã đóng"), vùng hiển thị danh sách và Empty State.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/viewmodel/SavedJobsViewModel.java`:
  - Quản lý danh sách, bộ lọc 3 tab (`FILTER_ALL`, `FILTER_OPEN`, `FILTER_CLOSED`), đếm số lượng, lắng nghe đăng xuất và cập nhật bất đồng bộ.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/jobs/SavedJobsActivity.java`:
  - Activity điều khiển hiển thị danh sách, xử lý đổi tab, mở chi tiết công việc, hiển thị AlertDialog xác nhận khi bỏ lưu và tối ưu cho người lớn tuổi.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/viewmodel/PublicJobsViewModel.java`:
  - Tích hợp `SavedJobRepository`, quản lý cờ `isSaved`, `saveBusy`, và các hàm `checkSaved()`, `toggleSave()`.
- `app/src/main/res/layout/activity_public_jobs.xml`:
  - Thêm nút "Đã lưu" (`pbSavedJobs`) trên toolbar và gắn id `pbBtnSave` cho nút Lưu việc trong khung chi tiết.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/jobs/PublicJobsActivity.java`:
  - Đọc `detail` từ Intent extra để mở trực tiếp chi tiết tin từ `SavedJobsActivity` và `GuidedSearchActivity`.
  - Bind nút `pbSavedJobs` mở màn hình Việc đã lưu; xử lý click `pbBtnSave` thực hiện lưu/bỏ lưu tin kèm Toast phản hồi.
- `app/src/main/res/layout/role_account_content.xml` & `RoleHomePanel.java`:
  - Thêm nút "Việc đã lưu" trong Tab Tài khoản dành riêng cho người tìm việc (SEEKER).
- `app/src/main/AndroidManifest.xml`:
  - Đăng ký `SavedJobsActivity` với cấu hình an toàn `exported="false"`.
