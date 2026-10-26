# C08 — cấu hình Console và dữ liệu nghiệm thu

26/10/2026. Codex chỉ thay file trong repo, **chưa Publish/deploy hoặc kiểm thử Rules**. Thực hiện trên dự án `grandmaassist-a1dfa`, Firestore `(default)`.

## 1. Publish Rules

Firestore → Rules → thay bằng **toàn bộ** `firebase/firestore.rules` hiện tại → Publish. File giữ mốc sửa C07 `2026-10-26 r4`; thay đổi C08 nằm ở `match /areas/{code}` và `match /jobCategories/{id}`:

- Khách được get danh mục/khu vực active, list với filter `active == true`, tối đa 100 mỗi query.
- Người đăng nhập vẫn được đọc danh mục active như trước; riêng query areas không thêm hạn mức mới cho code hồ sơ C04 vốn không truyền limit.
- Không ai được ghi catalog từ app. Không mở đọc danh mục inactive.
- Giữ nguyên jobs/history r4: khách chỉ đọc tin OPEN; pending/rejected/history vẫn theo quyền chủ tin/Admin, không có quyền ghi mới.

Không đổi Storage Rules, `google-services.json`, provider, SHA hoặc dữ liệu hồ sơ. Nếu chỉ tải danh sách mà bộ lọc khách chưa được Publish, danh sách OPEN vẫn có thể tải nhưng danh mục sẽ báo lỗi cấu hình.

## 2. Tạo bốn composite index mới

### Đi đến đúng hộp thoại trong ảnh

1. Mở Firestore → tab **Indexes** → chọn **Manual** ở bên phải.
2. Nhấn **Add index**.
3. Ở bước **Select index type**, chọn **Create structured index** → **Next**.
4. Ở bước **Configure index** như ảnh: nhập **Collection ID** là `jobs`; **Query scopes** chọn `Collection`.
5. Trong **Fields to index**, bấm vào từng ô có chữ mờ **Field path** rồi nhập tên trường. Dropdown bên phải của mọi dòng giữ **Ascending**.

Tên trường phân biệt hoa/thường: nhập `expiresAt`, `categoryId`, `areaCode` đúng như dưới. Đây là tên field, không nhập giá trị `OPEN`, tên khu vực hoặc tên loại việc. Bốn index được tạo riêng từng lần, không gộp tất cả vào một index.

### C08-I01 — danh sách tất cả việc

Hộp thoại có sẵn hai dòng, không cần bấm Add field:

| Dòng | Field path | Dropdown bên phải |
| --- | --- | --- |
| 1 | `status` | Ascending |
| 2 | `expiresAt` | Ascending |

Giữ Collection ID `jobs`, Query scopes `Collection` → nhấn **Create**.

### C08-I02 — lọc loại việc

Quay lại danh sách → **Add index** → **Create structured index** → **Next**. Nhập lại Collection ID `jobs`. Bấm **Add field một lần** để có ba dòng:

| Dòng | Field path | Dropdown bên phải |
| --- | --- | --- |
| 1 | `status` | Ascending |
| 2 | `categoryId` | Ascending |
| 3 | `expiresAt` | Ascending |

Query scopes `Collection` → **Create**.

### C08-I03 — lọc khu vực

Tạo index mới như trên. Collection ID `jobs`, bấm **Add field một lần**:

| Dòng | Field path | Dropdown bên phải |
| --- | --- | --- |
| 1 | `status` | Ascending |
| 2 | `areaCode` | Ascending |
| 3 | `expiresAt` | Ascending |

Query scopes `Collection` → **Create**.

### C08-I04 — lọc cả khu vực và loại việc

Tạo index mới. Collection ID `jobs`, bấm **Add field hai lần** để có bốn dòng:

| Dòng | Field path | Dropdown bên phải |
| --- | --- | --- |
| 1 | `status` | Ascending |
| 2 | `categoryId` | Ascending |
| 3 | `areaCode` | Ascending |
| 4 | `expiresAt` | Ascending |

Query scopes `Collection` → **Create**.

### Nếu hiện lỗi hoặc nút Create bị mờ

