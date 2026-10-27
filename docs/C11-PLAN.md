# Kế hoạch Triển khai C11: Quản lý tin đã đăng (Manage Job Posts)

## 1. Mục tiêu và Phạm vi
- **Mục tiêu:** Cung cấp đầy đủ khả năng quản lý vòng đời tin tuyển dụng theo phân tích thiết kế v3 (Mục 12 PDF):
  1. Người thuê (EMPLOYER):
     - **Đóng tin (OPEN -> CLOSED):** Ngừng nhận đơn mới khi đã đủ người hoặc không còn nhu cầu.
     - **Sửa tin đã duyệt (OPEN -> PENDING_REVIEW):** Cho phép sửa nội dung tin đang mở, tin sẽ tăng version và quay về trạng thái chờ duyệt (BR07).
  2. Quản trị viên (ADMIN):
     - **Ẩn tin vi phạm (OPEN/CLOSED -> HIDDEN):** Ẩn tin có lý do rõ ràng, lưu lịch sử và ngăn hiển thị công khai.
  3. Firestore & Bảo mật:
     - Cập nhật `firebase/firestore.rules` cho phép các bước chuyển trạng thái trên một cách an toàn và ghi nhận `history/{version}` bất biến.

## 2. Ma trận Chuyển trạng thái Tin tuyển dụng (JobStatus State Machine)
| Trạng thái cũ | Thao tác | Trạng thái mới | Ai thực hiện | Yêu cầu / Ràng buộc |
|---|---|---|---|---|
| `OPEN` | Đóng tin | `CLOSED` | Chủ tin (EMPLOYER) | Xác nhận đóng; không đổi nội dung; action `'CLOSED'` |
| `OPEN` | Sửa nội dung | `PENDING_REVIEW` | Chủ tin (EMPLOYER) | Sửa qua `JobDraftActivity`; tăng version; action `'SUBMITTED'` |
| `OPEN` hoặc `CLOSED` | Ẩn tin | `HIDDEN` | Quản trị viên (ADMIN) | Nhập lý do ẩn (tối đa 1.000 ký tự); action `'HIDDEN'` |
| `PENDING_REVIEW` | Duyệt / Từ chối | `OPEN` / `REJECTED` | Quản trị viên (ADMIN) | Đã có từ C07 |
| `REJECTED` | Sửa và gửi lại | `PENDING_REVIEW` | Chủ tin (EMPLOYER) | Đã có từ C07 |

## 3. Các thành phần cần triển khai
1. **Firebase Security Rules (`firebase/firestore.rules`):**
   - Mở rộng hàm `jobHistoryCreate` chấp nhận các action: `'CLOSED'`, `'HIDDEN'`.
   - Mở rộng quy tắc `allow update` trên `/jobs/{id}`:
     + Nhánh Employer đóng tin: `before.status == 'OPEN'` -> `after.status == 'CLOSED'`, chỉ thay đổi `status`, `version`, `updatedAt`.
     + Nhánh Employer sửa tin OPEN: `before.status == 'OPEN'` -> `after.status == 'PENDING_REVIEW'`, cập nhật nội dung hợp lệ và tăng version.
     + Nhánh Admin ẩn tin: `admin()` -> `before.status in ['OPEN', 'CLOSED']` -> `after.status == 'HIDDEN'`, ghi `reviewReason`, `reviewedBy`, `reviewedAt`.
2. **Data Layer (`JobPostRepository.java`):**
   - Thêm phương thức `close(String uid, JobPost expected)`.
   - Thêm phương thức `hide(String uid, JobPost expected, String reason)`.
   - Bổ sung xử lý trong `submit(String uid, LocalJobDraft original)`: Cho phép `old.exists() && "OPEN".equals(old.getString("status"))` chuyển sang `PENDING_REVIEW` với version mới.
3. **ViewModel (`JobPostViewModel.java`):**
   - Thêm phương thức `closePost()`.
   - Thêm phương thức `hidePost(String reason)`.
   - Quản lý trạng thái nhập lý do (cho cả Từ chối và Ẩn tin) với nhãn tiêu đề tương ứng (`actionTitle`).
4. **UI Presentation (`JobPostActivity.java` & `activity_job_posts.xml`):**
   - Nút "Đóng tin" (`jpClose`): Hiện khi Employer xem tin `OPEN`.
   - Nút "Sửa tin" (`jpEdit`): Mở rộng hiện khi Employer xem tin `REJECTED` HOẶC `OPEN`.
   - Nút "Ẩn tin" (`jpHide`): Hiện khi Admin xem tin `OPEN` hoặc `CLOSED`.
   - Hộp thoại xác nhận thao tác và thông báo kết quả thân thiện cho người lớn tuổi.
