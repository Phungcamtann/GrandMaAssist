# C01 — người dùng kiểm tra trong Android Studio

**Người dùng đã xác nhận trên bản trước chỉnh giao diện:** đăng nhập Google, đăng xuất, hủy đăng nhập và khôi phục phiên thành công. Đây là xác nhận cho bốn trường hợp chung; chưa có xác nhận chi tiết cho các biến thể lỗi/mạng/vòng đời trong bảng dưới. Không suy ra đã đạt đăng nhập lần đầu với tài khoản mới, đổi tài khoản, Back sau đăng xuất, xoay, font lớn hoặc lỗi dọn credential.

**Cập nhật:** người dùng đã duyệt giao diện và xác nhận bản sửa dùng worker hết treo khi đăng xuất. Logcat 23/10/2026 lúc 23:41:03 ghi gọi trên worker → lời gọi trả về → `provider confirmed` trên main, tổng khoảng 51 ms. Ca đăng xuất bình thường với provider xác nhận đã được người dùng kiểm tra thành công; không phải kết quả nhánh timeout. Bản timeout trước đó vẫn treo và đã được thay thế. Codex không chạy build/test/lint/emulator; các biến thể khác trong bảng dưới không tự được đánh dấu đạt.

## Kiểm tra lại bản sửa chờ đăng xuất

1. **Người dùng đã xác nhận đạt:** Run bản mới, đăng nhập rồi đăng xuất; hết treo, Logcat có `provider confirmed` ngày 23/10/2026 lúc 23:41:03.770.
2. Nếu provider tiếp tục không phản hồi như bản cũ: sau khoảng 10 giây khi app hoạt động, vòng quay phải dừng, nút Google mở lại và thông báo chỉ xác nhận **Firebase đã đăng xuất, Google chưa xác nhận dọn phiên**. Không yêu cầu đóng app để thoát trạng thái chờ.
3. Nhấn **Thử lại bước dọn phiên Google**: mỗi lần thử đều có cùng giới hạn chờ, không bị khóa mãi.
4. Sau timeout, nhấn Google và đăng nhập lại; callback dọn phiên cũ về muộn không đổi màn hình hoặc trạng thái tài khoản mới.
5. Xoay trong lúc đang dọn phiên: không khởi tạo lần dọn mới hoặc bắt đầu lại mốc chờ; kiểm tra Back/mở lại sau đăng xuất không phục hồi tài khoản cũ. Đây là các ca chờ người dùng kiểm tra, chưa có kết quả bản sửa.
6. Nếu vẫn treo, lấy các dòng Logcat với tag `GrandMaAuth` từ lúc đăng xuất đến sau 15 giây. Các mốc cố định trong tag này không ghi dữ liệu tài khoản/credential; không gửi toàn bộ Logcat chưa lọc. Chưa xác minh bản mới chỉ từ việc review source.

## Chuẩn bị và Sync/Run

1. Mở project gốc trong Android Studio, lưu công việc đang có; giữ nguyên thay đổi `.idea` riêng.
2. Sync Project with Gradle Files; chờ tải Credentials 1.6.0 và Google ID 1.2.1. Build/Run từ Android Studio theo cách thông thường.
3. Chọn thiết bị/emulator có Google Play services, mạng và tài khoản Google. Đối chiếu cấu hình Google provider/package/SHA-1/Web client tại [SETUP](SETUP.md).
4. Nếu build lỗi, ghi lỗi đầu tiên trong Build Output. Không gọi đạt chỉ vì Sync hoặc build thành công.

## Luồng chính và phiên

| Tình huống | Kết quả cần thấy |
| --- | --- |
| Cài/mở khi chưa có phiên | Logo gốc, chào và nút Google; không tự mở bộ chọn; không có OTP/TTS/nút tương lai |
| Google lần đầu với tài khoản chưa cấp quyền | Chọn/thêm tài khoản và đồng ý; chỉ sau Firebase thành công mới vào tài khoản; Console Auth có user tương ứng |
| Google với tài khoản đã dùng | Đăng nhập được, thông tin đúng tài khoản vừa chọn |
| Bấm Google nhiều lần nhanh | Một yêu cầu, nút tạm khóa và có trạng thái đang xử lý |
| Hủy bộ chọn bằng Back/đóng | Về đăng nhập, nút dùng lại được, không báo đăng nhập thành công |
| Thử lại sau hủy/lỗi | Bộ chọn mở lại từ nút, không cần khởi động lại app |
| Đóng app, mở lại khi có mạng | Vào tài khoản từ phiên SDK; trạng thái chuyển sang đã kiểm tra với máy chủ sau khi kiểm tra thành công |
| Mất mạng trước khi mở lại với phiên đã lưu | Không tự kết luận hết phiên; tài khoản vẫn hiển thị với thông báo chưa kiểm tra server và nút kiểm tra lại |
| Có mạng lại, Kiểm tra lại phiên | Chuyển trạng thái xử lý rồi cập nhật kết quả, không tạo đăng nhập mới |
| Tài khoản bị disable/xóa hoặc refresh token bị thu hồi theo cấu hình thử của người dùng | Khi kiểm tra được với Firebase, phiên không hợp lệ quay về đăng nhập; lỗi mạng đơn thuần không bị xem là disable |
| Tên/email thiếu (nếu có tài khoản cung cấp thiếu) | Thông báo thay thế, không hiện giá trị mẫu hoặc `null` |

