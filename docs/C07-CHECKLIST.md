# C07 — checklist người dùng nghiệm thu

**26/10/2026:** có ngoại lệ được người dùng cho phép để kiểm Rules cục bộ trong lần điều tra lỗi gửi tin. Đã tái hiện lỗi 1.000 biểu thức và bản sửa r4 đạt 24/24 ca giả lập ([báo cáo](C07-EXPRESSION-LIMIT-FIX.md)). Đây là bằng chứng kiểm Rules, không tự đánh dấu các ca UI/Firebase thật bên dưới; người dùng cần Publish r4 và thử lại trên thiết bị.

25/10/2026. **Chờ người dùng kiểm tra** sau [thiết lập Console](C07-CONSOLE-SCHEMA.md). Codex chỉ rà tĩnh, không chạy Sync/build/test/app hoặc Rules tests. Ghi thiết bị/API, tài khoản theo vai trò và kết quả thật; không đưa giấy tờ/số điện thoại/địa chỉ thật vào log báo lỗi. Không suy ra mọi ca đạt từ nghiệm thu C06.

## Địa chỉ và nháp

- [ ] C07-01 Mở nháp C06 cũ: không mất tiêu đề/lịch/tiền, địa chỉ trống; vẫn lưu dở được. Xem trước/gửi yêu cầu bổ sung địa chỉ, lỗi sát trường.
- [ ] C07-02 Địa chỉ chỉ khoảng trắng hoặc >500 ký tự bị chặn, tiếng Việt/xuống dòng giữ đúng; bản xem trước hiện đầy đủ khu vực/địa chỉ, không cắt nội dung.
- [ ] C07-03 Lưu/đóng mở/xoay/soạn offline giữ địa chỉ theo UID; đăng xuất A→B không thấy nháp A. Cache/offline C06 còn hoạt động; không gửi tin offline.

## Gửi tin

- [ ] C07-04 EMPLOYER ACTIVE đủ số/hồ sơ gửi → một jobs/{id} PENDING_REVIEW v1 và history/1 SUBMITTED. Địa chỉ và mọi trường đúng bản xem trước; chưa duyệt không hiện công khai.
- [ ] C07-05 Thiếu OTP hoặc contact chưa đồng bộ: báo đúng, có đường mở Tài khoản / hồ sơ; chọn lưu nháp trước khi rời rồi quay lại mở được nháp cũ. Thiếu giới thiệu/khu vực hồ sơ/tên đơn vị ORGANIZATION cũng chặn đúng; không buộc C05 VERIFIED mới soạn/gửi.
- [ ] C07-06 Danh mục/khu vực vừa inactive, hết hạn, thiếu trường, sai vai trò hoặc tài khoản bị hạn chế: không gửi thành công giả, không mất nháp. Lỗi Rules/index có phản hồi; không làm trống danh sách như đã tải thành công.
- [ ] C07-07 Bấm gửi nhiều lần, xoay khi đang gửi, mở lại nháp rồi gửi lại: cùng ID, không thêm tin/version/sự kiện trùng. Tin đã xử lý vẫn được đối chiếu, không quay lại chờ duyệt chỉ vì retry.
- [ ] C07-08 Sửa local sau lần gửi rồi bấm gửi lại cùng nháp: hiển thị bản server và thông báo phần sửa chưa thay đổi tin; không âm thầm ghi đè PENDING_REVIEW/OPEN.
- [ ] C07-09 Nếu tái hiện mạng chậm/mất phản hồi sau commit: sau tối đa 45 giây UI báo chưa rõ kết quả, thao tác lại được; tải lại/thử cùng nháp đối chiếu đúng ID. Không tự coi timeout là chưa ghi server.

## Tin của tôi và điều hướng

- [ ] C07-10 Tin của tôi có Nháp trên máy và Tin đã gửi; chủ A không thấy tin riêng B. Tải lại/rỗng/lỗi phân biệt rõ. Có đủ >20 tin thì Tải thêm không trùng, hết trang dừng; tải lại làm mới thứ tự.
- [ ] C07-11 Mở chi tiết từ lần gửi thành công và từ danh sách: đúng trạng thái/nội dung/tiền/hạn/địa chỉ/lý do. Mất mạng vẫn Back được về danh sách hoặc rời màn sau khi tác vụ đã kết thúc.
- [ ] C07-12 Back/Trang chủ/Tài khoản và chuyển từ nháp sang tin đã gửi không mất nội dung chưa lưu mà không hỏi. C05 ↔ Duyệt tin, Admin Xử lý chọn đúng loại; không quay về dữ liệu của UID cũ.

