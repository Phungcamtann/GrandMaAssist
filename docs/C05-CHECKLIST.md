# C05 — checklist người dùng

**26/10/2026:** bổ sung EF01–EF05 trong [checklist sửa gửi EMPLOYER](C05-EMPLOYER-SUBMISSION-FIX.md), chờ người dùng kiểm lại. EMPLOYER xác nhận đồng ý trong hộp Gửi danh tính, không tick phần nghỉ hưu; SEEKER giữ điều kiện bằng chứng/đồng ý. Không tự đánh dấu ca mới dựa trên nghiệm thu C05 cũ.

25/10/2026 — người dùng đã xác nhận QR/gửi, Admin/xét duyệt và nay chốt **đã duyệt hết tất cả bản hiện tại**, gồm Hồ sơ thống nhất và sửa Gradle sau pull. Nghiệm thu theo xác nhận người dùng trong [C05-ACCEPTANCE](C05-ACCEPTANCE.md); các ca Rules/cạnh tranh/thiết bị chưa tái hiện tiếp tục theo dõi riêng. Không có kết quả runtime/Rules do Codex chạy.
Làm thiết lập theo C05-CONSOLE-SCHEMA.md trước. Ghi thiết bị/API và kết quả riêng tư; không ghi UID, số căn cước, OTP, raw QR, ảnh hoặc token vào báo cáo chia sẻ.

## Chụp/đọc/chuẩn bị

- [ ] C05-01 C04 vẫn mở, sửa/lưu nghề nghiệp và role cố định; form chưa lưu không bị mất khi bấm xác minh. Khi trở về C04 đọc lại version mới.
- [ ] C05-02 Camera hỏi quyền đúng lúc, từ chối/hủy/không có camera có đường chọn ảnh. Từ chối vĩnh viễn mở cài đặt. Không tự mở camera khi xoay.
- [ ] C05-03 Chụp/xem lại/dùng/chụp lại; QR theo một trong hai cấu trúc được hỗ trợ trích đúng họ tên, ngày sinh, số; các field từ QR không sửa tay.
- [ ] C05-04 Không QR/QR khác loại/nhiều mã/thiếu ngày/tháng/năm/ngày không có thật/ngày tương lai: không crash, không đoán, không ghi định danh cũ thành bản mới.
- [ ] C05-05 JPEG/PNG đúng hướng; ảnh mờ, file hỏng, HEIC/GIF hoặc >10 MB bị báo phù hợp; không nhầm ảnh thất bại với ảnh đã chọn trước đó.
- [ ] C05-06 SEEKER chọn giấy nghỉ hưu/BHXH mô phỏng, thêm 1–3 ảnh, xem/phóng to, bỏ/chọn lại; không chọn loại BHYT/HT. EMPLOYER không hiện yêu cầu ảnh nghỉ hưu.
- [ ] C05-07 Cách xét duyệt nêu đúng mô phỏng, mục đích/quyền xem; cần đồng ý trước gửi. Không có watermark/ảnh giấy tờ AI, không tự tính tuổi để duyệt.
- [ ] C05-08 Google chưa có số vẫn chuẩn bị được; gửi bị chặn. Sau liên kết/đồng bộ số bằng C03 và lưu nghề nghiệp đủ thì gửi được. Liên hệ chưa khớp/area inactive không được gửi.

## Gửi và phiên bản

