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

## 4. Tác nhân và phân quyền nghiệp vụ

### 4.1. Tác nhân

- **Khách:** xem việc, sử dụng luồng gợi ý, xem dữ liệu công khai.
- **Người tìm việc — SEEKER:** quản lý hồ sơ, ứng tuyển, xác nhận ca, báo hoàn thành, xem tiền công và gửi tranh chấp.
- **Người thuê — EMPLOYER:** đăng tin, chọn ứng viên, đề xuất ca, cấp tiền mô phỏng, xác nhận kết quả.
- **Admin — ADMIN:** kiểm duyệt và xử lý nghiệp vụ được giao; không tự động có quyền sửa mọi dữ liệu.
- **Firebase:** Authentication xác thực phiên; Firestore/Storage Rules kiểm soát quyền; giao dịch ghi dữ liệu được app khởi tạo. Không có tài khoản dịch vụ toàn quyền trong APK, không có tiến trình backend tự viết trong phạm vi này.

Mỗi tài khoản thông thường có một vai trò cố định trong bản đầu. Admin được cấp riêng; người dùng không thể tự chọn admin.

### 4.2. Quyền theo mức xác minh

| Mức                                               | Người tìm việc                                                                                    | Người thuê                                           |
| ------------------------------------------------- | ------------------------------------------------------------------------------------------------- | ---------------------------------------------------- |
| Khách                                             | Xem việc, tìm việc theo hướng dẫn và xem nội dung đã cache                                        | Xem nội dung công khai                               |
| Đăng nhập, chưa xác minh số                       | Điền hồ sơ, chụp/đọc QR và chuẩn bị ảnh; lưu lựa chọn tìm việc. Cần OTP trước khi gửi hồ sơ duyệt | Điền hồ sơ, soạn nháp cục bộ                         |
| OTP đã xác minh, hồ sơ đủ, nghỉ hưu chưa APPROVED | Gửi/bổ sung hồ sơ; xem/tìm/lưu việc, báo cáo tin theo quyền v2; **không gửi đơn mới**             | Gửi tin duyệt, xử lý đơn theo điều kiện v2           |
| OTP + hồ sơ đủ + nghỉ hưu APPROVED đúng phiên bản | Ứng tuyển; rút đơn đang chờ; đủ điều kiện hồ sơ để chốt ca nếu các điều kiện khác hợp lệ          | Không áp dụng điều kiện nghỉ hưu                     |
| Hồ sơ danh tính mẫu người thuê VERIFIED           | Không phát sinh một vòng duyệt danh tính mẫu thứ hai cho SEEKER                                   | Lập/xác nhận ca, cấp tiền mô phỏng theo điều kiện v2 |

**Điều kiện gửi đơn mới:** role SEEKER, accountStatus ACTIVE, số liên hệ khớp Firebase Auth, hồ sơ nghề nghiệp đủ, retirementStatus APPROVED gắn đúng request và phiên bản định danh hiện tại, tin còn nhận đơn. Firebase Rules kiểm tra độc lập với UI.

NOT_SUBMITTED, SUBMITTED, NEEDS_INFO, REJECTED, REVERIFY_REQUIRED đều không được ứng tuyển. Hiển thị lý do và đường đến hồ sơ; không khóa toàn ứng dụng. Khi được duyệt, quay về tin đã quan tâm để người dùng chủ động xác nhận; không tự gửi đơn.

Giới hạn quyền giao dịch mới không che lịch sử, chặn rút đơn hợp lệ, chặn báo vấn đề hoặc làm mất tiền/ca đang xử lý. Thay đổi thông tin định danh cốt lõi làm phiên bản đã duyệt không còn hiệu lực cho đơn/ca mới; các ca cũ tiếp tục có đường xử lý. Sửa kỹ năng hoặc sở thích không tự buộc duyệt nghỉ hưu lại.

## 5. Yêu cầu chức năng và sơ đồ phân rã

### 5.1. Danh sách chức năng

| Mã  | Nhóm        | Yêu cầu                                                                                                                         |
| --- | ----------- | ------------------------------------------------------------------------------------------------------------------------------- |
| F01 | Tài khoản   | Đăng nhập bằng số điện thoại/Google, đăng xuất                                                                                  |
| F02 | Tài khoản   | Xác minh số liên hệ bằng OTP thật; liên kết phương thức đăng nhập                                                               |
| F03 | Tài khoản   | Chọn vai trò lần đầu, cập nhật hồ sơ theo vai trò                                                                               |
| F04 | Hồ sơ       | SEEKER chụp căn cước, đọc QR, đính kèm chứng nhận nghỉ hưu/BHXH mô phỏng, gửi/bổ sung và xem tiến độ; EMPLOYER giữ hồ sơ mẫu v2 |
| F05 | Xét duyệt   | Admin tra cứu mô phỏng, duyệt/từ chối/yêu cầu bổ sung, ghi lịch sử và thông báo kết quả                                         |
| F06 | Tìm việc    | Hỏi từng bước về loại việc, khu vực, thời gian và yêu cầu muốn tránh                                                            |
| F07 | Tìm việc    | Lưu/sửa lựa chọn, gợi ý theo quy tắc, giải thích lý do phù hợp                                                                  |
| F08 | Tìm việc    | Xem tất cả, lọc, phân trang, xem chi tiết                                                                                       |
| F09 | Tìm việc    | Lưu/bỏ lưu việc quan tâm                                                                                                        |
| F10 | Ứng tuyển   | Chỉ hồ sơ nghỉ hưu APPROVED mới gửi đơn; snapshot hồ sơ/tin, đồng ý chia sẻ liên hệ và không gửi trùng                          |
| F11 | Ứng tuyển   | Xem trạng thái, rút đơn đang chờ                                                                                                |
| F12 | Tuyển dụng  | Tạo/sửa/đóng tin và gửi kiểm duyệt                                                                                              |
| F13 | Tuyển dụng  | Xem ứng viên, chấp nhận/từ chối đơn                                                                                             |
| F14 | Kiểm duyệt  | Duyệt/từ chối/ẩn tin, có lý do theo quy tắc                                                                                     |
| F15 | Báo cáo     | Gửi, xem và xử lý báo cáo tin                                                                                                   |
| F16 | Ca làm      | Đề xuất thỏa thuận, xem trước, sửa trước khi khóa                                                                               |
| F17 | Ca làm      | Hai bên xác nhận cùng phiên bản thỏa thuận                                                                                      |
| F18 | Ca làm      | Ghi nhận bắt đầu, báo hoàn thành và xác nhận kết quả                                                                            |
| F19 | Ca làm      | Yêu cầu hủy, đồng ý hủy hoặc chuyển tranh chấp                                                                                  |
| F20 | Thanh toán  | Tính phí 10% từ người thuê và hiển thị tổng trước xác nhận                                                                      |
| F21 | Thanh toán  | Cấp/giữ tiền mô phỏng; theo dõi kết quả giao dịch                                                                               |
| F22 | Thanh toán  | Chi trả, hoàn tiền, ghi nhận phí và lịch sử mô phỏng                                                                            |
| F23 | Tranh chấp  | Gửi vấn đề, cung cấp giải trình, admin chốt phân bổ                                                                             |
| F24 | Liên hệ     | Mở trình quay số từ thông tin được phép xem                                                                                     |
| F25 | Tiếp cận    | Giọng đọc theo yêu cầu, bật/tắt, dừng, nghe lại                                                                                 |
| F26 | Ngoại tuyến | Lưu/xem lại tin đã tải; lưu câu trả lời hướng dẫn                                                                               |
| F27 | Quản trị    | Hàng đợi quá hạn, hồ sơ, báo cáo, tranh chấp                                                                                    |
| F28 | Thống kê    | Tổng tiền cấp, tiền đang giữ, đã chi/hoàn, doanh thu phí mô phỏng                                                               |

### 5.2. Sơ đồ phân rã chức năng

<!-- diagram:functions -->

```mermaid
flowchart TB
    app["GrandMa Assist"]
    app --> account["Tài khoản và xác minh"]
    app --> search["Tìm việc có hướng dẫn"]
    app --> recruitment["Tin và ứng tuyển"]
    app --> session["Thỏa thuận và ca làm"]
    app --> payment["Thanh toán và phí mô phỏng"]
    app --> admin["Quản trị"]
    app --> support["Hỗ trợ sử dụng"]
    account --> accountDetail["Đăng nhập; OTP; QR; duyệt hồ sơ"]
    search --> searchDetail["Câu hỏi; lưu lựa chọn; gợi ý; lọc; lưu việc"]
    recruitment --> recruitmentDetail["Đăng và duyệt tin; gửi và xử lý đơn"]
    session --> sessionDetail["Chốt phiên bản; bắt đầu; hoàn thành; hủy"]
    payment --> paymentDetail["Cấp tiền; giữ tiền; chi trả; hoàn; phí 10%"]
    admin --> adminDetail["Báo cáo; tranh chấp; lịch sử; thống kê"]
    support --> supportDetail["Giọng đọc; gọi điện; xem offline"]
```

Responsive, bảo mật và khả năng đọc là yêu cầu phi chức năng, không phải các nghiệp vụ độc lập trong sơ đồ use case.

## 6. Thiết kế trải nghiệm cho người lớn tuổi

### 6.1. Trang chính người tìm việc

Thứ tự ưu tiên: (1) việc cần xử lý ngay nếu có; (2) nút lớn **Giúp tôi tìm việc**; (3) **Việc của tôi**; (4) **Nghe hướng dẫn/Trợ giúp**. Khi chưa có ca hoặc yêu cầu xác nhận, nút tìm việc là nội dung nổi bật nhất.

Mục **Thêm** chứa hồ sơ, việc đã lưu và cài đặt. Không giấu ca sắp tới, tiền công đang xử lý hoặc hỗ trợ vào menu khó phát hiện. **Việc của tôi** gom đơn ứng tuyển, ca làm và trạng thái tiền của từng ca, không yêu cầu người dùng hiểu một ví tiền riêng.

### 6.2. Luồng tìm việc từng bước

| Bước     | Nội dung hiển thị                    | Dữ liệu                                                          |
| -------- | ------------------------------------ | ---------------------------------------------------------------- |
| 1        | Cô/chú muốn làm việc gì?             | Chọn một/nhiều categoryId; có Xem thêm và Chưa có ưu tiên        |
| 2        | Cô/chú muốn làm ở khu vực nào?       | Một/nhiều areaCode; gợi ý từ hồ sơ; cho bỏ qua                   |
| 3        | Cô/chú rảnh lúc nào?                 | Ngày trong tuần và ca sáng/chiều/tối; cho Linh hoạt              |
| 4        | Cô/chú muốn tránh yêu cầu nào?       | Mang vác nặng, đứng lâu, di chuyển nhiều; Không có yêu cầu riêng |
| Kết thúc | Tóm tắt lựa chọn và Xem việc phù hợp | Cho sửa từng bước, không phải làm lại từ đầu                     |

Một màn hình tập trung một câu hỏi; hiển thị Bước 1/4 và các nút Quay lại/Tiếp tục. Các lựa chọn “không có ưu tiên” loại trừ lựa chọn cụ thể tương ứng. Giữ câu trả lời khi quay lại hoặc xoay màn hình. Lần sau cho chọn **Dùng lựa chọn lần trước** hoặc **Thay đổi lựa chọn**. Có lối **Xem tất cả việc**.

Nguyên tắc chia nhóm câu hỏi, chỉ báo tiến trình và cho bỏ qua phần tùy chọn được tham khảo từ hướng dẫn biểu mẫu nhiều bước của W3C; cần kiểm thử lại trong bối cảnh ứng dụng Android và người dùng của nhóm. [S6]

Không hỏi thông tin bệnh tật. Các yêu cầu tránh phản ánh mong muốn người dùng, không phải kết luận y tế. Nhóm cần thử nghiệm cách xưng hô “cô/chú” với người dùng; nội dung phải nhất quán và có thể điều chỉnh.

### 6.3. Quy tắc gợi ý

**Tập ứng viên:** tin OPEN, chưa hết hạn, qua kiểm duyệt; lọc trên dữ liệu có cấu trúc.

**Điều kiện bắt buộc:** nếu có chọn khu vực thì chỉ nhận tin trong khu vực đó; lịch cố định phải nằm trong khoảng người dùng khai rảnh; loại các tin có yêu cầu người dùng muốn tránh. Tin thiếu mô tả yêu cầu thể lực không được xem như không có yêu cầu. Ca FLEXIBLE hoặc lịch chưa cụ thể hiển thị “Cần trao đổi lịch”, không khẳng định khớp hoàn toàn.

**Xếp hạng đề xuất:** cộng điểm cho danh mục được chọn, lịch khớp cụ thể và kỹ năng đã khai; hòa điểm thì ưu tiên tin mới. Trọng số ban đầu 50/30/20 là tham số thiết kế, không phải kết quả nghiên cứu. Nếu người dùng không khai một tiêu chí thì không trừ điểm do thiếu thông tin. Không hiển thị phần trăm phù hợp như một xác suất khoa học.

