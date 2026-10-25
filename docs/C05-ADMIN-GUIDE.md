# C05 — cấp tài khoản Admin và duyệt hồ sơ

25/10/2026. Áp dụng cho app hiện tại, project Firebase `grandmaassist-a1dfa`.

## 1. Đăng nhập lần đầu để có UID Admin

1. Chuẩn bị một tài khoản Google riêng chưa chọn vai trò Tìm việc/Thuê người trong app.
2. Đăng xuất tài khoản đang gửi hồ sơ. Đăng nhập app bằng Google riêng đó.
3. Nếu hiện màn chọn vai trò, dừng tại đây, không chọn vai trò và không bấm Tiếp tục.
4. Trong Firebase Console bằng tài khoản quản lý project hiện tại, vào Authentication → Users. Tìm đúng email Google vừa đăng nhập và sao chép UID của nó.
5. Đăng xuất tài khoản vừa đăng nhập trong app trước khi cấp quyền.

App tạo Firebase Auth user khi đăng nhập Google lần đầu; không cần tạo email/password bằng Add user trong Console vì app hiện chỉ có Google/OTP. Nếu UID đã có hồ sơ SEEKER/EMPLOYER, dùng tài khoản Google riêng khác, không sửa/xóa hồ sơ nghiệp vụ cũ để chuyển thành Admin. Tài khoản Admin app không cần được mời làm quản trị Firebase project.

## 2. Tạo grant trong Firestore

Firestore Database → Data → Start collection:

- Collection ID: `adminAccess` (nếu đã có thì mở collection đó).
- Document ID: dán UID Admin vừa lấy; không dùng Auto-ID hoặc email.
- Field: `enabled`, type **boolean**, value **true**.
- Save.

## 3. Tạo User Admin

Trong Firestore Data, tạo/mở collection `users`, thêm document có ID bằng đúng UID Admin trên.

| Field | Type | Value |
|---|---|---|
| id | string | Dán UID Admin |
| role | string | ADMIN |
| accountStatus | string | ACTIVE |
| displayName | string | Quản trị viên |
| createdAt | timestamp | Chọn ngày/giờ hiện tại |
| updatedAt | timestamp | Chọn ngày/giờ hiện tại |

ADMIN/ACTIVE viết hoa; enabled phải là boolean. UID trong adminAccess, document users và field id phải giống nhau. Không tạo profile nghề nghiệp cho Admin; không cần thêm phone/version hoặc hồ sơ căn cước cho tài khoản Admin thử này.

Nếu đã có hai document đúng từ C04 thì giữ nguyên. Nếu đang thấy document users của UID này mang role SEEKER/EMPLOYER, quay lại dùng tài khoản riêng khác.

### Nếu app báo “Tài khoản quản trị chưa được cấp đủ quyền”

App đọc `enabled` từ `adminAccess/{uid}`, nhưng đọc `role`/`accountStatus` từ **`users/{uid}`**. Hai collection phải nằm ở cấp gốc, cùng cấp với `areas` và `appConfig`; `users` không phải collection con của document `adminAccess`.

Nếu đã đặt toàn bộ thông tin Admin trong `adminAccess`, giữ `enabled` boolean true tại đó rồi tạo/kiểm tra document tương ứng trong collection gốc `users` theo bảng ở mục 3. Các field role/accountStatus/displayName trong adminAccess không thay thế User; không cần xóa chúng để khắc phục lỗi đọc quyền này.

Document ID của cả hai phải bằng UID thực sao chép từ Authentication → Users của đúng tài khoản Google đang đăng nhập app. Field `id` trong User cũng dán chính UID đó; không nhập nguyên văn “UID Admin”, email hoặc Auto-ID. Nếu User đã có role SEEKER/EMPLOYER, không đổi vai trò nghiệp vụ cũ để sửa lỗi; dùng tài khoản riêng chưa chọn vai trò.

Sau khi lưu đủ hai document, bấm Tải lại hồ sơ/Kiểm tra lại tài khoản nếu có, hoặc đăng xuất rồi đăng nhập lại. Chỉ sửa dữ liệu cấp quyền này không cần sửa mã nguồn/build lại/Publish lại Rules. Đây là hướng dẫn đối chiếu từ ảnh và mã nguồn; chưa xác nhận hai document thực tế trên Console đã đúng hoặc Admin đã đăng nhập thành công.

## 4. Đăng nhập Admin

