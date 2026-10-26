# C09 — Bàn giao danh sách file triển khai

27/10/2026.

## 1. File mới tạo
1. `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/domain/model/SearchPreference.java`:
   - Value Object bất biến chứa 4 tiêu chí tìm việc, JSON serialize/deserialize v1.
2. `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/local/SearchPreferenceStore.java`:
   - Quản lý SQLite `search_preferences.db`, bảng `local_search_preferences(owner_key, selections_json, updated_at)`.
3. `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/domain/service/GuidedJobMatcher.java`:
   - Matching Engine lọc cứng và chấm điểm 50/30/20, sinh explanationTags trực quan.
4. `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/GuidedSearchRepository.java`:
   - Cầu nối bất đồng bộ Task giữa SQLite Store và `PublicJobRepository`, hỗ trợ fallback offline.
5. `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/viewmodel/GuidedSearchViewModel.java`:
   - Quản lý state 4 bước wizard, tóm tắt và kết quả matching.
6. `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/jobs/GuidedSearchActivity.java`:
   - Màn hình Wizard 4 bước + Tóm tắt + Kết quả, hỗ trợ nút Back, cuộn trang, chữ lớn, tương phản cao.
7. `app/src/main/res/layout/activity_guided_search.xml`:
   - Layout XML cho `GuidedSearchActivity`.
8. `app/src/main/res/values/guided_search_strings.xml`:
   - Định nghĩa chuỗi giao diện tiếng Việt thân thiện người cao tuổi.
9. `docs/C09-PLAN.md`, `docs/C09-CHECKLIST.md`, `docs/C09-FILES.md`.

## 2. File cập nhật
1. `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/navigation/RoleHomePanel.java`:
   - Tinh chỉnh giao diện Trang chủ Seeker (tiêu đề, phụ đề, nút "Giúp tôi tìm việc", 4 card lớn, nút pill "Nghe hướng dẫn").
2. `app/src/main/res/layout/role_home_content.xml`:
   - Bổ sung nút pill `homeAudioGuide` và ẩn nút thừa.
3. `app/src/main/AndroidManifest.xml`:
   - Đăng ký `GuidedSearchActivity` (`exported=false`, `windowSoftInputMode="adjustResize"`).
4. `app/src/main/res/xml/backup_rules.xml` & `data_extraction_rules.xml`:
   - Loại trừ database `search_preferences.db` khỏi backup/transfer.
