# C05 — hồ sơ xác minh, QR, ảnh riêng và xét duyệt

**Xác nhận hiện hành 26/10/2026:** người dùng đã duyệt các luồng hoạt động khác cùng bản chốt C07; bản sửa EMPLOYER nằm trong commit `08d2211`, khớp local/origin/main sau fetch. Ghi nhận hồi quy tổng thể, các ca khó chưa có kết quả riêng tiếp tục theo dõi. Xem [C07-ACCEPTANCE](C07-ACCEPTANCE.md); các câu chờ kiểm lại bên dưới là lịch sử.

**26/10/2026 — cập nhật theo phản hồi mới nhất:** đã gỡ bố cục ghim nút, đưa về khối Gửi hồ sơ cũ. Nút đang soạn bấm được để báo đúng điều kiện thiếu, chỉ khóa khi đang sending; không bỏ kiểm tra thực sự trước upload/submit. Giữ nút Sửa ảnh căn cước sau đọc QR. Xem EF09–EF11 trong [báo cáo sửa EMPLOYER](C05-EMPLOYER-SUBMISSION-FIX.md), đang chờ người dùng kiểm lại.

**26/10/2026 — sửa xác minh EMPLOYER, chờ kiểm tra lại:** nút gửi trước đó dùng chung consent và thiếu giải thích khi bị khóa. Đã tách hộp đồng ý gửi danh tính riêng, bỏ phần hướng dẫn nghỉ hưu khỏi màn Người thuê và thống nhất lý do chặn nút. Backend vốn chỉ đòi CCCD cho EMPLOYER; không cần đổi Rules/Console. Xem [C05-EMPLOYER-SUBMISSION-FIX](C05-EMPLOYER-SUBMISSION-FIX.md). Nghiệm thu trước đây bên dưới không tự xác nhận bản sửa mới.

25/10/2026 — **người dùng đã chốt toàn bộ bản hiện tại**, gồm C05/Hồ sơ thống nhất và sửa sau pull. Nền đối chiếu local/GitHub là `11fcdeb`, bản sửa cấu hình/resource nằm trong `a63734e`. Xem [C05-ACCEPTANCE](C05-ACCEPTANCE.md); các trạng thái chờ trong lịch sử được thay bằng xác nhận mới, không tự coi các biến thể kỹ thuật chưa tái hiện đã đạt.

## Kết quả triển khai

