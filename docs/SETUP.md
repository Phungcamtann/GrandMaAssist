# Bàn giao nền tảng và C01 — 23/10/2026

## Trạng thái được xác nhận

- Người dùng đã duyệt thiết kế v3 trong README.
- C00 tại `e0f90aa` đã được người dùng Sync/chạy và xác nhận: đọc thông báo Firebase mới, giữ kết quả qua xoay màn hình, báo lỗi khi mất mạng và tải lại thành công sau khi có mạng. Người dùng đã push commit nền tảng.
- Người dùng đã kiểm tra thành công bốn trường hợp C01 trên bản trước lần chỉnh giao diện này: **đăng nhập Google, đăng xuất, hủy đăng nhập và khôi phục phiên**. Ảnh Android Studio cung cấp cho thấy bản đó build thành công. Không suy rộng kết quả này sang toàn bộ lỗi/mạng/vòng đời hoặc các thiết bị khác.
- **Người dùng đã duyệt giao diện mới và xác nhận bản sửa đăng xuất hết treo.** Logcat ngày 23/10/2026 ghi provider call vào worker lúc 23:41:03.719, trả về lúc .727 và `provider confirmed` trên main lúc .770, khoảng 51 ms từ lúc gọi đến xác nhận. Lần này hoàn tất bằng callback thành công, không nhờ timeout. Không suy ra đã kiểm tra riêng mọi kịch bản lỗi/thiết bị trong checklist.
- Codex chỉ đọc, sửa mã/tài liệu và review tĩnh; không chạy build, test, lint, emulator hoặc kiểm thử tự động; không stage, commit, push, merge, deploy hoặc thay đổi Console.
- Thay đổi `.idea` có sẵn, kể cả file đã staged, thuộc người dùng và được giữ nguyên.

## Cấu hình dự án

Android Java/XML; package `vn.edu.ueh.nhom4.grandmaassist.app`.
minSdk 29, compileSdk 36.1, targetSdk 36, Java 11. Giữ nguyên các phiên bản build, Firebase BoM 35.0.1 và Lifecycle 2.10.0.

Firebase project `grandmaassist-a1dfa`; Storage bucket `grandmaassist-a1dfa.firebasestorage.app`.
Có Auth/Firestore/Storage, không có Cloud Functions hoặc backend riêng.
Firestore chỉ cache trong bộ nhớ; SQLite chưa triển khai.

C01 bổ sung AndroidX Credentials + Credentials Play services Auth **1.6.0 stable**, Google ID **1.2.1**. Không dùng API GoogleSignIn cũ.

## Cấu trúc và hành vi C01