**Giải thích:** “Đúng khu vực đã chọn”, “Làm buổi sáng”, “Không yêu cầu mang vác nặng theo người đăng”. Không nói “cách nhà 2 km” vì chưa dùng GPS; không gợi ý “an toàn sức khỏe”.

**Không có kết quả:** nói rõ chưa có tin phù hợp; đề nghị nới một lựa chọn, người dùng phải đồng ý. Không tự bỏ điều kiện tránh. Kết quả offline chỉ xếp hạng trên tin đã tải; ghi rõ phạm vi. Trực tuyến cần lọc/tính thứ tự trên tập ứng viên hợp lệ trước khi phân trang, không gọi vài tin đầu tải về là “phù hợp nhất toàn hệ thống”.

### 6.4. Hình thức và âm thanh

- Giá trị khởi đầu để thử nghiệm: chữ nội dung 20sp, tiêu đề 26–28sp, vùng nút chính cao tối thiểu khoảng 56dp và được giãn theo chữ. Đây là đề xuất giao diện, không bảo đảm mọi người dùng đều phù hợp.
- Dùng xanh lá đậm làm màu nhấn, nền sáng, chữ tối; đặt mục tiêu tương phản chữ tối thiểu 4,5:1. Không dùng màu làm dấu hiệu duy nhất. [S7]
- Không cố định chiều cao khối văn bản; hỗ trợ font hệ thống lớn, TalkBack, thứ tự focus hợp lý và nhãn cho biểu tượng.
- Nút Nghe hướng dẫn, Dừng, Nghe lại luôn ở vị trí nhất quán. Mặc định không tự phát; người dùng có thể bật tự đọc các bước sau. Không phát nhạc nền.
- Có thể dùng TTS cho nội dung động, bản thu âm cục bộ cho câu hướng dẫn cố định. Giọng tiếng Việt và hỗ trợ offline cần kiểm tra trên thiết bị. [S8]
- Không đọc OTP hoặc dữ liệu liên hệ tự động. Khi đổi màn hình, dừng câu cũ; xoay màn hình không tự phát lại hoặc phát chồng. Tránh nói chồng TalkBack. Âm thanh không thay thế thao tác xác nhận.

## 7. Yêu cầu responsive và ngoại tuyến

### 7.1. Responsive/adaptive

| Không gian          | Bố cục                                                              |
| ------------------- | ------------------------------------------------------------------- |
| Hẹp, điện thoại dọc | Một cột, một câu hỏi, nút chính dễ chạm                             |
| Điện thoại ngang    | Cuộn được khi chiều cao thấp; lựa chọn có thể hai cột nếu đủ chỗ    |
| Tablet              | Giới hạn chiều rộng dòng chữ, không kéo nút hết màn hình            |
| Vùng rộng           | Kết quả việc có thể danh sách bên trái và chi tiết bên phải         |
| Chia đôi cửa sổ     | Chuyển bố cục theo chiều rộng hiện có, không theo tên loại thiết bị |

Điểm ngắt đề xuất cho vùng hiển thị là dưới 600dp, 600–839dp và từ 840dp trở lên; kiểm chứng lại với font lớn và chiều cao thực tế. Dù tablet rộng, luồng hướng dẫn vẫn giữ một câu hỏi chính mỗi bước. Dùng ViewModel và trạng thái được lưu để giữ lựa chọn, vị trí và nội dung form khi thay đổi cấu hình; không khởi động giao dịch từ hàm vẽ màn hình. Xử lý khoảng an toàn thanh hệ thống và bàn phím. [S5]

### 7.2. Phạm vi offline

| Được phép khi offline                             | Cần có mạng                                           |
| ------------------------------------------------- | ----------------------------------------------------- |
| Xem bản sao tin đã tải, danh mục và khu vực       | OTP, liên kết/đổi số liên hệ                          |
| Trả lời và lưu lựa chọn tìm việc cục bộ           | Gửi/rút đơn; tạo/sửa/duyệt tin                        |
| Nghe bản ghi âm hoặc giọng TTS có dữ liệu offline | Thay đổi lưu việc trên tài khoản                      |
| Soạn nháp form chưa gửi                           | Xác nhận ca, bắt đầu/kết thúc, thanh toán, tranh chấp |

SQLite lưu CachedJob, CachedCategory, CachedArea, CachedSavedJob và LocalSearchPreference. Mỗi bản ghi cần cachedAt; dữ liệu theo tài khoản cần ownerUid. Không lưu hồ sơ căn cước, snapshot đơn, địa chỉ riêng của ca, OTP hoặc token trong các bảng này. Hồ sơ thanh toán và thông tin danh tính không được trình bày là dữ liệu mới khi chỉ có cache.

Firestore Android có cơ chế offline riêng, không thay thế phần SQLite nhóm chủ động thiết kế. Để tránh lưu ngầm tài liệu riêng xuống đĩa, cấu hình Firestore dùng bộ nhớ cache không bền cho bản triển khai này; SQLite chỉ chứa dữ liệu được cho phép. Firebase Authentication quản lý phiên theo cơ chế SDK riêng. [S3]

Khi online: đọc nguồn từ dịch vụ, cập nhật cache và giao diện. Tin đã ẩn/đóng phải được kiểm tra lại bằng lần đọc Firestore từ server; không dùng kết quả từ cache làm bằng chứng tin còn mở. Khi offline chưa thể biết tin vừa bị gỡ: hiển thị nhãn dữ liệu cũ, không cho ứng tuyển. Khi đăng xuất, xóa cache theo tài khoản và dữ liệu nhạy cảm trong bộ nhớ.

## 8. Danh sách Use Case và hướng dẫn vẽ

### 8.1. Danh mục

| Mã   | Use case                                                                                | Tác nhân chính                                        |
| ---- | --------------------------------------------------------------------------------------- | ----------------------------------------------------- |
| UC01 | Đăng nhập/đăng xuất                                                                     | Người dùng                                            |
| UC02 | Xác minh số liên hệ/liên kết tài khoản                                                  | Người dùng                                            |
| UC03 | Thiết lập vai trò và hồ sơ                                                              | Người tìm việc, người thuê                            |
| UC04 | Gửi/bổ sung hồ sơ theo vai trò: nghỉ hưu đối với SEEKER, danh tính mẫu đối với EMPLOYER | Người tìm việc, người thuê                            |
| UC05 | Xét duyệt hồ sơ, ghi kết quả tra cứu mô phỏng và thông báo                              | Admin                                                 |
| UC06 | Tìm việc có hướng dẫn                                                                   | Khách, người tìm việc                                 |
| UC07 | Xem/lọc danh sách và xem chi tiết                                                       | Khách, người tìm việc                                 |
| UC08 | Lưu/bỏ lưu việc                                                                         | Người tìm việc                                        |
| UC09 | Gửi đơn ứng tuyển                                                                       | Người tìm việc                                        |
| UC10 | Theo dõi/rút đơn                                                                        | Người tìm việc                                        |
| UC11 | Đăng/sửa/gửi tin duyệt                                                                  | Người thuê                                            |
| UC12 | Đóng tin                                                                                | Người thuê                                            |
| UC13 | Duyệt/từ chối/ẩn tin                                                                    | Admin                                                 |
| UC14 | Xem và xử lý ứng viên                                                                   | Người thuê                                            |
| UC15 | Báo cáo tin                                                                             | Người tìm việc                                        |
| UC16 | Xử lý báo cáo                                                                           | Admin                                                 |
| UC17 | Đề xuất/sửa thỏa thuận ca                                                               | Người thuê                                            |
| UC18 | Xác nhận/từ chối thỏa thuận                                                             | Hai bên                                               |
| UC19 | Cấp tiền mô phỏng cho ca                                                                | Người thuê                                            |
| UC20 | Ghi nhận bắt đầu ca                                                                     | Hai bên                                               |
| UC21 | Báo hoàn thành                                                                          | Người làm                                             |
| UC22 | Xác nhận kết quả/đề nghị xử lý                                                          | Người thuê                                            |
| UC23 | Yêu cầu và xác nhận hủy ca                                                              | Hai bên                                               |
| UC24 | Gửi/trao đổi tranh chấp                                                                 | Hai bên                                               |
| UC25 | Xử lý tranh chấp                                                                        | Admin                                                 |
| UC26 | Quyết toán mô phỏng và xem lịch sử tiền                                                 | Người có quyền kích hoạt theo kết quả ca; hai bên xem |
| UC27 | Liên hệ bằng điện thoại                                                                 | Hai bên theo quyền                                    |
| UC28 | Nghe hướng dẫn/cài đặt hỗ trợ                                                           | Người dùng                                            |
| UC29 | Xem lại khi offline                                                                     | Người dùng                                            |
| UC30 | Xem hàng đợi và thống kê vận hành                                                       | Admin                                                 |

### 8.2. Biểu diễn UML

Vẽ biên **GrandMa Assist**; tác nhân ở ngoài, use case hình ellipse ở trong. Có tác nhân tổng quát Người dùng đã đăng nhập và chuyên biệt SEEKER, EMPLOYER, ADMIN khi cần giảm đường nối. Firebase Authentication là tác nhân hỗ trợ UC01–UC02; Android TTS hỗ trợ UC28; cổng thanh toán thật không xuất hiện như một tích hợp đã có.

“Đã đăng nhập”, “đã xác minh” thường là tiền điều kiện; không nối include đến đăng nhập từ mọi nghiệp vụ. Không nối use case bằng mũi tên để biểu diễn thứ tự bước. Nếu cần include: UC19 bao gồm kiểm tra tổng tiền và ghi nhận cấp tiền; UC26 bao gồm tính phân bổ theo chính sách đã chốt. Các xử lý nội bộ này có thể chỉ ghi trong đặc tả để sơ đồ gọn.

Nên vẽ ba sơ đồ con: (a) Tài khoản và tìm việc; (b) Tuyển dụng và ca làm; (c) Quản trị và thanh toán. Bộ nguồn đi kèm cung cấp sơ đồ tổng quan chọn các use case chính; bảng trên là danh sách đầy đủ.

<!-- diagram:usecases -->

### 8.3. Đặc tả các use case trọng tâm

#### UC02 — Xác minh số liên hệ

- **Tiền điều kiện:** có mạng; người dùng đồng ý cung cấp số cho cơ chế xác thực; số được chuẩn hóa E.164, xử lý +84 đúng, không ghép thành +840…
- **Luồng chính:** nhập số → xem lại số → yêu cầu OTP → nhận/nhập mã hoặc SDK xác minh hợp lệ → Firebase trả credential → đăng nhập hoặc liên kết vào UID hiện tại → làm mới ID token → đồng bộ số liên hệ; Rules đối chiếu số với claim phone_number do Firebase Auth cấp, không tin cờ do app tự gửi.
- **Ngoại lệ:** mã sai/hết hạn, gửi lại quá nhiều, mất mạng, số đã gắn tài khoản khác, liên kết thất bại. Không tự hợp nhất tài khoản chỉ dựa trên số hoặc email trùng; cần chứng minh quyền truy cập tài khoản liên quan.
- **Hậu điều kiện:** có kết quả xác minh số liên hệ, chưa tự đổi trạng thái danh tính.
- **Đổi số:** yêu cầu xác thực lại tài khoản, xác minh số mới; nếu không còn truy cập phương thức cũ thì vào quy trình hỗ trợ, không cấp quyền chỉ vì người mới nhận OTP của số tái sử dụng.

#### UC04–UC05 — Hồ sơ nghỉ hưu trước ứng tuyển

**SEEKER — tiền điều kiện gửi duyệt:** đã đăng nhập, chọn vai trò, OTP SMS thật hợp lệ; có mạng; người dùng đã xem mục đích sử dụng giấy tờ. Có thể chuẩn bị hồ sơ trước OTP nhưng không gửi khi chưa đủ điều kiện.

1. Trong hoàn thiện hồ sơ, chọn **Chụp CCCD/căn cước**. Xin quyền camera đúng lúc; người dùng chụp, xem lại và có thể chụp lại.
2. Đọc QR từ ảnh, phân tích các trường cần thiết: họ tên, ngày sinh, số định danh phục vụ đối chiếu riêng. Trường định danh đọc được không cho sửa âm thầm; đọc sai thì quét lại hoặc yêu cầu hỗ trợ. Không tự điền năm/tháng/ngày thiếu.
3. Người dùng kiểm tra thông tin, bổ sung khu vực, kỹ năng, kinh nghiệm và lịch mong muốn. Không đặt ngưỡng tuổi để tự duyệt hoặc miễn bằng chứng nghỉ hưu.
4. Đính kèm **giấy chứng nhận nghỉ hưu hoặc ảnh BHXH mô phỏng**. Đây là BHXH theo quyết định mới, không phải bắt kiểm tra tiền tố HT trên thẻ BHYT.
5. Xem lại hồ sơ → xác nhận gửi. Tải ảnh riêng lên Storage, sau đó mới gửi SUBMITTED khi đủ dữ liệu. Nếu upload lỗi, báo lỗi và thử lại; không báo đã gửi nếu Firestore chưa xác nhận.
6. Admin mở hồ sơ đang chờ, xem ảnh căn cước, dữ liệu trích xuất và bằng chứng. Tra cứu mô phỏng là đối chiếu theo kịch bản nội bộ của nhóm rồi ghi MATCH / NOT_MATCH / INSUFFICIENT, không phải một nút tự trả về xác minh thật. Admin nhập nhận xét và quyết định APPROVED / NEEDS_INFO / REJECTED; APPROVED cần kết quả MATCH và đủ căn cứ trong kịch bản. Không có API tra cứu BHXH hay VNeID thật.
7. Transaction lưu quyết định, người xử lý, thời gian server, phiên bản hồ sơ và thông báo kết quả. Đồng thời cập nhật trạng thái đủ điều kiện của user; không có thời điểm đã mở quyền nhưng quyết định chưa lưu.
8. Người dùng nhận kết quả trong app; nếu đang đóng app thì thấy khi mở lại. NEEDS_INFO hướng dẫn bổ sung; REJECTED có lý do và đường gửi hồ sơ mới. Chỉ APPROVED hiện hành mới mở ứng tuyển.

