# C09 — Checklist nghiệm thu: Tìm việc theo hướng dẫn

27/10/2026. **Đã triển khai mã nguồn.** Dùng để người dùng tự nghiệm thu trên máy thật / giả lập Android Studio.

## 1. Màn hình chính Seeker (Home Seeker)
- [ ] C09-01 Tiêu đề chính hiển thị: *"Cô/chú mong muốn công việc như thế nào ?"*, phụ đề: *"Cứ thong thả, từng bước một."*.
- [ ] C09-02 Nút lớn màu xanh lá: *"Giúp tôi tìm việc"* bấm vào chuyển mượt sang màn hình `GuidedSearchActivity`.
- [ ] C09-03 Bốn thẻ chức năng bên dưới hiển thị đúng nhãn và phụ đề: Việc cần xử lý, Việc của tôi, Xem các việc khác (bấm mở PublicJobsActivity), Trợ giúp.
- [ ] C09-04 Nút pill *"Nghe hướng dẫn"* hiển thị nổi bật ở phía dưới.

## 2. Wizard 4 Bước (Guided Search)
- [ ] C09-05 **Bước 1/4 (Loại việc):** Hiển thị danh sách danh mục việc active. Chọn một hoặc nhiều mục, viền đổi xanh và checkbox tick. Tick *"Chưa có ưu tiên"* thì tự bỏ chọn các mục cụ thể và ngược lại.
- [ ] C09-06 **Bước 2/4 (Khu vực):** Hiển thị danh sách khu vực. Nếu tài khoản Seeker đã có khu vực trong hồ sơ, mục đó hiển thị nhãn: *"[Tên KV] • Gợi ý từ hồ sơ"*. Hỗ trợ *"Chưa có ưu tiên"*.
- [ ] C09-07 **Bước 3/4 (Lịch rảnh):** 2 hàng pill chọn thứ `[T2]..[CN]`, 1 hàng pill chọn buổi `[Sáng] [Chiều] [Tối]`. Có card *"Linh hoạt"*.
- [ ] C09-08 **Bước 4/4 (Muốn tránh):** Các card *"Mang vác nặng"*, *"Đứng lâu"*, *"Di chuyển nhiều"*, *"Không có yêu cầu riêng"*.
- [ ] C09-09 Thanh tiến trình 4 đoạn ngang trên đầu đổi màu xanh lá đậm theo từng bước (1..4).
- [ ] C09-10 Nút điều hướng: `< Quay lại` lùi từng bước, `Bỏ qua` sang bước kế, nút lớn `Tiếp tục` hoạt động đúng. Nút Back hệ thống lùi từng bước thay vì thoát app đột ngột.

## 3. Màn Tóm tắt & Kết quả gợi ý
- [ ] C09-11 **Màn Tóm tắt (Step 5):** Hiển thị 4 mục đã chọn (Loại việc, Khu vực, Lịch làm, Muốn tránh). Bấm nút *"Sửa"* ở mục nào thì quay thẳng về bước đó để chỉnh sửa.
- [ ] C09-12 Nút lớn chuyển thành *"Xem việc phù hợp"*.
- [ ] C09-13 **Màn Kết quả (Step 6):** Các tin tuyển dụng được lọc cứng (loại bỏ hoàn toàn tin có yêu cầu thể lực muốn tránh và sai khu vực/lịch).
- [ ] C09-14 Tin hiển thị theo điểm số gợi ý từ cao đến thấp, kèm các chip giải thích lý do (*"Đúng loại việc đã chọn"*, *"Gần khu vực hồ sơ"*, *"Không yêu cầu mang vác nặng"*, *"Cần trao đổi lịch"*).
- [ ] C09-15 Bấm vào card công việc chuyển sang xem chi tiết tại `PublicJobsActivity`.
- [ ] C09-16 Nếu không có việc làm nào khớp cả 4 tiêu chí, hiển thị thông báo thân thiện và nút *"Xem tất cả việc"*.

## 4. SQLite & Ngoại tuyến
- [ ] C09-17 Lựa chọn của người dùng được lưu vào `search_preferences.db`. Mở lại app đọc lại đúng lựa chọn đã lưu.
- [ ] C09-18 Khi mất mạng, thuật toán vẫn matching mượt mà trên tập tin đã cache của C08 (`public_jobs.db`).
