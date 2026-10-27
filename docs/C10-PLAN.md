# Kế hoạch Triển khai C10: Lưu việc quan tâm (Saved Jobs)

## 1. Mục tiêu và Phạm vi
- **Mục tiêu:** Cho phép người tìm việc (SEEKER) lưu lại các tin tuyển dụng quan tâm để xem lại sau, hỗ trợ phân loại tin còn nhận đơn và tin đã đóng/hết hạn, chống trùng lặp, và đồng bộ Firestore cùng bộ nhớ đệm SQLite cục bộ.
- **Phạm vi C10:**
  1. **Core Logic & Data Layer (Part 1 - Đã hoàn thành):**
     - Model: `SavedJob.java` (bất biến, hỗ trợ kiểm tra `isOpen()`, `isClosed()`, `isExpired()`).
     - SQLite Local Store: `SavedJobStore.java` (`saved_jobs.db`, bảng `cached_saved_jobs`, phân vùng theo `owner_uid`).
     - Repository: `SavedJobRepository.java` (lưu Firestore tại `users/{uid}/savedJobs/{jobId}`, ghi cache SQLite, fallback offline khi mất mạng).
     - Security Rules: `firebase/firestore.rules` (chỉ chính chủ đọc/tạo/xóa, kiểm tra schema).
     - Cấu hình Backup: Loại trừ `saved_jobs.db` trong `backup_rules.xml` và `data_extraction_rules.xml`.
  2. **ViewModel & UI Presentation (Part 2 - Đang thực hiện):**
     - `SavedJobsViewModel.java`: Quản lý danh sách, bộ lọc (Tất cả, Còn nhận đơn, Đã đóng/Hết hạn), xử lý bất đồng bộ.
     - `SavedJobsActivity.java`: Màn hình danh sách việc đã lưu, phân loại tab rõ ràng, thẻ việc cỡ lớn, nút bỏ lưu với hộp thoại xác nhận.
     - Layouts & Strings: `activity_saved_jobs.xml`, `item_saved_job.xml`, `saved_jobs_strings.xml`.
     - Tích hợp màn chi tiết: Mở khóa nút "Lưu việc" trong `PublicJobsActivity.java` và `activity_public_jobs.xml`, hiển thị trạng thái đã lưu / chưa lưu.
     - Điểm truy cập (Entry Point): Thêm nút "Việc đã lưu" trong Tab Tài khoản (`RoleHomePanel.java` & `role_account_content.xml`) và Toolbar `PublicJobsActivity.java`.
     - Đăng ký Manifest: Khai báo `SavedJobsActivity` trong `AndroidManifest.xml`.

## 2. Thiết kế Kiến trúc Dữ liệu & Quy tắc Nghiệp vụ
- **Khóa định danh (Idempotency):**
  - Đường dẫn: `users/{uid}/savedJobs/{jobId}`.
  - Sử dụng `jobId` làm Document ID để tự động chống lưu trùng mà không cần thêm logic kiểm tra trùng.
- **Trạng thái công việc:**
  - `OPEN`: Tin còn mở và chưa quá hạn (`expiresAt > now`).
  - `EXPIRED`: Tin có hạn ứng tuyển đã qua (`expiresAt <= now`).
  - `CLOSED`: Tin đã bị người thuê đóng hoặc quản trị viên ẩn.
- **Tối ưu cho người cao tuổi:**
  - Nút bấm tối thiểu 48-56dp, bo góc, tương phản cao.
  - Chữ lớn (18-20sp), in đậm, không nhồi nhét.
  - Thao tác "Bỏ lưu" có hộp thoại xác nhận rõ ràng để tránh bấm nhầm.

## 3. Danh sách File triển khai (Part 2)
1. `app/src/main/res/values/saved_jobs_strings.xml`
2. `app/src/main/res/layout/item_saved_job.xml`
3. `app/src/main/res/layout/activity_saved_jobs.xml`
4. `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/viewmodel/SavedJobsViewModel.java`
5. `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/jobs/SavedJobsActivity.java`
6. `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/viewmodel/PublicJobsViewModel.java` (tích hợp toggle lưu & kiểm tra trạng thái)
7. `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/jobs/PublicJobsActivity.java` & `app/src/main/res/layout/activity_public_jobs.xml`
8. `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/navigation/RoleHomePanel.java` & `app/src/main/res/layout/role_account_content.xml`
9. `app/src/main/AndroidManifest.xml`
