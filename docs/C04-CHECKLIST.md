# C04 — danh sách kiểm tra người dùng

25/10/2026 — người dùng xác nhận đã sửa dữ liệu khu vực, kiểm tra lại toàn bộ checklist và phê duyệt C04. Kết quả được ghi nhận theo xác nhận của người dùng; Codex không tự kiểm thử.
Codex không build/test/lint/emulator hoặc kiểm thử Rules; không gửi SMS, deploy hay thao tác Git ghi.

## Thứ tự bắt buộc
1. Đọc C04-CONSOLE-SCHEMA.md.
2. Publish toàn bộ firebase/firestore.rules đúng project grandmaassist-a1dfa.
3. Nhập ba documents areas DEMO theo tài liệu.
4. Android Studio: Sync, Run. Nếu lỗi, gửi lỗi đầu tiên của Build Output.
5. Dùng ít nhất hai UID thử riêng cho SEEKER và EMPLOYER; role cố định sau xác nhận.
6. Ghi thiết bị/API và kết quả thật từng mục; không ghi số/UID/OTP/token vào tài liệu chia sẻ.

## Các ca chức năng — đạt theo xác nhận tổng thể của người dùng
- [x] C04-01 Login tài khoản chưa có User → tải server → màn chọn vai trò. Không tự chọn; chọn thẻ chỉ đổi giao diện; Tiếp tục mở xác nhận, hủy chưa tạo tài liệu.
- [x] C04-02 Chọn Tìm việc → xác nhận: User SEEKER và jobSeekerProfiles cùng UID được tạo; không có employerProfiles.
- [x] C04-03 Chọn Thuê người trên UID khác: User EMPLOYER và employerProfiles; không có jobSeekerProfiles.
- [x] C04-04 Mở lại app/đăng nhập Google hoặc phone đã link: đúng hồ sơ và vai trò, không hỏi lại hoặc đổi role.
- [x] C04-05 Hai thiết bị cùng UID chọn hai role khác nhau: chỉ vai trò được server chốt trước tồn tại; thiết bị kia tải vai trò thực tế, không ghi đè.
- [x] C04-06 SEEKER lưu/sửa tên hiển thị, khu vực, kỹ năng, kinh nghiệm, ca; dữ liệu đọc lại đúng, có báo lưu thành công.
- [x] C04-07 EMPLOYER cá nhân/tổ chức, tên tổ chức bắt buộc khi tổ chức; giới thiệu/khu vực; đổi về cá nhân không giữ tên tổ chức đã lưu.
- [x] C04-08 Google chưa có phone vẫn chọn vai trò và lưu hồ sơ nháp; không ghi fullName/dateOfBirth/giới tính/sức khỏe hoặc trạng thái APPROVED.
- [x] C04-09 Để trống khu vực/kỹ năng/kinh nghiệm: lưu nháp được, lời giải thích chưa đủ nghề nghiệp; tên hiển thị không rỗng khi lưu. Thử vượt giới hạn kỹ năng/tên/mô tả.
- [x] C04-10 Danh mục chưa seed/rỗng hoặc tải lỗi: báo đúng trạng thái, có tải lại; không cho nhập mã khu vực tùy ý. Khu vực bị vô hiệu hóa: chọn lại trước lưu.
- [x] C04-11 Hai thiết bị sửa cùng phiên bản: lần lưu sau báo xung đột; bản chỉnh chưa bị ghi đè lên dữ liệu mới. Đọc lại có xác nhận bỏ thay đổi nếu đang sửa.
- [x] C04-12 Xoay tại chọn vai trò/form: giữ lựa chọn và nội dung trong bộ nhớ, không tạo lại thao tác ghi. Process bị hủy: đọc lại server, không tự ghi form cũ.
- [x] C04-13 Back/đọc lại/đăng xuất khi form chưa lưu: hỏi bỏ thay đổi hoặc tiếp tục sửa; xem tài khoản C03 rồi quay lại không mất nháp.
- [x] C04-14 Mất mạng trước đọc/ghi: báo lỗi, không mở chọn role vì lỗi mạng, không âm thầm gửi lại khi có mạng; chỉ báo lưu sau server xác nhận.
- [x] C04-15 Quyền đọc bị chặn: báo permission và retry, không tạo hồ sơ mới. Cấu hình Admin thiếu cũng không hỏi chọn role thường.
- [x] C04-16 Đăng xuất/đổi UID: không lộ form/tên/số của người trước.
- [x] C04-17 Có phone → User khớp claim; phoneVerifiedAt là thời điểm sync, không tăng mỗi lần lưu không đổi số.
- [x] C04-18 Thêm/đổi số bằng C03 → refresh Auth/token → sync User cùng UID. Kiểm tra form đang có nháp và form sạch.
- [x] C04-19 Auth đã đổi nhưng sync lỗi: UI nói chưa đồng bộ, không gửi SMS/update Auth lần nữa; Đọc lại và sync riêng phục hồi được.
- [x] C04-20 Admin bootstrap đúng: tài khoản quản trị, không chọn vai trò; không có dashboard/nút nghiệp vụ giả.
- [x] C04-21 ACTIVE khác (Console thử SUSPENDED): vẫn đọc được hồ sơ/tài khoản, không sửa hồ sơ; không tự phục hồi ACTIVE.
- [x] C04-22 Hồi quy C01–C03: Google/phone login, hủy, OTP tay/tự động, captcha, khôi phục phiên, link/reauth/đổi số, đăng xuất.
- [x] C04-23 Font 200%, ngang/tablet/chia cửa sổ/bàn phím/cutout/TalkBack: đọc và cuộn đủ các nút, lỗi và tên/email; thứ tự hợp lý, không đọc logo lặp.

