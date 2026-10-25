package vn.edu.ueh.nhom4.grandmaassist.app.domain.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.*;

/** Server-owned recruitment state; local revision and server version are separate. */
public final class JobPost {
    public final LocalJobDraft content;
    public final String status,employerName,reason,submissionId,categoryName,areaName;
    public final long version;
    public final Map<String,Object> data;
    public JobPost(DocumentSnapshot doc){
        if(!doc.exists())throw new IllegalArgumentException("MISSING");
        data=Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(doc.getData())));
        content=new LocalJobDraft();content.id=doc.getId();content.ownerUid=text("employerId");
        status=text("status");employerName=text("employerDisplayName");reason=text("reviewReason");
        submissionId=text("lastSubmissionId");categoryName=text("categoryName");areaName=text("areaName");
        Object v=data.get("version");if(!(v instanceof Long))throw new IllegalArgumentException("SCHEMA");version=(Long)v;
        content.serverBaseVersion=version;
        content.title=text("title");content.categoryId=text("categoryId");content.areaCode=text("areaCode");
        content.workAddress=text("workAddress");content.description=text("description");content.requirements=text("requirementsText");
        content.skillsInput=String.join(", ",list("requiredSkillTags"));content.shift=text("workShift");content.schedule=text("scheduleText");
        content.payInput=String.valueOf(data.get("payAmount"));content.payUnit=text("payUnit");
        content.days.addAll(list("workDays"));content.demands.addAll(list("physicalDemands"));
        content.demandMode=content.demands.isEmpty()?"NONE":"SELECT";
        Object expiry=data.get("expiresAt");if(!(expiry instanceof Timestamp))throw new IllegalArgumentException("SCHEMA");
        content.expiresAt=((Timestamp)expiry).toDate().getTime();
        if(!content.bounded()||version<1||!Arrays.asList("PENDING_REVIEW","OPEN","REJECTED","CLOSED","HIDDEN").contains(status))throw new IllegalArgumentException("SCHEMA");
    }
    public String text(String key){Object v=data.get(key);return v instanceof String?(String)v:"";}
    private List<String> list(String key){Object v=data.get(key);if(!(v instanceof List))throw new IllegalArgumentException("SCHEMA");List<String> out=new ArrayList<>();for(Object item:(List<?>)v){if(!(item instanceof String))throw new IllegalArgumentException("SCHEMA");out.add((String)item);}return out;}
    public static String statusLabel(String status){switch(status){case "PENDING_REVIEW":return "Chờ duyệt";case "OPEN":return "Đã duyệt";case "REJECTED":return "Bị từ chối";case "CLOSED":return "Đã đóng";case "HIDDEN":return "Đã ẩn";default:return "Không rõ trạng thái";}}
    public String operationId(){return content.id+"_s"+(content.serverBaseVersion+1);}
    public boolean sameContent(LocalJobDraft d){LocalJobDraft s=content;
        return s.title.equals(d.title.trim())&&s.categoryId.equals(d.categoryId)&&s.areaCode.equals(d.areaCode)
            &&s.workAddress.equals(d.workAddress.trim())&&s.description.equals(d.description.trim())
            &&s.requirements.equals(d.requirements.trim())&&s.skills().equals(d.skills())&&s.days.equals(d.days)
            &&s.shift.equals(d.shift)&&s.schedule.equals(d.schedule.trim())&&s.demands.equals(d.demands)
            &&Objects.equals(s.payAmount(),d.payAmount())&&s.payUnit.equals(d.payUnit)&&s.expiresAt==d.expiresAt;
    }
}