- `MainActivity`: hai nội dung đăng nhập/tài khoản trong cùng một Activity, cuộn được và giới hạn chiều rộng trên tablet. Không có trang tài khoản cũ trong back stack. Xử lý system bars, cutout và IME bằng insets; giao diện sáng cả khi hệ thống bật dark mode.
- `AuthViewModel`: điều phối thao tác, không giữ Activity/Context màn hình hoặc lưu credential/token vào saved state.
- `AuthRepository`: một instance thuộc `GrandMaAssistApp`, dùng Firebase Auth làm nguồn phiên; giữ trạng thái và công việc Firebase qua thay đổi cấu hình. Tất cả chuyển trạng thái/callback trên main executor; mã yêu cầu loại bỏ callback cũ sau khi thao tác đã bị thay thế.
- Nút Google gọi `GetSignInWithGoogleOption`, phù hợp người quay lại, người dùng mới, thêm/đổi tài khoản; dùng `R.string.default_web_client_id` do Google Services sinh từ Web OAuth client trong JSON. Không tự mở bộ chọn khi vào app hoặc khi xoay.
- Chỉ `CustomCredential` đúng loại Google ID token, phân tích được và có token không rỗng mới được gửi đến Firebase Auth để xác thực. Không tự coi thông tin do bộ chọn trả về là phiên đăng nhập thành công.
- Hủy bộ chọn trở về trạng thái sẵn sàng. Xoay/hủy Activity khi bộ chọn còn mở sẽ hủy yêu cầu Activity cũ và cho thử lại; không tự mở cửa sổ mới. Nếu đã chuyển sang trao đổi với Firebase, công việc tiếp tục qua xoay mà không giữ Activity cũ.
- Phân biệt lỗi khi SDK cung cấp đủ thông tin: không có credential, gián đoạn, provider không sẵn sàng, mạng Firebase, credential lỗi, cấu hình, tài khoản vô hiệu hóa, xung đột và giới hạn yêu cầu. Lỗi chung của provider không bị đoán chắc thành lỗi mạng/OAuth. Không ghi nội dung lỗi SDK thô, token hoặc dữ liệu tài khoản ra log.
- Khi có phiên SDK đã lưu, hiển thị tên/email/phương thức với nhãn trạng thái thích hợp. Mỗi lần Activity vào foreground kiểm tra `reload()` và làm mới token qua Firebase; chỉ sau thành công mới báo máy chủ đã xác nhận trong lần kiểm tra đó. Lỗi mạng/không rõ giữ phiên cục bộ và cho kiểm tra lại. Phiên vô hiệu hóa/hết hiệu lực theo kết quả Firebase được đăng xuất. C01 không cấp bất kỳ quyền nghiệp vụ nào từ phiên offline hay nhãn kiểm tra phiên.
- Đăng xuất xóa thông tin tài khoản trên UI/bộ nhớ do app quản lý trước, gọi Firebase `signOut()`, rồi `clearCredentialStateAsync()` bằng application context. Nếu bước dọn credential lỗi, Firebase vẫn đã đăng xuất và có nút thử lại riêng; người dùng cũng có thể chủ động chọn tài khoản đăng nhập lại. Không xóa Firebase user hoặc thu hồi quyền Google.
- Bản timeout đầu tiên đã được người dùng chạy lại và xác nhận **vẫn treo sau 10 giây**; không coi bản đó đã sửa thành công. Đọc source AndroidX Credentials 1.6.0 cho thấy executor được dùng cho callback, còn lời gọi chọn provider/ủy quyền framework vẫn chạy trên luồng gọi. Bản trước vẫn gọi cả API và `CancellationSignal.cancel()` trên main; đây là rủi ro làm chính timer/UI không cập nhật, chưa phải kết luận từ stack thiết bị.
- Bản sửa tiếp theo chuyển **cả lời gọi dọn phiên và hủy** sang worker; pool tối đa hai luồng, không xếp hàng tác vụ dọn cũ để tránh thực thi sau lần đăng nhập mới. Callback, timeout và mọi thay đổi state được đưa về main. Giữ giới hạn 10 giây qua xoay; nếu chưa có kết quả thì về READY, dừng tiến trình, mở Google và nút thử lại, không báo dọn thành công giả. Callback về sớm gỡ timer; callback trễ bị chặn theo mã yêu cầu/phase. Nếu worker đều bị dịch vụ giữ thì yêu cầu mới thất bại có thể thử lại, không khóa UI hoặc tạo vô hạn luồng. Người dùng đã chạy lại và xác nhận hết treo, callback provider thành công; chưa xác nhận riêng nhánh timeout/thử lại.
- Logcat tag `GrandMaAuth` chỉ ghi các mốc cố định vào provider/trả về/timeout/kết quả/mở lại UI, không chứa UID, tên/email, token, credential hoặc nội dung exception. Nếu vẫn treo, các mốc này giúp phân biệt API bị giữ hay main không xử lý timer; cần log thực tế mới xác định nguyên nhân thiết bị.
- Tên/email thiếu có thông báo thay thế; không có dữ liệu mẫu, ảnh đại diện tải từ mạng, vai trò hoặc thông tin xác minh suy diễn.

`AppConfigRepository` và `ConnectionViewModel` của C00 vẫn được giữ để dùng lại cho chẩn đoán; không nối vào màn sản phẩm hoặc tạo nút kỹ thuật trong C01. Không cần đọc `appConfig/public` trước khi đăng nhập.

## Thiết kế và tài nguyên

Dùng bố cục C01 và ảnh tham chiếu đã được người dùng cung cấp; **chưa truy cập/đối chiếu trực tiếp Figma** trong lần này. Không cắt banner hay giao diện từ screenshot.

- `PrimaryLogo.png` gốc 1177×355 → `drawable-nodpi/grandma_primary_logo.png`, giữ nguyên byte, tỷ lệ/nền/màu; hiển thị `fitCenter`.
- Thay logo lớn bằng thanh gradient xanh nằm dưới status bar thật, PrimaryLogo nhỏ trên vùng trắng bên trái; dùng chung cho đăng nhập và tài khoản. Giữ nền ngà `#FAF9F5`, không thêm texture/màu nền mới. Nội dung căn trái, đệm ngang 20dp, khoảng cách tiêu đề → giới thiệu 8dp → nút Google 12dp. Không đổi kiểu nút Google đã áp dụng.
- `C:\Users\CYBORG\Downloads\Banner.jpg` do người dùng gửi, 1024×572 → `drawable-nodpi/grandma_banner.jpg`, sao chép nguyên byte. Thay banner người làm vườn bằng ảnh mới, `fitCenter`/`adjustViewBounds` để giữ toàn bộ chữ và hình; cao hơn dải ảnh cắt ngang trong mẫu. Banner chỉ minh họa trên màn đăng nhập; chữ/nút vẽ trong ảnh không trở thành điều khiển của app. Màn tài khoản dùng chung thanh thương hiệu/nền/font, giữ dữ liệu và nút đăng xuất.
- `AppIcon.png` gốc 396×384 → `drawable-nodpi/grandma_app_icon.png`, giữ nguyên byte; foreground adaptive icon dùng inset phần trăm, tương đương vùng 66×64 trên canvas 108×108, giữ đúng tỷ lệ khi launcher đổi kích thước và vùng biểu tượng G/lá ở trung tâm. Không thêm khối nền bo góc; bỏ monochrome robot cũ. Các launcher API 29+ dùng adaptive XML; cần xem thực tế với mask launcher của thiết bị.
- Plus Jakarta Sans Bold (font variable nguyên bản, weight 700), Be Vietnam Pro Regular/Semibold được đóng gói trong `res/font`. Đã đọc metadata font: trục `wght` hỗ trợ 200–800, mặc định 400. Wrapper font chọn 700; style tiêu đề đặt thêm `textStyle=bold`, `textFontWeight=700` và `fontVariationSettings='wght' 700` trực tiếp để không dựa vào tên file hoặc kế thừa weight thường. Cỡ tiêu đề 28sp, nội dung 20sp; người dùng đã duyệt giao diện mới.
- Nút Google dùng G màu nguyên bản tải từ Google và Google Sans Medium (weight 500), nền trắng/viền xám. Nút thông thường dùng nhận diện GrandMaAssist.
- Giấy phép OFL và điều khoản nhãn hiệu Google Sans đóng gói trong `app/src/main/assets/licenses`. Nguồn và hash tại [C01-ASSETS](assets/C01-ASSETS.md).
- Không có TTS hoặc nút nghe/dừng/nghe lại; chuyển toàn bộ phần này sang C22 theo chỉ dẫn bổ sung của người dùng.

