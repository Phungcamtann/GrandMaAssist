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