Đăng nhập lại app bằng đúng Google đã lấy UID. App đọc grant và User từ server, mở **Tổng quan hàng đợi**, topbar ghi **Admin**. Có thẻ **Kiểm tra hồ sơ** và thanh điều hướng **Tổng quan / Xử lý / Thống kê / Tài khoản**. Admin không chọn vai trò trong app.

Nếu vẫn ở màn chọn vai trò, bấm **Kiểm tra lại tài khoản** hoặc đăng xuất rồi đăng nhập lại; đối chiếu đúng email/UID cùng hai document. Nếu báo chưa được cấp đủ quyền, kiểm tra enabled=true, role=ADMIN, accountStatus=ACTIVE và hai bộ Rules C05 đã Publish. Không thêm tùy chọn ADMIN vào màn chọn vai trò. Bản bổ sung điều hướng chỉ cần Sync/Run mã nguồn; không phải đổi grant hoặc Publish lại Rules nếu cấu hình C05 đang đúng.

Số hồ sơ trên Tổng quan lấy từ server, tối đa 50 mỗi loại; chạm giới hạn hiện 50+ và tổng có dấu +. Bấm Tải lại để cập nhật. Lỗi tải được báo riêng, không giả hiện 0. Tin chờ duyệt/Báo cáo/Tranh chấp/Ca quá hạn và Thống kê hiện Sắp có, chưa dùng để thử nghiệp vụ.

## 5. Xem hồ sơ

1. Chọn **Kiểm tra hồ sơ** trên Tổng quan hoặc tab **Xử lý** bên dưới.
2. Chọn **Hồ sơ nghỉ hưu** cho SEEKER hoặc **Danh tính người thuê** cho EMPLOYER.
3. Bấm **Tải lại** nếu cần cập nhật danh sách chờ. Chọn tên hồ sơ.
4. Màn **Kiểm tra hồ sơ** chỉ hiện chi tiết bản đã chọn; xem thông tin đọc QR, thông tin nghề nghiệp và từng ảnh. Chạm ảnh để phóng to/kéo; có thao tác phóng to qua accessibility.
5. SEEKER cần căn cước và bằng chứng giấy nghỉ hưu/BHXH mô phỏng. EMPLOYER không cần bằng chứng nghỉ hưu.

Quay lại hoặc tab Xử lý trở về danh sách; nếu có nhận xét/lựa chọn chưa lưu thì app hỏi trước khi bỏ. Tab Tổng quan trở về trang chính và đọc lại số chờ. Tab Tài khoản có phương thức đăng nhập/đăng xuất; Back sau đăng xuất không mở lại hồ sơ riêng.

Ảnh chưa tải đủ thì có Tải lại ảnh và chưa được quyết định. Muốn xem bản đã quyết định hoặc tìm hồ sơ cụ thể, chọn đúng loại rồi nhập UID chủ hồ sơ hoặc mã request vào ô tìm. Khi có bản trước, có nút xem lịch sử.

## 6. Lưu quyết định

Sau khi xem đủ ảnh và đối chiếu theo kịch bản mô phỏng của nhóm:

1. Chọn **Phù hợp**, **Không phù hợp** hoặc **Chưa đủ căn cứ**.
2. Tích **Đã xem đủ ảnh và đối chiếu thông tin** sau khi đã xem đủ ảnh.
3. Chọn hành động:

- **Chấp thuận → Xác nhận**: không có ô nhập nhận xét, không yêu cầu gõ lý do. Nút chỉ mở khi chọn Phù hợp.
- **Yêu cầu bổ sung/Từ chối**: app mở ô Lý do tại vùng quyết định và cuộn tới đó. Nhập lý do ngắn, bấm nút hành động bên dưới ô rồi Xác nhận. Để trống hoặc ghi số căn cước sẽ có lỗi ngay tại ô. Hủy lựa chọn đóng ô mà không ghi quyết định.

Trạng thái Đang lưu…/lỗi hiển thị ngay cạnh các nút. Khi server xác nhận thành công, app trở về danh sách với thông báo kết quả, tải lại hàng đợi. Nếu lỗi phản hồi sau ghi, tải lại để kiểm tra trước khi quyết định tiếp; không tự gửi lại âm thầm.