## Các ca Rules — người dùng xác nhận checklist đạt; Codex chưa xác minh độc lập
Thực hiện trên dữ liệu thử qua Rules Playground hoặc công cụ client/môi trường kiểm tra phù hợp do người dùng chủ động chạy.
KHÔNG dùng edit Console làm phép kiểm quyền: Console dùng quyền quản trị bỏ qua Rules.
Playground không mô phỏng được mọi transaction/getAfter và cạnh tranh; nếu không thực hiện được thì ghi chưa xác minh, không suy ra đã đạt.

- [x] C04-R01 Anonymous hoặc UID B get User/profile của UID A bị từ chối; không list users/profile.
- [x] C04-R02 Owner đổi role/id/createdAt/accountStatus, xóa rồi tạo lại để đổi role, tự tạo ADMIN hoặc ghi adminAccess đều bị từ chối.
- [x] C04-R03 Owner sửa retirementStatus/identityStatus, thêm approvedVersion/identity legal fields hoặc trường ngoài allowlist bị từ chối.
- [x] C04-R04 phoneE164 giả/khác phone_number claim hoặc tự khai verification mode sai bị từ chối; claim không phone chỉ lưu rỗng/null/NONE hợp lệ.
- [x] C04-R05 Tạo profile sai vai trò/cả hai profile hoặc chỉ tạo User thiếu profile nguyên tử bị từ chối.
- [x] C04-R06 Update profile không tăng version User trong cùng transaction, timestamp giả, kỹ năng sai kiểu/quá dài hoặc enum ca sai bị từ chối.
- [x] C04-R07 Khu vực không tồn tại/inactive không được lưu; client không sửa danh mục, anonymous không đọc danh mục riêng C04.
- [x] C04-R08 Trạng thái tài khoản bị khóa không thể tự sửa hồ sơ hoặc tăng quyền; Admin C04 không tự có quyền đọc/ghi hồ sơ người khác.

## Ghi kết quả
Chỉ đánh dấu khi người dùng thực hiện thực tế. Nếu khó tái hiện lỗi timeout/đồng thời/quyền thì giữ chưa xác minh và mô tả giới hạn.
C01/C02/C03 đã được duyệt trên bản trước; không coi là hồi quy C04 đã đạt.
Giữ C02-F01 (ngôn ngữ/tên SMS) và các mục cũ còn mở. C05/QR và C22/TTS chưa thuộc C04.

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
