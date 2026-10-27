# Danh sách File Tính năng C12: Ứng tuyển công việc

## 1. Firebase Rules & Indexes
- `firebase/firestore.rules`: Bổ sung shape validation, helper function `applicationCanApply`, và quy tắc bảo mật collection `/applications/{applicationId}`.
- `firebase/firestore.indexes.json`: Composite indexes phục vụ truy vấn danh sách đơn theo `seekerId` và `createdAt` (DESC).

## 2. Domain Models
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/domain/model/ApplicantSnapshot.java`: Snapshot thông tin người tìm việc tại thời điểm nộp đơn.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/domain/model/JobSnapshot.java`: Snapshot công việc tại thời điểm nộp đơn.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/domain/model/Application.java`: Thực thể đơn ứng tuyển.

## 3. Data Repository
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/ApplicationRepository.java`: Thực thi các transaction nộp đơn, rút đơn, kiểm tra trạng thái đơn theo công việc và truy vấn danh sách đơn cá nhân.

## 4. ViewModel Layer
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/viewmodel/ApplicationViewModel.java`: Quản lý danh sách đơn, bộ lọc tab (Tất cả, Chờ phản hồi, Đã xử lý), hành động rút đơn.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/viewmodel/PublicJobsViewModel.java`: Thêm logic kiểm tra đơn đã nộp (`checkApplied()`).

## 5. UI & Navigation Layer
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/applications/MyApplicationsActivity.java`: Màn hình "Đơn của tôi" dành cho Seeker.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/jobs/PublicJobsActivity.java`: Xử lý nút "Ứng tuyển ngay", dialog nộp đơn, kiểm tra hồ sơ hưu trí `APPROVED`.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/navigation/RoleNavigationBar.java`: Kích hoạt tab "Việc của tôi" (`Action.WORK`) cho Seeker.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/navigation/RoleHomePanel.java`: Kích hoạt thẻ "Việc của tôi" trên trang chủ Seeker.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/profile/ProfilePanel.java`: Điều hướng `Action.WORK` của Seeker sang `MyApplicationsActivity`.
- `app/src/main/AndroidManifest.xml`: Đăng ký `MyApplicationsActivity`.

## 6. Layouts & Resources
- `app/src/main/res/values/application_strings.xml`: Chuỗi văn bản tiếng Việt chuẩn mực, thân thiện cho người lớn tuổi.
- `app/src/main/res/layout/activity_my_applications.xml`: Giao diện màn hình "Đơn của tôi".
- `app/src/main/res/layout/item_my_application.xml`: Card hiển thị từng đơn ứng tuyển.
- `app/src/main/res/layout/dialog_apply_job.xml`: Dialog nộp đơn ứng tuyển kèm lời nhắn và xác nhận chia sẻ số điện thoại.
- `app/src/main/res/layout/activity_public_jobs.xml`: Cập nhật nút ứng tuyển `pbBtnApply`.