| Hành động | Kết quả |
|---|---|
| Chấp thuận | SEEKER → APPROVED; EMPLOYER → VERIFIED. Chỉ cho phép khi đối chiếu Phù hợp; không nhập nhận xét. |
| Yêu cầu bổ sung | NEEDS_INFO, chủ hồ sơ xem lý do rồi gửi bản mới. |
| Từ chối | REJECTED, chủ hồ sơ xem lý do và có thể chuẩn bị bản mới. |

App ghi quyết định, cập nhật User và thông báo cùng transaction; hồ sơ đã quyết định rời danh sách SUBMITTED. Không tự sửa status trong Firestore Console để thay thao tác duyệt của app. Quy trình này là xét duyệt mô phỏng trong đồ án, không phải xác nhận từ cơ quan nhà nước. QR đọc được không tự thay cho đối chiếu bằng chứng.

Để tương thích Rules đang triển khai, Chấp thuận tự ghi dòng trạng thái “Hồ sơ đã được chấp thuận.” vào reason của request/notification; đây không phải nhận xét do Admin nhập. Giao diện không lặp dòng này bên dưới trạng thái, nhưng vẫn hiện nhận xét lịch sử/lý do bổ sung hoặc từ chối. Bản sửa này không đổi schema/Rules và không cần Publish lại.

## 7. Kiểm tra kết quả ở tài khoản người dùng

Đăng xuất Admin → đăng nhập lại tài khoản đã gửi → **Xem hồ sơ** trên Trang chủ hoặc Hồ sơ/Tài khoản → khối **Xác minh hồ sơ**. Đã duyệt hiện **Đã xác minh → Xem lại hồ sơ**, mở trực tiếp bản hiện hành chỉ đọc; đang chờ có Xem hồ sơ đã gửi. Cần bổ sung/từ chối/thu hồi hiện nút tương ứng để vào wizard xem lý do và chuẩn bị bản mới. Thông báo kết quả trong wizard vẫn giữ. Nếu dùng hai thiết bị, mở lại Hồ sơ hoặc đưa app lên foreground để cập nhật trạng thái.

Để kiểm tra nhánh bổ sung, dùng hồ sơ thử khác hoặc thực hiện ca này trước khi chấp thuận: Admin yêu cầu bổ sung → người dùng gửi bản mới → Admin duyệt bản mới. Không sửa nội dung của request đã gửi.

## 8. Thu hồi và lỗi thường gặp

- Muốn thu hồi kết quả hiện hành: chọn đúng loại, tìm bằng mã request/UID, xem ảnh, tích đã đối chiếu rồi **Thu hồi kết quả** → nhập lý do trong ô vừa mở → **Thu hồi kết quả → Xác nhận**. User chuyển cần xét duyệt lại; quyết định lịch sử vẫn giữ nguyên.
- Danh sách trống: kiểm tra chọn đúng loại và request đã SUBMITTED; bấm Tải lại. Chỉ 50 hồ sơ chờ được hiển thị mỗi lượt; xử lý rồi tải tiếp.
- Nút quyết định chưa dùng được: kiểm tra ảnh đã tải đủ, đã tích xác nhận và chọn kết quả đối chiếu; hồ sơ phải đang SUBMITTED. Chấp thuận chỉ mở với Phù hợp, không cần nhận xét.
- Lỗi quyền xem ảnh: đối chiếu grant/User Admin, Storage Rules và quyền kết nối Storage–Firestore đã cấp khi Publish.
- Lỗi xung đột: có thể bản hiện hành đã đổi hoặc Admin khác đã xử lý; tải lại để đọc kết quả. Không ép trạng thái bằng Console hoặc tự gửi quyết định lại nhiều lần.

Người dùng đã xác nhận sửa quyền, đăng nhập Admin/xét duyệt và nay chốt tất cả bản hiện tại, gồm Hồ sơ thống nhất/sửa sau pull. Xem [C05-ACCEPTANCE](C05-ACCEPTANCE.md). Kết quả do người dùng thực hiện, không phải Codex kiểm thử; các ca Rules/timeout/thiết bị chưa tái hiện vẫn tiếp tục theo dõi riêng.

Nếu còn lỗi lưu trên bản debug, lấy các dòng tag **GrandMaReview**: DECISION_START, DECISION_SAVED hoặc DECISION_FAILED kèm mã như PERMISSION_DENIED/UNAVAILABLE. Log chỉ có giai đoạn/mã lỗi, không có UID, ảnh, số căn cước, lý do hay nội dung exception. Nếu chỉ có DECISION_START thì chưa có xác nhận lưu; không suy ra đã duyệt.
