# Checklist Kiểm tra Tính năng C13: Xử lý ứng viên và liên hệ

## 1. Firebase Rules & Data Layer
- [x] Cập nhật `firebase/firestore.rules`:
  - [x] Kiểm tra quyền update đơn của EMPLOYER: `ACCEPTED` (kèm `employerContactSnapshot`) hoặc `REJECTED`.
  - [x] Cho phép EMPLOYER query danh sách đơn theo `jobId` và `employerId` (nâng limit lên 50).
- [x] Tạo Domain Models:
  - [x] `EmployerContactSnapshot.java`
  - [x] Cập nhật `Application.java` (hỗ trợ `employerContactSnapshot`).
- [x] Mở rộng `ApplicationRepository.java`:
  - [x] Phương thức `getApplicationsForJob(uid, jobId)`.
  - [x] Phương thức `accept(uid, applicationId)` qua Transaction nguyên tử.
  - [x] Phương thức `reject(uid, applicationId)` qua Transaction nguyên tử.

## 2. ViewModel & Giao diện (UI)
- [x] ViewModel:
  - [x] `JobApplicantsViewModel.java`: Tải danh sách ứng viên, lọc tab, xử lý chấp nhận/từ chối.
- [x] Giao diện (Layouts & Strings):
  - [x] `item_job_applicant.xml`: Card thông tin ứng viên, badge trạng thái, các nút hành động (Gọi điện, Chấp nhận, Từ chối).
  - [x] `activity_job_applicants.xml`: Màn hình danh sách ứng viên của tin tuyển dụng.
  - [x] Cập nhật `application_strings.xml` với các chuỗi thông báo, xác nhận tiếng Việt.
- [x] Màn hình `JobApplicantsActivity.java`:
  - [x] Hiển thị danh sách ứng viên, bộ lọc tab.
  - [x] Hộp thoại xác nhận khi bấm "Chấp nhận" hoặc "Từ chối".
  - [x] Gọi điện thoại trực tiếp qua `Intent.ACTION_DIAL`.
- [x] Tích hợp vào `JobPostActivity.java`:
  - [x] Nút "Xem danh sách ứng viên" xuất hiện khi Employer xem tin chi tiết (OPEN/CLOSED) và trên từng card việc.
- [x] Cập nhật `MyApplicationsActivity.java` & `item_my_application.xml`:
  - [x] Hiển thị thông tin liên hệ của Người thuê khi đơn `ACCEPTED` kèm nút gọi điện thoại.

## 3. Build & Kiểm thử
- [x] Biên dịch Java: `BUILD SUCCESSFUL`
- [x] Đóng gói APK: `./gradlew assembleDebug` -> `BUILD SUCCESSFUL`
