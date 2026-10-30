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
        return cancelUnfundedSession(uid, sessionId, "Người tìm việc từ chối thỏa thuận", "SEEKER");
    }

    public Task<Void> cancelUnfundedSession(String uid, String sessionId, String reason, String role) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));

        return bounded(db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");

            DocumentReference sessionRef = db.collection("workSessions").document(sessionId);
            DocumentSnapshot session = tx.get(sessionRef);
            if (!session.exists()) throw new Failure("NOT_FOUND");
            if (!uid.equals(session.getString("seekerId")) && !uid.equals(session.getString("employerId"))) {
                throw new Failure("FORBIDDEN");
            }
            String status = session.getString("status");
            if (!WorkSession.STATUS_PROPOSED.equals(status) && !WorkSession.STATUS_AWAITING_FUNDS.equals(status)) {
                throw new Failure("CANNOT_CANCEL");
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", WorkSession.STATUS_CANCELLED);
            updates.put("cancelledAt", FieldValue.serverTimestamp());
            updates.put("cancellationReason", reason != null ? reason : "");
            updates.put("cancelInitiatorRole", role != null ? role : "");
            updates.put("updatedAt", FieldValue.serverTimestamp());

            tx.update(sessionRef, updates);
            return null;
        }));
    }

    public Task<Void> requestCancelSession(String uid, String sessionId, String reason, String role) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));

        return bounded(db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");

            DocumentReference sessionRef = db.collection("workSessions").document(sessionId);
            DocumentSnapshot session = tx.get(sessionRef);
            if (!session.exists()) throw new Failure("NOT_FOUND");
            if (!uid.equals(session.getString("seekerId")) && !uid.equals(session.getString("employerId"))) {
                throw new Failure("FORBIDDEN");
            }

            String status = session.getString("status");
            if (WorkSession.STATUS_PROPOSED.equals(status) || WorkSession.STATUS_AWAITING_FUNDS.equals(status)) {
                // Cancel directly
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", WorkSession.STATUS_CANCELLED);
                updates.put("cancelledAt", FieldValue.serverTimestamp());
                updates.put("cancellationReason", reason != null ? reason : "");
                updates.put("cancelInitiatorRole", role != null ? role : "");
                updates.put("updatedAt", FieldValue.serverTimestamp());
                tx.update(sessionRef, updates);
                return null;
            }

            if (!WorkSession.STATUS_READY.equals(status)) {
                throw new Failure("CANNOT_CANCEL");
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("cancellationRequestedBy", uid);
            updates.put("cancellationReason", reason != null ? reason : "");
            updates.put("cancellationRequestedAt", FieldValue.serverTimestamp());
            updates.put("cancelInitiatorRole", role != null ? role : "");
            updates.put("updatedAt", FieldValue.serverTimestamp());

            tx.update(sessionRef, updates);
            return null;
        }));
    }

    public Task<Void> confirmCancelSession(String uid, String sessionId, String role) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));

        DocumentReference sessionRef = db.collection("workSessions").document(sessionId);
        return bounded(db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");

            DocumentSnapshot session = tx.get(sessionRef);
            if (!session.exists()) throw new Failure("NOT_FOUND");
            if (!uid.equals(session.getString("seekerId")) && !uid.equals(session.getString("employerId"))) {
                throw new Failure("FORBIDDEN");
            }

            String status = session.getString("status");
            if (!WorkSession.STATUS_READY.equals(status)) {
                throw new Failure("CANNOT_CANCEL");
            }

            String employerId = session.getString("employerId");
            Long totalFunded = session.getLong("totalFunded");
            long refundAmount = totalFunded != null ? totalFunded : 0L;
            String reason = session.getString("cancellationReason");

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", WorkSession.STATUS_CANCELLED);
            updates.put("paymentStatus", WorkSession.PAYMENT_REFUNDED);
            updates.put("heldAmount", 0L);
            updates.put("cancelledAt", FieldValue.serverTimestamp());
            updates.put("updatedAt", FieldValue.serverTimestamp());

            tx.update(sessionRef, updates);

            // Record paymentEvent
            String eventId = "pe_refund_" + sessionId;
            DocumentReference eventRef = sessionRef.collection("paymentEvents").document(eventId);
            Map<String, Object> peData = new HashMap<>();
            peData.put("id", eventId);
            peData.put("sessionId", sessionId);
            peData.put("operationId", "refund_" + sessionId);
            peData.put("type", "REFUND_COMPLETED");
            peData.put("refundDelta", refundAmount);
            peData.put("actorId", uid);
            peData.put("createdAt", FieldValue.serverTimestamp());
            tx.set(eventRef, peData);

            // If actor is Employer, credit employer wallet directly in the transaction
            if (uid.equals(employerId) && refundAmount > 0) {
                DocumentReference wRef = db.collection("wallets").document(employerId);
                DocumentSnapshot wSnap = tx.get(wRef);
                long curBalance = 0L;
                if (wSnap.exists()) {
                    Long b = wSnap.getLong("balance");
                    if (b != null) curBalance = b;
                }
                long newBalance = curBalance + refundAmount;
                Map<String, Object> wData = new HashMap<>();
                wData.put("balance", newBalance);
                wData.put("updatedAt", FieldValue.serverTimestamp());
                tx.set(wRef, wData, SetOptions.merge());

                String txId = "tx_refund_" + sessionId;
                DocumentReference txRef = db.collection("wallets").document(employerId)
                        .collection("transactions").document(txId);
                String displayId = sessionId.startsWith("session_") ? sessionId.substring("session_".length()) : sessionId;
                Map<String, Object> txData = new HashMap<>();
                txData.put("id", txId);
                txData.put("type", "REFUND");
                txData.put("amount", refundAmount);
                txData.put("balanceAfter", newBalance);
                txData.put("description", "Hoàn tiền cọc ca làm đã hủy #" + displayId + (reason != null && !reason.isEmpty() ? " - " + reason : ""));
                txData.put("refId", sessionId);
                txData.put("createdAt", FieldValue.serverTimestamp());
                tx.set(txRef, txData);
            }

            return null;
        }).continueWithTask(t -> {
            if (!t.isSuccessful()) return Tasks.forException(t.getException());
            // If actor was Seeker, try to credit employer's wallet asynchronously as best-effort
            sessionRef.get().addOnSuccessListener(doc -> {
                if (doc != null && doc.exists()) {
                    String empId = doc.getString("employerId");
                    Long tf = doc.getLong("totalFunded");
                    if (empId != null && tf != null && tf > 0 && !uid.equals(empId)) {
                        tryCreditEmployerRefund(empId, sessionId, tf, doc.getString("cancellationReason"));
                    }
                }
            });
            return Tasks.forResult(null);
        }));
    }

    private void tryCreditEmployerRefund(String employerId, String sessionId, long amount, String reason) {
        String txId = "tx_refund_" + sessionId;
        DocumentReference txRef = db.collection("wallets").document(employerId)
                .collection("transactions").document(txId);
        txRef.get().addOnSuccessListener(snap -> {
            if (snap != null && snap.exists()) return;
            DocumentReference wRef = db.collection("wallets").document(employerId);
            wRef.get().addOnSuccessListener(wSnap -> {
                long curBalance = 0L;
                if (wSnap != null && wSnap.exists()) {
                    Long b = wSnap.getLong("balance");
                    if (b != null) curBalance = b;
                }
                long newBalance = curBalance + amount;
                Map<String, Object> wData = new HashMap<>();
                wData.put("balance", newBalance);
                wData.put("updatedAt", FieldValue.serverTimestamp());
                wRef.set(wData, SetOptions.merge()).addOnSuccessListener(v -> {
                    String displayId = sessionId.startsWith("session_") ? sessionId.substring("session_".length()) : sessionId;
                    Map<String, Object> txData = new HashMap<>();
                    txData.put("id", txId);
                    txData.put("type", "REFUND");
                    txData.put("amount", amount);
                    txData.put("balanceAfter", newBalance);
                    txData.put("description", "Hoàn tiền cọc ca làm đã hủy #" + displayId + (reason != null && !reason.isEmpty() ? " - " + reason : ""));
                    txData.put("refId", sessionId);
                    txData.put("createdAt", FieldValue.serverTimestamp());
                    txRef.set(txData);
                });
            });
        });
    }

    public Task<Void> rejectCancelRequest(String uid, String sessionId) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));

        return bounded(db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");

            DocumentReference sessionRef = db.collection("workSessions").document(sessionId);
            DocumentSnapshot session = tx.get(sessionRef);
            if (!session.exists()) throw new Failure("NOT_FOUND");
            if (!uid.equals(session.getString("seekerId")) && !uid.equals(session.getString("employerId"))) {
                throw new Failure("FORBIDDEN");
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("cancellationRequestedBy", FieldValue.delete());
            updates.put("cancellationReason", FieldValue.delete());
            updates.put("cancellationRequestedAt", FieldValue.delete());
            updates.put("cancelInitiatorRole", FieldValue.delete());
            updates.put("updatedAt", FieldValue.serverTimestamp());

            tx.update(sessionRef, updates);
            return null;
        }));
    }

    public Task<Void> withdrawCancelRequest(String uid, String sessionId) {
        return rejectCancelRequest(uid, sessionId);
    }

    public Task<Void> syncRefundToEmployerWallet(String employerId, WorkSession session) {
        if (!same(employerId) || session == null || !session.isCancelled()) return Tasks.forResult(null);
        if (session.totalFunded <= 0) return Tasks.forResult(null);

        String txId = "tx_refund_" + session.id;
        DocumentReference txRef = db.collection("wallets").document(employerId)
                .collection("transactions").document(txId);

        return bounded(txRef.get().continueWithTask(t -> {
            if (t.isSuccessful() && t.getResult() != null && t.getResult().exists()) {
                // Already credited
                return Tasks.forResult(null);
            }
            long refundAmount = session.totalFunded;
            return db.runTransaction(tx -> {
                DocumentReference wRef = db.collection("wallets").document(employerId);
                DocumentSnapshot wSnap = tx.get(wRef);
                long curBalance = 0L;
                if (wSnap.exists()) {
                    Long b = wSnap.getLong("balance");
                    if (b != null) curBalance = b;
                }
                long newBalance = curBalance + refundAmount;
                Map<String, Object> wData = new HashMap<>();
                wData.put("balance", newBalance);
                wData.put("updatedAt", FieldValue.serverTimestamp());
                tx.set(wRef, wData, SetOptions.merge());

                String displayId = session.id.startsWith("session_") ? session.id.substring("session_".length()) : session.id;
                Map<String, Object> txData = new HashMap<>();
                txData.put("id", txId);
                txData.put("type", "REFUND");
                txData.put("amount", refundAmount);
                txData.put("balanceAfter", newBalance);
                String desc = "Hoàn tiền cọc ca làm đã hủy #" + displayId + (session.cancellationReason != null && !session.cancellationReason.isEmpty() ? " - " + session.cancellationReason : "");
                txData.put("description", desc);
                txData.put("refId", session.id);
                txData.put("createdAt", FieldValue.serverTimestamp());
                tx.set(txRef, txData);
                return null;
            });
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

    public Task<WorkSession> confirmCheckIn(String uid, String sessionId, String role) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));

        return bounded(db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");

            DocumentReference sessionRef = db.collection("workSessions").document(sessionId);
            DocumentSnapshot session = tx.get(sessionRef);
            if (!session.exists()) throw new Failure("NOT_FOUND");

            String seekerId = session.getString("seekerId");
            String employerId = session.getString("employerId");
            String status = session.getString("status");
            String paymentStatus = session.getString("paymentStatus");

            if (!WorkSession.STATUS_READY.equals(status) && !WorkSession.STATUS_IN_PROGRESS.equals(status)) {
                throw new Failure("NOT_READY");
            }
            if (!WorkSession.PAYMENT_HELD.equals(paymentStatus)) {
                throw new Failure("NOT_FUNDED");
            }

            Map<String, Object> updates = new HashMap<>();
            boolean isSeeker = uid.equals(seekerId);
            boolean isEmployer = uid.equals(employerId);

            if (!isSeeker && !isEmployer) {
                throw new Failure("FORBIDDEN");
            }

            Timestamp existingSeekerCheckIn = session.getTimestamp("seekerCheckedInAt");
            Timestamp existingEmployerCheckIn = session.getTimestamp("employerCheckedInAt");

            if (isSeeker) {
                if (existingSeekerCheckIn != null) {
                    throw new Failure("ALREADY_CHECKED_IN");
                }
                updates.put("seekerCheckedInAt", FieldValue.serverTimestamp());
                if (existingEmployerCheckIn != null) {
                    updates.put("status", WorkSession.STATUS_IN_PROGRESS);
                    updates.put("startedAt", FieldValue.serverTimestamp());
                }
            } else {
                if (existingEmployerCheckIn != null) {
                    throw new Failure("ALREADY_CHECKED_IN");
                }
                updates.put("employerCheckedInAt", FieldValue.serverTimestamp());
                if (existingSeekerCheckIn != null) {
                    updates.put("status", WorkSession.STATUS_IN_PROGRESS);
                    updates.put("startedAt", FieldValue.serverTimestamp());
                }
            }

            updates.put("updatedAt", FieldValue.serverTimestamp());
            tx.update(sessionRef, updates);
            return null;
        }).continueWithTask(t -> {
            if (!t.isSuccessful()) throw t.getException();
            return getSession(sessionId);
        }));
    }

    public Task<WorkSession> reportCompletion(String uid, String sessionId) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));

        return bounded(db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");

            DocumentReference sessionRef = db.collection("workSessions").document(sessionId);
            DocumentSnapshot session = tx.get(sessionRef);
            if (!session.exists()) throw new Failure("NOT_FOUND");

            String seekerId = session.getString("seekerId");
            if (!uid.equals(seekerId)) throw new Failure("FORBIDDEN");

            String status = session.getString("status");
            if (!WorkSession.STATUS_IN_PROGRESS.equals(status)) {
                throw new Failure("NOT_IN_PROGRESS");
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", WorkSession.STATUS_COMPLETION_PENDING);
            updates.put("completedAt", FieldValue.serverTimestamp());
            updates.put("updatedAt", FieldValue.serverTimestamp());

            tx.update(sessionRef, updates);
            return null;
        }).continueWithTask(t -> {
            if (!t.isSuccessful()) throw t.getException();
            return getSession(sessionId);
        }));
    }

    public Task<WorkSession> confirmCompletionAndSettle(String uid, String sessionId) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));

        return bounded(db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");

            DocumentReference sessionRef = db.collection("workSessions").document(sessionId);
            DocumentSnapshot session = tx.get(sessionRef);
            if (!session.exists()) throw new Failure("NOT_FOUND");

            String employerId = session.getString("employerId");
            if (!uid.equals(employerId)) throw new Failure("FORBIDDEN");

            String status = session.getString("status");
            if (!WorkSession.STATUS_COMPLETION_PENDING.equals(status) && !WorkSession.STATUS_IN_PROGRESS.equals(status)) {
                throw new Failure("CANNOT_SETTLE");
            }

            String jobId = session.getString("jobId");
            DocumentReference jobRef = (jobId != null && !jobId.trim().isEmpty()) ? db.collection("jobs").document(jobId) : null;
            DocumentSnapshot jobSnap = (jobRef != null) ? tx.get(jobRef) : null;

            String seekerId = session.getString("seekerId");
            Object agObj = session.get("agreement");
            long laborAmount = 0L;
            long serviceFee = 0L;
            if (agObj instanceof Map) {
                Object la = ((Map<?, ?>) agObj).get("laborAmount");
                if (la instanceof Number) laborAmount = ((Number) la).longValue();
                Object sf = ((Map<?, ?>) agObj).get("serviceFee");
                if (sf instanceof Number) serviceFee = ((Number) sf).longValue();
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", WorkSession.STATUS_COMPLETED);
            updates.put("paymentStatus", WorkSession.PAYMENT_SETTLED);
            updates.put("heldAmount", 0L);
            updates.put("settledLaborAmount", laborAmount);
            updates.put("settledServiceFee", serviceFee);
            updates.put("settledAt", FieldValue.serverTimestamp());
            updates.put("updatedAt", FieldValue.serverTimestamp());

            tx.update(sessionRef, updates);

            // Đóng bài đăng tuyển dụng (CLOSED) khi hoàn tất ca để không còn xuất hiện trên trang xem việc
            if (jobRef != null && jobSnap != null && jobSnap.exists()
                    && "OPEN".equals(jobSnap.getString("status")) && uid.equals(jobSnap.getString("employerId"))) {
                Long v = jobSnap.getLong("version");
                long nextVersion = (v != null ? v : 1L) + 1;
                Map<String, Object> jobData = new HashMap<>(Objects.requireNonNull(jobSnap.getData()));
                jobData.put("version", nextVersion);
                jobData.put("status", "CLOSED");
                jobData.put("updatedAt", FieldValue.serverTimestamp());
                tx.set(jobRef, jobData);

                Map<String, Object> e = new HashMap<>();
                e.put("jobId", jobId);
                e.put("version", nextVersion);
                e.put("actorId", uid);
                e.put("action", "CLOSED");
                e.put("createdAt", FieldValue.serverTimestamp());
                e.put("before", jobSnap.getData());
                e.put("after", new HashMap<>(jobData));
                tx.set(jobRef.collection("history").document(String.valueOf(nextVersion)), e);
            }

            return null;
        }).continueWithTask(t -> {
            if (!t.isSuccessful()) throw t.getException();
            return getSession(sessionId);
        }).continueWith(t -> {
            if (!t.isSuccessful()) throw t.getException();
            WorkSession s = t.getResult();
            if (s != null) {
                if (s.seekerId != null) {
                    tryCreditSeekerWallet(s);
                }
                if (s.jobId != null && !s.jobId.isEmpty()) {
                    new JobPostRepository().closeJobById(uid, s.jobId);
                }
            }
            return s;
        }));
    }

    private void tryCreditSeekerWallet(WorkSession session) {
        if (session == null || session.seekerId == null || session.agreement == null) return;
        long laborAmount = session.agreement.laborAmount;
        if (laborAmount <= 0) return;
        String seekerId = session.seekerId;
        String txId = "tx_settle_" + session.id;

        DocumentReference txRef = db.collection("wallets").document(seekerId)
                .collection("transactions").document(txId);
        txRef.get().addOnSuccessListener(doc -> {
            if (doc != null && doc.exists()) return;
            DocumentReference wRef = db.collection("wallets").document(seekerId);
            wRef.get().addOnSuccessListener(wDoc -> {
                long curBalance = 0L;
                if (wDoc != null && wDoc.exists()) {
                    Long b = wDoc.getLong("balance");
                    if (b != null) curBalance = b;
                }
                long newBalance = curBalance + laborAmount;
                Map<String, Object> wData = new HashMap<>();
                wData.put("balance", newBalance);
                wData.put("updatedAt", FieldValue.serverTimestamp());
                wRef.set(wData, SetOptions.merge()).addOnSuccessListener(v -> {
                    String displayId = session.id.startsWith("session_") ? session.id.substring("session_".length()) : session.id;
                    Map<String, Object> txData = new HashMap<>();
                    txData.put("id", txId);
                    txData.put("type", "PAYMENT_RECEIVED");
                    txData.put("amount", laborAmount);
                    txData.put("balanceAfter", newBalance);
                    txData.put("description", "Nhận tiền công hoàn thành ca làm việc #" + displayId);
                    txData.put("refId", session.id);
                    txData.put("createdAt", FieldValue.serverTimestamp());
                    txRef.set(txData);
                });
            });
        });
    }

    public Task<Void> syncSettlementToWallet(String seekerId, WorkSession session) {
        if (!same(seekerId) || session == null || !session.isCompleted()) return Tasks.forResult(null);
        if (session.agreement == null || session.agreement.laborAmount <= 0) return Tasks.forResult(null);

        String txId = "tx_settle_" + session.id;
        DocumentReference txRef = db.collection("wallets").document(seekerId)
                .collection("transactions").document(txId);

        return bounded(txRef.get().continueWithTask(t -> {
            if (t.isSuccessful() && t.getResult() != null && t.getResult().exists()) {
                // Already synced
                return Tasks.forResult(null);
            }
            // Need to credit wallet
            long laborAmount = session.agreement.laborAmount;
            return db.runTransaction(tx -> {
                DocumentReference wRef = db.collection("wallets").document(seekerId);
                DocumentSnapshot wSnap = tx.get(wRef);
                long curBalance = 0L;
                if (wSnap.exists()) {
                    Long b = wSnap.getLong("balance");
                    if (b != null) curBalance = b;
                }
                long newBalance = curBalance + laborAmount;
                Map<String, Object> wData = new HashMap<>();
                wData.put("balance", newBalance);
                wData.put("updatedAt", FieldValue.serverTimestamp());
                tx.set(wRef, wData, SetOptions.merge());

                String displayId = session.id.startsWith("session_") ? session.id.substring("session_".length()) : session.id;
                Map<String, Object> txData = new HashMap<>();
                txData.put("id", txId);
                txData.put("type", "PAYMENT_RECEIVED");
                txData.put("amount", laborAmount);
                txData.put("balanceAfter", newBalance);
                txData.put("description", "Nhận tiền công hoàn thành ca làm việc #" + displayId);
                txData.put("refId", session.id);
                txData.put("createdAt", FieldValue.serverTimestamp());
                tx.set(txRef, txData);
                return null;
            });
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
                    case "NOT_READY":
                        return "Ca làm việc chưa ở trạng thái sẵn sàng để điểm danh.";
                    case "NOT_FUNDED":
                        return "Ca làm việc chưa được người thuê nạp cọc bảo chứng.";
                    case "ALREADY_CHECKED_IN":
                        return "Bạn đã điểm danh trước đó rồi.";
                    case "NOT_IN_PROGRESS":
                        return "Ca làm việc chưa bắt đầu hoặc đã kết thúc.";
                    case "CANNOT_SETTLE":
                        return "Ca làm việc chưa ở trạng thái để xác nhận hoàn tất.";
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
