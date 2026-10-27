package vn.edu.ueh.nhom4.grandmaassist.app.domain.model;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public final class EmployerContactSnapshot {
    public final String displayName;
    public final String phoneE164;
    public final Timestamp acceptedAt;

    public EmployerContactSnapshot(String displayName, String phoneE164, Timestamp acceptedAt) {
        this.displayName = displayName == null ? "" : displayName;
        this.phoneE164 = phoneE164 == null ? "" : phoneE164;
        this.acceptedAt = acceptedAt;
    }

    public static EmployerContactSnapshot fromMap(Map<String, Object> map) {
        if (map == null) return null;
        String name = (String) map.get("displayName");
        String phone = (String) map.get("phoneE164");
        Timestamp accepted = (Timestamp) map.get("acceptedAt");
        return new EmployerContactSnapshot(name, phone, accepted);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("displayName", displayName);
        map.put("phoneE164", phoneE164);
        map.put("acceptedAt", acceptedAt);
        return map;
    }
}
