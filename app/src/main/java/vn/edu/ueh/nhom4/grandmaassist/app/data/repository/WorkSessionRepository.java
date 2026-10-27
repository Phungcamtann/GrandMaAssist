package vn.edu.ueh.nhom4.grandmaassist.app.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.*;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.AgreementSnapshot;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.Application;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.WorkSession;

public final class WorkSessionRepository {
    public static final class Failure extends RuntimeException {
        public final String code;
        public Failure(String code) { super(code); this.code = code; }
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static <T> Task<T> bounded(Task<T> task) {
        return Tasks.withTimeout(task, 45, java.util.concurrent.TimeUnit.SECONDS);
    }

    public boolean same(String uid) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u != null && u.getUid().equals(uid);
    }

    public Task<String> proposeSession(String uid, String applicationId, String tasks,
                                       String privateAddress, String areaCode,
                                       Timestamp startAt, Timestamp endAt, long laborAmount) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));
        if (startAt == null || endAt == null || startAt.compareTo(endAt) >= 0) {
            return Tasks.forException(new Failure("INVALID_TIME"));
        }
        if (startAt.toDate().getTime() <= System.currentTimeMillis()) {
            return Tasks.forException(new Failure("TIME_IN_PAST"));
        }
        if (laborAmount <= 0) {
            return Tasks.forException(new Failure("INVALID_PAY"));
        }

        return bounded(db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");

            DocumentReference appRef = db.collection("applications").document(applicationId);
            DocumentSnapshot app = tx.get(appRef);
            if (!app.exists()) throw new Failure("APPLICATION_NOT_FOUND");
            if (!uid.equals(app.getString("employerId"))) throw new Failure("FORBIDDEN");
            if (!Application.STATUS_ACCEPTED.equals(app.getString("status"))) {
                throw new Failure("APPLICATION_NOT_ACCEPTED");
            }

            String seekerId = app.getString("seekerId");
            String jobId = app.getString("jobId");
            if (seekerId == null || jobId == null) throw new Failure("DATA_CORRUPT");

            String sessionId = "session_" + applicationId;
            DocumentReference sessionRef = db.collection("workSessions").document(sessionId);
            DocumentSnapshot existing = tx.get(sessionRef);
            if (existing.exists()) {
                String existingStatus = existing.getString("status");
                if (!WorkSession.STATUS_CANCELLED.equals(existingStatus)) {
                    throw new Failure("SESSION_ALREADY_EXISTS");
                }
            }

            String finalWorkAddress = privateAddress;
            String finalAreaName = "";
            String finalAreaCode = areaCode;

            Object snapObj = app.get("jobSnapshot");
            if (snapObj instanceof Map) {
                Map<?, ?> jobSnap = (Map<?, ?>) snapObj;
                Object addrObj = jobSnap.get("workAddress");
                if (addrObj instanceof String && !((String) addrObj).trim().isEmpty()) {
                    finalWorkAddress = ((String) addrObj).trim();
                }
                Object areaNameObj = jobSnap.get("areaName");
                if (areaNameObj instanceof String && !((String) areaNameObj).trim().isEmpty()) {
                    finalAreaName = ((String) areaNameObj).trim();
                }
                Object areaCodeObj = jobSnap.get("areaCode");
                if (areaCodeObj instanceof String && !((String) areaCodeObj).trim().isEmpty()) {
                    finalAreaCode = ((String) areaCodeObj).trim();
                }
            }

            if (finalWorkAddress == null || finalWorkAddress.trim().isEmpty()) {
                finalWorkAddress = "Địa chỉ theo tin tuyển dụng";
            } else if (finalWorkAddress.startsWith("Địa chỉ:")) {
                finalWorkAddress = finalWorkAddress.substring("Địa chỉ:".length()).trim();
            }

            String fullDisplayAddress = finalWorkAddress;
            if (!finalAreaName.isEmpty()) {
                fullDisplayAddress += "\nKhu vực: " + finalAreaName;
            }

            AgreementSnapshot agreement = AgreementSnapshot.create(
                    tasks, fullDisplayAddress, finalAreaCode, startAt, endAt, laborAmount
            );

            Map<String, Object> data = new HashMap<>();
            data.put("id", sessionId);
            data.put("applicationId", applicationId);
            data.put("jobId", jobId);
            data.put("seekerId", seekerId);
            data.put("employerId", uid);
            data.put("agreement", agreement.toMap());
            data.put("agreementVersion", 1L);
            data.put("employerAcceptedVersion", 1L);
            data.put("status", WorkSession.STATUS_PROPOSED);
            data.put("paymentStatus", WorkSession.PAYMENT_UNFUNDED);
            data.put("heldAmount", 0L);
            data.put("totalFunded", 0L);
            data.put("createdAt", FieldValue.serverTimestamp());
            data.put("updatedAt", FieldValue.serverTimestamp());

            tx.set(sessionRef, data);
            return sessionId;
        }));
    }

    public Task<Void> editProposal(String uid, String sessionId, String tasks,
                                   String privateAddress, String areaCode,
                                   Timestamp startAt, Timestamp endAt, long laborAmount) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));
        if (startAt == null || endAt == null || startAt.compareTo(endAt) >= 0) {
            return Tasks.forException(new Failure("INVALID_TIME"));
        }
        if (startAt.toDate().getTime() <= System.currentTimeMillis()) {
            return Tasks.forException(new Failure("TIME_IN_PAST"));
        }
        if (laborAmount <= 0) {
            return Tasks.forException(new Failure("INVALID_PAY"));
        }

        return bounded(db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");

            DocumentReference sessionRef = db.collection("workSessions").document(sessionId);
            DocumentSnapshot session = tx.get(sessionRef);
            if (!session.exists()) throw new Failure("NOT_FOUND");
            if (!uid.equals(session.getString("employerId"))) throw new Failure("FORBIDDEN");
            if (!WorkSession.STATUS_PROPOSED.equals(session.getString("status"))) {
                throw new Failure("CANNOT_EDIT");
            }

            Long curVer = session.getLong("agreementVersion");
            long newVer = (curVer != null ? curVer : 1L) + 1L;

            String effectiveAddress = privateAddress;
            String appId = session.getString("applicationId");
            if (appId != null && !appId.isEmpty()) {
                DocumentSnapshot app = tx.get(db.collection("applications").document(appId));
                if (app.exists()) {
                    Object snapObj = app.get("jobSnapshot");
                    if (snapObj instanceof Map) {
                        Map<?, ?> jobSnap = (Map<?, ?>) snapObj;
                        Object addrObj = jobSnap.get("workAddress");
                        Object areaNameObj = jobSnap.get("areaName");
                        String wAddr = addrObj instanceof String ? ((String) addrObj).trim() : "";
                        String aName = areaNameObj instanceof String ? ((String) areaNameObj).trim() : "";
                        if (wAddr.startsWith("Địa chỉ:")) {
                            wAddr = wAddr.substring("Địa chỉ:".length()).trim();
                        }
                        if (!wAddr.isEmpty()) {
                            effectiveAddress = wAddr;
                            if (!aName.isEmpty()) effectiveAddress += "\nKhu vực: " + aName;
                        }
                    }
                }
            }

            AgreementSnapshot agreement = AgreementSnapshot.create(
                    tasks, effectiveAddress != null ? effectiveAddress : privateAddress, areaCode, startAt, endAt, laborAmount
            );

            Map<String, Object> updates = new HashMap<>();
            updates.put("agreement", agreement.toMap());
            updates.put("agreementVersion", newVer);
            updates.put("employerAcceptedVersion", newVer);
            updates.put("seekerAcceptedVersion", null);
            updates.put("updatedAt", FieldValue.serverTimestamp());

            tx.update(sessionRef, updates);
            return null;
        }));
    }

    public Task<Void> seekerAccept(String uid, String sessionId, long expectedVersion) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));

        return bounded(db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");

            DocumentReference sessionRef = db.collection("workSessions").document(sessionId);
            DocumentSnapshot session = tx.get(sessionRef);
            if (!session.exists()) throw new Failure("NOT_FOUND");
            if (!uid.equals(session.getString("seekerId"))) throw new Failure("FORBIDDEN");
            if (!WorkSession.STATUS_PROPOSED.equals(session.getString("status"))) {
                throw new Failure("NOT_PROPOSED");
            }

            Long curVer = session.getLong("agreementVersion");
            if (curVer == null || curVer != expectedVersion) {
                throw new Failure("VERSION_CONFLICT");
            }

            Long empVer = session.getLong("employerAcceptedVersion");
            boolean fullAgreed = empVer != null && empVer == expectedVersion;

            Map<String, Object> updates = new HashMap<>();
            updates.put("seekerAcceptedVersion", expectedVersion);
            if (fullAgreed) {
                updates.put("status", WorkSession.STATUS_AWAITING_FUNDS);
            }
            updates.put("updatedAt", FieldValue.serverTimestamp());

            tx.update(sessionRef, updates);
            return null;
        }));
    }

    public Task<Void> seekerReject(String uid, String sessionId) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));

        return bounded(db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");

            DocumentReference sessionRef = db.collection("workSessions").document(sessionId);
            DocumentSnapshot session = tx.get(sessionRef);
            if (!session.exists()) throw new Failure("NOT_FOUND");
            if (!uid.equals(session.getString("seekerId")) && !uid.equals(session.getString("employerId"))) {
                throw new Failure("FORBIDDEN");
            }
            if (!WorkSession.STATUS_PROPOSED.equals(session.getString("status"))) {
                throw new Failure("CANNOT_CANCEL");
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", WorkSession.STATUS_CANCELLED);
            updates.put("updatedAt", FieldValue.serverTimestamp());

            tx.update(sessionRef, updates);
            return null;
        }));
    }

    public Task<Void> fundSession(String uid, String sessionId) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));

        return bounded(db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");

            DocumentReference sessionRef = db.collection("workSessions").document(sessionId);
            DocumentSnapshot session = tx.get(sessionRef);
            if (!session.exists()) throw new Failure("NOT_FOUND");
            if (!uid.equals(session.getString("employerId"))) throw new Failure("FORBIDDEN");

            String status = session.getString("status");
            String paymentStatus = session.getString("paymentStatus");

            if (!WorkSession.STATUS_AWAITING_FUNDS.equals(status)) {
                throw new Failure("NOT_AWAITING_FUNDS");
            }
            if (WorkSession.PAYMENT_HELD.equals(paymentStatus)) {
                // Idempotent: already funded
                return null;
            }

            Long agVer = session.getLong("agreementVersion");
            Long empVer = session.getLong("employerAcceptedVersion");
            Long seekVer = session.getLong("seekerAcceptedVersion");
            if (agVer == null || !agVer.equals(empVer) || !agVer.equals(seekVer)) {
                throw new Failure("VERSION_MISMATCH");
            }

            Object agObj = session.get("agreement");
            long totalDue = 0L;
            if (agObj instanceof Map) {
                Object tdObj = ((Map<?, ?>) agObj).get("totalDue");
                if (tdObj instanceof Number) {
                    totalDue = ((Number) tdObj).longValue();
                }
            }
            if (totalDue <= 0) throw new Failure("INVALID_TOTAL_DUE");

            DocumentReference wRef = db.collection("wallets").document(uid);
            DocumentSnapshot wSnap = tx.get(wRef);
            long curBalance = 0L;
            if (wSnap.exists()) {
                Long b = wSnap.getLong("balance");
                if (b != null) curBalance = b;
            }

            if (curBalance < totalDue) {
                throw new Failure("INSUFFICIENT_FUNDS");
            }

            long newBalance = curBalance - totalDue;
            Map<String, Object> wUpdates = new HashMap<>();
            wUpdates.put("balance", newBalance);
            wUpdates.put("updatedAt", FieldValue.serverTimestamp());
            tx.set(wRef, wUpdates, SetOptions.merge());

            String txId = "tx_escrow_" + sessionId;
            DocumentReference txRef = db.collection("wallets").document(uid)
                    .collection("transactions").document(txId);
            Map<String, Object> txData = new HashMap<>();
            txData.put("id", txId);
            txData.put("type", "HOLD_ESCROW");
            txData.put("amount", totalDue);
            txData.put("balanceAfter", newBalance);
            String displayId = sessionId.startsWith("session_") ? sessionId.substring("session_".length()) : sessionId;
            txData.put("description", "Tạm giữ cọc ca làm việc #" + displayId);
            txData.put("refId", sessionId);
            txData.put("createdAt", FieldValue.serverTimestamp());
            tx.set(txRef, txData);

            Map<String, Object> sUpdates = new HashMap<>();
            sUpdates.put("status", WorkSession.STATUS_READY);
            sUpdates.put("paymentStatus", WorkSession.PAYMENT_HELD);
            sUpdates.put("heldAmount", totalDue);
            sUpdates.put("totalFunded", totalDue);
            sUpdates.put("fundedAt", FieldValue.serverTimestamp());
            sUpdates.put("updatedAt", FieldValue.serverTimestamp());
            tx.update(sessionRef, sUpdates);

            String eventId = "pe_fund_" + sessionId;
            DocumentReference eventRef = sessionRef.collection("paymentEvents").document(eventId);
            Map<String, Object> peData = new HashMap<>();
            peData.put("id", eventId);
            peData.put("sessionId", sessionId);
            peData.put("operationId", "funding_" + sessionId);
            peData.put("type", "FUNDING_SUCCEEDED");
            peData.put("fundingDelta", totalDue);
            peData.put("actorId", uid);
            peData.put("createdAt", FieldValue.serverTimestamp());
            tx.set(eventRef, peData);

            return null;
        }));
    }

    public Task<WorkSession> getSession(String sessionId) {
        return bounded(db.collection("workSessions").document(sessionId).get(Source.SERVER).continueWith(t -> {
            if (!t.isSuccessful()) throw t.getException();
            DocumentSnapshot doc = t.getResult();
            return doc != null && doc.exists() ? WorkSession.fromSnapshot(doc) : null;
        }));
    }

    public Task<List<WorkSession>> getSessionsForUser(String uid, String role) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));
        String field = "EMPLOYER".equals(role) ? "employerId" : "seekerId";
        Query q = db.collection("workSessions").whereEqualTo(field, uid).limit(50);

        return bounded(q.get(Source.SERVER).continueWith(t -> {
            if (!t.isSuccessful()) throw t.getException();
            if (!same(uid)) throw new Failure("SESSION");
            QuerySnapshot s = t.getResult();
            List<WorkSession> list = new ArrayList<>();
            if (s != null) {
                for (DocumentSnapshot doc : s) {
                    WorkSession ws = WorkSession.fromSnapshot(doc);
                    if (ws != null) list.add(ws);
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

    public Task<List<WorkSession>> getSessionsForApplication(String uid, String applicationId) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));
        String deterministicId = "session_" + applicationId;

        return bounded(db.collection("workSessions").document(deterministicId).get(Source.SERVER).continueWithTask(tDoc -> {
            if (tDoc.isSuccessful() && tDoc.getResult() != null && tDoc.getResult().exists()) {
                WorkSession ws = WorkSession.fromSnapshot(tDoc.getResult());
                List<WorkSession> list = new ArrayList<>();
                if (ws != null) list.add(ws);
                return Tasks.forResult(list);
            }

            // Fallback for query by employerId
            Query qEmp = db.collection("workSessions")
                    .whereEqualTo("employerId", uid)
                    .whereEqualTo("applicationId", applicationId)
                    .limit(10);

            return qEmp.get(Source.SERVER).continueWithTask(tEmp -> {
                if (tEmp.isSuccessful() && tEmp.getResult() != null && !tEmp.getResult().isEmpty()) {
                    List<WorkSession> list = new ArrayList<>();
                    for (DocumentSnapshot doc : tEmp.getResult()) {
                        WorkSession ws = WorkSession.fromSnapshot(doc);
                        if (ws != null) list.add(ws);
                    }
                    return Tasks.forResult(list);
                }

                // Fallback for query by seekerId
                Query qSeek = db.collection("workSessions")
                        .whereEqualTo("seekerId", uid)
                        .whereEqualTo("applicationId", applicationId)
                        .limit(10);

                return qSeek.get(Source.SERVER).continueWith(tSeek -> {
                    List<WorkSession> list = new ArrayList<>();
                    if (tSeek.isSuccessful() && tSeek.getResult() != null) {
                        for (DocumentSnapshot doc : tSeek.getResult()) {
                            WorkSession ws = WorkSession.fromSnapshot(doc);
                            if (ws != null) list.add(ws);
                        }
                    }
                    return list;
                });
            });
        }));
    }

    public Task<WorkSession> checkScheduleConflict(String seekerId, Timestamp startAt, Timestamp endAt, String excludeSessionId) {
        Query q = db.collection("workSessions").whereEqualTo("seekerId", seekerId).limit(50);

        return bounded(q.get(Source.SERVER).continueWith(t -> {
            if (!t.isSuccessful()) throw t.getException();
            QuerySnapshot s = t.getResult();
            if (s == null || s.isEmpty()) return null;

            long start = startAt.toDate().getTime();
            long end = endAt.toDate().getTime();

            for (DocumentSnapshot doc : s) {
                if (excludeSessionId != null && excludeSessionId.equals(doc.getId())) continue;
                WorkSession ws = WorkSession.fromSnapshot(doc);
                if (ws == null || ws.agreement == null) continue;
                if (ws.isCancelled() || ws.isCompleted()) continue;

                if (ws.agreement.startAt != null && ws.agreement.endAt != null) {
                    long exStart = ws.agreement.startAt.toDate().getTime();
                    long exEnd = ws.agreement.endAt.toDate().getTime();
                    // Overlap condition: start < exEnd && end > exStart
                    if (start < exEnd && end > exStart) {
                        return ws; // Found conflict!
                    }
                }
            }
            return null; // No conflict
        }));
    }

    public static String error(Exception ex) {
        Throwable c = ex;
        for (int i = 0; c != null && i < 10; i++, c = c.getCause()) {
            if (c instanceof Failure) {
                switch (((Failure) c).code) {
                    case "SESSION":
                        return "Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.";
                    case "FORBIDDEN":
                        return "Tài khoản không đủ quyền thực hiện thao tác trên ca làm này.";
                    case "APPLICATION_NOT_ACCEPTED":
                        return "Đơn ứng tuyển chưa được chấp nhận, không thể lập thỏa thuận ca.";
                    case "APPLICATION_NOT_FOUND":
                        return "Không tìm thấy thông tin đơn ứng tuyển.";
                    case "NOT_FOUND":
                        return "Không tìm thấy thông tin ca làm việc.";
                    case "INVALID_TIME":
                        return "Thời gian ca làm không hợp lệ (giờ kết thúc phải sau giờ bắt đầu).";
                    case "TIME_IN_PAST":
                        return "Thời gian bắt đầu ca phải ở tương lai.";
                    case "INVALID_PAY":
                        return "Tiền công ca làm phải lớn hơn 0.";
                    case "CANNOT_EDIT":
                        return "Ca làm việc không còn ở trạng thái chờ duyệt để chỉnh sửa.";
                    case "NOT_PROPOSED":
                        return "Ca làm việc đã được xử lý hoặc đã hủy.";
                    case "VERSION_CONFLICT":
                        return "Thỏa thuận đã được Người thuê cập nhật nội dung mới. Vui lòng xem lại nội dung mới nhất trước khi xác nhận.";
                    case "SCHEDULE_CONFLICT":
                        return "Cô/chú đã có một ca làm việc khác trùng vào khung giờ này.";
                    case "SESSION_ALREADY_EXISTS":
                        return "Đã có thỏa thuận ca làm việc cho đơn ứng tuyển này.";
                    case "CANNOT_CANCEL":
                        return "Ca làm việc hiện không thể hủy.";
                    case "INSUFFICIENT_FUNDS":
                        return "Số dư ví không đủ để cấp tiền ca làm này. Vui lòng nạp thêm tiền vào ví.";
                    case "NOT_AWAITING_FUNDS":
                        return "Ca làm việc chưa ở trạng thái sẵn sàng để cấp tiền.";
                    case "VERSION_MISMATCH":
                        return "Thỏa thuận ca làm chưa được cả hai bên xác nhận cùng một phiên bản.";
                    case "INVALID_TOTAL_DUE":
                        return "Số tiền cần thanh toán cho ca làm không hợp lệ.";
                    default:
                        return "Thao tác chưa hoàn thành. Hãy thử lại.";
                }
            }
            if (c instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException fe = (FirebaseFirestoreException) c;
                if (fe.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    return "Quy tắc bảo mật Firestore từ chối thao tác (PERMISSION_DENIED). Cần đảm bảo tài khoản có quyền hợp lệ.";
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
