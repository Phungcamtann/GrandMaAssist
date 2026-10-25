# File thuộc phạm vi C03

24/10/2026 — **Đã triển khai mã nguồn, chờ người dùng Sync/build/chạy và kiểm tra; chưa nghiệm thu C03.** Nền thực tế: HEAD `82c1edf` (C02), trước đó `ffcc6a2` (C01), working tree sạch. Không tìm thấy AGENTS.md trong dự án và hai thư mục cha đã kiểm tra.

| File | Nội dung |
| --- | --- |
| `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/AccountMethodsFlow.java` (mới) | Purpose, expected UID, generation; Google confirm; link/reauth/update; đọc lại kết quả |
| `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/AuthRepository.java` | Sở hữu coordinator bằng cùng FirebaseAuth; định tuyến, bảo vệ checkSession/listener/đăng xuất |
| `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/PhoneSignInFlow.java` | Tái sử dụng challenge C02, thêm review số cũ cố định từ Firebase |
| `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/viewmodel/AuthViewModel.java` | Truy cập coordinator và bắt đầu thao tác tài khoản |
| `app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/MainActivity.java` | Hành động tài khoản, picker dùng chung, render theo purpose, Back/process flag |
| `app/src/main/res/layout/activity_main.xml` | Phương thức đăng nhập, hành động/trạng thái; biểu mẫu phone dùng chung ngoài nhóm login |
| `app/src/main/res/layout/phone_entry.xml` | ID tiêu đề/giới thiệu theo ngữ cảnh |
| `app/src/main/res/layout/phone_review.xml` | ID tiêu đề review |
| `app/src/main/res/layout/phone_code.xml` | ID tiêu đề OTP |
| `app/src/main/res/values/strings.xml` | Tiếng Việt C03, xung đột, reauth, kết quả chưa rõ |
| `docs/C03-CHECKLIST.md`, `docs/C03-FILES.md` (mới), `docs/SETUP.md`, `README.md` | Bàn giao và hướng dẫn kiểm tra |

## Hành vi

- Google-only: nhập số VN → review/đồng ý → OTP/tự xác minh → `FirebaseUser.linkWithCredential`.
- Phone-only: Credential Manager hiện có → hiển thị Google đã chọn → xác nhận → `linkWithCredential`. Không quyết định cùng người từ tên/email.
- Đổi số: provider thực sự liên kết → `reauthenticate` → nhập/review/OTP số mới → `updatePhoneNumber`. Số cũ lấy từ Firebase, không thể sửa. Sai Google bị Firebase từ chối.
- C03 không gọi `signInWithCredential`, không signOut để link, không merge/xóa/unlink/rollback. Xung đột giữ phiên; hai user đã tạo riêng không được hứa liên kết trực tiếp.
- Expected UID/generation chỉ trong bộ nhớ; retire challenge trước bước mới. C03 dùng instance challenge riêng của cùng lớp C02; chỉ một luồng login hoặc quản lý tài khoản khả dụng, cùng FirebaseAuth. Không giữ Activity trong coordinator/repository/ViewModel.
- Trước mutation: Back/Quay lại bỏ challenge. Task đã gửi: khóa Quay lại/đăng xuất; Back giải thích đang hoàn tất. Task tiếp tục qua xoay/Home, giữ kết quả trong cùng process khi Activity đóng. Không giả hủy/rollback server.
- Thành công mutation → reload → force token refresh → snapshot Firebase. Reload/token lỗi: báo thay đổi đã thành công nhưng chưa đọc đầy đủ; chỉ retry đọc, không lặp mutation. Lỗi mạng/kết quả chưa rõ cũng yêu cầu đọc lại trước thao tác tiếp.
- Process chết: không phục hồi số/UID/OTP/credential/verificationId/resend token. Mở lại đọc phiên Firebase và reload/token qua checkSession, không tự SMS/mutation. Instance state chỉ có boolean ngữ cảnh.
- Không đổi dependency, asset/style/font, Console, Rules, schema profile, retirementStatus hoặc quyền ứng tuyển. C02-F01/các nhánh C01/C02 chưa xác minh vẫn mở.

