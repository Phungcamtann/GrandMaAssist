package vn.edu.ueh.nhom4.grandmaassist.app.domain.model;

import com.google.firebase.Timestamp;
import java.util.*;

public final class ApplicantSnapshot {
    public final String displayName;
    public final String phoneE164;
    public final List<String> skills;
    public final String experienceSummary;
    public final String areaCode;
    public final long profileVersion;
    public final Timestamp consentAt;

    public ApplicantSnapshot(String displayName, String phoneE164, List<String> skills,
                             String experienceSummary, String areaCode, long profileVersion, Timestamp consentAt) {
        this.displayName = displayName == null ? "" : displayName;
        this.phoneE164 = phoneE164 == null ? "" : phoneE164;
        this.skills = skills == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(skills));
        this.experienceSummary = experienceSummary == null ? "" : experienceSummary;
        this.areaCode = areaCode == null ? "" : areaCode;
        this.profileVersion = profileVersion;
        this.consentAt = consentAt;
    }

    @SuppressWarnings("unchecked")
    public static ApplicantSnapshot fromMap(Map<String, Object> map) {
        if (map == null) return null;
        String name = (String) map.get("displayName");
        String phone = (String) map.get("phoneE164");
        List<String> skillsList = (List<String>) map.get("skills");
        String exp = (String) map.get("experienceSummary");
        String area = (String) map.get("areaCode");
        Long ver = (Long) map.get("profileVersion");
        Timestamp consent = (Timestamp) map.get("consentAt");
        return new ApplicantSnapshot(name, phone, skillsList, exp, area, ver != null ? ver : 1L, consent);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("displayName", displayName);
        map.put("phoneE164", phoneE164);
        map.put("skills", new ArrayList<>(skills));
        map.put("experienceSummary", experienceSummary);
        map.put("areaCode", areaCode);
        map.put("profileVersion", profileVersion);
        map.put("consentAt", consentAt);
        return map;
    }
}
