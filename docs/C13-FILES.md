# Danh sách File Tính năng C13: Xử lý ứng viên và liên hệ

## 1. Firebase Rules
- `firebase/firestore.rules`: Tinh chỉnh nhánh `allow update` và `allow list` cho Employer xử lý đơn ứng tuyển.

## 2. Domain Models
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/domain/model/EmployerContactSnapshot.java`: Snapshot liên hệ người thuê khi chấp nhận đơn.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/domain/model/Application.java`: Bổ sung trường `employerContactSnapshot`.

## 3. Data Repository
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/ApplicationRepository.java`: Bổ sung `getApplicationsForJob()`, `accept()`, `reject()`.

## 4. ViewModel Layer
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/viewmodel/JobApplicantsViewModel.java`: Quản lý danh sách ứng viên theo tin tuyển dụng.

## 5. UI Layer
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/applications/JobApplicantsActivity.java`: Màn hình duyệt ứng viên dành cho người thuê.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/jobs/JobPostActivity.java`: Bổ sung nút mở danh sách ứng viên.
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/applications/MyApplicationsActivity.java`: Hiển thị thông tin liên hệ người thuê khi được nhận.
- `app/src/main/AndroidManifest.xml`: Đăng ký `JobApplicantsActivity`.

## 6. Layouts & Resources
- `app/src/main/res/layout/activity_job_applicants.xml`: Layout màn hình danh sách ứng viên.
- `app/src/main/res/layout/item_job_applicant.xml`: Layout thẻ ứng viên.
- `app/src/main/res/layout/item_my_application.xml`: Bổ sung thông tin liên hệ nhà tuyển dụng.
- `app/src/main/res/values/application_strings.xml`: Chuỗi văn bản C13.
