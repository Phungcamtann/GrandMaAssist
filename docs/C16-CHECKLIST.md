# Checklist Kiểm tra Giai đoạn 16: Thỏa thuận ca làm (Shift Agreement)

## 1. Domain & Data Layer
- [x] Tạo Domain Models:
  - [x] `AgreementSnapshot.java`: tasks, privateAddress, startAt, endAt, laborAmount, feeRateBps (1000), serviceFee (10%), totalDue, policy versions.
  - [x] `WorkSession.java`: id, applicationId, jobId, seekerId, employerId, agreement, agreementVersion, employerAcceptedVersion, seekerAcceptedVersion, status.
- [x] `WorkSessionRepository.java`:
  - [x] Session ID định danh bất biến: `session_` + `applicationId`.
  - [x] Chống tạo trùng lặp ca (`SESSION_ALREADY_EXISTS`): kiểm tra ca đang hoạt động trước khi tạo.
  - [x] Phương thức tạo đề xuất ca (`proposeSession`): kiểm tra đơn `ACCEPTED`, tính phí 10%, tạo version 1.
  - [x] Phương thức sửa đề xuất ca (`editProposal`): tăng `agreementVersion`, reset xác nhận seeker.
  - [x] Phương thức Người tìm việc xác nhận (`seekerAccept`): nếu trùng version -> chuyển `AWAITING_FUNDS`.
  - [x] Phương thức Người tìm việc từ chối (`seekerReject`): chuyển `CANCELLED`.
  - [x] Logic kiểm tra xung đột trùng lịch (`checkScheduleConflict`): $startAt < existingEndAt \land endAt > existingStartAt$.
  - [x] Truy vấn danh sách ca theo vai trò và trạng thái (`getSessionsForUser`).
  - [x] Tìm ca theo đơn (`getSessionsForApplication`): Lấy trực tiếp document `session_` + `applicationId`, khắc phục 100% lỗi `PERMISSION_DENIED` do query thiếu điều kiện lọc.
  - [x] Đồng bộ địa chỉ làm việc: tự động trích xuất `workAddress` và `areaName` từ `jobSnapshot` của đơn ứng tuyển, lưu thống nhất vào snapshot thỏa thuận.

## 2. Firestore Security Rules
- [x] Thêm `match /workSessions/{sessionId}`:
  - [x] `allow get`: seeker, employer của ca hoặc admin.
  - [x] `allow list`: kiểm tra `resource.data.seekerId == request.auth.uid || resource.data.employerId == request.auth.uid`.
  - [x] `allow create`: employer của đơn `ACCEPTED`, phiên bản 1, cấu trúc `sessionShape`.
  - [x] `allow update`: 
    - Employer sửa khi còn `PROPOSED` -> tăng version.
    - Seeker xác nhận đúng version -> `AWAITING_FUNDS`.
    - Seeker từ chối -> `CANCELLED`.

## 3. ViewModel & Giao diện (UI)
- [x] Thống nhất Địa chỉ làm việc (Không còn ô nhập riêng gây loãng):
  - [x] Bỏ hoàn toàn `EditText` nhập địa chỉ cụ thể trên màn hình lập thỏa thuận ca (`ShiftAgreementEditActivity`).
  - [x] Hiển thị cố định khối thông tin: Địa chỉ làm việc (`workAddress`) và Khu vực (`areaName`) y nguyên từ tin tuyển dụng ban đầu.
  - [x] Màn hình xem chi tiết thỏa thuận ca (`ShiftAgreementDetailActivity`): Hiển thị thống nhất nhãn "Địa chỉ làm việc", nội dung gồm Địa chỉ chính xác và Khu vực từ tin tuyển ban đầu.
- [x] Căn chỉnh tiêu đề & Huy hiệu trạng thái:
  - [x] Tiêu đề "Chi tiết thỏa thuận ca" nằm riêng trên dòng 1, `match_parent`, `singleLine="true"`, không bị ngắt dòng.
  - [x] Badge trạng thái nằm dòng 2: rút gọn thành "Đã thỏa thuận" (thay cho dòng dài cũ), "Chờ xác nhận", "Sẵn sàng".
- [x] Tính phí minh bạch ngay từ khi đăng việc (`JobDraftActivity` & `activity_job_drafts.xml`):
  - [x] Hiển thị ngay hộp tính phí dưới ô Tiền công: Tiền công người làm, Phí dịch vụ 10% (màu đỏ `#C62828`, ghi chú Người thuê trả thêm), Tổng chi phí dự tính mỗi ca.
  - [x] Cập nhật bảng xem trước tin (`jdPreviewPay`) với đầy đủ bóc tách 3 dòng phí.
  - [x] Không gắn nhãn Demo.
- [x] Sửa lỗi nút bị mất chữ (descender text clipping):
  - [x] Nút "Lập thỏa thuận ca" / "Xem thỏa thuận ca" trên `item_job_applicant.xml`: `layout_height="wrap_content"`, `minHeight="48dp"`, đối xứng padding 12dp, `includeFontPadding="false"`.
  - [x] Nút "Xem thỏa thuận ca" trên `item_my_application.xml`: `layout_height="wrap_content"`, `minHeight="48dp"`, đối xứng padding 12dp, `includeFontPadding="false"`.
  - [x] Tất cả các nút bấm trong `activity_shift_agreement_edit.xml` và `activity_shift_agreement_detail.xml` đều dùng `wrap_content` + padding an toàn.
- [x] Khóa nút chống spam:
  - [x] Vô hiệu hóa nút gửi đề xuất ngay khi bấm (`btnSubmit.setEnabled(false)`), chỉ mở lại khi có lỗi.
  - [x] Trên danh sách ứng viên `JobApplicantsActivity`: tự động đổi thành nút "Xem thỏa thuận ca" khi ca đã được lập, ngăn chặn việc tạo trùng ca.
- [x] Màu sắc phí dịch vụ:
  - [x] Hiển thị màu đỏ (`#C62828`) tại tất cả các màn hình (Đăng tin, Lập thỏa thuận ca, Chi tiết thỏa thuận ca).

## 4. Biên dịch & Đóng gói
- [x] `./gradlew compileDebugJavaWithJavac` -> BUILD SUCCESSFUL.
- [x] `./gradlew assembleDebug` -> BUILD SUCCESSFUL.