- Trang chủ/Tài khoản người dùng chỉ có **Xem hồ sơ**; trong Hồ sơ có khối xác minh với trạng thái và một nút phù hợp. Chưa gửi mở wizard; đang chờ/đã duyệt mở bản hiện hành chỉ đọc. Admin vào **Tổng quan → Kiểm tra hồ sơ** hoặc tab **Xử lý**. Yêu cầu lưu/bỏ thay đổi nghề nghiệp trước khi mở để không tạo hai bản nền khác nhau.
- Chụp bằng ứng dụng camera của thiết bị, xin quyền đúng lúc, hủy/từ chối/cài đặt; có thể chọn ảnh căn cước có sẵn. Xem lại, dùng/chọn lại ảnh, đọc QR qua ML Kit bundled. Một ảnh cần một QR hợp lệ trong định dạng hỗ trợ.
- Parser hỗ trợ hai cấu trúc tường minh: 7 trường với `|` (có ô CMND cũ) và 6 trường với `||` (không có ô CMND cũ, theo mẫu người dùng cung cấp); chấp nhận dấu phân cách rỗng ở cuối. Vẫn kiểm số định danh 12 chữ số, họ tên và ngày sinh `ddMMyyyy` hợp lệ theo lịch, không trong tương lai. Không suy đoán ngày thiếu; không lưu raw QR, địa chỉ, giới tính hay trường không dùng. Không lấy tuổi làm điều kiện duyệt.
- Họ tên/ngày sinh từ QR chỉ đọc; tên hiển thị C04 vẫn riêng biệt. SEEKER chọn giấy nghỉ hưu hoặc BHXH mô phỏng, 1–3 ảnh; EMPLOYER gửi căn cước/danh tính mẫu, không cần giấy nghỉ hưu và không xác minh doanh nghiệp.
- Ảnh JPEG/PNG đầu vào tối đa 10 MB; đọc nội dung, áp dụng hướng ảnh, giảm cạnh dài tối đa 3072 và nén JPEG bỏ EXIF. Có xem ảnh và phóng to/kéo, thao tác phóng to qua accessibility. Bản xem thu nhỏ được giới hạn kích thước giải mã.
- Có thể chuẩn bị ảnh khi chưa OTP; gửi cần số Auth khớp User, ACTIVE và thông tin nghề nghiệp đã lưu đủ. Chưa hỗ trợ giữ nháp xuyên process: ảnh/QR không vào preferences, SQLite, SavedStateHandle hoặc Intent. Ảnh camera tạm ở cache riêng, xóa khi xử lý/hủy/đóng; file sót sau process death được dọn khi mở lại màn.
- Trước gửi có mục đích sử dụng qua **Cách xét duyệt** và checkbox đồng ý. Không chèn watermark hoặc tự tạo ảnh giấy tờ.
- Tạo request STAGING bất biến, upload từng ảnh, rồi transaction đổi SUBMITTED + cập nhật User và phiên bản định danh trong profile. Retry cùng request ID và nội dung; file đã có chỉ tái dùng khi MD5 khớp, không ghi đè. Không tự gửi khi có mạng trở lại.
- Có **Dừng chờ gửi**: hủy upload còn hủy được, vô hiệu hóa callback cũ và bắt tải lại trước gửi tiếp. Transaction đã tới server có thể vẫn hoàn tất; UI không tuyên bố đã hủy hồ sơ trên server. Lỗi phản hồi sau ghi được đối chiếu lại bằng User/current request.
- Bản gửi khóa ảnh/nội dung. Bổ sung hoặc thay căn cước tạo request ID mới, previousRequestId và identityProfileVersion mới; đặt SUBMITTED, approvedIdentityProfileVersion = 0. Quyết định cũ không mở quyền cho bản mới; không xóa lịch sử/đơn/ca.
- Admin xem tối đa 50 hồ sơ SUBMITTED mỗi loại; xử lý rồi tải lại để lấy các hồ sơ tiếp. Có tìm bằng UID hoặc mã request, xem bản trước và ảnh riêng, đối chiếu mô phỏng MATCH/NOT_MATCH/INSUFFICIENT và xác nhận đã xem ảnh. Chấp thuận chỉ khi MATCH, không nhập nhận xét; SEEKER APPROVED, EMPLOYER VERIFIED. NEEDS_INFO/REJECTED và thu hồi mới cần ô lý do.
- Quyết định transaction đối chiếu current request/version, tăng User.version, cập nhật trạng thái và tạo một thông báo bất biến `{requestId}_1`. Hai Admin hoặc bản cũ không ghi đè quyết định. Không tự xét mình. Nhận xét không được chứa chuỗi 12 chữ số căn cước.
- Thu hồi kết quả hiện hành có lý do: ghi verificationRevocations bất biến, User REVERIFY_REQUIRED/version+1, approved version = 0 và thông báo `{requestId}_2`. Request đã duyệt giữ nguyên quyết định lịch sử; người dùng gửi bản mới để xét lại.
- Chủ hồ sơ xem kết quả/lý do và mở hồ sơ từ thông báo. Đọc server khi mở lại; listener User khi màn đang mở nhận phiên bản mới, không tự bỏ nháp. Không gửi SMS/FCM cho quyết định.
- FLAG_SECURE tại màn giấy tờ và hộp phóng to; không ghi log QR, số căn cước, ảnh hoặc token. Lấy ảnh qua SDK có Rules, không gọi getDownloadURL hoặc phát hành URL chia sẻ.

## Files

