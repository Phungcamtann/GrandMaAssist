# C06 — checklist người dùng

25/10/2026 — **người dùng đã cập nhật và kiểm tra ở các vai trò, xác nhận hoạt động tốt và duyệt C06**; xem [C06-ACCEPTANCE](C06-ACCEPTANCE.md), gồm đối chiếu địa chỉ chính xác theo v3. Không có kết quả runtime do Codex thực hiện. Giữ checkbox làm danh sách đối chiếu; không suy ra từng ca kỹ thuật khó đã được tái hiện từ xác nhận tổng quát. Dữ liệu nháp thử không chứa giấy tờ/OTP/token.

## Vào luồng và danh mục

- [ ] C06-01 EMPLOYER/ACTIVE → Trang chủ Soạn tin tuyển hoặc tab Tin của tôi mở được; danh sách ban đầu trống có phản hồi, không có số tin giả. SEEKER/Admin không mở soạn tin tuyển; điều hướng còn lại và C05 vẫn hoạt động.
- [ ] C06-02 Google EMPLOYER chưa có OTP/chưa VERIFIED vẫn soạn/lưu/xem trước nháp được. Không bắt giấy nghỉ hưu hoặc gửi C05 trước soạn. Online xác định role không đúng/không ACTIVE bị chặn.
- [ ] C06-03 Đã Publish Rules/seed đúng: tải bốn loại việc và các khu vực active. Cập nhật danh mục hoạt động, không tự đổi lựa chọn đang gõ khi dữ liệu về.
- [ ] C06-04 Lỗi quyền/thiếu seed/name sai kiểu/active string hiển thị lỗi hoặc trống phù hợp, không bịa danh mục. Dữ liệu đúng được tải sau sửa/cập nhật, không cần đổi JSON/SHA/Storage.

## Soạn, lưu và xem trước

- [ ] C06-05 Tạo nháp, nhập thiếu trường → Lưu nháp thành công; tên rỗng có nhãn Nháp chưa đặt tên nếu đã có nội dung khác. Form hoàn toàn rỗng không tạo bản vô tình. Mở lại giữ đúng từng giá trị đang gõ.
- [ ] C06-06 Điền tiêu đề/danh mục/mô tả/yêu cầu/kỹ năng/khu vực/ngày/buổi/lịch/thể lực/tiền/đơn vị/hạn. Sửa/lưu lại đúng một ID, không nhân đôi; danh sách theo lần lưu mới.
- [ ] C06-07 Xem trước thiếu/sai báo sát trường và tới lỗi đầu. Kiểm tra rỗng, giới hạn chữ, >8 kỹ năng, kỹ năng >60 ký tự, chưa chọn ngày/buổi, lịch trống.
- [ ] C06-08 Phân biệt chưa khai thể lực, chọn không có và có yêu cầu. SELECT nhưng chưa chọn yêu cầu bị báo; NONE xóa lựa chọn cũ, không tự coi ô chưa khai là không yêu cầu.
- [ ] C06-09 Tiền 150000 và 150.000/150,000 ra đúng 150.000đ; rỗng/0/âm/số thập phân 1,5/phân cách trộn 1.000,000/overflow không xem trước. HOUR/SESSION/DAY đúng nhãn, không nhân tiền hoặc tính phí trong C06.
- [ ] C06-10 Date/time picker giữ ngày/giờ đã chọn; hủy không thay giá trị trước. Hạn quá khứ/chưa chọn bị báo khi xem trước; lưu nháp dở vẫn được.
- [ ] C06-11 Xem trước đúng nội dung hiện tại, gồm phần chưa lưu; có Nháp/Chưa gửi duyệt. Quay lại sửa giữ mọi trường; lưu ở xem trước không tạo bản khác. Không có thao tác tự gửi Firebase/đã đăng.
- [ ] C06-12 Đổi ID category/area thành inactive trên Console → Cập nhật danh mục: giá trị cũ không đổi âm thầm, xem trước yêu cầu chọn lại. Cache lỗi không ghi đè snapshot hợp lệ bằng dữ liệu giả.

## Danh sách, xóa và dữ liệu bền

