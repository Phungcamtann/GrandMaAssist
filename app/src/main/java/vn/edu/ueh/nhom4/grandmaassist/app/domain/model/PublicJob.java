package vn.edu.ueh.nhom4.grandmaassist.app.domain.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;

/** Explicit public projection. Never serialize a Firestore document or a private JobPost wholesale. */
public final class PublicJob {
    public final String id, title, employerName, categoryId, categoryName, areaCode, areaName;
    public final String workAddress, description, requirements, shift, schedule, payUnit;
    public final List<String> skills, days, demands;
    public final long payAmount, expiresAt, version, updatedAt, cachedAt;

    private PublicJob(JSONObject j, long savedAt) throws JSONException {
        if (j.getInt("schemaVersion") != 1) throw new JSONException("SCHEMA");
        id = string(j,"id",128,true); title = string(j,"title",120,true);
        employerName = string(j,"employerName",150,true);
        categoryId = string(j,"categoryId",80,true); categoryName = string(j,"categoryName",120,true);
        areaCode = string(j,"areaCode",80,true); areaName = string(j,"areaName",120,true);
        workAddress = string(j,"workAddress",500,true); description = string(j,"description",3000,true);
        requirements = string(j,"requirements",1000,false); shift = string(j,"shift",20,true);
        schedule = string(j,"schedule",500,true); payUnit = string(j,"payUnit",20,true);
        skills = list(j,"skills",8,60); days = list(j,"days",7,20); demands = list(j,"demands",3,30);
        payAmount = number(j,"payAmount"); expiresAt = number(j,"expiresAt");
        version = number(j,"version"); updatedAt = number(j,"updatedAt"); cachedAt = savedAt;
        if (!id.matches("[A-Za-z0-9-]{36}") || payAmount <= 0 || expiresAt <= 0 || version < 1 || updatedAt <= 0
                || savedAt <= 0 || days.isEmpty() || !LocalJobDraft.DAYS.containsAll(days)
                || !LocalJobDraft.SHIFTS.contains(shift) || !LocalJobDraft.UNITS.contains(payUnit)
                || !LocalJobDraft.DEMANDS.containsAll(demands)) throw new JSONException("SCHEMA");
    }
    public static PublicJob fromServer(DocumentSnapshot doc, long now) throws JSONException {
        if (!doc.exists() || !"OPEN".equals(doc.getString("status"))) throw new JSONException("NOT_PUBLIC");
        JSONObject j = new JSONObject().put("schemaVersion",1).put("id",doc.getId());
        String[][] names = {{"title","title"},{"employerName","employerDisplayName"},
                {"categoryId","categoryId"},{"categoryName","categoryName"},{"areaCode","areaCode"},
                {"areaName","areaName"},{"workAddress","workAddress"},{"description","description"},
                {"requirements","requirementsText"},{"shift","workShift"},{"schedule","scheduleText"},{"payUnit","payUnit"}};
        for (String[] name : names) {
            Object value = doc.get(name[1]); if (!(value instanceof String)) throw new JSONException("SCHEMA");
            j.put(name[0],value);
        }
        for (String[] name : new String[][]{{"skills","requiredSkillTags"},{"days","workDays"},{"demands","physicalDemands"}}) {
            Object value = doc.get(name[1]); if (!(value instanceof List)) throw new JSONException("SCHEMA");
            j.put(name[0],new JSONArray((List<?>)value));
        }
        for (String name : new String[]{"payAmount","version"}) {
            Object value = doc.get(name); if (!(value instanceof Long)) throw new JSONException("SCHEMA"); j.put(name,value);
        }
        for (String name : new String[]{"expiresAt","updatedAt"}) {
            Object value = doc.get(name); if (!(value instanceof Timestamp)) throw new JSONException("SCHEMA");
            j.put(name,((Timestamp)value).toDate().getTime());
        }
        return new PublicJob(j,now);
    }
    public static PublicJob fromCache(String payload, long savedAt) throws JSONException {
        return new PublicJob(new JSONObject(payload),savedAt);
    }
    public String toJson() throws JSONException {
        return new JSONObject().put("schemaVersion",1).put("id",id).put("title",title).put("employerName",employerName)
                .put("categoryId",categoryId).put("categoryName",categoryName).put("areaCode",areaCode).put("areaName",areaName)
                .put("workAddress",workAddress).put("description",description).put("requirements",requirements)
                .put("skills",new JSONArray(skills)).put("days",new JSONArray(days)).put("demands",new JSONArray(demands))
                .put("shift",shift).put("schedule",schedule).put("payUnit",payUnit).put("payAmount",payAmount)
                .put("expiresAt",expiresAt).put("version",version).put("updatedAt",updatedAt).toString();
    }
    private static String string(JSONObject j,String key,int max,boolean required) throws JSONException {
        Object value = j.get(key);
        if (!(value instanceof String) || ((String)value).length() > max
                || (required && ((String)value).trim().isEmpty())) throw new JSONException("SCHEMA");
        return (String)value;
    }
    private static long number(JSONObject j,String key) throws JSONException {
        Object value = j.get(key);
        if (!(value instanceof Long) && !(value instanceof Integer)) throw new JSONException("SCHEMA");
        return ((Number)value).longValue();
    }
    private static List<String> list(JSONObject j,String key,int max,int width) throws JSONException {
        JSONArray a = j.getJSONArray(key); List<String> out = new ArrayList<>();
        if (a.length() > max) throw new JSONException("SCHEMA");
        for (int i=0;i<a.length();i++) {
            Object v=a.get(i); if (!(v instanceof String) || ((String)v).isEmpty() || ((String)v).length()>width || out.contains(v)) throw new JSONException("SCHEMA");
            out.add((String)v);
        }
        return Collections.unmodifiableList(out);
    }
}
