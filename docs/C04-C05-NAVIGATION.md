# C04/C05 — bổ sung trang chính và điều hướng theo vai trò

25/10/2026 — C04 tại commit `fce2c26` là nền đã duyệt. Người dùng xác nhận C05 đọc QR/gửi, vào Admin/tải hàng đợi và test lại các ca sau bản sửa quyết định đều đạt; D01–D06 ghi nhận theo xác nhận người dùng. **Phần cải tiến mới gộp lối vào Hồ sơ/xác minh đang chờ nghiệm thu**; không tự xác nhận các nhánh/biến thể chưa tái hiện đã đạt.

## Đối chiếu phạm vi và thiết kế v3

README mục 16 có S05 (trang chính người tìm việc), S12 (tin của người thuê), S20 (hàng đợi Admin) và S21a (duyệt hồ sơ). Bản C04 đã bàn giao tập trung chọn vai trò/hồ sơ; yêu cầu C04 lúc đó còn giới hạn chưa dựng trang chủ/taskbar. Vì vậy bản trước đưa tài khoản đã có vai trò về form hồ sơ, Admin chỉ có khối tài khoản và nút mở duyệt, chưa có giao diện điều hướng như mẫu mới.

Yêu cầu bổ sung hiện hành: dùng bốn ảnh người dùng gửi làm tham khảo; topbar xanh luôn có logo bên trái, vai trò một dòng bên phải; có trang chính/thanh điều hướng theo từng vai trò và đường vào Admin rõ ràng. Người dùng đã chốt **giữ vị trí mục chưa hoạt động, khóa và ghi “Sắp có”**. Không dùng các tên DEMO hoặc số đếm trong ảnh làm dữ liệu thật. Không sửa Figma.

## Các màn và đường đi

| Vai trò | Sau đăng nhập/khôi phục phiên hoặc chọn vai trò thành công | Thanh điều hướng |
|---|---|---|
| SEEKER | Trang chủ, lời chào và các thẻ việc theo mẫu; Xem hồ sơ vào trang Hồ sơ thống nhất | Trang chủ; Việc của tôi — Sắp có; Việc khác — Sắp có; Hồ sơ |
| EMPLOYER | Trang chủ người thuê, đường vào Hồ sơ thống nhất | Trang chủ; Tin của tôi — Sắp có; Ca làm — Sắp có; Tài khoản |
| ADMIN | Tổng quan hàng đợi, thẻ Kiểm tra hồ sơ với số liệu server | Tổng quan; Xử lý; Thống kê — Sắp có; Tài khoản |

Tài khoản chưa có vai trò vẫn ở màn chọn vai trò, phải chọn và xác nhận mới tiếp tục. Vai trò cố định và grant Admin giữ nguyên. Có **Kiểm tra lại tài khoản** tại màn chọn vai trò để đọc lại quyền vừa được quản lý cấp. Nếu grant và User ADMIN không khớp, app báo chưa được cấp đủ quyền, không tự chuyển thành Admin hoặc mở trình chọn vai trò để thay quyền.

Người thuê → Tài khoản: tên cá nhân/tổ chức, loại người thuê và tên khu vực từ hồ sơ đã lưu; Xem hồ sơ, Phương thức đăng nhập, Đăng xuất hoạt động. Không còn nút Hồ sơ xác minh song song. Cài đặt/Trợ giúp được khóa và ghi Sắp có. Admin dùng cùng khung Tài khoản nhưng không có form nghề nghiệp, có Kiểm tra hồ sơ và phương thức đăng nhập. Người tìm việc mở trang Hồ sơ thống nhất từ tab Hồ sơ và vào tài khoản/phương thức đăng nhập từ đó.

Admin → Tổng quan → **Kiểm tra hồ sơ**, hoặc tab **Xử lý** → chọn **Hồ sơ nghỉ hưu** / **Danh tính người thuê** → chọn hồ sơ → màn **Kiểm tra hồ sơ**. Khi xem chi tiết, danh sách được ẩn để tập trung thông tin/ảnh/quyết định. Quay lại hoặc tab Xử lý trở về hàng đợi, có hỏi trước khi bỏ nhận xét chưa lưu. Quy trình cấp Admin và các bước duyệt trong [C05-ADMIN-GUIDE](C05-ADMIN-GUIDE.md).

