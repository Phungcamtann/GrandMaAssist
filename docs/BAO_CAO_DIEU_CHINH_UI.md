# Báo Cáo Chi Tiết: Tối Ưu Hóa & Chuẩn Hóa Giao Diện (UI Cleanup Sprint)

> **Dự án:** GrandMaAssist - Ứng dụng hỗ trợ kết nối việc làm và ca làm linh hoạt cho người cao tuổi  
> **Thời gian thực hiện:** 29/10/2026  
> **Mục tiêu:** Tinh chỉnh, làm sạch và chuẩn hóa toàn bộ giao diện ở 2 vai trò **Người tìm việc (Seeker)** và **Người thuê (Employer)**, đảm bảo trải nghiệm trực quan, dễ hiểu, thân thiện tối đa với người cao tuổi (không can thiệp vào vai trò Admin).

---

## 1. Giao Diện Chung (Common UI)

### 1.1. Màn hình Đăng nhập (Login Screen)
* **Nút xem trước việc làm:**
  * Rút gọn chuỗi hiển thị từ `"Xem việc không cần đăng nhập"` thành **`"Xem trước công việc"`** (`pb_guest` & `pb_open`).
  * Đồng bộ hiển thị nút này ở cả 2 tab đăng nhập bằng **Số điện thoại** và **Google**.
* **Đăng nhập bằng Số điện thoại:**
  * Ô nhập số điện thoại: Đơn giản hóa hint từ `"0xxxxxxxxx hoặc +84xxxxxxxxx"` thành **`"Điền số điện thoại"`** (`phone_number_hint`).
  * Dòng ghi chú SMS bên dưới: Đổi từ `"Cô/chú sẽ nhận mã qua SMS"` thành **`"Mã sẽ được gửi vào tin nhắn"`** (`phone_sms_hint`) giúp người lớn tuổi dễ hiểu hơn.
  * Màn hình kiểm tra số điện thoại (Review Phone): Cập nhật thông báo giải thích rõ ràng và an tâm: **`"Có thể phát sinh cước phí để gửi tin nhắn. Nếu ứng dụng chuyển sang trình duyệt kiểm tra thì cô/chú cứ yên tâm chờ chút là thành công nhé"`** (`phone_consent`), ngăn ngừa sự bối rối khi gặp reCAPTCHA web check.

### 1.2. Tính năng "Liên hệ tổng đài" (Call Center Dialog)
* **Trang chủ 2 vai trò:**
  * Thay thế ô bị vô hiệu hóa `"Trợ giúp"` thành **`"Liên hệ tổng đài"`** (`n_help` & `gs_seeker_support`), bổ sung biểu tượng điện thoại hỗ trợ (`ic_phone_support.xml`).
  * Khi nhấn vào, hiển thị hộp thoại nổi `CallCenterDialog` (`dialog_call_center.xml`):
    * **Tiêu đề:** `"Liên hệ tổng đài"` (căn giữa, in đậm).
    * **Ghi chú hướng dẫn:** `"Cô/chú có thắc mắc về ứng dụng hoặc cần hỗ trợ trực tiếp có thể liên hệ qua các số liên hệ theo bộ phận dưới đây. Cô/chú có thể ấn trực tiếp vào biểu tượng để gọi"`.
    * **1. Hỗ trợ thông tin ứng dụng:**
      * LH1: Phạm Thiên Long - `0936719323` (kèm icon gọi điện).
      * LH2: Nguyễn Duy Thành Tài - `0935272284` (kèm icon gọi điện).
    * **2. Thắc mắc về chính sách và thông tin khác:**
      * LH: Dương Hoàng Sâm - `0775446799` (kèm icon gọi điện).
    * **3. Hỗ trợ sự cố và lỗi kỹ thuật:**
      * LH: Phùng Cẩm Tân - `0878805819` (kèm icon gọi điện).
    * **Lời chào:** `"Đội ngũ GrandMaAssist luôn sẵn sàng hỗ trợ nhé!"`.
    * Mỗi số điện thoại tích hợp nút icon gọi điện liên kết trực tiếp `Intent.ACTION_DIAL`, đưa người dùng vào trình quay số mà không cần nhập thủ công.

