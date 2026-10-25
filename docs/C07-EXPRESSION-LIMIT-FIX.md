# C07 — xác định và sửa giới hạn 1.000 biểu thức Rules

26/10/2026. **Đã tái hiện lỗi và xác nhận bản sửa bằng Emulator: 24/24 ca đạt. Chờ người dùng Publish r4 và nghiệm thu trên Firebase thật.** Người dùng đã cho phép ngoại lệ kiểm tra Rules cục bộ lần này sau khi C07_WRITE_CONTENT_OR_HISTORY vẫn xuất hiện. Quyền này không thay thế nguyên tắc người dùng tự Sync/build/test ứng dụng ở các luồng khác.

## Bằng chứng nguyên nhân

Trước kiểm tra, đọc bản nháp đã lưu qua ADB ở chế độ chỉ đọc: ID có cấu trúc UUID, độ dài/type trường hợp lệ, baseVersion=0, lịch/đơn vị/nhu cầu thể lực qua các kiểm tra cấu trúc. Không chạy app/gửi request từ thiết bị và không xuất nội dung hoặc UID ra log. Bản nháp không được dùng làm dữ liệu kiểm thử; toàn bộ fixture Emulator là dữ liệu giả.

Emulator Firestore 1.22.0, JBR 21.0.10, REST localhost `127.0.0.1:8088`, project cố định `demo-grandmaassist-c07`. Các kiểm tra nguồn r3 đều được phép đọc nhưng commit giả với chính Rules trước sửa trả 403/PERMISSION_DENIED:

> Unable to evaluate the expression as the maximum of 1000 expressions to evaluate has been reached.

Vì vậy lỗi có thể tái hiện ở ngân sách **biểu thức đánh giá**, không chỉ ngân sách get/exists. Bản giảm đọc trước đó chưa giải quyết nguyên nhân này. Giới hạn 1.000 biểu thức được ghi trong [tài liệu Firebase](https://firebase.google.com/docs/firestore/quotas#security_rules). Không gán lỗi cho hồ sơ chưa duyệt hoặc yêu cầu người dùng cấp Admin/nhập lại tin.

## Thay đổi r4

Chỉ thay khối C07 trong `firebase/firestore.rules`, có mốc `C07 Rules revision 2026-10-26 r4`:

- jobShape lấy keys một lần, kiểm đúng 28 khóa và hasOnly whitelist. Với Map có khóa duy nhất, cách này tương đương yêu cầu đủ và chỉ đúng toàn bộ 28 trường, tránh hai lần dựng danh sách dài hasAll/hasOnly.
- Các kiểm tra chuỗi của payload dùng trực tiếp size + matches thay vì nhiều lần gọi helper ba tham số. matches chỉ áp dụng cho string, nên type sai vẫn bị từ chối. Nội dung bắt buộc không được chỉ có khoảng trắng; optional requirementsText giữ đúng giới hạn và cho trống.
- jobSkills kiểm list tối đa 8, không trùng, mỗi phần tử là chuỗi 1–60 ký tự bằng các nhánh trực tiếp; không gọi chuỗi helper skills/skillAt tám lần. Giữ nguyên helper skills của C04/C05, không sửa logic các luồng đó.
- Guard phân biệt tạo mới/cập nhật trước các kiểm tra tốn chi phí. Giữ nguyên role, phone, hồ sơ, danh mục, hạn theo server, version, chủ tin, snapshot và lịch sử bắt buộc cùng transaction.
- Giữ các endpoint chẩn đoán r3 chỉ đọc để tương thích app đã cài. Không cần thay APK chỉ vì bản r4 này; không thay schema/index/JSON/Storage hoặc hạ quyền bảo vệ.

Ca gửi giả trước sửa trả 403, sau sửa trả 200. Đã kiểm lại cùng bản r4 từ bộ script trong repo.

## Kết quả 24 ca tập trung

Chi tiết máy đọc được: [C07-RULES-REGRESSION-RESULTS.json](C07-RULES-REGRESSION-RESULTS.json).

- Gửi v1 cùng history, từ chối v2, gửi lại v3 và duyệt v4: đạt.
- Dữ liệu ở giới hạn 3.000 ký tự mô tả/500 địa chỉ/1.000 yêu cầu/500 lịch, 8 kỹ năng, 7 ngày, 3 yêu cầu thể lực; gửi/từ chối/gửi lại/duyệt: đạt.
- Tiếng Việt và nội dung nhiều dòng: đạt.
- UID khác không đọc tin chờ; khách đọc OPEN nhưng không đọc history: đạt.
- Thiếu history, tự đặt OPEN, tiền âm, tiêu đề trắng/quá dài, text sai type, kỹ năng trùng/sai type, thiếu trường bắt buộc, history lệch nội dung, ghi dưới chủ khác và từ chối không lý do: đều bị chặn đúng.

Đây là kiểm tra backend Rules cục bộ bằng dữ liệu giả, **không phải** đã chạy app Android hoặc chứng minh UI/runtime Firebase thật đã đạt. Không tự đánh dấu toàn bộ C07-CHECKLIST hoàn tất. Không thay đổi dự án thật, không dùng tài khoản thật, không commit/push/deploy.

Các ca gửi dùng REST atomic commit với cùng cặp writes và server timestamp transforms như SDK; không kiểm thử vòng retry/concurrency của SDK Android, phân trang hoặc các luồng Auth/C05 trong ngoại lệ này.

## Artifact tái hiện

- `tools/firestore/c07_rules_fixture.py`: fixture tổng hợp và REST helpers; cứng hóa localhost + project demo, không đọc credentials/project thật.
- `tools/firestore/c07_rules_regression.py`: bộ kiểm tra có cả ca chấp nhận lẫn từ chối; nạp Rules từ repo, chỉ xóa/reset dữ liệu Emulator của project demo cố định.
- Emulator JAR đặt ở thư mục tạm ngoài repo, kiểm SHA-256 từ manifest Firebase CLI chính thức trước dùng; không đưa binary/dependency vào Git. Java dùng JBR sẵn của Android Studio, không đổi JAVA_HOME hệ thống.

Chỉ chạy lại khi có nhu cầu kiểm Rules và được người dùng cho phép. Cần khởi động Firestore Emulator ở localhost:8088, project `demo-grandmaassist-c07`, Java 21; sau đó từ repo chạy `python tools/firestore/c07_rules_regression.py`. Không tự chạy Gradle hoặc Firebase deploy trong script. Phiên Emulator của lần điều tra được dừng sau kiểm tra.

## Người dùng áp dụng

1. Firebase `grandmaassist-a1dfa` → Firestore `(default)` → Rules.
2. Dán **toàn bộ** `firebase/firestore.rules` mới, kiểm mốc r4 rồi Publish.
3. Trên app hiện tại, gửi lại nháp cũ; không cần tạo lại ID, đổi tài khoản hoặc xác minh lại C05. Không cần Sync/Run mới chỉ cho Rules r4.
4. Xác nhận tin thành Chờ duyệt; dùng Admin từ chối/duyệt và kiểm kết quả theo checklist C07 còn chờ.
