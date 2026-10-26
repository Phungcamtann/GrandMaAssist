# C07 — điều tra lỗi bị từ chối khi gửi tin

**Đã đóng lỗi theo xác nhận người dùng ngày 26/10/2026:** sau bản r4, gửi tin hoạt động bình thường và các luồng khác đã được duyệt. Commit `08d2211` đã khớp local/origin/main sau fetch; xem [C07-ACCEPTANCE](C07-ACCEPTANCE.md). Giữ các bước điều tra dưới đây làm lịch sử và giữ chẩn đoán tương thích APK hiện tại, không yêu cầu người dùng lặp lại các bước xử lý lỗi đã hết.

**Kết luận cập nhật 26/10/2026:** sau mã C07_WRITE_CONTENT_OR_HISTORY, người dùng cho phép kiểm tra Rules cục bộ. Đã tái hiện trên Emulator: commit bị chặn vì **vượt 1.000 biểu thức đánh giá**. Rút gọn jobShape/jobSkills ở r4 đã làm cùng ca gửi thành công và 24/24 ca tập trung đạt. Xem [bằng chứng và bản sửa r4](C07-EXPRESSION-LIMIT-FIX.md). Các đoạn r3/giả thuyết trước dưới đây giữ làm lịch sử, không còn là trạng thái kết luận hiện hành. Chờ Publish r4/nghiệm thu Firebase thật.

**Trạng thái mới nhất 26/10/2026:** người dùng thử sau bản giảm lượt đọc và vẫn nhận C07_SUBMIT_TRANSACTION; do đó chưa xác nhận giả thuyết giới hạn đọc là nguyên nhân. Đã bổ sung bản r3 chẩn đoán chỉ đọc trên chính các predicate Rules của owner (quyền, số, hồ sơ, hạn máy chủ). Cần cập nhật **cả app và Rules** cho r3; không được coi lỗi đã khắc phục khi chưa có kết quả người dùng.

**Cập nhật 26/10/2026:** người dùng xác nhận đã Publish C07, Sync/Run bản chẩn đoán và cung cấp ảnh mã **C07_SUBMIT_TRANSACTION**. Đã khoanh vùng lỗi về transaction, chưa có lỗi đọc riêng được ghi nhận. Đã sửa file Rules local để giảm truy cập tài liệu lặp trong kiểm tra ghi jobs/history; cần người dùng Publish lại toàn bộ file để nghiệm thu. Chưa kết luận chắc lỗi quan sát được do giới hạn đọc, vì chưa thực thi Rules và chưa có trace evaluator.

26/10/2026. Người dùng báo tài khoản EMPLOYER đã duyệt hồ sơ nhưng bấm Gửi duyệt nhận thông báo Chưa đủ quyền hoặc dữ liệu không hợp lệ. Kiểm tra Rules và tải lại. Đây là nhánh FirebaseFirestoreException PERMISSION_DENIED trong JobPostRepository; ảnh chưa xác định nó phát sinh khi đọc tài liệu nào hay khi commit. Chưa xác minh Rules đang Publish trên Console và chưa có Logcat để kết luận nguyên nhân gốc. Không ghi nhận lỗi đã được sửa thành công chỉ từ việc rà tĩnh.

## Đối chiếu

- C07 không lấy identityStatus VERIFIED làm điều kiện gửi; Rules kiểm EMPLOYER/ACTIVE, Auth phone khớp hồ sơ, dữ liệu nghề nghiệp, danh mục/khu vực active, schema tin, thời hạn và lịch sử ghi cùng transaction.
- Transaction đọc jobs/{id} trước để phục hồi retry, rồi users/adminAccess/employerProfiles/jobCategories/areas. Một lần đọc bị chặn cũng từng ra cùng thông báo như commit bị chặn.
- Chưa Publish Rules C07, danh mục/khu vực inactive hoặc dữ liệu không khớp Rules đều là khả năng, chưa coi khả năng nào đã được chứng minh. Composite index thiếu thường đi nhánh FAILED_PRECONDITION của query, không phải đủ căn cứ giải thích ảnh PERMISSION_DENIED này.

## Thay đổi chẩn đoán trong repo

JobPostRepository bọc lỗi đọc transaction theo mã C07_READ_JOB, C07_READ_USER, C07_READ_ACCESS, C07_READ_PROFILE, C07_READ_CATEGORY, C07_READ_AREA hoặc C07_READ_PROFILE_AREA. Nếu tất cả đọc qua được nhưng transaction thất bại, mã C07_SUBMIT_TRANSACTION. Giữ exception cause để còn phân loại lỗi mạng/phiên/xung đột đúng.

