# C07 — nghiệm thu và kiểm tra commit

26/10/2026: người dùng xác nhận lỗi gửi tin đã được khắc phục, ứng dụng chạy bình thường, các luồng hoạt động khác đều được duyệt và đã commit bản mới nhất. Đây là kết quả kiểm thử do người dùng cung cấp; Codex không chạy lại ứng dụng hoặc bộ kiểm thử trong lần kiểm tra commit này.

## Mốc bàn giao

- Commit `08d221198a0fd336d5730933dd95c294f41e86c6` — `Fix luồng Duyệt tin`.
- Đã fetch `origin`, đối chiếu HEAD local và `origin/main`: cùng commit trên. Working tree sạch trước khi cập nhật tài liệu nghiệm thu này.
- Commit chứa đủ bản sửa C05 cho người thuê, chẩn đoán C07, Rules r4, báo cáo nguyên nhân, kết quả 24 ca Rules cục bộ và hai script tái hiện. Không thiếu các file của lần bàn giao trước.
- Bằng chứng Emulator 24/24 giữ tại [kết quả Rules](C07-RULES-REGRESSION-RESULTS.json); nghiệm thu ứng dụng thực tế là xác nhận mới của người dùng. Hai nguồn kết quả được ghi riêng.

Trạng thái **C07 đã được người dùng nghiệm thu** thay thế các câu “chờ Publish/nghiệm thu” trong lịch sử điều tra. Không suy diễn thiết bị/API hoặc đánh dấu từng ca kỹ thuật đã tái hiện khi chưa có kết quả riêng. [Checklist C07](C07-CHECKLIST.md) tiếp tục làm danh mục tra cứu và theo dõi ca bổ sung.

## Có cần dọn dự án trước C08 không?

Không có hạng mục dọn mã bắt buộc được phát hiện trong phạm vi đối chiếu commit, diff bàn giao, Rules, tài liệu và các file Git quản lý:

- `.gradle/`, `build/`, `app/build/`, `local.properties`, Python `__pycache__/` đang được bỏ qua bởi Git. Không cần xóa cache hoặc dữ liệu ứng dụng; giữ nguyên nháp SQLite và dữ liệu người dùng.
- Giữ `gradle-wrapper.jar` vì thuộc Gradle Wrapper. Không tự xóa cấu hình `.idea` đã được nhóm commit.
- Giữ script Rules và kết quả giả lập để tra cứu lỗi vượt 1.000 biểu thức. Chúng không tự chạy cùng Gradle/app và không chứa dữ liệu tài khoản thật.
- Giữ chẩn đoán C07 hiện tại: log chỉ bật ở bản debug và ghi mã bước/mã lỗi; các lượt đọc chẩn đoán chỉ chạy sau lỗi từ chối ghi, endpoint chỉ đọc của chính chủ. Không gỡ riêng Rules chẩn đoán khi APK còn sử dụng chúng.
- Trước bản phát hành cho người dùng cuối, nên rút gọn thông báo lỗi kỹ thuật trên UI và xem xét gỡ đồng bộ mã/endpoint chẩn đoán nếu không còn cần. Việc đó phải là thay đổi riêng có review và nghiệm thu, không trộn vào lần chốt C07.

Lần này chỉ cập nhật tài liệu trạng thái, không sửa Java/XML/Rules/Gradle, không build/test, không deploy hoặc stage/commit/push.

## Bước tiếp theo và các mục theo dõi

C08 theo kế hoạch v3: xem/lọc/phân trang/chi tiết việc OPEN cho khách/người tìm việc và cache SQLite để mở lại tin đã xem khi mất mạng. Địa chỉ làm việc công khai `workAddress` đã chốt ở C07 phải được hiển thị trong chi tiết và xử lý cùng dữ liệu cache. Chưa triển khai C08 trong lần kiểm tra commit này; C09 hướng dẫn tìm việc, C10 lưu việc và C11 sửa/đóng/ẩn tin giữ phạm vi riêng.

Các ca timeout/callback trễ/cạnh tranh/biến thể thiết bị hoặc TalkBack chưa có bằng chứng riêng vẫn theo dõi tại checklist tương ứng. C02-F01 về tiếng Việt/tên “Your Firebase App” trong SMS vẫn mở; việc nghiệm thu C07 không tự giải quyết mục này.
