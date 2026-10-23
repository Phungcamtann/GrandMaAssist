# Bàn giao nền tảng — 23/10/2026

## Phạm vi
Android Java/XML; package vn.edu.ueh.nhom4.grandmaassist.app.
Firebase project grandmaassist-a1dfa; Storage bucket grandmaassist-a1dfa.firebasestorage.app.
Giữ Firebase BoM và các phiên bản build đang sử dụng; bổ sung Storage, bỏ Functions.
Lifecycle ViewModel/LiveData 2.10.0 được khai báo trực tiếp cho Java.

## Cấu trúc hiện tại
- MainActivity: màn hình kiểm tra kết nối, chỉ hiển thị trạng thái và nhận thao tác.
- viewmodel/ConnectionViewModel: giữ trạng thái qua xoay màn hình, chống tải trùng trong một yêu cầu.
- data/repository/AppConfigRepository: đọc appConfig/public từ server.
- GrandMaAssistApp: cấu hình Firestore cache chỉ trong bộ nhớ.
- firebase/: Rules và indexes được quản lý cùng mã nguồn.
- firebase.json, .firebaserc: đường dẫn cấu hình và project mặc định.

Các package ui theo chức năng, domain, data/local (SQLiteOpenHelper), verification và accessibility
sẽ được thêm cùng chức năng thực tế. Chưa tạo các lớp rỗng, schema nghiệp vụ hoặc màn hình giả.
Giao diện hiện tại là màn kết nối kỹ thuật; giao diện sản phẩm cần đối chiếu Figma khi làm từng luồng.
SQLite, đăng nhập, upload ảnh và các chức năng nghiệp vụ chưa được triển khai.

## Firebase Console
Người dùng đã chạy bản trước lần refactor này và gửi ảnh app đọc thành công thông báo
"GrandMaAssist kết nối Firebase mới thành công".
Ảnh Console xác nhận bucket đã tạo và Storage đang deny-all.
Bộ Rules trong repo phản ánh cấu hình đã cung cấp trong chat.
Không có lệnh deploy nào được chạy trong lần thay đổi này.
Auth Google/Phone đã được người dùng bật; chưa kiểm tra đăng nhập hay gửi SMS thật.
Firestore location được hướng dẫn Singapore; Storage được hướng dẫn US-EAST1,
cần đối chiếu Console nếu muốn xác nhận vị trí đã lưu.

## Người dùng kiểm tra trong Android Studio
1. Sync Project with Gradle Files.
2. Run app với mạng hoạt động: thông báo cấu hình từ Firebase mới phải xuất hiện.
3. Nhấn Tải lại; trong khi chờ, nút bị vô hiệu hóa.
4. Xoay màn hình khi đang tải và sau khi đã có kết quả: giữ trạng thái, nút không kẹt.
5. Tắt mạng rồi nhấn Tải lại: chờ yêu cầu kết thúc, phải có thông báo lỗi; bật mạng và thử lại.
6. Thử màn hình ngang/chữ lớn: nội dung cuộn được và nút vẫn tiếp cận được.
7. Không thử upload ảnh từ app ở bước này: chưa có luồng upload, Rules vẫn chặn.

Codex không chạy build, Sync, emulator, unit test hoặc kiểm thử Rules theo yêu cầu người dùng.
Các bước trên là danh sách chờ người dùng thực hiện, không phải kết quả đã đạt.
Nếu Sync/build báo lỗi, gửi lỗi đầu tiên trong Build Output để xử lý.

## Quy trình
Codex sửa code/cấu hình và commit từng luồng; người dùng Sync, chạy và kiểm tra trong Android Studio.
Không tự push hoặc merge. Các thay đổi .idea của người dùng không thuộc commit nền tảng.
Chưa đánh dấu toàn bộ sản phẩm hoặc các luồng Auth/Storage hoàn thành.
