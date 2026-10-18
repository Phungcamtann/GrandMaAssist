## Bản phân tích và thiết kế hệ thống

**Ngày cập nhật:** 18/10/2026  
**Loại tài liệu:** Đặc tả phân tích và thiết kế cho đồ án phát triển ứng dụng mobile.  
**Phạm vi:** Android Java · Firebase · người nghỉ hưu · tìm việc có hướng dẫn · OTP thật · thanh toán mô phỏng theo ca.  
**Trạng thái:** Đang thực hiện
**Nhóm thực hiện:** Nhóm 4

- Phùng Cẩm Tân (leader)
- Nguyễn Duy Thành Tài
- Dương Hoàng Sâm
- Phạm Thiên Long

## 1. Tóm tắt hệ thống và quyết định đã chốt

GrandMa Assist kết nối người nghỉ hưu muốn làm việc bán thời gian với cá nhân, gia đình và doanh nghiệp nhỏ cần thuê người làm. Hệ thống hướng dẫn người tìm việc lựa chọn từng bước, hỗ trợ chữ lớn và giọng đọc; quản lý từ tin tuyển dụng đến đơn ứng tuyển, thỏa thuận từng ca, kết quả làm việc và phân bổ tiền mô phỏng.

| Nội dung               | Quyết định của bản thiết kế                                                                                                                                               |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Nền tảng               | Android Studio; ứng dụng Android viết bằng Java, giao diện XML Views                                                                                                      |
| SDK hiện có            | minSdk 29; compileSdk 36.1; targetSdk 36; mã ứng dụng Java, cấu hình Gradle Kotlin DSL không có nghĩa ứng dụng viết Kotlin                                                |
| Dịch vụ và dữ liệu     | Firebase Authentication, Cloud Firestore, Firebase Storage; Security Rules là nơi kiểm soát quyền và thay đổi dữ liệu                                                     |
| Đăng nhập              | Số điện thoại và Google; liên kết phương thức về cùng UID khi người dùng sử dụng cả hai                                                                                   |
| Xác minh số điện thoại | Firebase OTP SMS thật khi nghiệm thu; số kiểm thử trong phát triển                                                                                                        |
| Hồ sơ người tìm việc   | Chụp căn cước → đọc QR → đính kèm chứng nhận nghỉ hưu/BHXH mô phỏng → Admin duyệt trước ứng tuyển                                                                         |
| Tìm việc               | Hướng dẫn từng bước; gợi ý theo quy tắc và giải thích lý do phù hợp                                                                                                       |
| Giao diện              | Chữ lớn, ít lựa chọn chính, xanh lá đậm làm màu nhấn; chữ và nền phải đủ tương phản                                                                                       |
| Âm thanh               | Hướng dẫn bằng giọng đọc, có bật/tắt, dừng và nghe lại; không điều khiển bằng giọng nói                                                                                   |
| Thiết bị               | Điện thoại và tablet, màn hình dọc/ngang, vùng ứng dụng thay đổi kích thước                                                                                               |
| Ngoại tuyến            | SQLite lưu nội dung việc đã xem và lựa chọn tìm việc; không quyết toán tiền hay xác minh khi offline                                                                      |
| Đơn vị giao dịch       | Mỗi ca/buổi có thời gian, nhiệm vụ, địa điểm và tiền công riêng                                                                                                           |
| Thanh toán             | Mô phỏng cấp tiền trước ca, giữ tiền, chi trả và hoàn tiền                                                                                                                |
| Phí duy trì            | Người thuê trả thêm 10% trên tiền công được công nhận; người làm không bị trừ phí nền tảng                                                                                |
| Quản trị               | Trong cùng ứng dụng: duyệt tin, hồ sơ nghỉ hưu/hồ sơ người thuê, báo cáo, tranh chấp và thống kê phí                                                                      |
| Ngoài phạm vi          | Chat, tiền thật, eKYC/VNeID/tra cứu BHXH thật, AI gợi ý việc, GPS tính khoảng cách, quảng cáo, hội viên, đánh giá sao; chưa tự thêm OCR, đọc chip hay nhận diện khuôn mặt |

**Giới hạn xác minh:** OTP thật xác minh khả năng nhận mã ở số liên hệ. QR trích xuất thông tin và Admin xét hồ sơ theo quy trình đồ án, không chứng minh chính chủ với cơ quan quản lý căn cước/BHXH. Kết quả hiển thị “Hồ sơ nghỉ hưu đã được duyệt”; giải thích rõ tra cứu mô phỏng trong phần mô tả quy trình, không đóng watermark lên ảnh giấy tờ.

## 2. Bài toán, mục tiêu và đối tượng

### 2.1. Vấn đề cần giải quyết

- Quá nhiều bộ lọc, biểu mẫu và thông tin có thể gây khó khăn cho người ít quen công nghệ.
- Người tìm việc cần biết công việc có phù hợp khu vực, thời gian và khả năng thực hiện của mình hay không.
- Người thuê cần thông tin ứng viên và cách ghi nhận thỏa thuận rõ ràng.
- Việc chỉ trao đổi số điện thoại không tạo được lịch sử ca làm, kết quả hoặc căn cứ phân bổ tiền.
- Hệ thống cần mô hình thu phí và theo dõi chi phí để giải thích khả năng duy trì.

