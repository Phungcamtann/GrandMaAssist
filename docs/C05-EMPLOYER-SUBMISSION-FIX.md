# C05 — sửa đường gửi xác minh danh tính người thuê

**Cập nhật hiện hành 26/10/2026:** người dùng xác nhận nút vẫn mờ, yêu cầu bỏ vùng ghim và trả vị trí cũ. Đã đưa Gửi hồ sơ/thông báo/tiến trình/Dừng chờ gửi về khối gửi trong ScrollView, bỏ toàn bộ logic di chuyển/ghim footer. Khi đang soạn, nút có thể bấm để kiểm điều kiện; chỉ vô hiệu trong lúc sending thật. Thiếu điều kiện hiển thị hộp thông báo cụ thể trước khi mở xác nhận gửi. Các mô tả footer/EF06/EF08 phía dưới giữ lịch sử và đã được thay bằng quyết định này. Bản hiện hành chưa được người dùng nghiệm thu.

26/10/2026. Nền local HEAD `313c478` — Luồng duyệt tin tuyển dụng; working tree sạch trước sửa. Người dùng báo đã đọc QR CCCD nhưng không gửi được xác minh người thuê và thấy phần Cách xét duyệt dễ nhầm với nghỉ hưu; xác nhận cụ thể **nút Gửi hồ sơ bị mờ**, chưa phải báo lỗi sau upload. Bản sửa này chờ người dùng Sync/Run/nghiệm thu.

## Đối chiếu nguyên nhân

Code/Firestore Rules trước sửa đã tách EMPLOYER đúng: request ở identityRequests, evidenceType IDENTITY_SAMPLE, đúng một ảnh identity.jpg; không đòi retirementStatus hoặc ảnh bằng chứng nghỉ hưu. Tuy nhiên giao diện dùng chung nút Cách xét duyệt và checkbox đồng ý với SEEKER, đồng thời nút gửi bắt buộc consent cho cả hai. Vì vậy người thuê đã quét QR vẫn bị khóa nếu chưa tick checkbox chung. Màn trước sửa chỉ giải thích thiếu số/hồ sơ, không giải thích điều kiện đồng ý/ảnh chưa xác nhận, nên không thể kết luận chỉ từ nút bị mờ rằng backend đang đòi nghỉ hưu. Chưa có log/runtime xác nhận riêng điều kiện nào thiếu trên thiết bị người dùng.

## Thay đổi

- EMPLOYER ẩn toàn bộ bằng chứng nghỉ hưu, nút Cách xét duyệt và checkbox đồng ý chung. Phần gửi có một dòng ngắn Chỉ xác minh danh tính người thuê, không cần giấy tờ nghỉ hưu.
- Khi CCCD đã được xác nhận/đọc QR, có số đã xác minh/đồng bộ và hồ sơ đủ/hoạt động, nút Gửi hồ sơ dùng được mà không cần tick checkbox ẩn hoặc xem hướng dẫn. Bấm nút mở hộp xác nhận danh tính; chỉ nút **Đồng ý và gửi** mới ghi nhận consent và gọi gửi. Hủy không cấp consent hoặc gửi dữ liệu.
- ViewModel có một nguồn điều kiện submissionRequirement dùng chung cho nút, dòng giải thích và hàm gửi. Nêu rõ thiếu số, hồ sơ, ảnh chưa đọc QR, bằng chứng (chỉ SEEKER), consent, trạng thái chờ hoặc yêu cầu tải lại; không thêm điều kiện nghỉ hưu cho EMPLOYER.
- Repository stage kiểm đúng loại/số ảnh: SEEKER 1 CCCD + 1–3 ảnh nghỉ hưu với enum bằng chứng hợp lệ; EMPLOYER chỉ một CCCD với IDENTITY_SAMPLE. Luồng upload/transaction/Rules/quyền Admin vẫn như C05.
- SEEKER giữ Cách xét duyệt/checkbox/bằng chứng và các bước đã chốt; không bỏ sự đồng ý hoặc tự chấp thuận kết quả xác minh của bất kỳ vai trò nào.

## File thay đổi

