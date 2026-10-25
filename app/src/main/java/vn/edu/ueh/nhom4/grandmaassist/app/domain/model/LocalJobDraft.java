package vn.edu.ueh.nhom4.grandmaassist.app.domain.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;

/** Authored local content. No Firebase JobStatus, contact, identity, or reviewer data. */
public final class LocalJobDraft {
    public String id=UUID.randomUUID().toString(),ownerUid="";
    public long revision,createdAt,updatedAt,serverBaseVersion;
    public String workAddress="";
    public String title="",categoryId="",areaCode="",description="",requirements="",skillsInput="";
    public String shift="",schedule="",payInput="",payUnit="",demandMode="";
    public long expiresAt;
    public final List<String> days=new ArrayList<>(),demands=new ArrayList<>();
    public static final List<String> DAYS=Collections.unmodifiableList(Arrays.asList("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"));
    public static final List<String> SHIFTS=Collections.unmodifiableList(Arrays.asList("MORNING","AFTERNOON","EVENING","FLEXIBLE"));
    public static final List<String> UNITS=Collections.unmodifiableList(Arrays.asList("HOUR","SESSION","DAY"));
    public static final List<String> DEMANDS=Collections.unmodifiableList(Arrays.asList("HEAVY_LIFTING","PROLONGED_STANDING","FREQUENT_MOVEMENT"));
    public LocalJobDraft copy(){
        try{return fromJson(toJson(),ownerUid,id,revision,createdAt,updatedAt);}catch(JSONException e){throw new IllegalStateException(e);}
    }
    public boolean hasContent(){
        return !(title+categoryId+areaCode+workAddress+description+requirements+skillsInput+shift+schedule+payInput+payUnit+demandMode).trim().isEmpty()
                || !days.isEmpty() || !demands.isEmpty() || expiresAt!=0;
    }
    public String toJson() throws JSONException {
        JSONObject j=new JSONObject();j.put("schemaVersion",1);
        j.put("workAddress",workAddress);j.put("serverBaseVersion",serverBaseVersion);
        j.put("title",title);j.put("categoryId",categoryId);j.put("areaCode",areaCode);j.put("description",description);
        j.put("requirements",requirements);j.put("skillsInput",skillsInput);j.put("shift",shift);j.put("schedule",schedule);
        j.put("payInput",payInput);j.put("payUnit",payUnit);j.put("demandMode",demandMode);j.put("expiresAt",expiresAt);
        j.put("days",new JSONArray(days));j.put("demands",new JSONArray(demands));return j.toString();
    }
    public static LocalJobDraft fromJson(String json,String uid,String id,long revision,long created,long updated) throws JSONException {
        JSONObject j=new JSONObject(json);if(j.getInt("schemaVersion")!=1)throw new JSONException("SCHEMA");
        LocalJobDraft d=new LocalJobDraft();d.ownerUid=uid;d.id=id;d.revision=revision;d.createdAt=created;d.updatedAt=updated;
        d.workAddress=j.optString("workAddress","");d.serverBaseVersion=j.optLong("serverBaseVersion",0);
        d.title=j.getString("title");d.categoryId=j.getString("categoryId");d.areaCode=j.getString("areaCode");
        d.description=j.getString("description");d.requirements=j.getString("requirements");d.skillsInput=j.getString("skillsInput");
        d.shift=j.getString("shift");d.schedule=j.getString("schedule");d.payInput=j.getString("payInput");
        d.payUnit=j.getString("payUnit");d.demandMode=j.getString("demandMode");d.expiresAt=j.getLong("expiresAt");
        for(int i=0;i<j.getJSONArray("days").length();i++)d.days.add(j.getJSONArray("days").getString(i));
        for(int i=0;i<j.getJSONArray("demands").length();i++)d.demands.add(j.getJSONArray("demands").getString(i));
        if(!d.bounded())throw new JSONException("BOUNDS");return d;
    }
    public boolean bounded(){
        return workAddress.length()<=500 && serverBaseVersion>=0 && title.length()<=120 && description.length()<=3000 && requirements.length()<=1000 && skillsInput.length()<=500
                && schedule.length()<=500 && payInput.length()<=24 && categoryId.length()<=80 && areaCode.length()<=80
                && (shift.isEmpty()||SHIFTS.contains(shift)) && (payUnit.isEmpty()||UNITS.contains(payUnit))
                && Arrays.asList("","NONE","SELECT").contains(demandMode) && DAYS.containsAll(days) && days.size()<=7
                && new HashSet<>(days).size()==days.size() && DEMANDS.containsAll(demands) && demands.size()<=3
                && new HashSet<>(demands).size()==demands.size();
    }
    public List<String> skills(){
        LinkedHashSet<String> tags=new LinkedHashSet<>();for(String s:skillsInput.split(",|\n"))if(!s.trim().isEmpty())tags.add(s.trim());
        return new ArrayList<>(tags);
    }
    public Long payAmount(){
        String value=payInput.trim().replace('\u00a0',' ');
        if(!value.matches("[0-9]+|[0-9]{1,3}(\\.[0-9]{3})+|[0-9]{1,3}(,[0-9]{3})+|[0-9]{1,3}( [0-9]{3})+"))return null;
        try{long amount=Long.parseLong(value.replaceAll("[., ]",""));return amount>0?amount:null;}catch(NumberFormatException e){return null;}
    }
    public LinkedHashMap<String,String> validate(Set<String> categories,Set<String> areas,long now){
        LinkedHashMap<String,String> errors=new LinkedHashMap<>();
        if(title.trim().isEmpty()||title.length()>120)errors.put("title","Nhập tiêu đề, tối đa 120 ký tự.");
        if(!categories.contains(categoryId))errors.put("categoryId","Chọn danh mục đang hoạt động.");
        if(description.trim().isEmpty()||description.length()>3000)errors.put("description","Nhập công việc cần làm, tối đa 3.000 ký tự.");
        if(requirements.length()>1000)errors.put("requirements","Yêu cầu tối đa 1.000 ký tự.");
        if(skills().size()>8 || skills().stream().anyMatch(s->s.length()>60))errors.put("skillsInput","Tối đa 8 kỹ năng, mỗi kỹ năng 60 ký tự.");
        if(!areas.contains(areaCode))errors.put("areaCode","Chọn khu vực đang hoạt động.");
        if(workAddress.trim().isEmpty()||workAddress.length()>500)errors.put("workAddress","Nhập địa chỉ làm việc, tối đa 500 ký tự.");
        if(days.isEmpty()||!DAYS.containsAll(days))errors.put("days","Chọn ít nhất một ngày làm việc.");
        if(!SHIFTS.contains(shift))errors.put("shift","Chọn buổi làm việc.");
        if(schedule.trim().isEmpty()||schedule.length()>500)errors.put("schedule","Nhập lịch cụ thể, tối đa 500 ký tự.");
        if(!Arrays.asList("NONE","SELECT").contains(demandMode) || ("SELECT".equals(demandMode)&&demands.isEmpty())
                || ("NONE".equals(demandMode)&&!demands.isEmpty()))errors.put("demands","Chọn yêu cầu thể lực hoặc không có yêu cầu trên.");
        if(payAmount()==null)errors.put("payInput","Nhập số tiền VND nguyên, lớn hơn 0.");
        if(!UNITS.contains(payUnit))errors.put("payUnit","Chọn đơn vị tiền công.");
        if(expiresAt<=now)errors.put("expiresAt","Chọn hạn nhận sau thời điểm hiện tại.");
        return errors;
    }
}
