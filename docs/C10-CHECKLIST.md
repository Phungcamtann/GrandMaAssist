# Checklist Kiểm tra Tính năng C10: Lưu việc quan tâm

## 1. Core Logic & Data Layer (Part 1 - Đã hoàn thành)
- [x] Tạo `SavedJob.java` với các hàm kiểm tra trạng thái `isOpen()`, `isClosed()`, `isExpired()`.
- [x] Tạo `SavedJobStore.java` quản lý `saved_jobs.db`, bảng `cached_saved_jobs`, phân vùng theo `owner_uid`.
- [x] Tạo `SavedJobRepository.java` kết hợp Firestore `users/{uid}/savedJobs/{jobId}` và SQLite cục bộ.
- [x] Cập nhật `firebase/firestore.rules` phân quyền subcollection `savedJobs`.
- [x] Cấu hình loại trừ `saved_jobs.db` trong `backup_rules.xml` và `data_extraction_rules.xml`.

## 2. ViewModel & UI Presentation (Part 2 - Đã hoàn thành)
- [x] Khai báo chuỗi giao diện trong `saved_jobs_strings.xml`.
- [x] Thiết kế layout thẻ tin đã lưu `item_saved_job.xml` (Badge trạng thái, thông tin rõ ràng, nút bỏ lưu).
- [x] Thiết kế layout màn hình chính `activity_saved_jobs.xml` (Topbar, 3 Tabs phân loại, Empty state, vùng cuộn).
- [x] Xây dựng `SavedJobsViewModel.java` quản lý danh sách, filter tabs, trạng thái loading/lỗi.
- [x] Xây dựng `SavedJobsActivity.java` điều khiển UI, tương thích người lớn tuổi, xử lý Back và xoay màn hình.
- [x] Tích hợp nút Lưu việc trong `PublicJobsActivity.java` và `activity_public_jobs.xml`:
  - [x] Hiển thị trạng thái "Lưu việc" / "Đã lưu".
  - [x] Xử lý lưu/bỏ lưu tin với phản hồi nhanh (Toast).
  - [x] Nhắc đăng nhập nếu là khách.
  - [x] Hỗ trợ mở chi tiết từ `SavedJobsActivity` (truyền extra `detail`).
- [x] Thêm nút truy cập "Việc đã lưu" trong Tab Tài khoản (`role_account_content.xml` + `RoleHomePanel.java`) và Toolbar `PublicJobsActivity.java`.
- [x] Khai báo `SavedJobsActivity` trong `AndroidManifest.xml`.