**Ngoại lệ phải xử lý:** từ chối quyền camera; ảnh mờ/không có QR/QR khác loại/định dạng chưa hỗ trợ; ngày sinh không hợp lệ; thiếu bằng chứng; file lỗi/quá giới hạn; mất mạng khi upload; đóng/xoay màn hình; bấm gửi hai lần; hai Admin duyệt đồng thời; Admin duyệt bản cũ; ảnh không tải được để xem; người dùng chuyển tài khoản.

**Phiên bản:** không sửa ảnh/nội dung của yêu cầu đã SUBMITTED. Bổ sung tạo yêu cầu mới tham chiếu yêu cầu cũ; chỉ quyết định cho currentRequestId và identityProfileVersion hiện hành có hiệu lực. Đổi định danh sau duyệt chặn ứng tuyển mới cho đến khi duyệt lại; không xóa lịch sử hoặc ca đang có.

**Thông báo:** tiêu đề gọn “Hồ sơ đã được duyệt”, “Hồ sơ cần bổ sung” hoặc “Hồ sơ chưa được chấp thuận”; nội dung có lý do phù hợp và nút xem hồ sơ. Không chứa số căn cước/ảnh giấy tờ. Không dùng SMS thông báo kết quả; SMS chỉ phục vụ OTP. Không bắt buộc FCM ở bản này.

**Giới hạn và dữ liệu nội bộ:** nhóm dùng giấy tờ hiện có để test nội bộ, tự tạo ảnh BHXH tượng trưng. Không bắt làm lại giấy tờ hay gắn nhãn lên ảnh. Màn giải thích quy trình và tài liệu nêu tra cứu mô phỏng; không hiển thị đã xác minh bởi cơ quan nhà nước. QR và ảnh chưa chứng minh chính chủ.

**EMPLOYER — giữ v2:** gửi/bổ sung hồ sơ danh tính mẫu, Admin xét VERIFIED / NEEDS_INFO / REJECTED trước thỏa thuận/ca. Không bắt giấy nghỉ hưu, không áp ngưỡng tuổi SEEKER, không biến xác minh người đại diện thành xác minh doanh nghiệp.

<!-- diagram:retirementFlow -->

#### UC06 — Tìm việc có hướng dẫn

- **Tiền điều kiện:** không bắt đăng nhập.
- **Luồng:** trả lời bốn bước → xem tóm tắt → áp dụng điều kiện → xếp hạng → xem danh sách và lý do phù hợp → mở chi tiết.
- **Ngoại lệ:** không có kết quả; thiếu dữ liệu; offline chỉ có cache; quay lại sửa câu trả lời. Không tự nới điều kiện cứng.
- **Hậu điều kiện:** lựa chọn được giữ cục bộ; khi đăng nhập có thể đồng bộ theo sự lựa chọn của người dùng, không ghi đè âm thầm lựa chọn đã có.

#### UC11–UC13 — Đăng và duyệt tin

- **Tiền điều kiện gửi tin:** người thuê đã xác minh số, hồ sơ đủ, có mạng.
- **Luồng:** nhập nội dung cấu trúc → xem trước → gửi PENDING_REVIEW → admin đọc → OPEN hoặc REJECTED kèm lý do.
- **Kiểm tra:** tiêu đề/nhiệm vụ rõ; danh mục/khu vực hợp lệ; tiền công dương; thời hạn tương lai; lịch và yêu cầu thể lực được khai rõ.
- **Ngoại lệ:** tin hết hạn trước lúc duyệt; thiếu trường; người khác sửa; tài khoản không có quyền. Dùng version để tránh duyệt nhầm nội dung mới.
- **Hậu điều kiện:** chỉ bản đã duyệt được công khai. Sửa tin OPEN làm nó quay lại chờ duyệt; snapshot đơn và thỏa thuận cũ không bị đổi.

#### UC09 — Gửi đơn ứng tuyển

- **Tiền điều kiện:** SEEKER, ACTIVE, hồ sơ đủ, số liên hệ đã xác minh và **retirementStatus APPROVED đúng hồ sơ hiện hành trước khi ứng tuyển**. Kiểm tra lại trạng thái từ Firestore khi ghi đơn; kết quả QR hoặc tuổi không thay thế quyết định Admin.
- **Luồng:** xem thông tin sẽ gửi → lời nhắn tùy chọn → đồng ý chia sẻ số với chủ tin → xác nhận → kiểm tra tin OPEN/chưa hết hạn và chưa có đơn → tạo PENDING cùng snapshot.
- **Ngoại lệ:** trùng đơn, tin vừa đóng/ẩn, hết hạn, mất mạng. ID duy nhất cho cặp jobId–seekerId và transaction ngăn gửi trùng.
- **Hậu điều kiện:** đúng một đơn được tạo sau xác nhận thành công; thất bại không tạo đơn giả trong giao diện.

#### UC10/UC14 — Rút và xử lý đơn

- Người gửi chỉ rút PENDING; chủ tin chỉ chấp nhận/từ chối PENDING của tin mình.
- Tin OPEN/CLOSED cho xử lý đơn đã có; PENDING_REVIEW/REJECTED/HIDDEN không cho xử lý đơn chờ.
- Khi chấp nhận, chia sẻ snapshot liên hệ chủ tin với ứng viên. Chủ tin đã đọc số ứng viên theo đồng ý khi nộp đơn.
- Nếu rút và chấp nhận đồng thời, chỉ một chuyển trạng thái thắng; thao tác còn lại nhận trạng thái mới, không ghi đè.
- ACCEPTED không tự tạo ca, không tự thu tiền; không cho gửi lại cùng tin sau trạng thái cuối trong bản đầu.

#### UC17–UC18 — Chốt thỏa thuận ca

- **Tiền điều kiện:** đơn ACCEPTED; cả hai đủ hồ sơ; SEEKER có hồ sơ nghỉ hưu APPROVED hiện hành, EMPLOYER có danh tính mẫu VERIFIED như v2; tài khoản hoạt động; tin không HIDDEN. Tin CLOSED hoặc hết hạn nhận đơn vẫn cho lập ca từ quan hệ đã được chấp nhận.
- **Luồng:** người thuê đề xuất nhiệm vụ, địa điểm riêng, thời gian, tiền công → hệ thống tính phí và tổng → người thuê xác nhận gửi phiên bản → người làm xem tiền mình nhận và điều kiện → xác nhận đúng phiên bản → AWAITING_FUNDS.
- **Sửa trước cấp tiền:** tạo version mới, xóa hiệu lực cả hai xác nhận cũ, quay về PROPOSED. Khi đã cấp tiền, bản đầu không sửa trực tiếp; dùng hủy có thỏa thuận hoặc tạo ca bổ sung riêng.
- **Ngoại lệ:** lịch không hợp lệ/trùng ca READY hoặc IN_PROGRESS của người làm; trạng thái xác minh thay đổi; xác nhận phiên bản cũ; số tiền vượt giới hạn cấu hình.
- **Hậu điều kiện:** khóa snapshot và feePolicyVersion đã được cả hai xác nhận.

#### UC19 — Cấp tiền mô phỏng

- **Tiền điều kiện:** người thuê của ca, ca AWAITING_FUNDS, chưa quá hạn, version hiện tại được hai bên xác nhận.
- **Luồng:** hiển thị tiền công + phí → xác nhận mô phỏng → Java tạo đề nghị mô phỏng → Firestore Rules kiểm tra chủ ca, version, tổng theo chính sách và trạng thái → transaction ghi sự kiện và số tiền giữ → ca READY.
- **Ngoại lệ:** giao dịch thất bại/đang xử lý, double tap, mất phản hồi, request cũ. Không cho ghi riêng funded=true hoặc số tiền tùy ý; Rules chỉ chấp nhận toàn bộ chuyển trạng thái và bản ghi liên quan hợp lệ. Retry cùng requestId trả kết quả cũ thay vì cấp tiền lần hai.
- **Hậu điều kiện:** READY chỉ khi đủ toàn bộ tổng tiền; đang xử lý chưa được bắt đầu ca.

#### UC20–UC22 — Bắt đầu và hoàn thành

- **Bắt đầu:** ca READY, đủ tiền và tới khoảng thời gian cho phép; hai bên xác nhận bắt đầu. Lưu thời điểm từng xác nhận, chuyển IN_PROGRESS khi đủ hai bên. Thiếu xác nhận thì cần hỗ trợ, không coi một nút bấm là bằng chứng tuyệt đối.
- **Kết thúc:** người làm báo xong → COMPLETION_PENDING → người thuê xác nhận hoặc mở tranh chấp. Xác nhận đồng ý tạo quyết định phân bổ và yêu cầu quyết toán.
- **Không phản hồi:** sau 24 giờ đề xuất, hiển thị quá hạn trong hàng đợi khi app/Admin tải lại; xử lý quá hạn là thao tác có ghi nhận, không tự trả tiền chỉ vì hết thời gian. Admin xử lý theo quy trình có bằng chứng và lý do.
- **Ngoại lệ:** mất mạng, sai trạng thái, hai thao tác đồng thời. Không gửi lệnh nghiệp vụ âm thầm từ hàng đợi offline.

#### UC23 — Hủy ca

- PROPOSED/AWAITING_FUNDS chưa có tiền: một trong hai bên có thể hủy hoặc từ chối; không phát sinh phí.
- READY đã có tiền và chưa bắt đầu: một bên yêu cầu hủy; bên còn lại đồng ý thì hoàn toàn bộ và phí bằng 0. Trong lúc yêu cầu hủy chờ xử lý, không bắt đầu ca.
- Không đồng ý, không phản hồi hoặc có bất đồng việc đã bắt đầu: mở tranh chấp, không tự hoàn.
- Khi funding đang xử lý, yêu cầu hủy phải được tuần tự hóa với kết quả funding: nếu funding thành công muộn, hoàn theo quy trình; không để ca hủy nhưng tiền bị bỏ quên.
- Đang làm thì dùng tranh chấp để xác định phần công việc đã hoàn thành; không áp chính sách hủy trước ca máy móc.

#### UC24–UC26 — Tranh chấp và quyết toán

- **Tiền điều kiện:** hai bên của ca có quyền gửi; tối đa một tranh chấp đang mở mỗi ca; ca chưa quyết toán cuối trong bản đầu.
- **Luồng:** nêu lý do → lưu thỏa thuận và lịch sử → nhận giải trình bên kia → admin xem → quyết định phần công được duyệt, phí và tiền hoàn → ghi lý do → thực thi phân bổ mô phỏng → đóng tranh chấp sau khi hoàn tất.
- **Nguyên tắc:** admin không tự quyết vụ liên quan tài khoản của mình; không dùng kiểm duyệt tin thay cho giải quyết tranh chấp; hệ thống kiểm tra tổng số tiền, Rules kiểm tra lại phép tính và các tài liệu liên quan, không tin kết quả client.
- **Hậu điều kiện:** tiền chi + phí ghi nhận + hoàn + tiền còn giữ bằng tổng đã cấp; có lịch sử bất biến. Nếu thao tác thất bại, vẫn có trạng thái cần xử lý, không báo thành công.
- **Giới hạn:** chưa xử lý tranh chấp mở mới sau quyết toán cuối/chargeback thật. Báo cáo vận hành sau quyết toán vẫn được tiếp nhận để hỗ trợ nhưng không tự đảo tiền.

## 9. Quy tắc nghiệp vụ thống nhất

