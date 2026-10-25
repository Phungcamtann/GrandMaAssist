# C06 — danh mục Firebase và dữ liệu nháp cục bộ

25/10/2026. Project **grandmaassist-a1dfa**, Firestore `(default)`. C04/C05 đang hoạt động; không xóa dữ liệu, không đổi tài khoản/JSON/SHA/provider hoặc Storage Rules.

## Người dùng thực hiện trước nghiệm thu

1. Firebase Console → Firestore Database → Rules: thay bằng **toàn bộ** `firebase/firestore.rules` hiện tại và Publish. C06 chỉ thêm match `jobCategories`: tài khoản đã đăng nhập đọc tài liệu active; list cần giới hạn tối đa 100; client không ghi danh mục. Các quy tắc C04/C05 và default-deny giữ nguyên.
2. Firestore → Data → **Start collection ở cấp gốc**, Collection ID **jobCategories**, cùng cấp với areas/users/adminAccess. Không tạo bên trong document User/Admin.
3. Tạo bốn document theo bảng. **Document ID** là cột đầu, không dùng Auto-ID; thêm ba field `name`, `iconKey`, `active` bên trong từng document:

| Document ID | name — string | iconKey — string | active — boolean |
|---|---|---|---|
| GARDEN_CARE | Chăm sóc cây | garden | true |
| KITCHEN_HELP | Phụ bếp | kitchen | true |
| LIBRARY_HELP | Hỗ trợ thư viện | library | true |
| SHOP_HELP | Phụ giúp cửa hàng | shop | true |

`active` phải chọn kiểu **boolean**, không nhập chuỗi “true”. `name` là tên field, không dùng tên công việc làm tên field. File `firebase/job-categories-demo.json` mô tả dữ liệu tham khảo, không phải file Rules và không phải lệnh import tự động của Console. Không tạo các field `collection`, `documents`, `fields` từ lớp bọc JSON trong document.

4. Giữ nguyên ba `areas/DEMO_AREA_01..03` đã đúng `name` string/`active` boolean. Không nhập lại nếu đang hoạt động.
5. Android Studio Sync/Run. Đăng nhập tài khoản **EMPLOYER/ACTIVE** → **Soạn tin tuyển** hoặc **Tin của tôi**. Lần đầu cần online để xác nhận vai trò và tải danh mục. Không cần OTP/VERIFIED chỉ để soạn nháp.
6. Vào form → Cập nhật danh mục nếu cần. Tải được bốn loại việc và ba khu vực; sau lần tải thành công thử offline. SEEKER/Admin không mở soạn tin tuyển.

Không có yêu cầu đổi Gradle hoặc thêm thư viện cho C06. SQLite dùng API sẵn có. Không Publish Storage Rules lại chỉ vì C06.

## App đọc và lưu gì

Firestore chỉ đọc `users/{uid}`, `adminAccess/{uid}`, profile liên quan theo C04 để kiểm EMPLOYER/ACTIVE; danh mục `jobCategories` và `areas` query `active == true`, `limit(100)`, `Source.SERVER`. C06 không tạo collection jobs/jobPosts, không ghi PENDING_REVIEW, không tạo notification hoặc tự cấp quyền. Gửi/duyệt tin thuộc C07.

Sau khi server xác nhận người thuê hoạt động, app lưu marker UID đã từng được xác nhận để dùng nháp local khi offline. Nếu server trả thiếu quyền/role sai/tài khoản hạn chế thì màn soạn bị khóa; chỉ lỗi mạng mới được giữ chế độ local. Marker/cache này không được dùng làm quyền ghi Firebase ở C07.

### SQLite — job_drafts.db, version 1

**Bổ sung C07:** payload có thêm workAddress/serverBaseVersion tùy chọn đọc, tương thích nháp cũ; không thay bảng/DB version. Chính sách gửi và schema server mới ở [C07-CONSOLE-SCHEMA](C07-CONSOLE-SCHEMA.md). Phần C06 bên dưới mô tả nền trước khi có gửi tin.

| Bảng | Nội dung |
|---|---|
| job_drafts | Khóa ghép owner_uid/id, revision, created_at/updated_at cục bộ và payload JSON schemaVersion=1 |
| draft_owners | owner_uid và confirmed_at cục bộ; chỉ cho biết UID từng được xác nhận để mở nháp local |
| draft_catalog | kind CATEGORY/AREA, ID, tên, iconKey, cached_at; snapshot danh mục sau lần tải thành công |

Payload gồm tiêu đề, categoryId, areaCode, mô tả, yêu cầu, kỹ năng, ngày/buổi/lịch, demandMode/demands, ô tiền công đang gõ, đơn vị và hạn nhận. Không có JobStatus/reviewer/contact/ảnh/QR/snapshot đơn/thanh toán. ID UUID được tạo một lần khi mở nháp mới, giữ qua các lần lưu. Revision tăng theo mỗi lần lưu và được kiểm tra khi sửa/xóa để tránh ghi đè bản mới.

Nháp được giữ khi đăng xuất và chỉ đúng UID đăng nhập lại thấy. Đây là dữ liệu đã soạn, khác cache phiên phải dọn; dữ liệu RAM bị xóa khi đổi UID/đóng màn. Không đồng bộ sang máy khác. DB và journal/WAL/SHM được khai báo loại khỏi cloud backup/device transfer; gỡ app hoặc xóa dữ liệu có thể mất nháp. Không có thao tác xóa DB khi nâng cấp; version hiện tại là 1, phiên bản sau phải thêm migration tường minh.

Danh mục được thay cùng transaction SQLite chỉ sau khi cả hai query thành công và dữ liệu hợp lệ. Lỗi không tạo danh mục giả hoặc thay ID. Cache có nhãn trên form khi sử dụng, C07 vẫn phải kiểm lại server trước gửi. C06 giới hạn tối đa 100 mục mỗi loại cho danh mục DEMO hiện tại.

## Lỗi thường gặp

- Không có danh mục/không tải được: kiểm tra đúng project/database, Publish Rules mới, collection/document/field đúng bảng, active boolean true và có mạng. Không mở quyền read/write toàn bộ để vượt lỗi.
- Chưa có nháp khi offline: UID cần từng mở soạn tin thành công online; nháp của UID khác không được dùng. Nếu Main đang kiểm tra tài khoản hoặc báo lỗi mạng, nút **Nháp tin trên máy** chỉ hiện cho UID đã có marker local.
- Đã đăng xuất không thấy nháp ở tài khoản khác: đúng thiết kế; đăng nhập lại đúng Google/phone đã liên kết cùng UID. Hai tài khoản cùng tên không đồng nghĩa cùng UID.
- “Nháp đã thay đổi”: bản khác vừa được lưu/xóa; bấm Tải lại nháp hoặc Tải lại danh sách sau khi xác nhận bỏ thay đổi. Không ghi đè âm thầm.
- Danh mục cũ inactive: giữ ID trong nháp và báo chọn lại khi xem trước, không tự chuyển sang mục khác.

Codex chưa truy cập/đổi Console, Publish Rules, Sync/build/test hoặc chạy app. Người dùng thực hiện các bước trên và checklist C06.
