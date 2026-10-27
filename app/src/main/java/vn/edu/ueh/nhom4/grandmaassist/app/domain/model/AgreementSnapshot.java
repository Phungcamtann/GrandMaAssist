package vn.edu.ueh.nhom4.grandmaassist.app.domain.model;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public final class AgreementSnapshot {
    public static final String FEE_POLICY_V1 = "GMA_FEE_V1";
    public static final String CANCEL_POLICY_V1 = "GMA_CANCEL_V1";
    public static final int FEE_RATE_BPS = 1000; // 10%

    public final String tasks;
    public final String privateAddress;
    public final String areaCode;
    public final Timestamp startAt;
    public final Timestamp endAt;
    public final long laborAmount;
    public final int feeRateBps;
    public final long serviceFee;
    public final long totalDue;
    public final String feePolicyVersion;
    public final String cancellationPolicyVersion;
    public final Timestamp fundingDeadline;

    public AgreementSnapshot(String tasks, String privateAddress, String areaCode,
                             Timestamp startAt, Timestamp endAt, long laborAmount,
                             int feeRateBps, long serviceFee, long totalDue,
                             String feePolicyVersion, String cancellationPolicyVersion,
                             Timestamp fundingDeadline) {
        this.tasks = tasks == null ? "" : tasks;
        this.privateAddress = privateAddress == null ? "" : privateAddress;
        this.areaCode = areaCode == null ? "" : areaCode;
        this.startAt = startAt;
        this.endAt = endAt;
        this.laborAmount = laborAmount;
        this.feeRateBps = feeRateBps <= 0 ? FEE_RATE_BPS : feeRateBps;
        this.serviceFee = serviceFee;
        this.totalDue = totalDue;
        this.feePolicyVersion = feePolicyVersion == null ? FEE_POLICY_V1 : feePolicyVersion;
        this.cancellationPolicyVersion = cancellationPolicyVersion == null ? CANCEL_POLICY_V1 : cancellationPolicyVersion;
        this.fundingDeadline = fundingDeadline;
    }

    public static long computeFee(long labor) {
        if (labor <= 0) return 0L;
        return Math.round(labor * 0.10);
    }

    public static long computeTotal(long labor) {
        return labor + computeFee(labor);
    }

    public static AgreementSnapshot create(String tasks, String privateAddress, String areaCode,
                                           Timestamp startAt, Timestamp endAt, long laborAmount) {
        long fee = computeFee(laborAmount);
        long total = laborAmount + fee;
        Timestamp deadline = startAt; // Mặc định hạn cấp tiền là trước giờ bắt đầu ca
        return new AgreementSnapshot(tasks, privateAddress, areaCode, startAt, endAt,
                laborAmount, FEE_RATE_BPS, fee, total, FEE_POLICY_V1, CANCEL_POLICY_V1, deadline);
    }

    public static AgreementSnapshot fromMap(Map<String, Object> map) {
        if (map == null) return null;
        String tasks = (String) map.get("tasks");
        String privateAddress = (String) map.get("privateAddress");
        String areaCode = (String) map.get("areaCode");
        Timestamp startAt = (Timestamp) map.get("startAt");
        Timestamp endAt = (Timestamp) map.get("endAt");
        Long laborAmount = (Long) map.get("laborAmount");
        Long feeRateBps = (Long) map.get("feeRateBps");
        Long serviceFee = (Long) map.get("serviceFee");
        Long totalDue = (Long) map.get("totalDue");
        String feePolicy = (String) map.get("feePolicyVersion");
        String cancelPolicy = (String) map.get("cancellationPolicyVersion");
        Timestamp fundingDeadline = (Timestamp) map.get("fundingDeadline");

        return new AgreementSnapshot(
                tasks,
                privateAddress,
                areaCode,
                startAt,
                endAt,
                laborAmount != null ? laborAmount : 0L,
                feeRateBps != null ? feeRateBps.intValue() : FEE_RATE_BPS,
                serviceFee != null ? serviceFee : 0L,
                totalDue != null ? totalDue : 0L,
                feePolicy,
                cancelPolicy,
                fundingDeadline
        );
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("tasks", tasks);
        map.put("privateAddress", privateAddress);
        map.put("areaCode", areaCode);
        map.put("startAt", startAt);
        map.put("endAt", endAt);
        map.put("laborAmount", laborAmount);
        map.put("feeRateBps", (long) feeRateBps);
        map.put("serviceFee", serviceFee);
        map.put("totalDue", totalDue);
        map.put("feePolicyVersion", feePolicyVersion);
        map.put("cancellationPolicyVersion", cancellationPolicyVersion);
        map.put("fundingDeadline", fundingDeadline);
        return map;
    }
}