| Mã   | Quy tắc bắt buộc                                                                                                                                    |
| ---- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| BR01 | Vai trò do cơ chế tin cậy cấp; không cho tự nâng quyền admin                                                                                        |
| BR02 | Một số liên hệ đã xác minh chỉ gắn với một tài khoản đang sử dụng trong bản đầu; số điện thoại không phải bằng chứng một người chỉ có một tài khoản |
| BR03 | OTP SMS thật; xét hồ sơ nghỉ hưu có tra cứu mô phỏng và thanh toán mô phỏng được giải thích đúng; không bắt đóng nhãn lên ảnh giấy tờ               |
| BR04 | Số liên hệ chia sẻ phải là số đã xác minh, không phải trường điện thoại tùy ý trong hồ sơ                                                           |
| BR05 | SEEKER/EMPLOYER chỉ chỉnh hồ sơ của mình; thay đổi dữ liệu định danh cốt lõi yêu cầu xét lại                                                        |
| BR06 | Tin chỉ nhận đơn khi OPEN, chưa hết hạn và chủ tin còn quyền hoạt động                                                                              |
| BR07 | Sửa tin đã duyệt làm tin trở về PENDING_REVIEW; lưu version                                                                                         |
| BR08 | Người thuê chỉ sửa/đóng tin của mình; admin chỉ xử lý trường kiểm duyệt được phép                                                                   |
| BR09 | Mỗi cặp người tìm việc–tin có tối đa một đơn; chưa gửi lại sau rút/từ chối                                                                          |
| BR10 | Chỉ PENDING được rút/chấp nhận/từ chối; chuyển trạng thái phải kiểm tra lại trên server                                                             |
| BR11 | Không xóa đơn hoặc tin để làm mất lịch sử nghiệp vụ                                                                                                 |
| BR12 | Một đơn ACCEPTED có thể có nhiều ca; mỗi ca độc lập về thỏa thuận, thời gian và tiền                                                                |
| BR13 | Mỗi ca chỉ có một người làm và một người thuê; nhiều người làm phải là các ca riêng                                                                 |
| BR14 | SEEKER có nghỉ hưu APPROVED hiện hành, EMPLOYER có danh tính mẫu VERIFIED trước ca mới; không mở thêm vòng duyệt mẫu cho SEEKER                     |
| BR15 | Hai bên xác nhận cùng agreementVersion; đổi nội dung trước cấp tiền làm mất hiệu lực xác nhận cũ                                                    |
| BR16 | Ca đã cấp tiền không được sửa âm thầm tiền công/nhiệm vụ/thời gian/địa điểm                                                                         |
| BR17 | Tổng tiền cần cấp bằng tiền công cộng phí 10%; không khấu trừ phí vào tiền công người làm                                                           |
| BR18 | Chỉ READY với đủ tiền và không có yêu cầu hủy/tranh chấp chờ mới được bắt đầu                                                                       |
| BR19 | Tiền đang giữ không được tính là doanh thu phí                                                                                                      |
| BR20 | Ca hoàn thành đầy đủ thu 10%; phần hoàn thành được công nhận thu 10% phần đó                                                                        |
| BR21 | Ca hủy chưa thực hiện và đủ điều kiện hoàn toàn bộ không thu phí                                                                                    |
| BR22 | Không tính phí trên tiền hoàn, bồi thường hoặc các khoản ngoài tiền công                                                                            |
| BR23 | Phí áp dụng theo snapshot chính sách của ca, không áp ngược cấu hình mới                                                                            |
| BR24 | Một request nghiệp vụ có requestId; retry không tạo kết quả tài chính thứ hai                                                                       |
| BR25 | Quyết toán phải bảo toàn tiền, số nguyên VND và không âm                                                                                            |
| BR26 | Chưa phản hồi không đồng nghĩa đồng ý; quá hạn chuyển hàng đợi admin                                                                                |
| BR27 | Tin HIDDEN chặn đơn/ca mới; không xóa thỏa thuận, đơn hoặc tiền của các ca hiện có                                                                  |
| BR28 | Cần dừng ca hiện có vì vi phạm phải có quyết định riêng và đường xử lý tiền, không chỉ đổi trạng thái tin                                           |
| BR29 | Người dùng chỉ xem dữ liệu riêng thuộc quan hệ nghiệp vụ của mình; admin đọc đúng phạm vi cần xử lý                                                 |
| BR30 | Offline không được xác nhận nghiệp vụ nhạy cảm; không dùng đồng hồ thiết bị để quyết định hạn hay tiền                                              |
| BR31 | Mở trình quay số không tự gọi; người dùng chủ động xác nhận cuộc gọi                                                                                |
| BR32 | Không hiển thị “đã đảm bảo an toàn” chỉ từ kết quả duyệt tin, OTP hoặc hồ sơ mẫu                                                                    |
| BR33 | SEEKER phải được Admin duyệt nghỉ hưu trước khi gửi đơn; không miễn hồ sơ bằng tuổi hoặc mã HT                                                      |
| BR34 | Yêu cầu đã gửi là phiên bản bất biến; chỉ bản hiện hành được duyệt; bổ sung tạo bản mới                                                             |
| BR35 | Quyết định, trạng thái quyền và thông báo kết quả ghi nguyên tử; người dùng không tự APPROVED                                                       |
| BR36 | Người thuê/khách không được đọc ảnh căn cước, bằng chứng nghỉ hưu hoặc nội dung tra cứu riêng                                                       |

## 10. Mô hình trạng thái

### 10.1. Tin và đơn

**JobStatus:** PENDING_REVIEW, OPEN, REJECTED, CLOSED, HIDDEN.

| Chuyển trạng thái         | Ai thực hiện | Điều kiện                   |
| ------------------------- | ------------ | --------------------------- |
| Tạo → PENDING_REVIEW      | Người thuê   | Đủ dữ liệu và quyền         |
| PENDING_REVIEW → OPEN     | Admin        | Duyệt đúng version, còn hạn |
| PENDING_REVIEW → REJECTED | Admin        | Có lý do                    |
| REJECTED → PENDING_REVIEW | Chủ tin      | Sửa và gửi lại              |
| OPEN → PENDING_REVIEW     | Chủ tin      | Sửa nội dung                |
| OPEN → CLOSED             | Chủ tin      | Đóng nhận đơn mới           |
| OPEN/CLOSED → HIDDEN      | Admin        | Có lý do, lưu lịch sử       |

expiresAt là điều kiện riêng; OPEN nhưng quá hạn không nhận đơn. Không cần tự đổi sang trạng thái EXPIRED. Chưa hỗ trợ mở lại CLOSED/HIDDEN trong bản đầu; chủ tin có thể tạo tin mới qua kiểm duyệt. Soạn nháp là local, không phải trạng thái server của tin.

**ApplicationStatus:** PENDING → ACCEPTED / REJECTED / WITHDRAWN. Ba trạng thái cuối không đổi lại. ACCEPTED là liên kết để đề xuất ca, không phải trạng thái hoàn thành việc.

### 10.2. Ca làm

<!-- diagram:sessionState -->

```mermaid
stateDiagram-v2
    [*] --> PROPOSED
    PROPOSED --> AWAITING_FUNDS: Hai bên xác nhận cùng phiên bản
    AWAITING_FUNDS --> PROPOSED: Sửa trước cấp tiền
    AWAITING_FUNDS --> READY: Cấp đủ tiền
    PROPOSED --> CANCELLED: Từ chối hoặc hủy
    AWAITING_FUNDS --> CANCELLED: Hủy hoặc hết hạn khi chưa có tiền
    READY --> IN_PROGRESS: Hai bên xác nhận bắt đầu
    IN_PROGRESS --> COMPLETION_PENDING: Người làm báo xong
    COMPLETION_PENDING --> SETTLEMENT_PENDING: Người thuê đồng ý
    READY --> SETTLEMENT_PENDING: Hai bên đồng ý hủy
    READY --> DISPUTED: Có bất đồng
    IN_PROGRESS --> DISPUTED: Có bất đồng
    COMPLETION_PENDING --> DISPUTED: Có bất đồng hoặc cần admin xử lý
    DISPUTED --> SETTLEMENT_PENDING: Admin quyết định
    SETTLEMENT_PENDING --> COMPLETED: Đã phân bổ, có tiền công được duyệt
    SETTLEMENT_PENDING --> CANCELLED: Đã phân bổ, tiền công bằng không
```

Yêu cầu hủy chờ được lưu bằng CancelRequest, không ép ca sang CANCELLED trước khi hoàn tiền. COMPLETED có completionOutcome FULL/PARTIAL. Các lỗi giao dịch được giữ ở PaymentRecord, không tự đổi ca thành hoàn thành. Các terminal COMPLETED/CANCELLED chỉ được đặt khi tiền còn giữ bằng 0 hoặc chưa từng cấp tiền.

### 10.3. Thanh toán

**PaymentStatus:** UNFUNDED → FUNDING → HELD → SETTLING → SETTLED.

- Funding thất bại chắc chắn: trở lại UNFUNDED, lưu lỗi và sự kiện thất bại. Chưa biết kết quả: giữ FUNDING để đối chiếu, không cho cấp thêm một lần khác.
- Settlement thất bại trước áp dụng: trở lại HELD, ca vẫn SETTLEMENT_PENDING, có nút thử xử lý lại theo quyền. Chưa rõ kết quả: giữ SETTLING để đối chiếu.
- SETTLED nghĩa là toàn bộ tiền đã được phân bổ theo quyết định, có thể chi trả đầy đủ, hoàn toàn bộ hoặc kết hợp chi/hoàn; không đồng nghĩa luôn trả toàn bộ cho người làm.
- Trong bản mô phỏng, áp dụng các phần chi/hoàn/phí cùng transaction. Kịch bản lỗi xảy ra trước transaction hoặc trả lại kết quả đã ghi; không giả lập chuyển khoản thật từng phần.

**IdentityStatus (EMPLOYER):** NOT_SUBMITTED → SUBMITTED → VERIFIED / NEEDS_INFO / REJECTED; sửa định danh hoặc thu hồi quyết định → REVERIFY_REQUIRED. Giữ luồng v2. Hồ sơ nghỉ hưu SEEKER dùng trạng thái riêng bên dưới, không suy ra từ IdentityStatus.

**DisputeStatus:** OPEN → UNDER_REVIEW → DECIDED → RESOLVED; DECIDED có thể tồn tại trong lúc quyết toán thất bại. **ReportStatus:** OPEN → RESOLVED với resolution NO_VIOLATION/JOB_HIDDEN.

### 10.4. Hồ sơ nghỉ hưu và điều kiện ứng tuyển

| Trạng thái hiện hành | Hiển thị                         | Ai/chuyển tiếp                                     | Ứng tuyển mới                            |
| -------------------- | -------------------------------- | -------------------------------------------------- | ---------------------------------------- |
| NOT_SUBMITTED        | Chưa gửi hồ sơ                   | Chủ hồ sơ chuẩn bị, gửi → SUBMITTED                | Không                                    |
| SUBMITTED            | Đang chờ duyệt                   | Admin → APPROVED / NEEDS_INFO / REJECTED           | Không                                    |
| NEEDS_INFO           | Cần bổ sung — kèm lý do          | Chủ hồ sơ tạo bản mới → SUBMITTED                  | Không                                    |
| REJECTED             | Chưa được chấp thuận — kèm lý do | Có thể sửa/bổ sung tạo bản mới → SUBMITTED         | Không                                    |
| APPROVED             | Hồ sơ nghỉ hưu đã được duyệt     | Đổi định danh/thu hồi có lý do → REVERIFY_REQUIRED | Có, nếu OTP và các điều kiện khác hợp lệ |
| REVERIFY_REQUIRED    | Cần xét duyệt lại                | Chủ hồ sơ tạo bản mới → SUBMITTED                  | Không                                    |

Mỗi request đã quyết định giữ nguyên kết quả lịch sử. “Trạng thái hiện hành” thuộc tài khoản và trỏ đúng request/version; không ghi đè hồ sơ cũ để tạo cảm giác đã duyệt bản mới. Admin không tự xét hồ sơ của mình. Kết quả hết hiệu lực không hủy ngầm đơn/ca cũ.

## 11. Chính sách tiền công, phí và duy trì hệ thống

### 11.1. Công thức

Gọi C là tiền công đã chốt, r = 10%, F(x) = làm tròn half-up(x × r) đến một đồng. Tất cả giá trị tiền là số nguyên VND, không dùng số thực dấu phẩy động để cộng tiền.

- Tổng phải cấp T = C + F(C).
- Hoàn thành đầy đủ: người làm nhận C; phí F(C); hoàn 0.
- Hoàn thành một phần được duyệt A, với 0 ≤ A ≤ C: người làm nhận A; phí F(A); hoàn T − A − F(A).
- Hủy đủ điều kiện hoàn toàn bộ: A = 0, phí = 0, hoàn T.
- Bất biến: **totalFunded = workerPaid + feeRecognized + employerRefunded + heldAmount**.

Một ca chỉ cấp tiền thành công một lần trong bản đầu. Không có nạp ví, rút ví hoặc top-up ca đã khóa. Nếu có phần việc bổ sung, tạo một ca bổ sung riêng được xác nhận và cấp tiền trước khi làm phần đó. Không cho tăng A vượt C khi quyết toán.

| Kịch bản ca 240.000đ   |  Đã cấp | Người làm |    Phí |    Hoàn | Còn giữ |
| ---------------------- | ------: | --------: | -----: | ------: | ------: |
| Vừa cấp tiền           | 264.000 |         0 |      0 |       0 | 264.000 |
| Hoàn thành đủ          | 264.000 |   240.000 | 24.000 |       0 |       0 |
| Duyệt một phần 120.000 | 264.000 |   120.000 | 12.000 | 132.000 |       0 |
| Hủy hoàn toàn bộ       | 264.000 |         0 |      0 | 264.000 |       0 |

