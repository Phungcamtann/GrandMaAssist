# Danh sách File Giai đoạn 16: Thỏa thuận ca làm (Shift Agreement)

## 1. Domain Models
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/domain/model/AgreementSnapshot.java` (Tạo mới)
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/domain/model/WorkSession.java` (Tạo mới)

## 2. Repository & Data Layer
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/WorkSessionRepository.java` (Tạo mới)
- `firebase/firestore.rules` (Cập nhật quy tắc cho `workSessions`)

## 3. ViewModel
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/viewmodel/ShiftAgreementViewModel.java` (Tạo mới)

## 4. Layouts & Resources
- `app/src/main/res/values/session_strings.xml` (Tạo mới)
- `app/src/main/res/layout/activity_shift_agreement_edit.xml` (Tạo mới - Người thuê lập đề xuất ca)
- `app/src/main/res/layout/activity_shift_agreement_detail.xml` (Tạo mới - Người tìm việc xem & xác nhận ca)
- `app/src/main/res/layout/item_job_applicant.xml` (Cập nhật - bổ sung nút "Lập ca" cho ứng viên ACCEPTED)
- `app/src/main/res/layout/item_my_application.xml` (Cập nhật - bổ sung nút "Xem thỏa thuận ca" cho đơn ACCEPTED)

## 5. Activities & Navigation
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/sessions/ShiftAgreementEditActivity.java` (Tạo mới)
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/sessions/ShiftAgreementDetailActivity.java` (Tạo mới)
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/applications/JobApplicantsActivity.java` (Cập nhật điều hướng)
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/applications/MyApplicationsActivity.java` (Cập nhật điều hướng)
- `app/src/main/AndroidManifest.xml` (Đăng ký Activity mới)
