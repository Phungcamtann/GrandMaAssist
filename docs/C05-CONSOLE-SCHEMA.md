# C05 — thiết lập Console và dữ liệu

25/10/2026 — làm trước khi chạy C05. Project **grandmaassist-a1dfa**, database `(default)`, bucket **grandmaassist-a1dfa.firebasestorage.app**. Không đổi JSON/Auth/SHA đang hoạt động.

## Thao tác của người dùng

1. Firebase Console → Firestore → Rules: thay toàn bộ bằng `firebase/firestore.rules` hiện tại và Publish.
2. Storage → Rules: thay toàn bộ bằng `firebase/storage.rules` và Publish. Rules C05 dùng Firestore để giới hạn ảnh theo hồ sơ và kiểm Admin. Nếu Console hỏi cấp kết nối/quyền giữa Storage và Firestore, thực hiện theo hướng dẫn của Console rồi Publish lại. Không thay bằng `allow read, write: if true` để vượt lỗi.
3. Giữ nguyên ba khu vực DEMO đã sửa `name`/`active`. Không tự tạo các request, profile identity hoặc notification: app tạo theo transaction. C04 cũ không cần xóa/migrate để có các field mới; chỉ bổ sung khi gửi C05.
4. Chuẩn bị tài khoản người dùng đã có role, hồ sơ nghề nghiệp đã lưu đủ và số điện thoại liên kết/khớp. SEEKER cần khu vực, kỹ năng, kinh nghiệm, ít nhất một ca. EMPLOYER cần khu vực/giới thiệu, tên tổ chức nếu là tổ chức.
5. Chuẩn bị một tài khoản Admin riêng chưa chọn SEEKER/EMPLOYER theo phần bên dưới. Không đổi vai trò của tài khoản đang có nghiệp vụ để thử.
6. Android Studio Sync rồi Run. SEEKER/EMPLOYER vào **Hồ sơ → khối Xác minh hồ sơ**, chọn nút theo trạng thái. Admin vào **Tổng quan → Kiểm tra hồ sơ** hoặc tab **Xử lý**. Đọc [C05-ADMIN-GUIDE](C05-ADMIN-GUIDE.md) để cấp quyền và duyệt. Người dùng đã xác nhận QR/gửi, vào Admin và các ca bản sửa quyết định đạt; đợt gộp Hồ sơ/xác minh mới còn chờ kiểm tra.

Phần bổ sung trang chính/điều hướng ngày 25/10 không đổi schema hay Rules. Nếu đã Publish bản C05 chấp nhận parserVersion [1,2] và gửi thành công thì không cần Publish lại vì phần giao diện này.

## Admin

Lấy UID riêng tư của tài khoản Admin tại Authentication → Users. Tạo bằng Console:

`adminAccess/{uid}`: `enabled` kiểu boolean = `true`.

`users/{uid}`:

| Field | Type | Giá trị |
|---|---|---|
| id | string | UID đó |
| role | string | ADMIN |
| accountStatus | string | ACTIVE |
| displayName | string | Tên Admin |
| createdAt | timestamp | Thời điểm cấp |
| updatedAt | timestamp | Thời điểm cấp |

Nếu đã cấp đúng ở C04 thì giữ nguyên. Không cần tạo jobSeekerProfiles/employerProfiles cho Admin. Client không cấp/sửa adminAccess hoặc tự gán ADMIN. Quyền xem/xét C05 cần cả grant enabled, User ADMIN và ACTIVE. Console bỏ qua Rules nên sửa dữ liệu trong Console không phải phép thử quyền client.

## Schema app tự tạo

### Request

`retirementRequests/{UUID}` cho SEEKER, `identityRequests/{UUID}` cho EMPLOYER.

| Nhóm | Field |
|---|---|
| Liên kết | id, userId, role, previousRequestId (rỗng khi lần đầu), baseUserVersion, identityProfileVersion |
| Dữ liệu khóa | identitySnapshot, professionalSnapshot, evidenceType, imagePaths, lookupMode = SIMULATED |
| Vòng đời | status STAGING → SUBMITTED → APPROVED/VERIFIED/NEEDS_INFO/REJECTED |
| Thời điểm | createdAt, updatedAt, consentedAt, submittedAt, reviewedAt |
| Quyết định | reviewedBy, lookupResult, reason, decisionVersion 0 → 1 |

identitySnapshot chỉ fullName, dateOfBirth dạng YYYY-MM-DD, documentNumber, source CCCD_QR, parserVersion 1 (7 trường với |) hoặc 2 (6 trường với || theo cấu trúc người dùng cung cấp). Không lưu raw QR, địa chỉ, giới tính hoặc số cũ. professionalSnapshot lấy từ dữ liệu đã lưu ở C04, không lấy từ form chưa lưu. evidenceType SEEKER là RETIREMENT_CERTIFICATE/BHXH_SIMULATED; EMPLOYER là IDENTITY_SAMPLE. LookupResult MATCH/NOT_MATCH/INSUFFICIENT là đối chiếu mô phỏng do Admin nhập, không tự gọi dịch vụ nhà nước.

