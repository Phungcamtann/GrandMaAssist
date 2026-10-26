# C08 — checklist người dùng nghiệm thu

26/10/2026. **Đã triển khai mã nguồn, chưa nghiệm thu.** Codex chỉ rà tĩnh; không Sync/build/test/lint/emulator/Rules tests/Console/deploy/stage/commit/push. Ngoại lệ Rules cục bộ lần sửa C07 không được dùng làm quyền tự kiểm thử C08. Thiết lập [Console/index](C08-CONSOLE-SCHEMA.md) trước.

**Bổ sung UI theo mẫu mới:** kiểm card ảnh minh họa/mạch thông tin/bấm cả card và thanh Quay lại–Tải lại cố định theo UI01–UI05 tại [C08-UI-REFINEMENT](C08-UI-REFINEMENT.md). Không cần thay Rules/index riêng cho bản UI này.

## Đường vào và danh sách

- [ ] C08-01 Đăng xuất, ở màn nhập số hoặc tab Google ban đầu: có Xem việc không cần đăng nhập. Mở được, không tạo tài khoản anonymous; Back trở về màn đăng nhập. Đang gửi/nhập OTP hoặc xử lý Auth không mở đường này.
- [ ] C08-02 SEEKER mở Trang chủ → Việc khác, thanh Việc khác; từ Hồ sơ/C05 cũng đến đúng màn, hỏi bỏ thay đổi nếu có. Topbar logo và vai trò đúng; Back/Home/Hồ sơ hoạt động. Việc của tôi, tìm việc từng bước và các mục chưa làm vẫn Sắp có.
- [ ] C08-03 Danh sách chỉ OPEN còn hạn, thứ tự hạn gần nhất rồi ID. Pending/rejected không xuất hiện. Rỗng khác lỗi mạng/index/quyền. Thẻ có tên việc, người thuê, loại/khu vực, tiền/đơn vị, hạn.
- [ ] C08-04 Bốn tổ hợp: không lọc, chỉ khu vực, chỉ loại, cả hai; chọn rồi Áp dụng mới đổi kết quả. Nhãn theo bộ lọc đã áp dụng, Bỏ bộ lọc về tất cả; không có kết quả không tự nới điều kiện.
- [ ] C08-05 Trên 20 tin phù hợp: Tải thêm nối đúng, không trùng ID; hết trang dừng (trang đúng 20 có thể cần lần tải cuối rỗng). Hai tin cùng expiresAt vẫn phân trang đúng. Đổi bộ lọc/Tải mới bắt đầu trang đầu.
- [ ] C08-06 Danh mục active mới/cũ tải được cho khách và SEEKER; inactive không được chọn từ nguồn mới. Danh mục nhiều hơn 100 phân trang; khi không tải được vẫn xem tất cả việc và có Tải lại danh mục. Kiểm hồi quy danh sách khu vực hồ sơ C04.

## Chi tiết và trạng thái mới

- [ ] C08-07 Xem việc hiện đúng toàn bộ mô tả, yêu cầu/kỹ năng/thể lực, ngày/buổi/lịch, tiền/đơn vị/hạn, người thuê, khu vực và **địa chỉ chính xác** từ tin C07. Dữ liệu dài xuống dòng/cuộn được; không có dữ liệu giấy tờ/liên hệ riêng hoặc reviewReason.
- [ ] C08-08 Đã xem tin online, quay lại/mở lần nữa online: đọc lại server, không dùng SQLite làm bằng chứng trạng thái mới. Tin hết hạn báo Hết hạn và ra khỏi danh sách. Không có thao tác ứng tuyển/lưu thật ở C08; nút hiện Sắp có và khóa.
- [ ] C08-09 Tin bị mất/không OPEN hoặc server từ chối get: không hiện lại bản cache thay lỗi, loại tin đó khỏi danh sách trong bộ nhớ và cache. Ca ẩn/đóng bằng nghiệp vụ chính thức nghiệm thu bổ sung ở C11; nếu chưa có điều kiện kiểm thì ghi theo dõi, không sửa Console phá schema để đánh dấu đạt.
- [ ] C08-10 Back trong lúc tải chi tiết dùng được, không bị callback trễ kéo trở lại chi tiết. Back về danh sách giữ filter/vị trí cuộn. Tải thêm không kéo lên đầu; Tải mới/Áp dụng có về đầu.

