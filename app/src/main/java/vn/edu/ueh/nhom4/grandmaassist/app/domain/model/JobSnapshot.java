package vn.edu.ueh.nhom4.grandmaassist.app.domain.model;

import java.util.*;

public final class JobSnapshot {
    public final String title;
    public final String categoryId;
    public final String categoryName;
    public final String areaCode;
    public final String areaName;
    public final String workAddress;
    public final String scheduleText;
    public final List<String> physicalDemands;
    public final long payAmount;
    public final String payUnit;
    public final long jobVersion;

    public JobSnapshot(String title, String categoryId, String categoryName, String areaCode, String areaName,
                       String workAddress, String scheduleText, List<String> physicalDemands,
                       long payAmount, String payUnit, long jobVersion) {
        this.title = title == null ? "" : title;
        this.categoryId = categoryId == null ? "" : categoryId;
        this.categoryName = categoryName == null ? "" : categoryName;
        this.areaCode = areaCode == null ? "" : areaCode;
        this.areaName = areaName == null ? "" : areaName;
        this.workAddress = workAddress == null ? "" : workAddress;
        this.scheduleText = scheduleText == null ? "" : scheduleText;
        this.physicalDemands = physicalDemands == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(physicalDemands));
        this.payAmount = payAmount;
        this.payUnit = payUnit == null ? "" : payUnit;
        this.jobVersion = jobVersion;
    }

    @SuppressWarnings("unchecked")
    public static JobSnapshot fromMap(Map<String, Object> map) {
        if (map == null) return null;
        String title = (String) map.get("title");
        String catId = (String) map.get("categoryId");
        String catName = (String) map.get("categoryName");
        String areaCode = (String) map.get("areaCode");
        String areaName = (String) map.get("areaName");
        String address = (String) map.get("workAddress");
        String sched = (String) map.get("scheduleText");
        List<String> demands = (List<String>) map.get("physicalDemands");
        Long pay = (Long) map.get("payAmount");
        String unit = (String) map.get("payUnit");
        Long ver = (Long) map.get("jobVersion");
        return new JobSnapshot(title, catId, catName, areaCode, areaName, address, sched, demands,
                pay != null ? pay : 0L, unit, ver != null ? ver : 1L);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("categoryId", categoryId);
        map.put("categoryName", categoryName);
        map.put("areaCode", areaCode);
        map.put("areaName", areaName);
        map.put("workAddress", workAddress);
        map.put("scheduleText", scheduleText);
        map.put("physicalDemands", new ArrayList<>(physicalDemands));
        map.put("payAmount", payAmount);
        map.put("payUnit", payUnit);
        map.put("jobVersion", jobVersion);
        return map;
    }
}
