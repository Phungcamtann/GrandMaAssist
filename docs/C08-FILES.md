# C08 — bàn giao xem việc công khai

26/10/2026. **Đã triển khai, chờ người dùng cấu hình Console và Sync/Run/nghiệm thu.** Nền code `08d2211` đã chốt C07. Giữ các thay đổi tài liệu nghiệm thu C07 chưa commit từ lượt trước. Codex không build/test/lint/emulator/Rules tests/app/Console/deploy/stage/commit/push.

**Cập nhật UI theo mẫu người dùng 26/10/2026:** card ảnh minh họa/badge/tiêu đề/người thuê-khu vực/lịch/tiền phân vùng rõ, bấm cả card mở chi tiết. Tải lại gọn bên phải Quay lại trên thanh cố định dưới topbar. Sau khi người dùng đã duyệt luồng C08, tiếp tục thu gọn phần lọc thành nút **Tìm theo ?**, bỏ lối vào cache thủ công và làm tiêu đề card 26sp đậm hơn. Xem [C08-UI-REFINEMENT](C08-UI-REFINEMENT.md) và [nguồn ảnh](assets/C08-ASSETS.md), gồm file và checklist UI01–UI06. Không đổi dữ liệu/Rules/index/cache/logic tải cho lần sửa này.

## Hành vi

Khách mở Xem việc không cần đăng nhập; SEEKER mở Việc khác từ Trang chủ/taskbar/C05. Xem việc công khai không cần hồ sơ được duyệt hoặc xác minh số. Người có phiên Auth nhưng lỗi tải hồ sơ vẫn có đường Xem việc. Lối vào Google/Phone và các vai trò khác giữ nghiệp vụ đã có.

Danh sách lọc categoryId/areaCode, OPEN và hạn sau mốc bắt đầu, xếp expiresAt/ID tăng dần, 20/trang; Tải mới reset cursor, Tải thêm deduplicate ID. Loại các tin đã hết hạn theo giờ thiết bị khi vào màn và mỗi 30 giây. Mỗi chuỗi phân trang giữ mốc hạn đầu; không khẳng định snapshot nhất quán toàn collection nếu dữ liệu thay đổi trong lúc tải nhiều trang.

Chi tiết đọc Source.SERVER trước khi hiển thị. Hiện đủ workAddress cùng các trường công việc công khai. Không tải ảnh/giấy tờ, lịch sử riêng hay số liên hệ; không giả tạo ảnh, khoảng cách hoặc badge sức khỏe. Lưu việc/Ứng tuyển vẫn khóa/Sắp có. Back không bị tác vụ đọc chặn; ViewModel giữ tác vụ/filter qua xoay, đổi Auth UID kết thúc màn.

Đọc mạng chờ tối đa 20 giây trước khi xử lý lỗi/fallback; timeout task không hủy request gốc. Callback UI được đánh số generation để không kéo người dùng về màn cũ. Cache public có tombstone theo ID/thời điểm để response bắt đầu trước lần vô hiệu không ghi lại tin vào SQLite. Không có mutation nền.

## File mới

- `domain/model/PublicJob.java`: projection whitelist, validation, JSON cache v1; không giữ employerId/reviewedBy/reviewReason/lastSubmissionId hoặc dữ liệu riêng trong bản cache.
- `data/local/PublicJobStore.java`: SQLiteOpenHelper `public_jobs.db` v1, cached_jobs/categories/areas/unavailable_jobs; local filter/cursor, tối đa 500 việc, không thay DB nháp C06.
- `data/repository/PublicJobRepository.java`: Source.SERVER list/detail/catalog, cursor 20 và catalog từng trang 100, executor riêng cho SQLite; lỗi quyền/index không giả thành cache hoặc rỗng. Catalog active có ngưỡng 1.000 mục/loại với lỗi rõ.
- `viewmodel/PublicJobsViewModel.java`: session/generation guards, filter, online/offline pagination, phục hồi chi tiết, catalog và expiry.
- `ui/jobs/PublicJobsActivity.java`, `res/layout/activity_public_jobs.xml`, `res/values/public_job_strings.xml`, `res/values/public_job_ids.xml`: UI danh sách/chi tiết, insets/chiều rộng/scroll/TalkBack, status và lối về.
- `docs/C08-PLAN.md`, `C08-CONSOLE-SCHEMA.md`, `C08-CHECKLIST.md`, báo cáo này.

