# C07 — gửi và duyệt tin tuyển

25/10/2026. **Người dùng đã duyệt bố cục, bổ sung địa chỉ và yêu cầu triển khai; mã nguồn C07 đã được bổ sung, chờ nghiệm thu.** Nền đã đối chiếu HEAD local và GitHub main cùng `afd789144d460754d17c8701464548a189541d85` — Soạn tin tuyển dụng. C06 được người dùng nghiệm thu theo C06-ACCEPTANCE. Không Sync/build/test hoặc thay đổi Console/commit/push. Bàn giao hiện hành: [C07-FILES](C07-FILES.md), [C07-CONSOLE-SCHEMA](C07-CONSOLE-SCHEMA.md), [C07-CHECKLIST](C07-CHECKLIST.md).

## Phạm vi

Căn cứ README UC11–UC13, bảng JobPost và thứ tự C06–C11: người thuê gửi bản xem trước thành PENDING_REVIEW; Admin đọc đúng phiên bản, chấp thuận thành OPEN hoặc từ chối thành REJECTED có lý do; chủ tin xem trạng thái/kết quả và sửa gửi lại tin bị từ chối. Tin chưa duyệt không công khai. C08 làm danh sách việc công khai/cache; C11 làm sửa tin đã duyệt, đóng/ẩn và lịch sử tương ứng. Không mở các thao tác C11 sớm chỉ để có đủ nút.

**Bổ sung đã được người dùng chốt ngày 25/10/2026:** người thuê nhập địa chỉ làm việc chính xác ngay khi soạn tin, lưu trong `LocalJobDraft.workAddress` và `JobPost.workAddress`. Tin OPEN hiển thị địa chỉ từ khi người tìm việc xem tin, không đợi thỏa thuận ca. `areaCode` vẫn dùng lọc/tìm việc; `workAddress` là nội dung công khai của tin đã duyệt. Không lấy địa chỉ từ CCCD hoặc tự coi địa chỉ hồ sơ là địa chỉ làm việc. Chi tiết thay đổi và tiêu chí tại [C07-ADDRESS-CHANGE](C07-ADDRESS-CHANGE.md). TTS và các nghiệp vụ còn lại giữ phạm vi đã chốt.

## Bố cục đã được duyệt, bổ sung địa chỉ

Người dùng đã chốt bố cục C07 và yêu cầu bổ sung địa chỉ làm việc chính xác. Dùng lại hệ màu/font/logo đã duyệt; không cần duyệt lại bố cục hoặc gửi lại toàn bộ ảnh quy tắc cho phần bổ sung này.

1. **Tin của tôi (Người thuê):** hai lựa chọn Nháp trên máy / Tin đã gửi. Mỗi thẻ tin đã gửi có tiêu đề, khu vực, tiền công, trạng thái và nút Xem; không nhồi toàn bộ nội dung. Nháp giữ đường vào offline như C06. Tin server có tải lại, trạng thái rỗng/lỗi, tải thêm; không trộn lỗi mạng thành danh sách trống.
2. **Form và xem trước nháp:** khối Địa điểm và lịch có Khu vực, ngay dưới là ô nhiều dòng Địa chỉ làm việc, gợi ý Số nhà, đường, phường/xã và tỉnh/thành; một dòng phụ Địa chỉ sẽ hiển thị trên tin đã duyệt. Bản xem trước hiện đầy đủ khu vực và địa chỉ. Bổ sung Gửi duyệt và xác nhận ngắn, nhắc địa chỉ hiển thị công khai; không thêm màn giải thích dài. Lưu nháp/Quay lại sửa vẫn có. Nếu thiếu OTP/hồ sơ thì nêu đúng mục cần hoàn thiện và có đường đến tài khoản/hồ sơ; quay lại không mất nháp.
3. **Chi tiết tin đã gửi:** phần đầu tiêu đề và trạng thái; các khối Công việc / Địa điểm và lịch / Yêu cầu / Tiền công và hạn nhận. Hiện đầy đủ địa chỉ làm việc, cho xuống dòng thay vì cắt mất nội dung. PENDING_REVIEW chỉ xem và tải lại. OPEN hiện Đã duyệt. REJECTED hiện lý do cùng Sửa và gửi lại. Không có ô nhập nhận xét ở màn người thuê.
4. **Admin:** mở thẻ Duyệt tin hiện có ở Tổng quan và thêm lối vào rõ ràng từ Xử lý, tách khỏi Kiểm tra hồ sơ C05. Danh sách chờ có tải lại/tải thêm; chi tiết chia khối như bản xem trước, hành động ở cuối. Chấp thuận chỉ cần xác nhận; Từ chối mới mở ô lý do bắt buộc. Tiến trình/lỗi sát hành động, không để người dùng cuộn tìm thông báo.

Topbar luôn logo + vai trò một dòng; điều hướng theo vai trò giữ nguyên. Nút tối thiểu 56dp, khoảng cách rõ, chữ nội dung theo rule, hỗ trợ chữ lớn/màn ngang/bàn phím. Các mục tương lai giữ khóa/Sắp có; không tạo số lượng tin giả.

## Thứ tự triển khai sau khi chốt bố cục

