# C02 — kiểm tra đăng nhập điện thoại/OTP thật

**Đã triển khai, chờ người dùng kiểm tra; chưa nghiệm thu.** Codex chỉ review tĩnh, không chạy build/test/lint/emulator hoặc gửi SMS. Nền C01 thực tế: `ffcc6a2` (Đăng nhập Google), đã được người dùng commit và phê duyệt. Giữ [kết quả và các nhánh theo dõi C01](C01-CHECKLIST.md), không biến các mục chưa xác minh thành đạt.

## Chuẩn bị trong Android Studio/Console

**Cập nhật 24/10/2026:** Lần gửi SMS trước thất bại với mã **17006**, thông báo “SMS unable to be sent until this region enabled by the app developer”. Người dùng đã bật **Allow Vietnam (VN)** trong SMS region policy và lưu thành công. **Chưa thử lại sau thay đổi; nhận SMS thật và giao diện mới đều chờ kiểm tra.** Không đánh dấu ca SMS đạt chỉ vì đã lưu cấu hình.

1. Sync rồi Run app trong Android Studio. Không cần nâng dependency/build tools hoặc thay Rules để chạy C02.
2. Trong đúng Firebase project `grandmaassist-a1dfa`, đối chiếu Phone provider enabled; SHA-1/SHA-256 của chứng thư ký bản đang chạy; SMS region policy cho phép Việt Nam; billing/quota SMS thực tế. Không yêu cầu thiết lập lại phần đang hoạt động nếu chưa có bằng chứng thiếu.
3. Dùng thiết bị thật nhận SMS khi nghiệm thu. Google Play services/Play Integrity cần được kiểm tra theo thiết bị; bản cài ngoài Play có thể dùng reCAPTCHA, cần SHA-1 và cấu hình API key/auth domain phù hợp. Không mở rộng API key/Rules tùy tiện; theo hướng dẫn Firebase nếu có lỗi cụ thể.
4. Có thể dùng số kiểm thử cấu hình trên Firebase để phát triển, nhưng phải ghi rõ ca thử; không thay thế ca nhận SMS thật. App không hardcode mã thử hoặc tắt app verification. Không thêm quyền READ_SMS/RECEIVE_SMS.

## Nhập số và chủ động gửi

| Ca | Kết quả mong đợi | Trạng thái |
| --- | --- | --- |
| Mở mới khi đã đăng xuất | Mặc định Số điện thoại; có lựa chọn Google; không tự gửi SMS | Chờ kiểm tra |
| Nhập dạng `0…` và `+84…`, có khoảng trắng/dấu chấm/gạch/ngoặc | Chấp nhận định dạng di động VN hợp lý, màn xem lại hiển thị E.164 đúng, không tạo `+840…` | Chờ kiểm tra |
| Thiếu/thừa số, ký tự không hợp lệ, `+840…`, dạng không hỗ trợ | Báo định dạng, không gửi Firebase; không tuyên bố số tồn tại/thuộc nhà mạng chỉ từ client | Chờ kiểm tra |
| Nhấn Gửi mã xác nhận | Chỉ mở bước xem lại; chưa gửi SMS | Chờ kiểm tra |
| Xem số và thông báo Google xử lý/chống lạm dụng | Chỉ nhấn Xác nhận gửi SMS mới gửi yêu cầu; Back/sửa số không gửi | Chờ kiểm tra |
| Bấm xác nhận nhiều lần nhanh | Một thế hệ yêu cầu; có trạng thái đang gửi, không báo SMS đã gửi trước callback | Chờ kiểm tra |
| Nhận SMS thật | Chỉ sau onCodeSent mới báo Firebase đã xác nhận gửi; nhập mã được | Chờ kiểm tra |
| Không nhận callback gửi | Có giới hạn chờ 90 giây; báo chưa rõ kết quả, không tự gửi lại | Chờ kiểm tra |

## OTP, gửi lại và xác minh tự động

| Ca | Kết quả mong đợi | Trạng thái |
| --- | --- | --- |
| Nhập/dán 6 chữ số đúng | Chỉ vào tài khoản sau Firebase sign-in thành công | Chờ kiểm tra |
| Mã thiếu ký tự/mã sai | Báo lỗi gần form, không đăng nhập nhầm; có thể nhập lại | Chờ kiểm tra |
| Phiên/mã hết hiệu lực theo Firebase | Báo lỗi, cho chủ động yêu cầu mã mới sau cooldown | Chờ kiểm tra |
| Gửi lại | Đếm 60 giây theo elapsed realtime, không reset khi vẽ/xoay; dùng ForceResendingToken khi còn giữ trong bộ nhớ | Chờ kiểm tra |
| Dùng mã cũ sau gửi lại | Theo kết quả Firebase, không ép chấp nhận mã cũ; callback thế hệ cũ không ghi đè màn mới | Chờ kiểm tra |
| Instant verification/auto-retrieval khi SDK hỗ trợ | Có thể bỏ qua nhập mã; chỉ sau Firebase sign-in mới vào tài khoản | Chờ kiểm tra |
| Auto callback đến cùng lúc nhấn Xác nhận | Không có hai tác vụ sign-in cạnh tranh | Chờ kiểm tra |
| onCodeAutoRetrievalTimeOut | Chỉ hết chờ tự nhận; không nói mã đã hết hạn, vẫn cho nhập mã thủ công | Chờ kiểm tra |
| Cooldown hết nhưng SDK còn throttle/quota | Vẫn báo giới hạn theo SDK, không hứa gửi được chỉ vì đồng hồ về 0 | Chờ kiểm tra |

