package vn.edu.ueh.nhom4.grandmaassist.app.domain.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.*;

public final class Application {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_WITHDRAWN = "WITHDRAWN";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";

    public final String id;
    public final String jobId;
    public final String seekerId;
    public final String employerId;
    public final String message;
    public final String status;
    public final boolean sharePhoneConsent;
    public final ApplicantSnapshot applicantSnapshot;
    public final JobSnapshot jobSnapshot;
    public final EmployerContactSnapshot employerContactSnapshot;
    public final Timestamp createdAt;
    public final Timestamp updatedAt;

    public Application(String id, String jobId, String seekerId, String employerId, String message,
                       String status, boolean sharePhoneConsent, ApplicantSnapshot applicantSnapshot,
                       JobSnapshot jobSnapshot, EmployerContactSnapshot employerContactSnapshot,
                       Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.jobId = jobId;
        this.seekerId = seekerId;
        this.employerId = employerId;
        this.message = message == null ? "" : message;
        this.status = status == null ? STATUS_PENDING : status;
        this.sharePhoneConsent = sharePhoneConsent;
        this.applicantSnapshot = applicantSnapshot;
        this.jobSnapshot = jobSnapshot;
        this.employerContactSnapshot = employerContactSnapshot;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Application(String id, String jobId, String seekerId, String employerId, String message,
                       String status, boolean sharePhoneConsent, ApplicantSnapshot applicantSnapshot,
                       JobSnapshot jobSnapshot, Timestamp createdAt, Timestamp updatedAt) {
        this(id, jobId, seekerId, employerId, message, status, sharePhoneConsent,
             applicantSnapshot, jobSnapshot, null, createdAt, updatedAt);
    }

    @SuppressWarnings("unchecked")
    public static Application fromSnapshot(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        Map<String, Object> data = doc.getData();
        if (data == null) return null;

        String id = doc.getId();
        String jobId = (String) data.get("jobId");
        String seekerId = (String) data.get("seekerId");
        String employerId = (String) data.get("employerId");
        String message = (String) data.get("message");
        String status = (String) data.get("status");
        Boolean consent = (Boolean) data.get("sharePhoneConsent");
        ApplicantSnapshot applicant = ApplicantSnapshot.fromMap((Map<String, Object>) data.get("applicantSnapshot"));
        JobSnapshot job = JobSnapshot.fromMap((Map<String, Object>) data.get("jobSnapshot"));
        EmployerContactSnapshot employer = EmployerContactSnapshot.fromMap((Map<String, Object>) data.get("employerContactSnapshot"));
        Timestamp created = (Timestamp) data.get("createdAt");
        Timestamp updated = (Timestamp) data.get("updatedAt");

        return new Application(id, jobId, seekerId, employerId, message, status,
                Boolean.TRUE.equals(consent), applicant, job, employer, created, updated);
    }

    public boolean isPending() { return STATUS_PENDING.equals(status); }
    public boolean isWithdrawn() { return STATUS_WITHDRAWN.equals(status); }
    public boolean isAccepted() { return STATUS_ACCEPTED.equals(status); }
    public boolean isRejected() { return STATUS_REJECTED.equals(status); }
    public boolean canWithdraw() { return isPending(); }

    public static String statusLabel(String status) {
        if (status == null) return "Chờ phản hồi";
        switch (status) {
            case STATUS_PENDING: return "Chờ phản hồi";
            case STATUS_WITHDRAWN: return "Đã rút đơn";
            case STATUS_ACCEPTED: return "Được chấp nhận";
            case STATUS_REJECTED: return "Bị từ chối";
            default: return status;
        }
    }
}