- **Field path must be unique:** mỗi dòng phải có tên trường khác nhau. Trong ảnh hai ô Field path đang trống; điền đầy đủ các tên của index đang tạo rồi kiểm tra lại. Không thêm một field hai lần.
- Có dòng thừa: xóa dòng đó nếu UI cho phép, hoặc đóng hộp thoại và tạo lại đúng số dòng. Không để dòng trống rồi nhấn Create.
- Index đã tồn tại: đối chiếu đúng Collection ID, fields, chiều và scope; nếu đã đúng thì dùng index đó, không cần tạo trùng.
- Không nhập `C08-I01`… vào form: đó chỉ là mã hướng dẫn, không phải tên collection/field.

### Kiểm tra sau khi tạo

Chờ cả bốn index mới chuyển từ trạng thái đang tạo sang **Enabled**. Giữ hai index C07 đang có trong nền ảnh (`employerId + updatedAt DESC` và `status + updatedAt DESC`); không sửa/xóa chúng. Sau khi xong có tổng cộng **6 index cho jobs** theo file repo, nếu chưa tạo thêm index khác.

Không cần tự thêm dòng `__name__` cho bốn form này: khóa cuối trong file JSON là `__name__ ASCENDING`, cùng chiều field cuối expiresAt. Console quản lý khóa tên tài liệu cuối index. Mọi field được nhập thủ công ở trên đều chọn **Ascending**, không chọn Descending, Array contains hoặc scope Collection group.

Nguồn chính xác: `firebase/firestore.indexes.json` hiện có tổng cộng sáu index. Các index C08 dùng ASCENDING cho cả expiresAt và ID; không chọn Array contains hoặc Vector. Query code: `status == OPEN`, hạn sau thời điểm bắt đầu tải, tùy chọn category/area, orderBy expiresAt ASC + document ID ASC, limit 20, startAfter document cuối.

Nếu Firebase trả thiếu index, kiểm tra bộ lọc đang áp dụng và bốn cấu hình trên. Thông báo app phân biệt lỗi index với danh sách rỗng; không bỏ Rules hoặc ép dùng cache để xem như server đã thành công.

## 3. Chuẩn bị dữ liệu bằng C06/C07

1. Người thuê tạo/gửi tin với hạn còn tương lai, địa chỉ rõ, danh mục/khu vực active.
2. Admin chấp thuận thành OPEN. Tạo thêm một tin PENDING_REVIEW và một REJECTED để kiểm tra không xuất hiện công khai.
3. Dùng ít nhất hai khu vực/hai loại việc để kiểm bốn tổ hợp lọc. Muốn kiểm phân trang cần trên 20 tin OPEN còn hạn phù hợp bộ lọc; dùng dữ liệu demo không chứa thông tin riêng.
4. Không tạo jobs trực tiếp bằng một document thiếu schema/history. Dùng luồng gửi/duyệt đã nghiệm thu; không thay status bằng Console để né quy trình.
5. Sync/Run trong Android Studio. Đăng xuất → Xem việc không cần đăng nhập; đăng nhập SEEKER → Việc khác. Kiểm theo [C08-CHECKLIST](C08-CHECKLIST.md).

## SQLite trên thiết bị

`public_jobs.db` v1: cached_jobs, cached_categories, cached_areas, unavailable_jobs. cached_jobs chỉ lưu public projection, version, cachedAt và dữ liệu phục vụ lọc/hạn. unavailable_jobs giữ ID/thời điểm vô hiệu để response cũ không đưa tin đã bị từ chối xem vào cache trở lại. Đây không phải collection Firestore.

Không có migration của `job_drafts.db`; app tạo DB cache mới riêng. DB mới và journal/WAL/SHM được loại khỏi cloud backup và device transfer. Không uninstall/clear data để cài bản C08, vì thao tác đó sẽ làm mất nháp C06.

Căn cứ API: [quản lý index Firebase](https://firebase.google.com/docs/firestore/query-data/indexing), [Firestore cursors](https://firebase.google.com/docs/firestore/query-data/query-cursors), [query cấu trúc](https://firebase.google.com/docs/firestore/query-data/queries), [SQLite Android](https://developer.android.com/training/data-storage/sqlite).