## Firebase Console: phần người dùng cần đối chiếu

JSON hiện có đúng project/package/bucket và Android/Web OAuth client. Người dùng đã báo Google/Phone được bật. C01 không sửa JSON, Rules, indexes, `.firebaserc` hoặc `firebase.json`; Firestore vẫn chỉ cho `get appConfig/public`, Storage vẫn deny-all.

Trước nghiệm thu Google, người dùng kiểm tra:

1. Firebase Authentication → Sign-in method → Google vẫn enabled trong đúng project.
2. SHA-1 của chứng thư ký bản đang chạy khớp Android OAuth client/package đã đăng ký. Với bản release hoặc máy phát triển khác, cần chứng thư tương ứng. Lấy fingerprint qua Android Studio/công cụ của người dùng; Codex chưa chạy signingReport.
3. Sau Sync, Google Services sinh được resource `default_web_client_id` từ Web client trong `app/google-services.json`. Không thay bằng Android client ID.
4. Thiết bị có Google Play services khả dụng và tài khoản Google; emulator nên dùng image có Google Play. Nếu OAuth audience đang giới hạn Testing/organization, tài khoản thử phải thuộc đối tượng được phép theo cấu hình Console thực tế.

Người dùng đã đăng nhập Google thành công trên cấu hình/bản chạy trước chỉnh giao diện; lần này không yêu cầu đổi Console. Những mục trên dùng để đối chiếu nếu đổi thiết bị/chứng thư hoặc phát sinh lỗi, không phải yêu cầu thiết lập lại cấu hình đang hoạt động. Không đưa Web client secret hay service-account key vào app. Billing/quota SMS, Storage và vị trí dịch vụ thuộc các bước sau; C01 không gửi SMS hoặc upload ảnh.

## Người dùng kiểm tra và tự commit

Thực hiện [C01-CHECKLIST](C01-CHECKLIST.md) trong Android Studio. Nếu Sync/build lỗi, gửi lỗi đầu tiên trong Build Output; nếu lỗi đăng nhập, cung cấp thông báo UI và lỗi liên quan đã che thông tin riêng, không chia sẻ token/credential/API key.

Danh sách file C01 để review/tự commit ở [C01-FILES](C01-FILES.md). Không dùng thao tác stage tất cả vì `.idea/deviceManager.xml` có thể đang staged sẵn. Người dùng tự chọn đúng file C01 và kiểm tra diff staged trước commit. Codex chưa stage/commit bất kỳ file nào.

## Tài liệu API đã đối chiếu

- [Firebase Google Auth và signOut/clearCredentialState](https://firebase.google.com/docs/auth/android/google-signin).
- [Android: button flow với GetSignInWithGoogleOption](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation).
- [AndroidX Credentials: bản stable 1.6.0](https://developer.android.com/jetpack/androidx/releases/credentials).
- [Google ID Maven metadata](https://dl.google.com/dl/android/maven2/com/google/android/libraries/identity/googleid/googleid/maven-metadata.xml): release 1.2.1 lúc chuẩn bị C01.
- [Google branding](https://developers.google.com/identity/branding-guidelines).
- [Firebase: quản lý người dùng/phiên](https://firebase.google.com/docs/auth/android/manage-users).

Codex chỉ review tĩnh, chưa tự build/chạy/kiểm thử. Người dùng đã xác nhận bốn trường hợp cơ bản, duyệt giao diện và kiểm tra bản sửa đăng xuất hết treo, có callback provider thành công. Các nhánh timeout/thử lại/callback trễ và biến thể chưa được xác nhận cụ thể vẫn chưa được đánh dấu đạt.
