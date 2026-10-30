# KỊCH BẢN KIỂM THỬ (TEST CASES) TOÀN DIỆN VÀ MÔ TẢ DÒNG DỮ LIỆU (DATA FLOW) HỆ THỐNG GRANDMAASSIST

> **Dự án:** GrandMaAssist — Hệ thống kết nối việc làm và hỗ trợ toàn diện cho người cao tuổi  
> **Phiên bản:** v3.0 (Final Release)  
> **Mục đích tài liệu:** Cung cấp kịch bản kiểm thử chi tiết từng bước (Test Scenarios) cho 10 luồng nghiệp vụ cốt lõi, đồng thời **bóc tách chính xác 100% dòng dữ liệu di chuyển (Data Flow)** từ lúc người dùng tương tác trên UI, qua tầng ViewModel, xử lý nghiệp vụ tại Repository/SQLite, kiểm soát qua Firebase Security Rules, cho đến khi máy chủ phản hồi và cập nhật trạng thái hiển thị.

---

## MỤC LỤC 10 LUỒNG NGHIỆP VỤ CỐT LÕI

1. [TC01: Đăng nhập xác thực người dùng (Google One-Tap qua Credential Manager & Phone OTP SMS)](#tc01-đăng-nhập-xác-thực-người-dùng-google-one-tap-qua-credential-manager--phone-otp-sms)
2. [TC02: Phân luồng vai trò & Khởi tạo hồ sơ ban đầu (Người tìm việc SEEKER vs Người thuê EMPLOYER)](#tc02-phân-luồng-vai-trò--khởi-tạo-hồ-sơ-ban-đầu-người-tìm-việc-seeker-vs-người-thuê-employer)
3. [TC03: Xác thực danh tính & Thẩm định hồ sơ hưu trí (Chụp ảnh CCCD, ML Kit đọc QR trên máy, upload hồ sơ)](#tc03-xác-thực-danh-tính--thẩm-định-hồ-sơ-hưu-trí-chụp-ảnh-cccd-ml-kit-đọc-qr-trên-máy-upload-hồ-sơ)
4. [TC04: Người thuê Tạo mới & Đăng tin tuyển dụng (Kèm ảnh chụp nơi làm việc Base64, bộ đệm nháp SQLite, ước tính phí sàn 10%)](#tc04-người-thuê-tạo-mới--đăng-tin-tuyển-dụng-kèm-ảnh-chụp-nơi-làm-việc-base64-bộ-đệm-nháp-sqlite-ước-tính-phí-sàn-10)
5. [TC05: Quản trị viên (Admin) Xét duyệt Hồ sơ CCCD & Thẩm định Tin tuyển dụng (Dashboard thống kê 4 box, bộ lọc Chưa/Đã duyệt)](#tc05-quản-trị-viên-admin-xét-duyệt-hồ-sơ-cccd--thẩm-định-tin-tuyển-dụng-dashboard-thống-kê-4-box-bộ-lọc-chưađã-duyệt)
6. [TC06: Người cao tuổi Tìm kiếm việc làm & Nộp đơn ứng tuyển (Thuật toán gợi ý 4 bước GuidedJobMatcher, việc đã lưu SQLite, nộp/rút đơn)](#tc06-người-cao-tuổi-tìm-kiếm-việc-làm--nộp-đơn-ứng-tuyển-thuật-toán-gợi-ý-4-bước-guidedjobmatcher-việc-đã-lưu-sqlite-nộprút-đơn)
7. [TC07: Đàm phán Thỏa thuận Ca làm việc & Ký quỹ an toàn (Thương lượng ca 2 bên, xác nhận phiên bản expectedVersion, cọc tiền vào quỹ tạm giữ)](#tc07-đàm-phán-thỏa-thuận-ca-làm-việc--ký-quỹ-an-toàn-thương-lượng-ca-2-bên-xác-nhận-phiên-bản-expectedversion-cọc-tiền-vào-quỹ-tạm-giữ)
8. [TC08: Vận hành Ca làm việc, Điểm danh 2 chiều (Check-in), Báo thức ca làm (AlarmManager) & Quyết toán tự động 10% phí sàn](#tc08-vận-hành-ca-làm-việc-điểm-danh-2-chiều-check-in-báo-thức-ca-làm-alarmmanager--quyết-toán-tự-động-10-phí-sàn)
9. [TC09: Trợ năng cao cấp: Giọng đọc nhân tạo TTS, Tùy chỉnh Cỡ chữ (100% - 115% - 130%) & Xem trước trực quan](#tc09-trợ-năng-cao-cấp-giọng-đọc-nhân-tạo-tts-tùy-chỉnh-cỡ-chữ-100---115---130--xem-trước-trực-quan)
10. [TC10: Tích hợp Bản đồ chỉ đường Google Maps Intent Navigation (Từ vị trí hiện tại đến địa điểm làm việc)](#tc10-tích-hợp-bản-đồ-chỉ-đường-google-maps-intent-navigation-từ-vị-trí-hiện-tại-đến-địa-điểm-làm-việc)

---

## TC01: ĐĂNG NHẬP XÁC THỰC NGƯỜI DÙNG (GOOGLE ONE-TAP QUA CREDENTIAL MANAGER & PHONE OTP SMS)

### 1. Thông tin Test Case
- **Mã kiểm thử:** `TC_AUTH_01`
- **Mục tiêu:** Xác thực người dùng an toàn vào hệ sinh thái Firebase bằng phương thức hiện đại 1-chạm (Google Sign-In) hoặc số điện thoại viễn thông thật (SMS OTP 6 số).
- **Mức độ ưu tiên:** Critical (P0)
- **Điều kiện tiên quyết:** Thiết bị có kết nối Internet (Wifi/4G); Cửa hàng Google Play Services hoạt động; SIM nhận được tin nhắn SMS (nếu test Phone Auth).

### 2. Kịch bản kiểm thử (Test Scenario Steps)
1. Khởi chạy ứng dụng GrandMaAssist.
2. Tại màn hình Đăng nhập:
   - **Nhánh A (Google):** Nhấn nút *"Đăng nhập bằng Google"*. Hộp thoại chọn tài khoản Google của Android Credential Manager hiển thị $\rightarrow$ Chọn tài khoản $\rightarrow$ Hoàn tất xác thực.
   - **Nhánh B (Số điện thoại):** Nhấn *"Sử dụng số điện thoại"*, nhập số điện thoại định dạng Việt Nam (VD: `0912345678`) $\rightarrow$ Nhấn *"Gửi mã xác thực"* $\rightarrow$ Chờ SMS OTP $\rightarrow$ Nhập mã 6 chữ số $\rightarrow$ Nhấn *"Xác nhận"*.
3. Quan sát màn hình tiếp theo: Người dùng được đưa vào màn hình "Chọn vai trò" (nếu là user mới) hoặc "Trang chủ tương ứng" (nếu đã có hồ sơ).

### 3. Dữ liệu thử nghiệm mẫu
- Tài khoản Google: `testuser@gmail.com`
- Số điện thoại test: `+84912345678`, OTP: `123456` (trong môi trường Phone Auth Test) hoặc OTP thật qua mạng Mobifone/Viettel/Vinaphone.

### 4. Kết quả mong đợi
- Đăng nhập thành công không văng ứng dụng (Zero crash).
- Quá trình đăng nhập lưu lại phiên làm việc an toàn (`FirebaseAuth.getCurrentUser() != null`).

### 5. MÔ TẢ CHI TIẾT DÒNG DỮ LIỆU DI CHUYỂN (DETAILED DATA FLOW)

```text
[UI: Nút Google / SĐT] 
         │ (1) User Interaction
         ▼
[MainActivity / AccountMethodsFlow] 
         │ (2) Gọi CredentialManager.getCredentialAsync() hoặc PhoneAuthProvider.verifyPhoneNumber()
         ▼
[Google Play Services & Firebase Auth Server] 
         │ (3) Xác thực chữ ký IdToken hoặc gửi SMS OTP viễn thông
         ▼
[AuthRepository: signInWithCredential()] 
         │ (4) FirebaseAuth.getInstance().signInWithCredential()
         ▼
[Firebase Auth Core] ──> Tạo/Trả về FirebaseUser (UID, phone_number, email)
         │ (5) Token Claims Callback
         ▼
[ProfileRepository.load(uid)] 
         │ (6) Đọc Firestore: adminAccess/{uid} & users/{uid}
         ▼
[ProfileViewModel / UI Render] ──> Điều hướng: ADMIN / SEEKER / EMPLOYER / ROLE_SELECTION
```

- **(A) UI Input & View:** Người dùng chạm vào nút Google hoặc nhập text vào `EditText` SĐT (`authPhoneInput`). Khi bấm Gửi OTP, nút chuyển sang trạng thái disabled kèm ProgressBar xoay tròn để chống spam nhấn nhiều lần.
- **(B) ViewModel & Validation:** `AuthViewModel` kiểm tra số điện thoại hợp lệ theo biểu thức chính quy E.164 (`+84xxxxxxxxx`). Nếu không đúng 10 số đầu 0x, hiển thị lỗi đỏ: *"Số điện thoại không hợp lệ"*.
- **(C) Repository & Data Layer Execution:**
  - Đối với Google: Gọi `CredentialManager.getCredentialAsync()`. Sau khi người dùng chọn tài khoản, lấy được `CustomCredential` mang Type `GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL`. Trích xuất chuỗi `idToken` và gọi `GoogleAuthProvider.getCredential(idToken, null)`.
  - Đối với Phone: `PhoneSignInFlow.java` đăng ký callback `OnVerificationStateChangedCallbacks`. Gửi yêu cầu qua `PhoneAuthProvider.verifyPhoneNumber()`. Khi người dùng nhập đủ 6 số OTP, gọi `PhoneAuthProvider.getCredential(verificationId, code)`.
  - Gọi `auth.signInWithCredential(credential)`.
- **(D) Firestore Rules & Server Processing:** Phía Firebase Auth phát hành `IdToken` kèm các claim (`sub=uid`, `phone_number`). `ProfileRepository.withProof(uid)` gọi `user.getIdToken(true)` để verify token còn hiệu lực trước khi truy vấn Firestore.
- **(E) Callback, Local Sync & UI Feedback:** Task hoàn tất thành công (`addOnSuccessListener`). `ProfileRepository.load(uid)` kích hoạt kiểm tra xem UID đã có hồ sơ chưa. Trả về `Snapshot` $\rightarrow$ `ProfileViewModel` cập nhật `state.screen` $\rightarrow$ `MainActivity` thực hiện chuyển cảnh sang màn hình tiếp theo mượt mà.

---

## TC02: PHÂN LUỒNG VAI TRÒ & KHỞI TẠO HỒ SƠ BAN ĐẦU (NGƯỜI TÌM VIỆC SEEKER VS NGƯỜI THUÊ EMPLOYER)

### 1. Thông tin Test Case
- **Mã kiểm thử:** `TC_ROLE_01`
- **Mục tiêu:** Kiểm tra quy trình người dùng mới chọn vai trò lần đầu, thiết lập thông tin cơ bản và lưu trữ nhất quán vào Firestore.
- **Mức độ ưu tiên:** High (P1)
- **Điều kiện tiên quyết:** Tài khoản mới đăng nhập lần đầu, chưa tồn tại trong collection `users`.

### 2. Kịch bản kiểm thử (Test Scenario Steps)
1. Đăng nhập tài khoản mới $\rightarrow$ Xuất hiện màn hình "Chọn vai trò của bạn".
2. Thử nghiệm **Nhánh 1 (Người tìm việc - SEEKER):**
   - Chọn thẻ *"Tôi là Người cao tuổi tìm việc"*.
   - Nhập Họ và tên: *"Bác Nguyễn Văn An"*.
   - Chọn Khu vực sinh sống: *"Quận 1"*.
   - Chọn các ca làm việc ưu tiên: Sáng, Chiều.
   - Chọn kỹ năng: *"Nấu ăn, Trông nhà"*.
   - Nhấn *"Hoàn tất đăng ký"*.
3. Thử nghiệm **Nhánh 2 (Người thuê - EMPLOYER):**
   - Chọn thẻ *"Tôi là Người thuê / Hộ gia đình"*.
   - Chọn Loại người thuê: *"Hộ gia đình"*.
   - Nhập Tên liên hệ / Đại diện: *"Chị Trần Mai"*.
   - Nhập Giới thiệu ngắn: *"Cần người phụ nấu ăn gia đình vào các buổi sáng"*.
   - Nhấn *"Hoàn tất đăng ký"*.

### 3. Dữ liệu thử nghiệm mẫu
- Role: `SEEKER` / `EMPLOYER`
- Khu vực: `Q01` (Quận 1, TP.HCM)
- DisplayName: Không được để trống, độ dài từ 2 đến 50 ký tự.

### 4. Kết quả mong đợi
- Giao diện phản hồi nhanh, hiển thị thông báo chào mừng.
- Trang chủ hiển thị đúng giao diện đặc thù của vai trò đã chọn.
- Dữ liệu lưu vào Firestore đúng cấu trúc schema và quan hệ 1-1 giữa `users` và profile tương ứng.

### 5. MÔ TẢ CHI TIẾT DÒNG DỮ LIỆU DI CHUYỂN (DETAILED DATA FLOW)

- **(A) UI Input & View:** Người dùng chọn Radio Button hoặc Card vai trò $\rightarrow$ Giao diện mở rộng các trường nhập liệu tương ứng. Khi nhấn nút Submit, giao diện khóa bàn phím và kích hoạt cờ `vm.busy = true`.
- **(B) ViewModel & Validation:** `ProfileViewModel.submitRoleSelection()` kiểm tra:
  - `displayName` không được rỗng và không chứa ký tự đặc biệt nguy hiểm.
  - `areaCode` phải thuộc danh sách quận hợp lệ lấy từ collection `appConfig/areas`.
  - Đối với `SEEKER`: Phải chọn ít nhất 1 ca làm việc (`morning`, `afternoon`, hoặc `evening`).
- **(C) Repository & Data Layer Execution:**
  - `ProfileRepository.java` thực thi giao dịch trực tuyến qua `db.runTransaction(tx -> { ... })`.
  - Giao dịch thực hiện ghi đồng thời 2 văn bản:
    1. `tx.set(users/{uid}, userMap)`: Tạo văn bản người dùng với các trường `id`, `role`, `accountStatus = "ACTIVE"`, `displayName`, `phoneE164`, `version = 1`, `createdAt = serverTimestamp`.
    2. `tx.set(jobSeekerProfiles/{uid}` hoặc `employerProfiles/{uid}, profileMap)`: Lưu các thông tin chi tiết về ca làm, kỹ năng, khu vực, `identityProfileVersion = 0`.
- **(D) Firestore Rules & Server Processing:**
  - Rule kiểm tra: `request.auth != null && request.auth.uid == uid`.
  - Kiểm tra `role` gửi lên phải là một trong hai giá trị `"SEEKER"` hoặc `"EMPLOYER"` (Không cho phép client tự gửi `"ADMIN"`).
  - Giao dịch được Firestore commit an toàn xuống máy chủ đám mây.
- **(E) Callback, Local Sync & UI Feedback:** Task thành công $\rightarrow$ Repository trả về đối tượng `Snapshot` mới. ViewModel phát tín hiệu LiveData/State cập nhật `state.screen = Screen.SEEKER_HOME` (hoặc `EMPLOYER_HOME`). Giao diện tự động chuyển cảnh vào trang chủ của vai trò đó.

---

## TC03: XÁC THỰC DANH TÍNH & THẨM ĐỊNH HỒ SƠ HƯU TRÍ (CHỤP ẢNH CCCD, ML KIT ĐỌC QR TRÊN MÁY, UPLOAD HỒ SƠ)

### 1. Thông tin Test Case
- **Mã kiểm thử:** `TC_VERIFY_01`
- **Mục tiêu:** Đảm bảo người cao tuổi có thể chụp ảnh CCCD, trí tuệ nhân tạo On-Device ML Kit tự động quét bóc tách mã QR để điền thông tin, tải ảnh lên Cloud Storage và nộp hồ sơ chờ Admin duyệt.
- **Mức độ ưu tiên:** Critical (P0)
- **Điều kiện tiên quyết:** Đã đăng nhập vai trò `SEEKER`; Thiết bị có camera hoạt động tốt hoặc có sẵn ảnh mẫu CCCD gắn chip trong máy.

### 2. Kịch bản kiểm thử (Test Scenario Steps)
1. Tại Trang chủ SEEKER, nhấn vào thẻ thông báo *"Xác minh hồ sơ để bắt đầu nhận việc"*.
2. Nhấn nút *"Chụp ảnh mặt trước CCCD"* (hoặc *"Chọn ảnh từ thư viện"*).
3. Sau khi ảnh được chọn, hệ thống tự động kích hoạt `CardQrReader` chạy ngầm.
4. Quan sát: Các trường Số CCCD, Họ tên, Ngày sinh tự động được trích xuất và hiển thị lên màn hình.
5. Tiếp tục chụp/chọn *"Ảnh mặt sau CCCD"* và *"Ảnh minh chứng hưu trí / Thẻ hưu trí / Sổ BHXH"*.
6. Kiểm tra lại thông tin $\rightarrow$ Nhấn nút *"Nộp hồ sơ thẩm định"*.
7. Quan sát trạng thái: Chuyển sang *"Đang chờ duyệt (SUBMITTED)"*.

### 3. Dữ liệu thử nghiệm mẫu
- Ảnh CCCD chứa mã QR chuẩn của Bộ Công an: Cấu trúc chuỗi QR `SốCCCD|SốCMNDCũ|HọVàTên|NgàySinh|GiớiTính|ĐịaChỉ|NgàyCấp`.
- Ảnh minh chứng hưu trí định dạng JPEG/PNG.

### 4. Kết quả mong đợi
- ML Kit nhận diện QR trong vòng dưới 1 giây.
- Ứng dụng không bao giờ bị Crash hay rò rỉ đường dẫn file nhạy cảm.
- Hồ sơ được gửi lên collection `retirementRequests` với trạng thái `SUBMITTED`.

### 5. MÔ TẢ CHI TIẾT DÒNG DỮ LIỆU DI CHUYỂN (DETAILED DATA FLOW)

```text
[Camera / Thư viện ảnh] 
         │ (1) Trả về Uri ảnh
         ▼
[VerificationActivity: completeImageRequest()] 
         │ (2) Nén ảnh, tạo byte[] an toàn trong bộ nhớ RAM
         ▼
[CardQrReader.readQrFromBitmap(bitmap)] 
         │ (3) Google ML Kit BarcodeScanning xử lý On-Device
         ▼
[Bóc tách thông tin: Số CCCD, Tên, Ngày sinh] 
         │ (4) Tự động điền vào VerificationDraft
         ▼
[VerificationRepository.submitRetirementRequest()] 
         │ (5) Upload ảnh lên Firebase Storage & Giao dịch Firestore
         ▼
[Firestore: retirementRequests/{requestId}] ──> Lưu request status = "SUBMITTED"
         │ (6) Cập nhật users/{uid}.retirementStatus = "SUBMITTED"
         ▼
[UI Feedback] ──> Hiển thị nhãn "Hồ sơ đang chờ duyệt"
```

- **(A) UI Input & View:** Người dùng kích hoạt camera qua `ActivityResultContracts.TakePicture()` hoặc chọn ảnh qua `GetContent()`. Ảnh được hiển thị tức thời lên `ZoomDocumentView` cho người dùng xem trước và kiểm tra độ nét.
- **(B) ViewModel & Validation:** `VerificationViewModel` nhận `Uri`, chuyển đổi thành Bitmap. Kiểm tra dung lượng ảnh, tự động scale kích thước tối đa 1920x1080 để tối ưu băng thông mà vẫn giữ rõ chữ số trên CCCD.
- **(C) Repository & Data Layer Execution:**
  - `CardQrReader.java` tạo đối tượng `InputImage.fromBitmap(bitmap, 0)`.
  - Khởi tạo `BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()`.
  - Khi phát hiện mã QR, thuật toán phân tách chuỗi ký tự bằng dấu phân cách gạch đứng `|` để lấy chính xác: `idNumber`, `fullName`, `dateOfBirth`.
  - Khi nộp hồ sơ, `VerificationRepository` gọi `FirebaseStorage.getInstance().getReference("verifications/" + uid + "/...")` để tải ảnh lên.
  - Sử dụng Firestore Transaction: Tạo document mới trong `retirementRequests` với ID duy nhất, lưu `status: "SUBMITTED"`, đường dẫn Storage, và cập nhật trường `users/{uid}.retirementStatus = "SUBMITTED"`.
- **(D) Firestore Rules & Server Processing:** Security Rules kiểm tra người dùng chỉ được tạo request cho chính UID của mình (`request.resource.data.userId == request.auth.uid`), trạng thái ban đầu bắt buộc là `"SUBMITTED"`, không được tự ý ghi `"APPROVED"`.
- **(E) Callback, Local Sync & UI Feedback:** Transaction commit thành công $\rightarrow$ ViewModel nhận thông báo $\rightarrow$ Trạng thái trên UI cập nhật từ `NOT_SUBMITTED` sang `SUBMITTED`, vô hiệu hóa các nút chỉnh sửa để chờ Admin thẩm định.

---

## TC04: NGƯỜI THUÊ TẠO MỚI & ĐĂNG TIN TUYỂN DỤNG (KÈM ẢNH CHỤP NƠI LÀM VIỆC BASE64, BỘ ĐỆM NHÁP SQLITE, ƯỚC TÍNH PHÍ SÀN 10%)

### 1. Thông tin Test Case
- **Mã kiểm thử:** `TC_JOB_POST_01`
- **Mục tiêu:** Cho phép Người thuê soạn thảo tin tuyển dụng, lưu nháp ngoại tuyến vào SQLite, đính kèm ảnh môi trường làm việc thực tế, tính toán minh bạch tiền công và 10% phí sàn, gửi duyệt lên hệ thống.
- **Mức độ ưu tiên:** High (P1)
- **Điều kiện tiên quyết:** Đã đăng nhập vai trò `EMPLOYER` (đã được xác minh danh tính).

### 2. Kịch bản kiểm thử (Test Scenario Steps)
1. Tại Trang chủ EMPLOYER, nhấn nút *"Đăng tin tuyển dụng mới"* (hoặc biểu tượng dấu cộng `+`).
2. Chọn Danh mục công việc: *"Phụ bếp"* (hoặc Dọn dẹp nhà, Giữ trẻ, Chăm vườn...).
3. Nhập Tiêu đề tin: *"Tìm cô phụ bếp nấu ăn bữa trưa cho gia đình"*.
4. Nhập Mô tả công việc, các yêu cầu nhẹ nhàng phù hợp người lớn tuổi.
5. Chọn Ngày làm việc và Ca làm việc (Ví dụ: Ca Sáng 08:00 - 11:30).
6. Nhập Mức tiền công: Nhập `250000` (250.000 VNĐ).
   - Quan sát mục Ước tính chi phí: Hệ thống tự động tính Phí dịch vụ sàn 10% = `25.000đ`, Tổng chi phí = `275.000đ`.
7. Nhập Địa chỉ nơi làm việc: *"Số 123 Nguyễn Thị Minh Khai"*, chọn Quận: *"Quận 3"*.
8. Tại khối **"Ảnh nơi làm việc thực tế"**: Bấm *"Thêm ảnh nơi làm việc"* $\rightarrow$ Chọn một bức ảnh chụp gian bếp/phòng khách $\rightarrow$ Ảnh hiển thị xem trước trực quan.
9. Thoát ứng dụng đột ngột (hoặc tắt mạng) $\rightarrow$ Mở lại: Tin vẫn còn nguyên vẹn trong tab *"Bản nháp"* nhờ SQLite `JobDraftStore`.
10. Nhấn nút *"Gửi duyệt tin"*.

### 3. Dữ liệu thử nghiệm mẫu
- CategoryId: `phu-bep`, AreaCode: `Q03`.
- Tiền công (`wage`): 250.000đ. Phí sàn (`serviceFee` 10%): 25.000đ.
- Workplace Image: Ảnh nén Base64 (chuỗi String an toàn) hoặc URL.

### 4. Kết quả mong đợi
- Bản nháp lưu tức thời vào SQLite không bị mất khi thoát ngang.
- Công thức tính phí 10% hiển thị trực quan, chính xác từng số tiền.
- Tin đăng được gửi lên collection `jobs` với trạng thái `PENDING_REVIEW` để chờ Admin duyệt.

### 5. MÔ TẢ CHI TIẾT DÒNG DỮ LIỆU DI CHUYỂN (DETAILED DATA FLOW)

- **(A) UI Input & View:** Người dùng nhập các trường vào `JobDraftActivity`. Mỗi khi thay đổi chữ trong `EditText` hoặc thay đổi mức lương, `TextWatcher` kích hoạt tính toán tự động. Người dùng bấm chọn ảnh nơi làm việc $\rightarrow$ `ActivityResultContracts.GetContent()` trả về Uri $\rightarrow$ `ImageHelper` nén ảnh thành chuỗi Base64 và gán vào `ImageView` xem trước.
- **(B) ViewModel & Validation:** `JobDraftViewModel` kiểm tra: Tiêu đề không rỗng, tiền công $\ge 50.000đ$, ngày làm không được ở quá khứ, địa chỉ cụ thể không để trống. Tự động lưu bản nháp vào SQLite nội bộ qua `JobDraftStore.saveDraft()`.
- **(C) Repository & Data Layer Execution:**
  - Khi bấm "Gửi duyệt tin", `JobPostRepository.createPost(draft)` được gọi.
  - Repository chuẩn bị Map dữ liệu với các trường: `title`, `description`, `categoryId`, `areaCode`, `addressDetail`, `wage`, `serviceFee = wage * 0.1`, `totalEstimated = wage + serviceFee`, `workplaceImageBase64`, `status = "PENDING_REVIEW"`, `employerId = uid`, `createdAt = serverTimestamp`.
  - Thực thi ghi vào Firestore: `db.collection("jobs").document(jobId).set(jobData)`.
  - Xóa bản nháp tương ứng trong SQLite `JobDraftStore.deleteDraft(draftId)`.
- **(D) Firestore Rules & Server Processing:** Rules kiểm tra người gửi tin có `role == "EMPLOYER"`, trạng thái phải bắt đầu là `"PENDING_REVIEW"`, trường `employerId` phải khớp với `request.auth.uid`. Máy chủ Firestore lưu tin thành công.
- **(E) Callback, Local Sync & UI Feedback:** Task hoàn thành $\rightarrow$ Activity nhận `RESULT_OK`, hiển thị Toast thông báo: *"Tin tuyển dụng đã được gửi duyệt thành công. Vui lòng chờ quản trị viên phê duyệt"*. Điều hướng người dùng về màn hình danh sách tin tuyển dụng.

---

## TC05: QUẢN TRỊ VIÊN (ADMIN) XÉT DUYỆT HỒ SƠ CCCD & THẨM ĐỊNH TIN TUYỂN DỤNG (DASHBOARD THỐNG KÊ 4 BOX, BỘ LỌC CHƯA/ĐÃ DUYỆT)

### 1. Thông tin Test Case
- **Mã kiểm thử:** `TC_ADMIN_01`
- **Mục tiêu:** Kiểm tra Dashboard quản trị hiển thị chính xác các chỉ số thời gian thực, bộ đếm hàng đợi, bộ lọc trạng thái và khả năng phê duyệt/từ chối hồ sơ người dùng và tin tuyển dụng.
- **Mức độ ưu tiên:** Critical (P0)
- **Điều kiện tiên quyết:** Đăng nhập bằng tài khoản có quyền Admin (đã thiết lập đủ `adminAccess` và `users.role = "ADMIN"`).

### 2. Kịch bản kiểm thử (Test Scenario Steps)
1. Đăng nhập tài khoản Admin $\rightarrow$ Quan sát **Trang chủ Admin**:
   - 4 khối Dashboard: Khối Người thuê, Khối Người tìm việc, Khối Tin tuyển dụng, Khối Doanh thu phí sàn (đơn vị VNĐ).
   - 2 khối Hàng đợi: Khối "Tin cần duyệt" (có bộ đếm số tin chờ) và Khối "Hồ sơ chờ duyệt" (có bộ đếm số hồ sơ chờ).
2. **Kịch bản Thẩm định Hồ sơ CCCD:**
   - Chạm vào khối "Hồ sơ chờ duyệt" $\rightarrow$ Chuyển đến màn hình Thẩm định.
   - Thử nghiệm các nút bộ lọc ngang:
     - Bấm *"Chưa duyệt"*: Chỉ hiển thị danh sách hồ sơ trạng thái `SUBMITTED`.
     - Bấm *"Đã duyệt"*: Hiển thị danh sách hồ sơ đã được duyệt thành công (`APPROVED`/`VERIFIED`).
     - Bấm icon Cái phễu: Lọc theo Người cao tuổi hoặc Người thuê.
   - Chọn một hồ sơ chưa duyệt $\rightarrow$ Xem chi tiết ảnh CCCD, thông tin QR bóc tách $\rightarrow$ Chọn Đánh giá: *"Phù hợp"* $\rightarrow$ Bấm *"Chấp thuận"* $\rightarrow$ Xác nhận duyệt.
3. **Kịch bản Thẩm định Tin tuyển dụng:**
   - Quay lại Trang chủ Admin $\rightarrow$ Chạm vào khối "Tin cần duyệt" $\rightarrow$ Chuyển đến trang Duyệt tin.
   - Bấm chọn bộ lọc *"Chưa duyệt"* $\rightarrow$ Chọn một tin tuyển dụng đang chờ $\rightarrow$ Xem thông tin, địa chỉ, ảnh nơi làm việc thực tế $\rightarrow$ Bấm *"Duyệt tin"* (hoặc *"Từ chối"* có kèm lý do ngắn).

### 3. Dữ liệu thử nghiệm mẫu
- Admin UID hợp lệ.
- Hồ sơ CCCD đang ở trạng thái `SUBMITTED`. Tin đăng đang ở `PENDING_REVIEW`.

### 4. Kết quả mong đợi
- Các con số thống kê trên 4 box và 2 hàng đợi cập nhật chính xác theo dữ liệu máy chủ Firestore.
- Khi Admin bấm Duyệt, trạng thái của hồ sơ và tin đăng đổi sang `APPROVED` / `OPEN` ngay lập tức.
- Hồ sơ/tin đã duyệt tự động rời khỏi tab "Chưa duyệt" và chuyển sang tab "Đã duyệt".

### 5. MÔ TẢ CHI TIẾT DÒNG DỮ LIỆU DI CHUYỂN (DETAILED DATA FLOW)

- **(A) UI Input & View:** Admin mở Trang chủ hoặc bấm nút Reload ở góc trên bên phải. `RoleHomePanel` kích hoạt sự kiện tải dữ liệu thống kê. Admin bấm chuyển các tab bộ lọc $\rightarrow$ UI lọc danh sách tức thì mà không cần load lại từ đầu.
- **(B) ViewModel & Validation:** `ProfileViewModel` hoặc `VerificationViewModel` nhận diện vai trò Admin, gọi phương thức đếm tổng hợp từ Repository.
- **(C) Repository & Data Layer Execution:**
  - `ProfileRepository.adminCounts(uid)` gửi đồng thời các truy vấn đếm trực tiếp từ Server:
    - `db.collection("retirementRequests").whereEqualTo("status", "SUBMITTED").count().get(AggregateSource.SERVER)`
    - `db.collection("jobs").whereEqualTo("status", "PENDING_REVIEW").count().get(AggregateSource.SERVER)`
    - `db.collection("workSessions").whereEqualTo("status", "COMPLETED").get(Source.SERVER)` $\rightarrow$ Tính tổng doanh thu phí sàn `totalFee += session.settledServiceFee`.
  - Khi Admin duyệt hồ sơ: `VerificationRepository.approveRequest()` thực thi `db.runTransaction()`. Ghi đè trạng thái `retirementRequests/{id}.status = "APPROVED"` và cập nhật `users/{targetUid}.retirementStatus = "APPROVED"`.
  - Khi Admin duyệt tin: `JobPostRepository.adminApprovePost(jobId)` cập nhật `jobs/{jobId}.status = "OPEN"`, `reviewedAt = serverTimestamp()`.
- **(D) Firestore Rules & Server Processing:** Firestore Security Rules kiểm tra hàm `admin()`:
  - `function admin() { return request.auth != null && exists(/databases/$(database)/documents/adminAccess/$(request.auth.uid)) && get(/databases/$(database)/documents/adminAccess/$(request.auth.uid)).data.enabled == true; }`.
  - Chỉ khi hàm này trả về `true`, quyền cập nhật trạng thái của người khác và quyền đọc toàn bộ dữ liệu mới được Server cho phép.
- **(E) Callback, Local Sync & UI Feedback:** Transaction commit thành công $\rightarrow$ Giao diện Admin hiển thị Dialog thành công, tải lại danh sách hàng đợi, số lượng đếm trên Dashboard tự động giảm đi 1 tương ứng.

---

## TC06: NGƯỜI CAO TUỔI TÌM KIẾM VIỆC LÀM & NỘP ĐƠN ỨNG TUYỂN (THUẬT TOÁN GỢI Ý 4 BƯỚC GUIDEDJOBMATCHER, VIỆC ĐÃ LƯU SQLITE, NỘP/RÚT ĐƠN)

### 1. Thông tin Test Case
- **Mã kiểm thử:** `TC_SEEKER_JOB_01`
- **Mục tiêu:** Kiểm tra trải nghiệm tìm việc thân thiện cho người cao tuổi qua wizard 4 bước, lưu trữ tiêu chí vào SQLite ngoại tuyến, chấm điểm phù hợp bằng `GuidedJobMatcher`, xem chi tiết việc làm và gửi đơn ứng tuyển.
- **Mức độ ưu tiên:** Critical (P0)
- **Điều kiện tiên quyết:** Đã đăng nhập vai trò `SEEKER` (hồ sơ đã được phê duyệt `APPROVED`).

### 2. Kịch bản kiểm thử (Test Scenario Steps)
1. Tại Trang chủ SEEKER, nhấn *"Tìm việc phù hợp"* (Khởi động tìm việc 4 bước `GuidedSearchActivity`).
   - **Bước 1:** Chọn việc muốn làm (VD: Phụ bếp, Trông trẻ).
   - **Bước 2:** Chọn ngày và ca làm việc rảnh (VD: Ngày mai, Ca Sáng).
   - **Bước 3:** Chọn khu vực thuận tiện đi lại (VD: Quận 1, Quận 3).
   - **Bước 4:** Xác nhận điều kiện sức khỏe (Không bê vác nặng, Không leo cầu thang cao).
2. Nhấn *"Xem kết quả gợi ý"*.
3. Quan sát danh sách việc làm trả về:
   - Các công việc phù hợp nhất được xếp lên đầu kèm huy hiệu *"Phù hợp cao"*.
4. Bấm vào icon Trái tim để Lưu việc làm $\rightarrow$ Mở tab *"Việc đã lưu"* kiểm tra xem công việc đã được lưu vào SQLite `SavedJobStore` chưa.
5. Chạm vào một tin tuyển dụng để xem chi tiết:
   - Xem Tiêu đề, Mô tả, Lương, Địa chỉ, Ảnh môi trường làm việc thực tế do người thuê đăng tải.
6. Nhấn nút to rõ ràng *"Nộp đơn ứng tuyển"*.
7. Chuyển sang màn hình *"Đơn ứng tuyển của tôi"*: Kiểm tra đơn hiển thị trạng thái `PENDING`.
8. Thử nghiệm nút *"Rút đơn ứng tuyển"* $\rightarrow$ Trạng thái chuyển thành `WITHDRAWN`.

### 3. Dữ liệu thử nghiệm mẫu
- Tiêu chí tìm việc: Category `phu-bep`, Ca `MORNING`, Area `Q01`.
- Job Id hợp lệ đang có trạng thái `OPEN`.

### 4. Kết quả mong đợi
- Thuật toán `GuidedJobMatcher` tính toán điểm trọng số chuẩn xác ngay trên thiết bị.
- Đơn ứng tuyển được ghi nhận tức thì vào collection `applications`.
- Khi rút đơn, hệ thống cập nhật trạng thái hợp lệ, không phát sinh lỗi xung đột.

### 5. MÔ TẢ CHI TIẾT DÒNG DỮ LIỆU DI CHUYỂN (DETAILED DATA FLOW)

- **(A) UI Input & View:** Người cao tuổi chạm vào các ô tùy chọn kích thước lớn, màu sắc tương phản cao trong `GuidedSearchActivity`. Khi nộp đơn, ứng dụng bật hộp thoại xác nhận chữ to tránh bấm nhầm.
- **(B) ViewModel & Validation:** `GuidedSearchViewModel` lưu các lựa chọn của 4 bước vào `SearchPreferenceStore` (SQLite nội bộ). Truy vấn danh sách tin việc có trạng thái `OPEN` từ `PublicJobRepository` (hoặc cache `public_jobs.db` nếu ngoại tuyến).
- **(C) Repository & Data Layer Execution:**
  - `GuidedJobMatcher.java` duyệt qua từng công việc, tính toán điểm số dựa trên: Trùng danh mục (+40đ), Trùng ca làm (+30đ), Trùng khu vực (+20đ), Thỏa mãn sức khỏe (+10đ). Sắp xếp mảng công việc giảm dần theo điểm số.
  - Khi nộp đơn: `ApplicationRepository.submitApplication(jobId, seekerId)` tạo một document mới trong collection `applications` với ID cấu trúc `app_{jobId}_{seekerId}` để đảm bảo một người không thể nộp 2 đơn trùng lặp cho cùng 1 tin.
  - Khi rút đơn: `ApplicationRepository.withdrawApplication(applicationId)` cập nhật `status = "WITHDRAWN"`.
- **(D) Firestore Rules & Server Processing:** Security Rules kiểm tra: Chỉ người dùng có vai trò `SEEKER` đã qua duyệt (`retirementStatus == "APPROVED"`) mới được nộp đơn; `seekerId` phải bằng `request.auth.uid`; Tin tuyển dụng phải đang ở trạng thái `"OPEN"`.
- **(E) Callback, Local Sync & UI Feedback:** Đơn nộp thành công $\rightarrow$ Nút trên UI đổi thành *"Đã nộp đơn"*; Người thuê nhận được đơn trong danh sách ứng viên của tin tuyển dụng đó.

---

## TC07: ĐÀM PHÁN THỎA THUẬN CA LÀM VIỆC & KÝ QUỸ AN TOÀN (THƯƠNG LƯỢNG CA 2 BÊN, XÁC NHẬN PHIÊN BẢN EXPECTEDVERSION, CỌC TIỀN VÀO QUỸ TẠM GIỮ)

### 1. Thông tin Test Case
- **Mã kiểm thử:** `TC_SESSION_AGREEMENT_01`
- **Mục tiêu:** Kiểm tra cơ chế đàm phán thỏa thuận ca làm việc giữa 2 bên, bảo vệ tính toàn vẹn phiên bản đồng thuận (`expectedVersion`), và quy trình ký quỹ tiền cọc an toàn từ ví mô phỏng của người thuê.
- **Mức độ ưu tiên:** Critical (P0)
- **Điều kiện tiên quyết:** Người thuê đã chấp thuận đơn ứng tuyển của người tìm việc, tạo ra một Ca làm việc ở trạng thái `PROPOSED`.

### 2. Kịch bản kiểm thử (Test Scenario Steps)
1. Người thuê mở Ca làm việc $\rightarrow$ Bấm *"Chỉnh sửa thỏa thuận"* $\rightarrow$ Thay đổi giờ làm việc từ 08:00 thành 08:30 $\rightarrow$ Bấm *"Lưu và gửi lại thỏa thuận"*.
   - Quan sát: Số phiên bản thỏa thuận tăng lên (`agreementVersion = 2`).
2. Người cao tuổi mở màn hình Ca làm việc bên phía mình:
   - Nhìn thấy chi tiết thay đổi mới nhất.
   - Nhấn nút *"Đồng ý thỏa thuận"* (Xác nhận phiên bản 2).
   - Quan sát: Ca làm việc chuyển trạng thái sang *"Chờ nạp tiền cọc (AWAITING_FUNDS)"*.
3. Người thuê mở Ca làm việc:
   - Hiển thị thông báo: *"Người tìm việc đã đồng ý thỏa thuận. Vui lòng ký quỹ tiền công để chốt ca làm việc"*.
   - Nhấn nút *"Ký quỹ an toàn (Nạp cọc)"*.
   - Quan sát: Hệ thống tự động trừ tiền trong ví người thuê, ghi nhận cọc vào ca làm việc và chuyển trạng thái ca sang *"Sẵn sàng làm việc (READY)"*.

### 3. Dữ liệu thử nghiệm mẫu
- SessionId: `session_xxxxx`.
- Mức lương ca: 200.000đ. Phí sàn 10%: 20.000đ. Tổng cọc: 220.000đ.
- Ví người thuê có số dư khả dụng $\ge 220.000đ$.

### 4. Kết quả mong đợi
- Không xảy ra xung đột phiên bản (Version Conflict) khi hai bên cùng sửa đồng thời.
- Tiền cọc được trừ chính xác từ ví và tạm giữ an toàn trong Ca làm việc (`heldAmount = 220.000đ`).
- Trạng thái ca chuyển sang `READY`.

### 5. MÔ TẢ CHI TIẾT DÒNG DỮ LIỆU DI CHUYỂN (DETAILED DATA FLOW)

```text
[Người thuê: Sửa ca] ──> Tăng agreementVersion = 2
         │
         ▼
[Người tìm việc: Đồng ý ca] ──> Gửi expectedVersion = 2
         │
         ▼
[Firestore Transaction: seekerAccept()] 
         │ Kiểm tra: curVer == expectedVersion == 2?
         ▼
[Cập nhật workSessions/{id}.status = "AWAITING_FUNDS"]
         │
         ▼
[Người thuê: Bấm Ký quỹ cọc] 
         │
         ▼
[Firestore Transaction: fundSession()] 
         ├── Kiểm tra số dư ví người thuê: balance >= totalAmount?
         ├── Trừ tiền ví người thuê: balance = balance - totalAmount
         ├── Tạo transaction record: type = "PAYMENT_HOLD"
         └── Cập nhật workSessions/{id}: status = "READY", heldAmount = totalAmount
         │
         ▼
[UI Feedback] ──> Cả 2 bên nhận trạng thái "READY (Sẵn sàng làm việc)"
```

- **(A) UI Input & View:** Người dùng thao tác trên `ShiftAgreementEditActivity` và `ShiftAgreementDetailActivity`. Nút ký quỹ hiển thị rõ số tiền cọc cần trích từ ví.
- **(B) ViewModel & Validation:** `ShiftAgreementViewModel` kiểm tra số dư ví hiện tại qua `WalletRepository`. Nếu không đủ tiền, hiển thị thông báo hướng dẫn người thuê nạp thêm tiền mô phỏng vào ví trước khi cọc.
- **(C) Repository & Data Layer Execution:**
  - `WorkSessionRepository.seekerAccept(uid, sessionId, expectedVersion)`: Chạy trong Firestore Transaction. Đọc document ca làm việc, so sánh `agreementVersion` hiện tại trên server với `expectedVersion`. Nếu trùng khớp, cập nhật `seekerAcceptedVersion = expectedVersion` và chuyển status thành `AWAITING_FUNDS`.
  - `WorkSessionRepository.fundSession(uid, sessionId)`: Chạy giao dịch nguyên tử (ACID Transaction) đa văn bản:
    1. Đọc `workSessions/{sessionId}`: Kiểm tra trạng thái phải là `AWAITING_FUNDS`.
    2. Đọc `wallets/{employerId}`: Kiểm tra `balance >= totalAmount`.
    3. Cập nhật `wallets/{employerId}`: `balance = balance - totalAmount`.
    4. Tạo lịch sử giao dịch `wallets/{employerId}/transactions/tx_fund_{sessionId}`: `type = "PAYMENT_HOLD"`, `amount = totalAmount`.
    5. Cập nhật `workSessions/{sessionId}`: `status = "READY"`, `heldAmount = totalAmount`, `fundedAt = serverTimestamp()`.
- **(D) Firestore Rules & Server Processing:** Security Rules kiểm tra nghiêm ngặt: Không cho phép đổi trạng thái sang `READY` nếu không có giao dịch ký quỹ hợp lệ; người thực hiện ký quỹ bắt buộc phải là `employerId`.
- **(E) Callback, Local Sync & UI Feedback:** Giao dịch thành công $\rightarrow$ Cả hai bên đều nhận được cập nhật thời gian thực qua Realtime Snapshot Listener $\rightarrow$ Màn hình hiển thị nhãn xanh lá: *"Đã ký quỹ an toàn - Sẵn sàng làm việc"*.

---

## TC08: VẬN HÀNH CA LÀM VIỆC, ĐIỂM DANH 2 CHIỀU (CHECK-IN), BÁO THỨC CA LÀM (ALARMMANAGER) & QUYẾT TOÁN TỰ ĐỘNG 10% PHÍ SÀN

### 1. Thông tin Test Case
- **Mã kiểm thử:** `TC_WORK_SESSION_01`
- **Mục tiêu:** Kiểm tra toàn bộ vòng đời thực tế của ca làm việc: Nhận thông báo báo thức tự động trước ca làm việc bằng `AlarmManager`, điểm danh 2 chiều độc lập (Check-in), hoàn thành ca và giải ngân tiền công tự động trích 10% phí sàn.
- **Mức độ ưu tiên:** Critical (P0)
- **Điều kiện tiên quyết:** Ca làm việc đang ở trạng thái `READY`.

### 2. Kịch bản kiểm thử (Test Scenario Steps)
1. **Kiểm tra Báo thức ca làm việc:**
   - Khi ca làm việc chuyển sang `READY`, hệ thống tự động lập lịch báo thức qua `ShiftAlarmScheduler` trước giờ làm 30 phút.
   - Khi đến giờ hẹn: Xuất hiện thông báo Notification trên thanh trạng thái điện thoại với âm thanh và nội dung nhắc nhở rõ ràng: *"Bạn có ca làm việc sắp diễn ra trong 30 phút nữa"*.
2. **Kiểm tra Điểm danh 2 chiều (Check-in):**
   - Người cao tuổi đến nơi làm việc $\rightarrow$ Mở ca làm việc $\rightarrow$ Bấm *"Điểm danh vào ca"*.
   - Quan sát: Ghi nhận thời gian `seekerCheckedInAt`.
   - Người thuê mở ca làm việc $\rightarrow$ Bấm *"Xác nhận người làm đã đến"*.
   - Quan sát: Khi cả 2 bên cùng điểm danh, Ca làm việc tự động chuyển trạng thái sang *"Đang làm việc (IN_PROGRESS)"*.
3. **Kiểm tra Hoàn thành & Quyết toán tài chính:**
   - Hết giờ làm, Người cao tuổi bấm *"Báo cáo hoàn thành ca làm"*.
   - Trạng thái ca chuyển sang *"Chờ người thuê xác nhận (COMPLETION_PENDING)"*.
   - Người thuê bấm *"Xác nhận hoàn thành & Thanh toán"*.
   - Quan sát:
     - Ca làm việc chuyển trạng thái sang *"Đã hoàn tất (COMPLETED)"*.
     - Tiền công người làm (200.000đ) được cộng trực tiếp vào ví người cao tuổi.
     - Phí dịch vụ sàn 10% (20.000đ) được hệ thống ghi nhận vào tổng doanh thu phí sàn của Admin.

### 3. Dữ liệu thử nghiệm mẫu
- Ca làm việc trạng thái `READY`.
- Tiền công (`laborAmount`): 200.000đ. Phí sàn (`serviceFee` 10%): 20.000đ. Tiền cọc tạm giữ: 220.000đ.

### 4. Kết quả mong đợi
- Báo thức Notification hiển thị đúng hẹn, không bị trễ ngay cả khi thiết bị ở chế độ Doze Mode (ngủ sâu).
- Quy trình điểm danh 2 chiều minh bạch, ngăn chặn gian lận điểm danh khống.
- Tiền công giải ngân tức thì, phí sàn 10% được trích chuẩn xác, ví người tìm việc tăng đúng 200.000đ.

### 5. MÔ TẢ CHI TIẾT DÒNG DỮ LIỆU DI CHUYỂN (DETAILED DATA FLOW)

- **(A) UI Input & View:** Người dùng chạm các nút lớn *"Điểm danh vào ca"* và *"Xác nhận hoàn tất ca làm"* trên `WorkSessionsActivity`. Nút có hiệu ứng gợn sóng và hộp thoại xác nhận số tiền thanh toán rõ ràng.
- **(B) ViewModel & Validation:** `WorkSessionViewModel` kiểm tra logic ca làm việc, đảm bảo ca đang ở đúng trạng thái trước khi cho phép gọi Repository.
- **(C) Repository & Data Layer Execution:**
  - Lập lịch thông báo: `ShiftAlarmScheduler.scheduleShiftReminder(context, session)` tính toán thời gian `triggerAtMillis = shiftStart - 30 * 60 * 1000`. Gọi `AlarmManager.setExactAndAllowWhileIdle()`. Khi chuông reo, `ShiftReminderReceiver` dựng `NotificationCompat.Builder` bắn thông báo ra hệ thống Android.
  - Điểm danh: `WorkSessionRepository.confirmCheckIn(uid, sessionId, role)` trong Transaction. Ghi nhận `seekerCheckedInAt` hoặc `employerCheckedInAt`. Nếu cả 2 timestamp đều tồn tại $\rightarrow$ Cập nhật `status = "IN_PROGRESS"`.
  - Quyết toán tài chính: `WorkSessionRepository.confirmCompletionAndSettle(uid, sessionId)` thực thi giao dịch nguyên tử:
    1. Cập nhật `workSessions/{sessionId}`: `status = "COMPLETED"`, `paymentStatus = "SETTLED"`, `heldAmount = 0`, `settledLaborAmount = 200000`, `settledServiceFee = 20000`, `settledAt = serverTimestamp()`.
    2. Gọi `tryCreditSeekerWallet(session)`: Đọc `wallets/{seekerId}`, tính `newBalance = currentBalance + 200000`, ghi đè số dư mới và tạo bản ghi lịch sử `wallets/{seekerId}/transactions/tx_settle_{sessionId}` với `type = "PAYMENT_RECEIVED"`, `amount = 200000`.
- **(D) Firestore Rules & Server Processing:** Rules đảm bảo chỉ Người thuê mới có quyền gọi hoàn tất ca và giải ngân tiền; số tiền công giải ngân và phí sàn trích xuất phải khớp chính xác với thỏa thuận ban đầu đã ký quỹ.
- **(E) Callback, Local Sync & UI Feedback:** Giao dịch hoàn tất $\rightarrow$ UI người cao tuổi nhận thông báo ting-ting, số dư ví nhảy lên ngay lập tức; UI người thuê hiển thị trạng thái hoàn tất thành công; Dashboard của Quản trị viên cộng thêm 20.000đ vào ô Doanh thu phí sàn.

---

## TC09: TRỢ NĂNG CAO CẤP: GIỌNG ĐỌC NHÂN TẠO TTS, TÙY CHỈNH CỠ CHỮ (100% - 115% - 130%) & XEM TRƯỚC TRỰC QUAN

### 1. Thông tin Test Case
- **Mã kiểm thử:** `TC_ACCESSIBILITY_01`
- **Mục tiêu:** Kiểm tra các tính năng trợ năng chuyên biệt cho người lớn tuổi: Bộ đọc giọng nói nhân tạo tiếng Việt (TTS) thông minh, chuyển đổi tiền tệ thành chữ đọc, và hộp thoại tùy chỉnh cỡ chữ với 3 mức độ kèm khung xem trước trực tiếp.
- **Mức độ ưu tiên:** High (P1)
- **Điều kiện tiên quyết:** Thiết bị Android có cài đặt Google Text-To-Speech (mặc định có trên 99% thiết bị Android).

### 2. Kịch bản kiểm thử (Test Scenario Steps)
1. **Kiểm tra Giọng đọc nhân tạo (TTS):**
   - Tại màn hình Chi tiết công việc, chạm vào biểu tượng Chiếc loa vàng ở góc trên.
   - Lắng nghe: Hệ thống phát giọng đọc tiếng Việt chuẩn, đọc rõ ràng Tiêu đề, Thời gian, Địa chỉ.
   - Đặc biệt lắng nghe mức lương: Chuỗi `250.000đ` được đọc chính xác thành *"hai trăm năm mươi nghìn đồng"*.
   - Thoát màn hình (bấm Back hoặc xoay màn hình): Giọng đọc lập tức ngắt ngay, không bị đọc đè hoặc phát âm thanh ngầm.
2. **Kiểm tra Tùy chỉnh Cỡ chữ (Font Scale):**
   - Vào mục **Tài khoản / Cài đặt** $\rightarrow$ Chọn *"Cài đặt Trợ năng & Giọng đọc"*.
   - Hộp thoại `AccessibilitySettingsDialog` mở lên.
   - Thử nghiệm chọn các mức cỡ chữ:
     - Chạm vào *"Mặc định (100%)"*: Khung xem trước hiển thị cỡ chữ tiêu chuẩn.
     - Chạm vào *"Chữ lớn (115%)"*: Khung xem trước phóng to chữ lên rõ rệt.
     - Chạm vào *"Chữ rất lớn (130%)"*: Khung xem trước phóng to chữ đậm nét, dễ đọc cho mắt kém.
   - Thử nghiệm điều chỉnh Tốc độ đọc: *"Chậm (0.75x)"*, *"Vừa phải (0.85x)"*, *"Bình thường (1.0x)"*.
   - Bấm nút *"Nghe thử giọng đọc"* $\rightarrow$ Hệ thống phát câu mẫu để kiểm tra âm lượng và tốc độ.
   - Bấm nút *"Lưu & Áp dụng"*.

### 3. Dữ liệu thử nghiệm mẫu
- Cỡ chữ: `1.0f` (100%), `1.15f` (115%), `1.30f` (130%).
- Tốc độ đọc TTS: `0.75f`, `0.85f`, `1.0f`.
- Chuỗi text test: `"Lương: 250.000 đ/buổi"`.

### 4. Kết quả mong đợi
- Giọng đọc phát âm chuẩn tiếng Việt tự nhiên, ngắt câu theo ngữ điệu.
- Cỡ chữ được áp dụng toàn diện trên giao diện, bố cục không bị vỡ hoặc che khuất nhờ cấu trúc `ScrollView` và `dimens` co giãn.
- Thiết lập trợ năng được lưu vĩnh viễn vào SQLite `accessibility.db`.

### 5. MÔ TẢ CHI TIẾT DÒNG DỮ LIỆU DI CHUYỂN (DETAILED DATA FLOW)

- **(A) UI Input & View:** Người dùng tương tác trên `AccessibilitySettingsDialog`. Khi chọn radio button cỡ chữ, sự kiện `OnCheckedChangeListener` lập tức gọi `tvFontPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSize * scale)` để cập nhật xem trước ngay trong 16ms (60 FPS).
- **(B) ViewModel & Controller:** `TtsControllerHelper` xử lý logic phát giọng nói. Chuẩn hóa chuỗi văn bản trước khi đọc bằng giải thuật Regex: Tìm kiếm các mẫu số tiền `(\d+[\.\d]*)\s*(?:đ|vnd|đồng)` và dịch thành từ ngữ tiếng Việt tự nhiên qua hàm đọc số chuyên biệt.
- **(C) Repository & Hardware Layer Execution:**
  - `TtsManager.java` nắm giữ đối tượng `android.speech.tts.TextToSpeech`. Thiết lập ngôn ngữ `Locale("vi", "VN")`. Gán tốc độ đọc qua `tts.setSpeechRate(rate)`.
  - Kiểm tra dịch vụ hệ thống `AccessibilityManager`: Nếu máy đang bật TalkBack, tự động hạ mức can thiệp để tránh xung đột âm thanh với người khiếm thị.
  - Khi người dùng bấm Lưu, `AccessibilityStore` ghi các giá trị cấu hình vào SQLite nội bộ trên máy (`accessibility.db` / `SharedPreferences`).
- **(D) Lifecycle Observer & Safety:** `TtsManager` tích hợp `LifecycleEventObserver`. Khi nhận sự kiện `Lifecycle.Event.ON_PAUSE` hoặc `ON_DESTROY`, tự động gọi `tts.stop()` để giải phóng kênh âm thanh ngay lập tức.
- **(E) Callback & UI Refresh:** Hộp thoại đóng lại $\rightarrow$ Listener `onSaved(newSettings)` kích hoạt cập nhật lại tỷ lệ hiển thị trên Activity hiện tại, người dùng trải nghiệm cỡ chữ to rõ ngay tức thì.

---

## TC10: TÍCH HỢP BẢN ĐỒ CHỈ ĐƯỜNG GOOGLE MAPS INTENT NAVIGATION (TỪ VỊ TRÍ HIỆN TẠI ĐẾN ĐỊA ĐIỂM LÀM VIỆC)

### 1. Thông tin Test Case
- **Mã kiểm thử:** `TC_MAPS_NAV_01`
- **Mục tiêu:** Giúp người cao tuổi dễ dàng xem đường đi từ vị trí hiện tại đến nơi làm việc chỉ với 1 chạm duy nhất, không cần ghi nhớ địa chỉ hay copy dán phức tạp sang ứng dụng khác.
- **Mức độ ưu tiên:** High (P1)
- **Điều kiện tiên quyết:** Thiết bị có cài đặt ứng dụng Google Maps (hoặc có trình duyệt web Chrome).

### 2. Kịch bản kiểm thử (Test Scenario Steps)
1. Đăng nhập vai trò `SEEKER`.
2. Mở một Công việc chi tiết bất kỳ trong danh sách việc làm (hoặc mở Chi tiết ca làm việc đã chốt).
3. Tìm đến khối thông tin **"Địa điểm làm việc"**:
   - Quan sát: Bên cạnh dòng địa chỉ hiển thị một nút có icon Bản đồ mang nhãn *"Xem đường đi"*.
4. Chạm vào nút *"Xem đường đi"*:
   - **Trường hợp A (Máy có ứng dụng Google Maps):** Ứng dụng Google Maps bản quyền của máy lập tức được bật lên ở chế độ Dẫn đường (Turn-by-turn Navigation), tự động lấy điểm xuất phát là vị trí GPS hiện tại của người dùng và đích đến là địa chỉ công việc.
   - **Trường hợp B (Máy không cài Google Maps / Máy ảo):** Hệ thống tự động kích hoạt trình duyệt web mở Google Maps Web Navigation trực tuyến tương đương.
5. Kiểm tra đường đi $\rightarrow$ Bấm nút Back trên điện thoại: Quay trở lại chính xác màn hình công việc của GrandMaAssist mà không bị tải lại hay mất dữ liệu.

### 3. Dữ liệu thử nghiệm mẫu
- Địa chỉ làm việc: `"Số 123 Nguyễn Thị Minh Khai, Phường Bến Thành"`, Quận: `"Quận 1"`.
- Chuỗi địa chỉ đích: `"Số 123 Nguyễn Thị Minh Khai, Phường Bến Thành, Quận 1"`.

### 4. Kết quả mong đợi
- Ứng dụng mở đúng ứng dụng Google Maps mà không gây treo hay văng app (No ActivityNotFoundException).
- Địa chỉ đích được mã hóa chuẩn URL Encode, hỗ trợ đầy đủ tiếng Việt có dấu.
- Khi người dùng quay lại app, trạng thái làm việc được giữ nguyên vẹn.

### 5. MÔ TẢ CHI TIẾT DÒNG DỮ LIỆU DI CHUYỂN (DETAILED DATA FLOW)

```text
[UI: Nút "Xem đường đi" tại ô Địa chỉ] 
         │ (1) User click
         ▼
[MapNavigationHelper.openDirections(context, address, area)] 
         │ (2) Ghép chuỗi & Chuẩn hóa: address + ", " + area
         ▼
[Uri.encode(destination)] ──> Mã hóa tiếng Việt an toàn: google.navigation:q=...
         │ (3) Tạo Intent(Intent.ACTION_VIEW, gmmIntentUri)
         ▼
[Kiểm tra Intent Resolver]
    ├── (A) Nếu có com.google.android.apps.maps ──> Chạy Google Maps Native App
    └── (B) Nếu không có ──> Fallback mở https://www.google.com/maps/dir/?api=1&destination=...
         │
         ▼
[Hệ điều hành Android] ──> Điều hướng người dùng trực tiếp trên bản đồ số
```

- **(A) UI Input & View:** Người dùng chạm vào nút `btnViewMapDirections` tích hợp ngay bên cạnh trường địa chỉ trong `PublicJobsActivity`, `JobPostActivity` hoặc `ShiftAgreementDetailActivity`.
- **(B) Helper & Parameter Preparation:** Phương thức `MapNavigationHelper.openDirections(context, address, area)` tiếp nhận dữ liệu. Kiểm tra an toàn: Nếu chuỗi địa chỉ rỗng, hiển thị Toast cảnh báo: *"Chưa có thông tin địa chỉ để xem đường đi"*. Ghép nối chuỗi địa chỉ chi tiết và tên quận/huyện đầy đủ.
- **(C) Intent Resolution & Packaging:**
  - Tạo Uri với scheme chuyên biệt của Google Maps Navigation: `Uri.parse("google.navigation:q=" + Uri.encode(destination))`.
  - Đóng gói `Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri)`. Gán package mục tiêu: `mapIntent.setPackage("com.google.android.apps.maps")`.
  - Gọi `mapIntent.resolveActivity(context.getPackageManager())`:
    - Nếu khác `null`: Gọi `context.startActivity(mapIntent)`.
    - Nếu bằng `null` (máy không có app Maps): Xây dựng Fallback Web Uri: `https://www.google.com/maps/dir/?api=1&destination=` kèm chuỗi encode, kích hoạt qua trình duyệt hệ thống.
- **(D) OS System Execution:** Android Activity Manager nhận Intent và đưa màn hình Bản đồ lên Foreground, tính toán lộ trình tối ưu bằng xe máy/đi bộ phù hợp cho người cao tuổi di chuyển.
- **(E) Lifecycle & Return:** Khi người dùng xem xong đường đi và nhấn nút Back (hoặc cử chỉ vuốt về), Activity của GrandMaAssist tiếp tục vòng đời `onResume()`, giữ nguyên vẹn toàn bộ giao diện và dữ liệu công việc đang xem.

---

## BẢNG TỔNG KẾT TRẠNG THÁI KIỂM THỬ VÀ ĐỘ BAO PHỦ DÒNG DỮ LIỆU

| Mã Test Case | Tên Luồng Nghiệp Vụ | Mức Độ Ưu Tiên | Độ Bao Phủ Dữ Liệu | Trạng Thái Biên Dịch & Code |
|:---:|---|:---:|:---:|:---:|
| **TC01** | Đăng nhập Google & Số điện thoại OTP | P0 | Auth $\rightarrow$ Token $\rightarrow$ Role Navigation | **ĐẠT (PASS)** |
| **TC02** | Phân luồng vai trò & Khởi tạo hồ sơ | P1 | Input $\rightarrow$ Validation $\rightarrow$ Transaction $\rightarrow$ UI | **ĐẠT (PASS)** |
| **TC03** | Xác thực CCCD & Quét QR ML Kit | P0 | Camera $\rightarrow$ ML Kit On-Device $\rightarrow$ Storage/Firestore | **ĐẠT (PASS)** |
| **TC04** | Đăng tin tuyển dụng & Bản nháp | P1 | Input $\rightarrow$ SQLite Draft $\rightarrow$ Base64 Image $\rightarrow$ Firestore | **ĐẠT (PASS)** |
| **TC05** | Admin duyệt hồ sơ & Dashboard 4 box | P0 | Server Aggregation $\rightarrow$ Filter $\rightarrow$ Transaction | **ĐẠT (PASS)** |
| **TC06** | Tìm việc 4 bước & Nộp đơn ứng tuyển | P0 | SQLite Prefs $\rightarrow$ GuidedJobMatcher $\rightarrow$ Firestore | **ĐẠT (PASS)** |
| **TC07** | Đàm phán ca & Ký quỹ an toàn | P0 | Two-way Versioning $\rightarrow$ Hold Funds Transaction | **ĐẠT (PASS)** |
| **TC08** | Vận hành ca, Báo thức & Trích phí 10% | P0 | AlarmManager $\rightarrow$ 2-way Checkin $\rightarrow$ Settlement | **ĐẠT (PASS)** |
| **TC09** | Trợ năng Giọng đọc TTS & Cỡ chữ | P1 | Vietnamese TTS $\rightarrow$ Currency Parser $\rightarrow$ Preview | **ĐẠT (PASS)** |
| **TC10** | Bản đồ chỉ đường Google Maps | P1 | Intent Navigation $\rightarrow$ Web Fallback $\rightarrow$ Zero Crash | **ĐẠT (PASS)** |

*(Tất cả 10 kịch bản kiểm thử trên đã được đồng bộ chuẩn hóa theo đúng kiến trúc mã nguồn thực tế của dự án GrandMaAssist v3).*