## SQLite và ngoại tuyến

- [ ] C08-11 Online tải/xem chi tiết → tắt mạng → Tải mới/mở lại tin: dùng bản đã lưu, có nhãn thời điểm lưu và chưa kiểm tra mới; giữ đầy đủ địa chỉ. Chưa tải tin trước đó thì báo chưa lưu, không báo rỗng như server.
- [ ] C08-12 Xem bản đã lưu trên máy: chỉ tập đã tải, lọc và Tải thêm local cùng thứ tự; không được gọi là toàn bộ tin. Bật lại mạng → Tải mới thay bằng dữ liệu server. Mở chi tiết từ cache vẫn thử server trước, chỉ lỗi mạng/timeout mới fallback.
- [ ] C08-13 Mất mạng lúc Tải thêm: khởi động lại danh sách cache cùng filter, không nối lẫn server cursor. Quá thời gian chờ có phản hồi; callback cũ không đổi bộ lọc/màn hiện tại.
- [ ] C08-14 Đóng/mở lại app offline giữ bản công khai đã lưu. Nếu Auth còn phiên nhưng hồ sơ không tải được, có nút Xem việc để mở cache công khai. Chưa có cache vẫn có đường Quay lại/Tải mới.
- [ ] C08-15 Cache tối đa 500 tin tải gần nhất; thiếu tin vì chưa tải/đã bị thay cache không được trình bày là không tồn tại trên server. Lỗi đọc/ghi SQLite có thông báo; lỗi lưu cache không phủ nhận kết quả server đã tải.
- [ ] C08-16 Đăng xuất A → khách → đăng nhập B không nhận dữ liệu riêng/lựa chọn của A. Cache C08 chỉ công khai dùng chung, không lưu UID; đổi UID khi đang tải đóng màn cũ, không hiển thị callback cho phiên mới. Nháp C06 vẫn chỉ đúng UID và còn sau đăng xuất.

## Giao diện và hồi quy

- [ ] C08-17 Xoay lúc tải/danh sách/chi tiết giữ đúng nội dung, không nhân request; đóng process rồi mở lại có đường tải lại/cache, không khẳng định trạng thái server đã được phục hồi chỉ từ saved state.
- [ ] C08-18 Chữ 200%, ngang/hẹp, cutout/IME, TalkBack: nhãn spinner, status, thứ tự đọc và tên nút Xem việc có tên tin; cuộn đến toàn bộ nội dung/hành động. Các nút đủ vùng chạm, có khoảng cách, nav không che nội dung.
- [ ] C08-19 Hồi quy C01–C07: Google/Phone/liên kết/đăng xuất, hồ sơ/chọn khu vực, C05 QR/gửi/Admin, nháp theo UID/địa chỉ, gửi/duyệt/từ chối/gửi lại/history C07 vẫn đúng. Không cần đổi SHA/JSON/Storage.
- [ ] C08-20 Quyền nếu có tooling phù hợp: khách đọc active catalog và OPEN, không đọc pending/rejected/history/profile/admin/Storage riêng, không ghi jobs/catalog. Giữ ca chưa làm riêng; không tự chạy Rules tests hoặc tái sử dụng kết quả 24 ca C07 như bằng chứng C08.

Ghi nhận ca timeout/callback trễ/thiết bị/cạnh tranh chưa tái hiện và C02-F01 riêng, không tự đánh dấu đạt. Người dùng tự review và commit sau nghiệm thu; không chuyển C09 khi C08 chưa được duyệt.
