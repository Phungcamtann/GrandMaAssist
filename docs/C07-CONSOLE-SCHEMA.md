# C07 — Firebase Console và schema

**Trạng thái nghiệm thu hiện hành — 26/10/2026:** người dùng xác nhận lỗi gửi tin được khắc phục và duyệt các luồng hoạt động khác; commit `08d2211` khớp local/origin/main sau fetch. Không cần thực hiện lại các bước Console dưới đây chỉ để chốt C07. Xem [C07-ACCEPTANCE](C07-ACCEPTANCE.md); phần thiết lập và các trạng thái chờ bên dưới giữ làm tài liệu lịch sử/tham chiếu.

25/10/2026. Mã nguồn đã triển khai, chờ người dùng Publish Rules/tạo index và Sync/Run. Codex chưa sửa Console, deploy hoặc chạy kiểm thử. Căn cứ dự án hiện hành `grandmaassist-a1dfa`, Firestore `(default)`; giữ Auth/C04/C05/danh mục/Storage hiện có.

## 1. Publish Rules

**r4 — bản hiện hành:** đã tái hiện lỗi giới hạn 1.000 biểu thức và kiểm tra bản sửa đạt 24/24 ca bằng Emulator, theo ngoại lệ người dùng cho phép. Publish **toàn bộ file có mốc `2026-10-26 r4`**, rồi gửi lại nháp cũ trên app hiện có. Không cần Sync/Run hoặc tạo thêm index cho r4. Xem [C07-EXPRESSION-LIMIT-FIX](C07-EXPRESSION-LIMIT-FIX.md). Hướng dẫn/giả thuyết r3 phía dưới là lịch sử; C07 vẫn chờ người dùng nghiệm thu thật.

**r3 — trạng thái mới nhất:** lỗi C07_SUBMIT_TRANSACTION vẫn tái hiện sau bản giảm đọc; cập nhật cả app và toàn bộ Rules có mốc `2026-10-26 r3`. Bản này thêm các endpoint chỉ đọc cho chính chủ để khoanh vùng quyền/phone/profile/hạn server, không cần tạo tài liệu và không mở quyền ghi. Xem phần r3 trong [báo cáo điều tra](C07-PERMISSION-DIAGNOSTICS.md). Hướng dẫn bản giảm đọc phía dưới là lịch sử và chưa giải quyết được lỗi người dùng.

**Sửa 26/10/2026 sau lỗi C07_SUBMIT_TRANSACTION:** Publish lại toàn bộ file mới có mốc `C07 Rules revision 2026-10-26`. Bản này giảm đọc lặp ở các điều kiện jobs/history; không đổi schema, index hoặc yêu cầu duyệt hồ sơ. Xem [báo cáo điều tra](C07-PERMISSION-DIAGNOSTICS.md); chờ người dùng thử lại, chưa ghi nhận runtime thành công.

Mở đúng dự án → Firestore Database → Rules. Thay bằng **toàn bộ** nội dung `firebase/firestore.rules` trong repo rồi Publish. File gồm cả C04/C05/C06 và C07; không chỉ dán khối jobs đè mất Rules cũ. Không sửa Storage Rules, JSON, SHA hoặc provider để làm C07.

Nếu Console báo lỗi cú pháp/đánh giá Rules, giữ nguyên thông báo và vị trí dòng để kiểm tra; không mở `allow read, write: if true` để vượt lỗi.

## 2. Tạo hai composite index

Firestore → Indexes → Composite → Add index. Scope **Collection**, Collection ID `jobs`:

| Index | Field 1 | Order | Field 2 | Order |
|---|---|---|---|---|
| Tin của người thuê | employerId | Ascending | updatedAt | Descending |
| Hàng chờ Admin | status | Ascending | updatedAt | Descending |

Chờ cả hai **Enabled**. `__name__` đi cùng chiều Descending theo trường sắp xếp cuối; khai báo đầy đủ ở `firebase/firestore.indexes.json`. Giữ các index cũ nếu có. Nếu Logcat Firebase trả link tạo index còn thiếu, mở link, kiểm đúng collection/fields trên và tạo; không sửa thứ tự query để che lỗi cấu hình.

Không cần tạo thủ công `jobs` hoặc `history`: ứng dụng ghi nguyên tử khi gửi/duyệt. Không cần seed tin giả hoặc thêm trường địa chỉ vào users/employerProfiles. Bốn jobCategories và areas C06 vẫn dùng `active` boolean true, `name` string.

## 3. Điều kiện tài khoản

- **Người thuê:** users role EMPLOYER/accountStatus ACTIVE; không có adminAccess enabled; số Firebase Auth đã xác minh khớp users.phoneE164, phoneVerificationMode FIREBASE_AUTH và phoneVerifiedAt hợp lệ. Hồ sơ có tên, giới thiệu, khu vực active; ORGANIZATION cần tên đơn vị. Thiếu số/hồ sơ có nút mở Tài khoản / hồ sơ, lưu hoặc giữ nháp trước khi rời.
- Không thêm điều kiện identityStatus VERIFIED cho gửi tin. C05 vẫn là luồng xác minh riêng và điều kiện thỏa thuận ca ở giai đoạn sau.
- **Admin:** users/{uid} role ADMIN/accountStatus ACTIVE, adminAccess/{uid}.enabled boolean true như C05. Đăng nhập bình thường, mở Tổng quan → Duyệt tin tuyển hoặc Xử lý → Duyệt tin tuyển. Không tạo Admin trong app.

## 4. jobs/{id}

