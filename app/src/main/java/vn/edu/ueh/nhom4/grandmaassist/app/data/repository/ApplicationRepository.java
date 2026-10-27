package vn.edu.ueh.nhom4.grandmaassist.app.data.repository;

import com.google.android.gms.tasks.*;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import java.util.*;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.*;

public final class ApplicationRepository {
    public static final class Failure extends RuntimeException {
        public final String code;
        public Failure(String code) { super(code); this.code = code; }
    }

    public static final class Page {
        public final List<Application> items;
        public final DocumentSnapshot cursor;
        public final boolean more;
        Page(QuerySnapshot s) {
            items = new ArrayList<>();
            for (DocumentSnapshot d : s) {
                Application a = Application.fromSnapshot(d);
                if (a != null) items.add(a);
            }
            items.sort((a, b) -> {
                if (a.createdAt == null && b.createdAt == null) return 0;
                if (a.createdAt == null) return 1;
                if (b.createdAt == null) return -1;
                return b.createdAt.compareTo(a.createdAt);
            });
            cursor = s.isEmpty() ? null : s.getDocuments().get(s.size() - 1);
            more = s.size() == 20;
        }
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static <T> Task<T> bounded(Task<T> task) {
        return Tasks.withTimeout(task, 45, java.util.concurrent.TimeUnit.SECONDS);
    }

    public boolean same(String uid) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u != null && u.getUid().equals(uid);
    }

