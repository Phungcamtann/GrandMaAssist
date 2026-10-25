# C07 — bổ sung địa chỉ làm việc công khai trên tin

25/10/2026 — **người dùng đã chốt**, thay thế đề xuất trước đó chỉ nhập địa chỉ chính xác tại thỏa thuận ca. Lý do: khu vực quá rộng; người tìm việc cần biết địa điểm cụ thể trước khi quyết định ứng tuyển. Bố cục C07 cũng đã được duyệt; không cần xin lại xác nhận cho phạm vi này.

## Dữ liệu và cách hiển thị

- Giữ `areaCode` cho phân loại và bộ lọc của cả hai vai trò. Thêm `workAddress` do người thuê tự nhập cho từng tin, không bắt người tìm việc khai địa chỉ nhà.
- Nhãn Địa chỉ làm việc; ô nhiều dòng ngay dưới Khu vực trong form. Gợi ý Số nhà, đường, phường/xã và tỉnh/thành. Chỉ có dòng phụ ngắn: Địa chỉ sẽ hiển thị trên tin đã duyệt.
- `workAddress` bắt buộc khi xem trước/gửi, trim không rỗng, tối đa 500 ký tự. Lưu nháp dở vẫn cho trống; vượt giới hạn báo sát trường. Đây là địa chỉ người thuê cung cấp, không được gắn nhãn đã được bản đồ/cơ quan xác minh.
- Lưu cùng nội dung nháp theo UID, giữ chính sách logout/backup của C06. Nháp cũ thiếu trường đọc thành chuỗi rỗng; không xóa dữ liệu, không ép người dùng gỡ app. Khi xem trước/gửi nháp cũ, yêu cầu bổ sung địa chỉ.
- Bản xem trước, chi tiết chủ tin và Admin hiển thị đầy đủ địa chỉ. Thẻ danh sách ngắn vẫn có thể chỉ hiện khu vực; ngay khi mở chi tiết tin OPEN phải thấy địa chỉ, không đợi được chấp nhận hoặc thỏa thuận ca. Giao diện danh sách/chi tiết công khai triển khai ở C08 theo thứ tự đã chốt.

## Quyền và vòng đời

- `JobPost.workAddress` là dữ liệu công khai khi tin OPEN, kể cả khách được xem tin theo ma trận quyền. PENDING_REVIEW/REJECTED vẫn chỉ chủ tin và Admin được đọc theo quyền hiện hành. Rules kiểm trường/type/độ dài và version cùng phần nội dung tin.
- Không tự đọc/điền địa chỉ thường trú từ QR CCCD, không đưa số điện thoại/giấy tờ/ghi chú kiểm duyệt vào địa chỉ. Không thêm GPS, bản đồ, geocoding hoặc tính khoảng cách vào C07.
- Admin xét địa chỉ cùng nội dung tin. Sửa địa chỉ là sửa nội dung: tin REJECTED gửi lại phải tăng version ở C07; tin OPEN sửa ở C11 phải trở lại PENDING_REVIEW, không sửa âm thầm khi vẫn OPEN.
- C08 cache địa chỉ công khai cùng bản tin đã đọc; không nhầm với `AgreementSnapshot.privateAddress`. C12 snapshot đơn giữ `workAddress` theo đúng jobVersion để tin sửa sau này không đổi snapshot cũ.
- C15/S15 vẫn chốt địa chỉ chính xác của từng ca trong phiên bản thỏa thuận. Có thể gợi ý từ địa chỉ tin nhưng hai bên phải xác nhận lại; hướng dẫn tiếp cận/điểm hẹn hoặc nội dung riêng của ca không tự công khai ngược lên tin.

## Bàn giao và kiểm tra

Khi code C07 phải cập nhật model/serialization/copy/dirty detection/validator, đọc nháp cũ, form/bản xem trước/chi tiết, gửi và gửi lại, Rules whitelist/schema, tài liệu Console và checklist. Giữ đúng UID, dữ liệu khi xoay/đóng mở/lỗi mạng, và chống gửi trùng. C08/C11/C12/C15 kế thừa yêu cầu này trong phạm vi của từng luồng.

Ca người dùng cần kiểm: nháp C06 cũ; trống/chỉ khoảng trắng/quá 500 ký tự; địa chỉ dài có dấu và xuống dòng; lưu/mở lại/xoay/offline; gửi lần đầu; bị từ chối rồi sửa địa chỉ/gửi lại; Admin đọc đúng địa chỉ; không lộ tin chờ cho tài khoản khác. Hiển thị tin OPEN công khai được nghiệm thu cùng C08.

**Trạng thái:** đã bổ sung địa chỉ vào mã nguồn C07 và file Rules local, chờ người dùng cập nhật Console/Sync/Run/nghiệm thu. Xem [C07-FILES](C07-FILES.md). Chưa sửa Console, không có kết quả build/test do Codex thực hiện, không commit/push. Màn tin công khai/cache/snapshot đơn và thỏa thuận tiếp tục triển khai theo các luồng C08/C12/C15.
