# C06 — chuẩn bị nháp tin tuyển và xem trước

25/10/2026. **Người dùng đã duyệt kế hoạch và yêu cầu triển khai; mã nguồn C06 đã hoàn thành, chờ người dùng nghiệm thu.** Nền đã đối chiếu `11fcdebd6db0b70cab9d3289121b2a7f352edd7d`; trước khi code chỉ có tài liệu chuẩn bị/nghiệm thu chưa commit. Bàn giao hiện hành: [C06-FILES](C06-FILES.md), [C06-CONSOLE-SCHEMA](C06-CONSOLE-SCHEMA.md), [C06-CHECKLIST](C06-CHECKLIST.md). Nội dung bên dưới là phạm vi đã duyệt.

Người dùng xác nhận đã duyệt tất cả bản hiện tại, gồm sửa sau pull và Hồ sơ thống nhất. C04/C05 chốt theo xác nhận của người dùng, không phải kiểm thử độc lập của Codex. Các ca khó chưa tái hiện/Rules/thiết bị và C02-F01 tiếp tục theo dõi theo báo cáo trước; không tự bổ sung kết quả giả.

## Phạm vi C06

Căn cứ README mục 22.2: **Nháp tin tuyển và xem trước — lưu nháp cục bộ, sửa, kiểm tra dữ liệu**. C07 mới gửi PENDING_REVIEW và Admin duyệt tin. C06 không tạo JobPost trên Firestore, không công khai tin, không mở ứng tuyển/ca làm và không dùng trạng thái server DRAFT.

Luồng hoàn chỉnh: người thuê → Tin của tôi → danh sách nháp trên máy → tạo/sửa → Lưu nháp hoặc Xem trước → quay lại sửa/lưu; mở lại app vẫn có nháp đã lưu. Có xóa nháp với xác nhận. Người tìm việc/Admin không có thao tác soạn tin tuyển.

**Chính sách đã được người dùng chốt:** nháp đã lưu được giữ khi đăng xuất, chỉ đúng UID đăng nhập lại mới thấy; tài khoản khác không thấy. Đây là dữ liệu người dùng chủ động soạn, tách khỏi cache theo tài khoản phải dọn lúc đăng xuất. Chưa đồng bộ nháp qua thiết bị/Firebase. Xóa dữ liệu ứng dụng/gỡ app vẫn có thể mất nháp trên máy.

## Các bước triển khai