## Lỗi và vòng đời

| Tình huống | Kết quả cần thấy |
| --- | --- |
| Không có tài khoản trên thiết bị | Button flow cho thêm tài khoản nếu provider hỗ trợ; nếu SDK báo không có credential thì UI giải thích và cho thử lại |
| Tắt mạng khi đăng nhập | Không báo thành công giả; nhận lỗi từ provider/Firebase, nút trở lại để thử lại sau khi có mạng |
| Provider/Google Play services không sẵn sàng | Thông báo có thể hiểu được, không crash; SDK chỉ trả lỗi chung thì app không khẳng định chắc lỗi cấu hình/mạng |
| Credential/cấu hình lỗi hoặc bị giới hạn yêu cầu (khi có điều kiện tái hiện) | Thông báo đúng mức SDK cung cấp, không hiện stack trace/token |
| Xoay khi bộ chọn Google còn mở | Yêu cầu của Activity cũ bị hủy hoặc kết quả đã được chấp nhận chuyển sang Firebase; không mở thêm bộ chọn, không kẹt nút; nếu gián đoạn có thể thử lại |
| Xoay khi đang trao đổi với Firebase | Công việc tiếp tục qua repository, không gửi hai lần; kết quả đến Activity mới |
| Xoay khi đã vào tài khoản/đang kiểm tra phiên | Đúng tài khoản, không tự mở bộ chọn; kiểm tra đang chạy không bị nhân đôi |
| Home rồi quay lại/đóng mở khi tác vụ đang chạy | Không crash hoặc hiển thị tài khoản mẫu; khôi phục theo Firebase, không dựa token do app tự lưu |

Một số lỗi provider/clearCredentialState phụ thuộc SDK/thiết bị và có thể khó tái hiện thủ công; ghi **chưa xác minh** nếu không gây được lỗi, không đánh dấu đạt suy diễn.

## Đăng xuất và đổi tài khoản

1. Đăng xuất khi có mạng: thông tin tên/email cũ biến mất, về đăng nhập và báo kết quả; Back không mở tài khoản cũ.
2. Đăng xuất khi đang kiểm tra phiên hoặc khi offline: vẫn xóa phiên Firebase cục bộ; kết quả kiểm tra cũ không dựng lại màn tài khoản. Nếu dọn credential lỗi, thông báo rõ Firebase đã đăng xuất và có **Thử lại bước dọn phiên Google**.
3. Khi tái hiện được lỗi dọn credential, nhấn thử lại: có tiến trình và kết quả đúng; không gọi xóa Firebase user hoặc thu hồi quyền Google.
4. Sau đăng xuất, chọn Google khác: hiển thị đúng tài khoản mới, không lộ tên/email cũ. Có thể thêm tài khoản nếu thiết bị/provider cho phép.
5. Đăng xuất rồi đóng/mở lại: không tự quay về tài khoản cũ. Không phát sinh ghi Firestore, upload hoặc thay Rules.

## Giao diện và khả năng tiếp cận

- Run bản giao diện mới: thanh gradient nằm sát dưới status bar hệ thống, logo nhỏ trên vùng trắng bên trái; không còn logo lớn hoặc thanh giờ/pin/sóng vẽ trong app.
- Màn đăng nhập có banner mới người dùng cung cấp, giữ toàn ảnh và chữ; không kéo méo hoặc cắt chữ. Đây là ảnh minh họa, không có hành động cho các nút vẽ sẵn bên trong ảnh. Màn tài khoản dùng cùng thanh thương hiệu, không đổi tên/email/phương thức hoặc thao tác đăng xuất.
- Tiêu đề “Chào cô/chú!” và “Tài khoản của cô/chú” hiển thị rõ nét Bold Plus Jakarta Sans 700; phần giới thiệu và Google nằm ngay dưới, căn trái/đệm ngang khoảng 20dp. Nền ngà giữ màu đã thống nhất.
- Điện thoại dọc/ngang, tablet/chia đôi cửa sổ; nội dung căn giữa với chiều rộng hợp lý, cuộn tới toàn bộ nút và trạng thái.
- Font hệ thống mặc định và 200%: không cắt tiêu đề, nhãn nút, tên/email dài hoặc lỗi; không dùng ellipsis để giấu thông tin cần đọc.
- System light/dark đều giữ nền/chữ sáng nhất quán; thanh hệ thống/cutout không đè nội dung. Có thể kiểm tra vùng IME khi hệ thống hiển thị bàn phím của bộ chọn.
- TalkBack: đọc tiêu đề, nhãn/tên/email/phương thức và nút theo thứ tự; logo trang trí không đọc lặp; trạng thái thay đổi được thông báo.
- Nút Google có G nguyên bản, không kéo giãn/tô màu lại; logo GrandMa đúng tỷ lệ/nền, không cắt G/lá.
- Kiểm tra launcher/splash với mask tròn, squircle của thiết bị; icon GrandMa hiện đúng, không còn robot, G/lá không bị cắt. Bộ themed-icon có thể do launcher xử lý vì app không cung cấp monochrome để tự vẽ lại logo.
- Không có giọng đọc/nút nghe trong C01; phần này thuộc C22.