- [ ] C06-13 Tạo ít nhất hai nháp, sửa/xóa đúng bản. Xóa có xác nhận với tên; hủy giữ nguyên. Bấm lưu/xóa nhanh nhiều lần không tạo bản trùng/xóa nhầm.
- [ ] C06-14 Đóng/mở app hoặc dừng process sau khi đã lưu: nháp còn đúng; chưa lưu không tự xuất hiện như đã lưu. Xoay khi đang sửa giữ nháp RAM, không tự lưu hoặc gửi lại.
- [ ] C06-15 Rời form bằng Back/Trang chủ/Tài khoản/Tin của tôi khi dirty: Tiếp tục sửa giữ dữ liệu, Bỏ bỏ phần chưa lưu, Lưu chỉ đi tiếp khi SQLite thành công. Rời xem trước bằng Back về form, không mất dữ liệu.
- [ ] C06-16 Đăng xuất A → B không thấy/sửa/xóa nháp A; đăng nhập lại A thấy đúng nháp đã lưu. Callback cũ sau đổi UID không hiện form/message ở B. Không tự xóa nháp đã lưu lúc logout.

## Offline và các nhánh kỹ thuật

- [ ] C06-17 Sau lần online xác nhận EMPLOYER/tải danh mục, tắt mạng vẫn mở/sửa/lưu/xem trước nội dung đủ từ danh mục đã lưu; có nhãn local/cache, không báo tải server thành công giả.
- [ ] C06-18 Cold start offline với phiên Firebase Auth còn lưu: Main khi đang tải hoặc báo lỗi mạng có Nháp tin trên máy cho UID đã có marker C06; vào được danh sách. UID chưa từng xác nhận online không được dùng marker tài khoản khác để soạn.
- [ ] C06-19 Chưa có cache danh mục: vẫn lưu nội dung dở nhưng xem trước thiếu category/area bị chặn, trạng thái cần tải danh mục rõ. Bật mạng và cập nhật phục hồi, không mất nội dung.
- [ ] C06-20 Nếu tái hiện được hai màn cùng một ID: màn cũ không ghi đè/xóa bản revision mới; có thông báo và Tải lại nháp/danh sách. Không tái hiện được thì ghi theo dõi riêng.
- [ ] C06-21 Lỗi DB/đầy đĩa/quyền/migration/callback trễ: không giả báo đã lưu, không xóa bản cũ hoặc đổi UID. Chỉ đánh dấu sau khi tái hiện; không cần xóa dữ liệu ứng dụng để làm ca thường.
- [ ] C06-22 Backup/cloud/device transfer không mang DB nháp/cache/marker C06; nháp không đồng bộ lên Firebase. Đây là ca thiết bị riêng, không suy ra đã đạt chỉ từ XML exclude.

## Giao diện và hồi quy

- [ ] C06-23 Chữ 200%, màn ngang/hẹp/chia cửa sổ, bàn phím/cutout: cuộn tới mọi trường/nút, điều hướng 2 cột/ẩn lúc nhập như các màn cũ. Bốn vùng tách rõ, nút có khoảng trống; TalkBack đọc nhãn/error/nháp cần xóa.
- [ ] C06-24 Main ↔ Tin của tôi ↔ Hồ sơ/Tài khoản và từ C05 của EMPLOYER → Tin của tôi đúng trang, có hỏi trước khi bỏ nháp chưa gửi. Admin Xử lý và SEEKER vẫn giữ các mục đúng phạm vi.
- [ ] C06-25 Hồi quy Google/OTP/lưu phiên/liên kết/đổi số, vai trò/Hồ sơ thống nhất, QR/ảnh/gửi và Admin duyệt C05. Không thay đổi các quyết định đã có.
- [ ] C06-26 Firestore không có job/PENDING_REVIEW/notification mới chỉ vì C06 lưu/xem trước. Chỉ danh mục được đọc; client vẫn không ghi jobCategories hoặc jobs.

Các mục timeout/cạnh tranh/lỗi đĩa/backup/Rules/thiết bị chưa tái hiện phải ghi riêng, không tự coi đạt. Codex không chạy các ca này; người dùng nghiệm thu và tự commit. C07/C22 và C02-F01 giữ phạm vi/theo dõi trước đó.
