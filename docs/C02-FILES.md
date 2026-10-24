# File thuộc phạm vi C02

**Đã triển khai, chờ người dùng kiểm tra; chưa nghiệm thu.** Nền C01 thực tế là `ffcc6a2` (Đăng nhập Google). Working tree sạch khi bắt đầu C02. Không đổi Firebase/Rules/Gradle/asset C01/.idea; không stage/commit/push/deploy.

| Loại | File |
| --- | --- |
| Sửa | `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/AuthRepository.java` |
| Mới | `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/PhoneSignInFlow.java` |
| Sửa | `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/MainActivity.java` |
| Sửa | `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/viewmodel/AuthViewModel.java` |
| Mới | `app/src/main/res/drawable/button_method.xml` |
| Mới | `app/src/main/res/drawable/input_background.xml` |
| Sửa | `app/src/main/res/layout/activity_main.xml` |
| Mới | `app/src/main/res/layout/phone_code.xml` |
| Mới | `app/src/main/res/layout/phone_entry.xml` |
| Mới | `app/src/main/res/layout/phone_review.xml` |
| Sửa | `app/src/main/res/values/strings.xml` |
| Sửa | `app/src/main/res/values/styles.xml` |
| Sửa | `docs/C01-FILES.md` |
| Mới | `docs/C02-CHECKLIST.md` |
| Mới | `docs/C02-FILES.md` |
| Sửa | `docs/SETUP.md` |
| Sửa | `README.md` |


## Hành vi chính

- `PhoneSignInFlow`: nhập/chuẩn hóa số, review/đồng ý gửi, callback gửi/tự xác minh/timeout/lỗi, cooldown 60 giây và thế hệ challenge trong bộ nhớ; không giữ Activity.
- `AuthRepository`/`AuthViewModel`: dùng cùng nguồn Firebase Auth C01, chặn sign-in kép, tuần tự hóa hủy Task, chặn callback cũ; account đọc phone/provider thực từ Firebase; phone-only không gọi dọn Google. Một cờ boolean phục hồi ý định hủy, không lưu số/UID/OTP/token.
- `MainActivity` và XML: tab phương thức, review inline trước SMS, một trường OTP với bàn phím số/dán/autofill; gắn SDK callback bằng Activity hiện tại, gắn lại có giới hạn cửa sổ gốc; không tự gửi lại khi quay lại muộn. Tái sử dụng logo/banner/font/nút Google đã duyệt.
- Giữ phạm vi C03 (link/đổi số) và C22 (TTS) riêng. Không tạo Firestore profile/role hay nới Rules.

## Review tĩnh

- Đọc tài liệu C01/README/SETUP, status/log; xác định C01 đã commit và không ghi đè thay đổi riêng.
- Đọc diff và từng file mới, rà soát callback/generation, cooldown, hủy/sửa/đổi phương thức, vòng đời, số/proof nguồn Firebase và nhánh đăng xuất phone-only.
- Đọc/đối chiếu 27 XML và các tham chiếu resource/ID Java; không thiếu nguồn cục bộ. `default_web_client_id` vẫn do Google Services sinh như C01.
- Đọc metadata class trong Firebase Auth 25.0.0 artifact đang cache để đối chiếu API PhoneAuthProvider/Options.Builder và exception mới; không chạy compiler/SDK hay gửi SMS.
- Review whitespace diff. Không chạy build/test/lint/emulator/kiểm thử tự động.

## Chưa xác minh và bước tiếp theo

### Chỉnh giao diện nhập số/OTP — 24/10/2026

- Lần gửi trước thất bại với mã **17006**, nội dung “SMS unable to be sent until this region enabled by the app developer”. Đã xác định lỗi chặn vùng SMS; người dùng đã bật **Allow Vietnam (VN)** và lưu thành công. **Chưa thử lại sau khi bật vùng, chưa xác nhận SMS thật hoạt động.** Codex không thay cấu hình dịch vụ.
- Đợt chỉnh này sửa đúng 8 file: `strings.xml`, `layout/phone_entry.xml`, `layout/phone_review.xml`, `layout/phone_code.xml`, `MainActivity.java`, `PhoneSignInFlow.java`, `docs/C02-FILES.md`, `docs/C02-CHECKLIST.md`. Các thay đổi C02 có sẵn trong working tree được giữ nguyên.
- Rút gọn màn nhập số và thông báo đồng ý; nút sửa ở bước xem lại ghi “Sửa số điện thoại”. OTP có mô tả đích nhận, số hiện tại, một EditText căn giữa, hướng dẫn bên dưới, lỗi riêng gần nút xác nhận; hành động quay lại dạng chữ có vùng chạm tối thiểu 56dp. Không thêm giới hạn dòng/ellipsis hoặc số điền sẵn; giữ font 20sp/28sp, cuộn/insets và nhận diện C01.
- Wiring đã có: `onCodeSent` → `Step.CODE` → `MainActivity.renderPhone()` hiển thị `phoneCode`. Không thiếu layout OTP. Java chỉ bổ sung cờ `codeSent` trong trạng thái tạm và nối mô tả hiển thị: cờ chỉ bật trong `onCodeSent`, reset khi gửi yêu cầu mới/hủy; timeout tự nhận không tự bật cờ. Trước callback dùng “Số đang xác minh”, sau callback mới dùng “Mã SMS đã được gửi đến”. Thông báo đã gửi không lặp lại ở vùng lỗi; hai mô tả OTP luôn riêng biệt.
- Không đổi logic Google, sign-in, cooldown, chống callback cũ, Rules hoặc cấu hình. Xác minh tự động vẫn có thể vào thẳng tài khoản. Đã review tĩnh phần thay đổi; giao diện mới, gửi lại sau bật VN và các ca OTP vẫn chờ người dùng kiểm tra; không chạy build/test/lint/emulator/gửi SMS.

Người dùng thực hiện [C02-CHECKLIST](C02-CHECKLIST.md): Sync/Run, SMS thật, mã đúng/sai/hết hiệu lực, auto verification, resend, hủy/đổi phương thức/callback cũ, xoay/Home/process bị hủy, mạng/quota/reCAPTCHA, provider/account, phone-only logout và hồi quy Google C01. Cần kiểm tra SHA-1/SHA-256, SMS region Việt Nam, billing/quota và thiết bị theo cấu hình thực tế; không yêu cầu làm lại cấu hình đang hoạt động.

SDK không cho hủy trực tiếp Task sign-in đang chạy; lúc này app chờ Task cũ kết thúc và dọn kết quả trước khi cho Auth mới chạy. Reattach dựa cơ chế reentrant của SDK chỉ trong cửa sổ bảo thủ trước timeout gốc, không force resend; ngoài cửa sổ app không tự gửi, có thể cần nhập mã thủ công hoặc chủ động yêu cầu mới. Các nhánh này chưa được chạy trên thiết bị.

Người dùng tự review/stage/commit đúng các file trên. Danh sách theo dõi C01-F01–F09 trong C01-CHECKLIST giữ nguyên; không tự chuyển C03.

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