Trường hợp hủy/tranh chấp phải có lý do và kết quả được thông báo cho hai bên. Phí của phần việc được công nhận sau tranh chấp là chính sách GrandMa Assist, cần được nêu trước khi chốt ca.

### 11.2. Cách hiển thị

Người thuê thấy: “Tiền công người làm nhận: 240.000đ”; “Phí dịch vụ 10%: 24.000đ”; “Tổng: 264.000đ”. Người làm thấy nổi bật: “Tiền công của cô/chú: 240.000đ — không trừ phí nền tảng”. Mọi màn hình tiền có nhãn **Mô phỏng — không sử dụng tiền thật**.

Tổng phí phải được dự tính ngay khi nhập tiền công, không xuất hiện như phụ thu bất ngờ ở bước cuối. Không tự thay mức phí với thỏa thuận đã xác nhận. Bản đầu dùng policy GMA_FEE_V1, rateBps = 1000, payer = EMPLOYER, chưa có phí tối thiểu/trần/hội viên/khuyến mại.

### 11.3. Cơ sở tham khảo và giới hạn

bTaskee công bố trong tài liệu đối tác mức khấu trừ 20% theo giao dịch hoàn thành, có thể thay đổi; Taskrabbit mô tả phí tính cho khách ngoài đơn giá người làm; Airtasker ở Úc có phí đối tác và phí kết nối khách riêng. Đây là cơ sở tham khảo các cách thu, không chứng minh 10% là trung bình thị trường. [S9][S10][S11]

GrandMa Assist chọn người thuê chịu 10% để người nghỉ hưu nhìn thấy tiền công nhất quán. Thu phí tạo nguồn thu nhưng chưa chứng minh đủ duy trì. Chi phí cần theo dõi gồm hạ tầng, OTP, hỗ trợ, kiểm duyệt, xử lý tranh chấp, bảo trì; khi có tiền thật còn có phí xử lý thanh toán và các nghĩa vụ liên quan.

**Kịch bản minh họa, không phải báo giá:** tiền công bình quân 200.000đ, phí 20.000đ/ca; biến phí 5.000đ/ca; định phí 3.000.000đ/tháng. Điểm hòa vốn theo giả định = 3.000.000 / (20.000 − 5.000) = 200 ca/tháng. Nếu phí không lớn hơn biến phí thì không thể hòa vốn chỉ bằng tăng số ca. Chưa có dữ liệu thực tế về nhu cầu hay lợi nhuận.

Admin xem tách biệt: tổng giao dịch, tiền đang giữ, đã trả người làm, đã hoàn, phí đã ghi nhận và số ca quyết toán. Lợi nhuận không đồng nghĩa phí thu được; cần trừ chi phí. Mô hình duy trì còn cần sao lưu, kiểm thử phân quyền, kiểm soát chi phí OTP, giới hạn truy vấn và lịch sử xử lý.

## 12. Mô hình lớp nghiệp vụ và từ điển dữ liệu

### 12.1. Quy ước chung

- String là chuỗi; Long là số nguyên VND hoặc bộ đếm; Instant lưu timestamp server; LocalTime dùng cho lịch mẫu; dấu ? là trường tùy chọn.
- ID là giá trị không chứa dữ liệu riêng. Không dùng số điện thoại/căn cước làm document ID công khai.
- Mọi snapshot đã khóa giữ nguyên; thông tin hiện tại của hồ sơ không tự thay thế nội dung đã xác nhận.
- Dữ liệu thời gian sự kiện lưu UTC, hiển thị theo Asia/Ho_Chi_Minh cho bối cảnh Việt Nam; không quyết định hết hạn bằng giờ thiết bị.
- Các thuộc tính *By là UID/reference, *At là Instant; trường trạng thái dùng enum, không dùng chuỗi tiếng Việt tùy ý.

### 12.2. Tài khoản, hồ sơ và gợi ý

| Lớp | Thuộc tính chính | Trách nhiệm/phương thức |
| --- | --- | --- |
| User | id, displayName, role, accountStatus, phoneE164?, phoneVerifiedAt?, phoneVerificationMode, identityStatus?, identityMode?, identityRequestId?, retirementStatus?, currentRetirementRequestId?, approvedIdentityProfileVersion?, createdAt, updatedAt | canApply(), canConfirmSession(); Firestore Rules kiểm tra lại, client không tự cấp quyền |
| JobSeekerProfile | userId, fullName, dateOfBirth, identityProfileVersion, areaCode, skills: List, experienceSummary, preferredShifts: List | isCompleteForApplication() |
| EmployerProfile | userId, employerType, organizationName?, introduction, areaCode | isCompleteForPosting() |
| SearchPreference | userId, categoryIds: List, areaCodes: List, availableDays: List, availableShifts: List, avoidedDemands: List, updatedAt | validate(), hasHardConstraints() |
| AccessibilitySettings | fontScaleOption, voiceEnabled, autoReadEnabled, speechRate | Value object cục bộ; không giảm cỡ chữ hệ thống |
| Area | code, name, parentCode?, active | Danh mục khu vực; quan hệ tự tham chiếu 0..1 cha |
| JobCategory | id, name, iconKey, active | Danh mục việc |
| IdentityVerificationRequest | id, userId, profileVersion, sampleEvidenceRefs: List, mode, status, submittedAt, reviewedBy?, reviewedAt?, reason? | canReview(); chỉ EMPLOYER, mode DEMO theo v2 |
| RetirementVerificationRequest | id, userId, previousRequestId?, identityProfileVersion, identitySnapshot, identityImagePath, evidenceType, evidenceImagePaths, status: RetirementRequestStatus, submittedAt?, reviewedBy?, reviewedAt?, lookupMode, lookupResult?, reason?, decisionVersion | SEEKER; lookupMode SIMULATED; nội dung gửi được khóa theo phiên bản |
| IdentitySnapshot | fullName, dateOfBirth, documentNumber, source: CCCD_QR, parserVersion | Thông tin riêng phục vụ xét duyệt, không đưa vào snapshot ứng tuyển |
| InAppNotification | id, recipientUid, type, requestId, decisionVersion, title, message, createdAt, readAt? | Kết quả xét duyệt bền vững; chỉ chủ tài khoản đánh dấu đã đọc |

dateOfBirth dùng ngày lịch YYYY-MM-DD; không lưu tuổi làm nguồn chuẩn vì tuổi thay đổi theo thời gian. Không đưa toàn bộ chuỗi QR, địa chỉ thường trú hoặc trường không cần thiết vào hồ sơ công khai. evidenceType là RETIREMENT_CERTIFICATE hoặc BHXH_SIMULATED; không dùng bộ phân loại BHYT/HT.

Số liên hệ không lặp dưới dạng có thể sửa tùy ý trong hai profile. Lấy từ User đã được đối chiếu với Firebase Auth qua Rules để tạo contact snapshot. User là tài liệu riêng, không đọc công khai; public name/badge nếu cần dùng một bản chiếu chỉ chứa trường công khai.

### 12.3. Tin, đơn và báo cáo

| Lớp | Thuộc tính chính | Trách nhiệm/phương thức |
| --- | --- | --- |
| JobPost | id, employerId, employerDisplayName, categoryId, areaCode, title, description, requirementsText, requiredSkillTags: List, workDays: List, workShift, scheduleText, physicalDemands: List, demandsDeclared, payAmount: Long, payUnit, status, version, expiresAt, createdAt, updatedAt, reviewedBy?, reviewedAt?, reviewReason? | isExpired(now), canReceiveApplication(now), validate() |
| Application | id, jobId, seekerId, employerId, message?, status, applicantSnapshot, jobSnapshot, employerContactSnapshot?, createdAt, updatedAt | canWithdraw(), canReview() |
| SavedJob | seekerId, jobId, savedAt | Liên kết duy nhất theo cặp |
| Report | id, reporterId, jobId, reason, detail?, status, resolution?, handledBy?, resolutionNote?, createdAt, resolvedAt? | isResolved() |
| ApplicantSnapshot | displayName, verifiedPhone, skills, experienceSummary, consentAt, profileVersion | Dữ liệu người gửi đồng ý chia sẻ khi ứng tuyển |
| JobSnapshot | title, categoryId, areaCode, scheduleText, physicalDemands, payAmount, payUnit, jobVersion | Điều kiện tin tại lúc nộp đơn |
| ContactSnapshot | displayName, verifiedPhone, sharedAt | Liên hệ chủ tin khi chấp nhận |

payUnit của tin có thể HOUR/SESSION/DAY. Tin là thông tin tham khảo; khi lập ca phải chốt tổng laborAmount cụ thể. Nếu tính từ đơn giá giờ thì hiển thị thời lượng và phép tính để hai bên xác nhận, không tự dùng số giờ GPS hoặc bộ đếm làm tiền cuối.

### 12.4. Ca, tiền và tranh chấp

| Lớp | Thuộc tính chính | Trách nhiệm/phương thức |
| --- | --- | --- |
| WorkSession | id, applicationId, jobId, seekerId, employerId, agreement, agreementVersion, employerAcceptedVersion?, seekerAcceptedVersion?, status, cancellationRequest?, employerStartedAt?, seekerStartedAt?, completionReportedAt?, completionResponseDueAt?, completionOutcome?, createdAt, updatedAt | isAgreementConfirmed(), canStart(), canReportCompletion() |
| AgreementSnapshot | tasks, privateAddress, areaCode, startAt, endAt, laborAmount: Long, feeRateBps, serviceFee: Long, totalDue: Long, feePolicyVersion, cancellationPolicyVersion, fundingDeadline | Bản chốt có version; thay đổi tạo phiên bản mới trước funding |
| CancelRequest | requestedBy, reason, requestedAt, respondedBy?, respondedAt?, status | Value object; PENDING/ACCEPTED/REJECTED |
| FeePolicy | version, rateBps, payer, roundingMode, effectiveAt, active | feeFor(amount), quote(amount); không sửa policy đã được ca tham chiếu |
| PaymentRecord | sessionId, employerId, seekerId, mode, agreementVersion, status, expectedTotal, totalFunded, heldAmount, workerPaid, feeRecognized, employerRefunded, approvedLaborAmount?, lastOperationId?, lastError?, updatedAt | allocationFor(approvedAmount), isBalanced() |
| PaymentEvent | id, sessionId, operationId, type, status, fundingDelta, workerPaidDelta, feeDelta, refundDelta, actorId?, createdAt, errorCode? | Bất biến; chỉ sự kiện thành công thay đổi số tiền |
| Dispute | id, sessionId, openedBy?, openedByType: USER/ADMIN, reason, description, status, responseDueAt, assignedAdminId?, decision?, createdAt, resolvedAt? | canRespond(), canResolve(); ADMIN khi Admin tiếp nhận quá hạn; không tự tạo bởi tác vụ nền |
| DisputeDecision | approvedLaborAmount, computedFee, computedRefund, reason, decidedBy, decidedAt, version | Lý do quyết định; tiền tính theo FeePolicy snapshot |
| AuditEvent | id, entityType, entityId, action, actorId, requestId, previousVersion?, newVersion?, createdAt | Nhật ký thay đổi quan trọng; không chứa OTP/giấy tờ thô |

Dispute có các phản hồi bên trong subcollection Responses gồm id, authorId, message, createdAt. Bản đầu dùng giải trình chữ và lịch sử hệ thống; chưa bắt buộc tải ảnh nhà riêng hay ảnh người thật. Mỗi quyết định cần lý do; dữ liệu hiện có không được coi là chứng cứ tuyệt đối.

### 12.5. Enum

| Enum | Giá trị |
| --- | --- |
| UserRole | SEEKER, EMPLOYER, ADMIN |
| AccountStatus | ACTIVE, RESTRICTED |
| EmployerType | INDIVIDUAL, ORGANIZATION |
| WorkShift | MORNING, AFTERNOON, EVENING, FLEXIBLE |
| PhysicalDemand | HEAVY_LIFTING, PROLONGED_STANDING, FREQUENT_MOVEMENT |
| PayUnit | HOUR, SESSION, DAY |
| JobStatus | PENDING_REVIEW, OPEN, REJECTED, CLOSED, HIDDEN |
| ApplicationStatus | PENDING, ACCEPTED, REJECTED, WITHDRAWN |
| SessionStatus | PROPOSED, AWAITING_FUNDS, READY, IN_PROGRESS, COMPLETION_PENDING, DISPUTED, SETTLEMENT_PENDING, COMPLETED, CANCELLED |
| PaymentStatus | UNFUNDED, FUNDING, HELD, SETTLING, SETTLED |
| VerificationMode | REAL, TEST, DEMO; áp dụng phù hợp từng loại xác minh |
| IdentityStatus | NOT_SUBMITTED, SUBMITTED, NEEDS_INFO, VERIFIED, REJECTED, REVERIFY_REQUIRED |
| RetirementStatus | NOT_SUBMITTED, SUBMITTED, NEEDS_INFO, APPROVED, REJECTED, REVERIFY_REQUIRED |
| RetirementRequestStatus | DRAFT, SUBMITTED, NEEDS_INFO, APPROVED, REJECTED; DRAFT chỉ phục vụ upload, chưa là hồ sơ gửi duyệt |
| RetirementEvidenceType | RETIREMENT_CERTIFICATE, BHXH_SIMULATED |
| LookupResult | MATCH, NOT_MATCH, INSUFFICIENT; luôn là kết quả tra cứu mô phỏng do Admin ghi |
| DisputeStatus | OPEN, UNDER_REVIEW, DECIDED, RESOLVED |
| ReportReason | MISLEADING, INAPPROPRIATE, SUSPECTED_SCAM, OTHER |
| ReportResolution | NO_VIOLATION, JOB_HIDDEN |
| PaymentEventType | FUNDING_SUCCEEDED, SETTLEMENT_SUCCEEDED, ATTEMPT_FAILED |