## Hủy, vòng đời và mạng

| Ca | Kết quả mong đợi | Trạng thái |
| --- | --- | --- |
| Sửa số/Back ở bước review, gửi hoặc OTP | Về nhập số, loại bỏ id/token thế hệ cũ; không tự đăng nhập khi callback cũ về | Chờ kiểm tra |
| Đổi phone → Google hoặc ngược lại khi đang chờ mã | Hủy trạng thái phone cũ; nhánh Google giữ hoạt động; không liên kết/merge UID | Chờ kiểm tra |
| Hủy khi Firebase đang sign-in | Vô hiệu kết quả UI; chờ Task Firebase kết thúc rồi signOut kết quả cũ, trước khi cho tác vụ Auth mới bắt đầu | Chờ kiểm tra |
| Xoay ở nhập số/review/đang gửi/OTP/đang xác minh | Giữ phương thức, số, bước và deadline trong bộ nhớ; OTP đã gõ không lưu qua xoay; không tạo SMS mới vì render | Chờ kiểm tra |
| Home rồi quay lại sớm trong cửa sổ active | Gắn callback bằng reentrant SDK cùng yêu cầu, không ForceResendingToken; cần kiểm tra không phát sinh SMS thứ hai | Chờ kiểm tra |
| Home/xoay quay lại sau cửa sổ active | Không tự gọi gửi mới; có id thì vẫn nhập tay, thiếu kết quả gửi thì chờ giới hạn hoặc chủ động thao tác lại | Chờ kiểm tra |
| Process bị hủy khi chờ OTP | Không khôi phục token/OTP/số từ saved state; nếu không có phiên Auth đã hoàn tất thì về nhập số với hướng dẫn và không tự gửi SMS | Chờ kiểm tra |
| Mất mạng trước gửi/đang gửi/xác minh, sau đó có mạng | Thông báo đúng mức SDK cung cấp; không tự retry SMS hoặc báo thành công giả | Chờ kiểm tra |
| reCAPTCHA bị hủy, provider/configuration lỗi | Có thông báo tiếng Việt và đường chủ động thử lại; không log credential/OTP/số | Chờ kiểm tra |
| Quota/throttling khi tái hiện được | Không vòng lặp gửi mã hoặc reset giới hạn để tự gửi | Chờ kiểm tra |

Task Firebase không có API hủy sign-in đang chạy. App tuần tự hóa các lần sign-in để kết quả cũ không ghi đè phiên mới; khi đã nhấn hủy trong lúc trao đổi với Firebase, UI giải thích cần chờ Task kết thúc. Không tự mở một tác vụ Auth khác song song để giả lập hủy ngay.

Khi kiểm tra process bị hủy sau thao tác hủy sign-in, app chỉ dùng cờ boolean phục hồi ý định hủy để xóa phiên cũ nếu cần; không lưu số/UID/OTP/credential vào cờ này. OTP đã gõ được chủ ý xóa qua xoay hoặc chuyển bước; không coi mất OTP nhập dở là lỗi khôi phục dữ liệu nhạy cảm.

## Tài khoản, đăng xuất và hồi quy C01

- Phone-only: số hiển thị lấy từ FirebaseUser, không lấy số form làm bằng chứng đã xác minh. Tên/email thiếu có giá trị thay thế. Provider thực tế hiển thị phone; tài khoản có sẵn nhiều provider hiển thị cả Google/phone.
- Đóng/mở lại: khôi phục phiên điện thoại theo Firebase như C01; offline không tự coi hết phiên hoặc cấp quyền nghiệp vụ mới.
- Đăng xuất phone-only: xóa UI và phiên Firebase, hoàn tất rõ ràng, không gọi dọn Google; Back không mở tài khoản cũ.
- Google → đăng xuất → phone và phone → đăng xuất → Google: đúng tài khoản vừa đăng nhập; không hứa UID giữ nguyên nếu chưa liên kết. C03 mới thực hiện linking/đổi số.
- Hồi quy C01: Google thật, hủy chọn Google, mở lại/khôi phục phiên, đăng xuất Google và callback dọn phiên. Các nhánh C01-F01–F09 còn mở giữ nguyên trạng thái cũ.

## Giao diện