## Điểm tích hợp C04

C03 chỉ quản lý Firebase Auth. C04 phải lấy claim `phone_number` từ token đã refresh của cùng UID, đối chiếu số Auth trước đồng bộ hồ sơ. Firestore Rules kiểm tra độc lập `request.auth.token.phone_number` với số liên hệ, không tin boolean UI. Auth thành công hoặc UI có phone không có nghĩa hồ sơ đã đồng bộ. C03 chưa ghi users/profile hoặc thay quyền nghiệp vụ.

## Tài liệu đã đối chiếu

- [Account linking](https://firebase.google.com/docs/auth/android/account-linking): cùng UID; credential thuộc user khác gây thất bại. Không dùng ví dụ merge/sign-in sang UID khác vì ngoài phạm vi.
- [FirebaseUser](https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseUser): link, reauthenticate, updatePhoneNumber; collision, provider đã link, credential sai/hết hạn, recent login.
- [Manage users](https://firebase.google.com/docs/auth/android/manage-users), [Phone Auth](https://firebase.google.com/docs/auth/android/phone-auth): reload/token, callback Activity, reentrant verification, quota/reCAPTCHA.
- [Credential Manager Google](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation): giữ button flow và xử lý hủy/lỗi C01, không nâng dependency theo mẫu mới.
- Trang linking cảnh báo [known issue #7675](https://github.com/firebase/firebase-js-sdk/issues/7675), liên quan anonymous → email/password và Email Enumeration Protection. Không suy diễn rằng Google/phone Java sẽ gặp hoặc đã hết lỗi; không tắt protection/đổi Console theo workaround. Linking lỗi giữ phiên, không chuyển sang sign-in hoặc ghi exception thật vào log.

## Giới hạn xác minh

Chỉ review tĩnh code/diff và tham chiếu tài nguyên. Chưa xác minh biên dịch, UI, SDK/backend, UID, SMS, collision, vòng đời hoặc hồi quy trên bản C03. Không chạy build/test/lint/emulator, gửi SMS, thay Console hay thao tác Git ghi. Người dùng làm [C03-CHECKLIST](C03-CHECKLIST.md) trong Android Studio, tự chọn file review/stage/commit sau đó.

## Chốt C03 — 25/10/2026

**Người dùng xác nhận đã kiểm tra lại toàn bộ C03 và phê duyệt.** Xác nhận này thay thế trạng thái “chờ người dùng kiểm tra/chưa nghiệm thu C03” trong các phần bàn giao trước. Các ca C03-01 đến C03-23 trong C03-CHECKLIST được ghi nhận đạt theo xác nhận tổng thể của người dùng; Codex không tự chạy kiểm thử hoặc bổ sung kết quả đo/thiết bị không được cung cấp.

Phạm vi được duyệt: liên kết Google/phone vào cùng UID, đăng nhập lại bằng các provider đã liên kết, xử lý xung đột giữ tài khoản hiện tại, xác thực lại và đổi số giữ UID, xử lý hủy/lỗi/vòng đời, giao diện và hồi quy C01/C02 theo checklist. Không có dữ liệu tài khoản thật được thêm vào báo cáo.

Giữ nguyên các mục theo dõi riêng từ C01/C02, đặc biệt C02-F01 về ngôn ngữ SMS và tên “Your Firebase App”; việc duyệt C03 không có nghĩa các mục đó đã được sửa. Đồng bộ số vào hồ sơ/claim và Rules tích hợp C04 vẫn thuộc bước sau; TTS thuộc C22.

Người dùng tự review và commit C03 trước khi triển khai luồng tiếp theo. Lần ghi nhận này chỉ sửa tài liệu; Codex không sửa code, build/test, stage, commit, push hoặc deploy.