- VerificationViewModel.java: điều kiện tập trung, xác nhận gửi riêng EMPLOYER, guard send.
- VerificationActivity.java: hộp xác nhận theo vai trò, trạng thái nút/giải thích và ẩn phần hướng dẫn chung với EMPLOYER.
- VerificationRepository.java: kiểm loại yêu cầu và số ảnh tại stage.
- activity_verification.xml và verification_strings.xml: thông tin ngắn và nội dung xác nhận danh tính.
- Báo cáo này, C05-FILES và C05-CHECKLIST.

## Người dùng kiểm tra lại

- [ ] EF01 EMPLOYER vào Hồ sơ → Xác minh: không có bằng chứng nghỉ hưu, Cách xét duyệt hoặc checkbox nghỉ hưu; có ghi rõ chỉ xác minh danh tính.
- [ ] EF02 Đủ số/hồ sơ, chụp/chọn CCCD → Dùng ảnh và đọc QR: thông tin đúng, nút gửi sáng. Nếu nút chưa sáng, dòng ngay trên nút chỉ đúng điều kiện còn thiếu.
- [ ] EF03 Gửi → Quay lại: không upload/tạo request. Gửi → Đồng ý và gửi: tạo identityRequests SUBMITTED với evidenceType IDENTITY_SAMPLE và một identity.jpg, không tạo retirementRequests/bằng chứng nghỉ hưu cho EMPLOYER.
- [ ] EF04 Thiếu số/hồ sơ/QR, hủy ảnh, xoay khi chuẩn bị/xác nhận, lỗi gửi và thử lại: giữ dữ liệu đúng, không vượt điều kiện thật, không gửi trùng hay gửi sau đổi UID.
- [ ] EF05 Admin chọn Danh tính người thuê, đọc và duyệt thành VERIFIED; SEEKER vẫn cần bằng chứng nghỉ hưu và đồng ý, gửi/xét như trước.

Codex chỉ rà tĩnh, không build/test/Sync/app hoặc kiểm thử Rules; không sửa Firebase Console/Rules/Storage, không commit/push. Người dùng chỉ cần Sync/Run để kiểm bản sửa này; không cần đổi dữ liệu hoặc cấp thêm quyền để mở nút gửi.

## Bổ sung sau khi người dùng kiểm lại — 26/10/2026

Người dùng báo vẫn chưa thấy nút Gửi hồ sơ và không thấy thông báo thiếu điều kiện, đồng thời yêu cầu sau đọc QR chỉ giữ nút sửa ảnh. Lần sửa consent trước chưa được coi nghiệm thu đạt. Rà cấu trúc XML xác nhận vùng gửi là vùng riêng cùng cấp vùng bằng chứng, không bị lồng trong vùng nghỉ hưu; nút và thông báo trước đó nằm cuối ScrollView sau ảnh/các khối, không có một vùng hành động luôn dễ thấy. Chưa có ảnh hoặc trạng thái runtime để kết luận duy nhất nguyên nhân trên thiết bị người dùng.

Đã bổ sung:

- Tách nút gửi, thông báo điều kiện/lỗi, tiến trình và Dừng chờ gửi thành một vùng hành động riêng. Màn dọc đủ chiều cao/cỡ chữ thường ghim vùng này phía trên điều hướng; ghi rõ visibility theo đúng trạng thái đang soạn của chủ hồ sơ. Hồ sơ đang SUBMITTED/chi tiết chỉ đọc/Admin không hiện nút gửi mới.
- Màn ngang, chữ lớn hoặc mở bàn phím đưa vùng hành động vào cuối nội dung cuộn để không chiếm hết màn hình. Sau đọc QR thành công cho EMPLOYER ở bố cục này, cuộn đến vùng gửi. Giới hạn chiều rộng đồng bộ nội dung hiện tại.
- Sau đọc QR, ẩn Chụp căn cước và Chọn ảnh căn cước; hiện Sửa ảnh căn cước. Nút sửa mở hai lựa chọn chụp/chọn lại. Hủy hoặc ảnh mới chưa đọc được giữ căn cước đã xác nhận trước đó; chỉ thay khi ảnh mới đọc QR thành công. Thay căn cước thành công đặt lại consent, không giữ sự đồng ý cũ cho ảnh mới.
- Khi đang có ảnh xem lại, chỉ hiện các thao tác Dùng ảnh và đọc QR/Bỏ ảnh xem lại. Khóa sửa ảnh khi đang xử lý hoặc bản gửi đã đóng băng để retry an toàn.

