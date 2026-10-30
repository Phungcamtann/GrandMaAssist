# DANH MỤC CHI TIẾT CÁC APIS, ĐOẠN MÃ NGUỒN VÀ DÒNG DỮ LIỆU HỆ THỐNG GRANDMAASSIST

> **Dự án:** GrandMaAssist — Ứng dụng hỗ trợ việc làm và trợ năng cho người cao tuổi  
> **Phiên bản:** v3.0 (Final Release)  
> **Mục đích tài liệu:** Mô tả toàn diện 8 APIs và dịch vụ nền tảng cốt lõi được khai thác trực tiếp trong mã nguồn ứng dụng, bao gồm: Mục đích nghiệp vụ, Trích dẫn đoạn mã nguồn Java thực tế đang chạy trong repo, và Phân tích chi tiết dòng di chuyển dữ liệu (Data Flow) từ đầu vào đến đầu ra.

---

## MỤC LỤC 8 APIS TÍCH HỢP

1. [API 1: Firebase Authentication API (Xác thực người dùng đa phương thức)](#api-1-firebase-authentication-api-xác-thực-người-dùng-đa-phương-thức)
2. [API 2: Android Credential Manager API (Google Sign-In 1-chạm hiện đại)](#api-2-android-credential-manager-api-google-sign-in-1-chạm-hiện-đại)
3. [API 3: Cloud Firestore API (Database NoSQL, Transactions & Server Aggregations)](#api-3-cloud-firestore-api-database-nosql-transactions--server-aggregations)
4. [API 4: Google ML Kit Barcode Scanning API (AI thị giác máy tính quét QR CCCD On-Device)](#api-4-google-ml-kit-barcode-scanning-api-ai-thị-giác-máy-tính-quét-qr-cccd-on-device)
5. [API 5: Android Text-To-Speech (TTS) API (Giọng đọc nhân tạo trợ năng tiếng Việt)](#api-5-android-text-to-speech-tts-api-giọng-đọc-nhân-tạo-trợ-năng-tiếng-việt)
6. [API 6: Google Maps Intent Navigation API (Bản đồ chỉ đường 1-chạm đến nơi làm việc)](#api-6-google-maps-intent-navigation-api-bản-đồ-chỉ-đường-1-chạm-đến-nơi-làm-việc)
7. [API 7: Android AlarmManager & Notification Channel API (Nhắc việc & Báo thức ca làm)](#api-7-android-alarmmanager--notification-channel-api-nhắc-việc--báo-thức-ca-làm)
8. [API 8: Android Activity Result Contracts & Image Processing API (Xử lý & Nén ảnh nơi làm việc)](#api-8-android-activity-result-contracts--image-processing-api-xử-lý--nén-ảnh-nơi-làm-việc)

---

## API 1: FIREBASE AUTHENTICATION API (XÁC THỰC NGƯỜI DÙNG ĐA PHƯƠNG THỨC)

### 1. Mục đích sử dụng trong đồ án
- Quản lý phiên đăng nhập và định danh duy nhất (`UID`) cho toàn bộ người dùng trong hệ sinh thái GrandMaAssist.
- Cung cấp phương thức xác thực số điện thoại thật viễn thông qua mã SMS OTP 6 số (`PhoneAuthProvider`), hỗ trợ người cao tuổi không có tài khoản email vẫn sử dụng được app.
- Tích hợp liên kết chứng thực với Google Credential để cấp quyền truy cập Firebase an toàn.

### 2. Đoạn mã nguồn thực tế sử dụng API
Trích xuất từ [`AuthRepository.java`](file:///e:/Dev/Study'/Mobie/GrandMaAssist/GrandMaAssist/app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/AuthRepository.java) và [`PhoneSignInFlow.java`](file:///e:/Dev/Study'/Mobie/GrandMaAssist/GrandMaAssist/app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/PhoneSignInFlow.java):

```java
// AuthRepository.java - Đăng nhập bằng AuthCredential (Google hoặc Phone OTP)
public Task<FirebaseUser> signInWithCredential(AuthCredential credential) {
    return auth.signInWithCredential(credential)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        return Tasks.forException(new Failure("Mã xác thực không hợp lệ hoặc đã hết hạn."));
                    }
                    return Tasks.forException(task.getException());
                }
                FirebaseUser user = auth.getCurrentUser();
                if (user == null) {
                    return Tasks.forException(new Failure("Không tìm thấy phiên làm việc người dùng."));
                }
                return Tasks.forResult(user);
            });
}

// PhoneSignInFlow.java - Gửi SMS OTP viễn thông qua PhoneAuthProvider
PhoneAuthOptions options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
        .setPhoneNumber(phoneNumberE164)
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                // Tự động xác thực nếu thiết bị đọc được tin nhắn SMS
                onSuccessCallback.accept(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                onErrorCallback.accept(e.getMessage());
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                // Lưu verificationId để đối soát khi người dùng nhập 6 số OTP
                currentVerificationId = verificationId;
                onCodeSentCallback.run();
            }
        })
        .build();
PhoneAuthProvider.verifyPhoneNumber(options);
```

### 3. Dòng di chuyển dữ liệu (Data Flow)

```text
[Người dùng nhập SĐT / Bấm Google]
       │ 
       ▼
[PhoneAuthProvider.verifyPhoneNumber / CredentialManager]
       │ (1) Gửi yêu cầu qua mạng di động / Google OAuth
       ▼
[Firebase Auth Server] ──(SMS gửi tới điện thoại)──> [Người dùng nhận mã OTP 6 số]
       │                                                         │
       │ (2) Gửi lại verificationId + mã OTP 6 số                │
       ▼                                                         ▼
[PhoneAuthProvider.getCredential(verificationId, otpCode)]
       │ 
       ▼
[FirebaseAuth.signInWithCredential(credential)]
       │ (3) Xác thực chữ ký mã hóa tại máy chủ Firebase
       ▼
[Trả về FirebaseUser] ──> Chứa UID định danh, Số điện thoại E.164, IdToken Claims
       │ 
       ▼
[ProfileRepository.load(uid)] ──> Phân luồng điều hướng giao diện theo vai trò
```

---

## API 2: ANDROID CREDENTIAL MANAGER API (GOOGLE SIGN-IN 1-CHẠM HIỆN ĐẠI)

### 1. Mục đích sử dụng trong đồ án
- Thay thế hoàn toàn thư viện GoogleSignInClient cũ (đã bị Google gắn cờ Deprecated).
- Triển khai chuẩn xác thực AndroidX Credential Manager hiện đại nhất của Android 14+, hỗ trợ người dùng đăng nhập Google an toàn chỉ bằng 1 chạm mà không cần nhập mật khẩu.

### 2. Đoạn mã nguồn thực tế sử dụng API
Trích xuất từ [`AccountMethodsFlow.java`](file:///e:/Dev/Study'/Mobie/GrandMaAssist/GrandMaAssist/app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/AccountMethodsFlow.java):

```java
// Chuẩn bị yêu cầu đăng nhập Google One-Tap qua Android Credential Manager
GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(context.getString(R.string.default_web_client_id))
        .setAutoSelectEnabled(false)
        .build();

GetCredentialRequest request = new GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build();

CredentialManager credentialManager = CredentialManager.create(context);

credentialManager.getCredentialAsync(context, request, cancellationSignal, executor,
        new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
            @Override
            public void onResult(GetCredentialResponse response) {
                Credential credential = response.getCredential();
                if (credential instanceof CustomCredential &&
                    credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                    try {
                        GoogleIdTokenCredential googleCredential = 
                                GoogleIdTokenCredential.createFrom(credential.getData());
                        String idToken = googleCredential.getIdToken();
                        
                        // Chuyển đổi thành AuthCredential của Firebase
                        AuthCredential authCred = GoogleAuthProvider.getCredential(idToken, null);
                        signInWithFirebase(authCred);
                    } catch (GoogleIdTokenParsingException e) {
                        notifyError("Không thể phân tích dữ liệu Google ID Token.");
                    }
                }
            }

            @Override
            public void onError(GetCredentialException e) {
                notifyError("Đăng nhập Google bị gián đoạn: " + e.getMessage());
            }
        });
```

### 3. Dòng di chuyển dữ liệu (Data Flow)

```text
[Chạm nút: "Đăng nhập bằng Google"]
       │
       ▼
[CredentialManager.getCredentialAsync()]
       │
       ▼
[Android System UI BottomSheet hiển thị danh sách tài khoản Google]
       │ (Người dùng chạm chọn 1 tài khoản)
       ▼
[Google Play Services sinh chữ ký mật mã GoogleIdToken]
       │
       ▼
[GoogleIdTokenCredential.createFrom(credential.getData())] ──> Lấy chuỗi idToken
       │
       ▼
[GoogleAuthProvider.getCredential(idToken, null)]
       │
       ▼
[Firebase Auth kết nối và cấp phiên làm việc với UID tương ứng]
```

---

## API 3: CLOUD FIRESTORE API (DATABASE NOSQL, TRANSACTIONS & SERVER AGGREGATIONS)

### 1. Mục đích sử dụng trong đồ án
- Cơ sở dữ liệu đám mây thời gian thực lưu trữ các collection: `users`, `jobs`, `workSessions`, `wallets`, `retirementRequests`.
- Sử dụng **Firestore Online Transactions (`db.runTransaction`)** để bảo vệ tính toàn vẹn dữ liệu (ACID) tuyệt đối trong các thao tác ký quỹ tiền cọc, điểm danh 2 bên và quyết toán tiền công trích 10% phí sàn.
- Khai thác **Server Count Aggregation API (`AggregateSource.SERVER`)** giúp Quản trị viên đếm hàng nghìn tin tuyển dụng và hồ sơ mà không tốn chi phí đọc văn bản (Zero read costs).

### 2. Đoạn mã nguồn thực tế sử dụng API
Trích xuất từ [`WorkSessionRepository.java`](file:///e:/Dev/Study'/Mobie/GrandMaAssist/GrandMaAssist/app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/WorkSessionRepository.java) và [`ProfileRepository.java`](file:///e:/Dev/Study'/Mobie/GrandMaAssist/GrandMaAssist/app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/ProfileRepository.java):

```java
// WorkSessionRepository.java - Quyết toán ca làm & trích 10% phí sàn bằng Firestore Transaction
public Task<WorkSession> confirmCompletionAndSettle(String uid, String sessionId) {
    return bounded(db.runTransaction(tx -> {
        DocumentReference sessionRef = db.collection("workSessions").document(sessionId);
        DocumentSnapshot session = tx.get(sessionRef);
        if (!session.exists()) throw new Failure("NOT_FOUND");

        String employerId = session.getString("employerId");
        if (!uid.equals(employerId)) throw new Failure("FORBIDDEN");

        // Bóc tách thỏa thuận tài chính
        Object agObj = session.get("agreement");
        long laborAmount = 0L, serviceFee = 0L;
        if (agObj instanceof Map) {
            laborAmount = ((Number) ((Map<?, ?>) agObj).get("laborAmount")).longValue();
            serviceFee = ((Number) ((Map<?, ?>) agObj).get("serviceFee")).longValue();
        }

        // Cập nhật trạng thái quyết toán
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", WorkSession.STATUS_COMPLETED);
        updates.put("paymentStatus", WorkSession.PAYMENT_SETTLED);
        updates.put("heldAmount", 0L);
        updates.put("settledLaborAmount", laborAmount);
        updates.put("settledServiceFee", serviceFee);
        updates.put("settledAt", FieldValue.serverTimestamp());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        tx.update(sessionRef, updates);
        return null;
    }));
}

// ProfileRepository.java - Đếm tổng hợp trực tiếp trên Server (Server-side Aggregation)
Task<AggregateQuerySnapshot> tSeekerPending = db.collection("retirementRequests")
        .whereEqualTo("status", "SUBMITTED")
        .limit(50).count().get(AggregateSource.SERVER);

Task<AggregateQuerySnapshot> tJobsPending = db.collection("jobs")
        .whereEqualTo("status", "PENDING_REVIEW")
        .limit(50).count().get(AggregateSource.SERVER);
```

### 3. Dòng di chuyển dữ liệu (Data Flow)

```text
[Client khởi tạo Transaction: db.runTransaction()]
       │
       ├── (1) Đọc snapshot hiện tại trên server (Optimistic concurrency lock)
       ├── (2) Kiểm tra trạng thái nghiệp vụ: status == "COMPLETION_PENDING"?
       ├── (3) Tính toán số tiền công & 10% phí sàn
       ├── (4) Ghi đồng thời các văn bản (workSessions, wallets, transactions)
       │
       ▼
[Firebase Cloud Engine Commit]
       │ ──> Kiểm tra Firebase Security Rules (Phải đúng request.auth.uid)
       ▼
[Realtime Snapshot Listeners trên điện thoại 2 phía tự động kích hoạt]
       │
       ├── Người cao tuổi nhận tiền công vào ví (Labor Amount)
       └── Quản trị viên cập nhật thêm 10% doanh thu phí sàn (Service Fee)
```

---

## API 4: GOOGLE ML KIT BARCODE SCANNING API (AI THỊ GIÁC MÁY TÍNH QUÉT QR CCCD ON-DEVICE)

### 1. Mục đích sử dụng trong đồ án
- Tự động nhận diện và quét mã QR trên ảnh Căn cước công dân (CCCD) gắn chip của người cao tuổi.
- **Xử lý hoàn toàn On-Device (trên thiết bị):** Tốc độ quét dưới 1 giây, bảo vệ dữ liệu nhân thân nhạy cảm, không gửi ảnh lên API bên ngoài, hoạt động mượt mà ngay cả khi không có mạng Internet.

### 2. Đoạn mã nguồn thực tế sử dụng API
Trích xuất từ [`CardQrReader.java`](file:///e:/Dev/Study'/Mobie/GrandMaAssist/GrandMaAssist/app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/data/repository/CardQrReader.java):

```java
public final class CardQrReader {

    public interface Callback {
        void onResult(Result result);
    }

    public static void readQrFromBitmap(Bitmap bitmap, Callback callback) {
        if (bitmap == null) {
            callback.onResult(new Result(false, null, null, null, "Không có ảnh để xử lý"));
            return;
        }

        // Khởi tạo InputImage từ Bitmap bộ nhớ đệm
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // Giới hạn định dạng chỉ quét mã QR Code để tối ưu hóa hiệu năng
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();

        BarcodeScanner scanner = BarcodeScanning.getClient(options);

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes == null || barcodes.isEmpty()) {
                        callback.onResult(new Result(false, null, null, null, "Không tìm thấy mã QR"));
                        return;
                    }
                    for (Barcode barcode : barcodes) {
                        String rawValue = barcode.getRawValue();
                        if (rawValue != null && !rawValue.trim().isEmpty()) {
                            // Bóc tách cấu trúc chuỗi CCCD: Số|CMND|HọTên|NgàySinh|GiớiTính|ĐịaChỉ|NgàyCấp
                            Result parsed = parseCccdQr(rawValue);
                            if (parsed.success) {
                                callback.onResult(parsed);
                                return;
                            }
                        }
                    }
                    callback.onResult(new Result(false, null, null, null, "Mã QR không đúng định dạng CCCD"));
                })
                .addOnFailureListener(e -> callback.onResult(new Result(false, null, null, null, e.getMessage())));
    }
}
```

### 3. Dòng di chuyển dữ liệu (Data Flow)

```text
[Camera chụp / Thư viện ảnh] ──> Trả về URI tập tin ảnh
       │
       ▼
[ImageDecoder / BitmapFactory] ──> Chuyển đổi thành Bitmap an toàn trong RAM
       │
       ▼
[InputImage.fromBitmap(bitmap, 0)]
       │
       ▼
[ML Kit On-Device Vision Engine] ──(Trích xuất ma trận điểm ảnh 2 chiều)──┐
                                                                          │
                                                                          ▼
[Barcode.getRawValue()] <──(Chuỗi text mã hóa chuẩn Bộ Công an)────────────┘
       │
       ▼
[Hàm parseCccdQr: Tách chuỗi theo ký tự '|']
       │
       ├── Trích xuất: Số CCCD (12 số)
       ├── Trích xuất: Họ và tên
       └── Trích xuất: Ngày tháng năm sinh (ddMMyyyy)
       │
       ▼
[Tự động điền dữ liệu vào Form xác minh của Người cao tuổi trên UI]
```

---

## API 5: ANDROID TEXT-TO-SPEECH (TTS) API (GIỌNG ĐỌC NHÂN TẠO TRỢ NĂNG TIẾNG VIỆT)

### 1. Mục đích sử dụng trong đồ án
- Hỗ trợ người cao tuổi mắt kém hoặc không thạo chữ có thể nghe thông tin công việc, địa chỉ, giờ giấc ca làm bằng giọng đọc tiếng Việt chuẩn (`Locale("vi", "VN")`).
- Tích hợp bộ giải thuật thông minh nhận diện và chuyển đổi số tiền tệ `250.000đ` thành âm thanh tự nhiên: *"hai trăm năm mươi nghìn đồng"*.
- Cho phép tùy chỉnh tốc độ đọc phù hợp độ tuổi (0.75x chậm rãi, 0.85x khuyến nghị, 1.0x bình thường).

### 2. Đoạn mã nguồn thực tế sử dụng API
Trích xuất từ [`TtsManager.java`](file:///e:/Dev/Study'/Mobie/GrandMaAssist/GrandMaAssist/app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/accessibility/TtsManager.java):

```java
// Khởi tạo TextToSpeech với tiếng Việt và UtteranceProgressListener
tts = new TextToSpeech(appContext, status -> {
    if (status == TextToSpeech.SUCCESS) {
        Locale viLocale = new Locale("vi", "VN");
        int langResult = tts.setLanguage(viLocale);
        if (langResult == TextToSpeech.LANG_MISSING_DATA ||
            langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            isLanguageAvailable = false;
        } else {
            isLanguageAvailable = true;
            tts.setSpeechRate(speechRate); // Thiết lập tốc độ 0.75f - 1.0f
        }
    }
});

// Chuyển đổi tiền tệ thông minh trước khi phát âm thanh
public void speak(String text, String utteranceId) {
    if (tts == null || !isLanguageAvailable || text == null) return;
    
    // Tự động dịch số tiền tệ thành chữ tiếng Việt
    String readableText = convertCurrencyToWords(text);
    
    Bundle params = new Bundle();
    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
    
    tts.speak(readableText, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
}
```

### 3. Dòng di chuyển dữ liệu (Data Flow)

```text
[Người cao tuổi chạm vào biểu tượng Chiếc loa vàng]
       │
       ▼
[Chuỗi văn bản gốc: "Lương: 250.000 đ/buổi, làm tại Quận 1"]
       │
       ▼
[Regex Currency Parser: Chuyển đổi số tiền tệ]
       │ ──> "Lương: hai trăm năm mươi nghìn đồng một buổi, làm tại Quận 1"
       ▼
[TextToSpeech.setSpeechRate(0.85f)] ──> Tinh chỉnh tốc độ đọc phù hợp
       │
       ▼
[Android TTS Audio Synthesis Engine]
       │
       ▼
[Loa ngoài điện thoại phát âm thanh tiếng Việt trong trẻo, dễ nghe]
       │
       ▼
[LifecycleEventObserver: Ngắt dừng (tts.stop()) tức thì khi người dùng thoát/xoay màn hình]
```

---

## API 6: GOOGLE MAPS INTENT NAVIGATION API (BẢN ĐỒ CHỈ ĐƯỜNG 1-CHẠM ĐẾN NƠI LÀM VIỆC)

### 1. Mục đích sử dụng trong đồ án
- Cho phép người cao tuổi chỉ cần bấm 1 chạm duy nhất vào ô địa chỉ là hệ thống lập tức mở Google Maps và kích hoạt chế độ chỉ đường từng bước (Turn-by-turn Navigation) từ vị trí GPS hiện tại đến nơi làm việc.
- Loại bỏ hoàn toàn sự phiền toái khi phải nhớ địa chỉ, sao chép dán hoặc gõ bàn phím tìm kiếm khó khăn trên bản đồ.

### 2. Đoạn mã nguồn thực tế sử dụng API
Trích xuất từ [`MapNavigationHelper.java`](file:///e:/Dev/Study'/Mobie/GrandMaAssist/GrandMaAssist/app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/util/MapNavigationHelper.java):

```java
package vn.edu.ueh.nhom4.grandmaassist.app.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public final class MapNavigationHelper {

    public static void openDirections(Context context, String address, String area) {
        if (context == null) return;
        StringBuilder sb = new StringBuilder();
        if (address != null && !address.trim().isEmpty()) {
            sb.append(address.trim());
        }
        if (area != null && !area.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(area.trim());
        }
        if (sb.length() == 0) {
            Toast.makeText(context, "Chưa có thông tin địa chỉ để xem đường đi", Toast.LENGTH_SHORT).show();
            return;
        }

        String destination = sb.toString();
        // Scheme chuyên dụng kích hoạt Turn-by-turn Navigation của Google Maps
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(destination));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(mapIntent);
        } else {
            // Fallback mở trình duyệt web nếu máy không cài sẵn app Google Maps
            Uri webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(destination));
            Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
            try {
                context.startActivity(webIntent);
            } catch (Exception e) {
                Toast.makeText(context, "Không thể mở ứng dụng bản đồ chỉ đường", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
```

### 3. Dòng di chuyển dữ liệu (Data Flow)

```text
[Người cao tuổi chạm nút: "Xem đường đi" tại ô Địa chỉ công việc]
       │
       ▼
[MapNavigationHelper.openDirections(context, address, area)]
       │
       ├── (1) Chuẩn hóa ghép chuỗi: "Số 123 Nguyễn Thị Minh Khai, Quận 3"
       ├── (2) Mã hóa URL Encode: "S%E1%BB%91%20123..."
       ├── (3) Tạo Uri: google.navigation:q={encodedDestination}
       │
       ▼
[PackageManager.resolveActivity()]
       │
       ├── Có ứng dụng Google Maps ──> Khởi chạy trực tiếp App Google Maps
       └── Không có ứng dụng Maps ──> Fallback mở Google Maps Web trên Chrome
       │
       ▼
[Google Maps tự động định vị GPS của người già và vẽ lộ trình dẫn đường tối ưu]
```

---

## API 7: ANDROID ALARMMANAGER & NOTIFICATION CHANNEL API (NHẮC VIỆC & BÁO THỨC CA LÀM)

### 1. Mục đích sử dụng trong đồ án
- Tự động lập lịch báo thức trước giờ làm 30 phút ngay khi Ca làm việc được ký quỹ (`READY`).
- Sử dụng phương thức `setExactAndAllowWhileIdle()` của Android AlarmManager để đảm bảo thiết bị reo chuông đúng hẹn ngay cả khi điện thoại đang ở chế độ ngủ sâu tiết kiệm pin (Doze Mode).
- Đăng ký `NotificationChannel` chuẩn Android 8.0+ với mức ưu tiên `IMPORTANCE_HIGH` giúp thông báo nổi rõ trên màn hình khóa.

### 2. Đoạn mã nguồn thực tế sử dụng API
Trích xuất từ [`ShiftAlarmScheduler.java`](file:///e:/Dev/Study'/Mobie/GrandMaAssist/GrandMaAssist/app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/notification/ShiftAlarmScheduler.java) và [`ShiftReminderReceiver.java`](file:///e:/Dev/Study'/Mobie/GrandMaAssist/GrandMaAssist/app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/notification/ShiftReminderReceiver.java):

```java
// ShiftAlarmScheduler.java - Đặt lịch chính xác bằng AlarmManager
AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
Intent intent = new Intent(context, ShiftReminderReceiver.class);
intent.putExtra("session_id", session.id);
intent.putExtra("shift_title", session.jobTitle);

PendingIntent pi = PendingIntent.getBroadcast(
        context,
        session.id.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
);

long triggerAtMillis = shiftStartMillis - (30 * 60 * 1000L); // Trước 30 phút
am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);

// ShiftReminderReceiver.java - Bắn thông báo Notification khi chuông reo
NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_bell)
        .setContentTitle("Nhắc nhở ca làm việc sắp bắt đầu")
        .setContentText("Ca làm '" + shiftTitle + "' sẽ bắt đầu sau 30 phút nữa.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingContentIntent);

NotificationManagerCompat.from(context).notify(notificationId, builder.build());
```

### 3. Dòng di chuyển dữ liệu (Data Flow)

```text
[Ca làm việc chuyển sang trạng thái READY (Đã ký quỹ)]
       │
       ▼
[ShiftAlarmScheduler.scheduleShiftReminder()]
       │ ──> Tính toán thời điểm: triggerAtMillis = GiờBắtĐầu - 30 phút
       ▼
[Hệ điều hành Android đăng ký Báo thức phần cứng: AlarmManager.RTC_WAKEUP]
       │
       ▼ (Thời gian trôi đến trước ca làm 30 phút)
[Hệ điều hành đánh thức thiết bị và kích hoạt: ShiftReminderReceiver.onReceive()]
       │
       ▼
[Tạo NotificationChannel với mức ưu tiên cao: IMPORTANCE_HIGH]
       │
       ▼
[Hiển thị Heads-up Notification kèm âm thanh báo thức rung chuông trên thanh trạng thái]
```

---

## API 8: ANDROID ACTIVITY RESULT CONTRACTS & IMAGE PROCESSING API (XỬ LÝ & NÉN ẢNH NƠI LÀM VIỆC)

### 1. Mục đích sử dụng trong đồ án
- Tận dụng `ActivityResultContracts.GetContent()` để người dùng chọn ảnh nơi làm việc hoặc ảnh giấy tờ từ thư viện máy an toàn mà không cần xin quyền nguy hiểm `READ_EXTERNAL_STORAGE`.
- Tự động nén kích thước, scale ảnh và mã hóa định dạng chuỗi Base64 an toàn lưu trữ trực tiếp vào văn bản Firestore của tin tuyển dụng, giúp người tìm việc dễ dàng quan sát môi trường làm việc thực tế.

### 2. Đoạn mã nguồn thực tế sử dụng API
Trích xuất từ [`JobDraftActivity.java`](file:///e:/Dev/Study'/Mobie/GrandMaAssist/GrandMaAssist/app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/ui/jobs/JobDraftActivity.java) và [`ImageHelper.java`](file:///e:/Dev/Study'/Mobie/GrandMaAssist/GrandMaAssist/app/src/main/java/vn/edu/ueh/nhom4/grandmaassist/app/util/ImageHelper.java):

```java
// Đăng ký ActivityResultLauncher chọn ảnh môi trường làm việc
private final ActivityResultLauncher<String> workplacePhotoLauncher = registerForActivityResult(
        new ActivityResultContracts.GetContent(),
        uri -> {
            if (uri != null) {
                // Xử lý nén và mã hóa ảnh thành chuỗi Base64
                String base64 = ImageHelper.compressUriToBase64(this, uri, 800, 600, 80);
                if (base64 != null) {
                    vm.setWorkplaceImageBase64(base64);
                    // Hiển thị xem trước lên giao diện
                    ivWorkplacePreview.setImageBitmap(ImageHelper.decodeBase64(base64));
                    ivWorkplacePreview.setVisibility(View.VISIBLE);
                }
            }
        });

// ImageHelper.java - Nén Bitmap và mã hóa Base64
public static String compressUriToBase64(Context context, Uri uri, int maxW, int maxH, int quality) {
    try (InputStream is = context.getContentResolver().openInputStream(uri)) {
        Bitmap original = BitmapFactory.decodeStream(is);
        Bitmap scaled = scaleBitmap(original, maxW, maxH);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    } catch (Exception e) {
        return null;
    }
}
```

### 3. Dòng di chuyển dữ liệu (Data Flow)

```text
[Người thuê bấm nút: "Thêm ảnh nơi làm việc"]
       │
       ▼
[ActivityResultContracts.GetContent().launch("image/*")]
       │
       ▼
[Trình chọn ảnh hệ thống Android cho phép chọn 1 bức ảnh an toàn]
       │ ──> Trả về URI nội bộ
       ▼
[ContentResolver mở InputStream đọc dữ liệu ảnh]
       │
       ▼
[ImageHelper tự động scale ảnh xuống tối đa 800x600 & nén JPEG chất lượng 80%]
       │
       ▼
[Chuyển đổi mảng byte[] thành chuỗi Base64 String]
       │
       ├── (1) Gán Bitmap vào ImageView để người thuê xem trước tức thời
       └── (2) Đính kèm chuỗi Base64 vào JobDraft gửi lên Firestore collection "jobs"
       │
       ▼
[Người cao tuổi mở xem chi tiết việc làm: Thấy ảnh nơi làm việc thực tế sinh động]
```

---

## BẢNG TỔNG HỢP SO SÁNH 8 APIS TRONG DỰ ÁN GRANDMAASSIST

| STT | Tên API / Dịch Vụ | Package / Lớp Kỹ Thuật Chính | Vị Trí File Mã Nguồn | Nghiệp Vụ Thực Tế Giải Quyết |
|:---:|---|---|---|---|
| **1** | **Firebase Auth API** | `FirebaseAuth`<br>`PhoneAuthProvider` | `AuthRepository.java`<br>`PhoneSignInFlow.java` | Đăng nhập tài khoản & nhận mã OTP qua SMS mạng di động thật. |
| **2** | **Credential Manager API** | `androidx.credentials`<br>`GoogleIdTokenCredential` | `AccountMethodsFlow.java` | Đăng nhập 1-chạm Google chuẩn Android 14+ không dùng thư viện cũ. |
| **3** | **Cloud Firestore API** | `FirebaseFirestore`<br>`AggregateSource.SERVER` | `WorkSessionRepository.java`<br>`ProfileRepository.java` | Giao dịch nguyên tử ký quỹ/quyết toán trích phí 10% & đếm số liệu Server. |
| **4** | **Google ML Kit API** | `BarcodeScanning`<br>`InputImage` | `CardQrReader.java` | AI quét mã QR thẻ CCCD gắn chip On-Device bảo mật, siêu tốc. |
| **5** | **Android TTS API** | `android.speech.tts.TextToSpeech` | `TtsManager.java`<br>`TtsControllerHelper.java` | Giọng đọc nhân tạo tiếng Việt trợ năng & đọc số tiền tự nhiên. |
| **6** | **Google Maps API** | `google.navigation:q=`<br>`Intent.ACTION_VIEW` | `MapNavigationHelper.java` | Bản đồ chỉ đường 1-chạm từ vị trí GPS hiện tại đến địa chỉ làm việc. |
| **7** | **AlarmManager API** | `AlarmManager.setExactAndAllowWhileIdle`<br>`NotificationCompat` | `ShiftAlarmScheduler.java`<br>`ShiftReminderReceiver.java` | Đặt lịch báo thức chính xác nhắc nhở ca làm trước 30 phút. |
| **8** | **Image Processing API** | `ActivityResultContracts.GetContent`<br>`Base64` | `JobDraftActivity.java`<br>`ImageHelper.java` | Chọn ảnh thư viện an toàn & nén ảnh nơi làm việc thực tế. |

*(Toàn bộ 8 APIs trên đều được lập trình bằng ngôn ngữ Java chuẩn mực, tương thích hoàn toàn từ Android 8.0 Oreo đến Android 14+).*
