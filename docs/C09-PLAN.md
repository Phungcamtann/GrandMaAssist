# C09 — Tìm việc theo hướng dẫn (Guided Search)

27/10/2026. **Đã triển khai mã nguồn, chuyển giao để người dùng nghiệm thu.**
Căn cứ: Mục 10 bảng phân công, tài liệu đặc tả `GrandMaAssist_PhanTichThietKe_v3.md` (mục 6.2, 6.3, 7.2, 8.3 UC06, 13.3).

## 1. Mục tiêu và phạm vi
1. **Wizard 4 bước cho người lớn tuổi:**
   - Bước 1/4: Loại việc (danh mục active, chọn nhiều hoặc "Chưa có ưu tiên").
   - Bước 2/4: Khu vực (khu vực active, gợi ý khu vực từ hồ sơ Seeker, "Chưa có ưu tiên").
   - Bước 3/4: Lịch rảnh (chọn thứ T2..CN, buổi Sáng/Chiều/Tối, hoặc "Linh hoạt").
   - Bước 4/4: Yêu cầu muốn tránh (Mang vác nặng, Đứng lâu, Di chuyển nhiều, hoặc "Không có yêu cầu riêng").
2. **Màn Tóm tắt (Step 5):**
   - Tóm tắt 4 tiêu chí; cho phép bấm nút "Sửa" từng bước mà không cần làm lại từ đầu.
3. **Thuật toán Gợi ý (Matching Engine 50/30/20):**
   - Lọc cứng: Khu vực, yêu cầu thể lực cần tránh, lịch làm rảnh.
   - Chấm điểm: Khớp loại việc (+50/30 cơ bản), khớp lịch (+30/15 linh hoạt), gần khu vực hồ sơ (+20).
   - Nhãn giải thích trực quan: "Đúng loại việc đã chọn", "Gần khu vực hồ sơ", "Không yêu cầu mang vác nặng", "Cần trao đổi lịch".
4. **Lưu trữ Cục bộ SQLite:**
   - Database `search_preferences.db` độc lập, bảng `local_search_preferences(owner_key, selections_json, updated_at)`.
   - Phân biệt khách và user có phiên Auth; lưu và đọc lại lựa chọn cũ.
5. **Tinh chỉnh Giao diện Trang chủ Seeker:**
   - Đồng bộ câu chào, nút "Giúp tôi tìm việc", 4 card chức năng và nút pill "Nghe hướng dẫn".
