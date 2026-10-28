# TỔNG HỢP KỸ THUẬT, CÔNG NGHỆ VÀ APIS ĐỒ ÁN GRANDMA ASSIST
> **Mục đích tài liệu:** Tổng hợp toàn diện, chính xác 100% dựa trên mã nguồn thực tế để sinh viên đưa vào **Báo cáo PDF (chiếm 40% điểm)**, minh chứng **Kiến trúc Code (chiếm 40% điểm)** và chuẩn bị nội dung **Thuyết trình 5 phút (chiếm 20% điểm)** trước Hội đồng bảo vệ.

---

## PHẦN 1: ĐỐI SOÁT CÁC YÊU CẦU BẮT BUỘC `[*]` CỦA MÔN HỌC

Trong đề cương đồ án môn học, phần kỹ thuật bắt buộc **`[*]`** bao gồm:
1. **Firebase (Firestore)**
2. **Responsive (khi xoay màn hình)**
3. **Khuyến khích sync dữ liệu về dạng local/offline với SQLite (ngắt mạng vẫn xem được trang đã tải)**
4. **Khai thác APIs hỗ trợ (chứng thực Google, v.v.)**

Dưới đây là chi tiết hiện thực từng yêu cầu trong dự án:

### 1.1. Firebase (Firestore) — Cơ sở dữ liệu đám mây thời gian thực
* **Thư viện tích hợp:** `com.google.firebase:firebase-firestore` (Firebase BoM `35.0.1` trong `app/build.gradle.kts`).
* **Vai trò trong hệ thống:** Lưu trữ toàn bộ dữ liệu nghiệp vụ theo mô hình NoSQL hướng tài liệu (Document-oriented), hỗ trợ Realtime Snapshot Listener và bảo mật phân quyền đa tầng qua Firebase Security Rules.
* **Các Collection chính đang hoạt động thực tế:**
  1. `users`: Quản lý tài khoản, vai trò cố định (`SEEKER`, `EMPLOYER`, `ADMIN`), số điện thoại E.164 đã qua xác minh OTP, trạng thái duyệt hưu trí (`retirementStatus`).
  2. `jobSeekerProfiles`: Hồ sơ người tìm việc (kỹ năng, kinh nghiệm tóm tắt, ca làm việc ưu tiên).
  3. `employerProfiles`: Hồ sơ người thuê / hộ gia đình (tên tổ chức/cá nhân, địa chỉ giới thiệu).
  4. `jobPosts`: Tin đăng tuyển dụng việc làm với các trạng thái (`PENDING_REVIEW`, `OPEN`, `CLOSED`, `REJECTED`).
  5. `applications`: Đơn ứng tuyển việc làm của người cao tuổi (`PENDING`, `ACCEPTED`, `WITHDRAWN`, `REJECTED`).
  6. `workSessions`: Thỏa thuận và vòng đời ca làm việc (`AWAITING_FUNDS`, `READY`, `IN_PROGRESS`, `COMPLETED_PENDING_CONFIRMATION`, `COMPLETED`, `CANCELLED_MUTUAL`).
  7. `retirementVerificationRequests`: Hồ sơ thẩm định hưu trí gửi Admin xét duyệt (ảnh CCCD, ảnh quyết định nghỉ hưu/sổ BHXH mô phỏng).
  8. `wallets`: Ví tiền mô phỏng (số dư khả dụng, tiền cọc tạm giữ, lịch sử giao dịch nạp/rút/quyết toán).
  9. `appConfig`: Cấu hình danh mục công việc (`categories`) và quận/huyện TP.HCM (`areas`).
* **Kỹ thuật Firestore nâng cao đã áp dụng:**
  - **Firestore Transactions (Giao dịch nguyên tử):** Đảm bảo tính toàn vẹn dữ liệu tài chính (ACID) tuyệt đối trong:
    - Nạp tiền cọc ca làm việc (`fundSession` trong `WorkSessionRepository.java`).
    - Quyết toán tiền công và tự động trích 10% phí sàn duy trì (`confirmCompletionAndSettle`).
    - Hủy ca làm việc và hoàn cọc 100% về ví người thuê (`confirmCancelSession`).
  - **Firestore Security Rules:** Ngăn chặn gian lận phía Client, bắt buộc kiểm tra `request.auth.uid`, kiểm tra quyền Admin, và không cho phép tự sửa điểm danh/tiền bạc tùy tiện.

