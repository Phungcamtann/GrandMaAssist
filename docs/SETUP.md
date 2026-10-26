# C08 đã triển khai, chờ người dùng nghiệm thu — 26/10/2026

**Bước hiện hành:** [C08-CONSOLE-SCHEMA](C08-CONSOLE-SCHEMA.md) hướng dẫn Publish toàn bộ Rules hiện tại (mở đọc danh mục active cho khách, giữ jobs/history r4) và tạo bốn index mới, giữ hai index C07. Chờ Enabled rồi Sync/Run và [C08-CHECKLIST](C08-CHECKLIST.md). Khách → Xem việc không cần đăng nhập; SEEKER → Việc khác. Bàn giao tại [C08-FILES](C08-FILES.md). Không gỡ app/xóa dữ liệu hoặc thay Auth/JSON/SHA/Storage; C06 dùng nguyên DB nháp. Codex không chạy build/test/app/Emulator/Rules tests, không thay Console hoặc commit/deploy. Các trạng thái “chuẩn bị C08/chưa triển khai” bên dưới thuộc lịch sử.

**Trạng thái hiện hành:** người dùng đã xác nhận hết lỗi gửi tin và duyệt các luồng hoạt động khác. Commit `08d2211` đã được đối chiếu sau fetch, local HEAD trùng `origin/main`; không có thay đổi chưa commit trước lần cập nhật tài liệu này. Xem [C07-ACCEPTANCE](C07-ACCEPTANCE.md). Không cần làm lại cấu hình C07 hoặc xóa cache/dữ liệu chỉ để chuyển bước. C08 chưa triển khai; các hướng dẫn chờ Publish/nghiệm thu phía dưới là lịch sử trước xác nhận mới. Codex không chạy lại app/test hoặc commit/deploy trong lần đối chiếu này.

**26/10/2026 — bản Rules hiện hành r4:** đã tìm và tái hiện nguyên nhân C07_SUBMIT_TRANSACTION/C07_WRITE_CONTENT_OR_HISTORY là giới hạn 1.000 biểu thức. Người dùng cho phép kiểm tra cục bộ lần này, kết quả 24/24 ca đạt. Publish toàn bộ Rules mới rồi gửi lại nháp cũ trên APK hiện tại; không đổi index/Storage/JSON. Chi tiết [C07-EXPRESSION-LIMIT-FIX](C07-EXPRESSION-LIMIT-FIX.md); nghiệm thu Firebase thật vẫn do người dùng.

C06 đã nghiệm thu và commit `afd7891`, local/GitHub main đã đối chiếu. C07 đã có mã nguồn địa chỉ/gửi tin/Tin đã gửi/Admin duyệt/từ chối/sửa gửi lại. Người dùng thực hiện [C07-CONSOLE-SCHEMA](C07-CONSOLE-SCHEMA.md): Publish toàn bộ firestore.rules và tạo hai composite index jobs trước Sync/Run; không cần tự tạo jobs/history. Sau đó kiểm [C07-CHECKLIST](C07-CHECKLIST.md). Chi tiết [C07-FILES](C07-FILES.md). Codex chỉ rà tĩnh, chưa Sync/build/test/app/Rules hoặc Console/deploy/commit. Không sửa JSON/SHA/provider/Gradle/Storage vì thay đổi này; không xóa dữ liệu app để cập nhật nháp cũ.

# Chốt bản hiện tại và chuẩn bị C06 — 25/10/2026

Người dùng đã duyệt hết bản C04/C05/Hồ sơ thống nhất/sửa sau pull. Đã đối chiếu local/GitHub main cùng `11fcdeb`, working tree sạch trước cập nhật tài liệu; bản sửa Gradle/resource đầy đủ trong `a63734e`, commit cuối chỉ chỉnh JDK jbr-21 của Android Studio. Xem [C05-ACCEPTANCE](C05-ACCEPTANCE.md). Không tự chạy kiểm thử hoặc tự commit để xác minh.

C06 đã được người dùng duyệt kế hoạch và đã triển khai mã nguồn nháp tin tuyển/xem trước, chờ nghiệm thu mới. Giữ nháp đã lưu cho đúng UID đăng nhập lại, không hiện cho tài khoản khác. Người dùng làm [C06-CONSOLE-SCHEMA](C06-CONSOLE-SCHEMA.md): Publish Firestore Rules đọc jobCategories và seed bốn danh mục, giữ areas/C05/Storage/Auth đang đúng; Sync/Run rồi kiểm [C06-CHECKLIST](C06-CHECKLIST.md). Chưa có kết quả runtime do Codex chạy hoặc thay đổi Console/deploy/commit. Chi tiết [C06-FILES](C06-FILES.md). Các mục chờ trong lịch sử không thay thế xác nhận mới; ca kỹ thuật khó chưa tái hiện/C02-F01 vẫn theo dõi riêng.