### 12.6. Quan hệ và bội số

| Quan hệ | Bội số và ràng buộc |
| --- | --- |
| User — JobSeekerProfile | 1 — 0..1, chỉ role SEEKER |
| User — EmployerProfile | 1 — 0..1, chỉ role EMPLOYER; hai profile loại trừ nhau |
| User — SearchPreference | 1 — 0..1; khách có bản local chưa gắn User |
| User — IdentityVerificationRequest | EMPLOYER 1 — 0..\*; tối đa một yêu cầu đang xét hiện hành |
| User — RetirementVerificationRequest | SEEKER 1 — 0..\*; một currentRequestId; version ảnh và hồ sơ không đổi sau gửi |
| User — InAppNotification | 1 — 0..\*; người khác không được đọc |
| EmployerProfile — JobPost | 1 — 0..\* |
| JobCategory/Area — JobPost | Mỗi tin thuộc 1 category và 1 area; danh mục có 0..\* tin |
| JobPost — Application | 1 — 0..\* |
| JobSeekerProfile — Application | 1 — 0..\*; duy nhất cặp seekerId–jobId |
| JobSeekerProfile/JobPost — SavedJob | Mỗi SavedJob liên kết đúng 1 người và 1 tin |
| Application — WorkSession | 1 — 0..\*; chỉ Application ACCEPTED |
| WorkSession — AgreementSnapshot | Composition 1 — 1 cho phiên bản hiện hành; lịch sử riêng |
| WorkSession — PaymentRecord | 1 — 1, tạo UNFUNDED khi tạo ca |
| PaymentRecord — PaymentEvent | 1 — 0..\* |
| WorkSession — Dispute | 1 — 0..\* về lịch sử, tối đa một tranh chấp hoạt động |
| FeePolicy — AgreementSnapshot | 1 — 0..\*, theo version đã chốt |
| User — Report; JobPost — Report | Mỗi report có 1 người gửi và 1 tin; mỗi bên có 0..\* report |

Không dùng kế thừa SEEKER/EMPLOYER/ADMIN như ba bản sao người dùng; dùng role và profile chuyên biệt để ánh xạ Firebase UID. Các phương thức trong lớp chỉ thể hiện quy tắc, không thay thế kiểm tra quyền tại Firebase Rules.


### 12.7. Class diagram A — tài khoản và tuyển dụng

```mermaid
classDiagram
    class User {
        +String id
        +UserRole role
        +IdentityStatus identityStatus
        +RetirementStatus retirementStatus
        +canConfirmSession() boolean
    }
    class JobSeekerProfile {
        +String userId
        +List skills
        +isCompleteForApplication() boolean
    }
    class EmployerProfile {
        +String userId
        +EmployerType employerType
    }
    class JobPost {
        +String id
        +JobStatus status
        +long payAmount
        +canReceiveApplication(now) boolean
    }
    class Application {
        +String id
        +ApplicationStatus status
        +canWithdraw() boolean
    }
    class SavedJob
    class SearchPreference
    class Area
    class JobCategory
    class IdentityVerificationRequest
    class RetirementVerificationRequest
    class InAppNotification
    class Report
    User "1" -- "0..1" JobSeekerProfile : có
    User "1" -- "0..1" EmployerProfile : có
    User "1" -- "0..1" SearchPreference : lựa chọn
    User "1" -- "0..*" IdentityVerificationRequest : người thuê gửi
    User "1" -- "0..*" RetirementVerificationRequest : người tìm việc gửi
    User "1" -- "0..*" InAppNotification : nhận
    EmployerProfile "1" -- "0..*" JobPost : đăng
    JobCategory "1" -- "0..*" JobPost : phân loại
    Area "1" -- "0..*" JobPost : khu vực
    JobPost "1" -- "0..*" Application : nhận
    JobSeekerProfile "1" -- "0..*" Application : gửi
    JobSeekerProfile "1" -- "0..*" SavedJob : lưu
    JobPost "1" -- "0..*" SavedJob : được lưu
    User "1" -- "0..*" Report : gửi
    JobPost "1" -- "0..*" Report : được báo cáo
```
    
### 12.8. Class diagram B — ca và thanh toán

```mermaid
classDiagram
    class Application
    class WorkSession {
        +String id
        +int agreementVersion
        +SessionStatus status
        +isAgreementConfirmed() boolean
        +canStart() boolean
    }
    class AgreementSnapshot {
        +long laborAmount
        +long serviceFee
        +long totalDue
        +String feePolicyVersion
    }
    class PaymentRecord {
        +PaymentStatus status
        +long totalFunded
        +long heldAmount
        +long workerPaid
        +long feeRecognized
        +long employerRefunded
        +isBalanced() boolean
    }
    class PaymentEvent {
        +String operationId
        +PaymentEventType type
    }
    class FeePolicy {
        +String version
        +int rateBps
        +feeFor(amount) long
    }
    class Dispute {
        +DisputeStatus status
        +String reason
    }
    class DisputeDecision
    Application "1" -- "0..*" WorkSession : sinh ca
    WorkSession "1" *-- "1" AgreementSnapshot
    WorkSession "1" -- "1" PaymentRecord
    PaymentRecord "1" -- "0..*" PaymentEvent
    FeePolicy "1" -- "0..*" AgreementSnapshot : áp dụng
    WorkSession "1" -- "0..*" Dispute
    Dispute "1" *-- "0..1" DisputeDecision
```

## 13. Thiết kế Firestore và SQLite

### 13.1. Cấu trúc Firestore

| Đường dẫn | Nội dung |
| --- | --- |
| users/{uid} | Tài khoản riêng, số liên hệ, quyền, trạng thái xác minh |
| seekerProfiles/{uid} | Hồ sơ tìm việc |
| employerProfiles/{uid} | Hồ sơ người thuê |
| users/{uid}/preferences/search | SearchPreference |
| users/{uid}/savedJobs/{jobId} | SavedJob |
| areas/{code}; jobCategories/{id} | Danh mục chuẩn bị sẵn |
| jobs/{jobId} | Tin và version hiện hành |
| applications/{applicationId} | Đơn, snapshot và hai UID liên quan |
| identityRequests/{requestId} | Hồ sơ danh tính mẫu của EMPLOYER, giữ v2 |
| retirementRequests/{requestId} | Dữ liệu riêng, snapshot QR, đường dẫn ảnh, quyết định xét nghỉ hưu của SEEKER |
| users/{uid}/notifications/{notificationId} | Kết quả xét duyệt; khóa theo requestId + decisionVersion để chống thông báo trùng |
| adminAccess/{uid} | Quyền Admin cấp sẵn qua Console; client không tạo/sửa |
| workSessions/{sessionId} | Ca và snapshot thỏa thuận hiện hành |
| workSessions/{sessionId}/agreementVersions/{version} | Lịch sử phiên bản thỏa thuận |
| payments/{sessionId} | PaymentRecord một-một với ca |
| payments/{sessionId}/events/{eventId} | Sự kiện tiền bất biến |
| disputes/{disputeId} | Tranh chấp và quyết định |
| disputes/{disputeId}/responses/{responseId} | Giải trình của hai bên/admin |
| reports/{reportId} | Báo cáo tin |
| feePolicies/{version} | Chính sách phí có phiên bản |
| operations/{operationId} | Khóa idempotency, actor, lệnh, fingerprint và kết quả |
| auditEvents/{eventId} | Nhật ký hành động quan trọng |

applicationId tạo nhất quán từ jobId và seekerId; quy ước ID có ranh giới rõ và Rules kiểm tra khớp các trường, không chỉ tin Java. SavedJob dùng jobId dưới đường dẫn người dùng. Không đổi UID khi bổ sung Google/phone. Trường liên kết ở Firestore không tự có tính chất khóa ngoại: transaction và Rules phải kiểm tra tài liệu đích, chủ sở hữu và trạng thái.

### 13.2. Nhóm truy vấn và chỉ mục dự kiến

| Màn hình | Truy vấn | Chỉ mục dự kiến |
| --- | --- | --- |
| Danh sách việc | status, areaCode, categoryId, hạn và thứ tự | Chỉ mục ghép theo bộ lọc thực tế |
| Tin của tôi | employerId, status, updatedAt | employerId + status + updatedAt |
| Đơn của tôi | seekerId, createdAt | seekerId + createdAt |
| Ứng viên của tin | jobId, status, createdAt | jobId + status + createdAt |
| Ca của tôi | seekerId hoặc employerId, status, agreement.startAt | Hai nhóm chỉ mục theo vai trò |
| Hàng đợi admin | status và createdAt/submittedAt/dueAt | Chỉ mục riêng từng collection |

Chỉ mục chính xác được tạo theo truy vấn triển khai và kiểm thử, không giả định Firestore tự tìm kiếm toàn văn tiếng Việt. Bản đầu ưu tiên bộ lọc cấu trúc; chưa có tìm kiếm ngữ nghĩa hay search engine ngoài.

Gợi ý được Java tính trên tập dữ liệu công khai hợp lệ đã tải đầy đủ trong giới hạn demo, trước khi phân trang kết quả. Đề xuất giới hạn kiểm thử 500 tin còn hiệu lực: nếu vượt giới hạn xử lý thì trả trạng thái giới hạn rõ ràng hoặc chuyển sang danh sách lọc theo thời gian; không cắt âm thầm rồi khẳng định kết quả tốt nhất. Khi triển khai lớn cần thiết kế chỉ mục/tìm kiếm riêng.

### 13.3. SQLite

| Bảng | Khóa và trường quan trọng |
| --- | --- |
| cached_jobs | job_id PK, public fields, remote_version, cached_at |
| cached_categories | category_id PK, name, active |
| cached_areas | area_code PK, name, parent_code, active |
| cached_saved_jobs | (owner_uid, job_id) PK, saved_at, last_synced_at |
| local_search_preferences | owner_key PK, selections_json, updated_at |

owner_key của khách là giá trị local riêng; khi đăng nhập không tự gán toàn bộ dữ liệu của một người trước đó cho tài khoản mới. Sử dụng SQLiteOpenHelper và lớp truy cập SQLite bằng Java để sát yêu cầu môn học. Migration schema phải có version. Truy vấn local không chạy trên main thread.

### 13.4. Ảnh hồ sơ trong Firebase Storage

- Đường dẫn riêng: verification/{uid}/{requestId}/{fileId}.jpg; Firestore chỉ lưu đường dẫn và metadata cần thiết, không lưu ảnh Base64.
- Chủ hồ sơ upload vào bản nháp của mình; Admin được phân quyền đọc để xét. Storage Rules đối chiếu quyền và trạng thái yêu cầu; không dùng bucket công khai hoặc link tải có token làm cơ chế phân quyền.
- Sau SUBMITTED, client không ghi đè/xóa ảnh của bản đang xét hoặc đã quyết định. Bổ sung dùng request/fileId mới. Chủ hồ sơ không được thay bằng đường dẫn ảnh của người khác.
- Upload Storage và transaction Firestore không nguyên tử: dùng bản nháp kỹ thuật DRAFT trước SUBMITTED. DRAFT là trạng thái request, không phải trạng thái duyệt của User; không được ghi đè quyết định cũ hoặc tự mở quyền. Bản nháp chưa nằm trong hàng đợi xét. Chỉ gửi sau khi SDK xác nhận tải xong; Admin chưa xem được ảnh thì không duyệt.
- Giới hạn đề xuất: JPEG/PNG, tối đa 10 MB mỗi ảnh; một ảnh căn cước chứa QR và một đến ba ảnh bằng chứng. Kiểm tra cả nội dung đọc được trên app và metadata/kích thước tại Rules. Các giới hạn là tham số triển khai để kiểm thử, không yêu cầu người dùng làm giấy tờ mới.
- Ảnh nháp tạm giữ trong vùng riêng ứng dụng để phục hồi khi xoay; dọn khi hủy, đăng xuất hoặc gửi thành công. Không lưu giấy tờ vào SQLite/offline cache, log, Git hay ảnh chụp báo cáo công khai.
- Không có tác vụ xóa tự động nền trong bản này. Đề xuất nhóm quản trị xóa ảnh kiểm thử và dữ liệu định danh thô sau khi nghiệm thu kết thúc; giữ bản ghi quyết định đã tối giản. Thời điểm xóa cụ thể được xác nhận với nhóm trước khi nhận dữ liệu thật.
- Storage Rules và Firestore Rules là hai lớp riêng, cần kiểm thử cả hai. Không tuyên bố Firestore tự xác nhận một file tồn tại/đúng nội dung. 