UI hiển thị mã bước đối với PERMISSION_DENIED. Debug Logcat tag GrandMaJobs chỉ ghi stage và enum error code; không ghi UID, ID tài liệu, số điện thoại, địa chỉ, token, nội dung, exception message hoặc dữ liệu giấy tờ. Không cấp quyền rộng hoặc tự sửa Rules để che lỗi.

Preflight Java bổ sung kiểm phoneVerifiedAt là Timestamp, userId của hồ sơ và employerType đúng enum, để các dữ liệu sai rõ ràng được báo PHONE/PROFILE trước commit, khớp các điều kiện Rules hiện hành. Không thêm yêu cầu duyệt danh tính hoặc thay đổi schema.

## Người dùng cung cấp kết quả để xác định bước tiếp theo

1. Xác nhận Firestore `(default)` của `grandmaassist-a1dfa` đã Publish toàn bộ file firestore.rules C07, có `match /jobs/{id}` và khối history.
2. Nếu đã Publish và vẫn lỗi: Sync/Run bản có chẩn đoán, mở nháp cũ và gửi lại một lần, không cần tạo lại nháp hoặc gỡ app.
3. Gửi mã C07_* trên màn hình và dòng Logcat lọc GrandMaJobs. Không cần gửi UID/số điện thoại/địa chỉ hoặc nội dung tin.
4. Từ mã bước, đối chiếu đúng Rules/dữ liệu đang áp dụng, rồi mới sửa điều kiện gây chặn. Không suy ra phải cấp Admin cho EMPLOYER hoặc bắt xác minh lại C05.

Thay đổi C05 đang có trong working tree được giữ nguyên. Bản điều tra này không thay Firebase Console/Rules/index/Storage, không Sync/build/test/app, không commit/push. Ca C07-04 còn chờ nghiệm thu gửi thật sau khi xác định và sửa nguyên nhân.