Ca kiểm bổ sung (chưa tự đánh dấu đạt):

- [ ] EF06 EMPLOYER đọc QR → hai nút chụp/chọn biến mất, có Sửa ảnh căn cước; nút gửi dễ thấy ở vùng cuối màn, hoặc tự cuộn tới trên màn nhỏ/chữ lớn. Nếu bị khóa có lý do sát nút.
- [ ] EF07 Sửa ảnh → hủy camera/gallery hoặc Bỏ ảnh xem lại giữ thông tin cũ; đọc QR mới thành công thay đúng thông tin, không còn ảnh cũ bị dùng ngầm.
- [ ] EF08 Xoay/chữ 200%/bàn phím: vùng gửi không trùng, không che toàn bộ nội dung, cuộn được. SEEKER vẫn thấy bằng chứng/đồng ý; Admin/chi tiết chỉ đọc không hiện gửi mới. Gửi/thử lại/hủy chờ vẫn dùng đúng ID và dữ liệu.

Chỉ cập nhật code/giao diện/tài liệu; không cần Publish lại Rules. Codex không tự chạy app/build/test hoặc commit.

## Sửa nút bị khóa và trả bố cục cũ — hiện hành

Nhánh render trước dùng `editable && requirement.isEmpty()` cho enabled của nút. Dù không có bằng chứng nghỉ hưu trong điều kiện EMPLOYER, bất kỳ thiếu sót về phone/profile/ảnh vẫn làm nút vô hiệu, nên người dùng không thể bấm để tìm nguyên nhân. Vị trí footer không giải quyết điều kiện này và đã được gỡ theo yêu cầu.

- Nút nằm đúng khối Gửi hồ sơ như trước, bên dưới phần xác nhận; không còn vSubmitFooter/vSubmitActions hoặc mã chuyển cha View theo xoay/chữ lớn.
- Đang soạn và chưa sending: nút bấm được. Nếu còn busy đọc ảnh/tải hoặc chờ chọn ảnh, bấm giải thích trạng thái; không âm thầm bỏ qua. Khi thực sự sending, nút khóa chống lặp và Dừng chờ gửi được bật riêng.
- Bấm nút kiểm submissionRequirement trước: nếu thiếu thì mở hộp Chưa thể gửi hồ sơ với đúng lý do. Home bổ sung profileRequirement chỉ rõ tên hiển thị/khu vực/kỹ năng/kinh nghiệm/buổi làm hoặc giới thiệu tùy vai trò; không chỉ báo chung Hồ sơ chưa đủ. Dòng điều kiện hiện tại ưu tiên điều kiện đang thiếu, không bị thông báo cũ che.
- Khi đủ dữ liệu mới mở xác nhận; EMPLOYER Đồng ý và gửi mới cấp consent rồi đi stage/upload/submit. Giữ toàn bộ kiểm tra dữ liệu thật, role, số điện thoại, trạng thái, version và Rules, không làm nút sáng bằng cách bỏ kiểm tra backend.
- Giữ cải tiến CCCD: sau đọc thành công chỉ hiện Sửa ảnh căn cước, hủy thay ảnh giữ thông tin cũ. SEEKER vẫn cần bằng chứng/đồng ý. Không cần sửa Rules/Console.

Ca kiểm hiện hành, thay ca footer trước đó:

- [ ] EF09 Nút ở vị trí cũ trong khối Gửi hồ sơ, không ghim cuối màn; sau quét QR bấm được khi chưa sending.
- [ ] EF10 Nếu thiếu trường, bấm có hộp thông báo chỉ rõ; hoàn thiện/lưu đúng trường rồi quay lại và quét QR nếu đã rời màn (nháp giấy tờ chỉ RAM), đủ điều kiện mở Đồng ý và gửi. Không tự gửi khi vẫn thiếu.
- [ ] EF11 Gửi thật chỉ một yêu cầu, nút khóa trong lúc sending và Dừng chờ gửi bấm được; sau lỗi phục hồi theo cùng bản/ID. Xoay, phóng chữ và hồi quy SEEKER/Admin vẫn đúng, không có footer trùng.

Chưa có runtime/build/test do Codex thực hiện; người dùng Sync/Run tự nghiệm thu. Không commit/push/deploy.
