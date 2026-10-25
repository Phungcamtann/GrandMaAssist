package vn.edu.ueh.nhom4.grandmaassist.app.domain.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/** Editable, memory-only professional profile. No legal identity or contact fields. */
public final class ProfileDraft {
    public String displayName = "";
    public String areaCode = "";
    public String skills = "";
    public String experience = "";
    public boolean morning, afternoon, evening;
    public String employerType = "INDIVIDUAL";
    public String organizationName = "";
    public String introduction = "";

    public ProfileDraft copy() {
        ProfileDraft d = new ProfileDraft();
        d.displayName = displayName; d.areaCode = areaCode; d.skills = skills;
        d.experience = experience; d.morning = morning; d.afternoon = afternoon;
        d.evening = evening; d.employerType = employerType;
        d.organizationName = organizationName; d.introduction = introduction;
        return d;
    }

    public List<String> skillList() {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String value : skills.split(",|\n")) {
            if (!value.trim().isEmpty()) values.add(value.trim());
        }
        return new ArrayList<>(values);
    }

    public List<String> shifts() {
        List<String> values = new ArrayList<>();
        if (morning) values.add("MORNING");
        if (afternoon) values.add("AFTERNOON");
        if (evening) values.add("EVENING");
        return values;
    }

    public boolean valid(String role) {
        if (displayName.trim().isEmpty() || displayName.length() > 100 || areaCode.length() > 80) return false;
        if ("SEEKER".equals(role)) {
            if (experience.length() > 1000 || skills.length() > 1000 || skillList().size() > 8) return false;
            for (String skill : skillList()) if (skill.length() > 60) return false;
            return true;
        }
        return Arrays.asList("INDIVIDUAL", "ORGANIZATION").contains(employerType)
                && organizationName.length() <= 150 && introduction.length() <= 1000
                && (!"ORGANIZATION".equals(employerType) || !organizationName.trim().isEmpty());
    }

    /** Completeness is descriptive only, never permission to apply/post. */
    public boolean professionalDetailsPresent(String role) {
        return !areaCode.isEmpty() && ("SEEKER".equals(role)
                ? !skillList().isEmpty() && !experience.trim().isEmpty() && !shifts().isEmpty()
                : !introduction.trim().isEmpty());
    }
}