# Cập nhật cấu hình sau nhận commit C05 — 25/10/2026

Đã đối chiếu commit `2552a04` trùng local/GitHub main. Trong commit, nội dung module app bị đặt vào build.gradle.kts gốc; app/build.gradle.kts thiếu thư viện QR, profile_strings.xml/profile_styles.xml thiếu phần bổ sung đã được Java/layout tham chiếu. Đã sửa bốn file tại working tree, giữ nguyên phiên bản và thay đổi IDE có sẵn. Chi tiết [C05-COMMIT-REPAIR](C05-COMMIT-REPAIR.md).

Người dùng Sync Project with Gradle Files rồi Run, không cần đổi Firebase/Publish Rules. Codex chỉ rà tĩnh nguồn/XML/resource/diff, không Sync/build/test/app hoặc commit/push. Nếu vẫn lỗi, lấy nguyên phần What went wrong/Caused by trong Sync/Build Output; dòng vị trí file/line:1 chưa phải nội dung nguyên nhân đầy đủ. Các báo cáo và xác nhận chức năng trước đó giữ nguyên, bản sửa sau pull chờ người dùng kiểm tra.

# Bàn giao nền tảng, C01 và C02 — 24/10/2026

## Trạng thái được xác nhận

- Người dùng đã duyệt thiết kế v3 trong README.
- C00 tại `e0f90aa` đã được người dùng Sync/chạy và xác nhận: đọc thông báo Firebase mới, giữ kết quả qua xoay màn hình, báo lỗi khi mất mạng và tải lại thành công sau khi có mạng. Người dùng đã push commit nền tảng.
- Người dùng đã kiểm tra thành công bốn trường hợp C01 trên bản trước lần chỉnh giao diện này: **đăng nhập Google, đăng xuất, hủy đăng nhập và khôi phục phiên**. Ảnh Android Studio cung cấp cho thấy bản đó build thành công. Không suy rộng kết quả này sang toàn bộ lỗi/mạng/vòng đời hoặc các thiết bị khác.
- **Người dùng đã duyệt giao diện mới và xác nhận bản sửa đăng xuất hết treo.** Logcat ngày 23/10/2026 ghi provider call vào worker lúc 23:41:03.719, trả về lúc .727 và `provider confirmed` trên main lúc .770, khoảng 51 ms từ lúc gọi đến xác nhận. Lần này hoàn tất bằng callback thành công, không nhờ timeout. Không suy ra đã kiểm tra riêng mọi kịch bản lỗi/thiết bị trong checklist.
- Codex chỉ đọc, sửa mã/tài liệu và review tĩnh; không chạy build, test, lint, emulator hoặc kiểm thử tự động; không stage, commit, push, merge, deploy hoặc thay đổi Console.
- Thay đổi `.idea` có sẵn, kể cả file đã staged, thuộc người dùng và được giữ nguyên.

## C02 — đăng nhập điện thoại/OTP

**Đã triển khai, chờ người dùng kiểm tra; chưa nghiệm thu.** Khi bắt đầu C02, Git working tree sạch; commit C01 thực tế là `ffcc6a2` — “Đăng nhập Google”. C01 đã được người dùng duyệt và commit; giữ nguyên kết quả cùng danh sách C01-F01–F09 trong [C01-CHECKLIST](C01-CHECKLIST.md), không tự đánh dấu các nhánh còn mở là đạt.

C02 dùng lại nhận diện/logo/banner/font/spacing và nút Google C01, bổ sung hai lựa chọn Số điện thoại/Google, form số, bước xem lại E.164 và đồng ý gửi số đến Google, form một trường OTP 6 chữ số. Không có nút nghe/TTS (C22), liên kết tài khoản/đổi số (C03), Firestore profile, role hoặc nới Rules. Không thay Gradle, Firebase JSON hoặc Console.

### Tổ chức và vòng đời