## 18. Tiêu chí nghiệm thu và kiểm thử

Các dòng dưới là ca kiểm thử cần thực hiện, chưa phải kết quả đã chạy trên ứng dụng.

| Mã  | Kịch bản                                                                                  | Kết quả mong đợi                                                                                       |
| --- | ----------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| T01 | OTP thật gửi đến số thật                                                                  | Nhận mã, xác minh thành công, ghi đúng số                                                              |
| T02 | Sai/hết hạn mã, gửi lại nhiều                                                             | Không đăng nhập sai; có thông báo và giới hạn                                                          |
| T03 | Số test trong phát triển                                                                  | Luồng chạy nhưng không báo đó là OTP thật                                                              |
| T04 | Google và phone của cùng người                                                            | Liên kết đúng UID, không mất hồ sơ/đơn                                                                 |
| T05 | Credential đã thuộc tài khoản khác                                                        | Không tự gộp hoặc chiếm tài khoản                                                                      |
| T06 | Client cố tự đặt admin/verified                                                           | Bị từ chối tại Firebase Rules                                                                          |
| T07 | SEEKER được Admin duyệt hồ sơ nghỉ hưu                                                    | APPROVED đúng version, có thông báo; đủ điều kiện ứng tuyển khi OTP/hồ sơ đủ, không tuyên bố eKYC thật |
| T08 | Quay lại/xoay ở bước 3 tìm việc                                                           | Giữ câu trả lời và bước hiện tại                                                                       |
| T09 | Không có tin đáp ứng điều kiện tránh                                                      | Không tự nới; có thông báo và cách sửa                                                                 |
| T10 | Chọn giờ rảnh không khớp                                                                  | Không hiển thị “khớp lịch” sai                                                                         |
| T11 | TTS đang đọc rồi xoay                                                                     | Không đọc chồng; có dừng/nghe lại                                                                      |
| T12 | Font 200%, điện thoại ngang, bàn phím                                                     | Chữ và nút chính vẫn tiếp cận được                                                                     |
| T13 | Tablet và chia đôi cửa sổ                                                                 | Bố cục thay đổi theo vùng ứng dụng                                                                     |
| T14 | Tin chưa duyệt                                                                            | Khách không đọc được bản riêng/chưa công khai                                                          |
| T15 | Sửa tin sau khi có đơn                                                                    | Chờ duyệt lại; snapshot đơn cũ không đổi                                                               |
| T16 | Hai lần bấm ứng tuyển                                                                     | Một đơn; request retry không tạo thêm                                                                  |
| T17 | Rút/chấp nhận đồng thời                                                                   | Chỉ một trạng thái cuối hợp lệ                                                                         |
| T18 | Chưa đủ mức xác minh, bấm xác nhận ca                                                     | Chuyển hướng giải thích, không bắt đầu giao dịch                                                       |
| T19 | Xác nhận version cũ của thỏa thuận                                                        | Bị từ chối; yêu cầu xem lại version mới                                                                |
| T20 | Ca 240.000đ, phí 10%                                                                      | Tổng 264.000đ; người làm 240.000đ                                                                      |
| T21 | Cấp tiền bấm hai lần/timeout                                                              | Một funding thành công; tra cứu operation                                                              |
| T22 | Chưa đủ tiền, bấm bắt đầu                                                                 | Không chuyển IN_PROGRESS                                                                               |
| T23 | Chỉ một bên xác nhận bắt đầu                                                              | Giữ READY và chỉ dẫn cần xác nhận/hỗ trợ                                                               |
| T24 | Hủy trước làm có đồng ý                                                                   | Hoàn 264.000đ, phí 0                                                                                   |
| T25 | Funding và hủy cùng lúc                                                                   | Không mất tiền, không ca hủy có tiền bị bỏ quên                                                        |
| T26 | Quyết toán đầy đủ                                                                         | 240.000 + 24.000 + 0 + 0 = 264.000                                                                     |
| T27 | Tranh chấp duyệt 120.000đ                                                                 | Chi 120.000, phí 12.000, hoàn 132.000                                                                  |
| T28 | Settlement lỗi rồi thử lại                                                                | Không chi hoặc ghi nhận phí hai lần                                                                    |
| T29 | Thay policy sau khi chốt ca                                                               | Ca cũ giữ rate/phiên bản đã xác nhận                                                                   |
| T30 | Người thuê không phản hồi 24h                                                             | Mở/tải lại hàng đợi có ca quá hạn; Admin xử lý, không tự trả tiền; không hứa chuyển nền khi app đóng   |
| T31 | Người ngoài mở ca/đơn/hồ sơ nghỉ hưu/ảnh Storage                                          | Rules từ chối; người thuê không thấy căn cước hay bằng chứng của ứng viên                              |
| T32 | Xem tin online rồi ngắt mạng                                                              | Đọc SQLite và có nhãn dữ liệu cũ                                                                       |
| T33 | Offline bấm ứng tuyển/thanh toán                                                          | Không tạo thành công giả, không tự gửi sau                                                             |
| T34 | Đăng xuất/chuyển tài khoản                                                                | Không lộ cache/ảnh/snapshot của tài khoản cũ                                                           |
| T35 | Tin bị ẩn khi ca đã có tiền                                                               | Chặn giao dịch mới; ca/tiền cũ còn đường xử lý                                                         |
| T36 | Admin xem thống kê                                                                        | Không cộng tiền giữ thành doanh thu                                                                    |
| T37 | Hồ sơ NOT_SUBMITTED/SUBMITTED/NEEDS_INFO/REJECTED/REVERIFY_REQUIRED gọi ghi đơn trực tiếp | Firebase từ chối dù sửa APK hoặc bỏ qua UI                                                             |
| T38 | Quét QR hợp lệ từ ảnh chụp căn cước                                                       | Trích đúng thông tin trong định dạng được hỗ trợ, xem lại được; không tự APPROVED                      |
| T39 | QR lạ/thiếu ngày sinh/ảnh mờ                                                              | Không đoán thông tin, không crash; hướng dẫn chụp lại/hỗ trợ                                           |
| T40 | Từ chối camera; xoay/đóng rồi mở màn upload                                               | Không mất form ngoài dự kiến, không upload/gửi/duyệt lặp                                               |
| T41 | Thiếu ảnh bằng chứng hoặc upload lỗi                                                      | Không chuyển SUBMITTED và không báo đã gửi                                                             |
| T42 | Nhóm dùng ảnh CCCD hiện có và ảnh BHXH tượng trưng                                        | Gửi/xem được theo quyền, không bắt watermark; quy trình mô tả tra cứu mô phỏng                         |
| T43 | Hai Admin duyệt cùng bản hoặc Admin duyệt bản cũ                                          | Một quyết định hiệu lực; không ghi đè version mới; có lý do và audit                                   |
| T44 | Bổ sung hồ sơ sau NEEDS_INFO                                                              | Tạo request mới, giữ bản cũ, vẫn chặn ứng tuyển đến khi APPROVED                                       |
| T45 | Thay định danh sau APPROVED                                                               | Quyết định cũ không mở đơn mới; không xóa lịch sử/ca/tiền                                              |
| T46 | Quyết định duyệt gặp timeout hoặc client gửi lặp                                          | Trạng thái, audit, notification nhất quán; không thông báo trùng                                       |
| T47 | App đóng lúc Admin duyệt                                                                  | Mở lại thấy kết quả đã lưu; không tuyên bố đã push/SMS                                                 |
| T48 | Xem tin khi chờ duyệt                                                                     | Vẫn xem/tìm việc; bấm ứng tuyển mở trạng thái hồ sơ, không tự tạo đơn sau duyệt                        |
| T49 | Người thuê tạo tin và chốt ca                                                             | Không bị yêu cầu giấy nghỉ hưu; giữ điều kiện danh tính mẫu v2                                         |
| T50 | Client đổi số tiền/APPROVED/Admin/ảnh sau gửi                                             | Rules từ chối đúng nơi; không chỉ dựa vào nút bị khóa                                                  |
| T51 | Đăng xuất/chuyển tài khoản trong upload hoặc xem ảnh                                      | Không hiện/ghi ảnh hoặc kết quả vào UID mới; dọn file tạm                                              |
| T52 | Đủ 60 tuổi hoặc ảnh BHYT có HT nhưng chưa được duyệt                                      | Không có nhánh tự mở quyền; bằng chứng đúng luồng mới là chứng nhận nghỉ hưu/BHXH mô phỏng             |

**Thiết bị kiểm thử tối thiểu đề xuất:** emulator/thiết bị API 29 và API 36; một điện thoại nhỏ, một điện thoại lớn và một tablet; mỗi loại dọc/ngang; font mặc định và 200%; online/offline; một thiết bị thật nhận SMS. Các trường hợp tiền/quyền/đồng thời cần kiểm thử tự động trên môi trường phù hợp và bổ sung kiểm thử đầu-cuối; không chỉ chụp màn hình thành công.

**Thử nghiệm khả năng sử dụng:** mời một số người thuộc nhóm tuổi mục tiêu thực hiện tìm một việc, ứng tuyển và xem tiền công; ghi số lần cần trợ giúp, lỗi thao tác, bước gây khó hiểu và nhận xét. Chưa đặt số liệu kết quả giả định thành kết quả thật; tiêu chí ban đầu nên điều chỉnh sau thử nghiệm.

## 19. Truy vết yêu cầu — chức năng — lớp — kiểm thử

| Yêu cầu                        | Use case              | Lớp/chức năng chính                                             | Kiểm thử                         |
| ------------------------------ | --------------------- | --------------------------------------------------------------- | -------------------------------- |
| Xác minh số thật               | UC01–UC02             | User, AuthRepository                                            | T01–T06                          |
| Duyệt nghỉ hưu trước ứng tuyển | UC04–UC05, UC09       | RetirementVerificationRequest, User, Storage, InAppNotification | T07, T37–T48, T50–T52            |
| Danh tính mẫu người thuê       | UC04–UC05, UC17–UC18  | IdentityVerificationRequest                                     | T18, T49                         |
| Tìm việc dễ dùng               | UC06–UC07             | SearchPreference, JobPost, MatchingService                      | T08–T10                          |
| Giọng đọc, responsive          | UC28 và phi chức năng | AccessibilitySettings, ViewModel                                | T11–T13                          |
| Tin có kiểm duyệt              | UC11–UC13             | JobPost, Report                                                 | T14–T15, T35                     |
| Ứng tuyển nhất quán            | UC09–UC10, UC14       | Application                                                     | T16–T17                          |
| Thỏa thuận theo ca             | UC17–UC18             | WorkSession, AgreementSnapshot                                  | T18–T19                          |
| Tiền và phí duy trì            | UC19, UC26            | PaymentRecord, PaymentEvent, FeePolicy                          | T20–T29, T36                     |
| Hoàn thành/tranh chấp          | UC20–UC25             | WorkSession, Dispute                                            | T23–T30                          |
| Phân quyền và riêng tư         | Tất cả UC riêng       | Firestore/Storage Rules, AuditEvent                             | T06, T31, T34, T37, T43, T50–T51 |
| SQLite ngoại tuyến             | UC29                  | CachedJob, LocalSearchPreference                                | T32–T34                          |

## 20. Phạm vi còn mở và nguyên tắc thay đổi

1. **Phạm vi xác minh đã chốt:** OTP SMS thật; chụp căn cước/đọc QR; chứng nhận nghỉ hưu hoặc BHXH mô phỏng; Admin tra cứu mô phỏng và duyệt trước ứng tuyển. Chưa có đối chiếu danh tính pháp lý với cơ quan nhà nước.
2. **Mẫu báo cáo chính thức:** chưa được cung cấp. Tài liệu này cung cấp nội dung, chưa khẳng định đúng mẫu PDF của môn học.
3. **Tham số đề xuất:** điểm gợi ý, 24h phản hồi, mục tiêu admin 48h, giới hạn tin xử lý và mốc bắt đầu ca cần kiểm chứng trước nghiệm thu.
4. **Mức phí 10%:** là quyết định sản phẩm đã chốt để mô phỏng, không phải kết quả nghiên cứu giá hay lợi nhuận thực tế.
5. **Kiến trúc môn học:** dùng Java/Firebase trực tiếp như mục 14, không triển khai TypeScript/Cloud Functions. Phải kiểm chứng Rules cho quyền, lịch và transaction; chưa có kết quả kiểm thử để khẳng định đã bảo vệ được mọi bất biến.
6. **Ca dài hạn:** có thể tạo nhiều ca từ một đơn ACCEPTED, nhưng chưa có lịch lặp tự động hoặc quyết toán gộp tháng.
7. **Nội dung tin và nhận định phù hợp:** dựa trên khai báo người đăng, không phải đánh giá y tế, chứng nhận nghề hay bảo đảm chất lượng.