## File sửa

- MainActivity/activity_main: lối xem việc cho khách và khi đang lỗi tải hồ sơ. Không signInAnonymously hoặc thay phương thức đăng nhập.
- RoleHomePanel/RoleNavigationBar/ProfilePanel: mở Việc khác cho SEEKER, giữ hỏi bỏ form và thứ tự hai ActivityResult launcher cũ; thêm launcher công khai sau chúng. Từ màn công khai về Home/Hồ sơ đúng route.
- VerificationActivity: hỗ trợ route OTHER đang được bật trên taskbar, dùng guard bỏ nháp cũ trước rời.
- AndroidManifest: PublicJobsActivity private exported=false.
- backup_rules/data_extraction_rules: loại cache public và sidecar DB khỏi backup/transfer, giữ loại trừ nháp C06.
- firebase/firestore.rules: chỉ mở đọc active catalog cho khách; giữ query areas người đăng nhập không bắt limit để không phá ProfileRepository C04. Khối job/history r4 nguyên vẹn.
- firebase/firestore.indexes.json: thêm bốn tổ hợp status/(categoryId)/(areaCode)/expiresAt ASC/name ASC; giữ hai index C07.
- README/SETUP: trạng thái triển khai C08, đường cấu hình và nghiệm thu.

## Giới hạn và nguyên tắc cache

Cache C08 là public dùng chung trên thiết bị, không có bảng theo UID/lựa chọn cá nhân. Không tự mang dữ liệu search/save của tài khoản cũ qua tài khoản mới; các dữ liệu đó chưa triển khai. Draft C06 vẫn tách UID và giữ sau đăng xuất. Bộ lọc chỉ giữ trong RAM/saved state màn hiện tại, không đồng bộ preferences C09.

Tải mới server không chứng minh những tin không trả về đã đóng (có thể ngoài filter/trang); vì vậy không xóa toàn cache chỉ từ một query. Mở chi tiết server không còn cho xem mới vô hiệu cache của đúng tin. Offline không thể biết thay đổi mới từ máy khác; luôn nhãn cũ và không ứng tuyển. Không có quyền offline lấy cache để đọc tài liệu riêng.

Nguồn catalog online được ưu tiên; lỗi lưu catalog không làm hỏng danh sách đang xem, lần mở offline sau có thể còn catalog cũ. Cache dữ liệu giả mạo/hỏng báo lỗi, không silently reset/drop dữ liệu nháp. Dùng giờ thiết bị cho lọc hạn hiển thị; cần giờ tự động đúng khi nghiệm thu, quyền ghi về sau vẫn do Rules kiểm server.

## Review và người dùng áp dụng

Rà tĩnh các query/filter/index, whitelist cache, SQLite dùng executor/transaction, branch permission vs network, lifecycle/auth/Back và route C04/C05. Đã parse 60 XML, đối chiếu tham chiếu tài nguyên Java (tính cả client ID do Google Services sinh sẵn), JSON sáu index và liên kết tài liệu; rà chuỗi/comment/ngoặc của năm Java mới và git diff --check. Đối chiếu nguyên khối C07 jobs/history với HEAD cho kết quả không đổi. Đây không phải biên dịch Java, thực thi SQLite, Rules hoặc kiểm thử ứng dụng.

Làm [C08-CONSOLE-SCHEMA](C08-CONSOLE-SCHEMA.md) — Publish toàn bộ Rules và tạo bốn index, chờ Enabled — rồi Sync/Run và [C08-CHECKLIST](C08-CHECKLIST.md). Không gỡ app/xóa data/nháp. Chưa ghi nhận kết quả runtime C08 hoặc nghiệm thu. Kết quả Rules 24/24 trong C07 là lịch sử cho bản r4, không thay kiểm tra quyền mới C08.

Căn cứ: [Firestore cursors](https://firebase.google.com/docs/firestore/query-data/query-cursors), [Firestore queries](https://firebase.google.com/docs/firestore/query-data/queries), [SQLite Android](https://developer.android.com/training/data-storage/sqlite).