- `PhoneSignInFlow`: trạng thái và challenge tạm trong bộ nhớ, không giữ Activity. Chuẩn hóa số di động VN dạng `0…`/`+84…`, bỏ khoảng trắng/dấu phân cách thông thường; không chấp nhận `+840…` hoặc suy ra số tồn tại/nhà mạng/chủ sở hữu từ format. Không lưu số vào SQLite, preferences, log hoặc tài liệu bàn giao.
- `MainActivity`: chỉ gọi `PhoneAuthProvider.verifyPhoneNumber` khi người dùng xác nhận gửi/gửi lại, hoặc gắn lại callback theo cơ chế reentrant được Firebase mô tả trong cửa sổ yêu cầu gốc. Activity chỉ nằm trong PhoneAuthOptions do SDK quản lý; không chuyển Activity vào ViewModel/repository. Có Activity để hỗ trợ reCAPTCHA.
- SDK tự tháo callback khi Activity dừng. Khi `onStart`, chỉ reattach nếu challenge còn active và trước mốc bảo thủ 55 giây từ yêu cầu 60 giây gốc, không dùng ForceResendingToken. Không reattach sau khi SDK đã báo timeout/failed, khi đã bắt đầu sign-in, hoặc ngoài cửa sổ này; không tự gửi SMS mới khi quay lại muộn. Cần kiểm tra reentrant/auto-retrieval thực tế trên thiết bị và khi reCAPTCHA làm app đổi foreground.
- Countdown gửi lại dựa `SystemClock.elapsedRealtime`, ít nhất 60 giây từ yêu cầu/onCodeSent; không reset khi render/xoay/tab/sửa số. Đây chỉ là UX; Firebase vẫn có thể giới hạn thêm. Gửi lại do người dùng bấm, dùng ForceResendingToken khi có. Không tự retry khi có mạng lại hoặc chưa rõ kết quả.
- Có watchdog 90 giây cho trạng thái đang gửi mà chưa có kết quả; báo chưa rõ kết quả và để người dùng chủ động xử lý. Chỉ onCodeSent mới báo Firebase đã xác nhận gửi. Timeout tự nhận mã không đồng nghĩa OTP hết hạn; vẫn cho nhập mã nếu có verification id.
- `AuthRepository` vẫn là nguồn phiên duy nhất. Callback tự động và nhập tay đi qua cùng cổng sign-in, chặn thao tác kép. Chỉ thành công Firebase mới mở màn tài khoản. Mỗi thế hệ gửi lại/sửa số/đổi phương thức/hủy vô hiệu kết quả cũ trước khi chúng có thể gọi sign-in.
- Nếu hủy sau khi Task Firebase sign-in đã bắt đầu (không có API hủy Task), app giữ trạng thái giải thích chờ hủy và không cho tác vụ Auth mới chạy song song. Khi Task cũ hoàn tất, đăng xuất kết quả cũ rồi mới cho đăng nhập tiếp. Chỉ lưu một cờ boolean `cancelled_phone_exchange` trong `auth_recovery` để không phục hồi một phiên đã yêu cầu hủy nếu process chết trước callback; không lưu số, UID, OTP hoặc token trong cờ này.
- Giữ số đang nhập, phương thức, bước, verification id/resend token và deadline trong bộ nhớ qua xoay. Không lưu OTP đã gõ, verification id hoặc token vào saved state/file/preferences/log. Form không được lưu trong view hierarchy; OTP được xóa khi submit/đổi thế hệ/rời màn và không khôi phục qua xoay. Saved state chỉ có cờ boolean báo từng có form phone; nếu process chết, challenge không được khôi phục bằng dữ liệu nhạy cảm, về nhập số với hướng dẫn, không tự gửi mã.
- Số và provider trên màn tài khoản lấy từ FirebaseUser. Phone-only có tên/email thay thế nếu thiếu; hỗ trợ hiển thị tài khoản có sẵn cả Google/phone theo provider thực tế. Khôi phục phiên dùng Firebase như C01. Đăng xuất phone-only không gọi dọn Google; tài khoản có provider Google vẫn dùng bản dọn credential worker/timeout của C01.

### Phân biệt với C03

Chỉ đăng nhập phone từ trạng thái đã đăng xuất. Không dùng signInWithCredential để thêm số cho một phiên Google đang mở, không tự link/merge/xóa/chuyển dữ liệu giữa UID. Số đã thuộc tài khoản nào thì đăng nhập tài khoản đó theo Firebase; không hứa UID Google chưa liên kết được giữ nguyên. C03 mới làm liên kết/đổi số và kiểm tra quyền liên quan.

### Cấu hình và kiểm tra C02

Đối chiếu Phone provider trong `grandmaassist-a1dfa`, SHA-1/SHA-256 bản ký đang chạy, app verification/Play Integrity/reCAPTCHA, SMS region policy Việt Nam và billing/quota gửi SMS thật. Không tự thay Console hoặc yêu cầu làm lại cấu hình đang hoạt động nếu không có lỗi/bằng chứng thiếu. Số test có thể dùng phát triển nhưng không thay ca nghiệm thu nhận SMS thật trên thiết bị thật.