- [ ] C05-09 Gửi SEEKER: upload đủ ảnh trước SUBMITTED; User/currentRequest, profile identity version và request nhất quán. EMPLOYER tương tự, không nghỉ hưu. Chưa có APPROVED/VERIFIED tự động.
- [ ] C05-10 Bấm gửi lặp/đồng thời hai thiết bị không tạo hai bản hiện hành, không ghi đè version. Cùng ID retry không sinh trùng thông báo hoặc thay byte ảnh.
- [ ] C05-11 Mất mạng trước stage/giữa upload/trước submit: báo lỗi, không báo đã gửi. Kết nối lại chỉ retry khi người dùng bấm; ảnh đã có cùng nội dung được tái dùng.
- [ ] C05-12 Dừng chờ gửi: nút gửi bị khóa tới tải lại. Nếu transaction đã thành công thì đọc lại thấy SUBMITTED; nếu chưa thì không giả báo hủy/thành công. Không gửi lại âm thầm sau callback trễ.
- [ ] C05-13 Xoay/Home khi chụp/picker/đọc QR/upload: giữ nháp trong ViewModel, không mở camera/gửi lần nữa. Process bị hủy: đọc trạng thái server, không tự khôi phục raw QR/ảnh hay tự gửi.
- [ ] C05-14 Back/tải lại khi có nháp hỏi xác nhận; bỏ nháp thật, mở lại không lộ ảnh cũ. Đổi UID/đăng xuất từ phiên khác không hiển thị hoặc hoàn tất thao tác dưới người mới.
- [ ] C05-15 Bản SUBMITTED không sửa ảnh/nội dung. Sau NEEDS_INFO/REJECTED có lý do, gửi mới có previousRequestId và identity version mới; xem bản trước còn nguyên.
- [ ] C05-16 Sau APPROVED/VERIFIED, gửi định danh mới chuyển SUBMITTED, approved version = 0; quyết định cũ không còn mở quyền cho bản mới. C04 lưu nghề nghiệp không xóa fullName/dateOfBirth/identityProfileVersion.

## Admin và kết quả

- [ ] C05-17 Admin grant + User ADMIN ACTIVE mở danh sách; hai loại đúng role, tối đa 50, duyệt xong tải tiếp. Tìm UID/mã request đúng loại, không tìm được có phản hồi.
- [ ] C05-18 Xem đủ ảnh, phóng to/kéo/TalkBack; ảnh lỗi có tải lại và không bấm quyết định khi chưa đủ ảnh. Snapshot nghề nghiệp/danh tính đúng bản gửi; không dùng ảnh/lựa chọn của request trước.
- [ ] C05-19 MATCH + xác nhận xem đủ ảnh → Chấp thuận → Xác nhận, không cần nhập nhận xét. NOT_MATCH/INSUFFICIENT không chấp thuận; NEEDS_INFO/REJECTED mở ô lý do và bắt nhập. Không nhập số căn cước trong lý do/thông báo.
- [ ] C05-20 Quyết định ghi nguyên tử request/User/notification: SEEKER APPROVED, EMPLOYER VERIFIED; bản khác không bị cấp quyền. Hai Admin cùng duyệt/bản cũ chỉ một quyết định hợp lệ.
- [ ] C05-21 Mất mạng/timeout sau quyết định: không tự áp dụng lại. Tải lại để đối chiếu trạng thái và chỉ một notification `{requestId}_1`.
- [ ] C05-22 Chủ hồ sơ thấy kết quả/lý do, mở được từ thông báo. Đang ở màn hoặc mở lại app sau Admin duyệt đọc đúng server; có nháp thì không tự bỏ nháp. Không nhận SMS kết quả.
- [ ] C05-23 Thu hồi có lý do: request lịch sử không đổi, revocation + User REVERIFY_REQUIRED/approved0 + thông báo `_2` cùng ghi; không thu hồi bản cũ không còn hiện hành. Gửi mới sau thu hồi được xét lại.
- [ ] C05-24 Admin bị disable grant hoặc SUSPENDED không thao tác/đọc ảnh mới. Người dùng SUSPENDED không gửi. Admin không tự xét chính mình, không đổi role/status/contact người khác.

## Rules và riêng tư — kiểm bằng client/Rules tooling phù hợp

Console quản trị bỏ qua Rules; sửa document trong Console không chứng minh quyền. Playground không mô phỏng được hết getAfter/transaction/Storage cross-service; ca không chạy được phải để chưa xác minh.