---

### 1.2. Responsive & Thích ứng khi xoay màn hình (Screen Rotation)
Dự án giải quyết bài toán giao diện thân thiện với người cao tuổi trên nhiều kích thước màn hình (điện thoại dọc, xoay ngang Landscape, máy tính bảng Tablet, hoặc chia đôi cửa sổ Split-screen):

* **Kỹ thuật giới hạn chiều rộng nội dung (`content_max_width = 560dp`):**
  - Khai báo tại `res/values/dimens.xml`.
  - Được tính toán tự động qua `addOnLayoutChangeListener` trong các Activity chính (`JobDraftActivity`, `JobPostActivity`, `PublicJobsActivity`, `VerificationActivity`).
  - **Tác dụng:** Khi người dùng xoay ngang điện thoại hoặc dùng Tablet, bố cục không bị kéo dãn bẹt bè toàn màn hình mà được gom gọn ở giữa (`Gravity.CENTER_HORIZONTAL`) với độ rộng tối đa 560dp. Người cao tuổi không phải đảo mắt quá xa từ cạnh này sang cạnh kia.
* **Bọc cuộn an toàn (`ScrollView` với `fillViewport="true"`):**
  - Toàn bộ giao diện từ đăng nhập, tạo tin, tìm việc đến chi tiết thỏa thuận ca đều được bọc trong `ScrollView`.
  - Khi xoay ngang (chiều cao hẹp) hoặc khi bàn phím ảo hiển thị (`IME`), giao diện tự động cuộn mượt mà, không bao giờ bị tràn viền (overflow) hoặc che mất các nút bấm hành động chính.
* **Bảo toàn dữ liệu qua Vòng đời (`ViewModel` & `onSaveInstanceState`):**
  - Khi xoay màn hình làm Activity bị hủy và tạo lại (Recreate), toàn bộ trạng thái dữ liệu (bước đang chọn trong tìm việc 4 bước, bộ lọc việc làm, vị trí cuộn trang `listScroll`, bản nháp đang soạn dở) đều được lưu giữ nguyên vẹn thông qua Android Jetpack ViewModel.
* **Xử lý âm thanh thông minh theo Quy tắc T11:**
  - `TtsManager` tích hợp `LifecycleEventObserver`. Khi xoay màn hình làm Activity chuyển trạng thái `ON_PAUSE`, hệ thống lập tức ngắt (`stop()`) câu đọc cũ, không bao giờ bị phát chồng 2 giọng nói khi màn hình mới hiện lên.

---

### 1.3. Đồng bộ Dữ liệu Cục bộ Ngoại tuyến với SQLite (Offline Sync)
Đáp ứng trọn vẹn tiêu chí *"Ngắt mạng vẫn có thể chạy được những trang đã xem"*, hệ thống xây dựng **4 kho cơ sở dữ liệu SQLite cục bộ** thông qua `SQLiteOpenHelper` (thư mục `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/local/`):

| Tên SQLite Store | Tên File DB SQLite | Các bảng bên trong | Nghiệp vụ ngoại tuyến (Offline Capabilities) |
|---|---|---|---|
| **`PublicJobStore.java`** | `public_jobs.db` | `cached_jobs`<br>`cached_categories`<br>`cached_areas` | Lưu cache các tin việc làm công khai và danh mục đã tải. Khi tắt Wifi/4G, người tìm việc vẫn mở xem được danh sách tin việc, xem chi tiết công việc cũ kèm nhãn cảnh báo rõ ràng: *"Dữ liệu cũ - Ngoại tuyến"*. |
| **`SavedJobStore.java`** | `saved_jobs.db` | `saved_jobs` | Cho phép người tìm việc lưu các công việc yêu thích theo từng `user_id`. Mất mạng hoàn toàn vẫn có thể mở trang "Việc đã lưu" để xem lại thời gian, địa chỉ làm việc. |
| **`JobDraftStore.java`** | `job_drafts.db` | `job_drafts` | Lưu bản nháp tin tuyển dụng của người thuê. Đang soạn tin mà mất mạng hoặc vô tình thoát ứng dụng thì bản nháp vẫn được giữ nguyên vẹn trong SQLite. |
| **`SearchPreferenceStore.java`** | `search_preferences.db` | `local_preferences` | Lưu các tiêu chí tìm việc 4 bước (công việc muốn làm, ngày rảnh, khu vực, hạn chế sức khỏe) để chạy thuật toán so khớp `GuidedJobMatcher` ngay trên máy mà không phụ thuộc vào mạng. |

