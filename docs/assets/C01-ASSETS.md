# Nguồn asset/font C01 — 23/10/2026

Hai PNG do người dùng cung cấp được sao chép nguyên byte; bản gốc ở thư mục Downloads không bị thay đổi. Bản trong repo là nguồn nguyên bản để truy vết, không resize/recolor/cắt/xóa nền. Launcher co giãn bằng drawable XML, không tạo thêm PNG biến đổi.

| Nguồn | File trong repo | Kích thước/ghi chú |
| --- | --- | --- |
| `C:\Users\CYBORG\Downloads\PrimaryLogo.png` | `app/src/main/res/drawable-nodpi/grandma_primary_logo.png` | 1177×355, `fitCenter` |
| `C:\Users\CYBORG\Downloads\AppIcon.png` | `app/src/main/res/drawable-nodpi/grandma_app_icon.png` | 396×384; inset tương đối cho vùng 66×64 trên canvas 108×108, giữ tỷ lệ 1.03125 ở mọi kích thước |
| `C:\Users\CYBORG\Downloads\Banner.jpg` do người dùng gửi | `app/src/main/res/drawable-nodpi/grandma_banner.jpg` | 1024×572; sao chép nguyên byte, `fitCenter` và giữ toàn bộ ảnh; không cắt banner từ screenshot |
| [Google G chính thức](https://developers.google.com/static/identity/images/g-logo.png) từ [branding guidelines](https://developers.google.com/identity/branding-guidelines) | `app/src/main/res/drawable-nodpi/google_g.png` | G màu gradient nguyên bản, giữ tỷ lệ trong vùng tối đa 20dp trên nút Google trắng |

## Font nguyên bản từ kho Google Fonts chính thức

Các font dưới được đóng gói cục bộ, không tải font lúc mở app. Giấy phép OFL tương ứng nằm trong `app/src/main/assets/licenses` và được đóng gói cùng APK.

| File đóng gói | Nguồn chính thức | Sử dụng |
| --- | --- | --- |
| `res/font/plus_jakarta_sans_variable.ttf` | [Plus Jakarta Sans](https://github.com/google/fonts/tree/main/ofl/plusjakartasans), `PlusJakartaSans[wght].ttf` | Trục wght 200–800, mặc định 400; wrapper chọn 700, TextAppearance tiêu đề cũng yêu cầu Bold/weight 700 và axis wght 700 |
| `res/font/be_vietnam_pro_regular.ttf` | [Be Vietnam Pro](https://github.com/google/fonts/tree/main/ofl/bevietnampro), `BeVietnamPro-Regular.ttf` | Nội dung |
| `res/font/be_vietnam_pro_semibold.ttf` | Cùng nguồn, `BeVietnamPro-SemiBold.ttf` | Nhãn/nút thông thường |
| `res/font/google_sans_variable.ttf` | [Google Sans](https://github.com/google/fonts/tree/main/ofl/googlesans), `GoogleSans[GRAD,opsz,wght].ttf` | Wrapper XML chọn weight 500 riêng nút Google theo branding hiện hành |

Google Sans đi kèm `googlesans-TRADEMARKS.md`; việc sử dụng font không hàm ý Google bảo trợ ứng dụng. Không sửa font hoặc nhãn hiệu. Không có giấy phép cấp lại cho logo GrandMa; đây là asset người dùng cung cấp cho dự án.

## SHA-256 của bản nguồn được đóng gói

```text
grandma_primary_logo.png       ED9FA446A77809B6EE64EF7D5F2066DF69F1CF7FFA8DCEF4D9F11FA01613FADF
grandma_app_icon.png           B5B5F15FF6A22B0D95EB90A37E5EFA399C25C75CF80453C88A1C34BE63AEEB5A
grandma_banner.jpg             05D94ECA597F7E81EC860638CEC761AF14390CAB2847615BB0AEA7AD6726413C
google_g.png                  D1CE9C2AF0B10A7333ABC99BC706F9A6A199E5B65BF3E3009624F076B8638E6A
plus_jakarta_sans_variable.ttf 89B3FB38AA0D275D7A731D0D817A4F1622B316B4D7FBDEDCF02EE9099FF68BC8
be_vietnam_pro_regular.ttf     CD1EF6E9D7DB28AD5CDB88A65CCBE693870E60D340B791F349D248342B4FE4C3
be_vietnam_pro_semibold.ttf    BD8E27EB02720B9D91E59E4F10A90878643219F25CE6A8D9A4F06A8A88D3BB71
google_sans_variable.ttf       D0A87D835A944B8B40D0E82A5651BB59AB97B936A2AEED5946EB57E7B2A3A90A
```

Hash của hai PNG và Banner.jpg trong repo đã đối chiếu bằng đọc file với bản Downloads và khớp. Screenshot và banner người làm vườn không được đưa vào giao diện; ảnh minh họa mới người dùng gửi đã thay thế. Không thiếu asset cho phạm vi giao diện đang triển khai. Người dùng đã duyệt giao diện mới; chưa suy ra kết quả cho launcher hoặc các trường hợp chưa xác nhận. Bản sửa chờ đăng xuất không thay asset hoặc giao diện.
