# Kế hoạch Triển khai Giai đoạn 16: Thỏa thuận ca làm (Shift Agreement)

## 1. Bối cảnh & Khớp nối Hệ thống
- **Vị trí theo Bảng phân rã tính năng (Roadmap)**:
  - **Mục 16**: **Thỏa thuận ca làm** (Tương ứng UC17 & UC18 trong tài liệu thiết kế hệ thống `GrandMaAssist_PhanTichThietKe_v3.md`).
  - **Mục 17**: **Cấp và giữ tiền mô phỏng** (Tương ứng UC19).
  - **Mục 18**: **Theo dõi ca và xác nhận bắt đầu** (Tương ứng UC20).
- **Tiền điều kiện**: Ứng viên đã được duyệt trúng tuyển (`Application.status == 'ACCEPTED'`).
- **Mục tiêu Giai đoạn 16**:
  1. Cho phép Người thuê tạo đề xuất ca làm việc từ đơn đã chấp nhận.
  2. Kế thừa toàn bộ thông tin từ Tin tuyển dụng (không bắt người dùng nhập lại từ đầu): Tự động điền tiêu đề nhiệm vụ, địa chỉ, khu vực, tiền công mẫu.
  3. Người thuê chỉ cần chọn **Ngày làm việc cụ thể** và **Khung giờ (Bắt đầu - Kết thúc)** của ca, có thể bổ sung địa chỉ chi tiết/chỉ dẫn riêng.
  4. **Minh bạch phí dịch vụ 10% ngay từ đầu**: Tính toán tức thời ngay khi nhập tiền công; hiển thị rõ ràng phần Người thuê trả thêm 10% và phần Người tìm việc nhận trọn vẹn 100%.
  5. **Cơ chế xác nhận hai bên (Dual Confirmation)**: Quản lý theo `agreementVersion`. Cả hai bên cùng xác nhận phiên bản mới nhất -> chuyển trạng thái sang `AWAITING_FUNDS`. Nếu sửa đổi -> version tăng lên và reset xác nhận.
  6. **Kiểm soát trùng lịch (Schedule Conflict Control)**: Ngăn chặn tạo hoặc xác nhận ca nếu Người tìm việc đã có một ca khác bị trùng khoảng thời gian (`startAt` - `endAt`).

---

## 2. Giải đáp thắc mắc & Tối ưu hóa Thiết kế

### A. Khoản phí 10% có bị thừa hay không?
- **Không thừa, mà là chính sách cốt lõi của nền tảng**:
  - Theo Mục 11 của tài liệu thiết kế: Phí dịch vụ nền tảng là 10% do Người thuê chi trả (không trừ vào tiền công của người cao tuổi).
  - **Giải pháp hiển thị minh bạch ngay từ đầu**:
    - Khi Người thuê nhập tiền công: $C = 240.000$ đ.
    - Hệ thống tự động tính: Phí nền tảng (10%) $F(C) = 24.000$ đ.
    - Tổng tiền Người thuê cần thanh toán $T = C + F(C) = 264.000$ đ.
    - Phía Người tìm việc: Hiển thị nổi bật nhãn: *"Tiền công của cô/chú: 240.000 đ — không trừ bất kỳ khoản phí nào"*.
    - Theo yêu cầu của người dùng (phục vụ đồ án): Không gắn các nhãn demo/mô phỏng, giữ giao diện sạch đẹp, chân thực.

### B. Có bị lặp lại dữ liệu từ các luồng trước (C06, C07, C12, C13) không?
- **Hoàn toàn kế thừa thông minh (Auto-Prefill)**:
  - Khi Người thuê bấm *"Tạo thỏa thuận ca"* từ đơn ứng tuyển đã `ACCEPTED`:
    - Tiêu đề công việc: Tự động lấy từ `jobSnapshot.title`.
    - Tiền công mẫu: Tự động lấy từ `jobSnapshot.payAmount`.
    - Địa chỉ làm việc: Tự động điền từ `jobSnapshot.workAddress`.
    - Khu vực: Tự động lấy từ `jobSnapshot.areaCode`.
  - **Người thuê CHỈ CẦN LÀM**:
    - Chọn ngày làm việc (DatePicker).
    - Chọn giờ bắt đầu & giờ kết thúc (TimePicker).
    - Bổ sung số nhà/hướng dẫn riêng cho ca (nếu có).
    - Bấm *"Gửi thỏa thuận"*.
  => Không hề có thao tác nhập lại thông tin trùng lặp!

---

## 3. Kiến trúc Dữ liệu & Firestore Rules

### Collection: `workSessions` (hoặc `sessions`)
- **Document ID**: `session_<applicationId>_<timestamp>`
- **Fields**:
  - `id`: String
  - `applicationId`: String (ID đơn ứng tuyển `ACCEPTED`)
  - `jobId`: String
  - `seekerId`: String
  - `employerId`: String
  - `agreementVersion`: Long (mặc định = 1)
  - `agreement`: Map
    - `tasks`: String (nội dung nhiệm vụ)
    - `privateAddress`: String (địa chỉ làm việc cụ thể)
    - `areaCode`: String
    - `startAt`: Timestamp (thời gian bắt đầu ca)
    - `endAt`: Timestamp (thời gian kết thúc ca)
    - `laborAmount`: Long (tiền công người làm nhận, số nguyên VND)
    - `feeRateBps`: 1000 (10%)
    - `serviceFee`: Long (làm tròn half-up $laborAmount \times 10\%$)
    - `totalDue`: Long ($laborAmount + serviceFee$)
    - `feePolicyVersion`: "GMA_FEE_V1"
    - `cancellationPolicyVersion`: "GMA_CANCEL_V1"
    - `fundingDeadline`: Timestamp
  - `employerAcceptedVersion`: Long
  - `seekerAcceptedVersion`: Long
  - `status`: String (`PROPOSED` -> `AWAITING_FUNDS` -> `READY` -> `IN_PROGRESS`...)
  - `createdAt`: Timestamp
  - `updatedAt`: Timestamp