- Trong Android Studio, Sync/Run rồi nhập số → Gửi mã xác nhận → xem lại số/đồng ý → Xác nhận gửi SMS. Khi đang gửi, mô tả là “Số đang xác minh”; chỉ sau `onCodeSent` mới hiện “Mã SMS đã được gửi đến”, số hiện tại, một ô OTP và “Nhập 6 chữ số trong SMS.”. Không ép mở trạng thái đã gửi để kiểm tra giao diện. Nếu xác minh tự động thành công có thể vào thẳng tài khoản. **Chờ kiểm tra sau khi bật VN.**
- Nhập mã sai/hết hiệu lực: hai mô tả OTP giữ nguyên, lỗi ngắn xuất hiện riêng gần nút Xác nhận. Gửi lại: khi yêu cầu mới đang xử lý không còn câu khẳng định đã gửi; countdown vẫn theo deadline hiện có, hết đếm không bảo đảm dịch vụ cho gửi. **Chờ kiểm tra.**
- Thanh thương hiệu/logo/banner/màu/font C01 giữ nguyên; tab chọn có nền/viền và trạng thái selected. Không có TTS/nút giả/OTP âm thanh.
- Font 200%, điện thoại ngang/tablet/chia đôi cửa sổ: cả nút tab, consent, số chuẩn hóa, OTP và lỗi đều đọc/tiếp cận được; cuộn được với bàn phím và system bars/cutout.
- Một trường OTP có bàn phím số, dán/autofill phù hợp; không lưu view hierarchy của form hoặc OTP. TalkBack đọc đúng nhãn; thông báo không chứa mã OTP.

Ghi mã ca, commit/bản chạy, thiết bị/API và kết quả thật. Không đưa số thật, OTP, token, credential hoặc ảnh tài khoản chưa che vào báo cáo. Nếu không tái hiện được một lỗi thì ghi chưa xác minh, không đánh dấu đạt. Sau review, người dùng tự commit đúng [file C02](C02-FILES.md); Codex không stage/commit/push/deploy và không chuyển C03.

## Cập nhật xác nhận C02 — 24/10/2026

**Người dùng đã duyệt luồng chính C02.** Mục cập nhật này thay thế các câu “chưa thử lại/chưa nhận SMS/chờ kiểm tra luồng chính” ở phần lịch sử phía trên; không thay thế trạng thái của các ca chưa xác nhận riêng.

Kết quả người dùng đã xác nhận:
- Trên Android thật: nhập số → xác nhận gửi → trình duyệt CAPTCHA → xác thực thành công quay về app → nhận SMS thật → tự xác minh và đăng nhập.
- SMS nhận trên thiết bị khác: nhập OTP sai có báo lỗi; nhập đúng đăng nhập thành công.
- Đăng xuất và các mục thử thêm hoạt động ổn; người dùng duyệt C02.
- Không suy rộng câu “các mục khác” thành đã kiểm tra từng dòng trong checklist.
- Các lỗi từng gặp: 17006 (chặn vùng SMS, đã bật Allow Vietnam), 17028 (Play Integrity trên bản thử trước), 17010 (giới hạn yêu cầu). Thành công trên điện thoại thật chưa chứng minh mọi môi trường máy ảo đã được khắc phục.

### C02-F01 — Ngôn ngữ và tên hiển thị trong SMS

Người dùng đã chọn tiếng Việt trên Firebase; code đặt setLanguageCode("vi") trước yêu cầu Phone Auth. SMS thật vẫn có nội dung tiếng Anh và tên chung “Your Firebase App”. Chưa xác định nguyên nhân. Tạm ghi theo dõi, giữ mẫu SMS Firebase; không cam kết đổi Public-facing name/nâng SDK sẽ sửa được, không đổi hệ thống gửi SMS và không đánh dấu đã giải quyết. Đây là hạn chế nội dung SMS, không phải lỗi đăng nhập đã xác nhận.

### Các mục chưa có xác nhận riêng

Nhóm dễ kiểm tra, người dùng xác nhận thêm nếu chưa làm:
1. Gửi lại OTP sau cooldown; không tự gửi và không reset đếm khi xoay.
2. Sửa số/Back và đổi tab khi đang chờ mã; callback cũ không tự đăng nhập.
3. Xoay ở OTP, Home/quay lại: không tự gửi thêm SMS.
4. Đóng/mở lại với phiên phone; Back sau đăng xuất; đăng nhập Google lại sau thay đổi C02.
5. Mất mạng/phục hồi; số thiếu/thừa hoặc sai định dạng.

Nhóm bổ sung còn mở: mã hết hiệu lực/mã cũ sau resend; timeout gửi; callback đồng thời/trễ; hủy lúc Firebase sign-in; process bị hủy; CAPTCHA bị hủy; font 200%/TalkBack/tablet/thiết bị khác. Không cố gửi nhiều SMS thật để gây quota. Giữ “chưa xác minh riêng” khi chưa tái hiện.

Luồng chính đã được duyệt; chưa khẳng định toàn bộ checklist đạt. TTS thuộc C22, liên kết/đổi số thuộc C03.
Codex chỉ cập nhật báo cáo, không chạy kiểm thử và không stage/commit/push.

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
