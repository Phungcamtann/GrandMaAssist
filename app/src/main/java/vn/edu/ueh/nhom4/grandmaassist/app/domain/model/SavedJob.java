package vn.edu.ueh.nhom4.grandmaassist.app.domain.model;

public final class SavedJob {
    public final String seekerId;
    public final String jobId;
    public final long savedAt;
    public final PublicJob job;
    public final String jobStatus;

    public SavedJob(String seekerId, String jobId, long savedAt, PublicJob job, String jobStatus) {
        this.seekerId = seekerId != null ? seekerId : "";
        this.jobId = jobId != null ? jobId : "";
        this.savedAt = savedAt;
        this.job = job;
        this.jobStatus = jobStatus != null ? jobStatus : "OPEN";
    }

    public boolean isOpen() {
        return "OPEN".equals(jobStatus) && (job == null || job.expiresAt > System.currentTimeMillis());
    }

    public boolean isClosed() {
        return "CLOSED".equals(jobStatus) || "HIDDEN".equals(jobStatus);
    }

    public boolean isExpired() {
        return job != null && job.expiresAt <= System.currentTimeMillis();
    }
}