1. **Chốt dữ liệu và điều kiện nháp.** Tách model LocalJobDraft khỏi JobPost server. Nháp có ID ổn định, ownerUid, schemaVersion/revision và thời điểm lưu cục bộ; nội dung theo JobPost v3 nhưng không có reviewer/quyền duyệt/status công khai. Lưu nháp đang soạn dở được; Xem trước yêu cầu đủ dữ liệu hợp lệ.
2. **Lưu SQLite theo tài khoản.** Dùng SQLiteOpenHelper có sẵn của Android, thao tác ngoài luồng giao diện; truy vấn/sửa/xóa luôn kèm ownerUid và ID, câu lệnh tham số hóa. Hỗ trợ nhiều nháp, thứ tự theo lần lưu mới nhất, migration không xóa nháp. Bấm Lưu lặp không sinh hai ID; chưa lưu chỉ giữ trong ViewModel khi xoay. Đăng xuất dọn dữ liệu RAM/giao diện nhưng giữ nháp đã lưu cho đúng UID. Loại DB nháp/cache khỏi backup/chuyển dữ liệu tự động để giữ nghĩa nháp trên máy.
3. **Danh mục việc/khu vực có cache.** Tái dùng areas DEMO hiện có; bổ sung jobCategories với ID ổn định, name/iconKey/active và seed rõ ràng. C06 chỉ thêm quyền đọc danh mục phù hợp; client không sửa danh mục hoặc ghi jobs. Đọc server khi online, lưu CachedCategory/CachedArea với cachedAt; offline dùng bản đã lưu có nhãn phù hợp. Chưa từng tải danh mục thì báo cần mạng để tải lần đầu, không bịa danh sách khi bị lỗi quyền.
4. **Màn Tin của tôi/danh sách nháp.** Mở tab Tin của tôi và thẻ Soạn tin tuyển đúng vai trò EMPLOYER; danh sách thật từ SQLite, trạng thái trống/lỗi, tạo/sửa/xóa. Không dựng số tin đã đăng/đã duyệt giả. Giữ topbar logo + Người thuê và điều hướng đã duyệt; cập nhật các đường điều hướng Main/C05 liên quan để không có tab bấm mà không đi đâu.
5. **Form soạn tin có phân vùng.** Các vùng ngắn: thông tin việc; khu vực/lịch; yêu cầu; tiền công/hạn nhận. Có Lưu nháp và Xem trước, lỗi sát trường. Cỡ chữ/nút/khoảng cách theo bộ rule đã duyệt, nội dung cuộn được khi chữ lớn/bàn phím. Back/đổi trang khi chưa lưu có lựa chọn lưu/bỏ/tiếp tục sửa; không tự gửi hoặc ghi từ hàm render.
6. **Xem trước và quay lại sửa.** Hiện đúng nội dung đang soạn với nhãn Nháp/Chưa gửi duyệt; lịch, yêu cầu, tiền VND và đơn vị dễ đọc. Trường thiếu/sai đưa về đúng vùng, giữ nội dung. Quay lại sửa không mất dữ liệu và không tạo bản nháp khác. Chưa mở nút Gửi duyệt của C07 hoặc giả báo đã đăng.
7. **Tích hợp vòng đời/offline và bàn giao.** Đọc/lưu lỗi có phản hồi, không báo đã lưu trước khi SQLite hoàn tất; callback theo UID/request/revision. Kiểm tra đường quay lại, xoay, đổi UID, đăng xuất/đăng nhập lại. Khi Main không đọc được role server lúc mở app offline, cần đường đọc/sửa các nháp đã lưu của UID Auth hiện tại, không dùng dữ liệu local để cấp quyền gửi; chưa từng xác nhận vai trò người thuê thì không mở soạn tin mới chỉ bằng dữ liệu tự khai. Sau code: rà tĩnh, lập C06-FILES/CHECKLIST/CONSOLE-SCHEMA và giao người dùng Sync/Run/nghiệm thu/tự commit.

## Nội dung và kiểm tra form

| Nhóm | Nội dung |
|---|---|
| Công việc | Tiêu đề, danh mục, mô tả nhiệm vụ, yêu cầu/nhãn kỹ năng |
| Khu vực và lịch | areaCode, ngày trong tuần, workShift, mô tả lịch |
| Thể lực | physicalDemands và demandsDeclared; phân biệt chưa khai với chủ động chọn không có yêu cầu trong danh sách |
| Tiền công | payAmount số nguyên VND dương khi xem trước, payUnit HOUR/SESSION/DAY; không dùng số thực cho tiền |
| Hạn nhận | expiresAt được chọn rõ; so sánh giờ máy chỉ để kiểm tra form, C07 sẽ kiểm hạn thật bằng server |

WorkShift giữ MORNING/AFTERNOON/EVENING/FLEXIBLE. PhysicalDemand giữ HEAVY_LIFTING/PROLONGED_STANDING/FREQUENT_MOVEMENT theo README; nhãn tiếng Việt ngắn. Không suy đoán tuổi/giới tính/sức khỏe để tạo điều kiện tuyển trái phạm vi. Nháp không chứa giấy tờ/QR/OTP/token/snapshot đơn hoặc địa chỉ riêng của ca.

Cho phép thiếu trường hoặc dữ liệu đang gõ dở khi Lưu nháp; cần có nội dung để tránh bản rỗng vô tình. Validation Xem trước kiểm đủ danh mục/khu vực, lịch và khai yêu cầu, mô tả/tiêu đề, tiền hợp lệ và hạn; giới hạn độ dài/số lượng dùng chung một validator để C07 có thể tái dùng. Sau tải lại danh mục, ID đã inactive/không còn hợp lệ phải được chọn lại, không đổi ID âm thầm.

