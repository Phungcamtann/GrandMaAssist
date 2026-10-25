# C06 — xác nhận nghiệm thu và đối chiếu địa chỉ

25/10/2026: người dùng xác nhận đã cập nhật cấu hình, kiểm tra ở các vai trò, mọi thứ hoạt động tốt và duyệt C06. Đây là kết quả người dùng báo, không phải Codex chạy build/test. Các nhánh kỹ thuật khó, backup/chuyển thiết bị, migration, callback trễ hoặc Rules âm tính chưa có kết quả cụ thể vẫn theo dõi theo C06-CHECKLIST; không tự đánh dấu đã đạt từng ca.

## Địa chỉ chính xác

**Cập nhật sau nghiệm thu:** người dùng đã chốt nhập địa chỉ làm việc khi soạn tin và công khai trên tin OPEN trong phạm vi bổ sung C07. Xem [C07-ADDRESS-CHANGE](C07-ADDRESS-CHANGE.md). Phân tích bên dưới giữ làm lịch sử v3/C06, không còn là quyết định trì hoãn toàn bộ địa chỉ đến C15. C06 đã commit không bị thay đổi kết quả nghiệm thu bởi yêu cầu mới.

Người dùng nêu cần xem lại địa chỉ chính xác cho người thuê. Đối chiếu README v3:

- EmployerProfile và JobPost dùng areaCode; SearchPreference của người tìm việc dùng areaCodes.
- AgreementSnapshot có privateAddress, cùng nhiệm vụ, thời gian và tiền công được hai bên xác nhận cho từng ca.
- Ma trận quyền chỉ cho các bên của ca và Admin theo vụ xử lý xem địa chỉ riêng. Không đặt dữ liệu private chung tài liệu public rồi chỉ ẩn trên giao diện.
- Cache SQLite không lưu địa chỉ riêng của ca; LocalJobDraft C06 hiện không chứa privateAddress.

Do đó chưa bổ sung địa chỉ chính xác vào tin tuyển công khai hoặc SQLite C06. Khi triển khai thỏa thuận ca/S15, phải có trường nhập địa chỉ làm việc chính xác cho người thuê, kiểm tra bắt buộc trước khi gửi thỏa thuận, hiển thị cho đúng người làm của ca và giữ trong phiên bản thỏa thuận được xác nhận. Không tự lấy địa chỉ thường trú từ QR căn cước, không dùng khu vực DEMO làm địa chỉ chính xác.

Nếu muốn nhập sẵn địa chỉ ngay lúc soạn tin hoặc lưu địa chỉ mặc định ở hồ sơ người thuê, đây là thay đổi so với schema v3 hiện hành: cần chốt riêng nơi lưu private, quyền đọc và thời điểm chia sẻ trước khi bổ sung. Chưa thay đổi mã nguồn, schema hoặc Rules cho phương án này.