Đối chiếu API theo [Firebase Android Phone Auth](https://firebase.google.com/docs/auth/android/phone-auth) và [PhoneAuthOptions.Builder](https://firebase.google.com/docs/reference/android/com/google/firebase/auth/PhoneAuthOptions.Builder). Không dùng mã mẫu ghi credential/id/OTP ra log, không tắt app verification để demo.

Người dùng Sync/Run theo [C02-CHECKLIST](C02-CHECKLIST.md); danh sách file tại [C02-FILES](C02-FILES.md). Chưa có kết quả chạy, SMS thật, reCAPTCHA, mạng/quota, vòng đời, autofill hoặc hồi quy C01 của bản C02. Codex chỉ review tĩnh, không build/test/lint/emulator hay gửi SMS; không stage/commit/push/deploy. Không tự chuyển C03.

## Cấu hình nền C00/C01

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

## Cập nhật nghiệm thu C02 — 24/10/2026

Người dùng đã duyệt luồng chính C02 sau khi thử trên Android thật: CAPTCHA quay về app, nhận SMS thật, tự xác minh, nhận SMS trên thiết bị khác và nhập mã thủ công sai/đúng, đăng xuất. Nội dung này thay thế các câu chờ kiểm tra SMS/luồng chính ở lịch sử phía trên.

Ngôn ngữ SMS còn tiếng Anh và tên “Your Firebase App” dù đã cấu hình tiếng Việt: ghi theo dõi C02-F01 tại docs/C02-CHECKLIST.md. Chưa xác định nguyên nhân hoặc đánh dấu đã khắc phục. Các ca gửi lại, vòng đời, mạng, hồi quy Google và các biến thể chưa xác nhận riêng được liệt kê ở mục cập nhật cuối checklist; không tự đánh dấu toàn bộ đạt. Không sửa code, chạy kiểm thử hoặc stage/commit/push trong lần ghi nhận này.

## Chốt C02 — người dùng xác nhận ngày 24/10/2026

**C02 đã được người dùng kiểm tra và phê duyệt, chờ người dùng tự commit.** Mục này là cập nhật hiện hành, thay thế trạng thái chờ xác nhận của năm nhóm kiểm tra được liệt kê trong lần rà soát trước.

Ngoài các kết quả SMS thật/tự xác minh/OTP sai-đúng/đăng xuất đã ghi nhận, người dùng đã xác nhận kiểm tra và duyệt:
1. Gửi lại mã sau countdown.
2. Sửa số, Back và đổi sang Google khi chờ OTP.
3. Xoay màn hình, Home và quay lại trong lúc chờ mã.
4. Đóng/mở app khôi phục phiên phone; Back sau đăng xuất; đăng nhập Google trở lại.
5. Mất mạng/phục hồi và số thiếu/thừa/sai định dạng.

Xác nhận trên áp dụng cho các nhóm người dùng vừa kiểm tra; không tự suy ra mọi biến thể thiết bị hoặc mọi callback đều đã được tái hiện.

Tiếp tục theo dõi riêng:
- C02-F01: SMS vẫn tiếng Anh và tên “Your Firebase App” dù cấu hình tiếng Việt; chưa xác định nguyên nhân, không cản chốt luồng chức năng theo quyết định người dùng.
- Các nhánh khó tái hiện còn ghi trong checklist: mã thực sự hết hiệu lực/mã cũ sau resend, timeout, callback đồng thời/trễ, hủy đúng lúc Firebase sign-in, process bị hủy, CAPTCHA bị hủy và các biến thể font 200%/TalkBack/tablet/thiết bị khác, trừ khi có xác nhận cụ thể sau này.

Không còn mục chờ xác nhận trong năm nhóm cơ bản nêu trên. Codex chỉ cập nhật tài liệu, không chạy kiểm thử và không stage/commit/push. Liên kết/đổi số thuộc C03; TTS thuộc C22.

## C03 — liên kết và đổi số (24/10/2026)

**Trạng thái hiện hành: C01/C02 đã được người dùng duyệt và commit tại ffcc6a2/82c1edf. C03 đã triển khai mã nguồn, chờ người dùng Sync/build/chạy/kiểm tra; chưa nghiệm thu. Chưa chuyển C04.** Những câu chờ commit hoặc chờ triển khai C03 ở phần lịch sử phía trên không còn là trạng thái hiện hành.

Màn Tài khoản có Phương thức đăng nhập, Google và số từ Firebase; thêm số, thêm Google hoặc đổi số theo provider thực tế. Liên kết dùng linkWithCredential, xác thực lại dùng reauthenticate, đổi số dùng updatePhoneNumber trên cùng UID. Google được hiển thị để xác nhận trước link; đổi số phải reauth bằng Google đã link hoặc OTP số cũ cố định từ Firebase rồi mới xác minh số mới. Credential thuộc UID khác báo xung đột, không merge/xóa/unlink hoặc chuyển phiên.

Không cần dependency/backend/Console mới so với C01/C02 đã hoạt động. Giữ Google/Phone enabled, Web client/SHA/Play services và vùng/quota SMS hiện có. Ca link thành công cần Google/số chưa thuộc UID khác; không xóa user Console để chuẩn bị dữ liệu. Người dùng tự Sync/build và chạy theo [C03-CHECKLIST](C03-CHECKLIST.md), tự đối chiếu UID trước/sau riêng tư trong Authentication → Users. Danh sách thay đổi và tài liệu API ở [C03-FILES](C03-FILES.md).

Back/Quay lại trước mutation bỏ challenge. Khi Task Firebase đã gửi, Quay lại/đăng xuất bị khóa và Back giải thích đang hoàn tất; không giả hủy server. Mutation thành công nhưng reload/token lỗi được báo riêng, chỉ Đọc lại kết quả, không lặp thay đổi. Kết quả mạng chưa rõ cũng phải đọc lại. Process chết chỉ phục hồi phiên SDK, đọc lại FirebaseUser/reload/token; không tự SMS hoặc khôi phục số/UID/credential/OTP/verificationId/resend token từ app storage. Xoay giữ coordinator trong bộ nhớ và cơ chế cooldown/reattach C02.

C03 chưa ghi profile Firestore. C04 phải đối chiếu số hồ sơ với claim phone_number của token đã refresh của cùng UID; Rules kiểm tra độc lập UI. Nếu refresh chưa đạt, chưa xác nhận claim/hồ sơ đồng bộ. Không đổi retirementStatus/quyền ứng tuyển. C02-F01 và các nhánh khó tái hiện C01/C02 vẫn theo dõi nguyên trạng.

Codex chỉ review tĩnh mã/diff/tham chiếu tài nguyên. Chưa xác minh biên dịch, UI thực tế, SMS, backend linking/collision, vòng đời và hồi quy C01/C02 trên bản C03. Không chạy build/test/lint/emulator, gửi SMS, sửa Console, stage/commit/push/merge/deploy.
## Chốt C03 — 25/10/2026

**Người dùng xác nhận đã kiểm tra lại toàn bộ C03 và phê duyệt.** Xác nhận này thay thế trạng thái “chờ người dùng kiểm tra/chưa nghiệm thu C03” trong các phần bàn giao trước. Các ca C03-01 đến C03-23 trong C03-CHECKLIST được ghi nhận đạt theo xác nhận tổng thể của người dùng; Codex không tự chạy kiểm thử hoặc bổ sung kết quả đo/thiết bị không được cung cấp.

Phạm vi được duyệt: liên kết Google/phone vào cùng UID, đăng nhập lại bằng các provider đã liên kết, xử lý xung đột giữ tài khoản hiện tại, xác thực lại và đổi số giữ UID, xử lý hủy/lỗi/vòng đời, giao diện và hồi quy C01/C02 theo checklist. Không có dữ liệu tài khoản thật được thêm vào báo cáo.

Giữ nguyên các mục theo dõi riêng từ C01/C02, đặc biệt C02-F01 về ngôn ngữ SMS và tên “Your Firebase App”; việc duyệt C03 không có nghĩa các mục đó đã được sửa. Đồng bộ số vào hồ sơ/claim và Rules tích hợp C04 vẫn thuộc bước sau; TTS thuộc C22.

Người dùng tự review và commit C03 trước khi triển khai luồng tiếp theo. Lần ghi nhận này chỉ sửa tài liệu; Codex không sửa code, build/test, stage, commit, push hoặc deploy.

## C04 — vai trò cố định và hồ sơ (25/10/2026)

**Trạng thái hiện hành: C03 đã duyệt và commit 211730a. C04 mới triển khai, chờ người dùng Publish Rules/seed và tự Sync/build/chạy/kiểm tra; chưa nghiệm thu.**
Đọc [C04-CONSOLE-SCHEMA](C04-CONSOLE-SCHEMA.md) trước khi Run: Publish firebase/firestore.rules vào (default) của grandmaassist-a1dfa và nhập areas DEMO. Không đổi Storage/Google Services/Auth provider.

User riêng theo UID, role cố định SEEKER/EMPLOYER và profile riêng loại trừ nhau được tạo nguyên tử. Save dùng transaction/version; đọc lại khi xung đột hoặc kết quả chưa rõ. Form chỉ trong bộ nhớ ViewModel, giữ qua xoay nhưng không phục hồi dữ liệu riêng khi process chết.
Số contact lấy từ Auth/claim đã refresh, Rules kiểm tra phone_number; Google chưa có số vẫn chuẩn bị hồ sơ. C03 thêm/đổi số có đường sync User và báo lỗi riêng nếu Firestore chưa đồng bộ; không gửi lại OTP để retry đồng bộ.

CCCD/QR, họ tên/ngày sinh pháp lý và xét duyệt thuộc C05; hồ sơ C04 không có giấy tờ/giới tính/sức khỏe tự nhập. TTS thuộc C22. Không có taskbar/trang chủ giả. Admin cấp riêng bằng Console theo tài liệu, không tự chọn hoặc tự cấp quyền trong app.

Xem [C04-FILES](C04-FILES.md) và [C04-CHECKLIST](C04-CHECKLIST.md). Codex chỉ triển khai và review tĩnh, không chạy build/test/lint/emulator/SMS/kiểm thử Rules, không stage/commit/push/deploy. Các mục C01/C02 đang theo dõi không tự được giải quyết bởi C04.

## Chỉnh giao diện và danh mục khu vực — 25/10/2026

Người dùng xác nhận vào được chọn vai trò và chỉ đi tiếp sau khi chọn rồi bấm Tiếp tục. Chưa nghiệm thu các phần khác C04.

Ảnh Console: areas/DEMO_AREA_01 có tên field “Khu vực A (DEMO)” = true thay vì hai field name (string) và active (boolean). Nguyên nhân dữ liệu sai schema đã có bằng chứng ở document này; các document B/C cần người dùng đối chiếu tương tự. Xem hướng dẫn bổ sung trong C04-CONSOLE-SCHEMA.md. Query và Rules giữ nguyên; không dùng fallback field tùy ý hoặc mở quyền để che lỗi seed.

Thay đổi lần này:
- profile_strings.xml: rút gọn nhãn/lỗi/thông báo, bỏ thuật ngữ Firebase/token/Rules khỏi giao diện người dùng.
- profile_content.xml: bỏ heading danh tính thừa; hint kỹ năng ngắn; không hiện giải thích độ đầy đủ hồ sơ thường trực.
- ProfilePanel.java: chỉ hiện Tải lại hồ sơ khi cần phục hồi; chỉ hiện Tải lại khu vực khi trống/lỗi; ẩn thông báo Auth thường lệ trên form; không lặp đoạn đồng bộ khi contact khớp; tên tổ chức chỉ hiện khi chọn tổ chức.
- ProfileViewModel.java: quản lý trạng thái đang tải khu vực và sequence của từng yêu cầu, bỏ kết quả cũ; lỗi tải không giữ danh sách cũ như dữ liệu mới.
- ProfileRepository.java: vẫn query active == true và đọc name; document có active đúng nhưng name thiếu/sai kiểu báo lỗi schema thay vì bỏ qua im lặng.
- docs/C04-CONSOLE-SCHEMA.md, C04-CHECKLIST.md, C04-FILES.md và SETUP.md: bổ sung kết quả người dùng và cách sửa seed.

Chỉ review tĩnh, không build/test/lint/emulator/SMS/Console/stage/commit/push.
Người dùng: sửa name/active trên cả ba document → Sync/Run bản mới → Tải lại khu vực → chọn và lưu → mở lại để đối chiếu. Kiểm tra cả SEEKER/EMPLOYER, lỗi danh mục và giao diện font lớn. Các kết quả này đang chờ người dùng xác nhận.

## Xác nhận nghiệm thu C04 — 25/10/2026

**Trạng thái hiện hành: C04 đã được người dùng kiểm tra và phê duyệt; phần commit do người dùng thực hiện.**
Người dùng xác nhận đã sửa dữ liệu khu vực và thử lại các yêu cầu trong checklist, tất cả hoạt động ổn. Các mục C04-01 đến C04-23 và C04-R01 đến C04-R08 được ghi nhận đạt theo xác nhận tổng thể này. Không có thêm thông tin thiết bị/API hoặc bằng chứng từng ca được cung cấp; không tự bổ sung kết quả chi tiết hay khẳng định Codex đã kiểm thử độc lập.
Xác nhận này thay thế các trạng thái chờ kiểm tra/chưa nghiệm thu C04 trong phần lịch sử phía trên. Lỗi danh mục khu vực và đợt rút gọn giao diện đã được người dùng kiểm tra lại và duyệt.
Codex chỉ cập nhật báo cáo; không chạy build/test/lint/emulator/SMS, không sửa Console/deploy hoặc stage/commit/push. C02-F01 và các mục theo dõi từ luồng trước vẫn giữ nguyên. CCCD/QR thuộc C05; giọng đọc thuộc C22, chưa được tính là đã triển khai trong C04.

## Bàn giao mã nguồn C05 — 25/10/2026

Trạng thái hiện hành: C04 đã duyệt và commit fce2c26; C05 đã triển khai mã nguồn, CHỜ người dùng kiểm tra, chưa nghiệm thu. Không tự kế thừa kết quả C04 cho phần C05.

Có màn xác minh riêng từ hồ sơ, camera/chọn ảnh/xem lại/QR, hồ sơ SEEKER với bằng chứng nghỉ hưu/BHXH mô phỏng và danh tính mẫu EMPLOYER, upload ảnh riêng, request/version bất biến, Admin xem/duyệt/bổ sung/từ chối/thu hồi và thông báo trong app. Lưu nghề nghiệp C04 giữ nguyên dữ liệu định danh C05. Giọng đọc vẫn thuộc C22.

Trước chạy: Publish toàn bộ firebase/firestore.rules và firebase/storage.rules mới tại grandmaassist-a1dfa; chấp nhận kết nối Storage–Firestore nếu Console yêu cầu; chuẩn bị Admin riêng theo tài liệu. Không cần xóa dữ liệu C04 hoặc sửa JSON/SHA/Auth đang chạy ổn.

Xem docs/C05-CONSOLE-SCHEMA.md (thiết lập), docs/C05-FILES.md (phạm vi/giới hạn/file) và docs/C05-CHECKLIST.md (các ca chưa kiểm tra). Codex chỉ rà nguồn/XML/resource/diff, không build/test/lint/emulator/SMS/kiểm thử Rules, không Console/deploy/stage/commit/push. Người dùng tự Sync/Run/kiểm tra rồi mới chốt. C02-F01 và các mục theo dõi cũ giữ nguyên.

### Cập nhật C05 sau lần Sync/Run đầu — 25/10/2026

Người dùng đã Sync/Run app thành công, nhưng chưa duyệt C05 do bố cục và chọn ảnh bằng chứng. Đã tách vùng Căn cước/Bằng chứng/Gửi, giãn nút 12dp/khối 24dp, tách đường nhận ảnh và cho bằng chứng hiển thị ngay sau chọn hợp lệ. Chờ người dùng kiểm tra lại theo C05-CHECKLIST mục C05-UI01–UI06. Chi tiết trong C05-FILES. Đợt này không đổi Rules/schema/Firebase; không cần Publish lại Rules chỉ vì bản sửa giao diện. Codex không build/test/chạy app hoặc commit/push.

### C05 — rà lỗi QR sau xác nhận giao diện/ảnh

Người dùng xác nhận giao diện cải thiện và phần ảnh đã thành công; lỗi còn lại là thông báo đọc QR sau nhận ảnh. Đã chuyển scan sang ảnh nguồn tạm trong RAM, thử vùng QR tuần tự và tách lỗi không thấy/không rõ/khác định dạng/dữ liệu sai/bộ đọc. Chờ người dùng kiểm tra theo C05-QR01–QR07, chưa nghiệm thu C05. Không đổi Rules/schema/dependency hoặc tự build/test/commit. Chi tiết và mã Logcat an toàn nằm trong C05-FILES.

### C05 — tương thích QR 6 trường với dấu ||

Đã nhận cấu trúc QR đã che dữ liệu do người dùng cung cấp cho thẻ cấp năm 2024: 6 mục ngăn bằng || và dấu rỗng ở cuối, khác parser cũ 7 mục | có ô CMND. Bổ sung parserVersion=2 cho mẫu này, giữ version=1 cũ; Rules chấp nhận [1,2]. Cần người dùng Publish lại toàn bộ firebase/firestore.rules để gửi hồ sơ version=2, rồi Run/đối chiếu họ tên/ngày sinh/gửi. Storage Rules/JSON không đổi; chưa tự build/test/commit hoặc nghiệm thu. Xem C05-FILES và C05-CHECKLIST mục C05-FMT01–FMT04.

## Người dùng xác nhận QR/gửi hồ sơ và chuyển sang kiểm tra Admin — 25/10/2026

Người dùng xác nhận đã cập nhật, quét/đọc QR thành công, đối chiếu thông tin đều đúng, giao diện ổn và gửi hồ sơ thành công. Đây là kết quả do người dùng thực hiện; Codex không chạy lại hoặc tự xem dữ liệu thật. Phần cấp tài khoản Admin, đăng nhập, xem ảnh, chấp thuận/bổ sung/từ chối/thu hồi và kết quả ở tài khoản người dùng vẫn chờ kiểm tra; chưa đánh dấu toàn bộ C05 đạt.

Đã bổ sung docs/C05-ADMIN-GUIDE.md: đăng nhập Google riêng để có UID, dừng trước chọn role, cấp adminAccess enabled=true và users role ADMIN/accountStatus ACTIVE bằng Console, đăng nhập lại rồi Duyệt hồ sơ. Có trình tự xem ảnh, chọn kết quả đối chiếu mô phỏng, nhận xét, xác nhận và kiểm tra kết quả ở chủ hồ sơ.

Các ca hồi quy/Rules/cạnh tranh/timeout/thiết bị không tự được coi đạt chỉ từ xác nhận quét và gửi thành công. Không chạy build/test, không Console/deploy/stage/commit/push trong đợt hướng dẫn này.

## Bổ sung trang chính/điều hướng và cửa vào Admin — 25/10/2026

Đã bổ sung mã nguồn theo ảnh/yêu cầu hiện hành: topbar xanh logo + vai trò một dòng, trang chính theo vai trò, Tài khoản người thuê/Admin và thanh điều hướng dùng chung Main/C05. Người dùng chọn giữ các mục chưa triển khai ở vị trí mẫu, khóa và ghi Sắp có. Không có số việc/hàng đợi DEMO; số SUBMITTED Admin đọc server, giới hạn 50 mỗi loại và có nhãn 50+ nếu chạm giới hạn.

Admin có đủ grant/User ADMIN/ACTIVE → Tổng quan → Kiểm tra hồ sơ hoặc tab Xử lý → chọn loại và tên → chi tiết/ảnh/quyết định. Hướng dẫn cấp quyền giữ nguyên; nếu đang ở màn chọn vai trò sau khi quản lý cấp, có Kiểm tra lại tài khoản. Không chọn/sửa vai trò người dùng thành Admin trong client. Xem [C05-ADMIN-GUIDE](C05-ADMIN-GUIDE.md).

Đợt này không thay Rules/schema/Firebase: nếu bản C05/parserVersion 2 đang gửi thành công thì chỉ cần Sync/Run mã nguồn mới. Thử lại từng vai trò, điều hướng/nháp/xoay/đăng xuất và Admin đọc/duyệt/kết quả theo các mục C04-N01–N07 và C05-N01–N06. Chi tiết [C04-C05-NAVIGATION](C04-C05-NAVIGATION.md). Chưa tự coi phần bổ sung đạt; Codex chỉ rà tĩnh, không build/test/app/kiểm thử Rules hoặc Console/deploy/stage/commit/push.

## Admin đã vào hàng đợi, bản sửa thao tác quyết định — 25/10/2026

Người dùng xác nhận đã cấp lại đúng hai document, vào Admin và tải hồ sơ chờ thành công. Chấp thuận chưa lưu; app vẫn cuộn/quay lại được. Đã sửa validation bắt nhập nhận xét cho mọi quyết định và vị trí phản hồi bị khuất: Chấp thuận không cần gõ; Bổ sung/Từ chối/Thu hồi mới mở ô lý do; trạng thái lưu/lỗi ngay cạnh nút, có thông báo thành công và tải lại danh sách.

Không đổi Rules/schema/cấu hình Firebase; Chấp thuận ghi dòng trạng thái chuẩn vào reason để tương thích bản Rules hiện có. Chỉ cần Sync/Run mã nguồn mới, thử C05-D01–D06 theo [C05-CHECKLIST](C05-CHECKLIST.md) và [C05-ADMIN-GUIDE](C05-ADMIN-GUIDE.md). Nếu còn lỗi lấy các dòng GrandMaReview, không gửi giấy tờ/UID/raw QR. Chưa tự coi quyết định/kết quả đã đạt; Codex không build/test/app/Console/deploy/stage/commit/push.

## Người dùng duyệt quyết định; cải tiến một trang Hồ sơ — 25/10/2026

Đã ghi nhận người dùng kiểm tra lại các ca sau bản sửa quyết định và duyệt, D01–D06 đạt theo xác nhận tổng thể. Đề xuất tiếp theo đã triển khai mã nguồn: Trang chủ/Tài khoản người dùng chỉ có Xem hồ sơ; trong Hồ sơ phân vùng thông tin/công việc/số điện thoại và khối Xác minh với trạng thái/nút theo tiến trình. Đã duyệt mở Xem lại chỉ đọc, đang chờ mở bản đã gửi, còn các trạng thái cần hành động mở đúng wizard. Admin giữ lối duyệt riêng.

Không cần sửa Console/Publish Rules/schema. Chỉ Sync/Run và thử C04-P01–P06 trong [C04-CHECKLIST](C04-CHECKLIST.md); phần UI mới chưa được người dùng xác minh. Trạng thái đọc lại khi mở/foreground, nháp công việc được giữ. Không build/test/app/stage/commit/push trong đợt Codex này; chi tiết [C04-C05-NAVIGATION](C04-C05-NAVIGATION.md).