Tham khảo giới hạn và điều kiện Rules khi đối chiếu transaction: [Firebase Rules conditions](https://firebase.google.com/docs/firestore/security/rules-conditions), [Firestore quotas](https://firebase.google.com/docs/firestore/quotas). Chưa chạy kiểm thử Rules hoặc kết luận lỗi do vượt giới hạn truy cập tài liệu.

## Bản sửa Rules sau mã C07_SUBMIT_TRANSACTION

File `firebase/firestore.rules` có dấu mốc `C07 Rules revision 2026-10-26`. Chỉ thay khối C07:

- submittedJob đọc users/employerProfiles một lần và truyền dữ liệu cho jobEmployer, thay vì helper đọc lại.
- jobArea chỉ đọc tài liệu khu vực để kiểm active; không gọi exists rồi get cho cùng điều kiện. jobCatalog đọc mỗi category/area một lần và dùng lại cả trạng thái lẫn tên.
- jobHistoryCreate dùng let cho trạng thái trước/sau, không lặp get/getAfter ở từng phép so sánh; đồng thời đối chiếu action với trạng thái sau tương ứng.
- Theo đếm nhánh văn bản không trừ cache: kiểm ghi tin gửi cần tối đa 8 lời gọi đọc; kiểm history gửi lần đầu 2, gửi lại 3, quyết định Admin tối đa 5. Đếm tĩnh này không phải trace runtime. Nhánh cũ có thể vượt 10 nếu không được cache vì đọc trùng trong nhiều helper; tài liệu Firebase cho biết chỉ một số lượt đọc có thể được cache và quá hạn mức trả permission denied.
- Giữ role/ACTIVE/phone/profile/danh mục/nội dung/expiry, bất biến owner/createdAt, version và lịch sử bắt buộc cùng transaction. Tin chờ không công khai, client không tự đặt OPEN, history không sửa/xóa. C04–C06/Storage/JSON/index không đổi. Không thêm yêu cầu C05 VERIFIED để gửi tin.

Người dùng: mở đúng dự án `grandmaassist-a1dfa` → Firestore `(default)` → Rules → dán **toàn bộ file mới** → Publish. Nhánh sửa này chỉ cần Publish Rules; app chẩn đoán đã chạy trong ảnh nên không cần Sync/Run lại chỉ vì sửa file Rules. Gửi lại **nháp cũ**, không tạo ID mới để tránh trùng nếu đã có kết quả chưa rõ.

- [ ] PD01 Sau Publish, gửi tin từ nháp cũ → PENDING_REVIEW và history/1 đúng transaction.
- [ ] PD02 Retry, Admin duyệt/từ chối, sửa gửi lại vẫn đúng ID/version/history; không mở quyền rộng để vượt lỗi.
- [ ] PD03 Ca Rules âm tính cho role khác/tự OPEN/thiếu history/owner sai giữ bị từ chối; chỉ đánh dấu sau kiểm thật.

Nếu còn C07_SUBMIT_TRANSACTION, cần đối chiếu tiếp dữ liệu thực tế/Rules đang áp dụng, không được coi bản tối ưu đã chứng minh nguyên nhân hay đã sửa xong. Codex chỉ rà tĩnh/diff, không build/test/Rules execution hoặc Console/deploy/commit.

## r3 — định vị điều kiện thay vì tiếp tục phỏng đoán

Không tiếp tục thay schema hoặc bỏ điều kiện ghi để thử may rủi. Rules r3 tách jobAccount/jobPhone/jobProfile và dùng lại đúng các helper đó trong cả điều kiện ghi và kiểm tra chỉ đọc. Không tạo/lưu tài liệu chẩn đoán.

Sau một transaction gửi bị PERMISSION_DENIED ở bước ghi (không phải lỗi đọc đã có mã riêng), app đọc lần lượt các đường owner-only dưới `jobSubmissionDiagnostics/{uid}/checks`:

| Kết quả | Ý nghĩa |
|---|---|
| C07_CHECK_RULESET | Endpoint revision_20261026_3 chưa được phép: chưa áp dụng đầy đủ Rules r3 hoặc sai dự án/phiên |
| C07_CHECK_ACCOUNT | Predicate EMPLOYER/ACTIVE/noAdmin không đạt tại thời điểm kiểm tra |
| C07_CHECK_PHONE | Claim phone_number/phoneE164/FIREBASE_AUTH/phoneVerifiedAt theo request.time không đạt |
| C07_CHECK_PROFILE | Predicate tên hiển thị/userId/giới thiệu/loại người thuê/tên đơn vị/khu vực active không đạt |
| C07_CHECK_EXPIRY | Hạn nhận không lớn hơn request.time của máy chủ; không chỉ suy theo giờ điện thoại |
| C07_WRITE_CONTENT_OR_HISTORY | Các predicate nguồn và hạn đã qua kiểm tra sau lỗi; còn phải đối chiếu payload/lifecycle/lịch sử hoặc tình huống dữ liệu đổi giữa hai thời điểm |

Revision không đọc dữ liệu business. Account/phone/profile chỉ đọc dữ liệu người chủ đã có quyền đọc; expiry dùng timestamp.value(epochMillis). Mọi endpoint cấm list/write, cấm tài khoản khác đọc. Thành công trả snapshot không tồn tại là bình thường; không cần tạo collection/doc trên Console. Không có UID/token/nội dung/địa chỉ hoặc exception message trong log GrandMaJobs. Các lượt đọc chẩn đoán là RPC sau một lần gửi lỗi, không phải nhánh mở quyền ghi hoặc công khai tin; có thể phát sinh phí đọc Firestore bình thường. Nếu lỗi mạng trong chẩn đoán, báo lỗi mạng, không suy ra predicate thất bại.

App cũng kiểm rõ giới hạn tên hiển thị/giới thiệu/tên đơn vị/tên danh mục/tên khu vực lấy từ server, tránh generic permission khi metadata server vượt schema. Không thay giới hạn Rules.

Người dùng Publish **toàn bộ firestore.rules mới có revision r3**, rồi Sync/Run app mới. Gửi lại cùng nháp một lần, cung cấp mã mới trên màn hình hoặc dòng GrandMaJobs. Không cần sửa dữ liệu giấy tờ/duyệt lại C05 hoặc gỡ app. Sau khi có mã mới mới sửa điều kiện cụ thể. Không khẳng định mọi lần đọc trước transaction đều đã qua chỉ dựa trên mã gộp nếu SDK không giữ nguyên cause.

- [ ] PD04 App + Rules r3 cùng phiên bản; lỗi gửi được phân loại theo mã kiểm tra cụ thể, không tạo dữ liệu chẩn đoán hoặc tin thử.
- [ ] PD05 Quyền các endpoint: owner đọc đúng predicate; UID khác/list/create/update/delete bị từ chối; gửi/duyệt jobs/history không được nới điều kiện.

Rà tĩnh nguồn/khai báo và diff; không build/Sync/test/app/Rules execution, Console/deploy/commit. Tham khảo [timestamp.value](https://firebase.google.com/docs/reference/rules/rules.timestamp). Thay đổi C05 có sẵn được giữ nguyên.