### 1.3. Hồ sơ & Thông tin tài khoản
* **Hồ sơ cá nhân (`profile_content.xml`):**
  * Loại bỏ nút rời rạc chưa kích hoạt `"Trợ giúp & Hướng dẫn"`.
  * Gom cài đặt về một mối chuẩn mực: giữ nút **`"Cài đặt hỗ trợ tiếp cận"`** (`AccessibilitySettingsDialog`), bổ sung icon bánh răng cài đặt (`ic_settings.xml`).
* **Khử trùng lặp Ví tiền:**
  * Nút `"Ví tiền của tôi"` trước đây nằm ở cả phần Hồ sơ và Thông tin tài khoản (`role_account_content.xml`).
  * Đã dọn dẹp và ẩn hoàn toàn khỏi phần Thông tin tài khoản, chỉ duy trì 1 nơi duy nhất tại **Hồ sơ cá nhân** để tránh phân mảnh điều hướng.
* **Chuẩn hóa nút "Đăng xuất" (Sign Out):**
  * Cả 3 nút Đăng xuất trong toàn bộ ứng dụng (`profile_content.xml`, `role_account_content.xml`, `activity_main.xml`) được đồng bộ sang kiểu **Viên thuốc viền đỏ** (`button_sign_out.xml`):
    * Nền hồng nhạt `#FFF5F5`, viền đỏ `#E53935` 1.5dp, chữ đỏ đậm căn giữa `17sp`, chiều cao tối thiểu `54dp`.
  * **Hộp thoại xác nhận an toàn:** Khi bấm đăng xuất, ứng dụng hiển thị `AlertDialog` xác nhận:
    * Tiêu đề: `"Xác nhận đăng xuất"`
    * Nội dung: `"Cô/chú có chắc chắn muốn đăng xuất khỏi ứng dụng không?"`
    * Nút: `"Ở lại"` (Hủy) & `"Đăng xuất"` (Đồng ý), loại bỏ hoàn toàn rủi ro người lớn tuổi chạm nhầm bị văng khỏi tài khoản.

### 1.4. Chuẩn hóa "Việc làm sắp tới" & Thẻ hiển thị bộ đếm công việc (Home Screen Card)
* **Thống nhất tên gọi:** Đổi tên `"Việc cần xử lý"` ở cả 2 vai trò thành **`"Việc làm sắp tới"`**.
* **Thiết kế Thẻ trạng thái thông minh trực quan (`role_home_content.xml`):**
  * Thay vì nút bấm phẳng đơn giản, giao diện trang chủ của cả Người tìm việc và Người thuê được nâng cấp thành một **Thẻ trạng thái bo tròn chuyên biệt** (`@drawable/verification_section`), gồm 2 thành phần chính:
    * **Biểu tượng:** Icon sổ ghi chú checklist màu xanh lá thương hiệu (`@drawable/ic_nav_queue`, kích thước 32dp).
    * **Nội dung 2 tầng chữ trực quan:**
      * Dòng 1: **`Việc làm sắp tới`** (cỡ chữ `20sp`, in đậm, màu `#1A1C19`, font `be_vietnam_pro_semibold`).
      * Dòng 2: **`Có N việc sắp tới`** (hoặc **`Chưa có việc sắp tới`** khi `N = 0`, cỡ chữ `15sp`, màu xám `#5C6360`).
  * **Quy chuẩn lọc ca hiển thị & đếm:**
    * Tự động truy vấn từ `WorkSessionRepository.getSessionsForUser(...)`.
    * Chỉ đếm và hiển thị các ca làm việc đã được **cả 2 bên xác nhận thỏa thuận ca** (trạng thái `READY`, `IN_PROGRESS`, `COMPLETION_PENDING`, hoặc cả 2 bên đã xác nhận phiên bản thỏa thuận) và chưa hoàn thành/chưa hủy.
  * Khi bấm vào thẻ: Chuyển trực tiếp sang danh sách ca làm việc (`WorkSessionsActivity.java`) với tiêu đề đồng nhất là `"Việc làm sắp tới"`.

