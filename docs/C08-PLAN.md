# C08 — xem việc công khai và cache SQLite

26/10/2026. Người dùng yêu cầu triển khai C08 sau khi nghiệm thu C07. Nền mã nguồn `08d2211`; các tài liệu chốt C07 đang sửa từ lượt trước được giữ nguyên. Chỉ một agent sửa mã; không build/test/emulator/Console/deploy/stage/commit/push.

## Phạm vi và thứ tự thực hiện

1. Tách model chỉ chứa các trường công khai của tin OPEN; giữ `workAddress` đã chốt ở C07. Không sao chép toàn bộ JobPost/Firestore document vào cache.
2. Tạo SQLiteOpenHelper riêng `public_jobs.db` v1, DAO và executor: cache việc, danh mục/khu vực; tối đa 500 tin được tải gần nhất. Không thay bảng/schema/version `job_drafts.db` của C06.
3. Đọc danh sách `status=OPEN`, `expiresAt > mốc bắt đầu tải`; lọc tùy chọn một khu vực và một loại việc. Xếp `expiresAt ASC`, document ID ASC, mỗi lần 20 tin bằng document cursor. Danh mục active đọc từng trang 100; nếu chạm ngưỡng 1.000 mục/loại báo không tải được đầy đủ thay vì âm thầm cắt.
4. Màn danh sách, lọc, Tải thêm, chi tiết phân vùng; khách mở từ đăng nhập, SEEKER mở từ Trang chủ → Việc khác hoặc thanh điều hướng. Khách không bị tạo tài khoản Firebase ẩn danh. Người đã đăng nhập nhưng đang lỗi tải hồ sơ vẫn có đường Xem việc công khai.
5. Mở chi tiết luôn thử server. Chỉ lỗi mạng/timeout mới dùng SQLite; tin mất/không OPEN hoặc bị server từ chối thì xóa bản cache của tin và báo không còn xem được. Bản offline ghi thời điểm lưu và trạng thái chưa được kiểm tra mới; không mở ứng tuyển.
6. Bổ sung Rules đọc danh mục active cho khách và bốn index đúng bốn tổ hợp bộ lọc; giữ nguyên job/history Rules r4 và mọi quyền ghi C04–C07.
7. Review tĩnh và bàn giao Console/checklist để người dùng tự Sync/Run/nghiệm thu.

## Giao diện

Giữ topbar xanh/logo/vai trò, nền ngà, font và style đang dùng. Danh sách có khối lọc khu vực/loại việc, nút Áp dụng/Bỏ bộ lọc, Xem bản đã lưu, các card và Tải thêm. Theo mẫu người dùng bổ sung, card có ảnh thật minh họa loại việc, nhãn rõ và các mạch thông tin; Quay lại/Tải lại gọn cùng hàng cố định dưới topbar. Chi tiết tại [C08-UI-REFINEMENT](C08-UI-REFINEMENT.md). Nhãn bộ lọc theo lựa chọn đã áp dụng. Không giả ảnh nơi tuyển hoặc khoảng cách/GPS.

Chi tiết gồm tiêu đề/người thuê/hạn, Công việc cần làm, Địa điểm làm việc có địa chỉ đầy đủ, Lịch làm việc, Yêu cầu và thể lực, Tiền công. Lưu việc/Ứng tuyển khóa và ghi Sắp có. Back từ chi tiết về danh sách giữ bộ lọc/vị trí cuộn; Back vẫn dùng được khi đang tải.

## Ngoại tuyến và ranh giới

Cache công khai dùng chung cho khách và các tài khoản, không có UID/lựa chọn cá nhân/đơn/giấy tờ/liên hệ/token. Vì vậy đăng xuất không chuyển dữ liệu riêng của A sang B; danh sách/form trong ViewModel bị đóng khi UID thay đổi. Các cache theo tài khoản ở C09/C10 vẫn phải tách owner và dọn đúng kế hoạch; nháp C06 vẫn giữ theo UID.

SQLite chỉ là tập đã tải, không phải toàn bộ tin trên server. Bộ lọc local dùng cùng categoryId/areaCode và thứ tự hạn/ID; không trộn cursor local với server. Tin hết hạn bị loại khỏi danh sách theo giờ thiết bị; chi tiết đã lưu vẫn báo Hết hạn. Khi offline không thể biết tin vừa bị ẩn/đóng từ máy khác. Không khẳng định bản offline còn mở hoặc mới nhất.

Không triển khai C09 bốn bước/gợi ý, C10 lưu việc, C11 sửa/đóng/ẩn, C12 ứng tuyển hoặc TTS C22. Không thêm dependency, đổi Auth/JSON/SHA/Storage, không xóa dữ liệu ứng dụng.