- [ ] C05-R01 Anonymous/UID B không đọc request/ảnh/thông báo của UID A; không list hồ sơ/ảnh riêng. Đọc document không tồn tại không trả dữ liệu của ai.
- [ ] C05-R02 Owner không tự ghi APPROVED/VERIFIED/approvedVersion, decisionVersion, reviewer, grant hoặc notification; không sửa role/accountStatus. Không tạo request role khác/đường dẫn ảnh của UID khác.
- [ ] C05-R03 Submit thiếu OTP/không khớp claim/profile nghề nghiệp/version, thiếu paired User/profile/request hoặc snapshot ngoài allowlist bị từ chối. Không tự sửa tên/ngày sinh trên profile ngoài transaction gửi hợp lệ.
- [ ] C05-R04 Sau STAGING không thay nội dung/đường dẫn/snapshot; Storage chỉ tạo đúng file đã khai, đúng metadata/size khi STAGING; update/delete byte/metadata bị từ chối cả trước/sau SUBMITTED. Admin cũng không sửa byte ảnh.
- [ ] C05-R05 Admin duyệt stale request/version, thiếu notice/User paired, approval với lookup khác MATCH, tự xét mình hoặc sửa field ngoài quyết định bị từ chối.
- [ ] C05-R06 Thu hồi thiếu paired revocation/User/notice, sửa/xóa lịch sử, thu hồi lại/cũ bị từ chối. Không đổi APPROVED thành REVERIFY_REQUIRED chỉ bằng update User rời rạc.
- [ ] C05-R07 APK không có khóa quản trị, log/Intent/saved state/preferences/SQLite không chứa QR/ảnh/số căn cước; màn nhạy cảm chặn screenshot; cache camera được dọn. Đóng process không tự upload lại.
- [ ] C05-R08 Client đã sửa có thể khai đúng đường dẫn nhưng không upload: Admin không xem được đủ ảnh nên không được chấp thuận qua UI. Ghi rõ giới hạn Firestore không kiểm nội dung Storage/QR pháp lý hoặc sự trung thực của người duyệt.

## Giao diện và hồi quy

- [ ] C05-25 Font 200%, xoay/ngang/tablet/chia cửa sổ, cutout/bàn phím: cuộn tới mọi nút, chữ không cắt; hướng dẫn ngắn, chi tiết ở mục riêng.
- [ ] C05-26 TalkBack thứ tự và nhãn ảnh/nút/phóng to, lỗi đọc được; không đọc logo lặp; thao tác hủy/đổi request không mất nhận xét chưa lưu mà không hỏi.
- [ ] C05-27 Hồi quy C01–C04: Google/OTP, lưu phiên, liên kết/reauth/đổi số, role/hồ sơ/area, đăng xuất; không phát sinh quyền ứng tuyển/đăng việc giả từ nhãn đã duyệt.

Không tự đánh dấu toàn bộ đạt. Timeout/callback trễ/cạnh tranh/biến thể thiết bị/quyền chưa tái hiện được phải ghi riêng. C02-F01 còn mở; TTS C22, chưa phải C05.

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

### Kiểm tra lại sau chỉnh giao diện

- [ ] C05-UI01 Ba khối Căn cước/Bằng chứng/Gửi có tiêu đề, viền và khoảng cách rõ; các nút có khoảng trống, không dính nhau khi font 200% hoặc xoay. EMPLOYER không có khối bằng chứng.
- [ ] C05-UI02 Chụp căn cước → ảnh xem lại ở khối Căn cước → Dùng ảnh và đọc QR → tên/ngày sinh và ảnh nằm trong cùng khối; không cuộn/chuyển sang bằng chứng. Chọn căn cước từ thư viện tương tự.
- [ ] C05-UI03 Thêm ảnh bằng chứng JPEG/PNG → thấy ngay ảnh và số lượng tăng; không cần bấm Dùng ảnh. Phóng to/bỏ/thêm lại, tối đa 3 ảnh; thay bộ ảnh có cùng số lượng vẫn hiển thị ảnh mới.
- [ ] C05-UI04 Chọn bằng chứng rồi chụp/chọn lại căn cước và chiều ngược lại: không lẫn loại, không đọc QR từ bằng chứng, không ghi căn cước vào danh sách bằng chứng. Có thể giữ ảnh căn cước đang xem lại trong khi chọn bằng chứng; ảnh căn cước vẫn cần xác nhận riêng trước gửi.
- [ ] C05-UI05 Hủy camera/picker, ảnh hỏng/HEIC/GIF/>10 MB: báo tại đúng khối, số ảnh và dữ liệu hợp lệ trước đó không mất. Gửi ảnh chỉ bắt đầu khi bấm Gửi hồ sơ.
- [ ] C05-UI06 Xoay khi đang chọn/đọc/xem ảnh: đúng đích, không mở picker/camera lại, ảnh không biến mất hoặc thêm hai lần; vị trí cuộn hợp lý. Process bị hủy có yêu cầu chọn lại, không tự gán ảnh cho tài khoản khác.

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

