package vn.edu.ueh.nhom4.grandmaassist.app.data.repository;

import com.google.android.gms.tasks.*;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import java.util.*;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.*;

/** Online transactions only. Every retry resolves the same job and submission version. */
public final class JobPostRepository {
    public static final class Failure extends RuntimeException {public final String code;public Failure(String c){super(c);code=c;}}
    public static final class Page {public final List<JobPost> items;public final DocumentSnapshot cursor;public final boolean more;
        Page(QuerySnapshot s){items=new ArrayList<>();for(DocumentSnapshot d:s)items.add(new JobPost(d));cursor=s.isEmpty()?null:s.getDocuments().get(s.size()-1);more=s.size()==20;}}
    private final FirebaseFirestore db=FirebaseFirestore.getInstance();
    public static <T> Task<T> bounded(Task<T> task){return Tasks.withTimeout(task,45,java.util.concurrent.TimeUnit.SECONDS);}
    public boolean same(String uid){FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();return u!=null&&u.getUid().equals(uid);}
    private Task<String> proof(String uid){FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();
        if(u==null||!same(uid))return Tasks.forException(new Failure("SESSION"));
        return u.getIdToken(true).continueWith(t->{if(!t.isSuccessful())throw t.getException();if(!same(uid))throw new Failure("SESSION");Object v=t.getResult().getClaims().get("phone_number");return v instanceof String?(String)v:"";});}
    public Task<Page> list(String uid,boolean admin,DocumentSnapshot cursor){
        if(!same(uid))return Tasks.forException(new Failure("SESSION"));
        Query q=db.collection("jobs");q=admin?q.whereEqualTo("status","PENDING_REVIEW"):q.whereEqualTo("employerId",uid);
        q=q.orderBy("updatedAt",Query.Direction.DESCENDING).orderBy(FieldPath.documentId(),Query.Direction.DESCENDING).limit(20);
        if(cursor!=null)q=q.startAfter(cursor);
        return bounded(q.get(Source.SERVER).continueWith(t->{if(!t.isSuccessful())throw t.getException();if(!same(uid))throw new Failure("SESSION");return new Page(t.getResult());}));
    }
    public Task<JobPost> get(String uid,String id){
        if(!same(uid))return Tasks.forException(new Failure("SESSION"));
        return bounded(db.collection("jobs").document(id).get(Source.SERVER).continueWith(t->{if(!t.isSuccessful())throw t.getException();if(!same(uid))throw new Failure("SESSION");if(!t.getResult().exists())throw new Failure("MISSING");return new JobPost(t.getResult());}));
    }
    private String text(DocumentSnapshot d,String k){String s=d.getString(k);return s==null?"":s;}
    private void active(DocumentSnapshot user,String role){if(!user.exists()||!role.equals(user.getString("role"))||!"ACTIVE".equals(user.getString("accountStatus")))throw new Failure("ACCESS");}
    public Task<JobPost> submit(String uid,LocalJobDraft original){
        final LocalJobDraft d=original.copy();if(!uid.equals(d.ownerUid))return Tasks.forException(new Failure("SESSION"));
        final String op=d.id+"_s"+(d.serverBaseVersion+1);
        return bounded(proof(uid).continueWithTask(p->{
            if(!p.isSuccessful())throw p.getException();String phone=p.getResult();
            return db.runTransaction(tx->{
                if(!same(uid))throw new Failure("SESSION");DocumentReference ref=db.collection("jobs").document(d.id);
                DocumentSnapshot old=tx.get(ref);
                // Recovery must also work after an Admin has already decided this submission.
                if(old.exists()&&uid.equals(old.getString("employerId"))&&op.equals(old.getString("lastSubmissionId")))return null;
                DocumentSnapshot user=tx.get(db.collection("users").document(uid));
                DocumentSnapshot access=tx.get(db.collection("adminAccess").document(uid));
                DocumentSnapshot profile=tx.get(db.collection("employerProfiles").document(uid));
                DocumentSnapshot cat=tx.get(db.collection("jobCategories").document(d.categoryId));
                DocumentSnapshot area=tx.get(db.collection("areas").document(d.areaCode));
                active(user,"EMPLOYER");if(Boolean.TRUE.equals(access.getBoolean("enabled")))throw new Failure("ACCESS");
                if(phone.isEmpty()||!phone.equals(user.getString("phoneE164"))||!"FIREBASE_AUTH".equals(user.getString("phoneVerificationMode")))throw new Failure("PHONE");
                if(!profile.exists()||text(profile,"introduction").trim().isEmpty()||text(profile,"areaCode").isEmpty()
                    ||text(user,"displayName").trim().isEmpty()||("ORGANIZATION".equals(profile.getString("employerType"))&&text(profile,"organizationName").trim().isEmpty()))throw new Failure("PROFILE");
                DocumentSnapshot profileArea=tx.get(db.collection("areas").document(text(profile,"areaCode")));
                if(!Boolean.TRUE.equals(profileArea.getBoolean("active")))throw new Failure("PROFILE");
                if(!Boolean.TRUE.equals(cat.getBoolean("active"))||!Boolean.TRUE.equals(area.getBoolean("active")))throw new Failure("CATALOG");
                if(!d.validate(Collections.singleton(d.categoryId),Collections.singleton(d.areaCode),System.currentTimeMillis()).isEmpty())throw new Failure("FORM");
                if(old.exists()){
                    if(!uid.equals(old.getString("employerId"))||!"REJECTED".equals(old.getString("status"))||!Objects.equals(old.getLong("version"),d.serverBaseVersion))throw new Failure("CONFLICT");
                }else if(d.serverBaseVersion!=0)throw new Failure("CONFLICT");
                long version=d.serverBaseVersion+1;
                Map<String,Object> m=new HashMap<>();m.put("id",d.id);m.put("employerId",uid);
                String name="ORGANIZATION".equals(profile.getString("employerType"))?text(profile,"organizationName"):text(user,"displayName");
                m.put("employerDisplayName",name);m.put("title",d.title.trim());m.put("categoryId",d.categoryId);m.put("categoryName",text(cat,"name"));
                m.put("areaCode",d.areaCode);m.put("areaName",text(area,"name"));m.put("workAddress",d.workAddress.trim());m.put("description",d.description.trim());
                m.put("requirementsText",d.requirements.trim());m.put("requiredSkillTags",d.skills());m.put("workDays",new ArrayList<>(d.days));
                m.put("workShift",d.shift);m.put("scheduleText",d.schedule.trim());m.put("physicalDemands",new ArrayList<>(d.demands));m.put("demandsDeclared",true);
                m.put("payAmount",d.payAmount());m.put("payUnit",d.payUnit);m.put("expiresAt",new Timestamp(new Date(d.expiresAt)));
                m.put("status","PENDING_REVIEW");m.put("version",version);m.put("lastSubmissionId",op);
                m.put("createdAt",old.exists()?old.get("createdAt"):FieldValue.serverTimestamp());m.put("updatedAt",FieldValue.serverTimestamp());
                m.put("reviewedBy","");m.put("reviewedAt",null);m.put("reviewReason","");
                tx.set(ref,m);tx.set(ref.collection("history").document(String.valueOf(version)),event(m,old,uid,"SUBMITTED"));return null;
            }).continueWithTask(t->{if(!t.isSuccessful())throw t.getException();return get(uid,d.id);});
        }));
    }
    private Map<String,Object> event(Map<String,Object> after,DocumentSnapshot before,String actor,String action){
        Map<String,Object> e=new HashMap<>();e.put("jobId",after.get("id"));e.put("version",after.get("version"));
        e.put("actorId",actor);e.put("action",action);e.put("createdAt",FieldValue.serverTimestamp());
        e.put("before",before.exists()?before.getData():null);e.put("after",new HashMap<>(after));return e;
    }
    public Task<JobPost> decide(String uid,JobPost expected,boolean approve,String reason){
        String note=approve?"":reason.trim();if(!approve&&(note.isEmpty()||note.length()>1000))return Tasks.forException(new Failure("REASON"));
        return bounded(proof(uid).continueWithTask(p->{if(!p.isSuccessful())throw p.getException();return db.runTransaction(tx->{
            if(!same(uid))throw new Failure("SESSION");DocumentReference ref=db.collection("jobs").document(expected.content.id);
            DocumentSnapshot old=tx.get(ref),u=tx.get(db.collection("users").document(uid)),a=tx.get(db.collection("adminAccess").document(uid));
            active(u,"ADMIN");if(!Boolean.TRUE.equals(a.getBoolean("enabled")))throw new Failure("ACCESS");
            if(!old.exists()||uid.equals(old.getString("employerId"))||!"PENDING_REVIEW".equals(old.getString("status"))||!Objects.equals(old.getLong("version"),expected.version))throw new Failure("CONFLICT");
            if(approve){
                DocumentSnapshot category=tx.get(db.collection("jobCategories").document(old.getString("categoryId")));
                DocumentSnapshot area=tx.get(db.collection("areas").document(old.getString("areaCode")));
                if(!Boolean.TRUE.equals(category.getBoolean("active"))||!Boolean.TRUE.equals(area.getBoolean("active")))throw new Failure("CATALOG");
                if(old.getTimestamp("expiresAt").toDate().getTime()<=System.currentTimeMillis())throw new Failure("EXPIRED");
            }
            Map<String,Object> m=new HashMap<>(Objects.requireNonNull(old.getData()));m.put("version",expected.version+1);
            m.put("status",approve?"OPEN":"REJECTED");m.put("reviewReason",note);m.put("reviewedBy",uid);
            m.put("reviewedAt",FieldValue.serverTimestamp());m.put("updatedAt",FieldValue.serverTimestamp());
            tx.set(ref,m);tx.set(ref.collection("history").document(String.valueOf(expected.version+1)),event(m,old,uid,approve?"APPROVED":"REJECTED"));return null;
        }).continueWithTask(t->{if(!t.isSuccessful())throw t.getException();return get(uid,expected.content.id);});}));
    }
    public static String error(Exception ex){Throwable c=ex;for(int i=0;c!=null&&i<10;i++,c=c.getCause()){
        if(c instanceof java.util.concurrent.TimeoutException)return "Chưa xác định được kết quả sau 45 giây. Hãy tải lại hoặc gửi lại để đối chiếu; không tạo bản khác.";
        if(c instanceof Failure){switch(((Failure)c).code){
            case "PHONE":return "Cần xác minh và đồng bộ số điện thoại trong Tài khoản.";
            case "PROFILE":return "Cần hoàn thiện hồ sơ người thuê và khu vực hoạt động.";
            case "ACCESS":return "Tài khoản không có quyền thực hiện thao tác này.";
            case "CONFLICT":return "Tin đã thay đổi hoặc đã được xử lý. Hãy tải lại tin.";
            case "EXPIRED":return "Tin đã hết hạn nhận. Không thể chấp thuận.";
            case "REASON":return "Nhập lý do từ chối, tối đa 1.000 ký tự.";
            case "CATALOG":return "Danh mục hoặc khu vực không còn hoạt động. Hãy cập nhật lại.";
            case "FORM":return "Kiểm tra lại nội dung, địa chỉ và hạn nhận trước khi gửi.";
            case "MISSING":return "Tin không còn tồn tại.";
            case "SESSION":return "Tài khoản đã thay đổi. Hãy đăng nhập lại.";
        }}
        if(c instanceof FirebaseFirestoreException){FirebaseFirestoreException.Code code=((FirebaseFirestoreException)c).getCode();
            if(code==FirebaseFirestoreException.Code.PERMISSION_DENIED)return "Chưa đủ quyền hoặc dữ liệu không hợp lệ. Kiểm tra Rules và tải lại.";
            if(code==FirebaseFirestoreException.Code.FAILED_PRECONDITION)return "Chưa hoàn tất. Kiểm tra chỉ mục Firestore hoặc tải lại tin.";}
    }return JobDraftRepository.network(ex)?"Chưa kết nối được. Nếu vừa gửi, thử lại để đối chiếu đúng tin; nháp vẫn được giữ.":"Chưa hoàn tất thao tác. Hãy tải lại và thử lại.";}
}