ID bằng UUID ổn định của nháp. Các trường được whitelist:

| Nhóm | Trường và kiểu |
|---|---|
| Chủ tin | id, employerId, employerDisplayName: string |
| Phân loại | categoryId/categoryName, areaCode/areaName: string; tên chụp từ danh mục server lúc gửi |
| Nội dung | title: string ≤120; description ≤3000; requirementsText ≤1000; requiredSkillTags: list tối đa 8 string ≤60 |
| Địa chỉ | workAddress: string không trống ≤500, do người thuê nhập; **công khai khi OPEN**, không lấy từ CCCD |
| Lịch | workDays: list 1–7 ngày không trùng; workShift MORNING/AFTERNOON/EVENING/FLEXIBLE; scheduleText không trống ≤500 |
| Thể lực | physicalDemands: list tối đa 3, không trùng, đúng enum C06; demandsDeclared boolean true (list rỗng nghĩa chủ động không có các yêu cầu đã liệt kê) |
| Tiền/hạn | payAmount: integer Long >0; payUnit HOUR/SESSION/DAY; expiresAt: timestamp tương lai khi gửi/chấp thuận |
| Vòng đời | status PENDING_REVIEW/OPEN/REJECTED trong C07; version integer; lastSubmissionId string; createdAt/updatedAt timestamp server |
| Quyết định hiện hành | reviewedBy string; reviewedAt timestamp hoặc null; reviewReason string ≤1000 |

Khi gửi: reviewedBy/reviewReason rỗng, reviewedAt null. Chấp thuận: OPEN, reviewedBy UID Admin, reviewedAt timestamp server, reviewReason rỗng. Từ chối: REJECTED, lý do bắt buộc không toàn khoảng trắng. Owner/createdAt không đổi; **version tăng ở mỗi chuyển trạng thái**, ví dụ gửi v1 → từ chối v2 → gửi lại v3 → duyệt v4. Không tự thêm EXPIRED: hạn là điều kiện riêng.

Rules cho public đọc tin OPEN; PENDING_REVIEW/REJECTED chỉ chủ tin/Admin. List phải limit ≤20 và query phù hợp; Rules không lọc kết quả thay query. C08 mới có màn danh sách/chi tiết việc công khai, không cần tìm màn đó khi nghiệm thu C07.

## 5. jobs/{id}/history/{version}

Mỗi thao tác ghi cùng transaction một sự kiện bất biến gồm jobId, version, actorId, action SUBMITTED/APPROVED/REJECTED, createdAt server, before (null ở lần tạo) và after (bản tin sau thao tác). Rules đối chiếu cả job và event bằng getAfter; không được chỉ ghi quyết định rồi ghi lịch sử sau. Không sửa/xóa lịch sử; chỉ chủ tin/Admin được đọc. Sự kiện không chứa hồ sơ căn cước/ảnh/số điện thoại. Không tạo notification giả hay hứa push nền; kết quả thấy trong Tin đã gửi khi tải lại.

## 6. Nháp, retry và dữ liệu cũ

Không thay cấu trúc bảng SQLite hoặc DB version 1. JSON schemaVersion 1 có thêm hai trường tùy chọn đọc tương thích: workAddress mặc định rỗng, serverBaseVersion mặc định 0. Không cần gỡ app/xóa dữ liệu; nháp đã lưu và UID giữ nguyên. Đây là mở rộng payload, không phải đã chạy thử migration DB.

Trước gửi, app lưu nháp thành công vào SQLite. Lần tạo dùng ID nháp và lastSubmissionId `{id}_s1`; gửi lại từ REJECTED dùng `{id}_s{baseVersion+1}`. Nếu server đã có cùng submission ID, trả bản server hiện hành, kể cả Admin đã xử lý; không ghi lại hoặc tăng version. Nếu nội dung nháp đã thay sau lần gửi đó, hiện rõ phần sửa local chưa thay nội dung server. Muốn gửi lại tin bị từ chối phải mở **Tin đã gửi → tin → Sửa và gửi lại** để lấy đúng baseVersion.

Nút Sửa và gửi lại mở bản server và đọc revision local để chống ghi đè. Có xác nhận trước khi thay nháp cùng ID khi lưu. Nháp không bị tự xóa sau gửi; nháp local không là bằng chứng trạng thái server. Đổi tài khoản không thấy nháp/tin riêng của tài khoản cũ.

Tác vụ C07 có thời gian chờ UI tối đa 45 giây. Timeout không hủy hoặc chứng minh transaction thất bại: người dùng tải lại/thử cùng tin để đối chiếu. Admin thao tác lại trên version cũ bị chặn và phải tải lại. Không dùng kết quả local để công khai tin offline.

## 7. Bắt đầu nghiệm thu

1. Người thuê → Soạn tin tuyển, điền cả địa chỉ → Xem trước → Gửi duyệt. Kiểm tra Tin đã gửi: Chờ duyệt.
2. Admin → Duyệt tin tuyển → xem đầy đủ → Chấp thuận (không lý do), hoặc Từ chối rồi nhập lý do.
3. Người thuê → Tin của tôi → Tin đã gửi → Tải lại: đúng kết quả/lý do. Tin bị từ chối → Sửa và gửi lại → xem trước/gửi → Admin nhận bản mới.

Chi tiết nghiệm thu trong [C07-CHECKLIST](C07-CHECKLIST.md), phạm vi/file trong [C07-FILES](C07-FILES.md). Các mục C08/C11/C12/C15 theo thứ tự, không mở sớm trong C07.