### Kiểm tra lại đường QR

- [ ] C05-QR01 Camera có ảnh xem lại trong khối Căn cước; Dùng ảnh và đọc QR đọc đúng thông tin trên mẫu 6 trường || người dùng cung cấp; mẫu 7 trường | vẫn được hỗ trợ. Chọn cùng ảnh từ thư viện cho kết quả tương ứng.
- [ ] C05-QR02 Ảnh ngang/dọc/EXIF xoay được đọc theo hướng ảnh; QR ở giữa/góc vẫn được thử đọc. Ảnh gửi Admin còn nguyên khung căn cước.
- [ ] C05-QR03 Ảnh không QR/mờ/lóa báo NOT_FOUND hoặc UNREADABLE tương ứng, không gộp thành lỗi camera; có thể giữ ảnh xem lại hoặc chọn lại.
- [ ] C05-QR04 QR khác cấu trúc/ngày sinh thiếu hoặc sai lịch/số định danh không hợp lệ báo đúng nhóm FORMAT/BIRTH_DATE/DOCUMENT_NUMBER/NAME; không điền dữ liệu giả hoặc lưu thay định danh cũ.
- [ ] C05-QR05 Nhiều nội dung QR khác nhau không được tự chọn; cùng một mã phát hiện ở các vùng chồng lấp không bị coi là nhiều căn cước.
- [ ] C05-QR06 Xoay/rời màn/đổi UID trong khi scan không ghi kết quả cũ, không đọc lặp do render; bitmap và scanner được giải phóng. Bằng chứng vẫn chọn/hiển thị/gửi như bản trước.
- [ ] C05-QR07 Nếu còn lỗi, ghi nguyên văn thông báo mới và dòng result của tag GrandMaQR trên bản debug; không đưa ảnh giấy tờ/raw QR/thông tin định danh vào báo cáo.

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

### Kiểm tra bổ sung cấu trúc 6 trường dấu đôi

- [ ] C05-FMT01 Run lại, quét đúng thẻ đã báo lỗi: không còn FORMAT với cấu trúc ID||NAME||DOB||GENDER||ADDRESS||ISSUED_DATE||||; đối chiếu đúng ID/họ tên/ngày sinh trong vùng riêng.
- [ ] C05-FMT02 Dấu rỗng ở cuối không làm lệch thông tin; thiếu tên/sai ngày/sai ID vẫn bị từ chối đúng nhóm. Mẫu 7 trường | có ô CMND cũ rỗng không bị nhận nhầm là mẫu 6 trường ||.
- [ ] C05-FMT03 Sau Publish Firestore Rules mới, gửi version=2 thành công theo điều kiện C05; không có lỗi permission do parserVersion. Version=1 cũ vẫn đọc/hiển thị được, không đổi dữ liệu lịch sử.
- [ ] C05-FMT04 Nếu vẫn FORMAT, chỉ báo dòng structure metadata; không gửi raw QR hay ảnh/số giấy tờ. Giữ trạng thái chưa đạt nếu chưa chạy được ca thực tế.

## Người dùng xác nhận QR/gửi hồ sơ và chuyển sang kiểm tra Admin — 25/10/2026