### 1.5. Chi tiết công việc (`PublicJobsActivity.java`)
* Tăng nhẹ cỡ chữ nội dung thẻ chi tiết công việc từ `15sp` $\rightarrow$ **`17sp`** (`labelHeading` & `labelBody`):
  * Địa chỉ làm việc
  * Thời gian làm việc (buổi, ngày trong tuần)
  * Công việc cần làm (mô tả nhiệm vụ)
  * Yêu cầu thể lực & kỹ năng
* Đảm bảo người cao tuổi nhìn rõ ràng mà không bị tràn khung hay phá vỡ bố cục thẻ.

### 1.6. Làm sạch Lịch sử Giao dịch Ví (`WalletTransactionAdapter.java`)
* Lược bỏ toàn bộ các mã định danh kỹ thuật dài dòng (như `#session_...`, `tx_...`).
* Chỉ hiển thị tiêu đề giao dịch thân thiện và ngắn gọn:
  * *"Nạp tiền vào ví qua VietQR"*
  * *"Rút tiền về tài khoản ngân hàng"*
  * *"Tạm giữ cọc ca làm"*
  * *"Nhận tiền công ca làm"*
  * *"Hoàn tiền cọc ca làm đã hủy"*

---

## 2. Giao Diện Người Thuê (Employer UI)

### 2.1. Tinh gọn Trang chủ Người thuê (`RoleHomePanel.java`)
* Xóa bỏ nút trùng lặp `"Ca làm việc"` trên trang chủ người thuê.
* Thống nhất chỉ dùng 1 thẻ trạng thái duy nhất **`"Việc làm sắp tới"`** dẫn đến màn hình theo dõi ca.
* Thanh điều hướng dưới đáy (Bottom Navigation Bar): Đồng nhất nhãn tab thành **`"Việc làm sắp tới"`** (`n_sessions`).

### 2.2. Phân nhóm thẻ Tin của tôi (`JobPostActivity.java` & `activity_job_posts.xml`)
* Thay thế nút nháp rời rạc bằng thanh **3 Tab bộ lọc trực quan**:
  1. **Đang đăng:** Lọc và hiển thị các tin tuyển dụng đang mở tiếp nhận đơn (`OPEN`) hoặc đang chờ admin duyệt (`PENDING_REVIEW`).
  2. **Đã xong:** Lọc các tin đã đóng (`CLOSED`) hoặc bị từ chối (`REJECTED`).
  3. **Bản nháp:** Đọc danh sách các bản nháp cục bộ lưu trên máy từ `JobDraftStore`. Mỗi thẻ nháp hiển thị tên nháp, khu vực, thời gian cập nhật gần nhất và nút **`"Tiếp tục chỉnh sửa"`** giúp người thuê mở lại form soạn thảo dễ dàng.
* **Xử lý triệt để lỗi mất chữ/cắt mép chữ (Text Clipping Fix):**
  * Nguyên nhân gốc: Do style button mặc định áp padding dọc lớn kết hợp với chiều cao cố định và việc gọi `setBackgroundResource()` lúc runtime làm reset lại padding hệ thống, khiến chữ tiếng Việt có dấu bị cắt viền trên/dưới.
  * Giải pháp xử lý: Cấu hình `layout_height="wrap_content"`, `minHeight="48dp"`, `paddingTop="10dp"`, `paddingBottom="10dp"`, `includeFontPadding="false"`, đồng thời tạo hàm `applyTabStyle()` tái áp đặt padding chuẩn ngay sau khi đổi background. Đảm bảo toàn bộ chữ "Đang đăng", "Đã xong", "Bản nháp" hiển thị trọn vẹn, căn giữa hoàn hảo.