Mới:
- `domain/model/CardIdentity.java`: parser và snapshot tối giản.
- `data/repository/VerificationImages.java`: giới hạn/giải mã/nén ảnh và fingerprint cho retry.
- `data/repository/CardQrReader.java`: đọc QR từ ảnh nguồn, thử vùng có QR/các vùng chồng lấp khi cần, giải phóng bitmap/scanner và trả mã lỗi riêng.
- `data/repository/VerificationRepository.java`: đọc server, staging/upload/submit, danh sách/tìm/xem, quyết định/thu hồi/thông báo.
- `viewmodel/VerificationViewModel.java`: nháp trong bộ nhớ, kiểm soát UID/generation, điều phối async, retry và đối chiếu kết quả.
- `ui/verification/VerificationActivity.java`, `ZoomDocumentView.java`: màn người dùng/Admin, camera/picker, xem/phóng to và biểu mẫu xét duyệt.
- `res/layout/activity_verification.xml`, `res/values/verification_strings.xml`, `res/xml/verification_paths.xml`.
- Đợt chỉnh giao diện bổ sung `res/values/verification_styles.xml`, `res/drawable/verification_section.xml`.
- `docs/C05-FILES.md`, `C05-CONSOLE-SCHEMA.md`, `C05-CHECKLIST.md`.

Sửa:
- `app/build.gradle.kts`: ML Kit barcode-scanning 17.3.0 bundled.
- `AndroidManifest.xml`: camera tùy chọn, activity nội bộ, FileProvider chỉ thư mục camera cache.
- `ProfileRepository.java`: update các field nghề nghiệp thay vì set đè toàn bộ profile; giữ dữ liệu định danh C05.
- `ProfilePanel.java`, `profile_content.xml`: nút mở xác minh/duyệt, đọc lại C04 khi trở về; bỏ ghi chú chưa xác minh cố định đã lỗi thời.
- `firebase/firestore.rules`, `firebase/storage.rules`: quyền riêng, allowlist, phiên bản, nguyên tử và ảnh bất biến. Quyền Admin chỉ được mở cho các thao tác C05, không ghi tùy ý các collection khác.
- README và SETUP: trạng thái bàn giao hiện hành.

## Giới hạn và căn cứ

Firestore Rules không đọc được sự tồn tại/nội dung byte trong Storage. App chỉ submit sau upload thành công; client đã sửa có thể khai đường dẫn đúng mà chưa tải ảnh. Admin phải xem được đủ ảnh và đối chiếu thủ công trước quyết định; Rules kiểm quyền/metadata/phiên bản, không chứng minh ảnh thật, QR thật, sự đồng ý thật hay Admin thực sự đã nhìn ảnh. Không có backend/eKYC/BHXH thật. QR dạng khác được báo chưa hỗ trợ, không chuyển sang nhập tay âm thầm.

Nháp bỏ dở có thể để lại request STAGING/ảnh trên Firebase. Client không tự xóa lịch sử hay chạy dọn server; nhóm quản trị đối chiếu và dọn dữ liệu thử theo chính sách đã chốt. Không có cơ chế ứng tuyển trong C05: các collection nghiệp vụ chưa triển khai vẫn default-deny; luồng sau phải kiểm APPROVED đúng current request/version và OTP, không chỉ nhìn nhãn UI.

Rà soát nguồn/XML/resource references và diff; `default_web_client_id` là resource được Google Services sinh, không phải thiếu resource C05. Không chạy compiler/Gradle/build/lint/test/emulator/SMS hoặc kiểm thử Rules. Không sửa Console, deploy, stage, commit, push. `.idea/misc.xml` đang có thay đổi JDK từ trước được giữ nguyên. Chưa khẳng định runtime, giao diện và Rules đã đạt; người dùng xác minh theo checklist.