Người dùng xác nhận đã cập nhật, quét/đọc QR thành công, đối chiếu thông tin đều đúng, giao diện ổn và gửi hồ sơ thành công. Đây là kết quả do người dùng thực hiện; Codex không chạy lại hoặc tự xem dữ liệu thật. Phần cấp tài khoản Admin, đăng nhập, xem ảnh, chấp thuận/bổ sung/từ chối/thu hồi và kết quả ở tài khoản người dùng vẫn chờ kiểm tra; chưa đánh dấu toàn bộ C05 đạt.

Đã bổ sung docs/C05-ADMIN-GUIDE.md: đăng nhập Google riêng để có UID, dừng trước chọn role, cấp adminAccess enabled=true và users role ADMIN/accountStatus ACTIVE bằng Console, đăng nhập lại rồi Duyệt hồ sơ. Có trình tự xem ảnh, chọn kết quả đối chiếu mô phỏng, nhận xét, xác nhận và kiểm tra kết quả ở chủ hồ sơ.

Các ca hồi quy/Rules/cạnh tranh/timeout/thiết bị không tự được coi đạt chỉ từ xác nhận quét và gửi thành công. Không chạy build/test, không Console/deploy/stage/commit/push trong đợt hướng dẫn này.

## Điều hướng Admin/C05 bổ sung — chờ người dùng kiểm tra

- [x] C05-N01 Đăng nhập UID riêng có enabled=true, User ADMIN/ACTIVE → Tổng quan hàng đợi, topbar Admin. Nếu vừa cấp khi đang ở màn chọn vai trò, Kiểm tra lại tài khoản đọc đúng quyền; grant thiếu/User không khớp báo chưa đủ quyền, không tự chọn ADMIN.
- [x] C05-N02 Tổng quan hiển thị số SUBMITTED đúng mỗi loại và tổng; danh sách trống thật hiện 0. Lỗi mạng/quyền có báo chưa tải được và thử lại, không hiện 0 giả. Nếu số lượng chạm giới hạn 50 thì nhãn 50+/tổng+; các thẻ còn lại và Thống kê khóa/Sắp có.
- [x] C05-N03 Tổng quan → Kiểm tra hồ sơ và tab Xử lý đều tới hàng đợi, hai loại đúng nội dung. Chọn một hồ sơ → chỉ thấy chi tiết/ảnh/quyết định, không lẫn danh sách; Quay lại/tab Xử lý về danh sách. Bỏ nhận xét chưa lưu có hỏi, hủy thao tác thì còn nội dung.
- [x] C05-N04 Thực hiện C05-18–23 (ảnh, chấp thuận, bổ sung, từ chối, thu hồi, kết quả chủ hồ sơ) trên bản mới. Sau xử lý trở về Tổng quan số đếm đọc lại; mở Tài khoản/đăng xuất rồi Back không khôi phục ảnh hoặc quyền của UID cũ. Cấp Admin theo C05-ADMIN-GUIDE, không cần sửa grant đúng đang có.
- [x] C05-N05 SEEKER/EMPLOYER → Hồ sơ → vùng xác minh → nút theo trạng thái. Xem lại/Xem hồ sơ đã gửi mở trực tiếp bản hiện hành, Quay lại về Hồ sơ; từ wizard xác minh mở chi tiết rồi Quay lại vẫn về wizard. Tab Trang chủ/Hồ sơ/Tài khoản về đúng trang Main và đọc lại version; có nháp thì hỏi trước khi bỏ. Chụp/đọc QR/nhận bằng chứng/gửi vẫn như phần đã người dùng duyệt.
- [x] C05-N06 Xoay khi đang ở hàng đợi/chi tiết/upload không lặp thao tác; font 200%/màn hẹp/TalkBack và bàn phím dùng được topbar/nav/quyết định. Grant bị thu hồi/UID đổi trong khi tải vẫn bị kiểm quyền, không cập nhật dữ liệu callback của tài khoản trước.