## Ghi nhận kết quả

Ghi thiết bị/API, bước thực hiện, kết quả thật và lỗi đầu tiên. Ảnh bàn giao cần che dữ liệu tài khoản; không chia sẻ token, credential hoặc API key. Sau review và sửa lỗi phát sinh, người dùng tự stage/commit đúng [danh sách C01](C01-FILES.md), không lẫn `.idea` có sẵn; không chuyển C02 trong lần này.

## Danh sách 1 — Các nhánh chưa xác minh riêng, theo dõi sau C01

Người dùng đã xác nhận C01 hoạt động ổn và duyệt chức năng/giao diện. Các mục dưới là phần kiểm tra bổ sung còn mở, không phải lỗi đã được xác nhận và không được tự đánh dấu đạt. Không trì hoãn ghi nhận phê duyệt C01 vì chưa tái hiện được các điều kiện này. Người dùng thực hiện khi có thiết bị/điều kiện phù hợp; Codex không tự chạy kiểm thử.

| Mã | Nhánh/điều kiện | Kết quả cần kiểm tra | Trạng thái |
| --- | --- | --- | --- |
| C01-F01 | Provider dọn credential không phản hồi | Sau khoảng 10 giây khi app hoạt động, dừng tiến trình, mở lại thao tác; thông báo Firebase đã đăng xuất nhưng Google chưa xác nhận dọn phiên, không báo thành công giả | Chưa xác minh riêng |
| C01-F02 | Thử lại dọn credential sau lỗi/timeout | Có giới hạn chờ cho từng lần, nút không kẹt; kết quả phản ánh đúng callback hoặc timeout | Chưa xác minh riêng |
| C01-F03 | Callback dọn phiên cũ đến sau timeout và sau khi đăng nhập tài khoản mới | Không đổi màn hình, xóa trạng thái hoặc làm sai tài khoản mới do callback cũ | Chưa xác minh riêng |
| C01-F04 | Xoay/Home/quay lại trong lúc dọn phiên hoặc tác vụ Auth đang chạy | Không nhân đôi thao tác, không bắt đầu lại mốc chờ, không giữ Activity cũ, có đường thử lại nếu bị gián đoạn | Chưa xác minh riêng |
| C01-F05 | TalkBack | Thứ tự focus/đọc đúng; tiêu đề, nhãn và nút có nghĩa; logo không đọc lặp, thay đổi trạng thái được thông báo | Chưa xác minh riêng |
| C01-F06 | Font 200%, tên/email dài, màn hình ngang | Không cắt thông tin hoặc nút; cuộn tới mọi thao tác và thông báo | Chưa xác minh riêng |
| C01-F07 | Tablet, chia đôi cửa sổ, thiết bị/API khác | Bố cục có chiều rộng hợp lý, insets đúng; Auth và khôi phục phiên không bị ảnh hưởng | Chưa xác minh riêng |
| C01-F08 | Launcher/splash với các mask và themed icon khác nhau | Biểu tượng G/lá không bị cắt hoặc kéo méo, không hiện icon robot cũ | Chưa xác minh riêng |
| C01-F09 | Phiên Firebase bị disable/xóa/thu hồi, lỗi provider hoặc mất mạng đúng lúc Auth | Không báo đăng nhập thành công giả; phân biệt lỗi mạng với phiên không hợp lệ khi SDK cung cấp đủ thông tin, có đường phục hồi | Chưa xác minh riêng |

Khi kiểm tra một mục, ghi: mã mục, phiên bản/commit, thiết bị và API, điều kiện tái hiện, kết quả thật và bằng chứng đã che thông tin tài khoản. Nếu không tạo được điều kiện thì giữ “Chưa xác minh riêng”. Khi phát hiện lỗi, ghi lại và xử lý trước khi đánh dấu mục đó đạt.

Nghe hướng dẫn/TTS không thuộc danh sách tồn đọng C01: đã chuyển cả giao diện và âm thanh sang C22 theo quyết định của người dùng.

Quyết định bàn giao mới: người dùng dự định tự commit toàn bộ trạng thái dự án hiện tại sau khi xem diff; danh sách C01 vẫn dùng để truy vết phạm vi. Các thay đổi .idea đang có là cấu hình IDE riêng, không phải chức năng C01. Codex không stage, commit hoặc push.