### 2.2. Mục tiêu

1. Người dùng có thể tìm được danh sách việc phù hợp qua một chuỗi câu hỏi ngắn.
2. Mỗi thao tác chính có chỉ dẫn rõ, dùng được khi tăng cỡ chữ hoặc xoay thiết bị.
3. Tin tuyển dụng được kiểm duyệt; số liên hệ được xác minh; trạng thái xác minh không gây hiểu lầm.
4. Hai bên xác nhận cùng một phiên bản thỏa thuận trước khi ca được cấp tiền.
5. Mỗi khoản tiền mô phỏng có lịch sử, không bị ghi nhận hai lần và có thể đối chiếu tổng phân bổ.
6. Xem lại nội dung đã tải khi mất mạng.

### 2.3. Đối tượng và giới hạn

| Đối tượng                                                               | Nhu cầu thiết kế                                                                                       |
| ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| Người nghỉ hưu tìm việc, ưu tiên trải nghiệm dễ dùng cho người lớn tuổi | Hạn chế gõ dài; chữ dễ đọc; lựa chọn từng bước; công việc phù hợp và tiền công dễ hiểu                 |
| Cá nhân/gia đình tuyển dụng                                             | Đăng tin, chọn ứng viên, lập thỏa thuận và theo dõi từng ca                                            |
| Doanh nghiệp nhỏ                                                        | Tương tự người thuê cá nhân; thông tin đơn vị tự khai báo, chưa xác minh tư cách đại diện doanh nghiệp |
| Admin                                                                   | Hàng đợi việc cần xử lý, lý do quyết định, lịch sử nghiệp vụ                                           |

Không đặt ngưỡng tuổi 55/60 và không tự suy ra nghỉ hưu từ ngày sinh. Được xem việc không đồng nghĩa được ứng tuyển: mọi SEEKER phải qua cùng quy trình duyệt hồ sơ nghỉ hưu. Ngày sinh hỗ trợ hồ sơ; tuổi chỉ là thông tin, không phải nhánh tự mở quyền. Không mặc định mọi người lớn tuổi đều ít kỹ năng công nghệ: luôn có đường truy cập nhanh cho người đã quen. Các giả định trải nghiệm cần được kiểm chứng bằng khảo sát và thử nghiệm, không ghi trong báo cáo như dữ liệu nghiên cứu đã thu thập.

## 3. Thuật ngữ và ranh giới hệ thống

| Thuật ngữ                      | Định nghĩa                                                                                                                |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------- |
| Tin tuyển dụng — JobPost       | Nhu cầu tìm người làm; có thể nhận nhiều đơn                                                                              |
| Đơn ứng tuyển — Application    | Đề nghị của một người tìm việc đối với một tin                                                                            |
| Đơn được chấp nhận             | Hai bên được kết nối; chưa có nghĩa đã chốt ca hoặc đã trả tiền                                                           |
| Ca làm — WorkSession           | Một lần thực hiện cụ thể; chỉ có một người làm và một người thuê                                                          |
| Thỏa thuận — AgreementSnapshot | Phiên bản nhiệm vụ, địa điểm, lịch, tiền công và chính sách đã xác nhận cho một ca                                        |
| Cấp tiền — Funding             | Ghi nhận thanh toán mô phỏng đủ tiền công cộng phí dự kiến                                                                |
| Giữ tiền — Held amount         | Phần tiền mô phỏng chưa được phân bổ; chưa là doanh thu                                                                   |
| Quyết toán — Settlement        | Chốt phần trả người làm, phí nền tảng và hoàn người thuê                                                                  |
| Báo cáo tin — Report           | Phản ánh nội dung tin, không phải tranh chấp tiền của ca                                                                  |
| Tranh chấp — Dispute           | Bất đồng về một ca làm, hủy ca hoặc phân bổ tiền                                                                          |
| Xác minh điện thoại            | Xác nhận nhận được OTP hoặc cơ chế xác minh hợp lệ của Firebase cho số liên hệ                                            |
| Duyệt hồ sơ nghỉ hưu           | Admin xét ảnh căn cước/thông tin QR và bằng chứng nghỉ hưu hoặc BHXH mô phỏng; quyết định điều kiện ứng tuyển trong đồ án |
| Tra cứu mô phỏng               | Admin nhập kết quả đối chiếu theo kịch bản của nhóm; không truy vấn cơ sở dữ liệu nhà nước                                |
| Danh tính mẫu người thuê       | Giữ quy trình v2 trước thỏa thuận/ca; tách biệt với hồ sơ nghỉ hưu của SEEKER                                             |

**Hệ thống ngoài:** Firebase Authentication và Google hỗ trợ đăng nhập; ML Kit hỗ trợ đọc QR từ ảnh trên Android; Firebase Storage giữ ảnh riêng; Android TTS cung cấp giọng đọc; trình quay số hỗ trợ gọi điện. Cổng thanh toán và nhà cung cấp eKYC thật không nằm trong bản đầu.
