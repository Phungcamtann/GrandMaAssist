package vn.edu.ueh.nhom4.grandmaassist.app.domain.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Map;

public final class WorkSession {
    public static final String STATUS_PROPOSED = "PROPOSED";
    public static final String STATUS_AWAITING_FUNDS = "AWAITING_FUNDS";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETION_PENDING = "COMPLETION_PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_DISPUTED = "DISPUTED";

    public static final String PAYMENT_UNFUNDED = "UNFUNDED";
    public static final String PAYMENT_FUNDING = "FUNDING";
    public static final String PAYMENT_HELD = "HELD";
    public static final String PAYMENT_SETTLING = "SETTLING";
    public static final String PAYMENT_SETTLED = "SETTLED";

    public final String id;
    public final String applicationId;
    public final String jobId;
    public final String seekerId;
    public final String employerId;
    public final AgreementSnapshot agreement;
    public final long agreementVersion;
    public final Long employerAcceptedVersion;
    public final Long seekerAcceptedVersion;
    public final String status;
    public final String paymentStatus;
    public final long heldAmount;
    public final long totalFunded;
    public final Timestamp createdAt;
    public final Timestamp updatedAt;

    public WorkSession(String id, String applicationId, String jobId, String seekerId, String employerId,
                       AgreementSnapshot agreement, long agreementVersion,
                       Long employerAcceptedVersion, Long seekerAcceptedVersion,
                       String status, String paymentStatus, long heldAmount, long totalFunded,
                       Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.applicationId = applicationId;
        this.jobId = jobId;
        this.seekerId = seekerId;
        this.employerId = employerId;
        this.agreement = agreement;
        this.agreementVersion = agreementVersion;
        this.employerAcceptedVersion = employerAcceptedVersion;
        this.seekerAcceptedVersion = seekerAcceptedVersion;
        this.status = status == null ? STATUS_PROPOSED : status;
        this.paymentStatus = paymentStatus == null ? PAYMENT_UNFUNDED : paymentStatus;
        this.heldAmount = heldAmount;
        this.totalFunded = totalFunded;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @SuppressWarnings("unchecked")
    public static WorkSession fromSnapshot(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        Map<String, Object> data = doc.getData();
        if (data == null) return null;

        String id = doc.getId();
        String applicationId = (String) data.get("applicationId");
        String jobId = (String) data.get("jobId");
        String seekerId = (String) data.get("seekerId");
        String employerId = (String) data.get("employerId");
        AgreementSnapshot agreement = AgreementSnapshot.fromMap((Map<String, Object>) data.get("agreement"));
        Long version = (Long) data.get("agreementVersion");
        Long empVer = (Long) data.get("employerAcceptedVersion");
        Long seekVer = (Long) data.get("seekerAcceptedVersion");
        String status = (String) data.get("status");
        String paymentStatus = (String) data.get("paymentStatus");
        Long hAmt = (Long) data.get("heldAmount");
        Long tFund = (Long) data.get("totalFunded");
        Timestamp createdAt = (Timestamp) data.get("createdAt");
        Timestamp updatedAt = (Timestamp) data.get("updatedAt");

        return new WorkSession(
                id,
                applicationId,
                jobId,
                seekerId,
                employerId,
                agreement,
                version != null ? version : 1L,
                empVer,
                seekVer,
                status,
                paymentStatus,
                hAmt != null ? hAmt : 0L,
                tFund != null ? tFund : 0L,
                createdAt,
                updatedAt
        );
    }

    public boolean isProposed() { return STATUS_PROPOSED.equals(status); }
    public boolean isAwaitingFunds() { return STATUS_AWAITING_FUNDS.equals(status); }
    public boolean isReady() { return STATUS_READY.equals(status); }
    public boolean isFunded() { return PAYMENT_HELD.equals(paymentStatus); }
    public boolean isInProgress() { return STATUS_IN_PROGRESS.equals(status); }
    public boolean isCompleted() { return STATUS_COMPLETED.equals(status); }
    public boolean isCancelled() { return STATUS_CANCELLED.equals(status); }

    public boolean isAgreementConfirmed() {
        return employerAcceptedVersion != null && seekerAcceptedVersion != null
                && employerAcceptedVersion.equals(seekerAcceptedVersion)
                && employerAcceptedVersion == agreementVersion;
    }

    public static String statusLabel(String status) {
        if (status == null) return "Đang đề xuất";
        switch (status) {
            case STATUS_PROPOSED: return "Chờ xác nhận";
            case STATUS_AWAITING_FUNDS: return "Đã thỏa thuận";
            case STATUS_READY: return "Sẵn sàng";
            case STATUS_IN_PROGRESS: return "Đang thực hiện";
            case STATUS_COMPLETION_PENDING: return "Chờ xác nhận xong";
            case STATUS_COMPLETED: return "Đã hoàn thành";
            case STATUS_CANCELLED: return "Đã hủy";
            case STATUS_DISPUTED: return "Đang khiếu nại";
            default: return status;
        }
    }
}