Chi tiết phạm vi trong [C04-C05-NAVIGATION](C04-C05-NAVIGATION.md). Codex chưa chạy build/test/app hoặc tự xác nhận các mục này đạt.

## Rút gọn lý do và phản hồi khi duyệt — 25/10/2026

Người dùng đã vào Admin/tải hồ sơ chờ thành công. Chấp thuận chưa lưu được; app vẫn cuộn/quay lại, không ghi nhận ANR hoặc crash. Mã cũ bắt reason không rỗng với mọi quyết định và báo thiếu ở đầu trang; đã bỏ yêu cầu nhập khi chấp thuận và đưa phản hồi xuống vùng quyết định. Chưa có kết quả chạy bản sửa.

- [x] C05-D01 Mở hồ sơ chờ, xem đủ ảnh → Phù hợp → tích xác nhận → Chấp thuận → Xác nhận: không có ô nhận xét bắt buộc; thấy Đang lưu… ở vùng nút, sau thành công thấy kết quả và hồ sơ rời hàng đợi. Kiểm tra lại trạng thái ở chủ hồ sơ.
- [x] C05-D02 Không phù hợp/Chưa đủ căn cứ không mở Chấp thuận. Chưa xem đủ ảnh hoặc chưa tích xác nhận không được lưu quyết định.
- [x] C05-D03 Yêu cầu bổ sung/Từ chối mới mở ô lý do, có cuộn tới ô; để trống hoặc chứa 12 chữ số báo ngay tại ô. Nhập hợp lệ → nút hành động bên dưới → Xác nhận → chủ hồ sơ nhận đúng lý do. Hủy lựa chọn không lưu quyết định.
- [x] C05-D04 Thu hồi chỉ mở trên bản đủ điều kiện, vẫn phải nhập lý do và xác nhận. Xoay khi đang nhập lý do giữ hành động/nội dung trong ViewModel, không tự gửi; Back hỏi trước khi bỏ lựa chọn.
- [x] C05-D05 Chấp thuận không lấy nhầm lý do đang gõ cho Bổ sung/Từ chối. Request và notification lưu cùng dòng trạng thái chuẩn, cùng một transaction; UI không lặp dòng chuẩn nhưng vẫn hiển thị lý do/nhận xét lịch sử.
- [x] C05-D06 Lỗi mạng/quyền/xung đột không báo thành công. Sau lỗi đọc lại server trước khi xử lý tiếp; không tự replay khi có mạng. Nếu còn lỗi, gửi tag GrandMaReview với giai đoạn/mã lỗi; không gửi dữ liệu giấy tờ. Các ca timeout/callback trễ chưa tái hiện vẫn theo dõi, chưa tự coi đạt.

Không đổi Rules/schema/Console; chỉ Sync/Run mã mới. Codex rà tĩnh, không build/test/chạy app hoặc commit/push.

## Xác nhận và cải tiến Hồ sơ thống nhất — 25/10/2026

Người dùng xác nhận đã test lại các case sau bản sửa, mọi thứ đạt. D01–D06 được đánh dấu theo xác nhận tổng thể của người dùng, không phải Codex chạy; không có thêm thiết bị/API/log từng ca được cung cấp. Trạng thái chưa đạt/chờ kiểm tra quyết định trong lịch sử phía trên được thay thế bởi xác nhận này. Các biến thể timeout/callback trễ/Rules/thiết bị chưa tái hiện vẫn theo dõi riêng.

Đã triển khai tiếp đề xuất gộp lối vào: Trang chủ/Tài khoản người dùng chỉ có Hồ sơ; trong Hồ sơ có vùng thông tin, công việc, số điện thoại, Lưu và vùng xác minh với một dòng trạng thái + một nút theo trạng thái. Chưa xác minh mở wizard; đang chờ/đã duyệt mở bản hiện hành; bổ sung/từ chối/thu hồi có nút phù hợp. Các ca mới C04-P01–P06 trong C04-CHECKLIST đang chờ người dùng kiểm tra. Không đổi Rules/schema hoặc logic duyệt; chưa tự build/test/app/commit.
