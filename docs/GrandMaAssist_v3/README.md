# GrandMa Assist — Bản thiết kế v3.0 trình duyệt

Ngày: 18/10/2026. Trạng thái: CHỜ DUYỆT TRƯỚC KHI TRIỂN KHAI.

1. Mở GrandMaAssist_PhanTichThietKe_v3.html trong trình duyệt để đọc. HTML có mục lục, 7 hình SVG nhúng sẵn và các liên kết mở hình lớn. Không cần tải thư viện từ mạng.
2. Đọc mục 0 trước để xem phần thay đổi; xem tiếp mục 4 (quyền), UC04–UC05 và UC09 (luồng), 10.4 (trạng thái nghỉ hưu), 16 (màn hình), 18 (tiêu chí kiểm thử), 22 (thực hiện sau duyệt).
3. GrandMaAssist_PhanTichThietKe_v3.md là bản nguồn cùng nội dung.
4. diagrams/ chứa hình SVG, nguồn DOT, các khối Mermaid và UML use case. Bảng lớp trong tài liệu là từ điển dữ liệu đầy đủ; hình lớp là góc nhìn tóm tắt.

Thay đổi chính: chụp CCCD/căn cước → đọc QR từ ảnh → tải bằng chứng nghỉ hưu hoặc ảnh BHXH mô phỏng → Admin tra cứu mô phỏng và duyệt → thông báo kết quả. Phải APPROVED trước ứng tuyển. OTP SMS thật vẫn giữ; không có nhánh tự duyệt theo tuổi/HT, không bắt đóng nhãn lên ảnh giấy tờ.

Bản này thay thế các điểm đang chờ chốt trong tài liệu đối chiếu Figma trước đó. V2 gốc và Figma được giữ nguyên. Không có code ứng dụng, thay đổi Firebase hoặc commit chức năng trong lần bàn giao thiết kế này.

Các ca kiểm thử trong mục 18 là kế hoạch cần chạy khi code, không phải kết quả kiểm thử ứng dụng. Việc kiểm tra bản bàn giao chỉ xác nhận cấu trúc tài liệu, liên kết và hiển thị HTML.

Giải nén toàn bộ gói để các liên kết mở hình lớn và nguồn hoạt động. HTML có thể đọc riêng vì các hình chính được nhúng sẵn.
