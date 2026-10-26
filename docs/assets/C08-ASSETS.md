# C08 — ảnh minh họa card công việc

26/10/2026. Bổ sung theo mẫu người dùng gửi. Bốn ảnh thật từ các ID Pexels trong bảng tham khảo thiết kế; đã mở xem từng file để đối chiếu. Không dùng ảnh AI, không suy đoán tuổi/địa điểm/danh tính hoặc gán người trong ảnh là người thuê/người làm của một tin.

| Loại việc | Asset trong drawable-nodpi | Nguồn |
| --- | --- | --- |
| GARDEN_CARE | job_illustration_garden.jpg | [Pexels 5028043](https://www.pexels.com/photo/5028043/) |
| KITCHEN_HELP | job_illustration_kitchen.jpg | [Pexels 10155499](https://www.pexels.com/photo/10155499/) |
| LIBRARY_HELP | job_illustration_library.jpg | [Pexels 9572674](https://www.pexels.com/photo/9572674/) |
| SHOP_HELP | job_illustration_shop.jpg | [Pexels 8422732](https://www.pexels.com/photo/8422732/) |

JPEG tải từ CDN chính thức `https://images.pexels.com/photos/{id}/pexels-photo-{id}.jpeg?auto=compress&cs=tinysrgb&w=800`, tổng khoảng 525 KiB. Chỉ minh họa loại việc, có nhãn trên ảnh; loại khác dùng icon vector sẵn có. ImageView hiển thị centerCrop trong khung cao 176dp; không đổi banner/logo/ảnh người dùng. Đóng gói trong APK để có ảnh khi offline.

Đã đối chiếu [Pexels License](https://www.pexels.com/license/) cho việc dùng ảnh trong app; không thể hiện sự bảo trợ của người/nhãn hiệu trong ảnh. Credits nguồn tại assets/licenses/C08-photo-credits.txt. Không thêm trường ảnh vào JobPost/Firestore/SQLite cho lần sửa UI này.

SHA-256 của các bản JPEG đóng gói:

```text
garden  a32586af1fe3c07d7b90cbe2fb2538f65f9cbb9c11983139875bea034ca0572a
kitchen c8661dce34189c23f215458781108cc1a42952e21a2c151df25c969abb8b0763
library d25bbac9e7f5fff28ffc30058c50d1c3326511a3c7a36202499da6095f326c63
shop    5d005799aca816705659b50e2a3d541a83552fbce0d84a7fc05d71c0edd88e42
```
