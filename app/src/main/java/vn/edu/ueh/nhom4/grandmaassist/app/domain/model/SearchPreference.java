package vn.edu.ueh.nhom4.grandmaassist.app.domain.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;

public final class SearchPreference {
    public final List<String> categoryIds;
    public final List<String> areaCodes;
    public final List<String> days;
    public final List<String> shifts;
    public final List<String> avoidedDemands;
    public final long updatedAt;

    public SearchPreference(List<String> categoryIds, List<String> areaCodes, List<String> days, List<String> shifts, List<String> avoidedDemands, long updatedAt) {
        this.categoryIds = copy(categoryIds);
        this.areaCodes = copy(areaCodes);
        this.days = copy(days);
        this.shifts = copy(shifts);
        this.avoidedDemands = copy(avoidedDemands);
        this.updatedAt = updatedAt;
    }

    private static List<String> copy(List<String> src) {
        return Collections.unmodifiableList(src != null ? new ArrayList<>(src) : Collections.emptyList());
    }

    public String toJson() {
        try {
            return new JSONObject().put("schemaVersion", 1)
                    .put("categoryIds", new JSONArray(categoryIds))
                    .put("areaCodes", new JSONArray(areaCodes))
                    .put("days", new JSONArray(days))
                    .put("shifts", new JSONArray(shifts))
                    .put("avoidedDemands", new JSONArray(avoidedDemands))
                    .put("updatedAt", updatedAt).toString();
        } catch (JSONException e) { return "{}"; }
    }

    public static SearchPreference fromJson(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            JSONObject j = new JSONObject(json);
            if (j.optInt("schemaVersion", 0) != 1) return null;
            return new SearchPreference(parseList(j, "categoryIds"), parseList(j, "areaCodes"),
                    parseList(j, "days"), parseList(j, "shifts"),
                    parseList(j, "avoidedDemands"), j.optLong("updatedAt", 0));
        } catch (Exception e) { return null; }
    }

    private static List<String> parseList(JSONObject j, String key) {
        List<String> out = new ArrayList<>();
        JSONArray a = j.optJSONArray(key);
        if (a != null) {
            for (int i = 0; i < a.length(); i++) {
                String s = a.optString(i, null);
                if (s != null && !s.isEmpty()) out.add(s);
            }
        }
        return out;
    }

    public boolean hasCategory(String id) { return id != null && categoryIds.contains(id); }
    public boolean hasArea(String code) { return code != null && areaCodes.contains(code); }
    public boolean isShiftFlexible() { return shifts.contains("FLEXIBLE"); }
    public boolean isEmpty() {
        return categoryIds.isEmpty() && areaCodes.isEmpty() && days.isEmpty() && shifts.isEmpty() && avoidedDemands.isEmpty();
    }
}