* Nếu nhóm không có tin nào, hiển thị thông báo trống lịch sự và rõ ràng.

---

## 3. Giao Diện Người Tìm Việc (Seeker UI)

### 3.1. Hướng dẫn Tìm việc Nâng cao (`GuidedSearchActivity.java` & `activity_guided_search.xml`)
* **Bổ sung nút "Hủy tìm":**
  * Đặt tại Bước 1 ở góc phải hàng điều hướng (cùng hàng với nút "Bỏ qua"), màu đỏ nổi bật `18sp`.
  * Giúp cô/chú khi lỡ bấm nhầm vào luồng tìm việc có thể thoát ngay về trang chủ mà không phải bấm lùi qua nhiều bước.
* **Tăng cỡ chữ phần chốt lựa chọn (Bước 5 Tóm tắt):**
  * Tiêu đề mục: Tăng từ `18sp` $\rightarrow$ **`20sp`** (in đậm).
  * Nội dung đã chọn: Tăng từ `16sp` $\rightarrow$ **`18sp`**.
  * Nút "Sửa": Tăng từ `16sp` $\rightarrow$ **`17sp`**.
* **Sửa lỗi hiển thị chữ bị kéo dọc:**
  * Thẻ công việc phù hợp (Bước 6) trước đây sử dụng `LinearLayout` chiều ngang khiến các nhãn giải thích tiêu chí (tags) khi vượt quá chiều rộng màn hình bị ép thành một cột hẹp chỉ vài pixel, làm chữ bị ngắt từng ký tự xếp dọc.
  * Đã chuyển sang bố cục **hàng dọc (`LinearLayout.VERTICAL`)** với biểu tượng dấu đầu dòng (`• tag`), bo góc nhẹ, nền xanh nhạt, chiều rộng phủ toàn thẻ (`MATCH_PARENT`). Đảm bảo mọi giải thích tiêu chí hiển thị trọn vẹn, không bao giờ bị cắt xén hay đè chữ.

### 3.2. Trang chủ Người tìm việc (`RoleHomePanel.java`)
* Phóng to các nút lựa chọn chính của người tìm việc:
  * `"Giúp tôi tìm việc"`
  * `"Việc làm sắp tới"` (thẻ trạng thái bo tròn kèm biểu tượng và bộ đếm)
  * `"Việc của tôi"`
  * `"Xem các việc khác"`
  * Chiều cao tối thiểu: Tăng lên **`80dp`**.
  * Kích thước chữ: Tăng lên **`20sp`**, in đậm (`Typeface.BOLD`).
* Giữ nguyên nút `"Liên hệ tổng đài"` ở kích thước chuẩn `72dp` để tạo phân cấp rõ ràng giữa hành động chính và chức năng hỗ trợ.

### 3.3. Trạng thái nút "Đã nộp đơn" (`PublicJobsActivity.java`)
* Khi người dùng đã ứng tuyển vào công việc, nút chuyển sang trạng thái:
  * **Nền màu xanh lá thương hiệu** (`button_primary`).
  * **Chữ màu trắng** in đậm.
  * Thay vì kiểu nền trắng chữ xanh trước đây, giúp thể hiện trạng thái thành công rõ ràng, tương phản tốt theo chuẩn tiếp cận.

---

## 4. Bảng Tổng Hợp Files Chỉnh Sửa

