# Kiểm tra C03 — liên kết và đổi số, giữ UID

24/10/2026 — **C03 chờ người dùng kiểm tra. Mọi ô dưới đây chưa xác minh.** C01 `ffcc6a2` và C02 `82c1edf` đã duyệt; không thay thế hồi quy trên mã C03.

## Chuẩn bị

1. Review [C03-FILES](C03-FILES.md); tự Sync/build/chạy bằng Android Studio. Nếu lỗi, gửi lỗi đầu tiên trong Build Output đã che thông tin riêng. Codex không chạy build/test/lint/emulator hoặc gửi SMS.
2. Chuẩn bị Google-only và phone-only. Ca link thành công cần Google/số **chưa thuộc UID khác**; chuẩn bị credential đã thuộc UID khác cho ca xung đột. Dùng tài khoản thuộc quyền kiểm soát của người kiểm tra, không tự xóa user Console để chuẩn bị dữ liệu.
3. Giữ cấu hình C01/C02 đã hoạt động: đúng project/package/Web client, SHA chứng thư, Play services, vùng Vietnam, quota/billing SMS. C03 không yêu cầu Console/backend mới.
4. Tự đối chiếu UID/provider/phone trước và sau trong Firebase Console → Authentication → Users. Chỉ ghi kết quả “UID giữ nguyên/khác” trong báo cáo công khai; không đưa UID, số thật, email, OTP, verificationId, token, credential hoặc nguyên exception vào báo cáo/log chia sẻ.

## Các ca cần thực hiện

- [ ] **C03-01** Google-only thêm số VN chưa thuộc UID khác: entry → review E.164/đồng ý → OTP đúng/tự xác minh → có cả hai provider; UID giữ nguyên; số hiển thị từ Firebase.
- [ ] **C03-02** Phone-only thêm Google chưa thuộc UID khác: picker → nhận biết Google đã chọn → Xác nhận → hai provider, cùng UID. Quay lại trước xác nhận không link.
- [ ] **C03-03** Đăng xuất rồi đăng nhập lần lượt từng provider đã link; cùng UID ban đầu trong Console. Cleanup Google C01 vẫn hoạt động.
- [ ] **C03-04** Link số đã thuộc UID khác: báo xung đột, giữ phiên/UID/provider; không merge/xóa/unlink/sign-in sang user kia.
- [ ] **C03-05** Link Google đã thuộc UID khác: báo xung đột, giữ phiên. Không hứa link trực tiếp hai user có sẵn; đã có Google thì không có nút thay Google.
- [ ] **C03-06** Đổi số bằng Google reauth đúng: thấy số hiện tại, chọn phương thức, xác nhận Google, reauth thành công mới nhập số mới/OTP; UID và Google giữ nguyên.
- [ ] **C03-07** Google reauth sai: lỗi, không đổi phiên/số, không mở số mới. Hủy picker/no credential/provider lỗi/xoay lúc picker có đường chọn lại, không tự link.
- [ ] **C03-08** Reauth OTP số cũ lấy từ Firebase, không được sửa sang số tùy ý. Review/đồng ý trước gửi; OTP đúng/tự xác minh mới mở số mới. Có cả hai provider thì được chọn cả hai cách reauth.
- [ ] **C03-09** Không truy cập provider cũ: giải thích chưa thể đổi số an toàn, quay lại được; không có hỗ trợ giả/đường bỏ qua reauth chỉ bằng số mới.
- [ ] **C03-10** Số mới trùng số hiện tại: báo trước SMS. Số mới thuộc UID khác: xung đột, giữ số cũ/UID/Google. Provider đã link: đọc lại thực tế, không thay thế.
- [ ] **C03-11** Mỗi challenge link phone/reauth phone/update phone: mã sai/đúng/hết hiệu lực/mã cũ sau resend; tự xác minh; resend/cooldown; timeout; CAPTCHA quay về/hủy; mất mạng/phục hồi; quota/cấu hình nếu có điều kiện. Không spam SMS để gây quota.
- [ ] **C03-12** Sửa số, Back/Quay lại trước mutation: bỏ challenge, giữ tài khoản. Callback cũ/trễ/đồng thời với nhập tay không làm sai bước; OTP số cũ không được update số mới.
- [ ] **C03-13** Requires-recent-login: quay về chọn reauth, bỏ credential/challenge cũ; reauth rồi xác minh số mới lại. Không bỏ qua/tự retry mutation.
- [ ] **C03-14** Mutation đã gửi: Back giải thích đang hoàn tất; Quay lại/đăng xuất khóa. Home/xoay/đóng Activity không rollback/signOut; kết quả tiếp tục khi mở lại cùng process.
- [ ] **C03-15** Mutation thành công nhưng reload/token lỗi: báo Auth đã thành công, chưa đọc đầy đủ; Đọc lại kết quả chỉ reload/refresh, không SMS/lặp mutation. Chưa báo claim/hồ sơ đồng bộ khi refresh chưa đạt.
- [ ] **C03-16** Kết quả mutation chưa rõ do mạng: yêu cầu đọc lại trước thao tác mới; không giả hủy server. Sau đọc lại hiển thị dữ liệu Firebase hiện tại, không tự kết luận mutation thành công nếu chưa biết.
- [ ] **C03-17** Xoay/Home tại entry/review/SMS/OTP/Google confirm/reauth/mutation/refresh: ngữ cảnh trong bộ nhớ, không reset cooldown/tự resend; reattach chỉ cửa sổ C02, không force token khi foreground.
- [ ] **C03-18** Process chết trước/sau mutation có thể áp dụng: mở lại đọc FirebaseUser/reload/token; không lưu/phục hồi số/UID/OTP/token/verificationId/resend token; không tự SMS/lặp mutation. Không tái hiện được thì giữ chưa xác minh.
- [ ] **C03-19** Phiên signOut/đổi UID/invalid khi callback cũ về: không áp kết quả vào UI/tài khoản mới, không tạo mutation mới khi Task trước còn chạy.
- [ ] **C03-20** Đổi số thành công: số mới đăng nhập cùng UID; Google vẫn cùng UID nếu đã link; Console cho thấy số cũ không còn thuộc UID này. **Không thử đăng nhập số cũ bằng SMS thật trước khi hiểu Firebase có thể tạo tài khoản mới**, không coi đó là phục hồi user cũ.
- [ ] **C03-21** Hồi quy C01/C02: Google/phone login, OTP tay/tự động, hủy/lỗi/mạng, resend/sửa số/Back/đổi phương thức, CAPTCHA, xoay/Home, phục hồi phiên, đăng xuất. Link/update giữ màn tài khoản trong lúc chờ OTP.
- [ ] **C03-22** Font 200%, email/số/lỗi dài, ngang/tablet/chia cửa sổ, bàn phím/insets/TalkBack: cuộn đủ, không cắt nội dung; chữ chính 20sp/tiêu đề 28sp/vùng chạm ≥56dp theo style C01/C02. Không có TTS/nút nghe thuộc C22.
- [ ] **C03-23** Không thay Firestore/Rules/profile/role/retirementStatus/quyền ứng tuyển; chưa đồng bộ C04; không có dữ liệu nhạy cảm mới trong log/saved state/preferences.