---

## PHẦN 2: DANH MỤC KHAI THÁC CÁC APIS HỖ TRỢ TRONG ĐỒ ÁN

### 2.1. Các APIs và SDKs CÓ SỬ DỤNG THỰC TẾ (Kèm dẫn chứng mã nguồn)

Dưới đây là 8 APIs / Dịch vụ nền tảng được khai thác trực tiếp trong mã nguồn:

| STT | Tên API / Dịch vụ | Thư viện / Package | Vị trí file mã nguồn | Nghiệp vụ thực tế trong ứng dụng |
|:---:|---|---|---|---|
| **1** | **Google Identity API (Google Sign-In)** | `com.google.android.libraries.identity.googleid`<br>`androidx.credentials` | `AccountMethodsFlow.java`<br>`AuthRepository.java`<br>`MainActivity.java` | Đăng nhập 1 chạm bằng tài khoản Google thông qua Android Credential Manager hiện đại, lấy `GoogleIdToken` liên kết vào Firebase Auth. |
| **2** | **Firebase Phone Authentication API** | `com.google.firebase:firebase-auth`<br>`PhoneAuthProvider` | `PhoneSignInFlow.java`<br>`AuthRepository.java` | Xác minh số điện thoại người dùng thật qua mạng viễn thông bằng mã OTP SMS thật. |
| **3** | **Google ML Kit Barcode Scanning API** | `com.google.mlkit:barcode-scanning:17.3.0` | `CardQrReader.java` | Trí tuệ nhân tạo thị giác máy tính chạy On-Device (trên máy), tự động phát hiện và đọc mã QR trên ảnh Căn cước công dân gắn chip để bóc tách Họ tên, Ngày sinh, Số định danh. |
| **4** | **Android Text-To-Speech (TTS) API** | `android.speech.tts.TextToSpeech` | `TtsManager.java`<br>`TtsControllerHelper.java` | Giọng đọc tiếng Việt (`Locale("vi", "VN")`) hỗ trợ người cao tuổi; chuyển đổi số tiền tệ `250.000đ` $\rightarrow$ *"hai trăm năm mươi nghìn đồng"*; tùy chỉnh tốc độ đọc 0.75x, 0.85x, 1.0x. |
| **5** | **Firebase Cloud Storage API** | `com.google.firebase:firebase-storage` | `VerificationRepository.java`<br>`VerificationImages.java` | Lưu trữ tập tin ảnh thẻ căn cước và ảnh quyết định hưu trí lên máy chủ Cloud Storage an toàn. |
| **6** | **Android System Telephony Dial API** | `android.content.Intent.ACTION_DIAL`<br>`Uri.parse("tel:...")` | `JobApplicantsActivity.java`<br>`MyApplicationsActivity.java` | Tích hợp trình quay số hệ thống để gọi điện liên hệ trực tiếp giữa người thuê và ứng viên lớn tuổi chỉ với 1 chạm. |
| **7** | **Android Accessibility Manager API** | `android.view.accessibility.AccessibilityManager` | `TtsManager.java` | Truy vấn dịch vụ trợ năng của hệ điều hành để phát hiện máy có đang bật TalkBack hay không, tự động né đọc chồng âm thanh. |
| **8** | **Android Photo Picker API** | `ActivityResultContracts.PickVisualMedia` | `VerificationActivity.java` | API chọn ảnh hiện đại của AndroidX, cho phép người dùng chọn ảnh giấy tờ từ thư viện an toàn mà không cần cấp quyền đọc toàn bộ thẻ nhớ (`READ_EXTERNAL_STORAGE`). |

---

