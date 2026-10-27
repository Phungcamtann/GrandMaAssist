# Checklist Kiểm tra Tính năng C12: Ứng tuyển công việc (Hoàn thành 100%)

## 1. Firebase Rules & Data Layer
- [x] Cập nhật `firebase/firestore.rules`:
  - [x] Thêm quy tắc phân quyền cho `/applications/{applicationId}`.
  - [x] Khóa định danh đơn theo cặp `jobId + "_" + seekerId`.
  - [x] Kiểm tra điều kiện nộp đơn: tài khoản SEEKER, ACTIVE, phone FIREBASE_AUTH, retirementStatus APPROVED.
  - [x] Kiểm tra điều kiện tin tuyển dụng: OPEN, chưa hết hạn, không phải chính chủ đăng tin.
  - [x] Kiểm tra quyền rút đơn: chỉ chính chủ SEEKER, trạng thái PENDING -> WITHDRAWN, không sửa các trường snapshot.
  - [x] Phân quyền đọc: chỉ chính chủ seeker, employer của tin, hoặc admin.
- [x] Tạo Domain Models:
  - [x] `ApplicantSnapshot.java`
  - [x] `JobSnapshot.java`
  - [x] `Application.java`
- [x] Tạo `ApplicationRepository.java`:
  - [x] Phương thức `apply()` qua Firestore Transaction (ngăn chặn gửi trùng, kiểm tra điều kiện).
  - [x] Phương thức `withdraw()` chuyển trạng thái sang WITHDRAWN.
  - [x] Phương thức `getApplicationForJob()` kiểm tra đã nộp đơn hay chưa.
  - [x] Phương thức `getMyApplications()` tải danh sách đơn của người tìm việc.

## 2. ViewModel & Giao diện (UI)
- [x] ViewModel:
  - [x] `ApplicationViewModel.java`
  - [x] `PublicJobsViewModel.java` (bổ sung `hasApplied`, `applyCheckBusy`, `checkApplied()`).
- [x] Layout & UI:
  - [x] Dialog nộp đơn (`dialog_apply_job.xml`): xem tóm tắt thông tin, nhập lời nhắn, checkbox đồng ý chia sẻ số điện thoại.
  - [x] Màn hình danh sách đơn của tôi (`MyApplicationsActivity.java` & `activity_my_applications.xml` & `item_my_application.xml`).
  - [x] Cập nhật `activity_public_jobs.xml` & `PublicJobsActivity.java`:
    - [x] Nút "Ứng tuyển ngay" hoạt động thay vì nút disable.
    - [x] Hiển thị trạng thái "Đã nộp đơn" khi seeker đã nộp đơn cho tin này.
    - [x] Kiểm tra điều kiện `retirementStatus == APPROVED` trước khi mở dialog nộp đơn.
  - [x] Cập nhật điều hướng `RoleNavigationBar.java`, `RoleHomePanel.java`, `ProfilePanel.java`:
    - [x] Bật `live = true` cho tab "Việc của tôi" (`Action.WORK`) của SEEKER dẫn tới danh sách đơn ứng tuyển (`MyApplicationsActivity`).
    - [x] Hỗ trợ điều hướng chuyển đổi mượt mà giữa Tìm việc, Việc của tôi, và Trang chủ.

## 3. Build & Kiểm thử
- [x] Biên dịch Java: `BUILD SUCCESSFUL`
- [x] Đóng gói APK: `./gradlew assembleDebug` -> `BUILD SUCCESSFUL`