## Ghi nhận và tồn đọng

Ghi mã ca, bản chạy, thiết bị/API, điều kiện tái hiện, kết quả thực tế đã che thông tin riêng. Review tĩnh không đủ đánh dấu đạt. Chỉ nghiệm thu C03 sau xác nhận người dùng.

Giữ nguyên C02-F01: SMS đôi khi tiếng Anh/tên “Your Firebase App”; C03 không sửa. Các nhánh timeout/thử lại/callback trễ C01 và nhánh khó tái hiện C02 giữ trạng thái checklist cũ, không tự đánh dấu giải quyết hoặc suy rộng kết quả đã duyệt.

## Chốt C03 — 25/10/2026

**Người dùng xác nhận đã kiểm tra lại toàn bộ C03 và phê duyệt.** Xác nhận này thay thế trạng thái “chờ người dùng kiểm tra/chưa nghiệm thu C03” trong các phần bàn giao trước. Các ca C03-01 đến C03-23 trong C03-CHECKLIST được ghi nhận đạt theo xác nhận tổng thể của người dùng; Codex không tự chạy kiểm thử hoặc bổ sung kết quả đo/thiết bị không được cung cấp.

Phạm vi được duyệt: liên kết Google/phone vào cùng UID, đăng nhập lại bằng các provider đã liên kết, xử lý xung đột giữ tài khoản hiện tại, xác thực lại và đổi số giữ UID, xử lý hủy/lỗi/vòng đời, giao diện và hồi quy C01/C02 theo checklist. Không có dữ liệu tài khoản thật được thêm vào báo cáo.

Giữ nguyên các mục theo dõi riêng từ C01/C02, đặc biệt C02-F01 về ngôn ngữ SMS và tên “Your Firebase App”; việc duyệt C03 không có nghĩa các mục đó đã được sửa. Đồng bộ số vào hồ sơ/claim và Rules tích hợp C04 vẫn thuộc bước sau; TTS thuộc C22.

Người dùng tự review và commit C03 trước khi triển khai luồng tiếp theo. Lần ghi nhận này chỉ sửa tài liệu; Codex không sửa code, build/test, stage, commit, push hoặc deploy.