1. **Model, validation và quyền gửi:** thêm JobPost server tách LocalJobDraft; thêm workAddress ở nháp, form, bản xem trước, chi tiết chủ tin/Admin và schema server. Địa chỉ trim không rỗng, tối đa 500 ký tự khi xem trước/gửi; nháp dở được để trống. Đọc nháp C06 cũ thiếu trường thành chuỗi rỗng, không xóa hoặc chặn mở nháp; yêu cầu bổ sung khi xem trước/gửi. Tái dùng giới hạn C06, kiểm lại danh mục/khu vực active, hạn và tiền nguyên dương. Trước gửi cần EMPLOYER/ACTIVE, số Firebase Auth đã xác minh và đồng bộ, hồ sơ nghề nghiệp đủ. Không buộc identityStatus VERIFIED để gửi tin; đó là điều kiện của bước thỏa thuận theo v3. Không tin marker/cache C06 để cấp quyền ghi.
2. **Gửi an toàn:** kiểm tra phiên/token và dữ liệu server mới, gửi transaction với ID ổn định và version. Có ánh xạ nháp–tin và dấu vết phiên bản đã gửi để bấm lặp, mất mạng sau server commit, đóng/mở app hoặc lỗi cập nhật SQLite không tạo hai tin. Thay đổi schema SQLite phải có migration giữ nháp cũ, không DROP. Không xóa nháp trước khi xác nhận kết quả server; không coi transaction timeout là bằng chứng server chưa ghi. Khóa nội dung khi đang gửi; đổi UID vô hiệu callback cũ. Phân biệt retry cùng nội dung với sửa nội dung sau khi đã gửi.
3. **Tin đã gửi của người thuê:** query theo employerId hiện tại với giới hạn và phân trang; trạng thái/lý do lấy thật từ server. Gửi thành công chuyển sang chi tiết đúng tin. Tải lại hoặc quay lại từ tài khoản giữ nháp. Offline vẫn có nháp local; không nhận gửi/duyệt bằng cache.
4. **Admin duyệt:** dùng AdminAccess + users ACTIVE/ADMIN hiện hành; query PENDING_REVIEW có giới hạn/phân trang. Transaction đọc lại trạng thái/version/hạn và quyền; ghi quyết định cùng thông tin người duyệt/thời điểm. Không tự xử lý tin của mình. Hai Admin hoặc callback trễ không ghi đè quyết định mới; xung đột yêu cầu tải lại. Hết hạn trước lúc duyệt không được OPEN, có thông báo rõ; không tự tạo trạng thái EXPIRED.
5. **Sửa tin bị từ chối và gửi lại:** sửa đủ trường trên nền form C06, giữ jobId và baseVersion; transaction chỉ REJECTED → PENDING_REVIEW, tăng version, xóa thông tin quyết định hiện hành sau khi lưu lịch sử bất biến. Không được sửa OPEN/PENDING_REVIEW qua đường này. Danh mục/hạn/quyền phải kiểm lại như lần đầu.
6. **Rules, dữ liệu và bàn giao:** whitelist trường JobPost; bất biến ID/owner/createdAt; chuyển trạng thái và version đúng; thời gian server; chặn sửa quyết định/history bởi chủ tin. Tách lịch sử kiểm duyệt riêng khỏi nội dung công khai, ghi nguyên tử cùng quyết định/gửi lại. Public chỉ đọc OPEN theo đúng query, tin riêng chỉ chủ/Admin; không để một match rộng làm lộ tin chờ. Giữ nguyên C04/C05/Storage. Chuẩn bị Rules/index/schema đầy đủ và hướng dẫn người dùng Publish, không tự deploy. Hoàn thành C07-FILES, C07-CONSOLE-SCHEMA và C07-CHECKLIST.

## Điều kiện nghiệm thu dự kiến

- Người thuê đủ điều kiện gửi đúng một tin; thiếu OTP/hồ sơ, danh mục inactive, hết hạn, offline hoặc sai vai trò bị chặn đúng chỗ và giữ nháp.
- Nháp cũ mở/sửa/lưu được khi chưa có địa chỉ; xem trước/gửi yêu cầu địa chỉ hợp lệ. Địa chỉ giữ qua lưu, xoay, mở lại, retry và sửa gửi lại; Admin và chủ tin thấy đúng nội dung. Tin chưa duyệt không lộ địa chỉ cho người khác. C08 phải hiển thị địa chỉ ngay trong chi tiết tin OPEN, snapshot đơn của C12 giữ địa chỉ đúng phiên bản.
- Đổi UID, bấm lặp, xoay, rời màn khi đang gửi không tạo trùng hoặc nhận kết quả của tài khoản khác; retry sau kết quả chưa rõ đối chiếu lại ID server.
- Chủ tin thấy đúng tin của mình và trạng thái thật. Admin thấy danh sách chờ, xem nội dung, chấp thuận không cần lý do; từ chối cần lý do. Chủ tin đọc được lý do, sửa/gửi lại đúng ID/version; Admin duyệt bản mới.
- Tin REJECTED/PENDING_REVIEW không được khách/tài khoản khác đọc. Không cấp quyền chỉ bằng cache/UI; không tự áp kết quả kiểm thử Rules nếu chưa thực hiện.
- Cạnh tranh, hết hạn, mất quyền giữa thao tác, lỗi mạng hiển thị đúng và phục hồi; không báo duyệt thành công trước khi server hoàn tất. Ca khó chưa tái hiện được ghi theo dõi riêng.
- Hồi quy nháp C06, các vai trò và hồ sơ C05; chữ lớn/màn ngang/TalkBack/bàn phím theo checklist người dùng.

Codex chỉ rà tĩnh mã nguồn/XML/resources/diff sau triển khai. Người dùng Sync/Run/kiểm thử và tự commit; không tự stage/commit/push/deploy hoặc chỉnh Firebase Console.