| STT | File | Thay đổi chính |
|---|---|---|
| 1 | `app/src/main/res/values/strings.xml` | Cập nhật hint điện thoại, ghi chú SMS, ghi chú an tâm khi reCAPTCHA. |
| 2 | `app/src/main/res/values/public_job_strings.xml` | Đổi nhãn `pb_guest` & `pb_open` sang `"Xem trước công việc"`. |
| 3 | `app/src/main/res/values/navigation_strings.xml` | Đổi `n_sessions` sang `"Việc làm sắp tới"`, `n_help` sang `"Liên hệ tổng đài"`. |
| 4 | `app/src/main/res/drawable/button_sign_out.xml` | Tạo drawable viên thuốc viền đỏ, nền hồng nhạt `#FFF5F5`. |
| 5 | `app/src/main/res/drawable/ic_phone_support.xml` | Icon vector điện thoại hỗ trợ tổng đài. |
| 6 | `app/src/main/res/drawable/ic_settings.xml` | Icon vector bánh răng cài đặt hỗ trợ tiếp cận. |
| 7 | `app/src/main/res/layout/dialog_call_center.xml` | Giao diện hộp thoại Liên hệ tổng đài với 4 liên hệ và nút gọi. |
| 8 | `CallCenterDialog.java` | Lớp logic hiển thị hộp thoại tổng đài và kích hoạt `Intent.ACTION_DIAL`. |
| 9 | `role_home_content.xml` | Thiết kế thẻ bo tròn "Việc làm sắp tới" với icon sổ tay checklist và 2 tầng chữ. |
| 10 | `RoleHomePanel.java` | Kết nối CallCenterDialog; cập nhật dữ liệu thẻ "Việc làm sắp tới"; ẩn nút ca làm Employer; phóng to nút Seeker. |
| 11 | `ProfilePanel.java` | Thêm `AlertDialog` xác nhận đăng xuất an toàn; dọn dẹp các nút phụ. |
| 12 | `WorkSessionsActivity.java` | Đổi tiêu đề sang `"Việc làm sắp tới"`; chỉ hiển thị ca đã xác nhận 2 bên. |
| 13 | `PublicJobsActivity.java` | Tăng font chi tiết lên `17sp`; nút `"Đã nộp đơn"` nền xanh chữ trắng. |
| 14 | `WalletTransactionAdapter.java` | Lược bỏ ID kỹ thuật dài dòng, chỉ giữ tiêu đề giao dịch sạch. |
| 15 | `JobPostActivity.java` | Phân nhóm 3 tabs ("Đang đăng", "Đã xong", "Bản nháp"); nạp nháp từ JobDraftStore; sửa lỗi mất chữ tab. |
| 16 | `GuidedSearchActivity.java` | Bổ sung nút Hủy tìm; tăng font tóm tắt; fix bug chữ kéo dọc ở tags kết quả. |
| 17 | `activity_job_posts.xml` | Thêm thanh 3 tab lọc tin người thuê; căn chỉnh padding tránh cắt mép chữ. |
| 18 | `activity_guided_search.xml` | Thêm `gsBtnCancel` ở Bước 1. |
| 19 | `profile_content.xml` | Ẩn Help cũ; nút Đăng xuất đỏ; icon Cài đặt tiếp cận. |
| 20 | `role_account_content.xml` | Ẩn Ví tiền trùng lặp; nút Đăng xuất đỏ. |
| 21 | `AuthRepository.java` | Cung cấp `public final String uid` trong `Account`. |

---

## 5. Kết Quả Kiểm Tra (Verification)
* **Biên dịch Gradle:** `.\gradlew.bat assembleDebug` thành công 100% không lỗi (`BUILD SUCCESSFUL in 37s`).
* **Triển khai thiết bị thực:** Cài đặt thành công qua ADB trên thiết bị thực `adb-7L8XDIFU75JBMJBI-X00rBg` (`Success`).
* **Kiểm tra trực quan (Screenshots):**
  * Thẻ "Việc làm sắp tới" hiển thị chuẩn xác icon checklist, dòng tiêu đề in đậm `20sp` và dòng phụ `Chưa có việc sắp tới` / `Có N việc sắp tới`.
  * Các tab lọc tin người thuê "Đang đăng", "Đã xong", "Bản nháp" hiển thị đầy đủ 100% chữ tiếng Việt, không bị co ép hay mất mép.
  * Các nút điều hướng và chuyển màn hình hoạt động trơn tru, không có hiện tượng văng ứng dụng hay lỗi runtime.