### 2.2. Các APIs KHÔNG SỬ DỤNG (Nêu rõ phạm vi để tự tin trả lời vấn đáp)

Khi thuyết trình và trả lời câu hỏi của Hội đồng, cần nắm rõ các thành phần sau **KHÔNG** nằm trong phạm vi dự án:

1. **Cổng thanh toán tiền thật (VNPAY, MoMo, ZaloPay, Banking API):** 👉 **KHÔNG CÓ.**
   - *Lý giải:* Ứng dụng triển khai hệ thống **Ví tiền mô phỏng (Simulated Wallet)** hoạt động dựa trên Firestore Transactions và bảng ghi sổ kép `WalletTransaction`. Toàn bộ thao tác nạp/rút/giữ cọc/hoàn tiền là tiền mô phỏng phục vụ mục đích học tập và bảo vệ đồ án.
2. **Google Maps Platform / GPS Navigation API (Bản đồ số động, tính km):** 👉 **KHÔNG CÓ.**
   - *Lý giải:* Ứng dụng lọc khu vực làm việc dựa trên danh mục Quận/Huyện TP.HCM (`areaCode`), không sử dụng định vị GPS hay tính đường đi thời gian thực.
3. **API AI Đám mây bên ngoài (OpenAI ChatGPT, Gemini API, Chatbot):** 👉 **KHÔNG CÓ.**
   - *Lý giải:* Thuật toán gợi ý việc làm cho người cao tuổi được xây dựng nội bộ bằng giải thuật tính điểm trọng số cục bộ trong file [`GuidedJobMatcher.java`](file:///e:/Dev/Study'/Mobie/GrandMaAssist/GrandMaAssist/app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/domain/service/GuidedJobMatcher.java) (độ tương đồng về danh mục, ca làm, khu vực và tiêu chuẩn sức khỏe), không gọi API ngoài.
4. **Cơ sở dữ liệu quốc gia về dân cư / VNeID / BHXH Việt Nam:** 👉 **KHÔNG CÓ.**
   - *Lý giải:* Việc quét QR CCCD là offline từ ảnh chụp; việc thẩm định hưu trí do Admin duyệt tay mô phỏng.
5. **Speech-To-Text (Nhận diện giọng nói để ra lệnh):** 👉 **KHÔNG CÓ.**
   - *Lý giải:* Ứng dụng chỉ sử dụng **Text-To-Speech (Máy phát giọng nói đọc cho người cao tuổi nghe)**, không thu âm nhận diện lệnh nói.

---

## PHẦN 3: CẤU TRÚC PHÂN CHIA CODE KHOA HỌC (CHIẾM 40% ĐIỂM CODE)

Dự án áp dụng mô hình kiến trúc phân lớp chuẩn **Clean Architecture kết hợp MVVM (Model-View-ViewModel)**:

```
vn.edu.ueh.nhom4.grandmaassist.app/
│
├── domain/                      [LỚP NGHIỆP VỤ CỐT LÕI - DOMAIN]
│   ├── model/                   # Các Entity, Value Object (JobPost, WorkSession, Application, AccessibilitySettings,...)
│   └── service/                 # Dịch vụ tính toán logic (GuidedJobMatcher - Thuật toán gợi ý việc)
│
├── data/                        [LỚP DỮ LIỆU - DATA]
│   ├── local/                   # Cơ sở dữ liệu SQLite cục bộ (PublicJobStore, SavedJobStore, JobDraftStore,...)
│   └── repository/              # Cầu nối dữ liệu Firestore, Storage, Auth, ML Kit (JobPostRepository, WorkSessionRepository,...)
│
├── viewmodel/                   [LỚP ĐIỀU KHIỂN DỮ LIỆU GIAO DIỆN - VIEWMODEL]
│   ├── AuthViewModel.java       # Quản lý luồng đăng nhập, vai trò
│   ├── GuidedSearchViewModel.java# Quản lý tiến trình tìm việc 4 bước
│   ├── ShiftAgreementViewModel.java# Quản lý trạng thái và giao dịch ca làm
│   └── ...                      # Các ViewModel độc lập cho từng màn hình
│
├── ui/                          [LỚP HIỂN THỊ GIAO DIỆN - PRESENTATION]
│   ├── jobs/                    # Giao diện Tìm việc, Chi tiết việc làm, Đăng tin
│   ├── sessions/                # Giao diện Thỏa thuận ca làm việc, Điểm danh, Hoàn thành, Hủy ca
│   ├── applications/            # Giao diện Quản lý ứng viên & Đơn ứng tuyển
│   ├── wallet/                  # Giao diện Ví tiền mô phỏng & Lịch sử giao dịch
│   ├── verification/            # Giao diện Chụp CCCD, Quét QR, Nộp duyệt hưu trí
│   ├── accessibility/           # Nút điều khiển âm thanh TTS, Hộp thoại Cài đặt giọng đọc
│   ├── profile/                 # Giao diện Hồ sơ cá nhân
│   └── navigation/              # Thanh điều hướng phân quyền theo vai trò (RoleNavigationBar)
│
└── accessibility/               [MODULE HỖ TRỢ TIẾP CẬN]
    └── TtsManager.java          # Bộ đọc giọng nói Lifecycle-Aware cho người lớn tuổi
```

---

## PHẦN 4: KỊCH BẢN THUYẾT TRÌNH BÁO CÁO 5 PHÚT (CHEATSHEET)

Khi trình bày trước hội đồng trong 5 phút, bro nên bám sát dàn ý sau để nêu bật đúng các kỹ thuật trong `[*]`:

* **Phút 1: Giới thiệu đề tài & Giá trị thực tế**
  - Giới thiệu đề tài: GrandMa Assist — Nền tảng kết nối việc làm bán thời gian cho người đã nghỉ hưu.
  - Vấn đề giải quyết: Người cao tuổi mắt kém, thao tác chậm, dễ bị lừa đảo $\rightarrow$ Ứng dụng tập trung vào tính tiếp cận (chữ to, giọng đọc hướng dẫn, quy trình rõ ràng).
* **Phút 2: Minh chứng các yêu cầu bắt buộc `[*]`**
  - Nêu rõ 4 trụ cột kỹ thuật:
    1. **Firestore Realtime & Security Rules:** Toàn bộ dữ liệu tin đăng, ca làm, ví tiền đều chạy trên Firestore với Transaction đảm bảo tiền cọc không bị sai lệch.
    2. **Responsive:** Giao diện co giãn thông minh, tự động căn giữa 560dp khi xoay ngang/máy tính bảng, bọc ScrollView toàn diện.
    3. **SQLite Offline Sync:** 4 database SQLite riêng biệt giúp cô chú mất mạng vẫn xem lại được việc làm và việc đã lưu.
    4. **APIs hỗ trợ:** Đăng nhập 1 chạm Google Identity, OTP SMS thật, Google ML Kit quét QR căn cước, và Android Text-To-Speech.
* **Phút 3 - 4: Chạy Live Demo ứng dụng**
  - **Demo 1:** Mở Tìm việc 4 bước $\rightarrow$ Bấm nút **[🔊 Nghe hướng dẫn]** để app phát giọng đọc tiếng Việt rõ ràng.
  - **Demo 2:** Xoay ngang điện thoại $\rightarrow$ Chứng minh giao diện tự căn giữa 560dp và giọng đọc lập tức tự ngắt an toàn (tuân thủ T11).
  - **Demo 3:** Bật chế độ máy bay (ngắt mạng) $\rightarrow$ Mở trang Việc làm $\rightarrow$ App vẫn tải dữ liệu việc làm từ SQLite cục bộ kèm nhãn *"Dữ liệu cũ - Ngoại tuyến"*.
  - **Demo 4:** Luồng ca làm việc $\rightarrow$ Người thuê nạp cọc $\rightarrow$ Hai bên điểm danh check-in $\rightarrow$ Báo hoàn thành $\rightarrow$ Quyết toán tiền về ví người làm (trừ 10% phí duy trì sàn).
* **Phút 5: Kết luận & Sẵn sàng hỏi đáp**
  - Nhấn mạnh: Dự án tuân thủ 100% yêu cầu môn học, code phân chia khoa học theo Clean Architecture, sẵn sàng nhận câu hỏi phản biện từ thầy cô.