Mọi thay đổi nghiệp vụ phải cập nhật đồng thời: yêu cầu, use case, trạng thái, lớp/dữ liệu, quyền và kiểm thử. Không quay lại bản chỉ có đơn ứng tuyển khi giải thích thanh toán; một ca là đơn vị thỏa thuận và quyết toán.

## 21. Nguồn tham khảo chính thức

Các nguồn dùng để kiểm chứng khả năng kỹ thuật và cách thu phí; quyết định thiết kế riêng của GrandMa Assist đã được nêu rõ. Nguồn nền của v2 được giữ để đối chiếu; các tài liệu Rules, Storage, OTP và QR được rà soát bổ sung ngày 18/10/2026. Các mức giá/phí dịch vụ bên ngoài không phải báo giá hiện tại của đồ án.

- **[S1] Firebase Phone Authentication:** OTP, giới hạn xác thực bằng số điện thoại, số kiểm thử và sự đồng ý khi dùng dịch vụ. https://firebase.google.com/docs/auth/android/phone-auth
- **[S2] Firebase Authentication Limits:** SMS xác thực thật thuộc gói Blaze và có giới hạn. https://firebase.google.com/docs/auth/limits
- **[S3] Firestore offline:** cấu hình lưu trữ ngoại tuyến; cần phân biệt với SQLite do nhóm thiết kế. https://firebase.google.com/docs/firestore/manage-data/enable-offline
- **[S4] Điều kiện Firestore Rules:** kiểm tra Auth, tài liệu liên quan và getAfter. https://firebase.google.com/docs/firestore/security/rules-conditions
- **[S5] Android responsive/adaptive Views:** bố cục theo kích thước và vùng hiển thị. https://developer.android.com/develop/ui/views/layout/responsive-adaptive-design-with-views
- **[S6] W3C biểu mẫu nhiều bước:** nhóm nội dung, tiến trình, bước tùy chọn. https://www.w3.org/WAI/tutorials/forms/multi-page/
- **[S7] W3C độ tương phản:** nguyên tắc tương phản chữ/nền. https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html
- **[S8] Android TextToSpeech và Voice:** https://developer.android.com/reference/android/speech/tts/TextToSpeech ; https://developer.android.com/reference/android/speech/tts/Voice
- **[S9] bTaskee, điều khoản đối tác, Phụ lục 2:** https://www.btaskee.com/vi-VN/bien-ban-hop-tac-doi-tac/
- **[S10] Taskrabbit, các khoản phí trên hóa đơn:** https://support.taskrabbit.com/hc/en-us/articles/46260407116955-I-d-Like-To-Understand-The-Fees-On-My-Task-s-Invoice
- **[S11] Airtasker Úc, phí người làm và người thuê:** https://support.airtasker.com/hc/en-au/articles/200294499-What-is-the-service-fee ; https://support.airtasker.com/hc/en-au/articles/360031769372-What-is-the-Connection-Fee
- **[S12] Firestore transactions:** https://firebase.google.com/docs/firestore/manage-data/transactions
- **[S13] Firestore Security Rules:** https://firebase.google.com/docs/firestore/security/get-started
- **[S14] Firebase liên kết phương thức đăng nhập:** https://firebase.google.com/docs/auth/android/account-linking
- **[S15] Android cấu hình SDK:** https://developer.android.com/build

- **[S16] Storage Security Rules:** bảo vệ file và metadata. https://firebase.google.com/docs/storage/security
- **[S17] ML Kit Barcode Scanning Android:** đọc QR từ ảnh, có ví dụ Java. https://developers.google.com/ml-kit/vision/barcode-scanning/android
- **[S18] Firebase Storage và billing:** yêu cầu Blaze. https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024
- **[S19] Bộ Công an — đọc QR CCCD:** dữ liệu trích xuất trên mẫu CCCD gắn chip. https://bocongan.gov.vn/bai-viet/tao-dieu-kien-thuan-loi-giam-phien-ha-cho-cong-dan-trong-thuc-hien-cac-thu-tuc-hanh-chinh-giao-dich-dan-su-d3-t801

## 22. Giao diện, trình tự triển khai và điều kiện bàn giao

### 22.1. Căn cứ Figma và phần cần bổ sung

File GRANDMA-ASSIST, Page 1 là nguồn giao diện. Page 2 và 3 trống theo xác nhận người dùng. Bản đối chiếu đã kiểm kê 56 màn hình/trạng thái; không có nghĩa mọi luồng đã đủ. Bản v3 này cập nhật tài liệu và sơ đồ cục bộ, **chưa sửa Figma hoặc code app**.

| Khu vực           | Giữ / bổ sung                                                                                                                |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| Nhận diện         | Giữ màu xanh, nền sáng, chữ lớn, font và bố cục tham chiếu Figma; kiểm tra tương phản khi triển khai                         |
| Hồ sơ SEEKER      | Thay ngày sinh tự chọn làm căn cứ duyệt bằng thông tin đọc QR; thêm chụp ảnh, xem lại, bằng chứng, tiến độ, bổ sung, kết quả |
| Admin hồ sơ       | Tách nghỉ hưu SEEKER với danh tính mẫu EMPLOYER; thêm xem ảnh, tra cứu mô phỏng, lý do, version và lịch sử                   |
| Nút ứng tuyển     | Trước APPROVED dẫn về hồ sơ; sau APPROVED mở xác nhận ứng tuyển; không gửi tự động                                           |
| Tìm việc          | Giữ bốn bước; dùng khu vực, bỏ số km/GPS và phần trăm phù hợp không có cơ sở                                                 |
| Tiền              | Giữ phí 10% người thuê; mọi màn hình tiền ghi mô phỏng, không sử dụng tiền thật                                              |
| Ca người tìm việc | Bổ sung các trạng thái đơn/thỏa thuận/bắt đầu/hoàn thành/hủy/tranh chấp còn thiếu theo v2                                    |
| Responsive        | Dọc/ngang, chữ 200%, bàn phím, tablet; không sao chép thanh trạng thái hệ điều hành từ Figma                                 |
| Lỗi và trạng thái | Loading, rỗng, mất mạng, từ chối quyền camera, thiếu QR, upload lỗi, xung đột phiên bản và không đủ quyền                    |

Không triển khai nội dung Figma thừa so với nghiệp vụ như quảng cáo bảo hiểm, MFA chưa làm, chat hoặc tính khoảng cách. Màn hình bổ sung phải được duyệt bố cục trước khi code chức năng tương ứng; duyệt tài liệu này không đồng nghĩa đã duyệt các hình chưa được tạo.

### 22.2. Các chức năng theo thứ tự dự kiến

Mỗi dòng là một đơn vị chức năng có thể nghiệm thu và commit, không phải nút giả hoặc chỉ dựng khung. Có thể gộp các dòng phụ thuộc khi cần để không để luồng bị bế tắc; không cam kết số commit cố định.

| Mã  | Chức năng hoàn chỉnh                                   | Điều kiện hoàn tất nổi bật                                                                                                         |
| --- | ------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------- |
| C01 | Google, phục hồi phiên và đăng xuất                    | Tài khoản thật, hủy/lỗi/mạng, màn tài khoản sau login hoạt động                                                                    |
| C02 | Đăng nhập điện thoại/OTP                               | SMS thật khi nghiệm thu, sai/hết hạn/gửi lại, xoay màn hình                                                                        |
| C03 | Liên kết Google/phone và đổi số                        | Giữ UID, không chiếm tài khoản, Rules dựa Auth                                                                                     |
| C04 | Vai trò cố định và hồ sơ                               | SEEKER/EMPLOYER riêng, Admin cấp sẵn; giữ form khi xoay                                                                            |
| C05 | Hồ sơ nghỉ hưu từ chụp QR đến Admin duyệt và thông báo | Đủ gửi/bổ sung/xét/kết quả, Storage riêng và chặn ứng tuyển; gồm danh tính mẫu EMPLOYER hoặc tách thành chức năng hoàn chỉnh riêng |
| C06 | Nháp tin tuyển và xem trước                            | Lưu nháp cục bộ, sửa, kiểm tra dữ liệu                                                                                             |
| C07 | Gửi/duyệt tin tuyển                                    | Trạng thái và lý do thật; tin chưa duyệt không công khai                                                                           |
| C08 | Xem/lọc/phân trang/chi tiết việc và cache SQLite       | Khách xem được; tin đã xem mở lại khi mất mạng                                                                                     |
| C09 | Bốn bước tìm việc có hướng dẫn                         | Giữ/sửa lựa chọn, giải thích kết quả, lựa chọn local                                                                               |
| C10 | Lưu/bỏ lưu việc                                        | Đúng UID, không trùng, trạng thái tin đóng rõ ràng                                                                                 |
| C11 | Sửa/đóng/ẩn tin                                        | Duyệt lại khi sửa, giữ lịch sử                                                                                                     |
| C12 | Gửi/xem/rút đơn                                        | APPROVED hiện hành trước gửi, transaction chống trùng                                                                              |
| C13 | Xử lý ứng viên và liên hệ                              | Đúng chủ tin, xử lý cạnh tranh với rút đơn                                                                                         |
| C14 | Báo cáo tin và Admin xử lý                             | Không lộ người báo; phân biệt tranh chấp ca                                                                                        |
| C15 | Thỏa thuận hai bên                                     | Cùng version, lịch hợp lệ, điều kiện hồ sơ đúng vai trò                                                                            |
| C16 | Cấp/giữ tiền mô phỏng                                  | Rules kiểm tra số và trạng thái, retry an toàn                                                                                     |
| C17 | Ca và xác nhận bắt đầu                                 | Hai bên, đủ tiền, đúng khoảng thời gian                                                                                            |
| C18 | Báo xong/xác nhận/quyết toán/lịch sử                   | Số tiền bảo toàn, một lần áp dụng                                                                                                  |
| C19 | Hủy và giải quyết tranh chấp đầy đủ                    | Đồng ý hủy hoặc Admin phân xử; có đường xử lý tiền khép kín                                                                        |
| C20 | Nhắc việc/quá hạn trong app                            | Tải lại thấy việc cần xử lý, không tự chi tiền                                                                                     |
| C21 | Thống kê Admin                                         | Tách tiền giữ, tiền chi, hoàn, phí; đối chiếu ca                                                                                   |
| C22 | Cài đặt và trợ giúp/giọng đọc                          | Bật/tắt, dừng/nghe lại, không đọc OTP hoặc đọc chồng                                                                               |

Responsive và offline được đưa vào từng chức năng liên quan, không chờ cuối dự án. Nếu hiện nút nghe ở chức năng sớm thì phải có TTS hoạt động ngay; không để nút chờ C22. C15–C18 có ngay màn xem kết quả/ca cần thiết, không chờ một “trang tổng” chưa làm.

### 22.3. Quy trình làm việc sau khi được duyệt

Codex có thể sửa cùng thư mục dự án mà Android Studio đang mở; Android Studio dùng build/run, thiết bị, Logcat và kiểm tra giao diện. Chỉ một agent sửa code tại một thời điểm. Mỗi chức năng: chốt màn hình và tiêu chí → code Java/XML + dữ liệu/Rules → kiểm thử phù hợp → review diff và kết quả → một commit có nội dung rõ. Không tự push/merge.

Báo cáo bàn giao phải ghi file thay đổi, hành vi, kiểm tra đã chạy và kết quả thật, ảnh màn hình không lộ giấy tờ, giới hạn còn tồn tại. Không gọi hoàn tất nếu chỉ build được mà chưa thử luồng chính/lỗi/quyền liên quan. Không tạo commit chức năng trong lần cập nhật thiết kế này.

### 22.4. Trạng thái phê duyệt

**Chờ duyệt bản v3.** Quyết định “duyệt trước ứng tuyển” đã chốt; không hỏi lại. Người dùng duyệt tài liệu hoặc ghi các điểm cần sửa, rồi mới tiến hành chức năng đầu tiên. Bản v2 giữ nguyên để đối chiếu; bản v3 chưa phải xác nhận hệ thống đã triển khai.

## Phụ lục A. Cách sử dụng bộ tài liệu

- Bản HTML trình bày cùng nội dung, có mục lục và các sơ đồ tổng quan để đọc nhanh; có thể mở trong trình duyệt và in.
- Bản Markdown là nguồn để chỉnh sửa, chuyển sang Word/PDF theo mẫu báo cáo sau này.
- Thư mục diagrams chứa Mermaid cho các sơ đồ có trong tài liệu, PlantUML cho use case đầy đủ và nguồn DOT/SVG của hình minh họa. Nguồn UML là cơ sở để nhóm chỉnh trong công cụ vẽ, không phải ảnh chụp từ ứng dụng.
- Class diagram được tách thành hai phần để đọc rõ. Sơ đồ use case minh họa trong HTML là bản tổng quan; danh mục UC01–UC30 và nguồn PlantUML là bản đầy đủ.
- Các bảng kiểm thử là kế hoạch nghiệm thu, không phải báo cáo đã kiểm thử. Chưa có mã ứng dụng nào được triển khai trong lần soạn tài liệu này.
