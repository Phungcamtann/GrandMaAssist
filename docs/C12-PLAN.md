# Kế hoạch Triển khai Tính năng C12: Ứng tuyển công việc (Gửi, xem và rút đơn)

## 1. Mục tiêu
Triển khai hoàn chỉnh tính năng ứng tuyển công việc dành cho Người tìm việc (SEEKER) theo đặc tả kiến trúc GrandMa Assist:
1. **Kiểm tra điều kiện ứng tuyển:**
   - Người tìm việc (vai trò `SEEKER`, tài khoản `ACTIVE`).
   - Đã xác minh số điện thoại qua `FIREBASE_AUTH`.
   - Đã được Admin phê duyệt hồ sơ nghỉ hưu (`retirementStatus == 'APPROVED'`).
   - Tin tuyển dụng đang ở trạng thái `OPEN` và còn hạn nhận (`expiresAt > serverTime`).
   - Không được tự ứng tuyển vào tin của chính mình.
   - Chưa từng nộp đơn cho tin này (`applicationId = jobId + "_" + seekerId` ngăn chặn trùng đơn).
2. **Gửi đơn ứng tuyển (Apply):**
   - Cho phép nhập lời nhắn tùy chọn (tối đa 500 ký tự).
   - Xác nhận chia sẻ số điện thoại liên hệ đã xác minh với nhà tuyển dụng.
   - Tạo bản ghi `applications/{jobId_seekerId}` kèm snapshot ứng viên (`ApplicantSnapshot`) và snapshot tin tuyển dụng (`JobSnapshot`).
3. **Xem danh sách đơn ("Đơn của tôi"):**
   - Danh sách các công việc đã nộp đơn, thời gian nộp, thông tin tóm tắt và trạng thái (`Chờ phản hồi`, `Đã chấp nhận`, `Bị từ chối`, `Đã rút`).
4. **Rút đơn ứng tuyển (Withdraw):**
   - Cho phép người tìm việc chủ động rút đơn khi còn ở trạng thái `PENDING`.

---

## 2. Lộ trình thực hiện 2 giai đoạn

### Giai đoạn 1: Core Logic, Domain Model, Data Repository & Firebase Security Rules
1. **Firebase Security Rules (`firebase/firestore.rules`):**
   - Bổ sung quy tắc bảo mật cho collection `/applications/{applicationId}`.
   - Ràng buộc cấu trúc ID (`id == jobId + '_' + seekerId`).
   - Kiểm tra điều kiện nộp đơn (`role == 'SEEKER'`, `retirementStatus == 'APPROVED'`, phone verified, tin `OPEN`, chưa hết hạn).
   - Kiểm tra điều kiện rút đơn (`seekerId == request.auth.uid`, `status == 'PENDING' -> 'WITHDRAWN'`).
2. **Domain Models:**
   - `Application.java`: Model chính của đơn ứng tuyển.
   - `ApplicantSnapshot.java`: Snapshot thông tin người tìm việc khi nộp đơn.
   - `JobSnapshot.java`: Snapshot tin tuyển dụng tại thời điểm nộp đơn.
3. **Repository Layer:**
   - `ApplicationRepository.java`: Thực hiện Transaction nộp đơn, rút đơn, kiểm tra trạng thái đã nộp theo tin và lấy danh sách đơn của tôi.

### Giai đoạn 2: ViewModel, UI & Tích hợp luồng điều hướng
1. **ViewModel:**
   - `ApplicationViewModel.java`: Quản lý trạng thái nộp đơn, rút đơn và danh sách đơn.
2. **Giao diện (UI):**
   - Dialog/Sheet xác nhận nộp đơn từ màn hình chi tiết việc (`PublicJobsActivity`).
   - Màn hình "Đơn của tôi" (`MyApplicationsActivity`) hoặc tích hợp vào tab "Việc của tôi" của Seeker.
   - Cập nhật nút "Ứng tuyển ngay" trong `PublicJobsActivity` (thay thế nút disable "Ứng tuyển (sắp có)").
   - Kích hoạt tab điều hướng `Việc của tôi` (`Action.WORK`) trên thanh `RoleNavigationBar` cho Seeker.
