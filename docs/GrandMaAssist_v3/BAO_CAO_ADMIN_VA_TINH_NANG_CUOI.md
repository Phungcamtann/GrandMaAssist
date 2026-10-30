# BÁO CÁO HOÀN THIỆN 100% CÁC TÍNH NĂNG & QUẢN TRỊ ADMIN DỰ ÁN GRANDMAASSIST

> **Dự án**: GrandMaAssist — Nền tảng kết nối việc làm vừa sức cho người cao tuổi  
> **Thời điểm hoàn thiện**: Tháng 10/2026  
> **Phiên bản ứng dụng**: GrandMaAssist v3.0

---

## 1. THIẾT KẾ & TRIỂN KHAI HỆ THỐNG THÔNG BÁO (IN-APP & LOCAL NOTIFICATIONS)

### 1.1. Bảng kế hoạch chi tiết thông báo ca làm việc

| Thời điểm kích hoạt | Kênh thông báo | Tiêu đề | Nội dung thông điệp | Hành vi khi ấn vào |
| :--- | :--- | :--- | :--- | :--- |
| **Trước 2 tiếng** | Notification Hệ thống & In-app | ⏰ Sắp đến giờ làm việc (còn 2 tiếng) | "Ca làm: [Tên công việc] bắt đầu lúc [Giờ:Phút]. Cô/chú xem lại thỏa thuận và chuẩn bị nhé!" | Mở trực tiếp màn hình chi tiết Thỏa thuận ca làm việc (`ShiftAgreementDetailActivity`) |
| **Trước 30 phút** | Notification Hệ thống (Rung + Âm thanh) | 🚗 Chuẩn bị xuất phát (còn 30 phút) | "Ca làm: [Tên công việc] lúc [Giờ:Phút] tại [Địa chỉ]. Chúc cô/chú di chuyển an toàn!" | Mở chi tiết ca làm, có sẵn nút "Xem đường đi (Google Maps)" để chỉ đường tức thì |
| **Trong vòng 24 giờ tới** | In-app Card trên Trang chủ (`RoleHomePanel`) | 📌 Việc làm sắp tới | Hiển thị thẻ thông tin ca làm nổi bật với nút chuyển nhanh đến trang ca làm việc | Giúp người lớn tuổi không bỏ lỡ ca làm ngay cả khi tắt thông báo hệ thống |

### 1.2. Kiến trúc kỹ thuật đã triển khai
- **`ShiftReminderReceiver`**: Kế thừa `BroadcastReceiver`, tạo `NotificationChannel` (`grandma_shift_channel`) với mức ưu tiên `IMPORTANCE_HIGH`, tự động gắn cờ rung và âm thanh thông báo.
- **`ShiftAlarmScheduler`**: Sử dụng `AlarmManager.setExactAndAllowWhileIdle()` nhằm kích hoạt thông báo chuẩn xác từng phút ngay cả khi điện thoại ở chế độ Doze hoặc tắt màn hình.
- **Tự động hủy thông báo**: Khi ca làm việc được hủy hoặc đã hoàn thành, bộ lập lịch sẽ tự động hủy các PendingIntent tương ứng.

---

## 2. HƯỚNG DẪN LÀM SẠCH VÀ NẠP DỮ LIỆU SEED TRÊN FIREBASE CONSOLE

Để dữ liệu ứng dụng hiển thị hoàn hảo, không còn nhãn "DEMO", nhóm thực hiện các bước sau:

### 2.1. Các bước làm sạch dữ liệu cũ trên Firebase Console
1. Đăng nhập [Firebase Console](https://console.firebase.google.com/) và chọn project **GrandMaAssist**.
2. Vào mục **Build** $\rightarrow$ **Firestore Database**.
3. Tại cột danh sách các collection:
   - **Xóa dữ liệu cũ của collection `areas`**: Chọn collection `areas`, nhấp vào dấu 3 chấm góc trên bên phải $\rightarrow$ chọn **Delete collection** $\rightarrow$ gõ xác nhận tên collection.
   - **Xóa dữ liệu cũ của collection `jobCategories`**: Chọn collection `jobCategories` $\rightarrow$ chọn **Delete collection**.
   - (Tùy chọn) Xóa các tin tuyển dụng test trong collection `jobs` để bắt đầu với dữ liệu mới.

### 2.2. Dữ liệu hạt giống chuẩn thực tế (Seed Data)
Tất cả đã được lưu tại thư mục `firebase/`:

1. **File `firebase/areas-data.json`** — 10 quận huyện thực tế tại TP.HCM:
   - `Q1`: Quận 1, TP.HCM
   - `Q3`: Quận 3, TP.HCM
   - `Q5`: Quận 5, TP.HCM
   - `Q7`: Quận 7, TP.HCM
   - `Q10`: Quận 10, TP.HCM
   - `Q_TANBINH`: Quận Tân Bình, TP.HCM
   - `Q_BINHTHANH`: Quận Bình Thạnh, TP.HCM
   - `Q_PHUNHUAN`: Quận Phú Nhuận, TP.HCM
   - `Q_GOVAP`: Quận Gò Vấp, TP.HCM
   - `TP_THUDUC`: Thành phố Thủ Đức, TP.HCM

2. **File `firebase/job-categories-data.json`** — 8 danh mục công việc thiết thực cho người cao tuổi:
   - `KITCHEN_HELP`: Phụ bếp & Sơ chế thực phẩm
   - `CLEANING_HOME`: Dọn dẹp & Sắp xếp nhà cửa
   - `GARDEN_CARE`: Chăm sóc sân vườn & Cây cảnh
   - `BABYSITTING`: Trông trẻ & Đón bé tan học
   - `SHOP_HELP`: Phụ giúp quán ăn & Tạp hóa
   - `PARKING_ASSIST`: Trông giữ xe & Hướng dẫn khách
   - `LIBRARY_HELP`: Hỗ trợ sắp xếp sách & Thư viện
   - `OTHER_HELP`: Việc phụ giúp gia đình khác

---

## 3. TỐI ƯU HÓA TOÀN DIỆN PHÍA QUẢN TRỊ ADMIN

### 3.1. Trang chủ Admin (`role_home_content.xml` & `RoleHomePanel.java`)
- **Tiêu đề & Nút tải lại**: Tiêu đề **"Tổng quan"**, bố trí nút icon tải lại (`ic_reload`) ở góc phải trên cùng ngang hàng với tiêu đề để làm mới toàn bộ số liệu thời gian thực.
- **4 Khối Thống kê Dashboard (Hiển thị ngay khi vào trang chủ)**:
  1. **Khối Người thuê**: Tổng số người thuê & số lượng đã xác minh (`Đã xác minh: X`). Bấm vào mở trực tiếp màn hình duyệt/quản lý hồ sơ.
  2. **Khối Người tìm việc**: Tổng số người tìm việc & số lượng đã duyệt CCCD (`Đã duyệt CCCD: Y`). Bấm vào mở trực tiếp màn hình duyệt hồ sơ.
  3. **Khối Tin tuyển dụng**: Tổng số tin đăng & số lượng tin đang mở (`Đang mở: Z`). Bấm vào mở trực tiếp màn hình duyệt tin tuyển dụng.
  4. **Khối Phí dịch vụ thu**: Tổng doanh thu phí 10% thu được từ tất cả các ca hoàn tất (`COMPLETED`, ví dụ: `450.000 đ`) & số ca làm việc đã hoàn thành. Bấm vào mở chi tiết bảng phân tích thống kê.
- **2 Khối Hàng đợi Xử lý (Nằm ngay bên dưới 4 khối thống kê)**:
  - **Khối "Tin cần duyệt"**: Hiển thị bộ đếm động theo thời gian thực (ví dụ: *"Hiện có 5 tin đang chờ duyệt"*). Bấm vào chuyển thẳng đến trang duyệt tin.
  - **Khối "Hồ sơ chờ duyệt"**: Hiển thị bộ đếm hồ sơ CCCD đang chờ (ví dụ: *"Hiện có 3 hồ sơ chờ duyệt"*). Bấm vào chuyển thẳng đến trang duyệt hồ sơ.
- **Dọn dẹp các nút thừa**: Loại bỏ hoàn toàn các nút Báo cáo vi phạm, Tranh chấp, Ca quá hạn khỏi màn hình chính.

### 3.2. Trang Duyệt Hồ sơ (`activity_verification.xml` & `VerificationActivity.java`)
- Bố trí nút tải lại (`ic_reload`) trên toolbar góc phải.
- Thanh tìm kiếm thu gọn với icon tìm kiếm (`ic_search`) nằm trên cùng một hàng.
- Loại bỏ nút chuyển "Duyệt tin tuyển" không cần thiết.
- Thanh bộ lọc ngang hiện đại:
  - Nút **[Chưa duyệt]**: Lọc hồ sơ trạng thái `SUBMITTED`.
  - Nút **[Đã duyệt]**: Lọc hồ sơ trạng thái `APPROVED` / `VERIFIED` / `REJECTED`.
  - Nút icon Phễu lọc (`ic_filter`): Mở hộp thoại chọn nhanh *Tất cả hồ sơ / Người tìm việc / Người thuê*.

### 3.3. Trang Duyệt Tin Tuyển (`activity_job_posts.xml` & `JobPostActivity.java`)
- Loại bỏ nút nhảy "Kiểm tra hồ sơ".
- Thanh tab phân loại ở đầu trang: **[Chưa duyệt]** vs **[Đã duyệt]** giúp Admin dễ dàng xem lại các tin tuyển đã duyệt hoặc từ chối trước đó.

### 3.4. Bảng điều khiển Thống kê Admin (`AdminStatsActivity.java` & `activity_admin_stats.xml`)
Giao diện Dashboard dạng lưới 2 hàng $\times$ 2 cột với thiết kế Material Card chuyên nghiệp:
1. **Khối Người thuê (Employers)**: Tổng số tài khoản và số tài khoản đã xác minh.
2. **Khối Người tìm việc (Job Seekers)**: Tổng số người tìm việc và số lượng đã duyệt CCCD.
3. **Khối Tin tuyển dụng (Job Posts)**: Tổng số tin đăng và số tin đang mở (`OPEN`).
4. **Khối Doanh thu phí dịch vụ (Platform Revenue)**: Tổng số phí dịch vụ nền tảng (10%) thu được từ tất cả các ca làm việc đã hoàn thành thành công (`COMPLETED`), định dạng chuẩn tiền tệ VNĐ.
5. **Thẻ Vận hành ca làm việc**: Thống kê tổng số ca, số ca hoàn thành và tỷ lệ hoàn thành (%).

---

## 4. HAI TÍNH NĂNG NÂNG CAO TRẢI NGHIỆM

### 4.1. Tích hợp chỉ đường Google Maps ("Xem đường đi")
- **`MapNavigationHelper`**: Khởi chạy Google Maps qua URI `google.navigation:q=...` với địa chỉ cụ thể và khu vực. Tự động chuyển tiếp sang trình duyệt nếu máy chưa cài Google Maps.
- **Vị trí tích hợp**:
  - Chi tiết việc làm công khai (`PublicJobsActivity`).
  - Chi tiết tin tuyển dụng của Admin & Người thuê (`JobPostActivity`).
  - Chi tiết thỏa thuận ca làm việc của 2 vai trò (`ShiftAgreementDetailActivity`).

### 4.2. Ảnh nơi làm việc khi đăng tin & Fallback hình minh họa
- **Chọn và nén ảnh**: Trong màn hình tạo tin (`JobDraftActivity`), người thuê có thể ấn "Chọn ảnh nơi làm việc". Ảnh được tự động nén nhẹ (JPEG chất lượng cao, kích thước $\le 800$px, dung lượng an toàn dưới 200KB dưới dạng Data URI Base64), lưu trữ bền vững.
- **Hiển thị & Fallback**:
  - Nếu tin có ảnh nơi làm việc $\rightarrow$ hiển thị ảnh thực tế tại nơi làm việc.
  - Nếu người thuê không tải ảnh $\rightarrow$ hệ thống tự động hiển thị tranh minh họa theo đúng danh mục công việc (`job_illustration_kitchen`, `job_illustration_garden`, `job_illustration_shop`, `job_illustration_library`), đảm bảo giao diện luôn sinh động và thân thiện với người cao tuổi.

---

## 5. TÙY CHỈNH CỠ CHỮ HIỂN THỊ (ACCESSIBILITY FONT SCALING)

- **Cấu hình**: Trong hộp thoại Cài đặt hỗ trợ tiếp cận (`AccessibilitySettingsDialog`), bổ sung mục "Cỡ chữ hiển thị" với 3 mức lựa chọn:
  - **Vừa (100%)**: Cỡ chữ chuẩn của hệ thống.
  - **Lớn (115%)**: Cỡ chữ to rõ, dễ đọc.
  - **Rất lớn (130%)**: Cỡ chữ đặc biệt dành cho cô chú mắt kém.
- **Xem trước trực quan (Live Preview)**: Khi chọn mức cỡ chữ, đoạn văn bản mẫu bên dưới sẽ lập tức co giãn kích thước theo thời gian thực để người dùng thấy rõ trước khi ấn "Lưu cài đặt".
- **Áp dụng toàn diện**: Cỡ chữ được lưu vào `AccessibilityStore` và tự động áp dụng thông qua `Configuration.fontScale`, làm mới giao diện ngay sau khi lưu.

---

## 6. HẠN CHẾ & HƯỚNG MỞ RỘNG (DÀNH CHO BÁO CÁO ĐỒ ÁN)

Do thời lượng và phạm vi của đồ án tốt nghiệp, một số luồng được giả định trong điều kiện vận hành lý tưởng và khuyến nghị phát triển trong tương lai:
1. **Xử lý tranh chấp nâng cao**: Phiên bản hiện tại tập trung vào trải nghiệm tích cực và phòng ngừa rủi ro từ đầu bằng hợp đồng ca và check-in 2 chiều. Luồng gửi khiếu nại tranh chấp và trọng tài can thiệp sẽ là module mở rộng tiếp theo.
2. **Cổng thanh toán tự động thời gian thực**: Hiện tại hệ thống mô phỏng dòng tiền ký quỹ (Escrow) và ví tiền nội bộ theo chu trình an toàn; hướng mở rộng tiếp theo là tích hợp trực tiếp Webhook ngân hàng (VietQR / Napas / MoMo).
3. **OCR CCCD tự động với AI**: Hiện tại khâu xác thực kết hợp đọc QR trên thẻ CCCD và Admin đối chiếu bằng mắt; hướng mở rộng là tích hợp Gemini Vision API để bóc tách thông tin tự động 100%.