    private Task<String> proof(String uid) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null || !same(uid)) return Tasks.forException(new Failure("SESSION"));
        return u.getIdToken(true).continueWith(t -> {
            if (!t.isSuccessful()) throw t.getException();
            if (!same(uid)) throw new Failure("SESSION");
            Object v = t.getResult().getClaims().get("phone_number");
            return v instanceof String ? (String) v : "";
        });
    }

    public Task<String> apply(String uid, String jobId, String message, boolean sharePhoneConsent) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));
        final String note = message == null ? "" : message.trim();
        if (note.length() > 500) return Tasks.forException(new Failure("MESSAGE_TOO_LONG"));

        return bounded(proof(uid).continueWithTask(p -> {
            if (!p.isSuccessful()) throw p.getException();
            final String authPhone = p.getResult();

            return db.runTransaction(tx -> {
                if (!same(uid)) throw new Failure("SESSION");

                DocumentReference userRef = db.collection("users").document(uid);
                DocumentSnapshot user;
                try {
                    user = tx.get(userRef);
                } catch (FirebaseFirestoreException fe) {
                    if (fe.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        throw new Failure("RULES_READ_USER_DENIED");
                    }
                    throw fe;
                }
                if (!user.exists() || !"SEEKER".equals(user.getString("role")) || !"ACTIVE".equals(user.getString("accountStatus"))) {
                    throw new Failure("FORBIDDEN");
                }
                String phone = user.getString("phoneE164");
                if (phone == null || phone.isEmpty() || !phone.equals(authPhone)
                        || !"FIREBASE_AUTH".equals(user.getString("phoneVerificationMode"))
                        || !(user.get("phoneVerifiedAt") instanceof Timestamp)) {
                    throw new Failure("PHONE_REQUIRED");
                }
                if (!"APPROVED".equals(user.getString("retirementStatus"))) {
                    throw new Failure("RETIREMENT_REQUIRED");
                }

                DocumentReference seekerRef = db.collection("jobSeekerProfiles").document(uid);
                DocumentSnapshot seeker;
                try {
                    seeker = tx.get(seekerRef);
                } catch (FirebaseFirestoreException fe) {
                    if (fe.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        throw new Failure("RULES_READ_PROFILE_DENIED");
                    }
                    throw fe;
                }
                if (!seeker.exists()) {
                    throw new Failure("PROFILE_INCOMPLETE");
                }

                DocumentReference jobRef = db.collection("jobs").document(jobId);
                DocumentSnapshot job;
                try {
                    job = tx.get(jobRef);
                } catch (FirebaseFirestoreException fe) {
                    if (fe.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        throw new Failure("RULES_READ_JOB_DENIED");
                    }
                    throw fe;
                }
                if (!job.exists()) {
                    throw new Failure("JOB_NOT_FOUND");
                }
                if (!"OPEN".equals(job.getString("status"))) {
                    throw new Failure("JOB_NOT_OPEN");
                }
                Timestamp expiresAt = job.getTimestamp("expiresAt");
                if (expiresAt != null && expiresAt.toDate().getTime() <= System.currentTimeMillis()) {
                    throw new Failure("JOB_EXPIRED");
                }
                String employerId = job.getString("employerId");
                if (uid.equals(employerId)) {
                    throw new Failure("OWN_JOB");
                }

                String applicationId = jobId + "_" + uid;
                DocumentReference appRef = db.collection("applications").document(applicationId);
                DocumentSnapshot existingApp;
                try {
                    existingApp = tx.get(appRef);
                } catch (FirebaseFirestoreException fe) {
                    if (fe.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        throw new Failure("RULES_CHECK_EXISTING_APP");
                    }
                    throw fe;
                }
                if (existingApp.exists()) {
                    throw new Failure("DUPLICATE_APPLICATION");
                }

                List<String> seekerSkills = new ArrayList<>();
                Object sObj = seeker.get("skills");
                if (sObj instanceof List) {
                    for (Object item : (List<?>) sObj) if (item instanceof String) seekerSkills.add((String) item);
                }
                Long profVer = seeker.getLong("identityProfileVersion");
                ApplicantSnapshot applicantSnapshot = new ApplicantSnapshot(
                        user.getString("displayName"),
                        phone,
                        seekerSkills,
                        seeker.getString("experienceSummary"),
                        seeker.getString("areaCode"),
                        profVer != null ? profVer : 1L,
                        null
                );

                List<String> jobDemands = new ArrayList<>();
                Object dObj = job.get("physicalDemands");
                if (dObj instanceof List) {
                    for (Object item : (List<?>) dObj) if (item instanceof String) jobDemands.add((String) item);
                }
                Long pay = job.getLong("payAmount");
                Long jobVer = job.getLong("version");
                JobSnapshot jobSnapshot = new JobSnapshot(
                        job.getString("title"),
                        job.getString("categoryId"),
                        job.getString("categoryName"),
                        job.getString("areaCode"),
                        job.getString("areaName"),
                        job.getString("workAddress"),
                        job.getString("scheduleText"),
                        jobDemands,
                        pay != null ? pay : 0L,
                        job.getString("payUnit"),
                        jobVer != null ? jobVer : 1L
                );

                Map<String, Object> applicantMap = applicantSnapshot.toMap();
                applicantMap.put("consentAt", FieldValue.serverTimestamp());

                Map<String, Object> data = new HashMap<>();
                data.put("id", applicationId);
                data.put("jobId", jobId);
                data.put("seekerId", uid);
                data.put("employerId", employerId);
                data.put("message", note);
                data.put("status", Application.STATUS_PENDING);
                data.put("sharePhoneConsent", sharePhoneConsent);
                data.put("applicantSnapshot", applicantMap);
                data.put("jobSnapshot", jobSnapshot.toMap());
                data.put("createdAt", FieldValue.serverTimestamp());
                data.put("updatedAt", FieldValue.serverTimestamp());

                tx.set(appRef, data);
                return applicationId;
            });
        }));
    }

    public Task<Void> withdraw(String uid, String applicationId) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));

        return bounded(db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");
            DocumentReference appRef = db.collection("applications").document(applicationId);
            DocumentSnapshot doc = tx.get(appRef);
            if (!doc.exists()) throw new Failure("NOT_FOUND");
            if (!uid.equals(doc.getString("seekerId"))) throw new Failure("FORBIDDEN");
            if (!Application.STATUS_PENDING.equals(doc.getString("status"))) throw new Failure("CANNOT_WITHDRAW");

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", Application.STATUS_WITHDRAWN);
            updates.put("updatedAt", FieldValue.serverTimestamp());
            tx.update(appRef, updates);
            return null;
        }));
    }

    public Task<List<Application>> getApplicationsForJob(String uid, String jobId) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));
        Query q = db.collection("applications")
                .whereEqualTo("employerId", uid)
                .whereEqualTo("jobId", jobId)
                .limit(50);

        return bounded(q.get(Source.SERVER).continueWith(t -> {
            if (!t.isSuccessful()) throw t.getException();
            if (!same(uid)) throw new Failure("SESSION");
            QuerySnapshot s = t.getResult();
            List<Application> list = new ArrayList<>();
            if (s != null) {
                for (DocumentSnapshot doc : s) {
                    Application a = Application.fromSnapshot(doc);
                    if (a != null) list.add(a);
                }
            }
            list.sort((a, b) -> {
                if (a.createdAt == null && b.createdAt == null) return 0;
                if (a.createdAt == null) return 1;
                if (b.createdAt == null) return -1;
                return b.createdAt.compareTo(a.createdAt);
            });
            return list;
        }));
    }

    public Task<Void> accept(String uid, String applicationId) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));
        return bounded(db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");
            DocumentReference appRef = db.collection("applications").document(applicationId);
            DocumentSnapshot app = tx.get(appRef);
            if (!app.exists()) throw new Failure("NOT_FOUND");
            if (!uid.equals(app.getString("employerId"))) throw new Failure("FORBIDDEN");
            if (!Application.STATUS_PENDING.equals(app.getString("status"))) {
                throw new Failure("APPLICATION_NOT_PENDING");
            }

            String jobId = app.getString("jobId");
            if (jobId != null) {
                DocumentReference jobRef = db.collection("jobs").document(jobId);
                DocumentSnapshot job = tx.get(jobRef);
                if (!job.exists()) throw new Failure("JOB_NOT_FOUND");
                String jobStatus = job.getString("status");
                if (!"OPEN".equals(jobStatus) && !"CLOSED".equals(jobStatus)) {
                    throw new Failure("JOB_INACTIVE");
                }
            }

            DocumentReference userRef = db.collection("users").document(uid);
            DocumentSnapshot user = tx.get(userRef);
            String displayName = user.exists() ? user.getString("displayName") : "";
            String phoneE164 = user.exists() ? user.getString("phoneE164") : "";
            if (phoneE164 == null) phoneE164 = "";
            if (displayName == null || displayName.trim().isEmpty()) displayName = "Người tuyển dụng";

            Map<String, Object> contactMap = new HashMap<>();
            contactMap.put("displayName", displayName);
            contactMap.put("phoneE164", phoneE164);
            contactMap.put("acceptedAt", FieldValue.serverTimestamp());

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", Application.STATUS_ACCEPTED);
            updates.put("employerContactSnapshot", contactMap);
            updates.put("updatedAt", FieldValue.serverTimestamp());
            tx.update(appRef, updates);
            return null;
        }));
    }

    public Task<Void> reject(String uid, String applicationId) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));
        return bounded(db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");
            DocumentReference appRef = db.collection("applications").document(applicationId);
            DocumentSnapshot app = tx.get(appRef);
            if (!app.exists()) throw new Failure("NOT_FOUND");
            if (!uid.equals(app.getString("employerId"))) throw new Failure("FORBIDDEN");
            if (!Application.STATUS_PENDING.equals(app.getString("status"))) {
                throw new Failure("APPLICATION_NOT_PENDING");
            }

            String jobId = app.getString("jobId");
            if (jobId != null) {
                DocumentReference jobRef = db.collection("jobs").document(jobId);
                DocumentSnapshot job = tx.get(jobRef);
                if (!job.exists()) throw new Failure("JOB_NOT_FOUND");
                String jobStatus = job.getString("status");
                if (!"OPEN".equals(jobStatus) && !"CLOSED".equals(jobStatus)) {
                    throw new Failure("JOB_INACTIVE");
                }
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", Application.STATUS_REJECTED);
            updates.put("updatedAt", FieldValue.serverTimestamp());
            tx.update(appRef, updates);
            return null;
        }));
    }

    public Task<Application> getApplicationForJob(String uid, String jobId) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));
        String applicationId = jobId + "_" + uid;
        return bounded(db.collection("applications").document(applicationId).get(Source.SERVER).continueWith(t -> {
            if (!t.isSuccessful()) throw t.getException();
            if (!same(uid)) throw new Failure("SESSION");
            DocumentSnapshot doc = t.getResult();
            return doc != null && doc.exists() ? Application.fromSnapshot(doc) : null;
        }));
    }

    public Task<Page> getMyApplications(String uid, DocumentSnapshot cursor) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));
        Query q = db.collection("applications")
                .whereEqualTo("seekerId", uid)
                .limit(20);
        if (cursor != null) q = q.startAfter(cursor);

        return bounded(q.get(Source.SERVER).continueWith(t -> {
            if (!t.isSuccessful()) throw t.getException();
            if (!same(uid)) throw new Failure("SESSION");
            return new Page(t.getResult());
        }));
    }

    public static String error(Exception ex) {
        Throwable c = ex;
        for (int i = 0; c != null && i < 10; i++, c = c.getCause()) {
            if (c instanceof Failure) {
                switch (((Failure) c).code) {
                    case "RULES_CHECK_EXISTING_APP":
                        return "Chưa cập nhật Rules cho đơn ứng tuyển trên Firebase Console (RULES_CHECK_EXISTING_APP). Hãy Publish toàn bộ file firestore.rules mới nhất.";
                    case "RULES_READ_USER_DENIED":
                        return "Rules từ chối đọc thông tin người dùng (RULES_READ_USER_DENIED).";
                    case "RULES_READ_PROFILE_DENIED":
                        return "Rules từ chối đọc hồ sơ người tìm việc (RULES_READ_PROFILE_DENIED).";
                    case "RULES_READ_JOB_DENIED":
                        return "Rules từ chối đọc thông tin việc làm (RULES_READ_JOB_DENIED).";
                    case "RETIREMENT_REQUIRED":
                        return "Cô/chú cần được duyệt hồ sơ nghỉ hưu trước khi nộp đơn ứng tuyển.";
                    case "PHONE_REQUIRED":
                        return "Cần xác minh số điện thoại trong mục Tài khoản trước khi ứng tuyển.";
                    case "DUPLICATE_APPLICATION":
                        return "Cô/chú đã nộp đơn ứng tuyển cho công việc này rồi.";
                    case "JOB_EXPIRED":
                        return "Công việc này đã hết hạn nhận đơn hoặc đã đóng.";
                    case "JOB_NOT_OPEN":
                        return "Công việc này hiện không còn nhận đơn ứng tuyển.";
                    case "OWN_JOB":
                        return "Không thể ứng tuyển vào công việc do chính mình đăng.";
                    case "CANNOT_WITHDRAW":
                        return "Đơn ứng tuyển không còn ở trạng thái chờ để rút.";
                    case "APPLICATION_NOT_PENDING":
                        return "Đơn ứng tuyển không còn ở trạng thái chờ duyệt (ứng viên đã rút đơn hoặc đã được phản hồi).";
                    case "JOB_INACTIVE":
                        return "Tin tuyển dụng này hiện không ở trạng thái hợp lệ để tiếp nhận ứng viên.";
                    case "NOT_FOUND":
                        return "Không tìm thấy thông tin đơn ứng tuyển.";
                    case "FORBIDDEN":
                        return "Tài khoản không đủ quyền thực hiện thao tác này.";
                    case "MESSAGE_TOO_LONG":
                        return "Lời nhắn quá dài (tối đa 500 ký tự).";
                    default:
                        return "Thao tác chưa hoàn thành. Hãy thử lại.";
                }
            }
            if (c instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException fe = (FirebaseFirestoreException) c;
                if (fe.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    return "Quy tắc bảo mật Firestore từ chối tạo đơn (PERMISSION_DENIED). Cần đảm bảo đã Publish file firestore.rules mới nhất trên Firebase Console và tài khoản có số điện thoại xác minh.";
                }
                if (fe.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    return "Cơ sở dữ liệu đang yêu cầu cấu hình Index (FAILED_PRECONDITION).";
                }
            }
            if (c instanceof java.util.concurrent.TimeoutException) {
                return "Chưa thể kết nối tới máy chủ sau 45 giây. Hãy kiểm tra kết nối mạng và thử lại.";
            }
        }
        return "Đã xảy ra lỗi kết nối. Hãy thử lại sau giây lát.";
    }
}