## Điều kiện tài khoản và C07

- C06 cho EMPLOYER đang hoạt động soạn nháp dù chưa OTP/chưa VERIFIED; không yêu cầu giấy nghỉ hưu hoặc hoàn tất C05 mới được gõ nháp.
- Gửi tin ở C07 mới cần OTP + hồ sơ nghề nghiệp đủ + mạng và Rules kiểm lại; danh tính mẫu VERIFIED là điều kiện trước thỏa thuận/ca theo v3, không tự áp thêm làm điều kiện soạn nháp.
- Khi offline, quyền đọc/sửa nháp đã lưu chỉ theo UID Auth hiện tại và dữ liệu local của UID đó. Không xem cache role hay bản xem trước là quyền ghi Firebase. Đổi tài khoản không nhận form/kết quả lưu của UID trước.
- Danh mục cache dùng để hỗ trợ soạn/xem trước, C07 phải kiểm lại active và dữ liệu server trước gửi.

## Chuẩn bị Firebase và giao diện

Lúc triển khai C06 sẽ có file seed jobCategories và hướng dẫn Publish Firestore Rules chỉ bổ sung đọc danh mục. Giữ nguyên users/profiles/C05/Storage Rules đang duyệt; không xóa dữ liệu thử cũ, không đổi JSON/SHA/Auth/provider hoặc thêm Functions. Chưa thực hiện bất kỳ thay đổi Console/Rules nào trong bước chuẩn bị này.

Dùng hệ màu/font/logo/topbar/thẻ và nút đã duyệt. Nếu người dùng cung cấp ảnh màn soạn tin/xem trước thì dùng làm tham khảo và ưu tiên luồng/schema v3; chưa coi ảnh Figma là cam kết có mọi chức năng trong C06. TTS/trợ giúp vẫn thuộc C22.

File dự kiến theo nhóm: JobDraft model/validator; SQLite helper + repository + cache danh mục; ViewModel; màn danh sách/form/xem trước Java/XML; strings/styles; tích hợp RoleNavigationBar/RoleHomePanel/Main và đường trở về C05; backup exclusions; seed/Rules đọc danh mục; tài liệu bàn giao. Không chuyển mã Gradle module app vào file gốc hoặc thêm thư viện không cần thiết.

## Người dùng nghiệm thu sau khi triển khai

- [ ] Tạo nhiều nháp; lưu thiếu trường được, xem trước thiếu/sai bị chặn đúng trường.
- [ ] Sửa/lưu/xem trước/quay lại/xóa đúng bản; bấm lặp không sinh bản trùng.
- [ ] Đóng/mở app vẫn còn nháp đã lưu; xoay giữ nội dung đang sửa, không tự lưu/gửi.
- [ ] Đăng xuất A/đăng nhập B không thấy nháp A; đăng nhập lại A thấy đúng nháp A đã lưu.
- [ ] Soạn/sửa/lưu nháp offline với danh mục đã tải; cold start offline có đường vào nháp đã lưu của đúng UID. Không có cache thì trạng thái trống/lỗi rõ, không giả tải thành công.
- [ ] Danh mục cập nhật/inactive, lỗi SQLite hoặc đổi UID giữa callback không làm hỏng/ghi nhầm nháp.
- [ ] Chữ 200%, ngang/hẹp/TalkBack/bàn phím dùng được; hồi quy C01–C05 và Admin vẫn hoạt động.
- [ ] Không có JobPost/PENDING_REVIEW hoặc thông báo đã đăng được tạo trên Firebase chỉ từ lưu/xem trước.

Người dùng đã yêu cầu triển khai sau khi duyệt; code và tài liệu bàn giao đã được bổ sung theo phạm vi này. Codex chỉ rà tĩnh, không tự chạy build/test/lint/Sync/emulator/app, stage/commit/push/deploy hoặc sửa Firebase Console. Các checkbox dự kiến ở trên chưa tự được coi đạt; người dùng nghiệm thu theo C06-CHECKLIST.
