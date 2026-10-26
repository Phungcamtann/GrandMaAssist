# C08 — tối ưu card xem việc và thanh thao tác

26/10/2026. Người dùng đã Sync/Run và duyệt các luồng C08 hiện tại. Bản tinh chỉnh UI này tiếp tục thu gọn phần lọc theo phản hồi mới; chưa Sync/build/test/app riêng phần tinh chỉnh, chưa commit. Giữ nguyên `.idea/misc.xml` do người dùng thay đổi và phần C08 đang có; không stage/commit/push/deploy.

## Card mới

- Ảnh minh họa theo categoryId: chăm cây, phụ bếp, thư viện, cửa hàng. Nguồn Pexels đóng gói cục bộ, không tải ảnh lúc xem việc; xem [nguồn asset](assets/C08-ASSETS.md). Loại việc khác dùng icon chung, không đoán ảnh từ tên tin/đơn vị.
- Nhãn Ảnh minh họa tách khỏi Tin đã kiểm duyệt. Kiểm duyệt nói về tin OPEN từ server, không nói người trong ảnh/nhà tuyển dụng đã được xác minh. Card cache đổi badge thành Bản đã lưu và giữ thời điểm lưu.
- Trình tự: ảnh → badge → tiêu đề → người thuê/khu vực → loại việc → ngày/buổi/lịch → đường phân cách → tiền công/đơn vị → hạn nhận; bản cache thêm thời điểm lưu.
- Tiêu đề 24sp/32sp Plus Jakarta Sans Bold; nội dung chính 20sp, badge phụ 16sp. Tiêu đề tối đa 3 dòng, người thuê/khu vực và lịch xem nhanh tối đa 2 dòng; chi tiết và mô tả trợ năng giữ nội dung đầy đủ. Không thu nhỏ chữ để ép vừa card.
- Ngày theo thứ tự trong tuần, rút gọn T2…CN trên card, Hằng ngày khi đủ bảy ngày; TalkBack đọc tên ngày đầy đủ. Không tự suy ra số giờ, tiền theo giờ, khoảng cách hoặc nhãn Mô phỏng khi tin không có dữ liệu tương ứng.
- Bấm toàn bộ card mở chi tiết, có ripple/viền focus/mũi tên; bỏ nút Xem việc chiếm một hàng lớn. Card là một điểm focus trợ năng có vai trò nút, nội dung việc và hành động Xem chi tiết công việc; không đọc lặp nội dung con. Đang tải không nhận bấm thêm.

## Quay lại và Tải lại

Thanh nằm ngay dưới topbar xanh, ngoài ScrollView, luôn thấy khi cuộn. Quay lại trái, Tải lại gọn phải, cùng hàng ở danh sách và chi tiết. Nhãn ngắn Tải lại, tooltip/mô tả trợ năng Tải mới từ mạng, vẫn gọi vm.reload().

Chữ 20sp và vùng chạm tối thiểu 56dp; nút phải wrap_content trong nửa hàng, không chiếm toàn chiều ngang. Nhãn xuống dòng khi chữ lớn, thanh cùng lề/giới hạn chiều rộng nội dung. Back vẫn dùng khi tải; Tải lại khóa trong lúc tác vụ chạy.

## Thu gọn bộ lọc

- Danh sách chỉ hiện nút xanh **Tìm theo ?** ngay dưới tiêu đề. Bấm nút này mới mở vùng chọn khu vực/loại việc, Áp dụng và Bỏ bộ lọc; bấm lại để thu gọn. Trạng thái mở/đóng giữ qua xoay màn hình.
- Bỏ nút **Xem bản đã lưu trên máy**. Cache vẫn tự ghi khi tải online và tự được dùng khi không có mạng, cùng badge/thông báo bản đã lưu như trước; không đổi repository, ViewModel, SQLite hay điều kiện fallback.
- Đổi dòng đếm thành **Hiện đang có n việc**. Tiêu đề card tăng 24sp lên 26sp, vẫn Plus Jakarta Sans Bold 700; nội dung bên dưới giữ 20sp.

## File và phạm vi

Sửa PublicJobsActivity.java, activity_public_jobs.xml, public_job_strings.xml. Thêm item_public_job.xml, public_job_styles.xml, color/public_card_stroke.xml, drawable/public_back_button.xml, public_job_badge.xml, ic_public_chevron.xml, bốn JPEG drawable-nodpi và tài liệu/credits. Không sửa ViewModel/repository/model/SQLite, Rules/index/schema/Auth/Storage hoặc Gradle cho lần tối ưu UI này.

## Người dùng kiểm tra

- [ ] UI01 Online: ảnh đúng loại, nhãn minh họa/kiểm duyệt riêng; thông tin tên/người thuê/khu vực/ngày/lịch/tiền/hạn đúng tin. Loại chưa ánh xạ dùng icon chung.
- [ ] UI02 Bấm ảnh/thân/card hoặc bàn phím/TalkBack mở đúng chi tiết; không đọc lặp hoặc mở lặp lúc tải. Chi tiết/địa chỉ vẫn đầy đủ.
- [ ] UI03 Cuộn cuối danh sách/chi tiết: Quay lại và Tải lại vẫn ở hàng trên; Tải lại đọc server, Back giữ vị trí danh sách, Tải thêm giữ hoạt động cũ.
- [ ] UI04 Offline: ảnh vẫn có, badge Bản đã lưu/thời điểm đúng; không coi minh họa hoặc badge là bằng chứng tin còn OPEN mới nhất.
- [ ] UI05 Bộ lọc: lúc mới vào chỉ có nút Tìm theo ?, mở/đóng không đổi filter hay tải lại danh sách; xoay giữ trạng thái. Offline vẫn tự dùng cache, không còn nút yêu cầu hiểu "bản đã lưu".
- [ ] UI06 Xoay, màn hẹp, chữ 200%, TalkBack: thanh không tràn ngang, nút đủ chạm, card lớn theo chữ/cuộn được, chi tiết chứa đầy đủ phần rút gọn trên card.

Đã rà tĩnh đợt card trước: parse 66 XML, đối chiếu tài nguyên Java/XML gồm ảnh JPEG và style mới; kiểm chỉ có một cặp Back/Reload cùng thanh ngoài ScrollView; rà chuỗi/comment/ngoặc Activity và git diff --check. Đợt thu gọn này cần Sync/Run do người dùng thực hiện; Codex không chạy app để tự nghiệm thu.

Chỉ Sync/Run để cập nhật UI; không cần Publish thêm Rules/index riêng cho lần sửa này. Thiết lập Console C08 trước đó vẫn cần hoàn tất nếu chưa làm. Codex không chạy lại build/app/test hoặc commit.