### Quy tắc Firestore Rules:
- `allow read`: Hai bên tham gia ca (`auth.uid == seekerId || auth.uid == employerId || admin`).
- `allow create`:
  - Người thuê của tin (`auth.uid == employerId`).
  - Đơn ứng tuyển tham chiếu phải tồn tại và có `status == 'ACCEPTED'`.
  - `agreementVersion == 1`, `employerAcceptedVersion == 1`, `seekerAcceptedVersion == null`.
  - `status == 'PROPOSED'`.
  - Kiểm tra tính toán phí: `serviceFee == laborAmount / 10` và `totalDue == laborAmount + serviceFee`.
- `allow update`:
  - **Người thuê sửa đổi đề xuất** (khi còn ở `PROPOSED`): Tăng `agreementVersion = before.agreementVersion + 1`, đặt `employerAcceptedVersion = newVersion`, reset `seekerAcceptedVersion = null`.
  - **Người tìm việc xác nhận**: Cập nhật `seekerAcceptedVersion = agreementVersion`. Nếu cả 2 bên cùng bằng `agreementVersion` -> tự động chuyển `status = 'AWAITING_FUNDS'`.
  - **Người tìm việc từ chối**: Chuyển `status = 'CANCELLED'`.

---

## 4. Kiểm soát Trùng lịch (Schedule Conflict Control)
- Khi Người thuê gửi đề xuất hoặc Người tìm việc bấm xác nhận:
  - Hệ thống truy vấn các ca làm việc của Người tìm việc có trạng thái trong tập `['PROPOSED', 'AWAITING_FUNDS', 'READY', 'IN_PROGRESS']`.
  - Kiểm tra điều kiện chồng lấn:
    $$(startAt < existingEndAt) \land (endAt > existingStartAt)$$
  - Nếu phát hiện trùng lịch: Cảnh báo ngay cho người dùng với thông tin chi tiết của ca bị trùng và ngăn chặn xác nhận.

---

## 5. Danh sách Màn hình & Thành phần UI

1. **Phía Người thuê (EMPLOYER)**:
   - Trên màn hình danh sách ứng viên (`JobApplicantsActivity`): Với ứng viên có trạng thái `ACCEPTED`, hiển thị thêm nút **"Lập thỏa thuận ca"** (`btnCreateSession`).
   - Màn hình **`ShiftAgreementEditActivity.java`**:
     - Form lập/sửa thỏa thuận: Kế thừa dữ liệu sẵn có, chọn ngày & giờ, hiển thị bảng tính tiền & phí 10% tức thời, nút *"Gửi đề xuất"*.
2. **Phía Người tìm việc (SEEKER)**:
   - Màn hình **"Việc của tôi / Ca làm việc"** hoặc xem thỏa thuận ca từ thông báo/đơn ứng tuyển:
   - Màn hình **`ShiftAgreementDetailActivity.java`**:
     - Xem chi tiết nhiệm vụ, ngày giờ, địa điểm, tiền công nhận trọn vẹn (không trừ phí).
     - 2 nút hành động lớn: **"Đồng ý nhận ca"** và **"Từ chối ca"**.
3. **Màn hình Danh sách Ca làm việc (`WorkSessionsActivity.java`)**:
   - Dùng chung cho cả 2 bên (EMPLOYER & SEEKER):
   - Phân loại tab: *Chờ xác nhận (PROPOSED)*, *Chờ cấp tiền (AWAITING_FUNDS)*, *Sắp tới (READY)*, *Đang làm/Hoàn thành*.

---

## 6. Lộ trình thực hiện Giai đoạn 16
- [ ] **Bước 1**: Tạo Domain Model (`WorkSession.java`, `AgreementSnapshot.java`, `FeePolicy.java`).
- [ ] **Bước 2**: Viết logic tính phí & kiểm tra trùng lịch trong `WorkSessionRepository.java`.
- [ ] **Bước 3**: Cập nhật `firebase/firestore.rules` bảo vệ collection `workSessions`.
- [ ] **Bước 4**: Tạo ViewModel `ShiftAgreementViewModel.java`.
- [ ] **Bước 5**: Thiết kế Layouts & Strings (Giao diện thân thiện người cao tuổi, font to, bảng tiền rõ ràng).
- [ ] **Bước 6**: Xây dựng Activity tạo/sửa thỏa thuận cho Người thuê (`ShiftAgreementEditActivity`).
- [ ] **Bước 7**: Xây dựng Activity xem & xác nhận thỏa thuận cho Người tìm việc (`ShiftAgreementDetailActivity`).
- [ ] **Bước 8**: Tích hợp nút mở thỏa thuận từ `JobApplicantsActivity` và `MyApplicationsActivity`.
- [ ] **Bước 9**: Biên dịch, kiểm thử luồng thỏa thuận 2 bên và bàn giao nghiệm thu.