## Admin

- [ ] C07-13 Admin được cấp đúng C05 vào Tổng quan → Duyệt tin tuyển hoặc Xử lý → Duyệt tin tuyển; hàng chờ chỉ PENDING_REVIEW, số lượng không giả. Thẻ và chi tiết chia vùng, địa chỉ đầy đủ.
- [ ] C07-14 Chấp thuận → xác nhận, không cần nhận xét → OPEN v2 và history/2 APPROVED, reviewedBy/reviewedAt thật; tiến trình/kết quả gần nút, không kẹt busy.
- [ ] C07-15 Từ chối mới mở ô lý do; trống/chỉ khoảng trắng/>1000 không gửi. Hủy giữ tin chưa quyết định; hợp lệ → REJECTED, reason đúng, history cùng transaction. Người thuê Tải lại đọc đúng lý do.
- [ ] C07-16 Tin hết hạn trước lúc duyệt hoặc danh mục/khu vực inactive không được OPEN; có thể từ chối với lý do. Tải lại sau tin đã được xử lý không duyệt thêm lần nữa.
- [ ] C07-17 Nếu tái hiện hai Admin/đổi phiên bản/mất quyền giữa lúc xem và ghi: chỉ quyết định hợp lệ thắng; bản cũ báo tải lại, không ghi đè. Admin không duyệt tin thuộc UID của mình.
- [ ] C07-18 Nhập lý do rồi xoay còn nội dung; Back/tải lại/đổi mục hỏi trước khi bỏ. Chấp thuận/từ chối khi đang xử lý không nhận thêm yêu cầu; sau lỗi có đường thử lại/tải lại.

## Sửa và gửi lại

- [ ] C07-19 REJECTED → Sửa và gửi lại: form là nội dung server có baseVersion hiện tại; có xác nhận về nháp cùng ID. Sửa địa chỉ/nội dung/hạn → lưu/xem trước/gửi: cùng jobId, v3 PENDING_REVIEW, quyết định hiện hành được xóa, history/2 giữ nguyên và history/3 SUBMITTED đúng before/after.
- [ ] C07-20 Đóng mở bản nháp đã lưu khi sửa tin từ chối giữ baseVersion. Retry sau gửi lại không tạo thêm version. Admin thấy bản mới, duyệt thành OPEN v4; người thuê thấy đúng kết quả mới.
- [ ] C07-21 Tin OPEN/PENDING_REVIEW không có đường sửa trực tiếp trong C07. Sửa tin đã duyệt/đóng/ẩn thuộc C11, ứng tuyển thuộc C12, địa điểm thỏa thuận ca thuộc C15.

## Quyền, giao diện và hồi quy

- [ ] C07-22 Kiểm quyền trực tiếp nếu có điều kiện: khách/SEEKER/người thuê khác không đọc tin PENDING_REVIEW/REJECTED hoặc history; query không đúng filter/limit bị từ chối. OPEN đọc được theo quyền public, địa chỉ công khai đúng chủ đích; màn công khai nghiệm thu ở C08.
- [ ] C07-23 Ghi trực tiếp sai owner/version/trường tiền/hạn/địa chỉ, tự đặt OPEN hoặc chỉ ghi history/decision thiếu tài liệu đối ứng phải bị Rules từ chối. Sửa/xóa lịch sử không được phép. Chỉ đánh dấu sau kiểm thử thật.
- [ ] C07-24 Đổi UID giữa callback/tác vụ, lỗi SQLite lưu trước gửi và các biến thể timeout không ghi/hiện nhầm; ca khó chưa tái hiện ghi theo dõi riêng, không tự đánh dấu đạt.
- [ ] C07-25 Chữ 200%, màn ngang/hẹp, IME/cutout, TalkBack dùng được, vùng chạm và khoảng cách rõ; topbar logo/vai trò và nav theo mẫu. Danh sách dài và địa chỉ nhiều dòng vẫn cuộn tới hành động.
- [ ] C07-26 Hồi quy C01–C06 và Admin duyệt hồ sơ C05; QR/upload không bị thay đổi. Các mục sau vẫn khóa/Sắp có; không có map/GPS/push/ứng tuyển/thống kê giả.

Các ca cạnh tranh/Rules âm tính/timeout/thiết bị chưa làm giữ riêng trong danh sách theo dõi; người dùng tự commit sau nghiệm thu, Codex không tự commit/push/deploy.
