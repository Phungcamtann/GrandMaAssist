# Kế hoạch Triển khai Tính năng C13: Xử lý ứng viên và liên hệ (Employer Review Applications & Contact)

## 1. Mục tiêu và Phạm vi
Theo đặc tả phân tích thiết kế hệ thống v3 (Mục 13 & Use cases UC14, UC27):
1. **Người thuê (EMPLOYER)** xem danh sách ứng viên đã nộp đơn vào các tin tuyển dụng của mình (khi tin ở trạng thái `OPEN` hoặc `CLOSED`).
2. **Xem chi tiết hồ sơ ứng viên:**
   - Họ tên, số điện thoại đã xác minh (nếu người tìm việc đã đồng ý chia sẻ khi nộp đơn).
   - Kỹ năng, tóm tắt kinh nghiệm làm việc, khu vực hoạt động.
   - Lời nhắn của ứng viên gửi tới nhà tuyển dụng.
   - Trạng thái đơn: `Chờ phản hồi` (`PENDING`), `Được chấp nhận` (`ACCEPTED`), `Bị từ chối` (`REJECTED`), `Đã rút đơn` (`WITHDRAWN`).
3. **Thao tác xử lý của Người thuê:**
   - **Chấp nhận (`ACCEPTED`):** Chủ tin đồng ý nhận ứng viên -> Hệ thống lưu `employerContactSnapshot` (họ tên, số điện thoại đã xác minh của Người thuê để Người tìm việc có thể liên hệ).
   - **Từ chối (`REJECTED`):** Chủ tin từ chối đơn ứng tuyển.
   - **Xử lý cạnh tranh nguyên tử (Concurrency):** Thực hiện bằng Firestore Transaction; nếu ứng viên rút đơn (`WITHDRAWN`) cùng thời điểm chủ tin thao tác, chỉ một bên thành công, không bị ghi đè.
4. **Liên hệ nhanh bằng điện thoại (UC27):**
   - Nút gọi điện thoại trực tiếp (`ACTION_DIAL`) kết nối số điện thoại đã xác minh của ứng viên.
5. **Cập nhật màn hình "Đơn của tôi" (`MyApplicationsActivity`):**
   - Khi đơn được chấp nhận (`ACCEPTED`), hiển thị thêm thông tin liên hệ của Người thuê (`employerContactSnapshot`: Tên và nút gọi điện thoại trực tiếp cho Người thuê).

---

## 2. Kế hoạch triển khai chi tiết

### Giai đoạn 1: Backend, Security Rules & Data Repository
1. **Firebase Security Rules (`firebase/firestore.rules`):**
   - Tinh chỉnh nhánh `allow update` cho Người thuê:
     - Khi `ACCEPTED`: Cho phép cập nhật `status`, `updatedAt`, `employerContactSnapshot` (kiểm tra `employerId == request.auth.uid`, tin thuộc trạng thái cho phép).
     - Khi `REJECTED`: Cho phép cập nhật `status`, `updatedAt`.
   - Bổ sung `allow list` trên `/applications`: Đảm bảo Người thuê có thể query `applications` theo `jobId` và `employerId`.
2. **Domain Model:**
   - Tạo `EmployerContactSnapshot.java`: Gồm `displayName`, `phoneE164`, `acceptedAt`.
   - Cập nhật `Application.java`: Thêm trường `employerContactSnapshot`.
3. **Repository Layer (`ApplicationRepository.java`):**
   - Thêm phương thức `getApplicationsForJob(String uid, String jobId)`: Lấy danh sách ứng viên của một tin tuyển dụng (không cần composite index, sắp xếp theo thời gian nộp).
   - Thêm phương thức `accept(String uid, String applicationId)`: Transaction chấp nhận đơn và lưu snapshot liên hệ người thuê.
   - Thêm phương thức `reject(String uid, String applicationId)`: Transaction từ chối đơn.

### Giai đoạn 2: ViewModel & Giao diện người dùng (UI/UX)
1. **ViewModel:**
   - Tạo `JobApplicantsViewModel.java` (hoặc mở rộng `ApplicationViewModel`): Quản lý danh sách ứng viên của tin, lọc theo tab (Tất cả, Chờ duyệt, Đã chấp nhận, Đã từ chối), gọi `accept()` và `reject()`.
2. **Layouts & Resources:**
   - Tạo `activity_job_applicants.xml`: Màn hình danh sách ứng viên với tiêu đề công việc, các tab trạng thái.
   - Tạo `item_job_applicant.xml`: Card hiển thị thông tin từng ứng viên, badge trạng thái, nút "Gọi điện", nút "Chấp nhận", nút "Từ chối".
   - Cập nhật chuỗi văn bản trong `application_strings.xml`.
3. **Màn hình `JobApplicantsActivity.java`:**
   - Hiển thị danh sách ứng viên của tin tuyển dụng, xử lý click Chấp nhận / Từ chối (hộp thoại xác nhận rõ ràng) và Gọi điện thoại (`Intent.ACTION_DIAL`).
4. **Tích hợp vào `JobPostActivity.java`:**
   - Khi Người thuê xem tin chi tiết (ở trạng thái `OPEN` hoặc `CLOSED`), hiển thị nút **"Xem danh sách ứng viên"** dẫn tới `JobApplicantsActivity`.
5. **Cập nhật `MyApplicationsActivity.java` & `item_my_application.xml`:**
   - Khi đơn ở trạng thái `ACCEPTED`, hiển thị thêm khung thông tin người thuê kèm nút gọi điện thoại nhanh cho người thuê.