SEEKER/EMPLOYER → Hồ sơ → khối xác minh với một dòng trạng thái và một nút. Chưa xác minh mở wizard; đang chờ/đã duyệt mở thẳng bản hiện hành chỉ đọc, Quay lại về Hồ sơ. Từ wizard đang chuẩn bị mà mở chi tiết thì Quay lại vẫn trở về wizard để giữ nháp. Nút điều hướng dưới cùng trở về đúng trang của Main; Main đọc lại hồ sơ/version và số liệu sau khi trở về.

## Dữ liệu và giới hạn đang hiển thị

- Số chờ duyệt là hai phép đếm server: `retirementRequests` và `identityRequests`, `status == SUBMITTED`, mỗi loại giới hạn 50 như quyền list hiện có. Mức chạm giới hạn hiển thị **50+** cho loại đó; tổng cũng có dấu **+** nếu một loại chạm giới hạn. Đây không phải tổng không giới hạn hoặc thống kê toàn hệ thống.
- Có trạng thái đang tải, chưa tải được và Tải lại. Không biến lỗi quyền/mạng thành số 0. Số liệu được đọc khi tải tài khoản Admin, khi bấm Tải lại hoặc sau khi trở về từ màn Xử lý; không phải bộ đếm thời gian thực.
- Thẻ tin tuyển, báo cáo, tranh chấp, ca quá hạn và tab thống kê vẫn ở vị trí tham khảo nhưng khóa/Sắp có. Không gán số lượng DEMO hoặc tạo thao tác giả. Việc làm/tin tuyển/ca làm/trợ giúp ở tài khoản thường cũng chưa mở nghiệp vụ.
- Giọng đọc TTS vẫn ở C22, chưa thêm nút phát âm thanh giả. Không triển khai thêm nghiệp vụ ứng tuyển/đăng việc/thống kê trong đợt điều hướng này.
- Mở Trang chủ khi hồ sơ chưa được duyệt không cấp quyền nghiệp vụ. C05 vẫn kiểm phiên, ACTIVE, contact, request và version bằng Repository/Rules; client không tự cấp Admin.
- Không đổi schema, Rules hoặc cấu hình Firebase cho phần bổ sung điều hướng. Nếu đã Publish bản C05/parserVersion 2 đang gửi thành công thì không cần Publish lại chỉ vì phần giao diện này.

## Bố cục và trạng thái

Topbar dùng logo sẵn có, nền xanh, nhãn Chung/Người tìm việc/Người thuê/Admin một dòng. Nhãn có mô tả đầy đủ cho accessibility; logo nhỏ hơn khi chữ lớn để nhường chỗ. Nội dung cuộn riêng, thanh điều hướng cố định dưới vùng nội dung và tránh thanh hệ thống; ẩn khi bàn phím mở. Nội dung/thanh điều hướng có chiều rộng giới hạn trên màn lớn. Thanh điều hướng đổi từ 4 sang 2 cột khi màn hẹp hoặc chữ lớn.

Khoảng cách thẻ/nút trang chính và tài khoản 16dp; nút chính trong các trang này tối thiểu 72dp, điều hướng tối thiểu 64dp. Form nghề nghiệp, nhóm Căn cước/Bằng chứng/Gửi và xử lý ảnh C05 giữ đường đi hiện tại. Không nhân bản logic Google/OTP/QR/upload/quyết định.

Chuyển Trang chủ khi form chưa lưu phải xác nhận bỏ thay đổi; mở Tài khoản rồi quay lại Hồ sơ giữ nháp trong ViewModel. Mở xác minh yêu cầu lưu/bỏ nháp nghề nghiệp trước. Đổi UID/đăng xuất xóa trang đích, dữ liệu tài khoản và số đếm khỏi trạng thái cũ. Callback đếm cũ có kiểm UID/generation/request.

## File chính

Mới:

- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/navigation/RoleNavigationBar.java`
- `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/navigation/RoleHomePanel.java`
- `app/src/main/res/layout/role_topbar.xml`, `role_navigation.xml`, `role_home_content.xml`, `role_account_content.xml`
- `app/src/main/res/values/navigation_strings.xml`, `navigation_styles.xml`
- `app/src/main/res/drawable/navigation_surface.xml`, `navigation_item.xml`, `ic_nav_home.xml`, `ic_nav_account.xml`, `ic_nav_queue.xml`, `ic_nav_search.xml`, `ic_nav_stats.xml`

Sửa: `MainActivity.java`, `ProfilePanel.java`, `ProfileViewModel.java`, `ProfileRepository.java`, `activity_main.xml`, `profile_content.xml`, `VerificationActivity.java`, `VerificationViewModel.java`, `activity_verification.xml`. Cập nhật README/SETUP, báo cáo/checklist C04/C05 và hướng dẫn Admin.

## Bàn giao kiểm tra

Đã rà mã nguồn, cấu trúc XML, tham chiếu resource và khoảng trắng diff. Đây là rà tĩnh, không xác nhận app đã chạy. Không chạy build/test/lint/emulator/SMS/kiểm thử Rules; không sửa Console/deploy/stage/commit/push. Thay đổi `.idea/misc.xml` có từ trước được giữ nguyên.

Các ca mới trong [C04-CHECKLIST](C04-CHECKLIST.md) mục C04-N01–N07 và [C05-CHECKLIST](C05-CHECKLIST.md) mục C05-N01–N06 đều đang chờ người dùng kiểm tra. C02-F01 và những ca timeout/callback trễ/biến thể thiết bị chưa tái hiện của các luồng cũ vẫn giữ trạng thái theo dõi.

## Cải tiến Hồ sơ thống nhất theo đề xuất người dùng

Trong Hồ sơ, các vùng Thông tin hồ sơ/Công việc/Số điện thoại được tách bằng thẻ, giữ các trường và nút Lưu hiện có. Bên dưới là một khối Xác minh hồ sơ gồm trạng thái ngắn và một nút:

| Trạng thái server | Nhãn | Nút |
|---|---|---|
| NOT_SUBMITTED hoặc User C04 chưa có trường trạng thái | Chưa xác minh | Xác minh hồ sơ |
| SUBMITTED | Đang chờ duyệt | Xem hồ sơ đã gửi |
| APPROVED (SEEKER) / VERIFIED (EMPLOYER), đúng version hiện hành | Đã xác minh | Xem lại hồ sơ |
| NEEDS_INFO | Cần bổ sung | Bổ sung hồ sơ |
| REJECTED | Chưa được chấp thuận | Cập nhật hồ sơ |
| REVERIFY_REQUIRED | Cần xác minh lại | Xác minh lại |
| Lỗi tải / trạng thái hoặc version không nhất quán | Chưa tải được / Chưa xác định | Tải lại |

Xem lại không tự chuyển sang chuẩn bị bản mới. Người dùng đã duyệt có thể chủ động Cập nhật giấy tờ trong màn xem để vào luồng gửi mới, có xác nhận; chỉ gửi mới thành công mới làm thay đổi trạng thái theo C05.

ProfileRepository bổ sung tóm tắt trạng thái từ User/profile trong lần đọc server; nhãn Đã xác minh cần đúng loại role, approvedIdentityProfileVersion > 0 bằng identityProfileVersion và pointer hiện hành không rỗng. Đây là nhãn UI, không thay kiểm quyền/version tại thao tác nghiệp vụ. Không đưa số căn cước/ảnh/raw QR vào trang Hồ sơ thường.

ProfileViewModel cập nhật khi mở Hồ sơ, foreground và trở về từ C05; lỗi không giả thành Chưa xác minh/Đã xác minh. Trạng thái được tải lại có kiểm UID/generation/request; nháp nghề nghiệp không bị thay thế. Không phải listener thời gian thực trong lúc app đứng nguyên một màn. Kết quả từ thiết bị khác được cập nhật khi mở lại/foreground/tải lại.

Sửa các lớp Main/ProfileRepository/ProfileViewModel/ProfilePanel/RoleHomePanel, layout profile_content/role_home_content/role_account_content, profile strings/styles; C05 Activity/ViewModel nhận cờ điều hướng boolean và tự đọc request hiện hành từ server, không truyền giấy tờ qua Intent. Chức năng Admin/QR/upload/quyết định/Rules/schema giữ nguyên. C04-P01–P06 đang chờ người dùng kiểm tra; không tự build/test/app hoặc commit/push.