Nguồn kỹ thuật đã đối chiếu: [ML Kit Android](https://developers.google.com/ml-kit/vision/barcode-scanning/android), [Android TakePicture](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.TakePicture), [ImageDecoder](https://developer.android.com/reference/android/graphics/ImageDecoder), [Firestore transactions](https://firebase.google.com/docs/firestore/manage-data/transactions), [Storage conditions/cross-service](https://firebase.google.com/docs/storage/security/rules-conditions), [Storage operations](https://firebase.google.com/docs/storage/security/core-syntax).

C02-F01 và các mục theo dõi cũ vẫn giữ nguyên. Giọng đọc thuộc C22.

## Chỉnh bố cục và nhận ảnh C05 — 25/10/2026

Người dùng xác nhận Sync/Run thành công nhưng giao diện nhiều nút/ghi chú sát nhau; chụp căn cước xong vùng xem lại nằm dưới mục bằng chứng, chọn ảnh bằng chứng chưa thấy ảnh.

Rà nguồn cho thấy bản cũ dùng một candidate/preview chung phía dưới phần bằng chứng và biến candidateIdentity vừa làm loại ảnh đang xem, vừa làm đích của lần chọn tiếp theo. Bằng chứng còn cần bấm Dùng ảnh ở vùng chung trước khi vào danh sách. Đây là các điểm gây nhầm và thao tác thừa đã xác định trong mã; chưa có log để kết luận mọi trường hợp không đọc được file đều do cùng nguyên nhân.

Đã sửa:
- Tách các khối có nền/viền/tiêu đề: Trạng thái, 1. Căn cước, 2. Bằng chứng nghỉ hưu, 3. Gửi hồ sơ; EMPLOYER bỏ bằng chứng và gửi ở bước 2. Kết quả và phần Admin/chi tiết/quyết định cũng có vùng riêng.
- Padding mỗi khối 16dp; các khối cách 24dp; nút cách nhau 12dp, chạm tối thiểu 56dp. Cả nút XML lẫn nút sinh khi hiện ảnh/danh sách đều áp dụng khoảng cách. Bỏ thông báo trống/thừa, lỗi/progress ảnh nằm đúng khối; giải thích dài giữ trong Cách xét duyệt.
- Hai launcher chọn ảnh riêng cho căn cước/bằng chứng, camera cố định đích căn cước. pendingImageTarget độc lập candidate; URI kết quả chỉ nhận khi đúng yêu cầu/UID còn sống, không suy ra đích từ ảnh đang xem. Không mở quyền hoặc đổi Rules.
- Căn cước xem lại ngay trong khối Căn cước, Dùng ảnh và đọc QR chỉ xử lý căn cước. Ảnh cũ giữ nguyên cho tới khi dùng bản mới; hủy/đọc lỗi không biến căn cước thành bằng chứng.
- Bằng chứng JPEG/PNG hợp lệ được thêm vào nháp và hiển thị ngay trong khối bằng chứng, cập nhật số ảnh n/3; không cần bấm Dùng ảnh thêm lần nữa. Có phóng to, bỏ ảnh, thêm lại; lỗi đọc/ảnh quá giới hạn báo tại khối và giữ các ảnh đã chọn. Chỉ tải lên Firebase khi bấm Gửi hồ sơ như trước.
- Sau thao tác ảnh, cuộn về đúng khối vừa xử lý thay vì dùng preview chung bên dưới. Xoay giữ vị trí cuộn và số sự kiện trong Bundle (không có ảnh, QR hoặc URI); nháp vẫn chỉ trong ViewModel.

Files thay đổi đợt này: activity_verification.xml, verification_strings.xml, VerificationActivity.java, VerificationViewModel.java; thêm verification_styles.xml và verification_section.xml; cập nhật báo cáo/checklist/SETUP/README. Không thay schema, Firestore/Storage Rules hay cấu hình Firebase trong đợt này; không cần Publish Rules lại chỉ vì đợt sửa giao diện.

Đã rà nguồn, phân tích XML, đối chiếu ID/style và diff. Chưa chạy build/test/lint/app/emulator hoặc tự kiểm thử ảnh; không stage/commit/push/deploy. Người dùng cần kiểm tra lại các mục UI bổ sung bên dưới, không coi lần Run trước là đã nghiệm thu bản sửa này.

## Cập nhật đọc QR căn cước — 25/10/2026

Người dùng xác nhận giao diện đã cải thiện và phần ảnh thành công, còn bước căn cước báo “Chưa đọc được QR căn cước. Hãy chụp rõ một mã QR rồi thử lại”. Người dùng đã cung cấp đúng thông báo qua câu hỏi làm rõ. Thông báo này thuộc bước scan/parse sau khi nhận ảnh, không chứng minh camera không chụp được.

Rà nguồn thấy thông báo cũ gộp các trường hợp ML Kit không tìm QR, đọc nhiều mã, lỗi bộ đọc và parser không chấp nhận dữ liệu. Ảnh đưa vào scan trước đây là JPEG đã nén lại và được giải mã bằng hàm preview RGB_565. Chưa có log kết quả scanner thực tế để khẳng định trường hợp nào đã xảy ra trên thiết bị của người dùng; không quy kết mọi lỗi cho ảnh mờ.

Thay đổi:
- VerificationImages.prepare tách ảnh JPEG chuẩn hóa để xem/gửi khỏi nguồn ảnh căn cước tạm trong RAM. Chỉ căn cước giữ nguồn trước tái nén cho tới khi đọc/bỏ/thay ảnh; bằng chứng vẫn nhận ngay như bản đã sửa.
- CardQrReader đọc ảnh nguồn đã xử lý hướng/EXIF bằng ImageDecoder/sRGB, giới hạn cạnh dài 3072 để kiểm soát bộ nhớ, không dùng bitmap RGB_565 của UI. Lượt đầu toàn ảnh tối đa 2048; nếu chưa giải mã được, dùng bounding box tiềm năng của ML Kit hoặc tối đa 5 vùng chồng lấp, quét tuần tự. Vùng nhỏ có thể được đổi kích thước để thử đọc; không khôi phục được chi tiết vốn mất do mờ/lóa.
- Các vùng cắt chỉ dùng cho bộ đọc. Ảnh căn cước gửi xét duyệt vẫn là toàn khung đã chuẩn hóa, không đổi thành ảnh QR cắt riêng. Không đổi giới hạn 10 MB/JPEG-PNG, schema hoặc quyền Firebase.
- Kết quả QR trùng nhau giữa các vùng được gộp; nhiều nội dung QR khác nhau báo cần chụp riêng. Không chọn đại mã đầu tiên. Parser vẫn chỉ hỗ trợ 7 trường phân cách |, số định danh 12 chữ số và ngày sinh ddMMyyyy hợp lệ. Chỉ trim khoảng trắng ở biên số/ngày; không đoán ngày, không thêm parser cho định dạng mới khi chưa có đặc tả.
- CardIdentity trả ParseFailure theo loại cấu trúc/số định danh/họ tên/ngày sinh. Không còn catch rỗng rồi báo chung là chụp không rõ.
- VerificationViewModel giữ generation/UID và bỏ kết quả muộn; chỉ cập nhật danh tính khi toàn bộ đọc/parse hợp lệ. Thất bại giữ ảnh xem lại; thông tin hợp lệ trước đó không tự đổi. Nguồn scan không vào Bundle/preferences/SQLite/Intent; tham chiếu được bỏ khi dùng/bỏ/thay ảnh, rời màn hoặc đổi phiên.
- Debug Logcat tag GrandMaQR chỉ ghi result=OK/NOT_FOUND/UNREADABLE/MULTIPLE/FORMAT/DOCUMENT_NUMBER/NAME/BIRTH_DATE/IMAGE/ENGINE. Không log raw QR, số giấy tờ, ngày sinh, URI/path, ảnh hoặc thông điệp exception. Release không ghi dòng này.

Ý nghĩa thông báo mới:
| Mã | Hướng xử lý |
|---|---|
| NOT_FOUND | Chụp đúng mặt có QR, gần hơn, đủ sáng. |
| UNREADABLE | Có vùng nghi là QR nhưng chưa giải mã; tránh lóa và lấy nét lại. |
| MULTIPLE | Chụp riêng một căn cước; nhiều nội dung QR khác nhau không được tự chọn. |
| FORMAT | QR đã đọc ra nhưng cấu trúc chưa được hỗ trợ; chụp lại cùng mã không tự làm đổi định dạng. |
| DOCUMENT_NUMBER / NAME / BIRTH_DATE | Dữ liệu QR không đạt kiểm tra từng field; không tự sửa hoặc tự điền thông tin. |
| IMAGE / ENGINE | Lỗi xử lý ảnh/bộ đọc, tách biệt với dữ liệu QR. |

Đã đối chiếu [ML Kit Android](https://developers.google.com/ml-kit/vision/barcode-scanning/android) và [BarcodeScannerOptions.Builder](https://developers.google.com/android/reference/com/google/mlkit/vision/barcode/BarcodeScannerOptions.Builder) cho enableAllPotentialBarcodes của thư viện bundled 17.3.0 đang có. Không thêm dependency mới. Chưa có kết quả runtime trên ảnh thật sau đợt sửa này; Codex chỉ rà nguồn, không build/test/lint/chạy app/đọc log thiết bị, không stage/commit/push/deploy. Người dùng tự Run lại; không cần Publish Rules lại vì đợt sửa chỉ ở Android.

## Cập nhật tương thích cấu trúc QR người dùng — 25/10/2026

Người dùng cung cấp result=FORMAT, xác nhận thẻ cấp năm 2024 và mô tả chuỗi văn bản đã che dữ liệu theo cấu trúc:

`ID||FULL_NAME||ddMMyyyy||GENDER||ADDRESS||ISSUED_DATE||||`

Đây là 6 mục có dữ liệu, ngăn bằng dấu đôi ||, không có ô CMND cũ. Parser trước đó yêu cầu đúng 7 mục ngăn bằng | và đặt họ tên ở vị trí sau ô CMND cũ nên từ chối bằng FORMAT. Lỗi tương thích đã có căn cứ từ cấu trúc người dùng cung cấp; không có căn cứ kết luận CCCD không hợp lệ, ảnh mờ hay mọi thẻ cấp năm 2024 dùng cùng cấu trúc.

Đã sửa CardIdentity.parse:
- Nhận riêng mẫu 6 trường ngăn bằng ||, loại các mục rỗng bổ sung ở cuối, lấy ID ở vị trí 0, họ tên 1 và ngày sinh 2. Đánh parserVersion=2.
- Giữ mẫu 7 trường ngăn bằng |, kể cả ô CMND cũ rỗng; lấy họ tên 2/ngày sinh 3, parserVersion=1. Không thay tất cả || thành | hoặc xóa rỗng giữa chuỗi vì sẽ làm lệch trường của mẫu cũ.
- Không bỏ mục có dữ liệu ở cuối để ép khớp, không tìm đại một ngày hay một tên trong chuỗi. Vẫn kiểm ID 12 chữ số, tên và ngày hợp lệ; không đọc thêm địa chỉ/giới tính/ngày cấp vào hồ sơ hoặc thông báo.
- Nếu vẫn FORMAT, debug tag GrandMaQR thêm dòng `structure kind=... pipeFields=... trailingEmpty=... semicolons=... tabs=... lineBreaks=...`. Chỉ nhãn cố định và số đếm; không có giá trị field, độ dài riêng từng field, ảnh, URI/path, raw QR hoặc dữ liệu định danh. Không tự gửi diagnostic lên Firebase; release không log.

Firestore Rules hiện cho identitySnapshot.parserVersion thuộc [1, 2], các quyền/allowlist khác giữ nguyên. **Người dùng cần Publish lại toàn bộ firebase/firestore.rules trước khi gửi hồ sơ parserVersion=2. Storage Rules và JSON không đổi.** Hồ sơ/parserVersion=1 đã có vẫn hợp lệ; không sửa/xóa lịch sử để nâng phiên bản.

Đây là hỗ trợ cấu trúc do người dùng cung cấp và đã che thông tin. Tài liệu công khai được tham khảo mô tả các loại thông tin QR, không được dùng như đặc tả đảm bảo chuỗi 6/7 trường cho mọi thẻ. Không thêm SDK eKYC, dịch vụ ngoài, OCR thay thế hay tự cấp trạng thái duyệt.

Đã rà mã nguồn, vị trí field và diff. Chưa build/test/chạy app/kiểm thử Rules; không stage/commit/push/deploy hoặc sửa Console. Người dùng cần Run lại để xác nhận đọc đúng họ tên/ngày sinh và thử gửi sau Publish; C05 vẫn chưa nghiệm thu.

## Người dùng xác nhận QR/gửi hồ sơ và chuyển sang kiểm tra Admin — 25/10/2026

Người dùng xác nhận đã cập nhật, quét/đọc QR thành công, đối chiếu thông tin đều đúng, giao diện ổn và gửi hồ sơ thành công. Đây là kết quả do người dùng thực hiện; Codex không chạy lại hoặc tự xem dữ liệu thật. Phần cấp tài khoản Admin, đăng nhập, xem ảnh, chấp thuận/bổ sung/từ chối/thu hồi và kết quả ở tài khoản người dùng vẫn chờ kiểm tra; chưa đánh dấu toàn bộ C05 đạt.

Đã bổ sung docs/C05-ADMIN-GUIDE.md: đăng nhập Google riêng để có UID, dừng trước chọn role, cấp adminAccess enabled=true và users role ADMIN/accountStatus ACTIVE bằng Console, đăng nhập lại rồi Duyệt hồ sơ. Có trình tự xem ảnh, chọn kết quả đối chiếu mô phỏng, nhận xét, xác nhận và kiểm tra kết quả ở chủ hồ sơ.

Các ca hồi quy/Rules/cạnh tranh/timeout/thiết bị không tự được coi đạt chỉ từ xác nhận quét và gửi thành công. Không chạy build/test, không Console/deploy/stage/commit/push trong đợt hướng dẫn này.

## Bổ sung điều hướng và giao diện Admin — 25/10/2026

Đã nối tài khoản Admin từ Main vào Tổng quan hàng đợi và tab Xử lý thật, dùng topbar/logo/nhãn vai trò và thanh điều hướng chung với hai vai trò thường. Tổng quan đếm server hồ sơ SUBMITTED theo hai loại, tối đa 50 mỗi loại theo quyền truy vấn hiện có, hiển thị 50+ khi chạm giới hạn; lỗi tải không hiện số 0. Có Tải lại số đếm và tải lại sau khi trở về từ Xử lý. Không phải thống kê toàn hệ thống hoặc cập nhật thời gian thực.

Khi chọn hồ sơ, ẩn hàng đợi để hiện chi tiết/ảnh/quyết định trong một màn tập trung. Quay lại/tab Xử lý về danh sách và hỏi trước khi bỏ nhận xét chưa lưu; tab Tổng quan/Tài khoản trả Main qua extra chỉ chứa trang đích, không chứa giấy tờ. Chủ hồ sơ Quay lại từ chi tiết vẫn về phần xác minh và giữ nháp trong bộ nhớ. Các mục Admin chưa triển khai được khóa/Sắp có theo lựa chọn người dùng, không có số DEMO.

Sửa `VerificationActivity.java`, `VerificationViewModel.java`, `activity_verification.xml`; dùng lớp/layout/resources `ui/navigation`, `role_*`, `navigation_*` mới. ProfileRepository bổ sung đếm server và thông báo grant/User ADMIN không khớp; ProfileViewModel quản lý đích trang/số đếm và loại callback cũ khi đổi UID. Toàn bộ file/phạm vi: [C04-C05-NAVIGATION](C04-C05-NAVIGATION.md). Hướng dẫn Admin đã đổi theo đường vào mới.

Đợt này không sửa Rules/schema/cấu hình Firebase hoặc logic QR/upload/transaction quyết định. Rà tĩnh nguồn/XML/resources/diff; chưa build/test/app/kiểm thử Rules/Console/deploy/stage/commit/push. C05-N01–N06 đang chờ người dùng; không coi phần Admin đã đạt từ kết quả QR/gửi trước đó.

## Chấp thuận không cần nhập nhận xét, phản hồi tại nút — 25/10/2026

Người dùng đã sửa hai document quyền, vào Admin/tải hồ sơ chờ thành công; bấm Chấp thuận chưa duyệt. Làm rõ app vẫn cuộn/quay lại được, chưa có bằng chứng ANR/crash. Rà nguồn thấy ViewModel chặn mọi quyết định khi reason trống, còn lỗi đặt trên đầu trang, khớp trường hợp không nhập nhận xét mà bấm lại không thấy tiến triển. Chưa có log hay kết quả chạy để kết luận mọi lỗi lưu server đã hết.

- `VerificationActivity.java`/`activity_verification.xml`/`verification_strings.xml`: ẩn ô nhận xét thường trực; Bổ sung/Từ chối/Thu hồi mới mở vùng Lý do và cuộn tới đó, có xác nhận/hủy và lỗi tại ô. Chấp thuận xác nhận trực tiếp. Điều kiện đủ ảnh/đã đối chiếu/MATCH vẫn giữ; trạng thái tải/lưu/lỗi đặt trong vùng quyết định, có spinner khi đang xử lý. Callback xác nhận kiểm bản hồ sơ đang chọn để tránh áp quyết định sang bản khác.
- `VerificationViewModel.java`: chấp thuận bỏ validation reason, các hành động còn lại kiểm trống/độ dài/số căn cước; giữ hành động/lý do trong RAM khi xoay, xóa khi đổi hồ sơ/UID. Lưu thành công báo kết quả rõ rồi tải hàng đợi; lỗi không tự replay. Tag debug GrandMaReview chỉ ghi giai đoạn/mã lỗi cố định.
- `VerificationRepository.java`: chuẩn hóa APPROVED/VERIFIED sang reason cố định “Hồ sơ đã được chấp thuận.”, không dùng nội dung đang gõ cho quyết định khác. Đây là dòng trạng thái để tương thích Rules yêu cầu reason không rỗng, không phải nhận xét của Admin. Kiểm lại status/role/lookup/lý do trước transaction; request/User/notification vẫn ghi nguyên tử. UI bỏ lặp dòng chuẩn, giữ lý do và nhận xét lịch sử.
- Cập nhật hướng dẫn Admin/schema/checklist/README/SETUP. Không đổi Rules, schema, quyền, QR hoặc upload; không cần Publish lại.

Rà tĩnh nguồn/XML/resource/diff; không build/test/app/Console/deploy/stage/commit/push. Các ca mới C05-D01–D06 đang chờ người dùng. C05 chưa hoàn tất nghiệm thu quyết định/kết quả.

## Người dùng duyệt bản sửa quyết định; cải tiến Hồ sơ thống nhất — 25/10/2026

Người dùng xác nhận đã test lại các case sau chỉnh sửa, mọi thứ đều đạt. D01–D06 được ghi nhận đạt theo xác nhận tổng thể này; không bổ sung thiết bị/API/log hay kết quả kiểm thử độc lập. Trạng thái chờ kiểm tra quyết định trong lịch sử phía trên được thay thế bởi xác nhận mới; các nhánh chưa tái hiện tiếp tục theo dõi riêng.

Đã thực hiện đề xuất tiếp theo: một lối vào Hồ sơ cho SEEKER/EMPLOYER; phân thẻ thông tin/công việc/số điện thoại và khối xác minh có một dòng trạng thái + một nút. Loại bỏ nút Hồ sơ xác minh song song ở Trang chủ/Tài khoản người dùng. Admin giữ Kiểm tra hồ sơ riêng.

Đã duyệt/đang chờ mở thẳng bản gửi hiện hành ở chế độ xem, không tự mở mẫu gửi mới. Đã duyệt có Cập nhật giấy tờ riêng trong màn xem để giữ khả năng gửi bản thay thế C05, có xác nhận; trạng thái chỉ đổi sau khi gửi. Bổ sung/từ chối/thu hồi vào đúng luồng cũ. Metadata tóm tắt đọc server khi mở Hồ sơ/foreground/trở về, giữ nháp công việc và chặn callback UID cũ. Không đưa số căn cước/ảnh vào trang Hồ sơ thường.

Sửa ProfileRepository/ProfileViewModel/ProfilePanel/MainActivity/RoleHomePanel, các layout/profile strings/styles; VerificationActivity/ViewModel nhận cờ boolean mở bản hiện hành, đọc tài liệu thật sau kiểm phiên, không nhận giấy tờ trong Intent. Xem [C04-C05-NAVIGATION](C04-C05-NAVIGATION.md) cho bảng trạng thái/nút. Không đổi Rules/schema, QR/upload/transaction xét duyệt. Chưa build/test/app/commit/push; phần UI mới chờ C04-P01–P06.

## Đối chiếu commit nhận về và sửa cấu hình — 25/10/2026

Đã xác nhận `2552a04` tại local/main và GitHub main. Commit có mã C05/Hồ sơ thống nhất nhưng root build.gradle.kts bị thay bằng nội dung module app; module app thiếu dependency QR, profile_strings/styles thiếu 19 chuỗi và hai style đã được mã nguồn tham chiếu. Đã khôi phục đúng root plugin declarations, đưa ML Kit vào app và bổ sung resources; README cũng được cập nhật hiện trạng. Không đổi phiên bản/nghiệp vụ/Rules/JDK/Console, không Sync/build/test hoặc commit/push. Chi tiết và đúng đường dẫn file trong [C05-COMMIT-REPAIR](C05-COMMIT-REPAIR.md). Người dùng Sync/Run lại để xác nhận bản sửa sau pull.