Các field STAGING đã khóa ngay khi tạo. Chỉ được đổi trạng thái/timestamp khi submit; không đổi snapshot, đường dẫn, ảnh hay base version. Sửa nháp đã thử gửi cần bỏ nháp, tải lại và tạo ID mới.

### User/profile

- SEEKER: User.currentRetirementRequestId + retirementStatus.
- EMPLOYER: User.identityRequestId + identityStatus.
- Cả hai: User.approvedIdentityProfileVersion (0 nếu chưa duyệt/đã thu hồi); User.version tăng mỗi mutation.
- Profile tương ứng thêm fullName, dateOfBirth và identityProfileVersion. Không lưu documentNumber trong profile.
- Transaction gửi tăng identityProfileVersion, ghi thông tin đọc QR và đặt SUBMITTED; không tự APPROVED/VERIFIED. Chỉnh nghề nghiệp C04 giữ nguyên các field này.

### Storage

`verification/{uid}/{requestId}/identity.jpg`, `evidence1.jpg` đến `evidence3.jpg` nếu SEEKER. imagePaths trong request liệt kê chính xác các file được phép.

Chỉ chủ hồ sơ tạo file JPEG <= 10 MB khi request STAGING và tài khoản ACTIVE. Không cập nhật/xóa file, kể cả khi STAGING; app retry tái dùng nội dung giống hệt. Chủ hồ sơ và Admin có quyền đọc từng file; không list, không getDownloadURL trong app, không đưa đường dẫn/URL vào thông báo công khai. Rules kiểm MIME/kích thước; app kiểm giải mã ảnh. Không coi MIME là bằng chứng nội dung thật.

### Quyết định, thông báo, thu hồi

Quyết định ghi cùng transaction request + User + `users/{uid}/notifications/{requestId}_1`. Notification chỉ requestId, decisionVersion=1, status, reason, createdAt; không chứa snapshot số căn cước/ảnh. Nhận xét không ghi số giấy tờ hoặc thông tin định danh không cần thiết.

Theo yêu cầu rút gọn ngày 25/10, Chấp thuận không cần ô nhập nhận xét. Client tự ghi reason = “Hồ sơ đã được chấp thuận.” cho APPROVED/VERIFIED để giữ tương thích điều kiện reason không rỗng của Rules hiện hành; đây là dòng trạng thái, không phải nhận xét tự viết của Admin. NEEDS_INFO/REJECTED và thu hồi vẫn cần lý do thực nhập. Không đổi schema/Rules và không cần Publish lại cho bản sửa thao tác này.

Thu hồi ghi `verificationRevocations/{requestId}` bất biến gồm requestId/userId/role/identityProfileVersion/reason/reviewedBy/createdAt, đồng thời User REVERIFY_REQUIRED/approvedVersion0 và notification `{requestId}_2`, decisionVersion=2. Không đổi quyết định lịch sử trong request cũ.

Admin chỉ xử lý current request đúng identity version. Không tự xét/thu hồi mình; không sửa role, accountStatus hoặc contact người khác. Hai Admin xử lý đồng thời chỉ một quyết định thành công. Để tìm hồ sơ đã duyệt/thu hồi, chọn đúng loại rồi nhập mã request hoặc UID. Quyền bị thu hồi trên server có hiệu lực cho thao tác mới; dữ liệu đã xem không phải một lần xác minh quyền vĩnh viễn.

## Đối chiếu khi có lỗi

- Thiếu nút duyệt: kiểm tra đúng UID/Admin grant/User ADMIN ACTIVE; không thêm role ADMIN trong picker.
- Gửi bị từ chối: kiểm tra đã Publish cả hai bộ Rules, OTP liên kết/đồng bộ, hồ sơ nghề nghiệp, vùng active; gửi lỗi đầu tiên và bước xảy ra. Không chia sẻ ảnh giấy tờ/raw QR/token.
- Upload bị từ chối: kiểm tra cross-service permission và đúng bucket; request phải STAGING, tên file/MIME/size đúng. Không mở toàn bộ Storage.
- Lỗi/xung đột hoặc dừng chờ: tải lại trạng thái từ server trước tạo bản mới. Request/ảnh STAGING bỏ dở được quản trị rà dọn có chủ đích, không xóa hàng loạt lịch sử đã gửi.
- Không có seed request hoặc chỉ mục composite mới bắt buộc. Queue dùng equality status và giới hạn 50; thông báo orderBy một field createdAt. Nếu project đã tắt single-field index tương ứng, cần bật lại trong Console.

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
